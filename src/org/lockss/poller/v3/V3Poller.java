/*
 * $Id: V3Poller.java,v 1.56.2.4 2007-09-11 06:34:51 smorabito Exp $
 */

/*

Copyright (c) 2000-2005 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.poller.v3;

import java.io.*;
import java.net.MalformedURLException;
import java.security.*;
import java.util.*;

import org.apache.commons.collections.*;

import org.lockss.app.*;
import org.lockss.config.*;
import org.lockss.crawler.*;
import org.lockss.daemon.*;
import org.lockss.hasher.*;
import org.lockss.plugin.*;
import org.lockss.poller.*;
import org.lockss.poller.v3.V3Serializer.*;
import org.lockss.protocol.*;
import org.lockss.protocol.psm.*;
import org.lockss.protocol.V3LcapMessage.PollNak;
import org.lockss.repository.*;
import org.lockss.scheduler.*;
import org.lockss.scheduler.Schedule.*;
import org.lockss.state.*;
import org.lockss.util.*;

/**
 * <p>The caller of a V3 Poll.  This class is responsible for inviting
 * participants into a poll, tallying their votes, and taking action based
 * on the result.</p>
 */
public class V3Poller extends BasePoll {

  // Status strings used by the peers.
  public static final int PEER_STATUS_INITIALIZED = 0;
  public static final int PEER_STATUS_WAITING_POLL_ACK = 1;
  public static final int PEER_STATUS_ACCEPTED_POLL = 2;
  public static final int PEER_STATUS_NOMINATED = 3;
  public static final int PEER_STATUS_WAITING_VOTE = 4;
  public static final int PEER_STATUS_VOTED = 5;
  public static final int PEER_STATUS_COMPLETE = 6;
  public static final int PEER_STATUS_ERROR = 7;
  public static final int PEER_STATUS_DROPPED_OUT = 8;
  public static final int PEER_STATUS_DECLINED_POLL = 9;

  public static final String[] PEER_STATUS_STRINGS =
  {
   "Initialized", "Invited", "Accepted Poll", "Sent Nominees",
   "Waiting for Vote", "Voted", "Complete", "Error", "Dropped Out",
   "Declined Poll"
  };

  public static final int POLLER_STATUS_STARTING = 0;
  public static final int POLLER_STATUS_NO_TIME = 1;
  public static final int POLLER_STATUS_RESUMING = 2;
  public static final int POLLER_STATUS_INVITING_PEERS = 3;
  public static final int POLLER_STATUS_HASHING = 4;
  public static final int POLLER_STATUS_TALLYING = 5;
  public static final int POLLER_STATUS_COMPLETE = 6;
  public static final int POLLER_STATUS_NO_QUORUM = 7;
  // Some error occured
  public static final int POLLER_STATUS_ERROR = 8;
  // The poll expired while it was hibernated
  public static final int POLLER_STATUS_EXPIRED = 9;
  public static final int POLLER_STATUS_WAITING_REPAIRS = 10;

  public static final String[] POLLER_STATUS_STRINGS =
  {
   "Starting", "No Time Available", "Resuming", "Inviting Peers", "Hashing",
   "Tallying", "Complete", "No Quorum", "Error", "Expired", 
   "Waiting for Repairs"
  };

  private static String PREFIX = Configuration.PREFIX + "poll.v3.";

  /** Quorum for V3 polls. */
  public static final String PARAM_QUORUM = PREFIX + "quorum";
  public static final int DEFAULT_QUORUM = 5;

  /** Minimum number of participants for a V3 poll. */
  public static final String PARAM_MIN_POLL_SIZE = PREFIX + "minPollSize";
  public static final int DEFAULT_MIN_POLL_SIZE = 5;

  /** Maximum number of participants for a V3 poll. */
  public static final String PARAM_MAX_POLL_SIZE = PREFIX + "maxPollSize";
  public static final int DEFAULT_MAX_POLL_SIZE = 10;

  /** Target size of the outer circle poll. */
  public static final String PARAM_TARGET_OUTER_CIRCLE_SIZE =
    PREFIX + "targetOuterCircle";
  public static final int DEFAULT_TARGET_OUTER_CIRCLE_SIZE = 10;
  
  /** The maximum allowable number of simultaneous V3 Pollers */
  public static final String PARAM_MAX_SIMULTANEOUS_V3_POLLERS =
    PREFIX + "maxSimultaneousV3Pollers";
  public static final int DEFAULT_MAX_SIMULTANEOUS_V3_POLLERS = 20;

  /** If true, drop participants from this poll that do not send
   * outer circle nominees. */
  public static final String PARAM_DROP_EMPTY_NOMINATIONS =
    PREFIX + "dropEmptyNominations";
  public static final boolean DEFAULT_DROP_EMPTY_NOMINATIONS = false;
  
  /** If true, keep inviting peers into the poll until either enough
   * participants have agreed to participate, OR the vote timer expires,
   * whichever comes first.
   */
  public static final String PARAM_ENABLE_INVITATIONS =
    PREFIX + "enableFollowupInvitations";
  public static final boolean DEFAULT_ENABLE_INVITATIONS = true;
  
  /** The time to wait between inviting more peers, until at least
   * MIN_POLL_SIZE have agreed to participate.
   */
  public static final String PARAM_TIME_BETWEEN_INVITATIONS =
    PREFIX + "timeBetweenInvitations";
  /** By default, wait two minutes between invitation messages and 
   * re-invitations.  This is sufficiently long for a voter to receive
   * and respond to an invitation if it has the AU and is willing to
   * participate.
   */
  public static final long DEFAULT_TIME_BETWEEN_INVITATIONS =
    2 * 60 * 1000; // 2 minutes;
  
  /** The maximum number of peers to invite into the poll during each
   * followup invitation phase.
   */
  public static final String PARAM_INVITATION_SIZE = PREFIX + "invitationSize";
  public static final int DEFAULT_INVITATION_SIZE = 10;
  
  /** If true, enable the discovery mechanism that attempts to invite peers
   * from outside our Initial Peer List into polls. */
  public static final String PARAM_ENABLE_DISCOVERY =
    PREFIX + "enableDiscovery";
  public static final boolean DEFAULT_ENABLE_DISCOVERY = true;

  /** If true, just log a message rather than deleting files that are
   * considered to be missing from a majority of peers.
   */
  public static final String PARAM_DELETE_EXTRA_FILES =
    PREFIX + "deleteExtraFiles";
  public static final boolean DEFAULT_DELETE_EXTRA_FILES =
    true;

  /**
   * Directory in which to store message data.
   */
  public static final String PARAM_V3_MESSAGE_REL_DIR =
    PREFIX + "messageRelDir";
  // Default is for production.  Override this for
  // testing.
  public static final String DEFAULT_V3_MESSAGE_REL_DIR =
    "v3state";

  /**
   * An optional multiplier to apply to the estimated hash duration
   * when computing the vote deadline.  This can be fine-tuned with
   * the voteDeadlinePadding paramter as well.
   */
  public static final String PARAM_VOTE_DURATION_MULTIPLIER =
    PREFIX + "voteDeadlineMultiplier";
  public static final int DEFAULT_VOTE_DURATION_MULTIPLIER = 15;

  /**
   * Padding to add to the scheduled vote deadline, in ms.
   */
  public static final String PARAM_VOTE_DEADLINE_PADDING =
    PREFIX + "voteDeadlinePadding";
  public static final long DEFAULT_VOTE_DEADLINE_PADDING =
    1000 * 60 * 10; // ten minutes

  public static final String PARAM_V3_TRUSTED_WEIGHT =
    PREFIX + "trustedWeight";
  public static final int DEFAULT_V3_TRUSTED_WEIGHT =
    350;

  public static final String PARAM_V3_VOTE_MARGIN =
    PREFIX + "voteMargin";
  public static final int DEFAULT_V3_VOTE_MARGIN =
    75;

  /** Define the maximum number of queued repair requests that will
   * be allowed for this poll.  Zero (0) means that no repairs are 
   * allowed.  Any value less than zero means unlimited.
   */
  public static final String PARAM_MAX_REPAIRS =  PREFIX + "maxRepairs";
  public static final int DEFAULT_MAX_REPAIRS = 1000;

  /** The maximum number of block errors that can be encountered
   * during the tally before the poll is aborted.
   */
  public static final String PARAM_MAX_BLOCK_ERROR_COUNT =
    PREFIX + "maxBlockErrorCount";
  public static final int DEFAULT_MAX_BLOCK_ERROR_COUNT = 10;

  /**
   * The amount of time, in ms, to hold the poll open past normal closing time
   * if we are waiting for pending repairs.
   */
  public static final String PARAM_V3_EXTRA_POLL_TIME =
    PREFIX + "extraPollTime";
  public static final long DEFAULT_V3_EXTRA_POLL_TIME = 
    1000 * 60 * 60; // 60 minutes
  /**
   * The probability of requesting a repair from other caches.
   */
  public static final String PARAM_V3_REPAIR_FROM_CACHE_PERCENT =
    PREFIX + "repairFromCachePercent";

  /**
   * In the absence of other advice, set the probability to match the repair
   * crawler's default probability.
   */
  public static final double DEFAULT_V3_REPAIR_FROM_CACHE_PERCENT =
    CrawlManagerImpl.DEFAULT_REPAIR_FROM_CACHE_PERCENT;
  
  public static final String PARAM_V3_ENABLE_REPAIR_FROM_CACHE =
    PREFIX + "enableRepairFromCache";
  public static final boolean DEFAULT_V3_ENABLE_REPAIR_FROM_CACHE = true;
  
  /**
   * The number of bytes to hash before saving poll status during hashing.
   */
  public static final String PARAM_V3_HASH_BYTES_BEFORE_CHECKPOINT =
    PREFIX + "hashBytesBeforeCheckpoint";
  public static final long DEFAULT_V3_HASH_BYTES_BEFORE_CHECKPOINT =
    1024 * 1024; // 1 MB


