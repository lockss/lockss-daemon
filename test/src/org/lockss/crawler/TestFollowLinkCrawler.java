/*
 * $Id$
 */

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

package org.lockss.crawler;

import java.util.*;
import java.util.stream.*;
import java.io.*;

import org.apache.commons.collections.set.*;
import org.apache.oro.text.regex.*;
import org.lockss.config.*;
import org.lockss.daemon.Crawler.CrawlerFacade;
import org.lockss.daemon.*;
import org.lockss.util.*;
import org.lockss.plugin.*;
import org.lockss.plugin.ArchivalUnit.ConfigurationException;
import org.lockss.test.*;
import org.lockss.test.MockCrawler.MockCrawlerFacade;
import org.lockss.state.*;
import org.lockss.util.urlconn.*;

import static org.lockss.state.SubstanceChecker.State;

public class TestFollowLinkCrawler extends LockssTestCase {

  private MockLockssDaemon theDaemon;
  private CrawlManagerImpl crawlMgr;
  private MockPlugin plug;
  private MyMockArchivalUnit mau = null;
  private MockCachedUrlSet mcus = null;
  private MockAuState aus;
  private static List testUrlList = ListUtil.list("http://example.com");
  private MockCrawlRule crawlRule = null;
  private String startUrl = "http://www.example.com/index.html";
  private List startUrls;
  private TestableFollowLinkCrawler crawler = null;
  private MockLinkExtractor extractor = new MockLinkExtractor();

  private static final String PARAM_RETRY_TIMES =
    BaseCrawler.PARAM_DEFAULT_RETRY_COUNT;
  private static final int DEFAULT_RETRY_TIMES =
    BaseCrawler.DEFAULT_DEFAULT_RETRY_COUNT;;

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
    mau = new MyMockArchivalUnit();
    mau.setPlugin(plug);
    mau.setAuId("MyMockTestAu");
    MockNodeManager nodeManager = new MockNodeManager();
    getMockLockssDaemon().setNodeManager(nodeManager, mau);
    aus = new MockAuState(mau);
    nodeManager.setAuState(aus);
    startUrls = ListUtil.list(startUrl);
    mcus = (MockCachedUrlSet)mau.getAuCachedUrlSet();

    crawlRule = new MockCrawlRule();
    crawlRule.addUrlToCrawl(startUrl);
    mau.setCrawlRule(crawlRule);
    mau.setStartUrls(startUrls);
    mau.setPermissionUrls(startUrls);
    mau.setRefetchDepth(1);
    
    crawlMgr.newCrawlRateLimiter(mau);
    
    crawler = makeTestableCrawler();

