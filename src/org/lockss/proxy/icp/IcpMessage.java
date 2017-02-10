/*
 * $Id$
 */

/*

Copyright (c) 2000-2007 Board of Trustees of Leland Stanford Jr. University,
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

import java.net.DatagramPacket;

import org.lockss.util.IPAddr;

/**
 * <p>An abstraction for ICP messages as described in
 * {@link <a href="http://www.ietf.org/rfc/rfc2186.txt">RFC2186</a>}
 * and
 * {@link <a href="http://www.ietf.org/rfc/rfc2187.txt">RFC2187</a>}.</p>
 * @author Thib Guicherd-Callin
 * @see #ICP_VERSION
 * @see IcpFactory
 */
public interface IcpMessage {

  /**
   * <p>If this message is a query, determines if this message
   * has the hit object bit set.</p>
   * @return True if and only if this message is a query and has the
   *         hit object bit set.
   * @see #isQuery
   * @see #ICP_FLAG_HIT_OBJ
   */
  public boolean requestsHitObj();

  /**
   * <p>If this message is a query, determines if this message
   * has the source return time trip bit set.</p>
   * @return True if and only if this message is a query and has the
   *         source return trip time bit set.
   * @see #isQuery
   * @see #ICP_FLAG_SRC_RTT
   */
  public boolean requestsSrcRtt();

  /**
   * <p>If this message is an ICP response, determines if this message
   * contains a source return trip time response.</p>
   * @return True if and only if this message is an ICP response and
   * has the source return trip time bit set.
   * @see #isResponse
   * @see #ICP_FLAG_SRC_RTT
   */
  boolean containsSrcRttResponse();

  /**
   * <p>Returns the ICP length field of this message.</p>
   * <p>The ICP length field is not the same as the length of the
   * UDP packet.</p>
   * @return The length field as reported by the ICP message.
   */
  short getLength();

  /**
   * <p>Retrieves this message's opcode.</p>
   * @return A byte representing the opcode of this message.
   * @see #ICP_OP_DECHO
   * @see #ICP_OP_DENIED
   * @see #ICP_OP_ERR
   * @see #ICP_OP_HIT
   * @see #ICP_OP_HIT_OBJ
   * @see #ICP_OP_INVALID
   * @see #ICP_OP_MISS
   * @see #ICP_OP_MISS_NOFETCH
   * @see #ICP_OP_QUERY
   * @see #ICP_OP_SECHO
   */
  byte getOpcode();

  /**
   * <p>Retrieves the raw integer of option data.</p>
   * @return The option data field.
   * @see #getOptions()
   * @see #getSrcRttResponse()
   */
  int getOptionData();

  /**
   * <p>Retrieves the options bit field.</p>
   * @return The options bit field.
   * @see #containsSrcRttResponse
   * @see #getOptionData
   * @see #requestsHitObj
   * @see #requestsSrcRtt
   * @see #ICP_FLAG_HIT_OBJ
   * @see #ICP_FLAG_SRC_RTT
   */
  int getOptions();

  /**
   * <p>If this message has a hit-object opcode, retrieves this
   * message's hit object as an array of bytes.</p>
   * @return An array of bytes representing the object that piggy-
   *         backed in the ICP packet if this message has a hit-object
   *         opcode, null otherwise.
   * @see #getOpcode
   * @see #getPayloadObjectLength
   */
  byte[] getPayloadObject();

  /**
   * <p>If this message has a hit-object opcode, retrieves this
   * message's hit object length field.</p>
   * <p>The return value should, but may not, be the same as the value
   * of <code>getPayloadObject().length</code>.</p>
   * @return The self-reported payload object length field if this
   *         message has a hit-object opcode, 0 otheriwse.
   * @see #getOpcode
   * @see #getPayloadObject
   */
  short getPayloadObjectLength();

  /**
   * <p>Retrieves the URL contained in this message.</p>
   * @return This message's URL.
   */
  String getPayloadUrl();

  /**
   * <p>If this message is an ICP query, retrieves the address of the
   * initial requester.</p>
   * <p>The requester is the originator of the request. It may not be
   * the same as the IP from which this ICP message originated.</p>
   * @return The IP of the original requester of the URL if this
   *         message is a query, null otherwise.
   * @see #getSender
   * @see #getUdpAddress
   * @see #getUdpPort
   * @see #isQuery
   */
  IPAddr getRequester();

