/*
 * $Id: IcpFactoryImpl.java,v 1.14 2005-11-23 21:12:36 thib_gc Exp $
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

import java.io.*;
import java.net.*;
import java.nio.ByteBuffer;

import org.lockss.util.*;

/**
 * <p>An implementation of the {@link IcpFactory} interface, which
 * defines its own ICP encoder, decoder and builder, as well as an
 * implementation of the {@link IcpMessage} interface.</p>
 * @author Thib Guicherd-Callin
 */
public class IcpFactoryImpl implements IcpFactory {

  /*
   * begin STATIC NESTED CLASS
   * =========================
   */
  /**
   * <p>Implements an ICP builder.</p>
   * @author Thib Guicherd-Callin
   */
  private static class IcpBuilderImpl implements IcpBuilder {

    /* Inherit documentation */
    public IcpMessage makeDenied(IcpMessage query)
        throws IcpProtocolException {
      if (!query.isQuery()) {
        throw new IcpProtocolException(
            "ICP_OP_DENIED is a response to ICP_OP_QUERY");
      }
      return new IcpMessageImpl(IcpMessage.ICP_OP_DENIED,
                                IcpMessage.ICP_VERSION,
                                query.getLength(),
                                query.getRequestNumber(),
                                0,
                                0,
                                ZERO_ADDRESS,
                                query.getPayloadUrl());
    }

    /* Inherit documentation */
    public IcpMessage makeDiscoveryEcho(String query) {
      return null; // Unimplemented
    }

    /* Inherit documentation */
    public IcpMessage makeError(IcpMessage query)
        throws IcpProtocolException {
      if (!query.isQuery()) {
        throw new IcpProtocolException(
            "ICP_OP_ERR is a response to ICP_OP_QUERY");
      }
      return new IcpMessageImpl(IcpMessage.ICP_OP_ERR,
                                IcpMessage.ICP_VERSION,
                                query.getLength(),
                                query.getRequestNumber(),
                                0,
                                0,
                                ZERO_ADDRESS,
                                query.getPayloadUrl());
    }

    /* Inherit documentation */
    public IcpMessage makeHit(IcpMessage query)
        throws IcpProtocolException {
      if (!query.isQuery()) {
        throw new IcpProtocolException(
            "ICP_OP_HIT is a response to ICP_OP_QUERY");
      }
      return new IcpMessageImpl(IcpMessage.ICP_OP_HIT,
                                IcpMessage.ICP_VERSION,
                                (short)(query.getLength() - 4),
                                query.getRequestNumber(),
                                0,
                                0,
                                ZERO_ADDRESS,
                                query.getPayloadUrl());
    }

    /* Inherit documentation */
    public IcpMessage makeHit(IcpMessage query,
                              short srcRttResponse)
        throws IcpProtocolException {
      if (!query.isQuery()) {
        throw new IcpProtocolException(
            "ICP_OP_HIT is a response to ICP_OP_QUERY");
      }
      if (!query.requestsSrcRtt()) {
        throw new IcpProtocolException(
            "Query does not request source return trip time");
      }
      return new IcpMessageImpl(IcpMessage.ICP_OP_HIT,
                                IcpMessage.ICP_VERSION,
                                (short)(query.getLength() - 4),
                                query.getRequestNumber(),
                                IcpMessage.ICP_FLAG_SRC_RTT,
                                (int)srcRttResponse,
                                ZERO_ADDRESS,
                                query.getPayloadUrl());
    }

    /* Inherit documentation */
    public IcpMessage makeHitObj(IcpMessage query,
                                 byte[] payloadObject)
        throws IcpProtocolException {
      if (payloadObject == null) {
        throw new NullPointerException(
            "The payload object cannot be null");
      }
      if (!query.isQuery()) {
        throw new IcpProtocolException(
            "ICP_OP_HIT_OBJ is a response to ICP_OP_QUERY");
      }
      return new IcpMessageImpl(
          IcpMessage.ICP_OP_HIT_OBJ,
          IcpMessage.ICP_VERSION,
          (short)(query.getLength() - 2 + payloadObject.length),
          query.getRequestNumber(),
          IcpMessage.ICP_FLAG_HIT_OBJ,
          0,
          ZERO_ADDRESS,
          query.getPayloadUrl(),
          payloadObject,
          (short)payloadObject.length
      );
    }

