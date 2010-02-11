/*
 * $Id: TestDefinablePlugin.java,v 1.31 2010-02-11 10:05:40 tlipkis Exp $
 */

/*

Copyright (c) 2000-2010 Board of Trustees of Leland Stanford Jr. University,
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

  private MyDefinablePlugin definablePlugin = null;
  ExternalizableMap defMap;

  protected void setUp() throws Exception {
    super.setUp();
    definablePlugin = new MyDefinablePlugin();
    defMap = new ExternalizableMap();
    definablePlugin.initPlugin(getMockLockssDaemon(), defMap);
  }

  protected void tearDown() throws Exception {
    definablePlugin = null;
    super.tearDown();
  }

  public void testInitMimeMapDefault() throws Exception {
    // 2nd plugin to verify changes to 1st don't effect global mime map
    MyDefinablePlugin p2 = new MyDefinablePlugin();
    p2.initPlugin(getMockLockssDaemon(), new ExternalizableMap());
    MimeTypeInfo mti;

    mti = definablePlugin.getMimeTypeInfo("text/html");
    assertTrue(mti.getLinkExtractorFactory()
	       instanceof GoslingHtmlLinkExtractor.Factory);
    assertTrue(""+mti.getLinkRewriterFactory().getClass(),
	       mti.getLinkRewriterFactory() instanceof
	       NodeFilterHtmlLinkRewriterFactory);
    assertNull(mti.getArticleIteratorFactory());
    assertNull(mti.getMetadataExtractorFactory());
    mti = definablePlugin.getMimeTypeInfo("text/css");
    assertTrue(mti.getLinkExtractorFactory()
	       instanceof CssLinkExtractor.Factory);
    assertTrue(mti.getLinkRewriterFactory()
	       instanceof StringFilterCssLinkRewriterFactory);
    mti = definablePlugin.getMimeTypeInfo("application/pdf");
    assertNull(mti.getHashFilterFactory());
    assertNull(mti.getCrawlFilterFactory());
    assertNull(mti.getFetchRateLimiter());
    assertNull(mti.getLinkRewriterFactory()); // XXX 

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
    Map factMap = new HashMap();
    factMap.put(MimeTypeInfo.DEFAULT_METADATA_TYPE,
		"org.lockss.test.MockMetadataExtractorFactory");
    defMap.putMap(  ("text/html"
		     + DefinableArchivalUnit.SUFFIX_METADATA_EXTRACTOR_FACTORY_MAP),
                  factMap);
    defMap.putString(  ("text/html"
			+ DefinableArchivalUnit.SUFFIX_ARTICLE_ITERATOR_FACTORY),
		     "org.lockss.test.MockArticleIteratorFactory");
    defMap.putString(  ("application/pdf"
			+ DefinableArchivalUnit.SUFFIX_FETCH_RATE_LIMITER),
		     "1/30s");
    definablePlugin.initPlugin(getMockLockssDaemon(), defMap);

    mti = definablePlugin.getMimeTypeInfo("text/html");
    System.err.println("fact: " + mti.getLinkExtractorFactory());
    assertTrue(mti.getLinkExtractorFactory()
	       instanceof LinkExtractorFactoryWrapper);
    assertTrue(WrapperUtil.unwrap(mti.getLinkExtractorFactory())
	       instanceof MockLinkExtractorFactory);
    System.err.println("fact: " + mti.getLinkRewriterFactory());
    assertTrue(mti.getLinkRewriterFactory()
	       instanceof LinkRewriterFactoryWrapper);
    assertTrue(WrapperUtil.unwrap(mti.getLinkRewriterFactory())
	       instanceof MockLinkRewriterFactory);
    assertTrue(mti.getArticleIteratorFactory()
	       instanceof ArticleIteratorFactoryWrapper);
    assertTrue(WrapperUtil.unwrap(mti.getArticleIteratorFactory())
	       instanceof MockArticleIteratorFactory);
    assertTrue(mti.getMetadataExtractorFactory()
	       instanceof MetadataExtractorFactoryWrapper);
    assertTrue(WrapperUtil.unwrap(mti.getMetadataExtractorFactory())
	       instanceof MockMetadataExtractorFactory);
    assertNull(mti.getFetchRateLimiter());
    mti = definablePlugin.getMimeTypeInfo("text/css");
    assertTrue(mti.getLinkExtractorFactory()
	       instanceof CssLinkExtractor.Factory);
    assertNull(mti.getFetchRateLimiter());
    mti = definablePlugin.getMimeTypeInfo("application/pdf");
    assertTrue(mti.getHashFilterFactory()
	       instanceof FilterFactoryWrapper);
    assertTrue(mti.getCrawlFilterFactory()
	       instanceof FilterFactoryWrapper);
    assertTrue(WrapperUtil.unwrap(mti.getHashFilterFactory())
	       instanceof MockFilterFactory);
    assertTrue(WrapperUtil.unwrap(mti.getCrawlFilterFactory())
	       instanceof MockFilterFactory);
    assertEquals("1/30s", mti.getFetchRateLimiter().getRate());

    // verify 2nd plugin still has mime defaults
    mti = p2.getMimeTypeInfo("text/html");
    assertTrue(mti.getLinkExtractorFactory()
	       instanceof GoslingHtmlLinkExtractor.Factory);
    assertTrue(""+mti.getLinkRewriterFactory().getClass(),
	       mti.getLinkRewriterFactory() instanceof
	       NodeFilterHtmlLinkRewriterFactory);
    assertNull(mti.getArticleIteratorFactory());
    assertNull(mti.getMetadataExtractorFactory());
    mti = p2.getMimeTypeInfo("text/css");
    assertTrue(mti.getLinkExtractorFactory()
	       instanceof CssLinkExtractor.Factory);
    assertTrue(mti.getLinkRewriterFactory()
	       instanceof StringFilterCssLinkRewriterFactory);

    mti = p2.getMimeTypeInfo("application/pdf");
    assertNull(mti.getHashFilterFactory());
    assertNull(mti.getCrawlFilterFactory());
    assertNull(mti.getFetchRateLimiter());
    assertNull(mti.getLinkRewriterFactory()); // XXX 
  }

  public void testInitMimeMap() {
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
    assertTrue(actualReturn instanceof DefinableArchivalUnit);
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

  public void testGetPluginId() throws Exception {
    LockssDaemon daemon = getMockLockssDaemon();
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
    assertEquals("text/html", definablePlugin.getDefaultArticleMimeType());
    String expectedReturn = "mime/type";
    defMap.putString(DefinablePlugin.KEY_DEFAULT_ARTICLE_MIME_TYPE,
		     expectedReturn);
    assertEquals("return value", expectedReturn,
		 definablePlugin.getDefaultArticleMimeType());
  }

  public void testInitPlugin() throws Exception {
    definablePlugin = null; //   ensure don't accidentally use wrong veriable
    LockssDaemon daemon = getMockLockssDaemon();
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

  public void testInherit() throws Exception {
    String prefix = "org.lockss.plugin.definable.";
    LockssDaemon daemon = getMockLockssDaemon();
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
    assertEquals("bar", map.getString("foo"));
  }

  public void testIllInherit() throws Exception {
    String prefix = "org.lockss.plugin.definable.";
    LockssDaemon daemon = getMockLockssDaemon();
    String extMapName = prefix + "ChildPluginNoParent";
    try {
      definablePlugin.initPlugin(daemon, extMapName);
      fail("initPlugin() of child with nonexistent parent should throw");
    } catch (FileNotFoundException e) {
    }
  }

  public void testInheritLoop() throws Exception {
    String prefix = "org.lockss.plugin.definable.";
    LockssDaemon daemon = getMockLockssDaemon();
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
    assertTrue(hand instanceof CacheResultHandlerWrapper);
    assertTrue(WrapperUtil.unwrap(hand) instanceof MockHttpResultHandler);

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
		getHttpResultMap(plugin).mapException(null, null,
						      404, null));
    assertClass(CacheException.RetryableNetworkException_3_30S.class,
		getHttpResultMap(plugin).mapException(null, null,
						      ioe1, null));
    assertClass(CacheException.RetryableNetworkException_3_30S.class,
		getHttpResultMap(plugin).mapException(null, null,
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
		getHttpResultMap(plugin).mapException(null, null,
						      404, null));
    // changing just SocketException should change result for
    // ConnectException as well
    assertClass(CacheException.RetryableNetworkException_2_5M.class,
		getHttpResultMap(plugin).mapException(null, null,
						      ioe1, null));
    assertClass(CacheException.RetryableNetworkException_2_5M.class,
		getHttpResultMap(plugin).mapException(null, null,
						      ioe2, null));

    assertClass(RecordingCacheException.class,
		getHttpResultMap(plugin).mapException(null, null,
						      407, null));
    assertClass(RecordingCacheException.class,
		getHttpResultMap(plugin).mapException(null, null,
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
    assertTrue(urlNormalizer instanceof UrlNormalizerWrapper);
    assertTrue(WrapperUtil.unwrap(urlNormalizer) instanceof MyNormalizer);
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
    assertTrue(comparator instanceof MyCrawlUrlComparator);
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
  };

  public void testLoadBadPlugin() throws Exception {
    String prefix = "org.lockss.plugin.definable.";
    // first ensure that the canonical good plugin does load
    assertTrue("Control (good) plugin didn't load",
	       attemptToLoadPlugin(prefix + "GoodPlugin"));
    // then try various perturbations of it, which should all fail
    for (String bad : badPlugins) {
      testLoadBadPlugin(prefix + bad);
    }
  }

  public void testLoadBadPlugin(String pname) throws Exception {
    assertFalse("Bad plugin " + pname + " should not have loaded successfully",
		attemptToLoadPlugin(pname));
  }

  private boolean attemptToLoadPlugin(String pname)  {
    PluginManager pmgr = getMockLockssDaemon().getPluginManager();
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

}
