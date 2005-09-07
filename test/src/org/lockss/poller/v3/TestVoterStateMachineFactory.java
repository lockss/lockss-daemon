/*
 * $Id: TestVoterStateMachineFactory.java,v 1.4 2005-09-07 03:06:29 smorabito Exp $
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
  private PsmMsgEvent msgVoteRequest;
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
					     ByteArray.makeRandomBytes(20),
                                             ByteArray.makeRandomBytes(20)));
    this.msgVoteRequest =
      V3Events.fromMessage(new V3LcapMessage(V3LcapMessage.MSG_VOTE_REQ,
					     this.id,
					     "http://www.test.com/",
					     123456789, 987654321,
					     ByteArray.makeRandomBytes(20),
                                             ByteArray.makeRandomBytes(20)));
    this.msgRepairRequest =
      V3Events.fromMessage(new V3LcapMessage(V3LcapMessage.MSG_REPAIR_REQ,
					     this.id,
					     "http://www.test.com/",
					     123456789, 987654321,
					     ByteArray.makeRandomBytes(20),
                                             ByteArray.makeRandomBytes(20)));
    this.msgReceipt =
      V3Events.fromMessage(new V3LcapMessage(V3LcapMessage.MSG_EVALUATION_RECEIPT,
					     this.id,
					     "http://www.test.com/",
					     123456789, 987654321,
					     ByteArray.makeRandomBytes(20),
                                             ByteArray.makeRandomBytes(20)));

    this.msgNoOp = V3Events.fromMessage(V3LcapMessage.makeNoOpMsg(this.id));

  }

  /**
   * Ensure that a repair request is honored.
   */
  public void testRepairMachineOrder() throws Exception {

    PsmMachine mach = VoterStateMachineFactory.getMachine(TestActions.class);
    PsmInterp interp = new PsmInterp(mach, new ArrayList());

    interp.init();

    // Verify that the poller is waiting in "WaitPollAck"
    assertWaiting(interp, "WaitPollProof");

    // Cause the next event to be handled.
    interp.handleEvent(msgPollProof);

    // Verify that the interpreter is waiting in "WaitVoteRequest"
    assertWaiting(interp, "WaitVoteRequest");

    // Send a Vote Request
    interp.handleEvent(msgVoteRequest);

    // Verify that the interpreter is waiting in "WaitReceipt"
    assertWaiting(interp, "WaitReceipt");

    // Send a Repair Request message.
    interp.handleEvent(msgRepairRequest);

    // Verify that the interpreter is back in "WaitReceipt"
    assertWaiting(interp, "WaitReceipt");

    // Send a Receipt message
    interp.handleEvent(msgReceipt);

    // Verify that the interpreterr is in its final state.
    assertTrue(interp.isFinalState());
    String[] expectedResults = {"verifyPollEffort",
				"provePollAck", "sendPollAck",
				"receivePollProof", "verifyPollProof",
				"sendNominate", "generateVote",
				"receiveVoteRequest", "sendVote",
				"receiveRepairRequest", "sendRepair",
				"receiveReceipt", "processReceipt"};
    assertIsomorphic(ListUtil.fromArray(expectedResults),
		     (List)interp.getUserData());
  }

  /**
   * Ensure multiple repair requests are honored.
   */
  public void testMultipleRepairMachineOrder() throws Exception {

    PsmMachine mach = VoterStateMachineFactory.getMachine(TestActions.class);
    PsmInterp interp = new PsmInterp(mach, new ArrayList());

    interp.init();

    // Verify that the poller is waiting in "WaitPollAck"
    assertWaiting(interp, "WaitPollProof");

    // Send a Poll Proof message
    interp.handleEvent(msgPollProof);

    // Verify that the interpreter is waiting in "WaitVoteRequest"
    assertWaiting(interp, "WaitVoteRequest");

    // Send a Vote Request
    interp.handleEvent(msgVoteRequest);

    // Verify that the interpreter is waiting in "WaitReceipt"
    assertWaiting(interp, "WaitReceipt");

    // Loop through five times, each time sending a repair request
    // message.
    for (int i = 0; i < 5; i++) {
      // Send a Repair Request message.
      interp.handleEvent(msgRepairRequest);

      // Verify that the interpreter is back in "WaitReceipt"
      assertWaiting(interp, "WaitReceipt");
    }

    // Cause the next event to be handled.
    interp.handleEvent(msgReceipt);

    // Verify that the interpreter is in its final state
    assertTrue(interp.isFinalState());
    String[] expectedResults = {"verifyPollEffort",
				"provePollAck", "sendPollAck",
				"receivePollProof", "verifyPollProof",
				"sendNominate", "generateVote",
				"receiveVoteRequest", "sendVote",
				"receiveRepairRequest", "sendRepair",
				"receiveRepairRequest", "sendRepair",
				"receiveRepairRequest", "sendRepair",
				"receiveRepairRequest", "sendRepair",
				"receiveRepairRequest", "sendRepair",
				"receiveReceipt", "processReceipt"};
    assertIsomorphic(ListUtil.fromArray(expectedResults),
		     (List)interp.getUserData());
  }

  /**
   * Ensure that a receipt request is honored if no repair has
   * been requested.
   */
  public void testNoRepairMachineOrder() throws Exception {

    PsmMachine mach = VoterStateMachineFactory.getMachine(TestActions.class);
    PsmInterp interp = new PsmInterp(mach, new ArrayList());

    interp.init();

    // Verify that the poller is waiting in "WaitPollAck"
    assertWaiting(interp, "WaitPollProof");

    // Cause the next event to be handled.
    interp.handleEvent(msgPollProof);

    // Verify that the interpreter is waiting in "WaitVoteRequest"
    assertWaiting(interp, "WaitVoteRequest");

    // Send a Vote Request
    interp.handleEvent(msgVoteRequest);

    // Verify that the interpreter is waiting in "WaitReceipt"
    assertWaiting(interp, "WaitReceipt");

    // Send a Receipt message
    interp.handleEvent(msgReceipt);

    // Verify that the interpreterr is in its final state.
    assertTrue(interp.isFinalState());
    String[] expectedResults = {"verifyPollEffort",
				"provePollAck", "sendPollAck",
				"receivePollProof", "verifyPollProof",
				"sendNominate", "generateVote",
				"receiveVoteRequest", "sendVote",
				"receiveReceipt", "processReceipt"};
    assertIsomorphic(ListUtil.fromArray(expectedResults),
		     (List)interp.getUserData());
  }

  /**
   * Ensure that each state properly handles errors.
   */

  /**  Error in VerifyPollEffort */
  public void testVerifyPollEffortError() throws Exception {
    PsmMachine mach = VoterStateMachineFactory.getMachine(Errors2.class);
    PsmInterp interp = new PsmInterp(mach, new ArrayList());
    interp.init();
    assertTrue(interp.isFinalState());
    String[] expectedResults = new String[] {"error"};
    assertIsomorphic(ListUtil.fromArray(expectedResults),
		     (List)interp.getUserData());
  }

  /**  Error in ProvePollAck */
  public void testProvePollAckError() throws Exception {
    PsmMachine mach = VoterStateMachineFactory.getMachine(Errors3.class);
    PsmInterp interp = new PsmInterp(mach, new ArrayList());
    interp.init();
    assertTrue(interp.isFinalState());
    String[] expectedResults = new String[] {"verifyPollEffort", "error"};
    assertIsomorphic(ListUtil.fromArray(expectedResults),
		     (List)interp.getUserData());
  }

  /**  Error in SendingPollAck */
  public void testSendPollAckError() throws Exception {
    PsmMachine mach = VoterStateMachineFactory.getMachine(Errors4.class);
    PsmInterp interp = new PsmInterp(mach, new ArrayList());
    interp.init();
    assertTrue(interp.isFinalState());
    String[] expectedResults = new String[] {"verifyPollEffort",
					     "provePollAck", "error"};
    assertIsomorphic(ListUtil.fromArray(expectedResults),
		     (List)interp.getUserData());
  }

  /**  Error in WaitPollProof */
  public void testWaitPollProofError() throws Exception {
    PsmMachine mach = VoterStateMachineFactory.getMachine(Errors5.class);
    PsmInterp interp = new PsmInterp(mach, new ArrayList());
    interp.init();
    assertFalse(interp.isFinalState());
    // Send poll proof message
    interp.handleEvent(msgPollProof);
    assertTrue(interp.isFinalState());
    String[] expectedResults = new String[] {"verifyPollEffort",
					     "provePollAck", "sendPollAck", "error"};
    assertIsomorphic(ListUtil.fromArray(expectedResults),
		     (List)interp.getUserData());

  }

  /**  Error in VerifyPollProof */
  public void testVerifyPollProofError() throws Exception {
    PsmMachine mach = VoterStateMachineFactory.getMachine(Errors6.class);
    PsmInterp interp = new PsmInterp(mach, new ArrayList());
    interp.init();
    assertFalse(interp.isFinalState());
    // Send poll proof message
    interp.handleEvent(msgPollProof);
    assertTrue(interp.isFinalState());
    String[] expectedResults = new String[] {"verifyPollEffort",
					     "provePollAck", "sendPollAck", "receivePollProof",
					     "error"};
    assertIsomorphic(ListUtil.fromArray(expectedResults),
		     (List)interp.getUserData());

  }

  /**  Error in SendNominate */
  public void testSendNominateError() throws Exception {
    PsmMachine mach = VoterStateMachineFactory.getMachine(Errors7.class);
    PsmInterp interp = new PsmInterp(mach, new ArrayList());
    interp.init();
    assertFalse(interp.isFinalState());
    // Send poll proof message
    interp.handleEvent(msgPollProof);
    assertTrue(interp.isFinalState());
    String[] expectedResults = new String[] {"verifyPollEffort",
					     "provePollAck", "sendPollAck", "receivePollProof",
					     "verifyPollProof", "error"};
    assertIsomorphic(ListUtil.fromArray(expectedResults),
		     (List)interp.getUserData());
  }

  /**  Error in GenerateVote */
  public void testGenerateVoteError() throws Exception {
    PsmMachine mach = VoterStateMachineFactory.getMachine(Errors8.class);
    PsmInterp interp = new PsmInterp(mach, new ArrayList());
    interp.init();
    assertFalse(interp.isFinalState());
    // Send poll proof message
    interp.handleEvent(msgPollProof);
    assertTrue(interp.isFinalState());
    String[] expectedResults = new String[] {"verifyPollEffort",
					     "provePollAck", "sendPollAck", "receivePollProof",
					     "verifyPollProof", "sendNominate", "error"};
    assertIsomorphic(ListUtil.fromArray(expectedResults),
		     (List)interp.getUserData());
  }

  /** Error in ReceiveVoteRequest */
  public void testErrorReceiveVoteRequest() throws Exception {
    PsmMachine mach = VoterStateMachineFactory.getMachine(Errors9.class);
    PsmInterp interp = new PsmInterp(mach, new ArrayList());
    interp.init();
    assertFalse(interp.isFinalState());
    // Send poll proof message
    interp.handleEvent(msgPollProof);
    assertWaiting(interp, "WaitVoteRequest");
    interp.handleEvent(msgVoteRequest);
    assertTrue(interp.isFinalState());
    String[] expectedResults = new String[] {"verifyPollEffort",
					     "provePollAck", "sendPollAck", "receivePollProof",
					     "verifyPollProof", "sendNominate", 
					     "generateVote", "error"};
    assertIsomorphic(ListUtil.fromArray(expectedResults),
		     (List)interp.getUserData());    
  }

  /**  Error in SendVote */
  public void testSendVoteError() throws Exception {
    PsmMachine mach = VoterStateMachineFactory.getMachine(Errors10.class);
    PsmInterp interp = new PsmInterp(mach, new ArrayList());
    interp.init();
    assertFalse(interp.isFinalState());
    // Send poll proof message
    interp.handleEvent(msgPollProof);
    assertWaiting(interp, "WaitVoteRequest");
    interp.handleEvent(msgVoteRequest);
    assertTrue(interp.isFinalState());
    String[] expectedResults = new String[] {"verifyPollEffort",
					     "provePollAck", "sendPollAck", "receivePollProof",
					     "verifyPollProof", "sendNominate",
					     "generateVote", "receiveVoteRequest", "error"};
    assertIsomorphic(ListUtil.fromArray(expectedResults),
		     (List)interp.getUserData());
  }

  /**  Error in WaitReceipt - Receiving a Repair Request */
  public void testWaitReceiptError1() throws Exception {
    PsmMachine mach = VoterStateMachineFactory.getMachine(Errors11.class);
    PsmInterp interp = new PsmInterp(mach, new ArrayList());
    interp.init();
    assertFalse(interp.isFinalState());
    // Send a poll proof message
    interp.handleEvent(msgPollProof);
    assertWaiting(interp, "WaitVoteRequest");
    interp.handleEvent(msgVoteRequest);
    assertWaiting(interp, "WaitReceipt");
    interp.handleEvent(msgRepairRequest);
    assertTrue(interp.isFinalState());
    String[] expectedResults = new String[] {"verifyPollEffort",
					     "provePollAck", "sendPollAck", "receivePollProof",
					     "verifyPollProof", "sendNominate",
					     "generateVote", "receiveVoteRequest",
					     "sendVote", "error"};
    assertIsomorphic(ListUtil.fromArray(expectedResults),
		     (List)interp.getUserData());
  }

  /**  Error in SendRepair */
  public void testSendRepairError() throws Exception {
    PsmMachine mach = VoterStateMachineFactory.getMachine(Errors12.class);
    PsmInterp interp = new PsmInterp(mach, new ArrayList());
    interp.init();
    // Send a poll proof message
    interp.handleEvent(msgPollProof);
    assertWaiting(interp, "WaitVoteRequest");
    interp.handleEvent(msgVoteRequest);
    assertWaiting(interp, "WaitReceipt");
    interp.handleEvent(msgRepairRequest);
    // Should error out trying to send repair
    assertTrue(interp.isFinalState());
    String[] expectedResults = new String[] {"verifyPollEffort",
					     "provePollAck", "sendPollAck", "receivePollProof",
					     "verifyPollProof", "sendNominate",
					     "generateVote", "receiveVoteRequest",
					     "sendVote", "receiveRepairRequest",
					     "error"};
    assertIsomorphic(ListUtil.fromArray(expectedResults),
		     (List)interp.getUserData());
  }

  /** Error in WaitReceipt - Receiving a Receipt */
   public void testWaitReceiptError2() throws Exception {
    PsmMachine mach = VoterStateMachineFactory.getMachine(Errors13.class);
    PsmInterp interp = new PsmInterp(mach, new ArrayList());
    interp.init();
    assertFalse(interp.isFinalState());
    // Send a poll proof message
    interp.handleEvent(msgPollProof);
    assertWaiting(interp, "WaitVoteRequest");
    interp.handleEvent(msgVoteRequest);
    assertWaiting(interp, "WaitReceipt");
    interp.handleEvent(msgRepairRequest);
    assertWaiting(interp, "WaitReceipt");
    interp.handleEvent(msgReceipt);
    assertTrue(interp.isFinalState());
    String[] expectedResults = new String[] {"verifyPollEffort",
					     "provePollAck", "sendPollAck", "receivePollProof",
					     "verifyPollProof", "sendNominate",
					     "generateVote", "receiveVoteRequest",
					     "sendVote", "receiveRepairRequest",
					     "sendRepair", "error"};
    assertIsomorphic(ListUtil.fromArray(expectedResults),
		     (List)interp.getUserData());
  } 

  /**  Error in ProcessingReceipt */
  public void testProcessReceiptError() throws Exception {
    PsmMachine mach = VoterStateMachineFactory.getMachine(Errors14.class);
    PsmInterp interp = new PsmInterp(mach, new ArrayList());
    interp.init();
    assertFalse(interp.isFinalState());
    // Send a poll proof message
    interp.handleEvent(msgPollProof);
    assertWaiting(interp, "WaitVoteRequest");
    interp.handleEvent(msgVoteRequest);
    assertWaiting(interp, "WaitReceipt");
    interp.handleEvent(msgRepairRequest);
    assertWaiting(interp, "WaitReceipt");
    interp.handleEvent(msgReceipt);
    assertTrue(interp.isFinalState());
    // Should error out trying to process the receipt
    String[] expectedResults = new String[] {"verifyPollEffort",
					     "provePollAck", "sendPollAck", "receivePollProof",
					     "verifyPollProof", "sendNominate",
					     "generateVote", "receiveVoteRequest",
					     "sendVote", "receiveRepairRequest",
					     "sendRepair", "receiveReceipt",
					     "error"};
    assertIsomorphic(ListUtil.fromArray(expectedResults),
		     (List)interp.getUserData());
  }


  /**
   * Actions class that extends and hides VoterActions
   * implementations for testing.
   */
  public static class TestActions extends VoterActions {
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

    public static PsmEvent handleReceivePollProof(PsmMsgEvent evt, PsmInterp interp) {
      ((List)interp.getUserData()).add("receivePollProof");
      return V3Events.evtOk;
    }

    public static PsmEvent handleVerifyPollProof(PsmEvent evt, PsmInterp interp) {
      ((List)interp.getUserData()).add("verifyPollProof");
      return V3Events.evtOk;
    }

    public static PsmEvent handleSendNominate(PsmEvent evt, PsmInterp interp) {
      ((List)interp.getUserData()).add("sendNominate");
      return V3Events.evtOk;
    }

    public static PsmEvent handleGenerateVote(PsmEvent evt, PsmInterp interp) {
      ((List)interp.getUserData()).add("generateVote");
      return V3Events.evtOk;
    }

    public static PsmEvent handleReceiveVoteRequest(PsmMsgEvent evt, PsmInterp interp) {
      ((List)interp.getUserData()).add("receiveVoteRequest");
      return V3Events.evtReadyToVote;
    }

    public static PsmEvent handleSendVote(PsmEvent evt, PsmInterp interp) {
      ((List)interp.getUserData()).add("sendVote");
      return V3Events.evtOk;
    }

    public static PsmEvent handleReceiveRepairRequest(PsmMsgEvent evt, PsmInterp interp) {
      ((List)interp.getUserData()).add("receiveRepairRequest");
      return V3Events.evtRepairRequestOk;
    }

    public static PsmEvent handleSendRepair(PsmEvent evt, PsmInterp interp) {
      ((List)interp.getUserData()).add("sendRepair");
      return V3Events.evtOk;
    }

    public static PsmEvent handleReceiveReceipt(PsmMsgEvent evt, PsmInterp interp) {
      ((List)interp.getUserData()).add("receiveReceipt");
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
    public static PsmEvent handleReceivePollProof(PsmMsgEvent evt, PsmInterp interp) {
      return V3Events.evtError;
    }
  }

  // Error in VerifyPollProof
  public static class Errors6 extends TestActions {
    public static PsmEvent handleVerifyPollProof(PsmEvent evt, PsmInterp interp) {
      return V3Events.evtError;
    }
  }

  // Error in SendNominate
  public static class Errors7 extends TestActions {
    public static PsmEvent handleSendNominate(PsmEvent evt, PsmInterp interp) {
      return V3Events.evtError;
    }
  }

  // Error in GenerateVote
  public static class Errors8 extends TestActions {
    public static PsmEvent handleGenerateVote(PsmEvent evt, PsmInterp interp) {
      return V3Events.evtError;
    }
  }

  // Error in WaitVoteRequest
  public static class Errors9 extends TestActions {
    public static PsmEvent handleReceiveVoteRequest(PsmMsgEvent evt, PsmInterp interp) {
      return V3Events.evtError;
    }
  }

  // Error in SendVote
  public static class Errors10 extends TestActions {
    public static PsmEvent handleSendVote(PsmEvent evt, PsmInterp interp) {
      return V3Events.evtError;
    }
  }

  // Error in WaitReceipt - Repair Request
  public static class Errors11 extends TestActions {
    public static PsmEvent handleReceiveRepairRequest(PsmMsgEvent evt, PsmInterp interp) {
      return V3Events.evtError;
    }
  }

  // Error in SendRepair
  public static class Errors12 extends TestActions {
    public static PsmEvent handleSendRepair(PsmEvent evt, PsmInterp interp) {
      return V3Events.evtError;
    }
  }

  // Error in WaitReceipt - Receipt
  public static class Errors13 extends TestActions {
    public static PsmEvent handleReceiveReceipt(PsmMsgEvent evt, PsmInterp interp) {
      return V3Events.evtError;
    }
  }

  // Error in ProcessReceipt
  public static class Errors14 extends TestActions {
    public static PsmEvent handleProcessReceipt(PsmEvent evt, PsmInterp interp) {
      return V3Events.evtError;
    }
  }

  private void assertWaiting(PsmInterp interp, String stateName) {
    assertEquals(stateName, interp.getCurrentState().getName());
    assertTrue(interp.isWaiting());
    assertFalse(interp.isFinalState());
  }
}
