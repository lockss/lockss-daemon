/*
 * $Id: LcapRouter.java,v 1.15 2003-04-03 02:28:48 tal Exp $
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
import org.lockss.plugin.*;
import org.lockss.poller.PollManager;
import org.apache.commons.collections.LRUMap;

// tk - synchronization here and in PartnerList

/**
 * LcapRouter implements unicast routing parts of the LCAP V1 protocol.
 * Routing involves decisions about using unicast to supplement multicast,
 * including forwarding received unicast packets.
 */
public class LcapRouter extends BaseLockssManager {
  static final String PREFIX = Configuration.PREFIX + "comm.router.";
  static final String ORIGRATE_PREFIX = PREFIX + "maxOriginateRate.";
  static final String PARAM_ORIG_PKTS_PER_INTERVAL = ORIGRATE_PREFIX+"packets";
  static final String PARAM_ORIG_PKT_INTERVAL = ORIGRATE_PREFIX + "interval";
  static final String FWDRATE_PREFIX = PREFIX + "maxForwardRate.";
  static final String PARAM_FWD_PKTS_PER_INTERVAL = FWDRATE_PREFIX + "packets";
  static final String PARAM_FWD_PKT_INTERVAL = FWDRATE_PREFIX + "interval";
  static final String PARAM_BEACON_INTERVAL = PREFIX + "beacon.interval";
  static final String PARAM_INITIAL_HOPCOUNT = PREFIX + "maxHopCount";
  static final String PARAM_PROB_PARTNER_ADD =
    PREFIX + "partnerAddProbability";

  static final String PARAM_LOCAL_IPS =
    Configuration.PREFIX + "platform.interfaceAddresses";

  static final int DEFAULT_ORIG_PKTS_PER_INTERVAL = 40;
  static final long DEFAULT_ORIG_PKT_INTERVAL = 10 * Constants.SECOND;
  static final int DEFAULT_FWD_PKTS_PER_INTERVAL = 40;
  static final long DEFAULT_FWD_PKT_INTERVAL = 10 * Constants.SECOND;
  static final double DEFAULT_PROB_PARTNER_ADD = 0.5;

  static Logger log = Logger.getLogger("Router");

  private LcapComm comm;
  private IdentityManager idMgr;
  private RateLimiter fwdRateLimiter;
  private RateLimiter origRateLimiter;
  private float probAddPartner;
  private List localInterfaces;
  private long beaconInterval = 0;
  private int initialHopCount = LcapMessage.MAX_HOP_COUNT_LIMIT;

  private Deadline beaconDeadline = Deadline.at(TimeBase.MAX);;
  private PartnerList partnerList = new PartnerList();
  private List messageHandlers = new ArrayList();

  public void startService() {
    super.startService();
    comm = getDaemon().getCommManager();
    idMgr = getDaemon().getIdentityManager();
    registerDefaultConfigCallback();
    comm.registerMessageHandler(LockssDatagram.PROTOCOL_LCAP,
				new LcapComm.MessageHandler() {
				    public void
				      handleMessage(LockssReceivedDatagram rd){
				      processIncomingMessage(rd);
				    }
				  });
  }

  public void stopService() {
    stopBeacon();
    super.stopService();
  }

  private RateLimiter getRateLimiter(Configuration config,
				     RateLimiter currentLimiter,
				     String pktsParam, String intervalParam,
				     int pktsDefault, long intervalDefault) {
    int pkts = config.getInt(pktsParam, pktsDefault);
    long interval = config.getTimeInterval(intervalParam, intervalDefault);
    if (currentLimiter == null || currentLimiter.getInterval() != interval ||
	currentLimiter.getLimit() != pkts) {
      return new RateLimiter(pkts, interval);
    } else {
      return currentLimiter;
    }
  }

