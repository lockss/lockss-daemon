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

package org.lockss.plugin.elifesciences;

import java.net.URL;
import java.util.*;

import org.apache.commons.lang.StringUtils;
import org.lockss.config.Configuration;
import org.lockss.daemon.*;
import org.lockss.plugin.*;
import org.lockss.plugin.base.BaseCachedUrlSet;
import org.lockss.plugin.definable.*;
import org.lockss.state.AuState;
import org.lockss.test.*;
import org.lockss.util.*;

public class TestELifeArchivalUnit extends LockssTestCase {
  private MockLockssDaemon theDaemon;
  static final String BASE_URL_KEY = ConfigParamDescr.BASE_URL.getKey();
  static final String YM_KEY = "year_month";
  static final String ROOT_URL = "http://www.jrnl.com/"; //this is not a real url

  private static final Logger log = Logger.getLogger(TestELifeArchivalUnit.class);

  static final String PLUGIN_ID = "org.lockss.plugin.elifesciences.ELifeSciencesPlugin";
  static final String PluginName = "eLife Sciences Plugin";

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

 
}

