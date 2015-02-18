/*
 * $Id$
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

package org.lockss.poller.v3;

import java.io.*;
import java.net.MalformedURLException;
import java.security.*;
import java.util.*;

import org.apache.commons.collections.*;

import org.lockss.app.*;
import org.lockss.alert.*;
import org.lockss.config.*;
import org.lockss.daemon.CachedUrlSetHasher;
import org.lockss.daemon.ShouldNotHappenException;
import org.lockss.hasher.*;
import org.lockss.plugin.*;
import org.lockss.poller.*;
import org.lockss.poller.v3.V3Serializer.PollSerializerException;
import org.lockss.protocol.*;
import org.lockss.protocol.V3LcapMessage.PollNak;
import org.lockss.protocol.psm.*;
import org.lockss.repository.RepositoryNode;
import org.lockss.state.*;
import org.lockss.scheduler.*;
import org.lockss.scheduler.Schedule.*;
import org.lockss.util.*;

/**
 * <p>Represents a voter in a V3 poll.</p>
 *
 * <p>State is maintained in a V3VoterState object, which is periodically 
 * serialized to disk so that polls may be resumed in case the daemon exits
 * before the poll is over.</p>
 */
public class V3Voter extends BasePoll {
  
  public static final int STATUS_INITIALIZED = 0;
  public static final int STATUS_ACCEPTED_POLL = 1;
  public static final int STATUS_HASHING = 2;
  public static final int STATUS_VOTED = 3;
  public static final int STATUS_NO_TIME = 4;
  public static final int STATUS_COMPLETE = 5;
  public static final int STATUS_EXPIRED = 6;
  public static final int STATUS_ERROR = 7;
  public static final int STATUS_DECLINED_POLL = 8;
  public static final int STATUS_VOTE_ACCEPTED = 9;
  public static final int STATUS_ABORTED = 10;
  public static final int STATUS_NO_SUBSTANCE = 11;
  
  public static final String[] STATUS_STRINGS = 
  {
   "Initialized", "Accepted Poll", "Hashing", "Voted",
   "No Time Available", "Complete", "Expired w/o Voting", "Error",
   "Declined Poll", "Vote Accepted", "Aborted",
   "Aborted: No Substantial Content",
  };

  private static final String PREFIX = Configuration.PREFIX + "poll.v3.";

  /** The minimum number of peers to select for a nomination message.
   * If there are fewer than this number of peers available to nominate,
   * an empty nomination message will be sent. */
  public static final String PARAM_MIN_NOMINATION_SIZE =
    PREFIX + "minNominationSize";
  public static final int DEFAULT_MIN_NOMINATION_SIZE = 1;

  /** The minimum number of peers to select for a nomination message. */
  public static final String PARAM_MAX_NOMINATION_SIZE = 
    PREFIX + "maxNominationSize";
  public static final int DEFAULT_MAX_NOMINATION_SIZE = 5;
  
  /** The maximum allowable number of simultaneous V3 Voters */
  public static final String PARAM_MAX_SIMULTANEOUS_V3_VOTERS =
    PREFIX + "maxSimultaneousV3Voters";
  public static final int DEFAULT_MAX_SIMULTANEOUS_V3_VOTERS = 60;
  
  /**
   * The minimum percent agreement required before we're willing to serve
   * repairs, if using per-AU agreement.
   */
  // CR: apply to bytes, not URLs
  public static final String PARAM_MIN_PERCENT_AGREEMENT_FOR_REPAIRS =
    PREFIX + "minPercentAgreementForRepairs";
  public static final double DEFAULT_MIN_PERCENT_AGREEMENT_FOR_REPAIRS = 0.5f; 

  /**
   * If true, previous agreement will be required to serve repairs even for
   * open access AUs
   */
  public static final String PARAM_OPEN_ACCESS_REPAIR_NEEDS_AGREEMENT =
    PREFIX + "openAccessRepairNeedsAgreement";
  public static final boolean
    DEFAULT_OPEN_ACCESS_REPAIR_NEEDS_AGREEMENT = false;

  /**
   * Extend reputation from old PID to new PID.  Reputation may be extended
   * from and to only one peer.  (E.g., both {A->B, A->C} and {B->A, C->A}
   * are illegal.) transitive mappings (E.g., {A->B, B->C}) are legal, up
   * to a macimum path length of 10.  This is for use by PLN admins when
   * changing IP of a node.  Should be replaced by secure, automatic
   * mechanism (e.g., box proves it's the same one by returning a
   * short-lived cookie from a recent poll).
   */
  public static final String PARAM_REUTATION_TRANSFER_MAP =
    PREFIX + "reputationTransferMap";

  /** 
   * Allowance for vote message send time: hash time multiplier
   */
  public static final String PARAM_VOTE_SEND_HASH_MULTIPLIER =
    PREFIX + "voteMsgHashMultiplier";
  public static final double DEFAULT_VOTE_SEND_HASH_MULTIPLIER = 0.01;

  /** 
   * Allowance for vote message send time: padding
   */
  public static final String PARAM_VOTE_SEND_PADDING =
    PREFIX + "voteMsgPadding";
  public static final long DEFAULT_VOTE_SEND_PADDING = 15 * Constants.SECOND;

  /** 
   * Extra time added to the poll deadline (as sent by the poller) to 
   * wait for a receipt message.
   */
  public static final String PARAM_RECEIPT_PADDING = PREFIX + "receiptPadding";
  public static final long DEFAULT_RECEIPT_PADDING = 10 * Constants.MINUTE;

  /** 
   * Guess as to how long after we accept we'll get a vote request.
   * S.b. used to send vote request deadline to poller
   */
  public static final String PARAM_VOTE_REQUEST_DELAY =
    PREFIX + "voteRequestDelay";
  public static final long DEFAULT_VOTE_REQUEST_DELAY =
    30 * Constants.SECOND;

