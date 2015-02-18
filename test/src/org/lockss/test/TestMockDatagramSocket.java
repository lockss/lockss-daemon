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

package org.lockss.test;
import java.io.*;
import java.net.*;
import java.util.*;

import junit.framework.TestCase;


public class TestMockDatagramSocket extends LockssTestCase{
  private MockDatagramSocket ds = null;

  public TestMockDatagramSocket(String msg){
    super(msg);
  }

  public void setUp() throws Exception {
    super.setUp();
    try{
      ds = new MockDatagramSocket();
    }
    catch (SocketException se){
      se.printStackTrace();
    }
  }

  public void testIsClosedWhenNotClosed(){
    assertFalse(ds.isClosed());
  }

  public void testIsClosedWhenIsClosed(){
    ds.close();
    assertTrue(ds.isClosed());
  }

  public void testNoPacketSentReturnsNull(){
    Vector packets = ds.getSentPackets();
    assertEquals(0, packets.size());
  }

  public void testOnePacketSentReturnsOne() throws UnknownHostException{
    String dataStr = "This is test data";
    byte[] data = dataStr.getBytes();
    InetAddress addr = InetAddress.getByName("127.0.0.1");
    int port = 1234;
    DatagramPacket sendPacket =
      new DatagramPacket(data, data.length, addr, port);
    ds.send(sendPacket);
    Vector packets = ds.getSentPackets();
    assertEquals(1, packets.size());
    verifyEqual(sendPacket, (DatagramPacket)packets.elementAt(0));
  }

  public void testMultiPacketSentReturnsMulti() throws UnknownHostException{
    Vector sentPackets = new Vector();
    int numPackets = 5;
    for (int ix=0; ix<numPackets; ix++){
      String dataStr = ix+"This is test data"+ix;
      byte[] data = dataStr.getBytes();
      InetAddress addr = InetAddress.getByName("127.0.0.1");
      int port = 1234;
      DatagramPacket sendPacket =
	new DatagramPacket(data, data.length, addr, port);
      sentPackets.add(sendPacket);
      ds.send(sendPacket);
    }
    Vector packets = ds.getSentPackets();
    assertEquals(numPackets, packets.size());
    for (int ix=0; ix<numPackets; ix++){
      verifyEqual((DatagramPacket)sentPackets.elementAt(ix),
		  (DatagramPacket)packets.elementAt(ix));
    }
  }

  public void testReceiveWithOutSetPacketsWaits() {
    DatagramPacket packet = createEmptyPacket(10);
    Interrupter intr = null;
    try {
      intr = interruptMeIn(TIMEOUT_SHOULD);
      ds.receive(packet);
      fail("receive() returned when no packets");
    } catch (IOException e) {
      // this is what we're expecting
    } finally {
      intr.cancel();
      if (!intr.did()) {
	fail("receive() when no packets, returned");
      }
    }
  }

  public void testReceivePacketGetsOnePacketSameSize() throws Exception{
    byte data[] = "Test data".getBytes();
    DatagramPacket packet =
      new DatagramPacket(data, data.length,
			 InetAddress.getByName("127.0.0.1"), 1234);
    ds.addToReceiveQueue(packet);

    DatagramPacket receivedPacket = createEmptyPacket(data.length);
    ds.receive(receivedPacket);
    verifyEqual(packet, receivedPacket);
  }

  public void testReceivePacketHandlesTruncation() throws Exception{
    byte data[] = "Test data".getBytes();
    DatagramPacket packet =
      new DatagramPacket(data, data.length,
			 InetAddress.getByName("127.0.0.1"), 1234);
    ds.addToReceiveQueue(packet);

    DatagramPacket receivedPacket = createEmptyPacket(data.length-1);
    ds.receive(receivedPacket);
    assertEquals(data.length-1, receivedPacket.getLength());
    verifyEqualUptoTruncation(packet, receivedPacket);
  }

  public void testReceivePacketGetsMultiPackets() throws Exception{
    int numPackets = 5;
    Vector queuedPackets = new Vector();
    for (int ix = 0; ix <= numPackets; ix++){
      String dataStr = ix+"Test data"+ix;
      byte data[] = dataStr.getBytes();
      DatagramPacket packet =
	new DatagramPacket(data, data.length,
			   InetAddress.getByName("127.0.0.1"), 1234);
      ds.addToReceiveQueue(packet);
      queuedPackets.add(packet);
    }

    for (int ix = 0; ix <= numPackets; ix++){
      DatagramPacket expectedPacket =
	(DatagramPacket)queuedPackets.elementAt(ix);
      DatagramPacket receivedPacket =
	createEmptyPacket(expectedPacket.getLength());
      ds.receive(receivedPacket);
      verifyEqual(receivedPacket, expectedPacket);
    }
  }



  private void verifyEqual(DatagramPacket pac1, DatagramPacket pac2){
    assertEquals(pac1.getPort(), pac2.getPort());
    assertEquals(pac1.getAddress(), pac2.getAddress());
    assertEquals(pac1.getLength(), pac2.getLength());
    byte[] data1 = pac1.getData();
    byte[] data2 = pac2.getData();
    for (int ix=0; ix<data1.length; ix++){
      assertEquals(data1[ix], data2[ix]);
    }
  }

  private void verifyEqualUptoTruncation(DatagramPacket truncatedPac,
					 DatagramPacket fullPac){
    assertEquals(truncatedPac.getPort(), fullPac.getPort());
    assertEquals(truncatedPac.getAddress(), fullPac.getAddress());
    byte[] data1 = truncatedPac.getData();
    byte[] data2 = fullPac.getData();
    for (int ix=0; ix<data1.length; ix++){
      assertEquals(data1[ix], data2[ix]);
    }
  }

  private DatagramPacket createEmptyPacket(int length){
    byte[] data = new byte[length];
    DatagramPacket packet = new DatagramPacket(data, length);
    return packet;
  }

}
