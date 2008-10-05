/*
 * $Id: TestIcpUtil.java,v 1.6 2007-03-14 23:39:41 thib_gc Exp $
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

import java.nio.ByteBuffer;

import junit.framework.AssertionFailedError;

import org.lockss.test.LockssTestCase;
import org.lockss.util.Logger;

/**
 * <p>Tests the {@link IcpUtil} class.</p>
 * @author Thib Guicherd-Callin
 */
public class TestIcpUtil extends LockssTestCase {

  /**
   * <p>Encapsulates the assertion performed for each of the
   * methods in {@link IcpUtil} that retrieve a field from a
   * {@link ByteBuffer}.</p>
   * @author Thib Guicherd-Callin
   */
  protected interface GetSomethingTester {

    /**
     * <p>Asserts that a field found in the byte buffer indeed
     * corresponds to what is expected in the actual message.</p>
     * @param actual An ICP message that the byte buffer should
     *               correspond to.
     * @param in     An ICP byte buffer.
     */
    void doAssert(IcpMessage actual, ByteBuffer in);

  }

  /**
   * <p>Tests the {@link IcpUtil#computeLength} method.</p>
   */
  public void testComputeLength() {
    int failed = 0;

    for (int test = 0 ; test < MockIcpMessage.countTestPairs(); test++) {
      try {
        logger.info("testComputeLength: begin test #" + test);
        IcpMessage message = MockIcpMessage.getTestMessage(test);
        assertEquals(message.getLength(), IcpUtil.computeLength(message));
        logger.info("testComputeLength: PASSED test #" + test);
      }
      catch (AssertionFailedError afe) {
        logger.info("testComputeLength: FAILED test #" + test);
        ++failed;
      }
    }

    assertTrue("Number of failed tests: " + failed, failed == 0);
  }

  /**
   * <p>Tests the {@link IcpUtil#getIpFromBuffer} method.</p>
   * @throws Exception if something unexpected happens.
   */
  public void testGetIpFromBuffer() throws Exception {
    byte[] array = {
        (byte)0, (byte)0, (byte)0, (byte)0,
        (byte)12, (byte)34, (byte)56, (byte)78,
        (byte)0, (byte)0, (byte)0, (byte)0
    };
    ByteBuffer buffer = ByteBuffer.wrap(array);
    assertEquals(new byte[] {(byte)12, (byte)34, (byte)56, (byte)78},
                 IcpUtil.getIpFromBuffer(buffer, 4).getAddress());
  }

  /**
   * <p>Tests the {@link IcpUtil#getLengthFromBuffer} method.</p>
   * @throws Exception if something unexpected happens.
   */
  public void testGetLengthFromBuffer() throws Exception {
    testGetSomethingFromBuffer(
        "testGetLengthFromBuffer",
        new GetSomethingTester() {
          public void doAssert(IcpMessage actual, ByteBuffer in) {
            assertEquals(actual.getLength(),
                         IcpUtil.getLengthFromBuffer(in));
          }
        });
  }

  /**
   * <p>Tests the {@link IcpUtil#getOpcodeFromBuffer} method.</p>
   * @throws Exception if something unexpected happens.
   */
  public void testGetOpcodeFromBuffer() throws Exception {
    testGetSomethingFromBuffer(
        "testGetOpcodeFromBuffer",
        new GetSomethingTester() {
          public void doAssert(IcpMessage actual, ByteBuffer in) {
            assertEquals(actual.getOpcode(),
                         IcpUtil.getOpcodeFromBuffer(in));
          }
        });
  }

  /**
   * <p>Tests the {@link IcpUtil#getOptionDataFromBuffer} method.</p>
   * @throws Exception if something unexpected happens.
   */
  public void testGetOptionDataFromBuffer() throws Exception {
    testGetSomethingFromBuffer(
        "testGetOptionDataFromBuffer",
        new GetSomethingTester() {
          public void doAssert(IcpMessage actual, ByteBuffer in) {
            assertEquals(actual.getOptionData(),
                         IcpUtil.getOptionDataFromBuffer(in));
          }
        });
  }

  /**
   * <p>Tests the {@link IcpUtil#getOptionsFromBuffer} method.</p>
   * @throws Exception if something unexpected happens.
   */
  public void testGetOptionsFromBuffer() throws Exception {
    testGetSomethingFromBuffer(
        "testGetOptionsFromBuffer",
        new GetSomethingTester() {
          public void doAssert(IcpMessage actual, ByteBuffer in) {
            assertEquals(actual.getOptions(),
                         IcpUtil.getOptionsFromBuffer(in));
          }
        });
  }

