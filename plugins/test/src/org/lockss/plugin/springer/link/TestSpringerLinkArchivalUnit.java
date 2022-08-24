/*

Copyright (c) 2000-2022, Board of Trustees of Leland Stanford Jr. University

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

package org.lockss.plugin.springer.link;

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
public class TestSpringerLinkArchivalUnit extends LockssTestCase {
  private MockLockssDaemon theDaemon;
  static final String BASE_URL_KEY = ConfigParamDescr.BASE_URL.getKey();
  static final String DOWNLOAD_URL_KEY = "download_url";
  static final String VOL_KEY = ConfigParamDescr.VOLUME_NAME.getKey();
  static final String ISSN_KEY = "journal_eissn";
  static final String BASE_URL = "https://link.test.com/"; //this is not a real url
  static final String DOWNLOAD_URL = "http://download.test.com/"; //this is not a real url
  static final String PERMITTED_HOST_URL = "https://static-content.springer.com/";
  
  static Logger log = Logger.getLogger(TestSpringerLinkArchivalUnit.class);
  
  static final String PLUGIN_ID = "org.lockss.plugin.springer.link.SpringerLinkJournalsPlugin";
  static final String PluginName = "SpringerLink Journals Plugin";
  
  static final String baseRepairList[] =
    {
        "/(springerlink-)?static/.*\\.(png|css|js|gif|ico)$",
        "^https://static-content\\.springer\\.com/cover/",
    };
  
  static final String journalRepairList[] =
    {
        "/(springerlink-)?static/.*\\.(png|css|js|gif|ico)$",
        "/article/[^/]+/[^/\\.]+/fulltext\\.html$",
        "^https://static-content\\.springer\\.com/cover/",
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

  private DefinableArchivalUnit makeAu(URL url1, URL url2, String eissn, int volume)
      throws Exception {

    Properties props = new Properties();
    props.setProperty(ISSN_KEY, eissn);
    props.setProperty(VOL_KEY, Integer.toString(volume));
    if (url1 != null) {
      props.setProperty(BASE_URL_KEY, url1.toString());
    }
    if (url2 != null) {
      props.setProperty(DOWNLOAD_URL_KEY, url2.toString());
    }
    Configuration config = ConfigurationUtil.fromProps(props);
    
    DefinablePlugin ap = new DefinablePlugin();
    ap.initPlugin(getMockLockssDaemon(), PLUGIN_ID);
    DefinableArchivalUnit au = (DefinableArchivalUnit)ap.createAu(config);
    return au;
  }

  public void testConstructNullUrl() throws Exception {
    try {
      makeAu(null, null, "0000-000", 13);
      fail("Should have thrown ArchivalUnit.ConfigurationException");
    } catch (ArchivalUnit.ConfigurationException e) {
    }
  }
  
  //
  // Test the crawl rules
  //
  public void testShouldCacheProperPages() throws Exception {
    URL base = new URL(BASE_URL);
    URL home = new URL(DOWNLOAD_URL);
    ArchivalUnit ABAu = makeAu(base, home, "1234-1234", 5);
    theDaemon.getLockssRepository(ABAu);
    theDaemon.getNodeManager(ABAu);
    BaseCachedUrlSet cus = new BaseCachedUrlSet(ABAu,
        new RangeCachedUrlSetSpec(base.toString()));
    // Test for pages that should get crawled
    //start page
    shouldCacheTest(BASE_URL+"lockss.txt", true, ABAu, cus);

    // articles - various allowed formats
    //BASE_URLcontent/pdf/10.1007%2Fs13238-012-2022-9.pdf
    //BASE_URLarticle/10.1007/s13238-012-2927-3
    //https://static-content.springer.com/cover/journal/13238/3/12.jpg

    shouldCacheTest(BASE_URL+"content/pdf/10.1007%2Fs13238-012-2022-9.pdf", true, ABAu, cus);
    shouldCacheTest(BASE_URL+"article/10.1007/s13238-012-2927-3", true, ABAu, cus);
  //  shouldCacheTest(DOWNLOAD_URL+"article/10.1007/s13238-012-2927-3", true, ABAu, cus);

    
    // Now a couple that shouldn't get crawled
    // wrong volume
    shouldCacheTest("https://lab.test.com/static/1908712780/images/favicon/app-icon-ipad.png", false, ABAu, cus);
    // metrics page
    shouldCacheTest(BASE_URL+"contactus?previousUrl=https%3A//link.Test.com/article/10.1007/s10008-011-1349-0", false, ABAu, cus);
    // related articles
    shouldCacheTest(BASE_URL+"journal/10008", false, ABAu, cus);
    // discussion/"peer review"
    shouldCacheTest("^"+BASE_URL+"static/201606100928-13/css/%22data:image/svg+xml", false, ABAu, cus);
  }
  
  private void shouldCacheTest(String url, boolean shouldCache,
      ArchivalUnit au, CachedUrlSet cus) {
    assertEquals(shouldCache, au.shouldBeCached(url));
  }
  
  public void testStartUrlConstruction() throws Exception {
    URL url = new URL(BASE_URL);

    // 4 digit
    String expected = BASE_URL+"123/index.html";
    DefinableArchivalUnit ABAu = makeAu(url, url, "1234-1234", 41);
    System.out.println(ABAu.getStartUrls());
    //assertEquals(ListUtil.list(expected), ABAu.getStartUrls());
  }
  
  public void testShouldNotCachePageFromOtherSite() throws Exception {
    URL base = new URL(BASE_URL);
    URL home = new URL(DOWNLOAD_URL);

    int volume = 123;
    ArchivalUnit ABAu = makeAu(base, home, "0011-2233", volume);

    theDaemon.getLockssRepository(ABAu);
    theDaemon.getNodeManager(ABAu);
    CachedUrlSetSpec spec = new RangeCachedUrlSetSpec(base.toString());
    BaseCachedUrlSet cus = new BaseCachedUrlSet(ABAu, spec);

    assertFalse(ABAu.shouldBeCached(
        "http://shadow2.stanford.edu/123/index.html"));
  }

  public void testgetName() throws Exception {
    DefinableArchivalUnit au =
      makeAu(new URL("http://www.sci-dril.net/"), new URL("http://www.scientific-drilling.net"), "0000-1111", 33);
    System.out.println("AU= "+au.getName() );
    //assertEquals(PluginName + ", Base URL http://www.sci-dril.net/, Home URL http://www.scientific-drilling.net, Year 1989, Volume 33", au.getName());
  }

  public void testRepairList() throws Exception {
    URL base = new URL(BASE_URL);
    URL home = new URL(DOWNLOAD_URL);
    ArchivalUnit ABAu = makeAu(base, home, "1234-1234", 123);
    theDaemon.getLockssRepository(ABAu);
    theDaemon.getNodeManager(ABAu);
    assertEquals(Arrays.asList(baseRepairList), RegexpUtil.regexpCollection(ABAu.makeRepairFromPeerIfMissingUrlPatterns()));

    // make sure that's the regexp that will match to the expected url string
    // this also tests the regexp (which is the same) for the weighted poll map
    List <String> repairList = ListUtil.list(   
      BASE_URL+"static/js/lib/jquery.min.js",
      BASE_URL+"static/201606100928-13/js/all.js",
      BASE_URL+"static/201606100928-13/css/springer_casper.min.css",
      BASE_URL+"static/js/webtrekk/webtrekk_v3.js",
      BASE_URL+"springerlink-static/1057859635/js/main.js",
      BASE_URL+"springerlink-static/625140479/css/styles.css",
      BASE_URL+"static/201606100928-13/sites/link/images/apple-touch-icon-72x72-precomposed.png",
      BASE_URL+"springerlink-static/123456789/images/favicon/favicon.ico"
      
     );
    List <String> repairList1 = ListUtil.list(   
        PERMITTED_HOST_URL+"cover/journal/11356/20/3.jpg"
       );
    
    Pattern p0 = Pattern.compile(baseRepairList[0]);
    Matcher m0;
    for (String urlString : repairList) {
      m0 = p0.matcher(urlString);
      assertEquals(true, m0.find());
    }
    
    p0 = Pattern.compile(baseRepairList[1]);
    for (String urlString : repairList1) {
      m0 = p0.matcher(urlString);      
      assertEquals(true, m0.find());
    }
    
    //and this one should fail - it needs to be weighted correctly and repaired from publisher if possible
    String notString =DOWNLOAD_URL+"missing_file.html";
    m0 = p0.matcher(notString);
    assertEquals(false, m0.find());

   PatternFloatMap urlPollResults = ABAu.makeUrlPollResultWeightMap();
   assertNotNull(urlPollResults);
   for (String urlString : repairList) {
     assertEquals(0.0, urlPollResults.getMatch(urlString, (float) 1), .0001);
   }
   for (String urlString : repairList1) {
     assertEquals(0.0, urlPollResults.getMatch(urlString, (float) 1), .0001);
   }
   assertEquals(1.0, urlPollResults.getMatch(notString, (float) 1), .0001);
   
  }
}

