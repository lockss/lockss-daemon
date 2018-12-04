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

package org.lockss.plugin.igiglobal;


import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.lockss.config.Configuration;
import org.lockss.daemon.ConfigParamDescr;
import org.lockss.daemon.RangeCachedUrlSetSpec;
import org.lockss.plugin.*;
import org.lockss.plugin.ArchivalUnit.ConfigurationException;
import org.lockss.plugin.base.BaseCachedUrlSet;
import org.lockss.state.AuState;
import org.lockss.test.*;
import org.lockss.util.Constants;
import org.lockss.util.ListUtil;
import org.lockss.util.PatternFloatMap;
import org.lockss.util.RegexpUtil;
import org.lockss.util.TimeBase;

public class TestIgiGlobalBooksPlugin extends LockssPluginTestCase {
	
	protected MockLockssDaemon daemon;
	private final String PLUGIN_NAME = "org.lockss.plugin.igiglobal.IgiGlobalBooksPlugin";
	static final String BASE_URL_KEY = ConfigParamDescr.BASE_URL.getKey();
	static final String BOOK_ISBN_KEY = "book_isbn";
	static final String BOOK_ISBN = "9781234567890";
	static final String VOLUME_NUMBER_KEY = ConfigParamDescr.VOLUME_NUMBER.getKey();
	private final String BASE_URL = "http://www.example.com/";
	private final String VOLUME = "21";
	private final Configuration AU_CONFIG = ConfigurationUtil.fromArgs(
										 		BASE_URL_KEY, BASE_URL,
										 		BOOK_ISBN_KEY, BOOK_ISBN,
										 		VOLUME_NUMBER_KEY, VOLUME);
	
	// from au_url_poll_result_weight in plugins/src/org/lockss/plugin/igiglobal/IgiGlobalPlugin.xml
	// if it changes in the plugin, you will likely need to change the test, so verify
	static final String  IGI_REPAIR_FROM_PEER_REGEXP1 = "(?i)://[^/]+/(images|.*jquery.*|sourcecontent)/.*[.](bmp|gif|ico|jpe?g|png|tif?f)$";
	static final String  IGI_REPAIR_FROM_PEER_REGEXP2 = "[.](css|js)$";
	static final String  IGI_REPAIR_FROM_PEER_REGEXP3 = "^https?://[^/]+/lockss/books.aspx$";

  public void setUp() throws Exception {
    super.setUp();
  }
  
  public void tearDown() throws Exception {
	    super.tearDown();
	  }

  protected ArchivalUnit createAu() throws ArchivalUnit.ConfigurationException {
    return
      createAu(AU_CONFIG);
  }
  
  protected ArchivalUnit createAu(Configuration config) throws ArchivalUnit.ConfigurationException {
	return
	  PluginTestUtil.createAndStartAu(PLUGIN_NAME, config);
  }
  
  /*
  public void testCreateAu() {

	    try {
	      createAu(ConfigurationUtil.fromArgs(
	    		  						BASE_URL_KEY, BASE_URL,
	    		  						BOOK_ISBN_KEY, BOOK_ISBN,
	    		  						VOLUME_NUMBER_KEY, VOLUME));
	      fail("Bad AU configuration should throw configuration exception");
	    }
	    catch (ConfigurationException ex) {
	    }
	    try {
	    	createAu();
	    }
	    catch (ConfigurationException ex) {
	    	fail("Unable to creat AU from valid configuration");
	    }
  }
  */
  
  public void testShouldCacheProperPages() throws Exception {
	    ArchivalUnit au = createAu();
	    BaseCachedUrlSet cus = new BaseCachedUrlSet(au, new RangeCachedUrlSetSpec(BASE_URL));

	    // start url, title
	    assertShouldCache(BASE_URL + "gateway/book/" + VOLUME, true, au, cus);

	    // substance, chapters
	    assertShouldCache(BASE_URL + "gateway/chapter/123456" , true, au, cus);
	    assertShouldCache(BASE_URL + "gateway/chapter/full-text-html/123456" , true, au, cus);
	    assertShouldCache(BASE_URL + "gateway/chapter/full-text-pdf/1234560" , true, au, cus);
	    assertShouldCache(BASE_URL + "pdf.aspx?tid=123456&ptid=123456&ctid=123456&t=Title+Page" , true, au, cus);
	    
	  }

	  private void assertShouldCache(String url, boolean shouldCache,
				       ArchivalUnit au, CachedUrlSet cus) {
	    assertEquals("AU crawl rules applied incorrectly to " + url + " ",
	        shouldCache, au.shouldBeCached(url));
	  }

