/*

Copyright (c) 2000-2021, Board of Trustees of Leland Stanford Jr. University
All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are met:

1. Redistributions of source code must retain the above copyright notice,
this list of conditions and the following disclaimer.

2. Redistributions in binary form must reproduce the above copyright notice,
this list of conditions and the following disclaimer in the documentation
and/or other materials provided with the distribution.

3. Neither the name of the copyright holder nor the names of its contributors
may be used to endorse or promote products derived from this software without
specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
POSSIBILITY OF SUCH DAMAGE.

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
  
  private static final String withNavArticle = "" +
      "<div id=\"col-x\">" +
      "<div class=\"article-nav sidebar-nav\">\n" + 
      "<a class=\"previous\" title=\"Previous article\" " +
      "href=\"/content/1/6/2.short\">« Previous</a>\n" + 
      "<span class=\"article-nav-sep\"> | </span>\n" + 
      "<a class=\"next\" title=\"Next article\" " +
      "href=\"/content/1/6/8.short\">Next Article »</a>\n" + 
      "<span class=\"toc-link\">\n" + 
      "</span></div></div>";
  private static final String withoutNavArticle = // no longer filter nav
      "<div id=\"col-x\">" +
      "<div class=\"article-nav sidebar-nav\">\n" + 
      "<a class=\"previous\" title=\"Previous article\" " +
      "href=\"/content/1/6/2.short\">« Previous</a>\n" + 
      "<span class=\"article-nav-sep\"> | </span>\n" + 
      "<a class=\"next\" title=\"Next article\" " +
      "href=\"/content/1/6/8.short\">Next Article »</a>\n" + 
      "<span class=\"toc-link\">\n" + 
      "</span></div></div>";
  
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
  
  private static final String withHiddenLink =
      " <div>" +
      "<p class=\"contents_label\">\n" + 
      "<a rel=\"issue-aindex\" title=\"Index by Author1\" href=\"/content/54/10.author-index\">[Author index1]</a>" + 
      "<a rel=\"issue-aindex\" title=\"Index by Author2\" href=\"/content/54/10.author-index\" style=\"display: none;\">[Author index2]</a>" +
      "<ul class=\"toc-links\">" + 
      "  <li class=\"aindex\" style=\"display: none;\"><a rel=\"issue-aindex\" title=\"Index by Author\" href=\"/content/22/Suppl_1_Pt_1.author-index\">Index By Author</a></li>\n" + 
      "  <li><a rel=\"alternate\" title=\"TOC (PDF)\" href=\"/content/22/Suppl_1_Pt_1.toc.pdf\">Table of Contents (PDF)</a></li>\n" + 
      "</ul>" + 
      "</p>" +
      "</div>";
  private static final String withoutHiddenLink = // author index link is removed
      " <div>" +
      "<p class=\"contents_label\">\n" + 
      "<a rel=\"issue-aindex\" title=\"Index by Author1\" href=\"/content/54/10.author-index\">[Author index1]</a>" + 
      "<ul class=\"toc-links\">" + 
      "  \n" + 
      "  <li><a rel=\"alternate\" title=\"TOC (PDF)\" href=\"/content/22/Suppl_1_Pt_1.toc.pdf\">Table of Contents (PDF)</a></li>\n" + 
      "</ul>" + 
      "</p>" +
      "</div>";

  
  public void testFiltering() throws Exception {
    assertFilterToSame(withHeader, withoutHeader);
    assertFilterToSame(withAds, withoutAds);
    assertFilterToSame(withRefList, withoutRefList);
    assertFilterToSame(withNavArticle, withoutNavArticle);
    assertFilterToSame(withHiddenLink, withoutHiddenLink);
    
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
