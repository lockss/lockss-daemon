/*
 * $Id$
 */

/*

Copyright (c) 2000-2015 Board of Trustees of Leland Stanford Jr. University,
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
import java.net.ProtocolException;
import java.util.*;
import org.apache.oro.text.regex.*;
import org.lockss.test.*;
import org.lockss.util.*;

/**
 * Network tests for HttpClient, HttpClientUrlConnection,
 * LockssUrlConnectionPool, etc.
 */
public class FuncLockssHttpClient extends LockssTestCase {
  static Logger log = Logger.getLogger("FuncLockssHttpClient");

  static String URL_BAD_PROTOCOL = "noproto://foo.bar/";
  static String URL_NO_DOMAIN = "http://no.such.domain.lockss.org/";
  // This url must be one that results in no response from the host.  If
  // there is any response (e.g.,"host not found", "no route to host",
  // "connection refused") the test may fail
  static String URL_CONN_TIMEOUT = "http://example.com:1234/";

  static String EOH = "\r\n\r\n";

  LockssUrlConnectionPool connectionPool;
  LockssUrlConnection conn;
  ConnAbort aborter;

  public void setUp() throws Exception {
    super.setUp();
    connectionPool = new LockssUrlConnectionPool();
    connectionPool.setConnectTimeout(10000);
    aborter = null;
  }

  public void tearDown() throws Exception {
    if (aborter != null) {
      aborter.cancel();
      aborter = null;
    }
    super.tearDown();
  }

  /** Return "http://127.0.0.1:port" */
  String localurl(int port) {
    return "http://127.0.0.1:" + port + "/";
  }

  public void testBadProto() throws Exception {
    try {
      UrlUtil.openConnection(LockssUrlConnection.METHOD_GET,
			     URL_BAD_PROTOCOL, connectionPool);
      fail("Opening malformed url should throw");
    } catch (MalformedURLException e) {
    }
  }

  public void testDnsFail() throws Exception {
    if (isSkipNetworkTests()) return;
    try {
      conn = UrlUtil.openConnection(LockssUrlConnection.METHOD_GET,
				    URL_NO_DOMAIN, connectionPool);
      conn.execute();
      fail("Opening unknown host should throw");
    } catch (UnknownHostException e) {
    }
  }

  public void testRefused() throws Exception {
    int port = TcpTestUtil.findUnboundTcpPort();
    try {
      conn = UrlUtil.openConnection(LockssUrlConnection.METHOD_GET,
				    localurl(port), connectionPool);
      conn.execute();
      fail("Connect refused should throw");
    } catch (ConnectException e) {
      assertMatchesRE("Connection refused", e.getMessage());
    }
  }

  public void testConnectTimeout() throws Exception {
    if (isSkipNetworkTests()) return;
    connectionPool.setConnectTimeout(1);
    try {
      conn = UrlUtil.openConnection(LockssUrlConnection.METHOD_GET,
				    URL_CONN_TIMEOUT, connectionPool);
      aborter = abortIn(TIMEOUT_SHOULDNT, conn);
      conn.execute();
      fail("Expected connect to " + URL_CONN_TIMEOUT +
	   " to timeout, but got: " + conn.getResponseCode() + ": " +
	   conn.getResponseMessage());
    } catch (HttpClientUrlConnection.ConnectionTimeoutException e) {
      // expected
    } catch (Exception e) {
      log.debug2("Unexpected Connect exception", e);
      fail("Expected connect to " + URL_CONN_TIMEOUT +
	   " to timeout, but got: " + e);
      throw e;
    }
  }

  public void testKeepAlive() throws Exception {
    if (isSkipNetworkTests()) return;
    connectionPool.setConnectTimeout(1);
    try {
      conn = UrlUtil.openConnection(LockssUrlConnection.METHOD_GET,
				    URL_CONN_TIMEOUT, connectionPool);
      aborter = abortIn(TIMEOUT_SHOULDNT, conn);
      conn.setKeepAlive(true);
      conn.execute();
      fail("Expected connect to " + URL_CONN_TIMEOUT +
	   " to timeout, but got: " + conn.getResponseCode() + ": " +
	   conn.getResponseMessage());
    } catch (HttpClientUrlConnection.ConnectionTimeoutException e) {
      // expected
    } catch (Exception e) {
      log.debug2("Unexpected Connect exception", e);
      fail("Expected connect to " + URL_CONN_TIMEOUT +
	   " to timeout, but got: " + e);
      throw e;
    }
  }

