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
import java.io.*;
import java.util.*;

/**
 * This class is quick and dirty creation to set up a datagram server which will
 * grab the first packet that comes in and then exit.  It's used to test
 * org.lockss.util.SyslogTarget
 */

public class DatagramSocketListener implements Runnable{
  private static final int BEGINNING_PORT=12000;
  private int port;
  private static final String host = "127.0.0.1";
  private DatagramSocket socket = null;
  private DatagramPacket packet = null;
  private Vector packets;
  private int numPacketsToGet = 0;
  private int numPacketsGot = 0;
  private int nextPacket=0;
  private SimpleBinarySemaphore readyToReceive = new SimpleBinarySemaphore();
  private SimpleBinarySemaphore gotPacket = new SimpleBinarySemaphore();
  private static Random random = new Random();

  /**
   * Creates a DatagramSocketListener object.  beginListening() needs to be
   * called before it will actually listen to the port, though.
   * @param port the port to listen to
   * @param numPackets number of packets to get before closing the socket
   * @see #beginListening
   */
  public DatagramSocketListener(int port, int numPackets) {
    packets = new Vector();
    this.port = port;
    numPacketsToGet = numPackets;
  }

  /**
   * Starts up a separate thread listening to the specified port for datagram
   * packets.  It will then sleep for an interval and check that the object
   * is actually listening.
   * @throws DatagramSocketListenerException if the listener is not ready after
   * a certain interval.
   */
  public void beginListening()
    throws DatagramSocketListenerException, BindException{
    new Thread(this).start();
    if (numPacketsToGet > 0 && !readyToReceive.take(2000)) {
      throw new DatagramSocketListenerException("Listener thread not ready");
    }
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
        readyToReceive.give();
        socket.receive(packet);
        numPacketsGot++;
        synchronized(packets){
          packets.add(packet);
          gotPacket.give();
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
   * @throws DatagramSocketListenerException if called before all expected
   * packages have been received
   */
  public DatagramPacket getPacket() throws DatagramSocketListenerException {
    while(numPacketsToGet > numPacketsGot) {
      if (!gotPacket.take(2000)){//wait 2 seconds for another packet
	String msg = "getPacket called before all packets were sent";
	throw new DatagramSocketListenerException(msg);
      }
    }

    synchronized(packets){
      if (packets.size() <= 0)
	return null;
      return (DatagramPacket)packets.elementAt(nextPacket++);
    }
  }

  /**
   * @param numPackets number of packets you want the listener to expect
   * @return a new <code>DatagramSocketListener</code> object bound to
   * an open port.
   */
  public static DatagramSocketListener createOnOpenPort(int numPackets)
      throws DatagramSocketListenerException {
    boolean done = false;
    DatagramSocketListener dsl = null;
    while (!done) {
      done = true;
      int port = random.nextInt(1000)+BEGINNING_PORT;
      try{
	dsl = new DatagramSocketListener(port, numPackets);
	dsl.beginListening();
      }
      catch (BindException be){
	done = false;
      }
    }
    return dsl;
  }

  /**
   * @return the port that this object is bound to or will bind to when
   * <code>beginListening()</code> is called
   * @see #beginListening()
   */
   public int getPort(){
    return port;
  }

}