  protected void setConfig(Configuration config, Configuration oldConfig,
			   Set changedKeys) {
    fwdRateLimiter =
      getRateLimiter(config, fwdRateLimiter,
		     PARAM_FWD_PKTS_PER_INTERVAL, PARAM_FWD_PKT_INTERVAL,
		     DEFAULT_FWD_PKTS_PER_INTERVAL, DEFAULT_FWD_PKT_INTERVAL);
    origRateLimiter =
      getRateLimiter(config, origRateLimiter,
		     PARAM_ORIG_PKTS_PER_INTERVAL, PARAM_ORIG_PKT_INTERVAL,
		     DEFAULT_ORIG_PKTS_PER_INTERVAL, DEFAULT_ORIG_PKT_INTERVAL);
    if (changedKeys.contains(PARAM_BEACON_INTERVAL)) {
      beaconInterval = config.getTimeInterval(PARAM_BEACON_INTERVAL, 0);
      if (beaconInterval != 0) {
	ensureBeaconRunning();
	updateBeacon();
      } else {
	stopBeacon();
      }
    }
    probAddPartner = config.getPercentage(PARAM_PROB_PARTNER_ADD,
					  DEFAULT_PROB_PARTNER_ADD);
    initialHopCount =
      config.getInt(PARAM_INITIAL_HOPCOUNT, LcapMessage.MAX_HOP_COUNT_LIMIT);

    partnerList.setConfig(config);

    // make list of InetAddresses of local interfaces
    if (localInterfaces == null || changedKeys.contains(PARAM_LOCAL_IPS)) {
      String s = config.get(PARAM_LOCAL_IPS, "");
      List ipStrings = StringUtil.breakAt(s, ';');
      localInterfaces = new ArrayList();
      for (Iterator iter = ipStrings.iterator(); iter.hasNext(); ) {
	String ip = (String)iter.next();
	try {
	  InetAddress inet = InetAddress.getByName(ip);
	  localInterfaces.add(inet);
	} catch (UnknownHostException e) {
	  log.warning("Couldn't parse local interface IP address: " + ip);
	}
      }
      // if not specified, assume single interface is same as local identity
      if (localInterfaces.isEmpty()) {
	localInterfaces.add(getLocalIdentityAddr());
      }
    }
  }

  /** Multicast a message to all caches holding the ArchivalUnit.  All
   * messages originated by this cache go through either this or {@link
   * #sendTo(LcapMessage, ArchivalUnit, LcapIdentity)}.
   * @param msg the message to send
   * @param au archival unit for which this message is relevant.  Used to
   * determine which multicast socket/port to send to.
   * @throws IOException if message couldn't be sent
   */
  public void send(LcapMessage msg, ArchivalUnit au) throws IOException {
    msg.setHopCount(initialHopCount);
    log.debug("send(" + msg + ")");
    LockssDatagram dg = new LockssDatagram(LockssDatagram.PROTOCOL_LCAP,
					   msg.encodeMsg());
    doMulticast(dg, origRateLimiter, au);
    doUnicast(dg, origRateLimiter, null, null);
  }

  /** Unicast a message to a single cache.  All
   * messages originated by this cache go through either this or {@link
   * #send(LcapMessage, ArchivalUnit)}.
   * @param msg the message to send
   * @param au archival unit for which this message is relevant.  Used to
   * determine which multicast socket/port to send to.
   * @param id the identity of the cache to which to send the message
   * @throws IOException if message couldn't be sent
   */
  public void sendTo(LcapMessage msg, ArchivalUnit au, LcapIdentity id)
      throws IOException {
    msg.setHopCount(initialHopCount);
    log.debug("sendTo(" + msg + ", " + id + ")");
    LockssDatagram dg = new LockssDatagram(LockssDatagram.PROTOCOL_LCAP,
					   msg.encodeMsg());
    comm.sendTo(dg, au, id);
    updateBeacon();
    origRateLimiter.event();
  }

  private void updateBeacon() {
    if (beaconInterval != 0) {
      beaconDeadline.expireIn(beaconInterval);
    }
  }

