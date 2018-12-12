/*

Copyright (c) 2000-2018, Board of Trustees of Leland Stanford Jr. University
All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are met:

1. Redistributions of source code must retain the above copyright notice,
this list of conditions and the following disclaimer.

2. Redistributions in binary form must reproduce the above copyright notice,
this list of conditions and the following disclaimer in the documentation
and/or other materials provided with the distribution.

3. Neither the name of the copyright holder nor the names of its contributors
may be used to endorse or promote products derived from this software without
specific prior written permission. 

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
POSSIBILITY OF SUCH DAMAGE.

*/

package org.lockss.util;

import java.io.*;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileChannel.MapMode;
import java.util.*;

import org.apache.commons.collections.primitives.*;
import org.apache.commons.collections4.map.LRUMap;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.output.ByteArrayOutputStream;

public class FileBackedList<E> extends AbstractList<E> {

  protected static final int CHUNK = 1024 * 1024 * 1024; // 1GB
  
  public static class Chunk {
    long beginOffset;
    long endOffset;
    public Chunk(long beginOffset, long endOffset) {
      this.beginOffset = beginOffset;
      this.endOffset = endOffset;
    }
    
  }
  
  protected File file;
  
  protected boolean deleteFile;
  
  protected RandomAccessFile craf;
  
  protected FileChannel chan;
  
  protected LongList offsets;
  
  protected ArrayLongList arrayLongList;
  
  protected FileBackedLongList fileBackedLongList;

  protected List<Chunk> chunks;
  
  protected LRUMap<Integer, MappedByteBuffer> buffers;
  
  public FileBackedList() throws IOError {
    this(Collections.<E>emptyIterator(),
         createTempFile());
    this.deleteFile = true;
  }

  public FileBackedList(String filePath) throws IOError {
    this(Collections.<E>emptyIterator(),
         new File(filePath));
  }
  
  public FileBackedList(File file) throws IOError {
    this(Collections.<E>emptyIterator(),
         file);
  }
  
  public FileBackedList(Iterator<E> iterator) throws IOError {
    this(iterator,
         createTempFile());
    this.deleteFile = true;
  }
  
  public FileBackedList(Collection<E> coll) throws IOError {
    this(coll.iterator(),
         createTempFile());
    this.deleteFile = true;
  }

  public FileBackedList(Iterator<E> iterator,
                        String filePath)
      throws IOError {
    this(iterator,
         new File(filePath));
  }
  
  public FileBackedList(Collection<E> coll,
                        String filePath)
      throws IOError {
    this(coll.iterator(),
         new File(filePath));
  }
  
  public FileBackedList(Collection<E> coll,
                        File file) 
      throws IOError {
    this(coll.iterator(),
         file);
  }
  
  public FileBackedList(Iterator<E> iterator,
                        File file) 
      throws IOError {
    try {
      this.file = file;
      this.deleteFile = false; // reset by some constructors
      this.craf = new CountingRandomAccessFile(file, CountingRandomAccessFile.MODE_READ_WRITE, false);
      this.chan = craf.getChannel();
      this.arrayLongList = new ArrayLongList() {
        @Override
        public void clear() {
          // Slightly less horrible than what Commons Primitives 1.0 does
          for (int i = size() - 1 ; i >= 0 ; --i) {
            removeElementAt(i);
          }
        }
      };
      this.fileBackedLongList = null;
      this.offsets = arrayLongList;
      this.chunks = new ArrayList<Chunk>();
      chunks.add(new Chunk(0L, 0L));
      this.buffers = new LRUMap<Integer, MappedByteBuffer>(3);
      buffers.put(0, chan.map(MapMode.READ_WRITE, 0L, CHUNK));
      populate(iterator);
    }
    catch (IOException exc) {
      throw new IOError(exc);
    }
  }
  
  @Override
  public void add(int index, E element) throws IOError {
    if (index < 0 || index > size()) {
      throw new IndexOutOfBoundsException(Integer.toString(index));
    }
    try {
      offsets.add(index, append(element));
      if (size() > 1_000_000 && fileBackedLongList == null) {
        // Starting to get too large for main memory; go to disk also
        fileBackedLongList = new FileBackedLongList(file.getPath() + ".longs");
        for (LongIterator iter = arrayLongList.iterator() ; iter.hasNext() ; ) {
          fileBackedLongList.add(iter.next());
        }
        arrayLongList.clear(); // see Commons Primitives 1.0 note in constructor
        arrayLongList.trimToSize();
        arrayLongList = null;
        offsets = fileBackedLongList;
      }
    }
    catch (IOException exc) {
      throw new IOError(exc);
    }
  }
  
