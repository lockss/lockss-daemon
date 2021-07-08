/*
 * $Id$
 */

/*

Copyright (c) 2000-2018 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.plugin.pubfactory;

import org.lockss.config.Configuration;
import org.lockss.daemon.ConfigParamDescr;
import org.lockss.daemon.RangeCachedUrlSetSpec;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.plugin.CachedUrlSet;
import org.lockss.plugin.base.BaseCachedUrlSet;
import org.lockss.plugin.definable.DefinableArchivalUnit;
import org.lockss.plugin.definable.DefinablePlugin;
import org.lockss.test.ConfigurationUtil;
import org.lockss.test.LockssTestCase;
import org.lockss.test.MockLockssDaemon;
import org.lockss.util.ListUtil;
import org.lockss.util.Logger;
import org.lockss.util.PatternFloatMap;
import org.lockss.util.RegexpUtil;

import java.net.URL;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

//
// This plugin test framework is set up to run the same tests in two variants - CLOCKSS and GLN
// without having to actually duplicate any of the written tests
//a
public class TestPubFactoryArchivalUnit extends LockssTestCase {
  private MockLockssDaemon theDaemon;
  static final String BASE_URL_KEY = ConfigParamDescr.BASE_URL.getKey();
  static final String JID_KEY = ConfigParamDescr.JOURNAL_ID.getKey();
  static final String VOL_KEY = ConfigParamDescr.VOLUME_NAME.getKey();
  static final String ROOT_URL = "https://www.berghahnjournals.com/";
  static final String ROOT_HOST = "www.berghahnjournals.com"; 
  

  static final String bgRepairList[] = 
    {
    "://[^/]+/(assets|fileasset|skin)/",
    };


  
  private static final Logger log = Logger.getLogger(TestPubFactoryArchivalUnit.class);

  static final String PLUGIN_ID = "org.lockss.plugin.pubfactory.PubFactoryJournalsPlugin";
  static final String PluginName = "PubFactory Journals Plugin";

  public void setUp() throws Exception {
    super.setUp();
    setUpDiskSpace();
    theDaemon = getMockLockssDaemon();
    theDaemon.getHashService();
  }

  public void tearDown() throws Exception {
    super.tearDown();
  }

  private DefinableArchivalUnit makeAu(URL url, String vol_name, String jid)
      throws Exception {

    Properties props = new Properties();
    props.setProperty(VOL_KEY, vol_name);
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
      makeAu(null, "10", "boyhood-studies");
      fail("Should have thrown ArchivalUnit.ConfigurationException");
    } catch (ArchivalUnit.ConfigurationException e) {
    }
  }

  //
  // Test the crawl rules
  //
  public void testShouldCacheProperPages() throws Exception {
    URL base = new URL(ROOT_URL);
    ArchivalUnit bgAu = makeAu(base, "10", "boyhood-studies");
    theDaemon.getLockssRepository(bgAu);
    theDaemon.getNodeManager(bgAu);
    BaseCachedUrlSet cus = new BaseCachedUrlSet(bgAu,
        new RangeCachedUrlSetSpec(base.toString()));
    
    // yes to these
    shouldCacheTest("https://www.berghahnjournals.com/assets/20180516/app/base/AbstractViewPage.js", true, bgAu, cus); 
    	shouldCacheTest("https://www.berghahnjournals.com/assets/20180516/applib/mixins/default.css", true, bgAu, cus); 
    	shouldCacheTest("https://www.berghahnjournals.com/assets/20180516/core/spacer.gif",true, bgAu, cus); 
    	shouldCacheTest("https://www.berghahnjournals.com/assets/20180516/js/scriptaculous_1_9_1/effects.js",true, bgAu, cus); 
    	shouldCacheTest("https://www.berghahnjournals.com/assets/20180516/vendor/fancybox/source/blank.gif",true, bgAu, cus); 
    	shouldCacheTest("https://www.berghahnjournals.com/downloadpdf/journals/boyhood-studies/10/1/bhs100101.pdf",true, bgAu, cus); 
    	shouldCacheTest("https://www.berghahnjournals.com/downloadpdf/journals/boyhood-studies/10/1/bhs100101.xml",true, bgAu, cus); 
    	shouldCacheTest("https://www.berghahnjournals.com/fileasset/BB%20left%20side%20logo.png",true, bgAu, cus); 
    	shouldCacheTest("https://www.berghahnjournals.com/lockss-manifest/journal/boyhood-studies/10",true, bgAu, cus);
    	shouldCacheTest("https://www.berghahnjournals.com/skin/20180516/css/style.css",true, bgAu, cus); 
    	shouldCacheTest("https://www.berghahnjournals.com/skin/20180516/fonts/fontawesome-webfont.woff?v=4.4.0",true, bgAu, cus); 
    	shouldCacheTest("https://www.berghahnjournals.com/skin/20180516/img/ajax-loader.gif",true, bgAu, cus); 
   	shouldCacheTest("https://www.berghahnjournals.com/view/journals/boyhood-studies/10/1/bhs100101.xml",true, bgAu, cus); 
    	shouldCacheTest("https://www.berghahnjournals.com/view/journals/boyhood-studies/10/1/bhs100101.xml?pdfVersion=true", true, bgAu, cus); 
    	shouldCacheTest("https://www.berghahnjournals.com/view/journals/boyhood-studies/10/1/boyhood-studies.10.issue-1.xml", true, bgAu, cus); 
    	shouldCacheTest("https://www.berghahnjournals.com/view/journals/boyhood-studies/large-boyhood-studies_cover.jpg", true, bgAu, cus); 
    		
//and not these
shouldCacheTest("https://www.berghahnjournals.com/browse", false, bgAu, cus); 
	shouldCacheTest("https://www.berghahnjournals.com/view/journals/boyhood-studies/11/1/bhs100101.xml", false, bgAu, cus); 
		shouldCacheTest("https://www.berghahnjournals.com/view/journals/other-studies/10/1/bhs100101.xml", false, bgAu, cus); 
	shouldCacheTest("https://www.berghahnjournals.com/view/journals/boyhood-studies/11/1/bhs100101.xml?&pdfVersion=true", false, bgAu, cus); 
			shouldCacheTest("http://www.berghahnjournals.com/view/journals/boyhood-studies/boyhood-studies-overview.xml", false, bgAu, cus);
shouldCacheTest("https://www.berghahnjournals.com/view/journals/boyhood-studies/10/2/boyhood-studies.10.issue-2.xml?print", false, bgAu, cus); 
shouldCacheTest("https://www.berghahnjournals.com/search?f_0=author&q_0=Garth+Stahl", false, bgAu, cus); 
shouldCacheTest("https://www.berghahnjournals.com/view/journals/boyhood-studies/11/1/boyhood-studies.11.issue-1.xml", false, bgAu, cus); 
shouldCacheTest("https://www.berghahnjournals.com/view/journals/boyhood-studies/1/1/boyhood-studies.1.issue-1.xml", false, bgAu, cus); 
shouldCacheTest("https://www.berghahnjournals.com/search?f_0=author&q_0=Alexandra+Mountain", false, bgAu, cus); 
shouldCacheTest("https://www.berghahnjournals.com/view/journals/boyhood-studies/10/2/bhs100201.xml?print", false, bgAu, cus); 
//addition of citation download overlay and url normalized ris citation link
shouldCacheTest("https://www.berghahnjournals.com/cite/$002fjournals$002fboyhood-studies$002f10$002f2$002fbhs100201.xml/$N?nojs=true", true, bgAu, cus); 
shouldCacheTest("https://www.berghahnjournals.com/cite/$002fjournals$002fboyhood-studies$002f3$002f2$002fbhs100202.xml/$N?nojs=true", false, bgAu, cus); 
shouldCacheTest("https://www.berghahnjournals.com/view/journals/boyhood-studies/10/2/bhs100201.xml?pdfVersion=true&print", false, bgAu, cus);
//wrong volume
shouldCacheTest("https://www.berghahnjournals.com/cite:exportcitation/ris?t:ac=$002fjournals$002fboyhood-studies$002f10$002f2$002fbhs100206.xml/$N&t:state:client=H4sDnAAAA", true, bgAu, cus);
//ris
shouldCacheTest("https://www.berghahnjournals.com/cite:exportcitation/ris?t:ac=$002fjournals$002fboyhood-studies$002f10$002f2$002fbhs100206.xml/$N", true, bgAu, cus);
// and a likely template error we are excluding (link of pdf while on pdf tab already)
shouldCacheTest("https://www.berghahnjournals.com/view/journals/boyhood-studies/10/1/bhs100101.xml?&pdfVersion=true", false, bgAu, cus);
//shouldCacheTest("https://www.berghahnjournals.com/", false, bgAu, cus);
//shouldCacheTest("https://www.berghahnjournals.com/", false, bgAu, cus);

  }

 


  private void shouldCacheTest(String url, boolean shouldCache,
      ArchivalUnit au, CachedUrlSet cus) {
    assertEquals(shouldCache, au.shouldBeCached(url));
  }

  public void testStartUrlConstruction() throws Exception {
    URL url = new URL(ROOT_URL);

    // 4 digit
    String expected1 = ROOT_URL+"lockss-manifest/journal/boyhood-studies/10";
    DefinableArchivalUnit ABAu = makeAu(url, "10", "boyhood-studies");
    assertEquals(ListUtil.list(expected1), ABAu.getStartUrls());
  }


  
  public void testPollSpecial() throws Exception {
    ArchivalUnit FooAu = makeAu(new URL("http://www.emeraldinsight.com/"), "33", "yyyy");
    theDaemon.getLockssRepository(FooAu);
    theDaemon.getNodeManager(FooAu);

    // if it changes in the plugin, you might need to change the test, so verify
    assertEquals(Arrays.asList(bgRepairList),
    		RegexpUtil.regexpCollection(FooAu.makeRepairFromPeerIfMissingUrlPatterns()));

    // make sure that's the regexp that will match to the expected url string
    // this also tests the regexp (which is the same) for the weighted poll map
    List <String> repairList = ListUtil.list(
    		"https://www.berghahnjournals.com/assets/20180516/app/components/display/AddToDownloadButton.js",
    		"https://www.berghahnjournals.com/assets/20180516/vendor/modernizr.min.js",
    		"https://www.berghahnjournals.com/fileasset/header_bg.png",
    		"https://www.berghahnjournals.com/skin/20180516/img/sortable-icon.svg");

    Pattern p0 = Pattern.compile(bgRepairList[0]);
    Matcher m0;
    for (String urlString : repairList) {
    	m0 = p0.matcher(urlString);
    	assertEquals(urlString, true, m0.find());
    }
    //and this one should fail - it needs to be weighted correctly and repaired from publisher if possible
    String notString ="https://www.berghahnjournals.com/view/journals/boyhood-studies/10/1/bhs100103.xml";
    m0 = p0.matcher(notString);
    assertEquals(false, m0.find());

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

