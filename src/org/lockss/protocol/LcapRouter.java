/*
 * $Id: LcapRouter.java,v 1.46 2006-03-07 02:35:07 smorabito Exp $
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
import org.lockss.config.Configuration;
import org.lockss.daemon.*;
import org.lockss.plugin.*;
import org.lockss.poller.*;

/**
 * LcapRouter is the topmost layer of the comm services, to which clients
 * pass outgoing messages and with which they register incoming message
 * handlers.  This class interacts with LcapDatagramRouter or
 * LcapStreamComm to send and receive datagram and stream messages,
 * respectively.
 */
public class LcapRouter
  extends BaseLockssDaemonManager implements ConfigurableManager {

  private static final String PREFIX =
    Configuration.PREFIX + "comm.";
  private static final String PARAM_V3_LCAP_MESSAGE_DATA_DIR =
    PREFIX + "v3LcapMessageDataDir";
  private static final String DEFAULT_V3_LCAP_MESSAGE_DATA_DIR =
    System.getProperty("java.io.tmpdir");
  
  static Logger log = Logger.getLogger("Router");

  private IdentityManager idMgr;
  private LcapStreamComm scomm = null;
  private LcapDatagramRouter drouter = null;
  private LcapDatagramRouter.MessageHandler drouterHandler;

  private List messageHandlers = new ArrayList();
  private File dataDir = null;

  public void startService() {
    super.startService();
    LockssDaemon daemon = getDaemon();
    try {
      drouter = daemon.getDatagramRouterManager();
      drouterHandler = new LcapDatagramRouter.MessageHandler() {
	  public void handleMessage(LcapMessage msg) {
	    runHandlers(msg);
	  }};
      drouter.registerMessageHandler(drouterHandler);
    } catch (IllegalArgumentException e) {
      log.warning("No datagram router");
      drouter = null;
    }
    try {
      scomm = daemon.getStreamCommManager();
      scomm.registerMessageHandler(PeerMessage.PROTOCOL_LCAP_V3,
				   new LcapStreamComm.MessageHandler() {
				     public void handleMessage(PeerMessage
							       msg) {
				       handleIncomingPeerMessage(msg);
				     }});
    } catch (IllegalArgumentException e) {
      log.warning("No stream comm");
      scomm = null;
    }
    idMgr = daemon.getIdentityManager();
  }

  public void stopService() {
    if (scomm != null) {
      scomm.unregisterMessageHandler(PeerMessage.PROTOCOL_LCAP_V3);
    }
    if (drouter != null && drouterHandler != null) {
      drouter.unregisterMessageHandler(drouterHandler);
    }
    super.stopService();
  }

  public void setConfig(Configuration config, Configuration oldConfig,
			Configuration.Differences changedKeys) {
    if (changedKeys.contains(PARAM_V3_LCAP_MESSAGE_DATA_DIR)) {
        String paramDataDir = config.get(PARAM_V3_LCAP_MESSAGE_DATA_DIR,
                                         DEFAULT_V3_LCAP_MESSAGE_DATA_DIR);
        File dir = new File(paramDataDir);
        if (dir.exists() || dir.mkdirs()) {
          dataDir = dir;
          log.debug2("V3LcapMessage data dir: " + dataDir);
        } else {
          log.warning("No V3LcapMessage data dir: " + dir);
          dataDir = null;
        }
    }
  }

  /** Multicast a message to all peers.  Only supported for V1 messages
   * @param msg the message to send
   * @param au archival unit for which this message is relevant.  Used to
   * determine which multicast socket/port to send to.
   * @throws IOException if message couldn't be sent
   */
  public void send(V1LcapMessage msg, ArchivalUnit au) throws IOException {
    drouter.send(msg, au);
  }

  /** Send a message to a peer.
   * @param msg the message to send
   * @param au archival unit for which this message is relevant (not used).
   * @param id the identity of the peer to which to send the message
   * @throws IOException if message couldn't be sent
   */
  public void sendTo(V1LcapMessage msg, ArchivalUnit au, PeerIdentity id)
      throws IOException {
    drouter.sendTo(msg, au, id);
  }

  /** Send a message to a peer.
   * @param msg the message to send
   * @param id the identity of the peer to which to send the message
   * @throws IOException if message couldn't be sent
   */
  public void sendTo(V3LcapMessage msg, PeerIdentity id)
      throws IOException {
    PeerMessage pm = makePeerMessage(msg);
    scomm.sendTo(pm, id, null);
  }

  /** Encode a V3LcapMessage into a PeerMessage */
  PeerMessage makePeerMessage(V3LcapMessage lmsg) throws IOException {
    OutputStream out = null;
    InputStream in = null;
    try {
      // XXX need estimate of lcap msg size here
      PeerMessage pmsg = newPeerMessage();
      pmsg.setProtocol(PeerMessage.PROTOCOL_LCAP_V3);
      out = pmsg.getOutputStream();
      in = lmsg.getInputStream();
      StreamUtil.copy(in, out);
      return pmsg;
    } finally {
      IOUtil.safeClose(in);
      IOUtil.safeClose(out);
    }
  }

  /** Decode a PeerMessage into a V3LcapMessage  */
  V3LcapMessage makeV3LcapMessage(PeerMessage pmsg) throws IOException {
    InputStream in = null;
    try {
      in = pmsg.getInputStream();
      V3LcapMessage lmsg = new V3LcapMessage(in, dataDir);
      lmsg.setOriginatorId(pmsg.getSender());
      return lmsg;
    } finally {
      IOUtil.safeClose(in);
    }
  }

  PeerMessage newPeerMessage() {
    return scomm.newPeerMessage();
  }

  // Incoming peer message - decode to V3LcapMessage and run handlers
  void handleIncomingPeerMessage(PeerMessage pmsg) {
    try {
      LcapMessage lmsg = makeV3LcapMessage(pmsg);
      runHandlers(lmsg);
    } catch (Exception e) {
      log.warning("Exception while processing incoming " + pmsg, e);
    } finally {
      pmsg.delete();
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
