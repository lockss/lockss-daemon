/*
 * $Id: TestHttpClientUrlConnection.java,v 1.12 2005-07-25 01:21:05 tlipkis Exp $
 */

/*

Copyright (c) 2000-2003 Board of Trustees of Leland Stanford Jr. University,
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
import java.text.*;
import org.lockss.daemon.*;
import org.lockss.test.*;
import org.lockss.util.*;
import org.apache.commons.httpclient.*;

/**
 * Test class for org.lockss.util.urlconn.HttpClientUrlConnection
 */
public class TestHttpClientUrlConnection extends LockssTestCase {
  static Logger log = Logger.getLogger("TestHttpClientUrlConnection");

  MyMockHttpClient client;
  MyMockGetMethod method;
  MyMockHttpClientUrlConnection conn;
  int newClientCtr;
  String urlString = "http://Test.Url/";

  public void setUp() throws Exception {
    super.setUp();
    client = new MyMockHttpClient();
    conn = newConn(urlString);
    method = conn.getMockMethod();
  }

  public void tearDown() throws Exception {
  }

  MyMockHttpClientUrlConnection newConn(String url) throws IOException {
    return new MyMockHttpClientUrlConnection(url, client);
  }

  public void testPred() {
    assertTrue(conn.isHttp());
    assertTrue(conn.canProxy());
  }

  public void testMalformedUrl() throws Exception {
    try {
      newConn("nsp://foo.bar/");
      fail("Failed to throw MalformedURLException");
    } catch (java.net.MalformedURLException e) {}
    try {
      newConn("http://foo.bar/a<");
      fail("Failed to throw MalformedURLException");
    } catch (java.net.MalformedURLException e) {}
  }

  void assertProperException(Class cls, String msg, Exception origE) {
    HttpRecoverableException re =
      new HttpRecoverableException(origE.toString());
    IOException e = conn.exceptionFromRecoverableException(re);
    assertTrue("'" + e + "' is not an instance of " + cls.getName(),
	       cls.isInstance(e));
    assertEquals(msg, e.getMessage());
  }

  public void testExceptionFromRecoverableException() {
    assertProperException(BindException.class, "foo msg",
			  new BindException("foo msg"));
    assertProperException(InterruptedIOException.class, "bar",
			  new InterruptedIOException("bar"));
    assertProperException(ConnectException.class, "bar",
			  new ConnectException("bar"));
    assertProperException(NoRouteToHostException.class, "bar",
			  new NoRouteToHostException("bar"));
    assertProperException(ProtocolException.class, "bar",
			  new ProtocolException("bar"));
    assertProperException(UnknownHostException.class, "bar",
			  new UnknownHostException("bar"));
    // This one should stay an HttpRecoverableException
    assertProperException(HttpRecoverableException.class,
			  "java.io.IOException: bar",
			  new IOException("bar"));
  }

  public void testReqProps() {
    Header hdr;

    assertEquals(urlString, conn.getURL());
    conn.setUserAgent("irving");
    hdr = method.getRequestHeader("user-agent");
    assertEquals("irving", hdr.getValue());

    conn.setRequestProperty("p2", "v7");
    hdr = method.getRequestHeader("p2");
    assertEquals("v7", hdr.getValue());

    assertTrue(method.getFollowRedirects());
    conn.setFollowRedirects(false);
    assertFalse(method.getFollowRedirects());
    conn.setFollowRedirects(true);
    assertTrue(method.getFollowRedirects());
  }

  public void testStateNotEx() throws Exception {
    assertFalse(conn.isExecuted());
    try {
      conn.getResponseCode();
      fail("Failed to throw IllegalStateException");
    } catch (IllegalStateException e) {}
    try {
      conn.getResponseMessage();
      fail("Failed to throw IllegalStateException");
    } catch (IllegalStateException e) {}
    try {
      conn.getResponseDate();
      fail("Failed to throw IllegalStateException");
    } catch (IllegalStateException e) {}
    try {
      conn.getResponseContentLength();
      fail("Failed to throw IllegalStateException");
    } catch (IllegalStateException e) {}
    try {
      conn.getResponseContentType();
      fail("Failed to throw IllegalStateException");
    } catch (IllegalStateException e) {}
    try {
      conn.getResponseContentEncoding();
      fail("Failed to throw IllegalStateException");
    } catch (IllegalStateException e) {}
    try {
      conn.getResponseHeaderValue("foo");
      fail("Failed to throw IllegalStateException");
    } catch (IllegalStateException e) {}
    conn.execute();
  }

