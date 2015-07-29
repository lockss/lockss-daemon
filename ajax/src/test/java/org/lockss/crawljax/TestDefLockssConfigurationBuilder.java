package org.lockss.crawljax;


/*
 * $Id: DefLockssConfigurationBuilder.java,v 1.1 2014/04/14 23:08:24 clairegriffin Exp $
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
import com.crawljax.core.configuration.*;
import com.crawljax.core.configuration.CrawlRules.CrawlRulesBuilder;
import com.crawljax.core.configuration.CrawljaxConfiguration
           .CrawljaxConfigurationBuilder;
import com.crawljax.core.plugin.Plugin;
import com.crawljax.plugins.proxy.WebScarabProxyPlugin;
import com.google.common.collect.ImmutableList;
import junit.framework.TestCase;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.util.concurrent.TimeUnit;

/**
 * Test class for DefLockssConfigurationBuilder
 * Created by claire on 3/18/14.
 */
public class TestDefLockssConfigurationBuilder  extends TestCase {
  File m_crawljaxDir;
  File m_cacheDir;
  DefLockssConfigurationBuilder m_configBuilder;
  String m_testUrl="http://www.example.com";
  String m_configFileName;
  private String m_cacheDirName;
  private PropertiesConfiguration m_defaultConfig =
      DefLockssConfigurationBuilder.defaultConfig();

  public void setUp() throws Exception {
    super.setUp();
    m_crawljaxDir = new File(FileUtils.getTempDirectory(), "crawljax");
    m_cacheDir = new File(m_crawljaxDir, "cache");
    m_cacheDir.mkdirs();
    m_cacheDirName = m_cacheDir.getAbsolutePath();
    m_configBuilder = new DefLockssConfigurationBuilder();
    File config = new File(m_crawljaxDir, "lockss.config");
    m_configFileName = config.getAbsolutePath();

  }

  public void tearDown() throws Exception {
    FileUtils.deleteDirectory(m_crawljaxDir);
    super.tearDown();
  }

  public void testConfigure() throws Exception{
    CrawljaxConfigurationBuilder builder;
    CrawljaxConfiguration crawljaxConfig;
    int maxStates = 100;
    int depth = 1;
    long runtime = 10;
    String browser = "chrome";
    createMockConfig(m_configFileName,maxStates, depth, runtime, browser,
                     DefLockssConfigurationBuilder.PROXY_PARAM_DEFAULT);

    // test configure with all data
    builder = m_configBuilder.configure(m_testUrl, m_cacheDirName,
                                        m_configFileName);
    crawljaxConfig = builder.build();

    assertEquals(maxStates, crawljaxConfig.getMaximumStates());
    assertEquals(depth, crawljaxConfig.getMaximumDepth());
    assertEquals(runtime * 60000, crawljaxConfig.getMaximumRuntime());
    assertEquals(browser.toUpperCase(),
                 crawljaxConfig.getBrowserConfig().getBrowsertype().name());

    // test null configfile - valid input
    builder = m_configBuilder.configure(m_testUrl, m_cacheDirName, null);
    crawljaxConfig = builder.build();

    assertEquals(DefLockssConfigurationBuilder.MAX_STATES_DEFAULT,
                 crawljaxConfig.getMaximumStates());
    assertEquals(DefLockssConfigurationBuilder.DEPTH_DEFAULT,
                 crawljaxConfig.getMaximumDepth());
    assertEquals(DefLockssConfigurationBuilder.TIMEOUT_DEFAULT * 60000,
                 crawljaxConfig.getMaximumRuntime());
    assertEquals(DefLockssConfigurationBuilder.BROWSER_DEFAULT,
                 crawljaxConfig.getBrowserConfig().getBrowsertype());

    // test null input.
    try {
      builder = m_configBuilder.configure(null, m_cacheDirName,
                                          m_configFileName);
      crawljaxConfig = builder.build();
      fail("null url should throw null pointer exception");
    }
    catch(NullPointerException npe) {

    }

    // test null cache dir
    try {
      builder = m_configBuilder.configure(m_testUrl, null, m_configFileName);
      crawljaxConfig = builder.build();
      fail("null output should throw null pointer exception");
    }
    catch(NullPointerException npe) {

    }
  }

