/*
 * $Id:$
 */

/*

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

package org.lockss.plugin.pub2web;

import java.net.URL;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;
import org.lockss.config.Configuration;
import org.lockss.daemon.*;
import org.lockss.plugin.*;
import org.lockss.plugin.base.BaseCachedUrlSet;
import org.lockss.plugin.definable.*;
import org.lockss.state.AuState;
import org.lockss.test.*;
import org.lockss.util.*;

public class TestPub2WebArchivalUnit extends LockssTestCase {
  private MockLockssDaemon theDaemon;
  static final String BASE_URL_KEY = ConfigParamDescr.BASE_URL.getKey();
  static final String JID_KEY = ConfigParamDescr.JOURNAL_ID.getKey();
  static final String DOI_KEY = "doi";
  static final String VOL_KEY = ConfigParamDescr.VOLUME_NAME.getKey();
  static final String ROOT_URL = "http://www.jrnl.com/"; //this is not a real url
  static final String BOOK_ROOT_URL = "http://www.book.com/"; //this is not a real url

  private static final Logger log = Logger.getLogger(TestPub2WebArchivalUnit.class);

  // variants for different plugins - ultimately may need to split the test
  static final String MS_PLUGIN_ID = "org.lockss.plugin.pub2web.ms.ClockssMicrobiologySocietyJournalsPlugin";
  static final String ASM_PLUGIN_ID = "org.lockss.plugin.pub2web.asm.ClockssASMscienceJournalsPlugin";
  static final String ASM_BOOK_PLUGIN_ID = "org.lockss.plugin.pub2web.asm.ClockssASMscienceBooksPlugin";
  static final String MS_PluginName = "Microbiology Society Journals Plugin (CLOCKSS)";
  static final String ASM_PluginName = "ASMScience Journals Plugin (CLOCKSS)";
  static final String ASM_BookPluginName = "ASMscience Books Plugin (CLOCKSS)";

  
  static final String P2WRepairList[] = 
    {
    "(.+\\.mathjax\\.org|code\\.jquery\\.com|://[^/]+/(css|files|images|js|marketing)/)",
    "/docserver/preview/.*\\.gif$",
    };

  static final String P2W_BooksRepairList[] = 
    {
    "(://[^/]+/(css|files|images|js|marketing)/)",
    "/docserver/preview/.*\\.gif$",
    };
  

  public void setUp() throws Exception {
    super.setUp();
    setUpDiskSpace();
    theDaemon = getMockLockssDaemon();
    theDaemon.getHashService();
  }

  public void tearDown() throws Exception {
    super.tearDown();
  }

  private DefinableArchivalUnit makeAu(String plugin_id, URL base_url, int volume, String jid)
      throws Exception {

    Properties props = new Properties();
    props.setProperty(VOL_KEY, Integer.toString(volume));
    props.setProperty(JID_KEY, jid);
    if (base_url != null) {
      props.setProperty(BASE_URL_KEY, base_url.toString());
    }
    Configuration config = ConfigurationUtil.fromProps(props);

    DefinablePlugin ap = new DefinablePlugin();
    ap.initPlugin(getMockLockssDaemon(),
        plugin_id);
    DefinableArchivalUnit au = (DefinableArchivalUnit)ap.createAu(config);
    return au;
  }
  
  private DefinableArchivalUnit makeBookAu(String plugin_id,URL base_url, String doi)
      throws Exception {

    Properties props = new Properties();
    props.setProperty(DOI_KEY, doi);
    if (base_url != null) {
      props.setProperty(BASE_URL_KEY, base_url.toString());
    }
    Configuration config = ConfigurationUtil.fromProps(props);

    DefinablePlugin ap = new DefinablePlugin();
    ap.initPlugin(getMockLockssDaemon(),
        plugin_id);
    DefinableArchivalUnit au = (DefinableArchivalUnit)ap.createAu(config);
    return au;
  }

  //
  // Test the crawl rules
  //
  //http://jgv.microbiologyresearch.org/deliver/fulltext/jgv/96/10/3090_jgv000250.pdf?itemId=/content/journal/jgv/10.1099/jgv.0.000250&mimeType=pdf&isFastTrackArticle=      
//  http://jgv.microbiologyresearch.org/deliver/fulltext/jgv/96/10/3072_jgv000259.pdf?itemId=/content/journal/jgv/10.1099/jgv.0.000259&mimeType=pdf
 // excluded: why
//  http://jgv.microbiologyresearch.org/deliver/fulltext/jgv/96/12/toc.pdf?itemId=/content/journal/jgv/96/12/tocpdf1&mimeType=pdf

  // for a journals plugin
  public void testShouldCacheProperPages() throws Exception {
    String REAL_ROOT= "http://jgv.microbiologyresearch.org/";
    String OTHER_ROOT = "http://www.microbiologyresearch.org/";
    URL base = new URL(REAL_ROOT);
    ArchivalUnit  msau = makeAu(MS_PLUGIN_ID, base, 96, "jgv");
    theDaemon.getLockssRepository(msau);
    theDaemon.getNodeManager(msau);
    BaseCachedUrlSet cus = new BaseCachedUrlSet(msau,
        new RangeCachedUrlSetSpec(base.toString()));

    //manifest page
    shouldCacheTest(REAL_ROOT+"content/journal/jgv/clockssissues?volume=96", true, msau, cus);    
    //toc
    //http://jgv.microbiologyresearch.org/content/journal/jgv/96/9
    shouldCacheTest(REAL_ROOT+"content/journal/jgv/96/12", true, msau, cus);    
    //toc contents
    shouldCacheTest(REAL_ROOT+"articles/renderlist.action?fmt=ahah&items=http://sgm.metastore.ingenta.com/content/journal/jgv/10.1099/jgv.0.000294,http://sgm.metastore.ingenta.com/content/journal/jgv/10.1099/jgv.0.000314", true, msau,cus);
   
    // article landing page/abstract
    // http://jgv.microbiologyresearch.org/content/journal/jgv/10.1099/vir.0.070979-0
    shouldCacheTest(REAL_ROOT+"content/journal/jgv/10.1099/vir.0.070979-0", true, msau, cus);
 
    // citation files
    // http://jgv.microbiologyresearch.org/content/journal/jgv/10.1099/vir.0.070979-0/cite/endnote
    shouldCacheTest(REAL_ROOT+"content/journal/jgv/10.1099/vir.0.070979-0/cite/endnote", true, msau, cus);
    shouldCacheTest(REAL_ROOT+"content/journal/jgv/10.1099/vir.0.070979-0/cite/refworks", true, msau, cus);
    shouldCacheTest(REAL_ROOT+"content/journal/jgv/10.1099/vir.0.070979-0/cite/bibtex", true, msau, cus);
    shouldCacheTest(REAL_ROOT+"content/journal/jgv/10.1099/vir.0.070979-0/cite/plaintext", true, msau, cus);

    //but not the citation links off the toc
    shouldCacheTest(REAL_ROOT+"content/journal/jgv/96/12/cite/refworks", false, msau, cus);
    
    //FULL-TEXT
    //crawler friendly
    // http://jgv.microbiologyresearch.org/content/journal/jgv/10.1099/vir.0.069872-0?crawler=true&mimetype=application/pdf
    shouldCacheTest(REAL_ROOT+"content/journal/jgv/10.1099/vir.0.069872-0?crawler=true&mimetype=application/pdf", true, msau, cus);
    shouldCacheTest(REAL_ROOT+"content/journal/jgv/10.1099/vir.0.069872-0?crawler=true&mimetype=html", true, msau, cus);
   
    //pdf links with redirection - leave in this because this type of link handles toc and supplemental data    
    shouldCacheTest(REAL_ROOT+"deliver/fulltext/jgv/96/10/3090_jgv000250.pdf?itemId=/content/journal/jgv/10.1099/jgv.0.000250&mimeType=pdf&isFastTrackArticle=", true, msau, cus);    
    shouldCacheTest(REAL_ROOT+"deliver/fulltext/jgv/96/12/3457_jgv000286.pdf?itemId=/content/journal/jgv/10.1099/jgv.0.000286&mimeType=pdf", true, msau, cus);    
    //redirects to:
    shouldCacheTest(OTHER_ROOT+"docserver/fulltext/jgv/96/12/3457_jgv000286.pdf?expires=1458588778&id=id&accname=guest&checksum=06E5E7675BC310642B40D918B52C8A42", true, msau, cus);     
    
    //supplemental data
    //http://jgv.microbiologyresearch.org/content/journal/jgv/10.1099/jgv.0.000003/supp-data
    //http://jgv.microbiologyresearch.org/deliver/fulltext/jgv/96/1/64816a.pdf?itemId=/content/suppdata/jgv/10.1099/vir.0.064816-0-1&mimeType=pdf
    shouldCacheTest(REAL_ROOT+"content/journal/jgv/10.1099/jgv.0.000003/supp-data", true, msau, cus);
    shouldCacheTest(REAL_ROOT+"deliver/fulltext/jgv/96/1/64816a.pdf?itemId=/content/suppdata/jgv/10.1099/vir.0.064816-0-1&mimeType=pdf", true, msau, cus);
    
    // images (etc.) 
    shouldCacheTest(REAL_ROOT+"content/jgv/10.1099/vir.0.000205.vir000205-f01", true, msau, cus);
    shouldCacheTest(REAL_ROOT+"docserver/fulltext/jgv/96/9/vir000205-f1_thmb.gif", true, msau, cus);
    shouldCacheTest(REAL_ROOT+"docserver/fulltext/jgv/96/11/jgv000266-f4.gif", true, msau, cus);
    
    //supporting files with url args
    shouldCacheTest(REAL_ROOT + "js/sgm/plugins.js?1", true, msau, cus);
    shouldCacheTest("http://cdn.mathjax.org/mathjax/latest/MathJax.js?config=TeX-AMS-MML_HTMLorMML", true, msau, cus);
    
    
    //excluded
    shouldCacheTest(REAL_ROOT+"content/journal/jgv/97/1", false, msau,cus);    

  }

  
  public void testBookShouldCacheProperPages() throws Exception {
    String REAL_ROOT= "http://asmscience.org/";
    URL base = new URL(REAL_ROOT);
    ArchivalUnit  asmbau = makeBookAu(ASM_BOOK_PLUGIN_ID, base, "10.1128/9781555817992");
    theDaemon.getLockssRepository(asmbau);
    theDaemon.getNodeManager(asmbau);
    BaseCachedUrlSet cus = new BaseCachedUrlSet(asmbau,
        new RangeCachedUrlSetSpec(base.toString()));

    //manifest page/book landing page
    shouldCacheTest(REAL_ROOT+"content/book/10.1128/9781555817992", true, asmbau, cus);    
 
    // chapter landing page/abstract
    shouldCacheTest(REAL_ROOT+"content/book/10.1128/9781555817992.ch02", true, asmbau, cus);
    shouldCacheTest(REAL_ROOT+"content/book/10.1128/9781555817992.index", true, asmbau, cus);
    shouldCacheTest(REAL_ROOT+"content/book/10.1128/9781555817992.s0-3", true, asmbau, cus);
    shouldCacheTest(REAL_ROOT+"content/book/10.1128/9781555817992.pre02", true, asmbau, cus);
 
    // citation files
    // http://jgv.microbiologyresearch.org/content/journal/jgv/10.1099/vir.0.070979-0/cite/endnote
    shouldCacheTest(REAL_ROOT+"content/book/10.1128/9781555817992.ch02/cite/endnote", true, asmbau, cus);
    shouldCacheTest(REAL_ROOT+"content/book/10.1128/9781555817992.ch02/cite/refworks", true, asmbau, cus);
    shouldCacheTest(REAL_ROOT+"content/book/10.1128/9781555817992.ch02/cite/bibtex", true, asmbau, cus);
    shouldCacheTest(REAL_ROOT+"content/book/10.1128/9781555817992.ch02/cite/plaintext", true, asmbau, cus);

    //FULL-TEXT
    //crawler friendly
    // http://jgv.microbiologyresearch.org/content/journal/jgv/10.1099/vir.0.069872-0?crawler=true&mimetype=application/pdf
    shouldCacheTest(REAL_ROOT+"content/book/10.1128/9781555817992.ch02?crawler=true&mimetype=application/pdf", true, asmbau, cus);
    shouldCacheTest(REAL_ROOT+"content/book/10.1128/9781555817992.ch02?crawler=true&mimetype=html", true, asmbau, cus);
    // and the originating links
    shouldCacheTest(REAL_ROOT+"deliver/fulltext/10.1128/9781555817992/chap3.pdf?itemId=/content/book/10.1128/9781555817992.chap3&amp;mimeType=pdf&amp;isFastTrackArticle=", true, asmbau, cus);
    shouldCacheTest(REAL_ROOT+"deliver/fulltext/10.1128/9781555817992/chap3.xml?itemId=/content/book/10.1128/9781555817992.chap3&amp;mimeType=xml&amp;isFastTrackArticle=", true, asmbau, cus);    
    //excluded - other form of html...we ignore
    //shouldCacheTest(REAL_ROOT+"deliver/fulltext/10.1128/9781555817992/chap3.html?itemId=/content/book/10.1128/9781555817992.chap3&amp;mimeType=html&amp;isFastTrackArticle=", false, asmbau,cus);    
    // but allow this version
    shouldCacheTest(REAL_ROOT+"deliver/fulltext/10.1128/9781555817992/chap3.html?itemId=/content/book/10.1128/9781555817992.chap3&amp;mimeType=html&amp;fmt=ahahisFastTrackArticle=", true, asmbau, cus);    
   
    // images (etc.) 
    shouldCacheTest(REAL_ROOT+"content/10.1128/9781555817992.ch02.fig10-1", true, asmbau, cus);
    shouldCacheTest(REAL_ROOT+"docserver/fulltext/10.1128/9781555817992.ch02/fig11-5_thmb.gif", true, asmbau, cus);
    shouldCacheTest(REAL_ROOT+"docserver/fulltext/10.1128/9781555817992.ch02/fig11-6.gif", true, asmbau, cus);
    shouldCacheTest(REAL_ROOT+"docserver/preview/fulltext/10.1128/9781555817992/9781555817992_Chap02-1.gif", true, asmbau, cus);
             
    //supporting files with url args
    shouldCacheTest(REAL_ROOT + "css/asm/bespoke-fonts/OpenSans-BoldItalic-webfont.ttf", true, asmbau, cus);
    shouldCacheTest(REAL_ROOT + "css/asm/bespoke-fonts/OpenSans-BoldItalic-webfont.ttf", true, asmbau, cus);
    shouldCacheTest(REAL_ROOT + "css/jp/ViewNLM.css", true, asmbau, cus);
    shouldCacheTest(REAL_ROOT + "files/2015_Fall_Catalog_Cover.gif", true, asmbau, cus);

  }
  


  private void shouldCacheTest(String url, boolean shouldCache,
      ArchivalUnit au, CachedUrlSet cus) {
    assertEquals(shouldCache, au.shouldBeCached(url));
  }

  public void testStartUrlConstruction() throws Exception {
    URL url = new URL(ROOT_URL);

    // 4 digit
    String expected = ROOT_URL+"content/journal/foo/clockssissues?volume=123";
 
    DefinableArchivalUnit au = makeAu(MS_PLUGIN_ID,url, 123, "foo");
    assertEquals(ListUtil.list(expected), au.getStartUrls());
  }

  public void testMSPollSpecial() throws Exception {
    ArchivalUnit MSAu = makeAu(MS_PLUGIN_ID,new URL("http://jgv.microbiologyresearch.org/"), 96, "jgv");
    theDaemon.getLockssRepository(MSAu);
    theDaemon.getNodeManager(MSAu);


    // if it changes in the plugin, you might need to change the test, so verify
  assertEquals(Arrays.asList(P2WRepairList),
    RegexpUtil.regexpCollection(MSAu.makeRepairFromPeerIfMissingUrlPatterns()));
    
    // make sure that's the regexp that will match to the expected url string
    // this also tests the regexp (which is the same) for the weighted poll map
    List <String> repairList = ListUtil.list(
        "http://jgv.microbiologyresearch.org/images/jp/uibg_glass_75_ffffff_1x400.png",
          "http://jgv.microbiologyresearch.org/css/sgm/bespoke-fonts/fontawesome-webfont.svg?v=4.1.0",
          "http://jgv.microbiologyresearch.org/images/jp/ui-icons_222222_256x240.png",
          "http://jgv.microbiologyresearch.org/images/sgm/Header-shapes-small.png",
          "http://cdn.mathjax.org/mathjax/latest/MathJax.js?config=TeX-AMS-MML_HTMLorMML",
          "http://jgv.microbiologyresearch.org/images/sgm/ijsem_banner.png",
          "https://code.jquery.com/jquery-1.11.1.min.js",
          "http://jgv.microbiologyresearch.org/css/sgm/fulltext-html-tab.css",
          "http://jgv.microbiologyresearch.org/css/sgm/site.css?2",
          "http://jgv.microbiologyresearch.org/css/contentpreview/preview.css",
          "http://jgv.microbiologyresearch.org/css/jp/ViewNLM.css",
          "http://jgv.microbiologyresearch.org/css/jp/ingenta-branding-new.css",
          "http://jgv.microbiologyresearch.org/css/jp/shopping.css",
          "http://jgv.microbiologyresearch.org/css/metrics/metrics.css",
          "http://www.asmscience.org/js/asm/contentpreview/preview.js",
          "http://www.asmscience.org/css/asm/bespoke-fonts/fontawesome-webfont.eot",
          "http://www.asmscience.org/css/asm/bespoke-fonts/glyphicons-halflings-regular.eot",
          "http://www.asmscience.org/css/asm/bespoke-fonts/glyphicons-halflings-regular.svg",
           "http://www.asmscience.org/css/asm/bespoke-fonts/glyphicons-halflings-regular.ttf",
            "http://www.asmscience.org/css/asm/bespoke-fonts/glyphicons-halflings-regular.woff");  
    Pattern p0 = Pattern.compile(P2WRepairList[0]);
    Pattern p1 = Pattern.compile(P2WRepairList[1]);
    Matcher m0, m1;
    for (String urlString : repairList) {
      m0 = p0.matcher(urlString);
      m1 = p1.matcher(urlString);
      assertEquals(urlString, true, m0.find() || m1.find());
    }
     //and this one should fail - it needs to be weighted correctly and repaired from publisher if possible
     String notString ="http://jgv.microbiologyresearch.org/docserver/fulltext/jgv/96/1/064816-f1.gif";
     m0 = p0.matcher(notString);
     m1 = p1.matcher(notString);
     assertEquals(false, m0.find() && m1.find());

     
    PatternFloatMap urlPollResults = MSAu.makeUrlPollResultWeightMap();
    assertNotNull(urlPollResults);
    for (String urlString : repairList) {
      assertEquals(0.0,
          urlPollResults.getMatch(urlString, (float) 1),
          .0001);
    }
    assertEquals(1.0, urlPollResults.getMatch(notString, (float) 1), .0001);
  }
  
  public void testBooksPollSpecial() throws Exception {
    ArchivalUnit  ASMAu = makeBookAu(ASM_BOOK_PLUGIN_ID, new URL("http://www.asmscience.org/"), "10.1128/9781555817992");
    theDaemon.getLockssRepository(ASMAu);
    theDaemon.getNodeManager(ASMAu);


    // if it changes in the plugin, you might need to change the test, so verify
  assertEquals(Arrays.asList(P2W_BooksRepairList),
    RegexpUtil.regexpCollection(ASMAu.makeRepairFromPeerIfMissingUrlPatterns()));
    
    
    // make sure that's the regexp that will match to the expected url string
    // this also tests the regexp (which is the same) for the weighted poll map
    List <String> repairList = ListUtil.list(
        "http://www.asmscience.org/js/asm/contentpreview/preview.js",
        "http://www.asmscience.org/css/asm/bespoke-fonts/fontawesome-webfont.eot",
        "http://www.asmscience.org/css/asm/bespoke-fonts/glyphicons-halflings-regular.eot",
        "http://www.asmscience.org/css/asm/bespoke-fonts/glyphicons-halflings-regular.svg",
         "http://www.asmscience.org/css/asm/bespoke-fonts/glyphicons-halflings-regular.ttf",
          "http://www.asmscience.org/css/asm/bespoke-fonts/glyphicons-halflings-regular.woff");

    Pattern p0 = Pattern.compile(P2W_BooksRepairList[0]);
    Pattern p1 = Pattern.compile(P2W_BooksRepairList[1]);
    Matcher m0, m1;
    for (String urlString : repairList) {
      m0 = p0.matcher(urlString);
      m1 = p1.matcher(urlString);
      assertEquals(urlString, true, m0.find() || m1.find());
    }
     //and this one should fail - it needs to be weighted correctly and repaired from publisher if possible
     String notString ="http://www.asmscience.org/content/book/10.1128/9781555816445.ch07?crawler=true&mimetype=html";
     m0 = p0.matcher(notString);
     m1 = p1.matcher(notString);
     assertEquals(false, m0.find() && m1.find());

     
    PatternFloatMap urlPollResults = ASMAu.makeUrlPollResultWeightMap();
    assertNotNull(urlPollResults);
    for (String urlString : repairList) {
      assertEquals(0.0,
          urlPollResults.getMatch(urlString, (float) 1),
          .0001);
    }
    assertEquals(1.0, urlPollResults.getMatch(notString, (float) 1), .0001);
  }
  
  public void testGetName() throws Exception {
    DefinableArchivalUnit au =
        makeAu(MS_PLUGIN_ID, new URL("http://www.ajrnl.com/"), 33, "blah");
    //Microbiology Society Journals Plugin (CLOCKSS), Base URL %s, Journal ID %s, Volume %s
    assertEquals(MS_PluginName + ", Base URL http://www.ajrnl.com/, Journal ID blah, Volume 33", au.getName());
  }

 
}