  // Global state for the poll.
  private PollerStateBean pollerState;
  // Map of PeerIdentity => ParticipantState for all participants in
  // this poll, both inner and outer circle
  protected Map theParticipants =
    Collections.synchronizedMap(new LinkedHashMap());
  private LockssDaemon theDaemon;
  private PollManager pollManager;
  private IdentityManager idManager;
  private V3PollerSerializer serializer;
  private boolean resumedPoll;
  private boolean activePoll = true;
  private boolean dropEmptyNominators = DEFAULT_DROP_EMPTY_NOMINATIONS;
  private boolean deleteExtraFiles = DEFAULT_DELETE_EXTRA_FILES;
  private File stateDir;
  // The length, in ms., to hold the poll open past normal closing if
  // a little extra poll time is required to wait for pending repairs. 
  private long extraPollTime = DEFAULT_V3_EXTRA_POLL_TIME;
  // Used by setConfig when setting or restoring state
  private int minParticipants;
  private int maxParticipants;
  private int quorum;
  private int outerCircleTarget;
  private int maxRepairs = DEFAULT_MAX_REPAIRS;
  private long voteDeadlinePadding = DEFAULT_VOTE_DEADLINE_PADDING;
  private long hashBytesBeforeCheckpoint =
    DEFAULT_V3_HASH_BYTES_BEFORE_CHECKPOINT;

  private static Logger log = Logger.getLogger("V3Poller");
  private static LockssRandom theRandom = new LockssRandom();
  private int blockErrorCount = 0;
  private int maxBlockErrorCount = DEFAULT_MAX_BLOCK_ERROR_COUNT;
  private boolean enableInvitations = DEFAULT_ENABLE_INVITATIONS;
  private long timeBetweenInvitations = DEFAULT_TIME_BETWEEN_INVITATIONS;
  private int invitationSize = DEFAULT_INVITATION_SIZE;

  private SchedulableTask task;
  private TimerQueue.Request invitationRequest;
  private TimerQueue.Request pollCompleteRequest;
  private TimerQueue.Request voteCompleteRequest;
  private long bytesHashedSinceLastCheckpoint = 0;
  private int voteDeadlineMultiplier = DEFAULT_VOTE_DURATION_MULTIPLIER;
  private boolean enableDiscovery = DEFAULT_ENABLE_DISCOVERY;
  
  // Probability of repairing from another cache.  A number between
  // 0.0 and 1.0.
  private double repairFromCache =
    V3Poller.DEFAULT_V3_REPAIR_FROM_CACHE_PERCENT;
  
  private boolean enableRepairFromCache =
    V3Poller.DEFAULT_V3_ENABLE_REPAIR_FROM_CACHE;

  /**
   * <p>Create a new Poller to call a V3 Poll.</p>
   * 
   * @param spec The PollSpec
   * @param daemon The LockssDaemon
   * @param orig The originator of the poll (i.e., my PeerIdentity)
   * @param key The key for the poll, generated by {@link V3PollFactory}
   * @param duration  The length of the poll.
   * @param hashAlg The hashing algorithm to use.
   */
  public V3Poller(PollSpec spec, LockssDaemon daemon, PeerIdentity orig,
                  String key, long duration, String hashAlg)
      throws V3Serializer.PollSerializerException {
    this.theDaemon = daemon;
    this.pollManager = daemon.getPollManager();
    this.idManager = daemon.getIdentityManager();
    if (hashAlg == null) hashAlg = LcapMessage.DEFAULT_HASH_ALGORITHM;
    // If the hash algorithm is not available, fail the poll immediately.
    try {
      MessageDigest.getInstance(hashAlg);
    } catch (NoSuchAlgorithmException ex) {
      throw new IllegalArgumentException("Algorithm " + hashAlg +
                                         " is not supported");
    }
    this.serializer = new V3PollerSerializer(theDaemon);

    // Get set-up information from config.
    setConfig();

    int pollSize = getInnerCircleSize(minParticipants,
                                      maxParticipants,
                                      quorum);
    pollerState = new PollerStateBean(spec, orig, key, duration,
                                      pollSize, outerCircleTarget,
                                      quorum, hashAlg);

    long estimatedHashTime = getCachedUrlSet().estimatedHashDuration();
    // The vote deadline is the deadline by which all voters must have
    // voted.
    long voteDeadline = TimeBase.nowMs() +
                        (estimatedHashTime * voteDeadlineMultiplier) +
                        voteDeadlinePadding;
    pollerState.setVoteDeadline(voteDeadline);

    // Checkpoint the poll.
    checkpointPoll();
  }

  /**
   * <p>Restore a V3 Poll from a serialized state.</p>
   */
  public V3Poller(LockssDaemon daemon, File pollDir)
      throws V3Serializer.PollSerializerException {
    theDaemon = daemon;
    serializer = new V3PollerSerializer(theDaemon, pollDir);
    pollerState = serializer.loadPollerState();
    int oldState = pollerState.getStatus();
    setStatus(POLLER_STATUS_RESUMING);
    // If the hash algorithm used when the poll was first created is
    // no longer available, fail the poll immediately.
    try {
      MessageDigest.getInstance(pollerState.getHashAlgorithm());
    } catch (NoSuchAlgorithmException ex) {
      throw new IllegalArgumentException("Algorithm " +
                                         pollerState.getHashAlgorithm() +
                                         " is no longer supported");
    }
    pollManager = daemon.getPollManager();
    idManager = daemon.getIdentityManager();
    setConfig();
    // Restore transient cus and pollspec in the poller state.
    PluginManager plugMgr = theDaemon.getPluginManager();
    CachedUrlSet cus = plugMgr.findCachedUrlSet(pollerState.getAuId());
    if (cus == null) {
      throw new NullPointerException("CUS for AU " + pollerState.getAuId() +
                                     " is null!");
    }
    pollerState.setCachedUrlSet(cus);
    pollerState.setPollSpec(new PollSpec(cus, Poll.V3_POLL));
    // Restore the peers for this poll.
    try {
      restoreParticipants();
    } catch (V3Serializer.PollSerializerException ex) {
      log.error("Unable to restore poll state!");
    }

    resumedPoll = true;
    setStatus(oldState);
    log.debug2("Restored serialized poll " + pollerState.getPollKey());
  }

  private void setConfig() {
    Configuration c = ConfigManager.getCurrentConfig();
    // Determine the number of peers to invite into this poll.
    minParticipants = c.getInt(PARAM_MIN_POLL_SIZE,
                               DEFAULT_MIN_POLL_SIZE);
    maxParticipants = c.getInt(PARAM_MAX_POLL_SIZE,
                               DEFAULT_MAX_POLL_SIZE);
    outerCircleTarget = c.getInt(PARAM_TARGET_OUTER_CIRCLE_SIZE,
                                 DEFAULT_TARGET_OUTER_CIRCLE_SIZE);
    quorum = c.getInt(PARAM_QUORUM, DEFAULT_QUORUM);
    dropEmptyNominators = c.getBoolean(PARAM_DROP_EMPTY_NOMINATIONS,
                                       DEFAULT_DROP_EMPTY_NOMINATIONS);
    deleteExtraFiles = c.getBoolean(PARAM_DELETE_EXTRA_FILES,
                                    DEFAULT_DELETE_EXTRA_FILES);
    voteDeadlineMultiplier = c.getInt(PARAM_VOTE_DURATION_MULTIPLIER,
				      DEFAULT_VOTE_DURATION_MULTIPLIER);
    voteDeadlinePadding = c.getTimeInterval(PARAM_VOTE_DEADLINE_PADDING,
                                            DEFAULT_VOTE_DEADLINE_PADDING);
    timeBetweenInvitations =
      c.getTimeInterval(PARAM_TIME_BETWEEN_INVITATIONS,
                        DEFAULT_TIME_BETWEEN_INVITATIONS);
    invitationSize = c.getInt(PARAM_INVITATION_SIZE, DEFAULT_INVITATION_SIZE);
    extraPollTime = c.getLong(PARAM_V3_EXTRA_POLL_TIME,
                              DEFAULT_V3_EXTRA_POLL_TIME);
    enableDiscovery = c.getBoolean(PARAM_ENABLE_DISCOVERY,
                                   DEFAULT_ENABLE_DISCOVERY);
    hashBytesBeforeCheckpoint = 
      c.getLong(PARAM_V3_HASH_BYTES_BEFORE_CHECKPOINT,
                DEFAULT_V3_HASH_BYTES_BEFORE_CHECKPOINT);
    repairFromCache = 
      c.getPercentage(PARAM_V3_REPAIR_FROM_CACHE_PERCENT,
                      DEFAULT_V3_REPAIR_FROM_CACHE_PERCENT);
    maxRepairs = c.getInt(PARAM_MAX_REPAIRS, DEFAULT_MAX_REPAIRS);
    // Determine the proper location for the V3 message dir.
    List dSpaceList =
      CurrentConfig.getList(ConfigManager.PARAM_PLATFORM_DISK_SPACE_LIST);
    String relPluginPath =
      CurrentConfig.getParam(PARAM_V3_MESSAGE_REL_DIR,
                             DEFAULT_V3_MESSAGE_REL_DIR);

    if (dSpaceList == null || dSpaceList.size() == 0) {
      log.error(ConfigManager.PARAM_PLATFORM_DISK_SPACE_LIST +
                " not specified, not configuring V3 message dir.");
    } else {
      stateDir = new File((String)dSpaceList.get(0), relPluginPath);
    }

    if (stateDir == null ||
        (!stateDir.exists() && !stateDir.mkdir()) ||
        !stateDir.canWrite()) {
      throw new IllegalArgumentException("Configured V3 data directory " +
                                         stateDir +
                                         " does not exist or cannot be " +
                                         "written to.");
    }
  }