  /**
   * <p>Retrieves the opaque request number.</p>
   * @return The identifying number of this message.
   */
  int getRequestNumber();

  /**
   * <p>Retrieves the ICP sender field.</p>
   * <p>The ICP specification instructs implementors to trust the
   * sender information reported by the UDP packet. It is likely that
   * the address returned by this method is <code>0.0.0.0</code> or
   * some other meaningless value.</p>
   * @return The sender's IP address.
   */
  IPAddr getSender();

  /**
   * <p>If this message is an ICP response and contains a source
   * return trip time response, retrieves the latter from this
   * message.</p>
   * @return The source return trip time response if this message
   *         contains one, 0 otherwise.
   * @see #containsSrcRttResponse
   * @see #isResponse
   */
  short getSrcRttResponse();

  /**
   * <p>Retrieves the IP address from which the UDP packet carrying
   * this message was received.</p>
   * <p>If this message was not actually received from a UDP packet,
   * the return value will be meaningless, and shold be null.</p>
   * @return The sender's IP address as reported by the UDP packet;
   *         null if not applicable.
   * @see #getSender
   * @see #getRequester
   * @see #getUdpPort
   */
  IPAddr getUdpAddress();

  /**
   * <p>Retrieves the port from which this message's sender sent the
   * UDP packet.</p>
   * <p>If this message was not actually received from a UDP packet,
   * the return value will be meaningless, and shold be zero.</p>
   * @return The origin's port number as reported by the UDP packet;
   *         zero if not applicable.
   * @see #getSender
   * @see #getRequester
   * @see #getUdpAddress
   */
  int getUdpPort();

  /**
   * <p>Returns this message's ICP version field.</p>
   * @return The ICP version field of this message.
   * @see #ICP_VERSION
   */
  byte getVersion();

  /**
   * <p>Determines if this message is an ICP query.</p>
   * <p>An ICP message is an ICP query if and only if its opcode is
   * {@link #ICP_OP_QUERY}.</p>
   * @return True if and only if this message's opcode is the
   *         query opcode.
   * @see #getOpcode
   * @see #ICP_OP_QUERY
   */
  boolean isQuery();

  /**
   * <p>Determines if this message is an ICP response.</p>
   * <p>An ICP message is an ICP response if and only if its opcode is
   * one of {@link #ICP_OP_HIT}, {@link #ICP_OP_HIT_OBJ},
   * {@link #ICP_OP_MISS} or {@link #ICP_OP_MISS_NOFETCH}.</p>
   * @return True if and only if this message's opcode is the hit,
   * hit-object, miss or miss-no-fetch opcode.
   * @see #getOpcode
   * @see #ICP_OP_HIT
   * @see #ICP_OP_HIT_OBJ
   * @see #ICP_OP_MISS
   * @see #ICP_OP_MISS_NOFETCH
   */
  boolean isResponse();

  /**
   * <p>Produces a denied message in response to this message.</p>
   * @return A denied message in response to the query.
   * @throws IcpException if this message is not a query.
   * @see #isQuery
   */
  IcpMessage makeDenied()
      throws IcpException;

  /**
   * <p>Produces an error message in response to this message.</p>
   * @return An error message in response to this message.
   * @throws IcpException if this message is not a query.
   * @see #isQuery
   */
  IcpMessage makeError()
      throws IcpException;

  /**
   * <p>Produces a hit response to this message.</p>
   * @return A hit response based on this message.
   * @throws IcpException if this message is not a query.
   * @see #isQuery
   */
  IcpMessage makeHit()
      throws IcpException;

  /**
   * <p>Produces a hit response to this message, with the given source
   * return trip time.</p>
   * @param srcRttResponse A source return trip time.
   * @return A hit response based on this message.
   * @throws IcpException if this message is not a query, or
   *                              if the query did not request a
   *                              source return trip time response.
   * @see #isQuery
   * @see #requestsSrcRtt
   */
  IcpMessage makeHit(short srcRttResponse)
      throws IcpException;

  /**
   * <p>Produces a hit-object response to this message using the given
   * array of bytes.</p>
   * @param payloadObject A payload as an array of bytes.
   * @return A hit-object response based on this message.
   * @throws IcpException if this message is not a query.
   * @throws NullPointerException if payloadObject is null.
   * @see #isQuery
   */
  IcpMessage makeHitObj(byte[] payloadObject)
      throws IcpException;

