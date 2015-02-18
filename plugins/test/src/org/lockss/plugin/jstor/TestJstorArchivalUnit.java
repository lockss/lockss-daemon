/*
 * $Id$
 */

/*

Copyright (c) 2000-2014 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.plugin.jstor;

import java.net.URL;
import java.util.List;
import java.util.Properties;

import org.apache.oro.text.regex.Pattern;
import org.apache.oro.text.regex.PatternMatcher;
import org.lockss.config.Configuration;
import org.lockss.daemon.*;
import org.lockss.plugin.*;
import org.lockss.plugin.base.*;
import org.lockss.plugin.definable.*;
import org.lockss.test.*;
import org.lockss.util.*;

//
// This plugin test framework is set up to run the same tests in two variants - CLOCKSS and GLN
// without having to actually duplicate any of the written tests
//
public class TestJstorArchivalUnit extends LockssTestCase {
  private MockLockssDaemon theDaemon;
  static final String BASE_URL_KEY = ConfigParamDescr.BASE_URL.getKey();
  static final String BASE_URL2_KEY = ConfigParamDescr.BASE_URL2.getKey();
  static final String JID_KEY = ConfigParamDescr.JOURNAL_ID.getKey();
  static final String VOL_KEY = ConfigParamDescr.VOLUME_NAME.getKey();
  static final String ROOT_URL = "http://www.jstor.org/"; 
  static final String ROOT_URL2 = "https://www.jstor.org/"; 
  
  static Logger log = Logger.getLogger(TestJstorArchivalUnit.class);
  
  static final String PLUGIN_ID = "org.lockss.plugin.jstor.JstorPlugin";
  static final String CLOCKSS_PLUGIN_ID = "org.lockss.plugin.jstor.ClockssJstorPlugin";
  static final String PluginName = "JSTOR Plugin";
  
  public void setUp() throws Exception {
    super.setUp();
    setUpDiskSpace();
    theDaemon = getMockLockssDaemon();
    theDaemon.getHashService();
  }

  public void tearDown() throws Exception {
    super.tearDown();
  }
  
  // backward compatible - make AU based on lockss version of plugin
  private DefinableArchivalUnit makeAu(boolean valid, int volume, String jid) 
    throws Exception {
      return makeAu(valid, volume, jid, false);
  }

  // A more flexible version of the makeAU routine that allows choice of plugin
  private DefinableArchivalUnit makeAu(boolean valid, int volume, String jid, boolean isClockss)
      throws Exception {

    Properties props = new Properties();
    props.setProperty(VOL_KEY, Integer.toString(volume));
    props.setProperty(JID_KEY, jid);
    if (valid == true) {
      props.setProperty(BASE_URL_KEY, ROOT_URL);
      props.setProperty(BASE_URL2_KEY, ROOT_URL2);
    }
    Configuration config = ConfigurationUtil.fromProps(props);
    
    DefinablePlugin ap = new DefinablePlugin();
    if (isClockss == false) {
      ap.initPlugin(getMockLockssDaemon(),PLUGIN_ID);
    } else {
      ap.initPlugin(getMockLockssDaemon(),CLOCKSS_PLUGIN_ID);
    }
    DefinableArchivalUnit au = (DefinableArchivalUnit)ap.createAu(config);
    return au;
  }

  public void testConstructNullUrl() throws Exception {
    try {
      makeAu(false, 1, "jmorahist");
      fail("Should have thrown ArchivalUnit.ConfigurationException");
    } catch (ArchivalUnit.ConfigurationException e) {
    }
  }
  
  public void testGetUrlStems() throws Exception {
    ArchivalUnit JSAu = makeAu(true, 123, "xxxx" );
    assertEquals(ListUtil.list(ROOT_URL, ROOT_URL2), JSAu.getUrlStems());
  }
  
  //
  // Test the crawl rules
  //
  public void testShouldCacheProperPages() throws Exception {
    ArchivalUnit JSAu = makeAu(true, 123, "xxxx");
    theDaemon.getLockssRepository(JSAu);
    theDaemon.getNodeManager(JSAu);
    BaseCachedUrlSet cus = new BaseCachedUrlSet(JSAu,
        new RangeCachedUrlSetSpec(ROOT_URL));
    // Test for pages that should get crawled
    //manifest page
    shouldCacheTest(ROOT_URL+"lockss/xxxx/123/index.html", true, JSAu, cus);    

    // YES: toc page reached off a manifest page
    //http://www.jstor.org/action/showToc?journalCode=jmorahist&issue=1%2F2&volume=13
    shouldCacheTest(ROOT_URL+"action/showToc?journalCode=xxxx&issue=1%2F2&volume=123", true, JSAu, cus);    
    shouldCacheTest(ROOT_URL+"action/showToc?journalCode=xxxx&volume=123&issue=1%2F2", true, JSAu, cus);    
    //NO: toc page NOT reached from manifest (from prev/next)
    // http://www.jstor.org/stable/i249413 or http://www.jstor.org/stable/10.1525/abt.2010.72.issue-9
    shouldCacheTest(ROOT_URL+"toc/xxxx/123/5", false, JSAu, cus); // usual Atypon, but not jstor
    shouldCacheTest(ROOT_URL+"stable/i249413", false, JSAu, cus);
    shouldCacheTest(ROOT_URL+"stable/11.1111/xxxx.2010.123.issue-5", false, JSAu, cus);


    // article pages
    // Take the pdf, pdfplus 
    //http://www.jstor.org/stable/pdfplus/41179291.pdf
    //http://www.jstor.org/stable/pdfplus/10.3764/aja.116.4.0573.pdf
    shouldCacheTest(ROOT_URL+"stable/pdf/11.1111/1234-abc.12G.pdf", true, JSAu, cus);
    shouldCacheTest(ROOT_URL+"stable/pdfplus/11.1111/1234-abc.12G.pdf", true, JSAu, cus);
    shouldCacheTest(ROOT_URL+"stable/full/11.1111/1234-abc.12G", true, JSAu, cus);
    shouldCacheTest(ROOT_URL+"stable/media/11.1111/1234-abc.12G", true, JSAu, cus);
    shouldCacheTest(ROOT_URL+"stable/select/11.1111/1234-abc.12G", true, JSAu, cus);
    // old style article pages
    shouldCacheTest(ROOT_URL+"stable/pdf/1234567.pdf", true, JSAu, cus);
    shouldCacheTest(ROOT_URL+"stable/full/1234567", true, JSAu, cus);
    // old style is only numbers
    shouldCacheTest(ROOT_URL+"stable/select/1234-abc.12G", false, JSAu, cus);
    

    // NO -abstract, view, info aspects of article pages (both opaque and not) redirect to pdf version
    //<!-- <base>/stable/10.1525/abt.2010.72.9.1 or <base>/stable/7436318 or -->
    //<!-- <base>/stable/info/746318 or <base>/stable/view/746318-->
    shouldCacheTest(ROOT_URL+"stable/11.1111/xxxx.2010.123.5.1", false, JSAu, cus);
    shouldCacheTest(ROOT_URL+"stable/123456", false, JSAu, cus);
    shouldCacheTest(ROOT_URL+"base/stable/info/123456", false, JSAu, cus);
    shouldCacheTest(ROOT_URL+"base/stable/view/123456", false, JSAu, cus);
    
    // YES -version of PDF with argument -- it will get normalized off
    //http://www.jstor.org/stable/pdfplus/41827175.pdf?acceptTC=true
    shouldCacheTest(ROOT_URL+"stable/pdfplus/11.1111/1234-abc.12G?acceptTC=true", true, JSAu, cus);
    
    // YES - citation download information - under base_url2 (ROOT_URL2)
    //https://www.jstor.org/action/downloadSingleCitationSec?format=refman&doi=10.5325/jmorahist.13.2.0158
    //https://www.jstor.org/action/downloadSingleCitationSec?format=refman&doi=10.3764/aja.116.4.0751
    //https://www.jstor.org/action/downloadSingleCitationSec?format=refman&doi=10.2307/41827175
    shouldCacheTest(ROOT_URL2+"action/downloadSingleCitationSec?format=refman&doi=11.1111/1234-abc.12G", true, JSAu, cus);
    shouldCacheTest(ROOT_URL2+"action/downloadSingleCitationSec?format=refman&doi=10.2307/41827175", true, JSAu, cus);
    
    // YES - support files
    //http://www.jstor.org/jawr/1105425977/bundles/jstorSiteCatalyst.js
    //http://www.jstor.org/literatum/publisher/jstor/journals/content/jmorahist/2013/
    //   jmorahist.13.1.issue-1/jmorahist.13.1.issue-1/20130523/jmorahist.13.1.issue-1.cover.jpg
    shouldCacheTest(ROOT_URL+"jawr/1105425977/bundles/jstorSiteCatalyst.js", true, JSAu, cus);
    shouldCacheTest(ROOT_URL+"literatum/publisher/jstor/journals/content/jmorahist/2013/jmorahist.13.1.issue-1/jmorahist.13.1.issue-1/20130523/jmorahist.13.1.issue-1.cover.jpg", true, JSAu, cus);
    shouldCacheTest(ROOT_URL2 + "action/downloadSingleCitationSec?format=refman&doi=10.5325/jmorahist.13.2.0197", true, JSAu, cus);
    
    // Now a couple that shouldn't get crawled
    // wrong volume
    shouldCacheTest(ROOT_URL+"action/showToc?journalCode=xxxx&volume=124%2F2&issue=5", false, JSAu, cus);   ;
    // avoid spider trap
    shouldCacheTest(ROOT_URL+"stable/pdfplus/11.1111/99.9999-99999", false, JSAu, cus);
    // LOCKSS
    shouldCacheTest("http://lockss.stanford.edu", false, JSAu, cus);
    // other sites
  }
  
  private void shouldCacheTest(String url, boolean shouldCache,
      ArchivalUnit au, CachedUrlSet cus) {
    assertEquals(shouldCache, au.shouldBeCached(url));
  }
  
  public void testStartUrlConstruction() throws Exception {

    // 4 digit
    String expected = ROOT_URL+"lockss/xxxx/123/index.html";
    DefinableArchivalUnit JSAu = makeAu(true, 123, "xxxx");
    assertEquals(ListUtil.list(expected), JSAu.getStartUrls());
  }
  
  public void testShouldNotCachePageFromOtherSite() throws Exception {
    int volume = 123;
    String jid = "xxxx";
    ArchivalUnit JSAu = makeAu(true, volume, jid);

    theDaemon.getLockssRepository(JSAu);
    theDaemon.getNodeManager(JSAu);
    CachedUrlSetSpec spec = new RangeCachedUrlSetSpec(ROOT_URL);
    BaseCachedUrlSet cus = new BaseCachedUrlSet(JSAu, spec);
    assertFalse(JSAu.shouldBeCached(
        "http://shadow2.stanford.edu/lockss/xxxx/123/index.html"));
  }


  public void testgetName() throws Exception {
    DefinableArchivalUnit au =
      makeAu(true, 33, "yyyy");
    assertEquals(PluginName + ", Base URL http://www.jstor.org/, Base URL 2 https://www.jstor.org/, Journal ID yyyy, Volume 33", au.getName());
  }
  
  private static final String gln_message = 
      "JSTOR hosts this archival unit (AU) " +
          "and may require you to register the IP address "+
          "of this LOCKSS box as a crawler. For more information, visit the <a " +
          "href=\'http://www.lockss.org/support/use-a-lockss-box/adding-titles/publisher-ip-address-registration-contacts-for-global-lockss-network/\'>" +
          "LOCKSS IP address registration page</a>.";
 
  public void testConfigUsrMsg() throws Exception {
    // a clockssAU
    ArchivalUnit jsClockssAu = makeAu(true, 137, "tranamerentosoc3", true);
    assertEquals(null, jsClockssAu.getProperties().getString(DefinableArchivalUnit.KEY_AU_CONFIG_USER_MSG, null));

    ArchivalUnit jsGLNAu = makeAu(true, 137, "tranamerentosoc3", false);
    assertEquals(gln_message, jsGLNAu.getProperties().getString(DefinableArchivalUnit.KEY_AU_CONFIG_USER_MSG, null));
  }
  
  List<String> substanceList = ListUtil.list(
  ROOT_URL+"stable/pdf/11.1111/1234-abc.12G.pdf",
  ROOT_URL+"stable/pdfplus/11.1111/1234-abc.12G.pdf",
  ROOT_URL+"stable/pdf/1234567.pdf",
  ROOT_URL+"stable/pdfplus/1234567.pdf",
  ROOT_URL+"stable/full/11.1111/1234-abc.12G",
  ROOT_URL+"stable/full/1234567");
  
  List<String> notSubstanceList = ListUtil.list(
  ROOT_URL+"action/showToc?journalCode=xxxx&issue=1%2F2&volume=123",    
  ROOT_URL+"stable/media/11.1111/1234-abc.12G",
  ROOT_URL+"stable/select/11.1111/1234-abc.12G",
  ROOT_URL2+"action/downloadSingleCitationSec?format=refman&doi=11.1111/1234-abc.12G",
  ROOT_URL2+"action/downloadSingleCitationSec?format=refman&doi=10.2307/41827175",
  ROOT_URL+"jawr/1105425977/bundles/jstorSiteCatalyst.js",
  ROOT_URL+"literatum/publisher/jstor/journals/content/jmorahist/2013/jmorahist.13.1.issue-1/jmorahist.13.1.issue-1/20130523/jmorahist.13.1.issue-1.cover.jpg");
  
  public void testCheckSubstanceRules() throws Exception {
    boolean found;
    URL base = new URL(ROOT_URL);
    ArchivalUnit jsAu = makeAu(true, 137, "tranamerentosoc3", false);
    PatternMatcher matcher = RegexpUtil.getMatcher();   
    List<Pattern> patList = jsAu.makeSubstanceUrlPatterns();


    for (String nextUrl : substanceList) {
      log.debug3("testing for substance: "+ nextUrl);
      found = false;
      for (Pattern nextPat : patList) {
            found = matcher.matches(nextUrl, nextPat);
            if (found) break;
      }
      assertEquals(true,found);
    }
    
    for (String nextUrl : notSubstanceList) {
      log.debug3("testing for not substance: "+ nextUrl);
      found = false;
      for (Pattern nextPat : patList) {
            found = matcher.matches(nextUrl, nextPat);
            if (found) break;
      }
      assertEquals(false,found);
    }

  }

}

