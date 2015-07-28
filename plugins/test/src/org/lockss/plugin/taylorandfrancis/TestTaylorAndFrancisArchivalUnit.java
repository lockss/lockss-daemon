/*
 * $Id$
 */

/*

Copyright (c) 2000-2013 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.plugin.taylorandfrancis;

import java.net.URL;
import java.util.Properties;

import junit.framework.Test;

import org.lockss.app.LockssDaemon;
import org.lockss.config.Configuration;
import org.lockss.crawler.CrawlRateLimiter;
import org.lockss.daemon.*;
import org.lockss.plugin.*;
import org.lockss.plugin.base.*;
import org.lockss.plugin.definable.*;
import org.lockss.state.AuState;
import org.lockss.test.*;
import org.lockss.util.*;

//
// This plugin test framework is set up to run the same tests in two variants - CLOCKSS and GLN
// without having to actually duplicate any of the written tests
//
public class TestTaylorAndFrancisArchivalUnit extends LockssTestCase {
  private MockLockssDaemon theDaemon;
  static final String BASE_URL_KEY = ConfigParamDescr.BASE_URL.getKey();
  static final String JID_KEY = ConfigParamDescr.JOURNAL_ID.getKey();
  static final String VOL_KEY = ConfigParamDescr.VOLUME_NAME.getKey();
  static final String ROOT_URL = "http://www.tandfonline.com/";
  
  static Logger log = Logger.getLogger("TestTaylorAndFrancisArchivalUnit");
  protected String PluginIdentifier;
  protected String keyword;
  protected String PluginName;
  protected Boolean override;
 
  //Variant to test with GLN version of plugin
  public static class TestGLNPlugin extends TestTaylorAndFrancisArchivalUnit {
   
    public void setUp() throws Exception {
      override = false; 
      super.setUp();
      PluginIdentifier="org.lockss.plugin.taylorandfrancis.TaylorAndFrancisPlugin";   
      keyword = "lockss";
      PluginName = "Taylor & Francis Plugin";
   }
    
  }
  
  //Variant to test with the CLOCKSS version of plugin
  public static class TestCLOCKSSPlugin extends TestTaylorAndFrancisArchivalUnit {
    
    public void setUp() throws Exception {
      override = false;
      super.setUp();
      PluginIdentifier="org.lockss.plugin.taylorandfrancis.ClockssTaylorAndFrancisPlugin";   
      keyword="clockss";
      PluginName = "Taylor & Francis Plugin (CLOCKSS)";
   }
    
  }

  //Variant to test with the CLOCKSS override version of plugin
  public static class TestOverrideCLOCKSSPlugin extends TestTaylorAndFrancisArchivalUnit {
    
    public void setUp() throws Exception {
      override = true; // must come before super setup();
      super.setUp();
      PluginIdentifier="org.lockss.plugin.taylorandfrancis.ClockssTaylorAndFrancisPlugin";   
      keyword="clockss";
      PluginName = "Taylor & Francis Plugin (CLOCKSS)";
   }
  }
  
  public void setUp() throws Exception {
    super.setUp();
    setUpDiskSpace();
    theDaemon = getMockLockssDaemon();
    theDaemon.getHashService();
    
    // reduce the minimum so we check the plugins set value
    ConfigurationUtil.addFromArgs(BaseArchivalUnit.PARAM_MIN_FETCH_DELAY,"17");
    if (override) {
      ConfigurationUtil.addFromArgs(LockssDaemon.PARAM_TESTING_MODE, "clockss");
    }
  }

  public void tearDown() throws Exception {
    super.tearDown();
  }

  // make the AU using the appropriate plugin
  private DefinableArchivalUnit makeAu(URL url, int volume, String jid)
      throws Exception {

    Properties props = new Properties();
    props.setProperty(VOL_KEY, Integer.toString(volume));
    props.setProperty(JID_KEY, jid);
    if (url != null) {
      props.setProperty(BASE_URL_KEY, url.toString());
    }
    Configuration config = ConfigurationUtil.fromProps(props);
    
    DefinablePlugin ap = new DefinablePlugin();
    ap.initPlugin(getMockLockssDaemon(),
        PluginIdentifier);
    DefinableArchivalUnit au = (DefinableArchivalUnit)ap.createAu(config);
    return au;
  }
  
  
  private static final String gln_user_msg = 
      "Atypon Systems hosts this archival unit (AU) " +
          "and requires that you <a " +
          "href=\'http://www.tandfonline.com/action/institutionLockssIpChange\'>" +
          "register the IP address of this LOCKSS box in your institutional account as" +
          " a crawler</a> before allowing your LOCKSS box to harvest this AU." +
          " Failure to comply with this publisher requirement may trigger crawler traps" + 
          " on the Atypon Systems platform, and your LOCKSS box or your entire institution" +
          " may be temporarily banned from accessing the site. You only need to register the IP " +
          "address of your LOCKSS box once for all AUs published by Taylor & Francis.";


  
  public void testConfigUsrMsg() throws Exception {
     URL base = new URL(ROOT_URL);
    ArchivalUnit tfAu = makeAu(base, 39, "rabr20");
    if (keyword == "lockss") {
      log.debug3("testing GLN user message");
      assertEquals(gln_user_msg, tfAu.getProperties().getString(DefinableArchivalUnit.KEY_AU_CONFIG_USER_MSG, null));
    } else {
      log.debug3("testing CLOCKSS, no user message");
      assertEquals(null, tfAu.getProperties().getString(DefinableArchivalUnit.KEY_AU_CONFIG_USER_MSG, null));
    }
  }

  public void testConstructNullUrl() throws Exception {
    try {
      makeAu(null, 1, "rabr20");
      fail("Should have thrown ArchivalUnit.ConfigurationException");
    } catch (ArchivalUnit.ConfigurationException e) {
    }
  }

  public void testShouldCacheProperPages() throws Exception {
    URL base = new URL(ROOT_URL);
    ArchivalUnit tfAu = makeAu(base, 39, "rabr20");
    theDaemon.getLockssRepository(tfAu);
    theDaemon.getNodeManager(tfAu);
    BaseCachedUrlSet cus = new BaseCachedUrlSet(tfAu,
        new RangeCachedUrlSetSpec(base.toString()));
    // toc page for an issue
    shouldCacheTest(ROOT_URL+"toc/rabr20/39/1", true, tfAu, cus);
    // wrong volume
    shouldCacheTest(ROOT_URL+"toc/rabr20/12/index.html", false, tfAu, cus);
    // a few special crawl rules in BaseAtypon specific to T&F
    shouldCacheTest(ROOT_URL+"doi/abs/10.1111/ABC.2010.XYZ?tab=permissions", false, tfAu, cus);
    shouldCacheTest(ROOT_URL+"doi/abs/10.1111/ABC.2010.XYZ", true, tfAu, cus);
    shouldCacheTest(ROOT_URL+"imgJawr/cb1468962619/templates/jsp/_style2/_tandf/images/favicon.png", false, tfAu, cus);
    shouldCacheTest(ROOT_URL+"doi/abs/10.1111/null?sequence=rlsh20%2F2012%2Frlsh20.v033.i01%2Frlsh20.v033.i01%2Fproduction", false, tfAu, cus);
    shouldCacheTest(ROOT_URL+"doi/full/10.1111/null?sequence=rlsh20%2F2012%2Frlsh20.v033.i01%2Frlsh20.v033.i01%2Fproduction", false, tfAu, cus);
    // LOCKSS
    shouldCacheTest("http://lockss.stanford.edu", false, tfAu, cus);
    // other sites
  }
  
  private void shouldCacheTest(String url, boolean shouldCache,
      ArchivalUnit au, CachedUrlSet cus) {
    assertEquals(shouldCache, au.shouldBeCached(url));
  }
  
  public void testStartUrlConstruction() throws Exception {
    URL url = new URL(ROOT_URL);

    // 4 digit
    String expected = ROOT_URL+keyword+"/uytj20/24/index.html";
    DefinableArchivalUnit tfAu = makeAu(url, 24, "uytj20");
    assertEquals(ListUtil.list(expected), tfAu.getStartUrls());
  }
  
  public void testShouldNotCachePageFromOtherSite() throws Exception {
    URL base = new URL("http://www.tandfonline.com/");
    int volume = 24;
    String jid = "uytj20";
    ArchivalUnit tfAu = makeAu(base, volume, jid);

    theDaemon.getLockssRepository(tfAu);
    theDaemon.getNodeManager(tfAu);
    CachedUrlSetSpec spec = new RangeCachedUrlSetSpec(base.toString());
    BaseCachedUrlSet cus = new BaseCachedUrlSet(tfAu, spec);
    assertFalse(tfAu.shouldBeCached(
        "http://shadow2.stanford.edu/lockss/uytj20/24/index.html"));
  }


  public void testShouldDoNewContentCrawlTooEarly() throws Exception {
    ArchivalUnit tfAu =
      makeAu(new URL("http://www.tandfonline.com/"), 39, "rabr20");

    AuState aus = new MockAuState(null, TimeBase.nowMs(), -1, -1, null);

    assertFalse(tfAu.shouldCrawlForNewContent(aus));
  }

  public void testShouldDoNewContentCrawlFor0() throws Exception {
    ArchivalUnit tfAu =
      makeAu(new URL("http://www.tandfonline.com/"), 39, "rabr20");

    AuState aus = new MockAuState(null, 0, -1, -1, null);

    assertTrue(tfAu.shouldCrawlForNewContent(aus));
  }

  public void testShouldDoNewContentCrawlEachMonth() throws Exception {
    ArchivalUnit tfAu =
      makeAu(new URL("http://www.tandfonline.com/"), 39, "rabr20");

    AuState aus = new MockAuState(null, 4 * Constants.WEEK, -1, -1, null);

    assertTrue(tfAu.shouldCrawlForNewContent(aus));
  }

  public void testgetName() throws Exception {
    DefinableArchivalUnit au =
      makeAu(new URL("http://www.tandfonline.com/"), 39, "rabr20");
    assertEquals(PluginName + ", Base URL http://www.tandfonline.com/, Journal ID rabr20, Volume 39", au.getName());
    DefinableArchivalUnit au1 =
      makeAu(new URL("http://www.bmj.com/"), 24, "uytj20");
    assertEquals(PluginName + ", Base URL http://www.bmj.com/, Journal ID uytj20, Volume 24", au1.getName());
  }

  public void testCrawlRateWindow() throws Exception {
 
    String slowrate = "1/3500";
    String fastrate = "1/2s";
  
    DefinableArchivalUnit au =
        makeAu(new URL("http://www.tandfonline.com/"), 39, "rabr20");
    
    RateLimiterInfo rli = au.getRateLimiterInfo();
    
    if (override) {
      //au_def_pause_time is 100, which will translate to an implied numerator of "1" in RateLimiterInfo
      // note that for override, the daemon also had its default rate minimum set to "17"
      slowrate = fastrate = "1/100";

      log.debug3("AU fetch rate limiter is: " + au.findFetchRateLimiter().getInterval());
      assertEquals(slowrate, rli.getDefaultRate());
      return;
    }
 
    // In the non-override case, we will need to check against crawl window rates...
    CrawlRateLimiter crl = CrawlRateLimiter.Util.forRli(rli);
    
    //Using different dates/times check the rate used
    // WINDOW is: 1/2s from 12:00 - 24:00PS/DT; 1/6s from 24:00 - 12:00PS/DT
    // TimeBase timezone is default is GMT so take this in to account...
    // currently no differentiation based on file type
    
    TimeBase.setSimulated("2013/03/25 13:00:00"); //Will be 7/8 hours earlier for PD/ST 
    log.debug3("The time was set to 13:00:00 and TimeBase is set to: " + TimeBase.nowDate());
    assertEquals(slowrate, crl.getRateLimiterFor("file.pdf", null).getRate());
    
    TimeBase.setSimulated("2013/03/25 1:00:00");
    log.debug3("The time was set to 1:00:00 and TimeBase is set to: " + TimeBase.nowDate());
    assertEquals(fastrate, crl.getRateLimiterFor("file.html", null).getRate());
    
    TimeBase.setSimulated("2013/03/25 11:59:59");
    log.debug3("The time was set to 11:59:59 and TimeBase is set to: " + TimeBase.nowDate());
    assertEquals(slowrate, crl.getRateLimiterFor("file.html", null).getRate());
    
    TimeBase.setSimulated("2013/03/25 12:00:00");
    log.debug3("The time was set to 12:00:00 and TimeBase is set to: " + TimeBase.nowDate());
    assertEquals(slowrate, crl.getRateLimiterFor("file.html", null).getRate());
    
    TimeBase.setSimulated("2013/03/25 23:00:01");
    log.debug3("The time was set to 23:00:01 and TimeBase is set to: " + TimeBase.nowDate());
    assertEquals(fastrate, crl.getRateLimiterFor("file.pdf", null).getRate());
    
    TimeBase.setSimulated("2013/03/25 24:00:01");
    log.debug3("The time was set to 24:00:01 and TimeBase is set to: " + TimeBase.nowDate());
    assertEquals(fastrate, crl.getRateLimiterFor("file.pdf", null).getRate());
    
  }

public static Test suite() {
  return variantSuites(new Class[] {
      TestGLNPlugin.class,
      TestCLOCKSSPlugin.class,
      TestOverrideCLOCKSSPlugin.class
    });
}

}