  /**
   * <p>Reserve time in the schedule to tally the votes.</p>
   * 
   * @param maxParticipants  The maximum number of participants that will
   *        participate in this poll (invitees and outer circle)
   */
  boolean reserveScheduleTime(int maxParticipants) {
    CachedUrlSet cus = this.getCachedUrlSet();
    long estimatedHashDuration = cus.estimatedHashDuration();
    long now = TimeBase.nowMs();
    
    // Earliest time we could possibly start tallying.
    Deadline earliestStart =
      Deadline.at(now + (estimatedHashDuration * maxParticipants));
    
    // Latest time that we want to stop tallying.
    // XXX: To reserve time at the end of the poll to handle sending receipts,
    // subtract the estimated hash duration from the expiration time.  This
    // may be much more than we need.
    Deadline latestFinish =
      Deadline.at(getDeadline().getExpirationTime() - estimatedHashDuration);
    TaskCallback tc = new TaskCallback() {
      public void taskEvent(SchedulableTask task, EventType type) {
        // do nothing... yet!
      }
    };
    this.task = new StepTask(earliestStart, latestFinish,
                             estimatedHashDuration, tc, this) {
      public int step(int n) {
	// finish immediately, in case we start running
	setFinished();
        return n;
      }
    };
    boolean suc = theDaemon.getSchedService().scheduleTask(task);
    if (!suc) {
      String msg = "No time for V3 Poller in poll " + getKey() + ". " +
                   " Requested time for step task with earliest start at " +
                   earliestStart +", latest finish at " + latestFinish + ", " +
                   "with an estimated hash duration of " + estimatedHashDuration +
                   "ms as of " + TimeBase.nowDate();
      pollerState.setErrorDetail(msg);
      log.warning(msg);
    }
    return suc;
  }

  /**
   * Build the initial set of inner circle peers.
   *
   * @param pollSize The number of peers to invite.
   */
  protected void constructInnerCircle(int pollSize) {
    Collection refList = getReferenceList();
    log.debug3("constructInnerCircle: refList.size()=" + refList.size());
    log.debug3("constructInnerCircle: pollSize=" + pollSize);
    int innerCount = pollSize > refList.size() ? refList.size() : pollSize;
    Collection innerCircleVoters =
      CollectionUtil.randomSelection(refList, innerCount);
    log.debug2("Selected " + innerCircleVoters.size()
        + " participants for poll ID " + pollerState.getPollKey());
    for (Iterator it = innerCircleVoters.iterator(); it.hasNext();) {
      addInnerCircleVoter((PeerIdentity)it.next());
    }
  }
  
  /**
   * Given the total set of all known peers, and the set of peers already
   * invited, return a new list of peers that should be invited into the poll.
   * @param allPeers   All known V3 peers.
   * @param existingPeers  Peers that have already been invited.
   * @return  A new list of peers to invite.
   */
  Collection choosePeers(Collection allPeers, Collection invitedPeers,
                         int count) {
    // Subtract the peers who have already agreed to participate from
    // the list of all peers.
    //
    Collection availablePeers = new HashSet(allPeers);
    availablePeers.removeAll(invitedPeers);
    // Select 'count' peers, or the entire list, whichever is smaller.
    count = count > availablePeers.size() ? availablePeers.size() : count;
    // Select up to 'size' peers
    return CollectionUtil.randomSelection(availablePeers, count);
  }

  /**
   * Restore the set of serialized pariticipants when restoring a poll.
   *
   * @throws V3Serializer.PollSerializerException
   */
  protected void restoreParticipants()
      throws V3Serializer.PollSerializerException {
    Collection peers = serializer.loadVoterStates();
    for (Iterator iter = peers.iterator(); iter.hasNext();) {
      final ParticipantUserData voterState = (ParticipantUserData) iter.next();
      PeerIdentity id = voterState.getVoterId();
      PsmInterpStateBean pisb = voterState.getPsmInterpState();
      PsmMachine machine =
        PollerStateMachineFactory.getMachine(getPollerActionsClass());
      PsmInterp interp = new PsmInterp(machine, voterState);
      voterState.setPsmInterpState(pisb);
      interp.setCheckpointer(new PsmInterp.Checkpointer() {
        public void checkpoint(PsmInterpStateBean resumeStateBean) {
          // Replace the statebean in this machine's state object.
          voterState.setPsmInterpState(resumeStateBean);
          try {
            checkpointParticipant(voterState);
          } catch (PollSerializerException ex) {
            log.error("Unable to save participant state for peer " +
                      voterState.getVoterId());
          }
        }
      });
      voterState.setPsmInterp(interp);
      voterState.setPollState(pollerState);
      voterState.setPoller(this);
      theParticipants.put(id, voterState);
    }
  }


  /**
   * Choose the size of the inner circle for this poll.
   *
   * @param min Minimum number of peers to invite.
   * @param max Maximum number of peers to invite.
   * @param quorum Quorum size.
   * @return The number of peers to invite into the inner circle.
   */
  protected int getInnerCircleSize(int min, int max, int quorum) {
    int pollSize = 0;
    if (min < quorum) {
      throw new IllegalArgumentException("Cannot start a poll with minimum " +
                "size " + min + " because at least " + quorum +
                " participants are required for quorum");
    }
    if (max < min) {
      log.warning("Impossible poll size range [min=" + min + ", max=" + max
                  + "].  Defaulting to min size.");
      return min;
    } else if (max == min) {
      log.debug("Max poll size and min poll size are identical");
      pollSize = min;
    } else {
      // Pick a random number of participants for this poll between
      // minParticipants and maxParticipants
      int randCount = theRandom.nextInt(max - min);
      pollSize = min + randCount;
    }

    return pollSize;
  }

  /**
   * Start a poll.  Overrides BasePoll.startPoll().
   */
  public void startPoll() {
    if (!resumedPoll) {
      // Construct the initial inner circle only once
      constructInnerCircle(pollerState.getPollSize());
      setStatus(V3Poller.POLLER_STATUS_INVITING_PEERS);
    }

    log.debug("Scheduling V3 poll " + pollerState.getPollKey() +
              " to complete by " + pollerState.getPollDeadline());

    Deadline voteCompleteDeadline = null;
    Deadline pollDeadline = null;

    if (resumedPoll) {
      // Bypass sanity check.
      voteCompleteDeadline = Deadline.restoreDeadlineAt(pollerState.getVoteDeadline());
      pollDeadline = Deadline.restoreDeadlineAt(pollerState.getPollDeadline());
    } else {
      // Sanity check
      voteCompleteDeadline = Deadline.at(pollerState.getVoteDeadline());
      pollDeadline = Deadline.at(pollerState.getPollDeadline());
    }

    // Schedule the vote tally callback.  The poll will tally votes no earlier
    // than this deadline.
    if ((voteCompleteDeadline.expired() &&
        pollerState.votedPeerCount() <= pollerState.getQuorum()) ||
        pollDeadline.expired()) {
      log.info("Poll expired before tallying could complete: " +
               pollerState.getPollKey());
      stopPoll(V3Poller.POLLER_STATUS_EXPIRED);
      return;
    } else {
      voteCompleteRequest =
        TimerQueue.schedule(voteCompleteDeadline,
                            new VoteTallyCallback(), this);
    }

    if (reserveScheduleTime(theParticipants.size())) {
      log.debug("Scheduled time for a new poll with a requested poll size of "
                + theParticipants.size());

      // Schedule the poll deadline.  The poll must complete by this time.
      pollCompleteRequest =
        TimerQueue.schedule(pollDeadline,
                            new PollCompleteCallback(), this);

      // Sanity check.  This really *should not* ever happen.
      if (pollerState.getVoteDeadline() >= getDeadline().getExpirationTime()) {
        log.warning("Vote deadline (" + pollerState.getVoteDeadline() + 
                    ") is later than poll deadline (" + getDeadline() +
                    ") in poll " + getKey());
        stopPoll(POLLER_STATUS_EXPIRED);
        return;
      }
    } else {
      log.warning("Unable to schedule time for poll " + getKey());
      stopPoll(POLLER_STATUS_NO_TIME);
      return;
    }

    for (Iterator it = theParticipants.values().iterator(); it.hasNext();) {
      ParticipantUserData ud = (ParticipantUserData)it.next();
      if (!ud.isOuterCircle()) { // Start polling only inner circle members.
        PsmInterp interp = ud.getPsmInterp();
        try {
          if (resumedPoll) {
            interp.resume(ud.getPsmInterpState());
          } else {
            interp.start();
          }
        } catch (PsmException e) {
          log.warning("State machine error", e);
          stopPoll(POLLER_STATUS_ERROR);
        }
      }
    }
    
    if (enableInvitations) {
      // Set up an event on the timer queue to check for accepted peers.
      // If we haven't got enough peers, invite more.
      log.debug("Scheduling check for more peers to invite in " +
                timeBetweenInvitations + "ms.");
      invitationRequest = 
        TimerQueue.schedule(Deadline.in(timeBetweenInvitations),
                            new InvitationCallback(), this);
    } else {
      log.debug("Not scheduling a followup invitation check, " +
                "due to configuration.");
    }
  }

  /**
   * Stop the poll, and set the supplied status.
   */
  public void stopPoll(final int status) {
    if (activePoll) {
      activePoll = false;
    } else {
      log.debug("Poll has already been closed: " + getKey());
      return;
    }

    // Want this action to be serialized with the other poll actions.
    pollManager.runTask(new PollRunner.Task("Stopping Poll", getKey()) {
      public void lockssRun() {
        log.info("Stopping poll " + getKey() + " with status " + 
                 V3Poller.POLLER_STATUS_STRINGS[status]);
        setStatus(status);
        if (task != null && !task.isExpired()) {
          log.debug2("Cancelling task");
          task.cancel();
        }
        if (invitationRequest != null) {
          log.debug2("Cancelling invitation request timer event.");
          TimerQueue.cancel(invitationRequest);
        }
        if (voteCompleteRequest != null) {
          log.debug2("Cancelling vote completion timer event.");
          TimerQueue.cancel(voteCompleteRequest);
        }
        if (pollCompleteRequest != null) {
          log.debug2("Cancelling poll completion timer event.");
          TimerQueue.cancel(pollCompleteRequest);
        }
        // Reset the duration and deadline to reflect reality
        long oldDeadline = pollerState.getPollDeadline();
        long now = TimeBase.nowMs();
        pollerState.setPollDeadline(now);
        pollerState.setDuration(now - pollerState.getCreateTime());
        // Clean up any lingering participants.
        for (Iterator voters = theParticipants.values().iterator(); voters.hasNext(); ) {
          ParticipantUserData ud = (ParticipantUserData)voters.next();
          VoteBlocks vb = ud.getVoteBlocks();
          if (vb != null) {
            vb.release();
          }
        }
        serializer.closePoll();
        pollManager.closeThePoll(pollerState.getPollKey());      
        
        log.debug("Closed poll " + pollerState.getPollKey());

        // Finally, release unneeded resources
        release();
      }
    });
  }

