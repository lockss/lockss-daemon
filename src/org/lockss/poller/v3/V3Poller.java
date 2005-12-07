/*
 * $Id: V3Poller.java,v 1.16 2005-12-07 21:12:01 smorabito Exp $
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
import org.lockss.poller.v3.V3Serializer.*;
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

  /** Target size of the outer circle poll. */
  public static String PARAM_TARGET_OUTER_CIRCLE_SIZE =
    PREFIX + "targetOuterCircle";
  public static int DEFAULT_TARGET_OUTER_CIRCLE_SIZE = 10;

  /** If true, drop participants from this poll that do not send
   * outer circle nominees. */
  public static String PARAM_DROP_EMPTY_NOMINATIONS =
    PREFIX + "dropEmptyNominations";
  public static boolean DEFAULT_DROP_EMPTY_NOMINATIONS = false;

  // Global state for the poll.
  private PollerStateBean pollerState;
  // Map of PeerIdentity => ParticipantState for all participants in
  // this poll, both inner and outer circle
  protected Map theParticipants =
    Collections.synchronizedMap(new LinkedHashMap());
  private LockssDaemon theDaemon;
  private PollManager pollManager;
  private IdentityManager idManager;
  private V3PollerSerializer serializer;
  private BlockTally tally;
  private boolean resumedPoll;
  private boolean activePoll = true;
  private boolean dropEmptyNominators = DEFAULT_DROP_EMPTY_NOMINATIONS;

  private static Logger log = Logger.getLogger("V3Poller");
  private static LockssRandom theRandom = new LockssRandom();

  /**
   * Create a new V3 Poll.
   */
  public V3Poller(PollSpec spec, LockssDaemon daemon, PeerIdentity orig,
                  String key, long duration, String hashAlg)
      throws V3Serializer.PollSerializerException {
    this.theDaemon = daemon;
    this.pollManager = daemon.getPollManager();
    this.idManager = daemon.getIdentityManager();
    if (hashAlg == null) hashAlg = LcapMessage.DEFAULT_HASH_ALGORITHM;
    // If the hash algorithm is not available, fail the poll immediately.
    try {
      MessageDigest.getInstance(hashAlg);
    } catch (NoSuchAlgorithmException ex) {
      throw new IllegalArgumentException("Algorithm " + hashAlg +
                                         " is not supported");
    }
    this.serializer = new V3PollerSerializer(theDaemon);
    Configuration c = ConfigManager.getCurrentConfig();
    // Determine the number of peers to invite into this poll.
    int minParticipants = c.getInt(PARAM_MIN_POLL_SIZE,
                                        DEFAULT_MIN_POLL_SIZE);
    int maxParticipants = c.getInt(PARAM_MAX_POLL_SIZE,
                                        DEFAULT_MAX_POLL_SIZE);
    int outerCircleTarget = c.getInt(PARAM_TARGET_OUTER_CIRCLE_SIZE,
                                          DEFAULT_TARGET_OUTER_CIRCLE_SIZE);
    int quorum = c.getInt(PARAM_QUORUM, DEFAULT_QUORUM);
    this.dropEmptyNominators = c.getBoolean(PARAM_DROP_EMPTY_NOMINATIONS,
                                            DEFAULT_DROP_EMPTY_NOMINATIONS);

    int pollSize = getInnerCircleSize(minParticipants, maxParticipants, quorum);

    this.pollerState = new PollerStateBean(spec, orig, key, duration,
                                           pollSize, outerCircleTarget,
                                           quorum, hashAlg);
    this.tally = new BlockTally(pollerState.getQuorum());
    log.debug("Creating a new poll with a requested poll size of "
              + pollSize);
    // Checkpoint the poll.
    checkpointPoll();
  }

  /**
   * Restore a V3 Poll from a serialized state.
   */
  public V3Poller(LockssDaemon daemon, File pollDir)
      throws V3Serializer.PollSerializerException {
    this.theDaemon = daemon;
    this.serializer = new V3PollerSerializer(theDaemon, pollDir);
    this.pollerState = serializer.loadPollerState();
    setStatus("Resuming");
    // If the hash algorithm used when the poll was first created is
    // no longer available, fail the poll immediately.
    try {
      MessageDigest.getInstance(pollerState.getHashAlgorithm());
    } catch (NoSuchAlgorithmException ex) {
      throw new IllegalArgumentException("Algorithm " +
                                         pollerState.getHashAlgorithm() +
                                         " is no longer supported");
    }
    this.pollManager = daemon.getPollManager();
    this.idManager = daemon.getIdentityManager();
    Configuration c = ConfigManager.getCurrentConfig();
    this.dropEmptyNominators = c.getBoolean(PARAM_DROP_EMPTY_NOMINATIONS,
                                            DEFAULT_DROP_EMPTY_NOMINATIONS);
    this.tally = new BlockTally(pollerState.getQuorum());
    // Restore transient cus and pollspec in the poller state.
    PluginManager plugMgr = theDaemon.getPluginManager();
    CachedUrlSet cus = plugMgr.findCachedUrlSet(pollerState.getAuId());
    if (cus == null) {
      throw new NullPointerException("CUS for AU " + pollerState.getAuId() +
                                     " is null!");
    }
    pollerState.setCachedUrlSet(cus);
    pollerState.setPollSpec(new PollSpec(cus, Poll.V3_POLL));
    // Restore the peers for this poll.
    try {
      restoreParticipants();
    } catch (V3Serializer.PollSerializerException ex) {
      log.error("Unable to restore poll state!");
    }
    this.resumedPoll = true;

    log.debug2("Restored serialized poll " + pollerState.getPollKey());
  }


  /**
   * Build the initial set of inner circle peers.
   *
   * @param pollSize The number of peers to invite.
   */
  protected void constructInnerCircle(int pollSize) {
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
   * Restore the set of serialized pariticipants when restoring a poll.
   *
   * @throws V3Serializer.PollSerializerException
   */
  protected void restoreParticipants()
      throws V3Serializer.PollSerializerException {
    Collection peers = serializer.loadVoterStates();
    for (Iterator iter = peers.iterator(); iter.hasNext();) {
      final ParticipantUserData voterState = (ParticipantUserData) iter.next();
      PeerIdentity id = voterState.getVoterId();
      PsmInterpStateBean pisb = voterState.getPsmInterpState();
      PsmMachine machine =
        PollerStateMachineFactory.getMachine(getPollerActionsClass());
      PsmInterp interp = new PsmInterp(machine, voterState);
      voterState.setPsmInterpState(pisb);
      interp.setCheckpointer(new PsmInterp.Checkpointer() {
        public void checkpoint(PsmInterpStateBean resumeStateBean) {
          // Replace the statebean in this machine's state object.
          voterState.setPsmInterpState(resumeStateBean);
          try {
            checkpointParticipant(voterState);
          } catch (PollSerializerException ex) {
            log.error("Unable to save participant state for peer " +
                      voterState.getVoterId());
          }
        }
      });
      voterState.setPsmInterp(interp);
      voterState.setPollState(pollerState);
      voterState.setPoller(this);
      theParticipants.put(id, voterState);
    }
  }


  /**
   * Choose the size of the inner circle for this poll.
   *
   * @param min Minimum number of peers to invite.
   * @param max Maximum number of peers to invite.
   * @param quorum Quorum size.
   * @return The number of peers to invite into the inner circle.
   */
  protected int getInnerCircleSize(int min, int max, int quorum) {
    int pollSize = 0;
    if (min < quorum) {
      throw new IllegalArgumentException("Cannot start a poll with minimum " +
                "size " + min + " because at least " + quorum +
                " participants are required for quorum");
    }
    if (max < min) {
      log.warning("Impossible poll size range [min=" + min + ", max=" + max
                  + "].  Defaulting to min size.");
      return min;
    } else if (max == min) {
      log.debug("Max poll size and min poll size are identical");
      pollSize = min;
    } else {
      // Pick a random number of participants for this poll between
      // minParticipants and maxParticipants
      int randCount = theRandom.nextInt(max - min);
      pollSize = min + randCount;
    }

    return pollSize;
  }

  /**
   * Start a poll.  Overrides BasePoll.startPoll().
   */
  public void startPoll() {
    if (!resumedPoll) {
      // Construct the initial inner circle only once
      constructInnerCircle(pollerState.getPollSize());
    }
    // Schedule the poll deadline
    setStatus("Polling");
    log.debug("Scheduling V3 poll " + pollerState.getPollKey() +
              " to complete by " + pollerState.getDeadline());
    TimerQueue.schedule(Deadline.at(pollerState.getDeadline()),
                        new PollTimerCallback(), this);
    for (Iterator it = theParticipants.values().iterator(); it.hasNext();) {
      ParticipantUserData ud = (ParticipantUserData)it.next();
      if (!ud.isOuterCircle()) { // Start polling only inner circle members.
        PsmInterp interp = ud.getPsmInterp();
        try {
          if (resumedPoll) {
            interp.resume(ud.getPsmInterpState());
          } else {
            interp.start();
          }
        } catch (PsmException e) {
          log.warning("State machine error", e);
          abortPoll();
        }
      }
    }
  }

  /**
   * <p>Stop the poll.  Overrides BasePoll.stopPoll().</p>
   *
   * <p>May be called by the PollManager, or internally to abort the poll.</p>
   */
  public void stopPoll() {
    // XXX: Set a final status string -- OK / Repaired / Unrepairable
    setStatus("Complete");
    activePoll = false;
    serializer.closePoll();
    pollManager.closeThePoll(pollerState.getPollKey());
    log.debug("Closed poll " + pollerState.getPollKey());
  }

  private void abortPoll() {
    stopPoll();
    setStatus("Error");
  }

  /**
   * <p>Examine the nominees from each peer. Choose a fixed number
   * to allow from each peer.  If any peer has sent no nominees,
   * remove it from the poll.</p>
   */
  private void pollOuterCircle() {
    // Calculate the correct number to request from each host.
    int targetSize =
      (int)(pollerState.getOuterCircleTarget() / theParticipants.size());
    log.debug2("Target for nominees from each inner circle participant: " +
               targetSize);

    Collection outerCircle = constructOuterCircle(targetSize);

    // Now start polling the outer circle.
    for (Iterator it = outerCircle.iterator(); it.hasNext(); ) {
      String idStr = (String) it.next();
      PeerIdentity id = idManager.findPeerIdentity(idStr);
      if (!theParticipants.containsKey(id)) {
        log.debug2("Adding new peer " + id + " to the outer circle");
        ParticipantUserData participant = makeParticipant(id);
        participant.isOuterCircle(true);
        theParticipants.put(id, participant);
        try {
          participant.getPsmInterp().start();
        } catch (PsmException e) {
          log.warning("State machine error, removing peer from poll.", e);
          // Drop this peer from the poll.
          this.removeParticipant(id);
        }
      } else {
        log.debug2("We already have peer " + id + " in our list of voters");
      }
    }
  }

  protected Collection constructOuterCircle(int target) {
    Collection outerCircle = new ArrayList();
    // Attempt to randomly select 'target' peers from each voter's
    // nominee list.  If there are not enough peers, just nominate whoever
    // we can.
    for (Iterator it = theParticipants.values().iterator(); it.hasNext(); ) {
      ParticipantUserData participant = (ParticipantUserData)it.next();
      List nominees = participant.getNominees();
      if (nominees == null || nominees.size() == 0) {
        // If we want to drop peers that don't send any nominees,
        // they will have already been removed at this point,
        // so just log a debug statement and go on.
        log.debug2("Peer " + participant.getVoterId()
                   + " did not nominate anyone");
        continue;
      } else if (nominees.size() < target) {
        log.warning("Peer " + participant.getVoterId() +
                    " only sent " + nominees.size() + " nominations.");
        outerCircle.addAll(nominees);
        continue;
      } else {
        log.debug3("Randomly selecting " + target + " nominees from " +
                   "the set " + nominees);
        outerCircle.addAll(CollectionUtil.randomSelection(nominees, target));
      }
    }
    return outerCircle;
  }

  /**
   * Add a voter to the inner circle of the poll.
   *
   * @param id
   * @return PsmInterp
   */
  private ParticipantUserData addInnerCircleVoter(PeerIdentity id) {
    ParticipantUserData participant = makeParticipant(id);
    theParticipants.put(id, participant);
    return participant;
  }



  /**
   * Create a new ParticipantUserData state object for the specified peer.
   *
   * @return ParticipantUserData for the specified peer
   */
  private ParticipantUserData makeParticipant(final PeerIdentity id) {
    final ParticipantUserData participant = new ParticipantUserData(id, this);
    participant.setPollerNonce(makePollerNonce());
    PsmMachine machine =
      PollerStateMachineFactory.getMachine(getPollerActionsClass());
    PsmInterp interp = new PsmInterp(machine, participant);
    interp.setCheckpointer(new PsmInterp.Checkpointer() {
      public void checkpoint(PsmInterpStateBean resumeStateBean) {
        // Replace the statebean in this machine's state object.
        participant.setPsmInterpState(resumeStateBean);
        try {
          checkpointParticipant(participant);
        } catch (PollSerializerException ex) {
          log.error("Unable to save participant state for peer " +
                    participant.getVoterId());
        }
      }
    });
    participant.setPsmInterp(interp);
    return participant;
  }

  /**
   * Called by a participant's state machine when it receives a set of nominees
   * from a voter.
   *
   * @param id The PeerIdentity of the participant sending the nominees.
   * @param nominatedPeers A list of string representations of PeerIdendities
   *                       the peer is nominating.
   */
  void nominatePeers(PeerIdentity id, List nominatedPeers) {
    // Only honor nominations if this is an inner circle peer.
    ParticipantUserData peer = (ParticipantUserData)theParticipants.get(id);
    if (peer.isOuterCircle()) {
      log.debug2("Ignoring nominations from outer circle voter " + id);
      return;
    }
    // Peers should never be allowed to nominate themsevles.
    nominatedPeers.remove(id);
    log.debug2("Received nominations from inner circle voter: " + id +
               "; Nominations = " + nominatedPeers);
    // If the peer has sent us no nominations, decide whether to drop him.
    if (dropEmptyNominators &&
        (nominatedPeers == null || nominatedPeers.size() == 0)) {
      log.warning("Peer " + id + " did not nominate anyone.  Removing from " +
                  "poll.");
      removeParticipant(id);
      return;
    }
    // Store the nominees in the participant's userdata
    peer.setNominees(nominatedPeers);
    pollerState.signalVoterNominated(id);
    checkpointPoll();
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
    checkpointPoll();
    if (pollerState.allVotersReadyToTally()) {
      log.debug2("Ready to schedule hash.");
      if (!scheduleHash()) {
        // XXX:  Reserve time to hash poll at START of poll, and abort
        //       the poll if time is unavailable.
        log.error("No time available to schedule our hash for poll "
                  + pollerState.getPollKey());
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
   * @return true iff the hash is scheduled successfully.
   */
  protected boolean scheduleHash() {
    log.debug("Scheduling our hash for poll " + pollerState.getPollKey());
    Deadline deadline = Deadline.at(pollerState.getDeadline());
    BlockHasher hasher = new BlockHasher(pollerState.getCachedUrlSet(),
                                         initHasherDigests(),
                                         initHasherByteArrays(),
                                         new BlockCompleteHandler());
    HashService hashService = theDaemon.getHashService();
    if (hashService.scheduleHash(hasher, deadline,
                                    new HashingCompleteCallback(), null)) {
      setStatus("Hashing");
      return true;
    } else {
      return false;
    }
  }

  /**
   * Create an array of byte arrays containing hasher initializer bytes, one for
   * each participant in the poll. The initializer bytes are constructed by
   * concatenating the participant's poller nonce and the voter nonce.
   *
   * @return Block hasher initialization bytes.
   */
  private byte[][] initHasherByteArrays() {
    int len = theParticipants.size();
    byte[][] initBytes = new byte[len][];
    int ix = 0;
    for (Iterator it = theParticipants.values().iterator(); it.hasNext();) {
      ParticipantUserData ud = (ParticipantUserData)it.next();
      log.debug2("Initting hasher byte arrays for voter " + ud.getVoterId());
      initBytes[ix] = ByteArray.concat(ud.getPollerNonce(),
                                       ud.getVoterNonce());
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
  private MessageDigest[] initHasherDigests() {
    int len = theParticipants.size();
    MessageDigest[] digests = new MessageDigest[len];
    for (int ix = 0; ix < len; ix++) {
      try {
        digests[ix] = MessageDigest.getInstance(pollerState.getHashAlgorithm());
      } catch (NoSuchAlgorithmException ex) {
        // This will have been caught at construction time, and so should
        // never happen here.
        log.critical("Unexpected NoSuchAlgorithmException in " +
                     "initHasherDigests");
        abortPoll();
      }
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
    log.debug2("Resuming hash for next block after: " + target);
  }

  /**
   * Called by the BlockHasher's event handler callback when hashing is complete
   * for one block. For each block in the hash, we compare our hash against all
   * participant hashes and build up a tally.
   */
  private void blockHashComplete(HashBlock block) {
    log.debug("Hashing for block " + block.getUrl()
              + " complete, now tallying.");
    String targetUrl = block.getUrl();
    // Store the most recent block targetUrl in the state bean
    pollerState.setLastHashedBlock(targetUrl);
    checkpointPoll();
    // Compare with each participant's hash for this vote block,
    // then tally that participant's ID as either agree or disagree.
    tallyBlock(block);

    int result = tally.getTallyResult();
    switch(result) {
    case BlockTally.RESULT_WON:
      // Great, we won!  Do nothing.
      log.debug2("Won poll for block " + targetUrl);
      break;
    case BlockTally.RESULT_LOST:
      // XXX: Implement repairs.  Just log and abort the poll until then.
      log.info("*** We believe we need to repair block " + block.getUrl());
      abortPoll();
      break;
    case BlockTally.RESULT_LOST_EXTRA_BLOCK:
      // XXX: Implement repairs.  Just log and abort the poll until then.
      log.info("*** We believe that the majority of voters do not have block " +
               block.getUrl() + ". we should delete it");
      abortPoll();
      break;
    case BlockTally.RESULT_LOST_MISSING_BLOCK:
      // XXX: Implement repairs.  Just log and abort the poll until then.
      log.info("*** We believe that the majority of voters have a block " +
               "that we do not.   We should retrieve it.");
      abortPoll();
      break;
    case BlockTally.RESULT_NOQUORUM:
    case BlockTally.RESULT_TOO_CLOSE:
      log.warning("Tally was inconclusive.  Stopping poll "
                  + pollerState.getPollKey());
      // XXX: Alerts?
      abortPoll();
      setStatus("Too Close");
      break;
    default:
      log.warning("Unexpected results from tallying block " + targetUrl + ": "
                  + tally.getStatusString());
      // XXX: Alerts?
      abortPoll();
    }
  }

  /**
   * Given a HashBlock result from the hasher, compare its hash (generated by
   * us) with the hash in the VoteBlock from each participant (generated by the
   * voters).
   *
   * @param hb The hashblock being tallied.
   */
  protected void tallyBlock(HashBlock hb) {
    tally.reset();
    setStatus("Tallying");

    String blockUrl = hb.getUrl();
    log.debug2("Tallying block: " + blockUrl);

    int myIndex = pollerState.getNextVoteBlockIndex();
    checkpointPoll();

    int digestIndex = -1;
    for (Iterator iter = theParticipants.values().iterator(); iter.hasNext(); ) {
      digestIndex++;
      ParticipantUserData voterState = (ParticipantUserData)iter.next();
      PeerIdentity voter = voterState.getVoterId();
      if (voterState.isOuterCircle()) {
        log.debug2("Not including outer circle voter in tally: " + voter);
        continue;
      }
      PsmInterp interp = voterState.getPsmInterp();
      VoteBlocks blocks = voterState.getVoteBlocks();
      int idx = voterState.getVoteBlockIndex();
      log.debug("Now tallying voter " + voter + ", block index: " + idx);
      VoteBlock vb = blocks.getVoteBlock(idx);
      if (vb == null) {
        // If the vote block is null, we've hit the end of this participant's
        // blocks.  If the last vote message had the voteComplete flag
        // set, the voter is just missing this block, and we disagree with
        // it.  Otherwise, suspend the hash and send the next vote request.
        if (voterState.isVoteComplete()) {
          tally.addDisagreeVoter(voter);
        } else {
          // XXX: Suspend / resume to be done.
          log.info("*** Suspending hash and sending next vote request"
                   + " to voter " + voter);
        }
      } else {
        // Is the voter's block the same file as ours?
        if (vb.getUrl().equals(hb.getUrl())) {
          // Yes, it's our block.
          compareBlocks(vb, hb, digestIndex, voter, tally);
        } else {
          // No.  Either this participant has a block we don't have,
          // or we  have a block this participant doesn't have.
          VoteBlock lookAheadBlock = blocks.getVoteBlock(idx + 1);
          // Again, check to see if it's null.  If it is, we need to request
          // new blocks from this peer.
          if (vb == null) {
            if (voterState.isVoteComplete()) {
              tally.addDisagreeVoter(voter);
            } else {
              // XXX:  Suspend/resume to be done.
              log.info("*** Suspending hash and sending next vote request"
                       + " to voter " + voter);
            }
          } else {
            // The voter had another block for us to examine.
            if (lookAheadBlock.getUrl().equals(hb.getUrl())) {
              // This is the correct block.  The voter had an
              // extra block that we don't have.  Compare the
              // look-ahead block, then increment the voter's index
              // to skip over the extra block in the next iteration.
              // Also flag that there is a block disagreement.
              compareBlocks(lookAheadBlock, hb, digestIndex, voter, tally);
              tally.addExtraBlockVoter(voter);
              idx++;
            } else {
              // Nope, still not the block we're looking for.  It may
              // be that this voter is missing a block that we
              // have.  We'll just decline to increment this voter's
              // block index, and disagree with him for now.  Also flag
              // that there is a block disagreement.
              tally.addDisagreeVoter(voter);
              tally.addMissingBlockVoter(voter);
              continue;
            }
          }
        }
      }
      voterState.setVoteBlockIndex(idx + 1);
      log.debug("Just set voteBlockIndex to: " + voterState.getVoteBlockIndex());
    }
    tally.tallyVotes();
  }

  private void compareBlocks(VoteBlock vb, HashBlock hb,
                             int digestIndex, PeerIdentity voter,
                             BlockTally tally) {
    byte[] voterResults = vb.getChallengeHash();
    byte[] hasherResults = hb.getDigests()[digestIndex].digest();
    if (log.isDebug3()) {
      log.debug3("Comparing hashes for participant " + voter
                 + ", digestIndex=" + digestIndex);
      log.debug3("Hasher results: "
                 + ByteArray.toHexString(hasherResults));
      log.debug3("Voter results: "
                 + (voterResults == null ? "null" :
                   ByteArray.toHexString(voterResults)));
    }
    if (Arrays.equals(voterResults, hasherResults)) {
      log.debug2("I agree with " + voter + " on block " + vb.getUrl());
      tally.addAgreeVoter(voter);
    } else {
      log.debug2("I disagree with " + voter + " on block " + vb.getUrl());
      tally.addDisagreeVoter(voter);
    }
  }

  /**
   * Called by the HashService callback when all hashing is complete for
   * the entire CUS.
   */
  private void hashComplete() {
    log.debug("Hashing is complete for this CUS.");
    for (Iterator iter = theParticipants.values().iterator(); iter.hasNext();) {
      ParticipantUserData ud = (ParticipantUserData)iter.next();
      PsmInterp interp = ud.getPsmInterp();
      try {
        interp.handleEvent(V3Events.evtVoteComplete);
      } catch (PsmException e) {
        log.warning("State machine error", e);
      }
      if (log.isDebug2()) {
        log.debug2("Gave peer " + ud.getVoterId()
                   + " the Vote Complete event.");
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
    removeParticipant(id);
  }

  /**
   * Drop a voter from the poll.
   *
   * @param id
   */
  void removeParticipant(PeerIdentity id) {
    log.debug("Removing voter " + id + " from poll " +
              pollerState.getPollKey());
    try {
      ParticipantUserData ud = (ParticipantUserData)theParticipants.get(id);
      serializer.removePollerUserData(id);
      pollerState.signalVoterRemoved(id, ud.isOuterCircle());
      theParticipants.remove(id);
      checkpointPoll();
      // XXX: To do, actually signal the poller's machine to stop!
    } catch (Exception ex) {
      // XXX: If this happens, the poll should probably be stopped.
      log.error("Unable to remove voter from poll!", ex);
    }
  }

  /**
   * Checkpoint the per-poll state.
   */
  private void checkpointPoll() {
    try {
      serializer.savePollerState(pollerState);
    } catch (PollSerializerException ex) {
      log.warning("Unable to save poller state!", ex);
    }
  }

  /**
   * Checkpoint the per-participant state.
   * @throws PollSerializerException
   */
  private void checkpointParticipant(ParticipantUserData ud)
      throws PollSerializerException {
    serializer.savePollerUserData(ud);
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
    ParticipantUserData ud = (ParticipantUserData)theParticipants.get(sender);
    PsmInterp interp = ud.getPsmInterp();
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
   * Not used by V3
   */
  public PeerIdentity getCallerID() {
    return pollerState.getPollerId();
  }

  /**
   * Not used by V3
   */
  protected boolean isErrorState() {
    return false;
  }

  public boolean isPollActive() {
    return activePoll;
  }

  public boolean isPollCompleted() {
    return !activePoll;
  }

  /**
   * Not used by V3
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
   * Return the size of the poll.
   */
  public int getPollSize() {
    return theParticipants.size();
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

  public long getDuration() {
    return pollerState.getDuration();
  }

  public int getQuorum() {
    return pollerState.getQuorum();
  }

  public Iterator getParticipants() {
    return theParticipants.values().iterator();
  }

  public PollTally getVoteTally() {
    throw new UnsupportedOperationException("Not implemented for V3");
  }

  public int getType() {
    return Poll.V3_POLL;
  }

  public ArchivalUnit getAu() {
    return pollerState.getCachedUrlSet().getArchivalUnit();
  }

  public String getStatusString() {
    return pollerState.getStatusString();
  }

  private void setStatus(String status) {
    pollerState.setStatusString(status);
    checkpointPoll();
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
