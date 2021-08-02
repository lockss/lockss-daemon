/*

Copyright (c) 2000-2021 Board of Trustees of Leland Stanford Jr. University,
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
package org.lockss.plugin.definable;

import java.io.*;
import java.net.*;
import java.util.*;

import org.lockss.app.LockssDaemon;
import org.lockss.config.*;
import org.lockss.daemon.*;
import org.lockss.plugin.*;
import org.lockss.plugin.wrapper.*;
import org.lockss.plugin.base.*;
import org.lockss.test.*;
import org.lockss.extractor.*;
import org.lockss.crawler.*;
import org.lockss.rewriter.*;
import org.lockss.util.*;
import org.lockss.util.urlconn.*;

/**
 * <p>TestConfigurablePlugin: test case for ConfigurablePlugin</p>
 * @author Claire Griffin
 * @version 1.0
 */

public class TestDefinablePlugin extends LockssTestCase {
  static final String DEFAULT_PLUGIN_VERSION = "1";

  private MockLockssDaemon daemon;
  private MyDefinablePlugin definablePlugin = null;
  ExternalizableMap defMap;

  protected void setUp() throws Exception {
    super.setUp();
    daemon = getMockLockssDaemon();
    definablePlugin = new MyDefinablePlugin();
    defMap = new ExternalizableMap();
    definablePlugin.initPlugin(daemon, defMap);
  }

  protected void tearDown() throws Exception {
    definablePlugin = null;
    super.tearDown();
  }

  public void testGetElementList() throws Exception {
    defMap.putString("str", "one");
    defMap.putCollection("list", ListUtil.list("un", "deux"));
    defMap.putMap("map", MapUtil.map("k1", ListUtil.list("1", "2", "3"),
				     "k2", "42"));
    definablePlugin.initPlugin(daemon, defMap);
    assertSameElements(ListUtil.list("one"),
		       definablePlugin.getElementList("str"));
    assertSameElements(ListUtil.list("un", "deux"),
		       definablePlugin.getElementList("list"));
    assertNull(definablePlugin.getElementList("map"));
  }

  public void testFlatten() throws Exception {
    defMap.putString("str", "one");
    defMap.putCollection("list", ListUtil.list("un", "deux"));
    defMap.putMap("map", MapUtil.map("k1", ListUtil.list("1", "2", "3"),
				     "k2", "42"));
    definablePlugin.initPlugin(daemon, defMap);
    assertSameElements(ListUtil.list("str"),
		       definablePlugin.flatten("str"));
    assertSameElements(ListUtil.list("un", "deux"),
		       definablePlugin.flatten(ListUtil.list("un", "deux")));
    assertSameElements(ListUtil.list("1", "2", "3", "42"),
		       definablePlugin.flatten(MapUtil.map("k1", ListUtil.list("1", "2", "3"),
							   "k2", "42")));

    assertEmpty(definablePlugin.flatten(null));
  }

