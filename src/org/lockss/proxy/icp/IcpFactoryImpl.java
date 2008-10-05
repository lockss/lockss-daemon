/*
 * $Id: IcpFactoryImpl.java,v 1.21 2007-03-14 23:39:41 thib_gc Exp $
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

import java.io.*;
import java.net.DatagramPacket;
import java.nio.ByteBuffer;

import org.lockss.util.*;

/**
 * <p>An implementation of {@link IcpFactory} that produces immutable
 * ICP messages.</p>
 * <p>Currently, the LOCKSS daemon uses this implementation unless
 * <code>org.lockss.proxy.icp.slow</code> is set to
 * <code>false</code>.</p>
 * @author Thib Guicherd-Callin
 */
public class IcpFactoryImpl extends BaseIcpFactory {

  /*
   * begin PROTECTED STATIC NESTED CLASS
   * ===================================
   */
  /**
   * <p>An immutable implementation of {@link IcpMessage}.</p>
   * @author Thib Guicherd-Callin
   */
  protected static class IcpMessageImpl extends BaseIcpMessage {

    /**
     * <p>Builds an ICP query message by invoking
     * <code>IcpMessageImpl#IcpMessageImpl(byte, byte, short, int, int, int, IPAddr, String)}</code>
     * and initializing the requester field with the given
     * argument.</p>
     * @param udpAddress    This message's UDP address.
     * @param udpPort       This message's UDP port.
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
    protected IcpMessageImpl(IPAddr udpAddress,
                             int udpPort,
                             byte opcode,
                             byte version,
                             short length,
                             int requestNumber,
                             int options,
                             int optionData,
                             IPAddr sender,
                             IPAddr requester,
                             String payloadUrl) {
      this(udpAddress,
           udpPort,
           opcode,
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
     * @param udpAddress    This message's UDP address.
     * @param udpPort       This message's UDP port.
     * @param opcode        An ICP opcode.
     * @param version       A version number.
     * @param length        An ICP message length.
     * @param requestNumber An opaque request number.
     * @param options       A raw options integer.
     * @param optionData    A raw options data integer.
     * @param sender        A sender address.
     * @param payloadUrl    A payload URL.
     */
    protected IcpMessageImpl(IPAddr udpAddress,
                             int udpPort,
                             byte opcode,
                             byte version,
                             short length,
                             int requestNumber,
                             int options,
                             int optionData,
                             IPAddr sender,
                             String payloadUrl) {
      super(udpAddress, udpPort);
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
     * <code>IcpMessageImpl(byte, byte, short, int, int, int, IPAddr, IPAddr, String)}</code>
     * and by initializing the
     * payload object and payload object length fields with the given
     * arguments.</p>
     * @param udpAddress          This message's UDP address.
     * @param udpPort             This message's UDP port.
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
    protected IcpMessageImpl(IPAddr udpAddress,
                             int udpPort,
                             byte opcode,
                             byte version,
                             short length,
                             int requestNumber,
                             int options,
                             int optionData,
                             IPAddr sender,
                             String payloadUrl,
                             byte[] payloadObject,
                             short payloadObjectLength) {
      this(udpAddress,
           udpPort,
           opcode,
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
    public IcpMessage makeDenied()
        throws IcpException {
      if (!isQuery()) {
        throw new IcpException(DENIED_NOT_QUERY_ERROR);
      }
      return new IcpMessageImpl(getUdpAddress(),
                                getUdpPort(),
                                IcpMessage.ICP_OP_DENIED,
                                IcpMessage.ICP_VERSION,
                                getLength(),
                                getRequestNumber(),
                                0,
                                0,
                                ZERO_ADDRESS,
                                getPayloadUrl());
    }

    /* Inherit documentation */
    public IcpMessage makeError()
        throws IcpException {
      if (!isQuery()) {
        throw new IcpException(ERR_NOT_QUERY_ERROR);
      }
      return new IcpMessageImpl(getUdpAddress(),
                                getUdpPort(),
                                IcpMessage.ICP_OP_ERR,
                                IcpMessage.ICP_VERSION,
                                getLength(),
                                getRequestNumber(),
                                0,
                                0,
                                ZERO_ADDRESS,
                                getPayloadUrl());
    }

    /* Inherit documentation */
    public IcpMessage makeHit()
        throws IcpException {
      if (!isQuery()) {
        throw new IcpException(HIT_NOT_QUERY_ERROR);
      }
      return new IcpMessageImpl(getUdpAddress(),
                                getUdpPort(),
                                IcpMessage.ICP_OP_HIT,
                                IcpMessage.ICP_VERSION,
                                (short)(getLength() - 4),
                                getRequestNumber(),
                                0,
                                0,
                                ZERO_ADDRESS,
                                getPayloadUrl());
    }

    /* Inherit documentation */
    public IcpMessage makeHit(short srcRttResponse)
        throws IcpException {
      if (!isQuery()) {
        throw new IcpException(HIT_NOT_QUERY_ERROR);
      }
      if (!requestsSrcRtt()) {
        throw new IcpException(SRC_RTT_NOT_REQUESTED_ERROR);
      }
      return new IcpMessageImpl(getUdpAddress(),
                                getUdpPort(),
                                IcpMessage.ICP_OP_HIT,
                                IcpMessage.ICP_VERSION,
                                (short)(getLength() - 4),
                                getRequestNumber(),
                                IcpMessage.ICP_FLAG_SRC_RTT,
                                (int)srcRttResponse,
                                ZERO_ADDRESS,
                                getPayloadUrl());
    }

    /* Inherit documentation */
    public IcpMessage makeHitObj(byte[] payloadObject)
        throws IcpException {
      if (payloadObject == null) {
        throw new NullPointerException(NULL_PAYLOAD_OBJECT_ERROR);
      }
      if (!isQuery()) {
        throw new IcpException(HIT_OBJ_NOT_QUERY_ERROR);
      }
      return new IcpMessageImpl(getUdpAddress(),
                                getUdpPort(),
                                IcpMessage.ICP_OP_HIT_OBJ,
                                IcpMessage.ICP_VERSION,
                                (short)(getLength() - 2 + payloadObject.length),
                                getRequestNumber(),
                                IcpMessage.ICP_FLAG_HIT_OBJ,
                                0,
                                ZERO_ADDRESS,
                                getPayloadUrl(),
                                payloadObject,
                                (short)payloadObject.length);
    }

    /* Inherit documentation */
    public IcpMessage makeHitObj(short srcRttResponse,
                                 byte[] payloadObject)
        throws IcpException {
      if (payloadObject == null) {
        throw new NullPointerException(NULL_PAYLOAD_OBJECT_ERROR);
      }
      if (!isQuery()) {
        throw new IcpException(HIT_OBJ_NOT_QUERY_ERROR);
      }
      if (!requestsSrcRtt()) {
        throw new IcpException(SRC_RTT_NOT_REQUESTED_ERROR);
      }
      return new IcpMessageImpl(getUdpAddress(),
                                getUdpPort(),
                                IcpMessage.ICP_OP_HIT_OBJ,
                                IcpMessage.ICP_VERSION,
                                (short)(getLength() - 2 + payloadObject.length),
                                getRequestNumber(),
                                IcpMessage.ICP_FLAG_HIT_OBJ | IcpMessage.ICP_FLAG_SRC_RTT,
                                (int)srcRttResponse, ZERO_ADDRESS,
                                getPayloadUrl(),
                                payloadObject,
                                (short)payloadObject.length);
    }

    /* Inherit documentation */
    public IcpMessage makeMiss()
        throws IcpException {
      if (!isQuery()) {
        throw new IcpException(MISS_NOT_QUERY_ERROR);
      }
      return new IcpMessageImpl(getUdpAddress(),
                                getUdpPort(),
                                IcpMessage.ICP_OP_MISS,
                                IcpMessage.ICP_VERSION,
                                (short)(getLength() - 4),
                                getRequestNumber(),
                                0,
                                0,
                                ZERO_ADDRESS,
                                getPayloadUrl());
    }

    /* Inherit documentation */
    public IcpMessage makeMiss(short srcRttResponse)
        throws IcpException {
      if (!isQuery()) {
        throw new IcpException(MISS_NOT_QUERY_ERROR);
      }
      if (!requestsSrcRtt()) {
        throw new IcpException(SRC_RTT_NOT_REQUESTED_ERROR);
      }
      return new IcpMessageImpl(getUdpAddress(),
                                getUdpPort(),
                                IcpMessage.ICP_OP_MISS,
                                IcpMessage.ICP_VERSION,
                                (short)(getLength() - 4),
                                getRequestNumber(),
                                IcpMessage.ICP_FLAG_SRC_RTT,
                                (int)srcRttResponse,
                                ZERO_ADDRESS,
                                getPayloadUrl());
    }

    /* Inherit documentation */
    public IcpMessage makeMissNoFetch()
        throws IcpException {
      if (!isQuery()) {
        throw new IcpException(MISS_NOFETCH_NOT_QUERY_ERROR);
      }
      return new IcpMessageImpl(getUdpAddress(),
                                getUdpPort(),
                                IcpMessage.ICP_OP_MISS_NOFETCH,
                                IcpMessage.ICP_VERSION,
                                (short)(getLength() - 4),
                                getRequestNumber(),
                                0,
                                0,
                                ZERO_ADDRESS,
                                getPayloadUrl());
    }

    /* Inherit documentation */
    public IcpMessage makeMissNoFetch(short srcRttResponse)
        throws IcpException {
      if (!isQuery()) {
        throw new IcpException(MISS_NOFETCH_NOT_QUERY_ERROR);
      }
      if (!requestsSrcRtt()) {
        throw new IcpException(SRC_RTT_NOT_REQUESTED_ERROR);
      }
      return new IcpMessageImpl(getUdpAddress(),
                                getUdpPort(),
                                IcpMessage.ICP_OP_MISS_NOFETCH,
                                IcpMessage.ICP_VERSION,
                                (short)(getLength() - 4),
                                getRequestNumber(),
                                IcpMessage.ICP_FLAG_SRC_RTT,
                                srcRttResponse,
                                ZERO_ADDRESS,
                                getPayloadUrl());
    }

    /* Inherit documentation */
    public DatagramPacket toDatagramPacket(IPAddr recipient,
                                           int port) {
      byte[] rawBytes;

      try {
        int length = IcpUtil.computeLength(this);
        ByteBuffer out =
          ByteBuffer.allocate(length);

        // Write out header
        out.put(IcpUtil.OFFSET_BYTE_OPCODE, getOpcode());
        out.put(IcpUtil.OFFSET_BYTE_VERSION, getVersion());
        out.putShort(IcpUtil.OFFSET_SHORT_LENGTH, (short)length);
        out.putInt(IcpUtil.OFFSET_INT_REQUESTNUMBER, getRequestNumber());
        out.putInt(IcpUtil.OFFSET_INT_OPTIONS, getOptions());
        out.putInt(IcpUtil.OFFSET_INT_OPTIONDATA, getOptionData());
        rawBytes = getSender().getAddress();
        for (int off = 0 ; off < 4 ; ++off) {
          out.put(IcpUtil.OFFSET_INT_SENDER + off, rawBytes[off]);
        }

        // Write out payload
        final int OFFSET_PAYLOAD;
        if (isQuery()) {
          rawBytes = getRequester().getAddress();
          for (int off = 0 ; off < 4 ; ++off) {
            out.put(IcpUtil.OFFSET_INT_REQUESTER + off, rawBytes[off]);
          }
          OFFSET_PAYLOAD = IcpUtil.OFFSET_PAYLOAD_QUERY;
        }
        else {
          OFFSET_PAYLOAD = IcpUtil.OFFSET_PAYLOAD_NONQUERY;
        }
        rawBytes = getPayloadUrl().getBytes(Constants.URL_ENCODING);
        for (int off = 0 ; off < rawBytes.length ; ++off) {
          out.put(OFFSET_PAYLOAD + off, rawBytes[off]);
        }
        out.put(OFFSET_PAYLOAD + rawBytes.length, (byte)0); // null terminator
        if (getOpcode() == IcpMessage.ICP_OP_HIT_OBJ) {
          final int OFFSET_SHORT_PAYLOADLENGTH =
            IcpUtil.OFFSET_PAYLOAD_NONQUERY + rawBytes.length + 1;
          final int OFFSET_PAYLOADOBJECT =
            OFFSET_SHORT_PAYLOADLENGTH + 2;
          rawBytes = getPayloadObject();
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
   * end PROTECTED STATIC NESTED CLASS
   * =================================
   */

  /**
   * <p>Prevents the instantiation of this class.</p>
   */
  private IcpFactoryImpl() {}

  /* Inherit documentation */
  public IcpMessage makeMessage(DatagramPacket udpPacket)
      throws IcpException {
    return parseIcp(udpPacket);
  }

  /* Inherit documentation */
  public IcpMessage makeQuery(IPAddr requesterAddress,
                              String query,
                              boolean requestSrcRtt,
                              boolean requestHitObj) {
    try {
      return new IcpMessageImpl(null,
                                -1,
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
      throw new RuntimeException("Could not create byte array from String", uee);
    }
  }

  /**
   * <p>A singleton instance of this class.</p>
   */
  private static IcpFactory singleton;

  /**
   * <p>Obtains an instance of this class.</p>
   * @return An instance of {@link IcpFactoryImpl}.
   */
  public static synchronized IcpFactory getInstance() {
    if (singleton == null) {
      singleton = new IcpFactoryImpl();
    }
    return singleton;
  }

  /**
   * <p>Parses a UDP packet into an ICP message.</p>
   * @param packet A UDP packet.
   * @return An ICP message.
   * @throws IcpException
   */
  private static IcpMessage parseIcp(DatagramPacket packet)
      throws IcpException {

    //  Local variables
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
    IPAddr udpAddress = new IPAddr(packet.getAddress());
    int udpPort = packet.getPort();

    try {
      // Unconditional processing
      opcode = IcpUtil.getOpcodeFromBuffer(in);
      if (!IcpUtil.isValidOpcode(opcode)) {
        throw new IcpException("Invalid opcode: " + opcode);
      }
      version = IcpUtil.getVersionFromBuffer(in);
      if (version != IcpMessage.ICP_VERSION) {
        throw new IcpException("Unknown version: " + version);
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
          ret = new IcpMessageImpl(udpAddress,
                                   udpPort,
                                   opcode,
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
          ret = new IcpMessageImpl(udpAddress,
              udpPort,
              opcode,
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
            ret = new IcpMessageImpl(udpAddress,
                                     udpPort,
                                     opcode,
                                     version,
                                     length,
                                     requestNumber,
                                     options,
                                     optionData,
                                     sender,
                                     payloadUrl);
            break;

      }

      return ret;
    }
    catch (IcpException ipe) {
      throw ipe; // rethrow
    }
    catch (Exception exc) {
      throw new IcpException("Error while decoding ICP packet", exc);
    }
  }

}
