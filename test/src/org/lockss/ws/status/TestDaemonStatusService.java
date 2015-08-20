/*
 * $Id$
 */

/*

 Copyright (c) 2013-2015 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.ws.status;

import java.io.IOException;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import org.lockss.config.ConfigManager;
import org.lockss.config.Configuration;
import org.lockss.daemon.PluginException;
import org.lockss.extractor.ArticleMetadata;
import org.lockss.extractor.ArticleMetadataExtractor;
import org.lockss.extractor.MetadataTarget;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.plugin.ArticleFiles;
import org.lockss.plugin.ArticleIteratorFactory;
import org.lockss.plugin.Plugin;
import org.lockss.plugin.PluginManager;
import org.lockss.plugin.PluginTestUtil;
import org.lockss.plugin.SubTreeArticleIterator;
import org.lockss.plugin.simulated.SimulatedArchivalUnit;
import org.lockss.plugin.simulated.SimulatedContentGenerator;
import org.lockss.plugin.simulated.SimulatedDefinablePlugin;
import org.lockss.protocol.IdentityManager;
import org.lockss.test.ConfigurationUtil;
import org.lockss.test.LockssTestCase;
import org.lockss.test.MockLockssDaemon;
import org.lockss.util.*;
import org.lockss.ws.entities.AuWsResult;
import org.lockss.ws.entities.IdNamePair;
import org.lockss.ws.entities.PluginWsResult;
import org.lockss.ws.entities.PlatformConfigurationWsResult;
import org.lockss.ws.entities.RepositorySpaceWsResult;
import org.lockss.ws.entities.RepositoryWsResult;
import org.lockss.ws.entities.DaemonVersionWsResult;
import org.lockss.ws.entities.JavaVersionWsResult;
import org.lockss.ws.entities.PlatformWsResult;

/**
 * Test class for org.lockss.ws.status.DaemonStatusService
 * 
 * @author Fernando Garcia-Loygorri
 */
public class TestDaemonStatusService extends LockssTestCase {
  static Logger log = Logger.getLogger(TestDaemonStatusService.class);
  static String TEST_LOCAL_IP = "127.1.2.3";

  private MockLockssDaemon theDaemon;
  private DaemonStatusServiceImpl service;
  private String tempDirPath;
  private SimulatedArchivalUnit sau0, sau1;

  public void setUp() throws Exception {
    super.setUp();

    tempDirPath = setUpDiskSpace();

    ConfigurationUtil.addFromArgs(IdentityManager.PARAM_LOCAL_IP, TEST_LOCAL_IP);

    theDaemon = getMockLockssDaemon();
    theDaemon.setDaemonInited(true);

    PluginManager pluginManager = theDaemon.getPluginManager();
    pluginManager.startService();

    sau0 = PluginTestUtil.createAndStartSimAu(MySimulatedPlugin0.class,
	simAuConfig(tempDirPath + "0"));

    PluginTestUtil.crawlSimAu(sau0);

    sau1 = PluginTestUtil.createAndStartSimAu(MySimulatedPlugin1.class,
	simAuConfig(tempDirPath + "/1"));

    PluginTestUtil.crawlSimAu(sau1);

    theDaemon.getRemoteApi().startService();

    service = new DaemonStatusServiceImpl();
  }

  /**
   * Runs the tests that verify whether the daemon is ready.
   * 
   * @throws Exception
   */
  public void testIsDaemonReady() throws Exception {
    theDaemon.setAusStarted(true);
    assertTrue(service.isDaemonReady());
    theDaemon.setAusStarted(false);
    assertFalse(service.isDaemonReady());
  }

  /**
   * Runs the test that gets the identifiers of all the AUs.
   * 
   * @throws Exception
   */
  public void testGetAuIds() throws Exception {
    Collection<IdNamePair> auIds = service.getAuIds();
    assertEquals(2, auIds.size());
  }

