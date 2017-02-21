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

package org.lockss.plugin.massachusettsmedicalsociety;

import org.lockss.config.Configuration;
import org.lockss.daemon.ConfigParamDescr;
import org.lockss.daemon.RangeCachedUrlSetSpec;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.plugin.ArchivalUnit.ConfigurationException;
import org.lockss.plugin.CachedUrlSet;
import org.lockss.plugin.PluginTestUtil;
import org.lockss.plugin.base.BaseCachedUrlSet;
import org.lockss.state.AuState;
import org.lockss.test.ConfigurationUtil;
import org.lockss.test.LockssPluginTestCase;
import org.lockss.test.MockAuState;
import org.lockss.test.MockLockssDaemon;
import org.lockss.util.Constants;
import org.lockss.util.ListUtil;
import org.lockss.util.TimeBase;

public class TestMassachusettsMedicalSocietyPlugin extends LockssPluginTestCase {
	
	protected MockLockssDaemon daemon;
	private final String PLUGIN_NAME = "org.lockss.plugin.massachusettsmedicalsociety.MassachusettsMedicalSocietyPlugin";
	static final String BASE_URL_KEY = ConfigParamDescr.BASE_URL.getKey();
	static final String BASE_URL2_KEY = ConfigParamDescr.BASE_URL2.getKey();
	static final String JOURNAL_ID_KEY = ConfigParamDescr.JOURNAL_ID.getKey();
	static final String VOLUME_NAME_KEY = ConfigParamDescr.VOLUME_NAME.getKey();
	private final String BASE_URL = "http://www.example.com/";
	private final String BASE_URL2 = "http://cdn.example.com/";
	private final String VOLUME_NAME = "352";
	private final String BAD_VOLUME_NAME = "354";
	private final String JOURNAL_ID = "nejm";
	private final Configuration AU_CONFIG = ConfigurationUtil.fromArgs(
												BASE_URL_KEY, BASE_URL,
												BASE_URL2_KEY, BASE_URL2,
												VOLUME_NAME_KEY, VOLUME_NAME,
												JOURNAL_ID_KEY, JOURNAL_ID);
	
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
  
