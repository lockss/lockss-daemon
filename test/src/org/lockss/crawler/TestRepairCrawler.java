/*
 * $Id: TestRepairCrawler.java,v 1.37 2006-04-23 05:51:51 tlipkis Exp $
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

package org.lockss.crawler;
import java.util.*;
import java.net.*;
import java.io.*;
import org.lockss.daemon.*;
import org.lockss.util.*;
import org.lockss.util.urlconn.*;
import org.lockss.plugin.*;
import org.lockss.protocol.*;
import org.lockss.state.*;
import org.lockss.test.*;
import org.lockss.util.urlconn.CacheException;
import org.lockss.protocol.IdentityManager.MalformedIdentityKeyException;

/**
 *TODO
 *1)add tests for fetching from other caches
 */

public class TestRepairCrawler extends LockssTestCase {
  private MockArchivalUnit mau = null;
  private MockCachedUrlSet cus = null;

  private CrawlSpec spec = null;
  private MockAuState aus = new MockAuState();
  private static List testUrlList = ListUtil.list("http://example.com");
  private MockCrawlRule crawlRule = null;
  private String startUrl = "http://www.example.com/index.html";
  private List startUrls = ListUtil.list(startUrl);
  private BaseCrawler crawler = null;
  private MockContentParser parser = new MockContentParser();
  private MockIdentityManager idm;
  private MockLockssDaemon theDaemon = getMockLockssDaemon();

  private String permissionPage = "http://example.com/permission.html";
  private String permissionPage2 = "http://example.com/permission2.html";

  private List permissionPages = ListUtil.list(permissionPage, permissionPage2);

  private String url1 = "http://example.com/blah.html";

  public void setUp() throws Exception {
    super.setUp();
    theDaemon = getMockLockssDaemon();
    theDaemon.getAlertManager();
    idm = new MockIdentityManager();
    theDaemon.setIdentityManager(idm);
    idm.initService(theDaemon);
    mau = new MockArchivalUnit();
    mau.setAuId("MyMockTestAu");
    cus = (MockCachedUrlSet)mau.getAuCachedUrlSet();
    crawlRule = new MockCrawlRule();

    crawlRule.addUrlToCrawl(url1);
    crawlRule.addUrlToCrawl(permissionPage);
    crawlRule.addUrlToCrawl(permissionPage2);

    spec = new SpiderCrawlSpec(startUrls, ListUtil.list(permissionPage),
			       crawlRule, 1);

    mau.setCrawlSpec(spec);
    mau.addUrl(url1);
    mau.addUrl(permissionPage, "noperm");

    mau.setPlugin(new MockPlugin());

    List repairUrls = ListUtil.list(url1);
    crawler = new RepairCrawler(mau, spec, aus, repairUrls, 0);
    ((BaseCrawler)crawler).daemonPermissionCheckers =
      ListUtil.list(new MockPermissionChecker(100));
  }

  public void testMrcThrowsForNullAu() {
    try {
      new RepairCrawler(null, spec, aus, testUrlList, 0);
      fail("Contstructing a RepairCrawler with a null ArchivalUnit"
	   +" should throw an IllegalArgumentException");
    } catch (IllegalArgumentException iae) {
    }
  }

  public void testMrcThrowsForNullSpec() {
    try {
      new RepairCrawler(mau, null, aus, testUrlList, 0);
      fail("Contstructing a RepairCrawler with a null CrawlSpec"
	   +" should throw an IllegalArgumentException");
    } catch (IllegalArgumentException iae) {
    }
  }

  public void testMrcThrowsForNullList() {
    try {
      new RepairCrawler(mau, spec, aus, null, 0);
      fail("Contstructing a RepairCrawler with a null repair list"
	   +" should throw a NullPointerException");
    } catch (NullPointerException npe) {
    }
  }

  public void testMrcThrowsForEmptyList() {
    try {
      new RepairCrawler(mau, spec, aus, ListUtil.list(), 0);
      fail("Contstructing a RepairCrawler with a empty repair list"
	   +" should throw an IllegalArgumentException");
    } catch (IllegalArgumentException iae) {
    }
  }

  public void testGetType() {
    assertEquals(Crawler.REPAIR, crawler.getType());
    assertEquals("Repair", crawler.getTypeString());
  }

  public void testIsWholeAU() {
    assertFalse(crawler.isWholeAU());
  }

  public void testRepairCrawlCallsForceCache() {
    spec = new SpiderCrawlSpec(startUrls, startUrls, crawlRule, 1);

    crawler.doCrawl();

    Set cachedUrls = cus.getForceCachedUrls();
    assertEquals(1, cachedUrls.size());
    assertTrue("cachedUrls: "+cachedUrls, cachedUrls.contains(url1));
  }

  public void testRepairCrawlObeysCrawlWindow() {
    spec.setCrawlWindow(new MyMockCrawlWindow());

    crawler.doCrawl();

    Set cachedUrls = cus.getForceCachedUrls();
    assertEquals(0, cachedUrls.size());
    assertEquals(Crawler.STATUS_WINDOW_CLOSED,
		 crawler.getStatus().getCrawlError());
  }