  public void testGetPlatformConfiguration() throws Exception {
    Properties p = new Properties();
    p.setProperty(ConfigManager.PARAM_PLATFORM_FQDN, "host.foo");
    p.setProperty(ConfigManager.PARAM_PLATFORM_IP_ADDRESS, "2.3.4.1");
    p.setProperty(ConfigManager.PARAM_DAEMON_GROUPS, "foogroup");
    p.setProperty(ConfigManager.PARAM_PLATFORM_PROJECT, "mercury");
    p.setProperty(ConfigManager.PARAM_PLATFORM_LOCAL_V3_IDENTITY,
		  "TCP:[127.0.0.1]:9720");
    p.setProperty(ConfigManager.PARAM_PLATFORM_SMTP_HOST, "smtphost");
    p.setProperty(ConfigManager.PARAM_PLATFORM_SMTP_PORT, "25");
    p.setProperty(ConfigManager.PARAM_PLATFORM_ADMIN_EMAIL, "admin@host");
    p.setProperty(ConfigManager.PARAM_PLATFORM_VERSION, "Linux RPM-1-2");
    ConfigurationUtil.addFromProps(p);

    PlatformConfigurationWsResult platConf = service.getPlatformConfiguration();
    assertEquals("host.foo", platConf.getHostName());
    assertEquals("2.3.4.1", platConf.getIpAddress());
    assertEquals(ListUtil.list("foogroup"), platConf.getGroups());
    assertEquals("mercury", platConf.getProject());
    assertEquals("TCP:[127.0.0.1]:9720", platConf.getV3Identity());
    assertEquals("smtphost:25", platConf.getMailRelay());
    assertEquals("admin@host", platConf.getAdminEmail());
    assertEquals(ListUtil.list(tempDirPath), platConf.getDisks());
    assertTrue("Uptime should be <= current time",
	       platConf.getUptime() <= platConf.getCurrentTime());
    assertNotNull(platConf.getCurrentWorkingDirectory());
    assertEquals(ConfigManager.getConfigManager().getConfigUrlList(),
		 platConf.getProperties());
    assertEquals(BuildInfo.getBuildProperty(BuildInfo.BUILD_HOST),
		 platConf.getBuildHost());

    DaemonVersionWsResult dver = platConf.getDaemonVersion();
    DaemonVersion daemonVersion = ConfigManager.getDaemonVersion();
    assertEquals(daemonVersion.displayString(), dver.getFullVersion());
    assertEquals(daemonVersion.getMajorVersion(), dver.getMajorVersion());
    assertEquals(daemonVersion.getMinorVersion(), dver.getMinorVersion());
    assertEquals(daemonVersion.getBuildVersion(), dver.getBuildVersion());

    JavaVersionWsResult jver = platConf.getJavaVersion();
    // Change this when we support 1.8
    String specVar = jver.getSpecificationVersion();
    assertTrue("Java version expected to be 1.6 or 1.7",
	       ListUtil.list("1.6", "1.7").contains(specVar));
    assertTrue("java version s.b. initial substring of java spec version",
	       jver.getSpecificationVersion().startsWith(specVar));
    assertTrue("java version s.b. initial substring of java runtime version",
	       jver.getRuntimeVersion().startsWith(specVar));

    PlatformWsResult plat = platConf.getPlatform();
    assertEquals("Linux RPM", plat.getName());
    assertEquals("1", plat.getVersion());
    assertEquals("2", plat.getSuffix());
  }

