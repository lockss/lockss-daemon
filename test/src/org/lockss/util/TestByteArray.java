/*
 * $Id: TestByteArray.java,v 1.3 2003-06-20 22:34:56 claire Exp $
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

package org.lockss.util;
import org.lockss.test.*;

/**
 * Test class for org.lockss.util.ByteArray
 *
 */

public class TestByteArray extends LockssTestCase {
  public static Class testedClasses[] = {
    org.lockss.util.ByteArray.class
  };

  public TestByteArray(String msg) {
    super(msg);
  }

  public void testConcat() {
    String s1 = "hi there";
    String s2 = "I am an apteryx";
    byte c[] = ByteArray.concat(s1.getBytes(), s2.getBytes());
    assertEquals(s1.concat(s2), new String(c));
  }

  public void testToHexString() {
    byte t1[] = {1, 0x42, 0x7e, (byte)0xff};
    byte t2[] = {(byte)255, (byte)255, (byte)255, (byte)255};
    assertEquals("01427EFF", ByteArray.toHexString(t1));
    assertEquals("FFFFFFFF", ByteArray.toHexString(t2));
  }

  public void testEncode() {
    byte exp1[] = {0, 0, 1, 5};
    byte exp2[] = {42, 75, 1, (byte)255};
    byte tst[] = {(byte)255, (byte)255, (byte)255, (byte)255};
    ByteArray.encodeInt(256 + 5, tst, 0);
    assertEquals(exp1, tst);
    ByteArray.encodeInt(((42 * 256 + 75) * 256 + 1) * 256 + 255, tst, 0);
    assertEquals(exp2, tst);
  }

  public void testDecode() {
    byte tst[] = {0, 0, 1, 5,
		  42, 75, 1, (byte)255,
		  (byte)255, (byte)255, (byte)255, (byte)255};
    ByteArray.encodeInt(256 + 5, tst, 0);
    assertEquals(256 + 5, ByteArray.decodeInt(tst, 0));
    assertEquals(((42 * 256 + 75) * 256 + 1) * 256 + 255,
		 ByteArray.decodeInt(tst, 4));
    assertEquals(-1, ByteArray.decodeInt(tst, 8));
  }

  public void testEncodeDecode() {
    byte tst[] = new byte[8];
    ByteArray.encodeInt(12345679, tst, 0);
    ByteArray.encodeInt(-1, tst, 4);
    assertEquals(12345679, ByteArray.decodeInt(tst, 0));
    assertEquals(-1, ByteArray.decodeInt(tst, 4));
  }

  public void testEncodeLongSimple() {
    byte[] expectedBytes = {5};
    byte[] actualBytes = ByteArray.encodeLong(5);
    assertEquals(expectedBytes, actualBytes);
  }

  public void testEncodeLongMultiBytes() {
    byte[] expectedBytes = {1, 1};
    byte[] actualBytes = ByteArray.encodeLong(257);
    assertEquals(expectedBytes, actualBytes);
  }

  public void testDecodeLongSimple() {
    byte[] bytes = {7};
    assertEquals(7, ByteArray.decodeLong(bytes));
  }

  public void testDecodeLongMultiBytes() {
    byte[] bytes = {1, 5};
    assertEquals(261, ByteArray.decodeLong(bytes));
  }

  public void testDecodeEncodeLong() {
    long num = 109937;
    assertEquals(num, ByteArray.decodeLong(ByteArray.encodeLong(num)));
  }

  public void testEncodeDecodeLong() {
    byte[] bytes = {5, 99, 87};
    assertEquals(bytes, ByteArray.encodeLong(ByteArray.decodeLong(bytes)));
  }
}
