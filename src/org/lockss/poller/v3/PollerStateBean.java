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

import java.util.*;

import org.lockss.hasher.HashBlock;
import org.lockss.plugin.*;
import org.lockss.poller.*;
import org.lockss.protocol.*;
import org.lockss.protocol.psm.*;
import org.lockss.util.*;
import org.lockss.poller.v3.V3Poller.PollVariant;

/**
 * Persistant state object for the V3Poller.
 */
public class PollerStateBean implements LockssSerializable {
  private static final Logger log = Logger.getLogger("PollerStateBean");

  /* Unique poll identifier. */
  private String pollKey;
  /* The time by which the poll must have completed. */
  private long pollDeadline;
  /* The time by which all voters must have voted. */
  private long voteDeadline;
  /* The requested duration of this poll. */
  private long duration;
  /* The time the poll actually ended, or -1 if not ended yet. */
  private long pollEnd = -1;
  /* The ArchivalUnit identifier. */
  private String auId;
  /* The version of the plugin used by the AU. */
  private String pluginVersion;
  /* The version of the polling protocol to use. */
  private int protocolVersion;
  private String url;
  private PeerIdentity pollerId;
  private String hashAlgorithm;
  private int modulus;
  private PollVariant variant;
  private long createTime;
  private int quorum;
  private int voteMargin;
  private boolean activePoll;
  private int outerCircleTarget;
  private String statusString;
  private int status;
  private RepairQueue repairQueue;
  private boolean hashStarted;
  private Collection votedPeers;
  /** List of peers who have said they don't have the AU */
  private Collection<PeerIdentity> noAuPeers;
  private TallyStatus tallyStatus;
  private String errorDetail; // If non-null, detailed error information to
			      // be displayed in the status table
  private String additionalInfo;    // If non-null, extra information to be
				    // displayed in the status table

  /* Non-serializable transient fields */
  private transient PollSpec spec;
  private transient CachedUrlSet cus;

  /** @deprecated
   * Left here only for deserialization compatibility.
   */
  private transient int pollSize;
  /** @deprecated
   * Left here only for deserialization compatibility.
   */
  private transient int hashBlockIndex;
  /** @deprecated
   * Left here only for deserialization compatibility.
   */
  private transient ArrayList hashedBlocks;

  /**
   * Counter of participants who have not yet nominated any peers.
   */
  private int nomineeCounter = 0;

  /**
   * The target URL of the most recently hashed block. Updated after each block
   * is hashed and tallied by V3Poller. Used when returning to tally more blocks
   * after requesting a repair, or when sending a vote request for the next in a
   * sequence of votes.
   */
  private String lastHashedBlock;

  protected PollerStateBean() {}

  public PollerStateBean(PollSpec spec, PeerIdentity orig, String pollKey,
                         long duration, long pollDeadline,
			 int outerCircleTarget, int quorum, int voteMargin,
			 String hashAlg, int modulus, PollVariant variant,
			 int maxRepairs) {
    this.pollerId = orig;
    this.pollKey = pollKey;
    this.duration = duration;
    this.pollDeadline = pollDeadline;
    this.outerCircleTarget = outerCircleTarget;
    this.auId = spec.getAuId();
    this.protocolVersion = spec.getProtocolVersion();
    this.pluginVersion = spec.getPluginVersion();
    this.url = spec.getUrl();
    this.cus = spec.getCachedUrlSet();
    this.spec = spec;
    this.hashAlgorithm = hashAlg;
    this.modulus = modulus;
    this.variant = variant;
    this.createTime = TimeBase.nowMs();
    this.quorum = quorum;
    this.voteMargin = voteMargin;
    this.statusString = "Initializing";
    this.repairQueue = new RepairQueue(maxRepairs);
    this.votedPeers = new ArrayList();
    this.tallyStatus = new TallyStatus();
  }

  public long getCreateTime() {
    return createTime;
  }

  public void setPollKey(String id) {
    this.pollKey = id;
  }

  public String getPollKey() {
    return pollKey;
  }

  public void setPollerId(PeerIdentity pollerId) {
    this.pollerId = pollerId;
  }

  public PeerIdentity getPollerId() {
    return pollerId;
  }

  public void setLastHashedBlock(String target) {
    this.lastHashedBlock = target;
  }

  public String getLastHashedBlock() {
    return lastHashedBlock;
  }

  public void setAuId(String auId) {
    this.auId = auId;
  }

  public String getAuId() {
    return auId;
  }

  public void setProtocolVersion(int protocolVersion) {
    this.protocolVersion = protocolVersion;
  }

  public int getProtocolVersion() {
    return protocolVersion;
  }

