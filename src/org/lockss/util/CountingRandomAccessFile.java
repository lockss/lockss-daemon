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
import java.lang.reflect.Method;
import java.nio.MappedByteBuffer;
import java.security.*;

/**
 * <p>
 * A specialization of the standard {@link RandomAccessFile} that caches the
 * file pointer and file length, which affords a slight performance improvement.
 * If the file is resized externally, the results are unspecified.
 * </p>
 *
 * @since 1.74.4
 * @see RandomAccessFile
 * @see #getFilePointer()
 * @see #length()
 */
public class CountingRandomAccessFile extends RandomAccessFile {

  /**
   * <p>
   * The {@link File} backing this {@link RandomAccessFile}.
   * </p>
   * 
   * @since 1.74.4
   */
  protected File file;

  /**
   * <p>
   * Whether the file backing this {@link RandomAccessFile} must be deleted when
   * the {@link RandomAccessFile} is garbage-collected or {@link #close()} is
   * called; should be false when the {@link RandomAccessFile} was instantiated
   * with a user-provided file and true when instantiated with a
   * constructed-provided temporary file.
   * </p>
   * 
   * @since 1.74.4
   * @see #file
   */
  protected boolean deleteFile;
  
  /**
   * <p>
   * The cached length of this {@link RandomAccessFile}.
   * </p>
   * 
   * @since 1.74.4
   */
  protected long length;

  /**
   * <p>
   * The cached file pointer of this {@link RandomAccessFile}.
   * </p>
   * 
   * @since 1.74.4
   */
  protected long pointer;
  
  /**
   * <p>
   * Makes a new {@link CountingRandomAccessFile}, backed by a freshly created temporary file,
   * with the default opening mode, in the default append mode.
   * </p>
   * 
   * @throws FileNotFoundException
   *           if the opening mode is {@link #MODE_READ_ONLY} but the given file
   *           object does not denote an existing regular file, or if the
   *           opening mode is not {@link #MODE_READ_ONLY} but the given file
   *           object does not denote an existing, writable regular file and a
   *           new regular file of that name cannot be created, or if some other
   *           error occurs while opening or creating the file.
   * @throws FileNotFoundException
   *           If some error occurs while opening or creating
   *           the temporary file.
   * @throws IOException
   *           If the file once opened cannot be truncated to zero bytes.
   * @since 1.74.4
   * @see #createTempFile()
   * @see #DEFAULT_MODE
   * @see #DEFAULT_APPEND
   */
  public CountingRandomAccessFile()
      throws IOException {
    this(createTempFile(),
         DEFAULT_MODE,
         DEFAULT_APPEND);
    this.deleteFile = true;
  }
  
  /**
   * <p>
   * Makes a new {@link CountingRandomAccessFile}, backed by the given file,
   * with the default opening mode, in the default append mode.
   * </p>
   * 
   * @param file
   *          The file backing this {@link CountingRandomAccessFile}.
   * @throws FileNotFoundException
   *           if the opening mode is {@link #MODE_READ_ONLY} but the given file
   *           object does not denote an existing regular file, or if the
   *           opening mode is not {@link #MODE_READ_ONLY} but the given file
   *           object does not denote an existing, writable regular file and a
   *           new regular file of that name cannot be created, or if some other
   *           error occurs while opening or creating the file.
   * @throws IOException
   *           If the given file once opened cannot be truncated to zero bytes
   *           in an opening mode other than {@link #MODE_READ_ONLY}.
   * @since 1.74.4
   * @see #DEFAULT_MODE
   * @see #DEFAULT_APPEND
   */
  public CountingRandomAccessFile(File file)
      throws FileNotFoundException, IOException {
    this(file,
         DEFAULT_MODE,
         DEFAULT_APPEND);
  }
  
