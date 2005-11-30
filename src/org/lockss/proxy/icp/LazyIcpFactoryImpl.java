/*
 * $Id: LazyIcpFactoryImpl.java,v 1.1 2005-11-30 17:46:54 thib_gc Exp $
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

import java.net.DatagramPacket;
import java.nio.ByteBuffer;

import org.lockss.util.IPAddr;

public class LazyIcpFactoryImpl extends IcpFactoryImpl {

  /*
   * begin STATIC NESTED CLASS
   * =========================
   */
  /**
   * <p>An implementation of {@link IcpBuilder} customized for the
   * LOCKSS daemon's {@link IcpManager}, that attempts to use
   * fast mutable ICP messages.</p>
   * @author Thib Guicherd-Callin
   * @see LazyIcpMessageImpl
   */
  protected static class LazyIcpBuilderImpl extends IcpBuilderImpl {

    /* Inherit documentation */
    public IcpMessage makeDenied(IcpMessage query) throws IcpProtocolException {
      if (!query.isQuery()) {
        throw new IcpProtocolException(DENIED_NOT_QUERY_ERROR);
      }
      if (!(query instanceof LazyIcpMessageImpl)) {
        return super.makeDenied(query);
      }
      IcpUtil.writeResponse(((LazyIcpMessageImpl)query).getByteBuffer(),
                            IcpMessage.ICP_OP_DENIED);
      return query;
    }

    /* Inherit documentation */
    public IcpMessage makeError(IcpMessage query) throws IcpProtocolException {
      if (!query.isQuery()) {
        throw new IcpProtocolException(ERR_NOT_QUERY_ERROR);
      }
      if (!(query instanceof LazyIcpMessageImpl)) {
        return super.makeError(query);
      }
      IcpUtil.writeResponse(((LazyIcpMessageImpl)query).getByteBuffer(),
                            IcpMessage.ICP_OP_ERR);
      return query;
    }

    /* Inherit documentation */
    public IcpMessage makeHit(IcpMessage query) throws IcpProtocolException {
      if (!query.isQuery()) {
        throw new IcpProtocolException(HIT_NOT_QUERY_ERROR);
      }
      if (!(query instanceof LazyIcpMessageImpl)) {
        return super.makeHit(query);
      }
      IcpUtil.writeResponse(((LazyIcpMessageImpl)query).getByteBuffer(),
                            IcpMessage.ICP_OP_HIT);
      return query;
    }

    /* Inherit documentation */
    public IcpMessage makeMissNoFetch(IcpMessage query) throws IcpProtocolException {
      if (!query.isQuery()) {
        throw new IcpProtocolException(MISS_NOFETCH_NOT_QUERY_ERROR);
      }
      if (!(query instanceof LazyIcpMessageImpl))
        return super.makeMissNoFetch(query);
      IcpUtil.writeResponse(((LazyIcpMessageImpl)query).getByteBuffer(),
                            IcpMessage.ICP_OP_MISS_NOFETCH);
      return query;
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
   * <p>An implementation of {@link IcpEncoder} that attempts to
   * use fast mutable ICP messages.</p>
   * @author Thib Guicherd-Callin
   * @see LazyIcpMessageImpl
   */
  protected static class LazyIcpEncoderImpl extends IcpEncoderImpl {

    /* Inherit documentation */
    public DatagramPacket encode(IcpMessage message,
                                 IPAddr recipient,
                                 int port) {
      if (!(message instanceof LazyIcpMessageImpl))
        return super.encode(message, recipient, port);
      DatagramPacket packet = ((LazyIcpMessageImpl)message).getUdpPacket();
      packet.setLength(packet.getLength() - 4); // requester field gone
      packet.setAddress(recipient.getInetAddr());
      packet.setPort(port);
      return packet;
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
   * <p>A fast mutable implementation of {@link IcpMessage} that is
   * lazily parsed from, and acts directly upon, a UDP packet.</p>
   * @author Thib Guicherd-Callin
   */
  protected static class LazyIcpMessageImpl extends IcpMessageImpl {

    /**
     * <p>A byte buffer wrapped around the UDP packet's bytes.</p>
     */
    protected ByteBuffer byteBuffer;

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
     * <p>The UDP packet representing this message.</p>
     */
    protected DatagramPacket udpPacket;

    /**
     * <p>Builds a new lazily parsed ICP message based on
     * the given datagram packet.</p>
     * @param packet An ICP datagram packet.
     */
    protected LazyIcpMessageImpl(DatagramPacket packet) {
      super(); // builds a completely shallow message
      this.udpPacket = packet;
      this.byteBuffer = ByteBuffer.wrap(packet.getData());
    }

    /**
     * <p>Retrieves this message's underlying byte buffer.</p>
     * @return This message's byte buffer.
     */
    public ByteBuffer getByteBuffer() {
      return byteBuffer;
    }

    /* Inherit documentation */
    public short getLength() {
      if (!parsedLength) {
        length = IcpUtil.getLengthFromBuffer(byteBuffer);
        parsedLength = true;
      }
      return length;
    }

    /* Inherit documentation */
    public byte getOpcode() {
      if (!parsedOpcode) {
        opcode = IcpUtil.getOpcodeFromBuffer(byteBuffer);
        parsedOpcode = true;
      }
      return opcode;
    }

    /* Inherit documentation */
    public int getOptionData() {
      if (!parsedOptionData) {
        optionData = IcpUtil.getOptionDataFromBuffer(byteBuffer);
        parsedOptionData = true;
      }
      return optionData;
    }

    /* Inherit documentation */
    public int getOptions() {
      if (!parsedOptions) {
        options = IcpUtil.getOptionsFromBuffer(byteBuffer);
        parsedOptions = true;
      }
      return options;
    }

    /* Inherit documentation */
    public byte[] getPayloadObject() {
      if (!parsedPayloadObject) {
        if (getOpcode() == IcpMessage.ICP_OP_HIT_OBJ) {
          payloadObject = new byte[getPayloadObjectLength()];
          IcpUtil.getPayloadObjectFromBytes(udpPacket.getData(),
              getPayloadUrl(), getPayloadObjectLength(), payloadObject);
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
            IcpUtil.getPayloadObjectLengthFromBuffer(byteBuffer, getPayloadUrl());
        }
        parsedPayloadObjectLength = true;
      }
      return payloadObjectLength;
    }

    /* Inherit documentation */
    public String getPayloadUrl() {
      if (!parsedPayloadUrl) {
        if (getOpcode() == ICP_OP_HIT_OBJ) {
          payloadUrl = IcpUtil.getPayloadUrlFromBuffer(byteBuffer, false);
        }
        else {
          boolean isQuery = isQuery();
          payloadUrl = IcpUtil.getPayloadUrlFromBytes(udpPacket.getData(), isQuery,
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
          requester = IcpUtil.getRequesterFromBuffer(byteBuffer);
        }
        parsedRequester = true;
      }
      return requester;
    }

    /* Inherit documentation */
    public int getRequestNumber() {
      if (!parsedRequestNumber) {
        requestNumber = IcpUtil.getRequestNumberFromBuffer(byteBuffer);
        parsedRequestNumber = true;
      }
      return requestNumber;
    }

    /* Inherit documentation */
    public IPAddr getSender() {
      if (!parsedSender) {
        sender = IcpUtil.getSenderFromBuffer(byteBuffer);
        parsedSender = true;
      }
      return sender;
    }

    public DatagramPacket getUdpPacket() {
      return udpPacket;
    }

    /* Inherit documentation */
    public byte getVersion() {
      if (!parsedVersion) {
        version = IcpUtil.getVersionFromBuffer(byteBuffer);
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
      sb.append(";[call toLongString() for detailed string]]");
      return sb.toString();
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
   * fast mutable ICP messages based directly on a UDP packet.</p>
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

  /**
   * <p>Cannot instantiate instances of this class; use static methods
   * instead to obtain service.</p>
   * @see #getInstance
   */
  protected LazyIcpFactoryImpl() {}

  /* Inherit documentation */
  public synchronized IcpBuilder makeIcpBuilder() {
    if (singletonBuilder == null) {
      singletonBuilder = new LazyIcpBuilderImpl();
    }
    return singletonBuilder;
  }

  /* Inherit documentation */
  public synchronized IcpDecoder makeIcpDecoder() {
    if (singletonDecoder == null) {
      singletonDecoder = new LazyIcpDecoderImpl();
    }
    return singletonDecoder;
  }

  /* Inherit documentation */
  public synchronized IcpEncoder makeIcpEncoder() {
    if (singletonEncoder == null) {
      singletonEncoder = new LazyIcpEncoderImpl();
    }
    return singletonEncoder;
  }

  /**
   * <p>A singleton instance of {@link IcpFactoryImpl}.</p>
   */
  private static IcpFactory singleton;

  /**
   * <p>A singleton instance of {@link LazyIcpBuilderImpl}.</p>
   */
  private static IcpBuilder singletonBuilder;

  /**
   * <p>A singleton instance of {@link LazyIcpDecoderImpl}.</p>
   */
  private static IcpDecoder singletonDecoder;

  /**
   * <p>A singleton instance of {@link LazyIcpEncoderImpl}.</p>
   */
  private static IcpEncoder singletonEncoder;

  /**
   * <p>Gets an instance of type {@link IcpFactory}.</p>
   * @return An ICP factory.
   */
  public synchronized static IcpFactory getInstance() {
    if (singleton == null) {
      singleton = new LazyIcpFactoryImpl();
    }
    return singleton;
  }

}
