/*
 * $Id: TestBaseArchivalUnit.java,v 1.34 2006-09-16 22:55:22 tlipkis Exp $
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

package org.lockss.plugin.base;

import java.io.*;
import java.util.*;
import org.lockss.daemon.*;
import org.lockss.test.*;
import org.lockss.plugin.*;
import org.lockss.util.*;
import org.lockss.config.*;
import org.lockss.crawler.*;
import org.lockss.plugin.ArchivalUnit.*;
import java.net.*;
import org.lockss.plugin.ArchivalUnit.ConfigurationException;
import org.lockss.plugin.base.BaseArchivalUnit.*;

public class TestBaseArchivalUnit extends LockssTestCase {
  private MyMockBaseArchivalUnit mbau;
  private MyMockPlugin mplug;
  private String baseUrl = "http://www.example.com/foo/";
  private String startUrl = baseUrl + "/index.html";
  private String auName = "MockBaseArchivalUnit";
  private CrawlRule crawlRule = null;

  public void setUp() throws Exception {
    super.setUp();
    Properties props = new Properties();
    props.setProperty(BaseArchivalUnit.PARAM_TOPLEVEL_POLL_INTERVAL_MIN, "5s");
    props.setProperty(BaseArchivalUnit.PARAM_TOPLEVEL_POLL_INTERVAL_MAX, "10s");
    props.setProperty(BaseArchivalUnit.PARAM_TOPLEVEL_POLL_PROB_INITIAL, "50");
    props.setProperty(BaseArchivalUnit.PARAM_TOPLEVEL_POLL_PROB_INCREMENT, "5");
    props.setProperty(BaseArchivalUnit.PARAM_TOPLEVEL_POLL_PROB_MAX, "85");
    ConfigurationUtil.setCurrentConfigFromProps(props);

    List rules = new LinkedList();
    // exclude anything which doesn't start with our base url
    rules.add(new CrawlRules.RE("^" + baseUrl, CrawlRules.RE.NO_MATCH_EXCLUDE));
    // include the start url
    rules.add(new CrawlRules.RE(startUrl, CrawlRules.RE.MATCH_INCLUDE));
    CrawlRule rule = new CrawlRules.FirstMatch(rules);
    mplug = new MyMockPlugin();
    mbau =  new MyMockBaseArchivalUnit(mplug, auName, rule, startUrl);
  }

  public void tearDown() throws Exception {
    TimeBase.setReal();
    super.tearDown();
  }


  public void testIllConst() {
    try {
      new MyMockBaseArchivalUnit(new MyNullPlugin());
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
    props.setProperty(ConfigParamDescr.BASE_URL.getKey(), baseUrl);
    props.setProperty(ConfigParamDescr.VOLUME_NUMBER.getKey(), "10");
    Configuration exp = ConfigurationUtil.fromProps(props);
    mbau.setConfiguration(exp);
    assertEquals(exp, mbau.getConfiguration());
    BaseArchivalUnit.ParamHandlerMap paramMap = mbau.getParamMap();
    assertEquals(baseUrl,
		 paramMap.getUrl(BaseArchivalUnit.AU_BASE_URL).toString());
    assertEquals("www.example.com",
		 paramMap.getString(ConfigParamDescr.BASE_URL.getKey() +
				    BaseArchivalUnit.AU_HOST_SUFFIX));
    assertEquals("/foo/",
		 paramMap.getString(ConfigParamDescr.BASE_URL.getKey() +
				    BaseArchivalUnit.AU_PATH_SUFFIX));
  }

  public void testSetConfigurationShortYear(int year, int exp)
      throws ConfigurationException {
    mplug.setAuConfigDescrs(ListUtil.list(ConfigParamDescr.BASE_URL,
					  ConfigParamDescr.YEAR));
    Properties props = new Properties();
    props.setProperty(ConfigParamDescr.BASE_URL.getKey(), baseUrl);
    props.setProperty(ConfigParamDescr.YEAR.getKey(), Integer.toString(year));
    Configuration config = ConfigurationUtil.fromProps(props);
    mbau = new MyMockBaseArchivalUnit(mplug);
    mbau.setConfiguration(config);
    BaseArchivalUnit.ParamHandlerMap paramMap = mbau.getParamMap();
    assertEquals(baseUrl,
		 paramMap.getUrl(BaseArchivalUnit.AU_BASE_URL).toString());
    assertEquals("www.example.com",
		 paramMap.getString(ConfigParamDescr.BASE_URL.getKey() +
				    BaseArchivalUnit.AU_HOST_SUFFIX));
    assertEquals(year,
		 paramMap.getInt(ConfigParamDescr.YEAR.getKey()));
    assertEquals(exp,
		 paramMap.getInt(BaseArchivalUnit.AU_SHORT_YEAR_PREFIX +
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
				   baseUrl);
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
    props.setProperty(ConfigParamDescr.BASE_URL.getKey(), baseUrl);
    props.setProperty(ConfigParamDescr.VOLUME_NUMBER.getKey(), "10");
    Configuration exp = ConfigurationUtil.fromProps(props);
    mbau.setConfiguration(exp);
    assertEquals(exp, mbau.getConfiguration());

    props.setProperty(ConfigParamDescr.BASE_URL.getKey(), "not" + baseUrl);
    try {
      mbau.setConfiguration(ConfigurationUtil.fromProps(props));
      fail("Changing definitional param (base_url) should throw");
    } catch (ConfigurationException ex) {}
    props.setProperty(ConfigParamDescr.BASE_URL.getKey(), baseUrl);
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
    props.setProperty(ConfigParamDescr.BASE_URL.getKey(), baseUrl);
    props.setProperty(ConfigParamDescr.VOLUME_NUMBER.getKey(), "10");
    Configuration config = ConfigurationUtil.fromProps(props);
    mbau.setConfiguration(config);
    String expectedReturn =
        "org|lockss|plugin|base|TestBaseArchivalUnit$MyMockPlugin&base_url~http%3A%2F%2Fwww%2Eexample%2Ecom%2Ffoo%2F&volume~10";
    String actualReturn = mbau.getAuId();
    assertEquals("return value", expectedReturn, actualReturn);
  }

  public void testGetFetchDelay() {
    long expectedReturn =
        BaseArchivalUnit.DEFAULT_FETCH_DELAY;
    long actualReturn = mbau.findFetchRateLimiter().getInterval();
    assertEquals("return value", expectedReturn, actualReturn);
  }

  public void testFindFetchRateLimiterDefault() throws Exception {
    RateLimiter limit = mbau.findFetchRateLimiter();
    assertEquals("1/6000ms", limit.getRate());
    assertSame(limit, mbau.findFetchRateLimiter());
  }

  public void testFindFetchRateLimiterAu() throws Exception {
    Properties props = new Properties();
    props.setProperty(ConfigParamDescr.BASE_URL.getKey(), baseUrl);
    props.setProperty(ConfigParamDescr.VOLUME_NUMBER.getKey(), "10");
    Configuration config = ConfigurationUtil.fromProps(props);

    mbau.getParamMap().putString(BaseArchivalUnit.AU_FETCH_RATE_LIMITER_SOURCE,
				 "au");
    mbau.setConfiguration(config);
    RateLimiter limit = mbau.findFetchRateLimiter();
    assertEquals("1/6000ms", limit.getRate());
    assertSame(limit, mbau.findFetchRateLimiter());
    config.put(BaseArchivalUnit.PAUSE_TIME_KEY, "7s");
    mbau.setConfiguration(config);
    assertEquals("1/7000ms", limit.getRate());
    assertSame(limit, mbau.findFetchRateLimiter());
  }

  public void testFindFetchRateLimiterPlugin() throws Exception {
    MyMockBaseArchivalUnit mbau2 = new MyMockBaseArchivalUnit(mplug);
    MyMockBaseArchivalUnit mbau3 =
      new MyMockBaseArchivalUnit(new MyMockPlugin());

    mbau.getParamMap().putString(BaseArchivalUnit.AU_FETCH_RATE_LIMITER_SOURCE,
				 "plugin");
    mbau2.getParamMap().putString(BaseArchivalUnit.AU_FETCH_RATE_LIMITER_SOURCE,
				  "plugin");
    mbau3.getParamMap().putString(BaseArchivalUnit.AU_FETCH_RATE_LIMITER_SOURCE,
				  "plugin");
    RateLimiter limit = mbau.findFetchRateLimiter();
    RateLimiter limit2 = mbau2.findFetchRateLimiter();
    RateLimiter limit3 = mbau3.findFetchRateLimiter();
    assertEquals("1/6000ms", limit.getRate());
    assertSame(limit, limit2);
    assertNotSame(limit, limit3);
  }

  public void testFindFetchRateLimiterDefaultPlugin() throws Exception {
    ConfigurationUtil.setFromArgs(BaseArchivalUnit.PARAM_DEFAULT_FETCH_RATE_LIMITER_SOURCE,
				  "plugin");
    MyMockBaseArchivalUnit mbau2 = new MyMockBaseArchivalUnit(mplug);
    MyMockBaseArchivalUnit mbau3 =
      new MyMockBaseArchivalUnit(new MyMockPlugin());

    RateLimiter limit = mbau.findFetchRateLimiter();
    RateLimiter limit2 = mbau2.findFetchRateLimiter();
    RateLimiter limit3 = mbau3.findFetchRateLimiter();
    assertEquals("1/6000ms", limit.getRate());
    assertSame(limit, limit2);
    assertNotSame(limit, limit3);
  }

  public void testFindFetchRateLimiterDefaultIllegal() throws Exception {
    ConfigurationUtil.setFromArgs(BaseArchivalUnit.PARAM_DEFAULT_FETCH_RATE_LIMITER_SOURCE,
				  "Thurgood Marshall");
    MyMockBaseArchivalUnit mbau2 = new MyMockBaseArchivalUnit(mplug);

    RateLimiter limit = mbau.findFetchRateLimiter();
    RateLimiter limit2 = mbau2.findFetchRateLimiter();
    assertEquals("1/6000ms", limit.getRate());
    assertNotSame(limit, limit2);
  }

  public void testFindFetchRateLimiterHost() throws Exception {
    MyMockBaseArchivalUnit mbau2 = new MyMockBaseArchivalUnit(mplug);
    MyMockBaseArchivalUnit mbau3 =
      new MyMockBaseArchivalUnit(new MyMockPlugin());
    mbau.getParamMap().putString(BaseArchivalUnit.AU_FETCH_RATE_LIMITER_SOURCE,
				 "host:base_url");
    mbau2.getParamMap().putString(BaseArchivalUnit.AU_FETCH_RATE_LIMITER_SOURCE,
				  "host:base_url");
    mbau3.getParamMap().putString(BaseArchivalUnit.AU_FETCH_RATE_LIMITER_SOURCE,
				  "host:base_url");
    Properties props = new Properties();
    props.setProperty(ConfigParamDescr.BASE_URL.getKey(), baseUrl);
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
  }

  public void testFindFetchRateLimiterTitleAttr() throws Exception {
    String src = BaseArchivalUnit.AU_FETCH_RATE_LIMITER_SOURCE;
    MyMockBaseArchivalUnit mbau2 = new MyMockBaseArchivalUnit(mplug);
    MyMockBaseArchivalUnit mbau3 = new MyMockBaseArchivalUnit(mplug);
    MyMockBaseArchivalUnit mbau4 = new MyMockBaseArchivalUnit(mplug);
    mbau.getParamMap().putString(src, "title_attribute:server");
    mbau2.getParamMap().putString(src, "title_attribute:server");
    mbau3.getParamMap().putString(src, "title_attribute:client");
    mbau4.getParamMap().putString(src, "title_attribute:server");
    setTCAttrs(mbau, "server", "s1");
    setTCAttrs(mbau2, "server", "s2");
    setTCAttrs(mbau3, "server", "s1").put("client", "s1");
    setTCAttrs(mbau4, "server", "s1");
    Properties props = new Properties();
    props.setProperty(ConfigParamDescr.BASE_URL.getKey(), baseUrl);
    props.setProperty(ConfigParamDescr.VOLUME_NUMBER.getKey(), "10");
    Configuration config = ConfigurationUtil.fromProps(props);
    mbau.setConfiguration(config);
    mbau2.setConfiguration(config);
    mbau3.setConfiguration(config);
    mbau4.setConfiguration(config);

    RateLimiter limit = mbau.findFetchRateLimiter();
    RateLimiter limit2 = mbau2.findFetchRateLimiter();
    RateLimiter limit3 = mbau3.findFetchRateLimiter();
    RateLimiter limit4 = mbau4.findFetchRateLimiter();
    assertNotSame(limit, limit2);
    assertNotSame(limit, limit3);
    assertSame(limit, limit4);
    RateLimiter.Pool pool = RateLimiter.getPool();
    assertSame(pool.findNamedRateLimiter("server:s1", 1, 1),
	       limit);
    assertSame(pool.findNamedRateLimiter("server:s2", 1, 1),
	       limit2);
    assertSame(pool.findNamedRateLimiter("client:s1", 1, 1),
	       limit3);
  }

  public Map setTCAttrs(MyMockBaseArchivalUnit mau, String key, String val) {
    TitleConfig tc = new TitleConfig("foo", new MockPlugin());
    Map attrs = new HashMap();
    attrs.put(key, val);
    tc.setAttributes(attrs);
    mau.setTitleConfig(tc);
    return attrs;
  }

  public void testGetName() throws ConfigurationException {
    String expectedReturn = auName;
    String actualReturn = mbau.getName();
    assertNull(actualReturn);
    Properties props = new Properties();
    props.setProperty(ConfigParamDescr.BASE_URL.getKey(), baseUrl);
    ConfigParamDescr descr = ConfigParamDescr.BASE_URL;
    Configuration config = ConfigurationUtil.fromProps(props);
    mbau.setBaseAuParams(config);
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
    props.setProperty(ConfigParamDescr.BASE_URL.getKey(), baseUrl);
    ConfigParamDescr descr = ConfigParamDescr.BASE_URL;
    Configuration config = ConfigurationUtil.fromProps(props);
    URL expectedReturn = null;
    try {
      expectedReturn = new URL(baseUrl);
    }
    catch (MalformedURLException ex) {
    }
    URL actualReturn = mbau.loadConfigUrl(descr, config);
    assertEquals("return value", expectedReturn, actualReturn);
  }

  public void testConstructFilterRule() {
    assertNull(mbau.getFilterRule("text/html"));
    assertEquals(1, mbau.ruleCacheMiss);
  }

  public void testGetFilterRule() {
    assertNull(mbau.getFilterRule("text/html"));
  }

  public void testConstructFilterFactory() {
    assertNull(mbau.getFilterFactory("text/html"));
    assertEquals(1, mbau.factoryCacheMiss);
  }

  public void testGetFilterFactory() {
    assertNull(mbau.getFilterFactory("text/html"));
  }

  public void testGetCrawlSpec() throws ConfigurationException {
    // we're null until we're configured
    assertNull(mbau.getCrawlSpec());
    Properties props = new Properties();
    props.setProperty(ConfigParamDescr.BASE_URL.getKey(), baseUrl);
    Configuration config = ConfigurationUtil.fromProps(props);
    mbau.setBaseAuParams(config);
    assertNotNull(mbau.getCrawlSpec());
  }

  public void testGetNewContentCrawlUrls() throws ConfigurationException {
    Properties props = new Properties();
    props.setProperty(ConfigParamDescr.BASE_URL.getKey(), baseUrl);
    Configuration config = ConfigurationUtil.fromProps(props);
    mbau.setBaseAuParams(config);
    List expectedReturn = ListUtil.list(startUrl);
    List actualReturn = mbau.getNewContentCrawlUrls();
    assertIsomorphic("return value", expectedReturn, actualReturn);
  }
  public void testGetPermissionPages() throws ConfigurationException {
    Properties props = new Properties();
    props.setProperty(ConfigParamDescr.BASE_URL.getKey(), baseUrl);
    Configuration config = ConfigurationUtil.fromProps(props);
    mbau.setBaseAuParams(config);
    List expectedReturn = ListUtil.list(startUrl);
    List actualReturn = mbau.getPermissionPages();
    assertIsomorphic("return value", expectedReturn, actualReturn);
  }

  public void testGetUrlStems() throws ConfigurationException  {
    // uncofigured base url - return an empty list
    assertIsomorphic(Collections.EMPTY_LIST, mbau.getUrlStems());
    Properties props = new Properties();
    props.setProperty(ConfigParamDescr.BASE_URL.getKey(), baseUrl);
    Configuration config = ConfigurationUtil.fromProps(props);
    mbau.setBaseAuParams(config);
    Collection expectedReturn = ListUtil.list("http://www.example.com");
    Collection actualReturn = mbau.getUrlStems();
    assertIsomorphic("return value", expectedReturn, actualReturn);
  }

  public void testSiteNormalizeUrl() {
    String url = "http://www.foo.com";
    String actualReturn = mbau.siteNormalizeUrl(url);
    assertEquals("return value", url, actualReturn);
  }

  public void testSetBaseAuParams()
      throws ArchivalUnit.ConfigurationException {
    Properties props = new Properties();
    props.setProperty(ConfigParamDescr.BASE_URL.getKey(), baseUrl);
    props.setProperty(ConfigParamDescr.VOLUME_NUMBER.getKey(), "10");
    props.setProperty(BaseArchivalUnit.PAUSE_TIME_KEY, "10000");
    props.setProperty(BaseArchivalUnit.USE_CRAWL_WINDOW, "true");
    props.setProperty(BaseArchivalUnit.NEW_CONTENT_CRAWL_KEY, "10000");
    Configuration config = ConfigurationUtil.fromProps(props);
    mbau.setBaseAuParams(config);
    assertEquals(10000, mbau.findFetchRateLimiter().getInterval());
    assertEquals(10000, mbau.newContentCrawlIntv);
    assertEquals(auName,mbau.getName());
    assertEquals(ListUtil.list(startUrl), mbau.getNewContentCrawlUrls());
    assertTrue(mbau.getCrawlSpec().getCrawlWindow()
	       instanceof MyMockCrawlWindow);
    assertEquals("1/10s", mbau.findFetchRateLimiter().getRate());
  }

  public void testSetBaseAuParamsDefaults()
      throws ArchivalUnit.ConfigurationException {
    Properties props = new Properties();
    props.setProperty(ConfigParamDescr.BASE_URL.getKey(), baseUrl);
    props.setProperty(ConfigParamDescr.VOLUME_NUMBER.getKey(), "10");
    Configuration config = ConfigurationUtil.fromProps(props);
    mbau.setBaseAuParams(config);
    assertEquals(BaseArchivalUnit.DEFAULT_FETCH_DELAY, mbau.findFetchRateLimiter().getInterval());
    assertEquals(BaseArchivalUnit.DEFAULT_NEW_CONTENT_CRAWL_INTERVAL,
		 mbau.newContentCrawlIntv);
    assertEquals(ListUtil.list(startUrl), mbau.getNewContentCrawlUrls());
    assertTrue(mbau.getCrawlSpec().getCrawlWindow()
	       instanceof MyMockCrawlWindow);
  }

  // Check that setBaseAuParams() doesn't overwrite values already set in
  // param map with default config values.
  public void testSetBaseAuParamsWithOverride()
      throws ArchivalUnit.ConfigurationException {
    Properties props = new Properties();
    props.setProperty(ConfigParamDescr.BASE_URL.getKey(), baseUrl);
    props.setProperty(ConfigParamDescr.VOLUME_NUMBER.getKey(), "10");
    props.setProperty(BaseArchivalUnit.PAUSE_TIME_KEY, "10000");
    // per-plugin or per-au crawl window settings are overridden by the
    // o.l.baseau.useCrawlWindowByDefault
    props.setProperty(BaseArchivalUnit.USE_CRAWL_WINDOW, "false");
    props.setProperty(BaseArchivalUnit.NEW_CONTENT_CRAWL_KEY, "10000");
    Configuration config = ConfigurationUtil.fromProps(props);
    mbau.doSetAuParams(true);
    mbau.loadAuConfigDescrs(config);
    props.setProperty(BaseArchivalUnit.PAUSE_TIME_KEY, "55555");
    props.setProperty(BaseArchivalUnit.USE_CRAWL_WINDOW, "false");
    props.setProperty(BaseArchivalUnit.NEW_CONTENT_CRAWL_KEY, "67890");
    Configuration config2 = ConfigurationUtil.fromProps(props);
    mbau.setBaseAuParams(config2);
    assertEquals(55555, mbau.findFetchRateLimiter().getInterval());
    assertEquals(67890, mbau.newContentCrawlIntv);
    assertTrue(mbau.getCrawlSpec().getCrawlWindow()
	       instanceof MyMockCrawlWindow);
//     assertNull(mbau.getCrawlSpec().getCrawlWindow());
  }

  public void testCheckNextPollInterval() {
    TimeBase.setSimulated();
    for (int ii=0; ii<10; ii++) {
      mbau.nextPollInterval = -1;
      mbau.checkNextPollInterval();
      assertTrue(mbau.nextPollInterval >= 5000);
      assertTrue(mbau.nextPollInterval <= 10000);
    }

    Properties props = new Properties();
    props.setProperty(BaseArchivalUnit.PARAM_TOPLEVEL_POLL_INTERVAL_MIN, "1s");
    props.setProperty(BaseArchivalUnit.PARAM_TOPLEVEL_POLL_INTERVAL_MAX, "2s");
    ConfigurationUtil.setCurrentConfigFromProps(props);

    mbau.checkNextPollInterval();
    assertTrue(mbau.nextPollInterval >= 1000);
    assertTrue(mbau.nextPollInterval <= 2000);
  }

  public void testIncrementPollProb() {
    assertEquals(0.15, mbau.incrementPollProb(0.10), 0.001);
    assertEquals(0.50, mbau.incrementPollProb(0.45), 0.001);
    // shouldn't increment past max
    assertEquals(0.85, mbau.incrementPollProb(0.83), 0.001);
    assertEquals(0.85, mbau.incrementPollProb(0.85), 0.001);
  }

  public void testCheckPollProb() {
    mbau.curTopLevelPollProb = -1.0;
    mbau.checkPollProb();
    assertEquals(0.50, mbau.curTopLevelPollProb, 0.001);

    mbau.curTopLevelPollProb = .35;
    mbau.checkPollProb();
    assertEquals(0.50, mbau.curTopLevelPollProb, 0.001);

    mbau.curTopLevelPollProb = .90;
    mbau.checkPollProb();
    assertEquals(0.85, mbau.curTopLevelPollProb, 0.001);
  }

  public void testShouldCallTopLevelPoll() throws IOException {
    TimeBase.setSimulated(100);
    MockAuState state = new MockAuState(mbau, -1, TimeBase.nowMs(), -1, null);

    // no interval yet
    assertEquals(-1, mbau.nextPollInterval);
    assertEquals(-1.0, mbau.curTopLevelPollProb, 0);
    assertFalse(mbau.shouldCallTopLevelPoll(state));
    // should determine random interval
    assertTrue(mbau.nextPollInterval >= 5000);
    assertTrue(mbau.nextPollInterval <= 10000);
    assertEquals(0.5, mbau.curTopLevelPollProb, 0.001);

    // move to proper time
    TimeBase.step(mbau.nextPollInterval);
    boolean result = mbau.shouldCallTopLevelPoll(state);
    // should have reset interval
    assertEquals(-1, mbau.nextPollInterval);
    // may or may not have allowed poll
    if (mbau.curTopLevelPollProb != -1) {
      assertEquals(0.55, mbau.curTopLevelPollProb, 0.001);
      assertFalse(result);
    } else {
      assertEquals(-1.0, mbau.curTopLevelPollProb, 0);
      assertTrue(result);
    }

    TimeBase.setReal();
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
				 ConfigParamDescr.BASE_URL.getKey(), baseUrl,
				 ConfigParamDescr.VOLUME_NUMBER.getKey(),"42");
    mbau.setConfiguration(conf);
    assertFalse(mbau.shouldCrawlForNewContent(state));
  }

  public void testGetContentParserReturnsNullForNullMimeTupe() {
    assertNull(mbau.getContentParser(null));
  }

  public void testGetContentParserReturnsNullForMissingMimeType() {
    assertNull(mbau.getContentParser(""));
  }

  public void testGetContentParserReturnsGoslingHtmlParser() {
    assertTrue(mbau.getContentParser("text/html")
	       instanceof org.lockss.crawler.GoslingHtmlParser);

    assertTrue(mbau.getContentParser("Text/Html")
	       instanceof org.lockss.crawler.GoslingHtmlParser);
  }

  public void testGetContentParserReturnsGoslingHtmlParserSpaces() {
    assertTrue(mbau.getContentParser(" text/html ")
	       instanceof org.lockss.crawler.GoslingHtmlParser);
  }

  public void testReturnsGHPWithJunkAfterContentType() {
    assertTrue(mbau.getContentParser("text/html ; random content")
	       instanceof org.lockss.crawler.GoslingHtmlParser);
  }

  public void testGetContentParserReturnsSameGoslingHtmlParser() {
    assertSame(mbau.getContentParser("text/html"),
	       mbau.getContentParser("text/html"));
  }

  public void testFilterRuleCaching() throws IOException {
    MockFilterRule rule1 = new MockFilterRule();
    rule1.setFilteredReader(new StringReader("rule1"));
    MockFilterRule rule2 = new MockFilterRule();
    rule2.setFilteredReader(new StringReader("rule2"));

    assertNull(mbau.rule);
    assertEquals(0, mbau.ruleCacheMiss);
    assertNull(mbau.getFilterRule("test1"));
    assertEquals(1, mbau.ruleCacheMiss);
    mbau.rule = rule1;
    assertNotNull(mbau.getFilterRule("test1"));
    assertEquals(2, mbau.ruleCacheMiss);
    mbau.rule = rule2;
    assertNotNull(mbau.getFilterRule("test2"));
    assertEquals(3, mbau.ruleCacheMiss);

    rule1 = (MockFilterRule)mbau.getFilterRule("test2");
    assertEquals(3, mbau.ruleCacheMiss);
    assertEquals("rule2", StringUtil.fromReader(
        rule1.createFilteredReader(null)));
    rule2 = (MockFilterRule)mbau.getFilterRule("test1");
    assertEquals(3, mbau.ruleCacheMiss);
    assertEquals("rule1", StringUtil.fromReader(
        rule2.createFilteredReader(null)));
  }

  public void testFilterFactoryCaching() throws IOException {
    MockFilterFactory factory1 = new MockFilterFactory();
    factory1.setFilteredInputStream(new StringInputStream("factory1"));
    MockFilterFactory factory2 = new MockFilterFactory();
    factory2.setFilteredInputStream(new StringInputStream("factory2"));

    assertNull(mbau.factory);
    assertEquals(0, mbau.factoryCacheMiss);
    assertNull(mbau.getFilterFactory("test1"));
    assertEquals(1, mbau.factoryCacheMiss);
    mbau.factory = factory1;
    assertNotNull(mbau.getFilterFactory("test1"));
    assertEquals(2, mbau.factoryCacheMiss);
    mbau.factory = factory2;
    assertNotNull(mbau.getFilterFactory("test2"));
    assertEquals(3, mbau.factoryCacheMiss);

    factory1 = (MockFilterFactory)mbau.getFilterFactory("test2");
    assertEquals(3, mbau.factoryCacheMiss);
    assertEquals("factory2", StringUtil.fromInputStream(
        factory1.createFilteredInputStream(null, null, null)));
    factory2 = (MockFilterFactory)mbau.getFilterFactory("test1");
    assertEquals(3, mbau.factoryCacheMiss);
    assertEquals("factory1", StringUtil.fromInputStream(
        factory2.createFilteredInputStream(null, null, null)));
  }

  TitleConfig makeTitleConfig() {
    ConfigParamDescr d1 = new ConfigParamDescr("key1");
    ConfigParamDescr d2 = new ConfigParamDescr("key2");
    ConfigParamAssignment a1 = new ConfigParamAssignment(d1, "a");
    ConfigParamAssignment a2 = new ConfigParamAssignment(d2, "foo");
    a1.setEditable(false);
    a2.setEditable(false);
    TitleConfig tc1 = new TitleConfig("a", "b");
    tc1.setParams(ListUtil.list(a1, a2));
    return tc1;
  }


  public void testGetTitleConfig() throws IOException {
    TitleConfig tc = makeTitleConfig();
    mplug.setTitleConfig(tc);
    mplug.setSupportedTitles(ListUtil.list("a", "b"));
    mplug.setAuConfigDescrs(ListUtil.list(new ConfigParamDescr("key1"),
					  new ConfigParamDescr("key2")));

    Configuration config = ConfigurationUtil.fromArgs("key1", "a",
						      "key2", "foo");
    assertEquals(tc, mbau.findTitleConfig(config));

    Configuration config2 = ConfigurationUtil.fromArgs("key1", "b",
						       "key2", "foo");
    assertNull(mbau.findTitleConfig(config2));

    // remove one of tc's params, so it now incompletely describes the AU
    List lst = tc.getParams();
    lst.remove(0);
    tc.setParams(lst);
    assertNull(mbau.findTitleConfig(config));
  }

  public void testParamHandlerMap() throws ConfigurationException {
    Properties props = new Properties();
    props.setProperty(ConfigParamDescr.BASE_URL.getKey(), baseUrl);
    props.setProperty(ConfigParamDescr.VOLUME_NUMBER.getKey(), "10");
    props.setProperty(BaseArchivalUnit.PAUSE_TIME_KEY, "10000");
    props.setProperty(BaseArchivalUnit.USE_CRAWL_WINDOW, "true");
    props.setProperty(BaseArchivalUnit.NEW_CONTENT_CRAWL_KEY, "10000");

    Configuration config = ConfigurationUtil.fromProps(props);
    mbau.setBaseAuParams(config);
    // test that the ParamHandlerMap and the properties are the same
    String key = BaseArchivalUnit.AU_NEW_CRAWL_INTERVAL;
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

  public static void main(String[] argv) {
    String[] testCaseList = { MyMockBaseArchivalUnit.class.getName()};
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

  static class MyMockPlugin extends MockPlugin {
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

  static class MyMockBaseArchivalUnit extends BaseArchivalUnit {
    private String auId = null;
    private String m_name = "MockBaseArchivalUnit";
    int ruleCacheMiss = 0;
    int factoryCacheMiss = 0;
    FilterRule rule = null;
    FilterFactory factory = null;
    private CrawlRule m_rules = null;
    private String m_startUrl ="http://www.example.com/index.html";
    private boolean setAuParams = false;
    TitleConfig tc = null;

    private String mimeTypeCalledWith = null;

    MyMockBaseArchivalUnit(Plugin plugin, String name, CrawlRule rules,
			   String startUrl) {
      super(plugin);
      m_name = name;
      m_startUrl = startUrl;
      m_rules = rules;
   }

    public MyMockBaseArchivalUnit(Plugin myPlugin) {
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

    protected CrawlRule makeRules() {
      if(m_rules == null) {
        return new MockCrawlRule();
      }
      return m_rules;
    }

    protected String makeStartUrl() {
      return m_startUrl;
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
	paramMap.putLong(AU_NEW_CRAWL_INTERVAL, 12345);
	paramMap.putLong(AU_FETCH_DELAY, 54321);
	paramMap.putBoolean(AU_USE_CRAWL_WINDOW, false);
      }
    }

    protected FilterRule constructFilterRule(String mimeType) {
      ruleCacheMiss++;
      return rule;
    }

    protected FilterFactory constructFilterFactory(String mimeType) {
      factoryCacheMiss++;
      return factory;
    }

    public TitleConfig getTitleConfig() {
      if (tc != null) return tc;
      return super.getTitleConfig();
    }

    public void setTitleConfig(TitleConfig tc) {
      this.tc = tc;
    }
  }
}
