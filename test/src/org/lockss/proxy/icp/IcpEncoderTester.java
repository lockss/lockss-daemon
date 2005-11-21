/*
 * $Id: IcpEncoderTester.java,v 1.7 2005-11-21 21:38:24 thib_gc Exp $
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

import org.lockss.proxy.icp.IcpEncoder;
import org.lockss.proxy.icp.IcpEncoder.Factory;
import org.lockss.test.LockssTestCase;
import org.lockss.util.Logger;

/**
 * <p>Tests classes that implement {@link IcpEncoder}.</p>
 * @author Thib Guicherd-Callin
 */
public abstract class IcpEncoderTester extends LockssTestCase {

  /**
   * <p>An ICP encoder factory.</p>
   */
  private Factory factory;

  /* Inherit documentation */
  public void setUp() {
    this.factory = makeFactory();
  }

  /**
   * <p>Tests encoding.</p>
   * @throws Exception if an error occurs.
   */
  public void testEncoding() throws Exception {
    IcpEncoder encoder = factory.makeIcpEncoder();

    for (int test = 0 ; test < MockIcpMessage.countTestPairs(); test++) {
      logger.info("testEncoding: begin test #" + test);
      IcpMessage message = MockIcpMessage.getTestMessage(test);
      DatagramPacket packet =
        encoder.encode(message,
                       MockIcpMessage.getStandardDestination(),
                       MockIcpMessage.getStandardUdpPort());
      expect(MockIcpMessage.getTestPacket(test), packet);
      logger.info("testEncoding: PASSED test #" + test);
    }
  }

  /**
   * <p>Produces an ICP encoder factory that produces ICP encoders of
   * the class under consideration.</p>
   * @return An ICP encoder factory.
   */
  protected abstract Factory makeFactory();

  /**
   * <p>A logger for use by these tests.</p>
   */
  private static final Logger logger = Logger.getLogger("IcpEncoderTester");

  /**
   * <p>Asserts that the argument packet has desired properties with
   * respect to the expected packet.</p>
   * @param expected An expected packet.
   * @param packet   An actual packet.
   */
  private static void expect(DatagramPacket expected, DatagramPacket packet) {
    assertEquals(expected.getAddress(), packet.getAddress());
    assertEquals(expected.getPort(), packet.getPort());
    assertEquals(expected.getData(), packet.getData());
  }

}
