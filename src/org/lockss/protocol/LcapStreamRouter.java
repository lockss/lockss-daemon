/*
 * $Id: LcapStreamRouter.java,v 1.1.2.2 2004-11-18 15:45:07 dshr Exp $
 */

/*

Copyright (c) 2004 Board of Trustees of Leland Stanford Jr. University,
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
import org.lockss.config.*;
import org.lockss.poller.PollManager;
import org.apache.commons.collections.LRUMap;
import org.mortbay.util.B64Code;

/**
 * LcapStreamRouter is an analog of LcapDatagramRouter that layers on
 * top of LcapStreamComm to convert messages to and from byte arrays.
 */
public class LcapStreamRouter
  extends BaseLockssDaemonManager implements ConfigurableManager {

  static final String PREFIX = Configuration.PREFIX + "comm.router.";
  static final String ORIGRATE_PREFIX = PREFIX + "maxOriginateRate.";
  static final String PARAM_ORIG_PKTS_PER_INTERVAL = ORIGRATE_PREFIX+"packets";
  static final String PARAM_ORIG_PKT_INTERVAL = ORIGRATE_PREFIX + "interval";
  static final String PARAM_PROB_PARTNER_ADD =
    PREFIX + "partnerAddProbability";
  static final int DEFAULT_ORIG_PKTS_PER_INTERVAL = 40;
  static final long DEFAULT_ORIG_PKT_INTERVAL = 8 * Constants.MINUTE;
  static final double DEFAULT_PROB_PARTNER_ADD = 0.5;
  static final int DEFAULT_INITIAL_HOPCOUNT = 2;

  static Logger log = Logger.getLogger("StreamRouter");

  private LcapDatagramComm comm;
  private PollManager pollMgr;
  private IdentityManager idMgr;
  protected RateLimiter origRateLimiter;
  private float probAddPartner;
  private PartnerList partnerList;
  private List messageHandlers = new ArrayList();

  public void startService() {
    super.startService();
    LockssDaemon daemon = getDaemon();
    comm = daemon.getDatagramCommManager();
    idMgr = daemon.getIdentityManager();
    pollMgr = daemon.getPollManager();
    partnerList = new PartnerList(idMgr);
    resetConfig();
    if (false) {
      //  XXX need an analog of this for stream
      comm.registerMessageHandler(LockssDatagram.PROTOCOL_LCAP,
				  new LcapDatagramComm.MessageHandler() {
				    public void
				      handleMessage(LockssReceivedDatagram rd){
				      processIncomingMessage(rd);
				    }
				  });
    }
  }

  public void stopService() {
    if (false) {
      comm.unregisterMessageHandler(LockssDatagram.PROTOCOL_LCAP);
    }
    super.stopService();
  }

  public void setConfig(Configuration config, Configuration oldConfig,
			Configuration.Differences changedKeys) {
    origRateLimiter =
      RateLimiter.getConfiguredRateLimiter(config, origRateLimiter,
					   PARAM_ORIG_PKTS_PER_INTERVAL,
					   DEFAULT_ORIG_PKTS_PER_INTERVAL,
					   PARAM_ORIG_PKT_INTERVAL,
					   DEFAULT_ORIG_PKT_INTERVAL);
    probAddPartner = config.getPercentage(PARAM_PROB_PARTNER_ADD,
					  DEFAULT_PROB_PARTNER_ADD);
    if (partnerList != null) {
      partnerList.setConfig(config, oldConfig, changedKeys);
    }
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
  public void sendTo(V3LcapMessage msg, ArchivalUnit au, PeerIdentity id)
      throws IOException {
      log.debug2("sendTo(" + msg.toString() + ", " + id + ")");
    if (false) {
      //  XXX need an analog of this for stream
//       LockssDatagram dg = new LockssDatagram(LockssDatagram.PROTOCOL_LCAP,
// 					     msg.encodeMsg());
//       comm.sendTo(dg, au, id);
    }
    origRateLimiter.event();
  }

  // handle received message.  pass msg to handlers
  // XXX do we need another class for received stream "packets"
  void processIncomingMessage(LockssReceivedDatagram dg) {
    V3LcapMessage msg = null;
    log.debug2("rcvd message: " + dg);
    byte[] msgBytes = dg.getData();
//     try {
// 	msg = LcapMessage.decodeToMsg(msgBytes, dg.isMulticast());
//     } catch (IOException e) {
//       // XXX move the constants to IdentityManager
//       PeerIdentity pid = idMgr.ipAddrToPeerIdentity(dg.getSender());
//       idEvent(pid, LcapIdentity.EVENT_ERRPKT, null);
//       log.error("Couldn't decode incoming message", e);
//       return;
//     }
    if (!msg.isNoOp()) {
      runHandlers(msg);
    }
  }

  protected void runHandlers(V3LcapMessage msg) {
    for (Iterator iter = messageHandlers.iterator(); iter.hasNext();) {
      runHandler((MessageHandler)iter.next(), msg);
    }
  }

  private void runHandler(MessageHandler handler, V3LcapMessage msg) {
    try {
      handler.handleMessage(msg);
    } catch (Exception e) {
      log.error("callback threw", e);
    }
  }

  void idEvent(PeerIdentity pid, int event, V3LcapMessage msg) {
    idMgr.rememberEvent(pid, event, msg);
  }

  /**
   * Register a {@link LcapStreamRouter.MessageHandler}, which will be called
   * whenever an LcapMessage is received.
   * @param handler MessageHandler to add
   */
  public void registerMessageHandler(MessageHandler handler) {
    if (!messageHandlers.contains(handler)) {
      messageHandlers.add(handler);
    }
  }

  /**
   * Unregister a {@link LcapStreamRouter.MessageHandler}.
   * @param handler MessageHandler to remove
   */
  public void unregisterMessageHandler(MessageHandler handler) {
    messageHandlers.remove(handler);
  }

  /**
   * The LcapStreamRouter.MessageHandler interface defines the
   * callback registered by clients of {@link LcapStreamRouter} who want to process
   * incoming LcapMessages.
   */
  public interface MessageHandler {
    /**
     * Callback used to inform clients that an LcapMessage has been received.
     * @param msg the received LcapMessage
     * @see LcapStreamRouter#registerMessageHandler */
    public void handleMessage(V3LcapMessage msg);
  }
}
