/*
 * $Id$
 */

/* Copyright (c) 2000-2015 Board of Trustees of Leland Stanford Jr. University, all rights reserved.

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
package org.lockss.plugin.atypon.futurescience;


import java.io.InputStream;
import org.lockss.test.*;
import org.lockss.util.Constants;
import org.lockss.util.StringUtil;

public class TestFutureScienceHtmlHashFilterFactory extends LockssTestCase{
  private FutureScienceHtmlHashFilterFactory filt;
  private MockArchivalUnit mau;

  public void setUp() throws Exception {
    super.setUp();
    filt = new FutureScienceHtmlHashFilterFactory();
    mau = new MockArchivalUnit();
  }
  
  /*
   * We have now added removal of tags and whitespace filtering so the filtered
   * result looks significantly different!
   */

  private static final String topBannerHtml = "<start><div class=\"institutionBanner\" >Access provided by CLOCKSS archive </div><end>";
  private static final String topBannerHtmlFiltered = " ";

  private static final String loginHtml =
      "<tr><td colspan=\"2\" align=\"right\" valign=\"bottom\" class=\"identitiesBar\" style=\"padding-bottom:3px;\">" +
          "<span class=\"identitiesWelcome\">Welcome</span>" +
          "<span class=\"identitiesName\"> CLOCKSS archive  | Login via <a href=\"http://blah\"> Athens</a>" +
          "or your <a href=\"/action/ssostart?redirectUri=blah\">home institution</a>" +
          "</span>" +
          "</td>" +
          "</tr>" +
          "</table>";
  private static final String loginHtmlFiltered =
          " ";

  private static final String rssFeedHtml =
      "<font color=\"#000000\">" +
          "" +
          "" +
          "&nbsp;";

  private static final String rssFeedHtmlFiltered =
      " " +
          "&nbsp;";


  private static final String freeHtml =
      "    <!--totalCount15--><!--modified:1375798955000-->" +
          "<h2 class=\"tocHeading\">" +
          "<span class=\"subj-group\">Special Focus Issue</span>" +
          "</h2>" +
          "<table border=\"0\" width=\"100%\" class=\"articleEntry\">" +
          "<tr>" +
          "<td align=\"right\" valign=\"top\" width=\"18\">" +
          "<input type=\"checkbox\" name=\"doi\" value=\"10.4155/xxx\"/>" +
          "<br />" +
          "</td>" +
          "<td valign=\"top\">" +
          "<div class=\"art_title\">" +
          "<img src=\"/templates/jsp/_midtier/_FFA/images/free.gif\" alt=\"\" border=\"0\"/> &nbsp;Title Here" +
          "</div>" +
          "</td>" +
          "</tr>" +
          "</table>";

  private static final String freeHtmlFiltered =
      " " +
          "Special Focus Issue" +
          " &nbsp;Title Here ";


  private static final String footerHtml =
      "<hr size=\"1\" />" +
          "<!-- contact info -->" +
          "<div class=\"bottomContactInfo\">Future Science Ltd, Unitec House, 2 Albert Place, London, N3 1QB, UK" +
          "<br />" +
          "Registered in England &amp; Wales, No: 4982360; VAT No: GB 893 6723 76" +
          "<br />" +
          "Tel: +44 (0)20 8371 6080 &middot; Fax: +44 (0)20 8371 6089" +
          "<br />" +
          "We welcome your <a class=\"siteMapLink\" href=\"/feedback/show#FF\">Feedback</a>" +
          "<br />" +
          "</div>";
  private static final String footerHtmlFiltered =
          " ";

  private static final String quickLinksHtml =
      "<table border=\"0\" cellpadding=\"2\" cellspacing=\"0\" width=\"100%\"><tr>" +
          "<td align=\"center\" class=\"section_head quickLinks_head\">Quick Links</td></tr>" +
          "<tr><td class=\"quickLinks_content\"><table border=\"0\" cellspacing=\"0\" cellpadding=\"1\" width=\"100%\"><tr><td class=\"black9pt\"> &bull; </td>" +
          "<td><a class=\"ref\" href=\"javascript:void(0);\" title=\"Opens new window\" onclick=\"window.open('/action/showReprints')\">Reprints &amp; Permissions</a></td></tr>" +
          "<script type=\"text/javascript\">genSideCitation('8','/servlet/');</script>" +
          "<tr><td class=\"black9pt fullSideBullet\"> &bull; </td>" +
          "<td class=\"black9pt\" valign=\"middle\">Alert me when:<br /></td></tr>" +
          "<tr><td class=\"black9pt fullSideBullet\"> &bull; </td>" +
          "<td class=\"black9pt\" valign=\"middle\">Related articles found in:<br /><a href=\"/action/doSearch\">Future Science</a>" +
          "<script type=\"text/javascript\">genSideRelated('8','/servlet/linkout?suffix=s0','PubMed');" +
          "</script></td></tr>" +
          "<tr><td class=\"black9pt\" valign=\"middle\"><a href=\"/action/showMostReadArticles?journalCode=tde\">View Most Downloaded Articles</a></td>" +
          "</tr></table></td></tr>" +
          "</table>";
  private static final String quickLinksHtmlFiltered =
      " " +
          "Quick Links" +
          " ";

  private static final String sideMenuHtml =
      "      <td valign=\"top\" width=\"165\">" +
          "  <table class=\"sideMenu mceItemTable\" cellpadding=\"2\" width=\"165\">" +
          "  <tbody style=\"text-align: left;\">" +
          " <tr style=\"text-align: left;\">" +
          " <td style=\"text-align: left;\" class=\"sideMenuHead\">Journals</td>" +
          "  </tr>" +
          "  <tr style=\"text-align: left;\">" +
          "  <td style=\"text-align: left;\"><a href=\"/loi/bio\">Bioanalysis</a>" +
          "  <br />" +
          "  </td>" +
          "  <tr style=\"text-align: left;\">" +
          "  <td style=\"text-align: left;\" class=\"sideMenuHead\">Downloads/Links</td>" +
          "  </tr>" +
          "  <tr style=\"text-align: left;\">" +
          "  <td style=\"text-align: left;\"><a href=\"/userimages/FSG_Catalogue_2013.pdf\" target=\"_blank\">2013 Catalogue</a>" +
          "  <br />" +
          "  </td>" +
          "  </tr>" +
          "  </tbody>" +
          "  </table>" +
          "  <!-- end left side menu -->" +
          "  </td>";
  private static final String sideMenuHtmlFiltered =
      " ";

  private static final String alsoReadHtml =
      "<td><div class=\"full_text\">" +
          "<div class=\"header_divide\"><h3>Users who read this article also read:</h3></div>" +
          "<table border=\"0\" width=\"100%\" class=\"articleEntry\"><tr>" +
          "<td align=\"right\" valign=\"top\" width=\"18\"><input type=\"checkbox\" name=\"doi\" value=\"10.4155/tde.12.122\"/><br /></td>" +
          "<td valign=\"top\"><div class=\"art_title\">Title </div>" +
          "<a class=\"ref nowrap\" href=\"/doi/abs/10.4155/tde.12.122\">Citation</a>" +
          "    | <a class=\"ref nowrap\" href=\"/doi/full/10.4155/tde.12.122\">Full Text</a>" +
          "    | <a class=\"ref nowrap\" target=\"_blank\" title=\"Opens new window\" href=\"/doi/pdf/10.4155/tde.12.122\">PDF (1093 KB)</a>" +
          "    | <a class=\"ref nowrap\" target=\"_blank\" title=\"Opens new window\" href=\"/doi/pdfplus/10.4155/tde.12.122\">PDF Plus (1103 KB)</a>" +
          "    | <a class=\"ref\" href=\"/personalize/addFavoriteArticle?doi=10.4155%2Ftde.12.122\">Add to Favorites</a>" +
          "    | <a class=\"ref\" href=\"/action/doSearch?doi=10.4155%2Ftde.12.122&amp;target=related\">Related</a>&nbsp;<!-- ${xml_link: 10.4155%2Ftde.12.122} -->" +
          "   | <a class=\"ref nowrap\" href=\"javascript:void(0)\" title=\"Opens new window\" onclick=\"window.open('/action/showReprints\">" +
          " Reprints &amp; Permissions </a>" +
          " <script type=\"text/javascript\"> genSfxLinks('s0', '', '10.4155/tde.12.122');</script></td><td valign=\"top\"></td></tr></table>" +
          "  </div></td><td width=\"10\">&nbsp;</td>";
  private static final String alsoReadHtmlFiltered =
      " &nbsp; ";


  private static final String adPlaceholderLimitTest =
      "<table><tr><td><h1>YES</h1><h1>YES</h1><h1>YES</h1></td>" +
          "<td><h1>NO</h1><h1>NO</h1><!-- placeholder id=null, description=test --><a href=\"foo\"></a></td>" +
          "<td>YES<table><tr><td>YES</td><td><h1>NO</h1><span>blah</span><!-- placeholder id=null, description=test -->    " +
          "<a href=\"foo\"></a></td><td><h3>YES</h3></td></tr></table></td>" +
          "</tr></table>";
  private static final String adPlaceholderLimitFiltered =
      " YES YES YES " +
          "YES YES YES" +
          " ";

  private static final String adSimple =
      "<html><body<table><tr><td><!-- placeholder id=null --></td></tr></table></body></html>";
  private static final String adSimpleFiltered =
      " ";

  private static final String citedBySection =
      "</div><!-- /abstract content --><!-- fulltext content -->" +
          "<div class=\"citedBySection\"><a name=\"citedBySection\"></a><h2>Cited by</h2>" +
          "<div class=\"citedByEntry\">" +
          "<a href=\"/action/foo\">Author</a>.  (2012) Recent Title. <i><span class=\"NLM_source\">Bioanalysis</span></i> <b>4</b>:9, 1123-1140<br />" +
          "Online publication date: 1-May-2012.<br /><span class=\"CbLinks\"><a class=\"ref nowrap\" href=\"/doi/abs/10.4155/bio.xx.73\">Summary</a>" +
          " | <a class=\"ref nowrap\" href=\"/doi/full/10.4155/bio.xx.73\">Full Text</a>" +
          " | <a class=\"ref nowrap\" target=\"_blank\" title=\"Opens new window\" href=\"/doi/pdf/10.4155/bio.xx.73\">PDF (1157 KB)</a>" +
          " | <a class=\"ref nowrap\" target=\"_blank\" title=\"Opens new window\" href=\"/doi/pdfplus/10.4155/bio.xx.73\">PDF Plus (1157 KB)</a>" +
          "&nbsp;<!-- ${cadmus-articleworks: 10.4155%2Fbio.12.73 class='ref' } --><!-- ${xml_link: 10.4155%2Fbio.12.73} -->" +
          " | <a class=\"ref nowrap\" href=\"javascript:void(0)\" title=\"Opens new window\" onclick=\"window.open('/action/showReprints', '_blank', 'width=950,height=800')\">" +
          " Reprints &amp; Permissions" +
          "</a></span></div>" +
          "</div><!-- /fulltext content -->" +
          "<div class=\"article_link\">";
  private static final String citedBySectionFiltered =
      " ";

  // expert-reviews uses slightly different text for bottom information
  // for the filter to work there needs to be a parent of the node to remove (as in real life examples)
  private static final String expReviewsBottom =
      "<body>" +
          "<!--end main menu-->" +
          "<table>foo goes here </table>" +
          "<br />" +
          "<div class=\"bottomSiteMapLink\">" +
          "<a class=\"siteMapLink\" href=\"/\">Home</a> | " +
          "<br />" +
          "<a class=\"siteMapLink\" href=\"/page/advertisers\">For Advertisers</a>" +
          "<span style=\"COLOR: rgb(0,127,97)\"> </span>" +
          "<a class=\"siteMapLink\" href=\"/page/reprints.jsp\">Reprints and Supplements</a> " +
          "<span style=\"COLOR: rgb(0,127,97)\">|</span> " +
          "<a class=\"siteMapLink\" href=\"/page/advertising\">Advertising</a>" +
          "<span style=\"COLOR: rgb(0,127,97)\"> | " +
          "<a class=\"siteMapLink\" href=\"/page/pressrelease\">Press Releases and News</a></span>" +
          "<span style=\"COLOR: rgb(0,127,97)\"> |</span> " +
          "<a class=\"siteMapLink\" href=\"/page/fdhelp\">Help</a>" +
          "<br />" +
          "<a href=\"http://www.crossref.org/\">" +
          "<img style=\"WIDTH: 80px; HEIGHT: 31px\" alt=\"\" src=\"/userimages/blah.gif\" border=\"0\" height=\"35\" hspace=\"0\" width=\"80\" /></a>  " +
          "<a href=\"http://www.projectcounter.org/\">" +
          "<img style=\"WIDTH: 81px; HEIGHT: 31px\" alt=\"\" src=\"/userimages/blah.gif\" border=\"0\" height=\"33\" hspace=\"0\" width=\"85\" /></a>" +
          "<br />" +
          "</div>" +
          "<hr size=\"1\" />" +
          "<!-- contact info -->" +
          "<div style=\"text-align: center;\">" +
          "<span class=\"fontSize1\">Copyright © 2013 Informa Plc. All rights reserved." +
          "<br />" +
          "<br />" +
          " This site is owned and operated by whatever plus contact info." +
          "<br />" +
          "</span></div>" +
          "<span class=\"fontSize1\"></span>" +
          "</body>";

  private static final String expReviewsBottomFiltered =
      " " +
          "foo goes here ";

  private static final String spanCommentHtml =
      "<div class=\"ack\"> " +
      "<span class=\"title\" id=\"d119043e299\">Put the Title Here.</span>" +
      "  <p>    <!--totalCount11--><!--modified:1386095090000-->" +
      "<h2 class=\"tocHeading\"></h2></p>" +
      "<table><tr><td>ONE</td>" +
      "<td>TWO" +
      "<!-- placeholder id=null, description=TOC Marketing Message 2 --></td>" +
      "</tr></table>" +
      "</div>";

  private static final String spanCommentFiltered =
      " " +
      "Put the Title Here." +
      " " +
      "ONE" +
      " ";

  // the listing of articles on a TOC
  private static final String pdfPlusSize =
      "<div>" +
          "<a class=\"ref nowrap\" target=\"_blank\" title=\"Opens new window\" " +
          "href=\"/doi/pdfplus/10.2217/foo\">PDF Plus (584 KB)</a>" +
          "<a class=\"ref nowrap\" target=\"_blank\" title=\"Opens new window\" " +
          "href=\"/doi/pdfplus/10.2217/foo\">Pdf (584 KB)</a>" +
          "<a class=\"ref nowrap\" target=\"_blank\" title=\"Opens new window\" " +
          "href=\"/doi/pdfplus/10.2217/foo\">PDFplus(584 KB)</a>" +
      "</div>";
  private static final String pdfPlusSizeFiltered =
      " ";

  // the filesize with the links on an article page
  private static final String fileSize =
      "<div>" +
      "<a href=\"/doi/pdfplus/10.2217/foo\" target=\"_blank\">View PDF Plus " +
  "<span class=\"fileSize\">(584 KB)</span></a>" +
          "</div>";
  private static final String fileSizeFiltered =
      " View PDF Plus ";

  public void test_topBannerHtml() throws Exception {
    InputStream actIn = filt.createFilteredInputStream(mau,
        new StringInputStream(topBannerHtml),
        Constants.DEFAULT_ENCODING);

    assertEquals(topBannerHtmlFiltered, StringUtil.fromInputStream(actIn));
  }

  public void test_loginHtml() throws Exception {
    InputStream actIn = filt.createFilteredInputStream(mau,
        new StringInputStream(loginHtml),
        Constants.DEFAULT_ENCODING);

    assertEquals(loginHtmlFiltered, StringUtil.fromInputStream(actIn));
  }
  public void test_footerHtml() throws Exception {
    InputStream actIn = filt.createFilteredInputStream(mau,
        new StringInputStream(footerHtml),
        Constants.DEFAULT_ENCODING);

    assertEquals(footerHtmlFiltered, StringUtil.fromInputStream(actIn));

    actIn = filt.createFilteredInputStream(mau,
        new StringInputStream(expReviewsBottom),
        Constants.DEFAULT_ENCODING);

    assertEquals(expReviewsBottomFiltered, StringUtil.fromInputStream(actIn));
  }

  public void test_quickLinksHtml() throws Exception {
    InputStream actIn = filt.createFilteredInputStream(mau,
        new StringInputStream(quickLinksHtml),
        Constants.DEFAULT_ENCODING);

    assertEquals(quickLinksHtmlFiltered, StringUtil.fromInputStream(actIn));
  }
  public void test_sideMenuHtml() throws Exception {
    InputStream actIn = filt.createFilteredInputStream(mau,
        new StringInputStream(sideMenuHtml),
        Constants.DEFAULT_ENCODING);

    assertEquals(sideMenuHtmlFiltered, StringUtil.fromInputStream(actIn));
  }
  public void test_alsoReadHtml() throws Exception {
    InputStream actIn = filt.createFilteredInputStream(mau,
        new StringInputStream(alsoReadHtml),
        Constants.DEFAULT_ENCODING);

    assertEquals(alsoReadHtmlFiltered, StringUtil.fromInputStream(actIn));
  }

  public void test_adPlaceholder() throws Exception {
    InputStream actIn = filt.createFilteredInputStream(mau,
        new StringInputStream(adSimple),
        Constants.DEFAULT_ENCODING);

    assertEquals(adSimpleFiltered, StringUtil.fromInputStream(actIn));
  }

  public void test_adPlaceholderAgain() throws Exception {
    InputStream actIn = filt.createFilteredInputStream(mau,
        new StringInputStream(adPlaceholderLimitTest),
        Constants.DEFAULT_ENCODING);
    assertEquals(adPlaceholderLimitFiltered, StringUtil.fromInputStream(actIn));

  }

  public void test_citedBySection() throws Exception {
    InputStream actIn = filt.createFilteredInputStream(mau,
        new StringInputStream(citedBySection),
        Constants.DEFAULT_ENCODING);
    assertEquals(citedBySectionFiltered, StringUtil.fromInputStream(actIn));

  }

  public void test_rssFeedSection() throws Exception {
    InputStream actIn = filt.createFilteredInputStream(mau,
        new StringInputStream(rssFeedHtml),
        Constants.DEFAULT_ENCODING);
    assertEquals(rssFeedHtmlFiltered, StringUtil.fromInputStream(actIn));

  }

  public void test_freeGlyphSection() throws Exception {
    InputStream actIn = filt.createFilteredInputStream(mau,
        new StringInputStream(freeHtml),
        Constants.DEFAULT_ENCODING);
    assertEquals(freeHtmlFiltered, StringUtil.fromInputStream(actIn));

  }

  public void test_spanCommentHtml() throws Exception {
    InputStream actIn = filt.createFilteredInputStream(mau,
        new StringInputStream(spanCommentHtml),
        Constants.DEFAULT_ENCODING);
    assertEquals(spanCommentFiltered, StringUtil.fromInputStream(actIn));

  }

  public void test_pdfPlusSize() throws Exception {
    InputStream actIn = filt.createFilteredInputStream(mau,
        new StringInputStream(pdfPlusSize),
        Constants.DEFAULT_ENCODING);
    assertEquals(pdfPlusSizeFiltered, StringUtil.fromInputStream(actIn));
    actIn = filt.createFilteredInputStream(mau,
        new StringInputStream(fileSize),
        Constants.DEFAULT_ENCODING);
    assertEquals(fileSizeFiltered, StringUtil.fromInputStream(actIn));
  }

}
