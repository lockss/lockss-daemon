/*
 * $Id$
 */

/*

Copyright (c) 2000-2016 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.plugin.hindawi;

import java.net.MalformedURLException;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.lockss.config.Configuration;
import org.lockss.daemon.*;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.plugin.PluginTestUtil;
import org.lockss.plugin.ArchivalUnit.ConfigurationException;
import org.lockss.plugin.definable.*;
import org.lockss.test.*;
import org.lockss.util.ListUtil;
import org.lockss.util.PatternFloatMap;
import org.lockss.util.RegexpUtil;
import org.lockss.util.urlconn.*;
import org.lockss.util.urlconn.CacheException.*;

public class TestHindawiPublishingCorporationPlugin extends LockssTestCase {
  
  private final String PLUGIN_NAME = "org.lockss.plugin.hindawi.HindawiPublishingCorporationPlugin";

  static final String BASE_URL_KEY = ConfigParamDescr.BASE_URL.getKey();
  static final String DOWNLOAD_URL_KEY = "download_url";
  static final String JOURNAL_ID_KEY = ConfigParamDescr.JOURNAL_ID.getKey();
  static final String VOLUME_NAME_KEY = ConfigParamDescr.VOLUME_NAME.getKey();

  private final String BASE_URL = "http://www.example.com/";
  private final String DOWNLOAD_URL = "http://download.example.com/";
  private final String VOLUME_NAME = "2008";
  private final String JOURNAL_ID = "jid";

  private final Configuration AU_CONFIG =
      ConfigurationUtil.fromArgs(BASE_URL_KEY, BASE_URL,
                                 DOWNLOAD_URL_KEY, DOWNLOAD_URL,
                                 VOLUME_NAME_KEY, VOLUME_NAME,
                                 JOURNAL_ID_KEY, JOURNAL_ID);

  // from au_url_poll_result_weight in plugins/src/org/lockss/plugin/highwire/HighWirePressH20Plugin.xml
  // if it changes in the plugin, you will likely need to change the test, so verify
  static final String REPAIR_FROM_PEER_REGEXP[] = 
    {
            ".+[.](bmp|css|eot|gif|ico|jpe?g|js|otf|png|svg|tif?f|ttf|woff.?)(\\?.*)?$"
    };

  private DefinablePlugin plugin;
  private ArchivalUnit au;

  public TestHindawiPublishingCorporationPlugin(String msg) {
    super(msg);
  }

  public void setUp() throws Exception {
    super.setUp();
    plugin = new DefinablePlugin();
    plugin.initPlugin(getMockLockssDaemon(), PLUGIN_NAME);
  }

  public void testGetAuNullConfig()
      throws ArchivalUnit.ConfigurationException {
    try {
      plugin.configureAu(null, null);
      fail("Didn't throw ArchivalUnit.ConfigurationException");
    } catch (ArchivalUnit.ConfigurationException e) {
    }
  }
  public void testCreateAu() throws ConfigurationException {
    plugin.configureAu(AU_CONFIG, null);
  }

  public void testGetAuConstructsProperAu()
      throws ArchivalUnit.ConfigurationException, MalformedURLException {
    String starturl =
        BASE_URL + "journals/" + JOURNAL_ID + "/" + VOLUME_NAME + "/";
    au = (DefinableArchivalUnit)plugin.configureAu(AU_CONFIG, null);
    assertEquals("Hindawi Plugin (Legacy), Base URL " + BASE_URL +
        ", Download URL " + DOWNLOAD_URL +
        ", Journal ID " + JOURNAL_ID +
        ", Volume " + VOLUME_NAME, au.getName());
    assertEquals(ListUtil.list(starturl), au.getStartUrls());
  }

  public void testGetPluginId() {
    assertEquals(PLUGIN_NAME, plugin.getPluginId());
  }

  public void testHandles403Result() throws Exception {
    CacheException exc =
      ( (HttpResultMap) plugin.getCacheResultMap()).mapException(null, "",
                                                                 403, "foo");
    assertEquals(NoRetryDeadLinkException.class, exc.getClass() );
  }

  public void testHandles400Result() throws Exception {
    CacheException exc =
      ((HttpResultMap)plugin.getCacheResultMap()).mapException(au, "",
                                                               400, "foo");
    assertClass(NoRetryDeadLinkException.class, exc);
  }
  
  public void testPollSpecial() throws Exception {
    String ROOT_URL = "http://www.example.com/";
    
    au = (DefinableArchivalUnit)plugin.configureAu(AU_CONFIG, null);
    
    // if it changes in the plugin, you will likely need to change the test, so verify
    assertEquals(Arrays.asList(
        REPAIR_FROM_PEER_REGEXP),
        RegexpUtil.regexpCollection(au.makeRepairFromPeerIfMissingUrlPatterns()));
    
    // make sure that's the regexp that will match to the expected url string
    // this also tests the regexp (which is the same) for the weighted poll map
    List <String> repairList = ListUtil.list(
        ROOT_URL + "css/9.2.6/print.css",
        ROOT_URL + "css/9.3.2/lib/functional.min.css",
        ROOT_URL + "css/9.3.2/lib/min.css?ver=123");
    
    Pattern p0 = Pattern.compile(REPAIR_FROM_PEER_REGEXP[0]);
    Matcher m0;
    for (String urlString : repairList) {
      m0 = p0.matcher(urlString);
      assertEquals(urlString, true, m0.find());
    }
    //and this one should success - it needs to be weighted correctly and repaired from publisher if possible
    String notString = ROOT_URL + "img/close-icon.png";
    m0 = p0.matcher(notString);
    assertEquals(true, m0.find());
    
    PatternFloatMap urlPollResults = au.makeUrlPollResultWeightMap();
    assertNotNull(urlPollResults);
    for (String urlString : repairList) {
      assertEquals(0.0, urlPollResults.getMatch(urlString, (float) 1), .0001);
    }
    assertEquals(0.0, urlPollResults.getMatch(notString, (float) 1), .0001);
  }

  public void testPollAndRepair() throws Exception {


    Properties props = new Properties();
    props.setProperty(BASE_URL_KEY, "http://www.example.com");
    props.setProperty(JOURNAL_ID_KEY, "journal_id");
    props.setProperty(DOWNLOAD_URL_KEY, "http://downloads.example.com");
    props.setProperty(VOLUME_NAME_KEY, "2003");

    Configuration config = ConfigurationUtil.fromProps(props);
    DefinableArchivalUnit au = (DefinableArchivalUnit)plugin.createAu(config);

    List <String> repaireList = ListUtil.list(
            "http://www.hindawi.com/images/arr_select.gif",
            "http://www.hindawi.com/images/arr_select.svg",
            "http://www.hindawi.com/images/arr_select2.svg",
            "http://www.hindawi.com/images/reprint.svg",
            "http://www.hindawi.com/scripts/jquery.magnific-popup-initializer.js",
            "http://www.hindawi.com/images/apple-touch-icon-precomposed.png",
            "http://www.hindawi.com/images/why_publish.jpg",
            "http://www.hindawi.com/images/warning_1.gif",
            "http://www.hindawi.com/scripts/menu.js?ver=2",
            "http://www.hindawi.com/images/office.png",
            "http://www.hindawi.com/images/apple-touch-icon-144x144.png",
            "http://www.hindawi.com/images/apple-touch-icon-precomposed.png",
            "http://www.hindawi.com/scripts/jquery.magnific-popup.js",
            "http://www.hindawi.com/images/arr_select2.svg",
            "http://www.hindawi.com/css/site.css",
            "http://www.hindawi.com/static/page.css",
            "http://www.hindawi.com/images/apple-touch-icon-precomposed-2312312.png",
            "http://www.hindawi.com/scripts/jquery.magnific-popup-231231231231.js",
            "http://www.hindawi.com/images/arr_select2-3241324343.svg",
            "http://www.hindawi.com/css/site-231231312.css"

    );

    Pattern p0 = Pattern.compile(REPAIR_FROM_PEER_REGEXP[0]);

    Matcher m0;

    for (String urlString : repaireList) {
      m0 = p0.matcher(urlString);

      assertEquals(urlString, true, m0.find());
    }

    // Failed case
    List<String>  wrongStringList = ListUtil.list(
            "http://www.hindawi.com/journals/ahci/2017/6787504/reprint",
            "http://www.hindawi.com/journals/ahci/2017/8962762/cta",
            "http://www.hindawi.com/journals/ahci/2017/6131575/ref",
            "http://www.hindawi.com/journals/ahci/2017/7219098/abs",
            "http://downloads.hindawi.com/journals/acisc/2017/5680398.epub",
            "http://www.hindawi.com/journals/acisc/2017/3481709",
            "http://downloads.hindawi.com/journals/acisc/2017/5680398.pdf",
            "http://www.hindawi.com/images/warning_1.gif2",
            "http://www.hindawi.com/scripts/jquery.magnific-popup-initializer.ts",
            "http://www.hindawi.com/scripts/jquery.magnific-popup-initializer.less",
            "http://www.hindawi.com/images/apple-touch-icon-precomposed-2312312.p2g"
    );

    for (String urlString : wrongStringList) {
      m0 = p0.matcher(urlString);

      assertEquals(urlString, false, m0.find());
    }

    PatternFloatMap urlPollResults = au.makeUrlPollResultWeightMap();

    assertNotNull(urlPollResults);
  }
  
}