  @Override
  public E get(int index) throws IOError {
    if (index < 0 || index >= size()) {
      throw new IndexOutOfBoundsException(Integer.toString(index));
    }
    try {
      long off = offsets.get(index);
      int chunkNum = getChunkNumberByOffset(off);
      MappedByteBuffer mbbuf = getBufferByChunkNumber(chunkNum);
      mbbuf.position((int)(off - chunks.get(chunkNum).beginOffset));
      int len = mbbuf.getInt();
      byte[] bytes = new byte[len];
      mbbuf.get(bytes);
      return (E)fromBytes(bytes);
    }
    catch (IOException exc) {
      throw new IOError(exc);
    }
  }
  
  public void release() {
    for (MappedByteBuffer mbbuf : buffers.values()) {
      mbbuf.force();
    }
    IOUtils.closeQuietly(craf);
    craf = null;
    IOUtils.closeQuietly(chan);
    chan = null;
    buffers.clear();
    buffers = null;
    if (arrayLongList != null) {
      arrayLongList.clear();
      arrayLongList.trimToSize();
      arrayLongList = null;
    }
    if (fileBackedLongList != null) {
      fileBackedLongList.release();
      fileBackedLongList = null;
      if (deleteFile) {
        new File(file.getPath() + ".longs").delete();
      }
    }
    offsets = null;
    if (deleteFile) {
      file.delete();
    }
    file = null;
  }
  
  @Override
  public E remove(int index) {
    E ret = get(index); // does the bounds check
    offsets.removeElementAt(index);
    return ret;
  }
  
  @Override
  public E set(int index, E element) throws IOError {
    E ret = get(index); // does the bounds check
    try {
      offsets.set(index, append(element));
      return ret;
    }
    catch (IOException exc) {
      throw new IOError(exc);
    }
  }
  
  @Override
  public int size() {
    return offsets.size();
  }

  protected long append(E element) throws IOException {
    byte[] bytes = toBytes(element);
    // Allocate new chunk if necessary
    int chunkNum = chunks.size() - 1;
    Chunk lastChunk = chunks.get(chunkNum);
    long ret = lastChunk.endOffset;
    if (lastChunk.endOffset - lastChunk.beginOffset + bytes.length > CHUNK) {
      lastChunk = new Chunk(lastChunk.endOffset, lastChunk.endOffset);
      chunks.add(lastChunk);
      ++chunkNum;
    }
    // Append bytes to last chunk
    MappedByteBuffer mbbuf = getBufferByChunkNumber(chunkNum);
    mbbuf.position((int)(lastChunk.endOffset - lastChunk.beginOffset));
    mbbuf.putInt(bytes.length);
    lastChunk.endOffset += 4;
    mbbuf.put(bytes);
    lastChunk.endOffset += bytes.length;
    return ret;
  }

  @Override
  protected void finalize() throws Throwable {
    super.finalize();
    release();
  }
  
  protected MappedByteBuffer getBufferByChunkNumber(int chunkNum) throws IOException {
    MappedByteBuffer ret = buffers.get(chunkNum);
    if (ret == null) {
      // Potentially evict a buffer
      Collection<MappedByteBuffer> before = buffers.values();
      ret = chan.map(MapMode.READ_WRITE, chunks.get(chunkNum).beginOffset, CHUNK);
      buffers.put(chunkNum, ret);
      Collection<MappedByteBuffer> after = buffers.values();
      before.removeAll(after); // before now contains zero or one evicted buffer
      for (Iterator<MappedByteBuffer> iter = before.iterator() ; iter.hasNext() ; ) {
        iter.next().force(); // flush to disk
      }
    }
    return ret;
  }
  
  protected int getChunkNumberByOffset(long offset) {
    int low = 0;
    int high = chunks.size() - 1;
    while (low <= high) {
      int mid = (low + high) / 2;
      Chunk chunk = chunks.get(mid);
      if (offset < chunk.beginOffset) {
        high = mid - 1;
      }
      else if (offset >= chunk.endOffset) {
        low = mid + 1;
      }
      else {
        return mid;
      }
    }
    return -1; // should not happen
  }
  
  protected void populate(Iterator<E> iterator) throws IOException {
    while (iterator.hasNext()) {
      offsets.add(append(iterator.next()));
    }
  }
  
  protected static File createTempFile() throws IOError {
    try {
      File tempFile = File.createTempFile(FileBackedList.class.getSimpleName(), ".bin");
      tempFile.deleteOnExit();
      return tempFile;
    }
    catch (IOException exc) {
      throw new IOError(exc);
    }
  }
  
  protected static byte[] toBytes(Object obj) throws IOError {
    try {
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      ObjectOutputStream oos = new ObjectOutputStream(baos);
      oos.writeObject(obj);
      return baos.toByteArray();
    }
    catch (IOException exc) {
      throw new IOError(exc);
    }
  }
  
  protected static Object fromBytes(byte[] bytes) throws IOError {
    try {
      ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
      ObjectInputStream ois = new ObjectInputStream(bais);
      return ois.readObject();
    }
    catch (ClassNotFoundException | IOException exc) {
      throw new IOError(exc);
    }
  }
  
}
