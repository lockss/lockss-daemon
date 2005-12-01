/*
 * $Id: V1NamePoll.java,v 1.21 2005-12-01 01:54:44 smorabito Exp $
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

package org.lockss.poller;

import java.io.*;
import java.security.*;
import java.util.*;

import org.lockss.daemon.*;
import org.lockss.hasher.*;
import org.lockss.protocol.*;
import org.lockss.util.*;
import org.lockss.plugin.*;

/**
 * <p>
 * Implements a version one name poll.
 * </p>
 *
 * @author Claire Griffin
 * @version 1.0
 */

public class V1NamePoll extends V1Poll {

  ArrayList m_entries;

  private boolean m_calledSubpoll = false;

  public V1NamePoll(PollSpec pollspec, PollManager pm, PeerIdentity orig,
		    byte[] challenge, long duration, String hashAlg) {
    super(pollspec, pm, orig, challenge, duration);
    m_replyOpcode = V1LcapMessage.NAME_POLL_REP;
    m_tally = new V1PollTally(this, V1_NAME_POLL, m_createTime, duration,
			      V1PollFactory.getQuorum(), // XXX AU-specific
			      hashAlg);
  }

  /** kludge to remember whether we called a subpoll */
  public boolean isSubpollRunning() {
    return m_calledSubpoll;
  }

  /**
   * cast our vote for this poll
   */
  void castOurVote() {
    if (m_msg == null) {
      log.error("No vote to cast for " + this);
      return;
    }
    V1LcapMessage msg;
    PeerIdentity local_id = idMgr.getLocalPeerIdentity(Poll.V1_PROTOCOL);
    long remainingTime = m_deadline.getRemainingTime();
    log.debug("castOurVote: " + local_id);
    try {
      msg = V1LcapMessage.makeReplyMsg(m_msg, m_hash, m_verifier, getEntries(),
				       m_replyOpcode, remainingTime, local_id);
      log.debug("vote:" + msg.toString());
      m_pollmanager.sendMessage(msg, m_cus.getArchivalUnit());
    } catch (IOException ex) {
      log.info("unable to cast our vote.", ex);
    }
  }

  /**
   * handle a message which may be a incoming vote
   *
   * @param msg
   *          the Message to handle
   */
  protected void receiveMessage(LcapMessage msg) {
    if (msg.getProtocolVersion() != 1) {
      log.error("Not a V1 message: " + msg);
      return;
    }
    int opcode = msg.getOpcode();

    if (m_msg == null) {
      m_msg = msg;
      log.debug("Setting message for " + this + " from " + msg);
    }
    if (opcode == V1LcapMessage.NAME_POLL_REP) {
      startVoteCheck((V1LcapMessage)msg);
    }
  }

  /**
   * schedule the hash for this poll.
   *
   * @param digest
   *          the MessageDigest used to hash the content
   * @param timer
   *          the Deadline by which we must complete
   * @param key
   *          the Object which will be returned from the hasher. Always the
   *          message which triggered the hash
   * @param callback
   *          the hashing callback to use on return
   * @return true if hash successfully completed.
   */
  boolean scheduleHash(MessageDigest digest, Deadline timer, Object key,
		       HashService.Callback callback) {

    HashService hs = m_pollmanager.getHashService();
    return hs.scheduleHash(m_cus.getNameHasher(digest),
			   timer, callback, key);
  }

  /**
   * start the hash required for a vote cast in this poll
   *
   * @param msg
   *          the LcapMessage containing the vote we're going to check
   */
  void startVoteCheck(V1LcapMessage msg) {
    super.startVoteCheck();

    if (shouldCheckVote(msg)) {
      Vote vote = new NameVote(msg, false);
      log.debug3("created a new NameVote instead of a Vote");

      MessageDigest digest =
	getInitedDigest(msg.getChallenge(), msg.getVerifier());

      if (!scheduleHash(digest, m_hashDeadline, vote, new VoteHashCallback())) {
	log.info(m_key + " no time to hash vote by " + m_hashDeadline);
	stopVoteCheck();
      }
    }
  }

  void clearEntryList() {
    m_entries = null;
  }

  ArrayList generateEntries() {
    Iterator it = m_cus.flatSetIterator();
    ArrayList alist = new ArrayList();
    CachedUrlSetSpec spec = m_cus.getSpec();
    String baseUrl = spec.getUrl();
    log.debug2("getting a list of entries for spec " + m_cus.getSpec());
    while (it.hasNext()) {
      CachedUrlSetNode cusn = (CachedUrlSetNode) it.next();
      String name = cusn.getUrl();
      if (spec.matches(name)) {
	boolean hasContent = cusn.hasContent();
	if (name.startsWith(baseUrl)) {
	  name = name.substring(baseUrl.length());
	} //XXX add error message
	log.debug3("adding file name " + name + " - hasContent=" + hasContent);
	alist.add(new PollTally.NameListEntry(hasContent, name));
      }
    }
    m_entries = alist;
    return m_entries;
  }

  ArrayList getEntries() {
    if (m_entries == null) {
      generateEntries();
    }
    log.debug2("found " + m_entries.size() + " items in list");
    return m_entries;
  }

