/*
 * $Id: TestLockssTestCase.java,v 1.4 2002-10-25 21:47:40 tal Exp $
 */

/*

Copyright (c) 2002 Board of Trustees of Leland Stanford Jr. University,
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

import java.util.*;
import java.io.*;
import junit.framework.TestCase;
import org.lockss.util.*;
import org.lockss.test.*;


/**
 * Test class for <code>org.lockss.test.LockssTestCase</code>
 */

public class TestLockssTestCase extends LockssTestCase {
  public static Class testedClasses[] = {
    org.lockss.test.LockssTestCase.class
  };

  public TestLockssTestCase(String msg) {
    super(msg);
  }

  public void testIso() {
    Vector v1 = new Vector();
    String a0[] = {};
    Object a1[] = {"12", new Integer(42)};
    assertIsomorphic(a0, v1);
    try {
      assertIsomorphic(a1, v1);
      fail("assertIsomorphic should have thrown AssertionFailedError");
    } catch (junit.framework.AssertionFailedError e) {
    }
    v1.add(a1[0]);
    try {
      assertIsomorphic(a1, v1);
      fail("assertIsomorphic should have thrown AssertionFailedError");
    } catch (junit.framework.AssertionFailedError e) {
    }
    v1.add(a1[1]);
    assertIsomorphic(a1, v1);
    assertIsomorphic(a1, v1.iterator());
    assertIsomorphic(v1, v1);
  }

  public void testMap() {
    Map m1 = new HashMap();
    Map m2 = new Hashtable();
    assertEquals(m1, m2);
    m1.put("1", "one");
    m2.put("1", "one");
    assertEquals(m1, m2);
    assertEquals(m2, m1);
  }

  public void testTempDir() throws Exception {
    File tmp = getTempDir();
    assertTrue(tmp.exists());
    assertTrue(tmp.isDirectory());
    // how to test that it gets deleted by tearDown()?
    System.out.println("Make sure " + tmp.getPath() + " is gone.");
  }
}
