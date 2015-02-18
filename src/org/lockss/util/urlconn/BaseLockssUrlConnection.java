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

import java.io.*;
import java.net.URL;
import java.util.*;
import java.text.*;

import org.lockss.util.*;

/** Common functionality for implementations of LockssUrlConnection */
public abstract class BaseLockssUrlConnection implements LockssUrlConnection {
  private static Logger log = Logger.getLogger("BaseLockssUrlConnection");

  protected String urlString;
  protected URL url;
  protected boolean isExecuted = false;
  protected String proxyHost = null;
  protected int proxyPort;
  protected IPAddr localAddress = null;
  protected LockssSecureSocketFactory sockFact = null;
  protected boolean isAuthenticatedServer = false;

  protected BaseLockssUrlConnection(String url) throws IOException {
    this.urlString = url;
    this.url = new URL(url);
  }

  /** Return the URL
   * @return the URL
   */
  public String getURL() {
    return urlString;
  }

  public boolean isExecuted() {
    return isExecuted;
  }

  protected void assertExecuted() {
    if (!isExecuted) {
      throw
	new IllegalStateException("LockssUrlConnection not yet executed");
    }
  }

  protected void assertNotExecuted() {
    if (isExecuted) {
      throw
	new IllegalStateException("LockssUrlConnection has been executed");
    }
  }

  public void setProxy(String host, int port) throws CantProxyException {
    if (!canProxy()) {
      throw new CantProxyException();
    }
    assertNotExecuted();
    proxyHost = host;
    proxyPort = port;
  }

  public void setSecureSocketFactory(LockssSecureSocketFactory sockFact) {
    assertNotExecuted();
    this.sockFact = sockFact;
  }

  public boolean isAuthenticatedServer() {
    return isAuthenticatedServer;
  }

  public void setLocalAddress(IPAddr localAddress) {
    assertNotExecuted();
    this.localAddress = localAddress;
  }

  public void setFollowRedirects(boolean followRedirects) {
    throw new UnsupportedOperationException();
  }

  public void setCookiePolicy(String policy) {
    throw new UnsupportedOperationException();
  }

  public final void addCookie(String name, String value) {
    addCookie("/", name, value);
  }

  public final void addCookie(String path, String name, String value) {
    addCookie(url.getHost(), path, name, value);
  }

  public void addCookie(String domain, String path, String name, String value) {
    throw new UnsupportedOperationException();
  }

  public void setIfModifiedSince(long time) {
    log.debug3("setIfModifiedSince(" + time + ")");
    setRequestPropertyDate("If-Modified-Since", time);
  }

  public void setIfModifiedSince(String time) {
    log.debug3("setIfModifiedSince(" + time + ")");
    setRequestProperty("If-Modified-Since", time);
  }

  public void setCredentials(String username, String password) {
    throw new UnsupportedOperationException();
  }

  public void setUserAgent(String value) {
    setRequestProperty("user-agent", value);
  }

  public void setRequestPropertyDate(String key, long time) {
    assertNotExecuted();
    Date date = new Date(time);
    setRequestProperty(key, DateTimeUtil.GMT_DATE_FORMATTER.format(date));
  }

  public void addRequestProperty(String key, String value) {
    throw new UnsupportedOperationException();
  }

  /** Return the uncompressed input (undoing any Content-Encoding:).  If
   * the encoding is unsupported, returns the unmodified stream */
  public InputStream getUncompressedResponseInputStream()
      throws IOException {
    InputStream in = getResponseInputStream();
    String contentEncoding = getResponseContentEncoding();
    try {
      return StreamUtil.getUncompressedInputStream(in, contentEncoding);
    } catch (UnsupportedEncodingException e) {
      log.warning("Unsupported Content-Encoding: " + contentEncoding);
      return in;
    }
  }

  public String getActualUrl() {
    assertExecuted();
    return urlString;
  }
}
