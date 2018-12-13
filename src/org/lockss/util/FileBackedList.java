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
import java.nio.*;
import java.nio.channels.FileChannel;
import java.nio.channels.FileChannel.MapMode;
import java.util.*;

import org.apache.commons.collections.primitives.*;
import org.apache.commons.collections4.map.LRUMap;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.output.ByteArrayOutputStream;

/**
 * <p>
 * A concrete implementation of {@link AbstractList}that is backed by
 * memory-mapped files rather than main memory.
 * </p>
 * <p>
 * This class implements {@link AutoCloseable} so it can be used in a
 * try-with-resources block. Although {@link #close()} will be called by
 * {@link #finalize()} when the instance is garbage-collected, you should
 * call {@link #close()} appropriately, whether with try-with-resources,
 * try/finally, or some other means.
 * </p>
 * <p>
 * The underlying implementation uses a {@link CountingRandomAccessFile}
 * and views it as a succession of chunks, that are accessed via
 * {@link MappedByteBuffer} instances. The {@link MappedByteBuffer} for each
 * chunk is opened with a length of exactly {@link #CHUNK} bytes even though
 * each chunk is at most {@link #CHUNK} bytes, but this class is constructed in
 * such a way that chunks and chunk accesses are strictly non-overlapping. To
 * avoid running out of handles, an {@link LRUMap} cache is used to keep up to
 * {@link #CHUNKS} most recently-accessed chunks, while less-recently used
 * chunks are evicted.
 * </p>
 * <p>
 * This backing byte array does not slide when list elements are altered,
 * inserted or removed; rather, new elements are appended to the end and the
 * list of internal file offsets for each list element is updated. This internal
 * list is a {@link ArrayLongList} instance up to {@link #OFFSETS} list
 * elements. Beyond that, it switches to a {@link FileBackedLongList} so as not
 * to grow excessively in main memory.
 * </p>
 * 
 * @since 1.75
 * @see Chunk
 */