  public void testRepairCrawlPokesWatchdog() {
    String repairUrl1 = "http://example.com/forcecache1.html";
    String repairUrl2 = "http://example.com/forcecache2.html";
    String repairUrl3 = "http://example.com/forcecache3.html";
    mau.addUrl(repairUrl1);
    mau.addUrl(repairUrl2);
    mau.addUrl(repairUrl3);
    crawlRule.addUrlToCrawl(repairUrl1);
    crawlRule.addUrlToCrawl(repairUrl2);
    crawlRule.addUrlToCrawl(repairUrl3);

    MockLockssWatchdog wdog = new MockLockssWatchdog();

    List repairUrls = ListUtil.list(repairUrl1, repairUrl2, repairUrl3);
    spec = new SpiderCrawlSpec(startUrls, startUrls, crawlRule, 1);
    crawler = new RepairCrawler(mau, spec, aus, repairUrls, 0);
    crawler.setWatchdog(wdog);
    crawler.doCrawl();

    wdog.assertPoked(3);
  }

  public void testRepairCrawlObeysCrawlSpec() {
    String repairUrl1 = "http://example.com/url1.html";
    String repairUrl2 = "http://example.com/url2.html";

    mau.addUrl(repairUrl1);
    mau.addUrl(repairUrl2);

    crawlRule.addUrlToCrawl(repairUrl1);

    List repairUrls = ListUtil.list(repairUrl1, repairUrl2);
    spec = new SpiderCrawlSpec(startUrls, permissionPages, crawlRule, 1);
    crawler = new RepairCrawler(mau, spec, aus, repairUrls, 0);
    ((BaseCrawler)crawler).daemonPermissionCheckers =
      ListUtil.list(new MockPermissionChecker(100));

    assertTrue(crawler.doCrawl());

    Set cachedUrls = cus.getForceCachedUrls();
    assertSameElements(ListUtil.list(repairUrl1), cachedUrls);
  }

  public void testRepairCrawlObeysCrawlSepcV1Hack() {
    String repairUrl1 = "http://example.com/2005";
    String repairUrl2 = "http://example.com/2005/";

    mau.addUrl(repairUrl1);
    mau.addUrl(repairUrl2);

    crawlRule.addUrlToCrawl(repairUrl2);

    List repairUrls = ListUtil.list(repairUrl1);
    spec = new SpiderCrawlSpec(startUrls, permissionPages, crawlRule, 1);
    crawler = new RepairCrawler(mau, spec, aus, repairUrls, 0);
    ((BaseCrawler)crawler).daemonPermissionCheckers =
      ListUtil.list(new MockPermissionChecker(100));

    crawler.doCrawl();

    Set cachedUrls = cus.getForceCachedUrls();
    assertSameElements(ListUtil.list(repairUrl2), cachedUrls);
  }

  public void testRepairCrawlDoesntFollowLinks() {
    String repairUrl1 = "http://example.com/forcecache.html";
    String repairUrl2 = "http://example.com/link3.html";
    String url1 = "http://example.com/link1.html";
    String url2 = "http://example.com/link2.html";

    mau.addUrl(repairUrl1);
    parser.addUrlSetToReturn(repairUrl1, SetUtil.set(url1, url2, repairUrl2));
    mau.addUrl(repairUrl2);
    mau.addUrl(url1);
    mau.addUrl(url2);
    crawlRule.addUrlToCrawl(repairUrl1);
    crawlRule.addUrlToCrawl(repairUrl2);
    crawlRule.addUrlToCrawl(url1);
    crawlRule.addUrlToCrawl(url2);

    List repairUrls = ListUtil.list(repairUrl1, repairUrl2);
    spec = new SpiderCrawlSpec(startUrls, permissionPages, crawlRule, 1);
    crawler = new RepairCrawler(mau, spec, aus, repairUrls, 0);
    ((BaseCrawler)crawler).daemonPermissionCheckers =
      ListUtil.list(new MockPermissionChecker(100));

    crawler.doCrawl();

    Set cachedUrls = cus.getForceCachedUrls();
    assertSameElements(repairUrls, cachedUrls);
  }

  public void testPluginThrowsRuntimeException() {
    String repairUrl = "http://example.com/forcecache.html";
    mau.addUrl(repairUrl, new ExpectedRuntimeException("Test exception"), 1);
    List repairUrls = ListUtil.list(repairUrl);
    crawlRule.addUrlToCrawl(repairUrl);
    spec = new SpiderCrawlSpec(startUrls, startUrls, crawlRule, 1);
    crawler = new RepairCrawler(mau, spec, aus, repairUrls, 0);

    assertFalse(crawler.doCrawl());
  }

  //these tests use MyRepairCrawler, which overrides fetchFromCache
  //and fetchFromPublisher; these methods should be tested separately

  void setAgreeingPeers(int numPeers) throws MalformedIdentityKeyException {
    Map map = new HashMap();
    for (int ix = 0; ix < numPeers; ix++) {
      MockPeerIdentity id = new MockPeerIdentity("127.0.0."+ix);
      map.put(id, new Long(10+ix));
    }
    idm.setAgeedForAu(mau, map);
  }

