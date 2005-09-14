/*
 * $Id: V3Poller.java,v 1.4 2005-09-14 23:57:49 smorabito Exp $
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

import java.util.*;
import java.security.*;
import org.lockss.daemon.*;
import org.lockss.plugin.*;
import org.lockss.util.*;
import org.lockss.config.*;
import org.lockss.poller.*;
import org.lockss.protocol.*;
import org.lockss.protocol.psm.*;
import org.lockss.app.*;
import org.lockss.hasher.*;
import org.mortbay.util.B64Code;

/**
 * Class that runs a V3 Poll.
 */

public class V3Poller {

  private static String PREFIX = Configuration.PREFIX + "poll.v3.";

  /** Minimum number of participants for a V3 poll. */
  public static String PARAM_MIN_POLL_SIZE = PREFIX + "minPollSize";
  public static int DEFAULT_MIN_POLL_SIZE = 4;

  /** Maximum number of participants for a V3 poll. */
  public static String PARAM_MAX_POLL_SIZE = PREFIX + "maxPollSize";
  public static int DEFAULT_MAX_POLL_SIZE = 10;

  private PollerStateBean pollerState;
  protected Map innerCircle;
  protected Map outerCircle;
  private LockssDaemon theDaemon;
  private boolean continuedPoll = false;
  private V3PollerSerializer serializer;
  private int pollSize;

  private static Logger log = Logger.getLogger("V3Poller");
  private static LockssRandom theRandom = new LockssRandom();

  /**
   * Common constructor.
   * 
   * @param key
   */
  protected V3Poller() throws V3Serializer.PollSerializerException {
    innerCircle = new LinkedHashMap();
    outerCircle = new LinkedHashMap();
    serializer = new V3PollerSerializer();
  }

  /**
   * Create a new V3 Poll.
   */
  public V3Poller(PollSpec spec, LockssDaemon daemon, PeerIdentity orig,
                  String key, long duration, String hashAlg)
      throws V3Serializer.PollSerializerException {
    this();
    // Determine the number of participants for this poll.
    int minParticipants = Configuration.getIntParam(PARAM_MIN_POLL_SIZE,
                                                    DEFAULT_MIN_POLL_SIZE);
    int maxParticipants = Configuration.getIntParam(PARAM_MAX_POLL_SIZE,
                                                    DEFAULT_MAX_POLL_SIZE);
    if (maxParticipants < minParticipants) {
      throw new IllegalArgumentException("Impossible poll size range: "
          + (maxParticipants - minParticipants));
    } else if (maxParticipants == minParticipants) {
      log.debug("Max poll size and min poll size are identical");
      pollSize = minParticipants;
    } else {
      // Pick a random number of participants for this poll between
      // minParticipants and maxParticipants
      LockssRandom rand = new LockssRandom();
      int randCount = rand.nextInt(maxParticipants - minParticipants);
      pollSize = minParticipants + randCount;
    }
    this.pollerState = new PollerStateBean(spec, orig, key, duration,
                                           pollSize, hashAlg,
                                           serializer);
    this.theDaemon = daemon;
    log.debug("Creating a new poll with a requested poll size of "
              + pollSize);
  }

  /**
   * Restore a V3 Poll from a serialized state.
   */
  public V3Poller(LockssDaemon daemon, String pollDir)
      throws V3Serializer.PollSerializerException {
    this();
    this.theDaemon = daemon;
    this.serializer = new V3PollerSerializer(pollDir);
    this.pollerState = serializer.loadPollerState();
    // Restore transient CUS and PollSerializer in poll state
    PluginManager plugMgr = theDaemon.getPluginManager();
    CachedUrlSet cus = plugMgr.findCachedUrlSet(pollerState.getAuId());
    if (cus == null) {
      throw new NullPointerException("CUS for AU " + pollerState.getAuId() +
                                     " is null!");
    }
    this.pollerState.setCachedUrlSet(cus);
    this.continuedPoll = true;
    log.debug2("Restored serialized poll " + pollerState.getPollKey());
  }

  public void startPoll() {
    // Schedule the poll.
    log.debug("Scheduling V3 poll " + pollerState.getPollKey() +
              " to complete by " + pollerState.getDeadline());

    TimerQueue.schedule(Deadline.at(pollerState.getDeadline()),
                        new PollTimerCallback(), this);

    if (continuedPoll) {
      restoreInnerCircle();
    } else {
      constructInnerCircle();
    }

    pollInnerCircle();
  }

