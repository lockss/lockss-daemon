/*
 * $Id: FuncLockssHttpClient.java,v 1.1 2005-10-02 00:07:18 tlipkis Exp $
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

package org.lockss.util.urlconn;

import java.io.*;
import java.net.*;
import java.net.ProtocolException;
import java.util.*;
import org.lockss.test.*;
import org.lockss.util.*;

/**
 * Network tests for HttpClient, HttpClientUrlConnection,
 * LockssUrlConnectionPool, etc.
 */
public class FuncLockssHttpClient extends LockssTestCase {
  static Logger log = Logger.getLogger("FuncLockssHttpClient");

  static String URL_BAD_PROTOCOL = "noproto://foo.bar/";
  static String URL_NO_DOMAIN = "http://no.such.domain/";
  static String URL_CONN_TIMEOUT = "http://documents.lockss.org:1234/";

  static String EOH = "\r\n\r\n";

  LockssUrlConnectionPool connectionPool;
  LockssUrlConnection conn;

  public void setUp() throws Exception {
    super.setUp();
    connectionPool = new LockssUrlConnectionPool();
  }

  public void tearDown() throws Exception {
    super.tearDown();
  }

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
    connectionPool.setConnectTimeout(1);
    connectionPool.setDataTimeout(10000);
    try {
      conn = UrlUtil.openConnection(LockssUrlConnection.METHOD_GET,
				    URL_CONN_TIMEOUT, connectionPool);
      conn.execute();
      fail("Connect timeout should throw");
    } catch (HttpClientUrlConnection.ConnectionTimeoutException e) {
    }
  }

  public void testOpenNoResponse() throws Exception {
    connectionPool.setConnectTimeout(10000);
    connectionPool.setDataTimeout(100);
    int port = TcpTestUtil.findUnboundTcpPort();
    final ServerSocket server = new ServerSocket(port);
//     SockAbort intr = abortIn(TIMEOUT_SHOULDNT, server);
    ServerThread th = new ServerThread(server);
    th.start();
    try {
      conn = UrlUtil.openConnection(LockssUrlConnection.METHOD_GET,
				    localurl(port), connectionPool);
      conn.execute();
      fail("Connect timeout should throw");
    } catch (java.net.SocketTimeoutException e) {
      assertTrue(th.getNumConnects() + " connects", th.getNumConnects() < 3);
    }

    server.close();
  }

  static String RESP_200_EMPTY = "HTTP/1.1 200 OK\r\n" +
    "Content-Length: 0\r\n" +
    "Keep-Alive: timeout=15, max=100\r\n" +
    "Connection: Keep-Alive\r\n" +
    "Content-Type: text/html\r\n" +
    "\r\n";

  static String RESP_304 = 
    "HTTP/1.1 304 Not Modified\r\n" +
    "Connection: Keep-Alive\r\n" +
    "Keep-Alive: timeout=15, max=98\r\n" +
    "\r\n";

  public void testOneGet() throws Exception {
//     connectionPool.setConnectTimeout(10000);
//     connectionPool.setDataTimeout(10000);
    int port = TcpTestUtil.findUnboundTcpPort();
    final ServerSocket server = new ServerSocket(port);
    ServerThread th = new ServerThread(server);
    th.setResponses(ListUtil.list(RESP_200_EMPTY));
    th.setMaxReads(10);
    th.start();
    conn = UrlUtil.openConnection(LockssUrlConnection.METHOD_GET,
				  localurl(port), connectionPool);
    ConnAbort abort = abortIn(TIMEOUT_SHOULDNT, conn);
    conn.execute();
    abort.cancel();
    assertMatchesRE("^GET / HTTP/", th.getRequest(0));
    assertEquals(200, conn.getResponseCode());
    conn.release();
    server.close();
    assertEquals(1, th.getNumConnects());
  }

  public void test200_304_200() throws Exception {
//     connectionPool.setConnectTimeout(10000);
//     connectionPool.setDataTimeout(10000);
    int port = TcpTestUtil.findUnboundTcpPort();
    final ServerSocket server = new ServerSocket(port);
    ServerThread th = new ServerThread(server);
    th.setResponses(ListUtil.list(RESP_200_EMPTY, RESP_304, RESP_200_EMPTY));
    th.setMaxReads(10);
    th.start();
    conn = UrlUtil.openConnection(LockssUrlConnection.METHOD_GET,
				  localurl(port) + "foo",
				  connectionPool);
    ConnAbort abort = abortIn(TIMEOUT_SHOULDNT, conn);
    conn.execute();
    abort.cancel();
    assertMatchesRE("^GET /foo HTTP/", th.getRequest(0));
    assertEquals(200, conn.getResponseCode());
    conn.release();
    conn = UrlUtil.openConnection(LockssUrlConnection.METHOD_GET,
				  localurl(port) + "bar",
				  connectionPool);
//     ConnAbort abort = abortIn(TIMEOUT_SHOULDNT, conn);
    conn.execute();
//     abort.cancel();
    assertMatchesRE("^GET /bar HTTP/", th.getRequest(1));
    assertEquals(304, conn.getResponseCode());
    conn.release();

    conn = UrlUtil.openConnection(LockssUrlConnection.METHOD_GET,
				  localurl(port) + "bar",
				  connectionPool);
//     ConnAbort abort = abortIn(TIMEOUT_SHOULDNT, conn);
    conn.execute();
//     abort.cancel();
    assertMatchesRE("^GET /bar HTTP/", th.getRequest(2));
    assertEquals(200, conn.getResponseCode());
    conn.release();
    server.close();
    assertEquals(1, th.getNumConnects());
  }

  public void testRetryAfterClose() throws Exception {
//     connectionPool.setConnectTimeout(10000);
//     connectionPool.setDataTimeout(10000);
    int port = TcpTestUtil.findUnboundTcpPort();
    final ServerSocket server = new ServerSocket(port);
    ServerThread th = new ServerThread(server);
    th.setResponses(ListUtil.list(RESP_200_EMPTY, RESP_304, RESP_200_EMPTY));
    th.setMaxReads(2);
    th.start();
    conn = UrlUtil.openConnection(LockssUrlConnection.METHOD_GET,
				  localurl(port) + "foo",
				  connectionPool);
    ConnAbort abort = abortIn(TIMEOUT_SHOULDNT, conn);
    conn.execute();
    abort.cancel();
    assertMatchesRE("^GET /foo HTTP/", th.getRequest(0));
    assertEquals(200, conn.getResponseCode());
    conn.release();
    conn = UrlUtil.openConnection(LockssUrlConnection.METHOD_GET,
				  localurl(port) + "bar",
				  connectionPool);
//     ConnAbort abort = abortIn(TIMEOUT_SHOULDNT, conn);
    conn.execute();
//     abort.cancel();
    assertMatchesRE("^GET /bar HTTP/", th.getRequest(1));
    assertEquals(304, conn.getResponseCode());
    conn.release();

    conn = UrlUtil.openConnection(LockssUrlConnection.METHOD_GET,
				  localurl(port) + "bar",
				  connectionPool);
//     ConnAbort abort = abortIn(TIMEOUT_SHOULDNT, conn);
    conn.execute();
//     abort.cancel();
    assertMatchesRE("^GET /bar HTTP/", th.getRequest(2));
    assertEquals(200, conn.getResponseCode());
    conn.release();
    server.close();
    assertEquals(2, th.getNumConnects());
  }

  public void testRetryAfterClose2() throws Exception {
//     connectionPool.setConnectTimeout(10000);
//     connectionPool.setDataTimeout(10000);
    int port = TcpTestUtil.findUnboundTcpPort();
    final ServerSocket server = new ServerSocket(port);
    ServerThread th = new ServerThread(server);
    th.setResponses(ListUtil.list(RESP_200_EMPTY, RESP_304, RESP_200_EMPTY));
    th.setMaxReads(2);
    th.setDelayClose(true);
    th.start();
    conn = UrlUtil.openConnection(LockssUrlConnection.METHOD_GET,
				  localurl(port) + "foo",
				  connectionPool);
    ConnAbort abort = abortIn(TIMEOUT_SHOULDNT, conn);
    conn.execute();
    abort.cancel();
    assertMatchesRE("^GET /foo HTTP/", th.getRequest(0));
    assertEquals(200, conn.getResponseCode());
    conn.release();
    conn = UrlUtil.openConnection(LockssUrlConnection.METHOD_GET,
				  localurl(port) + "bar",
				  connectionPool);
//     ConnAbort abort = abortIn(TIMEOUT_SHOULDNT, conn);
    conn.execute();
//     abort.cancel();
    assertMatchesRE("^GET /bar HTTP/", th.getRequest(1));
    assertEquals(304, conn.getResponseCode());
    conn.release();

    conn = UrlUtil.openConnection(LockssUrlConnection.METHOD_GET,
				  localurl(port) + "bar",
				  connectionPool);
//     ConnAbort abort = abortIn(TIMEOUT_SHOULDNT, conn);
    conn.execute();
//     abort.cancel();
    assertMatchesRE("^GET /bar HTTP/", th.getRequest(2));
    assertEquals(200, conn.getResponseCode());
    conn.release();
    server.close();
    assertEquals(2, th.getNumConnects());
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
    int nconnects = 0;
    int maxaccepts = 1000;
    int maxreads = 10;
    List responses;
    List requests = new ArrayList();
    boolean delayClose = false;

    ServerThread(ServerSocket srvSock) {
      this.srvSock = srvSock;
    }

    public void run() {
      try {
	for (int ix = 0; ix < maxaccepts; ix++) {
	  Socket sock = srvSock.accept();
	  log.debug3("accepted");
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

    void setMaxAccepts(int n) {
      maxaccepts = n;
    }

    void setMaxReads(int n) {
      maxreads = n;
    }

    void setResponses(List l) {
      responses = l;
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
  }

  /**
   * Close the socket after a timeout
   * @param ms interval to wait before interrupting
   * @param sock the Socket to close
   * @return a SockAbort
   */
  public ConnAbort abortIn(long inMs, LockssUrlConnection conn) {
    ConnAbort sa = new ConnAbort(inMs, conn);
    if (Boolean.getBoolean("org.lockss.test.threadDump")) {
      sa.setThreadDump();
    }
    sa.start();
    return sa;
  }

  /** ConnAbort aborts a connection by closing it
   */
  public class ConnAbort extends DoLater {
    LockssUrlConnection conn;

    ConnAbort(long waitMs, LockssUrlConnection conn) {
      super(waitMs);
      this.conn = conn;
    }

    protected void doit() {
//       try {
	if (conn != null) {
	  log.debug("Closing conn");
	  conn.release();
	}
//       } catch (IOException e) {
// 	log.warning("conn", e);
//       }
    }
  }

}
