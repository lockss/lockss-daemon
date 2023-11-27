/*

Copyright (c) 2000-2020, Board of Trustees of Leland Stanford Jr. University
All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are met:

1. Redistributions of source code must retain the above copyright notice,
this list of conditions and the following disclaimer.

2. Redistributions in binary form must reproduce the above copyright notice,
this list of conditions and the following disclaimer in the documentation
and/or other materials provided with the distribution.

3. Neither the name of the copyright holder nor the names of its contributors
may be used to endorse or promote products derived from this software without
specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
POSSIBILITY OF SUCH DAMAGE.

*/

package org.lockss.plugin.copernicus;

import java.net.URL;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.oro.text.regex.*;
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
public class TestCopernicusArchivalUnit extends LockssTestCase {
  private MockLockssDaemon theDaemon;
  static final String BASE_URL_KEY = ConfigParamDescr.BASE_URL.getKey();
  static final String HOME_URL_KEY = "home_url";
  static final String VOL_KEY = ConfigParamDescr.VOLUME_NAME.getKey();
  static final String YEAR_KEY = ConfigParamDescr.YEAR.getKey();

  static final String BASE_HOST = "www.test-cop.com"; //this is not a real url
  static final String HOME_HOST = "www.TestCopernicus.com"; //this is not a real url
  
  static final String ROOT_URL = "http://" + BASE_HOST + "/"; //this is not a real url
  static final String ROOT_HOME_URL = "http://" + HOME_HOST + "/"; //this is not a real url
  
  static Logger log = Logger.getLogger(TestCopernicusArchivalUnit.class);
  
  static final String PLUGIN_ID = "org.lockss.plugin.copernicus.CopernicusPublicationsPlugin";
  static final String PluginName = "Copernicus Publications Plugin";
  
  // need to call quotemeta on the base/home_urls because the RegexpUtil.regexpCollection 
  // call on the returning strings calls it (but only on the base/home_url params)
  static final String baseRepairList[] =
    {
    "^https?://(contentmanager|cdn)\\.copernicus\\.org/",
    "^https?://" + Perl5Compiler.quotemeta(BASE_HOST) + "/inc/[^/]+/[^/]+\\.(gif|png|css|js)$",
    "^https?://" + Perl5Compiler.quotemeta(HOME_HOST) + "/[^/]+\\.(gif|jpe?g|png|tif?f|css|js)$",
    };
  
