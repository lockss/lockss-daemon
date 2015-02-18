/*
 * $Id$
 *

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

import java.util.*;
import java.io.*;
import java.net.*;

import org.lockss.util.*;

/** Encapsulates Java's HttpURLConnection impelementation as a
 * LockssUrlConnection.  Provides native java-compatible http behavior.
 */
public class JavaHttpUrlConnection extends JavaUrlConnection {
  private static Logger log = Logger.getLogger("JavaHttpUrlConn");

  private HttpURLConnection hurlConn;
  private int responseCode;
  private String responseMessage;

  public JavaHttpUrlConnection(String urlString) throws IOException {
    this(new URL(urlString));
  }

  public JavaHttpUrlConnection(URL url) throws IOException {
    super(url);
    hurlConn = (HttpURLConnection)getURLConnection();
    log.debug("new JavaHttpUrlConnection(" + url + ")");
  }

  public boolean isHttp() {
    return true;
  }

  public void execute() throws IOException {
    assertNotExecuted();
    isExecuted = true;
    hurlConn.connect();
    // cause any IOException to happen now
    responseCode = hurlConn.getResponseCode();
    responseMessage = hurlConn.getResponseMessage();
  }

  public boolean canProxy() {
    return true;
  }

  public void setFollowRedirects(boolean followRedirects) {
    HttpURLConnection.setFollowRedirects(followRedirects);
  }

  public int getResponseCode() {
    assertExecuted();
    return responseCode;
  }

  public String getResponseMessage() {
    assertExecuted();
    return responseMessage;
  }
}
// This hack allows use of a per-connection proxy in Java 1.3.  It doesn't
// work, or even compile, in 1.4, and while a similar hack would be easy, I
// don't immediately see an easy way to do it that works in both 1.3 and
// 1.4.  Unless we abandon HttpClient and revert back to Java URLConnection
// we don't need this functionality, so it isn't worth the time.

// Use:
//  URL url = new URL(null, uc.getUrl(), new MyHandler(id, proxyPort));
//  HttpURLConnection conn = (HttpURLConnection)url.openConnection();


// class MyHandler extends sun.net.www.protocol.http.Handler {
//   public MyHandler (String proxy, int port) {
//     super(proxy, port);
//   }

//   protected java.net.URLConnection openConnection(URL u) throws IOException {
//     return new MyHttpURLConnection(u, this);
//   }

//   public String getProxyHost() {
//     return proxy;
//   }

//   public int getProxyPort() {
//     return proxyPort;
//   }
// }

// class MyHttpURLConnection extends sun.net.www.protocol.http.HttpURLConnection {
//   protected MyHttpURLConnection(URL u, MyHandler handler) throws IOException {
//     super(u, handler);
//   }

//   public void connect() throws IOException {
//     if (connected) {
//       return;
//     }
//     try {
//       // Always create new connection when proxying
//       MyHandler h = (MyHandler)handler;
//       http = getProxiedClient(url, h.getProxyHost(), h.getProxyPort());
//       ps = (PrintStream)http.getOutputStream();
//     } catch (IOException e) {
//       throw e;
//     }
//     // constructor to HTTP client calls openserver
//     connected = true;
//   }
// }
