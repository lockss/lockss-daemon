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

package org.lockss.plugin.elifesciences;

import java.net.URL;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.lockss.config.Configuration;
import org.lockss.daemon.*;
import org.lockss.plugin.*;
import org.lockss.plugin.base.BaseCachedUrlSet;
import org.lockss.plugin.definable.*;
import org.lockss.state.SubstanceChecker;
import org.lockss.test.*;
import org.lockss.util.*;

public class TestELifeArchivalUnit extends LockssTestCase {
  private MockLockssDaemon theDaemon;
  static final String BASE_URL_KEY = ConfigParamDescr.BASE_URL.getKey();
  static final String YM_KEY = "year_month";
  static final String ROOT_URL = "http://www.jrnl.com/"; //this is not a real url

  private static final Logger log = Logger.getLogger(TestELifeArchivalUnit.class);

  static final String PLUGIN_ID = "org.lockss.plugin.elifesciences.ELifeSciencesPlugin";
  static final String PluginName = "eLife Sciences Journals Plugin (Legacy 2016)";
  
  static final String  ELIFE_REPAIR_FROM_PEER_REGEXP1 = "sites/default/files/(css|js)/(css|js)_[^/]+\\.(css|js)$";
//  static final String  ELIFE_REPAIR_FROM_PEER_REGEXP2 = "\\.(mp4)$";
  
  public void setUp() throws Exception {
    super.setUp();
    setUpDiskSpace();
    theDaemon = getMockLockssDaemon();
    theDaemon.getHashService();
  }

  public void tearDown() throws Exception {
    super.tearDown();
  }

  private DefinableArchivalUnit makeAu(URL base_url, String year_month)
      throws Exception {

    Properties props = new Properties();
    props.setProperty(YM_KEY, year_month);
    if (base_url != null) {
      props.setProperty(BASE_URL_KEY, base_url.toString());
    }
    Configuration config = ConfigurationUtil.fromProps(props);

    DefinablePlugin ap = new DefinablePlugin();
    ap.initPlugin(getMockLockssDaemon(), PLUGIN_ID);
    DefinableArchivalUnit au = (DefinableArchivalUnit)ap.createAu(config);
    return au;
  }

  //
  // Test the crawl rules
  //
 
  public void testShouldCacheProperPages() throws Exception {
    String REAL_ROOT= "http://elifesciences.org/";
    String OTHER_ROOT = "https://elife-publishing-cdn.s3.amazonaws.com/";
    String OTHER_OTHER_ROOT = "http://cdn.elifesciences.org/";

    URL base = new URL(REAL_ROOT);
    ArchivalUnit  msau = makeAu(base, "2015/04");
    theDaemon.getLockssRepository(msau);
    theDaemon.getNodeManager(msau);
    BaseCachedUrlSet cus = new BaseCachedUrlSet(msau,
        new RangeCachedUrlSetSpec(base.toString()));

    //manifest page and TOC
    shouldCacheTest(REAL_ROOT+"archive/2015/04", true, msau, cus);    
    
    //pdf links    
    shouldCacheTest(OTHER_OTHER_ROOT+"elife-articles/06397/figures-pdf/elife06397-figures.pdf", true, msau, cus);    
    
    //toc contents
    shouldCacheTest(REAL_ROOT+"content/4/e12523v1", true, msau,cus);
    shouldCacheTest(OTHER_OTHER_ROOT+"12523/index.html", true, msau,cus);

    // images (etc.) 
    shouldCacheTest(OTHER_ROOT+"02777/elife-02777-fig1-figsupp1-v2-480w.jpg", true, msau, cus);
    shouldCacheTest(REAL_ROOT+"profiles/elife_profile/libraries/cluetip/images/arrowleft.gif", true, msau, cus);
    shouldCacheTest(REAL_ROOT+"misc/progress.gif", true, msau, cus);
       
    
    //excluded
    shouldCacheTest(REAL_ROOT+"panels_ajax_tab/elife_article_figdata/node:51048/1", false, msau,cus);    
    shouldCacheTest(REAL_ROOT+"panels_ajax_tab/elife_article/node:97986/0", false, msau,cus);    

  }
  