  public void testConfigureCrawlRules() throws Exception {
    CrawljaxConfigurationBuilder builder =
        CrawljaxConfiguration.builderFor(m_testUrl);
    m_configBuilder.setConfig(m_defaultConfig);
    m_configBuilder.configureCrawlRules(builder);
    CrawljaxConfiguration cconfig = builder.build();
    CrawlRules rules = cconfig.getCrawlRules();
    assertEquals(DefLockssConfigurationBuilder.CRAWL_HIDDEN_DEFAULT,
                 rules.isCrawlHiddenAnchors());
    assertEquals(DefLockssConfigurationBuilder.CLICK_ONCE_DEFAULT,
                 rules.isClickOnce());
    assertEquals(DefLockssConfigurationBuilder.CRAWL_FRAMES_DEFAULT,
                 rules.shouldCrawlFrames());
    assertEquals(DefLockssConfigurationBuilder.INSERT_RANDOM_DATA_DEFAULT,
                 rules.isRandomInputInForms());
    assertEquals(DefLockssConfigurationBuilder.TIMEOUT_DEFAULT * 60000,
                 cconfig.getMaximumRuntime());
    assertEquals(DefLockssConfigurationBuilder.WAIT_AFTER_EVENT_DEFAULT,
                 rules.getWaitAfterEvent());
    assertEquals(DefLockssConfigurationBuilder.WAIT_AFTER_RELOAD_DEFAULT,
                 rules.getWaitAfterReloadUrl());
    // reassign the defaults
    PropertiesConfiguration config = new PropertiesConfiguration();

    config.setProperty(DefLockssConfigurationBuilder.CRAWL_HIDDEN_PARAM,
                  !DefLockssConfigurationBuilder.CRAWL_HIDDEN_DEFAULT);
    config.setProperty(DefLockssConfigurationBuilder.CLICK_ONCE_PARAM,
                  !DefLockssConfigurationBuilder.CLICK_ONCE_DEFAULT);
    config.setProperty(DefLockssConfigurationBuilder.TIMEOUT_PARAM,
                  2 * DefLockssConfigurationBuilder.TIMEOUT_DEFAULT);
    config.setProperty(DefLockssConfigurationBuilder.WAIT_AFTER_RELOAD_PARAM,
                  2 * DefLockssConfigurationBuilder.WAIT_AFTER_RELOAD_DEFAULT);
    config.setProperty(DefLockssConfigurationBuilder.WAIT_AFTER_EVENT_PARAM,
                  2 * DefLockssConfigurationBuilder.WAIT_AFTER_EVENT_DEFAULT);
    config.setProperty(DefLockssConfigurationBuilder.CRAWL_FRAMES_PARAM,
                  !DefLockssConfigurationBuilder.CRAWL_FRAMES_DEFAULT);
    config.setProperty(DefLockssConfigurationBuilder.INSERT_RANDOM_DATA_PARAM,
                  !DefLockssConfigurationBuilder.INSERT_RANDOM_DATA_DEFAULT);
    m_configBuilder.setConfig(config);
    m_configBuilder.configureCrawlRules(builder);
    cconfig = builder.build();
    rules = cconfig.getCrawlRules();
    assertEquals(!DefLockssConfigurationBuilder.CRAWL_HIDDEN_DEFAULT,
                 rules.isCrawlHiddenAnchors());
    assertEquals(!DefLockssConfigurationBuilder.CLICK_ONCE_DEFAULT,
                 rules.isClickOnce());
    assertEquals(!DefLockssConfigurationBuilder.CRAWL_FRAMES_DEFAULT,
                 rules.shouldCrawlFrames());
    assertEquals(!DefLockssConfigurationBuilder.INSERT_RANDOM_DATA_DEFAULT,
                 rules.isRandomInputInForms());
    assertEquals(DefLockssConfigurationBuilder.TIMEOUT_DEFAULT * 60000 * 2,
                 cconfig.getMaximumRuntime());
    assertEquals(DefLockssConfigurationBuilder.WAIT_AFTER_EVENT_DEFAULT * 2,
                 rules.getWaitAfterEvent());
    assertEquals(DefLockssConfigurationBuilder.WAIT_AFTER_RELOAD_DEFAULT *2,
                 rules.getWaitAfterReloadUrl());
 }
  public void testConfigureCrawlClicks() throws Exception {
    CrawljaxConfigurationBuilder builder =
        CrawljaxConfiguration.builderFor(m_testUrl);
    m_configBuilder.setConfig(m_defaultConfig);
    m_configBuilder.configureCrawlClicks(builder);
    CrawljaxConfiguration cconfig = builder.build();
    CrawlRules rules = cconfig.getCrawlRules();
    ImmutableList<CrawlElement> included =
        rules.getPreCrawlConfig().getIncludedElements();
    assertEquals(1, included.size());
    ImmutableList<CrawlElement> excluded =
        rules.getPreCrawlConfig().getExcludedElements();
    assertEquals(0, excluded.size());

    // modify the rules by changing the basic config
    builder = CrawljaxConfiguration.builderFor(m_testUrl);
    PropertiesConfiguration config = new PropertiesConfiguration();
    config.setProperty(DefLockssConfigurationBuilder.CLICK_PARAM,
                       "A");
    config.setProperty(DefLockssConfigurationBuilder.DONT_CLICK_PARAM,
                       "OPTION");
    config.setProperty(DefLockssConfigurationBuilder
                           .DONT_CLICK_CHILDREN_PARAM,
                       "FORM");
    m_configBuilder.setConfig(config);
    m_configBuilder.configureCrawlClicks(builder);
    cconfig = builder.build();
    rules = cconfig.getCrawlRules();
    included = rules.getPreCrawlConfig().getIncludedElements();
    assertEquals(1, included.size());
    excluded = rules.getPreCrawlConfig().getExcludedElements();
    assertEquals(2, excluded.size());

  }

