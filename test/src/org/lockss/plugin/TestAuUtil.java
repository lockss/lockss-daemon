/*
 * $Id$
 */

/*

Copyright (c) 2000-2016 Board of Trustees of Leland Stanford Jr. University,
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

import java.io.*;
import java.net.*;
import java.util.*;
import org.lockss.config.Configuration;
import org.lockss.daemon.*;
import org.lockss.crawler.*;
import org.lockss.state.*;
import org.lockss.test.*;
import org.lockss.plugin.base.*;
import org.lockss.util.*;
import org.lockss.util.urlconn.*;
import org.lockss.plugin.ArchivalUnit.ConfigurationException;
import org.lockss.plugin.exploded.*;
import org.lockss.plugin.definable.*;

/**
 * This is the test class for org.lockss.plugin.AuUtil
 */
public class TestAuUtil extends LockssTestCase {

  static final ConfigParamDescr PD_VOL = ConfigParamDescr.VOLUME_NUMBER;
  static final ConfigParamDescr PD_YEAR = ConfigParamDescr.YEAR;
  static final ConfigParamDescr PD_OPT = new ConfigParamDescr("OPT_KEY");
  static {
    PD_OPT.setDefinitional(false);
  }

  static final String AUPARAM_VOL = PD_VOL.getKey();
  static final String AUPARAM_YEAR = PD_YEAR.getKey();
  static final String AUPARAM_OPT = PD_OPT.getKey();

  LocalMockPlugin mbp;

  public void setUp() throws Exception {
    super.setUp();
    mbp = new LocalMockPlugin();
    mbp.initPlugin(getMockLockssDaemon());
  }

  public void tearDown() throws Exception {
    super.tearDown();
  }

  TitleConfig makeTitleConfig(ConfigParamDescr descr, String val) {
    TitleConfig tc = new TitleConfig("foo", new MockPlugin());
    tc.setParams(ListUtil.list(new ConfigParamAssignment(descr, val)));
    return tc;
  }

  public void testGetDaemon()  {
    LocalMockArchivalUnit mau = new LocalMockArchivalUnit(mbp);
    assertSame(getMockLockssDaemon(), AuUtil.getDaemon(mau));
  }

  public void testThreadName() throws IOException {
    MockArchivalUnit mau = new MockArchivalUnit();
    mau.setName("Artichokes Volume Six");
    assertEquals("Crawl: Artichokes Volume Six",
		 AuUtil.getThreadNameFor("Crawl", mau));
    mau.setName("Fran\u00E7ais Volume Six");
    assertEquals("Crawl: Francais Volume Six",
		 AuUtil.getThreadNameFor("Crawl", mau));
  }

  public void testGetPollVersion() throws IOException {
    LocalMockArchivalUnit mau = new LocalMockArchivalUnit(mbp);
    assertEquals(null, AuUtil.getPollVersion(mau));
    mbp.setVersion("12");
    assertEquals("12", AuUtil.getPollVersion(mau));
    mbp.setFeatureVersionMap(MapUtil.map(Plugin.Feature.Poll, "3"));
    assertEquals("3", AuUtil.getPollVersion(mau));
  }

  public void testGetAuState() throws IOException {
    setUpDiskSpace();
    LocalMockArchivalUnit mau = new LocalMockArchivalUnit(mbp);
    getMockLockssDaemon().getNodeManager(mau).startService();
    AuState aus = AuUtil.getAuState(mau);
    assertEquals(AuState.CLOCKSS_SUB_UNKNOWN,
		 aus.getClockssSubscriptionStatus());
  }

  public void testIsCurrentFeatureVersion() throws IOException {
    setUpDiskSpace();
    LocalMockArchivalUnit mau = new LocalMockArchivalUnit(mbp);
    getMockLockssDaemon().getNodeManager(mau).startService();
    AuState aus = AuUtil.getAuState(mau);
    assertTrue(AuUtil.isCurrentFeatureVersion(mau, Plugin.Feature.Substance));
    assertTrue(AuUtil.isCurrentFeatureVersion(mau, Plugin.Feature.Metadata));
    mbp.setFeatureVersionMap(MapUtil.map(Plugin.Feature.Substance, "3"));
    assertFalse(AuUtil.isCurrentFeatureVersion(mau, Plugin.Feature.Substance));
    assertTrue(AuUtil.isCurrentFeatureVersion(mau, Plugin.Feature.Metadata));
    aus.setFeatureVersion(Plugin.Feature.Substance, "3");
    assertTrue(AuUtil.isCurrentFeatureVersion(mau, Plugin.Feature.Substance));
    assertTrue(AuUtil.isCurrentFeatureVersion(mau, Plugin.Feature.Metadata));
    mbp.setFeatureVersionMap(MapUtil.map(Plugin.Feature.Substance, "3",
					 Plugin.Feature.Metadata, "18"));
    assertTrue(AuUtil.isCurrentFeatureVersion(mau, Plugin.Feature.Substance));
    assertFalse(AuUtil.isCurrentFeatureVersion(mau, Plugin.Feature.Metadata));
    aus.setFeatureVersion(Plugin.Feature.Metadata, "18");
    assertTrue(AuUtil.isCurrentFeatureVersion(mau, Plugin.Feature.Substance));
    assertTrue(AuUtil.isCurrentFeatureVersion(mau, Plugin.Feature.Metadata));
    mbp.setFeatureVersionMap(MapUtil.map(Plugin.Feature.Substance, "4",
					 Plugin.Feature.Metadata, "18"));
    assertFalse(AuUtil.isCurrentFeatureVersion(mau, Plugin.Feature.Substance));
    assertTrue(AuUtil.isCurrentFeatureVersion(mau, Plugin.Feature.Metadata));
    aus.setSubstanceState(SubstanceChecker.State.Yes);
    assertTrue(AuUtil.isCurrentFeatureVersion(mau, Plugin.Feature.Substance));
    assertTrue(AuUtil.isCurrentFeatureVersion(mau, Plugin.Feature.Metadata));
  }

