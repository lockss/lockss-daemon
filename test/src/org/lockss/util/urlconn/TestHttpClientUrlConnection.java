/*

Copyright (c) 2000-2017 Board of Trustees of Leland Stanford Jr. University,
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
//import java.net.ProtocolException;
import java.util.*;
//import java.util.zip.*;
//import java.text.*;

import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.config.CookieSpecs;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.conn.HttpHostConnectException;
import org.apache.http.conn.socket.LayeredConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.message.BasicHeader;
import org.apache.http.message.BasicHttpResponse;
import org.apache.http.message.BasicStatusLine;
//import org.lockss.daemon.*;
import org.lockss.test.*;
import org.lockss.util.*;
//HC3 import org.apache.commons.httpclient.*;
//HC3 import org.apache.commons.httpclient.cookie.CookiePolicy;
//HC3 import org.apache.commons.httpclient.params.*;
//HC3 import org.apache.commons.httpclient.protocol.*;
//HC3 import org.lockss.util.urlconn.HttpClientUrlConnection.LockssGetMethod;
//HC3 import org.lockss.util.urlconn.HttpClientUrlConnection.LockssGetMethodImpl;

/**
 * Test class for org.lockss.util.urlconn.HttpClientUrlConnection
 */
public class TestHttpClientUrlConnection extends LockssTestCase {
  static Logger log = Logger.getLogger("TestHttpClientUrlConnection");

  MyMockHttpClient client;
//HC3   MyMockGetMethod method;
  MyMockHttpClientUrlConnection conn;
  int newClientCtr;
  String urlString = "http://Test.Url/";
  LockssUrlConnectionPool connectionPool;

  public void setUp() throws Exception {
    super.setUp();
    client = new MyMockHttpClient();
    conn = newConn(urlString);
//HC3     method = conn.getMockMethod();
  }

  public void tearDown() throws Exception {
  }

  MyMockHttpClientUrlConnection newConn(String url) throws IOException {
//HC3     return new MyMockHttpClientUrlConnection(url, client);
    try {
      return new MyMockHttpClientUrlConnection(url);
    } catch (IllegalArgumentException iae) {
      throw new MalformedURLException(iae.getMessage());
    }
  }

  public void testPred() {
    assertTrue(conn.isHttp());
    assertTrue(conn.canProxy());
  }

  String getCookiePolicy(String policy) {
    return HttpClientUrlConnection.getCookiePolicy(policy);
  }

