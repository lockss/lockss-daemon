/*
 * $Id: IcpDecoderTester.java,v 1.1 2005-08-25 20:12:38 thib_gc Exp $
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
import java.util.Arrays;

import junit.framework.TestCase;

public abstract class IcpDecoderTester extends TestCase {

  private IcpDecoderFactory factory;
  
  protected abstract IcpDecoderFactory makeFactory();
  
  public void setUp() {
    this.factory = makeFactory();
  }
  
  public void testDecoding() throws Exception {
    IcpDecoder decoder = factory.makeIcpDecoder();
    
    for (int test = 0 ; test < MockIcpMessage.countTestPairs(); test++) {
      DatagramPacket packet = MockIcpMessage.getTestPacket(test);
      packet.setAddress(MockIcpMessage.getStandardUdpAddress());
      IcpMessage message = decoder.parseIcp(packet);      
      expect(MockIcpMessage.getTestMessage(test), message);
    }
  }
  
  private static void expect(IcpMessage expected, IcpMessage message) {
    assertEquals(expected.getOpcode(), message.getOpcode());
    assertEquals(expected.getVersion(), message.getVersion());
    assertEquals(expected.getLength(), message.getLength());
    assertEquals(expected.getRequestNumber(), message.getRequestNumber());
    assertEquals(expected.getOptions(), message.getOptions());
    assertEquals(expected.getOptionData(), message.getOptionData());
    assertEquals(expected.getSender(), message.getSender());
    assertEquals(expected.getPayloadUrl(), message.getPayloadUrl());
    switch (expected.getOpcode()) {
      case IcpMessage.ICP_OP_QUERY:
        assertEquals(expected.getRequester(), message.getRequester());
        break;
      case IcpMessage.ICP_OP_HIT_OBJ:
        assertEquals(expected.getPayloadObjectLength(),
                     message.getPayloadObjectLength());
        assertTrue(Arrays.equals(expected.getPayloadObject(),
                                 message.getPayloadObject()));
        break;
    }

    assertEquals(expected.getUdpAddress(), message.getUdpAddress());
    return;
  }
  
}