  public void testHasCrawled() throws IOException {
    setUpDiskSpace();
    LocalMockArchivalUnit mau = new LocalMockArchivalUnit(mbp);
    getMockLockssDaemon().getNodeManager(mau).startService();
    assertFalse(AuUtil.hasCrawled(mau));
    AuState aus = AuUtil.getAuState(mau);
    aus.newCrawlFinished(Crawler.STATUS_ERROR, "foo");
    assertFalse(AuUtil.hasCrawled(mau));
    aus.newCrawlFinished(Crawler.STATUS_SUCCESSFUL, "foo");
    assertTrue(AuUtil.hasCrawled(mau));
  }

  public void testGetPluginDefinition() throws Exception {
    ExternalizableMap map = new ExternalizableMap();
    map.setMapElement("foo", "bar");
    DefinablePlugin dplug = new DefinablePlugin();
    dplug.initPlugin(getMockLockssDaemon(), "FooPlugin", map, null);
    Plugin plug = dplug;
    assertSame(map, AuUtil.getPluginDefinition(plug));

    plug = new MockPlugin();
    assertEquals(0, AuUtil.getPluginDefinition(plug).size());
  }

  public void testMapException() {
    LocalMockArchivalUnit mau = new LocalMockArchivalUnit(mbp);
    assertClass(CacheException.RetryableNetworkException.class,
		AuUtil.mapException(mau, null,
				    new UnknownHostException(),
				    "foo"));
    assertClass(CacheException.UnknownExceptionException.class,
		AuUtil.mapException(mau, null,
				    new RuntimeException(),
				    "foo"));
  }

  public void testGetPluginList() throws IOException {
    LocalMockArchivalUnit mau = new LocalMockArchivalUnit(mbp);
    assertEmpty(AuUtil.getPluginList(mau, "foolst"));
//     Empty list isn't really modifiable but fails this test
//     assertUnmodifiable(AuUtil.getPluginList(mau, "foolst"));

    ExternalizableMap map = new ExternalizableMap();
    map.setMapElement("lst1", "bar");
    map.setMapElement("lst2", ListUtil.list("a", "bb", "ccc"));
    DefinablePlugin dplug = new DefinablePlugin();
    dplug.initPlugin(getMockLockssDaemon(), "FooPlugin", map, null);
    DefinableArchivalUnit au = new LocalDefinableArchivalUnit(dplug, map);
    assertEmpty(AuUtil.getPluginList(au, "lstnot"));
//     assertUnmodifiable(AuUtil.getPluginList(au, "lstnot"));
    assertEquals(ListUtil.list("bar"), AuUtil.getPluginList(au, "lst1"));
    assertUnmodifiable(AuUtil.getPluginList(au, "lst1"));
    assertEquals(ListUtil.list("a", "bb", "ccc"),
		 AuUtil.getPluginList(au, "lst2"));
    assertUnmodifiable(AuUtil.getPluginList(au, "lst2"));
  }

  public void testIsClosed() throws Exception {
    LocalMockArchivalUnit mau = new LocalMockArchivalUnit();
    assertFalse(AuUtil.isClosed(mau));
    mau.setConfiguration(ConfigurationUtil.fromArgs(ConfigParamDescr.AU_CLOSED.getKey(), "true"));
    assertTrue(AuUtil.isClosed(mau));
    mau.setTitleConfig(makeTitleConfig(ConfigParamDescr.AU_CLOSED, "false"));
    assertTrue(AuUtil.isClosed(mau));
    mau.setConfiguration(ConfigurationUtil.fromArgs("foo", "bar"));
    assertFalse(AuUtil.isClosed(mau));
    mau.setTitleConfig(makeTitleConfig(ConfigParamDescr.AU_CLOSED, "true"));
    assertTrue(AuUtil.isClosed(mau));
    mau.setTitleConfig(makeTitleConfig(ConfigParamDescr.AU_CLOSED, "false"));
    assertFalse(AuUtil.isClosed(mau));
  }

  public void testIsPubDown() throws Exception {
    LocalMockArchivalUnit mau = new LocalMockArchivalUnit();
    assertFalse(AuUtil.isPubDown(mau));
    assertFalse(AuUtil.isPubNever(mau));
    mau.setConfiguration(ConfigurationUtil.fromArgs(ConfigParamDescr.PUB_DOWN.getKey(), "true"));
    assertTrue(AuUtil.isPubDown(mau));
    assertFalse(AuUtil.isPubNever(mau));
    mau.setTitleConfig(makeTitleConfig(ConfigParamDescr.PUB_DOWN, "false"));
    assertTrue(AuUtil.isPubDown(mau));
    mau.setConfiguration(ConfigurationUtil.fromArgs("foo", "bar"));
    assertFalse(AuUtil.isPubDown(mau));
    mau.setTitleConfig(makeTitleConfig(ConfigParamDescr.PUB_DOWN, "true"));
    assertTrue(AuUtil.isPubDown(mau));
    mau.setTitleConfig(makeTitleConfig(ConfigParamDescr.PUB_DOWN, "false"));
    assertFalse(AuUtil.isPubDown(mau));
  }

