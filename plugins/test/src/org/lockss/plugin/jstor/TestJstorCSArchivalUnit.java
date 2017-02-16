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
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
public class TestJstorCSArchivalUnit extends LockssTestCase {
  private MockLockssDaemon theDaemon;
  static final String BASE_URL_KEY = ConfigParamDescr.BASE_URL.getKey();
  static final String JID_KEY = ConfigParamDescr.JOURNAL_ID.getKey();
  static final String YEAR_KEY = ConfigParamDescr.YEAR.getKey();
  static final String ROOT_URL = "http://www.jstor.org/"; 
  
  static Logger log = Logger.getLogger(TestJstorCSArchivalUnit.class);
  
  static final String PLUGIN_ID = "org.lockss.plugin.jstor.JstorCurrentScholarshipPlugin";
  static final String CLOCKSS_PLUGIN_ID = "org.lockss.plugin.jstor.ClockssJstorCurrentScholarshipPlugin";
  static final String PluginName = "JSTOR Current Scholarship Plugin";

  // fudge this a little bit to match what the daemon generates from the plugin pattern
  // to whit: escape everything in the base_url and
  // turn the &amp; to &
  static final String jstorSubstanceList[] = 
    {
    "^http\\:\\/\\/www\\.jstor\\.org\\/stable/pdf/([.0-9]+/)?[^/?&]+\\.pdf$",
    };
  static final String jstorRepairList[] = 
    {
    "://[^/]+/assets/.+\\.(css|gif|jpe?g|js|png)(_v[0-9]+)?$",
    "://assets\\.adobedtm\\.com/.+\\.(css|gif|jpe?g|js|png)$",    
    };  
  
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
  private DefinableArchivalUnit makeAu(String jid, String year) 
    throws Exception {
      return makeAu(jid, year);
  }

