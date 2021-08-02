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

public class TestHighWirePressH20HtmlFilterFactory extends LockssTestCase {
  static String ENC = Constants.DEFAULT_ENCODING;

  private HighWirePressH20HtmlFilterFactory fact;
  private MockArchivalUnit mau;

  public void setUp() throws Exception {
    super.setUp();
    fact = new HighWirePressH20HtmlFilterFactory();
    mau = new MockArchivalUnit();
  }

  private static final String inst1 = "<div class=\"leaderboard-ads leaderboard-ads-two\"</div>"
      + "<ul>Fill in SOMETHING SOMETHING</ul>";

  private static final String inst2 = "<ul>Fill in SOMETHING SOMETHING</ul>";

  private static final String withAds = "<div id=\"footer\">"
      + "<div class=\"block-1\">"
      + "<div class=\"leaderboard-ads-ft\">"
      + "<ul>"
      + "<li><a href=\"com%2FAbout.html\"><img title=\"Advertiser\""
      + "src=\"http:/adview=true\""
      + "alt=\"Advertiser\" /></a></li>"
      + "</ul>"
      + "</div>"
      + "<p class=\"disclaimer\">The content of this site is intended for health care professionals</p>"
      + "<p class=\"copyright\">Copyright © 2012 by "
      + "The Journal of Rheumatology" + "</p>" + "<ul class=\"issns\">"
      + "<li><span>Print ISSN: </span>"
      + "<span class=\"issn\">0315-162X</span></li>"
      + "<li><span>Online ISSN: </span>"
      + "<span class=\"issn\">1499-2752</span></li>" + "</ul>" + "</div>"
      + "<div class=\"block-2 sb-div\"></div>" + "</div>\"";

  private static final String withoutAds = "\""; // div id=footer is now filtered

  private static final String withCol4SquareAds = "<div id=\"footer\">"
      + "<div class=\"block-1\">"
      + "<ul class=\"col4-square\">"
      + "<li><a href=\"/cgi/adclick/?ad=35597&amp;adclick=true&amp;"
      + "url=http%3A%2F%2Fwww.facebook.com%2FPlantphysiology\">"
      + "<img class=\"adborder0\" title=\"PlantPhysFacebook\" width=\"160\" "
      + "height=\"150\" src=\"http://www.plantphysiol.org/adsystem/graphics/"
      + "5602385865303331/plantphysiol/squarepp.jpg?ad=35597&amp;adview=true\" "
      + "alt=\"PlantPhysFacebook\" /></a></li>"
      + "</ul>"
      + "<div class=\"block-2 sb-div\"></div>" + "</div>";

  private static final String withoutCol4SquareAds = ""; // div id=footer is now filtered

  private static final String withCol4TowerAds = "<div id=\"footer\">"
      + "<div class=\"block-1\">"
      + "<ul class=\"col4-tower\">"
      + "<li><a href=\"/cgi/adclick/?ad=35598&amp;adclick=true&amp;url=http%3A%2F%2Fwww.plantphysiol.org%2F\">"
      + "<img class=\"adborder10\" title=\"10pdfPromo\" width=\"160\" height=\"600\" " +
      "src=\"http://www.plantphysiol.org/adsystem/graphics/06456092319841111/plantphysiol/" +
      "vertauthors.jpg?ad=35598&amp;adview=true alt=\"10pdfPromo\" /></a></li>"
      + "</ul>"
      + "<div class=\"block-2 sb-div\"></div>" + "</div>";

  private static final String withoutCol4TowerAds = ""; // div id=footer is now filtered

  private static final String withSageAnchors = "<div id=\"test\">" +
      "<a href=\"/cgi/openurl?query=rft.jtitle%3DPlast+Reconstr\">" +
      "<a href=\"/openurl?query=rft.jtitle%3DPlast+Reconstr\">" +
      "<a href=\"/external-ref?access_num=19469&displayid=767\">Order article</a>" +
      "</div>\"";

  private static final String withoutSageAnchors = "<div>" + // div attributes are removed
      "</div>\"";

