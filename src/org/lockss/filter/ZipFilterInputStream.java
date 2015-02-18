/*
 * $Id$
 */

/*

Copyright (c) 2000-2014 Board of Trustees of Leland Stanford Jr. University,
all rights reserved.

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in
all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.  IN NO EVENT SHALL
STANFORD UNIVERSITY BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR
IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.

Except as contained in this notice, the name of Stanford University shall not
be used in advertising or otherwise to promote the sale, use or other dealings
in this Software without prior written authorization from Stanford University.

*/

package org.lockss.filter;

import java.io.*;
import java.util.*;
import java.util.zip.*;

import org.apache.commons.io.input.CloseShieldInputStream;
import org.lockss.util.Constants;

/**
 * <p>
 * Processes a {@link ZipInputStream} entry by entry but only return unzipped
 * bytes from those entries designated by a predicate implemented by a concrete
 * subclass ({@link #keepZipEntry(ZipEntry, String)}).
 * </p>
 * <p>
 * The returned bytes are not a valid Zip stream. For each Zip entry that is
 * retained by the concrete subclass, first the name of the entry is output
 * (normalized not to begin with <code>./</code>) as UTF-8 bytes, then the
 * unzipped bytes of the entry are output (possibly zero if the entry is a
 * directory entry or if the entry is for an empty file). The entries are
 * enumerated in the same order as internally to the Zip file.
 * </p>
 * 
 * @author Thib Guicherd-Callin
 * @since 1.67
 * @see #keepZipEntry(ZipEntry, String)
 * @see #skipFiles(ZipInputStream, String...)
 */
public abstract class ZipFilterInputStream extends InputStream {

  /**
   * <p>
   * Current entry input stream. 
   * </p>
   * 
   * @since 1.67
   */
  private InputStream currentInputStream;
  
  /**
   * <p>
   * Underlying Zip input stream (set to null when this stream has been closed).
   * </p>
   * 
   * @since 1.67
   */
  private ZipInputStream zipInputStream;
  
  /**
   * <p>
   * Flag for when end of file has been reached on the underlying Zip input
   * stream.
   * </p>
   * 
   * @since 1.67
   */
  private boolean eof;

  /**
   * <p>
   * Makes a new Zip filter input stream from the given Zip input stream.
   * </p>
   * 
   * @param zipInputStream
   *          A Zip input stream.
   * @since 1.67
   */
  public ZipFilterInputStream(ZipInputStream zipInputStream) {
    this.zipInputStream = zipInputStream;
    this.currentInputStream = null;
    this.eof = false;
  }

  /**
   * <p>
   * Makes a new Zip filter input stream from the given input stream interpreted
   * as a Zip input stream).
   * </p>
   * 
   * @param inputStream
   *          An input stream (interpreted as a Zip input stream).
   * @since 1.67
   * @see #ZipFilterInputStream(ZipInputStream)
   */
  public ZipFilterInputStream(InputStream inputStream) {
    this(new ZipInputStream(inputStream));
  }
  
  @Override
  public int read(byte[] buf, int off, int len) throws IOException {
    ensureInput();
    if (eof) {
      return -1;
    }
    int charsRequested = Math.min(len, buf.length - off);
    int charsProcessed = 0;
    int charsRead = -1;
    do {
      charsRead = currentInputStream.read(buf, off + charsProcessed, len - charsProcessed);
      if (charsRead == -1) {
        currentInputStream = null;
      }
      else {
        charsProcessed += charsRead;
      }
    } while (charsRead != -1 && charsProcessed < charsRequested);
    return charsProcessed;
  }
  
  @Override
  public int read() throws IOException {
    ensureInput();
    if (eof) {
      return -1;
    }
    int ret = currentInputStream.read();
    if (ret == -1) {
      currentInputStream = null;
      ensureInput();
      if (eof) {
        return -1;
      }
      ret = currentInputStream.read(); // never EOF (first letter of file name) 
    }
    return ret;
  }

