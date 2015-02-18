/*
 * $Id$
 */

/*

Copyright (c) 2000-2003 Board of Trustees of Leland Stanford Jr. University,
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
import org.lockss.util.*;

public class TestRandomInputStream extends LockssTestCase {

  public static void assertOffsetInputStreamProducesBytes(byte[] expected,
							 InputStream in,
							 int chunkLen)
      throws IOException {
    int expectedLen = expected.length;
    byte[] ba = new byte[expectedLen];
    int off = 0;
    int n;
    while ((n = in.read(ba, off, Math.min(chunkLen, ba.length - off)))
	   > 0) {
      if (n > Math.min(chunkLen, ba.length - off)) {
	fail("ret value: " + n + " greater than length arg: " +
	     Math.min(chunkLen, ba.length - off));
      }
      off += n;
    }
    if (off != expectedLen) fail("Stream wrong length, expected " +
				 expectedLen + ", was " + off);
    assertEquals(expected, ba);
  }

  public void testRead() throws IOException {
    long s1 = 42;
    long s2 = 1234567890123456L;
    byte[] exp1 = ByteArray.makeRandomBytes(20, new LockssRandom(s1));
    byte[] exp2 = ByteArray.makeRandomBytes(12345, new LockssRandom(s2));

    assertOffsetInputStreamProducesBytes(exp1, new RandomInputStream(s1), 1);
    assertOffsetInputStreamProducesBytes(exp1, new RandomInputStream(s1), 2);
    assertOffsetInputStreamProducesBytes(exp1, new RandomInputStream(s1), 8);
    assertOffsetInputStreamProducesBytes(exp1, new RandomInputStream(s1), 50);
    assertOffsetInputStreamProducesBytes(exp2, new RandomInputStream(s2), 1);
    assertOffsetInputStreamProducesBytes(exp2, new RandomInputStream(s2), 10);
    assertOffsetInputStreamProducesBytes(exp2, new RandomInputStream(s2), 1000);
    assertOffsetInputStreamProducesBytes(exp2, new RandomInputStream(s2),
					 100000);
  }
}
