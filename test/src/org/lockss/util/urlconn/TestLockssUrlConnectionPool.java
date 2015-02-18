/*
 * $Id$
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
import java.util.*;
import org.lockss.test.*;
import org.lockss.util.*;
import org.apache.commons.httpclient.*;
import org.apache.commons.httpclient.params.*;

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

  public void testCreateAndCacheHttpClient() {
    assertEquals(0, newClientCtr);
    HttpClient client = pool.getHttpClient();
    assertEquals(1, newClientCtr);
    assertSame(client, pool.getHttpClient());
    assertEquals(1, newClientCtr);
  }

  private int getConnectionTimeout(HttpClient client) {
    HttpParams params = client.getHttpConnectionManager().getParams();
    return params.getIntParameter(HttpConnectionParams.CONNECTION_TIMEOUT, -1);
  }

  private int getTimeout(HttpClient client) {
    HttpParams params = client.getHttpConnectionManager().getParams();
    return params.getIntParameter(HttpConnectionParams.SO_TIMEOUT, -1);
  }

  public void testTimeoutsAfter() {
    assertEquals(0, newClientCtr);
    HttpClient client = pool.getHttpClient();
    assertEquals(1, newClientCtr);
    assertEquals(-1, getConnectionTimeout(client));
    assertEquals(-1, getTimeout(client));
    pool.setConnectTimeout(444);
    assertEquals(444, getConnectionTimeout(client));
    assertEquals(-1, getTimeout(client));
    pool.setDataTimeout(666);
    assertEquals(444, getConnectionTimeout(client));
    assertEquals(666, getTimeout(client));
  }

  public void testTimeoutsBefore() {
    pool.setConnectTimeout(123);
    pool.setDataTimeout(321);
    assertEquals(0, newClientCtr);
    HttpClient client = pool.getHttpClient();
    assertEquals(1, newClientCtr);
    assertEquals(123, getConnectionTimeout(client));
    assertEquals(321, getTimeout(client));
    pool.setConnectTimeout(444);
    assertEquals(444, getConnectionTimeout(client));
    assertEquals(321, getTimeout(client));
    pool.setDataTimeout(666);
    assertEquals(444, getConnectionTimeout(client));
    assertEquals(666, getTimeout(client));
  }

  public void testStaleCheckedEnbled() {
    assertTrue(pool.getConnectionManagerParams().isStaleCheckingEnabled());
  }

  public void testMultiThreaded() {
    pool.setMultiThreaded(8, 3);
    HttpClient client = pool.getHttpClient();
    HttpConnectionManager mgr = client.getHttpConnectionManager();
    assertTrue(mgr instanceof MultiThreadedHttpConnectionManager);
    MultiThreadedHttpConnectionManager mtm =
      (MultiThreadedHttpConnectionManager)mgr;
    assertEquals(8, mtm.getParams().getMaxTotalConnections());
    assertEquals(3, mtm.getParams().getDefaultMaxConnectionsPerHost());
  }

  class MyMockHttpClient extends HttpClient {
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
    protected HttpClient newHttpClient() {
      newClientCtr++;
//       return super.newHttpClient();
      return new MyMockHttpClient();
    }
  }
}