  /**
   * Runs the test that queries plugins.
   * 
   * @throws Exception
   */
  public void testQueryPlugins() throws Exception {
    String pluginIdStart =
	"org.lockss.ws.status.TestDaemonStatusService$MySimulatedPlugin";
    String query = "select *";
    List<PluginWsResult> plugins = service.queryPlugins(query);
    assertEquals(2, plugins.size());
    PluginWsResult plugin = plugins.get(0);
    assertTrue(plugin.getPluginId().startsWith(pluginIdStart));
    assertEquals("Simulated Content", plugin.getName());
    assertEquals("SimulatedVersion", plugin.getVersion());
    assertEquals("Builtin", plugin.getType());
    assertNull(plugin.getRegistry());
    plugin = plugins.get(1);
    assertTrue(plugin.getPluginId().startsWith(pluginIdStart));
    assertEquals("Simulated Content", plugin.getName());
    assertEquals("SimulatedVersion", plugin.getVersion());
    assertEquals("Builtin", plugin.getType());
    assertNull(plugin.getRegistry());

    query = "select pluginId";
    plugins = service.queryPlugins(query);
    assertEquals(2, plugins.size());
    plugin = plugins.get(0);
    assertTrue(plugin.getPluginId().startsWith(pluginIdStart));
    assertNull(plugin.getName());
    assertNull(plugin.getVersion());
    assertNull(plugin.getType());
    assertNull(plugin.getDefinition());
    assertNull(plugin.getRegistry());
    plugin = plugins.get(0);
    assertTrue(plugin.getPluginId().startsWith(pluginIdStart));
    assertNull(plugin.getName());
    assertNull(plugin.getVersion());
    assertNull(plugin.getType());
    assertNull(plugin.getDefinition());
    assertNull(plugin.getRegistry());

    query = "select name";
    plugins = service.queryPlugins(query);
    assertEquals(2, plugins.size());
    plugin = plugins.get(0);
    assertNull(plugin.getPluginId());
    assertEquals("Simulated Content", plugin.getName());
    assertNull(plugin.getVersion());
    assertNull(plugin.getType());
    assertNull(plugin.getDefinition());
    assertNull(plugin.getRegistry());
    plugin = plugins.get(1);
    assertNull(plugin.getPluginId());
    assertEquals("Simulated Content", plugin.getName());
    assertNull(plugin.getVersion());
    assertNull(plugin.getType());
    assertNull(plugin.getDefinition());
    assertNull(plugin.getRegistry());

    query = "select version, type";
    plugins = service.queryPlugins(query);
    assertEquals(2, plugins.size());
    plugin = plugins.get(0);
    assertNull(plugin.getPluginId());
    assertNull(plugin.getName());
    assertEquals("SimulatedVersion", plugin.getVersion());
    assertEquals("Builtin", plugin.getType());
    assertNull(plugin.getDefinition());
    assertNull(plugin.getRegistry());
    plugin = plugins.get(1);
    assertNull(plugin.getPluginId());
    assertNull(plugin.getName());
    assertEquals("SimulatedVersion", plugin.getVersion());
    assertEquals("Builtin", plugin.getType());
    assertNull(plugin.getDefinition());
    assertNull(plugin.getRegistry());

    String pluginIdStart0 =
	"org.lockss.ws.status.TestDaemonStatusService$MySimulatedPlugin0";
    query = "select * where pluginId like '%Plugin0'";
    plugins = service.queryPlugins(query);
    assertEquals(1, plugins.size());
    plugin = plugins.get(0);
    assertEquals(pluginIdStart0, plugin.getPluginId());
    assertEquals("Simulated Content", plugin.getName());
    assertEquals("SimulatedVersion", plugin.getVersion());
    assertEquals("Builtin", plugin.getType());
    assertEquals(1, plugin.getDefinition().size());
    assertNull(plugin.getRegistry());

    String pluginIdStart1 =
	"org.lockss.ws.status.TestDaemonStatusService$MySimulatedPlugin1";
    query = "select * where pluginId like '%Plugin1'";
    plugins = service.queryPlugins(query);
    assertEquals(1, plugins.size());
    plugin = plugins.get(0);
    assertEquals(pluginIdStart1, plugin.getPluginId());
    assertEquals("Simulated Content", plugin.getName());
    assertEquals("SimulatedVersion", plugin.getVersion());
    assertEquals("Builtin", plugin.getType());
    assertEquals(2, plugin.getDefinition().size());
    assertNull(plugin.getRegistry());

    query = "select * where name = 'Real Content'";
    plugins = service.queryPlugins(query);
    assertEquals(0, plugins.size());

    query = "select * where pluginId like '%Plugin0' or name = 'Real Content'";
    plugins = service.queryPlugins(query);
    assertEquals(1, plugins.size());
    plugin = plugins.get(0);
    assertEquals(pluginIdStart0, plugin.getPluginId());
    assertEquals("Simulated Content", plugin.getName());
    assertEquals("SimulatedVersion", plugin.getVersion());
    assertEquals("Builtin", plugin.getType());
    assertEquals(1, plugin.getDefinition().size());
    assertNull(plugin.getRegistry());

    query = "select * where pluginId like '%Plugin0' and name = 'Real Content'";
    plugins = service.queryPlugins(query);
    assertEquals(0, plugins.size());

    query = "select pluginId,definition where type='Builtin' order by pluginId";
    plugins = service.queryPlugins(query);
    assertEquals(2, plugins.size());
    plugin = plugins.get(0);
    assertEquals(pluginIdStart0, plugin.getPluginId());
    assertNull(plugin.getName());
    assertNull(plugin.getVersion());
    assertNull(plugin.getType());
    assertEquals(1, plugin.getDefinition().size());
    assertNull(plugin.getRegistry());
    plugin = plugins.get(1);
    assertEquals(pluginIdStart1, plugin.getPluginId());
    assertNull(plugin.getName());
    assertNull(plugin.getVersion());
    assertNull(plugin.getType());
    assertEquals(2, plugin.getDefinition().size());
    assertNull(plugin.getRegistry());
  }

