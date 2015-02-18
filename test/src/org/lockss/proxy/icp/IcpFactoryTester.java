/*
 * $Id$
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

import junit.framework.AssertionFailedError;

import org.lockss.test.LockssTestCase;
import org.lockss.util.*;

/**
 * <p>A tester base class for implementors of {@link IcpFactory}.</p>
 * @author Thib Guicherd-Callin
 */
public abstract class IcpFactoryTester extends LockssTestCase {

  /**
   * <p>The ICP factory being tested.</p>
   */
  private IcpFactory icpFactory;

  /* Inherit documentation */
  public void setUp() {
    this.icpFactory = makeIcpFactory();
  }

  /**
   * <p>Tests {@link IcpFactory#makeMessage}.</p>
   * @throws Exception if an unexpected error occurs.
   */
  public void testMakeMessage() throws Exception {
    int failed = 0;
    logger.warning("Numbered tests are counted from zero.");

    for (int test = 0 ; test < MockIcpMessage.countTestPairs(); test++) {
      try {
        logger.info("testMakeMessage: begin test #" + test);
        DatagramPacket packet = MockIcpMessage.getTestPacket(test);
        IcpMessage message = icpFactory.makeMessage(packet);
        expectMessage(MockIcpMessage.getTestMessage(test), message);
        logger.info("testMakeMessage: PASSED test #" + test);
      }
      catch (IcpException ipe) {
        logger.error("testMakeMessage: FAILED test #" + test, ipe);
        ++failed;
      }
      catch (AssertionFailedError afe) {
        logger.error("testMakeMessage: FAILED test #" + test, afe);
        ++failed;
      }
    }

    assertTrue("Number of failed tests: " + failed, failed == 0);

  }

  /**
   * <p>Tests the {@link IcpFactory#makeQuery(IPAddr, String)} and
   * {@link IcpFactory#makeQuery(IPAddr, String, boolean, boolean)}
   * methods.</p>
   * @throws Exception if an unexpected error occurs.
   */
  public void testMakeQuery() throws Exception {
    expectQuery(icpFactory.makeQuery(MockIcpMessage.getMockRequester(),
                                     MockIcpMessage.getMockQueryUrl()),
                false,
                false);
    testMakeQuery(false, false);
    testMakeQuery(false, true);
    testMakeQuery(true, false);
    testMakeQuery(true, true);

  }

  /**
   * <p>Produces an {@link IcpFactory} instance of the class under
   * consideration.</p>
   * @return An ICP factory.
   */
  protected abstract IcpFactory makeIcpFactory();

  /**
   * <p>A helper method for {@link #testMakeQuery()}.</p>
   * @param srcrtt Whether to request a source return trip time.
   * @param hitobj Whether to request a hit object.
   * @throws Exception if an unexpected error occurs.
   */
  protected void testMakeQuery(boolean srcrtt, boolean hitobj)
      throws Exception {
    expectQuery(icpFactory.makeQuery(MockIcpMessage.getMockRequester(),
                                     MockIcpMessage.getMockQueryUrl(),
                                     srcrtt,
                                     hitobj),
                srcrtt,
                hitobj);
  }

  /**
   * <p>A logger for use by this class.</p>
   */
  private static final Logger logger = Logger.getLogger("IcpFactoryTester");

  /**
   * <p>Asserts that the argument message has desired properties with
   * respect to the expected message.</p>
   * @param expected An expected message.
   * @param message  An actual message.
   */
  protected static void expectMessage(IcpMessage expected,
                                      IcpMessage message) {
    assertEquals(expected.getUdpAddress(), message.getUdpAddress());
    assertEquals(expected.getUdpPort(), message.getUdpPort());
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
        assertEquals(expected.getPayloadObject(),
                     message.getPayloadObject());
        break;
    }
  }

  /**
   * <p>Asserts that the argument query has desired properties with
   * respect to the expected query.</p>
   * @param query  A query message.
   * @param srcrtt The expected value for whether the query requests a
   *               source return trip time.
   * @param hitobj The expected value for whether the query requests a
   *               hit object.
   */
  protected static void expectQuery(IcpMessage query,
                                    boolean srcrtt,
                                    boolean hitobj) {
    assertEquals(IcpMessage.ICP_OP_QUERY, query.getOpcode());
    assertEquals(srcrtt, query.requestsSrcRtt());
    assertEquals(hitobj, query.requestsHitObj());
    assertEquals(MockIcpMessage.getMockRequester(), query.getRequester());
    assertEquals(MockIcpMessage.getMockQueryUrl(), query.getPayloadUrl());
  }

}
