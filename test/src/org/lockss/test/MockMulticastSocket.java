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

/**
 * This class is a mock implementation of MulticastSocket to be used for
 * unit testing.
 */
public class MockMulticastSocket
  extends MulticastSocket implements MockDatagramSocketExtras {
  // argggh.  Want to extend MockDatagramSocket to get its functionality,
  // but can't.  So delegate to it.
  private MockDatagramSocket mds;

  public MockMulticastSocket() throws IOException {
    mds = new MockDatagramSocket();
  }

  /**
   * @param port this is ignored and only here to override the
   * DatagramSocket contructor
   */
  public MockMulticastSocket(int port) throws IOException {
    mds = new MockDatagramSocket(port);
  }

  /**
   * Flags this as a closed socket.  Can be tested with isClosed method
   *@see #isClosed
   */
  public void close(){
    mds.close();
  }

  public void connect(InetAddress address, int port){
    mds.connect(address, port);
  }

  public void disconnect(){
    mds.disconnect();
  }

  public InetAddress getInetAddress(){
    return mds.getInetAddress();
  }

  public InetAddress getLocalAddress(){
    return mds.getLocalAddress();
  }

  public int getLocalPort(){
    return mds.getLocalPort();
  }

  public int getPort(){
    return mds.getPort();
  }

  public int getReceiverBufferSize(){
    return mds.getReceiverBufferSize();
  }

  public int getSendBufferSize(){
    return mds.getSendBufferSize();
  }

  public int getSoTimeout(){
    return mds.getSoTimeout();
  }

  /**
   * "Receive" a packet that has been added to the receive queue with
   * {@link  #addToReceiveQueue}.  Waits until a packet arrives if necessary
   * @param p pre-allocated packet to receive data from queued packet
   */
  public void receive(DatagramPacket p) throws IOException {
    mds.receive(p);
  }

  /**
   * "Send" a packet by adding it to the output vector (which can be read with
   * {@link #getSentPackets})
   * @param p packet to "send"
   */
  public void send(DatagramPacket p) {
    mds.send(p);
  }

  public static void setDatagramSocketImplFactory(DatagramSocketImplFactory
						  fac) {
  }

  public void setReceiveBufferSize(int size){
    mds.setReceiveBufferSize(size);
  }

  public void setSendBufferSize(int size){
    mds.setSendBufferSize(size);
  }

  public void setSoTimeout(int timeout){
    mds.setSoTimeout(timeout);
  }


  //non-DatagramSocket methods

  /**
   * @return true if close() has been called on this socket, false otherwise
   * @see #close
   */
  public boolean isClosed(){
    // Java 1.4 MulticastSocket calls this from within its constructor, before
    // we have set mds
    return mds != null && mds.isClosed();
  }

  /**
   * @return a vector containing all packets which have been "sent" on the
   * socket
   * @see #send
   */
  public Vector getSentPackets(){
    return mds.getSentPackets();
  }

  /**
   * Add a packet to the receive queue to be processed by {@link #receive}
   */
  public void addToReceiveQueue(DatagramPacket packet){
    mds.addToReceiveQueue(packet);
  }

  //stubs for MulticastSocket methods

  public void joinGroup(InetAddress mcastaddr) {
  }

  public void leaveGroup(InetAddress mcastaddr) {
  }

  // Overridden method is deprecated in 1.4, jikes considers this illegal
//   public void send(DatagramPacket p, byte ttl) {
//     this.send(p);
//   }

}