  /**
   * If true, when an invitation is declined because our estimated hash
   * time is so long that it can't be completed within the vote time, even
   * if no other hashes are scheduled, the estimated hash time for the AU
   * will be recalculated.
   */
  public static final String PARAM_RECALC_EXCESSIVE_HASH_ESTIMATE =
    PREFIX + "recalcExcessiveHashEstimate";
  public static final boolean DEFAULT_RECALC_EXCESSIVE_HASH_ESTIMATE = true;

  /** 
   * Factor of vote duration used as guess for duration of hash to recalc
   * hash estimate.
   */
  public static final String
    PARAM_RECALC_HASH_ESTIMATE_VOTE_DURATION_MULTIPLIER
    = PREFIX + "recalcHashEstimateVoteDurationMultiplier";
  /** Just used to document default */
  public static final String
    DEFAULT_RECALC_HASH_ESTIMATE_VOTE_DURATION_MULTIPLIER
    = "Twice reciprocal of " + V3Poller.PARAM_VOTE_DURATION_MULTIPLIER;

  /** Curve expressing decreasing weight of nominating peer who has last
   * voted in one of our polls X time ago.  X coord is time (ms) since last
   * vote, Y is float nomination weight.
   * @see org.lockss.util.CompoundLinearSlope */
  public static final String PARAM_NOMINATION_WEIGHT_AGE_CURVE =
    PREFIX + "nominationWeightAgeCurve";
  public static final String DEFAULT_NOMINATION_WEIGHT_AGE_CURVE =
    "[10d,1.0],[30d,0.1],[40d,0.01]";

  /** Curve giving vote message retry interval as a function of remaining
   * time before vote deadline.
   * @see org.lockss.util.CompoundLinearSlope */
  public static final String PARAM_VOTE_RETRY_INTERVAL_DURATION_CURVE =
    PREFIX + "voteRetryIntervalDurationCurve";
  public static final String DEFAULT_VOTE_RETRY_INTERVAL_DURATION_CURVE =
    "[5m,2m],[20m,4m],[1d,5h]";

  /** If true, always request a symmetric poll */
  public static final String PARAM_ALL_SYMMETRIC_POLLS =
    PREFIX + "allSymmetricPolls";
  public static final boolean DEFAULT_ALL_SYMMETRIC_POLLS = false;

  /** If true, can vote in a Proof of Possession poll - for testing */
  public static final String PARAM_ENABLE_POP_VOTING =
    PREFIX + "enablePoPVoting";
  public static final boolean DEFAULT_ENABLE_POP_VOTING = true;

  /** If true, can request a symmetric poll */
  public static final String PARAM_ENABLE_SYMMETRIC_POLLS =
    PREFIX + "enableSymmetricPolls";
  public static final boolean DEFAULT_ENABLE_SYMMETRIC_POLLS = false;

  /* Minimum weight to call a symmetric poll */
  public static final String PARAM_MIN_WEIGHT_SYMMETRIC_POLL =
    PREFIX + "minWeightSymmetricPoll";
  public static final double DEFAULT_MIN_WEIGHT_SYMMETRIC_POLL = 0.5f; 

  /* The internal use of the arrays used by the hasher. */
  private static final int PLAIN_HASH = 0;
  private static final int CHALLENGE_HASH = 1;
  private static final int SYMMETRIC_HASH = 2;

  private PsmInterp stateMachine;
  private VoterUserData voterUserData;
  private SampledBlockHasher.FractionalInclusionPolicy inclusionPolicy = null;

  // CR: use global random
  private LockssRandom theRandom = new LockssRandom();
  private LockssDaemon theDaemon;
  private V3VoterSerializer pollSerializer;
  private PollManager pollManager;
  private LcapStreamComm scomm;
  private IdentityManager idManager;
  private boolean continuedPoll = false;
  private int nomineeCount;
  private File stateDir;
  private int blockErrorCount = 0;
  private int maxBlockErrorCount = V3Poller.DEFAULT_MAX_BLOCK_ERROR_COUNT;
  private SubstanceChecker subChecker;

  // Task used to reserve time for hashing at the start of the poll.
  // This task is cancelled before the real hash is scheduled.
  private SchedulableTask task;

  private static final Logger log = Logger.getLogger("V3Voter");

