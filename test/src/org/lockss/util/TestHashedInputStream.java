/*
 * $Id: TestHashedInputStream.java,v 1.1.4.1 2013-07-22 18:07:48 dshr Exp $
 */

/*

Copyright (c) 2013 Board of Trustees of Leland Stanford Jr. University,
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
import org.lockss.test.*;

public class TestHashedInputStream extends LockssTestCase {

  /*
   * NB - the specification of HashedInputStream is that it
   * hashes the entire content of the unerlying stream whether
   * or not the client reads it.
   */
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
    InputStream inp = makeEmptyStream();
    try {
      HashedInputStream is = new HashedInputStream(null, md);
      fail("Calling the constructor with a null InputStream should have thrown");
    } catch (IllegalArgumentException e) {
    }
    try {
      HashedInputStream is = new HashedInputStream(inp, null);
      fail("Calling the constructor with a null InputStream should have thrown");
    } catch (IllegalArgumentException e) {
    }
  }

  public void testEmptyStream() throws IOException {
    InputStream inp = makeEmptyStream();
    MockMessageDigest md = makeMessageDigest();
    HashedInputStream is = new HashedInputStream(inp, md);
    assertEquals(-1, is.read());
    is.close();
    assertEquals(0, md.getUpdatedBytes().length);
  }

  public void testFullStreamReadOne() throws IOException {
    InputStream inp = makeFullStream();
    MockMessageDigest md = makeMessageDigest();
    HashedInputStream is = new HashedInputStream(inp, md);
    assertEquals(fullStream[0], is.read());
    byte[] t = new byte[1];
    t[0] = fullStream[0];
    is.close();
    assertEquals(fullStream, md.getUpdatedBytes());
  }

  public void testFullStreamReadBuff() throws IOException {
    InputStream inp = makeFullStream();
    MockMessageDigest md = makeMessageDigest();
    HashedInputStream is = new HashedInputStream(inp, md);
    byte[] t = new byte[fullStream.length];
    assertEquals(fullStream.length, is.read(t));
    assertEquals(fullStream, t);
    is.close();
    assertEquals(fullStream, md.getUpdatedBytes());
  }

  public void testFullStreamReadOffset() throws IOException {
    InputStream inp = makeFullStream();
    MockMessageDigest md = makeMessageDigest();
    HashedInputStream is = new HashedInputStream(inp, md);
    byte[] t = new byte[fullStream.length];
    int off = 11;
    int len = 33;
    assertEquals(len, is.read(t, off, len));
    byte[] p = new byte[len];
    for (int i = 0; i < len; i++) {
      assertEquals(t[i+off], fullStream[i]);
      p[i] = t[i + off];
    }
    is.close();
    assertEquals(fullStream, md.getUpdatedBytes());
  }

  public void testMarkReset() throws IOException {
    InputStream inp = makeFullStream();
    MockMessageDigest md = makeMessageDigest();
    HashedInputStream is = new HashedInputStream(inp, md);
    byte[] t = new byte[11];
    int len = t.length;
    byte[] p = new byte[len*3];
    for (int i = 0; i < len; i++) {
      // We read len bytes, then call mark()
      p[i] = fullStream[i];
      // We read len bytes, then call reset()
      p[i+len] = fullStream[i+len];
      // We read len bytes
      p[i+2*len] = fullStream[i+len];
    }
    assertEquals(len, is.read(t));
    for (int i = 0; i < len; i++) {
      assertEquals(t[i], p[i]);
    }
    is.mark(2*len);
    assertEquals(len, is.read(t));
    for (int i = 0; i < len; i++) {
      assertEquals(t[i], p[i+len]);
    }
    is.reset();
    assertEquals(len, is.read(t));
    for (int i = 0; i < len; i++) {
      assertEquals(t[i], p[i+2*len]);
    }
    is.close();
    assertEquals(fullStream, md.getUpdatedBytes());
  }
}