  // Server opens connection, reads header, doesn't send response
  public void testOpenNoResponse() throws Exception {
    connectionPool.setDataTimeout(100);
    int port = TcpTestUtil.findUnboundTcpPort();
    ServerSocket server = new ServerSocket(port);
    ServerThread th = new ServerThread(server);
    th.start();
    try {
      conn = UrlUtil.openConnection(LockssUrlConnection.METHOD_GET,
				    localurl(port), connectionPool);
      aborter = abortIn(TIMEOUT_SHOULDNT, conn);
      conn.execute();
      fail("Socket timeout should throw");
    } catch (java.net.SocketTimeoutException e) {
      assertTrue(th.getNumConnects() + " connects", th.getNumConnects() < 3);
    }
    th.stopServer();
  }

  // canned responses for next tests

  static String RESP_200 =
    "HTTP/1.1 200 OK\r\n" +
    "Content-Length: 0\r\n" +
    "Keep-Alive: timeout=15, max=100\r\n" +
    "Connection: Keep-Alive\r\n" +
    "Content-Type: text/html\r\n";

  static String RESP_304 =
    "HTTP/1.1 304 Not Modified\r\n" +
    "Connection: Keep-Alive\r\n" +
    "Keep-Alive: timeout=15, max=98\r\n";

  static String RESP_401 =
    "HTTP/1.1 401 Authorization Required\r\n" +
    "Date: Sun, 14 Sep 2008 20:46:12 GMT\r\n" +
    "WWW-Authenticate: Basic realm=\"Middle Earth\"\r\n" +
    "Content-Length: 0\r\n" +
    "Keep-Alive: timeout=15, max=100\r\n" +
    "Connection: Keep-Alive\r\n" +
    "Content-Type: text/html; charset=iso-8859-1\r\n";

  static String RESP_401_DIGEST =
    "HTTP/1.1 401 Authorization Required\r\n" +
    "Date: Sun, 14 Sep 2008 20:46:12 GMT\r\n" +
    "WWW-Authenticate: Digest realm=\"Middle Earth\",\r\n" +
    "    qop=\"auth,auth-int\",\r\n" +
    "    nonce=\"dcd98b7102dd2f0e8b11d0f600bfb0c093\",\r\n" +
    "    opaque=\"5ccc069c403ebaf9f0171e9517f40e41\"\r\n" +
    "Content-Length: 0\r\n" +
    "Keep-Alive: timeout=15, max=100\r\n" +
    "Connection: Keep-Alive\r\n" +
    "Content-Type: text/html; charset=iso-8859-1\r\n";

  // turn the string into a complete http header by appending crlf to it
  String resp(String hdr) {
    return resp(hdr, null);
  }

  // turn the header and content into a complete http response by
  // separating them with crlf
  String resp(String hdr, String cont) {
    StringBuffer sb = new StringBuffer();
    sb.append(hdr);
    sb.append("\r\n");
    if (cont != null) {
      sb.append(cont);
    }
    return sb.toString();
  }

  // Generate a Set-Cookie: header for each of the cookies
  String setCookies(List cookies) {
    StringBuffer sb = new StringBuffer();
    for (Iterator iter = cookies.iterator(); iter.hasNext(); ) {
      sb.append(SET_COOKIE);
      sb.append((String)iter.next());
      sb.append("\r\n");
    }
    return sb.toString();
  }

  static String SET_COOKIE = "Set-Cookie: ";
  static String cookie1 = "monster=42;path=/";
  static String cookie2 = "jar=full;path=/foo/";
  static String cookie3 = "cutter=leaf;path=/";


  /** Assert that the pattern exists in the string, interpreting the string
   * as multiple lines */
  void assertHeaderLine(String expPat, String hdr) {
    Pattern pat = RegexpUtil.uncheckedCompile(expPat,
					      Perl5Compiler.MULTILINE_MASK);
    assertMatchesRE(pat, hdr);
  }

  /** Assert that the pattern does not exist in the string, interpreting
   * the string as multiple lines */
  void assertNoHeaderLine(String expPat, String hdr) {
    Pattern pat = RegexpUtil.uncheckedCompile(expPat,
					      Perl5Compiler.MULTILINE_MASK);
    assertNotMatchesRE(pat, hdr);
  }

  // Do one complete GET operation
  public void testOneGet() throws Exception {
    int port = TcpTestUtil.findUnboundTcpPort();
    ServerSocket server = new ServerSocket(port);
    ServerThread th = new ServerThread(server);
    th.setResponses(resp(RESP_200));
    th.setMaxReads(10);
    th.start();
    conn = UrlUtil.openConnection(LockssUrlConnection.METHOD_GET,
				  localurl(port), connectionPool);
    aborter = abortIn(TIMEOUT_SHOULDNT, conn);
    conn.execute();
    aborter.cancel();
    String req = th.getRequest(0);
    assertMatchesRE("^GET / HTTP/", req);
    // check for the standard default request headers
    assertHeaderLine("^Accept:", req);
    assertHeaderLine("^Connection:", req);
    assertHeaderLine("^User-Agent: Jakarta Commons-HttpClient", req);

    assertEquals(200, conn.getResponseCode());
    conn.release();
    th.stopServer();
    assertEquals(1, th.getNumConnects());
  }

