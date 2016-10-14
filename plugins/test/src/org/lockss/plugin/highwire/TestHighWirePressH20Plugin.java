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

package org.lockss.plugin.highwire;

import java.net.MalformedURLException;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.lockss.config.Configuration;
import org.lockss.daemon.*;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.plugin.ArchivalUnit.ConfigurationException;
import org.lockss.plugin.definable.*;
import org.lockss.test.*;
import org.lockss.util.ListUtil;
import org.lockss.util.PatternFloatMap;
import org.lockss.util.RegexpUtil;
import org.lockss.util.urlconn.*;
import org.lockss.util.urlconn.CacheException.*;

public class TestHighWirePressH20Plugin extends LockssTestCase {

  static final String BASE_URL_KEY = ConfigParamDescr.BASE_URL.getKey();
  static final String VOL_KEY = ConfigParamDescr.VOLUME_NAME.getKey();
  
  // from au_url_poll_result_weight in plugins/src/org/lockss/plugin/highwire/HighWirePressH20Plugin.xml
  // if it changes in the plugin, you will likely need to change the test, so verify
  static final String HW_REPAIR_FROM_PEER_REGEXP[] = 
    {
        "[.](css|js)$",
        "://[^/]+(?!.*/content/)(/[^/]+)+[.](gif|png)$",
        "://[^/]+(/shared/img/).*[.](gif|png)$"
    };

  private DefinablePlugin plugin;

  public TestHighWirePressH20Plugin(String msg) {
    super(msg);
  }

  public void setUp() throws Exception {
    super.setUp();
    plugin = new DefinablePlugin();
    plugin.initPlugin(getMockLockssDaemon(),
                      "org.lockss.plugin.highwire.HighWirePressH20Plugin");
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
    Properties props = new Properties();
    props.setProperty(BASE_URL_KEY, "http://www.example.com/");
    props.setProperty(VOL_KEY, "32");
    makeAuFromProps(props);
  }

  private DefinableArchivalUnit makeAuFromProps(Properties props)
      throws ArchivalUnit.ConfigurationException {
    Configuration config = ConfigurationUtil.fromProps(props);
    return (DefinableArchivalUnit)plugin.configureAu(config, null);
  }

  public void testGetAuConstructsProperAu()
      throws ArchivalUnit.ConfigurationException, MalformedURLException {
    Properties props = new Properties();
    props.setProperty(VOL_KEY, "322");
    props.setProperty(BASE_URL_KEY, "http://www.example.com/");

    String starturl =
      "http://www.example.com/lockss-manifest/vol_322_manifest.dtl";
    DefinableArchivalUnit au = makeAuFromProps(props);
    assertEquals("HighWire Press Plugin (H20), Base URL http://www.example.com/, Volume 322", au.getName());
    assertEquals(ListUtil.list(starturl), au.getStartUrls());
  }

  public void testGetPluginId() {
    assertEquals("org.lockss.plugin.highwire.HighWirePressH20Plugin",
                 plugin.getPluginId());
  }

  public void testGetAuConfigProperties() {
    assertEquals(ListUtil.list(ConfigParamDescr.BASE_URL,
                               ConfigParamDescr.VOLUME_NAME),
                 plugin.getLocalAuConfigDescrs());
  }

  public void testHandles404Result() throws Exception {
    CacheException exc =
      ( (HttpResultMap) plugin.getCacheResultMap()).mapException(null, "",
                                                                 404, "foo");
    assertEquals(NoRetryDeadLinkException.class, exc.getClass() );

  }

  public void testHandles500Result() throws Exception {
    Properties props = new Properties();
    props.setProperty(VOL_KEY, "322");
    props.setProperty(BASE_URL_KEY, "http://www.example.com/");

    String starturl =
      "http://www.example.com/lockss-manifest/vol_322_manifest.dtl";
    DefinableArchivalUnit au = makeAuFromProps(props);
    MockLockssUrlConnection conn = new MockLockssUrlConnection();
    conn.setURL("http://uuu17/");
    CacheException exc =
      ((HttpResultMap)plugin.getCacheResultMap()).mapException(au, conn,
                                                               500, "foo");
    assertClass(NoRetryDeadLinkException.class, exc);

    conn.setURL(starturl);
    exc = ((HttpResultMap)plugin.getCacheResultMap()).mapException(au, conn,
                                                                   500, "foo");
    assertClass(RetrySameUrlException.class, exc);

  }
  
  public void testPollSpecial() throws Exception {
    String ROOT_URL = "http://www.example.com/";
    Properties props = new Properties();
    props.setProperty(VOL_KEY, "322");
    props.setProperty(BASE_URL_KEY, ROOT_URL);
    DefinableArchivalUnit au = makeAuFromProps(props);
    
    // if it changes in the plugin, you will likely need to change the test, so verify
    assertEquals(Arrays.asList(
        HW_REPAIR_FROM_PEER_REGEXP),
        RegexpUtil.regexpCollection(au.makeRepairFromPeerIfMissingUrlPatterns()));
    
    // make sure that's the regexp that will match to the expected url string
    // this also tests the regexp (which is the same) for the weighted poll map
    List <String> repairList = ListUtil.list(
        ROOT_URL + "css/9.2.6/print.css",
        ROOT_URL + "js/9.3.2/lib/functional.min.js",
        ROOT_URL + "css/8.10.4/custom-theme/images/ui-bg_flat_100_006eb2_40x100.png",
        ROOT_URL + "publisher/icons/login.gif",
        ROOT_URL + "shared/img/common/hw-lens-monocle-xsm.png",
        ROOT_URL + "shared/img/content/int-data-supp-closed.png",
        ROOT_URL + "shared/img/fancybox/fancy_title_over.png"
        );
    
    Pattern p0 = Pattern.compile(HW_REPAIR_FROM_PEER_REGEXP[0]);
    Pattern p1 = Pattern.compile(HW_REPAIR_FROM_PEER_REGEXP[1]);
    Pattern p2 = Pattern.compile(HW_REPAIR_FROM_PEER_REGEXP[2]);

    Matcher m0, m1, m2;
    for (String urlString : repairList) {
      m0 = p0.matcher(urlString);
      m1 = p1.matcher(urlString);
      m2 = p2.matcher(urlString);
      assertEquals(urlString, true, m0.find() || m1.find() || m2.find());
    }
    //and this one should fail - it needs to be weighted correctly and repaired from publisher if possible
    String notString = ROOT_URL + "img/close-icon.png";
    m0 = p0.matcher(notString);
    m1 = p1.matcher(notString);
    m2 = p2.matcher(notString);
    assertEquals(false, m0.find() && m1.find() && m2.find());
    
    PatternFloatMap urlPollResults = au.makeUrlPollResultWeightMap();
    assertNotNull(urlPollResults);
    for (String urlString : repairList) {
      assertEquals(0.0, urlPollResults.getMatch(urlString, (float) 1), .0001);
    }
    assertEquals(0.0, urlPollResults.getMatch(ROOT_URL + "search?submit=yes&issue=Suppl%202&volume=101&sortspec=first-page&tocsectionid=Abstracts&FIRSTINDEX=0", (float) 1), .0001);
    assertEquals(1.0, urlPollResults.getMatch(ROOT_URL + "search?submit=yes&sortspec=first-page&tocsectionid=Abstracts&volume=101&issue=Suppl%202", (float) 1), .0001);
  }
  
}