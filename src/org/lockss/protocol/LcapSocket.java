/*
 * $Id: LcapSocket.java,v 1.17 2006-06-04 06:25:53 tlipkis Exp $
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
import java.io.*;
import java.net.*;
import java.util.*;
import org.lockss.util.*;
import org.lockss.util.Queue;
import org.lockss.daemon.*;

/** Send and receive unicast and multicast datagrams.
 */
public class LcapSocket {
  static final String PRIORITY_PARAM_SOCKET = "Socket";
  static final int PRIORITY_DEFAULT_SOCKET = -1;

  Logger log;

  DatagramSocket sock;

  /** Create an LcapSocket to send packets on a new datagram socket. */
  public LcapSocket() throws SocketException {
    this(new DatagramSocket());
  }

  /** Create an LcapSocket to send packets on the datagram socket.
   * Intended for internal use and testing. */
  LcapSocket(DatagramSocket sock) {
    this.sock = sock;
  }

  /** Return the underlying DatagramSocket */
  DatagramSocket getSocket() {
    return sock;
  }

  /** Send a packet
   * @param pkt the DatagramPacket to send
   */
  public void send(DatagramPacket pkt) throws IOException {
    sock.send(pkt);
  }

  /** Abstract base class for receive sockets */
  abstract static class RcvSocket extends LcapSocket {
    Queue rcvQ;
    private ReceiveThread rcvThread;

    /** Subclasses should call this with the socket they create */
    protected RcvSocket(Queue rcvQ, DatagramSocket sock) {
      super(sock);
      this.rcvQ = rcvQ;
      log = Logger.getLogger(getThreadName());
    }

    /** Subclasses must implement this to handle received packets */
    abstract void processReceivedDatagram(LockssReceivedDatagram dg);

    /** Convenience method to make names for loggers and threads */
    String getThreadName() {
      return "Sock_" + sock.getLocalPort() + "_Rcv";
    }

    /** Start the socket's receive thread */
    public void start() {
      ensureRcvThread();
    }

    // tk add watchdog
    private void ensureRcvThread() {
      if (rcvThread == null) {
	log.info("Starting receive thread");
	rcvThread = new ReceiveThread(getThreadName());
	rcvThread.start();
	rcvThread.waitRunning();
      }
    }

    /** Stop the socket's receive thread */
    public void stop() {
      if (rcvThread != null) {
	log.info("Stopping rcv thread");
	rcvThread.stopRcvThread();
	rcvThread = null;
      }
    }

    /** Receive one packet, build a DatagramPacket, call the LcapSocket's
	processReceivedDatagram() */
    private void receivePacket() throws IOException {
      byte buf[] = new byte[LockssReceivedDatagram.MAX_SIZE];
      DatagramPacket pkt =
	new DatagramPacket(buf, LockssReceivedDatagram.MAX_SIZE);
      try {
	sock.receive(pkt);
	LockssReceivedDatagram dg = new LockssReceivedDatagram(pkt);
	dg.setReceiveSocket(this);
	processReceivedDatagram(dg);
      } catch (InterruptedIOException e) {
      }
    }

    // Receive thread
    private class ReceiveThread extends LockssThread {
      private volatile boolean goOn = true;

      private ReceiveThread(String name) {
	super(name);
      }

      public void lockssRun() {
	triggerWDogOnExit(true);
	setPriority(PRIORITY_PARAM_SOCKET, PRIORITY_DEFAULT_SOCKET);
	nowRunning();

	try {
	  while (goOn) {
	    receivePacket();
	  }
//        } catch (InterruptedException e) {
	} catch (IOException e) {
	  // tk - what do to here?
	  log.warning("receive()", e);
	} finally {
	  rcvThread = null;
	}
      }

      private void stopRcvThread() {
	triggerWDogOnExit(false);
	goOn = false;
	this.interrupt();
      }
    }
  }

  /** LcapSocket.Unicast receives packets on a unicast socket */
  public static class Unicast extends RcvSocket {

    /* Create a Unicast socket that receives packets sent to <i>port</i>,
     * and puts them on the specified queue.
     @param rcvQ The queue onto which to put received packets
     @param port The UDP port to which tobind the socket
    */
    public Unicast(Queue rcvQ, int port) throws SocketException {
      this(rcvQ, new DatagramSocket(port));
    }

    /** Create a Unicast receive socket that receives from the datagram socket.
     * Intended for internal use and testing only. */
    Unicast(Queue rcvQ, DatagramSocket sock) {
      super(rcvQ, sock);
    }

    /* Mark the packet as unicast, add to the queue */
    void processReceivedDatagram(LockssReceivedDatagram dg) {
      dg.setMulticast(false);
      if (log.isDebug2()) {
	log.debug2("Received " + dg);
      }
      rcvQ.put(dg);
    }

    String getThreadName() {
      return "U" + super.getThreadName();
    }
  }

  /** LcapSocket.Multicast receives multicast packets.
   * It identifies as multicast only those packets that were actually
   * multicast (<i>ie</i>, not just unicast to a multicast port on this
   * machine
   */
  public static class Multicast extends RcvSocket {

    /** Create a Multicast socket that receives packets sent to <i>port</i>,
     * and puts them on the specified queue.
     * @param rcvQ The queue onto which to put received packets
     * @param grp The multicast group to join
     * @param port The UDP port to which to bind the socket
    */
    public Multicast(Queue rcvQ, IPAddr grp, int port)
	throws IOException {
      this(rcvQ, new MulticastSocket(port), grp);
    }

    /** Create a Multicast receive socket that receives from the multicast
     * socket.  Intended for internal use and testing only. */
    Multicast(Queue rcvQ, MulticastSocket sock, IPAddr grp)
	throws IOException {
      super(rcvQ, sock);
      sock.joinGroup(grp.getInetAddr());
    }

    /** Mark the packet as multicast, add to the queue */
    void processReceivedDatagram(LockssReceivedDatagram dg) {
      dg.setMulticast(true);
      if (log.isDebug2()) {
	log.debug2("Received " + dg);
      }
      rcvQ.put(dg);
    }

    String getThreadName() {
      return "M" + super.getThreadName();
    }
  }
}
