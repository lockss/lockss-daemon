/*

Copyright (c) 2000-2020 Board of Trustees of Leland Stanford Jr. University,
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

import org.apache.commons.collections.set.ListOrderedSet;
import org.lockss.config.ConfigManager;
import org.lockss.config.Configuration;
import org.lockss.daemon.Crawler.CrawlerFacade;
import org.lockss.crawler.FollowLinkCrawler.MyLinkExtractorCallback;
import org.lockss.crawler.TestFollowLinkCrawler.MyMockUrlFetcher;
import org.lockss.crawler.TestFollowLinkCrawler2.MyMockArchivalUnit;
import org.lockss.daemon.*;
import org.lockss.util.*;
import org.lockss.plugin.*;
import org.lockss.plugin.UrlFetcher.FetchResult;
import org.lockss.plugin.base.BaseUrlFetcher;
import org.lockss.test.*;
import org.lockss.test.MockCrawler.MockCrawlerFacade;
import org.lockss.state.*;
import org.lockss.alert.*;
import org.lockss.util.urlconn.*;

import org.lockss.extractor.*;

import static org.lockss.crawler.BaseCrawler.PARAM_DEFAULT_RETRY_COUNT;
import static org.lockss.crawler.BaseCrawler.DEFAULT_DEFAULT_RETRY_COUNT;

/**
 * Tests for the new content crawler.
 */
public class TestFollowLinkCrawlerErrorPaths extends LockssTestCase {
  
  public static String LOGIN_ERROR_MSG = "Sample login page error";

  protected MockLockssDaemon theDaemon;
  protected CrawlManagerImpl crawlMgr;
  protected MyMockArchivalUnit mau = null;
  protected MockCachedUrlSet mcus = null;
  protected MockAuState aus;
  protected static List testUrlList = ListUtil.list("http://example.com");
  protected MockCrawlRule crawlRule = null;
  protected String startUrl = "http://www.example.com/index.html";
  protected String permissionPage = "http://www.example.com/permission.html";
  protected List startUrls;
  protected TestableFollowLinkCrawler crawler = null;
  protected MockLinkExtractor extractor = new MockLinkExtractor();
  protected CrawlerFacade cf;
  CrawlerStatus status;
  private MockPlugin plug;

  public void setUp() throws Exception {
    super.setUp();
    TimeBase.setSimulated(10);

    theDaemon = getMockLockssDaemon();
    crawlMgr = new NoPauseCrawlManagerImpl();
    theDaemon.setCrawlManager(crawlMgr);
    crawlMgr.initService(theDaemon);
    
    theDaemon.getAlertManager();
    
    plug = new MockPlugin(getMockLockssDaemon());
    plug.initPlugin(getMockLockssDaemon());
    mau = newMyMockArchivalUnit();
    mau.setPlugin(plug);
    mau.setAuId("MyMockTestAu");
    aus = new MockAuState(mau);
    startUrls = ListUtil.list(startUrl);
    mcus = (MockCachedUrlSet)mau.getAuCachedUrlSet();

    crawlRule = new MockCrawlRule();
    crawlRule.addUrlToCrawl(startUrl);
    crawlRule.addUrlToCrawl(permissionPage);
    mau.addUrl(permissionPage);
    mau.setStartUrls(startUrls);
    mau.setPermissionUrls(ListUtil.list(permissionPage));
    mau.setCrawlRule(crawlRule);
    mau.setRefetchDepth(1);
    crawlMgr.newCrawlRateLimiter(mau);
    crawler = new TestableFollowLinkCrawler(mau, aus);
    crawler.setDaemonPermissionCheckers(ListUtil.list(new MockPermissionChecker(2)));
    status = crawler.getCrawlerStatus();
    mau.setLinkExtractor("*", extractor);
    Properties p = new Properties();
    p.setProperty(FollowLinkCrawler.PARAM_DEFAULT_RETRY_DELAY, "0");
    p.setProperty(FollowLinkCrawler.PARAM_MIN_RETRY_DELAY, "0");
    p.setProperty("org.lockss.log.FollowLinkCrawler.level", "3");
    ConfigurationUtil.setCurrentConfigFromProps(p);
    cf = crawler.getCrawlerFacade();
  }
  
  public void testPermissionException() {
    crawler.setPermissionMap(new AlwaysPermissionMap(cf));
    mau.setUrlFetchers(ListUtil.list(new TestableUrlFetcher(cf, startUrl, new CacheException.PermissionException(LOGIN_ERROR_MSG))));
    assertFalse(crawler.fetch(new CrawlUrlData(startUrl, 0)));
    assertEquals(Crawler.STATUS_FETCH_ERROR, status.getCrawlStatus());
    assertEquals(true, crawler.isAborted());
    assertEquals(LOGIN_ERROR_MSG, status.getCrawlStatusMsg());
  }
  
