/*
 * $Id$
 */

/*

Copyright (c) 2000-2012 Board of Trustees of Leland Stanford Jr. University,
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

import java.io.*;
import java.util.*;
import org.lockss.app.*;
import org.lockss.util.*;
import org.lockss.util.urlconn.CacheException;
import org.lockss.test.*;
import org.lockss.plugin.*;
import org.lockss.daemon.*;
import org.lockss.crawler.CrawlerStatus.*;
import static org.lockss.crawler.CrawlerStatus.CRAWL_STATUS_POOL;
import static org.lockss.crawler.CrawlerStatus.ReferrerType;
import static org.lockss.crawler.CrawlerStatus.Severity;

/**
 * Test class for CrawlerStatus.
 */
public class TestCrawlerStatus extends LockssTestCase {

  String url1 = "http://example.com/foo1";
  String url2 = "http://example.com/foobar2";
  String url3 = "http://example.com/query?foo=bar";
  String url_off1 = "http://nother.host/path/1";
  String url_off2 = "http://nother.host/path/2";

  String ref1 = "http://example.com/ref1";
  String ref2 = "http://example.com/ref2";
  String ref3 = "http://example.com/ref3";
  String ref4 = "http://example.com/ref4";

  MockArchivalUnit mau;
  CrawlerStatus cs;

  public void setUp() throws Exception {
    super.setUp();
    MockLockssDaemon daemon = getMockLockssDaemon();
    mau = MockArchivalUnit.newInited(daemon);
    PluginTestUtil.registerArchivalUnit(mau.getPlugin(), mau);
    mau.setUrlStems(ListUtil.list("http://example.com/"));
    cs = new CrawlerStatus(mau, null, "a type");
  }

  public void testAccessors() {
    mau.setAuId("Mach 7");
    CrawlerStatus c1 = new CrawlerStatus(mau, null, "Type 42");
    assertEquals(mau, c1.getAu());
    assertEquals("Mach 7", c1.getAuId());
    assertEquals("MockAU", c1.getAuName());
    assertEquals("Type 42", c1.getType());

    CrawlerStatus c2 = new CrawlerStatus(mau, null, null);
    assertNotEquals(c1.getKey(), c2.getKey());
  }

  public void testAuDeleted() throws IOException {
    CrawlerStatus c1 = new CrawlerStatus(mau, null, "Type 43");
    assertEquals(mau, c1.getAu());
    c1.auDeleted(mau);
    assertEquals(mau, c1.getAu());
    assertTrue(c1.isOffHost("http://foo.bar/"));
    PluginTestUtil.unregisterArchivalUnit(mau);
    c1.auDeleted(mau);
    // Shouldn't cause NPE, but now value should be default false
    assertFalse(c1.isOffHost("http://foo.bar/"));
  }

  public void testAllHaveDefaultMessage() {
    CrawlerStatus c1 = new CrawlerStatus(mau, null, "Type 42");
    for (int stat = 0; stat <= Crawler.STATUS_LAST; stat++) {
      assertNotMatchesRE("^Unknown code", c1.getDefaultMessage(stat));
    }
  }

  public void testGetDefaultMessage() {
    CrawlerStatus c1 = new CrawlerStatus(mau, null, "Type 42");
    assertEquals("Unknown",
		 c1.getDefaultMessage(Crawler.STATUS_UNKNOWN));
    assertEquals("Pending",
		 c1.getDefaultMessage(Crawler.STATUS_QUEUED));
    assertEquals("Active",
		 c1.getDefaultMessage(Crawler.STATUS_ACTIVE));
    assertEquals("Successful",
		 c1.getDefaultMessage(Crawler.STATUS_SUCCESSFUL));
    assertEquals("Error",
		 c1.getDefaultMessage(Crawler.STATUS_ERROR));
    assertEquals("Aborted",
		 c1.getDefaultMessage(Crawler.STATUS_ABORTED));
    assertEquals("Interrupted by crawl window",
		 c1.getDefaultMessage(Crawler.STATUS_WINDOW_CLOSED));
    assertEquals("Fetch error",
		 c1.getDefaultMessage(Crawler.STATUS_FETCH_ERROR));
    assertEquals("No permission from publisher",
		 c1.getDefaultMessage(Crawler.STATUS_NO_PUB_PERMISSION));
    assertEquals("Plugin error",
		 c1.getDefaultMessage(Crawler.STATUS_PLUGIN_ERROR));
    assertEquals("Repository error",
		 c1.getDefaultMessage(Crawler.STATUS_REPO_ERR));
    assertEquals("Interrupted by plugin reload or daemon exit",
		 c1.getDefaultMessage(Crawler.STATUS_RUNNING_AT_CRASH));
    assertEquals("Link extractor error",
		 c1.getDefaultMessage(Crawler.STATUS_EXTRACTOR_ERROR));
  }

