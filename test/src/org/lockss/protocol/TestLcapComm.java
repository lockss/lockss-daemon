/*
 * $Id: TestLcapComm.java,v 1.10 2003-06-01 21:21:30 tal Exp $
 */

/*

Copyright (c) 2002 Board of Trustees of Leland Stanford Jr. University,
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
import junit.framework.TestCase;
import org.lockss.daemon.*;
import org.lockss.plugin.*;
import org.lockss.util.*;
import org.lockss.test.*;


/**
 * This is the test class for org.lockss.protocol.LcapComm
 */

public class TestLcapComm extends LockssTestCase {
  public static Class testedClasses[] = {
    org.lockss.protocol.LcapComm.class
  };

  public TestLcapComm(String msg) {
    super(msg);
  }

  static Logger log = Logger.getLogger("SockTest");
  static int TIMEOUT = 1000;

  static final String PARAM_MULTI_GROUP = LcapComm.PARAM_MULTI_GROUP;
  static final String PARAM_MULTI_PORT = LcapComm.PARAM_MULTI_PORT;
  static final String PARAM_UNI_PORT = LcapComm.PARAM_UNI_PORT;
  static final String PARAM_UNI_PORT_SEND = LcapComm.PARAM_UNI_PORT_SEND;
  static final String PARAM_MULTI_VERIFY = LcapComm.PARAM_MULTI_VERIFY;

  private static MockLockssDaemon daemon = new MockLockssDaemon(null);

  String testStr = "This is test data";
  byte[] testData;
  byte[] testPktData;
  int testPort = 1234;
  InetAddress testAddr;
  String[] ar1 = {"foo"};
  byte[] chal = {1,2,3,4,5,6,7,8,9,10};
  LockssDatagram testSend;
  DatagramPacket testPacket;
  DatagramPacket testPacket2;

  MockSocketFactory fact;
  LcapComm comm;
  Configuration config;

  MockDatagramSocket ssock;
  MockDatagramSocket usock;
  MockMulticastSocket msock1;
  MockMulticastSocket msock2;

  SimpleQueue rcvdMsgs;


  public void setUp() throws Exception {
    super.setUp();
    testData = testStr.getBytes();
    testAddr = InetAddress.getByName("127.0.0.1");
    testSend = new LockssDatagram(LockssDatagram.PROTOCOL_TEST, testData);
    byte[] testHeader = {0, 0, 0, LockssDatagram.PROTOCOL_TEST};
    testPktData = ByteArray.concat(testHeader, testData);
    testPacket = new DatagramPacket(testPktData, testPktData.length,
				    testAddr, testPort);
    testPacket2 = new DatagramPacket(testPktData, testPktData.length,
				     testAddr, testPort - 1);

    fact = new MockSocketFactory();
    Properties props = new Properties();
    props.put(PARAM_MULTI_GROUP, "239.3.4.5");
    props.put(PARAM_MULTI_PORT, "5432");
    props.put(PARAM_UNI_PORT, "2345");
//     PARAM_UNI_PORT_SEND + "=" + "\n" +
    props.put(IdentityManager.PARAM_LOCAL_IP, "127.0.0.1");
    props.put(PARAM_MULTI_VERIFY, "yes");
    ConfigurationUtil.setCurrentConfigFromProps(props);
    config = Configuration.getCurrentConfig();
//     config = Configuration.readConfig(ListUtil.list(FileUtil.urlOfString(c1)));
    comm = new LcapComm(fact, config);
    daemon.setCommManager(comm);
    comm.initService(daemon);
    daemon.getIdentityManager();
    comm.startService();
    ssock = ((MockDatagramSocket)fact.ssocks.get(0));
    usock = ((MockDatagramSocket)fact.usocks.get(0));
    msock1 = ((MockMulticastSocket)fact.msocks.get(0));
    msock2 = ((MockMulticastSocket)fact.msocks.get(1));
    rcvdMsgs = new SimpleQueue.Fifo();
    comm.registerMessageHandler(LockssDatagram.PROTOCOL_TEST,
				new MessageHandler());
  }

