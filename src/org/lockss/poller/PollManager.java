/*
 * $Id$
 */

/*

Copyright (c) 2000-2014 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.poller;

import static org.lockss.poller.v3.V3PollFactory.*;
import static org.lockss.poller.v3.V3Poller.*;
import static org.lockss.poller.v3.V3Voter.*;
import static org.lockss.util.Constants.*;

import java.io.*;
import java.util.*;

import org.apache.commons.collections.map.*;
import org.apache.commons.lang3.mutable.MutableInt;
import org.lockss.alert.*;
import org.lockss.app.*;
import org.lockss.config.*;
import org.lockss.daemon.*;
import org.lockss.daemon.status.StatusService;
import org.lockss.hasher.HashService;
import org.lockss.hasher.BlockHasher;
import org.lockss.plugin.*;
import org.lockss.plugin.base.DefaultUrlCacher;
import org.lockss.poller.v3.*;
import org.lockss.poller.v3.V3Serializer.PollSerializerException;
import org.lockss.protocol.*;
import org.lockss.protocol.V3LcapMessage.PollNak;
import org.lockss.protocol.psm.PsmManager;
import org.lockss.state.*;
import org.lockss.util.*;
import org.lockss.repository.*;

/**
 * <p>Class that manages the polling process.</p>
 * @author Claire Griffin
 * @version 1.0
 */

// CR: Code review comments are marked with CR:

// todo(bhayes): Much of the synchronization here could be eliminated
// or simplified, particularly with the elimination of V1 code. There
// are public methods for status information, and otherwise all
// activity is under the control of the PollStarter [which starts V3
// polls from the pollQueue] and handleIncomingMessage [which starts
// V3 votes].

