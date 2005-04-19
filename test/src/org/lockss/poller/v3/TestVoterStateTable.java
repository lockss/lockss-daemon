/*
 * $Id: TestVoterStateTable.java,v 1.1 2005-04-19 03:08:33 smorabito Exp $
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

public class TestVoterStateTable extends LockssTestCase {

  private IdentityManager theIdMgr;
  private LockssDaemon theDaemon;
  private PeerIdentity id;

  private PsmMsgEvent msgPollProof;
  private PsmMsgEvent msgRepairRequest;
  private PsmMsgEvent msgReceipt;
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


    this.msgPollProof =
      new VoterStateTable.
      PollProof(new V3LcapMessage(V3LcapMessage.MSG_POLL_PROOF, this.id,
				  "http://www.test.com/",
				  123456789, 987654321,
				  ByteArray.makeRandomBytes(20)));

    this.msgRepairRequest =
      new VoterStateTable.
      RepairRequest(new V3LcapMessage(V3LcapMessage.MSG_REPAIR_REQ, this.id,
				      "http://www.test.com/",
				      123456789, 987654321,
				      ByteArray.makeRandomBytes(20)));

    this.msgReceipt =
      new VoterStateTable.
      Receipt(new V3LcapMessage(V3LcapMessage.MSG_EVALUATION_RECEIPT, this.id,
				"http://www.test.com/",
				123456789, 987654321,
				ByteArray.makeRandomBytes(20)));

    this.msgNoOp = new PsmMsgEvent(V3LcapMessage.makeNoOpMsg(this.id));

  }

  /**
   * Ensure that a repair request is honored.
   */
  public void testMachineOrderRepairRequired() throws Exception {
    MyMockVoterStateTable table =
      new MyMockVoterStateTable();

    // Simply run the machine for now.
    PsmInterp interp = table.getInterp();

    interp.init();

    // Verify that the poller is waiting in "WaitingPollAck"
    assertEquals("WaitingPollProof", interp.getCurrentState().getName());
    assertTrue(interp.isWaiting());
    assertFalse(interp.isFinalState());

    // Cause the next event to be handled.
    interp.handleEvent(msgPollProof);

    // Verify that the interpreter is waiting in "WaitingRepairRequestOrReceipt"
    assertEquals("WaitingRepairRequestOrReceipt", interp.getCurrentState().getName());
    assertTrue(interp.isWaiting());
    assertFalse(interp.isFinalState());

    // Send a Repair Request message.
    interp.handleEvent(msgRepairRequest);

    // Verify that the interpreter is back in "WaitingRepairRequestOrReceipt"
    assertEquals("WaitingRepairRequestOrReceipt", interp.getCurrentState().getName());
    assertTrue(interp.isWaiting());
    assertFalse(interp.isFinalState());

    // Send a Receipt message
    interp.handleEvent(msgReceipt);

    // Verify that the interpreterr is in its final state.
    assertTrue(interp.isFinalState());
    String[] expectedResults = {"initialize", "verifyPollEffort",
				"provePollAck", "sendPollAck",
				"receivedPollProof",
				"verifyPollProof",
				"generateVote", "sendVote",
				"receivedRepairRequest",
				"sendRepair",
				"receivedReceipt",
				"processReceipt"};
    assertIsomorphic(ListUtil.fromArray(expectedResults), table.eventList);
  }

  /**
   * Ensure multiple repair requests are honored.
   */
  public void testMachineOrderMultipleRepairsRequired() throws Exception {
    MyMockVoterStateTable table =
      new MyMockVoterStateTable();

    // Simply run the machine for now.
    PsmInterp interp = table.getInterp();

    interp.init();

    // Verify that the poller is waiting in "WaitingPollAck"
    assertEquals("WaitingPollProof", interp.getCurrentState().getName());
    assertTrue(interp.isWaiting());
    assertFalse(interp.isFinalState());

    // Send a Poll Proof message
    interp.handleEvent(msgPollProof);

    // Verify that the interpreter is waiting in "WaitingRepairRequestOrReceipt"
    assertEquals("WaitingRepairRequestOrReceipt", interp.getCurrentState().getName());
    assertTrue(interp.isWaiting());
    assertFalse(interp.isFinalState());

    // Loop through five times, each time sending a repair request
    // message.
    for (int i = 0; i < 5; i++) {
      // Send a Repair Request message.
      interp.handleEvent(msgRepairRequest);

      // Verify that the interpreter is back in "WaitingRepairRequestOrReceipt"
      assertEquals("WaitingRepairRequestOrReceipt", interp.getCurrentState().getName());
      assertTrue(interp.isWaiting());
      assertFalse(interp.isFinalState());
    }

    // Cause the next event to be handled.
    interp.handleEvent(msgReceipt);

    // Verify that the interpreter is in its final state
    assertTrue(interp.isFinalState());
    String[] expectedResults = {"initialize", "verifyPollEffort",
				"provePollAck", "sendPollAck",
				"receivedPollProof", "verifyPollProof",
				"generateVote", "sendVote",
				"receivedRepairRequest", "sendRepair",
				"receivedRepairRequest", "sendRepair",
				"receivedRepairRequest", "sendRepair",
				"receivedRepairRequest", "sendRepair",
				"receivedRepairRequest", "sendRepair",
				"receivedReceipt", "processReceipt"};
    assertIsomorphic(ListUtil.fromArray(expectedResults), table.eventList);
  }

  /**
   * Ensure that a receipt request is honored if no repair has
   * been requested.
   */
  public void testMachineOrderNoRepair() throws Exception {
    MyMockVoterStateTable table = new MyMockVoterStateTable();

    // Simply run the machine for now.
    PsmInterp interp = table.getInterp();

    interp.init();

    // Verify that the poller is waiting in "WaitingPollAck"
    assertEquals("WaitingPollProof", interp.getCurrentState().getName());
    assertTrue(interp.isWaiting());
    assertFalse(interp.isFinalState());

    // Cause the next event to be handled.
    interp.handleEvent(msgPollProof);

    // Verify that the interpreter is waiting in "WaitingRepairRequestOrReceipt"
    assertEquals("WaitingRepairRequestOrReceipt", interp.getCurrentState().getName());
    assertTrue(interp.isWaiting());
    assertFalse(interp.isFinalState());

    // Send a Receipt message (no repair request needed)
    interp.handleEvent(msgReceipt);

    // Verify that the interpreter is in its final state
    assertTrue(interp.isFinalState());
    String[] expectedResults = {"initialize", "verifyPollEffort",
				"provePollAck", "sendPollAck",
				"receivedPollProof",
				"verifyPollProof",
				"generateVote", "sendVote",
				"receivedReceipt",
				"processReceipt"};
    assertIsomorphic(ListUtil.fromArray(expectedResults), table.eventList);
  }

  /**
   * Ensure that each state properly handles errors.
   */
  public void testMachineOrderErrors() {
    PsmInterp interp = null;
    String[] expectedResults = null;
    MyMockVoterStateTable table = null;

    // Error in Initializing
    table = new MyMockVoterStateTable() {
	protected PsmEvent handleInitialize(PsmEvent evt, PsmInterp interp) {
	  return evtError;
	}
      };
    interp = table.getInterp();
    interp.init();
    assertTrue(interp.isFinalState());
    expectedResults = new String[] {"error"};
    assertIsomorphic(ListUtil.fromArray(expectedResults), table.eventList);

    // Error in VerifyingPollEffort
    table = new MyMockVoterStateTable() {
	protected PsmEvent handleVerifyPollEffort(PsmEvent evt, PsmInterp interp) {
	  return evtError;
	}
      };
    interp = table.getInterp();
    interp.init();
    assertTrue(interp.isFinalState());
    expectedResults = new String[] {"initialize", "error"};
    assertIsomorphic(ListUtil.fromArray(expectedResults), table.eventList);

    // Error in ProvingPollAck
    table = new MyMockVoterStateTable() {
	protected PsmEvent handleProvePollAck(PsmEvent evt, PsmInterp interp) {
	  return evtError;
	}
      };
    interp = table.getInterp();
    interp.init();
    assertTrue(interp.isFinalState());
    expectedResults = new String[] {"initialize", "verifyPollEffort", "error"};
    assertIsomorphic(ListUtil.fromArray(expectedResults), table.eventList);

    // Error in SendingPollAck
    table = new MyMockVoterStateTable() {
	protected PsmEvent handleSendPollAck(PsmEvent evt, PsmInterp interp) {
	  return evtError;
	}
      };
    interp = table.getInterp();
    interp.init();
    assertTrue(interp.isFinalState());
    expectedResults = new String[] {"initialize", "verifyPollEffort",
				    "provePollAck", "error"};
    assertIsomorphic(ListUtil.fromArray(expectedResults), table.eventList);

    // Error in WaitingPollProof
    table = new MyMockVoterStateTable() {
	protected PsmEvent handleReceivedPollProof(PsmMsgEvent evt, PsmInterp interp) {
	  return evtError;
	}
      };
    interp = table.getInterp();
    interp.init();
    assertFalse(interp.isFinalState());
    // Send poll proof message
    interp.handleEvent(msgPollProof);
    assertTrue(interp.isFinalState());
    expectedResults = new String[] {"initialize", "verifyPollEffort",
				    "provePollAck", "sendPollAck", "error"};
    assertIsomorphic(ListUtil.fromArray(expectedResults), table.eventList);

    // Error in VerifyingPollProof
    table = new MyMockVoterStateTable() {
	protected PsmEvent handleVerifyPollProof(PsmEvent evt, PsmInterp interp) {
	  return evtError;
	}
      };
    interp = table.getInterp();
    interp.init();
    assertFalse(interp.isFinalState());
    // Send poll proof message
    interp.handleEvent(msgPollProof);
    assertTrue(interp.isFinalState());
    expectedResults = new String[] {"initialize", "verifyPollEffort",
				    "provePollAck", "sendPollAck", "receivedPollProof",
				    "error"};
    assertIsomorphic(ListUtil.fromArray(expectedResults), table.eventList);

    // Error in GeneratingVote
    table = new MyMockVoterStateTable() {
	protected PsmEvent handleGenerateVote(PsmEvent evt, PsmInterp interp) {
	  return evtError;
	}
      };
    interp = table.getInterp();
    interp.init();
    assertFalse(interp.isFinalState());
    // Send poll proof message
    interp.handleEvent(msgPollProof);
    assertTrue(interp.isFinalState());
    expectedResults = new String[] {"initialize", "verifyPollEffort",
				    "provePollAck", "sendPollAck", "receivedPollProof",
				    "verifyPollProof", "error"};
    assertIsomorphic(ListUtil.fromArray(expectedResults), table.eventList);


    // Error in SendingVote
    table = new MyMockVoterStateTable() {
	protected PsmEvent handleSendVote(PsmEvent evt, PsmInterp interp) {
	  return evtError;
	}
      };
    interp = table.getInterp();
    interp.init();
    assertFalse(interp.isFinalState());
    // Send poll proof message
    interp.handleEvent(msgPollProof);
    assertTrue(interp.isFinalState());
    expectedResults = new String[] {"initialize", "verifyPollEffort",
				    "provePollAck", "sendPollAck", "receivedPollProof",
				    "verifyPollProof", "generateVote", "error"};
    assertIsomorphic(ListUtil.fromArray(expectedResults), table.eventList);


    // Error in WaitingRepairRequestOrReceipt
    table = new MyMockVoterStateTable() {
	protected PsmEvent handleReceivedRepairRequest(PsmMsgEvent evt, PsmInterp interp) {
	  return evtError;
	}
      };
    interp = table.getInterp();
    interp.init();
    assertFalse(interp.isFinalState());
    // Send a poll proof message
    interp.handleEvent(msgPollProof);
    assertFalse(interp.isFinalState());
    assertTrue(interp.isWaiting());
    assertEquals("WaitingRepairRequestOrReceipt", interp.getCurrentState().getName());
    // Send a message that has no handler in WaitingRepairRequestOrReceipt
    interp.handleEvent(msgNoOp);
    assertTrue(interp.isFinalState());
    expectedResults = new String[] {"initialize", "verifyPollEffort",
				    "provePollAck", "sendPollAck", "receivedPollProof",
				    "verifyPollProof", "generateVote", "sendVote",
				    "error"};
    assertIsomorphic(ListUtil.fromArray(expectedResults), table.eventList);


    // Error in SendingRepair
    table = new MyMockVoterStateTable() {
	protected PsmEvent handleSendRepair(PsmEvent evt, PsmInterp interp) {
	  return evtError;
	}
      };
    interp = table.getInterp();
    interp.init();
    assertFalse(interp.isFinalState());
    // Send a poll proof message
    interp.handleEvent(msgPollProof);
    assertFalse(interp.isFinalState());
    assertTrue(interp.isWaiting());
    assertEquals("WaitingRepairRequestOrReceipt", interp.getCurrentState().getName());
    // Send a RepairRequest
    interp.handleEvent(msgRepairRequest);
    // Should error out trying to send repair
    assertTrue(interp.isFinalState());
    expectedResults = new String[] {"initialize", "verifyPollEffort",
				    "provePollAck", "sendPollAck", "receivedPollProof",
				    "verifyPollProof", "generateVote", "sendVote",
				    "receivedRepairRequest", "error"};
    assertIsomorphic(ListUtil.fromArray(expectedResults), table.eventList);


    // Error in ProcessingReceipt
    table = new MyMockVoterStateTable() {
	protected PsmEvent handleReceivedReceipt(PsmMsgEvent evt, PsmInterp interp) {
	  return evtError;
	}
      };
    interp = table.getInterp();
    interp.init();
    assertFalse(interp.isFinalState());
    // Send a poll proof message
    interp.handleEvent(msgPollProof);
    assertFalse(interp.isFinalState());
    assertTrue(interp.isWaiting());
    assertEquals("WaitingRepairRequestOrReceipt", interp.getCurrentState().getName());
    // Send a Receipt
    interp.handleEvent(msgReceipt);
    // Should error out trying to process the receipt
    expectedResults = new String[] {"initialize", "verifyPollEffort",
				    "provePollAck", "sendPollAck", "receivedPollProof",
				    "verifyPollProof", "generateVote", "sendVote",
				    "error"};
    assertIsomorphic(ListUtil.fromArray(expectedResults), table.eventList);


  }

  /**
   * Mock class that overrides event handlers and logs entry actions
   * as they are called.
   */
  private static class MyMockVoterStateTable
    extends VoterStateTable {

    public List eventList = new ArrayList();

    protected MyMockVoterStateTable() {};

    protected PsmEvent handleInitialize(PsmEvent evt, PsmInterp interp) {
      eventList.add("initialize");
      return evtOk;
    }

    protected PsmEvent handleVerifyPollEffort(PsmEvent evt, PsmInterp interp) {
      eventList.add("verifyPollEffort");
      return evtOk;
    }

    protected PsmEvent handleProvePollAck(PsmEvent evt, PsmInterp interp) {
      eventList.add("provePollAck");
      return evtOk;
    }

    protected PsmEvent handleSendPollAck(PsmEvent evt, PsmInterp interp) {
      eventList.add("sendPollAck");
      return evtOk;
    }

    protected PsmEvent handleReceivedPollProof(PsmMsgEvent evt, PsmInterp interp) {
      eventList.add("receivedPollProof");
      return evtOk;
    }

    protected PsmEvent handleVerifyPollProof(PsmEvent evt, PsmInterp interp) {
      eventList.add("verifyPollProof");
      return evtOk;
    }

    protected PsmEvent handleGenerateVote(PsmEvent evt, PsmInterp interp) {
      eventList.add("generateVote");
      return evtOk;
    }

    protected PsmEvent handleSendVote(PsmEvent evt, PsmInterp interp) {
      eventList.add("sendVote");
      return evtOk;
    }

    protected PsmEvent handleReceivedRepairRequest(PsmMsgEvent evt, PsmInterp interp) {
      eventList.add("receivedRepairRequest");
      return evtRepairOk;
    }

    protected PsmEvent handleSendRepair(PsmEvent evt, PsmInterp interp) {
      eventList.add("sendRepair");
      return evtOk;
    }

    protected PsmEvent handleReceivedReceipt(PsmMsgEvent evt, PsmInterp interp) {
      eventList.add("receivedReceipt");
      return evtReceiptOk;
    }

    protected PsmEvent handleProcessReceipt(PsmEvent evt, PsmInterp interp) {
      eventList.add("processReceipt");
      return evtOk;
    }

    protected PsmEvent handleError(PsmEvent evt, PsmInterp interp) {
      eventList.add("error");
      return evtOk;
    }
  }
}
