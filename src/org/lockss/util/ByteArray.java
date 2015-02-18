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

import java.io.*;
import java.math.BigInteger;
import java.util.Comparator;

import org.mortbay.util.*;

/**
 * Byte array utilities
 */
public class ByteArray {
  public static final byte[] EMPTY_BYTE_ARRAY = new byte[]{};

  private static final char[] HEX_CHARS = {
    '0','1','2','3','4','5','6','7','8','9','A','B','C','D','E','F'
  };

  /**
   * A Comparator for lexicographical comparison of arrays of bytes,
   * with each byte considered unsigned.
   * The null byte array is less than any non-null byte array.
   * @see #lexicographicalCompare
   */
  public static final Comparator<byte[]> lexicographicalComparator =
    new Comparator<byte[]>() {
    public int compare(byte[] o1, byte[] o2) {
      if (o1 == o2) return 0;
      if (o1 == null) {
	return -1;
      } else if (o2 == null) {
	return 1;
      }
      return lexicographicalCompare(o1, o2);
    }
  };

  /**
   * Decode 1 byte at pos into an int (treating byte as unsigned)
   * @param b byte array
   * @param pos position in array
   */
  public static int decodeByte(byte[] b, int pos) {
    return (((int)b[pos]) & 0xFF);
  }

  /**
   * Insert the 4 byte big-endian encoding of an int at the specified
   * position in a byte array
   * @param n  int to encode
   * @param b byte array
   * @param pos starting position in array
   */
  public static void encodeInt(int n, byte[] b, int pos) {
    b[pos + 3] = (byte)(n & 0xFF);
    n >>>= 8;
    b[pos + 2] = (byte)(n & 0xFF);
    n >>>= 8;
    b[pos + 1] = (byte)(n & 0xFF);
    n >>>= 8;
    b[pos    ] = (byte)(n & 0xFF);
  }

  /**
   * Decode 4 byte at pos into an int
   * @param b byte array
   * @param pos starting position in array
   */
  public static int decodeInt(byte[] b, int pos) {
    return
      ((((int)b[pos    ]) & 0xFF) << 24) +
      ((((int)b[pos + 1]) & 0xFF) << 16) +
      ((((int)b[pos + 2]) & 0xFF) << 8) +
      (((int)b[pos + 3]) & 0xFF);
  }

  /**
   * Insert the 8 byte big-endian encoding of a long at the specified
   * position in a byte array
   * @param n  long to encode
   * @param b byte array
   * @param pos starting position in array
   */
  public static void encodeLong(long n, byte[] b, int pos) {
    for (int ix = 7; ix >= 0; ix--) {
      b[pos + ix] = (byte)(n & 0xFF);
      n >>>= 8;
    }
  }

  /**
   * Decode 8 bytes at pos into a long
   * @param b byte array
   * @param pos starting position in array
   */
  public static long decodeLong(byte[] b, int pos) {
    long res = 0;
    for (int ix = 0, shift = 56; ix < 8; ix++, shift -= 8) {
      res += (((long)b[pos + ix]) & 0xFF) << shift;
    }
    return res;
  }

  public static byte[] encodeLong(long n) {
    BigInteger bigI = new BigInteger(Long.toString(n));
    // note that this byte array has a sign bit, which should be removed
    // for optimization
    return bigI.toByteArray();
  }

  public static long decodeLong(byte[] b) {
    BigInteger bigI = new BigInteger(b);
    return bigI.longValue();
  }

  /** Return the concatenation of two byte arrays. */
  public static byte[] concat(byte[] a, byte[] b) {
    byte[] res = new byte[a.length + b.length];
    System.arraycopy(a, 0, res, 0, a.length);
    System.arraycopy(b, 0, res, a.length, b.length);
    return res;
  }

  /** Return a Base64 encoded representation of bytes in array */
  public static String toBase64(byte[] b) {
    return String.valueOf(B64Code.encode(b));
  }

  /** Return hex representation of bytes in array */
  public static String toHexString(byte[] b) {
    char[] buf = new char[b.length * 2];

    for (int ix = 0, bufx = 0; ix < b.length; ix++) {
      int tmp = b[ix];
      buf[bufx++] = HEX_CHARS[(tmp >>> 4) & 0x0F];
      buf[bufx++] = HEX_CHARS[tmp & 0x0F];
    }
    return new String(buf);
  }

  /** Parse hax string to byte array.  Result includes leading zeros - this
   * is intended to be used with MessageDigest results.
   * @param hex input string, length must be even.
   * @return byte array w/ numeric value of hex string
   */
  public static byte[] fromHexString(String hex) {
    return javax.xml.bind.DatatypeConverter.parseHexBinary(hex);
  }

  private static LockssRandom THE_RAND = new LockssRandom();

  /**
   * Return a pseudo-random array of bytes of length len.
   *
   * @param len The size of the array to return.
   * @return A pseudo-random array of bytes.
   */
  public static byte[] makeRandomBytes(int len) {
    return makeRandomBytes(len, THE_RAND);
  }

  /**
   * Return a pseudo-random array of bytes of length len.
   *
   * @param len The size of the array to return.
   * @param rand the LockssRandom to use
   * @return A pseudo-random array of bytes.
   */
  // Generate one byte per call to Random.nextInt() so this method will
  // generated the same sequence of random bytes as RandomInputStream.
  public static byte[] makeRandomBytes(int len, LockssRandom rand) {
    byte[] retVal = new byte[len];
    for (int i = 0; i < len; i++) {
      retVal[i] = (byte)rand.nextInt(0x100);
    }
    return retVal;
  }

  /**
   * Compare the two byte arrays lexicographically, with each byte
   * considered unsigned.
   *
   * @param left The "left" array.
   * @param right The "right" array.
   * @return a negative integer, zero, or a positive integer as the
   * first argument is less than, equal to, or greater than the
   * second.
   * @throws NullPointerException if either array is null.
   */
  public static int lexicographicalCompare(byte[] left, byte[] right) {
    for (int i = 0; i < left.length && i < right.length; i++) {
      // byte is signed; force each array entry to be a small int.
      int leftByte = (left[i] & 0xff);
      int rightByte = (right[i] & 0xff);
      if (leftByte != rightByte) {
	return leftByte-rightByte;
      }
    }
    return left.length - right.length;
  }
}