  static final String journalRepairList[] =
    {
    "^https?://(contentmanager|cdn)\\.copernicus\\.org/",
    "^https?://" + Perl5Compiler.quotemeta(BASE_HOST) + "/inc/[^/]+/[^/]+\\.(gif|png|css|js)$",
    "^https?://" + Perl5Compiler.quotemeta(HOME_HOST) + "/[^/]+\\.(gif|jpe?g|png|tif?f|css|js)$",
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

  private DefinableArchivalUnit makeAu(URL url1, URL url2,
      int volume, String year)
      throws Exception {

    Properties props = new Properties();
    props.setProperty(VOL_KEY, Integer.toString(volume));
    props.setProperty(YEAR_KEY, year);
    if (url1 != null) {
      props.setProperty(BASE_URL_KEY, url1.toString());
    }
    if (url2 != null) {
      props.setProperty(HOME_URL_KEY, url2.toString());
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
      makeAu(null, null, 1, "2012");
      fail("Should have thrown ArchivalUnit.ConfigurationException");
    } catch (ArchivalUnit.ConfigurationException e) {
    }
  }
  
  //
  // Test the crawl rules
  //
  public void testShouldCacheProperPages() throws Exception {
    URL base = new URL(ROOT_URL);
    URL home = new URL(ROOT_HOME_URL);
    ArchivalUnit ABAu = makeAu(base, home, 123, "2012");
    theDaemon.getLockssRepository(ABAu);
    theDaemon.getNodeManager(ABAu);
    BaseCachedUrlSet cus = new BaseCachedUrlSet(ABAu,
        new RangeCachedUrlSetSpec(base.toString()));
    // Test for pages that should get crawled
    //start page
    shouldCacheTest(ROOT_URL+"123/index.html", true, ABAu, cus);    
    //TOC - same as start page, or 
    shouldCacheTest(ROOT_URL+"123/issue5.html", true, ABAu, cus);
    // articles - various allowed formats
    shouldCacheTest(ROOT_URL+"123/14/tc-123-18-2012.html", true, ABAu, cus);
    shouldCacheTest(ROOT_URL+"123/14/tc-123-18-2012.pdf", true, ABAu, cus);
    shouldCacheTest(ROOT_URL+"123/14/tc-123-18-2012.xml", true, ABAu, cus);
    shouldCacheTest(ROOT_URL+"123/14/tc-123-18-2012.bib", true, ABAu, cus);
    shouldCacheTest(ROOT_URL+"123/14/tc-123-18-2012.ris", true, ABAu, cus);
    shouldCacheTest(ROOT_URL+"123/14/tc-123-18-2012-supplement.pdf", true, ABAu, cus);

    //home_url stuff
    shouldCacheTest(ROOT_HOME_URL+"foo/blah/whatsit.css", true, ABAu, cus);
    
    // Now a couple that shouldn't get crawled
    // wrong volume
    shouldCacheTest(ROOT_URL+"12/index.html", false, ABAu, cus);
    // metrics page
    shouldCacheTest(ROOT_URL+"123/14/tc-123-18-2012-metrics.html", false, ABAu, cus);
    // related articles
    shouldCacheTest(ROOT_URL+"123/14/tc-123-18-2012-relations.html", false, ABAu, cus);
    // discussion/"peer review"
    shouldCacheTest(ROOT_URL+"123/14/tc-123-18-2012-discussion.html", true, ABAu, cus);

  }
  
  private void shouldCacheTest(String url, boolean shouldCache,
      ArchivalUnit au, CachedUrlSet cus) {
    assertEquals(shouldCache, au.shouldBeCached(url));
  }
  
  public void testStartUrlConstruction() throws Exception {
    URL url = new URL(ROOT_URL);

    // 4 digit
    String expected = ROOT_URL+"123/index.html";
    String expected2 = ROOT_URL+"articles/123/index.html";
    DefinableArchivalUnit ABAu = makeAu(url, url, 123, "2010");
    assertEquals(ListUtil.list(expected, expected2), ABAu.getStartUrls());
  }
  
  public void testShouldNotCachePageFromOtherSite() throws Exception {
    URL base = new URL("http://www.BaseAtypon.com/");
    int volume = 123;
    ArchivalUnit ABAu = makeAu(base, base, volume, "2020");

    theDaemon.getLockssRepository(ABAu);
    theDaemon.getNodeManager(ABAu);
    CachedUrlSetSpec spec = new RangeCachedUrlSetSpec(base.toString());
    BaseCachedUrlSet cus = new BaseCachedUrlSet(ABAu, spec);

    assertFalse(ABAu.shouldBeCached(
        "http://shadow2.stanford.edu/123/index.html"));
  }

  public void testgetName() throws Exception {
    DefinableArchivalUnit au =
      makeAu(new URL("http://www.sci-dril.net/"), new URL("http://www.scientific-drilling.net"), 33, "1989");
    assertEquals(PluginName + ", Base URL http://www.sci-dril.net/, Home URL http://www.scientific-drilling.net, Year 1989, Volume 33", au.getName());
  }

  public void testRepairList() throws Exception {
    URL base = new URL(ROOT_URL);
    URL home = new URL(ROOT_HOME_URL);
    ArchivalUnit ABAu = makeAu(base, home, 123, "2012");
    theDaemon.getLockssRepository(ABAu);
    theDaemon.getNodeManager(ABAu);
    assertEquals(Arrays.asList(baseRepairList), RegexpUtil.regexpCollection(ABAu.makeRepairFromPeerIfMissingUrlPatterns()));

    // make sure that's the regexp that will match to the expected url string
    // this also tests the regexp (which is the same) for the weighted poll map
    List <String> repairList = ListUtil.list(   
      ROOT_URL+"inc/amt/xml_fulltext_icon.gif",
      ROOT_URL+"inc/amt/xml_icon.png",
      ROOT_URL+"inc/amt/max1140.css",
      ROOT_URL+"inc/amt/jquery-1.9.0.min.js",
      ROOT_HOME_URL+"AMT_figure_amt-7-4267-2014-f10_50x50.png",
      ROOT_HOME_URL+"base_print_new.css",
      ROOT_HOME_URL+"check-icon.png",
      ROOT_HOME_URL+"co_auth_check.js",
      ROOT_HOME_URL+"graphic_red_close_button.gif"
     );
    
    Pattern p0 = Pattern.compile(baseRepairList[0]);
    Pattern p1 = Pattern.compile(baseRepairList[1]);
    Pattern p2 = Pattern.compile(baseRepairList[2]);
    Matcher m0, m1, m2;
    for (String urlString : repairList) {
      m0 = p0.matcher(urlString);
      m1 = p1.matcher(urlString);
      m2 = p2.matcher(urlString);
      assertEquals(urlString, true, m0.find() || m1.find() || m2.find());
    }
    
    //and this one should fail - it needs to be weighted correctly and repaired from publisher if possible
    String notString =ROOT_HOME_URL+"missing_file.html";
    m0 = p0.matcher(notString);
    m1 = p1.matcher(notString);
    assertEquals(false, m0.find() && m1.find());

   PatternFloatMap urlPollResults = ABAu.makeUrlPollResultWeightMap();
   assertNotNull(urlPollResults);
   for (String urlString : repairList) {
     assertEquals(0.0,
         urlPollResults.getMatch(urlString, (float) 1),
         .0001);
   }
   assertEquals(1.0, urlPollResults.getMatch(notString, (float) 1), .0001);
   
  }
}

