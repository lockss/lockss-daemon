/*
 * $Id:$
 */
/*

 Copyright (c) 2000-2015 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.plugin.pub2web.asm;

import java.util.HashSet;
import java.util.Set;
import org.lockss.plugin.UrlNormalizer;
import org.lockss.extractor.JsoupHtmlLinkExtractor;
import org.lockss.extractor.LinkExtractor;
import org.lockss.test.LockssTestCase;
import org.lockss.test.MockArchivalUnit;
import org.lockss.test.MockCachedUrl;
import org.lockss.util.Constants;
import org.lockss.util.SetUtil;


public class TestAsmHtmlLinkExtractorFactory extends LockssTestCase {

  private AsmHtmlLinkExtractorFactory fact;
  private LinkExtractor m_extractor;
  private MyLinkExtractorCallback m_callback;
  static String ENC = Constants.DEFAULT_ENCODING;
  private MockArchivalUnit m_mau;
  private final String BASE_URL = "http://www.asmscience.org/";
  private final String JID = "microbiolspec";


  @Override
  public void setUp() throws Exception {
    //log.setLevel("debug3");
    super.setUp();
    m_mau = new MockArchivalUnit();
    m_callback = new MyLinkExtractorCallback();

    fact = new AsmHtmlLinkExtractorFactory();
    m_extractor = fact.createLinkExtractor("html");

  }

  Set<String> expectedUrls;

  private static final String toc_landing_html =
      "<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Strict//EN\" \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd\">" +
          "<html xmlns=\"http://www.w3.org/1999/xhtml\" xml:lang=\"en\" lang=\"en\">" +
          "<head>" +
          "<meta http-equiv=\"Content-Type\" content=\"text/html; charset=utf-8\" />" +
          "<title>ASMscience | Volume 2, Issue 3</title>" +
          "</head>" +
          "<body id=\"issue\">" +
          "<a href=\"/content/searchbrowse\" title=\"Search and Browse\" ><span id=\"searchandBrowse\">" +
          "Search/Browse" +
          "<img src=\"/images/asm/white-triangle03.png\" alt=\"mega drop down\" /></span></a>" +
          "<div class=\"tocheadingarticlelisting retrieveTocheadingArticle hiddenjsdiv\">" +
          "/content/journal/microbiolspec/2/3/articles?fmt=ahah&tocHeading=http://asm.metastore.ingenta.com/content/journal/microbiolspec/reviewarticle</div>" +
          "</body>" +
          "</html>";

  //http://www.asmscience.org/content/journal/microbiolspec/2/3
  public void testTOCLinks() throws Exception {
    UrlNormalizer normalizer = new AsmUrlNormalizer();
    expectedUrls = SetUtil.set(
        BASE_URL + "content/searchbrowse",
        BASE_URL + "images/asm/white-triangle03.png",
        BASE_URL + "content/journal/microbiolspec/2/3/articles?fmt=ahah&tocHeading=http://asm.metastore.ingenta.com/content/journal/microbiolspec/reviewarticle"
        );      

    Set<String> result_strings = parseSingleSource(toc_landing_html, BASE_URL + "content/journal/" + JID + "/2/3");

    // do you have the expected number
    assertEquals(expectedUrls.size(), result_strings.size());

    if (log.isDebug3()) {
      log.debug3("expectedURLs are:");
      for (String url : expectedUrls) {
        log.debug3(url);
      }
    }

    String norm_url;
    Set<String> norm_urls = new HashSet<String>();

    // normalize each result- and as a first check, is it in the expected list 
    log.debug3("normalizedURLs are:");
    for (String url : result_strings) {
      norm_url = normalizer.normalizeUrl(url, m_mau);
      log.debug3(norm_url);
      assertTrue(expectedUrls.contains(norm_url));
      norm_urls.add(norm_url);
    }

    // did we get every one of the expected urls
    for (String url : expectedUrls) {
      assertTrue(norm_urls.contains(url));
    }
  }


  private static final String article_landing_html =
      "<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Strict//EN\" \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd\">" +
          "<html xmlns=\"http://www.w3.org/1999/xhtml\" xml:lang=\"en\" lang=\"en\">" +
          "<head>" +
          "<meta http-equiv=\"Content-Type\" content=\"text/html; charset=utf-8\" />" +
          "<meta name=\"CRAWLER.fullTextLink\" content=\"http://www.asmscience.org/content/journal/microbiolspec/10.1128/microbiolspec.MGM2-0012-2013?crawler=true\"/>" +
          "<meta name=\"citation_pdf_url\" content=\"http://www.asmscience.org/content/journal/microbiolspec/10.1128/microbiolspec.MGM2-0012-2013?crawler=true&mimetype=application/pdf\" />" +
          "<title>ASMscience | Metallobiology of Tuberculosis</title>" +
          "</head>" +
          "<body id=\"microbiologyspectrumarticle\">" +
          "<a href=\"/content/searchbrowse\" title=\"Search and Browse\" ><span id=\"searchandBrowse\">" +
          "Search/Browse" +
          "<img src=\"/images/asm/white-triangle03.png\" alt=\"mega drop down\" /></span></a>" +
          "<div class=\"clear singleFigureContainer\">" +
          "<div class=\"hiddenjsdiv mediametadata\">" +
          "<div class=\"metadata_webId\">/content/microbiolspec/10.1128/microbiolspec.MGM2-0012-2013.fig1</div>" +
          "<div class=\"metadata_title\">FIGURE 1</div>" +
          "<div class=\"metadata_thumbnailImage\">microbiolspec/2/3/MGM2-0012-2013-fig1_thmb.gif</div>" +
          "<div class=\"metadata_fullSizeImage\">microbiolspec/2/3/MGM2-0012-2013-fig1.gif</div>" +
          "<div class=\"metadata_multimediaFile\"></div>" +
          "<div class=\"metadata_multimediaFormat\"></div>" +
          "</body>" +
          "</html>";


  //http://www.asmscience.org/content/journal/microbiolspec/10.1128/microbiolspec.MGM2-0012-2013
  public void testArticlePageLinks() throws Exception {
    UrlNormalizer normalizer = new AsmUrlNormalizer();
    expectedUrls = SetUtil.set(
        BASE_URL + "content/searchbrowse",
        BASE_URL + "images/asm/white-triangle03.png",
        BASE_URL + "content/journal/microbiolspec/10.1128/microbiolspec.MGM2-0012-2013?crawler=true",
        BASE_URL + "content/microbiolspec/10.1128/microbiolspec.MGM2-0012-2013.fig1",
        BASE_URL + "docserver/fulltext/microbiolspec/2/3/MGM2-0012-2013-fig1_thmb.gif",
        BASE_URL + "docserver/fulltext/microbiolspec/2/3/MGM2-0012-2013-fig1.gif"
        );      

    Set<String> result_strings = parseSingleSource(article_landing_html, BASE_URL + "content/journal/" + JID + "/10.1128/microbiolspec.MGM2-0012-2013");

    // do you have the expected number
    assertEquals(expectedUrls.size(), result_strings.size());

    if (log.isDebug3()) {
      log.debug3("expectedURLs are:");
      for (String url : expectedUrls) {
        log.debug3(url);
      }
    }

    String norm_url;
    Set<String> norm_urls = new HashSet<String>();

    // normalize each result- and as a first check, is it in the expected list 
    log.debug3("normalizedURLs are:");
    for (String url : result_strings) {
      norm_url = normalizer.normalizeUrl(url, m_mau);
      log.debug3(norm_url);
      assertTrue(expectedUrls.contains(norm_url));
      norm_urls.add(norm_url);
    }

    // did we get every one of the expected urls
    for (String url : expectedUrls) {
      assertTrue(norm_urls.contains(url));
    }
  }

  /*------------------SUPPORT FUNCTIONS --------------------- */

  // all setting of page type (full, abs) to test restrictions
  // if doi argument is null, use default; otherwise use given
  private Set<String> parseSingleSource(String source, String srcUrl)
      throws Exception {

    MockArchivalUnit m_mau = new MockArchivalUnit();
    LinkExtractor ue = new JsoupHtmlLinkExtractor();
    m_mau.setLinkExtractor("html", ue);
    MockCachedUrl mcu =
        new org.lockss.test.MockCachedUrl(srcUrl, m_mau);
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