  public void testInternedStatusStrings() {
    CrawlerStatus c1 = new CrawlerStatus(mau, null, "Type 42");
    for (int stat = 0; stat <= Crawler.STATUS_LAST; stat++) {
      String s = c1.getDefaultMessage(stat);
      assertSame(s, CRAWL_STATUS_POOL.intern(new String(s)));
    }
    String s1 = new String("Nonstandard status string");
    assertSame(s1, CRAWL_STATUS_POOL.intern(s1));
    assertNotSame(s1, CRAWL_STATUS_POOL.intern(new String(s1)));
  }

  public void testGetCrawlStatus() {
    CrawlerStatus c1 = new CrawlerStatus(mau, null, "Type 42");
    assertTrue(c1.isCrawlWaiting());
    assertFalse(c1.isCrawlActive());
    assertFalse(c1.isCrawlError());
    assertEquals("Pending", c1.getCrawlStatusMsg());
    assertEquals("Pending", c1.getCrawlErrorMsg());
    c1.signalCrawlStarted();
    assertFalse(c1.isCrawlWaiting());
    assertTrue(c1.isCrawlActive());
    assertFalse(c1.isCrawlError());
    assertEquals("Active", c1.getCrawlStatusMsg());
    assertEquals("Active", c1.getCrawlErrorMsg());
    c1.signalCrawlEnded();
    assertFalse(c1.isCrawlWaiting());
    assertFalse(c1.isCrawlActive());
    assertFalse(c1.isCrawlError());
    assertEquals("Successful", c1.getCrawlStatusMsg());
    assertEquals("Successful", c1.getCrawlErrorMsg());
  }

  public void testGetCrawlStatusError() {
    CrawlerStatus c1 = new CrawlerStatus(mau, null, "Type 42");
    assertTrue(c1.isCrawlWaiting());
    assertFalse(c1.isCrawlActive());
    assertFalse(c1.isCrawlError());
    assertEquals("Pending", c1.getCrawlStatusMsg());
    c1.signalCrawlStarted();
    assertFalse(c1.isCrawlWaiting());
    assertTrue(c1.isCrawlActive());
    assertFalse(c1.isCrawlError());
    assertEquals("Active", c1.getCrawlStatusMsg());
    assertEquals("Active", c1.getCrawlErrorMsg());
    // code sets error as soon as one happens, crawl must still appear
    // active
    c1.setCrawlStatus(Crawler.STATUS_NO_PUB_PERMISSION);
    assertFalse(c1.isCrawlWaiting());
    assertTrue(c1.isCrawlActive());
    assertTrue(c1.isCrawlError());
    assertEquals("Active", c1.getCrawlStatusMsg());
    assertEquals("No permission from publisher", c1.getCrawlErrorMsg());
    c1.signalCrawlEnded();
    assertFalse(c1.isCrawlWaiting());
    assertFalse(c1.isCrawlActive());
    assertTrue(c1.isCrawlError());
    assertEquals("No permission from publisher", c1.getCrawlStatusMsg());
    assertEquals("No permission from publisher", c1.getCrawlErrorMsg());
  }

  void setRecord(String types) {
    ConfigurationUtil.setFromArgs(CrawlerStatus.PARAM_RECORD_URLS, types);
  }

  public void testUrlCount() {
    UrlCount u1 = new UrlCount();
    assertEquals(0, u1.getCount());
    assertFalse(u1.hasList());
    assertFalse(u1.hasMap());
    assertEmpty(u1.getList());
    assertEmpty(u1.getMap());

    u1.addToList("foo");
    u1.addToList("foo");
    assertEquals(2, u1.getCount());
    assertFalse(u1.hasList());
    assertFalse(u1.hasMap());
    assertEmpty(u1.getList());
    assertEmpty(u1.getMap());

    UrlCount u2 = u1.seal(true);
    assertSame(u1, u2);
    assertFalse(u1.hasList());
    assertFalse(u1.hasMap());
    assertEmpty(u1.getList());
    assertEmpty(u1.getMap());
  }

