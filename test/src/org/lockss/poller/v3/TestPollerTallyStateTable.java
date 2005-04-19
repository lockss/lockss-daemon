/*
 * $Id: TestPollerTallyStateTable.java,v 1.1 2005-04-19 03:08:33 smorabito Exp $
 */

/*

Copyright (c) 2000-2005 Board of Trustees of Leland Stanford Jr. University,
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

import java.io.*;
import java.util.*;
import org.lockss.app.*;
import org.lockss.test.*;
import org.lockss.util.*;
import org.lockss.protocol.*;
import org.lockss.protocol.psm.*;

public class TestPollerTallyStateTable extends LockssTestCase {

  private IdentityManager theIdMgr;
  private LockssDaemon theDaemon;
  private PeerIdentity id;

  private PsmMsgEvent msgRepair;
  private PsmMsgEvent msgNoOp;

  public void setUp() throws Exception {
    super.setUp();

    // Set properties.
    Properties p = new Properties();
    p.setProperty(IdentityManager.PARAM_LOCAL_IP, "127.0.0.1");
    ConfigurationUtil.setCurrentConfigFromProps(p);

    this.theDaemon = getMockLockssDaemon();
    this.theIdMgr = theDaemon.getIdentityManager();

    this.id = theIdMgr.stringToPeerIdentity("127.0.0.1");

    this.msgRepair =
      new PollerTallyStateTable.Repair(new V3LcapMessage(V3LcapMessage.MSG_REPAIR_REP, this.id,
							 "http://www.test.com/",
							 123456789, 987654321,
							 ByteArray.makeRandomBytes(20)));

    this.msgNoOp = new PsmMsgEvent(V3LcapMessage.makeNoOpMsg(this.id));

  }

  public void testMachineOrderRepairRequired() throws Exception {
    MyMockPollerTallyStateTable table =
      new MyMockPollerTallyStateTable();

    // Simply run the machine for now.
    PsmInterp interp = table.getInterp();

    interp.init();

    // Verify that the poller is waiting in "WaitingRepair"
    assertEquals("WaitingRepair", interp.getCurrentState().getName());
    assertTrue(interp.isWaiting());
    assertFalse(interp.isFinalState());

    // Send a Repair
    interp.handleEvent(msgRepair);

    // Verify the final state has been reached
    assertTrue(interp.isFinalState());
    assertIsomorphic(ListUtil.list("initialize", "verifyVoteEffort",
				   "tally", "proveRepairEffort",
				   "sendRepairRequest",
				   "receivedRepair",
				   "sendReceipt"),
		     table.eventList);
  }

  public void testMachineOrderNoRepair() throws Exception {
    MyMockPollerTallyStateTable table =
      new MyMockPollerTallyStateTable() {
	protected PsmEvent handleTally(PsmEvent evt, PsmInterp interp) {
	  eventList.add("tally");
	  return evtRepairNotNeeded;
	}
      };

    // Simply run the machine for now.
    PsmInterp interp = table.getInterp();

    interp.init();

    // No repair should be required, so verify interpreter is in its
    // final state.
    assertTrue(interp.isFinalState());
    assertIsomorphic(ListUtil.list("initialize", "verifyVoteEffort",
				   "tally", "sendReceipt"),
		     table.eventList);
  }


  public void testMachineOrderErrors() throws Exception {
    PsmInterp interp = null;
    String[] expectedResults = null;
    MyMockPollerTallyStateTable table = null;

    // Error in Initializing
    table = new MyMockPollerTallyStateTable() {
	protected PsmEvent handleInitialize(PsmEvent evt, PsmInterp interp) {
	  return evtError;
	}
      };
    interp = table.getInterp();
    interp.init();
    assertTrue(interp.isFinalState());
    expectedResults = new String[] {"error"};
    assertIsomorphic(ListUtil.fromArray(expectedResults), table.eventList);

    // Error in VerifyingVoteEffort
    table = new MyMockPollerTallyStateTable() {
	protected PsmEvent handleVerifyVoteEffort(PsmEvent evt, PsmInterp interp) {
	  return evtError;
	}
      };
    interp = table.getInterp();
    interp.init();
    assertTrue(interp.isFinalState());
    expectedResults = new String[] {"initialize", "error"};
    assertIsomorphic(ListUtil.fromArray(expectedResults), table.eventList);

    // Error in Tallying
    table = new MyMockPollerTallyStateTable() {
	protected PsmEvent handleTally(PsmEvent evt, PsmInterp interp) {
	  return evtError;
	}
      };
    interp = table.getInterp();
    interp.init();
    assertTrue(interp.isFinalState());
    expectedResults = new String[] {"initialize", "verifyVoteEffort", "error"};
    assertIsomorphic(ListUtil.fromArray(expectedResults), table.eventList);

    // Error in ProvingRepairEffort
    table = new MyMockPollerTallyStateTable() {
	protected PsmEvent handleProveRepairEffort(PsmEvent evt, PsmInterp interp) {
	  return evtError;
	}
      };
    interp = table.getInterp();
    interp.init();
    assertTrue(interp.isFinalState());
    expectedResults = new String[] {"initialize", "verifyVoteEffort",
				    "tally", "error"};
    assertIsomorphic(ListUtil.fromArray(expectedResults), table.eventList);


    // Error in SendRepairRequest
    table = new MyMockPollerTallyStateTable() {
	protected PsmEvent handleSendRepairRequest(PsmEvent evt, PsmInterp interp) {
	  return evtError;
	}
      };
    interp = table.getInterp();
    interp.init();
    assertTrue(interp.isFinalState());
    expectedResults = new String[] {"initialize", "verifyVoteEffort",
				    "tally", "proveRepairEffort", "error"};
    assertIsomorphic(ListUtil.fromArray(expectedResults), table.eventList);

    // Error in WaitingRepair
    table = new MyMockPollerTallyStateTable() {
	protected PsmEvent handleReceivedRepair(PsmMsgEvent evt, PsmInterp interp) {
	  return evtError;
	}
      };
    interp = table.getInterp();
    interp.init();
    // Send Repair message
    interp.handleEvent(msgRepair);
    // Should error out in WaitingRepair
    assertTrue(interp.isFinalState());
    expectedResults = new String[] {"initialize", "verifyVoteEffort",
				    "tally", "proveRepairEffort",
				    "sendRepairRequest", "error"};
    assertIsomorphic(ListUtil.fromArray(expectedResults), table.eventList);

    // Error in SendingReceipt
    table = new MyMockPollerTallyStateTable() {
	protected PsmEvent handleSendReceipt(PsmEvent evt, PsmInterp interp) {
	  return evtError;
	}
      };
    interp = table.getInterp();
    interp.init();
    // Send Repair message
    interp.handleEvent(msgRepair);
    // Should error out in SendingReceipt
    assertTrue(interp.isFinalState());
    expectedResults = new String[] {"initialize", "verifyVoteEffort",
				    "tally", "proveRepairEffort",
				    "sendRepairRequest", "receivedRepair",
				    "error"};
    assertIsomorphic(ListUtil.fromArray(expectedResults), table.eventList);

  }

  /**
   * Mock class that overrides event handlers.
   */
  private static class MyMockPollerTallyStateTable extends PollerTallyStateTable {

    public List eventList = new ArrayList();

    protected PsmEvent handleInitialize(PsmEvent evt, PsmInterp interp) {
      eventList.add("initialize");
      return evtOk;
    }

    protected PsmEvent handleVerifyVoteEffort(PsmEvent evt, PsmInterp interp) {
      eventList.add("verifyVoteEffort");
      return evtOk;
    }

    protected PsmEvent handleTally(PsmEvent evt, PsmInterp interp) {
      eventList.add("tally");
      return evtRepairNeeded;
    }

    protected PsmEvent handleProveRepairEffort(PsmEvent evt, PsmInterp interp) {
      eventList.add("proveRepairEffort");
      return evtOk;
    }

    protected PsmEvent handleSendRepairRequest(PsmEvent evt, PsmInterp interp) {
      eventList.add("sendRepairRequest");
      return evtOk;
    }

    protected PsmEvent handleReceivedRepair(PsmMsgEvent evt, PsmInterp interp) {
      eventList.add("receivedRepair");
      return evtRepairOk;
    }

    protected PsmEvent handleSendReceipt(PsmEvent evt, PsmInterp interp) {
      eventList.add("sendReceipt");
      return evtOk;
    }

    protected PsmEvent handleError(PsmEvent evt, PsmInterp interp) {
      eventList.add("error");
      return evtOk;
    }

  }
}
