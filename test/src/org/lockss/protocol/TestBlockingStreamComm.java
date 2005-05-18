/*
 * $Id: TestBlockingStreamComm.java,v 1.1 2005-05-18 05:44:19 tlipkis Exp $
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

import java.util.*;
import java.io.*;
import java.net.*;

import org.lockss.config.Configuration;
import org.lockss.daemon.*;
import org.lockss.plugin.*;
import org.lockss.util.*;
import org.lockss.util.Queue;
import org.lockss.test.*;

/**
 * This is the test class for org.lockss.protocol.BlockingStreamComm
 */
public class TestBlockingStreamComm extends LockssTestCase {
  public static Class testedClasses[] = {
    BlockingStreamComm.class,
    BlockingPeerChannel.class,
  };

  static Logger log = Logger.getLogger("SCommTest");

  static final int MAX_COMMS = 5;

  static final int HEADER_LEN = BlockingPeerChannel.HEADER_LEN;

  private MockLockssDaemon daemon;

  int[] testports = new int[MAX_COMMS];
  PeerIdentity[] pids = new PeerIdentity[MAX_COMMS];
  PeerAddress.Tcp[] pads = new PeerAddress.Tcp[MAX_COMMS];
  MyBlockingStreamComm[] comms = new MyBlockingStreamComm[MAX_COMMS];
  SimpleQueue[] rcvdMsgss = new SimpleQueue[MAX_COMMS];
  int testport1, testport2;
  PeerIdentity pid1, pid2;
  PeerAddress.Tcp pad1, pad2;
  MyBlockingStreamComm comm1, comm2;
  SimpleQueue rcvdMsgs1 = new SimpleQueue.Fifo();
  SimpleQueue rcvdMsgs2 = new SimpleQueue.Fifo();

  String testStr1 = "This is test data 1";
  String testStr2 = "This message contains a null \000 character";
  String testStr3 = "They that can give up essential liberty to obtain " +
    "a little temporary safety deserve neither liberty nor safety.";
  PeerMessage msg1, msg2, msg3;

  private byte[] rcvHeader = new byte[BlockingPeerChannel.HEADER_LEN];
  private byte[] sndHeader = new byte[BlockingPeerChannel.HEADER_LEN];
  private byte[] peerbuf = new byte[BlockingPeerChannel.MAX_PEERID_LEN];

  Properties cprops = new Properties();
  Configuration config;
  private IdentityManager idmgr;
  MySocketFactory fact;
  SimpleBinarySemaphore sem;
  boolean useInternalSockets = false;

  public void setUp() throws Exception {
    super.setUp();
    sem = new SimpleBinarySemaphore();
    daemon = getMockLockssDaemon();
    String tempDirPath = null;
    try {
      tempDirPath = getTempDir().getAbsolutePath() + File.separator;
    }
    catch (IOException ex) {
      fail("unable to create a temporary directory");
    }

    cprops.setProperty(IdentityManager.PARAM_IDDB_DIR, tempDirPath + "iddb");
    cprops.setProperty(IdentityManager.PARAM_LOCAL_IP, "127.0.0.1");
    ConfigurationUtil.setCurrentConfigFromProps(cprops);
    idmgr = new MyIdentityManager();
    daemon.setIdentityManager(idmgr);
    idmgr.initService(daemon);
    daemon.setDaemonInited(true);
//    idmgr.startService();
    fact = new MySocketFactory();
    config = Configuration.getCurrentConfig();
    setupMessages();
    useInternalSockets(false);
  }

  public void tearDown() throws Exception {
    for (int comm = 0; comm < MAX_COMMS; comm++) {
      stopComm(comm);
    }
    if (idmgr != null) {
      idmgr.stopService();
      idmgr = null;
    }
    super.tearDown();
    TimeBase.setReal();
  }

  void stopComm(int ix) {
    if (comms[ix] != null) {
      comms[ix].stopService();
      comms[ix] = null;
    }
  }

  void useInternalSockets(boolean flg) {
    useInternalSockets = flg;
  }

  void setupPid(int ix) throws IOException {
    if (useInternalSockets) {
      setupInternalPid(ix);
    } else {
      setupRealPid(ix);
    }
  }

  PeerIdentity findPeerId(String addr, int port) {
    String id = IdentityManager.ipAddrToKey(addr, port);
    return idmgr.findPeerIdentity(id);
  }

