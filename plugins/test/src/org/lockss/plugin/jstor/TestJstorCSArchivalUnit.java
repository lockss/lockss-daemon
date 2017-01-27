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
  ROOT_URL+"stable/pdf/11.1111/1234-abc.12G.pdf");
  
  List<String> notSubstanceList = ListUtil.list(
  ROOT_URL+"assets/legacy_20170113T1556/files/legacy/css");    
  
  public void testCheckSubstanceRules() throws Exception {
    boolean found;
    URL base = new URL(ROOT_URL);
    ArchivalUnit jsAu = makeAu("tranamerentosoc3", "2015", false);
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

