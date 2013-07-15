/*
 * $Id: TestHashResult.java,v 1.7 2013-07-15 18:46:10 tlipkis Exp $
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

package org.lockss.hasher;

import org.lockss.test.*;

public class TestHashResult extends LockssTestCase {
  public static byte[] bytes = {0, 1, 2};
  public static byte[] bytes2 = {2, 1, 0};
  public static byte[] empty_bytes = {};

  public void testCreate() {
    HashResult.make(bytes);
  }

  public void testCreateNoBytes() {
    try {
      HashResult.make((byte[])null);
      fail();
    } catch (HashResult.IllegalByteArray e) {
      // expected
    }
    try {
      HashResult.make(empty_bytes);
      fail();
    } catch (HashResult.IllegalByteArray e) {
      // expected
    }
    try {
      HashResult.make((String)null);
      fail();
    } catch (HashResult.IllegalByteArray e) {
      // expected
    }
  }

  public void testEqualsBytes() {
    HashResult o1 = HashResult.make(bytes);
    assertTrue(o1.equalsBytes(bytes));

    try {
      o1.equalsBytes(null);
    } catch (HashResult.IllegalByteArray e) {
      // expected
    }

    try {
      o1.equalsBytes(empty_bytes);
    } catch (HashResult.IllegalByteArray e) {
      // expected
    }
  }

  public void testEqualsAndHashCode() {
    HashResult o1 = HashResult.make(bytes);
    HashResult o2 = HashResult.make(bytes);
    HashResult o3 = HashResult.make(bytes2);
    HashResult o4 = HashResult.make(bytes, "fooalg");
    HashResult o5 = HashResult.make(bytes, "baralg");
    
    // o1 and o2 are not == but are equals.
    assertNotSame(o1, o2);
    assertTrue(o1.equals(o2));
    assertTrue(o2.equals(o1));

    assertFalse(o1.equals(o3));
    assertFalse(o3.equals(o1));

    assertFalse(o4.equals(o1));
    assertFalse(o1.equals(o4));

    assertFalse(o4.equals(o5));
    assertFalse(o5.equals(o4));

    assertEquals(o1.hashCode(), o2.hashCode());
  }

  private String[] bad = {
    "deadbeef",
    "SHA-1:",
    "SHA-1:bar",
    ":deadbeef"
  };

  public void testMakeFromString() {
    String good = "SHA-1:deadbeef";
    HashResult hr = HashResult.make(good);
    assertEquals(0, good.compareToIgnoreCase(hr.toString()));
    for (int i = 0; i < bad.length; i++) {
      try {
	hr = HashResult.make(bad[i]);
	fail("Should be illegal: " + bad[i]);
      } catch (HashResult.IllegalByteArray ex) {
	// Expected
      }
    }
  }
}