  public void testFetchFromACacheOnly() throws MalformedIdentityKeyException {
    idm.addPeerIdentity("127.0.0.1", new MockPeerIdentity("127.0.0.1"));
    PeerIdentity id = idm.stringToPeerIdentity("127.0.0.1");

    Map map = new HashMap();
    map.put(id, new Long(10));
    idm.setAgeedForAu(mau, map);

    String repairUrl = "http://example.com/blah.html";

    mau.addUrl(repairUrl);
    MockUrlCacher muc = (MockUrlCacher)mau.makeUrlCacher(repairUrl);
    muc.setUncachedInputStream(new StringInputStream("blah"));
    muc.setUncachedProperties(new CIProperties());
    crawlRule.addUrlToCrawl(repairUrl);

    MyRepairCrawler crawler =
      new MyRepairCrawler(mau, spec, aus, ListUtil.list(repairUrl),0);
    ((BaseCrawler)crawler).daemonPermissionCheckers =
      ListUtil.list(new MockPermissionChecker(100));

    Properties p = new Properties();
    p.setProperty(RepairCrawler.PARAM_FETCH_FROM_OTHER_CACHES_ONLY, "true");
    ConfigurationUtil.setCurrentConfigFromProps(p);

    assertTrue("doCrawl() returned false", crawler.doCrawl());
    assertEquals("Fail! fetch from "+ crawler.getContentSource(repairUrl),
		 id, crawler.getContentSource(repairUrl));
    assertTrue("Fail! fetch from caches occur, fetchCacheCnt = " +
	         crawler.getFetchCacheCnt(), crawler.getFetchCacheCnt() == 1);
    assertTrue("Fail! fetch from publisher occurs",
	       crawler.getFetchPubCnt() == 0);
    Crawler.Status status = crawler.getStatus();
    assertEquals(SetUtil.set("127.0.0.1"), status.getSources());
    assertEquals("Successful", status.getCrawlStatus());
  }

  public void testFetchFromACacheOnlyPercent()
      throws MalformedIdentityKeyException {
    idm.addPeerIdentity("127.0.0.1", new MockPeerIdentity("127.0.0.1"));
    PeerIdentity id = idm.stringToPeerIdentity("127.0.0.1");

    Map map = new HashMap();
    map.put(id, new Long(10));
    idm.setAgeedForAu(mau, map);

    String repairUrl = "http://example.com/blah.html";

    mau.addUrl(repairUrl);
    MockUrlCacher muc = (MockUrlCacher)mau.makeUrlCacher(repairUrl);
    muc.setUncachedInputStream(new StringInputStream("blah"));
    muc.setUncachedProperties(new CIProperties());
    crawlRule.addUrlToCrawl(repairUrl);

    MyRepairCrawler crawler =
      makeCrawlerWPermission(mau, spec, aus, ListUtil.list(repairUrl), 100);

    assertTrue("doCrawl() returned false", crawler.doCrawl());
    assertEquals("Fail! fetch from "+ crawler.getContentSource(repairUrl),
		 id, crawler.getContentSource(repairUrl));
    assertTrue("Fail! fetch from caches occur, fetchCacheCnt = " +
	         crawler.getFetchCacheCnt(), crawler.getFetchCacheCnt() == 1);
    assertTrue("Fail! fetch from publisher occurs",
	       crawler.getFetchPubCnt() == 0);
    Crawler.Status status = crawler.getStatus();
    assertEquals(SetUtil.set("127.0.0.1"), status.getSources());
    assertEquals("Successful", status.getCrawlStatus());
  }

  public void testFetchFromCacheIgnoresLocalHost()
      throws MalformedIdentityKeyException {
    PeerIdentity id = idm.stringToPeerIdentity("127.0.0.1");
    idm.setLocalIdentity(id);

    Map map = new HashMap();
    map.put(id, new Long(10));
    idm.setAgeedForAu(mau, map);

    String repairUrl = "http://example.com/blah.html";
    MyRepairCrawler crawler =
      new MyRepairCrawler(mau, spec, aus, ListUtil.list(repairUrl),0);

    Properties p = new Properties();
    p.setProperty(RepairCrawler.PARAM_FETCH_FROM_OTHER_CACHES_ONLY, "true");
    ConfigurationUtil.setCurrentConfigFromProps(p);

    assertFalse("doCrawl() returned true", crawler.doCrawl());
    assertEquals("Tried to fetch from a cache", 0, crawler.getFetchCacheCnt());
    assertEquals("Fetch from publisher occured", 0, crawler.getFetchPubCnt());
  }

