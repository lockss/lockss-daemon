/*
 * $Id$
 */

/*

Copyright (c) 2000-2015 Board of Trustees of Leland Stanford Jr. University,
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
import org.lockss.plugin.*;
import org.lockss.app.*;
import org.lockss.util.*;
import org.lockss.daemon.*;
import org.lockss.crawler.CrawlerStatus;
import org.lockss.poller.v3.*;
import org.lockss.repository.*;

/**
 * AuState contains the state information for an au.<br>
 *
 * In this class, default values *will* be present after deserialization,
 * for fields that did not exist when the object was serialized.  {@see
 * org.lockss.util.XStreamSerializer#CLASSES_NEEDING_CONSTRUCTOR}
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
  protected long lastMetadataIndex;     // last time metadata extraction
					// completed
  protected long lastContentChange;     // last time a new URL version created
  protected long lastPoPPoll;		// last completed PoP poll time
  protected int lastPoPPollResult;	// result of last PoP poll
  protected long lastLocalHashScan;	// last completed local hash scan
  protected int numAgreePeersLastPoR = -1; // Number of agreeing peers last PoR
  protected int numWillingRepairers = -1; // Number of willing repairers
  protected int numCurrentSuspectVersions = -1; // # URLs w/ current version suspect
  protected List<String> cdnStems;	// URL stems of content on hosts
					// not predicted by permission URLs

  protected transient long lastPollAttempt; // last time we attempted to
					    // start a poll

  // Note - the copy constructor (AuState(ArchivalUnit, HistoryRepository))
  // must be udpated whenever a new persistent field is added

  // Non-persistent state vars

  // saves previous lastCrawl* state while crawl is running
  protected transient AuState previousCrawlState = null;

  // Runtime (non-state) vars
  protected transient ArchivalUnit au;
  private transient HistoryRepository historyRepo;
  private transient boolean needSave = false;
  private transient int batchSaveDepth = 0;

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

  /** No-arg constructor required for pure-Java deserialization. */
  private AuState() {
  }

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
	 -1, // lastMetadataIndex
	 0, // lastContentChange
	 -1, // lastPoPPoll
	 -1, // lastPoPPollResult
	 -1, // lastLocalHashScan
	 0,  // numAgreePeersLastPoR
	 0,  // numWillingRepairers
	 0,  // numCurrentSuspectVersions
	 null, // cdnStems
	 historyRepo);
  }

  /** Copy constructor adds au and historyRepo, used after state loaded
   * from file.  This constructor *must* be updated whenever new fields are
   * added to the calss. */
  public AuState(AuState aus, ArchivalUnit au, HistoryRepository historyRepo) {
    this.au = au;
    this.historyRepo = historyRepo;
    this.lastCrawlTime = aus.lastCrawlTime;
    this.lastCrawlAttempt = aus.lastCrawlAttempt;
    this.lastCrawlResult = aus.lastCrawlResult;
    this.lastCrawlResultMsg = aus.lastCrawlResultMsg;
    this.lastTopLevelPoll = aus.lastTopLevelPoll;
    this.lastPollStart = aus.lastPollStart;
    this.lastPollResult = aus.lastPollResult;
    this.lastPollResultMsg = aus.lastPollResultMsg;
    this.pollDuration = aus.pollDuration;
    this.lastTreeWalk = aus.lastTreeWalk;
    this.crawlUrls = aus.crawlUrls;
    this.accessType = aus.accessType;
    this.clockssSubscriptionStatus = aus.clockssSubscriptionStatus;
    this.v3Agreement = aus.v3Agreement;
    this.highestV3Agreement = aus.highestV3Agreement;
    this.hasSubstance = aus.hasSubstance;
    this.substanceVersion = aus.substanceVersion;
    this.metadataVersion = aus.metadataVersion;
    this.lastMetadataIndex = aus.lastMetadataIndex;
    this.lastContentChange = aus.lastContentChange;
    this.lastPoPPoll = aus.lastPoPPoll;
    this.lastPoPPollResult = aus.lastPoPPollResult;
    this.lastLocalHashScan = aus.lastLocalHashScan;
    this.numAgreePeersLastPoR = aus.numAgreePeersLastPoR;
    this.numWillingRepairers = aus.numWillingRepairers;
    this.numCurrentSuspectVersions = aus.numCurrentSuspectVersions;
    this.cdnStems = aus.cdnStems;

    if (cdnStems != null) {
      flushAuCaches();
    }
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
		 long lastMetadataIndex,
		 long lastContentChange,
		 long lastPoPPoll,
		 int lastPoPPollResult,
		 long lastLocalHashScan,
		 int numAgreePeersLastPoR,
		 int numWillingRepairers,
		 int numCurrentSuspectVersions,
		 List<String> cdnStems,
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
    this.lastMetadataIndex = lastMetadataIndex;
    this.lastContentChange = lastContentChange;
    this.lastPoPPoll = lastPoPPoll;
    this.lastPoPPollResult = lastPoPPollResult;
    this.lastLocalHashScan = lastLocalHashScan;
    this.numAgreePeersLastPoR = numAgreePeersLastPoR;
    this.numWillingRepairers = numWillingRepairers;
    this.numCurrentSuspectVersions = numCurrentSuspectVersions;
    this.cdnStems = cdnStems;
    this.historyRepo = historyRepo;

    if (cdnStems != null) {
      flushAuCaches();
    }
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
   * @return last time metadata indexing was completed.
   */
  public long getLastMetadataIndex() {
    return lastMetadataIndex;
  }

  /**
   * Set last time metadata indexing was completed.
   */
  public synchronized void setLastMetadataIndex(long time) {
    lastMetadataIndex = time;
    needSave();
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
   * Returns the last time a Local hash scan completed.
   * @return the last scan time in ms
   */
  public long getLastLocalHashScan() {
    return lastLocalHashScan;
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
      return "Poll result " + lastPoPPollResult;
    }
  }

  public int getNumAgreePeersLastPoR() {
    return numAgreePeersLastPoR;
  }

  public synchronized void setNumAgreePeersLastPoR(int n) {
    if (numAgreePeersLastPoR != n) {
      numAgreePeersLastPoR = n;
      needSave();
    }
  }

  public int getNumWillingRepairers() {
    return numWillingRepairers;
  }

  public synchronized void setNumWillingRepairers(int n) {
    if (numWillingRepairers != n) {
      if (logger.isDebug3()) {
	logger.debug3("setNumWillingRepairers: " +
		      numWillingRepairers + " -> " + n);
      }
      numWillingRepairers = n;
      needSave();
    }
  }

  public int getNumCurrentSuspectVersions() {
    return numCurrentSuspectVersions;
  }

  public synchronized void setNumCurrentSuspectVersions(int n) {
    if (numCurrentSuspectVersions != n) {
      if (logger.isDebug3()) {
	logger.debug3("setNumCurrentSuspectVersions: " +
		      numCurrentSuspectVersions + " -> " + n);
      }
      numCurrentSuspectVersions = n;
      needSave();
    }
  }

  public synchronized void incrementNumCurrentSuspectVersions(int n) {
    if (numCurrentSuspectVersions < 0) {
      // If -1, this object was deserialized from a file written before
      // this field existed, so it needs to be computed.
      recomputeNumCurrentSuspectVersions();
    }
    setNumCurrentSuspectVersions(getNumCurrentSuspectVersions() + n);
  }

  public synchronized int recomputeNumCurrentSuspectVersions() {
    int n = 0;
    if (AuUtil.hasSuspectUrlVersions(au)) {
      AuSuspectUrlVersions asuv = AuUtil.getSuspectUrlVersions(au);
      n = asuv.countCurrentSuspectVersions(au);
    }
    logger.debug2("recomputeNumCurrentSuspectVersions(" + au + "): " +
		  numCurrentSuspectVersions + " -> " + n);
    setNumCurrentSuspectVersions(n);
    return n;
  }

  public List<String> getCdnStems() {
    return cdnStems == null ? (List<String>)Collections.EMPTY_LIST : cdnStems;
  }

  public synchronized void setCdnStems(List<String> stems) {
    if (!getCdnStems().equals(stems)) {
      cdnStems = stems == null ? null : ListUtil.minimalArrayList(stems);
      flushAuCaches();
      needSave();
    }
  }

  public synchronized void addCdnStem(String stem) {
    if (!getCdnStems().contains(stem)) {
      if (cdnStems == null) {
	cdnStems = new ArrayList(4);
      }
      cdnStems.add(stem);
      flushAuCaches();
      AuUtil.getDaemon(au).getPluginManager().addAuStem(stem, au);
      needSave();
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
  public synchronized void newCrawlStarted() {
    saveLastCrawl();
    lastCrawlAttempt = TimeBase.nowMs();
    lastCrawlResult = Crawler.STATUS_RUNNING_AT_CRASH;
    lastCrawlResultMsg = null;
    needSave();
  }

  /**
   * Sets the last crawl time to the current time.  Saves itself to disk.
   */
  public synchronized void newCrawlFinished(int result, String resultMsg) {
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
    needSave();
  }

  /**
   * Records a content change
   */
  public synchronized void contentChanged() {
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
    needSave();
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
		       lastMetadataIndex,
		       lastContentChange,
		       lastPoPPoll, lastPoPPollResult,
		       lastLocalHashScan,
		       numAgreePeersLastPoR,
		       numWillingRepairers,
		       numCurrentSuspectVersions,
		       cdnStems,
		       null);
  }

  /**
   * Sets the last time a poll was started.
   */
  public synchronized void pollStarted() {
    lastPollStart = TimeBase.nowMs();
    needSave();
  }

  /**
   * Sets the last time a poll was attempted.
   */
  public synchronized void pollAttempted() {
    lastPollAttempt = TimeBase.nowMs();
    needSave();
  }

  /**
   * Sets the last poll time to the current time.
   */
  public synchronized void pollFinished(int result,
					V3Poller.PollVariant variant) {
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
	lastLocalHashScan = now;
      }
      break;
    }
    needSave();
  }

  /**
   * Sets the last poll time to the current time. Only for V1 polls.
   */
  public void pollFinished() {
    pollFinished(V3Poller.POLLER_STATUS_COMPLETE,
		 V3Poller.PollVariant.PoR); // XXX Bogus!
  }

  public synchronized void setV3Agreement(double d) {
    v3Agreement = d;
    if (v3Agreement > highestV3Agreement) {
      highestV3Agreement = v3Agreement;
    }
    needSave();
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
  
  public synchronized void setSubstanceState(SubstanceChecker.State state) {
    batchSaves();
    setFeatureVersion(Plugin.Feature.Substance,
		      au.getPlugin().getFeatureVersion(Plugin.Feature.Substance));
    if (getSubstanceState() != state) {
      hasSubstance = state;
      needSave();
    }
    unBatchSaves();
  }

  public SubstanceChecker.State getSubstanceState() {
    if (hasSubstance == null) {
      return SubstanceChecker.State.Unknown;
    }
    return hasSubstance;
  }

  public boolean hasNoSubstance() {
    return hasSubstance == SubstanceChecker.State.No;
  }

  /** Get the version string that was last set for the given feature */
  public String getFeatureVersion(Plugin.Feature feat) {
    switch (feat) {
    case Substance: return substanceVersion;
    case Metadata: return metadataVersion;
    default: return null;
    }
  }

  /** Set the version of the feature that was just used to process the
   * AU */
  public synchronized void setFeatureVersion(Plugin.Feature feat, String ver) {
    String over = getFeatureVersion(feat);
    if (!StringUtil.equalStrings(ver, over)) {
      switch (feat) {
      case Substance: substanceVersion = ver; break;
      case Metadata: metadataVersion = ver; break;
      default:
      }
      needSave();
    }
  }

  /**
   * Sets the last treewalk time to the current time.  Does not save itself
   * to disk, as it is desireable for the treewalk to run every time the
   * server restarts.  Consequently, it is non-persistent.
   * @deprecated
   */
  void setLastTreeWalkTime() {
    lastTreeWalk = TimeBase.nowMs();
  }

  /**
   * Gets the collection of crawl urls.
   * @return a {@link Collection}
   * @deprecated
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
   * @deprecated
   */
  public void updatedCrawlUrls(boolean forceWrite) {
    urlUpdateCntr++;
    if (forceWrite || (urlUpdateCntr >= URL_UPDATE_LIMIT)) {
      needSave();
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

  public synchronized void setClockssSubscriptionStatus(int val) {
    if (clockssSubscriptionStatus != val) {
      clockssSubscriptionStatus = val;
      needSave();
    }
  }

  /** Start a batch of updates, deferring saving until unBatchSaves() is
   * called. */
  public synchronized void batchSaves() {
    if (logger.isDebug3()) {
      logger.debug3("Start batch: " + (batchSaveDepth + 1));
    }
    ++batchSaveDepth;
  }

  /** End the update batch, saving if any changes have been made. */
  public synchronized void unBatchSaves() {
    if (batchSaveDepth == 0) {
      logger.warning("unBatchSaves() called when no batch in progress",
		     new Throwable());
      return;
    }
    if (logger.isDebug3()) {
      logger.debug3("End batch: " + batchSaveDepth);
    }
    if (--batchSaveDepth == 0 && needSave) {
      storeAuState();
    }
  }

  private void needSave() {
    if (batchSaveDepth == 0) {
      storeAuState();
    } else {
      needSave = true;
    }
  } 

  public synchronized void storeAuState() {
    historyRepo.storeAuState(this);
    needSave = false;
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
    if (cdnStems != null) {
      if (cdnStems.isEmpty()) {
	cdnStems = null;
      } else {
	cdnStems = StringPool.URL_STEMS.internList(cdnStems);
      }
    }
  }

  protected void flushAuCaches() {
    try {
      au.setConfiguration(au.getConfiguration());
    } catch (ArchivalUnit.ConfigurationException e) {
      logger.error("Shouldn't happen: au.setConfiguration(au.getConfiguration())",
		   e);
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
