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

import java.net.*;
import java.util.*;
import junit.framework.TestCase;

public class TestDatagramSocketListener extends LockssTestCase {
  //private static final int port = 9898;
  private int port = 12345;
  private static final String host = "127.0.0.1";
  private DatagramSocketListener dsl = null;
  private static String lockVar = "blah"; //FIXME


  public TestDatagramSocketListener(String msg){
    super(msg);
  }
   public void testNoPacketSentGivesNull() throws DatagramSocketListenerException {
     DatagramSocketListener dsl =
       DatagramSocketListener.createOnOpenPort(0);

     DatagramPacket receivePacket = dsl.getPacket();
     assertNull(receivePacket);
   }

  public void testSinglePacket() throws Exception{
     DatagramSocketListener dsl =
       DatagramSocketListener.createOnOpenPort(1);

    String msg = "blah";
    byte[] msgBytes = msg.getBytes();

    DatagramPacket sentPacket =
      new DatagramPacket(msgBytes,
			 msg.length(),
			 InetAddress.getByName(host),
			 dsl.getPort());
    DatagramSocket socket = new DatagramSocket();
    socket.send(sentPacket);
    TimerUtil.guaranteedSleep(10);
    socket.close();

    DatagramPacket receivePacket = dsl.getPacket();
    assertTrue(receivePacket != null);

    byte[] recData = receivePacket.getData();
    byte[] sentData = sentPacket.getData();

    assertEquals(sentPacket.getLength(), receivePacket.getLength());

    for (int ix=0; ix<sentPacket.getLength(); ix++){
      assertEquals(sentData[ix], recData[ix]);
    }
  }

  public void testMultiplePackets() throws Exception{
    DatagramSocketListener dsl =
      DatagramSocketListener.createOnOpenPort(3);

    Vector sentPackets = new Vector();
    for (int ix=0; ix<3; ix++){
      String msg = ix+"blah"+ix;
      byte[] msgBytes = msg.getBytes();

      DatagramPacket sendPacket =
	new DatagramPacket(msgBytes,
			   msg.length(),
			   InetAddress.getByName(host),
			   dsl.getPort());
      DatagramSocket socket = new DatagramSocket();
      socket.send(sendPacket);
      TimerUtil.guaranteedSleep(10);
      socket.close();
      sentPackets.add(sendPacket);
    }

    for (int ix=0; ix<3; ix++){
      DatagramPacket receivePacket = dsl.getPacket();
      assertTrue(receivePacket != null);
      DatagramPacket sentPacket = (DatagramPacket)sentPackets.elementAt(ix);
      byte[] recData = receivePacket.getData();
      byte[] sentData = sentPacket.getData();

      assertEquals(receivePacket.getLength(), sentPacket.getLength());
      for (int jx=0; jx<sentPacket.getLength(); jx++){
	assertEquals(sentData[jx], recData[jx]);
      }
    }
  }

  public void testBlah() throws Exception{
    DatagramSocketListener.createOnOpenPort(5);
  }

}