  public void testUrlCountEmpty() {
    UrlCount u1, u2;
    u1 = new UrlCount();
    assertEquals(0, u1.getCount());
    assertFalse(u1.hasList());
    assertFalse(u1.hasMap());
    assertEmpty(u1.getList());
    assertEmpty(u1.getMap());
    assertSame(u1, u1.seal(true));
    assertEquals(0, u1.getCount());
    assertFalse(u1.hasList());
    assertFalse(u1.hasMap());
    assertEmpty(u1.getList());
    assertEmpty(u1.getMap());

    u1 = new UrlCount();
    assertSame(u1, u1.seal(false));
    assertEquals(0, u1.getCount());
    assertFalse(u1.hasList());
    assertFalse(u1.hasMap());
    assertEmpty(u1.getList());
    assertEmpty(u1.getMap());

    u1 = new UrlCountWithList();
    assertEquals(0, u1.getCount());
    assertTrue(u1.hasList());
    assertFalse(u1.hasMap());
    assertEmpty(u1.getList());
    assertEmpty(u1.getMap());
    u2 = u1.seal(true);
    assertEquals(0, u2.getCount());
    assertTrue(u2.hasList());
    assertFalse(u2.hasMap());
    assertEmpty(u2.getList());
    assertEmpty(u2.getMap());

    u1 = new UrlCountWithList();
    u2 = u1.seal(false);
    assertEquals(0, u2.getCount());
    assertFalse(u2.hasList());
    assertFalse(u2.hasMap());
    assertEmpty(u2.getList());
    assertEmpty(u2.getMap());

    u1 = new UrlCountWithSet();
    assertEquals(0, u1.getCount());
    assertTrue(u1.hasList());
    assertFalse(u1.hasMap());
    assertEmpty(u1.getList());
    assertEmpty(u1.getMap());
    u2 = u1.seal(true);
    assertEquals(0, u2.getCount());
    assertTrue(u2.hasList());
    assertFalse(u2.hasMap());
    assertEmpty(u2.getList());
    assertEmpty(u2.getMap());

    u1 = new UrlCountWithSet();
    u2 = u1.seal(false);
    assertEquals(0, u2.getCount());
    assertFalse(u2.hasList());
    assertFalse(u2.hasMap());
    assertEmpty(u2.getList());
    assertEmpty(u2.getMap());

    u1 = new UrlCountWithMap();
    assertEquals(0, u1.getCount());
    assertFalse(u1.hasList());
    assertTrue(u1.hasMap());
    assertEmpty(u1.getList());
    assertEmpty(u1.getMap());
    u2 = u1.seal(true);
    assertEquals(0, u2.getCount());
    assertFalse(u1.hasList());
    assertTrue(u1.hasMap());
    assertEmpty(u2.getList());
    assertEmpty(u2.getMap());

    u1 = new UrlCountWithMap();
    u2 = u1.seal(false);
    assertEquals(0, u2.getCount());
    assertFalse(u2.hasList());
    assertFalse(u2.hasMap());
    assertEmpty(u2.getList());
    assertEmpty(u2.getMap());
  }

  public void testUrlCountWithList() {
    UrlCount u1 = new CrawlerStatus.UrlCountWithList();
    assertEquals(0, u1.getCount());
    assertTrue(u1.hasList());
    assertFalse(u1.hasMap());
    assertEmpty(u1.getList());
    assertEmpty(u1.getMap());

    u1.addToList("foo");
    u1.addToList("bar");
    u1.addToList("foo");
    assertEquals(3, u1.getCount());
    assertTrue(u1.hasList());
    assertFalse(u1.hasMap());
    List lst = u1.getList();
    assertEquals(ListUtil.list("foo", "bar", "foo"), lst);
    assertEmpty(u1.getMap());

    UrlCount u2 = u1.seal(true);
    assertTrue(u1.hasList());
    assertFalse(u1.hasMap());
    assertEquals(lst, u2.getList());
    assertEmpty(u1.getMap());

    UrlCount u3 = new CrawlerStatus.UrlCountWithList().seal(true);
    assertEquals(0, u3.getCount());
    assertTrue(u3.hasList());
    assertFalse(u3.hasMap());
    assertEmpty(u3.getList());
    assertEmpty(u3.getMap());
  }

