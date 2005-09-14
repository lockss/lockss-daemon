/*
 * $Id: IcpFactoryImpl.java,v 1.7 2005-09-14 00:33:40 thib_gc Exp $
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
    public IcpMessage makeQuery(InetAddress requesterAddress,
                                String query) {
      return makeQuery(requesterAddress, query, false, false);
    }

    /* Inherit documentation */
    public IcpMessage makeQuery(InetAddress requesterAddress,
                                String query,
                                boolean requestSrcRtt,
                                boolean requestHitObj) {
      try {
        return new IcpMessageImpl(
            IcpMessage.ICP_OP_QUERY,
            IcpMessage.ICP_VERSION,
            (short)(21 + query.toString().getBytes("US-ASCII").length),
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
    private static final InetAddress ZERO_ADDRESS;

    /*
     * begin STATIC INITIALIZER
     * ========================
     */
    static {
      try {
        byte[] zero = new byte[] {0, 0, 0, 0};
        ZERO_ADDRESS = InetAddress.getByAddress(zero);
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
  
    /* Inherit documentation */
    public IcpMessage parseIcp(DatagramPacket packet) 
        throws IcpProtocolException {
      try {
        // *** Local variables ***
        byte version;
        short length;
        int requestNumber;
        int options;
        int optionData;
        InetAddress sender = null;
        InetAddress requester = null;
        String payloadUrl = null;
        short payloadLength;
        byte[] payloadObject = null;
        IcpMessage ret = null;
        ByteArrayInputStream inBytes =
          new ByteArrayInputStream(packet.getData());
        DataInputStream inData = 
          new DataInputStream(inBytes);
  
        // *** Unconditional processing ***
        byte opcode;
        try {
          opcode = inData.readByte();
          if (!IcpUtil.isValidOpcode(opcode)) {
            throw new IcpProtocolException(
                "Invalid opcode: " + opcode);
          }
          version = inData.readByte();
          length = inData.readShort();
          requestNumber = inData.readInt();
          options = inData.readInt();
          optionData = inData.readInt();
          sender = getIpFromStream(inData);
        }
        catch (EOFException eofe) {
          throw new IcpProtocolException(
              END_OF_STREAM, eofe);
        }
        
        // *** Conditional processing ***
        switch (opcode) {
          case IcpMessage.ICP_OP_QUERY:
            requester = getIpFromStream(inData);
            payloadUrl = getUrlFromStream(inData);
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
          case IcpMessage.ICP_OP_HIT_OBJ:
            payloadUrl = getUrlFromStream(inData);
            payloadLength = inData.readShort();
            payloadObject = new byte[payloadLength];
            inData.read(payloadObject, 0, payloadObject.length);
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
          default:
            payloadUrl = getUrlFromStream(inData);
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
        
        // *** Set UDP info ***
        ret.setUdpAddress(packet.getAddress());
        ret.setUdpPort(packet.getPort());
        return ret;
      }
      catch (Exception exc) {
        throw new IcpProtocolException(
            "Error while parsing datagram", exc);
      }
    }
    
    /**
     * <p>Reads an IP address from the given stream.</p>
     * @param inData A data input stream.
     * @return The IP address obtained from the next 4 bytes of the
     *         argument stream.
     * @throws IcpProtocolException if any exception arises.
     */
    private InetAddress getIpFromStream(DataInputStream inData)
        throws IcpProtocolException {
      try {
        int rawIpInt = inData.readInt();
        return InetAddress.getByAddress(new byte[] {
            (byte)((rawIpInt & 0xff000000) >>> 24),
            (byte)((rawIpInt & 0x00ff0000) >>> 16),
            (byte)((rawIpInt & 0x0000ff00) >>> 8),
            (byte)(rawIpInt & 0x000000ff)
        });
      }
      catch (EOFException eofe) {
        throw new IcpProtocolException(
            END_OF_STREAM, eofe);
      }
      catch (Exception exc) {
        throw new IcpProtocolException(
            "Error while parsing IP from stream", exc);
      }
    }
    
    /**
     * <p>Reads a null-terminated URL from the given stream (and
     * consumes the null byte).</p>
     * @param inData A data input stream.
     * @return A URL string obtained from reading bytes from the
     *         stream.
     * @throws IcpProtocolException if any exception arises.
     */
    private String getUrlFromStream(DataInputStream inData)
        throws IcpProtocolException {
      try {
        StringBuffer buffer = new StringBuffer();
        byte[] inputBytes = new byte[1];
        while ( (inputBytes[0] = inData.readByte()) != (byte)0) {
          buffer.append((char)inputBytes[0]);
        }
        return buffer.toString();
      }
      catch (EOFException eofe) {
        throw new IcpProtocolException(
            END_OF_STREAM, eofe);
      }
      catch (Exception exc) {
        throw new IcpProtocolException(
            "Error while parsing URL from stream", exc);
      }
    }

    /**
     * <p>An error string used multiple times.</p>
     */
    private static final String END_OF_STREAM =
      "Unexpected end of data stream";
    
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
                                 InetAddress recipient) {
      return encode(message, recipient, IcpMessage.ICP_PORT);
    }
    
    /* Inherit documentation */
    public DatagramPacket encode(IcpMessage message,
                                 InetAddress recipient,
                                 int port) {
      try {
        ByteArrayOutputStream outBytes =
          new ByteArrayOutputStream(message.getLength());
        DataOutputStream outData =
          new DataOutputStream(outBytes);
        
        // Write out data
        outData.writeByte(message.getOpcode());
        outData.writeByte(message.getVersion());
        outData.writeShort(message.getLength());
        outData.writeInt(message.getRequestNumber());
        outData.writeInt(message.getOptions());
        outData.writeInt(message.getOptionData());
        outData.write(message.getSender().getAddress());
        if (message.isQuery()) {
          outData.write(message.getRequester().getAddress());
        }
        outData.writeBytes(message.getPayloadUrl().toString());
        outData.write(0); // payload URL must be null-terminated
        if (message.getOpcode() == IcpMessage.ICP_OP_HIT_OBJ) {
          outData.writeShort(message.getPayloadObjectLength());
          outData.write(message.getPayloadObject());
        }
        
        // Make datagram
        byte[] rawBytes = outBytes.toByteArray();
        return new DatagramPacket(rawBytes, 0, rawBytes.length,
            recipient, port);
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
   * <p>Implements an ICP message.</p>
   * @author Thib Guicherd-Callin
   */
  private static class IcpMessageImpl implements IcpMessage {

    /**
     * <p>A length field.<p>
     */
    private short length;

    /**
     * <p>An opcode field.</p>
     */
    private byte opcode;

    /**
     * <p>An options data field.</p>
     */
    private int optionData;

    /**
     * <p>An options field.</p>
     */
    private int options;

    /**
     * <p>A payload object field.</p>
     */
    private byte[] payloadObject;
    
    /**
     * <p>A payload length field, which may be different from
     * <code>payloadObject.length</code>.</p>
     */
    private short payloadObjectLength;

    /**
     * <p>A payload URL field.</p>
     */
    private String payloadUrl;

    /**
     * <p>A requester field.</p>
     */
    private InetAddress requester;

    /**
     * <p>A request number field.</p>
     */
    private int requestNumber;

    /**
     * <p>A sender field.</p>
     */
    private InetAddress sender;

    /**
     * <p>A UDP sender field.</p>
     */
    private InetAddress udpAddress;
    
    /**
     * <p>A UDP port field.</p>
     */
    private int udpPort;

    /**
     * <p>A version field.</p>
     */
    private byte version;

    /**
     * <p>Builds an ICP query message by invoking
     * {@link #IcpMessageImpl(byte, byte, short, int, int, int, InetAddress, String)}
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
                             InetAddress sender,
                             InetAddress requester,
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
                             InetAddress sender,
                             String payloadUrl) {
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
     * {@link #IcpMessageImpl(byte, byte, short, int, int, int, InetAddress, String)}
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
                             InetAddress sender,
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
    public InetAddress getRequester() {
      return isQuery() ? requester : null;
    }

    /* Inherit documentation */
    public int getRequestNumber() {
      return requestNumber;
    }
    
    /* Inherit documentation */
    public InetAddress getSender() {
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
    public InetAddress getUdpAddress() {
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
    public void setUdpAddress(InetAddress udpAddress) {
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
        buffer.append(";payloadObject=");
        byte[] payload = getPayloadObject();
        for (int ii = 0 ; ii < payload.length ; ii++) {
          buffer.append(Integer.toHexString(payload[ii]));
          if ((ii % 8) == 7) {
            buffer.append(" ");
          }
        }
      }
      buffer.append("]");
      return buffer.toString();
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