  public void testStateEx() throws Exception {
    assertFalse(conn.isExecuted());
    conn.execute();
    assertTrue(conn.isExecuted());
    try {
      conn.setUserAgent("foo");
      fail("Failed to throw IllegalStateException");
    } catch (IllegalStateException e) {}
    try {
      conn.setRequestProperty("foo", "bar");
      fail("Failed to throw IllegalStateException");
    } catch (IllegalStateException e) {}
    try {
      conn.setFollowRedirects(true);
      fail("Failed to throw IllegalStateException");
    } catch (IllegalStateException e) {}
  }

  public void testExecute() throws Exception {
    client.setRes(201, 202);
    conn.execute();
    assertTrue(conn.isExecuted());
    assertEquals(201, conn.getResponseCode());
    Header hdr;
    hdr = method.getRequestHeader("connection");
    assertEquals("keep-alive", hdr.getValue());
    hdr = method.getRequestHeader("accept");
    assertEquals(HttpClientUrlConnection.ACCEPT_STRING, hdr.getValue());

  }

  public void testResponseStream() throws Exception {
    client.setRes(200, 200);
    String test = "foo123";
    StringInputStream sis = new StringInputStream(test);
    method.setResponseStream(sis);
    conn.execute();
    assertTrue(conn.isExecuted());
    assertEquals(200, conn.getResponseCode());
    InputStream is = conn.getResponseInputStream();
    assertTrue(is instanceof
	       HttpClientUrlConnection.EofMonitoringInputStream);
    String res = StringUtil.fromInputStream(is);
    assertEquals(test, res);
    assertEquals(0, is.available());
    sis.close();
    assertEquals(0, is.available());
  }

  public void testResponseStreamNull() throws Exception {
    client.setRes(200, 200);
    method.setResponseStream(null);
    conn.execute();
    assertTrue(conn.isExecuted());
    assertEquals(200, conn.getResponseCode());
    assertNull(conn.getResponseInputStream());
  }

  public void testResponseStreamNoWrapper() throws Exception {
    ConfigurationUtil.setFromArgs(HttpClientUrlConnection.
				  PARAM_USE_WRAPPER_STREAM, "false");
    client.setRes(200, 200);
    String test = "foo123";
    StringInputStream sis = new StringInputStream(test);
    method.setResponseStream(sis);
    conn.execute();
    assertTrue(conn.isExecuted());
    assertEquals(200, conn.getResponseCode());
    InputStream is = conn.getResponseInputStream();
    assertFalse(is instanceof
		HttpClientUrlConnection.EofMonitoringInputStream);
  }

  public void testExecuteProxy() throws Exception {
    client.setRes(201, 202);
    conn.setProxy("phost", 9009);
    conn.execute();
    assertTrue(conn.isExecuted());
    assertEquals(202, conn.getResponseCode());
    Header hdr;
    hdr = method.getRequestHeader("connection");
    assertEquals("keep-alive", hdr.getValue());
    hdr = method.getRequestHeader("accept");
    assertEquals(HttpClientUrlConnection.ACCEPT_STRING, hdr.getValue());
    HostConfiguration hc = client.getHostConfiguration();
    assertEquals("phost", hc.getProxyHost());
    assertEquals(9009, hc.getProxyPort());
  }

  public void testResponse() throws Exception {
    String datestr = "Mon, 23 Feb 2004 00:28:11 GMT";
    client.setRes(201, 202);
    method.setResponseHeader("Date", datestr);
    method.setResponseHeader("Content-Encoding", "text/html");
    method.setResponseHeader("Content-type", "type1");
    method.setStatusText("stext");
    method.setResponseContentLength(3333);

    conn.execute();
    assertEquals(201, conn.getResponseCode());
    assertEquals("stext", conn.getResponseMessage());
    assertEquals(1077496091000L, conn.getResponseDate());
    assertEquals(3333, conn.getResponseContentLength());
    assertEquals("text/html", conn.getResponseContentEncoding());
    assertEquals("type1", conn.getResponseContentType());
    assertEquals(urlString, conn.getActualUrl());
    Properties props = new Properties();
    conn.storeResponseHeaderInto(props, "x_");
    Properties eprops = new Properties();
    eprops.put("x_content-type", "type1");
    eprops.put("x_date", datestr);
    eprops.put("x_content-encoding", "text/html");
    assertEquals(eprops, props);
  }

