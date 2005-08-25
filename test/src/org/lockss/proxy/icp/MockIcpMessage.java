/*
 * $Id: MockIcpMessage.java,v 1.1 2005-08-25 20:12:38 thib_gc Exp $
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

import java.io.UnsupportedEncodingException;
import java.net.*;
import java.util.ArrayList;

public abstract class MockIcpMessage implements IcpMessage {

  /*
   * begin static inner class
   * ========================
   */
  private static class MockIcpQueryMessage extends MockIcpMessage {
    public short getLength() { return (short)(4 + super.getLength()); }
    public byte getOpcode() { return ICP_OP_QUERY; }
    public InetAddress getRequester() { return getStandardRequester(); }
  }
  /*
   * end static inner class
   * ======================
   */
  
  /*
   * begin static inner class
   * ========================
   */
  private static abstract class MockIcpResponseMessage extends MockIcpMessage {
    public boolean containsSrcRttResponse() { return true; }
    public int getOptionData() { return getStandardSrcRttResponse(); }
    public int getOptions() { return ICP_FLAG_SRC_RTT; }
    public short getSrcRttResponse() { return getStandardSrcRttResponse(); }
  }
  /*
   * end static inner class
   * ======================
   */
  
  /*
   * begin static inner class
   * ========================
   */
  private static class TestPair {
    private IcpMessage message;
    private DatagramPacket packet;
    public TestPair(IcpMessage message, DatagramPacket packet) {
      this.message = message;
      this.packet = packet;
    }
  }
  /*
   * end static inner class
   * ======================
   */
  
  public boolean containsSrcRttResponse() {
    return false;
  }
  
  public short getLength() {
    try {
      return (short)(  20  // for the header
                     + getStandardQueryUrl().toString().getBytes("US-ASCII").length
                     + 1); // for the null terminator
    }
    catch (UnsupportedEncodingException uee) {
      throw new RuntimeException("Runtime exception while creating mock URL "
          + getStandardQueryUrl().toString());
    }
  }

  public int getOptionData() {
    return 0;
  }

  public int getOptions() {
    return 0;
  }

  public byte[] getPayloadObject() {
    return null;
  }
  
  public short getPayloadObjectLength() {
    byte[] payload = getPayloadObject();
    if (getOpcode() == ICP_OP_HIT_OBJ && payload != null) {
      return (short)payload.length;
    }
    else {
      return (short)0;
    }
  }
  public URL getPayloadUrl() {
    return getStandardQueryUrl();
  }
  public InetAddress getRequester() {
    return null;
  }
  
  public int getRequestNumber() {
    return 0xdeadbeef;
  }
  
  public InetAddress getSender() {
    return getStandardSender();
  }

  public short getSrcRttResponse() {
    return 0;
  }

  public InetAddress getUdpAddress() {
    return getStandardUdpAddress();
  }

  public int getUdpPort() {
    return getStandardUdpPort();
  }

  public byte getVersion() {
    return (byte)2; // version 2
  }

  public boolean requestsHitObj() {
    return false;
  }
  
  public boolean requestsSrcRtt() {
    return false;
  }

  public void setUdpAddress(InetAddress udpAddress) {
    // nothing
  }

  public void setUdpPort(int port) {
    // nothing
  }

  private static InetAddress standardDestination;

  private static final byte[] standardPayloadData;

  private static URL standardQueryUrl;

  private static InetAddress standardRequester;

  private static InetAddress standardSender;

  private static InetAddress standardUdpAddress;

  private static final int standardUdpPort = 3128;

  private static ArrayList testPairs;
  
  /*
   * begin static block
   * ==================
   */
  static {
    try {
      standardSender = makeAddress(1, 2, 3, 4);
      standardUdpAddress = makeAddress(4, 3, 2, 1);
      standardRequester = makeAddress(11, 12, 13, 14);
      standardDestination = makeAddress(111, 112, 113, 114);
      standardQueryUrl = makeUrl("http://www.stanford.edu/");
      standardPayloadData = 
        "<html><head><title>Sample</title></head><body><p>Test</p></body></html>"
        .getBytes("US-ASCII");
    }
    catch (Exception exc) {
      throw new RuntimeException("Runtime exception while initializing "
          + MockIcpMessage.class.getName(), exc);
    }
  }
  /*
   * end static block
   * ================
   */
  
  /*
   * begin static block
   * ==================
   */
  static {
    try {
      testPairs = new ArrayList();
      DatagramPacket packet;

      IcpMessage[] messages = new IcpMessage[] {
          query(),
          queryRequestSrcRtt(),
          queryRequestHitObj(),
          queryRequestSrcRttRequestHitObj(),
          new MockIcpMessage() {
            public byte getOpcode() { return ICP_OP_HIT; }
          },
          new MockIcpResponseMessage() {
            public byte getOpcode() { return ICP_OP_HIT; }
          },
          new MockIcpMessage() {
            public byte getOpcode() { return ICP_OP_MISS; }
          },
          new MockIcpResponseMessage() {
            public byte getOpcode() { return ICP_OP_MISS; }
          },
          new MockIcpMessage() {
            public byte getOpcode() { return ICP_OP_MISS_NOFETCH; }
          },
          new MockIcpResponseMessage() {
            public byte getOpcode() { return ICP_OP_MISS_NOFETCH; }
          },
          new MockIcpMessage() {
            public short getLength() { return (short)(2 + getPayloadObjectLength() + super.getLength()); }
            public byte getOpcode() { return ICP_OP_HIT_OBJ; }
            public byte[] getPayloadObject() { return getStandardPayloadData(); }
            public short getPayloadObjectLength() { return (short)getPayloadObject().length; }
            
          },
          new MockIcpResponseMessage() {
            public short getLength() { return (short)(2 + getPayloadObjectLength() + super.getLength()); }
            public byte getOpcode() { return ICP_OP_HIT_OBJ; }
            public byte[] getPayloadObject() { return getStandardPayloadData(); }
            public short getPayloadObjectLength() { return (short)getPayloadObject().length; }
          },
          new MockIcpMessage() {
            public byte getOpcode() { return ICP_OP_ERR; }
          },
          new MockIcpMessage() {
            public byte getOpcode() { return ICP_OP_DENIED; }
          }
      };
      for (int ii = 0 ; ii < messages.length ; ii++) {
        packet = new DatagramPacket(makePacketBytes(messages[ii]),
                                    messages[ii].getLength(),
                                    getStandardDestination(),
                                    ICP_PORT);
        testPairs.add(new TestPair(messages[ii], packet));
      }
      
    }
    catch (Exception exc) {
      throw new RuntimeException("Runtime exception while initializing "
          + MockIcpMessage.class.getName(), exc);
    }
  }
  /*
   * end static block
   * ================
   */

  public static int countTestPairs() {
    // TODO Auto-generated method stub
    return testPairs.size();
  }
  
  public static InetAddress getStandardDestination() {
    return standardDestination;
  }
  
  public static byte[] getStandardPayloadData() {
    return standardPayloadData;
  }

  public static URL getStandardQueryUrl() {
    return standardQueryUrl;
  }
  
  public static InetAddress getStandardRequester() {
    return standardRequester;
  }
  
  public static InetAddress getStandardSender() {
    return standardSender;
  }

  public static short getStandardSrcRttResponse() {
    return (short)12345;
  }

  public static InetAddress getStandardUdpAddress() {
    return standardUdpAddress;
  }

  public static int getStandardUdpPort() {
    return standardUdpPort;
  }

  public static IcpMessage getTestMessage(int nth) {
    return ((TestPair)testPairs.get(nth)).message;
  }
  
  public static DatagramPacket getTestPacket(int nth) {
    return ((TestPair)testPairs.get(nth)).packet;
  }

  public static IcpMessage invalid() {
    return new MockIcpMessage() {
      public byte getOpcode() { return ICP_OP_INVALID; }
    };
  }
  
  public static IcpMessage query() {
    return new MockIcpQueryMessage();
  }
  
  public static IcpMessage queryRequestHitObj() {
    return new MockIcpQueryMessage() {
      public int getOptions() { return ICP_FLAG_HIT_OBJ; }
      public boolean requestsHitObj() { return true; }
    };    
  }
  
  public static IcpMessage queryRequestSrcRtt() {
    return new MockIcpQueryMessage() {
      public int getOptions() { return ICP_FLAG_SRC_RTT; }
      public boolean requestsSrcRtt() { return true; }
    };
  }
  
  public static IcpMessage queryRequestSrcRttRequestHitObj() {
    return new MockIcpQueryMessage() {
      public int getOptions() { return ICP_FLAG_HIT_OBJ | ICP_FLAG_SRC_RTT; }
      public boolean requestsHitObj() { return true; }
      public boolean requestsSrcRtt() { return true; }
    };    
  }

  private static InetAddress makeAddress(int i1, int i2, int i3, int i4) {
    try {
      return InetAddress.getByAddress(
          new byte[] { (byte)i1, (byte)i2, (byte)i3, (byte)i4});
    }
    catch (UnknownHostException uhe) {
      final String dot = ".";
      StringBuffer buffer = new StringBuffer();
      buffer.append("Could not create mock IP address [");
      buffer.append(Integer.toString(i1));
      buffer.append(dot);
      buffer.append(Integer.toString(i2));
      buffer.append(dot);
      buffer.append(Integer.toString(i3));
      buffer.append(dot);
      buffer.append(Integer.toString(i4));
      buffer.append("].");
      throw new RuntimeException(buffer.toString(), uhe);
    }
  }
  
  private static byte[] makePacketBytes(IcpMessage message)
      throws Exception {
    byte[] ret = new byte[message.getLength()];    
    int ii = 0;
    
    // Basic payload
    ret[ii++] = message.getOpcode();
    ret[ii++] = message.getVersion();
    for (int jj = 8 ; jj >= 0 ; jj -= 8) {
      ret[ii++] = (byte)(message.getLength() >>> jj);      
    }
    for (int jj = 24 ; jj >= 0 ; jj -= 8) {
      ret[ii++] = (byte)(message.getRequestNumber() >>> jj);      
    }
    for (int jj = 24 ; jj >= 0 ; jj -= 8) {
      ret[ii++] = (byte)(message.getOptions() >>> jj);      
    }
    for (int jj = 24 ; jj >= 0 ; jj -= 8) {
      ret[ii++] = (byte)(message.getOptionData() >>> jj);      
    }
    for (int jj = 0 ; jj < 4 ; jj++) {
      ret[ii++] = message.getSender().getAddress()[jj];
    }
    if (message.getOpcode() == ICP_OP_QUERY) {
      for (int jj = 0 ; jj < 4 ; jj++) {
        ret[ii++] = message.getRequester().getAddress()[jj];
      }
    }
    
    // Payload URL
    byte[] urlBytes = message.getPayloadUrl().toString().getBytes("US-ASCII");
    System.arraycopy(urlBytes, 0, ret, ii, urlBytes.length);
    ii += urlBytes.length;
    ret[ii++] = (byte)0;
    
    // Payload object
    if (message.getOpcode() == ICP_OP_HIT_OBJ) {
      for (int jj = 8 ; jj >= 0 ; jj -= 8) {
        ret[ii++] = (byte)(message.getPayloadObjectLength() >>> jj);
      }
      System.arraycopy(message.getPayloadObject(), 0, ret,
                       ii, message.getPayloadObjectLength());
      ii += message.getPayloadObjectLength();
    }
    
    return ret;
  }
  
  private static URL makeUrl(String url) {
    try {
      return new URL(url);
    }
    catch (MalformedURLException mue) {
      StringBuffer buffer = new StringBuffer();
      buffer.append("Could not create mock URL [");
      buffer.append(url);
      buffer.append("]");
      throw new RuntimeException(buffer.toString(), mue);
    }
  }

}
