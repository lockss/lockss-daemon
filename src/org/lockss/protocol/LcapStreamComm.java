/*
 * $Id: LcapStreamComm.java,v 1.2.8.1 2009-11-03 23:44:52 edwardsb1 Exp $
 */

/*

Copyright (c) 2000-2008 Board of Trustees of Leland Stanford Jr. University,
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
import org.lockss.config.*;
import org.lockss.daemon.*;
import org.lockss.daemon.status.*;
import org.apache.commons.collections.map.LRUMap;
import org.lockss.app.*;
import org.lockss.poller.*;
import org.lockss.plugin.*;

/**
 * LcapStreamCom supports stream-based message communication
 * between LOCKSS peers.  The packets sent at this level are {@link
 * PeerMessage}s.
 */
public interface LcapStreamComm extends PeerMessage.Factory {
  static final String PREFIX = Configuration.PREFIX + "scomm.";
  static final String PARAM_ENABLED = PREFIX + "enabled";
  static final boolean DEFAULT_ENABLED = true;

  /** Listen socket backlog */
  public static final String PARAM_LISTEN_BACKLOG =
    PREFIX + "listenBacklog";
  static final int DEFAULT_LISTEN_BACKLOG = -1;

  /**
   * Register a {@link LcapStreamComm.MessageHandler}, which will be called
   * whenever a message is received.
   * @param protocol an int representing the protocol
   * @param handler MessageHandler to add
   */
  public void registerMessageHandler(int protocol, MessageHandler handler);

  /**
   * Unregister a {@link LcapStreamComm.MessageHandler}.
   * @param protocol an int representing the protocol
   */
  public void unregisterMessageHandler(int protocol);

  /** Send a message to a peer.
   * @param msg the message to send
   * @param id the identity of the peer to which to send the message
   * @throws IOException
   */
  public void sendTo(PeerMessage msg, PeerIdentity id, RateLimiter limiter)
      throws IOException;

  /** Return true iff all connections are authenticated; <i>ie</i>, we only
   * talk to known peers */
  public boolean isTrustedNetwork();

  /**
   * LcapStreamComm.MessageHandler defines the callback registered by
   * clients who want to process incoming messages
   */
  public interface MessageHandler {
    /**
     * Callback used to inform clients that a message has been received.
     * @param msg  the received PeerMessage
     * @see LcapStreamComm#registerMessageHandler */
    public void handleMessage(PeerMessage msg);
  }

}
