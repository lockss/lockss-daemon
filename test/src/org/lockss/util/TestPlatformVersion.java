/*
 * $Id: TestPlatformVersion.java,v 1.1 2004-06-14 03:08:45 smorabito Exp $
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
 * Test class for org.lockss.util.PlatformVersion
 *
 */

public class TestPlatformVersion extends LockssTestCase {
  public static Class testedClasses[] = {
    org.lockss.util.PlatformVersion.class
  };

  public void testMakeVersion() {
    try {
      Version a = new PlatformVersion("1");
      Version b = new PlatformVersion("123");
      Version c = new PlatformVersion("123abc");
      Version d = new PlatformVersion("123abc-test");
      Version e = new PlatformVersion("123abc-beta");
    } catch (Throwable t) {
      fail("Should not have thrown.");
    }
  }

  public void testEquals() {
    Version a = new PlatformVersion("150");
    Version b = new PlatformVersion("150");
    assertEquals(a.toLong(), b.toLong());

    Version c = new PlatformVersion("150-test");
    assertEquals(a.toLong(), c.toLong());

    Version d = new PlatformVersion("150a");
    assertNotEquals(a.toLong(), d.toLong());
  }

  public void testTooLong() {
    try {
      Version a = new PlatformVersion("12345678901");
    } catch (IllegalArgumentException ex) {
      fail("Should not have thrown.");
    }

    try {
      Version a = new PlatformVersion("123456789012");
      fail("Should have thrown.");
    } catch (IllegalArgumentException ex) {
    }
  }

  public void testIllegalFormat() {
    try {
      Version a = new PlatformVersion("!@#$");
      fail("Should have thrown.");
    } catch (IllegalArgumentException ex) {
    }

    try {
      Version b = new PlatformVersion("150.5");
      fail("Should have thrown.");
    } catch (IllegalArgumentException ex) {
    }
  }

  public void testGreaterOrLessThan() {
    Version a = new PlatformVersion("235");
    Version b = new PlatformVersion("236");
    Version c = new PlatformVersion("240");
    Version d = new PlatformVersion("320");
    Version e = new PlatformVersion("235a");
    Version f = new PlatformVersion("235z");
    Version g = new PlatformVersion("235-test"); // should equal a

    assertTrue(a.toLong() < b.toLong());
    assertTrue(a.toLong() < c.toLong());
    assertTrue(a.toLong() < d.toLong());
    assertTrue(a.toLong() < e.toLong());
    assertTrue(a.toLong() < f.toLong());
    assertFalse(a.toLong() < g.toLong());
    assertEquals(a.toLong(), g.toLong());
  }

}