  public void testUrlCountWithMap() {
    UrlCount u1 = new CrawlerStatus.UrlCountWithMap();
    assertEquals(0, u1.getCount());
    assertFalse(u1.hasList());
    assertTrue(u1.hasMap());
    assertEmpty(u1.getList());
    assertEmpty(u1.getMap());

    u1.addToMap("foo", "1");
    u1.addToMap("bar", "2");
    assertEquals(2, u1.getCount());
    assertFalse(u1.hasList());
    assertTrue(u1.hasMap());
    Map map = u1.getMap();
    assertEquals(MapUtil.map("foo", "1", "bar", "2"), map);
    assertEquals(ListUtil.list("foo", "bar"), u1.getList());

    UrlCount u2 = u1.seal(true);
    assertFalse(u1.hasList());
    assertTrue(u1.hasMap());
    assertEquals(ListUtil.list("foo", "bar"), u1.getList());
    assertEquals(map, u2.getMap());

    UrlCount u3 = new CrawlerStatus.UrlCountWithMap().seal(true);
    assertEquals(0, u3.getCount());
    assertFalse(u3.hasList());
    assertTrue(u3.hasMap());
    assertEmpty(u3.getList());
    assertEmpty(u3.getMap());
  }

  public void testFetched() {
    assertEmpty(cs.getUrlsFetched());
    assertEquals(0, cs.getNumFetched());
    UrlCount uc = cs.getFetchedCtr();
    assertEmpty(uc.getList());
    assertEquals(0, uc.getCount());
    
    cs.signalUrlFetched(url1);

    assertEquals(ListUtil.list(url1), cs.getUrlsFetched());
    assertEquals(1, cs.getNumFetched());
    uc = cs.getFetchedCtr();
    assertEquals(cs.getUrlsFetched(), uc.getList());
    assertEquals(1, uc.getCount());

    cs.signalUrlFetched(url2);
    cs.signalUrlFetched(url1);

    assertEquals(ListUtil.list(url1, url2, url1), cs.getUrlsFetched());
    assertEquals(3, cs.getNumFetched());
    uc = cs.getFetchedCtr();
    assertEquals(cs.getUrlsFetched(), uc.getList());
    assertEquals(3, uc.getCount());

  }

  public void testParsed() {
    assertEmpty(cs.getUrlsParsed());
    assertEquals(0, cs.getNumParsed());
    UrlCount uc = cs.getParsedCtr();
    assertTrue(uc.hasList());
    assertEmpty(uc.getList());
    assertEquals(0, uc.getCount());
    
    cs.signalUrlParsed(url1);

    assertEquals(ListUtil.list(url1), cs.getUrlsParsed());
    assertEquals(1, cs.getNumParsed());
    uc = cs.getParsedCtr();
    assertEquals(cs.getUrlsParsed(), uc.getList());
    assertEquals(1, uc.getCount());

    cs.signalUrlParsed(url2);
    cs.signalUrlParsed(url1);

    assertEquals(ListUtil.list(url1, url2, url1), cs.getUrlsParsed());
    assertEquals(3, cs.getNumParsed());
    uc = cs.getParsedCtr();
    assertEquals(cs.getUrlsParsed(), uc.getList());
    assertEquals(3, uc.getCount());
  }

  public void testParsedNo() {
    setRecord("fetched,error");
    CrawlerStatus cs = new CrawlerStatus(mau, null, "Type 42");
    assertEmpty(cs.getUrlsParsed());
    assertEquals(0, cs.getNumParsed());
    UrlCount uc = cs.getParsedCtr();
    assertFalse(uc.hasList());
    assertEmpty(uc.getList());
    assertEquals(0, uc.getCount());
    
    cs.signalUrlParsed(url1);

    assertEmpty(cs.getUrlsParsed());
    assertEquals(1, cs.getNumParsed());
    uc = cs.getParsedCtr();
    assertEmpty(uc.getList());
    assertEquals(1, uc.getCount());

    cs.signalUrlParsed(url2);
    cs.signalUrlParsed(url1);

    assertEmpty(cs.getUrlsParsed());
    assertEquals(3, cs.getNumParsed());
  }

  public void testExcluded() {
    ConfigurationUtil.setFromArgs(CrawlerStatus.PARAM_KEEP_OFF_HOST_EXCLUDES,
				  "50");
    cs = new CrawlerStatus(mau, null, "a type");
    assertEmpty(cs.getUrlsExcluded());
    assertEquals(0, cs.getNumExcluded());
    UrlCount uc = cs.getExcludedCtr();
    assertEmpty(uc.getList());
    assertEquals(0, uc.getCount());
    
    cs.signalUrlExcluded(url1);

    assertEquals(ListUtil.list(url1), cs.getUrlsExcluded());
    assertEquals(1, cs.getNumExcluded());
    uc = cs.getExcludedCtr();
    assertEquals(cs.getUrlsExcluded(), uc.getList());
    assertEquals(1, uc.getCount());

    cs.signalUrlExcluded(url2);
    cs.signalUrlExcluded(url1);
    cs.signalUrlExcluded(url_off1);

    assertEquals(ListUtil.list(url1, url2, url_off1), cs.getUrlsExcluded());
    assertEquals(3, cs.getNumExcluded());
    uc = cs.getExcludedCtr();
    assertEquals(cs.getUrlsExcluded(), uc.getList());
    assertEquals(3, uc.getCount());
  }

