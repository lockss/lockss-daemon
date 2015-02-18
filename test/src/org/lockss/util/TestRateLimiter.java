/*
 * $Id$
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

  public void testAccessors() {
    RateLimiter lim = new RateLimiter(10, 100);
    assertEquals(10, lim.getLimit());
    assertEquals(100, lim.getInterval());
    String rate = lim.getRate();
    assertEquals("10/100ms", rate);
    assertSame(rate, lim.getRate());
    assertTrue(lim.isRate(10, 100));
    assertFalse(lim.isRate(1, 100));
    assertFalse(lim.isRate(10, 1000));
    assertTrue(lim.isRate("10/100ms"));
    assertFalse(lim.isRate("10/100"));
    assertFalse(lim.isUnlimited());
    assertTrue(lim.isModifiable());
  }

  public void testAccessors2() {
    RateLimiter lim = new RateLimiter("10/100");
    assertEquals(10, lim.getLimit());
    assertEquals(100, lim.getInterval());
    String rate = lim.getRate();
    assertEquals("10/100", rate);
    assertSame(rate, lim.getRate());
    assertTrue(lim.isRate(10, 100));
    assertFalse(lim.isRate(1, 100));
    assertFalse(lim.isRate(10, 1000));
    assertTrue(lim.isRate("10/100"));
    assertFalse(lim.isRate("10/100ms"));
    assertFalse(lim.isUnlimited());
    assertTrue(lim.isModifiable());
  }

  public void testUnlimited() throws InterruptedException {
    RateLimiter lim = new RateLimiter("Unlimited");
    assertTrue(lim.isUnlimited());
    assertTrue(lim.isModifiable());
    assertEquals(0, lim.getLimit());
    assertEquals(0, lim.getInterval());

    assertTrue(lim.isEventOk());
    assertEquals(0, lim.timeUntilEventOk());
    assertTrue(lim.waitUntilEventOk());
    lim.event();
    assertTrue(lim.isEventOk());
    lim.event();
    assertTrue(lim.isEventOk());
    lim.unevent();
    assertTrue(lim.isEventOk());
  }

  public void testConstant() throws InterruptedException {
    RateLimiter lim = RateLimiter.UNLIMITED;
    assertTrue(lim.isUnlimited());
    assertFalse(lim.isModifiable());
    assertEquals(0, lim.getLimit());
    assertEquals(0, lim.getInterval());

    assertTrue(lim.isEventOk());
    assertEquals(0, lim.timeUntilEventOk());
    assertTrue(lim.waitUntilEventOk());
    lim.event();
    assertTrue(lim.isEventOk());
    lim.event();
    assertTrue(lim.isEventOk());
    lim.unevent();
    assertTrue(lim.isEventOk());
  }

  public void testSimple() {
    TimeBase.setSimulated(1000);
    RateLimiter lim = new RateLimiter(1, 10);
    assertTrue(lim.isEventOk());
    assertEquals(0, lim.timeUntilEventOk());
    lim.event();
    assertFalse(lim.isEventOk());
    assertEquals(10, lim.timeUntilEventOk());
    TimeBase.step(10);
    assertTrue(lim.isEventOk());
    assertEquals(0, lim.timeUntilEventOk());
    lim.event();
    assertFalse(lim.isEventOk());
    assertEquals(10, lim.timeUntilEventOk());

    lim.unevent();
    assertTrue(lim.isEventOk());
    assertEquals(0, lim.timeUntilEventOk());
    lim.event();
    assertFalse(lim.isEventOk());
    assertEquals(10, lim.timeUntilEventOk());
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

    lim.unevent();
    assertTrue(lim.isEventOk());
    assertEquals(0, lim.timeUntilEventOk());
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

  public void xxxtestFifoWaitAndSignalEvent() throws InterruptedException {
    // XXX difficult because order is not totally predicatable
  }

  public void testResizeEventArray() {
    RateLimiter lim = new RateLimiter(1, 1);
    assertEquals(new long[]{2},
                 RateLimiter.resizeEventArray(new long[]{2}, 0, 1));
    assertEquals(new long[]{2, 3},
                 RateLimiter.resizeEventArray(new long[]{2, 3}, 0, 2));
    assertEquals(new long[]{3, 2},
                 RateLimiter.resizeEventArray(new long[]{2, 3}, 1, 2));
    assertEquals(new long[]{0, 2},
                 RateLimiter.resizeEventArray(new long[]{2}, 0, 2));
    assertEquals(new long[]{0, 2, 3},
                 RateLimiter.resizeEventArray(new long[]{2, 3}, 0, 3));
    assertEquals(new long[]{0, 3, 2},
                 RateLimiter.resizeEventArray(new long[]{2, 3}, 1, 3));
    assertEquals(new long[]{0, 0, 2, 3},
                 RateLimiter.resizeEventArray(new long[]{2, 3}, 0, 4));
    assertEquals(new long[]{0, 0, 3, 2},
                 RateLimiter.resizeEventArray(new long[]{2, 3}, 1, 4));
    assertEquals(new long[]{3, 4, 5},
                 RateLimiter.resizeEventArray(new long[]{2, 3, 4, 5, 6}, 4, 3));
    assertEquals(new long[]{2, 3, 4},
                 RateLimiter.resizeEventArray(new long[]{2, 3, 4, 5, 6}, 3, 3));
    assertEquals(new long[]{6, 2, 3},
                 RateLimiter.resizeEventArray(new long[]{2, 3, 4, 5, 6}, 2, 3));
    assertEquals(new long[]{5, 6, 2},
                 RateLimiter.resizeEventArray(new long[]{2, 3, 4, 5, 6}, 1, 3));
    assertEquals(new long[]{4, 5, 6},
                 RateLimiter.resizeEventArray(new long[]{2, 3, 4, 5, 6}, 0, 3));
    assertEquals(new long[]{3},
                 RateLimiter.resizeEventArray(new long[]{2, 3}, 0, 1));
    assertEquals(new long[]{2},
                 RateLimiter.resizeEventArray(new long[]{2, 3}, 1, 1));
  }

  public void testSetRate1() {
    TimeBase.setSimulated(1000);
    RateLimiter lim = new RateLimiter(4, 10);
    lim.event();
    TimeBase.step(15);
    lim.event();
    TimeBase.step(1);
    assertTrue(lim.isEventOk());
    assertEquals("4/10ms", lim.getRate());
    lim.setRate(2, 20);
    assertEquals(2, lim.getLimit());
    assertEquals(20, lim.getInterval());
    assertEquals("2/20ms", lim.getRate());
    assertFalse(lim.isEventOk());
    assertEquals(4, lim.timeUntilEventOk());
    TimeBase.step(5);
    assertTrue(lim.isEventOk());
    lim.event();
    assertEquals(14, lim.timeUntilEventOk());
  }

  public void testSetRate2() {
    TimeBase.setSimulated(1000);
    RateLimiter lim = new RateLimiter(4, 10);
    lim.event();
    TimeBase.step(15);
    lim.event();
    TimeBase.step(1);
    assertTrue(lim.isEventOk());
    assertEquals("4/10ms", lim.getRate());
    lim.setRate("2/20");
    assertEquals(2, lim.getLimit());
    assertEquals(20, lim.getInterval());
    assertEquals("2/20", lim.getRate());
    assertFalse(lim.isEventOk());
    assertEquals(4, lim.timeUntilEventOk());
    TimeBase.step(5);
    assertTrue(lim.isEventOk());
    lim.event();
    assertEquals(14, lim.timeUntilEventOk());
  }

  public void testSetRateLimitedToUnlimited() {
    RateLimiter lim = new RateLimiter(4, 10);
    lim.event();
    lim.event();
    lim.event();
    assertTrue(lim.isEventOk());
    lim.event();
    assertFalse(lim.isEventOk());
    assertFalse(lim.isUnlimited());
    lim.setRate("unlimited");
    assertTrue(lim.isUnlimited());
    assertTrue(lim.isEventOk());
    lim.event();
    lim.event();
    lim.event();
    lim.event();
    lim.event();
    lim.event();
    assertTrue(lim.isEventOk());
  }

  public void testSetRateUnlimitedToLimited() {
    RateLimiter lim = new RateLimiter("unlimited");
    assertTrue(lim.isEventOk());
    lim.event();
    assertTrue(lim.isEventOk());
    assertTrue(lim.isUnlimited());
    lim.setRate("2/10");
    assertFalse(lim.isUnlimited());
    assertTrue(lim.isEventOk());
    lim.event();
    lim.event();
    assertFalse(lim.isEventOk());
  }

  public void testSetRateIll() {
    RateLimiter lim = new RateLimiter(4, 10);
    try {
      lim.setRate("2/d");
      fail("illegal interval");
    } catch (IllegalArgumentException e) {}
    try {
      lim.setRate(0, 20);
      fail("setRate(0, ...) should throw");
    } catch (IllegalArgumentException e) {}
    try {
      lim.setRate(2, 0);
      fail("setRate(..., 0) should throw");
    } catch (IllegalArgumentException e) {}
  }

  public void testGetRate() {
    RateLimiter lim = new RateLimiter(4, 10);
    assertEquals("4/10ms", lim.getRate());
    lim.setRate("4/10");
    assertEquals("4/10", lim.getRate());
    lim.setRate(10, 50000);
    assertEquals("10/50s", lim.getRate());
    lim = new RateLimiter("4/10");
    assertEquals("4/10", lim.getRate());
    lim.setRate(70, 10000);
    assertEquals("70/10s", lim.getRate());
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
    RateLimiter lim2 = RateLimiter.getConfiguredRateLimiter(config, lim,
							    "events", 3,
							    "interval", 200);
    assertSame(lim, lim2);
    assertEquals(7, lim.getLimit());
    assertEquals(200, lim.getInterval());

    config = ConfigurationUtil.fromArgs("foo", "7");
    lim = RateLimiter.getConfiguredRateLimiter(config, null,
					       "events", 3, "interval", 200);
    assertEquals(3, lim.getLimit());
    assertEquals(200, lim.getInterval());
  }

  public void testGetFromConfig2Err() {
    RateLimiter lim;
    Configuration config;
    config = ConfigurationUtil.fromArgs("rate1", "7/10m/22");
    lim = RateLimiter.getConfiguredRateLimiter(config, null,
					       "rate1", "3/200");
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
    RateLimiter lim1 = RateLimiter.getConfiguredRateLimiter(config, null,
							    "rate1", "3/200");
    assertTrue(lim1.isUnlimited());
    config = ConfigurationUtil.fromArgs("rate1", "1/2");
    RateLimiter lim2 = RateLimiter.getConfiguredRateLimiter(config, lim1,
							    "rate1", "3/200");
//     assertNotSame(lim2, lim1);
    assertFalse(lim2.isUnlimited());
    assertTrue(lim2.isModifiable());
  }

  public void testRateString() {
    Configuration config = ConfigurationUtil.fromArgs("foo", "7/3s");
    RateLimiter lim = RateLimiter.getConfiguredRateLimiter(config, null,
							   "foo", "3/2");
    assertEquals("7/3000ms", lim.rateString());
    assertEquals("7/3s", lim.getRate());
    lim = new RateLimiter("22/33000");
    assertEquals("22/33s", lim.rateString());
    assertEquals("22/33000", lim.getRate());
    lim = new RateLimiter("unlimited");
    assertEquals("unlimited", lim.rateString());
    assertEquals("unlimited", lim.getRate());
  }

  public void testToString() {
    Configuration config = ConfigurationUtil.fromArgs("foo", "7/3s");
    RateLimiter lim = RateLimiter.getConfiguredRateLimiter(config, null,
							   "foo", "3/2");
    assertEquals("[RL: 7/3s]", lim.toString());
  }

  public void testPool1() {
    String r1 = "7/20s";
    String r2 = "2/123";

    RateLimiter.Pool pool = RateLimiter.getPool();
    assertSame(pool, RateLimiter.getPool());

    RateLimiter l1 = pool.findNamedRateLimiter("one", r1);
    assertEquals(r1, l1.getRate());

    RateLimiter l2 = pool.findNamedRateLimiter("one", r1);
    assertSame(l1, l2);
    assertEquals(r1, l2.getRate());

    RateLimiter l3 = pool.findNamedRateLimiter("one", r2);
    assertSame(l1, l3);
    assertEquals(r2, l1.getRate());

    RateLimiter lb = pool.findNamedRateLimiter("b", r1);
    assertNotSame(l1, lb);
    assertEquals(r1, lb.getRate());

    RateLimiter lc = pool.findNamedRateLimiter("c", r1);
    assertNotSame(l1, lc);
    assertEquals(r1, lc.getRate());

    RateLimiter l4 = pool.findNamedRateLimiter("one", r1);
    assertNotSame(lb, l4);
    assertNotSame(lc, l4);
    assertEquals(r1, l4.getRate());
  }

  public void testPool2() {
    int e1 = 7, i1 = 20000;
    int e2 = 2, i2 = 123;
    String r1 = "7/20s";
    String r2 = "2/123ms";

    RateLimiter.Pool pool = RateLimiter.getPool();
    assertSame(pool, RateLimiter.getPool());

    RateLimiter l1 = pool.findNamedRateLimiter("111", e1, i1);
    assertEquals(r1, l1.getRate());

    RateLimiter l2 = pool.findNamedRateLimiter("111", e1, i1);
    assertSame(l1, l2);
    assertEquals(r1, l2.getRate());

    RateLimiter l3 = pool.findNamedRateLimiter("111", e2, i2);
    assertSame(l1, l3);
    assertEquals(r2, l1.getRate());

    RateLimiter lb = pool.findNamedRateLimiter("bbb", e1, i1);
    assertNotSame(l1, lb);
    assertEquals(r1, lb.getRate());

    RateLimiter lc = pool.findNamedRateLimiter("ccc", e1, i1);
    assertNotSame(l1, lc);
    assertEquals(r1, lc.getRate());

    RateLimiter l4 = pool.findNamedRateLimiter("111", e1, i1);
    assertNotSame(lb, l4);
    assertNotSame(lc, l4);
    assertEquals(r1, l4.getRate());
  }

  public void testLimiterMap() {
    String par = "par.am";
    String def = "4/3000";

    String key1 = "keyone";
    MockArchivalUnit key2 = new MockArchivalUnit();
    MockArchivalUnit key3 = new MockArchivalUnit();

    RateLimiter.LimiterMap map = new RateLimiter.LimiterMap(par, def);
    RateLimiter lim1 = map.getRateLimiter(key1);
    assertEquals("4/3000", lim1.getRate());
    RateLimiter lim2 = map.getRateLimiter(key2);
    RateLimiter lim3 = map.getRateLimiter(key3);
    assertEquals(4, lim2.getLimit());
    assertEquals(3000, lim2.getInterval());
    assertNotSame(lim1, lim2);
    assertNotSame(lim1, lim3);
    assertNotSame(lim2, lim3);
    assertSame(lim1, map.getRateLimiter(key1));
    assertSame(lim2, map.getRateLimiter(key2));
    assertSame(lim3, map.getRateLimiter(key3));

    map.resetRateLimiters(ConfigurationUtil.fromArgs("foo", "bar"));
    assertSame(lim1, map.getRateLimiter(key1));
    assertSame(lim2, map.getRateLimiter(key2));
    assertSame(lim3, map.getRateLimiter(key3));
    assertEquals("4/3000", lim1.getRate());

    map.resetRateLimiters(ConfigurationUtil.fromArgs(par, "3/4000"));
    assertSame(lim1, map.getRateLimiter(key1));
    assertSame(lim2, map.getRateLimiter(key2));
    assertSame(lim3, map.getRateLimiter(key3));
    assertEquals("3/4000", lim1.getRate());
    assertEquals("3/4000", lim2.getRate());
    assertEquals("3/4000", lim3.getRate());
  }
}
