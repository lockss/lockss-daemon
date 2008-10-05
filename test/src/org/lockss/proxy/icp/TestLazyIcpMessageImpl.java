/*
 * $Id: TestLazyIcpMessageImpl.java,v 1.2 2007-03-14 23:39:41 thib_gc Exp $
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

/**
 * <p>Tests the {@link IcpMessage} implementation provided by
 * {@link LazyIcpFactoryImpl}.</p>
 * @author Thib Guicherd-Callin
 */
public class TestLazyIcpMessageImpl extends IcpMessageTester {

  /* Inherit documentation */
  public void testMakeHitObj() throws Exception {
    try {
      super.testMakeHitObj();
      fail(NO_EXCEPTION);
    }
    catch (UnsupportedOperationException uoeIgnore) {}
  }

  /* Inherit documentation */
  public void testMakeHitSrcRtt() throws Exception {
    try {
      super.testMakeHitSrcRtt();
      fail(NO_EXCEPTION);
    }
    catch (UnsupportedOperationException uoeIgnore) {}
  }

  /* Inherit documentation */
  public void testMakeMiss() throws Exception {
    try {
      super.testMakeMiss();
      fail(NO_EXCEPTION);
    }
    catch (UnsupportedOperationException uoeIgnore) {}
  }

  /* Inherit documentation */
  public void testMakeMissNoFetchSrcRtt() throws Exception {
    try {
      super.testMakeMissNoFetchSrcRtt();
      fail(NO_EXCEPTION);
    }
    catch (UnsupportedOperationException uoeIgnore) {}
  }

  /* Inherit documentation */
  public void testMakeMissSrcRtt() throws Exception {
    try {
      super.testMakeMissSrcRtt();
      fail(NO_EXCEPTION);
    }
    catch (UnsupportedOperationException uoeIgnore) {}
  }

  /* Inherit documentation */
  protected IcpFactory makeIcpFactory() {
    return LazyIcpFactoryImpl.getInstance();
  }

  /* Inherit documentation */
  protected IcpMessage makeQuery() {
    try {
      return icpFactory.makeMessage(MockIcpMessage.getSampleQueryUdpPacket());
    }
    catch (IcpException ipe) {
      throw new RuntimeException(
          "Unexpected internal error in " + getClass().getName());
    }
  }

  /**
   * <p>An error string.</p>
   */
  private static final String NO_EXCEPTION =
    "Should have thrown an UnsupportedOperationException";

}