  public void testSubstancePattern() throws Exception {
	ArchivalUnit  msau = makeAu(new URL(ROOT_URL), "2015/04");
	SubstanceChecker checker = new SubstanceChecker(msau);
    
    assertFalse(checker.isSubstanceUrl(ROOT_URL + "content/4/e05959"));
    assertTrue(checker.isSubstanceUrl(ROOT_URL + "content/4/e05959-download.pdf"));
  }

  private void shouldCacheTest(String url, boolean shouldCache,
      ArchivalUnit au, CachedUrlSet cus) {
    assertEquals(shouldCache, au.shouldBeCached(url));
  }

  public void testStartUrlConstruction() throws Exception {
    URL url = new URL(ROOT_URL);

    // 4 digit
    String expected = ROOT_URL +"archive/2014/04";
 
    DefinableArchivalUnit au = makeAu(url, "2014/04");
    assertEquals(ListUtil.list(expected), au.getStartUrls());
  }


  public void testGetName() throws Exception {
    DefinableArchivalUnit au =
        makeAu(new URL("http://www.ajrnl.com/"), "2012/03");
    //eLife Sciences Plugin, Base URL %s, Month %s
    assertEquals(PluginName + ", Base URL http://www.ajrnl.com/, Month 2012/03", au.getName());
  }
  
  public void testPollSpecial() throws Exception {
    String REAL_ROOT= "http://elifesciences.org/";
    String OTHER_ROOT = "https://elife-publishing-cdn.s3.amazonaws.com/";
    String OTHER_OTHER_ROOT = "http://cdn.elifesciences.org/";
    
    URL base = new URL(REAL_ROOT);
    ArchivalUnit  au = makeAu(base, "2015/04");
    
    // if it changes in the plugin, you will likely need to change the test, so verify
    assertEquals(ListUtil.list(
        ELIFE_REPAIR_FROM_PEER_REGEXP1),
        RegexpUtil.regexpCollection(au.makeRepairFromPeerIfMissingUrlPatterns()));
    
    // make sure that's the regexp that will match to the expected url string
    // this also tests the regexp (which is the same) for the weighted poll map
    List <String> repairList = ListUtil.list(
        REAL_ROOT + "sites/default/files/css/css_xE-rWrJf-fncB6ztZfd2huxqgxu4WO-qwma6Xer30m4.css",
        REAL_ROOT + "sites/default/files/js/js_9xhttD4CLtluGwHnuEz_uVz7OnsXLXJ3WZVHYkHd3kI.js",
        OTHER_ROOT + "sites/default/files/css/css_xE-rWrJf-fncB6ztZfd2huxqgxu4WO-qwma6Xer30m4.css",
        OTHER_ROOT + "sites/default/files/js/js_9xhttD4CLtluGwHnuEz_uVz7OnsXLXJ3WZVHYkHd3kI.js",
        OTHER_OTHER_ROOT + "sites/default/files/css/css_xE-rWrJf-fncB6ztZfd2huxqgxu4WO-qwma6Xer30m4.css",
        OTHER_OTHER_ROOT + "sites/default/files/js/js_9xhttD4CLtluGwHnuEz_uVz7OnsXLXJ3WZVHYkHd3kI.js");
    Pattern p = Pattern.compile(ELIFE_REPAIR_FROM_PEER_REGEXP1);
    for (String urlString : repairList) {
      Matcher m = p.matcher(urlString);
      assertEquals(urlString, true, m.find());
    }
    
    //and this one should fail - it needs to be weighted correctly and repaired from publisher if possible
    String notString = REAL_ROOT + "print.css";
    Matcher m = p.matcher(notString);
    assertEquals(false, m.find());
    
    PatternFloatMap urlPollResults = au.makeUrlPollResultWeightMap();
    assertNotNull(urlPollResults);
    for (String urlString : repairList) {
      assertEquals(0.0, urlPollResults.getMatch(urlString, (float) 1), .0001);
    }
    assertEquals(1.0, urlPollResults.getMatch(notString, (float) 1), .0001);
  }
  
}