  public void testInitMimeMapDefault() throws Exception {
    // 2nd plugin to verify changes to 1st don't effect global mime map
    MyDefinablePlugin p2 = new MyDefinablePlugin();
    p2.initPlugin(daemon, new ExternalizableMap());
    MimeTypeInfo mti;

    mti = definablePlugin.getMimeTypeInfo("text/html");
    assertClass(GoslingHtmlLinkExtractor.Factory.class,
		mti.getLinkExtractorFactory());
    assertClass(""+mti.getLinkRewriterFactory().getClass(),
		NodeFilterHtmlLinkRewriterFactory.class,
		mti.getLinkRewriterFactory());
    assertNull(mti.getFileMetadataExtractorFactory());
    assertNull(mti.getContentValidatorFactory());
    mti = definablePlugin.getMimeTypeInfo("text/css");
    assertClass(RegexpCssLinkExtractor.Factory.class,
		mti.getLinkExtractorFactory());
    assertClass(RegexpCssLinkRewriterFactory.class,
		mti.getLinkRewriterFactory());
    mti = definablePlugin.getMimeTypeInfo("application/pdf");
    assertNull(mti.getHashFilterFactory());
    assertNull(mti.getCrawlFilterFactory());
    assertNull(mti.getLinkRewriterFactory()); // XXX 
    assertNull(mti.getContentValidatorFactory());

    defMap.putString(  ("application/pdf"
			+ DefinableArchivalUnit.SUFFIX_HASH_FILTER_FACTORY),
		     "org.lockss.test.MockFilterFactory");
    defMap.putString(  ("application/pdf"
			+ DefinableArchivalUnit.SUFFIX_CRAWL_FILTER_FACTORY),
		     "org.lockss.test.MockFilterFactory");
    defMap.putString(  ("text/html"
			+ DefinableArchivalUnit.SUFFIX_LINK_EXTRACTOR_FACTORY),
		     "org.lockss.test.MockLinkExtractorFactory");
    defMap.putString(  ("text/html"
			+ DefinableArchivalUnit.SUFFIX_LINK_REWRITER_FACTORY),
		     "org.lockss.test.MockLinkRewriterFactory");
    defMap.putString(  ("text/html"
			+ DefinableArchivalUnit.SUFFIX_CONTENT_VALIDATOR_FACTORY),
		       "org.lockss.test.MockContentValidatorFactory");
    // au.getContentValidatorFactory() supports wildcards
    defMap.putString(  ("text/*"
			+ DefinableArchivalUnit.SUFFIX_CONTENT_VALIDATOR_FACTORY),
		       Validator1.class.getName());
    defMap.putString(  ("*/*"
			+ DefinableArchivalUnit.SUFFIX_CONTENT_VALIDATOR_FACTORY),
		       Validator2.class.getName());
    Map factMap = new HashMap();
    factMap.put(MimeTypeInfo.DEFAULT_METADATA_TYPE,
		"org.lockss.test.MockFileMetadataExtractorFactory");
    defMap.putMap(  ("text/html"
		     + DefinableArchivalUnit.SUFFIX_METADATA_EXTRACTOR_FACTORY_MAP),
                  factMap);
    definablePlugin.initPlugin(daemon, defMap);

    mti = definablePlugin.getMimeTypeInfo("text/html");
    assertClass(LinkExtractorFactoryWrapper.class,
		mti.getLinkExtractorFactory());
    assertClass(MockLinkExtractorFactory.class,
		WrapperUtil.unwrap(mti.getLinkExtractorFactory()));
    assertClass(LinkRewriterFactoryWrapper.class,
		mti.getLinkRewriterFactory());
    assertClass(MockLinkRewriterFactory.class,
		WrapperUtil.unwrap(mti.getLinkRewriterFactory()));
    assertClass(MockContentValidatorFactory.class,
		WrapperUtil.unwrap(mti.getContentValidatorFactory()));
    assertClass(FileMetadataExtractorFactoryWrapper.class,
		mti.getFileMetadataExtractorFactory());
    assertClass(MockFileMetadataExtractorFactory.class,
		WrapperUtil.unwrap(mti.getFileMetadataExtractorFactory()));
    mti = definablePlugin.getMimeTypeInfo("text/css");
    assertClass(RegexpCssLinkExtractor.Factory.class,
		mti.getLinkExtractorFactory());
    mti = definablePlugin.getMimeTypeInfo("application/pdf");
    assertClass(FilterFactoryWrapper.class,
		mti.getHashFilterFactory());
    assertClass(FilterFactoryWrapper.class,
		mti.getCrawlFilterFactory());
    assertClass(MockFilterFactory.class,
		WrapperUtil.unwrap(mti.getHashFilterFactory()));
    assertClass(MockFilterFactory.class,
		WrapperUtil.unwrap(mti.getCrawlFilterFactory()));

    
    mti = definablePlugin.getMimeTypeInfo("text/foo");
    assertNull(WrapperUtil.unwrap(mti.getContentValidatorFactory()));
    assertClass(Validator1.class,
		WrapperUtil.unwrap(definablePlugin.getContentValidatorFactory("text/foo")));
    assertClass(Validator2.class,
		WrapperUtil.unwrap(definablePlugin.getContentValidatorFactory("application/foo")));


    // verify 2nd plugin still has mime defaults
    mti = p2.getMimeTypeInfo("text/html");
    assertClass(GoslingHtmlLinkExtractor.Factory.class,
		mti.getLinkExtractorFactory());
    assertClass(""+mti.getLinkRewriterFactory().getClass(),
		NodeFilterHtmlLinkRewriterFactory.class,
		mti.getLinkRewriterFactory());
    assertNull(mti.getFileMetadataExtractorFactory());
    mti = p2.getMimeTypeInfo("text/css");
    assertClass(RegexpCssLinkExtractor.Factory.class,
		mti.getLinkExtractorFactory());
    assertClass(RegexpCssLinkRewriterFactory.class,
		mti.getLinkRewriterFactory());

    mti = p2.getMimeTypeInfo("application/pdf");
    assertNull(mti.getHashFilterFactory());
    assertNull(mti.getCrawlFilterFactory());
    assertNull(mti.getLinkRewriterFactory()); // XXX 

  }

