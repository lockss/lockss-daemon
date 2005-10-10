/*
 * $Id: V3Voter.java,v 1.4 2005-10-10 16:57:22 troberts Exp $
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

import java.io.IOException;
import java.security.*;
import java.util.*;

import org.lockss.app.*;
import org.lockss.config.*;
import org.lockss.daemon.*;
import org.lockss.hasher.*;
import org.lockss.plugin.*;
import org.lockss.poller.*;
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
  public static int DEFAULT_MIN_NOMINATION_SIZE = 0;

  /** The minimum number of peers to select for a nomination message. */
  public static String PARAM_MAX_NOMINATION_SIZE = PREFIX + "maxNominationSize";
  public static int DEFAULT_MAX_NOMINATION_SIZE = 5;
  
  private V3VoterInterp stateMachine;
  private VoterUserData voterUserData;
  private LockssDaemon theDaemon;
  private V3VoterSerializer pollSerializer;
  private PollManager pollManager;
  private IdentityManager idManager;
  private boolean continuedPoll = false;
  private int nomineeCount;
  
  private PollTally tally; // XXX: Refactor
  
  private static final LockssRandom theRandom = new LockssRandom();

  private static final Logger log = Logger.getLogger("V3Voter");

  /**
   * Create a new V3Voter.
   */
  public V3Voter(PollSpec spec, LockssDaemon daemon, PeerIdentity orig,
                 String key, byte[] introEffortProof, byte[] pollerNonce,
                 long duration, String hashAlg)
      throws V3Serializer.PollSerializerException {
    log.debug3("Creating V3 Voter for poll: " + key);
    pollSerializer = new V3VoterSerializer();
    this.voterUserData = new VoterUserData(spec, this, orig, key,
                                           duration, hashAlg,
                                           pollerNonce,
                                           makeVoterNonce(),
                                           introEffortProof,
                                           pollSerializer);
    this.theDaemon = daemon;
    this.idManager = theDaemon.getIdentityManager();
    this.pollManager = daemon.getPollManager();
    this.tally = new MockTally(Poll.V3_POLL, TimeBase.nowMs(), duration,
                               0, 0, 0, 0, 0, hashAlg, this);
    
    int min = Configuration.getIntParam(PARAM_MIN_NOMINATION_SIZE,
                                        DEFAULT_MIN_NOMINATION_SIZE);
    int max = Configuration.getIntParam(PARAM_MAX_NOMINATION_SIZE,
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
  }
  
  /**
   * Restore a V3Voter from a previously saved poll.
   */
  public V3Voter(LockssDaemon daemon, String pollDir)
      throws V3Serializer.PollSerializerException {
    pollSerializer = new V3VoterSerializer(pollDir);
    this.voterUserData = pollSerializer.loadVoterUserData();
    this.theDaemon = daemon;
    this.pollManager = daemon.getPollManager();
    this.continuedPoll = true;
    // Restore transient state.
    PluginManager plugMgr = theDaemon.getPluginManager();
    CachedUrlSet cus = plugMgr.findCachedUrlSet(voterUserData.getAuId());
    if (cus == null) {
      throw new NullPointerException("CUS for AU " + voterUserData.getAuId() +
                                     " is null!");
    }
    voterUserData.setCachedUrlSet(cus);
    voterUserData.setSerializer(pollSerializer);
    voterUserData.setVoter(this);
  }

  public void startPoll() {
    log.debug("Starting poll " + voterUserData.getPollKey());
    PsmMachine machine = VoterStateMachineFactory.
      getMachine(getVoterActionsClass());
    if (continuedPoll) {
      try {
        stateMachine = new V3VoterInterp(machine, voterUserData,
                                         pollSerializer.loadVoterInterpState(),
                                         pollSerializer);
      } catch (V3Serializer.PollSerializerException ex) {
        log.error("Unable to restore poll state!");
        stopPoll();
        return;
      }
    } else {
      stateMachine = new V3VoterInterp(machine, voterUserData, pollSerializer);
    }
    stateMachine.init();
  }

  public void stopPoll() {
    log.debug("Stopping poll " + voterUserData.getPollKey());
    // XXX: Notify state machine to stop.
    pollSerializer.closePoll();
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
    int opcode = msg.getOpcode();
    switch(opcode) {
    case V3LcapMessage.MSG_POLL: // we won't actually see these, but for completeness...
    case V3LcapMessage.MSG_POLL_PROOF:
    case V3LcapMessage.MSG_VOTE_REQ:
    case V3LcapMessage.MSG_REPAIR_REQ:
    case V3LcapMessage.MSG_EVALUATION_RECEIPT:
      PsmMsgEvent evt = V3Events.fromMessage(msg);
      stateMachine.handleEvent(evt);
      return;
    default:
      log.debug2("Ignoring message: " + msg);
      return;
    }
  }
  
  /**
   * Generate a list of outer circle nominees.
   */
  public void nominatePeers() {
    // XXX:  'allPeers' must contain only peers that have agreed with
    //       us in the past for this au!  Change this signature to:
    //       getTcpPeerIdentities(ArchivalUnit au);
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
  }
  
  /**
   * Create an array of byte arrays containing hasher initializer bytes for
   * this voter.  The initializer bytes are constructed by concatenating the 
   * voter's poller nonce and voter nonce.
    * 
   * @return Block hasher initialization bytes.
   */
  private byte[][] initHasherByteArrays() {
    return new byte[][] {ByteArray.concat(voterUserData.getPollerNonce(),
                                          voterUserData.getVoterNonce())};
  }

  /**
   * Create the message digester for this voter's hasher.
   * 
   * @return An array of MessageDigest objects to be used by the BlockHasher.
   */
  private MessageDigest[] initHasherDigests() throws NoSuchAlgorithmException {
    String hashAlg = voterUserData.getHashAlgorithm();
    if (hashAlg == null) {
      hashAlg = LcapMessage.DEFAULT_HASH_ALGORITHM;
    }
    return new MessageDigest[] { MessageDigest.getInstance(hashAlg) };
  }
  
  /**
   * Schedule a hash.
   */
  boolean generateVote() throws NoSuchAlgorithmException {
    log.debug("Scheduling vote hash for poll " + voterUserData.getPollKey());
    CachedUrlSetHasher hasher = new BlockHasher(voterUserData.getCachedUrlSet(),
                                                initHasherDigests(),
                                                initHasherByteArrays(),
                                                new BlockCompleteHandler());  
    HashService hashService = theDaemon.getHashService();
    return hashService.scheduleHash(hasher,
                                    Deadline.at(voterUserData.getDeadline()),
                                    new HashingCompleteCallback(), null);
  }
  
  /**
   * Called by the HashService callback when hashing for this CU is
   * complete.
   * 
   * XXX:  Multi-message voting TBD.
   */
  public void hashComplete() {
    log.debug("Hashing complete for poll " + voterUserData.getPollKey());
    // If we've received a vote request, send our vote right away.  Otherwise,
    // wait for a vote request.
    voterUserData.hashingDone(true);
    if (voterUserData.voteRequested()) {
      stateMachine.handleEvent(V3Events.evtReadyToVote);
    } else {
      stateMachine.handleEvent(V3Events.evtWaitVoteRequest);
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
    VoteBlocks blocks = voterUserData.getVoteBlocks();
    MessageDigest digest = block.getDigests()[0];
    VoteBlock vb = new VoteBlock(block.getUrl(),
                                 block.getFilteredLength(),
                                 block.getFilteredOffset(),
                                 block.getUnfilteredLength(),
                                 block.getUnfilteredOffset(),
                                 digest.digest(),
                                 VoteBlock.CONTENT_VOTE);
    blocks.addVoteBlock(vb);
  }

  public void setMessage(LcapMessage msg) {
    voterUserData.setPollMessage(msg);
  }

  public long getCreateTime() {
    // TODO Auto-generated method stub
    return 0;
  }

  public PeerIdentity getCallerID() {
    // TODO Auto-generated method stub
    return null;
  }

  protected boolean isErrorState() {
    // TODO Auto-generated method stub
    return false;
  }

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
    return Deadline.at(voterUserData.getDeadline());
  }

  public PollTally getVoteTally() {
    return tally;
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
        stopPoll();
      }
    }
  }

  private class BlockCompleteHandler implements BlockHasher.EventHandler {
    public void blockDone(HashBlock block) {
      blockHashComplete(block);
    }
  }
  
  // XXX - only used as a stepping stone to get V3Voter working properly with
  // the poll manager.  This should be refactored away in daemon 1.13.
  private static class MockTally extends PollTally {

    private BasePoll poll;

    public MockTally(int type, long startTime, long duration,
                     int numAgree, int numDisagree, int wtAgree, int wtDisagree,
                     int quorum, String hashAlgorithm, V3Voter poll) {
      super(type, startTime, duration, numAgree, numDisagree,
            wtAgree, wtDisagree, quorum, hashAlgorithm);
      this.poll = poll;
    }

    public int getErr() {
      return 0;
    }

    public String getErrString() {
      return "N/A";
    }

    public String getStatusString() {
      return "N/A";
    }

    public BasePoll getPoll() {
      return poll;
    }
    
    public PollSpec getPollSpec() {
      return poll.getPollSpec();
    }

    public void tallyVotes() {
      // do nothing
    }

    public boolean stateIsActive() {
      return false;
    }

    public boolean stateIsFinished() {
      return false;
    }

    public boolean stateIsSuspended() {
      return false;
    }

    public void setStateSuspended() {
      // do nothing
    }

    public void replayVoteCheck(Vote vote, Deadline deadline) {
      // do nothing
    }

    public void adjustReputation(PeerIdentity voterID, int repDelta) {
      // do nothing
    }

    public int getTallyResult() {
      return 0;
    }
    
  }
  
}
