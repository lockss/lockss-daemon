/*
 * $Id: TestLockssDatagram.java,v 1.4 2003-05-29 01:49:07 tal Exp $
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
 * This is the test class for org.lockss.protocol.LockssDatagram
 */

public class TestLockssDatagram extends LockssTestCase {
  public static Class testedClasses[] = {
    org.lockss.protocol.LockssDatagram.class,
    org.lockss.protocol.LockssReceivedDatagram.class
  };

  public TestLockssDatagram(String msg) {
    super(msg);
  }

  String testStr;
  byte[] testData;
  int testPort;

  public void setUp() throws Exception {
    super.setUp();
    testStr = "This is test data";
    testData = testStr.getBytes();
    testPort = 1234;
  }

  public void testFromData() throws Exception {
    LockssDatagram dg = new LockssDatagram(4, testData);
    assertEquals(4, dg.getProtocol());
    assertEquals(testData, dg.getData());
  }

  public void testMakePacket() throws Exception {
    LockssDatagram dg = new LockssDatagram(4, testData);
    InetAddress testAddr = InetAddress.getByName("127.0.0.1");
    DatagramPacket pkt = dg.makeSendPacket(testAddr, testPort);
    assertEquals(testAddr, pkt.getAddress());
    assertEquals(testPort, pkt.getPort());
    byte[] data = pkt.getData();
    assertEquals(LockssDatagram.HEADER_LENGTH + testData.length,
		 pkt.getLength());
    byte[] prot = new byte[4];
    byte[] expprot = {0, 0, 0, 4};
    System.arraycopy(data, 0, prot, 0, 4);
    assertEquals(expprot, prot);
    int len = pkt.getLength() - LockssDatagram.HEADER_LENGTH;
    byte[] udata = new byte[len];
    System.arraycopy(data, LockssDatagram.HEADER_LENGTH, udata, 0, len);
    assertEquals(testData, udata);
  }

  public void testEncodeDecode() throws Exception {
    LockssDatagram dg = new LockssDatagram(27, testData);
    InetAddress testAddr = InetAddress.getByName("10.4.111.27");
    DatagramPacket pkt = dg.makeUncmpressedSendPacket(testAddr, testPort);
    LockssReceivedDatagram rdg = new LockssReceivedDatagram(pkt);
    assertFalse(rdg.isCompressed());
    assertEquals(pkt, rdg.getPacket());
    assertEquals(testData, rdg.getData());
    assertEquals(27, rdg.getProtocol());
    LockssReceivedDatagram rdg2 = new LockssReceivedDatagram(pkt);
    assertEquals(27, rdg.getProtocol());
    assertEquals(testData, rdg.getData());
  }

  public void testCompressedEncodeDecode() throws Exception {
    LockssDatagram dg = new LockssDatagram(27, testData);
    InetAddress testAddr = InetAddress.getByName("10.4.111.27");
    DatagramPacket pkt = dg.makeCompressedSendPacket(testAddr, testPort);
    LockssReceivedDatagram rdg = new LockssReceivedDatagram(pkt);
    assertTrue(rdg.isCompressed());
    assertEquals(pkt, rdg.getPacket());
    assertEquals(testData, rdg.getData());
    assertEquals(27, rdg.getProtocol());
    LockssReceivedDatagram rdg2 = new LockssReceivedDatagram(pkt);
    assertEquals(27, rdg.getProtocol());
    assertEquals(testData, rdg.getData());
  }

  public void testEquals() throws Exception {
    InetAddress testAddr = InetAddress.getByName("127.0.0.1");
    byte[] otherData = new String(testData).getBytes();
    DatagramPacket pkt1 =
      new DatagramPacket(testData, testData.length, testAddr, testPort);
    DatagramPacket pkt2 =
      new DatagramPacket(otherData, otherData.length, testAddr, testPort);
    LockssReceivedDatagram d1 = new LockssReceivedDatagram(pkt1);
    LockssReceivedDatagram d2 = new LockssReceivedDatagram(pkt2);
    // Don't use assertEquals() here; want to make sure really testing
    // LockssReceivedDatagram's equals() method, not some overloaded
    // assertEquals()
    assertTrue(d1.equals(d2));
    // changing the receive socket shouldn't affect equals()
    d1.setReceiveSocket(new LcapSocket());
    assertTrue(d1.equals(d2));
    pkt2.setLength(pkt2.getLength() - 1);
    assertNotEquals(d1, d2);
    pkt2.setLength(pkt2.getLength() + 1);
    assertTrue(d1.equals(d2));
    pkt2.setPort(testPort + 1);
    assertNotEquals(d1, d2);
    pkt2.setPort(testPort);
    assertTrue(d1.equals(d2));
    pkt2.setAddress(InetAddress.getByName("127.0.0.2"));
    assertNotEquals(d1, d2);
    pkt2.setAddress(testAddr);
    assertTrue(d1.equals(d2));
    otherData[0] = 'X';
    pkt2.setData(otherData);
    assertNotEquals(d1, d2);
  }

  // Testing hashCode not equal is a bit dicey, as it needn't necessarily
  // be.  But knowing how LockssReceivedDatagram.hashCode() is implemented,
  // these tests should work.
  public void testHash() throws Exception {
    InetAddress testAddr = InetAddress.getByName("127.0.0.1");
    byte[] otherData = new String(testData).getBytes();
    DatagramPacket pkt1 =
      new DatagramPacket(testData, testData.length, testAddr, testPort);
    DatagramPacket pkt2 =
      new DatagramPacket(otherData, otherData.length, testAddr, testPort);
    LockssReceivedDatagram d1 = new LockssReceivedDatagram(pkt1);
    LockssReceivedDatagram d2 = new LockssReceivedDatagram(pkt2);
    assertEquals(d1.hashCode(), d2.hashCode());
    // changing the receive socket shouldn't affect hashCode()
    d1.setReceiveSocket(new LcapSocket());
    assertEquals(d1.hashCode(), d2.hashCode());
    pkt2.setLength(pkt2.getLength() - 1);
    // LockssReceivedDatagram caches hashCode, so must make a new one to
    // reflect change
    d2 = new LockssReceivedDatagram(pkt2);
    assertTrue(d1.hashCode() != d2.hashCode());
    pkt2.setLength(pkt2.getLength() + 1);
    d2 = new LockssReceivedDatagram(pkt2);
    assertEquals(d1.hashCode(), d2.hashCode());
    pkt2.setPort(testPort + 1);
    d2 = new LockssReceivedDatagram(pkt2);
    assertTrue(d1.hashCode() != d2.hashCode());
    pkt2.setPort(testPort);
    d2 = new LockssReceivedDatagram(pkt2);
    assertEquals(d1.hashCode(), d2.hashCode());
    pkt2.setAddress(InetAddress.getByName("127.0.0.2"));
    d2 = new LockssReceivedDatagram(pkt2);
    assertTrue(d1.hashCode() != d2.hashCode());
    pkt2.setAddress(testAddr);
    d2 = new LockssReceivedDatagram(pkt2);
    assertEquals(d1.hashCode(), d2.hashCode());
    otherData[0] = 'X';
    pkt2.setData(otherData);
    d2 = new LockssReceivedDatagram(pkt2);
    assertTrue(d1.hashCode() != d2.hashCode());
  }
}
