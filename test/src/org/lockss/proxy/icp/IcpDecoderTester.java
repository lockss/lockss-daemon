/*
 * $Id: IcpDecoderTester.java,v 1.2 2005-09-08 01:24:41 thib_gc Exp $
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

import org.lockss.proxy.icp.IcpDecoder;
import org.lockss.proxy.icp.IcpDecoder.Factory;

import junit.framework.TestCase;

/**
 * <p>Tests classes that implement {@link IcpDecoder}.</p>
 * @author Thib Guicherd-Callin
 */
public abstract class IcpDecoderTester extends TestCase {

  /**
   * <p>An ICP decoder factory.</p>
   */
  private Factory factory;

  /* Inherit documentation */
  public void setUp() {
    this.factory = makeFactory();
  }
  
  /**
   * <p>Tests decoding.</p>
   * @throws Exception if an error occurs.
   */
  public void testDecoding() throws Exception {
    IcpDecoder decoder = factory.makeIcpDecoder();
    
    for (int test = 0 ; test < MockIcpMessage.countTestPairs(); test++) {
      DatagramPacket packet = MockIcpMessage.getTestPacket(test);
      packet.setAddress(MockIcpMessage.getStandardUdpAddress());
      IcpMessage message = decoder.parseIcp(packet);      
      expect(MockIcpMessage.getTestMessage(test), message);
    }
  }
  
  /**
   * <p>Produces an ICP decoder factory that produces ICP decoders of
   * the class under consideration.</p>
   * @return An ICP decoder factory.
   */
  protected abstract Factory makeFactory();
  
  /**
   * <p>Asserts that the argument message has desired properties with
   * respect to the expected message.</p>
   * @param expected An expected message.
   * @param packet   An actual message.
   */
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
