/*
 * $Id: TestHttpResultMap.java,v 1.3 2004-03-26 23:29:53 clairegriffin Exp $
 */

/*

Copyright (c) 2000-2004 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.util.urlconn;

import org.lockss.test.*;
import org.lockss.plugin.*;

public class TestHttpResultMap extends LockssTestCase {
  private HttpResultMap resultMap = null;

  protected void setUp() throws Exception {
    super.setUp();
    resultMap = new HttpResultMap();
  }

  protected void tearDown() throws Exception {
    resultMap = null;
    super.tearDown();
  }

  public void testGetException() {
    CacheException exception;
    int[] checkArray;
    int result_code = 0;
    int ic;

    //check unknown result code
    exception = resultMap.mapException(null,result_code, "foo");
    assertTrue("code " + result_code + ": " + exception,
	       exception instanceof CacheException.UnknownCodeException);

    // check Success result codes
    checkArray = resultMap.SuccessCodes;
    for(ic = 0; ic < checkArray.length; ic++) {
      result_code = checkArray[ic];
      exception = resultMap.mapException(null, result_code, "foo");
      assertNull("code " + result_code + ": " + exception, exception);
    }

    // check RetrySameUrlExceptions
    checkArray = resultMap.SameUrlCodes;
    for(ic =0; ic < checkArray.length; ic++) {
      result_code = checkArray[ic];
      exception = resultMap.mapException(null,result_code, "foo");
      assertTrue("code:" + result_code, exception instanceof
                 CacheException.RetrySameUrlException);
    }

    // test the RetryPermUrlException
    checkArray = resultMap.MovePermCodes;
    for(ic =0; ic < checkArray.length; ic++) {
      result_code = checkArray[ic];
      exception = resultMap.mapException(null,result_code, "foo");
      assertTrue("code:" + result_code, exception instanceof
                 CacheException.NoRetryPermUrlException);
    }

    // test the RetryTempUrlException
    checkArray = resultMap.MoveTempCodes;
    for(ic =0; ic < checkArray.length; ic++) {
      result_code = checkArray[ic];
      exception = resultMap.mapException(null,result_code, "foo");
      assertTrue("code:" + result_code, exception instanceof
                 CacheException.NoRetryTempUrlException);
    }

    // test the UnimplementedCodeException
    checkArray = resultMap.UnimplementedCodes;
    for(ic =0; ic < checkArray.length; ic++) {
      result_code = checkArray[ic];
      exception = resultMap.mapException(null, result_code, "foo");
      assertTrue("code:" + result_code, exception instanceof
                 CacheException.UnimplementedCodeException);
    }

    // test the ExpectedNoRetryException
    checkArray = resultMap.ExpectedCodes;
    for(ic =0; ic < checkArray.length; ic++) {
      result_code = checkArray[ic];
      exception = resultMap.mapException(null,result_code, "foo");
      assertTrue("code:" + result_code, exception instanceof
                 CacheException.ExpectedNoRetryException);
    }

    // test the UnexpectedNoRetryException
    checkArray = resultMap.UnexpectedFailCodes;
    for(ic =0; ic < checkArray.length; ic++) {
      result_code = checkArray[ic];
      exception = resultMap.mapException(null,result_code, "foo");
      assertTrue("code:" + result_code, exception instanceof
                 CacheException.UnexpectedNoRetryFailException);
    }


  }


  public void testInitExceptionTable() {
    int[] checkArray;
    String   checkClassName;

    // test the RetrySameUrlException
    checkArray = resultMap.SameUrlCodes;
    checkExceptionClass(checkArray,
			CacheException.RetrySameUrlException.class);

    // test the RetryPermUrlException
    checkArray = resultMap.MovePermCodes;
    checkExceptionClass(checkArray,
			CacheException.NoRetryPermUrlException.class);

    // test the RetryTempUrlException
    checkArray = resultMap.MoveTempCodes;
    checkExceptionClass(checkArray,
			CacheException.NoRetryTempUrlException.class);

    // test the UnimplentedCodeException
    checkArray = resultMap.UnimplementedCodes;
    checkExceptionClass(checkArray,
			CacheException.UnimplementedCodeException.class);

    // test the ExpectedNoRetryException
    checkArray = resultMap.ExpectedCodes;
    checkExceptionClass(checkArray,
			CacheException.ExpectedNoRetryException.class);

   // test the UnexpectedNoRetryException
    checkArray = resultMap.UnexpectedFailCodes;
    checkExceptionClass(checkArray,
			CacheException.UnexpectedNoRetryFailException.class);

    checkArray = resultMap.UnexpectedNoFailCodes;
    checkExceptionClass(checkArray,
                        CacheException.UnexpectedNoRetryNoFailException.class);

    checkArray = resultMap.RetryDeadLinkCodes;
    checkExceptionClass(checkArray,
                        CacheException.RetryDeadLinkException.class);

    checkArray = resultMap.NoRetryDeadLinkCodes;
    checkExceptionClass(checkArray,
                        CacheException.NoRetryDeadLinkException.class);
  }

  public void testClassTree() {
    Exception exception;
    Class checkClass;

    // test UnknownCodeException
    checkClass = CacheException.UnknownCodeException.class;
    exception = makeException(checkClass);
    assertTrue(checkClass.getName(), exception instanceof CacheException);

    // test our base Retryable root
    checkClass = CacheException.RetryableException.class;
    exception = makeException(checkClass);
    assertTrue(checkClass.getName(), exception instanceof CacheException);

    // test our base Unretryable root
    checkClass = CacheException.UnretryableException.class;
    exception = makeException(checkClass);
    assertTrue(checkClass.getName(), exception instanceof CacheException);

    // test the RetrySameUrlException
    checkClass = CacheException.RetrySameUrlException.class;
    exception = makeException(checkClass);
    assertTrue(checkClass.getName(),
	       exception instanceof CacheException.RetryableException);

    // test the RetryDeadLinkException
    checkClass = CacheException.RetryDeadLinkException.class;
    exception = makeException(checkClass);
    assertTrue(checkClass.getName(),
               exception instanceof CacheException.RetryableException);

    // test base RetryNewUrlException root
    checkClass = CacheException.NoRetryNewUrlException.class;
    exception = makeException(checkClass);
    assertTrue(checkClass.getName(),
	       exception instanceof CacheException.UnretryableException);

    // test the RetryPermUrlException
    checkClass = CacheException.NoRetryPermUrlException.class;
    exception = makeException(checkClass);
    assertTrue(checkClass.getName(),
	       exception instanceof CacheException.NoRetryNewUrlException);

    // test the RetryTempUrlException
    checkClass = CacheException.NoRetryTempUrlException.class;
    exception = makeException(checkClass);
    assertTrue(checkClass.getName(),
	       exception instanceof CacheException.NoRetryNewUrlException);

    // test the UnimplentedCodeException
    checkClass = CacheException.UnimplementedCodeException.class;
    exception = makeException(checkClass);
    assertTrue(checkClass.getName(),
	       exception instanceof CacheException.UnretryableException);

    // test the ExpectedNoRetryException
    checkClass = CacheException.ExpectedNoRetryException.class;
    exception = makeException(checkClass);
    assertTrue(checkClass.getName(),
	       exception instanceof CacheException.UnretryableException);

    checkClass = CacheException.NoRetryDeadLinkException.class;
    exception = makeException(checkClass);
    assertTrue(checkClass.getName(),
               exception instanceof CacheException.ExpectedNoRetryException);

   // test the UnexpectedNoRetryException
    checkClass = CacheException.UnexpectedNoRetryFailException.class;
    exception = makeException(checkClass);
    assertTrue(checkClass.getName(),
	       exception instanceof CacheException.UnretryableException);

    checkClass = CacheException.UnexpectedNoRetryNoFailException.class;
    exception = makeException(checkClass);
    assertTrue(checkClass.getName(),
               exception instanceof CacheException.UnretryableException);
  }

  public void testHttpResultHandler() {
    int[] handledCodes = {200, 405, 302};
    CacheException exception;
    int result_code = 0;

    MockHttpResultHandler handler = new MockHttpResultHandler();
    handler.setHandledCodes(handledCodes);
    handler.init(resultMap);
    // now lets find out if we actually handle the codes.
    // test the UnexpectedNoRetryException
    for(int ic =0; ic < handledCodes.length; ic++) {
      result_code = handledCodes[ic];
      exception = resultMap.mapException(null, result_code, "foo");
      assertTrue("code:" + result_code, exception instanceof CacheException);
    }
  }

  private void checkExceptionClass(int[] checkArray,
                                  Class checkClass) {
    for (int ic = 0; ic < checkArray.length; ic++) {
      Class cls = resultMap.getExceptionClass(checkArray[ic]);
      assertEquals("Wrong class for "+checkArray[ic], checkClass, cls);
    }
  }

  private Exception makeException(Class exceptionClass) {
    try {
      Object exception = exceptionClass.newInstance();
      Exception cacheException = (Exception) exception;
      return cacheException;
    }
    catch (Exception ex) {
      return null;
    }
  }

  static public class MockHttpResultHandler
      implements CacheResultHandler {
    private static int[] m_returnCodes;

    public MockHttpResultHandler() {
    }

    public void init(CacheResultMap crmap) {
      HttpResultMap map = (HttpResultMap)crmap;
      if(m_returnCodes != null) {
        map.storeArrayEntries(m_returnCodes, this.getClass());
      }
    }
    void setHandledCodes(int[] returnCodes) {
      m_returnCodes = returnCodes;
    }

    /**
     * handleException
     *
     * @param code int
     * @param connection HttpURLConnection
     * @return CacheException
     */
    public CacheException handleResult(int code,
				       LockssUrlConnection connection) {
      for(int i=0; i< m_returnCodes.length; i++) {
        if(m_returnCodes[i] == code) {
          return new CacheException("Handled Exception");
        }
      }
      return null;
    }

  }

}