  /**
   * <p>
   * Makes a new {@link CountingRandomAccessFile}, backed by the given file,
   * with the given opening mode, in the default append mode.
   * </p>
   * 
   * @param file
   *          The file backing this {@link CountingRandomAccessFile}.
   * @param mode
   *          The opening mode; one of {@link #MODE_READ_ONLY},
   *          {@link #MODE_READ_WRITE}, {@link #MODE_READ_WRITE_CONTENT} or
   *          {@link #MODE_READ_WRITE_CONTENT_METADATA}.
   * @throws FileNotFoundException
   *           if the opening mode is {@link #MODE_READ_ONLY} but the given file
   *           object does not denote an existing regular file, or if the
   *           opening mode is not {@link #MODE_READ_ONLY} but the given file
   *           object does not denote an existing, writable regular file and a
   *           new regular file of that name cannot be created, or if some other
   *           error occurs while opening or creating the file.
   * @throws IOException
   *           If the given file once opened cannot be truncated to zero bytes
   *           in an opening mode other than {@link #MODE_READ_ONLY}.
   * @since 1.74.4
   * @see #DEFAULT_APPEND
   */
  public CountingRandomAccessFile(File file,
                                  String mode)
      throws FileNotFoundException, IOException {
    this(file,
         mode,
         DEFAULT_APPEND);
  }

  /**
   * <p>
   * Makes a new {@link CountingRandomAccessFile}, backed by the given file,
   * with the given opening mode, in the given append mode.
   * </p>
   * 
   * @param file
   *          The file backing this {@link CountingRandomAccessFile}.
   * @param mode
   *          The opening mode; one of {@link #MODE_READ_ONLY},
   *          {@link #MODE_READ_WRITE}, {@link #MODE_READ_WRITE_CONTENT} or
   *          {@link #MODE_READ_WRITE_CONTENT_METADATA}.
   * @param append
   *          Whether to append to an existing file (in opening modes other than
   *          {@link #MODE_READ_ONLY}); if {@code false}, the file is
   *          overwritten with an empty file.
   * @throws FileNotFoundException
   *           if the opening mode is {@link #MODE_READ_ONLY} but the given file
   *           object does not denote an existing regular file, or if the
   *           opening mode is not {@link #MODE_READ_ONLY} but the given file
   *           object does not denote an existing, writable regular file and a
   *           new regular file of that name cannot be created, or if some other
   *           error occurs while opening or creating the file.
   * @throws IOException
   *           If the given file once opened cannot be truncated to zero bytes
   *           in an opening mode other than {@link #MODE_READ_ONLY}.
   * @since 1.74.4
   */
  public CountingRandomAccessFile(File file,
                                  String mode,
                                  boolean append)
      throws FileNotFoundException, IOException {
    super(file, mode);
    this.file = file;
    this.deleteFile = false; // reset by some constructors
    switch (mode) {
      case MODE_READ_ONLY:
        // Ignore append
        break;
      case MODE_READ_WRITE:
      case MODE_READ_WRITE_CONTENT:
      case MODE_READ_WRITE_CONTENT_METADATA:
        if (!append) {
          setLength(0L); // truncates and seeks in one
        }
        break;
      default:
        // Shouldn't happen (parent constructor)
        throw new IllegalArgumentException("Illegal mode: " + mode);
    }
    this.length = 0L;
    this.pointer = 0L;
  }
  
  /**
   * <p>
   * Makes a new {@link CountingRandomAccessFile}, backed by the file with the
   * given name, with the default opening mode, in the default append mode.
   * </p>
   * 
   * @param name
   *          The name of the file backing this
   *          {@link CountingRandomAccessFile}.
   * @throws FileNotFoundException
   *           if the opening mode is {@link #MODE_READ_ONLY} but the given file
   *           object does not denote an existing regular file, or if the
   *           opening mode is not {@link #MODE_READ_ONLY} but the given file
   *           object does not denote an existing, writable regular file and a
   *           new regular file of that name cannot be created, or if some other
   *           error occurs while opening or creating the file.
   * @throws IOException
   *           If the given file once opened cannot be truncated to zero bytes
   *           in an opening mode other than {@link #MODE_READ_ONLY}.
   * @since 1.74.4
   * @see #CountingRandomAccessFile(File, String, boolean)
   * @see #DEFAULT_MODE
   * @see #DEFAULT_APPEND
   */
  public CountingRandomAccessFile(String name)
      throws FileNotFoundException, IOException {
    this(new File(name),
         DEFAULT_MODE,
         DEFAULT_APPEND);
  }
  
