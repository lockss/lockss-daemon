/*
 * $Id: V3Poller.java,v 1.24 2006-06-02 20:27:15 smorabito Exp $
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
import java.security.*;
import java.util.*;

import org.lockss.app.*;
import org.lockss.config.*;
import org.lockss.crawler.*;
import org.lockss.crawler.CrawlManager.*;
import org.lockss.daemon.*;
import org.lockss.daemon.Crawler.*;
import org.lockss.hasher.*;
import org.lockss.plugin.*;
import org.lockss.poller.*;
import org.lockss.poller.v3.V3Serializer.*;
import org.lockss.protocol.*;
import org.lockss.protocol.psm.*;
import org.lockss.repository.RepositoryManager;
import org.lockss.scheduler.*;
import org.lockss.scheduler.Schedule.*;
import org.lockss.state.NodeManager;
import org.lockss.util.*;

/**
 * Class that runs a V3 Poll.
 */

public class V3Poller extends BasePoll {

  // Status strings used by the peers.
  public static final String PEER_STATUS_INITIALIZED = "Initialized";
  public static final String PEER_STATUS_WAITING_POLL_ACK = "Invited";
  public static final String PEER_STATUS_ACCEPTED_POLL = "Accepted Poll";
  public static final String PEER_STATUS_NOMINATED = "Sent Nominees";
  public static final String PEER_STATUS_WAITING_VOTE = "Waiting for Vote";
  public static final String PEER_STATUS_VOTED = "Voted";
  public static final String PEER_STATUS_COMPLETE = "Complete";
  public static final String PEER_STATUS_ERROR = "Error";
  public static final String PEER_STATUS_DROPPED_OUT = "Dropped Out";
  
  public static final String POLLER_STATUS_STARTING = "Starting";
  public static final String POLLER_STATUS_NO_TIME = "No Time Available";
  public static final String POLLER_STATUS_RESUMING = "Resuming";
  public static final String POLLER_STATUS_INVITING_PEERS = "Inviting Peers";
  public static final String POLLER_STATUS_HASHING = "Hashing";
  public static final String POLLER_STATUS_TALLYING = "Tallying";
  public static final String POLLER_STATUS_COMPLETE = "Complete";
  public static final String POLLER_STATUS_NO_QUORUM = "No Quorum";
  public static final String POLLER_STATUS_ERROR = "Error";

  private static String PREFIX = Configuration.PREFIX + "poll.v3.";

  /** Quorum for V3 polls. */
  static final String PARAM_QUORUM = PREFIX + "quorum";
  static final int DEFAULT_QUORUM = 5;

  /** Minimum number of participants for a V3 poll. */
  public static String PARAM_MIN_POLL_SIZE = PREFIX + "minPollSize";
  public static int DEFAULT_MIN_POLL_SIZE = 5;

  /** Maximum number of participants for a V3 poll. */
  public static String PARAM_MAX_POLL_SIZE = PREFIX + "maxPollSize";
  public static int DEFAULT_MAX_POLL_SIZE = 10;

  /** Target size of the outer circle poll. */
  public static String PARAM_TARGET_OUTER_CIRCLE_SIZE =
    PREFIX + "targetOuterCircle";
  public static int DEFAULT_TARGET_OUTER_CIRCLE_SIZE = 10;

  /** If true, drop participants from this poll that do not send
   * outer circle nominees. */
  public static String PARAM_DROP_EMPTY_NOMINATIONS =
    PREFIX + "dropEmptyNominations";
  public static boolean DEFAULT_DROP_EMPTY_NOMINATIONS = false;
  
  /** If true, just log a message rather than deleting files that are
   * considered to be missing from a majority of peers.
   */
  public static String PARAM_DELETE_EXTRA_FILES =
    PREFIX + "deleteExtraFiles";
  public static boolean DEFAULT_DELETE_EXTRA_FILES =
    false;
  
  /**
   * Directory in which to store message data.
   */
  public static String PARAM_V3_MESSAGE_DIR =
    PREFIX + "messageDir";
  public static String DEFAULT_V3_MESSAGE_DIR =
    System.getProperty("java.io.tmpdir");
  
  /**
   * Padding to add to the scheduled vote deadline, in ms.
   */
  public static final String PARAM_VOTE_DEADLINE_PADDING =
    PREFIX + "voteDeadlinePadding";
  public static final long DEFAULT_VOTE_DEADLINE_PADDING =
    1000 * 60 * 10; // ten minutes 