  public void setPluginVersion(String pluginVersion) {
    this.pluginVersion = pluginVersion;
  }

  public String getPluginVersion() {
    return pluginVersion;
  }

  public void setUrl(String url) {
    this.url = url;
  }

  public String getUrl() {
    return url;
  }

  public void setCachedUrlSet(CachedUrlSet cus) {
    this.cus = cus;
  }

  public CachedUrlSet getCachedUrlSet() {
    return cus;
  }

  public void setPollSpec(PollSpec spec) {
    this.spec = spec;
  }

  public PollSpec getPollSpec() {
    return spec;
  }

  // NOTE: This is purely for the use of unit tests. In production,
  // the hash algorithm should be available at V3Voter creation time,
  // and changing it from what the poller expects is unlikely to be a
  // good idea.
  public void setHashAlgorithm(String hashAlgorithm) {
    this.hashAlgorithm = hashAlgorithm;
  }

  public String getHashAlgorithm() {
    return hashAlgorithm;
  }

  public int getModulus() {
    return modulus;
  }

  public void setModulus(int modulus) {
    this.modulus = modulus;
  }

  public PollVariant getPollVariant() {
    return variant;
  }

  public void setPollDeadline(long l) {
    this.pollDeadline = l;
  }

  public long getPollDeadline() {
    return pollDeadline;
  }

  public void setVoteDeadline(long l) {
    this.voteDeadline = l;
  }

  public long getVoteDeadline() {
    return this.voteDeadline;
  }
  
  public long getVoteDuration() {
    long dur = voteDeadline - TimeBase.nowMs();
    if (dur < 0) {
      return 0;
    } else {
      return dur;
    }
  }

  public long getDuration() {
    return duration;
  }

  public void setDuration(long duration) {
    this.duration = duration;
  }

  public void setPollEnd(long l) {
    this.pollEnd = l;
  }

  /* The time the poll actually ended, or -1 if not ended yet. */
  public long getPollEnd() {
    return pollEnd;
  }

  public int getQuorum() {
    return quorum;
  }

  public void setQuorum(int quorum) {
    this.quorum = quorum;
  }

  public int getVoteMargin() {
    return voteMargin;
  }

  public void setVoteMargin(int voteMargin) {
    this.voteMargin = voteMargin;
  }

  public boolean hashStarted() {
    return hashStarted;
  }

  public void hashStarted(boolean b) {
    this.hashStarted = b;
  }

  // Simple counter
  public void addVotedPeer(PeerIdentity id) {
    synchronized(votedPeers) {
      votedPeers.add(id);
    }
  }

  public boolean hasPeerVoted(PeerIdentity id) {
    synchronized(votedPeers) {
      return votedPeers.contains(id);
    }
  }
  
  public int votedPeerCount() {
    synchronized(votedPeers) {
      return votedPeers.size();
    }
  }

  public void addNoAuPeer(PeerIdentity id) {
    if (noAuPeers == null) {
      noAuPeers = new ArrayList();
    }
    noAuPeers.add(id);
  }

  public Collection<PeerIdentity> getNoAuPeers() {
    return noAuPeers;
  }

  public int getOuterCircleTarget() {
    return outerCircleTarget;
  }

  public void setOuterCircleTarget(int outerCircleTargetSize) {
    this.outerCircleTarget = outerCircleTargetSize;
  }

  public void signalVoterNominated(PeerIdentity id) {
    nomineeCounter++;
  }
  
  public boolean isPollActive() {
    return activePoll;
  }
  
  public boolean isPollCompleted() {
    return !activePoll;
  }
  
  public void setActivePoll(boolean b) {
    this.activePoll = b;
  }

  /**
   * Return true if the V3Poller should proceed to request votes.
   *
   * @return true if the poller should proceed to requesting votes.
   */
  public boolean sufficientPeers() {
    return nomineeCounter >= getQuorum();
  }

  /**
   * Return the ordered list of hashed blocks.
   * @deprecated
   */
  public ArrayList getHashedBlocks() {
    throw new UnsupportedOperationException("getHashedBlocks is no longer "
                                            + "implemented.");
  }

  /**
   * @deprecated 
   */
  public void addHashBlock(HashBlock hb) {
    throw new UnsupportedOperationException("addHashBlock is no longer "
                                            + "implemented.");
  }
  
  public String toString() {
    StringBuffer sb = new StringBuffer("[V3PollerState: ");
    sb.append("pollKey=" + pollKey + ", ");
    sb.append("deadline=" + pollDeadline + "]");
    return sb.toString();
  }
  
  /**
   * @deprecated Use getStatus instead.
   * @return The status of this poll, as a string.
   */
  public String getStatusString() {
    return statusString;
  }

