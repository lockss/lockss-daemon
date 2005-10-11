/*
 * $Id: TestPollerStateMachineFactory.java,v 1.4 2005-10-11 05:50:29 tlipkis Exp $
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
 * PollerStateMachineFactory behaves correctly and that the
 * expected order of states occurs during operation on its state
 * table.
 */

public class TestPollerStateMachineFactory extends LockssTestCase {

  private IdentityManager theIdMgr;
  private LockssDaemon theDaemon;
  private PeerIdentity id;

  private PsmMsgEvent msgPollAck;
  private PsmMsgEvent msgNominate;
  private PsmMsgEvent msgVote;
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

    this.msgPollAck =
      V3Events.fromMessage(new V3LcapMessage(V3LcapMessage.MSG_POLL_ACK,
                                             "key",
					     this.id,
					     "http://www.test.com/",
					     123456789, 987654321,
					     ByteArray.makeRandomBytes(20),
                                             ByteArray.makeRandomBytes(20)));
    this.msgNominate =
      V3Events.fromMessage(new V3LcapMessage(V3LcapMessage.MSG_NOMINATE,
                                             "key",
					     this.id,
					     "http://www.test.com/",
					     123456789, 987654321,
					     ByteArray.makeRandomBytes(20),
                                             ByteArray.makeRandomBytes(20)));
    this.msgVote =
      V3Events.fromMessage(new V3LcapMessage(V3LcapMessage.MSG_VOTE,
                                             "key",
					     this.id,
					     "http://www.test.com/",
					     123456789, 987654321,
					     ByteArray.makeRandomBytes(20),
                                             ByteArray.makeRandomBytes(20)));
    this.msgRepair =
      V3Events.fromMessage(new V3LcapMessage(V3LcapMessage.MSG_REPAIR_REP,
                                             "key",
					     this.id,
					     "http://www.test.com/",
					     123456789, 987654321,
					     ByteArray.makeRandomBytes(20),
                                             ByteArray.makeRandomBytes(20)));
    this.msgNoOp =
      new PsmMsgEvent(V3LcapMessage.makeNoOpMsg(this.id));

