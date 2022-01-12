package org.lockss.plugin.interresearch;

import org.lockss.config.Configuration;
import org.lockss.daemon.ConfigParamDescr;
import org.lockss.plugin.UrlNormalizer;
import org.lockss.test.ConfigurationUtil;
import org.lockss.test.LockssTestCase;
import org.lockss.test.MockArchivalUnit;

import java.util.Properties;

public class TestInterResearchUrlNormalizer extends LockssTestCase {

  static final String BASE_URL_KEY = ConfigParamDescr.BASE_URL.getKey();
  static final String VOL_KEY = ConfigParamDescr.VOLUME_NAME.getKey();
  static final String JID_KEY = ConfigParamDescr.JOURNAL_ID.getKey();
  private MockArchivalUnit m_mau;


  @Override
  public void setUp() throws Exception {
    super.setUp();
    Properties props = new Properties();
    props.setProperty(VOL_KEY, "3");
    props.setProperty(BASE_URL_KEY, "http://www.int-res.com/");
    props.setProperty(JID_KEY, "foo");

    Configuration config = ConfigurationUtil.fromProps(props);
    m_mau = new MockArchivalUnit();
    m_mau.setConfiguration(config);
  }

  public void testUrlNormalizer() throws Exception {
    UrlNormalizer normalizer = new InterResearchUrlNormalizer();

    assertEquals("https://www.int-res.com/fileadmin/templates/dist/vendor/unify/plugins/font-awesome-5p/css/all.css",
        normalizer.normalizeUrl("https://www.int-res.com/fileadmin/templates/dist/vendor/unify/plugins/font-awesome-5p/css/all.css?13924234", m_mau));

    assertEquals("https://www.int-res.com/fileadmin/templates/dist/vendor/unify/css/headers/header-default.css"	,
        normalizer.normalizeUrl("https://www.int-res.com/fileadmin/templates/dist/vendor/unify/css/headers/header-default.css?13924234"	, m_mau));

    assertEquals("https://www.int-res.com/typo3temp/javascript_dd82474708.js"	,
        normalizer.normalizeUrl("https://www.int-res.com/typo3temp/javascript_dd82474708.js?13924234"	, m_mau));
  }
}
