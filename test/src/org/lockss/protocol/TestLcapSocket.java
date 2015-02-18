/*
 * $Id$
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
import junit.framework.TestCase;
import org.lockss.daemon.*;
import org.lockss.util.*;
import org.lockss.util.Queue;
import org.lockss.test.*;


/**
 * This is the test class for org.lockss.protocol.LcapSocket
 */

public class TestLcapSocket extends LockssTestCase {
  public static Class testedClasses[] = {
    org.lockss.protocol.LcapSocket.class
  };


  public TestLcapSocket(String msg) {
    super(msg);
  }

  String testStr = "This is test data";
  byte[] testData = testStr.getBytes();
  int testPort = 1234;
  IPAddr testAddr;
  DatagramPacket testPacket;

  public void setUp() throws Exception {
    super.setUp();
    testAddr = IPAddr.getByName("127.0.0.1");
    testPacket =
      new DatagramPacket(testData, testData.length,
			 testAddr.getInetAddr(), testPort);
  }

  /** Sender sends a packet to a mock datagram socket in a while */
  class Sender extends DoLater {
    MockDatagramSocketExtras sock;
    DatagramPacket pkt;

    Sender(long waitMs, MockDatagramSocketExtras sock, DatagramPacket pkt) {
      super(waitMs);
      this.sock = sock;
      this.pkt = pkt;
    }

    protected void doit() {
      sock.addToReceiveQueue(pkt);
    }
  }

  /** Put something onto a queue in a while */
  private Sender sendIn(long ms, MockDatagramSocketExtras sock,
			DatagramPacket pkt) {
    Sender p = new Sender(ms, sock, pkt);
    p.start();
    return p;
  }

  public void testSend() throws Exception {
    MockDatagramSocket dskt = new MockDatagramSocket();
    LcapSocket skt = new LcapSocket(dskt);
    assertTrue(dskt.getSentPackets().isEmpty());
    skt.send(testPacket);
    DatagramPacket sent = (DatagramPacket)dskt.getSentPackets().elementAt(0);
    assertEquals(testPacket, sent);
  }

  static Logger log = Logger.getLogger("SockTest");

  public void testUnicastReceive() throws Exception {
    Queue rcvQ = new FifoQueue();
    MockDatagramSocket dskt = new MockDatagramSocket();
    LcapSocket.Unicast lskt = new LcapSocket.Unicast(rcvQ, dskt);
    dskt.addToReceiveQueue(testPacket);
    Interrupter intr;
    intr = interruptMeIn(TIMEOUT_SHOULDNT, true);
    PrivilegedAccessor.invokeMethod(lskt, "receivePacket");
    assertFalse(rcvQ.isEmpty());
    LockssReceivedDatagram rcvd =
      (LockssReceivedDatagram)rcvQ.get(Deadline.in(0));
    assertEquals(testPacket, rcvd.getPacket());
    intr.cancel();
  }

  public void testMulticastReceive() throws Exception {
    Queue rcvQ = new FifoQueue();
    MockMulticastSocket mskt = new MockMulticastSocket();
    LcapSocket.Multicast lskt =
      new LcapSocket.Multicast(rcvQ, mskt, IPAddr.getByName("127.0.0.1"));
    mskt.addToReceiveQueue(testPacket);
    Interrupter intr = interruptMeIn(500);
    PrivilegedAccessor.invokeMethod(lskt, "receivePacket");
    assertFalse(rcvQ.isEmpty());
    LockssReceivedDatagram rcvd =
      (LockssReceivedDatagram)rcvQ.get(Deadline.in(0));
    assertEquals(testPacket, rcvd.getPacket());
    intr.cancel();
  }
}
