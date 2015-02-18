/*
 * $Id$
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
import org.lockss.state.*;
import org.lockss.util.*;

/**
 * Persistent user data state object used by V3Voter state machine.
 */
public class VoterUserData
  implements LockssSerializable {

  private V3LcapMessage pollMessage;
  private PeerIdentity pollerId;
  private String auId;
  private String pollKey;
  private int pollVersion;
  private String pluginVersion;
  private long duration;
  private long deadline;
  private long voteDeadline;
  private String hashAlgorithm;
  private VoteBlocks voteBlocks;
  private VoteBlocks symmetricVoteBlocks = null;
  private String url;
  private List nominees;
  private byte[] pollerNonce;
  private byte[] voterNonce;
  private byte[] voterNonce2 = null;
  private byte[] introEffortProof;
  private byte[] pollAckEffortProof;
  private byte[] remainingEffortProof;
  private byte[] repairEffortProof;
  private byte[] receiptEffortProof;
  private int modulus;
  private byte[] sampleNonce;
  private boolean hashingDone = false;
  private boolean voteRequested = false;
  private long createTime;
  private PsmInterpStateBean psmState;
  private int status;
  private String errorDetail;
  private boolean activePoll = true;
  private double agreementHint;
  private int numAgreeUrl;
  private int numDisagreeUrl;
  private int numVoterOnlyUrl;
  private int numPollerOnlyUrl;
  private boolean hasReceivedHint = false;
  private SubstanceChecker.State subCheckerState;
  /** @deprecated 
   * Left here only for deserialization compatibility */
  private String statusString;

  /** Transient non-serialized fields */
  private transient PollSpec spec;
  private transient CachedUrlSet cus;
  private transient V3Voter voter;
  private transient File messageDir;

  private static Logger log = Logger.getLogger("VoterUserData");

  VoterUserData() {}

  public VoterUserData(PollSpec spec, V3Voter voter, PeerIdentity pollerId,
                       String pollKey, long duration, String hashAlgorithm,
		       int modulus, byte[] sampleNonce,
                       byte[] pollerNonce, byte[] voterNonce,
                       byte[] introEffortProof, File messageDir) throws IOException {
    log.debug3("Creating V3 Voter User Data for poll " + pollKey + ", " + spec);
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
    this.modulus = modulus;
    if (modulus != 0) {
      log.info("Vote in sampled poll, modulus: " + modulus);
    }
    this.voterNonce = voterNonce;
    this.pollerNonce = pollerNonce;
    this.introEffortProof = introEffortProof;
    this.createTime = TimeBase.nowMs();
    this.messageDir = messageDir;
    this.voteBlocks = new DiskVoteBlocks(voter.getStateDir());
  }

  public void setPollMessage(LcapMessage msg) {
    this.pollMessage = (V3LcapMessage)msg;
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

  public long getVoteDeadline() {
    return voteDeadline;
  }

  public void setVoteDeadline(long voteDeadline) {
    this.voteDeadline = voteDeadline;
  }

  public String getHashAlgorithm() {
    return hashAlgorithm;
  }

  // NOTE: This is purely for the use of unit tests. In production,
  // the hash algorithm should be available at V3Voter creation time,
  // and changing it from what the poller expects is unlikely to be a
  // good idea.
  public void setHashAlgorithm(String hashAlgorithm) {
    this.hashAlgorithm = hashAlgorithm;
  }

  public int getModulus() {
    return modulus;
  }

  public void setModulus(int modulus) {
    this.modulus = modulus;
  }

  public byte[] getSampleNonce() {
    return sampleNonce;
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

  public String getRepairTarget() {
    return url;
  }

  public void setRepairTarget(String url) {
    this.url = url;
  }

  public VoteBlocks getVoteBlocks() {
    return voteBlocks;
  }

  public void setVoteBlocks(VoteBlocks voteBlocks) {
    this.voteBlocks = voteBlocks;
  }

  public VoteBlocks getSymmetricVoteBlocks() {
    return symmetricVoteBlocks;
  }

  public V3Voter getVoter() {
    return voter;
  }

  public File getMessageDir() {
    return messageDir;
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

  public byte[] getVoterNonce2() {
    return voterNonce2;
  }

  /**
   * @param symmetricNonce The voter's challenge to the poller. 
   * @throws IllegalArgumentException if symmetricNonce is null or zero-length.
   * @throws IOException if a DiskVoteBlocks could not be created.
   */
  public void enableSymmetricPoll(byte[] symmetricNonce) throws IOException {
    byte[] voterNonce2 = symmetricNonce;
    VoteBlocks symmetricVoteBlocks = new DiskVoteBlocks(voter.getStateDir());
    setVoterNonce2AndBlocks(voterNonce2, symmetricVoteBlocks);
  }

  // Also used by testing.
  void setVoterNonce2AndBlocks(byte[] voterNonce2,
			       VoteBlocks symmetricVoteBlocks) {
    if (voterNonce2 == null) {
      throw new IllegalArgumentException("Null symmetric nonce not allowed.");
    } 
    if (voterNonce2.length == 0) {
      throw new IllegalArgumentException(
        "Zero-length symmetric nonce not allowed.");
    }
    if (this.voterNonce2 != null) {
      throw new IllegalStateException(
	"Trying to set symmetric nonce when symmetric nonce is already set.");
    }
    if (symmetricVoteBlocks == null) {
      throw new IllegalArgumentException(
	"Null symmetricVoteBlocks not allowed.");
    }
    
    this.voterNonce2 = voterNonce2;
    this.symmetricVoteBlocks = symmetricVoteBlocks;
  }

  public boolean isSymmetricPoll() {
    return voterNonce2 != null;
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

  public boolean generateVote() {
    return voter.generateVote();
  }

  public String getStatusString() {
    return V3Voter.STATUS_STRINGS[getStatus()];
  }

  public int getStatus() {
    return status;
  }

  public void setStatus(int status) {
    this.status = status;
  }

  /**
   * @deprecated  Use {@link #setStatus(int)} instead.
   */
  public void setStatusString(String s) {
    // do nothing
  }

  public void setErrorDetail(String err) {
    this.errorDetail = err;
  }
  
  public String getErrorDetail() {
    return errorDetail;
  }
  
  synchronized public boolean isPollActive() {
    return activePoll;
  }
  
  synchronized public boolean isPollCompleted() {
    return !activePoll;
  }
  
  synchronized public void setActivePoll(boolean b) {
    this.activePoll = b;
  }

  /** Close the poll.
      @return true if the poll was previously open. */
  synchronized public boolean checkAndCompletePoll() {
    boolean previous = this.activePoll;
    this.activePoll = false;
    return previous;
  }

  public double getAgreementHint() {
    return agreementHint;
  }

  public void setAgreementHint(double agreementHint) {
    this.agreementHint = agreementHint;
    this.hasReceivedHint = true;
  }

  public boolean hasReceivedHint() {
    return hasReceivedHint;
  }

  public int getNumAgreeUrl() {
    return numAgreeUrl;
  }

  public void setNumAgreeUrl(int num) {
    numAgreeUrl = num;
  }

  public int getNumDisagreeUrl() {
    return numDisagreeUrl;
  }

  public void setNumDisagreeUrl(int num) {
    numDisagreeUrl = num;
  }

  public int getNumVoterOnlyUrl() {
    return numVoterOnlyUrl;
  }

  public void setNumVoterOnlyUrl(int num) {
    numVoterOnlyUrl = num;
  }

  public int getNumPollerOnlyUrl() {
    return numVoterOnlyUrl;
  }

  public void setNumPollerOnlyUrl(int num) {
    numPollerOnlyUrl = num;
  }

  public SubstanceChecker.State getSubstanceCheckerState() {
    return subCheckerState;
  }

  public void setSubstanceCheckerState(SubstanceChecker.State subCheckerState) {
    this.subCheckerState = subCheckerState;
  }

  /*
   * Implementation of V3LcapMessage.Factory
   */
  public V3LcapMessage makeMessage(int opcode) {
    return new V3LcapMessage(getAuId(), getPollKey(), getPluginVersion(),
                             getPollerNonce(), getVoterNonce(), getVoterNonce2(),
			     opcode, getModulus(), getSampleNonce(),
                             getDeadline(), getPollerId(), messageDir,
                             voter.getLockssDaemon());
  }

  public V3LcapMessage makeMessage(int opcode, long sizeEst) {
    return makeMessage(opcode);
  }
  
  /**
   * Release unneeded resources used by this object at the end of a poll.
   */
  public void release() {
    deadline = TimeBase.nowMs();
    introEffortProof = null;
    pollAckEffortProof = null;
    receiptEffortProof = null;
    remainingEffortProof = null;
    repairEffortProof = null;

    hashAlgorithm = null;
    messageDir = null;
    nominees = null;
    pollMessage = null;
    psmState = null;
    voteBlocks = null;
    symmetricVoteBlocks = null;
  }
}
