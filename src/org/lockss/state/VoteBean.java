/*
 * $Id: VoteBean.java,v 1.2 2002-12-13 23:51:32 aalto Exp $
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

package org.lockss.state;

public class VoteBean {
  public String idStr = null;
  public boolean agree = false;
  public String challenge = null;
  public String verifier = null;
  public String hash = null;

  /**
   * Empty constructor for bean creation during marshalling
   */
  public VoteBean() {
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
  public String getChallenge() {
    return challenge;
  }

  /**
   * Sets the challenge.
   * @param challenge the new challenge
   */
  public void setChallenge(String challenge) {
    this.challenge = challenge;
  }

  /**
   * Returns the vote's verifier in Base64.
   * @return the verifier
   */
  public String getVerifier() {
    return verifier;
  }

  /**
   * Sets the verifier.
   * @param verifier the new verifier
   */
  public void setVerifier(String verifier) {
    this.verifier = verifier;
  }

  /**
   * Returns the vote's hash in Base64.
   * @return the hash
   */
  public String getHash() {
    return hash;
  }

  /**
   * Sets the hash.
   * @param hash the new hash
   */
  public void setHash(String hash) {
    this.hash = hash;
  }

}