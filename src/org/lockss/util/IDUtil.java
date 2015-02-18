/*
 * $Id$
 *

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

package org.lockss.util;

import java.util.*;
import java.io.*;
import org.lockss.protocol.*;

public class IDUtil {
  // Length of the Protocol field of stored keys, in bytes. 
  private static final short PROTOCOL_LEN = 1;
  private static final short CRC_LEN = 1;
  
  // Length of the TCP Address data, in bytes.
  private static final short TCP_ADDR_LEN = 4;
  // Length of the TCP port data, in bytes.
  private static final short TCP_PORT_LEN = 2;
  
  // Only the TCP protocol used by V3 is currently supported.
  private static final byte PROTOCOL_TCP = (byte)0x00;
  
  // CRC-8 utility.  Must be reset with Crc8.reset() before each use.
  private static final Crc8 crc = new Crc8();

  private static final Logger log = Logger.getLogger("IDUtil");

  private static final String V3_TCP_KEY_PREFIX =
    IdentityManager.V3_ID_PROTOCOL_TCP +
    IdentityManager.V3_ID_PROTOCOL_SUFFIX +
    IdentityManager.V3_ID_TCP_ADDR_PREFIX;

  private static final String V3_TCP_KEY_MIDDLE =
    IdentityManager.V3_ID_TCP_ADDR_SUFFIX +
    IdentityManager.V3_ID_TCP_IP_PORT_SEPARATOR;

  public static String ipAddrToKey(String addr, int port) {
    if (port == 0) {
      // V1 key is IPAddr
      return addr;
    }
    return ipAddrToKey(addr, String.valueOf(port));
  }

  public static String ipAddrToKey(String addr, String port) {
    // V3 key is TCP:[ip]:port
    return V3_TCP_KEY_PREFIX + addr + V3_TCP_KEY_MIDDLE + port;
  }

  public static String ipAddrToKey(IPAddr addr, int port) {
    return ipAddrToKey(addr.getHostAddress(), port);
  }
  
  /**
   * Read and decode one identity key from the supplied stream.
   * If a recoverable IdentityParseException occurs, this method will
   * try to leave the stream at the start of the next key, but there
   * is no guarantee.
   * 
   * @return The decoded key, or null if at the end of the stream.
   * @throws IdentityParseException if the key cannot be parsed.
   */
  public static String decodeOneKey(DataInputStream dis)
      throws IdentityParseException {
    // Read the protocol byte off the input stream.
    try {
      byte protocol = dis.readByte();
      
      switch (protocol) {
      case PROTOCOL_TCP:
        return decodeTCPKey(dis);
      default:
        // Unhandled protocol, or malformed data.  There isn't a good way
        // to recover from this, because without knowing what type of record
        // we're trying to read we can't seek to the next record position,
        // in the general case.
        log.error("Unexpected protocol header: " + protocol);
        throw new IdentityParseException("Unexpected protocol header " +
                                         protocol);
      }
    } catch (EOFException ex) {
      // Return null.
      log.debug2("End of agreement history file reached, returning null");
      return null;
    } catch (IOException ex) {
      throw new IdentityParseException("Cannot read from agreement history file.");
    }
  }

  /**
   * Return a byte array representing the supplied key.  Used for encoding
   * TCP peer identity.
   * 
   * @param key The peer identity key to encode.
   * @return A byte array representation of the key.
   * @throws IdentityParseException if the key cannot be parsed.
   */
  public synchronized static byte[] encodeTCPKey(String key) 
      throws IdentityParseException {
    
    byte[] encodedKey = new byte[PROTOCOL_LEN + TCP_ADDR_LEN +
                                 TCP_PORT_LEN + CRC_LEN];

    try {
      int protEndIdx = key.indexOf(IDUtil.V3_TCP_KEY_PREFIX);
      int addrStartIdx = protEndIdx + V3_TCP_KEY_PREFIX.length();
      int addrEndIdx = key.indexOf(V3_TCP_KEY_MIDDLE);
      int portStartIdx = addrEndIdx + V3_TCP_KEY_MIDDLE.length();
      
      byte[] addrBytes = encodeTCPAddr(key.substring(addrStartIdx, addrEndIdx));
      byte[] portBytes = encodeTCPPort(key.substring(portStartIdx, key.length()));

      encodedKey[0] = PROTOCOL_TCP;
      
      for (int i = 0; i < addrBytes.length; i++) {
        encodedKey[i+1] = addrBytes[i];
      }
      
      for (int i = 0; i < portBytes.length; i++) {
        encodedKey[i+5] = portBytes[i];
      }

      crc.reset();
      crc.update(addrBytes);
      crc.update(portBytes);

      encodedKey[7] = crc.getValue();

    } catch (IdentityParseException ex) {
      throw ex;
    } catch (Exception ex) {
      throw new IdentityParseException("Unable to parse key: " + key);
    }
    
    return encodedKey;
  }

  /** 
   * Read and return a single TCP key from the input stream.
   * The byte-format of a TCP key is:
   *   
   *    4 Bytes:  Address
   *    2 Bytes:  Port
   *    1 Byte:  CRC-8 checksum
   */
  synchronized static String decodeTCPKey(DataInputStream is) 
      throws IOException, IdentityParseException {
    byte[] addr = new byte[TCP_ADDR_LEN];
    byte[] port = new byte[TCP_PORT_LEN];
    
    is.read(addr);
    is.read(port);
    byte checkByte = is.readByte();

    crc.reset();
    crc.update(addr);
    crc.update(port);

    if (crc.getValue() != checkByte) {
      throw new IdentityParseException("Bad CRC in TCP address: " +
                                       checkByte + " found, needed " + 
                                       crc.getValue());
    }

    StringBuffer key =  new StringBuffer(V3_TCP_KEY_PREFIX);
    key.append(decodeTCPAddr(addr));
    key.append(V3_TCP_KEY_MIDDLE);
    key.append(decodeTCPPort(port));
    
    return key.toString();
  }
  
  /* Encode a single TCP address into an array of bytes */
  static byte[] encodeTCPAddr(String addr)
      throws IdentityParseException {

    String[] tuples = addr.split("\\.");

    if (tuples.length != 4) {
      throw new IdentityParseException("Cannot parse address " + addr);
    }

    byte[] results = new byte[4];

    try {
      int a = Integer.parseInt(tuples[0]);
      int b = Integer.parseInt(tuples[1]);
      int c = Integer.parseInt(tuples[2]);
      int d = Integer.parseInt(tuples[3]);

      // If any tuple is > 255, it's not valid.
      if (a < 0 || a > 255 || b < 0 || b > 255 || 
          c < 0 || c > 255 || d < 0 || d > 255) {
        throw new IdentityParseException("Cannot parse address " + addr);
      }

      results[0] = (byte)(a & 0xff);
      results[1] = (byte)(b & 0xff);
      results[2] = (byte)(c & 0xff);
      results[3] = (byte)(d & 0xff);

    } catch (NumberFormatException ex) {
      throw new IdentityParseException("Cannot parse address " + addr);
    }

    return results;
  }
  
  /* Decode a single TCP address from an array of bytes */
  static String decodeTCPAddr(byte[] addrBytes)
      throws IdentityParseException {
    if (addrBytes.length != TCP_ADDR_LEN) {
      throw new IdentityParseException("Address must be exactly four bytes.");
    }

    int a = 0xff & (byte)(addrBytes[0]);
    int b = 0xff & (byte)(addrBytes[1]);
    int c = 0xff & (byte)(addrBytes[2]);
    int d = 0xff & (byte)(addrBytes[3]);
    
    return Integer.toString(a) + "." + Integer.toString(b) + "." + 
           Integer.toString(c) + "." + Integer.toString(d);
  }

  static byte[] encodeTCPPort(String port)
      throws IdentityParseException {
   
    byte[] portBytes = new byte[TCP_PORT_LEN];

    try {
      int p = Integer.parseInt(port);
      if (p < 0 || p > 65535) {
        throw new IdentityParseException("Cannot parse port " + port);
      }

      // Mask low byte
      portBytes[1] = (byte)(p & 0x00ff);
      // Mask the high byte
      portBytes[0] = (byte)((p & 0xff00) >> 8);

    } catch (NumberFormatException ex) {
      throw new IdentityParseException("Cannot parse port " + port);
    }

    return portBytes;
  }

  static String decodeTCPPort(byte[] portBytes)
      throws IdentityParseException {
    
    if (portBytes.length != TCP_PORT_LEN) {
      throw new IdentityParseException("Port must be exactly two bytes.");
    }
    
    int val = 0;
    
    val += portBytes[1] & 0xff;
    val += (portBytes[0] & 0xff) << 8;

    return Integer.toString(val);
  }
}