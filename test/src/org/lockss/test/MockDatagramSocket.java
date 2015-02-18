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

public class MockDatagramSocket
  extends DatagramSocket implements MockDatagramSocketExtras {
  /**
   * This class is a mock implementation of DatagramSocket to be used for
   * unit testing
   */

  private boolean isClosed = false;
  private int localPort = -1;

  private Vector sentPackets;
  private SimpleQueue.Fifo receiveQueue;

  //stubs for DatagramSocket methods

  public MockDatagramSocket() throws SocketException{
    sentPackets = new Vector();
    receiveQueue = new SimpleQueue.Fifo();
  }

  /**
   * @param port this is ignored and only here to override the
   * DatagramSocket contructor
   */
  public MockDatagramSocket(int port) throws SocketException{
    this();
  }

  /**
   * @param port this is ignored and only here to override the
   * DatagramSocket contructor
   * @param laddr ditto.
   */
  public MockDatagramSocket(int port, InetAddress laddr)
      throws SocketException{
    this();
  }

  /**
   * Flags this as a closed socket.  Can be tested with isClosed method
   *@see #isClosed
   */
  public void close(){
    this.isClosed = true;
  }

  /**
   * Stubbed
   */
  public void connect(InetAddress address, int port){
    throw new UnsupportedOperationException("Not Implemented");
  }

  /**
   * Stubbed
   */
  public void disconnect(){
    throw new UnsupportedOperationException("Not Implemented");
  }

  /**
   * Stubbed
   */
  public InetAddress getInetAddress(){
    throw new UnsupportedOperationException("Not Implemented");
  }

  /**
   * Stubbed
   */
  public InetAddress getLocalAddress(){
    throw new UnsupportedOperationException("Not Implemented");
  }

  public int getLocalPort(){
    return localPort;
  }


  public void setLocalPort(int localPort) {
    this.localPort=localPort;
  }

  /**
   * Stubbed
   */
  public int getPort(){
    throw new UnsupportedOperationException("Not Implemented");
  }

  /**
   * Stubbed
   */
  public int getReceiverBufferSize(){
    throw new UnsupportedOperationException("Not Implemented");
  }

  /**
   * Stubbed
   */
  public int getSendBufferSize(){
    throw new UnsupportedOperationException("Not Implemented");
  }

  /**
   * Stubbed
   */
  public int getSoTimeout(){
    throw new UnsupportedOperationException("Not Implemented");
  }

  /**
   * "Receive" a packet that has been added to the receive queue with
   * {@link  #addToReceiveQueue}.  Waits until a packet arrives if necessary
   * @param p pre-allocated packet to receive data from queued packet
   */
  public void receive(DatagramPacket p) throws IOException {
    DatagramPacket qPkt = (DatagramPacket)receiveQueue.get();
    if (qPkt == null) {
      // didn't get a packet, must have been interrupted
      throw new IOException("MockDatagramSocket.receive interrupted");
    }
    // copy fields from "received" packet into p.
    // In Java 1.4 setData() can make the packet larger, so we must explicitly
    // set the length to the smaller of p and the received packet.
    int plen = p.getLength();
    p.setData(qPkt.getData());
    p.setLength(Math.min(plen, qPkt.getLength()));
    p.setAddress(qPkt.getAddress());
    p.setPort(qPkt.getPort());
  }

  /**
   * "Send" a packet by adding it to the output vector (which can be read with
   * {@link #getSentPackets})
   * @param p packet to "send"
   */
  public void send(DatagramPacket p){
    // enqueue a copy, as that's what a real DatagramSocket would do
    DatagramPacket qPkt =
      new DatagramPacket(new String(p.getData()).getBytes(),
			 p.getOffset(),
			 p.getLength(),
			 p.getAddress(),
			 p.getPort());
    sentPackets.add(qPkt);
  }

  public static void setDatagramSocketImplFactory(DatagramSocketImplFactory
						  fac){
    throw new UnsupportedOperationException("Not Implemented");
  }

  public void setReceiveBufferSize(int size){
    throw new UnsupportedOperationException("Not Implemented");
  }

  public void setSendBufferSize(int size){
    throw new UnsupportedOperationException("Not Implemented");
  }

  public void setSoTimeout(int timeout){
    throw new UnsupportedOperationException("Not Implemented");
  }


  //non-DatagramSocket methods

  /**
   * @return true if close() has been called on this socket, false otherwise
   * @see #close
   */
  public boolean isClosed(){
    return this.isClosed;
  }

  /**
   * @return a vector containing all packets which have been "sent" on the
   * socket
   * @see #send
   */
  public Vector getSentPackets(){
    return sentPackets;
  }

  /**
   * Add a packet to the receive queue to be processed by {@link #receive}
   */
  public void addToReceiveQueue(DatagramPacket packet){
    receiveQueue.put(packet);
  }
}
