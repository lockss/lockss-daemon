/*
 * $Id: TestCacheExceptionMap.java,v 1.1 2004-02-05 03:01:21 clairegriffin Exp $
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
package org.lockss.plugin;

import java.net.*;

import org.lockss.test.*;

public class TestCacheExceptionMap extends LockssTestCase {
  private CacheExceptionMap cacheExceptionHandler = null;
  String RootString = CacheExceptionMap.EXCEPTION_CLASS_ROOT;

  protected void setUp() throws Exception {
    super.setUp();
    cacheExceptionHandler = new CacheExceptionMap();
  }

  protected void tearDown() throws Exception {
    cacheExceptionHandler = null;
    super.tearDown();
  }

  public void testMakeClassByName() {
    Exception exception;
    String checkClassName;

    // test the RetrySameUrlException
    checkClassName = RootString + "RetrySameUrlException";
    exception = makeExceptionClass(checkClassName);
    assertNotNull("made " + checkClassName, exception);

    // test the RetryPermUrlException
    checkClassName = RootString + "RetryPermUrlException";
    exception = makeExceptionClass(checkClassName);
    assertNotNull("made " + checkClassName, exception);

    // test the RetryTempUrlException
    checkClassName = RootString + "RetryTempUrlException";
    exception = makeExceptionClass(checkClassName);
    assertNotNull("made " + checkClassName, exception);

    // test the UnimplentedCodeException
    checkClassName = RootString + "UnimplementedCodeException";
    exception = makeExceptionClass(checkClassName);
    assertNotNull("made " + checkClassName, exception);

    // test the ExpectedNoRetryException
    checkClassName = RootString + "ExpectedNoRetryException";
    exception = makeExceptionClass(checkClassName);
    assertNotNull("made " + checkClassName, exception);

   // test the UnexpectedNoRetryException
    checkClassName = RootString + "UnexpectedNoRetryException";
    exception = makeExceptionClass(checkClassName);
    assertNotNull("made " + checkClassName, exception);

  }

  public void testGetException() {
    CacheException exception;
    int[] checkArray;
    int result_code = 0;
    int ic;

    //check unknown result code
    exception = cacheExceptionHandler.mapException(null,result_code, "foo");
    assertTrue("code:" + result_code, exception instanceof
               CacheException.UnknownCodeException);

    // check Success result codes
    checkArray = cacheExceptionHandler.SuccessCodes;
    for(ic = 0; ic < checkArray.length; ic++) {
      result_code = checkArray[ic];
      exception = cacheExceptionHandler.mapException(null, result_code, "foo");
      assertNull("code:" + result_code, exception);
    }

    // check RetrySameUrlExceptions
    checkArray = cacheExceptionHandler.SameUrlCodes;
    for(ic =0; ic < checkArray.length; ic++) {
      result_code = checkArray[ic];
      exception = cacheExceptionHandler.mapException(null,result_code, "foo");
      assertTrue("code:" + result_code, exception instanceof
                 CacheException.RetrySameUrlException);
    }

    // test the RetryPermUrlException
    checkArray = cacheExceptionHandler.MovePermCodes;
    for(ic =0; ic < checkArray.length; ic++) {
      result_code = checkArray[ic];
      exception = cacheExceptionHandler.mapException(null,result_code, "foo");
      assertTrue("code:" + result_code, exception instanceof
                 CacheException.RetryPermUrlException);
    }

    // test the RetryTempUrlException
    checkArray = cacheExceptionHandler.MoveTempCodes;
    for(ic =0; ic < checkArray.length; ic++) {
      result_code = checkArray[ic];
      exception = cacheExceptionHandler.mapException(null,result_code, "foo");
      assertTrue("code:" + result_code, exception instanceof
                 CacheException.RetryTempUrlException);
    }

    // test the UnimplementedCodeException
    checkArray = cacheExceptionHandler.UnimplementedCodes;
    for(ic =0; ic < checkArray.length; ic++) {
      result_code = checkArray[ic];
      exception = cacheExceptionHandler.mapException(null, result_code, "foo");
      assertTrue("code:" + result_code, exception instanceof
                 CacheException.UnimplementedCodeException);
    }

    // test the ExpectedNoRetryException
    checkArray = cacheExceptionHandler.ExpectedCodes;
    for(ic =0; ic < checkArray.length; ic++) {
      result_code = checkArray[ic];
      exception = cacheExceptionHandler.mapException(null,result_code, "foo");
      assertTrue("code:" + result_code, exception instanceof
                 CacheException.ExpectedNoRetryException);
    }

    // test the UnexpectedNoRetryException
    checkArray = cacheExceptionHandler.UnexpectedCodes;
    for(ic =0; ic < checkArray.length; ic++) {
      result_code = checkArray[ic];
      exception = cacheExceptionHandler.mapException(null,result_code, "foo");
      assertTrue("code:" + result_code, exception instanceof
                 CacheException.UnexpectedNoRetryException);
    }


  }


  public void testInitExceptionTable() {
    int[] checkArray;
    String   checkClassName;

    // test the RetrySameUrlException
    checkArray = cacheExceptionHandler.SameUrlCodes;
    checkClassName = RootString + "RetrySameUrlException";
    checkExceptionClassName(checkArray, checkClassName);

    // test the RetryPermUrlException
    checkArray = cacheExceptionHandler.MovePermCodes;
    checkClassName = RootString + "RetryPermUrlException";
    checkExceptionClassName(checkArray, checkClassName);

    // test the RetryTempUrlException
    checkArray = cacheExceptionHandler.MoveTempCodes;
    checkClassName = RootString + "RetryTempUrlException";
    checkExceptionClassName(checkArray, checkClassName);

    // test the UnimplentedCodeException
    checkArray = cacheExceptionHandler.UnimplementedCodes;
    checkClassName = RootString + "UnimplementedCodeException";
    checkExceptionClassName(checkArray, checkClassName);

    // test the ExpectedNoRetryException
    checkArray = cacheExceptionHandler.ExpectedCodes;
    checkClassName = RootString + "ExpectedNoRetryException";
    checkExceptionClassName(checkArray, checkClassName);

   // test the UnexpectedNoRetryException
    checkArray = cacheExceptionHandler.UnexpectedCodes;
    checkClassName = RootString + "UnexpectedNoRetryException";
    checkExceptionClassName(checkArray, checkClassName);
  }

  public void testClassTree() {
    Exception exception;
    String checkClassName;

    // test UnknownCodeException
    checkClassName = RootString + "UnknownCodeException";
    exception = makeExceptionClass(checkClassName);
    assertTrue(checkClassName, exception instanceof CacheException);

    // test our base Retryable root
    checkClassName = RootString + "RetryableException";
    exception = makeExceptionClass(checkClassName);
    assertTrue(checkClassName, exception instanceof CacheException);

    // test our base Unretryable root
    checkClassName = RootString + "UnretryableException";
    exception = makeExceptionClass(checkClassName);
    assertTrue(checkClassName, exception instanceof CacheException);

    // test the RetrySameUrlException
    checkClassName = RootString + "RetrySameUrlException";
    exception = makeExceptionClass(checkClassName);
    assertTrue(checkClassName, exception instanceof
               CacheException.RetryableException);

    // test base RetryNewUrlException root
    checkClassName = RootString + "RetryNewUrlException";
    exception = makeExceptionClass(checkClassName);
    assertTrue(checkClassName, exception instanceof
               CacheException.RetryableException);

    // test the RetryPermUrlException
    checkClassName = RootString + "RetryPermUrlException";
    exception = makeExceptionClass(checkClassName);
    assertTrue(checkClassName, exception instanceof
               CacheException.RetryNewUrlException);

    // test the RetryTempUrlException
    checkClassName = RootString + "RetryTempUrlException";
    exception = makeExceptionClass(checkClassName);
    assertTrue(checkClassName, exception instanceof
               CacheException.RetryNewUrlException);

    // test the UnimplentedCodeException
    checkClassName = RootString + "UnimplementedCodeException";
    exception = makeExceptionClass(checkClassName);
    assertTrue(checkClassName, exception instanceof
               CacheException.UnretryableException);

    // test the ExpectedNoRetryException
    checkClassName = RootString + "ExpectedNoRetryException";
    exception = makeExceptionClass(checkClassName);
    assertTrue(checkClassName, exception instanceof
               CacheException.UnretryableException);


   // test the UnexpectedNoRetryException
    checkClassName = RootString + "UnexpectedNoRetryException";
    exception = makeExceptionClass(checkClassName);
    assertTrue(checkClassName, exception instanceof
               CacheException.UnretryableException);

  }

  public void testCacheExceptionHandler() {
    int[] handledCodes = {200, 405, 302};
    CacheException exception;
    int result_code = 0;

    MockCacheExceptionHandler handler = new MockCacheExceptionHandler();
    handler.setHandledCodes(handledCodes, cacheExceptionHandler);
    // now lets find out if we actually handle the codes.
    // test the UnexpectedNoRetryException
    for(int ic =0; ic < handledCodes.length; ic++) {
      result_code = handledCodes[ic];
      exception = cacheExceptionHandler.mapException(null, result_code, "foo");
      assertTrue("code:" + result_code, exception instanceof CacheException);
    }
  }

  private void checkExceptionClassName(int[] checkArray,
                                       String checkClassName) {
    String className;
    for (int ic = 0; ic < checkArray.length; ic++) {
      className = cacheExceptionHandler.getExceptionClassName(checkArray[ic]);
      assertEquals("class for "+checkArray[ic], checkClassName, className);
    }
  }

  private Exception makeExceptionClass(String exceptionClassName) {
    try {
      Class exceptionClass = Class.forName(exceptionClassName);
      Object exception = exceptionClass.newInstance();
      Exception cacheException = (Exception) exception;
      return cacheException;
    }
    catch (Exception ex) {
      return null;
    }
  }

  static public class MockCacheExceptionHandler
      implements CacheExceptionHandler {
    private static int[] m_returnCodes;

    public MockCacheExceptionHandler() {
    }

    void setHandledCodes(int[] returnCodes, CacheExceptionMap map) {
      m_returnCodes = returnCodes;
      map.storeArrayEntries(returnCodes, this.getClass().getName());
    }

    /**
     * handleException
     *
     * @param code int
     * @param connection HttpURLConnection
     * @return CacheException
     */
    public CacheException handleException(int code,
                                          HttpURLConnection connection) {
      for(int i=0; i< m_returnCodes.length; i++) {
        if(m_returnCodes[i] == code) {
          return new CacheException("Handled Exception");
        }
      }
      return null;
    }

  }
}
