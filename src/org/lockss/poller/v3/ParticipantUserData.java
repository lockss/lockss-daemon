/*
 * $Id$
 */

/*
Copyright (c) 2000-2017 Board of Trustees of Leland Stanford Jr. University,
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
import org.lockss.config.*;

import java.io.*;
import java.util.*;

/**
 * Persistent user data state object used by V3Poller state machine.
 */
public class ParticipantUserData implements LockssSerializable {

  private static transient final Logger log = Logger.getLogger("V3Poller");

  /** Keeps track of per-peer agreement data.  Concrete subclasses either
   * do or don't keep full URL lists */
  public static abstract class VoteCounts implements LockssSerializable {
    // weighted agree, etc., counts
    protected float wAgreed;
    protected float wDisagreed;
    protected float wPollerOnly;
    protected float wVoterOnly;

    abstract void release();

    private static List asArrayList(Collection coll) {
      List res = new ArrayList(coll.size());
      res.addAll(coll);
      return res;
    }

    // For testing
    /**
     * @return a String representing the votes by category, separated by "/":
     * agree/disagree/pollerOnly/voterOnly/neither/spoiled
     */
    String votes() {
      return getAgreedVotes() + "/" + getDisagreedVotes() + "/" +
	getPollerOnlyVotes() + "/" + getVoterOnlyVotes() + "/" +
	getNeitherVotes() + "/" + getSpoiledVotes();
    }

    public abstract boolean hasPeerUrlLists();

    abstract void addAgreed(String url);

    abstract void addDisagreed(String url);

    abstract void addPollerOnly(String url);

    abstract void addVoterOnly(String url);

    abstract void addNeither(String url);

    abstract void addSpoiled(String url);
    

    /**
     * Returns the count of agreed votes for this peer.
     */
    public abstract long getAgreedVotes();

    /**
     * Returns the count of disagreed votes for this peer.
     */
    public abstract long getDisagreedVotes();

    /**
     * Returns the count of poller-only votes for this peer.
     */
    public abstract long getPollerOnlyVotes();

    /**
     * Returns the count of voter-only votes for this peer.
     */
    public abstract long getVoterOnlyVotes();

    /**
     * Returns the count of spoiled votes for this peer.
     */
    public abstract long getSpoiledVotes();
      
    /**
     * Returns the count of neither votes for this peer.
     */
    public abstract long getNeitherVotes();
      
    /** 
     * Return the percent agreement for this peer.
     * 
     * @return The percent agreement for this peer (a number between
     * 0.0 and 1.0, inclusive)
     */
    public float getPercentAgreement() {
      long tallied = talliedVotes();
      long agreed = getAgreedVotes();
      long disagreed = getDisagreedVotes();
      log.debug2("[getPercentAgreement] agreeVotes = " + agreed +
		 "; talliedVotes = " + tallied);
      if (agreed > 0 && tallied > 0) {
	return (float)agreed / (float)tallied;
      } else {
	return 0.0f;
      }
    }

    /** 
     * Return the weighted percent agreement for this peer.
     * 
     * @return The weighted percent agreement for this peer (a number between
     * 0.0 and 1.0, inclusive)
     */
    public float getWeightedPercentAgreement() {
      float wTallied = getWeightedTalliedVotes();
      log.debug2("[getWeightedPercentAgreement] wAgreed = "
		 + wAgreed + "; wTallied = " + wTallied);
      return wTallied > 0.0 ? wAgreed / wTallied : 0.0f;
    }

    /**
     * Returns the weighted count of agreed votes for this peer.
     * 
     * @return a float with the count of agreed votes.
     */
    public float getWeightedAgreedVotes() {
      return wAgreed;
    }

    /**
     * Returns the weighted count of disagreed votes for this peer.
     * 
     * @return a float with the count of disagreed votes.
     */
    public float getWeightedDisagreedVotes() {
      return wDisagreed;
    }

    /**
     * Returns the weighted count of poller-only votes for this peer.
     * 
     * @return a float with the count of poller-only votes.
     */
    public float getWeightedPollerOnlyVotes() {
      return wPollerOnly;
    }

    /**
     * Returns the weighted count of voter-only votes for this peer.
     * 
     * @return a float with the count of voter-only votes.
     */
    public float getWeightedVoterOnlyVotes() {
      return wVoterOnly;
    }

    /**
     * Returns the list of agreed urls for this peer.
     */
    public abstract Collection<String> getAgreedUrls();

    /**
     * Returns the list of disagreed urls for this peer.
     */
    public abstract Collection<String> getDisagreedUrls();

