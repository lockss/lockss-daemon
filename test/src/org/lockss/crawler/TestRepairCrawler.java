/*
 * $Id$
 */

/*

Copyright (c) 2000-2011 Board of Trustees of Leland Stanford Jr. University,
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

import org.lockss.crawler.PermissionRecord.PermissionStatus;
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

  private MockAuState aus = new MockAuState();
  private static List testUrlList = ListUtil.list("http://example.com");
  private MockCrawlRule crawlRule = null;
  private String startUrl = "http://www.example.com/index.html";
  private List startUrls = ListUtil.list(startUrl);
  private MyRepairCrawler crawler = null;
  private MockLinkExtractor extractor = new MockLinkExtractor();
  private MockIdentityManager idm;
  private MockLockssDaemon theDaemon = getMockLockssDaemon();
  private CrawlManagerImpl crawlMgr;

  private String permissionPage = "http://example.com/permission.html";
  private String permissionPage2 = "http://example.com/permission2.html";

  private List permissionPages = ListUtil.list(permissionPage, permissionPage2);

  private String url1 = "http://example.com/blah.html";

  public void setUp() throws Exception {
    super.setUp();
    theDaemon = getMockLockssDaemon();
    crawlMgr = new NoPauseCrawlManagerImpl();
    theDaemon.setCrawlManager(crawlMgr);
    crawlMgr.initService(theDaemon);
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
    
    mau.setStartUrls(startUrls);
    mau.setPermissionUrls(ListUtil.list(permissionPage));
    mau.setCrawlRule(crawlRule);

    mau.addUrl(url1);
    mau.addUrl(permissionPage, "noperm");

    mau.setPlugin(new MockPlugin(getMockLockssDaemon()));

    List repairUrls = ListUtil.list(url1);
    crawler = new MyRepairCrawler(mau, aus, repairUrls);
    crawler.setCrawlManager(crawlMgr);
    crawlMgr.newCrawlRateLimiter(mau);
    crawler.daemonPermissionCheckers =
      ListUtil.list(new MockPermissionChecker(100));
  }

  public void testMrcThrowsForNullAu() {
    try {
      new MyRepairCrawler(null, aus, testUrlList);
      fail("Contstructing a RepairCrawler with a null ArchivalUnit"
           +" should throw an IllegalArgumentException");
    } catch (IllegalArgumentException iae) {
    }
  }

  public void testMrcThrowsForNullList() {
    try {
      new MyRepairCrawler(mau, aus, null);
      fail("Contstructing a RepairCrawler with a null repair list"
           +" should throw a NullPointerException");
    } catch (NullPointerException npe) {
    }
  }

  public void testMrcThrowsForEmptyList() {
    try {
      new MyRepairCrawler(mau, aus, ListUtil.list());
      fail("Contstructing a RepairCrawler with a empty repair list"
           +" should throw an IllegalArgumentException");
    } catch (IllegalArgumentException iae) {
    }
  }

  public void testGetType() {
    assertEquals(Crawler.Type.REPAIR, crawler.getType());
    assertEquals("Repair", crawler.getTypeString());
  }

  public void testIsWholeAU() {
    assertFalse(crawler.isWholeAU());
  }

  public void testRepairCrawlCallsForceCache() {
		mau.setPermissionUrls( startUrls);
		mau.setCrawlRule(crawlRule);
		mau.setRefetchDepth(1);

    assertTrue(crawler.doCrawl());

    Set cachedUrls = cus.getForceCachedUrls();
    assertEquals(1, cachedUrls.size());
    assertTrue("cachedUrls: "+cachedUrls, cachedUrls.contains(url1));
  }

  public void testRepairCrawlObeysCrawlWindow() {
    mau.setCrawlWindow(new MyMockCrawlWindow());

    crawler.doCrawl();

    Set cachedUrls = cus.getForceCachedUrls();
    assertEquals(0, cachedUrls.size());
    assertEquals(Crawler.STATUS_WINDOW_CLOSED,
                 crawler.getCrawlerStatus().getCrawlStatus());
  }

  public void testRepairCrawlIgnoreCrawlWindow() {
    ConfigurationUtil.addFromArgs(RepairCrawler.PARAM_MAX_REPAIRS_OUTSIDE_WINDOW, "5");

    String repairUrl1 = "http://example.com/url1.html";
    String repairUrl2 = "http://example.com/url2.html";

    mau.addUrl(repairUrl1);
    mau.addUrl(repairUrl2);

    crawlRule.addUrlToCrawl(repairUrl1);
    crawlRule.addUrlToCrawl(repairUrl2);

    List repairUrls = ListUtil.list(repairUrl1, repairUrl2);
    mau.setStartUrls(startUrls);
    mau.setPermissionUrls( permissionPages);
    mau.setCrawlRule(crawlRule);
    mau.setRefetchDepth( 1);
    mau.setCrawlWindow(new MyMockCrawlWindow());
    crawler = new MyRepairCrawler(mau, aus, repairUrls);
    crawler.setCrawlManager(crawlMgr);
    crawler.setDaemonPermissionCheckers(
      ListUtil.list(new MockPermissionChecker(100)));

    assertTrue(crawler.doCrawl());

    Set cachedUrls = cus.getForceCachedUrls();
    assertSameElements(ListUtil.list(repairUrl1, repairUrl2), cachedUrls);
  }

  public void testRepairCrawlIgnoreCrawlWindowLimitSize() {
    ConfigurationUtil.addFromArgs(RepairCrawler.PARAM_MAX_REPAIRS_OUTSIDE_WINDOW, "3");

    String repairUrl1 = "http://example.com/url1.html";
    String repairUrl2 = "http://example.com/url2.html";
    String repairUrl3 = "http://example.com/url3.html";
    String repairUrl4 = "http://example.com/url4.html";

    mau.addUrl(repairUrl1);
    mau.addUrl(repairUrl2);
    mau.addUrl(repairUrl3);
    mau.addUrl(repairUrl4);

    crawlRule.addUrlToCrawl(repairUrl1);
    crawlRule.addUrlToCrawl(repairUrl2);
    crawlRule.addUrlToCrawl(repairUrl3);
    crawlRule.addUrlToCrawl(repairUrl4);

    List repairUrls = ListUtil.list(repairUrl1, repairUrl2,
                                    repairUrl3, repairUrl4);
    mau.setStartUrls(startUrls);
mau.setPermissionUrls( permissionPages);
mau.setCrawlRule(crawlRule);
mau.setRefetchDepth( 1);
    mau.setCrawlWindow(new MyMockCrawlWindow());
    crawler = new MyRepairCrawler(mau, aus, repairUrls);
    crawler.setCrawlManager(crawlMgr);
    crawler.setDaemonPermissionCheckers(
      ListUtil.list(new MockPermissionChecker(100)));

    assertFalse(crawler.doCrawl());

    Set cachedUrls = cus.getForceCachedUrls();
    assertSameElements(ListUtil.list(repairUrl1, repairUrl2, repairUrl3),
                       cachedUrls);
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
    mau.setStartUrls(startUrls);
	mau.setPermissionUrls( startUrls);
	mau.setCrawlRule(crawlRule);
	mau.setRefetchDepth( 1);
    crawler = new MyRepairCrawler(mau, aus, repairUrls);
    crawler.setCrawlManager(crawlMgr);
    crawler.setWatchdog(wdog);
    crawler.doCrawl();

    wdog.assertPoked(3);
  }

  public void testRepairCrawlObeysCrawlRules() {
    String repairUrl1 = "http://example.com/url1.html";
    String repairUrl2 = "http://example.com/url2.html";

    mau.addUrl(repairUrl1, false, true);
    mau.addUrl(repairUrl2, false, false);

    List repairUrls = ListUtil.list(repairUrl1, repairUrl2);
    mau.setStartUrls(startUrls);
		mau.setPermissionUrls( permissionPages);

    crawler = new MyRepairCrawler(mau, aus, repairUrls);
    crawler.setCrawlManager(crawlMgr);
    crawler.setDaemonPermissionCheckers(
      ListUtil.list(new MockPermissionChecker(100)));

    assertTrue(crawler.doCrawl());

    Set cachedUrls = cus.getForceCachedUrls();
    assertSameElements(ListUtil.list(repairUrl1), cachedUrls);
  }

  public void testRepairCrawlObeysCrawlSepcV1Hack() {
    String repairUrl1 = "http://example.com/2005";
    String repairUrl2 = "http://example.com/2005/";

    mau.addUrl(repairUrl1, true, false);
    mau.addUrl(repairUrl2, true, true);

    List repairUrls = ListUtil.list(repairUrl1);
    mau.setStartUrls(startUrls);
		mau.setPermissionUrls( permissionPages);

    crawler = new MyRepairCrawler(mau, aus, repairUrls);
    crawler.setCrawlManager(crawlMgr);
    crawler.setDaemonPermissionCheckers(
      ListUtil.list(new MockPermissionChecker(100)));

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
    extractor.addUrlsToReturn(repairUrl1,
                                 SetUtil.set(url1, url2, repairUrl2));
    mau.addUrl(repairUrl2);
    mau.addUrl(url1);
    mau.addUrl(url2);
    crawlRule.addUrlToCrawl(repairUrl1);
    crawlRule.addUrlToCrawl(repairUrl2);
    crawlRule.addUrlToCrawl(url1);
    crawlRule.addUrlToCrawl(url2);

    List repairUrls = ListUtil.list(repairUrl1, repairUrl2);
    mau.setStartUrls(startUrls);
		mau.setPermissionUrls( permissionPages);
		mau.setCrawlRule(crawlRule);
		mau.setRefetchDepth( 1);
    crawler = new MyRepairCrawler(mau, aus, repairUrls);
    crawler.setCrawlManager(crawlMgr);
    crawler.setDaemonPermissionCheckers(
      ListUtil.list(new MockPermissionChecker(100)));

    crawler.doCrawl();

    Set cachedUrls = cus.getForceCachedUrls();
    assertSameElements(repairUrls, cachedUrls);
  }

  public void testPluginThrowsRuntimeException() {
    String repairUrl = "http://example.com/forcecache.html";
    mau.addUrl(repairUrl, new ExpectedRuntimeException("Test exception"), 1);
    List repairUrls = ListUtil.list(repairUrl);
    crawlRule.addUrlToCrawl(repairUrl);
    mau.setStartUrls(startUrls);
		mau.setPermissionUrls( startUrls);
		mau.setCrawlRule(crawlRule);
		mau.setRefetchDepth( 1);
    crawler = new MyRepairCrawler(mau, aus, repairUrls);
    crawler.setCrawlManager(crawlMgr);

    assertFalse(crawler.doCrawl());
  }

  //these tests use TestableRepairCrawler, which overrides fetchFromCache
  //and fetchFromPublisher; these methods should be tested separately

  void setAgreeingPeers(int numPeers) throws MalformedIdentityKeyException {
    Map map = new HashMap();
    for (int ix = 0; ix < numPeers; ix++) {
      MockPeerIdentity id = new MockPeerIdentity("127.0.0."+ix);
      map.put(id, new Long(10+ix));
    }
    idm.setAgeedForAu(mau, map);
  }


  public void testFetchFromPublisherOnly()
      throws MalformedIdentityKeyException {
    String repairUrl1 = "http://example.com/blah.html";
    String repairUrl2 = "http://example.com/flurb.html";
    TestableRepairCrawler crawler =
      makeCrawlerWPermission(mau, aus,
			     ListUtil.list(repairUrl1, repairUrl2));
    crawler.setCrawlManager(crawlMgr);

    mau.addUrl(repairUrl1).setContentSize(4321);
    mau.addUrl(repairUrl2).setContentSize(1357);
    crawlRule.addUrlToCrawl(repairUrl1);
    crawlRule.addUrlToCrawl(repairUrl2);

    assertTrue("doCrawl() returned false", crawler.doCrawl());
    assertEquals(0, crawler.getFetchCacheCnt());
    assertEquals(2, crawler.getFetchPubCnt());
    CrawlerStatus status = crawler.getCrawlerStatus();
    assertEquals(ListUtil.list("Publisher"), status.getSources());
    assertEquals(4321+1357, status.getContentBytesFetched());
  }

  public void testFetchFromPublisherOnlyFailure()
      throws MalformedIdentityKeyException {
    setAgreeingPeers(3);

    String repairUrl = "http://example.com/blah.html";
    TestableRepairCrawler crawler =
      new TestableRepairCrawler(mau, aus, ListUtil.list(repairUrl));
    crawler.setCrawlManager(crawlMgr);
    crawler.setTimesToThrow(3);
    
    assertFalse("doCrawl() returned true", crawler.doCrawl());
    assertTrue("Fetch from caches occur, fetchCacheCnt = " +
                crawler.getFetchCacheCnt() , crawler.getFetchCacheCnt() == 0);
    assertTrue("Fetch from publisher" +
               crawler.getFetchPubCnt(), crawler.getFetchPubCnt() == 1);
  }

  //Status tests
  public void testRepairedUrlsNotedInStatus() {
    ConfigurationUtil.addFromArgs(RepairCrawler.PARAM_REPAIR_NEEDS_PERMISSION, "true");

    String repairUrl1 = "http://example.com/blah.html";
    String repairUrl2 = "http://example.com/blah2.html";

    crawlRule.addUrlToCrawl(repairUrl1);
    crawlRule.addUrlToCrawl(repairUrl2);

    TestableRepairCrawler crawler =
      makeCrawlerWPermission(mau, aus,
                             ListUtil.list(repairUrl1, repairUrl2));
    crawler.setCrawlManager(crawlMgr);
    crawler.setDaemonPermissionCheckers(
      ListUtil.list(new MockPermissionChecker(100)));

    mau.addUrl(repairUrl1);
    MockUrlFetcher muf = 
        (MockUrlFetcher)crawler.makeRepairUrlFetcher(repairUrl1);
    muf.setUncachedInputStream(new StringInputStream("blah"));
    muf.setUncachedProperties(new CIProperties());
    crawlRule.addUrlToCrawl(repairUrl1);

    mau.addUrl(repairUrl2);
    muf = (MockUrlFetcher)crawler.makeRepairUrlFetcher(repairUrl1);
    muf.setUncachedInputStream(new StringInputStream("blah"));
    muf.setUncachedProperties(new CIProperties());
    crawlRule.addUrlToCrawl(repairUrl2);

    assertTrue("doCrawl() returned false", crawler.doCrawl());
    CrawlerStatus crawlStatus = crawler.getCrawlerStatus();
    assertEquals(3, crawlStatus.getNumFetched()); //2 repairs & permission page
    assertEquals(0, crawlStatus.getNumParsed());
    assertEquals(SetUtil.set(permissionPage, repairUrl1, repairUrl2),
                 SetUtil.theSet(crawlStatus.getUrlsFetched()));
  }

  public void testPluginThrowsRuntimeExceptionDoesntUpdateStatus() {
    setRepairNeedsPermission(true);
    String repairUrl = "http://example.com/forcecache.html";
    mau.addUrl(repairUrl, new ExpectedRuntimeException("Test exception"), 1);
    List repairUrls = ListUtil.list(repairUrl);
    crawlRule.addUrlToCrawl(repairUrl);
    mau.setStartUrls(startUrls);
		mau.setPermissionUrls( permissionPages);
		mau.setCrawlRule(crawlRule);
		mau.setRefetchDepth( 1);
    crawler = new MyRepairCrawler(mau, aus, repairUrls);
    crawler.setCrawlManager(crawlMgr);
    crawler.setDaemonPermissionCheckers(
      ListUtil.list(new MockPermissionChecker(100)));

    crawler.doCrawl();

    CrawlerStatus crawlStatus = crawler.getCrawlerStatus();
    assertEquals(1, crawlStatus.getNumFetched()); //permission page
    assertEquals(0, crawlStatus.getNumParsed());
    Map errorUrls = crawlStatus.getUrlsWithErrors();
    assertEquals("Unexpected error: Test exception", errorUrls.get(repairUrl));
    assertEquals(ListUtil.list(permissionPage), crawlStatus.getUrlsFetched());
  }

  private TestableRepairCrawler makeCrawlerWPermission(ArchivalUnit au,
                                                 AuState aus,
                                                 Collection repairUrls) {
    TestableRepairCrawler crawler =
      new TestableRepairCrawler(au, aus, repairUrls);
    crawler.setDaemonPermissionCheckers(
      ListUtil.list(new MockPermissionChecker(100)));
    return crawler;
  }

  public void testFailedFetchDoesntUpdateStatus()
      throws MalformedIdentityKeyException {
    ConfigurationUtil.addFromArgs(RepairCrawler.PARAM_REPAIR_NEEDS_PERMISSION, "true");

    String repairUrl = "http://example.com/blah.html";
    TestableRepairCrawler crawler =
      makeCrawlerWPermission(mau, aus, ListUtil.list(repairUrl));
    crawler.setCrawlManager(crawlMgr);
    crawler.setDaemonPermissionCheckers(
      ListUtil.list(new MockPermissionChecker(100)));

    crawler.setTimesToThrow(3);

    crawler.doCrawl();

    CrawlerStatus crawlStatus = crawler.getCrawlerStatus();
    assertEquals(1, crawlStatus.getNumFetched()); //permission page
    assertEquals(0, crawlStatus.getNumParsed());

    Map errorUrls = crawlStatus.getUrlsWithErrors();
    assertNotNull(errorUrls.get(repairUrl));
    assertEquals(ListUtil.list(permissionPage), crawlStatus.getUrlsFetched());
  }
  /**
   * convinience method to set the config up so that the repair crawl does or
   * does not need permission to get repairs
   * @param needsPermission
   */
  private void setRepairNeedsPermission(boolean needsPermission) {
    ConfigurationUtil.addFromArgs(RepairCrawler.PARAM_REPAIR_NEEDS_PERMISSION,
                                  needsPermission ? "true" : "false");
  }

  public void testGetPermissionMap() throws MalformedURLException {
    setRepairNeedsPermission(true);
    Set cachedUrls = cus.getCachedUrls();
    assertSameElements(ListUtil.list(), cachedUrls);
    crawler.populatePermissionMap();
    PermissionMap pMap = crawler.permissionMap;
    assertNotNull(pMap);
    assertEquals(PermissionStatus.PERMISSION_OK,
                 pMap.getStatus("http://example.com/blah.html"));
  }

  /**
   * Don't generate a permission map by default
   */
  public void testGetPermissionMapDefault() throws MalformedURLException {
    Set cachedUrls = cus.getCachedUrls();
    assertSameElements(ListUtil.list(), cachedUrls);
    PermissionMap pMap = crawler.permissionMap;
    assertNull(pMap);
  }


  /**
   * Test that we don't try to crawl something we don't have permission to
   */
  public void testDontCrawlIfNoPermission() {
    setRepairNeedsPermission(true);

    String repairUrl1 = "http://www.example.com/url1.html";
    String repairUrl2 = "http://www.example.com/url2.html";
    crawler.setDaemonPermissionCheckers(
      ListUtil.list(new MockPermissionChecker(0)));
    mau.addUrl(repairUrl1);
    mau.addUrl(repairUrl2);

    crawlRule.addUrlToCrawl(repairUrl1);
    crawlRule.addUrlToCrawl(repairUrl2);

    List repairUrls = ListUtil.list(repairUrl1, repairUrl2);
    mau.setStartUrls(startUrls);
		mau.setPermissionUrls( startUrls);
		mau.setCrawlRule(crawlRule);
		mau.setRefetchDepth( 1);
    crawler = new MyRepairCrawler(mau, aus, repairUrls);
    crawler.setCrawlManager(crawlMgr);

    crawler.doCrawl();

    Set cachedUrls = cus.getForceCachedUrls();
    assertSameElements(ListUtil.list(), cachedUrls);
   }

  /**
   * Test that we don't require permission for repair crawls is param set
   */
  public void testIgnorePermissionIfNoParam() {
    ConfigurationUtil.addFromArgs(RepairCrawler.PARAM_REPAIR_NEEDS_PERMISSION, "false");

    String repairUrl1 = "http://www.example.com/url1.html";
    String repairUrl2 = "http://www.example.com/url2.html";
    crawler.setDaemonPermissionCheckers(
      ListUtil.list(new MockPermissionChecker(0)));
    mau.addUrl(repairUrl1);
    mau.addUrl(repairUrl2);

    crawlRule.addUrlToCrawl(repairUrl1);
    crawlRule.addUrlToCrawl(repairUrl2);

    List repairUrls = ListUtil.list(repairUrl1, repairUrl2);
    mau.setStartUrls(startUrls);
		mau.setPermissionUrls( startUrls);
		mau.setCrawlRule(crawlRule);
		mau.setRefetchDepth( 1);
    crawler = new MyRepairCrawler(mau, aus, repairUrls);
    crawler.setCrawlManager(crawlMgr);

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
    crawler.setDaemonPermissionCheckers(
      ListUtil.list(new MockPermissionChecker(0)));
    mau.addUrl(repairUrl1);
    mau.addUrl(repairUrl2);

    crawlRule.addUrlToCrawl(repairUrl1);
    crawlRule.addUrlToCrawl(repairUrl2);

    List repairUrls = ListUtil.list(repairUrl1, repairUrl2);
    mau.setStartUrls(startUrls);
		mau.setPermissionUrls( startUrls);
		mau.setCrawlRule(crawlRule);
		mau.setRefetchDepth( 1);
    crawler = new MyRepairCrawler(mau, aus, repairUrls);
    crawler.setCrawlManager(crawlMgr);

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
    List<PermissionChecker> daemonPermissionCheckers;
    
    public MyRepairCrawler(ArchivalUnit au, AuState aus,
        Collection<String> repairUrls) {
      super(au, aus, repairUrls);
    }
    
    List<PermissionChecker> getDaemonPermissionCheckers() {
      if(daemonPermissionCheckers != null) {
        return daemonPermissionCheckers;
      } else {
        return super.getDaemonPermissionCheckers();
      }
    }
    
    public void setDaemonPermissionCheckers(List<PermissionChecker> pc) {
      this.daemonPermissionCheckers = pc;
    }
  }
  
  private class TestableRepairCrawler extends MyRepairCrawler {
    private Map contentMap = new HashMap();
    private int fetchCacheCnt = 0;
    private int fetchPubCnt = 0;
    private int fetchSequence = 0;
    private int cacheLastCall = 0;
    private int pubLastCall = 0;
    private int timesToThrow = 0;
    

    public TestableRepairCrawler(ArchivalUnit au,
                           AuState aus, Collection repairUrls) {
      super(au, aus, repairUrls);
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

    @Override
    protected void fetchFromPublisher(String url) throws IOException {
      fetchPubCnt++;
      pubLastCall = ++fetchSequence;

      super.fetchFromPublisher(url);
    }
    
    @Override
    protected UrlFetcher makeRepairUrlFetcher(String url) {
      if (timesToThrow > 0) {
        timesToThrow--;
        MockUrlFetcher uf = new MockUrlFetcher(this.getCrawlerFacade(), url);
        uf.setCachingException(new CacheException("Expected from publisher"), 1);
        return uf;
      } else {
        return super.makePermissionUrlFetcher(url);
      }
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
}