  /**
   * Set the status of this poll
   * @deprecated Use setStatus instead.
   */
  public void setStatusString(String s) {
    this.statusString = s;
  }

  /**
   * Return the status of this poll.
   */
  public int getStatus() {
    return status;
  }
  
  public boolean expectingRepairs() {
    return repairQueue.expectingRepairs();
  }

  /**
   * Set the status of this poll.
   *
   * @param s Current status
   */
  public void setStatus(int s) {
    this.status = s;
  }

  public RepairQueue getRepairQueue() {
    return repairQueue;
  }

  public TallyStatus getTallyStatus() {
    return tallyStatus;
  }

  public void setErrorDetail(String err) {
    this.errorDetail = err;
  }
  
  public String getErrorDetail() {
    return errorDetail;
  }

  public void setAdditionalInfo(String info) {
    this.additionalInfo = info;
  }
  
  public String getAdditionalInfo () {
    return additionalInfo;
  }

  /**
   * Simple object to hold tally status.
   */
  public static class TallyStatus implements LockssSerializable {
    private Set agreedUrls;
    private Set disagreedUrls;
    private Set tooCloseUrls;
    private Set noQuorumUrls;
    private Map<String,String> errorUrls;

    public TallyStatus() {
      agreedUrls = new HashSet();
      disagreedUrls = new HashSet();
      tooCloseUrls = new HashSet();
      noQuorumUrls = new HashSet();
      errorUrls = new HashMap();
    }

    public synchronized void addAgreedUrl(String url) {
      removeUrl(url);
      agreedUrls.add(url);
    }

    public synchronized void addDisagreedUrl(String url) {
      removeUrl(url);
      disagreedUrls.add(url);
    }

    public synchronized void addTooCloseUrl(String url) {
      removeUrl(url);
      tooCloseUrls.add(url);
    }

    public synchronized void addNoQuorumUrl(String url) {
      removeUrl(url);
      noQuorumUrls.add(url);
    }
    
    public synchronized void addErrorUrl(String url, Throwable t) {
      removeUrl(url);
      errorUrls.put(url, t.getMessage());
    }

    private synchronized void removeUrl(String url) {
      agreedUrls.remove(url);
      disagreedUrls.remove(url);
      tooCloseUrls.remove(url);
      noQuorumUrls.remove(url);
      errorUrls.remove(url);
    }

    /* Getters are copy-on-read */

    public synchronized Set getAgreedUrls() {
      if (agreedUrls == null) {
        return new HashSet();
      } else {
        return new HashSet(agreedUrls);
      }
    }
    
    public synchronized Set getDisagreedUrls() {
      if (disagreedUrls == null) {
        return new HashSet();
      } else {
        return new HashSet(disagreedUrls);
      }
    }
    
    public synchronized Set getTooCloseUrls() {
      if (tooCloseUrls == null) {
        return new HashSet();
      } else {
        return new HashSet(tooCloseUrls);
      }
    }
    
    public synchronized Set getNoQuorumUrls() {
      if (noQuorumUrls == null) {
        return new HashSet();
      } else {
        return new HashSet(noQuorumUrls);
      }
    }
    
    public synchronized Map getErrorUrls() {
      if (errorUrls == null) {
        return new HashMap();
      } else {
        return new HashMap(errorUrls);
      }
    }
  }

  /**
   * Simple object to hold status for a repair.
   */
  public static class Repair implements LockssSerializable {
    /** The URL of the block being repaired. */
    protected final String url;
    /** Peer from which to request a repair. If null, this is a 
     * publisher repair. */
    protected final PeerIdentity repairFrom;
    /** Result of tallying the obtained repair.  May be null if no repair
     * was fetched (or if restored from saved state pre-1.65) */
    protected BlockTally.Result tallyResult;

    /**
     * Create a publisher repair object.
     * 
     * @param url
     */
    public Repair(String url) {
      this(url, null);
    }
    
    /**
     * Create a peer repair object.
     * 
     * @param url
     * @param repairFrom
     */
    public Repair(String url, PeerIdentity repairFrom) {
      this.url = url;
      this.repairFrom = repairFrom;
    }

    public PeerIdentity getRepairFrom() {
      return repairFrom;
    }

    public String getUrl() {
      return url;
    }

    public boolean isPublisherRepair() {
      return (repairFrom == null);
    }

    protected void setTallyResult(BlockTally.Result tallyResult) {
      this.tallyResult = tallyResult;
    }

    public BlockTally.Result getTallyResult() {
      return tallyResult;
    }
  }

  /**
   * Collection of repairs: pending, active, and completed.
   */
  