  public void testCreateAu() throws ArchivalUnit.ConfigurationException {
    Properties p = new Properties();
    p.setProperty("TEST_KEY", "TEST_VALUE");
    p.setProperty(ConfigParamDescr.BASE_URL.getKey(), "http://www.example.com/");
     p.setProperty(BaseArchivalUnit.KEY_PAUSE_TIME,"10000");
    List rules = ListUtil.list("1,\"http://www.example.com\"");
    ExternalizableMap map = definablePlugin.getDefinitionMap();
    map.putString(DefinablePlugin.KEY_PLUGIN_NAME, "testplugin");
    map.putCollection(DefinablePlugin.KEY_PLUGIN_CONFIG_PROPS,
                      Collections.EMPTY_LIST);
    map.putCollection(DefinableArchivalUnit.KEY_AU_CRAWL_RULES,rules);
    map.putString("au_start_url", "http://www.example.com/");
    ConfigurationUtil.setCurrentConfigFromProps(p);
    Configuration auConfig = CurrentConfig.getCurrentConfig();
    ArchivalUnit actualReturn = definablePlugin.createAu(auConfig);
    assertClass(DefinableArchivalUnit.class, actualReturn);
    assertEquals("configuration", auConfig, actualReturn.getConfiguration());
  }

  public void testGetAuConfigProperties() {
    Collection expectedReturn = ListUtil.list("Item1", "Item2");
    ExternalizableMap map = definablePlugin.getDefinitionMap();
    map.putCollection(DefinablePlugin.KEY_PLUGIN_CONFIG_PROPS,
                      expectedReturn);

    List actualReturn = definablePlugin.getLocalAuConfigDescrs();
    assertIsomorphic("return value", expectedReturn, actualReturn);
  }

  public void testGetConfigurationMap() {
    ExternalizableMap expectedReturn = definablePlugin.definitionMap;
    ExternalizableMap actualReturn = definablePlugin.getDefinitionMap();
    assertEquals("return value", expectedReturn, actualReturn);
  }

  public void testGetPluginName() {
    // no name set
    assertEquals("Internal", definablePlugin.getPluginName());

    // set the name
    String expectedReturn = "TestPlugin";
    defMap.putString(DefinablePlugin.KEY_PLUGIN_NAME, expectedReturn);
    assertEquals("return value", expectedReturn,
		 definablePlugin.getPluginName());
  }

  public void testGetVersion() {
    // no version set
    String expectedReturn = DEFAULT_PLUGIN_VERSION;
    String actualReturn = definablePlugin.getVersion();
    assertEquals("return value", expectedReturn, actualReturn);

    // set the version
    expectedReturn = "Version 1.0";
    ExternalizableMap map = definablePlugin.getDefinitionMap();
    map.putString(DefinablePlugin.KEY_PLUGIN_VERSION, expectedReturn);
    actualReturn = definablePlugin.getVersion();
    assertEquals("return value", expectedReturn, actualReturn);

  }