    // Set RepairTestActions callCount to 0
    RepairTestActions.callCount = 0;
  }

  /**
   * Test a normal no-repair machine order
   */
  public void testNoRepairMachineOrder() throws Exception {
    PsmMachine mach = PollerStateMachineFactory.getMachine(NoRepairTestActions.class);
    PsmInterp interp = new PsmInterp(mach, new ArrayList());
    interp.init();
    assertWaiting(interp, "WaitPollAck");
    interp.handleEvent(msgPollAck);
    assertWaiting(interp, "WaitNominate");
    interp.handleEvent(msgNominate);
    assertWaiting(interp, "WaitVote");
    interp.handleEvent(msgVote);
    assertTrue(interp.isFinalState());
    String[] expectedResults = {"proveIntroEffort",
				"sendPoll", "receivePollAck",
				"verifyPollAckEffort",
				"proveRemainingEffort",
				"sendPollProof", "receiveNominate",
				"sendVoteRequest", "receiveVote",
				"tallyVote", "sendReceipt"};
    assertIsomorphic(ListUtil.fromArray(expectedResults),
                     (List)interp.getUserData());
  }

  /**
   * Test a normal repair machine order
   */
  public void testRepairMachineOrder() throws Exception {
    PsmMachine mach = PollerStateMachineFactory.getMachine(RepairTestActions.class);
    PsmInterp interp = new PsmInterp(mach, new ArrayList());
    interp.init();
    assertWaiting(interp, "WaitPollAck");
    interp.handleEvent(msgPollAck);
    assertWaiting(interp, "WaitNominate");
    interp.handleEvent(msgNominate);
    assertWaiting(interp, "WaitVote");
    interp.handleEvent(msgVote);
    assertWaiting(interp, "WaitRepair");
    interp.handleEvent(msgRepair);
    assertTrue(interp.isFinalState());
    String[] expectedResults = {"proveIntroEffort",
				"sendPoll", "receivePollAck",
				"verifyPollAckEffort",
				"proveRemainingEffort",
				"sendPollProof", "receiveNominate",
				"sendVoteRequest", "receiveVote",
				"tallyVote", "proveRepairEffort",
				"sendRepairRequest", "receiveRepair",
				"processRepair", "tallyVote",
				"sendReceipt"};
    assertIsomorphic(ListUtil.fromArray(expectedResults),
                     (List)interp.getUserData());
  }

  /**
   * Ensure that each state properly handles errors.
   */

  /**  Error in ProveIntroEffort */
  public void testProveIntroEffortError() throws Exception {
    PsmMachine mach = PollerStateMachineFactory.getMachine(Errors2.class);
    PsmInterp interp = new PsmInterp(mach, new ArrayList());
    interp.init();
    assertTrue(interp.isFinalState());
    String[] expectedResults = new String[] {"error"};
    assertIsomorphic(ListUtil.fromArray(expectedResults),
		     (List)interp.getUserData());

  }

  /**  Error in SendPoll */
  public void testSendPollError() throws Exception {
    PsmMachine mach = PollerStateMachineFactory.getMachine(Errors3.class);
    PsmInterp interp = new PsmInterp(mach, new ArrayList());
    interp.init();
    assertTrue(interp.isFinalState());
    String[] expectedResults = new String[] {"proveIntroEffort", "error"};
    assertIsomorphic(ListUtil.fromArray(expectedResults),
		     (List)interp.getUserData());
  }


  /**  Error in WaitPollAck */
  public void testWaitPollAckError() throws Exception {
    PsmMachine mach = PollerStateMachineFactory.getMachine(Errors4.class);
    PsmInterp interp = new PsmInterp(mach, new ArrayList());
    interp.init();
    assertWaiting(interp, "WaitPollAck");
    interp.handleEvent(msgPollAck);
    assertTrue(interp.isFinalState());
    String[] expectedResults = new String[] {"proveIntroEffort",
					     "sendPoll", "error"};
    assertIsomorphic(ListUtil.fromArray(expectedResults),
		     (List)interp.getUserData());
  }

  /**  Error in VerifyPollAckEffort */
  public void testVerifyPollAckEffortError() throws Exception {
    PsmMachine mach = PollerStateMachineFactory.getMachine(Errors5.class);
    PsmInterp interp = new PsmInterp(mach, new ArrayList());
    interp.init();
    assertWaiting(interp, "WaitPollAck");
    interp.handleEvent(msgPollAck);
    assertTrue(interp.isFinalState());
    String[] expectedResults = new String[] {"proveIntroEffort",
				    "sendPoll", "receivePollAck", "error"};
    assertIsomorphic(ListUtil.fromArray(expectedResults),
		     (List)interp.getUserData());

  }

  /**  Error in ProveRemainingEffort */
  public void testProveRemainingEffortError() throws Exception {
    PsmMachine mach = PollerStateMachineFactory.getMachine(Errors6.class);
    PsmInterp interp = new PsmInterp(mach, new ArrayList());
    interp.init();
    assertWaiting(interp, "WaitPollAck");
    interp.handleEvent(msgPollAck);
    assertTrue(interp.isFinalState());
    String[] expectedResults = new String[] {"proveIntroEffort",
				    "sendPoll", "receivePollAck",
				    "verifyPollAckEffort", "error"};
    assertIsomorphic(ListUtil.fromArray(expectedResults),
		     (List)interp.getUserData());

  }


  /**  Error in SendPollProof */
  public void testSendPollProofError() throws Exception {
    PsmMachine mach = PollerStateMachineFactory.getMachine(Errors7.class);
    PsmInterp interp = new PsmInterp(mach, new ArrayList());
    interp.init();
    assertWaiting(interp, "WaitPollAck");
    interp.handleEvent(msgPollAck);
    assertTrue(interp.isFinalState());
    String[] expectedResults = new String[] {"proveIntroEffort",
				    "sendPoll", "receivePollAck",
				    "verifyPollAckEffort",
				    "proveRemainingEffort", "error"};
    assertIsomorphic(ListUtil.fromArray(expectedResults),
		     (List)interp.getUserData());

  }


  /**  Error in WaitNominate */
  public void testWaitNominateError() throws Exception {
    PsmMachine mach = PollerStateMachineFactory.getMachine(Errors8.class);
    PsmInterp interp = new PsmInterp(mach, new ArrayList());
    interp.init();
    assertWaiting(interp, "WaitPollAck");
    interp.handleEvent(msgPollAck);
    assertWaiting(interp, "WaitNominate");
    interp.handleEvent(msgNominate);
    assertTrue(interp.isFinalState());
    String[] expectedResults = new String[] {"proveIntroEffort",
				    "sendPoll", "receivePollAck",
				    "verifyPollAckEffort",
				    "proveRemainingEffort", "sendPollProof",
				    "error"};
    assertIsomorphic(ListUtil.fromArray(expectedResults),
		     (List)interp.getUserData());

  }

  /**  Error in SendVoteRequest */
  public void testSendVoteRequestError() throws Exception {
    PsmMachine mach = PollerStateMachineFactory.getMachine(Errors9.class);
    PsmInterp interp = new PsmInterp(mach, new ArrayList());
    interp.init();
    assertWaiting(interp, "WaitPollAck");
    interp.handleEvent(msgPollAck);
    assertWaiting(interp, "WaitNominate");
    interp.handleEvent(msgNominate);
    assertTrue(interp.isFinalState());
    String[] expectedResults = new String[] {"proveIntroEffort",
				    "sendPoll", "receivePollAck",
				    "verifyPollAckEffort",
				    "proveRemainingEffort", "sendPollProof",
				    "receiveNominate", "error"};
    assertIsomorphic(ListUtil.fromArray(expectedResults),
		     (List)interp.getUserData());

  }

  /**  Error in WaitVote */
  public void testWaitVoteError() throws Exception {
    PsmMachine mach = PollerStateMachineFactory.getMachine(Errors10.class);
    PsmInterp interp = new PsmInterp(mach, new ArrayList());
    interp.init();
    assertWaiting(interp, "WaitPollAck");
    interp.handleEvent(msgPollAck);
    assertWaiting(interp, "WaitNominate");
    interp.handleEvent(msgNominate);
    assertWaiting(interp, "WaitVote");
    interp.handleEvent(msgVote);
    assertTrue(interp.isFinalState());
    String[] expectedResults = new String[] {"proveIntroEffort",
				    "sendPoll", "receivePollAck",
				    "verifyPollAckEffort",
				    "proveRemainingEffort", "sendPollProof",
				    "receiveNominate", "sendVoteRequest",
				    "error"};
    assertIsomorphic(ListUtil.fromArray(expectedResults),
		     (List)interp.getUserData());

  }


  /**  Error in TallyVote */
  public void testTallyVoteError() throws Exception {
    PsmMachine mach = PollerStateMachineFactory.getMachine(Errors11.class);
    PsmInterp interp = new PsmInterp(mach, new ArrayList());
    interp.init();
    assertWaiting(interp, "WaitPollAck");
    interp.handleEvent(msgPollAck);
    assertWaiting(interp, "WaitNominate");
    interp.handleEvent(msgNominate);
    assertWaiting(interp, "WaitVote");
    interp.handleEvent(msgVote);
    assertTrue(interp.isFinalState());
    String[] expectedResults = new String[] {"proveIntroEffort",
				    "sendPoll", "receivePollAck",
				    "verifyPollAckEffort",
				    "proveRemainingEffort", "sendPollProof",
				    "receiveNominate", "sendVoteRequest",
				    "receiveVote", "error"};
    assertIsomorphic(ListUtil.fromArray(expectedResults),
		     (List)interp.getUserData());

  }


  /**  Error in ProveRepairEffort */
  public void testProveRepairEffortError() throws Exception {
    PsmMachine mach = PollerStateMachineFactory.getMachine(Errors12.class);
    PsmInterp interp = new PsmInterp(mach, new ArrayList());
    interp.init();
    assertWaiting(interp, "WaitPollAck");
    interp.handleEvent(msgPollAck);
    assertWaiting(interp, "WaitNominate");
    interp.handleEvent(msgNominate);
    assertWaiting(interp, "WaitVote");
    interp.handleEvent(msgVote);
    assertTrue(interp.isFinalState());
    String[] expectedResults = new String[] {"proveIntroEffort",
				    "sendPoll", "receivePollAck",
				    "verifyPollAckEffort",
				    "proveRemainingEffort", "sendPollProof",
				    "receiveNominate", "sendVoteRequest",
				    "receiveVote", "tallyVote", "error"};
    assertIsomorphic(ListUtil.fromArray(expectedResults),
		     (List)interp.getUserData());


  }


  /**  Error in SendRepairRequest */
  public void testSendRepairRequestError() throws Exception {
    PsmMachine mach = PollerStateMachineFactory.getMachine(Errors13.class);
    PsmInterp interp = new PsmInterp(mach, new ArrayList());
    interp.init();
    assertWaiting(interp, "WaitPollAck");
    interp.handleEvent(msgPollAck);
    assertWaiting(interp, "WaitNominate");
    interp.handleEvent(msgNominate);
    assertWaiting(interp, "WaitVote");
    interp.handleEvent(msgVote);
    assertTrue(interp.isFinalState());
    String[] expectedResults = new String[] {"proveIntroEffort",
				    "sendPoll", "receivePollAck",
				    "verifyPollAckEffort",
				    "proveRemainingEffort", "sendPollProof",
				    "receiveNominate", "sendVoteRequest",
				    "receiveVote", "tallyVote",
				    "proveRepairEffort", "error"};
    assertIsomorphic(ListUtil.fromArray(expectedResults),
		     (List)interp.getUserData());

  }


  /**  Error in WaitRepair */
  public void testWaitRepairError() throws Exception {
    PsmMachine mach = PollerStateMachineFactory.getMachine(Errors14.class);
    PsmInterp interp = new PsmInterp(mach, new ArrayList());
    interp.init();
    assertWaiting(interp, "WaitPollAck");
    interp.handleEvent(msgPollAck);
    assertWaiting(interp, "WaitNominate");
    interp.handleEvent(msgNominate);
    assertWaiting(interp, "WaitVote");
    interp.handleEvent(msgVote);
    assertWaiting(interp, "WaitRepair");
    interp.handleEvent(msgRepair);
    // Should error out in WaitRepair
    assertTrue(interp.isFinalState());
    String[] expectedResults = new String[] {"proveIntroEffort",
				    "sendPoll", "receivePollAck",
				    "verifyPollAckEffort",
				    "proveRemainingEffort", "sendPollProof",
				    "receiveNominate", "sendVoteRequest",
				    "receiveVote", "tallyVote",
				    "proveRepairEffort", "sendRepairRequest",
				    "error"};
    assertIsomorphic(ListUtil.fromArray(expectedResults),
		     (List)interp.getUserData());

  }


  /**  Error in ProcessRepair */
  public void testProcessRepairError() throws Exception {
    PsmMachine mach = PollerStateMachineFactory.getMachine(Errors15.class);
    PsmInterp interp = new PsmInterp(mach, new ArrayList());
    interp.init();
    assertWaiting(interp, "WaitPollAck");
    interp.handleEvent(msgPollAck);
    assertWaiting(interp, "WaitNominate");
    interp.handleEvent(msgNominate);
    assertWaiting(interp, "WaitVote");
    interp.handleEvent(msgVote);
    assertWaiting(interp, "WaitRepair");
    interp.handleEvent(msgRepair);
    assertTrue(interp.isFinalState());
    String[] expectedResults = new String[] {"proveIntroEffort",
				    "sendPoll", "receivePollAck",
				    "verifyPollAckEffort",
				    "proveRemainingEffort", "sendPollProof",
				    "receiveNominate", "sendVoteRequest",
				    "receiveVote", "tallyVote",
				    "proveRepairEffort", "sendRepairRequest",
				    "receiveRepair", "error"};
    assertIsomorphic(ListUtil.fromArray(expectedResults),
		     (List)interp.getUserData());

  }


  /**  Error in SendReceipt */
  public void testSendReceiptError() throws Exception {
    PsmMachine mach = PollerStateMachineFactory.getMachine(Errors16.class);
    PsmInterp interp = new PsmInterp(mach, new ArrayList());
    interp.init();
    assertWaiting(interp, "WaitPollAck");
    interp.handleEvent(msgPollAck);
    assertWaiting(interp, "WaitNominate");
    interp.handleEvent(msgNominate);
    assertWaiting(interp, "WaitVote");
    interp.handleEvent(msgVote);
    assertWaiting(interp, "WaitRepair");
    interp.handleEvent(msgRepair);
    assertTrue(interp.isFinalState());
    String[] expectedResults = new String[] {"proveIntroEffort",
				    "sendPoll", "receivePollAck",
				    "verifyPollAckEffort",
				    "proveRemainingEffort", "sendPollProof",
				    "receiveNominate", "sendVoteRequest",
				    "receiveVote", "tallyVote",
				    "proveRepairEffort", "sendRepairRequest",
				    "receiveRepair", "processRepair",
				    "tallyVote", "error"};
    assertIsomorphic(ListUtil.fromArray(expectedResults),
		     (List)interp.getUserData());
  }

  /**
   * Action class that extends and hides PollerActions
   * implementations for testing.
   */
  public static class NoRepairTestActions extends PollerActions {
    public static PsmEvent handleProveIntroEffort(PsmEvent evt, PsmInterp interp) {
      ((List)interp.getUserData()).add("proveIntroEffort");
      return V3Events.evtOk;
    }

    public static PsmEvent handleSendPoll(PsmEvent evt, PsmInterp interp) {
      ((List)interp.getUserData()).add("sendPoll");
      return V3Events.evtOk;
    }

    public static PsmEvent handleReceivePollAck(PsmMsgEvent evt, PsmInterp interp) {
      ((List)interp.getUserData()).add("receivePollAck");
      return V3Events.evtOk;
    }

    public static PsmEvent handleVerifyPollAckEffort(PsmEvent evt, PsmInterp interp) {
      ((List)interp.getUserData()).add("verifyPollAckEffort");
      return V3Events.evtOk;
    }

    public static PsmEvent handleProveRemainingEffort(PsmEvent evt, PsmInterp interp) {
      ((List)interp.getUserData()).add("proveRemainingEffort");
      return V3Events.evtOk;
    }

    public static PsmEvent handleSendPollProof(PsmEvent evt, PsmInterp interp) {
      ((List)interp.getUserData()).add("sendPollProof");
      return V3Events.evtOk;
    }

    public static PsmEvent handleReceiveNominate(PsmMsgEvent evt, PsmInterp interp) {
      ((List)interp.getUserData()).add("receiveNominate");
      return V3Events.evtOk;
    }

    public static PsmEvent handleSendVoteRequest(PsmEvent evt, PsmInterp interp) {
      ((List)interp.getUserData()).add("sendVoteRequest");
      return V3Events.evtOk;
    }

    public static PsmEvent handleReceiveVote(PsmMsgEvent evt, PsmInterp interp) {
      ((List)interp.getUserData()).add("receiveVote");
      return V3Events.evtOk;
    }

    public static PsmEvent handleTallyVote(PsmEvent evt, PsmInterp interp) {
      ((List)interp.getUserData()).add("tallyVote");
      return V3Events.evtVoteComplete;
    }

    public static PsmEvent handleProveRepairEffort(PsmEvent evt, PsmInterp interp) {
      ((List)interp.getUserData()).add("proveRepairEffort");
      return V3Events.evtOk;
    }

    public static PsmEvent handleSendRepairRequest(PsmEvent evt, PsmInterp interp) {
      ((List)interp.getUserData()).add("sendRepairRequest");
      return V3Events.evtOk;
    }

    public static PsmEvent handleReceiveRepair(PsmMsgEvent evt, PsmInterp interp) {
      ((List)interp.getUserData()).add("receiveRepair");
      return V3Events.evtOk;
    }

    public static PsmEvent handleProcessRepair(PsmEvent evt, PsmInterp interp) {
      ((List)interp.getUserData()).add("processRepair");
      return V3Events.evtOk;
    }

    public static PsmEvent handleSendReceipt(PsmEvent evt, PsmInterp interp) {
      ((List)interp.getUserData()).add("sendReceipt");
      return V3Events.evtOk;
    }

    public static PsmEvent handleError(PsmEvent evt, PsmInterp interp) {
      ((List)interp.getUserData()).add("error");
      return V3Events.evtOk;
    }
  }


  // The first time it's called, handleTallyVote will indicate that it
  // needs repairs.  Subsequent calls will return 'evtVoteComplete'.
  public static class RepairTestActions extends NoRepairTestActions {
    static int callCount = 0;

    public static PsmEvent handleTallyVote(PsmEvent evt, PsmInterp interp) {
      ((List)interp.getUserData()).add("tallyVote");
      if (callCount++ == 0) {
	return V3Events.evtNeedRepair;
      } else {
	return V3Events.evtVoteComplete;
      }
    }
  }

  /**
   * A series of Action classes that hide each of the handleXxx methods
   * and cause error events to be returned.
   */

  // Error in ProveIntroEffort
  public static class Errors2 extends RepairTestActions {
    public static PsmEvent handleProveIntroEffort(PsmEvent evt, PsmInterp interp) {
      return V3Events.evtError;
    }
  }

  // Error in SendPoll
  public static class Errors3 extends RepairTestActions {
    public static PsmEvent handleSendPoll(PsmEvent evt, PsmInterp interp) {
      return V3Events.evtError;
    }
  }

  // Error in WaitPollAck
  public static class Errors4 extends RepairTestActions {
    public static PsmEvent handleReceivePollAck(PsmMsgEvent evt, PsmInterp interp) {
	return V3Events.evtError;
    }
  }

  // Error in VerifyPollAckEffort
  public static class Errors5 extends RepairTestActions {
    public static PsmEvent handleVerifyPollAckEffort(PsmEvent evt, PsmInterp interp) {
      return V3Events.evtError;
    }
  }

  // Error in ProveRemainingEffort
  public static class Errors6 extends RepairTestActions {
    public static PsmEvent handleProveRemainingEffort(PsmEvent evt, PsmInterp interp) {
      return V3Events.evtError;
    }
  }

  // Error in SendPollProof
  public static class Errors7 extends RepairTestActions {
    public static PsmEvent handleSendPollProof(PsmEvent evt, PsmInterp interp) {
      return V3Events.evtError;
    }
  }

  // Error in WaitNominate
  public static class Errors8 extends RepairTestActions {
    public static PsmEvent handleReceiveNominate(PsmMsgEvent evt, PsmInterp interp) {
      return V3Events.evtError;
    }
  }

  // Error in SendVoteRequest
  public static class Errors9 extends RepairTestActions {
    public static PsmEvent handleSendVoteRequest(PsmEvent evt, PsmInterp interp) {
      return V3Events.evtError;
    }
  }

  // Error in WaitVote
  public static class Errors10 extends RepairTestActions {
    public static PsmEvent handleReceiveVote(PsmMsgEvent evt, PsmInterp interp) {
      return V3Events.evtError;
    }
  }

  // Error in TallyVote
  public static class Errors11 extends RepairTestActions {
    public static PsmEvent handleTallyVote(PsmEvent evt, PsmInterp interp) {
      return V3Events.evtError;
    }
  }

  // Error in ProveRepairEffort
  public static class Errors12 extends RepairTestActions {
    public static PsmEvent handleProveRepairEffort(PsmEvent evt, PsmInterp interp) {
      return V3Events.evtError;
    }
  }

  // Error in SendRepairRequest
  public static class Errors13 extends RepairTestActions {
    public static PsmEvent handleSendRepairRequest(PsmEvent evt, PsmInterp interp) {
      return V3Events.evtError;
    }
  }

  // Error in WaitRepair
  public static class Errors14 extends RepairTestActions {
    public static PsmEvent handleReceiveRepair(PsmMsgEvent evt, PsmInterp interp) {
      return V3Events.evtError;
    }
  }

  // Error in ProcessRepair
  public static class Errors15 extends RepairTestActions {
    public static PsmEvent handleProcessRepair(PsmEvent evt, PsmInterp interp) {
      return V3Events.evtError;
    }
  }

  // Error in SendReceipt
  public static class Errors16 extends RepairTestActions {
    public static PsmEvent handleSendReceipt(PsmEvent evt, PsmInterp interp) {
      return V3Events.evtError;
    }
  }

  // Assert that a given interpreter is waiting in the specified state
  private void assertWaiting(PsmInterp interp, String stateName) {
    assertEquals(stateName, interp.getCurrentState().getName());
    assertTrue(interp.isWaiting());
    assertFalse(interp.isFinalState());
  }

}