  // Global state for the poll.
  private PollerStateBean pollerState;
  // Map of PeerIdentity => ParticipantState for all participants in
  // this poll, both inner and outer circle
  protected Map theParticipants =
    Collections.synchronizedMap(new LinkedHashMap());
  private LockssDaemon theDaemon;
  private PollManager pollManager;
  private RepositoryManager repositoryManager;
  private IdentityManager idManager;
  private V3PollerSerializer serializer;
  private boolean resumedPoll;
  private boolean activePoll = true;
  private boolean dropEmptyNominators = DEFAULT_DROP_EMPTY_NOMINATIONS;
  private boolean deleteExtraFiles = DEFAULT_DELETE_EXTRA_FILES;
  private File messageDir;

  // Used by setConfig when setting or restoring state
  private int minParticipants;
  private int maxParticipants;
  private int quorum;
  private int outerCircleTarget;
  private long voteDeadlinePadding = DEFAULT_VOTE_DEADLINE_PADDING;

  private static Logger log = Logger.getLogger("V3Poller");
  private static LockssRandom theRandom = new LockssRandom();

  private SchedulableTask task;
  private TimerQueue.Request pollCompleteRequest;
  private TimerQueue.Request voteCompleteRequest;

  /**
   * Create a new V3 Poll.
   */
  public V3Poller(PollSpec spec, LockssDaemon daemon, PeerIdentity orig,
                  String key, long duration, String hashAlg)
      throws V3Serializer.PollSerializerException {
    this.theDaemon = daemon;
    this.pollManager = daemon.getPollManager();
    this.idManager = daemon.getIdentityManager();
    this.repositoryManager = daemon.getRepositoryManager();
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

    if (reserveScheduleTime(maxParticipants + outerCircleTarget)) {
      log.debug("Scheduled time for a new poll with a requested poll size of "
                + pollSize);
      // Set the voter deadline.
      long estimatedHashTime = getCachedUrlSet().estimatedHashDuration();
      long voteDeadline = TimeBase.nowMs() +
                          estimatedHashTime +
                          voteDeadlinePadding;
      // Sanity check.
      if (voteDeadline >= getDeadline().getExpirationTime()) {
        log.warning("Estimated hash time of CU with vote deadline padding (" +
                    voteDeadlinePadding +
                    ") is longer than the poll duration.  Can't start poll.");
        stopPoll(POLLER_STATUS_NO_TIME);
        return;
      }
      pollerState.setVoteDeadline(voteDeadline);
    } else {
      log.warning("Unable to schedule time for this poll!");
      stopPoll(POLLER_STATUS_NO_TIME);
      return;
    }

    // Checkpoint the poll.
    checkpointPoll();
  }

  /**
   * Restore a V3 Poll from a serialized state.
   */
  public V3Poller(LockssDaemon daemon, File pollDir)
      throws V3Serializer.PollSerializerException {
    theDaemon = daemon;
    serializer = new V3PollerSerializer(theDaemon, pollDir);
    pollerState = serializer.loadPollerState();
    repositoryManager = daemon.getRepositoryManager();
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
    voteDeadlinePadding = c.getTimeInterval(PARAM_VOTE_DEADLINE_PADDING,
                                            DEFAULT_VOTE_DEADLINE_PADDING);
    messageDir =
      new File(c.get(PARAM_V3_MESSAGE_DIR, DEFAULT_V3_MESSAGE_DIR));
    if (!messageDir.exists() || !messageDir.canWrite()) {
      throw new IllegalArgumentException("Configured V3 data directory " +
                                         messageDir +
                                         " does not exist or cannot be " +
                                         "written to.");
    }
  }