  public void testGetFeatureVersion() throws Exception {
    // no versions set
    assertEquals(null, definablePlugin.getFeatureVersion(Plugin.Feature.Poll));

    String extMapName = "org.lockss.plugin.definable.GoodPlugin";
    definablePlugin.initPlugin(daemon, extMapName);

    assertEquals("Poll_2",
		 definablePlugin.getFeatureVersion(Plugin.Feature.Poll));
    assertEquals("Metadata_7",
		 definablePlugin.getFeatureVersion(Plugin.Feature.Metadata));
    assertEquals("Substance_farty-two",
		 definablePlugin.getFeatureVersion(Plugin.Feature.Substance));

    assertEquals("  Poll: Poll_2\n  Metadata: Metadata_7\n  Substance: Substance_farty-two",
                 PluginManager.pluginFeatureVersionsString(definablePlugin));
  }

  public void testGetPluginId() throws Exception {
    String extMapName = "org.lockss.test.MockConfigurablePlugin";
    definablePlugin.initPlugin(daemon, extMapName);
    assertEquals("org.lockss.test.MockConfigurablePlugin",
                 definablePlugin.getPluginId());
    List<String> urls = definablePlugin.getLoadedFromUrls();
    assertMatchesRE("/org/lockss/test/MockConfigurablePlugin.xml$",
		    urls.get(0));
  }

  public void testGetPublishingPlatform() throws Exception {
    assertNull("Internal", definablePlugin.getPublishingPlatform());
    String expectedReturn = "Publisher Platform Shoes";
    defMap.putString(DefinablePlugin.KEY_PUBLISHING_PLATFORM, expectedReturn);
    assertEquals("return value", expectedReturn,
		 definablePlugin.getPublishingPlatform());
  }

  public void testGetDefaultArticleMimeType() throws Exception {
    assertEquals(null, definablePlugin.getDefaultArticleMimeType());
    String expectedReturn = "mime/type";
    defMap.putString(DefinablePlugin.KEY_DEFAULT_ARTICLE_MIME_TYPE,
		     expectedReturn);
    assertEquals("return value", expectedReturn,
		 definablePlugin.getDefaultArticleMimeType());
  }

  public void testInitPlugin() throws Exception {
    definablePlugin = null; //   ensure don't accidentally use wrong veriable
    DefinablePlugin plug = new DefinablePlugin();
    try {
      plug.initPlugin(daemon, (String)null);
      fail("initPlugin(, null) Should throw");
    }
    catch (NullPointerException npe) {
    }

    String extMapName = "org.lockss.test.MockConfigurablePlugin";
    plug.initPlugin(daemon, extMapName);
    assertEquals("Absinthe Literary Review",
                 plug.getPluginName());
    assertEquals("1", plug.getVersion());

    // check some other field
    StringBuffer sb = new StringBuffer("\"%sarchives%02d.htm\", ");
    sb.append(ConfigParamDescr.BASE_URL.getKey());
    sb.append(", ");
    sb.append(ConfigParamDescr.YEAR.getKey());
    ExternalizableMap map = plug.getDefinitionMap();
    assertEquals(sb.toString(),
                 map.getString(DefinableArchivalUnit.KEY_AU_START_URL, null));

  }

  public void testGoodPlugin() throws Exception {
    String prefix = "org.lockss.plugin.definable.";
    String extMapName = prefix + "GoodPlugin";
    definablePlugin.initPlugin(daemon, extMapName);
    assertEquals(prefix + "GoodPlugin", definablePlugin.getPluginId());
    List<String> urls = definablePlugin.getLoadedFromUrls();
    assertMatchesRE("/org/lockss/plugin/definable/GoodPlugin.xml$",
		    urls.get(0));
    assertEquals(1, urls.size());

    ExternalizableMap map = definablePlugin.getDefinitionMap();
    assertEquals(prefix + "GoodPlugin", map.getString("plugin_identifier"));
    assertEquals("Good Plugin", map.getString("plugin_name"));
    assertEquals("\"Good Plugin AU %s\", base_url", map.getString("au_name"));
    assertIsomorphic(ListUtil.list("\"%s\", base_url"),
		     map.getCollection("au_start_url"));
    assertEquals(6000, map.getLong("au_def_pause_time"));
    assertFalse(map.containsKey("parent_only"));
    assertEquals("child_val", map.getString("parent_cancel"));

    ArchiveFileTypes aft = definablePlugin.getArchiveFileTypes();
    assertSame(aft,ArchiveFileTypes.DEFAULT);
    assertEquals("val_17", map.getString("child_cancel"));
  }

