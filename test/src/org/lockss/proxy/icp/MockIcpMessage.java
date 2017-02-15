/*
 * $Id$
 */

/*

Copyright (c) 2000-2012 Board of Trustees of Leland Stanford Jr. University,
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

import java.io.UnsupportedEncodingException;
import java.net.*;
import java.util.ArrayList;

import org.lockss.util.*;

/**
 * <p>A lightweight implementation of {@link IcpMessage} used in
 * testing.</p>
 * @author Thib Guicherd-Callin
 */
public abstract class MockIcpMessage implements IcpMessage {

  /**
   * <p>A mock ICP query without a hit object.</p>
   * @author Thib Guicherd-Callin
   */
  private static class MockIcpQueryMessage extends MockIcpMessage {

    /* Inherit documentation */
    public short getLength() {
      return (short)(4 + super.getLength());
    }

    /* Inherit documentation */
    public byte getOpcode() {
      return ICP_OP_QUERY;
    }

    /* Inherit documentation */
    public IPAddr getRequester() {
      return getMockRequester();
    }

    /* Inherit documentation */
    public boolean isQuery() {
      return true;
    }
  }

  /**
   * <p>A mock ICP response message with a source return time trip
   * reponse.</p>
   * @author Thib Guicherd-Callin
   */
  private static abstract class MockIcpResponseMessage extends MockIcpMessage {

    /* Inherit documentation */
    public boolean containsSrcRttResponse() {
      return true;
    }

    /* Inherit documentation */
    public int getOptionData() {
      return getMockSrcRttResponse();
    }

    /* Inherit documentation */
    public int getOptions() {
      return ICP_FLAG_SRC_RTT;
    }

    /* Inherit documentation */
    public short getSrcRttResponse() {
      return getMockSrcRttResponse();
    }

    /* Inherit documentation */
    public boolean isResponse() {
      return true;
    }

  }

  /**
   * <p>A simple struct associating an ICP message and an equivalent
   * UDP packet representation.</p>
   */
  private static class TestPair {

    private IcpMessage message;

    private DatagramPacket packet;

    public TestPair(IcpMessage message, DatagramPacket packet) {
      this.message = message;
      this.packet = packet;
    }

  }

  /* Inherit documentation */
  public boolean containsSrcRttResponse() {
    return false;
  }

  /* Inherit documentation */
  public short getLength() {
    try {
      return (short)(  20  // for the header
                     + getMockQueryUrl().getBytes(Constants.URL_ENCODING).length
                     + 1); // for the null terminator
    }
    catch (UnsupportedEncodingException uee) {
      throw new RuntimeException("Runtime exception while creating mock URL "
          + getMockQueryUrl().toString());
    }
  }

  /* Inherit documentation */
  public int getOptionData() {
    return 0;
  }

  /* Inherit documentation */
  public int getOptions() {
    return 0;
  }

  /* Inherit documentation */
  public byte[] getPayloadObject() {
    return null;
  }

  /* Inherit documentation */
  public short getPayloadObjectLength() {
    return 0;
  }

  /* Inherit documentation */
  public String getPayloadUrl() {
    return getMockQueryUrl();
  }

  /* Inherit documentation */
  public IPAddr getRequester() {
    return null;
  }

  /* Inherit documentation */
  public int getRequestNumber() {
    return getMockRequestNumber();
  }

  /* Inherit documentation */
  public IPAddr getSender() {
    return getMockSender();
  }

  /* Inherit documentation */
  public short getSrcRttResponse() {
    return 0;
  }

  /* Inherit documentation */
  public IPAddr getUdpAddress() {
    return getMockUdpAddress();
  }

  /* Inherit documentation */
  public int getUdpPort() {
    return getMockUdpPort();
  }

  /* Inherit documentation */
  public byte getVersion() {
    return getMockVersion();
  }

  /* Inherit documentation */
  public boolean isQuery() {
    return false;
  }

  /* Inherit documentation */
  public boolean isResponse() {
    return false;
  }

  /**
   * <p>Throws an {@link UnsupportedOperationException}.</p>
   */
  public IcpMessage makeDenied() throws IcpException {
    throw unsupported();
  }

