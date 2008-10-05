/*
 * $Id: ZeroInputStream.java,v 1.2 2006-10-08 01:17:00 tlipkis Exp $
 */

/*

Copyright (c) 2000-2006 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.test;

import java.io.*;
import java.util.*;

/** An input stream that provides a source of zero (or some other value)
 * bytes */
public class ZeroInputStream extends InputStream {

  public static final int BUFSIZE = 4096;

  protected boolean closed = false;
  protected byte val = 0;
  protected byte[] srcbuf;
  protected long strmlen;

  /**
   * Creates a new ZeroInputStream that produces Long.MAX_VALUE bytes
   */
  public ZeroInputStream() {
    this(Long.MAX_VALUE);
  }

  /**
   * Creates a new ZeroInputStream.
   * @param len number of bytes to produce
   */
  public ZeroInputStream(long len) {
    this((byte)0, len);
  }

  /**
   * Creates a new ZeroInputStream.
   * @param val value of bytes in stream
   * @param len number of bytes to produce
   */
  public ZeroInputStream(byte val, long len) {
    if (len < 0) throw new IllegalArgumentException("len < 0");
    this.strmlen = len;
    this.val = val;
    srcbuf = new byte[BUFSIZE];
    Arrays.fill(srcbuf, val);
  }

  public void close() {
    closed = true;
  }

  public int read() throws IOException {
    if (closed) throw new IOException("Stream closed");
    if (strmlen <= 0) return -1;
    --strmlen;
    return val;
  }

  public int read(byte[] buf, int off, int len) throws IOException {
    int res = 0;
    if (closed) throw new IOException("Stream closed");
    for (int ix = off; strmlen > 0 && ix < off + len; ix += srcbuf.length) {
      int n = Math.min(off + len - ix, srcbuf.length);
      if (strmlen < n) n = (int)strmlen;
      System.arraycopy(srcbuf, 0, buf, ix, n);
      res += n;
      strmlen -= n;
    }
    return res == 0 ? -1 : res;
  }

  public int available() {
    return (int)Math.min(strmlen, Integer.MAX_VALUE);
  }
}
