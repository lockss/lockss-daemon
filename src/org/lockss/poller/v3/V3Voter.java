/*
 * $Id: V3Voter.java,v 1.17.6.1 2006-08-03 01:19:20 smorabito Exp $
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
import java.security.*;
import java.util.*;

import org.lockss.app.LockssDaemon;
import org.lockss.config.*;
import org.lockss.daemon.CachedUrlSetHasher;
import org.lockss.hasher.*;
import org.lockss.plugin.*;
import org.lockss.poller.*;
import org.lockss.poller.v3.V3Serializer.PollSerializerException;
import org.lockss.protocol.*;
import org.lockss.protocol.psm.*;
import org.lockss.util.*;

/**
 * Represents a voter in a V3 poll.
 *
 * State is maintained in a V3VoterState object.  On the voter's side
 * of a poll, this object is transient.
 */
public class V3Voter extends BasePoll {

  static String PREFIX = Configuration.PREFIX + "poll.v3.";

  /** The minimum number of peers to select for a nomination message.
   * If there are fewer than this number of peers available to nominate,
   * an empty nomination message will be sent. */
  public static String PARAM_MIN_NOMINATION_SIZE = PREFIX + "minNominationSize";
  public static int DEFAULT_MIN_NOMINATION_SIZE = 1;

  /** The minimum number of peers to select for a nomination message. */
  public static String PARAM_MAX_NOMINATION_SIZE = 
    PREFIX + "maxNominationSize";
  public static int DEFAULT_MAX_NOMINATION_SIZE = 5;
  
  /**
   * Directory in which to store message data.
   */
  public static String PARAM_V3_MESSAGE_DIR = V3Poller.PARAM_V3_MESSAGE_DIR;
  public static String DEFAULT_V3_MESSAGE_DIR = 
    V3Poller.DEFAULT_V3_MESSAGE_DIR;

  private PsmInterp stateMachine;
  private VoterUserData voterUserData;
  private LockssRandom theRandom = new LockssRandom();
  private LockssDaemon theDaemon;
  private V3VoterSerializer pollSerializer;
  private PollManager pollManager;
  private IdentityManager idManager;
  private boolean continuedPoll = false;
  private boolean activePoll = true;
  private int nomineeCount;
  private File messageDir;

  private static final Logger log = Logger.getLogger("V3Voter");

  /**
   * Create a new V3Voter to participate in a poll.
   */
  public V3Voter(PollSpec spec, LockssDaemon daemon, PeerIdentity orig,
                 String key, byte[] introEffortProof, byte[] pollerNonce,
                 long duration, String hashAlg)
      throws V3Serializer.PollSerializerException {
    log.debug3("Creating V3 Voter for poll: " + key);

    
    this.theDaemon = daemon;
    pollSerializer = new V3VoterSerializer(theDaemon);
    this.messageDir =
      new File(CurrentConfig.getParam(PARAM_V3_MESSAGE_DIR,
                                      DEFAULT_V3_MESSAGE_DIR));
    if (!messageDir.exists() || !messageDir.canWrite()) {
      throw new IllegalArgumentException("Configured V3 data directory " +
                                         messageDir +
                                         " does not exist or cannot be " +
                                         "written to.");
    }
    this.voterUserData = new VoterUserData(spec, this, orig, key,
                                           duration, hashAlg,
                                           pollerNonce,
                                           makeVoterNonce(),
                                           introEffortProof,
                                           messageDir);
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
    log.debug2("Will choose " + nomineeCount +
    " outer circle nominees to send to poller");
    stateMachine = makeStateMachine(voterUserData);
    checkpointPoll();
  }

  /**
   * Restore a V3Voter from a previously saved poll.
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

  public void startPoll() {
    log.debug("Starting poll " + voterUserData.getPollKey());
    Deadline pollDeadline = null;
    if (!continuedPoll) {
      // Skip sanity check
      pollDeadline = Deadline.at(voterUserData.getDeadline());
    } else {
      pollDeadline = Deadline.restoreDeadlineAt(voterUserData.getDeadline());
    }
    
    // If this poll has already expired, don't start it.
    if (pollDeadline.expired()) {
      log.info("Not restoring expired voter for poll " +
               voterUserData.getPollKey());
      stopPoll();
      return;
    }

    TimerQueue.schedule(pollDeadline, new PollTimerCallback(), this);
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

  public void stopPoll() {
    // XXX: Set proper status string (Complete / Sent Repair / ?)
    voterUserData.setStatusString("Complete");
    activePoll = false;
    pollSerializer.closePoll();
    pollManager.closeThePoll(voterUserData.getPollKey());
    log.debug2("Closed poll " + voterUserData.getPollKey());
  }

  private void abortPoll() {
    stopPoll();
    voterUserData.setStatusString("Error");
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
    return hashService.scheduleHash(hasher,
                                    Deadline.at(voterUserData.getDeadline()),
                                    new HashingCompleteCallback(), null);
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
                    challengeDigest);
    }
    
    // Add this vote block to our hash block container.
    VoteBlocks blocks = voterUserData.getVoteBlocks();
    try {
      blocks.addVoteBlock(vb);
    } catch (IOException ex) {
      log.critical("Unexpected IO Exception trying to add vote block.  " +
                   "Aborting our participation.", ex);
      abortPoll();
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
    public void blockStart(HashBlock block) { /* do nothing */ }
    public void blockDone(HashBlock block) {
      blockHashComplete(block);
    }
  }

  public int getType() {
    return Poll.V3_POLL;
  }

  public String getStatusString() {
    return voterUserData.getStatusString();
  }

  public ArchivalUnit getAu() {
    return voterUserData.getCachedUrlSet().getArchivalUnit();
  }

  public PeerIdentity getPollerId() {
    return voterUserData.getPollerId();
  }

  public boolean isPollActive() {
    return activePoll;
  }

  public boolean isPollCompleted() {
    return !activePoll;
  }

  public VoterUserData getVoterUserData() {
    return voterUserData;
  }

  /**
   * Checkpoint the current state of the voter.
   */
  private void checkpointPoll() {
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
  }

}