  private static final String withCopyright = "<div id=\"footer\">"
      + "<div class=\"block-1\">"
      + "<p class=\"disclaimer\">The content of this site is intended for health care professionals</p>"
      + "<p class=\"copyright\">Copyright © 2012 by "
      + "The Journal of Rheumatology" + "</p>" + "<ul class=\"issns\">"
      + "<li><span>Print ISSN: </span>"
      + "<span class=\"issn\">0315-162X</span></li>"
      + "<li><span>Online ISSN: </span>"
      + "<span class=\"issn\">1499-2752</span></li>" + "</ul>" + "</div>"
      + "<div class=\"block-2 sb-div\"></div>" + "</div>\"";

  private static final String withoutCopyright = "\""; // div id=footer is now filtered

  private static final String withCurrentIssue = "<div class=\"col-3-top sb-div\"></div>"
      + "<div class=\"content-box\" id=\"sidebar-current-issue\">"
      + "<div class=\"cb-contents\">"
      + "<h3 class=\"cb-contents-header\"><span>Current Issue</span></h3>"
      + "<div class=\"cb-section\">"
      + "<ol>"
      + "<li><span><a href=\"/content/current\" rel=\"current-issue\">May 2012, 39 (5)</a></span></li>"
      + "</ol>"
      + "</div>"
      + "<div class=\"cb-section\">"
      + "<ol>"
      + "<div class=\"current-issue\"><a href=\"/content/current\" "
      + "rel=\"current-issue\"><img src=\"/local/img/sample_cover.gif\" "
      + "width=\"67\" height=\"89\" alt=\"Current Issue\" /></a></div>"
      + "</ol>"
      + "</div>"
      + "<div class=\"cb-section sidebar-etoc-link\">"
      + "<ol>"
      + "<li><a href=\"/cgi/alerts/etoc\">Alert me to new issues of The Journal"
      + "</a></li>" + "</ol>" + "</div>" + "</div>" + "</div>";
  private static final String withoutCurrentIssue = "<div></div>"; // div attributes are removed

  private static final String headHtml = "<html><head>Title</head></HTML>";
  private static final String headHtmlFiltered = ""; // <html></HTML>";

  private static final String headHtmlWithComment = "<html><head>Title</head><!-- COMMENT HERE --></HTML>";

  private static final String withSporadicDivs =
      "<div><div id=\"fragment-reference-display\"></div>" +
          "<div class=\"cit-extra\">stuff</div></div";
  private static final String withoutSporadicDivs =
      "<div></div>";

  private static final String withCmeCredit =
      "<ol><li><a href=\"/content/28/7/911/suppl/DC1\" rel=\"supplemental-data\"" +
          "class=\"dslink-earn-free-cme-credit\">Earn FREE CME Credit</a></li></ol>";
  private static final String withoutCmeCredit = 
      "<ol></ol>";

  private static final String withCbSection =
      "<div><div class=\"cb-section collapsible default-closed\" id=\"cb-art-gs\">Content" +
          "<h4></h4><ol><li></li></ol></div></div>";
  private static final String withoutCbSection =
      "<div></div>";

  private static final String withHwGenPage =
      "<div class=\"hw-gen-page pagetype-content hw-pub-id-article\" " +
          "id=\"pageid-content\" itemscope=\"itemscope\" " +
          "itemtype=\"http://schema.org/ScholarlyArticle\">content</div>";
  private static final String withoutHwGenPage = // div attributes are removed
      "<div>content</div>";

  private static final String withNavCurrentIssue =
      "<li id=\"nav_current_issue\" title=\"Current\">" +
          "<a href=\"/content/current\">" +
          "<span>View Current Issue (Volume 175 Issue 12 June 15, 2012)" +
          "</span></a></li>";

  private static final String withoutNavCurrentIssue = ""; 

  private static final String withNavArticle =
      "<div id=\"col-x\">" +
          "<div class=\"article-nav\">\nfoo</div>" + 
          "<div class=\"article-nav sidebar-nav\">\n" + 
          "<a class=\"previous\" title=\"Previous article\" " +
          "href=\"/content/1/6/2.short\">« Previous</a>\n" + 
          "<span class=\"article-nav-sep\"> | </span>\n" + 
          "<a class=\"next\" title=\"Next article\" " +
          "href=\"/content/1/6/8.short\">Next Article »</a>\n" + 
          "<span class=\"toc-link\">\n" + 
          "</div></div>";

