package org.lockss.plugin.archiveit;

import org.lockss.config.Configuration;
import org.lockss.daemon.ConfigParamDescr;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.plugin.definable.DefinableArchivalUnit;
import org.lockss.plugin.definable.DefinablePlugin;
import org.lockss.test.ConfigurationUtil;
import org.lockss.test.LockssTestCase;
import org.lockss.test.MockLockssDaemon;
import org.lockss.test.MockLockssUrlConnection;
import org.lockss.util.Logger;
import org.lockss.util.urlconn.CacheException;

import java.io.IOException;
import java.util.Properties;

public class TestArchiveItApiHttpResponseHandler extends LockssTestCase {

  static final String BASE_URL_KEY = ConfigParamDescr.BASE_URL.getKey();
  static final String COLLECTION_KEY = "collection";

  private static final Logger log = Logger.getLogger(TestArchiveItApiHttpResponseHandler.class);
  private MockLockssDaemon theDaemon;
  private DefinablePlugin plugin;

  static final String PLUGIN_ID = "org.lockss.plugin.archiveit.ArchiveItApiPlugin";

  public void setUp() throws Exception {
    super.setUp();
    setUpDiskSpace();
    theDaemon = getMockLockssDaemon();
    theDaemon.getHashService();
    theDaemon.getRepositoryManager();
    plugin = new DefinablePlugin();
    plugin.initPlugin(theDaemon,
        PLUGIN_ID);
  }

  public void tearDown() throws Exception {
    super.tearDown();
  }

  private DefinableArchivalUnit makeAuFromProps(Properties props)
      throws ArchivalUnit.ConfigurationException {
    Configuration config = ConfigurationUtil.fromProps(props);
    return (DefinableArchivalUnit)plugin.configureAu(config, null);
  }

  private DefinableArchivalUnit makeAu() throws ArchivalUnit.ConfigurationException {
    Properties props = new Properties();
    props.setProperty(BASE_URL_KEY, "https://partner.archive-it.org/");
    props.setProperty(COLLECTION_KEY, "16175");
    props.setProperty("organization", "730");
    return makeAuFromProps(props);
  }

  private CacheException getCacheExceptionForUrlAndCode(String url,
                                                        int responseCode
  ) throws ArchivalUnit.ConfigurationException, IOException {
    DefinableArchivalUnit au = makeAu();
    MockLockssUrlConnection conn = new MockLockssUrlConnection();
    conn.setURL(url);
    CacheException exc = plugin.getCacheResultMap().mapException(au, conn, responseCode, "foo");
    return exc;
  }

  // as the pattern is currently the same for 404 and 504, this test is redundant
  public void testShouldReturn5_30s() throws Exception {
    assertClass(
        CacheException.RetryableNetworkException_5_30S.class,
        getCacheExceptionForUrlAndCode("https://partner.archive-it.org/foo/bar", 500)
    );
  }

}


