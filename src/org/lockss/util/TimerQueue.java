/*
 * $Id: TimerQueue.java,v 1.1 2002-11-15 01:24:03 tal Exp $
 *

Copyright (c) 2000-2002 Board of Trustees of Leland Stanford Jr. University,
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
import java.io.*;
import java.util.*;

/** TimerQueue implements a queue of actions to be performed at a specific
 * time.
 */
public class TimerQueue implements Serializable {
  protected static Logger log = Logger.getLogger("TimerQueue");
  private static TimerQueue singleton = new TimerQueue();

  private PriorityQueue queue = new PriorityQueue();
  private TimerThread timerThread;

  /** Schedule an event.  At time <code>deadline</code>, <code>callback</code>
   * will be called with <code>cookie</code> as an argument.
   */
  public static boolean schedule(Deadline deadline, Callback callback,
				 Object cookie) {
    return singleton.add(new Request(deadline, callback, cookie));
  }

  private boolean add(Request req) {
    queue.put(req);
    return true;
  }

  static class Request implements Serializable, Comparable {
    Deadline deadline;
    Callback callback;
    Object cookie;

    Request(Deadline deadline, Callback callback, Object cookie) {
      this.deadline = deadline;
      this.callback = callback;
      this.cookie = cookie;
    }

    public int compareTo(Object o) {
      return deadline.compareTo(((Request)o).deadline);
    }
  }

  private void doNotify(Request req) {
    // tk - run these in a separate thread
    try {
      req.callback.timerExpired(req.cookie);
    } catch (Exception e) {
      log.error("Timer callback threw", e);
    }
  }    

  public void stop() {
    if (timerThread != null) {
      log.info("Stopping thread");
      timerThread.stopScheduler();
      timerThread = null;
    }
  }

  // tk add watchdog
  synchronized void ensureQRunner() {
    if (timerThread == null) {
      log.info("Starting thread");
      timerThread = new TimerThread("TimerQ");
      timerThread.start();
    }
  }

  // Hash thread
  private class TimerThread extends Thread {
    private boolean goOn = false;

    private TimerThread(String name) {
      super(name);
    }

    public void run() {
//       if (timerPriority > 0) {
// 	Thread.currentThread().setPriority(timerPriority);
//       }
      goOn = true;

      ProbabilisticTimer timeout = new ProbabilisticTimer(60000, 10000);
      while (goOn) {
	try {
	  Request req = (Request)queue.get(timeout);
	  if (req != null) {
	    doNotify(req);
	  }
	} catch (InterruptedException e) {
	  // no action - expected when stopping
	} catch (Exception e) {
	  log.error("Unexpected exception caught in TimerQueue thread", e);
	}
      }
      timerThread = null;
    }

    private void stopScheduler() {
      goOn = false;
      this.interrupt();
    }
  }
  /**
   * The TimerQueue.Callback interface defines the
   * method that will be called then a timer expires.
   */
  public interface Callback {
    /**
     * Called when the timer expires.
     * @param cookie  data supplied by caller to schedule()
     */
    public void timerExpired(Object cookie);
  }
}