  private static final String withoutNavArticle = // div attributes are removed
      "<div></div>";

  private static final String withRelatedURLs =
      "<div>  <span id=\"related-urls\"" +
          "/span></div>";
  private static final String withoutRelatedURLs =
      "<div></div>";

  // occmed.oxfordjournals - impact factor is nested as text with no id tag - use smart combination filtering
  private static final String textIndexFactor =
      "<div id=\"second\">" +
          "<h2 id=\"read_this_journal\" title=\"Read This Journal\"><span>Read This Journal</span></h2>" +
          "<div class=\"features\">" +
          "<div class=\"feature separator_after first\">" +
          "<h2 id=\"the_journal\" title=\"The Journal\"><span>The Journal</span></h2>" +
          "<ul class=\"emphasised\">" +
          "<li><a>About this journal</a></li>" +
          "</ul>" +
          "<p><a>Occupational Medicine Podcasts</a></p>" +
          "</div>" +
          "<div class=\"feature separator_after\">" +
          "<h3>Impact factor:  1.136</h3>" +
          "</div>" +
          "<div class=\"feature\">" +
          "<h3>Honorary Editor</h3>" +
          "</div>" +
          "</div>" +
          "<div class=\"features\">" +
          "<h2 id=\"KEEP THIS\"><span>KEEP THIS BIT</span></h2>" +
          "</div>" +
          "</div>";
  private static final String textIndexFactorFiltered = // div attributes are removed
//      "<div>" +
//      "<h2 id=\"read_this_journal\" title=\"Read This Journal\"><span>" +
      "ReadThisJournal" +
//      "</span></h2>" +
//      "<div>" +
//      "<h2 id=\"KEEP THIS\"><span>" +
      "KEEPTHISBIT" +
//      "</span></h2>" +
//      "</div>" +
//      "</div>" +
      "";

  private static final String hiddenInputHtml = 
      "<form action=\"http://www.example.org/search\" class=\"searchbox\" method=\"get\">" +
          "<input value=\"\" type=\"text\" name=\"fulltext\" id=\"header-qs-input\" " +
          "maxlength=\"80\" width=\"60\" class=\"field\" />" +
          " <input type=\"hidden\" name=\"submit\" value=\"yes\" /><input type=\"image\"" +
          " value=\"GO\" alt=\"Link: Go\" id=\"header-qs-search-go\"" +
          "src=\"/publisher/img/go.gif\" />" +
          "<input type=\"hidden\" name=\"qs\" value=\"yes\" /><p>                        " +
          "<input type=\"hidden\" name=\"domain\" value=\"highwire\" /></p>" +
          "<input type=\"hidden\" name=\"group-code\" value=\"gsw\" /><input type=\"hidden\"" +
          " name=\"resourcetype\" value=\"HWCIT\" /></form>";
  private static final String hiddenInputFiltered =
//      "<form action=\"https://www.example.org/search\" class=\"searchbox\" method=\"get\">" +
//          "<input value=\"\" type=\"text\" name=\"fulltext\" id=\"header-qs-input\" maxlength=\"80\" " +
//          "width=\"60\" class=\"field\" />" +
//          " <input type=\"image\" value=\"GO\" alt=\"Link: Go\" id=\"header-qs-search-go\"" +
//          "src=\"/publisher/img/go.gif\" />" +
//          "<p>" +
          "" +
//          "</p>" +
//          "</form>" +
          "";

  private static final String accessCheckHtml =
      "     <div class=\"cb-section cb-views\">" +
          "<ol>" +
          "<li class=\"abstract-view-link primary\"><span class=\"viewspecificaccesscheck" +
          " gsclaymin;47/1/1 abstract\"></span></li>" +
          "<li><a href=\"/content/47/1/1.figures-only\" rel=\"view-figures-only\">Figures Only" +
          "</a><span class=\"viewspecificaccesscheck gsclaymin;47/1/1 figsonly\"></span></li>" +
          "<li class=\"notice full-text-view-link primary\"><a href=\"/content/47/1/1.full\"" +
          " rel=\"view-full-text\">Full Text</a><span class=\"viewspecificaccesscheck " +
          "gsclaymin;47/1/1 full\"></span></li>" +
          "<li class=\"notice full-text-pdf-view-link primary\"><a href=\"/content/47/1/1.full.pdf+html\" " +
          "rel=\"view-full-text.pdf\">" +
          "Full Text (PDF)</a><span class=\"viewspecificaccesscheck gsclaymin;47/1/1 reprint\"></span></li>" +
          "</ol>" +
          "</div>";

