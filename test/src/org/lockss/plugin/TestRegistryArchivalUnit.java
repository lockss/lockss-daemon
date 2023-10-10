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

package org.lockss.plugin;

import java.util.*;

import org.lockss.config.*;
import org.lockss.daemon.*;
import org.lockss.test.*;
import org.lockss.util.*;
import org.lockss.state.*;

/**
 * Test class for org.lockss.plugin.RegistryArchivalUnit
 */
public class TestRegistryArchivalUnit extends LockssTestCase {
	
  private static final Logger log = Logger.getLogger(TestRegistryArchivalUnit.class);
  
  private RegistryPlugin regPlugin;
  private MockLockssDaemon daemon;
  private PluginManager pluginMgr;
  String baseUrl = "http://foo.com/bar";

  public void setUp() throws Exception {
    super.setUp();
    daemon = getMockLockssDaemon();
    // make and init a real Pluginmgr
    pluginMgr = daemon.getPluginManager();

    // Make and start a UrlManager to set up the URLStreamHandlerFactory.
    // This is all so the cuurl created below can be opened by the parser
    UrlManager uMgr = new UrlManager();
    uMgr.initService(daemon);
    daemon.setDaemonInited(true);
    uMgr.startService();
    regPlugin = new MyRegistryPlugin();
    regPlugin.initPlugin(daemon);
  }

  public void tearDown() throws Exception {
    super.tearDown();
    // more...
  }

  public void testLoadAuConfigDescrs()
      throws ArchivalUnit.ConfigurationException {
    Properties auProps = new Properties();
    auProps.setProperty(ConfigParamDescr.BASE_URL.getKey(), baseUrl);
    Configuration auConfig = ConfigurationUtil.fromProps(auProps);

    Properties props = new Properties();
    props.setProperty(RegistryArchivalUnit.PARAM_REGISTRY_CRAWL_INTERVAL,
		      "107m");
    ConfigurationUtil.setCurrentConfigFromProps(props);
    ArchivalUnit au = regPlugin.createAu(auConfig);
    TypedEntryMap paramMap = au.getProperties();
    assertEquals(107 * Constants.MINUTE,
		 paramMap.getLong(ArchivalUnit.KEY_AU_NEW_CONTENT_CRAWL_INTERVAL));
    assertEquals(baseUrl,
		 paramMap.getString(ConfigParamDescr.BASE_URL.getKey()));
    assertNull(paramMap.getString(ConfigParamDescr.CRAWL_PROXY.getKey(), null));
    assertEquals("org|lockss|plugin|TestRegistryArchivalUnit$MyRegistryPlugin&base_url~http%3A%2F%2Ffoo%2Ecom%2Fbar", au.getAuId());
  }

  public void testCrawlRules() throws Exception {
    Configuration auConfig =
      ConfigurationUtil.fromArgs(ConfigParamDescr.BASE_URL.getKey(),
                                 "http://foo.com/bar");
    ArchivalUnit au = regPlugin.createAu(auConfig);
    assertTrue(au.shouldBeCached("http://foo.com/bar"));
    assertTrue(au.shouldBeCached("HTTP://foo.com/bar"));
    assertTrue(au.shouldBeCached("https://foo.com/bar"));
    assertTrue(au.shouldBeCached("HTTPS://foo.com/bar"));
    assertTrue(au.shouldBeCached("http://foo.com/bar/"));
    assertTrue(au.shouldBeCached("HTTP://foo.com/bar/"));
    assertTrue(au.shouldBeCached("https://foo.com/bar/"));
    assertTrue(au.shouldBeCached("HTTPS://foo.com/bar/"));
    assertFalse(au.shouldBeCached("http://foo.com/bar/file"));
    assertFalse(au.shouldBeCached("https://foo.com/bar/file"));
    assertTrue(au.shouldBeCached("http://foo.com/bar/file.jar"));
    assertTrue(au.shouldBeCached("https://foo.com/bar/file.jar"));
    assertFalse(au.shouldBeCached("https://foo.com/bar/file.jar.baz"));

    auConfig =
      ConfigurationUtil.fromArgs(ConfigParamDescr.BASE_URL.getKey(),
                                 "https://foo.com/bar");
    au = regPlugin.createAu(auConfig);
    assertFalse(au.shouldBeCached("http://foo.com/bar"));
    assertFalse(au.shouldBeCached("HTTP://foo.com/bar"));
    assertTrue(au.shouldBeCached("https://foo.com/bar"));
    assertTrue(au.shouldBeCached("HTTPS://foo.com/bar"));
    assertFalse(au.shouldBeCached("http://foo.com/bar/"));
    assertFalse(au.shouldBeCached("HTTP://foo.com/bar/"));
    assertTrue(au.shouldBeCached("https://foo.com/bar/"));
    assertTrue(au.shouldBeCached("HTTPS://foo.com/bar/"));
    assertFalse(au.shouldBeCached("http://foo.com/bar/file"));
    assertFalse(au.shouldBeCached("https://foo.com/bar/file"));
    assertTrue(au.shouldBeCached("http://foo.com/bar/file.jar"));
    assertTrue(au.shouldBeCached("https://foo.com/bar/file.jar"));
    assertFalse(au.shouldBeCached("http://foo.com/bar/file.jar.baz"));
    assertFalse(au.shouldBeCached("https://foo.com/bar/file.jar.baz"));
  }

