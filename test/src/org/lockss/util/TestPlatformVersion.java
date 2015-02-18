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
 * Test class for org.lockss.util.PlatformVersion
 *
 */

public class TestPlatformVersion extends LockssTestCase {
  public static Class testedClasses[] = {
    org.lockss.util.PlatformVersion.class
  };

  private static String OPENBSD = "OpenBSD CD";

  public void testOld() throws Exception {
    PlatformVersion a = new PlatformVersion("1");
    assertEquals(1, a.toLong());
    assertEquals(OPENBSD, a.getName());
    assertEquals(OPENBSD + "-1", a.toString());
    assertEquals(OPENBSD + " 1", a.displayString());
    PlatformVersion b = new PlatformVersion("123");
    assertEquals(123, b.toLong());
    assertEquals(OPENBSD, b.getName());
    PlatformVersion c = new PlatformVersion("321-foobar");
    assertEquals(321, c.toLong());
    assertEquals(OPENBSD, c.getName());
    assertEquals(OPENBSD + "-321-foobar", c.toString());
    new PlatformVersion("12345678901");
    try {
      new PlatformVersion("123-");
      fail("123- Should have thrown.");
    } catch (IllegalArgumentException e) {}
    try {
      new PlatformVersion("123abc");
      fail("123abc Should have thrown.");
    } catch (IllegalArgumentException e) {}
    try {
      new PlatformVersion("123abc-test");
      fail("123abc-test Should have thrown.");
    } catch (IllegalArgumentException e) {}
    try {
      new PlatformVersion("123456789012");
      fail("123456789012 Should have thrown.");
    } catch (IllegalArgumentException e) {}
  }

  public void testNew() throws Exception {
    PlatformVersion a = new PlatformVersion("foo-1");
    assertEquals(1, a.toLong());
    assertEquals("foo", a.getName());
    assertEquals("foo-1", a.toString());
    PlatformVersion b = new PlatformVersion("now is the time-123");
    assertEquals(123, b.toLong());
    assertEquals("now is the time", b.getName());
    PlatformVersion c = new PlatformVersion("plat-321-foobar");
    assertEquals(321, c.toLong());
    assertEquals("plat", c.getName());
    assertEquals("plat-321-foobar", c.toString());
    assertEquals("plat 321-foobar", c.displayString());
    new PlatformVersion("12345678901");
    try {
      new PlatformVersion("a-123-");
      fail("a-123- Should have thrown.");
    } catch (IllegalArgumentException e) {}
    try {
      new PlatformVersion("a-123abc");
      fail("a-123abc Should have thrown.");
    } catch (IllegalArgumentException e) {}
    try {
      new PlatformVersion("a-123abc-test");
      fail("a-123abc-test Should have thrown.");
    } catch (IllegalArgumentException e) {}
    try {
      new PlatformVersion("a-123456789012");
      fail("a-123456789012 Should have thrown.");
    } catch (IllegalArgumentException e) {}
  }

  public void testIllegalFormat() {
    try {
      new PlatformVersion("!@#$");
      fail("!@#$Should have thrown.");
    } catch (IllegalArgumentException ex) {
    }

    try {
      new PlatformVersion("150.5");
      fail("150.5Should have thrown.");
    } catch (IllegalArgumentException ex) {
    }
  }
}
