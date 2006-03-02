/*
 * $Id: BaseIcpFactory.java,v 1.1 2006-01-31 01:29:19 thib_gc Exp $
 */

/*

Copyright (c) 2000-2006 Board of Trustees of Leland Stanford Jr. University,
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

import java.net.UnknownHostException;

import org.lockss.util.*;

/**
 * <p>A skeleton implementation of {@link IcpFactory}.</p>
 * @author Thib Guicherd-Callin
 */
public abstract class BaseIcpFactory implements IcpFactory {

  /*
   * begin PROTECTED STATIC NESTED CLASS
   * ===================================
   */
  /**
   * <p>A skeleton implementation of {@link IcpMessage}.</p>
   * <p>This class only provides field storage and convenience
   * methods.</p>
   * @author Thib Guicherd-Callin
   */
  protected static abstract class BaseIcpMessage
      implements IcpMessage {

    /**
     * <p>A length field.<p>
     */
    protected short length;

    /**
     * <p>An opcode field.</p>
     */
    protected byte opcode;

    /**
     * <p>An options data field.</p>
     */
    protected int optionData;

    /**
     * <p>An options field.</p>
     */
    protected int options;

    /**
     * <p>A payload object field.</p>
     */
    protected byte[] payloadObject;

    /**
     * <p>A payload length field, which may be different from
     * <code>payloadObject.length</code>.</p>
     */
    protected short payloadObjectLength;

    /**
     * <p>A payload URL field.</p>
     */
    protected String payloadUrl;

    /**
     * <p>A requester field.</p>
     */
    protected IPAddr requester;

    /**
     * <p>A request number field.</p>
     */
    protected int requestNumber;

    /**
     * <p>A sender field.</p>
     */
    protected IPAddr sender;

    /**
     * <p>A UDP sender field.</p>
     */
    protected IPAddr udpAddress;

    /**
     * <p>A UDP port field.</p>
     */
    protected int udpPort;

    /**
     * <p>A version field.</p>
     */
    protected byte version;

    /**
     * <p>Makes a new ICP message with the specified UDP address
     * and UDP port.</p>
     * @param udpAddress This message's UDP address.
     * @param udpPort    This message's UDP port.
     */
    protected BaseIcpMessage(IPAddr udpAddress,
                             int udpPort) {
      this.udpAddress = udpAddress;
      this.udpPort = udpPort;
    }

    /* Inherit documentation */
    public boolean containsSrcRttResponse() {
      return isResponse() && ((getOptions() & ICP_FLAG_SRC_RTT) != 0);
    }

    /* Inherit documentation */
    public short getLength() {
       return length;
     }

    /* Inherit documentation */
    public byte getOpcode() {
       return opcode;
     }

    /* Inherit documentation */
    public int getOptionData() {
       return optionData;
     }

    /* Inherit documentation */
    public int getOptions() {
       return options;
     }

    /* Inherit documentation */
    public byte[] getPayloadObject() {
       return (getOpcode() == ICP_OP_HIT_OBJ) ? payloadObject : null;
     }

    /* Inherit documentation */
    public short getPayloadObjectLength() {
       return (getOpcode() == ICP_OP_HIT_OBJ) ? payloadObjectLength : (short)0;
     }

    /* Inherit documentation */
    public String getPayloadUrl() {
       return payloadUrl;
     }

    /* Inherit documentation */
    public IPAddr getRequester() {
       return isQuery() ? requester : null;
     }

    /* Inherit documentation */
    public int getRequestNumber() {
       return requestNumber;
     }

    /* Inherit documentation */
    public IPAddr getSender() {
       return sender;
     }

    /* Inherit documentation */
    public short getSrcRttResponse() {
      if (containsSrcRttResponse()) {
        return (short)(getOptionData() & 0x0000ffff);
      }
      else {
        return (short)0;
      }
    }

    /* Inherit documentation */
    public IPAddr getUdpAddress() {
      return udpAddress;
    }

    /* Inherit documentation */
    public int getUdpPort() {
      return udpPort;
    }

    /* Inherit documentation */
    public byte getVersion() {
      return version;
    }

    /* Inherit documentation */
    public boolean isQuery() {
      return getOpcode() == ICP_OP_QUERY;
    }

    /* Inherit documentation */
    public boolean isResponse() {
      byte opcode = getOpcode();
      return    opcode == ICP_OP_HIT
             || opcode == ICP_OP_HIT_OBJ
             || opcode == ICP_OP_MISS
             || opcode == ICP_OP_MISS_NOFETCH;
    }

    /* Inherit documentation */
    public boolean requestsHitObj() {
      return isQuery() && (getOptions() & ICP_FLAG_HIT_OBJ) != 0;
    }

    /* Inherit documentation */
    public boolean requestsSrcRtt() {
      return isQuery() && (getOptions() & ICP_FLAG_SRC_RTT) != 0;
    }

    /**
     * <p>Builds a textual representation of this ICP message and its
     * contents.</p>
     * @return A String representing this ICP message.
     */
    public String toString() {
      StringBuffer buffer = new StringBuffer();
      buffer.append("[");
      buffer.append(getClass().getName());
      buffer.append(";opcode=");
      buffer.append(Integer.toHexString(getOpcode()));
      buffer.append(";version=");
      buffer.append(Integer.toHexString(getVersion()));
      buffer.append(";length=");
      buffer.append(Integer.toHexString(getLength()));
      buffer.append(";requestNumber=");
      buffer.append(Integer.toHexString(getRequestNumber()));
      buffer.append(";options=");
      buffer.append(Integer.toHexString(getOptions()));
      if (isQuery()) {
        buffer.append(";requestsSrcRtt=");
        buffer.append(Boolean.toString(requestsSrcRtt()));
        buffer.append(";requestsHitObj=");
        buffer.append(Boolean.toString(requestsHitObj()));
      }
      buffer.append(";optionData=");
      buffer.append(Integer.toHexString(getOptionData()));
      if (isResponse()) {
        buffer.append(";containsSrcRttResponse=");
        buffer.append(Boolean.toString(containsSrcRttResponse()));
        buffer.append(";srcRttResponse=");
        buffer.append(Integer.toHexString(getSrcRttResponse()));
      }
      buffer.append(";sender=");
      buffer.append(getSender());
      if (isQuery()) {
        buffer.append(";requester=");
        buffer.append(getRequester());
      }
      buffer.append(";payloadUrl=");
      buffer.append(getPayloadUrl());
      if (getPayloadObject() != null) {
        buffer.append(";payloadObject=");
        buffer.append(ByteArray.toHexString(getPayloadObject()));
      }
      buffer.append("]");
      return buffer.toString();
    }

    /**
     * <p>An error string for {@link IcpMessage#makeDenied()}.</p>
     */
    protected static final String DENIED_NOT_QUERY_ERROR =
      "ICP_OP_DENIED is a response to ICP_OP_QUERY";

    /**
     * <p>An error string for {@link IcpMessage#makeError()}.</p>
     */
    protected static final String ERR_NOT_QUERY_ERROR =
      "ICP_OP_ERR is a response to ICP_OP_QUERY";

    /**
     * <p>An error string for {@link IcpMessage#makeHit()} and
     * {@link IcpMessage#makeHit(short)}.</p>
     */
    protected static final String HIT_NOT_QUERY_ERROR =
      "ICP_OP_HIT is a response to ICP_OP_QUERY";

    /**
     * <p>An error string for {@link IcpMessage#makeHitObj(byte[])} and
     * {@link IcpMessage#makeHitObj(short, byte[])}.</p>
     */
    protected static final String HIT_OBJ_NOT_QUERY_ERROR =
      "ICP_OP_HIT_OBJ is a response to ICP_OP_QUERY";

    /**
     * <p>An error string for {@link IcpMessage#makeMissNoFetch()} and
     * {@link IcpMessage#makeMissNoFetch(short)}.</p>
     */
    protected static final String MISS_NOFETCH_NOT_QUERY_ERROR =
      "ICP_OP_MISS_NOFETCH is a response to ICP_OP_QUERY";

    /**
     * <p>An error string for {@link IcpMessage#makeHit()} and
     * {@link IcpMessage#makeHit(short)}.</p>
     */
    protected static final String MISS_NOT_QUERY_ERROR =
      "ICP_OP_MISS is a response to ICP_OP_QUERY";

    /**
     * <p>An error string for {@link IcpMessage#makeHitObj(byte[])} and
     * {@link IcpMessage#makeHitObj(short, byte[])}.</p>
     */
    protected static final String NULL_PAYLOAD_OBJECT_ERROR =
      "The payload object cannot be null";

    /**
     * <p>An error string for {@link IcpMessage#makeHit(short)},
     * {@link IcpMessage#makeHitObj(short, byte[])},
     * {@link IcpMessage#makeMiss(short)} and
     * {@link IcpMessage#makeMissNoFetch(short)}.</p>
     */
    protected static final String SRC_RTT_NOT_REQUESTED_ERROR =
      "Query does not request source return trip time";

  }
  /*
   * end PROTECTED STATIC NESTED CLASS
   * =================================
   */

  /* Inherit documentation */
  public IcpMessage makeQuery(IPAddr requesterAddress, String query) {
    return makeQuery(requesterAddress, query, false, false);
  }

  /**
   * <p>The 0.0.0.0 IP address.</p>
   */
  protected static final IPAddr ZERO_ADDRESS;

  /**
   * <p>An opaque counter to use for new request numbers.</p>
   */
  private static int requestNumberCounter = 1;

  /*
   * begin STATIC INITIALIZER
   * ========================
   */
  static {
    try {
      byte[] zero = new byte[] {0, 0, 0, 0};
      ZERO_ADDRESS = IPAddr.getByAddress(zero);
    }
    catch (UnknownHostException e) {
      throw new RuntimeException("Could not create the zero address constant");
    }
  }
  /*
   * end STATIC INITIALIZER
   * ======================
   */

  /**
   * <p>Obtains a fresh request number.</p>
   * @return An opaque request number.
   */
  protected static int getNewRequestNumber() {
    return requestNumberCounter++;
  }

}