  // CR: refactor common parts of constructors
  /**
   * <p>Upon receipt of a request to participate in a poll, create a new
   * V3Voter.  The voter will not start running until {@link #startPoll()}
   * is called by {@link org.lockss.poller.v3.V3PollFactory}</p>
   */
  public V3Voter(LockssDaemon daemon, V3LcapMessage msg)
      throws V3Serializer.PollSerializerException {
    this.theDaemon = daemon;
    long padding =
      CurrentConfig.getTimeIntervalParam(V3Voter.PARAM_RECEIPT_PADDING,
                                         V3Voter.DEFAULT_RECEIPT_PADDING);
    long duration = msg.getDuration() + padding;

    log.debug3("Creating V3 Voter for poll: " + msg.getKey() +
               "; duration=" + StringUtil.timeIntervalToString(duration));

    String hashAlgorithm = msg.getHashAlgorithm();
    if (hashAlgorithm == null) {
      throw new IllegalArgumentException("hash algorithm in message was null.");
    }

    // If the hash algorithm is not available, fail the vote immediately.
    if (! PollUtil.canUseHashAlgorithm(hashAlgorithm)) {
      throw new IllegalArgumentException("Algorithm " + hashAlgorithm +
                                         " is not supported");
    }

    pollSerializer = new V3VoterSerializer(theDaemon);
    
    stateDir = PollUtil.ensurePollStateRoot();

    maxBlockErrorCount =
      CurrentConfig.getIntParam(V3Poller.PARAM_MAX_BLOCK_ERROR_COUNT,
                                V3Poller.DEFAULT_MAX_BLOCK_ERROR_COUNT);

    if (msg.getModulus() != 0) {
      if (CurrentConfig.getBooleanParam(PARAM_ENABLE_POP_VOTING,
					DEFAULT_ENABLE_POP_VOTING)) {
	MessageDigest sampleHasher = 
	  PollUtil.createMessageDigest(hashAlgorithm);
	int sampleModulus = msg.getModulus();
	byte[] sampleNonce = msg.getSampleNonce();
	this.inclusionPolicy = 
	  new SampledBlockHasher.FractionalInclusionPolicy(
            sampleModulus, sampleNonce, sampleHasher);
	log.debug("Sampled voter: "+this.inclusionPolicy.typeString());
      } else {
	log.debug("Ignoring sampled vote request: " + msg.getModulus());
      }
    }

    try {
      this.voterUserData = 
	new VoterUserData(new PollSpec(msg), this,
			  msg.getOriginatorId(), 
			  msg.getKey(),
			  duration,
			  hashAlgorithm,
			  msg.getModulus(),
			  msg.getSampleNonce(),
			  msg.getPollerNonce(),
			  PollUtil.makeHashNonce(V3Poller.HASH_NONCE_LENGTH),
			  msg.getEffortProof(),
			  stateDir);
      voterUserData.setPollMessage(msg);
      voterUserData.setVoteDeadline(TimeBase.nowMs() + msg.getVoteDuration());
    } catch (IOException ex) {
      log.critical("IOException while trying to create VoterUserData: ", ex);
      stopPoll();
    }
    this.idManager = theDaemon.getIdentityManager();

    this.pollManager = daemon.getPollManager();
    this.scomm = daemon.getStreamCommManager();

    if (shouldSymmetricPoll(msg.getOriginatorId())) {
      // Create a second nonce to request a symmetric poll
      byte[] nonce2 = PollUtil.makeHashNonce(V3Poller.HASH_NONCE_LENGTH);
      try {
	voterUserData.enableSymmetricPoll(nonce2);
      } catch (IOException ex) {
	log.warning("symmetric poll not possible: ", ex);
      }
      log.debug2("voterNonce2 set to: " + nonce2);
    } else {
      log.debug2("poll is not symmetric");
    }
    int min = CurrentConfig.getIntParam(PARAM_MIN_NOMINATION_SIZE,
                                        DEFAULT_MIN_NOMINATION_SIZE);
    int max = CurrentConfig.getIntParam(PARAM_MAX_NOMINATION_SIZE,
                                        DEFAULT_MAX_NOMINATION_SIZE);
    if (min < 0) min = 0;
    if (max < 0) max = 0;
    if (min > max) {
      log.warning("Nomination size min (" +  min + ") > max (" + max
                  + "). Using min.");
      nomineeCount = min;
    } else if (min == max) {
      log.debug2("Minimum nominee size is same as maximum nominee size: " +
                 min);
      nomineeCount = min;
    } else {
      int r = theRandom.nextInt(max - min);
      nomineeCount = min + r;
    }
    log.debug2("Will choose " + nomineeCount
               + " outer circle nominees to send to poller");
    postConstruct();
    checkpointPoll();
  }

  /**
   * <p>Restore a V3Voter from a previously saved poll.  This method is called
   * by {@link org.lockss.poller.PollManager} when the daemon starts up if a
   * serialized voter is found.</p>
   */
  public V3Voter(LockssDaemon daemon, File pollDir)
      throws V3Serializer.PollSerializerException {
    this.theDaemon = daemon;
    // CR: why pollDir passed to some V3VoterSerializer constructors, not
    // others?
    this.pollSerializer = new V3VoterSerializer(theDaemon, pollDir);
    this.voterUserData = pollSerializer.loadVoterUserData();
    this.idManager = theDaemon.getIdentityManager();

    this.pollManager = daemon.getPollManager();
    this.scomm = daemon.getStreamCommManager();
    this.continuedPoll = true;

    String hashAlgorithm = voterUserData.getHashAlgorithm();
    // If the hash algorithm is not available, fail the vote immediately.
    if (! PollUtil.canUseHashAlgorithm(hashAlgorithm)) {
      throw new IllegalArgumentException("Algorithm " + hashAlgorithm +
                                         " is not supported");
    }

    // Restore transient state.
    PluginManager plugMgr = theDaemon.getPluginManager();
    CachedUrlSet cus = plugMgr.findCachedUrlSet(voterUserData.getAuId());
    if (cus == null) {
      throw new NullPointerException("CUS for AU " + voterUserData.getAuId() +
                                     " is null!");
    }
    // Restore transient state
    voterUserData.setCachedUrlSet(cus);
    voterUserData.setPollSpec(new PollSpec(cus, Poll.V3_POLL));
    voterUserData.setVoter(this);

    postConstruct();
  }

  private void postConstruct() {
    stateMachine = makeStateMachine(voterUserData);

    // Set up substance checker
    subChecker = makeSubstanceChecker();
  }

  SubstanceChecker makeSubstanceChecker() {
    SubstanceChecker res = new SubstanceChecker(getAu());
    if (res.isEnabledFor(SubstanceChecker.CONTEXT_VOTE)) {
      AuState aus = AuUtil.getAuState(getAu());
      if (aus.getSubstanceState() == SubstanceChecker.State.Yes &&
	  AuUtil.isCurrentFeatureVersion(getAu(), Plugin.Feature.Substance)) {
	// Don't need to check for substance if already known to exist,
	// unless substance pattern version has changed.
	// This is not a valid assumption if polls are allowed to delete files.
	res = null;
      } else {
	log.debug2("Enabling substance checking");
	SubstanceChecker.State state = voterUserData.getSubstanceCheckerState();
	if (state != null) {
	  res.setHasSubstance(state);
	}
      }
    } else {
      res = null;
    }
    return res;
  }

  PsmInterp newPsmInterp(PsmMachine stateMachine, Object userData) {
    PsmManager mgr = theDaemon.getPsmManager();
    PsmInterp interp = mgr.newPsmInterp(stateMachine, userData);
    interp.setThreaded(true);
    return interp;
  }

