/*
 * $Id: TestRateLimiter.java,v 1.4 2004-07-12 06:25:59 tlipkis Exp $
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

import junit.framework.TestCase;
import org.lockss.test.*;


/**
 * Test class for org.lockss.util.RateLimiter
 */

public class TestRateLimiter extends LockssTestCase {
  public static Class testedClasses[] = {
    org.lockss.util.RateLimiter.class
  };

  public TestRateLimiter(String msg) {
    super(msg);
  }

  public void tearDown() throws Exception {
    TimeBase.setReal();
    super.tearDown();
  }

  public void testIll() {
    try {
      new RateLimiter(0, 10);
      fail("illegal limit");
    } catch (IllegalArgumentException e) {
    }
    try {
      new RateLimiter(10, 0);
      fail("illegal interval");
    } catch (IllegalArgumentException e) {
    }
    try {
      new RateLimiter(-1, 10);
      fail("illegal limit");
    } catch (IllegalArgumentException e) {
    }
    try {
      new RateLimiter(10, -1);
      fail("illegal interval");
    } catch (IllegalArgumentException e) {
    }
  }

  public void testLimit() {
    TimeBase.setSimulated(1000);
    RateLimiter lim = new RateLimiter(2, 10);
    assertTrue(lim.isEventOk());
    assertEquals(0, lim.timeUntilEventOk());
    lim.event();
    assertTrue(lim.isEventOk());
    TimeBase.step(5);
    assertTrue(lim.isEventOk());
    lim.event();
    assertFalse(lim.isEventOk());
    assertEquals(5, lim.timeUntilEventOk());
    TimeBase.step(4);
    assertFalse(lim.isEventOk());
    assertEquals(1, lim.timeUntilEventOk());
    TimeBase.step(1);
    assertTrue(lim.isEventOk());
    lim.event();
    assertFalse(lim.isEventOk());
    assertEquals(5, lim.timeUntilEventOk());
  }

}
