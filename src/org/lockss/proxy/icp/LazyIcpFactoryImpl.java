/*
 * $Id: LazyIcpFactoryImpl.java,v 1.4 2007-03-14 23:39:41 thib_gc Exp $
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
import java.nio.ByteBuffer;

import org.lockss.util.IPAddr;

/**
 * <p>An implementation of {@link IcpFactory} that produces mutable
 * ICP messages based directly on an underlyin UDP packet.</p>
 * <p>Currently, the LOCKSS daemon uses this implementation when
 * <code>org.lockss.proxy.icp.slow</code> is set to
 * <code>true</code>.</p>
 * @author Thib Guicherd-Callin
 */
public class LazyIcpFactoryImpl extends BaseIcpFactory {

  /*
   * begin PROTECTED STATIC NESTED CLASS
   * ===================================
   */
  /**
   * <p>A mutable implementation of {@link IcpMessage} based directly
   * on an underlyin UDP packet.</p>
   * @author Thib Guicherd-Callin
   */
  protected static class LazyIcpMessageImpl extends BaseIcpMessage {

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
      super(new IPAddr(packet.getAddress()), packet.getPort());
      this.udpPacket = packet;
      this.byteBuffer = ByteBuffer.wrap(packet.getData());
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

    /* Inherit documentation */
    public byte getVersion() {
      if (!parsedVersion) {
        version = IcpUtil.getVersionFromBuffer(byteBuffer);
        parsedVersion = true;
      }
      return version;
    }

    /* Inherit documentation */
    public IcpMessage makeDenied() throws IcpException {
      if (!isQuery()) {
        throw new IcpException(DENIED_NOT_QUERY_ERROR);
      }
      writeResponse(IcpMessage.ICP_OP_DENIED);
      return this;
    }

    /* Inherit documentation */
    public IcpMessage makeError() throws IcpException {
      if (!isQuery()) {
        throw new IcpException(ERR_NOT_QUERY_ERROR);
      }
      writeResponse(IcpMessage.ICP_OP_ERR);
      return this;
    }

    /* Inherit documentation */
    public IcpMessage makeHit() throws IcpException {
      if (!isQuery()) {
        throw new IcpException(HIT_NOT_QUERY_ERROR);
      }
      writeResponse(IcpMessage.ICP_OP_HIT);
      return this;
    }

    /**
     * <p>Not supported; throws an {@link UnsupportedOperationException}.</p>
     */
    public IcpMessage makeHit(short srcRttResponse) throws IcpException {
      throw new UnsupportedOperationException(NOT_IMPLEMENTED_ERROR);
    }

    /**
     * <p>Not supported; throws an {@link UnsupportedOperationException}.</p>
     */
    public IcpMessage makeHitObj(byte[] payloadObject) throws IcpException {
      throw unsupported();
    }

    /**
     * <p>Not supported; throws an {@link UnsupportedOperationException}.</p>
     */
    public IcpMessage makeHitObj(short srcRttResponse, byte[] payloadObject) throws IcpException {
      throw unsupported();
    }

    /**
     * <p>Not supported; throws an {@link UnsupportedOperationException}.</p>
     */
    public IcpMessage makeMiss() throws IcpException {
      throw unsupported();
    }

    /**
     * <p>Not supported; throws an {@link UnsupportedOperationException}.</p>
     */
    public IcpMessage makeMiss(short srcRttResponse) throws IcpException {
      throw unsupported();
    }

    /* Inherit documentation */
    public IcpMessage makeMissNoFetch() throws IcpException {
      if (!isQuery()) {
        throw new IcpException(MISS_NOFETCH_NOT_QUERY_ERROR);
      }
      writeResponse(IcpMessage.ICP_OP_MISS_NOFETCH);
      return this;
    }

    /**
     * <p>Not supported; throws an {@link UnsupportedOperationException}.</p>
     */
    public IcpMessage makeMissNoFetch(short srcRttResponse) throws IcpException {
      throw new UnsupportedOperationException(NOT_IMPLEMENTED_ERROR);
    }

