/*
 * $Id: LcapV1Comm.java,v 1.1 2003-03-19 07:55:49 tal Exp $
 */

/*

Copyright (c) 2000-2002 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.protocol;
import java.io.*;
import java.net.*;
import java.util.*;
import org.lockss.app.*;
import org.lockss.util.*;
import org.lockss.daemon.*;
import org.lockss.poller.PollManager;
import org.apache.commons.collections.LRUMap;

// tk - beacon thread
// tk - decrement hop count
// tk - send() call from poller (multicast, unicast to each)
// tk - synchronization here and in PartnerList

/**
 * LcapV1Comm implements unicast routing parts of the LCAP V1 protocol.
 * Routing involves decisions about using unicast to supplement multicast,
 * including forwarding received unicast packets.
 */
public class LcapV1Comm extends BaseLockssManager {
  static final String PREFIX = Configuration.PREFIX + "comm.v1";
  static final String PARAM_PKTS_PER_INTERVAL = PREFIX + "packetsPerInterval";
  static final String PARAM_PKT_INTERVAL = PREFIX + "packetInterval";
  static final String PARAM_PROB_PARTNER_ADD =
    PREFIX + "partnerAddProbability";

  static final int defaultPktsPerInterval = 40;
  static final int defaultPktInterval = 10000;

  static Logger log = Logger.getLogger("V1Comm");

  private LcapComm comm;
  private RateLimiter rateLimiter;
  private double probAddPartner;
  private PartnerList partnerList = new PartnerList();

  public void startService() {
    super.startService();
    comm = getDaemon().getCommManager();
    Configuration.registerConfigurationCallback(new Configuration.Callback() {
	public void configurationChanged(Configuration oldConfig,
					 Configuration newConfig,
					 Set changedKeys) {
	  setConfig(newConfig);
	}
      });
    comm.registerMessageHandler(LockssDatagram.PROTOCOL_LCAP,
				new LcapComm.MessageHandler() {
				    public void
				      handleMessage(LockssReceivedDatagram rd){
				      processIncomingMessage(rd);
				    }
				  });
//      singleton.start();
  }

  private void setConfig(Configuration config) {
    long interval = config.getLong(PARAM_PKTS_PER_INTERVAL,
				   defaultPktsPerInterval);
    int limit = config.getInt(PARAM_PKT_INTERVAL, defaultPktInterval);
    if (rateLimiter == null || rateLimiter.getInterval() != interval ||
	rateLimiter.getLimit() != limit) {
      rateLimiter = new RateLimiter(limit, interval);
    }
    probAddPartner = (((double)config.getInt(PARAM_PROB_PARTNER_ADD, 0))
		      / 100.0);
    partnerList.setConfig(config);
  }

  // handle received messge.  do unicast/multicast routing, then send to
  // poll manager
  void processIncomingMessage(LockssReceivedDatagram dg) {
    LcapMessage msg;
    log.debug("rcvd message: " + dg);
    byte[] msgBytes = dg.getData();
    try {
      msg = LcapMessage.decodeToMsg(msgBytes, dg.isMulticast());
    } catch (IOException e) {
      log.error("Couldn't decode incoming message", e);
      return;
    }
    routeIncomingMessage(dg, msg);
//        theManager.handleMessage(msg);
//      PollManager.getPollManager().handleMessage(msg);
  }


  // decide where to forward incoming message
  void routeIncomingMessage(LockssReceivedDatagram dg, LcapMessage msg) {
    InetAddress sender = dg.getSender();
    InetAddress originator = msg.getOriginAddr();
    if (isEligibleToForward(dg, msg)) {
      if (dg.isMulticast()) {
	partnerList.removePartner(sender);
	if (!sender.equals(originator)) {
	  partnerList.addPartner(originator, probAddPartner);
	}
	doUnicast(dg, msg, sender, originator);
      } else {
	partnerList.addPartner(sender, 1.0);
	partnerList.addPartner(originator, probAddPartner);
	doMulticast(dg, msg);
	doUnicast(dg, msg, sender, originator);
      }
    }
    partnerList.packetReceivedFrom(sender);
  }


  boolean isEligibleToForward(LockssReceivedDatagram dg, LcapMessage msg) {
    // Don't forward if ...
    if (msg.getHopCount() == 0) {	// forwarded enough times already
      return false;
    }
    if (isUnicastOpcode(msg)) {		// is verify poll message
      return false;
    }
    if (didISend(dg, msg)) {		// was sent be me
      return false;
    }

    if (!rateLimiter.isEventOk()) {	// exceeded max send packet rate
      return false;
    }
//      if (msg.getStopTime() >= TimeBase.nowMs()) { // poll has ended
//        return false;
//      }

    return true;
  }

  boolean isUnicastOpcode(LcapMessage msg) {
    switch (msg.getOpcode()) {
    case LcapMessage.VERIFY_POLL_REQ:
    case LcapMessage.VERIFY_POLL_REP:
      return true;
    default:
      return false;
    }
  }

  void doUnicast(LockssDatagram dg, LcapMessage msg,
		 InetAddress sender, InetAddress originator) {
    Set partners = partnerList.getPartners();
    if (partners.contains(getLocalIP())) {
      log.warning("Local IP found in partner list: " + getLocalIP());
    }
    for (Iterator iter = partners.iterator(); iter.hasNext(); ) {
      InetAddress part = (InetAddress)iter.next();
      if (!(part.equals(sender) || part.equals(originator))) {
	try {
	  comm.sendTo(dg, part);
	} catch (IOException e) {
	  // tk - remove partner here?
	}
      }
    }
  }

  // tk - ok to send same packet here and in unicast, repeatedly?
  void doMulticast(LockssDatagram dg, LcapMessage msg) {
    try {
      comm.send(dg, null);
    } catch (IOException e) {
      // tk - what to do here?
    }
  }


  // tk - to be implemented

  InetAddress getLocalIP() {
    return null;
  }

  // Should this test originator or sender?
  // Is it enough to test that against the local identity's ip, or should
  // we check all local IP addrs?
  boolean didISend(LockssReceivedDatagram dg, LcapMessage msg) {
    return false;
  }

}
