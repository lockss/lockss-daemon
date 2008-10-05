/*
 * $Id: IcpMessageTester.java,v 1.2 2007-03-14 23:39:41 thib_gc Exp $
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
import java.util.ArrayList;

import junit.framework.AssertionFailedError;

import org.lockss.test.LockssTestCase;
import org.lockss.util.*;

/**
 * <p>A tester base class for implementors of {@link IcpMessage}.</p>
 * @author Thib Guicherd-Callin
 */
public abstract class IcpMessageTester extends LockssTestCase {

  /*
   * begin NESTED INTERFACE
   * ======================
   */
  /**
   * <p>Encapsulates a test on an ICP response.</p>
   * @author Thib Guicherd-Callin
   */
  private interface ResponseTester {

    /**
     * <p>Make a response to the given query.</p>
     * @return An ICP message.
     * @throws IcpException if an ICP error occurs.
     */
    IcpMessage makeResponse() throws IcpException;

  }
  /*
   * end NESTED INTERFACE
   * ====================
   */

  /*
   * begin NESTED INTERFACE
   * ======================
   */
  /**
   * <p>Encapsulates a test on an ICP response made from a query
   * requesting a source return time trip.</p>
   * @author Thib Guicherd-Callin
   */
  private interface SrcRttResponseTester extends ResponseTester {

    /**
     * <p>Make a response to the given query.</p>
     * @return An ICP message.
     * @throws IcpException if an ICP error occurs.
     */
    IcpMessage makeSrcRttResponse() throws IcpException;

  }
  /*
   * end NESTED INTERFACE
   * ====================
   */

  /**
   * <p>An ICP factory whose produced type of ICP messages is being
   * tested.</p>
   */
  protected IcpFactory icpFactory;

  /* Inherit documentation */
  public void setUp() {
    this.icpFactory = makeIcpFactory();
  }

  /**
   * <p>Tests {@link IcpMessage#makeDenied}.</p>
   * @throws Exception if an unexpected error condition arises.
   */
  public void testMakeDenied() throws Exception {
    ResponseTester tester = new ResponseTester() {
      public IcpMessage makeResponse() throws IcpException {
        return makeQuery().makeDenied();
      }
    };
    performResponseTest(tester, IcpMessage.ICP_OP_DENIED);
  }

  /**
   * <p>Tests {@link IcpMessage#makeError}.</p>
   * @throws Exception if an unexpected error condition arises.
   */
  public void testMakeError() throws Exception {
    ResponseTester tester = new ResponseTester() {
      public IcpMessage makeResponse() throws IcpException {
        return makeQuery().makeError();
      }
    };
    performResponseTest(tester, IcpMessage.ICP_OP_ERR);
  }

  /**
   * <p>Tests {@link IcpMessage#makeHit()}.</p>
   * @throws Exception if an unexpected error condition arises.
   */
  public void testMakeHit() throws Exception {
    ResponseTester tester = new ResponseTester() {
      public IcpMessage makeResponse() throws IcpException {
        return makeQuery().makeHit();
      }
    };
    performResponseTest(tester, IcpMessage.ICP_OP_HIT);
  }

  /**
   * <p>Tests {@link IcpMessage#makeHitObj(byte[])} and
   * {@link IcpMessage#makeHitObj(short, byte[])}.</p>
   * @throws Exception if an unexpected error condition arises.
   */
  public void testMakeHitObj() throws Exception {
    SrcRttResponseTester tester = new SrcRttResponseTester() {
      public IcpMessage makeResponse() throws IcpException {
        return makeQuery().makeHitObj(MockIcpMessage.getMockPayloadData());
      }
      public IcpMessage makeSrcRttResponse() throws IcpException {
        return makeQueryRequestSrcRtt().makeHitObj(MockIcpMessage.getMockSrcRttResponse(),
                                                   MockIcpMessage.getMockPayloadData());
      }
    };
    performSrcRttResponseTest(tester, IcpMessage.ICP_OP_HIT_OBJ);

    // Additional tests
    IcpMessage query;
    IcpMessage response;

    query = makeQueryRequestHitObj();
    response = tester.makeResponse();
    assertEquals(MockIcpMessage.getMockPayloadData(),
                 response.getPayloadObject());

    query = makeQueryRequestSrcRttRequestHitObj();
    response = tester.makeSrcRttResponse();
    assertEquals(MockIcpMessage.getMockPayloadData(),
                 response.getPayloadObject());
  }


