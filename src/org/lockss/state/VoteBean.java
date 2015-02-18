/*
 * $Id$
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

package org.lockss.state;

import org.lockss.poller.Vote;
import org.lockss.protocol.IdentityManager;
import java.net.UnknownHostException;

/**
 * Simple class to allow marshalling of Vote instances.  It should be
 * applied only to inactive Vote objects, i.e. those in closed Polls.
 * XXX This makes it an open question as to why it exists at all.
 */
public class VoteBean extends Vote {
  public String idStr = null;
  public String challengeStr = null;
  public String verifierStr = null;
  public String hashStr = null;  // V1 only
  // XXX Need to cope with V3

  /**
   * Empty constructor for bean creation during marshalling
   */
  public VoteBean() {
  }

  /**
   * Constructor for converting from a Vote object.  Called by
   * PollHistoryBean.convertVotesToVoteBeans().
   * @param vote the Vote
   */
  VoteBean(Vote vote) {
    idStr = vote.getVoterIdentity().getIdString();
    agree = vote.isAgreeVote();
    challengeStr = vote.getChallengeString();
    verifierStr = vote.getVerifierString();
    hashStr = vote.getHashString();
  }

  /**
   * Returns a Vote object in the active state based on the values in the
   * VoteBean class.
   * @return a Vote object
   */
  Vote getVote() {
    return super.makeVote(challengeStr, verifierStr, hashStr, idStr, agree);
  }

  /**
   * Returns a Vote object in the inactive state based on the values in the
   * VoteBean class.
   * @return a Vote object
   */
  Vote getInactiveVote() {
    return super.makeVote(null, null, null, idStr, agree);
  }

  /**
   * Returns the vote's id string.
   * @return the id string
   */
  public String getId() {
    return idStr;
  }

  /**
   * Sets the id string.
   * @param idStr the new id
   */
  public void setId(String idStr) {
    this.idStr = idStr;
  }

  /**
   * Returns the agree/disagree state.
   * @return the agree state
   */
  public boolean getAgreeState() {
    return agree;
  }

  /**
   * Sets the agree/disagree state.
   * @param agree_state the new agree state
   */
  public void setAgreeState(boolean agree_state) {
    agree = agree_state;
  }

  /**
   * Returns the vote's challenge in Base64.
   * @return the challenge
   */
  public String getChallengeString() {
    return challengeStr;
  }

  /**
   * Sets the challenge.
   * @param challengeStr the new challenge
   */
  public void setChallengeString(String challengeStr) {
    this.challengeStr = challengeStr;
  }

  /**
   * Returns the vote's verifier in Base64.
   * @return the verifier
   */
  public String getVerifierString() {
    return verifierStr;
  }

  /**
   * Sets the verifier.
   * @param verifierStr the new verifier
   */
  public void setVerifierString(String verifierStr) {
    this.verifierStr = verifierStr;
  }

  /**
   * Returns the vote's hash in Base64.
   * @return the hash
   */
  public String getHashString() {
    return hashStr;
  }

  /**
   * Sets the hash.
   * @param hashStr the new hash
   */
  public void setHashString(String hashStr) {
    this.hashStr = hashStr;
  }

}
