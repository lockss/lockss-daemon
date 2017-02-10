/*
 * $Id$
 */

/*

Copyright (c) 2000-2015 Board of Trustees of Leland Stanford Jr. University,
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
import java.util.*;
import java.net.*;

import org.lockss.daemon.*;
import org.lockss.test.*;
import org.lockss.plugin.*;
import org.lockss.state.*;
import org.lockss.util.*;
import org.lockss.poller.*;
import org.lockss.config.*;
import org.lockss.crawler.*;
import org.lockss.plugin.ArchivalUnit.*;
import org.lockss.plugin.ArchivalUnit.ConfigurationException;
import org.lockss.plugin.base.BaseArchivalUnit.*;
import org.lockss.extractor.*;
import org.lockss.rewriter.*;

public class TestBaseArchivalUnit extends LockssTestCase {
  private static final String BASE_URL = "http://www.example.com/foo/";
  private static final String START_URL = BASE_URL + "/index.html";
  private static final String AU_NAME = "MockBaseArchivalUnit";


  PluginManager pluginMgr;
  private PollManager pollMgr;
  private TestableBaseArchivalUnit mbau;
  private MyMockPlugin mplug;
  private CrawlRule crawlRule = null;

  public void setUp() throws Exception {
    super.setUp();

    setUpDiskSpace();

    pollMgr = getMockLockssDaemon().getPollManager();
    pluginMgr = getMockLockssDaemon().getPluginManager();

    mbau = makeMbau(AU_NAME, BASE_URL, START_URL);
  }

  public void tearDown() throws Exception {
    TimeBase.setReal();
    super.tearDown();
  }

  static String MY_PLUG_ID = MyMockPlugin.class.getName();

  TestableBaseArchivalUnit makeMbau(String name, String baseUrl, String startUrl)
      throws LockssRegexpException {
    List rules = new LinkedList();
    // exclude anything which doesn't start with our base url
    rules.add(new CrawlRules.RE("^" + baseUrl, CrawlRules.RE.NO_MATCH_EXCLUDE));
    // include the start url
    rules.add(new CrawlRules.RE(startUrl, CrawlRules.RE.MATCH_INCLUDE));
    CrawlRule rule = new CrawlRules.FirstMatch(rules);
    String pkey = PluginManager.pluginKeyFromId(MY_PLUG_ID);
    pluginMgr.ensurePluginLoaded(pkey);
    mplug = (MyMockPlugin)pluginMgr.getPlugin(pkey);

    TestableBaseArchivalUnit au =
      new TestableBaseArchivalUnit(mplug, name, rule, startUrl);
    MockNodeManager nm = new MockNodeManager();
    nm.setAuState(new MockAuState(au));
    getMockLockssDaemon().setNodeManager(nm, au);
    return au;
  }

  public void testIllConst() {
    try {
      new TestableBaseArchivalUnit(new MyNullPlugin());
      fail("new BaseArchivalUnit(non-BasePlugin) should throw");
    } catch (IllegalArgumentException ex) {
    }
  }

  public void testSetConfiguration() throws ConfigurationException {
    // unconfigured  - return null
    assertEquals(null, mbau.getConfiguration());

    // null configuration throws when set
    try {
      mbau.setConfiguration(null);
      fail("null value should throw");
    } catch (ConfigurationException ex) {}

    // cofiguration return by getConfiguration same as one previously set
    Properties props = new Properties();
    props.setProperty(ConfigParamDescr.BASE_URL.getKey(), BASE_URL);
    props.setProperty(ConfigParamDescr.VOLUME_NUMBER.getKey(), "10");
    Configuration exp = ConfigurationUtil.fromProps(props);
    mbau.setConfiguration(exp);
    assertEquals(exp, mbau.getConfiguration());
    assertNotSame(exp, mbau.getConfiguration());
    BaseArchivalUnit.ParamHandlerMap paramMap = mbau.getParamMap();
    assertEquals(BASE_URL,
		 paramMap.getUrl(BaseArchivalUnit.KEY_AU_BASE_URL).toString());
    assertEquals("www.example.com",
		 paramMap.getString(ConfigParamDescr.BASE_URL.getKey() +
				    BaseArchivalUnit.SUFFIX_AU_HOST));
    assertEquals("/foo/",
		 paramMap.getString(ConfigParamDescr.BASE_URL.getKey() +
				    BaseArchivalUnit.SUFFIX_AU_PATH));
  }

  public void testSetConfigurationShortYear(int year, int exp)
      throws ConfigurationException {
    mplug.setAuConfigDescrs(ListUtil.list(ConfigParamDescr.BASE_URL,
					  ConfigParamDescr.YEAR));
    Properties props = new Properties();
    props.setProperty(ConfigParamDescr.BASE_URL.getKey(), BASE_URL);
    props.setProperty(ConfigParamDescr.YEAR.getKey(), Integer.toString(year));
    Configuration config = ConfigurationUtil.fromProps(props);
    mbau = new TestableBaseArchivalUnit(mplug);
    mbau.setConfiguration(config);
    BaseArchivalUnit.ParamHandlerMap paramMap = mbau.getParamMap();
    assertEquals(BASE_URL,
		 paramMap.getUrl(BaseArchivalUnit.KEY_AU_BASE_URL).toString());
    assertEquals("www.example.com",
		 paramMap.getString(ConfigParamDescr.BASE_URL.getKey() +
				    BaseArchivalUnit.SUFFIX_AU_HOST));
    assertEquals(year,
		 paramMap.getInt(ConfigParamDescr.YEAR.getKey()));
    assertEquals(exp,
		 paramMap.getInt(BaseArchivalUnit.PREFIX_AU_SHORT_YEAR +
				    ConfigParamDescr.YEAR.getKey()));
  }

  public void testSetConfigurationShortYear() throws ConfigurationException {
    testSetConfigurationShortYear(1984, 84);
    testSetConfigurationShortYear(1999, 99);
    testSetConfigurationShortYear(2000, 0);
    testSetConfigurationShortYear(2003, 3);
    testSetConfigurationShortYear(2012, 12);
    testSetConfigurationShortYear(2212, 12);
  }

  public void testIncompleteConfiguration() throws ConfigurationException {
    Configuration config = ConfigManager.EMPTY_CONFIGURATION;
    try {
      mbau.setConfiguration(config);
      fail("Empty config should throw");
    } catch (ConfigurationException ex) {}
    try {
      config =
	ConfigurationUtil.fromArgs(ConfigParamDescr.BASE_URL.getKey(),
				   BASE_URL);
      mbau.setConfiguration(config);
      fail("Missing required param (volume) should throw");
    } catch (ConfigurationException ex) {}
    try {
      config =
	ConfigurationUtil.fromArgs(ConfigParamDescr.VOLUME_NUMBER.getKey(),
				   "32");
      mbau.setConfiguration(config);
      fail("Missing required param (base_url) should throw");
    } catch (ConfigurationException ex) {}
  }

  public void testIllConfigChange() throws ConfigurationException {
    Properties props = new Properties();
    props.setProperty(ConfigParamDescr.BASE_URL.getKey(), BASE_URL);
    props.setProperty(ConfigParamDescr.VOLUME_NUMBER.getKey(), "10");
    Configuration exp = ConfigurationUtil.fromProps(props);
    mbau.setConfiguration(exp);
    assertEquals(exp, mbau.getConfiguration());

    props.setProperty(ConfigParamDescr.BASE_URL.getKey(), "not" + BASE_URL);
    try {
      mbau.setConfiguration(ConfigurationUtil.fromProps(props));
      fail("Changing definitional param (base_url) should throw");
    } catch (ConfigurationException ex) {}
    props.setProperty(ConfigParamDescr.BASE_URL.getKey(), BASE_URL);
    mbau.setConfiguration(exp);
    props.setProperty(ConfigParamDescr.VOLUME_NUMBER.getKey(), "11");
    try {
      mbau.setConfiguration(ConfigurationUtil.fromProps(props));
      fail("Changing definitional param (volume) should throw");
    } catch (ConfigurationException ex) {}
    props.setProperty(ConfigParamDescr.VOLUME_NUMBER.getKey(), "10");
    props.setProperty(ConfigParamDescr.PUBLISHER_NAME.getKey(), "PubCo");
    mbau.setConfiguration(exp);
  }

  public void testLoadConfigInt() {
    Properties props = new Properties();
    props.setProperty(ConfigParamDescr.VOLUME_NUMBER.getKey(), "10");
    ConfigParamDescr descr = ConfigParamDescr.VOLUME_NUMBER;
    Configuration config = ConfigurationUtil.fromProps(props);
    int expectedReturn = 10;
    int actualReturn = 0;
    try {
      actualReturn = mbau.loadConfigInt(descr, config);
      assertEquals("return value", expectedReturn, actualReturn);
    }
    catch (ConfigurationException ex1) {
    }

    props.setProperty(ConfigParamDescr.VOLUME_NUMBER.getKey(), "xyz");
    config = ConfigurationUtil.fromProps(props);

    try {
      actualReturn = mbau.loadConfigInt(descr, config);
      assertTrue("invalid value should throw", true);
    }
    catch (ConfigurationException ex) {
    }
  }

  public void testLoadConfigString() throws ArchivalUnit.ConfigurationException {
    Properties props = new Properties();
    props.setProperty(ConfigParamDescr.JOURNAL_DIR.getKey(), "foo");
    ConfigParamDescr descr = ConfigParamDescr.JOURNAL_DIR;
    Configuration config = ConfigurationUtil.fromProps(props);
    String expectedReturn = "foo";
    String actualReturn = mbau.loadConfigString(descr, config);
    assertEquals("return value", expectedReturn, actualReturn);
    // no value assigned should throw
    try  {
      expectedReturn =
          mbau.loadConfigString(ConfigParamDescr.JOURNAL_ABBR, config);
      assertTrue("missing value should throw", true);
    }
    catch(ConfigurationException ex) {

    }
  }

  public void testGetPlugin() {
    Plugin expectedReturn = mplug;
    Plugin actualReturn = mbau.getPlugin();
    assertEquals("return value", expectedReturn, actualReturn);
  }

  public void testGetPluginId() {
    String expectedReturn = mplug.getClass().getName();
    String actualReturn = mbau.getPluginId();
    assertEquals("return value", expectedReturn, actualReturn);
  }

  public void testGetAuId() throws ConfigurationException {
    // cofiguration return by getConfiguration same as one previously set
    Properties props = new Properties();
    props.setProperty(ConfigParamDescr.BASE_URL.getKey(), BASE_URL);
    props.setProperty(ConfigParamDescr.VOLUME_NUMBER.getKey(), "10");
    Configuration config = ConfigurationUtil.fromProps(props);
    mbau.setConfiguration(config);
    String expectedReturn =
        "org|lockss|plugin|base|TestBaseArchivalUnit$MyMockPlugin&base_url~http%3A%2F%2Fwww%2Eexample%2Ecom%2Ffoo%2F&volume~10";
    String actualReturn = mbau.getAuId();
    assertEquals("return value", expectedReturn, actualReturn);
  }

  public void testDefaultFetchDelay() {
    assertEquals(BaseArchivalUnit.DEFAULT_FETCH_DELAY,
		 mbau.findFetchRateLimiter().getInterval());
  }

  public void testMinFetchDelayHigher() throws Exception {
    ConfigurationUtil.setFromArgs(BaseArchivalUnit.PARAM_MIN_FETCH_DELAY,
				  "12004");
    Properties props = new Properties();
    props.setProperty(ConfigParamDescr.BASE_URL.getKey(), BASE_URL);
    props.setProperty(ConfigParamDescr.VOLUME_NUMBER.getKey(), "10");
    Configuration config = ConfigurationUtil.fromProps(props);
    mbau.setConfiguration(config);
    assertEquals(12004, mbau.findFetchRateLimiter().getInterval());
  }

  public void testMinFetchDelayLowerWithPluginDefault() throws Exception {
    ConfigurationUtil.setFromArgs(BaseArchivalUnit.PARAM_MIN_FETCH_DELAY,
				  "17");
    Properties props = new Properties();
    props.setProperty(ConfigParamDescr.BASE_URL.getKey(), BASE_URL);
    props.setProperty(ConfigParamDescr.VOLUME_NUMBER.getKey(), "10");
    Configuration config = ConfigurationUtil.fromProps(props);
    mbau.setConfiguration(config);
    assertEquals(6000, mbau.findFetchRateLimiter().getInterval());
  }

  public void testMinFetchDelayLowerWithPluginLower() throws Exception {
    ConfigurationUtil.setFromArgs(BaseArchivalUnit.PARAM_MIN_FETCH_DELAY,
				  "25");
    Properties props = new Properties();
    props.setProperty(ConfigParamDescr.BASE_URL.getKey(), BASE_URL);
    props.setProperty(ConfigParamDescr.VOLUME_NUMBER.getKey(), "10");
    Configuration config = ConfigurationUtil.fromProps(props);
    mbau.getParamMap().putLong(BaseArchivalUnit.KEY_AU_FETCH_DELAY, 17);
    mbau.setConfiguration(config);
    assertEquals(25, mbau.findFetchRateLimiter().getInterval());
  }

  public void testFindFetchRateLimiterDefault() throws Exception {
    RateLimiter limit = mbau.findFetchRateLimiter();
    assertEquals("1/6000ms", limit.getRate());
    assertSame(limit, mbau.findFetchRateLimiter());
    assertNull(mbau.getFetchRateLimiterKey());
  }

  public void testFindFetchRateLimiterAu() throws Exception {
    Properties props = new Properties();
    props.setProperty(ConfigParamDescr.BASE_URL.getKey(), BASE_URL);
    props.setProperty(ConfigParamDescr.VOLUME_NUMBER.getKey(), "10");
    Configuration config = ConfigurationUtil.fromProps(props);

    mbau.getParamMap().putString(BaseArchivalUnit.KEY_AU_FETCH_RATE_LIMITER_SOURCE,
				 "au");
    mbau.setConfiguration(config);
    RateLimiter limit = mbau.findFetchRateLimiter();
    assertEquals("1/6000ms", limit.getRate());
    assertSame(limit, mbau.findFetchRateLimiter());
    config.put(BaseArchivalUnit.KEY_PAUSE_TIME, "7s");
    mbau.setConfiguration(config);
    assertEquals("1/7000ms", limit.getRate());
    assertSame(limit, mbau.findFetchRateLimiter());
    assertNull(mbau.getFetchRateLimiterKey());
  }

  public void testFindFetchRateLimiterPlugin() throws Exception {
    TestableBaseArchivalUnit mbau2 = new TestableBaseArchivalUnit(mplug);
    TestableBaseArchivalUnit mbau3 =
      new TestableBaseArchivalUnit(new MyMockPlugin());

    mbau.getParamMap().putString(BaseArchivalUnit.KEY_AU_FETCH_RATE_LIMITER_SOURCE,
				 "plugin");
    mbau2.getParamMap().putString(BaseArchivalUnit.KEY_AU_FETCH_RATE_LIMITER_SOURCE,
				  "plugin");
    mbau3.getParamMap().putString(BaseArchivalUnit.KEY_AU_FETCH_RATE_LIMITER_SOURCE,
				  "au");
    RateLimiter limit = mbau.findFetchRateLimiter();
    RateLimiter limit2 = mbau2.findFetchRateLimiter();
    RateLimiter limit3 = mbau3.findFetchRateLimiter();
    assertEquals("1/6000ms", limit.getRate());
    assertSame(limit, limit2);
    assertNotSame(limit, limit3);
    assertEquals(mbau.getPlugin().getPluginId(), mbau.getFetchRateLimiterKey());
  }

  public void testFindFetchRateLimiterDefaultPlugin() throws Exception {
    ConfigurationUtil.setFromArgs(BaseArchivalUnit.PARAM_DEFAULT_FETCH_RATE_LIMITER_SOURCE,
				  "plugin");
    TestableBaseArchivalUnit mbau2 = new TestableBaseArchivalUnit(mplug);
    TestableBaseArchivalUnit mbau3 =
      new TestableBaseArchivalUnit(new MyMockPlugin());

    RateLimiter limit = mbau.findFetchRateLimiter();
    RateLimiter limit2 = mbau2.findFetchRateLimiter();
    RateLimiter limit3 = mbau3.findFetchRateLimiter();
    assertEquals("1/6000ms", limit.getRate());
    assertSame(limit, limit2);
    assertSame(limit, limit3);
    assertEquals(mbau.getPlugin().getPluginId(), mbau.getFetchRateLimiterKey());
  }

  public void testFindFetchRateLimiterDefaultIllegal() throws Exception {
    ConfigurationUtil.setFromArgs(BaseArchivalUnit.PARAM_DEFAULT_FETCH_RATE_LIMITER_SOURCE,
				  "Thurgood Marshall");
    TestableBaseArchivalUnit mbau2 = new TestableBaseArchivalUnit(mplug);

    RateLimiter limit = mbau.findFetchRateLimiter();
    RateLimiter limit2 = mbau2.findFetchRateLimiter();
    assertEquals("1/6000ms", limit.getRate());
    assertNotSame(limit, limit2);
  }

  public void testFindFetchRateLimiterHost() throws Exception {
    TestableBaseArchivalUnit mbau2 = new TestableBaseArchivalUnit(mplug);
    TestableBaseArchivalUnit mbau3 =
      new TestableBaseArchivalUnit(new MyMockPlugin());
    mbau.getParamMap().putString(BaseArchivalUnit.KEY_AU_FETCH_RATE_LIMITER_SOURCE,
				 "host:base_url");
    mbau2.getParamMap().putString(BaseArchivalUnit.KEY_AU_FETCH_RATE_LIMITER_SOURCE,
				  "host:base_url");
    mbau3.getParamMap().putString(BaseArchivalUnit.KEY_AU_FETCH_RATE_LIMITER_SOURCE,
				  "host:base_url");
    Properties props = new Properties();
    props.setProperty(ConfigParamDescr.BASE_URL.getKey(), BASE_URL);
    props.setProperty(ConfigParamDescr.VOLUME_NUMBER.getKey(), "10");
    mbau.setConfiguration(ConfigurationUtil.fromProps(props));
    props.setProperty(ConfigParamDescr.VOLUME_NUMBER.getKey(), "12");
    mbau2.setConfiguration(ConfigurationUtil.fromProps(props));
    props.setProperty(ConfigParamDescr.BASE_URL.getKey(),
		      "http://examplenot.com/bar");
    mbau3.setConfiguration(ConfigurationUtil.fromProps(props));

    RateLimiter limit = mbau.findFetchRateLimiter();
    RateLimiter limit2 = mbau2.findFetchRateLimiter();
    RateLimiter limit3 = mbau3.findFetchRateLimiter();
    assertEquals("1/6000ms", limit.getRate());
    assertSame(limit, limit2);
    assertNotSame(limit, limit3);
    RateLimiter.Pool pool = RateLimiter.getPool();
    assertSame(pool.findNamedRateLimiter("host:www.example.com", 1, 1),
	       limit);
    assertSame(pool.findNamedRateLimiter("host:examplenot.com", 1, 1),
	       limit3);
    assertEquals("host:www.example.com", mbau.getFetchRateLimiterKey());
  }

  public void testFindFetchRateLimiterTitleAttr() throws Exception {
    String src = BaseArchivalUnit.KEY_AU_FETCH_RATE_LIMITER_SOURCE;
    TestableBaseArchivalUnit mbau2 = new TestableBaseArchivalUnit(mplug);
    TestableBaseArchivalUnit mbau3 = new TestableBaseArchivalUnit(mplug);
    TestableBaseArchivalUnit mbau4 = new TestableBaseArchivalUnit(mplug);
    TestableBaseArchivalUnit mbau5 = new TestableBaseArchivalUnit(mplug);
    mbau.getParamMap().putString(src, "title_attribute:server");
    mbau2.getParamMap().putString(src, "title_attribute:server");
    mbau3.getParamMap().putString(src, "title_attribute:client");
    mbau4.getParamMap().putString(src, "title_attribute:server");
    mbau5.getParamMap().putString(src, "title_attribute:server:defaultserver");
    setTCAttrs(mbau, "server", "s1");
    setTCAttrs(mbau2, "server", "s2");
    setTCAttrs(mbau3, "server", "s1").put("client", "s1");
    setTCAttrs(mbau4, "server", "s1");
    // None for mbau5
    Properties props = new Properties();
    props.setProperty(ConfigParamDescr.BASE_URL.getKey(), BASE_URL);
    props.setProperty(ConfigParamDescr.VOLUME_NUMBER.getKey(), "10");
    Configuration config = ConfigurationUtil.fromProps(props);
    mbau.setConfiguration(config);
    mbau2.setConfiguration(config);
    mbau3.setConfiguration(config);
    mbau4.setConfiguration(config);
    mbau5.setConfiguration(config);

    RateLimiter limit = mbau.findFetchRateLimiter();
    RateLimiter limit2 = mbau2.findFetchRateLimiter();
    RateLimiter limit3 = mbau3.findFetchRateLimiter();
    RateLimiter limit4 = mbau4.findFetchRateLimiter();
    RateLimiter limit5 = mbau5.findFetchRateLimiter();
    assertNotSame(limit, limit2);
    assertNotSame(limit, limit3);
    assertSame(limit, limit4);
    assertNotSame(limit, limit5);
    RateLimiter.Pool pool = RateLimiter.getPool();
    assertSame(pool.findNamedRateLimiter("server:s1", 1, 1), limit);
    assertSame(pool.findNamedRateLimiter("server:s2", 1, 1), limit2);
    assertSame(pool.findNamedRateLimiter("client:s1", 1, 1), limit3);
    assertSame(pool.findNamedRateLimiter("server:defaultserver", 1, 1), limit5);
    assertEquals("server:s1", mbau.getFetchRateLimiterKey());
    assertEquals("server:defaultserver", mbau5.getFetchRateLimiterKey());
  }

  public Map setTCAttrs(TestableBaseArchivalUnit mau, String key, String val) {
    TitleConfig tc = new TitleConfig("foo", new MockPlugin());
    Map attrs = new HashMap();
    attrs.put(key, val);
    tc.setAttributes(attrs);
    mau.setTitleConfig(tc);
    return attrs;
  }

  public void testGetName() throws ConfigurationException {
    String expectedReturn = AU_NAME;
    String actualReturn = mbau.getName();
    assertNull(actualReturn);
    Configuration config =
      ConfigurationUtil.fromArgs(ConfigParamDescr.BASE_URL.getKey(), BASE_URL,
				 ConfigParamDescr.VOLUME_NUMBER.getKey(), "10");
    mbau.setConfiguration(config);
    actualReturn = mbau.getName();
    assertEquals("return value", expectedReturn, actualReturn);

  }

  public void testGetParamMap() {
    BaseArchivalUnit.ParamHandlerMap expectedReturn =
        (BaseArchivalUnit.ParamHandlerMap)mbau.paramMap;
    BaseArchivalUnit.ParamHandlerMap actualReturn = mbau.getParamMap();
    assertEquals("return value", expectedReturn, actualReturn);
  }

  public void testGetProperties() {
    TypedEntryMap expectedReturn = mbau.paramMap;
    TypedEntryMap actualReturn = mbau.getProperties();
    assertEquals("return value", expectedReturn, actualReturn);
  }


  public void testLoadConfigUrl() throws ArchivalUnit.ConfigurationException {
    Properties props = new Properties();
    props.setProperty(ConfigParamDescr.BASE_URL.getKey(), BASE_URL);
    ConfigParamDescr descr = ConfigParamDescr.BASE_URL;
    Configuration config = ConfigurationUtil.fromProps(props);
    URL expectedReturn = null;
    try {
      expectedReturn = new URL(BASE_URL);
    }
    catch (MalformedURLException ex) {
    }
    URL actualReturn = mbau.loadConfigUrl(descr, config);
    assertEquals("return value", expectedReturn, actualReturn);
  }

  public void testGetFilterRule() {
    assertNull(mbau.getFilterRule("text/html"));
  }

  public void testGetLinkRewriterFactory() {
    assertTrue(mbau.getLinkRewriterFactory("text/html")
	       instanceof LinkRewriterFactory );
  }

  public void testGetArticleIterator() {
    Iterator<ArticleFiles> articleIterator = mbau.getArticleIterator();
    assertNotNull(articleIterator);
    assertFalse(articleIterator.hasNext());
    MockArticleIteratorFactory maif = new MockArticleIteratorFactory();
    mplug.setArticleIteratorFactory(maif);
    String[] urls = {
	"http://www.example.com/1",
	"http://www.example.com/2",
	"http://www.example.com/3",
	"http://www.example.com/4",
	"http://www.example.com/5",
    };
    int listSize = urls.length;
    ArrayList<ArticleFiles> l = new ArrayList(listSize);
    for (int i = 0; i < listSize; i++) {
      ArticleFiles af = new ArticleFiles();
      af.setFullTextCu(new MockCachedUrl(urls[i]));
      l.add(af);
    }
    maif.setArticleIterator(l.iterator());
    int count = 0;
    for (Iterator<ArticleFiles> it = mbau.getArticleIterator();
	 it.hasNext();
	 count++) {
      CachedUrl cu = it.next().getFullTextCu();
      assertEquals(urls[count], cu.getUrl());
    }
    assertEquals(count,listSize);
  }

  public void testGetCrawlSpec() throws ConfigurationException {
    // we're null until we're configured
    Configuration config =
      ConfigurationUtil.fromArgs(ConfigParamDescr.BASE_URL.getKey(), BASE_URL,
				 ConfigParamDescr.VOLUME_NUMBER.getKey(), "10");
    mbau.setConfiguration(config);
  }

  public void testGetNewContentCrawlUrls() throws ConfigurationException {
    Configuration config =
      ConfigurationUtil.fromArgs(ConfigParamDescr.BASE_URL.getKey(), BASE_URL,
				 ConfigParamDescr.VOLUME_NUMBER.getKey(), "10");
    mbau.setConfiguration(config);
    List expectedReturn = ListUtil.list(START_URL);
    List actualReturn = (List) mbau.getStartUrls();
    assertIsomorphic("return value", expectedReturn, actualReturn);
  }
  public void testGetPermissionPages() throws ConfigurationException {
    Configuration config =
      ConfigurationUtil.fromArgs(ConfigParamDescr.BASE_URL.getKey(), BASE_URL,
				 ConfigParamDescr.VOLUME_NUMBER.getKey(), "10");
    mbau.setConfiguration(config);
    List expectedReturn = ListUtil.list(START_URL);
    List actualReturn = (List) mbau.getPermissionUrls();
    assertIsomorphic("return value", expectedReturn, actualReturn);
  }

  public void testGetUrlStems() throws Exception  {
    // uncofigured base url - return an empty list
    mbau = makeMbau(AU_NAME, BASE_URL, START_URL);

    mbau.setStartUrl(null);
    assertEmpty(mbau.getUrlStems());
    Configuration config =
      ConfigurationUtil.fromArgs(ConfigParamDescr.BASE_URL.getKey(), BASE_URL,
				 ConfigParamDescr.VOLUME_NUMBER.getKey(), "10");
    mbau.setConfiguration(config);
    mbau = makeMbau(AU_NAME, BASE_URL, START_URL);
    mbau.setConfiguration(config);
    assertSameElements(ListUtil.list("http://www.example.com/"),
		       mbau.getUrlStems());

    mbau = makeMbau(AU_NAME, BASE_URL, START_URL);
    mbau.setConfiguration(config);
    mbau.setPermissionPages(ListUtil.list(BASE_URL,
					  "http://foo.other.com:8080/vol20/manifest.html"));
    assertSameElements(ListUtil.list("http://www.example.com/",
				     "http://foo.other.com:8080/"),
		       mbau.getUrlStems());
    mbau = makeMbau(AU_NAME, BASE_URL, START_URL);
    mbau.setConfiguration(config);
    mbau.setPermissionPages(ListUtil.list(BASE_URL,
					  "http://foo.other.com:8080/vol20/manifest.html",
					  "http://foo.other.com:8080/vol21/"));
    assertSameElements(ListUtil.list("http://www.example.com/",
				     "http://foo.other.com:8080/"),
		       mbau.getUrlStems());

    AuState aus = AuUtil.getAuState(mbau);
    // ensure that adding a cdn host causes stems to be recomputed
    aus.addCdnStem("http://cdn.host/");
    Collection stems = mbau.getUrlStems();
    assertSameElements(ListUtil.list("http://www.example.com/",
				     "http://foo.other.com:8080/",
				     "http://cdn.host/"),
		       stems);
    // and that they're not recomputed if cdn hosts haven't changed
    assertSame(stems, mbau.getUrlStems());

    ConfigParamDescr nondefUrl = new ConfigParamDescr();
    nondefUrl.setDefinitional(false)
      .setKey("base_url17")
      .setType(ConfigParamDescr.TYPE_URL);

    // Add another param of type URL, ensure its value gets added to stems
    // even though isn't used in start or permission URLs
    mplug.setAuConfigDescrs(ListUtil.list(ConfigParamDescr.BASE_URL,
					  ConfigParamDescr.VOLUME_NUMBER,
					  nondefUrl));
    config.put(nondefUrl.getKey(), "http://base2.url/");
    mbau.setConfiguration(config);
    assertSameElements(ListUtil.list("http://www.example.com/",
				     "http://foo.other.com:8080/",
				     "http://cdn.host/",
				     "http://base2.url/"),
		       mbau.getUrlStems());
  }



  public void testSiteNormalizeUrl() {
    String url = "http://www.foo.com";
    String actualReturn = mbau.siteNormalizeUrl(url);
    assertEquals("return value", url, actualReturn);
  }

  public void testSetBaseAuParams()
      throws ArchivalUnit.ConfigurationException {
    Properties props = new Properties();
    props.setProperty(ConfigParamDescr.BASE_URL.getKey(), BASE_URL);
    props.setProperty(ConfigParamDescr.VOLUME_NUMBER.getKey(), "10");
    props.setProperty(BaseArchivalUnit.KEY_PAUSE_TIME, "10000");
    props.setProperty(BaseArchivalUnit.KEY_NEW_CONTENT_CRAWL_INTERVAL, "10001");
    Configuration config = ConfigurationUtil.fromProps(props);
    mbau.setConfiguration(config);
    assertEquals(10000, mbau.findFetchRateLimiter().getInterval());
    assertEquals(10001, mbau.newContentCrawlIntv);
    assertEquals(AU_NAME, mbau.getName());
    assertEquals(ListUtil.list(START_URL), mbau.getStartUrls());
    assertTrue(mbau.getCrawlWindow()
	       instanceof MyMockCrawlWindow);
    assertEquals("1/10s", mbau.findFetchRateLimiter().getRate());
  }

  public void testSetBaseAuParamsDefaults()
      throws ArchivalUnit.ConfigurationException {
    Properties props = new Properties();
    props.setProperty(ConfigParamDescr.BASE_URL.getKey(), BASE_URL);
    props.setProperty(ConfigParamDescr.VOLUME_NUMBER.getKey(), "10");
    Configuration config = ConfigurationUtil.fromProps(props);
    mbau.setConfiguration(config);
    assertEquals(BaseArchivalUnit.DEFAULT_FETCH_DELAY, mbau.findFetchRateLimiter().getInterval());
    assertEquals(BaseArchivalUnit.DEFAULT_NEW_CONTENT_CRAWL_INTERVAL,
		 mbau.newContentCrawlIntv);
    assertEquals(ListUtil.list(START_URL), mbau.getStartUrls());
    assertTrue(mbau.getCrawlWindow()
	       instanceof MyMockCrawlWindow);
  }

  // Check that setBaseAuParams() doesn't overwrite values already set in
  // param map with default config values.
  public void testSetBaseAuParamsWithOverride()
      throws ArchivalUnit.ConfigurationException {
    Properties props = new Properties();
    props.setProperty(ConfigParamDescr.BASE_URL.getKey(), BASE_URL);
    props.setProperty(ConfigParamDescr.VOLUME_NUMBER.getKey(), "10");
    props.setProperty(BaseArchivalUnit.KEY_PAUSE_TIME, "10000");
    // per-plugin or per-au crawl window settings are overridden by the
    // o.l.baseau.useCrawlWindowByDefault
    props.setProperty(BaseArchivalUnit.KEY_NEW_CONTENT_CRAWL_INTERVAL, "10000");
    Configuration config = ConfigurationUtil.fromProps(props);
    mbau.doSetAuParams(true);
    mbau.loadAuConfigDescrs(config);
    props.setProperty(BaseArchivalUnit.KEY_PAUSE_TIME, "55555");
    props.setProperty(BaseArchivalUnit.KEY_NEW_CONTENT_CRAWL_INTERVAL, "67890");
    Configuration config2 = ConfigurationUtil.fromProps(props);
    mbau.setConfiguration(config2);
    assertEquals(55555, mbau.findFetchRateLimiter().getInterval());
    assertEquals(67890, mbau.newContentCrawlIntv);
    assertTrue(mbau.getCrawlWindow()
	       instanceof MyMockCrawlWindow);
  }

  public void testShouldCallTopLevelPoll() throws IOException {
    MockAuState state = new MockAuState(mbau, -1, TimeBase.nowMs(), -1, null);
    assertTrue(mbau.shouldCallTopLevelPoll(state));

    // ensure title "nopoll" attr suppresses poll
    assertTrue(mbau.shouldCallTopLevelPoll(state));
    setTCAttrs(mbau, "flags", "nocrawl,nopoll");
    assertFalse(mbau.shouldCallTopLevelPoll(state));
  }

  public void testShouldCrawlForNewContent()
      throws IOException, ArchivalUnit.ConfigurationException {
    TimeBase.setSimulated(100);
    MockAuState state;

    state = new MockAuState(mbau, TimeBase.nowMs(), -1, -1, null);
    assertFalse(mbau.shouldCrawlForNewContent(state));

    TimeBase.step(BaseArchivalUnit.DEFAULT_NEW_CONTENT_CRAWL_INTERVAL + 1);
    assertTrue(mbau.shouldCrawlForNewContent(state));

    // mark publisher down; should then return false
    Configuration conf =
      ConfigurationUtil.fromArgs(ConfigParamDescr.PUB_DOWN.getKey(), "true",
				 ConfigParamDescr.BASE_URL.getKey(), BASE_URL,
				 ConfigParamDescr.VOLUME_NUMBER.getKey(),"42");
    mbau.setConfiguration(conf);
    assertFalse(mbau.shouldCrawlForNewContent(state));
  }

  public void testGetLinkExtractorNullMimeType() {
    assertNull(mbau.getLinkExtractor(null));
  }

  public void testGetLinkExtractorMissingMimeType() {
    assertNull(mbau.getLinkExtractor(""));
  }

  public void testGetLinkExtractorCapitalization() {
    assertTrue(mbau.getLinkExtractor("text/html")
	       instanceof GoslingHtmlLinkExtractor);

    assertTrue(mbau.getLinkExtractor("Text/Html")
	       instanceof GoslingHtmlLinkExtractor);
  }

  public void testGetLinkExtractorSpaces() {
    assertTrue(mbau.getLinkExtractor(" text/html ")
	       instanceof GoslingHtmlLinkExtractor);
  }

  public void testGetLinkExtractorJunkAfterContentType() {
    assertTrue(mbau.getLinkExtractor("text/html ; random content")
	       instanceof GoslingHtmlLinkExtractor);
  }

  public void testGetLinkExtractor_text_html() {
    assertTrue(mbau.getLinkExtractor("text/html")
	       instanceof GoslingHtmlLinkExtractor);
  }

  public void testGetLinkExtractor_text_css() {
    assertTrue(mbau.getLinkExtractor("text/css")
	       instanceof RegexpCssLinkExtractor);
  }

  public void testGetLinkExtractor_application_pdf() {
    assertNull(mbau.getLinkExtractor("application/pdf"));
  }

  TitleConfig makeTitleConfig() {
    ConfigParamDescr d1 = new ConfigParamDescr("base_url");
    ConfigParamDescr d2 = new ConfigParamDescr("volume");
    ConfigParamAssignment a1 = new ConfigParamAssignment(d1, BASE_URL);
    ConfigParamAssignment a2 = new ConfigParamAssignment(d2, "42");
    a1.setEditable(false);
    a2.setEditable(false);
    TitleConfig tc1 = new TitleConfig("a", mplug.getPluginId());
    tc1.setParams(ListUtil.list(a1, a2));
    tc1.setJournalTitle("jt");
    return tc1;
  }

  public void testGetTitleConfig() throws Exception {
    TitleConfig tc = makeTitleConfig();
    Tdb tdb = new Tdb();
    tdb.addTdbAuFromProperties(tc.toProperties());
    ConfigurationUtil.setTdb(tdb);

    Configuration config =
      ConfigurationUtil.fromArgs(ConfigParamDescr.BASE_URL.getKey(), BASE_URL,
				 ConfigParamDescr.VOLUME_NUMBER.getKey(),"42");
    mbau.setConfiguration(config);
    assertEquals(tc, mbau.findTitleConfig());

    TestableBaseArchivalUnit mbau2 =
      makeMbau(AU_NAME+"2", BASE_URL+"2", START_URL+"2");
    Configuration config2 =
      ConfigurationUtil.fromArgs(ConfigParamDescr.BASE_URL.getKey(), BASE_URL+"2",
				 ConfigParamDescr.VOLUME_NUMBER.getKey(),"422");
    mbau2.setConfiguration(config2);
    assertEquals(null, mbau2.findTitleConfig());
  }

  public void testParamHandlerMap() throws ConfigurationException {
    Properties props = new Properties();
    props.setProperty(ConfigParamDescr.BASE_URL.getKey(), BASE_URL);
    props.setProperty(ConfigParamDescr.VOLUME_NUMBER.getKey(), "10");
    props.setProperty(BaseArchivalUnit.KEY_PAUSE_TIME, "10000");
    props.setProperty(BaseArchivalUnit.KEY_NEW_CONTENT_CRAWL_INTERVAL, "10000");

    Configuration config = ConfigurationUtil.fromProps(props);
    mbau.setConfiguration(config);
    // test that the ParamHandlerMap and the properties are the same
    String key = BaseArchivalUnit.KEY_AU_NEW_CONTENT_CRAWL_INTERVAL;
    ParamHandlerMap pmap = mbau.getParamMap();
    TypedEntryMap temap = mbau.getProperties();
    assertEquals( (TypedEntryMap) pmap, temap);
    // check our return before we've added a param handler
    assertEquals(10000, pmap.getLong(key));
    // now add the param handler and check the return
    MyParamHandler handler = new MyParamHandler();
    handler.addParamAndValue(key, new Long(30000));
    pmap.addParamHandler(key, handler);
    assertEquals(30000, pmap.getLong(key));
    // now check remove works
    assertEquals(handler, pmap.removeParamHandler(key));
    assertEquals(10000, pmap.getLong(key));
  }

  public void testMakeCachedUrl() {
    String u1 = "http://www.example.com/1";
    CachedUrl cu = mbau.makeCachedUrl(u1);
    assertEquals(u1, cu.getUrl());
    assertClass(BaseCachedUrl.class, cu);
    BaseCachedUrl bcu = (BaseCachedUrl)cu;
    assertFalse(bcu.isArchiveMember());
  }

  public void testMakeCachedUrlNotInAu() throws Exception {
    Properties props = new Properties();
    props.setProperty(ConfigParamDescr.BASE_URL.getKey(), BASE_URL);
    props.setProperty(ConfigParamDescr.VOLUME_NUMBER.getKey(), "10");
    Configuration exp = ConfigurationUtil.fromProps(props);
    mbau.setConfiguration(exp);
    String url = "http://other.site/non-preserved.html";
    CachedUrl cu = mbau.makeCachedUrl(url);
    assertEquals(url, cu.getUrl());
    assertFalse(mbau.shouldBeCached(url));
  }

  public void testMakeCachedUrlWithMember() {
    mbau.setArchiveFileTypes(ArchiveFileTypes.DEFAULT);
    String u1 = "http://www.example.com/foo.zip!/member/path.ext";
    CachedUrl cu = mbau.makeCachedUrl(u1);
    assertEquals(u1, cu.getUrl());
    assertClass(BaseCachedUrl.Member.class, cu);
    BaseCachedUrl.Member bcu = (BaseCachedUrl.Member)cu;
    assertTrue(bcu.isArchiveMember());
    assertEquals("http://www.example.com/foo.zip", bcu.getArchiveUrl());
    ArchiveMemberSpec ams = bcu.getArchiveMemberSpec();
    assertNotNull(ams);
    assertEquals("member/path.ext", ams.getName());
  }

  public void testMakeCachedUrlWithMemberNoArchives() {
    String u1 = "http://www.example.com/foo.zip!/member/path.ext";
    CachedUrl cu = mbau.makeCachedUrl(u1);
    assertEquals(u1, cu.getUrl());
    assertNotClass(BaseCachedUrl.Member.class, cu);
  }

  public void testMakeUrlCacher() {
    mbau.setAuId("random");
    String u1 = "http://www.example.com/1.zip";
    UrlCacher uc = mbau.makeUrlCacher(new UrlData(null, new CIProperties(), u1));
    assertEquals(u1, uc.getUrl());
    assertClass(DefaultUrlCacher.class, uc);
  }

  public void testMakeUrlCacherWithMemberNoArchives() {
    mbau.setAuId("random");
    String u1 = "http://www.example.com/1.zip!/foo/bar";
    UrlCacher uc = mbau.makeUrlCacher(new UrlData(null, new CIProperties(), u1));
    assertEquals(u1, uc.getUrl());
    assertClass(DefaultUrlCacher.class, uc);
  }

  public void testMakeUrlCacherWithMember() {
    mbau.setArchiveFileTypes(ArchiveFileTypes.DEFAULT);
    mbau.setAuId("random");
    String u1 = "http://www.example.com/1.zip!/foo/bar";
    try {
      mbau.makeUrlCacher(new UrlData(null, null, u1));
      fail("Should not be able to make a UrlCacher for an archive member");
    } catch (IllegalArgumentException e) {
    }
  }

  public static void main(String[] argv) {
    String[] testCaseList = { TestableBaseArchivalUnit.class.getName()};
    junit.swingui.TestRunner.main(testCaseList);
  }

  static class MyParamHandler implements ParamHandler {
    HashMap m_map = new HashMap();

    /**
     * getParamValue
     *
     * @param paramKey String
     * @return Object
     */
    public Object getParamValue(String paramKey) {
      return m_map.get(paramKey);
    }

    public void addParamAndValue(String paramKey, Object value) {
      m_map.put(paramKey, value);
    }

    public void removeParamAndValue(String paramKey) {
      m_map.remove(paramKey);
    }
  }

  static class MyNullPlugin extends NullPlugin.Plugin {
  }

  public static class MyMockPlugin extends MockPlugin {
    TitleConfig tc;
    List titles;

    public TitleConfig getTitleConfig(String title) {
      return tc;
    }

    public List getSupportedTitles() {
      return titles;
    }

    void setTitleConfig(TitleConfig tc) {
      this.tc = tc;
    }
    void setSupportedTitles(List titles) {
      this.titles = titles;
    }

  }
  static class MyMockCrawlWindow implements CrawlWindow {
    /**
     * canCrawl
     *
     * @return boolean
     */
    public boolean canCrawl() {
      return false;
    }

    /**
     * canCrawl
     *
     * @param date Date
     * @return boolean
     */
    public boolean canCrawl(Date date) {
      return false;
    }

  }

  static class TestableBaseArchivalUnit extends BaseArchivalUnit {
    private String m_name = "MockBaseArchivalUnit";
    private CrawlRule m_rules = null;
    private String m_startUrl ="http://www.example.com/index.html";
    private boolean setAuParams = false;
    private List permissionPages = null;
    TitleConfig tc = null;

    private String mimeTypeCalledWith = null;

    TestableBaseArchivalUnit(Plugin plugin, String name, CrawlRule rules,
			   String startUrl) {
      super(plugin);
      m_name = name;
      m_startUrl = startUrl;
      m_rules = rules;
   }

    public TestableBaseArchivalUnit(Plugin myPlugin) {
      super(myPlugin);
    }

    public void setStartUrl(String url) {
      m_startUrl = url;
    }

    public void setName(String name) {
      m_name = name;
    }

    protected String makeName() {
      return m_name;
    }

    public String getMimeTypeCalledWith() {
      return mimeTypeCalledWith;
    }

    protected CrawlRule makeRule() throws ConfigurationException {
      if(m_rules == null) {
        return new MockCrawlRule();
      }
      return m_rules;
    }

    protected String makeStartUrl() {
      return m_startUrl;
    }

    public Collection<String> getPermissionUrls() {
      if (permissionPages != null) {
        return permissionPages;
      }
      return super.getPermissionUrls();
    }

    protected void setPermissionPages(List val) {
      permissionPages = val;
    }

    protected CrawlWindow makeCrawlWindow() {
      return new MyMockCrawlWindow();
    }

    void doSetAuParams(boolean ena) {
      setAuParams = ena;
    }

    protected void loadAuConfigDescrs(Configuration config)
	throws ArchivalUnit.ConfigurationException {
      super.loadAuConfigDescrs(config);
      if (setAuParams) {
	paramMap.putLong(KEY_AU_NEW_CONTENT_CRAWL_INTERVAL, 12345);
	paramMap.putLong(KEY_AU_FETCH_DELAY, 54321);
	paramMap.putBoolean(KEY_AU_USE_CRAWL_WINDOW, false);
      }
    }

    public TitleConfig getTitleConfig() {
      if (tc != null) return tc;
      return super.getTitleConfig();
    }

    public void setTitleConfig(TitleConfig tc) {
      this.tc = tc;
    }

    public void setFetchRateLimiter(RateLimiter limit) {
      fetchRateLimiter = limit;      
    }

    void setAuId(String auid) {
      this.auId = auid;
    }

    ArchiveFileTypes aft = null;

    public ArchiveFileTypes getArchiveFileTypes() {
      return aft;
    }

    public void setArchiveFileTypes(ArchiveFileTypes aft) {
      this.aft = aft;
    }

    @Override
    public List<PermissionChecker> makePermissionCheckers() {
      return null;
    }

    @Override
    public int getRefetchDepth() {
      return 0;
    }

    @Override
    public LoginPageChecker getLoginPageChecker() {
      return null;
    }

    @Override
    public String getCookiePolicy() {
      return null;
    }

    @Override
    public Collection<String> getStartUrls() {
      return ListUtil.list(m_startUrl);
    }
  }
}
