/*
 * $Id: TestBaseAtyponArchivalUnit.java,v 1.1 2013-04-19 22:49:44 alexandraohlson Exp $
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

package org.lockss.plugin.atypon;

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
public class TestBaseAtyponArchivalUnit extends LockssTestCase {
  private MockLockssDaemon theDaemon;
  static final String BASE_URL_KEY = ConfigParamDescr.BASE_URL.getKey();
  static final String JID_KEY = ConfigParamDescr.JOURNAL_ID.getKey();
  static final String VOL_KEY = ConfigParamDescr.VOLUME_NAME.getKey();
  static final String ROOT_URL = "http://www.BaseAtypon.com/"; //this is not a real url
  
  static Logger log = Logger.getLogger("TestBaseAtyponArchivalUnit");
  
  static final String PLUGIN_ID = "org.lockss.plugin.atypon.BaseAtyponPlugin";
  static final String PluginName = "Base Atypon Plugin";
  
  public void setUp() throws Exception {
    super.setUp();
    setUpDiskSpace();
    theDaemon = getMockLockssDaemon();
    theDaemon.getHashService();
  }

  public void tearDown() throws Exception {
    super.tearDown();
  }

  private DefinableArchivalUnit makeAu(URL url, int volume, String jid)
      throws Exception {

    Properties props = new Properties();
    props.setProperty(VOL_KEY, Integer.toString(volume));
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
      makeAu(null, 1, "ajph");
      fail("Should have thrown ArchivalUnit.ConfigurationException");
    } catch (ArchivalUnit.ConfigurationException e) {
    }
  }
  
  //
  // Test the crawl rules
  //
  public void testShouldCacheProperPages() throws Exception {
    URL base = new URL(ROOT_URL);
    ArchivalUnit ABAu = makeAu(base, 123, "xxxx");
    theDaemon.getLockssRepository(ABAu);
    theDaemon.getNodeManager(ABAu);
    BaseCachedUrlSet cus = new BaseCachedUrlSet(ABAu,
        new RangeCachedUrlSetSpec(base.toString()));
    // Test for pages that should get crawled
    //manifest page
    shouldCacheTest(ROOT_URL+"lockss/xxxx/123/index.html", true, ABAu, cus);    
    // toc page for an issue
    shouldCacheTest(ROOT_URL+"toc/xxxx/123/5", true, ABAu, cus);
    // special issue
    shouldCacheTest(ROOT_URL+"doi/abs/11.1111/1234-abc.12G", true, ABAu, cus);
    shouldCacheTest(ROOT_URL+"doi/pdf/11.1111/1234-abc.12G", true, ABAu, cus);
    shouldCacheTest(ROOT_URL+"doi/full/11.1111/1234-abc.12G", true, ABAu, cus);
    shouldCacheTest(ROOT_URL+"doi/pdfplus/11.1111/1234-abc.12G", true, ABAu, cus);
    shouldCacheTest(ROOT_URL+"doi/suppl/11.1111/1234-abc.12G", true, ABAu, cus);
    shouldCacheTest(ROOT_URL+"doi/suppl/11.1111/1234-abc.12G/suppl_stuff.docx", true, ABAu, cus);
    shouldCacheTest(ROOT_URL+"doi/suppl/11.1111/1234-abc.12G/suppl_pres.ppt", true, ABAu, cus);
    shouldCacheTest(ROOT_URL+"doi/ref/11.1111/1234-abc.12G", true, ABAu, cus);
    shouldCacheTest(ROOT_URL+"doi/video_original/11.1111/1234-abc.12G", true, ABAu, cus);
    //other DOI forms
    shouldCacheTest(ROOT_URL+"doi/abs/11.1111/XX12FG", true, ABAu, cus);
    shouldCacheTest(ROOT_URL+"doi/abs/11.1111/mypubfile", true, ABAu, cus);
    shouldCacheTest(ROOT_URL+"doi/abs/11.1111/1111111", true, ABAu, cus);
    // other argument based URLS
    shouldCacheTest(ROOT_URL+"action/showImage?doi=", true, ABAu, cus);
    shouldCacheTest(ROOT_URL+"action/showPopup?citid=citart1&id=T0003&doi=10.1080/19416520.2010.495530", true, ABAu, cus);
    shouldCacheTest(ROOT_URL+"action/showPopup?citid=citart1&id=CIT0013&doi=10.1080/19416521003732362", true, ABAu, cus);
    shouldCacheTest(ROOT_URL+"action/showFullPopup?doi=10.1206%2F3743.2&id=m05", true, ABAu, cus);
    // images figures and tables can live here
    shouldCacheTest(ROOT_URL+"na101/home/literatum/publisher/apha/journals/covergifs/xxxx/2005/15200477-86.6/cover.jpg", true, ABAu, cus);   
    
    // Now a couple that shouldn't get crawled
    // wrong volume
    shouldCacheTest(ROOT_URL+"toc/xxxx/12/index.html", false, ABAu, cus);
    // avoid spider trap
    shouldCacheTest(ROOT_URL+"doi/abs/11.1111/99.9999-99999", false, ABAu, cus);
    // LOCKSS
    shouldCacheTest("http://lockss.stanford.edu", false, ABAu, cus);
    // other sites
  }
  
  private void shouldCacheTest(String url, boolean shouldCache,
      ArchivalUnit au, CachedUrlSet cus) {
    UrlCacher uc = au.makeUrlCacher(url);
    assertEquals(shouldCache, uc.shouldBeCached());
  }
  
  public void testStartUrlConstruction() throws Exception {
    URL url = new URL(ROOT_URL);

    // 4 digit
    String expected = ROOT_URL+"lockss/xxxx/123/index.html";
    DefinableArchivalUnit ABAu = makeAu(url, 123, "xxxx");
    assertEquals(ListUtil.list(expected), ABAu.getNewContentCrawlUrls());
  }
  
  public void testShouldNotCachePageFromOtherSite() throws Exception {
    URL base = new URL("http://www.BaseAtypon.com/");
    int volume = 123;
    String jid = "xxxx";
    ArchivalUnit ABAu = makeAu(base, volume, jid);

    theDaemon.getLockssRepository(ABAu);
    theDaemon.getNodeManager(ABAu);
    CachedUrlSetSpec spec = new RangeCachedUrlSetSpec(base.toString());
    BaseCachedUrlSet cus = new BaseCachedUrlSet(ABAu, spec);
    UrlCacher uc = ABAu.makeUrlCacher("http://shadow2.stanford.edu/lockss/xxxx/123/index.html");

    assertFalse(uc.shouldBeCached());
  }


  public void testShouldDoNewContentCrawlTooEarly() throws Exception {
    ArchivalUnit ABAu =
      makeAu(new URL("http://www.BaseAtypon.com/"), 33, "yyyy");

    AuState aus = new MockAuState(null, TimeBase.nowMs(), -1, -1, null);

    assertFalse(ABAu.shouldCrawlForNewContent(aus));
  }

  public void testShouldDoNewContentCrawlFor0() throws Exception {
    ArchivalUnit ABAu =
      makeAu(new URL("http://www.BaseAtypon.com/"), 33, "yyyy");

    AuState aus = new MockAuState(null, 0, -1, -1, null);

    assertTrue(ABAu.shouldCrawlForNewContent(aus));
  }


  public void testgetName() throws Exception {
    DefinableArchivalUnit au =
      makeAu(new URL("http://www.BaseAtypon.com/"), 33, "yyyy");
    assertEquals(PluginName + ", Base URL http://www.BaseAtypon.com/, Journal ID yyyy, Volume 33", au.getName());
    DefinableArchivalUnit au1 =
      makeAu(new URL("http://www.apha.com/"), 24, "apha");
    assertEquals(PluginName + ", Base URL http://www.apha.com/, Journal ID apha, Volume 24", au1.getName());
  }

}

