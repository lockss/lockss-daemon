/*
 * $Id: TestLcapSocket.java,v 1.1 2002-11-05 21:10:02 tal Exp $
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
import org.lockss.util.*;
import org.lockss.test.*;


/**
 * This is the test class for org.lockss.protocol.TestLcapSocket
 */

public class TestLcapSocket extends LockssTestCase{
  public static Class testedClasses[] = {
    org.lockss.protocol.LcapSocket.class
  };


  public TestLcapSocket(String msg){
    super(msg);
  }

  String testStr = "This is test data";
  byte[] testData = testStr.getBytes();
  int testPort = 1234;
  InetAddress testAddr;
  DatagramPacket testPacket;


  public void setUp() throws Exception {
    testAddr = InetAddress.getByName("127.0.0.1");
    testPacket =
      new DatagramPacket(testData, testData.length, testAddr, testPort);
  }

  public void testSend() throws Exception {
    MockDatagramSocket dskt = new MockDatagramSocket();
    LcapSocket skt = new LcapSocket(dskt);
    assertTrue(dskt.getSentPackets().isEmpty());
    skt.send(testPacket);
    DatagramPacket sent = (DatagramPacket)dskt.getSentPackets().elementAt(0);
    assertEqualPkts(testPacket, sent);
  }
  
  /*
  public void testMulticastReceive() throws Exception {
    MockDatagramSocket dskt = new MockDatagramSocket();
    LcapSocket skt = new LcapSocket(dskt);
    LcapSocket.Multicast mSock =
      new LcapSocket.Multicast(socketInQ, group, multiPort);
    skt.send(testPacket);
    DatagramPacket sent = (DatagramPacket)dskt.getSentPackets().elementAt(0);
    assertEqualPkts(testPacket, sent);
  }
  */  
    /*
    try {
      sendSock = new LcapSocket();
    } catch (SocketException e) {
      log.critical("Can't create send socket", e);
    }
    try {
      socketInQ = new FifoQueue();
      log.debug("new LcapSocket.Multicast("+socketInQ+", "+group+", "+multiPort);
    
      mSock.start();
      this.mSock = mSock;
    } catch (UnknownHostException e) {
      log.error("Can't create socket", e);
    } catch (IOException e) {
      log.error("Can't create socket", e);
    }
    assertEquals("Critical", Logger.nameOf(Logger.LEVEL_CRITICAL));
  }
    */
}

