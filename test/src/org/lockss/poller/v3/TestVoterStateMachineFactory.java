/*
 * $Id: TestVoterStateMachineFactory.java,v 1.11 2008-01-27 06:46:04 tlipkis Exp $
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

  private PsmMsgEvent msgPoll;
  private PsmMsgEvent msgPollProof;
  private PsmMsgEvent msgVoteRequest;
  private PsmMsgEvent msgRepairRequest;
  private PsmMsgEvent msgReceipt;
  private PsmMsgEvent msgNoOp;
  private File tempDir;
  
  public void setUp() throws Exception {
    super.setUp();

    tempDir = getTempDir();
    // Set properties.
    Properties p = new Properties();
    p.setProperty(IdentityManager.PARAM_LOCAL_IP, "127.0.0.1");
    ConfigurationUtil.setCurrentConfigFromProps(p);

    this.theDaemon = getMockLockssDaemon();
    this.theIdMgr = theDaemon.getIdentityManager();

    this.id = theIdMgr.stringToPeerIdentity("127.0.0.1");
    this.msgPoll =
      V3Events.fromMessage(new V3LcapMessage("auid", "key", "1",
                                             ByteArray.makeRandomBytes(20),
                                             ByteArray.makeRandomBytes(20),
                                             V3LcapMessage.MSG_POLL,
                                             987654321,
                                             this.id, tempDir, theDaemon));
    this.msgPollProof =
      V3Events.fromMessage(new V3LcapMessage("auid", "key", "1",
                                             ByteArray.makeRandomBytes(20),
                                             ByteArray.makeRandomBytes(20),
                                             V3LcapMessage.MSG_POLL_PROOF,
                                             987654321,
                                             this.id, tempDir, theDaemon));
    this.msgVoteRequest =
      V3Events.fromMessage(new V3LcapMessage("auid", "key", "1",
                                             ByteArray.makeRandomBytes(20),
                                             ByteArray.makeRandomBytes(20),
                                             V3LcapMessage.MSG_VOTE_REQ,
                                             987654321,
                                             this.id, tempDir, theDaemon));
    this.msgRepairRequest =
      V3Events.fromMessage(new V3LcapMessage("auid", "key", "1",
                                             ByteArray.makeRandomBytes(20),
                                             ByteArray.makeRandomBytes(20),
                                             V3LcapMessage.MSG_REPAIR_REQ,
                                             987654321,
                                             this.id, tempDir, theDaemon));
    this.msgReceipt =
      V3Events.fromMessage(new V3LcapMessage("auid", "key", "1",
                                             ByteArray.makeRandomBytes(20),
                                             ByteArray.makeRandomBytes(20),
                                             V3LcapMessage.MSG_EVALUATION_RECEIPT,
                                             987654321,
                                             this.id, tempDir, theDaemon));

    this.msgNoOp = V3Events.fromMessage(V3LcapMessage.makeNoOpMsg(this.id,
                                                                  theDaemon));

  }

  private PsmMachine makeMachine(Class factoryClass, Class actionsClass)
      throws Exception {
    PsmMachine.Factory fact = (PsmMachine.Factory)factoryClass.newInstance();
    return fact.getMachine(actionsClass);
  }

  /**
   * Ensure that a repair request is honored.
   */
  public void testRepairMachineOrder() throws Exception {

    PsmMachine mach = makeMachine(VoterStateMachineFactory.class,
				  TestActions.class);
    PsmInterp interp = new PsmInterp(mach, new ArrayList());

    interp.start();

    // Verify that the poller is waiting in "Initialize"
    assertWaiting(interp, "Initialize");
    // Send a Poll
    interp.handleEvent(msgPoll);
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
    String[] expectedResults = {"initialize", "verifyPollEffort",
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

    PsmMachine mach = makeMachine(VoterStateMachineFactory.class,
				  TestActions.class);
    PsmInterp interp = new PsmInterp(mach, new ArrayList());

    interp.start();
    // Verify that the poller is waiting in "Initialize"
    assertWaiting(interp, "Initialize");
    // Send a Poll
    interp.handleEvent(msgPoll);
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
    String[] expectedResults = {"initialize", "verifyPollEffort",
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
    PsmMachine mach = makeMachine(VoterStateMachineFactory.class,
				  TestActions.class);
    PsmInterp interp = new PsmInterp(mach, new ArrayList());
    interp.start();
    // Verify that the poller is waiting in "Initialize"
    assertWaiting(interp, "Initialize");
    // Send a Poll
    interp.handleEvent(msgPoll);
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
    String[] expectedResults = {"initialize", "verifyPollEffort",
				"provePollAck", "sendPollAck",
				"receivePollProof", "verifyPollProof",
				"sendNominate", "generateVote",
				"receiveVoteRequest", "sendVote",
				"receiveReceipt", "processReceipt"};
    assertIsomorphic(ListUtil.fromArray(expectedResults),
		     (List)interp.getUserData());
  }
  
  /**
   * Actions class that extends and hides VoterActions
   * implementations for testing.
   */
  public static class TestActions extends VoterActions {
    public static PsmEvent handleReceivePoll(PsmMsgEvent evt, PsmInterp interp) {
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

  private void assertWaiting(PsmInterp interp, String stateName) {
    assertEquals(stateName, interp.getCurrentState().getName());
    assertTrue(interp.isWaiting());
    assertFalse(interp.isFinalState());
  }
}
