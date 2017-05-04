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

//HC3 import java.io.*;
//HC3 import java.util.*;
import java.util.concurrent.TimeUnit;
import org.lockss.config.CurrentConfig;
import org.lockss.util.*;
//HC3 import org.apache.commons.httpclient.*;
//HC3 import org.apache.commons.httpclient.HttpConnectionManager;
//HC3 import org.apache.commons.httpclient.MultiThreadedHttpConnectionManager;
//HC3 import org.apache.commons.httpclient.SimpleHttpConnectionManager;
//HC3 import org.apache.commons.httpclient.params.*;
//HC3 import org.apache.commons.httpclient.params.HttpConnectionManagerParams;
//HC3 import org.apache.commons.httpclient.protocol.*;
//HC3 import org.apache.commons.httpclient.protocol.SecureProtocolSocketFactory;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.config.Registry;
import org.apache.http.conn.HttpClientConnectionManager;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.LayeredConnectionSocketFactory;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;

/** Encapsulates connection sharing object(s) used by implementations of
 * LockssUrlConnection.  Clients wishing to reuse connections should create
 * an instance of this at the beginning of an activity, then pass it in to
 * {@link UrlUtil#openConnection(String, LockssUrlConnectionPool)}.  This
 * is also the object on which connection and data timeouts must be set, as
 * they are properties of (possibly reused) connections, so aren't settable
 * on individual requests.
 */
public class LockssUrlConnectionPool {
  private static Logger log = Logger.getLogger(LockssUrlConnectionPool.class);
  /* The configuration parameter for the maximum number of total connections
   * supported by this pool.
   */
  static final String PARAM_MAX_TOTAL_CONNECTION_COUNT =
      LockssUrlConnection.PREFIX + "maxTotalConnections";
  static final int DEFAULT_MAX_TOTAL_CONNECTION_COUNT = 1;

  /** HttpClient caches connections inside the HttpClient object */
//HC3   private HttpClient httpClient;
  private int connectTimeout = -1;
  private int dataTimeout = -1;
//HC3   private HttpConnectionManager hcConnManager =
//HC3     new SimpleHttpConnectionManager();
  private HttpClientConnectionManager hcConnManager = null;
//HC3  private SecureProtocolSocketFactory sockFact;
  private LayeredConnectionSocketFactory sockFact;
  private HttpClientContext context = null;
  private boolean keepAlive = false;
  private boolean isMultithreaded = false;
  private int maxTotalConnections = DEFAULT_MAX_TOTAL_CONNECTION_COUNT;
  private int maxConnectionsPerHost = 1;

//HC3   /** Return (creating if necessary) an HttpClient */
//HC3   public HttpClient getHttpClient() {
//HC3     if (httpClient == null) {
//HC3       httpClient = setupNewHttpClient();
//HC3     }
//HC3     return httpClient;
//HC3   }

  /** Return (creating if necessary) an HttpClientContext */
  public HttpClientContext getHttpClientContext() {
    final String DEBUG_HEADER = "getHttpClientContext(): ";
    if (log.isDebug3()) log.debug3(DEBUG_HEADER + "context = " + context);
    if (context == null) {
      context = setupNewHttpClientContext();
    }
    return context;
  }

  public void setMultiThreaded(int maxConn, int maxPerHost) {
//HC3     MultiThreadedHttpConnectionManager cm =
//HC3       new MultiThreadedHttpConnectionManager();
//HC3     HttpConnectionManagerParams params = cm.getParams();
//HC3     params.setMaxTotalConnections(maxConn);
    maxTotalConnections = maxConn;
//HC3     params.setDefaultMaxConnectionsPerHost(maxPerHost);
    maxConnectionsPerHost = maxPerHost;
//HC3     setTimeouts(params);
//HC3     if (httpClient != null) {
//HC3       httpClient.setHttpConnectionManager(cm);
//HC3     }
    isMultithreaded = true;
  }

  public void setSingleThreaded() {
    maxTotalConnections = CurrentConfig.getCurrentConfig()
	.getInt(PARAM_MAX_TOTAL_CONNECTION_COUNT,
	    DEFAULT_MAX_TOTAL_CONNECTION_COUNT);
    maxConnectionsPerHost = 1;
//HC3     HttpConnectionManager cm = new SimpleHttpConnectionManager();
//HC3     setTimeouts(cm);
//HC3     if (httpClient != null) {
//HC3       httpClient.setHttpConnectionManager(cm);
//HC3     }
    isMultithreaded = false;
  }

  /** Set the maximum time to wait for the underlying socket connection to
   * open. */
  public void setConnectTimeout(long connectTimeout) {
    this.connectTimeout = shortenToInt(connectTimeout);
//HC3     hcConnManager.getParams().setConnectionTimeout(this.connectTimeout);
  }

  public int getConnectTimeout() {
    return connectTimeout;
  }

  /** Set the maximum time to wait for data to be returned by the server. */
  public void setDataTimeout(long dataTimeout) {
    this.dataTimeout = shortenToInt(dataTimeout);
//HC3     hcConnManager.getParams().setSoTimeout(this.dataTimeout);
  }