  /**
   * <p>Produces a hit-object response to this message using the given
   * array of bytes, with the given source return trip time.</p>
   * @param srcRttResponse A source return trip time.
   * @param payloadObject  A payload as an array of bytes.
   * @return A hit response based on this message.
   * @throws IcpException if this message is not a query, or
   *                              if the query did not request a
   *                              source return trip time response.
   * @throws NullPointerException if payloadObject is null.
   * @see #isQuery
   * @see #requestsSrcRtt
   */
  IcpMessage makeHitObj(short srcRttResponse,
                        byte[] payloadObject)
      throws IcpException;

  /**
   * <p>Produces a miss response to this message.</p>
   * @return A miss response based on this message.
   * @throws IcpException if this message is not a query.
   * @see #isQuery
   */
  IcpMessage makeMiss()
      throws IcpException;

  /**
   * <p>Produces a miss response to this message, with the given
   * source return trip time.</p>
   * @param srcRttResponse A source return trip time.
   * @return A miss response based on this message.
   * @throws IcpException if this message is not a query, or
   *                              if the query did not request a
   *                              source return trip time response.
   * @see #isQuery
   * @see #requestsSrcRtt
   */
  IcpMessage makeMiss(short srcRttResponse)
      throws IcpException;

  /**
   * <p>Produces a miss-no-fectch response to this message.</p>
   * @return A miss-no-fetch response based on this message.
   * @throws IcpException if this message is not a query.
   * @see #isQuery
   */
  IcpMessage makeMissNoFetch()
      throws IcpException;

  /**
   * <p>Produces a miss-no-fetch response to this message, with the
   * given source return trip time.</p>
   * @param srcRttResponse A source return trip time.
   * @return A miss response based on this message.
   * @throws IcpException if this message is not a query, or
   *                              if the query did not request a
   *                              source return trip time response.
   * @see #isQuery
   * @see #requestsSrcRtt
   */
  IcpMessage makeMissNoFetch(short srcRttResponse)
      throws IcpException;

  /**
   * <p>Constructs a UDP packet to the given address and port, from
   * this message.</p>
   * @param recipient The destination IP.
   * @param port      The destination port.
   * @return A UDP packet <code>p</code> representing the message,
   *         such that <code>p.getAddress() == recipient</code> and
   *         <code>p.getPort() == port</code>.
   */
  DatagramPacket toDatagramPacket(IPAddr recipient,
                                  int port);

  /**
   * <p>The hit object flag.</p>
   */
  static final int ICP_FLAG_HIT_OBJ = 0x80000000;

  /**
   * <p>The source return trip time flag.</p>
   */
  static final int ICP_FLAG_SRC_RTT = 0x40000000;

  /**
   * <p>The discovery echo opcode.</p>
   */
  static final byte ICP_OP_DECHO = 11;

  /**
   * <p>The denied opcode.</p>
   */
  static final byte ICP_OP_DENIED = 22;

  /**
   * <p>The error opcode.</p>
   */
  static final byte ICP_OP_ERR = 4;

  /**
   * <p>The hit opcode.</p>
   */
  static final byte ICP_OP_HIT = 2;

  /**
   * <p>The hit-object opcode.</p>
   */
  static final byte ICP_OP_HIT_OBJ = 23;

  /**
   * <p>The opcode whose value is zero.</p>
   * <p>The ICP specification stipulates that any opcode other than
   * the pre-defined opcodes is an invalid opcode, but specifies the
   * zero opcode as being ICP_OP_INVALID.</p>
   */
  static final byte ICP_OP_INVALID = 0;

  /**
   * <p>The miss opcode.</p>
   */
  static final byte ICP_OP_MISS = 3;

  /**
   * <p>The miss-no-fetch opcode.</p>
   */
  static final byte ICP_OP_MISS_NOFETCH = 21;

  /**
   * <p>The query opcode.</p>
   */
  static final byte ICP_OP_QUERY = 1;

  /**
   * <p>The source echo opcode.</p>
   */
  static final byte ICP_OP_SECHO = 10;

  /**
   * <p>The version of ICP reflected by this interface.</p>
   */
  static final byte ICP_VERSION = 2;

  /**
   * <p>The maximum length of an ICP packet.</p>
   */
  static final int MAX_LENGTH = 16384;

}