    /* Inherit documentation */
    public DatagramPacket toDatagramPacket(IPAddr recipient, int port) {
      udpPacket.setLength(udpPacket.getLength() - 4); // requester field gone
      udpPacket.setAddress(recipient.getInetAddr());
      udpPacket.setPort(port);
      return udpPacket;
    }

    /**
     * <p>Returns a detailed string representation of this message
     * (<strong>warning: causes the entire message to be parsed;
     * the benefits of lazy parsing are lost after a call to this
     * method</strong>).</p>
     * @return A detailed string representation of this message, as
     *         returned by the parent <code>toString()</code> method.
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
      sb.append("]");
      return sb.toString();
    }

    /**
     * <p>Alters an ICP query byte buffer so that it becomes an ICP
     * response byte buffer with the specified opcode.</p>
     * @param opcode     The opcode of the resultuing ICP response.
     */
    public void writeResponse(byte opcode) {
      // Modify fields
      this.opcode = opcode;
      this.options = 0;
      this.optionData = 0;
      this.sender = ZERO_ADDRESS;
      byteBuffer.put(IcpUtil.OFFSET_BYTE_OPCODE, this.opcode);
      byteBuffer.putInt(IcpUtil.OFFSET_INT_OPTIONS, this.options);
      byteBuffer.putInt(IcpUtil.OFFSET_INT_OPTIONDATA, this.optionData);
      byteBuffer.putInt(IcpUtil.OFFSET_INT_SENDER, 0);

      // Shift URL string
      byte[] bytes = byteBuffer.array();
      short length = IcpUtil.getLengthFromBuffer(byteBuffer);
      int urlLength = IcpUtil.stringLength(length, true, false, (short)0);
      System.arraycopy(bytes, 24, bytes, 20, urlLength + 1);

      /*
       * Add 4 null bytes. Illustration:
       *
       * Index:            length-9 length-8 length-7 length-6 length-5 length-4 length-3 length-2 length-1
       * Before arraycopy:    a        b        c        d        .        c        o        m        \0
       * After arraycopy:     .        c        o        m        \0       c        o        m        \0
       * Desired result:      .        c        o        m        \0       \0       \0       \0       \0
       *                                                          ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
       */
      byteBuffer.putInt(length - 5, 0);

      // Shorten message
      this.length = (short)(length - 4);
      byteBuffer.putShort(IcpUtil.OFFSET_SHORT_LENGTH, this.length);
    }

  }
  /*
   * end PROTECTED STATIC NESTED CLASS
   * =================================
   */

  private LazyIcpFactoryImpl() {}

  /* Inherit documentation */
  public IcpMessage makeMessage(DatagramPacket udpPacket)
      throws IcpException {
    return new LazyIcpMessageImpl(udpPacket);
  }

  /**
   * <p>Not supported; throws an {@link UnsupportedOperationException}.</p>
   */
  public IcpMessage makeQuery(IPAddr requesterAddress,
                              String query,
                              boolean requestSrcRtt,
                              boolean requestHitObj) {
    throw unsupported();
  }

  /**
   * <p>An error message used for ICP operations that are not
   * supported by this class.</p>
   */
  protected static final String NOT_IMPLEMENTED_ERROR = "Not implemented";

  private static IcpFactory singleton;

  public static synchronized IcpFactory getInstance() {
    if (singleton == null) {
      singleton = new LazyIcpFactoryImpl();
    }
    return singleton;
  }

  /**
   * <p>Returns an exception for errors related to ICP operations not
   * supported by this class.</p>
   * @return A new {@link UnsupportedOperationException}.
   * @see #NOT_IMPLEMENTED_ERROR
   */
  protected static UnsupportedOperationException unsupported() {
    return new UnsupportedOperationException(NOT_IMPLEMENTED_ERROR);
  }

}