public class PollManager
  extends BaseLockssDaemonManager implements ConfigurableManager {

  // Shared with MockPollManager
  protected static final Logger theLog = Logger.getLogger(PollManager.class);

  static final String PREFIX = Configuration.PREFIX + "poll.";
  static final String PARAM_RECENT_EXPIRATION = PREFIX + "expireRecent";

  static final long DEFAULT_RECENT_EXPIRATION = DAY;
  
  /** If true, empty poll state directories found at startup will be
   * deleted.
   */
  static final String PARAM_DELETE_INVALID_POLL_STATE_DIRS =
    PREFIX + "deleteInvalidPollStateDirs";
  static final boolean DEFAULT_DELETE_INVALID_POLL_STATE_DIRS = true;
  
  /** If true, discard saved poll state at startup (i.e., don't restore
   * polls that were running before exit).  */
  static final String PARAM_DISCARD_SAVED_POLLS = PREFIX + "discardSavedPolls";
  static final boolean DEFAULT_DISCARD_SAVED_POLLS = false;
  
  // todo(bhayes): Is PREFIX right? Should this be a "v3" param, not a
  // "poll" param?
  /** If set to 'true', enable repairs even when there is no active
   * voter.
   */
  public static final String PARAM_ENABLE_V3_REPAIRER =
    PREFIX + "enableV3Repairer";
  public static final boolean DEFAULT_ENABLE_V3_REPAIRER = false;

  /** [Only examined if PARAM_ENABLE_V3_REPAIRER is true.]  If set to
      'true', never use the V3Voter for repairs. If set to 'false' use
      the V3Voter if one is active, otherwise use the non-V3Voter
      repair mechanism.
   */
  public static final String PARAM_PREFER_V3_REPAIRER =
    PREFIX + "preferV3Repairer";
  public static final boolean DEFAULT_PREFER_V3_REPAIRER = true;

  /** The classes of AUs for which polls should be run.  May be a singleton
   * or list of:
   * <dl>
   * <dt>All<dd> All AUs
   * <dt>Internal<dd> Internal AUs (plugin registries)
   * <dt>Priority<dd> Poll that have been requested from DebugPanel
   * </dl>
   */
  public static final String PARAM_AUTO_POLL_AUS =
    PREFIX + "autoPollAuClassess"; // FIXME typo
  public static final List<String> DEFAULT_AUTO_POLL_AUS =
    ListUtil.list("All");

  /** Map of AUID regexp to poll weight multiplier.  If set, AU's poll
   * weight is multiplied by the weight of the first regexp that its AUID
   * matches.  Weight should be a flost. */
  static final String PARAM_POLL_PRIORITY_AUID_MAP =
    PREFIX + "pollWeightAuidMap";
  static final List DEFAULT_POLL_PRIORITY_AUID_MAP = null;

  /** Maps AU patterns to poll weight.  Keys are XPath expressions (see
   * {@link org.lockss.util.AuXpathMatcher}.  If set, AU's poll weight is
   * multiplied by the weight of the first matching XPath..
   * Weight should be a float. */
  static final String PARAM_POLL_PRIORITY_AU_MAP =
    PREFIX + "pollWeightAuMap";
  static final List DEFAULT_POLL_PRIORITY_AU_MAP = null;

  // Poll starter

  public static final String PARAM_START_POLLS_INITIAL_DELAY = 
    PREFIX + "pollStarterInitialDelay";
  public static final long DEFAULT_START_POLLS_INITIAL_DELAY = 
    MINUTE * 10;
  
  /** Minimum interval between poll attempts on an AU.  This takes effect
   * even if the poll failed to start. */
  public static final String PARAM_MIN_POLL_ATTEMPT_INTERVAL =
    PREFIX + "minPollAttemptInterval";
  public static final long DEFAULT_MIN_POLL_ATTEMPT_INTERVAL = 4 * HOUR;

  /** The time, in ms, that will be added between launching new polls.
   * This time is added to the channel timeout time provided by SCOMM.
   */
  public static final String PARAM_ADDED_POLL_DELAY =
    PREFIX + "pollStarterAdditionalDelayBetweenPolls";
  public static final long DEFAULT_ADDED_POLL_DELAY = SECOND;
  
  /** Max interval between recalculating poll queue order */
  public static final String PARAM_REBUILD_POLL_QUEUE_INTERVAL =
    PREFIX + "queueRecalcInterval";
  static final long DEFAULT_REBUILD_POLL_QUEUE_INTERVAL = HOUR;

  /** Interval to sleep when queue empty, before recalc. */
  public static final String PARAM_QUEUE_EMPTY_SLEEP =
    PREFIX + "queueEmptySleep";
  static final long DEFAULT_QUEUE_EMPTY_SLEEP = 30 * MINUTE;

  /** Interval to sleep when max number of pollers are active, before
   * checking again. */
  public static final String PARAM_MAX_POLLERS_SLEEP =
    PREFIX + "maxPollersSleep";
  static final long DEFAULT_MAX_POLLERS_SLEEP = 10 * MINUTE;

  /** Size of poll queue. */
  public static final String PARAM_POLL_QUEUE_MAX =
    PREFIX + "pollQueueMax";
  static final int DEFAULT_POLL_QUEUE_MAX = 20;

  /**
   * If set, poll starting will be throttled.  This is the default.
   */
  public static final String PARAM_ENABLE_POLL_STARTER_THROTTLE =
    PREFIX + "enablePollStarterThrottle";
  public static boolean DEFAULT_ENABLE_POLL_STARTER_THROTTLE = true; 

  /** Interval after which we'll try inviting peers that we think are not
   * in our polling group */
  public static final String PARAM_WRONG_GROUP_RETRY_TIME =
    PREFIX + "wrongGroupRetryTime";
  public static final long DEFAULT_WRONG_GROUP_RETRY_TIME = 4 * WEEK;

  static final String V3PREFIX = PREFIX + "v3.";

  /** Curve expressing desired inter-poll interval based on last agreement
   * value */
  public static final String PARAM_POLL_INTERVAL_AGREEMENT_CURVE =
    V3PREFIX + "pollIntervalAgreementCurve";
  public static final String DEFAULT_POLL_INTERVAL_AGREEMENT_CURVE = null;

  /** Previous poll results for which we want to apply {@link
   * #PARAM_POLL_INTERVAL_AGREEMENT_CURVE} */
  public static final String PARAM_POLL_INTERVAL_AGREEMENT_LAST_RESULT =
    V3PREFIX + "pollIntervalAgreementLastResult";
  public static final List DEFAULT_POLL_INTERVAL_AGREEMENT_LAST_RESULT =
    Collections.EMPTY_LIST;

  /** Curve expressing desired inter-poll interval based on number of
   * at-risk instances of AU */
  public static final String PARAM_POLL_INTERVAL_AT_RISK_PEERS_CURVE =
    V3PREFIX + "pollIntervalAtRiskPeersCurve";
  public static final String DEFAULT_POLL_INTERVAL_AT_RISK_PEERS_CURVE = null;

  /** Curve expressing poll weight multiplier based on number of at-risk
   * instances of AU */
  public static final String PARAM_POLL_WEIGHT_AT_RISK_PEERS_CURVE =
    V3PREFIX + "pollWeightAtRiskPeersCurve";
  public static final String DEFAULT_POLL_WEIGHT_AT_RISK_PEERS_CURVE = null;

  /** Curve giving reset interval of NoAuPeerIdSet as a function of AU
   * age */
  public static final String PARAM_NO_AU_RESET_INTERVAL_CURVE =
    V3PREFIX + "noAuResetIntervalCurve";
  public static final String DEFAULT_NO_AU_RESET_INTERVAL_CURVE =
    "[1w,2d],[1w,7d],[30d,7d],[30d,30d],[100d,30d],[100d,50d]";

  /** Target poll interval if no other mechanism is used */
  public static final String PARAM_TOPLEVEL_POLL_INTERVAL =
    V3PREFIX + "toplevelPollInterval";
  public static final long DEFAULT_TOPLEVEL_POLL_INTERVAL = 10 * WEEK;

    /** If true, enable sampled (Proof of Possession) polls.
   */
  public static final String PARAM_V3_ENABLE_POP_POLLS =
    V3PREFIX + "enablePoPPolls";
  public static final boolean DEFAULT_V3_ENABLE_POP_POLLS = false;
  
  /** For testing, if true, all polls are sampled.
   */
  public static final String PARAM_V3_ALL_POP_POLLS =
    V3PREFIX + "allPoPPolls";
  public static final boolean DEFAULT_V3_ALL_POP_POLLS = false;

  /** If true, enable local polls (i.e. polls that do not invite any
   * voters but depend on local hashes.
   */
  public static final String PARAM_V3_ENABLE_LOCAL_POLLS =
    V3PREFIX + "enableLocalPolls";
  public static final boolean DEFAULT_V3_ENABLE_LOCAL_POLLS = false;

  /** For testing, if true, all polls are local.
   */
  public static final String PARAM_V3_ALL_LOCAL_POLLS =
    V3PREFIX + "allLocalPolls";
  public static final boolean DEFAULT_V3_ALL_LOCAL_POLLS = false;

  /**
   * Minimum time between polls of any kind on an AU.
   */
  public static final String PARAM_MIN_TIME_BETWEEN_ANY_POLL =
    V3PREFIX + "minTimeBetweenAnyPoll";
  public static final long DEFAULT_MIN_TIME_BETWEEN_ANY_POLL = 1 * DAY;

  /**
   * Maximum delay between PoR polls, expressed as a multiplier for the
   * AU's poll interval as determined by pollIntervalAgreementLastResult or
   * toplevelPollInterval.
   */
  public static final String PARAM_MAX_DELAY_BETWEEN_POR_MULTIPLIER =
    V3PREFIX + "maxDelayBetweenPoRMultiplier";
  public static final long DEFAULT_MAX_DELAY_BETWEEN_POR_MULTIPLIER = 10;

  /**
   * Minimum agreeing peers in last PoR poll to allow for PoP poll.
   * XXX should be dynamic not static.
   */
  public static final String PARAM_MIN_AGREE_PEERS_LAST_POR_POLL =
    V3PREFIX + "minAgreePeersLastPoRPoll";
  public static final int DEFAULT_MIN_AGREE_PEERS_LAST_POR_POLL = 3;

  /**
   * Minimum number of willing repairers to allow for local poll
   * XXX Should be dynamic not static.
   */
  public static final String PARAM_REPAIRER_THRESHOLD =
    V3PREFIX + "repairerThreshold";
  public static final int DEFAULT_REPAIRER_THRESHOLD = 3;

  public static class AuPeersMap extends HashMap<String,Set<PeerIdentity>> {}

  // todo(bhayes): It would be nice to get this class out of the
  // business if knowing about isPollComplete. The behavior before
  // this class existed was that current entries were always
  // isPollActive and the recent entries were always
  // !isPollActive. When transitioning, the entry was first removed,
  // then transitioned, then put back as "recent". In the transition,
  // the list if all current and recent polls would not contain the
  // transitioning items.

  // todo(bhayes): It would also be nice to get this class out of the
  // business of catering to the V3PollStatus; it feels as if there's
  // some core functions about tracking current and recent polls [that
  // is, "objects which receive LcapMessages"], and letting some
  // wrapper deal with V3PollStatus needing to distinguish V3Poller
  // from V3Voter.

  /**
   * Note: This documentation is intended to be accurate for V3; V1
   * may or may not conform to this use.
   *
   * Maintain a collection of current and recent V3Pollers and
   * V3Voters.  The "recent" entries will expire from the collection
   * as time passes; the "current" entries will not.
   *
   * Taking a poll to the completed state involves writing to disk; it
   * would be inadvisable to hold a lock while completing a poll. [Is
   * this V1 only?] There is some inconsistency between which list a
   * poll is in and whether isPollComplete returns true.
   *
   * Since a poll may be closed at any time, there is no guarantee
   * that an item which was active when returned by EntryManager will
   * still be active in the calling code; calling code has to be aware
   * that a "current" poll may be closed.
   */
  private static class EntryManager {
    // todo(bhayes): Move this to its own file for testing, or at
    // least refactor tests.
    /**
     * A collection of current polls.
     */
    private HashMap<String,PollManagerEntry> thePolls =
      new HashMap<String,PollManagerEntry>();

    // todo(bhayes): FixedTimedMap could be made generic.
    /**
     * A collection of finished polls, automatically deleted from the
     * collection when they reach a certain age.
     */
    private FixedTimedMap theRecentPolls =
      new FixedTimedMap(DEFAULT_RECENT_EXPIRATION);

    private Object pollMapLock = thePolls;

    /** Set the time after which recent polls will expire. */
    void setExpireInterval(long recentPollExpireTime) {
      synchronized (pollMapLock) {
	theRecentPolls.setInterval(recentPollExpireTime);
      }
    }

    /** Remember a current poll. It will remain available to
     * getCurrentPoll until it is closed by closePoll.
     * @throw IllegalArgumentException if the pool's key is already
     * current.
     */
    void addPoll(BasePoll poll) {
      synchronized (pollMapLock) {
	if (thePolls.containsKey(poll.getKey())) {
	  throw new IllegalArgumentException("Poll "+poll.getKey()+
            " is already in the EntryManager.");
	}
	thePolls.put(poll.getKey(), new PollManagerEntry(poll));
      }
    }

    /** Return the PollManagerEntry for the current poll with the specified
     * key; the poll may be closed at any time. */
    PollManagerEntry getCurrentPoll(String key) {
      synchronized (pollMapLock) {
	return thePolls.get(key);
      }
    }

    /**
     * Is there a current, active V3Poller for the given ArchivalUnit
     * @param spec the ArchivalUnit
     * @return true if we have a current, active V3Poller for au
     */
    boolean isPollRunning(ArchivalUnit au) {
      if (au == null) {
	throw new NullPointerException("isPollRunning: null AU");
      }
      String auid = au.getAuId();
      if (auid == null) {
	throw new NullPointerException("isPollRunning: null auid");
      }

      for (PollManagerEntry pme : forAuId(auid)) {
	if (pme.getPoll() instanceof V3Poller) {
	  if (pme.isPollActive()) {
	    return true;
	  }
	}
      }
      return false;
    }

    /**
     * @return a Set of PollManagerEntries which are current, and are
     * concerned with this au.
     */
    // Probably this can be replaced by (or call) forAuId.  Keeping them
    // separate for now out of caution.  Are there any situations in which
    // comparing the auid to the PollSpec's auid differs from comparing the
    // au to the poll's au?  And there's probably no reason this needs to
    // return a Set.
    Set<PollManagerEntry> forAu(ArchivalUnit au) {
      Set<PollManagerEntry> forAu = new HashSet<PollManagerEntry>();
      synchronized (pollMapLock) {
	for (PollManagerEntry pme : thePolls.values()) {
	  ArchivalUnit pau = pme.poll.getCachedUrlSet().getArchivalUnit();
	  if (pau == au) {
	    forAu.add(pme);
	  }
	}
      }
      return forAu;
    }

    /**
     * @return a Collection of current PollManagerEntries for the specified
     * auid.
     */
    Collection<PollManagerEntry> forAuId(String auid) {
      Collection<PollManagerEntry> res = new ArrayList<PollManagerEntry>();
      synchronized (pollMapLock) {
	for (PollManagerEntry pme : thePolls.values()) {
	  if (auid.equals(pme.getPollSpec().getAuId())) {
	    res.add(pme);
	  }
	}
      }
      return res;
    }

    /** Allow the current poll to expire. It will no longer be
     * available from getCurrentPoll, but will be available in
     * the accessors for recent polls. 
     * @throws IllegalStateException if the entry specified by the key
     * is not complete.
     */
    PollManagerEntry allowToExpire(String key) {
      PollManagerEntry pme = getCurrentPoll(key);
      if (!pme.isPollCompleted()) { 
	throw new IllegalStateException("Poll "+key+
					" should have been completed before" +
					" it was expired.");
      }
      synchronized (pollMapLock) {
	PollManagerEntry pme1 = thePolls.remove(key);
	if (pme1 != null && pme1.poll != null) {
	  theRecentPolls.put(key, pme1);
	}
	return pme1;
      }
    }
 
    /** Remove all the polls, both current and recent. */
    void clear() {
      synchronized (pollMapLock) {
	thePolls.clear();
	theRecentPolls.clear();
      }
    }

    /**
     * @return the number of current V3Pollers.
     */
    int countCurrentV3Pollers() {
      int count = 0;
      synchronized (pollMapLock) {
	for (PollManagerEntry pme : thePolls.values()) {
	  if (pme.isV3Poll() && pme.getPoll() instanceof V3Poller) {
	    count++;
	  }
	}
      }
      return count;
    }
  
    /**
     * @return the number of current V3Pollers.
     */
    int countCurrentV3Voters() {
      int count = 0;
      synchronized (pollMapLock) {
	for (PollManagerEntry pme : thePolls.values()) {
	  if (pme.isV3Poll() && pme.getPoll() instanceof V3Voter) {
	    count++;
	  }
	}
      }
      return count;
    }

    //--------------- V3PollStatus Accessors from here -----------------------

    /** Find the poll either in current or recent collection;
     * reached only by V3PollStatus. */
    PollManagerEntry getCurrentOrRecentV3PollEntry(String key) {
      synchronized (pollMapLock) {
	PollManagerEntry pme = thePolls.get(key);
	if (pme == null) {
	  pme = (PollManagerEntry)theRecentPolls.get(key);
	}
	if (pme != null && !pme.isV3Poll()) {
	  throw new IllegalStateException("Expected V3Poll: "+key);
	}
	return pme;
      }
    }

    /**
     * @return the union of the current and recent V3Pollers
     */
    Collection<V3Poller> getV3Pollers() {
      Collection<V3Poller> polls = new ArrayList<V3Poller>();
      synchronized (pollMapLock) {
	for (PollManagerEntry pme : thePolls.values()) {
	  if (pme.isV3Poll() && pme.getPoll() instanceof V3Poller) {
	    polls.add((V3Poller)pme.getPoll());
	  }
	}
	for (Object o : theRecentPolls.values()) {
	  PollManagerEntry pme = (PollManagerEntry)o;
	  if (pme.isV3Poll() && pme.getPoll() instanceof V3Poller) {
	    polls.add((V3Poller)pme.getPoll());
	  }
	}
      }
      return polls;
    }

    /**
     * @return the union of the active and recent V3Voters
     */
    Collection<V3Voter> getV3Voters() {
      Collection<V3Voter> polls = new ArrayList<V3Voter>();
      synchronized (pollMapLock) {
	for (PollManagerEntry pme : thePolls.values()) {
	  if (pme.isV3Poll() && pme.getPoll() instanceof V3Voter) {
	    polls.add((V3Voter)pme.getPoll());
	  }
	}
	for (Object o : theRecentPolls.values()) {
	  PollManagerEntry pme = (PollManagerEntry)o;
	  if (pme.isV3Poll() && pme.getPoll() instanceof V3Voter) {
	    polls.add((V3Voter)pme.getPoll());
	  }
	}
      }
      return polls;
    }

    // Reached only by TestPollManager.
    /**
     * @return true iff there is an entry in the recent polls, and
     * the poll's isPollCompleted returns true.
     */
    boolean isPollClosed(String key) {
      PollManagerEntry pme;
      synchronized (pollMapLock) {
	pme = (PollManagerEntry)theRecentPolls.get(key);
      }
      // todo(bhayes): is the isPollCompleted needed for V3?
      return (pme != null) ? pme.isPollCompleted() : false;
    }

    //--------------- V1 Accessors from here -----------------------------

    /**
     * Is a poll of the given type and spec currently running
     * @param spec the PollSpec definining the location of the poll.
     * @return true if we have a poll which is running that matches pollspec
     * 
     * @deprecated  This method may be removed in a future release.
     */
    boolean isPollRunning(PollSpec spec) {
      PollManagerEntry ent = forPollSpec(spec);
      return ent != null && !ent.isPollCompleted();
    }

    PollManagerEntry forPollSpec(PollSpec spec) {
      synchronized (pollMapLock) {
	for (PollManagerEntry pme : thePolls.values()) {
	  if (pme.isSamePoll(spec)) {
	    return pme;
	  }
	}
	return null;
      }
    }

    // Used only in V1PollFactory.
    public boolean hasPoll(String key) {
      synchronized (pollMapLock) {
	return thePolls.containsKey(key);
      }
    }

    // Only used in V1PollFactory.
    /**
     * getActivePollSpecIterator returns an Iterator over the set of
     * PollSpec instances which currently have active polls on the given au.
     * @return Iterator over set of PollSpec
     */
    Iterator<PollSpec> getActivePollSpecIterator(ArchivalUnit au,
						 BasePoll dontIncludePoll) {
      Set<PollSpec> pollspecs = new HashSet<PollSpec>();
      for (PollManagerEntry pme : forAu(au)) {
	if (pme.poll != dontIncludePoll &&
	    !pme.isPollCompleted()) {
	  pollspecs.add(pme.poll.getPollSpec());
	}
      }
      return (pollspecs.iterator());
    }

    // Used in PollerStatus.getSummary, which is V1 code.
    /** Find the poll either in current or recent polls */
    PollManagerEntry getCurrentOrRecentV1PollEntry(String key) {
      synchronized (pollMapLock) {
	PollManagerEntry pme = thePolls.get(key);
	if (pme == null) {
	  pme = (PollManagerEntry)theRecentPolls.get(key);
	}
	if (pme != null && pme.isV3Poll()) {
	  throw new IllegalStateException("Expected V1Poll: "+key);
	}
	return pme;
      }
    }

    //--------------- PollerStatus Accessors -----------------------------
    Collection<PollManagerEntry> getV1Polls() {
      Collection<PollManagerEntry> polls = new ArrayList<PollManagerEntry>();
      synchronized (pollMapLock) {
	for (PollManagerEntry pme : thePolls.values()) {
	  if (pme.getType() == Poll.V1_CONTENT_POLL ||
	      pme.getType() == Poll.V1_NAME_POLL ||
	      pme.getType() == Poll.V1_VERIFY_POLL) {
	    polls.add(pme);
	  }
	}
	for (Iterator it = theRecentPolls.values().iterator(); it.hasNext(); ) {
	  PollManagerEntry pme = (PollManagerEntry)it.next();
	  if (pme.getType() == Poll.V1_CONTENT_POLL ||
	      pme.getType() == Poll.V1_NAME_POLL ||
	      pme.getType() == Poll.V1_VERIFY_POLL) {
	    polls.add(pme);
	  }
	}
      }
      return polls;
    }

    /**
     * suspend a poll while we wait for a repair
     * @param key the identifier key of the poll to suspend
     */
    // XXX: V3 -- Only required for V1 polls.
    PollManagerEntry suspendPoll(String key) {
      PollManagerEntry pme;
      synchronized (pollMapLock) {
	pme = this.getCurrentOrRecentV1PollEntry(key);
	if (pme != null) {
	  // todo(bhayes): Why is a poll that isn't already in
	  // thePolls being suspended?
	  theRecentPolls.remove(key);
	  pme.setPollSuspended();
	  thePolls.put(key, pme);
	}
      }
      return pme;
    }
  }

  private static EntryManager entryManager = new EntryManager();
  
  private static PollManager theManager = null;
  private static LcapRouter.MessageHandler m_msgHandler;
  private static IdentityManager theIDManager;
  private static HashService theHashService;
  private static LcapRouter theRouter = null;
  private RepairPolicy theRepairPolicy = null;
  private V3ReusableRepairer theReusableRepairer = null;
  private AlertManager theAlertManager = null;
  private PluginManager pluginMgr = null;
  private static SystemMetrics theSystemMetrics = null;
  private AuEventHandler auEventHandler;
  // CR: serializedPollers and serializedVoters s.b. updated as new
  // polls/votes are created, in case AU is deactivated & reactivated
  private HashMap serializedPollers;
  private HashMap serializedVoters;
  private V3PollStatusAccessor v3Status;
  private boolean deleteInvalidPollStateDirs =
    DEFAULT_DELETE_INVALID_POLL_STATE_DIRS;
  private long paramToplevelPollInterval = DEFAULT_TOPLEVEL_POLL_INTERVAL;

  private long pollStartInitialDelay = DEFAULT_START_POLLS_INITIAL_DELAY;
  private boolean enableV3Poller = DEFAULT_ENABLE_V3_POLLER;
  private int maxSimultaneousPollers = DEFAULT_MAX_SIMULTANEOUS_V3_POLLERS;
  private PollStarter pollStarter;
  private boolean isPollStarterEnabled = false;
  private boolean enablePollStarterThrottle =
    DEFAULT_ENABLE_POLL_STARTER_THROTTLE;
  private long paramRebuildPollQueueInterval =
    DEFAULT_REBUILD_POLL_QUEUE_INTERVAL;
  private long paramQueueEmptySleep = DEFAULT_QUEUE_EMPTY_SLEEP;
  private long paramMaxPollersSleep = DEFAULT_MAX_POLLERS_SLEEP;
  private int paramPollQueueMax = DEFAULT_POLL_QUEUE_MAX;
  private long interPollStartDelay = DEFAULT_ADDED_POLL_DELAY;
  private long paramMinPollAttemptInterval = DEFAULT_MIN_POLL_ATTEMPT_INTERVAL;
  private double paramMinPercentForRepair =
    V3Voter.DEFAULT_MIN_PERCENT_AGREEMENT_FOR_REPAIRS;
  private boolean paramDiscardSavedPolls =
    DEFAULT_DISCARD_SAVED_POLLS;

  private long wrongGroupRetryTime = DEFAULT_WRONG_GROUP_RETRY_TIME;
  private IpFilter noInvitationSubnetFilter = null;
  private CompoundLinearSlope v3InvitationWeightAgeCurve = null;
//   private CompoundLinearSlope v3InvitationWeightSafetyCurve = null;
  private CompoundLinearSlope v3AcceptProbabilitySafetyCurve = null;
  private CompoundLinearSlope v3NominationWeightAgeCurve = null;
  private CompoundLinearSlope pollIntervalAgreementCurve = null;
  private CompoundLinearSlope pollIntervalAtRiskPeersCurve = null;
  private CompoundLinearSlope pollWeightAtRiskPeersCurve = null;
  private Set pollIntervalAgreementLastResult =
    SetUtil.theSet(DEFAULT_POLL_INTERVAL_AGREEMENT_LAST_RESULT);
  private long paramWillingRepairerLiveness = DEFAULT_WILLING_REPAIRER_LIVENESS;
  private double paramAcceptRepairersPollPercent =
    DEFAULT_ACCEPT_REPAIRERS_POLL_PERCENT;
  private double paramInvitationWeightAtRisk =
    DEFAULT_INVITATION_WEIGHT_AT_RISK;
  private double paramInvitationWeightAlreadyRepairable =
    DEFAULT_INVITATION_WEIGHT_ALREADY_REPAIRABLE;
  private CompoundLinearSlope v3NoAuResetIntervalCurve = null;
  private CompoundLinearSlope v3VoteRetryIntervalDurationCurve = null;
  private ReputationTransfers reputationTransfers;
  private PatternFloatMap pollPriorityAuidMap;
  private AuXpathFloatMap pollPriorityAuMap;


  private AuPeersMap atRiskAuInstances = null;

  // If true, restore V3 Pollers
  private boolean enablePollers = DEFAULT_ENABLE_V3_POLLER;
  // If true, restore V3 Voters
  private boolean enableVoters = DEFAULT_ENABLE_V3_VOTER;

  // If true, reply to a repair request when there is no V3Voter.
  private boolean enableRepairers = DEFAULT_ENABLE_V3_REPAIRER;
  // If true, and enableRepairers is true, use the V3ReusableRepairer
  // even if there is a V3Voter.
  private boolean preferRepairers = DEFAULT_PREFER_V3_REPAIRER;

  private List<String> autoPollAuClassess = DEFAULT_AUTO_POLL_AUS;
  
  // Executor used to carry out serialized poll operations. 
  // Implementations include a queued poll executor and a pooled poll executor.
  private PollRunner theTaskRunner;

  // our configuration variables
  protected long m_recentPollExpireTime = DEFAULT_RECENT_EXPIRATION;

  private boolean enableLocalPolls = DEFAULT_V3_ENABLE_LOCAL_POLLS;
  private boolean allLocalPolls = DEFAULT_V3_ALL_LOCAL_POLLS;
  private boolean enablePoPPolls = DEFAULT_V3_ENABLE_POP_POLLS;
  private boolean allPoPPolls = DEFAULT_V3_ALL_POP_POLLS;
  private long minTimeSinceAnyPoll = DEFAULT_MIN_TIME_BETWEEN_ANY_POLL;
  private long maxDelayBetweenPoRMultiplier =
    DEFAULT_MAX_DELAY_BETWEEN_POR_MULTIPLIER;
  private int minAgreePeersLastPoRPoll = DEFAULT_MIN_AGREE_PEERS_LAST_POR_POLL;
  private int repairerThresholdForLocal = DEFAULT_REPAIRER_THRESHOLD;

  Deadline startOneWait = Deadline.in(0);

  static class PollReq {
    ArchivalUnit au;
    int priority = 0;
    PollSpec spec;

    public PollReq(ArchivalUnit au) {
      this.au = au;
    }

    public PollReq setPriority(int val) {
      priority = val;
      return this;
    }

    public PollReq setPollSpec(PollSpec spec) { 	 
      this.spec = spec; 	 
      return this; 	 
    } 	 

    public PollSpec getPollSpec() {
      return spec;
    }

    public ArchivalUnit getAu() {
      return au;
    }

    public int getPriority() {
      return priority;
    }

    public boolean isHighPriority() {
      return priority > 0;
    }

    public String toString() {
      return "[PollReq: " + au + ", pri: " + priority + "]";
    }
  }

  /**
   * Keep an ordered list of PollReq. High-priority requests will
   * always be done first, followed by requests generated by tne
   * PollManager itself.
   */
  class PollQueue {
    Deadline timeToRebuildPollQueue = Deadline.in(0);
    Object queueLock = new Object();	// lock for pollQueue
    /**
     * The processed list of poll requests, in the order they will be executed.
     */
    private List<PollReq> pollQueue = new ArrayList<PollReq>();
    /**
     * The high-priority pending requests.
     */
    private Map<ArchivalUnit,PollReq> highPriorityPollRequests =
      Collections.synchronizedMap(new ListOrderedMap());

    /**
     * Make a PollQueue.
     */
    PollQueue() {
    }
    
    /**
     * @return the next PollReq to start.
     */
    public PollReq nextReq() throws InterruptedException {
      boolean rebuilt = rebuildPollQueueIfNeeded();
      PollReq req = nextReqFromBuiltQueue();
      if (req != null) {
	return req;
      }
      if (!rebuilt) {
	rebuildPollQueue();
      }
      return nextReqFromBuiltQueue();
    }

    /**
     * Request a poll, as specified by the PollReq, sooner than might
     * otherwise happen. Overrides any previous requests.
     * @param req the PollReq
     */
    public void enqueueHighPriorityPoll(PollReq req) {
      highPriorityPollRequests.put(req.au, req);
      needRebuildPollQueue();
    }

    /**
     * Remove any requests previously registered using
     * enqueueHighPriorityPoll.
     * @param au the ArchivalUnit
     */
    public void cancelAuPolls(ArchivalUnit au) {
      // todo(bhayes): This doesn't force a rebuild? Why not?
      highPriorityPollRequests.remove(au);
    }

    /**
     * Invalidate the current list of pending polls.
     */
    public void needRebuild() {
      timeToRebuildPollQueue.expire();
    }

    /**
     * @return a List of ArchivalUnits in the queue to poll.
     */
    public List<ArchivalUnit> getPendingQueueAus() {
      rebuildPollQueueIfNeeded();
      ArrayList<ArchivalUnit> aus = new ArrayList<ArchivalUnit>();
      synchronized (queueLock) {
	for (PollReq req : pollQueue) {
	  aus.add(req.getAu());
	}
      }
      return aus;
    }

    /**
     * Pop the next PollReq from the queue, and return it.
     */
    private PollReq nextReqFromBuiltQueue() {
      synchronized (queueLock) {
	if (theLog.isDebug3()) {
	  theLog.debug3("nextReqFromBuiltQueue(), " +
			pollQueue.size() + " in queue");
	}
	while (!pollQueue.isEmpty()) {
	  PollReq req = pollQueue.remove(0);
	  // todo(bhayes): Why is this check here, rather than in
	  // startOnePoll()? Also, if this is in the high priority
	  // list, what keeps it from getting back in the rebuilt
	  // list?
	  // ignore deleted AUs
	  if (pluginMgr.isActiveAu(req.getAu())) {
	    return req;
	  }
	}
	if (theLog.isDebug3()) {
	  theLog.debug3("nextReqFromBuiltQueue(): null");
	}
	return null;
      }
    }
    
    /**
     * Rebuild the poll queue, if needed.
     * @return true iff the queue was rebuilt.
     */
    private boolean rebuildPollQueueIfNeeded() {
      synchronized (queueLock) {
	if (timeToRebuildPollQueue.expired()) {
	  rebuildPollQueue();
	  return true;
	}
	return false;
      }
    }

    // package level for testing; todo(bhayes): The tests should
    // probably be calling needRebuildPollQueue and using the other
    // public methods.
    /**
     * Force poll queue to be rebuilt from the pending high-priority
     * requests and the PollManager's list of ArchivalUnits.
     */
    void rebuildPollQueue() {
      timeToRebuildPollQueue.expireIn(paramRebuildPollQueueInterval);
      long startTime = TimeBase.nowMs();

      rebuildPollQueue0();
      theLog.debug("rebuildPollQueue(): "+
		   (TimeBase.nowMs() - startTime)+"ms");
    }

    /**
     * REALLY force poll queue to be rebuilt from the pending
     * high-priority requests and the PollManager's list of
     * ArchivalUnits.
     */
    private void rebuildPollQueue0() {
      synchronized (queueLock) {
	pollQueue.clear();
	// XXX Until have real priority system, just add these in the
	// order they were created.
	Set<ArchivalUnit> highPriorityAus = new HashSet<ArchivalUnit>();
	synchronized (highPriorityPollRequests) {
	  for (PollReq req : highPriorityPollRequests.values()) {
	    highPriorityAus.add(req.au);
	    if (isEligibleForPoll(req)) {
	      pollQueue.add(req);
	    }
	  }
	}
	int availablePollCount = paramPollQueueMax - pollQueue.size();
	if (availablePollCount > 0) {
	  Map<ArchivalUnit, PollWeight> weightMap =
	    new HashMap<ArchivalUnit, PollWeight>();
	  for (ArchivalUnit au : pluginMgr.getAllAus()) {
	    try {
	      if (highPriorityAus.contains(au)) {
		// already tried above; might or might not have been added.
		continue;
	      }
	      try {
		PollWeight weight = pollWeight(au);
		if (weight.value > 0.0) {
		  weightMap.put(au, weight);
		}
	      } catch (NotEligibleException e) {
		if (theLog.isDebug3()) {
		  theLog.debug3("Not eligible for poll: " + au + " reason " + e);
		}
	      }
	    } catch (RuntimeException e) {
	      theLog.warning("Checking for pollworthiness: " + au.getName(), e);
	      // ignore AU if it caused an error
	    }
	  }
	  // weightedRandomSelection throws if the count is larger
	  // than the size.
	  int count = Math.min(weightMap.size(), availablePollCount);
	  if (!weightMap.isEmpty()) {
	    List<ArchivalUnit> selected =
	      weightedRandomSelection(weightMap, count);
	    for (ArchivalUnit au : selected) {
	      PollSpec spec =
		new PollSpec(au.getAuCachedUrlSet(), Poll.V3_POLL);
	      spec.setPollVariant(weightMap.get(au).pollVariant());
	      PollReq req = new PollReq(au).setPollSpec(spec);
	      pollQueue.add(req);
	    }
	  }
	}
	if (theLog.isDebug()) {
	  theLog.debug("Poll queue: " + pollQueue);
	}
      }
    }
  }

  /**
   * The poll queue for ordering poll requests.
   */
  protected PollQueue pollQueue = new PollQueue();

  // The PollFactory instances
  PollFactory [] pf = {
    null,
    new V1PollFactory(),
    null, // new V2PollFactory(),
    new V3PollFactory(this),
  };

  public PollManager() {
  }

  /**
   * start the poll manager.
   * @see org.lockss.app.LockssManager#startService()
   */
  public void startService() {
    super.startService();
    // Create a poll runner.
    theTaskRunner = new PollRunner();
    
    // the services we use on an ongoing basis
    LockssDaemon theDaemon = getDaemon();
    theIDManager = theDaemon.getIdentityManager();
    theHashService = theDaemon.getHashService();
    theAlertManager = theDaemon.getAlertManager();
    pluginMgr = theDaemon.getPluginManager();

    Configuration config = ConfigManager.getCurrentConfig();
    if (config.containsKey(PARAM_AT_RISK_AU_INSTANCES)) {
      atRiskAuInstances =
	makeAuPeersMap(config.getList(PARAM_AT_RISK_AU_INSTANCES),
		       theIDManager);
    }

    reputationTransfers = new ReputationTransfers(theIDManager);

    // register a message handler with the router
    theRouter = theDaemon.getRouterManager();
    m_msgHandler =  new RouterMessageHandler();
    theRouter.registerMessageHandler(m_msgHandler);

    // get System Metrics
    theSystemMetrics = theDaemon.getSystemMetrics();
    // register our status
    StatusService statusServ = theDaemon.getStatusService();
    statusServ.registerStatusAccessor(V3PollStatus.POLLER_STATUS_TABLE_NAME,
                                      new V3PollStatus.V3PollerStatus(this));
    statusServ.registerOverviewAccessor(V3PollStatus.POLLER_STATUS_TABLE_NAME,
				       new V3PollStatus.PollOverview(this));
    statusServ.registerStatusAccessor(V3PollStatus.VOTER_STATUS_TABLE_NAME,
                                      new V3PollStatus.V3VoterStatus(this));
    statusServ.registerOverviewAccessor(V3PollStatus.VOTER_STATUS_TABLE_NAME,
				       new V3PollStatus.VoterOverview(this));
    statusServ.registerStatusAccessor(V3PollStatus.POLLER_DETAIL_TABLE_NAME,
                                      new V3PollStatus.V3PollerStatusDetail(this));
    statusServ.registerStatusAccessor(V3PollStatus.VOTER_DETAIL_TABLE_NAME,
                                      new V3PollStatus.V3VoterStatusDetail(this));
    statusServ.registerStatusAccessor(V3PollStatus.ACTIVE_REPAIRS_TABLE_NAME,
                                      new V3PollStatus.V3ActiveRepairs(this));
    statusServ.registerStatusAccessor(V3PollStatus.COMPLETED_REPAIRS_TABLE_NAME,
                                      new V3PollStatus.V3CompletedRepairs(this));
    statusServ.registerStatusAccessor(V3PollStatus.NO_QUORUM_TABLE_NAME,
                                      new V3PollStatus.V3NoQuorumURLs(this));
    statusServ.registerStatusAccessor(V3PollStatus.TOO_CLOSE_TABLE_NAME,
                                      new V3PollStatus.V3TooCloseURLs(this));
    statusServ.registerStatusAccessor(V3PollStatus.AGREE_TABLE_NAME,
                                      new V3PollStatus.V3AgreeURLs(this));
    statusServ.registerStatusAccessor(V3PollStatus.DISAGREE_TABLE_NAME,
                                      new V3PollStatus.V3DisagreeURLs(this));
    statusServ.registerStatusAccessor(V3PollStatus.ERROR_TABLE_NAME,
                                      new V3PollStatus.V3ErrorURLs(this));

    statusServ.registerStatusAccessor(V3PollStatus.PEER_AGREE_URLS_TABLE_NAME,
                                      new V3PollStatus.V3PeerAgreeURLs(this));
    statusServ.registerStatusAccessor(V3PollStatus.PEER_DISAGREE_URLS_TABLE_NAME,
                                      new V3PollStatus.V3PeerDisagreeURLs(this));
    statusServ.registerStatusAccessor(V3PollStatus.PEER_POLLER_ONLY_URLS_TABLE_NAME,
                                      new V3PollStatus.V3PeerPollerOnlyURLs(this));
    statusServ.registerStatusAccessor(V3PollStatus.PEER_VOTER_ONLY_URLS_TABLE_NAME,
                                      new V3PollStatus.V3PeerVoterOnlyURLs(this));


    // register our AU event handler
    auEventHandler = new AuEventHandler.Base() {
	@Override public void auCreated(AuEvent event, ArchivalUnit au) {
 	  restoreAuPolls(au);
	}
	@Override public void auDeleted(AuEvent event, ArchivalUnit au) {
	  cancelAuPolls(au);
	}};
    pluginMgr.registerAuEventHandler(auEventHandler);

    // Maintain the state of V3 polls, since these do not use the V1 per-node
    // history mechanism.
    v3Status = new V3PollStatusAccessor();

    theRepairPolicy = new RepairPolicy(theDaemon);
    
    // One time load of an in-memory map of AU IDs to directories. 
    preloadStoredPolls();
    
    // Enable the poll starter.
    enablePollStarter();
  }

  private void enablePollStarter() {
    theLog.info("Starting PollStarter");
    if (pollStarter != null) {
      theLog.debug("PollStarter already running. " + 
                   "Stopping old one first");
      disablePollStarter();
    }
    pollStarter = new PollStarter(getDaemon(), this);
    new Thread(pollStarter).start();
    isPollStarterEnabled = true;
  }
  
  private void disablePollStarter() {
    if (pollStarter != null) {
      theLog.info("Stopping PollStarter");
      pollStarter.stopPollStarter();
      pollStarter.waitExited(Deadline.in(SECOND));
      pollStarter = null;
    }
    isPollStarterEnabled = false;
  }

  /**
   * stop the poll manager
   * @see org.lockss.app.LockssManager#stopService()
   */
  public void stopService() {
    disablePollStarter();
    if (auEventHandler != null) {
      getDaemon().getPluginManager().unregisterAuEventHandler(auEventHandler);
      auEventHandler = null;
    }
    // unregister our status
    StatusService statusServ = getDaemon().getStatusService();
    statusServ.unregisterStatusAccessor(V3PollStatus.POLLER_STATUS_TABLE_NAME);
    statusServ.unregisterOverviewAccessor(V3PollStatus.POLLER_STATUS_TABLE_NAME);
    statusServ.unregisterStatusAccessor(V3PollStatus.VOTER_STATUS_TABLE_NAME);
    statusServ.unregisterOverviewAccessor(V3PollStatus.VOTER_STATUS_TABLE_NAME);
    statusServ.unregisterStatusAccessor(V3PollStatus.POLLER_DETAIL_TABLE_NAME);
    statusServ.unregisterStatusAccessor(V3PollStatus.VOTER_DETAIL_TABLE_NAME);
    statusServ.unregisterStatusAccessor(V3PollStatus.ACTIVE_REPAIRS_TABLE_NAME);
    statusServ.unregisterStatusAccessor(V3PollStatus.COMPLETED_REPAIRS_TABLE_NAME);
    statusServ.unregisterStatusAccessor(V3PollStatus.NO_QUORUM_TABLE_NAME);
    statusServ.unregisterStatusAccessor(V3PollStatus.TOO_CLOSE_TABLE_NAME);
    statusServ.unregisterStatusAccessor(V3PollStatus.AGREE_TABLE_NAME);
    statusServ.unregisterStatusAccessor(V3PollStatus.DISAGREE_TABLE_NAME);
    statusServ.unregisterStatusAccessor(V3PollStatus.ERROR_TABLE_NAME);

    // unregister our router
    theRouter.unregisterMessageHandler(m_msgHandler);

    // Stop the poll runner.
    if (theTaskRunner != null) {
      theTaskRunner.stop();
    }

    // null anything which might cause problems
    theTaskRunner = null;
    theIDManager = null;
    theHashService = null;
    theSystemMetrics = null;
    
    entryManager.clear();
    v3Status.clear();
    super.stopService();
  }


  /** Cancel all polls on the specified AU.
   * @param au the AU
   */
  void cancelAuPolls(ArchivalUnit au) {
    // first remove from queues, so none will run.
    pollQueue.cancelAuPolls(au);

    // collect PollManagerEntries related to this au
    Set<PollManagerEntry> forAu = entryManager.forAu(au);
    // then actually cancel the polls
    for (PollManagerEntry pme : forAu) {
      BasePoll poll = pme.poll;
      if (!poll.isPollCompleted()) {
	ArchivalUnit pau = poll.getCachedUrlSet().getArchivalUnit();
	theHashService.cancelAuHashes(pau);
	poll.abortPoll();
      }
    }
  }

  /**
   * Call a poll.  Used by NodeManagerImpl; V1 only.
   * @param pollspec the <code>PollSpec</code> that defines the subject of
   *                 the <code>Poll</code>.
   * @return the poll, if it was successfuly called, else null.
   */
  public Poll callPoll(PollSpec pollspec) {
    if (pollspec.getProtocolVersion() != 1) {
      throw new IllegalArgumentException("V1 method called with: "+pollspec);
    }
    return callPoll0(pollspec);
  }
  

  /**
   * Call a poll.  Used by PollStarter.
   * @param pollspec the <code>PollSpec</code> that defines the subject of
   *                 the <code>Poll</code>.
   * @param au
   * @return the poll, if it was successfuly called, else null.
   */
  public Poll callPoll(ArchivalUnit au, PollSpec pollspec) {
    AuState auState = AuUtil.getAuState(au);
    auState.pollAttempted();
    return callPoll0(pollspec);
  }

  private Poll callPoll0(PollSpec pollspec) {
    String errMsg = null;
    PollFactory pollFact = getPollFactory(pollspec);
    if (pollFact != null) {
      long duration = pollFact.calcDuration(pollspec, this);
      if (duration > 0) {
	try {
	  PeerIdentity orig =
	    theIDManager.getLocalPeerIdentity(pollspec.getProtocolVersion());
	  BasePoll thePoll =
	    makePoller(pollspec, duration, orig);
	  if (thePoll != null) {
	    return thePoll;
	  } else {
	    theLog.debug("makePoller(" + pollspec + ") returned null");
	  }
	} catch (ProtocolException ex) {
	  theLog.debug("Error in makePoller or callPoll", ex);
	}
      } else {
	errMsg = "Too busy";
	theLog.debug("No duration within limit");
      }
    } else {
      errMsg = "Unknown poll version: " + pollspec.getProtocolVersion();
    }
    theLog.debug("Poll not started: " + errMsg + ", au: " + pollspec.getAuId());
    return null;
  }

  /**
   * Is a poll of the given type and spec currently running
   * @param spec the PollSpec definining the location of the poll.
   * @return true if we have a poll which is running that matches pollspec
   * 
   * @deprecated  This method may be removed in a future release.
   */
  public boolean isPollRunning(PollSpec spec) {
    return entryManager.isPollRunning(spec);
  }
  
  /**
   * Is a poll of the given ArchivalUnit currrently running
   * @param au the ArchivalUnit
   * @return true if we have a poll which is running for the ArchivalUnit
   */
  public boolean isPollRunning(ArchivalUnit au) {
    return entryManager.isPollRunning(au);
  }

  // Used in PollerStatus.getSummary, which is V1 code.
  /** Find the poll either in current or recent polls */
  PollManagerEntry getCurrentOrRecentV1PollEntry(String key) {
    return entryManager.getCurrentOrRecentV1PollEntry(key);
  }

  // XXX: V3 -- Only required for V1 polls.
  public ActivityRegulator.Lock acquirePollLock(String key) {
    ActivityRegulator.Lock lock = null;
    PollManagerEntry pme = entryManager.getCurrentOrRecentV1PollEntry(key);
    if(pme != null) {
      PollTally tally = pme.poll.getVoteTally();
      if(tally != null) {
	lock = tally.getActivityLock();
	tally.setActivityLock(null);
      }
    }
    return lock;
  }

  /**
   * suspend a poll while we wait for a repair
   * @param key the identifier key of the poll to suspend
   */
  // XXX: V3 -- Only required for V1 polls.
  public void suspendPoll(String key) {
    PollManagerEntry pme = entryManager.suspendPoll(key);
    if (pme == null) {
      theLog.debug2("ignoring suspend request for unknown key " + key);
    } else {
      theLog.debug("suspended poll " + key);
    }
  }


  /**
   * resume a poll that had been suspended for a repair and check the repair
   * @param replayNeeded true we now need to replay the poll results
   * @param key the key of the suspended poll
   */
  // XXX: V3 -- Only required for V1 polls.
  public void resumePoll(boolean replayNeeded,
			 String key,
			 ActivityRegulator.Lock lock) {
    PollManagerEntry pme = entryManager.getCurrentPoll(key);
    if(pme == null) {
      theLog.debug2("ignoring resume request for unknown key " + key);
      return;
    }
    theLog.debug("resuming poll " + key);
    PollTally tally = pme.getPoll().getVoteTally();
    tally.setActivityLock(lock);
    long expiration = 0;
    Deadline d;
    NodeManager nm = getDaemon().getNodeManager(tally.getArchivalUnit());
    nm.startPoll(tally.getCachedUrlSet(), tally, true);
    if (replayNeeded) {
      theLog.debug2("starting replay of poll " + key);
      PollFactory pollFact = getPollFactory(pme.poll.getVersion());
      // should be equivalent to this.  is it?
      //       PollFactory pollFact = getPollFactory(pme.spec);
      if (pollFact != null) {
	expiration = pollFact.getMaxPollDuration(Poll.V1_CONTENT_POLL);
      } else {
	expiration = 0; // XXX
      }
      d = Deadline.in(expiration);
      tally.startReplay(d);
    }
    else {
      pme.poll.stopPoll();
    }
    theLog.debug3("completed resume poll " + (String) key);
  }

  /**
   * handle an incoming message packet.  This will create a poll if
   * one is not already running. It will then call recieveMessage on
   * the poll.  This was moved from node state which kept track of the polls
   * running in the node.  This will need to be moved or amended to support this.
   * @param msg the message used to generate the poll
   * @throws IOException thrown if the poll was unsuccessfully created
   */
  void handleIncomingMessage(LcapMessage msg) throws IOException {
    if (theLog.isDebug2()) theLog.debug2("Got a message: " + msg);
    // V1 only: discard duplicates; this is a no-op in V3.
    PollFactory fact = getPollFactory(msg);
    if(fact.isDuplicateMessage(msg, this)) {
      theLog.debug3("Dropping duplicate message:" + msg);
      return;
    }
    // todo(bhayes): Since PollManager is asynchronous with polls
    // closing, the check here can't always know; BasePoll objects
    // should be ready to have receiveMessage called when they are
    // closed.
    // Avoid sending messages to polls that aren't active. 
    String key = msg.getKey();
    PollManagerEntry pme;
    if (msg instanceof V1LcapMessage) {
      // Needed for TestPollManager.testCloseThePoll to test V1
      // semantics.
      pme = getCurrentOrRecentV1PollEntry(key);
    } else {
      pme = entryManager.getCurrentPoll(key);
    }
    if(pme != null) {
      if(pme.isPollCompleted() || pme.isPollSuspended()) {
	theLog.debug("Message received after poll was closed." + msg);
	return;
      }
    }
    if (msg.getOpcode() == V3LcapMessage.MSG_REPAIR_REQ) {
      if (enableRepairers) {
	// If there's both a reusable and a pme, preferRepairers tells
	// us which to go with.
	if (pme == null || preferRepairers) {
	  // Should always be on the message delivery thread, so no
	  // sync needed.
	  if (theReusableRepairer == null) {
	    theReusableRepairer = new V3ReusableRepairer(getDaemon());
	  }
	  theReusableRepairer.receiveMessage(msg);
	  return;
	}
      }
    }
    BasePoll p = findPoll(msg);
    if (p != null) {
      p.setMessage(msg);
      p.receiveMessage(msg);
    }
  }

  // todo(bhayes): This is synchronized because in the case where
  // entryManager.getCurrentPoll is null, we need to create an entry
  // [in V3, a V3Voter] and have it in the entryManager before the
  // next message comes in. In fact, findPoll is only called by the
  // RouterMessageHandler callback, which is defined to be serialized,
  // so the synchronize here isn't doing anything. It may be a
  // misguided effort to synchronize entryManager getting entries from
  // the PollStarter, or some remnant from some past requirement.
  /**
   * Find the poll defined by the <code>Message</code>.  If the poll
   * does not exist this will create a new poll (iff there are no conflicts)
   * @param msg <code>Message</code>
   * @return <code>Poll</code> which matches the message opcode, or a new
   * poll, or null if the new poll would conflict with a currently running poll.
   * @throws IOException if message opcode is unknown.
   */
  synchronized BasePoll findPoll(LcapMessage msg) throws IOException {
    String key = msg.getKey();
    BasePoll ret = null;

    PollManagerEntry pme = entryManager.getCurrentPoll(key);
    if (pme == null) {
      theLog.debug3("findPoll: Making new poll: " + key);
      // makePoll will add the poll to the entryMap.  todo(bhayes):
      // can this synchronization be improved? As it is, we lock this,
      // then makePoll will eventually grab the pollMapLock.
      ret = makePoll(msg);
      if (theLog.isDebug3()) {
	if (ret != null) {
	  theLog.debug3("findPoll: Made new poll: " + key);
	} else {
	  theLog.debug3("findPoll: Did not make new poll: " + key);
	}
      }
    } else {
      theLog.debug3("findPoll: Returning existing poll: " + key);
      ret = pme.poll;
    }
    return ret;
  }

  /**
   * make a new poll of the type and version defined by the incoming message.
   * @param msg <code>Message</code> to use for
   * @return a new Poll object of the required type, or null if we don't
   * want to run this poll now (<i>ie</i>, due to a conflict with another
   * poll).
   * @throws ProtocolException if message opcode is unknown
   */
  BasePoll makePoll(LcapMessage msg) throws ProtocolException {
    theLog.debug2("makePoll: From message: " + msg);
    // XXX: V3 Refactor - this could be cleaned up
    // Dispatch on the type of the msg.
    if (msg instanceof V1LcapMessage) {
      return makeV1Poll((V1LcapMessage)msg);
    } else if (msg instanceof V3LcapMessage) {
      return makeV3Voter((V3LcapMessage)msg);
    } else {
      throw new ProtocolException("Unexpected LCAP Message type.");
    }
  }

  /**
   * Make a V3Voter.
   */
  private BasePoll makeV3Voter(V3LcapMessage msg) throws ProtocolException {
    PollSpec spec = new PollSpec(msg);
    long duration = msg.getDuration();
    PeerIdentity orig = msg.getOriginatorId();
    String hashAlg = msg.getHashAlgorithm();

    theLog.debug("Making V3Voter from: " + spec);
    PollFactory pollFact = getPollFactory(spec);
    BasePoll poll = pollFact.createPoll(spec, getDaemon(),
					orig, duration, hashAlg, msg);
    if (poll != null && !(poll instanceof V3Voter)) {
      throw new ProtocolException("msg "+msg+
				  " made unexpected kind of poll: "+poll);
    }
    processNewPoll(poll, msg);
    return poll;
  }

  /**
   * V1 for testing only.
   */
  private BasePoll makeV1Poll(V1LcapMessage msg) throws ProtocolException {
    PollSpec spec = new PollSpec(msg);
    long duration = msg.getDuration();
    PeerIdentity orig = msg.getOriginatorId();
    String hashAlg = msg.getHashAlgorithm();

    CachedUrlSet cus = spec.getCachedUrlSet();
    // check for presence of item in the cache
    if (cus == null) {
      theLog.debug2("Ignoring poll request, don't have AU: " +
		    spec.getAuId());
      return null;
    }
    ArchivalUnit au = cus.getArchivalUnit();
    if (!spec.getPluginVersion().equals(AuUtil.getPollVersion(au))) {
      theLog.debug("Ignoring poll request for " + au.getName() +
		   " from peer " + msg.getOriginatorId() +
		   ". plugin version mismatch; have: " +
		   AuUtil.getPollVersion(au) +
		   ", need: " + spec.getPluginVersion());
      return null;
    }

    theLog.debug("Making poll from: " + spec);
    PollFactory pollFact = getPollFactory(spec);
    BasePoll poll = pollFact.createPoll(spec, getDaemon(),
					orig, duration, hashAlg, msg);
    processNewPoll(poll, msg);
    return poll;
  }

  private BasePoll makePoller(PollSpec spec,
			      long duration,
			      PeerIdentity orig) throws ProtocolException {
    theLog.debug("Making poll from: " + spec);
    // If this is a V3 PollSpec, passing null to V3PollFactory will
    // create a V3Poller
    PollFactory pollFact = getPollFactory(spec);
    String hashAlg = LcapMessage.getDefaultHashAlgorithm();
    if (hashAlg == null) {
      throw new ShouldNotHappenException("The default hash algorithm is null.");
    }
    BasePoll poll = pollFact.createPoll(spec, getDaemon(),
					orig, duration, hashAlg, null);
    processNewPoll(poll, null);
    return poll;
  }

  /**
   * If poll is not null, do what needs to be done to new polls.
   */
  private void processNewPoll(BasePoll poll, LcapMessage msg) {
    if (poll != null) {
      poll.setMessage(msg);
      // Make sure the new poll is in the entryManager before it is
      // started; PollManager will be ready to handleIncomingMessage
      // from the addPoll.
      entryManager.addPoll(poll);
      poll.startPoll();
    }
  }

  /**
   * close the poll from any further voting
   * @param key the poll signature
   */
  public void closeThePoll(String key)  {
    PollManagerEntry pme = entryManager.getCurrentPoll(key);
    if (pme == null || pme.poll == null) {
      theLog.warning("Attempt to close unknown poll : " + key);
      return;
    }
    // todo(bhayes): I believe that in V3 the poll.isPollComplete() is
    // always true at this point; in V1 calling closeThePoll() calls
    // setPollCompleted(), which makes poll.isPollComplete() true.

    // todo(bhayes): No idea what this comment is saying. V1? V3?
    // mark the poll completed because if we need to call a repair poll
    // we don't want this one to be in conflict with it.
    // PollTally tally = pme.poll.getVoteTally();
    pme.setPollCompleted();
    entryManager.allowToExpire(key);
    try {
      theIDManager.storeIdentities();
    } catch (ProtocolException ex) {
      theLog.error("Unable to write Identity DB file.");
    }

    BasePoll p = pme.getPoll();
    NodeManager nm = getDaemon().getNodeManager(p.getAu());

    // XXX: This is hacked up, admittedly.  The entire NodeManager
    //      and repository are getting overhauled anyway, so it makes
    //      no sense to do the "right" thing here by integrating this
    //      into the NodeManager somehow.
    if (p.getType() == Poll.V3_POLL) {
      // Retrieve the node state for the top-level AU
      NodeStateImpl ns = (NodeStateImpl)nm.getNodeState(p.getCachedUrlSet());
      if (ns != null) ns.closeV3Poll(p.getKey());
    }

    // XXX: V3 -- Only required for V1 polls.
    //
    // Don't tell the node manager about verify polls
    // If closing a name poll that started ranged subpolls, don't tell
    // the node manager about it until all ranged subpolls have finished
    if ((p.getType() == Poll.V1_NAME_POLL ||
        p.getType() == Poll.V1_CONTENT_POLL) &&
        !p.isSubpollRunning()) {
      V1PollTally tally = (V1PollTally)p.getVoteTally();
      // if closing last name poll, concatenate all the name lists into the
      // first tally and pass that to node manager
      if (p.getType() == Poll.V1_NAME_POLL) {
        V1PollTally lastTally = (V1PollTally)tally;
        tally = lastTally.concatenateNameSubPollLists();
      }
      theLog.debug("handing poll results to node manager: " + tally);
      nm.updatePollResults(p.getCachedUrlSet(), tally);
      // free the activity lock
      ActivityRegulator.Lock lock = tally.getActivityLock();
      if(lock != null) {
        lock.expire();
      }
    }
  }

  // Only used in V1PollFactory.
  /**
   * getActivePollSpecIterator returns an Iterator over the set of
   * PollSpec instances which currently have active polls on the given au.
   * @return Iterator over set of PollSpec
   */
  protected Iterator<PollSpec>
    getActivePollSpecIterator(ArchivalUnit au, BasePoll dontIncludePoll) {
    return entryManager.getActivePollSpecIterator(au, dontIncludePoll);
  }

  public void raiseAlert(Alert alert) {
    theAlertManager.raiseAlert(alert);
  }

  public void raiseAlert(Alert alert, String msg) {
    theAlertManager.raiseAlert(alert, msg);
  }

  /**
   * Ask that the specified poll runner task be executed.
   */
  public void runTask(PollRunner.Task task) {
    theTaskRunner.runTask(task);
  }
  
  /**
   * send a message to the multicast address for this archival unit
   * @param msg the LcapMessage to send
   * @param au the ArchivalUnit for this message
   * @throws IOException
   */
  void sendMessage(V1LcapMessage msg, ArchivalUnit au) throws IOException {
    if(theRouter != null) {
      theRouter.send(msg, au);
    }
  }

  /**
   * send a message to the unicast address given by an identity
   * @param msg the LcapMessage to send
   * @param au the ArchivalUnit for this message
   * @param id the PeerIdentity of the identity to send to
   * @throws IOException
   */
  void sendMessageTo(V1LcapMessage msg, ArchivalUnit au, PeerIdentity id)
      throws IOException {
    theRouter.sendTo(msg, au, id);
  }

  /**
   * send a message to the unicast address given by an identity
   * @param msg the LcapMessage to send
   * @param id the PeerIdentity of the identity to send to
   * @throws IOException
   */
  public void sendMessageTo(V3LcapMessage msg, PeerIdentity id)
      throws IOException {
    theRouter.sendTo(msg, id);
  }

  /**
   * @return the state directory for the given V3 poll.
   */
  // CR: add getStateDir() to BasePoll to avoid downcast
  public File getStateDir(String pollKey) {
    if (pollKey == null) return null;

    Poll p = this.getPoll(pollKey);
    if (p != null) {
      if (p instanceof V3Voter) {
        return ((V3Voter)p).getStateDir();
      } else if (p instanceof V3Poller) {
        return ((V3Poller)p).getStateDir();
      }
    }
    return null;
  }
  
  public IdentityManager getIdentityManager() {
    return theIDManager;
  }

  HashService getHashService() {
    return theHashService;
  }

  /**
   * @return the current RepairPolicy.
   */
  public RepairPolicy getRepairPolicy() {
    return theRepairPolicy;
  }

  public static void processConfigMacros(Configuration config) {
    // If local polls are enabled, ensure that BlockHasher computes and
    // checks hashes and DefaultUrlCacher computes hashes.

    if (config.getBoolean(PollManager.PARAM_V3_ENABLE_LOCAL_POLLS,
			  PollManager.DEFAULT_V3_ENABLE_LOCAL_POLLS)) {
      config.put(BlockHasher.PARAM_ENABLE_LOCAL_HASH, "true");
      if (config.get(BlockHasher.PARAM_LOCAL_HASH_ALGORITHM, null) == null) {
	config.put(BlockHasher.PARAM_LOCAL_HASH_ALGORITHM,
		   BlockHasher.DEFAULT_LOCAL_HASH_ALGORITHM);
      }
      if (config.get(DefaultUrlCacher.PARAM_CHECKSUM_ALGORITHM,
		     DefaultUrlCacher.DEFAULT_CHECKSUM_ALGORITHM) == null) {
	config.put(DefaultUrlCacher.PARAM_CHECKSUM_ALGORITHM,
		   config.get(BlockHasher.PARAM_LOCAL_HASH_ALGORITHM));
      }
    }
  }

  public void setConfig(Configuration newConfig,
			Configuration oldConfig,
			Configuration.Differences changedKeys) {

    if (changedKeys.contains(PREFIX)) {
      m_recentPollExpireTime =
	newConfig.getTimeInterval(PARAM_RECENT_EXPIRATION,
				  DEFAULT_RECENT_EXPIRATION);
      entryManager.setExpireInterval(m_recentPollExpireTime);

      enablePollers =
        newConfig.getBoolean(PARAM_ENABLE_V3_POLLER, DEFAULT_ENABLE_V3_POLLER);
      
      enableVoters =
        newConfig.getBoolean(PARAM_ENABLE_V3_VOTER, DEFAULT_ENABLE_V3_VOTER);
      
      enableRepairers =
        newConfig.getBoolean(PARAM_ENABLE_V3_REPAIRER, DEFAULT_ENABLE_V3_REPAIRER);

      preferRepairers =
        newConfig.getBoolean(PARAM_PREFER_V3_REPAIRER, DEFAULT_PREFER_V3_REPAIRER);
      
      autoPollAuClassess = newConfig.getList(PARAM_AUTO_POLL_AUS,
					     DEFAULT_AUTO_POLL_AUS);
      for (ListIterator<String> iter = autoPollAuClassess.listIterator();
	   iter.hasNext(); ) {
	iter.set(iter.next().toLowerCase());
      }

      deleteInvalidPollStateDirs =
        newConfig.getBoolean(PARAM_DELETE_INVALID_POLL_STATE_DIRS,
                             DEFAULT_DELETE_INVALID_POLL_STATE_DIRS);

      paramDiscardSavedPolls =
        newConfig.getBoolean(PARAM_DISCARD_SAVED_POLLS,
			     DEFAULT_DISCARD_SAVED_POLLS);

      paramToplevelPollInterval =
	newConfig.getTimeInterval(PARAM_TOPLEVEL_POLL_INTERVAL,
				  DEFAULT_TOPLEVEL_POLL_INTERVAL);

      pollStartInitialDelay =
        newConfig.getTimeInterval(PARAM_START_POLLS_INITIAL_DELAY,
                                  DEFAULT_START_POLLS_INITIAL_DELAY);
      paramQueueEmptySleep = newConfig.getTimeInterval(PARAM_QUEUE_EMPTY_SLEEP,
						    DEFAULT_QUEUE_EMPTY_SLEEP);
      paramMaxPollersSleep =
	newConfig.getTimeInterval(PARAM_MAX_POLLERS_SLEEP,
				  DEFAULT_MAX_POLLERS_SLEEP);
      paramPollQueueMax = newConfig.getInt(PARAM_POLL_QUEUE_MAX,
					  DEFAULT_POLL_QUEUE_MAX);

      paramRebuildPollQueueInterval =
	newConfig.getTimeInterval(PARAM_REBUILD_POLL_QUEUE_INTERVAL,
			       DEFAULT_REBUILD_POLL_QUEUE_INTERVAL);
      paramMinPollAttemptInterval =
	newConfig.getTimeInterval(PARAM_MIN_POLL_ATTEMPT_INTERVAL,
				  DEFAULT_MIN_POLL_ATTEMPT_INTERVAL);
      boolean oldEnable = enableV3Poller;
      enableV3Poller =
        newConfig.getBoolean(PARAM_ENABLE_V3_POLLER,
                             DEFAULT_ENABLE_V3_POLLER);
      maxSimultaneousPollers =
        newConfig.getInt(PARAM_MAX_SIMULTANEOUS_V3_POLLERS,
                         DEFAULT_MAX_SIMULTANEOUS_V3_POLLERS);
      enablePollStarterThrottle =
        newConfig.getBoolean(PARAM_ENABLE_POLL_STARTER_THROTTLE,
                             DEFAULT_ENABLE_POLL_STARTER_THROTTLE);
      wrongGroupRetryTime =
	newConfig.getTimeInterval(PARAM_WRONG_GROUP_RETRY_TIME,
				  DEFAULT_WRONG_GROUP_RETRY_TIME); 
      paramMinPercentForRepair =
        newConfig.getPercentage(V3Voter.PARAM_MIN_PERCENT_AGREEMENT_FOR_REPAIRS,
				V3Voter.DEFAULT_MIN_PERCENT_AGREEMENT_FOR_REPAIRS);
      paramWillingRepairerLiveness =
        newConfig.getTimeInterval(PARAM_WILLING_REPAIRER_LIVENESS,
				  DEFAULT_WILLING_REPAIRER_LIVENESS);

      paramAcceptRepairersPollPercent =
        newConfig.getPercentage(PARAM_ACCEPT_REPAIRERS_POLL_PERCENT,
				DEFAULT_ACCEPT_REPAIRERS_POLL_PERCENT);

      paramInvitationWeightAtRisk =
        newConfig.getDouble(PARAM_INVITATION_WEIGHT_AT_RISK,
			    DEFAULT_INVITATION_WEIGHT_AT_RISK);

      paramInvitationWeightAlreadyRepairable =
        newConfig.getDouble(PARAM_INVITATION_WEIGHT_ALREADY_REPAIRABLE,
			    DEFAULT_INVITATION_WEIGHT_ALREADY_REPAIRABLE);

      List<String> noInvitationIps =
	newConfig.getList(V3Poller.PARAM_NO_INVITATION_SUBNETS, null); 
      if (noInvitationIps == null || noInvitationIps.isEmpty()) {
	noInvitationSubnetFilter = null;
      } else {
	try {
	  IpFilter filter = new IpFilter();
	  filter.setFilters(noInvitationIps, Collections.EMPTY_LIST);
	  noInvitationSubnetFilter = filter;
	} catch (IpFilter.MalformedException e) {
	  theLog.warning("Malformed noInvitationIps, not installed: "
			 + noInvitationIps,
			 e);
	}
      }
      if (changedKeys.contains(PARAM_AT_RISK_AU_INSTANCES) &&
	  theIDManager != null) {
	atRiskAuInstances =
	  makeAuPeersMap(newConfig.getList(PARAM_AT_RISK_AU_INSTANCES),
			 theIDManager);
      }
      if (changedKeys.contains(PARAM_INVITATION_WEIGHT_AGE_CURVE)) {
	v3InvitationWeightAgeCurve =
	  processWeightCurve("V3 invitation weight age curve",
			     newConfig,
			     PARAM_INVITATION_WEIGHT_AGE_CURVE,
			     DEFAULT_INVITATION_WEIGHT_AGE_CURVE);
      }
//       if (changedKeys.contains(PARAM_INVITATION_WEIGHT_SAFETY_CURVE)) {
// 	v3InvitationWeightSafetyCurve =
// 	  processWeightCurve("V3 invitation weight safety curve",
// 			     newConfig,
// 			     PARAM_INVITATION_WEIGHT_SAFETY_CURVE,
// 			     DEFAULT_INVITATION_WEIGHT_SAFETY_CURVE);
//       }
      if (changedKeys.contains(PARAM_POLL_INTERVAL_AGREEMENT_CURVE)) {
	pollIntervalAgreementCurve =
	  processWeightCurve("V3 poll interval agreement curve",
			     newConfig,
			     PARAM_POLL_INTERVAL_AGREEMENT_CURVE,
			     DEFAULT_POLL_INTERVAL_AGREEMENT_CURVE);
      }
      if (changedKeys.contains(PARAM_POLL_INTERVAL_AT_RISK_PEERS_CURVE)) {
	pollIntervalAtRiskPeersCurve =
	  processWeightCurve("V3 poll interval at risk peers curve",
			     newConfig,
			     PARAM_POLL_INTERVAL_AT_RISK_PEERS_CURVE,
			     DEFAULT_POLL_INTERVAL_AT_RISK_PEERS_CURVE);
      }
      if (changedKeys.contains(PARAM_POLL_INTERVAL_AGREEMENT_LAST_RESULT)) {
	List<String> lst =
	  newConfig.getList(PARAM_POLL_INTERVAL_AGREEMENT_LAST_RESULT,
			    DEFAULT_POLL_INTERVAL_AGREEMENT_LAST_RESULT);
	Set res = new HashSet();
	for (String str : lst) {
	  res.add(Integer.valueOf(str));
	}
	pollIntervalAgreementLastResult = res;
      }
      if (changedKeys.contains(PARAM_POLL_WEIGHT_AT_RISK_PEERS_CURVE)) {
	pollWeightAtRiskPeersCurve =
	  processWeightCurve("V3 poll weight at risk peers curve",
			     newConfig,
			     PARAM_POLL_WEIGHT_AT_RISK_PEERS_CURVE,
			     DEFAULT_POLL_WEIGHT_AT_RISK_PEERS_CURVE);
      }
      if (changedKeys.contains(PARAM_ACCEPT_PROBABILITY_SAFETY_CURVE)) {
	v3AcceptProbabilitySafetyCurve =
	  processWeightCurve("V3 accept probability safety curve",
			     newConfig,
			     PARAM_ACCEPT_PROBABILITY_SAFETY_CURVE,
			     DEFAULT_ACCEPT_PROBABILITY_SAFETY_CURVE);
      }
      if (changedKeys.contains(PARAM_NOMINATION_WEIGHT_AGE_CURVE)) {
	v3NominationWeightAgeCurve =
	  processWeightCurve("V3 nomination weight age curve",
			     newConfig,
			     PARAM_NOMINATION_WEIGHT_AGE_CURVE,
			     DEFAULT_NOMINATION_WEIGHT_AGE_CURVE);
      }
      if (changedKeys.contains(PARAM_NO_AU_RESET_INTERVAL_CURVE)) {
	v3NoAuResetIntervalCurve =
	  processWeightCurve("V3 no-AU reset interval curve",
			     newConfig,
			     PARAM_NO_AU_RESET_INTERVAL_CURVE,
			     DEFAULT_NO_AU_RESET_INTERVAL_CURVE);
      }
      if (changedKeys.contains(PARAM_VOTE_RETRY_INTERVAL_DURATION_CURVE)) {
	v3VoteRetryIntervalDurationCurve =
	  processWeightCurve("V3 vote message retry interval age curve",
			     newConfig,
			     PARAM_VOTE_RETRY_INTERVAL_DURATION_CURVE,
			     DEFAULT_VOTE_RETRY_INTERVAL_DURATION_CURVE);
      }

      if (changedKeys.contains(PARAM_POLL_PRIORITY_AUID_MAP)) {
	installPollPriorityAuidMap(newConfig.getList(PARAM_POLL_PRIORITY_AUID_MAP,
						     DEFAULT_POLL_PRIORITY_AUID_MAP));
      }
      if (changedKeys.contains(PARAM_POLL_PRIORITY_AU_MAP)) {
	installPollPriorityAuMap(newConfig.getList(PARAM_POLL_PRIORITY_AU_MAP,
						     DEFAULT_POLL_PRIORITY_AU_MAP));
      }
      if (changedKeys.contains(V3PREFIX)) {
	enableLocalPolls = newConfig.getBoolean(PARAM_V3_ENABLE_LOCAL_POLLS,
						DEFAULT_V3_ENABLE_LOCAL_POLLS);
	allLocalPolls = newConfig.getBoolean(PARAM_V3_ALL_LOCAL_POLLS,
					     DEFAULT_V3_ALL_LOCAL_POLLS);
	enablePoPPolls = newConfig.getBoolean(PARAM_V3_ENABLE_POP_POLLS,
					      DEFAULT_V3_ENABLE_POP_POLLS);
	allPoPPolls = newConfig.getBoolean(PARAM_V3_ALL_POP_POLLS,
					   DEFAULT_V3_ALL_POP_POLLS);
	minTimeSinceAnyPoll =
	  newConfig.getTimeInterval(PARAM_MIN_TIME_BETWEEN_ANY_POLL,
				    DEFAULT_MIN_TIME_BETWEEN_ANY_POLL);
	maxDelayBetweenPoRMultiplier =
	  newConfig.getTimeInterval(PARAM_MAX_DELAY_BETWEEN_POR_MULTIPLIER,
				    DEFAULT_MAX_DELAY_BETWEEN_POR_MULTIPLIER);
	minAgreePeersLastPoRPoll =
	  newConfig.getInt(PARAM_MIN_AGREE_PEERS_LAST_POR_POLL,
			   DEFAULT_MIN_AGREE_PEERS_LAST_POR_POLL);
	repairerThresholdForLocal =
	  newConfig.getInt(PARAM_REPAIRER_THRESHOLD,
			   DEFAULT_REPAIRER_THRESHOLD);
      }

      needRebuildPollQueue();
    }
    if (theRepairPolicy != null) {
      // May be null in testing
      theRepairPolicy.setConfig(newConfig, oldConfig, changedKeys);
    }
    long scommTimeout =
      newConfig.getTimeInterval(BlockingStreamComm.PARAM_CONNECT_TIMEOUT,
				BlockingStreamComm.DEFAULT_CONNECT_TIMEOUT);
    long psmRunnerTimeout =
      newConfig.getTimeInterval(PsmManager.PARAM_RUNNER_IDLE_TIME,
				PsmManager.DEFAULT_RUNNER_IDLE_TIME);
    long addedTimeout = 
      newConfig.getTimeInterval(PARAM_ADDED_POLL_DELAY,
				DEFAULT_ADDED_POLL_DELAY);
    interPollStartDelay = (Math.max(scommTimeout, psmRunnerTimeout)
			   + addedTimeout);

    for (int i = 0; i < pf.length; i++) {
      if (pf[i] != null) {
	pf[i].setConfig(newConfig, oldConfig, changedKeys);
      }
    }
  }

  public boolean isV3PollPolicyEnabled() {
    return enablePoPPolls || enableLocalPolls;
  }

  AuPeersMap makeAuPeersMap(Collection<String> auPeersList,
			    IdentityManager idMgr) {
    AuPeersMap res = new AuPeersMap();
    Map<Integer,MutableInt> hist = new TreeMap<Integer,MutableInt>();
    for (String oneAu : auPeersList) {
      List<String> lst = StringUtil.breakAt(oneAu, ',', -1, true, true);
      if (lst.size() >= 2) {
	String auid = null;
	Set peers = new HashSet();
	for (String s : lst) {
	  if (auid == null) {
	    auid = s;
	  } else {
	    try {
	      PeerIdentity pid = idMgr.stringToPeerIdentity(s);
	      peers.add(pid);
	    } catch (IdentityManager.MalformedIdentityKeyException e) {
	      theLog.warning("Bad peer on at risk list for " + auid, e);
	    }
	  }
	}
	res.put(auid, peers);
	int size = peers.size();
	MutableInt n = hist.get(size);
	if (n == null) {
	  n = new MutableInt();
	  hist.put(size, n);
	}
	n.add(1);
      }
    }
    StringBuilder sb = new StringBuilder();
    sb.append("AU peers hist:\nAUs at risk on\n\tPeers");
    for (Map.Entry<Integer,MutableInt> ent : hist.entrySet()) {
      sb.append("\n");
      sb.append(ent.getKey());
      sb.append("\t");
      sb.append(ent.getValue());
    }
    theLog.debug(sb.toString());
    return res;
  }

  /** Set up poll priority map. */
  void installPollPriorityAuidMap(List<String> patternPairs) {
    if (patternPairs == null) {
      log.debug("Installing empty priority auid poll map");
      pollPriorityAuidMap = PatternFloatMap.EMPTY;
    } else {
      try {
	pollPriorityAuidMap = PatternFloatMap.fromSpec(patternPairs);
	theLog.debug("Installing poll priority auid map: " + 
		     pollPriorityAuidMap);
      } catch (IllegalArgumentException e) {
	theLog.error("Illegal poll priority auid map, ignoring", e);
	theLog.error("Poll priority auid map unchanged, still: " +
		     pollPriorityAuidMap);
      }
    }
  }

  void installPollPriorityAuMap(List<String> patternPairs) {
    if (patternPairs == null) {
      log.debug("Installing empty poll priority au map");
      pollPriorityAuMap = AuXpathFloatMap.EMPTY;
    } else {
      try {
	pollPriorityAuMap = new AuXpathFloatMap(patternPairs);
	theLog.debug("Installing poll priority au map: " + pollPriorityAuMap);
      } catch (IllegalArgumentException e) {
	theLog.error("Illegal poll priority au map, ignoring", e);
	theLog.error("Poll priority au map unchanged, still: " +
		     pollPriorityAuMap);
      }
    }
  }

  public Collection<PeerIdentity>
      getAllReputationsTransferredFrom(PeerIdentity pid) {
    return reputationTransfers.getAllReputationsTransferredFrom(pid);
  }

  public Set<PeerIdentity> getPeersWithAuAtRisk(ArchivalUnit au) {
    if (atRiskAuInstances == null) {
      return null;
    }
    return atRiskAuInstances.get(au.getAuId());
  }

  CompoundLinearSlope processWeightCurve(String name,
					 Configuration config,
					 String param,
					 String dfault) {
    String probCurve = config.get(param, dfault); 
    if (StringUtil.isNullString(probCurve)) {
      return null;
    } else {
      try {
	CompoundLinearSlope curve = new CompoundLinearSlope(probCurve);
	theLog.info("Installed " + name + ": " + curve);
	return curve;
      } catch (Exception e) {
	theLog.warning("Malformed " + name + ": " + probCurve, e);
	return null;
      }
    }
  }

  public long getWrongGroupRetryTime() {
    return wrongGroupRetryTime;
  }

  public IpFilter getNoInvitationSubnetFilter() {
    return noInvitationSubnetFilter;
  }

  public CompoundLinearSlope getInvitationWeightAgeCurve() {
    return v3InvitationWeightAgeCurve;
  }

//   public CompoundLinearSlope getInvitationWeightSafetyCurve() {
//     return v3InvitationWeightSafetyCurve;
//   }

  public CompoundLinearSlope getAcceptProbabilitySafetyCurve() {
    return v3AcceptProbabilitySafetyCurve;
  }

  public CompoundLinearSlope getNominationWeightAgeCurve() {
    return v3NominationWeightAgeCurve;
  }

  public double getInvitationWeightAtRisk() {
    return paramInvitationWeightAtRisk;
  }

  public double getInvitationWeightAlreadyRepairable() {
    return paramInvitationWeightAlreadyRepairable;
  }

  public CompoundLinearSlope getPollIntervalAgreementCurve() {
    return pollIntervalAgreementCurve;
  }

  public Set getPollIntervalAgreementLastResult() {
    return pollIntervalAgreementLastResult;
  }

  public CompoundLinearSlope getVoteRetryIntervalDurationCurve() {
    return v3VoteRetryIntervalDurationCurve;
  }

  public long getWillingRepairerLiveness() {
    return paramWillingRepairerLiveness;
  }

  public double getAcceptRepairersPollPercent() {
    return paramAcceptRepairersPollPercent;
  }

  public double getMinPercentForRepair() {
    return paramMinPercentForRepair;
  }

  public boolean isNoInvitationSubnet(PeerIdentity pid) {
    IpFilter filter = getNoInvitationSubnetFilter();
    return filter != null && pid.getPeerAddress().isAllowed(filter);
  }

  // Ensure only a single instance of a noAuSet exists for each AU, so can
  // synchronize on them and use in multiple threads.
  Map<ArchivalUnit,DatedPeerIdSet> noAuPeerSets =
    new ReferenceMap(ReferenceMap.HARD, ReferenceMap.WEAK);

  /** Return the noAuSet for the AU.  If an instance of the noAuSet for
   * this AU already exists in memory it will be returned.  The caller must
   * synchronize on that object before operating on it */
  public DatedPeerIdSet getNoAuPeerSet(ArchivalUnit au) {
    synchronized (noAuPeerSets) {
      DatedPeerIdSet noAuSet = noAuPeerSets.get(au);
      if (noAuSet == null) {
	HistoryRepository historyRepo = getDaemon().getHistoryRepository(au);
	noAuSet = historyRepo.getNoAuPeerSet();
	noAuPeerSets.put(au, noAuSet);
      }
      return noAuSet;
    }
  }

  /** Clear the noAuSet if it's older than the interval specified as a
   * function of the AU's age by v3NoAuResetIntervalCurve */
  public void ageNoAuSet(ArchivalUnit au, DatedPeerIdSet noAuSet) {
    try {
      if (noAuSet.isEmpty()) {
	return;
      }
      long lastTimestamp = noAuSet.getDate();
      if (lastTimestamp < 0) {
	return;
      }
      AuState state = AuUtil.getAuState(au);
      long auAge = TimeBase.msSince(state.getAuCreationTime());
      long threshold = (long)Math.round(v3NoAuResetIntervalCurve.getY(auAge));
      if (TimeBase.msSince(lastTimestamp) >= threshold) {
	noAuSet.clear();
	noAuSet.store(false);
      }
    } catch (IOException e) {
      // impossible with loaded PersistentPeerIdSet
      theLog.warning("Impossible error in loaded PersistentPeerIdSet", e);
    }
  }

  public PollFactory getPollFactory(PollSpec spec) {
    return getPollFactory(spec.getProtocolVersion());
  }

  public PollFactory getPollFactory(LcapMessage msg) {
    return getPollFactory(msg.getProtocolVersion());
  }

  public PollFactory getPollFactory(int version) {
    try {
      return pf[version];
    } catch (ArrayIndexOutOfBoundsException e) {
      theLog.error("Unknown poll version: " + version, e);
      return null;
    }
  }

  /**
   * Load and start V3 polls that are found in a serialized state
   * on the disk.  If the poll has expired, or if the state has been
   * corrupted, delete the poll directory.
   */
  private void preloadStoredPolls() {
    this.serializedPollers = new HashMap();
    this.serializedVoters = new HashMap();
    File stateDir = PollUtil.ensurePollStateRoot();

    File[] dirs = stateDir.listFiles();
    if (dirs == null || dirs.length == 0) {
      theLog.debug2("No saved polls found.");
      return;
    }
    for (int ix = 0; ix < dirs.length; ix++) {
      boolean restored = false;
      // 1. See if there's a serialized poller.
      if (enablePollers) {
        File poller = new File(dirs[ix],
                               V3PollerSerializer.POLLER_STATE_BEAN);
        if (poller.exists()) {
	  if (paramDiscardSavedPolls) {
	    theLog.debug("Discarding poll in directory " + dirs[ix]);
	    FileUtil.delTree(dirs[ix]);
	    continue;
	  }
          // Add this poll dir to the serialized polls map.
          try {
            V3PollerSerializer pollSerializer =
              new V3PollerSerializer(getDaemon(), dirs[ix]);
            PollerStateBean psb = pollSerializer.loadPollerState();
            // Check to see if this poll has expired.
            boolean expired = psb.getPollDeadline() <= TimeBase.nowMs();
            if (expired) {
              theLog.debug("Discarding expired poll in directory " + dirs[ix]);
              FileUtil.delTree(dirs[ix]);
              continue;
            }
            
            theLog.debug2("Found saved poll for AU " + psb.getAuId()
                          + " in directory " + dirs[ix]);
	    // CR: Should never be more than one saved poll per AU.  Don't
	    // need Set, and error if find more than one
            Set pollsForAu = null;
            if ((pollsForAu = (Set)serializedPollers.get(psb.getAuId())) == null) {
              pollsForAu = new HashSet();
              serializedPollers.put(psb.getAuId(), pollsForAu);
            }
            pollsForAu.add(dirs[ix]);
            restored = true;
          } catch (PollSerializerException e) {
            theLog.error("Exception while trying to restore poller from " +
                         "directory: " + dirs[ix] + ".  Cleaning up dir.", e);
            FileUtil.delTree(dirs[ix]);
            continue;
          }
        } else {
          theLog.debug("No serialized poller found in dir " + dirs[ix]);
        }
      }
      
      // 2. See if there's a serialized voter.
      if (enableVoters) {
        File voter = new File(dirs[ix],
                              V3VoterSerializer.VOTER_USER_DATA_FILE);
        if (voter.exists()) {
	  if (paramDiscardSavedPolls) {
	    theLog.debug("Discarding vote in directory " + dirs[ix]);
	    FileUtil.delTree(dirs[ix]);
	    continue;
	  }
          theLog.info("Found serialized voter in file: " + voter);
          try {
            V3VoterSerializer voterSerializer =
              new V3VoterSerializer(getDaemon(), dirs[ix]);
            VoterUserData vd = voterSerializer.loadVoterUserData();
            // Check to see if this poll has expired.
            boolean expired = vd.getDeadline() <= TimeBase.nowMs();
            if (expired) {
              theLog.debug("Discarding expired vote in directory " + dirs[ix]);
              FileUtil.delTree(dirs[ix]);
              continue;
            }
            
            theLog.debug2("Found saved poll for AU " + vd.getAuId()
                          + " in directory " + dirs[ix]);
            Set pollsForAu = null;
            if ((pollsForAu = (Set)serializedVoters.get(vd.getAuId())) == null) {
              pollsForAu = new HashSet();
              serializedVoters.put(vd.getAuId(), pollsForAu);
            }
            pollsForAu.add(dirs[ix]);
            restored = true;
          } catch (PollSerializerException e) {
            theLog.error("Exception while trying to restore voter from " +
                         "directory: " + dirs[ix] + ".  Cleaning up dir.", e);
            FileUtil.delTree(dirs[ix]);
            continue;
          }
        } else {
          theLog.debug("No serialized voter found in dir " + dirs[ix]);
        }
      }
      
      // If neither a voter nor a poller was found, this dir can be
      // cleaned up, unless KEEP_INVALID_POLLSTATE_DIRS is true.
      if (!restored) {
        if (deleteInvalidPollStateDirs) {
          theLog.debug("Deleting invalid poll state directory " + dirs[ix]);
          FileUtil.delTree(dirs[ix]);
        } else {
          theLog.debug("Not deleting invalid poll state directory " 
                       + dirs[ix]  + " due to config.");
        }
      }
    }
  }

  public void restoreAuPolls(ArchivalUnit au) {
    // Shouldn't happen.
    if (serializedPollers == null) {
      throw new NullPointerException("Null serialized poll map.");
    }
    if (serializedVoters == null) {
      throw new NullPointerException("Null serialized voter map.");
    }
    // Restore any pollers for this AU.
    // CR: Don't need loop here, s.b. max 1 poller per AU
    Set pollDirs = (Set)serializedPollers.get(au.getAuId());
    if (pollDirs != null) {
      Iterator pollDirIter = pollDirs.iterator();
      while (pollDirIter.hasNext()) {
        File dir = (File)pollDirIter.next();
        try {
          V3Poller p = new V3Poller(getDaemon(), dir);
          entryManager.addPoll(p);
          p.startPoll();
        } catch (PollSerializerException e) {
          theLog.error("Unable to restore poller from dir: " + dir, e);
        }
      }
      serializedPollers.remove(au.getAuId());
    }
    
    // Restore any voters for this AU.
    Set voterDirs = (Set)serializedVoters.get(au.getAuId());
    if (voterDirs != null) {
      Iterator voterDirIter = voterDirs.iterator();
      while (voterDirIter.hasNext()) {
        File dir = (File)voterDirIter.next();
        try {
          V3Voter v = new V3Voter(getDaemon(), dir);
          entryManager.addPoll(v);
          v.startPoll();
        } catch (PollSerializerException e) {
          theLog.error("Unable to restore poller from dir: " + dir, e);
        }
      }
      serializedVoters.remove(au.getAuId());
    }
  }

  public PollRunner getPollRunner() {
    return theTaskRunner;
  }

  //--------------- PollerStatus Accessors -----------------------------
  public Collection<PollManagerEntry> getV1Polls() {
    return entryManager.getV1Polls();
  }

  /**
   * Check the current policy to see if a request for a new V3Voter
   * should be rejected due to too many V3Voters already present.
   * @return true iff the number of active V3Voters is already at or
   * above the limit.
   */
  public boolean tooManyV3Voters() {
    int maxVoters =
      CurrentConfig.getIntParam(V3Voter.PARAM_MAX_SIMULTANEOUS_V3_VOTERS,
                                V3Voter.DEFAULT_MAX_SIMULTANEOUS_V3_VOTERS);
    int activeVoters = entryManager.countCurrentV3Voters();
    if (activeVoters >= maxVoters) {
      theLog.info("Maximum number of active voters is " 
	       + maxVoters + "; " + activeVoters + " are already running.");
      return true;
    }
    return false;
  }
  
  // Used by V3PollStatus.
  public Collection<V3Poller> getV3Pollers() {
    return entryManager.getV3Pollers();
  }
  
  // Used by V3PollStatus
  public Collection<V3Voter> getV3Voters() {
    return entryManager.getV3Voters();
  }

  // Used by V3PollStatus
  public BasePoll getPoll(String key) {
    PollManagerEntry pme = entryManager.getCurrentOrRecentV3PollEntry(key);
    if(pme != null) {
      return pme.getPoll();
    }
    return null;
  }

  // Used only in TestPollManager
  void addPoll(BasePoll p) {
    entryManager.addPoll(p);
  }

  // Used only in TestPollManager
  boolean isPollActive(String key) {
    PollManagerEntry pme = entryManager.getCurrentPoll(key);
    return (pme != null) ? pme.isPollActive() : false;
  }

  // Used only in TestPollManager
  boolean isPollClosed(String key) {
    return entryManager.isPollClosed(key);
  }

  // Used only in TestPollManager
  boolean isPollSuspended(String key) {
    PollManagerEntry pme = entryManager.getCurrentPoll(key);
    return (pme != null) ? pme.isPollSuspended() : false;
  }

  // Used only in TestPollManager
  static BasePoll makeTestPoll(LcapMessage msg) throws ProtocolException {
    if(theManager == null) {
      theManager = new PollManager();
    }
    return theManager.makePoll(msg);
  }

  long getSlowestHashSpeed() {
    return theSystemMetrics.getSlowestHashSpeed();
  }

  long getBytesPerMsHashEstimate()
      throws SystemMetrics.NoHashEstimateAvailableException {
    return theSystemMetrics.getBytesPerMsHashEstimate();
  }

  // Used only in V1PollFactory.
  public boolean hasPoll(String key) {
    return entryManager.hasPoll(key);
  }
  
  public V3PollStatusAccessor getV3Status() {
    return v3Status;
  }

  // ----------------  Callbacks -----------------------------------

  class RouterMessageHandler implements LcapRouter.MessageHandler {
    public void handleMessage(LcapMessage msg) {
      theLog.debug3("received from router message:" + msg.toString());
      try {
	handleIncomingMessage(msg);
      }
      catch (ProtocolException ex) {
	theLog.error("handleIncomingMessage() threw " + ex);
      }
      catch (IOException ex) {
	theLog.error("handleIncomingMessage() threw", ex);
      }
    }
  }

  /**
   * <p>PollManagerEntry: </p>
   * <p>Description: Class to represent the data store in the polls table.
   * @version 1.0
   */
  public static class PollManagerEntry {
    private final BasePoll poll;
    private final PollSpec spec;
    private final Deadline pollDeadline;
    private final int type;
    private final String key;

    PollManagerEntry(BasePoll p) {
      poll = p;
      spec = p.getPollSpec();
      type = p.getPollSpec().getPollType();
      pollDeadline = p.getDeadline();
      key = p.getKey();
    }

    // Note: V3Poller and V3Voter have only two states: excatly one of
    // isPollActive or isPollCompleted will return true. In V1, there
    // is also a "suspended" state.

    boolean isPollActive() {
      return poll.isPollActive();
    }

    boolean isPollCompleted() {
      return poll.isPollCompleted();
    }

    boolean isPollSuspended() {
      if (isV3Poll()) return false;
      return poll.getVoteTally().stateIsSuspended();
    }

    synchronized void setPollCompleted() {
      // todo(bhayes): Why is this synchronized?
      if (!isV3Poll()) {
        PollTally tally = poll.getVoteTally();
        tally.tallyVotes();
      }
    }

    synchronized void setPollSuspended() {
      // todo(bhayes): Why is this synchronized?
      poll.getVoteTally().setStateSuspended();
    }

    public String getStatusString() {
      // Hack for V3.
      if (isV3Poll()) {
        return poll.getStatusString();
      } else {
        if (isPollCompleted()) {
          return poll.getVoteTally().getStatusString();
        }
        else if(isPollActive()) {
          return "Active";
        }
        else if(isPollSuspended()) {
          return "Repairing";
        }
        return "Unknown";
      }
    }

    public String getTypeString() {
      return Poll.POLL_NAME[type];
    }

    // Only used in V1
    public String getShortKey() {
      return(key.substring(0,10));
    }

    public String getKey() {
      return key;
    }

    public BasePoll getPoll() {
      return poll;
    }

    public int getType() {
      return type;
    }

    public PollSpec getPollSpec() {
      return spec;
    }

    // V1 only; V3 uses the current deadline in the V3Poller and V3Voter.
    /** Return the Deadline in effect when the poll was first created. */
    public Deadline getPollDeadline() {
      return pollDeadline;
    }

    public boolean isSamePoll(PollSpec otherSpec) {
      if(this.type == otherSpec.getPollType()) {
	return this.spec.getCachedUrlSet().equals(otherSpec.getCachedUrlSet());
      }
      return false;
    }

    /**
     * Convenience method
     * @return True iff this is a V3 poll.
     */
    // XXX: V3 -- Remove when V1 polling is no longer supported.
    public boolean isV3Poll() {
      return (this.type == Poll.V3_POLL);
    }
  }
  
  /*
   * XXX:  This is a temporary class to hold AU-specific status for
   *       V3 polls.  Eventually, the goal is to replace the current
   *       node and poll history with a centralized V3-centric poll
   *       history mechanism.  Until then, this in-memory structure will
   *       hold poll history for V3 AUs between reboots.
   */
  public class V3PollStatusAccessor {
    HashMap map;
    
    long nextPollStartTime = -1;
    
    public V3PollStatusAccessor() {
      map = new HashMap();
    }
    
    private synchronized V3PollStatusAccessorEntry getEntry(String auId) {
      V3PollStatusAccessorEntry e = (V3PollStatusAccessorEntry)map.get(auId);
      if (e == null) {
        e = new V3PollStatusAccessorEntry();
        map.put(auId, e);
      }
      return e;
    }
    
    /**
     * Set the last completed V3 poll time for an AU.
     * 
     * @param auId The ID of the Archival Unit.
     * @param lastPollTime The timestamp of the last completed V3 poll.
     */
    public void setLastPollTime(String auId, long lastPollTime) {
      getEntry(auId).lastPollTime = lastPollTime;
    }
    
    /**
     * Get the last completed V3 poll time for an AU.
     * 
     * @param auId The ID of the Archival Unit.
     * @return The timestamp of the last completed V3 poll.
     */
    public long getLastPollTime(String auId) {
      return getEntry(auId).lastPollTime;
    }

    /**
     * Increment the number of completed V3 polls for an AU.
     * 
     * @param auId The ID of the Archival Unit.
     */
    public void incrementNumPolls(String auId) {
      getEntry(auId).numPolls++;
    }
    
    /**
     * Return the number of polls (since the last restart) for
     * an Archival Unit.
     * 
     * @param auId  The ID of the Archival Unit.
     * @return The number of completed V3 polls since the last
     *         daemon restart.
     */
    public int getNumPolls(String auId) {
      return getEntry(auId).numPolls;
    }
    
    /**
     * Set the percent agreement for an archival unit as of the
     * last completed V3 poll.
     * 
     * @param auId The ID of the Archival Unit.
     * @param agreement  The percent agreement as of the last completed V3 poll.
     */
    public void setAgreement(String auId, float agreement) {
      getEntry(auId).agreement = agreement;
    }

    /**
     * Return the percent agreement for an archival unit as of the last
     * completed V3 poll.
     * 
     * @param auId The ID of the Archival Unit.
     * @return The percent agreement as of the last completed V3 poll.
     */
    public float getAgreement(String auId) {
      return getEntry(auId).agreement;
    }
    
    public void setNextPollStartTime(Deadline when) {
      if (when == null) {
        nextPollStartTime = -1;
      } else {
        nextPollStartTime = when.getExpirationTime();
      }
    }
    
    public Deadline getNextPollStartTime() {
      if (nextPollStartTime > -1) {
        return Deadline.restoreDeadlineAt(nextPollStartTime);
      } else {
        return null;
      }
    }

    /**
     * Clear the poll history map.
     */
    public synchronized void clear() {
      map.clear();
    }
  }

  /*
   * Just a struct to hold status information per-au
   */
  // CR: Seth thinks this is redundant
  private static class V3PollStatusAccessorEntry {
    public long lastPollTime = -1;
    public int numPolls = 0;
    public float agreement = 0.0f;
  }
  
  /** LOCKSS Runnable responsible for occasionally scanning for AUs that
   * need polls.
   */
  private class PollStarter extends LockssRunnable {
    
    static final String PRIORITY_PARAM_POLLER = "Poller";
    static final int PRIORITY_DEFAULT_POLLER = Thread.NORM_PRIORITY - 1;
        
    private LockssDaemon lockssDaemon;
    private PollManager pollManager;
    
    private volatile boolean goOn = true;

    public PollStarter(LockssDaemon lockssDaemon,
                       PollManager pollManager) {
      super("PollStarter");
      this.lockssDaemon = lockssDaemon;
      this.pollManager = pollManager;
    }

    public void lockssRun() {
      // Triggur the LockssRun thread watchdog on exit.
      triggerWDogOnExit(true);

      setPriority(PRIORITY_PARAM_POLLER, PRIORITY_DEFAULT_POLLER);
      
      if (goOn) {
        try {
          theLog.debug("Waiting until AUs started");
          lockssDaemon.waitUntilAusStarted();
          Deadline initial = Deadline.in(pollStartInitialDelay);
          pollManager.getV3Status().setNextPollStartTime(initial);
          initial.sleep();
        } catch (InterruptedException e) {
          // just wakeup and check for exit
        }
      }
      
      while (goOn) {
	pollManager.getV3Status().setNextPollStartTime(null);
	try {
	  startOnePoll();
	} catch (RuntimeException e) {
	  // Can happen if AU deactivated recently
	  theLog.debug2("Error starting poll", e);
	  // Avoid tight loop if startOnePoll() throws.  Just being extra
	  // cautious in case another bug similar to Roundup 4091 arises.
	  try {
	    Deadline errorWait = Deadline.in(Constants.MINUTE);
	    errorWait.sleep();
	  } catch (InterruptedException ign) {
	    // ignore
	  }
	  
	} catch (InterruptedException e) {
	  // check goOn
	}
      }
    }

    public void stopPollStarter() {
      goOn = false;
      interruptThread();
    }
    
  }

  boolean startOnePoll() throws InterruptedException {
    if (!enableV3Poller) {
      startOneWait.expireIn(paramMaxPollersSleep);
    } else {
      int activePollers = entryManager.countCurrentV3Pollers();
      if (activePollers >= maxSimultaneousPollers) {
	startOneWait.expireIn(paramMaxPollersSleep);
      } else {
	PollReq req = pollQueue.nextReq();
	if (req != null) {
	  startPoll(req);
	  // todo(bhayes): This seems odd; I would expect that this
	  // would be done inside nextReq().
	  pollQueue.cancelAuPolls(req.au);
	  return true;
	} else {
	  startOneWait.expireIn(paramQueueEmptySleep);
	}
      }
    }
    v3Status.setNextPollStartTime(startOneWait);
    while (!startOneWait.expired()) {
      try {
	startOneWait.sleep();
      } catch (InterruptedException e) {
	// just wakeup and check
      }
    }
    return false;
  }


  public List<String> getAutoPollAuClasses() {
    return autoPollAuClassess;
  }

  public List<ArchivalUnit> getPendingQueueAus() {
    return pollQueue.getPendingQueueAus();
  }

  public void enqueueHighPriorityPoll(ArchivalUnit au, PollSpec spec) 
      throws NotEligibleException {
    if (au.getAuId() != spec.getAuId()) {
      throw new IllegalArgumentException("auId in au \""+au.getAuId()
					 +"\" does not match auId in spec \""
					 +spec.getAuId()+"\"");
    }
    PollReq req = new PollManager.PollReq(au)
      .setPollSpec(spec)
      .setPriority(2);
    enqueueHighPriorityPoll(req);
  }

  private void enqueueHighPriorityPoll(PollReq req)
      throws NotEligibleException {
    theLog.debug2("enqueueHighPriorityPoll(" + req + ")");
    if (!req.isHighPriority()) {
      throw new IllegalArgumentException(
        "High priority polls must have a positive priority: "+req);
    }
    // the check will throw NotEligibleException with an appropriate message.
    checkEligibleForPoll(req);
    pollQueue.enqueueHighPriorityPoll(req);
  }

  void needRebuildPollQueue() {
    // Expiration of these timers causes nextReq() to rebuild the poll
    // queue the next time it's called.  As it doesn't trigger an immediate
    // event, there's no need for a short delay.
    pollQueue.needRebuild();
    startOneWait.expire();
  }

  // testing will override.
  protected List<ArchivalUnit>
      weightedRandomSelection(Map<ArchivalUnit, PollWeight> weightMap, int n) {
    HashMap<ArchivalUnit, Double> tempMap = new HashMap<ArchivalUnit, Double>();
    for (ArchivalUnit au : weightMap.keySet()) {
      tempMap.put(au, weightMap.get(au).value());
    }
    return (List<ArchivalUnit>)CollectionUtil.
      weightedRandomSelection(tempMap, n);
  }

  /** Used to convey reason an AU is ineligible to be polled to clients for
   * logging/display */
  public class NotEligibleException extends Exception {
    public NotEligibleException(String msg) {
      super(msg);
    }
  }

  private boolean isEligibleForPoll(PollReq req) {
    try {
      checkEligibleForPoll(req);
      return true;
    } catch (NotEligibleException e) {
      return false;
    }
  }

  // todo(bhayes): DebugPanel calls this, assuming the default is a V3
  // Content Poll. On the other hand, it also creates an explicit
  // PollSpec for enqueueHighPriorityPoll. It feels as if in this API
  // the two calls should be parallel. Also possible would be for
  // checkEligibleForPoll to let the caller know if a high priority
  // would be eligible when a default priority would not.
  /**
   * @return true iff the ArchivalUnit would be eligible for a
   * "default" V3 Content Poll.
   */
  public void checkEligibleForPoll(ArchivalUnit au)
      throws NotEligibleException {
    // todo(bhayes): This is creating a PollReq with no PollSpec,
    // purely for the prupose of checking eligibility, which happens
    // to not use the spec.
    checkEligibleForPoll(new PollReq(au));
  }

  private void checkEligibleForPoll(PollReq req)
      throws NotEligibleException {
    ArchivalUnit au = req.au;

    // If a poll is already running, don't start another one.
    if (entryManager.isPollRunning(au)) {
      throw new NotEligibleException("AU is already running a poll.");
    }

    checkAuClassAllowed(req);

    if (req.isHighPriority()) {
      return;
    }

    // Following tests suppressed for high priority (manually enqueued)
    // polls

    AuState auState = AuUtil.getAuState(req.au);
    // Does AU want to be polled?
    if (!au.shouldCallTopLevelPoll(auState)) {
      throw new NotEligibleException("AU does not want to be polled.");
    }

    // Do not call polls on AUs that have not crawled, UNLESS that AU
    // is marked pubdown.
    if (!auState.hasCrawled() && !AuUtil.isPubDown(au)) {
      theLog.debug3("Not crawled or down, not calling a poll on " + au);
      throw new NotEligibleException("AU has not crawled and is not marked down.");
    }

    long sinceLast = TimeBase.msSince(auState.getLastPollAttempt());
    if (sinceLast < paramMinPollAttemptInterval) {
      String msg = "Poll attempted too recently (" +
	StringUtil.timeIntervalToString(sinceLast) + " < " +
	StringUtil.timeIntervalToString(paramMinPollAttemptInterval) + ").";
      theLog.debug3(msg + " " + au);
      throw new NotEligibleException(msg);
    }
  }

  void checkAuClassAllowed(PollReq req) throws NotEligibleException {
    if (autoPollAuClassess.contains("all")) {
      return;
    }
    ArchivalUnit au = req.au;
    if (pluginMgr.isInternalAu(au) && autoPollAuClassess.contains("internal")) {
      return;
    }
    if (req.isHighPriority() &&
	autoPollAuClassess.contains("priority")) {
      return;
    }
    throw
      new NotEligibleException("Only AU classes {" +
			       StringUtil.separatedString(autoPollAuClassess,
							  ", ") +
			       "} are allowed to poll.");
  }

  /** Return a number proportional to the desirability of calling a poll on
   * the AU. */
  PollWeight pollWeight(ArchivalUnit au) throws NotEligibleException {
    checkEligibleForPoll(au);
    AuState auState = AuUtil.getAuState(au);
    long lastEnd = auState.getLastTopLevelPollTime();
    long pollInterval;
    if (pollIntervalAgreementCurve != null &&
	pollIntervalAgreementLastResult.contains(auState.getLastPollResult())) {
      int agreePercent = (int)Math.round(auState.getV3Agreement() * 100.0);
      pollInterval = (int)pollIntervalAgreementCurve.getY(agreePercent);
      theLog.debug3("Poll interval from curve: " + pollInterval);
    } else {
      pollInterval = paramToplevelPollInterval;
      theLog.debug3("Poll interval from prop: " + pollInterval);
    }
    int numrisk = numPeersWithAuAtRisk(au);
    if (pollIntervalAtRiskPeersCurve != null) {
      int atRiskInterval = (int)pollIntervalAtRiskPeersCurve.getY(numrisk);
      if (atRiskInterval >= 0) {
	pollInterval = Math.min(pollInterval, atRiskInterval);
      theLog.debug3("Poll interval from risk: " + pollInterval);
      }
    }
    if (lastEnd + pollInterval > TimeBase.nowMs()) {
      theLog.debug3("Not ready for poll on AU " + au + " interval: "
		    + pollInterval );
      return new PollWeight(PollVariant.PoR, 0.0);
    }
    long num = TimeBase.msSince(lastEnd);
    long denom = pollInterval + auState.getPollDuration();
    double weight = (double)num / (double)denom;
    if (pollWeightAtRiskPeersCurve != null) {
      weight *= pollWeightAtRiskPeersCurve.getY(numrisk);
    }
    if (pollPriorityAuidMap != null) {
      weight *= pollPriorityAuidMap.getMatch(au.getAuId(), 1.0f);
    }
    if (pollPriorityAuMap != null) {
      weight *= pollPriorityAuMap.getMatch(au, 1.0f);
    }
    long maxDelayBetweenPoR = pollInterval * maxDelayBetweenPoRMultiplier;
    return new PollWeight(choosePollVariant(au, maxDelayBetweenPoR), weight);
  }

  int numPeersWithAuAtRisk(ArchivalUnit au) {
    Set peers = getPeersWithAuAtRisk(au);
    if (peers == null) {
      return 0;
    }
    return peers.size();
  }

  /**
   * Decide which variant of the poll to call. NB - this is called
   * for every AU every time the queue is rebuilt, so it needs to
   * be as fast as possible.
   * @return the code for the variant of the poll to call.
   * @param au the AU to poll on
   *
   * XXX DSHR should be protected but this requires moving the
   * XXX test code from TestV3Poller.
   */
  public PollVariant choosePollVariant(ArchivalUnit au,
				       long maxDelayBetweenPoR) {
    // Poll is PoR unless conditions fulfilled
    PollVariant ret = PollVariant.PoR;
    AuState aus = AuUtil.getAuState(au);
    // This is now time of last PoR time, not last poll
    long lastPollTime = aus.getLastTopLevelPollTime();
    // XXX TAL says changes on successful fetch even if unchanged,
    // XXX docs say changes when new version created.
    long lastContentChange = aus.getLastContentChange();
    int agreePeersLastPoll = aus.getNumAgreePeersLastPoR();
    long timeSinceAnyPoll = TimeBase.msSince(aus.getLastPollStart());
    int numCurrentSuspectVersions = aus.getNumCurrentSuspectVersions();
    int minAgreePeersLastPoll = minAgreePeersLastPoRPoll; // XXX
    int willingRepairers = aus.getNumWillingRepairers(); 
    int repairerThreshold = repairerThresholdForLocal;

    aus.batchSaves();

    if (agreePeersLastPoll < 0) {
      // This AuState has not been initialized with this field before.
      // We initialize it here rather than in AuState because doing this
      // for every AU will take a long time and thrash the agreeMap cache,
      // so doing it in the constructor is a bad idea. Doing it here
      // means that the first queue rebuild will be slow, but that is OK.

      // XXX ER/EE this count s.b. derived from symmetric voters' agreement
      // value, not pollers.  (Likely the same except in cases where poller
      // has only a small subset of voter's files.)
      agreePeersLastPoll = countLastPoRAgreePeers(au, aus);
      aus.setNumAgreePeersLastPoR(agreePeersLastPoll);
    }
    if (willingRepairers < 0) {
      // Same for willingRepairers
      willingRepairers = theIDManager.countCachesToRepairFrom(au);
      theLog.debug3("initializing willing repairers to " + willingRepairers);
      aus.setNumWillingRepairers(willingRepairers);
    }
    if (numCurrentSuspectVersions < 0) {
      numCurrentSuspectVersions = countCurrentSuspectVersions(au, aus);
      aus.setNumCurrentSuspectVersions(numCurrentSuspectVersions);
    }

    aus.unBatchSaves();

    // XXX BH When we track the agreement from different variant polls
    // XXX separately minAgreePeersLastPoll can be the quorum, but until
    // XXX then there is a possibility of undercounting. DSHR hack
    // XXX set it to quorum-1.
    theLog.debug3("Last content change " + lastContentChange +
		  " Agree last poll " + agreePeersLastPoll +
		  " Time since poll " + timeSinceAnyPoll +
		  " Time since last PoR " + TimeBase.msSince(lastPollTime) +
		  " Min time since poll " + minTimeSinceAnyPoll +
		  " Suspect versions " + numCurrentSuspectVersions +
		  " Min agree last poll " + minAgreePeersLastPoll +
		  " Repairers " + willingRepairers +
		  " Repair thresh " + repairerThreshold +
		  " Enable local " + enableLocalPolls +
		  " Enable PoP " + enablePoPPolls +
		  " AU " + au);
    if (timeSinceAnyPoll < minTimeSinceAnyPoll) {
      // Too soon to do anything
      ret = PollVariant.NoPoll;
      theLog.debug3("Too soon for next poll of any kind");
    } else if (enablePoPPolls && allPoPPolls) {
      // For testing
      ret = PollVariant.PoP;
      theLog.debug3("PoP poll forced");
    } else if (enableLocalPolls && allLocalPolls) {
      // For testing
      ret = PollVariant.Local;
      theLog.debug3("Local poll forced");
    } else if (numCurrentSuspectVersions > 0) {
      // Local "polling" found suspect version(s)
      ret = PollVariant.PoR; // XXX should be high priority
      theLog.debug3("Current suspect versions - PoR poll");
    } else if (TimeBase.msSince(lastPollTime) > maxDelayBetweenPoR) {
      ret = PollVariant.PoR; // XXX should be high priority
      theLog.debug3("Too long (" + TimeBase.msSince(lastPollTime) + ") since PoR poll");
    } else if (lastContentChange > lastPollTime) {
      // AU has changed since last PoR - PoR poll
      ret = PollVariant.PoR;
      theLog.debug3("New content since last PoR - PoR poll");
    } else if (agreePeersLastPoll >= minAgreePeersLastPoll) {
      // AU was in good shape the last time we looked
      if (willingRepairers < repairerThreshold) {
	// But it needs repairers
	if (enablePoPPolls && isAuEligibleForPoPPoll(au)) {
	  ret = PollVariant.PoP;
	}
	theLog.debug3("AU OK but needs repairers - PoP poll");
      } else if (enableLocalPolls) {
	// Good shape and enough repairers
	ret = PollVariant.Local;
	theLog.debug3("AU OK, has repairers - Local poll");
      }
    } else {
      // AU not in good shape
      if (log.isDebug3()) {
	long lastCrawlTime = aus.getLastCrawlTime();
	theLog.debug3("Last (poll/crawl) times: (" + lastPollTime + "/" +
		   lastCrawlTime + ") last agree" + agreePeersLastPoll +
		 " threshold " + minAgreePeersLastPoll);
      }
    }
    /* XXX DSHR - this isn't adequate. We need to know whether any
     * XXX new hashes were created during a Local poll and if they
     * XXX were ensure that the next poll is PoR. Configuration
     * XXX isn't sufficient. But it helps catch setup problems in
     * XXX unit tests and STF,
     */
    if (ret == PollVariant.PoP) {
      if (CurrentConfig.getIntParam(V3Poller.PARAM_V3_MODULUS, 0) == 0) {
	theLog.error("Not configured for PoP polls");
	ret = PollVariant.PoR;
      }
    }	
    theLog.debug2("Poll variant: " + ret);
    return ret;
  }

  boolean isAuEligibleForPoPPoll(ArchivalUnit au) {
    return !pluginMgr.isRegistryAu(au);
  }

  /**
   * @return an estimate of the number of peers that agreed in the
   * last PoR poll.
   * @param au the ArchvalUnit being polled
   * @param aus the state of the AU
   * XXX this needs to be changed when we are tracking the agreement
   * XXX from different poll variants separately to only count agreement
   * XXX PoR polls. Until then there is a possibility that the same peer's
   * XXX agreement in a subsequent PoP poll could result in an undercount.
   *
   * XXX DSHR should be protected
   */
  public int countLastPoRAgreePeers(ArchivalUnit au, AuState aus) {
    int ret = 0;
    // agreeMap maps peer ID to last agree time
    Map agreeMap = theIDManager.getAgreed(au);
    long lastPoRPoll = aus.getLastTopLevelPollTime();
    long threshold = aus.getPollDuration();
    theLog.debug3("Last PoR: " + lastPoRPoll + " thresh: " + threshold +
	       " map size " + agreeMap.size());
    // Iterate through the agreeMap counting the number of agreeing
    // peers whose times are threshold or less before lastPoRPoll
    for (Iterator it = agreeMap.entrySet().iterator(); it.hasNext(); ) {
      Map.Entry ent = (Map.Entry)it.next();
      long agreeTime = ((Long)ent.getValue()).longValue();
      long delta = lastPoRPoll - agreeTime;
      theLog.debug3("agreeTime: " + agreeTime + " delta " + delta);
      PeerIdentity pid = (PeerIdentity)ent.getKey();
      if (delta >= 0 && delta < threshold) {
	ret++;
      }
    }
    return ret;
  }

  /**
   * @return the number of URLs whose current version is suspect
   * @param au the ArchvalUnit being polled
   * @param aus the state of the AU
   */
  public static int countCurrentSuspectVersions(ArchivalUnit au, AuState aus) {
    int ret = 0;
    if (AuUtil.hasSuspectUrlVersions(au)) {
      AuSuspectUrlVersions asuv = AuUtil.getSuspectUrlVersions(au);
      ret = asuv.countCurrentSuspectVersions(au);
    }
    return ret;
  }

  boolean startPoll(PollReq req) {
    ArchivalUnit au = req.getAu();
    if (entryManager.isPollRunning(au)) {
      theLog.debug("Attempted to start poll when one is already running: " +
		   au.getName());
      return false;
    }

    PollSpec spec = req.getPollSpec();
    theLog.debug("Calling a V3 " + spec.getPollVariant() + " poll on AU " + au);

    if (callPoll(au, spec) == null) {
      theLog.debug("pollManager.callPoll returned null. Failed to call "
		   + "a V3 poll on " + au);
      return false;
    }
        
    // Add a delay to throttle poll starting.  The delay is the sum of 
    // the scomm timeout and an additional number of milliseconds.
    if (enablePollStarterThrottle) {
      try {
	Deadline dontStartPollBefore = Deadline.in(interPollStartDelay);
	v3Status.setNextPollStartTime(dontStartPollBefore);
	dontStartPollBefore.sleep();
      } catch (InterruptedException ex) {
	// Just proceed to the next poll.
      }
      v3Status.setNextPollStartTime(null);
    }
    return true;
  }
  
  private Set recalcingAus = Collections.synchronizedSet(new HashSet());

  /** Remember that we have scheduled a hash to recalculate the hash time
   * for this AU */
  public void addRecalcAu(ArchivalUnit au) {
    recalcingAus.add(au);
  }

  /** Done with hash to recalculate the hash time for this AU */
  public void removeRecalcAu(ArchivalUnit au) {
    recalcingAus.remove(au);
  }

  /** Return true if already have scheduled a hash to recalculate the hash
   * time for this AU */
  public boolean isRecalcAu(ArchivalUnit au) {
    return recalcingAus.contains(au);
  }

  public enum EventCtr {Polls,
      Invitations,
      Accepted,
      Declined,
      Voted,
      ReceivedVoteReceipt,
      };

  Map<EventCtr,MutableInt> eventCounters =
    new EnumMap<EventCtr,MutableInt>(EventCtr.class);
  
  Map<PollNak,MutableInt> voterNakEventCounters =
    new EnumMap<PollNak,MutableInt>(PollNak.class);
  
  int[] pollEndEventCounters = new int[POLLER_STATUS_STRINGS.length];

  public void countEvent(EventCtr c) {
    synchronized (eventCounters) {
      MutableInt n = eventCounters.get(c);
      if (n == null) {
	n = new MutableInt();
	eventCounters.put(c, n);
      }
      n.add(1);
    }
  }

  public void countVoterNakEvent(PollNak nak) {
    synchronized (voterNakEventCounters) {
      MutableInt n = voterNakEventCounters.get(nak);
      if (n == null) {
	n = new MutableInt();
	voterNakEventCounters.put(nak, n);
      }
      n.add(1);
    }
    countEvent(EventCtr.Declined);
  }

  public void countPollEndEvent(int status) {
    synchronized (pollEndEventCounters) {
      pollEndEventCounters[status]++;
    }
  }

  public int getEventCount(EventCtr c) {
    synchronized (eventCounters) {
      MutableInt n = eventCounters.get(c);
      return n == null ? 0 : n.intValue();
    }
  }

  public int getVoterNakEventCount(PollNak c) {
    synchronized (voterNakEventCounters) {
      MutableInt n = voterNakEventCounters.get(c);
      return n == null ? 0 : n.intValue();
    }
  }

  public int getPollEndEventCount(int status) {
    return pollEndEventCounters[status];
  }

  public int getNumActiveV3Votes() {
    return entryManager.countCurrentV3Voters();
  }

  public int getNumActiveV3Polls() {
    return entryManager.countCurrentV3Pollers();
  }

  static class PollWeight {
    V3Poller.PollVariant pollVariant;
    double value;

    PollWeight(V3Poller.PollVariant v, double w) {
      pollVariant = v;
      if (v != V3Poller.PollVariant.NoPoll) {
	value = w;
      } else {
	value = 0.0;
      }
    }

    V3Poller.PollVariant pollVariant() {
      return pollVariant;
    }

    Double value() {
      return new Double(value);
    }
  }
}
