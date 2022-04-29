/*
 * $Id$
 */

/*

Copyright (c) 2017 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.plugin.silverchair.oup;

import java.util.List;
import java.util.Properties;

import org.apache.oro.text.regex.Pattern;
import org.apache.oro.text.regex.PatternMatcher;
import org.lockss.config.Configuration;
import org.lockss.daemon.*;
import org.lockss.plugin.*;
import org.lockss.plugin.definable.*;
import org.lockss.test.*;
import org.lockss.util.*;

//
// This plugin test framework is set up to run the same tests in two variants - CLOCKSS
// without having to actually duplicate any of the written tests
//
public class TestOupScArchivalUnit extends LockssTestCase {
  private MockLockssDaemon theDaemon;
  static final String BASE_URL_KEY = ConfigParamDescr.BASE_URL.getKey();
  static final String JID_KEY = "journal_id";
  static final String YEAR_KEY = ConfigParamDescr.YEAR.getKey();
  
  static Logger log = Logger.getLogger(TestOupScArchivalUnit.class);
  
  static final String PLUGIN_ID = "org.lockss.plugin.silverchair.oup.ClockssOupSilverchairPlugin";
  static final String ROOT_URL = "http://academic.oup.com/";
  
  public void setUp() throws Exception {
    super.setUp();
    setUpDiskSpace();
    theDaemon = getMockLockssDaemon();
    theDaemon.getHashService();
  }

  public void tearDown() throws Exception {
    super.tearDown();
  }
  
  private DefinableArchivalUnit makeAu(String journal_id, String year)
      throws Exception {
    
    Properties props = new Properties();
    props.setProperty(JID_KEY, journal_id);
    props.setProperty(YEAR_KEY, year);
    props.setProperty(BASE_URL_KEY, ROOT_URL);
    Configuration config = ConfigurationUtil.fromProps(props);
    
    DefinablePlugin ap = new DefinablePlugin();
    ap.initPlugin(theDaemon,PLUGIN_ID);
    DefinableArchivalUnit au = (DefinableArchivalUnit)ap.createAu(config);
    return au;
  }

  List<String> substanceList = ListUtil.list(
      ROOT_URL+"ptj/article-pdf/31/1/119/6999904/btu602.pdf");
  
  List<String> notSubstanceList = ListUtil.list(
      ROOT_URL+"ptj/article/31/1/119/13244/PseKNC-Hello-World",
      ROOT_URL+"ptj/article-abstract/31/1/119/13244/PseKNC-Hello-World");
  
  public void testCheckSubstanceRules() throws Exception {
    boolean found;
    ArchivalUnit jsAu = makeAu("ptj","2012");
    PatternMatcher matcher = RegexpUtil.getMatcher();   
    List<Pattern> patList = jsAu.makeSubstanceUrlPatterns();

    for (String nextUrl : substanceList) {
      log.debug3("testing for substance: "+ nextUrl +" with pattern" + patList.get(0).getPattern());
      found = false;
      for (Pattern nextPat : patList) {
        found = matcher.matches(nextUrl, nextPat);
        if (found) break;
      }
      assertEquals(true,found);
    }
    
    for (String nextUrl : notSubstanceList) {
      log.debug3("testing for not substance: "+ nextUrl);
      found = false;
      for (Pattern nextPat : patList) {
        found = matcher.matches(nextUrl, nextPat);
        if (found) break;
      }
      assertEquals(false,found);
    }
  }
  
  // Test the crawl rules for plugin
  public void testShouldCacheProperPages() throws Exception {
    ArchivalUnit au = makeAu("database","2015");
    theDaemon.getLockssRepository(au);
    // Test for pages that should get crawled
    
    // toc page for an issue
    shouldCacheTest(ROOT_URL + "database/issue/volume/2015", true, au);
    shouldCacheTest(ROOT_URL + "database/list-of-issues/2015", true, au);
    
    //more issue TOC links
    shouldCacheTest(ROOT_URL + "database/issue/46/6",true, au);
    shouldCacheTest(ROOT_URL + "database/issue/46/Suppl_3",true, au);
    shouldCacheTest(ROOT_URL + "database/issue/46/suppl_2",true, au);
    // but exclude the following - they're also filtered, but just in case
    shouldCacheTest(ROOT_URL + "database/issue/46/6?browseBy=volume",false, au);
    shouldCacheTest(ROOT_URL + "database/issue/46/Suppl_3?browseBy=volume",false, au);
    
    // meeting abstract TOCs use a search url
    shouldCacheTest(ROOT_URL + "database/search-results?q=&f_IssueNo=suppl_1&f_Volume=46&f_TocCategories=Scientific%20Research%20^S%20Neurology%20and%20Neurosciences", true, au);
    
    // article files
    shouldCacheTest(ROOT_URL + "database/article/2433123/ProtoBug-functional-families-from-the-complete", true, au);
    shouldCacheTest(ROOT_URL + "database/article-abstract/2433123/ProtoBug-functional-families-from-the-complete", true, au);
    shouldCacheTest(ROOT_URL + "database/article-pdf/doi/10.1093/database/bau122/7298443/bau122.pdf", true, au);
    
    
    shouldCacheTest(ROOT_URL + "database/downloadcitation/2433123?format=ris", true, au);
    // but not the other formats
    shouldCacheTest(ROOT_URL + "database/downloadcitation/2433123?format=bibtex", false, au);
    shouldCacheTest(ROOT_URL + "database/downloadcitation/2433123?format=txt", false, au);
    shouldCacheTest(ROOT_URL + "database/downloadcitation/2433123?format=anythingelse", false, au);
    shouldCacheTest(ROOT_URL + "data/sitebuilderassetsoriginals/images/database/database_feature_panel.jpg", true, au);
    shouldCacheTest(ROOT_URL + "UI/app/img/apple-touch-icon.png", true, au);
    shouldCacheTest(ROOT_URL + "UI/app/img/favicon-16x16.png", true, au);
    shouldCacheTest(ROOT_URL + "UI/app/img/favicon.ico", true, au);
    shouldCacheTest(ROOT_URL + "UI/app/img/safari-pinned-tab.svg", true, au);
    
    shouldCacheTest("https://ajax.googleapis.com/ajax/libs/jquery/1.8.3/jquery.min.js", true, au);
    shouldCacheTest("https://cdn.jsdelivr.net/chartist.js/latest/chartist.min.css", true, au);
    shouldCacheTest("https://fonts.googleapis.com/css?family=Merriweather:300,400,400italic,700,700italic%7CSource+Sans+Pro:400,400italic,700,700italic", true, au);
    shouldCacheTest("https://fonts.gstatic.com/s/merriweather/v15/EYh7Vl4ywhowqULgRdYwICxQL91WRy8t8mPvAX_dIgA.ttf", true, au);
    shouldCacheTest("https://fonts.gstatic.com/s/sourcesanspro/v10/fpTVHK8qsXbIeTHTrnQH6Edtd7Dq2ZflsctMEexj2lw.ttf", true, au);
    shouldCacheTest("https://oup.silverchair-cdn.com/cassette.axd/file/UI/app/fonts/icomoon-10c8cce3e34f3a0fe0d722e3ee322184b824f902.ttf?2wsrjz", false, au);
    shouldCacheTest("https://oup.silverchair-cdn.com/cassette.axd/script/92f525bab295d0ffa6f942402d5f7034757b1440/OupCookiePolicyJS", false, au);
    shouldCacheTest("https://oup.silverchair-cdn.com/data/SiteBuilderAssets/Live/CSS/database/Site1329903961.css", true, au);
    
    // non-redirecting supplemental data, images and figures with expiration but only for constant expiration time of 2032: 2147483647? 
    shouldCacheTest("https://oup.silverchair-cdn.com/oup/backfile/Content_public/Journal/database/46/1/10.1093_ageing_afw131/11/aa-15-0870-File001.docx?Expires=2147483647&Signature=wDg8A__&Key-Pair-Id=APKAIE5G5CRDK6RD3PGA", true, au);
    shouldCacheTest("https://oup.silverchair-cdn.com/oup/backfile/Content_public/Journal/database/46/1/10.1093_ageing_afw152/12/afw152_Supplementary_Data.zip?Expires=2147483647&Signature=TKw__&Key-Pair-Id=APKAIE5G5CRDK6RD3PGA", true, au);
    // wrong Expires
    shouldCacheTest("https://oup.silverchair-cdn.com/oup/backfile/Content_public/Journal/database/46/1/10.1093_ageing_afw131/11/aa-15-0870-File001.docx?Expires=1497052403&Signature=wDg8A__&Key-Pair-Id=APKAIE5G5CRDK6RD3PGA", false, au);
    //PDF files start at 
    shouldCacheTest(ROOT_URL + "database/article-pdf/46/1/119/11062483/afw166.pdf", true, au);
    // and redirect to a variable expiring url but as they're stored at the canonical, this is okay
    shouldCacheTest("https://oup.silverchair-cdn.com/oup/backfile/Content_public/Journal/database/46/1/10.1093_ageing_afw166/11/afw166.pdf?Expires=1524256967&Signature=wzdP26Lxkw__&Key-Pair-Id=APKAIE5G5CRDK6RD3PGA", true, au);
    
    // TODO - expiration needs to be the firm steady date
    shouldCacheTest("https://oup.silverchair-cdn.com/oup/backfile/Content_public/Journal/database/2015/10.1093_database_bau122/2/bau122f1p.png?Expires=1497074690&Signature=S60KGC7x1rMgczcd6O-A__&Key-Pair-Id=APKAIULVPAVW3Q", false, au);
    // now with the expected date... 
    shouldCacheTest("https://oup.silverchair-cdn.com/oup/backfile/Content_public/Journal/database/2015/10.1093_database_bau122/2/bau122f1p.png?Expires=2147483647&Signature=S60KGC7x1rMgczcd6O-A__&Key-Pair-Id=APKAIULVPAVW3Q", true, au);
    // toc, front-matter, et al PDFs with expiration are now preserved? XXX
    shouldCacheTest("https://oup.silverchair-cdn.com/oup/backfile/Content_public/Journal/database/Issue/461/1/toc.pdf" +
        "?Expires=1497074690&Signature=S60KGC7x1rMgczcd6O-A__&Key-Pair-Id=APKAIULVPAVW3Q", true, au);
    shouldCacheTest("https://oup.silverchair-cdn.com/oup/backfile/Content_public/Journal/database/Issue/461/1/front-matter.pdf" +
        "?Expires=1497074690&Signature=S60KGC7x1rMgczcd6O-A__&Key-Pair-Id=APKAIULVPAVW3Q", true, au);
    
    // www. embedded in url
    shouldCacheTest(ROOT_URL + "database/article/2433123/ProtoBug-functional-families-from-the-complete/www.foo.bar?param", false, au);
    // some additional special items
    shouldCacheTest("https://academic.oup.com/database/article/76/7/578/%5BXSLTImagePath%5D", false, au);
    shouldCacheTest("https://academic.oup.com/database/article/76/7/578/[XSLTImagePath]", false, au);
    shouldCacheTest("https://academic.oup.com/database/article/76/7/578/3850161", true, au);
    
    // should not get crawled - LOCKSS
    shouldCacheTest("http://lockss.stanford.edu", false, au);
  }
  
  private void shouldCacheTest(String url, boolean shouldCache, ArchivalUnit au) {
    //log.info ("shouldCacheTest url: " + url);
    assertEquals(shouldCache, au.shouldBeCached(url));
  }
  
}

