/*
 * $Id: LockssReceivedDatagram.java,v 1.4 2003-01-21 18:33:48 tal Exp $
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
import java.net.*;
import java.util.*;
import org.lockss.util.*;

/**
 * LockssReceivedDatagram is a LockssDatagram for an incoming packet.  It
 * contains the received DatagramPacket, and knows how to hash and compare
 * incoming packets.
 */
public class LockssReceivedDatagram extends LockssDatagram {
  private DatagramPacket packet;	// received packet

  // Filled in for received packets.
  // These do not participate in equals() or hashCode().
  private boolean multicast;		// true iff verified multicast
  private LcapSocket rcvSocket;		// used for multicast spoof detection

  /** Build a LockssReceivedDatagram from a received DatagramPacket.
   * @param packet the received DatagramPacket
   */
  public LockssReceivedDatagram(DatagramPacket packet) {
    this.packet = packet;
  }

  /** Return the received DatagramPacket */
  public DatagramPacket getPacket() {
    return packet;
  }

  /** Return the sender's InetAddress */
  public InetAddress getSender() {
    return packet.getAddress();
  }

  /** Return the data portion of the received packet */
  public byte[] getData() {
    if (payload == null) {
      decodePacket();
    }
    return payload;
  }

  /** Return the protocol from the received packet */
  public int getProtocol() {
    if (payload == null) {
      decodePacket();
    }
    return protocol;
  }

  private void decodePacket() {
    int len = packet.getLength() - HEADER_LENGTH;
    if (len < 0) {
      throw new RuntimeException("short packet");
    }
    byte[] pktData = packet.getData();
    protocol = ByteArray.decodeInt(pktData, 0);
    payload = new byte[len];
    System.arraycopy(pktData, HEADER_LENGTH, payload, 0, len);
  }

  /** Return true iff the packet was a multicast packet */
  public boolean isMulticast() {
    return multicast;
  }

  /** Set the multicast flag */
  void setMulticast(boolean multicast) {
    this.multicast = multicast;
  }

  /** Return the LcapSocket on which the packet was received.  Used only
   * for multicast spoof detection */
  LcapSocket getReceiveSocket() {
    return rcvSocket;
  }

  /** Set the received LcapSocket */
  void setReceiveSocket(LcapSocket socket) {
    rcvSocket = socket;
  }

  /** Return true iff the underlying packets are the same, ignoring the
   * LcapSocket on which they were received.  Intended for use in multicast
   * spoof detection. */
  public boolean equals(Object o) {
    if (o instanceof LockssReceivedDatagram) {
      LockssReceivedDatagram d = (LockssReceivedDatagram)o;
      DatagramPacket opacket = d.getPacket();
      return (multicast == d.multicast &&
	      packet.getPort() == opacket.getPort() &&
	      packet.getLength() == opacket.getLength() &&
	      packet.getOffset() == opacket.getOffset() &&
	      packet.getAddress().equals(opacket.getAddress()) &&
	      Arrays.equals(packet.getData(), opacket.getData()));
    }
    return false;
  }

  // save from having to recalculate each time
  private int hash = -1;

  /** Hash the packet contents and address info.  Intended for use in
   * multicast spoof detection. */
  public int hashCode() {
    if (hash == -1) {
      hash = 0;
      byte b[] = packet.getData();
      int len = b.length;
      for (int ix = 0; ix < len; ix++) {
	hash = 31 * hash + b[ix];
      }
      hash ^= (packet.getAddress().hashCode() ^
	       (packet.getPort() << 15) ^
	       (packet.getLength() << 3) ^
	       (packet.getOffset() << 8));
    }
    return hash;
  }

  public String toString() {
    return "[LRDG: proto=" + protocol + ", " +
      (isMulticast() ? "M" : "U") +
      " from " + packet.getAddress() + "]";
  }
}