  // Add a manually set cookie
  public void testAddCookie() throws Exception {
    int port = TcpTestUtil.findUnboundTcpPort();
    ServerSocket server = new ServerSocket(port);
    ServerThread th = new ServerThread(server);
    th.setResponses(resp(RESP_200));
    th.setMaxReads(10);
    th.start();
    conn = UrlUtil.openConnection(LockssUrlConnection.METHOD_GET,
				  localurl(port), connectionPool);
    conn.addCookie("127.0.0.1", "/", "cooktop", "eggs");

    aborter = abortIn(TIMEOUT_SHOULDNT, conn);
    conn.execute();
    aborter.cancel();
    String req = th.getRequest(0);
    assertMatchesRE("^GET / HTTP/", req);
    assertEquals(200, conn.getResponseCode());
    assertHeaderLine("^Cookie: .*cooktop=eggs", req);
    conn.release();
    th.stopServer();
    assertEquals(1, th.getNumConnects());
  }

  // Ensure repeated headers get combined into one
  public void testMultiValueHeader() throws Exception {
    ConfigurationUtil.addFromArgs(HttpClientUrlConnection.PARAM_SINGLE_VALUED_HEADERS,
				  "Single");
    int port = TcpTestUtil.findUnboundTcpPort();
    ServerSocket server = new ServerSocket(port);
    ServerThread th = new ServerThread(server);
    th.setResponses(resp(RESP_200 + "Dup: vvv1\r\nDup: vvv2\r\n"
			 + "Single: sss1\r\nSingle: sss2\r\n"));
    th.setMaxReads(10);
    th.start();
    conn = UrlUtil.openConnection(LockssUrlConnection.METHOD_GET,
				  localurl(port), connectionPool);
    aborter = abortIn(TIMEOUT_SHOULDNT, conn);
    conn.execute();
    aborter.cancel();
    String req = th.getRequest(0);
    assertMatchesRE("^GET / HTTP/", req);
    // check for the standard default request headers
    assertHeaderLine("^Accept:", req);
    assertHeaderLine("^Connection:", req);
    assertHeaderLine("^User-Agent: Jakarta Commons-HttpClient", req);

    assertEquals(200, conn.getResponseCode());
    for (int i = 0; true; i++) {
      String key = conn.getResponseHeaderFieldKey(i);
      String val = conn.getResponseHeaderFieldVal(i);
      if ((key == null) && (val == null)) {
	break;
      }
      log.debug2("hdr: " + key + ": " + val);
    }
    CIProperties props = new CIProperties();
    conn.storeResponseHeaderInto(props, "x_");
    assertEquals("Keep-Alive", props.getProperty("x_Connection"));
    assertEquals("Keep-Alive", props.getProperty("x_Connection"));
    assertEquals("vvv1,vvv2", props.getProperty("x_Dup"));
    assertEquals("sss2", props.getProperty("x_Single"));

    conn.release();
    th.stopServer();
    assertEquals(1, th.getNumConnects());
  }

  public void testBasicAuth() throws Exception {
    ConfigurationUtil.setFromArgs(HttpClientUrlConnection.PARAM_USE_PREEMPTIVE_AUTH, "false");
    int port = TcpTestUtil.findUnboundTcpPort();
    ServerSocket server = new ServerSocket(port);
    ServerThread th = new ServerThread(server);
    th.setResponses(resp(RESP_401), resp(RESP_200));
    th.setMaxReads(10);
    th.start();
    conn = UrlUtil.openConnection(LockssUrlConnection.METHOD_GET,
				  localurl(port) + "foo",
				  connectionPool);
    conn.setCredentials("userfoo", "passbar");
    aborter = abortIn(TIMEOUT_SHOULDNT, conn);
    conn.execute();
    aborter.cancel();
    String req1 = th.getRequest(0);
    assertMatchesRE("^GET /foo HTTP/", req1);
    assertNotMatchesRE("Authorization:", req1);
    // second request only should have Authorization: header
    String req2 = th.getRequest(1);
    assertMatchesRE("Authorization: Basic dXNlcmZvbzpwYXNzYmFy", req2);
    assertEquals(200, conn.getResponseCode());
    conn.release();

    th.stopServer();
    assertEquals(1, th.getNumConnects());
  }