  /**
   * <p>Tests the {@link IcpUtil#getPayloadObjectLengthFromBuffer} method.</p>
   * @throws Exception if something unexpected happens.
   */
  public void testGetPayloadObjectLengthFromBuffer() throws Exception {
    testGetSomethingFromBuffer(
        "testGetPayloadObjectLengthFromBuffer",
        new GetSomethingTester() {
          public void doAssert(IcpMessage actual, ByteBuffer in) {
            if (actual.getOpcode() == IcpMessage.ICP_OP_HIT_OBJ) {
              assertEquals(actual.getPayloadObjectLength(),
                           IcpUtil.getPayloadObjectLengthFromBuffer(
                               in,
                               actual.getPayloadUrl()));
            } // else, skip
          }
        });
  }

  /**
   * <p>Tests the {@link IcpUtil#getPayloadUrlFromBuffer} method.</p>
   * @throws Exception if something unexpected happens.
   */
  public void testGetPayloadUrlFromBuffer() throws Exception {
    testGetSomethingFromBuffer(
        "testGetPayloadUrlFromBuffer",
        new GetSomethingTester() {
          public void doAssert(IcpMessage actual, ByteBuffer in) {
            assertEquals(actual.getPayloadUrl(),
                         IcpUtil.getPayloadUrlFromBuffer(
                             in,
                             actual.isQuery()));
          }
        });
  }

  /**
   * <p>Tests the {@link IcpUtil#getPayloadUrlFromBytes} method.</p>
   */
  public void testGetPayloadUrlFromBytes() {
    int failed = 0;

    for (int test = 0 ; test < MockIcpMessage.countTestPairs(); test++) {
      try {
        logger.info("testGetPayloadUrlFromBytes: begin test #" + test);
        byte[] raw = MockIcpMessage.getTestPacket(test).getData();
        IcpMessage message = MockIcpMessage.getTestMessage(test);
        assertEquals(message.getPayloadUrl(),
                     IcpUtil.getPayloadUrlFromBytes(
                         raw,
                         message.isQuery(),
                         message.getPayloadUrl().length()));
        logger.info("testGetPayloadUrlFromBytes: PASSED test #" + test);
      }
      catch (AssertionFailedError afe) {
        logger.info("testGetPayloadUrlFromBytes: FAILED test #" + test);
        ++failed;
      }
    }

    assertTrue("Number of failed tests: " + failed, failed == 0);
  }

  /**
   * <p>Tests the {@link IcpUtil#getPayloadObjectFromBytes} method.</p>
   */
  public void testGetPayloadObjectFromBytes() {
    int failed = 0;

    for (int test = 0 ; test < MockIcpMessage.countTestPairs(); test++) {
      try {
        logger.info("testGetPayloadObjectFromBytes: begin test #" + test);
        IcpMessage message = MockIcpMessage.getTestMessage(test);
        if (message.getOpcode() == IcpMessage.ICP_OP_HIT_OBJ) {
          byte[] raw = MockIcpMessage.getTestPacket(test).getData();
          byte dest[] = new byte[message.getPayloadObjectLength()];
          IcpUtil.getPayloadObjectFromBytes(raw,
                                            message.getPayloadUrl(),
                                            message.getPayloadObjectLength(),
                                            dest);
          assertEquals(message.getPayloadObject(), dest);
        }
        logger.info("testGetPayloadObjectFromBytes: PASSED test #" + test);
      }
      catch (AssertionFailedError afe) {
        logger.info("testGetPayloadObjectFromBytes: FAILED test #" + test);
        ++failed;
      }
    }

    assertTrue("Number of failed tests: " + failed, failed == 0);
  }

  /**
   * <p>Tests the {@link IcpUtil#getRequesterFromBuffer} method.</p>
   * @throws Exception if something unexpected happens.
   */
  public void testGetRequesterFromBuffer() throws Exception {
    testGetSomethingFromBuffer(
        "testGetRequesterFromBuffer",
        new GetSomethingTester() {
          public void doAssert(IcpMessage actual, ByteBuffer in) {
            if (actual.isQuery()) {
              assertEquals(actual.getRequester().getAddress(),
                           IcpUtil.getRequesterFromBuffer(in).getAddress());
            } // else, skip
          }
        });
  }