  /**
   * <p>
   * Makes a new {@link CountingRandomAccessFile}, backed by the file with the
   * given name, with the given opening mode, in the default append mode.
   * </p>
   * 
   * @param name
   *          The name of the file backing this
   *          {@link CountingRandomAccessFile}.
   * @param mode
   *          The opening mode; one of {@link #MODE_READ_ONLY},
   *          {@link #MODE_READ_WRITE}, {@link #MODE_READ_WRITE_CONTENT} or
   *          {@link #MODE_READ_WRITE_CONTENT_METADATA}.
   * @throws FileNotFoundException
   *           if the opening mode is {@link #MODE_READ_ONLY} but the given file
   *           object does not denote an existing regular file, or if the
   *           opening mode is not {@link #MODE_READ_ONLY} but the given file
   *           object does not denote an existing, writable regular file and a
   *           new regular file of that name cannot be created, or if some other
   *           error occurs while opening or creating the file.
   * @throws IOException
   *           If the given file once opened cannot be truncated to zero bytes
   *           in an opening mode other than {@link #MODE_READ_ONLY}.
   * @since 1.74.4
   * @see #CountingRandomAccessFile(File, String, boolean)
   * @see #DEFAULT_APPEND
   */
  public CountingRandomAccessFile(String name,
                                  String mode)
      throws FileNotFoundException, IOException {
    this(new File(name),
         mode,
         DEFAULT_APPEND);
  }
  
  /**
   * <p>
   * Makes a new {@link CountingRandomAccessFile}, backed by the file with the
   * given name, with the given opening mode, in the given append mode.
   * </p>
   * 
   * @param name
   *          The name of the file backing this
   *          {@link CountingRandomAccessFile}.
   * @param mode
   *          The opening mode; one of {@link #MODE_READ_ONLY},
   *          {@link #MODE_READ_WRITE}, {@link #MODE_READ_WRITE_CONTENT} or
   *          {@link #MODE_READ_WRITE_CONTENT_METADATA}.
   * @param append
   *          Whether to append to an existing file (in opening modes other than
   *          {@link #MODE_READ_ONLY}); if {@code false}, the file is
   *          overwritten with an empty file.
   * @throws FileNotFoundException
   *           if the opening mode is {@link #MODE_READ_ONLY} but the given file
   *           object does not denote an existing regular file, or if the
   *           opening mode is not {@link #MODE_READ_ONLY} but the given file
   *           object does not denote an existing, writable regular file and a
   *           new regular file of that name cannot be created, or if some other
   *           error occurs while opening or creating the file.
   * @throws IOException
   *           If the given file once opened cannot be truncated to zero bytes
   *           in an opening mode other than {@link #MODE_READ_ONLY}.
   * @since 1.74.4
   * @see #CountingRandomAccessFile(File, String, boolean)
   */
  public CountingRandomAccessFile(String name,
                                  String mode,
                                  boolean append)
      throws FileNotFoundException, IOException {
    this(new File(name),
         mode,
         DEFAULT_APPEND);
  }

  @Override
  public void close() throws IOException {
    try {
      super.close();
    }
    finally {
      try {
        if (deleteFile) {
          file.delete();
        }
      }
      catch (Throwable thr) {
        // ignore
      }
    }
  }
  
  @Override
  public long getFilePointer() {
    return pointer;
  }
  
  @Override
  public long length() {
    return length;
  }
  
  @Override
  public int read() throws IOException {
    int ret = super.read();
    if (ret != -1) {
      ++pointer;
    }
    return ret;
  }
  
  @Override
  public int read(byte[] b) throws IOException {
    int ret = super.read(b);
    if (ret != -1) {
      pointer += ret;
    }
    return ret;
  }
  
  @Override
  public int read(byte[] b, int off, int len) throws IOException {
    int ret = super.read(b, off, len);
    if (ret != -1) {
      pointer += ret;
    }
    return ret;
  }

  @Override
  public void seek(long pos) throws IOException {
    if (pointer == pos) {
      return;
    }
    super.seek(pos);
    pointer = pos;
  }

  @Override
  public void setLength(long newLength) throws IOException {
    super.setLength(newLength);
    if (newLength < length) {
      if (newLength < pointer) {
        pointer = newLength;
      }
    }
    length = newLength;
  }

  @Override
  public int skipBytes(int n) throws IOException {
    int ret = super.skipBytes(n);
    pointer += ret;
    return ret;
  }

