/*
 * $Id$
 */

/*

Copyright (c) 2000-2008 Board of Trustees of Leland Stanford Jr. University,
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

import java.util.*;
import java.io.*;
import org.lockss.util.*;

/** An InputStream that returns a sequence of pseudo-random bytes.
 * Guaranteed to produce the same sequence of bytes if the same seed is
 * used.  (For that reason, the only method called on the Random is
 * nextInt(), as nextBytes() is *not* repeatable unless exactly the same
 * buffers sizes are used. */
public class RandomInputStream extends InputStream {
  private LockssRandom rand;

  public RandomInputStream() {
    rand = new LockssRandom();
  }

  public RandomInputStream(long seed) {
    rand = new LockssRandom(seed);
  }

  private void storeBytes(byte[] buf, int off, int len) {
    for (int i = 0; i < len; i++) {
      buf[off + i] = (byte)rand.nextInt(0x100);
    }
  }

  public int read() {
    byte[] temp = new byte[1];
    storeBytes(temp, 0, 1);
    return temp[0];
  }

  public int read(byte[] buf, int off, int len) {
    storeBytes(buf, off, len);
    return len;
  }

  public int read(byte[] buf) {
    storeBytes(buf, 0, buf.length);
    return buf.length;
  }

  public long skip(long bytesToSkip) {
    return bytesToSkip;
  }
}