  // handle received message.  do unicast/multicast routing, then pass msg
  // to handlers
  void processIncomingMessage(LockssReceivedDatagram dg) {
    LcapMessage msg;
    log.debug("rcvd message: " + dg);
    byte[] msgBytes = dg.getData();
    try {
      msg = LcapMessage.decodeToMsg(msgBytes, dg.isMulticast());
    } catch (IOException e) {
      idEvent(dg.getSender(), LcapIdentity.EVENT_ERRPKT, null);
      log.error("Couldn't decode incoming message", e);
      return;
    }
    routeIncomingMessage(dg, msg);
    // if not a no-op, give to incoming message handlers
    if (!msg.isNoOp()) {
      runHandlers(msg);
    }
  }


  private void runHandlers(LcapMessage msg) {
    for (Iterator iter = messageHandlers.iterator(); iter.hasNext();) {
      runHandler((MessageHandler)iter.next(), msg);
    }
  }

  private void runHandler(MessageHandler handler, LcapMessage msg) {
    try {
      handler.handleMessage(msg);
    } catch (Exception e) {
      log.error("callback threw", e);
    }
  }

  // decide where to forward incoming message
  void routeIncomingMessage(LockssReceivedDatagram dg, LcapMessage msg) {
    InetAddress sender = dg.getSender();
    InetAddress originator = msg.getOriginAddr();
    idEvent(originator, LcapIdentity.EVENT_ORIG, msg);
    if (!msg.isNoOp()) {
      idEvent(originator, LcapIdentity.EVENT_ORIG_OP, msg);
    }
    if (!sender.equals(originator)) {
      idEvent(sender, LcapIdentity.EVENT_SEND_ORIG, msg);
    } else {
      idEvent(sender, LcapIdentity.EVENT_SEND_FWD, msg);
    }
    log.debug2("incoming orig: " + originator + " , sender: " + sender);
    if (isEligibleToForward(dg, msg)) {
      msg.setHopCount(msg.getHopCount() - 1);
      if (dg.isMulticast()) {
	partnerList.multicastPacketReceivedFrom(sender);
	if (!sender.equals(originator)) {
	  partnerList.addPartner(originator, probAddPartner);
	}
	doUnicast(dg, fwdRateLimiter, sender, originator);
      } else {
	partnerList.addPartner(sender, 1.0);
	if (!sender.equals(originator)) {
	  partnerList.addPartner(originator, probAddPartner);
	}
	doMulticast(dg, fwdRateLimiter, null);
	doUnicast(dg, fwdRateLimiter, sender, originator);
      }
    }
  }

  void idEvent(InetAddress addr, int event, LcapMessage msg) {
    LcapIdentity id = idMgr.findIdentity(addr);
    if (id != null) {
      id.rememberEvent(event, msg);
    }
  }

  boolean isEligibleToForward(LockssReceivedDatagram dg, LcapMessage msg) {
    // Don't forward if ...
    if (msg.getHopCount() <= 0) {	// forwarded enough times already
      return false;
    }
    if (isUnicastOpcode(msg)) {
      return false;
    }
    if (didISend(dg, msg)) {
      return false;
    }

    if (!fwdRateLimiter.isEventOk()) {	// exceeded max send packet rate
      log.debug("Message forwarding rate exceeded, not forwarding" + msg);
      return false;
    }
     if (msg.getStopTime() >= TimeBase.nowMs()) { // poll has ended
       return false;
     }

    return true;
  }

  boolean isUnicastOpcode(LcapMessage msg) {
    return msg.isVerifyPoll();
  }

  // true if either the packet's source address is one of my interfaces,
  // or if I am (my identity is) the originator
  boolean didISend(LockssReceivedDatagram dg, LcapMessage msg) {
    if (msg.getOriginAddr().equals(getLocalIdentityAddr())) {
      return true;
    }
    if (localInterfaces != null) {
      InetAddress sender = dg.getSender();
      for (Iterator iter = localInterfaces.iterator(); iter.hasNext(); ) {
	if (sender.equals((InetAddress)iter.next())) {
	  return true;
	}
      }
    }
    return false;
  }

