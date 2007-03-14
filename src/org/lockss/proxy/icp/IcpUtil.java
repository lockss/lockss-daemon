/*
 * $Id: IcpUtil.java,v 1.9 2007-03-14 23:39:41 thib_gc Exp $
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

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;

import org.lockss.util.*;

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
      + message.getPayloadUrl().length(); // URL
    if (message.isQuery()) {
      result += 4; // requester field
    }
    if (message.getOpcode() == IcpMessage.ICP_OP_HIT_OBJ) {
      byte[] obj = message.getPayloadObject();
      result += (obj.length + 2); // paylod object length field
    }
    return (short)result;
  }

  /**
   * <p>Reads an IP address from the given byte buffer.</p>
   * @param in     A byte buffer.
   * @param offset A byte offset.
   * @return The IP address obtained from the 4 bytes at the given
   *         offset of the argument buffer.
   * @throws IcpException if any exception arises.
   */
  public static IPAddr getIpFromBuffer(ByteBuffer in, int offset)
      throws IcpException {
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
      throw new IcpException(
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
   */
  public static void getPayloadObjectFromBytes(byte[] data,
                                               String payloadUrl,
                                               short payloadObjectLength,
                                               byte[] destination) {
    System.arraycopy(data,
                     OFFSET_PAYLOAD_NONQUERY + payloadUrl.length() + 3,
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
    return in.getShort(OFFSET_PAYLOAD_NONQUERY + payloadUrl.length() + 1);
  }

  /**
   * <p>Extracts the payload URL from an ICP buffer.</p>
   * <p>This method is needed only to decode ICP packets whose opcode
   * is {@link IcpMessage#ICP_OP_HIT_OBJ}. Because the length of the
   * URL string cannot be inferred from the length field in that case,
   * this method has to iterate byte by byte until the null terminator,
   * which is not very efficient. In contrast, for all other already
   * decoded ICP messages and for all ICP packets whose opcode is not
   * {@link IcpMessage#ICP_OP_HIT_OBJ}, the length of the URL string can
   * be computed by {@link #stringLength} and therefore it is much
   * better to call {@link #getPayloadUrlFromBytes} instead.</p>
   * @param in      An ICP buffer.
   * @param isQuery True if and only if the ICP message is a query.
   * @return The ICP buffer's payload URL.
   * @see #getPayloadUrlFromBytes
   */
  public static String getPayloadUrlFromBuffer(ByteBuffer in, boolean isQuery) {
    int offset = isQuery ? OFFSET_PAYLOAD_QUERY : OFFSET_PAYLOAD_NONQUERY;
    StringBuffer buffer = new StringBuffer();
    byte one;
    while ( (one = in.get(offset)) != (byte)0 ) {
      buffer.append((char)one);
      offset++;
    }
    return buffer.toString();
  }

  /**
   * <p>Lifts the payload URL from an ICP byte buffer.</p>
   * <p>This method cannot be used in the case of an undecoded
   * message of type {@link IcpMessage#ICP_OP_HIT_OBJ}, because in
   * that case it is not possible to infer the length of the URL
   * string directly; use m{@link #getPayloadUrlFromBuffer} instead,
   * which is not very efficient.</p>
   * @param bytes        An ICP byte buffer.
   * @param isQuery      True if and only if the ICP message is a query.
   * @param stringLength The length of the URL string.
   * @return The ICP message's payload URL.
   * @see #stringLength(short, boolean, boolean, short)
   */
  public static String getPayloadUrlFromBytes(byte[] bytes,
                                              boolean isQuery,
                                              int stringLength) {
    try {
      return new String(
          bytes,
          isQuery ? OFFSET_PAYLOAD_QUERY : OFFSET_PAYLOAD_NONQUERY,
          stringLength,
          Constants.URL_ENCODING);
    }
    catch (UnsupportedEncodingException uee) {
      // This should never happen; US-ASCII is guaranteed to be available
      throw new RuntimeException(uee);
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
    catch (IcpException ipe) {
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
    catch (IcpException ipe) {
      throw new IndexOutOfBoundsException(ipe.getMessage());
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
   * <p>Computes the length of the URL string in an ICP message.</p>
   * @param lengthField       The length field of the message.
   * @param isQuery           True if and only if the message is a query.
   * @param isHitObj          True if and only if the message is a
   *                          hit-object.
   * @param hitObjLengthField If <code>isHitObj</code> is true, the
   *                          hit object length field of the message.
   *                          Ignored otherwise (<code>(short)0</code>
   *                          is appropriate).
   * @return The length of the message's URL string, excluding its null
   *         terminator.
   */
  public static int stringLength(short lengthField,
                                 boolean isQuery,
                                 boolean isHitObj,
                                 short hitObjLengthField) {
    return   lengthField
           - (isQuery ? 25 : 21) // incl. 1 for null terminator
           - (isHitObj ? hitObjLengthField + 2 : 0); // incl. 2 for obj length field
  }

}
