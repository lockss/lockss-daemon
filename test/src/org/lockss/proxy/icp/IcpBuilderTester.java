/*
 * $Id: IcpBuilderTester.java,v 1.2.2.1 2005-09-08 01:03:18 thib_gc Exp $
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

import java.net.InetAddress;
import java.util.Arrays;

import org.lockss.proxy.icp.IcpBuilder;
import org.lockss.proxy.icp.IcpBuilder.Factory;

import junit.framework.TestCase;

/**
 * <p>Tests classes that implement {@link IcpBuilder}.</p>
 * @author Thib Guicherd-Callin
 */
public abstract class IcpBuilderTester extends TestCase {

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
     * @param query An ICP query.
     * @return An ICP message.
     * @throws IcpProtocolException if an ICP error occurs.
     */
    IcpMessage makeResponse(IcpMessage query)
        throws IcpProtocolException;
    
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
     * @param query An ICP query.
     * @return An ICP message.
     * @throws IcpProtocolException if an ICP error occurs.
     */
    IcpMessage makeSrcRttResponse(IcpMessage query)
        throws IcpProtocolException;
    
  }
  /*
   * end NESTED INTERFACE
   * ====================
   */

  /**
   * <p>An ICP builder.</p>
   */
  private IcpBuilder builder;

  /* Inherit documentation */
  public void setUp() throws Exception {
    this.builder = makeFactory().makeIcpBuilder();
  }

  /**
   * <p>Tests {@link IcpBuilder#makeDenied}.</p>
   * @throws Exception if an error occurs.
   */
  public void testMakeDenied() throws Exception {
    ResponseTester tester = new ResponseTester() {
      public IcpMessage makeResponse(IcpMessage query)
          throws IcpProtocolException {
        return builder.makeDenied(query);
      }
    };
    performResponseTest(tester,
                        IcpMessage.ICP_OP_DENIED,
                        MockIcpMessage.getStandardSender());
  }

  /**
   * <p>Tests {@link IcpBuilder#makeDiscoveryEcho}.</p>
   * @throws Exception if an error occurs.
   */
  public void testMakeDiscoveryEcho() {
    // Unimplemented
  }
  
  /**
   * <p>Tests {@link IcpBuilder#makeError}.</p>
   * @throws Exception if an error occurs.
   */
  public void testMakeError() throws Exception {
    ResponseTester tester = new ResponseTester() {
      public IcpMessage makeResponse(IcpMessage query)
          throws IcpProtocolException {
        return builder.makeError(query);
      }
    };
    performResponseTest(tester,
                        IcpMessage.ICP_OP_ERR,
                        MockIcpMessage.getStandardSender());
  }
  
  /**
   * <p>Tests {@link IcpBuilder#makeHit}.</p>
   * @throws Exception if an error occurs.
   */
  public void testMakeHit() throws Exception {
    SrcRttResponseTester tester = new SrcRttResponseTester() {
      public IcpMessage makeResponse(IcpMessage query)
          throws IcpProtocolException {
        return builder.makeHit(query);
      }
      public IcpMessage makeSrcRttResponse(IcpMessage query)
          throws IcpProtocolException {
        return builder.makeHit(query,
                               MockIcpMessage.getStandardSrcRttResponse());
      }  
    };
    performResponseTest(tester,
                        IcpMessage.ICP_OP_HIT,
                        MockIcpMessage.getStandardSender());
  }

  /**
   * <p>Tests {@link IcpBuilder#makeHitObj}.</p>
   * @throws Exception if an error occurs.
   */
  public void testMakeHitObj() throws Exception {
    SrcRttResponseTester tester = new SrcRttResponseTester() {
      public IcpMessage makeResponse(IcpMessage query)
          throws IcpProtocolException {
        return builder.makeHitObj(query,
                                  MockIcpMessage.getStandardPayloadData());
      }
      public IcpMessage makeSrcRttResponse(IcpMessage query)
          throws IcpProtocolException {
        return builder.makeHitObj(query,
                                  MockIcpMessage.getStandardSrcRttResponse(),
                                  MockIcpMessage.getStandardPayloadData());
      }  
    };
    performResponseTest(tester,
                        IcpMessage.ICP_OP_HIT_OBJ,
                        MockIcpMessage.getStandardSender());
    
    // Additional tests
    IcpMessage query;
    IcpMessage response;
    query = MockIcpMessage.queryRequestHitObj();
    response = tester.makeResponse(query);
    assertTrue(Arrays.equals(MockIcpMessage.getStandardPayloadData(),
                             response.getPayloadObject()));
    query = MockIcpMessage.queryRequestSrcRttRequestHitObj();
    response = tester.makeSrcRttResponse(query);
    assertTrue(Arrays.equals(MockIcpMessage.getStandardPayloadData(),
                             response.getPayloadObject()));
  }

  /**
   * <p>Tests {@link IcpBuilder#makeMiss}.</p>
   * @throws Exception if an error occurs.
   */
  public void testMakeMiss() throws Exception {
    SrcRttResponseTester tester = new SrcRttResponseTester() {
      public IcpMessage makeResponse(IcpMessage query)
          throws IcpProtocolException {
        return builder.makeMiss(query);
      }
      public IcpMessage makeSrcRttResponse(IcpMessage query)
          throws IcpProtocolException { 
        return builder.makeMiss(query,
                                MockIcpMessage.getStandardSrcRttResponse());
      }
    };
    performResponseTest(tester,
                        IcpMessage.ICP_OP_MISS,
                        MockIcpMessage.getStandardSender());
  }
  
  /**
   * <p>Tests {@link IcpBuilder#makeMissNoFetch}.</p>
   * @throws Exception if an error occurs.
   */
  public void testMakeMissNoFetch() throws Exception {
    SrcRttResponseTester tester = new SrcRttResponseTester() {
      public IcpMessage makeResponse(IcpMessage query)
          throws IcpProtocolException {
        return builder.makeMissNoFetch(query);
      }
      public IcpMessage makeSrcRttResponse(IcpMessage query)
          throws IcpProtocolException { 
        return builder.makeMissNoFetch(query,
                                       MockIcpMessage.getStandardSrcRttResponse());
      }
    };
    performResponseTest(tester,
                        IcpMessage.ICP_OP_MISS_NOFETCH,
                        MockIcpMessage.getStandardSender());
  }

  /**
   * <p>Tests {@link IcpBuilder#makeQuery}.</p>
   * @throws Exception if an error occurs.
   */
  public void testMakeQuery() throws Exception {
    IcpMessage query;
    
    query = builder.makeQuery(MockIcpMessage.getStandardRequester(),
                              MockIcpMessage.getStandardQueryUrl());
    assertEquals(IcpMessage.ICP_OP_QUERY, query.getOpcode());
    assertEquals(0, query.getOptions());
    assertEquals(0, query.getOptionData());
    assertEquals(MockIcpMessage.getStandardRequester(), query.getRequester());
    assertEquals(MockIcpMessage.getStandardQueryUrl(), query.getPayloadUrl());
  }

  /**
   * <p>Tests {@link IcpBuilder#makeSourceEcho}.</p>
   * @throws Exception if an error occurs.
   */
  public void testMakeSourceEcho() {
    // Unimplemented
  }

  /**
   * <p>Produces an ICP builder factory that produces ICP builders of
   * the class under consideration.</p>
   * @return An ICP builder factory.
   */
  protected abstract Factory makeFactory();

  /**
   * <p>Performs a response test with the given response tester.</p>
   * @param tester         A response tester.
   * @param expectedOpcode The expected opcode.
   * @param expectedSender The expected sender.
   * @throws Exception if an error occurs.
   */
  private void performResponseTest(ResponseTester tester,
                                   byte expectedOpcode,
                                   InetAddress expectedSender)
      throws Exception {
    
    // Test null query
    try {
      IcpMessage response = tester.makeResponse(null);
      fail("Should have thrown NullPointerException but returned "
          + response.toString());
    }
    catch (NullPointerException npe) { /* succeed */ }

    // Test invalid query
    try {
      IcpMessage query = MockIcpMessage.invalid();
      IcpMessage response = tester.makeResponse(query);
      fail("Should have thrown IcpProtocolException but returned "
          + response.toString());
    }
    catch (IcpProtocolException ipe) { /* succeed */ }
    
    IcpMessage query = MockIcpMessage.query();
    IcpMessage response = tester.makeResponse(query);
    expect(response, expectedOpcode, query.getVersion(),
        query.getRequestNumber(), expectedSender,
        query.getPayloadUrl());
    assertFalse("Should not have contained a source return trip time: "
        + response.toString(), response.containsSrcRttResponse());
  }

  /**
   * <p>Performs a response test with the given response tester.</p>
   * @param tester         A response tester.
   * @param expectedOpcode The expected opcode.
   * @param expectedSender The expected sender.
   * @throws Exception if an error occurs.
   */
  private void performResponseTest(SrcRttResponseTester tester,
                                   byte expectedOpcode,
                                   InetAddress expectedSender)
      throws Exception {

    // Test invalid source return trip response
    try {
      IcpMessage query = MockIcpMessage.query();
      IcpMessage response = tester.makeSrcRttResponse(query);
      fail("Should have thrown IcpProtocolException but returned "
          + response.toString());
    }
    catch (IcpProtocolException ipe) { /* succeed */ }
    
    // Test valid source return trip response
    IcpMessage query = MockIcpMessage.queryRequestSrcRtt();
    IcpMessage response = tester.makeSrcRttResponse(query);
    expect(response, expectedOpcode, query.getVersion(),
        query.getRequestNumber(), expectedSender,
        query.getPayloadUrl());
    assertTrue("Should have contained source return trip time: "
        + response.toString(), response.containsSrcRttResponse());
    assertEquals(MockIcpMessage.getStandardSrcRttResponse(),
                 response.getSrcRttResponse());
    
    // Perform other response tests
    performResponseTest((ResponseTester)tester,
                        expectedOpcode,
                        expectedSender);
  }

  /**
   * <p>Asserts that the argument message has given properties.</p>
   * @param message               An ICP message.
   * @param expectedOpcode        The expected opcode.
   * @param expectedVersion       The expected version number.
   * @param expectedRequestNumber The expected request number.
   * @param expectedSender        The expected sender address.
   * @param expectedPayloadUrl    The expected payload URL.
   */
  private static void expect(IcpMessage message,
                             byte expectedOpcode,
                             byte expectedVersion,
                             int expectedRequestNumber,
                             InetAddress expectedSender,
                             String expectedPayloadUrl) {
    assertEquals(expectedOpcode, message.getOpcode());
    assertEquals(expectedVersion, message.getVersion());
    assertEquals(expectedRequestNumber, message.getRequestNumber());
    assertEquals(expectedPayloadUrl, message.getPayloadUrl());    
  }

}