  // A more flexible version of the makeAU routine that allows choice of plugin
  private DefinableArchivalUnit makeAu(String jid, String year, boolean isClockss)
      throws Exception {

    Properties props = new Properties();
    props.setProperty(YEAR_KEY, year);
    props.setProperty(JID_KEY, jid);
    props.setProperty(BASE_URL_KEY, ROOT_URL);
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
  
  //
  // Test the crawl rules
  //
  public void testShouldCacheProperPages() throws Exception {
    ArchivalUnit JSAu = makeAu("xxxx", "2015",true);
    theDaemon.getLockssRepository(JSAu);
    theDaemon.getNodeManager(JSAu);
    BaseCachedUrlSet cus = new BaseCachedUrlSet(JSAu,
        new RangeCachedUrlSetSpec(ROOT_URL));
    // Test for pages that should get crawled
    //manifest page
    //http://www.jstor.org/clockss-manifest/hesperia/2015
    shouldCacheTest(ROOT_URL+"clockss-manifest/xxxx/2015", true, JSAu, cus);    

    // YES: toc page reached off a manifest page
    //http://www.jstor.org/stable/10.2972/hesperia.84.issue-3
    //http://www.jstor.org/stable/i40044030
    shouldCacheTest(ROOT_URL+"stable/10.2972/xxxx.84.issue-3", true, JSAu, cus);    
    shouldCacheTest(ROOT_URL+"stable/10.2972/i40044030", true, JSAu, cus);
    // alternate version of legacy index on prev/next links
    shouldCacheTest(ROOT_URL+"stable/i40044030", true, JSAu, cus);    

    // article pages
    //http://www.jstor.org/stable/10.2972/hesperia.84.3.0515
    //http://www.jstor.org/stable/pdf/10.2972/hesperia.84.3.0515.pdf
    //http://www.jstor.org/doi/xml/10.2972/hesperia.84.3.0515
    //http://www.jstor.org/stable/40981057
    //http://www.jstor.org/stable/10.2307/40981054
    //http://www.jstor.org/stable/pdf/40981057.pdf
    //http://www.jstor.org/doi/xml/10.2307/40981057
    shouldCacheTest(ROOT_URL+"stable/10.2972/xxxx.84.3.0515", true, JSAu, cus);
    shouldCacheTest(ROOT_URL+"stable/pdf/10.2972/xxxx.84.3.0515.pdf", true, JSAu, cus);
    shouldCacheTest(ROOT_URL+"doi/xml/10.2972/xxxx.84.3.0515", true, JSAu, cus);
    // old style article pages from hesperia
    shouldCacheTest(ROOT_URL+"stable/40981057", true, JSAu, cus);
    shouldCacheTest(ROOT_URL+"stable/10.2307/40981054", true, JSAu, cus);
    shouldCacheTest(ROOT_URL+"stable/pdf/40981057.pdf", true, JSAu, cus);
    shouldCacheTest(ROOT_URL+"doi/xml/10.2307/40981057", true, JSAu, cus);    
    
    // YES -version of PDF with argument -- it will get swallowed as redirect
    shouldCacheTest(ROOT_URL+"stable/pdf/11.1111/xxxx.-abc.12G.pdf?acceptTC=true&coverpage=false", true, JSAu, cus);
    
    //american journal of archaeology uses alernative to journal_id as doi component "aja" not "amerjarch"
    shouldCacheTest(ROOT_URL+"stable/10.3764/aja.120.4.0603", true, JSAu, cus);
    shouldCacheTest(ROOT_URL+"stable/pdf/10.3764/aja.120.4.0603.pdf", true, JSAu, cus);
    
    //image from full-text - this url is consistent for this image
    // came from http://www.jstor.org/stable/10.5325/critphilrace.3.1.0028
    shouldCacheTest(ROOT_URL + "stable/get_asset/10.5325/critphilrace.3.1.0028?path=czM6Ly9zZXF1b2lhLWNlZGFyL2NpZC1wcm9kLTEvNzQ3YzEyNWMvMzRlMy8zMWExLzg2ZDcvZDA2OGMwZTBiMTRlL2NyaXRwaGlscmFjZS4zLjEuaXNzdWUtMS9jcml0cGhpbHJhY2UuMy4xLjAwMjgvZ3JhcGhpYy9jcml0cGhpbHJhY2UuMy4xLjAwMjgtZjAxLnRpZl9fLi5NRURJVU0uR0lG", true, JSAu, cus);
    
    // YES - citation download information 
    //http://www.jstor.org/citation/info/10.2972/hesperia.84.3.0515
    //http://www.jstor.org/citation/info/10.2307/40981057
    //http://www.jstor.org/citation/ris/10.2307/40981057
    shouldCacheTest(ROOT_URL+"citation/info/10.2972/xxxx.84.3.0515", true, JSAu, cus);
    shouldCacheTest(ROOT_URL+"citation/info/10.2307/40981057", true, JSAu, cus);
    shouldCacheTest(ROOT_URL+"citation/ris/10.2307/40981057", true, JSAu, cus);
    shouldCacheTest(ROOT_URL+"citation/text/10.2307/40981057", true, JSAu, cus);
    //but not
    shouldCacheTest(ROOT_URL+"citation/refworks/10.2307/40981057", false, JSAu, cus);
    
    // support files
    //http://www.jstor.org/px/xhr/api/v1/collector/pxPixel.gif?appId=PXu4K0s8nX
    shouldCacheTest(ROOT_URL+"px/xhr/api/v1/collector/pxPixel.gif?appId=PXu4K0s8nX", true, JSAu, cus);
    //cdn?
    shouldCacheTest("http://assets.adobedtm.com/e0b918ad/satelliteLib-5c3c81244f0b.js", true, JSAu, cus);
    //http://assets.adobedtm.com/e0b918adcf7233db110ce33e1416a2e6448e08e6/satelliteLib-5c3c84854607a1fc3328b9b539472ebd81244f0b.js
    
    // Now a couple that shouldn't get crawled
    // avoid spider trap
    shouldCacheTest(ROOT_URL+"stable/11.1111/99.9999-99999", false, JSAu, cus);
    // LOCKSS
    shouldCacheTest("http://lockss.stanford.edu", false, JSAu, cus);
    // other sites
  }
  
  private void shouldCacheTest(String url, boolean shouldCache,
      ArchivalUnit au, CachedUrlSet cus) {
    assertEquals(shouldCache, au.shouldBeCached(url));
  }
  

  
  List<String> substanceList = ListUtil.list(
  ROOT_URL+"stable/pdf/40025106.pdf");
  
  List<String> notSubstanceList = ListUtil.list(
  ROOT_URL+"doi/xml/10.2307/40026123",
  ROOT_URL + "stable/10.5325/goodsociety.24.1.0049",
  ROOT_URL + "citation/text/10.5325/chaucerrev.49.4.0427",
  ROOT_URL + "stable/10.5325/chaucerrev.50.1-2.0055");    
  
  public void testCheckSubstanceRules() throws Exception {
    boolean found;
    ArchivalUnit jsAu = makeAu("hesperia", "2015", false);

    assertEquals(Arrays.asList(jstorSubstanceList),
        RegexpUtil.regexpCollection(jsAu.makeSubstanceUrlPatterns()));
    Pattern p0 = Pattern.compile(jstorSubstanceList[0]);
    Matcher m0;

    for (String nextUrl : substanceList) {
      log.debug3("testing for substance: "+ nextUrl);
        m0 = p0.matcher(nextUrl);
        assertEquals(nextUrl, true, m0.find());
      }

    
    for (String nextUrl : notSubstanceList) {
      log.debug3("testing for not substance: "+ nextUrl);
        m0 = p0.matcher(nextUrl);
        assertEquals(false, m0.find());
    }

  }
  
  public void testPollSpecial() throws Exception {
    ArchivalUnit FooAu = makeAu("xxxx", "2015",true);
    theDaemon.getLockssRepository(FooAu);
    theDaemon.getNodeManager(FooAu);

    // if it changes in the plugin, you might need to change the test, so verify
    assertEquals(Arrays.asList(jstorRepairList),
        RegexpUtil.regexpCollection(FooAu.makeRepairFromPeerIfMissingUrlPatterns()));

    // make sure that's the regexp that will match to the expected url string
    // this also tests the regexp (which is the same) for the weighted poll map
    List <String> repairList = ListUtil.list(
        "http://www.jstor.org/assets/legacy_20170215T1357/files/legacy/css/legacy.css",
        "http://www.jstor.org/assets/legacy_20170215T1357/files/legacy/js/legacy.min.js",
        "http://www.jstor.org/assets/legacy_20170215T1357/files/shared/images/JSTOR_Logo_RGB_60x76.gif",
        "http://www.jstor.org/assets/legacy_20170215T1357/files/shared/images/MagnifyingGlass_22px.png",
        "http://www.jstor.org/assets/legacy_20170215T1357/files/shared/images/jstor_logo.jpg",
        "http://www.jstor.org/assets/toc-view_20170215T1026/files/shared/images/expand.gif",
        "http://www.jstor.org/assets/toc-view_20170215T1026/files/toc-view/css/toc-view.css",
        "http://www.jstor.org/assets/toc-view_20170215T1026/files/toc-view/js/toc-view.min.js",
        "http://assets.adobedtm.com/e0b918adcf7233db110ce33e1416a2e6448e08e6/satelliteLib-5c3c84854607a1fc3328b9b539472ebd81244f0b.js");

    Pattern p0 = Pattern.compile(jstorRepairList[0]);
    Pattern p1 = Pattern.compile(jstorRepairList[1]);
    Matcher m0, m1, m2;
    for (String urlString : repairList) {
      m0 = p0.matcher(urlString);
      m1 = p1.matcher(urlString);
      assertEquals(urlString, true, m0.find() || m1.find());
    }
    //this a consistent path to this resource
    String notString ="http://www.jstor.org/stable/get_asset/10.5325/critphilrace.3.1.0028?path=czM6Ly9zZXF1b2lhLWNlZGFyL2NpZC1wcm9kLTEvNzQ3YzEyNWMvMzRlMy8zMWExLzg2ZDcvZDA2OGMwZTBiMTRlL2NyaXRwaGlscmFjZS4zLjEuaXNzdWUtMS9jcml0cGhpbHJhY2UuMy4xLjAwMjgvZ3JhcGhpYy9jcml0cGhpbHJhY2UuMy4xLjAwMjgtZjAxLnRpZl9fLi5NRURJVU0uR0lG";
    m0 = p0.matcher(notString);
    m1 = p1.matcher(notString);
    assertEquals(false, m0.find() && m1.find());


    PatternFloatMap urlPollResults = FooAu.makeUrlPollResultWeightMap();
    assertNotNull(urlPollResults);
    for (String urlString : repairList) {
      assertEquals(0.0,
          urlPollResults.getMatch(urlString, (float) 1),
          .0001);
    }
    assertEquals(1.0, urlPollResults.getMatch(notString, (float) 1), .0001);
  }  

}