  public void testFetchFromCacheLocalHost()
      throws MalformedIdentityKeyException {
    Map map = new HashMap();
    MockPeerIdentity id = null;
    for (int ix = 0; ix < 3; ix++) {
      id = new MockPeerIdentity("127.0.0."+ix);
      map.put(id, new Long(10+ix));
    }
    idm.setLocalIdentity(id);
    idm.setAgeedForAu(mau, map);


    String repairUrl = "http://example.com/blah.html";
    MyRepairCrawler crawler =
      new MyRepairCrawler(mau, spec, aus, ListUtil.list(repairUrl),0);
    crawler.setTimesToThrow(3);

    Properties p = new Properties();
    p.setProperty(RepairCrawler.PARAM_FETCH_FROM_OTHER_CACHES_ONLY, "true");
    ConfigurationUtil.setCurrentConfigFromProps(p);

    assertFalse("doCrawl() returned true", crawler.doCrawl());
    assertEquals("Tried to fetch from more than 2 caches",
		 2, crawler.getFetchCacheCnt());
    assertEquals("Fetch from publisher occured", 0, crawler.getFetchPubCnt());
  }

  public void testFetchFromCacheLocalHostOnlyOne()
      throws MalformedIdentityKeyException {
    Map map = new HashMap();
    MockPeerIdentity id = new MockPeerIdentity("127.0.0.0");
    map.put(id, new Long(10));
    idm.setLocalIdentity(id);
    idm.setAgeedForAu(mau, map);


    String repairUrl = "http://example.com/blah.html";
    MyRepairCrawler crawler =
      new MyRepairCrawler(mau, spec, aus, ListUtil.list(repairUrl),0);
    crawler.setTimesToThrow(1);

    Properties p = new Properties();
    p.setProperty(RepairCrawler.PARAM_FETCH_FROM_OTHER_CACHES_ONLY, "true");
    ConfigurationUtil.setCurrentConfigFromProps(p);

    assertFalse("doCrawl() returned true", crawler.doCrawl());
    assertEquals("Tried to fetch from more than a cache",
		 0, crawler.getFetchCacheCnt());
    assertEquals("Fetch from publisher occured", 0, crawler.getFetchPubCnt());
  }

  public void testFetchFromOtherCachesOnlyWithoutRetryLimit()
      throws MalformedIdentityKeyException {

    setAgreeingPeers(3);

    String repairUrl = "http://example.com/blah.html";
    MyRepairCrawler crawler =
      new MyRepairCrawler(mau, spec, aus, ListUtil.list(repairUrl),0);
    crawler.setTimesToThrow(3);


    Properties p = new Properties();
    p.setProperty(RepairCrawler.PARAM_FETCH_FROM_OTHER_CACHES_ONLY, "true");
    ConfigurationUtil.setCurrentConfigFromProps(p);

    assertFalse("doCrawl() returned true", crawler.doCrawl());
    assertTrue("Fetch from caches occur, fetchCacheCnt = " +
	         crawler.getFetchCacheCnt() , crawler.getFetchCacheCnt() == 3);
    assertTrue("Fetch from publisher should never occur, yet FetchPubCnt = " +
	       crawler.getFetchPubCnt(), crawler.getFetchPubCnt() == 0);
  }

  public void testFetchFromOtherCachesOnlyWithRetryLimit()
      throws MalformedIdentityKeyException {
    setAgreeingPeers(3);

    String repairUrl = "http://example.com/blah.html";
    MyRepairCrawler crawler =
      new MyRepairCrawler(mau, spec, aus, ListUtil.list(repairUrl),0);
    crawler.setTimesToThrow(3);

    Properties p = new Properties();
    p.setProperty(RepairCrawler.PARAM_FETCH_FROM_OTHER_CACHES_ONLY, "true");
    p.setProperty(RepairCrawler.PARAM_NUM_RETRIES_FROM_CACHES, ""+2);
    ConfigurationUtil.setCurrentConfigFromProps(p);

    assertFalse("doCrawl() returned true", crawler.doCrawl());
    assertTrue("Fetch from caches occur, fetchCacheCnt = " +
	       crawler.getFetchCacheCnt() , crawler.getFetchCacheCnt() == 2);
    assertTrue("Fetch from publisher never occur",
               crawler.getFetchPubCnt() == 0);
  }

  public void testFetchFromCacheFailureNoMap() {
    idm.setAgeedForAu(mau, null);

    String repairUrl = "http://example.com/blah.html";
    MyRepairCrawler crawler =
      makeCrawlerWPermission(mau, spec, aus, ListUtil.list(repairUrl),0);

    Properties p = new Properties();
    p.setProperty(RepairCrawler.PARAM_FETCH_FROM_OTHER_CACHES_ONLY, "true");
    ConfigurationUtil.setCurrentConfigFromProps(p);

    assertFalse(crawler.doCrawl());
    Crawler.Status status = crawler.getStatus();
    assertEquals(Crawler.STATUS_FETCH_ERROR, status.getCrawlStatus());
  }

  public void testFetchFromCacheFailureEmptyMap() {
    idm.setAgeedForAu(mau, new HashMap());

    String repairUrl = "http://example.com/blah.html";
    MyRepairCrawler crawler =
      makeCrawlerWPermission(mau, spec, aus, ListUtil.list(repairUrl),0);

    Properties p = new Properties();
    p.setProperty(RepairCrawler.PARAM_FETCH_FROM_OTHER_CACHES_ONLY, "true");
    ConfigurationUtil.setCurrentConfigFromProps(p);

    assertFalse(crawler.doCrawl());
    Crawler.Status status = crawler.getStatus();
    assertEquals(Crawler.STATUS_FETCH_ERROR, status.getCrawlStatus());
  }

