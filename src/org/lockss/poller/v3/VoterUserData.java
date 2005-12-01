/*
 * $Id: VoterUserData.java,v 1.9 2005-12-01 01:54:44 smorabito Exp $
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
import java.security.NoSuchAlgorithmException;
import java.util.*;

import org.lockss.plugin.*;
import org.lockss.poller.*;
import org.lockss.protocol.*;
import org.lockss.protocol.psm.*;
import org.lockss.util.*;

/**
 * Persistent user data state object used by V3Voter state machine.
 */
public class VoterUserData
  implements LockssSerializable {

  // XXX: LcapMessage may not be serializable after repair is implemented!
  private LcapMessage pollMessage;
  private PeerIdentity pollerId;
  private String auId;
  private String pollKey;
  private int pollVersion;
  private String pluginVersion;
  private long duration;
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
  private long createTime;
  private PsmInterpStateBean psmState;
  private String statusString;

  /** Transient non-serialized fields */
  private transient PollSpec spec;
  private transient CachedUrlSet cus;
  private transient V3Voter voter;

  private static Logger log = Logger.getLogger("VoterUserData");

  VoterUserData() {}

  public VoterUserData(PollSpec spec, V3Voter voter, PeerIdentity pollerId,
                       String pollKey, long duration, String hashAlgorithm,
                       byte[] pollerNonce, byte[] voterNonce,
                       byte[] introEffortProof) {
    log.debug3("Creating V3 Voter User Data for poll " + pollKey);
    this.spec = spec;
    this.auId = spec.getAuId();
    this.url = spec.getUrl();
    this.cus = spec.getCachedUrlSet();
    this.pollVersion = spec.getProtocolVersion();
    this.pluginVersion = spec.getPluginVersion();
    this.voter = voter;
    this.pollerId = pollerId;
    this.pollKey = pollKey;
    this.duration = duration;
    this.deadline = Deadline.in(duration).getExpirationTime();
    this.hashAlgorithm = hashAlgorithm;
    this.voterNonce = voterNonce;
    this.pollerNonce = pollerNonce;
    this.introEffortProof = introEffortProof;
    this.voteBlocks = new MemoryVoteBlocks();
    this.createTime = TimeBase.nowMs();
    this.statusString = "Active";
  }

  public void setPollMessage(LcapMessage msg) {
    this.pollMessage = msg;
  }

  public LcapMessage getPollMessage() {
    return pollMessage;
  }

  public long getCreateTime() {
    return createTime;
  }

  public String getAuId() {
    return auId;
  }

  public void setAuId(String auId) {
    this.auId = auId;
  }

  public CachedUrlSet getCachedUrlSet() {
    return cus;
  }

  public void setCachedUrlSet(CachedUrlSet cus) {
    this.cus = cus;
  }

  public void setPollSpec(PollSpec spec) {
    this.spec = spec;
  }

  public PollSpec getPollSpec() {
    return spec;
  }

  public long getDuration() {
    return duration;
  }

  public void setDuration(long duration) {
    this.duration = duration;
  }

  public long getDeadline() {
    return deadline;
  }

  public void setDeadline(long deadline) {
    this.deadline = deadline;
  }

  public String getHashAlgorithm() {
    return hashAlgorithm;
  }

  public void setHashAlgorithm(String hashAlgorithm) {
    this.hashAlgorithm = hashAlgorithm;
  }

  public byte[] getIntroEffortProof() {
    return introEffortProof;
  }

  public void setIntroEffortProof(byte[] introEffortProof) {
    this.introEffortProof = introEffortProof;
  }

  public List getNominees() {
    return nominees;
  }

  public void setNominees(List nominees) {
    this.nominees = nominees;
  }

  public byte[] getPollAckEffortProof() {
    return pollAckEffortProof;
  }

  public void setPollAckEffortProof(byte[] pollAckEffortProof) {
    this.pollAckEffortProof = pollAckEffortProof;
  }

  public byte[] getReceiptEffortProof() {
    return receiptEffortProof;
  }

  public void setReceiptEffortProof(byte[] receiptEffortProof) {
    this.receiptEffortProof = receiptEffortProof;
  }

  public byte[] getRemainingEffortProof() {
    return remainingEffortProof;
  }

  public void setRemainingEffortProof(byte[] remainingEffortProof) {
    this.remainingEffortProof = remainingEffortProof;
  }

  public byte[] getRepairEffortProof() {
    return repairEffortProof;
  }

  public void setRepairEffortProof(byte[] repairEffortProof) {
    this.repairEffortProof = repairEffortProof;
  }

  public String getPluginVersion() {
    return pluginVersion;
  }

  public void setPluginVersion(String pluginVersion) {
    this.pluginVersion = pluginVersion;
  }

  public int getPollVersion() {
    return pollVersion;
  }

  public void setPollVersion(int pollVersion) {
    this.pollVersion = pollVersion;
  }

  public PeerIdentity getPollerId() {
    return pollerId;
  }

  public void setPollerId(PeerIdentity pollerId) {
    this.pollerId = pollerId;
  }

  public byte[] getPollerNonce() {
    return pollerNonce;
  }

  public void setPollerNonce(byte[] pollerNonce) {
    this.pollerNonce = pollerNonce;
  }

  public String getPollKey() {
    return pollKey;
  }

  public void setPollKey(String pollKey) {
    this.pollKey = pollKey;
  }

  public String getUrl() {
    return url;
  }

  public void setUrl(String url) {
    this.url = url;
  }

  public VoteBlocks getVoteBlocks() {
    return voteBlocks;
  }

  public void setVoteBlocks(VoteBlocks voteBlocks) {
    this.voteBlocks = voteBlocks;
  }

  public V3Voter getVoter() {
    return voter;
  }

  public PsmInterpStateBean getPsmState() {
    return psmState;
  }

  public void setPsmState(PsmInterpStateBean psmState) {
    this.psmState = psmState;
  }

  public void setVoter(V3Voter voter) {
    this.voter = voter;
  }

  public byte[] getVoterNonce() {
    return voterNonce;
  }

  public void setVoterNonce(byte[] voterNonce) {
    this.voterNonce = voterNonce;
  }

  public synchronized void hashingDone(boolean hashingDone) {
    this.hashingDone = hashingDone;
  }

  public synchronized boolean hashingDone() {
    return hashingDone;
  }

  public synchronized void voteRequested(boolean voteRequested) {
    this.voteRequested = voteRequested;
  }

  public synchronized boolean voteRequested() {
    return voteRequested;
  }

  /* Delegate methods for V3Voter */

  public void sendMessageTo(V3LcapMessage msg, PeerIdentity id)
      throws IOException {
    voter.sendMessageTo(msg, id);
  }

  public void nominatePeers() {
    voter.nominatePeers();
  }

  public boolean generateVote() throws NoSuchAlgorithmException {
    return voter.generateVote();
  }

  public String getStatusString() {
    return statusString;
  }

  public void setStatusString(String s) {
    this.statusString = s;
  }
}