    /**
     * Returns the list of poller-only urls for this peer.
     */
    public abstract Collection<String> getPollerOnlyUrls();

    /**
     * Returns the list of voter-only urls for this peer.
     */
    public abstract Collection<String> getVoterOnlyUrls();

    /**
     * Returns the list of spoiled urls for this peer.
     */
    public abstract Collection<String> getSpoiledUrls();


    /**
     * @return the total of the "significant" votes.
     * This excludes "neither" and "spoiled" votes.
     */
    long talliedVotes() {
      return getAgreedVotes() + getDisagreedVotes() + 
	getPollerOnlyVotes() + getVoterOnlyVotes();
    }

    float getWeightedTalliedVotes() {
      return wAgreed + wDisagreed + wPollerOnly + wVoterOnly;
    }
  }

  /** A VoteCounts that maintains counters only.  It cannot accurately
   * track repairs, */
  public class VoteCountsOnly extends VoteCounts {
    private long agreedVotes;
    private long disagreedVotes;
    private long pollerOnlyVotes;
    private long voterOnlyVotes;
    private long neitherVotes;
    private long spoiledVotes;

    void release() {
    }

    private VoteCountsOnly() {
    }

    public boolean hasPeerUrlLists() {
      return false;
    }

    void addAgreed(String url) {
      if (log.isDebug3()) {
	log.debug3("agreed: " + url, new Throwable());
      }
      agreedVotes++;
      wAgreed += getUrlResultWeight(url);
    }

    void addDisagreed(String url) {
      if (log.isDebug3()) {
	log.debug3("disagreed: " + url, new Throwable());
      }
      disagreedVotes++;
      wDisagreed += getUrlResultWeight(url);
    }

    void addPollerOnly(String url) {
      if (log.isDebug3()) {
	log.debug3("poller-only: " + url, new Throwable());
      }
      pollerOnlyVotes++;
      wPollerOnly += getUrlResultWeight(url);
    }

    void addVoterOnly(String url) {
      if (log.isDebug3()) {
	log.debug3("voter-only: " + url, new Throwable());
      }
      voterOnlyVotes++;
      wVoterOnly += getUrlResultWeight(url);
    }

    void addSpoiled(String url) {
      spoiledVotes++;
    }

    void addNeither(String url) {
      neitherVotes++;
    }

    public long getAgreedVotes() {
      return agreedVotes;
    }

    public long getDisagreedVotes() {
      return disagreedVotes;
    }

    public long getPollerOnlyVotes() {
      return pollerOnlyVotes;
    }

    public long getVoterOnlyVotes() {
      return voterOnlyVotes;
    }

    public long getSpoiledVotes() {
      return spoiledVotes;
    }
      
    public long getNeitherVotes() {
      return neitherVotes;
    }

    public Collection<String> getAgreedUrls() {
      throw new UnsupportedOperationException("Per peer URL lists not available");
    }

    public Collection<String> getDisagreedUrls() {
      throw new UnsupportedOperationException("Per peer URL lists not available");
    }

    public Collection<String> getPollerOnlyUrls() {
      throw new UnsupportedOperationException("Per peer URL lists not available");
    }

    public Collection<String> getVoterOnlyUrls() {
      throw new UnsupportedOperationException("Per peer URL lists not available");
    }

    public Collection<String> getSpoiledUrls() {
      throw new UnsupportedOperationException("Per peer URL lists not available");
    }
  }

  /** A VoteCounts that maintains collections of the agreeing, disagreeing,
   * etc., URLs.  It accurately tracks updates due to repairs, */
  public class VoteCountsUrls extends VoteCounts {

    private long neitherVotes;
    private Collection<String> agreedUrls;
    private Collection<String> disagreedUrls;
    private Collection<String> pollerOnlyUrls;
    private Collection<String> voterOnlyUrls;
    private Collection<String> spoiledUrls;

    void release() {
      agreedUrls = asList(agreedUrls);
      disagreedUrls = asList(disagreedUrls);
      pollerOnlyUrls = asList(pollerOnlyUrls);
      voterOnlyUrls = asList(voterOnlyUrls);
      spoiledUrls = asList(spoiledUrls);
    }

    List asList(Collection coll) {
      List res = new ArrayList(coll.size());
      res.addAll(coll);
      return res;
    }

    private VoteCountsUrls() {
      agreedUrls = new HashSet<String>();
      disagreedUrls = new HashSet<String>();
      pollerOnlyUrls = new HashSet<String>();
      voterOnlyUrls = new HashSet<String>();
      spoiledUrls = new HashSet<String>();
    }