  /**
   * <p>Stop the poll.  Overrides BasePoll.stopPoll().</p>
   *
   */
  public void stopPoll() {
    stopPoll(POLLER_STATUS_COMPLETE);
  }

  /**
   * <p>Examine the nominees from each peer. Choose a fixed number
   * to allow from each peer.  If any peer has sent no nominees,
   * remove it from the poll.</p>
   */
  private void pollOuterCircle() {
    // Calculate the correct number to request from each host.
    int targetSize =
      (int)(pollerState.getOuterCircleTarget() / theParticipants.size());
    log.debug2("Target for nominees from each inner circle participant: " +
               targetSize);
    Collection outerCircle = constructOuterCircle(targetSize);
    // Now start polling the outer circle.
    synchronized(theParticipants) {
      for (Iterator it = outerCircle.iterator(); it.hasNext(); ) {
        String idStr = (String) it.next();
        PeerIdentity id = idManager.findPeerIdentity(idStr);
        if (!theParticipants.keySet().contains(id)) {
          log.debug2("Adding new peer " + id + " to the outer circle");
          ParticipantUserData participant = makeParticipant(id);
          participant.isOuterCircle(true);
          theParticipants.put(id, participant);
          try {
            participant.getPsmInterp().start();
          } catch (PsmException e) {
            log.warning("State machine error, removing peer from poll.", e);
            // Drop this peer from the poll.
            removeParticipant(id);
          }
        } else {
          log.debug("We already have peer " + id + " in our list of voters");
        }
      }
    }
  }

  protected Collection constructOuterCircle(int target) {
    Collection outerCircle = new ArrayList();
    // Attempt to randomly select 'target' peers from each voter's
    // nominee list.  If there are not enough peers, just nominate whoever
    // we can.
    for (Iterator it = theParticipants.values().iterator(); it.hasNext(); ) {
      ParticipantUserData participant = (ParticipantUserData)it.next();

      if (participant.getNominees() == null)
        continue;

      List<String> nominees = new ArrayList<String>();
      
      // Reject any nominees we should not include.
      for (String pidKey : (List<String>)participant.getNominees()) {
        if (shouldIncludePeer(pidKey)) {
          nominees.add(pidKey);
        }
      }

      if (nominees == null || nominees.size() == 0) {
        // If we want to drop peers that don't send any nominees,
        // they will have already been removed at this point,
        // so just log a debug statement and go on.
        log.debug2("Peer " + participant.getVoterId()
                   + " did not nominate anyone we can include.");
        continue;
      } else if (nominees.size() < target) {
        log.warning("Peer " + participant.getVoterId() +
                    " only sent " + nominees.size() + " nominations.");
        outerCircle.addAll(nominees);
        continue;
      } else {
        log.debug3("Randomly selecting " + target + " nominees from " +
                   "the set " + nominees);
        outerCircle.addAll(CollectionUtil.randomSelection(nominees, target));
      }
    }
    return outerCircle;
  }

  /**
   * Add a voter to the inner circle of the poll.
   *
   * @param id
   * @return PsmInterp
   */
  private ParticipantUserData addInnerCircleVoter(PeerIdentity id) {
    ParticipantUserData participant = makeParticipant(id);
    theParticipants.put(id, participant);
    return participant;
  }

  /**
   * Create a new ParticipantUserData state object for the specified peer.
   *
   * @return ParticipantUserData for the specified peer
   */
  private ParticipantUserData makeParticipant(final PeerIdentity id) {
    final ParticipantUserData participant =
      new ParticipantUserData(id, this, stateDir);
    participant.setPollerNonce(makePollerNonce());
    PsmMachine machine =
      PollerStateMachineFactory.getMachine(getPollerActionsClass());
    PsmInterp interp = new PsmInterp(machine, participant);
    interp.setCheckpointer(new PsmInterp.Checkpointer() {
      public void checkpoint(PsmInterpStateBean resumeStateBean) {
        // Replace the statebean in this machine's state object.
        participant.setPsmInterpState(resumeStateBean);
        try {
          checkpointParticipant(participant);
        } catch (PollSerializerException ex) {
          log.error("Unable to save participant state for peer " +
                    participant.getVoterId());
        }
      }
    });
    participant.setPsmInterp(interp);
    return participant;
  }

  /**
   * Called by a participant's state machine when it receives a set of nominees
   * from a voter.
   *
   * @param id The PeerIdentity of the participant sending the nominees.
   * @param nominatedPeers A list of string representations of PeerIdendities
   *          the peer is nominating.
   */
  void nominatePeers(PeerIdentity id, List nominatedPeers) {
    // Only honor nominations if this is an inner circle peer.
    ParticipantUserData peer = (ParticipantUserData)theParticipants.get(id);
    if (peer.isOuterCircle()) {
      log.debug2("Ignoring nominations from outer circle voter " + id);
      return;
    }
    // Peers should never be allowed to nominate themsevles.
    if (nominatedPeers != null) {
      nominatedPeers.remove(id);
    }
    log.debug2("Received nominations from inner circle voter: " + id +
               "; Nominations = " + nominatedPeers);
    // If the peer has sent us no nominations, decide whether to drop him.
    if (dropEmptyNominators &&
        (nominatedPeers == null || nominatedPeers.size() == 0)) {
      log.warning("Peer " + id + " did not nominate anyone.  Removing from " +
                  "poll.");
      removeParticipant(id);
      return;
    }
    // Store the nominees in the participant's userdata
    peer.setNominees(nominatedPeers);
    pollerState.signalVoterNominated(id);
    checkpointPoll();
    if (pollerState.sufficientPeers()) {
      pollOuterCircle();
    }
  }


  /**
   * Tally an individual hash block.
   *
   * @param hb  The {@link HashBlock} to tally.
   */
  void tallyBlock(HashBlock hb, BlockTally tally) {
    setStatus(V3Poller.POLLER_STATUS_TALLYING);

    log.debug3("Opening block " + hb.getUrl() + " to tally.");
    int missingBlockVoters = 0;
    int digestIndex = 0;
    int bytesHashed = 0;
    
    // By this time, only voted peers will exist in 'theParticipants'.
    // Everyone else will have been removed.
    do {
      missingBlockVoters = 0;
      digestIndex = 0;
      
      // Reset the tally
      tally.reset();

      // Iterate over the peers looking for the lowest-sorted URL.
      // lowestUrl will be null if there is no URL lower than the poller's
      
      String lowestUrl = null;
      for (Iterator it = theParticipants.values().iterator(); it.hasNext();) {
        ParticipantUserData voter = (ParticipantUserData)it.next();
        VoteBlocksIterator iter = voter.getVoteBlockIterator();
        
        if (iter == null) {
          log.warning("Voter " + voter + " gave us a null vote block iterator " 
                      + " while tallying block " + hb.getUrl()
                      + " in poll " + getKey() + ".  It is possible that the " 
                      + " poller had no votes to cast, but it could also be "
                      + " a problem.");
          // Next voter.
          continue;
        }

        VoteBlock vb = null;
        try {
          vb = iter.peek();
          if (vb == null) {
            continue;
          } else {
            if (hb.getUrl().compareTo(vb.getUrl()) > 0 &&
                (lowestUrl == null || hb.getUrl().compareTo(lowestUrl) > 0)) { 
              lowestUrl = vb.getUrl();
            }
          }
        } catch (IOException ex) {
          continue;
        }
      }

      for (Iterator it = theParticipants.values().iterator(); it.hasNext();) {
        ParticipantUserData voter = (ParticipantUserData)it.next();
        VoteBlocksIterator iter = voter.getVoteBlockIterator();

        if (iter == null) {
          log.warning("Voter " + voter + " gave us a null vote block iterator " 
                      + " while tallying block " + hb.getUrl()
                      + " in poll " + getKey() + ".  It is possible that the " 
                      + " poller had no votes to cast, but it could also be "
                      + " a problem.");
          // Next voter.
          continue;
        }

        VoteBlock vb = null;
        try {
          vb = iter.peek();
          if (vb == null) {
            // No block was returned.  This means this voter is out of
            // blocks.
            tally.addExtraBlockVoter(voter.getVoterId());
          } else {
            int sortOrder = hb.getUrl().compareTo(vb.getUrl());
            if (sortOrder > 0) {
              log.debug3("Participant " + voter.getVoterId() + 
                         " seems to have an extra block that I don't: " +
                         vb.getUrl());
              tally.addMissingBlockVoter(voter.getVoterId(), vb.getUrl());
              missingBlockVoters++;
              iter.next();
            } else if (sortOrder < 0) {
              log.debug3("Participant " + voter.getVoterId() +
                         " doesn't seem to have block " + hb.getUrl());
              tally.addExtraBlockVoter(voter.getVoterId());
            } else { // equal
              if (lowestUrl == null) {
                log.debug3("Our blocks are the same, now we'll compare them.");
                iter.next();
                compareBlocks(voter.getVoterId(), ++digestIndex, vb, hb, tally);
              } else {
                log.debug3("Not incrementing peer's vote block iterator");
              }
            }
          }
        } catch (IOException ex) {
          // On IOExceptions, attempt to move the poll forward. Just skip
          // this block, but be sure to log the error.
          log.error("IOException while iterating over vote blocks.", ex);
          if (++blockErrorCount > maxBlockErrorCount) {
            pollerState.setErrorDetail("Too many errors during tally");
            stopPoll(V3Poller.POLLER_STATUS_ERROR);
          }
        } finally {
          voter.incrementTalliedBlocks();
        }
      }

      tally.tallyVotes();
      checkTally(tally, hb.getUrl(), false);
    } while (missingBlockVoters > 0);
    
    // Check to see if it's time to checkpoint the poll.
    bytesHashedSinceLastCheckpoint += hb.getTotalFilteredBytes();
    if (bytesHashedSinceLastCheckpoint >= this.hashBytesBeforeCheckpoint) {
      checkpointPoll();
    }
  }