  public void testBasicAuthPreemptive() throws Exception {
    ConfigurationUtil.setFromArgs(HttpClientUrlConnection.PARAM_USE_PREEMPTIVE_AUTH, "true");
    int port = TcpTestUtil.findUnboundTcpPort();
    ServerSocket server = new ServerSocket(port);
    ServerThread th = new ServerThread(server);
    th.setResponses(resp(RESP_200));
    th.setMaxReads(10);
    th.start();
    conn = UrlUtil.openConnection(LockssUrlConnection.METHOD_GET,
				  localurl(port) + "foo",
				  connectionPool);
    conn.setCredentials("userfoo", "passbar");
    aborter = abortIn(TIMEOUT_SHOULDNT, conn);
    conn.execute();
    aborter.cancel();
    String req1 = th.getRequest(0);
    assertMatchesRE("^GET /foo HTTP/", req1);
    // first request should have Authorization: header
    assertMatchesRE("Authorization: Basic dXNlcmZvbzpwYXNzYmFy", req1);
    assertEquals(200, conn.getResponseCode());
    conn.release();

    th.stopServer();
    assertEquals(1, th.getNumConnects());
  }

  public void testDigestAuth() throws Exception {
    ConfigurationUtil.setFromArgs(HttpClientUrlConnection.PARAM_USE_PREEMPTIVE_AUTH, "false");
    int port = TcpTestUtil.findUnboundTcpPort();
    ServerSocket server = new ServerSocket(port);
    ServerThread th = new ServerThread(server);
    th.setResponses(resp(RESP_401_DIGEST), resp(RESP_200));
    th.setMaxReads(10);
    th.start();
    conn = UrlUtil.openConnection(LockssUrlConnection.METHOD_GET,
				  localurl(port) + "foo",
				  connectionPool);
    conn.setCredentials("userfoo", "passbar");
    aborter = abortIn(TIMEOUT_SHOULDNT, conn);
    conn.execute();
    aborter.cancel();
    String req1 = th.getRequest(0);
    assertMatchesRE("^GET /foo HTTP/", req1);
    assertNotMatchesRE("Authorization:", req1);
    // second request only should have Authorization: header
    String req2 = th.getRequest(1);
    assertMatchesRE("Authorization: Digest", req2);
    assertMatchesRE("username=\"userfoo\"", req2);
    assertMatchesRE("realm=\"Middle Earth\"", req2);
    assertMatchesRE("nonce=\"dcd98b7102dd2f0e8b11d0f600bfb0c093\"", req2);
    assertMatchesRE("uri=\"/foo\"", req2);
    assertMatchesRE("response=\"[0-9a-fA-F]+\"", req2);
    assertMatchesRE("qop=auth", req2);
    assertMatchesRE("nc=00000001", req2);
    assertMatchesRE("cnonce=\"[0-9a-fA-F]+\"", req2);
    assertMatchesRE("opaque=\"5ccc069c403ebaf9f0171e9517f40e41\"", req2);
    assertEquals(200, conn.getResponseCode());
    conn.release();

    th.stopServer();
    assertEquals(1, th.getNumConnects());
  }

  //Authorization: Digest username="userfoo", realm="Middle Earth", nonce="dcd98b7102dd2f0e8b11d0f600bfb0c093", uri="/foo", response="d6e5a4474a129bfee9dd1bc8faec6d5f", qop=auth, nc=00000001, cnonce="b64c91d1c6426a8d95144416f191e98c", opaque="5ccc069c403ebaf9f0171e9517f40e41"

// Authorization: Digest username="userfoo", realm="Middle Earth\" qop=\"auth,auth-int", nonce="dcd98b7102dd2f0e8b11d0f600bfb0c093\" opaque=\"5ccc069c403ebaf9f0171e9517f40e41", uri="/foo", response="23e1a5f7fd6d4cad5fdb0d4f75f3ce5a"



  public void xtestRepeatAuth() throws Exception {
    int port = TcpTestUtil.findUnboundTcpPort();
    ServerSocket server = new ServerSocket(port);
    ServerThread th = new ServerThread(server);
    th.setResponses(resp(RESP_401), resp(RESP_200), resp(RESP_200));
    th.setMaxReads(10);
    th.start();
    conn = UrlUtil.openConnection(LockssUrlConnection.METHOD_GET,
				  localurl(port) + "foo",
				  connectionPool);
    conn.setCredentials("userfoo", "passbar");
    aborter = abortIn(TIMEOUT_SHOULDNT, conn);
    conn.execute();
    aborter.cancel();
    String req1 = th.getRequest(0);
    assertMatchesRE("^GET /foo HTTP/", req1);
    assertNotMatchesRE("Authorization:", req1);
    // second request should have Authorization: header
    String req2 = th.getRequest(1);
    assertMatchesRE("Authorization: Basic dXNlcmZvbzpwYXNzYmFy", req2);
    assertEquals(200, conn.getResponseCode());

    conn = UrlUtil.openConnection(LockssUrlConnection.METHOD_GET,
				  localurl(port) + "bar",
				  connectionPool);
    conn.setCredentials("userfoo", "passbar");
    aborter = abortIn(TIMEOUT_SHOULDNT, conn);
    conn.execute();
    aborter.cancel();
    // thirs request should also have Authorization: header
    String req3 = th.getRequest(2);
    assertMatchesRE("Authorization: Basic dXNlcmZvbzpwYXNzYmFy", req3);

    conn.release();

    th.stopServer();
    assertEquals(1, th.getNumConnects());
  }