  // XXX: Not clear what needs be done here, yet. TBD.
  public void stopPoll() {
    log.debug("Stopping poll " + pollerState.getPollKey());
  }

  /**
   * Called when the poll should be closed. This method cleans up resources used
   * by the poll.
   */
  public void closePoll() {
    // Clean up after the serializer.
    this.serializer.closePoll();
  }

  /**
   * Construct the inner circle of voters.
   */
  void constructInnerCircle() {
    Collection refList = getReferenceList();
    Collection selectedVoters = CollectionUtil
        .randomSelection(refList, ((pollSize > refList.size()) ? refList
            .size() : pollSize));
    log.debug2("Selected " + selectedVoters.size()
        + " participants for poll ID " + pollerState.getPollKey());

    for (Iterator it = selectedVoters.iterator(); it.hasNext();) {
      PeerIdentity id = (PeerIdentity) it.next();
      PollerUserData ud = 
        new PollerUserData(id, this, serializer);
      ud.setPollerNonce(makePollerNonce());
      PsmMachine machine = PollerStateMachineFactory
          .getMachine(getPollerActionsClass());
      V3PollerInterp interp = new V3PollerInterp(machine, ud, serializer);
      innerCircle.put(id, interp);
    }
  }

  /**
   * Generate a random nonce for the poller.
   * 
   * @return A random array of 20 bytes.
   */
  private byte[] makePollerNonce() {
    byte[] secret = new byte[20];
    theRandom.nextBytes(secret);
    return secret;
  }

  /**
   * Reconstruct the inner circle list for a restored poll.
   */
  void restoreInnerCircle() {
    try {
      List innerCircleStates = serializer.loadInnerCircleStates();
      for (Iterator iter = innerCircleStates.iterator(); iter.hasNext();) {
        PollerUserData voterState = (PollerUserData) iter.next();
        PsmMachine machine = PollerStateMachineFactory.getMachine(getPollerActionsClass());
        V3PollerInterp interp =
          new V3PollerInterp(machine, voterState, 
                             serializer.loadPollerInterpState(voterState.getVoterId()), 
                             serializer);
        innerCircle.put(voterState.getVoterId(), interp);
      }
    } catch (V3Serializer.PollSerializerException ex) {
      log.error("Unable to restore poll state!");
      stopPoll();
    }
  }

  /**
   * Begin (or resume) polling the inner circle of voters.
   */
  private void pollInnerCircle() {
    for (Iterator it = innerCircle.values().iterator(); it.hasNext();) {
      V3PollerInterp interp = (V3PollerInterp) it.next();
      interp.init();
    }
  }

  private void constructOuterCircle() {
    // XXX: TBD
  }

  private void restoreOuterCircle() {
    // XXX: TBD
  }

  private void pollOuterCircle() {
    for (Iterator it = outerCircle.values().iterator(); it.hasNext();) {
      V3PollerInterp interp = (V3PollerInterp) it.next();
      interp.init();
    }
  }

  void nominatePeers(PeerIdentity id, List nominatedPeers) {
    // XXX: When enough nominations have been received, create
    // and poll the outer circle. TBD.
    log.debug("Received nominations from peer: " + id + "; Nominations = "
        + nominatedPeers);
  }

  /**
   * Callback method called by each PollerStateMachine when entering the
   * TallyVoter state.
   */
  boolean tallyIfReady(PeerIdentity id) {
    pollerState.readyToHash(true);
    if (pollerState.readyToHash()) {
      log.debug2("Ready to schedule hash.");
      try {
        if (!scheduleHash()) {
          // XXX: Better error handling / retrying
          log.error("No time available to schedule our hash for poll "
              + pollerState.getPollKey());
          stopPoll();
          return false;
        }
      } catch (NoSuchAlgorithmException ex) {
        log.error("Hash algorithm not available while scheduling hash: " + ex);
        stopPoll();
        return false;
      }
    } else {
      log.debug2("Not all voters are ready to hash.");
    }
    return true;
  }