    mau.setLinkExtractor("text/html", extractor);
    Properties p = new Properties();
    p.setProperty(FollowLinkCrawler.PARAM_DEFAULT_RETRY_DELAY, "0");
    p.setProperty(FollowLinkCrawler.PARAM_MIN_RETRY_DELAY, "0");
    ConfigurationUtil.setCurrentConfigFromProps(p);
  }

  TestableFollowLinkCrawler makeTestableCrawler() {
    TestableFollowLinkCrawler res = new TestableFollowLinkCrawler(mau, aus);
    res.setDaemonPermissionCheckers(ListUtil.list(new MyMockPermissionChecker(1)));
    return res;
  }

  public void testFlcThrowsForNullAu() {
    try {
      crawler = new TestableFollowLinkCrawler(null, new MockAuState());
      fail("Constructing a FollowLinkCrawler with a null ArchivalUnit"
	   +" should throw an IllegalArgumentException");
    } catch (IllegalArgumentException iae) {
    }
  }

  public void testFlcThrowsForNullAuState() {
    try {
      crawler = new TestableFollowLinkCrawler(mau, null);
      fail("Calling makeTestableFollowLinkCrawler with a null AuState"
	   +" should throw an IllegalArgumentException");
    } catch (IllegalArgumentException iae) {
    }
  }

  public void testReturnsProperType() {
    try {
      crawler.getType();
      fail("Calling getType() ," +
	   " which should not be implemented in FollowLinkCrawler");
    } catch (UnsupportedOperationException usoe) {
    }
  }

  protected String permissionUrl = "http://www.example.com/permission.html";

  public void testCrawlSeedStartUrlsNotInCrawlSpec()
      throws ConfigurationException, PluginException, IOException {

    mau.addUrlToBeCached(startUrl);
    mau.addUrlToBeCached(permissionUrl);

    MyBaseCrawlSeed bcs = new MyBaseCrawlSeed(mau);
    List moreStartUrls = ListUtil.list("http://www.example2.com/index2.html",
				       "http://www.example3.com/index3.html");
    startUrls.addAll(moreStartUrls);
    bcs.setStartUrls(startUrls);
    mau.setCrawlSeed(bcs);

    assertEquals(startUrls, bcs.getStartUrls());
    crawler.enqueueStartUrls();

    CrawlerStatus cs = crawler.getCrawlerStatus();
    assertEquals(Crawler.STATUS_PLUGIN_ERROR, cs.getCrawlStatus());
    assertEquals(MapUtil.map("http://www.example3.com/index3.html",
			     "Start URL from CrawlSeed not within crawl rules",
			     "http://www.example2.com/index2.html",
			     "Start URL from CrawlSeed not within crawl rules"),
		 cs.getUrlsWithErrors());
  }

  public void testCrawlSeedReturnsDuplicates()
      throws ConfigurationException, PluginException, IOException {

    String addlUrl1 = "http://www.example2.com/index2.html";
    String addlUrl2 = "http://www.example3.com/index3.html";
    mau.addUrlToBeCached(startUrl);
    mau.addUrlToBeCached(permissionUrl);
    mau.addUrlToBeCached(addlUrl1);
    mau.addUrlToBeCached(addlUrl2);

    MyBaseCrawlSeed bcs = new MyBaseCrawlSeed(mau);
    List moreStartUrls = ListUtil.list(addlUrl1, addlUrl1, addlUrl2);
    startUrls.addAll(moreStartUrls);
    bcs.setStartUrls(startUrls);
    mau.setCrawlSeed(bcs);

    assertEquals(startUrls, bcs.getStartUrls());
    crawler.enqueueStartUrls();

    CrawlerStatus cs = crawler.getCrawlerStatus();
    assertEquals(Crawler.STATUS_QUEUED, cs.getCrawlStatus());
    assertEmpty(cs.getUrlsWithErrors());
    crawler.getFetchQueue();
    List<String> queuedUrls = crawler.getFetchQueue().asList().stream()
      .map(cData -> cData.getUrl())
      .collect(Collectors.toList());
    assertSameElements(SetUtil.set(startUrl, addlUrl1, addlUrl2),
                       queuedUrls);
  }

  class MyBaseCrawlSeed extends BaseCrawlSeed {
    List<String> startUrls;

    MyBaseCrawlSeed(ArchivalUnit au) {
      super(au);
    }
    @Override
    public Collection<String> doGetStartUrls()
	throws ConfigurationException, PluginException, IOException {
      if (startUrls == null) {
	log.critical("doGetStartUrls: " + super.doGetStartUrls());
	return super.doGetStartUrls();
      }
      log.debug("doGetStartUrls: " + startUrls);
      return startUrls;
    }
    void setStartUrls(List<String> urls) {
      startUrls = urls;
    }
  }

  public void testNoProxy() {
    crawler.setCrawlConfig(ConfigManager.getCurrentConfig());
    mau.addUrl(startUrl);
    crawler.makeUrlFetcher(startUrl);
    MyMockUrlFetcher mmuf = mau.lastMmuf;
    assertEquals(null, mmuf.proxyHost);
  }

  public void testGlobalProxy() {
    Properties p = new Properties();
    p.put(FollowLinkCrawler.PARAM_PROXY_ENABLED, "true");
    p.put(FollowLinkCrawler.PARAM_PROXY_HOST, "pr.wub");
    p.put(FollowLinkCrawler.PARAM_PROXY_PORT, "27");
    ConfigurationUtil.addFromProps(p);

    crawler.setCrawlConfig(ConfigManager.getCurrentConfig());
    mau.addUrl(startUrl);
    crawler.makeUrlFetcher(startUrl);
    MyMockUrlFetcher mmuf = mau.lastMmuf;
    assertEquals("pr.wub", mmuf.proxyHost);
    assertEquals(27, mmuf.proxyPort);
  }

  public void testAuProxyOverride() throws Exception {
    Properties p = new Properties();
    p.put(FollowLinkCrawler.PARAM_PROXY_ENABLED, "true");
    p.put(FollowLinkCrawler.PARAM_PROXY_HOST, "pr.wub");
    p.put(FollowLinkCrawler.PARAM_PROXY_PORT, "27");
    ConfigurationUtil.addFromProps(p);
    Configuration auConfig =
      ConfigurationUtil.fromArgs(ConfigParamDescr.CRAWL_PROXY.getKey(),
				 "proxy.host:8086");
    mau.setConfiguration(auConfig);
    crawler.setCrawlConfig(ConfigManager.getCurrentConfig());
    mau.addUrl(startUrl);
    crawler.makeUrlFetcher(startUrl);
    MyMockUrlFetcher mmuf = mau.lastMmuf;
    assertEquals("proxy.host", mmuf.proxyHost);
    assertEquals(8086, mmuf.proxyPort);
  }

  public void testAuProxyOnly() throws Exception {
    Configuration auConfig =
      ConfigurationUtil.fromArgs(ConfigParamDescr.CRAWL_PROXY.getKey(),
				 "proxy.host:8087");
    mau.setConfiguration(auConfig);
    crawler.setCrawlConfig(ConfigManager.getCurrentConfig());
    mau.addUrl(startUrl);
    crawler.makeUrlFetcher(startUrl);
    MyMockUrlFetcher mmuf = mau.lastMmuf;
    assertEquals("proxy.host", mmuf.proxyHost);
    assertEquals(8087, mmuf.proxyPort);
  }

  public void testAuProxyDisable() throws Exception {
    Properties p = new Properties();
    p.put(FollowLinkCrawler.PARAM_PROXY_ENABLED, "true");
    p.put(FollowLinkCrawler.PARAM_PROXY_HOST, "pr.wub");
    p.put(FollowLinkCrawler.PARAM_PROXY_PORT, "27");
    ConfigurationUtil.addFromProps(p);
    Configuration auConfig =
      ConfigurationUtil.fromArgs(ConfigParamDescr.CRAWL_PROXY.getKey(),
				 "direct");
    mau.setConfiguration(auConfig);
    crawler.setCrawlConfig(ConfigManager.getCurrentConfig());
    mau.addUrl(startUrl);
    crawler.makeUrlFetcher(startUrl);
    MyMockUrlFetcher mmuf = mau.lastMmuf;
    assertEquals(null, mmuf.proxyHost);
  }

  public void testIllAuProxyAbort() throws Exception {
    Properties p = new Properties();
    p.put(FollowLinkCrawler.PARAM_PROXY_ENABLED, "true");
    p.put(FollowLinkCrawler.PARAM_PROXY_HOST, "pr.wub");
    p.put(FollowLinkCrawler.PARAM_PROXY_PORT, "27");
    ConfigurationUtil.addFromProps(p);
    Configuration auConfig =
      ConfigurationUtil.fromArgs(ConfigParamDescr.CRAWL_PROXY.getKey(),
				 "proxy.host:8086:foo");
    mau.setConfiguration(auConfig);

    MockCachedUrlSet cus = (MockCachedUrlSet)mau.getAuCachedUrlSet();
    String url1="http://www.example.com/blah.html";
    mau.addUrl(startUrl, false, true);
    crawler.setUrlsToFollow(ListUtil.list(url1));
    mau.addUrl(url1, false, true);

    assertFalse(crawler.doCrawl());
    assertTrue(crawler.isAborted());
  }

  public void testIllAuProxyContinue() throws Exception {
    Properties p = new Properties();
    p.put(BaseCrawler.PARAM_ABORT_ON_INVALID_PROXY, "false");
    p.put(FollowLinkCrawler.PARAM_PROXY_ENABLED, "true");
    p.put(FollowLinkCrawler.PARAM_PROXY_HOST, "pr.wub");
    p.put(FollowLinkCrawler.PARAM_PROXY_PORT, "27");
    ConfigurationUtil.addFromProps(p);
    Configuration auConfig =
      ConfigurationUtil.fromArgs(ConfigParamDescr.CRAWL_PROXY.getKey(),
				 "proxy.host:8086:foo");
    mau.setConfiguration(auConfig);

    MockCachedUrlSet cus = (MockCachedUrlSet)mau.getAuCachedUrlSet();
    String url1="http://www.example.com/blah.html";
    mau.addUrl(startUrl, false, true);
    crawler.setUrlsToFollow(ListUtil.list(url1));
    mau.addUrl(url1, false, true);

    assertTrue(crawler.doCrawl());
    assertFalse(crawler.isAborted());
  }

  public void testMakeUrlFetcher() {
    mau.addUrl(startUrl);
    crawler.makeUrlFetcher(startUrl);
    MyMockUrlFetcher mmuf = mau.lastMmuf;
    assertNull(mmuf.proxyHost);
  }

  public void testMakeUrlFetcherProxy() {
    Properties p = new Properties();
    p.put(FollowLinkCrawler.PARAM_PROXY_ENABLED, "true");
    p.put(FollowLinkCrawler.PARAM_PROXY_HOST, "pr.wub");
    p.put(FollowLinkCrawler.PARAM_PROXY_PORT, "27");
    ConfigurationUtil.addFromProps(p);

    crawler.setCrawlConfig(ConfigManager.getCurrentConfig());
    mau.addUrl(startUrl);
    crawler.makeUrlFetcher(startUrl);
    MyMockUrlFetcher mmuf = mau.lastMmuf;
    assertEquals("pr.wub", mmuf.proxyHost);
    assertEquals(27, mmuf.proxyPort);
  }

  public void testReturnsTrueWhenCrawlSuccessful() {
    MockCachedUrlSet cus = (MockCachedUrlSet)mau.getAuCachedUrlSet();
    String url1="http://www.example.com/blah.html";
    mau.addUrl(startUrl, false, true);
    crawler.setUrlsToFollow(ListUtil.list(url1));
    mau.addUrl(url1, false, true);

    assertTrue(doCrawl0(crawler));
  }

  //Fetch startUrl, extractor will return a single url that already exists
  //we should only cache startUrl
  public void testDoesNotCacheExistingFile() {
    String url1="http://www.example.com/blah.html";

    crawler.setUrlsToFollow(ListUtil.list(url1));

    MockCachedUrlSet cus = (MockCachedUrlSet)mau.getAuCachedUrlSet();
    mau.addUrl(startUrl, true, true);
    mau.addUrl(url1, true, true);

    assertTrue(crawler.doCrawl());

    Set expected = SetUtil.set(startUrl);
    assertEquals(expected, cus.getCachedUrls());
  }

  public void testRefetchEmptyFileFalse() {
    String url1="http://www.example.com/blah.html";
    String url2="http://www.example.com/halb.html";

    crawler.setUrlsToFollow(ListUtil.list(url1, url2));

    MockCachedUrlSet cus = (MockCachedUrlSet)mau.getAuCachedUrlSet();
    mau.addUrl(startUrl, true, true);
    mau.addUrl(url1, "non-empty");
    mau.addUrl(url2, true, true);

    assertTrue(crawler.doCrawl());
    assertEquals(SetUtil.set(startUrl), cus.getCachedUrls());
  }

  public void testRefetchEmptyFileTrue() {
    ConfigurationUtil.addFromArgs(FollowLinkCrawler.PARAM_REFETCH_EMPTY_FILES,
				  "true");
    String url1="http://www.example.com/blah.html";
    String url2="http://www.example.com/halb.html";

    crawler.setUrlsToFollow(ListUtil.list(url1, url2));

    MockCachedUrlSet cus = (MockCachedUrlSet)mau.getAuCachedUrlSet();
    mau.addUrl(startUrl, true, true);
    mau.addUrl(url1, "non-empty");
    mau.addUrl(url2, true, true);
    
    assertTrue(crawler.doCrawl());
    assertEquals(SetUtil.set(startUrl, url2), cus.getCachedUrls());
    CrawlerStatus cs = crawler.getCrawlerStatus();
  }

  public void testRefetchEmptyFileTruePluginIgnores() {
    ConfigurationUtil.addFromArgs(FollowLinkCrawler.PARAM_REFETCH_EMPTY_FILES,
				  "true");
    HttpResultMap hResultMap = (HttpResultMap)plug.getCacheResultMap();
    hResultMap.storeMapEntry(ContentValidationException.EmptyFile.class,
			     CacheSuccess.class);

    String url1="http://www.example.com/blah.html";
    String url2="http://www.example.com/halb.html";

    crawler.setUrlsToFollow(ListUtil.list(url1, url2));

    MockCachedUrlSet cus = (MockCachedUrlSet)mau.getAuCachedUrlSet();
    mau.addUrl(startUrl, true, true);
    mau.addUrl(url1, "non-empty");
    mau.addUrl(url2, true, true);

    assertTrue(crawler.doCrawl());
    assertEquals(SetUtil.set(startUrl), cus.getCachedUrls());
  }

  public void testHandlesRedirects() {
    String url1="http://www.example.com/blah.html";

    crawler.setUrlsToFollow(ListUtil.list(url1));

    MockCachedUrlSet cus = (MockCachedUrlSet)mau.getAuCachedUrlSet();
    mau.addUrl(startUrl, false, true);
    CIProperties props = new CIProperties();
    props.put(CachedUrl.PROPERTY_CONTENT_TYPE, "text/html");
    props.put(CachedUrl.PROPERTY_CONTENT_URL,
	      "http://www.example.com/extra_level/");
    mau.addUrl(url1, false, true, props);

    assertTrue(crawler.doCrawl());

    Set expectedSrcUrls = SetUtil.set("http://www.example.com/extra_level/", startUrl);
    assertEquals(expectedSrcUrls,extractor.getSrcUrls());
  }

  public void testCdnHost() throws Exception {
    ConfigurationUtil.addFromArgs(CrawlManagerImpl.PARAM_PERMITTED_HOSTS,
				  "foo\\.com");

    String url1="http://www.example.com/one.html";
    String url2="http://foo.com/two";
    String url3="http://www.example.com/three";

    mau.setUrlStems(ListUtil.list("http://www.example.com/"));
    mau.setStartUrls(ListUtil.list(url1));
    crawler = makeTestableCrawler();

    mau.addUrl(startUrl, false, true);
    mau.addUrl(url1);
    mau.addUrl(url2);
    mau.addUrl(url3);
    crawler.setUrlsToFollow(ListUtil.list(url2, url3));
    assertEmpty(aus.getCdnStems());
    assertTrue(crawler.doCrawl());
    // url2 should have been eliminated by the crawl filter
    assertEquals(SetUtil.set(url1, url2, url3), crawler.fetched);
    assertEquals(ListUtil.list(UrlUtil.getUrlPrefix(url2)), aus.getCdnStems());
  }

  public void testRefindCdnHost() throws Exception {
    ConfigurationUtil.addFromArgs(CrawlManagerImpl.PARAM_PERMITTED_HOSTS,
				  "cdn\\.host");
    String url1="http://www.example.com/blah.html";
    String url2="http://cdn.host/halb.html";
    mau.setUrlStems(ListUtil.list("http://www.example.com/"));
    mau.setStartUrls(ListUtil.list(url1));
    crawler = makeTestableCrawler();

    mau.addUrl(startUrl, false, true);
    mau.addUrl(url1);
    mau.addUrl(url2, "foo");		// has content so won't get fetched
    crawler.setUrlsToFollow(ListUtil.list(url2));
    assertEmpty(aus.getCdnStems());
    
    assertTrue(crawler.doCrawl());
    assertEmpty(aus.getCdnStems());
    ConfigurationUtil.addFromArgs(FollowLinkCrawler.PARAM_REFIND_CDN_STEMS,
				  "true");
    crawler = makeTestableCrawler();
    crawler.setUrlsToFollow(ListUtil.list(url2));
    assertTrue(crawler.doCrawl());
    assertEquals(ListUtil.list(UrlUtil.getUrlPrefix(url2)), aus.getCdnStems());
  }

  CIProperties fromArgs(String prop, String val) {
    CIProperties props = new CIProperties();
    props.put(prop, val);
    return props;
  }

  public void testParseCharset() {
    MockLinkExtractor ext2 = new MockLinkExtractor();
    mau.setLinkExtractor("audio/inaudible", ext2);
    String url1="http://www.example.com/one.html";
    String url2="http://www.example.com/two.html";
    String url3="http://www.example.com/three.html";

    mau.setStartUrls(ListUtil.list(url1, url2, url3));

    MockCachedUrlSet cus = (MockCachedUrlSet)mau.getAuCachedUrlSet();
    mau.addUrl(startUrl, false, true);
    mau.addUrl(url1, false, true, fromArgs(CachedUrl.PROPERTY_CONTENT_TYPE,
					   "text/html"));
    mau.addUrl(url2, false, true, fromArgs(CachedUrl.PROPERTY_CONTENT_TYPE,
					   "text/html;charset=utf-8"));
    mau.addUrl(url3, false, true, fromArgs(CachedUrl.PROPERTY_CONTENT_TYPE,
					   "audio/inaudible"));

    assertTrue(crawler.doCrawl());

    Set exp =
      SetUtil.set(ListUtil.list(mau, null, Constants.DEFAULT_ENCODING,
				"http://www.example.com/one.html"),
		  ListUtil.list(mau, null, "utf-8",
				"http://www.example.com/two.html"));
    assertSameElements(exp, extractor.getArgs());

    exp =
      SetUtil.set(ListUtil.list(mau, null, Constants.DEFAULT_ENCODING,
				"http://www.example.com/three.html"));
    assertSameElements(exp, ext2.getArgs());
  }

  public void testCrawlFilter() {

    mau.setLinkExtractor("text/html", new DelimitedLinkExtractor());
    // Set a crawl filter that removes the first blank-delimited word
    mau.setCrawlFilterFactory(new MyFiltFact(1));
    String url1="http://www.example.com/one.html";
    String url2="http://www.example.com/two";
    String url3="http://www.example.com/three";
    String url4="http://www.example.com/four";

    mau.setStartUrls(ListUtil.list(url1));

    MockCachedUrlSet cus = (MockCachedUrlSet)mau.getAuCachedUrlSet();
    mau.addUrl(startUrl, false, true);
    mau.addUrl(url1, StringUtil.separatedString(ListUtil.list(url2, url3, url4),
						" "));
    mau.addUrl(url2, "");
    mau.addUrl(url3, "");
    mau.addUrl(url4, "");
    assertTrue(crawler.doCrawl());
    // url2 should have been eliminated by the crawl filter
    assertEquals(SetUtil.set(url1, url3, url4), crawler.fetched);
  }

  List<Pattern> compileRegexps(List<String> regexps)
      throws MalformedPatternException {
    return RegexpUtil.compileRegexps(regexps);
  }

  String nsurl1="http://www.example.com/one.html";
  String nsurl2="http://www.example.com/two.xml";
  String nsurl3="http://www.example.com/three.xml";
  String nsurl4="http://www.example.com/four.html";
  String nsurl5="http://www.example.com/redir1.html";

  public void testNoSubstance(SubstanceChecker.State expSubState,
			      List<String> substanceUrlRegexps,
			      List<String> nonSubstanceUrlRegexps)
      throws Exception {
    testNoSubstance(expSubState, substanceUrlRegexps,
		    nonSubstanceUrlRegexps, null);
  }

  public void testNoSubstance(SubstanceChecker.State expSubState,
			      List<String> substanceUrlRegexps,
			      List<String> nonSubstanceUrlRegexps,
			      List<String> urlsToFollow
			      )
      throws Exception {

    if (substanceUrlRegexps != null)
      mau.setSubstanceUrlPatterns(compileRegexps(substanceUrlRegexps));
    if (nonSubstanceUrlRegexps != null)
      mau.setNonSubstanceUrlPatterns(compileRegexps(nonSubstanceUrlRegexps));

    if (urlsToFollow == null) {
      urlsToFollow = ListUtil.list(nsurl1, nsurl2, nsurl4);
    }
    crawler.setUrlsToFollow(urlsToFollow);

    MockCachedUrlSet cus = (MockCachedUrlSet)mau.getAuCachedUrlSet();
    mau.addUrl(startUrl, false, true);
//     mau.addUrl(url1, StringUtil.separatedString(ListUtil.list(nsurl2, nsurl3, nsurl4),
// 						" "));
    mau.addUrl(nsurl1, false, true);
    mau.addUrl(nsurl2, false, true);
    CIProperties props = new CIProperties();
    props.put(CachedUrl.PROPERTY_CONTENT_URL, nsurl5);
    mau.addUrl(nsurl3, false, true, props);

    mau.addUrl(nsurl4, true, true);
    mau.populateAuCachedUrlSet();

    assertTrue(crawler.doCrawl());
    AuState aus1 = AuUtil.getAuState(mau);
    SubstanceChecker.State subState = aus1.getSubstanceState();
    assertEquals(expSubState, subState);
  }

  void setSubstanceMode(String mode) {
    ConfigurationUtil.addFromArgs(SubstanceChecker.PARAM_DETECT_NO_SUBSTANCE_MODE,
				  mode);
  }

  public void testNoSubstanceNoPats() throws Exception {
    setSubstanceMode("Crawl");
    testNoSubstance(State.Unknown, null, null);
  }

  public void testNoSubstanceNoUrlsMatchSubstancePatterns() throws Exception {
    setSubstanceMode("Crawl");
    testNoSubstance(State.No, ListUtil.list("important"), null);
  }

  public void testNoSubstanceSomeUrlsMatchSubstancePatterns() throws Exception {
    setSubstanceMode("Crawl");
    testNoSubstance(State.Yes, ListUtil.list("important","two"), null);
  }

  public void testNoSubstanceNoCrawledUrlsMatchSubstancePatterns() throws Exception {
    ConfigurationUtil.addFromArgs(FollowLinkCrawler.PARAM_IS_FULL_SUBSTANCE_CHECK,
				  "false");
    setSubstanceMode("Crawl");
    testNoSubstance(State.No, ListUtil.list("important","redir"), null);
  }

  public void testNoSubstanceFullWhenNoCrawledUrlsMatchSubstancePatterns() throws Exception {
    ConfigurationUtil.addFromArgs(FollowLinkCrawler.PARAM_IS_FULL_SUBSTANCE_CHECK,
				  "true");
    setSubstanceMode("Crawl");
    testNoSubstance(State.Yes, ListUtil.list("important","redir"), null);
  }

  // Same as previous but disabled
  public void testNoSubstanceDisabled() throws Exception {
    setSubstanceMode("None");
    testNoSubstance(State.Unknown, ListUtil.list("important","two"), null);
  }

  public void testNoSubstanceSomeAlreadyCachedUrlsMatchSubstancePatterns()
      throws Exception {
    setSubstanceMode("Crawl");
    testNoSubstance(State.Yes, ListUtil.list("four"), null);
  }

  public void testNoSubstanceRedirUrlMatchesSubstancePatterns() throws Exception {
    setSubstanceMode("Crawl");
    testNoSubstance(State.Yes, ListUtil.list("important","redir"), null,
		    ListUtil.list(nsurl1, nsurl2, nsurl3));
  }

  public void testNoSubstanceSumeUrlsMatchNonSubstancePatterns() throws Exception {
    setSubstanceMode("Crawl");
    testNoSubstance(State.Yes, null, ListUtil.list("important","two"));
  }

  public void testNoSubstanceAllUrlsMatchNonSubstancePatterns() throws Exception {
    setSubstanceMode("Crawl");
    ConfigurationUtil.addFromArgs(FollowLinkCrawler.PARAM_IS_FULL_SUBSTANCE_CHECK,
				  "false");
    testNoSubstance(State.No, null,
		    ListUtil.list("one","two","three", "four"));
  }

  public void testNoSubstanceFullUrlsDontAllMatchNonSubstancePatterns() throws Exception {
    setSubstanceMode("Crawl");
    ConfigurationUtil.addFromArgs(FollowLinkCrawler.PARAM_IS_FULL_SUBSTANCE_CHECK,
				  "true");
    testNoSubstance(State.Yes, null,
		    ListUtil.list("one","two","three", "four"));
  }

  public void testNoSubstanceMostUrlsMatchNonSubstancePatterns() throws Exception {
    setSubstanceMode("Crawl");
    testNoSubstance(State.Yes, null,
		    ListUtil.list("one","tow","three", "four"));
  }

  private static class MyFiltFact implements FilterFactory {
    int skip;

    MyFiltFact(int skip) {
      this.skip = skip;
    }

    public InputStream createFilteredInputStream(ArchivalUnit au,
						 InputStream in,
						 String encoding)
	throws PluginException {
      try {
	String content = StringUtil.fromInputStream(in);
	List lst = StringUtil.breakAt(content, ' ');
	for (int ix = 0; ix < skip && !lst.isEmpty(); ix++) {
	  lst.remove(0);
	}
	String filtered = StringUtil.separatedString(lst, " ");
	log.debug2("before: " + content + ", after: " + filtered);
	return new StringInputStream(filtered);
      } catch (IOException e) {
	throw new RuntimeException(e);
      }
    }
  }

  public void testGetRetryCount() {
    int maxRetries = BaseCrawler.DEFAULT_MAX_RETRY_COUNT;
    CacheException ce;
    CrawlerFacade cf = crawler.getCrawlerFacade();

    ce = new MyMockRetryableCacheException("Test exception",
					   maxRetries + 5,
					   0);
    assertEquals(maxRetries, cf.getRetryCount(ce));

    maxRetries = BaseCrawler.DEFAULT_MAX_RETRY_COUNT + 47;

    ConfigurationUtil.addFromArgs(BaseCrawler.PARAM_MAX_RETRY_COUNT,
				  String.valueOf(maxRetries));
    crawler.setCrawlConfig(ConfigManager.getCurrentConfig());

    ce = new MyMockRetryableCacheException("Test exception",
					   maxRetries - 7,
					   0);
    assertEquals(maxRetries - 7, cf.getRetryCount(ce));

    ce = new MyMockRetryableCacheException("Test exception",
					   maxRetries + 5,
					   0);
    assertEquals(maxRetries, cf.getRetryCount(ce));

    ce = new MyMockRetryableCacheException("Test exception");
    assertEquals(BaseCrawler.DEFAULT_DEFAULT_RETRY_COUNT,
		 cf.getRetryCount(ce));

    ConfigurationUtil.addFromArgs(BaseCrawler.PARAM_DEFAULT_RETRY_COUNT, "7");
    crawler.setCrawlConfig(ConfigManager.getCurrentConfig());

    ce = new MyMockRetryableCacheException("Test exception");
    assertEquals(7, cf.getRetryCount(ce));
  }

  public void testGetRetryDelay() {
    long minRetryDelay = BaseCrawler.DEFAULT_MIN_RETRY_DELAY;
    CacheException ce;
    CrawlerFacade cf = crawler.getCrawlerFacade();
    
    ce = new MyMockRetryableCacheException("Test exception",
					   0,
					   minRetryDelay - 5);
    assertEquals(minRetryDelay, cf.getRetryDelay(ce));

    minRetryDelay = BaseCrawler.DEFAULT_MIN_RETRY_DELAY - 7;
    ConfigurationUtil.addFromArgs(BaseCrawler.PARAM_MIN_RETRY_DELAY,
				  String.valueOf(minRetryDelay));
    crawler.setCrawlConfig(ConfigManager.getCurrentConfig());

    ce = new MyMockRetryableCacheException("Test exception",
					   0,
					   minRetryDelay + 3);
    assertEquals(minRetryDelay + 3, cf.getRetryDelay(ce));

    ce = new MyMockRetryableCacheException("Test exception",
					   0,
					   minRetryDelay - 2);
    assertEquals(minRetryDelay, cf.getRetryDelay(ce));

    ConfigurationUtil.setCurrentConfigFromProps(new Properties());
    crawler.setCrawlConfig(ConfigManager.getCurrentConfig());

    ce = new MyMockRetryableCacheException("Test exception");
    assertEquals(BaseCrawler.DEFAULT_DEFAULT_RETRY_DELAY,
		 cf.getRetryDelay(ce));

    ConfigurationUtil.addFromArgs(BaseCrawler.PARAM_DEFAULT_RETRY_DELAY,
				  "765432");
    crawler.setCrawlConfig(ConfigManager.getCurrentConfig());

    ce = new MyMockRetryableCacheException("Test exception");
    assertEquals(765432, cf.getRetryDelay(ce));
  }

  private static final String CW_URL1 = "http://www.example.com/link1.html";
  private static final String CW_URL2 = "http://www.example.com/link2.html";
  private static final String CW_URL3 = "http://www.example.com/link3.html";

  private void setUpCrawlWindowTest(CrawlWindow myCrawlWindow) {
    mau.setCrawlWindow(myCrawlWindow);
    crawler = makeTestableCrawler();

    mau.addUrl(startUrl);
    
    crawler.setUrlsToFollow(ListUtil.list(CW_URL1, CW_URL2, CW_URL3));
    addUrls(ListUtil.list(CW_URL1, CW_URL2, CW_URL3));

    crawler.setDaemonPermissionCheckers(ListUtil.list(new MyMockPermissionChecker(100)));
    crawler.doCrawl();
  }

  public void testCrawlWindow() {
    setUpCrawlWindowTest(new MyMockCrawlWindow(3));
    MockCachedUrlSet cus = (MockCachedUrlSet)mau.getAuCachedUrlSet();

    // only gets 2 urls because start url 's canCrawl check is
    Set expected = SetUtil.set(startUrl, CW_URL1);
    assertEquals(expected, cus.getCachedUrls());

    CrawlerStatus crawlStatus = crawler.getCrawlerStatus();
    assertEquals(Crawler.STATUS_WINDOW_CLOSED, crawlStatus.getCrawlStatus());
  }

  public void testCrawlWindowFetchNothing() {
    setUpCrawlWindowTest(new MyMockCrawlWindow(0));
    MockCachedUrlSet cus = (MockCachedUrlSet)mau.getAuCachedUrlSet();
    Set expected = new HashSet();
    assertEquals(expected, cus.getCachedUrls());
    CrawlerStatus crawlStatus = crawler.getCrawlerStatus();
    assertEquals(Crawler.STATUS_WINDOW_CLOSED, crawlStatus.getCrawlStatus());
  }

  public void testCrawlWindowFetchOnePermissionPage() {
    setUpCrawlWindowTest(new MyMockCrawlWindow(1));
    MockCachedUrlSet cus = (MockCachedUrlSet)mau.getAuCachedUrlSet();
    Set expected = new HashSet();
    assertEquals(expected, cus.getCachedUrls());
    CrawlerStatus crawlStatus = crawler.getCrawlerStatus();
    assertEquals(Crawler.STATUS_WINDOW_CLOSED, crawlStatus.getCrawlStatus());
  }

  public void testOutsideOfWindowAfterGetUrlsToFollow() {
    MockCachedUrlSet cus = (MockCachedUrlSet)mau.getAuCachedUrlSet();

    String url1= "http://www.example.com/link1.html";
    mau.setCrawlWindow(new MyMockCrawlWindow(0));
    crawler.setUrlsToFollow(ListUtil.list(url1));
    mau.addUrl(startUrl, false, true);
    mau.addUrl(url1);
    assertFalse(crawler.doCrawl());
  }

  public void testAborted1() {
    String url1= "http://www.example.com/link1.html";
    crawler.setUrlsToFollow(ListUtil.list(url1));
    mau.addUrl(startUrl, true, true);
    crawler.abortCrawl();
    assertFalse(crawler.doCrawl());
    assertEmpty(mcus.getCachedUrls());
  }

  public void testAborted2() {
    String url1= "http://www.example.com/link1.html";
    String url2 = "http://www.example.com/link4.html";
    crawler.setUrlsToFollow(ListUtil.list(url1, url2));
    MockCachedUrlSet cus = (MockCachedUrlSet)mau.getAuCachedUrlSet();

    mau.addUrl(startUrl, true, true);
    mau.addUrl(url1);
    mau.addUrl(url2);
    // make it abort after either url1 or url2 cached.  (Can't predict
    // order, so do both.)
    MyMockUrlFetcher mmuf = (MyMockUrlFetcher)
      mau.makeUrlFetcher(crawler.getCrawlerFacade(), url1);
    mmuf.abortCrawl = true;
    mmuf = (MyMockUrlFetcher)
      mau.makeUrlFetcher(crawler.getCrawlerFacade(), url2);
    mmuf.abortCrawl = true;
    assertFalse(crawler.doCrawl());
    // should have cached startUrl and one of the others
    Set cached = cus.getCachedUrls();
    assertEquals(2, cached.size());
    assertTrue(cached.contains(startUrl));
  }

  private Set crawlUrls(List urls) {
    MockCachedUrlSet cus = (MockCachedUrlSet)mau.getAuCachedUrlSet();
    mau.addUrl(startUrl);
    addUrls(urls);
    crawler.doCrawl();
    return cus.getCachedUrls();
  }

  public void testDoesCollectHttps() {
    String url1= "http://www.example.com/link1.html";
    String url2= "https://www.example.com/link2.html";
    String url3= "http://www.example.com/link3.html";

    crawler.setUrlsToFollow(ListUtil.list(url1));
    extractor.addUrlsToReturn(url1, SetUtil.set(url1, url2, url3));
    assertEquals(SetUtil.set(startUrl, url1, url2, url3),
		 crawlUrls(ListUtil.list(url1, url2, url3)));
  }

  public void testDoesCollectFtpAndGopher() {
    //we will collect ftp and gopher eventually, it is not yet implemented
    //though and this test is to make sure ftp gopher urls will not break
    //the whole system

    String url1= "http://www.example.com/link1.html";
    String url2= "ftp://www.example.com/link2.html";
    String url3= "gopher://www.example.com/link3.html";

    crawler.setUrlsToFollow(ListUtil.list(url1));
    extractor.addUrlsToReturn(url1, SetUtil.set(url1, url2, url3));
    assertEquals(SetUtil.set(startUrl, url1),
		 crawlUrls(ListUtil.list(url1, url2, url3)));
    MockCachedUrlSet cus = (MockCachedUrlSet)mau.getAuCachedUrlSet();
  }

  public void testDoesNotLoopOnSelfReferentialPage() {
    String url1= "http://www.example.com/link1.html";
    String url2= "http://www.example.com/link2.html";
    String url3= "http://www.example.com/link3.html";

    crawler.setUrlsToFollow(ListUtil.list(url1));
    extractor.addUrlsToReturn(url1, SetUtil.set(url1, url2, url3));
    assertEquals(SetUtil.set(startUrl, url1, url2, url3),
		 crawlUrls(ListUtil.list(url1, url2, url3)));
  }

  public void testDoesNotLoopOnSelfReferentialLoop() {
    String url1= "http://www.example.com/link1.html";
    String url2= "http://www.example.com/link2.html";
    String url3= "http://www.example.com/link3.html";

    crawler.setUrlsToFollow(ListUtil.list(url1, url2, url3));
    extractor.addUrlsToReturn(url1, SetUtil.set(startUrl));
    assertEquals(SetUtil.set(startUrl, url1, url2, url3),
		 crawlUrls(ListUtil.list(url1, url2, url3)));
  }