  public void testGoodPluginWithOverride() throws Exception {
    ConfigurationUtil.addFromArgs("org.lockss.daemon.testingMode",
				  "content-testing");
    String prefix = "org.lockss.plugin.definable.";
    String extMapName = prefix + "GoodPlugin";
    definablePlugin.initPlugin(daemon, extMapName);
    assertEquals(prefix + "GoodPlugin", definablePlugin.getPluginId());
    List<String> urls = definablePlugin.getLoadedFromUrls();
    assertMatchesRE("/org/lockss/plugin/definable/GoodPlugin.xml$",
		    urls.get(0));
    assertEquals(1, urls.size());

    ExternalizableMap map = definablePlugin.getDefinitionMap();
    assertEquals(prefix + "GoodPlugin", map.getString("plugin_identifier"));
    assertEquals("Good Plugin", map.getString("plugin_name"));
    assertEquals("\"Good Plugin AU %s\", base_url", map.getString("au_name"));
    assertIsomorphic(ListUtil.list("\"%s\", base_url"),
		     map.getCollection("au_start_url"));
    assertEquals(3000, map.getLong("au_def_pause_time"));
    assertEquals("pval", map.getString("parent_only"));
    assertNull(map.getMapElement("parent_cancel"));
  }

  public void testInherit() throws Exception {
    String prefix = "org.lockss.plugin.definable.";
    String extMapName = prefix + "ChildPlugin";
    definablePlugin.initPlugin(daemon, extMapName);
    assertEquals(prefix + "ChildPlugin", definablePlugin.getPluginId());
    List<String> urls = definablePlugin.getLoadedFromUrls();
    assertMatchesRE("/org/lockss/plugin/definable/ChildPlugin.xml$",
		    urls.get(0));
    assertMatchesRE("/org/lockss/plugin/definable/GoodPlugin.xml$",
		    urls.get(1));
    assertEquals(2, urls.size());

    ExternalizableMap map = definablePlugin.getDefinitionMap();
    assertEquals(prefix + "ChildPlugin", map.getString("plugin_identifier"));
    assertEquals(prefix + "GoodPlugin", map.getString("plugin_parent"));
    assertEquals("Child Plugin", map.getString("plugin_name"));
    assertEquals("\"Good Plugin AU %s\", base_url", map.getString("au_name"));
    assertIsomorphic(ListUtil.list("\"%s\", base_url"),
		     map.getCollection("au_start_url"));
    assertEquals(6000, map.getLong("au_def_pause_time"));
    assertFalse(map.containsKey("parent_only"));
    assertEquals("child_val", map.getString("parent_cancel"));
    assertEquals("bar", map.getString("foo"));
    assertFalse(map.containsKey("child_cancel"));
  }

  public void testCorrectParentVersion() throws Exception {
    ConfigurationUtil.addFromArgs(DefinablePlugin.PARAM_PARENT_VERSION_MISMATCH_ACTION, "Error");
    String prefix = "org.lockss.plugin.definable.";
    String extMapName = prefix + "ChildPlugin";
    definablePlugin.initPlugin(daemon, extMapName);
    assertEquals(prefix + "ChildPlugin", definablePlugin.getPluginId());
  }

  public void testWrongParentVersion() throws Exception {
    ConfigurationUtil.addFromArgs(DefinablePlugin.PARAM_PARENT_VERSION_MISMATCH_ACTION, "Error");
    String prefix = "org.lockss.plugin.definable.";
    String extMapName = prefix + "ChildPluginWrongParentVersion";
    try {
      definablePlugin.initPlugin(daemon, extMapName);
      fail("Wrong parent plugin version should throw");
    } catch (PluginException.ParentVersionMismatch e) {
      assertMatchesRE("GoodPlugin has version 17 expected 18",
		      e.getMessage());
    }
  }

