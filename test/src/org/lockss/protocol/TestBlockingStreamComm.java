/*
 * $Id: TestBlockingStreamComm.java,v 1.27 2009-07-22 06:40:21 tlipkis Exp $
 */

/*

Copyright (c) 2000-2008 Board of Trustees of Leland Stanford Jr. University,
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
import javax.net.*;
import javax.net.ssl.*;
import junit.framework.*;

import org.lockss.protocol.BlockingPeerChannel.ChannelState;
import org.lockss.protocol.BlockingStreamComm.PeerData;

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

  static Logger log = Logger.getLogger("TestBlockingStreamComm");

  // limit on number of BlockingStreamComm instances, and number created by
  // testMultipleChannels
  static final int MAX_COMMS = 5;

  static final int HEADER_LEN = PeerChannel.HEADER_LEN;
  static final int HEADER_CHECK = PeerChannel.HEADER_CHECK;
  static final int HEADER_OFF_CHECK = PeerChannel.HEADER_OFF_CHECK;
  static final int HEADER_OFF_OP = PeerChannel.HEADER_OFF_OP;
  static final int HEADER_OFF_LEN = PeerChannel.HEADER_OFF_LEN;
  static final int HEADER_OFF_PROTO = PeerChannel.HEADER_OFF_PROTO;
  static final byte OP_PEERID = PeerChannel.OP_PEERID;
  static final byte OP_DATA = PeerChannel.OP_DATA;

  // Arrays store multiple instances of test objects (ports, PeerIds,
  // queues, etc).  Usually used in parallel (comms[n] has local id
  // pids[n], on port testports[n]

  int[] testports = new int[MAX_COMMS];
  PeerIdentity[] pids = new PeerIdentity[MAX_COMMS];
  PeerAddress.Tcp[] pads = new PeerAddress.Tcp[MAX_COMMS];
  MyBlockingStreamComm[] comms = new MyBlockingStreamComm[MAX_COMMS];
  SimpleQueue[] rcvdMsgss = new SimpleQueue[MAX_COMMS];

  // Convenience equivalents to [1] and [2] entries in arrays
  int testport1, testport2;
  PeerIdentity pid1, pid2;
  PeerAddress.Tcp pad1, pad2;
  MyBlockingStreamComm comm1, comm2;
  SimpleQueue rcvdMsgs1, rcvdMsgs2;

  String testStr1 = "This is test data 1";
  String testStr2 = "This message contains a null \000 character";
  String testStr3 =
    "They that can give up essential liberty to obtain " +
    "a little temporary safety deserve neither liberty nor safety.";

  PeerMessage msg1, msg2, msg3;

  // buffers for manually sending to or receiving from an scomm under test
  private byte[] rcvHeader = new byte[HEADER_LEN];
  private byte[] peerbuf = new byte[BlockingPeerChannel.MAX_PEERID_LEN];

  private MockLockssDaemon daemon;
  private IdentityManager idmgr;

  Properties cprops;			// some tests add more to this and
					// reconfigure
  SimpleBinarySemaphore sem1;
  SimpleBinarySemaphore sem2;
  SimpleQueue assocQ, assocQ2;
  boolean useInternalSockets = false;
  protected boolean shutdownOutputSupported = true;

  TestBlockingStreamComm(String name) {
    super(name);
  }

  protected boolean isSsl() {
    return false;
  }

  void addSuiteProps(Properties p) {
  }

  public void setUp() throws Exception {
    super.setUp();
    sem1 = new SimpleBinarySemaphore();
    sem2 = new SimpleBinarySemaphore();
    assocQ = new SimpleQueue.Fifo();
    daemon = getMockLockssDaemon();
    String tempDirPath = null;
    try {
      tempDirPath = getTempDir().getAbsolutePath() + File.separator;
    }
    catch (IOException ex) {
      fail("unable to create a temporary directory");
    }
    cprops = new Properties();
    addSuiteProps(cprops);
    cprops.setProperty(IdentityManager.PARAM_IDDB_DIR, tempDirPath + "iddb");
    cprops.setProperty(IdentityManager.PARAM_LOCAL_IP, "127.0.0.1");
    ConfigurationUtil.setCurrentConfigFromProps(cprops);
    idmgr = new MyIdentityManager();
    daemon.setIdentityManager(idmgr);
    idmgr.initService(daemon);
    daemon.setDaemonInited(true);
//    idmgr.startService();
    setupMessages();
    useInternalSockets(false);
  }

  public void tearDown() throws Exception {
    for (int comm = 0; comm < MAX_COMMS; comm++) {
      Interrupter intr = null;
      try {
	intr = interruptMeIn(Math.max(BlockingStreamComm.DEFAULT_WAIT_EXIT + 2,
				      TIMEOUT_SHOULDNT));
	stopComm(comm);
	Thread.interrupted();		// clear interrupt flag
	if (intr.did()) {
	  log.warning("Timeout waiting for comm " + comm + " to exit");
	}
      } finally {
	if (intr != null) intr.cancel();
      }
    }
    if (idmgr != null) {
      idmgr.stopService();
      idmgr = null;
    }
    TimeBase.setReal();
    super.tearDown();
  }

  void stopComm(int ix) {
    if (comms[ix] != null) {
      comms[ix].stopService();
      comms[ix] = null;
    }
  }

  BlockingPeerChannel getChannel(BlockingStreamComm comm,
				 PeerIdentity pid) {
    PeerData pdata = comm.getPeerData(pid);
    if (pdata == null) {
      return null;
    }
    return pdata.getPrimaryChannel();
  }

  List<BlockingPeerChannel> getChannels(BlockingStreamComm comm) {
    List<BlockingPeerChannel> res = new ArrayList<BlockingPeerChannel>();
    for (PeerData pdata : comm.getAllPeerData()) {
      BlockingPeerChannel chan = pdata.getPrimaryChannel();
      if (chan != null) res.add(chan);
    }
    return res;
  }

  List<BlockingPeerChannel> getRcvChannels(BlockingStreamComm comm) {
    List<BlockingPeerChannel> res = new ArrayList<BlockingPeerChannel>();
    for (PeerData pdata : comm.getAllPeerData()) {
      BlockingPeerChannel chan = pdata.getSecondaryChannel();
      if (chan != null) res.add(chan);
    }
    return res;
  }



  void useInternalSockets(boolean flg) {
    useInternalSockets = flg;
  }

  PeerIdentity setupPid(int ix) throws IOException {
    if (useInternalSockets) {
      setupInternalPid(ix);
    } else {
      setupRealPid(ix);
    }
    return pids[ix];
  }

  /** Create a PeerIdentity for a port on localhost
   */
  void setupRealPid(int ix) throws IOException {
    if (pids[ix] == null) {
      testports[ix] = TcpTestUtil.findUnboundTcpPort();
      pids[ix] = findPeerId("127.0.0.1", testports[ix]);
      pads[ix] = (PeerAddress.Tcp)pids[ix].getPeerAddress();
      peerhack(ix);
    }
  }

  PeerIdentity findPeerId(String addr, int port)
      throws IdentityManager.MalformedIdentityKeyException {
    String id = IDUtil.ipAddrToKey(addr, port);
    return idmgr.findPeerIdentity(id);
  }

  /** Create a PeerIdentity for an InternalSocket
   */
  void setupInternalPid(int ix) throws IOException {
    if (pids[ix] == null) {
      InetAddress ip = InternalSocket.internalInetAddr;
      String addr = ip.getHostAddress();
      testports[ix] = InternalServerSocket.findUnboundPort(2000);
      pids[ix] = findPeerId(addr, testports[ix]);
      pads[ix] = (PeerAddress.Tcp)pids[ix].getPeerAddress();
      peerhack(ix);
    }
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

  /** Create and start BlockingStreamComm instance ix, register message
   * handlers for it
   */
   void setupComm(int ix) throws IOException {
    if (pids[ix] == null) setupPid(ix);
    setupCommArrayEntry(ix);
    comms[ix].initService(daemon);
    comms[ix].startService();
    rcvdMsgss[ix] = new SimpleQueue.Fifo();
    for (int proto = 1; proto <= 3; proto++) {
      comms[ix].registerMessageHandler(proto,
				       new MessageHandler(rcvdMsgss[ix]));
    }
    commhack(ix);
  }

  void setupCommArrayEntry(int ix) {
    comms[ix] = new MyBlockingStreamComm(pids[ix]);
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

  PeerMessage makePeerMessage(int proto, String data, int rpt)
      throws IOException {
    PeerMessage pm = makePeerMessage(proto);
    byte[] bdata = data.getBytes();
    OutputStream os = pm.getOutputStream();
    for (int ix = rpt; ix > 0; ix--) {
      os.write(bdata);
    }
    os.close();
    return pm;
  }

  /** Write a peerid message to an output stream */
  void writePeerId(OutputStream outs, PeerIdentity pid) throws IOException {
    writePeerId(outs, pid.getIdString());
  }

  /** Write a peerid message to an output stream */
  void writePeerId(OutputStream outs, String idstr) throws IOException {
    writeHeader(outs, OP_PEERID, idstr.length(), 0);
    outs.write(idstr.getBytes());
    outs.flush();
  }

  /** Write a message header to an output stream */
  void writeHeader(OutputStream outs, int op, int len, int proto)
      throws IOException {
    byte[] sndHeader = new byte[HEADER_LEN];
    sndHeader[HEADER_OFF_CHECK] = HEADER_CHECK;
    sndHeader[HEADER_OFF_OP] = (byte)op;
    ByteArray.encodeLong(len, sndHeader, HEADER_OFF_LEN);
    ByteArray.encodeInt(proto, sndHeader, HEADER_OFF_PROTO);
    outs.write(sndHeader);
  }

  /** Read header from input stream, check that it's a message header */
  public void assertRcvHeader(InputStream ins, int op) throws IOException {
    StreamUtil.readBytes(ins, rcvHeader, HEADER_LEN);
    assertHeaderOp(rcvHeader, op);
  }

  /** Read message data from input stream, return as String */
  String rcvMsgData(InputStream ins) throws IOException {
    long len = ByteArray.decodeLong(rcvHeader, 2);
    assertTrue("len: " + len, len <= Integer.MAX_VALUE);
    StreamUtil.readBytes(ins, peerbuf, (int)len);
    return new String(peerbuf, 0, (int)len);
  }

  /** Assert that buf contains a valid header */
  public void assertHeader(byte[] buf) {
    assertEquals(HEADER_CHECK, buf[HEADER_OFF_CHECK]);
  }

  /** Assert that buf contains a valid header with expected op */
  public void assertHeaderOp(byte[] buf, int op) {
    assertHeader(buf);
    assertEquals(op, buf[HEADER_OFF_OP]);
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

  // Tests of BlockingPeerChannel

  // Assumes that ssl test enables clientAuth, which may change
  public void testIsTrusted() throws IOException {
    setupComm1();
    if (isSsl()) {
      assertTrue(comm1.isTrustedNetwork());
    } else {
      assertFalse(comm1.isTrustedNetwork());
    }
  }

  public void testStateTrans() throws IOException {
    setupComm1();
    BlockingPeerChannel chan =
      new BlockingPeerChannel(comm1, pid1, null, null);
    assertEquals(ChannelState.INIT, chan.getState());
    assertFalse(chan.stateTrans(ChannelState.CONNECTING,
				ChannelState.CONNECT_FAIL));
    assertEquals(ChannelState.INIT, chan.getState());
    assertTrue(chan.stateTrans(ChannelState.INIT, ChannelState.ACCEPTED));
    assertEquals(ChannelState.ACCEPTED, chan.getState());
    assertTrue(chan.stateTrans(ChannelState.ACCEPTED, ChannelState.STARTING,
			       "shouldn't"));
    assertEquals(ChannelState.STARTING, chan.getState());
    try {
      chan.stateTrans(ChannelState.ACCEPTED, ChannelState.STARTING,
		      "should error");
      fail("stateTrans should have thrown");
    } catch (IllegalStateException e) {
    }
    // array version of stateTrans() nyi
//     int[] lst = {ChannelState.CONNECT_FAIL, ChannelState.STARTING,
// 		 ChannelState.DRAIN_INPUT};
//     assertTrue(chan.stateTrans(lst, ChannelState.DRAIN_INPUT, "shouldn't"));
//     assertTrue(chan.stateTrans(lst, 8, "shouldn't"));
//     assertFalse(chan.stateTrans(lst, 8));
//     assertEquals(8, chan.getState());
  }

  public void testNotStateTrans() throws IOException {
    setupComm1();
    BlockingPeerChannel chan =
      new BlockingPeerChannel(comm1, pid1, null, null);
    assertEquals(ChannelState.INIT, chan.getState());
    assertFalse(chan.notStateTrans(ChannelState.INIT,
				   ChannelState.CONNECT_FAIL));
    assertEquals(ChannelState.INIT, chan.getState());
    assertTrue(chan.notStateTrans(ChannelState.CONNECTING,
				  ChannelState.ACCEPTED));
    assertEquals(ChannelState.ACCEPTED, chan.getState());
    assertTrue(chan.notStateTrans(ChannelState.CONNECT_FAIL, ChannelState.OPEN,
				  "shouldn't"));
    assertEquals(ChannelState.OPEN, chan.getState());
    try {
      chan.notStateTrans(ChannelState.OPEN, ChannelState.STARTING,
			 "should error");
      fail("notStateTrans should have thrown");
    } catch (IllegalStateException e) {
    }
    assertEquals(ChannelState.OPEN, chan.getState());
    ChannelState[] lst = {ChannelState.CONNECT_FAIL,
			  ChannelState.STARTING,
			  ChannelState.DRAIN_INPUT};
    assertTrue(chan.notStateTrans(lst, ChannelState.ACCEPTED, "shouldn't"));
    assertTrue(chan.notStateTrans(lst, ChannelState.STARTING, "shouldn't"));
    assertFalse(chan.notStateTrans(lst, ChannelState.CLOSING));
    assertEquals(ChannelState.STARTING, chan.getState());
  }

  public void testReadHeader() throws IOException {
    setupComm1();
    byte[] hdr = {(byte)0xff, 1, 1, 2, 3, 4, 5, 6, 7, 8, 2, 3, 4, 5};
    assertEquals(HEADER_LEN, hdr.length);
    byte[] buf = new byte[HEADER_LEN];
    InputStream ins = new ByteArrayInputStream(hdr);
    BlockingPeerChannel chan = new BlockingPeerChannel(comm1, pid1, ins, null);
    long t0 = chan.getLastActiveTime();
    assertTrue(chan.readHeader());
    // ensure using a readbytes() that updates the time
    assertNotEquals(t0, chan.getLastActiveTime());

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

    ins = new StringInputStream("\177");
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
    assertEquals(16, ByteArray.decodeLong(hdr, 2));
    assertEquals(34, ByteArray.decodeInt(hdr, 10));

  }

  // BlockingStreamComm functional tests

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
    List event;
    setupComm1();
    setupPid(2);
    comm1.setAssocQueue(assocQ);
    comm1.sendTo(msg1, pid2, null);
    assertNotNull("Connecting channel didn't dissociate",
		  (event = (List)assocQ.get(TIMEOUT_SHOULDNT)));
    assertEquals("Connecting channel didn't dissociate",
		 "dissoc", event.get(0));
    assertEmpty(getChannels(comm1));
    assertEmpty(getRcvChannels(comm1));

    PeerData pdata = comm1.getPeerData(pid2);
    assertNotNull(pdata);
    assertFalse(pdata.isRetryNeeded());
  }

  public void testRefusedRetry() throws IOException {
    TimeBase.setSimulated(10000);
    cprops.put(BlockingStreamComm.PARAM_RETRY_BEFORE_EXPIRATION, "0");
    ConfigurationUtil.setCurrentConfigFromProps(cprops);
    List event;
    setupComm1();
    setupPid(2);
    comm1.setAssocQueue(assocQ);
    msg1.setExpiration(11000);
    comm1.sendTo(msg1, pid2, null);
    assertNotNull("Connecting channel didn't dissociate",
		  (event = (List)assocQ.get(TIMEOUT_SHOULDNT)));
    assertEquals("Connecting channel didn't dissociate",
		 "dissoc", event.get(0));
    assertEmpty(getChannels(comm1));
    assertEmpty(getRcvChannels(comm1));

    PeerData pdata = comm1.getPeerData(pid2);
    assertNotNull(pdata);
    assertTrue(pdata.isRetryNeeded());
    assertEquals(1, pdata.sendQueue.size());
  }

  // Cause connect to fail with SSLException instead of Refused - shouldn't
  // retry
  public void testSSLFailNoRetry() throws IOException {
    TimeBase.setSimulated(10000);
    cprops.put(BlockingStreamComm.PARAM_RETRY_BEFORE_EXPIRATION, "0");
    ConfigurationUtil.setCurrentConfigFromProps(cprops);
    List event;
    setupComm1();
    MySocketFactory msf = (MySocketFactory)comm1.getSocketFactory();
    msf.setNewSocketThrow(new SSLPeerUnverifiedException("Fake SSL exception"));
    setupPid(2);
    comm1.setAssocQueue(assocQ);
    msg1.setExpiration(11000);
    comm1.sendTo(msg1, pid2, null);
    assertNotNull("Connecting channel didn't dissociate",
		  (event = (List)assocQ.get(TIMEOUT_SHOULDNT)));
    assertEquals("Connecting channel didn't dissociate",
		 "dissoc", event.get(0));
    assertEmpty(getChannels(comm1));
    assertEmpty(getRcvChannels(comm1));

    PeerData pdata = comm1.getPeerData(pid2);
    assertNotNull(pdata);
    assertFalse(pdata.isRetryNeeded());
    assertNull(pdata.sendQueue);
  }

  public void testIncoming() throws IOException {
    setupComm1();
    Interrupter intr1 = null;
    SockAbort intr2 = null;
    try {
      intr1 = interruptMeIn(TIMEOUT_SHOULDNT);
      BlockingStreamComm.SocketFactory sf = comm1.getSocketFactory();
      Socket sock = sf.newSocket(pad1.getIPAddr(), pad1.getPort());
      intr2 = abortIn(TIMEOUT_SHOULDNT, sock);
      InputStream ins = sock.getInputStream();
      StreamUtil.readBytes(ins, rcvHeader, HEADER_LEN);
      assertHeaderOp(rcvHeader, OP_PEERID);
      assertEquals(pid1.getIdString(), rcvMsgData(ins));
    } finally {
      if (intr1 != null) intr1.cancel();
      if (intr2 != null) intr2.cancel();
    }
  }

  public void testIncomingRcvPeerId(String peerid, boolean isGoodId)
      throws IOException {
    log.debug("Incoming rcv pid " + peerid);
    setupComm1();
    Interrupter intr1 = null;
    SockAbort intr2 = null;
    try {
      intr1 = interruptMeIn(TIMEOUT_SHOULDNT);
      BlockingStreamComm.SocketFactory sf = comm1.getSocketFactory();
      Socket sock = sf.newSocket(pad1.getIPAddr(), pad1.getPort());
      intr2 = abortIn(TIMEOUT_SHOULDNT, sock);
      InputStream ins = sock.getInputStream();
      OutputStream outs = sock.getOutputStream();
      StreamUtil.readBytes(ins, rcvHeader, HEADER_LEN);
      assertHeaderOp(rcvHeader, OP_PEERID);
      assertEquals(pid1.getIdString(), rcvMsgData(ins));
      comm1.setAssocQueue(assocQ);
      writePeerId(outs, peerid);
      List event;
      if (isGoodId) {
        assertNotNull("Connecting channel didn't associate",
  		    (event = (List)assocQ.get(TIMEOUT_SHOULDNT)));
        assertEquals("Connecting channel didn't associate",
  		   "assoc", event.get(0));
        assertEquals(1, getChannels(comm1).size());
        assertEquals(0, getRcvChannels(comm1).size());
      } else {
        assertNotNull("Connecting channel didn't dissociate",
  		    (event = (List)assocQ.get(TIMEOUT_SHOULDNT)));
        assertEquals("Connecting channel didn't dissociate",
  		   "dissoc", event.get(0));
        assertEquals(0, getChannels(comm1).size());
        assertEquals(0, getRcvChannels(comm1).size());
      }
    } finally {
      if (intr1 != null) intr1.cancel();
      if (intr2 != null) intr2.cancel();
    }
  }

  public void testIncomingRcvGoodPeerId1() throws IOException {
    setupPid(2);
    testIncomingRcvPeerId(pid2.getIdString(), true);
  }

  public void testIncomingRcvBadPeerId1() throws IOException {
    // illegal tcp port
    testIncomingRcvPeerId("tcp:[127.0.0.1]:65541", false);
  }

  public void testIncomingRcvBadPeerId2() throws IOException {
    // V1 (non-stream) id
    testIncomingRcvPeerId("127.0.0.1", false);
  }

  public void testOriginate() throws IOException {
    SockAbort intr1 = null;
    SockAbort intr2 = null;
    try {
      setupComm1();
      setupPid(2);
      BlockingStreamComm.SocketFactory sf = comm1.getSocketFactory();
      ServerSocket server = sf.newServerSocket(pad2.getPort(), 3);
      intr1 = abortIn(TIMEOUT_SHOULDNT, server);
      comm1.findOrMakeChannel(pid2);
      Socket sock = server.accept();
      InputStream ins = sock.getInputStream();
      intr2 = abortIn(TIMEOUT_SHOULDNT, sock);
      assertRcvHeader(ins, OP_PEERID);
      assertEquals(pid1.getIdString(), rcvMsgData(ins));
      IOUtil.safeClose(server);
      IOUtil.safeClose(sock);
    } finally {
      if (intr1 != null) intr1.cancel();
      if (intr2 != null) intr2.cancel();
    }
  }

  // No longer possible as can't create bad PeerId
//   public void testOriginateToBadPeerId() throws IOException {
//     setupComm1();
//     PeerIdentity bad1 = findPeerId("127.0.0.1", 100000);
//     try {
//       comm1.sendTo(msg1, bad1, null);
//       fail("sendTo(..., " + bad1 + ") should throw");
//     } catch (IdentityManager.MalformedIdentityKeyException e) {
//     }
//   }

  public void testOriginateRcvPeerId(String peerid, boolean isGoodId)
      throws IOException {
    log.debug("Orig, send pid " + peerid);
    SockAbort intr1 = null;
    try {
      setupComm1();
      comm1.setAssocQueue(assocQ);
      setupPid(2);
      log.debug2("Listening on " + pad2.getPort());
      BlockingStreamComm.SocketFactory sf = comm1.getSocketFactory();
      ServerSocket server = sf.newServerSocket(pad2.getPort(), 3);
      intr1 = abortIn(TIMEOUT_SHOULDNT, server);
      comm1.findOrMakeChannel(pid2);
      Socket sock = server.accept();
      InputStream ins = sock.getInputStream();
      OutputStream outs = sock.getOutputStream();
      assertRcvHeader(ins, OP_PEERID);
      assertEquals(pid1.getIdString(), rcvMsgData(ins));
      writePeerId(outs, peerid);
      List event;
      if (isGoodId) {
        assertNull("Connecting channel shouldn't call associate",
  		 assocQ.get(TIMEOUT_SHOULD));
        assertEquals(1, getChannels(comm1).size());
        assertEquals(0, getRcvChannels(comm1).size());
      } else {
        assertNotNull("Connecting channel didn't dissociate",
  		    (event = (List)assocQ.get(TIMEOUT_SHOULDNT)));
        assertEquals("Connecting channel didn't dissociate",
  		   "dissoc", event.get(0));
        assertEquals(0, getChannels(comm1).size());
        assertEquals(0, getRcvChannels(comm1).size());
      }
      IOUtil.safeClose(server);
      IOUtil.safeClose(sock);
    } finally {
      if (intr1 != null) intr1.cancel();
    }
  }

  public void testOriginateRcvGoodPeerId1() throws IOException {
    setupPid(2);
    testOriginateRcvPeerId(pid2.getIdString(), true);
  }

  public void testOriginateRcvBadPeerId1() throws IOException {
    // illegal tcp port
    testOriginateRcvPeerId("tcp:[127.0.0.1]:65542", false);
  }

  // any illegal pid sent to originator causes conflict (can't be equal to
  // actual address used to open conn) before legality check, so this is
  // equivalent to the previous test
  public void testOriginateRcvBadPeerId2() throws IOException {
    // V1 (non-stream) id
    testOriginateRcvPeerId("127.0.0.1", false);
  }

  // Don't know how to test this without actually waiting for socket to timeout
  public void XXXtestHangingClose() throws IOException {
    TimeBase.setSimulated(1000);
    cprops.setProperty(BlockingStreamComm.PARAM_CHANNEL_IDLE_TIME, "5000");
    ConfigurationUtil.setCurrentConfigFromProps(cprops);
    setupComm1();
    setupPid(2);
    Interrupter intr1 = null;
    SockAbort intr2 = null;
    try {
      intr1 = interruptMeIn(TIMEOUT_SHOULDNT);
      BlockingStreamComm.SocketFactory sf = comm1.getSocketFactory();
      Socket sock = sf.newSocket(pad1.getIPAddr(), pad1.getPort());
      intr2 = abortIn(TIMEOUT_SHOULDNT, sock);
      InputStream ins = sock.getInputStream();
      OutputStream outs = sock.getOutputStream();
      StreamUtil.readBytes(ins, rcvHeader, HEADER_LEN);
      assertHeaderOp(rcvHeader, OP_PEERID);
      assertEquals(pid1.getIdString(), rcvMsgData(ins));
      comm1.setAssocQueue(assocQ);
      writePeerId(outs, pid2);
      // wait for it to get peerid before sending to it
      List event;
      assertNotNull("Channel didn't assoc",
  		  (event = (List)assocQ.get(TIMEOUT_SHOULDNT)));
      assertEquals("Channel didn't assoc",
  		 "assoc", event.get(0));
      assertEquals(1, getChannels(comm1).size());
      msg1 = makePeerMessage(1, "1234567890123456789012345678901234567890", 100);
      long msgsize = msg1.getDataSize();
      int tobuffer = 20 * (sock.getReceiveBufferSize() +
  			sock.getSendBufferSize());
      for (long bytes = 0; bytes < tobuffer; bytes += msgsize) {
//       comm1.sendTo(msg1, pid2, null);
      }
      assertEquals(1, getChannels(comm1).size());
      BlockingPeerChannel chan = (BlockingPeerChannel)getChannel(comm1, pid2);
      assertNotNull("Didn't find expected channel", chan);
//     assertFalse("Send queue shouldn't be empty", chan.isSendIdle());
      assertEquals(0, getRcvChannels(comm1).size());
      TimeBase.step(6000);
      assertEquals(0, ins.available());
      assertNotNull("Channel didn't close automatically after timeout",
  		  (event = (List)assocQ.get(TIMEOUT_SHOULDNT)));
      assertEquals("Channel didn't close automatically after timeout",
  		 "dissoc", event.get(0));
      assertFalse(intr1.did());
      assertFalse(intr2.did());
    } finally {
      if (intr1 != null) intr1.cancel();
      if (intr2 != null) intr2.cancel();
    }
  }

  public void testHangingSend() throws IOException {
    Interrupter intr1 = null;
    SockAbort intr2 = null;
    try {
      TimeBase.setSimulated(1000);
      cprops.setProperty(BlockingStreamComm.PARAM_CHANNEL_IDLE_TIME, "5000");
      ConfigurationUtil.setCurrentConfigFromProps(cprops);
      setupComm1();
      setupPid(2);
      intr1 = interruptMeIn(TIMEOUT_SHOULDNT);
      BlockingStreamComm.SocketFactory sf = comm1.getSocketFactory();
      Socket sock = sf.newSocket(pad1.getIPAddr(), pad1.getPort());
      intr2 = abortIn(TIMEOUT_SHOULDNT * 2, sock);
      InputStream ins = sock.getInputStream();
      OutputStream outs = sock.getOutputStream();
      StreamUtil.readBytes(ins, rcvHeader, HEADER_LEN);
      assertHeaderOp(rcvHeader, OP_PEERID);
      assertEquals(pid1.getIdString(), rcvMsgData(ins));
      comm1.setAssocQueue(assocQ);
      writePeerId(outs, pid2);
      // wait for it to get peerid before sending to it
      List event;
      assertNotNull("Channel didn't assoc",
  		  (event = (List)assocQ.get(TIMEOUT_SHOULDNT)));
      assertEquals("Channel didn't assoc",
  		 "assoc", event.get(0));
      assertEquals(1, getChannels(comm1).size());
      // a 30KB message
      msg1 = makePeerMessage(1, "123456789012345678901234567890", 1000);
      long msgsize = msg1.getDataSize();
      int tobuffer = 200 * (sock.getReceiveBufferSize() +
  			sock.getSendBufferSize());
      // Send lots of data to ensure send thread blocks waiting for socket to
      // have buffer space
      for (long bytes = 0; bytes < tobuffer; bytes += msgsize) {
        comm1.sendTo(msg1, pid2, null);
      }
      assertEquals(1, getChannels(comm1).size());
      BlockingPeerChannel chan = (BlockingPeerChannel)getChannel(comm1, pid2);
      assertNotNull("Didn't find expected channel", chan);
      assertFalse("Send queue shouldn't be empty", chan.isSendIdle());
      assertEquals(0, getRcvChannels(comm1).size());
  
      // give hung checker a chance to run.  If fails, interrupter will stop us
      while (null == (event = (List)assocQ.get(10))) {
        TimeBase.step(6000);
      }
      assertNotNull("Channel didn't close automatically after timeout", event);
      assertEquals("Channel didn't close automatically after timeout",
  		 "dissoc", event.get(0));
      assertFalse(intr1.did());
      assertFalse(intr2.did());
    } finally {
      if (intr1 != null) intr1.cancel();
      if (intr2 != null) intr2.cancel();
    }
  }

  public void testFileMessage() throws IOException {
    cprops.setProperty(BlockingStreamComm.PARAM_MIN_FILE_MESSAGE_SIZE, "1000");
    ConfigurationUtil.setCurrentConfigFromProps(cprops);

    PeerMessage msgIn;
    setupComm1();
    setupComm2();
    msg2 = makePeerMessage(1, "1234567890123456789012345678901234567890", 100);
    comm1.sendTo(msg1, pid2, null);
    comm1.sendTo(msg2, pid2, null);
    msgIn = (PeerMessage)rcvdMsgs2.get(TIMEOUT_SHOULDNT);
    try {
      assertEqualsMessageFrom(msg1, pid1, msgIn);
      assertTrue(msgIn.toString(), msgIn instanceof MemoryPeerMessage);
      msgIn = (PeerMessage)rcvdMsgs2.get(TIMEOUT_SHOULDNT);
      assertEqualsMessageFrom(msg2, pid1, msgIn);
      assertTrue(msgIn.toString(), msgIn instanceof FilePeerMessage);
    } finally {
      msgIn.delete();
    }
  }

  public void testMemMessage() throws IOException {
    cprops.setProperty(BlockingStreamComm.PARAM_MIN_FILE_MESSAGE_SIZE, "5000");
    ConfigurationUtil.setCurrentConfigFromProps(cprops);

    PeerMessage msgIn;
    setupComm1();
    setupComm2();
    msg2 = makePeerMessage(1, "1234567890123456789012345678901234567890", 10);
    comm1.sendTo(msg1, pid2, null);
    comm1.sendTo(msg2, pid2, null);
    msgIn = (PeerMessage)rcvdMsgs2.get(TIMEOUT_SHOULDNT);
    try {
      assertEqualsMessageFrom(msg1, pid1, msgIn);
      assertTrue(msgIn.toString(), msgIn instanceof MemoryPeerMessage);
      msgIn = (PeerMessage)rcvdMsgs2.get(TIMEOUT_SHOULDNT);
      assertEqualsMessageFrom(msg2, pid1, msgIn);
      assertTrue(msgIn.toString(), msgIn instanceof MemoryPeerMessage);
    } finally {
      msgIn.delete();
    }
  }

  public void testTooLargeMsg() throws IOException {
    cprops.setProperty(BlockingStreamComm.PARAM_MAX_MESSAGE_SIZE, "2000");
    ConfigurationUtil.setCurrentConfigFromProps(cprops);

    PeerMessage msgIn;
    setupComm1();
    setupComm2();
    List event;
    comm2.setAssocQueue(assocQ);
    comm1.sendTo(msg1, pid2, null);
    msg2 = makePeerMessage(1, "1234567890123456789012345678901234567890", 100);
    msgIn = (PeerMessage)rcvdMsgs2.get(TIMEOUT_SHOULDNT);
    assertEqualsMessageFrom(msg1, pid1, msgIn);
    assertNotNull("Channel didn't associate",
		  (event = (List)assocQ.get(TIMEOUT_SHOULDNT)));
    assertEquals("Channel didn't associate",
		 "assoc", event.get(0));
    comm1.sendTo(msg2, pid2, null);
    assertNotNull("Channel didn't close on too-large message",
		  (event = (List)assocQ.get(TIMEOUT_SHOULDNT)));
    assertEquals("Channel didn't close automatically after timeout",
		 "dissoc", event.get(0));
  }

  // Connect reqtry & msg queueing mechanism

  public void testWaitingPeerTreeSet() throws IOException {
    setupPid(1);
    setupPid(2);
    PeerIdentity pid3 = setupPid(3);
    PeerIdentity pid4 = setupPid(4);
    setupComm1();
    PeerData pd1 = comm1.makePeerData(pid1);
    PeerData pd2 = comm1.makePeerData(pid2);
    PeerData pd3 = comm1.makePeerData(pid3);
    PeerData pd4 = comm1.makePeerData(pid4);
    pd1.nextRetry = 1000;
    pd2.nextRetry = 100;
    pd3.nextRetry = 500;
    pd4.nextRetry = 500;    // time equal to pd3, TreeSet comparator should
			    // treat as distinct
    comm1.addPeerToRetry(pd1);
    assertIsomorphic(ListUtil.list(pd1), comm1.peersToRetry);
    comm1.addPeerToRetry(pd2);
    assertIsomorphic(ListUtil.list(pd2, pd1), comm1.peersToRetry);
    comm1.addPeerToRetry(pd2);
    assertIsomorphic(ListUtil.list(pd2, pd1), comm1.peersToRetry);
    comm1.addPeerToRetry(pd3);
    assertIsomorphic(ListUtil.list(pd2, pd3, pd1), comm1.peersToRetry);
    comm1.addPeerToRetry(pd4);
    assertEquals(4, comm1.peersToRetry.size());
    assertSame(pd2, comm1.peersToRetry.first());
  }

  // Simple retry test
  public void testRetry1() throws IOException {
    TimeBase.setSimulated(1000);
    cprops.put(BlockingStreamComm.PARAM_MIN_FILE_MESSAGE_SIZE, "5000");
    cprops.put(BlockingStreamComm.PARAM_MAX_PEER_RETRY_INTERVAL, "1000");
    cprops.put(BlockingStreamComm.PARAM_MIN_PEER_RETRY_INTERVAL, "10");
    cprops.put(BlockingStreamComm.PARAM_RETRY_BEFORE_EXPIRATION, "100");
    ConfigurationUtil.setCurrentConfigFromProps(cprops);

    List event;
    PeerMessage msgIn;
    setupComm1();
    setupPid(2);
    comm1.setAssocQueue(assocQ);
    msg1.setExpiration(3000);
    assertEquals(0, msg1.getRetryCount());
    comm1.sendTo(msg1, pid2, null);
    assertNotNull("Connecting channel didn't dissociate",
		  (event = (List)assocQ.get(TIMEOUT_SHOULDNT)));
    assertEquals("Connecting channel didn't dissociate",
		 "dissoc", event.get(0));

    PeerData pdata = comm1.getPeerData(pid2);
    assertNotNull(pdata);
    assertNull(pdata.getPrimaryChannel());
    assertNull(pdata.getSecondaryChannel());
    assertTrue(pdata.isRetryNeeded());
    assertEquals(1, msg1.getRetryCount());
    setupComm2();
    // Shouldn't retry until 1000
    TimeBase.step(800);
    assertTrue(pdata.isRetryNeeded());
    TimeBase.step(300);
    msgIn = (PeerMessage)rcvdMsgs2.get(TIMEOUT_SHOULDNT);
    assertEqualsMessageFrom(msg1, pid1, msgIn);
    assertTrue(msgIn.toString(), msgIn instanceof MemoryPeerMessage);
    assertEquals(1, msg1.getRetryCount());
  }

  // Ensure 2nd msg gets queued if already waiting for retry
  public void testRetry2() throws IOException {
    TimeBase.setSimulated(1000);
    cprops.put(BlockingStreamComm.PARAM_MIN_FILE_MESSAGE_SIZE, "5000");
    cprops.put(BlockingStreamComm.PARAM_MAX_PEER_RETRY_INTERVAL, "1000");
    cprops.put(BlockingStreamComm.PARAM_MIN_PEER_RETRY_INTERVAL, "10");
    cprops.put(BlockingStreamComm.PARAM_RETRY_BEFORE_EXPIRATION, "100");
    ConfigurationUtil.setCurrentConfigFromProps(cprops);
    msg2 = makePeerMessage(1, "1234567890123456789012345678901234567890", 20);
    msg1.setExpiration(3000);
    msg2.setExpiration(4000);

    List event;
    PeerMessage msgIn;
    setupComm1();
    setupPid(2);
    comm1.setAssocQueue(assocQ);
    comm1.sendTo(msg1, pid2, null);
    PeerData pdata = comm1.getPeerData(pid2);
    int sendRpt = pdata.lastSendRpt;
    assertNotNull("Connecting channel didn't dissociate",
		  (event = (List)assocQ.get(TIMEOUT_SHOULDNT)));
    assertEquals("Connecting channel didn't dissociate",
		 "dissoc", event.get(0));

    assertNotNull(pdata);
    assertNull(pdata.getPrimaryChannel());
    assertNull(pdata.getSecondaryChannel());
    assertEquals(sendRpt, pdata.getOrigCnt());
    assertEquals(sendRpt, pdata.getFailCnt());
    assertEquals(0, pdata.getAcceptCnt());
    assertTrue(pdata.isRetryNeeded());
    setupComm2();
    TimeBase.step(800);
    assertNull("Retried too early", (List)assocQ.get(TIMEOUT_SHOULD));
    assertTrue(pdata.isRetryNeeded());
    comm1.sendTo(msg2, pid2, null);
    assertTrue(pdata.isRetryNeeded());
    TimeBase.step(300);
    msgIn = (PeerMessage)rcvdMsgs2.get(TIMEOUT_SHOULDNT);
    assertEqualsMessageFrom(msg1, pid1, msgIn);
    assertTrue(msgIn.toString(), msgIn instanceof MemoryPeerMessage);
    msgIn = (PeerMessage)rcvdMsgs2.get(TIMEOUT_SHOULDNT);
    assertEqualsMessageFrom(msg2, pid1, msgIn);
    assertTrue(msgIn.toString(), msgIn instanceof MemoryPeerMessage);
    assertEquals(1, msg1.getRetryCount());
    // msg2 went directly into queue, which doesn't count as a retry
    assertEquals(0, msg2.getRetryCount());
    assertEquals(sendRpt + 1, pdata.getOrigCnt());
    assertEquals(sendRpt, pdata.getFailCnt());
    assertEquals(0, pdata.getAcceptCnt());
  }

  // Already awaiting retry, 2nd message has earlier expiration so triggers
  // immediate retry.
  public void testRetry3() throws IOException {
    TimeBase.setSimulated(1000);
    cprops.put(BlockingStreamComm.PARAM_MIN_FILE_MESSAGE_SIZE, "5000");
    cprops.put(BlockingStreamComm.PARAM_MAX_PEER_RETRY_INTERVAL, "5000");
    cprops.put(BlockingStreamComm.PARAM_MIN_PEER_RETRY_INTERVAL, "10");
    cprops.put(BlockingStreamComm.PARAM_RETRY_BEFORE_EXPIRATION, "1000");
    cprops.put(BlockingStreamComm.PARAM_RETRY_DELAY, "100");
    ConfigurationUtil.setCurrentConfigFromProps(cprops);
    msg2 = makePeerMessage(1, "1234567890123456789012345678901234567890", 20);
    msg1.setExpiration(4000);
    msg2.setExpiration(3000);

    List event;
    PeerMessage msgIn;
    setupComm1();
    setupPid(2);
    comm1.setAssocQueue(assocQ);
    comm1.sendTo(msg1, pid2, null);
    assertNotNull("Connecting channel didn't dissociate",
		  (event = (List)assocQ.get(TIMEOUT_SHOULDNT)));
    assertEquals("Connecting channel didn't dissociate",
		 "dissoc", event.get(0));

    PeerData pdata = comm1.getPeerData(pid2);
    assertNotNull(pdata);
    assertNull(pdata.getPrimaryChannel());
    assertNull(pdata.getSecondaryChannel());
    assertTrue(pdata.isRetryNeeded());
    setupComm2();
    TimeBase.step(1000);
    assertNull("Retried too early", (List)assocQ.get(TIMEOUT_SHOULD));
    assertTrue(pdata.isRetryNeeded());
    comm1.sendTo(msg2, pid2, null);
    msgIn = (PeerMessage)rcvdMsgs2.get(TIMEOUT_SHOULDNT);
    assertFalse(pdata.isRetryNeeded());
    assertEqualsMessageFrom(msg1, pid1, msgIn);
    assertTrue(msgIn.toString(), msgIn instanceof MemoryPeerMessage);
    msgIn = (PeerMessage)rcvdMsgs2.get(TIMEOUT_SHOULDNT);
    assertEqualsMessageFrom(msg2, pid1, msgIn);
    assertTrue(msgIn.toString(), msgIn instanceof MemoryPeerMessage);
  }

  // Multiple peers with evolving nextRetry, ensure TreeSet stays an ordered set
  public void testRetryOrder() throws IOException {
    TimeBase.setSimulated(100);
    cprops.put(BlockingStreamComm.PARAM_MIN_FILE_MESSAGE_SIZE, "5000");
    cprops.put(BlockingStreamComm.PARAM_MAX_PEER_RETRY_INTERVAL, "5000");
    cprops.put(BlockingStreamComm.PARAM_MIN_PEER_RETRY_INTERVAL, "10");
    cprops.put(BlockingStreamComm.PARAM_RETRY_BEFORE_EXPIRATION, "1000");
    cprops.put(BlockingStreamComm.PARAM_RETRY_DELAY, "100");
    ConfigurationUtil.setCurrentConfigFromProps(cprops);
    setupPid(2);
    PeerIdentity pid3 = setupPid(3);
    msg2 = makePeerMessage(1, "22222", 20);
    msg3 = makePeerMessage(1, "33333", 20);
    msg1.setExpiration(1000);
    msg2.setExpiration(2000);
    msg3.setExpiration(3000);

    List event;
    setupComm1();
    comm1.setAssocQueue(assocQ);
    comm1.sendTo(msg3, pid3, null);
    assertNotNull("1st channel didn't dissociate",
		  (event = (List)assocQ.get(TIMEOUT_SHOULDNT)));
    assertEquals("1st channel didn't dissociate",
		 "dissoc", event.get(0));
    PeerData pd1 = comm1.getPeerData(pid3);
    assertIsomorphic(SetUtil.set(pd1), comm1.peersToRetry);

    comm1.sendTo(msg2, pid2, null);
    assertNotNull("2nd channel didn't dissociate",
		  (event = (List)assocQ.get(TIMEOUT_SHOULDNT)));
    assertEquals("2nd channel didn't dissociate",
		 "dissoc", event.get(0));
    PeerData pd2 = comm1.getPeerData(pid2);
    assertIsomorphic(SetUtil.set(pd2, pd1), comm1.peersToRetry);

    comm1.sendTo(msg1, pid3, null);
    assertIsomorphic(SetUtil.set(pd1, pd2), comm1.peersToRetry);
  }

  // If awaiting retry, incoming connection should cause queued msgs to be
  // sent with no retry.
  public void testRetryIncoming() throws IOException {
    TimeBase.setSimulated(1000);
    cprops.put(BlockingStreamComm.PARAM_MIN_FILE_MESSAGE_SIZE, "5000");
    cprops.put(BlockingStreamComm.PARAM_MAX_PEER_RETRY_INTERVAL, "5000");
    cprops.put(BlockingStreamComm.PARAM_RETRY_BEFORE_EXPIRATION, "0");

    ConfigurationUtil.setCurrentConfigFromProps(cprops);
    msg2 = makePeerMessage(1, "1234567890123456789012345678901234567890", 20);
    msg1.setExpiration(4000);
    msg2.setExpiration(3000);

    List event;
    PeerMessage msgIn;
    setupComm1();
    setupPid(2);
    comm1.setAssocQueue(assocQ);
    comm1.sendTo(msg1, pid2, null);
    PeerData pdata1 = comm1.getPeerData(pid2);
    int sendRpt = pdata1.lastSendRpt;
    assertNotNull("Connecting channel didn't dissociate",
		  (event = (List)assocQ.get(TIMEOUT_SHOULDNT)));
    assertEquals("Connecting channel didn't dissociate",
		 "dissoc", event.get(0));

    assertNotNull(pdata1);
    assertNull(pdata1.getPrimaryChannel());
    assertNull(pdata1.getSecondaryChannel());
    assertTrue(pdata1.isRetryNeeded());
    assertEquals(sendRpt, pdata1.getOrigCnt());
    assertEquals(sendRpt, pdata1.getFailCnt());
    assertEquals(0, pdata1.getAcceptCnt());
    setupComm2();
    TimeBase.step(1000);
    assertNull("Retried too early", (List)assocQ.get(TIMEOUT_SHOULD));
    assertTrue(pdata1.isRetryNeeded());
    comm2.sendTo(msg2, pid1, null);
    PeerData pdata2 = comm2.getPeerData(pid1);
    msgIn = (PeerMessage)rcvdMsgs2.get(TIMEOUT_SHOULDNT);
    assertFalse(pdata1.isRetryNeeded());
    assertEqualsMessageFrom(msg1, pid1, msgIn);
    assertTrue(msgIn.toString(), msgIn instanceof MemoryPeerMessage);
    msgIn = (PeerMessage)rcvdMsgs1.get(TIMEOUT_SHOULDNT);
    assertEqualsMessageFrom(msg2, pid2, msgIn);
    assertTrue(msgIn.toString(), msgIn instanceof MemoryPeerMessage);
    assertEquals(sendRpt, pdata1.getOrigCnt());
    assertEquals(sendRpt, pdata1.getFailCnt());
    assertEquals(1, pdata1.getAcceptCnt());
    assertEquals(1, pdata2.getOrigCnt());
    assertEquals(0, pdata2.getFailCnt());
    assertEquals(0, pdata2.getAcceptCnt());
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
    assertEquals(1, getChannels(comm1).size());
    assertEquals(1, getChannels(comm2).size());
    assertEquals(0, getRcvChannels(comm1).size());
    assertEquals(0, getRcvChannels(comm2).size());

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
    assertEquals(1, getChannels(comm1).size());
    assertEmpty(getRcvChannels(comm1));
    assertEquals(1, getChannels(comm2).size());
    assertEmpty(getRcvChannels(comm2));

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
    setupComm1();
    setupComm2();
    // delay comm2's accept()
    comm2.setAcceptSem(sem2);
    comm1.sendTo(msg1, pid2, null);
    comm2.sendTo(msg2, pid1, null);
    // both should have one connecting channel
    assertEquals(1, getChannels(comm1).size());
    assertEquals(1, getChannels(comm2).size());
    // comm1 should receive message
    msgIn = (PeerMessage)rcvdMsgs1.get(TIMEOUT_SHOULDNT);
    assertEqualsMessageFrom(msg2, pid2, msgIn);
    assertEquals(1, getRcvChannels(comm1).size());
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
    setupComm1();
    setupComm2();
    // delay accept() in both channels
    comm1.setAcceptSem(sem1);
    comm2.setAcceptSem(sem2);
    comm1.sendTo(msg1, pid2, null);
    comm2.sendTo(msg2, pid1, null);
    // both should have one connecting channel
    assertEquals(1, getChannels(comm1).size());
    assertEquals(1, getChannels(comm2).size());
    assertEquals(0, getRcvChannels(comm1).size());
    assertEquals(0, getRcvChannels(comm2).size());
    assertTrue(rcvdMsgs1.isEmpty());
    assertTrue(rcvdMsgs2.isEmpty());
    // allow comm1 accept to proceed, it should receive message
    sem1.give();
    msgIn = (PeerMessage)rcvdMsgs1.get(TIMEOUT_SHOULDNT);
    // comm1 only should have secondary channel
    assertEqualsMessageFrom(msg2, pid2, msgIn);
    assertEquals(1, getChannels(comm1).size());
    assertEquals(1, getChannels(comm2).size());
    assertEquals(1, getRcvChannels(comm1).size());
    assertEquals(0, getRcvChannels(comm2).size());
    // allow comm2 to proceed
    sem2.give();
    msgIn = (PeerMessage)rcvdMsgs2.get(TIMEOUT_SHOULDNT);
    assertEqualsMessageFrom(msg1, pid1, msgIn);
    assertEquals(1, getChannels(comm1).size());
    assertEquals(1, getChannels(comm2).size());
    assertEquals(1, getRcvChannels(comm1).size());
    assertEquals(1, getRcvChannels(comm2).size());
  }

  // allow channel to timeout and close after use
  public void testChannelCloseAfterTimeout() throws IOException {
    TimeBase.setSimulated(1000);
    PeerMessage msgIn;
    cprops.setProperty(BlockingStreamComm.PARAM_CHANNEL_IDLE_TIME, "5000");
    ConfigurationUtil.setCurrentConfigFromProps(cprops);
    setupComm1();
    setupComm2();
    // prevent comm2 from timing out and closing, thereby closing comm1's
    // input socket, invalidating the test
    comm2.setChannelIdleTime(10000);

    comm1.setAssocQueue(assocQ);
    comm1.sendTo(msg1, pid2, null);
    msgIn = (PeerMessage)rcvdMsgs2.get(TIMEOUT_SHOULDNT);
    comm2.sendTo(msg2, pid1, null);
    msgIn = (PeerMessage)rcvdMsgs1.get(TIMEOUT_SHOULDNT);

    // Must ensure we don't step time until after channel has calculated
    // the send wait time.  Set up a post-calcSendWaitTime semaphore, send
    // another message, wait on sem until calcSendWaitTime has been called
    // 3 times (before each of 2 messages pulled from queue, plus final
    // wait for (nonexistent) 3rd message).
    MyBlockingPeerChannel chan =
      (MyBlockingPeerChannel)getChannel(comm1, pid2);
    assertNotNull(chan);

    chan.setCalcSendWaitSem(sem1);
    chan.setStopSem(sem2);
    comm1.sendTo(msg1, pid2, null);
    msgIn = (PeerMessage)rcvdMsgs2.get(TIMEOUT_SHOULDNT);
    assertTrue(sem1.take(TIMEOUT_SHOULDNT));
    if (chan.calcSendWaitCtr < 3) {
      assertTrue(sem1.take(TIMEOUT_SHOULDNT));
    }
    assertEquals(3, chan.calcSendWaitCtr);

    TimeBase.step(4000);
    assertEquals(1, getChannels(comm1).size());

    TimeBase.step(2000);
    List event;
    assertNotNull("Channel wasn't dissociated after timeout",
		  (event = (List)assocQ.get(TIMEOUT_SHOULDNT)));
    assertEquals("Channel wasn't dissociated after timeout",
		 "dissoc", event.get(0));
    assertEquals(0, getChannels(comm1).size());
    assertEquals(0, getRcvChannels(comm1).size());
    assertTrue("Channel didn't stop", sem2.take(TIMEOUT_SHOULDNT));
  }

  // allow channel to timeout and close after use
  public void testChannelCloseAfterTimeoutDrainTimeout() throws IOException {
    TimeBase.setSimulated(1000);
    PeerMessage msgIn;
    cprops.setProperty(BlockingStreamComm.PARAM_CHANNEL_IDLE_TIME, "5000");
    cprops.setProperty(BlockingStreamComm.PARAM_DRAIN_INPUT_TIME, "3000");
    ConfigurationUtil.setCurrentConfigFromProps(cprops);
    setupComm1();
    setupComm2();
    // prevent comm2 from timing out and closing, thereby closing comm1's
    // input socket, invalidating the test
    comm2.setChannelIdleTime(20000);

    comm1.setAssocQueue(assocQ);
    comm1.sendTo(msg1, pid2, null);
    msgIn = (PeerMessage)rcvdMsgs2.get(TIMEOUT_SHOULDNT);
    comm2.sendTo(msg2, pid1, null);
    msgIn = (PeerMessage)rcvdMsgs1.get(TIMEOUT_SHOULDNT);

    // Must ensure we don't step time until after channel has calculated
    // the send wait time.  Set up a post-calcSendWaitTime semaphore, send
    // another message, wait on sem until calcSendWaitTime has been called
    // 3 times (before each of 2 messages pulled from queue, plus final
    // wait for (nonexistent) 3rd message).
    MyBlockingPeerChannel chan1 =
      (MyBlockingPeerChannel)getChannel(comm1, pid2);
    MyBlockingPeerChannel chan2 =
      (MyBlockingPeerChannel)getChannel(comm2, pid1);
    assertNotNull(chan1);
    assertNotNull(chan2);

    chan1.setCalcSendWaitSem(sem1);
    chan1.setStopSem(sem2);
    chan2.setSimulateSendBusy(true);
    comm1.sendTo(msg1, pid2, null);
    msgIn = (PeerMessage)rcvdMsgs2.get(TIMEOUT_SHOULDNT);
    assertTrue(sem1.take(TIMEOUT_SHOULDNT));
    if (chan1.calcSendWaitCtr < 3) {
      assertTrue(sem1.take(TIMEOUT_SHOULDNT));
    }
    assertEquals(3, chan1.calcSendWaitCtr);

    TimeBase.step(4000);
    assertEquals(1, getChannels(comm1).size());

    TimeBase.step(2000);
    List event;
    assertNotNull("Channel wasn't dissociated after timeout",
		  (event = (List)assocQ.get(TIMEOUT_SHOULDNT)));
    assertEquals("Channel wasn't dissociated after timeout",
		 "dissoc", event.get(0));
    assertEquals(0, getChannels(comm1).size());
    assertEquals(0, getRcvChannels(comm1).size());
    if (!isSsl()) {
      // Since shutdownOutput() doesn't work with SSL, channel doesn't end
      // up draining
      assertContains(comm1.drainingChannels, chan1);
    }
    if (shutdownOutputSupported) {
      assertFalse("Channel stopped before drain input timer",
		  sem2.take(TIMEOUT_SHOULD));
    }
    TimeBase.step(4000);
    assertTrue("Drain input timer didn't stop channel",
	       sem2.take(TIMEOUT_SHOULDNT));
    assertEmpty(comm1.drainingChannels);
  }

  // read (so) timeout should abort channel
  public void testReadTimeout() throws IOException {
    TimeBase.setSimulated(1000);
    PeerMessage msgIn;
    cprops.setProperty(BlockingStreamComm.PARAM_CHANNEL_IDLE_TIME, "10h");
    cprops.setProperty(BlockingStreamComm.PARAM_DATA_TIMEOUT, "100");
    ConfigurationUtil.setCurrentConfigFromProps(cprops);
    setupComm1();
    setupComm2();
    comm1.setAssocQueue(assocQ);
    comm1.sendTo(msg1, pid2, null);
    msgIn = (PeerMessage)rcvdMsgs2.get(TIMEOUT_SHOULDNT);
    // Timeout might happen before channel reads 1st message (or even
    // before it reads the peerid).  If this happens no message will be
    // read and the queue get() will timeout and return null
    if (msgIn != null) {
      assertEqualsMessageFrom(msg1, pid1, msgIn);
    }
    List event;
    assertNotNull("Channel didn't close automatically after timeout",
		  (event = (List)assocQ.get(TIMEOUT_SHOULDNT)));
    assertEquals("Channel didn't close automatically after timeout",
		 "dissoc", event.get(0));
    assertEquals(0, getChannels(comm1).size());
    assertEquals(0, getRcvChannels(comm1).size());
  }

  // create MAX_COMMS comm instances, have each of them sent messages to
  // each
  public void testMultipleChannels() throws IOException {
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
    for (int comm = 0; comm < MAX_COMMS; comm++) {
      Set peers = allPeers();
      while (!peers.isEmpty()) {
	PeerMessage msgIn = (PeerMessage)rcvdMsgss[comm].get(TIMEOUT_SHOULDNT*4);
	assertNotNull("Comm" + comm + " didn't receive messages from " + peers,
		      msgIn);
	peers.remove(msgIn.getSender());
      }
    }
  }

  Set allPeers() {
    return SetUtil.fromArray(pids);
  }

  static int createCounter = 1;
  class MyBlockingStreamComm extends BlockingStreamComm {

    SocketFactory sockFact;
    PeerIdentity localId;
    SimpleQueue assocEvents;
    SimpleBinarySemaphore acceptSem;
    BlockingStreamComm.SocketFactory superSockFact;
    int uniqueId;

    MyBlockingStreamComm(PeerIdentity localId) {
      this.localId = localId;
      sockFact = null;
      uniqueId = createCounter++;
    }

    protected String getStatusAccessorName(String base) {
      if (false) super.getStatusAccessorName(base);
      return base + uniqueId;
    }

    SocketFactory getSocketFactory() {
      if (sockFact == null) {
	superSockFact = super.getSocketFactory();
	sockFact = new MySocketFactory(superSockFact);
      }
      return sockFact;
    }

    protected PeerIdentity getLocalPeerIdentity() {
      return localId;
    }

    @Override
    void associateChannelWithPeer(BlockingPeerChannel chan,
				  PeerIdentity peer) {
      super.associateChannelWithPeer(chan, peer);
      if (assocEvents != null) {
	assocEvents.put(ListUtil.list("assoc", this));
      }
    }

    @Override
    void dissociateChannelFromPeer(BlockingPeerChannel chan,
				   PeerIdentity peer,
				   Queue sendQueue) {
      super.dissociateChannelFromPeer(chan, peer, sendQueue);
      if (assocEvents != null) {
	assocEvents.put(ListUtil.list("dissoc", this));
      }
    }

    void processIncomingConnection(Socket sock) throws IOException {
      if (acceptSem != null) {
	acceptSem.take();
      }
      super.processIncomingConnection(sock);
    }

    void setAssocQueue(SimpleQueue sem1) {
      assocEvents = sem1;
    }
    void setAcceptSem(SimpleBinarySemaphore sem1) {
      acceptSem = sem1;
    }

    void setChannelIdleTime(long time) {
      paramChannelIdleTime = time;
    }

    PeerData makePeerData(PeerIdentity pid) {
      return new PeerData(pid);
    }

    void addPeerToRetry(PeerData pdata) {
      peersToRetry.add(pdata);
    }
  }

  /** Socket factory creates either real or internal sockets, and
   * MyBlockingPeerChannels.
   */
  class MySocketFactory implements BlockingStreamComm.SocketFactory {
    protected BlockingStreamComm.SocketFactory sf;
    IOException newSocketThrow = null;

    MySocketFactory(BlockingStreamComm.SocketFactory s) {
      sf = s;
    }

    public ServerSocket newServerSocket(int port, int backlog)
	throws IOException {
      if (useInternalSockets) {
	return new InternalServerSocket(port, backlog);
      } else {
	ServerSocket ss = sf.newServerSocket(port, backlog);
	if (isSsl()) {
	  assertTrue(ss instanceof SSLServerSocket);
	} else {
	  assertFalse(ss instanceof SSLServerSocket);
	}
        return ss;
      }
    }

    public Socket newSocket(IPAddr addr, int port) throws IOException {
      if (newSocketThrow != null) {
	throw newSocketThrow;
      }
      if (useInternalSockets) {
	return new InternalSocket(addr.getInetAddr(), port);
      } else {
	Socket s = sf.newSocket(addr, port);
	if (isSsl()) {
	  assertTrue(s instanceof SSLSocket);
	} else {
	  assertFalse(s instanceof SSLSocket);
	}
        return s;
      }
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

    void setNewSocketThrow(IOException e) {
      newSocketThrow = e;
    }

  }

  static class MyBlockingPeerChannel extends BlockingPeerChannel {
    volatile SimpleBinarySemaphore stopSem;
    volatile SimpleBinarySemaphore calcSendWaitSem;
    volatile int calcSendWaitCtr = 0;
    boolean simulateSendBusy = false;

    MyBlockingPeerChannel(BlockingStreamComm scomm, PeerIdentity peer) {
      super(scomm, peer);
    }

    MyBlockingPeerChannel(BlockingStreamComm scomm, Socket sock) {
      super(scomm, sock);
    }

    void stopChannel(boolean abort, String msg, Throwable t) {
      super.stopChannel(abort, msg, t);
      if (stopSem != null) {
	stopSem.give();
      }
    }
    void setStopSem(SimpleBinarySemaphore sem1) {
      stopSem = sem1;
    }

    Deadline calcSendWaitDeadline() {
      Deadline res = super.calcSendWaitDeadline();
      calcSendWaitCtr++;
      if (calcSendWaitSem != null) {
	calcSendWaitSem.give();
      }
      return res;
    }

    void setCalcSendWaitSem(SimpleBinarySemaphore sem1) {
      calcSendWaitSem = sem1;
    }

    void setSimulateSendBusy(boolean val) {
      simulateSendBusy = val;
    }

    boolean isSendIdle() {
      if (simulateSendBusy) {
	return false;
      }
      return super.isSendIdle();
    }

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

  static class MyIdentityManager extends IdentityManagerImpl {
    public void storeIdentities() throws ProtocolException {
    }
  }

  public void readIdentityAgreementFrom(ArchivalUnit au, InputStream in)
      throws IOException {
    throw new UnsupportedOperationException("not implemented");
  }

//   public void writeIdentityDbTo(OutputStream out) throws IOException {
//     throw new UnsupportedOperationException("not implemented");
//   }

  // Suppress delete() because it prevents comparison with messages that
  // have been sent
  static class MyMemoryPeerMessage extends MemoryPeerMessage {
    boolean isDeleted = false;
    public void delete() {
      isDeleted = true;
    }
  }

  // Variants:

  /** Buffered socket OutputStream */
  public static class Buff extends TestBlockingStreamComm {
    public Buff(String name) {
      super(name);
    }
    public void addSuiteProps(Properties p) {
      p.setProperty(BlockingStreamComm.PARAM_IS_BUFFERED_SEND, "true");
      p.setProperty(BlockingStreamComm.PARAM_TCP_NODELAY, "false");
    }
  }

  /** Unbuffered socket OutputStream */
  public static class UnBuff extends TestBlockingStreamComm {
    public UnBuff(String name) {
      super(name);
    }
    public void addSuiteProps(Properties p) {
      p.setProperty(BlockingStreamComm.PARAM_IS_BUFFERED_SEND, "false");
      p.setProperty(BlockingStreamComm.PARAM_TCP_NODELAY, "false");
    }
  }

  /** Buffered OutputStream, TCP_NODELAY */
  public static class BuffNoDelay extends TestBlockingStreamComm {
    public BuffNoDelay(String name) {
      super(name);
    }
    public void addSuiteProps(Properties p) {
      p.setProperty(BlockingStreamComm.PARAM_IS_BUFFERED_SEND, "true");
      p.setProperty(BlockingStreamComm.PARAM_TCP_NODELAY, "true");
    }
  }

  /** Unbuffered OutputStream, TCP_NODELAY */
  public static class UnBuffNoDelay extends TestBlockingStreamComm {
    public UnBuffNoDelay(String name) {
      super(name);
    }
    public void addSuiteProps(Properties p) {
      p.setProperty(BlockingStreamComm.PARAM_IS_BUFFERED_SEND, "false");
      p.setProperty(BlockingStreamComm.PARAM_TCP_NODELAY, "true");
    }
  }

  /** Channel threads run at high priority */
  public static class HighPri extends TestBlockingStreamComm {
    public HighPri(String name) {
      super(name);
    }
    public void addSuiteProps(Properties p) {
      String prop = "org.lockss.thread." +
	BlockingStreamComm.PRIORITY_PARAM_CHANNEL + ".priority";
      p.setProperty(prop, "5");
    }
  }


  public static Test suite() {
    return variantSuites(new Class[] {
      Buff.class,
//       UnBuff.class,
//       BuffNoDelay.class,
      UnBuffNoDelay.class,
      HighPri.class,
    });
  }
}

