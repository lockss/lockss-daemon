/*
 * $Id: FuncSchedService.java,v 1.3 2004-09-29 18:57:56 tlipkis Exp $
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
import java.io.*;
import org.lockss.daemon.*;
import org.lockss.util.*;
import org.lockss.test.*;
import org.lockss.plugin.*;


/**
 * Test class for org.lockss.scheduler.SchedService
 */

public class FuncSchedService extends LockssTestCase {

  public static Class testedClasses[] = {
    org.lockss.scheduler.SchedService.class,
    org.lockss.scheduler.TaskRunner.class,
  };

  private MockLockssDaemon theDaemon;

  private SchedService svc;

  public void setUp() throws Exception {
    super.setUp();
    theDaemon = getMockLockssDaemon();
    svc = theDaemon.getSchedService();
    svc.startService();
  }

  public void tearDown() throws Exception {
    svc.stopService();
    super.tearDown();
  }

  void waitUntilDone() throws Exception {
    Interrupter intr = null;
    try {
      intr = interruptMeIn(TIMEOUT_SHOULDNT);
      while (!svc.isIdle()) {
	TimerUtil.guaranteedSleep(100);
      }
      intr.cancel();
    } finally {
      if (intr.did()) {
	fail("Hasher timed out");
      }
    }
  }    

  BackgroundTask btask(long start, long end, double loadFactor,
		      TaskCallback cb) {
    return new BackgroundTask(Deadline.in(start), Deadline.in(end),
			   loadFactor, cb);
  }

  SchedulableTask taskToAbort = null;

  public void testOneBack() throws Exception {
    final List rec = new ArrayList();
    TaskCallback cb = new TaskCallback() {
	public void taskEvent(SchedulableTask task, Schedule.EventType event)
	    throws TaskCallback.Abort {
	  rec.add(new BERec(Deadline.in(0), (BackgroundTask)task, event));
	  if (event == Schedule.EventType.START) {
	    if (task == taskToAbort) {
	      throw new TaskCallback.Abort();
	    }
	  }
	}};
  
    BackgroundTask task = btask(100, 200, .1, cb);
    assertTrue(svc.scheduleTask(task));
    waitUntilDone();
    List exp = ListUtil.list(new BERec(task, Schedule.EventType.START),
			     new BERec(task, Schedule.EventType.FINISH));
    assertEquals(exp, rec);
    //    t1.taskIsFinished();
  }
 
  
  
  // Background event record
  class BERec {
    Deadline when;
    BackgroundTask task;
    Schedule.EventType event;
    BERec(Deadline when, BackgroundTask task, Schedule.EventType event) {
      this.when = when;
      this.task = task;
      this.event = event;
    }
    BERec(long when, BackgroundTask task, Schedule.EventType event) {
      this.when = Deadline.at(when);
      this.task = task;
      this.event = event;
    }
    BERec(BackgroundTask task, Schedule.EventType event) {
      this.when = Deadline.EXPIRED;
      this.task = task;
      this.event = event;
    }
    public boolean equals(Object obj) {
      if (obj instanceof BERec) {
	BERec o = (BERec)obj;
	return (!TimeBase.isSimulated() || when.equals(o.when)) &&
	  task.equals(o.task) &&
	  event == o.event;
      }
      return false;
    }
    public String toString() {
      return "[BERec: " + event + ", " + when + ", " + task + "]";
    }
  }


}