  public void testFetchFromPublisherOnly()
      throws MalformedIdentityKeyException {
    String repairUrl = "http://example.com/blah.html";
    MyRepairCrawler crawler =
      makeCrawlerWPermission(mau, spec, aus, ListUtil.list(repairUrl),0);

    mau.addUrl(repairUrl);
    crawlRule.addUrlToCrawl(repairUrl);

    Properties p = new Properties();
    //p.setProperty(RepairCrawler.PARAM_FETCH_FROM_OTHER_CACHES_ONLY, "false");
    p.setProperty(RepairCrawler.PARAM_FETCH_FROM_PUBLISHER_ONLY, "true");
    ConfigurationUtil.setCurrentConfigFromProps(p);

    assertTrue("doCrawl() returned false", crawler.doCrawl());
    assertTrue("Fetch from caches occur, fetchCacheCnt = " +
	        crawler.getFetchCacheCnt() , crawler.getFetchCacheCnt() == 0);
    assertTrue("Fetch from publisher" +
	       crawler.getFetchPubCnt(), crawler.getFetchPubCnt() == 1);
    Crawler.Status status = crawler.getStatus();
    assertEquals(SetUtil.set("Publisher"), status.getSources());
  }

  public void testFetchFromPublisherOnlyFailure()
      throws MalformedIdentityKeyException {
    setAgreeingPeers(3);

    String repairUrl = "http://example.com/blah.html";
    MyRepairCrawler crawler =
      new MyRepairCrawler(mau, spec, aus, ListUtil.list(repairUrl),0);
    crawler.setTimesToThrow(3);

    Properties p = new Properties();
    p.setProperty(RepairCrawler.PARAM_FETCH_FROM_PUBLISHER_ONLY, "true");
    ConfigurationUtil.setCurrentConfigFromProps(p);

    assertFalse("doCrawl() returned true", crawler.doCrawl());
    assertTrue("Fetch from caches occur, fetchCacheCnt = " +
	        crawler.getFetchCacheCnt() , crawler.getFetchCacheCnt() == 0);
    assertTrue("Fetch from publisher" +
	       crawler.getFetchPubCnt(), crawler.getFetchPubCnt() == 1);
  }

  public void testFetchFromOtherCachesThenPublisher()
      throws MalformedIdentityKeyException {
    setAgreeingPeers(3);


    String repairUrl = "http://example.com/blah.html";
    MyRepairCrawler crawler =
      makeCrawlerWPermission(mau, spec, aus, ListUtil.list(repairUrl),1);
    crawler.setTimesToThrow(2);

    Properties p = new Properties();
    //p.setProperty(RepairCrawler.PARAM_FETCH_FROM_OTHER_CACHES_ONLY, "false");
    p.setProperty(RepairCrawler.PARAM_NUM_RETRIES_FROM_CACHES, ""+2);
    ConfigurationUtil.setCurrentConfigFromProps(p);

    assertTrue("doCrawl() returned false", crawler.doCrawl());
    assertTrue("Fail fetch from other caches count, fetchCacheCnt = " +
	       crawler.getFetchCacheCnt() , crawler.getFetchCacheCnt() == 2);
    assertTrue("Fail fetch from publisher count, fetchPubCnt = " +
	       crawler.getFetchPubCnt(), crawler.getFetchPubCnt() == 1);
    assertTrue("Fail: sequence in caching from",
	       crawler.getCacheLastCall() < crawler.getPubLastCall() );
  }

  public void testFetchFromOtherCachesThenPublisherFailure()
      throws MalformedIdentityKeyException {
    setAgreeingPeers(3);


    String repairUrl = "http://example.com/blah.html";
    MyRepairCrawler crawler =
      new MyRepairCrawler(mau, spec, aus, ListUtil.list(repairUrl),1);
    crawler.setTimesToThrow(3);

    Properties p = new Properties();
    //p.setProperty(RepairCrawler.PARAM_FETCH_FROM_OTHER_CACHES_ONLY, "false");
    p.setProperty(RepairCrawler.PARAM_NUM_RETRIES_FROM_CACHES, ""+2);
    ConfigurationUtil.setCurrentConfigFromProps(p);

    assertFalse("doCrawl() returned true", crawler.doCrawl());
    assertTrue("Fail fetch from other caches count, fetchCacheCnt = " +
	       crawler.getFetchCacheCnt() , crawler.getFetchCacheCnt() == 2);
    assertTrue("Fail fetch from publisher count, fetchPubCnt = " +
	       crawler.getFetchPubCnt(), crawler.getFetchPubCnt() == 1);
    assertTrue("Fail: sequence in caching from",
	       crawler.getCacheLastCall() < crawler.getPubLastCall() );
  }

