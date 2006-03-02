/*
 * $Id: LcapDatagramComm.java,v 1.15.6.1 2006-03-02 19:41:16 tlipkis Exp $
 */

/*

Copyright (c) 2000-2006 Board of Trustees of Leland Stanford Jr. University,
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
import java.text.*;
import org.lockss.util.*;
import org.lockss.util.Queue;
import org.lockss.config.ConfigManager;
import org.lockss.config.Configuration;
import org.lockss.daemon.*;
import org.lockss.daemon.status.*;
import org.apache.commons.collections.map.LRUMap;
import org.lockss.app.*;
import org.lockss.alert.*;
import org.lockss.plugin.*;

/**
 * LcapDatagramComm supports datagram (multicast and/or unicast) communication
 * between LOCKSS caches.  The packets sent at this level are {@link
 * LockssDatagram}s.  They are sent and received via {@link LcapSocket}s.
 */
public class LcapDatagramComm
  extends BaseLockssDaemonManager implements ConfigurableManager {

  static final String PARAM_LOCAL_IPS =
    Configuration.PREFIX + "platform.localIPs";

  static final String PREFIX = Configuration.PREFIX + "comm.";
  public static final String PARAM_ENABLED = PREFIX + "enabled";
  public static final boolean DEFAULT_ENABLED = true;

  public static final String PARAM_MULTI_GROUP = PREFIX + "multicast.group";
  public static final String PARAM_MULTI_PORT = PREFIX + "multicast.port";
  public static final String PARAM_MULTI_VERIFY = PREFIX + "multicast.verify";
  public static final boolean DEFAULT_MULTI_VERIFY = false;

  /** If we receive no multicast packets (other than from ourself) for this
   * long, stop sending multicast */
  static final String PARAM_MULTI_DISABLE_TIMEOUT = PREFIX +
    "multicast.disableAfterInactive";
  static final long DEFAULT_MULTI_DISABLE_TIMEOUT = 1 * Constants.HOUR;

  /** If multicast has been disabled, explicitly loopback all
   * normally-multicast packets */
  static final String PARAM_LOOPBACK_IF_MUZZLED = PREFIX +
    "loopbackIfMulticastMuzzled";
  static final boolean DEFAULT_LOOPBACK_IF_MUZZLED = true;

  static final String PARAM_UNI_PORT = PREFIX + "unicast.port";
  static final String PARAM_UNI_PORT_SEND = PREFIX + "unicast.sendToPort";
  static final String PARAM_UNI_ADDR_SEND = PREFIX + "unicast.sendToAddr";

  static final String PARAM_COMPRESS_PACKETS = PREFIX + "compress";
  static final String PARAM_COMPRESS_MIN = PREFIX + "compress.min";
  static final boolean DEFAULT_COMPRESS_PACKETS = false;
  static final int DEFAULT_COMPRESS_MIN = 200;

  static final String WDOG_PARAM_COMM = "Comm";
  static final long WDOG_DEFAULT_COMM = 1 * Constants.HOUR;

  static final String PRIORITY_PARAM_COMM = "Comm";
  static final int PRIORITY_DEFAULT_COMM = -1;

  static Logger log = Logger.getLogger("Comm");

  private boolean enabled = DEFAULT_ENABLED;
  private boolean isMuzzleMulticast = false;
  private long lastMulticastTime;
  private long muzzleMulticastAfter = DEFAULT_MULTI_DISABLE_TIMEOUT;
  private boolean loopbackIfMulticastMuzzled = DEFAULT_LOOPBACK_IF_MUZZLED;

  private IdentityManager idMgr;
  private OneShot configShot = new OneShot();
  private LRUMap multicastVerifyMap = new LRUMap(100);
  private boolean verifyMulticast = false;

  // These may change if/when we use multiple groups/ports
  private IPAddr group;
  private int multiPort = -1;		// multicast port
  private int uniPort = -1;		// unicast port
  private int uniSendToPort = -1;       // unicast send-to port, for testing
					// multiple instances on one machine
  private IPAddr uniSendToAddr = null; // unicast send-to addr, for
					    // testing multiple instances on
					    // one machine

  private List localInterfaces;
  private SocketFactory sockFact;

  private LcapSocket sendSock;	// socket used for sending only
  private LcapSocket.Multicast mSock1;
  private LcapSocket.Multicast mSock2;
  private LcapSocket.Unicast uSock;

  private FifoQueue socketInQ;		// received packets from LcapSocket
  private ReceiveThread rcvThread;

  private Vector messageHandlers = new Vector();

  public LcapDatagramComm() {
    sockFact = new NormalSocketFactory();
  }

  /** For testing */
  LcapDatagramComm(SocketFactory factory, Configuration config) {
    sockFact = factory;
    configure(config,
	      ConfigManager.EMPTY_CONFIGURATION,
	      Configuration.DIFFERENCES_ALL);
  }

  /**
   * start the comm manager.
   * @see org.lockss.app.LockssManager#startService()
   */
  public void startService() {
    super.startService();
    idMgr = getDaemon().getIdentityManager();
    if (enabled) {
      start();
      getDaemon().getStatusService().registerStatusAccessor("CommStats",
							    new Status());
    }
  }

  /**
   * stop the comm manager
   * @see org.lockss.app.LockssManager#stopService()
   */
  public void stopService() {
    // TODO: checkpoint here.
    getDaemon().getStatusService().unregisterStatusAccessor("CommStats");
    stop();
    super.stopService();
  }


  /**
   * Set communication parameters from configuration, once only.
   * This service currently cannot be reconfigured.
   * @param config the Configuration
   */
  public void setConfig(Configuration config,
			Configuration prevConfig,
			Configuration.Differences changedKeys) {
    loopbackIfMulticastMuzzled =
      config.getBoolean(PARAM_LOOPBACK_IF_MUZZLED,
			DEFAULT_LOOPBACK_IF_MUZZLED);

    if (configShot.once()) {
      configure(config, prevConfig, changedKeys);
    }
  }

  /** Internal config, so can invoke from test constructor  */
  void configure(Configuration config,
		 Configuration prevConfig,
		 Configuration.Differences changedKeys) {
    enabled = config.getBoolean(PARAM_ENABLED, DEFAULT_ENABLED);
    if (!enabled) {
      return;
    }
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
      verifyMulticast = config.getBoolean(PARAM_MULTI_VERIFY,
					  DEFAULT_MULTI_VERIFY);
    } catch (Configuration.InvalidParam e) {
      log.critical("Config error, not started: " + e);
      noCommAlert("Bad config: " + e.getMessage());
      return;
    }
    muzzleMulticastAfter =
      config.getTimeInterval(PARAM_MULTI_DISABLE_TIMEOUT,
			     DEFAULT_MULTI_DISABLE_TIMEOUT);
    try {
      if (groupName != null) {
	group = IPAddr.getByName(groupName);
      }
      if (group == null) {
	log.critical("null group addr");
	noCommAlert("Unknown group addr: " + groupName);
	return;
      }
    } catch (UnknownHostException e) {
      log.critical("Can't get group addr, not started: " + e);
      noCommAlert("Unknown group addr: " + groupName);
      return;
    }
    try {
      if (uniSendToName != null) {
	uniSendToAddr = IPAddr.getByName(uniSendToName);
      }
    } catch (UnknownHostException e) {
      log.error("Unknown unicast send-to addr: " + e);
    }
    // make list of IPAddrs of local interfaces
    if (localInterfaces == null || changedKeys.contains(PARAM_LOCAL_IPS)) {
      String s = config.get(PARAM_LOCAL_IPS, "");
      List ipStrings = StringUtil.breakAt(s, ';');
      List newList = new ArrayList();
      for (Iterator iter = ipStrings.iterator(); iter.hasNext(); ) {
	String ip = (String)iter.next();
	try {
	  IPAddr inet = IPAddr.getByName(ip);
	  newList.add(inet);
	} catch (UnknownHostException e) {
	  log.warning("Couldn't parse local interface IP address: " + ip);
	}
      }
      // set localInterfaces only if new list non empty
      if (!newList.isEmpty()) {
	localInterfaces = newList;
      }
    }
    if (log.isDebug()) {
      log.debug("groupName = " + groupName);
      log.debug("multiPort = " + multiPort);
      log.debug("uniPort = " + uniPort);
      log.debug("uniSendToPort = " + uniSendToPort);
      log.debug("verifyMulticast = " + verifyMulticast);
    }
  }

  void noCommAlert(String text) {
    try {
      AlertManager alertMgr = theDaemon.getAlertManager();
      alertMgr.raiseAlert(Alert.cacheAlert(Alert.CONFIGURATION_ERROR),
			  "V1 LCAP comm not started.  " + text);
    } catch (Exception e) {
      // ignored, expected during testing
    }
  }

  boolean checkMuzzleMulticast() {
    if (isMuzzleMulticast) return true;
    if (TimeBase.msSince(lastMulticastTime) >= muzzleMulticastAfter) {
      log.info("No multicast connectivity; disabling multicast send");
      isMuzzleMulticast = true;
    }
    return isMuzzleMulticast;
  }

  /** Return true if the packet's source address is one of my interfaces. */
  boolean didISend(LockssReceivedDatagram dg) {
    IPAddr sender = dg.getSender();
    if (localInterfaces == null) {
      return sender.equals(getLocalIdentityAddr());
    } else {
      for (Iterator iter = localInterfaces.iterator(); iter.hasNext(); ) {
	if (sender.equals(iter.next())) {
	  return true;
	}
      }
      return false;
    }
  }

  private IPAddr localIp;

  IPAddr getLocalIdentityAddr() {
    if (localIp == null) {
      localIp = idMgr.getLocalIPAddr();
    }
    return localIp;
  }

  private int derivedMultiPort() {
    try {
      IPAddr local = IPAddr.getLocalHost();
      byte[] addr = local.getAddress();
      return ((int)addr[3]) + 1234;
    } catch (UnknownHostException e) {
      log.error("Can't find local IP address, so can't use ip-addr-derived multicast port", e);
      return -1;
    }
  }

  private void sendToSelf(LockssDatagram ld, RateLimiter limiter)
      throws IOException {
    log.debug2("sendToSelf(" + ld + ")");
    DatagramPacket pkt = ld.makeSendPacket(getLocalIdentityAddr(), uniPort);
    LockssReceivedDatagram rdg = new LockssReceivedDatagram(pkt);
    rdg.setReceiveSocket(uSock);
    rdg.setMulticast(false);
    socketInQ.put(rdg);
  }

  /** Multicast a message to all caches holding the ArchivalUnit.
   * @param ld the datagram to send
   * @param au archival unit for which this message is relevant.  Used to
   * determine which multicast socket/port to send to.
   * @throws IOException
   */
  public void send(LockssDatagram ld, ArchivalUnit au, RateLimiter limiter)
      throws IOException {
    if (multiPort < 0) {
      throw new IllegalStateException("Multicast port not configured");
    }
    if (group == null) {
      throw new IllegalStateException("Multicast group not configured");
    }
    if (checkMuzzleMulticast()) {
      // We normally see our own packets via multicast; if that's muzzled,
      // send the packet to ourself
      if (loopbackIfMulticastMuzzled) {
	sendToSelf(ld, limiter);
      }
      return;
    }
    if (limiter == null || limiter.isEventOk()) {
      updateOutStats(ld, multiPort, true);
      DatagramPacket pkt = ld.makeSendPacket(group, multiPort);
      sendSock.send(pkt);
      if (limiter != null) limiter.event();
    } else {
      log.debug2("Pkt rate limited");
    }
  }

  /** Unicast a message to a single cache.
   * @param ld the datagram to send
   * @param au archival unit for which this message is relevant.  Used to
   * determine which multicast socket/port to send to.
   * @param id the identity of the cache to which to send the message
   * @throws IOException
   */
  public void sendTo(LockssDatagram ld, ArchivalUnit au, PeerIdentity id,
		     RateLimiter limiter)
      throws IOException {
    if (uniSendToPort < 0) {
      throw new IllegalStateException("Unicast port not configured");
    }
    log.debug2("sendTo(" + ld + ", " + id + ")");
    PeerIdentity pid = id;
    if (uniSendToAddr != null) {
      pid = idMgr.ipAddrToPeerIdentity(uniSendToAddr, 0);
    }
    sendTo(ld, pid, uniSendToPort, limiter);
  }

  void sendTo(LockssDatagram ld, PeerIdentity id, RateLimiter limiter)
      throws IOException {
    sendTo(ld, id, uniSendToPort, limiter);
  }

  void sendTo(LockssDatagram ld, PeerIdentity id, int port,
		      RateLimiter limiter)
      throws IOException {
    log.debug2("sending "+ ld +" to "+ id +":"+ port);
    if (limiter == null || limiter.isEventOk()) {
      updateOutStats(ld, uniSendToPort, false);
      IPAddr ipAddr = idMgr.identityToIPAddr(id);
      DatagramPacket pkt = ld.makeSendPacket(ipAddr, port);
      sendSock.send(pkt);
      if (limiter != null) limiter.event();
    } else {
      log.debug2("Pkt rate limited");
    }
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
	  verifyMulticast = false;
	}
      }
    } else {
      log.error("Multicast group or port not configured, not starting multicast receive");
    }

    if (uniPort >= 0) {
      try {
	log.debug("new LcapSocket.Unicast("+socketInQ+", "+uniPort);
	LcapSocket.Unicast lsu =
	  sockFact.newUnicastSocket(socketInQ, uniPort);
	lsu.start();
	uSock = lsu;
	log.info("Unicast socket started: " + lsu);
      } catch (IOException e) {
	log.error("Can't create unicast socket", e);
	noCommAlert("Can't create unicast socket: " + e.getMessage());
	throw
	  new ResourceUnavailableException("Can't bind unicast socket", e);
      }
    } else {
      log.error("Unicast port not configured, not starting unicast receive");
    }
    isMuzzleMulticast = false;
    lastMulticastTime = TimeBase.nowMs();
    ensureQRunner();
  }

  // tk add watchdog
  synchronized void ensureQRunner() {
    if (rcvThread == null) {
      log.info("Starting receive thread");
      rcvThread = new ReceiveThread("CommRcv");
      rcvThread.start();
      rcvThread.waitRunning();
    }
  }

  synchronized void stop() {
    if (uSock != null) {
      uSock.stop();
      uSock = null;
    }
    if (mSock1 != null) {
      mSock1.stop();
      mSock1 = null;
    }
    if (mSock2 != null) {
      mSock2.stop();
      mSock2 = null;
    }
    if (rcvThread != null) {
      log.info("Stopping Q runner");
      rcvThread.stopRcvThread();
      rcvThread = null;
    }
  }

  /**
   * Verify that the packet is one we should process, <i>ie</i>, it is
   * not a spoofed multicast packet
   * @param dg the LockssReceivedDatagram
   * @return true if the packet should be processed
   */
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
      log.debug2("Received " + ld);
      try {
	updateInStats(ld);
	if (ld.isMulticast() && !didISend(ld)) {
	  isMuzzleMulticast = false;
	  lastMulticastTime = TimeBase.nowMs();
	}
	runHandlers(ld);
      } catch (ProtocolException e) {
	log.warning("Cannot process incoming packet", e);
      }
    }
  }

  protected void runHandler(MessageHandler handler,
			    LockssReceivedDatagram ld) {
    try {
      handler.handleMessage(ld);
    } catch (Exception e) {
      log.error("callback threw", e);
    }
  }

  private void runHandlers(LockssReceivedDatagram ld)
      throws ProtocolException {
    try {
      int proto = ld.getProtocol();
      MessageHandler handler;
      if (proto >= 0 && proto < messageHandlers.size() &&
	  (handler = (MessageHandler)messageHandlers.get(proto)) != null) {
	runHandler(handler, ld);
      } else {
	log.warning("Received message with unregistered protocol: " + proto +
		    " from " + ld.getSender());
      }
    } catch (RuntimeException e) {
      log.warning("Unexpected error in runHandlers", e);
      throw new ProtocolException(e.toString());
    }
  }

  /**
   * Register a {@link LcapDatagramComm.MessageHandler}, which will be called
   * whenever a message is received.
   * @param protocol an int representing the protocol
   * @param handler MessageHandler to add
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
  }

  /**
   * Unregister a {@link LcapDatagramComm.MessageHandler}.
   * @param protocol an int representing the protocol
   */
  public void unregisterMessageHandler(int protocol) {
    if (protocol < messageHandlers.size()) {
      messageHandlers.set(protocol, null);
    }
  }


  // Receive thread
  private class ReceiveThread extends LockssThread {
    private volatile boolean goOn = true;
    private long sleep = Constants.MINUTE;
    private Deadline timeout = Deadline.in(sleep);

    private ReceiveThread(String name) {
      super(name);
    }

    public void lockssRun() {
      setPriority(PRIORITY_PARAM_COMM, PRIORITY_DEFAULT_COMM);
      triggerWDogOnExit(true);
      startWDog(WDOG_PARAM_COMM, WDOG_DEFAULT_COMM);
      nowRunning();

      while (goOn) {
	pokeWDog();
	try {
	  synchronized (timeout) {
	    if (goOn) {
	      timeout.expireIn(sleep);
	    }
	  }
	  if (log.isDebug3()) log.debug3("socketInQ.get(" + timeout + ")");
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
      synchronized (LcapDatagramComm.this) {
	rcvThread = null;
      }
    }

    private void stopRcvThread() {
      synchronized (timeout) {
	stopWDog();
	triggerWDogOnExit(false);
	goOn = false;
	timeout.expire();
      }
    }
  }

  /**
   * The <code>LcapDatagramComm.MessageHandler</code> interface defines the
   * callback registered by clients of {@link LcapDatagramComm} who want to process
   * incoming messages
   */
  public interface MessageHandler {
    /**
     * Callback used to inform clients that a message has been received.
     * @param ld  the received LockssReceivedDatagram
     * @see LcapDatagramComm#registerMessageHandler */
    public void handleMessage(LockssReceivedDatagram ld);
  }

  /** SocketFactory interface is so test case can use mock sockets */
  interface SocketFactory {
    LcapSocket.Multicast newMulticastSocket(Queue rcvQ,
					    IPAddr group,
					    int port)
	throws IOException;

    LcapSocket.Unicast newUnicastSocket(Queue rcvQ, int port)
	throws IOException;

    LcapSocket newSendSocket() throws SocketException;
  }

  /** Normal socket factory creates real LcapSockets */
  static class NormalSocketFactory implements SocketFactory {
    public LcapSocket.Multicast newMulticastSocket(Queue rcvQ,
						   IPAddr group,
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

  // Statistics

  private static class Stats {
    int port;
    int proto;
    boolean multicast;
    boolean compressed;
    int inPkts;
    long inBytes;
    long inUncBytes;
    int outPkts;
    long outBytes;
    long outUncBytes;

  }

  protected void updateInStats(LockssReceivedDatagram ld)
      throws ProtocolException {
    boolean mcast = ld.isMulticast();
    if (mcast && didISend(ld)) {
      // We receive all the multicast packets we send; don't count them in
      // receive statistics.
      // This does not work when running multiple daemons on one machine,
      // as the packets from all the instances have the same source addr
      // (which is probably not the local identity addr).
      return;
    }
    LcapSocket lsock = ld.getReceiveSocket();
    DatagramSocket sock = lsock.getSocket();
    updateStats(ld, sock.getLocalPort(), true, mcast);
  }

  private void updateOutStats(LockssDatagram ld, int port, boolean multicast) throws ProtocolException {
    updateStats(ld, port, false, multicast);
  }

  private void updateStats(LockssDatagram ld, int port,
			   boolean in, boolean multicast) throws ProtocolException {
    int proto = ld.getProtocol();
    Stats stats = getStatsObj(port, proto, multicast, false);
    if (in) {
      stats.inPkts++;
      stats.inBytes += ld.getPacketSize();
      stats.inUncBytes += ld.getDataSize();
      if (ld.isCompressed()) {
	inCompressed += ld.getPacketSize();
	inUncompressed += ld.getDataSize();
      }
    } else {
      stats.outPkts++;
      stats.outBytes += ld.getPacketSize();
      stats.outUncBytes += ld.getDataSize();
      if (ld.isCompressed()) {
	outCompressed += ld.getPacketSize();
	outUncompressed += ld.getDataSize();
      }
    }
  }

  Stats getStatsObj(int port, int proto, boolean multicast,
		    boolean compressed) {
    StringBuffer sb = new StringBuffer();
    sb.append(port);
    sb.append(proto);
    sb.append(multicast);
//     sb.append(in);
    sb.append(compressed);
    String key = sb.toString();
    Stats stats = (Stats)statsMap.get(key);
    if (stats == null) {
      stats = new Stats();
      stats.port = port;
      stats.proto = proto;
      stats.multicast = multicast;
      stats.compressed = compressed;
      statsMap.put(key, stats);
    }
    return stats;
  }

  private Map statsMap = new HashMap();
  private long inCompressed = 0;
  private long inUncompressed = 0;
  private long outCompressed = 0;
  private long outUncompressed = 0;


  private static final List statusSortRules =
    ListUtil.list(new StatusTable.SortRule("sort", true),
		  new StatusTable.SortRule("proto", true),
		  new StatusTable.SortRule("port", true),
		  new StatusTable.SortRule("cast", true),
		  new StatusTable.SortRule("dir", true),
		  new StatusTable.SortRule("comp", true));

  private static final String multiExcludeNote =
    "Excludes multicast packets sent by this cache.";

  private static final List statusColDescs =
    ListUtil.list(
		  new ColumnDescriptor("proto", "Proto",
				       ColumnDescriptor.TYPE_INT),
		  new ColumnDescriptor("port", "Port",
				       ColumnDescriptor.TYPE_INT,
				       "U/M = Unicast/Multicast"),
// 		  new ColumnDescriptor("cast", "U/M",
// 				       ColumnDescriptor.TYPE_STRING,
// 				       "Unicast/Multicast"),
// 		  new ColumnDescriptor("comp", "Comprsd",
// 				       ColumnDescriptor.TYPE_STRING),
		  new ColumnDescriptor("inPkts", "Pkts in",
				       ColumnDescriptor.TYPE_INT,
				       multiExcludeNote),
		  new ColumnDescriptor("inBytes", "Bytes in",
				       ColumnDescriptor.TYPE_INT,
				       multiExcludeNote),
// 		  new ColumnDescriptor("inUncBytes", "(uncompressed)",
// 				       ColumnDescriptor.TYPE_INT,
// 				       multiExcludeNote),
		  new ColumnDescriptor("outPkts", "Pkts out",
				       ColumnDescriptor.TYPE_INT),
		  new ColumnDescriptor("outBytes", "Bytes out",
				       ColumnDescriptor.TYPE_INT)
// 		  new ColumnDescriptor("outUncBytes", "(uncompressed)",
// 				       ColumnDescriptor.TYPE_INT)
		  );

  private class Status implements StatusAccessor {
    // port, proto, u/m, direction, compressed, pkts, bytes
    long start;

    public String getDisplayName() {
      return "Comm Statistics";
    }

    public void populateTable(StatusTable table) {
      table.setResortable(false);
      String key = table.getKey();
      String tfoot = "Statistics are for LCAP traffic only.  " +
	"Traffic due to fetching content (crawling) is not included.";
      table.setTitleFootnote(tfoot);
      table.setColumnDescriptors(statusColDescs);
      table.setDefaultSortRules(statusSortRules);
      table.setRows(getRows(key));
      table.setSummaryInfo(getSummaryInfo(key));
    }

    public boolean requiresKey() {
      return false;
    }

    private List getSummaryInfo(String key) {
      List res = new ArrayList();
      if (inCompressed != 0) {
	double cmp = 1.0 - ((double)inCompressed / (double)inUncompressed);
	res.add(new StatusTable.SummaryInfo("Input compression",
					    ColumnDescriptor.TYPE_PERCENT,
					    new Double(cmp)));
      }
      if (outCompressed != 0) {
	double cmp = 1.0 - ((double)outCompressed / (double)outUncompressed);
	res.add(new StatusTable.SummaryInfo("Output compression",
					    ColumnDescriptor.TYPE_PERCENT,
					    new Double(cmp)));
      }
      return res;
    }

    private List getRows(String key) {
      List table = new ArrayList();
      Stats tot = new Stats();
      Iterator iter = statsMap.values().iterator();
      if (!iter.hasNext()) {
	return table;
      }
      while (iter.hasNext()) {
	Stats st = (Stats)iter.next();
	table.add(makeRow(st, 0));
	tot.inPkts += st.inPkts;
	tot.inBytes += st.inBytes;
	tot.inUncBytes += st.inUncBytes;
	tot.outPkts += st.outPkts;
	tot.outBytes += st.outBytes;
	tot.outUncBytes += st.outUncBytes;
      }
      table.add(makeRow(tot, 1));
      start = getDaemon().getStartDate().getTime();
      if (TimeBase.msSince(start) >= Constants.HOUR) {
	table.add(makeRow(tot, 2));
      }
      return table;
    }

    NumberFormat rateFormat = new DecimalFormat("0");

    String rate(long n) {
      long ms = TimeBase.msSince(start);
      double phr = (double)n * (double)Constants.HOUR / (double)ms;
      return rateFormat.format(phr);
    }

    private Map makeRow(Stats stats, int special) {
      Map row = new HashMap();
      boolean isGrey = false;
      switch (special) {
      case 0:
	isGrey = isMuzzleMulticast && stats.multicast;
	StringBuffer sb = new StringBuffer();
	sb.append(stats.port);
	sb.append(stats.multicast ? " M" : " U");
	row.put("port", sb.toString());
	row.put("proto", new Integer(stats.proto));
	break;
      case 1:
	row.put("proto", "Total");
	row.put(StatusTable.ROW_SEPARATOR, "");
	break;
      case 2:
	row.put("proto", "per hour");
	break;
      }
      row.put("sort", new Integer(special));
      switch (special) {
      case 0:
      case 1:
	row.put("inPkts", new Integer(stats.inPkts));
	row.put("inBytes", new Long(stats.inBytes));
// 	row.put("inUncBytes", new Long(stats.inUncBytes));
	row.put("outPkts", greyObj(new Integer(stats.outPkts), isGrey));
	row.put("outBytes", greyObj(new Long(stats.outBytes), isGrey));
// 	row.put("outUncBytes", greyObj(new Long(stats.outUncBytes), isGrey));
	break;
      case 2:
	row.put("inPkts", rate(stats.inPkts));
	row.put("inBytes", rate(stats.inBytes));
// 	row.put("inUncBytes", rate(stats.inUncBytes));
	row.put("outPkts", rate(stats.outPkts));
	row.put("outBytes", rate(stats.outBytes));
// 	row.put("outUncBytes", rate(stats.outUncBytes));
	break;
      }
      return row;
    }

    Object greyObj(Object val, boolean isGrey) {
      if (!isGrey) return val;
      StatusTable.DisplayedValue dv = new StatusTable.DisplayedValue(val);
      dv.setColor("gray");
      return dv;
    }
  }
}
