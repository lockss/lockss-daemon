/*
 * $Id: MockLcapStreamRouter.java,v 1.1.2.1 2004-09-30 01:06:33 dshr Exp $
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

package org.lockss.protocol;
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
 */
public class MockLcapStreamRouter extends LcapStreamRouter
  implements Runnable {

  boolean goOn = false;
  Thread myThread = null;
  FifoQueue theReceiveQueue = null;
  FifoQueue theSendQueue = null;


  public void startService() {
    super.startService();
    theReceiveQueue = new FifoQueue();
    theSendQueue = new FifoQueue();
    goOn = true;
    myThread = new Thread(this);
    myThread.start();
  }

  public void stopService() {
    goOn = false;
    // XXX wake up myThread
    super.stopService();
    myThread = null;
    theReceiveQueue = null;
    theSendQueue = null;
  }

  public void run() {
    LcapMessage msg = null;
    while (goOn) {
      Deadline dl = Deadline.in(10000);
      try {
	msg = (LcapMessage) theReceiveQueue.get(dl);
      } catch (InterruptedException ex) {
	// No action intended
      }
      if (msg != null && !msg.isNoOp()) {
	runHandlers(msg);
      }
    }
  }

  /**
   * Simulate receiving a message
   */
  
  public void receiveMessage(LcapMessage msg) {
    theReceiveQueue.put(msg);
  }

  /** Simulate unicasting a message to a single cache.
   * @param msg the message to send
   * @param au archival unit for which this message is relevant.  Used to
   * determine which multicast socket/port to send to.
   * @param id the identity of the cache to which to send the message
   * @throws IOException if message couldn't be sent
   */
  public void sendTo(LcapMessage msg, ArchivalUnit au, PeerIdentity id)
      throws IOException {
    log.debug2("sendTo(" + msg + ", " + id + ")");
    theSendQueue.put(msg);
    origRateLimiter.event();
  }

}
