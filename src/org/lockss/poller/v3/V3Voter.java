/*
 * $Id: V3Voter.java,v 1.1 2005-07-13 07:53:06 smorabito Exp $
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

import org.lockss.poller.*;
import org.lockss.protocol.*;
import org.lockss.protocol.psm.*;
import org.lockss.util.*;

/**
 * Represents a voter in a V3 poll.
 *
 * State is maintained in a V3VoterState object.  On the voter's side
 * of a poll, this object is transient.
 */
public class V3Voter {

  private PsmInterp m_stateMachine;

  private String m_pollKey;
  private String m_hashAlg;
  private String m_auId;
  private PeerIdentity m_pollerId;

  private static final Logger log = Logger.getLogger("V3Voter");

  public V3Voter(PollSpec spec, PeerIdentity orig, String key, String hashAlg) {
    m_pollerId = orig;
    m_pollKey = key;
    m_hashAlg = hashAlg;
    m_auId = spec.getAuId();
  }

  public void startPoll() {
    PsmMachine machine = VoterStateMachineFactory.
      getMachine(getVoterActionsClass());
    PsmInterp interp = new PsmInterp(machine, this);
    m_stateMachine = interp;
    interp.init();
  }

  public void stopPoll() {
    // XXX
  }

  
  // XXX: Called by callback when a vote has been successfully sent.
  private void voteCast() {
    log.debug("Our vote has been cast.");
//     PsmMachine machine = VoterRepairStateMachineFactory.
//       getMachine(getVoterRepairActionsClass());
//     PsmInterp interp = new PsmInterp(machine, m_voterState);
//     m_stateMachine = interp;
//     interp.init();
  }

  /**
   * Called by the Error callback in the event that an
   * unrecoverable error occurs.
   */
  private void error(Throwable t) {
    // XXX: Implement
  }

  Class getVoterActionsClass() {
    return VoterActions.class;
  }

  /**
   * Handle an incoming V3LcapMessage.
   */
  public void handleMessage(V3LcapMessage msg) {
    PeerIdentity sender = msg.getOriginatorId();
    PsmMsgEvent evt = V3Events.fromMessage(msg);
    m_stateMachine.handleEvent(evt);
  }

  public interface VoterHandler {
    
  }
}