  public void testInheritWithOverride() throws Exception {
    ConfigurationUtil.addFromArgs("org.lockss.daemon.testingMode",
				  "content-testing");
    String prefix = "org.lockss.plugin.definable.";
    String extMapName = prefix + "ChildPlugin";
    definablePlugin.initPlugin(daemon, extMapName);
    assertEquals(prefix + "ChildPlugin", definablePlugin.getPluginId());
    List<String> urls = definablePlugin.getLoadedFromUrls();
    assertMatchesRE("/org/lockss/plugin/definable/ChildPlugin.xml$",
		    urls.get(0));
    assertMatchesRE("/org/lockss/plugin/definable/GoodPlugin.xml$",
		    urls.get(1));
    assertEquals(2, urls.size());

    ExternalizableMap map = definablePlugin.getDefinitionMap();
    assertEquals(prefix + "ChildPlugin", map.getString("plugin_identifier"));
    assertEquals(prefix + "GoodPlugin", map.getString("plugin_parent"));
    assertEquals("Child Plugin(test)", map.getString("plugin_name"));
    assertEquals("\"Good Plugin AU %s\", base_url", map.getString("au_name"));
    assertIsomorphic(ListUtil.list("\"%s\", base_url"),
		     map.getCollection("au_start_url"));
    assertEquals(3000, map.getLong("au_def_pause_time"));
    assertEquals("pval", map.getString("parent_only"));
    assertFalse(map.containsKey("parent_cancel"));
    assertEquals("barprime", map.getString("foo"));
  }

  public void testIllInherit() throws Exception {
    String prefix = "org.lockss.plugin.definable.";
    String extMapName = prefix + "ChildPluginNoParent";
    try {
      definablePlugin.initPlugin(daemon, extMapName);
      fail("initPlugin() of child with nonexistent parent should throw");
    } catch (PluginException.ParentNotFoundException e) {
    }
  }

  public void testInheritLoop() throws Exception {
    String prefix = "org.lockss.plugin.definable.";
    String extMapName = prefix + "ChildPluginParentLoop";
    try {
      definablePlugin.initPlugin(daemon, extMapName);
      fail("initPlugin() with inheritance loop should throw");
    } catch (PluginException.InvalidDefinition e) {
    }
  }

  public void testInstallCacheExceptionHandler() {
    DefinablePlugin plugin = new DefinablePlugin();
    ExternalizableMap map = plugin.getDefinitionMap();
    String name = new MockHttpResultHandler().getClass().getName();
    // test using a special class
    map.putString(DefinablePlugin.KEY_EXCEPTION_HANDLER,name);
    plugin.initResultMap();
    CacheResultHandler hand = plugin.getCacheResultHandler();
    assertClass(CacheResultHandlerWrapper.class, hand);
    assertClass(MockHttpResultHandler.class, WrapperUtil.unwrap(hand));

  }

  HttpResultMap getHttpResultMap(DefinablePlugin plugin) {
    return (HttpResultMap)plugin.getCacheResultMap();
  }

  class IOEParent extends IOException {
  }
  class IOEChild extends IOEParent {
  }

  public void testInstallCacheExceptionEntries() throws Exception {
    DefinablePlugin plugin = new DefinablePlugin();
    ExternalizableMap map = plugin.getDefinitionMap();
    IOException ioe1 = new SocketException("sock1");
    IOException ioe2 = new ConnectException("conn1");

    plugin.initResultMap();

    // nothing installed, should give the default
    assertClass(CacheException.NoRetryDeadLinkException.class,
		getHttpResultMap(plugin).mapException(null, "",
						      404, null));
    assertClass(CacheException.RetryableNetworkException_3_30S.class,
		getHttpResultMap(plugin).mapException(null, "",
						      ioe1, null));
    assertClass(CacheException.RetryableNetworkException_3_30S.class,
		getHttpResultMap(plugin).mapException(null, "",
						      ioe2, null));

    String spec1 =
      "404=org.lockss.util.urlconn.CacheException$RetryDeadLinkException";
    String spec2 =
      "java.net.SocketException" +
      "=org.lockss.util.urlconn.CacheException$RetryableNetworkException_2_5M";
    String spec3 =
      "407=" + MyHttpResultHandler.class.getName();
    String spec4 =
      "java.io.FileNotFoundException=" + MyHttpResultHandler.class.getName();

    map.putCollection(DefinablePlugin.KEY_EXCEPTION_LIST,
		      ListUtil.list(spec1, spec2, spec3, spec4));
    plugin.initResultMap();

    assertClass(CacheException.RetryDeadLinkException.class,
		getHttpResultMap(plugin).mapException(null, "",
						      404, null));
    // changing just SocketException should change result for
    // ConnectException as well
    assertClass(CacheException.RetryableNetworkException_2_5M.class,
		getHttpResultMap(plugin).mapException(null, "",
						      ioe1, null));
    assertClass(CacheException.RetryableNetworkException_2_5M.class,
		getHttpResultMap(plugin).mapException(null, "",
						      ioe2, null));

    assertClass(RecordingCacheException.class,
		getHttpResultMap(plugin).mapException(null, "",
						      407, null));
    assertClass(RecordingCacheException.class,
		getHttpResultMap(plugin).mapException(null, "",
						      new FileNotFoundException("foo"), null));
  }

