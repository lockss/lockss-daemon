/*
 * $Id: LcapComm.java,v 1.14 2002-12-19 00:05:46 tal Exp $
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
import org.lockss.util.*;
import org.lockss.daemon.*;
import org.apache.commons.collections.LRUMap;

/**
 * LcapComm implements the routing parts of the LCAP protocol, using
 * {@link LcapSocket} to send and receive packets.
 * Routing involves decisions about using unicast to supplement multicast,
 * including forwarding received unicast packets.
 */
public class LcapComm {

  static final String PREFIX = Configuration.PREFIX + "comm.";
  static final String PARAM_MULTI_GROUP = PREFIX + "multicast.group";
  static final String PARAM_MULTI_PORT = PREFIX + "multicast.port";
  static final String PARAM_UNI_PORT = PREFIX + "unicast.port";
  static final String PARAM_UNI_PORT_SEND = PREFIX + "unicast.sendToPort";
  static final String PARAM_UNI_ADDR_SEND = PREFIX + "unicast.sendToAddr";
  static final String PARAM_VERIFY_MULTICAST = PREFIX + "verifyMulticast";

  static Logger log = Logger.getLogger("Comm");
  private static LcapComm singleton;

  private LRUMap multicastVerifyMap = new LRUMap(100);
  private boolean verifyMulticast = false;

  // These may change if/when we use multiple groups/ports
  private InetAddress group;
  private int multiPort = -1;		// multicast port
  private int uniPort = -1;		// unicast port
  private int uniSendToPort = -1;       // unicast send-to port, for testing
					// multiple instances on one machine
  private InetAddress uniSendToAddr = null; // unicast send-to addr, for
					    // testing multiple instances on
					    // one machine

  private SocketFactory sockFact;

  private LcapSocket sendSock;	// socket used for sending only
  private LcapSocket.Multicast mSock1;
  private LcapSocket.Multicast mSock2;
  private LcapSocket.Unicast uSock;

  private FifoQueue socketInQ;		// received packets from LcapSocket
  private ReceiveThread rcvThread;

  private Vector messageHandlers = new Vector();

  LcapComm(SocketFactory factory) {
    sockFact = factory;
  }

  /** Create a singleton, configure it from the Configuration singleton,
   * and start its thread(s) */
  public static void startComm() {
    singleton = new LcapComm(new NormalSocketFactory());
    singleton.configure(Configuration.getCurrentConfig());
    singleton.start();
  }

  /** Return the singleton LcapComm instance */
  public static LcapComm getComm() {
    return singleton;
  }

  /** Set communication parameters from configuration */
  public void configure(Configuration config) {
    String groupName = null;
    String uniSendToName = null;
    try {
      groupName = config.get(PARAM_MULTI_GROUP);
      String multiPortString = config.get(PARAM_MULTI_PORT);
      if ("localIp".equalsIgnoreCase(multiPortString)) {
	multiPort = derivedMultiPort();
      } else {
	multiPort = config.getInt(PARAM_MULTI_PORT);
      }
      uniPort = config.getInt(PARAM_UNI_PORT); // 
      uniSendToPort = config.getInt(PARAM_UNI_PORT_SEND, uniPort);
      uniSendToName = config.get(PARAM_UNI_ADDR_SEND);
      verifyMulticast = config.getBoolean(PARAM_VERIFY_MULTICAST, false);
    } catch (Configuration.InvalidParam e) {
      log.critical("Config error, not started", e);
    }
    try {
      if (groupName != null) {
	group = InetAddress.getByName(groupName);
      }
      if (group == null) {
	log.critical("null group addr");
	return;
      }
    } catch (UnknownHostException e) {
      log.critical("Can't get group addr, not started", e);
    }
    try {
      if (uniSendToName != null) {
	uniSendToAddr = InetAddress.getByName(uniSendToName);
      }
    } catch (UnknownHostException e) {
      log.critical("Can't get unicast send-to addr, not started", e);
    }
	
    if (log.isDebug()) {
      log.debug("groupName = " + groupName);
      log.debug("multiPort = " + multiPort);
      log.debug("uniPort = " + uniPort);
      log.debug("uniSendToPort = " + uniSendToPort);
      log.debug("verifyMulticast = " + verifyMulticast);
    }
  }

  private int derivedMultiPort() {
    try {
      InetAddress local = InetAddress.getLocalHost();
      byte[] addr = local.getAddress();
      return ((int)addr[3]) + 1234;
    } catch (UnknownHostException e) {
      log.error("Can't find local IP address, so can't use ip-addr-derived multicast port", e);
      return -1;
    }
  }

  /** Multicast a message to all caches holding the ArchivalUnit.
   * @param ld the datagram to send
   * @param au archival unit for which this message is relevant.  Used to
   * determine which multicast socket/port to send to.
   */
  public void send(LockssDatagram ld, ArchivalUnit au) throws IOException {
    if (multiPort < 0) {
      throw new IllegalStateException("Multicast port not configured");
    }
    if (group == null) {
      throw new IllegalStateException("Multicast group not configured");
    }
    sendTo(ld, group, multiPort);
  }