  /**
   * @param maxParticipants  The maximum number of participants that will
   *        participate in this poll (invitees and outer circle)
   */
  boolean reserveScheduleTime(int maxParticipants) {
    CachedUrlSet cus = this.getCachedUrlSet();
    long estimatedHashDuration = cus.estimatedHashDuration();
    long now = TimeBase.nowMs();
    // This assumes that all participants have roughly the same estimated
    // hash duration for this CUS.
    Deadline earliestStart = Deadline.at(now + estimatedHashDuration);
    Deadline latestFinish =
      Deadline.at(earliestStart.getExpirationTime() +
                  (estimatedHashDuration * maxParticipants));
    TaskCallback tc = new TaskCallback() {
      public void taskEvent(SchedulableTask task, EventType type) {
        // do nothing... yet!
      }
    };
    this.task = new StepTask(earliestStart, latestFinish,
                             estimatedHashDuration,
                             tc, this) {
      public int step(int n) {
        // do nothing... yet!
        return n;
      }
    };
    return theDaemon.getSchedService().scheduleTask(task);
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
    }
    setStatus(V3Poller.POLLER_STATUS_INVITING_PEERS);
    log.debug("Scheduling V3 poll " + pollerState.getPollKey() +
              " to complete by " + pollerState.getPollDeadline());
    // Schedule the poll deadline.  The poll must complete by this time.
    pollCompleteRequest =
      TimerQueue.schedule(Deadline.at(pollerState.getPollDeadline()),
                          new PollCompleteCallback(), this);
    // Schedule the vote tally callback.  The poll will tally votes no earlier
    // than this deadline.
    voteCompleteRequest =
      TimerQueue.schedule(Deadline.at(pollerState.getVoteDeadline()),
                          new VoteTallyCallback(), this);
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
  }

  /**
   * Stop the poll, and set the supplied status.
   */
  public void stopPoll(final String status) {
    // Want this action to be serialized.
    pollManager.runTask(new V3PollerTask() {
      public void run() {
        setStatus(status);
        activePoll = false;
        if (task != null && !task.isExpired()) {
          log.debug2("Cancelling task");
          task.cancel();
        }
        if (voteCompleteRequest != null) {
          log.debug2("Cancelling vote completion timer event.");
          TimerQueue.cancel(voteCompleteRequest);
        }
        if (pollCompleteRequest != null) {
          log.debug2("Cancelling poll completion timer event.");
          TimerQueue.cancel(pollCompleteRequest);
        }
        // Clean up any lingering participants.
        serializer.closePoll();
        for (Iterator voters = theParticipants.values().iterator(); voters.hasNext(); ) {
          ParticipantUserData ud = (ParticipantUserData)voters.next();
          VoteBlocks vb = ud.getVoteBlocks();
          if (vb != null) {
            vb.release();
          }
        }
        pollManager.closeThePoll(pollerState.getPollKey());
        log.debug("Closed poll " + pollerState.getPollKey());
      }
    });
  }
  
  /**
   * <p>Stop the poll.  Overrides BasePoll.stopPoll().</p>
   *
   * <p>May be called by the PollManager, or internally to abort the poll.</p>
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
    for (Iterator it = outerCircle.iterator(); it.hasNext(); ) {
      String idStr = (String) it.next();
      PeerIdentity id = idManager.findPeerIdentity(idStr);
      if (!theParticipants.containsKey(id)) {
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
        log.debug2("We already have peer " + id + " in our list of voters");
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
      List nominees = participant.getNominees();
      if (nominees == null || nominees.size() == 0) {
        // If we want to drop peers that don't send any nominees,
        // they will have already been removed at this point,
        // so just log a debug statement and go on.
        log.debug2("Peer " + participant.getVoterId()
                   + " did not nominate anyone");
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
      new ParticipantUserData(id, this, messageDir);
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
   * @param hb
   */
  private void tallyBlock(HashBlock hb) {
    setStatus(V3Poller.POLLER_STATUS_TALLYING);

    int missingBlockVoters = 0;
    int digestIndex = 0;

    // By this time, only voted peers will exist in 'theParticipants'.
    // Everyone else will have been removed.
    do {
      missingBlockVoters = 0;
      digestIndex = 0;
      BlockTally tally = new BlockTally(pollerState.getQuorum());
      for (Iterator it = theParticipants.values().iterator(); it.hasNext();) {
        ParticipantUserData voter = (ParticipantUserData)it.next();
        VoteBlocksIterator iter = voter.getVoteBlockIterator();
        
        VoteBlock vb = null;
        try {
          vb = iter.peek();
          if (vb == null) {
            // No block was returned.  This means this voter is out of
            // blocks.
            tally.addExtraBlockVoter(voter.getVoterId());
          } else {
            // Cache results in case we need to check this repair.
            tally.addVoteForBlock(voter.getVoterId(), vb);
            int sortOrder = hb.getUrl().compareTo(vb.getUrl());
            if (sortOrder > 0) {
              tally.addMissingBlockVoter(voter.getVoterId(), vb.getUrl());
              iter.next();
              missingBlockVoters++;
            } else if (sortOrder < 0) {
              tally.addExtraBlockVoter(voter.getVoterId());
            } else { // equal
              iter.next();
              compareBlocks(voter.getVoterId(), ++digestIndex, vb, hb, tally);
            }
          }
        } catch (IOException ex) {
          // This would be bad enough to stop the poll and raise an alert.
          log.error("IOException while iterating over vote blocks.", ex);
          stopPoll(V3Poller.POLLER_STATUS_ERROR);
          return;
        }
      }
      
      tally.tallyVotes();

      checkTally(tally, hb.getUrl(), false);
    } while (missingBlockVoters > 0);
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
        
        try {
          if (iter.peek() != null) {
            VoteBlock vb = iter.next();
            tally.addVoteForBlock(voter.getVoterId(), vb);
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
        checkTally(tally, tally.getMissingBlockUrl(), false);
      }
      
    } while (missingBlockVoters > 0);
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
    setStatus(V3Poller.POLLER_STATUS_TALLYING);
    int result = tally.getTallyResult();
    PollerStateBean.TallyStatus tallyStatus = pollerState.getTallyStatus();
    
    // Linked hash map - order is significant
    LinkedHashMap votesForBlock = tally.getVotesForBlock();
    String pollKey = pollerState.getPollKey();
    switch(result) {
    case BlockTally.RESULT_WON:
      tallyStatus.addAgreedUrl(url);
      // Great, we won!  Do nothing.
      log.debug3("Won tally for block: " + url + " in poll " + pollKey);
      // If this is the result of a previous repair, mark it complete.
      if (markComplete) pollerState.getRepairQueue().markComplete(url);
      break;
    case BlockTally.RESULT_LOST:
      tallyStatus.addDisagreedUrl(url);
      log.debug3("Lost tally for block: " + url);
      requestRepair(url, tally.getDisagreeVoters(), votesForBlock);
      break;
    case BlockTally.RESULT_LOST_EXTRA_BLOCK:
      log.debug3("Lost tally. Removing extra block " + url +
                 " in poll " + pollKey);
      deleteBlock(url);
      break;
    case BlockTally.RESULT_LOST_MISSING_BLOCK:
      log.debug3("Lost tally. Requesting repair for missing block: "
                 + url + " in poll " + pollKey);
      tallyStatus.addDisagreedUrl(url);
      String missingURL = tally.getMissingBlockUrl();
      requestRepair(missingURL,
                    tally.getMissingBlockVoters(missingURL),
                    votesForBlock);
      break;
    case BlockTally.RESULT_NOQUORUM:
      tallyStatus.addNoQuorumUrl(url);
      log.warning("No Quorum for block " + url
                  + " in poll " + pollKey);
      // If this is the result of a previous repair, mark it complete.
      if (markComplete) pollerState.getRepairQueue().markComplete(url);
      break;
    case BlockTally.RESULT_TOO_CLOSE:
    case BlockTally.RESULT_TOO_CLOSE_MISSING_BLOCK:
    case BlockTally.RESULT_TOO_CLOSE_EXTRA_BLOCK:
      tallyStatus.addTooCloseUrl(url);
      log.warning("Tally was inconclusive for block " + url
                  + " in poll " + pollKey);
      // If this is the result of a previous repair, mark it complete.
      if (markComplete) pollerState.getRepairQueue().markComplete(url);
      break;
    default:
      log.warning("Unexpected results from tallying block " + url + ": "
                  + tally.getStatusString());
      // If this is the result of a previous repair, mark it complete.
      if (markComplete) pollerState.getRepairQueue().markComplete(url);
    }
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
    if (hashService.scheduleHash(hasher, deadline,
                                 cb, null)) {
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
   * Request a repair for the specified URL.
   * @param url
   * @param disagreeingVoters Set of disagreeing voters.
   * @param votesForBlock Ordered map of votes previously collected for this block
   */
  private void requestRepair(final String url,
                             final Collection disagreeingVoters,
                             final LinkedHashMap votesForBlock) {
    log.debug("Attempting to repair url " + url + " from the publisher.");
    CrawlManager cm = theDaemon.getCrawlManager();

    CrawlManager.Callback cb = new CrawlManager.Callback() {
      public void signalCrawlAttemptCompleted(boolean success, Set urlsFetched,
                                              Object cookie, Status status) {
        if (success) {
          // Check the repair
          receivedRepair(url);
        } else {
          // If that didn't work, request a repair from a voter.
          requestRepairFromPeer(url, disagreeingVoters, votesForBlock);
        }
      }
    };

    // Set this repair active.
    PollerStateBean.Repair r = new PollerStateBean.Repair(url, votesForBlock);
    r.setRepairFromPublisher();
    pollerState.getRepairQueue().addActiveRepair(url, r);

    cm.startRepair(getAu(), ListUtil.list(url),
                   cb, null, null);
    
  }
  
  private void requestRepairFromPeer(String url,
                                     Collection disagreeingVoters,
                                     LinkedHashMap votesForBlock) {
    // XXX:  Use plain hash as a hint for who to requet a repair from.
    PeerIdentity repairVoter =
      (PeerIdentity)CollectionUtil.randomSelection(disagreeingVoters);
    log.debug2("Requesting repair for target: " + url + " from "
              + repairVoter);
    pollerState.getRepairQueue().addActiveRepair(url,
                                                 repairVoter,
                                                 votesForBlock);
    ParticipantUserData ud =
      (ParticipantUserData)theParticipants.get(repairVoter);
    V3LcapMessage msg = ud.makeMessage(V3LcapMessage.MSG_REPAIR_REQ);
    msg.setTargetUrl(url);
    msg.setEffortProof(null);
    try {
      sendMessageTo(msg, repairVoter);
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
    if (deleteExtraFiles) {
      CachedUrlSetSpec cuss =
        new SingleNodeCachedUrlSetSpec(url);
      CachedUrlSet cus = getAu().makeCachedUrlSet(cuss);
      NodeManager nm = theDaemon.getNodeManager(getAu());
      try {
        log.debug("Marking block deleted: " + url);
        nm.deleteNode(cus);
      } catch (IOException ex) {
        log.warning("Unable to mark CUS deleted: ", ex);
      }
    } else {
      log.info("Asked to mark file " + url + " deleted in poll " +
               pollerState.getPollKey());
    }

    pollerState.getRepairQueue().addCompletedRepair(url);
  }

  /**
   * Callback used to schedule a small re-check hash
   * when a repair has been received.
   */
  public void receivedRepair(final String url) {
    BlockHasher.EventHandler blockDone =
      new BlockHasher.EventHandler() {
        public void blockDone(HashBlock hblock) {
          PollerStateBean.RepairQueue rq = pollerState.getRepairQueue();
          Map votesForBlock = rq.getVotesForBlock(url);
          log.debug3("Finished hashing repair sent for block " + hblock);
          // Replay the block comparison using the new hash results.
          int digestIndex = 0;
          BlockTally tally = new BlockTally(pollerState.getQuorum());
          for (Iterator iter = votesForBlock.keySet().iterator(); iter.hasNext(); ) {
            digestIndex++;
            PeerIdentity id = (PeerIdentity)iter.next();
            VoteBlock vb = (VoteBlock)votesForBlock.get(id);
            compareBlocks(id, digestIndex, vb, hblock, tally);
          }
          tally.tallyVotes();
          log.debug3("After-vote hash tally for repaired block " + url
                     + ": " + tally.getStatusString());
          checkTally(tally, hblock.getUrl(), true);
        }
    };

    HashService.Callback hashDone =
      new HashService.Callback() {
        public void hashingFinished(CachedUrlSet urlset, Object cookie,
                                    CachedUrlSetHasher hasher, Exception e) {
          // Do nothing.
        }
    };

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

  /**
   * Compare a hash block and a vote block.
   */
  void compareBlocks(PeerIdentity voter,
                     int hashIndex,
                     VoteBlock voteBlock,
                     HashBlock hashBlock,
                     BlockTally tally) {

    boolean foundAgreement = false;

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

    for (int hbIdx = 0; hbIdx < hbVersions.length;  hbIdx++ ) {
      byte[] hasherResults = hbVersions[hbIdx].getHashes()[hashIndex];
      for (int vbIdx = 0; vbIdx < vbVersions.length; vbIdx++) {
        byte[] voterResults = vbVersions[vbIdx].getHash();
        if (log.isDebug3()) {
          log.debug3("Comparing blocks for voter " + voter);
          log.debug3("Hasher results: "
                     + ByteArray.toBase64(hasherResults));
          log.debug3("Voter results: "
                     + ByteArray.toBase64(voterResults));
        }
        
        if (Arrays.equals(voterResults, hasherResults)) {
          log.debug3("Found agreement between voter version " + vbIdx +
                     " and hash block version " + hbIdx);
          foundAgreement = true;
          break;
        }
      }
    }
    
    if (foundAgreement) {
      tally.addAgreeVoter(voter);
    } else {
      log.debug3("No agreement found.  Lost tally.");
      tally.addDisagreeVoter(voter);
    }
    
  }


  // The vote is over.
  private void voteComplete() {
    pollManager.runTask(new V3PollerTask() {
      public void run() {
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
        }
        setStatus(POLLER_STATUS_COMPLETE);
      }
    });
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
    try {
      serializer.savePollerState(pollerState);
    } catch (PollSerializerException ex) {
      // XXX: REMOVE
      log.warning("Unable to save poller state", ex);
    }
  }

  /**
   * Checkpoint the per-participant state.
   * @throws PollSerializerException
   */
  private void checkpointParticipant(ParticipantUserData ud)
      throws PollSerializerException {
    serializer.savePollerUserData(ud);
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
        log.warning("State machine error", e);
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

  Collection getReferenceList() {
    Collection refList = idManager.getTcpPeerIdentities();
    log.debug2("Initial reference list for poll: " + refList);
    return refList;
  }

  Class getPollerActionsClass() {
    return PollerActions.class;
  }
  
  private class VoteTallyCallback implements TimerQueue.Callback {
    public void timerExpired(Object cookie) {
      
      // Prune "theParticipants", and remove any who have not cast a vote.
      // Iterate over a COPY of the participants, to avoid concurrent
      // modifications.
      Collection peerListCopy = new ArrayList(theParticipants.keySet());
      for (Iterator it = peerListCopy.iterator(); it.hasNext(); ) {
        PeerIdentity id = (PeerIdentity)it.next();
        if (!pollerState.getVotedPeers().contains(id)) {
          removeParticipant(id);
        }
      }

      // Determine whether enough peers have voted to reach a quorum.
      // If not, kill this poll.
      if (theParticipants.size() < getQuorum()) {
        log.warning("Not enough participants voted to achieve quorum " +
                    "in poll " + getKey() + ". Stopping poll.");
        stopPoll(V3Poller.POLLER_STATUS_NO_QUORUM);
        return;
      } else {
        log.debug2("Vote Tally deadline reached.  Scheduling hash.");
        // XXX: Refactor when our hash can be associated with an
        //      existing step task.
        task.cancel();
        if (!scheduleHash(pollerState.getCachedUrlSet(),
                          Deadline.at(pollerState.getPollDeadline()),
                          new HashingCompleteCallback(),
                          new BlockEventHandler())) {
          log.error("No time available to schedule our hash for poll "
                    + pollerState.getPollKey());
          stopPoll(POLLER_STATUS_NO_TIME);
        }
        pollerState.hashStarted(true);
      }
    }
    public String toString() {
      return "V3 Poll Tally";
    }
  }

  private class PollCompleteCallback implements TimerQueue.Callback {
    /**
     * Called when the poll timer expires.
     *
     * @param cookie data supplied by caller to schedule()
     */
    public void timerExpired(Object cookie) {
      log.debug("Timer expired.  Completing the vote.");
      voteComplete();
    }

    public String toString() {
      return "V3 Poll";
    }
  }

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

  private class BlockEventHandler implements BlockHasher.EventHandler {
    public void blockDone(HashBlock block) {
      tallyBlock(block);
    }
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
    return pollerState.getStatusString();
  }

  private void setStatus(String status) {
    pollerState.setStatusString(status);
    checkpointPoll();
  }
  
  public long getVoteDeadline() {
    return pollerState.getVoteDeadline();
  }

  // Status accessor convenience methods
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
    return pollerState.getTallyStatus().agreedUrls;
  }
  
  public Set getDisagreedUrls() {
    return pollerState.getTallyStatus().disagreedUrls;
  }
  
  public Set getTooCloseUrls() {
    return pollerState.getTallyStatus().tooCloseUrls;
  }
  
  public Set getNoQuorumUrls() {
    return pollerState.getTallyStatus().noQuorumUrls;
  }

  /**
   * Generate a random nonce for the poller.
   *
   * @return A random array of 20 bytes.
   */
  private byte[] makePollerNonce() {
    return ByteArray.makeRandomBytes(20);
  }
  
  /**
   * Base class for V3Poller Tasks
   */
  
  private abstract class V3PollerTask implements PollRunner.Task {
    public String toString() {
      return "[V3PollerTask poll=" + getPollId() + "]";
    }
    public String getPollId() {
      return getKey();
    }
  }
}
