/*

Copyright (c) 2000-2021 Board of Trustees of Leland Stanford Jr. University,
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

import java.io.*;
import java.net.*;
import java.util.*;

import org.lockss.test.*;
import org.lockss.plugin.*;
import org.lockss.util.*;
import org.lockss.util.urlconn.*;

public class TestHttpResultMap extends LockssTestCase {
  private HttpResultMap resultMap = null;
  private MockArchivalUnit mau;

  protected void setUp() throws Exception {
    super.setUp();
    mau = new MockArchivalUnit();
    resultMap = new HttpResultMap();
  }

  protected void tearDown() throws Exception {
    resultMap = null;
    super.tearDown();
  }

  void assertX(int code, Class cls) {
    CacheException exception = resultMap.mapException(null, "", code, "foo");
    assertTrue("code:" + code, cls.isInstance(exception));
  }

  public void testGetResultCodeException() {
    CacheException exception;
    int result_code = 0;
    int ic;

    //check unknown result code
    exception = resultMap.mapException(null, "", result_code, "foo");
    assertTrue("code " + result_code + ": " + exception,
	       exception instanceof CacheException.UnknownCodeException);

    // check Success result codes
    exception = resultMap.mapException(null, "", 200, "foo");
    assertNull("code " + result_code + ": " + exception, exception);
    exception = resultMap.mapException(null, "", 203, "foo");
    assertNull("code " + result_code + ": " + exception, exception);
    exception = resultMap.mapException(null, "", 304, "foo");
    assertNull("code " + result_code + ": " + exception, exception);

    // check RetrySameUrlExceptions
    assertX(408, CacheException.RetrySameUrlException.class);
    assertX(409, CacheException.RetrySameUrlException.class);
    assertX(413, CacheException.RetrySameUrlException.class);
    assertX(500, CacheException.RetrySameUrlException.class);
    assertX(502, CacheException.RetrySameUrlException.class);
    assertX(503, CacheException.RetrySameUrlException.class);
    assertX(504, CacheException.RetrySameUrlException.class);

    // test the moved permanently codes
    assertX(301, CacheException.NoRetryPermUrlException.class);

    // test the moved temporarily codes
    assertX(307, CacheException.NoRetryTempUrlException.class);
    assertX(303, CacheException.NoRetryTempUrlException.class);
    assertX(302, CacheException.NoRetryTempUrlException.class);

    // test the UnimplementedCodeException

    // test the ExpectedNoRetryException
    assertX(305, CacheException.ExpectedNoRetryException.class);
    assertX(402, CacheException.ExpectedNoRetryException.class);

    // test the PermissionException codes
    assertX(401, CacheException.PermissionException.class);
    assertX(403, CacheException.PermissionException.class);
    assertX(407, CacheException.PermissionException.class);

    // test NoRetryDeadLinkException codes
    assertX(204, CacheException.NoRetryDeadLinkException.class);
    assertX(300, CacheException.NoRetryDeadLinkException.class);
    assertX(400, CacheException.NoRetryDeadLinkException.class);
    assertX(404, CacheException.NoRetryDeadLinkException.class);
    assertX(405, CacheException.NoRetryDeadLinkException.class);
    assertX(406, CacheException.NoRetryDeadLinkException.class);
    assertX(410, CacheException.NoRetryDeadLinkException.class);

    // test the UnexpectedNoRetryException codes
    assertX(201, CacheException.UnexpectedNoRetryFailException.class);
    assertX(202, CacheException.UnexpectedNoRetryFailException.class);
    assertX(205, CacheException.UnexpectedNoRetryFailException.class);
    assertX(206, CacheException.UnexpectedNoRetryFailException.class);
    assertX(306, CacheException.UnexpectedNoRetryFailException.class);
    assertX(411, CacheException.UnexpectedNoRetryFailException.class);
    assertX(412, CacheException.UnexpectedNoRetryFailException.class);
    assertX(416, CacheException.UnexpectedNoRetryFailException.class);
    assertX(417, CacheException.UnexpectedNoRetryFailException.class);
    assertX(501, CacheException.UnexpectedNoRetryFailException.class);
    assertX(505, CacheException.UnexpectedNoRetryFailException.class);
  }

  public void testGetExceptionException() {
    CacheException exception;

    //check unknown exception
    exception = resultMap.mapException(null, "", new Exception(), "foo");
    assertTrue(exception instanceof CacheException.UnknownExceptionException);

    exception = resultMap.mapException(null, "",
				       new RuntimeException(), "foo");
    assertTrue(exception instanceof CacheException.UnknownExceptionException);

    exception = resultMap.mapException(null, "",
				       new SocketException(), "foo");
    assertTrue(exception.toString(),
	       exception instanceof
	       CacheException.RetryableNetworkException_3_30S);

    exception = resultMap.mapException(null, "",
				       new SocketTimeoutException(), "foo");
    assertTrue(exception.toString(),
	       exception instanceof
	       CacheException.RetryableNetworkException_3_30S);

    exception = resultMap.mapException(null, "",
				       new ConnectException(), "foo");
    assertTrue(exception instanceof
	       CacheException.RetryableNetworkException_3_30S);

    exception = resultMap.mapException(null, "",
				       new NoRouteToHostException(),
				       "foo");
    assertTrue(exception instanceof
	       CacheException.RetryableNetworkException_3_30S);

    exception = resultMap.mapException(null, "",
				       new UnknownHostException("h.tld"),
				       "foo");
    assertTrue(exception instanceof
	       CacheException.RetryableNetworkException_2_30S);
    assertEquals("Unknown host: h.tld", exception.getMessage());

    exception = resultMap.mapException(null, "",
				       new LockssUrlConnection.ConnectionTimeoutException("msg"),
				       "foo");
    assertTrue(exception instanceof
	       CacheException.RetryableNetworkException_3_30S);

    exception = resultMap.mapException(null, "",
				       new java.io.IOException("CRLF expected at end of chunk: -1/-1"),
				       "foo");
    assertTrue(exception instanceof CacheException.UnknownExceptionException);
    resultMap.storeMapEntry(IOException.class,
			    CacheException.RetryableNetworkException_3_30S.class);

    exception = resultMap.mapException(null, "",
				       new java.io.IOException("CRLF expected at end of chunk: -1/-1"),
				       "foo");
    assertTrue(exception instanceof
	       CacheException.RetryableNetworkException_3_30S);

    exception = resultMap.mapException(null, "",
				       new ContentValidationException.EmptyFile("Empty File 42"),
				       "foo");
    assertTrue(exception instanceof CacheException.WarningOnly);

    exception = resultMap.mapException(null, "",
				       new javax.net.ssl.SSLException("ssl ex"),
				       "foo");
    assertTrue(exception.toString(),
	       exception instanceof
	       CacheException.RetryableNetworkException_3_30S);

    // Unmapped subclass of ContentValidationException should map to what
    // ContentValidationException maps to
    exception = resultMap.mapException(null, "",
				       new UnmappedContentValidationException(),
				       "foo");
    assertClass(CacheException.UnretryableException.class, exception);

    exception = resultMap.mapException(null, "",
				       new ContentValidationException.LogOnly("Important warning message"),
				       "foo");
    assertTrue(exception instanceof CacheException.WarningOnly);

    Exception ce =
      new CacheException.PermissionException("Already mapped exception");
    assertSame(ce, resultMap.mapException(null, "", ce, "foo"));
  }

  static class UnmappedContentValidationException
    extends ContentValidationException {
  }

  public void testInitExceptionTable() {
    int[] checkArray;
    String   checkClassName;

    // test the RetrySameUrlException
    checkArray = resultMap.RetrySameUrlCodes;
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

  public void testHttpResultHandlerInit() {
    int[] handledCodes = {200, 405, 302};
    CacheException exception;
    int result_code = 0;

    MyHttpResultHandler handler = new MyHttpResultHandler();
    handler.setHandledCodes(handledCodes);
    handler.init(resultMap);
    // now lets find out if we actually handle the codes.
    // test the UnexpectedNoRetryException
    for(int ic =0; ic < handledCodes.length; ic++) {
      result_code = handledCodes[ic];
      exception = resultMap.mapException(null, "", result_code, "foo");
      assertClass("code:" + result_code,
		  CacheException.class, exception);
      assertNotClass("code:" + result_code,
		     CacheException.UnknownCodeException.class, exception);
    }
  }

  public void testHttpResultHandlerHandleCode() throws IOException {
    int[] handledCodes = {200, 405, 302};
    CacheException exception;
    int result_code = 0;

    MyHttpResultHandler handler = new MyHttpResultHandler();
    handler.setHandledCodes(handledCodes);
    resultMap.storeMapEntry(405, new MyHttpResultHandler());
    MockLockssUrlConnection conn = new MockLockssUrlConnection();
    conn.setURL("http://uuu17/");
    Exception ee = resultMap.mapException(mau, conn, 405, null);
    assertEquals(RecordingCacheException.class, ee.getClass());
    RecordingCacheException re = (RecordingCacheException)ee;
    assertEquals("http://uuu17/", re.url);
    assertSame(mau, re.au);
    assertEquals(405, re.responseCode);
    assertEquals(null, re.triggerException);
    assertEquals(CacheException.NoRetryTempUrlException.class,
		 resultMap.mapException(null, "", 302, null).getClass());
    assertEquals(null, resultMap.mapException(null, "", 200, null));
  }

  public void testHttpResultHandlerHandleException() throws IOException {
    Exception e1 = new RuntimeException("foo");
    Exception e2 = new SocketException("foo");
    MyHttpResultHandler handler = new MyHttpResultHandler();
    handler.setHandledExceptions(SetUtil.set(e1));
    resultMap.storeMapEntry(e1.getClass(), new MyHttpResultHandler());

    MockLockssUrlConnection conn = new MockLockssUrlConnection();
    conn.setURL("http://uuu17/");

    Exception ee = resultMap.mapException(mau, conn, e1, null);
    assertEquals(RecordingCacheException.class, ee.getClass());
    RecordingCacheException re = (RecordingCacheException)ee;
    assertEquals("http://uuu17/", re.url);
    assertSame(mau, re.au);
    assertSame(e1, re.triggerException);
    assertEquals(-1, re.responseCode);

    assertEquals(CacheException.RetryableNetworkException_3_30S.class,
		 resultMap.mapException(null, "", e2, null).getClass());
  }

  private void checkExceptionClass(int[] checkArray,
                                  Class checkClass) {
    for (int ic = 0; ic < checkArray.length; ic++) {
      CacheException ex = resultMap.mapException(null, "",
						 checkArray[ic], null);
      assertTrue("Wrong class for " + checkArray[ic] + ": " + ex.getClass(),
		 checkClass.isInstance(ex));
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

  static class RecordingCacheException extends CacheException {
    ArchivalUnit au;
    String url;
    int responseCode;
    Exception triggerException;

    RecordingCacheException(ArchivalUnit au,
			    String url,
			    int responseCode,
			    Exception triggerException) {
      this.au = au;
      this.url = url;
      this.responseCode = responseCode;
      this.triggerException = triggerException;
    }
  }

  static public class MyHttpResultHandler implements CacheResultHandler {
    private static int[] m_returnCodes;
    private static Set<Exception> m_exceptions;

    public MyHttpResultHandler() {
    }

    public void init(CacheResultMap crmap) {
      HttpResultMap map = (HttpResultMap)crmap;
      if (m_returnCodes != null) {
        map.storeArrayEntries(m_returnCodes, this);
      }
    }

    void setHandledCodes(int[] returnCodes) {
      m_returnCodes = returnCodes;
    }

    void setHandledExceptions(Set<Exception> exceptions) {
      m_exceptions = exceptions;
    }

    /**
     * handleException
     *
     * @param code int
     * @param connection HttpURLConnection
     * @return CacheException
     */
    public CacheException handleResult(ArchivalUnit au,
				       String url,
				       int responseCode) {
      for (int code : m_returnCodes) {
        if (code == responseCode) {
          return new RecordingCacheException(au, url,
					     responseCode, null);
        }
      }
      return null;
    }

    public CacheException handleResult(ArchivalUnit au,
				       String url,
				       Exception ex) {
      if (m_exceptions.contains(ex)) {
	return new RecordingCacheException(au, url, -1, ex);
      }
      return null;
    }
  }
}