  public void testInstallUnkClassCacheExceptionEntries() throws Exception {
    DefinablePlugin plugin = new DefinablePlugin();
    ExternalizableMap map = plugin.getDefinitionMap();

    String spec1 =
      "404=org.lockss.absent.package.NoClass";

    map.putCollection(DefinablePlugin.KEY_EXCEPTION_LIST,
		      ListUtil.list(spec1));
    try {
      plugin.initResultMap();
      fail("Ill http response should throw");
    } catch (PluginException.InvalidDefinition e) {
    }
  }

  public void testInstallIllClassCacheExceptionEntries() throws Exception {
    DefinablePlugin plugin = new DefinablePlugin();
    ExternalizableMap map = plugin.getDefinitionMap();

    String spec1 = "404=" + this.getClass().getName();

    map.putCollection(DefinablePlugin.KEY_EXCEPTION_LIST,
		      ListUtil.list(spec1));
    try {
      plugin.initResultMap();
      fail("Ill http response should throw");
    } catch (PluginException.InvalidDefinition e) {
    }
  }

  public void testSiteNormalizeUrlNull() {
    UrlNormalizer urlNormalizer = definablePlugin.getUrlNormalizer();
    assertSame(BasePlugin.NullUrlNormalizer.INSTANCE, urlNormalizer);
  }

  public void testSiteNormalizeUrl() {
    defMap.putString(ArchivalUnit.KEY_AU_URL_NORMALIZER,
		     "org.lockss.plugin.definable.TestDefinablePlugin$MyNormalizer");
    UrlNormalizer urlNormalizer = definablePlugin.getUrlNormalizer();
    assertClass(UrlNormalizerWrapper.class, urlNormalizer);
    assertClass(MyNormalizer.class, WrapperUtil.unwrap(urlNormalizer));
  }

  public void testMakeUrlNormalizerThrowsOnBadClass()
      throws LockssRegexpException {
    defMap.putString(ArchivalUnit.KEY_AU_URL_NORMALIZER,
		     "org.lockss.bogus.FakeClass");

    try {
      UrlNormalizer urlNormalizer = definablePlugin.getUrlNormalizer();
      fail("Should have thrown on a non-existant class");
    } catch (PluginException.InvalidDefinition e){
    }
  }

  public void testCreateCrawlUrlComparator() throws Exception {
    defMap.putString(DefinablePlugin.KEY_PLUGIN_CRAWL_URL_COMPARATOR_FACTORY,
		     "org.lockss.plugin.definable.TestDefinablePlugin$MyCrawlUrlComparatorFactory");
    MockArchivalUnit mau = new MockArchivalUnit();
    Comparator<CrawlUrl> comparator = definablePlugin.getCrawlUrlComparator(mau);
    assertClass(MyCrawlUrlComparator.class, comparator);
    assertSame(mau, ((MyCrawlUrlComparator)comparator).getAu());
  }