  public static class RepairQueue implements LockssSerializable {
    private final int maxRepairs;
    private final Map<String, Repair> pendingRepairs;
    private final Map<String, Repair> activeRepairs;
    private final List<Repair> completedRepairs;
    private int	unrepaired = 0;

    
    public RepairQueue(int maxRepairs) {
      this.maxRepairs = maxRepairs < 0 ? Integer.MAX_VALUE : maxRepairs;
      this.pendingRepairs = new HashMap();
      this.activeRepairs = new HashMap();
      this.completedRepairs = new ArrayList();
    }

    /**
     * @return The total number of requests which have been made.
     */
    public synchronized int size() {
      return pendingRepairs.size() + activeRepairs.size() + 
	completedRepairs.size();
    }

    // todo(bhayes): simply cutting off repairs after maxRepairs biases
    // repairs to URL listed early. URLs which are down the list may
    // never be repaired.

    /** @return true iff we haven't queued more than the max allowed
     * number of requests.
     */
    public boolean okToQueueRepair() {
      return size() < maxRepairs;
    }

    /**
     * Enqueue a pending request to repair from the publisher.
     */
    public synchronized void repairFromPublisher(String url) {
      pendingRepairs.put(url, new Repair(url));
    }
    
    /**
     * Enqueue a pending request to repair from this peer.
     */
    public synchronized void repairFromPeer(String url, 
                                            PeerIdentity peer) {
      pendingRepairs.put(url, new Repair(url, peer));
    }
    
    /**
     * @return the URLs of the pending publisher repairs.
     */
    public synchronized List<String> getPendingPublisherRepairUrls() {
      List<String> publisherRepairs = new ArrayList();
      for (Repair r: pendingRepairs.values()) {
        if (r.isPublisherRepair()) {
          publisherRepairs.add(r.getUrl());
        }
      }
      return publisherRepairs;
    }
    
    /**
     * @return a list of all pending repairs that should be fetched from
     * other V3 peers.
     */
    public synchronized List<Repair> getPendingPeerRepairs() {
      List<Repair> peerRepairs = new ArrayList();
      for (Repair r: pendingRepairs.values()) {
        if (!r.isPublisherRepair()) {
          peerRepairs.add(r);
        }
      }
      return peerRepairs;
    }

    /**
     * @return true if any repairs are pending or active.
     */
    public boolean expectingRepairs() {
      return !pendingRepairs.isEmpty() || !activeRepairs.isEmpty();
    }

    /**
     * Mark an entire list of URls active.
     * 
     * @param urls The list of URLs to move to the active state.
     */
    public synchronized void markActive(List<String> urls) {
      for (String url: urls) {
        markActive(url);
      }
    }

    /**
     * Mark a URL active.
     * 
     * @param url The URL to move to the active state.
     */
    public synchronized void markActive(String url) {
      Repair r = pendingRepairs.remove(url);
      if (r != null) {
	activeRepairs.put(url, r);
      }
    }
    
    /**
     * Mark a URL completed.
     * 
     * @param url The URL to move to the completed state.
     * @param tallyResult The disposition of the repair.
     */
    public synchronized void markComplete(String url,
					  BlockTally.Result tallyResult) {
      Repair r = activeRepairs.remove(url);
      if (r != null) {
	r.setTallyResult(tallyResult);
        completedRepairs.add(r);
      }
      switch (tallyResult) {
      case WON:
      case LOST_POLLER_ONLY_BLOCK:
	break;
      case LOST:
      case TOO_CLOSE:
	unrepaired++;
	break;
      case LOST_VOTER_ONLY_BLOCK:
      case NOQUORUM:
	log.warning("Shouldn't happen: " + tallyResult.printString +
		    " after repair: " + url);
	break;
      default:
	log.warning("Unexpected result from post-repair tally: " +
		    tallyResult.printString + ": " +
		    url);
      }
    }
    
    // The following methods are for the use of V3PollStatus.
    public synchronized List<PollerStateBean.Repair> getPendingRepairs() {
      return new ArrayList(pendingRepairs.values());
    }

    public synchronized List<PollerStateBean.Repair> getActiveRepairs() {
      return new ArrayList(activeRepairs.values());
    }
    
    public synchronized List<PollerStateBean.Repair> getCompletedRepairs() {
      return completedRepairs;
    }

    public synchronized int getNumUnrepaired() {
      return pendingRepairs.size() + activeRepairs.size() + unrepaired;
    }

    public synchronized int getNumFailedRepair() {
      return pendingRepairs.size() + activeRepairs.size() + unrepaired;
    }
  }

  /**
   * Release resources used by this object that are no longer required after
   * the end of a poll.
   */
  public void release() {
    hashAlgorithm = null;
    hashedBlocks = null;
    spec = null;
    votedPeers = null;
  }
}
