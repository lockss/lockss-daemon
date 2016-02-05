/*
 * $Id$
 */
/*

 Copyright (c) 2000-2016 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.plugin.atypon.practicalaction;

import java.util.HashSet;
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


public class TestPracticalActionHtmlLinkExtractorFactory extends LockssTestCase {
  UrlNormalizer normalizer = new BaseAtyponUrlNormalizer();

  private PracticalActionHtmlLinkExtractorFactory fact;
  private LinkExtractor m_extractor;
  private MyLinkExtractorCallback m_callback;
  static String ENC = Constants.DEFAULT_ENCODING;
  private MockArchivalUnit m_mau;
  private final String PA_BASE_URL = "http://www.developmentbookshelf.com/";



  public static final String basicLinksHtml =
      "<div>" +
          "<title>Waterlines: Vol 34, No 2</title>" +
          "<link rel=\"stylesheet\" type=\"text/css\" href=\"pa_toc_files/product.css\"></link>" +
          "<script src=\"pa_toc_files/analytics.js\" async=\"\"></script>" +
          "<span class=\"journalNavLeftTd\">" +
          " <a href=\"http://www.developmentbookshelf.com/toc/wl/34/1\">" +
          "  &lt; Previous issue" +
          " </a>" +
          "</span>" +
          "<span class=\"journalNavRightTd\">" +
          "<a href=\"http://www.developmentbookshelf.com/toc/wl/34/3\">" +
          "  Next issue &gt;" +
          "</a>" +
          "</span>" +
          "<a class=\"ref nowrap\" href=\"http://www.developmentbookshelf.com/doi/abs/10.3362/1756-3488.2015.011\"></a>" +
          "<a href=\"http://www.developmentbookshelf.com/action/showCitFormats?doi=10.3362%2F1756-3488.2015.34.issue-2\">Send to Citation Mgr</a>" +
          "</div>";

  @Override
  public void setUp() throws Exception {
    super.setUp();
    m_mau = new MockArchivalUnit();
    m_callback = new MyLinkExtractorCallback();
    fact = new PracticalActionHtmlLinkExtractorFactory();
    m_extractor = fact.createLinkExtractor("html");

  }
  Set<String> expectedUrls;

  public void testBasicLinks() throws Exception {
    expectedUrls = SetUtil.set(
        //relative support
        PA_BASE_URL + "doi/abs/10.1111/pa_toc_files/product.css",
        PA_BASE_URL + "doi/abs/10.1111/pa_toc_files/analytics.js",
        //absolute prev-next
        PA_BASE_URL + "toc/wl/34/1",
        PA_BASE_URL + "toc/wl/34/3",
        //absolute
        PA_BASE_URL + "doi/abs/10.3362/1756-3488.2015.011",
        // this one only when on an article page
        PA_BASE_URL + "action/showCitFormats?doi=10.3362%2F1756-3488.2015.34.issue-2"
        );
    // first see what we get from a regular article page
    String articleUrl = PA_BASE_URL + "doi/abs/10.1111/12345";
    log.debug3("now starting article page test");
    Set<String> result_strings = parseSingleSource(articleUrl, basicLinksHtml);
    assertEquals(6, result_strings.size());

    // loop over the expected to make sure every one is in the results
    for (String url : expectedUrls) {
      log.debug3("expected URL: " + url);
      assertTrue(result_strings.contains(url));
    }

    log.debug3("now starting toc test");
    // now make sure we don't pick up the showCitations from a toc
    String tocUrl = PA_BASE_URL + "toc/wl/34/2";
    expectedUrls = SetUtil.set(
        //relative support
        PA_BASE_URL + "toc/wl/34/pa_toc_files/product.css",
        PA_BASE_URL + "toc/wl/34/pa_toc_files/analytics.js",
        //absolute prev-next
        PA_BASE_URL + "toc/wl/34/1",
        PA_BASE_URL + "toc/wl/34/3",
        //absolute
        PA_BASE_URL + "doi/abs/10.3362/1756-3488.2015.011"
        );    
    result_strings = parseSingleSource(tocUrl, basicLinksHtml);
    assertEquals(5, result_strings.size());

    //loop over the results and make sure they are in the expected
    //and then make sure they don't equal the one we wanted suppressed
    // loop over the expected to make sure every one is in the results
    assertFalse(result_strings.contains(PA_BASE_URL + "action/showCitFormats?doi=10.3362%2F1756-3488.2015.34.issue-2"));
    for (String url : expectedUrls) {
      log.debug3("expected URL: " + url);
      assertTrue(result_strings.contains(url));
    }    
  }


  private Set<String> parseSingleSource(String url, String source)
      throws Exception {
    MockArchivalUnit m_mau = new MockArchivalUnit();
    LinkExtractor ue = new RegexpCssLinkExtractor();
    m_mau.setLinkExtractor("text/css", ue);
    MockCachedUrl mcu =
        new org.lockss.test.MockCachedUrl(url, m_mau);
    mcu.setContent(source);

    m_callback.reset();
    m_extractor.extractUrls(m_mau,
        new org.lockss.test.StringInputStream(source), ENC,
        url, m_callback);
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
