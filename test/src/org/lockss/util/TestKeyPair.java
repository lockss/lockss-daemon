/*
 * $Id: TestKeyPair.java,v 1.1 2004-10-18 03:23:08 tlipkis Exp $
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

import java.util.*;
import org.lockss.test.*;

/**
 * This is the test class for org.lockss.daemon.KeyPair
 */
public class TestKeyPair extends LockssTestCase {

  public void testEquals() {
    KeyPair knn = new KeyPair(null, null);
    KeyPair kn1 = new KeyPair(null, "1");
    KeyPair k1n = new KeyPair("1", null);
    KeyPair k11 = new KeyPair("1", "1");
    KeyPair k12 = new KeyPair("1", "2");

    assertEquals(knn, new KeyPair(null, null));
    assertEquals(kn1, new KeyPair(null, "1"));
    assertEquals(k1n, new KeyPair("1", null));
    assertEquals(k11, new KeyPair("1", "1"));
    assertEquals(k12, new KeyPair("1", "2"));

    assertNotEquals(knn, kn1);
    assertNotEquals(knn, kn1);
    assertNotEquals(knn, k1n);
    assertNotEquals(knn, k11);
    assertNotEquals(kn1, knn);
    assertNotEquals(kn1, k1n);
    assertNotEquals(kn1, k11);
  }

  public void testHashCode() {
    KeyPair knn = new KeyPair(null, null);
    KeyPair kn1 = new KeyPair(null, "1");
    KeyPair k1n = new KeyPair("1", null);
    KeyPair k11 = new KeyPair("1", "1");
    KeyPair k12 = new KeyPair("1", "2");

    assertEquals(knn.hashCode(), new KeyPair(null, null).hashCode());
    assertEquals(kn1.hashCode(), new KeyPair(null, "1").hashCode());
    assertEquals(k1n.hashCode(), new KeyPair("1", null).hashCode());
    assertEquals(k11.hashCode(), new KeyPair("1", "1").hashCode());
    assertEquals(k12.hashCode(), new KeyPair("1", "2").hashCode());
  }
}