//   public void testCrawlListPreservesUncrawledUrls() {
//     setProperty(TestableFollowLinkCrawler.PARAM_PERSIST_CRAWL_LIST, "true");
//     String url1= "http://www.example.com/link1.html";
//     String url2= "http://www.example.com/link2.html";
//     String url3= "http://www.example.com/link3.html";

//     spec.setCrawlWindow(new MyMockCrawlWindow(3)); //permission page & first URL
//     crawler.setUrlsToFollow(ListUtil.list(url1, url2,
//                                                                      url3));
//     crawlUrls(ListUtil.list(url1, url2, url3));
//     // assertEquals(SetUtil.set(url2, url3), aus.getCrawlUrls());
//     // crawlUrls in AuState has to be a HashSet for historical reasons
//     // so we can't predict the order in which the URLs will be crawled
//     // so all we can do here is to check how many there are.
//     assertEquals(2, aus.getCrawlUrls().size());
//   }

//   public void testCrawlListDoesntPreserveUncrawledUrlsIfParam() {
//     setProperty(TestableFollowLinkCrawler.PARAM_PERSIST_CRAWL_LIST, "false");
//     String url1= "http://www.example.com/link1.html";
//     String url2= "http://www.example.com/link2.html";
//     String url3= "http://www.example.com/link3.html";

