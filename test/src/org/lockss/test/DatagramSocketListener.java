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

package org.lockss.test;
import java.net.*;
import java.io.*;
import java.util.*;

/**
 * This class is quick and dirty creation to set up a datagram server which will
 * grab the first packet that comes in and then exit.  It's used to test 
 * org.lockss.util.SyslogTarget
 */

public class DatagramSocketListener implements Runnable{
  private int port;
  private static final String host = "127.0.0.1";;
  private DatagramSocket socket = null;
  private DatagramPacket packet = null;
  private Vector packets;
  private int numPacketsToGet;
  private int numPacketsGot = 0;

  /**
   * Creates a DatagramSocketListener object
   * @param port the port to listen to
   */
  public DatagramSocketListener(int port, int numPackets){
    packets = new Vector();
    this.port = port;
    numPacketsToGet = numPackets;
  }

  /**
   * Tells the object to connect, and get numPackets packets
   */
  public void run(){
    try{
      socket = new DatagramSocket(port, InetAddress.getByName(host));
      int length = socket.getReceiveBufferSize();
      while(numPacketsGot < numPacketsToGet){
	packet = new DatagramPacket(new byte[length], length);
	socket.receive(packet);
	numPacketsGot++;
	synchronized(packets){
	  System.err.println("Got a packet");
	  packets.add(packet);
	}
      }
      socket.close();
    }
    catch (IOException ioe){
      ioe.printStackTrace();
    }
  }
  
  /**
   * @return the last packet send to this listener, or null if none have been
   */
  public DatagramPacket getPacket(){
    synchronized(packets){
      if (packets.size() <= 0)
	return null;
      return (DatagramPacket)packets.remove(0);
    }
  }

}
    
    
