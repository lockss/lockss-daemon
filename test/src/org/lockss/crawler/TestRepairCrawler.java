/*
 * $Id: TestRepairCrawler.java,v 1.9 2004-07-12 06:27:30 tlipkis Exp $
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
import org.lockss.daemon.*;
import org.lockss.util.*;
import org.lockss.plugin.*;
import org.lockss.protocol.*;
import org.lockss.state.*;
import org.lockss.test.*;

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
  private CrawlerImpl crawler = null;
  private MockContentParser parser = new MockContentParser();


  private String url1 = "http://example.com/blah.html";
  

  public void setUp() throws Exception {
    super.setUp();
    new MockLockssDaemon().getAlertManager();

    mau = new MockArchivalUnit();
    crawlRule = new MockCrawlRule();

    crawlRule.addUrlToCrawl(url1);

    spec = new CrawlSpec(startUrls, crawlRule);
    cus = new MockCachedUrlSet(mau, null);
    cus.addUrl(url1);

    mau.setAuCachedUrlSet(cus);
    mau.setPlugin(new MockPlugin());

    List repairUrls = ListUtil.list(url1);
    crawler = new RepairCrawler(mau, spec, aus, repairUrls, 0);
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
	   +" should throw an IllegalArgumentException");
    } catch (IllegalArgumentException iae) {
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
  }

  public void testRepairCrawlCallsForceCache() {
    spec = new CrawlSpec(startUrls, crawlRule, 1);

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
  }

  public void testRepairCrawlPokesWatchdog() {
    String repairUrl1 = "http://example.com/forcecache1.html";
    String repairUrl2 = "http://example.com/forcecache2.html";
    String repairUrl3 = "http://example.com/forcecache3.html";
    cus.addUrl(repairUrl1);
    cus.addUrl(repairUrl2);
    cus.addUrl(repairUrl3);
    crawlRule.addUrlToCrawl(repairUrl1);
    crawlRule.addUrlToCrawl(repairUrl2);
    crawlRule.addUrlToCrawl(repairUrl3);

    MockLockssWatchdog wdog = new MockLockssWatchdog();

    List repairUrls = ListUtil.list(repairUrl1, repairUrl2, repairUrl3);
    spec = new CrawlSpec(startUrls, crawlRule, 1);
    crawler = new RepairCrawler(mau, spec, aus, repairUrls, 0);
    crawler.setWatchdog(wdog);
    crawler.doCrawl();

    wdog.assertPoked(3);
  }

  public void testRepairCrawlDoesntFollowLinks() {
    String repairUrl1 = "http://www.example.com/forcecache.html";
    String repairUrl2 = "http://www.example.com/link3.html";
    String url1 = "http://www.example.com/link1.html";
    String url2 = "http://www.example.com/link2.html";
    
    cus.addUrl(repairUrl1);
    parser.addUrlSetToReturn(repairUrl1, SetUtil.set(url1, url2, repairUrl2));
    cus.addUrl(repairUrl2);
    cus.addUrl(url1);
    cus.addUrl(url2);
    crawlRule.addUrlToCrawl(repairUrl1);
    crawlRule.addUrlToCrawl(repairUrl2);
    crawlRule.addUrlToCrawl(url1);
    crawlRule.addUrlToCrawl(url2);

    List repairUrls = ListUtil.list(repairUrl1, repairUrl2);
    spec = new CrawlSpec(startUrls, crawlRule, 1);
    crawler = new RepairCrawler(mau, spec, aus, repairUrls, 0);

    crawler.doCrawl();

    Set cachedUrls = cus.getForceCachedUrls();
    assertSameElements(repairUrls, cachedUrls);
  }

  public void testPluginThrowsRuntimeException() {
    String repairUrl = "http://example.com/forcecache.html";
    cus.addUrl(repairUrl, new ExpectedRuntimeException("Test exception"), 0);
    List repairUrls = ListUtil.list(repairUrl);
     crawlRule.addUrlToCrawl(repairUrl);
    spec = new CrawlSpec(startUrls, crawlRule, 1);
    crawler = new RepairCrawler(mau, spec, aus, repairUrls, 0);

    assertFalse(crawler.doCrawl());
  }

//   public void testFetchFromOtherCache() throws UnknownHostException {
//     MockLockssDaemon theDaemon = new MockLockssDaemon();
//     MockIdentityManager idm = new MockIdentityManager();
//     LcapIdentity id = new LcapIdentity(LcapIdentity.stringToAddr("127.0.0.1"));
//     Map map = new HashMap();
//     map.put(id, new Long(10));
//     idm.setAgeedForAu(mau, map);
		      
//     theDaemon.setIdentityManager(idm);


//     String repairUrl = "http://example.com/blah.html";
//     MyRepairCrawler crawler =
//       new MyRepairCrawler(mau, spec, aus, ListUtil.list(repairUrl));

//     Properties p = new Properties();
//     p.setProperty(RepairCrawler.PARAM_FETCH_FROM_OTHER_CACHE, "true");
//     ConfigurationUtil.setCurrentConfigFromProps(p);

//     crawler.doCrawl(Deadline.MAX);
//     assertEquals(id, crawler.getContentSource(repairUrl));
//   }


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

    public MyRepairCrawler(ArchivalUnit au, CrawlSpec spec,
			   AuState aus, Collection repairUrls,
			   float percentFetchFromCache) {
      super(au, spec, aus, repairUrls, percentFetchFromCache);
    }

    protected void fetchFromCache(LcapIdentity id, String url) {
      contentMap.put(url, id);
    }

    public LcapIdentity getContentSource(String url) {
      return (LcapIdentity)contentMap.get(url);
    }

  }
}
