/*
 * $Id: TimerQueue.java,v 1.30 2006-06-04 06:25:53 tlipkis Exp $
 *

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
import java.io.*;
import java.util.*;
import org.lockss.app.*;
import org.lockss.daemon.*;
import org.lockss.daemon.status.*;

/** TimerQueue implements a queue of actions to be performed at a specific
 * time.
 */
public class TimerQueue {
  static final String PRIORITY_PARAM_TIMERQUEUE = "TimerQueue";
  static final int PRIORITY_DEFAULT_TIMERQUEUE = Thread.NORM_PRIORITY + 1;

  protected static Logger log = Logger.getLogger("TimerQueue");
  private static TimerQueue singleton = new TimerQueue();

  private PriorityQueue queue = new PriorityQueue();
  private TimerThread timerThread;
  private boolean needResort = false;

  /** Schedule an event.  At time <code>deadline</code>, <code>callback</code>
   * will be called with <code>cookie</code> as an argument.
   * @return a TimerQueue.Request object, which can be used to cancel the
   * request.
   */
  public static Request schedule(Deadline deadline, Callback callback,
				 Object cookie) {
    return singleton.add(deadline, callback, cookie);
  }

  /** Cancel a previously scheduled request.
   * @param req the {@link TimerQueue.Request} object returned from an
   * earlier call to schedule()
   */
  public static void cancel(Request req) {
    singleton.cancelReq(req);
  }

  /** Wait until all requests whose time has been reached to execute.
   * Useful when running in simulated time mode.
   */
  public static void runAllExpired() {
    singleton.runAllExpired0();
  }

  /** Called by LockssTestCase.tearDown() to prevent events from happening
   * after tests complete.  tk - fix when this is made a LockssManager. */
  public static void stopTimerQueue() {
    singleton.stop();
    singleton.queue = new PriorityQueue();
  }

  private Request add(Deadline deadline, Callback callback, Object cookie) {
    Request req = new Request(deadline, callback, cookie);
    req.deadline.registerCallback(req.deadlineCb);
    queue.put(req);
    startOrKickThread();
    return req;
  }

  private void cancelReq(Request req) {
    req.cancelled = true;
    req.deadline = Deadline.EXPIRED;
    // no need to resort or notify thread, since we don't care if the
    // cancelled request isn't noticed immediately
  }

  private void runAllExpired0() {
    Request req = (Request)queue.peek();
    if (req == null || !req.deadline.expired()) {
      // queue is empty, or there are no unexpired requests
      return;
    }
    // Need to wait until expired requests run.  Easiest way is to put
    // our own request on the queue (which will come after any with earlier
    // or equal deadlines) and wait for it to happen.
    final BinarySemaphore sem = new BinarySemaphore();
    add(Deadline.in(0),
	new Callback() {
	  public void timerExpired(Object cookie) {
	    sem.give();
	  }},
	null);
    try {
      sem.take(Deadline.MAX);
    } catch (InterruptedException e) {
    }
  }


  /** Timer Request element; only used to cancel a request. */
  public class Request implements Comparable {
    private Deadline deadline;
    private Callback callback;
    private Object cookie;
    private Deadline.Callback deadlineCb;
    private volatile boolean cancelled = false;

    private Request(Deadline deadline, Callback callback, Object cookie) {
      this.deadline = deadline;
      this.callback = callback;
      this.cookie = cookie;
      deadlineCb = new Deadline.Callback() {
	  public void changed(Deadline deadline) {
	    deadlineChanged(deadline);
	  }};
    }

    public Deadline getDeadline() {
      return deadline;
    }

    public int compareTo(Object o) {
      return deadline.compareTo(((Request)o).deadline);
    }
  }

  private void doNotify(Request req) {
    if (!req.cancelled) {
      // tk - run these in a separate thread
      req.deadline.unregisterCallback(req.deadlineCb);
      try {
	req.callback.timerExpired(req.cookie);
      } catch (Exception e) {
	log.error("Timer callback threw", e);
      }
    }
    queue.remove(req);
  }

  public void stop() {
    if (timerThread != null) {
      log.debug("Stopping thread");
      timerThread.stopTimer();
      timerThread = null;
    }
  }

  // tk add watchdog
  synchronized void startOrKickThread() {
    if (timerThread == null) {
      log.debug("Starting thread");
      timerThread = new TimerThread("TimerQ");
      timerThread.start();
      timerThread.waitRunning();
    } else {
      threadWait.give();
    }
  }

  private void deadlineChanged(Deadline deadline) {
    // remember that we need to resort the queue
    needResort = true;
    // but do it now only if it would change the current sleep
    if (deadline == threadWaitingForDeadline ||
	(threadWaitingUntil != 0 && deadline.getExpirationTime() < threadWaitingUntil)) {
      resort();
      if (timerThread != null) {
	threadWait.give();
      }
    }
  }

  private void resort() {
    needResort = false;
    queue.sort();
  }

  // Timer thread.

  BinarySemaphore threadWait = new BinarySemaphore();
  private Deadline threadWaitingForDeadline;
  private long threadWaitingUntil = 0;

