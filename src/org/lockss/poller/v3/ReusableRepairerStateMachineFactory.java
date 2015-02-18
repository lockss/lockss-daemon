/*
 * $Id$
 */

/*

Copyright (c) 2012 Board of Trustees of Leland Stanford Jr. University,
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

public class ReusableRepairerStateMachineFactory implements PsmMachine.Factory {

  /**
   * Obtain a PsmMachine for the ReusableRepairer state table.
   *
   * @return A PsmMachine representing the ReusableRepairer state table.
   * @param actionClass A class containing static handler methods
   *                    for the state machine to call.
   */
  public PsmMachine getMachine(Class actionClass) {
    return new PsmMachine("ReusableRepairer",
                          makeStates(actionClass),
                          "WaitForRequest");
  }

  /**
   * Mapping of states to response handlers.
   */
  private static PsmState[] makeStates(Class actionClass) {
    PsmState[] states = {
      new PsmState("WaitForRequest", PsmWait.FOREVER,
                   new PsmResponse(V3Events.msgRepairRequest,
                                   new PsmMethodMsgAction(actionClass,
							  "handleReceiveRepairRequest")),
                   new PsmResponse(V3Events.evtNoSuchRepair, "WaitForRequest"),

                   new PsmResponse(V3Events.evtRepairRequestOk, "SendRepair")),
      new PsmState("SendRepair",
                   new PsmMethodAction(actionClass, "handleSendRepair"),
                   new PsmResponse(V3Events.evtOk, "WaitForRequest")),
    };
    return states;
  }
}
