/*
 * $Id: LockssDatagram.java,v 1.3 2002-12-16 21:55:33 tal Exp $
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


/** LockssDatagram describes the datagram packets exchanged between LOCKSS
 * caches, and is the object passed from clients to the communications
 * layer.  Received packets are represented by the subclass, {@link
 * LockssReceivedDatagram}.
 */
public class LockssDatagram {
  /** Maximum data size */
  public static final int MAX_SIZE = 2048;
  public static final int PROTOCOL_TEST = 1;
  public static final int PROTOCOL_LCAP = 2;
  static final int HEADER_LENGTH = 4;

  byte[] payload;			// Client data
  int protocol;				// Client protocol

  /** Only for subclass. */
  LockssDatagram() {
  }

  /** Build a LockssDatagram to send a protocol message containing data.
   * @param protocol protocol number, one of PROTOCOL_xxx constants.
   * @param data byte array containing message data.
   */
  public LockssDatagram(int protocol, byte[] data) {
    this.protocol = protocol;
    this.payload = data;
  }

  /** Return a DatagramPacket suitable for sending to a specific address.
   * @param addr the destination address (or multicast group)
   * @param port the destination port
   * @return the packet
   */
  public DatagramPacket makeSendPacket(InetAddress addr, int port) {
    byte[] pktData = new byte[payload.length + HEADER_LENGTH];
    ByteArray.encodeInt(protocol, pktData, 0);
    System.arraycopy(payload, 0, pktData, HEADER_LENGTH, payload.length);
    return new DatagramPacket(pktData, pktData.length, addr, port);
  }

  /** Return the data portion of the packet */
  public byte[] getData() {
    return payload;
  }

  /** Return the protocol under which to send the packet */
  public int getProtocol() {
    return protocol;
  }

  public String toString() {
    return "[LDG: proto=" + protocol + ", size=" + payload.length + "]";
  }
}
