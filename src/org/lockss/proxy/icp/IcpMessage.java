/*
 * $Id: IcpMessage.java,v 1.1 2005-08-25 20:12:37 thib_gc Exp $
 */

/*

Copyright (c) 2000-2005 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.proxy.icp;

import java.net.*;

/**
 * <p>An abstraction for ICP messages as described in
 * {@link <a href="http://www.ietf.org/rfc/rfc2186.txt">RFC2186</a>}
 * and
 * {@link <a href="http://www.ietf.org/rfc/rfc2187.txt">RFC2187</a>}.</p>
 * @author Thib Guicherd-Callin
 * @see #ICP_VERSION
 * @see IcpBuilder
 */
public interface IcpMessage {

  /**
   * <p>Determines if this message contains a source return trip time
   * response.</p>
   * @return True if and only if this message is an ICP response and
   * has the source return trip time bit set.
   * @see #ICP_OP_HIT
   * @see #ICP_OP_HIT_OBJ
   * @see #ICP_OP_MISS
   * @see #ICP_OP_MISS_NOFETCH
   * @see #ICP_FLAG_SRC_RTT
   */
  public boolean containsSrcRttResponse();

  /**
   * <p>Returns the ICP length field of this message.</p>
   * <p>The ICP length field is not the same as the length of the
   * UDP packet.</p>
   * @return The length field as reported by the ICP message.
   */
  public short getLength();

  /**
   * <p>Retrieves this message's opcode.</p>
   * @return a byte representing the opcode of this message.
   * @see #ICP_OP_DECHO
   *
  public static final byte ICP_OP_DENIED = 22;

  public static final byte ICP_OP_ERR = 4;

  public static final byte ICP_OP_HIT = 2;

  public static final byte ICP_OP_HIT_OBJ = 23;

  public static final byte ICP_OP_INVALID = 0;

  public static final byte ICP_OP_MISS = 3;

  public static final byte ICP_OP_MISS_NOFETCH = 21;

  public static final byte ICP_OP_QUERY = 1;

  public static final byte ICP_OP_SECHO = 10;
   */
  public byte getOpcode();

  public int getOptionData();

  public int getOptions();

  public byte[] getPayloadObject();
  
  public short getPayloadObjectLength();

  public URL getPayloadUrl();

  public InetAddress getRequester();

  public int getRequestNumber();

  public InetAddress getSender();

  public short getSrcRttResponse();

  public InetAddress getUdpAddress();
  
  public int getUdpPort();
  
  public byte getVersion();

  public boolean requestsHitObj();
  
  public boolean requestsSrcRtt();
  
  public void setUdpPort(int port);
  
  public void setUdpAddress(InetAddress udpAddress);
  
  public static final int ICP_FLAG_HIT_OBJ = 0x80000000;
  
  public static final int ICP_FLAG_SRC_RTT = 0x40000000;

  public static final byte ICP_OP_DECHO = 11;

  public static final byte ICP_OP_DENIED = 22;

  public static final byte ICP_OP_ERR = 4;

  public static final byte ICP_OP_HIT = 2;

  public static final byte ICP_OP_HIT_OBJ = 23;

  public static final byte ICP_OP_INVALID = 0;

  public static final byte ICP_OP_MISS = 3;

  public static final byte ICP_OP_MISS_NOFETCH = 21;

  public static final byte ICP_OP_QUERY = 1;

  public static final byte ICP_OP_SECHO = 10;

  public static final int ICP_PORT = 3130;
  
  public static final byte ICP_VERSION = 2;

  public static final int MAX_LENGTH = 1450;

}