  public void testExcludedExcludes() {
    ConfigurationUtil.setFromArgs(CrawlerStatus.PARAM_KEEP_OFF_HOST_EXCLUDES,
				  "1");
    cs = new CrawlerStatus(mau, null, "a type");
    assertEmpty(cs.getUrlsExcluded());
    assertEquals(0, cs.getNumExcluded());
    UrlCount uc = cs.getExcludedCtr();
    assertEmpty(uc.getList());
    assertEquals(0, uc.getCount());
    assertEquals(0, cs.getNumExcludedExcludes());
    
    cs.signalUrlExcluded(url1);

    cs.signalUrlExcluded(url2);
    cs.signalUrlExcluded(url1);
    assertEquals(2, cs.getNumExcluded());
    assertEquals(0, cs.getNumExcludedExcludes());
    cs.signalUrlExcluded(url_off1);
    assertEquals(3, cs.getNumExcluded());
    assertEquals(0, cs.getNumExcludedExcludes());
    cs.signalUrlExcluded(url_off2);

    assertEquals(ListUtil.list(url1, url2, url_off1), cs.getUrlsExcluded());
    assertEquals(3, cs.getNumExcluded());
    assertEquals(1, cs.getNumExcludedExcludes());
    uc = cs.getExcludedCtr();
    assertEquals(cs.getUrlsExcluded(), uc.getList());
    assertEquals(3, uc.getCount());
  }

  public void testNotModified() {
    assertEmpty(cs.getUrlsNotModified());
    assertEquals(0, cs.getNumNotModified());
    UrlCount uc = cs.getNotModifiedCtr();
    assertEmpty(uc.getList());
    assertEquals(0, uc.getCount());
    
    cs.signalUrlNotModified(url1);

    assertEquals(ListUtil.list(url1), cs.getUrlsNotModified());
    assertEquals(1, cs.getNumNotModified());
    uc = cs.getNotModifiedCtr();
    assertEquals(cs.getUrlsNotModified(), uc.getList());
    assertEquals(1, uc.getCount());

    cs.signalUrlNotModified(url2);
    cs.signalUrlNotModified(url1);

    assertEquals(ListUtil.list(url1, url2, url1), cs.getUrlsNotModified());
    assertEquals(3, cs.getNumNotModified());
    uc = cs.getNotModifiedCtr();
    assertEquals(cs.getUrlsNotModified(), uc.getList());
    assertEquals(3, uc.getCount());
  }

  public void testPending() {
    assertEmpty(cs.getUrlsPending());
    assertEquals(0, cs.getNumPending());
    UrlCount uc = cs.getPendingCtr();
    assertEmpty(uc.getList());
    assertEquals(0, uc.getCount());
    
    cs.addPendingUrl(url1);

    assertEquals(ListUtil.list(url1), cs.getUrlsPending());
    assertEquals(1, cs.getNumPending());
    uc = cs.getPendingCtr();
    assertEquals(cs.getUrlsPending(), uc.getList());
    assertEquals(1, uc.getCount());

    cs.addPendingUrl(url2);
    cs.addPendingUrl(url3);

    assertEquals(ListUtil.list(url1, url2, url3), cs.getUrlsPending());
    assertEquals(3, cs.getNumPending());
    uc = cs.getPendingCtr();
    assertEquals(cs.getUrlsPending(), uc.getList());
    assertEquals(3, uc.getCount());

    cs.removePendingUrl(url1);

    assertEquals(ListUtil.list(url2, url3), cs.getUrlsPending());
    assertEquals(2, cs.getNumPending());
    uc = cs.getPendingCtr();
    assertEquals(cs.getUrlsPending(), uc.getList());
    assertEquals(2, uc.getCount());

    cs.removePendingUrl(url2);
    cs.removePendingUrl(url3);

    assertEmpty(cs.getUrlsPending());
    assertEquals(0, cs.getNumPending());
    uc = cs.getPendingCtr();
    assertEmpty(uc.getList());
    assertEquals(0, uc.getCount());
  }

