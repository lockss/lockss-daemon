/*

Copyright (c) 2000-2022 Board of Trustees of Leland Stanford Jr. University,
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
import java.util.*;

import org.apache.commons.lang3.tuple.*;
import org.apache.commons.io.IOUtils;
import org.lockss.app.*;
import org.lockss.config.*;
import org.lockss.daemon.*;
import org.lockss.plugin.*;
import org.lockss.plugin.definable.*;
import org.lockss.state.*;
import org.lockss.test.*;
import org.lockss.util.*;
import org.lockss.extractor.*;

/** Performs basic well-formedness tests on a plugin. */
public final class PluginValidator {
  static Logger log = Logger.getLogger("PluginWellformednessTests");

  protected static String URL1 = "http://example.com/foo/bar";

  protected MockLockssDaemon daemon;
  protected Plugin plugin;
  protected String pluginName;

  public PluginValidator(MockLockssDaemon daemon, String pluginName, Plugin plugin) {
    this.daemon = daemon;
    this.plugin = plugin;
    this.pluginName = pluginName;
  }

  public class AssertionFailedException extends Exception {
    public AssertionFailedException(String msg) {
      super(msg);
    }
  }

  public void fail(String msg) throws AssertionFailedException {
    throw new AssertionFailedException(msg);
  }

  public void assertSame(Object o1, Object o2) throws AssertionFailedException {
    if (o1 != o2) {
      fail("Expected (same): " + o1 + " but was: " + o2);
    }
  }

  public void assertEquals(Object o1, Object o2) throws AssertionFailedException {
    if (o1 == null || o2 == null) {
      if (o1 != o2) {
        fail("Expected: " + o1 + " but was: " + o2);
      }
    } else if (!o1.equals(o2)) {
      fail("Expected: " + o1 + " but was: " + o2);
    }
  }

  public void assertNull(Object o1) throws AssertionFailedException {
    if (o1 != null) {
      fail("Expected null: " + o1);
    }
  }

  public void assertNotNull(Object o1) throws AssertionFailedException {
    if (o1 == null) {
      fail("Expected not null: " + o1);
    }
  }

  protected List<String> getSupportedMimeTypes() {
    return ListUtil.list("text/html", "text/css");
  }

  public void validatePlugin() throws Exception {
    ArchivalUnit au = createAu();

    assertSame(plugin, au.getPlugin());
    assertEquals(plugin.getPluginId(), au.getPluginId());

    if (plugin instanceof DefinablePlugin) {
      DefinablePlugin dplug = (DefinablePlugin)(plugin);
      TypedEntryMap plugDef = dplug.getDefinitionMap();
      if (!pluginName.equals(plugDef.getString(DefinablePlugin.KEY_PLUGIN_IDENTIFIER))) {
	log.warning("Wrong plugin_id: " +
		    plugDef.getString(DefinablePlugin.KEY_PLUGIN_IDENTIFIER) +
		    " should be " + pluginName);
      }
      if (dplug.getPluginId().endsWith("SourcePlugin")) {
	if (plugDef.containsKey(DefinablePlugin.KEY_PLUGIN_BULK_CONTENT)) {
	  if (!plugDef.getBoolean(DefinablePlugin.KEY_PLUGIN_BULK_CONTENT)) {
	    log.warning("Plugin name " + pluginName + " suggests it's a source plugin, but it has " + DefinablePlugin.KEY_PLUGIN_BULK_CONTENT + " explicitly set false");
	  }
	} else {
	  fail("Plugin " + pluginName + " is treated as a source/bulk plugin becuase of its name - it should have " + DefinablePlugin.KEY_PLUGIN_BULK_CONTENT + " set to true");
	}
      }

      // assertEquals("Wrong plugin_id", pluginName,
      // 		   plugDef.getString(DefinablePlugin.KEY_PLUGIN_IDENTIFIER));
    }

    assertEquals(getSampleAuConfig(), au.getConfiguration());

    au.getAuId();
    assertNotNull(au.getName());

    au.getAuCachedUrlSet();

    au.shouldBeCached(URL1);
    au.isLoginPageUrl(URL1);
    au.makeCachedUrl(URL1);
    au.makeUrlCacher(
        new UrlData(IOUtils.toInputStream(""), new CIProperties(), URL1));
    assertNotNull(au.siteNormalizeUrl(URL1));

    au.getUrlStems();
    au.getTitleConfig();

    for (String mime : getSupportedMimeTypes()) {
      au.getLinkExtractor(mime);
      au.getLinkRewriterFactory(mime);
      au.getFilterRule(mime);
      au.getHashFilterFactory(mime);
      au.getCrawlFilterFactory(mime);
      au.getFileMetadataExtractor(new MetadataTarget(), mime);
    }
    au.getArticleIterator();

    RateLimiterInfo rli = au.getRateLimiterInfo();
    new RateLimiter(rli.getDefaultRate());
    au.getFetchRateLimiterKey();
    au.getPermissionUrls();
    au.getStartUrls();
    au.getAccessUrls();
    au.getPerHostPermissionPath();
    au.makeExcludeUrlsFromPollsPatterns();
    au.makeUrlPollResultWeightMap();
    au.makeNonSubstanceUrlPatterns();
    au.makeSubstanceUrlPatterns();
    au.makeSubstancePredicate();
    au.makePermittedHostPatterns();
    au.makeRepairFromPeerIfMissingUrlPatterns();
    au.getCrawlUrlComparator();
    MockArchivalUnit mau = new MockArchivalUnit();
    MockCrawler.MockCrawlerFacade mcf = new MockCrawler().new MockCrawlerFacade(au);
    au.makeCrawlSeed(mcf);
    UrlFetcher uf = au.makeUrlFetcher(mcf, URL1);
    CIProperties props = new CIProperties();
    props.setProperty("Content-Length", "7");
    UrlData ud = new UrlData(IOUtils.toInputStream("foo"), props, URL1);
    au.makeUrlCacher(ud);

    au.getUrlConsumerFactory();
    FetchedUrlData fud = new FetchedUrlData(URL1, URL1,
                                            IOUtils.toInputStream("bar"),
                                            props, null, uf);
    au.getUrlConsumerFactory().createUrlConsumer(mcf, fud);

    au.getCrawlWindow();
    au.makePermissionCheckers();
    au.getLoginPageChecker();
    au.getCookiePolicy();

    au.siteNormalizeUrl("http://exmaple.com/path/");

    AuUtil.getConfigUserMessage(au);
    AuUtil.getProtocolVersion(au);
    AuUtil.getPollVersion(au);
    AuUtil.isDeleteExtraFiles(au, false);
    AuUtil.isDeleteExtraFiles(au, true);

    AuUtil.isRepairFromPublisherWhenTooClose(au, true);
    AuUtil.isRepairFromPublisherWhenTooClose(au, false);
    AuUtil.minReplicasForNoQuorumPeerRepair(au, 2);
    
    au.getUrlConsumerFactory();
  }

  protected Configuration getSampleAuConfig() throws IOException {
    Configuration config = ConfigManager.newConfiguration();
    for (ConfigParamDescr descr : plugin.getAuConfigDescrs()) {
      config.put(descr.getKey(), descr.getSampleValue());
    }
    return config;
  }

  protected ArchivalUnit createAu()
      throws ArchivalUnit.ConfigurationException, IOException {
    ArchivalUnit au = PluginTestUtil.createAu(pluginName, getSampleAuConfig());
    daemon.setNodeManager(new MockNodeManager(), au);
    daemon.setLockssRepository(new MockLockssRepository(), au);
    return au;
  }

}
