/*
 * $Id$
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
import org.lockss.config.Configuration;
import org.lockss.daemon.*;

/** A packet of data (protocol + payload) to be transmitted to, or received
 * from a peer.  API for setting/getting data is in terms of streams only,
 * as concrete implementations may hold data either in memory or on disk
 */
public abstract class PeerMessage {

  public static final int PROTOCOL_LCAP_V3_R1 = 10;
  public static final int PROTOCOL_LCAP_V3 = PROTOCOL_LCAP_V3_R1;

  private PeerIdentity senderId;

  private int protocol;

  private long expiration = 0;	  // Time after which to discard unsent msg
  private long retryInterval;	  // Target interval to wait between retries
  private long lastRetry;	  // For use by BlockingStreamComm.
  private int retryMax = 1;       // Max times to requeue message after
				  // connect fail or network error
  private int retryCount = 0;



  protected boolean isOutputOpen = false;

  /** Create a PeerMessage
   */
  PeerMessage() {
  }

  /** Return an InputStream on the payload.
   * @throws IllegalStateException if message data not stored yet
   */
  abstract public InputStream getInputStream()
      throws IllegalStateException, IOException;

  /** Return an OutputStream to which to write the payload.  May only be
   * called once.
   * @throws IllegalStateException if called a second time
   */
  abstract public OutputStream getOutputStream()
      throws IllegalStateException, IOException;

  /** Return true iff the data has been written and closed */
  abstract boolean hasData();

  /** Return the size of the data
   * @throws IllegalStateException if message data not stored yet
   */
  abstract public long getDataSize();

  /** Delete the message (desirable in some implementations to delete
   * backing file)
   */
  abstract public void delete();

  /** Set the message protocol
   */
  public void setProtocol(int protocol) {
    this.protocol = protocol;
  }

  /** Return the message's protocol number
   */
  public int getProtocol() {
    return protocol;
  }

  /** Set the message expiration time
   */
  public void setExpiration(long expiration) {
    this.expiration = expiration;
  }

  /** Return the message's expiration time
   */
  public long getExpiration() {
    return expiration;
  }

  public boolean isRequeueable() {
    return getExpiration() > 0;
  }

  public boolean isExpired() {
    long exp = getExpiration();
    return exp > 0 && TimeBase.nowMs() >= exp;
  }

  /** Return the PeerIdentity of the message sender, or null if not a
   * received message.
   */
  public PeerIdentity getSender() {
    return senderId;
  }

  /** Set the message sender */
  public void setSender(PeerIdentity sender) {
    this.senderId = sender;
  }

  /** Set the max number of retries */
  public void setRetryMax(int retryMax) {
    this.retryMax = retryMax;
  }

  /** Get the max number of retries */
  public int getRetryMax() {
    return retryMax;
  }

  /** Set the interval number of retries */
  public void setRetryInterval(long retryInterval) {
    this.retryInterval = retryInterval;
  }

  /** Get the targer retry interval */
  public long getRetryInterval() {
    return retryInterval;
  }

  /** Set the last retry time */
  public void setLastRetry(long lastRetry) {
    this.lastRetry = lastRetry;
  }

  /** Get the last retry time */
  public long getLastRetry() {
    return lastRetry;
  }

  /** Increment the message's retryCount
   */
  public void incrRetryCount() {
    this.retryCount++;
  }

  /** Return the message's retryCount
   */
  public int getRetryCount() {
    return retryCount;
  }

  protected void checkHasData() {
    if (!hasData()) {
      throw new IllegalStateException("PeerMessage data not set yet");
    }
  }

  public String toString(String type) {
    StringBuffer sb = new StringBuffer();
    sb.append("[");
    sb.append(type);
    sb.append("PeerMsg:");
    if (getSender() != null) {
      sb.append(" from ");
      sb.append(getSender());
    }
    sb.append(" proto=");
    sb.append(protocol);
    if (hasData()) {
      sb.append(", size=");
      sb.append(getDataSize());
    }
    sb.append("]");
    return sb.toString();
  }

  public boolean equals(Object o) {
    if (o instanceof PeerMessage) {
      PeerMessage msg = (PeerMessage)o;
      return (getSender() == msg.getSender()) && equalsButSender(msg);
    }
    return false;
  }

  public boolean equalsButSender(PeerMessage msg) {
    return (getProtocol() == msg.getProtocol()) &&
      (hasData() == msg.hasData()) &&
      (!hasData() ||
       getDataSize() == msg.getDataSize() && isEqualContent(this, msg));
  }

  private static boolean isEqualContent(PeerMessage m1, PeerMessage m2) {
    InputStream ins1 = null;
    InputStream ins2 = null;
    try {
      ins1 = m1.getInputStream();
      ins2 = m2.getInputStream();
      return StreamUtil.compare(ins1, ins2);
    } catch (IOException e) {
      return false;
    } finally {
      IOUtil.safeClose(ins1);
      IOUtil.safeClose(ins2);
    }
  }

  public int hashCode() {
    throw new UnsupportedOperationException("Unimplemented");
  }

  interface Factory {
    /** Create a new PeerMessage of the default implementation */
    public PeerMessage newPeerMessage();
    /** Create a new PeerMessage of an implementation appropriate for the
     * estimated data size */
    public PeerMessage newPeerMessage(long estSize);
  }

}