  CrawlerStatus.UrlErrorInfo uei(String msg) {
    return uei(msg, CrawlerStatus.Severity.Warning);
  }

  CrawlerStatus.UrlErrorInfo uei(String msg, CrawlerStatus.Severity sev) {
    return new CrawlerStatus.UrlErrorInfo(msg, sev);
  }


  public void testErrors() {
    assertEmpty(cs.getUrlsWithErrors());
    assertEquals(0, cs.getNumUrlsWithErrors());
    UrlCount uc = cs.getErrorCtr();
    assertEmpty(uc.getMap());
    assertEquals(0, uc.getCount());
    
    cs.signalErrorForUrl(url1, "err 1");

    assertEquals(MapUtil.map(url1, uei("err 1", Severity.Warning)),
		 cs.getUrlsErrorMap());
    assertEquals(1, cs.getNumUrlsWithErrors());
    assertEquals(1, cs.getNumUrlsWithErrorsOfSeverity(Severity.Warning));
    assertEquals(0, cs.getNumUrlsWithErrorsOfSeverity(Severity.Error));
    uc = cs.getErrorCtr();
    assertEquals(cs.getUrlsErrorMap(), uc.getMap());
    assertEquals(1, uc.getCount());

    cs.signalErrorForUrl(url2, "err 2", Crawler.STATUS_FETCH_ERROR);
    assertEquals(Crawler.STATUS_FETCH_ERROR, cs.getCrawlStatus());

    cs.signalErrorForUrl(url3, "err 42", Crawler.STATUS_FETCH_ERROR);

    assertEquals(3, cs.getNumUrlsWithErrors());
    assertEquals(1, cs.getNumUrlsWithErrorsOfSeverity(Severity.Warning));
    assertEquals(2, cs.getNumUrlsWithErrorsOfSeverity(Severity.Error));

    cs.signalErrorForUrl(url1, new CacheException.PermissionException("err 3"));

    assertEquals(MapUtil.map(url1, uei("err 3", Severity.Fatal),
			     url2, uei("err 2", Severity.Error),
			     url3, uei("err 42", Severity.Error)),
		 cs.getUrlsErrorMap());

    uc = cs.getErrorCtr();
    assertEquals(cs.getUrlsErrorMap(), uc.getMap());
    assertEquals(3, uc.getCount());
  }

  public void testErrorsNo() {
    setRecord("fetched,parsed");
    CrawlerStatus cs = new CrawlerStatus(mau, null, "Type 42");
    assertEmpty(cs.getUrlsWithErrors());
    assertEquals(0, cs.getNumUrlsWithErrors());
    UrlCount uc = cs.getErrorCtr();
    assertEmpty(uc.getMap());
    assertEquals(0, uc.getCount());
    
    cs.signalErrorForUrl(url1, "err 1");

    assertEmpty(uc.getMap());
    assertEquals(1, cs.getNumUrlsWithErrors());
    assertEquals(1, cs.getNumUrlsWithErrorsOfSeverity(Severity.Warning));
    assertEquals(0, cs.getNumUrlsWithErrorsOfSeverity(Severity.Error));
    uc = cs.getErrorCtr();
    assertEquals(cs.getUrlsWithErrors(), uc.getMap());
    assertEquals(1, uc.getCount());

    cs.signalErrorForUrl(url2, "err 2");
    cs.signalErrorForUrl(url1, "err 3");
    cs.signalErrorForUrl(url3, "err 3", Crawler.STATUS_FETCH_ERROR);

    assertEmpty(cs.getUrlsWithErrors());
    assertEquals(4, cs.getNumUrlsWithErrors());
    assertEquals(3, cs.getNumUrlsWithErrorsOfSeverity(Severity.Warning));
    assertEquals(1, cs.getNumUrlsWithErrorsOfSeverity(Severity.Error));
  }

  public void testReferrersNo() {
    ConfigurationUtil.setFromArgs(CrawlerStatus.PARAM_RECORD_REFERRERS_MODE,
				  "None");
    CrawlerStatus cs = new CrawlerStatus(mau, null, "Type 42");
    assertFalse(cs.hasReferrers());
    assertFalse(cs.hasReferrersOfType(ReferrerType.Included));
    assertFalse(cs.hasReferrersOfType(ReferrerType.Excluded));
  }