  // Ensure that execute ends with 401 error if incorrect credentials are
  // supplied.
  public void testAuthFail() throws Exception {
    ConfigurationUtil.setFromArgs(HttpClientUrlConnection.PARAM_USE_PREEMPTIVE_AUTH, "false");
    int port = TcpTestUtil.findUnboundTcpPort();
    ServerSocket server = new ServerSocket(port);
    ServerThread th = new ServerThread(server);
    th.setResponses(resp(RESP_401), resp(RESP_401));
    th.setMaxReads(10);
    th.start();
    conn = UrlUtil.openConnection(LockssUrlConnection.METHOD_GET,
				  localurl(port) + "foo",
				  connectionPool);
    conn.setCredentials("userfoo", "passbar");
    aborter = abortIn(TIMEOUT_SHOULDNT, conn);
    conn.execute();
    aborter.cancel();
    String req1 = th.getRequest(0);
    assertMatchesRE("^GET /foo HTTP/", req1);
    assertNotMatchesRE("Authorization:", req1);
    // second request only should have Authorization: header
    String req2 = th.getRequest(1);
    assertMatchesRE("Authorization: Basic dXNlcmZvbzpwYXNzYmFy", req2);
    assertEquals(401, conn.getResponseCode());
    conn.release();

    th.stopServer();
    assertEquals(1, th.getNumConnects());
  }

  // Ensure that execute ends with 401 error if incorrect credentials are
  // supplied.
  public void testAuthFailPreemptive() throws Exception {
    ConfigurationUtil.setFromArgs(HttpClientUrlConnection.PARAM_USE_PREEMPTIVE_AUTH, "true");
    int port = TcpTestUtil.findUnboundTcpPort();
    ServerSocket server = new ServerSocket(port);
    ServerThread th = new ServerThread(server);
    th.setResponses(resp(RESP_401));
    th.setMaxReads(10);
    th.start();
    conn = UrlUtil.openConnection(LockssUrlConnection.METHOD_GET,
				  localurl(port) + "foo",
				  connectionPool);
    conn.setCredentials("userfoo", "passbar");
    aborter = abortIn(TIMEOUT_SHOULDNT, conn);
    conn.execute();
    aborter.cancel();
    String req1 = th.getRequest(0);
    assertMatchesRE("^GET /foo HTTP/", req1);
    assertMatchesRE("Authorization: Basic dXNlcmZvbzpwYXNzYmFy", req1);
    assertEquals(401, conn.getResponseCode());
    conn.release();

    th.stopServer();
    assertEquals(1, th.getNumConnects());
  }

  public void testDontBindLocalAddress() throws Exception {
    int port = TcpTestUtil.findUnboundTcpPort();
    ServerSocket server = new ServerSocket(port);
    ServerThread th = new ServerThread(server);
    th.setResponses(resp(RESP_200));
    th.setMaxReads(10);
    th.start();
    conn = UrlUtil.openConnection(LockssUrlConnection.METHOD_GET,
				  localurl(port), connectionPool);
    aborter = abortIn(TIMEOUT_SHOULDNT, conn);
    conn.execute();
    aborter.cancel();
    InetSocketAddress client = th.getClient(0);
    log.debug("Connection from client: " + client.getAddress());
    assertEquals(InetAddress.getByName("127.0.0.1"), client.getAddress());
    conn.release();
    th.stopServer();
    assertEquals(1, th.getNumConnects());
  }

  public void testBindLocalAddress() throws Exception {
    // OpenBSD does not allow binding arbitrary loopback addresses
    // (12.7.x.x.x) that haven't explicitly been configured.  Use machine's
    // real address to test local address binding
    InetAddress lh = InetAddress.getLocalHost();
    String local = lh.getHostAddress();
    int port = TcpTestUtil.findUnboundTcpPort();
    ServerSocket server = new ServerSocket(port);
    ServerThread th = new ServerThread(server);
    th.setResponses(resp(RESP_200));
    th.setMaxReads(10);
    th.start();
    conn = UrlUtil.openConnection(LockssUrlConnection.METHOD_GET,
				  localurl(port), connectionPool);
    conn.setLocalAddress(IPAddr.getByName(local));
    aborter = abortIn(TIMEOUT_SHOULDNT, conn);
    try {
      conn.execute();
    }
    catch (java.net.BindException be) {
      throw new java.net.BindException(String.format("%s: %s (%s)", be.getMessage(), local, lh));
    }
    aborter.cancel();
    InetSocketAddress client = th.getClient(0);
    log.debug("Connection from client: " + client.getAddress());
    assertEquals(InetAddress.getByName(local), client.getAddress());
    conn.release();
    th.stopServer();
    assertEquals(1, th.getNumConnects());
  }

