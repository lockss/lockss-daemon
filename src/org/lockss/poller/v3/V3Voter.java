/*
 * $Id: V3Voter.java,v 1.2 2005-09-07 03:06:29 smorabito Exp $
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

import java.security.*;

import org.lockss.app.*;
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
public class V3Voter {

  private V3VoterInterp stateMachine;
  private VoterUserData voterUserData;
  private LockssDaemon theDaemon;
  private V3VoterSerializer pollSerializer;
  private PollManager pollManager;
  private boolean continuedPoll = false;
  
  private static final LockssRandom theRandom = new LockssRandom();

  private static final Logger log = Logger.getLogger("V3Voter");

  /**
   * Create a new V3Voter.
   */
  public V3Voter(PollSpec spec, LockssDaemon daemon, PeerIdentity orig,
                 String key, byte[] introEffortProof, byte[] pollerNonce,
                 long duration, String hashAlg)
      throws V3Serializer.PollSerializerException {
    pollSerializer = new V3VoterSerializer();
    this.voterUserData = new VoterUserData(spec, this, orig, key,
                                           duration, hashAlg,
                                           pollerNonce,
                                           makeVoterNonce(),
                                           introEffortProof,
                                           pollSerializer);
    this.theDaemon = daemon;
    this.pollManager = daemon.getPollManager();
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

  Class getVoterActionsClass() {
    return VoterActions.class;
  }

  /**
   * Send a message to the poller.
   */
  public void sendMessage(V3LcapMessage msg) {
    // XXX:  Implement.  Override in testing for now.
  }
  
  /**
   * Handle an incoming V3LcapMessage.
   */
  public void handleMessage(V3LcapMessage msg) {
    PsmMsgEvent evt = V3Events.fromMessage(msg);
    stateMachine.handleEvent(evt);
  }
  
  /**
   * Generate a list of outer circle nominees.
   */
  public void nominatePeers() {
    // XXX: Implement.  Override in testing for now.
  }
  
  /**
   * Create an array of byte arrays containing hasher initializer bytes for
   * this voter.  The initializer bytes are constructed by concatenating the 
   * voter's poller nonce and voter nonce.
    * 
   * @return Block hasher initialization bytes.
   */
  byte[][] initHasherByteArrays() {
    return new byte[][] {ByteArray.concat(voterUserData.getPollerNonce(),
                                          voterUserData.getVoterNonce())};
  }

  /**
   * Create the message digester for this voter's hasher.
   * 
   * @return An array of MessageDigest objects to be used by the BlockHasher.
   */
  MessageDigest[] initHasherDigests() throws NoSuchAlgorithmException {
    String hashAlg = voterUserData.getHashAlgorithm();
    if (hashAlg == null) {
      hashAlg = LcapMessage.DEFAULT_HASH_ALGORITHM;
    }
    return new MessageDigest[] { MessageDigest.getInstance(hashAlg) };
  }
  
  /**
   * Schedule a hash.
   */
  public boolean generateVote() throws NoSuchAlgorithmException {
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
  
  class HashingCompleteCallback implements HashService.Callback {
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

  class BlockCompleteHandler implements BlockHasher.EventHandler {
    public void blockDone(HashBlock block) {
      blockHashComplete(block);
    }
  }
  
}
