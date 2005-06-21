/*
 * $Id: TestVoterStateMachineFactory.java,v 1.2 2005-06-21 02:54:05 tlipkis Exp $
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

/**
 * Tests to ensure that the state machine returned by
 * VoterStateMachineFactory behaves correctly and that the expected
 * order of states occurs during operation on its state table.
 */

public class TestVoterStateMachineFactory extends LockssTestCase {

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
      V3Events.fromMessage(new V3LcapMessage(V3LcapMessage.MSG_POLL_PROOF,
					     this.id,
					     "http://www.test.com/",
					     123456789, 987654321,
					     ByteArray.makeRandomBytes(20)));

    this.msgRepairRequest =
      V3Events.fromMessage(new V3LcapMessage(V3LcapMessage.MSG_REPAIR_REQ,
					     this.id,
					     "http://www.test.com/",
					     123456789, 987654321,
					     ByteArray.makeRandomBytes(20)));

    this.msgReceipt =
      V3Events.fromMessage(new V3LcapMessage(V3LcapMessage.MSG_EVALUATION_RECEIPT,
					     this.id,
					     "http://www.test.com/",
					     123456789, 987654321,
					     ByteArray.makeRandomBytes(20)));

    this.msgNoOp = V3Events.fromMessage(V3LcapMessage.makeNoOpMsg(this.id));

  }

  /**
   * Ensure that a repair request is honored.
   */
  public void testMachineOrderRepairRequired() throws Exception {

    PsmMachine mach = VoterStateMachineFactory.getMachine(TestActions.class);
    PsmInterp interp = new PsmInterp(mach, new ArrayList());

    interp.init();

    // Verify that the poller is waiting in "WaitPollAck"
    assertEquals("WaitPollProof", interp.getCurrentState().getName());
    assertTrue(interp.isWaiting());
    assertFalse(interp.isFinalState());

    // Cause the next event to be handled.
    interp.handleEvent(msgPollProof);

    // Verify that the interpreter is waiting in "WaitRepairRequestOrReceipt"
    assertEquals("WaitRepairRequestOrReceipt", interp.getCurrentState().getName());
    assertTrue(interp.isWaiting());
    assertFalse(interp.isFinalState());

    // Send a Repair Request message.
    interp.handleEvent(msgRepairRequest);

    // Verify that the interpreter is back in "WaitRepairRequestOrReceipt"
    assertEquals("WaitRepairRequestOrReceipt", interp.getCurrentState().getName());
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
    assertIsomorphic(ListUtil.fromArray(expectedResults),
		     (List)interp.getUserData());
  }

  /**
   * Ensure multiple repair requests are honored.
   */
  public void testMachineOrderMultipleRepairsRequired() throws Exception {

    PsmMachine mach = VoterStateMachineFactory.getMachine(TestActions.class);
    PsmInterp interp = new PsmInterp(mach, new ArrayList());

    interp.init();

    // Verify that the poller is waiting in "WaitPollAck"
    assertEquals("WaitPollProof", interp.getCurrentState().getName());
    assertTrue(interp.isWaiting());
    assertFalse(interp.isFinalState());

    // Send a Poll Proof message
    interp.handleEvent(msgPollProof);

    // Verify that the interpreter is waiting in "WaitRepairRequestOrReceipt"
    assertEquals("WaitRepairRequestOrReceipt", interp.getCurrentState().getName());
    assertTrue(interp.isWaiting());
    assertFalse(interp.isFinalState());

    // Loop through five times, each time sending a repair request
    // message.
    for (int i = 0; i < 5; i++) {
      // Send a Repair Request message.
      interp.handleEvent(msgRepairRequest);

      // Verify that the interpreter is back in "WaitRepairRequestOrReceipt"
      assertEquals("WaitRepairRequestOrReceipt", interp.getCurrentState().getName());
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
    assertIsomorphic(ListUtil.fromArray(expectedResults),
		     (List)interp.getUserData());
  }

  /**
   * Ensure that a receipt request is honored if no repair has
   * been requested.
   */
  public void testMachineOrderNoRepair() throws Exception {

    PsmMachine mach = VoterStateMachineFactory.getMachine(TestActions.class);
    PsmInterp interp = new PsmInterp(mach, new ArrayList());

    interp.init();

    // Verify that the poller is waiting in "WaitPollAck"
    assertEquals("WaitPollProof", interp.getCurrentState().getName());
    assertTrue(interp.isWaiting());
    assertFalse(interp.isFinalState());

    // Cause the next event to be handled.
    interp.handleEvent(msgPollProof);

    // Verify that the interpreter is waiting in "WaitRepairRequestOrReceipt"
    assertEquals("WaitRepairRequestOrReceipt", interp.getCurrentState().getName());
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
    assertIsomorphic(ListUtil.fromArray(expectedResults),
		     (List)interp.getUserData());
  }

  /**
   * Ensure that each state properly handles errors.
   */
  public void testMachineOrderErrors() {

    PsmMachine mach = null;
    PsmInterp interp = null;
    String[] expectedResults = null;

    // Error in Initialize
    mach = VoterStateMachineFactory.getMachine(Errors1.class);
    interp = new PsmInterp(mach, new ArrayList());
    interp.init();
    assertTrue(interp.isFinalState());
    expectedResults = new String[] {"error"};
    assertIsomorphic(ListUtil.fromArray(expectedResults),
		     (List)interp.getUserData());

    // Error in VerifyPollEffort
    mach = VoterStateMachineFactory.getMachine(Errors2.class);
    interp = new PsmInterp(mach, new ArrayList());
    interp.init();
    assertTrue(interp.isFinalState());
    expectedResults = new String[] {"initialize", "error"};
    assertIsomorphic(ListUtil.fromArray(expectedResults),
		     (List)interp.getUserData());

    // Error in ProvePollAck
    mach = VoterStateMachineFactory.getMachine(Errors3.class);
    interp = new PsmInterp(mach, new ArrayList());
    interp.init();
    assertTrue(interp.isFinalState());
    expectedResults = new String[] {"initialize", "verifyPollEffort", "error"};
    assertIsomorphic(ListUtil.fromArray(expectedResults),
		     (List)interp.getUserData());

    // Error in SendingPollAck
    mach = VoterStateMachineFactory.getMachine(Errors4.class);
    interp = new PsmInterp(mach, new ArrayList());
    interp.init();
    assertTrue(interp.isFinalState());
    expectedResults = new String[] {"initialize", "verifyPollEffort",
				    "provePollAck", "error"};
    assertIsomorphic(ListUtil.fromArray(expectedResults),
		     (List)interp.getUserData());

    // Error in WaitPollProof
    mach = VoterStateMachineFactory.getMachine(Errors5.class);
    interp = new PsmInterp(mach, new ArrayList());
    interp.init();
    assertFalse(interp.isFinalState());
    // Send poll proof message
    interp.handleEvent(msgPollProof);
    assertTrue(interp.isFinalState());
    expectedResults = new String[] {"initialize", "verifyPollEffort",
				    "provePollAck", "sendPollAck", "error"};
    assertIsomorphic(ListUtil.fromArray(expectedResults),
		     (List)interp.getUserData());

    // Error in VerifyPollProof
    mach = VoterStateMachineFactory.getMachine(Errors6.class);
    interp = new PsmInterp(mach, new ArrayList());
    interp.init();
    assertFalse(interp.isFinalState());
    // Send poll proof message
    interp.handleEvent(msgPollProof);
    assertTrue(interp.isFinalState());
    expectedResults = new String[] {"initialize", "verifyPollEffort",
				    "provePollAck", "sendPollAck", "receivedPollProof",
				    "error"};
    assertIsomorphic(ListUtil.fromArray(expectedResults),
		     (List)interp.getUserData());

    // Error in GenerateVote
    mach = VoterStateMachineFactory.getMachine(Errors7.class);
    interp = new PsmInterp(mach, new ArrayList());
    interp.init();
    assertFalse(interp.isFinalState());
    // Send poll proof message
    interp.handleEvent(msgPollProof);
    assertTrue(interp.isFinalState());
    expectedResults = new String[] {"initialize", "verifyPollEffort",
				    "provePollAck", "sendPollAck", "receivedPollProof",
				    "verifyPollProof", "error"};
    assertIsomorphic(ListUtil.fromArray(expectedResults),
		     (List)interp.getUserData());


    // Error in SendVote
    mach = VoterStateMachineFactory.getMachine(Errors8.class);
    interp = new PsmInterp(mach, new ArrayList());
    interp.init();
    assertFalse(interp.isFinalState());
    // Send poll proof message
    interp.handleEvent(msgPollProof);
    assertTrue(interp.isFinalState());
    expectedResults = new String[] {"initialize", "verifyPollEffort",
				    "provePollAck", "sendPollAck", "receivedPollProof",
				    "verifyPollProof", "generateVote", "error"};
    assertIsomorphic(ListUtil.fromArray(expectedResults),
		     (List)interp.getUserData());


    // Error in WaitRepairRequestOrReceipt
    mach = VoterStateMachineFactory.getMachine(Errors9.class);
    interp = new PsmInterp(mach, new ArrayList());
    interp.init();
    assertFalse(interp.isFinalState());
    // Send a poll proof message
    interp.handleEvent(msgPollProof);
    assertFalse(interp.isFinalState());
    assertTrue(interp.isWaiting());
    assertEquals("WaitRepairRequestOrReceipt", interp.getCurrentState().getName());
    // Send a message that has no handler in WaitRepairRequestOrReceipt
    interp.handleEvent(msgNoOp);
    assertTrue(interp.isFinalState());
    expectedResults = new String[] {"initialize", "verifyPollEffort",
				    "provePollAck", "sendPollAck", "receivedPollProof",
				    "verifyPollProof", "generateVote", "sendVote",
				    "error"};
    assertIsomorphic(ListUtil.fromArray(expectedResults),
		     (List)interp.getUserData());


    // Error in SendRepair
    mach = VoterStateMachineFactory.getMachine(Errors10.class);
    interp = new PsmInterp(mach, new ArrayList());
    interp.init();
    assertFalse(interp.isFinalState());
    // Send a poll proof message
    interp.handleEvent(msgPollProof);
    assertFalse(interp.isFinalState());
    assertTrue(interp.isWaiting());
    assertEquals("WaitRepairRequestOrReceipt", interp.getCurrentState().getName());
    // Send a RepairRequest
    interp.handleEvent(msgRepairRequest);
    // Should error out trying to send repair
    assertTrue(interp.isFinalState());
    expectedResults = new String[] {"initialize", "verifyPollEffort",
				    "provePollAck", "sendPollAck", "receivedPollProof",
				    "verifyPollProof", "generateVote", "sendVote",
				    "receivedRepairRequest", "error"};
    assertIsomorphic(ListUtil.fromArray(expectedResults),
		     (List)interp.getUserData());


    // Error in ProcessingReceipt
    mach = VoterStateMachineFactory.getMachine(Errors11.class);
    interp = new PsmInterp(mach, new ArrayList());
    interp.init();
    assertFalse(interp.isFinalState());
    // Send a poll proof message
    interp.handleEvent(msgPollProof);
    assertFalse(interp.isFinalState());
    assertTrue(interp.isWaiting());
    assertEquals("WaitRepairRequestOrReceipt", interp.getCurrentState().getName());
    // Send a Receipt
    interp.handleEvent(msgReceipt);
    // Should error out trying to process the receipt
    expectedResults = new String[] {"initialize", "verifyPollEffort",
				    "provePollAck", "sendPollAck", "receivedPollProof",
				    "verifyPollProof", "generateVote", "sendVote",
				    "error"};
    assertIsomorphic(ListUtil.fromArray(expectedResults),
		     (List)interp.getUserData());

  }


  /**
   * Actions class that extends and hides VoterActions
   * implementations for testing.
   */
  public static class TestActions extends VoterActions {
    public static PsmEvent handleInitialize(PsmEvent evt, PsmInterp interp) {
      ((List)interp.getUserData()).add("initialize");
      return V3Events.evtOk;
    }

    public static PsmEvent handleVerifyPollEffort(PsmEvent evt, PsmInterp interp) {
      ((List)interp.getUserData()).add("verifyPollEffort");
      return V3Events.evtOk;
    }

    public static PsmEvent handleProvePollAck(PsmEvent evt, PsmInterp interp) {
      ((List)interp.getUserData()).add("provePollAck");
      return V3Events.evtOk;
    }

    public static PsmEvent handleSendPollAck(PsmEvent evt, PsmInterp interp) {
      ((List)interp.getUserData()).add("sendPollAck");
      return V3Events.evtOk;
    }

    public static PsmEvent handleReceivedPollProof(PsmMsgEvent evt, PsmInterp interp) {
      ((List)interp.getUserData()).add("receivedPollProof");
      return V3Events.evtOk;
    }

    public static PsmEvent handleVerifyPollProof(PsmEvent evt, PsmInterp interp) {
      ((List)interp.getUserData()).add("verifyPollProof");
      return V3Events.evtOk;
    }

    public static PsmEvent handleGenerateVote(PsmEvent evt, PsmInterp interp) {
      ((List)interp.getUserData()).add("generateVote");
      return V3Events.evtOk;
    }

    public static PsmEvent handleSendVote(PsmEvent evt, PsmInterp interp) {
      ((List)interp.getUserData()).add("sendVote");
      return V3Events.evtOk;
    }

    public static PsmEvent handleReceivedRepairRequest(PsmMsgEvent evt, PsmInterp interp) {
      ((List)interp.getUserData()).add("receivedRepairRequest");
      return V3Events.evtRepairOk;
    }

    public static PsmEvent handleSendRepair(PsmEvent evt, PsmInterp interp) {
      ((List)interp.getUserData()).add("sendRepair");
      return V3Events.evtOk;
    }

    public static PsmEvent handleReceivedReceipt(PsmMsgEvent evt, PsmInterp interp) {
      ((List)interp.getUserData()).add("receivedReceipt");
      return V3Events.evtReceiptOk;
    }

    public static PsmEvent handleProcessReceipt(PsmEvent evt, PsmInterp interp) {
      ((List)interp.getUserData()).add("processReceipt");
      return V3Events.evtOk;
    }

    public static PsmEvent handleError(PsmEvent evt, PsmInterp interp) {
      ((List)interp.getUserData()).add("error");
      return V3Events.evtOk;
    }
  }

  /**
   * A series of Action classes that hide each of the handleXxx methods
   * and cause error events to be returned.
   */

  // Error in Initialize
  public static class Errors1 extends TestActions {
    public static PsmEvent handleInitialize(PsmEvent evt, PsmInterp interp) {
      return V3Events.evtError;
    }
  }

  // Error in VerifyPollEffort
  public static class Errors2 extends TestActions {
    public static PsmEvent handleVerifyPollEffort(PsmEvent evt, PsmInterp interp) {
      return V3Events.evtError;
    }
  }

  // Error in ProvePollAck
  public static class Errors3 extends TestActions {
    public static PsmEvent handleProvePollAck(PsmEvent evt, PsmInterp interp) {
      return V3Events.evtError;
    }
  }

  // Error in SendPollAck
  public static class Errors4 extends TestActions {
    public static PsmEvent handleSendPollAck(PsmEvent evt, PsmInterp interp) {
      return V3Events.evtError;
    }
  }

  // Error in WaitPollProof
  public static class Errors5 extends TestActions {
    public static PsmEvent handleReceivedPollProof(PsmMsgEvent evt, PsmInterp interp) {
      return V3Events.evtError;
    }
  }

  // Error in VerifyPollProof
  public static class Errors6 extends TestActions {
    public static PsmEvent handleVerifyPollProof(PsmEvent evt, PsmInterp interp) {
      return V3Events.evtError;
    }
  }

  // Error in GenerateVote
  public static class Errors7 extends TestActions {
    public static PsmEvent handleGenerateVote(PsmEvent evt, PsmInterp interp) {
      return V3Events.evtError;
    }
  }

  // Error in SendVote
  public static class Errors8 extends TestActions {
    public static PsmEvent handleSendVote(PsmEvent evt, PsmInterp interp) {
      return V3Events.evtError;
    }
  }

  // Error in WaitRepairRequestOrReceipt
  public static class Errors9 extends TestActions {
    public static PsmEvent handleReceivedRepairRequest(PsmMsgEvent evt, PsmInterp interp) {
      return V3Events.evtError;
    }
  }

  // Error in SendRepair
  public static class Errors10 extends TestActions {
    public static PsmEvent handleSendRepair(PsmEvent evt, PsmInterp interp) {
      return V3Events.evtError;
    }
  }

  // Error in ProcessReceipt
  public static class Errors11 extends TestActions {
    public static PsmEvent handleReceivedReceipt(PsmMsgEvent evt, PsmInterp interp) {
      return V3Events.evtError;
    }
  }
}
