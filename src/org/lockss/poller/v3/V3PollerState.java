/*
 * $Id: V3PollerState.java,v 1.2 2005-08-12 18:26:37 thib_gc Exp $
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

import java.util.*;
import java.io.*;

import org.lockss.util.*;
import org.lockss.poller.*;
import org.lockss.protocol.*;
import org.lockss.protocol.psm.*;
import org.mortbay.util.B64Code;

/**
 * Record the current status of a poll.  Each V3 Poll has one and only
 * one V3PollerState on the Poller side.  This object must be
 * serializable so that it can persist between daemon invocations.
 *
 * There is only one V3PollerState object per poll.  A poll must be
 * reconstructable from this object.
 *
 * --------------------------------------------------------------------
 * XXX: Proper serialization is NOT YET handled.  This class should
 * ultimately know how to serialize itself in a transactional way when
 * important changes occur!
 * --------------------------------------------------------------------
 */
public class V3PollerState implements Serializable {

  /** The key of this poll */
  private String m_pollKey;

  /** The deadline for this poll */
  private Deadline m_deadline;

  /** The challenge hash for this poll */
  private byte[] m_challenge;

  private PollSpec m_pollSpec;

  /** The hash algorithm used to generate the challenge hash */
  private String m_hashAlgorithm;

  /** The AU ID for this poll */
  private String m_auId;

  /** Originator for this poll */
  private PeerIdentity m_origin;

  /** A map of peer identities to VoterState objects */
  private HashMap m_innerCircle;

  /** The set of voter Peer IDs currently not in the tally state.  This
      is used by the poller when waiting to trigger repairs */
  private Set m_votersNotTallied;

  /** The target URL of the most recently hashed block. Updated after
      each block is hashed and tallied by V3Poller.  Used when returning
      to tally more blocks after requesting a repair, or when sending a
      vote request for the next in a sequence of votes. */
  private String m_lastHashedBlock;

  /** Reference to the poller. */
  private V3Poller m_poller;

  private static final Logger log = Logger.getLogger("V3PollerState");

  protected V3PollerState() {
    m_innerCircle = new HashMap();
  }

  public V3PollerState(PollSpec spec, PeerIdentity orig, byte[] challenge,
		       V3Poller poller,	 String pollKey, long duration,
		       String hashAlg) {
    this();
    this.m_origin = orig;
    this.m_challenge = challenge;
    this.m_pollKey = pollKey;
    this.m_hashAlgorithm = hashAlg;
    this.m_deadline = Deadline.in(duration);
    this.m_auId = spec.getAuId();
    this.m_votersNotTallied = new HashSet();
    this.m_pollSpec = spec;
    this.m_poller = poller;
  }

  public void setPollKey(String id) {
    m_pollKey = id;
  }

  public String getPollKey() {
    return m_pollKey;
  }

  public PollSpec getPollSpec() {
    return m_pollSpec;
  }

  public void setPollSpec(PollSpec ps) {
    m_pollSpec = ps;
  }

  public PeerIdentity getPollerId() {
    return m_origin;
  }

  public V3Poller getPoller() {
    return m_poller;
  }

  public HashMap getInnerCircle() {
    return m_innerCircle;
  }

  public void addVoterState(PeerIdentity id, V3VoterState state) {
    // XXX: Need to handle inner and outer circles.
    m_innerCircle.put(id, state);
    m_votersNotTallied.add(id);
  }

  public V3VoterState getVoterState(PeerIdentity id) {
    // XXX:  Need to handle inner and outer circles.
    return (V3VoterState)m_innerCircle.get(id);
  }

  public void setChallenge(byte[] b) {
    m_challenge = b;
  }

  public byte[] getChallenge() {
    return m_challenge;
  }

  public void setHashAlgorithm(String s) {
    m_hashAlgorithm = s;
  }

  public String getHashAlgorithm() {
    return m_hashAlgorithm;
  }

  public void setLastHashedBlock(String target) {
    this.m_lastHashedBlock = target;
  }

  public String getLastHashedBlock() {
    return m_lastHashedBlock;
  }

  /**
   * Indicate that the given participant is currently tallied.
   */
  public void voterTallied(PeerIdentity id) {
    m_votersNotTallied.remove(id);
  }
 
  /**
   * Indicate that the given participant is not currently tallied.
   */
  public void voterNotTallied(PeerIdentity id) {
    m_votersNotTallied.add(id);
  }

  /**
   * Returns true iff all voters have been tallied.
   */
  public boolean allVotersTallied() {
    return m_votersNotTallied.isEmpty();
  }

  /**
   * Set the deadline for this poll.
   */
  public void setDeadline(Deadline d) {
    m_deadline = d;
  }

  /**
   * Get the deadline for this poll.
   */
  public Deadline getDeadline() {
    return m_deadline;
  }

  public String toString() {
    StringBuffer sb = new StringBuffer("[V3PollerState: ");
    sb.append("pollKey=" + m_pollKey + ", ");
    sb.append("challenge=" + m_challenge + ", ");
    sb.append("hashAlgorithm=" + m_hashAlgorithm + ", ");
    sb.append("deadline=" + m_deadline + ", ");
    sb.append("innerCircle={");
    for (Iterator iter = m_innerCircle.values().iterator(); iter.hasNext(); ) {
      sb.append(((V3VoterState)iter.next()).toString());
    }
    sb.append("}]");
    return sb.toString();
  }
}