  private PsmInterp makeStateMachine(final VoterUserData ud) {
    PsmMachine machine = makeMachine();
    PsmInterp interp = newPsmInterp(machine, ud);
    interp.setName(PollUtil.makeShortPollKey(getKey()));
    interp.setCheckpointer(new PsmInterp.Checkpointer() {
      public void checkpoint(PsmInterpStateBean resumeStateBean) {
        voterUserData.setPsmState(resumeStateBean);
        checkpointPoll();
      }
    });

    return interp;
  }
  
  public PsmInterp getPsmInterp() {
    return stateMachine;
  }

  /**
   * Provides a default no-arg constructor to be used for unit testing.
   */
  protected V3Voter() {
    
  }
  
  /**
   * <p>Reserve enough schedule time to hash our content and send our vote.</p>
   * 
   * @return True if time could be scheduled, false otherwise.
   */
  public boolean reserveScheduleTime() {
    long voteDeadline = voterUserData.getVoteDeadline();
    long estimatedHashDuration = getCachedUrlSet().estimatedHashDuration();
    long now = TimeBase.nowMs();

    // Ensure the vote deadline has not already passed.
    if (voteDeadline <= now) {
      String msg = "Vote deadline has already "
        + "passed.  Can't reserve schedule time.";
      voterUserData.setErrorDetail(msg);
      log.warning(msg);
      return false;
    }
    
    long voteReqDelay =
      CurrentConfig.getTimeIntervalParam(PARAM_VOTE_REQUEST_DELAY,
                                         DEFAULT_VOTE_REQUEST_DELAY);

    Deadline earliestStart = Deadline.at(now + voteReqDelay);
    // CR: eliminate reservation task; schedule hash here

    long messageSendPadding =
      calculateMessageSendPadding(estimatedHashDuration);

    Deadline latestFinish =
      Deadline.at(voterUserData.getVoteDeadline() - messageSendPadding);

    long voteDuration = latestFinish.minus(earliestStart);
    long schedDuration = getSchedDuration(voteDuration);

    if (estimatedHashDuration > voteDuration) {
      String msg = "Estimated hash duration (" 
        + StringUtil.timeIntervalToString(estimatedHashDuration) 
        + ") is too long to complete within the voting period ("
        + StringUtil.timeIntervalToString(voteDuration) + ")";
      voterUserData.setErrorDetail(msg);
      log.warning(msg);
      recalcHashEstimate(voterUserData.getVoteDeadline() - now);
      return false;
    }

    TaskCallback tc = new TaskCallback() {
      public void taskEvent(SchedulableTask task, EventType type) {
        // do nothing... yet!
      }
    };
    
    // Keep a hold of the task we're scheduling.
    this.task = new StepTask(earliestStart, latestFinish,
                             estimatedHashDuration,
                             tc, this) {
      public int step(int n) {
        // finish immediately, in case we start running
        setFinished();
        return n;
      }
    };

    boolean suc = theDaemon.getSchedService().scheduleTask(task);
    if (!suc) {
      voterUserData.setErrorDetail("No time for hash: " + task +
				   " at " + TimeBase.nowDate());
      log.warning("No time for hash: " + task);
    }
    return suc;
  }

  // Calculate min interval scheduler will require to accept a task,
  // including overhead.
  long getSchedDuration(long voteDuration) {
    Configuration config = ConfigManager.getCurrentConfig();
    double overheadLoad =
      config.getPercentage(SortScheduler.PARAM_OVERHEAD_LOAD,
			   SortScheduler.DEFAULT_OVERHEAD_LOAD);
    return (long)(voteDuration / (1.0 - overheadLoad));
  }

  /* XXX Ideally this would be a function of the number of vote blocks, but
   * that isn't available.  Instead, proportional to hash estimate, plus
   * padding  */
  private long calculateMessageSendPadding(long hashEst) {
    double mult =
      CurrentConfig.getDoubleParam(PARAM_VOTE_SEND_HASH_MULTIPLIER,
				   DEFAULT_VOTE_SEND_HASH_MULTIPLIER);
    return (long)(hashEst * mult)
      + CurrentConfig.getTimeIntervalParam(PARAM_VOTE_SEND_PADDING,
					   DEFAULT_VOTE_SEND_PADDING);
  }

  PsmInterp.ErrorHandler ehAbortPoll(final String msg) {
    return new PsmInterp.ErrorHandler() {
	public void handleError(PsmException e) {
	  log.warning(msg, e);
	  abortPollWithError();
	}
      };
  }

  private void sendNak(PollNak nak) {
    V3LcapMessage msg = voterUserData.makeMessage(V3LcapMessage.MSG_POLL_ACK);
    msg.setVoterNonce(null);
    msg.setNak(nak);
    try {
      sendMessageTo(msg, getPollerId());
      pollManager.countVoterNakEvent(nak);      
    } catch (IOException ex) {
      log.error("Unable to send POLL NAK message in poll " + getKey(), ex);
    }
  }

  void recalcHashEstimate(long voteDuration) {
    RecalcHashTime rht =
      new RecalcHashTime(theDaemon, getAu(), 2,
			 getHashAlgorithm(), voteDuration);
    rht.recalcHashTime();
    return;
  }

