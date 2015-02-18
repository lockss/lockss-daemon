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

package org.lockss.plugin.mediawiki;

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
public class TestMediaWikiArchivalUnit extends LockssTestCase {
  private MockLockssDaemon theDaemon;
  static final String BASE_URL_KEY = ConfigParamDescr.BASE_URL.getKey();
  static final String START_PATH_KEY = "start_path";
  static final String PERMISSION_PATH_KEY = "permission_path";
  static final String ROOT_URL = "http://some.url.org/";
  static final String DEFAULT_START = "face/Special:all";
  static final String DEFAULT_PERMISSION = "face/Page_Thing";
  
  static Logger log = Logger.getLogger("TestNatureArchivalUnit");
  
  static final String PLUGIN_ID = "org.lockss.plugin.mediawiki.MediaWikiPlugin";
  static final String PluginName = "MediaWiki Plugin";
  
  public void setUp() throws Exception {
    super.setUp();
    setUpDiskSpace();
    theDaemon = getMockLockssDaemon();
    theDaemon.getHashService();
  }

  public void tearDown() throws Exception {
    super.tearDown();
  }

  private DefinableArchivalUnit makeAu(URL url, String start_path, String permission_path)
      throws Exception {

    Properties props = new Properties();
    props.setProperty(START_PATH_KEY, start_path);
    props.setProperty(PERMISSION_PATH_KEY, permission_path);
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
      makeAu(null, DEFAULT_START, DEFAULT_PERMISSION);
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
    ArchivalUnit NAu = makeAu(base, DEFAULT_START, DEFAULT_PERMISSION);
    theDaemon.getLockssRepository(NAu);
    theDaemon.getNodeManager(NAu);
    BaseCachedUrlSet cus = new BaseCachedUrlSet(NAu,
        new RangeCachedUrlSetSpec(base.toString()));
    // Test for pages that should get crawled
    // Start page
    shouldCacheTest(ROOT_URL+DEFAULT_START, true, NAu, cus);   
    //permission page
    shouldCacheTest(ROOT_URL+DEFAULT_PERMISSION, true, NAu, cus);
    // Conent pages
    shouldCacheTest(ROOT_URL+"index.php/3.2.1.3_Professional_development_program", true, NAu, cus);        
    shouldCacheTest(ROOT_URL+"index.php/Main_Page", true, NAu, cus);   
    shouldCacheTest(ROOT_URL+"images/thumb/e/e2/CLOCKSS-OrgChart.png/800px-CLOCKSS-OrgChart.png", true, NAu, cus);   
    shouldCacheTest(ROOT_URL+"http://documents.clockss.org/skins/vector/images/tab-current-fade.png?2013-09-03T18:56:40Z", true, NAu, cus);   
    shouldCacheTest(ROOT_URL+"index.php/File:Triggering_Content-4.png", true, NAu, cus); 
    
    // Now a couple that shouldn't get crawled
    // Special non content pages
    shouldCacheTest(ROOT_URL+"http://documents.clockss.org/index.php?title=Special:RecentChanges&feed=atom", false, NAu, cus);
    // Edit links
    shouldCacheTest(ROOT_URL+"index.php?title=File:Sample_Graph_2.png&action=edit&externaledit=true&mode=file", false, NAu, cus);
    // Old versions of pages
    shouldCacheTest(ROOT_URL+"index.php?title=CLOCKSS:_Budget_and_Planning_Process&oldid=155", false, NAu, cus);
    // other sites
    shouldCacheTest("http://lockss.stanford.edu", false, NAu, cus);
  }
  
  private void shouldCacheTest(String url, boolean shouldCache,
      ArchivalUnit au, CachedUrlSet cus) {
    assertEquals(shouldCache, au.shouldBeCached(url));
  }
  
  public void testStartUrlConstruction() throws Exception {
    URL url = new URL(ROOT_URL);
    String expected = ROOT_URL+DEFAULT_START;
    DefinableArchivalUnit NAu = makeAu(url, DEFAULT_START, DEFAULT_PERMISSION);
    
    assertEquals(ListUtil.list(expected), NAu.getStartUrls());
  }
  
  public void testShouldNotCachePageFromOtherSite() throws Exception {
    URL base = new URL(ROOT_URL);
    ArchivalUnit NAu = makeAu(base, DEFAULT_START, DEFAULT_PERMISSION);
    theDaemon.getLockssRepository(NAu);
    theDaemon.getNodeManager(NAu);
    CachedUrlSetSpec spec = new RangeCachedUrlSetSpec(base.toString());
    BaseCachedUrlSet cus = new BaseCachedUrlSet(NAu, spec);
    assertFalse(NAu.shouldBeCached(
        "http://shadow2.stanford.edu/" + DEFAULT_START));
  }


  public void testgetName() throws Exception {
    DefinableArchivalUnit au =
      makeAu(new URL(ROOT_URL), DEFAULT_START, DEFAULT_PERMISSION);
    
    assertEquals(PluginName + ", Base URL " + ROOT_URL +", Start Path " + DEFAULT_START + ", Permission Path " + DEFAULT_PERMISSION , au.getName());
  }

}

