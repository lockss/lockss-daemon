/*
 * $Id: MockLcapStreamRouter.java,v 1.1.2.12 2004-11-29 20:51:15 dshr Exp $
 */

/*

Copyright (c) 2004 Board of Trustees of Leland Stanford Jr. University,
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
import org.lockss.protocol.*;
import org.lockss.poller.*;
import java.util.*;
import java.io.IOException;
import org.lockss.app.*;
import org.lockss.plugin.*;
import org.lockss.util.*;

/**
 * MockLcapStreamRouter is a version of LcapStreamRouter that doesn't
 * actually send or receive messages,  it takes messages from a queue
 * and adds messages to a queue.  This allows a test program to behave
 * as if it were the poller or a voter in a V3 poll. It implements
 * Runnable in order that its Thread can pull messages off the
 * queue and run the handlers on them,  replacing the Thread in
 * LcapStreamComm that would normally pull them off a socket.
 *
 * The router can run in two modes. In loopback mode,  messages
 * sent are also received.  In bit-bucket mode,  messages are sent
 * to the bit-bucket.
 */
public class MockLcapStreamRouter extends LcapStreamRouter
  implements Runnable {

  boolean goOn = false;
  Thread myThread = null;
  boolean loopBackMode = false;
  FifoQueue myReceiveQueue = null;
  FifoQueue mySendQueue = null;
  static Logger log = Logger.getLogger("MockLcapStreamRouter");

  public MockLcapStreamRouter(FifoQueue recv, FifoQueue send) {
    myReceiveQueue = recv;
    if (send == null) {
      loopBackMode = true;
    } else {
      mySendQueue = send;
    }
  }


  public void startService() {
    if (false) {
      super.startService();
    }
    goOn = true;
    myThread = new Thread(this);
    myThread.start();
  }

  public void stopService() {
    goOn = false;
    // XXX wake up myThread
    if (false) {
      super.stopService();
    }
    myThread = null;
  }

  public void run() {
    log.debug("Q runner started");
    while (goOn) {
      Deadline dl = Deadline.in(10000);
      try {
	Object obj = myReceiveQueue.get(dl);
	if (obj != null) {
	  if (obj instanceof V3LcapMessage) {
	    V3LcapMessage msg = (V3LcapMessage) obj;
	    log.debug("Pulled " + msg + " from receive Q");
	    runHandlers(msg);
	  } else {
	    log.error("Pulled an instance of " + obj.getClass().toString());
	  }
	}
      } catch (InterruptedException ex) {
	log.debug("Q runner interrupted");
	// No action intended
      }
    }
  }

  /**
   * Simulate receiving a message
   */
  
  public void receiveMessage(LcapMessage msg) {
    myReceiveQueue.put(msg);
    log.debug("Received " + msg);
  }

  /** Simulate unicasting a message to a single cache.
   * @param msg the message to send
   * @param au archival unit for which this message is relevant.  Used to
   * determine which multicast socket/port to send to.
   * @param id the identity of the cache to which to send the message
   * @throws IOException if message couldn't be sent
   */
  public void sendTo(V3LcapMessage msg, ArchivalUnit au, PeerIdentity id)
    throws IOException {
    if (loopBackMode) {
      log.debug("mock sendTo(" + msg + ") queued");
      myReceiveQueue.put(msg);
      // myThread.interrupt();
    } else {
      log.debug("mock sendTo(" + msg + ") dropped");
      mySendQueue.put(msg);
    }      
    Thread.yield();
    if (origRateLimiter != null) {
      origRateLimiter.event();
    }
    log.debug("sent " + msg + " to " + id);
  }

  public FifoQueue getReceiveQueue() {
    return myReceiveQueue;
  }

  public FifoQueue getSendQueue() {
    return mySendQueue;
  }

  public V3LcapMessage getSentMessage(Deadline dl) {
    V3LcapMessage msg = null;
    if (!loopBackMode) {
      try {
	int s = mySendQueue.size();
	msg = (V3LcapMessage) mySendQueue.get(dl);
	if ((s - mySendQueue.size()) != 1) {
	  log.error("get didn't remove msg");
	}
      } catch (InterruptedException ex) {
	log.debug("getSentMessage() interrupted");
	// No action intended
      }
      if (msg != null && !msg.isNoOp()) {
	log.debug("Pulled " + msg + " from send Q");
      }
    }
    return msg;
  }

  public boolean sendQueueEmpty() {
    return (mySendQueue.isEmpty());
  }

}