  /**
   * Called by the BlockHasher's hashComplete callback at the end of the tally.
   * This handles the edge case where we have finished our hash, and have no
   * more blocks to tally, but peers may still have blocks that have not been
   * checked.
   */
  private void finishTally() {
    int digestIndex = 0;
    int missingBlockVoters = 0;
    do {
      digestIndex = 0;
      missingBlockVoters = 0;
      BlockTally tally = new BlockTally(pollerState.getQuorum());
      for (Iterator it = theParticipants.values().iterator(); it.hasNext();) {
        ParticipantUserData voter = (ParticipantUserData)it.next();
        VoteBlocksIterator iter = voter.getVoteBlockIterator();
        
        if (iter == null) {
          log.warning("Voter " + voter + " gave us a null vote block iterator " 
                      + " while finishing the tally "
                      + " in poll " + getKey() + ".  It is possible that the " 
                      + " poller had no votes to cast, but it could also be "
                      + " a problem.");
          // Next voter.
          continue;
        }

        try {
          if (iter.peek() != null) {
            VoteBlock vb = iter.next();
            tally.addMissingBlockVoter(voter.getVoterId(), vb.getUrl());
            missingBlockVoters++;
          }
        } catch (IOException ex) {
          // This would be bad enough to stop the poll and raise an alert.
          log.error("IOException while iterating over vote blocks.", ex);
          stopPoll(V3Poller.POLLER_STATUS_ERROR);
          return;
        }
      }

      // Do not tally if this is the last time through the loop.
      if (missingBlockVoters > 0) {
        tally.tallyVotes();
        // This may be null if the tally does not yet have consensus on what
        // the name of the missing block is.
        if (tally.getMissingBlockUrl() != null) {
          checkTally(tally, tally.getMissingBlockUrl(), false);
        }
      }

    } while (missingBlockVoters > 0);
    
    // Checkpoint the poll.
    checkpointPoll();

    // Do any pending repairs.
    doRepairs();
  }

  /**
   * <p>Check the tally for a block, and perform repairs if necessary.</p>
   *
   * @param tally The tally to check.
   * @param url The target URL for any possible repairs.
   * @param markComplete  If true, mark this as a completed repair in the
   *                      status table.
   */
  private void checkTally(BlockTally tally,
                          String url,
                          boolean markComplete) {
    // Should never happen -- if it does, track it down.
    if (url == null) {
      throw new NullPointerException("Passed a null url to checkTally!");
    }
    
    setStatus(V3Poller.POLLER_STATUS_TALLYING);
    int result = tally.getTallyResult();
    PollerStateBean.TallyStatus tallyStatus = pollerState.getTallyStatus();
    
    // If there are any agreeing peers, update our agreement
    // history for them.
    
    if (tally.getAgreeVoters().size() > 0) {
      try {
        RepositoryNode node = AuUtil.getRepositoryNode(getAu(), url);
        if (node != null)
          node.signalAgreement(tally.getAgreeVoters());
      } catch (MalformedURLException ex) {
        log.error("Malformed URL while updating agreement history: " 
                  + url);
      }
    }

    // Linked hash map - order is significant
    String pollKey = pollerState.getPollKey();
    switch(result) {
    case BlockTally.RESULT_WON:
      tallyStatus.addAgreedUrl(url);
      // Great, we won!  Do nothing.
      log.debug3("Won tally for block: " + url + " in poll " + pollKey);
      break;
    case BlockTally.RESULT_LOST:
      tallyStatus.addDisagreedUrl(url);
      log.info("Lost tally for block " + url + " in poll " + getKey());
      requestRepair(url, tally.getDisagreeVoters());
      break;
    case BlockTally.RESULT_LOST_EXTRA_BLOCK:
      log.info("Lost tally. Removing extra block " + url +
               " in poll " + getKey());
      deleteBlock(url);
      break;
    case BlockTally.RESULT_LOST_MISSING_BLOCK:
      log.info("Lost tally. Requesting repair for missing block: " +
               url + " in poll " + getKey());
      tallyStatus.addDisagreedUrl(url);
      String missingURL = tally.getMissingBlockUrl();
      requestRepair(missingURL,
                    tally.getMissingBlockVoters(missingURL));
      break;
    case BlockTally.RESULT_NOQUORUM:
      tallyStatus.addNoQuorumUrl(url);
      log.warning("No Quorum for block " + url + " in poll " + getKey());
      break;
    case BlockTally.RESULT_TOO_CLOSE:
    case BlockTally.RESULT_TOO_CLOSE_MISSING_BLOCK:
    case BlockTally.RESULT_TOO_CLOSE_EXTRA_BLOCK:
      tallyStatus.addTooCloseUrl(url);
      log.warning("Tally was inconclusive for block " + url + " in poll " +
                  getKey());
      break;
    default:
      log.warning("Unexpected results from tallying block " + url + ": "
                  + tally.getStatusString());
    }

    // Mark this repair complete if this is a re-check after a repair
    if (markComplete) pollerState.getRepairQueue().markComplete(url);
  }

  /**
   * <p>Callback method called by each PollerStateMachine when entering the
   * TallyVoter state.</p>
   */
  boolean tallyVoter(PeerIdentity id) {
    pollerState.addVotedPeer(id);
    checkpointPoll();
    return true;
  }

  /**
   * Schedule hashing of the AU.
   *
   * @return true iff the hash is scheduled successfully.
   */
  protected boolean scheduleHash(CachedUrlSet cu, Deadline deadline,
                                 HashService.Callback cb,
                                 BlockHasher.EventHandler eh) {
    log.debug("Scheduling our hash for poll " + pollerState.getPollKey());
    BlockHasher hasher = new BlockHasher(cu,
                                         initHasherDigests(),
                                         initHasherByteArrays(),
                                         eh);
    // Now schedule the hash
    HashService hashService = theDaemon.getHashService();
    
    boolean canHash = hashService.scheduleHash(hasher, deadline,
                                               cb, null);

    if (canHash) {
      setStatus(POLLER_STATUS_HASHING);
      return true;
    } else {
      return false;
    }
  }

  /**
   * Create an array of byte arrays containing hasher initializer bytes, one for
   * each participant in the poll. The initializer bytes are constructed by
   * concatenating the participant's poller nonce and the voter nonce.
   *
   * @return Block hasher initialization bytes.
   */
  private byte[][] initHasherByteArrays() {
    int len = theParticipants.size();
    byte[][] initBytes = new byte[len + 1][]; // One for plain hash
    initBytes[0] = new byte[0];
    int ix = 1;
    for (Iterator it = theParticipants.values().iterator(); it.hasNext();) {
      ParticipantUserData ud = (ParticipantUserData)it.next();
      log.debug2("Initting hasher byte arrays for voter " + ud.getVoterId());
      initBytes[ix] = ByteArray.concat(ud.getPollerNonce(),
                                       ud.getVoterNonce());
      ix++;
    }
    return initBytes;
  }

  /**
   * Create an array of message digests, one for each participant in the poll.
   * This array is guaranteed to be in the same order as the inner circle
   * iterator and the array of byte arrays returned by
   * {@link #initHasherByteArrays()}
   *
   * @return An array of MessageDigest objects to be used by the BlockHasher.
   */
  private MessageDigest[] initHasherDigests() {
    int len = theParticipants.size() + 1; // One for plain hash.
    MessageDigest[] digests = new MessageDigest[len];
    for (int ix = 0; ix < len; ix++) {
      try {
        digests[ix] = MessageDigest.getInstance(pollerState.getHashAlgorithm());
      } catch (NoSuchAlgorithmException ex) {
        // This will have been caught at construction time, and so should
        // never happen here.
        log.critical("Unexpected NoSuchAlgorithmException in " +
                     "initHasherDigests");
        stopPoll(POLLER_STATUS_ERROR);
      }
    }
    return digests;
  }

  /**
   * Request a repair for the specified URL.  This method appends the URL
   * and its list of disagreeing voters to a queue, which is examined at the
   * end of tallying.
   * 
   * @param url
   * @param disagreeingVoters Set of disagreeing voters.
   */
  private void requestRepair(final String url,
                             final Collection disagreeingVoters) {

    PollerStateBean.RepairQueue repairQueue = pollerState.getRepairQueue();
    
    // If we already have more than maxRepairs and maxRepairs is >= 0, 
    // just return.  A value less than 0 means "unlimited repairs".
    int len = repairQueue.size();
    if (len >= 0 &&  len > maxRepairs) {
      return;
    }
    
    // If not, choose where to request the repair.
    log.debug2("Deciding whether to repair from cache or publisher.  Repair " +
               "from cache probability=" + repairFromCache);
    if (ProbabilisticChoice.choose(repairFromCache) ||
        (enableRepairFromCache && AuUtil.isPubDown(getAu()))) {
      PeerIdentity peer = findPeerForRepair(disagreeingVoters);
      log.debug2("Requesting repair for target: " + url + " from " + peer);
      repairQueue.repairFromPeer(url, peer);
    } else {
      log.debug2("Requesting repair for target: " + url + " from publisher");
      repairQueue.repairFromPublisher(url);
    }
  }

  /* Select a peer to attempt repair from. */
  PeerIdentity findPeerForRepair(Collection disagreeingVoters) {
    return (PeerIdentity)CollectionUtil.randomSelection(disagreeingVoters);
  }

  /**
   * Request a repair from the specified peer for the specified URL.  Called
   * from doRepairs().
   */
  private void requestRepairFromPeer(String url, PeerIdentity peer) {
    log.debug2("Requesting repair for URL " + url + " from "
               + peer);
    ParticipantUserData ud =
      (ParticipantUserData)theParticipants.get(peer);
    V3LcapMessage msg = ud.makeMessage(V3LcapMessage.MSG_REPAIR_REQ);
    msg.setTargetUrl(url);
    msg.setEffortProof(null);
    try {
      sendMessageTo(msg, peer);
    } catch (IOException ex) {
      log.error("IOException trying to send repair request", ex);
      // XXX: Alerts, retry
    }
  }

