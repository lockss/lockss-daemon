package org.lockss.plugin.ubiquitypress;

import java.io.File;
import java.net.URL;
import java.util.Properties;
import org.lockss.config.Configuration;
import org.lockss.daemon.*;
import org.lockss.util.*;
import org.lockss.test.*;
import org.lockss.plugin.*;
import org.lockss.plugin.base.BaseCachedUrlSet;
import org.lockss.repository.LockssRepositoryImpl;
import org.lockss.plugin.definable.*;

public class TestUbiquityPressArchivalUnit extends LockssTestCase {
  private MockLockssDaemon theDaemon;

  static final String BASE_URL_KEY = ConfigParamDescr.BASE_URL.getKey();
  static final String JOURNAL_ID_KEY = ConfigParamDescr.JOURNAL_ID.getKey();
  static final String YEAR_KEY = ConfigParamDescr.YEAR.getKey();
  static final String ROOT_URL = "http://www.presentpasts.info/";

  public void setUp() throws Exception {
    super.setUp();
    String tempDirPath = getTempDir().getAbsolutePath() + File.separator;
    Properties props = new Properties();
    props.setProperty(LockssRepositoryImpl.PARAM_CACHE_LOCATION, tempDirPath);
    ConfigurationUtil.setCurrentConfigFromProps(props);

    theDaemon = getMockLockssDaemon();
    theDaemon.getHashService();
  }

  public void tearDown() throws Exception {
    super.tearDown();
  }

  private DefinableArchivalUnit makeAu(URL url, String journal_id,String year)
      throws Exception {
    Properties props = new Properties();
    props.setProperty(JOURNAL_ID_KEY,journal_id);
    props.setProperty(YEAR_KEY,year);
    if (url!=null) {
      props.setProperty(BASE_URL_KEY, url.toString());
    }
    Configuration config = ConfigurationUtil.fromProps(props);
    DefinablePlugin ap = new DefinablePlugin();
    ap.initPlugin(getMockLockssDaemon(),
                      "org.lockss.plugin.ubiquitypress.ClockssUbiquityPressPlugin");
    DefinableArchivalUnit au = (DefinableArchivalUnit)ap.createAu(config);
    return au;
  }

  public void testConstructNullUrl() throws Exception {
    try {
      makeAu(null, "pp","2003");
      fail("Should have thrown ArchivalUnit.ConfigurationException");
    } catch (ArchivalUnit.ConfigurationException e) { }
  }

  public void testShouldCacheProperPages() throws Exception {
    URL base = new URL(ROOT_URL);
    ArchivalUnit pmAu = makeAu(base, "pp","2003");
    theDaemon.getLockssRepository(pmAu);
    theDaemon.getNodeManager(pmAu);
    BaseCachedUrlSet cus = new BaseCachedUrlSet(pmAu,
        new RangeCachedUrlSetSpec(base.toString()));
    // archives page
    shouldCacheTest(ROOT_URL+"/issue/archive.htm", false, pmAu, cus);
    // index page
    shouldCacheTest(ROOT_URL+"index.html", false, pmAu, cus);
    // LOCKSS
    shouldCacheTest("http://lockss.stanford.edu", false, pmAu, cus);
    // other sites
    shouldCacheTest("http://www.dandelionbooks.net/", false, pmAu, cus);
    shouldCacheTest("http://www.sixgallerypress.com/", false, pmAu, cus);
  }

  private void shouldCacheTest(String url, boolean shouldCache,
                               ArchivalUnit au, CachedUrlSet cus) {
    UrlCacher uc = au.makeUrlCacher(url);
    assertEquals(shouldCache, uc.shouldBeCached());
  }

  public void testStartUrlConstruction() throws Exception {
    URL url = new URL(ROOT_URL);

    // 4 digit
    String expected = ROOT_URL+"index.php/pp/gateway/lockss?year=2003";
    DefinableArchivalUnit pmAu = makeAu(url, "pp","2003");
    assertEquals(ListUtil.list(expected), pmAu.getNewContentCrawlUrls());
  }


  public void testGetName() throws Exception {
    DefinableArchivalUnit au = makeAu(new URL(ROOT_URL), "pp","2003");
    assertEquals("Ubiquity Press Plugin (CLOCKSS), Base URL http://www.presentpasts.info/, Journal ID pp, Year 2003", au.getName());
     }

  public static void main(String[] argv) {
    String[] testCaseList = {TestUbiquityPressArchivalUnit.class.getName()};
    junit.swingui.TestRunner.main(testCaseList);
  }

}
