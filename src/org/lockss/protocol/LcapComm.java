/*
 * $Id: LcapComm.java,v 1.1 2002-11-05 21:16:05 tal Exp $
 */

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

package org.lockss.protocol;
//import java.util.*;
import java.io.*;
import java.net.*;
import org.lockss.util.*;
import org.lockss.daemon.*;

/**
 * LcapComm implements the routing parts of the LCAP protocol, using
 * {@link LcapSocket} to send and receive packets.
 * Routing involves decisions about using unicast to supplement multicast,
 * including forwarding received unicast packets.
 */
public class LcapComm {

  protected static Logger log = Logger.getLogger("Comm");

  private static LcapComm singleton;
  private static LcapSocket sendSock;	// socket used for sending only

  // These may change if/when we use multiple groups/ports
  private static String groupName;	// multicast group
  private static InetAddress group;
  private static int multiPort;		// multicast port
  private LcapSocket.Multicast mSock;

  private FifoQueue socketInQ;		// received packets from LcapSocket
  private ReceiveThread rcvThread;

  /** Initialize and start the communications thread(s) */
  public static void startComm() {
    try {
      sendSock = new LcapSocket();
    } catch (SocketException e) {
      log.critical("Can't create send socket", e);
    }
    try {
      groupName = Configuration.getParam(Configuration.PREFIX + "group");
      multiPort = Configuration.getIntParam(Configuration.PREFIX + "port");
      group = InetAddress.getByName(groupName);
      if (group == null) {
	log.critical("null group addr");
	return;
      }
      singleton = new LcapComm();
      singleton.start();
    } catch (UnknownHostException e) {
      log.critical("Can't get group addr", e);
    } catch (Configuration.Error e) {
      log.critical("Multicase port not configured");
    }
  }

  /** Multicast a message to all caches holding the ArchivalUnit.
   * @param msg the message to send
   * @param au archival unit for which this message is relevant.  Used to
   * determine which multicast socket/port to send to.
   */
  public static void sendMessage(Message msg, ArchivalUnit au)
      throws IOException {
    sendMessageTo(msg, group, multiPort);
  }

  /** Unicast a message to a single cache.
   * @param msg the message to send
   * @param au archival unit for which this message is relevant.  Used to
   * determine which multicast socket/port to send to.
   * @param id the identity of the cache to which to send the message
   */
  public static void sendMessageTo(Message msg, ArchivalUnit au, Identity id)
      throws IOException {
    // tk - shouldn't be multiPort
    sendMessageTo(msg, id.getAddress(), multiPort);
  }

  private static void sendMessageTo(Message msg, InetAddress addr, int port)
      throws IOException {
    byte data[] = msg.encodeMsg();
    DatagramPacket pkt = new DatagramPacket(data, data.length, addr, port);
    sendSock.send(pkt);
  }

  private void start() {
    // This is where multiple multicast groups and/or ports would be started,
    // if they're all handled by the same thread.
    try {
      socketInQ = new FifoQueue();
      log.debug("new LcapSocket.Multicast("+socketInQ+", "+group+", "+multiPort);
      LcapSocket.Multicast mSock =
	new LcapSocket.Multicast(socketInQ, group, multiPort);
    
      mSock.start();
      this.mSock = mSock;
    } catch (UnknownHostException e) {
      log.error("Can't create socket", e);
    } catch (IOException e) {
      log.error("Can't create socket", e);
    }
    ensureQRunner();
  }

  // tk add watchdog
  private void ensureQRunner() {
    if (rcvThread == null) {
      log.info("Starting receive thread");
      rcvThread = new ReceiveThread("CommRcv");
      rcvThread.start();
    }
  }

  private void stop() {
    if (rcvThread != null) {
      log.info("Stopping Q runner");
      rcvThread.stopRcvThread();
      rcvThread = null;
    }
  }

  private void processReceivedPacket(LockssDatagram dgram) {
    Message msg;
    try {
      msg = Message.decodeToMsg(dgram.getPacket().getData(), 
				dgram.isMulticast());
      log.debug("Received " + msg);
    } catch (IOException e) {
      log.warning("Error decoding packet", e);
    }
  }

  // Receive thread
  private class ReceiveThread extends Thread {
    private boolean goOn = false;

    private ReceiveThread(String name) {
      super(name);
    }

    public void run() {
//        if (rcvPriority > 0) {
//  	Thread.currentThread().setPriority(rcvPriority);
//        }
      goOn = true;

      ProbabilisticTimer timeout = new ProbabilisticTimer(60000, 10000);
      try {
	while (goOn) {
	  Object qObj = socketInQ.get(timeout);
	  if (qObj != null) {
	    if (qObj instanceof LockssDatagram) {
	      processReceivedPacket((LockssDatagram)qObj);
	    } else {	      
	      log.warning("Non-LockssDatagram on rcv queue" + qObj);
	    }
	  }
	}
      } catch (InterruptedException e) {
	// tk ???
      } finally {
	rcvThread = null;
      }
    }

    private void stopRcvThread() {
      goOn = false;
      this.interrupt();
    }
  }
}
