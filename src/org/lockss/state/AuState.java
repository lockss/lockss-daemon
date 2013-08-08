/*
 * $Id: AuState.java,v 1.48.2.1 2013-08-08 05:51:42 tlipkis Exp $
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
import org.lockss.plugin.Plugin;
import org.lockss.app.*;
import org.lockss.util.*;
import org.lockss.daemon.*;
import org.lockss.crawler.CrawlerStatus;
import org.lockss.poller.v3.*;
import org.lockss.repository.*;

/**
 * AuState contains the state information for an au.
 */
public class AuState implements LockssSerializable {

  private static final Logger logger = Logger.getLogger("AuState");

  /** The number of updates between writing to file  (currently unused) */
  static final int URL_UPDATE_LIMIT = 1;

  public enum AccessType {OpenAccess, Subscription};


  // Persistent state vars
  protected long lastCrawlTime;		// last successful crawl
  protected long lastCrawlAttempt;
  protected String lastCrawlResultMsg;
  protected int lastCrawlResult;
  protected long lastTopLevelPoll;	// last completed PoR poll time
  protected long lastPollStart;		// last time a poll started
  protected int lastPollResult;         // ditto
  protected long pollDuration;		// average of last two PoRpoll durations
  protected int clockssSubscriptionStatus;
  protected double v3Agreement = -1.0;
  protected double highestV3Agreement = -1.0;
  protected AccessType accessType;
  protected SubstanceChecker.State hasSubstance;
  protected String substanceVersion;
  protected String metadataVersion;
  protected long lastContentChange;     // last time a new URL version created
  protected long lastPoPPoll;		// last completed PoP poll time
  protected int lastPoPPollResult;	// result of last PoP poll
  protected long lastLocalPoll;		// last completed Local poll time

  protected transient long lastPollAttempt; // last time we attempted to
					    // start a poll

  // Non-persistent state vars

  // saves previous lastCrawl* state while crawl is running
  protected transient AuState previousCrawlState = null;

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
  // No longer set, never had a non-standard value
  protected transient String lastPollResultMsg;   // result of last poll

  transient int urlUpdateCntr = 0;

  public AuState(ArchivalUnit au, HistoryRepository historyRepo) {
    this(au,
	 -1, // lastCrawlTime
	 -1, // lastCrawlAttempt
	 -1, // lastCrawlResult
	 null, // lastCrawlResultMsg,
	 -1, // lastTopLevelPoll
	 -1, // lastPollStart
	 -1, // lastPollresult
	 null, // lastPollresultMsg
	 0, // pollDuration
	 -1, // lastTreeWalk
	 null, // crawlUrls
	 null, // accessType
	 CLOCKSS_SUB_UNKNOWN, // clockssSubscriptionState
	 -1.0, // v3Agreement
	 -1.0, // highestV3Agreement
	 SubstanceChecker.State.Unknown,
	 null, // substanceVersion
	 null, // metadataVersion
	 0, // lastContentChange
	 -1, // lastPoPPoll
	 -1, // lastPoPPollResult
	 -1, // lastLocalPoll
	 historyRepo);
  }

  /**
   * DSHR believes this constructor is obsolete and should be removed
   */
  protected AuState(ArchivalUnit au,
		 long lastCrawlTime, long lastCrawlAttempt,
		 long lastTopLevelPoll, long lastPollStart,
		 long lastTreeWalk, HashSet crawlUrls,
		 int clockssSubscriptionStatus,
		 double v3Agreement, double highestV3Agreement,
		 HistoryRepository historyRepo) {
    this(au,
	 lastCrawlTime, lastCrawlAttempt, -1, null,
	 lastTopLevelPoll, lastPollStart, -1, null, 0,
	 lastTreeWalk,
	 crawlUrls, null, clockssSubscriptionStatus,
	 v3Agreement, highestV3Agreement,
	 SubstanceChecker.State.Unknown,
	 null,				// substanceFeatureVersion
	 null,				// metadataFeatureVersion
	 TimeBase.nowMs(),              // lastContentChange
	 -1, -1, -1,
	 historyRepo);
  }