  public void testCreateAu() {

	    try {
	      ArchivalUnit au = createAu(ConfigurationUtil.fromArgs(
	    		  						BASE_URL_KEY, BASE_URL,
	    		  						VOLUME_NAME_KEY, VOLUME_NAME));
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
  
  public void testShouldCacheProperPages() throws Exception {
	    ArchivalUnit au = createAu();
	    BaseCachedUrlSet cus = new BaseCachedUrlSet(au, new RangeCachedUrlSetSpec(BASE_URL));

	    // start page
	    assertShouldCache(BASE_URL + "lockss/" + JOURNAL_ID + "/" + VOLUME_NAME + "/index.html", true, au, cus);
	    
	    assertShouldCache(BASE_URL + "lockss/" + JOURNAL_ID + "/" + BAD_VOLUME_NAME + "/index.html", false, au, cus);
	    
	    // issue pages
	    assertShouldCache(BASE_URL + "toc/" + JOURNAL_ID + "/" + VOLUME_NAME + "/1", true, au, cus);
	    assertShouldCache(BASE_URL + "toc/" + JOURNAL_ID + "/" + VOLUME_NAME + "/33", true, au, cus);
	    assertShouldCache(BASE_URL + "toc/" + JOURNAL_ID + "/" + VOLUME_NAME + "/12", true, au, cus);
	    
	    assertShouldCache(BASE_URL + "toc/" + JOURNAL_ID + "/" + BAD_VOLUME_NAME + "/12", false, au, cus);
	    // article page and article materials
	    String [] validArticleMat = {"pdf", "full", "media"};
	    for(String vam : validArticleMat){
	    	assertShouldCache(BASE_URL + "doi/" + vam + "/10.1056/" + JOURNAL_ID + "p32411224", true, au, cus);
	    }
	    
	    String [] invalidArticleMat = {"citedby", "audio", "exam"};
	    for(String iam : invalidArticleMat){
	    	assertShouldCache(BASE_URL + "doi/" + iam + "/10.1056/" + JOURNAL_ID + "p32411224", false, au, cus);
	    }
	    
	    // supplementary article pages
	    String [] validArticleSup = {"Image", "Supplements", "MediaPlayer"};
	    for(String vas : validArticleSup){
	    	assertShouldCache(BASE_URL + "action/show" + vas + "?doi=10.1056%2F" + JOURNAL_ID + "p058021&aid=" + JOURNAL_ID + "p058021_attach_1&area=", true, au, cus);
	    }
	    
	    // images, css, js
	    assertShouldCache(BASE_URL + "na102/home/ACS/publisher/mms/journals/content/nejm/2005/nejm_2005.352.issue-26/nejmp058021/production/images/large/nejmp058021_f1.jpeg", true, au, cus);
	    assertShouldCache(BASE_URL + "na102/home/ACS/publisher/mms/journals/content/nejm/2005/nejm_2005.352.issue-26/nejmp058021/production/images/small/nejmp058021_f1.jpeg", true, au, cus);
	    assertShouldCache(BASE_URL + "templates/jsp/_style2/_mms/_nejm/css/toolLayer.css", true, au, cus);
	    assertShouldCache(BASE_URL + "templates/jsp/_style2/_mms/_nejm/js/mmsAddThis.js", true, au, cus);
	    assertShouldCache(BASE_URL + "templates/jsp/_style2/_mms/_nejm/img/perspectives-image-bar.png", true, au, cus);
	    
	    // off host css, js
	    assertShouldCache(BASE_URL2 + "js/jquery.min.js", true, au, cus);
	    assertShouldCache(BASE_URL2 + "js/jquery-ui-1.7.2.custom.min.js", true, au, cus);
	    assertShouldCache(BASE_URL2 + "css/jquery.jcarousel.css", true, au, cus);
	    
	    assertShouldCache(BASE_URL2 + "js/index.html", false, au, cus);
	    assertShouldCache(BASE_URL2, false, au, cus);
	    
	    // specified bad pages
	    assertShouldCache(BASE_URL + "action/clickThrough?id=3052&url=%2Fclinical-practice-center%3Fquery%3Dcm&loc=%2Fdoi%2Ffull%2F10.1056%2FNEJMp058021&pubId=40059754", false, au, cus);
	    assertShouldCache(BASE_URL + "journal-articles", false, au, cus);
	    assertShouldCache(BASE_URL + "servlet/linkout?suffix=r001&dbid=16384&doi=10.1056/NEJMp058021&url=http%3A%2F%2Fsfx.stanford.edu%2Flocal%3Fsid%3Dmms%26genre%3D", false, au, cus);
	    assertShouldCache(BASE_URL + "action/addTag?uactid=savePage&uacturi=%2Fdoi%2Ffull%2F10.1056%2FNEJMp058021&doi=10.1056%2FNEJMp058021", false, au, cus);
	    
	    // checkm8
	    assertShouldCache("http://web.checkm8.com/adam/cm8adam_1_ajax.js", false, au, cus);
	    
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

  public void testSubstancePatterns() throws Exception {
    ArchivalUnit au = createAu();
    // start page
    assertNotSubstanceUrl(BASE_URL + "lockss/" + JOURNAL_ID + "/"
			  + VOLUME_NAME + "/index.html",
			  au);
    // full text
    assertSubstanceUrl(BASE_URL + "doi/full/10.1056/"
		       + JOURNAL_ID + "p32411224",
		       au);
  }	    

	  public void testStartUrlConstruction() throws Exception {
	    String expectedStartUrl = BASE_URL + "lockss/" + JOURNAL_ID + "/" + VOLUME_NAME + "/index.html";
	    ArchivalUnit au = createAu();
	    assertEquals(ListUtil.list(expectedStartUrl), au.getStartUrls());
	  }

	  public void testGetUrlStems() throws Exception {
	    ArchivalUnit au = createAu();
		  assertSameElements(ListUtil.list(BASE_URL2, BASE_URL), au.getUrlStems());
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
	    assertEquals("Massachusetts Medical Society Plugin, Base URL " + BASE_URL + ", Base URL 2 " + BASE_URL2 + ", Journal ID " + JOURNAL_ID + ", Volume " + VOLUME_NAME, au.getName());
	  }

}
