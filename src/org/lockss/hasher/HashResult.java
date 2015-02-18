/*
 * $Id$
 */

/*

Copyright (c) 2013-2014 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.hasher;

import java.security.MessageDigest;
import java.util.*;

import org.lockss.util.*;

/**
 * Wrap the byte array returned by {@link MessageDigest#digest()}
 * to force equality and sorting by content of the byte array.
 */
final public class HashResult implements LockssSerializable {
  /** An internal byte array, never shared with clients. */
  private final byte[] bytes;
  private final String algorithm;

  private HashResult(byte[] bytes) {
    this.bytes = bytes;
    this.algorithm = null;
  }

  private HashResult(byte[] bytes, String algorithm) {
    this.bytes = bytes;
    this.algorithm = algorithm;
  }

  /**
   * Thrown by {@link #make} for byte arrays which are not permitted.
   */
  public static class IllegalByteArray extends IllegalArgumentException {
    public IllegalByteArray(String msg) {
      super(msg);
    }
  }

  /**
   * Wrap the given bytes.
   * @param bytes The bytes to wrap; probably the output from
   * MessageDigest.digest().
   * @return A HashResult for these bytes.
   * @throws {@link IllegalByteArray} if <code>bytes</code> is null or
   * zero-length.
   */
  public static HashResult make(byte[] bytes) {
    checkBytes(bytes);
    // Keep a safe copy, so someone mucking with the bytes won't break
    // our hash.
    return new HashResult(Arrays.copyOf(bytes, bytes.length));
  }

 /**
   * Wrap the given bytes.
   * @param bytes The bytes to wrap; probably the output from
   * MessageDigest.digest().
   * @param algorithm STring naming hash algorithm
   * @return A HashResult for these bytes.
   * @throws {@link IllegalByteArray} if <code>bytes</code> is null or
   * zero-length.
   */
  public static HashResult make(byte[] bytes, String algorithm) {
    checkBytes(bytes);
    // Keep a safe copy, so someone mucking with the bytes won't break
    // our hash.
    return new HashResult(Arrays.copyOf(bytes, bytes.length),
			  algorithm);
  }

  /**
   * Wrap the given bytes.
   * @param prop A String of the form alg:hex-bytes
   * @return A HashResult for these bytes.
   * @throws {@link IllegalByteArray} if <code>bytes</code> is null or
   * zero-length, or if the string doesn't parse.
   */
  public static HashResult make(String prop) {
    List<String> parts = StringUtil.breakAt(prop, ':');
    if (parts.size() != 2) {
      throw new IllegalByteArray(prop + " bad format");
    }
    String alg = parts.get(0);
    String hash = parts.get(1);
    if (StringUtil.isNullString(alg) || StringUtil.isNullString(hash)) {
      throw new IllegalByteArray(prop + " bad format");
    }
    try {
      byte[] bytes = ByteArray.fromHexString(parts.get(1));
      checkBytes(bytes);
      return new HashResult(bytes, alg);
    } catch (NumberFormatException ex) {
      throw new IllegalByteArray(hash + " not hex number");
    } catch (IllegalArgumentException ex) {
      throw new IllegalByteArray(hash + " not hex number");
    }
  }
  /**
   * @throws {@link IllegalByteArray} if <code>bytes</code> is null or
   * zero-length.
   */
  private static void checkBytes(byte[] bytes) {
    if (bytes == null || bytes.length == 0) {
      throw new IllegalByteArray("null or zero-length byte array is not allowed.");
    }
  }

  /** 
   * @return A copy of the underlying bytes. This will never be
   * <code>null</code> or zero-length.
   */
  public byte[] getBytes() {
    return Arrays.copyOf(bytes, bytes.length);
  }

  /**
   * @return The algorithm, which may be null
   */
  public String getAlgorithm() {
    return algorithm;
  }

  /** Compare the given bytes with ours.
   * <code>x.equals(bytes)</code> is a convenience method which
   * should behave the same as
   * <code>x.equals(HashResult.make(bytes))</code>.
   * @return true iff the given bytes have the same content as this
   * object's bytes. 
   * @throws {@link IllegalByteArray} if <code>bytes</code> is null or
   * zero-length.
   */
  public boolean equalsBytes(byte[] bytes) {
    checkBytes(bytes);
    return MessageDigest.isEqual(this.bytes, bytes);
  }

  /** Indicates whether some other object is "equal to" this one. */
  @Override public boolean equals(Object other) {
    if (!(other instanceof HashResult)) {
      return false;
    }
    return StringUtil.equalStrings(algorithm, ((HashResult)other).algorithm)
      && equalsBytes(((HashResult)other).bytes);
  }

  /** Returns a hash code value for the object, based on the content. */
  @Override public int hashCode() {
    return Arrays.hashCode(bytes);
  }

  public String toString() {
    String ret = ByteArray.toHexString(bytes);
    if (algorithm != null) {
      ret = algorithm + ":" + ret;
    }
    return ret;
  }
}
