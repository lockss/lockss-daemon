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

package org.lockss.plugin.bmc;

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
public class TestBioMedCentralPluginArchivalUnit extends LockssTestCase {
  private MockLockssDaemon theDaemon;
  static final String BASE_URL_KEY = ConfigParamDescr.BASE_URL.getKey();
  static final String JISSN_KEY = ConfigParamDescr.JOURNAL_ISSN.getKey();
  static final String VOL_KEY = ConfigParamDescr.VOLUME_NAME.getKey();
  static final String ROOT_URL = "http://www.biomed.com/"; //this is not a real url
  static final String ODD_ROOT_URL = "http://genbiomed.com/"; //this is not a real url
  
  static Logger log = Logger.getLogger(TestBioMedCentralPluginArchivalUnit.class);
  
  static final String PLUGIN_ID = "org.lockss.plugin.bmc.BioMedCentralPlugin";
  static final String PluginName = "BioMed Central Plugin (BMC Journals, Chemistry Central)";
  
  public void setUp() throws Exception {
    super.setUp();
    setUpDiskSpace();
    theDaemon = getMockLockssDaemon();
    theDaemon.getHashService();
  }

  public void tearDown() throws Exception {
    super.tearDown();
  }

  private DefinableArchivalUnit makeAu(URL url, String vname, String jissn)
      throws Exception {

    Properties props = new Properties();
    props.setProperty(VOL_KEY, vname);
    props.setProperty(JISSN_KEY, jissn);
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
      makeAu(null, "1", "1111-2222");
      fail("Should have thrown ArchivalUnit.ConfigurationException");
    } catch (ArchivalUnit.ConfigurationException e) {
    }
  }
  
  //
  // Test the crawl rules for the standard pages
  // biology-direct, v3, issn = 
  //
  
  private static final List<String> shouldList =  ListUtil.list(
      "http://www.biomed.com/bmcimages/article/opendata.gif",
      "http://www.biomed.com/content/3",
      "http://www.biomed.com/content/3/1/1",
      "http://www.biomed.com/content/3/1/1/abstract",
      "http://www.biomed.com/content/3/1/1/citation",
      "http://www.biomed.com/content/3/1/10/figure/F1",
      "http://www.biomed.com/content/3/1/10/figure/F1?highres=y",
      "http://www.biomed.com/content/3/1/11/mathml/M1",
      "http://www.biomed.com/content/3/1/11/suppl/S1",
      "http://www.biomed.com/content/3/1/18/mathml/M1",
      "http://www.biomed.com/content/3/1/20/table/T1",
      "http://www.biomed.com/content/3/1/35/abstract",
      "http://www.biomed.com/content/3/1/35/additional",
      "http://www.biomed.com/content/3/1/35/citation",
      "http://www.biomed.com/content/3/April/2008",
      "http://www.biomed.com/content/download/figures/1745-6150-3-10-1.jpeg",
      "http://www.biomed.com/content/download/supplementary/1745-6150-3-11-s1.mpg",
      "http://www.biomed.com/content/download/xml/1745-6150-3-1.xml",
      "http://www.biomed.com/content/figures/1745-6150-3-1-toc.gif",
      "http://www.biomed.com/content/inline/1745-6150-3-11-i1.gif",
      "http://www.biomed.com/content/pdf/1745-6150-3-1.pdf",
      "http://www.biomed.com/content/supplementary/1745-6150-3-11-S1-posterframe.jpg",
      "http://www.biomed.com/css/articles-0.css",
      "http://www.biomed.com/sites/10078/images/logo.gif"
      );
  
  private static final List<String> shouldNotList =  ListUtil.list(
      "http://www.biomed.com/content/4",
      "http://www.biomed.com/content/4/12",
      "http://lockss.stanford.edu/",
      "http://www.biomed.com/content/3/1/1email?from=standard"
      );
  public void testShouldCacheStandardPages() throws Exception {
    URL base = new URL(ROOT_URL);
    ArchivalUnit tau = makeAu(base, "3", "1745-6150");
    BaseCachedUrlSet cus = new BaseCachedUrlSet(tau,
        new RangeCachedUrlSetSpec(base.toString()));
    
    
    // Test for pages that should get crawled
    for (String shouldUrl : shouldList) {
      shouldCacheTest(shouldUrl, true, tau, cus); 
    }

    // Test for pages that should get crawled
    for (String shouldNotUrl : shouldNotList) {
      shouldCacheTest(shouldNotUrl, false, tau, cus); 
    }

  }
  
  //
  // Test the crawl rules for the oddball site, Genome-Biology
  //
  private static final List<String> GBshouldList =  ListUtil.list(
      "http://genbiomed.com/2009/10/1/101",
      "http://genbiomed.com/2009/10/1/101/abstract",
      "http://genbiomed.com/2009/10/1/101/citation",
      "http://genbiomed.com/2009/10/1/201/figure/F1",
      "http://genbiomed.com/2009/10/1/201/figure/F1?highres=y",
      "http://genbiomed.com/2009/10/1/R10/mathml/M1",
      "http://genbiomed.com/2009/10/1/R10/suppl/S2",
      "http://genbiomed.com/2009/10/1/R10/table/T2",
      "http://genbiomed.com/content/download/figures/gb-2009-10-1-201-1.eps"
      );
  private static final List<String> GBshouldNotList = ListUtil.list(
      "http://genbiomed.com/2009/10/12/115/email?from=standard"
      );
  public void testShouldCacheGBPages() throws Exception {
    URL base = new URL(ODD_ROOT_URL);
    ArchivalUnit tau = makeAu(base, "10", "1465-6906");
    //log.setLevel("debug3");
    BaseCachedUrlSet cus = new BaseCachedUrlSet(tau,
        new RangeCachedUrlSetSpec(base.toString()));
    // Test for pages that should get crawled
    for (String shouldUrl : GBshouldList) {
      shouldCacheTest(shouldUrl, true, tau, cus); 
    }

    // Test for pages that should get crawled
    for (String shouldNotUrl : GBshouldNotList) {
      shouldCacheTest(shouldNotUrl, false, tau, cus); 
    }


  }
  
  private void shouldCacheTest(String url, boolean shouldCache,
      ArchivalUnit au, CachedUrlSet cus) {
    log.debug3("testing: " + url);
    assertEquals(shouldCache, au.shouldBeCached(url));
  }

  public void testCheckSubstanceRules() throws Exception {
    URL base = new URL(ROOT_URL);
    ArchivalUnit tau = makeAu(base, "10", "1465-6906");
    PatternMatcher matcher = RegexpUtil.getMatcher();   
    List<Pattern> patList = tau.makeSubstanceUrlPatterns();
    //http://genomebiology.com/content/pdf/gb-2009-10-9-r101.pdf
    String testUrl = ROOT_URL + "content/pdf/gb-2009-10-9-r101.pdf";
    boolean found = false;
    for (Pattern nextPat : patList) {
          found = matcher.matches(testUrl, nextPat);
          if (found) break;
    }
    assertEquals(true,found);
    
    //"http://www.biomed.com/content/pdf/1745-6150-3-1.pdf",
    testUrl = ROOT_URL + "content/pdf/1465-6906-10-1.pdf";
    found = false;
    for (Pattern nextPat : patList) {
          found = matcher.matches(testUrl, nextPat);
          if (found) break;
    }
    assertEquals(true,found);
    
    //NOT substance - supplementary
    //"http://www.biomed.com/content/pdf/1745-6150-3-1.pdf",
    testUrl = ROOT_URL + "content/download/figures/1465-6906-10-11-1.pdf";
    found = false;
    for (Pattern nextPat : patList) {
          found = matcher.matches(testUrl, nextPat);
          if (found) break;
    }
    assertEquals(false,found);

    //NOT substance - wrong issn
    testUrl = ROOT_URL + "content/pdf/1745-6150-3-11-1.pdf";
    found = false;
    for (Pattern nextPat : patList) {
          found = matcher.matches(testUrl, nextPat);
          if (found) break;
    }
    assertEquals(false,found);
  }
  
}