  public void testGetCookiePolicy() throws Exception {
//HC3     assertEquals(CookiePolicy.RFC_2109,
    assertEquals("rfc2109",
		 getCookiePolicy(Constants.COOKIE_POLICY_RFC_2109));

//HC3     assertEquals(CookiePolicy.NETSCAPE,
    assertEquals(CookieSpecs.NETSCAPE,
		 getCookiePolicy(Constants.COOKIE_POLICY_NETSCAPE));

//HC3     assertEquals(CookiePolicy.IGNORE_COOKIES,
    assertEquals(CookieSpecs.IGNORE_COOKIES,
		 getCookiePolicy(Constants.COOKIE_POLICY_IGNORE));

//HC3     assertEquals(CookiePolicy.BROWSER_COMPATIBILITY,
    assertEquals(CookieSpecs.BROWSER_COMPATIBILITY,
		 getCookiePolicy(Constants.COOKIE_POLICY_COMPATIBILITY));
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

  public void testNonAsciiUrl () throws Exception {
    try {
      newConn("http://www.pensoft.net/journals/neobiota/article/1803/" +
              "plant-pathogens-as-biocontrol-agents-of-cirsium-arvense-–-an-overestimated-approach");

    } catch (java.net.MalformedURLException e) {
      fail("Threw MalformedURLException");
    }
  }

  public void testReqProps() {
    Header hdr;

    assertEquals(urlString, conn.getURL());
    conn.setUserAgent("irving");
//HC3     hdr = method.getRequestHeader("user-agent");
    hdr = conn.getRequestHeader("user-agent");
    assertEquals("irving", hdr.getValue());

    conn.setRequestProperty("p2", "v7");
//HC3     hdr = method.getRequestHeader("p2");
    hdr = conn.getRequestHeader("p2");
    assertEquals("v7", hdr.getValue());

//HC3     assertTrue(method.getFollowRedirects());
    assertTrue(conn.getFollowRedirects());
    conn.setFollowRedirects(false);
//HC3     assertFalse(method.getFollowRedirects());
    assertFalse(conn.getFollowRedirects());
    conn.setFollowRedirects(true);
//HC3     assertTrue(method.getFollowRedirects());
    assertTrue(conn.getFollowRedirects());
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
    client.setRes(201);
    conn.execute();
    assertTrue(conn.isExecuted());
    assertEquals(201, conn.getResponseCode());
    Header hdr;
//HC3     hdr = method.getRequestHeader("connection");
    hdr = conn.getRequestHeader("connection");
    assertEquals("keep-alive", hdr.getValue());
//HC3     hdr = method.getRequestHeader("accept");
    hdr = conn.getRequestHeader("accept");
    assertEquals(HttpClientUrlConnection.DEFAULT_ACCEPT_HEADER, hdr.getValue());

  }

  public void testHeaderCharsetDefault() throws Exception {
    // ensure default config
    ConfigurationUtil.resetConfig();
//HC3     assertEquals("ISO-8859-1", method.getParams().getHttpElementCharset());
    assertEquals("ISO-8859-1", conn.getCharset().toString());
    conn.setHeaderCharset("UTF-8");
//HC3     assertEquals("UTF-8", method.getParams().getHttpElementCharset());
    assertEquals("UTF-8", conn.getCharset().toString());
  }

  public void testHeaderCharset() throws Exception {
    // ensure default config
    ConfigurationUtil.addFromArgs(HttpClientUrlConnection.PARAM_HEADER_CHARSET,
				  "UTF-8");
//HC3     assertEquals("UTF-8", method.getParams().getHttpElementCharset());
    assertEquals("UTF-8", conn.getCharset().toString());
    conn.setHeaderCharset("ISO-8859-1");
//HC3     assertEquals("ISO-8859-1", method.getParams().getHttpElementCharset());
    assertEquals("ISO-8859-1", conn.getCharset().toString());
  }

  public void testServerTrustLevelDefault() throws Exception {
    // ensure default config
    ConfigurationUtil.resetConfig();
    client.setRes(201);
    conn.execute();
    assertTrue(conn.isExecuted());
//HC3     assertClass(PermissiveSSLProtocolSocketFactory.class,
    assertClass(SSLConnectionSocketFactory.class,
 		conn.getDefaultSocketFactory());
  }

  public void testServerTrustLevelTrusted() throws Exception {
    ConfigurationUtil.setFromArgs(HttpClientUrlConnection.
				  PARAM_SERVER_TRUST_LEVEL, "Trusted");
    client.setRes(201);
    conn.execute();
    assertTrue(conn.isExecuted());
//HC3     assertClass(SSLProtocolSocketFactory.class,
    assertClass(SSLConnectionSocketFactory.class,
                conn.getDefaultSocketFactory());
  }

  public void testServerTrustLevelSelfSigned() throws Exception {
    ConfigurationUtil.setFromArgs(HttpClientUrlConnection.
				  PARAM_SERVER_TRUST_LEVEL, "SelfSigned");
    client.setRes(201);
    conn.execute();
    assertTrue(conn.isExecuted());
//HC3     assertClass(EasySSLProtocolSocketFactory.class,
    assertClass(SSLConnectionSocketFactory.class,
 		conn.getDefaultSocketFactory());
  }

  public void testServerTrustLevelUntrusted() throws Exception {
    ConfigurationUtil.setFromArgs(HttpClientUrlConnection.
				  PARAM_SERVER_TRUST_LEVEL, "Untrusted");
    client.setRes(201);
    conn.execute();
    assertTrue(conn.isExecuted());
//HC3     assertClass(PermissiveSSLProtocolSocketFactory.class,
    assertClass(SSLConnectionSocketFactory.class,
		conn.getDefaultSocketFactory());
//        conn.getDefaultSocketFactory(ServerTrustLevel.Untrusted));
  }

  public void testServerTrustLevelCustom() throws Exception {
    // HttpClientUrlConnection associates factory with host:port in static
    // DispatchingSSLProtocolSocketFactory, so must use a unique host for
    // this test
    conn = newConn("http://another.host/");
//HC3     method = conn.getMockMethod();

    LockssSecureSocketFactory secureSockFact =
      new LockssSecureSocketFactory(null, null);
    client.setRes(201);
    conn.setSecureSocketFactory(secureSockFact);
    conn.execute();
    assertTrue(conn.isExecuted());
    assertClass(AuthSSLProtocolSocketFactory.class,
		conn.getDefaultSocketFactory());
  }

  public void testResponseStream() throws Exception {
    client.setRes(200);
    String test = "foo123";
    StringInputStream sis = new StringInputStream(test);
//HC3     method.setResponseStream(sis);
    conn.setResponseStream(sis);
    conn.execute();
    assertTrue(conn.isExecuted());
    assertEquals(200, conn.getResponseCode());
    InputStream is = conn.getResponseInputStream();
    assertTrue(is instanceof
	       EofBugInputStream);
    String res = StringUtil.fromInputStream(is);
    assertEquals(test, res);
    assertEquals(0, is.available());
    sis.close();
    assertEquals(0, is.available());
  }

  public void testUnCompressedResponseStream() throws Exception {
    client.setRes(200);
    String test = "foo123";
    StringInputStream sis = new StringInputStream(test);
//HC3     method.setResponseStream(sis);
    conn.setResponseStream(sis);
    conn.execute();
    assertTrue(conn.isExecuted());
    assertEquals(200, conn.getResponseCode());
    InputStream is = conn.getUncompressedResponseInputStream();
    assertTrue(is instanceof
	       EofBugInputStream);
    String res = StringUtil.fromInputStream(is);
    assertEquals(test, res);
    assertEquals(0, is.available());
    sis.close();
    assertEquals(0, is.available());
  }

  public void testCompressedResponseStream() throws Exception {
    client.setRes(200);
    String test = "this is some text to be compressssssed";
    InputStream sis = new GZIPpedInputStream(test);
//HC3     method.setResponseStream(sis);
//HC3     method.setResponseHeader("content-encoding", "gzip");
    conn.setResponseStream(sis);
    conn.setResponseHeader("content-encoding", "gzip");
    conn.execute();
    assertTrue(conn.isExecuted());
    assertEquals(200, conn.getResponseCode());
    InputStream is = conn.getUncompressedResponseInputStream();
    String res = StringUtil.fromInputStream(is);
    assertEquals(test, res);
    assertEquals(0, is.available());
    sis.close();
    assertEquals(0, is.available());
  }

  public void testResponseStreamNull() throws Exception {
    client.setRes(200);
//HC3     method.setResponseStream(null);
    conn.setResponseStream(null);
    conn.execute();
    assertTrue(conn.isExecuted());
    assertEquals(200, conn.getResponseCode());
    assertNull(conn.getResponseInputStream());
  }

  public void testResponseStreamWrapper() throws Exception {
    client.setRes(200);
    String test = "foo123";
    StringInputStream sis = new StringInputStream(test);
//HC3     method.setResponseStream(sis);
    conn.setResponseStream(sis);
    conn.execute();
    assertTrue(conn.isExecuted());
    assertEquals(200, conn.getResponseCode());
    InputStream is = conn.getResponseInputStream();
    assertTrue(is instanceof EofBugInputStream);
  }

  public void testResponseStreamNoWrapper() throws Exception {
    ConfigurationUtil.setFromArgs(HttpClientUrlConnection.
				  PARAM_USE_WRAPPER_STREAM, "false");
    client.setRes(200);
    String test = "foo123";
    StringInputStream sis = new StringInputStream(test);
//HC3     method.setResponseStream(sis);
    conn.setResponseStream(sis);
    conn.execute();
    assertTrue(conn.isExecuted());
    assertEquals(200, conn.getResponseCode());
    InputStream is = conn.getResponseInputStream();
    assertFalse(is instanceof EofBugInputStream);
  }

  public void testExecuteProxy() throws Exception {
    client.setRes(202);
    conn.setProxy("phost", 9009);
    conn.execute();
    assertTrue(conn.isExecuted());
    assertEquals(202, conn.getResponseCode());
    Header hdr;
//HC3     hdr = method.getRequestHeader("connection");
    hdr = conn.getRequestHeader("connection");
    assertEquals("keep-alive", hdr.getValue());
//HC3     hdr = method.getRequestHeader("accept");
    hdr = conn.getRequestHeader("accept");
    assertEquals(HttpClientUrlConnection.DEFAULT_ACCEPT_HEADER, hdr.getValue());
//HC3     HostConfiguration hc = client.getHostConfiguration();
    RequestConfig reqConfig = conn.getRequestConfig();
//HC3     assertEquals("phost", hc.getProxyHost());
    assertEquals("phost", reqConfig.getProxy().getHostName());
//HC3     assertEquals(9009, hc.getProxyPort());
    assertEquals(9009, reqConfig.getProxy().getPort());
//HC3     assertEquals(null, hc.getLocalAddress());
    assertEquals(null, reqConfig.getLocalAddress());

    conn = newConn(urlString + "foo");
    client.setRes(202);
    conn.execute();
    assertTrue(conn.isExecuted());
    assertEquals(202, conn.getResponseCode());
//HC3     hdr = method.getRequestHeader("connection");
    hdr = conn.getRequestHeader("connection");
    assertEquals("keep-alive", hdr.getValue());
//HC3    hdr = method.getRequestHeader("accept");
    hdr = conn.getRequestHeader("accept");
    assertEquals(HttpClientUrlConnection.DEFAULT_ACCEPT_HEADER, hdr.getValue());
//HC3     hc = client.getHostConfiguration();
    reqConfig = conn.getRequestConfig();
//HC3     assertEquals(null, hc.getProxyHost());
//HC3     assertEquals(-1, hc.getProxyPort());
    assertEquals(null, reqConfig.getProxy());
//HC3     assertEquals(null, hc.getLocalAddress());
    assertEquals(null, reqConfig.getLocalAddress());
  }

  public void testResponse() throws Exception {
    String datestr = "Mon, 23 Feb 2004 00:28:11 GMT";
    client.setRes(201);
//HC3     method.setResponseHeader("Date", datestr);
//HC3     method.setResponseHeader("Content-Encoding", "text/html");
//HC3     method.setResponseHeader("Content-type", "type1");
//HC3     method.setStatusText("stext");
//HC3     method.setResponseContentLength(3333);
    conn.setResponseHeader("Date", datestr);
    conn.setResponseHeader("Content-Encoding", "text/html");
    conn.setResponseHeader("Content-type", "type1");
    conn.setStatusText("stext");
    conn.setResponseContentLength(3333);

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

  public void testBindLocalAddress() throws Exception {
    String local ="127.3.42.6";
    client.setRes(202);
    conn.setLocalAddress(IPAddr.getByName(local));
    conn.execute();
    assertTrue(conn.isExecuted());
    assertEquals(202, conn.getResponseCode());
    Header hdr;
//HC3     hdr = method.getRequestHeader("connection");
    hdr = conn.getRequestHeader("connection");
    assertEquals("keep-alive", hdr.getValue());
//HC3     hdr = method.getRequestHeader("accept");
    hdr = conn.getRequestHeader("accept");
    assertEquals(HttpClientUrlConnection.DEFAULT_ACCEPT_HEADER, hdr.getValue());
//HC3     HostConfiguration hc = client.getHostConfiguration();
//HC3     assertEquals(InetAddress.getByName(local), hc.getLocalAddress());
    assertEquals(InetAddress.getByName(local),
	conn.getRequestConfig().getLocalAddress());

    conn = newConn(urlString + "foo");
    client.setRes(202);
    conn.execute();
    assertTrue(conn.isExecuted());
    assertEquals(202, conn.getResponseCode());
//HC3     hdr = method.getRequestHeader("connection");
    hdr = conn.getRequestHeader("connection");
    assertEquals("keep-alive", hdr.getValue());
//HC3     hdr = method.getRequestHeader("accept");
    hdr = conn.getRequestHeader("accept");
    assertEquals(HttpClientUrlConnection.DEFAULT_ACCEPT_HEADER, hdr.getValue());
//HC3     hc = client.getHostConfiguration();
//HC3     assertEquals(null, hc.getLocalAddress());
    assertEquals(null, conn.getRequestConfig().getLocalAddress());
  }

//HC3   public void testNoHttpResponseException() throws Exception {
  public void testIOException() throws Exception {
//HC3    client.setExecuteException(new org.apache.commons.httpclient.NoHttpResponseException("Test"));
    client.setExecuteException(new IOException("Test"));
    try {
      conn.execute();
      fail("execute should have thrown SocketException");
    } catch (java.net.SocketException e) {
      // expected
      assertMatchesRE("Connection reset by peer", e.getMessage());
    }
  }

  public void testConnectTimeoutException() throws Exception {
//HC3     client.setExecuteException(new org.apache.commons.httpclient.ConnectTimeoutException("Test"));
    client.setExecuteException(new HttpHostConnectException(null, null));
    try {
      conn.execute();
      fail("execute should have thrown HttpClientUrlConnection.ConnectionTimeoutException");
    } catch (HttpClientUrlConnection.ConnectionTimeoutException e) {
      // expected
      assertMatchesRE("Host did not respond", e.getMessage());
    }
  }

//HC3   class MyMockHttpClient extends HttpClient {
  class MyMockHttpClient {
    int res1 = -1;
    IOException executeException;
//HC3     HostConfiguration hc = new HostConfiguration();

//HC3     public int executeMethod(HttpMethod method)
//HC3       throws IOException, HttpException  {
    public int executeMethod(int methodCode) throws IOException {
      if (executeException != null) {
	throw executeException;
      }
      int mres = -1;
//HC3       if (method instanceof MyMockGetMethod) {
//HC3 	mres = ((MyMockGetMethod)method).getRes();
      if (methodCode == LockssUrlConnection.METHOD_GET) {
	mres = res1;
      }
      return (mres < 0) ? res1 : mres;
    }

//HC3     public int executeMethod(HostConfiguration hostConfiguration,
//HC3 			     HttpMethod method)
//HC3       throws IOException, HttpException {
//HC3       throw new UnsupportedOperationException();
//HC3     }

    void setRes(int res1) {
      this.res1 = res1;
    }

    void setExecuteException(IOException ex) {
      this.executeException = ex;
    }

//HC3     public HostConfiguration getHostConfiguration() {
//HC3       return hc;
//HC3     }
  }

//HC3   class MyMockGetMethod extends MockHttpMethod
//HC3     implements HttpClientUrlConnection.LockssGetMethod {
//HC3 
//HC3     String url;
//HC3     HttpClientUrlConnection.LockssGetMethodImpl getMeth;
//HC3     Properties respProps = new Properties();
//HC3     String statusText;
//HC3     InputStream respStream;
//HC3     int contentLength = -1;
//HC3     boolean released = false;
//HC3     int res = -1;
//HC3     HttpMethodParams params = new HttpMethodParams();
//HC3     HttpResponse response;
//HC3 
//HC3     public MyMockGetMethod(String url) {
//HC3       super();
//HC3       this.url = url;
//HC3       getMeth = new HttpClientUrlConnection.LockssGetMethodImpl(url);
//HC3     }
//HC3 
//HC3     public HttpMethodParams getParams() {
//HC3       return params;
//HC3     }
//HC3
//HC3     public void setParams(final HttpMethodParams params) {
//HC3       this.params = params;
//HC3     }
//HC3 
//HC3     // this doesn't set the url in getMeth, but that isn't used for
//HC3     // anything in testing
//HC3     void setUrl(String url) {
//HC3       this.url = url;
//HC3     }
//HC3 
//HC3     String getUrl() {
//HC3       return url;
//HC3     }
//HC3 
//HC3     void setRes(int res) {
//HC3       this.res = res;
//HC3     }
//HC3     int getRes() {
//HC3       return res;
//HC3     }
//HC3 
//HC3     public String getPath() {
//HC3       try {
//HC3 	org.apache.commons.httpclient.URI uri =
//HC3 	  new org.apache.commons.httpclient.URI(url, false);
//HC3 	return uri.getPath();
//HC3       } catch(URIException e) {
//HC3 	throw new RuntimeException("getPath couldn't create URI: " + e);
//HC3     }
//HC3 
//HC3     public String getQueryString() {
//HC3       try {
//HC3 	org.apache.commons.httpclient.URI uri =
//HC3 	  new org.apache.commons.httpclient.URI(url, false);
//HC3 	return uri.getQuery();
//HC3       } catch(URIException e) {
//HC3 	throw new RuntimeException("getQueryString couldn't create URI: " + e);
//HC3     }
//HC3 
//HC3     public void setRequestHeader(String headerName, String headerValue) {
//HC3       getMeth.setRequestHeader(headerName, headerValue);
//HC3     }
//HC3 
//HC3     public void setRequestHeader(Header header) {
//HC3       getMeth.setRequestHeader(header.getName(), header.getValue());
//HC3     }
//HC3 
//HC3     public Header getRequestHeader(String headerName) {
//HC3       return getMeth.getRequestHeader(headerName);
//HC3     }
//HC3 
//HC3     public void setFollowRedirects(boolean followRedirects) {
//HC3       getMeth.setFollowRedirects(followRedirects);
//HC3     }
//HC3 
//HC3     public boolean getFollowRedirects() {
//HC3       return getMeth.getFollowRedirects();
//HC3     }
//HC3 
//HC3     public String getStatusText() {
//HC3       return statusText;
//HC3     }
//HC3 
//HC3     public void releaseConnection() {
//HC3       released = true;
//HC3     }
//HC3 
//HC3     void setStatusText(String s) {
//HC3       statusText = s;
//HC3     }
//HC3     void setResponseHeader(String name, String value) {
//HC3       respProps.put(name.toLowerCase(), value);
//HC3     }
//HC3     public Header getResponseHeader(String headerName) {
//HC3       String val = (String)respProps.get(headerName.toLowerCase());
//HC3       log.debug(headerName + ": " + val);
//HC3       if (val != null) {
//HC3 	return new Header(headerName, val);
//HC3       }
//HC3       return null;
//HC3     }
//HC3     public Header[] getRequestHeaders() {
//HC3       Header[] res = new Header[respProps.size()];
//HC3       int ix = 0;
//HC3       for (Iterator iter = respProps.keySet().iterator(); iter.hasNext(); ) {
//HC3 	String key = (String)iter.next();
//HC3 	res[ix++] = new Header(key, (String)respProps.get(key));
//HC3       }
//HC3       return res;
//HC3     }
//HC3 
//HC3     public Header[] getResponseHeaders() {
//HC3       List keys = new ArrayList(respProps.keySet());
//HC3       int n = keys.size();
//HC3       Header[] hdrs = new Header[n];
//HC3       for (int ix = 0; ix < n; ix++) {
//HC3 	String key = (String)keys.get(ix);
//HC3 	hdrs[ix] = new Header(key, respProps.getProperty(key));
//HC3       }
//HC3       return hdrs;
//HC3     }
//HC3 
//HC3     public long getResponseContentLength() {
//HC3       return contentLength;
//HC3     }
//HC3     void setResponseContentLength(int l) {
//HC3       contentLength = l;
//HC3     }
//HC3     /** @deprecated */
//HC3     public HostConfiguration getHostConfiguration() {
//HC3       return new HostConfiguration();
//HC3     }
//HC3 
//HC3     public InputStream getResponseBodyAsStream() throws IOException {
//HC3       return respStream;
//HC3     }
//HC3 
//HC3     public void setResponseStream(InputStream strm) {
//HC3       respStream = strm;
//HC3     }
//HC3 
//HC3     public HttpResponse executeRequest(HttpUriRequest httpUriRequest,
//HC3 	HttpClientContext context) throws ClientProtocolException, IOException {
//HC3       int mres = client.executeMethod();
//HC3       mres = (mres < 0) ? 0 : mres;
//HC3 
//HC3       StatusLine statusLine =
//HC3 	  new BasicStatusLine(HttpVersion.HTTP_1_1, mres, statusText);
//HC3       response = new BasicHttpResponse(statusLine);
//HC3       return response;
//HC3     }
//HC3   }

  class MyMockHttpClientUrlConnection extends HttpClientUrlConnection {
//HC3     MyMockGetMethod mockMeth;
//HC3     List methods = new ArrayList();
    Properties respProps = new Properties();
    String statusText;
    int contentLength = -1;
    InputStream respStream;
    HttpResponse response;

//HC3     MyMockHttpClientUrlConnection(String urlString, MyMockHttpClient client)
    MyMockHttpClientUrlConnection(String urlString)
	throws IOException {
//HC3       super(urlString, client);
      super(urlString);
//HC3       mockMeth = new MyMockGetMethod(urlString);
    }
//HC3     protected LockssGetMethod newLockssGetMethodImpl(String urlString) {
//HC3       if (methods == null || methods.isEmpty()) {
//HC3 	mockMeth = new MyMockGetMethod(urlString);
//HC3       } else {
//HC3 	mockMeth = (MyMockGetMethod)methods.remove(0);
//HC3       }
//HC3       mockMeth.setUrl(urlString);
//HC3       return mockMeth;
//HC3     }
//HC3     MyMockGetMethod getMockMethod() {
//HC3       return mockMeth;
//HC3     }

//HC3     boolean getFollowRedirects() {
//HC3       return mockMeth.getFollowRedirects();
//HC3     }

//HC3     void addMethod(HttpMethod nextMethod) {
//HC3       methods.add(nextMethod);
//HC3     }

//HC3     ProtocolSocketFactory getDefaultSocketFactory() {
    LayeredConnectionSocketFactory getDefaultSocketFactory() {
//HC3       String host = url.getHost();
//HC3       int port = url.getPort();
//HC3       if (port <= 0) {
//HC3         port = UrlUtil.getDefaultPort(url.getProtocol().toLowerCase());
//HC3       }
//
//HC3       return DISP_FACT.getFactory(host, port);
      return getSslConnectionFactory();
    }

    @Override
    protected HttpResponse executeRequest(HttpUriRequest httpUriRequest,
  	HttpClientContext context) throws ClientProtocolException, IOException {
//HC3       return mockMeth.executeRequest(httpUriRequest, context);
      int mres = client.executeMethod(getMethodCode());
      mres = (mres < 0) ? 0 : mres;

      StatusLine statusLine =
      new BasicStatusLine(HttpVersion.HTTP_1_1, mres, statusText);
      response = new BasicHttpResponse(statusLine);
      setResponseCode(mres);
      return response;
    }

    @Override
    public InputStream getResponseBodyAsStream() throws IOException {
//HC3       return mockMeth.getResponseBodyAsStream();
      return respStream;
    }

    @Override
    public String getResponseContentEncoding() {
      return getResponseHeaderValue("content-encoding");
    }

    @Override
    public String getResponseMessage() {
      assertExecuted();
//HC3       return mockMeth.response.getStatusLine().getReasonPhrase();
      return response.getStatusLine().getReasonPhrase();
    }

    @Override
    public String getResponseHeaderValue(String name) {
      assertExecuted();
//HC3       Header header = mockMeth.getResponseHeader(name);
      Header header = getResponseHeader(name);
      return (header != null) ? header.getValue() : null;
    }

    @Override
    public long getResponseContentLength() {
      assertExecuted();
//HC3       return mockMeth.getResponseContentLength();
      return contentLength;
    }

    @Override
    protected Header[] getResponseHeaders() {
//HC3       return mockMeth.getResponseHeaders();
      List<String> keys = (List<String>)new ArrayList(respProps.keySet());
      int n = keys.size();
      Header[] hdrs = new Header[n];

      for (int ix = 0; ix < n; ix++) {
	String key = (String)keys.get(ix);
	hdrs[ix] = new BasicHeader(key, respProps.getProperty(key));
      }

      return hdrs;
    }
  
    public Header getRequestHeader(String headerName) {
      return getRequestBuilder().getFirstHeader(headerName);
    }
 
    public void setResponseStream(InputStream strm) {
      respStream = strm;
    }

    void setResponseHeader(String name, String value) {
      respProps.put(name.toLowerCase(), value);
    }

    void setStatusText(String s) {
      statusText = s;
    }

    void setResponseContentLength(int l) {
      contentLength = l;
    }

    public Header getResponseHeader(String headerName) {
      String val = (String)respProps.get(headerName.toLowerCase());
      log.debug(headerName + ": " + val);

      if (val != null) {
	return new BasicHeader(headerName, val);
      }

      return null;
    }

    public LockssSecureSocketFactory getSecureSocketFactory() {
      return sockFact;
    }
  }
}