  void setupRealPid(int ix) throws IOException {
    testports[ix] = findUnboundTcpPort();
    pids[ix] = findPeerId("127.0.0.1", testports[ix]);
    pads[ix] = (PeerAddress.Tcp)pids[ix].getPeerAddress();
    peerhack(ix);
  }

  void peerhack(int ix) {
    switch (ix) {
    case 1:
      testport1 = testports[ix];
      pid1 = pids[ix];
      pad1 = pads[ix];
      break;
    case 2:
      testport2 = testports[ix];
      pid2 = pids[ix];
      pad2 = pads[ix];
      break;
    default:
    }
  }

  void setupInternalPid(int ix) throws IOException {
    InetAddress ip = InternalSocket.internalInetAddr;
    String addr = ip.getHostAddress();
    testports[ix] = InternalServerSocket.findUnboundPort(2000);
    pids[ix] = findPeerId(addr, testports[ix]);
    pads[ix] = (PeerAddress.Tcp)pids[ix].getPeerAddress();
    peerhack(ix);
  }

  void setupComm(int ix) throws IOException {
    if (pids[ix] == null) setupPid(ix);
    comms[ix] = new MyBlockingStreamComm(pids[ix]);
    daemon.setStreamCommManager(comms[ix]);
    comms[ix].initService(daemon);
    comms[ix].startService();
    rcvdMsgss[ix] = new SimpleQueue.Fifo();
    for (int proto = 1; proto <= 3; proto++) {
      comms[ix].registerMessageHandler(proto,
				       new MessageHandler(rcvdMsgss[ix]));
    }
    commhack(ix);
  }

  void commhack(int ix) {
    switch (ix) {
    case 1:
      comm1 = comms[ix];
      rcvdMsgs1 = rcvdMsgss[ix];
      break;
    case 2:
      comm2 = comms[ix];
      rcvdMsgs2 = rcvdMsgss[ix];
      break;
    default:
    }
  }

  void setupComm1() throws IOException {
    setupComm(1);
  }

  void setupComm2() throws IOException {
    setupComm(2);
  }

  void setupMessages() throws IOException {
    msg1 = makePeerMessage(1, testStr1);
    msg2 = makePeerMessage(2, testStr2);
    msg3 = makePeerMessage(3, testStr3);
  }


  PeerMessage makePeerMessage(int proto) throws IOException {
    PeerMessage pm = new MyMemoryPeerMessage();
    pm.setProtocol(proto);
    return pm;
  }

  PeerMessage makePeerMessage(int proto, String data) throws IOException {
    return makePeerMessage(proto, data.getBytes());
  }

  PeerMessage makePeerMessage(int proto, byte[] data) throws IOException {
    PeerMessage pm = makePeerMessage(proto);
    OutputStream os = pm.getOutputStream();
    os.write(data);
    os.close();
    return pm;
  }

  /** Assert that buf contains a valid header */
  public void assertHeader(byte[] buf) {
    assertEquals(PeerChannel.HEADER_CHECK, buf[PeerChannel.HEADER_OFF_CHECK]);
  }

  /** Assert that buf contains a valid header with expected op */
  public void assertHeaderOp(byte[] buf, int op) {
    assertHeader(buf);
    assertEquals(op, buf[PeerChannel.HEADER_OFF_OP]);
  }

  /** Assert that messages are the same except for sender id */
  public static void assertEqualsButSender(PeerMessage expected,
					   PeerMessage actual) {
    if (expected == actual ||
	(expected != null && expected.equalsButSender(actual))) {
      return;
    }
    failNotEquals(null, expected, actual);
  }

  /** Assert that message is same as expected, with expected sender */
  public void assertEqualsMessageFrom(PeerMessage expectedMsg,
				      PeerIdentity expectedPid,
				      PeerMessage actualMsg) {
    assertNotNull("Null message", actualMsg);
    assertEquals(expectedPid, actualMsg.getSender());
    assertEqualsButSender(expectedMsg, actualMsg);
  }

  public void testStateTrans() throws IOException {
    setupComm1();
    BlockingPeerChannel chan =
      new BlockingPeerChannel(comm1, pid1, null, null);
    chan.state = 0;
    assertFalse(chan.stateTrans(1, 2));
    assertEquals(0, chan.state);
    assertTrue(chan.stateTrans(0, 3));
    assertEquals(3, chan.state);
    assertTrue(chan.stateTrans(3, 4, "shouldn't"));
    assertEquals(4, chan.state);
    try {
      chan.stateTrans(3, 4, "should error");
      fail("stateTrans should have thrown");
    } catch (IllegalStateException e) {
    }
//     int[] lst = {2, 4, 6};
//     assertTrue(chan.stateTrans(lst, 6, "shouldn't"));
//     assertTrue(chan.stateTrans(lst, 8, "shouldn't"));
//     assertFalse(chan.stateTrans(lst, 8));
//     assertEquals(8, chan.state);
  }

