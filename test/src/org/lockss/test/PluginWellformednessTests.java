/*
n * $Id$
 */

/*

Copyright (c) 2000-2014 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.test;

import java.io.File;
import java.util.*;

import org.lockss.util.*;
import org.lockss.config.*;
import org.lockss.daemon.*;
import org.lockss.plugin.*;
import org.lockss.extractor.*;

/** Performs basic well-formedness tests on one or more plugins.  The list
 * of plugins may be supplied as a semicolon-separated list in the System
 * property org.lockss.test.TestPluginNames or, if invoked directly (i.e.,
 * not as a junit test), on the command line. */

public final class PluginWellformednessTests extends LockssTestCase {
  static Logger log = Logger.getLogger("PluginWellformednessTests");

  /** The System property under which this class expects to find a
   * semicolon-separated list of plugin names. */
  public static String PLUGIN_NAME_PROP = "org.lockss.test.TestPluginNames";

  protected MockLockssDaemon daemon;
  protected String pluginName;
  protected Plugin plugin;

  public void setUp() throws Exception {
    super.setUp();
    setUpDiskSpace();

    daemon = getMockLockssDaemon();
    daemon.getPluginManager().startService();
  }

  public void tearDown() throws Exception {
    super.tearDown();
  }

  protected Plugin getPlugin() {
    if (plugin == null) {
      plugin = PluginTestUtil.findPlugin(pluginName);
    }
    return plugin;
  }

  protected Configuration getSampleAuConfig() {
    Configuration config = ConfigManager.newConfiguration();
    for (ConfigParamDescr descr : getPlugin().getAuConfigDescrs()) {
      config.put(descr.getKey(), descr.getSampleValue());
    }
    return config;
  }

  protected ArchivalUnit createAu()
      throws ArchivalUnit.ConfigurationException {
    ArchivalUnit au = PluginTestUtil.createAu(pluginName, getSampleAuConfig());
    daemon.setNodeManager(new MockNodeManager(), au);
    daemon.setLockssRepository(new MockLockssRepository(), au);
    return au;
  }

  protected static String URL1 = "http://example.com/foo/bar";

  protected List<String> getSupportedMimeTypes() {
    return ListUtil.list("text/html", "text/css");
  }

  /** This test expects to find a semicolon-separated list in the System
   * property org.lockss.test.TestPluginNames .  It runs {@link
   * #testWellFormed(String)} on each one. */
  public void testPlugins() throws Exception {
    String args = System.getProperty(PLUGIN_NAME_PROP);
    if (StringUtil.isNullString(args)) {
      return;
    }
    List<String> failed = new ArrayList<String>();
    for (String pluginName : (List<String>)StringUtil.breakAt(args, ";")) {
      try {
 	System.err.println("Testing plugin: " + pluginName);
	resetAndTest(pluginName);
      } catch (PluginFailedToLoadException e) {
	log.error("Plugin " + pluginName + " failed");
	failed.add(pluginName);
      } catch (Exception e) {
	log.error("Plugin " + pluginName + " failed", e);
	failed.add(pluginName);
      }
    }
    if (!failed.isEmpty()) {
      fail(StringUtil.numberOfUnits(failed.size(), "plugin") + " failed: "
	   + failed);
    }
  }

  // Hack to reset the local state in order to test all the plugins in one
  // junit invocation (which is 20-50 times faster than invoking junit for
  // each one).

  void resetAndTest(String pluginName) throws Exception {
    this.pluginName = pluginName;
    plugin = null;
    testWellFormed(pluginName);
  }

  /** Load the named plugin, create an AU using sample parameters and
   * access all of its elements to ensure all the patterns are well formed
   * and the factories are loadable and runnable.
   */
  public void testWellFormed(String pluginName) throws Exception {
    if (getPlugin() == null) {
      throw new PluginFailedToLoadException();
    }
    ArchivalUnit au = createAu();

    assertSame(plugin, au.getPlugin());
    assertEquals(plugin.getPluginId(), au.getPluginId());

    assertEquals(getSampleAuConfig(), au.getConfiguration());

    au.getAuId();
    assertNotNull(au.getName());

    au.getAuCachedUrlSet();

    au.shouldBeCached(URL1);
    au.isLoginPageUrl(URL1);
    au.makeCachedUrl(URL1);
    au.makeUrlCacher(
        new UrlData(new StringInputStream(""), new CIProperties(), URL1));
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
    au.getPerHostPermissionPath();
    au.makeExcludeUrlsFromPollsPatterns();
    au.makeNonSubstanceUrlPatterns();
    au.makeSubstanceUrlPatterns();
    au.makeSubstancePredicate();
    au.makePermittedHostPatterns();
    au.makeRepairFromPeerIfMissingUrlPatterns();
    au.getCrawlUrlComparator();

    au.getCrawlWindow();
    au.makePermissionCheckers();
    au.getLoginPageChecker();
    au.getCookiePolicy();

    AuUtil.getConfigUserMessage(au);
    AuUtil.getProtocolVersion(au);
    AuUtil.getPollVersion(au);
    AuUtil.isDeleteExtraFiles(au, false);
    AuUtil.isDeleteExtraFiles(au, true);

    AuUtil.isRepairFromPublisherWhenTooClose(au, true);
    AuUtil.isRepairFromPublisherWhenTooClose(au, false);
    AuUtil.minReplicasForNoQuorumPeerRepair(au, 2);
  }

  public static class PluginFailedToLoadException extends Exception {
  }

  public static void main(String[] argv) {
    if (argv.length > 0) {
      String pluginNames = StringUtil.separatedString(argv, ";");
      System.setProperty(PLUGIN_NAME_PROP, pluginNames);
    }
    junit.textui.TestRunner.main(new String[] {
	PluginWellformednessTests.class.getName() });
  }

}