  public int getDataTimeout() {
    return dataTimeout;
  }

  /** Set the so_keepalive value for future connections.  (This should be
   * set for every use of the socket but
   * org.apache.commons.httpclient.HttpConnection doesn't set that option
   * and there no other convenient hook */
  public void setKeepAlive(boolean val) {
//HC3     hcConnManager.getParams().setBooleanParameter(HttpClientUrlConnection.SO_KEEPALIVE,
//HC3 						  val);
    keepAlive = val;
  }

  public boolean getKeepAlive() {
    return keepAlive;
  }

  /** Set the SecureProtocolSocketFactory to be used for authenticated
   * connections. */
//HC3   public void setSecureProtocolSocketFactory(SecureProtocolSocketFactory
  public void setSecureProtocolSocketFactory(LayeredConnectionSocketFactory
					     sockFact) {
    this.sockFact = sockFact;
  }

  /** Return the SecureProtocolSocketFactory to be used for authenticated
   * connections. */
//HC3   public SecureProtocolSocketFactory getSecureProtocolSocketFactory() {
  public LayeredConnectionSocketFactory getSecureProtocolSocketFactory() {
    return sockFact;
  }

  /** Close connections that have been idle for at least idleTime.  If
   * zero, close all connections not currently actuve.  Use this when
   * you're done with the connection pool for a while. */
  public void closeIdleConnections(long idleTime) {
//HC3     hcConnManager.closeIdleConnections(idleTime);
    if (hcConnManager != null) {
      hcConnManager.closeIdleConnections(idleTime, TimeUnit.MILLISECONDS);
    }
  }

//HC3   private HttpClient setupNewHttpClient() {
//HC3     HttpClient client = newHttpClient();
//HC3     client.setHttpConnectionManager(hcConnManager);
//HC3     setTimeouts(hcConnManager);
//HC3     return client;
//HC3   }

  protected HttpClientContext setupNewHttpClientContext() {
    final String DEBUG_HEADER = "setupNewHttpClientContext(): ";
    HttpClientContext clientContext = HttpClientContext.create();
    if (log.isDebug2())
      log.debug2(DEBUG_HEADER + "clientContext = " + clientContext);
    return clientContext;
  }

//HC3   /** Return the HttpClientConnectionManager */
//HC3   public HttpClientConnectionManager getHttpClientConnectionManager() {
//HC3     return hcConnManager;
//HC3   }

  /** Return the HttpClientConnectionManager, creating it if it does not exist.
   */
  public HttpClientConnectionManager getHttpClientConnectionManager(
      Registry<ConnectionSocketFactory> rcsf) {
    if (hcConnManager == null) {
      setupNewHttpClientConnectionManager(rcsf);
    }

    return hcConnManager;
  }

  protected HttpClientConnectionManager setupNewHttpClientConnectionManager(
      Registry<ConnectionSocketFactory> rcsf) {
    PoolingHttpClientConnectionManager phcm = null;
    if (rcsf == null) {
	phcm = new PoolingHttpClientConnectionManager();
    } else {
	phcm = new PoolingHttpClientConnectionManager(rcsf);
    }

    if (!isMultiThreaded()) {
      maxTotalConnections = CurrentConfig.getCurrentConfig().getInt(
	  PARAM_MAX_TOTAL_CONNECTION_COUNT, DEFAULT_MAX_TOTAL_CONNECTION_COUNT);
      maxConnectionsPerHost = 1;
    }

    phcm.setMaxTotal(getMaxTotalConnections());
    phcm.setDefaultMaxPerRoute(getMaxConnectionsPerHost());

    hcConnManager = phcm;
    return hcConnManager;
  }

  public HttpClientContext resetHttpClientContext() {
    context = setupNewHttpClientContext();
    return context;
  }

//HC3   private void setTimeouts(HttpConnectionManager cm) {
//HC3     setTimeouts(cm.getParams());
//HC3   }

//HC3   private void setTimeouts(HttpConnectionManagerParams params) {
//HC3     if (connectTimeout != -1) {
//HC3       params.setConnectionTimeout(connectTimeout);
//HC3     }
//HC3     if (dataTimeout != -1) {
//HC3       params.setSoTimeout(dataTimeout);
//HC3     }
//HC3   }

//HC3   /** For testing only */
//HC3   HttpConnectionManagerParams getConnectionManagerParams() {
//HC3     return hcConnManager.getParams();
//HC3   }

//HC3   protected HttpClient newHttpClient() {
//HC3     return new HttpClient();
//HC3   }

  public void setHttpClientConnectionManager(HttpClientConnectionManager hccm) {
    hcConnManager = hccm;
  }

  public boolean isMultiThreaded() {
    return isMultithreaded;
  }

  public int getMaxTotalConnections() {
    return maxTotalConnections;
  }

  public int getMaxConnectionsPerHost() {
    return maxConnectionsPerHost;
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
