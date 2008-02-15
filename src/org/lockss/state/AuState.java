/*
 * $Id: AuState.java,v 1.33 2008-02-15 09:16:15 tlipkis Exp $
 */

/*

Copyright (c) 2000-2008 Board of Trustees of Leland Stanford Jr. University,
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


package org.lockss.state;

import java.util.*;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.util.*;
import org.lockss.daemon.*;
import org.lockss.crawler.CrawlerStatus;
import org.lockss.poller.v3.*;

/**
 * AuState contains the state information for an au.
 */
public class AuState implements LockssSerializable {

  private static final Logger log = Logger.getLogger("AuState");

  /** The number of updates between writing to file  (currently unused) */
  static final int URL_UPDATE_LIMIT = 1;

  public enum AccessType {OpenAccess, Subscription};

  // Persistent state vars
  protected long lastCrawlTime;		// last successful crawl
  protected long lastCrawlAttempt;
  protected String lastCrawlResultMsg;
  protected int lastCrawlResult;
  protected long lastTopLevelPoll;	// last completed poll
  protected long lastPollStart;		// last time a poll started
  protected String lastPollResultMsg;
  protected int lastPollResult;
  protected int clockssSubscriptionStatus;
  protected double v3Agreement = -1.0;
  protected AccessType accessType;

  protected transient long lastPollAttempt; // last time we attempted to
					    // start a poll

  // Non-persistent state vars

  // saves previous lastCrawl* state while crawl is running
  protected transient AuState previousCrawlState = null;

  // saves previous lastPoll* state while poll is running
  protected transient AuState previousPollState = null;

  // Runtime (non-state) vars
  protected transient ArchivalUnit au;
  private transient HistoryRepository historyRepo;

  // deprecated, kept for compatibility with old state files
  protected transient long lastTreeWalk = -1;
  // should be deprecated?
  protected HashSet crawlUrls;
  // deprecated, kept for compatibility with old state files
  /** @deprecated */
  protected transient boolean hasV3Poll = false;

  transient int urlUpdateCntr = 0;

  public AuState(ArchivalUnit au, HistoryRepository historyRepo) {
    this(au, -1, -1, -1, -1, -1, null,
	 CLOCKSS_SUB_UNKNOWN, -1.0, historyRepo);
  }

  public AuState(ArchivalUnit au,
		 long lastCrawlTime, long lastCrawlAttempt,
		 long lastTopLevelPoll, long lastPollStart,
		 long lastTreeWalk, HashSet crawlUrls,
		 int clockssSubscriptionStatus, double v3Agreement,
		 HistoryRepository historyRepo) {
    this(au,
	 lastCrawlTime, lastCrawlAttempt, -1, null,
	 lastTopLevelPoll, lastPollStart, -1, null,
	 lastTreeWalk,
	 crawlUrls, null, clockssSubscriptionStatus,
	 v3Agreement, historyRepo);
  }

  protected AuState(ArchivalUnit au,
		    long lastCrawlTime, long lastCrawlAttempt,
		    int lastCrawlResult, String lastCrawlResultMsg,
		    long lastTopLevelPoll, long lastPollStart,
		    int lastPollResult, String lastPollResultMsg,
		    long lastTreeWalk, HashSet crawlUrls,
		    AccessType accessType,
		    int clockssSubscriptionStatus, double v3Agreement,
		    HistoryRepository historyRepo) {
    this.au = au;
    this.lastCrawlTime = lastCrawlTime;
    this.lastCrawlAttempt = lastCrawlAttempt;
    this.lastCrawlResult = lastCrawlResult;
    this.lastCrawlResultMsg = lastCrawlResultMsg;
    this.lastTopLevelPoll = lastTopLevelPoll;
    this.lastPollStart = lastPollStart;
    this.lastPollResult = lastPollResult;
    this.lastPollResultMsg = lastPollResultMsg;
    this.lastTreeWalk = lastTreeWalk;
    this.crawlUrls = crawlUrls;
    this.accessType = accessType;
    this.clockssSubscriptionStatus = clockssSubscriptionStatus;
    this.v3Agreement = v3Agreement;
    this.historyRepo = historyRepo;
  }

  /**
   * Returns the au
   * @return the au
   */
  public ArchivalUnit getArchivalUnit() {
    return au;
  }

  public boolean isCrawlActive() {
    return previousCrawlState != null;
  }

  public boolean isPollActive() {
    return previousPollState != null;
  }


  /**
   * Returns the last new content crawl time of the au.
   * @return the last crawl time in ms
   */
  public long getLastCrawlTime() {
    return lastCrawlTime;
  }