  /**
   * Runs the test that queries Archival Units.
   * 
   * @throws Exception
   */
  public void testQueryAus() throws Exception {
    String auIdStart =
	"org|lockss|ws|status|TestDaemonStatusService$MySimulatedPlugin";
    String pluginNameStart =
	"org.lockss.ws.status.TestDaemonStatusService$MySimulatedPlugin";
    String query = "select *";
    List<AuWsResult> aus = service.queryAus(query);
    assertEquals(2, aus.size());
    AuWsResult au = aus.get(0);
    assertTrue(au.getAuId().startsWith(auIdStart));
    assertTrue(au.getName().startsWith("Simulated Content: /"));
    assertEquals(au.getName(), au.getVolume());
    assertTrue(au.getPluginName().startsWith(pluginNameStart));
    assertEquals("Subscription", au.getAccessType());
    assertTrue(au.getAvailableFromPublisher());
    assertEquals(1, au.getAuConfiguration().getDefParams().size());
    assertEquals(6, au.getAuConfiguration().getNonDefParams().size());
    assertFalse(au.getIsBulkContent());
    au = aus.get(1);
    assertTrue(au.getAuId().startsWith(auIdStart));
    assertTrue(au.getName().startsWith("Simulated Content: /"));
    assertEquals(au.getName(), au.getVolume());
    assertTrue(au.getPluginName().startsWith(pluginNameStart));
    assertEquals("Subscription", au.getAccessType());
    assertTrue(au.getAvailableFromPublisher());
    assertEquals(1, au.getAuConfiguration().getDefParams().size());
    assertEquals(6, au.getAuConfiguration().getNonDefParams().size());
    assertFalse(au.getIsBulkContent());

    query = "select auId";
    aus = service.queryAus(query);
    assertEquals(2, aus.size());
    au = aus.get(0);
    assertTrue(au.getAuId().startsWith(auIdStart));
    assertNull(au.getName());
    assertNull(au.getVolume());
    assertNull(au.getPluginName());
    assertNull(au.getAccessType());
    assertNull(au.getAvailableFromPublisher());
    assertNull(au.getAuConfiguration());
    assertNull(au.getIsBulkContent());
    au = aus.get(1);
    assertTrue(au.getAuId().startsWith(auIdStart));
    assertNull(au.getName());
    assertNull(au.getVolume());
    assertNull(au.getPluginName());
    assertNull(au.getAccessType());
    assertNull(au.getAvailableFromPublisher());
    assertNull(au.getAuConfiguration());
    assertNull(au.getIsBulkContent());

    query = "select name, accessType, isBulkContent";
    aus = service.queryAus(query);
    assertEquals(2, aus.size());
    au = aus.get(0);
    assertNull(au.getAuId());
    assertTrue(au.getName().startsWith("Simulated Content: /"));
    assertNull(au.getVolume());
    assertNull(au.getPluginName());
    assertEquals("Subscription", au.getAccessType());
    assertNull(au.getAvailableFromPublisher());
    assertNull(au.getAuConfiguration());
    assertFalse(au.getIsBulkContent());
    au = aus.get(1);
    assertNull(au.getAuId());
    assertTrue(au.getName().startsWith("Simulated Content: /"));
    assertNull(au.getVolume());
    assertNull(au.getPluginName());
    assertEquals("Subscription", au.getAccessType());
    assertNull(au.getAvailableFromPublisher());
    assertNull(au.getAuConfiguration());
    assertFalse(au.getIsBulkContent());

    String auIdStart0 =
	"org|lockss|ws|status|TestDaemonStatusService$MySimulatedPlugin0&root~%2F";
    String pluginName0 =
	"org.lockss.ws.status.TestDaemonStatusService$MySimulatedPlugin0";
    query = "select * where auId like '%Service\\$MySimulatedPlugin0%'";
    aus = service.queryAus(query);
    assertEquals(1, aus.size());
    au = aus.get(0);
    assertTrue(au.getAuId().startsWith(auIdStart0));
    assertTrue(au.getName().startsWith("Simulated Content: /"));
    assertEquals(au.getName(), au.getVolume());
    assertEquals(pluginName0, au.getPluginName());
    assertEquals("Subscription", au.getAccessType());
    assertTrue(au.getAvailableFromPublisher());
    assertEquals(1, au.getAuConfiguration().getDefParams().size());
    assertEquals(6, au.getAuConfiguration().getNonDefParams().size());
    assertFalse(au.getIsBulkContent());

    query = "select * where name like 'Real Content%'";
    aus = service.queryAus(query);
    assertEquals(0, aus.size());

    query = "select * where auId like '%Service\\$MySimulatedPlugin0%' "
	+ "or name like 'Real Content%'";
    aus = service.queryAus(query);
    assertEquals(1, aus.size());
    au = aus.get(0);
    assertTrue(au.getAuId().startsWith(auIdStart0));
    assertTrue(au.getName().startsWith("Simulated Content: /"));
    assertEquals(au.getName(), au.getVolume());
    assertEquals(pluginName0, au.getPluginName());
    assertEquals("Subscription", au.getAccessType());
    assertTrue(au.getAvailableFromPublisher());
    assertEquals(1, au.getAuConfiguration().getDefParams().size());
    assertEquals(6, au.getAuConfiguration().getNonDefParams().size());
    assertFalse(au.getIsBulkContent());

    query = "select * where auId like '%Service\\$MySimulatedPlugin0%' "
	+ "and name like 'Real Content%'";
    aus = service.queryAus(query);
    assertEquals(0, aus.size());

    String auIdStart1 =
	"org|lockss|ws|status|TestDaemonStatusService$MySimulatedPlugin1&root~%2F";
    String pluginName1 =
	"org.lockss.ws.status.TestDaemonStatusService$MySimulatedPlugin1";
    query = "select auId, pluginName where accessType = 'Subscription' "
	+ "order by auId";
    aus = service.queryAus(query);
    assertEquals(2, aus.size());
    au = aus.get(0);
    assertTrue(au.getAuId().startsWith(auIdStart0));
    assertNull(au.getName());
    assertNull(au.getVolume());
    assertEquals(pluginName0, au.getPluginName());
    assertNull(au.getAccessType());
    assertNull(au.getAvailableFromPublisher());
    assertNull(au.getAuConfiguration());
    assertNull(au.getIsBulkContent());
    au = aus.get(1);
    assertTrue(au.getAuId().startsWith(auIdStart1));
    assertNull(au.getName());
    assertNull(au.getVolume());
    assertEquals(pluginName1, au.getPluginName());
    assertNull(au.getAccessType());
    assertNull(au.getAvailableFromPublisher());
    assertNull(au.getAuConfiguration());
    assertNull(au.getIsBulkContent());

    query = "select newContentCrawlUrls where accessType = 'Subscription' "
	+ "order by auId";
    aus = service.queryAus(query);
    assertEquals(2, aus.size());
    au = aus.get(0);
    assertEquals(1, au.getNewContentCrawlUrls().size());
    au = aus.get(1);
    assertEquals(1, au.getNewContentCrawlUrls().size());

    query = "select urlStems where accessType = 'Subscription' order by auId";
    aus = service.queryAus(query);
    assertEquals(2, aus.size());
    au = aus.get(0);
    assertEquals(1, au.getUrlStems().size());
    au = aus.get(1);
    assertEquals(1, au.getUrlStems().size());
  }

