/*
 * $Id: LcapDatagramRouter.java,v 1.2 2004-09-22 19:13:03 dshr Exp $
 */

/*

Copyright (c) 2000-2003 Board of Trustees of Leland Stanford Jr. University,
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
import org.mortbay.util.B64Code;

// tk - synchronization here and in PartnerList

/**
 * LcapDatagramRouter implements unicast routing parts of the LCAP V1 protocol.
 * Routing involves decisions about using unicast to supplement multicast,
 * including forwarding received unicast packets.
 */
public class LcapDatagramRouter
  extends BaseLockssDaemonManager implements ConfigurableManager {

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
  static final String PARAM_DUP_MSG_HASH_SIZE = PREFIX + "dupMsgHashSize";

  static final int DEFAULT_ORIG_PKTS_PER_INTERVAL = 40;
  static final long DEFAULT_ORIG_PKT_INTERVAL = 8 * Constants.MINUTE;
  static final int DEFAULT_FWD_PKTS_PER_INTERVAL = 40;
  static final long DEFAULT_FWD_PKT_INTERVAL = 10 * Constants.SECOND;
  static final double DEFAULT_PROB_PARTNER_ADD = 0.5;
  static final int DEFAULT_DUP_MSG_HASH_SIZE = 100;
  static final int DEFAULT_INITIAL_HOPCOUNT = 2;
  static final int DEFAULT_BEACON_INTERVAL = 0;

  static final String PRIORITY_PARAM_BEACON = "Beacon";
  static final int PRIORITY_DEFAULT_BEACON = -1;

  static Logger log = Logger.getLogger("Router");

  private LcapDatagramComm comm;
  private PollManager pollMgr;
  private IdentityManager idMgr;
  private RateLimiter fwdRateLimiter;
  private RateLimiter origRateLimiter;
  private float probAddPartner;
  private long beaconInterval = 0;
  private int initialHopCount = LcapMessage.MAX_HOP_COUNT_LIMIT;

  private Deadline beaconDeadline = Deadline.at(TimeBase.MAX);;
  private PartnerList partnerList;
  private List messageHandlers = new ArrayList();
  private LRUMap recentVerifiers = new LRUMap(DEFAULT_DUP_MSG_HASH_SIZE);
  private Object verObj = new Object();

  public void startService() {
    super.startService();
    LockssDaemon daemon = getDaemon();
    comm = daemon.getDatagramCommManager();
    idMgr = daemon.getIdentityManager();
    pollMgr = daemon.getPollManager();
    partnerList = new PartnerList(idMgr);
    resetConfig();
    comm.registerMessageHandler(LockssDatagram.PROTOCOL_LCAP,
				new LcapDatagramComm.MessageHandler() {
				    public void
				      handleMessage(LockssReceivedDatagram rd){
				      processIncomingMessage(rd);
				    }
				  });
    startBeacon();
  }

  public void stopService() {
    stopBeacon();
    comm.unregisterMessageHandler(LockssDatagram.PROTOCOL_LCAP);
    super.stopService();
  }

  public void setConfig(Configuration config, Configuration oldConfig,
			Configuration.Differences changedKeys) {
    fwdRateLimiter =
      RateLimiter.getConfiguredRateLimiter(config, fwdRateLimiter,
					   PARAM_FWD_PKTS_PER_INTERVAL,
					   DEFAULT_FWD_PKTS_PER_INTERVAL,
					   PARAM_FWD_PKT_INTERVAL,
					   DEFAULT_FWD_PKT_INTERVAL);
    origRateLimiter =
      RateLimiter.getConfiguredRateLimiter(config, origRateLimiter,
					   PARAM_ORIG_PKTS_PER_INTERVAL,
					   DEFAULT_ORIG_PKTS_PER_INTERVAL,
					   PARAM_ORIG_PKT_INTERVAL,
					   DEFAULT_ORIG_PKT_INTERVAL);
    if (changedKeys.contains(PARAM_BEACON_INTERVAL)) {
      beaconInterval = config.getTimeInterval(PARAM_BEACON_INTERVAL,
					      DEFAULT_BEACON_INTERVAL);
      startBeacon();
    }
    probAddPartner = config.getPercentage(PARAM_PROB_PARTNER_ADD,
					  DEFAULT_PROB_PARTNER_ADD);
    initialHopCount =
      config.getInt(PARAM_INITIAL_HOPCOUNT, DEFAULT_INITIAL_HOPCOUNT);

    int dupSize =
      config.getInt(PARAM_DUP_MSG_HASH_SIZE, DEFAULT_DUP_MSG_HASH_SIZE);
    if (dupSize != recentVerifiers.getMaximumSize()) {
      recentVerifiers.setMaximumSize(dupSize);
    }

    if (partnerList != null) {
      partnerList.setConfig(config, oldConfig, changedKeys);
    }
  }

  /** Multicast a message to all caches holding the ArchivalUnit.  All
   * messages originated by this cache go through either this or {@link
   * #sendTo(LcapMessage, ArchivalUnit, PeerIdentity)}.
   * @param msg the message to send
   * @param au archival unit for which this message is relevant.  Used to
   * determine which multicast socket/port to send to.
   * @throws IOException if message couldn't be sent
   */
  public void send(LcapMessage msg, ArchivalUnit au) throws IOException {
    msg.setHopCount(initialHopCount);
    log.debug2("send(" + msg + ")");
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
  public void sendTo(LcapMessage msg, ArchivalUnit au, PeerIdentity id)
      throws IOException {
    msg.setHopCount(initialHopCount);
    log.debug2("sendTo(" + msg + ", " + id + ")");
    LockssDatagram dg = new LockssDatagram(LockssDatagram.PROTOCOL_LCAP,
					   msg.encodeMsg());
    comm.sendTo(dg, au, id);
    updateBeacon();
    origRateLimiter.event();
  }

  // handle received message.  do unicast/multicast routing, then pass msg
  // to handlers
  void processIncomingMessage(LockssReceivedDatagram dg) {
    LcapMessage msg;
    log.debug2("rcvd message: " + dg);
    byte[] msgBytes = dg.getData();
    try {
      msg = LcapMessage.decodeToMsg(msgBytes, dg.isMulticast());
    } catch (IOException e) {
      // XXX move the constants to IdentityManager
      PeerIdentity pid = idMgr.ipAddrToPeerIdentity(dg.getSender());
      idEvent(pid, LcapIdentity.EVENT_ERRPKT, null);
      log.error("Couldn't decode incoming message", e);
      return;
    }
    if (!isDuplicate(dg, msg)) {
      routeIncomingMessage(dg, msg);
      // if not a no-op, give to incoming message handlers
      if (!msg.isNoOp()) {
	runHandlers(msg);
      }
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

  boolean isDuplicate(LockssReceivedDatagram dg, LcapMessage msg) {
    String verifier = String.valueOf(B64Code.encode(msg.getVerifier()));
    if (recentVerifiers.put(verifier, verObj) != null) {
      log.debug2("Discarding dup from " + dg.getSender() + ": " + msg);
      PeerIdentity pid = idMgr.ipAddrToPeerIdentity(dg.getSender());
      idEvent(pid, LcapIdentity.EVENT_DUPLICATE, msg);
      return true;
    }
    return false;
  }

  // decide where to forward incoming message
  void routeIncomingMessage(LockssReceivedDatagram dg, LcapMessage msg) {
    PeerIdentity senderID = idMgr.ipAddrToPeerIdentity(dg.getSender());
    PeerIdentity originatorID = msg.getOriginatorID();
    idEvent(originatorID, LcapIdentity.EVENT_ORIG, msg);
      if (!msg.isNoOp()) {
	idEvent(originatorID, LcapIdentity.EVENT_ORIG_OP, msg);
      }
      if (senderID == originatorID) {
	idEvent(senderID, LcapIdentity.EVENT_SEND_ORIG, msg);
      } else {
	idEvent(senderID, LcapIdentity.EVENT_SEND_FWD, msg);
      }
      log.debug2("incoming orig: " + originatorID + " , sender: " + senderID);
      if (isEligibleToForward(dg, msg)) {
	//XXX need to clone message here to avoid overwrite problems
	msg.setHopCount(msg.getHopCount() - 1);
	try {
	  LockssDatagram fwddg = new LockssDatagram(dg.getProtocol(),
						  msg.encodeMsg());
	  if (dg.isMulticast()) {
	    partnerList.multicastPacketReceivedFrom(senderID);
	    if (senderID != originatorID) {
	      partnerList.addPartner(originatorID, probAddPartner);
	    }
	    doUnicast(fwddg, fwdRateLimiter, senderID, originatorID);
	  } else {
	    partnerList.addPartner(senderID, 1.0);
	    if (senderID != originatorID) {
	      partnerList.addPartner(originatorID, probAddPartner);
	    }
	    doMulticast(fwddg, fwdRateLimiter, null);
	    doUnicast(fwddg, fwdRateLimiter, senderID, originatorID);
	  }
	} catch (IOException e) {
	  log.warning("Couldn't forward message", e);
	}
      }
  }

  void idEvent(PeerIdentity pid, int event, LcapMessage msg) {
    idMgr.rememberEvent(pid, event, msg);
  }

  boolean isEligibleToForward(LockssReceivedDatagram dg, LcapMessage msg) {
    // Don't forward if ...
    if (msg.getHopCount() <= 0) {	// forwarded enough times already
      log.debug3("Not forwarding, hopcount = 0");
      return false;
    }
    if (isUnicastOpcode(msg)) {
      log.debug3("Not forwarding, is unicast opcode");
      return false;
    }
    if (didIOriginateOrSend(dg, msg)) {
      log.debug3("Not forwarding, I sent it");
      return false;
    }

    if (!fwdRateLimiter.isEventOk()) {	// exceeded max send packet rate
      log.warning("Not forwarding, forwarding rate exceeded");
      return false;
    }

    if (!msg.isNoOp() && msg.getStopTime() <= TimeBase.nowMs()) {
      // poll has ended
      log.debug3("Not forwarding, poll over");
      return false;
    }
    log.debug3("Forwarding msg");

    return true;
  }

  boolean isUnicastOpcode(LcapMessage msg) {
    return msg.isVerifyPoll();
  }

  // true if either the packet's source address is one of my interfaces,
  // or if I am (my identity is) the originator
  boolean didIOriginateOrSend(LockssReceivedDatagram dg, LcapMessage msg) {
    if (idMgr.isLocalIdentity(msg.getOriginatorID())) {
      return true;
    }
    return comm.didISend(dg);
  }

  /** Unicast to each of our partners.  Don't send the message to either
   * the sender or the originator of the message.  Either or both of sender and
   * originator may be null to disable this test.
   */
  void doUnicast(LockssDatagram dg, RateLimiter limiter,
		 PeerIdentity senderID, PeerIdentity originatorID) {
    Collection partners = partnerList.getPartners();
    for (Iterator iter = partners.iterator(); iter.hasNext(); ) {
      PeerIdentity part = (PeerIdentity)iter.next();
      // *** Either or both of sender and originator may be null here,
      //     so equals() will be false. ***
      if (!(part == senderID || part == originatorID)) {
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

  void sendNoOp() {
    try {
      LcapMessage noOp =
	LcapMessage.makeNoOpMsg(idMgr.getLocalPeerIdentity(),
				pollMgr.generateRandomBytes());
      log.debug2("noop: " + noOp);
      send(noOp, null);
    } catch (IOException e) {
      log.warning("Couldn't send NoOp message", e);
    }
  }

  private BeaconThread beaconThread;

  private void updateBeacon() {
    if (beaconInterval != 0) {
      beaconDeadline.expireIn(beaconInterval);
    }
  }

  private void startBeacon() {
    if (beaconInterval != 0 && isDaemonInited()) {
      ensureBeaconRunning();
      updateBeacon();
    } else {
      stopBeacon();
    }
  }

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
      beaconThread.waitRunning();
    }
  }

  // Beacon thread
  private class BeaconThread extends LockssThread {
    private boolean goOn = false;

    private BeaconThread(String name) {
      super(name);
    }

    public void lockssRun() {
      setPriority(PRIORITY_PARAM_BEACON, PRIORITY_DEFAULT_BEACON);
      goOn = true;
      nowRunning();

      while (goOn) {
	try {
	  beaconDeadline.sleep();
	  if (goOn && beaconDeadline.expired()) {
	    if (origRateLimiter.isEventOk()) {
	      log.debug("Beacon send");
	      sendNoOp();
	    } else {
	      log.warning("Message originate rate exceeded, not sending noop");
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
   * Register a {@link LcapDatagramRouter.MessageHandler}, which will be called
   * whenever an LcapMessage is received.
   * @param handler MessageHandler to add
   */
  public void registerMessageHandler(MessageHandler handler) {
    if (!messageHandlers.contains(handler)) {
      messageHandlers.add(handler);
    }
  }

  /**
   * Unregister a {@link LcapDatagramRouter.MessageHandler}.
   * @param handler MessageHandler to remove
   */
  public void unregisterMessageHandler(MessageHandler handler) {
    messageHandlers.remove(handler);
  }

  /**
   * The LcapDatagramRouter.MessageHandler interface defines the
   * callback registered by clients of {@link LcapDatagramRouter} who want to process
   * incoming LcapMessages.
   */
  public interface MessageHandler {
    /**
     * Callback used to inform clients that an LcapMessage has been received.
     * @param msg the received LcapMessage
     * @see LcapDatagramRouter#registerMessageHandler */
    public void handleMessage(LcapMessage msg);
  }
}