public class FileBackedList<E>
    extends AbstractList<E>
    implements AutoCloseable {

  /**
   * <p>
   * The length of a chunk's {@link MappedByteBuffer}.
   * </p>
   * 
   * @since 1.75
   */
  protected static final int CHUNK = 1024 * 1024 * 1024; // 1GB

  /**
   * <p>
   * The maximum number of chunks that can be kept live in the cache.
   * </p>
   * 
   * @since 1.75
   * @see #buffers
   */
  protected static final int CHUNKS = 3;

  /**
   * <p>
   * The number of list items beyond which the internal list of {@code long}
   * offsets is streamed to disk using a {@link FileBackedLongList}.
   * </p>
   * 
   * @since 1.75
   */
  protected static final int OFFSETS = 1_000_000;

  /**
   * <p>
   * A pair of offsets describing a disk chunk; {@link #beginOffset} is
   * inclusive and {@link endOffset} is exclusive, like the beginning and
   * ending indices of {@link String#substring(int, int)} or
   * {@link List#subList(int, int)}.
   * </p>
   * 
   * @since 1.75
   */
  public static class Chunk {
    
    /**
     * <p>
     * The beginning offset(inclusive).
     * </p>
     * 
     * @since 1.75
     */
    private long beginOffset;

    /**
     * <p>
     * The ending offset(exclusive).
     * </p>
     * 
     * @since 1.75
     */
    private long endOffset;
    
    /**
     * <p>
     * Makes a new chunk.
     * </p>
     * 
     * @param beginOffset
     *          The beginning offset (inclusive).
     * @param endOffset
     *          The ending offset (exclusive).
     * @since 1.75
     */
    public Chunk(long beginOffset, long endOffset) {
      this.beginOffset = beginOffset;
      this.endOffset = endOffset;
    }
    
  }
  
  /**
   * <p>
   * The {@link File} backing this list.
   * </p>
   * 
   * @since 1.75
   */
  protected File file;
  
  /**
   * <p>
   * Whether the file backing this list must be deleted when the list is
   * garbage-collected or {@link #close()} is called; should be false when the
   * list was instantiated with a user-provided file and true when
   * instantiated with a constructed-provided temporary file.
   * </p>
   * 
   * @since 1.75
   * @see #file
   */
  protected boolean deleteFile;
  
  /**
   * <p>
   * The {@link CountingRandomAccessFile} backing this list.
   * </p>
   * 
   * @since 1.75
   * @see CountingRandomAccessFile
   * @see #file
   */
  protected CountingRandomAccessFile craf;
  
  /**
   * <p>
   * The {@link FileChannel} backing this list.
   * </p>
   * 
   * @since 1.75
   * @see #craf
   */
  protected FileChannel chan;
  
  /**
   * <p>
   * The list of offsets in the backing file of each element in the list.
   * </p>
   * <p>
   * This is either the value of {@link #arrayLongList} or
   * {@link #fileBackedLongList}, which have their own fields because they do
   * not both have the same API for use in {@link #close()}.
   * </p>
   * 
   * @since 1.75
   */
  protected LongList offsets;

  /**
   * <p>
   * An {@link ArrayLongList} used if the offset list is deemed small enough to
   * fit in main memory.
   * </p>
   * 
   * @since 1.75
   * @see #offsets
   */
  protected ArrayLongList arrayLongList;
  
  /**
   * <p>
   * A {@link FileBackedLongList} used if the offset list is deemed too large to
   * fit in main memory.
   * </p>
   * 
   * @since 1.75
   * @see #offsets
   */
  protected FileBackedLongList fileBackedLongList;

  /**
   * <p>
   * A list of allocated chunks.
   * </p>
   * 
   * @since 1.75
   * @see Chunk
   */
  protected List<Chunk> chunks;

  /**
   * <p>
   * An {@link LRUMap} instances keeping up to {@link #CHUNKS} most-recently
   * used chunks' {@link MappedByteBuffer} instances.
   * </p>
   * 
   * @since 1.75
   */
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
      this.buffers = new LRUMap<Integer, MappedByteBuffer>(CHUNKS);
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
      if (size() > OFFSETS && fileBackedLongList == null) {
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
  
  /**
   * <p>
   * Releases all resources associated with this list; using the list after
   * closing it results in unspecified error conditions.
   * </p>
   * 
   * @since 1.75
   */
  @Override
  public void close() {
    for (MappedByteBuffer mbbuf : buffers.values()) {
      mbbuf.force();
      CountingRandomAccessFile.unmap(mbbuf);
    }
    buffers.clear();
    buffers = null;
    IOUtils.closeQuietly(craf);
    craf = null;
    IOUtils.closeQuietly(chan);
    chan = null;
    if (arrayLongList != null) {
      arrayLongList.clear(); // see Commons Primitives 1.0 note in constructor
      arrayLongList.trimToSize();
      arrayLongList = null;
    }
    if (fileBackedLongList != null) {
      fileBackedLongList.close();
      fileBackedLongList = null;
      new File(file.getPath() + ".longs").delete(); // FIXME
    }
    offsets = null;
    if (deleteFile) {
      file.delete();
    }
    file = null;
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
    if (lastChunk.endOffset - lastChunk.beginOffset + bytes.length + 4 > CHUNK) {
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
    close();
  }
  
  protected MappedByteBuffer getBufferByChunkNumber(int chunkNum) throws IOException {
    MappedByteBuffer ret = buffers.get(chunkNum);
    if (ret == null) {
      // Potentially evict a buffer
      Collection<MappedByteBuffer> before = buffers.values();
      buffers.put(chunkNum, null); // dummy value
      Collection<MappedByteBuffer> after = buffers.values();
      before.removeAll(after); // before now contains zero or one evicted buffer
      for (Iterator<MappedByteBuffer> iter = before.iterator() ; iter.hasNext() ; ) {
        MappedByteBuffer oldBuf = iter.next();
        CountingRandomAccessFile.unmap(oldBuf);
        oldBuf = null;
      }
      // Allocate new buffer
      ret = chan.map(MapMode.READ_WRITE, chunks.get(chunkNum).beginOffset, CHUNK);
      buffers.put(chunkNum, ret); // real value
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
