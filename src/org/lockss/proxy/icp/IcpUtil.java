/*
 * $Id: IcpUtil.java,v 1.5 2005-11-21 21:32:48 thib_gc Exp $
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

import java.nio.ByteBuffer;

import org.lockss.util.IPAddr;

/**
 * <p>Provides utility methods to deal with ICP.</p>
 * @author Thib Guicherd-Callin
 */
public class IcpUtil {

  /**
   * <p>The byte offset of the opcode field.</p>
   */
  static final int OFFSET_BYTE_OPCODE = 0;

  /**
   * <p>The byte offset of the version field.</p>
   */
  static final int OFFSET_BYTE_VERSION = 1;

  /**
   * <p>The byte offset of the option data field.</p>
   */
  static final int OFFSET_INT_OPTIONDATA = 12;

  /**
   * <p>The byte offset of the options field.</p>
   */
  static final int OFFSET_INT_OPTIONS = 8;

  /**
   * <p>The byte offset of the requester field.</p>
   * <p>The requester field only makes sense if the ICP message is a
   * query.</p>
   */
  static final int OFFSET_INT_REQUESTER = 20;

  /**
   * <p>The byte offset of the request number field.</p>
   */
  static final int OFFSET_INT_REQUESTNUMBER = 4;

  /**
   * <p>The byte offset of the sender field.</p>
   */
  static final int OFFSET_INT_SENDER = 16;

  /**
   * <p>The byte offset of the payload field, when the ICP message is
   * not a query.</p>
   */
  static final int OFFSET_PAYLOAD_NONQUERY = 20;

  /**
   * <p>The byte offset of the payload field, when the ICP message is
   * a query.</p>
   */
  static final int OFFSET_PAYLOAD_QUERY = 24;

  /**
   * <p>The byte offset of the length field.</p>
   */
  static final int OFFSET_SHORT_LENGTH = 2;

  /**
   * <p>An array of valid RFC2186 opcode, for fast lookup.</p>
   * @see #isValidOpcode
   */
  private static final boolean[] validOpcode = {
    false, // 00: ICP_OP_INVALID (named by RFC but not valid, obviously)
    true,  // 01: ICP_OP_QUERY
    true,  // 02: ICP_OP_HIT
    true,  // 03: ICP_OP_MISS
    true,  // 04: ICP_OP_ERR
    false,
    false,
    false,
    false,
    false,
    true,  // 10: ICP_OP_SECHO
    true,  // 11: ICP_OP_DECHO
    false,
    false,
    false,
    false,
    false,
    false,
    false,
    false,
    false,
    true,  // 21: ICP_OP_MISS_NOFETCH
    true,  // 22: ICP_OP_DENIED
    true   // 23: ICP_OP_HIT_OBJ
  };

  /**
   * <p>Computes the number of bytes corresponding to a given ICP
   * message.</p>
   * @param message An ICP message.
   * @return The number of bytes the ICP message represents.
   */
  public static short computeLength(IcpMessage message) {
    int result =
      21 // 20-byte header + null terminator
      + stringLength(message.getPayloadUrl()); // URL
    if (message.isQuery()) {
      result += 4; // requester field
    }
    if (message.getOpcode() == IcpMessage.ICP_OP_HIT_OBJ) {
      byte[] obj = message.getPayloadObject();
      result += (obj.length + 2);
    }
    return (short)result;
  }

  /**
   * <p>Reads an IP address from the given byte buffer.</p>
   * @param in     A byte buffer.
   * @param offset A byte offset.
   * @return The IP address obtained from the 4 bytes at the given
   *         offset of the argument buffer.
   * @throws IcpProtocolException if any exception arises.
   */
  public static IPAddr getIpFromBuffer(ByteBuffer in, int offset)
      throws IcpProtocolException {
    try {
      byte[] ipBytes = new byte[4];
      int rawIpInt = in.getInt(offset);
      for (int ii = ipBytes.length-1 ; ii >= 0 ; --ii) {
        ipBytes[ii] = (byte)(rawIpInt & 0x000000ff);
        rawIpInt >>>= 8;
      }
      return IPAddr.getByAddress(ipBytes);
    }
    catch (Exception exc) {
      throw new IcpProtocolException(
          "Error while parsing IP from byte buffer", exc);
    }
  }

  /**
   * <p>Extracts the length field from an ICP buffer.</p>
   * @param in A byte buffer.
   * @return The ICP buffer's length field.
   */
  public static short getLengthFromBuffer(ByteBuffer in) {
    return in.getShort(OFFSET_SHORT_LENGTH);
  }

  /**
   * <p>Extracts the opcode field from an ICP buffer.</p>
   * @param in A byte buffer.
   * @return The ICP buffer's opcode field.
   */
  public static byte getOpcodeFromBuffer(ByteBuffer in) {
    return in.get(IcpUtil.OFFSET_BYTE_OPCODE);
  }

  /**
   * <p>Extracts the option data field from an ICP buffer.</p>
   * @param in A byte buffer.
   * @return The ICP buffer's option data field.
   */
  public static int getOptionDataFromBuffer(ByteBuffer in) {
    return in.getInt(IcpUtil.OFFSET_INT_OPTIONDATA);
  }