  /**
   * <p>Start the V3Voter running and participate in the poll.  Called by
   * {@link org.lockss.poller.v3.V3PollFactory} when a vote request message
   * has been received, and by {@link org.lockss.poller.PollManager} when
   * restoring serialized voters.</p>
   */
  public void startPoll() {
    log.debug("Starting poll " + voterUserData.getPollKey());
    Deadline pollDeadline = null;
    if (!continuedPoll) {
      // Skip deadline sanity check if this is a restored poll.
      pollDeadline = Deadline.at(voterUserData.getDeadline());
    } else {
      pollDeadline = Deadline.restoreDeadlineAt(voterUserData.getDeadline());
    }
    
    // If this poll has already expired, don't start it.
    if (pollDeadline.expired()) {
      log.info("Not restoring expired voter for poll " +
               voterUserData.getPollKey());
      stopPoll(STATUS_EXPIRED);
      return;
    }

    // First, see if we have time to participate.  If not, there's no
    // point in going on.
    if (reserveScheduleTime()) {
      long voteDeadline = voterUserData.getVoteDeadline();
      if (voteDeadline >= pollDeadline.getExpirationTime()) {
        log.warning("Voting deadline (" + voteDeadline + ") is later than " +
                    "the poll deadline (" + pollDeadline.getExpirationTime() + 
                    ").  Can't participate in poll " + getKey());
	// CR: s.b. poller error, not expired
        stopPoll(STATUS_EXPIRED);
        return;
      }
      log.debug("Found enough time to participate in poll " + getKey());
    } else {
      sendNak(V3LcapMessage.PollNak.NAK_NO_TIME);
      stopPoll(STATUS_NO_TIME);
      return;
    }

    // Register a callback for the end of the poll.
    TimerQueue.schedule(pollDeadline, new PollTimerCallback(), this);

    // Register a callback for the end of the voting period.  We must have
    // voted by this time, or we can't participate.
    // CR: could be folded into hash done cb
    TimerQueue.schedule(Deadline.at(voterUserData.getVoteDeadline()),
                        new TimerQueue.Callback() {
      public void timerExpired(Object cookie) {
        // In practice, we must prevent this from happening. Unfortunately,
        // due to the nature of the scheduler and the wide variety of machines
        // in the field, it is quite possible for us to still be hashing when
        // the vote deadline has arrived.
        // 
        // It's the poller's responsibility to ensure that it compensates for
        // slow machines by padding the vote deadline as much as necessary to
        // compensate for slow machines.
        if (!voterUserData.hashingDone()) {
          log.warning("Vote deadline has passed before my hashing was done " +
                      "in poll " + getKey() + ". Stopping the poll.");
          stopPoll(V3Voter.STATUS_EXPIRED);
        }
      }
    }, this);
    
    resumeOrStartStateMachine();
  }

  protected void resumeOrStartStateMachine() {
    // Resume or start the state machine running.
    if (continuedPoll) {
      String msg = "Error resuming poll";
      try {
	stateMachine.enqueueResume(voterUserData.getPsmState(),
				   ehAbortPoll(msg));
      } catch (PsmException e) {
	log.warning(msg, e);
	abortPollWithError();
      }
    } else {
      String msg = "Error starting poll";
      try {
	stateMachine.enqueueStart(ehAbortPoll(msg));
      } catch (PsmException e) {
	log.warning(msg, e);
	abortPollWithError();
      }
    }
  }

  /**
   * Stop the poll and tell the {@link PollManager} to let go of us.
   * 
   * @param status The final status code of the poll, for the status table.
   */
  public void stopPoll(final int status) {
    // Force the poll to be complete, and continue only if it was
    // previously not complete, to ensure that the rest of this method
    // is executed only once.
    if (!voterUserData.checkAndCompletePoll()) {
      return;
    }
    if (task != null && !task.isExpired()) {
      log.debug2("Cancelling poll time reservation task");
      task.cancel();
    }
    voterUserData.setStatus(status);
    // Clean up after the serializer
    pollSerializer.closePoll();
    pollManager.closeThePoll(voterUserData.getPollKey());
    log.debug2("Closed poll " + voterUserData.getPollKey() + " with status " +
               getStatusString() );
    release();
  }
  
  /**
   * Stop the poll with STATUS_COMPLETE.
   */
  public void stopPoll() {
    stopPoll(STATUS_COMPLETE);
  }

  /**
   * Stop the poll with STATUS_ERROR.
   */
  public void abortPoll() {
    stopPoll(STATUS_ERROR);
  }

  /**
   * Stop the poll with STATUS_ERROR.
   */
  private void abortPollWithError() {
    stopPoll(STATUS_ERROR);
  }

  private Class getVoterActionsClass() {
    return VoterActions.class;
  }

  /**
   * Send a message to the poller.
   */
  void sendMessageTo(V3LcapMessage msg, PeerIdentity id)
      throws IOException {
    if (log.isDebug2()) {
      log.debug2("sendTo(" + msg + ", " + id + ")");
    }
    pollManager.sendMessageTo(msg, id);
  }

  /**
   * Handle an incoming V3LcapMessage.
   */
  public void receiveMessage(LcapMessage message) {
    // It's quite possible to receive a message after we've decided
    // to close the poll, but before the PollManager knows we're closed.
    if (voterUserData.isPollCompleted()) return;

    final V3LcapMessage msg = (V3LcapMessage)message;
    PeerIdentity sender = msg.getOriginatorId();
    PsmMsgEvent evt = V3Events.fromMessage(msg);
    log.debug3("Received message: " + message.getOpcodeString() + " " + message);
    String errmsg = "State machine error";
    stateMachine.enqueueEvent(evt, ehAbortPoll(errmsg),
			      new PsmInterp.Action() {
				public void eval() {
				  msg.delete();
				}
			      });
    // Finally, clean up after the V3LcapMessage
    // todo(bhayes): this still needed?
    msg.delete();    
  }

  /**
   * Generate a list of outer circle nominees.
   */
  public void nominatePeers() {
    // XXX:  'allPeers' should probably contain only peers that have agreed with
    //       us in the past for this au.
    if (isPollCompleted()) {
      log.warning("nominatePeers called on a possibly closed poll: "
                  + getKey());
      return;
    }

    Collection<PeerIdentity> nominees;
    DatedPeerIdSet noAuSet = pollManager.getNoAuPeerSet(getAu());
    synchronized (noAuSet) {
      try {
	try {
	  noAuSet.load();
	  pollManager.ageNoAuSet(getAu(), noAuSet);
	} catch (IOException e) {
	  log.error("Failed to load no AU set", e);
	  noAuSet.release();
	  noAuSet = null;
	}
	nominees = idManager.getTcpPeerIdentities(new NominationPred(noAuSet));
      } finally {
	if (noAuSet != null) {
	  noAuSet.release();
	}
      }
    }
    if (nomineeCount <= nominees.size()) {
      Map availablePeers = new HashMap();
      for (PeerIdentity id : nominees) {
	availablePeers.put(id, nominateWeight(id));
      }
      nominees = CollectionUtil.weightedRandomSelection(availablePeers,
							nomineeCount);
    }
    if (!nominees.isEmpty()) {
      // VoterUserData expects the collection to be KEYS, not PeerIdentities.
      ArrayList nomineeStrings = new ArrayList(nominees.size());
      for (PeerIdentity id : nominees) {
	nomineeStrings.add(id.getIdString());
      }
      voterUserData.setNominees(nomineeStrings);
      log.debug2("Nominating the following peers: " + nomineeStrings);
    } else {
      log.warning("No peers to nominate");
    }
    checkpointPoll();
  }

