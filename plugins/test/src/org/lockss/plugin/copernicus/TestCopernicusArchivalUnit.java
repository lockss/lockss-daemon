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

package org.lockss.plugin.copernicus;

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
public class TestCopernicusArchivalUnit extends LockssTestCase {
  private MockLockssDaemon theDaemon;
  static final String BASE_URL_KEY = ConfigParamDescr.BASE_URL.getKey();
  static final String HOME_URL_KEY = "home_url";
  static final String VOL_KEY = ConfigParamDescr.VOLUME_NAME.getKey();
  static final String YEAR_KEY = ConfigParamDescr.YEAR.getKey();
  static final String ROOT_URL = "http://www.test-cop.com/"; //this is not a real url
  static final String ROOT_HOME_URL = "http://www.TestCopernicus.com/"; //this is not a real url
  
  static Logger log = Logger.getLogger(TestCopernicusArchivalUnit.class);
  
  static final String PLUGIN_ID = "org.lockss.plugin.copernicus.CopernicusPublicationsPlugin";
  static final String PluginName = "Copernicus Publications Plugin";
  
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
  }
  
  private void shouldCacheTest(String url, boolean shouldCache,
      ArchivalUnit au, CachedUrlSet cus) {
    assertEquals(shouldCache, au.shouldBeCached(url));
  }
  
  public void testStartUrlConstruction() throws Exception {
    URL url = new URL(ROOT_URL);

    // 4 digit
    String expected = ROOT_URL+"123/index.html";
    DefinableArchivalUnit ABAu = makeAu(url, url, 123, "2010");
    assertEquals(ListUtil.list(expected), ABAu.getStartUrls());
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

}

