/*
 * $Id: PollerStateBean.java,v 1.6 2005-10-11 05:45:39 tlipkis Exp $
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

import java.util.*;

import org.lockss.plugin.*;
import org.lockss.poller.*;
import org.lockss.protocol.*;
import org.lockss.util.*;

/**
 * Persistant state object for the V3Poller.
 */
public class PollerStateBean implements LockssSerializable {

  private LcapMessage pollMessage;
  private String pollKey;
  private long deadline;
  private String auId;
  private String pluginVersion;
  private int pollVersion;
  private String url;
  private PeerIdentity pollerId;
  private String hashAlgorithm;
  private long createTime;
  private int quorum;
  private int currentDigestIndex;
  private int maxNomineeCount;
  private HashSet outerCircleVoters;

  /* Non-serializable transient fields */
  private transient PollSpec spec;
  private transient CachedUrlSet cus;
  private transient V3PollerSerializer serializer;

  private static Logger log = Logger.getLogger("PollerStateBean");

  /**
   * Counter of participants whose state machines do not want to allow the next
   * block to hash. When the poller checks to see if it can start hashing the
   * next block, it will consult this counter and only proceed if it is '0'.
   */
  private volatile int hashReadyCounter;

  /**
   * Counter of participants who have not yet nominated any peers.
   */
  private volatile int nomineeCounter;

  /**
   * The target URL of the most recently hashed block. Updated after each block
   * is hashed and tallied by V3Poller. Used when returning to tally more blocks
   * after requesting a repair, or when sending a vote request for the next in a
   * sequence of votes.
   */
  private String lastHashedBlock;

  /**
   * Package-level constructor used for testing.
   */
  PollerStateBean(V3PollerSerializer serializer) {
    this.serializer = serializer;
  }

