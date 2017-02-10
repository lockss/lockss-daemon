/*
 * $Id$
 */

/*

Copyright (c) 2012 Board of Trustees of Leland Stanford Jr. University,
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

import org.lockss.poller.*;
import org.lockss.app.LockssDaemon;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.plugin.PluginManager;
import org.lockss.protocol.psm.PsmMachine;
import org.lockss.protocol.psm.PsmInterp;
import org.lockss.protocol.psm.*;
import org.lockss.protocol.PeerIdentity;
import org.lockss.protocol.LcapMessage;
import org.lockss.protocol.LcapStreamComm;
import org.lockss.protocol.IdentityManager;
import org.lockss.config.*;
import org.lockss.protocol.V3LcapMessage;
import org.lockss.util.Logger;
import org.lockss.util.*;


/**
 */
public class V3ReusableRepairer {

  private PsmInterp stateMachine;

  private UserData userData;

  private LockssDaemon daemon;
  private PollManager pollManager;

  private static final Logger log = Logger.getLogger("V3ReusableRepairer");

  /**
   * Return true iff this is a valid starting message.
   */
  public static boolean canStart(LcapMessage msg) {
    return msg.getOpcode() == V3LcapMessage.MSG_REPAIR_REQ;
  }

  public V3ReusableRepairer(LockssDaemon daemon) {
    
    this.daemon = daemon;
    log.debug3("Creating V3ReusableRepairer");

    this.pollManager = daemon.getPollManager();
    this.userData = new UserData();

    postConstruct();
  }

  private void postConstruct() {
    stateMachine = makeStateMachine(userData);
    startStateMachine();
  }

  private PsmInterp newPsmInterp(PsmMachine stateMachine, UserData userData) {
    PsmManager mgr = daemon.getPsmManager();
    PsmInterp interp = mgr.newPsmInterp(stateMachine, userData);
    interp.setThreaded(true);
    return interp;
  }

  private PsmInterp makeStateMachine(final UserData ud) {
    PsmMachine machine = makeMachine();
    PsmInterp interp = newPsmInterp(machine, ud);
    interp.setName("ReusableRepairer");
    interp.setCheckpointer(new PsmInterp.Checkpointer() {
      public void checkpoint(PsmInterpStateBean resumeStateBean) {
	// Throw an error?
      }
    });

    return interp;
  }

  /**
   * Handle an incoming V3LcapMessage.
   */
  public void receiveMessage(LcapMessage message) {
    final V3LcapMessage msg = (V3LcapMessage)message;
    PeerIdentity sender = msg.getOriginatorId();
    PsmMsgEvent evt = V3Events.fromMessage(msg);
    log.debug3("Received message: " + message.getOpcodeString() + " " + message);
    String errmsg = "State machine error";
    stateMachine.enqueueEvent(evt, ehAbortPoll(errmsg),
			      new PsmInterp.Action() {
				public void eval() {
				  msg.delete();
				}
			      });
    // Finally, clean up after the V3LcapMessage
    // todo(bhayes): Really? And "finally" rather than "try/finally?"
    msg.delete();
  }

  private void startStateMachine() {
    // start the state machine running.
    String msg = "Error starting V3ReusableRepairer";
    try {
      stateMachine.enqueueStart(ehAbortPoll(msg));
    } catch (PsmException e) {
      log.warning(msg, e);
      abort();
    }
  }

  PsmInterp.ErrorHandler ehAbortPoll(final String msg) {
    return new PsmInterp.ErrorHandler() {
	public void handleError(PsmException e) {
	  log.warning(msg, e);
	  abort();
	}
      };
  }

  private void abort() {
    // todo(bhayes): implement
  }

  private Class getReusableRepairerActionsClass() {
    return ReusableRepairerActions.class;
  }

  private PsmMachine makeMachine() {
    try {
      PsmMachine.Factory fact = ReusableRepairerStateMachineFactory.class.newInstance();
      return fact.getMachine(getReusableRepairerActionsClass());
    } catch (Exception e) {
      String msg = "Can't create V3ReusableRepairer state machine";
      log.critical(msg, e);
      throw new RuntimeException(msg, e);
    }
  }

  /**
   * Release unneeded resources used by this object at the end of a poll.
   */
  public void release() {
    stateMachine = null;
  }

  // Non-static class
  public class UserData {
    private V3LcapMessage reqMsg = null;

    public LockssDaemon getDaemon() {
      return V3ReusableRepairer.this.daemon;
    }

    public PluginManager getPluginManager() {
      return getDaemon().getPluginManager();
    }

    public PollManager getPollManager() {
      return getDaemon().getPollManager();
    }

    public void setReqMsg(V3LcapMessage reqMsg) {
      this.reqMsg = reqMsg;
    }

    public void clearReqMsg() {
      this.reqMsg = null;
    }

    public V3LcapMessage getReqMsg() {
      return this.reqMsg;
    }
  }

}