  /**
   * Runs the test that queries repository spaces.
   * 
   * @throws Exception
   */
  public void testQueryRepositorySpaces() throws Exception {
    String query = "select *";
    List<RepositorySpaceWsResult> spaces = service.queryRepositorySpaces(query);
    int spaceCount = spaces.size();
    assertTrue(spaceCount > 0);

    RepositorySpaceWsResult result = spaces.get(0);
    assertEquals("local:" + tempDirPath, result.getRepositorySpaceId());
    assertTrue(result.getSize() > 0);
    assertTrue(result.getUsed() > 0);
    assertTrue(result.getFree() > 0);
    assertTrue(result.getPercentageFull() > 0);
    assertTrue(result.getPercentageFull() <= 1);
    assertEquals(2, result.getActiveCount().intValue());
    assertEquals(0, result.getInactiveCount().intValue());
    assertEquals(0, result.getDeletedCount().intValue());
    assertEquals(0, result.getOrphanedCount().intValue());

    query = "select * where size > 1";
    spaces = service.queryRepositorySpaces(query);
    assertEquals(spaceCount, spaces.size());

    query = "select * where used < 1";
    spaces = service.queryRepositorySpaces(query);
    assertEquals(0, spaces.size());

    query = "select * where activeCount = 2";
    spaces = service.queryRepositorySpaces(query);
    assertEquals(spaceCount, spaces.size());

    query = "select * where orphanedCount = 1";
    spaces = service.queryRepositorySpaces(query);
    assertEquals(0, spaces.size());
  }