  NameVote findWinningVote(Iterator voteIter) {
    ArrayList winners = new ArrayList();
    NameVoteCounter winningCounter = null;

    // build a list of unique disagree votes
    while (voteIter.hasNext()) {
      Object obj = voteIter.next();
      if (!(obj instanceof NameVote)) {
	log.error("Expected class NameVote found class "
		  + obj.getClass().getName());
	continue;
      }
      NameVote vote = (NameVote) obj;
      if (!vote.agree) {
	NameVoteCounter counter = new NameVoteCounter(vote);
	if (winners.contains(counter)) {
	  counter = (NameVoteCounter) winners.get(winners.indexOf(counter));
	  counter.addVote();
	} else {
	  winners.add(counter);
	}
      }
    }

    // find the "winner" with the most votes
    Iterator it = winners.iterator();
    while (it.hasNext()) {
      NameVoteCounter counter = (NameVoteCounter) it.next();
      if (winningCounter != null) {
	if (winningCounter.getNumVotes() < counter.getNumVotes()) {
	  winningCounter = counter;
	}
      } else {
	winningCounter = counter;
      }
    }

    return winningCounter;
  }

  void buildPollLists(Iterator voteIter) {
    NameVote winningVote = findWinningVote(voteIter);
    if (winningVote != null) {
      log.debug("found winning vote: " + winningVote);
      m_tally.votedEntries = winningVote.getKnownEntries();
      if (log.isDebug3()) {
	for (int i = 0; i < m_tally.votedEntries.size(); i++) {
	  log.debug3("winning entry " + i + ": " + m_tally.votedEntries.get(i));
	}
      }
      String lwrRem = winningVote.getLwrRemaining();
      String uprRem = winningVote.getUprRemaining();
      log.debug3("remainder lwr : " + lwrRem + " upr: " + uprRem);
      if (lwrRem != null) {
	callNameSubPoll(m_cus, lwrRem, uprRem);
	// we make our list from whatever is in our
	// master list that doesn't match the remainder;
	ArrayList localSet = new ArrayList();
	Iterator localIt = getEntries().iterator();
	log.debug3("finding local entries which are below our lwr remainder:"
		   + lwrRem);
	while (localIt.hasNext()) {
	  PollTally.NameListEntry entry = (PollTally.NameListEntry) localIt
	    .next();
	  String url = entry.name;
	  if ((lwrRem != null) && url.compareTo(lwrRem) < 0) {
	    log.debug3("adding local entry " + entry);
	    localSet.add(entry);
	  }
	}
	m_tally.localEntries = localSet;
      } else {
	log
	  .debug3("No entries remain to be sent, return all entries for spec: "
		  + m_cus.getSpec());
	m_tally.localEntries = getEntries();
      }
    }
  }

  /**
   * Calls a name poll poll with the lower and upper bounds set.
   *
   * @param cus CachedUrlSet
   * @param lwr lower bound
   * @param upr upper bound
   */
  private void callNameSubPoll(CachedUrlSet cus, String lwr, String upr) {
    String base = cus.getUrl();
    ArchivalUnit au = cus.getArchivalUnit();
    CachedUrlSet newCus = au.makeCachedUrlSet(new RangeCachedUrlSetSpec(base, lwr, upr));
    PollSpec spec = new PollSpec(newCus, lwr, upr, Poll.V1_NAME_POLL);
    log.debug3("calling new name poll on: " + spec);
    Poll subPoll = m_pollmanager.callPoll(spec);
    if (subPoll != null) {
      PollTally subTally = subPoll.getVoteTally();
      if (subTally instanceof V1PollTally) {
	V1PollTally v1tally = (V1PollTally) subTally;
	v1tally.setPreviousNamePollTally((V1PollTally) getVoteTally());
      }
      m_calledSubpoll = true;
    } else {
      log.error("unable to call name poll for " + spec);
    }
  }

  /**
   * make a NameVote. NB - used only by TestPoll
   *
   * @param msg the message needed to make the vote
   * @param agree a boolean set true if this is an agree vote, false otherwise.
   * @return the newly created NameVote object
   */
  NameVote makeNameVote(V1LcapMessage msg, boolean agree) {
    return new NameVote(msg, agree);
  }

  protected Vote copyVote(Vote vote, boolean agree) {
    NameVote v = new NameVote((NameVote) vote);
    v.agree = agree;
    return v;
  }

  /**
   * Return the type of the poll, Poll.V1_NAME_POLL
   */
  public int getType() {
    return Poll.V1_NAME_POLL;
  }

  public ArchivalUnit getAu() {
    return m_tally.getArchivalUnit();
  }

  public String getStatusString() {
    return m_tally.getStatusString();
  }

  public boolean isPollActive() {
    return m_tally.stateIsActive();
  }

  public boolean isPollCompleted() {
    return m_tally.stateIsFinished();
  }

  static class NameVote extends Vote {
    private ArrayList knownEntries;

    private String lwrRemaining;

    private String uprRemaining;

    NameVote(NameVote vote) {
      super(vote);
      knownEntries = vote.getKnownEntries();
      lwrRemaining = vote.getLwrRemaining();
      uprRemaining = vote.getUprRemaining();
    }

    NameVote(V1LcapMessage msg, boolean agree) {
      super(msg, agree);
      knownEntries = msg.getEntries();

      lwrRemaining = msg.getLwrRemain();
      uprRemaining = msg.getUprRemain();
    }

    ArrayList getKnownEntries() {
      return knownEntries;
    }

    String getLwrRemaining() {
      return lwrRemaining;
    }

    String getUprRemaining() {
      return uprRemaining;
    }

    public boolean equals(Object obj) {
      if (obj instanceof NameVote) {
	return (sameEntries(((NameVote) obj).knownEntries));
      }
      return false;
    }

    public int hashCode() {
      throw new UnsupportedOperationException();
    }

    boolean sameEntries(ArrayList entries) {
      return CollectionUtil.isIsomorphic(knownEntries, entries);
    }
  }

  static class NameVoteCounter extends NameVote {
    private int voteCount = 1;

    NameVoteCounter(NameVote vote) {
      super(vote);
    }

    void addVote() {
      voteCount++;
    }

    int getNumVotes() {
      return voteCount;
    }
  }

}
