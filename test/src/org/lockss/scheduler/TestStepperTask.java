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

package org.lockss.scheduler;

import java.util.*;
import org.lockss.util.*;
import org.lockss.test.*;


/**
 * Test class for <code>org.lockss.scheduler.StepperTask</code>
 */

public class TestStepperTask extends LockssTestCase {
  public static Class testedClasses[] = {
    org.lockss.scheduler.StepTask.class,
    org.lockss.scheduler.StepperTask.class,
  };

  static Logger log = Logger.getLogger("TestStepperTask");

  public void setUp() throws Exception {
    super.setUp();
    TimeBase.setSimulated();
  }

  public void tearDown() throws Exception {
    TimeBase.setReal();
    super.tearDown();
  }

  static StepperTask taskBetween(long minStart,
				 long deadline,
				 int duration,
				 Stepper stepper) {
    return new StepperTask(Deadline.at(minStart), Deadline.at(deadline),
			   duration, null, null, stepper);
  }

  Stepper newSt(final boolean isFinished) {
    return new Stepper() {
	public int computeStep(int metric) {
	  return 0;
	}
	public boolean isFinished() {
	  return isFinished;
	}};
  }


  public void testStepper() {
    Stepper st = newSt(false);
    StepperTask t = taskBetween(100, 200, 50, st);
    assertEquals(st, t.getStepper());
    assertFalse(t.isBackgroundTask());
    assertFalse(t.isFinished());
    t.e = new Exception();
    assertTrue(t.isFinished());

    Stepper st2 = newSt(true);
    StepperTask t2 = taskBetween(100, 200, 50, st2);
    assertTrue(t2.isFinished());
  }

  public void testToString() {
    Stepper st = newSt(false);
    StepperTask t = taskBetween(100, 200, 50, st);
    t.toString();
    t.cookie = "foo";
    t.toString();
  }
}
