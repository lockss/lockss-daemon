/*
 * $Id$
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
 * Test class for org.lockss.util.PluginVersion
 *
 */

public class TestPluginVersion extends LockssTestCase {
  public static Class testedClasses[] = {
    org.lockss.util.PluginVersion.class
  };

  public void testMakeVersion() {
    try {
      Version a = new PluginVersion("1");
      Version b = new PluginVersion("2-test");
      Version c = new PluginVersion("2b");
    } catch (Throwable t) {
      fail("Unparsable daemon version. " + t);
    }
  }

  public void testEquals() {
    Version a = new PluginVersion("25");
    Version b = new PluginVersion("25");
    assertEquals(a.toLong(), b.toLong());

    Version c = new PluginVersion("25-test");
    assertEquals(a.toLong(), c.toLong());

    Version d = new PluginVersion("25b");
    assertNotEquals(a.toLong(), d.toLong());
  }

  public void testTooLong() {
    try {
      Version a = new PluginVersion("12345678901");
    } catch (IllegalArgumentException ex) {
      fail("Should not have thrown.");
    }

    try {
      Version a = new PluginVersion("123456789012");
      fail("Should have thrown.");
    } catch (IllegalArgumentException ex) {
    }
  }

  public void testIllegalFormat() {
    try {
      Version a = new PluginVersion("1.2");
      fail("Should have thrown.");
    } catch (IllegalArgumentException ex) {
    }

    try {
      Version a = new PluginVersion("a$");
      fail("Should have thrown.");
    } catch (IllegalArgumentException ex) {
    }
  }

  public void testGreaterOrLessThan() {
    Version a = new PluginVersion("235");
    Version b = new PluginVersion("236");
    Version c = new PluginVersion("234");
    Version d = new PluginVersion("322");
    Version e = new PluginVersion("235a");
    Version f = new PluginVersion("235z");
    Version g = new PluginVersion("235-test"); // should equal a

    assertTrue(a.toLong() < b.toLong());
    assertTrue(a.toLong() > c.toLong());
    assertTrue(a.toLong() < d.toLong());
    assertTrue(a.toLong() < e.toLong());
    assertTrue(a.toLong() < f.toLong());
    assertFalse(a.toLong() < g.toLong());
    assertEquals(a.toLong(), g.toLong());
  }

}
