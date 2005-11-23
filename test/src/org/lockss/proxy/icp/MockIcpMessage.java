/*
 * $Id: MockIcpMessage.java,v 1.7 2005-11-23 21:12:36 thib_gc Exp $
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

import java.io.UnsupportedEncodingException;
import java.net.*;
import java.util.ArrayList;

import org.lockss.util.*;

/**
 * <p>Provides general facilities to generate lightweight mock ICP
 * messages and their binary array counterparts.</p>
 * @author Thib Guicherd-Callin
 */
public abstract class MockIcpMessage implements IcpMessage {

  /*
   * begin STATIC NESTED CLASS
   * =========================
   */
  /**
   * <p>A mock ICP query without a hit object.</p>
   * @author Thib Guicherd-Callin
   */
  private static class MockIcpQueryMessage extends MockIcpMessage {
    public short getLength() { return (short)(4 + super.getLength()); }
    public byte getOpcode() { return ICP_OP_QUERY; }
    public IPAddr getRequester() { return getStandardRequester(); }
    public boolean isQuery() { return true; }
  }
  /*
   * end STATIC NESTED CLASS
   * =======================
   */

  /*
   * begin STATIC NESTED CLASS
   * =========================
   */
  /**
   * <p>A mock ICP response message with a source return time trip
   * reponse.</p>
   * @author Thib Guicherd-Callin
   */
  private static abstract class MockIcpResponseMessage extends MockIcpMessage {
    public boolean containsSrcRttResponse() { return true; }
    public int getOptionData() { return getStandardSrcRttResponse(); }
    public int getOptions() { return ICP_FLAG_SRC_RTT; }
    public short getSrcRttResponse() { return getStandardSrcRttResponse(); }
    public boolean isResponse() { return true; }
  }
  /*
   * end STATIC NESTED CLASS
   * =======================
   */

  /*
   * begin STATIC NESTED CLASS
   * =========================
   */
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
  /*
   * end STATIC NESTED CLASS
   * =======================
   */

  /* Inherit documentation */
  public boolean containsSrcRttResponse() {
    return false;
  }

