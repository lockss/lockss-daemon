/*
 * $Id: LcapRouter.java,v 1.7 2003-03-28 08:13:20 tal Exp $
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
  static final String SENDRATE_PREFIX = PREFIX + "maxSendRate.";
  static final String PARAM_PKTS_PER_INTERVAL = SENDRATE_PREFIX + "packets";
  static final String PARAM_PKT_INTERVAL = SENDRATE_PREFIX + "interval";
  static final String PARAM_BEACON_INTERVAL = PREFIX + "beacon.interval";
  static final String PARAM_INITIAL_HOPCOUNT = PREFIX + "maxHopCount";

  static final String PARAM_PROB_PARTNER_ADD =
    PREFIX + "partnerAddProbability";
  static final String PARAM_LOCAL_IPS =
    Configuration.PREFIX + "platform.interfaceAddresses";

  static final int defaultPktsPerInterval = 40;
  static final long defaultPktInterval = 10 * Constants.SECOND;

  static Logger log = Logger.getLogger("Router");

  private LcapComm comm;
  private IdentityManager idMgr;
  private RateLimiter rateLimiter;
  private double probAddPartner;
  private List localInterfaces;
  private long beaconInterval = 0;
  private int initialHopCount = LcapMessage.MAX_MAX_HOP_COUNT;
  private Deadline beaconDeadline = Deadline.at(TimeBase.NEVER);;
  private PartnerList partnerList = new PartnerList();
  private Configuration.Callback configCallback;
  private List messageHandlers = new ArrayList();

  public void startService() {
    super.startService();
    comm = getDaemon().getCommManager();
    idMgr = getDaemon().getIdentityManager();
    configCallback  = new Configuration.Callback() {
	public void configurationChanged(Configuration oldConfig,
					 Configuration newConfig,
					 Set changedKeys) {
	  setConfig(newConfig, changedKeys);
	}
      };
    Configuration.registerConfigurationCallback(configCallback);
    comm.registerMessageHandler(LockssDatagram.PROTOCOL_LCAP,
				new LcapComm.MessageHandler() {
				    public void
				      handleMessage(LockssReceivedDatagram rd){
				      processIncomingMessage(rd);
				    }
				  });
  }

  public void stopService() {
    Configuration.unregisterConfigurationCallback(configCallback);
    stopBeacon();
    super.stopService();
  }

  private void setConfig(Configuration config, Set changedKeys) {
    long interval = config.getTimeInterval(PARAM_PKT_INTERVAL,
					   defaultPktInterval);
    int limit = config.getInt(PARAM_PKTS_PER_INTERVAL, defaultPktsPerInterval);
    if (rateLimiter == null || rateLimiter.getInterval() != interval ||
	rateLimiter.getLimit() != limit) {
      rateLimiter = new RateLimiter(limit, interval);
    }
    if (changedKeys.contains(PARAM_BEACON_INTERVAL)) {
      beaconInterval = config.getTimeInterval(PARAM_BEACON_INTERVAL, 0);
      if (beaconInterval != 0) {
	ensureBeaconRunning();
      } else {
	stopBeacon();
      }
    }
    probAddPartner = (((double)config.getInt(PARAM_PROB_PARTNER_ADD, 0))
		      / 100.0);
    initialHopCount =
      config.getInt(PARAM_INITIAL_HOPCOUNT, LcapMessage.MAX_MAX_HOP_COUNT);

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

  /** Multicast a message to all caches holding the ArchivalUnit.
   * @param msg the message to send
   * @param au archival unit for which this message is relevant.  Used to
   * determine which multicast socket/port to send to.
   * @throws IOException
   */
  public void send(LcapMessage msg, ArchivalUnit au) throws IOException {
    msg.setHopCount(initialHopCount);
    log.debug("send(" + msg + ")");
    LockssDatagram dg = new LockssDatagram(LockssDatagram.PROTOCOL_LCAP,
					   msg.encodeMsg());
    updateBeacon();
    comm.send(dg, au);
    doUnicast(dg, null, null);
    rateLimiter.event();
  }

  /** Unicast a message to a single cache.
   * @param msg the message to send
   * @param au archival unit for which this message is relevant.  Used to
   * determine which multicast socket/port to send to.
   * @param id the identity of the cache to which to send the message
   * @throws IOException
   */
  public void sendTo(LcapMessage msg, ArchivalUnit au, LcapIdentity id)
      throws IOException {
    msg.setHopCount(initialHopCount);
    log.debug("sendTo(" + msg + ", " + id + ")");
    LockssDatagram dg = new LockssDatagram(LockssDatagram.PROTOCOL_LCAP,
					   msg.encodeMsg());
    updateBeacon();
    comm.sendTo(dg, au, id);
    rateLimiter.event();
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
    if (isEligibleToForward(dg, msg)) {
      msg.setHopCount(msg.getHopCount() - 1);
      if (dg.isMulticast()) {
	partnerList.multicastPacketReceivedFrom(sender);
	if (!sender.equals(originator)) {
	  partnerList.addPartner(originator, probAddPartner);
	}
	doUnicast(dg, sender, originator);
      } else {
	partnerList.addPartner(sender, 1.0);
	if (!sender.equals(originator)) {
	  partnerList.addPartner(originator, probAddPartner);
	}
	doMulticast(dg, msg);
	doUnicast(dg, sender, originator);
      }
    }
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
  void doUnicast(LockssDatagram dg,
		 InetAddress sender, InetAddress originator) {
    Set partners = partnerList.getPartners();
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

  InetAddress getLocalIdentityAddr() {
    return idMgr.getLocalIdentity().getAddress();
  }

  void sendNoop() {
    try {
      LcapMessage noop = LcapMessage.makeNoOpMsg(idMgr.getLocalIdentity());
      send(noop, null);
    } catch (IOException e) {
      log.warning("Couldn't send Noop message", e);
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
	    log.debug3("Beacon send");
	    sendNoop();
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

