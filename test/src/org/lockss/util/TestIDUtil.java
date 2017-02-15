/*
 * $Id$
 */

/*

Copyright (c) 2000-2003 Board of Trustees of Leland Stanford Jr. University,
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

import org.lockss.test.LockssTestCase;
import java.io.*;
import org.lockss.test.*;

public class TestIDUtil extends LockssTestCase {
  
  public void setUp() throws Exception {
    super.setUp();
  }
  
  public void tearDown() throws Exception {
    super.tearDown();
  }
  
  public void testEncodeAndDecodePort() throws Exception {
    String port, decodedPort;
    byte[] encodedPort;

    // Test 0x0000
    port = "0";
    encodedPort = IDUtil.encodeTCPPort(port);
    decodedPort = IDUtil.decodeTCPPort(encodedPort);
    assertEquals(encodedPort, new byte[]{(byte)0x00, (byte)0x00});
    assertEquals(port, decodedPort);

    // Test 0x0001
    port = "1";
    encodedPort = IDUtil.encodeTCPPort(port);
    decodedPort = IDUtil.decodeTCPPort(encodedPort);
    assertEquals(encodedPort, new byte[]{(byte)0x00, (byte)0x01});
    assertEquals(port, decodedPort);

    // Test 0x00ff
    port = "255";
    encodedPort = IDUtil.encodeTCPPort(port);
    decodedPort = IDUtil.decodeTCPPort(encodedPort);
    assertEquals(encodedPort, new byte[]{(byte)0x00, (byte)0xff});
    assertEquals(port, decodedPort);

    // Test 0x0100
    port = "256";
    encodedPort = IDUtil.encodeTCPPort(port);
    decodedPort = IDUtil.decodeTCPPort(encodedPort);
    assertEquals(encodedPort, new byte[]{(byte)0x01, (byte)0x00});
    assertEquals(port, decodedPort);
    
    // Test 0xff00
    port = "65280";
    encodedPort = IDUtil.encodeTCPPort(port);
    decodedPort = IDUtil.decodeTCPPort(encodedPort);
    assertEquals(encodedPort, new byte[]{(byte)0xff, (byte)0x00});
    assertEquals(port, decodedPort);

    // Test 0xffff
    port = "65535";
    encodedPort = IDUtil.encodeTCPPort(port);
    decodedPort = IDUtil.decodeTCPPort(encodedPort);
    assertEquals(encodedPort, new byte[]{(byte)0xff, (byte)0xff});
    assertEquals(port, decodedPort);
  }

  public void testEncodeAndDecodeAddr() throws Exception {
    String addr, decodedAddr;
    byte[] encodedAddr;
    
    // Test 0x00000000
    addr = "0.0.0.0";
    encodedAddr = IDUtil.encodeTCPAddr(addr);
    decodedAddr = IDUtil.decodeTCPAddr(encodedAddr);
    assertEquals(encodedAddr, new byte[]{(byte)0x00, (byte)0x00,
                                         (byte)0x00, (byte)0x00});
    assertEquals(decodedAddr, addr);

    // Test 0x00000001
    addr = "0.0.0.1";
    encodedAddr = IDUtil.encodeTCPAddr(addr);
    decodedAddr = IDUtil.decodeTCPAddr(encodedAddr);
    assertEquals(encodedAddr, new byte[]{(byte)0x00, (byte)0x00,
                                         (byte)0x00, (byte)0x01});
    assertEquals(decodedAddr, addr);
   
    // Test 0x000000ff
    addr = "0.0.0.255";
    encodedAddr = IDUtil.encodeTCPAddr(addr);
    decodedAddr = IDUtil.decodeTCPAddr(encodedAddr);
    assertEquals(encodedAddr, new byte[]{(byte)0x00, (byte)0x00,
                                         (byte)0x00, (byte)0xff});
    assertEquals(decodedAddr, addr);

    // Test 0x0000ff00
    addr = "0.0.255.0";
    encodedAddr = IDUtil.encodeTCPAddr(addr);
    decodedAddr = IDUtil.decodeTCPAddr(encodedAddr);
    assertEquals(encodedAddr, new byte[]{(byte)0x00, (byte)0x00,
                                         (byte)0xff, (byte)0x00});
    assertEquals(decodedAddr, addr);

    // Test 0x00ff0000
    addr = "0.255.0.0";
    encodedAddr = IDUtil.encodeTCPAddr(addr);
    decodedAddr = IDUtil.decodeTCPAddr(encodedAddr);
    assertEquals(encodedAddr, new byte[]{(byte)0x00, (byte)0xff,
                                         (byte)0x00, (byte)0x00});
    assertEquals(decodedAddr, addr);

    // Test 0xff000000
    addr = "255.0.0.0";
    encodedAddr = IDUtil.encodeTCPAddr(addr);
    decodedAddr = IDUtil.decodeTCPAddr(encodedAddr);
    assertEquals(encodedAddr, new byte[]{(byte)0xff, (byte)0x00,
                                         (byte)0x00, (byte)0x00});
    assertEquals(decodedAddr, addr);
  }
  
  public void testEncodeAndDecodeKey() throws Exception {
    String key, decodedKey;
    byte[] encodedKey;
    ByteArrayInputStream bis;
    DataInputStream dis;

    key = "TCP:[0.0.0.0]:0";
    encodedKey = IDUtil.encodeTCPKey(key);
    assertEquals(8, encodedKey.length);
    // In practice, the protocol byte is stripped off before reading.
    dis = new DataInputStream(new ByteArrayInputStream(encodedKey));
    dis.readByte();
    // The rest is decoded.
    decodedKey = IDUtil.decodeTCPKey(dis);
    assertEquals(key, decodedKey);

    key = "TCP:[0.0.0.1]:0";
    encodedKey = IDUtil.encodeTCPKey(key);
    assertEquals(8, encodedKey.length);
    // In practice, the protocol byte is stripped off before reading.
    dis = new DataInputStream(new ByteArrayInputStream(encodedKey));
    dis.readByte();
    // The rest is decoded.
    decodedKey = IDUtil.decodeTCPKey(dis);
    assertEquals(key, decodedKey);

    key = "TCP:[0.0.0.1]:255";
    encodedKey = IDUtil.encodeTCPKey(key);
    assertEquals(8, encodedKey.length);
    // In practice, the protocol byte is stripped off before reading.
    dis = new DataInputStream(new ByteArrayInputStream(encodedKey));
    dis.readByte();
    // The rest is decoded.
    decodedKey = IDUtil.decodeTCPKey(dis);
    assertEquals(key, decodedKey);

    key = "TCP:[0.0.0.255]:256";
    encodedKey = IDUtil.encodeTCPKey(key);
    assertEquals(8, encodedKey.length);
    // In practice, the protocol byte is stripped off before reading.
    dis = new DataInputStream(new ByteArrayInputStream(encodedKey));
    dis.readByte();
    // The rest is decoded.
    decodedKey = IDUtil.decodeTCPKey(dis);
    assertEquals(key, decodedKey);

    key = "TCP:[0.0.255.255]:1234";
    encodedKey = IDUtil.encodeTCPKey(key);
    assertEquals(8, encodedKey.length);
    // In practice, the protocol byte is stripped off before reading.
    dis = new DataInputStream(new ByteArrayInputStream(encodedKey));
    dis.readByte();
    // The rest is decoded.
    decodedKey = IDUtil.decodeTCPKey(dis);
    assertEquals(key, decodedKey);

    key = "TCP:[192.168.105.105]:9723";
    encodedKey = IDUtil.encodeTCPKey(key);
    assertEquals(8, encodedKey.length);
    // In practice, the protocol byte is stripped off before reading.
    dis = new DataInputStream(new ByteArrayInputStream(encodedKey));
    dis.readByte();
    // The rest is decoded.
    decodedKey = IDUtil.decodeTCPKey(dis);
    assertEquals(key, decodedKey);

    key = "TCP:[10.1.5.208]:65535";
    encodedKey = IDUtil.encodeTCPKey(key);
    assertEquals(8, encodedKey.length);
    // In practice, the protocol byte is stripped off before reading.
    dis = new DataInputStream(new ByteArrayInputStream(encodedKey));
    dis.readByte();
    // The rest is decoded.
    decodedKey = IDUtil.decodeTCPKey(dis);
    assertEquals(key, decodedKey);

  }
  
  public void testEncodeTCPAddrIllegalValueThrows() throws Exception {
    try {
      // 256 is an illegal tuple
      IDUtil.encodeTCPAddr("192.168.0.256");
      fail("Should have thrown IdentityParseException");
    } catch (IdentityParseException ex) {
      ; // Expected.
    }
    
    try {
      // -1 is an illegal tuple
      IDUtil.encodeTCPAddr("-1.168.0.256");
      fail("Should have thrown IdentityParseException");
    } catch (IdentityParseException ex) {
      ; // Expected.
    }
    
    try {
      // Not enough bytes
      IDUtil.encodeTCPAddr("192.168.0");
      fail("Should have thrown IdentityParseException");
    } catch (IdentityParseException ex) {
      ; // Expected.
    }

    try {
      // Too many bytes
      IDUtil.encodeTCPAddr("192.168.1.0.3");
      fail("Should have thrown IdentityParseException");
    } catch (IdentityParseException ex) {
      ; // Expected.
    }
    
    try {
      // Not a number
      IDUtil.encodeTCPAddr("x.168.1.0.3");
      fail("Should have thrown IdentityParseException");
    } catch (IdentityParseException ex) {
      ; // Expected.
    }
  }
  
  public void testEncodeTCPPortIllegalValueThrows() throws Exception {
    try {
      // Too small
      IDUtil.encodeTCPPort("-1");
      fail("Should have thrown IdentityParseException");
    } catch (IdentityParseException ex) {
      ; // Expected.
    }
    
    try {
      // Too big
      IDUtil.encodeTCPPort("65536");
      fail("Should have thrown IdentityParseException");
    } catch (IdentityParseException ex) {
      ; // Expected.
    }

    try {
      // Not a number
      IDUtil.encodeTCPPort("xyz");
      fail("Should have thrown IdentityParseException");
    } catch (IdentityParseException ex) {
      ; // Expected.
    }
  }

  public void testCorruptCRCThrows() throws Exception {
    String key = "TCP:[192.168.105.105]:9723";
    byte[] encodedKey = IDUtil.encodeTCPKey(key);

    // Sanity check for the test.
    assertNotEquals(encodedKey[7], (byte)0x00);
    
    // Fiddle with the CRC
    encodedKey[7] = (byte)0x00;
    
    try {
      IDUtil.decodeOneKey(new DataInputStream(new ByteArrayInputStream(encodedKey)));
      // Should throw.
      fail("Should have thrown IdentityParseException");
    } catch (IdentityParseException ex) {
      ; // Expected
    }
  }
  
  
}