  public void testRedirect() throws Exception {
    String redir = "http://redirected.com/foo.bar";

    client.setRes(301, 301);
    method.setResponseHeader("location", redir);

    MyMockGetMethod meth2 = new MyMockGetMethod(null);
    meth2.setRes(200);
    conn.addMethod(meth2);

    conn.execute();
    assertEquals(200, conn.getResponseCode());
    assertEquals(redir, conn.getActualUrl());
    MyMockGetMethod cmeth = conn.getMockMethod();
    assertSame(meth2, cmeth);
    assertEquals(redir, cmeth.getUrl());
  }

  public void testRedirectWithQuery() throws Exception {
    String redir = "http://redirected.com/foo.bar?p=v";

    client.setRes(301, 301);
    method.setResponseHeader("location", redir);

    MyMockGetMethod meth2 = new MyMockGetMethod(null);
    meth2.setRes(200);
    conn.addMethod(meth2);

    conn.execute();
    assertEquals(200, conn.getResponseCode());
    assertEquals(redir, conn.getActualUrl());
    MyMockGetMethod cmeth = conn.getMockMethod();
    assertSame(meth2, cmeth);
    assertEquals(redir, cmeth.getUrl());
  }

  public void testMaxRedirect() throws Exception {
    String redir = "http://redirected.com/foo.bar";

    client.setRes(301, 301);
    method.setResponseHeader("location", redir);

    for (int ix=0; ix < ( 1 + HttpClientUrlConnection.MAX_REDIRECTS); ix++) {
      MyMockGetMethod meth2 = new MyMockGetMethod(null);
      meth2.setRes(301);
      meth2.setResponseHeader("location", redir);
      conn.addMethod(meth2);
    }
    MyMockGetMethod meth2 = new MyMockGetMethod(null);
    meth2.setRes(200);
    conn.addMethod(meth2);

    conn.execute();
    assertEquals(301, conn.getResponseCode());
  }

  public void xtestConnTimeout() throws Exception {
    String url0 = "http://10.222.111.99:43215/";
    String url = "http://sul-lockss28.stanford.edu:43215/";
    LockssUrlConnectionPool pool = new LockssUrlConnectionPool();
    pool.setConnectTimeout(600000);
    pool.setDataTimeout(30000);

    LockssUrlConnection conn;
    HttpClient client = pool.getHttpClient();
//     HttpClient client = new HttpClient();
    conn = new HttpClientUrlConnection(LockssUrlConnection.METHOD_GET,
				       url, client);
    conn.execute();
    log.debug("resp: " + conn.getResponseCode());
    log.debug("respMsg: " + conn.getResponseMessage());
    assertEquals(200, conn.getResponseCode());
  }

  public void xtestLegalUrl() throws Exception {
    String url = "http://sul-lockss28.stanford.edu/fo|o.bar";
    org.apache.commons.httpclient.URI u =
      new org.apache.commons.httpclient.URI(url.toCharArray());
    LockssUrlConnection conn;
    HttpClient client = new HttpClient();
    conn = new HttpClientUrlConnection(LockssUrlConnection.METHOD_GET,
				       url, client);
  }

  class MyMockHttpClient extends HttpClient {
    int res1 = -1;
    int res2 = -2;
    HostConfiguration hc = null;

    public int executeMethod(HttpMethod method)
	throws IOException, HttpException  {
      int mres = -1;
      if (method instanceof MyMockGetMethod) {
	mres = ((MyMockGetMethod)method).getRes();
      }
      return (mres < 0) ? res1 : mres;
    }

    public int executeMethod(HostConfiguration hostConfiguration,
			     HttpMethod method)
	throws IOException, HttpException {
      hc = hostConfiguration;
      int mres = -1;
      if (method instanceof MyMockGetMethod) {
	mres = ((MyMockGetMethod)method).getRes();
      }
      return (mres < 0) ? res2 : mres;
    }

    void setRes(int res1, int res2) {
      this.res1 = res1;
      this.res2 = res2;
    }

    public HostConfiguration getHostConfiguration() {
      return hc;
    }
  }