  /**
   * <p>Tests {@link IcpMessage#makeHit(short)}.</p>
   * @throws Exception if an unexpected error condition arises.
   */
  public void testMakeHitSrcRtt() throws Exception {
    SrcRttResponseTester tester = new SrcRttResponseTester() {
      public IcpMessage makeResponse() throws IcpException {
        return makeQuery().makeHit();
      }
      public IcpMessage makeSrcRttResponse() throws IcpException {
        return makeQueryRequestSrcRtt().makeHit(MockIcpMessage.getMockSrcRttResponse());
      }
    };
    performSrcRttResponseTest(tester, IcpMessage.ICP_OP_HIT);
  }

  /**
   * <p>Tests {@link IcpMessage#makeMiss()}.</p>
   * @throws Exception if an unexpected error condition arises.
   */
  public void testMakeMiss() throws Exception {
    ResponseTester tester = new ResponseTester() {
      public IcpMessage makeResponse() throws IcpException {
        return makeQuery().makeMiss();
      }
    };
    performResponseTest(tester, IcpMessage.ICP_OP_MISS);
  }

  /**
   * <p>Tests {@link IcpMessage#makeMissNoFetch()}.</p>
   * @throws Exception if an unexpected error condition arises.
   */
  public void testMakeMissNoFetch() throws Exception {
    ResponseTester tester = new ResponseTester() {
      public IcpMessage makeResponse() throws IcpException {
        return makeQuery().makeMissNoFetch();
      }
    };
    performResponseTest(tester, IcpMessage.ICP_OP_MISS_NOFETCH);
  }

  /**
   * <p>Tests {@link IcpMessage#makeMissNoFetch(short)}.</p>
   * @throws Exception if an unexpected error condition arises.
   */
  public void testMakeMissNoFetchSrcRtt() throws Exception {
    SrcRttResponseTester tester = new SrcRttResponseTester() {
      public IcpMessage makeResponse() throws IcpException {
        return makeQuery().makeMissNoFetch();
      }
      public IcpMessage makeSrcRttResponse() throws IcpException {
        return makeQueryRequestSrcRtt().makeMissNoFetch(MockIcpMessage.getMockSrcRttResponse());
      }
    };
    performSrcRttResponseTest(tester, IcpMessage.ICP_OP_MISS_NOFETCH);
  }

  /**
   * <p>Tests {@link IcpMessage#makeMiss(short)}.</p>
   * @throws Exception if an unexpected error condition arises.
   */
  public void testMakeMissSrcRtt() throws Exception {
    SrcRttResponseTester tester = new SrcRttResponseTester() {
      public IcpMessage makeResponse() throws IcpException {
        return makeQuery().makeMiss();
      }
      public IcpMessage makeSrcRttResponse() throws IcpException {
        return makeQueryRequestSrcRtt().makeMiss(MockIcpMessage.getMockSrcRttResponse());
      }
    };
    performSrcRttResponseTest(tester, IcpMessage.ICP_OP_MISS);
  }