  /**
   * Returns the last time a new content crawl was attempted.
   * @return the last crawl time in ms
   */
  public long getLastCrawlAttempt() {
    if (isCrawlActive()) {
      return previousCrawlState.getLastCrawlAttempt();
    }
    return lastCrawlAttempt;
  }

  /**
   * Returns the result code of the last new content crawl
   */
  public int getLastCrawlResult() {
    if (isCrawlActive()) {
      return previousCrawlState.getLastCrawlResult();
    }
    return lastCrawlResult;
  }

  /**
   * Returns the result of the last new content crawl
   */
  public String getLastCrawlResultMsg() {
    if (isCrawlActive()) {
      return previousCrawlState.getLastCrawlResultMsg();
    }
    if (lastCrawlResultMsg == null) {
      return CrawlerStatus.getDefaultMessage(lastCrawlResult);
    }
    return lastCrawlResultMsg;
  }

  /**
   * Returns the last time a top level poll completed.
   * @return the last poll time in ms
   */
  public long getLastTopLevelPollTime() {
    return lastTopLevelPoll;
  }

  /**
   * Returns the last time a poll started
   * @return the last poll time in ms
   */
  public long getLastPollStart() {
    if (isPollActive()) {
      return previousPollState.getLastPollStart();
    }
    return lastPollStart;
  }

  /**
   * Returns the last time a poll was attempted, since the last daemon
   * restart
   * @return the last poll time in ms
   */
  public long getLastPollAttempt() {
    return lastPollAttempt;
  }

  /**
   * Returns the result code of the last poll
   */
  public int getLastPollResult() {
    if (isPollActive()) {
      return previousPollState.getLastPollResult();
    }
    return lastPollResult;
  }

  /**
   * Returns the result of the last poll
   */
  public String getLastPollResultMsg() {
    if (isPollActive()) {
      return previousPollState.getLastPollResultMsg();
    }
    if (lastPollResultMsg == null) {
      try {
	return V3Poller.getStatusString(lastPollResult);
      } catch (IndexOutOfBoundsException e) {
	return null;
      }
    }
    return lastPollResultMsg;
  }

  /**
   * Returns the last treewalk time for the au.
   * @return the last treewalk time in ms
   */
  public long getLastTreeWalkTime() {
    return lastTreeWalk;
  }

  private void saveLastCrawl() {
    if (previousCrawlState != null) {
      log.error("saveLastCrawl() called twice", new Throwable());
    }
    previousCrawlState =
      new AuState(au,
		  lastCrawlTime, lastCrawlAttempt,
		  lastCrawlResult, lastCrawlResultMsg,
		  lastTopLevelPoll, lastPollStart,
		  lastPollResult, lastPollResultMsg,
		  lastTreeWalk, crawlUrls,
		  accessType,
		  clockssSubscriptionStatus, v3Agreement,
		  null);
  }

  /**
   * Sets the last time a crawl was attempted.
   */
  public void newCrawlStarted() {
    saveLastCrawl();
    lastCrawlAttempt = TimeBase.nowMs();
    lastCrawlResult = Crawler.STATUS_RUNNING_AT_CRASH;
    lastCrawlResultMsg = null;
    historyRepo.storeAuState(this);
  }

  /**
   * Sets the last crawl time to the current time.  Saves itself to disk.
   */
  protected void newCrawlFinished(int result, String resultMsg) {
    lastCrawlResultMsg = resultMsg;
    switch (result) {
    case Crawler.STATUS_SUCCESSFUL:
      lastCrawlTime = TimeBase.nowMs();
      // fall through
    default:
      lastCrawlResult = result;
      lastCrawlResultMsg = resultMsg;
      break;
    case Crawler.STATUS_ACTIVE:
      log.warning("Storing Active state", new Throwable());
      break;
    }
    previousCrawlState = null;
    historyRepo.storeAuState(this);
  }

  private void saveLastPoll() {
    if (previousPollState != null) {
      log.error("saveLastPoll() called twice", new Throwable());
    }
    previousPollState =
      new AuState(au,
		  lastCrawlTime, lastCrawlAttempt,
		  lastCrawlResult, lastCrawlResultMsg,
		  lastTopLevelPoll, lastPollStart,
		  lastPollResult, lastPollResultMsg,
		  lastTreeWalk, crawlUrls,
		  accessType,
		  clockssSubscriptionStatus, v3Agreement,
		  null);
  }

  /**
   * Sets the last time a poll was started.
   */
  public void pollStarted() {
    saveLastPoll();
    lastPollStart = TimeBase.nowMs();
    lastPollResult = Crawler.STATUS_RUNNING_AT_CRASH;
    lastPollResultMsg = null;
    historyRepo.storeAuState(this);
  }

