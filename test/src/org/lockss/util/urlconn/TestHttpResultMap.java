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

  int[] InfoCodes = { 100, 101, 103 };
  int[] SuccessCodes = { 200, 203, 304 };
  int[] PermissionCodes = { 401, 402, 403, 407, 495, 496, 511, 526, 561 };
  int[] ClientCodes = { 400, 405, 406, 411, 412, 414, 415, 416, 417, 421,
                        431, 449, 463, 494, 497, 498, 510 };
  int[] DeadLinkCodes = { 204, 404, 410, 451 };
  int[] PermServerCodes = { 501, 505, 506, 520, 525, 530 };
  int[] TransServerCodes = { 409, 413, 429,
                             500, 502, 503, 504, 507, 521, 523 };
  int[] RedirTempCodes = { 302, 303, 307 };
  int[] RedirPermCodes = { 301, 308 };
  int[] NetworkCodes = { 460, 499 };
  int[] ServerLimitCodes = { 509, 529 };
  int[] ServerTimeoutCodes = { 440, 408, 522, 524, 527, 598 };
  int[] UnexpectedCodes = { 201, 206, 418, 444, 530 };
  int[] UnhandledCodes = { 202, 205, 218, 226, 300, 305, 306,
                           419, 420, 426, 428, 430, 450 };
  int[] WebDAVCode = { 102, 207, 208, 422, 423, 424, 508 };


  public void testMapException() {
    // unknown result code
    assertMappings(CacheException.UnknownCodeException.class,
                   new int[] {666});
    assertMappings(null, SuccessCodes);
    assertMappings(CacheException.UnexpectedNoRetryFailException.class,
                   InfoCodes);
    assertMappings(CacheException.PermissionException.class,
                   PermissionCodes);
    assertMappings(CacheException.UnexpectedNoRetryFailException.class,
                   ClientCodes);
    assertMappings(CacheException.NoRetryDeadLinkException.class,
                   DeadLinkCodes);
    assertMappings(CacheException.UnexpectedNoRetryFailException.class,
                   PermServerCodes);
    assertMappings(CacheException.RetrySameUrlException.class,
                   TransServerCodes);
    assertMappings(CacheException.NoRetryPermUrlException.class,
                   RedirPermCodes);
    assertMappings(CacheException.NoRetryTempUrlException.class,
                   RedirTempCodes);
    assertMappings(CacheException.RetryableNetworkException.class,
                   NetworkCodes);
    assertMappings(CacheException.RetrySameUrlException.class,
                   ServerLimitCodes);
    assertMappings(CacheException.RetrySameUrlException.class,
                   ServerTimeoutCodes);
    assertMappings(CacheException.UnexpectedNoRetryFailException.class,
                   UnexpectedCodes);
    assertMappings(CacheException.UnexpectedNoRetryFailException.class,
                   UnhandledCodes);
    assertMappings(CacheException.UnexpectedNoRetryFailException.class,
                   WebDAVCode);

    //check unknown exception
    assertMapping(CacheException.UnknownExceptionException.class,
                  new Exception());

    assertEquals("repo ex",
                 assertMapping(CacheException.UnknownExceptionException.class,
                               new RuntimeException("repo ex"))
                 .getCause().getMessage());

    assertMapping(CacheException.UnknownExceptionException.class,
                  new java.io.IOException("CRLF expected at end of chunk: -1/-1"));

    // mapped exceptions
    assertEquals("Malformed URL: http:::/mal.url", 
                 assertMapping(CacheException.MalformedURLException.class,
                               new MalformedURLException("http:::/mal.url"))
                 .getMessage());

    assertEquals("Unknown host: h.tld",
                 assertMapping(CacheException.RetryableNetworkException.class,
                               new UnknownHostException("h.tld"))
                 .getMessage());


    assertMappings(CacheException.RetryableNetworkException.class,
                   new Exception[] {
                     new SocketException(),
                     new ConnectException(),
                     new NoRouteToHostException(),
                     new LockssUrlConnection.ConnectionTimeoutException("foo"),
                     new SocketTimeoutException(),
                     new javax.net.ssl.SSLException("foo"),
                     new ProtocolException(),
                     new java.nio.channels.ClosedChannelException() });

    assertMapping(CacheException.WarningOnly.class,
                  new ContentValidationException.EmptyFile("Empty File 42"));

    assertMapping(CacheException.WarningOnly.class,
                  new ContentValidationException.LogOnly("len != 3"));

    assertMapping(CacheException.RetrySameUrlException.class,
                  new ContentValidationException.WrongLength("len != 3"));

    // Unmapped subclass of ContentValidationException should map to what
    // ContentValidationException maps to
    assertMapping(CacheException.UnretryableException.class,
                  new UnmappedContentValidationException());

    // Store and verify a new mapping for IOException.
    resultMap.storeMapEntry(IOException.class,
			    CacheException.RetryableNetworkException_3_30S.class);
    assertMapping(CacheException.RetryableNetworkException_3_30S.class,
                  new java.io.IOException("CRLF expected at end of chunk: -1/-1"));

    // Store and verify a new mapping for a category
    resultMap.storeResultCategoryEntries(HttpResultCodeCategory.Timeout,
                                         CacheException.RetryableNetworkException_2_5M.class);

    assertMapping(CacheException.RetryableNetworkException_2_5M.class,
                  new LockssUrlConnection.ConnectionTimeoutException("foo"));
    assertMapping(CacheException.RetryableNetworkException_2_5M.class,
                  new SocketTimeoutException());
    assertMapping(CacheException.RetryableNetworkException_2_5M.class,
                  new LockssUrlConnection.ConnectionTimeoutException(""));
    assertMapping(CacheException.RetryableNetworkException_2_5M.class,
                  new LockssUrlConnection.ConnectionTimeoutException(""));


    // Test categories by changing the mapping for all Retryable
    // errors, check them, and some spot checks that others didn't
    // change
    resultMap.storeResultCategoryEntries(HttpResultCodeCategory.Retryable,
                                         CacheException_42.class);

    assertMappings(CacheException_42.class,
                   TransServerCodes);
    assertMappings(CacheException_42.class,
                   NetworkCodes);
    assertMappings(CacheException_42.class,
                   ServerLimitCodes);
    assertMappings(CacheException_42.class,
                   ServerTimeoutCodes);

    assertMappings(CacheException.UnknownCodeException.class,
                   new int[] {666});
    assertMappings(null, SuccessCodes);
    assertMappings(CacheException.PermissionException.class,
                   PermissionCodes);
    assertMappings(CacheException.UnexpectedNoRetryFailException.class,
                   ClientCodes);
    assertMappings(CacheException.NoRetryDeadLinkException.class,
                   DeadLinkCodes);
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

    assertEquals(CacheException.RetryableNetworkException.class,
		 resultMap.mapException(null, "", e2, null).getClass());
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


  /** Assert that all the codes map to the expected exception class */
  void assertMappings(Class expClass, int[] codes) {
    for (int code : codes) {
      assertMapping(expClass, code);
    }
  }

  /**
   * Assert that the code maps to the expected exception class.
   * @Return the mapped exception in case the test wants to make
   * additional assertions about it */
  CacheException assertMapping(Class expClass, int code) {
    CacheException exception = resultMap.mapException(null, "", code, "Msg");
    if (expClass == null) {
      assertNull("Code " + code + " should be null, was " + exception,
                 exception);
    } else {
      assertClass("Code " + code + " erroneously mapped to", expClass, exception);
    }
    return exception;
  }

  /** Assert that all the exceptions map to the expected exception */
  void assertMappings(Class expClass, Exception[] exceptions) {
    for (Exception ex : exceptions) {
      assertMapping(expClass, ex);
    }
  }

  /** Assert that the exception maps to the expected exception.
   * @Return the mapped exception in case the test wants to make
   * additional assertions about it */
  CacheException assertMapping(Class expClass, Exception ex) {
    CacheException exception = resultMap.mapException(null, "", ex, "Msg");
    if (expClass == null) {
      assertNull(ex + " should map to null, was " + exception, exception);
    } else {
      assertClass(ex + " erroneously mapped to", expClass, exception);
    }
    return exception;
  }

  static class UnmappedContentValidationException
    extends ContentValidationException {
  }

  static class CacheException_42 extends CacheException {
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
