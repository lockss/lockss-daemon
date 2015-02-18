/*
 * $Id$
 */

/*

Copyright (c) 2000-2006 Board of Trustees of Leland Stanford Jr. University,
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
import junit.framework.*;

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

    boolean exceptionThrown = false;
    try {
      assertIsomorphic(a1, v1);
    } catch (junit.framework.AssertionFailedError e) {
      exceptionThrown = true;
    }
    if (!exceptionThrown) {
      fail("assertIsomorphic should have thrown AssertionFailedError");
    }

    exceptionThrown = false;
    v1.add(a1[0]);
    try {
      assertIsomorphic(a1, v1);
    } catch (junit.framework.AssertionFailedError e) {
      exceptionThrown = true;
    }
    if (!exceptionThrown) {
      fail("assertIsomorphic should have thrown AssertionFailedError");
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

  static String UNLIKELY = "very|unlikely";

  public void testRestoreJavaIoTmpdir1() throws Exception {
    System.setProperty("java.io.tmpdir", UNLIKELY);
  }

  public void testRestoreJavaIoTmpdir2() throws Exception {
    assertNotEquals(UNLIKELY, System.getProperty("java.io.tmpdir"));
  }

  public void testAssertNotEqualsLong() {
    assertNotEquals((long)17, (long)15);
  }

  public void testAssertNotEqualsLongIsEqual() {
    boolean exceptionThrown = false;
    try {
      assertNotEquals((long)17, (long)17);
    } catch (AssertionFailedError afe) {
      exceptionThrown = true;
    }
    if (!exceptionThrown) {
      fail("assertNotEquals should have thrown an AssertionFailedException");
    }
  }

  public void testAssertNotEqualsInt() {
    assertNotEquals(17, 15);
  }

  public void testAssertNotEqualsIntIsEqual() {
    boolean exceptionThrown = false;
    try {
      assertNotEquals(17, 17);
    } catch (AssertionFailedError afe) {
      exceptionThrown = true;
    }
    if (!exceptionThrown) {
      fail("assertNotEquals should have thrown an AssertionFailedException");
    }
  }

  public void testAssertNotEqualsShort() {
    assertNotEquals((short)17, (short)15);
  }

  public void testAssertNotEqualsShortIsEqual() {
    boolean exceptionThrown = false;
    try {
      assertNotEquals((short)17, (short)17);
    } catch (AssertionFailedError afe) {
      exceptionThrown = true;
    }
    if (!exceptionThrown) {
      fail("assertNotEquals should have thrown an AssertionFailedException");
    }
  }

  public void testAssertNotEqualsByte() {
    assertNotEquals((byte)17, (byte)15);
  }

  public void testAssertNotEqualsByteIsEqual() {
    boolean exceptionThrown = false;
    try {
      assertNotEquals((byte)17, (byte)17);
    } catch (AssertionFailedError afe) {
      exceptionThrown = true;
    }
    if (!exceptionThrown) {
      fail("assertNotEquals should have thrown an AssertionFailedException");
    }
  }

  public void testAssertNotEqualsChar() {
    assertNotEquals('a', 'b');
  }

  public void testAssertNotEqualsCharIsEqual() {
    boolean exceptionThrown = false;
    try {
      assertNotEquals('a', 'a');
    } catch (AssertionFailedError afe) {
      exceptionThrown = true;
    }
    if (!exceptionThrown) {
      fail("assertNotEquals should have thrown an AssertionFailedException");
    }
  }

  public void testAssertNotEqualsBoolean() {
    assertNotEquals(true, false);
  }

  public void testAssertNotEqualsBooleanIsEqual() {
    boolean exceptionThrown = false;
    try {
      assertNotEquals(false, false);
    } catch (AssertionFailedError afe) {
      exceptionThrown = true;
    }
    if (!exceptionThrown) {
      fail("assertNotEquals should have thrown an AssertionFailedException");
    }
  }

  public void testAssertNotEqualsDouble() {
    assertNotEquals(1.0, 1.5, 0.0);
  }

  public void testAssertNotEqualsDoubleWithDelta() {
    assertNotEquals(1.0, 1.5, 0.4);
  }

  public void testAssertNotEqualsDoubleIsEqual() {
    boolean exceptionThrown = false;
    try {
      assertNotEquals(1.0, 1.0, 0.0);
    } catch (AssertionFailedError afe) {
      exceptionThrown = true;
    }
    if (!exceptionThrown) {
      fail("assertNotEquals should have thrown an AssertionFailedException");
    }
  }

  public void testAssertNotEqualsDoubleIsEqualWithinDelta() {
    boolean exceptionThrown = false;
    try {
      assertNotEquals(1.0, 1.3, 0.5);
    } catch (AssertionFailedError afe) {
      exceptionThrown = true;
    }
    if (!exceptionThrown) {
      fail("assertNotEquals should have thrown an AssertionFailedException");
    }
  }

  public void testAssertNotEqualsFloat() {
    assertNotEquals(1.0, 1.5, 0.0);
  }

  public void testAssertNotEqualsFloatWithDelta() {
    assertNotEquals(1.0, 1.5, 0.4);
  }

  public void testAssertNotEqualsFloatIsEqual() {
    boolean exceptionThrown = false;
    try {
      assertNotEquals(1.0, 1.0, 0.0);
    } catch (AssertionFailedError afe) {
      exceptionThrown = true;
    }
    if (!exceptionThrown) {
      fail("assertNotEquals should have thrown an AssertionFailedException");
    }
  }

  public void testAssertNotEqualsFloatIsEqualWithinDelta() {
    boolean exceptionThrown = false;
    try {
      assertNotEquals(1.0, 1.3, 0.5);
    } catch (AssertionFailedError afe) {
      exceptionThrown = true;
    }
    if (!exceptionThrown) {
      fail("assertNotEquals should have thrown an AssertionFailedException");
    }
  }

  public void testAssertNoDuplicates() {
    try {
      assertNoDuplicates(ListUtil.list("a", "b", "c"));
    } catch (AssertionFailedError afe) {
      fail("assertNoDuplicates([\"a\", \"b\", \"c\"]) failed");
    }
    try {
      assertNoDuplicates(ListUtil.list("a", "b", "a"));
      fail("assertNoDuplicates([\"a\", \"b\", \"a\"]) should have failed");
    } catch (AssertionFailedError afe) {
    }
  }

  int rpt;
  protected void successRateSetUp() {
    super.successRateSetUp();
    rpt = 10;
  }


  public void testSuccessRate100Succeed() {
    assertSuccessRate(1, 0);
  }

  public void testSuccessRate90Succeed() {
    assertSuccessRate(.9, 0);
  }

  public void testSuccessRate90Fail1A() {
    assertSuccessRate(.9, 10);
    if (rpt-- == 10) {
      fail("failing on iteration " + rpt);
    }
  }

  public void testSuccessRate90Fail1B() {
    assertSuccessRate(.9, 10);
    if (rpt-- == 5) {
      fail("failing on iteration " + rpt);
    }
  }

  public void donttestSuccessRate90Fail2() {
    assertSuccessRate(.9, 10);
    if (rpt-- >= 9) {
      fail("failing on iteration " + rpt);
    }
  }

  public void testAssertContainsAll() {
    Set set = new HashSet();
    set.add("one");
    set.add("two");
    set.add("three");
    String[] strs = { "three","two","one" };
    assertContainsAll(set,strs);
    String[] extra = { "three","two","one", "four" };
    try {
      assertContainsAll(set,extra);
      fail("assertContainsAll should have thrown an AssertionFailedException");
    } catch (AssertionFailedError afe) {
    }
  }

  public void testAssertMatchesRE() {
    assertMatchesRE("", "foo");
    assertNotMatchesRE("^$", "foo");
    assertMatchesRE(".*", "foo");
    assertMatchesRE("bar", "foobar");
    assertNotMatchesRE("bar", "baz");
  }

  public void testAssertPositive() {
    final String failed = "Asserted that this value was positive: ";

    for (int val = Integer.MAX_VALUE ; val > 0 ; val >>= 1) {
      assertPositive(val);
    }

    try {
      assertPositive(0);
      fail(failed + 0);
    } catch (AssertionFailedError afe) {
      // All is well
    }

    for (int val = Integer.MIN_VALUE ; val != -1 ; val >>= 1) {
      try {
        assertPositive(val);
        fail(failed + val);
      } catch (AssertionFailedError afe) {
        // All is well
      }
    }
  }

  public void testAssertNegative() {
    final String failed = "Asserted that this value was negative: ";

    for (int val = Integer.MIN_VALUE ; val != -1 ; val >>= 1) {
      assertNegative(val);
    }

    try {
      assertNegative(0);
      fail(failed + 0);
    } catch (AssertionFailedError afe) {
      // All is well
    }

    for (int val = Integer.MAX_VALUE ; val > 0 ; val >>= 1) {
      try {
        assertNegative(val);
        fail(failed + val);
      } catch (AssertionFailedError afe) {
        // All is well
      }
    }
  }

}
