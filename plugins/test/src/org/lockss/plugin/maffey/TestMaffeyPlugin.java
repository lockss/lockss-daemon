/*

Copyright (c) 2000-2011 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.plugin.maffey;

import java.io.File;
import java.net.URL;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.oro.text.regex.Perl5Compiler;
import org.lockss.config.Configuration;
import org.lockss.daemon.ConfigParamDescr;
import org.lockss.daemon.RangeCachedUrlSetSpec;
import org.lockss.plugin.*;
import org.lockss.plugin.ArchivalUnit.ConfigurationException;
import org.lockss.plugin.base.BaseCachedUrlSet;
import org.lockss.repository.LockssRepositoryImpl;
import org.lockss.state.AuState;
import org.lockss.test.*;
import org.lockss.util.Constants;
import org.lockss.util.ListUtil;
import org.lockss.util.PatternFloatMap;
import org.lockss.util.RegexpUtil;
import org.lockss.util.TimeBase;

public class TestMaffeyPlugin extends LockssPluginTestCase {
	
	protected MockLockssDaemon daemon;
	private final String PLUGIN_NAME = "org.lockss.plugin.maffey.MaffeyPlugin";
	static final String BASE_URL_KEY = ConfigParamDescr.BASE_URL.getKey();
	static final String JOURNAL_ID_KEY = ConfigParamDescr.JOURNAL_ID.getKey();
	static final String YEAR_KEY = ConfigParamDescr.YEAR.getKey();
	static final String BASE_URL = "http://www.example.com/";
	private final String YEAR = "2010";
	private final String JOURNAL_ID = "11";
	private final Configuration AU_CONFIG = ConfigurationUtil.fromArgs(BASE_URL_KEY,
	    								   BASE_URL,
									   YEAR_KEY,
									   YEAR,
									   JOURNAL_ID_KEY,
									   JOURNAL_ID);
  public void setUp() throws Exception {
    super.setUp();
  }
  
  public void tearDown() throws Exception {
    super.tearDown();
  }

  protected ArchivalUnit createAu() throws ArchivalUnit.ConfigurationException {
    return createAu(AU_CONFIG);
  }
  
  protected ArchivalUnit createAu(Configuration config) throws ArchivalUnit.ConfigurationException {
	return PluginTestUtil.createAndStartAu(PLUGIN_NAME, config);
  }
  
  public void testCreateAu() throws ConfigurationException {

	    try {
	      ArchivalUnit au = createAu(ConfigurationUtil.fromArgs(BASE_URL_KEY,
		  						    BASE_URL,
	    		  					    YEAR_KEY,
	    		  					    YEAR));
	      fail("Bad AU configuration should throw configuration exception");
	    }
	    catch (ConfigurationException ex) {
	    }
	    createAu();
  }
  
  public void testShouldCacheProperPages() throws Exception {
	    ArchivalUnit au = createAu();
	    BaseCachedUrlSet cus = new BaseCachedUrlSet(au, new RangeCachedUrlSetSpec(BASE_URL));

	    // start page
	    assertShouldCache(BASE_URL + "lockss.php?t=lockss&pa=issue&j_id=" + JOURNAL_ID + "&year=" + YEAR, true, au, cus);
	    
	    assertShouldCache(BASE_URL + "lockss.php?pa=issue&j_id=" + JOURNAL_ID + "&year=", false, au, cus);
	    
	    // issue pages
	    assertShouldCache(BASE_URL + "lockss.php?t=lockss&pa=article&i_id=3", true, au, cus);
	    assertShouldCache(BASE_URL + "lockss.php?pa=article&i_id=3", true, au, cus);
	    assertShouldCache(BASE_URL + "lockss.php?t=lockss&pa=article&i_id=11", true, au, cus);
	    
	    assertShouldCache(BASE_URL + "lockss.php?t=lockss&pa=article&i_id=", false, au, cus);
	    
	    // article pages
	    assertShouldCache(BASE_URL + "the-use-of-ion-chromatography-for-the-determination--of-clean-in-place-article-a5", true, au, cus);
	    
	    assertShouldCache(BASE_URL + "the-use-of-ion-chromatography-for-the-determination--of-clean-in-place-article-a", false, au, cus);
	    assertShouldCache(BASE_URL + "the-use-of-ion-chromatography-for-the-determination--of-clean-in-place-rticle-a5", false, au, cus);
	    assertShouldCache(BASE_URL + "the-use-of-ion-chromatography-for-the-/determination--of-clean-in-place-article-a5", false, au, cus);
	    
	    // pdfs
	    assertShouldCache(BASE_URL + "redirect_file.php?fileType=pdf&fileId=961&filename=ACI-1-Zayas(Pr)&nocount=1", true, au, cus);
	    
	    assertShouldCache(BASE_URL + "redirect_file.php?fileId=961&filename=ACI-1-Zayas(Pr)&fileType=pdf", true, au, cus);
	    assertShouldCache(BASE_URL + "redirect_file.php?fileType=pdf&fileId=961&filename=ACI-1-Zayas(Pr)", true, au, cus);
	    
	    // images, css, js
	    assertShouldCache(BASE_URL + "css/maffey_styles.css", true, au, cus);
	    assertShouldCache(BASE_URL + "la_press_basic_javascript.js", true, au, cus);
	    assertShouldCache(BASE_URL + "js/jquery-validate/jquery.validate.min.js", true, au, cus);
	    assertShouldCache(BASE_URL + "assets/img/stats_article.jpg", true, au, cus);
	    
	    
	    // images, css, js with bad formatting
	    assertShouldCache(BASE_URL + "css/grid.css?1333744447", true, au, cus);
	    
	    // internal should not crawl
	    assertShouldCache(BASE_URL + "analytical-chemistry-insights-journal-j1", false, au, cus);
	    assertShouldCache(BASE_URL + "testimonials.php", false, au, cus);
	    assertShouldCache(BASE_URL + "bibliography.php?article_id=5", false, au, cus);
	    
	    // facebook
	    assertShouldCache("http://www.facebook.com/pages/IGI-Global/138206739534176?ref=sgm", false, au, cus);
	    
	    // LOCKSS
	    assertShouldCache("http://lockss.stanford.edu", false, au, cus);

	    // other site
	    assertShouldCache("http://exo.com/~noid/ConspiracyNet/satan.html", false, au, cus);
	    
	  }

	  private void assertShouldCache(String url, boolean shouldCache,
				       ArchivalUnit au, CachedUrlSet cus) {
	    assertEquals("AU crawl rules applied incorrectly to " + url + " ",
	        shouldCache, au.shouldBeCached(url));
	  }

	  public void testStartUrlConstruction() throws Exception {
	    String expectedStartUrl = BASE_URL + "lockss.php?t=lockss&pa=issue&j_id=" + JOURNAL_ID + "&year=" + YEAR;
	    ArchivalUnit au = createAu();
	    assertEquals(ListUtil.list(expectedStartUrl), au.getStartUrls());
	  }

	  public void testGetUrlStems() throws Exception {
	    ArchivalUnit au = createAu();
	    assertEquals(ListUtil.list(BASE_URL), au.getUrlStems());
	  }

	  public void testShouldDoNewContentCrawlTooEarly() throws Exception {
	    ArchivalUnit au = createAu();
	    AuState aus = new MockAuState(null, TimeBase.nowMs(), -1, -1, null);
	    assertFalse(au.shouldCrawlForNewContent(aus));
	  }

	  public void testShouldDoNewContentCrawlForZero() throws Exception {
		  ArchivalUnit au = createAu();
	    AuState aus = new MockAuState(null, 0, -1, -1, null);
	    assertTrue(au.shouldCrawlForNewContent(aus));
	  }

	  public void testShouldDoNewContentCrawlEachMonth() throws Exception {
		  ArchivalUnit au = createAu();
	    AuState aus = new MockAuState(null, 4 * Constants.WEEK, -1, -1, null);
	    assertTrue(au.shouldCrawlForNewContent(aus));
	  }
	  
	  public void testGetName() throws Exception {
	    ArchivalUnit au = createAu();
	    assertEquals("Libertas Academica Plugin, Base URL " + BASE_URL + ", Journal ID " + JOURNAL_ID + ", Year " + YEAR, au.getName());
	  }
	  
	  // need to call quotemeta on the base/home_urls because the RegexpUtil.regexpCollection 
	  // call on the returning strings calls it (but only on the base/home_url params)
	  static final String baseRepairList[] =
	    {
	        "^"+Perl5Compiler.quotemeta(BASE_URL)+"(css|images)/",
	    };

	  public void testRepairList() throws Exception {
	    URL base = new URL(BASE_URL);
	    ArchivalUnit ABAu = createAu();

	    assertEquals(Arrays.asList(baseRepairList), RegexpUtil.regexpCollection(ABAu.makeRepairFromPeerIfMissingUrlPatterns()));

	    // make sure that's the regexp that will match to the expected url string
	    // this also tests the regexp (which is the same) for the weighted poll map
	    List <String> repairList = ListUtil.list(   
	      BASE_URL+"css/foo.css",
	      BASE_URL+"css/zip.js",
	      BASE_URL+"images/hello.jpg",
	      BASE_URL + "css/grid.css?1333744447",
	      BASE_URL+"images/test.gif"
	     );
	    
	    Pattern p0 = Pattern.compile(baseRepairList[0]);
	    Matcher m0;
	    for (String urlString : repairList) {
	      m0 = p0.matcher(urlString);
	      assertEquals(true, m0.find());
	    }
	    
	    //and this one should fail - it needs to be weighted correctly and repaired from publisher if possible
	    String notString =BASE_URL+"missing_file.html";
	    m0 = p0.matcher(notString);
	    assertEquals(false, m0.find());

	   PatternFloatMap urlPollResults = ABAu.makeUrlPollResultWeightMap();
	   assertNotNull(urlPollResults);
	   for (String urlString : repairList) {
	     assertEquals(0.0, urlPollResults.getMatch(urlString, (float) 1), .0001);
	   }
	   assertEquals(1.0, urlPollResults.getMatch(notString, (float) 1), .0001);
	   
	  }

}