    /* Inherit documentation */
    public IcpMessage makeHitObj(IcpMessage query,
                                 short srcRttResponse,
                                 byte[] payloadObject)
        throws IcpProtocolException {
      if (payloadObject == null) {
        throw new NullPointerException(
            "The payload object cannot be null");
      }
      if (!query.isQuery()) {
        throw new IcpProtocolException("ICP_OP_HIT_OBJ is a response to ICP_OP_QUERY");
      }
      if (!query.requestsSrcRtt()) {
        throw new IcpProtocolException("Query does not request source return trip time");
      }
      return new IcpMessageImpl(
          IcpMessage.ICP_OP_HIT_OBJ,
          IcpMessage.ICP_VERSION,
          (short)(query.getLength() - 2 + payloadObject.length),
          query.getRequestNumber(),
          IcpMessage.ICP_FLAG_HIT_OBJ | IcpMessage.ICP_FLAG_SRC_RTT,
          (int)srcRttResponse, ZERO_ADDRESS,
          query.getPayloadUrl(),
          payloadObject,
          (short)payloadObject.length
      );
    }

    /* Inherit documentation */
    public IcpMessage makeMiss(IcpMessage query)
        throws IcpProtocolException {
      if (!query.isQuery()) {
        throw new IcpProtocolException(
            "ICP_OP_MISS is a response to ICP_OP_QUERY");
      }
      return new IcpMessageImpl(IcpMessage.ICP_OP_MISS,
                                IcpMessage.ICP_VERSION,
                                (short)(query.getLength() - 4),
                                query.getRequestNumber(),
                                0,
                                0,
                                ZERO_ADDRESS,
                                query.getPayloadUrl());
    }

    /* Inherit documentation */
    public IcpMessage makeMiss(IcpMessage query,
                               short srcRttResponse)
        throws IcpProtocolException {
      if (!query.isQuery()) {
        throw new IcpProtocolException(
            "ICP_OP_MISS is a response to ICP_OP_QUERY");
      }
      if (!query.requestsSrcRtt()) {
        throw new IcpProtocolException(
            "Query does not request source return trip time");
      }
      return new IcpMessageImpl(IcpMessage.ICP_OP_MISS,
                                IcpMessage.ICP_VERSION,
                                (short)(query.getLength() - 4),
                                query.getRequestNumber(),
                                IcpMessage.ICP_FLAG_SRC_RTT,
                                (int)srcRttResponse,
                                ZERO_ADDRESS,
                                query.getPayloadUrl());
    }

    /* Inherit documentation */
    public IcpMessage makeMissNoFetch(IcpMessage query)
        throws IcpProtocolException {
      if (!query.isQuery()) {
        throw new IcpProtocolException(
            "ICP_OP_MISS_NOFETCH is a response to ICP_OP_QUERY");
      }
      return new IcpMessageImpl(IcpMessage.ICP_OP_MISS_NOFETCH,
                                IcpMessage.ICP_VERSION,
                                (short)(query.getLength() - 4),
                                query.getRequestNumber(),
                                0,
                                0,
                                ZERO_ADDRESS,
                                query.getPayloadUrl());
    }

    /* Inherit documentation */
    public IcpMessage makeMissNoFetch(IcpMessage query,
                                      short srcRttResponse)
        throws IcpProtocolException {
      if (!query.isQuery()) {
        throw new IcpProtocolException(
            "ICP_OP_MISS_NOFETCH is a response to ICP_OP_QUERY");
      }
      if (!query.requestsSrcRtt()) {
        throw new IcpProtocolException(
            "Query does not request source return trip time");
      }
      return new IcpMessageImpl(IcpMessage.ICP_OP_MISS_NOFETCH,
                                IcpMessage.ICP_VERSION,
                                (short)(query.getLength() - 4),
                                query.getRequestNumber(),
                                IcpMessage.ICP_FLAG_SRC_RTT,
                                srcRttResponse,
                                ZERO_ADDRESS,
                                query.getPayloadUrl());
    }