  // Test that the proxy method doesn't automatically add any request headers
  public void testProxy() throws Exception {
    int port = TcpTestUtil.findUnboundTcpPort();
    ServerSocket server = new ServerSocket(port);
    ServerThread th = new ServerThread(server);
    th.setResponses(resp(RESP_200));
    th.setMaxReads(10);
    th.start();
    conn = UrlUtil.openConnection(LockssUrlConnection.METHOD_PROXY,
				  localurl(port), connectionPool);
    aborter = abortIn(TIMEOUT_SHOULDNT, conn);
    conn.execute();
    aborter.cancel();
    String req = th.getRequest(0);
    assertMatchesRE("^GET / HTTP/", req);
    assertNoHeaderLine("^Accept:", req);
    assertNoHeaderLine("^Connection:", req);
    assertNoHeaderLine("^User-Agent: Jakarta Commons-HttpClient", req);

    assertEquals(200, conn.getResponseCode());
    conn.release();
    th.stopServer();
    assertEquals(1, th.getNumConnects());
  }

  public void test200_304_200() throws Exception {
    int port = TcpTestUtil.findUnboundTcpPort();
    ServerSocket server = new ServerSocket(port);
    ServerThread th = new ServerThread(server);
    th.setResponses(resp(RESP_200), resp(RESP_304), resp(RESP_200));
    th.setMaxReads(10);
    th.start();
    conn = UrlUtil.openConnection(LockssUrlConnection.METHOD_GET,
				  localurl(port) + "foo",
				  connectionPool);
    aborter = abortIn(TIMEOUT_SHOULDNT, conn);
    conn.execute();
    aborter.cancel();
    assertMatchesRE("^GET /foo HTTP/", th.getRequest(0));
    assertEquals(200, conn.getResponseCode());
    conn.release();

    conn = UrlUtil.openConnection(LockssUrlConnection.METHOD_GET,
				  localurl(port) + "bar",
				  connectionPool);
    aborter = abortIn(TIMEOUT_SHOULDNT, conn);
    conn.execute();
    aborter.cancel();
    assertMatchesRE("^GET /bar HTTP/", th.getRequest(1));
    assertEquals(304, conn.getResponseCode());
    conn.release();

    conn = UrlUtil.openConnection(LockssUrlConnection.METHOD_GET,
				  localurl(port) + "bar",
				  connectionPool);
    aborter = abortIn(TIMEOUT_SHOULDNT, conn);
    conn.execute();
    aborter.cancel();
    assertMatchesRE("^GET /bar HTTP/", th.getRequest(2));
    assertEquals(200, conn.getResponseCode());
    conn.release();
    th.stopServer();
    assertEquals(1, th.getNumConnects());
  }

  public void testCookieRFC2109() throws Exception {
    testCookie("RFC2109", false);
  }

  public void testCookieRFC2109A() throws Exception {
    testCookie("RFC2109", true);
  }

  public void testCookieCompatibility() throws Exception {
    testCookie("COMPATIBILITY", false);
  }

  public void testCookieCompatibilityA() throws Exception {
    testCookie("COMPATIBILITY", true);
  }

  public void testCookieNetscape() throws Exception {
    testCookie("NETSCAPE", false);
  }

  public void testCookieNetscapeA() throws Exception {
    testCookie("NETSCAPE", true);
  }

  public void testCookieIgnore() throws Exception {
    testCookie("IGNORE", false);
  }

  public void testCookieIgnoreA() throws Exception {
    testCookie("IGNORE", true);
  }

  public void testCookieDefault() throws Exception {
    testCookie("default", true);
  }