  /**
   * <p>Tests the {@link IcpUtil#getRequestNumberFromBuffer} method.</p>
   * @throws Exception if something unexpected happens.
   */
  public void testGetRequestNumberFromBuffer() throws Exception {
    testGetSomethingFromBuffer(
        "testGetRequestNumberFromBuffer",
        new GetSomethingTester() {
          public void doAssert(IcpMessage actual, ByteBuffer in) {
            assertEquals(actual.getRequestNumber(),
                         IcpUtil.getRequestNumberFromBuffer(in));
          }
        });
  }

  /**
   * <p>Tests the {@link IcpUtil#getSenderFromBuffer} method.</p>
   * @throws Exception if something unexpected happens.
   */
  public void testGetSenderFromBuffer() throws Exception {
    testGetSomethingFromBuffer(
        "testGetSenderFromBuffer",
        new GetSomethingTester() {
          public void doAssert(IcpMessage actual, ByteBuffer in) {
            assertEquals(actual.getSender(),
                         IcpUtil.getSenderFromBuffer(in));
          }
        });
  }

  /**
   * <p>Tests the {@link IcpUtil#getVersionFromBuffer} method.</p>
   * @throws Exception if something unexpected happens.
   */
  public void testGetVersionFromBuffer() throws Exception {
    testGetSomethingFromBuffer(
        "testGetVersionFromBuffer",
        new GetSomethingTester() {
          public void doAssert(IcpMessage actual, ByteBuffer in) {
            assertEquals(actual.getVersion(),
                         IcpUtil.getVersionFromBuffer(in));
          }
        });
  }

  /**
   * <p>Tests the {@link IcpUtil#stringLength} method.</p>
   */
  public void testStringLength() {
    int failed = 0;

    for (int test = 0 ; test < MockIcpMessage.countTestPairs(); test++) {
      try {
        logger.info("testStringLength: begin test #" + test);
        IcpMessage message = MockIcpMessage.getTestMessage(test);
        assertEquals(message.getPayloadUrl().length(),
                     IcpUtil.stringLength(message.getLength(),
                                          message.isQuery(),
                                          message.getOpcode() == IcpMessage.ICP_OP_HIT_OBJ,
                                          message.getPayloadObjectLength()));
        logger.info("testStringLength: PASSED test #" + test);
      }
      catch (AssertionFailedError afe) {
        logger.info("testStringLength: FAILED test #" + test);
        ++failed;
      }
    }

    assertTrue("Number of failed tests: " + failed, failed == 0);
  }

  /**
   * <p>Tests the {@link IcpUtil#isValidOpcode} method.</p>
   */
  public void testValidOpcodes() {
    // Careful not to run into an infinite overflow loop!
    // for (byte op = Byte.MIN_VALUE ; op <= Byte.MAX_VALUE ; op++)
    // doesn't work, and
    // for (byte op = Byte.MIN_VALUE ; op < Byte.MAX_VALUE ; op++)
    // requires an extra test for Byte.MAX_VALUE.
    byte op = Byte.MIN_VALUE;
    do {
      assertEquals(
             ( 1 <= op && op <=  4)  // QUERY, HIT, MISS, ERR
          || (10 <= op && op <= 11)  // SECHO, DECHO
          || (21 <= op && op <= 23), // MISS_NOFETCH, DENIED, HIT_OBJ
          IcpUtil.isValidOpcode(op)
      );
      op++;
    } while (op > Byte.MIN_VALUE);
  }

  /**
   * <p>Helper method to test various {@link IcpUtil} methods that
   * read fields from an ICP byte buffer.</p>
   * @param testName The name of the caller method.
   * @param tester   A corresponding {@link GetSomethingTester} tester.
   * @throws Exception if something unexpected occurs.
   */
  protected void testGetSomethingFromBuffer(String testName,
                                            GetSomethingTester tester)
      throws Exception {
    String testBegin = testName + ": begin test #";
    String testPassed = testName + ": PASSED test #";
    String testFailed = testName + ": FAILED test #";

    int failed = 0;

    for (int test = 0 ; test < MockIcpMessage.countTestPairs(); ++test) {
      try {
        logger.info(testBegin + test);
        ByteBuffer in =
          ByteBuffer.wrap(MockIcpMessage.getTestPacket(test).getData());
        IcpMessage actual = MockIcpMessage.getTestMessage(test);
        tester.doAssert(actual, in);
        logger.info(testPassed + test);
      }
      catch (AssertionFailedError afe) {
        logger.error(testFailed + test, afe);
        ++failed;
      }
    }

    assertTrue("Number of failed tests: " + failed, failed == 0);
  }

  /**
   * <p>A logger for use by this class.</p>
   */
  private static final Logger logger = Logger.getLogger("TestIcpUtil");

}