    /* Inherit documentation */
    public IcpMessage makeQuery(IPAddr requesterAddress,
                                String query) {
      return makeQuery(requesterAddress, query, false, false);
    }

    /* Inherit documentation */
    public IcpMessage makeQuery(IPAddr requesterAddress,
                                String query,
                                boolean requestSrcRtt,
                                boolean requestHitObj) {
      try {
        return new IcpMessageImpl(
            IcpMessage.ICP_OP_QUERY,
            IcpMessage.ICP_VERSION,
            (short)(21 + query.getBytes(Constants.URL_ENCODING).length),
            getNewRequestNumber(),
              (requestSrcRtt ? IcpMessage.ICP_FLAG_SRC_RTT : 0)
            | (requestHitObj ? IcpMessage.ICP_FLAG_HIT_OBJ : 0),
            0,
            ZERO_ADDRESS,
            requesterAddress,
            query);
      }
      catch (UnsupportedEncodingException uee) {
        throw new RuntimeException(
            "Could not create byte array from String", uee);
      }
    }

    /* Inherit documentation */
    public IcpMessage makeSourceEcho(String query) {
      return null; // Unimplemented
    }

    /**
     * <p>An opaque counter to use for new request numbers.</p>
     */
    private static int requestNumberCounter = 1;

    /**
     * <p>The 0.0.0.0 IP address.</p>
     */
    private static final IPAddr ZERO_ADDRESS;

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

    /* Inherit documentation */
    private static int getNewRequestNumber() {
      return requestNumberCounter++;
    }

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
   * <p>Implements an ICP decoder.</p>
   * @author Thib Guicherd-Callin
   */
  private static class IcpDecoderImpl implements IcpDecoder {

    public IcpMessage parseIcp(DatagramPacket packet)
        throws IcpProtocolException {

      // Local variables
      byte opcode;
      byte version;
      short length;
      int requestNumber;
      int options;
      int optionData;
      IPAddr sender = null;
      IPAddr requester = null;
      String payloadUrl = null;
      short payloadLength;
      byte[] payloadObject = null;
      IcpMessage ret = null;
      byte[] packetData = packet.getData();
      ByteBuffer in = ByteBuffer.wrap(packetData);

      try {
        // Unconditional processing
        opcode = IcpUtil.getOpcodeFromBuffer(in);
        if (!IcpUtil.isValidOpcode(opcode)) {
          throw new IcpProtocolException("Invalid opcode: " + opcode);
        }
        version = IcpUtil.getVersionFromBuffer(in);
        if (version != IcpMessage.ICP_VERSION) {
          throw new IcpProtocolException("Unknown version: " + version);
        }
        length = IcpUtil.getLengthFromBuffer(in);
        requestNumber = IcpUtil.getRequestNumberFromBuffer(in);
        options = IcpUtil.getOptionsFromBuffer(in);
        optionData = IcpUtil.getOptionDataFromBuffer(in);
        sender = IcpUtil.getSenderFromBuffer(in);

        switch (opcode) {
          // ... QUERY
          case IcpMessage.ICP_OP_QUERY:
            requester = IcpUtil.getRequesterFromBuffer(in);
            payloadUrl = IcpUtil.getPayloadUrlFromBytes(packetData,
                true, IcpUtil.stringLength(length, true, false, (short)0));
            ret = new IcpMessageImpl(opcode,
                                     version,
                                     length,
                                     requestNumber,
                                     options,
                                     optionData,
                                     sender,
                                     requester,
                                     payloadUrl);
            break;

          // ... HIT_OBJECT
          case IcpMessage.ICP_OP_HIT_OBJ:
            payloadUrl = IcpUtil.getPayloadUrlFromBuffer(in, false);
            payloadLength =
              IcpUtil.getPayloadObjectLengthFromBuffer(in, payloadUrl);
            payloadObject = new byte[payloadLength];
            IcpUtil.getPayloadObjectFromBytes(
                packetData, payloadUrl, payloadLength, payloadObject);
            ret = new IcpMessageImpl(opcode,
                                     version,
                                     length,
                                     requestNumber,
                                     options,
                                     optionData,
                                     sender,
                                     payloadUrl,
                                     payloadObject,
                                     payloadLength);
            break;

          // ... OTHER
          default:
            payloadUrl = IcpUtil.getPayloadUrlFromBytes(packetData,
                false, IcpUtil.stringLength(length, false, false, (short)0));
            ret = new IcpMessageImpl(opcode,
                                     version,
                                     length,
                                     requestNumber,
                                     options,
                                     optionData,
                                     sender,
                                     payloadUrl);
            break;
        }

        // Set UDP info
        ret.setUdpAddress(new IPAddr(packet.getAddress()));
        ret.setUdpPort(packet.getPort());
        return ret;
      }
      catch (IcpProtocolException ipe) {
        throw ipe; // rethrow
      }
      catch (Exception exc) {
        throw new IcpProtocolException(
            "Error while decoding ICP packet", exc);
      }
    }

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
   * <p>Implements an ICP encoder.</p>
   * @author Thib Guicherd-Callin
   */
  private static class IcpEncoderImpl implements IcpEncoder {

