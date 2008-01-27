/*
 * $Id: ParticipantUserData.java,v 1.17 2008-01-27 06:46:04 tlipkis Exp $
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

import org.lockss.plugin.CachedUrlSet;
import org.lockss.protocol.*;
import org.lockss.protocol.V3LcapMessage.PollNak;
import org.lockss.protocol.psm.*;
import org.lockss.util.*;

import java.io.*;
import java.util.*;

/**
 * Persistent user data state object used by V3Poller state machine.
 */
public class ParticipantUserData implements LockssSerializable {

  private PeerIdentity voterId;
  private String hashAlgorithm;
  private VoteBlocks voteBlocks;
  private List nominees;
  private byte[] pollerNonce;
  private byte[] voterNonce;
  // XXX: Effort proofs may eventually not be byte arrays.
  private byte[] introEffortProof;
  private byte[] pollAckEffortProof;
  private byte[] remainingEffortProof;
  private byte[] repairEffortProof;
  private byte[] receiptEffortProof;
  private boolean isVoteComplete = false;
  private boolean isOuterCircle = false;
  private PsmInterpStateBean psmState;
  private int status = V3Poller.PEER_STATUS_INITIALIZED;
  private String statusMsg = null;
  private VoteBlocksIterator voteBlockIterator;
  /** The number of blocks that have been tallied for this peer */ 
  private long talliedUrls;
  /** The number of blocks that the poller agrees with for this peer */
  private long agreeUrls;
  /** The poll NAK code, if any */
  private PollNak pollNak;

  /** Transient non-serialized fields */
  private transient V3Poller poller;
  private transient PollerStateBean pollState;
  private transient PsmInterp psmInterp;
  private transient File messageDir;

  protected ParticipantUserData() {}

  /**
   * Construct a new PollerUserData object.
   *
   * @param id
   * @param poller
   */
  public ParticipantUserData(PeerIdentity id, V3Poller poller,
                             File messageDir) {
    this.voterId = id;
    this.poller = poller;
    this.pollState = poller.getPollerStateBean();
    this.messageDir = messageDir;
  }

  public void isOuterCircle(boolean b) {
    this.isOuterCircle = b;
  }

  public boolean isOuterCircle() {
    return isOuterCircle;
  }

  public PsmInterp getPsmInterp() {
    return psmInterp;
  }

  public void setPsmInterp(PsmInterp interp) {
    this.psmInterp = interp;
  }

  public PsmInterpStateBean getPsmInterpState() {
    return psmState;
  }

  public void setPsmInterpState(PsmInterpStateBean psmState) {
    this.psmState = psmState;
  }

  public void setVoterId(PeerIdentity id) {
    this.voterId = id;
  }

  public PeerIdentity getVoterId() {
    return voterId;
  }

  public void setNominees(List l) {
    this.nominees = l;
  }

  public List getNominees() {
    return nominees;
  }
  
  /**
   * @deprecated Use setStatus instead.
   * @param s The status of this peer.
   */
  public void setStatusString(String s) {
    // Not implemented
  }
  
  public String getStatusString() {
    String msg = V3Poller.PEER_STATUS_STRINGS[getStatus()];
    if (statusMsg != null) {
      return msg + " (" + statusMsg + ")";
    }
    return msg;
  }
  
  public void setStatus(int s) {
    this.status = s;
  }
  
  public void setStatus(int s, String msg) {
    setStatus(s);
    statusMsg = msg;
  }
  
  public int getStatus() {
    return status;
  }
  
  public void setHashAlgorithm(String s) {
    this.hashAlgorithm = s;
  }

  public String getHashAlgorithm() {
    return hashAlgorithm;
  }

  public byte[] getPollerNonce() {
    return pollerNonce;
  }

  public void setPollerNonce(byte[] pollerNonce) {
    this.pollerNonce = pollerNonce;
  }

  public byte[] getVoterNonce() {
    return voterNonce;
  }

  public void setVoterNonce(byte[] voterNonce) {
    this.voterNonce = voterNonce;
  }

  public void setIntroEffortProof(byte[] b) {
    this.introEffortProof = b;
  }

  public byte[] getIntroEffortProof() {
    return introEffortProof;
  }

  public void setRemainingEffortProof(byte[] b) {
    this.remainingEffortProof = b;
  }

  public byte[] getRemainingEffortProof() {
    return remainingEffortProof;
  }

  public void setPollAckEffortProof(byte[] b) {
    pollAckEffortProof = b;
  }

  public byte[] getPollAckEffortProof() {
    return pollAckEffortProof;
  }

  public void setRepairEffortProof(byte[] b) {
    repairEffortProof = b;
  }

  public byte[] getRepairEffortProof() {
    return repairEffortProof;
  }

  public void setReceiptEffortProof(byte[] b) {
    receiptEffortProof = b;
  }

  public byte[] getReceiptEffortProof() {
    return receiptEffortProof;
  }

