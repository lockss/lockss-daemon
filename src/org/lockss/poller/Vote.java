/*
* $Id: Vote.java,v 1.8 2003-04-10 01:06:51 claire Exp $
 */

/*

Copyright (c) 2002 Board of Trustees of Leland Stanford Jr. University,
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
import java.net.InetAddress;

/**
 * Vote stores the information need to replay a single vote. These are needed
 * to run a repair poll.
 */
public class Vote implements Serializable {
  private InetAddress voteAddr;
  protected boolean agree;
  private byte[] challenge;
  private byte[] verifier;
  private byte[] hash;


  protected Vote() {
  }


  Vote(byte[] challenge, byte[] verifier, byte[] hash,
       InetAddress addr, boolean agree) {
    this.voteAddr = addr;
    this.agree = agree;
    this.challenge = challenge;
    this.verifier = verifier;
    this.hash = hash;
  }

  Vote(Vote vote) {
    this(vote.getChallenge(),vote.getVerifier(),vote.getHash(),
         vote.getIDAddress(),vote.agree);
  }

  Vote(LcapMessage msg, boolean agree) {
    this(msg.getChallenge(), msg.getVerifier(), msg.getHashed(),
         msg.getOriginAddr(), agree);
  }


  protected Vote makeVote(String challengeStr, String verifierStr, String hashStr,
                 String idStr, boolean agree) throws java.net.UnknownHostException{
    Vote vote = new Vote();
    vote.voteAddr = LcapIdentity.stringToAddr(idStr);
    vote.agree = agree;
    vote.challenge = B64Code.decode(challengeStr.toCharArray());
    vote.verifier = B64Code.decode(verifierStr.toCharArray());
    vote.hash = B64Code.decode(hashStr.toCharArray());
    return vote;
  }

  boolean setAgreeWithHash(byte[] new_hash) {
    agree = Arrays.equals(hash, new_hash);

    return agree;
  }

  public String toString() {
    StringBuffer sbuf = new StringBuffer();
    sbuf.append("[Vote: ");
    sbuf.append("from " + voteAddr);
    sbuf.append(" C(" + getChallengeString());
    sbuf.append(") V(" + getVerifierString());
    sbuf.append(") H(" + getHashString());
    sbuf.append(")]");
    return sbuf.toString();
  }

  /**
   * Return the Identity of the voter
   * @return <code>LcapIdentity</code> the id
   */
  public InetAddress getIDAddress() {
    return voteAddr;
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
    return challenge;
  }

  /**
   * Return the challenge bytes as a string
   * @return a String representing the challenge
   */
  public String getChallengeString() {
    return String.valueOf(B64Code.encode(challenge));
  }

  /**
   * Return the bytes of the hash computed by this voter
   * @return the array of bytes of the hash
   */
  public byte[] getHash() {
    return hash;
  }

  /**
   * Return the hash bytes as a string
   * @return a String representing the hash
   */
  public String getHashString() {
    if(hash != null) {
      return String.valueOf(B64Code.encode(hash));
    }
    else
      return null;
  }

  /**
   * Return the verifer bytes of the voter
   * @return the array of bytes of the verifier
   */
  public byte[] getVerifier() {
    return verifier;
  }

  /**
   * Return the verifier bytes as a string
   * @return a String representing the verifier
   */
  public String getVerifierString() {
    return String.valueOf(B64Code.encode(verifier));
  }

  public String getPollKey() {
    return getChallengeString();
  }

}