  public void testCrawlProxy()
      throws ArchivalUnit.ConfigurationException {
    Configuration auConfig =
      ConfigurationUtil.fromArgs(ConfigParamDescr.BASE_URL.getKey(), baseUrl);

    ConfigurationUtil.addFromArgs(RegistryArchivalUnit.PARAM_REGISTRY_CRAWL_PROXY,
				  "proxy.host:1234");
    ArchivalUnit au = regPlugin.createAu(auConfig);
    TypedEntryMap paramMap = au.getProperties();
    assertEquals("crawl_proxy", ConfigParamDescr.CRAWL_PROXY.getKey());
    assertEquals("proxy.host:1234", paramMap.getString("crawl_proxy"));
    AuUtil.AuProxyInfo aupinfo = AuUtil.getAuProxyInfo(au);
    assertEquals("proxy.host", aupinfo.getHost());
    assertEquals(1234, aupinfo.getPort());

    // Ensure can change on the fly
    ConfigurationUtil.addFromArgs(RegistryArchivalUnit.PARAM_REGISTRY_CRAWL_PROXY,
				  "proxy2.host:4321");
    aupinfo = AuUtil.getAuProxyInfo(au);
    assertEquals("proxy2.host", aupinfo.getHost());
    assertEquals(4321, aupinfo.getPort());

    // Ensure can remove
    ConfigurationUtil.removeKey(RegistryArchivalUnit.PARAM_REGISTRY_CRAWL_PROXY);
    aupinfo = AuUtil.getAuProxyInfo(au);
    assertNull(aupinfo.getHost());
    assertEquals(0, aupinfo.getPort());
  }

  public void testRefetchDepth() throws Exception {
    Configuration auConfig =
      ConfigurationUtil.fromArgs(ConfigParamDescr.BASE_URL.getKey(), baseUrl);
    ArchivalUnit au = regPlugin.createAu(auConfig);
    assertEquals(org.lockss.crawler.FollowLinkCrawler.DEFAULT_MAX_CRAWL_DEPTH,
		 au.getRefetchDepth());
    // ensure FollowLinkCrawler.DEFAULT_MAX_CRAWL_DEPTH is an appropriate
    // depth
    assertTrue(au.getRefetchDepth() >= 100);
  }

  public void testRateLimiter() throws Exception {
    Configuration auConfig =
      ConfigurationUtil.fromArgs(ConfigParamDescr.BASE_URL.getKey(), baseUrl);
    ArchivalUnit au = regPlugin.createAu(auConfig);
    // default limiter source is au, which is null key
    assertEquals(null, au.getFetchRateLimiterKey());

    Properties props = new Properties();
    props.setProperty(RegistryArchivalUnit.PARAM_REGISTRY_FETCH_RATE,
		      "4/2s");
    props.setProperty(RegistryArchivalUnit.PARAM_REGISTRY_FETCH_RATE_LIMITER_SOURCE,
		      "au");
    ConfigurationUtil.setCurrentConfigFromProps(props);
    RateLimiter limiter = au.findFetchRateLimiter();
    assertEquals("4/2s", limiter.getRate());
    assertEquals(null, au.getFetchRateLimiterKey());
    props.setProperty(RegistryArchivalUnit.PARAM_REGISTRY_FETCH_RATE,
		      "3/7s");
    props.setProperty(RegistryArchivalUnit.PARAM_REGISTRY_FETCH_RATE_LIMITER_SOURCE,
		      "plugin");
    ConfigurationUtil.setCurrentConfigFromProps(props);
    assertEquals("3/7s", au.getRateLimiterInfo().getDefaultRate());
    assertEquals(au.getPlugin().getPluginId(), au.getFetchRateLimiterKey());
  }