  // Timer callbacks are currently called in this thread, so hangs are
  // possible.  However, we don't need an explicit watchdog mechanism
  // because the WatchdogService is currently implemented using the
  // TimerQueue.  If this thread gets hung, the platform watchdog will go
  // off.  (The LockssThread watchdog cannot be used here because it relies
  // on the TimerQueue not being hung.)

  private class TimerThread extends LockssThread {
    private volatile boolean goOn = true;

    private TimerThread(String name) {
      super(name);
    }

    public void lockssRun() {
      triggerWDogOnExit(true);
      setPriority(PRIORITY_PARAM_TIMERQUEUE, PRIORITY_DEFAULT_TIMERQUEUE);
      nowRunning();

      while (goOn) {
	try {
	  Request req = (Request)queue.peekWait(Deadline.in(Constants.MINUTE));
	  if (req != null) {
	    threadWaitingForDeadline = req.deadline;
	    if (!threadWaitingForDeadline.expired()) {
	      threadWaitingUntil = threadWaitingForDeadline.getExpirationTime();
	      threadWait.take(threadWaitingForDeadline);
	      threadWaitingUntil = 0;
	    }
	    threadWaitingForDeadline = null;
	    // Check that this request is still at the head of the queue.
	    // Not strictly necessary, but makes behavior more predictable
	    // for testing.
	    Request newHead = (Request)queue.peek();
 	    if (req == newHead && req.deadline.expired()) {
	      doNotify(req);
	    }
	  }
	  if (needResort) {
	    resort();
	  }
	} catch (InterruptedException e) {
	  // no action - expected when stopping or when queue reordered
	} catch (Exception e) {
	  log.error("Unexpected exception caught in TimerQueue thread", e);
	}
      }
      if (!goOn) {
	triggerWDogOnExit(false);
      }
    }

    private void stopTimer() {
      goOn = false;
      this.interrupt();
    }
  }

  // status table

  private static final List statusSortRules =
    ListUtil.list(new StatusTable.SortRule("Time", true));

  static final String FOOT_IN = "Order in which requests were made.";

  static final String FOOT_OVER = "Red indicates overrun.";

  static final String FOOT_TITLE =
    "Pending requests are first in table, in the order they will be executed."+
    "  Completed requests follow, in reverse completion order " +
    "(most recent first).";

  private static final List statusColDescs =
    ListUtil.list(
		  new ColumnDescriptor("Time", "Time",
				       ColumnDescriptor.TYPE_DATE),
		  new ColumnDescriptor("In", "In",
				       ColumnDescriptor.TYPE_STRING),
		  new ColumnDescriptor("Callback", "Callback",
				       ColumnDescriptor.TYPE_STRING),
		  new ColumnDescriptor("Cookie", "Cookie",
				       ColumnDescriptor.TYPE_STRING)
		  );

  private static class Status implements StatusAccessor {
    private TimerQueue timerQ;

    Status(TimerQueue tq) {
      this.timerQ = tq;
    }

    public String getDisplayName() {
      return "Timer Queue";
    }

    public void populateTable(StatusTable table) {
      if (!table.getOptions().get(StatusTable.OPTION_NO_ROWS)) {
	table.setColumnDescriptors(statusColDescs);
	table.setDefaultSortRules(statusSortRules);
	table.setRows(getRows(table));
      }
      table.setSummaryInfo(getSummaryInfo());
    }

    public boolean requiresKey() {
      return false;
    }

    private List getRows(StatusTable table) {
      List q = timerQ.queue.copyAsList();
      List rows = new ArrayList(q.size());
      int ix = 0;
      for (Iterator iter = q.iterator(); iter.hasNext();) {
	TimerQueue.Request req = (TimerQueue.Request)iter.next();
	if (!req.cancelled ||
	    table.getOptions().get(StatusTable.OPTION_DEBUG_USER)) {
	  rows.add(makeRow(req));
	}
      }
      return rows;
    }

    private Map makeRow(TimerQueue.Request req) {
      Map row = new HashMap();
      row.put("Time", req.deadline);
      if (!req.deadline.equals(Deadline.MAX)) {
	row.put("In", StringUtil.timeIntervalToString(TimeBase.msUntil(req.deadline.getExpirationTime())));
      }
      if (req.callback != null) {
	row.put("Callback", req.callback.toString());
      }
      if (req.cookie != null) {
	row.put("Cookie", req.cookie.toString());
      }
      return row;
    }

    private List getSummaryInfo() {
      List res = new ArrayList();
//       res.add(new StatusTable.SummaryInfo("Total bytes hashed",
// 					  ColumnDescriptor.TYPE_INT,
// 					  totalBytesHashed));
      return res;
    }

  }

  /**
   * The TimerQueue.Callback interface defines the
   * method that will be called when a timer expires.
   */
  public interface Callback {
    /**
     * Called when the timer expires.
     * @param cookie  data supplied by caller to schedule()
     */
    public void timerExpired(Object cookie);
  }

  /** A little manager class just to register a status accessor. */
  public static class Manager extends BaseLockssDaemonManager {
    public void startService() {
      super.startService();
      getDaemon().getStatusService().
	registerStatusAccessor("TimerQ", new Status(singleton));
    }

    public void stopService() {
      getDaemon().getStatusService().unregisterStatusAccessor("TimerQ");
      super.stopService();
    }

  }

}