  public void testIsPubDownTC() throws Exception {
    assertTrue(AuUtil.isPubDown(makeTitleConfig(ConfigParamDescr.PUB_DOWN,
						"true")));
    assertFalse(AuUtil.isPubDown(makeTitleConfig(ConfigParamDescr.PUB_DOWN,
						 "false")));
    assertFalse(AuUtil.isPubDown(makeTitleConfig(ConfigParamDescr.BASE_URL,
						 "http://foo.bar/")));
  }

  public void testIsPubNever() throws Exception {
    LocalMockArchivalUnit mau = new LocalMockArchivalUnit();
    assertFalse(AuUtil.isPubNever(mau));
    assertFalse(AuUtil.isPubDown(mau));
    mau.setConfiguration(ConfigurationUtil.fromArgs(ConfigParamDescr.PUB_NEVER.getKey(), "true"));
    assertTrue(AuUtil.isPubNever(mau));
    assertTrue(AuUtil.isPubDown(mau));
    mau.setTitleConfig(makeTitleConfig(ConfigParamDescr.PUB_NEVER, "false"));
    assertTrue(AuUtil.isPubNever(mau));
    assertTrue(AuUtil.isPubDown(mau));
    mau.setConfiguration(ConfigurationUtil.fromArgs("foo", "bar"));
    assertFalse(AuUtil.isPubNever(mau));
    assertFalse(AuUtil.isPubDown(mau));
    mau.setTitleConfig(makeTitleConfig(ConfigParamDescr.PUB_NEVER, "true"));
    assertTrue(AuUtil.isPubNever(mau));
    assertTrue(AuUtil.isPubDown(mau));
    mau.setTitleConfig(makeTitleConfig(ConfigParamDescr.PUB_NEVER, "false"));
    assertFalse(AuUtil.isPubNever(mau));
    assertFalse(AuUtil.isPubDown(mau));
  }

  public void testIsPubNeverTC() throws Exception {
    assertTrue(AuUtil.isPubNever(makeTitleConfig(ConfigParamDescr.PUB_NEVER,
						 "true")));
    assertFalse(AuUtil.isPubNever(makeTitleConfig(ConfigParamDescr.PUB_NEVER,
						  "false")));
    assertFalse(AuUtil.isPubNever(makeTitleConfig(ConfigParamDescr.BASE_URL,
						  "http://foo.bar/")));
  }

  public void testIsParamUrlHttpHttps() throws Exception {
    final String MYURLKEY = "my_url";
    final String BADURLKEY = "bad_url";
    // HTTP URL
    LocalMockArchivalUnit httpau = new LocalMockArchivalUnit();
    httpau.setConfiguration(ConfigurationUtil.fromArgs(MYURLKEY, "http://www.example.com/"));
    assertFalse(AuUtil.isParamUrlHttps(httpau, MYURLKEY));
    assertTrue(AuUtil.isParamUrlHttp(httpau, MYURLKEY));
    assertFalse(AuUtil.isParamUrlHttp(httpau, BADURLKEY));
    // HTTPS URL
    LocalMockArchivalUnit httpsau = new LocalMockArchivalUnit();
    httpsau.setConfiguration(ConfigurationUtil.fromArgs(MYURLKEY, "https://www.example.com/"));
    assertFalse(AuUtil.isParamUrlHttp(httpsau, MYURLKEY));
    assertTrue(AuUtil.isParamUrlHttps(httpsau, MYURLKEY));
    assertFalse(AuUtil.isParamUrlHttps(httpsau, BADURLKEY));
  }
  
  public void testIsBaseUrlHttpHttps() throws Exception {
    final String BASEURLKEY = ConfigParamDescr.BASE_URL.getKey();
    // HTTP-defined AU
    LocalMockArchivalUnit httpau = new LocalMockArchivalUnit();
    assertFalse(AuUtil.isBaseUrlHttp(httpau));
    assertFalse(AuUtil.isBaseUrlHttps(httpau));
    httpau.setConfiguration(ConfigurationUtil.fromArgs(BASEURLKEY, "http://www.example.com/"));
    assertTrue(AuUtil.isBaseUrlHttp(httpau));
    assertFalse(AuUtil.isBaseUrlHttps(httpau));
    // HTTPS-defined AU
    LocalMockArchivalUnit httpsau = new LocalMockArchivalUnit();
    assertFalse(AuUtil.isBaseUrlHttp(httpsau));
    assertFalse(AuUtil.isBaseUrlHttps(httpsau));
    httpsau.setConfiguration(ConfigurationUtil.fromArgs(BASEURLKEY, "https://www.example.com/"));
    assertFalse(AuUtil.isBaseUrlHttp(httpsau));
    assertTrue(AuUtil.isBaseUrlHttps(httpsau));
  }
  