  public void testShouldCallTopLevelPoll() throws Exception {
    Configuration auConfig =
      ConfigurationUtil.fromArgs(ConfigParamDescr.BASE_URL.getKey(), baseUrl);

    ConfigurationUtil.setFromArgs(RegistryArchivalUnit.PARAM_ENABLE_REGISTRY_POLLS,
				  "false");
    ArchivalUnit au = regPlugin.createAu(auConfig);
    AuState aus = new AuState(au,
			      123, // lastCrawlTime
			      321, // lastCrawlAttempt
			      -1, // lastCrawlResult
			      null, // lastCrawlResultMsg,
			      -1, // lastDeepCrawlTime
			      -1, // lastDeepCrawlAttempt
			      -1, // lastDeepCrawlResult
			      null, // lastDeepCrawlResultMsg,
			      -1, // lastDeepCrawlDepth
			      -1, // lastTopLevelPoll
			      -1, // lastPollStart
			      -1, // lastPollresult
			      null, // lastPollresultMsg
			      0, // pollDuration
			      -1, // lastTreeWalk
			      null, // crawlUrls
			      null, // accessType
			      1, // clockssSubscriptionState
			      1.0, // v3Agreement
			      1.0, // highestV3Agreement
			      SubstanceChecker.State.Unknown,
			      null, // substanceVersion
			      null, // metadataVersion
			      -1, // lastMetadataIndex
			      0, // lastContentChange
			      -1, // lastPoPPoll
			      -1, // lastPoPPollResult
			      -1, // lastLocalHashScan
			      -1, // numAgreePeersLastPoR
			      -1, // numWillingRepairers
			      -1, // numCurrentSuspectVersions
			      null, // cdnStems
			      (HistoryRepository)null);
    assertFalse(au.shouldCallTopLevelPoll(aus));
    ConfigurationUtil.setFromArgs(RegistryArchivalUnit.PARAM_ENABLE_REGISTRY_POLLS,
				  "true");
    ArchivalUnit au2 = regPlugin.createAu(auConfig);
    assertTrue(au2.shouldCallTopLevelPoll(aus));
  }

  public void testRecomputeRegNameTitle() throws Exception {
	// Also test HTML escapes
	Properties auProps = new Properties();
    auProps.setProperty(ConfigParamDescr.BASE_URL.getKey(), baseUrl);
    Configuration auConfig = ConfigurationUtil.fromProps(auProps);
    MyRegistryArchivalUnit au = new MyRegistryArchivalUnit(regPlugin);
    au.setConfiguration(auConfig);
    PluginTestUtil.registerArchivalUnit(regPlugin, au);
    au.addContent(au.getStartUrls().iterator().next(),
		  "<html><head><h2>foobar</h2>\n" +
		  "<title>This Title &amp; Weird &aacute;s&ccedil;&icirc;&igrave;</title></head></html>");
    assertEquals("This Title & Weird \u00e1s\u00e7\u00ee\u00ec", au.recomputeRegName());
  }

  public void testRecomputeRegNameTwoTitles() throws Exception {
    Properties auProps = new Properties();
    auProps.setProperty(ConfigParamDescr.BASE_URL.getKey(), baseUrl);
    Configuration auConfig = ConfigurationUtil.fromProps(auProps);
    MyRegistryArchivalUnit au = new MyRegistryArchivalUnit(regPlugin);
    au.setConfiguration(auConfig);
    PluginTestUtil.registerArchivalUnit(regPlugin, au);
    au.addContent(au.getStartUrls().iterator().next(),
		  "<html><head><h2>foobar</h2>\n" +
		  "<title>First Title No Verb</title>" +
		  "<title>Second Title No Verb</title></head></html>");
    assertEquals("First Title No Verb", au.recomputeRegName());
  }

  public void testRecomputeRegNameNoTitle() throws Exception {
    Properties auProps = new Properties();
    auProps.setProperty(ConfigParamDescr.BASE_URL.getKey(), baseUrl);
    Configuration auConfig = ConfigurationUtil.fromProps(auProps);
    MyRegistryArchivalUnit au = new MyRegistryArchivalUnit(regPlugin);
    au.setConfiguration(auConfig);
    PluginTestUtil.registerArchivalUnit(regPlugin, au);
    au.addContent(au.getStartUrls().iterator().next(),
		  "<html><h3>This Page No Title</h3></html>");
    assertEquals(null, au.recomputeRegName());
  }

  // Both of these methods are currently empty implementations on
  // RegistryPlugin, but it's nice to exercise them anyway, since they
  // are part of Plugin's public interface.

  public void testSetTitleConfigFromConfig() throws Exception {
    regPlugin.setTitleConfigFromConfig(null);
  }

  static class MyRegistryPlugin
    extends RegistryPlugin implements PluginTestable{
    public void registerArchivalUnit(ArchivalUnit au) {
      aus.add(au);
    }

    public void unregisterArchivalUnit(ArchivalUnit au) {
      aus.remove(au);
    }

    protected RegistryArchivalUnit newRegistryArchivalUnit() {
      return new MyRegistryArchivalUnit(this);
    }
  }

  static class MyRegistryArchivalUnit extends RegistryArchivalUnit {
    Map cumap = new HashMap();
    public MyRegistryArchivalUnit(RegistryPlugin plugin) {
      super(plugin);
    }
    public void addContent(String url, String content) {
      MockCachedUrl cu = new MockCachedUrl(url, this);
      cu.setContent(content);
      cu.setExists(true);
      cu.setProperties(new CIProperties());
      cumap.put(url, cu);
    }
    public CachedUrl makeCachedUrl(String url) {
      return (CachedUrl)cumap.get(url);
    }
  }

}
