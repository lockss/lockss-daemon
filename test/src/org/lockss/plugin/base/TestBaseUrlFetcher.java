/*

 Copyright (c) 2000-2021 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.plugin.base;

import java.io.*;
import java.net.*;
import java.util.*;
import java.text.*;
import org.apache.commons.lang3.tuple.*;

import org.lockss.plugin.*;
import org.lockss.plugin.UrlFetcher.FetchResult;
import org.lockss.daemon.*;
import org.lockss.test.*;
import org.lockss.app.*;
import org.lockss.util.*;
import org.lockss.util.urlconn.*;
import org.lockss.repository.*;
import org.lockss.crawler.*;
import org.lockss.crawler.PermissionRecord.PermissionStatus;
import org.lockss.config.*;

import static org.lockss.util.DateTimeUtil.GMT_DATE_FORMATTER;

public class TestBaseUrlFetcher extends LockssTestCase {

  protected static Logger logger = Logger.getLogger("TestBaseUrlFetcher");

  private static final SimpleDateFormat GMT_DATE_PARSER =
    new SimpleDateFormat ("EEE, dd MMM yyyy HH:mm:ss 'GMT'");
  static {
    GMT_DATE_PARSER.setTimeZone(TimeZoneUtil.getExactTimeZone("GMT"));
  }

  TestableBaseUrlFetcher fetcher;
  MockCachedUrlSet mcus;
  MockPlugin plugin;

  private MyMockArchivalUnit mau;
  private MockLockssDaemon theDaemon;
  private MockCrawler.MockCrawlerFacade mcf;
  private int pauseBeforeFetchCounter;

  private MockNodeManager nodeMgr = new MockNodeManager();

  private static final String TEST_URL = "http://www.example.com/testDir/leaf1";
  private boolean saveDefaultSuppressStackTrace;

  public void setUp() throws Exception {
    super.setUp();

    String tempDirPath = getTempDir().getAbsolutePath() + File.separator;
    CIProperties props = new CIProperties();
    props.setProperty(LockssRepositoryImpl.PARAM_CACHE_LOCATION, tempDirPath);
    ConfigurationUtil.setCurrentConfigFromProps(props);

    theDaemon = getMockLockssDaemon();
    theDaemon.getHashService();

    mau = new MyMockArchivalUnit();
    
    mau.setConfiguration(ConfigManager.newConfiguration());

    plugin = new MockPlugin();
    plugin.initPlugin(theDaemon);
    mau.setPlugin(plugin);

    theDaemon.setNodeManager(nodeMgr, mau);

    mcus = new MockCachedUrlSet(TEST_URL);
    mcus.setArchivalUnit(mau);
    mau.setAuCachedUrlSet(mcus);
    mcf = new MockCrawler().new MockCrawlerFacade(mau);
    fetcher = new TestableBaseUrlFetcher(mcf, TEST_URL);
    fetcher.setUrlConsumerFactory(new PassiveUrlConsumerFactory());
    saveDefaultSuppressStackTrace =
      CacheException.setDefaultSuppressStackTrace(false);
    getMockLockssDaemon().getAlertManager();
  }

  public void tearDown() throws Exception {
    TimeBase.setReal();
    CacheException.setDefaultSuppressStackTrace(saveDefaultSuppressStackTrace);
    super.tearDown();
  }

  public void testFetch() throws IOException {
    pauseBeforeFetchCounter = 0;
    fetcher._input = new StringInputStream("test stream");
    fetcher._headers = new CIProperties();
    fetcher.setUrlConsumerFactory(new PassiveUrlConsumerFactory());
    // should cache
    assertEquals(UrlFetcher.FetchResult.FETCHED, fetcher.fetch());
    assertEquals(1, pauseBeforeFetchCounter);
    //assertNull(fetcher.getInfoException());
  }

  public void testCacheEmpty() throws IOException {
    pauseBeforeFetchCounter = 0;

    fetcher._input = new StringInputStream("");
    fetcher._headers = new CIProperties();
    fetcher.setUrlConsumerFactory(new PassiveUrlConsumerFactory());
    // should cache
    assertEquals(UrlFetcher.FetchResult.FETCHED, fetcher.fetch());
    assertEquals(1, pauseBeforeFetchCounter);
    //assertClass(CacheException.WarningOnly.class,
		//fetcher.getInfoException());
    //assertEquals("Empty file stored",
		//fetcher.getInfoException().getMessage());
  }

  public void testCacheEmptyPluginDoesntCare() throws IOException {
    HttpResultMap resultMap = (HttpResultMap)plugin.getCacheResultMap();
    resultMap.storeMapEntry(ContentValidationException.EmptyFile.class,
			    CacheSuccess.class);
    pauseBeforeFetchCounter = 0;

    fetcher._input = new StringInputStream("");
    fetcher._headers = new CIProperties();
    fetcher.setUrlConsumerFactory(new PassiveUrlConsumerFactory());
    // should cache
    assertEquals(UrlFetcher.FetchResult.FETCHED, fetcher.fetch());
    assertEquals(1, pauseBeforeFetchCounter);
    //assertNull(fetcher.getInfoException());
  }

  public void testReCacheWCookie() throws IOException {
    pauseBeforeFetchCounter = 0;

    fetcher._input = new StringInputStream("test stream");
    CIProperties headers = new CIProperties();
    headers.put(BaseUrlFetcher.SET_COOKIE_HEADER, "blah");
    fetcher._headers = headers;
    fetcher.setUrlConsumerFactory(new PassiveUrlConsumerFactory());
    // should cache
    assertEquals(UrlFetcher.FetchResult.FETCHED, fetcher.fetch());
    assertEquals(2, fetcher.getUncachedInputStreamCount);
    assertEquals(2, pauseBeforeFetchCounter);
  }

  public void testReCacheWCookieOverride() throws IOException {
    mau.setShouldRefetchOnCookies(false);
    pauseBeforeFetchCounter = 0;

    fetcher._input = new StringInputStream("test stream");
    CIProperties headers = new CIProperties();
    headers.put(BaseUrlFetcher.SET_COOKIE_HEADER, "blah");
    fetcher._headers = headers;
    fetcher.setUrlConsumerFactory(new PassiveUrlConsumerFactory());
    // should cache
    assertEquals(UrlFetcher.FetchResult.FETCHED, fetcher.fetch());
    assertEquals(1, fetcher.getUncachedInputStreamCount);
    assertEquals(1, pauseBeforeFetchCounter);
  }

  public void testLastModifiedfetch() throws IOException {
    // add the 'cached' version
    fetcher.setUrlConsumerFactory(new PassiveUrlConsumerFactory());
    CIProperties cachedProps = new CIProperties();
    cachedProps.setProperty(CachedUrl.PROPERTY_LAST_MODIFIED,
			    GMT_DATE_FORMATTER.format(new Date(12345)));
    mau.addUrl(TEST_URL, true, true, cachedProps);

    TimeBase.setSimulated(10000);
    fetcher._input = new StringInputStream("test stream");
    fetcher._headers = new CIProperties();
    // shouldn't cache
    assertEquals(UrlFetcher.FetchResult.FETCHED_NOT_MODIFIED, fetcher.fetch());

    TimeBase.step(5000);
    fetcher.reset();
    fetcher._input = new StringInputStream("test stream");
    fetcher._headers = new CIProperties();
    // should cache now
    assertEquals(FetchResult.FETCHED, fetcher.fetch());

    TimeBase.setReal();
  }

  public void testForcefetch() throws IOException {
    fetcher.setUrlConsumerFactory(new PassiveUrlConsumerFactory());
    // add the 'cached' version
    CIProperties cachedProps = new CIProperties();
    cachedProps.setProperty(CachedUrl.PROPERTY_LAST_MODIFIED,
			    GMT_DATE_FORMATTER.format(new Date(12345)));
    // mcus.addUrl("test stream", TEST_URL, true, true, cachedProps);
    mau.addUrl(TEST_URL, true, true, cachedProps);

    TimeBase.setSimulated(10000);
    fetcher._input = new StringInputStream("test stream");
    fetcher._headers = cachedProps;
    // should still cache
    BitSet bs = new BitSet();
    bs.set(UrlCacher.REFETCH_FLAG);
    fetcher.setFetchFlags(bs);
    assertEquals(FetchResult.FETCHED, fetcher.fetch());
    
    TimeBase.setReal();
  }

  public void testNullExceptions() throws IOException {
    fetcher.setUrlConsumerFactory(new PassiveUrlConsumerFactory());
    fetcher._input = new StringInputStream("test stream");
    fetcher._headers = null;
    try {
      fetcher.fetchWithRetries(null);
      fail("Should have thrown NullPointerException.");
    } catch (NullPointerException npe) {
    }

    // no exceptions from null inputstream
    try {
      fetcher.reset();
      fetcher._input = null;
      fetcher._headers = new CIProperties();
      log.warning("NullPointerException expected");
      fetcher.fetch();
    } catch (Exception e) {
      fail("should not have thrown " + e);
    }
    // should simply skip
    try {
      fetcher.reset();
      fetcher._input = new StringInputStream("test stream");
      fetcher._headers = new CIProperties();
      fetcher.fetch();
    } catch (Exception e) {
      fail("should not have thrown " + e);
    }
  }

  public void testFileFetch() throws IOException {
    fetcher.setUrlConsumerFactory(new PassiveUrlConsumerFactory());
    fetcher._input = new StringInputStream("test content");
    CIProperties props = new CIProperties();
    props.setProperty("test1", "value1");
    fetcher._headers = props;
    fetcher.fetch();

    InputStream is = fetcher.getUncachedInputStream();
    assertReaderMatchesString("test content", new InputStreamReader(is));

    props = fetcher.getUncachedProperties();
    assertEquals("value1", props.getProperty("test1"));
  }

  public void testCheckConnection() throws IOException {
    MockLockssUrlConnection conn = new MockLockssUrlConnection();
    conn.setResponseCode(200);
    conn.setResponseMessage("OK");
    
    try {
      fetcher.checkConnectException(conn);
    } catch(Exception e) {
      fail("should not have thrown " + e);
    }
    
    conn.setResponseCode(401);
    conn.setResponseMessage("Unauthorized");
    try {
      fetcher.checkConnectException(conn);
      fail("Exception expected");
    } catch (IOException ex) {
      //expected exception
    }

  }

  // The following tests do not use TestableBaseUrlFetcher, so test more of
  // BaseUrlFetcher. Mostly they test its behavior wrt the connection.
  // MockConnectionBaseUrlFetcher is used to create a mock connection.

  MyMockLockssUrlConnection makeConn(int respCode, String respMessage,
                                     String redirectTo)
      throws IOException {
    return makeConn(respCode, respMessage, redirectTo, (String)null);
  }

  MyMockLockssUrlConnection makeConn(int respCode, String respMessage,
                                     String redirectTo, String input)
      throws IOException {
    return makeConn(respCode, respMessage, redirectTo,
		    input != null ? new StringInputStream(input) : null);
  }

  MyMockLockssUrlConnection makeConn(int respCode, String respMessage,
                                     String redirectTo,
				     InputStream inputStream)
      throws IOException {
    MyMockLockssUrlConnection mconn = new MyMockLockssUrlConnection();
    mconn.setResponseCode(respCode);
    mconn.setResponseMessage(respMessage);
    if (redirectTo != null) {
      mconn.setResponseHeader("location", redirectTo);
    }
    mconn.setResponseContentType("");
    mconn.setResponseContentEncoding("");
    mconn.setResponseDate(0);
    if (inputStream != null) {
      mconn.setResponseInputStream(inputStream);
    }
    return mconn;
  }

  public void testCookiePolicy() throws IOException {
    TimeBase.setSimulated(555666);
    mau.setCookiePolicy("oatmeal-raisin");

    MockConnectionBaseUrlFetcher muf =
      new MockConnectionBaseUrlFetcher(mcf, TEST_URL);
    MyMockLockssUrlConnection mconn = makeConn(200, "", null, "foo");
    muf.addConnection(mconn);
    muf.getUncachedInputStream();
    assertEquals("oatmeal-raisin", mconn.getCookiePolicy());
    Properties props = muf.getUncachedProperties();
    assertEquals("", props.get(CachedUrl.PROPERTY_CONTENT_TYPE));
    assertEquals("555666", props.get(CachedUrl.PROPERTY_FETCH_TIME));
  }

  public void testPluginCookies() throws IOException {
    TimeBase.setSimulated(555666);
    mau.setHttpCookies(ListUtil.list("foo=bar"));

    MockConnectionBaseUrlFetcher muf =
      new MockConnectionBaseUrlFetcher(mcf, TEST_URL);
    MyMockLockssUrlConnection mconn = makeConn(200, "", null, "foo");
    muf.addConnection(mconn);
    muf.getUncachedInputStream();
    assertEquals(ListUtil.list(ListUtil.list("www.example.com", "/",
					     "foo", "bar")),
		 mconn.getCookies());
  }

  public void testGlobalRequestHeaders() throws IOException {
    ConfigurationUtil.addFromArgs(CrawlManagerImpl.PARAM_REQUEST_HEADERS,
				  "foo:baz;a:c;ill");
    MockConnectionBaseUrlFetcher muf =
      new MockConnectionBaseUrlFetcher(mcf, TEST_URL);
    MyMockLockssUrlConnection mconn = makeConn(200, "", null, "foo");
    muf.addConnection(mconn);
    muf.getUncachedInputStream();
    assertEquals(PropUtil.fromArgs("user-agent", "LOCKSS cache",
				   "foo", "baz",
				   "a", "c"),
		 mconn.getRequestProperties());
  }

  public void testPluginRequestHeaders() throws IOException {
    mau.setHttpRequestHeaders(ListUtil.list("foo:bar",
					    "Accept-Languege:da, en-gb;q=0.8, en;q=0.7",
					    "a:b",
					    "illegal"));
    MockConnectionBaseUrlFetcher muf =
      new MockConnectionBaseUrlFetcher(mcf, TEST_URL);
    MyMockLockssUrlConnection mconn = makeConn(200, "", null, "foo");
    muf.addConnection(mconn);
    muf.getUncachedInputStream();
    assertEquals(PropUtil.fromArgs("user-agent", "LOCKSS cache",
				   "accept-languege", "da, en-gb;q=0.8, en;q=0.7",
				   "a", "b",
				   "foo", "bar"),
		 mconn.getRequestProperties());
  }

  // Ensure plugin request headers override global
  public void testBothRequestHeaders() throws IOException {
    ConfigurationUtil.addFromArgs(CrawlManagerImpl.PARAM_REQUEST_HEADERS,
				  "foo:baz;a:c;ill");
    mau.setHttpRequestHeaders(ListUtil.list("foo:bar",
					    "Accept-Languege:da, en-gb;q=0.8, en;q=0.7",
					    "a:",
					    "illegal"));
    MockConnectionBaseUrlFetcher muf =
      new MockConnectionBaseUrlFetcher(mcf, TEST_URL);
    MyMockLockssUrlConnection mconn = makeConn(200, "", null, "foo");
    muf.addConnection(mconn);
    muf.getUncachedInputStream();
    assertEquals(PropUtil.fromArgs("user-agent", "LOCKSS cache",
				   "accept-languege", "da, en-gb;q=0.8, en;q=0.7",
				   "foo", "bar"),
		 mconn.getRequestProperties());
  }

  public void testCustomizeConnection() throws Exception {
    MockConnectionBaseUrlFetcher muf =
      new MockConnectionBaseUrlFetcher(mcf, TEST_URL);
    muf.setHeaderCharset("FOO_CHARSET");
    MyMockLockssUrlConnection mconn = makeConn(200, "", null, "foo");
    muf.addConnection(mconn);
    muf.getUncachedInputStream();
    assertEquals("FOO_CHARSET", mconn.headerCharset);
    assertTrue(muf.executeCalled);
  }

  public void testGetUncachedProperties() throws IOException {
    TimeBase.setSimulated(555666);
    MockConnectionBaseUrlFetcher muf =
      new MockConnectionBaseUrlFetcher(mcf, TEST_URL);
    MyMockLockssUrlConnection mconn = makeConn(200, "", null, "foo");
    muf.addConnection(mconn);
    muf.getUncachedInputStream();
    Properties props = muf.getUncachedProperties();
    assertEquals("", props.get(CachedUrl.PROPERTY_CONTENT_TYPE));
    assertEquals("555666", props.get(CachedUrl.PROPERTY_FETCH_TIME));
  }

  public void testGetUncachedPropertiesNull() throws IOException {
    TimeBase.setSimulated(555666);
    MockConnectionBaseUrlFetcher muf =
      new MockConnectionBaseUrlFetcher(mcf, TEST_URL);
    MyMockLockssUrlConnection mconn = makeConn(200, "", null, "foo");
    mconn.setResponseContentType(null);
    muf.addConnection(mconn);
    muf.getUncachedInputStream();
    Properties props = muf.getUncachedProperties();
    assertEquals(null, props.get(CachedUrl.PROPERTY_CONTENT_TYPE));
    assertEquals("555666", props.get(CachedUrl.PROPERTY_FETCH_TIME));
  }

  public void testReferrer() throws IOException {
    String refrr = "http://example.com/referer1";
    TimeBase.setSimulated(555666);
    MockConnectionBaseUrlFetcher muf =
      new MockConnectionBaseUrlFetcher(mcf, TEST_URL);
    MyMockLockssUrlConnection mconn = makeConn(200, "", null, "foo");
    mconn.setResponseContentType(null);
    muf.setRequestProperty(Constants.HTTP_REFERER, refrr);
    muf.addConnection(mconn);
    muf.getUncachedInputStream();
    Properties props = muf.getUncachedProperties();
    // ensure Referer was set in HTTP request
    assertEquals(refrr, mconn.getRequestProperty(Constants.HTTP_REFERER));
    // and in stored properties
    assertEquals(refrr, props.getProperty(CachedUrl.PROPERTY_REQ_REFERRER));
  }

  public void testReferrerNoRecord() throws IOException {
    ConfigurationUtil.addFromArgs(BaseUrlFetcher.PARAM_RECORD_REFERRER,
				  "false");
    String refrr = "http://example.com/referer1";
    TimeBase.setSimulated(555666);
    MockConnectionBaseUrlFetcher muf =
      new MockConnectionBaseUrlFetcher(mcf, TEST_URL);
    MyMockLockssUrlConnection mconn = makeConn(200, "", null, "foo");
    mconn.setResponseContentType(null);
    muf.setRequestProperty(Constants.HTTP_REFERER, refrr);
    muf.addConnection(mconn);
    muf.getUncachedInputStream();
    Properties props = muf.getUncachedProperties();
    // ensure Referer was set in HTTP request
    assertEquals(refrr, mconn.getRequestProperty(Constants.HTTP_REFERER));
    // and in stored properties
    assertNull(refrr, props.getProperty(CachedUrl.PROPERTY_REQ_REFERRER));
  }

  // Ensure no compression performed on receipt
  public void testGzippedFetch() throws IOException {
    String testString = "test gzipped content";
    MockConnectionBaseUrlFetcher muf =
      new MockConnectionBaseUrlFetcher(mcf, TEST_URL);
    MyMockLockssUrlConnection mconn =
      makeConn(200, "", null, new GZIPpedInputStream(testString));
    mconn.setResponseContentEncoding("gzip");
    muf.addConnection(mconn);
    assertSameBytes(new GZIPpedInputStream(testString),
		    muf.getUncachedInputStream());
  }

  public void testMalformedUrlException() throws IOException {
    MockConnectionBaseUrlFetcher muf =
      new MockConnectionBaseUrlFetcher(mcf, TEST_URL);
    ThrowingMockLockssUrlConnection mconn =
      new ThrowingMockLockssUrlConnection(new MalformedURLException());
    muf.addConnection(mconn);
    try {
      muf.getUncachedInputStream();
      fail("Should have thrown");
    } catch (CacheException.MalformedURLException ex) {
    }
  }

  public void testConnTimeout() throws IOException {
    MockConnectionBaseUrlFetcher muf =
      new MockConnectionBaseUrlFetcher(mcf, TEST_URL);
    ThrowingMockLockssUrlConnection mconn =
      new ThrowingMockLockssUrlConnection(new java.net.ConnectException());
    muf.addConnection(mconn);
    try {
      muf.getUncachedInputStream();
      fail("Should have thrown");
    } catch (CacheException.RetryableNetworkException ex) {
    }
  }

  public void testSocketTimeout() throws IOException {
    MockConnectionBaseUrlFetcher muf =
      new MockConnectionBaseUrlFetcher(mcf, TEST_URL);
    ThrowingMockLockssUrlConnection mconn =
      new ThrowingMockLockssUrlConnection(new java.net.SocketException());
    muf.addConnection(mconn);
    try {
      muf.getUncachedInputStream();
      fail("Should have thrown");
    } catch (CacheException.RetryableNetworkException ex) {
    }
  }

  public void testUnknownHostTimeout() throws IOException {
    MockConnectionBaseUrlFetcher muf =
      new MockConnectionBaseUrlFetcher(mcf, TEST_URL);
    ThrowingMockLockssUrlConnection mconn =
      new ThrowingMockLockssUrlConnection(new java.net.UnknownHostException());
    muf.addConnection(mconn);
    try {
      muf.getUncachedInputStream();
      fail("Should have thrown");
    } catch (CacheException.RetryableNetworkException ex) {
    }
  }

  public void testKeepAlive(boolean exp) throws IOException {
    MockConnectionBaseUrlFetcher muf =
      new MockConnectionBaseUrlFetcher(mcf, TEST_URL);
    MockLockssUrlConnection mconn = makeConn(200, "", null, "foo");
    muf.addConnection(mconn);
    muf.getUncachedInputStream();
    assertEquals(exp, mconn.getKeepAlive());
  }


  public void testKeepAliveDefault() throws IOException {
    MockConnectionBaseUrlFetcher muf =
      new MockConnectionBaseUrlFetcher(mcf, TEST_URL);
    testKeepAlive(false);
  }

  public void testKeepAliveTrue() throws IOException {
    ConfigurationUtil.addFromArgs(BaseUrlFetcher.PARAM_SO_KEEPALIVE, "true");
    testKeepAlive(true);
  }

  public void testKeepAliveFalse() throws IOException {
    ConfigurationUtil.addFromArgs(BaseUrlFetcher.PARAM_SO_KEEPALIVE, "false");
    testKeepAlive(false);
  }

  public void testNoProxy() throws Exception {
    MockConnectionBaseUrlFetcher muf =
      new MockConnectionBaseUrlFetcher(mcf, TEST_URL);
    muf.setUrlConsumerFactory(new PassiveUrlConsumerFactory());
    MyMockLockssUrlConnection mconn = makeConn(200, "", null, "foo");
    muf.addConnection(mconn);
    muf.fetch();
    assertEquals(TEST_URL, mconn.getURL());
    assertNull(mconn.proxyHost);
  }

  public void testProxy() throws Exception {
    MockConnectionBaseUrlFetcher muf =
      new MockConnectionBaseUrlFetcher(mcf, TEST_URL);
    muf.setUrlConsumerFactory(new PassiveUrlConsumerFactory());
    MyMockLockssUrlConnection mconn = makeConn(200, "", null, "foo");
    muf.addConnection(mconn);
    muf.setProxy("phost", 126);
    muf.fetch();
    assertEquals(TEST_URL, mconn.getURL());
    assertEquals("phost", mconn.proxyHost);
    assertEquals(126, mconn.proxyPort);
    assertNull(mconn.getRequestProperty(Constants.X_LOCKSS_AUID));
  }

  public void testProxyNyAuid() throws Exception {
    ConfigurationUtil.addFromArgs(BaseUrlFetcher.PARAM_PROXY_BY_AUID, "true");
    MockConnectionBaseUrlFetcher muf =
      new MockConnectionBaseUrlFetcher(mcf, TEST_URL);
    muf.setUrlConsumerFactory(new PassiveUrlConsumerFactory());
    MyMockLockssUrlConnection mconn = makeConn(200, "", null, "foo");
    muf.addConnection(mconn);
    muf.setProxy("phost", 126);
    muf.fetch();
    assertEquals(TEST_URL, mconn.getURL());
    assertEquals("phost", mconn.proxyHost);
    assertEquals(126, mconn.proxyPort);
    assertEquals(mau.getAuId(),
		 mconn.getRequestProperty(Constants.X_LOCKSS_AUID));
  }

  public void testLocalAddr() throws Exception {
    IPAddr addr = IPAddr.getByName("127.7.42.33");
    MockConnectionBaseUrlFetcher muf =
      new MockConnectionBaseUrlFetcher(mcf, TEST_URL);
    muf.setUrlConsumerFactory(new PassiveUrlConsumerFactory());
    MyMockLockssUrlConnection mconn = makeConn(200, "", null, "foo");
    muf.addConnection(mconn);
    muf.setLocalAddress(addr);
    muf.fetch();
    assertEquals(TEST_URL, mconn.getURL());
    assertEquals(addr, mconn.localAddr);
  }

  public void testNoLocalAddr() throws Exception {
    MockConnectionBaseUrlFetcher muf =
      new MockConnectionBaseUrlFetcher(mcf, TEST_URL);
    muf.setUrlConsumerFactory(new PassiveUrlConsumerFactory());
    MyMockLockssUrlConnection mconn = makeConn(200, "", null, "foo");
    muf.addConnection(mconn);
    muf.fetch();
    assertEquals(TEST_URL, mconn.getURL());
    assertEquals(null, mconn.localAddr);
  }

  public void testCredentials() throws Exception {
    MockConnectionBaseUrlFetcher muf =
      new MockConnectionBaseUrlFetcher(mcf, TEST_URL);
    muf.setUrlConsumerFactory(new PassiveUrlConsumerFactory());
    Configuration auConfig = mau.getConfiguration();
    auConfig.put(ConfigParamDescr.USER_CREDENTIALS.getKey(), "uuu:ppp");
    MyMockLockssUrlConnection mconn = makeConn(200, "", null, "foo");
    muf.addConnection(mconn);
    muf.fetch();
    assertEquals("uuu", mconn.username);
    assertEquals("ppp", mconn.password);
  }

  public void testCredentialsIndirect() throws Exception {
    ConfigurationUtil.setFromArgs("org.lockss.cred.wayback.foopln",
				  "foo_user:foopln_pass");
    MockConnectionBaseUrlFetcher muf =
      new MockConnectionBaseUrlFetcher(mcf, TEST_URL);
    muf.setUrlConsumerFactory(new PassiveUrlConsumerFactory());
    Configuration auConfig = mau.getConfiguration();
    auConfig.put(ConfigParamDescr.USER_CREDENTIALS.getKey(),
		 "@org.lockss.cred.wayback.foopln");
    MyMockLockssUrlConnection mconn = makeConn(200, "", null, "foo");
    muf.addConnection(mconn);
    muf.fetch();
    assertEquals("foo_user", mconn.username);
    assertEquals("foopln_pass", mconn.password);
  }

  public void testSetReqProp() throws Exception {
    MockConnectionBaseUrlFetcher muf =
      new MockConnectionBaseUrlFetcher(mcf, TEST_URL);
    muf.setUrlConsumerFactory(new PassiveUrlConsumerFactory());
    MyMockLockssUrlConnection mconn = makeConn(200, "", null, "foo");
    muf.addConnection(mconn);
    muf.setRequestProperty("foo-bar", "47");
    muf.fetch();
    assertEquals(TEST_URL, mconn.getURL());
    assertEquals("47", mconn.getRequestProperty("foo-bar"));
  }

  // Shouldn't generate if-modified-since header because no existing content
  public void testIfModifiedConnectionNoContent() throws Exception {
    MockConnectionBaseUrlFetcher muf =
      new MockConnectionBaseUrlFetcher(mcf, TEST_URL);
    muf.setUrlConsumerFactory(new PassiveUrlConsumerFactory());
    MockLockssUrlConnection mconn = makeConn(200, "", null, "foo");
    muf.addConnection(mconn);
    BitSet fetchFlags = new BitSet();
    fetchFlags.clear(UrlCacher.REFETCH_FLAG);
    muf.setFetchFlags(fetchFlags);
    muf.fetch();
    assertEquals(TEST_URL, mconn.getURL());
    assertNull(mconn.getRequestProperty("if-modified-since"));
  }

  MockConnectionBaseUrlFetcher makeMucWithContent() {
    MockConnectionBaseUrlFetcher muf =
      new MockConnectionBaseUrlFetcher(mcf, TEST_URL);
    CIProperties cuprops = new CIProperties();

    cuprops.setProperty(CachedUrl.PROPERTY_LAST_MODIFIED,
			GMT_DATE_FORMATTER.format(new Date(12345000)));

    mau.addUrl(TEST_URL, true, true, cuprops);
    return muf;
  }

  // Should generate if-modified-since header
  public void testIfModifiedConnection() throws Exception {
    MockConnectionBaseUrlFetcher muf = makeMucWithContent();
    MockLockssUrlConnection mconn = makeConn(200, "", null, "foo");
    muf.addConnection(mconn);
    BitSet fetchFlags = new BitSet();
    fetchFlags.clear(UrlCacher.REFETCH_FLAG);
    muf.setFetchFlags(fetchFlags);
    muf.fetch();
    assertEquals(TEST_URL, mconn.getURL());
    assertEquals("Thu, 01 Jan 1970 03:25:45 GMT", mconn
        .getRequestProperty("if-modified-since"));
  }

  // Shouldn't generate if-modified-since header because REFETCH_FLAG set
  public void testForcedConnection() throws Exception {
    MockConnectionBaseUrlFetcher muf = makeMucWithContent();
    MockLockssUrlConnection mconn = makeConn(200, "", null, "foo");
    muf.addConnection(mconn);
    BitSet bs = new BitSet();
    bs.set(UrlCacher.REFETCH_FLAG);
    muf.setFetchFlags(bs);
    muf.fetch();
    assertEquals(TEST_URL, mconn.getURL());
    assertNull(mconn.getRequestProperty("if-modified-since"));
    // check the CU contents and properties
    assertFetchContents(muf, "foo");
    assertCuProperty(muf, null, CachedUrl.PROPERTY_REDIRECTED_TO);
    assertCuProperty(muf, null, CachedUrl.PROPERTY_CONTENT_URL);
  }

  // Should throw exception derived from response code
  public void testResponseError() throws Exception {
    MockConnectionBaseUrlFetcher muf =
      new MockConnectionBaseUrlFetcher(mcf, TEST_URL);
    MockLockssUrlConnection mconn = makeConn(404, "Not fond", null);
    muf.addConnection(mconn);
    try {
      InputStream is = muf.getUncachedInputStream();
      fail("Should have thrown ExpectedNoRetryException");
    } catch (CacheException.ExpectedNoRetryException e) {
      assertEquals("404 Not fond", e.getMessage());
    }
  }

  // Shouldn't follow redirect because told not to.
  public void testNoRedirect() throws Exception {
    String redTo = "http://somewhere.else/foo";
    MockConnectionBaseUrlFetcher muf =
      new MockConnectionBaseUrlFetcher(mcf, TEST_URL);
    muf.addConnection(makeConn(301, "Moved to Spain", redTo));
    muf.addConnection(makeConn(301, "Moved to tears",
                               "http://elsewhere.org/foo"));
    muf.setRedirectScheme(UrlFetcher.REDIRECT_SCHEME_DONT_FOLLOW);
    try {
      InputStream is = muf.getUncachedInputStream();
      fail("Should have thrown RetryNewUrlException");
    } catch (CacheException.NoRetryNewUrlException e) {
      assertEquals("301 Moved to Spain", e.getMessage());
      CIProperties p = muf.getUncachedProperties();
      // In this case the new location should be in the UrlFetcher's
      // properties, even though no CachedUrl was written
      assertEquals(redTo, p.getProperty("location"));
    }
  }

  // Can't test REDIRECT_SCHEME_FOLLOW because MockLockssUrlConnection
  // doesn't do redirection.

  // Should follow redirection to URL in crawl spec
  public void testRedirectInSpec() throws Exception {
    String redTo = "http://somewhere.else/foo";
    MockConnectionBaseUrlFetcher muf =
      new MockConnectionBaseUrlFetcher(mcf, TEST_URL);
    MockPermissionMap map = new MockPermissionMap();
    map.putStatus(TEST_URL, PermissionStatus.PERMISSION_OK);
    map.putStatus(redTo, PermissionStatus.PERMISSION_OK);
    mcf.setPermissionMap(map);
    muf.addConnection(makeConn(301, "Moved to Spain", redTo));
    muf.addConnection(makeConn(200, "Ok", null, "bar"));
    muf.setRedirectScheme(UrlFetcher.REDIRECT_SCHEME_STORE_ALL_IN_SPEC);
    mau.addUrlToBeCached(redTo);
    InputStream is = muf.getUncachedInputStream();
    CIProperties p = muf.getUncachedProperties();
    assertNull(p.getProperty("location"));
    assertEquals(redTo, p.getProperty(CachedUrl.PROPERTY_REDIRECTED_TO));
    assertEquals(redTo, p.getProperty(CachedUrl.PROPERTY_CONTENT_URL));
    assertReaderMatchesString("bar", new InputStreamReader(is));
    // Make sure the UrlFetcher still has the original URL
    assertEquals(TEST_URL, muf.getUrl());
  }

  // Should not follow redirection to URL not in crawl spec
  public void testRedirectNotInSpec() throws Exception {
    String redTo = "http://somewhere.else/foo";
    MockConnectionBaseUrlFetcher muf =
      new MockConnectionBaseUrlFetcher(mcf, TEST_URL);
    muf.addConnection(makeConn(301, "Moved to Fresno", redTo));
    muf.addConnection(makeConn(200, "Ok", null, "bar"));
    muf.setRedirectScheme(UrlFetcher.REDIRECT_SCHEME_STORE_ALL_IN_SPEC);
    try {
      InputStream is = muf.getUncachedInputStream();
      fail("Should have thrown RedirectOutsideCrawlSpecException");
    } catch (CacheException.RedirectOutsideCrawlSpecException e) {
      assertEquals("Redirected to excluded URL: " + redTo, e.getMessage());
      CIProperties p = muf.getUncachedProperties();
      assertEquals(redTo, p.getProperty("location"));
    }
  }

  public void testRedirectNormalize() throws Exception {
    String redToUnNorm = "http://Somewhere.ELSE/foo#removeme";
    String redTo = "http://somewhere.else/foo";
    MockConnectionBaseUrlFetcher muf =
      new MockConnectionBaseUrlFetcher(mcf, TEST_URL);
    MockPermissionMap map = new MockPermissionMap();
    map.putStatus(TEST_URL, PermissionStatus.PERMISSION_OK);
    map.putStatus(redTo, PermissionStatus.PERMISSION_OK);
    mcf.setPermissionMap(map);
    muf.addConnection(makeConn(301, "Moved to Spain", redToUnNorm));
    muf.addConnection(makeConn(200, "Ok", null, "bar"));
    muf.setRedirectScheme(UrlFetcher.REDIRECT_SCHEME_STORE_ALL_IN_SPEC);
    mau.addUrlToBeCached(redTo);
    InputStream is = muf.getUncachedInputStream();
    CIProperties p = muf.getUncachedProperties();
    assertNull(p.getProperty("location"));
    assertEquals(redTo, p.getProperty(CachedUrl.PROPERTY_REDIRECTED_TO));
    assertEquals(redTo, p.getProperty(CachedUrl.PROPERTY_CONTENT_URL));
    assertReaderMatchesString("bar", new InputStreamReader(is));
    // Make sure the UrlFetcher still has the original URL
    assertEquals(TEST_URL, muf.getUrl());
  }

  public void testRedirectDontNormalize() throws Exception {
    ConfigurationUtil.setFromArgs(BaseUrlFetcher.PARAM_NORMALIZE_REDIRECT_URL,
				  "false");
    String redToUnNorm = "http://Somewhere.ELSE/foo#removeme";
    String redTo = "http://somewhere.else/foo";
    MockConnectionBaseUrlFetcher muf =
      new MockConnectionBaseUrlFetcher(mcf, TEST_URL);
    MockPermissionMap map = new MockPermissionMap();
    map.putStatus(TEST_URL, PermissionStatus.PERMISSION_OK);
    map.putStatus(redTo, PermissionStatus.PERMISSION_OK);
    mcf.setPermissionMap(map);
    muf.addConnection(makeConn(301, "Moved to Spain", redToUnNorm));
    muf.addConnection(makeConn(200, "Ok", null, "bar"));
    muf.setRedirectScheme(UrlFetcher.REDIRECT_SCHEME_STORE_ALL_IN_SPEC);
    mau.addUrlToBeCached(redTo);
    try {
      InputStream is = muf.getUncachedInputStream();
      fail("Should have thrown RedirectOutsideCrawlSpecException");
    } catch (CacheException.RedirectOutsideCrawlSpecException e) {
      assertEquals("Redirected to excluded URL: " + redToUnNorm,
		   e.getMessage());
      CIProperties p = muf.getUncachedProperties();
      assertEquals(redToUnNorm, p.getProperty("location"));
    }
  }

  // Should follow redirection to URL in crawl spec
  public void testRedirectChain() throws Exception {
    String redTo1 = "http://2.2/a";
    String redTo2 = "http://2.2/b";
    String redTo = "http://somewhere.else/foo";
    MockConnectionBaseUrlFetcher muf =
      new MockConnectionBaseUrlFetcher(mcf, TEST_URL);
    MockPermissionMap map = new MockPermissionMap();
    map.putStatus(TEST_URL, PermissionStatus.PERMISSION_OK);
    map.putStatus(redTo1, PermissionStatus.PERMISSION_OK);
    map.putStatus(redTo, PermissionStatus.PERMISSION_OK);
    mcf.setPermissionMap(map);
    muf.addConnection(makeConn(301, "Moved to Spain", redTo1));
    muf.addConnection(makeConn(301, "Moved to Spain", redTo2));
    muf.addConnection(makeConn(301, "Moved to Spain", redTo));
    muf.addConnection(makeConn(200, "Ok", null, "bar"));
    muf.setRedirectScheme(UrlFetcher.REDIRECT_SCHEME_STORE_ALL_IN_SPEC);
    mau.addUrlToBeCached(redTo1);
    mau.addUrlToBeCached(redTo2);
    mau.addUrlToBeCached(redTo);
    InputStream is = muf.getUncachedInputStream();
    CIProperties p = muf.getUncachedProperties();
    assertNull(p.getProperty("location"));
    assertEquals(redTo1, p.getProperty(CachedUrl.PROPERTY_REDIRECTED_TO));
    assertEquals(redTo, p.getProperty(CachedUrl.PROPERTY_CONTENT_URL));
    assertReaderMatchesString("bar", new InputStreamReader(is));
  }

  // Should throw because of max redirections
  public void testRedirectChainMax() throws Exception {
    String redTo = "http://foo.bar/foo";
    MockConnectionBaseUrlFetcher muf =
      new MockConnectionBaseUrlFetcher(mcf, TEST_URL);
    for (int ix = 0; ix < (1 + BaseUrlFetcher.MAX_REDIRECTS); ix++) {
      muf.addConnection(makeConn(301, "Moved to Spain", redTo));
    }
    muf.addConnection(makeConn(200, "Ok", null, "bar"));
    muf.setRedirectScheme(UrlFetcher.REDIRECT_SCHEME_STORE_ALL_IN_SPEC);
    mau.addUrlToBeCached(redTo);
    try {
      InputStream is = muf.getUncachedInputStream();
      fail("Should have thrown NoRetryNewUrlException");
    } catch (CacheException.NoRetryNewUrlException e) {
      assertEquals("301 Moved to Spain", e.getMessage());
      CIProperties p = muf.getUncachedProperties();
      assertEquals(redTo, p.getProperty("location"));
    }
  }

  // Should follow redirection to URL on same host
  public void testRedirectInSpecOnHost() throws Exception {
    String redTo = "http://www.example.com/foo";
    MockConnectionBaseUrlFetcher muf =
      new MockConnectionBaseUrlFetcher(mcf, TEST_URL);
    muf.addConnection(makeConn(301, "Moved to Spain", redTo));
    muf.addConnection(makeConn(200, "Ok", null, "bar"));
    muf.setRedirectScheme(UrlFetcher.REDIRECT_SCHEME_FOLLOW_IN_SPEC_ON_HOST);
    mau.addUrlToBeCached(redTo);
    InputStream is = muf.getUncachedInputStream();
    CIProperties p = muf.getUncachedProperties();
    assertNull(p.getProperty("location"));
    assertEquals(redTo, p.getProperty(CachedUrl.PROPERTY_REDIRECTED_TO));
    assertEquals(redTo, p.getProperty(CachedUrl.PROPERTY_CONTENT_URL));
    assertReaderMatchesString("bar", new InputStreamReader(is));
    // Make sure the UrlFetcher still has the original URL
    assertEquals(TEST_URL, muf.getUrl());
  }

  // Should not follow redirection to URL on different host
  public void testRedirectInSpecNotOnHost() throws Exception {
    String redTo = "http://somewhere.else/foo";
    MockConnectionBaseUrlFetcher muf =
      new MockConnectionBaseUrlFetcher(mcf, TEST_URL);
    muf.addConnection(makeConn(301, "Moved to Spain", redTo));
    muf.addConnection(makeConn(200, "Ok", null, "bar"));
    muf.setRedirectScheme(UrlFetcher.REDIRECT_SCHEME_FOLLOW_IN_SPEC_ON_HOST);
    mau.addUrlToBeCached(redTo);
    try {
      InputStream is = muf.getUncachedInputStream();
      fail("Should have thrown NoRetryNewUrlException");
    } catch (CacheException.NoRetryNewUrlException e) {
      assertEquals("301 Moved to Spain", e.getMessage());
      CIProperties p = muf.getUncachedProperties();
      assertEquals(redTo, p.getProperty("location"));
    }
  }

  // Should follow redirection to URL on same host
  public void testRedirectOnHost() throws Exception {
    String redTo = "http://www.example.com/foo";
    MockConnectionBaseUrlFetcher muf =
      new MockConnectionBaseUrlFetcher(mcf, TEST_URL);
    muf.addConnection(makeConn(301, "Moved to Spain", redTo));
    muf.addConnection(makeConn(200, "Ok", null, "bar"));
    muf.setRedirectScheme(UrlFetcher.REDIRECT_SCHEME_FOLLOW_ON_HOST);
    InputStream is = muf.getUncachedInputStream();
    CIProperties p = muf.getUncachedProperties();
    assertNull(p.getProperty("location"));
    assertEquals(redTo, p.getProperty(CachedUrl.PROPERTY_REDIRECTED_TO));
    assertEquals(redTo, p.getProperty(CachedUrl.PROPERTY_CONTENT_URL));
    assertReaderMatchesString("bar", new InputStreamReader(is));
    // Make sure the UrlFetcher still has the original URL
    assertEquals(TEST_URL, muf.getUrl());
  }

  // Should not follow redirection to URL on different host
  public void testRedirectNotOnHost() throws Exception {
    String redTo = "http://somewhere.else/foo";
    MockConnectionBaseUrlFetcher muf =
      new MockConnectionBaseUrlFetcher(mcf, TEST_URL);
    muf.addConnection(makeConn(301, "Moved to Spain", redTo));
    muf.addConnection(makeConn(200, "Ok", null, "bar"));
    muf.setRedirectScheme(UrlFetcher.REDIRECT_SCHEME_FOLLOW_ON_HOST);
    try {
      InputStream is = muf.getUncachedInputStream();
      fail("Should have thrown NoRetryNewUrlException");
    } catch (CacheException.NoRetryNewUrlException e) {
      assertEquals("301 Moved to Spain", e.getMessage());
      CIProperties p = muf.getUncachedProperties();
      assertEquals(redTo, p.getProperty("location"));
    }
  }

  AuHttpResultMap redirToLoginAuResultMap(String loginUrl) {
    List<Pair<String,ResultAction>> actionPats =
      ListUtil.list(Pair.of(loginUrl,
                            ResultAction.exClass(CacheException.RedirectToLoginPageException.class)));
    return new AuHttpResultMap(new HttpResultMap(),
                               PatternMap.fromPairs(actionPats));
  }


  public void testRedirectToLoginURL() throws Exception {
    String redTo = "http://somewhere.else/foo";
    mcf.setAuCacheResultMap(redirToLoginAuResultMap(redTo));
    mau.setLoginPageUrls(ListUtil.list(redTo));
    MockConnectionBaseUrlFetcher muf =
      new MockConnectionBaseUrlFetcher(mcf, TEST_URL);
    MockPermissionMap map = new MockPermissionMap();
    map.putStatus(TEST_URL, PermissionStatus.PERMISSION_OK);
    map.putStatus(redTo, PermissionStatus.PERMISSION_OK);
    mcf.setPermissionMap(map);
    muf.addConnection(makeConn(301, "Moved to Spain", redTo));
    muf.setRedirectScheme(UrlFetcher.REDIRECT_SCHEME_STORE_ALL_IN_SPEC);
    // Do NOT add redTo to crawl spec (mau.addUrlToBeCached(redTo)), to
    // ensure that login URL pattern is checked before crawl spec
    try {
      InputStream is = muf.getUncachedInputStream();
      fail("Should have thrown PermissionException");
    } catch (CacheException.PermissionException e) {
      assertEquals(expRedirToLoginPageMsg(TEST_URL, redTo), e.getMessage());
    }
  }

  public void testRedirectToLoginUrlUnsupportedProtocol() throws Exception {
    String redTo = "ftp://somewhere.else/foo";
    mcf.setAuCacheResultMap(redirToLoginAuResultMap(redTo));
    mau.setLoginPageUrls(ListUtil.list(redTo));
    MockConnectionBaseUrlFetcher muf =
      new MockConnectionBaseUrlFetcher(mcf, TEST_URL);
    MockPermissionMap map = new MockPermissionMap();
    map.putStatus(TEST_URL, PermissionStatus.PERMISSION_OK);
    map.putStatus(redTo, PermissionStatus.PERMISSION_OK);
    mcf.setPermissionMap(map);
    muf.addConnection(makeConn(301, "Moved to Spain", redTo));
    muf.setRedirectScheme(UrlFetcher.REDIRECT_SCHEME_STORE_ALL_IN_SPEC);
    // Do NOT add redTo to crawl spec (mau.addUrlToBeCached(redTo)), to
    // ensure that login URL pattern is checked before crawl spec
    try {
      InputStream is = muf.getUncachedInputStream();
      fail("Should have thrown PermissionException");
    } catch (CacheException.PermissionException e) {
      assertEquals(expRedirToLoginPageMsg(TEST_URL, redTo), e.getMessage());
    }
  }

  String expRedirToLoginPageMsg(String fromUrl, String toUrl) {
    return "Redirect to login page: " + toUrl + " Redirect from " + fromUrl;
  }


  public void testRedirectPassesBoth() throws Exception {
    mau.returnRealCachedUrl = true;
    String redTo = "http://somewhere.else/foo";
    MockConnectionBaseUrlFetcher muf =
      new MockConnectionBaseUrlFetcher(mcf, TEST_URL);
    MockPermissionMap map = new MockPermissionMap();
    map.putStatus(TEST_URL, PermissionStatus.PERMISSION_OK);
    map.putStatus(redTo, PermissionStatus.PERMISSION_OK);
    mcf.setPermissionMap(map);
    muf.addConnection(makeConn(301, "Moved to Spain", redTo));
    muf.addConnection(makeConn(200, "Ok", null, "bar"));
    muf.setRedirectScheme(UrlFetcher.REDIRECT_SCHEME_STORE_ALL_IN_SPEC);
    mau.addUrlToBeCached(redTo);
    mau.addUrlToBeCached(TEST_URL);
    muf.fetch();
    CIProperties p = muf.getUncachedProperties();
    assertNull(p.getProperty("location"));
    assertEquals(redTo, p.getProperty(CachedUrl.PROPERTY_REDIRECTED_TO));
    assertEquals(redTo, p.getProperty(CachedUrl.PROPERTY_CONTENT_URL));

    assertFetchContents(muf, "bar");
    assertCuProperty(muf, redTo, CachedUrl.PROPERTY_REDIRECTED_TO);
    assertCuProperty(muf, redTo, CachedUrl.PROPERTY_CONTENT_URL);
    assertEquals(ListUtil.list(redTo), muf.redirectUrls);
  }

  public void testRedirectPassesAll() throws Exception {
    String content = "oft redirected content";
    mau.returnRealCachedUrl = true;
    String redTo1 = "http://somewhere.else/foo";
    String redTo2 = "http://somewhere.else/bar/x.html";
    String redTo3 = "http://somewhere.else/bar/y.html";
    MockConnectionBaseUrlFetcher muf =
      new MockConnectionBaseUrlFetcher(mcf, TEST_URL);
    MockPermissionMap map = new MockPermissionMap();
    map.putStatus(TEST_URL, PermissionStatus.PERMISSION_OK);
    map.putStatus(redTo1, PermissionStatus.PERMISSION_OK);
    mcf.setPermissionMap(map);
    muf.addConnection(makeConn(301, "Moved to Spain", redTo1));
    muf.addConnection(makeConn(301, "Moved to Spain", redTo2));
    muf.addConnection(makeConn(301, "Moved to Spain", redTo3));
    muf.addConnection(makeConn(200, "Ok", null, content));
    muf.setRedirectScheme(UrlFetcher.REDIRECT_SCHEME_STORE_ALL_IN_SPEC);
    mau.addUrlToBeCached(redTo1);
    mau.addUrlToBeCached(redTo2);
    mau.addUrlToBeCached(redTo3);
    mau.addUrlToBeCached(TEST_URL);
    muf.fetch();
    CIProperties p = muf.getUncachedProperties();
    assertNull(p.getProperty("location"));
    assertEquals(redTo1, p.getProperty(CachedUrl.PROPERTY_REDIRECTED_TO));

    // verify all have the correct contents, and all but the last have a
    // redirected-to header
    assertFetchContents(muf, content);
    assertEquals(ListUtil.list(redTo1, redTo2, redTo3), muf.redirectUrls);
    assertCuProperty(muf, redTo1, CachedUrl.PROPERTY_REDIRECTED_TO);
    assertCuProperty(muf, redTo3, CachedUrl.PROPERTY_CONTENT_URL);
 }

  public void testSimpleDirRedirect() throws Exception {
    String content = "oft redirected content";
    mau.returnRealCachedUrl = true;
    String url = "http://a.b/bar";
    String redTo = url + "/";
    MockConnectionBaseUrlFetcher muf =
      new MockConnectionBaseUrlFetcher(mcf, url);
    muf.addConnection(makeConn(301, "Moved to Spain", redTo));
    muf.addConnection(makeConn(200, "Ok", null, content));
    muf.setRedirectScheme(UrlFetcher.REDIRECT_SCHEME_STORE_ALL_IN_SPEC);
    mau.addUrlToBeCached(redTo);
    mau.addUrlToBeCached(url);
    muf.fetch();
    CIProperties p = muf.getUncachedProperties();
    assertNull(p.getProperty("location"));
    assertEquals(redTo, p.getProperty(CachedUrl.PROPERTY_REDIRECTED_TO));

    // verify the correct contents and redirected-to header
    // (these are the same node)
    assertFetchContents(muf, content);
    assertEquals(redTo, muf.fetchUrl);
    assertEquals(url, muf.origUrl);
    assertCuProperty(muf, redTo, CachedUrl.PROPERTY_REDIRECTED_TO);
    assertCuProperty(muf, redTo, CachedUrl.PROPERTY_CONTENT_URL);
  }

  public void testSimpleDirNoRedirect() throws Exception {
    String content = "oft redirected content";
    mau.returnRealCachedUrl = true;
    String url = "http://a.b/bar/";
    MockConnectionBaseUrlFetcher muf =
      new MockConnectionBaseUrlFetcher(mcf, url);
    muf.addConnection(makeConn(200, "Ok", null, content));
    muf.setRedirectScheme(UrlFetcher.REDIRECT_SCHEME_STORE_ALL_IN_SPEC);
    mau.addUrlToBeCached(url);
    muf.fetch();
    CIProperties p = muf.getUncachedProperties();
    assertNull(p.getProperty("location"));
    assertNull(p.getProperty(CachedUrl.PROPERTY_REDIRECTED_TO));

    // verify the correct contents and redirected-to header
    // (these are the same node)
    assertFetchContents(muf, content);
    assertCuProperty(muf, null, CachedUrl.PROPERTY_REDIRECTED_TO);
    assertCuProperty(muf, null, CachedUrl.PROPERTY_CONTENT_URL);
    assertNull(muf.redirectUrls);
  }

  public void testDirRedirect() throws Exception {
    MockPermissionMap map = new MockPermissionMap();
    String content = "oft redirected content";
    mau.returnRealCachedUrl = true;
    String url = "http://a.b/bar";
    String redTo1 = "http://somewhere.else/foo";
    String redTo2 = "http://somewhere.else/foo/";
    MockConnectionBaseUrlFetcher muf =
      new MockConnectionBaseUrlFetcher(mcf, url);
    map.putStatus(url, PermissionStatus.PERMISSION_OK);
    map.putStatus(redTo1, PermissionStatus.PERMISSION_OK);
    mcf.setPermissionMap(map);
    muf.addConnection(makeConn(301, "Moved to Spain", redTo1));
    muf.addConnection(makeConn(301, "Moved to Spain", redTo2));
    muf.addConnection(makeConn(200, "Ok", null, content));
    muf.setRedirectScheme(UrlFetcher.REDIRECT_SCHEME_STORE_ALL_IN_SPEC);
    mau.addUrlToBeCached(redTo1);
    mau.addUrlToBeCached(redTo2);
    mau.addUrlToBeCached(url);
    muf.fetch();
    CIProperties p = muf.getUncachedProperties();
    assertNull(p.getProperty("location"));
    assertEquals(redTo1, p.getProperty(CachedUrl.PROPERTY_REDIRECTED_TO));

    // verify all have the correct contents, and all but the last have
    // redirected-to and content-url headers
    assertFetchContents(muf, content);
    assertEquals(ListUtil.list(redTo1), muf.redirectUrls);
    assertEquals(redTo2, muf.fetchUrl);
    assertEquals(url, muf.origUrl);
    assertCuProperty(muf, redTo1, CachedUrl.PROPERTY_REDIRECTED_TO);
  }

  public void testCacheLPC() throws IOException {
    MyMockLoginPageChecker loginPageChecker = new MyMockLoginPageChecker(false);
    List<String> urls = ListUtil.list("http://example.com");
    mau.setStartUrls(urls);
    mau.setPermissionUrls(urls);
    mau.setLoginPageChecker(loginPageChecker);
    mau.setRefetchDepth(99);
    fetcher.setUrlConsumerFactory(new PassiveUrlConsumerFactory());
    fetcher._input = new StringInputStream("test stream");
    fetcher._headers = new CIProperties();
    // should cache
    assertEquals(UrlFetcher.FetchResult.FETCHED, fetcher.fetch());

    assertTrue(loginPageChecker.wasCalled());
  }

  public void testCacheLPCNotModified() throws IOException {
    MyMockLoginPageChecker loginPageChecker =
      new MyMockLoginPageChecker(false);
    List<String> urls = ListUtil.list("http://example.com");
    mau.setStartUrls(urls);
    mau.setPermissionUrls(urls);
    mau.setLoginPageChecker(loginPageChecker);
    mau.setRefetchDepth(99);

    // add the 'cached' version
    CIProperties cachedProps = new CIProperties();
    cachedProps.setProperty(CachedUrl.PROPERTY_LAST_MODIFIED,
			    GMT_DATE_FORMATTER.format(new Date(12345)));
    // mcus.addUrl("test stream", TEST_URL, true, true, cachedProps);
    mau.addUrl(TEST_URL, true, true, cachedProps);

    TimeBase.setSimulated(10000);
    fetcher.setUrlConsumerFactory(new PassiveUrlConsumerFactory());
    fetcher._input = new StringInputStream("test stream");
    fetcher._headers = new CIProperties();
    // shouldn't cache
    assertEquals(UrlFetcher.FetchResult.FETCHED_NOT_MODIFIED, fetcher.fetch());
    assertFalse(loginPageChecker.wasCalled());
  }

  public void testCacheLPCResetAndMarkCalled() throws IOException {
    MyMockLoginPageChecker loginPageChecker = new MyMockLoginPageChecker(false);
    List<String> urls = ListUtil.list("http://example.com");
    mau.setStartUrls(urls);
    mau.setPermissionUrls(urls);
    mau.setLoginPageChecker(loginPageChecker);
    mau.setRefetchDepth(99);

    MyStringInputStream inStrm = new MyStringInputStream("test stream");
    fetcher.setUrlConsumerFactory(new PassiveUrlConsumerFactory());
    fetcher._input = inStrm;
    fetcher._headers = new CIProperties();
    // should cache
    assertEquals(UrlFetcher.FetchResult.FETCHED, fetcher.fetch());

    assertTrue(loginPageChecker.wasCalled());
    assertTrue(inStrm.markWasCalled());
    assertEquals(BaseUrlFetcher.DEFAULT_LOGIN_CHECKER_MARK_LIMIT,
		 inStrm.getMarkBufferSize());
    assertTrue(inStrm.resetWasCalled());
    assertEquals(1, fetcher.getUncachedInputStreamCount);
  }

  public void testCacheLPCResetAndMarkCalled2() throws IOException {
    ConfigurationUtil.addFromArgs(BaseUrlFetcher.PARAM_LOGIN_CHECKER_MARK_LIMIT,
				  "12345");
    MyMockLoginPageChecker loginPageChecker = 
        new MyMockLoginPageChecker(false);
    List<String> urls = ListUtil.list("http://example.com");
    mau.setStartUrls(urls);
    mau.setPermissionUrls(urls);
    mau.setLoginPageChecker(loginPageChecker);
    mau.setRefetchDepth(99);


    MyStringInputStream inStrm = new MyStringInputStream("test stream");;
    fetcher.setUrlConsumerFactory(new PassiveUrlConsumerFactory());
    fetcher._input = inStrm;
    fetcher._headers = new CIProperties();
    // should cache
    assertEquals(UrlFetcher.FetchResult.FETCHED, fetcher.fetch());

    assertTrue(loginPageChecker.wasCalled());
    assertTrue(inStrm.markWasCalled());
    assertEquals(12345, inStrm.getMarkBufferSize());
    assertTrue(inStrm.resetWasCalled());
    assertEquals(1, fetcher.getUncachedInputStreamCount);
  }

  public void testCacheLPCResetFails() throws IOException {
    MyMockLoginPageChecker loginPageChecker = 
        new MyMockLoginPageChecker(false);
    List<String> urls = ListUtil.list("http://example.com");
    mau.setStartUrls(urls);
    mau.setPermissionUrls(urls);
    mau.setLoginPageChecker(loginPageChecker);
    mau.setRefetchDepth(99);

    MyStringInputStream strIs =
      new MyStringInputStream("test stream",
			      new IOException("Test exception"));
    MyStringInputStream strIs2 = new MyStringInputStream("test stream2");

    fetcher = new TestableBaseUrlFetcher(mcf, TEST_URL, ListUtil
        .list(strIs, strIs2));
    
    fetcher.setUrlConsumerFactory(new PassiveUrlConsumerFactory());
    fetcher._headers = new CIProperties();
    // should cache
    assertEquals(UrlFetcher.FetchResult.FETCHED, fetcher.fetch());

    assertTrue(loginPageChecker.wasCalled());
    assertTrue(strIs.resetWasCalled());
    assertTrue(strIs.closeWasCalled());
    assertFalse(strIs2.resetWasCalled());
    assertTrue(strIs2.closeWasCalled());
    assertEquals(2, fetcher.getUncachedInputStreamCount);
  }

  public void testCacheLPCLoginPage() throws IOException {
    MyMockLoginPageChecker loginPageChecker = new MyMockLoginPageChecker(true);
    List<String> urls = ListUtil.list("http://example.com");
    mau.setStartUrls(urls);
    mau.setPermissionUrls(urls);
    mau.setLoginPageChecker(loginPageChecker);
    mau.setRefetchDepth(99);
    fetcher.setUrlConsumerFactory(new PassiveUrlConsumerFactory());
    fetcher._input = new StringInputStream("test stream");
    fetcher._headers = new CIProperties();
    // should cache
    try {
      fetcher.fetch();
      fail("Should have thrown a CacheException.PermissionException");
    } catch (CacheException.PermissionException ex) {
    }
  }

  public void testCacheLPCCompressedLoginPage() throws IOException {
    StringLoginPageChecker loginPageChecker =
      new StringLoginPageChecker("login string");
    List<String> urls = ListUtil.list("http://example.com");
    mau.setStartUrls(urls);
    mau.setPermissionUrls(urls);
    mau.setLoginPageChecker(loginPageChecker);
    mau.setRefetchDepth(99);
    fetcher.setUrlConsumerFactory(new PassiveUrlConsumerFactory());
    fetcher._input = new GZIPpedInputStream("xxxlogin stringmxxx");
    fetcher._headers = fromArgs(CachedUrl.PROPERTY_CONTENT_ENCODING, "gzip"); 
    // should cache
    try {
      fetcher.fetch();
      fail("Should have thrown a CacheException.PermissionException");
    } catch (CacheException.PermissionException ex) {
    }
  }

  public void testCacheLPCCompressedNotLoginPage() throws IOException {
    StringLoginPageChecker loginPageChecker =
      new StringLoginPageChecker("login string");
    List<String> urls = ListUtil.list("http://example.com");
    mau.setStartUrls(urls);
    mau.setPermissionUrls(urls);
    mau.setLoginPageChecker(loginPageChecker);
    mau.setRefetchDepth(99);
    fetcher.setUrlConsumerFactory(new PassiveUrlConsumerFactory());
    fetcher._input = new GZIPpedInputStream("xxx content content content xxx");
    fetcher._headers = fromArgs(CachedUrl.PROPERTY_CONTENT_ENCODING, "gzip"); 
    // should cache
    assertEquals(UrlFetcher.FetchResult.FETCHED, fetcher.fetch());
    assertTrue(loginPageChecker.wasCalled());
  }

  public void testCacheLPCNotCompressedLoginPage() throws IOException {
    StringLoginPageChecker loginPageChecker =
      new StringLoginPageChecker("login string");
    List<String> urls = ListUtil.list("http://example.com");
    mau.setStartUrls(urls);
    mau.setPermissionUrls(urls);
    mau.setLoginPageChecker(loginPageChecker);
    mau.setRefetchDepth(99);
    fetcher.setUrlConsumerFactory(new PassiveUrlConsumerFactory());
    fetcher._input = new StringInputStream("xxxlogin stringmxxx");
    fetcher._headers = fromArgs(CachedUrl.PROPERTY_CONTENT_ENCODING, "gzip");
    // should cache
    try {
      fetcher.fetch();
      fail("Should have thrown a CacheException.PermissionException");
    } catch (CacheException.PermissionException ex) {
    }
  }

  CIProperties fromArgs(String prop, String val) {
    CIProperties props = new CIProperties();
    props.put(prop, val);
    return props;
  }


  /**
   * Tests that we wrap input streams that don't support marks in a
   * BufferedInputStream
   *
   * @throws IOException
   */
  public void testMarkNotSupported() throws IOException {
    MyMockLoginPageChecker loginPageChecker = 
        new MyMockLoginPageChecker(false);
    List<String> urls = ListUtil.list("http://example.com");
    mau.setStartUrls(urls);
    mau.setPermissionUrls(urls);
    mau.setLoginPageChecker(loginPageChecker);
    mau.setRefetchDepth(99);
    

    MyStringInputStream inStrm =
      new MyStringInputStreamMarkNotSupported("test stream");
    fetcher.setUrlConsumerFactory(new PassiveUrlConsumerFactory());
    fetcher._input = inStrm;
    fetcher._headers = new CIProperties();
    // should cache
    assertEquals(UrlFetcher.FetchResult.FETCHED, fetcher.fetch());

    assertTrue(loginPageChecker.wasCalled());
    assertFalse(inStrm.resetWasCalled());
    assertFalse(inStrm.markWasCalled());
    assertEquals(1, fetcher.getUncachedInputStreamCount);
  }

  List<String> loginPages = new ArrayList<String>();

  class MyMockLoginPageChecker implements LoginPageChecker {
    private boolean wasCalled = false;
    private boolean isLoginPage;

    MyMockLoginPageChecker(boolean isLoginPage) {
      this.isLoginPage = isLoginPage;
    }

    public boolean isLoginPage(Properties props, Reader reader)
	throws IOException {
      wasCalled = true;
      loginPages.add(StringUtil.fromReader(reader));
      return this.isLoginPage;
    }

    public boolean wasCalled() {
      return this.wasCalled;
    }
  }

  class StringLoginPageChecker implements LoginPageChecker {
    private boolean wasCalled = false;
    private String str;

    StringLoginPageChecker(String str) {
      this.str = str;
    }

    public boolean isLoginPage(Properties props, Reader reader)
	throws IOException {
      wasCalled = true;
      return StringUtil.containsString(reader, str);
    }

    public boolean wasCalled() {
      return this.wasCalled;
    }
  }

  void assertFetchContents(UrlFetcher fetcher, String contents) throws IOException {
    InputStream is = fetcher.getUncachedInputStream();
    assertReaderMatchesString(contents, new InputStreamReader(is));
  }

  /**
   * Assert that this url has no content
   */
  void assertCuNoContent(String url) throws IOException {
    CachedUrl cu = new BaseCachedUrl(mau, url);
    assertFalse(cu.hasContent());
  }

  void assertCuProperty(UrlFetcher fetcher, String expected, String key) {
    CIProperties props = fetcher.getUncachedProperties();
    assertEquals(expected, props.getProperty(key));
  }

  // Allows a list of preconfigured MockLockssUrlConnection instances to be
  // used for successive redirect fetches.
  private class MockConnectionBaseUrlFetcher extends BaseUrlFetcher {
    List connections = new ArrayList();
    String headerCharset;
    boolean executeCalled = false;

    public MockConnectionBaseUrlFetcher(Crawler.CrawlerFacade cf,
        String url) {
      super(cf, url);
    }

    void addConnection(MockLockssUrlConnection conn) {
      connections.add(conn);
    }

    @Override
    protected LockssUrlConnection makeConnection0(String url,
						  LockssUrlConnectionPool pool)
        throws IOException {
      if (connections != null && !connections.isEmpty()) {
        MockLockssUrlConnection mconn = (MockLockssUrlConnection) connections
            .remove(0);
        mconn.setURL(url);
        return mconn;
      } else {
        return new MockLockssUrlConnection();
      }
    }

    @Override
    protected void pauseBeforeFetch() {
      pauseBeforeFetchCounter++;
    }

    @Override
    protected void customizeConnection(LockssUrlConnection conn) {
      if (headerCharset != null) {
	conn.setHeaderCharset(headerCharset);
      }
    }

    @Override
    protected void executeConnection(LockssUrlConnection conn)
	throws IOException {
      executeCalled = true;
      super.executeConnection(conn);
    }

    private void setHeaderCharset(String charset) {
      headerCharset = charset;
    }

  }

  // Mock BaseUrlFetcher that fakes the connection
  private class TestableBaseUrlFetcher extends BaseUrlFetcher {
    InputStream _input = null;
    CIProperties _headers;
    int getUncachedPropertiesCount = 0;
    int getUncachedInputStreamCount = 0;
    BaseArchivalUnit.ParamHandlerMap pMap;

    List inputList;

    public TestableBaseUrlFetcher(Crawler.CrawlerFacade cf, String url) {
      super(cf, url);
    }

    public TestableBaseUrlFetcher(Crawler.CrawlerFacade cf, String url,
        List inputList) {
      super(cf, url);
      this.inputList = inputList;
    }

    @Override
    public InputStream getUncachedInputStreamOnly(String lastModified)
	throws IOException {
      // simple version which returns null if shouldn't fetch
      // if (lastCached < TimeBase.nowMs()) {
      getUncachedInputStreamCount++;
      return super.getUncachedInputStreamOnly(lastModified);
    }

    @Override
    protected LockssUrlConnection makeConnection0(String url,
						  LockssUrlConnectionPool pool)
	throws IOException {
      MockLockssUrlConnection conn;
      if (inputList == null) {
        logger.debug3("Using old method for getUncachedInputStream");
        // this is way too much smarts for mock code, but is left here for
        // legacy support. It should be cleaned up
	conn = new MockLockssUrlConnection() {
	    String ifMod;
	    public void setRequestProperty(String key, String value) {
	      super.setRequestProperty(key, value);
	      if ("If-Modified-Since".equals(key)) {
		ifMod = value;
	      }
	    }
	    public void execute() throws IOException {
	      super.execute();
	      long last = -1;
	      if (ifMod != null) {
		try {
		  last = GMT_DATE_PARSER.parse(ifMod).getTime();
		} catch (ParseException e) {
		  throw new IOException(e);
		}
	      }
	      if (last < TimeBase.nowMs()) {
	      } else {
		setResponseCode(304);
		setResponseInputStream(null);
	      }
	    }
	  };
	conn.setResponseInputStream(_input);
	conn.setResponseCode(200);
      } else {
	logger.debug3("Using new method for getUncachedInputStream");
	conn = new MockLockssUrlConnection();
	InputStream is = (InputStream) inputList.remove(0);
	conn.setResponseInputStream(is);
	conn.setResponseCode(200);
	logger.debug3("Returning " + is);
      }
      return conn;
    }

    @Override
    public CIProperties getUncachedProperties() {
      getUncachedPropertiesCount++;
      return _headers;
    }

    @Override
    protected void pauseBeforeFetch() {
      pauseBeforeFetchCounter++;
    }
  }

  private class MyMockArchivalUnit extends MockArchivalUnit {
    boolean returnRealCachedUrl = false;

    public CachedUrlSet makeCachedUrlSet(CachedUrlSetSpec cuss) {
      return new BaseCachedUrlSet(this, cuss);
    }
    
    public UrlCacher makeUrlCacher(UrlData ud) {
      return new MockUrlCacher(mau, ud);
    }

    public CachedUrl makeCachedUrl(String url) {
      if (returnRealCachedUrl) {
        return new BaseCachedUrl(this, url);
      } else {
        return super.makeCachedUrl(url);
      }
    }
  }

  private class MyMockLockssUrlConnection extends MockLockssUrlConnection {
    String proxyHost = null;
    int proxyPort = -1;
    IPAddr localAddr = null;
    String username;
    String password;
    String cpolicy;
    String headerCharset;
    List<List<String>> cookies = new ArrayList<List<String>>();

    public MyMockLockssUrlConnection() throws IOException {
      super();
    }

    public MyMockLockssUrlConnection(String url) throws IOException {
      super(url);
    }

    public void setProxy(String host, int port) {
      proxyHost = host;
      proxyPort = port;
    }

    public void setLocalAddress(IPAddr addr) {
      localAddr = addr;
    }

    public void setCredentials(String username, String password) {
      this.username = username;
      this.password = password;
    }

    public void setCookiePolicy(String policy) {
      cpolicy = policy;
    }

    String getCookiePolicy() {
      return cpolicy;
    }

    public void addCookie(String domain, String path,
			  String name, String value) {
      cookies.add(ListUtil.list(domain, path, name, value));
    }

    public List<List<String>> getCookies() {
      return cookies;
    }

    public void setHeaderCharset(String charset) {
      headerCharset = charset;
    }

  }

  private class ThrowingMockLockssUrlConnection extends MockLockssUrlConnection {
    IOException ex;

    public ThrowingMockLockssUrlConnection(IOException ex) throws IOException {
      super();
      this.ex = ex;
    }

    public void execute() throws IOException {
      throw ex;
    }
  }

  class MyStringInputStreamMarkNotSupported extends MyStringInputStream {
    public MyStringInputStreamMarkNotSupported(String str) {
      super(str);
    }

    public boolean markSupported() {
      return false;
    }
  }

  class MyStringInputStream extends StringInputStream {
    private boolean resetWasCalled = false;
    private boolean markWasCalled = false;
    private boolean closeWasCalled = false;
    private IOException resetEx;

    private int buffSize = -1;

    public MyStringInputStream(String str) {
      super(str);
    }

    /**
     * @param str String to read from
     * @param resetEx IOException to throw when reset is called
     *
     * Same as one arg constructor, but can provide an exception that is thrown
     * when reset is called
     */
    public MyStringInputStream(String str, IOException resetEx) {
      super(str);
      this.resetEx = resetEx;
    }

    public void reset() throws IOException {
      resetWasCalled = true;
      if (resetEx != null) {
        throw resetEx;
      }
      super.reset();
    }

    public boolean resetWasCalled() {
      return resetWasCalled;
    }

    public void mark(int buffSize) {
      markWasCalled = true;
      this.buffSize = buffSize;
      super.mark(buffSize);
    }

    public boolean markWasCalled() {
      return markWasCalled;
    }

    public int getMarkBufferSize() {
      return this.buffSize;
    }

    public void close() throws IOException {
      Exception ex = new Exception("Blah");
      logger.debug3("Close called on " + this, ex);
      closeWasCalled = true;
      super.close();
    }

    public boolean closeWasCalled() {
      return closeWasCalled;
    }

  }

  public static void main(String[] argv) {
    String[] testCaseList = { TestBaseUrlFetcher.class.getName() };
    junit.swingui.TestRunner.main(testCaseList);
  }

  private static class MockPermissionMap extends PermissionMap {
    public MockPermissionMap() {
      super(new MockCrawler().new MockCrawlerFacade(),
	    new ArrayList(), new ArrayList(), null);
    }

    protected void putStatus(String permissionUrl, PermissionStatus status)
            throws MalformedURLException {
      super.createRecord(permissionUrl).setStatus(status);
    }

  }

}