  public void testFetchFromPublisherThenOtherCaches()
      throws MalformedIdentityKeyException {
    setAgreeingPeers(3);

    String repairUrl = "http://example.com/blah.html";
    MyRepairCrawler crawler =
      makeCrawlerWPermission(mau, spec, aus, ListUtil.list(repairUrl),0);
    crawler.setTimesToThrow(2); //first publisher, then first other cache

    mau.addUrl(repairUrl);
    MockUrlCacher muc = (MockUrlCacher)mau.makeUrlCacher(repairUrl);
    muc.setUncachedInputStream(new StringInputStream("blah"));
    muc.setUncachedProperties(new CIProperties());
    crawlRule.addUrlToCrawl(repairUrl);


    Properties p = new Properties();
    p.setProperty(RepairCrawler.PARAM_NUM_RETRIES_FROM_CACHES, ""+2);
    ConfigurationUtil.setCurrentConfigFromProps(p);

    assertTrue("doCrawl() returned false", crawler.doCrawl());
    assertTrue("Fail fetch from publisher count, fetchPubCnt = " +
	       crawler.getFetchPubCnt(), crawler.getFetchPubCnt() == 1);
    assertTrue("Fail fetch from other caches count, fetchCacheCnt = " +
	       crawler.getFetchCacheCnt() , crawler.getFetchCacheCnt() == 2);
    assertTrue("Fail: sequence in caching from",
	       crawler.getCacheLastCall() > crawler.getPubLastCall() );
  }

  public void testFetchFromPublisherThenOtherCachesFailure()
      throws MalformedIdentityKeyException {
    setAgreeingPeers(3);

    String repairUrl = "http://example.com/blah.html";
    MyRepairCrawler crawler =
      new MyRepairCrawler(mau, spec, aus, ListUtil.list(repairUrl),0);
    crawler.setTimesToThrow(3); //first publisher, then first other cache


    Properties p = new Properties();
    p.setProperty(RepairCrawler.PARAM_NUM_RETRIES_FROM_CACHES, ""+2);
    ConfigurationUtil.setCurrentConfigFromProps(p);

    assertFalse("doCrawl() returned true", crawler.doCrawl());
    assertTrue("Fail fetch from publisher count, fetchPubCnt = " +
	       crawler.getFetchPubCnt(), crawler.getFetchPubCnt() == 1);
    assertTrue("Fail fetch from other caches count, fetchCacheCnt = " +
	       crawler.getFetchCacheCnt() , crawler.getFetchCacheCnt() == 2);
    assertTrue("Fail: sequence in caching from",
	       crawler.getCacheLastCall() > crawler.getPubLastCall() );
  }

  //Status tests
  public void testRepairedUrlsNotedInStatus() {
    Properties p = new Properties();
    p.setProperty(RepairCrawler.PARAM_FETCH_FROM_PUBLISHER_ONLY, "true");
    p.setProperty(RepairCrawler.PARAM_REPAIR_NEEDS_PERMISSION, "true");
    ConfigurationUtil.setCurrentConfigFromProps(p);

    String repairUrl1 = "http://example.com/blah.html";
    String repairUrl2 = "http://example.com/blah2.html";

    crawlRule.addUrlToCrawl(repairUrl1);
    crawlRule.addUrlToCrawl(repairUrl2);

    MyRepairCrawler crawler =
      makeCrawlerWPermission(mau, spec, aus,
                             ListUtil.list(repairUrl1, repairUrl2), 0);
    ((BaseCrawler)crawler).daemonPermissionCheckers =
      ListUtil.list(new MockPermissionChecker(100));

    mau.addUrl(repairUrl1);
    MockUrlCacher muc = (MockUrlCacher)mau.makeUrlCacher(repairUrl1);
    muc.setUncachedInputStream(new StringInputStream("blah"));
    muc.setUncachedProperties(new CIProperties());
    crawlRule.addUrlToCrawl(repairUrl1);

    mau.addUrl(repairUrl2);
    muc = (MockUrlCacher)mau.makeUrlCacher(repairUrl2);
    muc.setUncachedInputStream(new StringInputStream("blah"));
    muc.setUncachedProperties(new CIProperties());
    crawlRule.addUrlToCrawl(repairUrl2);

    assertTrue("doCrawl() returned false", crawler.doCrawl());
    Crawler.Status crawlStatus = crawler.getStatus();
//    assertEquals(3, crawlStatus.getNumFetched()); //2 repairs & permission page
    assertEquals(0, crawlStatus.getNumParsed());
    assertEquals(SetUtil.set(repairUrl1, repairUrl2, permissionPage),
		 crawlStatus.getUrlsFetched());
  }

