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
import org.lockss.daemon.*;
import org.lockss.plugin.*;
import org.lockss.util.*;
import org.lockss.util.urlconn.*;
import org.lockss.util.urlconn.HttpResultMap.HttpResultCodeCategory;

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

  int[] SuccessCodes = {200, 203, 304};
  int[] RetrySameUrlCodes = { 408, 409, 413, 500, 502, 503, 504};
  int[] MovePermCodes = {301};
  int[] MoveTempCodes = { 307, 303, 302};
  int[] UnimplementedCodes = {};
  int[] PermissionCodes = { 401, 403,  407};
  int[] ExpectedCodes = { 305, 402};
  int[] RetryDeadLinkCodes = {};
  int[] NoRetryDeadLinkCodes= {204, 300, 400, 404, 405, 406, 410};
  int[] UnexpectedFailCodes = {
    201, 202, 205, 206, 306, 411, 412, 416, 417, 501, 505};
  int[] UnexpectedNoFailCodes = { 414, 415 };

  /** Assert that all the codes map to the expected exception */
  void assertCodeMappings(int[] codes, Class cls) {
    for (int code : codes) {
      if (cls == null) {
        assertNull("code " + code + " should be null, was " + cls, cls);
      } else {
        CacheException exception = resultMap.mapException(null, "", code, "foo");
        assertTrue("code:" + code + ", " + exception + " isn't an instance of " + cls,
                   cls.isInstance(exception));
      }
    }
  }

  public void testGetResultCodeException() {
    CacheException exception;
    int ic;

    // unknown result code
    assertCodeMappings(new int[] {666},
                       CacheException.UnknownCodeException.class);
    assertCodeMappings(SuccessCodes, null);
    assertCodeMappings(RetrySameUrlCodes,
                       CacheException.RetrySameUrlException.class);
    assertCodeMappings(MovePermCodes,
                       CacheException.NoRetryPermUrlException.class);
    assertCodeMappings(MoveTempCodes,
                       CacheException.NoRetryTempUrlException.class);
    assertCodeMappings(UnimplementedCodes, null);
    assertCodeMappings(ExpectedCodes,
                       CacheException.ExpectedNoRetryException.class);
    assertCodeMappings(ExpectedCodes,
                       CacheException.ExpectedNoRetryException.class);
    assertCodeMappings(PermissionCodes,
                       CacheException.PermissionException.class);
    assertCodeMappings(NoRetryDeadLinkCodes,
                       CacheException.NoRetryDeadLinkException.class);
    assertCodeMappings(UnexpectedFailCodes,
                       CacheException.UnexpectedNoRetryFailException.class);
    assertCodeMappings(UnexpectedNoFailCodes,
                       CacheException.UnexpectedNoRetryNoFailException.class);
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
    exception = resultMap.mapException(null, "",
				       new MalformedURLException("http:::/mal.url"),
				       "foo");
    assertTrue(exception instanceof
	       CacheException.MalformedURLException);
    assertEquals("http:::/mal.url", exception.getCause().getMessage());
    assertEquals("Malformed URL: http:::/mal.url", exception.getMessage());

    exception =
      resultMap.getMalformedURLException(null, "",
                                         new MalformedURLException("Mal URL: u:x"),
                                         "foo");
    assertTrue(exception instanceof
	       CacheException.MalformedURLException);
    assertEquals("Mal URL: u:x", exception.getCause().getMessage());

    exception =
      resultMap.getRepositoryException(new RuntimeException("repo ex"));
    assertTrue(exception instanceof
	       CacheException.RepositoryException);
    assertEquals("repo ex", exception.getCause().getMessage());

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

    exception = resultMap.mapException(null, "",
				       new ProtocolException("foo bar"),
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

    resultMap.storeMapEntry(IOException.class,
			    CacheException.RetryableNetworkException_3_30S.class);

    exception = resultMap.mapException(null, "",
				       new java.io.IOException("CRLF expected at end of chunk: -1/-1"),
				       "foo");
    assertTrue(exception instanceof
	       CacheException.RetryableNetworkException_3_30S);

  }

  static class UnmappedContentValidationException
    extends ContentValidationException {
  }

  public void testInitExceptionTable() {
    checkExceptionClass(RetrySameUrlCodes,
                        CacheException.RetrySameUrlException.class);
    checkExceptionClass(MovePermCodes,
                        CacheException.NoRetryPermUrlException.class);
    checkExceptionClass(MoveTempCodes,
                        CacheException.NoRetryTempUrlException.class);
    checkExceptionClass(UnimplementedCodes,
                        CacheException.UnimplementedCodeException.class);
    checkExceptionClass(PermissionCodes,
                        CacheException.PermissionException.class);
    checkExceptionClass(ExpectedCodes,
                        CacheException.ExpectedNoRetryException.class);
    checkExceptionClass(RetryDeadLinkCodes,
                        CacheException.RetryDeadLinkException.class);
    checkExceptionClass(NoRetryDeadLinkCodes,
                        CacheException.NoRetryDeadLinkException.class);
    checkExceptionClass(UnexpectedFailCodes,
                        CacheException.UnexpectedNoRetryFailException.class);
    checkExceptionClass(UnexpectedNoFailCodes,
                        CacheException.UnexpectedNoRetryNoFailException.class);
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

  public void testHttpResultHandlerInit() throws PluginException {
    // result handler that will be invoked for selected codes
    CacheResultHandler h1 = new CacheResultHandler() {
        public CacheException handleResult(ArchivalUnit au,
                                           String url,
                                           int responseCode) {
          return new RecordingCacheException(au, url, responseCode, null);
        }
        public CacheException handleResult(ArchivalUnit au,
                                           String url,
                                           Exception ex) {
          return new RecordingCacheException(au, url, -1, ex);
        }};

    MyHttpResultHandler handler = new MyHttpResultHandler() {
        @Override
        public void init(HttpResultMap map) {
          map.storeResultCategoryEntries(HttpResultCodeCategory.RedirectTempCodes,
                                         h1);
          map.storeResultCategoryEntries(HttpResultCodeCategory.AuthCodes,
                                         CacheException.RetryableNetworkException_5_60S.class);
        }};
    handler.init(resultMap);
    resultMap.storeMapEntry(667, CacheException.RetryableNetworkException_5_5M.class);

    for (int code : new int[] {302, 303, 307}) {
      CacheException exception = resultMap.mapException(null, "", code, "foo");
      assertClass("code:" + code, CacheException.class, exception);
      assertMatchesRE("code: " + code, exception.getMessage());
    }
    for (int code : new int[] {401, 403,  407}) {
      CacheException exception = resultMap.mapException(null, "", code, "foo");
      assertClass("code:" + code, CacheException.RetryableNetworkException_5_60S.class, exception);
      assertMatchesRE(code + " foo", exception.getMessage());
    }
    CacheException e667 = resultMap.mapException(null, "", 667, "bar");
    assertClass("code: 667", CacheException.RetryableNetworkException_5_5M.class, e667);
    assertMatchesRE("667 bar", e667.getMessage());
  }

  public void testHttpResultHandlerHandleCode() throws IOException {
    int[] handledCodes = {200, 405, 302};
    CacheException exception;

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

    public String getMessage() {
      return "code: " + responseCode;
    }
  }

  static public class MyHttpResultHandler implements CacheResultHandler {
    private static int[] m_returnCodes;
    private static Set<Exception> m_exceptions;

    public MyHttpResultHandler() {
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
