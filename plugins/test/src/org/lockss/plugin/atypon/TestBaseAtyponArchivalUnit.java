/*
 * $Id$
 */

/*

Copyright (c) 2000-2015 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.plugin.atypon;

import java.net.URL;
import java.util.*;

import org.apache.commons.lang.StringUtils;
import org.lockss.config.Configuration;
import org.lockss.daemon.*;
import org.lockss.plugin.*;
import org.lockss.plugin.base.BaseCachedUrlSet;
import org.lockss.plugin.definable.*;
import org.lockss.state.AuState;
import org.lockss.test.*;
import org.lockss.util.*;

//
// This plugin test framework is set up to run the same tests in two variants - CLOCKSS and GLN
// without having to actually duplicate any of the written tests
//
public class TestBaseAtyponArchivalUnit extends LockssTestCase {
  private MockLockssDaemon theDaemon;
  static final String BASE_URL_KEY = ConfigParamDescr.BASE_URL.getKey();
  static final String JID_KEY = ConfigParamDescr.JOURNAL_ID.getKey();
  static final String VOL_KEY = ConfigParamDescr.VOLUME_NAME.getKey();
  static final String ROOT_URL = "http://www.BaseAtypon.com/"; //this is not a real url

  private static final Logger log = Logger.getLogger(TestBaseAtyponArchivalUnit.class);

  static final String PLUGIN_ID = "org.lockss.plugin.atypon.BaseAtyponPlugin";
  static final String PluginName = "Base Atypon Plugin";

  public void setUp() throws Exception {
    super.setUp();
    setUpDiskSpace();
    theDaemon = getMockLockssDaemon();
    theDaemon.getHashService();
  }

  public void tearDown() throws Exception {
    super.tearDown();
  }

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
        PLUGIN_ID);
    DefinableArchivalUnit au = (DefinableArchivalUnit)ap.createAu(config);
    return au;
  }

  public void testConstructNullUrl() throws Exception {
    try {
      makeAu(null, 1, "ajph");
      fail("Should have thrown ArchivalUnit.ConfigurationException");
    } catch (ArchivalUnit.ConfigurationException e) {
    }
  }

  //
  // Test the crawl rules
  //
  public void testShouldCacheProperPages() throws Exception {
    URL base = new URL(ROOT_URL);
    ArchivalUnit ABAu = makeAu(base, 123, "xxxx");
    theDaemon.getLockssRepository(ABAu);
    theDaemon.getNodeManager(ABAu);
    BaseCachedUrlSet cus = new BaseCachedUrlSet(ABAu,
        new RangeCachedUrlSetSpec(base.toString()));
    // Test for pages that should get crawled
    //manifest page
    shouldCacheTest(ROOT_URL+"lockss/xxxx/123/index.html", true, ABAu, cus);    
    // images (etc.) but not as query arguments
    shouldCacheTest(ROOT_URL+"foo/bar/baz/qux.js", true, ABAu, cus);
    shouldCacheTest(ROOT_URL+"foo/bar/baz?url=qux.js", false, ABAu, cus);
    // toc page for an issue
    shouldCacheTest(ROOT_URL+"toc/xxxx/123/5", true, ABAu, cus);
    // special issue
    shouldCacheTest(ROOT_URL+"doi/abs/11.1111/1234-abc.12G", true, ABAu, cus);
    shouldCacheTest(ROOT_URL+"doi/pdf/11.1111/1234-abc.12G", true, ABAu, cus);
    shouldCacheTest(ROOT_URL+"doi/full/11.1111/1234-abc.12G", true, ABAu, cus);
    shouldCacheTest(ROOT_URL+"doi/pdfplus/11.1111/1234-abc.12G", true, ABAu, cus);
    shouldCacheTest(ROOT_URL+"doi/suppl/11.1111/1234-abc.12G", true, ABAu, cus);
    shouldCacheTest(ROOT_URL+"doi/suppl/11.1111/1234-abc.12G/suppl_stuff.docx", true, ABAu, cus);
    shouldCacheTest(ROOT_URL+"doi/suppl/11.1111/1234-abc.12G/suppl_pres.ppt", true, ABAu, cus);
    shouldCacheTest(ROOT_URL+"doi/ref/11.1111/1234-abc.12G", true, ABAu, cus);
    shouldCacheTest(ROOT_URL+"doi/video_original/11.1111/1234-abc.12G", true, ABAu, cus);
    //other DOI forms
    shouldCacheTest(ROOT_URL+"doi/abs/11.1111/XX12FG", true, ABAu, cus);
    shouldCacheTest(ROOT_URL+"doi/abs/11.1111/mypubfile", true, ABAu, cus);
    shouldCacheTest(ROOT_URL+"doi/abs/11.1111/1111111", true, ABAu, cus);
    // other argument based URLS
    shouldCacheTest(ROOT_URL+"action/showImage?doi=", true, ABAu, cus);
    shouldCacheTest(ROOT_URL+"action/showPopup?citid=citart1&id=T0003&doi=10.1080/19416520.2010.495530", true, ABAu, cus);
    shouldCacheTest(ROOT_URL+"action/showPopup?citid=citart1&id=CIT0013&doi=10.1080/19416521003732362", true, ABAu, cus);
    shouldCacheTest(ROOT_URL+"action/showFullPopup?doi=10.1206%2F3743.2&id=m05", true, ABAu, cus);
    // require option argument before doi has value
    shouldCacheTest(ROOT_URL+"action/showFullPopup?id=foo&doi=10.1206%2F3743.2", true, ABAu, cus);
    // missing value
    shouldCacheTest(ROOT_URL+"action/showFullPopup?id=&doi=10.1206%2F3743.2", false, ABAu, cus);
    // images figures and tables can live here
    shouldCacheTest(ROOT_URL+"na101/home/literatum/publisher/apha/journals/covergifs/xxxx/2005/15200477-86.6/cover.jpg", true, ABAu, cus);   

    // Now a couple that shouldn't get crawled
    // wrong volume
    shouldCacheTest(ROOT_URL+"toc/xxxx/12/index.html", false, ABAu, cus);
    // avoid spider trap
    shouldCacheTest(ROOT_URL+"doi/abs/11.1111/99.9999-99999", false, ABAu, cus);
    // LOCKSS
    shouldCacheTest("http://lockss.stanford.edu", false, ABAu, cus);
    // other sites

    // Later addition of crawl rules to avoid polling things we no longer collect or that we normalize off
    // this is normalized off but does exist in some aus from early crawls
    shouldCacheTest(ROOT_URL + "doi/abs/10.5504/50YRTIMB.2011.0036?queryID=%24%7BresultBean.queryID%7D", 
        false, ABAu, cus);
    // but if it were part of a doiname
    shouldCacheTest(ROOT_URL + "doi/abs/10.5504/50YRTIMB.2011.0036queryIDresultBean_name", 
        true, ABAu, cus);
    // no longer accepted citation formats
    shouldCacheTest(ROOT_URL + "action/downloadCitation?doi=10.1111%2F123456&format=bibtex&include=cit", 
        false, ABAu, cus);
    shouldCacheTest(ROOT_URL + "action/downloadCitation?doi=10.1111%2F123456&format=medlars&include=cit", 
        false, ABAu, cus);
    shouldCacheTest(ROOT_URL + "action/downloadCitation?doi=10.1111%2F123456&format=endnote&include=cit", 
        false, ABAu, cus);
    shouldCacheTest(ROOT_URL + "action/downloadCitation?doi=10.1111%2F123456&format=refworks&include=cit", 
        false, ABAu, cus);
    shouldCacheTest(ROOT_URL + "action/downloadCitation?doi=10.1111%2F123456&format=refworks=cn&include=cit", 
        false, ABAu, cus);
    //no longer accepted include types.  we allow abs and cit, but not ref
    shouldCacheTest(ROOT_URL + "action/downloadCitation?doi=10.1111%2F123456&format=ris&include=abs", 
        true, ABAu, cus);
    shouldCacheTest(ROOT_URL + "action/downloadCitation?doi=10.1111%2F123456&format=ris&include=ref", 
        false, ABAu, cus);
    // this one is valid 
    shouldCacheTest(ROOT_URL + "action/downloadCitation?doi=10.1111%2F123456&format=ris&include=cit", 
        true, ABAu, cus);

    // ASCE use of relative link where it should be absolute causes ever-deeper URLS because
    // the "page not found" page uses the template with the same relative link problem.
    // added to crawl rules to combat this until they fix it
    shouldCacheTest(ROOT_URL + "action/showCart?backUri=/action/showLogin?uri=/action/showCart?backUri=/action/showLogin?uri=/doi/abs/10.1061/templates/jsp/js/googleAnalyticsPlugin.js", 
        false, ABAu, cus); 
    shouldCacheTest(ROOT_URL + "doi/abs/10.1061/templates/jsp/js/googleAnalyticsPlugin.js", 
        false, ABAu, cus); 
    shouldCacheTest(ROOT_URL + "doi/abs/10.1061/templates/jsp/js/templates/jsp/js/templates/jsp/js/templates/jsp/js/googleAnalyticsPlugin.js", 
        false, ABAu, cus); 

  }



  private void shouldCacheTest(String url, boolean shouldCache,
      ArchivalUnit au, CachedUrlSet cus) {
    assertEquals(shouldCache, au.shouldBeCached(url));
  }

  public void testStartUrlConstruction() throws Exception {
    URL url = new URL(ROOT_URL);

    // 4 digit
    String expected = ROOT_URL+"lockss/xxxx/123/index.html";
    DefinableArchivalUnit ABAu = makeAu(url, 123, "xxxx");
    assertEquals(ListUtil.list(expected), ABAu.getStartUrls());
  }

  public void testShouldNotCachePageFromOtherSite() throws Exception {
    URL base = new URL("http://www.BaseAtypon.com/");
    int volume = 123;
    String jid = "xxxx";
    ArchivalUnit ABAu = makeAu(base, volume, jid);

    theDaemon.getLockssRepository(ABAu);
    theDaemon.getNodeManager(ABAu);
    CachedUrlSetSpec spec = new RangeCachedUrlSetSpec(base.toString());
    BaseCachedUrlSet cus = new BaseCachedUrlSet(ABAu, spec);
    assertFalse(ABAu.shouldBeCached(
        "http://shadow2.stanford.edu/lockss/xxxx/123/index.html"));
  }


  public void testShouldDoNewContentCrawlTooEarly() throws Exception {
    ArchivalUnit ABAu =
        makeAu(new URL("http://www.BaseAtypon.com/"), 33, "yyyy");

    AuState aus = new MockAuState(null, TimeBase.nowMs(), -1, -1, null);

    assertFalse(ABAu.shouldCrawlForNewContent(aus));
  }

  public void testShouldDoNewContentCrawlFor0() throws Exception {
    ArchivalUnit ABAu =
        makeAu(new URL("http://www.BaseAtypon.com/"), 33, "yyyy");

    AuState aus = new MockAuState(null, 0, -1, -1, null);

    assertTrue(ABAu.shouldCrawlForNewContent(aus));
  }


  public void testGetName() throws Exception {
    DefinableArchivalUnit au =
        makeAu(new URL("http://www.BaseAtypon.com/"), 33, "yyyy");
    assertEquals(PluginName + ", Base URL http://www.BaseAtypon.com/, Journal ID yyyy, Volume 33", au.getName());
    DefinableArchivalUnit au1 =
        makeAu(new URL("http://www.apha.com/"), 24, "apha");
    assertEquals(PluginName + ", Base URL http://www.apha.com/, Journal ID apha, Volume 24", au1.getName());
  }

  private static final String gln_lockss_user_msg = 
      "Atypon Systems hosts this archival unit (AU) " +
          "and may require you to register the IP address "+
          "of this LOCKSS box as a crawler. For more information, visit the <a " +
          "href=\'http://www.lockss.org/support/use-a-lockss-box/adding-titles/publisher-ip-address-registration-contacts-for-global-lockss-network/\'>" +
          "LOCKSS IP address registration page</a>.";

  private static final String bq_msg = 
      "Atypon Systems hosts this archival unit (AU) and may require you to register the IP address of " +
          "this LOCKSS box as a crawler, by sending e-mail to <a href=\'mailto:pcoyne@qf.org.qa\'>Paul Coyne</a>. " +
          "Failure to comply with this publisher requirement may trigger crawler traps on the Atypon Systems platform, " +
          "and your LOCKSS box or your entire institution may be temporarily banned from accessing the site. " +
          "You only need to register the IP address of your LOCKSS box once for all AUs published by Bloomsbury Qatar.";

  private static final String default_msg_part1 = 
      "Atypon Systems hosts this archival unit (AU) and requires " +
          "that you <a href=\'";

  private static final String default_msg_part2 =
      "action/institutionLockssIpChange\'>register the " +
          "IP address of this LOCKSS box in your institutional account as a " +
          "crawler</a> before allowing your LOCKSS box to harvest this AU. " +
          "Failure to comply with this publisher requirement may trigger crawler traps on the " +
          "Atypon Systems platform, and your LOCKSS box or your entire institution may be " +
          "temporarily banned from accessing the site. You only need to register the IP address " +
          "of your LOCKSS box once for all AUs published by this publisher.";



  private static final String BASE_ATYPON_BASE = "http://www.BaseAtypon.org/";
  /* test all the atypon child au_config_user_msgs
   * by first checking the gln message either against a specific passed-in message
   * or by making sure it contains the appropriate base-url and publisher name (to guard
   * against cut-n-past errors
   * and then checking that the clockss plugin has a null message
   */
  public void testUserMsgs() throws Exception {
    // the default when a child doesn't set
    testSpecificUserMsg(BASE_ATYPON_BASE, default_msg_part1 + BASE_ATYPON_BASE + default_msg_part2, 
       "org.lockss.plugin.atypon.BaseAtyponPlugin",
       null);

    //AMetSoc - points users at our web page with registration info
    testSpecificUserMsg("http://journals.ametsoc.org/", gln_lockss_user_msg, 
        "org.lockss.plugin.atypon.americanmeteorologicalsociety.AMetSocPlugin",
        "org.lockss.plugin.atypon.americanmeteorologicalsociety.ClockssAMetSocPlugin");
    // with a null msg it will verify that the message follows the form "<base_url>/action/institutionLockssIpChange"
    testSpecificUserMsg("http://arc.aiaa.org/", null, 
        "org.lockss.plugin.atypon.aiaa.AIAAPlugin",
        "org.lockss.plugin.atypon.aiaa.ClockssAIAAPlugin");
    //asce - only a clockss plugin
    testSpecificUserMsg("http://ascelibrary.org/", null, 
        null,
        "org.lockss.plugin.atypon.americansocietyofcivilengineers.ClockssASCEPlugin");
    //ammons
    testSpecificUserMsg("http://www.amsciepub.com/", null, 
        null,
        "org.lockss.plugin.atypon.ammonsscientific.ClockssAmmonsScientificPlugin");
    //apha
    testSpecificUserMsg("http://ajph.aphapublications.org/", null, 
        "org.lockss.plugin.atypon.apha.AmPublicHealthAssocPlugin",
        null);
    //arrs
    testSpecificUserMsg("http://www.ajronline.org/", null, 
        "org.lockss.plugin.atypon.arrs.ARRSPlugin",
        null);
    //bir
    testSpecificUserMsg("http://www.birpublications.org/", null, 
        "org.lockss.plugin.atypon.bir.BIRAtyponPlugin",
        "org.lockss.plugin.atypon.bir.ClockssBIRAtyponPlugin");
    //bloomsburyqatar - this one has a special message
    testSpecificUserMsg("http://www.qscience.com/", bq_msg, 
        "org.lockss.plugin.atypon.bloomsburyqatar.BloomsburyQatarPlugin",
        "org.lockss.plugin.atypon.bloomsburyqatar.ClockssBloomsburyQatarPlugin");
    //emeraldgroup
    testSpecificUserMsg("http://www.emeraldinsight.com/", null, 
        "org.lockss.plugin.atypon.emeraldgroup.EmeraldGroupPlugin",
        null);
    //endocrine society
    testSpecificUserMsg("http://press.endocrine.org/", null, 
        "org.lockss.plugin.atypon.endocrinesociety.EndocrineSocietyPlugin",
        "org.lockss.plugin.atypon.endocrinesociety.ClockssEndocrineSocietyPlugin");
    //futurescience
    testSpecificUserMsg("http://www.future-science.com/", null,
        "org.lockss.plugin.atypon.futurescience.FutureSciencePlugin",
        "org.lockss.plugin.atypon.futurescience.ClockssFutureSciencePlugin");
    //inderscience
    testSpecificUserMsg("http://www.inderscienceonline.com/", null, 
        "org.lockss.plugin.atypon.inderscience.InderscienceAtyponPlugin",
        "org.lockss.plugin.atypon.inderscience.ClockssInderscienceAtyponPlugin");    
    //liverpool
    testSpecificUserMsg("http://online.liverpooluniversitypress.co.uk/", null, 
        "org.lockss.plugin.atypon.liverpool.LiverpoolAtyponPlugin",
        "org.lockss.plugin.atypon.liverpool.ClockssLiverpoolAtyponPlugin");
    //maney
    testSpecificUserMsg("http://www.maneyonline.com/", null, 
        "org.lockss.plugin.atypon.maney.ManeyAtyponPlugin",
        "org.lockss.plugin.atypon.maney.ClockssManeyAtyponPlugin");
    //mark allen group
    testSpecificUserMsg("http://www.magonlinelibrary.com/", null, 
        "org.lockss.plugin.atypon.markallen.MarkAllenPlugin",
        "org.lockss.plugin.atypon.markallen.ClockssMarkAllenPlugin");
    //multiscience
    testSpecificUserMsg("http://multi-science.atypon.com/", null, 
        "org.lockss.plugin.atypon.multiscience.MultiScienceAtyponPlugin",
        "org.lockss.plugin.atypon.multiscience.ClockssMultiScienceAtyponPlugin");    
    //nrcresearch
    testSpecificUserMsg("http://www.nrcresearchpress.com/", null, 
        null,
        "org.lockss.plugin.atypon.nrcresearchpress.ClockssNRCResearchPressPlugin");
    //practicalaction
    testSpecificUserMsg("http://www.developmentbookshelf.com/", null, 
        null,
        "org.lockss.plugin.atypon.practicalaction.ClockssPracticalActionAtyponPlugin");    
    //seg
    testSpecificUserMsg("http://library.seg.org/", null, 
        null,
        "org.lockss.plugin.atypon.seg.ClockssSEGPlugin");
    //siam
    testSpecificUserMsg("http://epubs.siam.org/", null, 
        "org.lockss.plugin.atypon.siam.SiamPlugin",
        "org.lockss.plugin.atypon.siam.ClockssSiamPlugin");

    // and the ones that do not live below the atypon directory
    //bioone
    testSpecificUserMsg("http://www.bioone.org/", null, 
        "org.lockss.plugin.bioone.BioOneAtyponPlugin",
        "org.lockss.plugin.bioone.ClockssBioOneAtyponPlugin");
    //edinburgh
    testSpecificUserMsg("http://www.euppublishing.com/", null, 
        "org.lockss.plugin.edinburgh.EdinburghUniversityPressPlugin",
        "org.lockss.plugin.edinburgh.ClockssEdinburghUniversityPressPlugin");
    //t&f
    testSpecificUserMsg("http://www.tandfonline.com/", null, 
        "org.lockss.plugin.taylorandfrancis.TaylorAndFrancisPlugin",
        "org.lockss.plugin.taylorandfrancis.ClockssTaylorAndFrancisPlugin");
    //wageningen
    testSpecificUserMsg("http://www.wageningenacademic.com/", null, 
        null,
        "org.lockss.plugin.atypon.wageningen.ClockssWageningenAtyponPlugin");    
  }

  // Associate the base_url with the publisher name for convenience
  static private final Map<String, String> pluginPubMap =
      new HashMap<String,String>();
  static {
    pluginPubMap.put("http://journals.ametsoc.org/", "American Meteorological Society");
    pluginPubMap.put("http://arc.aiaa.org/", "American Institute of Aeronautics and Astronautics");
    pluginPubMap.put("http://www.ajronline.org/", "American Roentgen Ray Society");
    pluginPubMap.put("http://ascelibrary.org/", "American Society of Civil Engineers");
    pluginPubMap.put("http://www.amsciepub.com/", "Ammons Scientific Journals");
    pluginPubMap.put("http://ajph.aphapublications.org/", "American Public Health Association");
    pluginPubMap.put("http://www.birpublications.org/", "British Institute of Radiology");
    pluginPubMap.put("http://www.qscience.com/", "Bloomsbury Qatar Foundation Journals");
    pluginPubMap.put("http://press.endocrine.org/", "Endocrine Society");
    pluginPubMap.put("http://www.emeraldinsight.com/", "Emerald Group Publishing");
    pluginPubMap.put("http://www.future-science.com/", "Future Science");
    pluginPubMap.put("http://www.inderscienceonline.com/", "Inderscience");
    pluginPubMap.put("http://online.liverpooluniversitypress.co.uk/", "Liverpool University Press");
    pluginPubMap.put("http://www.maneyonline.com/", "Maney Publishing");
    pluginPubMap.put("http://www.magonlinelibrary.com/", "Mark Allen Group");
    pluginPubMap.put("http://multi-science.atypon.com/", "Multi-Science");    
    pluginPubMap.put("http://www.nrcresearchpress.com/", "NRC Research Press");
    pluginPubMap.put("http://www.developmentbookshelf.com/", "Practical Action Publishing");
    pluginPubMap.put("http://library.seg.org/", "Society of Exploration Geophysicists");
    pluginPubMap.put("http://epubs.siam.org/", "Society for Industrial and Applied Mathematics");
    pluginPubMap.put("http://www.bioone.org/", "BioOne");
    pluginPubMap.put("http://www.euppublishing.com/", "Edinburgh University Press");
    pluginPubMap.put("http://www.tandfonline.com/", "Taylor & Francis");
    pluginPubMap.put("http://www.wageningenacademic.com/", "Wageningen Academic Publishers");
  }


  private void testSpecificUserMsg(String plugin_base_url, String full_msg, 
      String gln_plugin, String clockss_plugin) throws Exception {

    Properties props = new Properties();
    props.setProperty(VOL_KEY, Integer.toString(17));
    props.setProperty(JID_KEY, "eint");
    props.setProperty(BASE_URL_KEY, plugin_base_url);
    Configuration config = ConfigurationUtil.fromProps(props);

    if (!StringUtils.isEmpty(gln_plugin)) {
      DefinablePlugin ap = new DefinablePlugin();
      ap.initPlugin(getMockLockssDaemon(),
          gln_plugin);
      DefinableArchivalUnit gAU = (DefinableArchivalUnit)ap.createAu(config);

      log.debug3("testing GLN user message");
      if (!StringUtils.isEmpty(full_msg)) {
        assertEquals(full_msg, gAU.getProperties().getString(DefinableArchivalUnit.KEY_AU_CONFIG_USER_MSG, null));
      } else {
        // check that the message includes "<base_url>/action/institutionLockssIpChange" and publisher name 
        String config_msg = gAU.getProperties().getString(DefinableArchivalUnit.KEY_AU_CONFIG_USER_MSG, null);
        log.debug3("config msg: " + config_msg);
        assertTrue(config_msg.contains(plugin_base_url + "action/institutionLockssIpChange"));
        assertTrue(config_msg.contains(pluginPubMap.get(plugin_base_url)));
      }
    }

    // now check clockss version non-message
    if (!StringUtils.isEmpty(clockss_plugin)) {
      DefinablePlugin cap = new DefinablePlugin();
      cap.initPlugin(getMockLockssDaemon(),
          clockss_plugin);
      DefinableArchivalUnit cAU = (DefinableArchivalUnit)cap.createAu(config);

      log.debug3("testing CLOCKSS absence of user message");
      assertEquals(null, cAU.getProperties().getString(DefinableArchivalUnit.KEY_AU_CONFIG_USER_MSG, null));
    }

  }

}