    /* Inherit documentation */
    public DatagramPacket encode(IcpMessage message,
                                 IPAddr recipient) {
      return encode(message, recipient, IcpMessage.ICP_PORT);
    }

    /* Inherit documentation */
    public DatagramPacket encode(IcpMessage message,
                                 IPAddr recipient,
                                 int port) {
      byte[] rawBytes;

      try {
        int length = IcpUtil.computeLength(message);
        ByteBuffer out =
          ByteBuffer.allocate(length);

        // Write out header
        out.put(IcpUtil.OFFSET_BYTE_OPCODE, message.getOpcode());
        out.put(IcpUtil.OFFSET_BYTE_VERSION, message.getVersion());
        out.putShort(IcpUtil.OFFSET_SHORT_LENGTH, (short)length);
        out.putInt(IcpUtil.OFFSET_INT_REQUESTNUMBER, message.getRequestNumber());
        out.putInt(IcpUtil.OFFSET_INT_OPTIONS, message.getOptions());
        out.putInt(IcpUtil.OFFSET_INT_OPTIONDATA, message.getOptionData());
        rawBytes = message.getSender().getAddress();
        for (int off = 0 ; off < 4 ; ++off) {
          out.put(IcpUtil.OFFSET_INT_SENDER + off, rawBytes[off]);
        }

        // Write out payload
        final int OFFSET_PAYLOAD;
        if (message.isQuery()) {
          rawBytes = message.getRequester().getAddress();
          for (int off = 0 ; off < 4 ; ++off) {
            out.put(IcpUtil.OFFSET_INT_REQUESTER + off, rawBytes[off]);
          }
          OFFSET_PAYLOAD = IcpUtil.OFFSET_PAYLOAD_QUERY;
        }
        else {
          OFFSET_PAYLOAD = IcpUtil.OFFSET_PAYLOAD_NONQUERY;
        }
        rawBytes = message.getPayloadUrl().getBytes(Constants.URL_ENCODING);
        for (int off = 0 ; off < rawBytes.length ; ++off) {
          out.put(OFFSET_PAYLOAD + off, rawBytes[off]);
        }
        out.put(OFFSET_PAYLOAD + rawBytes.length, (byte)0); // null terminator
        if (message.getOpcode() == IcpMessage.ICP_OP_HIT_OBJ) {
          final int OFFSET_SHORT_PAYLOADLENGTH =
            IcpUtil.OFFSET_PAYLOAD_NONQUERY + rawBytes.length + 1;
          final int OFFSET_PAYLOADOBJECT =
            OFFSET_SHORT_PAYLOADLENGTH + 2;
          rawBytes = message.getPayloadObject();
          out.putShort(OFFSET_SHORT_PAYLOADLENGTH, (short)rawBytes.length);
          for (int off = 0 ; off < rawBytes.length ; ++off) {
            out.put(OFFSET_PAYLOADOBJECT + off, rawBytes[off]);
          }
        }

        // Make datagram
        rawBytes = out.array();
        return new DatagramPacket(rawBytes, 0, rawBytes.length,
            recipient.getInetAddr(), port);
      }
      catch (IOException ioe) {
        return null;
      }
    }

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
   * <p>An implementation of {@link IcpMessage}.</p>
   * @author Thib Guicherd-Callin
   */
  private static class IcpMessageImpl implements IcpMessage {

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
     * <p>Builds a completely shallow message.</p>
     */
    protected IcpMessageImpl() {
      // Shallow object
    }