  public void testCreateCrawlUrlComparatorThrowsOnBadClass()
      throws PluginException.LinkageError {
    defMap.putString(DefinablePlugin.KEY_PLUGIN_CRAWL_URL_COMPARATOR_FACTORY,
		     "org.lockss.bogus.NoSuchUrlComparatorFactory");

    try {
      Comparator<CrawlUrl> c =
	definablePlugin.getCrawlUrlComparator(new MockArchivalUnit());
      fail("Should have thrown on a non-existant class");
    } catch (PluginException.InvalidDefinition e){
    }
  }

  String[] badPlugins = {
    "BadPluginIllArg1",
    "BadPluginIllArg2",
    "BadPluginIllArg3",
    "BadPluginIllArg4",
    "BadPluginIllArg5",
    "BadPluginIllArg6",
    "BadPluginIllArg7",
    "BadPluginIllArg8",
  };

  public void testLoadBadPlugin() throws Exception {
    String prefix = "org.lockss.plugin.definable.";
    // first ensure that a missing file is properly detected
    assertFalse(doesPluginExist(prefix + "NoPluginWithThisName"));
    // and that the canonical good plugin does load
    assertTrue("Control (good) plugin didn't load",
	       attemptToLoadPlugin(prefix + "GoodPlugin"));
    // then try various perturbations of it, which should all fail
    for (String bad : badPlugins) {
      testLoadBadPlugin(prefix + bad);
    }
  }

  public void testLoadBadPlugin(String pname) throws Exception {
    assertTrue(doesPluginExist(pname));
    assertFalse("Bad plugin " + pname + " should not have loaded successfully",
		attemptToLoadPlugin(pname));
  }

  public boolean doesPluginExist(String pname) {
    try {
      URL u = getClass().getClassLoader().getResource(pluginFilename(pname));
      return u != null;
    } catch (Exception ex) {
      log.debug2("No XML plugin: " + pname, ex);
      return false;
    }
  }

  String pluginFilename(String pname) {
    return pname.replace('.', '/') + ".xml";
  }

  private boolean attemptToLoadPlugin(String pname)  {
    PluginManager pmgr = daemon.getPluginManager();
    String key = PluginManager.pluginKeyFromId(pname);
    return pmgr.ensurePluginLoaded(key);
  }


  public static class MyDefinablePlugin extends DefinablePlugin {
    public MimeTypeInfo getMimeTypeInfo(String contentType) {
      return super.getMimeTypeInfo(contentType);
    }
  }

  public static class MyNormalizer implements UrlNormalizer {
    public String normalizeUrl (String url, ArchivalUnit au) {
      return "blah";
    }
  }

  public static class MyCrawlUrlComparatorFactory
    implements CrawlUrlComparatorFactory {
    public Comparator<CrawlUrl> createCrawlUrlComparator(ArchivalUnit au) {
      return new MyCrawlUrlComparator(au);
    }
  }

  public static class MyCrawlUrlComparator implements Comparator<CrawlUrl> {
    private ArchivalUnit au;

    public MyCrawlUrlComparator(ArchivalUnit au) {
      this.au = au;
    }

    public int compare(CrawlUrl curl1, CrawlUrl curl2) {
      return -1;
    }

    ArchivalUnit getAu() {
      return au;
    }
  }

  static public class MyHttpResultHandler implements CacheResultHandler {
    public void init(CacheResultMap crmap) {
      throw new UnsupportedOperationException();
    }
    public CacheException handleResult(ArchivalUnit au,
				       String url,
				       int responseCode) {
      return new RecordingCacheException(au, url, responseCode, null);
    }

    public CacheException handleResult(ArchivalUnit au,
				       String url,
				       Exception ex) {
      return new RecordingCacheException(au, url, -1, ex);
    }
  }

  static class RecordingCacheException extends CacheException {
    ArchivalUnit au;
    String url;
    int responseCode;
    Exception triggerException;

    RecordingCacheException(ArchivalUnit au,
			    String url,
			    int responseCode,
			    Exception triggerException) {
      this.au = au;
      this.url = url;
      this.responseCode = responseCode;
      this.triggerException = triggerException;
    }
  }

  public static class Validator1 extends MockContentValidatorFactory {};
  public static class Validator2 extends MockContentValidatorFactory {};

}
