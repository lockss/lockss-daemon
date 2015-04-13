/*
/    * $Id: TestHighWirePressH20HtmlFilterFactory.java 39864 2015-02-18 09:10:24Z thib_gc $
 */

/*

 Copyright (c) 2000-2015 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.plugin.highwire;

import java.io.*;

import org.lockss.util.*;
import org.lockss.test.*;

public class TestHighWirePressH20HtmlCrawlFilterFactory extends LockssTestCase {
  static String ENC = Constants.DEFAULT_ENCODING;

  private HighWirePressH20HtmlCrawlFilterFactory fact;
  private MockArchivalUnit mau;

  public void setUp() throws Exception {
    super.setUp();
    fact = new HighWirePressH20HtmlCrawlFilterFactory();
    mau = new MockArchivalUnit();
  }
  
  
  private static final String withHeader = "<body>\n"
      + "<header class=\"header-box\">"
      + "<div class=\"cb-contents\">xx</div>"
      + "</header>A"
      + "<footer class=\"footer-box\">"
      + "<ol>"
      + "<li><a href=\"/cgi/alerts/etoc\">Alert me to new issues of The Journal"
      + "</a></li>" + "</ol>"
      + "</footer>" + " </body>";
  private static final String withoutHeader = "<body>\nA </body>";
  
  private static final String withAds = "<div class=\"block-1\">"
      + "<div class=\"leaderboard-ads-ft\">"
      + "<ul>"
      + "<li><a href=\"com%2FAbout.html\"><img title=\"Advertiser\""
      + "src=\"http:/adview=true\""
      + "alt=\"Advertiser\" /></a></li>"
      + "</ul>"
      + "<p class=\"disclaimer\">The content of this site is intended for health care professionals</p>"
      + "<p class=\"copyright\">Copyright © 2012 by "
      + "The Journal of Rheumatology" + "</p>" + "<ul class=\"issns\">"
      + "<li><span>Print ISSN: </span>"
      + "<span class=\"issn\">0315-162X</span></li>"
      + "<li><span>Online ISSN: </span>"
      + "<span class=\"issn\">1499-2752</span></li>" + "</ul>" + "</div>"
      + "</div>\"";
  private static final String withoutAds = "<div class=\"block-1\"></div>\"";
  
  private static final String withRefList = "<div>B" +
      "<div id=\"ref-list-1\" class=\"section ref-list\">\n" + 
      "<div class=\"section-nav\"><a class=\"prev-section-link\" title=\"Footnotes\" href=\"#fn-group-1\"><span>Previous Section</span></a><a class=\"next-section-link\" title=\"Secondary sources\" href=\"#ref-list-2\"><span>Next Section</span></a></div>\n" + 
      "<h2>Bibliography<br>Primary sources\n" + 
      "</h2>\n" + 
      "<ol class=\"cit-list ref-use-labels\">\n" + 
      "<li><span class=\"ref-label ref-label-empty\">" +
      "</span><div id=\"cit-23.1.1.10\" class=\"cit ref-cit ref-journal no-rev-xref\">\n" + 
      "  <div class=\"cit-extra\"></div>\n" + 
      "</li>\n" + 
      "</ol>\n" + 
      "</div>" +
      "</div>";
  private static final String withoutRefList = "<div>B</div>";
  
  private static final String withNavArticle =
      "<div id=\"col-x\">" +
          "<div class=\"article-nav sidebar-nav\">\n" + 
          "<a class=\"previous\" title=\"Previous article\" " +
          "href=\"/content/1/6/2.short\">« Previous</a>\n" + 
          "<span class=\"article-nav-sep\"> | </span>\n" + 
          "<a class=\"next\" title=\"Next article\" " +
          "href=\"/content/1/6/8.short\">Next Article »</a>\n" + 
          "<span class=\"toc-link\">\n" + 
          "</span></div></div>";
  private static final String withoutNavArticle = // div attributes are removed
      "<div id=\"col-x\"></div>";
  
  private static final String col3Html =
      " <div id=\"generic\" class=\"hw-gen-page pagetype-content\">" +
      "<div id=\"col-3\" style=\"height: 1616px;\">" +
      "<div id=\"sidebar-current-issue\" class=\"content-box\">" +
      "<div class=\"cb-contents\"></div></div><div id=\"sidebar-global-nav\">" +
      "</div><div class=\"most-links-box \"></div>" +
      "<ul class=\"tower-ads\"><li class=\"no-ad tower\"><span>  </span></li></ul>" +
      "</div></div>";
  private static final String col3Filtered = // div attributes are removed
      " <div id=\"generic\" class=\"hw-gen-page pagetype-content\">" +
      "</div>";

  
  public void testFiltering() throws Exception {
    assertFilterToSame(withHeader, withoutHeader);
    assertFilterToSame(withAds, withoutAds);
    assertFilterToSame(withRefList, withoutRefList);
    assertFilterToSame(withNavArticle, withoutNavArticle);
    
    assertFilterToString(col3Html, col3Filtered);
  }

  private void assertFilterToSame(String str1, String str2) throws Exception {

    InputStream inA = fact.createFilteredInputStream(mau, new StringInputStream(str1),
        Constants.DEFAULT_ENCODING);
    InputStream inB = fact.createFilteredInputStream(mau, new StringInputStream(str2),
        Constants.DEFAULT_ENCODING);
    String actual = StringUtil.fromInputStream(inA);
    String expected = StringUtil.fromInputStream(inB);
    assertEquals(expected, actual);
//    assertEquals(StringUtil.fromInputStream(inB),
//        StringUtil.fromInputStream(inA));
  }

//Don't put the 2nd string through the filter - use it as a constant
  private void assertFilterToString(String orgString, String finalString) throws Exception {

    InputStream inA = fact.createFilteredInputStream(mau, new StringInputStream(orgString),
        Constants.DEFAULT_ENCODING);

    assertEquals(finalString,StringUtil.fromInputStream(inA));
  }
  
}