  /**
   * Runs the test that queries repositories.
   * 
   * @throws Exception
   */
  public void testQueryRepositories() throws Exception {
    String auNameStart = "Simulated Content: " + tempDirPath;
    String pluginNameStart =
	"org.lockss.ws.status.TestDaemonStatusService$MySimulatedPlugin";

    String query = "select *";
    List<RepositoryWsResult> repositories = service.queryRepositories(query);
    assertEquals(2, repositories.size());

    RepositoryWsResult result = repositories.get(0);
    assertEquals("local:" + tempDirPath, result.getRepositorySpaceId());
    assertTrue(result.getDirectoryName().startsWith(tempDirPath));
    assertTrue(result.getAuName().startsWith(auNameStart));
    assertFalse(result.getInternal());
    assertEquals("Active", result.getStatus());
    assertTrue(result.getDiskUsage() > 0);
    assertTrue(result.getPluginName().startsWith(pluginNameStart));

    result = repositories.get(1);
    assertEquals("local:" + tempDirPath, result.getRepositorySpaceId());
    assertTrue(result.getDirectoryName().startsWith(tempDirPath));
    assertTrue(result.getAuName().startsWith(auNameStart));
    assertFalse(result.getInternal());
    assertEquals("Active", result.getStatus());
    assertTrue(result.getDiskUsage() > 0);
    assertTrue(result.getPluginName().startsWith(pluginNameStart));

    String pluginName0 =
	"org.lockss.ws.status.TestDaemonStatusService$MySimulatedPlugin0";
    String pluginName1 =
	"org.lockss.ws.status.TestDaemonStatusService$MySimulatedPlugin1";
    query = "select * where status = 'Active' order by pluginName";
    repositories = service.queryRepositories(query);
    assertEquals(2, repositories.size());

    result = repositories.get(0);
    assertEquals(pluginName0, result.getPluginName());

    result = repositories.get(1);
    assertEquals(pluginName1, result.getPluginName());

    query = "select * where internal = 'true'";
    repositories = service.queryRepositories(query);
    assertEquals(0, repositories.size());
  }

  /**
   * Tests the parsing of the build timestamp.
   * 
   * @throws Exception
   */
  public void testGetBuildTimestamp() throws Exception {
    assertNotNull(service.getBuildTimestamp());
  }

