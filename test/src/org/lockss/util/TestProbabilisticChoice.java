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
import java.io.*;
import java.text.*;
import junit.framework.TestCase;
import org.lockss.test.*;


/**
 * Test class for <code>org.lockss.util.ProbabilisticChoice</code>
 */

public class TestProbabilisticChoice extends LockssTestCase {
  public static Class testedClasses[] = {
    org.lockss.util.ProbabilisticChoice.class
  };
  static final int REPEAT_EACH = 10000;
  static final int N_TESTS = 10;
  static final double ALLOWABLE_DELTA = .05;

  public TestProbabilisticChoice(String msg) {
    super(msg);
  }

  private void doOneProb(double prob) {
    int cnt = 0;
    for (int ix = 0; ix < REPEAT_EACH; ix++) {
      if (ProbabilisticChoice.choose(prob)) {
	cnt++;
      }
    }
    double actual = (double)cnt/(double)REPEAT_EACH;
    NumberFormat f = new DecimalFormat("0.000");
    log.debug("target probability: " + f.format(prob) +
	      ", actual: " + f.format(actual) +
	      ", delta: " + f.format(Math.abs(prob - actual)));
    assertEquals(prob, actual, ALLOWABLE_DELTA);
  }

  public void testChoice() {
    Random random = new Random();
    for (int ix = 0; ix < N_TESTS; ix++) {
      doOneProb(random.nextDouble());
    }
  }
}
