/*

Copyright (c) 2000-2023, Board of Trustees of Leland Stanford Jr. University

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are met:

1. Redistributions of source code must retain the above copyright notice,
this list of conditions and the following disclaimer.

2. Redistributions in binary form must reproduce the above copyright notice,
this list of conditions and the following disclaimer in the documentation
and/or other materials provided with the distribution.

3. Neither the name of the copyright holder nor the names of its contributors
may be used to endorse or promote products derived from this software without
specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
POSSIBILITY OF SUCH DAMAGE.

*/

package org.lockss.hasher;

import java.util.*;
import java.text.*;
import java.math.*;
import java.security.MessageDigest;

import org.lockss.config.Configuration;
import org.lockss.daemon.*;
import org.lockss.daemon.status.*;
import org.lockss.scheduler.*;
import org.lockss.util.*;
import org.lockss.app.*;
import org.lockss.plugin.*;

/**
 * Implementation of API for content and name hashing services that uses
 * the SchedService to execute tasks.
 */
public class HashSvcSchedImpl
  extends BaseLockssDaemonManager implements HashService, ConfigurableManager {

  static final String COL_SCHED = "sched";
  static final String COL_STATE = "state";
  static final String COL_AU = "au";
  static final String COL_CUS = "cus";
  static final String COL_TYPE = "type";
  static final String COL_DEADLINE = "deadline";
  static final String COL_ESTIMATE = "estimate";
  static final String COL_TIME_USED = "timeused";
  static final String COL_BYTES_HASHED = "bytesHashed";
  static final String COL_RATE = "rate";

  protected static Logger log = Logger.getLogger("HashSvcSchedImpl");

  public static final String HASH_STATUS_TABLE = "HashQ";

  private SchedService sched = null;
  private long estPadConstant = DEFAULT_ESTIMATE_PAD_CONSTANT;
  private long estPadPercent = DEFAULT_ESTIMATE_PAD_PERCENT;
  private List queue = new LinkedList();
  private HistoryList completed = new HistoryList(DEFAULT_COMPLETED_MAX);
  // lock object for both queue and completed
  private Object queueLock = new Object();
  private int hashStepBytes = DEFAULT_STEP_BYTES;
  private BigInteger totalBytesHashed = BigInteger.valueOf(0);
  private int reqCtr = 0;
  private long totalTime = 0;
  private long nameHashEstimate = DEFAULT_NAME_HASH_ESTIMATE;

  public HashSvcSchedImpl() {}

  /**
   * start the hash service.
   * @see org.lockss.app.LockssManager#startService()
   */
  public void startService() {
    super.startService();
    log.debug("startService()");
    sched = getDaemon().getSchedService();
    StatusService statSvc = getDaemon().getStatusService();
    statSvc.registerStatusAccessor(HASH_STATUS_TABLE, new Status());
    statSvc.registerOverviewAccessor(HASH_STATUS_TABLE, new HashOverview());
  }

  /**
   * stop the hash service
   * @see org.lockss.app.LockssManager#stopService()
   */
  public void stopService() {
    StatusService statSvc = getDaemon().getStatusService();
    statSvc.unregisterStatusAccessor(HASH_STATUS_TABLE);
    statSvc.unregisterOverviewAccessor(HASH_STATUS_TABLE);
    super.stopService();
  }

  public void setConfig(Configuration config, Configuration prevConfig,
			Configuration.Differences changedKeys) {
    estPadConstant = config.getTimeInterval(PARAM_ESTIMATE_PAD_CONSTANT,
                                            DEFAULT_ESTIMATE_PAD_CONSTANT);
    estPadPercent = config.getLong(PARAM_ESTIMATE_PAD_PERCENT,
				   DEFAULT_ESTIMATE_PAD_PERCENT);
    hashStepBytes = config.getInt(PARAM_STEP_BYTES, DEFAULT_STEP_BYTES);
    nameHashEstimate = config.getTimeInterval(PARAM_NAME_HASH_ESTIMATE,
					      DEFAULT_NAME_HASH_ESTIMATE);
    int cMax = config.getInt(PARAM_COMPLETED_MAX, DEFAULT_COMPLETED_MAX);
    if (changedKeys.contains(PARAM_COMPLETED_MAX) ) {
      synchronized (queueLock) {
	completed.setMax(config.getInt(PARAM_COMPLETED_MAX, cMax));
      }
    }
  }

  /**
   * Ask for the <code>CachedUrlSetHasher</code> to be
   * executed by the <code>hasher</code> before the expiration of
   * <code>deadline</code>, and the result provided to the
   * <code>callback</code>.
   * @param hasher   an instance of a <code>CachedUrlSetHasher</code>
   *                 representing a specific <code>CachedUrlSet</code>
   *                 and hash type
   * @param deadline the time by which the callback must have been
   *                 called.
   * @param callback the object whose <code>hashComplete()</code>
   *                 method will be called when hashing succeds
   *                 or fails.
   * @param cookie   used to disambiguate callbacks
   * @return <code>true</code> if the request has been queued,
   *         <code>false</code> if the resources to do it are not
   *         available.
   */
  public boolean scheduleHash(CachedUrlSetHasher hasher,
			      Deadline deadline,
			      Callback callback,
			      Object cookie) {
    HashTask task = new HashTask(hasher, deadline, callback, cookie);
    return scheduleTask(task);
  }

  /** Cancel all hashes on the specified AU.  Temporary until a better
   * cancel mechanism is implemented.
   * @param au the AU
   */
  public void cancelAuHashes(ArchivalUnit au) {
    synchronized (queueLock) {
      for (Iterator iter = queue.listIterator(); iter.hasNext(); ) {
	HashTask task = (HashTask)iter.next();
	if (task.urlset.getArchivalUnit() == au) {
	  task.cancel();
	  iter.remove();
	}
      }
    }
  }

  /** Return the average hash speed, or -1 if not known.
   * @param digest the hashing algorithm
   * @return hash speed in bytes/ms, or -1 if not known
   */
  public int getHashSpeed(MessageDigest digest) {
    if (sched == null) {
      throw new IllegalStateException("HashService has not been initialized");
    }
    if (totalTime < 5 * Constants.SECOND) {
      return -1;
    }
    int bpms =
      totalBytesHashed.divide(BigInteger.valueOf(totalTime)).intValue();
    return bpms;
  }

  /** Add the configured padding percentage, plus the constant */
  public long padHashEstimate(long estimate) {
    return estimate + ((estimate * estPadPercent) / 100) + estPadConstant;
  }

  private boolean scheduleTask(HashTask task) {
    if (sched == null) {
      throw new IllegalStateException("HashService has not been initialized");
    }
    task.setOverrunAllowed(true);
    if (sched.scheduleTask(task)) {
      task.hashReqSeq = ++reqCtr;
      synchronized (queueLock) {
	if (!task.finished) {
	  // Don't put on waiting queue if task has already finished (and
	  // been removed from waiting queue).
	  queue.add(task);
	}
      }
      return true;
    } else {
      return false;
    }
  }

  /** Test whether a hash request could be successfully sceduled before a
   * given deadline.
   * @param duration the estimated hash time needed.
   * @param when the deadline
   * @return true if such a request could be accepted into the scedule.
   */
  public boolean canHashBeScheduledBefore(long duration, Deadline when) {
    return sched.isTaskSchedulable(new DummyTask(when, duration));
  }

  /** Return true if the HashService has nothing to do.  Useful in unit
   * tests. */
  public boolean isIdle() {
    return queue.isEmpty();
  }

  List getQueueSnapshot() {
    synchronized (queueLock) {
      return new ArrayList(queue);
    }
  }
  List getCompletedSnapshot() {
    synchronized (queueLock) {
      return new ArrayList(completed);
    }
  }

  // HashTask
  class HashTask extends StepTask {
    CachedUrlSet urlset;
    HashService.Callback hashCallback;
    CachedUrlSetHasher urlsetHasher;
    int hashReqSeq = -1;
    boolean finished = false;
    long bytesHashed = 0;
    long unaccountedBytesHashed = 0;
    String typeString;
    boolean isRecalcEstimateHash = false;

    HashTask(CachedUrlSetHasher urlsetHasher,
	     Deadline deadline,
	     HashService.Callback hashCallback,
	     Object cookie) {

      super(Deadline.in(0), deadline, urlsetHasher.getEstimatedHashDuration(),
	    new TaskCallback() {
	      public void taskEvent(SchedulableTask task,
				    Schedule.EventType event) {
		if (log.isDebug2()) log.debug2("taskEvent: " + event);
		if (event == Schedule.EventType.FINISH) {
		  ((HashTask)task).doFinished();
		}
	      }
	    },
	    cookie);
      this.urlset = urlsetHasher.getCachedUrlSet();
      this.hashCallback = hashCallback;
      this.urlsetHasher = urlsetHasher;
      typeString = urlsetHasher.typeString();
      isRecalcEstimateHash = typeString.startsWith("E");
    }

    public String typeString() {
      // this must not reference the urlsetHasher, as it might have been
      // reset to null
      return typeString;
    }

    public int step(int n) {
      try {
	int res = urlsetHasher.hashStep(hashStepBytes);
	bytesHashed += res;
	unaccountedBytesHashed += res;
	return res;
      } catch (RuntimeException e) {
	log.error("hashStep threw", e);
	throw e;
      } catch (Exception e) {
	log.error("hashStep threw", e);
	throw new RuntimeException(e.toString(), e);
      }
    }

    protected void updateStats() {
      totalTime += unaccountedTime;
      totalBytesHashed =
	totalBytesHashed.add(BigInteger.valueOf(unaccountedBytesHashed));
      unaccountedBytesHashed = 0;
      super.updateStats();
     }

    public boolean isFinished() {
      return super.isFinished() || urlsetHasher.finished();
    }

    private void doFinished() {
      finished = true;
      long timeUsed = getTimeUsed();
      try {
	if (!urlsetHasher.finished()) {
	  urlsetHasher.abortHash();
	}
	urlsetHasher.storeActualHashDuration(timeUsed, getException());
      } catch (Exception e) {
	log.error("Hasher threw", e);
      }
      if (hashCallback != null) {
	try {
	  hashCallback.hashingFinished(urlset, timeUsed, cookie,
				       urlsetHasher, e);
	} catch (Exception e) {
	  log.error("Hash callback threw", e);
	}
      }
      // completed list for status only, don't hold on to caller's objects
      hashCallback = null;
      cookie = null;
      urlsetHasher = null;
      synchronized (queueLock) {
	queue.remove(this);
	completed.add(this);
      }
    }

    public String getShortText() {
      if (hashReqSeq != -1) {
	return "Hash " + hashReqSeq;
      } else {
	return "Hash";
      }
    }

    Object getState(boolean done) {
      if (!done) {
	return isStepping() ? TASK_STATE_RUN : TASK_STATE_WAIT;
      }
      if (getException() == null) {
	return TASK_STATE_DONE;
      } else if (getException() instanceof SchedService.Timeout) {
	if (isRecalcEstimateHash) {
	  // XXX add status accessor to Hasher interface
	  return TASK_STATE_RECALC_UNFINISHED;
	} else {
	  return TASK_STATE_TIMEOUT;
	}
      } else {
	return TASK_STATE_ERROR;
      }
    }

    public String toString() {
      StringBuffer sb = new StringBuffer();
      sb.append("[HTask:");
      sb.append(urlset);
      toStringCommon(sb);
      sb.append("]");
      return sb.toString();
    }
  }

  static class DummyTask extends StepTask {
    DummyTask(Deadline deadline,
	     long estimatedDuration) {
      super(Deadline.in(0), deadline, estimatedDuration, null, null);
    }

    public int step(int n) {
      return 0;
    }
  }

  // status table

  private static final List statusSortRules =
    ListUtil.list(new StatusTable.SortRule(COL_STATE, true),
		  new StatusTable.SortRule("sort", true),
		  new StatusTable.SortRule("sort2", true));

  static final String FOOT_IN = "Order in which requests were made.";

  static final String FOOT_OVER = "Red indicates overrun.";

  static final String FOOT_TITLE =
    "Pending requests are first in table, in the order they will be executed."+
    "  Completed requests follow, in reverse completion order " +
    "(most recent first).";

  private static final List statusColDescs =
    ListUtil.list(
		  new ColumnDescriptor(COL_SCHED, "Req",
				       ColumnDescriptor.TYPE_INT, FOOT_IN),
		  new ColumnDescriptor(COL_STATE, "State",
				       ColumnDescriptor.TYPE_STRING),
		  new ColumnDescriptor(COL_AU, "AU",
				       ColumnDescriptor.TYPE_STRING),
		  new ColumnDescriptor(COL_CUS, "Cached Url Set",
				       ColumnDescriptor.TYPE_STRING),
		  new ColumnDescriptor(COL_TYPE, "Type",
				       ColumnDescriptor.TYPE_STRING),
		  new ColumnDescriptor(COL_DEADLINE, "Deadline",
				       ColumnDescriptor.TYPE_DATE),
		  new ColumnDescriptor(COL_ESTIMATE, "Estimated",
				       ColumnDescriptor.TYPE_TIME_INTERVAL),
		  new ColumnDescriptor(COL_TIME_USED, "Used",
				       ColumnDescriptor.TYPE_TIME_INTERVAL,
				       FOOT_OVER),
		  new ColumnDescriptor(COL_BYTES_HASHED, "Bytes<br>Hashed",
				       ColumnDescriptor.TYPE_INT),
		  new ColumnDescriptor(COL_RATE, "Bytes/ms",
				       ColumnDescriptor.TYPE_STRING)
		  );


  private static final NumberFormat fmt_2dec = new DecimalFormat("0.00");
  private static final BigInteger big1000 = BigInteger.valueOf(1000);

  private class Status implements StatusAccessor {

    public String getDisplayName() {
      return "Hash Queue";
    }

    public void populateTable(StatusTable table) {
      table.setResortable(false);
      String key = table.getKey();
      table.setTitleFootnote(FOOT_TITLE);
      if (!table.getOptions().get(StatusTable.OPTION_NO_ROWS)) {
	table.setColumnDescriptors(statusColDescs);
	table.setDefaultSortRules(statusSortRules);
	table.setRows(getRows(key));
      }
      table.setSummaryInfo(getSummaryInfo(key));
    }

    public boolean requiresKey() {
      return false;
    }

    private List getRows(String key) {
      List table = new ArrayList();
      int ix = 0;
      for (ListIterator iter = getQueueSnapshot().listIterator();
	   iter.hasNext();) {
	table.add(makeRow((HashTask)iter.next(), false, ix++));
      }
      for (ListIterator iter = getCompletedSnapshot().listIterator();
	   iter.hasNext();) {
	Map row = makeRow((HashTask)iter.next(), true, 0);
	// if both parts of the table are present (ix is number of pending
	// requests), add a separator before the first displayed completed
	// request (which is the last one in the history list)
	if (ix != 0 && !iter.hasNext()) {
	  row.put(StatusTable.ROW_SEPARATOR, "");
	}
	table.add(row);
      }
      return table;
    }

    private Map makeRow(HashTask task, boolean done, int qpos) {
      Map row = new HashMap();
      row.put("sort",
	      new Long((done ? -task.getFinishDate().getTime() :
			task.getLatestFinish().getExpiration().getTime())));
      row.put("sort2", new Long(task.hashReqSeq));
      row.put(COL_SCHED, new Integer(task.hashReqSeq));
      row.put(COL_STATE, task.getState(done));
      row.put(COL_AU, task.urlset.getArchivalUnit().getName());
      row.put(COL_CUS, task.urlset.getSpec());
      row.put(COL_TYPE, task.typeString());
      row.put(COL_DEADLINE, task.getLatestFinish());
      row.put(COL_ESTIMATE, new Long(task.getOrigEst()));
      long timeUsed = task.getTimeUsed();
      Object used = new Long(timeUsed);
      if (task.hasOverrun()) {
	StatusTable.DisplayedValue val = new StatusTable.DisplayedValue(used);
	val.setColor("red");
	used = val;
      }
      row.put(COL_TIME_USED, used);
      row.put(COL_BYTES_HASHED, new Long(task.bytesHashed));
      if (timeUsed > 0 && task.bytesHashed > 0) {
	row.put(COL_RATE, hashRate(BigInteger.valueOf(task.bytesHashed),
				 timeUsed));
      }
      return row;
    }

    private List getSummaryInfo(String key) {
      List res = new ArrayList();
      res.add(new StatusTable.SummaryInfo("Total bytes hashed",
					  ColumnDescriptor.TYPE_INT,
					  totalBytesHashed));
      res.add(new StatusTable.SummaryInfo("Total hash time",
					  ColumnDescriptor.TYPE_TIME_INTERVAL,
					  new Long(totalTime)));
      if (totalTime != 0) {
	res.add(new StatusTable.SummaryInfo("Bytes/ms",
					    ColumnDescriptor.TYPE_STRING,
					    hashRate(totalBytesHashed,
						     totalTime)));
      }
      return res;
    }
  }

  String hashRate(BigInteger bytes, long time) {
    BigInteger bigTotal = BigInteger.valueOf(time);
    long bpms = bytes.divide(bigTotal).intValue();
    if (bpms >= 100) {
      return Long.toString(bpms);
    } else {
      long bpsec =
	bytes.multiply(big1000).divide(bigTotal).intValue();
      return fmt_2dec.format((double)bpsec / (double)1000);
    }
  }    

  static NumberFormat bigIntFmt = NumberFormat.getInstance();

  class HashOverview implements OverviewAccessor {

    public Object getOverview(String tableName, BitSet options) {
      List res = new ArrayList();

      String bytes = bigIntFmt.format(totalBytesHashed) + " bytes hashed";
      res.add(bytes);
      if (totalTime != 0) {
	res.add(" in " + StringUtil.timeIntervalToString(totalTime));
	res.add(" at " + hashRate(totalBytesHashed, totalTime) + " bytes/ms");
      }
      int wait = queue.size();
      if (wait != 0) {
	res.add(wait + " waiting");
      }
      String summ = StringUtil.separatedString(res, ", ");
      return new StatusTable.Reference(summ, HASH_STATUS_TABLE);
    }
  }

  static class TaskState implements Comparable {
    String name;
    int order;

    TaskState(String name, int order) {
      this.name = name;
      this.order = order;
    }

    public int compareTo(Object o) {
      return order - ((TaskState)o).order;
    }
    public String toString() {
      return name;
    }
  }
  static final TaskState TASK_STATE_RUN = new TaskState("Run", 1);
  static final TaskState TASK_STATE_WAIT = new TaskState("Wait", 2);
  static final TaskState TASK_STATE_DONE = new TaskState("Done", 3);

  static final StatusTable.DisplayedValue TASK_STATE_TIMEOUT =
    new StatusTable.DisplayedValue(new TaskState("Timeout", 3))
    .setColor("red");
  static final StatusTable.DisplayedValue TASK_STATE_RECALC_UNFINISHED =
    new StatusTable.DisplayedValue(new TaskState("Recalc Not Done", 3))
    .setColor("orange");
  static final StatusTable.DisplayedValue TASK_STATE_ERROR =
    new StatusTable.DisplayedValue(new TaskState("Error", 3))
    .setColor("red");
}