  public void testReferrersFirst() {
    ConfigurationUtil.setFromArgs(CrawlerStatus.PARAM_RECORD_REFERRERS_MODE,
				  "First");
    CrawlerStatus cs = new CrawlerStatus(mau, null, "Type 42");
    assertTrue(cs.hasReferrers());
    assertTrue(cs.hasReferrersOfType(ReferrerType.Included));
    assertTrue(cs.hasReferrersOfType(ReferrerType.Excluded));
    assertEmpty(cs.getReferrers(url1));

    cs.signalReferrer(url1, ref1, ReferrerType.Included);
    assertEquals(ListUtil.list(ref1), cs.getReferrers(url1));
    cs.signalReferrer(url1, ref2, ReferrerType.Included);
    assertEquals(ListUtil.list(ref1), cs.getReferrers(url1));
    cs.signalReferrer(url2, ref2, ReferrerType.Excluded);
    assertEquals(ListUtil.list(ref2), cs.getReferrers(url2));
    assertEquals(ListUtil.list(ref1), cs.getReferrers(url1));
  }

  public void testReferrersAll() {
    ConfigurationUtil.setFromArgs(CrawlerStatus.PARAM_RECORD_REFERRERS_MODE,
				  "All");
    CrawlerStatus cs = new CrawlerStatus(mau, null, "Type 42");
    assertTrue(cs.hasReferrers());
    assertTrue(cs.hasReferrersOfType(ReferrerType.Included));
    assertTrue(cs.hasReferrersOfType(ReferrerType.Excluded));
    assertEmpty(cs.getReferrers(url1));

    cs.signalReferrer(url1, ref1, ReferrerType.Included);
    assertEquals(ListUtil.list(ref1), cs.getReferrers(url1));
    assertEmpty(cs.getReferrers(url2));
    cs.signalReferrer(url1, ref2, ReferrerType.Excluded);
    assertEquals(ListUtil.list(ref1, ref2), cs.getReferrers(url1));
    cs.signalReferrer(url2, ref2, ReferrerType.Included);
    assertEquals(ListUtil.list(ref2), cs.getReferrers(url2));
    assertEquals(ListUtil.list(ref1, ref2), cs.getReferrers(url1));
    cs.signalReferrer(url1, ref1, ReferrerType.Included);
    // signalReferrer() contract is to not be called redundantly
    // Documenting its current list sementics, but a set implementation
    // would be ok.
    assertEquals(ListUtil.list(ref1, ref2, ref1), cs.getReferrers(url1));
  }

  // Ensure referrers.seal() doesn't throw NPE if map not created yet.
  public void testReferrersSealEmpty() {
    ConfigurationUtil.setFromArgs(CrawlerStatus.PARAM_RECORD_REFERRERS_MODE,
				  "All");
    CrawlerStatus cs = new CrawlerStatus(mau, null, "Type 42");
    assertTrue(cs.hasReferrers());
    cs.signalErrorForUrl(url1, "err 1");
    cs.sealCounters();
    assertEmpty(cs.getReferrers(url1));
  }

  public void testReferrersSeal(CrawlerStatus cs) {
    assertTrue(cs.hasReferrers());

    cs.signalReferrer(url1, ref1, ReferrerType.Included);
    cs.signalReferrer(url2, ref1, ReferrerType.Included);
    cs.signalReferrer(url3, ref1, ReferrerType.Included);
    cs.signalUrlFetched(url1);
    cs.signalUrlFetched(url2);
    cs.signalErrorForUrl(url3, "foo");

    cs.sealCounters();
    return;
  }

  public void testReferrersSeal1() {
    ConfigurationUtil.setFromArgs(CrawlerStatus.PARAM_RECORD_REFERRERS_MODE,
				  "All");
    CrawlerStatus cs = new CrawlerStatus(mau, null, "Type 42");
    testReferrersSeal(cs);
    assertEmpty(cs.getReferrers(url1));
    assertEmpty(cs.getReferrers(url2));
    assertEquals(ListUtil.list(ref1), cs.getReferrers(url3));
  }

  public void testReferrersSeal2() {
    ConfigurationUtil.setFromArgs(CrawlerStatus.PARAM_RECORD_REFERRERS_MODE,
				  "All",
				  CrawlerStatus.PARAM_KEEP_URLS, "all");
    CrawlerStatus cs = new CrawlerStatus(mau, null, "Type 42");
    testReferrersSeal(cs);
    assertEquals(ListUtil.list(ref1), cs.getReferrers(url1));
    assertEquals(ListUtil.list(ref1), cs.getReferrers(url2));
    assertEquals(ListUtil.list(ref1), cs.getReferrers(url3));
  }