  /**
   * <p>Extracts the options field from an ICP buffer.</p>
   * @param in A byte buffer.
   * @return The ICP buffer's options field.
   */
  public static int getOptionsFromBuffer(ByteBuffer in) {
    return in.getInt(IcpUtil.OFFSET_INT_OPTIONS);
  }

  /**
   * <p>Extracts the payload object from raw ICP bytes.</p>
   * @param data                A byte array.
   * @param payloadUrl          The payload URL.
   * @param payloadObjectLength The length of the payload object.
   * @param destination         A destination array.
   * @return The ICP byte buffer's payload object.
   */
  public static void getPayloadObjectFromBytes(byte[] data,
                                               String payloadUrl,
                                               short payloadObjectLength,
                                               byte[] destination) {
    System.arraycopy(data,
                     OFFSET_PAYLOAD_NONQUERY + stringLength(payloadUrl) + 3,
                     destination,
                     0,
                     payloadObjectLength);
  }

  /**
   * <p>Extracts the payload object length field from an ICP buffer.</p>
   * @param in A byte buffer.
   * @return The ICP buffer's payload object length field.
   */
  public static short getPayloadObjectLengthFromBuffer(ByteBuffer in,
                                                       String payloadUrl) {
    return in.getShort(OFFSET_PAYLOAD_NONQUERY + stringLength(payloadUrl) + 1);
  }

  /**
   * <p>Extracts the payload URL from an ICP buffer.</p>
   * @param in      A byte buffer.
   * @param isQuery True if and only if the ICP message is a query.
   * @return The ICP buffer's payload URL.
   */
  public static String getPayloadUrlFromBuffer(ByteBuffer in,
                                               boolean isQuery) {
    try {
      return getUrlFromBuffer(
          in, isQuery ? OFFSET_PAYLOAD_QUERY : OFFSET_PAYLOAD_NONQUERY);
    }
    catch (IcpProtocolException ipe) {
      throw new IndexOutOfBoundsException(ipe.getMessage());
    }
  }

  /**
   * <p>Extracts the requester field from a query ICP buffer.</p>
   * @param in A query ICP buffer.
   * @return The ICP buffer's requester field.
   */
  public static IPAddr getRequesterFromBuffer(ByteBuffer in) {
    try {
      return getIpFromBuffer(in, OFFSET_INT_REQUESTER);
    }
    catch (IcpProtocolException ipe) {
      throw new IndexOutOfBoundsException(ipe.getMessage());
    }
  }

  /**
   * <p>Extracts the request number field from an ICP buffer.</p>
   * @param in A byte buffer.
   * @return The ICP buffer's request number field.
   */
  public static int getRequestNumberFromBuffer(ByteBuffer in) {
    return in.getInt(IcpUtil.OFFSET_INT_REQUESTNUMBER);
  }

  /**
   * <p>Extracts the sender field from an ICP buffer.</p>
   * @param in A byte buffer.
   * @return The ICP buffer's sender field.
   */
  public static IPAddr getSenderFromBuffer(ByteBuffer in) {
    try {
      return getIpFromBuffer(in, OFFSET_INT_SENDER);
    }
    catch (IcpProtocolException ipe) {
      throw new IndexOutOfBoundsException(ipe.getMessage());
    }
  }

  /**
   * <p>Reads a null-terminated URL from the given byte buffer.</p>
   * @param in     A byte buffer.
   * @param offset A byte offset.
   * @return A URL string obtained from reading bytes from the
   *         argument buffer, starting at the given offset.
   * @throws IcpProtocolException if any exception arises.
   */
  public static String getUrlFromBuffer(ByteBuffer in, int offset)
      throws IcpProtocolException {
    try {
      StringBuffer buffer = new StringBuffer();
      byte one;
      while ( (one = in.get(offset)) != (byte)0 ) {
        buffer.append((char)one);
        offset++;
      }
      return buffer.toString();
    }
    catch (Exception exc) {
      throw new IcpProtocolException(
          "Error while parsing URL from byte buffer", exc);
    }
  }

  /**
   * <p>Extracts the version field from an ICP buffer.</p>
   * @param in A byte buffer.
   * @return The ICP buffer's version field.
   */
  public static byte getVersionFromBuffer(ByteBuffer in) {
    return in.get(IcpUtil.OFFSET_BYTE_VERSION);
  }

  /**
   * <p>Determines if an opcode is meaningful.</p>
   * @param opcode A potential opcode.
   * @return False if the opcode is unused in the ICP specification,
   *         version 2 (RFC2186); true otherwise.
   * @see #validOpcode
   */
  public static boolean isValidOpcode(int opcode) {
    try {
      return validOpcode[opcode];
    }
    catch (IndexOutOfBoundsException ioobe) {
      return false;
    }
  }

  /**
   * <p>Computes the length of a {@link String} with the assumption
   * it was created byte by byte.</p>
   * @param str A string.
   * @return The length of the strong's underlying array of characters.
   */
  public static int stringLength(String str) {
    return str.toCharArray().length;
  }

}