  public void testNormalizeHttpHttpsFromParamUrl() throws Exception {
    /*
     * Note how a URL from an unrelated host like www.lockss.org is also
     * normalized.
     */
    final String MYURLKEY = "my_url";
    // HTTP URL
    LocalMockArchivalUnit httpau = new LocalMockArchivalUnit();
    assertEquals("http://www.example.com/favicon.ico",
                 AuUtil.normalizeHttpHttpsFromParamUrl(httpau, MYURLKEY,
                                                       "http://www.example.com/favicon.ico"));
    assertEquals("https://www.example.com/favicon.ico",
                 AuUtil.normalizeHttpHttpsFromParamUrl(httpau, MYURLKEY,
                                                       "https://www.example.com/favicon.ico"));
    httpau.setConfiguration(ConfigurationUtil.fromArgs(MYURLKEY, "http://www.example.com/"));
    assertEquals("http://www.example.com/favicon.ico",
                 AuUtil.normalizeHttpHttpsFromParamUrl(httpau, MYURLKEY,
                                                       "http://www.example.com/favicon.ico"));
    assertEquals("http://www.example.com/favicon.ico",
                 AuUtil.normalizeHttpHttpsFromParamUrl(httpau, MYURLKEY,
                                                       "https://www.example.com/favicon.ico"));
    assertEquals("http://www.lockss.org/favicon.ico",
                 AuUtil.normalizeHttpHttpsFromParamUrl(httpau, MYURLKEY,
                                                       "https://www.lockss.org/favicon.ico"));
    // HTTPS URL
    LocalMockArchivalUnit httpsau = new LocalMockArchivalUnit();
    assertEquals("http://www.example.com/favicon.ico",
                 AuUtil.normalizeHttpHttpsFromParamUrl(httpsau, MYURLKEY,
                                                       "http://www.example.com/favicon.ico"));
    assertEquals("https://www.example.com/favicon.ico",
                 AuUtil.normalizeHttpHttpsFromParamUrl(httpsau, MYURLKEY,
                                                       "https://www.example.com/favicon.ico"));
    httpsau.setConfiguration(ConfigurationUtil.fromArgs(MYURLKEY, "https://www.example.com/"));
    assertEquals("https://www.example.com/favicon.ico",
                 AuUtil.normalizeHttpHttpsFromParamUrl(httpsau, MYURLKEY,
                                                       "http://www.example.com/favicon.ico"));
    assertEquals("https://www.example.com/favicon.ico",
                 AuUtil.normalizeHttpHttpsFromParamUrl(httpsau, MYURLKEY,
                                                       "https://www.example.com/favicon.ico"));
    assertEquals("https://www.lockss.org/favicon.ico",
                 AuUtil.normalizeHttpHttpsFromParamUrl(httpsau, MYURLKEY,
                                                       "http://www.lockss.org/favicon.ico"));
  }
  
  public void testNormalizeHttpHttpsFromBaseUrl() throws Exception {
    /*
     * Note how a URL from an unrelated host like www.lockss.org is also
     * normalized.
     */
    final String BASEURLKEY = ConfigParamDescr.BASE_URL.getKey();
    // HTTP AU
    LocalMockArchivalUnit httpau = new LocalMockArchivalUnit();
    assertEquals("http://www.example.com/favicon.ico",
                 AuUtil.normalizeHttpHttpsFromBaseUrl(httpau,
                                                      "http://www.example.com/favicon.ico"));
    assertEquals("https://www.example.com/favicon.ico",
                 AuUtil.normalizeHttpHttpsFromBaseUrl(httpau,
                                                      "https://www.example.com/favicon.ico"));
    httpau.setConfiguration(ConfigurationUtil.fromArgs(BASEURLKEY, "http://www.example.com/"));
    assertEquals("http://www.example.com/favicon.ico",
                 AuUtil.normalizeHttpHttpsFromBaseUrl(httpau,
                                                      "http://www.example.com/favicon.ico"));
    assertEquals("http://www.example.com/favicon.ico",
                 AuUtil.normalizeHttpHttpsFromBaseUrl(httpau,
                                                      "https://www.example.com/favicon.ico"));
    assertEquals("http://www.lockss.org/favicon.ico",
                 AuUtil.normalizeHttpHttpsFromBaseUrl(httpau,
                                                      "https://www.lockss.org/favicon.ico"));
    // HTTPS AU
    LocalMockArchivalUnit httpsau = new LocalMockArchivalUnit();
    assertEquals("http://www.example.com/favicon.ico",
                 AuUtil.normalizeHttpHttpsFromBaseUrl(httpsau,
                                                      "http://www.example.com/favicon.ico"));
    assertEquals("https://www.example.com/favicon.ico",
                 AuUtil.normalizeHttpHttpsFromBaseUrl(httpsau,
                                                      "https://www.example.com/favicon.ico"));
    httpsau.setConfiguration(ConfigurationUtil.fromArgs(BASEURLKEY, "https://www.example.com/"));
    assertEquals("https://www.example.com/favicon.ico",
                 AuUtil.normalizeHttpHttpsFromBaseUrl(httpsau,
                                                      "http://www.example.com/favicon.ico"));
    assertEquals("https://www.example.com/favicon.ico",
                 AuUtil.normalizeHttpHttpsFromBaseUrl(httpsau,
                                                      "https://www.example.com/favicon.ico"));
    assertEquals("https://www.lockss.org/favicon.ico",
                 AuUtil.normalizeHttpHttpsFromBaseUrl(httpsau,
                                                      "http://www.lockss.org/favicon.ico"));
  }
  