  class MyMockGetMethod extends MockHttpMethod
    implements HttpClientUrlConnection.LockssGetMethod {

    String url;
    HttpClientUrlConnection.LockssGetMethodImpl getMeth;
    Properties respProps = new Properties();
    String statusText;
    InputStream respStream;
    int contentLength = -1;
    boolean released = false;
    int res = -1;

    public MyMockGetMethod(String url) {
      super();
      this.url = url;
      getMeth = new HttpClientUrlConnection.LockssGetMethodImpl(url);
    }

    // this doesn't set the url in getMeth, but that isn't used for
    // anything in testing
    void setUrl(String url) {
      this.url = url;
    }

    String getUrl() {
      return url;
    }

    void setRes(int res) {
      this.res = res;
    }
    int getRes() {
      return res;
    }

    public String getPath() {
      try {
	org.apache.commons.httpclient.URI uri =
	  new org.apache.commons.httpclient.URI(url);
	return uri.getPath();
      } catch(URIException e) {
	throw new RuntimeException("getPath couldn't create URI: " + e);
      }
    }

    public String getQueryString() {
      try {
	org.apache.commons.httpclient.URI uri =
	  new org.apache.commons.httpclient.URI(url);
	return uri.getQuery();
      } catch(URIException e) {
	throw new RuntimeException("getQueryString couldn't create URI: " + e);
      }
    }

    public void setRequestHeader(String headerName, String headerValue) {
      getMeth.setRequestHeader(headerName, headerValue);
    }

    public void setRequestHeader(Header header) {
      getMeth.setRequestHeader(header.getName(), header.getValue());
    }

    public Header getRequestHeader(String headerName) {
      return getMeth.getRequestHeader(headerName);
    }

    public void setFollowRedirects(boolean followRedirects) {
      getMeth.setFollowRedirects(followRedirects);
    }

    public boolean getFollowRedirects() {
      return getMeth.getFollowRedirects();
    }

    public String getStatusText() {
      return statusText;
    }

    public void releaseConnection() {
      released = true;
    }

    void setStatusText(String s) {
      statusText = s;
    }
    void setResponseHeader(String name, String value) {
      respProps.put(name.toLowerCase(), value);
    }
    public Header getResponseHeader(String headerName) {
      String val = (String)respProps.get(headerName.toLowerCase());
      log.debug(headerName + ": " + val);
      if (val != null) {
	return new Header(headerName, val);
      }
      return null;
    }
    public Header[] getRequestHeaders() {
      Header[] res = new Header[respProps.size()];
      int ix = 0;
      for (Iterator iter = respProps.keySet().iterator(); iter.hasNext(); ) {
	String key = (String)iter.next();
	res[ix++] = new Header(key, (String)respProps.get(key));
      }
      return res;
    }

    public Header[] getResponseHeaders() {
      List keys = new ArrayList(respProps.keySet());
      int n = keys.size();
      Header[] hdrs = new Header[n];
      for (int ix = 0; ix < n; ix++) {
	String key = (String)keys.get(ix);
	hdrs[ix] = new Header(key, respProps.getProperty(key));
      }
      return hdrs;
    }

    public int getResponseContentLength() {
      return contentLength;
    }
    void setResponseContentLength(int l) {
      contentLength = l;
    }
    public HostConfiguration getHostConfiguration() {
      return new HostConfiguration();
    }

    public InputStream getResponseBodyAsStream() throws IOException {
      return respStream;
    }

    public void setResponseStream(InputStream strm) {
      respStream = strm;
    }

  }

  class MyMockHttpClientUrlConnection extends HttpClientUrlConnection {
    MyMockGetMethod mockMeth;
    List methods = new ArrayList();

    MyMockHttpClientUrlConnection(String urlString, MyMockHttpClient client)
	throws IOException {
      super(urlString, client);
    }
    protected LockssGetMethod newLockssGetMethodImpl(String urlString) {
      if (methods == null || methods.isEmpty()) {
	mockMeth = new MyMockGetMethod(urlString);
      } else {
	mockMeth = (MyMockGetMethod)methods.remove(0);
      }
      mockMeth.setUrl(urlString);
      return mockMeth;
    }
    MyMockGetMethod getMockMethod() {
      return mockMeth;
    }

    boolean getFollowRedirects() {
      return mockMeth.getFollowRedirects();
    }

    void addMethod(HttpMethod nextMethod) {
      methods.add(nextMethod);
    }
  }
}