    public boolean hasPeerUrlLists() {
      return true;
    }

    private synchronized void removeUrl(String url) {
      if (log.isDebug3()) {
	log.debug3("removeUrl("+url+")", new Throwable());
      }
      float weight = getUrlResultWeight(url);
      if (agreedUrls.remove(url)) wAgreed -= weight;
      if (disagreedUrls.remove(url)) wDisagreed -= weight;
      if (pollerOnlyUrls.remove(url)) wPollerOnly -= weight;
      if (voterOnlyUrls.remove(url)) wVoterOnly -= weight;
      spoiledUrls.remove(url);
    }

    void addAgreed(String url) {
      removeUrl(url);
      agreedUrls.add(url);
      wAgreed += getUrlResultWeight(url);
    }

    void addDisagreed(String url) {
      removeUrl(url);
      disagreedUrls.add(url);
      wDisagreed += getUrlResultWeight(url);
    }

    void addPollerOnly(String url) {
      removeUrl(url);
      pollerOnlyUrls.add(url);
      wPollerOnly += getUrlResultWeight(url);
    }

    void addVoterOnly(String url) {
      removeUrl(url);
      voterOnlyUrls.add(url);
      wVoterOnly += getUrlResultWeight(url);
    }

    void addSpoiled(String url) {
      removeUrl(url);
      spoiledUrls.add(url);
    }

    void addNeither(String url) {
      neitherVotes++;
    }

    public long getAgreedVotes() {
      return agreedUrls.size();
    }

    public long getDisagreedVotes() {
      return disagreedUrls.size();
    }

    public long getPollerOnlyVotes() {
      return pollerOnlyUrls.size();
    }

    public long getVoterOnlyVotes() {
      return voterOnlyUrls.size();
    }

    public long getSpoiledVotes() {
      return spoiledUrls.size();
    }
      
    public long getNeitherVotes() {
      return neitherVotes;
    }

    public Collection<String> getAgreedUrls() {
      return agreedUrls;
    }

    public Collection<String> getDisagreedUrls() {
      return disagreedUrls;
    }

    public Collection<String> getPollerOnlyUrls() {
      return pollerOnlyUrls;
    }

    public Collection<String> getVoterOnlyUrls() {
      return voterOnlyUrls;
    }

    public Collection<String> getSpoiledUrls() {
      return spoiledUrls;
    }

  }

  /**
   * A static VoteBlockTally to increment the vote for any particular
   * voter.
   */
  public static transient final 
    VoteBlockTallier.VoteBlockTally voteTally =
    new VoteBlockTallier.VoteBlockTally() {
      public void voteAgreed(ParticipantUserData voter,
			     String url) {
	voter.voteCounts.addAgreed(url);
      }
      public void voteDisagreed(ParticipantUserData voter,
				String url) {
	voter.voteCounts.addDisagreed(url);
      }
      public void votePollerOnly(ParticipantUserData voter,
				 String url) {
	voter.voteCounts.addPollerOnly(url);
      }
      public void voteVoterOnly(ParticipantUserData voter,
				String url) {
	voter.voteCounts.addVoterOnly(url);
      }
      public void voteNeither(ParticipantUserData voter,
			      String url) {
	voter.voteCounts.addNeither(url);
      }
      public void voteSpoiled(ParticipantUserData voter,
			      String url) {
	voter.voteCounts.addSpoiled(url);
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
  private VoteCounts voteCounts;
  private long bytesHashed;
  private long bytesRead;
  /** The poll NAK code, if any */
  private PollNak pollNak;

  /** Transient non-serialized fields */
  private transient V3Poller poller;
  private transient PollerStateBean pollState;
  private transient PsmInterp psmInterp;
  private transient File messageDir;

  protected ParticipantUserData(V3Poller poller) {
    if (poller.isRecordPeerUrlLists()) {
      voteCounts = new VoteCountsUrls();
    } else {
      voteCounts = new VoteCountsOnly();
    }
  }

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
    this(poller);
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
    return voteCounts;
  }

  /** 
   * Return the percent agreement for this peer.
   * 
   * @return The percent agreement for this peer (a number between 0.0 and 1.0)
   */
  public float getPercentAgreement() {
    return voteCounts.getPercentAgreement();
  }

  public float getWeightedPercentAgreement() {
    return voteCounts.getWeightedPercentAgreement();
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

  float getUrlResultWeight(String url) {
    return pollState.getUrlResultWeight(url);
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

    voteCounts.release();
  }

}