  // Don't nominate peers unless have positive evidence of correct group.
  // Also, no aging as with poll invites
  class NominationPred implements Predicate {
    DatedPeerIdSet noAuSet;

    NominationPred(DatedPeerIdSet noAuSet) {
      this.noAuSet = noAuSet;
    }

    public boolean evaluate(Object obj) {
      if (obj instanceof PeerIdentity) {
	PeerIdentity pid = (PeerIdentity)obj;
	// Never nominate the poller
	if (pid == voterUserData.getPollerId()) {
	  return false;
	}
	try {
	  if (noAuSet != null && noAuSet.contains(pid)) {
	    return false;
	  }
	} catch (IOException e) {
	  log.warning("Couldn't chech NoAUSet", e);
	}
	PeerIdentityStatus status = idManager.getPeerIdentityStatus(pid);
	if (status == null) {
	  return false;
	}
	List hisGroups = status.getGroups();
	if (hisGroups == null || hisGroups.isEmpty()) {
	  return false;
	}
	List myGroups = ConfigManager.getPlatformGroupList();
	if (!CollectionUtils.containsAny(hisGroups, myGroups)) {
	  return false;
	}
	return true;
      }
      return false;
    }
  }

  /**
   * Compute the weight that a peer should be given for consideration for
   * nomination into the poll.
   *  
   * @param status
   * @return A double between 0.0 and 1.0 representing the invitation
   * weight that we want to give this peer.
   */
  double nominateWeight(PeerIdentity pid) {
    PeerIdentityStatus status = idManager.getPeerIdentityStatus(pid);
    CompoundLinearSlope nominationWeightCurve =
      pollManager.getNominationWeightAgeCurve();
    if (nominationWeightCurve  == null) {
      return 1.0;
    }
    long lastVoteTime = status.getLastVoterTime();
    long noVoteFor = TimeBase.nowMs() - lastVoteTime;
    return nominationWeightCurve.getY(noVoteFor);
  }

  /**
   * Create an array of byte arrays containing hasher initializer bytes for
   * this voter.  The result will be an array of 2 or 3 byte arrays:  The first
   * has no initializing bytes, and will be used for the plain hash.  The
   * second is constructed by concatenating the poller nonce and voter nonce,
   * and will be used for the challenge hash. If the voter is requesting a
   * symmetric poll the third will be the concatenation of the poller hash
   * and the second voter hash.
   *
   * @return Block hasher initialization bytes.
   */
  private byte[][] initHasherByteArrays() {
    byte[][] hasherByteArrays = new byte[hasherSize()][];
    hasherByteArrays[PLAIN_HASH] = ByteArray.EMPTY_BYTE_ARRAY;
    hasherByteArrays[CHALLENGE_HASH] =
      ByteArray.concat(voterUserData.getPollerNonce(),
		       voterUserData.getVoterNonce());
    
    if (voterUserData.isSymmetricPoll()) {
      hasherByteArrays[SYMMETRIC_HASH] =
        ByteArray.concat(voterUserData.getPollerNonce(),
                         voterUserData.getVoterNonce2());
    }
    return hasherByteArrays;
  }

  /**
   * Create the message digesters for this voter's hasher -- one for
   * the plain hash, one for the challenge hash, and if necessary one for
   * the symmetric poll.
   *
   * @return An array of MessageDigest objects to be used by the BlockHasher.
   */
  private MessageDigest[] initHasherDigests() {
    return PollUtil.createMessageDigestArray(hasherSize(), getHashAlgorithm());
  }

  private int hasherSize() {
    return voterUserData.isSymmetricPoll() ? 3 : 2;
  }

  private String getHashAlgorithm() {
    return voterUserData.getHashAlgorithm();
  }

  /**
   * Schedule a hash.
   */
  boolean generateVote() {
    log.debug("Scheduling vote hash for poll " + voterUserData.getPollKey());
    if (isSampledPoll()) {
      log.debug("Vote in sampled poll: "+inclusionPolicy.typeString());
    }
    BlockHasher hasher = makeHasher(voterUserData.getCachedUrlSet(),
				    -1	// XXX
				    );
    HashService hashService = theDaemon.getHashService();
    Deadline hashDeadline = task.getLatestFinish();

    // Cancel the old task.
    task.cancel();

    boolean scheduled = false;
    try {
      // Schedule the hash using the old task's latest finish as the deadline.
      scheduled =
	hashService.scheduleHash(hasher, hashDeadline,
				 new HashingCompleteCallback(), null);
    } catch (IllegalArgumentException e) {
      log.error("Error scheduling hash time", e);
    }
    if (scheduled) {
      log.debug("Successfully scheduled time for vote in poll " +
		getKey());
    } else {
      log.debug("Unable to schedule time for vote.  Dropping " +
                "out of poll " + getKey());
    }
    return scheduled;
  }

  BlockHasher makeHasher(CachedUrlSet cus, int maxVersions) {
    BlockHasher hasher = isSampledPoll() ?
      new SampledBlockHasher(cus,
			     maxVersions,
			     initHasherDigests(),
			     initHasherByteArrays(),
			     new BlockEventHandler(),
			     inclusionPolicy) :
      new BlockHasher(cus,
		      maxVersions,
		      initHasherDigests(),
		      initHasherByteArrays(),
		      new BlockEventHandler());
    if (subChecker != null) {
      hasher.setSubstanceChecker(subChecker);
    }
    if (CurrentConfig.getBooleanParam(V3Poller.PARAM_V3_EXCLUDE_SUSPECT_VERSIONS,
				      V3Poller.DEFAULT_V3_EXCLUDE_SUSPECT_VERSIONS)) {
      hasher.setExcludeSuspectVersions(true);
    }
    return hasher;
  }