  /**
   * Delete (deactivate) the block in our repository.
   * @param url The block to be deleted.
   */
  private void deleteBlock(String url) {
    try {
      if (deleteExtraFiles) {
        CachedUrlSetSpec cuss =
          new SingleNodeCachedUrlSetSpec(url);
        CachedUrlSet cus = getAu().makeCachedUrlSet(cuss);
        NodeManager nm = theDaemon.getNodeManager(getAu());
        log.debug("Marking block deleted: " + url);
        nm.deleteNode(cus);
      } else {
        log.info("Asked to mark file " + url + " deleted in poll " +
                 pollerState.getPollKey() + ".  Not actually deleting.");
      }
      pollerState.getRepairQueue().markComplete(url);
    } catch (IOException ex) {
      log.warning("Unable to delete node " + url + " in poll " + getKey(), ex);
    }
  }
  
  /**
   * Called at the end of tallying if there are any pending repairs.
   */
  private void doRepairs() {
    PollerStateBean.RepairQueue queue = pollerState.getRepairQueue();

    List pendingPeerRepairs = queue.getPendingPeerRepairs();
    List pendingPublisherRepairs = queue.getPendingPublisherRepairUrls();
    
    boolean needPublisherRepairs = pendingPublisherRepairs.size() > 0;
    boolean needPeerRepairs = pendingPeerRepairs.size() > 0;
    boolean haveActiveRepairs = queue.getActiveRepairs().size() > 0;
    
    if (needPublisherRepairs || needPeerRepairs) {
      setStatus(V3Poller.POLLER_STATUS_WAITING_REPAIRS);
      if (log.isDebug()) {
        log.debug("Pending Peer Repairs: " + pendingPeerRepairs.size());
        log.debug("Pending Publisher Repairs: " + pendingPublisherRepairs.size());
        log.debug("Active Repairs: " + queue.getActiveRepairs().size());
      }
    } else {
      // It's OK to shortcut and end the poll here, there's nothing left to do!
      voteComplete();
      return;
    }

    // If we have decided to repair from any peers, pass the set of URLs
    // we want to repair to the RepairCrawler.  A callback handles checking
    // each successfully fetched repair.
    if (needPublisherRepairs) {
      log.debug("Starting publisher repair crawl for " + 
                pendingPublisherRepairs.size() + " urls.");
      CrawlManager cm = theDaemon.getCrawlManager();
      CrawlManager.Callback cb = new CrawlManager.Callback() {
        public void signalCrawlAttemptCompleted(boolean success,
                                                Object cookie,
                                                CrawlerStatus status) {
          if (success) {
            // Check the repairs.
            // XXX: It would be nice to be able to re-hash the repaired
            // URLs as a single set, but we don't have a notion of
            // a disjoint CachedUrlSetSpec that represents a collection of
            // unrelated nodes.
            Collection urlsFetched = status.getUrlsFetched();
            for (Iterator iter = urlsFetched.iterator(); iter.hasNext(); ) {
              receivedRepair((String)iter.next());
            }
          }
        }
      };

      cm.startRepair(getAu(), pendingPublisherRepairs,
                     cb, null, null);
      queue.markActive(pendingPublisherRepairs);
    }
    
    // If we have decided to repair from any caches, iterate over the list
    // of PollerStateBean.Repair objects and request each one.
    if (needPeerRepairs) {
      log.debug("Requesting repairs from peers for " +
                pendingPeerRepairs.size() + " urls.");

      for (Iterator iter = pendingPeerRepairs.iterator(); iter.hasNext(); ) {
        PollerStateBean.Repair r = (PollerStateBean.Repair)iter.next();
        requestRepairFromPeer(r.getUrl(), r.getRepairFrom());
        queue.markActive(r.getUrl());
      }

    }
  }

  /**
   * Callback used to schedule a small re-check hash when a repair has 
   * been received.
   */
  public void receivedRepair(final String url) {
    // It is possible that a repair may come in after the poll has been closed
    // and its resources released.  If pollManager is null, just return.
    if (pollManager == null) {
      log.debug("Repair was received after the poll was closed. " +
                "Poll key = " + getKey());
      return;
    }
    
    final BlockHasher.EventHandler blockDone =
      new BlockHasher.EventHandler() {
        public void blockDone(final HashBlock hblock) {
          // XXX: Handle errors here
          pollManager.runTask(new PollRunner.Task("Received Repair Block Complete", getKey()) {
            public void lockssRun() {
              PollerStateBean.RepairQueue rq = pollerState.getRepairQueue();
              log.debug3("Finished hashing repair sent for block " + hblock);
              // Replay the block comparison using the new hash results.
              int digestIndex = 0;
              BlockTally tally = new BlockTally(pollerState.getQuorum());
              for (Iterator iter = theParticipants.keySet().iterator();
                   iter.hasNext(); ) {
                digestIndex++;
                PeerIdentity id = (PeerIdentity)iter.next();
                VoteBlock vb = 
                  ((ParticipantUserData)theParticipants.get(id)).getVoteBlocks().getVoteBlock(url);
                if (vb == null) {
                  log.debug("Voter " + id + " does not seem to have url " + url + " in his vote blocks.");
                  // The voter did not have this URL in the first place.
                  tally.addMissingBlockVoter(id, url);
                } else {
                  compareBlocks(id, digestIndex, vb, hblock, tally);
                }
              }
              tally.tallyVotes();
              log.debug3("After-vote hash tally for repaired block " + url
                         + ": " + tally.getStatusString());
              setStatus(V3Poller.POLLER_STATUS_TALLYING);
              checkTally(tally, url, true);
            }
            
            public String toString() {
              return "[After-repair tally of " + url + " in poll " 
                     + getKey() + "]";
            }
          });
        }
    };

    final HashService.Callback hashDone =
      new HashService.Callback() {
        public void hashingFinished(CachedUrlSet urlset, Object cookie,
                                    CachedUrlSetHasher hasher, Exception e) {
          pollManager.runTask(new PollRunner.Task("Received Repair Hash Complete", getKey()) {
            public void lockssRun() {
              // If there are no more repairs outstanding, go ahead and
              // stop the poll at this point.
              PollerStateBean.RepairQueue queue = pollerState.getRepairQueue();
              int pending = queue.getPendingRepairs().size();
              int active = queue.getActiveRepairs().size();
              log.debug2("Repair hash on one block done.  Pending repairs="
                         + pending + "; Active repairs=" + active);
              if (pending == 0 && active == 0) {
                voteComplete();
              }
            }
            
            public String toString() {
              return "[Repairs complete check for poll " + getKey() + "]";
            }
          });
        }
    };

    
    // Serialize these
    pollManager.runTask(new PollRunner.Task("Scheduling Repair Hash", getKey()) {
      public void lockssRun() {
        CachedUrlSet blockCus =
          getAu().makeCachedUrlSet(new SingleNodeCachedUrlSetSpec(url));
        boolean hashing = scheduleHash(blockCus,
                                       Deadline.at(pollerState.getPollDeadline()),
                                       hashDone,
                                       blockDone);
        if (!hashing) {
          log.warning("Failed to schedule a repair check hash for block " + url);
        }
      }
      
      public String toString() {
        return "[Scheduling repair hash of " + url + " in poll "
                + getKey() + "]";
      }
    });
    
  }
  
  /**
   * Compare a hash block and a vote block.
   */
  void compareBlocks(PeerIdentity id,
                     int hashIndex,
                     VoteBlock voteBlock,
                     HashBlock hashBlock,
                     BlockTally tally) {

    /*
     * Implementation Note:
     *
     * The combinatorics of this can get hairy fast. The maximum number of
     * versions in the voteblock and hashblock is controlled by the parameter
     * BlockHasher.PARAM_HASH_MAX_VERSIONS, and should be set to a reasonably
     * small number for real world use.  The default is currently 5.
     */

    VoteBlock.Version[] vbVersions = voteBlock.getVersions();
    HashBlock.Version[] hbVersions = hashBlock.getVersions();

    log.debug3("Comparing block " + hashBlock.getUrl() + " against peer " +
               id + " in poll " + getKey());

    int disagreementCount = 0;

    for (int hbIdx = 0; hbIdx < hbVersions.length;  hbIdx++ ) {
      byte[] hasherResults = hbVersions[hbIdx].getHashes()[hashIndex];
      Throwable pollerHashError = hbVersions[hbIdx].getHashError();

      // If we had a hash error on this version, we need to count this
      // as an abstenion by not tallying it.
      if (pollerHashError != null) {
        this.pollerState.getTallyStatus().addErrorUrl(hashBlock.getUrl(),
                                                      pollerHashError);
        continue;
      }
      
      for (int vbIdx = 0; vbIdx < vbVersions.length; vbIdx++) {
        // If there were any hashing errors, we need to count this
        // as an abstension by not adding it to the tally.  If we don't
        // have enough voters, this will be a no quorum block.
        if (vbVersions[vbIdx].getHashError()) {
          log.info("Voter version " + vbIdx + " had a hashing error. "
                   + "Counting as an abstension.");
          continue;
        }
        byte[] voterResults = vbVersions[vbIdx].getHash();
        if (log.isDebug3()) {
          log.debug3("Comparing voter's version " + vbIdx +
                     " against poller's version " + hbIdx); 
          log.debug3("Hasher results: "
                     + ByteArray.toBase64(hasherResults));
          log.debug3("Voter results: "
                     + ByteArray.toBase64(voterResults));
        }

        if (Arrays.equals(voterResults, hasherResults)) {
          log.debug3("Found agreement between voter version " + vbIdx +
                     " and hash block version " + hbIdx);
          tally.addAgreeVoter(id);
          ParticipantUserData ud = (ParticipantUserData)theParticipants.get(id);
          if (ud != null) ud.incrementAgreedBlocks();
          return;
        } else {
          disagreementCount++;
        }
      }
    }

    if (disagreementCount == (vbVersions.length * hbVersions.length)) {
      log.debug3("No agreement found for any version of block " +
                 voteBlock.getUrl() + ".  Lost tally, adding voter " + id +
                 " to the disagreeing voter list.");
      tally.addDisagreeVoter(id);
    }
  }


