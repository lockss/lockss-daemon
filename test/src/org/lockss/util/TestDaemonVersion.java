/*
 * $Id: TestDaemonVersion.java,v 1.1 2004-06-14 03:08:45 smorabito Exp $
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
 * Test class for org.lockss.util.DaemonVersion
 *
 */

public class TestDaemonVersion extends LockssTestCase {
  public static Class testedClasses[] = {
    org.lockss.util.DaemonVersion.class
  };

  public void testMakeVersion() {
    try {
      Version a = new DaemonVersion("1.0.0");
      Version b = new DaemonVersion("2.3.5-test");
      Version c = new DaemonVersion("2.3b.5a");
    } catch (Throwable t) {
      fail("Unparsable daemon version. " + t);
    }
  }

  public void testEquals() {
    Version a = new DaemonVersion("2.3.5");
    Version b = new DaemonVersion("2.3.5");
    assertEquals(a.toLong(), b.toLong());

    Version c = new DaemonVersion("2.3.5-test");
    assertEquals(a.toLong(), c.toLong());

    Version d = new DaemonVersion("2.3.5b");
    assertNotEquals(a.toLong(), d.toLong());
  }

  public void testTooLong() {
    try {
      Version a = new DaemonVersion("123.123.123");
    } catch (IllegalArgumentException ex) {
      fail("Should not have thrown.");
    }

    try {
      Version a = new DaemonVersion("1234.123.123");
      fail("Should have thrown.");
    } catch (IllegalArgumentException ex) {
    }

    try {
      Version a = new DaemonVersion("123.1234.123");
      fail("Should have thrown.");
    } catch (IllegalArgumentException ex) {
    }

    try {
      Version a = new DaemonVersion("123.123.1234");
      fail("Should have thrown.");
    } catch (IllegalArgumentException ex) {
    }
  }

  public void testIllegalFormat() {
    try {
      Version a = new DaemonVersion("1.2.3.4");
      fail("Should have thrown.");
    } catch (IllegalArgumentException ex) {
    }

    try {
      Version a = new DaemonVersion("1.2.$.4");
      fail("Should have thrown.");
    } catch (IllegalArgumentException ex) {
    }
  }

  public void testGreaterOrLessThan() {
    Version a = new DaemonVersion("2.3.5");
    Version b = new DaemonVersion("2.3.6");
    Version c = new DaemonVersion("2.4.0");
    Version d = new DaemonVersion("3.2.0");
    Version e = new DaemonVersion("2.3.5a");
    Version f = new DaemonVersion("2.3.5z");
    Version g = new DaemonVersion("2.3.5-test"); // should equal a

    assertTrue(a.toLong() < b.toLong());
    assertTrue(a.toLong() < c.toLong());
    assertTrue(a.toLong() < d.toLong());
    assertTrue(a.toLong() < e.toLong());
    assertTrue(a.toLong() < f.toLong());
    assertFalse(a.toLong() < g.toLong());
    assertEquals(a.toLong(), g.toLong());
  }

}