  public void testCookie(String policy, boolean singleHeader)
      throws Exception {
    Properties p = new Properties();
    if ("default".equals(policy)) {
      policy = "compatibility";
      singleHeader = true;
    } else {
      p.put(LockssUrlConnection.PARAM_COOKIE_POLICY, policy);
      p.put(LockssUrlConnection.PARAM_SINGLE_COOKIE_HEADER,
	    singleHeader ? "true" : "false");
    }
    ConfigurationUtil.setCurrentConfigFromProps(p);

    int port = TcpTestUtil.findUnboundTcpPort();
    ServerSocket server = new ServerSocket(port);
    ServerThread th = new ServerThread(server);
    String c = setCookies(ListUtil.list(cookie1, cookie2, cookie3));
    th.setResponses(resp(RESP_200 + c), resp(RESP_200), resp(RESP_200));
    th.setMaxReads(10);
    th.start();

    conn = UrlUtil.openConnection(LockssUrlConnection.METHOD_GET,
				  localurl(port), connectionPool);
    aborter = abortIn(TIMEOUT_SHOULDNT, conn);
    conn.execute();
    aborter.cancel();
    assertMatchesRE("^GET / HTTP/", th.getRequest(0));
    assertNoHeaderLine("^Cookie:", th.getRequest(0));
    assertEquals(200, conn.getResponseCode());
    conn.release();

    conn = UrlUtil.openConnection(LockssUrlConnection.METHOD_GET,
				  localurl(port) + "bar",
				  connectionPool);
    aborter = abortIn(TIMEOUT_SHOULDNT, conn);
    conn.execute();
    aborter.cancel();
    assertMatchesRE("^GET /bar HTTP/", th.getRequest(1));
    assertHasCookie(th.getRequest(1), policy, singleHeader);
    conn.release();

    // Ensure next request still has cookies, even though last response
    // didn't set them
    conn = UrlUtil.openConnection(LockssUrlConnection.METHOD_GET,
				  localurl(port) + "xxx",
				  connectionPool);
    aborter = abortIn(TIMEOUT_SHOULDNT, conn);
    conn.execute();
    aborter.cancel();
    assertMatchesRE("^GET /xxx HTTP/", th.getRequest(2));
    assertHasCookie(th.getRequest(2), policy, singleHeader);
    conn.release();
    th.stopServer();
  }

  void assertHasCookie(String req, String policy, boolean singleHeader) {
    String c1 = null, c2 = null, ver = "";
    if (policy.equalsIgnoreCase("rfc2109")) {
      c1 = "monster=42; \\$Path=/";
      c2 = "cutter=leaf; \\$Path=/";
      ver = "\\$Version=0; ";
    } else {
      c1 = "monster=42";
      c2 = "cutter=leaf";
    }
    if (policy.equalsIgnoreCase("ignore")) {
      assertNoHeaderLine("^Cookie:", req);
    } else if (singleHeader) {
      assertHeaderLine("^Cookie: " + ver + c1 + "; " + c2, req);
    } else {
      assertHeaderLine("^Cookie: " + ver + c1, req);
      assertHeaderLine("^Cookie: " + ver + c2, req);
    }
  }

  public void testRetryAfterClose() throws Exception {
    int port = TcpTestUtil.findUnboundTcpPort();
    ServerSocket server = new ServerSocket(port);
    ServerThread th = new ServerThread(server);
    th.setResponses(resp(RESP_200), resp(RESP_304), resp(RESP_200));
    th.setMaxReads(2);
    th.start();
    conn = UrlUtil.openConnection(LockssUrlConnection.METHOD_GET,
				  localurl(port) + "foo",
				  connectionPool);
    aborter = abortIn(TIMEOUT_SHOULDNT, conn);
    conn.execute();
    aborter.cancel();
    assertMatchesRE("^GET /foo HTTP/", th.getRequest(0));
    assertEquals(200, conn.getResponseCode());
    conn.release();
    conn = UrlUtil.openConnection(LockssUrlConnection.METHOD_GET,
				  localurl(port) + "bar",
				  connectionPool);
    aborter = abortIn(TIMEOUT_SHOULDNT, conn);
    conn.execute();
    aborter.cancel();
    assertMatchesRE("^GET /bar HTTP/", th.getRequest(1));
    assertEquals(304, conn.getResponseCode());
    conn.release();

    conn = UrlUtil.openConnection(LockssUrlConnection.METHOD_GET,
				  localurl(port) + "bar",
				  connectionPool);
    aborter = abortIn(TIMEOUT_SHOULDNT, conn);
    conn.execute();
    aborter.cancel();
    assertMatchesRE("^GET /bar HTTP/", th.getRequest(2));
    assertEquals(200, conn.getResponseCode());
    conn.release();
    assertEquals(2, th.getNumConnects());
    th.stopServer();
  }