  /**
   * Called by the HashService callback when hashing for this AU is
   * complete.
   */
  public void hashComplete() {
    // The task should have been canceled by now if the poll ended before
    // hashing was complete, but it may not have been.
    if (isPollCompleted()) {
      log.debug("HashService callback called hashComplete() on a poll " +
      		"that was over.  Poll key = " + getKey());
      return;
    }
    
    // If we've received a vote request, send our vote right away.  Otherwise,
    // wait for a vote request.
    log.debug("Hashing complete for poll " + voterUserData.getPollKey());
    String errmsg = "State machine error";
    stateMachine.enqueueEvent(V3Events.evtHashingDone,
			      ehAbortPoll(errmsg));
  }

  /*
   * Append the results of the block hasher to the VoteBlocks for this
   * voter.
   *
   * Called by the BlockHasher's event handler callback when hashing is complete
   * for one block.
   */
  public void blockHashComplete(HashBlock block) {
    // Add each hash block version to this vote block.
    try {
      voterUserData.getVoteBlocks().
	addVoteBlock(makeVoteBlock(block, CHALLENGE_HASH));
      if (voterUserData.isSymmetricPoll()) {
	voterUserData.getSymmetricVoteBlocks().
	  addVoteBlock(makeVoteBlock(block, SYMMETRIC_HASH));
      }
    } catch (IOException ex) {
      log.error("Unexpected IO Exception trying to add vote block " +
                block.getUrl() + " in poll " + getKey(), ex);
      if (++blockErrorCount > maxBlockErrorCount) {
        log.critical("Too many errors while trying to create my vote blocks, " +
                     "aborting participation in poll " + getKey());
        abortPollWithError();
      }
    }
  }

  /**
   * Make a VoteBlock with one version per version in the HashBlock.
   * Use the hash with the voter nonce or the symmetric nonce, as
   * requested.
   */
  private VoteBlock makeVoteBlock(HashBlock block, int hashIndex) {
    VoteBlock vb = new VoteBlock(block.getUrl());
    Iterator<HashBlock.Version> hashVersionIter = block.versionIterator();
    while (hashVersionIter.hasNext()) {
      HashBlock.Version ver = hashVersionIter.next();
      byte[] plainHash = ver.getHashes()[PLAIN_HASH];
      byte[] otherHash = ver.getHashes()[hashIndex];
      vb.addVersion(ver.getFilteredOffset(),
                    ver.getFilteredLength(),
                    ver.getUnfilteredOffset(),
                    ver.getUnfilteredLength(),
                    plainHash,
                    otherHash,
                    ver.getHashError() != null);
    }
    return vb;
  }

  public void setMessage(LcapMessage msg) {
    voterUserData.setPollMessage(msg);
  }

  public long getCreateTime() {
    return voterUserData.getCreateTime();
  }

  public long getHashStartTime() {
    if (task != null) {
      return task.getEarliestStart().getExpirationTime();
    } else {
      return 0;
    }
  }

  public PeerIdentity getCallerID() {
    return voterUserData.getPollerId();
  }
  
  public File getStateDir() {
    if (pollSerializer != null) {
      return pollSerializer.pollDir;
    }
    return null;
  }

  // Not used by V3.
  protected boolean isErrorState() {
    return false;
  }

  // Not used by V3.
  public boolean isMyPoll() {
    // Always return false
    return false;
  }

  public PollSpec getPollSpec() {
    return voterUserData.getPollSpec();
  }

  public CachedUrlSet getCachedUrlSet() {
    return voterUserData.getCachedUrlSet();
  }

  public int getVersion() {
    return voterUserData.getPollVersion();
  }

  public LcapMessage getMessage() {
    return voterUserData.getPollMessage();
  }

  public String getKey() {
    return voterUserData.getPollKey();
  }

  public Deadline getDeadline() {
    return Deadline.restoreDeadlineAt(voterUserData.getDeadline());
  }
  
  public Deadline getVoteDeadline() {
    return Deadline.restoreDeadlineAt(voterUserData.getVoteDeadline());
  }

  public long getDuration() {
    return voterUserData.getDuration();
  }

  public byte[] getPollerNonce() {
    return voterUserData.getPollerNonce();
  }

  public byte[] getVoterNonce() {
    return voterUserData.getVoterNonce();
  }

  public PollTally getVoteTally() {
    throw new UnsupportedOperationException("V3Voter does not have a tally.");
  }

  private class HashingCompleteCallback implements HashService.Callback {
    /**
     * Called when the timer expires or hashing is complete.
     *
     * @param cookie data supplied by caller to schedule()
     */
    public void hashingFinished(CachedUrlSet cus, long timeUsed, Object cookie,
                                CachedUrlSetHasher hasher, Exception e) {
      if (!isPollActive()) {
	log.warning("Hash finished after poll closed: " + getKey());
	return;
      }
      if (e == null) {
	switch (updateSubstance(subChecker)) {
	case No:
	  log.warning("No files containing substantial content found during hash; not voting");
	  V3LcapMessage msg = voterUserData.makeMessage(V3LcapMessage.MSG_VOTE);
	  msg.setVoterNonce(null);
	  msg.setNak(PollNak.NAK_NO_SUBSTANCE);
	  try {
	    sendMessageTo(msg, getPollerId());
	  } catch (IOException ex) {
	    log.error("Unable to send message in poll " + getKey() + ": " +
		      msg, ex);
	  }
	  stopPoll(STATUS_NO_SUBSTANCE);
	  break;
	  default:
	}
	if (hasher instanceof BlockHasher && !isSampledPoll()) {
	  LocalHashResult lhr = ((BlockHasher)hasher).getLocalHashResult();
	  log.debug2("Recording local hash result: " + lhr);
	  idManager.signalLocalHashComplete(lhr);
	}
	hashComplete();
      } else {
        if (e instanceof SchedService.Timeout) {
          log.warning("Hash deadline passed before the hash was finished.");
	  sendNak(V3LcapMessage.PollNak.NAK_HASH_TIMEOUT);
          stopPoll(STATUS_EXPIRED);
        } else {
          log.warning("Hash failed: " + e.getMessage(), e);
          voterUserData.setErrorDetail(e.getMessage());
	  sendNak(V3LcapMessage.PollNak.NAK_HASH_ERROR);
          abortPollWithError();
        }
      }
    }
  }

