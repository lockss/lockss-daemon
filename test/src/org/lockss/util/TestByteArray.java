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

  public void testToBase64() {
    byte t1[] = {1, 0x42, 0x7e, (byte)0xff};
    byte t2[] = {(byte)255, (byte)255, (byte)255, (byte)255};
    assertEquals("AUJ+/w==", ByteArray.toBase64(t1));
    assertEquals("/////w==", ByteArray.toBase64(t2));
  }

  public void testToHexString() {
    byte t1[] = {1, 0x42, 0x7e, (byte)0xff};
    byte t2[] = {(byte)255, (byte)255, (byte)255, (byte)255};
    assertEquals("01427EFF", ByteArray.toHexString(t1));
    assertEquals("FFFFFFFF", ByteArray.toHexString(t2));
  }

  public void testFromHexString() {
    byte t1[] = {1, 0x42, 0x7e, (byte)0xff};
    byte t2[] = {(byte)255, (byte)255, (byte)255, (byte)255};
    byte t3[] = {(byte)143, (byte)255, (byte)255, (byte)255};
    byte t4[] = {(byte)127, (byte)255, (byte)255, (byte)255};
    assertEquals(t1, ByteArray.fromHexString("01427EFF"));
    assertEquals(t2, ByteArray.fromHexString("FFFFFFFF"));
    assertEquals(t3, ByteArray.fromHexString("8FFFFFFF"));
    assertEquals(t4, ByteArray.fromHexString("7FFFFFFF"));

    try {
      ByteArray.fromHexString("123");
      fail("fromHexString should throw on odd-length input");
    } catch (IllegalArgumentException ignore) {
    }
  }

  public void testEncodeInt() {
    byte exp1[] = {0, 0, 1, 5};
    byte exp2[] = {42, 75, 1, (byte)255};
    byte tst[] = {(byte)255, (byte)255, (byte)255, (byte)255};
    ByteArray.encodeInt(256 + 5, tst, 0);
    assertEquals(exp1, tst);
    ByteArray.encodeInt(((42 * 256 + 75) * 256 + 1) * 256 + 255, tst, 0);
    assertEquals(exp2, tst);
  }

  public void testDecodeInt() {
    byte tst[] = {0, 0, 1, 5,
		  42, 75, 1, (byte)255,
		  (byte)255, (byte)255, (byte)255, (byte)255};
    ByteArray.encodeInt(256 + 5, tst, 0);
    assertEquals(256 + 5, ByteArray.decodeInt(tst, 0));
    assertEquals(((42 * 256 + 75) * 256 + 1) * 256 + 255,
		 ByteArray.decodeInt(tst, 4));
    assertEquals(-1, ByteArray.decodeInt(tst, 8));
  }

  public void testEncodeLong() {
    byte exp1[] = {1, 2, 3, 4, 5, 6, 7, 8, 0, 0};
    byte exp2[] = {1, 2, (byte)255, 0, 0, 1, 2, 3, 4, 5};
    byte tst[] = new byte[10];
    ByteArray.encodeLong(72623859790382856L, tst, 0);
    assertEquals(exp1, tst);
    ByteArray.encodeLong(-72057589709208571L, tst, 2);
    assertEquals(exp2, tst);
  }

  public void testDecodeLong() {
    byte tst[] = {(byte)255, 0, 0, 1, 2, 3, 4, 5, 6, 7, 8,
		  0, 0, 0, 0, 0, 0, 0, 0, 1, (byte)255};
    assertEquals(-72057589709208571L, ByteArray.decodeLong(tst, 0));
    assertEquals(1108152157446L, ByteArray.decodeLong(tst, 1));
    assertEquals(283686952306183L, ByteArray.decodeLong(tst, 2));
    assertEquals(72623859790382856L, ByteArray.decodeLong(tst, 3));
    assertEquals(0, ByteArray.decodeLong(tst, 11));
    assertEquals(511, ByteArray.decodeLong(tst, 13));
  }

  public void testEncodeDecode() {
    byte tst[] = new byte[8];
    ByteArray.encodeInt(12345679, tst, 0);
    ByteArray.encodeInt(-1, tst, 4);
    assertEquals(12345679, ByteArray.decodeInt(tst, 0));
    assertEquals(-1, ByteArray.decodeInt(tst, 4));
    long exp = 72623859790382857L;
    ByteArray.encodeLong(exp, tst, 0);
    assertEquals(exp, ByteArray.decodeLong(tst, 0));
  }

  public void testDecodeByte() {
    byte arr[] = {1, 2, (byte)0xff, (byte)0x80};
    assertEquals(1, ByteArray.decodeByte(arr, 0));
    assertEquals(2, ByteArray.decodeByte(arr, 1));
    assertEquals(0xff, ByteArray.decodeByte(arr, 2));
    assertEquals(0x80, ByteArray.decodeByte(arr, 3));
  }

  public void testEncodeLongSimple() {
    byte[] expectedBytes = {5};
    assertEquals(new byte[] {5}, ByteArray.encodeLong(5));
    assertEquals(new byte[] {1, 1}, ByteArray.encodeLong(257));
  }

  public void testDecodeLongSimple() {
    assertEquals(7, ByteArray.decodeLong(new byte[] {7}));
    assertEquals(261, ByteArray.decodeLong(new byte[] {1, 5}));
  }

  public void testDecodeEncodeLong() {
    long num = 109937;
    assertEquals(num, ByteArray.decodeLong(ByteArray.encodeLong(num)));
  }

  public void testEncodeDecodeLong() {
    byte[] bytes = {5, 99, 87};
    assertEquals(bytes, ByteArray.encodeLong(ByteArray.decodeLong(bytes)));
  }

  public void testMakeRandomBytes() {
    assertSuccessRate(.5, 10);
    boolean[] flgs = new boolean[256];
    for (int ix = 100; ix > 0; ix--) {
      for (int val : ByteArray.makeRandomBytes(10000)) {
	flgs[(val & 0xff)] = true;
      }
    }
    for (int ix = 0; ix < 256; ix++) {
      assertTrue("Missing " + ix, flgs[ix]);
    }
  }

  public void testLexicographicalCompareNull() {
    byte b[] = {1, 2};
    try {
      ByteArray.lexicographicalCompare(b, null);
      fail("lexicographicalCompare should throw NullPointerException");
    } catch (NullPointerException ignore) {
    }
    try {
      ByteArray.lexicographicalCompare(null, b);
      fail("lexicographicalCompare should throw NullPointerException");
    } catch (NullPointerException ignore) {
    }
  }

  public void testLexicographicalCompareEmpty() {
    byte empty[] = {};
    byte some[] = {1};
    assertEquals(0, ByteArray.lexicographicalCompare(empty, empty));
    assertTrue(ByteArray.lexicographicalCompare(empty, some) < 0);
    assertTrue(ByteArray.lexicographicalCompare(some, empty) > 0);
  }

  public void testLexicographicalCompareEqual() {
    byte empty1[] = {};
    byte empty2[] = {};
    byte some1[] = {1, 1, 1};
    byte some2[] = {1, 1, 1};
    assertEquals(0, ByteArray.lexicographicalCompare(empty1, empty2));
    assertEquals(0, ByteArray.lexicographicalCompare(some1, some2));
  }

  public void testLexicographicalCompareDiffLengths() {
    byte some[] = {1};
    byte more[] = {1, 1, 1};
    assertTrue(ByteArray.lexicographicalCompare(some, more) < 0);
    assertTrue(ByteArray.lexicographicalCompare(more, some) > 0);
  }

  public void testLexicographicalCompareDiffValues() {
    byte b1[] = {1};
    byte b2[] = {5};
    byte b3[] = {(byte)0xff};
    assertTrue(ByteArray.lexicographicalCompare(b1, b2) < 0);
    assertTrue(ByteArray.lexicographicalCompare(b2, b3) < 0);
    assertTrue(ByteArray.lexicographicalCompare(b2, b1) > 0);
    assertTrue(ByteArray.lexicographicalCompare(b3, b2) > 0);
  }

  public void testLexicographicalComparatorNull() {
    byte b[] = {1, 2};
    assertEquals(0, ByteArray.lexicographicalComparator.compare(null, null));
    assertTrue(ByteArray.lexicographicalComparator.compare(b, null) > 0);
    assertTrue(ByteArray.lexicographicalComparator.compare(null, b) < 0);
  }

  public void testLexicographicalComparatorEmpty() {
    byte empty[] = {};
    byte some[] = {1};
    assertTrue(ByteArray.lexicographicalComparator.compare(empty, some) < 0);
    assertTrue(ByteArray.lexicographicalComparator.compare(some, empty) > 0);
  }

  public void testLexicographicalComparatorEqual() {
    byte empty1[] = {};
    byte empty2[] = {};
    byte some1[] = {1, 1, 1};
    byte some2[] = {1, 1, 1};
    assertEquals(0, ByteArray.lexicographicalComparator.compare(empty1, empty2));
    assertEquals(0, ByteArray.lexicographicalComparator.compare(some1, some2));
  }

  public void testLexicographicalComparatorDiffLengths() {
    byte some[] = {1};
    byte more[] = {1, 1, 1};
    assertTrue(ByteArray.lexicographicalComparator.compare(some, more) < 0);
    assertTrue(ByteArray.lexicographicalComparator.compare(more, some) > 0);
  }

  public void testLexicographicalComparatorDiffValues() {
    byte b1[] = {1};
    byte b2[] = {5};
    byte b3[] = {(byte)0xff};
    assertTrue(ByteArray.lexicographicalComparator.compare(b1, b2) < 0);
    assertTrue(ByteArray.lexicographicalComparator.compare(b2, b3) < 0);
    assertTrue(ByteArray.lexicographicalComparator.compare(b2, b1) > 0);
    assertTrue(ByteArray.lexicographicalComparator.compare(b3, b2) > 0);
  }

}