  public void testConfigurePlugins() throws Exception {
    CrawljaxConfigurationBuilder builder =
        CrawljaxConfiguration.builderFor(m_testUrl);
    m_configBuilder.setConfig(m_defaultConfig);
    // default config does not have any plugins
    assertFalse(m_configBuilder.configurePlugins(builder));

    // add a plugin
  }

  public void testInstallScarabProxyPlugin() throws Exception {
    CrawljaxConfigurationBuilder builder =
        CrawljaxConfiguration.builderFor(m_testUrl);
    m_configBuilder.setConfig(m_defaultConfig);
    m_configBuilder.setOutDir(m_cacheDirName);
    m_configBuilder.installScarabProxyPlugin(builder);
    CrawljaxConfiguration cconfig = builder.build();
    // test the scarab proxy has been installed
    ProxyConfiguration proxyConfig =
        cconfig.getProxyConfiguration();
    assertEquals(DefLockssConfigurationBuilder.SCARAB_PROXY_ADDR_DEFAULT,
                 proxyConfig.getHostname());
    assertEquals(DefLockssConfigurationBuilder.SCARAB_PROXY_PORT_DEFAULT,
                 proxyConfig.getPort());
    // test the Scarab proxy and output plugin as been installed
    ImmutableList<Plugin> plugins = cconfig.getPlugins();
    assertEquals(2, plugins.size());
    assertTrue(plugins.get(0) instanceof WebScarabProxyPlugin);
    assertTrue(plugins.get(1) instanceof WebScarabOutput);

  }

  public void testInstallLapProxyPlugin() throws Exception {
    CrawljaxConfigurationBuilder builder =
        CrawljaxConfiguration.builderFor(m_testUrl);
    m_configBuilder.setOutDir(m_cacheDirName);
    m_configBuilder.setConfig(m_defaultConfig);
    m_configBuilder.installWARCProxyPlugin(builder);
    CrawljaxConfiguration cconfig = builder.build();
    // test the scarab proxy has been installed
    ProxyConfiguration proxyConfig =
        cconfig.getProxyConfiguration();
    assertEquals(DefLockssConfigurationBuilder.WARC_PROXY_HOST_DEFAULT,
                 proxyConfig.getHostname());
    assertEquals(DefLockssConfigurationBuilder.WARC_PROXY_WEB_PORT_DEFAULT,
                 proxyConfig.getPort());
    // test the LapWarcOutput plugin as been installed
    ImmutableList<Plugin> plugins = cconfig.getPlugins();
    //assertEquals(1, plugins.size());
    //assertTrue(plugins.get(0) instanceof LapWarcOutput);
  }

  public void testAvailableBrowsers() throws Exception {
    String expected = "FIREFOX,INTERNET_EXPLORER,CHROME,REMOTE,PHANTOMJS";
    String actual = m_configBuilder.availableBrowsers();
    assertEquals(expected, actual);
  }

  private void createMockConfig(String fileName,int maxStates, int depth,
                                long runtime, String browser, String proxy)
                                throws ConfigurationException {
    PropertiesConfiguration config = new PropertiesConfiguration();
    config.setProperty(DefLockssConfigurationBuilder.MAX_STATES_PARAM , maxStates);
    config.setProperty(DefLockssConfigurationBuilder.DEPTH_PARAM, depth);
    config.setProperty(DefLockssConfigurationBuilder.TIMEOUT_PARAM, runtime);
    config.setProperty(DefLockssConfigurationBuilder.BROWSER_PARAM, browser);
    config.setProperty(DefLockssConfigurationBuilder.PROXY_PARAM, proxy);
    config.save(fileName);
  }
}