  private static final String accessCheckFiltered=
      "" +
//          "<div>" +
//          "<ol>" +
//          "<li></li>" +
//          "<li><a href=\"/content/47/1/1.figures-only\" rel=\"view-figures-only\">" +
          "FiguresOnly" +
//          "</a></li>" +
//          "<li><a href=\"/content/47/1/1.full\"" +
//          " rel=\"view-full-text\">" +
          "FullText" +
//          "</a></li>" +
//          "<li><a href=\"/content/47/1/1." +
//          "full.pdf+html\" rel=\"view-full-text.pdf\">" +
          "FullText(PDF)" +
//          "</a></li>" +
//          "</ol>" +
//          "</div>" +
          "";

  private static final String institutionLogoHtml =
      " <a class=\"hwac-institutional-logo\">" +
          "<img alt=\"Stanford University\"" +
          "src=\"/userimage/891eef32-7e9f-4198-9886-31192686655e-20120118\"" +
          "class=\"hwac-institutional-logo\" />" +
          "</a>" +
          "<div id=\"something-nav\">";

  private static final String institutionLogoFiltered = // div attributes are removed
      "";

  private static final String sidebarGlobalNavHtml =
      " <div id=\"sidebar-global-nav\">" +
          "<ul class=\"button-list pub-links\"> " +
          "<li class=\"first\"><a title=\"About the Journal\" href=\"http://" +
          "www.minersoc.org/clayminm.html\"><span>About the Journal</span></a></li>" +
          "</ul>" +
          "</div>" +
          "<div id=\"something-nav\">";

  private static final String sidebarGlobalNavFiltered = // div attributes are removed
      "";

  private static final String col3Html =
      " <div id=\"generic\" class=\"hw-gen-page pagetype-content\">" +
          "<div id=\"col-3\" style=\"height: 1616px;\">" +
          "<div id=\"sidebar-current-issue\" class=\"content-box\">" +
          "<div class=\"cb-contents\"></div></div><div id=\"sidebar-global-nav\">" +
          "</div><div class=\"most-links-box \"></div>" +
          "<ul class=\"tower-ads\"><li class=\"no-ad tower\"><span>  </span></li></ul>" +
          "</div><script type=\"text/javascript\"></script></div>";

  private static final String col3Filtered = // div attributes are removed
      "";

  private static final String tocBannerAdHtml =
      "   <div id=\"content-block\">" +
          "<ul class=\"toc-banner-ads\">" +
          " <li><a href=\"/cgi/adclick/?ad=35482&amp;adclick=true&amp;url=http%3A%2F%2F" +
          "www.aspetjournals.org%2Fsite%2Fmisc%2Fmobile_announce.xhtml\"><img class=\"adborder1\"" +
          " title=\"ASPET Journals Now Available for Mobile Devices\"" +
          "    width=\"195\"" +
          "    height=\"195\"" +
          "    src=\"http://pharmrev.aspetjournals.org/adsystem/graphics/932368436242021/" +
          "pharmrev/Mobile%20Versions%20195x195%20Banner%20Ad.gif?ad=35482&amp;adview=true\"" +
          "    alt=\"ASPET Journals Now Available for Mobile Devices\" /></a></li>" +
          "</ul> " +
          " <div id=\"toc-header\">   " +
          " <h1>Table of Contents</h1><cite>";
  private static final String tocBannerAdFiltered = // div attributes are removed and all tags
//      " <div>" +
//      " <div>" +
//      " <h1>" +
      "TableofContents" +
//      "</h1><cite>" +
      "";

