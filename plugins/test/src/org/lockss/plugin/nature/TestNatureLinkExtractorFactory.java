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

package org.lockss.plugin.nature;

import java.util.HashSet;
import java.util.Set;
import org.lockss.plugin.UrlNormalizer;
import org.lockss.extractor.JsoupHtmlLinkExtractor;
import org.lockss.extractor.LinkExtractor;
import org.lockss.extractor.LinkExtractorFactory;
import org.lockss.test.LockssTestCase;
import org.lockss.test.MockArchivalUnit;
import org.lockss.test.MockCachedUrl;
import org.lockss.util.Constants;
import org.lockss.util.SetUtil;


/*
 *  Nature doesn't have its own link extractor but it now specified the alternate
 *  base link extractor. That is, instead of the default
 *  GoslingHtmlLinkExtractor, it uses the
 *  JsoupHtmlLinkExtractor
 *  This was because we found one AU with titles that had unescaped ">" in the link title arg value
 *  and gosling drops everything else in the link whereas jsoup handles it just fine.
 *  The toc with the issue is:
 *   http://www.nature.com/bjc/journal/v107/n11/index.html
 *   and the title is
 *   "Association of transcobalamin c. 776C>G with overall survival in patients with primary central nervous system lymphoma"
 *   and the link is:
 *   <a title="Abstract of ...Assoc 776C>G..." href=\"/bjc/journal/v107/n11/abs/bjc2012476a.html">Abstract</a>
 */

public class TestNatureLinkExtractorFactory extends LockssTestCase {

  private LinkExtractorFactory fact;
  private LinkExtractor m_extractor;
  private MyLinkExtractorCallback m_callback;
  static String ENC = Constants.DEFAULT_ENCODING;
  private MockArchivalUnit m_mau;
  private final String BASE_URL = "http://test.org/";
  private static final String JID = "foo";
  private static final String VOL = "123";
  private static final String ISSUE = "4";
  private static final String DOI_START = "11.1111";
  private static final String DOI_END = "TEST";


  /* expected:
   *"http://test.org/foo/journal/v123/n4/full/relative/test/link.jpg",
   * "http://www.foo.com/absolute/link.jpg"
   */
  private static final String htmlTest = 
         "<div>" +
             "<a href=\"relative/test/link.jpg\">RELATIVE</a>" + 
             "<a href=\"http://www.foo.com/absolute/link.jpg\">ABSOLUTE</a>" +
          "</div>";

  /* expected:
   *"http://test.org/foo/journal/v123/n4/relative/test/link.jpg
   *"http://test.org/foo/journal/v123/n4/bjc/journal/v107/n11/abs/bjc2012476a.html"
   *"http://test.org/foo/journal/v123/n4/bjc/journal/v107/n11/full/bjc2012476a.html"
   *"http://test.org/foo/journal/v123/n4/bjc/journal/v107/n11/pdf/bjc2012476a.pdf
   * 
   */
  private static final String otherTest = 
      "<div>" +
          "<a href=\"relative/test/link.jpg\">" + 
          "<h4 class=\"atl\" id=\"abjc2012476\">" +
          "Assoc 776C&gt;G<span class=\"free i b\">" +
          "&nbsp;Open</span>" +
          "</h4> " +
          "<p class=\"links\">" +
          /*
          "<a title=\"Abstract of Assoc 776C>G\" href=\"/bjc/journal/v107/n11/abs/bjc2012476a.html\">Abstract</a>" +
          " | <a title=\"Full text of Assoc 776C>G\" href=\"/bjc/journal/v107/n11/full/bjc2012476a.html\">Full Text</a>" +
          " | <a title=\"PDF of Assoc 776C>G\" href=\"/bjc/journal/v107/n11/pdf/bjc2012476a.pdf\">PDF</a>" +
          */
          "<a title=\"Abstract of Assoc 776C>G\" href=\"/bjc/journal/v107/n11/abs/bjc2012476a.html\">Abstract</a>" +
          " | <a title=\"Full text of Assoc 776C>G\" href=\"/bjc/journal/v107/n11/full/bjc2012476a.html\">Full Text</a>" +
          " | <a title=\"PDF of Assoc 776C>G\" href=\"/bjc/journal/v107/n11/pdf/bjc2012476a.pdf\">PDF</a>" +
          "</p>" +
          "</div>";

  
  @Override
  public void setUp() throws Exception {
    log.setLevel("debug3");
    super.setUp();
    m_mau = new MockArchivalUnit();
    m_callback = new MyLinkExtractorCallback();
    
    //fact = new GoslingHtmlLinkExtractor.Factory();
    fact = new JsoupHtmlLinkExtractor.Factory();
    m_extractor = fact.createLinkExtractor("html");

  }
  
  Set<String> expectedUrls;
  

  public void testLinks() throws Exception {
    UrlNormalizer normalizer = new NaturePublishingGroupUrlNormalizer();
    expectedUrls = SetUtil.set(
        "http://test.org/foo/journal/v123/n4/relative/test/link.jpg",
        "http://test.org/bjc/journal/v107/n11/abs/bjc2012476a.html",
        "http://test.org/bjc/journal/v107/n11/full/bjc2012476a.html",
        "http://test.org/bjc/journal/v107/n11/pdf/bjc2012476a.pdf"
        );      

    Set<String> result_strings = parseSingleSource(otherTest, BASE_URL + JID + "/journal/v123/n4/index.html");

    // do you have the expected number
    assertEquals(expectedUrls.size(), result_strings.size());

    String norm_url;
    Set<String> norm_urls = new HashSet<String>();

    // normalize each result- and as a first check, is it in the expected list 
    for (String url : result_strings) {
      norm_url = normalizer.normalizeUrl(url, m_mau);
      //log.debug3("normalized URL: " + norm_url);
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