  // The vote is over.
  private void voteComplete() {
    if (idManager == null) {
      log.warning("voteComplete() called on a possibly closed poll: "
                  + getKey());
      return;
    }
    for (Iterator iter = theParticipants.values().iterator(); iter.hasNext();) {
      ParticipantUserData ud = (ParticipantUserData)iter.next();
      PsmInterp interp = ud.getPsmInterp();
      try {
        interp.handleEvent(V3Events.evtVoteComplete);
      } catch (PsmException e) {
        log.warning("State machine error", e);
        stopPoll(POLLER_STATUS_ERROR);
      }
      if (log.isDebug2()) {
        log.debug2("Gave peer " + ud.getVoterId()
                   + " the Vote Complete event.");
      }

      // Update the participant's agreement history.
      this.idManager.signalPartialAgreement(ud.getVoterId(), getAu(), 
                                            ud.getPercentAgreement());

    }
    // Tell the PollManager to hang on to our statistics for this poll.
    PollManager.V3PollStatusAccessor stats = pollManager.getV3Status();
    String auId = getAu().getAuId();
    stats.setAgreement(auId, getPercentAgreement());
    stats.setLastPollTime(auId, TimeBase.nowMs());
    stats.incrementNumPolls(auId);
    // Update the last poll time on the AU.
    AuState auState = theDaemon.getNodeManager(getAu()).getAuState();
    auState.newPollFinished();
    auState.setV3Agreement(getPercentAgreement());
    stopPoll(POLLER_STATUS_COMPLETE);
  }

  /**
   * Return the percent agreement for this poll.  Used by the ArchivalUnitStatus
   * accessor, and the V3PollStatus accessor
   */
  public float getPercentAgreement() {
    float agreeingUrls = (float)getAgreedUrls().size();
    float talliedUrls = (float)getTalliedUrls().size();
    float agreement;
    if (agreeingUrls > 0)
      agreement = agreeingUrls / talliedUrls;
    else
      agreement = 0.0f;
    return agreement;
  }

  /**
   * Called by participant state machines if an error occurs.
   *
   * @param id
   * @param errorMsg
   */
  void handleError(PeerIdentity id, String errorMsg) {
    log.error("Peer " + id + " experienced an error. Error =" +
              (errorMsg == null ? "Unknown error" : errorMsg));

    // Drop the voter from the poll.
    removeParticipant(id);
  }

  /**
   * Drop a voter from the poll.
   *
   * @param id
   */
  void removeParticipant(PeerIdentity id) {
    log.debug("Removing voter " + id + " from poll " +
              pollerState.getPollKey());
    try {
      synchronized(theParticipants) {
        ParticipantUserData ud = (ParticipantUserData)theParticipants.get(id);
        // Release used resources.
        VoteBlocks vb = ud.getVoteBlocks();
        if (vb != null) {
          vb.release();
        }
        serializer.removePollerUserData(id);
        theParticipants.remove(id);
        checkpointPoll();
      }
    } catch (Exception ex) {
      log.error("Unable to remove voter from poll!", ex);
      stopPoll(V3Poller.POLLER_STATUS_ERROR);
      return;
    }
  }

  /**
   * Checkpoint the per-poll state.
   */
  private void checkpointPoll() {
    if (serializer != null) {
      try {
        serializer.savePollerState(pollerState);
        bytesHashedSinceLastCheckpoint = 0;
      } catch (PollSerializerException ex) {
        log.warning("Unable to save poller state", ex);
      }
    }
  }

  /**
   * Checkpoint the per-participant state.
   * @throws PollSerializerException
   */
  private void checkpointParticipant(ParticipantUserData ud)
      throws PollSerializerException {
    // Should never happen.
    if (ud == null) {
      throw new NullPointerException("checkpointParticipant was passed a " +
      		"null ParticipantUserData reference.");
    }
    // May happen if a deferred action happens after this poll has been
    // cleaned up.
    if (serializer != null) {
      serializer.savePollerUserData(ud);
    }
  }

  /**
   * Used by the participant state machines to send messages to the appropriate
   * voter.
   *
   * @param msg
   * @param to
   * @throws IOException
   */
  void sendMessageTo(V3LcapMessage msg, PeerIdentity to)
      throws IOException {
    pollManager.sendMessageTo(msg, to);
  }

  /**
   * Handle an incoming V3LcapMessage.
   *
   */
  public void receiveMessage(LcapMessage message) {
    V3LcapMessage msg = (V3LcapMessage)message;
    PeerIdentity sender = msg.getOriginatorId();
    ParticipantUserData ud = (ParticipantUserData)theParticipants.get(sender);
    if (ud != null) {
      PsmInterp interp = ud.getPsmInterp();
      PsmMsgEvent evt = V3Events.fromMessage(msg);
      try {
        interp.handleEvent(evt);
      } catch (PsmException e) {
        log.warning("State machine exception handling message of type "
                    + msg.getOpcodeString() + " from peer "
                    + msg.getOriginatorId() + " in poll "
                    + getKey(), e);
      }
    } else {
      log.error("No voter user data for peer.  May have " +
                "been removed from poll: " + msg.getOriginatorId());
    }
    // Finally, clean up after the V3LcapMessage
    msg.delete();
  }

  public PollerStateBean getPollerStateBean() {
    return pollerState;
  }

  /**
   * @return The set of peers available for inviting into the poll.  The set
   * returned is either the full set of all known peers, or  
   */
  Collection getReferenceList() {
    Collection initialPeers = new ArrayList();

    if (idManager == null) {
      log.warning("getReferenceList possibly called after poll closed!");
      return initialPeers;
    }
    
    if (enableDiscovery) {
      Collection allTCPPeers = idManager.getTcpPeerIdentities();
      for (Iterator iter = allTCPPeers.iterator(); iter.hasNext(); ) {
        PeerIdentity id = (PeerIdentity)iter.next();
        if (shouldIncludePeer(id)) {
          initialPeers.add(id);
        }
      }
    } else {
      Collection keys =
        CurrentConfig.getList(IdentityManagerImpl.PARAM_INITIAL_PEERS,
                              IdentityManagerImpl.DEFAULT_INITIAL_PEERS);
      for (Iterator iter = keys.iterator(); iter.hasNext(); ) {
        String key = (String)iter.next();
        PeerIdentity id = (PeerIdentity)idManager.findPeerIdentity(key);
        if (shouldIncludePeer(id)) {
          initialPeers.add(id);
        }
      }
    }
    return initialPeers;
  }
  
  private boolean shouldIncludePeer(String pidKey) {
    return shouldIncludePeer(idManager.findPeerIdentity(pidKey));
  }
  
  boolean shouldIncludePeer(PeerIdentity pid) {
    // Never include a local id.
    if (pid.isLocalIdentity()) { return false; }
    PeerIdentityStatus status = idManager.getPeerIdentityStatus(pid);
    if (status != null) {
      
      // Never include a peer that has rejected one of our polls in
      // the past because of a group mismatch.
      if (status.getLastPollNak() != null &&
          status.getLastPollNak() == PollNak.NAK_GROUP_MISMATCH) {
        return false;
      }
      
      // Finally, make a probabilistic choice weighted by the last time that
      // we heard from this peer.
      return ProbabilisticChoice.choose(inviteProb(status.getLastPollInvitationTime(),
                                                   status.getLastMessageTime()));
    }
 
    // If we get this far, return true.
    return true;
  }
  
  /**
   * Given a peer identity, compute a probability (a float between 0.0 and
   * 1.0) that the peer should be considered for invitation into the
   * poll.
   *  
   * @param lastPollInvitationTime  The last time we attempted to invite
   *      the target peer into a poll.
   * @param lastMessageTime  The last time that we received any kind of
   *      message from the target peer.
   * @return A number between 0.0 and 1.0 representing the probability
   *      that we want to try to invite this peer into a poll.
   */
  double inviteProb(long lastPollInvitationTime,
                    long lastMessageTime) {
    // If we have never tried to contact this peer, we definitely want to try
    // them out.
    if (lastPollInvitationTime <= 0) return 1.0;

    long delta = lastPollInvitationTime - lastMessageTime;
    
    if (delta <= 1000L*60L*60L*24L*5) {           // < 5 days, or negative
      return 1.0;
    } else if (delta <= 1000L*60L*60L*24L*15) {   // < 15 days
      return 5.0/6.0;
    } else if (delta <= 1000L*60L*60L*24L*30) {   // < 30 days
      return 4.0/6.0;
    } else if (delta <= 1000L*60L*60L*24L*60) {   // < 60 days
      return 3.0/6.0;
    } else if (delta <= 1000L*60L*60L*24L*90) {   // < 90 days
      return 2.0/6.0;
    } else {                                      // > 90 days
      return 1.0/6.0;
    }
  }

  Class getPollerActionsClass() {
    return PollerActions.class;
  }
  
  /**
   * Check to see whether enough pollers have agreed to participate
   * with us.  If not, invite more, and schedule another check.
   */
  private class InvitationCallback implements TimerQueue.Callback {
    public void timerExpired(Object cookie) {
      // Get a list of peers who have decided to participate.
      int participatingPeers = 0;
      for (Iterator it = theParticipants.values().iterator(); it.hasNext(); ) {
        ParticipantUserData peer = (ParticipantUserData)it.next();
        if (peer.isParticipating()) {
          participatingPeers++;
        }
      }

      if (participatingPeers >= getQuorum()) {
        log.debug("[InvitationCallback] Enough peers are participating for " +
                  "quorum.  Count=" + participatingPeers);
      } else {
        // Invite 'quorum' new peers to participate.
        Collection newPeers =
          choosePeers(getReferenceList(), theParticipants.keySet(),
                      invitationSize);

        log.debug("[InvitationCallback] Inviting " + newPeers.size()
                  + " new peers to participate.");

        for (Iterator it = newPeers.iterator(); it.hasNext(); ) {
          PeerIdentity id = (PeerIdentity)it.next();
          ParticipantUserData participant = makeParticipant(id);
          theParticipants.put(id, participant);
          // Start the participant running to send in the invitation.
          try {
            participant.getPsmInterp().start();
          } catch (Exception ex) {
            log.warning("Unable to add participant " + id + " to poll "
                        + getKey(), ex);
          }
        }

        // Schedule another check
        invitationRequest =
          TimerQueue.schedule(Deadline.in(timeBetweenInvitations),
                              new InvitationCallback(),
                              cookie);
      }
    }