  public void testPluginThrowsRuntimeExceptionDoesntUpdateStatus() {
    setRepairNeedsPermission(true);
    String repairUrl = "http://example.com/forcecache.html";
    mau.addUrl(repairUrl, new ExpectedRuntimeException("Test exception"), 1);
    List repairUrls = ListUtil.list(repairUrl);
    crawlRule.addUrlToCrawl(repairUrl);
    spec = new SpiderCrawlSpec(startUrls, permissionPages, crawlRule, 1);
    crawler = new RepairCrawler(mau, spec, aus, repairUrls, 0);
    ((BaseCrawler)crawler).daemonPermissionCheckers =
      ListUtil.list(new MockPermissionChecker(100));

    crawler.doCrawl();

    Crawler.Status crawlStatus = crawler.getStatus();
    assertEquals(1, crawlStatus.getNumFetched()); //permission page
    assertEquals(0, crawlStatus.getNumParsed());
    Map errorUrls = crawlStatus.getUrlsWithErrors();
    assertEquals("Unexpected Exception", (String)errorUrls.get(repairUrl));
    assertEquals(SetUtil.set(permissionPage), crawlStatus.getUrlsFetched());
  }

  private MyRepairCrawler makeCrawlerWPermission(ArchivalUnit au,
                                                 CrawlSpec spec,
                                                 AuState aus,
                                                 Collection repairUrls,
                                                 float percentFetchFromCache) {
    MyRepairCrawler crawler =
      new MyRepairCrawler(au, spec, aus, repairUrls, percentFetchFromCache);
    ((BaseCrawler)crawler).daemonPermissionCheckers =
      ListUtil.list(new MockPermissionChecker(100));
    return crawler;
  }

  public void testFailedFetchDoesntUpdateStatus()
      throws MalformedIdentityKeyException {
    Properties p = new Properties();
    p.setProperty(RepairCrawler.PARAM_FETCH_FROM_OTHER_CACHES_ONLY, "true");
    p.setProperty(RepairCrawler.PARAM_REPAIR_NEEDS_PERMISSION, "true");
    ConfigurationUtil.setCurrentConfigFromProps(p);

    String repairUrl = "http://example.com/blah.html";
    MyRepairCrawler crawler =
      makeCrawlerWPermission(mau, spec, aus, ListUtil.list(repairUrl),0);
    ((BaseCrawler)crawler).daemonPermissionCheckers =
      ListUtil.list(new MockPermissionChecker(100));

    crawler.setTimesToThrow(3);



    crawler.doCrawl();

    Crawler.Status crawlStatus = crawler.getStatus();
    assertEquals(1, crawlStatus.getNumFetched()); //permission page
    assertEquals(0, crawlStatus.getNumParsed());

    Map errorUrls = crawlStatus.getUrlsWithErrors();
    assertEquals(Crawler.STATUS_FETCH_ERROR, (String)errorUrls.get(repairUrl));
    assertEquals(SetUtil.set(permissionPage), crawlStatus.getUrlsFetched());
  }
  /**
   * convinience method to set the config up so that the repair crawl does or
   * does not need permission to get repairs
   * @param needsPermission
   */
  private void setRepairNeedsPermission(boolean needsPermission) {
    Properties p = new Properties();
    p.setProperty(RepairCrawler.PARAM_REPAIR_NEEDS_PERMISSION,
                  needsPermission ? "true" : "false");
    ConfigurationUtil.setCurrentConfigFromProps(p);
  }

  public void testGetPermissionMap() throws MalformedURLException {
    setRepairNeedsPermission(true);
    Set cachedUrls = cus.getCachedUrls();
    assertSameElements(ListUtil.list(), cachedUrls);

    PermissionMap pMap = crawler.getPermissionMap();
    assertNotNull(pMap);
    assertEquals(PermissionRecord.PERMISSION_OK,
		 pMap.getStatus("http://example.com/blah.html"));

    //verify that it fetched the permission page
    cachedUrls = cus.getCachedUrls();
    assertSameElements(ListUtil.list(permissionPage), cachedUrls);
  }

  /**
   * Don't generate a permission map by default
   */
  public void testGetPermissionMapDefault() throws MalformedURLException {
    Set cachedUrls = cus.getCachedUrls();
    assertSameElements(ListUtil.list(), cachedUrls);

    PermissionMap pMap = crawler.getPermissionMap();
    assertNull(pMap);
  }


  /**
   * Test that we don't try to crawl something we don't have permission to
   */
  public void testDontCrawlIfNoPermission() {
    setRepairNeedsPermission(true);

    String repairUrl1 = "http://www.example.com/url1.html";
    String repairUrl2 = "http://www.example.com/url2.html";
    ((BaseCrawler)crawler).daemonPermissionCheckers =
      ListUtil.list(new MockPermissionChecker(0));
    mau.addUrl(repairUrl1);
    mau.addUrl(repairUrl2);

    crawlRule.addUrlToCrawl(repairUrl1);
    crawlRule.addUrlToCrawl(repairUrl2);

    List repairUrls = ListUtil.list(repairUrl1, repairUrl2);
    spec = new SpiderCrawlSpec(startUrls, startUrls, crawlRule, 1);
    crawler = new RepairCrawler(mau, spec, aus, repairUrls, 0);

    crawler.doCrawl();

    Set cachedUrls = cus.getForceCachedUrls();
    assertSameElements(ListUtil.list(), cachedUrls);
   }

