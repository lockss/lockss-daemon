/*
 * $Id: TestRateLimiter.java,v 1.7 2006-05-15 00:12:49 tlipkis Exp $
 */

/*

Copyright (c) 2000-2005 Board of Trustees of Leland Stanford Jr. University,
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

import org.lockss.config.*;
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

  public void testIllegal() {
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

  public void testConstructor() {
    RateLimiter lim = new RateLimiter(10, 100);
    assertEquals(10, lim.getLimit());
    assertEquals(100, lim.getInterval());
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

  public void testWait() throws InterruptedException {
    TimeBase.setSimulated(1000);
    RateLimiter lim = new RateLimiter(2, 10);
    assertTrue(lim.isEventOk());
    assertTrue(lim.waitUntilEventOk());
    lim.event();
    assertTrue(lim.isEventOk());
    TimeBase.step(5);
    assertTrue(lim.isEventOk());
    assertTrue(lim.waitUntilEventOk());
    lim.event();
    assertFalse(lim.isEventOk());
    assertEquals(5, lim.timeUntilEventOk());
    DoLater doer = null;
      doer = new DoLater(100) {
	  protected void doit() {
	    TimeBase.step(5);
	  }
	};
      doer.start();
      assertTrue(lim.waitUntilEventOk());
      doer.cancel();
  }

  public void testUnlimited() throws InterruptedException {
    RateLimiter lim = RateLimiter.UNLIMITED;
    assertTrue(lim.isEventOk());
    assertEquals(0, lim.timeUntilEventOk());
    assertTrue(lim.waitUntilEventOk());
    lim.event();
    assertTrue(lim.isEventOk());
    lim.event();
    assertTrue(lim.isEventOk());
  }

  public void testGetFromConfig1() {
    RateLimiter lim;
    Configuration config;
    config = ConfigurationUtil.fromArgs("events", "7", "interval", "10m");
    lim = RateLimiter.getConfiguredRateLimiter(config, null,
					       "events", 3, "interval", 200);
    assertEquals(7, lim.getLimit());
    assertEquals(10 * Constants.MINUTE, lim.getInterval());
    assertSame(lim, RateLimiter.getConfiguredRateLimiter(config, lim,
							 "events", 3,
							 "interval", 200));

    config = ConfigurationUtil.fromArgs("events", "7");
    lim = RateLimiter.getConfiguredRateLimiter(config, null,
					       "events", 3, "interval", 200);
    assertEquals(7, lim.getLimit());
    assertEquals(200, lim.getInterval());

    config = ConfigurationUtil.fromArgs("foo", "7");
    lim = RateLimiter.getConfiguredRateLimiter(config, null,
					       "events", 3, "interval", 200);
    assertEquals(3, lim.getLimit());
    assertEquals(200, lim.getInterval());
  }

  public void testGetFromConfig2() {
    RateLimiter lim;
    Configuration config;
    config = ConfigurationUtil.fromArgs("rate1", "7/10m");
    lim = RateLimiter.getConfiguredRateLimiter(config, null,
					       "rate1", "3/200");
    assertEquals(7, lim.getLimit());
    assertEquals(10 * Constants.MINUTE, lim.getInterval());
    assertSame(lim, RateLimiter.getConfiguredRateLimiter(config, lim,
							 "rate1", "3/200"));

    config = ConfigurationUtil.fromArgs("rate1", "4/20h");
    lim = RateLimiter.getConfiguredRateLimiter(config, lim,
					       "rate1", "3/200");
    assertEquals(4, lim.getLimit());
    assertEquals(20 * Constants.HOUR, lim.getInterval());

    config = ConfigurationUtil.fromArgs("rate1", "xx/20h");
    lim = RateLimiter.getConfiguredRateLimiter(config, lim,
					       "rate1", "3/200");
    assertEquals(3, lim.getLimit());
    assertEquals(200, lim.getInterval());

    config = ConfigurationUtil.fromArgs("rate1", "7/20q");
    lim = RateLimiter.getConfiguredRateLimiter(config, lim,
					       "rate1", "3/200");
    assertEquals(3, lim.getLimit());
    assertEquals(200, lim.getInterval());

    config = ConfigurationUtil.fromArgs("norate", "foo");
    lim = RateLimiter.getConfiguredRateLimiter(config, null, "rate1", "3/200");
    assertEquals(3, lim.getLimit());
    assertEquals(200, lim.getInterval());
  }

  public void testGetFromConfigUnlimited() {
    Configuration config = ConfigurationUtil.fromArgs("rate1", "Unlimited");
    RateLimiter lim = RateLimiter.getConfiguredRateLimiter(config, null,
					       "rate1", "3/200");
    assertSame(RateLimiter.UNLIMITED, lim);
  }
}
