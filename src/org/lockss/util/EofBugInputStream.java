/*
 * $Id: EofBugInputStream.java,v 1.2 2010-02-22 07:04:27 tlipkis Exp $
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
import java.util.*;

  /** Stream wrapper that doesn't call the underlying stream after it has
   * signalled EOF, to work around bugs in InputStream implementations.
   * (Heritrix ArchiveReader decrements the position pointer if read()
   * called at EOF, causing next read() call returns 1 byte.  HttpClient
   * sometimes automatically closes the input stream when it reaches EOF,
   * and if a BufferedInputStream is used to read from it, it might call
   * available(), etc. after the stream has been (automatically) closed,
   * causing an exception. */

  public class EofBugInputStream extends InputStream {
    private InputStream in;
    private boolean isEof = false;

    public EofBugInputStream(InputStream in) {
      this.in = in;
    }

    public int read() throws IOException {
      if (isEof) return -1;
      return check(in.read());
    }

    public int read(byte b[]) throws IOException {
      if (isEof) return -1;
      return check(read(b, 0, b.length));
    }

    public int read(byte b[], int off, int len) throws IOException {
      if (isEof) return -1;
      return check(in.read(b, off, len));
    }

    public long skip(long n) throws IOException {
      if (isEof) return 0;
      return check(in.skip(n));
    }

    public int available() throws IOException {
      if (isEof) return 0;
      return in.available();
    }

    public void close() throws IOException {
      in.close();
    }

    public synchronized void mark(int readlimit) {
      in.mark(readlimit);
    }

    public synchronized void reset() throws IOException {
      in.reset();
    }

    public boolean markSupported() {
      return in.markSupported();
    }

    int check(int n) {
      if (n < 0) {
 	isEof = true;
      }
      return n;
    }

    long check(long n) {
      if (n <= 0) {
 	isEof = true;
      }
      return n;
    }
  }
