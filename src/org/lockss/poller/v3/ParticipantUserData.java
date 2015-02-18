/*
 * $Id$
 */

/*
Copyright (c) 2000-2014 Board of Trustees of Leland Stanford Jr. University,
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

  private static transient final Logger log = Logger.getLogger("V3Poller");

  public static class VoteCounts implements LockssSerializable {
    long agreedVotes;
    long disagreedVotes;
    long pollerOnlyVotes;
    long voterOnlyVotes;
    long neitherVotes;
    long spoiledVotes;

    private VoteCounts() {
    }

    public VoteCounts(VoteCounts voteCounts) {
      agreedVotes = voteCounts.agreedVotes;
      disagreedVotes = voteCounts.disagreedVotes;
      pollerOnlyVotes = voteCounts.pollerOnlyVotes;
      voterOnlyVotes = voteCounts.voterOnlyVotes;
      neitherVotes = voteCounts.neitherVotes;
      spoiledVotes = voteCounts.spoiledVotes;
    }

    // For testing
    static String votes(long agreedVotes, long disagreedVotes,
			long pollerOnlyVotes, long voterOnlyVotes,
			long neitherVotes, long spoiledVotes) {
      return agreedVotes + "/" + disagreedVotes + "/" + pollerOnlyVotes + "/" +
	voterOnlyVotes + "/" + neitherVotes + "/" + spoiledVotes;
    }

    /** 
     * Return the percent agreement for this peer.
     * 
     * @return The percent agreement for this peer (a number between
     * 0.0 and 1.0, inclusive)
     */
    public float getPercentAgreement() {
      long talliedVotes = talliedVotes();
      log.debug2("[getPercentAgreement] agreeVotes = "
		 + agreedVotes + "; talliedVotes = " + talliedVotes);
      if (agreedVotes > 0 && talliedVotes > 0) {
	return (float)agreedVotes / (float)talliedVotes;
      } else {
	return 0.0f;
      }
    }

    /**
     * Returns the count of agreed votes for this peer.
     * 
     * @return a long with the count of agreed votes.
     */
    public long getAgreedVotes() {
      return agreedVotes;
    }

    /**
     * Returns the count of disagreed votes for this peer.
     * 
     * @return a long with the count of disagreed votes.
     */
    public long getDisagreedVotes() {
      return disagreedVotes;
    }

    /**
     * Returns the count of poller-only votes for this peer.
     * 
     * @return a long with the count of poller-only votes.
     */
    public long getPollerOnlyVotes() {
      return pollerOnlyVotes;
    }

    /**
     * Returns the count of voter-only votes for this peer.
     * 
     * @return a long with the count of voter-only votes.
     */
    public long getVoterOnlyVotes() {
      return voterOnlyVotes;
    }

    /**
     * @return the total of the "significant" votes.
     * This excludes "neither" and "spoiled" votes.
     */
    long talliedVotes() {
      return agreedVotes + disagreedVotes + pollerOnlyVotes + voterOnlyVotes;
    }

    /**
     * @return a String representing the votes by category, separated by "/":
     * agree/disagree/pollerOnly/voterOnly/neither/spoiled
     */
    String votes() {
      return votes(agreedVotes, disagreedVotes,
		   pollerOnlyVotes, voterOnlyVotes,
		   neitherVotes, spoiledVotes);
    }
  }

  /**
   * A static VoteBlockTally to increment the vote for any particular
   * voter.
   */
  public static transient final 
    VoteBlockTallier.VoteBlockTally voteTally =
      new VoteBlockTallier.VoteBlockTally() {
    public void voteAgreed(ParticipantUserData voter) {
      voter.voteCounts.agreedVotes++;
    }
    public void voteDisagreed(ParticipantUserData voter) {
      voter.voteCounts.disagreedVotes++;
    }
    public void votePollerOnly(ParticipantUserData voter) {
      voter.voteCounts.pollerOnlyVotes++;
    }
    public void voteVoterOnly(ParticipantUserData voter) {
      voter.voteCounts.voterOnlyVotes++;
    }
    public void voteNeither(ParticipantUserData voter) {
      voter.voteCounts.neitherVotes++;
    }
    public void voteSpoiled(ParticipantUserData voter) {
      voter.voteCounts.spoiledVotes++;
    }
  };

  private PeerIdentity voterId;
  private String hashAlgorithm;
  private VoteBlocks voteBlocks;
  private VoteBlocks symmetricVoteBlocks;
  private List nominees;
  private byte[] pollerNonce;
  private byte[] voterNonce;
  private byte[] voterNonce2;
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
  private VoteCounts voteCounts = new VoteCounts();
  private long bytesHashed;
  private long bytesRead;
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
    this(id, poller, poller.getPollerStateBean(), messageDir);
  }

  public ParticipantUserData(PeerIdentity id, V3Poller poller,
			     PollerStateBean pollState, File messageDir) {
    this.voterId = id;
    this.poller = poller;
    this.pollState = pollState;
    this.messageDir = messageDir;
  }

  public boolean isPollActive() {
    return poller.isPollActive();
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

  public byte[] getVoterNonce2() {
    return voterNonce2;
  }

  public void setVoterNonce2(byte[] voterNonce2) {
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
    this.voterNonce2 = voterNonce2;
  }

  /**
   * @param symmetricNonce The voter's challenge to the poller. 
   * @throws IllegalArgumentException if symmetricNonce is null or zero-length.
   */
  public void enableSymmetricPoll(byte[] symmetricNonce) {
    setVoterNonce2(symmetricNonce);
  }

  public boolean isSymmetricPoll() {
    return voterNonce2 != null;
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

  public void setSymmetricVoteBlocks(VoteBlocks blocks) {
    symmetricVoteBlocks = blocks;
  }

  public VoteBlocks getSymmetricVoteBlocks() {
    return symmetricVoteBlocks;
  }

  public void addHashStats(VoteBlock vb) {
    for (VoteBlock.Version ver : vb.getVersions()) {
      // Voter hashed the filtered content twice (plain & nonced), plus the
      // poller nonce plus voter nonce.  Replace this if/when hash count
      // included in VoteBlock.
      bytesHashed +=
	2 * ver.getFilteredLength() + 2 * V3Poller.HASH_NONCE_LENGTH;
      bytesRead += ver.getUnfilteredLength();
    }
  }

  public long getBytesHashed() {
    return bytesHashed;
  }

  public long getBytesRead() {
    return bytesRead;
  }

  /**
   * @return true if this peer has agreed to participate in the poll.
   */
  // XXX This is fragile, find a better way.
  public boolean isParticipating() {
    switch (status) {
    case V3Poller.PEER_STATUS_ACCEPTED_POLL:
    case V3Poller.PEER_STATUS_NOMINATED:
    case V3Poller.PEER_STATUS_WAITING_VOTE:
    case V3Poller.PEER_STATUS_VOTED:
    case V3Poller.PEER_STATUS_COMPLETE:
      return true;
    default:
      return false;
    }
  }

  public boolean hasVoted() {
    switch (status) {
    case V3Poller.PEER_STATUS_VOTED:
    case V3Poller.PEER_STATUS_COMPLETE:
      return true;
    default:
      return false;
    }
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

  /**
   * @return an copy of the voteCounts for this peer.
   */
  public VoteCounts getVoteCounts() {
    return new VoteCounts(voteCounts);
  }

  /** 
   * Return the percent agreement for this peer.
   * 
   * @return The percent agreement for this peer (a number between 0.0 and 1.0)
   */
  public float getPercentAgreement() {
    return voteCounts.getPercentAgreement();
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

  void removeParticipant(PollNak nak) {
    poller.removeParticipant(voterId, nak);
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
                             getPollerNonce(), getVoterNonce(), getVoterNonce2(),
			     opcode,
			     poller.getSampleModulus(), poller.getSampleNonce(),
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
    symmetricVoteBlocks = null;

    psmInterp = null;
    psmState = null;
  }

}
