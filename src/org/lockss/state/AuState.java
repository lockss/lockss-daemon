/*
 * $Id: AuState.java,v 1.28 2007-06-28 07:14:24 smorabito Exp $
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


package org.lockss.state;

import java.util.*;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.util.LockssSerializable;
import org.lockss.util.Logger;
import org.lockss.util.TimeBase;

/**
 * AuState contains the state information for an au.
 */
public class AuState implements LockssSerializable {
  protected transient ArchivalUnit au;
  protected long lastCrawlTime;
  protected long lastTopLevelPoll;
  protected long lastTreeWalk;
  private transient HistoryRepository historyRepo;
  protected HashSet crawlUrls;
  protected int clockssSubscriptionStatus;
  protected double v3Agreement = -1.0;
  //Has there ever been a completed V3 poll?
  /** @deprecated */
  protected boolean hasV3Poll = false;

  private static final Logger log = Logger.getLogger("AuState");

  transient int urlUpdateCntr = 0;

  /** The number of updates between writing to file */
  static final int URL_UPDATE_LIMIT = 1;

  public AuState(ArchivalUnit au, HistoryRepository historyRepo) {
    this(au, -1, -1, -1, null, CLOCKSS_SUB_UNKNOWN, -1.0, historyRepo);
  }

  protected AuState(ArchivalUnit au, long lastCrawlTime, long lastTopLevelPoll,
                    long lastTreeWalk, HashSet crawlUrls,
		    int clockssSubscriptionStatus, double v3Agreement,
                    HistoryRepository historyRepo) {
    this.au = au;
    this.lastCrawlTime = lastCrawlTime;
    this.lastTopLevelPoll = lastTopLevelPoll;
    this.lastTreeWalk = lastTreeWalk;
    this.crawlUrls = crawlUrls;
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

  /**
   * Returns the last new content crawl time of the au.
   * @return the last crawl time in ms
   */
  public long getLastCrawlTime() {
    return lastCrawlTime;
  }

  /**
   * Returns the last top level poll time for the au.
   * @return the last poll time in ms
   */
  public long getLastTopLevelPollTime() {
    return lastTopLevelPoll;
  }

  /**
   * Returns the last treewalk time for the au.
   * @return the last treewalk time in ms
   */
  public long getLastTreeWalkTime() {
    return lastTreeWalk;
  }

  /**
   * Sets the last crawl time to the current time.  Saves itself to disk.
   */
  protected void newCrawlFinished() {
    lastCrawlTime = TimeBase.nowMs();
    historyRepo.storeAuState(this);
  }

  /**
   * Sets the last poll time to the current time.  Saves itself to disk.
   */
  public void newPollFinished() {
    lastTopLevelPoll = TimeBase.nowMs();
    historyRepo.storeAuState(this);
  }

  public void setV3Agreement(double d) {
    v3Agreement = d;
    historyRepo.storeAuState(this);
  }

  public double getV3Agreement() {
    return v3Agreement;
  }
  
  /** 
   * Deprecated in Daemon 1.25.  Please remove after a few
   * daemon releases.
   * 
   * @deprecated
   */
  public void hasV3Poll(boolean b) {
    hasV3Poll = b;
  }
  
  /** 
   * Deprecated in Daemon 1.25.  Please remove after a few
   * daemon releases.
   * 
   * @deprecated
   */
  public boolean hasV3Poll() {
    return hasV3Poll;
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
    sb.append("lastTopLevelPoll=");
    sb.append(new Date(lastTopLevelPoll));
    sb.append(", ");
    sb.append("clockssSub=");
    sb.append(clockssSubscriptionStatus);
    sb.append("]");
    return sb.toString();
  }
}