  /**
   * Schedule hashing of the AU.
   * 
   * @throws NoSuchAlgorithmException if the hasher attempts to use a hash
   *           algorithm that is not available.
   */
  protected boolean scheduleHash() throws NoSuchAlgorithmException {
    log.debug("Scheduling our hash for poll " + pollerState.getPollKey());

    Deadline deadline = Deadline.at(pollerState.getDeadline());
    BlockHasher hasher = new BlockHasher(pollerState.getCachedUrlSet(),
                                         initHasherDigests(),
                                         initHasherByteArrays(),
                                         new BlockCompleteHandler());

    HashService hashService = theDaemon.getHashService();
    return hashService.scheduleHash(hasher, deadline,
                                    new HashingCompleteCallback(), null);
  }

  /**
   * Create an array of byte arrays containing hasher initializer bytes, one for
   * each participant in the poll. The initializer bytes are constructed by
   * concatenating the participant's poller nonce and the voter nonce.
   * 
   * @return Block hasher initialization bytes.
   */
  byte[][] initHasherByteArrays() {
    int len = innerCircle.size();
    byte[][] initBytes = new byte[len][];
    int ix = 0;
    for (Iterator it = innerCircle.values().iterator(); it.hasNext();) {
      V3PollerInterp interp = (V3PollerInterp) it.next();
      PollerUserData ud = (PollerUserData)interp.getUserData();
      initBytes[ix++] = ByteArray.concat(ud.getPollerNonce(), ud
          .getVoterNonce());
    }
    return initBytes;
  }

  /**
   * Create an array of message digests, one for each participant in the poll.
   * This array is guaranteed to be in the same order as the inner circle
   * iterator and the array of byte arrays returned by
   * {@link #initHasherByteArrays()}
   * 
   * @return An array of MessageDigest objects to be used by the BlockHasher.
   */
  MessageDigest[] initHasherDigests() throws NoSuchAlgorithmException {
    String hashAlg = pollerState.getHashAlgorithm();
    if (hashAlg == null) {
      hashAlg = LcapMessage.DEFAULT_HASH_ALGORITHM;
    }
    int len = innerCircle.size();
    MessageDigest[] digests = new MessageDigest[len];
    for (int ix = 0; ix < len; ix++) {
      digests[ix] = MessageDigest.getInstance(hashAlg);
    }
    return digests;
  }

  /**
   * Signal the hash service to resume its hash, starting with the vote block
   * specifed.
   * 
   * XXX: TBD.
   */
  protected void resumeHash(String target) {
    log.debug("Resuming hash for next block after: " + target);
  }

  /**
   * Called by the BlockHasher's event handler callback when hashing is complete
   * for one block.
   * 
   * For each block in the hash, we compare our hash against all participant
   * hashes and build up a tally. The tally is then examined to determine what
   * to do next.
   * 
   * XXX: Multiple vote message mechanism TBD!
   * 
   */
  private void blockHashComplete(HashBlock block) {
    log.debug("Hashing for block " + block.getUrl()
        + " complete, now tallying.");
    String targetUrl = block.getUrl();

    // Store the most recent block targetUrl in the state bean
    pollerState.setLastHashedBlock(targetUrl);

    // Compare with each participant's hash for this vote block,
    // then tally that participant's ID as either agree or disagree.

    BlockTally tally = tallyBlock(block);
    int result = tally.getResult();

    if (result == BlockTally.RESULT_WON) {
      // Great, we won! Signal the HashService to resume hashing
      // the next block. (TBD)
      log.debug2("Won poll for block " + targetUrl);
      resumeHash(targetUrl);
    } else if (result == BlockTally.RESULT_LOST) {
      // Pick one or more random disagreeing voters from whom to request
      // a repair. Add those voters to the list of participants NOT in
      // Tallying state.
      PeerIdentity disagreeingVoter = (PeerIdentity) CollectionUtil
          .randomSelection(tally.getDisagreeVotes());
      // Decrement the 'ready for hash' counter
      pollerState.readyToHash(false);

      log.debug2("Requesting a repair from voter: " + disagreeingVoter);
      // XXX: Suspend hash.  Repair mechanism TBD
      // Repair mechanism must signal Hash Service to resume hashing
      // the next block.
    } else if (result == BlockTally.RESULT_TOO_CLOSE) {
      log.warning("Tally was inconclusive.  Stopping poll "
          + pollerState.getPollKey());
      // XXX: Alerts?
      stopPoll();
    } else if (result == BlockTally.RESULT_NOQUORUM) {
      log.warning("No quorum.  Stopping poll " + pollerState.getPollKey());
      // XXX: Alerts?
      stopPoll();
    } else {
      log.warning("Unexpected results from tallying block " + targetUrl + ": "
          + tally.getResultString());
      stopPoll();
    }
  }

