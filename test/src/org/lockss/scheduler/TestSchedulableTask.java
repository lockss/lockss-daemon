/*
 * $Id: TestSchedulableTask.java,v 1.3 2006-09-16 22:59:41 tlipkis Exp $
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
 * Test class for <code>org.lockss.scheduler.SchedulableTask</code>
 */

public class TestSchedulableTask extends LockssTestCase {
  public static Class testedClasses[] = {
    org.lockss.scheduler.SchedulableTask.class,
  };

  static Logger log = Logger.getLogger("TestBTScheduler");

  public void setUp() throws Exception {
    super.setUp();
    TimeBase.setSimulated();
  }

  public void tearDown() throws Exception {
    TimeBase.setReal();
    super.tearDown();
  }

//   public SchedulableTask(Deadline minStart,
// 		     Deadline deadline,
// 		     long estimatedDuration,
// 		     TaskCallback callback,
// 		     Object cookie) {}

  static SchedulableTask taskBefore(long deadline, int duration) {
    return new SchedulableTask(Deadline.in(0), Deadline.at(deadline),
			   duration, null, null);
  }

  static SchedulableTask taskBetween(long minStart,
				 long deadline,
				 int duration) {
    return new SchedulableTask(Deadline.at(minStart), Deadline.at(deadline),
			   duration, null, null);
  }

  public void testAccessors() {
    SchedulableTask t1 = taskBetween(100, 200, 50);
    assertIntervalEquals(100, 200, t1.getWindow());
    assertEquals(100, t1.getEarliestStart().getExpirationTime());
    assertEquals(200, t1.getLatestFinish().getExpirationTime());
  }

  public void testIsProper() {
    assertTrue(taskBetween(100, 200, 50).isProperWindow());
    assertFalse(taskBetween(200, 100, 50).isProperWindow());
    assertFalse(taskBetween(200, 200, 50).isProperWindow());
  }

  public void testFinishComparator() {
    TreeSet s = new TreeSet(SchedulableTask.latestFinishComparator());
    SchedulableTask t1, t2, t3, t4, t5;
    s.add(t1 = taskBetween(100, 200, 50));
    s.add(t2 = taskBetween(100, 100, 50));
    s.add(t3 = taskBetween(100, 400, 50));
    s.add(t4 = taskBetween(100, 87, 50));
    assertEquals(ListUtil.list(t4, t2, t1, t3), new ArrayList(s));
    // same sort order as t2, ensure comparator doesn't treat is as equal
    s.add(t5 = taskBetween(100, 100, 50));
    assertEquals(5, s.size());
  }

  public void testcurEst() {
    SchedulableTask t1 = taskBetween(100, 200, 50);
    assertEquals(50, t1.curEst());
  }

  public void testToString() {
    SchedulableTask t1 =
      new SchedulableTask(Deadline.at(100), Deadline.at(200),
			  50, null, "cookie");
    t1.toString();
    SchedulableTask t2 =
      new SchedulableTask(Deadline.at(100), Deadline.at(200),
			  50, null, null);
    t2.toString();
  }

  private void assertIntervalEquals(long begin, long end,
				    Interval intrvl) {
    assertEquals("begin", begin, deadlineVal(intrvl.getLB()));
    assertEquals("end", end, deadlineVal(intrvl.getUB()));
  }

  long deadlineVal(Object x) {
    return ((Deadline)x).getExpirationTime();
  }
}
