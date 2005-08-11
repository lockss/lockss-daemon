/*
 * $Id: V3Poller.java,v 1.2 2005-08-11 06:33:19 tlipkis Exp $
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

import java.io.*;
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

  private static String PREFIX =
    Configuration.PREFIX + "poll.v3.";

  /** Minimum number of participants for a V3 poll. */
  public static String PARAM_MIN_POLL_SIZE =
    PREFIX + "minPollSize";
  public static int DEFAULT_MIN_POLL_SIZE = 4;

  /** Maximum number of participants for a V3 poll. */
  public static String PARAM_MAX_POLL_SIZE =
    PREFIX + "maxPollSize";
  public static int DEFAULT_MAX_POLL_SIZE = 10;


  // Persistent state for this poll
  private V3PollerState m_pollerState;

  private LockssDaemon m_theDaemon;

  // True iff this is a continuation of a previously called poll.
  private boolean m_continuedPoll = false;

  // Deadline for this poll.
  private Deadline m_deadline;

  // A mapping of PeerIdentity to state machine interpreters.
  protected HashMap m_stateMachines;

  private int m_pollsize;

  private static Logger log = Logger.getLogger("V3Poller");

  // Callback used by PollerActions
  private ActionHandler m_voterHandler = new ActionHandler() {
      /**
       * Called when a nomination message has been received.
       */
      public void handleNominate(PeerIdentity id, List nominees) {
	nominatePeers(id, nominees);
      }

      /**
       * Called when a vote has been received.
       */
      public void handleTally(PeerIdentity id) {
	tallyVoter(id);
      }

      /**
       * Called when a repair message has been received.
       */
      public void handleRepair(PeerIdentity id) {
	// XXX:  To be done.
      }
    };

  protected V3Poller() {
    m_stateMachines = new HashMap();
    // Determine the number of participants for this poll.
    int minParticipants = Configuration.getIntParam(PARAM_MIN_POLL_SIZE,
						    DEFAULT_MIN_POLL_SIZE);
    int maxParticipants = Configuration.getIntParam(PARAM_MAX_POLL_SIZE,
						    DEFAULT_MAX_POLL_SIZE);
    if (maxParticipants < minParticipants) {
      throw new IllegalArgumentException("Impossible poll size range: " +
					 (maxParticipants - minParticipants));
    } else if (maxParticipants == minParticipants) {
      log.debug("Max poll size and min poll size are identical");
      m_pollsize = minParticipants;
    } else {
      // Pick a random number of participants for this poll between
      // minParticipants and maxParticipants
      LockssRandom rand = new LockssRandom();
      int randCount = rand.nextInt(maxParticipants - minParticipants);
      m_pollsize = minParticipants + randCount;
    }
    log.debug("Creating a new poll with a requested poll size of " + m_pollsize);
  }

  /**
   * Create a new V3 Poll.
   */
  public V3Poller(PollSpec spec, LockssDaemon daemon, PeerIdentity orig,
		  byte[] challenge, String key, long duration, String hashAlg) {
    this();
    this.m_pollerState = new V3PollerState(spec, orig, challenge, this,
					   key, duration, hashAlg);
    this.m_theDaemon = daemon;
  }

  /**
   * Restore a V3 Poll from a serialized state.
   */
  public V3Poller(LockssDaemon daemon, String serializedState) {
    this();
    this.m_theDaemon = daemon;
    this.m_continuedPoll = true;
    // XXX: TBD.
  }

  public void startPoll() {
    // Schedule the poll.
    log.debug("Scheduling V3 poll " +
	      m_pollerState.getPollKey() +
	      " to complete by " +
	      m_pollerState.getDeadline());

    TimerQueue.schedule(m_pollerState.getDeadline(),
			new PollTimerCallback(),
			this);

    if (m_continuedPoll) {
      restoreInnerCircle();
    } else {
      constructInnerCircle();
    }

    pollInnerCircle();
  }

  // XXX: Not clear what should be done here, yet.  TBD.
  public void stopPoll() {
    log.debug("Stopping poll " + m_pollerState.getPollKey());
  }

  /**
   * Construct the inner circle of voters.
   */
  private void constructInnerCircle() {
      Collection refList = getReferenceList();

      Collection selectedVoters =
	CollectionUtil.randomSelection(refList,
				       (m_pollsize > refList.size() ?
					refList.size() : m_pollsize));
      log.debug("Selected " + selectedVoters.size() + " participants for poll ID " +
		m_pollerState.getPollKey());

      for (Iterator it = selectedVoters.iterator(); it.hasNext(); ) {
	PeerIdentity voterId = (PeerIdentity)it.next();
	V3VoterState voterState = new V3VoterState(voterId, m_pollerState, m_voterHandler);
	m_pollerState.addVoterState(voterId, voterState);

	PsmMachine machine = PollerStateMachineFactory.
	  getMachine(getPollerActionsClass());
	PsmInterp interp = new PsmInterp(machine, voterState);
	m_stateMachines.put(voterId, interp);
      }
  }

  /**
   * Reconstruct the inner circle list for a restored poll.
   */
  private void restoreInnerCircle() {
    // XXX: TBD
  }

  private void pollInnerCircle() {
    // XXX:
    // Poll the inner circle.
    //    - For every V3VoterState listed in the V3PollerState,
    //      init its state machine.
    for (Iterator it = m_stateMachines.values().iterator(); it.hasNext(); ) {
      PsmInterp interp = (PsmInterp)it.next();
      interp.init();
    }
  }

  private void restoreOuterCircle() {
    // XXX: TBD
  }

  private void constructOuterCircle() {
    // XXX: TBD
  }

  private void pollOuterCircle() {
    // XXX: TBD
  }

  void nominatePeers(PeerIdentity id, List nominatedPeers) {
    // XXX: When enough nominations have been received, create
    // and poll the outer circle.

    log.debug("Received nominations from peer: " + id +
	      "; Nominations = " + nominatedPeers);
  }

  /**
   * Called by the ActionHandler callback when a vote message
   * has been received from the specified participant ID.
   */
  private void tallyVoter(PeerIdentity id) {
    m_pollerState.voterTallied(id);
    if (m_pollerState.allVotersTallied()) {
      log.debug("All voters are tallied!");
      scheduleHash();
    } else {
      log.debug("Not all voters are tallied yet. Waiting.");
    }
  }

  /**
   * Schedule hashing of the AU.  When hashing is complete for each
   * block, call blockHashComplete().  When ALL hashing is done, call
   * hashComplete().
   */
  protected void scheduleHash() {
    // XXX: Implement hashing.  For now, override for testing
    //
    // Must be able to (1) Start hashing, and (2) Resume hashing
    // after a repair.
    log.debug("Scheduling hashing.");
  }

  /**
   * Signal the hash service to resume its hash, starting with the vote
   * block specifed.
   *
   * XXX: TBD.
   */
  protected void resumeHash(String target) {
    log.debug("Resuming hash for next block after: " + target);
  }

  /**
   * Called by the HashService callback when ganged hashing is complete
   * for one block.
   *
   * For each block in the hash, we compare our hash against all
   * participant hashes and build up a tally.  The tally is then
   * examined to determine what to do next.
   *
   * 1. If there's a landslide win for this block, call resumeHash() and
   *    return.
   *
   * 3. If there is a landslide loss for this block, pick one or more
   *    peers to request repairs from, take them out of the "Tallied"
   *    state, and send their state machines V3Events.evtRepairNeeded.
   * 
   * 4. If there is no quorum or if the vote is too close, warn and/or
   *    send an alert, then stop the poll.
   *
   * XXX: Multiple vote message mechanism TBD.
   */
  private void blockHashComplete(String targetUrl, byte[] hashResult) {
    log.debug("Ganged hashing for block " + targetUrl + 
	      " complete.  Hash results:" +
	      String.valueOf(B64Code.encode(hashResult)));

    // Store the most recent block targetUrl in the state bean
    m_pollerState.setLastHashedBlock(targetUrl);

    // Compare with each participant's hash for this vote block,
    // then tally that participant's ID as either agree or disagree.
    BlockTally tally = compareHash(targetUrl, hashResult);
    int result = tally.getResult();

    if (result == BlockTally.RESULT_WON) {
      // Great, we won!  Signal the HashService to resume hashing
      // the next block. (TBD)
      log.debug("Won poll for block " + targetUrl);
      resumeHash(targetUrl);
    } else if (result == BlockTally.RESULT_LOST) {
      // Pick one or more random disagreeing voters from whom to request
      // a repair.  Add those voters to the list of participants NOT in
      // Tallying state.
      PeerIdentity disagreeingVoter =
	(PeerIdentity)CollectionUtil.randomSelection(tally.getDisagreeVotes());
      m_pollerState.voterNotTallied(disagreeingVoter);
      log.debug("Requesting a repair from voter: " + disagreeingVoter);
      // XXX: Repair mechanism TBD

      // Repair mechanism must signal Hash Service to resume hashing
      // the next block.
    } else if (result == BlockTally.RESULT_TOO_CLOSE) {
      log.warning("Tally was inconclusive.  Stopping poll " + m_pollerState.getPollKey());
      // XXX: Alerts
      stopPoll();
    } else if (result == BlockTally.RESULT_NOQUORUM) {
      log.warning("No quorum.  Stopping poll " + m_pollerState.getPollKey());
      // XXX: Alerts
      stopPoll();
    } else {
      log.warning("Unexpected results from tallying block " + targetUrl + ": " +
		  tally.getResultString());
      stopPoll();
    }
  }

  protected BlockTally compareHash(String targetUrl, byte[] hashResult) {
    // XXX: Implement comparison of our hash (hashResult) with the
    // hash in the appropriate block of the participant's vote message.
    BlockTally tally = new BlockTally();
    Map innerCircle = m_pollerState.getInnerCircle();
    for (Iterator iter = innerCircle.keySet().iterator(); iter.hasNext(); ) {
      PeerIdentity voter = (PeerIdentity)iter.next();
      log.debug("Comparing hashes for participant " + voter);
      V3VoterState voterState = (V3VoterState)innerCircle.get(voter);
      // XXX... For development testing, just agree.
      tally.addAgreeVote(voter);
    }
    tally.tallyVotes();
    return tally;
  }
      
  /**
   * Called by the HashService callback when all hashing is complete.
   */
  private void hashComplete() {
    Collection innerCircle = m_pollerState.getInnerCircle().keySet();
    
    for (Iterator iter = innerCircle.iterator(); iter.hasNext(); ) {
      PeerIdentity voter = (PeerIdentity)iter.next();
      PsmInterp interp = (PsmInterp)m_stateMachines.get(voter);
      interp.handleEvent(V3Events.evtVoteComplete);
      log.debug2("Gave peer " + voter + " the Vote Complete event.");
    }
  }

  private MessageDigest getInitedDigest() {
    MessageDigest digest = null;
    try {
      digest = MessageDigest.getInstance(m_pollerState.getHashAlgorithm());
    } catch (NoSuchAlgorithmException ex) {
      log.error("Unable to run - no hasher");
      return null;
    }
    byte[] challenge = m_pollerState.getChallenge();
    digest.update(challenge, 0, challenge.length);
    log.debug("hashing: C[" + String.valueOf(B64Code.encode(challenge)) + "]");
    return digest;
  }

  // XXX: Implement!  Override for testing.
  public void sendMessageTo(V3LcapMessage msg, PeerIdentity to) {
    //
  }

  class HashingCompleteCallback implements HashService.Callback {
    /**
     * Called when the timer expires.
     * @param cookie  data supplied by caller to schedule()
     */
    public void hashingFinished(CachedUrlSet cus,
				Object cookie,
				CachedUrlSetHasher hasher,
				Exception e) {
      if (e == null) {
	hashComplete();
      } else {
	log.warning("Poll hash failed : " + e.getMessage());
	stopPoll();
      }
    }

    /**
     * Called when an individual block is finished
     */
    public void blockFinished(CachedUrlSet cus,
			      Object cookie,
			      MessageDigest digest,
			      Exception e) {
      if (e == null) {
	blockHashComplete((String)cookie, digest.digest());
      } else {
	log.warning("Poll hash failed : " + e.getMessage());
	stopPoll();
      }
    }
  }

  /**
   * Handle an incoming V3LcapMessage.  Needs to:
   *
   * - Determine what peer this came from, so it can
   *   look up the correct state machine interpreter.
   * - Create the right type of PsmMsgEvent.
   * - Call handleEvent().
   *
   */
  public void handleMessage(V3LcapMessage msg) {
    PeerIdentity sender = msg.getOriginatorId();
    PsmInterp interp = (PsmInterp)m_stateMachines.get(sender);
    if (interp != null) {
      PsmMsgEvent evt = V3Events.fromMessage(msg);
      interp.handleEvent(evt);
    } else {
      log.debug("No voter state for peer: " + msg.getOriginatorId());
    }
  }

  /* ------------------ Utilities ----------------------------------- */

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
     * @param cookie  data supplied by caller to schedule()
     */
    public void timerExpired(Object cookie) {
      stopPoll();
    }
  }

  /** Callback interface */
  public interface ActionHandler {
    public void handleNominate(PeerIdentity id, List nominees);
    public void handleTally(PeerIdentity id);
    public void handleRepair(PeerIdentity id);
  }
}