  /**
   * <p>Throws an {@link UnsupportedOperationException}.</p>
   */
  public IcpMessage makeError() throws IcpException {
    throw unsupported();
  }

  /**
   * <p>Throws an {@link UnsupportedOperationException}.</p>
   */
  public IcpMessage makeHit() throws IcpException {
    throw unsupported();
  }

  /**
   * <p>Throws an {@link UnsupportedOperationException}.</p>
   */
  public IcpMessage makeHit(short srcRttResponse) throws IcpException {
    throw unsupported();
  }

  /**
   * <p>Throws an {@link UnsupportedOperationException}.</p>
   */
  public IcpMessage makeHitObj(byte[] payloadObject) throws IcpException {
    throw unsupported();
  }

  /**
   * <p>Throws an {@link UnsupportedOperationException}.</p>
   */
  public IcpMessage makeHitObj(short srcRttResponse, byte[] payloadObject) throws IcpException {
    throw unsupported();
  }

  /**
   * <p>Throws an {@link UnsupportedOperationException}.</p>
   */
  public IcpMessage makeMiss() throws IcpException {
    throw unsupported();
  }

  /**
   * <p>Throws an {@link UnsupportedOperationException}.</p>
   */
  public IcpMessage makeMiss(short srcRttResponse) throws IcpException {
    throw unsupported();
  }

  /**
   * <p>Throws an {@link UnsupportedOperationException}.</p>
   */
  public IcpMessage makeMissNoFetch() throws IcpException {
    throw unsupported();
  }

  /**
   * <p>Throws an {@link UnsupportedOperationException}.</p>
   */
  public IcpMessage makeMissNoFetch(short srcRttResponse) throws IcpException {
    throw unsupported();
  }

  /* Inherit documentation */
  public boolean requestsHitObj() {
    return false;
  }

  /* Inherit documentation */
  public boolean requestsSrcRtt() {
    return false;
  }

  /**
   * <p>Throws an {@link UnsupportedOperationException}.</p>
   */
  public DatagramPacket toDatagramPacket(IPAddr recipient, int port) {
    throw unsupported();
  }

  /**
   * <p>A collection of test pairs.</p>
   */
  private static ArrayList testPairs;

  /*
  * begin STATIC INITIALIZER
  * ========================
  */
  static {
   /* Initialize the predefined test pairs */
   try {
     testPairs = new ArrayList();
     DatagramPacket packet;

     IcpMessage[] messages = new IcpMessage[] {
         new MockIcpQueryMessage(),
         new MockIcpQueryMessage() {
           public int getOptions() { return ICP_FLAG_HIT_OBJ; }
           public boolean requestsHitObj() { return true; }
         },
         new MockIcpQueryMessage() {
           public int getOptions() { return ICP_FLAG_SRC_RTT; }
           public boolean requestsSrcRtt() { return true; }
         },
         new MockIcpQueryMessage() {
           public int getOptions() { return ICP_FLAG_HIT_OBJ | ICP_FLAG_SRC_RTT; }
           public boolean requestsHitObj() { return true; }
           public boolean requestsSrcRtt() { return true; }
         },
         new MockIcpMessage() {
           public byte getOpcode() { return ICP_OP_HIT; }
           public boolean isResponse() { return true; }
         },
         new MockIcpResponseMessage() {
           public byte getOpcode() { return ICP_OP_HIT; }
         },
         new MockIcpMessage() {
           public byte getOpcode() { return ICP_OP_MISS; }
           public boolean isResponse() { return true; }
         },
         new MockIcpResponseMessage() {
           public byte getOpcode() { return ICP_OP_MISS; }
         },
         new MockIcpMessage() {
           public byte getOpcode() { return ICP_OP_MISS_NOFETCH; }
           public boolean isResponse() { return true; }
         },
         new MockIcpResponseMessage() {
           public byte getOpcode() { return ICP_OP_MISS_NOFETCH; }
         },
         new MockIcpMessage() {
           public short getLength() { return (short)(2 + getPayloadObjectLength() + super.getLength()); }
           public byte getOpcode() { return ICP_OP_HIT_OBJ; }
           public byte[] getPayloadObject() { return getMockPayloadData(); }
           public short getPayloadObjectLength() { return (short)getPayloadObject().length; }
           public boolean isResponse() { return true; }
         },
         new MockIcpResponseMessage() {
           public short getLength() { return (short)(2 + getPayloadObjectLength() + super.getLength()); }
           public byte getOpcode() { return ICP_OP_HIT_OBJ; }
           public byte[] getPayloadObject() { return getMockPayloadData(); }
           public short getPayloadObjectLength() { return (short)getPayloadObject().length; }
         },
         new MockIcpMessage() {
           public byte getOpcode() { return ICP_OP_ERR; }
         },
         new MockIcpMessage() {
           public byte getOpcode() { return ICP_OP_DENIED; }
         }
     };

     for (int ii = 0 ; ii < messages.length ; ii++) {
       packet = new DatagramPacket(makePacketBytes(messages[ii]),
                                   messages[ii].getLength(),
                                   getMockUdpAddress().getInetAddr(),
                                   getMockUdpPort());
       testPairs.add(new TestPair(messages[ii], packet));
     }

   }
   catch (Exception exc) {
     throw new RuntimeException("Runtime exception while initializing "
         + MockIcpMessage.class.getName(), exc);
   }
  }
  /*
  * end STATIC INITIALIZER
  * ======================
  */