  public void tearDown() throws Exception {
    if (comm != null) {
      comm.stopService();
      comm = null;
      fact = null;
    }
    super.tearDown();
  }

  public void testUnicastSend() throws Exception {
    assertTrue(ssock.getSentPackets().isEmpty());
    comm.sendTo(testSend, testAddr, testPort);
    DatagramPacket sent = (DatagramPacket)ssock.getSentPackets().elementAt(0);
    assertEquals(testAddr, sent.getAddress());
    assertEquals(testPort, sent.getPort());
    assertEquals(testPktData, sent.getData());
  }

  public void testMulticastSend() throws Exception {
    assertTrue(ssock.getSentPackets().isEmpty());
    comm.send(testSend, (ArchivalUnit)null);
    DatagramPacket sent = (DatagramPacket)ssock.getSentPackets().elementAt(0);
    assertEquals(InetAddress.getByName(config.get(PARAM_MULTI_GROUP)),
		 sent.getAddress());
    assertEquals(config.getInt(PARAM_MULTI_PORT), sent.getPort());
    assertEquals(testPktData, sent.getData());
  }

  public void testUnicastReceive() throws Exception {
    assertTrue(rcvdMsgs.isEmpty());
    System.err.println(ByteArray.toHexString(testPacket.getData()));
    usock.addToReceiveQueue(testPacket);
    LockssReceivedDatagram rd = (LockssReceivedDatagram)rcvdMsgs.get(TIMEOUT);
    assertFalse(rd.isMulticast());
    assertTrue(rcvdMsgs.isEmpty());
  }

  public void testMulticastReceive() throws Exception {
    LockssReceivedDatagram rd;
    assertTrue(rcvdMsgs.isEmpty());
    msock1.addToReceiveQueue(testPacket);
    // it should not show up until packet is received on other socket
    msock1.addToReceiveQueue(testPacket);
    TimerUtil.guaranteedSleep(10);
    assertTrue(rcvdMsgs.isEmpty());
    msock2.addToReceiveQueue(testPacket);
    rd = (LockssReceivedDatagram)rcvdMsgs.get(TIMEOUT);
    assertTrue(rd.isMulticast());
    // this one should not show up
    msock2.addToReceiveQueue(testPacket);
    TimerUtil.guaranteedSleep(10);
    // unicast a different packet
    usock.addToReceiveQueue(testPacket2);
    // and make sure we get that one.  ensures multi isn't just slow to
    // arrive
    rd = (LockssReceivedDatagram)rcvdMsgs.get(TIMEOUT);
    assertFalse(rd.isMulticast());
    assertEquals(testPort - 1, rd.getPacket().getPort());
    assertTrue(rcvdMsgs.isEmpty());
  }

  class MessageHandler implements LcapComm.MessageHandler {
    public void handleMessage(LockssReceivedDatagram rd) {
      rcvdMsgs.put(rd);
    }
  }

  /** Mock socket factory creates LcapSockets with mock datagram/multicast
   * sockets. */
  static class MockSocketFactory implements LcapComm.SocketFactory {
    List msocks = new ArrayList();
    List usocks = new ArrayList();
    List ssocks = new ArrayList();

    public LcapSocket.Multicast newMulticastSocket(Queue rcvQ,
					    InetAddress group,
					    int port)
	throws IOException {
      MockMulticastSocket mskt = new MockMulticastSocket();
      msocks.add(mskt);
      return new LcapSocket.Multicast(rcvQ, mskt, (InetAddress)null);
    }

    public LcapSocket.Unicast newUnicastSocket(Queue rcvQ, int port)
	throws IOException {
      MockDatagramSocket dskt = new MockDatagramSocket();
      usocks.add(dskt);
      return new LcapSocket.Unicast(rcvQ, dskt);
    }

    public LcapSocket newSendSocket() throws SocketException {
      MockDatagramSocket dskt = new MockDatagramSocket();
      ssocks.add(dskt);
      return new LcapSocket(dskt);
    }
  }
}