  public void testIsDeleteExtraFiles() throws Exception {
    ExternalizableMap map = new ExternalizableMap();
    DefinablePlugin dplug = new DefinablePlugin();
    dplug.initPlugin(getMockLockssDaemon(), "FooPlugin", map, null);
    DefinableArchivalUnit au = new LocalDefinableArchivalUnit(dplug, map);
    assertFalse(AuUtil.isDeleteExtraFiles(au, false));
    assertTrue(AuUtil.isDeleteExtraFiles(au, true));
    map.putBoolean(DefinablePlugin.KEY_PLUGIN_DELETE_EXTRA_FILES, true);
    assertTrue(AuUtil.isDeleteExtraFiles(au, false));
    map.putBoolean(DefinablePlugin.KEY_PLUGIN_DELETE_EXTRA_FILES, false);
    assertFalse(AuUtil.isDeleteExtraFiles(au, true));
  }

  public void testIsRepairFromPublisherWhenTooClose() throws Exception {
    ExternalizableMap map = new ExternalizableMap();
    DefinablePlugin dplug = new DefinablePlugin();
    dplug.initPlugin(getMockLockssDaemon(), "FooPlugin", map, null);
    DefinableArchivalUnit au = new LocalDefinableArchivalUnit(dplug, map);
    assertFalse(AuUtil.isRepairFromPublisherWhenTooClose(au, false));
    assertTrue(AuUtil.isRepairFromPublisherWhenTooClose(au, true));
    map.putBoolean(DefinablePlugin.KEY_REPAIR_FROM_PUBLISHER_WHEN_TOO_CLOSE,
		   true);
    assertTrue(AuUtil.isRepairFromPublisherWhenTooClose(au, false));
    map.putBoolean(DefinablePlugin.KEY_REPAIR_FROM_PUBLISHER_WHEN_TOO_CLOSE,
		   false);
    assertFalse(AuUtil.isRepairFromPublisherWhenTooClose(au, true));
  }

  public void testMinReplicasForNoQuorumPeerRepair() throws Exception {
    ExternalizableMap map = new ExternalizableMap();
    DefinablePlugin dplug = new DefinablePlugin();
    dplug.initPlugin(getMockLockssDaemon(), "FooPlugin", map, null);
    DefinableArchivalUnit au = new LocalDefinableArchivalUnit(dplug, map);
    assertEquals(-1, AuUtil.minReplicasForNoQuorumPeerRepair(au, -1));
    assertEquals(2, AuUtil.minReplicasForNoQuorumPeerRepair(au, 2));
    map.putInt(DefinablePlugin.KEY_MIN_REPLICAS_FOR_NO_QUORUM_PEER_REPAIR, 2);
    assertEquals(2, AuUtil.minReplicasForNoQuorumPeerRepair(au, -1));
  }

  public void testGetTitleAttribute() {
    LocalMockArchivalUnit mau = new LocalMockArchivalUnit();
    TitleConfig tc = makeTitleConfig(ConfigParamDescr.PUB_DOWN, "false");
    mau.setTitleConfig(tc);
    assertNull(AuUtil.getTitleAttribute(mau, null));
    assertNull(AuUtil.getTitleAttribute(mau, "foo"));
    assertEquals("7", AuUtil.getTitleAttribute(mau, null, "7"));
    assertEquals("7", AuUtil.getTitleAttribute(mau, "foo", "7"));
    Map<String,String> attrs = new HashMap<String,String>();
    tc.setAttributes(attrs);
    assertNull(AuUtil.getTitleAttribute(mau, null));
    assertNull(AuUtil.getTitleAttribute(mau, "foo"));
    assertEquals("7", AuUtil.getTitleAttribute(mau, null, "7"));
    assertEquals("7", AuUtil.getTitleAttribute(mau, "foo", "7"));
    attrs.put("foo", "bar");
    assertEquals("bar", AuUtil.getTitleAttribute(mau, "foo"));
    assertEquals("bar", AuUtil.getTitleAttribute(mau, "foo", "7"));
  }

  public void testHasSubstancePatterns() throws Exception {
    ExternalizableMap map = new ExternalizableMap();
    DefinablePlugin dplug = new DefinablePlugin();
    dplug.initPlugin(getMockLockssDaemon(), "FooPlugin", map, null);
    DefinableArchivalUnit au = new LocalDefinableArchivalUnit(dplug, map);
    assertFalse(AuUtil.hasSubstancePatterns(au));
    map.putString(DefinableArchivalUnit.KEY_AU_SUBSTANCE_URL_PATTERN,
		  "/fulltext/");
    assertTrue(AuUtil.hasSubstancePatterns(au));
    map.removeMapElement(DefinableArchivalUnit.KEY_AU_SUBSTANCE_URL_PATTERN);
    assertFalse(AuUtil.hasSubstancePatterns(au));
    map.putCollection(DefinableArchivalUnit.KEY_AU_NON_SUBSTANCE_URL_PATTERN,
		      ListUtil.list("/fulltext/"));
    assertTrue(AuUtil.hasSubstancePatterns(au));

    map.removeMapElement(DefinableArchivalUnit.KEY_AU_NON_SUBSTANCE_URL_PATTERN);
    assertFalse(AuUtil.hasSubstancePatterns(au));
    map.putString(DefinablePlugin.KEY_PLUGIN_SUBSTANCE_PREDICATE_FACTORY,
		  "factname");
    assertTrue(AuUtil.hasSubstancePatterns(au));
  }

