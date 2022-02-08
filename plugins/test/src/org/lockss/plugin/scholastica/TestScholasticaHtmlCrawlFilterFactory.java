package org.lockss.plugin.scholastica;

import org.lockss.config.ConfigManager;
import org.lockss.config.Tdb;
import org.lockss.config.TdbAu;
import org.lockss.daemon.ConfigParamDescr;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.plugin.PluginTestUtil;
import org.lockss.test.*;
import org.lockss.util.Constants;
import org.lockss.util.Logger;
import org.lockss.util.StringUtil;
import org.lockss.util.TypedEntryMap;

import java.io.InputStream;

public class TestScholasticaHtmlCrawlFilterFactory extends LockssTestCase {

  static Logger log = Logger.getLogger(TestScholasticaHtmlCrawlFilterFactory.class);

  private ScholasticaHtmlCrawlFilterFactory fact;
  private MockLockssDaemon theDaemon;
  private ArchivalUnit bau;
  static final String BASE_URL_KEY = ConfigParamDescr.BASE_URL.getKey();
  private static String BASE_URL = "https://onco-hema.healthbooktimes.org/";

  public void setUp() throws Exception {
    super.setUp();
    setUpDiskSpace(); // you need this to have startService work properly...

    theDaemon = getMockLockssDaemon();
    theDaemon.getAlertManager();
    theDaemon.getPluginManager().setLoadablePluginsReady(true);
    theDaemon.setDaemonInited(true);
    theDaemon.getPluginManager().startService();
    theDaemon.getCrawlManager();

    ConfigurationUtil.addFromUrl(getResource("test_scholastica.xml"));
    Tdb tdb = ConfigManager.getCurrentConfig().getTdb();

    TdbAu tdbau = tdb.getTdbAusLikeName("").get(0);
    bau = PluginTestUtil.createAndStartAu(tdbau);
    assertNotNull(bau);
    TypedEntryMap auConfig =  bau.getProperties();
    assertEquals(BASE_URL, auConfig.getString(BASE_URL_KEY));
    fact = new ScholasticaHtmlCrawlFilterFactory();
  }

  public void tearDown() throws Exception {
    theDaemon.stopDaemon();
    super.tearDown();
  }

  public String getFileAsString(String fName) throws Exception {
    InputStream actIn = fact.createFilteredInputStream(
        bau,
        getResourceAsStream(fName),
        Constants.ENCODING_UTF_8);
    String fStr = StringUtil.fromInputStream(actIn);
    return fStr;
  }

  public String filterString(String string) throws Exception {
    InputStream actIn = fact.createFilteredInputStream(
        bau,
        new StringInputStream(string),
        Constants.DEFAULT_ENCODING);
    String fStr = StringUtil.fromInputStream(actIn);
    return fStr;
  }


  public void testIortArticleOldNew() throws Exception {
    String art1 = getFileAsString("issues_toc.html");
    String art2 = getFileAsString("issues_toc_2020.html");
    log.info(art1);
    assertEquals(art1, art2);

  }

}
