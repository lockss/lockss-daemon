/*
 * $Id: TestPollerStateMachineFactory.java,v 1.9 2008-01-27 06:46:04 tlipkis Exp $
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
  private File tempDir;

  public void setUp() throws Exception {
    super.setUp();

    // Set properties.
    Properties p = new Properties();
    p.setProperty(IdentityManager.PARAM_LOCAL_IP, "127.0.0.1");
    ConfigurationUtil.setCurrentConfigFromProps(p);
    
    this.tempDir = getTempDir();

    this.theDaemon = getMockLockssDaemon();
    this.theIdMgr = theDaemon.getIdentityManager();

    this.id = theIdMgr.stringToPeerIdentity("127.0.0.1");

    this.msgPollAck =
      V3Events.fromMessage(new V3LcapMessage("auid", "key", "1",
                                             ByteArray.makeRandomBytes(20),
                                             ByteArray.makeRandomBytes(20),
                                             V3LcapMessage.MSG_POLL_ACK,
					     987654321,
                                             this.id, tempDir, theDaemon));
    this.msgNominate =
      V3Events.fromMessage(new V3LcapMessage("auid", "key", "1",
                                             ByteArray.makeRandomBytes(20),
                                             ByteArray.makeRandomBytes(20),
                                             V3LcapMessage.MSG_NOMINATE,
                                             987654321,
                                             this.id, tempDir, theDaemon));
    this.msgVote =
      V3Events.fromMessage(new V3LcapMessage("auid", "key", "1",
                                             ByteArray.makeRandomBytes(20),
                                             ByteArray.makeRandomBytes(20),
                                             V3LcapMessage.MSG_VOTE,
                                             987654321,
                                             this.id, tempDir, theDaemon));
    this.msgRepair =
      V3Events.fromMessage(new V3LcapMessage("auid", "key", "1",
                                             ByteArray.makeRandomBytes(20),
                                             ByteArray.makeRandomBytes(20),
                                             V3LcapMessage.MSG_REPAIR_REP,
                                             987654321,
                                             this.id, tempDir, theDaemon));
    this.msgNoOp =
      new PsmMsgEvent(V3LcapMessage.makeNoOpMsg(this.id, theDaemon));
  }

  private PsmMachine makeMachine(Class factoryClass, Class actionsClass)
      throws Exception {
    PsmMachine.Factory fact = (PsmMachine.Factory)factoryClass.newInstance();
    return fact.getMachine(actionsClass);
  }

  /**
   * Test a normal no-repair machine order
   */
  public void testMachineOrder() throws Exception {
    PsmMachine mach = makeMachine(PollerStateMachineFactory.class,
				  TestActions.class);
    PsmInterp interp = new PsmInterp(mach, new ArrayList());
    interp.start();
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
   * Action class that extends and hides PollerActions
   * implementations for testing.
   */
  public static class TestActions extends PollerActions {
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

    public static PsmEvent handleSendReceipt(PsmEvent evt, PsmInterp interp) {
      ((List)interp.getUserData()).add("sendReceipt");
      return V3Events.evtOk;
    }

    public static PsmEvent handleError(PsmEvent evt, PsmInterp interp) {
      ((List)interp.getUserData()).add("error");
      return V3Events.evtOk;
    }
  }

  // Assert that a given interpreter is waiting in the specified state
  private void assertWaiting(PsmInterp interp, String stateName) {
    assertEquals(stateName, interp.getCurrentState().getName());
    assertTrue(interp.isWaiting());
    assertFalse(interp.isFinalState());
  }

}