  public void testReferrersIncluded() {
    ConfigurationUtil.setFromArgs(CrawlerStatus.PARAM_RECORD_REFERRERS_MODE,
				  "All",
				  CrawlerStatus.PARAM_RECORD_REFERRER_TYPES,
				  "Included");
    CrawlerStatus cs = new CrawlerStatus(mau, null, "Type 42");
    assertTrue(cs.hasReferrers());
    assertTrue(cs.hasReferrersOfType(ReferrerType.Included));
    assertFalse(cs.hasReferrersOfType(ReferrerType.Excluded));
    assertEmpty(cs.getReferrers(url1));

    cs.signalReferrer(url1, ref1, ReferrerType.Included);
    assertEquals(ListUtil.list(ref1), cs.getReferrers(url1));
    cs.signalReferrer(url1, ref2, ReferrerType.Excluded);
    assertEquals(ListUtil.list(ref1), cs.getReferrers(url1));
    cs.signalReferrer(url1, ref3, ReferrerType.Included);
    assertEquals(ListUtil.list(ref1, ref3), cs.getReferrers(url1));
  }

  public void testReferrersExcluded() {
    ConfigurationUtil.setFromArgs(CrawlerStatus.PARAM_RECORD_REFERRERS_MODE,
				  "All",
				  CrawlerStatus.PARAM_RECORD_REFERRER_TYPES,
				  "Excluded");
    CrawlerStatus cs = new CrawlerStatus(mau, null, "Type 42");
    assertTrue(cs.hasReferrers());
    assertFalse(cs.hasReferrersOfType(ReferrerType.Included));
    assertTrue(cs.hasReferrersOfType(ReferrerType.Excluded));
    assertEmpty(cs.getReferrers(url1));

    cs.signalReferrer(url1, ref1, ReferrerType.Included);
    assertEmpty(cs.getReferrers(url1));
    cs.signalReferrer(url1, ref2, ReferrerType.Excluded);
    cs.signalReferrer(url_off1, ref3, ReferrerType.Excluded);
    assertEquals(ListUtil.list(ref2), cs.getReferrers(url1));
    assertEquals(ListUtil.list(ref3), cs.getReferrers(url_off1));
  }

  public void testReferrersExcludedOnHost() {
    ConfigurationUtil.setFromArgs(CrawlerStatus.PARAM_RECORD_REFERRERS_MODE,
				  "All",
				  CrawlerStatus.PARAM_RECORD_REFERRER_TYPES,
				  "ExcludedOnHost");
    CrawlerStatus cs = new CrawlerStatus(mau, null, "Type 42");
    assertTrue(cs.hasReferrers());
    assertFalse(cs.hasReferrersOfType(ReferrerType.Included));
    assertTrue(cs.hasReferrersOfType(ReferrerType.Excluded));
    assertEmpty(cs.getReferrers(url1));

    cs.signalReferrer(url1, ref1, ReferrerType.Included);
    assertEmpty(cs.getReferrers(url1));
    cs.signalReferrer(url1, ref2, ReferrerType.Excluded);
    cs.signalReferrer(url_off1, ref3, ReferrerType.Excluded);
    assertEquals(ListUtil.list(ref2), cs.getReferrers(url1));
    assertEmpty(cs.getReferrers(url_off1));
  }

  public void testReferrersExcludedOffHost() {
    ConfigurationUtil.setFromArgs(CrawlerStatus.PARAM_RECORD_REFERRERS_MODE,
				  "All",
				  CrawlerStatus.PARAM_RECORD_REFERRER_TYPES,
				  "ExcludedOffHost");
    CrawlerStatus cs = new CrawlerStatus(mau, null, "Type 42");
    assertTrue(cs.hasReferrers());
    assertFalse(cs.hasReferrersOfType(ReferrerType.Included));
    assertTrue(cs.hasReferrersOfType(ReferrerType.Excluded));
    assertEmpty(cs.getReferrers(url1));

    cs.signalReferrer(url1, ref1, ReferrerType.Included);
    assertEmpty(cs.getReferrers(url1));
    cs.signalReferrer(url1, ref2, ReferrerType.Excluded);
    cs.signalReferrer(url_off1, ref3, ReferrerType.Excluded);
    assertEmpty(cs.getReferrers(url1));
    assertEquals(ListUtil.list(ref3), cs.getReferrers(url_off1));
  }


}
