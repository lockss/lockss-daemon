/*
 * $Id: V1NamePoll.java,v 1.7 2004-09-28 08:53:16 tlipkis Exp $
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
 * <p>Implements a version one name poll.</p>
 * @author Claire Griffin
 * @version 1.0
 */

public class V1NamePoll extends V1Poll {

  ArrayList m_entries;

  public V1NamePoll(LcapMessage msg,
			    PollSpec pollspec, PollManager pm) {
    super(msg, pollspec, pm);
    m_replyOpcode = LcapMessage.NAME_POLL_REP;
    m_tally = new V1PollTally(this,
                              NAME_POLL,
                              m_createTime,
                              msg.getDuration(),
                              V1PollFactory.getQuorum(),  // XXX AU-specific
                              msg.getHashAlgorithm());
  }

  /**
   * cast our vote for this poll
   */
  void castOurVote() {
    LcapMessage msg;
    PeerIdentity local_id = idMgr.getLocalPeerIdentity();
    long remainingTime = m_deadline.getRemainingTime();
    log.debug("castOurVote: " + local_id);
    try {
      msg = LcapMessage.makeReplyMsg(m_msg, m_hash, m_verifier,
                                     getEntries(), m_replyOpcode,
                                     remainingTime, local_id);
      log.debug("vote:" + msg.toString());
      m_pollmanager.sendMessage(msg, m_cus.getArchivalUnit());
    }
    catch (IOException ex) {
      log.info("unable to cast our vote.", ex);
    }
  }


  /**
   * handle a message which may be a incoming vote
   * @param msg the Message to handle
   */
  void receiveMessage(LcapMessage msg) {
    int opcode = msg.getOpcode();

    if (opcode == LcapMessage.NAME_POLL_REP) {
      startVoteCheck(msg);
    }
  }

  /**
   * schedule the hash for this poll.
   * @param hasher the MessageDigest used to hash the content
   * @param timer the Deadline by which we must complete
   * @param key the Object which will be returned from the hasher. Always the
   * message which triggered the hash
   * @param callback the hashing callback to use on return
   * @return true if hash successfully completed.
   */
  boolean scheduleHash(MessageDigest hasher, Deadline timer, Object key,
                       HashService.Callback callback) {

    HashService hs = m_pollmanager.getHashService();
    return hs.hashNames(m_cus, hasher, timer, callback, key);
  }

  /**
   * start the hash required for a vote cast in this poll
   * @param msg the LcapMessage containing the vote we're going to check
   */
  void startVoteCheck(LcapMessage msg) {
    super.startVoteCheck();

    if (shouldCheckVote(msg)) {
      Vote vote = new NameVote(msg, false);
      log.debug3("created a new NameVote instead of a Vote");

      MessageDigest hasher = getInitedHasher(msg.getChallenge(),
                                             msg.getVerifier());

      if (!scheduleHash(hasher, m_hashDeadline, vote,
			new VoteHashCallback())) {
        log.info(m_key + " no time to hash vote by " + m_hashDeadline);
        stopVoteCheck();
      }
    }
  }


  ArrayList getEntries() {
    if (m_entries == null) {
      Iterator it = m_cus.flatSetIterator();
      ArrayList alist = new ArrayList();
      String baseUrl = m_cus.getSpec().getUrl();
      log.debug2("getting a list of entries for base url " + baseUrl);
      while(it.hasNext()) {
        CachedUrlSetNode cusn = (CachedUrlSetNode)it.next();
        String name = cusn.getUrl();
        boolean hasContent = cusn.hasContent();
        if(name.startsWith(baseUrl)) {
          name = name.substring(baseUrl.length());
        }

        log.debug3("adding file name "+ name +" - hasContent=" + hasContent);
        alist.add(new PollTally.NameListEntry(hasContent, name));
      }
      m_entries = alist;
    }
    log.debug2("successfully added " + m_entries.size() + " items to list");
    return m_entries;
  }

  NameVote findWinningVote(Iterator voteIter) {
    ArrayList winners = new ArrayList();
    NameVoteCounter winningCounter = null;

    // build a list of unique disagree votes
    while (voteIter.hasNext()) {
      Object obj = voteIter.next();
      if (! (obj instanceof NameVote)) {
        log.error("Expected class NameVote found class " +
                  obj.getClass().getName());
        continue;
      }
      NameVote vote = (NameVote) obj;
      if (!vote.agree) {
        NameVoteCounter counter = new NameVoteCounter(vote);
        if (winners.contains(counter)) {
          counter = (NameVoteCounter) winners.get(winners.indexOf(counter));
          counter.addVote();
        }
        else {
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
      }
      else {
        winningCounter = counter;
      }
    }

    return winningCounter;
  }

  void buildPollLists(Iterator voteIter) {
    log.debug3("buildPollLists");
    NameVote winningVote = findWinningVote(voteIter);
    log.debug("found winning vote: " + winningVote);
    if (winningVote != null) {
      log.debug3("buildPollLists 2");
      m_tally.votedEntries = winningVote.getKnownEntries();
      String lwrRem = winningVote.getLwrRemaining();
      String uprRem = winningVote.getUprRemaining();
      log.debug3("buildPollLists 2");
      if (lwrRem != null) {
        // we call a new poll on the remaining entries and set the regexp
	PollSpec spec = new PollSpec(m_pollspec.getCachedUrlSet(),
				     lwrRem, uprRem,
				     Poll.NAME_POLL);
	if (m_pollmanager.callPoll(spec)) {
          log.error("unable to call name poll for " + spec);
        }
	log.debug3("buildPollLists 3");
        // we make our list from whatever is in our
        // master list that doesn't match the remainder;
        ArrayList localSet = new ArrayList();
        Iterator localIt = getEntries().iterator();
	log.debug3("buildPollLists 4");
        while (localIt.hasNext()) {
	  log.debug3("buildPollLists 5");
          PollTally.NameListEntry entry = (PollTally.NameListEntry) localIt.next();
          String url = entry.name;
          if((lwrRem != null) && url.compareTo(lwrRem) < 0) {
            localSet.add(entry);
          }
          else if((uprRem != null) && url.compareTo(uprRem) > 0) {
            localSet.add(entry);
          }
        }
        m_tally.localEntries = localSet;
      } else {
        m_tally.localEntries = getEntries();
      }
    }
  }

  /**
   * make a NameVote.  NB - used only by TestPoll
   * @param msg the message needed to make the vote
   * @param agree a boolean set true if this is an agree vote, false otherwise.
   * @return the newly created NameVote object
   */
  NameVote makeNameVote(LcapMessage msg, boolean agree) {
    return new NameVote(msg, agree);
  }

  Vote copyVote(Vote vote, boolean agree) {
    NameVote v =  new NameVote((NameVote)vote);
    v.agree = agree;
    return v;
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

    NameVote(LcapMessage msg, boolean agree) {
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
        return (sameEntries( ( (NameVote) obj).knownEntries));
      }
      return false;
    }

    public int hashCode() {
      throw new UnsupportedOperationException();
    }

    boolean sameEntries(ArrayList entries) {
      return CollectionUtil.isIsomorphic(knownEntries,entries);
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
