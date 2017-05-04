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

//import java.io.*;
//import java.util.*;
//HC3 import org.apache.commons.httpclient.*;
//HC3 import org.apache.commons.httpclient.params.*;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.conn.HttpClientConnectionManager;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.lockss.test.*;
//import org.lockss.util.*;

/**
 * Test class for org.lockss.util.urlconn.LockssUrlConnectionPool
 */
public class TestLockssUrlConnectionPool extends LockssTestCase {

  LockssUrlConnectionPool pool;
  int newClientCtr;

  public void setUp() throws Exception {
    super.setUp();
    pool = new MyMockLockssUrlConnectionPool();
    newClientCtr = 0;

  }

  public void tearDown() throws Exception {
  }

//HC3   public void testCreateAndCacheHttpClient() {
  public void testCreateAndCacheHttpClientContext() {
    assertEquals(0, newClientCtr);
//HC3     HttpClient client = pool.getHttpClient();
    HttpClientContext clientContext = pool.getHttpClientContext();
    assertEquals(1, newClientCtr);
//HC3     assertSame(client, pool.getHttpClient());
    assertSame(clientContext, pool.getHttpClientContext());
    assertEquals(1, newClientCtr);
  }

//HC3   private int getConnectionTimeout(HttpClient client) {
//HC3     HttpParams params = client.getHttpConnectionManager().getParams();
//HC3     return params.getIntParameter(HttpConnectionParams.CONNECTION_TIMEOUT, -1);
//HC3   }

//HC3   private int getTimeout(HttpClient client) {
//HC3     HttpParams params = client.getHttpConnectionManager().getParams();
//HC3     return params.getIntParameter(HttpConnectionParams.SO_TIMEOUT, -1);
//HC3   }

  public void testTimeoutsAfter() {
    assertEquals(0, newClientCtr);
//HC3     HttpClient client = pool.getHttpClient();
    pool.getHttpClientContext();
    assertEquals(1, newClientCtr);
//HC3     assertEquals(-1, getConnectionTimeout(client));
    assertEquals(-1, pool.getConnectTimeout());
//HC3     assertEquals(-1, getTimeout(client));
    assertEquals(-1, pool.getDataTimeout());
    pool.setConnectTimeout(444);
//HC3     assertEquals(444, getConnectionTimeout(client));
    assertEquals(444, pool.getConnectTimeout());
//HC3     assertEquals(-1, getTimeout(client));
    assertEquals(-1, pool.getDataTimeout());
    pool.setDataTimeout(666);
//HC3     assertEquals(444, getConnectionTimeout(client));
    assertEquals(444, pool.getConnectTimeout());
//HC3     assertEquals(666, getTimeout(client));
    assertEquals(666, pool.getDataTimeout());
  }

  public void testTimeoutsBefore() {
    pool.setConnectTimeout(123);
    pool.setDataTimeout(321);
    assertEquals(0, newClientCtr);
//HC3     HttpClient client = pool.getHttpClient();
    pool.getHttpClientContext();
    assertEquals(1, newClientCtr);
//HC3     assertEquals(123, getConnectionTimeout(client));
    assertEquals(123, pool.getConnectTimeout());
//HC3     assertEquals(321, getTimeout(client));
    assertEquals(321, pool.getDataTimeout());
    pool.setConnectTimeout(444);
//HC3     assertEquals(444, getConnectionTimeout(client));
    assertEquals(444, pool.getConnectTimeout());
//HC3     assertEquals(321, getTimeout(client));
    assertEquals(321, pool.getDataTimeout());
    pool.setDataTimeout(666);
//HC3     assertEquals(444, getConnectionTimeout(client));
    assertEquals(444, pool.getConnectTimeout());
//HC3     assertEquals(666, getTimeout(client));
    assertEquals(666, pool.getDataTimeout());
  }

  public void testStaleCheckedEnbled() {
//HC3    assertTrue(pool.getConnectionManagerParams().isStaleCheckingEnabled());
    // From https://hc.apache.org/httpcomponents-client-ga/httpclient/apidocs/org/apache/http/client/config/RequestConfig.html:
    // The default setting for stale connection checking changed to false, and the feature was deprecated starting with version 4.4.
    assertFalse(RequestConfig.DEFAULT.isStaleConnectionCheckEnabled());
  }

