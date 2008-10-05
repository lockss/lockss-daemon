/*
 * $Id: TestZeroInputStream.java,v 1.2 2006-10-08 01:17:00 tlipkis Exp $
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

public class TestZeroInputStream extends LockssTestCase {

  public void testRead() throws IOException {
    ZeroInputStream zis = new ZeroInputStream(3);
    assertEquals(0, zis.read());
    assertEquals(0, zis.read());
    assertEquals(0, zis.read());
    assertEquals(-1, zis.read());
    assertEquals(-1, zis.read());
    assertEquals(-1, zis.read());
  }

  public static void assertOffsetInputStringProducesByte(int expected,
							 int expectedLen,
							 InputStream in,
							 int chunkLen)
      throws IOException {
    byte[] ba = new byte[expectedLen];
    int off = 0;
    int n;
    while ((n = in.read(ba, off, Math.min(chunkLen, ba.length - off)))
	   != -1) {
      if (n > Math.min(chunkLen, ba.length - off)) {
	fail("ret value: " + n + " greater than length arg: " +
	     Math.min(chunkLen, ba.length - off));
      }
      off += n;
    }
    if (off != expectedLen) fail("Stream wrong length, expected " +
				 expectedLen + ", was " + off);
    if (n != -1) fail("Stream longer than " + expectedLen);
    for (int ix = 0; ix < off; ix++) {
      if (ba[ix] != 0) {
	fail("Byte " + off + " = " + ba[ix]);
      }
    }
  }

  public void testReadArr() throws IOException {
    assertOffsetInputStringProducesByte(0, 5, new ZeroInputStream(5), 1);
    assertOffsetInputStringProducesByte(0, 5, new ZeroInputStream(5), 3);
    assertOffsetInputStringProducesByte(0, 5, new ZeroInputStream(5), 5);
    assertOffsetInputStringProducesByte(0, 5, new ZeroInputStream(5), 7);
    assertOffsetInputStringProducesByte(0, 1000,
					new ZeroInputStream(1000), 127);
    assertOffsetInputStringProducesByte(0, 100000,
					new ZeroInputStream(100000), 127);
  }

  public void testAvailable() throws IOException {
    ZeroInputStream zis = new ZeroInputStream(3);
    assertEquals(3, zis.available());
    assertEquals(0, zis.read());
    assertEquals(2, zis.available());
    assertEquals(0, zis.read());
    assertEquals(1, zis.available());
    assertEquals(0, zis.read());
    assertEquals(0, zis.available());
    assertEquals(-1, zis.read());
    assertEquals(Integer.MAX_VALUE,
		 new ZeroInputStream(Integer.MAX_VALUE * 1000L).available());
  }
}