  public void testRetryAfterClose2() throws Exception {
    int port = TcpTestUtil.findUnboundTcpPort();
    ServerSocket server = new ServerSocket(port);
    ServerThread th = new ServerThread(server);
    th.setResponses(resp(RESP_200), resp(RESP_304), resp(RESP_200));
    th.setMaxReads(2);
    th.setDelayClose(true);
    th.start();
    conn = UrlUtil.openConnection(LockssUrlConnection.METHOD_GET,
				  localurl(port) + "foo",
				  connectionPool);
    aborter = abortIn(TIMEOUT_SHOULDNT, conn);
    conn.execute();
    aborter.cancel();
    assertMatchesRE("^GET /foo HTTP/", th.getRequest(0));
    assertEquals(200, conn.getResponseCode());
    conn.release();
    conn = UrlUtil.openConnection(LockssUrlConnection.METHOD_GET,
				  localurl(port) + "bar",
				  connectionPool);
    aborter = abortIn(TIMEOUT_SHOULDNT, conn);
    conn.execute();
    aborter.cancel();
    assertMatchesRE("^GET /bar HTTP/", th.getRequest(1));
    assertEquals(304, conn.getResponseCode());
    conn.release();

    conn = UrlUtil.openConnection(LockssUrlConnection.METHOD_GET,
				  localurl(port) + "bar",
				  connectionPool);
    aborter = abortIn(TIMEOUT_SHOULDNT, conn);
    conn.execute();
    aborter.cancel();
    assertMatchesRE("^GET /bar HTTP/", th.getRequest(2));
    assertEquals(200, conn.getResponseCode());
    conn.release();
    assertEquals(2, th.getNumConnects());
    th.stopServer();
  }

  public static String readUntil(Reader rdr, String end) {
    StringBuffer sb = new StringBuffer();
    try {
      do {
	char c = (char)rdr.read();
	sb.append(Character.toString(c));
      } while (!sb.toString().endsWith(end));
    } catch (IOException e) {
    }
    return sb.toString();
  }

  static class ServerThread extends Thread {
    ServerSocket srvSock;
    Socket sock = null;
    int nconnects = 0;
    int maxaccepts = 1000;
    int maxreads = 10;
    List responses;
    List requests = new ArrayList();
    List clients = new ArrayList();
    boolean delayClose = false;

    ServerThread(ServerSocket srvSock) {
      this.srvSock = srvSock;
    }


    public void run() {
      try {
	for (int ix = 0; ix < maxaccepts; ix++) {
	  sock = srvSock.accept();
	  log.debug3("accepted");
	  clients.add(sock.getRemoteSocketAddress());
	  nconnects++;
	  try {
	    InputStream ins = sock.getInputStream();
	    Reader rdr = new InputStreamReader(ins);
	    OutputStream outs = sock.getOutputStream();
	    Writer wrtr = new OutputStreamWriter(outs);
	    for (int readIx = 0; readIx < maxreads; readIx++) {
	      String req = readUntil(rdr, EOH);
	      log.debug3("read " + req);
	      requests.add(req);
	      if (responses != null && responses.size() > 0) {
		String resp = (String)responses.remove(0);
		log.debug3("writing " + resp);
		wrtr.write(resp);
		wrtr.flush();
	      }
	    }
	    if (delayClose) rdr.read();
	  } finally {
	    sock.close();
	  }
	}
      } catch (IOException e) {
      }
    }

    void stopServer() {
      this.interrupt();
      IOUtil.safeClose(srvSock);
      IOUtil.safeClose(sock);
    }

    void setMaxAccepts(int n) {
      maxaccepts = n;
    }

    void setMaxReads(int n) {
      maxreads = n;
    }

    void setResponses(List l) {
      responses = l;
    }

    void setResponses(String r1) {
      responses = ListUtil.list(r1);
    }

    void setResponses(String r1, String r2) {
      responses = ListUtil.list(r1, r2);
    }

    void setResponses(String r1, String r2, String r3) {
      responses = ListUtil.list(r1, r2, r3);
    }

    void setDelayClose(boolean val) {
      delayClose = val;
    }

    int getNumConnects() {
      return nconnects;
    }

    List getRequests() {
      return requests;
    }

    String getRequest(int n) {
      return (String)requests.get(n);
    }

    InetSocketAddress getClient(int n) {
      return (InetSocketAddress)clients.get(n);
    }
  }

  /**
   * Abort the connection after a timeout
   * @param inMs interval to wait before aborting
   * @param conn the LockssUrlConnection to abort
   * @return a ConnAbort
   */
  public ConnAbort abortIn(long inMs, LockssUrlConnection conn) {
    ConnAbort sa = new ConnAbort(inMs, conn);
    if (Boolean.getBoolean("org.lockss.test.threadDump")) {
      sa.setThreadDump();
    }
    sa.start();
    return sa;
  }

  /** ConnAbort calls abort() on a connection after a timeout
   */
  public class ConnAbort extends DoLater {
    LockssUrlConnection conn;

    ConnAbort(long waitMs, LockssUrlConnection conn) {
      super(waitMs);
      this.conn = conn;
    }

    protected void doit() {
      log.debug("Aborting conn");
      conn.abort();
    }
  }
}