//     spec.setCrawlWindow(new MyMockCrawlWindow(3));
//     crawler.setUrlsToFollow(ListUtil.list(url1, url2,
//                                                                      url3));
//     crawlUrls(ListUtil.list(url1, url2, url3));
//     assertEquals(SetUtil.set(), aus.getCrawlUrls());
//   }

//   public void testUpdatedCrawlListCalledForEachFetchIfParam() {
//     setProperty(TestableFollowLinkCrawler.PARAM_PERSIST_CRAWL_LIST, "true");
//     String url1= "http://www.example.com/link1.html";
//     String url2= "http://www.example.com/link2.html";
//     String url3= "http://www.example.com/link3.html";

//     crawler.setUrlsToFollow(ListUtil.list(url1, url2, url3));
//     crawlUrls(ListUtil.list(url1, url2, url3));
//     aus.assertUpdatedCrawlListCalled(3); //not called for startUrl
//   }

//   public void testUpdatedCrawlListCalledForEachFetch() {
//     setProperty(TestableFollowLinkCrawler.PARAM_PERSIST_CRAWL_LIST, "false");
//     String url1= "http://www.example.com/link1.html";
//     String url2= "http://www.example.com/link2.html";
//     String url3= "http://www.example.com/link3.html";

//     crawler.setUrlsToFollow(ListUtil.list(url1, url2, url3));
//     crawlUrls(ListUtil.list(url1, url2, url3));
//     aus.assertUpdatedCrawlListCalled(0); //not called for startUrl
//   }
  
  public void testAbbreviatedCrawlTest(int expCrawlerStatus,
				       SubstanceChecker.State expSubState,
				       Collection<String> expFetched,
				       int substanceThreshold,
				       List<String> substanceUrlRegexps,
				       List<String> nonSubstanceUrlRegexps,
				       List<String> urlsToFollow
				       ) throws Exception {
    
    setSubstanceMode("Crawl");
    Configuration auConfig = mau.getConfiguration().copy();
    auConfig.put(ConfigParamDescr.CRAWL_TEST_SUBSTANCE_THRESHOLD.getKey(),
		 ""+substanceThreshold);
    mau.setConfiguration(auConfig);
    if (substanceUrlRegexps != null)
      mau.setSubstanceUrlPatterns(compileRegexps(substanceUrlRegexps));
    if (nonSubstanceUrlRegexps != null)
      mau.setNonSubstanceUrlPatterns(compileRegexps(nonSubstanceUrlRegexps));
    
    mau.setStartUrls(urlsToFollow);
    
    MockCachedUrlSet cus = (MockCachedUrlSet)mau.getAuCachedUrlSet();
    mau.addUrl(startUrl, false, true);
    mau.addUrl(nsurl1, false, true);
    mau.addUrl(nsurl2, false, true);
    mau.addUrl(nsurl3, false, true);
    mau.addUrl(nsurl4, true, true);
    
    assertFalse(crawler.doCrawl());
    AuState aus1 = AuUtil.getAuState(mau);
    SubstanceChecker.State subState = aus1.getSubstanceState();
    CrawlerStatus crawlStatus = crawler.getCrawlerStatus();
    assertEquals(expCrawlerStatus, crawlStatus.getCrawlStatus());
    assertEquals(expSubState, subState);
    assertEquals(expFetched, crawler.fetched);
  }

  public void testCrawlTestPassZero() throws Exception {
    testAbbreviatedCrawlTest(Crawler.STATUS_CRAWL_TEST_SUCCESSFUL,
			     State.Yes,
			     SetUtil.set(nsurl1),
			     0,
			     ListUtil.list("html"),
			     null,
			     ListUtil.list(nsurl1, nsurl2, nsurl3, nsurl4));
  }

  public void testCrawlTestPass1() throws Exception {
    testAbbreviatedCrawlTest(Crawler.STATUS_CRAWL_TEST_SUCCESSFUL,
			     State.Yes,
			     SetUtil.set(nsurl1),
			     1,
			     ListUtil.list("html"),
			     null,
			     ListUtil.list(nsurl1, nsurl2, nsurl3, nsurl4));
  }

  public void testCrawlTestPass2() throws Exception {
    testAbbreviatedCrawlTest(Crawler.STATUS_CRAWL_TEST_SUCCESSFUL,
			     State.Yes,
			     SetUtil.set(nsurl1, nsurl2, nsurl3, nsurl4),
			     2,
			     ListUtil.list("html"),
			     null,
			     ListUtil.list(nsurl1, nsurl2, nsurl3, nsurl4));
  }

  public void testCrawlTestFail() throws Exception {
    testAbbreviatedCrawlTest(Crawler.STATUS_CRAWL_TEST_FAIL,
			     State.Yes,
			     SetUtil.set(nsurl1, nsurl2, nsurl3, nsurl4),
			     3,
			     ListUtil.list("html"),
			     null,
			     ListUtil.list(nsurl1, nsurl2, nsurl3, nsurl4));
  }

  public void testCrawlTestCrawlFail() throws Exception {
    crawler.setDaemonPermissionCheckers(
					ListUtil.list(new MyMockPermissionChecker(0)));
    testAbbreviatedCrawlTest(Crawler.STATUS_NO_PUB_PERMISSION,
			     State.Unknown,
			     Collections.EMPTY_SET,
			     1,
			     ListUtil.list("html"),
			     null,
			     ListUtil.list(nsurl1, nsurl2, nsurl3, nsurl4));
  }

  List<String> queueUrlList(CrawlQueue cq) {
    List<String> res = new ArrayList<String>();
    for (CrawlUrl curl : queueList(cq)) {
      res.add(curl.getUrl());
    }
    return res;
  }

  List<CrawlUrl> queueList(CrawlQueue cq) {
    List<CrawlUrl> res = new ArrayList<CrawlUrl>();
    while (!cq.isEmpty()) {
      res.add(cq.remove());
    }
    return res;
  }

  public void testMyLinkExtractorCallback() {
    final String prefix = "http://www.example.com/"; // pseudo crawl rule
    MockArchivalUnit mau = new MockArchivalUnit() {
	// shouldBeCached() is true of anything starting with prefix
	public boolean shouldBeCached(String url) {
	  return StringUtil.startsWithIgnoreCase(url, prefix);
	}
	// siteNormalizeUrl() removes "SESSION/" from url
	public String siteNormalizeUrl(String url) {
	  return StringUtil.replaceString(url, "SESSION/", "");
	}
      };
    CrawlUrlData curl = new CrawlUrlData("referring.url", 0);
    CrawlQueue cq = new CrawlQueue(null);
    TestableFollowLinkCrawler.MyLinkExtractorCallback mfuc =
      crawler.newFoundUrlCallback(mau, curl, cq, new HashMap(), new HashMap());

    mfuc.foundLink("http://www.example.com/foo.bar");
    mfuc.foundLink("http://www.example.com/SESSION/foo.bar");
    mfuc.foundLink("HTTP://www.example.com/SESSION/foo.bar");
    List<CrawlUrl> queue = queueList(cq);
    CrawlUrlData qcurl = (CrawlUrlData)queue.get(0);
    assertEquals("http://www.example.com/foo.bar", qcurl.getUrl());
    assertEquals("referring.url", qcurl.getReferrer());
    assertEquals(1, qcurl.getDepth());

    // illegal url gets added depending on path traversal action
    mfuc.foundLink("http://www.example.com/foo/../..");
    switch (CurrentConfig.getIntParam(UrlUtil.PARAM_PATH_TRAVERSAL_ACTION,
				      UrlUtil.DEFAULT_PATH_TRAVERSAL_ACTION)) {
    case UrlUtil.PATH_TRAVERSAL_ACTION_ALLOW:
      assertEquals(ListUtil.list("http://www.example.com/../"),
		   queueUrlList(cq));
      break;
    case UrlUtil.PATH_TRAVERSAL_ACTION_REMOVE:
      assertEquals(ListUtil.list("http://www.example.com/"), queueUrlList(cq));
      break;
    case UrlUtil.PATH_TRAVERSAL_ACTION_THROW:
      assertTrue(cq.isEmpty());
      break;
    }
  }

  //test that we don't cache a file that our crawl rules reject
  public void testDoesNotCacheFileWhichShouldNotBeCached() {
    MockCachedUrlSet cus = (MockCachedUrlSet)mau.getAuCachedUrlSet();
    mau.addUrl(startUrl, false, false);

    crawler.doCrawl();
    assertEquals(SetUtil.set(), cus.getCachedUrls());
  }
  
  private void addUrls(List<String> urlList) {
    for (String url : urlList) {
      mau.addUrl(url);
      crawlRule.addUrlToCrawl(url);
    }
  }

  private MockCachedUrlSet permissionPageTestSetup(List<String> permissionPages,
                                                   int passPermissionCheck,
                                                   List<String> urlsToCrawl,
                                                   MockArchivalUnit mmau) {
    //set plugin
    mmau.setPlugin(new MockPlugin(getMockLockssDaemon()));
    mmau.setAuId("permissionPage au");
    MockCachedUrlSet mcus = (MockCachedUrlSet)mmau.getAuCachedUrlSet();
    crawlRule = new MockCrawlRule();


    //set Crawl spec
    mmau.setStartUrls((permissionPages != null
		       ? permissionPages : urlsToCrawl));
    mmau.setPermissionUrls((permissionPages != null
			    ? permissionPages : urlsToCrawl));
    mmau.setCrawlRule(crawlRule);
    mmau.setRefetchDepth(1);

    //set Crawler
    crawler = makeTestableCrawler();

    //set extractor
    mmau.setLinkExtractor("text/html", extractor);

    return mcus;
  }

  public void testMultiPermissionPageShouldPass(){
    String permissionUrl2 = "http://www.foo.com/index.html";
    List<String> permissionList = ListUtil.list(startUrl, permissionUrl2);

    String url1= "http://www.example.com/link1.html";
    String url2= "http://www.example.com/link2.html";
    String url3= "http://www.foo.com/link3.html";
    String url4= "http://www.foo.com/link4.html";
    List<String> urls = ListUtil.list(url1,url2,url3,url4);
    
    MockCachedUrlSet cus = (MockCachedUrlSet)mau.getAuCachedUrlSet();

    addUrls(permissionList);
    addUrls(urls);

    //set Crawl spec
    mau.setStartUrls(permissionList);
    mau.setPermissionUrls(permissionList);
    crawler.setDaemonPermissionCheckers(ListUtil.list(new MyMockPermissionChecker(2)));

    crawler.setUrlsToFollow(urls);
    assertTrue(crawler.doCrawl());
    Set<String> expected = SetUtil.fromList(ListUtil.append(urls, permissionList));
    assertEquals(expected, cus.getCachedUrls());
  }

  public void testMultiStartPageShouldPassPermission(){
    String permissionUrl2 = "http://www.foo.com/index.html";
    List<String> permissionList = ListUtil.list(startUrl,permissionUrl2);

    String url1= "http://www.example.com/link1.html";
    String url2= "http://www.example.com/link2.html";
    String url3= "http://www.foo.com/link3.html";
    String url4= "http://www.foo.com/link4.html";
    List<String> urls = ListUtil.list(url1,url2,url3,url4);
    List<String> allUrls = ListUtil.append(permissionList, urls);
    
    MockCachedUrlSet cus = (MockCachedUrlSet)mau.getAuCachedUrlSet();
    
    addUrls(allUrls);
    mau.setStartUrls(allUrls);
    mau.setPermissionUrls(allUrls);
    crawler.setDaemonPermissionCheckers(ListUtil.list(new MyMockPermissionChecker(2)));

    assertTrue(crawler.doCrawl());
    Set expected = SetUtil.fromList(allUrls);
    assertEquals(expected, cus.getCachedUrls());
  }

  public void testPermissionPageMissing(){
    String permissionUrl1 = "http://www.example.com/index.html";
    List permissionList = ListUtil.list(permissionUrl1);

    String url1 = "http://www.example.com/link1.html";
    String url3 = "http://www.foo.com/link3.html";
    List urls = ListUtil.list(url1,url3);
    
    MockCachedUrlSet cus = (MockCachedUrlSet)mau.getAuCachedUrlSet();
    
    addUrls(urls);
    addUrls(permissionList);

    mau.setStartUrls(startUrls);
    mau.setPermissionUrls(permissionList);
    crawler.setDaemonPermissionCheckers(ListUtil.list(new MyMockPermissionChecker(1)));

    crawler.setUrlsToFollow(urls);
    
    assertFalse(crawler.doCrawl());
    Set expected = SetUtil.set(permissionUrl1, url1);
    assertEquals(expected, cus.getCachedUrls());
  }

  private static void setProperty(String prop, String value) {
    ConfigurationUtil.addFromArgs(prop, value);
  }

  boolean doCrawl0(BaseCrawler crawler) {
    crawler.setCrawlConfig(ConfigManager.getCurrentConfig());
    return crawler.doCrawl0();
  }

  private class MyMockArchivalUnit extends MockArchivalUnit {
    MyMockUrlFetcher lastMmuf;

    protected MockUrlFetcher makeMockUrlFetcher(MockCrawlerFacade mcf, String url) {
      lastMmuf = new MyMockUrlFetcher(mcf, url);
      return lastMmuf;
    }
  }


  private class MyMockCrawlWindow implements CrawlWindow {
    int numTimesToReturnTrue = 0;

    /**
     * Construct a MockCrawlWindow that can set number of time
     * to return true when canCrawl is called.
     *
     * @param numTimesToReturnTrue the number of time to return true
     * excluding fetching of permission pages and starting urls.
     *
     */
    public MyMockCrawlWindow(int numTimesToReturnTrue) {
      this.numTimesToReturnTrue = numTimesToReturnTrue;
    }

    public boolean canCrawl() {
      if (numTimesToReturnTrue > 0) {
	numTimesToReturnTrue--;
	return true;
      }
      return false;
    }

    public boolean canCrawl(Date date) {
      throw new UnsupportedOperationException("not implemented");
    }

  }


  public class MyMockUrlFetcher extends MockUrlFetcher {
    private boolean abortCrawl = false;
    String proxyHost = null;
    int proxyPort;

    public MyMockUrlFetcher(CrawlerFacade cf,String url) {
      super(cf, url);
    }

    public InputStream getUncachedInputStream() throws IOException {
      checkAbort();
      return super.getUncachedInputStream();
    }
    public FetchResult fetch() throws CacheException {
      checkAbort();
      return super.fetch();
    }
    private void checkAbort() {
      if (abortCrawl) {
	crawler.abortCrawl();
      }
    }
    public void setProxy(String proxyHost, int proxyPort) {
      this.proxyHost = proxyHost;
      this.proxyPort = proxyPort;
    }
  }

  private class MyMockRetryableCacheException
    extends CacheException.RetryableException {

    private int retryCount = -1;
    private long retryDelay = -1;

    public MyMockRetryableCacheException(String msg) {
      super(msg);
    }
    public MyMockRetryableCacheException(String msg,
					 int retryCount, long retryDelay) {
      this(msg);
      this.retryCount = retryCount;
      this.retryDelay = retryDelay;
    }
    public int getRetryCount() {
      return retryCount != -1 ? retryCount : super.getRetryCount();
    }
    public long getRetryDelay() {
      return retryDelay != -1 ? retryDelay : super.getRetryDelay();
    }
  }

  private class MyMockUnretryableCacheException
    extends CacheException.UnretryableException {
    public MyMockUnretryableCacheException(String msg) {
      super(msg);
    }
  }

  private class MyMockCacheException
    extends CacheException {
    public MyMockCacheException(String msg) {
      super(msg);
    }
    public void setFailing() {
      attributeBits.set(CacheException.ATTRIBUTE_FAIL);
    }
  }

  private class MyMockPermissionChecker implements PermissionChecker {
    int numPermissionGranted=0;

    MyMockPermissionChecker(int numPermissionGranted) {
      this.numPermissionGranted = numPermissionGranted;
    }

    public void setNumPermissionGranted(int num){
      numPermissionGranted = num;
    }

    /**
     * checkPermission
     *
     * @param reader Reader
     * @return boolean
     */
    public boolean checkPermission(Crawler.CrawlerFacade cFacade,
				   Reader reader, String permissionUrl) {
      return (numPermissionGranted-- > 0);
    }
  }

  static class MyLoginPageChecker implements LoginPageChecker {
    public boolean isLoginPage(Properties props, Reader reader) {
      return false;
    }
  }

  private class TestableFollowLinkCrawler extends FollowLinkCrawler {

    Set<String> urlsToFollow = new HashSet<String>();
    Set<String> nonStartUrlsToFollow = new HashSet<String>();
    Set fetched = new HashSet();
    boolean isFailOnStartUrlError = true;
    List<PermissionChecker> daemonPermissionCheckers;

    protected TestableFollowLinkCrawler(ArchivalUnit au, AuState aus){
      super(au, aus);
      crawlStatus = new CrawlerStatus(au,
				      au.getStartUrls(),
				      null);
      setCrawlManager(TestFollowLinkCrawler.this.crawlMgr);
    }

    MyLinkExtractorCallback
      newFoundUrlCallback(ArchivalUnit au,
			  CrawlUrlData curl,
			  CrawlQueue fetchQueue,
			  Map<String,CrawlUrlData> processedUrls,
			  Map<String,CrawlUrlData> maxDepthUrls) {
      return new MyLinkExtractorCallback(au, curl, fetchQueue,
					 processedUrls, maxDepthUrls);
    }

    private CrawlQueue getFetchQueue() {
      return fetchQueue;
    }

    @Override
    protected boolean shouldFollowLink(){
      //always return true here
      return true;
    }

    /** suppress these actions */
    @Override
    protected void doCrawlEndActions() {
    }

    protected void setUrlsToFollow(List urls) {
      nonStartUrlsToFollow = new ListOrderedSet();
      nonStartUrlsToFollow.addAll(urls);
    }

    @Override
    protected void enqueueStartUrls()
        throws ConfigurationException, PluginException, IOException {
      super.enqueueStartUrls();
      if (nonStartUrlsToFollow != null) {
	for (String url : nonStartUrlsToFollow) {
	  CrawlUrlData curl = newCrawlUrlData(url, 2);
	  addToQueue(curl, fetchQueue, crawlStatus);
	}
      }
    }

    @Override
    public Crawler.Type getType() {
      throw new UnsupportedOperationException("not implemented");
    }

    @Override
    public String getTypeString() {
      return "Follow Link";
    }

    @Override
    public boolean isWholeAU() {
      return false;
    }

    @Override
    protected boolean fetch(CrawlUrlData curl) {
      fetched.add(curl.getUrl());
      return super.fetch(curl);
    }

    void setFailOnStartUrlError(boolean isFailOnStartUrlError) {
      this.isFailOnStartUrlError = isFailOnStartUrlError;
    }

    @Override
    protected boolean isFailOnStartUrlError() {
      return isFailOnStartUrlError;
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

  public static void main(String[] argv) {
    String[] testCaseList = {TestFollowLinkCrawler.class.getName()};
    junit.textui.TestRunner.main(testCaseList);
  }

}