    public String toString() {
      return "Poll Invitation Callback [" + getKey() + "]";
    }
  }

  /**
   * This callback is called when the vote tally deadline has expired.
   * If enough peers have cast votes, the tally can go forward, and the
   * callback schedules a hash.
   *
   */
  private class VoteTallyCallback implements TimerQueue.Callback {
    public void timerExpired(Object cookie) {
      
      // Ensure that the invitation timer is cancelled.
      if ( invitationRequest != null) {
        TimerQueue.cancel(invitationRequest);
        invitationRequest = null;
      }

      // Prune "theParticipants", and remove any who have not cast a vote.
      // Iterate over a COPY of the participants, to avoid concurrent
      // modifications.
      Collection peerListCopy = new ArrayList(theParticipants.keySet());
      for (Iterator it = peerListCopy.iterator(); it.hasNext(); ) {
        PeerIdentity id = (PeerIdentity)it.next();
        if (!pollerState.hasPeerVoted(id)) {
          removeParticipant(id);
        }
      }

      // Determine whether enough peers have voted to reach a quorum.
      // If not, kill this poll.
      if (theParticipants.size() < getQuorum()) {
        log.warning("Not enough participants voted to achieve quorum " +
                    "in poll " + getKey() + ": " + getQuorum() + " needed, only " +
                    theParticipants.size() + " voted. Stopping poll.");
        stopPoll(V3Poller.POLLER_STATUS_NO_QUORUM);
        return;
      } else {
        log.debug2("Vote Tally deadline reached.  Scheduling hash.");
        // XXX: Refactor when our hash can be associated with an
        //      existing step task.
        if (task != null) task.cancel();
        if (!scheduleHash(pollerState.getCachedUrlSet(),
                          Deadline.at(pollerState.getPollDeadline()),
                          new HashingCompleteCallback(),
                          new BlockEventHandler())) {
          log.error("No time available to schedule our hash for poll "
                    + pollerState.getPollKey());
          stopPoll(POLLER_STATUS_NO_TIME);
        }
        pollerState.hashStarted(true);
        checkpointPoll();
      }
    }
    public String toString() {
      return "V3 Poll Tally";
    }
  }

  /**
   * Callback called by the poll timer to signal that the poll should end.
   *
   */
  private class PollCompleteCallback implements TimerQueue.Callback {
    /**
     * Called when the poll timer expires.
     *
     * @param cookie data supplied by caller to schedule()
     */
    public void timerExpired(Object cookie) {
      log.debug("Poll time has expired.");
      if (pollerState.expectingRepairs() && extraPollTime > 0) {
        log.debug("Still expecting some repairs.  Holding the poll open for " +
                 "another " + extraPollTime + " ms.");
        
        // Compute the new deadline, and replace the existing deadline.
        Deadline newDeadline =
          Deadline.at(pollerState.getPollDeadline() + extraPollTime);
        TimerQueue.schedule(newDeadline, new ExtraTimeCallback(), null);
        pollerState.setPollDeadline(newDeadline.getExpirationTime());
        return;
      } else {
        log.debug("No expected repairs remain.  Ready to end the poll.");
      }
      voteComplete();
    }

    public String toString() {
      return "V3 Poller " + getKey();
    }
  }
  
  /** Callback used if extra poll time is requested at the initial poll 
   * deadline. */
  private class ExtraTimeCallback implements TimerQueue.Callback {
    public void timerExpired(Object cookie) {
      log.debug("Extra time for the poll has expired.  Ending the poll " +
                "whether we expect repairs or not.");
      if (log.isDebug() && pollerState.expectingRepairs()) {
        log.warning("Ending the poll while repairs are still expected.");
      }
      voteComplete();
    }
    
    public String toString() {
      return "V3 Poller " + getKey();
    }
  }

  /**
   * Callback called after all blocks have been hashed during our tally.
   */
  private class HashingCompleteCallback implements HashService.Callback {
    /**
     * Called when the timer expires or hashing is complete.
     *
     * @param cookie data supplied by caller to schedule()
     */
    public void hashingFinished(CachedUrlSet cus, Object cookie,
                                CachedUrlSetHasher hasher, Exception e) {
      if (e != null) {
        log.warning("Poll hash failed", e);
        stopPoll(POLLER_STATUS_ERROR);
      } else {
        finishTally();
      }
    }
  }

  /**
   * Callback called after each block has been hashed during our tally.
   */
  private class BlockEventHandler implements BlockHasher.EventHandler {
    public void blockDone(HashBlock block) {
      if (pollerState != null) {
        tallyBlock(block, new BlockTally(pollerState.getQuorum()));
      }
    }
  }
  
  public LockssApp getLockssDaemon() {
    return theDaemon;
  }

  /*
   * BasePoll implementations.
   */

  /**
   * Set the message that created this poll.  V3 does not provide an
   * implementation.
   */
  public void setMessage(LcapMessage msg) {
    // Not implemented.
  }

  /**
   * Get the poll creation time.
   */
  public long getCreateTime() {
    return pollerState.getCreateTime();
  }
  
  /**
   * Return the serialization state directory used by this poll.
   */
  public File getStateDir() {
    return serializer.pollDir;
  }

  /**
   * Not used by V3
   */
  public PeerIdentity getCallerID() {
    return pollerState.getPollerId();
  }

  /**
   * Not used by V3
   */
  protected boolean isErrorState() {
    return false;
  }

  public boolean isPollActive() {
    return activePoll;
  }

  public boolean isPollCompleted() {
    return !activePoll;
  }

  /**
   * Not used by V3
   */
  public boolean isMyPoll() {
    return true;
  }

  /**
   * Return the PollSpec for this poll.
   */
  public PollSpec getPollSpec() {
    return pollerState.getPollSpec();
  }

  /**
   * Return the CachedUrlSet for this poll.
   */
  public CachedUrlSet getCachedUrlSet() {
    return pollerState.getCachedUrlSet();
  }

  /**
   * Return the poll version.
   */
  public int getVersion() {
    return pollerState.getProtocolVersion();
  }
  
  /**
   * Return the ID Manager.
   */
  public IdentityManager getIdentityManager() {
    return idManager;
  }

  /**
   * Return the message that started this poll.  V3Poller does not provide
   * an implementation of this method.
   */
  public LcapMessage getMessage() {
    throw new UnsupportedOperationException("V3Poller does not suppor " +
                "Poller.getMessage()");
  }

  /**
   * Return the size of the poll.
   */
  public int getPollSize() {
    return theParticipants.size();
  }

  /**
   * Return the poll key.
   */
  public String getKey() {
    return pollerState.getPollKey();
  }

  public Deadline getDeadline() {
    return Deadline.restoreDeadlineAt(pollerState.getPollDeadline());
  }

  public long getDuration() {
    return pollerState.getDuration();
  }

  public int getQuorum() {
    return pollerState.getQuorum();
  }

  public Iterator getParticipants() {
    return theParticipants.values().iterator();
  }

  public PollTally getVoteTally() {
    throw new UnsupportedOperationException("Not implemented for V3");
  }

  public int getType() {
    return Poll.V3_POLL;
  }

  public ArchivalUnit getAu() {
    return pollerState.getCachedUrlSet().getArchivalUnit();
  }

  public String getStatusString() {
    return V3Poller.POLLER_STATUS_STRINGS[pollerState.getStatus()];
  }
  
  public int getStatus() {
    return pollerState.getStatus();
  }

  private void setStatus(int status) {
    if (pollerState.getStatus() != status) {
      pollerState.setStatus(status);
      checkpointPoll();
    }
  }

  public long getVoteDeadline() {
    return pollerState.getVoteDeadline();
  }
  
  public long getVoteDuration() {
    return pollerState.getVoteDuration();
  }

  public List getActiveRepairs() {
    return pollerState.getRepairQueue().getActiveRepairs();
  }

  public List getCompletedRepairs() {
    return pollerState.getRepairQueue().getCompletedRepairs();
  }

  public PollerStateBean.RepairQueue getRepairQueue() {
    return pollerState.getRepairQueue();
  }

  public List getTalliedUrls() {
    List allUrls = new ArrayList();
    allUrls.addAll(getAgreedUrls());
    allUrls.addAll(getDisagreedUrls());
    allUrls.addAll(getTooCloseUrls());
    allUrls.addAll(getNoQuorumUrls());
    return allUrls;
  }

  public Set getAgreedUrls() {
    return pollerState.getTallyStatus().getAgreedUrls();
  }

  public Set getDisagreedUrls() {
    return pollerState.getTallyStatus().getDisagreedUrls();
  }

  public Set getTooCloseUrls() {
    return pollerState.getTallyStatus().getTooCloseUrls();
  }

  public Set getNoQuorumUrls() {
    return pollerState.getTallyStatus().getNoQuorumUrls();
  }
  
  public Map<String,String> getErrorUrls() {
    return pollerState.getTallyStatus().getErrorUrls();
  }

  /**
   * Release members not used after the poll has been closed.
   */
  public void release() {
    pollerState.release();
    
    for (Iterator it = theParticipants.values().iterator(); it.hasNext(); ) {
      ((ParticipantUserData)it.next()).release();
    }

    stateDir = null;
    pollCompleteRequest = null;
    voteCompleteRequest = null;
    task = null;
    serializer = null;
    pollManager = null;
    idManager = null;
  }

  /**
   * Generate a random nonce for the poller.
   *
   * @return A random array of 20 bytes.
   */
  private byte[] makePollerNonce() {
    return ByteArray.makeRandomBytes(20);
  }
}