  @Override
  public void write(byte[] b) throws IOException {
    super.write(b);
    pointer += b.length;
    if (pointer > length) {
      length = pointer;
    }
  }

  @Override
  public void write(byte[] b, int off, int len) throws IOException {
    super.write(b, off, len);
    pointer += len;
    if (pointer > length) {
      length = pointer;
    }
  }

  @Override
  public void write(int b) throws IOException {
    super.write(b);
    ++pointer;
    if (pointer > length) {
      length = pointer;
    }
  }

  /**
   * <p>
   * The default append mode used by this class (@{value}).
   * </p>
   * 
   * @since 1.74.4
   * @see #CountingRandomAccessFile()
   * @see #CountingRandomAccessFile(File)
   * @see #CountingRandomAccessFile(String)
   * @see #CountingRandomAccessFile(File, String)
   * @see #CountingRandomAccessFile(String, String)
   */
  public static final boolean DEFAULT_APPEND = true;

  /**
   * <p>
   * The read-only mode of {@link RandomAccessFile} ({@value}).
   * </p>
   * 
   * @since 1.74.4
   */
  public static final String MODE_READ_ONLY = "r";

  /**
   * <p>
   * The read-write mode of {@link RandomAccessFile} ({@value}).
   * </p>
   * 
   * @since 1.74.4
   */
  public static final String MODE_READ_WRITE = "rw";

  /**
   * <p>
   * The read-write with synchronous content mode of {@link RandomAccessFile}
   * ({@value}).
   * </p>
   * 
   * @since 1.74.4
   */
  public static final String MODE_READ_WRITE_CONTENT = "rwd";

  /**
   * <p>
   * The read-write with synchronous content and metadata mode of
   * {@link RandomAccessFile} ({@value}).
   * </p>
   * 
   * @since 1.74.4
   */
  public static final String MODE_READ_WRITE_CONTENT_METADATA = "rws";

  /**
   * <p>
   * The default {@link RandomAccessFile} mode used by this class ({@value}).
   * </p>
   * 
   * @since 1.74.4
   * @see #MODE_READ_WRITE
   * @see #CountingRandomAccessFile()
   * @see #CountingRandomAccessFile(File)
   * @see #CountingRandomAccessFile(String)
   */
  public static final String DEFAULT_MODE = MODE_READ_WRITE;
  
  /**
   * <p>
   * Creates a temporary file.
   * </p>
   * 
   * @return A freshly created temporary file.
   * @throws IOException
   *           If a file could not be created.
   * @since 1.74.4
   * @see File#createTempFile(String, String)
   */
  protected static File createTempFile() throws IOException {
    File ret = File.createTempFile(CountingRandomAccessFile.class.getSimpleName(), ".bin");
    ret.deleteOnExit();
    return ret;
  }
  
  /**
   * @see https://www.programcreek.com/java-api-examples/index.php?source_dir=cp-common-utils-master/core/main/src/com/complexible/common/io/MMapUtil.java
   */
  protected static final boolean unmapSupported;
  
  static {
    boolean ret;
    try {
        Class.forName("sun.misc.Cleaner"); 
        Class.forName("java.nio.DirectByteBuffer").getMethod("cleaner"); 
        ret = true; 
    } 
    catch (Exception e) { 
        ret = false;
    }
    unmapSupported = ret;
  }
  
  /**
   * 
   * @param mbbuf
   * @return
   * @see https://www.programcreek.com/java-api-examples/index.php?source_dir=cp-common-utils-master/core/main/src/com/complexible/common/io/MMapUtil.java
   */
  public static boolean unmap(final MappedByteBuffer mbbuf) {
    if (unmapSupported) {
      try { 
        AccessController.doPrivileged(new PrivilegedExceptionAction<Object>() { 
          public Object run() throws Exception { 
            final Method getCleanerMethod = mbbuf.getClass().getMethod("cleaner"); 
            getCleanerMethod.setAccessible(true); 
            final Object cleaner = getCleanerMethod.invoke(mbbuf); 
            if (cleaner != null) { 
              cleaner.getClass().getMethod("clean").invoke(cleaner); 
            } 
            return null; 
          } 
        }); 
        return true; 
      } 
      catch (PrivilegedActionException exc) { 
        // ignore
      } 
    }
    return false;
  }

}