  public void testGetSubstanceTestThreshold() throws Exception {
    String key = ConfigParamDescr.CRAWL_TEST_SUBSTANCE_THRESHOLD.getKey();
    LocalMockArchivalUnit mau = new LocalMockArchivalUnit();
    assertEquals(-1, AuUtil.getSubstanceTestThreshold(mau));
    mau.setConfiguration(ConfigurationUtil.fromArgs(key, ""));
    assertEquals(-1, AuUtil.getSubstanceTestThreshold(mau));
    mau.setConfiguration(ConfigurationUtil.fromArgs(key, "foo"));
    assertEquals(-1, AuUtil.getSubstanceTestThreshold(mau));
    mau.setConfiguration(ConfigurationUtil.fromArgs(key, "0"));
    assertEquals(0, AuUtil.getSubstanceTestThreshold(mau));
    mau.setConfiguration(ConfigurationUtil.fromArgs(key, "3"));
    assertEquals(3, AuUtil.getSubstanceTestThreshold(mau));
    // title attr should override au config
    TitleConfig tc1 =
      makeTitleConfig(ConfigParamDescr.BASE_URL, "http://example.com");
    mau.setTitleConfig(tc1);
    assertEquals(3, AuUtil.getSubstanceTestThreshold(mau));
    tc1.setAttributes(MapUtil.map(key, "2"));
    assertEquals(2, AuUtil.getSubstanceTestThreshold(mau));
    tc1.setAttributes(MapUtil.map(key, "0"));
    assertEquals(0, AuUtil.getSubstanceTestThreshold(mau));
    tc1.setAttributes(MapUtil.map(key, "xxx"));
    assertEquals(3, AuUtil.getSubstanceTestThreshold(mau));
  }

  public void testGetTitleDefault() {
    TitleConfig tc = makeTitleConfig(ConfigParamDescr.CRAWL_PROXY, "foo:47");
    assertEquals(null, AuUtil.getTitleDefault(tc, ConfigParamDescr.BASE_URL));
    assertEquals("foo:47",
		 AuUtil.getTitleDefault(tc, ConfigParamDescr.CRAWL_PROXY));
  }

  public void testGetAuParamOrTitleDefault() throws Exception {
    LocalMockArchivalUnit mau = new LocalMockArchivalUnit();
    TitleConfig tc = makeTitleConfig(ConfigParamDescr.CRAWL_PROXY, "foo:47");
    assertNull(AuUtil.getAuParamOrTitleDefault(mau,
 					       ConfigParamDescr.CRAWL_PROXY));
    mau.setTitleConfig(tc);
    assertEquals("foo:47",
		 AuUtil.getAuParamOrTitleDefault(mau,
						 ConfigParamDescr.CRAWL_PROXY));
    Configuration config =
      ConfigurationUtil.fromArgs(ConfigParamDescr.CRAWL_PROXY.getKey(),
				 "abc:8080");
    mau.setConfiguration(config);
    assertEquals("abc:8080",
		 AuUtil.getAuParamOrTitleDefault(mau,
						 ConfigParamDescr.CRAWL_PROXY));
  }

  void setGlobalProxy(String host, int port) {
    Properties p = new Properties();
    if (host != null) {
      p.put(BaseCrawler.PARAM_PROXY_ENABLED, "true");
      p.put(BaseCrawler.PARAM_PROXY_HOST, host);
      p.put(BaseCrawler.PARAM_PROXY_PORT, ""+port);
    } else {
      p.put(BaseCrawler.PARAM_PROXY_ENABLED, "false");
    }
    ConfigurationUtil.setCurrentConfigFromProps(p);
  }
				    
  public void testGetAuProxyInfo() throws Exception {
    AuUtil.AuProxyInfo aupi;
    TitleConfig tc;

    LocalMockArchivalUnit mau = new LocalMockArchivalUnit();
    aupi = AuUtil.getAuProxyInfo(mau);
    assertEquals(null, aupi.getHost());
    assertFalse(aupi.isAuOverride());
    assertFalse(aupi.isInvalidAuOverride());
    assertEquals(null, aupi.getAuSpec());

    setGlobalProxy("host", 1111);
    aupi = AuUtil.getAuProxyInfo(mau);
    assertFalse(aupi.isAuOverride());
    assertFalse(aupi.isInvalidAuOverride());
    assertEquals("host", aupi.getHost());
    assertEquals(1111, aupi.getPort());
    assertEquals(null, aupi.getAuSpec());

    tc = makeTitleConfig(ConfigParamDescr.CRAWL_PROXY, "foo:47");
    mau.setTitleConfig(tc);
    aupi = AuUtil.getAuProxyInfo(mau);
    assertTrue(aupi.isAuOverride());
    assertFalse(aupi.isInvalidAuOverride());
    assertEquals("foo", aupi.getHost());
    assertEquals(47, aupi.getPort());
    assertEquals("foo:47", aupi.getAuSpec());

    tc = makeTitleConfig(ConfigParamDescr.CRAWL_PROXY, "HOST:1111");
    mau.setTitleConfig(tc);
    aupi = AuUtil.getAuProxyInfo(mau);
    assertFalse(aupi.isAuOverride());
    assertEquals("HOST", aupi.getHost());
    assertEquals(1111, aupi.getPort());

    setGlobalProxy(null, 0);
    aupi = AuUtil.getAuProxyInfo(mau);
    assertTrue(aupi.isAuOverride());
    assertEquals("HOST", aupi.getHost());
    assertEquals(1111, aupi.getPort());

    tc = makeTitleConfig(ConfigParamDescr.CRAWL_PROXY, "HOST:1112");
    mau.setTitleConfig(tc);
    aupi = AuUtil.getAuProxyInfo(mau);
    assertTrue(aupi.isAuOverride());
    assertEquals("HOST", aupi.getHost());
    assertEquals(1112, aupi.getPort());

    tc = makeTitleConfig(ConfigParamDescr.CRAWL_PROXY, "INVALIDPROXY");
    mau.setTitleConfig(tc);
    aupi = AuUtil.getAuProxyInfo(mau);
    assertFalse(aupi.isAuOverride());
    assertTrue(aupi.isInvalidAuOverride());
    assertEquals(null, aupi.getHost());
    assertEquals(0, aupi.getPort());
    assertEquals("INVALIDPROXY", aupi.getAuSpec());

  }

