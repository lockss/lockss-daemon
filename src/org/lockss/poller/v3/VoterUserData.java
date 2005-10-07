/*
 * $Id: VoterUserData.java,v 1.4 2005-10-07 16:19:56 thib_gc Exp $
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

import java.security.NoSuchAlgorithmException;
import java.util.*;

import org.lockss.plugin.*;
import org.lockss.poller.*;
import org.lockss.protocol.*;
import org.lockss.util.*;

/**
 * Persistent user data state object used by V3Voter state machine.
 */
public class VoterUserData implements LockssSerializable {
  
  private PeerIdentity pollerId;
  private String auId;
  private String pollKey;
  private int pollVersion;
  private String pluginVersion;
  private long deadline;
  private String hashAlgorithm;
  private VoteBlocks voteBlocks;
  private String url;
  private List nominees;
  private byte[] pollerNonce;
  private byte[] voterNonce;
  private byte[] introEffortProof;
  private byte[] pollAckEffortProof;
  private byte[] remainingEffortProof;
  private byte[] repairEffortProof;
  private byte[] receiptEffortProof;
  private boolean hashingDone = false;
  private boolean voteRequested = false;
  
  /** Transient non-serialized fields */
  private transient V3VoterSerializer serializer;
  private transient CachedUrlSet cus;
  private transient V3Voter voter;
  
  private static Logger log = Logger.getLogger("VoterUserData");
  
  /** Package-level constructor used in tests. */
  VoterUserData(V3VoterSerializer serializer) { 
    this.serializer = serializer;
  }
  
  public VoterUserData(PollSpec spec, V3Voter voter, PeerIdentity pollerId,
                       String pollKey, long duration, String hashAlgorithm,
                       byte[] pollerNonce, byte[] voterNonce,
                       byte[] introEffortProof, V3VoterSerializer serializer) 
      throws V3Serializer.PollSerializerException {
    this.url = spec.getUrl();
    this.cus = spec.getCachedUrlSet();
    this.pollVersion = spec.getPollVersion();
    this.pluginVersion = spec.getPluginVersion();
    this.voter = voter;
    this.pollerId = pollerId;
    this.pollKey = pollKey;
    this.deadline = Deadline.in(duration).getExpirationTime();
    this.hashAlgorithm = hashAlgorithm;
    this.voterNonce = voterNonce;
    this.pollerNonce = pollerNonce;
    this.introEffortProof = introEffortProof;
    this.serializer = serializer;
    this.voteBlocks = new MemoryVoteBlocks();
    saveState();
  }
  
  public String getAuId() {
    return auId;
  }

  public void setAuId(String auId) {
    this.auId = auId;
    saveState();
  }

  public CachedUrlSet getCachedUrlSet() {
    return cus;
  }

  public void setCachedUrlSet(CachedUrlSet cus) {
    this.cus = cus;
    // Transient - no need to save state.
  }

  public long getDeadline() {
    return deadline;
  }

  public void setDeadline(long deadline) {
    this.deadline = deadline;
    saveState();
  }

  public String getHashAlgorithm() {
    return hashAlgorithm;
  }

  public void setHashAlgorithm(String hashAlgorithm) {
    this.hashAlgorithm = hashAlgorithm;
    saveState();
  }

  public byte[] getIntroEffortProof() {
    return introEffortProof;
  }

  public void setIntroEffortProof(byte[] introEffortProof) {
    this.introEffortProof = introEffortProof;
    saveState();
  }

  public List getNominees() {
    return nominees;
  }

  public void setNominees(List nominees) {
    this.nominees = nominees;
    saveState();
  }

  public byte[] getPollAckEffortProof() {
    return pollAckEffortProof;
  }

  public void setPollAckEffortProof(byte[] pollAckEffortProof) {
    this.pollAckEffortProof = pollAckEffortProof;
    saveState();
  }
  
  public byte[] getReceiptEffortProof() {
    return receiptEffortProof;
  }

  public void setReceiptEffortProof(byte[] receiptEffortProof) {
    this.receiptEffortProof = receiptEffortProof;
    saveState();
  }

  public byte[] getRemainingEffortProof() {
    return remainingEffortProof;
  }

  public void setRemainingEffortProof(byte[] remainingEffortProof) {
    this.remainingEffortProof = remainingEffortProof;
    saveState();
  }

  public byte[] getRepairEffortProof() {
    return repairEffortProof;
  }

  public void setRepairEffortProof(byte[] repairEffortProof) {
    this.repairEffortProof = repairEffortProof;
    saveState();
  }

  public String getPluginVersion() {
    return pluginVersion;
  }

  public void setPluginVersion(String pluginVersion) {
    this.pluginVersion = pluginVersion;
    saveState();
  }

  public int getPollVersion() {
    return pollVersion;
  }

  public void setPollVersion(int pollVersion) {
    this.pollVersion = pollVersion;
    saveState();
  }

  public PeerIdentity getPollerId() {
    return pollerId;
  }

  public void setPollerId(PeerIdentity pollerId) {
    this.pollerId = pollerId;
    saveState();
  }

  public byte[] getPollerNonce() {
    return pollerNonce;
  }

  public void setPollerNonce(byte[] pollerNonce) {
    this.pollerNonce = pollerNonce;
    saveState();
  }

  public String getPollKey() {
    return pollKey;
  }

  public void setPollKey(String pollKey) {
    this.pollKey = pollKey;
    saveState();
  }

  public V3Serializer getSerializer() {
    return serializer;
  }

  public void setSerializer(V3VoterSerializer serializer) {
    this.serializer = serializer;
    // Transient - no need to save state
  }

  public String getUrl() {
    return url;
  }

  public void setUrl(String url) {
    this.url = url;
    saveState();
  }

  public VoteBlocks getVoteBlocks() {
    return voteBlocks;
  }

  public void setVoteBlocks(VoteBlocks voteBlocks) {
    this.voteBlocks = voteBlocks;
    saveState();
  }

  public V3Voter getVoter() {
    return voter;
  }

  public void setVoter(V3Voter voter) {
    this.voter = voter;
    saveState();
  }

  public byte[] getVoterNonce() {
    return voterNonce;
  }

  public void setVoterNonce(byte[] voterNonce) {
    this.voterNonce = voterNonce;
    saveState();
  }
  
  public synchronized void hashingDone(boolean hashingDone) {
    this.hashingDone = hashingDone;
    saveState();
  }
  
  public synchronized boolean hashingDone() {
    return hashingDone;
  }
  
  public synchronized void voteRequested(boolean voteRequested) {
    this.voteRequested = voteRequested;
    saveState();
  }
  
  public synchronized boolean voteRequested() {
    return voteRequested;
  }

  /* Delegate methods for V3Voter */
  
  public void sendMessage(V3LcapMessage msg) {
    voter.sendMessage(msg);
  }
  
  public void nominatePeers() {
    voter.nominatePeers();
  }
  
  public boolean generateVote() throws NoSuchAlgorithmException {
    return voter.generateVote();
  }
  
  /* Utility methods */
  
  private void saveState() {
    try {
      serializer.saveVoterUserData(this);
    } catch (V3Serializer.PollSerializerException ex) {
      log.error("Unable to save voter state in poll " + getPollKey());
    }
  }

  
}