  /** Unicast a message to a single cache.
   * @param ld the datagram to send
   * @param au archival unit for which this message is relevant.  Used to
   * determine which multicast socket/port to send to.
   * @param id the identity of the cache to which to send the message
   */
  public void sendTo(LockssDatagram ld, ArchivalUnit au, LcapIdentity id)
      throws IOException {
    if (uniSendToPort < 0) {
      throw new IllegalStateException("Unicast port not configured");
    }
    sendTo(ld,
	   (uniSendToAddr == null ? id.getAddress() : uniSendToAddr),
	   uniSendToPort);
  }

  void sendTo(LockssDatagram ld, InetAddress addr, int port)
      throws IOException {
    log.debug("sending "+ ld +" to "+ addr +":"+ port);
    DatagramPacket pkt = ld.makeSendPacket(addr, port);
    sendSock.send(pkt);
  }

  void start() {
    socketInQ = new FifoQueue();
    try {
      sendSock = sockFact.newSendSocket();
    } catch (SocketException e) {
      log.critical("Can't create send socket", e);
    }
    if (multiPort >= 0 && group != null) {
      try {
	log.debug("new LcapSocket.Multicast("+socketInQ+", "+group+", "+
		  multiPort);
	mSock1 = sockFact.newMulticastSocket(socketInQ, group, multiPort);
	mSock1.start();
	log.info("Multicast socket started: " + mSock1);
      } catch (IOException e) {
	log.error("Can't create multicast socket", e);
      }
      if (verifyMulticast) {
	try {
	  log.debug("new LcapSocket.Multicast("+socketInQ+", "+group+", "+
		    multiPort);
	  mSock2 = sockFact.newMulticastSocket(socketInQ, group, multiPort);
	  mSock2.start();
	  log.info("2nd multicast socket started: " + mSock2);
	} catch (IOException e) {
	  log.warning("Can't create 2nd multicast socket, not detecting multicast spoofing", e);
	}
      }
    } else {
      log.error("Multicast group or port not configured, not starting multicast receive");
    }

    if (uniPort >= 0) {
      try {
	log.debug("new LcapSocket.Unicast("+socketInQ+", "+uniPort);
	LcapSocket.Unicast uSock =
	  sockFact.newUnicastSocket(socketInQ, uniPort);
	uSock.start();
	this.uSock = uSock;
	log.info("Unicast socket started: " + uSock);
      } catch (IOException e) {
	log.error("Can't create unicast socket", e);
      }
    } else {
      log.error("Unicast port not configured, not starting unicast receive");
    }
    ensureQRunner();
  }

  // tk add watchdog
  synchronized void ensureQRunner() {
    if (rcvThread == null) {
      log.info("Starting receive thread");
      rcvThread = new ReceiveThread("CommRcv");
      rcvThread.start();
    }
  }

  synchronized void stop() {
    if (rcvThread != null) {
      log.info("Stopping Q runner");
      rcvThread.stopRcvThread();
      rcvThread = null;
    }
  }

  /** Verify that the packet is one we should process, <i>ie</i>, it is
   * not a spoofed multicast packet */
  private boolean verifyPacket(LockssReceivedDatagram dg) {
    if (!verifyMulticast || !dg.isMulticast()) {
      // Process all packets received on unicast socket.
      return true;
    }
    LcapSocket rcvSock = (LcapSocket)multicastVerifyMap.get(dg);
    if (rcvSock == null) {
      // This is the first time we've recieved this packet.
      // Remember the socket we received it on, but don't process it
      multicastVerifyMap.put(dg, dg.getReceiveSocket());
      return false;
    }
    if (rcvSock != dg.getReceiveSocket()) {
      // We've now received the same packet on both multicast sockets, so
      // it really was multicast.
      multicastVerifyMap.remove(dg);
      return true;
    }
    // Here if saw the same packet twice on the same multicast socket.
    // Ignore this one, keep waiting to see if we see it on other socket.
    return false;
  }

  private void processReceivedPacket(LockssReceivedDatagram ld) {
    if (verifyPacket(ld)) {
      log.debug("Received " + ld);
      runHandlers(ld);
    }
  }

  private void runHandler(MessageHandler handler, LockssReceivedDatagram ld) {
    try {
      handler.handleMessage(ld);
    } catch (Exception e) {
      log.error("callback threw", e);
    }
  }

  private void runHandlers(LockssReceivedDatagram ld) {
    int proto = ld.getProtocol();
    MessageHandler handler;
    if (proto < messageHandlers.size() &&
	(handler = (MessageHandler)messageHandlers.get(proto))  != null) {
      runHandler(handler, ld);
    } else {
      log.warning("Message receved for unregistered protocol: " + proto);
    }
//      for (Iterator iter = messageHandlers.iterator(); iter.hasNext();) {
//        runHandler((MessageHandler)iter.next(), ld);
//      }
  }

