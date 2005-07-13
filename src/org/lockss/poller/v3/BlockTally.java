/*
 * $Id: BlockTally.java,v 1.1 2005-07-13 07:53:06 smorabito Exp $
 */

/*

Copyright (c) 2000-2002 Board of Trustees of Leland Stanford Jr. University,
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

import org.lockss.poller.*;
import org.lockss.util.*;
import org.lockss.protocol.*;
import org.lockss.config.*;

import java.util.*;

/**
 * Representation of the tally for an individual vote block.
 */
public class BlockTally {

  // XXX: These may want to becom different than the V1 config
  // parameters.  Otherwise, just put those somewhere common and use them.
  static final String PARAM_VOTE_MARGIN = Configuration.PREFIX +
    "poll.voteMargin";
  static final String PARAM_TRUSTED_WEIGHT = Configuration.PREFIX +
    "poll.trustedWeight";
  static final String PARAM_QUORUM = Configuration.PREFIX +
    "poll.quorum";
  static final int DEFAULT_VOTE_MARGIN = 75;
  static final int DEFAULT_TRUSTED_WEIGHT = 350;
  static final int DEFAULT_QUORUM = 5;

  // List of agreeing voter PeerIdentities.
  List m_disagreeVotes;
  // List of disagreeing voter PeerIdentities.
  List m_agreeVotes;
  // Number of participants required for quorum
  int m_quorum;

  public static final int RESULT_POLLING = 0;
  public static final int RESULT_ERROR = 1;
  public static final int RESULT_NOQUORUM = 2;
  public static final int RESULT_TOO_CLOSE = 3;
  public static final int RESULT_UNTRUSTED = 4;
  public static final int RESULT_WON = 5;
  public static final int RESULT_LOST = 6;

  private int m_result;

  private static final String[] m_resultStrings =
  { "Polling", "Error", "No Quorum", "Too Close",
    "Untrusted", "Won", "Lost" };

  // The margin by which we must win or lose
  double m_voteMargin = 0;
  // The min avg. weight of the winners, when we lose.
  double m_trustedWeight = 0;

  BlockTally() {
    this.m_disagreeVotes = new ArrayList();
    this.m_agreeVotes = new ArrayList();
    this.m_quorum = Configuration.getIntParam(PARAM_QUORUM, DEFAULT_QUORUM);
    this.m_voteMargin = 
      ((double)Configuration.getIntParam(PARAM_VOTE_MARGIN,
					 DEFAULT_VOTE_MARGIN)) / 100;
    this.m_trustedWeight = 
      (double)Configuration.getIntParam(PARAM_TRUSTED_WEIGHT,
					DEFAULT_TRUSTED_WEIGHT);
    this.m_result = RESULT_POLLING;
  }
  
  public void addAgreeVote(PeerIdentity id) {
    m_agreeVotes.add(id);
  }

  public void addDisagreeVote(PeerIdentity id) {
    m_disagreeVotes.add(id);
  }

  public int getResult() {
    return m_result;
  }

  public String getResultString() {
    return m_resultStrings[m_result];
  }

  void tallyVotes() {
    if (!haveQuorum()) {
      m_result = RESULT_NOQUORUM;
    }
    else if (!isWithinMargin()) {
      m_result = RESULT_TOO_CLOSE;
    }
    else {
      boolean won = m_agreeVotes.size() > m_disagreeVotes.size();
      if (!won && !isTrustedResult()) {
	m_result = RESULT_UNTRUSTED;
      } else {
	m_result = won ? RESULT_WON : RESULT_LOST;
      }
    }
  } 

  boolean haveQuorum() {
    return m_agreeVotes.size() + m_disagreeVotes.size() >= m_quorum;
  }

  boolean isWithinMargin() {
    double numVotes = m_agreeVotes.size() + m_disagreeVotes.size();
    if (numVotes == 0) {
      return true;
    }
    double agreeVotes = (double)m_agreeVotes.size();
    double disagreeVotes = (double)m_disagreeVotes.size();
    double margin;
    if (agreeVotes > disagreeVotes) {
      margin = (double)(agreeVotes / numVotes);
    } else {
      margin = (double)(disagreeVotes / numVotes);
    }
    return margin > m_voteMargin;
  } 

  // XXX: Reputation system TBD
  boolean isTrustedResult() {
    return true;
  }

  public List getDisagreeVotes() {
    return m_disagreeVotes;
  }

  public List getAgreeVotes() {
    return m_agreeVotes;
  }

}
