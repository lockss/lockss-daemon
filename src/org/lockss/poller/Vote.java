/*
* $Id: Vote.java,v 1.11 2004-09-13 04:02:21 dshr Exp $
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

import org.mortbay.util.B64Code;
import org.lockss.protocol.LcapIdentity;
import org.lockss.protocol.LcapMessage;
import java.util.Arrays;
import java.io.Serializable;
import org.lockss.app.LockssDaemon;
import org.lockss.util.*;

/**
 * Vote stores the information need to replay a single vote. These are needed
 * to run a repair poll.
 */
public class Vote implements Serializable {
  // We don't store the LcapIdentity object itself so that IdentityManager
  // can change the representation of identities dynamically.
  private String voterID = null;
  protected boolean agree = false;
  private ActiveVote activeInfo = null;

  protected Vote() {
  }


  Vote(byte[] challenge, byte[] verifier, byte[] hash,
       LcapIdentity id, boolean agree) {
    this.voterID = id.getIdKey();
    this.agree = agree;
    activeInfo = new V1ActiveVote(challenge, verifier, hash);
  }

  Vote(byte[] challenge, byte[] verifier, byte[] hash,
       String idKey, boolean agree) {
    this.voterID = idKey;
    this.agree = agree;
    activeInfo = new V1ActiveVote(challenge, verifier, hash);
  }

  Vote(Vote vote) {
    this(vote.getChallenge(),vote.getVerifier(),vote.getHash(),
         vote.voterID,vote.agree);
  }

  Vote(LcapMessage msg, boolean agree) {
    this(msg.getChallenge(), msg.getVerifier(), msg.getHashed(),
         msg.getOriginatorID(), agree);
  }

  /**
   * Create a Vote object from strings.  used by VoteBean
   * XXX VoteBean should not need the challenge, verifier and hash.
   * @param challengeStr B64 encoded string of the challenge
   * @param verifierStr B64 encoded string of the challenge
   * @param hashStr B64 encoded string of the challenge
   * @param idStr the string ID of the voter's identity
   * @param agree boolean indiciating agreement or disagreement
   * @return newly created Vote object
   */

  protected Vote makeVote(String challengeStr, String verifierStr, String hashStr,
                 String idStr, boolean agree) {
    Vote vote = new Vote();
    vote.voterID = idStr;
    vote.agree = agree;
    // XXX the ActiveVote info should not be needed
    if (challengeStr != null && verifierStr != null && hashStr != null) {
      activeInfo = new V1ActiveVote(B64Code.decode(challengeStr.toCharArray()),
				    B64Code.decode(verifierStr.toCharArray()),
				    B64Code.decode(hashStr.toCharArray()));
    }
    return vote;
  }

  /**
   * Discard the information no longer needed after the poll has finished.
   */
  protected void pollClosed() {
    activeInfo = null;
  }

  /**
   * Set the agreement state of the Vote according to whether the argument
   * hash matches the stored hash.
   * @param new_hash a byte array containing the hash to be compared
   * @return true if the argument and stored hashes match
   */
  boolean setAgreeWithHash(byte[] new_hash) {
    if (activeInfo != null) {
      byte[] hash = ((V1ActiveVote) activeInfo).getHash();
      agree = Arrays.equals(hash, new_hash);
    }

    return agree;
  }

  public String toString() {
    StringBuffer sbuf = new StringBuffer();
    sbuf.append(agree ? "[YesVote: " : "[NoVote: ");
    sbuf.append("from " + voterID);
    if (activeInfo != null) {
      V1ActiveVote ai = (V1ActiveVote) activeInfo;
      sbuf.append(" C(" + getChallengeString());
      sbuf.append(") V(" + getVerifierString());
      sbuf.append(") H(" + getHashString());
      sbuf.append(")");
    }
    sbuf.append("]");
    return sbuf.toString();
  }

  /**
   * Return the Identity key of the voter
   * @return <code>LcapIdentity</code> the id
   */
  public String getIdentityKey() {
    return voterID;
  }

  /**
   * return whether we agreed or disagreed with this vote
   * @return booleean true if we agree; false otherwise
   */
  public boolean isAgreeVote() {
    return agree;
  }

  /**
   * Return the challenge bytes of the voter
   * @return the array of bytes of the challenge
   */
  public byte[] getChallenge() {
    byte[] ret = null;
    if (activeInfo != null) {
      ret = activeInfo.getChallenge();
    }
    return ret;
  }

  /**
   * Return the challenge bytes as a string
   * @return a String representing the challenge
   */
  public String getChallengeString() {
    String ret = null;
    if (activeInfo != null) {
      ret = String.valueOf(B64Code.encode(activeInfo.getChallenge()));
    }
    return ret;
  }

  /**
   * Return the bytes of the hash computed by this voter
   * @return the array of bytes of the hash
   */
  public byte[] getHash() {
    byte[] ret = null;
    if (activeInfo != null) {
      ret = activeInfo.getHash();
    }
    return ret;
  }

  /**
   * Return the hash bytes as a string
   * @return a String representing the hash
   */
  public String getHashString() {
    String ret = null;
    if (activeInfo != null) {
      byte[] hash = activeInfo.getHash();
      if(hash != null) {
	ret = String.valueOf(B64Code.encode(hash));
      }
    }
    return ret;
  }

  /**
   * Return the verifer bytes of the voter
   * @return the array of bytes of the verifier
   */
  public byte[] getVerifier() {
    byte[] ret = null;
    if (activeInfo != null) {
      ret = activeInfo.getVerifier();
    }
    return ret;
  }

  /**
   * Return the verifier bytes as a string
   * @return a String representing the verifier
   */
  public String getVerifierString() {
    String ret = null;
    if (activeInfo != null) {
      byte[] verifier = activeInfo.getVerifier();
      ret = String.valueOf(B64Code.encode(verifier));
    }
    return ret;
  }

  public String getPollKey() {
    return getChallengeString();
  }

}

class ActiveVote {
  byte[] challenge;
  byte[] verifier;

  byte[] getChallenge() {
    return challenge;
  }

  byte[] getVerifier() {
    return verifier;
  }

  byte[] getHash() {
    return null;
  }
}

class V1ActiveVote extends ActiveVote {
  byte[] hash;

  V1ActiveVote(byte[] c, byte[] v, byte[] h) {
    challenge = c;
    verifier = v;
    hash = h;
  }

  byte[] getHash() {
    return hash;
  }
}