  /**
   * Tests the operation that gets Archival Unit URL subtrees.
   * 
   * @throws Exception
   */
  public void testGetAuUrls() throws Exception {
    List<String> urls = service.getAuUrls(sau0.getAuId(), null);
    assertEquals(21, urls.size());
    urls = service.getAuUrls(sau0.getAuId(), "http://www.example.com/branch1");
    assertEquals(14, urls.size());
    urls = service.getAuUrls(sau1.getAuId(), null);
    assertEquals(21, urls.size());
    urls = service.getAuUrls(sau1.getAuId(),
	"http://www.example.com/branch1/branch1/");
    assertEquals(7, urls.size());
  }

  private Configuration simAuConfig(String rootPath) {
    Configuration conf = ConfigManager.newConfiguration();
    conf.put("root", rootPath);
    conf.put("depth", "2");
    conf.put("branch", "1");
    conf.put("numFiles", "3");
    conf.put("fileTypes", "" + (SimulatedContentGenerator.FILE_TYPE_PDF +
                                SimulatedContentGenerator.FILE_TYPE_HTML));
    conf.put("binFileSize", "7");
    return conf;
  }

  private static class MySubTreeArticleIteratorFactory
      implements ArticleIteratorFactory {
    String pat;
    public MySubTreeArticleIteratorFactory(String pat) {
      this.pat = pat;
    }
    
    /**
     * Create an Iterator that iterates through the AU's articles, pointing
     * to the appropriate CachedUrl of type mimeType for each, or to the
     * plugin's choice of CachedUrl if mimeType is null
     * @param au the ArchivalUnit to iterate through
     * @return the ArticleIterator
     */
    @Override
    public Iterator<ArticleFiles> createArticleIterator(
        ArchivalUnit au, MetadataTarget target) throws PluginException {
      Iterator<ArticleFiles> ret;
      SubTreeArticleIterator.Spec spec = 
        new SubTreeArticleIterator.Spec().setTarget(target);
      
      if (pat != null) {
       spec.setPattern(pat);
      }
      
      ret = new SubTreeArticleIterator(au, spec);
      log.debug(  "creating article iterator for au " + au.getName() 
                    + " hasNext: " + ret.hasNext());
      return ret;
    }
  }

  private static class MySimulatedPlugin extends SimulatedDefinablePlugin {
    ArticleMetadataExtractor simulatedArticleMetadataExtractor = null;
    int version = 2;
    /**
     * Returns the article iterator factory for the mime type, if any
     * @param contentType the content type
     * @return the ArticleIteratorFactory
     */
    @Override
    public ArticleIteratorFactory getArticleIteratorFactory() {
      return new MySubTreeArticleIteratorFactory(null);
    }
    @Override
    public ArticleMetadataExtractor 
      getArticleMetadataExtractor(MetadataTarget target, ArchivalUnit au) {
      return simulatedArticleMetadataExtractor;
    }

    @Override
    public String getFeatureVersion(Plugin.Feature feat) {
      if (Feature.Metadata == feat) {
	return feat + "_" + version;
      } else {
	return null;
      }
    }
  }

  public static class MySimulatedPlugin0 extends MySimulatedPlugin {
    public MySimulatedPlugin0() {
      simulatedArticleMetadataExtractor = new ArticleMetadataExtractor() {
        public void extract(MetadataTarget target, ArticleFiles af,
            Emitter emitter) throws IOException, PluginException {
          ArticleMetadata md = new ArticleMetadata();
          emitter.emitMetadata(af, md);
        }
      };
    }
    public ExternalizableMap getDefinitionMap() {
      ExternalizableMap map = new ExternalizableMap();
      map.putString("au_start_url", "\"%splugin0/%s\", base_url, volume");
      return map;
    }
  }

  public static class MySimulatedPlugin1 extends MySimulatedPlugin {
    public MySimulatedPlugin1() {
      simulatedArticleMetadataExtractor = new ArticleMetadataExtractor() {
        public void extract(MetadataTarget target, ArticleFiles af,
            Emitter emitter) throws IOException, PluginException {
          ArticleMetadata md = new ArticleMetadata();
          emitter.emitMetadata(af, md);
        }
      };
    }
    public ExternalizableMap getDefinitionMap() {
      ExternalizableMap map = new ExternalizableMap();
      map.putString("key1", "value1");
      map.putString("key2", "value2");
      return map;
    }
  }
}
