/*
 * $Id: FuncClock.java,v 1.8 2010-04-02 23:38:11 pgust Exp $
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

  package org.lockss.test;

import java.util.*;
import org.lockss.util.*;

/**
 * Functional tests for java clock and Thread.sleep
 */

public class FuncClock extends LockssTestCase {
  private static Logger log = Logger.getLogger("TestClock");

  
  public long getClockResolution() throws Exception {
    long start = System.currentTimeMillis();
    long now = start;
    long min = Long.MAX_VALUE;
    long next;
    while (true) {
      while (now == (next = System.currentTimeMillis())) {}
      long delta = next - now;
      if (delta < min) {
	min = delta;
	if (min == 1) {
	  break;
	}
      }
      now = next;
      if ((now - start) > Constants.SECOND) break;
    }
    return min;
  }

  public void testClockResolution() throws Exception {
    long clockResolution = getClockResolution();
    log.info("Clock resolution is " + clockResolution + "ms");
  }

    public void testSleep() throws Exception {
    Thread t = Thread.currentThread();
    long sleeps[] = {1, 10, 50, 100, 500};
    int fail = 0;
    long clockResolution = getClockResolution();
    for (int ix = 0, len = sleeps.length; ix < len; ix++) {
      long sleep = sleeps[ix];
      long start = System.currentTimeMillis();
      Thread.sleep(sleep);
      long slept = System.currentTimeMillis() - start;
      if (slept < sleep) {
        if (sleep < clockResolution) {
          // returning too soon OK if sleep time below clock resolution
          log.info("sleep(" + sleep + ") returned in " + slept + " (below clock resolution");
        } else {
          log.error("sleep(" + sleep + ") returned in " + slept);
          fail++;
        }
      }
    }
    if (fail > 0) fail(fail + " sleeps returned early");
  }

  public void testSleepIntr() {
    Thread t = Thread.currentThread();
    long sleeps[] = {200, 400, 1000, 60000};
    int fail = 0;
    for (int ix = 0, len = sleeps.length; ix < len; ix++) {
      long sleep = sleeps[ix];
      long start = System.currentTimeMillis();
      try {
	Interrupter intr = interruptMeIn(100);
	Thread.sleep(sleep);
	intr.cancel();
      } catch (InterruptedException e) {
	long slept = System.currentTimeMillis() - start;
	if (slept < sleep) return;
      }
    }

    fail("sleep() was not interrupted");
  }
}