  /**
   * Test that we don't require permission for repair crawls is param set
   */
  public void testIgnorePermissionIfNoParam() {
    Properties p = new Properties();
    p.setProperty(RepairCrawler.PARAM_REPAIR_NEEDS_PERMISSION, "false");
    ConfigurationUtil.setCurrentConfigFromProps(p);

    String repairUrl1 = "http://www.example.com/url1.html";
    String repairUrl2 = "http://www.example.com/url2.html";
    ((BaseCrawler)crawler).daemonPermissionCheckers =
      ListUtil.list(new MockPermissionChecker(0));
    mau.addUrl(repairUrl1);
    mau.addUrl(repairUrl2);

    crawlRule.addUrlToCrawl(repairUrl1);
    crawlRule.addUrlToCrawl(repairUrl2);

    List repairUrls = ListUtil.list(repairUrl1, repairUrl2);
    spec = new SpiderCrawlSpec(startUrls, startUrls, crawlRule, 1);
    crawler = new RepairCrawler(mau, spec, aus, repairUrls, 0);

    crawler.doCrawl();

    Set cachedUrls = cus.getForceCachedUrls();
    assertSameElements(ListUtil.list(repairUrl1, repairUrl2), cachedUrls);
   }

  /**
   * Test that we don't require permission for repair crawls by default
   */
  public void testIgnorePermissionDefault() {
    String repairUrl1 = "http://www.example.com/url1.html";
    String repairUrl2 = "http://www.example.com/url2.html";
    ((BaseCrawler)crawler).daemonPermissionCheckers =
      ListUtil.list(new MockPermissionChecker(0));
    mau.addUrl(repairUrl1);
    mau.addUrl(repairUrl2);

    crawlRule.addUrlToCrawl(repairUrl1);
    crawlRule.addUrlToCrawl(repairUrl2);

    List repairUrls = ListUtil.list(repairUrl1, repairUrl2);
    spec = new SpiderCrawlSpec(startUrls, startUrls, crawlRule, 1);
    crawler = new RepairCrawler(mau, spec, aus, repairUrls, 0);

    crawler.doCrawl();

    Set cachedUrls = cus.getForceCachedUrls();
    assertSameElements(ListUtil.list(repairUrl1, repairUrl2), cachedUrls);
   }


  private class MyMockCrawlWindow implements CrawlWindow {
    public boolean canCrawl() {
      return false;
    }
    public boolean canCrawl(Date date) {
      return canCrawl();
    }
  }

  private class MyRepairCrawler extends RepairCrawler {
    private Map contentMap = new HashMap();
    private int fetchCacheCnt = 0;
    private int fetchPubCnt = 0;
    private int fetchSequence = 0;
    private int cacheLastCall = 0;
    private int pubLastCall = 0;
    private int timesToThrow = 0;

    public MyRepairCrawler(ArchivalUnit au, CrawlSpec spec,
			   AuState aus, Collection repairUrls,
			   float percentFetchFromCache) {
      super(au, spec, aus, repairUrls, percentFetchFromCache);
    }

    protected void fetchFromCache(String url, PeerIdentity id)
	throws IOException {
      fetchCacheCnt++;
      cacheLastCall = ++fetchSequence;
      contentMap.put(url,id);
      UrlCacher uc = makeRepairUrlCacher(url);
      if (timesToThrow > 0) {
	timesToThrow--;
	((MockUrlCacher)uc).setCachingException(new LockssUrlConnection.CantProxyException("Expected from cache"), 1);
      }
      fetchFromCache(uc, id);
    }

    /**
     * Set the number of times fetchFromCache and/or fetchFromPublisher
     * should throw a LockssUrlConnection.CantProxyException
     */
    public void setTimesToThrow(int timesToThrow) {
      this.timesToThrow = timesToThrow;
    }

    protected int getFetchCacheCnt(){
      return fetchCacheCnt;
    }

    protected void fetchFromPublisher(String url) throws IOException {
      UrlCacher uc = makeRepairUrlCacher(url);
      fetchPubCnt++;
      pubLastCall = ++fetchSequence;

      if (timesToThrow > 0) {
	timesToThrow--;
	((MockUrlCacher)uc).setCachingException(new CacheException("Expected from publisher"), 1);
      }

      fetchFromPublisher(uc);
    }

    protected int getFetchPubCnt(){
      return fetchPubCnt;
    }

    protected int getCacheLastCall(){
      return cacheLastCall;
    }

    protected int getPubLastCall() {
      return pubLastCall;
    }

    // make the repair fail and then go thru other caches or publisher
    protected int getProxyPort(){
      return 8080; //XXX for testing only
    }

    public PeerIdentity getContentSource(String url) {
      return (PeerIdentity)contentMap.get(url);
    }

  }
  public class MyMockUrlCacher extends MockUrlCacher {

    public MyMockUrlCacher(String url, MockArchivalUnit mau) {
      super(url, mau);
    }

    public boolean shouldBeCached(){
      return true;
    }

    public void setProxy(String proxyHost, int proxyPort) {
    }

    public InputStream getUncachedInputStream() throws IOException {
      throw new IOException("Expected Exception");
    }

    public CIProperties getUncachedProperties(){
      return new CIProperties();
    }
  }
}