  /**
   * Register a {@link LcapComm.MessageHandler}, which will be called
   * whenever a message is received.
   * @param handler MessageHandler to add.
   */
  public void registerMessageHandler(int protocol, MessageHandler handler) {
    if (protocol >= messageHandlers.size()) {
      messageHandlers.setSize(protocol + 1);
    }
    if (messageHandlers.get(protocol) != null) {
      throw
	new RuntimeException("Protocol " + protocol + " already registered");
    }
    messageHandlers.set(protocol, handler);
//      if (!messageHandlers.contains(handler)) {
//        messageHandlers.add(handler);
//      }
  }
      
  /**
   * Unregister a {@link LcapComm.MessageHandler}.
   * @param handler MessageHandler to remove.
   */
  public void unregisterMessageHandler(int protocol, MessageHandler handler) {
    if (protocol < messageHandlers.size());
    messageHandlers.set(protocol, null);
//      messageHandlers.remove(handler);
  }


  // Receive thread
  private class ReceiveThread extends Thread {
    private boolean goOn = true;
    private int sleep = 60000;
    private Deadline timeout = Deadline.in(sleep);

    private ReceiveThread(String name) {
      super(name);
    }

    public void run() {
      //        if (rcvPriority > 0) {
      //  	Thread.currentThread().setPriority(rcvPriority);
      //        }

      while (goOn) {
	try {
	  synchronized (timeout) {
	    if (goOn) {
	      timeout.expireIn(sleep);
	    }
	  }
	  Object qObj = socketInQ.get(timeout);
	  if (qObj != null) {
	    if (qObj instanceof LockssReceivedDatagram) {
	      processReceivedPacket((LockssReceivedDatagram)qObj);
	    } else {	      
	      log.warning("Non-LockssReceivedDatagram on rcv queue" + qObj);
	    }
	  }
	} catch (InterruptedException e) {
	  // just wake up and check for exit
	} finally {
	}
      }
      // prevent rcvThread changing during ensureQRunner() or stop()
      synchronized (LcapComm.this) {
	rcvThread = null;
      }
    }

    private void stopRcvThread() {
      synchronized (timeout) {
	goOn = false;
	timeout.expire();
      }
    }
  }

  /**
   * The <code>LcapComm.MessageHandler</code> interface defines the
   * callback registered by clients of {@link LcapComm} who want to process
   * incoming messages
   */
  public interface MessageHandler {
    /**
     * Callback used to inform clients that a message has been received.
     * @param ld  the received LockssReceivedDatagram
     * @see LcapComm#registerMessageHandler */
    public void handleMessage(LockssReceivedDatagram ld);
  }

  /** SocketFactory interface is so test case can use mock sockets */
  interface SocketFactory {
    LcapSocket.Multicast newMulticastSocket(Queue rcvQ,
					    InetAddress group,
					    int port)
	throws IOException;

    LcapSocket.Unicast newUnicastSocket(Queue rcvQ, int port)
	throws IOException;

    LcapSocket newSendSocket() throws SocketException;
  }

  /** Normal socket factory creates real LcapSockets */
  static class NormalSocketFactory implements SocketFactory {
    public LcapSocket.Multicast newMulticastSocket(Queue rcvQ,
						   InetAddress group,
						   int port)
	throws IOException {
      return new LcapSocket.Multicast(rcvQ, group, port);
    }

    public LcapSocket.Unicast newUnicastSocket(Queue rcvQ, int port)
	throws IOException {
      return new LcapSocket.Unicast(rcvQ, port);
    }

    public LcapSocket newSendSocket() throws SocketException {
      return new LcapSocket();
    }
  }

  // remove these

  /** Multicast a message to all caches holding the ArchivalUnit.
   * @param msg the message to send
   * @param au archival unit for which this message is relevant.  Used to
   * determine which multicast socket/port to send to.
   * @deprecated Use {@link #send(LockssDatagram, ArchivalUnit)}
   */
  public static void sendMessage(LcapMessage msg, ArchivalUnit au)
      throws IOException {
    if (singleton == null) {
      throw new IllegalStateException("LcapComm singleton not created");
    }
    getComm().send(new LockssDatagram(LockssDatagram.PROTOCOL_LCAP,
				      msg.encodeMsg()),
		   au);
  }

  /** Unicast a message to a single cache.
   * @param msg the message to send
   * @param au archival unit for which this message is relevant.  Used to
   * determine which multicast socket/port to send to.
   * @param id the identity of the cache to which to send the message
   * @deprecated Use {@link #sendTo(LockssDatagram, ArchivalUnit,
   * LcapIdentity)}
   */
  public static void sendMessageTo(LcapMessage msg, ArchivalUnit au,
				   LcapIdentity id)
      throws IOException {
    if (singleton == null) {
      throw new IllegalStateException("LcapComm singleton not created");
    }
    getComm().sendTo(new LockssDatagram(LockssDatagram.PROTOCOL_LCAP,
					msg.encodeMsg()),
		     au, id);
  }
}