  public void testNotStateTrans() throws IOException {
    setupComm1();
    BlockingPeerChannel chan =
      new BlockingPeerChannel(comm1, pid1, null, null);
    chan.state = 0;
    assertFalse(chan.notStateTrans(0, 2));
    assertEquals(0, chan.state);
    assertTrue(chan.notStateTrans(1, 3));
    assertEquals(3, chan.state);
    assertTrue(chan.notStateTrans(2, 5, "shouldn't"));
    assertEquals(5, chan.state);
    try {
      chan.notStateTrans(5, 4, "should error");
      fail("notStateTrans should have thrown");
    } catch (IllegalStateException e) {
    }
    assertEquals(5, chan.state);
    int[] lst = {2, 4, 6};
    assertTrue(chan.notStateTrans(lst, 3, "shouldn't"));
    assertTrue(chan.notStateTrans(lst, 4, "shouldn't"));
    assertFalse(chan.notStateTrans(lst, 8));
    assertEquals(4, chan.state);
  }

  public void testReadHeader() throws IOException {
    setupComm1();
    byte[] hdr = {(byte)0xff, 1, 1, 2, 3, 4, 2, 3, 4, 5};
    assertEquals(HEADER_LEN, hdr.length);
    byte[] buf = new byte[HEADER_LEN];
    InputStream ins = new ByteArrayInputStream(hdr);
    BlockingPeerChannel chan = new BlockingPeerChannel(comm1, pid1, ins, null);
    assertTrue(chan.readHeader());

    hdr[0] = 42;
    ins = new ByteArrayInputStream(hdr);
    chan = new BlockingPeerChannel(comm1, pid1, ins, null);
    try {
      chan.readHeader();
      fail("readHeader() of illegal header should throw");
    } catch (ProtocolException e) {
    }

    ins = new StringInputStream("");
    chan = new BlockingPeerChannel(comm1, pid1, ins, null);
    assertFalse(chan.readHeader());

    ins = new StringInputStream("1");
    chan = new BlockingPeerChannel(comm1, pid1, ins, null);
    try {
      chan.readHeader();
      fail("readHeader() of partial header should throw");
    } catch (ProtocolException e) {
    }
  }

  public void testWriteHeader() throws IOException {
    setupComm1();
    ByteArrayOutputStream outs = new ByteArrayOutputStream();
    BlockingPeerChannel chan = new BlockingPeerChannel(comm1, pid1,
						       null, outs);
    chan.writeHeader(5, 16, 34);
    byte[] hdr = outs.toByteArray();
    assertEquals(HEADER_LEN, hdr.length);
    assertEquals(0xff, ByteArray.decodeByte(hdr, 0));
    assertEquals(5, hdr[1]);
    assertEquals(16, ByteArray.decodeInt(hdr, 2));
    assertEquals(34, ByteArray.decodeInt(hdr, 6));

  }

  public void testIllSend() throws IOException {
    setupComm1();
    try {
      comm1.sendTo(null, pid2, null);
      fail("Null message should throw");
    } catch (NullPointerException e) {
    }
    try {
      comm1.sendTo(msg1, null, null);
      fail("Null peer should throw");
    } catch (NullPointerException e) {
    }
  }

  public void testRefused() throws IOException {
    setupComm1();
    setupPid(2);
    comm1.setDissocSem(sem);
    comm1.sendTo(msg1, pid2, null);
    if (!sem.take(TIMEOUT_SHOULDNT)) {
      fail("Refused channel didn't dissociate itself from comm1");
    }
    assertEmpty(comm1.channels);
    assertEmpty(comm1.rcvChannels);
  }

  public void testIncoming() throws IOException {
    setupComm1();
    Socket sock = new Socket(pad1.getIPAddr().getInetAddr(), pad1.getPort());
    SockAbort intr = abortIn(TIMEOUT_SHOULDNT, sock);
    InputStream ins = sock.getInputStream();
    StreamUtil.readBytes(ins, rcvHeader, HEADER_LEN);
    assertHeaderOp(rcvHeader, PeerChannel.OP_PEERID);
    intr.cancel();
  }

