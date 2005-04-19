/*
 * $Id: TestPollerSolicitStateTable.java,v 1.1 2005-04-19 03:08:33 smorabito Exp $
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

public class TestPollerSolicitStateTable extends LockssTestCase {

  private IdentityManager theIdMgr;
  private LockssDaemon theDaemon;
  private PeerIdentity id;

  private PsmMsgEvent msgPollAck;
  private PsmMsgEvent msgNoOp;
  private PsmMsgEvent msgVote;

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
      new PollerSolicitStateTable.PollAck(new V3LcapMessage(V3LcapMessage.MSG_POLL_ACK, this.id,
							    "http://www.test.com/",
							    123456789, 987654321,
							    ByteArray.makeRandomBytes(20)));
    this.msgVote =
      new PollerSolicitStateTable.Vote(new V3LcapMessage(V3LcapMessage.MSG_VOTE, this.id,
							 "http://www.test.com/",
							 123456789, 987654321,
							 ByteArray.makeRandomBytes(20)));

    this.msgNoOp = new PsmMsgEvent(V3LcapMessage.makeNoOpMsg(this.id));
  }

  /**
   * Ensure that the default state transitions occur properly.
   */
  public void testMachineOrder() throws Exception {
    MyMockPollerSolicitStateTable table =
      new MyMockPollerSolicitStateTable();

    // Simply run the machine for now.
    PsmInterp interp = table.getInterp();

    interp.init();

    // Verify that the poller is waiting in "WaitingPollAck"
    assertEquals("WaitingPollAck", interp.getCurrentState().getName());
    assertTrue(interp.isWaiting());
    assertFalse(interp.isFinalState());

    // Send a Poll Ack message
    interp.handleEvent(msgPollAck);

    // Verify that the interpreter is waiting in "WaitingVote"
    assertEquals("WaitingVote", interp.getCurrentState().getName());
    assertTrue(interp.isWaiting());
    assertFalse(interp.isFinalState());

    // Send a Vote message
    interp.handleEvent(msgVote);

    // Verify that the interpreter is in its final state.
    assertTrue(interp.isFinalState());
    assertIsomorphic(ListUtil.list("initialize", "proveIntroEffort",
				   "sendPoll", "receivedPollAck",
				   "verifyPollAckEffort",
				   "proveRemainingEffort",
				   "sendPollProof", "receivedVote"),
		     table.eventList);
  }

  /**
   * Ensure that each state properly handles errors.
   */
  public void testMachineOrderErrors() throws Exception {
    PsmInterp interp = null;
    String[] expectedResults = null;
    MyMockPollerSolicitStateTable table = null;

    // Error in Initializing
    table = new MyMockPollerSolicitStateTable() {
	protected PsmEvent handleInitialize(PsmEvent evt, PsmInterp interp) {
	  return evtError;
	}
      };
    interp = table.getInterp();
    interp.init();
    assertTrue(interp.isFinalState());
    expectedResults = new String[] {"error"};
    assertIsomorphic(ListUtil.fromArray(expectedResults), table.eventList);

    // Error in ProvingIntroEffort
    table = new MyMockPollerSolicitStateTable() {
	protected PsmEvent handleProveIntroEffort(PsmEvent evt, PsmInterp interp) {
	  return evtError;
	}
      };
    interp = table.getInterp();
    interp.init();
    assertTrue(interp.isFinalState());
    expectedResults = new String[] {"initialize", "error"};
    assertIsomorphic(ListUtil.fromArray(expectedResults), table.eventList);

    // Error in SendingPoll
    table = new MyMockPollerSolicitStateTable() {
	protected PsmEvent handleSendPoll(PsmEvent evt, PsmInterp interp) {
	  return evtError;
	}
      };
    interp = table.getInterp();
    interp.init();
    assertTrue(interp.isFinalState());
    expectedResults = new String[] {"initialize", "proveIntroEffort", "error"};
    assertIsomorphic(ListUtil.fromArray(expectedResults), table.eventList);

    // Error in WaitingPollAck
    table = new MyMockPollerSolicitStateTable() {
	protected PsmEvent handleReceivedPollAck(PsmMsgEvent evt, PsmInterp interp) {
	  return evtError;
	}
      };
    interp = table.getInterp();
    interp.init();
    assertFalse(interp.isFinalState());
    assertTrue(interp.isWaiting());
    assertEquals("WaitingPollAck", interp.getCurrentState().getName());
    // Send PollAck messsage
    interp.handleEvent(msgPollAck);
    // Should error out in WaitingPollAck
    assertTrue(interp.isFinalState());
    expectedResults = new String[] {"initialize", "proveIntroEffort",
				    "sendPoll", "error"};
    assertIsomorphic(ListUtil.fromArray(expectedResults), table.eventList);

    // Error in VerifyingPollAckEffort
    table = new MyMockPollerSolicitStateTable() {
	protected PsmEvent handleVerifyPollAckEffort(PsmEvent evt, PsmInterp interp) {
	  return evtError;
	}
      };
    interp = table.getInterp();
    interp.init();
    assertFalse(interp.isFinalState());
    assertTrue(interp.isWaiting());
    assertEquals("WaitingPollAck", interp.getCurrentState().getName());
    // Send PollAck message
    interp.handleEvent(msgPollAck);
    // Should error out in VerifyingPollAckEffort
    assertTrue(interp.isFinalState());
    expectedResults = new String[] {"initialize", "proveIntroEffort",
				    "sendPoll", "receivedPollAck", "error"};
    assertIsomorphic(ListUtil.fromArray(expectedResults), table.eventList);


    // Error in ProvingRemainingEffort
    table = new MyMockPollerSolicitStateTable() {
	protected PsmEvent handleProveRemainingEffort(PsmEvent evt, PsmInterp interp) {
	  return evtError;
	}
      };
    interp = table.getInterp();
    interp.init();
    assertFalse(interp.isFinalState());
    assertTrue(interp.isWaiting());
    assertEquals("WaitingPollAck", interp.getCurrentState().getName());
    // Send PollAck message
    interp.handleEvent(msgPollAck);
    // Should error out in ProvingRemainingEffort
    assertTrue(interp.isFinalState());
    expectedResults = new String[] {"initialize", "proveIntroEffort",
				    "sendPoll", "receivedPollAck",
				    "verifyPollAckEffort", "error"};
    assertIsomorphic(ListUtil.fromArray(expectedResults), table.eventList);

    // Error in SendingPollProof
    table = new MyMockPollerSolicitStateTable() {
	protected PsmEvent handleSendPollProof(PsmEvent evt, PsmInterp interp) {
	  return evtError;
	}
      };
    interp = table.getInterp();
    interp.init();
    assertFalse(interp.isFinalState());
    assertTrue(interp.isWaiting());
    assertEquals("WaitingPollAck", interp.getCurrentState().getName());
    // Send PollAck message
    interp.handleEvent(msgPollAck);
    // Should error out in SendingPollProof
    assertTrue(interp.isFinalState());
    expectedResults = new String[] {"initialize", "proveIntroEffort",
				    "sendPoll", "receivedPollAck",
				    "verifyPollAckEffort",
				    "proveRemainingEffort", "error"};
    assertIsomorphic(ListUtil.fromArray(expectedResults), table.eventList);


    // Error in WaitingVote
    table = new MyMockPollerSolicitStateTable() {
	protected PsmEvent handleReceivedVote(PsmMsgEvent evt, PsmInterp interp) {
	  return evtError;
	}
      };
    interp = table.getInterp();
    interp.init();
    assertFalse(interp.isFinalState());
    assertTrue(interp.isWaiting());
    assertEquals("WaitingPollAck", interp.getCurrentState().getName());
    // Send PollAck message
    interp.handleEvent(msgPollAck);
    assertFalse(interp.isFinalState());
    assertTrue(interp.isWaiting());
    assertEquals("WaitingVote", interp.getCurrentState().getName());
    // Send Vote message
    interp.handleEvent(msgVote);
    // Should error out in WaitingVote
    assertTrue(interp.isFinalState());
    expectedResults = new String[] {"initialize", "proveIntroEffort",
				    "sendPoll", "receivedPollAck",
				    "verifyPollAckEffort",
				    "proveRemainingEffort", "sendPollProof",
				    "error"};
    assertIsomorphic(ListUtil.fromArray(expectedResults), table.eventList);

  }

  /**
   * Mock class that overrides event handlers and logs entry actions
   * as they are called.
   */
  private static class MyMockPollerSolicitStateTable
    extends PollerSolicitStateTable {

    public List eventList = new ArrayList();

    protected PsmEvent handleInitialize(PsmEvent evt, PsmInterp interp) {
      eventList.add("initialize");
      return evtOk;
    }

    protected PsmEvent handleProveIntroEffort(PsmEvent evt, PsmInterp interp) {
      eventList.add("proveIntroEffort");
      return evtOk;
    }

    protected PsmEvent handleSendPoll(PsmEvent evt, PsmInterp interp) {
      eventList.add("sendPoll");
      return evtOk;
    }

    protected PsmEvent handleReceivedPollAck(PsmMsgEvent evt, PsmInterp interp) {
      eventList.add("receivedPollAck");
      return evtOk;
    }

    protected PsmEvent handleVerifyPollAckEffort(PsmEvent evt, PsmInterp interp) {
      eventList.add("verifyPollAckEffort");
      return evtOk;
    }

    protected PsmEvent handleProveRemainingEffort(PsmEvent evt, PsmInterp interp) {
      eventList.add("proveRemainingEffort");
      return evtOk;
    }

    protected PsmEvent handleSendPollProof(PsmEvent evt, PsmInterp interp) {
      eventList.add("sendPollProof");
      return evtOk;
    }

    protected PsmEvent handleReceivedVote(PsmMsgEvent evt, PsmInterp interp) {
      eventList.add("receivedVote");
      return evtOk;
    }

    protected PsmEvent handleError(PsmEvent evt, PsmInterp interp) {
      eventList.add("error");
      return evtOk;
    }
  }


}