  /**
   * <p>Tests {@link IcpMessage#toDatagramPacket}.</p>
   * @throws Exception if an unexpected error condition arises.
   */
  public void testToDatagramPacket() throws Exception {
    int failed = 0;
    logger.warning("Numbered tests are counted from zero.");

    for (int test = 0 ; test < MockIcpMessage.countTestPairs(); ++test) {
      try {
        logger.info("testToDatagramPacket: begin test #" + test);
        DatagramPacket sample = MockIcpMessage.getTestPacket(test);
        IcpMessage message = icpFactory.makeMessage(sample);
        DatagramPacket packet =
          message.toDatagramPacket(MockIcpMessage.getMockDestination(),
                                   MockIcpMessage.getMockUdpPort());
        assertEquals(MockIcpMessage.getMockDestination().getInetAddr(), packet.getAddress());
        assertEquals(MockIcpMessage.getMockUdpPort(), packet.getPort());
        assertEquals(sample.getData(), packet.getData());
        logger.info("testToDatagramPacket: PASSED test #" + test);
      }
      catch (IcpException ipe) {
        logger.error("testToDatagramPacket: FAILED test #" + test, ipe);
        ++failed;
      }
      catch (AssertionFailedError afe) {
        logger.error("testToDatagramPacket: FAILED test #" + test, afe);
        ++failed;
      }
    }

    assertTrue("Number of failed tests: " + failed, failed == 0);
  }

  /**
   * <p>Obtains an instance of {@link IcpFactory} whose produced ICP
   * messages are of the type being tested.</p>
   * @return An ICP factory producing the required type of ICP
   *         messages.
   */
  protected abstract IcpFactory makeIcpFactory();

  /**
   * <p>Makes a query message.</p>
   * @return A query message.
   */
  protected IcpMessage makeQuery() {
    return icpFactory.makeQuery(MockIcpMessage.getMockSender(),
                                MockIcpMessage.getMockQueryUrl());
  }

  /**
   * <p>Makes a query message, requesting a hit object.</p>
   * @return A query message.
   */
  protected IcpMessage makeQueryRequestHitObj() {
    return icpFactory.makeQuery(MockIcpMessage.getMockSender(),
                                MockIcpMessage.getMockQueryUrl(),
                                false,
                                true);

  }

  /**
   * <p>Makes a query message, requesting a source return trip
   * time.</p>
   * @return A query message.
   */
  protected IcpMessage makeQueryRequestSrcRtt() {
    return icpFactory.makeQuery(MockIcpMessage.getMockSender(),
                                MockIcpMessage.getMockQueryUrl(),
                                true,
                                false);
  }

  /**
   * <p>Makes a query message, requesting a source return trip time
   * and a hit object.</p>
   * @return A query message.
   */
  protected IcpMessage makeQueryRequestSrcRttRequestHitObj() {
    return icpFactory.makeQuery(MockIcpMessage.getMockSender(),
                                MockIcpMessage.getMockQueryUrl(),
                                true,
                                true);
  }

  /**
   * <p>Performs a response test with the given response tester.</p>
   * @param tester         A response tester.
   * @param expectedOpcode The expected opcode.
   * @throws Exception if an error occurs.
   */
  private void performResponseTest(ResponseTester tester,
                                   byte expectedOpcode)
      throws Exception {
    IcpMessage response = tester.makeResponse();
    assertEquals(expectedOpcode, response.getOpcode());
    assertEquals(MockIcpMessage.getMockVersion(), response.getVersion());
    assertEquals(MockIcpMessage.getMockQueryUrl(), response.getPayloadUrl());
    assertFalse("Should not have contained a source return trip time",
                response.containsSrcRttResponse());
  }

  /**
   * <p>Performs a response test with the given response tester.</p>
   * @param tester         A response tester.
   * @param expectedOpcode The expected opcode.
   * @throws Exception if an error occurs.
   */
  private void performSrcRttResponseTest(SrcRttResponseTester tester,
                                         byte expectedOpcode)
      throws Exception {
    IcpMessage response = tester.makeSrcRttResponse();
    assertEquals(expectedOpcode, response.getOpcode());
    assertEquals(MockIcpMessage.getMockVersion(), response.getVersion());
    assertEquals(MockIcpMessage.getMockQueryUrl(), response.getPayloadUrl());
    assertTrue("Should have contained source return trip time",
               response.containsSrcRttResponse());
    assertEquals(MockIcpMessage.getMockSrcRttResponse(),
                 response.getSrcRttResponse());

    // Perform other response tests
    performResponseTest(tester, expectedOpcode);
  }

  /**
   * <p>A logger for use by this class.</p>
   */
  private static Logger logger = Logger.getLogger("IcpMessageTester");

}
