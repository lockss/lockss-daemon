/*
 * $Id: TestSimpleBinarySemaphore.java,v 1.3 2002-11-19 23:31:29 tal Exp $
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
import org.lockss.daemon.*;
import org.lockss.test.*;


/**
 * Test class for org.lockss.test.TestSimpleBinarySemaphore
 */

public class TestSimpleBinarySemaphore extends LockssTestCase {
  public static Class testedClasses[] = {
    org.lockss.test.SimpleBinarySemaphore.class
  };

  public TestSimpleBinarySemaphore(String msg) {
    super(msg);
  }

  /** Gover gives a semaphore in a while */
  class Giver extends DoLater {
    SimpleBinarySemaphore sem;

    Giver(long waitMs, SimpleBinarySemaphore sem) {
      super(waitMs);
      this.sem = sem;
    }

    protected void doit() {
      sem.give();
    }
  }

  /** Give the smeaphore in a while */
  private Giver giveIn(long ms, SimpleBinarySemaphore sem) {
    Giver g = new Giver(ms, sem);
    g.start();
    return g;
  }

  public void testEmpty() {
    SimpleBinarySemaphore sem = new SimpleBinarySemaphore();
    Interrupter intr = null;
    try {
      intr = interruptMeIn(1000);
      if (sem.take()) {
	fail("take() of empty semaphore returned true");
      }
      intr.cancel();
    } finally {
      if (!intr.did()) {
	fail("take() of empty returned");
      }
    }
  }

  public void testFull() {
    SimpleBinarySemaphore sem = new SimpleBinarySemaphore();
    sem.give();
    Interrupter intr = null;
    try {
      intr = interruptMeIn(1000);
      if (!sem.take()) {
	fail("take() of already full semaphore returned false");
      }
      intr.cancel();
    } finally {
      if (intr.did()) {
	fail("timed out");
      }
    }
  }

  public void testGive() {
    SimpleBinarySemaphore sem = new SimpleBinarySemaphore();
    Interrupter intr = null;
    try {
      Date start = new Date();
      intr = interruptMeIn(2000);
      giveIn(500, sem);
      if (!sem.take()) {
	fail("take() of given() semaphore returned false");
      }
      if (TimerUtil.timeSince(start) < 500) {
	fail("take() returned before give()");
      }
      intr.cancel();
    } finally {
      if (intr.did()) {
	fail("give() didn't cause take() to return");
      }
    }
  }

  public void testNoWaitEmpty() {
    SimpleBinarySemaphore sem = new SimpleBinarySemaphore();
    Interrupter intr = null;
    try {
      intr = interruptMeIn(1000);
      if (sem.take(0)) {
	fail("take(0) of empty semaphore returned true");
      }
      intr.cancel();
    } finally {
      if (intr.did()) {
	fail("take(0) of empty didn't return");
      }
    }
  }

  public void testNoWaitFull() {
    SimpleBinarySemaphore sem = new SimpleBinarySemaphore();
    sem.give();
    Interrupter intr = null;
    try {
      intr = interruptMeIn(1000);
      if (!sem.take(0)) {
	fail("take(0) of full semaphore returned false");
      }
      intr.cancel();
    } finally {
      if (intr.did()) {
	fail("take(0) of full didn't return");
      }
    }
  }

  public void testTimedEmpty() {
    SimpleBinarySemaphore sem = new SimpleBinarySemaphore();
    Interrupter intr = null;
    try {
      Date start = new Date();
      intr = interruptMeIn(2000);
      if (sem.take(500)) {
	fail("take() of empty semaphore returned true");
      }
      long delay = TimerUtil.timeSince(start);
      if (delay < 500) {
	fail("take(500) returned early");
      }
      if (delay > 1000) {
	fail("take(500) returned late");
      }
      intr.cancel();
    } finally {
      if (intr.did()) {
	fail("take(500) of empty failed to timeout");
      }
    }
  }

  public void testTimedFull() {
    SimpleBinarySemaphore sem = new SimpleBinarySemaphore();
    sem.give();
    Interrupter intr = null;
    try {
      Date start = new Date();
      intr = interruptMeIn(2000);
      if (!sem.take(1000)) {
	fail("take(1000) of full returned false");
      }
      long delay = TimerUtil.timeSince(start);
      if (delay > 1000) {
	fail("take(1000) of full timed out");
      }
      intr.cancel();
    } finally {
      if (intr.did()) {
	fail("take(2000) of full failed to return");
      }
    }
  }

  public void testTimedGive() {
    SimpleBinarySemaphore sem = new SimpleBinarySemaphore();
    Interrupter intr = null;
    try {
      Date start = new Date();
      intr = interruptMeIn(2000);
      giveIn(500, sem);
      if (!sem.take(1000)) {
	fail("take(1000) of semaphore given() after 500 returned false");
      }
      long delay = TimerUtil.timeSince(start);
      if (delay < 500) {
	fail("take(1000), given in 500, returned early");
      }
      if (delay > 1000) {
	fail("take(1000), given in 500, timed out");
      }
      intr.cancel();
    } finally {
      if (intr.did()) {
	fail("take(1000), given in 500, neither returned nor timed out");
      }
    }
  }
}