  /* Inherit documentation */
  public short getLength() {
    try {
      return (short)(  20  // for the header
                     + getStandardQueryUrl().getBytes(Constants.URL_ENCODING).length
                     + 1); // for the null terminator
    }
    catch (UnsupportedEncodingException uee) {
      throw new RuntimeException("Runtime exception while creating mock URL "
          + getStandardQueryUrl().toString());
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
    byte[] payload = getPayloadObject();
    if (getOpcode() == ICP_OP_HIT_OBJ && payload != null) {
      return (short)payload.length;
    }
    else {
      return (short)0;
    }
  }

  /* Inherit documentation */
  public String getPayloadUrl() {
    return getStandardQueryUrl();
  }

  /* Inherit documentation */
  public IPAddr getRequester() {
    return null;
  }

  /* Inherit documentation */
  public int getRequestNumber() {
    return 0xdeadbeef;
  }

  /* Inherit documentation */
  public IPAddr getSender() {
    return getStandardSender();
  }

  /* Inherit documentation */
  public short getSrcRttResponse() {
    return 0;
  }

  /* Inherit documentation */
  public IPAddr getUdpAddress() {
    return getStandardDestination();
  }

  /* Inherit documentation */
  public int getUdpPort() {
    return getStandardUdpPort();
  }

  /* Inherit documentation */
  public boolean isQuery() {
    return false;
  }

  /* Inherit documentation */
  public boolean isResponse() {
    return false;
  }

  /* Inherit documentation */
  public byte getVersion() {
    return (byte)2; // version 2
  }

  /* Inherit documentation */
  public boolean requestsHitObj() {
    return false;
  }

  /* Inherit documentation */
  public boolean requestsSrcRtt() {
    return false;
  }

  /* Inherit documentation */
  public void setUdpAddress(IPAddr udpAddress) {
    // nothing
  }

  /* Inherit documentation */
  public void setUdpPort(int port) {
    // nothing
  }

  /**
   * <p>The usual destination address.</p>
   */
  private static IPAddr standardDestination;

  /**
   * <p>The usual payload bytes.</p>
   */
  private static final byte[] standardPayloadData;

  /**
   * <p>The usual query URL string.</p>
   */
  private static String standardQueryUrl;

  /**
   * <p>The usual requester address.</p>
   */
  private static IPAddr standardRequester;

  /**
   * <p>The usual sender address.</p>
   */
  private static IPAddr standardSender;

  /**
   * <p>The usual UDP port.</p>
   */
  private static final int standardUdpPort = 65432;

  /**
   * <p>A list of predefined test pairs.</p>
   */
  private static ArrayList testPairs;

  /*
   * begin STATIC INITIALIZER
   * ========================
   */
  static {
    /* Initialize the usual values */
    try {
      standardSender = makeAddress(1, 2, 3, 4);
      standardRequester = makeAddress(11, 12, 13, 14);
      standardDestination = makeAddress(111, 112, 113, 114);
      standardQueryUrl = "http://www.stanford.edu/";
      standardPayloadData =
        "<html><head><title>Sample</title></head><body><p>Test</p></body></html>"
        .getBytes(Constants.US_ASCII_ENCODING);
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
          query(),
          queryRequestSrcRtt(),
          queryRequestHitObj(),
          queryRequestSrcRttRequestHitObj(),
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
            public byte[] getPayloadObject() { return getStandardPayloadData(); }
            public short getPayloadObjectLength() { return (short)getPayloadObject().length; }
            public boolean isResponse() { return true; }
          },
          new MockIcpResponseMessage() {
            public short getLength() { return (short)(2 + getPayloadObjectLength() + super.getLength()); }
            public byte getOpcode() { return ICP_OP_HIT_OBJ; }
            public byte[] getPayloadObject() { return getStandardPayloadData(); }
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
                                    getStandardDestination().getInetAddr(),
                                    getStandardUdpPort());
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
   * <p>Gets the usual destination address.</p>
   * @return A standard address used as the destination.
   */
  public static IPAddr getStandardDestination() {
    return standardDestination;
  }

  /**
   * <p>Gets the usual payload bytes.</p>
   * @return A standard array of bytes used as payload data.
   */
  public static byte[] getStandardPayloadData() {
    return standardPayloadData;
  }

  /**
   * <p>Gets the usual query URL string.</p>
   * @return A standard URL string used as a payload URL.
   */
  public static String getStandardQueryUrl() {
    return standardQueryUrl;
  }

  /**
   * <p>Gets the usual requester address.</p>
   * @return A standard address used as a requester address.
   */
  public static IPAddr getStandardRequester() {
    return standardRequester;
  }

  /**
   * <p>Gets the usual sender address.</p>
   * @return A standard address used as a sender address.
   */
  public static IPAddr getStandardSender() {
    return standardSender;
  }

  /**
   * <p>Get the usual source return trip time response.</p>
   * @return A standard value used as a source return trip time
   * response.
   */
  public static short getStandardSrcRttResponse() {
    return (short)12345;
  }

  /**
   * <p>Gets the usual UDP port.</p>
   * @return A standard value used as the UDP port number.
   */
  public static int getStandardUdpPort() {
    return standardUdpPort;
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
   * <p>builds an invalid ICP message.</p>
   * @return A new ICP message.
   */
  public static IcpMessage invalid() {
    return new MockIcpMessage() {
      public byte getOpcode() { return ICP_OP_INVALID; }
    };
  }

  /**
   * <p>Builds a query message.</p>
   * @return A new ICP message.
   * @see MockIcpQueryMessage
   */
  public static IcpMessage query() {
    return new MockIcpQueryMessage();
  }

  /**
   * <p>Builds a query message requesting a hit object.</p>
   * @return A new ICP MEssage.
   */
  public static IcpMessage queryRequestHitObj() {
    return new MockIcpQueryMessage() {
      public int getOptions() { return ICP_FLAG_HIT_OBJ; }
      public boolean requestsHitObj() { return true; }
    };
  }

  /**
   * <p>Builds a query message requesting a source return trip
   * time.</p>
   * @return A new ICP message.
   */
  public static IcpMessage queryRequestSrcRtt() {
    return new MockIcpQueryMessage() {
      public int getOptions() { return ICP_FLAG_SRC_RTT; }
      public boolean requestsSrcRtt() { return true; }
    };
  }

  /**
   * <p>Builds a query message requesting both a hit object and a
   * source return trip time.</p>
   * @return A new ICP message.
   */
  public static IcpMessage queryRequestSrcRttRequestHitObj() {
    return new MockIcpQueryMessage() {
      public int getOptions() { return ICP_FLAG_HIT_OBJ | ICP_FLAG_SRC_RTT; }
      public boolean requestsHitObj() { return true; }
      public boolean requestsSrcRtt() { return true; }
    };
  }

  /**
   * <p>Makes an IP address from four integers.</p>
   * @param i1 The first byte of the address.
   * @param i2 The second byte of the address.
   * @param i3 The third byte of the address.
   * @param i4 The fourth byte of the address.
   * @return The resulting address.
   */
  private static IPAddr makeAddress(int i1, int i2, int i3, int i4) {
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
   * @throws Exception if an unexpected problem arises.
   */
  private static byte[] makePacketBytes(IcpMessage message)
      throws Exception {
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

}
