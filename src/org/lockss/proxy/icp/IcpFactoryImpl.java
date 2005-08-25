/*
 * $Id: IcpFactoryImpl.java,v 1.1 2005-08-25 20:12:37 thib_gc Exp $
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

public class IcpFactoryImpl implements IcpFactory {

  private static class IcpBuilderImpl implements IcpBuilder {

    public IcpMessage makeDenied(IcpMessage query)
        throws IcpProtocolException {
      if (query.getOpcode() != IcpMessage.ICP_OP_QUERY) { 
        throw new IcpProtocolException("ICP_OP_DENIED is a response to ICP_OP_QUERY.");
      }
      return new IcpMessageImpl(IcpMessage.ICP_OP_DENIED, IcpMessage.ICP_VERSION,
          query.getRequestNumber(), 0, 0, ZERO_ADDRESS, query.getPayloadUrl());
    }
    
    public IcpMessage makeDiscoveryEcho(URL query) {
      return null; // Unimplemented
    }
    
    public IcpMessage makeError(IcpMessage query)
        throws IcpProtocolException {
      if (query.getOpcode() != IcpMessage.ICP_OP_QUERY) { 
        throw new IcpProtocolException("ICP_OP_ERR is a response to ICP_OP_QUERY.");
      }
      return new IcpMessageImpl(IcpMessage.ICP_OP_ERR, IcpMessage.ICP_VERSION,
          query.getRequestNumber(), 0, 0, ZERO_ADDRESS, query.getPayloadUrl());
    }

    public IcpMessage makeHit(IcpMessage query)
        throws IcpProtocolException {
      if (query.getOpcode() != IcpMessage.ICP_OP_QUERY) {
        throw new IcpProtocolException("ICP_OP_HIT is a response to ICP_OP_QUERY.");
      }
      return new IcpMessageImpl(IcpMessage.ICP_OP_HIT, IcpMessage.ICP_VERSION,
          query.getRequestNumber(), 0, 0, ZERO_ADDRESS, query.getPayloadUrl());
    }

    public IcpMessage makeHit(IcpMessage query,
                              short srcRttResponse)
        throws IcpProtocolException {
      if (query.getOpcode() != IcpMessage.ICP_OP_QUERY) {
        throw new IcpProtocolException("ICP_OP_HIT is a response to ICP_OP_QUERY.");
      }
      if (!query.requestsSrcRtt()) {
        throw new IcpProtocolException("Query does not request source return trip time.");
      }
      return new IcpMessageImpl(IcpMessage.ICP_OP_HIT, IcpMessage.ICP_VERSION,
          query.getRequestNumber(), IcpMessage.ICP_FLAG_SRC_RTT, (int)srcRttResponse,
          ZERO_ADDRESS, query.getPayloadUrl());
    }

    public IcpMessage makeHitObj(IcpMessage query,
                                 byte[] payloadObject)
        throws IcpProtocolException {
      if (query.getOpcode() != IcpMessage.ICP_OP_QUERY) {
        throw new IcpProtocolException("ICP_OP_HIT_OBJ is a response to ICP_OP_QUERY.");
      }
      return new IcpMessageImpl(IcpMessage.ICP_OP_HIT_OBJ, IcpMessage.ICP_VERSION,
          query.getRequestNumber(), IcpMessage.ICP_FLAG_HIT_OBJ, 0, ZERO_ADDRESS,
          query.getPayloadUrl(), payloadObject);
    }

    public IcpMessage makeHitObj(IcpMessage query,
                                 short srcRttResponse,
                                 byte[] payloadObject)
        throws IcpProtocolException {
      if (query.getOpcode() != IcpMessage.ICP_OP_QUERY) {
        throw new IcpProtocolException("ICP_OP_HIT_OBJ is a response to ICP_OP_QUERY.");
      }
      if (!query.requestsSrcRtt()) {
        throw new IcpProtocolException("Query does not request source return trip time.");
      }
      return new IcpMessageImpl(IcpMessage.ICP_OP_HIT_OBJ,
          IcpMessage.ICP_VERSION, query.getRequestNumber(),
          IcpMessage.ICP_FLAG_HIT_OBJ | IcpMessage.ICP_FLAG_SRC_RTT,
          (int)srcRttResponse, ZERO_ADDRESS, query.getPayloadUrl(),
          payloadObject);
    }

    public IcpMessage makeMiss(IcpMessage query)
        throws IcpProtocolException { 
      if (query.getOpcode() != IcpMessage.ICP_OP_QUERY) {
        throw new IcpProtocolException("ICP_OP_MISS is a response to ICP_OP_QUERY."); 
      }
      return new IcpMessageImpl(IcpMessage.ICP_OP_MISS, IcpMessage.ICP_VERSION,
          query.getRequestNumber(), 0, 0, ZERO_ADDRESS, query.getPayloadUrl());
    }

    public IcpMessage makeMiss(IcpMessage query,
                               short srcRttResponse)
        throws IcpProtocolException {
      if (query.getOpcode() != IcpMessage.ICP_OP_QUERY) {
        throw new IcpProtocolException("ICP_OP_MISS is a response to ICP_OP_QUERY."); 
      }
      if (!query.requestsSrcRtt()) {
        throw new IcpProtocolException("Query does not request source return trip time.");
      }
      return new IcpMessageImpl(IcpMessage.ICP_OP_MISS, IcpMessage.ICP_VERSION,
          query.getRequestNumber(), IcpMessage.ICP_FLAG_SRC_RTT,
          (int)srcRttResponse, ZERO_ADDRESS, query.getPayloadUrl());
    }

    public IcpMessage makeMissNoFetch(IcpMessage query) throws IcpProtocolException {
      if (query.getOpcode() != IcpMessage.ICP_OP_QUERY) {
        throw new IcpProtocolException("ICP_OP_MISS_NOFETCH is a response to ICP_OP_QUERY."); 
      }
      return new IcpMessageImpl(IcpMessage.ICP_OP_MISS_NOFETCH,
          IcpMessage.ICP_VERSION, query.getRequestNumber(), 0, 0,
          ZERO_ADDRESS, query.getPayloadUrl());
    }

    public IcpMessage makeMissNoFetch(IcpMessage query,
                                      short srcRttResponse)
        throws IcpProtocolException {
      if (query.getOpcode() != IcpMessage.ICP_OP_QUERY) {
        throw new IcpProtocolException("ICP_OP_MISS_NOFETCH is a response to ICP_OP_QUERY."); 
      }
      if (!query.requestsSrcRtt()) {
        throw new IcpProtocolException("Query does not request source return trip time.");
      }
      return new IcpMessageImpl(IcpMessage.ICP_OP_MISS_NOFETCH,
          IcpMessage.ICP_VERSION, query.getRequestNumber(),
          IcpMessage.ICP_FLAG_SRC_RTT, srcRttResponse, ZERO_ADDRESS,
          query.getPayloadUrl());
    }

    public IcpMessage makeQuery(InetAddress requesterAddress,
                                URL query) {
      return makeQuery(requesterAddress, query, false, false);
    }

    public IcpMessage makeQuery(InetAddress requesterAddress,
                                URL query,
                                boolean requestSrcRtt,
                                boolean requestHitObj) {
      return new IcpMessageImpl(
          IcpMessage.ICP_OP_QUERY,
          IcpMessage.ICP_VERSION,
          getNewRequestNumber(),
            (requestSrcRtt ? IcpMessage.ICP_FLAG_SRC_RTT : 0)
          | (requestHitObj ? IcpMessage.ICP_FLAG_HIT_OBJ : 0),
          0,
          ZERO_ADDRESS,
          requesterAddress,
          query);
    }

    public IcpMessage makeSourceEcho(URL query) {
      return null; // Unimplemented
    }

    private static int requestNumberCounter = 1;

    private static final InetAddress ZERO_ADDRESS;

    static {
      try {
        byte[] zero = new byte[] {0, 0, 0, 0};
        ZERO_ADDRESS = InetAddress.getByAddress(zero);
      }
      catch (UnknownHostException e) {
        throw new RuntimeException("Could not create the zero address constant");
      }
    }

    private static int getNewRequestNumber() {
      return requestNumberCounter++;
    }
    
  }
  
  private static class IcpDecoderImpl implements IcpDecoder {
  
    public IcpMessage parseIcp(DatagramPacket packet) 
        throws IcpProtocolException {
      try {
        IcpMessage ret = null;
        ByteArrayInputStream inBytes =
          new ByteArrayInputStream(packet.getData());
        DataInputStream inData =
          new DataInputStream(inBytes);
  
        // Unconditional processing
        byte opcode = inData.readByte();
        byte version = inData.readByte();
        short length = inData.readShort();
        int requestNumber = inData.readInt();
        int options = inData.readInt();
        int optionData = inData.readInt();
        InetAddress sender = getIpFromStream(inData);
        
        // Conditional processing
        InetAddress requester;
        URL payloadUrl;
        short payloadLength;
        byte[] payloadObject;
        switch (opcode) {
          case IcpMessage.ICP_OP_QUERY:
            requester = getIpFromStream(inData);
            payloadUrl = getUrlFromStream(inData);
            ret = new IcpMessageImpl(opcode, version, requestNumber,
                options, optionData, sender, requester, payloadUrl);
            break;
          case IcpMessage.ICP_OP_HIT_OBJ:
            payloadUrl = getUrlFromStream(inData);
            payloadLength = inData.readShort();
            payloadObject = new byte[payloadLength];
            inData.read(payloadObject, 0, payloadObject.length);
            ret = new IcpMessageImpl(opcode, version, requestNumber,
                options, optionData, sender, payloadUrl, payloadObject);
            break;
          default:
            payloadUrl = getUrlFromStream(inData);
            ret = new IcpMessageImpl(opcode, version, requestNumber,
                options, optionData, sender, payloadUrl);
            break;
        }
        
        ret.setUdpAddress(packet.getAddress());
        ret.setUdpPort(packet.getPort());
        return ret;
      }
      catch (Exception e) {
        throw new IcpProtocolException(
            "Error while parsing datagram", e);
      }
    }
    
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
      catch (Exception e) {
        throw new IcpProtocolException(
            "Error while parsing IP from stream", e);
      }
    }
    
    private URL getUrlFromStream(DataInputStream inData)
        throws IcpProtocolException {
      try {
        StringBuffer buffer = new StringBuffer();
        byte[] inputBytes = new byte[1];
        while ( (inputBytes[0] = inData.readByte()) != (byte)0) {
          buffer.append(new String(inputBytes, "US-ASCII"));
        }
        return new URL(buffer.toString());
      }
      catch (Exception e) {
        throw new IcpProtocolException(
            "Error while parsing URL from stream", e);
      }
    }
    
  }
  
  private static class IcpEncoderImpl implements IcpEncoder {
    
    public DatagramPacket encode(IcpMessage message,
                                 InetAddress recipient) {
      return encode(message, recipient, IcpMessage.ICP_PORT);
    }
    
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
        writeIpBytes(outData, message.getSender());
        if (message.getOpcode() == IcpMessage.ICP_OP_QUERY) {
          writeIpBytes(outData, message.getRequester());
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
    
    private static void writeIpBytes(OutputStream out, InetAddress address) 
        throws IOException {
      byte[] ip = address.getAddress();
      for (int bb = 0 ; bb < 4 ; bb++) { 
        out.write(ip[bb]); 
      }
    }
    
  }

  private static class IcpMessageImpl implements IcpMessage {

    private short length;

    private byte opcode;

    private int optionData;

    private int options;

    private byte[] payloadObject;

    private URL payloadUrl;

    private InetAddress requester;

    private int requestNumber;

    private InetAddress sender;

    private InetAddress udpAddress;
    
    private int udpPort;

    private byte version;

    protected IcpMessageImpl(byte opcode,
                             byte version,
                             int requestNumber,
                             int options,
                             int optionData,
                             InetAddress sender,
                             InetAddress requester,
                             URL payloadUrl) {
      this(opcode, version, requestNumber, options,
          optionData, sender, payloadUrl);
      if (getOpcode() == ICP_OP_QUERY) {
        this.requester = requester;
        this.length += 4; // size of requester field
      }
    }

    protected IcpMessageImpl(byte opcode,
                             byte version,
                             int requestNumber,
                             int options,
                             int optionData,
                             InetAddress sender,
                             URL payloadUrl) {
      this.opcode = opcode;
      this.version = version;
      this.requestNumber = requestNumber;
      this.options = options;
      this.optionData = optionData;
      this.sender = sender;
      this.payloadUrl = payloadUrl;
      this.length = (short)(21 + payloadUrl.toString().length());
      // 21 = size of header (20) + null-terminated URL (1)
    }

    protected IcpMessageImpl(byte opcode,
                             byte version,
                             int requestNumber,
                             int options,
                             int optionData,
                             InetAddress sender,
                             URL payloadUrl,
                             byte[] payloadObject) {
      this(opcode, version, requestNumber, options,
          optionData, sender, payloadUrl);
      if (getOpcode() == ICP_OP_HIT_OBJ) {
        this.payloadObject = payloadObject;
        this.length += payloadObject.length; // size of payload
        this.length += 2; // size of payload length field
      }
    }
    
    public boolean containsSrcRttResponse() {
      byte opcode = getOpcode();
      return (opcode == ICP_OP_HIT || opcode == ICP_OP_MISS
          || opcode == ICP_OP_MISS_NOFETCH || opcode == ICP_OP_HIT_OBJ)
          && (getOptions() & ICP_FLAG_SRC_RTT) != 0;
    }
    
    /**
     * @see org.thib.lockss.playground.icp.IcpMessage#getLength()
     */
    public short getLength() {
      return length;
    }
    
    /**
     * @see org.thib.lockss.playground.icp.IcpMessage#getOpcode()
     */
    public byte getOpcode() {
      return opcode;
    }

    /**
     * @see org.thib.lockss.playground.icp.IcpMessage#getOptionData()
     */
    public int getOptionData() {
      return optionData;
    }

    /**
     * @see org.thib.lockss.playground.icp.IcpMessage#getOptions()
     */
    public int getOptions() {
      return options;
    }

    /**
     * @see org.thib.lockss.playground.icp.IcpMessage#getPayloadObject()
     */
    public byte[] getPayloadObject() {
      return payloadObject;
    }
    
    /**
     * @see org.thib.lockss.playground.icp.IcpMessage#getPayloadObject()
     */
    public short getPayloadObjectLength() {
      byte[] payload = getPayloadObject();
      if (opcode == ICP_OP_HIT_OBJ && payload != null) {
        return (short)payload.length;
      }
      else {
        return (short)0;
      }
    }

    /**
     * @see org.thib.lockss.playground.icp.IcpMessage#getPayloadUrl()
     */
    public URL getPayloadUrl() {
      return payloadUrl;
    }

    /**
     * @see org.thib.lockss.playground.icp.IcpMessage#getRequester()
     */
    public InetAddress getRequester() {
      return requester;
    }

    /**
     * @see org.thib.lockss.playground.icp.IcpMessage#getRequestNumber()
     */
    public int getRequestNumber() {
      return requestNumber;
    }
    
    /**
     * @see org.thib.lockss.playground.icp.IcpMessage#getSender()
     */
    public InetAddress getSender() {
      return sender;
    }

    public short getSrcRttResponse() {
      if (containsSrcRttResponse()) {
        return (short)(getOptionData() & 0x0000ffff);
      }
      else {
        return (short)0;        
      }
    }
    
    /**
     * @see org.thib.lockss.playground.icp.IcpMessage#getUdpAddress()
     */
    public InetAddress getUdpAddress() {
      return udpAddress;
    }
    
    public int getUdpPort() {
      return udpPort;
    }

    /**
     * @see org.thib.lockss.playground.icp.IcpMessage#getVersion()
     */
    public byte getVersion() {
      return version;
    }

    public boolean requestsHitObj() {
      return    getOpcode() == ICP_OP_QUERY
             && (getOptions() & ICP_FLAG_HIT_OBJ) != 0;
    }

    public boolean requestsSrcRtt() {
      return    getOpcode() == ICP_OP_QUERY
             && (getOptions() & ICP_FLAG_SRC_RTT) != 0;

    }
    
    public void setUdpAddress(InetAddress udpAddress) {
      this.udpAddress = udpAddress;
    }
    
    public void setUdpPort(int port) {
      this.udpPort = port;
    }

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
      buffer.append(";requester=");
      buffer.append(getRequester());
      buffer.append(";payloadUrl=");
      buffer.append(getPayloadUrl());
      buffer.append(";payloadObject=");
      if (getPayloadObject() != null) {
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

  private IcpFactoryImpl() {}
  
  public synchronized IcpBuilder makeIcpBuilder() {
    if (singletonBuilder == null) {
      singletonBuilder = new IcpBuilderImpl();
    }
    return singletonBuilder;
  }

  public synchronized IcpDecoder makeIcpDecoder() {
    if (singletonDecoder == null) {
      singletonDecoder = new IcpDecoderImpl();
    }
    return singletonDecoder;
  }
  
  public synchronized IcpEncoder makeIcpEncoder() {
    if (singletonEncoder == null) {
      singletonEncoder = new IcpEncoderImpl();
    }
    return singletonEncoder;
  }

  private static IcpFactory singleton;
  
  private static IcpBuilder singletonBuilder;
  
  private static IcpDecoder singletonDecoder;

  private static IcpEncoder singletonEncoder;

  public static IcpBuilderFactory makeBuilderFactory() {
    return makeSingleton();
  }
  
  public static IcpDecoderFactory makeDecoderFactory() {
    return makeSingleton();
  }
  
  public static IcpEncoderFactory makeEncoderFactory() {
    return makeSingleton();
  }
  
  public static IcpFactory makeIcpFactory() {
    return makeSingleton();
  }

  private static synchronized IcpFactory makeSingleton() {
    if (singleton == null) {
      singleton = new IcpFactoryImpl();
    }
    return singleton;
  }
  
}
