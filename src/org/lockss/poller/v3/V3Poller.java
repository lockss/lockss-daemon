/*
 * $Id: V3Poller.java,v 1.13 2005-11-08 19:24:23 smorabito Exp $
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
import java.security.*;
import java.util.*;

import org.lockss.app.*;
import org.lockss.config.*;
import org.lockss.daemon.*;
import org.lockss.hasher.*;
import org.lockss.plugin.*;
import org.lockss.poller.*;
import org.lockss.protocol.*;
import org.lockss.protocol.psm.*;
import org.lockss.util.*;

/**
 * Class that runs a V3 Poll.
 */

public class V3Poller extends BasePoll {

  private static String PREFIX = Configuration.PREFIX + "poll.v3.";

  /** Quorum for V3 polls. */
  static final String PARAM_QUORUM = PREFIX + "quorum";
  static final int DEFAULT_QUORUM = 5;

  /** Minimum number of participants for a V3 poll. */
  public static String PARAM_MIN_POLL_SIZE = PREFIX + "minPollSize";
  public static int DEFAULT_MIN_POLL_SIZE = 5;

  /** Maximum number of participants for a V3 poll. */
  public static String PARAM_MAX_POLL_SIZE = PREFIX + "maxPollSize";
  public static int DEFAULT_MAX_POLL_SIZE = 10;

  /** Maximum number of peers to nominate from each inner circle member. */
  public static String PARAM_MAX_NOMINATION_SIZE =
    V3Voter.PARAM_MAX_NOMINATION_SIZE;
  public static int DEFAULT_MAX_NOMINATION_SIZE =
    V3Voter.DEFAULT_MAX_NOMINATION_SIZE;

  private PollerStateBean pollerState;
  /** Map of all voters in this poll, both inner and outer circle */
  protected LinkedHashMap theVoters;
  private LockssDaemon theDaemon;
  private PollManager pollManager;
  private IdentityManager idManager;
  private boolean continuedPoll = false;
  private V3PollerSerializer serializer;
  private int pollSize;
  private BlockTally tally;

  private static Logger log = Logger.getLogger("V3Poller");
  private static LockssRandom theRandom = new LockssRandom();

  /**
   * Create a new V3 Poll.
   */
  public V3Poller(PollSpec spec, LockssDaemon daemon, PeerIdentity orig,
                  String key, long duration, String hashAlg)
      throws V3Serializer.PollSerializerException {
    // Determine the number of peers to invite into this poll.
    int minParticipants = Configuration.getIntParam(PARAM_MIN_POLL_SIZE,
                                                    DEFAULT_MIN_POLL_SIZE);
    int maxParticipants = Configuration.getIntParam(PARAM_MAX_POLL_SIZE,
                                                    DEFAULT_MAX_POLL_SIZE);
    int maxNomineeCount = Configuration.getIntParam(PARAM_MAX_NOMINATION_SIZE,
                                                    DEFAULT_MAX_NOMINATION_SIZE);
    int quorum = Configuration.getIntParam(PARAM_QUORUM, DEFAULT_QUORUM);
    if (minParticipants < quorum) {
      throw new IllegalArgumentException("Cannot start a poll with minimum " +
                "size " + minParticipants + " because at least " + quorum +
                " participants are required for quorum");
    }
    if (maxParticipants < minParticipants) {
      throw new IllegalArgumentException("Impossible poll size range: "
          + (maxParticipants - minParticipants));
    } else if (maxParticipants == minParticipants) {
      log.debug("Max poll size and min poll size are identical");
      pollSize = minParticipants;
    } else {
      // Pick a random number of participants for this poll between
      // minParticipants and maxParticipants
      int randCount = theRandom.nextInt(maxParticipants - minParticipants);
      pollSize = minParticipants + randCount;
    }
    this.theDaemon = daemon;
    this.pollManager = daemon.getPollManager();
    this.idManager = daemon.getIdentityManager();
    this.serializer = new V3PollerSerializer(theDaemon);
    this.pollerState = new PollerStateBean(spec, orig, key, duration,
                                           pollSize, maxNomineeCount,
                                           quorum, hashAlg, serializer);
    this.tally =
      new BlockTally(this, pollerState.getCreateTime(),
                     pollerState.getDeadline(), 0, 0, pollerState.getQuorum(),
                     pollerState.getHashAlgorithm());
    log.debug("Creating a new poll with a requested poll size of "
              + pollSize);
    this.theVoters = new LinkedHashMap();
  }