  public void testIsConfigCompatibleWithPlugin() {
    mbp.setConfigDescrs(ListUtil.list(PD_VOL, PD_YEAR, PD_OPT));
    Configuration auconf;
    Properties p = new Properties();

    // missing definitional param
    p.put(AUPARAM_VOL, "42");
    auconf = ConfigurationUtil.fromProps(p);
    assertFalse(AuUtil.isConfigCompatibleWithPlugin(auconf, mbp));

    // has all definitional params
    p.put(AUPARAM_YEAR, "1942");
    auconf = ConfigurationUtil.fromProps(p);
    assertTrue(AuUtil.isConfigCompatibleWithPlugin(auconf, mbp));

    // extra non-definitional
    p.put(AUPARAM_OPT, "foo");
    auconf = ConfigurationUtil.fromProps(p);
    assertTrue(AuUtil.isConfigCompatibleWithPlugin(auconf, mbp));

    // wrong type
    p.put(AUPARAM_YEAR, "foo");
    auconf = ConfigurationUtil.fromProps(p);
    assertFalse(AuUtil.isConfigCompatibleWithPlugin(auconf, mbp));
  }

  public void testOkDeleteExtraFiles() {
    assertTrue(AuUtil.okDeleteExtraFiles(new MockArchivalUnit()));
    assertFalse(AuUtil.okDeleteExtraFiles(new ExplodedArchivalUnit(new ExplodedPlugin(), null)));
  }

  public void testGetCu() {
    String url = "http://foo/";
    MockArchivalUnit mau = new MockArchivalUnit();
    CachedUrl mcu = new MockCachedUrl(url, mau);
    assertSame(mcu, AuUtil.getCu(mcu));
    MockCachedUrlSet mcus = new MockCachedUrlSet(url);
    mcus.setArchivalUnit(mau);
    assertNull(AuUtil.getCu(mcus));
    mau.addUrl(url, "foo");
    CachedUrl cu2 = AuUtil.getCu(mcus);
    assertEquals(url, cu2.getUrl());
  }

  public void testGetRedirectChainOne() {
    String url = "http://foo/";
    MockArchivalUnit mau = new MockArchivalUnit();
    MockCachedUrl first = mau.addUrl(url);
    assertEquals(ListUtil.list(url), AuUtil.getRedirectChain(first));
  }

  public void testGetRedirectChainTwo() {
    String url1 = "http://foo/";
    String url2 = "http://bar/";
    MockArchivalUnit mau = new MockArchivalUnit();
    MockCachedUrl first = mau.addUrl(url1);
    CIProperties props = new CIProperties();
    props.put(CachedUrl.PROPERTY_REDIRECTED_TO, url2);
    props.put(CachedUrl.PROPERTY_CONTENT_URL, url2);
    first.setProperties(props);
    assertEquals(ListUtil.list(url1, url2), AuUtil.getRedirectChain(first));
    MockCachedUrl second = mau.addUrl(url2);
    assertEquals(ListUtil.list(url1, url2), AuUtil.getRedirectChain(first));
  }

  public void testGetRedirectChainN() {
    String url1 = "http://foo1/";
    String url2 = "http://foo2/";
    String url3 = "http://foo3/";
    List<String> urls = ListUtil.list(url1, url2, url3);
    MockArchivalUnit mau = new MockArchivalUnit();
    setupRedirects(mau, urls);
    assertEquals(urls, AuUtil.getRedirectChain(mau.makeCachedUrl(url1)));
  }

  void setupRedirects(MockArchivalUnit mau, List<String> urls) {
    List<String> remUrls = new ArrayList(urls);
    String first = remUrls.remove(0);
    String last = urls.get(urls.size() - 1);
    MockCachedUrl firstCu = mau.addUrl(first, "content: " + first);
    List<MockCachedUrl> mcus = new ArrayList<MockCachedUrl>();
    mcus.add(firstCu);
    MockCachedUrl mcu = firstCu;

    for (String url : remUrls) {
      CIProperties props = new CIProperties();
      props.put(CachedUrl.PROPERTY_REDIRECTED_TO, url);
      props.put(CachedUrl.PROPERTY_CONTENT_URL, last);
      mcu.setProperties(props);
      mcu = mau.addUrl(url, "content: " + url);
      mcus.add(mcu);
    }
    CIProperties props = new CIProperties();
    props.put(CachedUrl.PROPERTY_CONTENT_URL, last);
    mcu.setProperties(props);
  }


