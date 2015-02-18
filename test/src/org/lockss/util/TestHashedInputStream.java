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
import java.security.*;
import org.apache.commons.lang3.*;
import org.lockss.test.*;

public class TestHashedInputStream extends LockssTestCase {

  private byte[] emptyStream = new byte[0];
  private byte[] fullStream = new byte[77];

  private InputStream makeEmptyStream() {
    return new ByteArrayInputStream(emptyStream);
  }

  private InputStream makeFullStream() {
    for (int i = 0; i < fullStream.length; i++) {
      fullStream[i] = (byte)(i + 19);
    }
    return new ByteArrayInputStream(fullStream);
  }

  private MockMessageDigest makeMessageDigest() {
    return new MockMessageDigest();
  }

  public void testNullArgumentsToConstructor() {
    MessageDigest md = makeMessageDigest();
    HashedInputStream.Hasher h = new HashedInputStream.Hasher(md);
    InputStream inp = makeEmptyStream();
    try {
      HashedInputStream is = new HashedInputStream(null, h);
      fail("Calling the constructor with a null InputStream should have thrown");
    } catch (IllegalArgumentException e) {
    }
    try {
      HashedInputStream is = new HashedInputStream(inp, null);
      fail("Calling the constructor with a null Hasher should have thrown");
    } catch (IllegalArgumentException e) {
    }
  }

  public void testEmptyStream() throws IOException {
    InputStream inp = makeEmptyStream();
    MockMessageDigest md = makeMessageDigest();
    HashedInputStream.Hasher hasher = new HashedInputStream.Hasher(md);
    HashedInputStream is = new HashedInputStream(inp, hasher);
    assertFalse(hasher.isValid());
    assertEquals(-1, is.read());
    assertEquals(0, md.getUpdatedBytes().length);
    assertTrue(hasher.isValid());
    is.close();
    assertEquals(0, md.getUpdatedBytes().length);
    assertTrue(hasher.isValid());
  }

  public void testFullStreamReadOne() throws IOException {
    InputStream inp = makeFullStream();
    MockMessageDigest md = makeMessageDigest();
    HashedInputStream.Hasher hasher = new HashedInputStream.Hasher(md);
    HashedInputStream is = new HashedInputStream(inp, hasher);
    assertFalse(hasher.isValid());
    assertEquals(fullStream[0], is.read());
    assertFalse(hasher.isValid());
    byte[] t = new byte[1];
    t[0] = fullStream[0];
    assertEquals(t, md.getUpdatedBytes());
    assertFalse(hasher.isValid());
    is.close();
    assertFalse(hasher.isValid());
  }

  public void testFullStreamReadBuff() throws IOException {
    InputStream inp = makeFullStream();
    MockMessageDigest md = makeMessageDigest();
    HashedInputStream.Hasher hasher = new HashedInputStream.Hasher(md);
    HashedInputStream is = new HashedInputStream(inp, hasher);
    byte[] t = new byte[fullStream.length];
    assertFalse(hasher.isValid());
    assertEquals(fullStream.length, StreamUtil.readBytes(is, t, t.length));
    assertEquals(fullStream, t);
    assertEquals(t, md.getUpdatedBytes());
    is.close();
    // have read up to but not including signaling EOF.  close() should
    // figure out that whole stream was read
    assertTrue(hasher.isValid());
  }

  public void testReadToEof() throws IOException {
    InputStream inp = makeFullStream();
    MockMessageDigest md = makeMessageDigest();
    HashedInputStream.Hasher hasher = new HashedInputStream.Hasher(md);
    HashedInputStream is = new HashedInputStream(inp, hasher);
    byte[] t = new byte[fullStream.length + 10];
    assertFalse(hasher.isValid());
    int n = StreamUtil.readBytes(is, t, t.length);
    assertEquals(fullStream.length, n);
    assertNotEquals(t.length, n);
    assertTrue(hasher.isValid());
    byte[] subt = ArrayUtils.subarray(t, 0, fullStream.length);
    assertEquals(fullStream, subt);
    assertEquals(subt, md.getUpdatedBytes());
    is.close();
    assertTrue(hasher.isValid());
  }

  public void testFullStreamReadOffset() throws IOException {
    InputStream inp = makeFullStream();
    MockMessageDigest md = makeMessageDigest();
    HashedInputStream.Hasher hasher = new HashedInputStream.Hasher(md);
    HashedInputStream is = new HashedInputStream(inp, hasher);
    byte[] t = new byte[fullStream.length];
    int off = 11;
    int len = 33;
    assertEquals(len, is.read(t, off, len));
    assertFalse(hasher.isValid());
    byte[] p = new byte[len];
    for (int i = 0; i < len; i++) {
      assertEquals(t[i+off], fullStream[i]);
      p[i] = t[i + off];
    }
    assertFalse(hasher.isValid());
    assertEquals(p, md.getUpdatedBytes());
    is.close();
    assertFalse(hasher.isValid());
  }
}
