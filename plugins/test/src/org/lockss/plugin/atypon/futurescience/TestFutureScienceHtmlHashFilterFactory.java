/*
 * $Id: TestFutureScienceHtmlHashFilterFactory.java,v 1.2 2013-04-30 23:18:02 alexandraohlson Exp $
 */

/* Copyright (c) 2000-2013 Board of Trustees of Leland Stanford Jr. University, all rights reserved.

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
  
  private static final String topBannerHtml = "<start><div class=\"institutionBanner\" >Access provided by CLOCKSS archive </div><end>";
  private static final String topBannerHtmlFiltered = "<start><end>";
  
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
      "<tr><td colspan=\"2\" align=\"right\" valign=\"bottom\" class=\"identitiesBar\" style=\"padding-bottom:3px;\">" +
          "<span class=\"identitiesWelcome\">Welcome</span>" +
          "</td>" +
          "</tr>" +
          "</table>";
  
  private static final String footerHtml = 
      "<hr size=\"1\" />" +
          "<!-- contact info -->" +
          "<div class=\"bottomContactInfo\">Future Science Ltd, Unitec House, 2 Albert Place, London, N3 1QB, UK" +
          "<br />" +
          "Registered in England &amp; Wales, No: 4982360; VAT No: GB 893 6723 76" +
          "<br />" +
          "Tel: +44 (0)20 8371 6080 á Fax: +44 (0)20 8371 6089" +
          "<br />" +
          "We welcome your <a class=\"siteMapLink\" href=\"/feedback/show#FF\">Feedback</a>" +
          "<br />" +
          "</div>";
  private static final String footerHtmlFiltered =
      "<hr size=\"1\" />" +
          "<!-- contact info -->";

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
      "<table border=\"0\" cellpadding=\"2\" cellspacing=\"0\" width=\"100%\"><tr>" +
          "<td align=\"center\" class=\"section_head quickLinks_head\">Quick Links</td></tr>" +
          "<tr></tr>" +
          "</table>";
  
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
      "      <td valign=\"top\" width=\"165\">  " +
          "  <!-- end left side menu -->" +
          "  </td>";

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
      "<td></td><td width=\"10\">&nbsp;</td>";
  
  private static final String adPlaceholderHtml = 
      "<tr><td align=\"center\">" +
          "<form action=\"/action/doSearch\" name=\"quickSearchBoxForm\" class=\"quickSearchBoxForm\" method=\"get\">" +
          "<input type=\"hidden\" name=\"volume\" value=\"4\" />" +
          "<input type=\"hidden\" name=\"journal\" value=\"tde\" />" +
          "<table border=\"0\" cellspacing=\"3\" cellpadding=\"0\" class=\"quickSearchBoxForm\">" +
          "<tr><td><label for=\"QSFSearchText\" accesskey=\"t\" class=\"header\">Quick search:</label></td></tr>" +
          "<tr><td align=\"right\"><a href=\"/search/advanced\">Advanced Search</a></td></tr>" +
          "</table></form>" +
          "</td></tr>" +
          "</table>" +
          "<!-- placeholder id=null, description=TOC Marketing Message 2 --></td>" +
          "</tr>" +
          "</table>" +
          "</td>" +
          "</tr>" +
          "</table>" +
          "<td valign=\"top\" width=\"170\">" +
          "<table width=\"165\" cellspacing=\"0\" cellpadding=\"0\">" +
          "   <tr><td><!-- placeholder id=null, description=Right Region 1 --><a href=\"/action/clickThrough?id=1137&url=http%3A%2F%2Fwww.smi-online.co.uk%2Fgoto%2Fadcsummit37.asp&loc=%2Ftoc%2Ftde%2F4%2F1&pubId=40007148\" target=\"_blank\"><img src=\"/sda/1137/ADCSummit.gif\"/></a></td></tr>" +
          "<tr><td height=\"5\"></td></tr>" +
          "<tr><td><!-- placeholder id=null, description=Right Region 2 --></td></tr>" +
          "<tr><td height=\"5\"></td></tr>" +
          "<tr><td><!-- placeholder id=null, description=Right Region 3 --><a href=\"mailto:j.walker@future-science.com?subject=Accelerated Publication enquiry\" target=\"_blank\"><img src=\"/sda/1144/APub.jpg\"/></a></td></tr>" +
          "<tr><td height=\"5\"></td></tr>" +
          "</table>" +
          "   </td>" +
          "</tr>" +
          "</table>";
  
  private static final String adPlaceholderFiltered = 
      "<tr>" +
          "</tr>" +
          "</table>" +
          "<td valign=\"top\" width=\"170\">" +
          "<table width=\"165\" cellspacing=\"0\" cellpadding=\"0\">" +
          "   <tr></tr>" +
          "<tr><td height=\"5\"></td></tr>" +
          "<tr></tr>" +
          "<tr><td height=\"5\"></td></tr>" +
          "<tr></tr>" +
          "<tr><td height=\"5\"></td></tr>" +
          "</table>" +
          "   </td>" +
          "</tr>" +
          "</table>";
  
  private static final String adSimple =
      "<html><body<table><tr><td><!-- placeholder id=null --></td></tr></table></body></html>";
  private static final String adSimpleFiltered =
      "<html><body><table><tr></tr></table></body></html>";

  
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
 
}