  public void testPermission404(){
    CacheException.setDefaultSuppressStackTrace(false);
    String permissionUrl1 = "http://www.example.com/index.html";
    List permissionList = ListUtil.list(permissionUrl1);

    // Arrange for the UrlFetcher to throw RedirectOutsideCrawlSpecException
    CrawlerFacade cf = crawler.getCrawlerFacade();
    CacheException rocs = 
      new CacheException.NoRetryDeadLinkException("permission 404");
    TestableUrlFetcher uf =
      new TestableUrlFetcher(cf, permissionUrl1, rocs);
    mau.setUrlFetchers(ListUtil.list(uf));

    MockCachedUrlSet cus = (MockCachedUrlSet)mau.getAuCachedUrlSet();
    mau.setStartUrls(startUrls);
    mau.addUrl(permissionUrl1);
    mau.setPermissionUrls(permissionList);
    
    assertFalse(crawler.doCrawl());
    assertEquals(Crawler.STATUS_FETCH_ERROR, status.getCrawlStatus());
    assertEquals("Unable to fetch permission page", status.getCrawlStatusMsg());
    assertEmpty(cus.getCachedUrls());
  }

  public void testPermissionPageExcludedRedirect(){
    CacheException.setDefaultSuppressStackTrace(false);
    String permissionUrl1 = "http://www.example.com/index.html";
    List permissionList = ListUtil.list(permissionUrl1);

    // Arrange for the UrlFetcher to throw RedirectOutsideCrawlSpecException
    CrawlerFacade cf = crawler.getCrawlerFacade();
    CacheException rocs = 
      new CacheException.RedirectOutsideCrawlSpecException("ffff -> tttt");
    TestableUrlFetcher uf =
      new TestableUrlFetcher(cf, permissionUrl1, rocs);
    mau.setUrlFetchers(ListUtil.list(uf));

    MockCachedUrlSet cus = (MockCachedUrlSet)mau.getAuCachedUrlSet();
    mau.setStartUrls(startUrls);
    mau.addUrl(permissionUrl1);
    mau.setPermissionUrls(permissionList);
    
    assertFalse(crawler.doCrawl());
    assertEquals(Crawler.STATUS_FETCH_ERROR, status.getCrawlStatus());
    assertEquals("Unable to fetch permission page", status.getCrawlStatusMsg());
    assertEquals("ffff -> tttt", status.getErrorForUrl(permissionUrl1));
    assertEmpty(cus.getCachedUrls());
  }


  MyMockArchivalUnit newMyMockArchivalUnit() {
    NodeManager nodeManager = new MockNodeManager();
    MyMockArchivalUnit mau = new MyMockArchivalUnit();
    getMockLockssDaemon().setNodeManager(nodeManager, mau);
    return mau;
  }
  
  private class AlwaysPermissionMap extends PermissionMap {
    public AlwaysPermissionMap(CrawlerFacade cf) {
      super(cf,null,null,null);
    }
    
    @Override
    public boolean hasPermission(String url) {
      return true;
    }
  }
  
  private class TestableFollowLinkCrawler extends FollowLinkCrawler {
    List<PermissionChecker> daemonPermissionCheckers;

    protected TestableFollowLinkCrawler(ArchivalUnit au, AuState aus){
      super(au, aus);
      crawlStatus = new CrawlerStatus(au,
            au.getStartUrls(),
            null);
    }
    
    @Override
    public boolean fetch(CrawlUrlData curl) {
      return super.fetch(curl);
    }
    
    public void setPermissionMap(PermissionMap pMap) {
      permissionMap = pMap;
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
  
  protected class MyMockArchivalUnit extends MockArchivalUnit {
    List<UrlFetcher> fetcherList;
    RuntimeException getLinkExtractorThrows = null;
    
    public void setUrlFetchers(List<UrlFetcher> fetcherList) {
      this.fetcherList = fetcherList;
    }
    @Override
    public UrlFetcher makeUrlFetcher(CrawlerFacade mcf,
        String url) {
      if(fetcherList != null && !fetcherList.isEmpty()) {
        return fetcherList.remove(0);
      }
      return null;
    }

    public LinkExtractor getLinkExtractor(String mimeType) {
      if (getLinkExtractorThrows != null) {
        throw getLinkExtractorThrows;
      }
      return super.getLinkExtractor(mimeType);
    }
  }

  public class TestableUrlFetcher extends BaseUrlFetcher {
    IOException error;
    
    public TestableUrlFetcher(CrawlerFacade crawlFacade, String url, IOException error) {
      super(crawlFacade, url);
      this.error = error;
    }
    
    @Override
    protected InputStream getUncachedInputStreamOnly(String lastModified) throws IOException{
      if(error != null) {
	error.fillInStackTrace();
	throw error;
      }
      return null;
    }
    
    @Override
    public CIProperties getUncachedProperties() throws UnsupportedOperationException {
      return new CIProperties();
    }
    
  }

}