  /** Unicast to each of our partners.  Don't send the message to either
   * the sender or the originator of the message.  Either or both of sender and
   * originator may be null to disable this test.
   */
  void doUnicast(LockssDatagram dg, RateLimiter limiter,
		 InetAddress sender, InetAddress originator) {
    Collection partners = partnerList.getPartners();
    if (partners.contains(getLocalIdentityAddr())) {
      log.warning("Local IP found in partner list: " + getLocalIdentityAddr());
      partners.remove(getLocalIdentityAddr());
    }
    for (Iterator iter = partners.iterator(); iter.hasNext(); ) {
      InetAddress part = (InetAddress)iter.next();
      // *** Either or both of sender and originator may be null here,
      //     so equals() will be false. ***
      if (!(part.equals(sender) || part.equals(originator))) {
	try {
	  comm.sendTo(dg, part);
	  updateBeacon();
	  limiter.event();
	} catch (IOException e) {
	  partnerList.removePartner(part);
	}
      }
    }
  }

  // tk - ok to send same packet here and in unicast, repeatedly?
  void doMulticast(LockssDatagram dg, RateLimiter limiter, ArchivalUnit au) {
    try {
      comm.send(dg, au);
      updateBeacon();
      limiter.event();
    } catch (IOException e) {
      // tk - what to do here?
    }
  }

  InetAddress getLocalIdentityAddr() {
    return idMgr.getLocalIdentity().getAddress();
  }

  void sendNoOp() {
    try {
      LcapMessage noOp = LcapMessage.makeNoOpMsg(idMgr.getLocalIdentity());
      send(noOp, null);
    } catch (IOException e) {
      log.warning("Couldn't send NoOp message", e);
    }
  }

  private BeaconThread beaconThread;

  synchronized void stopBeacon() {
    if (beaconThread != null) {
      log.info("Stopping beacon");
      beaconThread.stopBeacon();
      beaconThread = null;
    }
  }

  // tk add watchdog
  synchronized void ensureBeaconRunning() {
    if (beaconThread == null) {
      log.info("Starting beacon");
      beaconThread = new BeaconThread("Beacon");
      beaconThread.start();
    }
  }

  // Beacon thread
  private class BeaconThread extends Thread {
    private boolean goOn = false;

    private BeaconThread(String name) {
      super(name);
    }

    public void run() {
//       if (beaconPriority > 0) {
// 	Thread.currentThread().setPriority(beaconPriority);
//       }
      goOn = true;

      while (goOn) {
	try {
	  beaconDeadline.sleep();
	  if (goOn && beaconDeadline.expired()) {
	    if (origRateLimiter.isEventOk()) {
	      log.debug3("Beacon send");
	      sendNoOp();
	    } else {
	      log.debug("Message originate rate exceeded, not sending noop");
	    }
	    beaconDeadline.expireIn(beaconInterval);
	  }
	} catch (InterruptedException e) {
	  // no action - expected when stopping
	} catch (Exception e) {
	  log.error("Unexpected exception caught in Beacon thread", e);
	}
      }
      beaconThread = null;
    }

    private void stopBeacon() {
      goOn = false;
      beaconDeadline.expire();
    }
  }

  /**
   * Register a {@link LcapRouter.MessageHandler}, which will be called
   * whenever an LcapMessage is received.
   * @param handler MessageHandler to add
   */
  public void registerMessageHandler(MessageHandler handler) {
    if (!messageHandlers.contains(handler)) {
      messageHandlers.add(handler);
    }
  }

  /**
   * Unregister a {@link LcapRouter.MessageHandler}.
   * @param handler MessageHandler to remove
   */
  public void unregisterMessageHandler(MessageHandler handler) {
    messageHandlers.remove(handler);
  }

  /**
   * The LcapRouter.MessageHandler interface defines the
   * callback registered by clients of {@link LcapRouter} who want to process
   * incoming LcapMessages.
   */
  public interface MessageHandler {
    /**
     * Callback used to inform clients that an LcapMessage has been received.
     * @param msg the received LcapMessage
     * @see LcapRouter#registerMessageHandler */
    public void handleMessage(LcapMessage msg);
  }
}