  public AuState(ArchivalUnit au,
		 long lastCrawlTime, long lastCrawlAttempt,
		 int lastCrawlResult, String lastCrawlResultMsg,
		 long lastTopLevelPoll, long lastPollStart,
		 int lastPollResult, String lastPollResultMsg,
		 long pollDuration,
		 long lastTreeWalk, HashSet crawlUrls,
		 AccessType accessType,
		 int clockssSubscriptionStatus,
		 double v3Agreement,
		 double highestV3Agreement,
		 SubstanceChecker.State hasSubstance,
		 String substanceVersion,
		 String metadataVersion,
		 long lastContentChange,
		 long lastPoPPoll,
		 int lastPoPPollResult,
		 long lastLocalPoll,
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
    this.pollDuration = pollDuration;
    this.lastTreeWalk = lastTreeWalk;
    this.crawlUrls = crawlUrls;
    this.accessType = accessType;
    this.clockssSubscriptionStatus = clockssSubscriptionStatus;
    this.v3Agreement = v3Agreement;
    this.highestV3Agreement = highestV3Agreement;
    this.hasSubstance = hasSubstance;
    this.substanceVersion = substanceVersion;
    this.metadataVersion = metadataVersion;
    this.lastContentChange = lastContentChange;
    this.lastPoPPoll = lastPoPPoll;
    this.lastPoPPollResult = lastPoPPollResult;
    this.lastLocalPoll = lastLocalPoll;
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

  /**
   * Returns the date/time the au was created.
   * @return au creation time
   * If there is a Lockss repository exception, this method returns -1.
   */
  public long getAuCreationTime() {
    try {
      return historyRepo.getAuCreationTime();
    } catch (LockssRepositoryException e) {
      logger.error("getAuCreationTime: LockssRepositoryException: " + e.getMessage());
      return -1;
    }
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
   * @return last time a new version of a URL was created. Note that
   * only the first such change per crawl is noted.
   */
  public long getLastContentChange() {
    return lastContentChange;
  }

  /**
   * Returns true if the AU has ever successfully completed a new content
   * crawl
   */
  public boolean hasCrawled() {
    return getLastCrawlTime() >= 0;
  }

  /**
   * Returns the last time a PoR poll completed.
   * @return the last poll time in ms
   */
  public long getLastTopLevelPollTime() {
    return lastTopLevelPoll;
  }

  /**
   * Returns the last time a PoP poll completed.
   * @return the last poll time in ms
   */
  public long getLastPoPPoll() {
    return lastPoPPoll;
  }

  /**
   * Returns the last time a Local poll completed.
   * @return the last poll time in ms
   */
  public long getLastLocalPoll() {
    return lastLocalPoll;
  }

  /**
   * Returns the last PoP poll result.
   * @return the last poll time in ms
   */
  public int getLastPoPPollResult() {
    return lastPoPPollResult;
  }

  /**
   * Returns the last time a PoP or PoR poll completed.
   * @return the last poll time in ms
   */
  public long getLastTimePollCompleted() {
    return Math.max(lastTopLevelPoll, lastPoPPoll);
  }

  /**
   * Returns the last time a poll started
   * @return the last poll time in ms
   */
  public long getLastPollStart() {
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
    return lastPollResult;
  }

  /**
   * Returns the result of the last PoR poll
   */
  public String getLastPollResultMsg() {
    if (lastPollResult < 0) {
      return null;
    }
    try {
      return V3Poller.getStatusString(lastPollResult);
    } catch (IndexOutOfBoundsException e) {
      return "Poll result " + lastPollResult;
    }
  }

  /**
   * Returns the result of the last PoP poll
   */
  public String getLastPoPPollResultMsg() {
    if (lastPoPPollResult < 0) {
      return null;
    }
    try {
      return V3Poller.getStatusString(lastPoPPollResult);
    } catch (IndexOutOfBoundsException e) {
      return "Poll result " + lastPollResult;
    }
  }

  /**
   * Returns the running average poll duration, or 0 if unknown
   */
  public long getPollDuration() {
    return pollDuration;
  }

  /**
   * Update the poll duration to the average of current and previous
   * average.  Return the new average.
   */
  public long setPollDuration(long duration) {
    if (pollDuration == 0) {
      pollDuration = duration;
    } else {
      pollDuration = (pollDuration + duration + 1) / 2;
    }
    return pollDuration;
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
      logger.error("saveLastCrawl() called twice", new Throwable());
    }
    previousCrawlState = copy();
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
  public void newCrawlFinished(int result, String resultMsg) {
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
      logger.warning("Storing Active state", new Throwable());
      break;
    }
    previousCrawlState = null;
    historyRepo.storeAuState(this);
  }

  /**
   * Records a content change
   */
  public void contentChanged() {
    // Is a crawl in progress?
    if (previousCrawlState != null) {
      // Is the previous content change after the start of this
      // crawl?
      if (lastContentChange > lastCrawlAttempt) {
	// Yes - we already know this crawl changed things
	return;
      }
    }
    // Yes - this is the first change in this crawl.
    lastContentChange = TimeBase.nowMs();
    historyRepo.storeAuState(this);
  }

  private AuState copy() {
    return new AuState(au,
		       lastCrawlTime, lastCrawlAttempt,
		       lastCrawlResult, lastCrawlResultMsg,
		       lastTopLevelPoll, lastPollStart,
		       lastPollResult, lastPollResultMsg, pollDuration,
		       lastTreeWalk, crawlUrls,
		       accessType,
		       clockssSubscriptionStatus,
		       v3Agreement, highestV3Agreement,
		       hasSubstance,
		       substanceVersion, metadataVersion,
		       lastContentChange,
		       lastPoPPoll, lastPoPPollResult,
		       lastLocalPoll,
		       null);
  }

  /**
   * Sets the last time a poll was started.
   */
  public void pollStarted() {
    lastPollStart = TimeBase.nowMs();
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
  public void pollFinished(int result, V3Poller.PollVariant variant) {
    long now = TimeBase.nowMs();
    boolean complete = result == V3Poller.POLLER_STATUS_COMPLETE;
    switch (variant) {
    case PoR:
      if (complete) {
	lastTopLevelPoll = now;
      }
      lastPollResult = result;
      setPollDuration(TimeBase.msSince(lastPollAttempt));
      break;
    case PoP:
      if (complete) {
	lastPoPPoll = now;
      }
      lastPoPPollResult = result;
      break;
    case Local:
      if (complete) {
	lastLocalPoll = now;
      }
      break;
    }
    historyRepo.storeAuState(this);
  }

  /**
   * Sets the last poll time to the current time. Only for V1 polls.
   */
  public void pollFinished() {
    pollFinished(V3Poller.POLLER_STATUS_COMPLETE,
		 V3Poller.PollVariant.PoR); // XXX Bogus!
  }

  public void setV3Agreement(double d) {
    v3Agreement = d;
    if (v3Agreement > highestV3Agreement) {
      highestV3Agreement = v3Agreement;
    }
    historyRepo.storeAuState(this);
  }

  /**
   * @return agreement in last V3 poll
   */
  public double getV3Agreement() {
    return v3Agreement;
  }
  
  public double getHighestV3Agreement() {
    // We didn't used to track highest, so return last if no highest recorded
    return v3Agreement > highestV3Agreement ? v3Agreement : highestV3Agreement;
  }
  
  public void setSubstanceState(SubstanceChecker.State state) {
    hasSubstance = state;
    setFeatureVersion(Plugin.Feature.Substance,
		      au.getPlugin().getFeatureVersion(Plugin.Feature.Substance));
  }

  public SubstanceChecker.State getSubstanceState() {
    if (hasSubstance == null) {
      hasSubstance = SubstanceChecker.State.Unknown;
    }
    return hasSubstance;
  }

  public boolean hasNoSubstance() {
    return hasSubstance == SubstanceChecker.State.No;
  }

  /** Get the version string that was last set for the given feature */
  public String getFeatureVersion(Plugin.Feature feat) {
    switch (feat) {
    case Substance: ;return substanceVersion;
    case Metadata: ;return metadataVersion;
    default: return null;
    }
  }

  /** Set the version of the feature that was just used to process the
   * AU */
  public void setFeatureVersion(Plugin.Feature feat, String ver) {
    switch (feat) {
    case Substance: substanceVersion = ver; break;
    case Metadata: metadataVersion = ver; break;
    default:
    }
    storeAuState();
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

  public boolean isOpenAccess() {
    return accessType == AccessType.OpenAccess;
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

  public void storeAuState() {
    historyRepo.storeAuState(this);
  }

  /**
   * Avoid duplicating common strings
   */
  protected void postUnmarshal(LockssApp lockssContext) {
    lastPollResultMsg = null;		// no longer used
    StringPool featPool = StringPool.FEATURE_VERSIONS;
    if (substanceVersion != null) {
      substanceVersion = featPool.intern(substanceVersion);
    }
    if (metadataVersion != null) {
      metadataVersion = featPool.intern(metadataVersion);
    }
    StringPool cPool = CrawlerStatus.CRAWL_STATUS_POOL;
    lastCrawlResultMsg = cPool.intern(lastCrawlResultMsg);
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