  private static final String viewingDate =
      "<ul class=\"button-list header-buttons\">" +
          " <li id=\"na_home\" class=\"first\"><a href=\"/\" title=\"Home\"><span>Home</span></a></li>" +
          " <li id=\"na_currentvol\"><a href=\"/content/current/\" title=\"Current Volume\">" +
          "<span>Current Volume</span></a></li>" +
          "</ul>" +
          " <div class=\"site-date\">April 22, 2013</div>" +
          " </div>";

  private static final String viewingDateFiltered =
      "<ul class=\"button-list header-buttons\">" +
          " <li><a href=\"/\" title=\"Home\"><span>Home</span></a></li>" +
          " <li><a href=\"/content/current/\" title=\"Current Volume\">" +
          "<span>Current Volume</span></a></li>" +
          "</ul>" +
          " " +
          " </div>";

  private static final String gswHeader =
      "<div id=\"header\">\n" + 
          "<div id=\"gsw-top-container\">\n" + 
          "<div id=\"gsw-head\">\n" + 
          "<div id=\"gsw-logo-and-buttons\">\n" + 
          "<a href=\"http://www.geoscienceworld.org\" id=\"gsw-logo\"><span>GeoScienceWorld</span></a>\n" + 
          "<div id=\"gsw-quick-search\">\n" + 
          "<h3>Quick search</h3>            \n" + 
          "</div>\n" + 
          "</div>\n" + 
          "<div class=\"inst-branding\"></div>\n" + 
          "</div>\n" + 
          "</div>\n" + 
          "<h1><a id=\"logo\" href=\"/\"><span>Rocky Geology</span></a></h1>\n" + 
          "</div>";

  private static final String gswHeaderFiltered = ""; // div id=header is now filtered

  private static final String relatedHtml =
      "<div id=\"header\">\n" +
          "<div class=\"relmgr-relation related\" id=\"rel-related-article\">\n" + 
          "<h2>Related Articles</h2>\n" + 
          "<ul class=\"related-list\">\n" + 
          "<li class=\"cit\">cite\n" + 
          "</li>\n" + 
          "</ul>\n" + 
          "</div>\n" + 
          "</div>";

  private static final String relatedHtmlFiltered = ""; // div id=header is now filtered

  private static final String citedHtml =
      "<div id=\"header\">\n" +
          "<div id=\"cited-by\" xmlns=\"http://www.w3.org/1999/xhtml\">\n" + 
          "<h2>Articles citing this article</h2>\n" + 
          "<ul class=\"cited-by-list\">\n" + 
          "<li class=\"cit\">Cite Initiation</li>\n" + 
          "</ul>\n" + 
          "</div>" +
          "</div>";

  private static final String citedHtmlFiltered = ""; // div id=header is now filtered

  private static final String socialHtml =
      "<div id=\"header\">\n" +
          "<div class=\"social-bookmarking\">\n" + 
          "<ul class=\"social-bookmark-links\">\n" + 
          "</ul>\n" + 
          "<p class=\"social-bookmarking-help\"><a href=\"/help.dtl\">What's this?</a></p>\n" + 
          "</div>" +
          "</div>";

  private static final String socialHtmlFiltered = ""; // div id=header is now filtered

  private static final String adfootHtml =
      "<body>\n" +
          "<div class=\"ad_unhidden\" id=\"oas_top\">\n" + 
          "Stuff\n" + 
          "</div>\n" + 
          "\n" + 
          "<div class=\"ad_hidden\" id=\"oas_bottom\">\n" + 
          "Stuff\n" + 
          "</div>\n" + 
          "\n" + 
          "<widget-container>\n" + 
          "<div id=\"disclaimer\">\n" + 
          "<p>Disclaimer: </p>\n" + 
          "</div>\n" + 
          "</widget-container>\n" + 
          "\n" + 
          "<div id=\"secondary_footer\">\n" + 
          "<div id=\"issn\">Online ISSN XXXX-2092 - Print ISSN XXXX-5129</div>\n" + 
          "</div>\n" + 
          "\n" + 
          "<div id=\"primary_footer\">\n" + 
          "<div id=\"site_logo\">\n" + 
          "</div>\n" + 
          "<div id=\"third_nav\">\n" + 
          "</div>\n" + 
          "</div>\n" + 
          "\n" + 
          "</body>";

