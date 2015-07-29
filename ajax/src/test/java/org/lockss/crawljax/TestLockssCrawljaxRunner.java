package org.lockss.crawljax;

import junit.framework.TestCase;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.commons.io.FileUtils;

import java.io.File;

public
class TestLockssCrawljaxRunner extends TestCase {
  File m_crawljaxDir;
  File m_cacheDir;
  DefLockssConfigurationBuilder m_configBuilder;
  String m_testUrl= "http://salt.ece.ubc.ca";
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

/*
  public void testStartWithConfig()
  {
    String[] args = new String[3];
    args[0] = m_testUrl;
    args[1] = m_cacheDirName;
    args[2] = m_configFileName;

    LockssCrawljaxRunner runner = new LockssCrawljaxRunner(args);
    runner.runIfConfigured();

  }
*/

  public void testStartNoConfig() throws Exception {
    String[] args = new String[2];
    args[0] = m_testUrl;
    args[1] = m_cacheDirName;
    LockssCrawljaxRunner runner = new LockssCrawljaxRunner(args);
    //runCrawl(runner.getBuilder());
    System.out.println("-----------------------");
    for (File file : m_cacheDir.listFiles()) {
      System.out.println(file.getName());
    }
    System.out.println("-----------------------");
  }
/*
  public static void runCrawl(final CrawljaxConfigurationBuilder builder) throws
                                                               Exception {
    SimpleSiteCrawl simpleCrawl = new SimpleSiteCrawl() {
      protected CrawljaxConfigurationBuilder newCrawlConfigurationBuilder() {
        return builder;
      }
    };
    simpleCrawl.setup();
    simpleCrawl.crawl();
  }
*/
}