  public void setVoteBlocks(VoteBlocks blocks) {
    voteBlocks = blocks;
  }

  public VoteBlocks getVoteBlocks() {
    return voteBlocks;
  }

  /**
   * @return true if this peer has agreed to participate in the poll.
   */
  public boolean isParticipating() {
    return (status >= V3Poller.PEER_STATUS_ACCEPTED_POLL);
  }
  /**
   * Return the vote block iterator for this peer.
   * @return the vote block iterator for this peer.
   */
  public VoteBlocksIterator getVoteBlockIterator() {
    if (voteBlockIterator == null && voteBlocks != null) {
      voteBlockIterator = voteBlocks.iterator();
    }
    return voteBlockIterator;
  }

  public String toString() {
    return "[PollerUserData: voterId=" +
      voterId + "]";
  }

  public String getKey() {
    return poller.getKey();
  }

  public V3Poller getPoller() {
    return poller;
  }

  /**
   * Return true false this peer should be asked for another vote block.
   *
   * @return False if this peer should be asked for another vote block.
   */
  public boolean isVoteComplete() {
    return isVoteComplete;
  }

  public void setVoteComplete(boolean b) {
    this.isVoteComplete = b;
  }

  /** Poller State delegate methods */
  public String getAuId() {
    return pollState.getAuId();
  }

  public CachedUrlSet getCachedUrlSet() {
    return pollState.getCachedUrlSet();
  }

  public long getDeadline() {
    return pollState.getPollDeadline();
  }

  public String getLastHashedBlock() {
    return pollState.getLastHashedBlock();
  }

  public String getPluginVersion() {
    return pollState.getPluginVersion();
  }

  public PeerIdentity getPollerId() {
    return pollState.getPollerId();
  }

  public int getPollVersion() {
    return pollState.getProtocolVersion();
  }

  public String getUrl() {
    return pollState.getUrl();
  }

  public int hashCode() {
    return pollState.hashCode();
  }

  public void setPoller(V3Poller poller) {
    this.poller = poller;
  }

  public void setPollState(PollerStateBean state) {
    this.pollState = state;
  }
 
  public void incrementAgreedBlocks() {
    this.agreeUrls++;
  }
  
  public void incrementTalliedBlocks() {
    this.talliedUrls++;
  }
  
  /**
   * Return the total number of URLs tallied for this peer.
   * @return The number of URLs tallied.
   */
  public long getNumTalliedUrls() {
    return talliedUrls;
  }
  
  /**
   * Return the total number of agreeing URLs for this peer.
   * @return  The number of agreeing URLs
   */
  public long getNumAgreeUrls() {
    return agreeUrls;
  }
  
  /**
   * Return the total number of disagreeing URLs for this peer.
   * 
   * @return The number of disagreeing URLs
   */
  public long getNumDisagreeUrls() {
    return talliedUrls - agreeUrls;
  }
  
  /** 
   * Return the percent agreement for this peer.
   * 
   * @return The percent agreement for this peer (a number between 0.0 and 1.0)
   */
  private static Logger log = Logger.getLogger("V3Poller");
  public float getPercentAgreement() {
    log.debug2("[getPercentAgreement] agreeUrls = "
               + agreeUrls + "; talliedUrls = " + talliedUrls);
    if (agreeUrls > 0 && talliedUrls > 0) {
      return (float)agreeUrls / (float)talliedUrls;
    } else {
      return 0.0f;
    }
  }

  public PollNak getPollNak() {
    return pollNak;
  }

  public void setPollNak(PollNak pollNak) {
    this.pollNak = pollNak;
  }

  /** Poller delegate methods */
  void sendMessageTo(V3LcapMessage msg, PeerIdentity to) throws IOException {
    poller.sendMessageTo(msg, to);
  }

  void removeParticipant() {
    poller.removeParticipant(voterId);
  }

  /*
   * Callbacks methods.
   */
  void nominatePeers(List peers) {
    poller.nominatePeers(getVoterId(), peers);
  }

  void tallyVoter() {
    poller.tallyVoter(getVoterId());
  }

  /*
   * Implementation of V3LcapMessage.Factory
   */
  public V3LcapMessage makeMessage(int opcode) {
    return new V3LcapMessage(getAuId(), getKey(), getPluginVersion(),
                             getPollerNonce(), getVoterNonce(), opcode,
                             getDeadline(), getPollerId(), messageDir,
                             poller.getLockssDaemon());
  }
  
  /**
   * Release unneeded resources used by this object at the end of a poll.
   */
  public void release() {
    introEffortProof = null;
    pollAckEffortProof = null;
    receiptEffortProof = null;
    remainingEffortProof = null;
    receiptEffortProof = null;
    
    hashAlgorithm = null;
    messageDir = null;
    nominees = null;
    voteBlocks = null;
    voteBlockIterator = null;

    psmInterp = null;
    psmState = null;
  }

}
