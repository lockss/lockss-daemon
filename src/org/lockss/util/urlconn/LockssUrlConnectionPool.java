/*
 * $Id: LockssUrlConnectionPool.java,v 1.4 2007-07-18 07:12:56 tlipkis Exp $
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
import java.util.*;
import org.lockss.util.*;
import org.apache.commons.httpclient.*;
import org.apache.commons.httpclient.params.*;

/** Encapsulates connection sharing object(s) used by implementations of
 * LockssUrlConnection.  Clients wishing to reuse connections should create
 * an instance of this at the beginning of an activity, then pass it in to
 * {@link UrlUtil#openConnection(String, LockssUrlConnectionPool)}.  This
 * is also the object on which connection and data timeouts must be set, as
 * they are properties of (possibly reused) connections, so aren't settable
 * on individual requests.
 */
public class LockssUrlConnectionPool {
  /** HttpClient caches connections inside the HttpClient object */
  private HttpClient httpClient;
  private int connectTimeout = -1;
  private int dataTimeout = -1;
  private HttpConnectionManager hcConnManager =
    new SimpleHttpConnectionManager();


  /** Return (creating if necessary) an HttpClient */
  public HttpClient getHttpClient() {
    if (httpClient == null) {
      httpClient = setupNewHttpClient();
    }
    return httpClient;
  }

  public void setMultiThreaded(int maxConn, int maxPerHost) {
    MultiThreadedHttpConnectionManager cm =
      new MultiThreadedHttpConnectionManager();
    HttpConnectionManagerParams params = cm.getParams();
    params.setMaxTotalConnections(maxConn);
    params.setDefaultMaxConnectionsPerHost(maxPerHost);
    setTimeouts(params);
    hcConnManager = cm;
    if (httpClient != null) {
      httpClient.setHttpConnectionManager(cm);
    }
  }

  public void setSingleThreaded() {
    HttpConnectionManager cm = new SimpleHttpConnectionManager();
    setTimeouts(cm);
    hcConnManager = cm;
    if (httpClient != null) {
      httpClient.setHttpConnectionManager(cm);
    }
  }

  /** Set the maximum time to wait for the underlying socket connection to
   * open. */
  public void setConnectTimeout(long connectTimeout) {
    this.connectTimeout = shortenToInt(connectTimeout);
    hcConnManager.getParams().setConnectionTimeout(this.connectTimeout);
  }

  /** Set the maximum time to wait for data to be returned by the server. */
  public void setDataTimeout(long dataTimeout) {
    this.dataTimeout = shortenToInt(dataTimeout);
    hcConnManager.getParams().setSoTimeout(this.dataTimeout);
  }

  /** Close connections that have been idle for at least idleTime.  If
   * zero, close all connections not currently actuve.  Use this when
   * you're done with the connection pool for a while. */
  public void closeIdleConnections(long idleTime) {
    hcConnManager.closeIdleConnections(idleTime);
  }

  private HttpClient setupNewHttpClient() {
    HttpClient client = newHttpClient();
    client.setHttpConnectionManager(hcConnManager);
    setTimeouts(hcConnManager);
    return client;
  }

  private void setTimeouts(HttpConnectionManager cm) {
    setTimeouts(cm.getParams());
  }

  private void setTimeouts(HttpConnectionManagerParams params) {
    if (connectTimeout != -1) {
      params.setConnectionTimeout(connectTimeout);
    }
    if (dataTimeout != -1) {
      params.setSoTimeout(dataTimeout);
    }
  }

  /** For testing only */
  HttpConnectionManagerParams getConnectionManagerParams() {
    return hcConnManager.getParams();
  }

  protected HttpClient newHttpClient() {
    return new HttpClient();
  }

  private int shortenToInt(long n) {
    if (n <= Integer.MAX_VALUE && n >= Integer.MIN_VALUE) {
      return (int)n;
    }
    if (n > Integer.MAX_VALUE) {
      return Integer.MAX_VALUE;
    } else {
      return Integer.MIN_VALUE;
    }
  }
}
