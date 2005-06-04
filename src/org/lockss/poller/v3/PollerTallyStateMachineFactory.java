/*
 * $Id: PollerTallyStateMachineFactory.java,v 1.2 2005-06-04 21:37:12 tlipkis Exp $
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

import org.lockss.util.*;
import org.lockss.protocol.*;
import org.lockss.protocol.psm.*;

public class PollerTallyStateMachineFactory {

  /**
   * Obtain a PsmMachine for the PollerTally state table.
   *
   * @return A PsmMachine representing the PollerTally
   *         state table.
   * @param actionClass A class containing static handler methods
   *                    for the state machine to call.
   */
  public static PsmMachine getMachine(Class actionClass) {
    return new PsmMachine("PollerTally",
			  makeStates(actionClass),
			  "Initialize");
  }

  /**
   * Mapping of states to response handlers.
   */
  private static PsmState[] makeStates(Class actionClass) {
    /**
     * Mapping of states to response handlers.
     */
    PsmState[] states = {
      new PsmState("Initialize",
		   new PsmMethodAction(actionClass, "handleInitialize"),
		   new PsmResponse(V3Events.evtOk, "VerifyVoteEffort"),
		   new PsmResponse(V3Events.evtElse, "Error")),
      new PsmState("VerifyVoteEffort",
		   new PsmMethodAction(actionClass, "handleVerifyVoteEffort"),
		   new PsmResponse(V3Events.evtOk, "Tally"),
		   new PsmResponse(V3Events.evtElse, "Error")),
      new PsmState("Tally",
		   new PsmMethodAction(actionClass, "handleTally"),
		   new PsmResponse(V3Events.evtRepairNeeded, "ProveRepairEffort"),
		   new PsmResponse(V3Events.evtRepairNotNeeded, "SendReceipt"),
		   new PsmResponse(V3Events.evtElse, "Error")),
      new PsmState("ProveRepairEffort",
		   new PsmMethodAction(actionClass, "handleProveRepairEffort"),
		   new PsmResponse(V3Events.evtOk, "SendRepairRequest"),
		   new PsmResponse(V3Events.evtElse, "Error")),
      new PsmState("SendRepairRequest",
		   new PsmMethodAction(actionClass, "handleSendRepairRequest"),
		   new PsmResponse(V3Events.evtOk, "WaitRepair"),
		   new PsmResponse(V3Events.evtElse, "Error")),
      new PsmState("WaitRepair", PsmWait.FOREVER,
		   new PsmResponse(V3Events.msgRepair,
				   new PsmMethodMsgAction(actionClass,
							  "handleReceivedRepair")),
		   new PsmResponse(V3Events.evtRepairOk, "SendReceipt"),
		   new PsmResponse(V3Events.evtElse, "Error")),
      new PsmState("SendReceipt",
		   new PsmMethodAction(actionClass, "handleSendReceipt"),
		   new PsmResponse(V3Events.evtOk, "Finalize"),
		   new PsmResponse(V3Events.evtElse, "Error")),
      new PsmState("Error",
		   new PsmMethodAction(actionClass, "handleError"),
		   new PsmResponse(V3Events.evtOk, "Finalize")),
      new PsmState("Finalize")
    };
    return states;
  }

}