  /**
   * <p>
   * Before reading bytes, ensure that the stream has not been closed, that
   * end of file has not been reached on the underlying Zip input stream, and
   * that the previous read did not exhaust an entry; if so, it opens the next
   * entry's input stream and prepends it with the entry's normalized named.
   * </p>
   * 
   * @throws IOException
   */
  private void ensureInput() throws IOException {
    if (zipInputStream == null) {
      throw new IOException("stream closed");
    }
    while (!eof && currentInputStream == null) {
      ZipEntry ze = zipInputStream.getNextEntry();
      if (ze == null) {
        eof = true;
        return;
      }
      String zipEntryName = ze.getName();
      String normalizedZipEntryName = zipEntryName;
      if (normalizedZipEntryName.startsWith("./")) {
        normalizedZipEntryName = normalizedZipEntryName.substring(2);
      }
      if (keepZipEntry(ze, normalizedZipEntryName)) {
        currentInputStream = new SequenceInputStream(new ByteArrayInputStream(normalizedZipEntryName.getBytes(Constants.ENCODING_UTF_8)),
                                                     new CloseShieldInputStream(zipInputStream));
      }
    }
  }
  
  @Override
  public void close() throws IOException {
    if (zipInputStream == null) {
      throw new IOException("stream closed");
    }
    try {
      zipInputStream.close();
    }
    finally {
      zipInputStream = null;
    }
  }
  
  /**
   * <p>
   * Determines whether to keep or skip an entry from the underlying Zip input
   * stream.
   * </p>
   * 
   * @param zipEntry
   *          The Zip entry under consideration.
   * @param normalizedZipEntryName
   *          The Zip entry's normalized name (without any leading
   *          <code>./</code>). The {@link ZipEntry} argument's
   *          {@link ZipEntry#getName()} method can be used to retrieve the
   *          entry's original name, if it matters.
   * @return True if and only if the given entry should be kept as part of this
   *         stream's output bytes.
   * @since 1.67
   */
  public abstract boolean keepZipEntry(ZipEntry zipEntry,
                                       String normalizedZipEntryName);
  
  /**
   * <p>
   * Convenience method to create a Zip filter input stream from the given Zip
   * input stream that will skip all entries with the given (normalized) names.
   * </p>
   * 
   * @param zipInputStream
   *          A Zip input stream.
   * @param normalizedZipEntryNames
   *          Zero or more normalized (meaning, not with a leading
   *          <code>./</code>) entry names that are to be skipped.
   * @return A Zip filter input stream that skips entries with the given names.
   * @since 1.67
   */
  public static ZipFilterInputStream skipFiles(ZipInputStream zipInputStream,
                                               String... normalizedZipEntryNames) {
    final Set<String> set = new HashSet<String>(Arrays.asList(normalizedZipEntryNames));
    return new ZipFilterInputStream(zipInputStream) {
      @Override
      public boolean keepZipEntry(ZipEntry zipEntry, String normalizedZipEntryName) {
        return !set.contains(normalizedZipEntryName);
      }
    };
  }
  
  /**
   * <p>
   * Convenience method to create a Zip filter input stream from the given input
   * stream (interpreted as a Zip input stream) that will skip all entries with
   * the given (normalized) names.
   * </p>
   * 
   * @param inputStream
   *          An input stream (interpreted as a Zip input stream).
   * @param normalizedZipEntryNames
   *          Zero or more normalized (meaning, not with a leading
   *          <code>./</code>) entry names that are to be skipped.
   * @return A Zip filter input stream that skips entries with the given names.
   * @since 1.67
   * @see #skipFiles(ZipInputStream, String...)
   */
  public static ZipFilterInputStream skipFiles(InputStream inputStream,
                                               String... normalizedZipEntryNames) {
    return skipFiles(new ZipInputStream(inputStream), normalizedZipEntryNames);
  }
  
}