  /**
   * Given a HashBlock result from the hasher, compare its hash (generated by
   * us) with the hash in the VoteBlock from each participant (generated by the
   * voters).
   * 
   * @param block The hashblock being compared.
   * @return A tally for the block.
   */
  protected BlockTally tallyBlock(HashBlock block) {
    BlockTally tally = new BlockTally();
    String blockUrl = block.getUrl();
    log.debug2("Tallying block: " + blockUrl);
    MessageDigest[] digests = block.getDigests();
    int ix = 0;
    for (Iterator iter = innerCircle.values().iterator(); iter.hasNext();) {
      // XXX:  This needs to change for daemon 1.12.
      //       What if a given voter has fewer blocks than the poller does?
      //       The logic here needs to be more complex.  If we run out of
      //       blocks for a voter, we need to:
      //
      //       1) break out of the tally for this block.
      //       2) call pollerState.readyForHash(false)
      //          and
      //       3) send another VoteRequest to that voter.
      //       
      //       For now, assume everyone has sent us all the vote blocks they
      //       have for this CUS.

      PollerUserData voterState = (PollerUserData)((V3PollerInterp)iter.next()).getUserData();
      PeerIdentity voter = voterState.getVoterId();
      VoteBlock vb = voterState.getVoteBlocks().getVoteBlock(ix);
      byte[] voterResults = vb == null ? null : vb.getHash();
      byte[] hasherResults = digests[ix].digest();
      ix++;
      if (log.isDebug3()) {
        log.debug3("Comparing hashes for participant " + voter);
        log.debug3("Hasher results: "
            + String.valueOf(B64Code.encode(hasherResults)));
        log.debug3("Voter results: "
            + (voterResults == null ? "null" : String.valueOf(B64Code
                .encode(voterResults))));
      }
      if (voterResults == null) {
        log.debug2("Results disagree:  Voter does not have block " + blockUrl);
        tally.addDisagreeVote(voter);
      } else if (Arrays.equals(voterResults, hasherResults)) {
        log.debug2("Results agree: Poller and Voter have the same hash");
        tally.addAgreeVote(voter);
      } else {
        log.debug2("Results disagree: Voter has a different hash "
            + "than the poller");
        tally.addDisagreeVote(voter);
      }
    }
    tally.tallyVotes();
    return tally;
  }

  /**
   * Called by the HashService callback when all hashing is complete.
   */
  private void hashComplete() {
    log.debug("Hashing is complete for this CUS.");
    for (Iterator iter = innerCircle.values().iterator(); iter
        .hasNext();) {
      V3PollerInterp interp = (V3PollerInterp) iter.next();
      interp.handleEvent(V3Events.evtVoteComplete);
      if (log.isDebug2()) {
        PollerUserData ud = (PollerUserData)interp.getUserData();
        log.debug2("Gave peer " + ud.getVoterId()
                   + " the Vote Complete event.");
      }
    }
  }

  // XXX: Implement! Override for testing.
  public void sendMessageTo(V3LcapMessage msg, PeerIdentity to) {
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
        log.warning("Poll hash failed : " + e.getMessage());
        stopPoll();
      }
    }
  }

  class BlockCompleteHandler implements BlockHasher.EventHandler {
    public void blockDone(HashBlock block) {
      blockHashComplete(block);
    }
  }

  /**
   * Handle an incoming V3LcapMessage.
   * 
   */
  public void handleMessage(V3LcapMessage msg) {
    PeerIdentity sender = msg.getOriginatorId();
    V3PollerInterp interp = (V3PollerInterp) innerCircle.get(sender);
    if (interp != null) {
      PsmMsgEvent evt = V3Events.fromMessage(msg);
      interp.handleEvent(evt);
    } else {
      log.debug("No voter state for peer: " + msg.getOriginatorId());
    }
  }

  public String getPollKey() {
    return pollerState.getPollKey();
  }
  
  public PollerStateBean getPollerStateBean() {
    return pollerState;
  }
  
  Collection getReferenceList() {
    // XXX: Override for testing, TBD.
    return new ArrayList();
  }

  Class getPollerActionsClass() {
    return PollerActions.class;
  }

  class PollTimerCallback implements TimerQueue.Callback {
    /**
     * Called when the timer expires.
     * 
     * @param cookie data supplied by caller to schedule()
     */
    public void timerExpired(Object cookie) {
      stopPoll();
    }
  }
}