  public PollerStateBean(PollSpec spec, PeerIdentity orig, String pollKey,
                         long duration, int pollSize, int maxNomineeCount,
                         int quorum, String hashAlg, V3PollerSerializer serializer) {
    this.pollerId = orig;
    this.pollKey = pollKey;
    this.deadline = Deadline.in(duration).getExpirationTime();
    this.hashReadyCounter = pollSize;
    log.debug3("hashReadyCounter: " + hashReadyCounter);
    this.nomineeCounter = pollSize;
    this.maxNomineeCount = maxNomineeCount;
    this.auId = spec.getAuId();
    this.pollVersion = spec.getPollVersion();
    this.pluginVersion = spec.getPluginVersion();
    this.url = spec.getUrl();
    this.cus = spec.getCachedUrlSet();
    this.spec = spec;
    this.hashAlgorithm = hashAlg;
    this.serializer = serializer;
    this.createTime = TimeBase.nowMs();
    this.quorum = quorum;
    this.currentDigestIndex = 0;
    this.outerCircleVoters = new HashSet();
    saveState();
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

  public void setPollKey(String id) {
    this.pollKey = id;
    saveState();
  }

  public String getPollKey() {
    return pollKey;
  }

  public void setPollerId(PeerIdentity pollerId) {
    this.pollerId = pollerId;
    saveState();
  }

  public PeerIdentity getPollerId() {
    return pollerId;
  }

  public void setLastHashedBlock(String target) {
    this.lastHashedBlock = target;
    saveState();
  }

  public String getLastHashedBlock() {
    return lastHashedBlock;
  }

  public void setAuId(String auId) {
    this.auId = auId;
    saveState();
  }

  public String getAuId() {
    return auId;
  }

  public void setPollVersion(int pollVersion) {
    this.pollVersion = pollVersion;
    saveState();
  }

  public int getPollVersion() {
    return pollVersion;
  }

  public void setPluginVersion(String pluginVersion) {
    this.pluginVersion = pluginVersion;
    saveState();
  }

  public String getPluginVersion() {
    return pluginVersion;
  }

  public void setUrl(String url) {
    this.url = url;
    saveState();
  }

  public String getUrl() {
    return url;
  }

  public void setCachedUrlSet(CachedUrlSet cus) {
    this.cus = cus;
    // Transient - no need to save state
  }

  public CachedUrlSet getCachedUrlSet() {
    return cus;
  }

  public void setPollSpec(PollSpec spec) {
    this.spec = spec;
    // Transient - no need to save state.
  }

  public PollSpec getPollSpec() {
    return spec;
  }

  public void setHashAlgorithm(String hashAlgorithm) {
    this.hashAlgorithm = hashAlgorithm;
    saveState();
  }

  public String getHashAlgorithm() {
    return hashAlgorithm;
  }

  public void setSerializer(V3PollerSerializer serializer) {
    this.serializer = serializer;
    // Transient - no need to save state
  }

  public void setDeadline(long l) {
    this.deadline = l;
    saveState();
  }

  public long getDeadline() {
    return deadline;
  }

  public int getQuorum() {
    return quorum;
  }

  public void setQuorum(int quorum) {
    this.quorum = quorum;
  }

  public int getMaxNomineeCount() {
    return maxNomineeCount;
  }

  public void setMaxNomineeCount(int maxNomineeCount) {
    this.maxNomineeCount = maxNomineeCount;
  }

  public int getNextVoteBlockIndex() {
    int idx = currentDigestIndex++;
    saveState();
    return idx;
  }

  public synchronized void addOuterCircle(PeerIdentity id) {
    if (outerCircleVoters.add(id)) {
      signalVoterAdded(id);
    }
  }

  public synchronized void removeOuterCircle(PeerIdentity id) {
    if (outerCircleVoters.remove(id)) {
      signalVoterRemoved(id);
    }
  }

  public void signalVoterNominated(PeerIdentity id) {
    nomineeCounter--;
    log.debug3("signalVoterNominated: nomineeCounter=" + nomineeCounter);
    saveState();
  }

  public boolean allVotersNominated() {
    return nomineeCounter == 0;
  }

  public void signalVoterRemoved(PeerIdentity id) {
    log.debug("signalVoterRemoved being called.", new Throwable("stacktrace"));
    if (!isInOuterCircle(id)) {
      nomineeCounter--; // No longer expect any nominees from this peer.
    }
    hashReadyCounter--; // No longer expect this peer to take part in hashing.
    log.debug3("signalVoterRemoved: hashReadyCounter=" + hashReadyCounter);
    saveState();
  }

  public void signalVoterAdded(PeerIdentity id) {
    hashReadyCounter++;
    log.debug3("signalVoterAdded: hashReadyCounter=" + hashReadyCounter);
  }

  public void signalVoterReadyToTally(PeerIdentity id) {
    hashReadyCounter--;
    log.debug3("signalVoterReadyToTally: hashReadyCounter=" + hashReadyCounter);
    saveState();
  }

  public void signalVoterNotReadyToTally(PeerIdentity id) {
    hashReadyCounter++;
    log.debug3("signalVoterNotReadyToTally: hashReadyCounter=" + hashReadyCounter);
    saveState();
  }

  public boolean allVotersReadyToTally() {
    return hashReadyCounter == 0;
  }

  public synchronized boolean isInOuterCircle(PeerIdentity id) {
    return outerCircleVoters.contains(id);
  }

  public synchronized Iterator getOuterCircleIterator() {
    return outerCircleVoters.iterator();
  }

  public String toString() {
    StringBuffer sb = new StringBuffer("[V3PollerState: ");
    sb.append("pollKey=" + pollKey + ", ");
    sb.append("deadline=" + deadline + "]");
    return sb.toString();
  }

  /**
   * Store the current V3VoterState
   */
  private void saveState() {
    try {
      serializer.savePollerState(this);
    } catch (V3Serializer.PollSerializerException ex) {
      log.error("Unable to save poller state for poll " +
                pollKey, ex);
    }
  }
}