    /**
     * <p>Builds an ICP query message by invoking
     * {@link #IcpMessageImpl(byte, byte, short, int, int, int, IPAddr, String)}
     * and initializing the requester field with the given
     * argument.</p>
     * @param opcode        An ICP opcode.
     * @param version       A version number.
     * @param length        An ICP message length.
     * @param requestNumber An opaque request number.
     * @param options       A raw options integer.
     * @param optionData    A raw options data integer.
     * @param sender        A sender address.
     * @param requester     A requester address.
     * @param payloadUrl    A payload URL.
     */
    protected IcpMessageImpl(byte opcode,
                             byte version,
                             short length,
                             int requestNumber,
                             int options,
                             int optionData,
                             IPAddr sender,
                             IPAddr requester,
                             String payloadUrl) {
      this(opcode,
           version,
           length,
           requestNumber,
           options,
           optionData,
           sender,
           payloadUrl);
      if (isQuery()) {
        this.requester = requester;
      }
    }

    /**
     * <p>Main constructor; builds an ICP message with the given
     * arguments, which means more initialization work may be
     * needed.</p>
     * @param opcode        An ICP opcode.
     * @param version       A version number.
     * @param length        An ICP message length.
     * @param requestNumber An opaque request number.
     * @param options       A raw options integer.
     * @param optionData    A raw options data integer.
     * @param sender        A sender address.
     * @param payloadUrl    A payload URL.
     */
    protected IcpMessageImpl(byte opcode,
                             byte version,
                             short length,
                             int requestNumber,
                             int options,
                             int optionData,
                             IPAddr sender,
                             String payloadUrl) {
      this();
      this.opcode = opcode;
      this.version = version;
      this.requestNumber = requestNumber;
      this.options = options;
      this.optionData = optionData;
      this.sender = sender;
      this.payloadUrl = payloadUrl;
      this.length = length;
    }