  public void assertGetCharsetOrDefault(String expCharset, Properties props) {
    CIProperties cip  = null;
    if (props != null) {
      cip = CIProperties.fromProperties(props);
    }
    assertEquals(expCharset, AuUtil.getCharsetOrDefault(cip));
  }

  static String DEF = Constants.DEFAULT_ENCODING;
  static String CT_PROP = CachedUrl.PROPERTY_CONTENT_TYPE;

  public void testGetCharsetOrDefault() {
    assertGetCharsetOrDefault(DEF, null);
    assertGetCharsetOrDefault(DEF, PropUtil.fromArgs("foo", "bar"));
    assertGetCharsetOrDefault(DEF, PropUtil.fromArgs(CT_PROP,
						     "text/html"));
    assertGetCharsetOrDefault(DEF, PropUtil.fromArgs(CT_PROP,
						     "text/html;charset"));
    assertGetCharsetOrDefault("utf-8",
			      PropUtil.fromArgs(CT_PROP,
						"text/html;charset=utf-8"));
    assertGetCharsetOrDefault("utf-8",
			      PropUtil.fromArgs(CT_PROP,
						"text/html;charset=\"utf-8\""));
  }

  public void testGetAuCreationTime() throws IOException {
    long now = TimeBase.nowMs() - 10000;
    setUpDiskSpace();
    LocalMockArchivalUnit mau = new LocalMockArchivalUnit(mbp);
    getMockLockssDaemon().getNodeManager(mau).startService();

    long creationTime = AuUtil.getAuCreationTime(mau);
    assertTrue(creationTime > now);
  }

  public void testGetUrlFetchTime() throws IOException {
    String url1 = "http://foo/one";
    String url2 = "http://foo/two";
    // No fetch time stored
    MockArchivalUnit mau = new MockArchivalUnit();
    mau.addUrl(url1, "c1");

    assertEquals(0L, AuUtil.getUrlFetchTime(mau, url1));

    // Test the precedence of date properties
    MockCachedUrl mcu2 = mau.addUrl(url2, true, true);
    mcu2.setProperty("date", "Fri, 10 Sep 2004 09:43:40 GMT");
    assertEquals(1094809420000L, AuUtil.getUrlFetchTime(mau, url2));

    mcu2.setProperty("X_Lockss-server-date", "33333");
    assertEquals(33333L, AuUtil.getUrlFetchTime(mau, url2));

    mcu2.setProperty("X-Lockss-Orig-X_Lockss-server-date", "1234555");
    assertEquals(1234555L, AuUtil.getUrlFetchTime(mau, url2));

    mcu2.setProperty("X-Lockss-Orig-X_Lockss-server-date", "");
    assertEquals(33333L, AuUtil.getUrlFetchTime(mau, url2));

    // Add some versions.  MockCachedUrl returns them in the order added,
    // contrary to real repository nodes, so we check for the last one
    // added.
    MockCachedUrl mcu2a = mcu2.addVersion("ver 1");
    mcu2a.setProperty("X-Lockss-Orig-X_Lockss-server-date", "321321321");
    MockCachedUrl mcu2b = mcu2.addVersion("ver 2");
    mcu2b.setProperty("X-Lockss-Orig-X_Lockss-server-date", "88886666");
    assertEquals(88886666L, AuUtil.getUrlFetchTime(mau, url2));

    try {
      AuUtil.getUrlFetchTime(mau, "http://foo/three");
      fail("getUrlFetchTime() didn't throw on bad URL");
    } catch (NullPointerException npe) {
    }
  }

  private static class LocalMockArchivalUnit extends MockArchivalUnit {
    TitleConfig tc = null;

    LocalMockArchivalUnit() {
      super();
    }

    LocalMockArchivalUnit(Plugin plugin) {
      super(plugin);
    }

    public TitleConfig getTitleConfig() {
      return tc;
    }
    public void setTitleConfig(TitleConfig tc) {
      this.tc = tc;
    }
  }

  private static class LocalMockPlugin extends BasePlugin {
    String name;
    String version;
    List<ConfigParamDescr> configDescrs;
    Map<Plugin.Feature,String> featureVersion;

    public LocalMockPlugin() {
      super();
    }

    public void setPluginName(String name) {
      this.name = name;
    }

    public void setVersion(String version) {
      this.version = version;
    }

    public void setFeatureVersionMap(Map<Plugin.Feature,String> featureVersion) {
      this.featureVersion = featureVersion;
    }

    public void setConfigDescrs(List<ConfigParamDescr> configDescrs) {
      this.configDescrs = configDescrs;
    }

    protected ArchivalUnit createAu0(Configuration auConfig) throws
        ConfigurationException {
      MockArchivalUnit mau = new MockArchivalUnit();
      mau.setConfiguration(auConfig);
      return mau;
    }

    public String getVersion() {
      return version;
    }

    public String getFeatureVersion(Plugin.Feature feat) {
      if (featureVersion == null) {
	return null;
      }
      return featureVersion.get(feat);
    }

    public String getPluginName() {
      return name;
    }

    public List<ConfigParamDescr> getLocalAuConfigDescrs() {
      return configDescrs;
    }
  }

  private static class LocalDefinableArchivalUnit
    extends DefinableArchivalUnit {

    protected LocalDefinableArchivalUnit(DefinablePlugin plugin,
					 ExternalizableMap definitionMap) {
      super(plugin, definitionMap);
    }
  }
}
