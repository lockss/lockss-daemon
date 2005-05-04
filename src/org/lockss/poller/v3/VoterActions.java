/*
 * $Id: VoterActions.java,v 1.1 2005-05-04 19:04:11 smorabito Exp $
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

import org.lockss.protocol.psm.*;

public class VoterActions {
  public static PsmEvent handleInitialize(PsmEvent evt, PsmInterp interp) {
    // XXX: Implement.
    return V3Events.evtOk;
  }

  public static PsmEvent handleVerifyPollEffort(PsmEvent evt, PsmInterp interp) {
    // XXX: Implement.
    return V3Events.evtOk;
  }

  public static PsmEvent handleProvePollAck(PsmEvent evt, PsmInterp interp) {
    // XXX: Implement.
    return V3Events.evtOk;
  }

  public static PsmEvent handleSendPollAck(PsmEvent evt, PsmInterp interp) {
    // XXX: Implement.
    return V3Events.evtOk;
  }

  public static PsmEvent handleReceivedPollProof(PsmMsgEvent evt, PsmInterp interp) {
    // XXX: Implement.
    return V3Events.evtOk;
  }

  public static PsmEvent handleVerifyPollProof(PsmEvent evt, PsmInterp interp) {
    // XXX: Implement.
    return V3Events.evtOk;
  }

  public static PsmEvent handleGenerateVote(PsmEvent evt, PsmInterp interp) {
    // XXX: Implement.
    return V3Events.evtOk;
  }

  public static PsmEvent handleSendVote(PsmEvent evt, PsmInterp interp) {
    // XXX: Implement.
    return V3Events.evtOk;
  }

  public static PsmEvent handleReceivedRepairRequest(PsmMsgEvent evt, PsmInterp interp) {
    // XXX: Implement.
    return V3Events.evtRepairOk;
  }

  public static PsmEvent handleSendRepair(PsmEvent evt, PsmInterp interp) {
    // XXX: Implement.
    return V3Events.evtOk;
  }

  public static PsmEvent handleReceivedReceipt(PsmMsgEvent evt, PsmInterp interp) {
    // XXX: Implement.
    return V3Events.evtReceiptOk;
  }

  public static PsmEvent handleProcessReceipt(PsmEvent evt, PsmInterp interp) {
    // XXX: Implement.
    return V3Events.evtOk;
  }

  public static PsmEvent handleError(PsmEvent evt, PsmInterp interp) {
    // XXX: Implement.
    return V3Events.evtOk;
  }
}