  private static final String adfootHtmlFiltered =
      "<body>\n" +
          "<widget-container></widget-container>\n" +
          "</body>";

  private static final String europaceHtml =
      "<body>\n" +
          "<div id=\"secondary_nav\"> <strong title=\"Oxford Journals\" id=\"page_logo\">" +
          "<a href=\"http://www.oxfordjournals.org/\"><span>Oxford Journals</span></a></strong> " +
          "<ul> <li title=\"My Account\" id=\"nav_my_account\">" +
          "<a href=\"http://services.oxfordjournals.org/cgi/tslogin?url=http://www.ox...\">" +
          "<span>My Account</span></a></li> </ul> " +
          "</div>" +
          "<div id=\"primary_nav\"> " +
          "<ul> <li title=\"About This Journal\" id=\"nav_about_this_journal\"> " +
          "<a href=\"http://www.oxfordjournals.org/europace/about.html\"> " +
          "<span>About This Journal</span> </a> </li> </ul> " +
          "</div>" +
          "<div id=\"cb-art-cat\" class=\"cb-section collapsible\">\n" + 
          "<h4 class=\"cb-section-header\">\n" + 
          "<span>Classifications</span>\n" + 
          "</h4>\n" +
          "</div>" +
          "<div id=\"related\"> <h2>Related articles</h2> <ul class=\"related-list\">" +
          " <div class=\"cit-metadata\"><span class=\"cit-first-element cit-section\">Editorial" +
          "<span class=\"cit-sep cit-sep-after-article-section\">:</span> </span>" +
          "</div> </ul> </div>" + 
          "<div class=\"cb-section collapsible\" id=\"cb-art-stats\">\n" + 
          "<h4 class=\"cb-section-header\"><span>Article Usage Stats</span></h4>\n" + 
          "<ol><li class=\"usage-stats-link icon-link\"></li></ol>\n" + 
          "</div>" + 
          "<ul id=\"site-breadcrumbs\">\n" + 
          "<li class=\"first\">\n" + 
          "<a href=\"http://services.oxfordjournals.org/cgi/tslogin?url=\">Oxford Journals</a>\n</li> " +
          "</ul> " +
          "" +
          "<ul class=\"kwd-group \"> <li class=\"kwd\"><span>Lead extraction</span></li> </ul>" +
          "" +
          "<ul class=\"copyright-statement\"> <li id=\"copyright-statement-1\"" +
          " class=\"fn\">For permissions please email: </li> </ul>" +
          "<span class=\"ccv cc-version-by-nc/2.0\"></span>\n" + 
          "</body>";

  private static final String europaceHtmlFiltered =
      "<body>\n" +
          "</body>";

  private static final String col2Html =
      "<body>\n" +
          "<div id=\"header\">\n" + 
          "  <h1 title=\"EP Europace\" id=\"page_title\"><a href=\"/\"><span>EP Europace</span></a></h1>\n" + 
          "</div>\n" + 
          "<div id=\"footer\">\n" + 
          "  <h4>Site Map</h4>\n" + 
          "</div>\n" + 
          "<div id=\"col-2\" style=\"height: 6326px;\">\n" + 
          "  <div class=\"article-nav sidebar-nav\">\n" + 
          "    <span class=\"toc-link\">\n" + 
          "      <a title=\"Table of Contents\" href=\"/content/14/1.toc\">Table of Contents</a>\n" + 
          "    </span>\n" + 
          "  </div>\n" + 
          "  <div id=\"article-dyn-nav\" class=\"content-box\">\n" + 
          "    <div class=\"cb-contents\">\n" + 
          "      <h3 class=\"cb-contents-header\"><span>Navigate This Article</span></h3>\n" + 
          "    </div>\n" + 
          "  </div>\n" + 
          "</div>" +
          "</body>";

  private static final String col2HtmlFiltered = // div id=header is now filtered
      "<body>\n" +
      "</body>";

  private static final String spanTag =
      "<body>\n" +
          "<span id=\"top\"/>" +
          "<span class=\"free\"/>" +
          "</body>";
  private static final String spanTagFiltered =
//      "<body>" +
          "" +
//          "<span id=\"top\"/>" +
//          "</body>" +
          "";