  public void testOrig() throws IOException {
    setupComm1();
    setupPid(2);
    ServerSocket server = new ServerSocket(pad2.getPort());
    SockAbort intr = abortIn(TIMEOUT_SHOULDNT, server);
    comm1.findOrMakeChannel(pid2);
    Socket sock = server.accept();
    InputStream ins = sock.getInputStream();
    SockAbort intr2 = abortIn(TIMEOUT_SHOULDNT, sock);
    StreamUtil.readBytes(ins, rcvHeader, HEADER_LEN);
    assertHeaderOp(rcvHeader, PeerChannel.OP_PEERID);
    int len = ByteArray.decodeInt(rcvHeader, 2);
    StreamUtil.readBytes(ins, peerbuf, len);
    String foo = new String(peerbuf, 0, len);
    assertEquals(pid1.getIdString(), foo);
    intr.cancel();
    intr2.cancel();
  }

  public void testSingleConnect() throws IOException {
    PeerMessage msgIn;
    setupComm1();
    setupComm2();
    comm1.sendTo(msg1, pid2, null);
    msgIn = (PeerMessage)rcvdMsgs2.get(TIMEOUT_SHOULDNT);
    assertEqualsMessageFrom(msg1, pid1, msgIn);
    // redundant checks
    assertEquals(1, msgIn.getProtocol());
    assertEquals(testStr1.length(), msgIn.getDataSize());
    assertEquals(testStr1.length(), msg1.getDataSize());

    // each comm should have only one channel
    assertEquals(1, comm1.channels.size());
    assertEquals(1, comm2.channels.size());
    assertEquals(0, comm1.rcvChannels.size());
    assertEquals(0, comm2.rcvChannels.size());

    assertTrue(rcvdMsgs1.isEmpty());
    assertTrue(rcvdMsgs2.isEmpty());

    comm1.sendTo(msg2, pid2, null);
    msgIn = (PeerMessage)rcvdMsgs2.get(TIMEOUT_SHOULDNT);
    assertEqualsMessageFrom(msg2, pid1, msgIn);

    comm2.sendTo(msg2, pid1, null);
    msgIn = (PeerMessage)rcvdMsgs1.get(TIMEOUT_SHOULDNT);
    assertNotNull(msgIn);
    assertEqualsMessageFrom(msg2, pid2, msgIn);

    comm2.sendTo(msg3, pid1, null);
    comm2.sendTo(msg1, pid1, null);
    comm1.sendTo(msg3, pid2, null);
    comm1.sendTo(msg2, pid2, null);

    // each comm should still have only one channel
    assertEquals(1, comm1.channels.size());
    assertEmpty(comm1.rcvChannels);
    assertEquals(1, comm2.channels.size());
    assertEmpty(comm2.rcvChannels);

    msgIn = (PeerMessage)rcvdMsgs1.get(TIMEOUT_SHOULDNT);
    assertEqualsMessageFrom(msg3, pid2, msgIn);
    msgIn = (PeerMessage)rcvdMsgs1.get(TIMEOUT_SHOULDNT);
    assertEqualsMessageFrom(msg1, pid2, msgIn);
    msgIn = (PeerMessage)rcvdMsgs2.get(TIMEOUT_SHOULDNT);
    assertEqualsMessageFrom(msg3, pid1, msgIn);
    msgIn = (PeerMessage)rcvdMsgs2.get(TIMEOUT_SHOULDNT);
    assertEqualsMessageFrom(msg2, pid1, msgIn);
  }

  // force delayed connect in one direction
  public void testSimultaneousConnect1() throws IOException {
    PeerMessage msgIn;
    SimpleBinarySemaphore sem2 = new SimpleBinarySemaphore();
    setupComm1();
    setupComm2();
    // delay comm2's accept()
    comm2.setAcceptSem(sem2);
    comm1.sendTo(msg1, pid2, null);
    comm2.sendTo(msg2, pid1, null);
    // both should have one connecting channel
    assertEquals(1, comm1.channels.size());
    assertEquals(1, comm2.channels.size());
    // comm1 should receive message
    msgIn = (PeerMessage)rcvdMsgs1.get(TIMEOUT_SHOULDNT);
    assertEqualsMessageFrom(msg2, pid2, msgIn);
    assertEquals(1, comm1.rcvChannels.size());
    // comm2 shouldn't
    assertTrue(rcvdMsgs2.isEmpty());
    sem2.give();
    // now comm2 should receive
    msgIn = (PeerMessage)rcvdMsgs2.get(TIMEOUT_SHOULDNT);
    assertEqualsMessageFrom(msg1, pid1, msgIn);
  }