	  public void testStartUrlConstruction() throws Exception {
	    String expectedStartUrl = BASE_URL + "gateway/book/" + VOLUME;
	    ArchivalUnit au = createAu();
	    assertSameElements(ListUtil.list(expectedStartUrl), au.getStartUrls());
	  }


	  //public void testShouldDoNewContentCrawlTooEarly() throws Exception {
	    //ArchivalUnit au = createAu();
	    //AuState aus = new MockAuState(null, TimeBase.nowMs(), -1, -1, null);
	    //assertFalse(au.shouldCrawlForNewContent(aus));
	  //}

	  //public void testShouldDoNewContentCrawlForZero() throws Exception {
		  //ArchivalUnit au = createAu();
	    //AuState aus = new MockAuState(null, 0, -1, -1, null);
	    //assertTrue(au.shouldCrawlForNewContent(aus));
	  //}

	  //public void testShouldDoNewContentCrawlEachMonth() throws Exception {
		  //ArchivalUnit au = createAu();
	    //AuState aus = new MockAuState(null, 4 * Constants.WEEK, -1, -1, null);
	    //assertTrue(au.shouldCrawlForNewContent(aus));
	  //}
	  
	  public void testGetName() throws Exception {
	    ArchivalUnit au = createAu();
	    assertEquals("IGI Global Books Plugin, Base URL " + BASE_URL + ", Book ISBN "+ BOOK_ISBN+ ", Volume " + VOLUME, au.getName());
	  }

	  public void testPollSpecial() throws Exception {
	    ArchivalUnit au = createAu();
	    
	    // if it changes in the plugin, you will likely need to change the test, so verify
	    assertEquals(ListUtil.list(
	        IGI_REPAIR_FROM_PEER_REGEXP1, IGI_REPAIR_FROM_PEER_REGEXP2),
	        RegexpUtil.regexpCollection(au.makeRepairFromPeerIfMissingUrlPatterns()));
	    
	    PatternFloatMap pfm = au.makeUrlPollResultWeightMap();
	    
	    // make sure that's the regexp that will match to the expected url string
	    // this also tests the regexp (which is the same) for the weighted poll map
	    // Add to pattern these urls? Has not been seen as problem, yet
	    //  http://www.igi-global.com/favicon.ico
	    
	    List <String> repairList1 = ListUtil.list(
	        BASE_URL + "sourcecontent/9781466601161_58264/978-1-4666-0116-1.ch002.f01.png",
	        BASE_URL + "images/ala-2017.png",
	        BASE_URL + "jquery/ala-2017.jpg");
	    Pattern p = Pattern.compile(IGI_REPAIR_FROM_PEER_REGEXP1, Pattern.CASE_INSENSITIVE);
	    for (String urlString : repairList1) {
	      assertEquals(urlString, true, p.matcher(urlString).find());
	      assertEquals(urlString, 0.0f, pfm.getMatch(urlString, 1.0f));
	    }
	    List <String> repairList2 = ListUtil.list(
	        BASE_URL + "includes/redesign/stuff.js",
	        BASE_URL + "hello/world/good//more_stuff.css");
	    p = Pattern.compile(IGI_REPAIR_FROM_PEER_REGEXP2, Pattern.CASE_INSENSITIVE);
	    for (String urlString : repairList2) {
	      assertEquals(urlString, true, p.matcher(urlString).find());
          assertEquals(urlString, 0.0f, pfm.getMatch(urlString, 1.0f));
	    }
	    p = Pattern.compile(IGI_REPAIR_FROM_PEER_REGEXP3, Pattern.CASE_INSENSITIVE);
	    List <String> repairList3 = ListUtil.list(
            BASE_URL + "lockss/books.aspx");
	    for (String urlString : repairList3) {
	      assertEquals(urlString, true, p.matcher(urlString).find());
	      assertEquals(urlString, 0.0f, pfm.getMatch(urlString, 1.0f));
	    }
	    
	    //and this one should fail - it needs to be weighted correctly and repaired from publisher if possible
	    String notString = BASE_URL + "favicon.ico";
	    Matcher m = p.matcher(notString);
	    assertEquals(false, m.find());
	    
	    PatternFloatMap urlPollResults = au.makeUrlPollResultWeightMap();
	    assertNotNull(urlPollResults);
	    for (String urlString : repairList2) {
	      assertEquals(0.0, urlPollResults.getMatch(urlString, (float) 1), .0001);
	    }
	    assertEquals(1.0, urlPollResults.getMatch(notString, (float) 1), .0001);
	  }
	  
}
