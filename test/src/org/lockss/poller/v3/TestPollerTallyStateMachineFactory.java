/*
 * $Id: TestPollerTallyStateMachineFactory.java,v 1.1 2005-05-04 19:04:09 smorabito Exp $
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
 * PollerTallyStateMachineFactory behaves correctly and that the
 * expected order of states occurs during operation on its state
 * table.
 */

public class TestPollerTallyStateMachineFactory extends LockssTestCase {

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
      new V3Events.Repair(new V3LcapMessage(V3LcapMessage.MSG_REPAIR_REP, this.id,
					    "http://www.test.com/",
					    123456789, 987654321,
					    ByteArray.makeRandomBytes(20)));

    this.msgNoOp = new PsmMsgEvent(V3LcapMessage.makeNoOpMsg(this.id));

  }

  public void testMachineOrderRepairRequired() throws Exception {
    PsmMachine mach = PollerTallyStateMachineFactory.getMachine(TestActions.class);
    PsmInterp interp = new PsmInterp(mach, new ArrayList());

    interp.init();

    // Verify that the poller is waiting in "WaitingRepair"
    assertEquals("WaitRepair", interp.getCurrentState().getName());
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
		     (List)interp.getUserData());
  }

  public void testMachineOrderNoRepair() throws Exception {
    PsmMachine mach =
      PollerTallyStateMachineFactory.
      getMachine(TestActionsRepairNotNeeded.class);
    PsmInterp interp = new PsmInterp(mach, new ArrayList());

    interp.init();

    // No repair should be required, so verify interpreter is in its
    // final state.
    assertTrue(interp.isFinalState());
    assertIsomorphic(ListUtil.list("initialize", "verifyVoteEffort",
				   "tally", "sendReceipt"),
		     (List)interp.getUserData());
  }


  public void testMachineOrderErrors() throws Exception {
    PsmMachine mach = null;
    PsmInterp interp = null;
    String[] expectedResults = null;

    // Error in Initialize
    mach = PollerTallyStateMachineFactory.getMachine(Errors1.class);
    interp = new PsmInterp(mach, new ArrayList());
    interp.init();
    assertTrue(interp.isFinalState());
    expectedResults = new String[] {"error"};
    assertIsomorphic(ListUtil.fromArray(expectedResults),
		     (List)interp.getUserData());

    // Error in VerifyVoteEffort
    mach = PollerTallyStateMachineFactory.getMachine(Errors2.class);
    interp = new PsmInterp(mach, new ArrayList());
    interp.init();
    assertTrue(interp.isFinalState());
    expectedResults = new String[] {"initialize", "error"};
    assertIsomorphic(ListUtil.fromArray(expectedResults),
		     (List)interp.getUserData());

    // Error in Tally
    mach = PollerTallyStateMachineFactory.getMachine(Errors3.class);
    interp = new PsmInterp(mach, new ArrayList());
    interp.init();
    assertTrue(interp.isFinalState());
    expectedResults = new String[] {"initialize", "verifyVoteEffort", "error"};
    assertIsomorphic(ListUtil.fromArray(expectedResults),
		     (List)interp.getUserData());

    // Error in ProveRepairEffort
    mach = PollerTallyStateMachineFactory.getMachine(Errors4.class);
    interp = new PsmInterp(mach, new ArrayList());
    interp.init();
    assertTrue(interp.isFinalState());
    expectedResults = new String[] {"initialize", "verifyVoteEffort",
				    "tally", "error"};
    assertIsomorphic(ListUtil.fromArray(expectedResults),
		     (List)interp.getUserData());


    // Error in SendRepairRequest
    mach = PollerTallyStateMachineFactory.getMachine(Errors5.class);
    interp = new PsmInterp(mach, new ArrayList());
    interp.init();
    assertTrue(interp.isFinalState());
    expectedResults = new String[] {"initialize", "verifyVoteEffort",
				    "tally", "proveRepairEffort", "error"};
    assertIsomorphic(ListUtil.fromArray(expectedResults),
		     (List)interp.getUserData());

    // Error in WaitRepair
    mach = PollerTallyStateMachineFactory.getMachine(Errors6.class);
    interp = new PsmInterp(mach, new ArrayList());
    interp.init();
    // Send Repair message
    interp.handleEvent(msgRepair);
    // Should error out in WaitingRepair
    assertTrue(interp.isFinalState());
    expectedResults = new String[] {"initialize", "verifyVoteEffort",
				    "tally", "proveRepairEffort",
				    "sendRepairRequest", "error"};
    assertIsomorphic(ListUtil.fromArray(expectedResults),
		     (List)interp.getUserData());

    // Error in SendReceipt
    mach = PollerTallyStateMachineFactory.getMachine(Errors7.class);
    interp = new PsmInterp(mach, new ArrayList());
    interp.init();
    // Send Repair message
    interp.handleEvent(msgRepair);
    // Should error out in SendingReceipt
    assertTrue(interp.isFinalState());
    expectedResults = new String[] {"initialize", "verifyVoteEffort",
				    "tally", "proveRepairEffort",
				    "sendRepairRequest", "receivedRepair",
				    "error"};
    assertIsomorphic(ListUtil.fromArray(expectedResults),
		     (List)interp.getUserData());

  }

  /**
   * Mock class that overrides event handlers.
   */
  public static class TestActions extends PollerTallyActions {
    public static PsmEvent handleInitialize(PsmEvent evt, PsmInterp interp) {
      ((List)interp.getUserData()).add("initialize");
      return V3Events.evtOk;
    }

    public static PsmEvent handleVerifyVoteEffort(PsmEvent evt, PsmInterp interp) {
      ((List)interp.getUserData()).add("verifyVoteEffort");
      return V3Events.evtOk;
    }

    public static PsmEvent handleTally(PsmEvent evt, PsmInterp interp) {
      ((List)interp.getUserData()).add("tally");
      return V3Events.evtRepairNeeded;
    }

    public static PsmEvent handleProveRepairEffort(PsmEvent evt, PsmInterp interp) {
      ((List)interp.getUserData()).add("proveRepairEffort");
      return V3Events.evtOk;
    }

    public static PsmEvent handleSendRepairRequest(PsmEvent evt, PsmInterp interp) {
      ((List)interp.getUserData()).add("sendRepairRequest");
      return V3Events.evtOk;
    }

    public static PsmEvent handleReceivedRepair(PsmMsgEvent evt, PsmInterp interp) {
      ((List)interp.getUserData()).add("receivedRepair");
      return V3Events.evtRepairOk;
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


  /**
   * A series of Action classes that hide each of the handleXxx methods
   * and cause error events to be returned.
   */

  // Cause 'evtRepairNotNeeded' to be returned from the Tally state.
  public static class TestActionsRepairNotNeeded extends TestActions {
    public static PsmEvent handleTally(PsmEvent evt, PsmInterp interp) {
      ((List)interp.getUserData()).add("tally");
      return V3Events.evtRepairNotNeeded;
    }
  }

  // Error in Initialize
  public static class Errors1 extends TestActions {
    public static PsmEvent handleInitialize(PsmEvent evt, PsmInterp interp) {
      return V3Events.evtError;
    }
  }


  // Error in VerifyVoteEffort
  public static class Errors2 extends TestActions {
    public static PsmEvent handleVerifyVoteEffort(PsmEvent evt, PsmInterp interp) {
      return V3Events.evtError;
    }
  }

  // Error in Tally
  public static class Errors3 extends TestActions {
    public static PsmEvent handleTally(PsmEvent evt, PsmInterp interp) {
      return V3Events.evtError;
    }
  }

  // Error in ProveRepairEffort
  public static class Errors4 extends TestActions {
    public static PsmEvent handleProveRepairEffort(PsmEvent evt, PsmInterp interp) {
      return V3Events.evtError;
    }
  }

  // Error in SendRepairRequest
  public static class Errors5 extends TestActions {
    public static PsmEvent handleSendRepairRequest(PsmEvent evt, PsmInterp interp) {
      return V3Events.evtError;
    }
  }

  // Error in WaitRepair
  public static class Errors6 extends TestActions {
    public static PsmEvent handleReceivedRepair(PsmMsgEvent evt, PsmInterp interp) {
      return V3Events.evtError;
    }
  }

  // Error in SendReceipt
  public static class Errors7 extends TestActions {
    public static PsmEvent handleSendReceipt(PsmEvent evt, PsmInterp interp) {
      return V3Events.evtError;
    }
  }
}