  // force delayed connect in both directions
  public void testSimultaneousConnect2() throws IOException {
    PeerMessage msgIn;
    SimpleBinarySemaphore sem1 = new SimpleBinarySemaphore();
    SimpleBinarySemaphore sem2 = new SimpleBinarySemaphore();
    setupComm1();
    setupComm2();
    // delay accept() in both channels
    comm1.setAcceptSem(sem1);
    comm2.setAcceptSem(sem2);
    comm1.sendTo(msg1, pid2, null);
    comm2.sendTo(msg2, pid1, null);
    // both should have one connecting channel
    assertEquals(1, comm1.channels.size());
    assertEquals(1, comm2.channels.size());
    assertEquals(0, comm1.rcvChannels.size());
    assertEquals(0, comm2.rcvChannels.size());
    assertTrue(rcvdMsgs1.isEmpty());
    assertTrue(rcvdMsgs2.isEmpty());
    // allow comm1 accept to proceed, it should receive message
    sem1.give();
    msgIn = (PeerMessage)rcvdMsgs1.get(TIMEOUT_SHOULDNT);
    // comm1 only should have secondary channel
    assertEqualsMessageFrom(msg2, pid2, msgIn);
    assertEquals(1, comm1.channels.size());
    assertEquals(1, comm2.channels.size());
    assertEquals(1, comm1.rcvChannels.size());
    assertEquals(0, comm2.rcvChannels.size());
    // allow comm2 to proceed
    sem2.give();
    msgIn = (PeerMessage)rcvdMsgs2.get(TIMEOUT_SHOULDNT);
    assertEqualsMessageFrom(msg1, pid1, msgIn);
    assertEquals(1, comm1.channels.size());
    assertEquals(1, comm2.channels.size());
    assertEquals(1, comm1.rcvChannels.size());
    assertEquals(1, comm2.rcvChannels.size());
  }

  // allow channel to timeout and close
  public void testChannelCloseAfterTimeout() throws IOException {
    TimeBase.setSimulated(1000);
    PeerMessage msgIn;
    cprops.setProperty(BlockingStreamComm.PARAM_CHANNEL_IDLE_TIME, "5000");
    ConfigurationUtil.setCurrentConfigFromProps(cprops);
    setupComm1();
    setupComm2();
    comm1.setDissocSem(sem);
    comm1.sendTo(msg1, pid2, null);
    msgIn = (PeerMessage)rcvdMsgs2.get(TIMEOUT_SHOULDNT);
    comm2.sendTo(msg2, pid1, null);
    msgIn = (PeerMessage)rcvdMsgs1.get(TIMEOUT_SHOULDNT);
    TimeBase.step(4000);
    assertEquals(1, comm1.channels.size());
    TimeBase.step(2000);
    assertTrue("Channel didn't close automatically after timeout",
	       sem.take(TIMEOUT_SHOULDNT + 20000));
    assertEquals(0, comm1.channels.size());
    assertEquals(0, comm1.rcvChannels.size());
  }

  public void xtestMultipleChannels() throws IOException {
    for (int comm = 0; comm < MAX_COMMS; comm++) {
      setupComm(comm);
    }
    for (int comm = 0; comm < MAX_COMMS; comm++) {
      for (int peer = 0; peer < MAX_COMMS; peer++) {
	String data =
	  pids[comm].getIdString() + ">" + pids[peer].getIdString();
	PeerMessage pm = makePeerMessage(1, data);
	comms[comm].sendTo(pm, pids[peer], null);
      }
    }
//     TimerUtil.guaranteedSleep(1000);
//     DebugUtils.getInstance().threadDump();
    for (int comm = 0; comm < MAX_COMMS; comm++) {
      Set peers = allPeers();
      while (!peers.isEmpty()) {
	PeerMessage msgIn = (PeerMessage)rcvdMsgss[comm].get(TIMEOUT_SHOULDNT);
	assertNotNull("Comm" + comm + " didn't receive messages from " + peers,
		      msgIn);
	peers.remove(msgIn.getSender());
      }
    }
  }

  Set allPeers() {
    return SetUtil.fromArray(pids);
  }

  class MyBlockingStreamComm extends BlockingStreamComm {
    SocketFactory sockFact;
    PeerIdentity localId;
    SimpleBinarySemaphore dissocSem;
    SimpleBinarySemaphore acceptSem;

    MyBlockingStreamComm(PeerIdentity localId) {
      this.localId = localId;
    }

    SocketFactory getSocketFactory() {
      return fact;
    }

