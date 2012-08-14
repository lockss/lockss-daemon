/*
 * $Id: RepairerActions.java,v 1.1 2012-08-14 21:27:13 barry409 Exp $
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
import java.util.*;
import java.security.*;
import org.apache.commons.collections.CollectionUtils;

import org.lockss.config.ConfigManager;
import org.lockss.poller.PollManager;
import org.lockss.poller.PollManager.EventCtr;
import org.lockss.plugin.*;
import org.lockss.protocol.*;
import org.lockss.protocol.psm.*;
import org.lockss.util.*;

public class RepairerActions {
  private static final Logger log = Logger.getLogger("RepairerActions");

  static PollManager getPollManager(VoterUserData ud) {
    return ud.getVoter().getPollManager();
  }

  @ReturnEvents("evtRepairRequestOk,evtNoSuchRepair")
  public static PsmEvent handleReceiveRepairRequest(PsmMsgEvent evt,
                                                    PsmInterp interp) {
    // todo(bhayes): This was copied out of V3Voter, and slightly
    // modified. The full VoterUserData is much more than is needed
    // here.
    VoterUserData ud = getUserData(interp);
    if (!ud.isPollActive()) return V3Events.evtNoSuchRepair;
    V3Voter voter = ud.getVoter();
    V3LcapMessage msg = (V3LcapMessage)evt.getMessage();
    String targetUrl = msg.getTargetUrl();
    // Unlike VoterActions, do not test cus.containsUrl(targetUrl).
    if (getPollManager(ud).getRepairPolicy().serveRepair(
	  msg.getOriginatorId(), voter.getAu(), targetUrl)) {
      // I have this repair and I'm willing to serve it.
      log.debug2("Accepting repair request from " + ud.getPollerId() +
                 " for URL: " + targetUrl);
      ud.setRepairTarget(targetUrl);
      return V3Events.evtRepairRequestOk;
    } else {
      // I don't have this repair
      log.error("No repair available to serve for URL: " + targetUrl);
      return V3Events.evtNoSuchRepair;
    }
  }

  @ReturnEvents("evtOk,evtError")
  @SendMessages("msgRepair")
  public static PsmEvent handleSendRepair(PsmEvent evt, PsmInterp interp) {
    VoterUserData ud = getUserData(interp);
    if (!ud.isPollActive()) return V3Events.evtError;
    log.debug2("Sending repair to " + ud.getPollerId() + " for URL : " +
               ud.getRepairTarget());
    CachedUrl cu = null;
    try {
      V3LcapMessage msg = ud.makeMessage(V3LcapMessage.MSG_REPAIR_REP);
      ArchivalUnit au = ud.getCachedUrlSet().getArchivalUnit();
      cu = au.makeCachedUrl(ud.getRepairTarget());
      msg.setTargetUrl(ud.getRepairTarget());
      msg.setRepairDataLength(cu.getContentSize());
      msg.setRepairProps(cu.getProperties());
      msg.setInputStream(cu.getUnfilteredInputStream());
      msg.setExpiration(ud.getDeadline());
      msg.setRetryMax(1);
      ud.sendMessageTo(msg, ud.getPollerId());
    } catch (IOException ex) {
      log.error("Unable to send message: ", ex);
      return V3Events.evtError;
    } finally {
      AuUtil.safeRelease(cu);
    }
    return V3Events.evtOk;
  }

  @ReturnEvents("evtOk")
  public static PsmEvent closeRepairer(PsmEvent evt, PsmInterp interp) {
    VoterUserData ud = getUserData(interp);
    ud.getVoter().stopPoll();
    return V3Events.evtOk;
  }

  @ReturnEvents("evtOk")
  public static PsmEvent handleError(PsmEvent evt, PsmInterp interp) {
    // XXX: Implement.
    return V3Events.evtOk;
  }

  private static VoterUserData getUserData(PsmInterp interp) {
    return (VoterUserData)interp.getUserData();
  }
}
