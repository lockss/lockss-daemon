/*
 * $Id: V3VoterState.java,v 1.1 2005-07-13 07:53:06 smorabito Exp $
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

import org.lockss.util.*;
import org.lockss.poller.*;
import org.lockss.protocol.*;
import org.lockss.protocol.psm.*;
import org.mortbay.util.B64Code;

import java.util.List;

/**
 * This class represents the Poller's view of the state of a voter
 * participating in a V3 poll.
 */
public class V3VoterState {
  private static Logger log = Logger.getLogger("V3VoterState");

  /** Reference to the PollerState (for information like
      AuID, pollKey, hashAlgorithm, etc.) */
  private V3PollerState m_pollerState;
  
  /** The Peer Identity key of the voter. */
  private PeerIdentity m_voterId;

  /** The vote cast by the voter. */
  // XXX: This will be replaced by a V3Vote class.
  private V3LcapMessage m_vote;

  /** The target URL to use when when requesting a repair (different for
   * each request). */
  private String m_targetUrl;

  /** List of PeerIdentity strings this voter has nominated for the outer
      circle. */
  private List m_nominees;

  /** Poll introductory effort proof. */
  private byte[] m_introEffortProof;

  /** Poll ACK effort proof. */
  private byte[] m_pollAckEffortProof;

  /** Remaining effort proof. */
  private byte[] m_remainingEffortProof;

  /** Repair effort proof. */
  private byte[] m_repairEffortProof;

  /** Receipt effort proof. */
  private byte[] m_receiptEffortProof;

  /** Handler for callbacks, used by PollerActions. */
  private V3Poller.ActionHandler m_handler;

  V3VoterState(PeerIdentity voterId,
	       V3PollerState pollerState,
	       V3Poller.ActionHandler handler) {
    this.m_voterId = voterId;
    this.m_pollerState = pollerState;
    this.m_handler = handler;
  }

  public V3Poller.ActionHandler getHandler() {
    return m_handler;
  }

  public void setVoterId(PeerIdentity id) {
    m_voterId = id;
  }

  public PeerIdentity getVoterId() {
    return m_voterId;
  }

  public void setTarget(String url) {
    this.m_targetUrl = url;
  }

  public String getTarget() {
    return m_targetUrl;
  }

  public void setNominees(List l) {
    this.m_nominees = l;
  }

  public List getNominees() {
    return m_nominees;
  }

  public void setIntroEffortProof(byte[] b) {
    m_introEffortProof = b;
  }

  public byte[] getIntroEffortProof() {
    return m_introEffortProof;
  }

  public void setRemainingEffortProof(byte[] b) {
    m_remainingEffortProof = b;
  }

  public byte[] getRemainingEffortProof() {
    return m_remainingEffortProof;
  }

  public void setPollAckEffortProof(byte[] b) {
    m_pollAckEffortProof = b;
  }

  public byte[] getPollAckEffortProof() {
    return m_pollAckEffortProof;
  }

  public void setRepairEffortProof(byte[] b) {
    m_repairEffortProof = b;
  }

  public byte[] getRepairEffortProof() {
    return m_repairEffortProof;
  }

  public void setReceiptEffortProof(byte[] b) {
    m_receiptEffortProof = b;
  }

  public byte[] getReceiptEffortProof() {
    return m_receiptEffortProof;
  }

  public void setVote(V3LcapMessage msg) {
    m_vote = msg;
  }

  public V3LcapMessage getVote() {
    return m_vote;
  }

  /*
   * PollerState proxy methods used by PollerActions.
   */

  public String getPollKey() {
    return m_pollerState.getPollKey();
  }

  public PeerIdentity getPollerId() {
    return m_pollerState.getPollerId();
  }

  public V3PollerState getPollerState() {
    return m_pollerState;
  }

  public V3Poller getPoller() {
    return m_pollerState.getPoller();
  }

  public PollSpec getPollSpec() {
    return m_pollerState.getPollSpec();
  }

  public byte[] getChallenge() {
    return m_pollerState.getChallenge();
  }

  public Deadline getDeadline() {
    return m_pollerState.getDeadline();
  }

  // XXX: Could be better.
  public String toString() {
    return "[V3VoterState: voterId=" +
      m_voterId + ", pollerId=" +
      getPollerId() + "]";
  }
}