  private static final String attrTag =
      "<body class=\"class\">\n" +
          "<h1 id=\"id\">title</h1>" +
          "<div id=\"top\"/>" +
          "</body>";
  private static final String attrTagFiltered =
//      "<body>" +
          "" +
//          "<h1>" +
          "title" +
//          "</h1>" +
//          "<div/>" +
//          "</body>" +
          "";

  private static final String divBlockInForm =
      "<form method=\"post\" action=\"/content/26/4.toc\">Show no tags" +
          "<div class=\"gca-buttons\">" +
          "<div class=\"hide-gca-buttons\">" +
          "<input type=\"reset\" name=\"reset\" value=\"Clear\" />" +
          "<input class=\"marked-citation-submit\" type=\"submit\" " +
          "name=\"submit\"value=\"Add to Marked Citations\" />" +
          "</div>" +
          "</div></form>";
  private static final String divBlockInFormFiltered = "Shownotags";
//      "<form method=\"post\" action=\"/content/26/4.toc\"></form>";


  public void testFiltering() throws Exception {
    assertFilterToSame(inst1, inst2);
    assertFilterToSame(withAds, withoutAds);
    assertFilterToSame(withSageAnchors, withoutSageAnchors);
    assertFilterToSame(withCopyright, withoutCopyright);
    assertFilterToSame(withCurrentIssue, withoutCurrentIssue);
    assertFilterToSame(withSporadicDivs, withoutSporadicDivs);
    assertFilterToSame(withCmeCredit, withoutCmeCredit);
    assertFilterToSame(withCbSection, withoutCbSection);
    assertFilterToSame(withRelatedURLs, withoutRelatedURLs);
    assertFilterToSame(withHwGenPage, withoutHwGenPage);
    assertFilterToSame(withNavCurrentIssue, withoutNavCurrentIssue);
    assertFilterToSame(withNavArticle, withoutNavArticle);
    assertFilterToSame(withCol4SquareAds, withoutCol4SquareAds);
    assertFilterToSame(withCol4TowerAds, withoutCol4TowerAds);
    assertFilterToSame(viewingDate, viewingDateFiltered);
    assertFilterToSame(gswHeader, gswHeaderFiltered);
    assertFilterToSame(relatedHtml, relatedHtmlFiltered);
    assertFilterToSame(citedHtml, citedHtmlFiltered);
    assertFilterToSame(socialHtml, socialHtmlFiltered);
    assertFilterToSame(adfootHtml, adfootHtmlFiltered);
    assertFilterToSame(europaceHtml, europaceHtmlFiltered);
    assertFilterToSame(col2Html, col2HtmlFiltered);

    assertFilterToString(textIndexFactor, textIndexFactorFiltered);
    assertFilterToString(hiddenInputHtml, hiddenInputFiltered);
    assertFilterToString(accessCheckHtml, accessCheckFiltered);
    assertFilterToString(institutionLogoHtml, institutionLogoFiltered);
    assertFilterToString(tocBannerAdHtml, tocBannerAdFiltered);
    assertFilterToString(sidebarGlobalNavHtml, sidebarGlobalNavFiltered);
    assertFilterToString(col3Html, col3Filtered);
    assertFilterToString(spanTag, spanTagFiltered);
    assertFilterToString(attrTag, attrTagFiltered);
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


  public void testHeadFiltering() throws Exception {
    InputStream actIn = fact.createFilteredInputStream(mau,
        new StringInputStream(headHtml),
        Constants.DEFAULT_ENCODING);

    assertEquals(headHtmlFiltered, StringUtil.fromInputStream(actIn));

    actIn = fact.createFilteredInputStream(mau,
        new StringInputStream(headHtmlWithComment),
        Constants.DEFAULT_ENCODING);
    assertEquals(headHtmlFiltered, StringUtil.fromInputStream(actIn));

  }

  public void testDivBlockInForm() throws Exception {
    InputStream actIn = fact.createFilteredInputStream(mau,
        new StringInputStream(divBlockInForm),
        Constants.DEFAULT_ENCODING);
    assertEquals(divBlockInFormFiltered, StringUtil.fromInputStream(actIn));
  }

}
