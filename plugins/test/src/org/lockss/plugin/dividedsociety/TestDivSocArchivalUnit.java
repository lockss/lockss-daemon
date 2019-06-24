/*
 $Id:$

Copyright (c) 2000-2016 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.plugin.dividedsociety;

import org.lockss.config.Configuration;
import org.lockss.daemon.*;
import org.lockss.plugin.*;
import org.lockss.plugin.base.BaseCachedUrlSet;
import org.lockss.plugin.definable.*;
import org.lockss.test.*;
import org.lockss.util.*;

public class TestDivSocArchivalUnit extends LockssTestCase {
  private MockLockssDaemon theDaemon;
  
  static final String BASE_URL_KEY = ConfigParamDescr.BASE_URL.getKey();

  private static final String BASE_URL = "https://www.dividedsociety.org/";
  private static final String COLLECTION = "posters";
  private static final String JRNL_NUM = "21";
  private static final String JRNL_ID = "test-me";
  private final Configuration AU_CONFIG_JRNL = ConfigurationUtil.fromArgs(
	      BASE_URL_KEY, BASE_URL,
	      "journal_number", JRNL_NUM,
	      "journal_id", JRNL_ID);
  private final Configuration AU_CONFIG_COLL = ConfigurationUtil.fromArgs(
	      BASE_URL_KEY, BASE_URL,
	      "collection_id", COLLECTION);

  
  private static final Logger log = Logger.getLogger(TestDivSocArchivalUnit.class);
  
  static final String COLL_PLUGIN_ID = "org.lockss.plugin.dividedsociety.ClockssDividedSocietyCollectionSnapshotPlugin";
  static final String CollPluginName = "Divided Society Collection Snapshot Plugin (CLOCKSS)";
  static final String JRNL_PLUGIN_ID = "org.lockss.plugin.dividedsociety.ClockssDividedSocietySnapshotPlugin";
  static final String JrnlPluginName = "Divided Society Snapshot Plugin (CLOCKSS)";
  
  
  public void setUp() throws Exception {
    super.setUp();
    setUpDiskSpace();
    theDaemon = getMockLockssDaemon();
    theDaemon.getHashService();
  }
  
  public void tearDown() throws Exception {
    super.tearDown();
  }
  
  private DefinableArchivalUnit makeAu(String pluginid)
      throws Exception {
    
    DefinablePlugin ap = new DefinablePlugin();
    DefinableArchivalUnit au;
    if (pluginid == COLL_PLUGIN_ID) {
        ap.initPlugin(getMockLockssDaemon(), COLL_PLUGIN_ID);
        au = (DefinableArchivalUnit)ap.createAu(AU_CONFIG_COLL);
    } else {
        ap.initPlugin(getMockLockssDaemon(), JRNL_PLUGIN_ID);
        au = (DefinableArchivalUnit)ap.createAu(AU_CONFIG_JRNL);
    }
    return au;
  }
  
  //
  // Test the crawl rules
  //
  
  public void testShouldCacheProperPagesCollection() throws Exception {
    ArchivalUnit  coll_au = makeAu(COLL_PLUGIN_ID);
    theDaemon.getLockssRepository(coll_au);
    theDaemon.getNodeManager(coll_au);
    BaseCachedUrlSet coll_cus = new BaseCachedUrlSet(coll_au,new RangeCachedUrlSetSpec(BASE_URL));
    

    // TRUE FOR BOTH FLAVORS - ALLOWED
    shouldCacheTest("http://clockss-ingest.lockss.org/clockss.txt", true,coll_au,coll_cus);
    shouldCacheTest(BASE_URL + "4e566d92-3778-41c0-b7d7-c9fe7bd71aa9/ip_access", true,coll_au,coll_cus);
    shouldCacheTest(BASE_URL + "user", true,coll_au,coll_cus);
    shouldCacheTest(BASE_URL + "user/9742", true,coll_au,coll_cus);
    shouldCacheTest(BASE_URL + "posters",true,coll_au,coll_cus);
    shouldCacheTest(BASE_URL + "posters?page=16", true,coll_au,coll_cus);
    
    // Content and supporting files
    shouldCacheTest(BASE_URL + "posters/15-festival-de-cinema", true,coll_au,coll_cus);
    shouldCacheTest(BASE_URL + "sites/default/files/styles/poster_watermark/public/PPO-0925.jpg?itok=qmNOWdE5", true,coll_au,coll_cus);
    shouldCacheTest(BASE_URL + "sites/default/files/thumbnails/44115/44116/200x200_WOR2_001_001.jpg", true,coll_au,coll_cus);
    shouldCacheTest(BASE_URL + "sites/default/files/styles/poster_large/public/PPO-0567.jpg?itok=xU96hRaF", true,coll_au,coll_cus);
    // from outreach, but same plugin
    //https://dl.dropboxusercontent.com/s/iajnk1x6ozf9f45/you%20got%20so%20used%20to%20it%20you%20thought%20nothing%20of%20it.mp3
    shouldCacheTest("https://dl.dropboxusercontent.com/s/iajnk1x6ozf9f45/you%20got%20so%20used%20to%20it%20you%20thought%20nothing%20of%20it.mp3", true,coll_au,coll_cus);
    shouldCacheTest(BASE_URL + "sites/default/files/2017-11/1%20DowningStreetDeclaration_ARTWORK_FORAPPROVAL.pdf", true,coll_au,coll_cus);
    shouldCacheTest(BASE_URL + "sites/default/files/inline-images/HLF_003.jpg", true,coll_au,coll_cus);
    
    // NOPE
    shouldCacheTest(BASE_URL + "outreach",false,coll_au,coll_cus);
    // exclude the various search combinations for poster selection -too many
    shouldCacheTest(BASE_URL + "?f%5B0%5D=topic_poster%3AApartheid&f%5B1%5D=topic_poster%3ASecurity&f%5B2%5D=people_posters%3ASeamus%20Mallon", false, coll_au, coll_cus);
    // for a specific journal
    shouldCacheTest(BASE_URL + "archive/journals/959/issues", false,coll_au,coll_cus);
    shouldCacheTest(BASE_URL + "archive/journals/issues/960/articles", false,coll_au,coll_cus);    
  }
    
  public void testShouldCacheProperPages() throws Exception {
	    ArchivalUnit  jrnl_au = makeAu(JRNL_PLUGIN_ID);
	    theDaemon.getLockssRepository(jrnl_au);
	    theDaemon.getNodeManager(jrnl_au);
	    BaseCachedUrlSet jrnl_cus = new BaseCachedUrlSet(jrnl_au,new RangeCachedUrlSetSpec(BASE_URL));
	    

	    // TRUE FOR BOTH FLAVORS - ALLOWED
	    shouldCacheTest("http://clockss-ingest.lockss.org/clockss.txt", true,jrnl_au,jrnl_cus);
	    shouldCacheTest(BASE_URL + "4e566d92-3778-41c0-b7d7-c9fe7bd71aa9/ip_access", true,jrnl_au,jrnl_cus);
	    shouldCacheTest(BASE_URL + "user", true,jrnl_au,jrnl_cus);
	    shouldCacheTest(BASE_URL + "user/9742", true,jrnl_au,jrnl_cus);
	    shouldCacheTest(BASE_URL + "archive/journals/21/issues", true,jrnl_au,jrnl_cus);
	    shouldCacheTest(BASE_URL + "archive/journals/issues/960/articles", true,jrnl_au,jrnl_cus);
	    
	    // Content and supporting files
	    shouldCacheTest(BASE_URL + "journals/test-me/november-1998/bicentennial-fellowship", true,jrnl_au,jrnl_cus);
	    shouldCacheTest(BASE_URL + "libraries/pdf.js/web/viewer.html?file=https%3A%2F%2Fdl.dropboxusercontent.com%2Fs%2F6lqxfim7xj97y8j%2FALU_001_006.pdf", true,jrnl_au,jrnl_cus);
	    shouldCacheTest("https://dl.dropboxusercontent.com/s/6lqxfim7xj97y8j/ALU_001_006.pdf", true,jrnl_au,jrnl_cus);
	    shouldCacheTest(BASE_URL + "sites/default/files/thumbnails/959/960/800x800_ALU_001_002.jpg", true,jrnl_au,jrnl_cus);
	    shouldCacheTest(BASE_URL + "sites/default/files/styles/poster_watermark/public/PPO-0925.jpg?itok=qmNOWdE5", true,jrnl_au,jrnl_cus);
	    shouldCacheTest(BASE_URL + "sites/default/files/thumbnails/44115/44116/200x200_WOR2_001_001.jpg", true,jrnl_au,jrnl_cus);

	    // from outreach, but mp3 also allowed
	    shouldCacheTest("https://dl.dropboxusercontent.com/s/iajnk1x6ozf9f45/you%20got%20so%20used%20to%20it%20you%20thought%20nothing%20of%20it.mp3", true,jrnl_au,jrnl_cus);
	    shouldCacheTest(BASE_URL + "sites/default/files/2017-11/1%20DowningStreetDeclaration_ARTWORK_FORAPPROVAL.pdf", true,jrnl_au,jrnl_cus);
	    shouldCacheTest(BASE_URL + "sites/default/files/inline-images/HLF_003.jpg", true,jrnl_au,jrnl_cus);
	    // NOPE
	    // other journal 
	    shouldCacheTest(BASE_URL + "archive/journals/456/issues", false,jrnl_au,jrnl_cus);
	    shouldCacheTest(BASE_URL + "journals/other/november-1998/bicentennial-fellowship", false,jrnl_au,jrnl_cus);
	    // other plugin
	    shouldCacheTest(BASE_URL + "outreach",false,jrnl_au,jrnl_cus);
	    shouldCacheTest(BASE_URL + "posters",false,jrnl_au,jrnl_cus);
	    shouldCacheTest(BASE_URL + "posters?page=16", false,jrnl_au,jrnl_cus);
	  }

  
  private void shouldCacheTest(String url, boolean shouldCache,
      ArchivalUnit au, CachedUrlSet cus) {
    assertEquals(shouldCache, au.shouldBeCached(url));
  }

  /*
  public void testStartUrlConstruction() throws Exception {
    String expected = BASE_URL + "journals/all_issues.php?issn=" + JOURNAL_ISSN;
    
    DefinableArchivalUnit au = makeAu();
    assertEquals(ListUtil.list(expected), au.getStartUrls());
  }
  
  
  public void testGetName() throws Exception {
    DefinableArchivalUnit au = makeAu();
    
    assertEquals(PluginName + ", Base URL " + BASE_URL + ", ISSN " + JOURNAL_ISSN +
        ", Volume " + VOLUME, au.getName());
  }
  */
}

