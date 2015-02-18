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

import java.io.IOException;
import java.io.File;
import java.util.*;
import java.security.*;
import org.apache.commons.collections.CollectionUtils;

import org.lockss.config.ConfigManager;
import org.lockss.poller.PollManager;
import org.lockss.poller.PollManager.EventCtr;
import org.lockss.app.LockssDaemon;
import org.lockss.plugin.*;
import org.lockss.poller.RepairPolicy;
import org.lockss.protocol.*;
import org.lockss.protocol.psm.*;
import org.lockss.util.*;

public class ReusableRepairerActions {
  private static final Logger log = Logger.getLogger("ReusableRepairerActions");

  @ReturnEvents("evtRepairRequestOk,evtNoSuchRepair")
  public static PsmEvent handleReceiveRepairRequest(PsmMsgEvent evt,
                                                    PsmInterp interp) {
    V3ReusableRepairer.UserData ud = getUserData(interp);

    V3LcapMessage reqMsg = (V3LcapMessage)evt.getMessage();
    PeerIdentity pid = reqMsg.getOriginatorId();
    ArchivalUnit au = ud.getPluginManager().getAuFromId(reqMsg.getArchivalId());
    String targetUrl = reqMsg.getTargetUrl();
    RepairPolicy repairPolicy = ud.getPollManager().getRepairPolicy();
    if (repairPolicy.shouldServeRepair(pid, au, targetUrl)) {
      // I have this repair and I'm willing to serve it.
      log.debug2("Accepting repair request from " + pid + " for URL: " +
		 targetUrl);
      ud.setReqMsg(reqMsg);
      return V3Events.evtRepairRequestOk;
    } else {
      // I don't have this repair
      log.error("No repair available to serve for URL: " + targetUrl);
      ud.clearReqMsg();
      return V3Events.evtNoSuchRepair;
    }
  }

  private static V3LcapMessage makeRepairMsg(V3LcapMessage reqMsg,
					     LockssDaemon daemon) {
    String auId = reqMsg.getArchivalId();
    String pollKey = reqMsg.getKey();
    String pluginVersion = reqMsg.getPluginVersion();
    byte[] pollerNonce = reqMsg.getPollerNonce();
    byte[] voterNonce = reqMsg.getVoterNonce();
    int opcode = V3LcapMessage.MSG_REPAIR_REP;
    int deadline = 0;
    IdentityManager idManager = daemon.getIdentityManager();
    PeerIdentity pid =
      idManager.getLocalPeerIdentity(reqMsg.getProtocolVersion());
    File messageDir = null;
    return new V3LcapMessage(auId, pollKey, pluginVersion,
			     pollerNonce, voterNonce, opcode,
			     deadline, pid, messageDir, daemon);
  }

  @ReturnEvents("evtOk,evtError")
  @SendMessages("msgRepair")
  public static PsmEvent handleSendRepair(PsmEvent evt, PsmInterp interp) {
    V3ReusableRepairer.UserData ud = getUserData(interp);
    V3LcapMessage reqMsg = ud.getReqMsg();
    PeerIdentity pid = reqMsg.getOriginatorId();
    ArchivalUnit au = ud.getPluginManager().getAuFromId(reqMsg.getArchivalId());
    String targetUrl = reqMsg.getTargetUrl();

    log.debug2("Sending repair to " + pid + " for URL : " + targetUrl);
    CachedUrl cu = null;
    try {
      V3LcapMessage repairMsg = makeRepairMsg(ud.getReqMsg(),
					      ud.getDaemon());
      cu = au.makeCachedUrl(targetUrl);
      repairMsg.setTargetUrl(targetUrl);
      repairMsg.setRepairDataLength(cu.getContentSize());
      repairMsg.setRepairProps(cu.getProperties());
      repairMsg.setInputStream(cu.getUnfilteredInputStream());
      // todo(bhayes): Should there be an expiration?
      repairMsg.setRetryMax(1);
      ud.getPollManager().sendMessageTo(repairMsg, pid);
    } catch (IOException ex) {
      log.error("Unable to send message: ", ex);
      return V3Events.evtError;
    } finally {
      AuUtil.safeRelease(cu);
    }
    return V3Events.evtOk;
  }

  @ReturnEvents("evtOk")
  public static PsmEvent handleError(PsmEvent evt, PsmInterp interp) {
    // XXX: Implement.
    // todo(bhayes): can this force state back to WaitForRequest?
    return V3Events.evtOk;
  }

  private static V3ReusableRepairer.UserData getUserData(PsmInterp interp) {
    return (V3ReusableRepairer.UserData)interp.getUserData();
  }
}