  /**
   * <p>Returns the number of predefined test pairs.</p>
   * @return The number of predefined test pairs.
   * @see #getTestMessage
   * @see #getTestPacket
   */
  public static int countTestPairs() {
    return testPairs.size();
  }

  /**
   * <p>Gets a standard destination address.</p>
   * @return A destination IP.
   */
  public static IPAddr getMockDestination() {
    return makeAddress(44, 33, 22, 11);
  }

  /**
   * <p>Gets a standard array of payload data bytes.</p>
   * @return An array of bytes.
   */
  public static byte[] getMockPayloadData() {
    try {
      return "<html><head><title>Sample</title></head><body><p>Test</p></body></html>"
             .getBytes(Constants.ENCODING_US_ASCII);
    } catch (UnsupportedEncodingException exc) {
      throw new RuntimeException(
          "Runtime exception while creating mock payload data");
    }
  }

  /**
   * <p>Gets a standard query URL.</p>
   * @return A URL string.
   */
  public static String getMockQueryUrl() {
    return "http://www.lockss.org/";
  }

  /**
   * <p>Gets a standard requester address.</p>
   * @return A requester IP.
   */
  public static IPAddr getMockRequester() {
    return makeAddress(55, 66, 77, 88);
  }

  /**
   * <p>Gets a standard request number.</p>
   * @return A request number.
   */
  public static int getMockRequestNumber() {
    return 54321;
  }

  /**
   * <p>Gets a standard sender address.</p>
   * @return A sender IP.
   */
  public static IPAddr getMockSender() {
    return makeAddress(98, 87, 76, 65);
  }

  /**
   * <p>Gets a standard source return trip time.</p>
   * @return A source return trip time.
   */
  public static short getMockSrcRttResponse() {
    return (short)333;
  }

  /**
   * <p>Gets a standard ICP UDP address,</p>
   * @return An IP.
   */
  public static IPAddr getMockUdpAddress() {
    return makeAddress(11, 22, 33, 44);
  }

  /**
   * <p>Gets a standard ICP UDP port.</p>
   * @return A port number.
   */
  public static int getMockUdpPort() {
    return 65000;
  }

  /**
   * <p>Gets a standard ICP version number.</p>
   * @return A version number.
   */
  public static byte getMockVersion() {
    return ICP_VERSION;
  }

  /**
   * <p>Makes a sample datagram packet corresponding to an ICP
   * query.</p>
   * @return A DatagramPacket representing an ICP query.
   */
  public static DatagramPacket getSampleQueryUdpPacket() {
    try {
      MockIcpQueryMessage query = new MockIcpQueryMessage();
      return new DatagramPacket(makePacketBytes(query),
                                query.getLength(),
                                getMockUdpAddress().getInetAddr(),
                                getMockUdpPort());
    } catch (Exception exc) {
      throw new RuntimeException("Could not create mock UDP packet");
    }
  }

