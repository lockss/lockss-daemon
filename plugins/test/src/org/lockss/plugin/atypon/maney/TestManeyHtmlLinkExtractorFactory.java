/*
 * $Id: TestManeyHtmlLinkExtractorFactory.java,v 1.2 2014-08-28 18:28:31 alexandraohlson Exp $
 */
/*

 Copyright (c) 2000-2014 Board of Trustees of Leland Stanford Jr. University,
 all rights reserved.

 Permission is hereby granted, free of charge, to any person obtaining a copy
 of his software and associated documentation files (the "Software"), to deal
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

package org.lockss.plugin.atypon.maney;

import java.util.Set;
import org.lockss.plugin.UrlNormalizer;
import org.lockss.plugin.atypon.BaseAtyponUrlNormalizer;
import org.lockss.extractor.LinkExtractor;
import org.lockss.extractor.RegexpCssLinkExtractor;
import org.lockss.test.LockssTestCase;
import org.lockss.test.MockArchivalUnit;
import org.lockss.test.MockCachedUrl;
import org.lockss.util.Constants;
import org.lockss.util.SetUtil;


public class TestManeyHtmlLinkExtractorFactory extends LockssTestCase {
  UrlNormalizer normalizer = new BaseAtyponUrlNormalizer();

  private ManeyAtyponHtmlLinkExtractorFactory fact;
  private LinkExtractor m_extractor;
  private MyLinkExtractorCallback m_callback;
  static String ENC = Constants.DEFAULT_ENCODING;
  private MockArchivalUnit m_mau;
  private final String MANEY_BASE_URL = "http://www.maneyonline.com/";
  private static final String DOI_START = "11.1111";
  private static final String DOI_END = "TEST";

  //temporary - waiting for fix to JSoup tag support 
  private static final String SCRIPT_TAG="script";
  /*
   * First the html for a limited test - just the image tag that needs expanding
   */
  private static final String test1_urlprefix = 
      "na101/home/literatum/publisher/maney/journals/" +                                                                 
      "content/amb/2013/amb.2013.60.issue-3/0002698013z.00000000033/production" +                                                                    
      "images/";

  private static final String test1_figureLinksHtml=
      "<html><head><title>Test Title</title></head><body>" +
          "<div class=\"holder\">" +
          "<a title=\"Open Figure Viewer\" onclick=\"showFigures(this,event); return false;\" href=\"JavaScript:void(0);\" class=\"thumbnail\">" +
          "<img alt=\"figure\" " +
          "src=\"/" + test1_urlprefix + "/images/small/s3-g1.gif\">" +
          "</img>" +
          "</a>" +
          "<span class=\"overlay\"></span>" +
          "</div>" +
          "<div>" +
          "<" + SCRIPT_TAG + " type=\"text/javascript\">" +
          "  window.figureViewer={doi:\'10.1179/0002698013Z.00000000033\',\n" +
          "path:\'/" + test1_urlprefix + "\',figures:[{i:\'S3F1\',g:[{m:\'s3-g1.gif\',l:\'s3-g1.jpeg\',size:\'116 kB\'}]}" +
          "   ,{i:\'S3F2\',g:[{m:\'s3-g2.gif\',l:\'s3-g2.jpeg\',size:\'70 kB\'}]}" +
          "   ,{i:\'S3F3\',g:[{m:\'s3-g3.gif\',l:\'s3-g3.jpeg\',size:\'61 kB\'}]}" +
          "   ,{i:\'S3F4\',g:[{m:\'s3-g4.gif\',l:\'s3-g4.jpeg\',size:\'37 kB\'}]}" +
          "   ,{i:\'S3F5\',g:[{m:\'s3-g5.gif\',l:\'s3-g5.jpeg\',size:\'28 kB\'}]}" +
          "   ]}</" + SCRIPT_TAG + ">" +
          "</div>" +
          
          "</body>" +
          "</html>";
  private static final String test2_urlprefix = "na101/home/literatum/publisher/maney/journals/content/amb/2013/" +
          "amb.2013.60.issue-4/0002698013z.00000000038/20131129";
  private static final String test2_figureLinksHtml = 
  "<html><head><title>Test Title</title></head><body>" +
  "<div class=\"holder\">" +
  "<a title=\"Open Figure Viewer\" onclick=\"showFigures(this,event); return false;\" href=\"JavaScript:void(0);\" class=\"thumbnail\">" +
  "<img alt=\"figure\" src=\"/" + test2_urlprefix + "/images/small/0002698013z.00000000038.01.gif\" />" +
  "</a>" +
  "<" + SCRIPT_TAG + " type=\"text/javascript\">" +
  "  window.figureViewer={doi:\'10.1179/0002698013Z.00000000038\'," +
  "path:\'/" + test2_urlprefix + "\'," +
  "figures:[{i:\'F1\',g:[{m:\'0002698013z.00000000038.01.gif\',l:" +
  "\'0002698013z.00000000038.01.jpeg\',size:\'102 KB\'}]}" +
  " ]}</" + SCRIPT_TAG + ">" +
  "</div>" +
          "</body>" +
          "</html>";
  /*
   * Now more complete html code to test that other links still work as expected
   * 
   */
  private static final String fullHtml =
  "<!DOCTYPE html>" +
  "<html lang=\"en\" class=\"pb-page\">" +
  "<head data-pb-dropzone=\"head\">" +
  "<title>TEST FOO</title>" +
  "<script type=\"text/javascript\" src=\"/wro/product.js\"></script>" +
  "<link rel=\"stylesheet\" type=\"text/css\" href=\"/wro/product.css\">" +
  "</head>" +
  "<body>" +
  " <div id=\"pb-page-content\">" +
  " <div data-pb-dropzone=\"main\" data-pb-dropzone-name=\"Main\">" +
  " <section class=\"widget general-image none  widget-none  widget-compact\" id=\"ec8b7042-594c-48ae-b50a-619a7c70b9b6\">" +
  " <div class=\"wrapped 1_12\" >" +
  " <div class=\"widget-body body body-none  body-compact\"><a href=\"/\">" +
  " <img src=\"/pb/assets/raw/Maney_logo.jpg\"/>" +
  " </a></div>" +
  "  </div>" +
  "  </section>" +
  "</div></div></body></html>";

  
  private static final String test3_figureLinksWithSpacesAndNewlines=
      "<html><head><title>Test Title</title></head><body>" +
          "<div class=\"holder\">" +
          "<a title=\"Open Figure Viewer\" onclick=\"showFigures(this,event); return false;\" href=\"JavaScript:void(0);\" class=\"thumbnail\">" +
          "<img alt=\"figure\" " +
          "src=\"/" + test1_urlprefix + "/images/small/s3-g1.gif\">" +
          "</img>" +
          "</a>" +
          "<span class=\"overlay\"></span>" +
          "</div>" +
          "<div>" +
          "<" + SCRIPT_TAG + " type=\"text/javascript\">" +
          "    window.figureViewer={  doi  : \'10.1179/0002698013Z.00000000033\'  ,  \n  " +
          " path : \'/" + test1_urlprefix + "\' ,  figures : [ { i : \'S3F1\' , \n " +
          		" g : [ { m : \'s3-g1.gif\' , l : \'s3-g1.jpeg\',size:\'116 kB\' } ] }\n" +
          "\n   ] } </" + SCRIPT_TAG + ">" +
          "</div>" +
          "\n" +          
          "</body>" +
          "</html>";

  @Override
  public void setUp() throws Exception {
    super.setUp();
    m_mau = new MockArchivalUnit();
    m_callback = new MyLinkExtractorCallback();
    fact = new ManeyAtyponHtmlLinkExtractorFactory();
    m_extractor = fact.createLinkExtractor("html");

  }
  Set<String> expectedUrls;


  public void test1FigureLinks() throws Exception {
    /* 
     * the snippet of html used to set this up only establishes the small
     * size for the first image. So it is correct that the other images, which
     * come form the figureViewer only include medium and large
     */
    expectedUrls = SetUtil.set(
        MANEY_BASE_URL + test1_urlprefix + "/images/small/s3-g1.gif",
        MANEY_BASE_URL + test1_urlprefix + "/images/medium/s3-g1.gif",
        MANEY_BASE_URL + test1_urlprefix + "/images/large/s3-g1.jpeg",
        MANEY_BASE_URL + test1_urlprefix + "/images/medium/s3-g2.gif",
        MANEY_BASE_URL + test1_urlprefix + "/images/large/s3-g2.jpeg",
        MANEY_BASE_URL + test1_urlprefix + "/images/medium/s3-g3.gif",
        MANEY_BASE_URL + test1_urlprefix + "/images/large/s3-g3.jpeg",
        MANEY_BASE_URL + test1_urlprefix + "/images/medium/s3-g4.gif",
        MANEY_BASE_URL + test1_urlprefix + "/images/large/s3-g4.jpeg",
        MANEY_BASE_URL + test1_urlprefix + "/images/medium/s3-g5.gif",
        MANEY_BASE_URL + test1_urlprefix + "/images/large/s3-g5.jpeg");

    Set<String> result_strings = parseSingleSource(test1_figureLinksHtml, "full");

    assertEquals(11, result_strings.size());
    
    for (String url : result_strings) {
      log.debug3("URL: " + url);
      assertTrue(expectedUrls.contains(url));
    }
    
    // Now try it not on a "full" page, should just extract the one listed lnk 
    result_strings = parseSingleSource(test1_figureLinksHtml, "abs");

    assertEquals(1, result_strings.size());
    for (String url : result_strings) {
      log.debug("URL: " + url);
      assertEquals(MANEY_BASE_URL + test1_urlprefix + "/images/small/s3-g1.gif", url);
    }
    
  }
  
  public void test2FigureLinks() throws Exception {

    expectedUrls = SetUtil.set(
        MANEY_BASE_URL + test2_urlprefix + "/images/small/0002698013z.00000000038.01.gif",
        MANEY_BASE_URL + test2_urlprefix + "/images/medium/0002698013z.00000000038.01.gif",
        MANEY_BASE_URL + test2_urlprefix + "/images/large/0002698013z.00000000038.01.jpeg");

    Set<String> result_strings = parseSingleSource(test2_figureLinksHtml, "full");

    assertEquals(3, result_strings.size());
    
    for (String url : result_strings) {
      log.debug3("URL: " + url);
      assertTrue(expectedUrls.contains(url));
    }
  }

  
  /*
   * This test makes sure other base link extraction continues to work
   */
   public void testfullHtml() throws Exception {
 
    Set<String> result_strings = parseSingleSource(fullHtml, "full");
    expectedUrls = SetUtil.set(
    MANEY_BASE_URL + "pb/assets/raw/Maney_logo.jpg",
    MANEY_BASE_URL + "wro/product.js",
    MANEY_BASE_URL + "wro/product.css",
    MANEY_BASE_URL);

    assertEquals(4, result_strings.size());
    for (String url : result_strings) {
      log.debug("URL: " + url);
      assertTrue(expectedUrls.contains(url));
    }
  }
  
   public void test3FigureLinks() throws Exception {
     /* 
      * Testing to make sure that the image extracgtor can handle spaces
      * and newlines within the regexp section
      */
     expectedUrls = SetUtil.set(
         MANEY_BASE_URL + test1_urlprefix + "/images/small/s3-g1.gif",
         MANEY_BASE_URL + test1_urlprefix + "/images/medium/s3-g1.gif",
         MANEY_BASE_URL + test1_urlprefix + "/images/large/s3-g1.jpeg");

     Set<String> result_strings = parseSingleSource(test3_figureLinksWithSpacesAndNewlines, "full");

     assertEquals(3, result_strings.size());
     
     for (String url : result_strings) {
       log.debug3("URL: " + url);
       assertTrue(expectedUrls.contains(url));
     }
     
   }
  
  
  // this is copied directory from the Jsoup test to make sure that our class extension
  // hasn't broken fallback behavior
  public void testResolvesHtmlEntities() throws Exception {
    String url1 = "http://www.example.com/bioone/?"
        + "request=get-toc&issn=0044-7447&volume=32&issue=1";

    String source = "<html><head><title>Test</title></head><body>"
        + "<a href=http://www.example.com/bioone/?"
        + "request=get-toc&#38;issn=0044-7447&#38;volume=32&issue=1>link1</a>";
    assertEquals(SetUtil.set(url1), parseSingleSource(source, "full"));

    // ensure character entities processed before rel url resolution
    source = "<html><head><title>Test</title></head><body>"
        + "<base href=http://www.example.com/foo/bar>"
        + "<a href=&#46&#46/xxx>link1</a>";
    assertEquals(SetUtil.set("http://www.example.com/xxx"),
        parseSingleSource(source, "full"));
  }


  // allow setting of page type (full, abs) to test whether link extractor limits correctly
  private Set<String> parseSingleSource(String source, String page_type)
      throws Exception {
    String srcUrl = MANEY_BASE_URL + "doi/" + page_type + "/" + DOI_START + "/" + DOI_END;
    MockArchivalUnit m_mau = new MockArchivalUnit();
    LinkExtractor ue = new RegexpCssLinkExtractor();
    m_mau.setLinkExtractor("text/css", ue);
    MockCachedUrl mcu =
        new org.lockss.test.MockCachedUrl(MANEY_BASE_URL + "doi/" + page_type + "/" + DOI_START + "/" + DOI_END, m_mau);
    mcu.setContent(source);

    m_callback.reset();
    m_extractor.extractUrls(m_mau,
        new org.lockss.test.StringInputStream(source), ENC,
        srcUrl, m_callback);
    return m_callback.getFoundUrls();
  }

  private static class MyLinkExtractorCallback implements
  LinkExtractor.Callback {

    Set<String> foundUrls = new java.util.HashSet<String>();

    public void foundLink(String url) {
      foundUrls.add(url);
    }

    public Set<String> getFoundUrls() {
      return foundUrls;
    }

    public void reset() {
      foundUrls = new java.util.HashSet<String>();
    }
  }

}