  /**
   * Restore a V3 Poll from a serialized state.
   */
  public V3Poller(LockssDaemon daemon, String pollDir)
      throws V3Serializer.PollSerializerException {
    this.serializer = new V3PollerSerializer(daemon, pollDir);
    this.pollerState = serializer.loadPollerState();
    this.theDaemon = daemon;
    this.pollManager = daemon.getPollManager();
    this.idManager = daemon.getIdentityManager();
    this.tally =
      new BlockTally(this, pollerState.getCreateTime(),
                     pollerState.getDeadline(), 0, 0, pollerState.getQuorum(),
                     pollerState.getHashAlgorithm());
    // Restore transient cus, pollspec and serializer in poll state
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
      restoreVoters();
    } else {
      constructInnerCircle();
    }

    pollInnerCircle();
  }

  public void stopPoll() {
    log.debug("Stopping poll " + pollerState.getPollKey());
    // XXX: Stop all state machines -- there is currently no
    //      graceful way to do this.
    //
    // XXX: Anything else to do here?
    serializer.closePoll();
  }

  /**
   * Construct the inner circle of voters.
   */
  private void constructInnerCircle() {
    Collection refList = getReferenceList();
    int innerCount = pollSize > refList.size() ? refList.size() : pollSize;
    Collection innerCircleVoters =
      CollectionUtil.randomSelection(refList, innerCount);
    log.debug2("Selected " + innerCircleVoters.size()
        + " participants for poll ID " + pollerState.getPollKey());
    for (Iterator it = innerCircleVoters.iterator(); it.hasNext();) {
      addInnerCircleVoter((PeerIdentity)it.next());
    }
  }

  /**
   * Reconstruct the inner and outer circle list for a restored poll.
   */
  private void restoreVoters() {
    try {
      List innerCircleStates = serializer.loadVoterStates();
      for (Iterator iter = innerCircleStates.iterator(); iter.hasNext();) {
        PollerUserData voterState = (PollerUserData) iter.next();
        PeerIdentity id = voterState.getVoterId();
        PsmInterpStateBean pisb = serializer.loadPollerInterpState(id);
        PsmInterp interp = makeInterp(id, pisb);
        theVoters.put(id, interp);
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
    for (Iterator it = theVoters.values().iterator(); it.hasNext();) {
      PsmInterp interp = (PsmInterp) it.next();
      if (interp != null) {
	try {
	  interp.start();
	} catch (PsmException e) {
	  log.warning("State machine error", e);
	}
      }
    }
  }

  /**
   * Examine the nominees from each peer. Choose a fixed number
   * to allow from each peer.  If any peer has sent no nominees,
   * remove it from the poll.
   */
  private void pollOuterCircle() {
    log.debug2("Starting to poll the outer circle");
    Collection outerCircle = new ArrayList();
    int minCount = pollerState.getMaxNomineeCount();
    synchronized(theVoters) {
      // First pass: determine the correct number of nominees to invite
      // from each peer.
      for (Iterator it = theVoters.values().iterator(); it.hasNext(); ) {
        PsmInterp interp = (PsmInterp)it.next();
        if (interp != null) {
          PollerUserData ud = (PollerUserData)interp.getUserData();
          List nominees = ud.getNominees();
          if (nominees.size() < minCount)
            minCount = nominees.size();
        }
      }

      // Second pass: Randomly select 'minCount' peers from each voter's
      // nominee list.
      for (Iterator it = theVoters.values().iterator(); it.hasNext(); ) {
        PsmInterp interp = (PsmInterp)it.next();
        if (interp != null) {
          PollerUserData ud = (PollerUserData)interp.getUserData();
          List nominees = ud.getNominees();
          log.debug3("Randomly selecting " + minCount + " nominees from " +
                    "the set " + nominees);
          outerCircle.addAll(CollectionUtil.randomSelection(nominees, minCount));
        }
      }

      // Now start polling the outer circle.
      for (Iterator it = outerCircle.iterator(); it.hasNext(); ) {
        String idStr = (String) it.next();
        PeerIdentity id = idManager.findPeerIdentity(idStr);
        if (!theVoters.containsKey(id)) {
          log.debug2("Adding new peer " + id + " to the outer circle");
          PsmInterp interp = addOuterCircleVoter(id);
	  try {
	    interp.start();
	  } catch (PsmException e) {
	    log.warning("State machine error", e);
	  }
        } else {
          log.debug2("We already have peer " + id + " in our list of voters");
        }
      }
    }
  }

  /**
   * Add a voter to the inner circle of the poll.
   *
   * @param id
   * @return PsmInterp
   */
  private PsmInterp addInnerCircleVoter(PeerIdentity id) {
    PsmInterp interp = makeInterp(id);
    theVoters.put(id, interp);
    return interp;
  }

  /**
   * Add a voter to the outer circle of the poll.
   *
   * @param id
   */
  private PsmInterp addOuterCircleVoter(PeerIdentity id) {
    PsmInterp interp = makeInterp(id);
    theVoters.put(id, interp);
    pollerState.addOuterCircle(id);
    return interp;
  }

  /**
   * Create a PsmInterp for the specified peer.
   *
   * @param id
   * @return PsmInterp
   */
  private PsmInterp makeInterp(PeerIdentity id) {
    return makeInterp(id, null);
  }

  /**
   * Create a PsmInterp for the specified peer, giving it the supplied
   * PsmInterpStateBean.
   *
   * @param id
   * @param psmState
   * @return PsmInterp for the specified peer, using the supplied
   * PsmInterpStateBean
   */
  private PsmInterp makeInterp(PeerIdentity id, PsmInterpStateBean psmState) {
    PollerUserData ud = new PollerUserData(id, this, serializer);
    ud.setPollerNonce(makePollerNonce());
    PsmMachine machine =
      PollerStateMachineFactory.getMachine(getPollerActionsClass());
    PsmInterp interp = new PsmInterp(machine, ud);
//     interp.setCheckpointer(...);
    if (psmState != null) {
      interp.setResumeStateHack(psmState);
    }
    return interp;
  }

  /**
   * Called by a participant's state machine when it receives a set of nominees
   * from a voter.
   *
   * @param id
   * @param nominatedPeers
   */
  void nominatePeers(PeerIdentity id, List nominatedPeers) {
    // Only honor nominations if this is an inner circle peer.
    if (pollerState.isInOuterCircle(id)) {
      log.debug2("Ignoring nominations from outer circle voter " + id);
      return;
    }
    log.debug("Received nominations from inner circle voter: " + id +
              "; Nominations = " + nominatedPeers);
    // If the peer has sent us no nominations, drop him.
    if (nominatedPeers == null || nominatedPeers.size() == 0) {
      log.warning("Peer " + id + " did not nominate anyone.  Removing from " +
                  "poll.");
      removeVoter(id);
      return;
    }

    pollerState.signalVoterNominated(id);
    if (pollerState.allVotersNominated()) {
      pollOuterCircle();
    }
  }

  /**
   * Callback method called by each PollerStateMachine when entering the
   * TallyVoter state.  This method is only called once.
   */
  boolean tallyVoter(PeerIdentity id) {
    pollerState.signalVoterReadyToTally(id);
    if (pollerState.allVotersReadyToTally()) {
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
  private byte[][] initHasherByteArrays() {
    int len = theVoters.size();
    byte[][] initBytes = new byte[len][];
    int ix = 0;
    for (Iterator it = theVoters.values().iterator(); it.hasNext();) {
      PsmInterp interp = (PsmInterp) it.next();
      // null check required to skip over removed voters
      if (interp != null) {
        PollerUserData ud = (PollerUserData)interp.getUserData();
        log.debug2("Initting hasher byte arrays for voter " + ud.getVoterId());
        initBytes[ix] = ByteArray.concat(ud.getPollerNonce(),
                                         ud.getVoterNonce());
      }
      ix++;
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
  private MessageDigest[] initHasherDigests() throws NoSuchAlgorithmException {
    String hashAlg = pollerState.getHashAlgorithm();
    if (hashAlg == null) {
      hashAlg = LcapMessage.DEFAULT_HASH_ALGORITHM;
    }
    int len = theVoters.size();
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
  private void resumeHash(String target) {
    log.debug("Resuming hash for next block after: " + target);
  }

  /**
   * Called by the BlockHasher's event handler callback when hashing is complete
   * for one block. For each block in the hash, we compare our hash against all
   * participant hashes and build up a tally.
   */
  private void blockHashComplete(HashBlock block) {
    // XXX: This method needs to be refactored, it is only suitable for
    //      very priliminary testing in daemon 1.12!  To Be Done:
    //  - determine if we are missing a block.
    //  - determine if we have an extra block.
    log.debug("Hashing for block " + block.getUrl()
              + " complete, now tallying.");
    String targetUrl = block.getUrl();
    // Store the most recent block targetUrl in the state bean
    pollerState.setLastHashedBlock(targetUrl);

    // Compare with each participant's hash for this vote block,
    // then tally that participant's ID as either agree or disagree.
    tallyBlock(block, pollerState.getNextVoteBlockIndex());

    int result = tally.getTallyResult();

    switch(result) {
    case BlockTally.RESULT_WON:
      // Great, we won!  Do nothing.
      log.debug2("Won poll for block " + targetUrl);
      break;
    case BlockTally.RESULT_LOST:
      // XXX: Pick one or more random disagreeing voters from whom to request
      // a repair. Add those voters to the list of participants NOT in
      // Tallying state.
      //
      // For now, just pick one.
      PeerIdentity disagreeingVoter =
        (PeerIdentity)CollectionUtil.randomSelection(tally.getDisagreeVoters());
      pollerState.signalVoterNotReadyToTally(disagreeingVoter);
      log.debug2("Requesting a repair from voter: " + disagreeingVoter);
      // XXX: Suspend hash.  Repair mechanism TBD
      // Repair mechanism must signal Hash Service to resume hashing
      // the next block.
      break;
    case BlockTally.RESULT_TOO_CLOSE:
      log.warning("Tally was inconclusive.  Stopping poll "
          + pollerState.getPollKey());
      // XXX: Alerts?
      stopPoll();
      break;
    case BlockTally.RESULT_NOQUORUM:
      log.warning("No quorum.  Stopping poll " + pollerState.getPollKey());
      // XXX: Alerts?
      stopPoll();
      break;
    case BlockTally.RESULT_NEED_MORE_BLOCKS:
      log.info("Requesting more vote blocks from the following voters, and " +
                "re-running tally for block: " + tally.getNeedBlocksFrom());
      // XXX: TBD
      break;
    default:
      log.warning("Unexpected results from tallying block " + targetUrl + ": "
          + tally.getStatusString());
      // XXX: Alerts?
      stopPoll();
    }
  }

  /**
   * Given a HashBlock result from the hasher, compare its hash (generated by
   * us) with the hash in the VoteBlock from each participant (generated by the
   * voters).
   *
   * @param block The hashblock being tallied.
   * @param blockIndex The index of the voter's vote block to examine.
   * @returns A tally for the block.
   */
  protected void tallyBlock(HashBlock block, int blockIndex) {
    tally.reset();

    List agreeVoters = new ArrayList();
    List disagreeVoters = new ArrayList();

    String blockUrl = block.getUrl();
    log.debug2("Tallying block: " + blockUrl);
    MessageDigest[] blockDigests = block.getDigests();

    int digestIndex = 0;
    for (Iterator iter = theVoters.values().iterator(); iter.hasNext(); ) {
      PsmInterp interp = (PsmInterp)iter.next();
      // null check required to skip over removed voters.
      if (interp != null) {
        PollerUserData voterState = (PollerUserData)interp.getUserData();
        PeerIdentity voter = voterState.getVoterId();
        if (pollerState.isInOuterCircle(voter)) {
          log.debug2("Not including outer circle voter in tally." + voter);
          continue;
        }
        log.debug2("Getting VoteBlock " + blockIndex + " from voter " +
                   voterState.getVoterId());
        VoteBlock vb = null;
        try {
          vb = voterState.getVoteBlock(blockIndex);
        } catch (VoteBlocks.NoSuchBlockException ex) {
          log.debug2("Ran out of blocks for peer: " + voter);
          tally.addNeedBlocksFromPeer(voter);
        }

        if (vb != null) {
          byte[] voterResults = vb.getHash();
          byte[] hasherResults = blockDigests[digestIndex].digest();
          if (log.isDebug3()) {
            log.debug3("Comparing hashes for participant " + voter);
            log.debug3("Hasher results: "
                       + ByteArray.toHexString(hasherResults));
            log.debug3("Voter results: "
                       + (voterResults == null ? "null" :
                         ByteArray.toHexString(voterResults)));
          }
          if (Arrays.equals(voterResults, hasherResults)) {
            log.debug2("I agree with " + voter + " on block " + vb.getFileName());
            agreeVoters.add(voter);
          } else {
            log.debug2("I disagree with " + voter + " on block " + vb.getFileName());
            disagreeVoters.add(voter);
          }
        }
      }
      digestIndex++;
    }

    // XXX: Weights, etc.
    tally.setAgreeVoters(agreeVoters);
    tally.setDisagreeVoters(disagreeVoters);
    tally.tallyVotes();
  }

  /**
   * Called by the HashService callback when all hashing is complete for
   * the entire CUS.
   */
  private void hashComplete() {
    log.debug("Hashing is complete for this CUS.");
    for (Iterator iter = theVoters.values().iterator(); iter.hasNext();) {
      PsmInterp interp = (PsmInterp) iter.next();
      // null check required to skip over removed voters.
      if (interp != null) {
	try {
	  interp.handleEvent(V3Events.evtVoteComplete);
	} catch (PsmException e) {
	  log.warning("State machine error", e);
	}
        if (log.isDebug2()) {
          PollerUserData ud = (PollerUserData)interp.getUserData();
          log.debug2("Gave peer " + ud.getVoterId()
                     + " the Vote Complete event.");
        }
      }
    }
  }

  /**
   * Called by participant state machines if an error occurs.
   *
   * @param id
   * @param errorMsg
   */
  void handleError(PeerIdentity id, String errorMsg) {
    log.error("Peer " + id + " experienced an error. Error =" +
              (errorMsg == null ? "Unknown error" : errorMsg));

    // Drop the voter from the poll.
    removeVoter(id);
  }

  /**
   * Drop a voter from the poll.
   *
   * @param id
   */
  private void removeVoter(PeerIdentity id) {
    synchronized(theVoters) {
      log.debug("Removing voter " + id + " from poll " +
                pollerState.getPollKey());
      try {
        serializer.removePollerUserData(id);
        pollerState.signalVoterRemoved(id);
        theVoters.put(id, null);
        // XXX: To do, actually signal the poller's machine to stop!
      } catch (Exception ex) {
        // XXX: If this happens, the poll should probably be stopped.
        log.error("Unable to remove voter from poll!", ex);
      }
    }
  }

  /**
   * Used by the participant state machines to send messages to the appropriate
   * voter.
   *
   * @param msg
   * @param to
   * @throws IOException
   */
  void sendMessageTo(V3LcapMessage msg, PeerIdentity to)
      throws IOException {
    pollManager.sendMessageTo(msg, to);
  }

  /**
   * Handle an incoming V3LcapMessage.
   *
   */
  public void receiveMessage(LcapMessage message) {
    V3LcapMessage msg = (V3LcapMessage)message;

    PeerIdentity sender = msg.getOriginatorId();
    PsmInterp interp = (PsmInterp) theVoters.get(sender);
    if (interp != null) {
      PsmMsgEvent evt = V3Events.fromMessage(msg);
      try {
	interp.handleEvent(evt);
      } catch (PsmException e) {
	log.warning("State machine error", e);
      }
    } else {
      log.error("No voter user data for peer.  May have " +
                "been removed from poll: " + msg.getOriginatorId());
    }
  }

  public PollerStateBean getPollerStateBean() {
    return pollerState;
  }

  Collection getReferenceList() {
    Collection refList = idManager.getTcpPeerIdentities();
    log.debug2("Initial reference list for poll: " + refList);
    return refList;
  }

  Class getPollerActionsClass() {
    return PollerActions.class;
  }

  private class PollTimerCallback implements TimerQueue.Callback {
    /**
     * Called when the timer expires.
     *
     * @param cookie data supplied by caller to schedule()
     */
    public void timerExpired(Object cookie) {
      stopPoll();
    }
  }

  private class HashingCompleteCallback implements HashService.Callback {
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
        log.warning("Poll hash failed", e);
        stopPoll();
      }
    }
  }

  private class BlockCompleteHandler implements BlockHasher.EventHandler {
    public void blockDone(HashBlock block) {
      blockHashComplete(block);
    }
  }

  /*
   * BasePoll implementations.
   */

  /**
   * Set the message that created this poll.
   */
  public void setMessage(LcapMessage msg) {
    pollerState.setPollMessage(msg);
  }

  /**
   * Get the poll creation time.
   */
  public long getCreateTime() {
    return pollerState.getCreateTime();
  }

  /**
   * Get the ID of the caller of this poll (always us)
   */
  public PeerIdentity getCallerID() {
    return pollerState.getPollerId();
  }

  /**
   * Return true iff this poll is in an error state.
   */
  protected boolean isErrorState() {
    // XXX: TODO
    return false;
  }

  /**
   * Always true.
   */
  public boolean isMyPoll() {
    return true;
  }

  /**
   * Return the PollSpec for this poll.
   */
  public PollSpec getPollSpec() {
    return pollerState.getPollSpec();
  }

  /**
   * Return the CachedUrlSet for this poll.
   */
  public CachedUrlSet getCachedUrlSet() {
    return pollerState.getCachedUrlSet();
  }

  /**
   * Return the poll version.
   */
  public int getVersion() {
    return pollerState.getPollVersion();
  }

  /**
   * Return the message that started this poll.
   */
  public LcapMessage getMessage() {
    return pollerState.getPollMessage();
  }

  /**
   * Return the poll key.
   */
  public String getKey() {
    return pollerState.getPollKey();
  }

  public Deadline getDeadline() {
    return Deadline.at(pollerState.getDeadline());
  }

  public PollTally getVoteTally() {
    return tally;
  }


  /**
   * Generate a random nonce for the poller.
   *
   * @return A random array of 20 bytes.
   */
  private byte[] makePollerNonce() {
    return ByteArray.makeRandomBytes(20);
  }
}