  /**
   * <p>Retrieves the <code>n</code>-th predefined ICP message.</p>
   * @param nth The index of the predefined message, between zero and
   *            ({@link #countTestPairs}()-1).
   * @return The predefined ICP message indexed by <code>nth</code>.
   */
  public static IcpMessage getTestMessage(int nth) {
    return ((TestPair)testPairs.get(nth)).message;
  }

  /**
   * <p>Retrieves the <code>n</code>-th predefined UDP packet.</p>
   * @param nth The index of the predefined packet, between zero and
   *            ({@link #countTestPairs}()-1).
   * @return The predefined UDP packet indexed by <code>nth</code>.
   */
  public static DatagramPacket getTestPacket(int nth) {
    return ((TestPair)testPairs.get(nth)).packet;
  }

  /**
    * <p>Makes an IP address from four integers.</p>
    * @param i1 The first byte of the address.
    * @param i2 The second byte of the address.
    * @param i3 The third byte of the address.
    * @param i4 The fourth byte of the address.
    * @return The resulting address.
    */
  public static IPAddr makeAddress(int i1, int i2, int i3, int i4) {
    int [] arr = new int[] { i1, i2, i3, i4 };
    try {
      return IPAddr.getByAddress(arr);
    }
    catch (UnknownHostException uhe) {
      StringBuffer buffer = new StringBuffer();
      buffer.append("Could not create mock IP address [");
      buffer.append(StringUtil.separatedString(arr, "."));
      buffer.append("].");
      throw new RuntimeException(buffer.toString(), uhe);
    }
  }

  /**
   * <p>Packs an ICP message into an equivalent array of bytes.</p>
   * @param message An original ICP message.
   * @return An array of bytes representing the message.
   */
  private static byte[] makePacketBytes(IcpMessage message) throws Exception {
    byte[] ret = new byte[message.getLength()];
    int ii = 0;

    // Basic payload
    ret[ii++] = message.getOpcode();
    ret[ii++] = message.getVersion();
    for (int jj = 8 ; jj >= 0 ; jj -= 8) {
      ret[ii++] = (byte)(message.getLength() >>> jj);
    }
    for (int jj = 24 ; jj >= 0 ; jj -= 8) {
      ret[ii++] = (byte)(message.getRequestNumber() >>> jj);
    }
    for (int jj = 24 ; jj >= 0 ; jj -= 8) {
      ret[ii++] = (byte)(message.getOptions() >>> jj);
    }
    for (int jj = 24 ; jj >= 0 ; jj -= 8) {
      ret[ii++] = (byte)(message.getOptionData() >>> jj);
    }
    for (int jj = 0 ; jj < 4 ; jj++) {
      ret[ii++] = message.getSender().getAddress()[jj];
    }
    if (message.getOpcode() == ICP_OP_QUERY) {
      for (int jj = 0 ; jj < 4 ; jj++) {
        ret[ii++] = message.getRequester().getAddress()[jj];
      }
    }

    // Payload URL
    byte[] urlBytes = message.getPayloadUrl().getBytes(Constants.URL_ENCODING);
    System.arraycopy(urlBytes, 0, ret, ii, urlBytes.length);
    ii += urlBytes.length;
    ret[ii++] = (byte)0;

    // Payload object
    if (message.getOpcode() == ICP_OP_HIT_OBJ) {
      for (int jj = 8 ; jj >= 0 ; jj -= 8) {
        ret[ii++] = (byte)(message.getPayloadObjectLength() >>> jj);
      }
      System.arraycopy(message.getPayloadObject(), 0, ret,
          ii, message.getPayloadObjectLength());
      ii += message.getPayloadObjectLength();
    }

    return ret;
  }

  /**
   * <p>Makes a new {@link UnsupportedOperationException}.</p>
   * @return An {@link UnsupportedOperationException}.
   */
  private static UnsupportedOperationException unsupported() {
    return new UnsupportedOperationException("Unimplemented");
  }

}