  SubstanceChecker.State updateSubstance(SubstanceChecker sc) {
    AuState aus = AuUtil.getAuState(getAu());
    if (sc != null) {
      SubstanceChecker.State oldSub = aus.getSubstanceState();
      SubstanceChecker.State newSub = sc.hasSubstance();
      if (newSub != oldSub && log.isDebug2()) {
	log.debug2("Change substance state: " + oldSub + " => " + newSub);
      }
      // update AuState unconditionally to record possible FeatureVersion change
      aus.setSubstanceState(newSub);
      switch (newSub) {
      case No:
	if (oldSub != SubstanceChecker.State.No) {
	  // Alert on transition to no substance
	  String msg =
	    "AU has no files containing substantial content; not voting.";
	  pollManager.raiseAlert(Alert.auAlert(Alert.CRAWL_NO_SUBSTANCE,
					       getAu()),
				 msg);
	}
      }
    }
    return aus.getSubstanceState();
  }

  private class BlockEventHandler implements BlockHasher.EventHandler {
    public void blockStart(HashBlock block) { 
      log.debug2("Poll " + getKey() + ": Starting hash for block " 
                 + block.getUrl());
    }
    public void blockDone(HashBlock block) {
      if (!isPollActive()) return;

      log.debug2("Poll " + getKey() + ": Ending hash for block " 
                 + block.getUrl());
      blockHashComplete(block);
    }
  }

  public int getType() {
    return Poll.V3_POLL;
  }
  
  public LockssApp getLockssDaemon() {
    return theDaemon;
  }

  public ArchivalUnit getAu() {
    return voterUserData.getCachedUrlSet().getArchivalUnit();
  }

  public PeerIdentity getPollerId() {
    return voterUserData.getPollerId();
  }

  public PollManager getPollManager() {
    return pollManager;
  }

  public boolean isPollActive() {
    return voterUserData.isPollActive();
  }

  public boolean isPollCompleted() {
    return voterUserData.isPollCompleted();
  }

  public boolean isSampledPoll() {
    return inclusionPolicy != null;
  }

  public VoterUserData getVoterUserData() {
    return voterUserData;
  }
  
  SubstanceChecker getSubstanceChecker() {
    return subChecker;
  }
  
  public String getStatusString() {
    return V3Voter.STATUS_STRINGS[voterUserData.getStatus()];
  }
  
  public int getStatus() {
    return voterUserData.getStatus();
  }
  
  IdentityManager getIdentityManager() {
    return this.idManager;
  }

  boolean shouldSymmetricPoll(PeerIdentity pid) {
    double minWeight =
      CurrentConfig.getDoubleParam(PARAM_MIN_WEIGHT_SYMMETRIC_POLL,
				   DEFAULT_MIN_WEIGHT_SYMMETRIC_POLL);
    return weightSymmetricPoll(pid) >= minWeight;
  }

  double weightSymmetricPoll(PeerIdentity pid) {
    if (CurrentConfig.getBooleanParam(PARAM_ALL_SYMMETRIC_POLLS,
				      DEFAULT_ALL_SYMMETRIC_POLLS)) {
      return 1.0;
    }
    if (!CurrentConfig.getBooleanParam(PARAM_ENABLE_SYMMETRIC_POLLS,
				      DEFAULT_ENABLE_SYMMETRIC_POLLS)) {
      return 0.0;
    }
    double highest = idManager.getHighestPercentAgreement(pid, getAu());
    if (highest >= pollManager.getMinPercentForRepair()) {
      // Poller is already a willing repairer
      return 0.0;
    }
    // See http://wiki.lockss.org/cgi-bin/wiki.pl?Mellon/SymmetricPolls
    // for discussion of symmetric poll costs and policies. Initial
    // decision is to avoid probabilistic policy here.
    return 1.0;
  }

  /**
   * Checkpoint the current state of the voter.
   */
  void checkpointPoll() {
    // This is sometimes the case during testing.
    if (pollSerializer == null) return;
    try {
      if (subChecker != null) {
	voterUserData.setSubstanceCheckerState(subChecker.hasSubstance());
      }
      pollSerializer.saveVoterUserData(voterUserData);
    } catch (PollSerializerException ex) {
      log.warning("Unable to save voter state.");
    }
  }

  private class PollTimerCallback implements TimerQueue.Callback {
    /**
     * Called when the timer for this poll expires.
     *
     * @param cookie data supplied by caller to schedule()
     */
    public void timerExpired(Object cookie) {
      stopPoll();
    }
    
    public String toString() {
      return "V3 Voter " + getKey();
    }
  }
  
  /**
   * Release unneeded resources.
   */
  // Do not set pollManager or theDaemon to null; it doesn't accomplish
  // anything (they're not GCable) and they may get referenced
  public void release() {
    if (task != null) task.cancel();
    voterUserData.release();
    stateDir = null;
    task = null;
    idManager = null;
    pollSerializer = null;
    stateMachine = null;
  }

  protected PsmMachine makeMachine() {
    try {
      PsmMachine.Factory fact = VoterStateMachineFactory.class.newInstance();
      return fact.getMachine(getVoterActionsClass());
    } catch (Exception e) {
      String msg = "Can't create voter state machine";
      log.critical(msg, e);
      throw new RuntimeException(msg, e);
    }
  }

}
