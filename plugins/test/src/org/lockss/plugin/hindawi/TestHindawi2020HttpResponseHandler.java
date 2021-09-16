package org.lockss.plugin.hindawi;

import org.lockss.config.Configuration;
import org.lockss.daemon.ConfigParamDescr;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.plugin.ajax.AjaxRequestResponse;
import org.lockss.plugin.definable.DefinableArchivalUnit;
import org.lockss.plugin.definable.DefinablePlugin;
import org.lockss.test.ConfigurationUtil;
import org.lockss.test.LockssTestCase;
import org.lockss.test.MockLockssDaemon;
import org.lockss.test.MockLockssUrlConnection;
import org.lockss.util.Logger;
import org.lockss.util.urlconn.CacheException;
import org.lockss.util.urlconn.HttpResultMap;

import java.util.Properties;

public class TestHindawi2020HttpResponseHandler extends LockssTestCase {

  static final String BASE_URL_KEY = ConfigParamDescr.BASE_URL.getKey();
  static final String DOWNLOAD_URL_KEY = "download_url";
  static final String JID_KEY = ConfigParamDescr.JOURNAL_ID.getKey();
  static final String YEAR_KEY = ConfigParamDescr.YEAR.getKey();

  private static final Logger log = Logger.getLogger(TestHindawi2020HttpResponseHandler.class);

  static final String PLUGIN_ID = "org.lockss.plugin.hindawi.HindawiPlugin";
  static final String ROOT_URL = "https://www.hindawi.com/";
  static final String DOWNLOAD_URL = "http://downloads.hindawi.com/lockss.html";
  private MockLockssDaemon theDaemon;
  private DefinablePlugin plugin;

  public void setUp() throws Exception {
    super.setUp();
    setUpDiskSpace();
    theDaemon = getMockLockssDaemon();
    theDaemon.getHashService();
    theDaemon.getRepositoryManager();
    plugin = new DefinablePlugin();
    plugin.initPlugin(theDaemon, PLUGIN_ID);
  }

  public void tearDown() throws Exception {
    super.tearDown();
  }

  private DefinableArchivalUnit makeAuFromProps(Properties props)
      throws ArchivalUnit.ConfigurationException {
    Configuration config = ConfigurationUtil.fromProps(props);
    return (DefinableArchivalUnit)plugin.configureAu(config, null);
  }

  public void testHandlesExceptionResult() throws Exception {
    Properties props = new Properties();
    props.setProperty(BASE_URL_KEY, ROOT_URL);
    props.setProperty(DOWNLOAD_URL_KEY, DOWNLOAD_URL);
    props.setProperty(YEAR_KEY, "2013");
    props.setProperty(JID_KEY, "ppar");
    DefinableArchivalUnit au = makeAuFromProps(props);
    String data_url = "https://www.hindawi.com/journals/ppar/2013/612971/'data:image/svg+xml";
    String non_data_url = "https://www.hindawi.com/journals/ppar/2013/612971/foo.bar";
    MockLockssUrlConnection conn = new MockLockssUrlConnection();

    conn.setURL("http://www.hindawi.com/");
    CacheException exc =
        ((HttpResultMap)plugin.getCacheResultMap()).mapException(au, conn,
            new javax.net.ssl.SSLHandshakeException("BAD"), "foo");
    // make sure it's a retryable
    assertClass(CacheException.RetryableException.class, exc);

    conn.setURL(data_url);
    log.info(" " + data_url.contains("'data:"));
    exc = ((HttpResultMap)plugin.getCacheResultMap()).mapException(au, conn, 500, "foo");
    assertTrue(exc instanceof CacheException.NoRetryDeadLinkException);

    conn.setURL(non_data_url);
    exc = ((HttpResultMap)plugin.getCacheResultMap()).mapException(au, conn, 500, "foo");
    assertTrue(exc instanceof CacheException.RetrySameUrlException);

  }

}