    /**
     * <p>Builds an ICP hit-object message by invoking
     * {@link #IcpMessageImpl(byte, byte, short, int, int, int, IPAddr, String)}
     * and by initializing the payload object and payload object
     * length fields with the given arguments.</p>
     * @param opcode              An ICP opcode.
     * @param version             A version number.
     * @param length              An ICP message length.
     * @param requestNumber       An opaque request number.
     * @param options             A raw options integer.
     * @param optionData          A raw options data integer.
     * @param sender              A sender address.
     * @param payloadUrl          A payload URL.
     * @param payloadObject       A payload object.
     * @param payloadObjectLength A payload object length.
     */
    protected IcpMessageImpl(byte opcode,
                             byte version,
                             short length,
                             int requestNumber,
                             int options,
                             int optionData,
                             IPAddr sender,
                             String payloadUrl,
                             byte[] payloadObject,
                             short payloadObjectLength) {
      this(opcode,
           version,
           length,
           requestNumber,
           options,
           optionData,
           sender,
           payloadUrl);
      if (getOpcode() == ICP_OP_HIT_OBJ) {
        this.payloadObject = payloadObject;
        this.payloadObjectLength = payloadObjectLength;
      }
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

    /* Inherit documentation */
    public void setUdpAddress(IPAddr udpAddress) {
      this.udpAddress = udpAddress;
    }

    /* Inherit documentation */
    public void setUdpPort(int port) {
      this.udpPort = port;
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
      buffer.append(";requestsSrcRtt=");
      buffer.append(Boolean.toString(requestsSrcRtt()));
      buffer.append(";requestsHitObj=");
      buffer.append(Boolean.toString(requestsHitObj()));
      buffer.append(";containsSrcRttResponse=");
      buffer.append(Boolean.toString(containsSrcRttResponse()));
      buffer.append(";optionData=");
      buffer.append(Integer.toHexString(getOptionData()));
      buffer.append(";srcRttResponse=");
      buffer.append(Integer.toHexString(getSrcRttResponse()));
      buffer.append(";sender=");
      buffer.append(getSender());
      if (isQuery()) {
        buffer.append(";requester=");
        buffer.append(getRequester());
      }
      buffer.append(";payloadUrl=");
      buffer.append(getPayloadUrl());
      if (getPayloadObject() != null) {
        buffer.append(ByteArray.toHexString(getPayloadObject()));
      }
      buffer.append("]");
      return buffer.toString();
    }

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
   * <p>An implementation of {@link IcpDecoder} that returns
   * ICP messages of type {@link LazyIcpMessageImpl}, which
   * are parsed lazily.</p>
   * @author Thib Guicherd-Callin
   * @see LazyIcpMessageImpl
   */
  private static class LazyIcpDecoderImpl implements IcpDecoder {

    /* Inherit documentation */
    public IcpMessage parseIcp(DatagramPacket packet)
        throws IcpProtocolException {
      LazyIcpMessageImpl ret = new LazyIcpMessageImpl(packet);
      ret.setUdpAddress(new IPAddr(packet.getAddress()));
      ret.setUdpPort(packet.getPort());
      return ret;
    }

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
   * <p>An implementation of {@link IcpMessage} that is lazily
   * parsed from raw byte data.</p>
   * @author Thib Guicherd-Callin
   */
  private static class LazyIcpMessageImpl extends IcpMessageImpl {

    /**
     * <p>The message's underlying raw bytes.</p>
     */
    protected byte[] bytes;

    /**
     * <p>A byte buffer wrapped around {@link bytes}.</p>
     */
    protected ByteBuffer in;

    /**
     * <p>A parsed flag for the length field.</p>
     */
    protected boolean parsedLength = false;

    /**
     * <p>A parsed flag for the opcode field.</p>
     */
    protected boolean parsedOpcode = false;

    /**
     * <p>A parsed flag for the options data field.</p>
     */
    protected boolean parsedOptionData = false;

    /**
     * <p>A parsed flag for the options field.</p>
     */
    protected boolean parsedOptions = false;

    /**
     * <p>A parsed flag for the payload object.</p>
     */
    protected boolean parsedPayloadObject = false;

    /**
     * <p>A parsed flag for the payload object length.</p>
     */
    protected boolean parsedPayloadObjectLength = false;

    /**
     * <p>A parsed flag for the payload URL.</p>
     */
    protected boolean parsedPayloadUrl = false;

    /**
     * <p>A parsed flag for the requester field.</p>
     */
    protected boolean parsedRequester = false;

    /**
     * <p>A parsed flag for the request number field.</p>
     */
    protected boolean parsedRequestNumber = false;

    /**
     * <p>A parsed flag for the sender field.</p>
     */
    protected boolean parsedSender = false;

    /**
     * <p>A parsed flag for the version field.</p>
     */
    protected boolean parsedVersion = false;

    /**
     * <p>Builds a new lazily parsed ICP message based on
     * the given raw bytes (which are copied).</p>
     * @param data An array of raw ICP bytes.
     */
    protected LazyIcpMessageImpl(byte[] data) {
      super();
      bytes = new byte[data.length];
      System.arraycopy(data, 0, bytes, 0, data.length);
      in = ByteBuffer.wrap(bytes);
    }

    /**
     * <p>Builds a new lazily parsed ICP message based on
     * the given datagram packet (whose raws bytes are copied).</p>
     * @param data An ICP datagram packet.
     * @see #LazyIcpMessageImpl(byte[])
     */
    protected LazyIcpMessageImpl(DatagramPacket packet) {
      this(packet.getData());
    }

    /* Inherit documentation */
    public short getLength() {
      if (!parsedLength) {
        length = IcpUtil.getLengthFromBuffer(in);
        parsedLength = true;
      }
      return length;
    }

    /* Inherit documentation */
    public byte getOpcode() {
      if (!parsedOpcode) {
        opcode = IcpUtil.getOpcodeFromBuffer(in);
        parsedOpcode = true;
      }
      return opcode;
    }

    /* Inherit documentation */
    public int getOptionData() {
      if (!parsedOptionData) {
        optionData = IcpUtil.getOptionDataFromBuffer(in);
        parsedOptionData = true;
      }
      return optionData;
    }

    /* Inherit documentation */
    public int getOptions() {
      if (!parsedOptions) {
        options = IcpUtil.getOptionsFromBuffer(in);
        parsedOptions = true;
      }
      return options;
    }

    /* Inherit documentation */
    public byte[] getPayloadObject() {
      if (!parsedPayloadObject) {
        if (getOpcode() == IcpMessage.ICP_OP_HIT_OBJ) {
          payloadObject = new byte[getPayloadObjectLength()];
          IcpUtil.getPayloadObjectFromBytes(
              bytes, getPayloadUrl(), getPayloadObjectLength(), payloadObject);
        }
        parsedPayloadObject = true;
      }
      return payloadObject;
    }

    /* Inherit documentation */
    public short getPayloadObjectLength() {
      if (!parsedPayloadObjectLength) {
        if (getOpcode() == IcpMessage.ICP_OP_HIT_OBJ) {
          payloadObjectLength =
            IcpUtil.getPayloadObjectLengthFromBuffer(in, getPayloadUrl());
        }
        parsedPayloadObjectLength = true;
      }
      return payloadObjectLength;
    }

    /* Inherit documentation */
    public String getPayloadUrl() {
      if (!parsedPayloadUrl) {
        if (getOpcode() == ICP_OP_HIT_OBJ) {
          payloadUrl = IcpUtil.getPayloadUrlFromBuffer(in, false);
        }
        else {
          boolean isQuery = isQuery();
          payloadUrl = IcpUtil.getPayloadUrlFromBytes(bytes, isQuery,
              IcpUtil.stringLength(getLength(), isQuery, false, (short)0));
        }
        parsedPayloadUrl = true;
      }
      return payloadUrl;
    }

    /* Inherit documentation */
    public IPAddr getRequester() {
      if (!parsedRequester) {
        if (isQuery()) {
          requester = IcpUtil.getRequesterFromBuffer(in);
        }
        parsedRequester = true;
      }
      return requester;
    }

    /* Inherit documentation */
    public int getRequestNumber() {
      if (!parsedRequestNumber) {
        requestNumber = IcpUtil.getRequestNumberFromBuffer(in);
        parsedRequestNumber = true;
      }
      return requestNumber;
    }

    /* Inherit documentation */
    public IPAddr getSender() {
      if (!parsedSender) {
        sender = IcpUtil.getSenderFromBuffer(in);
        parsedSender = true;
      }
      return sender;
    }

    /* Inherit documentation */
    public byte getVersion() {
      if (!parsedVersion) {
        version = IcpUtil.getVersionFromBuffer(in);
        parsedVersion = true;
      }
      return version;
    }

    /**
     * <p>Returns a detailed string representation of this message
     * (<strong>warning: causes the entire message to be parsed;
     * the benefits of lazy parsing are lost after a call to this
     * method</strong>).</p>
     * @return A detailed string representation of this message, as
     *         returned by the parent method
     *         {@link IcpMessageImpl#toString}.
     * @see #toString
     * @see IcpMessageImpl#toString
     */
    public String toLongString() {
      return super.toString();
    }

    /**
     * <p>Returns a brief string representation of this message
     * (with the least possible footprint on lazy parsing).</p>
     * @return A brief string representation of this message.
     * @see #toLongString
     */
    public String toString() {
      StringBuffer sb = new StringBuffer();
      sb.append("[");
      sb.append(getClass().getName());
      sb.append(";opcode=");
      sb.append(getOpcode());
      sb.append(";payloadUrl=");
      sb.append(getPayloadUrl());
      sb.append(";(call toLongString() for detailed string)]");
      return sb.toString();
    }

  }
  /*
   * end STATIC NESTED CLASS
   * =======================
   */

  /**
   * <p>Cannot instantiate instances of this class; use static methods
   * instead to obtain service.</p>
   * @see #makeBuilderFactory
   * @see #makeDecoderFactory
   * @see #makeEncoderFactory
   * @see #makeIcpFactory
   */
  private IcpFactoryImpl() {}

  /* Inherit documentation */
  public synchronized IcpBuilder makeIcpBuilder() {
    if (singletonBuilder == null) {
      singletonBuilder = new IcpBuilderImpl();
    }
    return singletonBuilder;
  }

  /* Inherit documentation */
  public synchronized IcpDecoder makeIcpDecoder() {
    if (singletonDecoder == null) {
      singletonDecoder = new IcpDecoderImpl();
    }
    return singletonDecoder;
  }

  /* Inherit documentation */
  public synchronized IcpEncoder makeIcpEncoder() {
    if (singletonEncoder == null) {
      singletonEncoder = new IcpEncoderImpl();
    }
    return singletonEncoder;
  }

  /**
   * <p>A singleton instance of {@link IcpFactoryImpl}.</p>
   */
  private static IcpFactory singleton;

  /**
   * <p>A singleton instance of {@link IcpBuilderImpl}.</p>
   */
  private static IcpBuilder singletonBuilder;

  /**
   * <p>A singleton instance of {@link IcpDecoderImpl}.</p>
   */
  private static IcpDecoder singletonDecoder;

  /**
   * <p>A singleton instance of {@link IcpEncoderImpl}.</p>
   */
  private static IcpEncoder singletonEncoder;

  /**
   * <p>A singleton instance of {@link LazyIcpDecoderImpl}.</p>
   */
  private static IcpDecoder singletonLazyDecoder;

  /**
   * <p>Gets an instance of type {@link IcpBuilder.Factory}.</p>
   * @return An ICP builder factory.
   */
  public static IcpBuilder.Factory makeBuilderFactory() {
    return makeSingleton();
  }

  /**
   * <p>Gets an instance of type {@link IcpDecoder.Factory}.</p>
   * @return An ICP decoder factory.
   */
  public static IcpDecoder.Factory makeDecoderFactory() {
    return makeSingleton();
  }

  /**
   * <p>Gets an instance of type {@link IcpEncoder}.</p>
   * @return An ICP encoder factory.
   */
  public static IcpEncoder.Factory makeEncoderFactory() {
    return makeSingleton();
  }

  /**
   * <p>Gets an instance of type {@link IcpFactory}.</p>
   * @return An ICP factory.
   */
  public static IcpFactory makeIcpFactory() {
    return makeSingleton();
  }

  /**
   * <p>Builds a lazy ICP decoder.</p>
   * <p>Eventually this direct method needs to find a better
   * hiding place.</p>
   * @return An instance of type {@link IcpDecoder}.
   */
  public static synchronized IcpDecoder makeLazyIcpDecoder() {
    if (singletonLazyDecoder == null) {
      singletonLazyDecoder = new LazyIcpDecoderImpl();
    }
    return singletonLazyDecoder;
  }

  /**
   * <p>Makes a singleton instance of this class.</p>
   * @return An ICP factory.
   */
  private static synchronized IcpFactory makeSingleton() {
    if (singleton == null) {
      singleton = new IcpFactoryImpl();
    }
    return singleton;
  }

}
