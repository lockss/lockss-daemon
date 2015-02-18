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

import java.util.*;
import org.lockss.test.*;


/**
 * Test class for <code>org.lockss.util.LockssRandom</code>
 */
public class TestLockssRandom extends LockssTestCase {
  public static Class testedClasses[] = {
    org.lockss.util.LockssRandom.class
  };

  LockssRandom rand;

  public void setUp() throws Exception {
    super.setUp();
    rand = new LockssRandom();
  }

  public void testNextBits(int bits) {
    boolean upper = false;
    boolean lower = false;
    long max;
    long mid;
    String lrange, urange;
    if (bits < 64) {
      max = (((long)1) << bits) - 1;
      mid = (max / 2);
      lrange = "0 - " + mid;
    } else {
      max = Long.MAX_VALUE;
      mid = 0;
      lrange = Long.MIN_VALUE + " - 0";
    }
    urange = mid + " - " + max;
    for (int ix = 0; ix < 1000; ix++) {
      long val = rand.nextBits(bits);
      assertTrue("nextBits(" + bits + ") > max: " + val + " > " + max,
		 val <= max);
      if (val <= mid) {
	lower = true;
      } else {
	upper = true;
      }
    }
    assertTrue("No values in range " + lrange, lower);
    assertTrue("No values in range " + urange, upper);
  }

  public void testNextBits() {
    testNextBits(1);
    testNextBits(2);
    testNextBits(4);
    testNextBits(8);
    testNextBits(16);
    testNextBits(31);
    testNextBits(32);
    testNextBits(33);
    testNextBits(63);
    testNextBits(64);
  }

  public void testNextLong() {
    boolean upper = false;
    boolean lower = false;
    long max = Long.MAX_VALUE;
    long mid = 0;
    String lrange = Long.MIN_VALUE + " - 0";
    String urange = mid + " - " + max;
    for (int ix = 0; ix < 1000; ix++) {
      long val = rand.nextLong();
      if (val <= mid) {
	lower = true;
      } else {
	upper = true;
      }
    }
    assertTrue("No values in range " + lrange, lower);
    assertTrue("No values in range " + urange, upper);
  }

}
