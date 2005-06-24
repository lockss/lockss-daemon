/*
 * $Id: PollerActions.java,v 1.1 2005-06-24 07:59:16 smorabito Exp $
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

import java.util.*;

import org.lockss.protocol.psm.*;
import org.lockss.protocol.*;
import org.lockss.util.*;

public class PollerActions {

  private static final Logger log = Logger.getLogger("PollerActions");

  public static PsmEvent handleInitialize(PsmEvent evt, PsmInterp interp) {
    // XXX: Implement.
    return V3Events.evtOk;
  }

  public static PsmEvent handleProveIntroEffort(PsmEvent evt, PsmInterp interp) {
    // XXX: Implement.
    return V3Events.evtOk;
  }

  public static PsmEvent handleSendPoll(PsmEvent evt, PsmInterp interp) {
    // XXX: Implement.
    return V3Events.evtOk;
  }

  public static PsmEvent handleReceivePollAck(PsmMsgEvent evt, PsmInterp interp) {
    // XXX: Implement.
    return V3Events.evtOk;
  }

  public static PsmEvent handleVerifyPollAckEffort(PsmEvent evt, PsmInterp interp) {
    // XXX: Implement.
    return V3Events.evtOk;
  }

  public static PsmEvent handleProveRemainingEffort(PsmEvent evt,
						    PsmInterp interp) {
    // XXX: Implement.
    return V3Events.evtOk;
  }

  public static PsmEvent handleSendPollProof(PsmEvent evt, PsmInterp interp) {
    // XXX: Implement.
    return V3Events.evtOk;
  }

  public static PsmEvent handleReceiveNominate(PsmMsgEvent evt, PsmInterp interp) {
    // XXX: Implement
    return V3Events.evtOk;
  }

  public static PsmEvent handleSendVoteRequest(PsmEvent evt, PsmInterp interp) {
    // XXX: Implement.
    return V3Events.evtOk;
  }

  public static PsmEvent handleReceiveVote(PsmMsgEvent evt, PsmInterp interp) {
    // XXX: Implement.
    return V3Events.evtOk;
  }

  public static PsmEvent handleTallyVote(PsmEvent evt, PsmInterp interp) {
    // XXX: Implement.
    return V3Events.evtOk;
  }

  public static PsmEvent handleProveRepairEffort(PsmEvent evt, PsmInterp interp) {
    // XXX: Implement.
    return V3Events.evtOk;
  }

  public static PsmEvent handleSendRepairRequest(PsmEvent evt, PsmInterp interp) {
    // XXX: Implement.
    return V3Events.evtOk;
  }

  public static PsmEvent handleReceiveRepair(PsmMsgEvent evt, PsmInterp interp) {
    // XXX: Implement.
    return V3Events.evtOk;
  }

  public static PsmEvent handleProcessRepair(PsmEvent evt, PsmInterp interp) {
    // XXX: Implement.
    return V3Events.evtOk;
  }

  public static PsmEvent handleSendReceipt(PsmEvent evt, PsmInterp interp) {
    // XXX: Implement.
    return V3Events.evtOk;
  }

  public static PsmEvent handleError(PsmEvent evt, PsmInterp interp) {
    // XXX: Implement.
    return V3Events.evtOk;
  }

  /**
   * Convenience method.
   */
  private static ParticipantState getVoterState(PsmInterp interp) {
    return (ParticipantState)interp.getUserData();
  }
}
