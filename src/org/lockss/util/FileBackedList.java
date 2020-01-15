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
import org.lockss.util.*;

/**
 * <p>
 * A concrete implementation of {@link AbstractList}that is backed by
 * memory-mapped files rather than main memory.
 * </p>
 * <p>
 * This class implements {@link AutoCloseable} so it can be used in a
 * try-with-resources block. Although {@link #close()} will be called by
 * {@link #finalize()} when the instance is garbage-collected, you should call
 * {@link #close()} appropriately, whether with try-with-resources, try/finally,
 * or some other means.
 * </p>
 * <p>
 * The underlying implementation uses a {@link CountingRandomAccessFile} and
 * views it as a succession of chunks, that are accessed via
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
 * @param <T>
 *          The type of element held by this list. <b>Currently this needs to be
 *          a {@link Serializable} type.
 * @since 1.74.4
 * @see Chunk
 */
public class FileBackedList<E>
    extends AbstractList<E>
    implements AutoCloseable {

  private static Logger log = Logger.getLogger("FileBackedList");

  /**
   * <p>
   * A pair of offsets describing a disk chunk; {@link #beginOffset} is
   * inclusive and {@link endOffset} is exclusive, like the beginning and
   * ending indices of {@link String#substring(int, int)} or
   * {@link List#subList(int, int)}.
   * </p>
   * 
   * @since 1.74.4
   */
  public static class Chunk {
    
    /**
     * <p>
     * The beginning offset(inclusive).
     * </p>
     * 
     * @since 1.74.4
     */
    private long beginOffset;

    /**
     * <p>
     * The ending offset(exclusive).
     * </p>
     * 
     * @since 1.74.4
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
     * @since 1.74.4
     */
    public Chunk(long beginOffset, long endOffset) {
      this.beginOffset = beginOffset;
      this.endOffset = endOffset;
    }
    
  }

  /**
   * <p>
   * A version of {@link LRUMap} that forces (flushes) and unmaps the evicted
   * {@link MappedByteBuffer} entry.
   * </p>
   * 
   * @since 1.74.4
   * @see #removeLRU(LinkEntry)
   * @see CountingRandomAccessFile#unmap(MappedByteBuffer)
   */
  public static class UnmapLRUMap extends LRUMap<Integer, MappedByteBuffer> {

    /**
     * <p>
     * Makes a new instance with the given maximum size.
     * </p>
     * 
     * @param maxSize
     *          The map's maximum size.
     * @since 1.74.4
     */
    public UnmapLRUMap(int maxSize) {
      super(maxSize);
    }
    
    @Override
    protected boolean removeLRU(LinkEntry<Integer, MappedByteBuffer> entry) {
      MappedByteBuffer mbbuf = entry.getValue();
      mbbuf.force();
      CountingRandomAccessFile.unmap(mbbuf); // Unmap evicted buffer
      return true;
    }
    
  }
  
  /**
   * <p>
   * Whether this auto-closeable object has been closed.
   * </p>
   * 
   * @since 1.74.7
   */
  protected boolean closed;
  
  /**
   * <p>
   * The {@link File} backing this list.
   * </p>
   * 
   * @since 1.74.4
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
   * @since 1.74.4
   * @see #file
   */
  protected boolean deleteFile;

  /**
   * <p>
   * The {@link CountingRandomAccessFile} backing this list.
   * </p>
   * 
   * @since 1.74.4
   * @see CountingRandomAccessFile
   * @see #file
   */
  protected CountingRandomAccessFile craf;
  
  /**
   * <p>
   * The {@link FileChannel} backing this list.
   * </p>
   * 
   * @since 1.74.4
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
   * @since 1.74.4
   */
  protected LongList offsets;
  
  /**
   * <p>
   * An {@link ArrayLongList} used if the offset list is deemed small enough to
   * fit in main memory.
   * </p>
   * 
   * @since 1.74.4
   * @see #offsets
   */
  protected ArrayLongList arrayLongList;
  
  /**
   * <p>
   * A {@link FileBackedLongList} used if the offset list is deemed too large to
   * fit in main memory.
   * </p>
   * 
   * @since 1.74.4
   * @see #offsets
   */
  protected FileBackedLongList fileBackedLongList;
  
  /**
   * <p>
   * A list of allocated chunks.
   * </p>
   * 
   * @since 1.74.4
   * @see Chunk
   */
  protected List<Chunk> chunks;

  /**
   * <p>
   * An {@link LRUMap} instances keeping up to {@link #CHUNKS} most-recently
   * used chunks' {@link MappedByteBuffer} instances.
   * </p>
   * 
   * @since 1.74.4
   */
  protected LRUMap<Integer, MappedByteBuffer> buffers;
  
  /**
   * <p>
   * Makes a new list, initially empty, backed by a freshly created temporary
   * file.
   * </p>
   * 
   * @throws FileNotFoundException
   *           If some error occurs while opening or creating
   *           the temporary file.
   * @throws IOException
   *           If the file once opened cannot be truncated to zero bytes.
   * @since 1.74.4
   * @see #FileBackedList(Iterator, File)
   * @see #createTempFile()
   */
  public FileBackedList()
      throws FileNotFoundException, IOException {
    this(Collections.<E>emptyIterator(),
         createTempFile());
    this.deleteFile = true;
  }

  /**
   * <p>
   * Makes a new list from the given collection, backed by a freshly created
   * temporary file.
   * </p>
   * 
   * @param coll
   *          A collection.
   * @throws FileNotFoundException
   *           If some error occurs while opening or creating
   *           the temporary file.
   * @throws IOException
   *           If the file once opened cannot be truncated to zero bytes.
   * @since 1.74.4
   * @see #FileBackedList(Iterator, File)
   * @see #createTempFile()
   */
  public FileBackedList(Collection<E> coll)
      throws FileNotFoundException, IOException {
    this(coll.iterator(),
         createTempFile());
    this.deleteFile = true;
  }

  /**
   * <p>
   * Makes a new list from the given collection, backed by the given file.
   * </p>
   * 
   * @param coll
   *          A collection.
   * @param file
   *          The file backing the list. <b>If the file already exists, the file
   *          will be overwritten with an empty file.</b>
   * @throws FileNotFoundException
   *           If the given file object does not denote an existing, writable
   *           regular file and a new regular file of that name cannot be
   *           created, or if some other error occurs while opening or creating
   *           the file.
   * @throws IOException
   *           If the given file once opened cannot be truncated to zero bytes,
   *           or if an error occurs during memory mapping of the file.
   * @since 1.74.4
   * @see #FileBackedList(Iterator, File)
   */
  public FileBackedList(Collection<E> coll,
                        File file) 
      throws FileNotFoundException, IOException {
    this(coll.iterator(),
         file);
  }
  
  /**
   * <p>
   * Makes a new list from the given collection, backed by the file with the
   * given name.
   * </p>
   * 
   * @param coll
   *          A collection.
   * @param name
   *          The name of the file backing the list. <b>If the file already
   *          exists, the file will be overwritten with an empty file.</b>
   * @throws FileNotFoundException
   *           If the given file object does not denote an existing, writable
   *           regular file and a new regular file of that name cannot be
   *           created, or if some other error occurs while opening or creating
   *           the file.
   * @throws IOException
   *           If the given file once opened cannot be truncated to zero bytes,
   *           or if an error occurs during memory mapping of the file.
   * @since 1.74.4
   * @see #FileBackedList(Iterator, File)
   */
  public FileBackedList(Collection<E> coll,
                        String name)
      throws FileNotFoundException, IOException {
    this(coll.iterator(),
         new File(name));
  }

  /**
   * <p>
   * Makes a new list, initially empty, backed by the given file.
   * </p>
   * 
   * @param file
   *          The file backing the list. <b>If the file already exists, the file
   *          will be overwritten with an empty file.</b>
   * @throws FileNotFoundException
   *           If the given file object does not denote an existing, writable
   *           regular file and a new regular file of that name cannot be
   *           created, or if some other error occurs while opening or creating
   *           the file.
   * @throws IOException
   *           If the given file once opened cannot be truncated to zero bytes,
   *           or if an error occurs during memory mapping of the file.
   * @since 1.74.4
   * @see #FileBackedList(Iterator, File)
   */
  public FileBackedList(File file)
      throws FileNotFoundException, IOException {
    this(Collections.<E>emptyIterator(),
         file);
  }
  
  /**
   * <p>
   * Makes a new list from the given iterator, backed by a freshly created
   * temporary file.
   * </p>
   * 
   * @param iterator
   *          An iterator.
   * @throws FileNotFoundException
   *           If some error occurs while opening or creating
   *           the temporary file.
   * @throws IOException
   *           If the file once opened cannot be truncated to zero bytes.
   * @since 1.74.4
   * @see #FileBackedList(Iterator, File)
   * @see #createTempFile()
   */
  public FileBackedList(Iterator<E> iterator)
      throws FileNotFoundException, IOException {
    this(iterator,
         createTempFile());
    this.deleteFile = true;
  }
  
  /**
   * <p>
   * Makes a new list from the given iterator, backed by the given file.
   * </p>
   * 
   * @param iterator
   *          An iterator.
   * @param file
   *          The file backing the list. <b>If the file already exists, the file
   *          will be overwritten with an empty file.</b>
   * @throws FileNotFoundException
   *           If the given file object does not denote an existing, writable
   *           regular file and a new regular file of that name cannot be
   *           created, or if some other error occurs while opening or creating
   *           the file.
   * @throws IOException
   *           If the given file once opened cannot be truncated to zero bytes,
   *           or if an error occurs during memory mapping of the file.
   * @since 1.74.4
   * @see CountingRandomAccessFile#CountingRandomAccessFile(File, String, boolean)
   * @see FileChannel#map(MapMode, long, long)
   */
  public FileBackedList(Iterator<E> iterator,
                        File file)
      throws FileNotFoundException, IOException {
    this.closed = false;
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
    this.buffers = new UnmapLRUMap(CHUNKS);
    buffers.put(0, chan.map(MapMode.READ_WRITE, 0L, CHUNK));
    populate(iterator);
  }
  
  /**
   * <p>
   * Makes a new list from the given iterator, backed by the file with the
   * given name.
   * </p>
   * 
   * @param iterator
   *          An iterator.
   * @param name
   *          The name of the file backing the list. <b>If the file already
   *          exists, the file will be overwritten with an empty file.</b>
   * @throws FileNotFoundException
   *           If the given file object does not denote an existing, writable
   *           regular file and a new regular file of that name cannot be
   *           created, or if some other error occurs while opening or creating
   *           the file.
   * @throws IOException
   *           If the given file once opened cannot be truncated to zero bytes,
   *           or if an error occurs during memory mapping of the file.
   * @since 1.74.4
   * @see #FileBackedList(Iterator, File)
   */
  public FileBackedList(Iterator<E> iterator,
                        String name)
      throws FileNotFoundException, IOException {
    this(iterator,
         new File(name));
  }

  /**
   * <p>
   * Makes a new list, initially empty, backed by the file with the given name.
   * </p>
   * 
   * @param name
   *          The name of the file backing the list. <b>If the file already
   *          exists, the file will be overwritten with an empty file.</b>
   * @throws FileNotFoundException
   *           If the given file object does not denote an existing, writable
   *           regular file and a new regular file of that name cannot be
   *           created, or if some other error occurs while opening or creating
   *           the file.
   * @throws IOException
   *           If the given file once opened cannot be truncated to zero bytes,
   *           or if an error occurs during memory mapping of the file.
   * @since 1.74.4
   * @see #FileBackedList(Iterator, File)
   */
  public FileBackedList(String name)
      throws FileNotFoundException, IOException {
    this(Collections.<E>emptyIterator(),
         new File(name));
  }
  
  @Override
  public void add(int index, E element) throws RuntimeException {
    if (index < 0 || index > size()) {
      throw new IndexOutOfBoundsException(Integer.toString(index));
    }
    try {
      offsets.add(index, append(element));
      if (size() > OFFSETS && fileBackedLongList == null) {
        flushOffsetsToDisk();
      }
    }
    catch (IOException exc) {
      throw new RuntimeException(exc);
    }
  }
  
  /**
   * <p>
   * Releases all resources associated with this list; using the list after
   * closing it results in unspecified error conditions.
   * </p>
   * 
   * @since 1.74.4
   */
  @Override
  public synchronized void close() {
    if (closed) {
      return;
    }
    closed = true;
    try {
      if (buffers != null) {
	force();
	for (MappedByteBuffer mbbuf : buffers.values()) {
	  CountingRandomAccessFile.unmap(mbbuf);
	}
	buffers.clear();
	buffers = null;
      }
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
	getLongsFile().delete();
      }
      offsets = null;
      if (deleteFile) {
	file.delete();
	deleteFile = false;
      }
      file = null;
    } catch (Exception e) {
      log.error("close() threw", e);
    }
  }
  
  /**
   * <p>
   * Forces (flushes) all live memory-mapped buffers.
   * </p>
   * 
   * @since 1.74.4
   * @see #buffers
   * @see MappedByteBuffer#force()
   */
  public void force() {
    for (MappedByteBuffer mbbuf : buffers.values()) {
      mbbuf.force();
    }
    if (fileBackedLongList != null) {
      fileBackedLongList.force();
    }
  }
  
  @Override
  public E get(int index) throws RuntimeException {
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
      throw new RuntimeException(exc);
    }
  }
  
  @Override
  public E remove(int index) {
    E ret = get(index); // does the bounds check
    offsets.removeElementAt(index);
    return ret;
  }
  
  @Override
  public E set(int index, E element) throws RuntimeException {
    E ret = get(index); // does the bounds check
    try {
      offsets.set(index, append(element));
      return ret;
    }
    catch (IOException exc) {
      throw new RuntimeException(exc);
    }
  }

  @Override
  public int size() {
    return offsets.size();
  }

  /**
   * <p>
   * Appends a new element to the end of the list and returns the offset where
   * the appended bytes begin.
   * </p>
   * 
   * @param element
   *          An element.
   * @return The offset where the appended element begins.
   * @throws IOException
   *           If an error occurs in memory mapping.
   * @since 1.74.4
   */
  protected long append(E element) throws IOException {
    byte[] bytes = toBytes(element);
    int chunkNum = chunks.size() - 1;
    Chunk lastChunk = chunks.get(chunkNum);
    long ret = lastChunk.endOffset; // return value is current end of the file
    // Allocate new chunk if necessary
    if (lastChunk.endOffset - lastChunk.beginOffset + bytes.length + 4 > CHUNK) {
      lastChunk = new Chunk(lastChunk.endOffset, lastChunk.endOffset);
      chunks.add(lastChunk);
      ++chunkNum;
    }
    // Append bytes to last chunk
    MappedByteBuffer mbbuf = getBufferByChunkNumber(chunkNum);
    mbbuf.position((int)(lastChunk.endOffset - lastChunk.beginOffset));
    mbbuf.putInt(bytes.length);
    mbbuf.put(bytes);
    lastChunk.endOffset += 4 + bytes.length;
    return ret;
  }

  @Override
  protected void finalize() throws Throwable {
    try {
      close();
    }
    finally {
      super.finalize();
    }
  }

  /**
   * <p>
   * Flushes the list of offsets from {@link ArrayLongList} to
   * {@link FileBackedLongList} (used in testing).
   * </p>
   * 
   * @throws IOException
   *           if there is an I/O error creating the on-disk list of longs.
   * @since 1.74.7
   */
  protected void flushOffsetsToDisk() throws IOException {
    if (fileBackedLongList == null) {
      // Starting to get too large for main memory; go to disk also
      fileBackedLongList = new FileBackedLongList(getLongsFile());
      for (LongIterator iter = arrayLongList.iterator() ; iter.hasNext() ; ) {
        fileBackedLongList.add(iter.next());
      }
      arrayLongList.clear(); // see Commons Primitives 1.0 note in constructor
      arrayLongList.trimToSize();
      arrayLongList = null;
      offsets = fileBackedLongList;
    }
  }
  
  /**
   * <p>
   * Returns the given chunk's {@link MappedByteBuffer}, possibly memory mapping
   * it and evicting a lesser-used one.
   * </p>
   * 
   * @param chunkNum
   *          A chunk number.
   * @return A ready {@link MappedByteBuffer} instance.
   * @throws IOException
   *           If an error occurs while allocating a memory-mapped buffer.
   * @since 1.74.4
   */
  protected MappedByteBuffer getBufferByChunkNumber(int chunkNum) throws IOException {
    MappedByteBuffer ret = buffers.get(chunkNum);
    if (ret == null) {
      ret = chan.map(MapMode.READ_WRITE, chunks.get(chunkNum).beginOffset, CHUNK);
      buffers.put(chunkNum, ret);
    }
    return ret;
  }

  /**
   * <p>
   * Translates a file offset into a chunk number.
   * </p>
   * 
   * @param offset
   *          A file offset.
   * @return A chunk number such that the offset is in the designated chunk.
   * @since 1.74.4
   * @see #chunks
   */
  protected int getChunkNumberByOffset(long offset) {
    // Binary search on the chunks
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

  protected File getLongsFile() {
    return new File(file.getPath() + ".longs");
  }
  
  protected void populate(Iterator<E> iterator) throws IOException {
    while (iterator.hasNext()) {
      offsets.add(append(iterator.next()));
    }
  }
  
  /**
   * <p>
   * The length of a chunk's {@link MappedByteBuffer}.
   * </p>
   * 
   * @since 1.74.4
   */
  protected static final int CHUNK = 16 * 1024 * 1024; // 16MB
  
  /**
   * <p>
   * The maximum number of chunks that can be kept live in the cache.
   * </p>
   * 
   * @since 1.74.4
   * @see #buffers
   */
  protected static final int CHUNKS = 4;
  
  /**
   * <p>
   * The number of list items beyond which the internal list of {@code long}
   * offsets is streamed to disk using a {@link FileBackedLongList}.
   * </p>
   * 
   * @since 1.74.4
   */
  protected static final int OFFSETS = 500_000;
  
  protected static File createTempFile() throws IOException {
    File tempFile = FileUtil.createTempFile(FileBackedList.class.getSimpleName(), ".bin");
    return tempFile;
  }
  
  protected static Object fromBytes(byte[] bytes) throws RuntimeException {
    try {
      ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
      ObjectInputStream ois = new ObjectInputStream(bais);
      return ois.readObject();
    }
    catch (ClassNotFoundException | IOException exc) {
      throw new RuntimeException(exc);
    }
  }
  
  protected static byte[] toBytes(Object obj) throws RuntimeException {
    try {
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      ObjectOutputStream oos = new ObjectOutputStream(baos);
      oos.writeObject(obj);
      return baos.toByteArray();
    }
    catch (IOException exc) {
      throw new RuntimeException(exc);
    }
  }
  
}
