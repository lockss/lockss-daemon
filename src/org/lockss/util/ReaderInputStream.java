/*
 * $Id$
 */

/*

Copyright (c) 2000-2010 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.util;
import java.io.*;
import java.nio.charset.*;

// Parts adapted from org.apache.tools.ant.util.ReaderInputStream,
// Copyright 1999-2006 The Apache Software Foundation

/**
 * Wrapper to turn a Reader into an InputStream.  The previous version of
 * this class did not handle correctly encode Unicode characters; it was
 * used primarily for hash filtering, where the result must be a
 * reproducible byte sequence but needn't necessarily be exactly
 * reconstructible to the original character sequence.  This stream is now
 * used by link rewriters, which do have to produce a valid decodable
 * result, so it now handles character encoding.  But this version isn't
 * necessarily reproducible because some encodings aren't unique - the
 * exact sequence of bytes produced can depend on the number of bytes read
 * at a time.
 *
 * If called with no encoder, this version reproduces the previous
 * behavior, making it polling-compatible.
 */
public class ReaderInputStream extends InputStream {

   protected static Logger log = Logger.getLogger("ReaderInputStream");

  /** Source Reader */
  private Reader in;

  private String encoding = null;
  private Charset cset;
  private CharsetEncoder encoder;

  private byte[] slack;

  private int begin;

  /**
   * Construct a <code>ReaderInputStream</code> for the specified
   * <code>Reader</code>.  This version (no encoding specified) converts
   * characters to bytes by casting, to be consistent with previous
   * behavior.
   *
   * @param reader   <code>Reader</code>.  Must not be <code>null</code>.
   */
  public ReaderInputStream(Reader reader) {
    if (reader == null) {
      throw new IllegalArgumentException("reader must not be null");
    }
    in = reader;
  }

  /**
   * Construct a <code>ReaderInputStream</code> for the specified
   * <code>Reader</code>, with the specified encoding.
   *
   * @param reader     non-null <code>Reader</code>.
   * @param encoding   charset name
   */
  public ReaderInputStream(Reader reader, String encoding) {
    this(reader);
    this.encoding = encoding;
  }

  /**
   * Returns the next byte of the encoded character sequence
   */
  public synchronized int read() throws IOException {
    if (in == null) {
      throw new IOException("Stream Closed");
    }

    byte result;
    if (slack != null && begin < slack.length) {
      result = slack[begin];
      if (++begin == slack.length) {
	slack = null;
      }
    } else {
      byte[] buf = new byte[1];
      if (read(buf, 0, 1) <= 0) {
	return -1;
      } else {
	result = buf[0];
      }
    }

    return result & 0xFF;
  }

  /**
   * Reads from the <code>Reader</code> into a byte array
   *
   * @param b  the byte array to read into
   * @param off the offset in the byte array
   * @param len the length in the byte array to fill
   * @return the actual number read into the byte array, -1 at
   *         the end of the stream
   * @exception IOException if an error occurs
   */
  public synchronized int read(byte[] b, int off, int len)
      throws IOException {
    if (in == null) {
      throw new IOException("Stream Closed");
    }
    if (len == 0) {
      return 0;
    }
    if (encoding == null) {
      return castingRead(b, off, len);
    }
    while (slack == null) {
      char[] buf = new char[len]; // might read too much
      int n = in.read(buf);
      if (n == -1) {
	return -1;
      }
      if (n > 0) {
	slack = new String(buf, 0, n).getBytes(encoding);
	begin = 0;
      }
    }

    if (len > slack.length - begin) {
      len = slack.length - begin;
    }

    System.arraycopy(slack, begin, b, off, len);

    if ((begin += len) >= slack.length) {
      slack = null;
    }

    return len;
  }

  static final int DEFAULT_BUFFER_CAPACITY = 16384;
  private char[] charBuffer;

  // Old version simply casts.  Must still be used for hashing unless
  // change polling version
  private int castingRead(byte[] outputBuf, int off, int len)
      throws IOException {
    if (charBuffer == null) {
      charBuffer = new char[DEFAULT_BUFFER_CAPACITY];
    }
    if (len > DEFAULT_BUFFER_CAPACITY) {
      len = DEFAULT_BUFFER_CAPACITY;
    }
    int numRead = in.read(charBuffer, 0, len);
    for (int ix=0; ix<numRead; ix++) {
      outputBuf[off+ix] = (byte)charBuffer[ix];
    }
    return numRead;
  }

  /**
   * Marks the read limit of the StringReader.
   *
   * @param limit the maximum limit of bytes that can be read before the
   *              mark position becomes invalid
   */
  public synchronized void mark(final int limit) {
    try {
      in.mark(limit);
    } catch (IOException ioe) {
      throw new RuntimeException(ioe.getMessage());
    }
  }


  /**
   * @return   the current number of bytes ready for reading
   * @exception IOException if an error occurs
   */
  public synchronized int available() throws IOException {
    if (in == null) {
      throw new IOException("Stream Closed");
    }
    if (slack != null) {
      return slack.length - begin;
    }
    if (in.ready()) {
      return 1;
    } else {
      return 0;
    }
  }

  /**
   * @return false - mark is not supported
   */
  public boolean markSupported () {
    return false;   // would be imprecise
  }

  /**
   * Resets the StringReader.
   *
   * @exception IOException if the StringReader fails to be reset
   */
  public synchronized void reset() throws IOException {
    if (in == null) {
      throw new IOException("Stream Closed");
    }
    slack = null;
    in.reset();
  }

  /**
   * Closes the Stringreader.
   *
   * @exception IOException if the original StringReader fails to be closed
   */
  public synchronized void close() throws IOException {
    if (in != null) {
      in.close();
      slack = null;
      in = null;
    }
  }

  /** Return the underlying Reader */
  public Reader getReader() {
    return in;
  }

}