  /**
   * Sets the last time a poll was attempted.
   */
  public void pollAttempted() {
    lastPollAttempt = TimeBase.nowMs();
  }

  /**
   * Sets the last poll time to the current time.  Saves itself to disk.
   */
  public void pollFinished(int result, String resultMsg) {
    lastPollResultMsg = resultMsg;
    switch (result) {
    case V3Poller.POLLER_STATUS_COMPLETE:
      lastTopLevelPoll = TimeBase.nowMs();
      // fall through
    default:
      lastPollResult = result;
      lastPollResultMsg = resultMsg;
      break;
    }
    previousPollState = null;
    historyRepo.storeAuState(this);
  }

  /**
   * Sets the last poll time to the current time.  Saves itself to disk.
   */
  public void pollFinished(int result) {
    pollFinished(result, null);
  }

  /**
   * Sets the last poll time to the current time.  Saves itself to disk.
   */
  public void pollFinished() {
    pollFinished(V3Poller.POLLER_STATUS_COMPLETE, null);
  }

  public void setV3Agreement(double d) {
    v3Agreement = d;
    historyRepo.storeAuState(this);
  }

  public double getV3Agreement() {
    return v3Agreement;
  }
  
  /**
   * Sets the last treewalk time to the current time.  Does not save itself
   * to disk, as it is desireable for the treewalk to run every time the
   * server restarts.  Consequently, it is non-persistent.
   */
  void setLastTreeWalkTime() {
    lastTreeWalk = TimeBase.nowMs();
  }

  /**
   * Gets the collection of crawl urls.
   * @return a {@link Collection}
   */
  public HashSet getCrawlUrls() {
    if (crawlUrls==null) {
      crawlUrls = new HashSet();
    }
    return crawlUrls;
  }

  /**
   * Alert the AuState that the crawl url collection has been updated.  Waits
   * until URL_UPDATE_LIMIT updates have been made, then writes the state to
   * file.
   * @param forceWrite forces state storage if true
   */
  public void updatedCrawlUrls(boolean forceWrite) {
    urlUpdateCntr++;
    if (forceWrite || (urlUpdateCntr >= URL_UPDATE_LIMIT)) {
      historyRepo.storeAuState(this);
      urlUpdateCntr = 0;
    }
  }

  public void setAccessType(AccessType accessType) {
    // don't store, this will get stored at end of crawl
    this.accessType = accessType;
  }

  public AccessType getAccessType() {
    return accessType;
  }

  // CLOCKSS status

  public static final int CLOCKSS_SUB_UNKNOWN = 0;
  public static final int CLOCKSS_SUB_YES = 1;
  public static final int CLOCKSS_SUB_NO = 2;
  public static final int CLOCKSS_SUB_INACCESSIBLE = 3;
  public static final int CLOCKSS_SUB_NOT_MAINTAINED = 4;

  /**
   * Return the CLOCKSS subscription status: CLOCKSS_SUB_UNKNOWN,
   * CLOCKSS_SUB_YES, CLOCKSS_SUB_NO
   */
  public int getClockssSubscriptionStatus() {
    return clockssSubscriptionStatus;
  }

  public String getClockssSubscriptionStatusString() {
    int status = getClockssSubscriptionStatus();
    switch (status) {
    case CLOCKSS_SUB_UNKNOWN: return "Unknown";
    case CLOCKSS_SUB_YES: return "Yes";
    case CLOCKSS_SUB_NO: return "No";
    case CLOCKSS_SUB_INACCESSIBLE: return "Inaccessible";
    case CLOCKSS_SUB_NOT_MAINTAINED: return "";
    default: return "Unknown status " + status;
    }
  }

  public void setClockssSubscriptionStatus(int val) {
    if (clockssSubscriptionStatus != val) {
      clockssSubscriptionStatus = val;
      historyRepo.storeAuState(this);
    }
  }

  public String toString() {
    StringBuffer sb = new StringBuffer();
    sb.append("[AuState: ");
    sb.append("lastCrawlTime=");
    sb.append(new Date(lastCrawlTime));
    sb.append(", ");
    sb.append("lastCrawlAttempt=");
    sb.append(new Date(lastCrawlAttempt));
    sb.append(", ");
    sb.append("lastCrawlResult=");
    sb.append(lastCrawlResult);
    sb.append(", ");
    sb.append("lastTopLevelPoll=");
    sb.append(new Date(lastTopLevelPoll));
    sb.append(", ");
    sb.append("clockssSub=");
    sb.append(clockssSubscriptionStatus);
    sb.append("]");
    return sb.toString();
  }
}