  public void testSingleThreaded() {
    ConfigurationUtil.resetConfig();
    HttpClientConnectionManager mgr = pool.getHttpClientConnectionManager(null);
    assertTrue(mgr instanceof PoolingHttpClientConnectionManager);
    assertFalse(pool.isMultiThreaded());

    PoolingHttpClientConnectionManager mtm = null;
    try {
      mtm = (PoolingHttpClientConnectionManager)mgr;
      assertEquals(1, mtm.getMaxTotal());
      assertEquals(1, mtm.getDefaultMaxPerRoute());
    } finally {
      mtm.close();
    }

    ConfigurationUtil.addFromArgs(
	LockssUrlConnectionPool.PARAM_MAX_TOTAL_CONNECTION_COUNT, "2");

    pool.setHttpClientConnectionManager(null);
    mgr = pool.getHttpClientConnectionManager(null);
    assertTrue(mgr instanceof PoolingHttpClientConnectionManager);
    assertFalse(pool.isMultiThreaded());

    try {
      mtm = (PoolingHttpClientConnectionManager)mgr;
      assertEquals(2, mtm.getMaxTotal());
      assertEquals(1, mtm.getDefaultMaxPerRoute());
    } finally {
      mtm.close();
    }

    pool.setHttpClientConnectionManager(null);
    pool.setSingleThreaded();
    mgr = pool.getHttpClientConnectionManager(null);
    assertTrue(mgr instanceof PoolingHttpClientConnectionManager);
    assertFalse(pool.isMultiThreaded());

    try {
      mtm = (PoolingHttpClientConnectionManager)mgr;
      assertEquals(2, mtm.getMaxTotal());
      assertEquals(1, mtm.getDefaultMaxPerRoute());
    } finally {
      mtm.close();
    }

    ConfigurationUtil.resetConfig();
    pool.setHttpClientConnectionManager(null);
    pool.setSingleThreaded();
    mgr = pool.getHttpClientConnectionManager(null);
    assertTrue(mgr instanceof PoolingHttpClientConnectionManager);
    assertFalse(pool.isMultiThreaded());

    try {
      mtm = (PoolingHttpClientConnectionManager)mgr;
      assertEquals(1, mtm.getMaxTotal());
      assertEquals(1, mtm.getDefaultMaxPerRoute());
    } finally {
      mtm.close();
    }
  }

  public void testMultiThreaded() {
    pool.setMultiThreaded(8, 3);
//HC3     HttpClient client = pool.getHttpClient();
//HC3     HttpConnectionManager mgr = client.getHttpConnectionManager();
    HttpClientConnectionManager mgr = pool.getHttpClientConnectionManager(null);
//HC3     assertTrue(mgr instanceof MultiThreadedHttpConnectionManager);
    assertTrue(mgr instanceof PoolingHttpClientConnectionManager);
    assertTrue(pool.isMultiThreaded());
//HC3     MultiThreadedHttpConnectionManager mtm =
//HC3       (MultiThreadedHttpConnectionManager)mgr;
    PoolingHttpClientConnectionManager mtm = null;
    try {
      mtm = (PoolingHttpClientConnectionManager)mgr;
//HC3     assertEquals(8, mtm.getParams().getMaxTotalConnections());
      assertEquals(8, mtm.getMaxTotal());
//HC3     assertEquals(3, mtm.getParams().getDefaultMaxConnectionsPerHost());
      assertEquals(3, mtm.getDefaultMaxPerRoute());
    } finally {
      mtm.close();
    }
  }

//HC3   class MyMockHttpClient extends HttpClient {
  class MyMockHttpClientContext extends HttpClientContext {
    int cto = -1;
    int dto = -1;

    /** @deprecated */
    public void setConnectionTimeout(int n) {
      cto = n;
    }
    /** @deprecated */
    public void setTimeout(int n) {
      dto = n;
    }
    public int getTimeout() {
      return dto;
    }
    public int getConnectionTimeout() {
      return cto;
    }
  }


  class MyMockLockssUrlConnectionPool extends LockssUrlConnectionPool {
//HC3     protected HttpClient newHttpClient() {
    protected HttpClientContext setupNewHttpClientContext() {
      newClientCtr++;
//       return super.newHttpClient();
//HC3       return new MyMockHttpClient();
      return new MyMockHttpClientContext();
    }
  }
}
