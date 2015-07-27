/*
 * $Id$
 */

/*

Copyright (c) 2000-2013 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.plugin.nature;

import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.util.Properties;

import junit.framework.Test;

import org.lockss.app.LockssDaemon;
import org.lockss.config.Configuration;
import org.lockss.crawler.CrawlRateLimiter;
import org.lockss.daemon.*;
import org.lockss.plugin.*;
import org.lockss.plugin.base.*;
import org.lockss.plugin.definable.*;
import org.lockss.plugin.maffey.MaffeyHtmlCrawlFilterFactory;
import org.lockss.plugin.maffey.TestMaffeyHtmlFilterFactory;
import org.lockss.plugin.maffey.TestMaffeyHtmlFilterFactory.TestCrawl;
import org.lockss.plugin.maffey.TestMaffeyHtmlFilterFactory.TestHash;
import org.lockss.plugin.wrapper.WrapperUtil;
import org.lockss.repository.LockssRepositoryImpl;
import org.lockss.state.AuState;
import org.lockss.test.*;
import org.lockss.util.*;

//
// This plugin test framework is set up to run the same tests in two variants - CLOCKSS and GLN
// without having to actually duplicate any of the written tests
//
public class TestNatureArchivalUnit extends LockssTestCase {
  private MockLockssDaemon theDaemon;
  static final String BASE_URL_KEY = ConfigParamDescr.BASE_URL.getKey();
  static final String JID_KEY = ConfigParamDescr.JOURNAL_ID.getKey();
  static final String VOL_KEY = ConfigParamDescr.VOLUME_NAME.getKey();
  static final String YEAR_KEY = ConfigParamDescr.YEAR.getKey();
  static final String ROOT_URL = "http://www.nature.com/"; 
  
  static Logger log = Logger.getLogger("TestNatureArchivalUnit");
  
  static final String PLUGIN_ID = "org.lockss.plugin.nature.ClockssNaturePublishingGroupPlugin";
  static final String PluginName = "Nature Publishing Group Plugin (CLOCKSS)";
  
  public void setUp() throws Exception {
    super.setUp();
    setUpDiskSpace();
    theDaemon = getMockLockssDaemon();
    theDaemon.getHashService();
  }

  public void tearDown() throws Exception {
    super.tearDown();
  }

  private DefinableArchivalUnit makeAu(URL url, int volume, String jid, String year)
      throws Exception {

    Properties props = new Properties();
    props.setProperty(VOL_KEY, Integer.toString(volume));
    props.setProperty(YEAR_KEY, year);
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
      makeAu(null, 29, "onc", "2010");
      fail("Should have thrown ArchivalUnit.ConfigurationException");
    } catch (ArchivalUnit.ConfigurationException e) {
    }
  }
  
  //
  // Test the crawl rules
  //
  
  // This test was implemented late in the game. So add to these tests as you update plugin
  public void testShouldCacheProperPages() throws Exception {
    URL base = new URL(ROOT_URL);
    ArchivalUnit NAu = makeAu(base, 123, "xxxx", "2009");
    theDaemon.getLockssRepository(NAu);
    theDaemon.getNodeManager(NAu);
    BaseCachedUrlSet cus = new BaseCachedUrlSet(NAu,
        new RangeCachedUrlSetSpec(base.toString()));
    // Test for pages that should get crawled
    //manifest page
    shouldCacheTest(ROOT_URL+"xxxx/clockss/xxxx_clockss_2009.html", true, NAu, cus);    
    // toc page 
    shouldCacheTest(ROOT_URL+"xxxx/journal/v123/n22/index.html", true, NAu, cus);
    //http://www.nature.com/onc/journal/v32/n23/abs/onc2012303a.html
    shouldCacheTest(ROOT_URL+"xxxx/journal/v123/n22/abs/onc2009303a.html", true, NAu, cus);
    //http://www.nature.com/onc/journal/v32/n23/full/onc2012308a.html
    shouldCacheTest(ROOT_URL+"xxxx/journal/v123/n22/full/onc2009303a.html", true, NAu, cus);        
    //http://www.nature.com/onc/journal/v32/n23/pdf/onc2012308a.pdf
    shouldCacheTest(ROOT_URL+"xxxx/journal/v123/n22/pdf/onc2009303a.pdf", true, NAu, cus);          
    //http://www.nature.com/onc/journal/v32/n23/suppinfo/onc2012308s1.html
    shouldCacheTest(ROOT_URL+"xxxx/journal/v123/n22/suppinfo/onc2009303s1.html", true, NAu, cus);          
    //http://www.nature.com/onc/journal/v32/n23/extref/onc2012308x1.ppt
    shouldCacheTest(ROOT_URL+"xxxx/journal/v123/n22/extref/onc2009303x1.ppt", true, NAu, cus);          
    //http://www.nature.com/onc/journal/v32/n23/extref/onc2012308x3.doc
    shouldCacheTest(ROOT_URL+"xxxx/journal/v123/n22/extref/onc2009303x3.doc", true, NAu, cus);          


    
    // Now a couple that shouldn't get crawled
    // wrong volume
    shouldCacheTest(ROOT_URL+"xxxx/journal/v666/n22/index.html", false, NAu, cus);
    // redundant
    shouldCacheTest(ROOT_URL+"xxxx/journal/v123/n22/abs/onc2009303a.html?message=remove", false, NAu, cus);
    //LOCKSS
    shouldCacheTest("http://lockss.stanford.edu", false, NAu, cus);
    // other sites
    
    
    // test some crawl rules that now exclude normalized off stuff
    // at end
    shouldCacheTest(ROOT_URL+"xxxx/journal/v123/n22/full/onc2010273a.html?message=remove", false, NAu, cus);          
    shouldCacheTest(ROOT_URL+"xxxx/journal/v123/n22/full/onc2010273a.html?message-global=remove", false, NAu, cus);
    // at beginning of arg list
    shouldCacheTest(ROOT_URL+"xxxx/journal/v123/n22/full/onc2010273a.html?message=remove&arg=foo", false, NAu, cus);          
    shouldCacheTest(ROOT_URL+"xxxx/journal/v123/n22/full/onc2010273a.html?message-global=remove&arg=foo", false, NAu, cus);          
    // in middle of arg list
    shouldCacheTest(ROOT_URL+"xxxx/journal/v123/n22/full/onc2010273a.html?arg=foo&message=remove&arg2=blah", false, NAu, cus);          
    shouldCacheTest(ROOT_URL+"xxxx/journal/v123/n22/full/onc2010273a.html?arg=foo&message-global=remove&arg2=blah", false, NAu, cus);
    
    // TOC page collected as unterminated issue page - normalize to vx/ny/index.html
    shouldCacheTest(ROOT_URL+"xxxx/journal/v123/n22", false, NAu, cus);          
    shouldCacheTest(ROOT_URL+"xxxx/journal/v123/n22/index.html", true, NAu, cus);          
    
  }
  
  private void shouldCacheTest(String url, boolean shouldCache,
      ArchivalUnit au, CachedUrlSet cus) {
    assertEquals(shouldCache, au.shouldBeCached(url));
  }
  
  public void testStartUrlConstruction() throws Exception {
    URL url = new URL(ROOT_URL);

    // 4 digit
    String expected = ROOT_URL+"xxxx/clockss/xxxx_clockss_2009.html";
    DefinableArchivalUnit NAu = makeAu(url, 123, "xxxx", "2009");
    assertEquals(ListUtil.list(expected), NAu.getStartUrls());
  }
  
  public void testShouldNotCachePageFromOtherSite() throws Exception {
    URL base = new URL("http://www.nature.com/");
    int volume = 123;
    String jid = "xxxx";
    String year = "2009";
    ArchivalUnit NAu = makeAu(base, volume, jid, year);

    theDaemon.getLockssRepository(NAu);
    theDaemon.getNodeManager(NAu);
    CachedUrlSetSpec spec = new RangeCachedUrlSetSpec(base.toString());
    BaseCachedUrlSet cus = new BaseCachedUrlSet(NAu, spec);
    assertFalse(NAu.shouldBeCached(
        "http://shadow2.stanford.edu/clockss/xxxx_clockss_2009.html"));
  }


  public void testgetName() throws Exception {
    DefinableArchivalUnit au =
      makeAu(new URL("http://www.nature.com/"), 33, "yyyy", "1965");
    assertEquals(PluginName + ", Base URL http://www.nature.com/, Journal ID yyyy, Year 1965, Volume 33", au.getName());

  }

}

