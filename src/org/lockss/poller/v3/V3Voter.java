/*
 * $Id: V3Voter.java,v 1.38 2007-05-09 10:34:10 smorabito Exp $
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

package org.lockss.poller.v3;

import java.io.*;
import java.net.MalformedURLException;
import java.security.*;
import java.util.*;

import org.lockss.app.*;
import org.lockss.config.*;
import org.lockss.daemon.CachedUrlSetHasher;
import org.lockss.hasher.*;
import org.lockss.plugin.*;
import org.lockss.poller.*;
import org.lockss.poller.v3.V3Serializer.PollSerializerException;
import org.lockss.protocol.*;
import org.lockss.protocol.psm.*;
import org.lockss.repository.RepositoryNode;
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
  
  public static final String[] STATUS_STRINGS = 
  {
   "Initialized", "Accepted Poll", "Hashing", "Voted",
   "No Time Available", "Complete", "Expired w/o Voting", "Error",
   "Declined Poll"
  };

  static String PREFIX = Configuration.PREFIX + "poll.v3.";

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
   * If false, do not serve any repairs via V3.
   */
  public static final String PARAM_ALLOW_V3_REPAIRS =
    PREFIX + "allowV3Repairs";
  public static final boolean DEFAULT_ALLOW_V3_REPAIRS = true;
  
  /**
   * Directory in which to store message data.
   */
  public static final String PARAM_V3_MESSAGE_REL_DIR =
    V3Poller.PARAM_V3_MESSAGE_REL_DIR;
  public static final String DEFAULT_V3_MESSAGE_REL_DIR = 
    V3Poller.DEFAULT_V3_MESSAGE_REL_DIR;
  
  /** 
   * Extra time added to the poll deadline (as sent by the poller) to 
   * wait for a receipt message.
   */
  public static final String PARAM_RECEIPT_PADDING = PREFIX + "receiptPadding";
  public static final long DEFAULT_RECEIPT_PADDING = 1000 * 60 * 10; // 10m

  private PsmInterp stateMachine;
  private VoterUserData voterUserData;
  private LockssRandom theRandom = new LockssRandom();
  private LockssDaemon theDaemon;
  private V3VoterSerializer pollSerializer;
  private PollManager pollManager;
  private IdentityManager idManager;
  private boolean continuedPoll = false;
  private int nomineeCount;
  private File stateDir;
  private int blockErrorCount = 0;
  private int maxBlockErrorCount = V3Poller.DEFAULT_MAX_BLOCK_ERROR_COUNT;

  // Task used to reserve time for hashing at the start of the poll.
  // This task is cancelled before the real hash is scheduled.
  private SchedulableTask task;

  private static final Logger log = Logger.getLogger("V3Voter");

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
               "; duration=" + duration);

    pollSerializer = new V3VoterSerializer(theDaemon);
    
    // Determine the proper location for the V3 message dir.
    List dSpaceList =
      CurrentConfig.getList(ConfigManager.PARAM_PLATFORM_DISK_SPACE_LIST);
    String relPluginPath =
      CurrentConfig.getParam(PARAM_V3_MESSAGE_REL_DIR,
                             DEFAULT_V3_MESSAGE_REL_DIR);
    
    maxBlockErrorCount =
      CurrentConfig.getIntParam(V3Poller.PARAM_MAX_BLOCK_ERROR_COUNT,
                                V3Poller.DEFAULT_MAX_BLOCK_ERROR_COUNT);

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

    try {
      this.voterUserData = new VoterUserData(new PollSpec(msg), this,
                                             msg.getOriginatorId(), 
                                             msg.getKey(),
                                             duration,
                                             msg.getHashAlgorithm(),
                                             msg.getPollerNonce(),
                                             makeVoterNonce(),
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

    int min = CurrentConfig.getIntParam(PARAM_MIN_NOMINATION_SIZE,
                                        DEFAULT_MIN_NOMINATION_SIZE);
    int max = CurrentConfig.getIntParam(PARAM_MAX_NOMINATION_SIZE,
                                        DEFAULT_MAX_NOMINATION_SIZE);
    if (min > max) {
      throw new IllegalArgumentException("Impossible nomination size range: "
                                         + (max - min));
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
    stateMachine = makeStateMachine(voterUserData);
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
    this.pollSerializer = new V3VoterSerializer(theDaemon, pollDir);
    this.voterUserData = pollSerializer.loadVoterUserData();
    this.pollManager = daemon.getPollManager();
    this.continuedPoll = true;
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

    stateMachine = makeStateMachine(voterUserData);
  }

  private PsmInterp makeStateMachine(final VoterUserData ud) {
    PsmMachine machine =
      VoterStateMachineFactory.getMachine(getVoterActionsClass());
    PsmInterp interp = new PsmInterp(machine, ud);
    interp.setCheckpointer(new PsmInterp.Checkpointer() {
      public void checkpoint(PsmInterpStateBean resumeStateBean) {
        voterUserData.setPsmState(resumeStateBean);
        checkpointPoll();
      }
    });

    return interp;
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
    // XXX:  We need to reserve some padding for the estimated time it will
    // take to send the message.  'estimatedHashDuration' is probably way
    // too much for this, but a better estimate would require taking into 
    // account the number of URLs and versions that we expect to hash, since
    // the message size is proportional to the number of VoteBlock.Versions
    
    long voteDeadline = voterUserData.getVoteDeadline();
    long estimatedHashDuration = getCachedUrlSet().estimatedHashDuration();
    long now = TimeBase.nowMs();

    // Ensure the vote deadline has not already passed.
    if (voteDeadline < now) {
      log.warning("In poll " + getKey() + " called by peer "
                  + voterUserData.getPollerId()
                  + ", vote deadline (" + voteDeadline + ") has already "
                  + "passed!  Can't reserve schedule time.");
      return false;
    }
    
    if (estimatedHashDuration > (voteDeadline - now)) {
      log.warning("In poll " + getKey() + " called by peer " + 
                  voterUserData.getPollerId() +
                  ", my estimated hash duration (" + estimatedHashDuration + 
                  "ms) is too long to complete within the voting period (" +
                  (voteDeadline - now) + "ms)");
      return false;
    }

    Deadline earliestStart = Deadline.at(now + estimatedHashDuration);
    Deadline latestFinish =
      Deadline.at(voterUserData.getVoteDeadline() - estimatedHashDuration);
    log.debug("Voter " + getKey() + ": Earliest Start = " +
              earliestStart + "; Latest Finish = " + latestFinish);
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
      String msg = "No time for V3 Voter in poll " + getKey() + ". " +
                   " Requested time for step task with earliest start at " +
                   earliestStart +", latest finish at " + latestFinish + ", " +
                   "with an estimated hash duration of " + estimatedHashDuration +
                   "ms as of " + TimeBase.nowDate();
      voterUserData.setErrorDetail(msg);
      log.warning(msg);
    }
    return suc;
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
      long estimatedHashTime = getCachedUrlSet().estimatedHashDuration();
      long voteDeadline = voterUserData.getVoteDeadline();
      if (voteDeadline >= pollDeadline.getExpirationTime()) {
        log.warning("Voting deadline (" + voteDeadline + ") is later than " +
                    "the poll deadline (" + pollDeadline.getExpirationTime() + 
                    ").  Can't participate in poll " + getKey());
        stopPoll(STATUS_EXPIRED);
        return;
      }
      log.debug("Found enough time to participate in poll " + getKey());
    } else {
      log.warning("Not enough time found to participate in poll " + getKey());
      stopPoll(STATUS_NO_TIME);
      return;
    }

    // Register a callback for the end of the poll.
    TimerQueue.schedule(pollDeadline, new PollTimerCallback(), this);

    // Register a callback for the end of the voting period.  We must have
    // voted by this time, or we can't participate.
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
          stopPoll(V3Voter.STATUS_NO_TIME);
        }
      }
    }, this);
    
    // Resume or start the state machine running.
    if (continuedPoll) {
      try {
	stateMachine.resume(voterUserData.getPsmState());
      } catch (PsmException e) {
	log.warning("Error resuming poll.", e);
	abortPoll();
      }
    } else {
      try {
	stateMachine.start();
      } catch (PsmException e) {
	log.warning("Error starting poll.", e);
	abortPoll();
      }
    }
  }

  /**
   * Stop the poll and tell the {@link PollManager} to let go of us.
   * 
   * @param status The final status code of the poll, for the status table.
   */
  public void stopPoll(final int status) {
    if (voterUserData.isPollActive()) {
      voterUserData.setActivePoll(false);
    } else {
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
  private void abortPoll() {
    stopPoll(STATUS_ERROR);
  }

  /**
   * Generate a random nonce.
   *
   * @return A random array of 20 bytes.
   */
  private byte[] makeVoterNonce() {
    byte[] secret = new byte[20];
    theRandom.nextBytes(secret);
    return secret;
  }

  private Class getVoterActionsClass() {
    return VoterActions.class;
  }

  /**
   * Send a message to the poller.
   */
  void sendMessageTo(V3LcapMessage msg, PeerIdentity id)
      throws IOException {
    pollManager.sendMessageTo(msg, id);
  }

  /**
   * Handle an incoming V3LcapMessage.
   */
  public void receiveMessage(LcapMessage message) {
    // It's quite possible to receive a message after we've decided
    // to close the poll, but before the PollManager knows we're closed.
    if (voterUserData.isPollCompleted()) return;

    V3LcapMessage msg = (V3LcapMessage)message;
    PeerIdentity sender = msg.getOriginatorId();
    PsmMsgEvent evt = V3Events.fromMessage(msg);
    log.debug3("Received message: " + message.getOpcodeString() + " " + message);
    try {
      stateMachine.handleEvent(evt);
    } catch (PsmException e) {
      log.warning("State machine error", e);
      abortPoll();
    }
    // Finally, clean up after the V3LcapMessage
    msg.delete();    
  }

  /**
   * Generate a list of outer circle nominees.
   */
  public void nominatePeers() {
    // XXX:  'allPeers' should probably contain only peers that have agreed with
    //       us in the past for this au.
    Collection allPeers = idManager.getTcpPeerIdentities();
    allPeers.remove(voterUserData.getPollerId()); // Never nominate the poller
    if (nomineeCount <= allPeers.size()) {
      Collection nominees =
        CollectionUtil.randomSelection(allPeers, nomineeCount);
      // VoterUserData expects the collection to be KEYS, not PeerIdentities.
      ArrayList nomineeStrings = new ArrayList();
      for (Iterator iter = nominees.iterator(); iter.hasNext(); ) {
        PeerIdentity id = (PeerIdentity)iter.next();
        nomineeStrings.add(id.getIdString());
      }
      voterUserData.setNominees(nomineeStrings);
      log.debug2("Nominating the following peers: " + nomineeStrings);
    } else {
      log.warning("Not enough peers to nominate.  Need " + nomineeCount +
                  ", only know about " + allPeers.size());
    }
    checkpointPoll();
  }

  /**
   * Create an array of byte arrays containing hasher initializer bytes for
   * this voter.  The result will be an array of two byte arrays:  The first
   * has no initializing bytes, and will be used for the plain hash.  The
   * second is constructed by concatenating the poller nonce and voter nonce,
   * and will be used for the challenge hash.
   *
   * @return Block hasher initialization bytes.
   */
  private byte[][] initHasherByteArrays() {
    return new byte[][] {
        {}, // Plain Hash
        ByteArray.concat(voterUserData.getPollerNonce(),
                         voterUserData.getVoterNonce()) // Challenge Hash
    };
  }

  /**
   * Create the message digesters for this voter's hasher -- one for
   * the plain hash, one for the challenge hash.
   *
   * @return An array of MessageDigest objects to be used by the BlockHasher.
   */
  private MessageDigest[] initHasherDigests() throws NoSuchAlgorithmException {
    String hashAlg = voterUserData.getHashAlgorithm();
    if (hashAlg == null) {
      hashAlg = LcapMessage.DEFAULT_HASH_ALGORITHM;
    }
    return new MessageDigest[] {
        MessageDigest.getInstance(hashAlg),
        MessageDigest.getInstance(hashAlg)
    };
  }

  /**
   * Schedule a hash.
   */
  boolean generateVote() throws NoSuchAlgorithmException {
    log.debug("Scheduling vote hash for poll " + voterUserData.getPollKey());
    CachedUrlSetHasher hasher = new BlockHasher(voterUserData.getCachedUrlSet(),
                                                initHasherDigests(),
                                                initHasherByteArrays(),
                                                new BlockEventHandler());
    HashService hashService = theDaemon.getHashService();
    Deadline hashDeadline = task.getLatestFinish();

    // Cancel the old task.
    task.cancel();

    // Schedule the hash using the old task's latest finish as the deadline.
    boolean scheduled =
      hashService.scheduleHash(hasher, hashDeadline,
                               new HashingCompleteCallback(), null);
    if (scheduled) {
      log.debug("Successfully scheduled time for vote in poll " +
                getKey());
    } else {
      log.debug("Unable to schedule time for vote.  Dropping " +
                "out of poll " + getKey());
    }
    return scheduled;
  }

  /**
   * Called by the HashService callback when hashing for this CU is
   * complete.
   */
  public void hashComplete() {
    log.debug("Hashing complete for poll " + voterUserData.getPollKey());
    // If we've received a vote request, send our vote right away.  Otherwise,
    // wait for a vote request.
    voterUserData.hashingDone(true);
    try {
      if (voterUserData.voteRequested()) {
	stateMachine.handleEvent(V3Events.evtReadyToVote);
      } else {
	stateMachine.handleEvent(V3Events.evtWaitVoteRequest);
      }
    } catch (PsmException e) {
      log.warning("State machine error", e);
      abortPoll();
    }
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
    VoteBlock vb = new VoteBlock(block.getUrl());
    Iterator hashVersionIter = block.versionIterator();
    while(hashVersionIter.hasNext()) {
      HashBlock.Version ver = (HashBlock.Version)hashVersionIter.next();
      byte[] plainDigest = ver.getHashes()[0];
      byte[] challengeDigest = ver.getHashes()[1];
      vb.addVersion(ver.getFilteredOffset(),
                    ver.getFilteredLength(),
                    ver.getUnfilteredOffset(),
                    ver.getUnfilteredLength(),
                    plainDigest,
                    challengeDigest,
                    ver.getHashError() != null);
    }
    
    // Add this vote block to our hash block container.
    VoteBlocks blocks = voterUserData.getVoteBlocks();
    try {
      blocks.addVoteBlock(vb);
    } catch (IOException ex) {
      log.error("Unexpected IO Exception trying to add vote block " +
                vb.getUrl() + " in poll " + getKey(), ex);
      if (++blockErrorCount > maxBlockErrorCount) {
        log.critical("Too many errors while trying to create my vote blocks, " +
                     "aborting participation in poll " + getKey());
        abortPoll();
      }
    }
  }

  public void setMessage(LcapMessage msg) {
    voterUserData.setPollMessage(msg);
  }

  public long getCreateTime() {
    return voterUserData.getCreateTime();
  }

  public PeerIdentity getCallerID() {
    return voterUserData.getPollerId();
  }
  
  public File getStateDir() {
    return pollSerializer.pollDir;
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
    public void hashingFinished(CachedUrlSet cus, Object cookie,
                                CachedUrlSetHasher hasher, Exception e) {
      if (e == null) {
        hashComplete();
      } else {
        log.warning("Hash failed : " + e.getMessage(), e);
        abortPoll();
      }
    }
  }

  private class BlockEventHandler implements BlockHasher.EventHandler {
    public void blockStart(HashBlock block) { 
      log.debug2("Poll " + getKey() + ": Starting hash for block " 
                 + block.getUrl());
    }
    public void blockDone(HashBlock block) {
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

  public boolean isPollActive() {
    return voterUserData.isPollActive();
  }

  public boolean isPollCompleted() {
    return voterUserData.isPollCompleted();
  }

  public VoterUserData getVoterUserData() {
    return voterUserData;
  }
  
  public String getStatusString() {
    return V3Voter.STATUS_STRINGS[voterUserData.getStatus()];
  }
  
  IdentityManager getIdentityManager() {
    return this.idManager;
  }
  
  /**
   * Returns true if we will serve a repair to the given peer for the
   * given AU and URL.
   */
  boolean serveRepairs(PeerIdentity pid, ArchivalUnit au, String url) {
    try {
      RepositoryNode node = AuUtil.getRepositoryNode(au, url);
      boolean previousAgreement = node.hasAgreement(pid);
      if (previousAgreement) {
        log.debug("Previous agreement found for peer " + pid + " on URL "
                  + url);
      } else {
        log.debug("No previous agreement found for peer " + pid + " on URL "
                  + url);
      }
      boolean allowRepairs = 
        CurrentConfig.getBooleanParam(PARAM_ALLOW_V3_REPAIRS,
                                      DEFAULT_ALLOW_V3_REPAIRS);
      return(previousAgreement && allowRepairs);
    } catch (MalformedURLException ex) {
      // Log the error, but certainly don't serve the repair.
      log.error("serveRepairs: The URL " + url + " appears to be malformed. "
                + "Cannot serve repairs for this URL.");
      return false;
    }
  }

  /**
   * Checkpoint the current state of the voter.
   */
  void checkpointPoll() {
    // This is sometimes the case during testing.
    if (pollSerializer == null) return;
    try {
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
  public void release() {
    if (task != null) task.cancel();
    voterUserData.release();
    stateDir = null;
    task = null;
    pollManager = null;
    idManager = null;
    pollSerializer = null;
    stateMachine = null;
    theDaemon = null;
  }

}