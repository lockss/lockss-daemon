/*
 * $Id: BaseLockssUrlConnection.java,v 1.7 2006-08-02 02:51:40 tlipkis Exp $
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
import java.text.*;
import org.lockss.util.*;
import org.apache.commons.httpclient.*;
import org.apache.commons.httpclient.methods.*;

/** Common functionality for implementations of LockssUrlConnection */
public abstract class BaseLockssUrlConnection implements LockssUrlConnection {
  private static Logger log = Logger.getLogger("BaseLockssUrlConnection");

  protected String urlString;
  protected boolean isExecuted = false;
  protected String proxyHost = null;
  protected int proxyPort;
  protected IPAddr localAddress = null;

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

  public void setLocalAddress(IPAddr localAddress) {
    assertNotExecuted();
    this.localAddress = localAddress;
  }

  public void setFollowRedirects(boolean followRedirects) {
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

  // Preferred date format according to RFC 2068(HTTP1.1),
  // RFC 822 and RFC 1123
  public static final SimpleDateFormat GMT_DATE_FORMAT =
    new SimpleDateFormat ("EEE, dd MMM yyyy HH:mm:ss 'GMT'");
  static {
    GMT_DATE_FORMAT.setTimeZone(TimeZone.getTimeZone("GMT"));
  }

  public void setUserAgent(String value) {
    setRequestProperty("user-agent", value);
  }

  public void setRequestPropertyDate(String key, long time) {
    assertNotExecuted();
    Date date = new Date(time);
    setRequestProperty(key, GMT_DATE_FORMAT.format(date));
  }

  public void addRequestProperty(String key, String value) {
    throw new UnsupportedOperationException();
  }

  public String getActualUrl() {
    assertExecuted();
    return urlString;
  }
}