    protected PeerIdentity getLocalPeerIdentity() {
      return localId;
    }

    void dissociateChannelFromPeer(BlockingPeerChannel chan,
						PeerIdentity peer) {
      super.dissociateChannelFromPeer(chan, peer);
      if (dissocSem != null) {
	dissocSem.give();
      }
    }

    void processIncomingConnection(Socket sock) throws IOException {
      if (acceptSem != null) {
	acceptSem.take();
      }
      super.processIncomingConnection(sock);
    }

    void setDissocSem(SimpleBinarySemaphore sem) {
      dissocSem = sem;
    }
    void setAcceptSem(SimpleBinarySemaphore sem) {
      acceptSem = sem;
    }
  }

  static class MyBlockingPeerChannel extends BlockingPeerChannel {
    SimpleBinarySemaphore stopSem;

    MyBlockingPeerChannel(BlockingStreamComm scomm, PeerIdentity peer) {
      super(scomm, peer);
    }

    MyBlockingPeerChannel(BlockingStreamComm scomm, Socket sock) {
      super(scomm, sock);
    }

    void stopChannel() {
      super.stopChannel();
      if (stopSem != null) {
	stopSem.give();
      }
    }
    void setStopSem(SimpleBinarySemaphore sem) {
      stopSem = sem;
    }

  }


  int findUnboundTcpPort() {
    int start = 2000;
    for (int p = start; p < 65535; p++) {
      try {
	ServerSocket sock = new ServerSocket(p);
	sock.close();
	return p;
      } catch (IOException e) {
      }
    }
    log.error("Couldn't find unused TCP port");
    return -1;
  }


  class MessageHandler implements BlockingStreamComm.MessageHandler {
    SimpleQueue queue;
    public MessageHandler(SimpleQueue queue) {
      this.queue = queue;
    }
    public void handleMessage(PeerMessage msg) {
      log.debug("handleMessage(" + msg + ")");
      queue.put(msg);
    }
  }

  /** Mock socket factory creates LcapSockets with mock datagram/multicast
   * sockets. */
 class MySocketFactory implements BlockingStreamComm.SocketFactory {

    public ServerSocket newServerSocket(int port, int backlog)
	throws IOException {
      return (useInternalSockets ? new InternalServerSocket(port, backlog)
	      : new ServerSocket(port, backlog));
    }

    public Socket newSocket(IPAddr addr, int port) throws IOException {
      return (useInternalSockets ? new InternalSocket(addr.getInetAddr(), port)
	      : new Socket(addr.getInetAddr(), port));
    }

    public BlockingPeerChannel newPeerChannel(BlockingStreamComm comm,
					      Socket sock)
	throws IOException {
      return new MyBlockingPeerChannel(comm, sock);
    }

    public BlockingPeerChannel newPeerChannel(BlockingStreamComm comm,
					      PeerIdentity peer)
	throws IOException {
      return new MyBlockingPeerChannel(comm, peer);
    }

  }

  static class MyIdentityManager extends IdentityManager {
    public void storeIdentities() throws ProtocolException {
    }
  }

  // Suppress delete() because it prevents comparison with messages that
  // have been sent
  static class MyMemoryPeerMessage extends MemoryPeerMessage {
    boolean isDeleted = false;
    public void delete() {
      isDeleted = true;
    }
  }

  SockAbort abortIn(long inMs, Socket sock) {
    SockAbort sa = new SockAbort(inMs, sock);
    sa.start();
    return sa;
  }

  SockAbort abortIn(long inMs, ServerSocket sock) {
    SockAbort sa = new SockAbort(inMs, sock);
    if (Boolean.getBoolean("org.lockss.test.threadDump")) {
      sa.setThreadDump();
    }
    sa.start();
    return sa;
  }


  /** SockAbort aborts a socket by closing it and interrupting the thread */
  class SockAbort extends DoLater {
    Socket sock;
    ServerSocket servsock;

    SockAbort(long waitMs, Socket sock) {
      super(waitMs);
      this.sock = sock;
    }

    SockAbort(long waitMs, ServerSocket servsock) {
      super(waitMs);
      this.servsock = servsock;
    }

    protected void doit() {
      try {
	if (sock != null) {
	  sock.close();
	  log.debug("Closing sock");
	}
      } catch (IOException e) {
	log.warning("sock", e);
      }
      try {
	if (servsock != null) {
	  servsock.close();
	  log.debug("Closing servsock");
	}
      } catch (IOException e) {
	log.warning("servsock", e);
      }
    }
  }

}
