/*
 * $Id$
 */

package org.lockss.plugin.edinburgh;

import java.io.*;
import org.lockss.util.*;
import org.lockss.test.*;

public class TestEdinburghHashHtmlFilterFactory extends LockssTestCase {
  static String ENC = Constants.DEFAULT_ENCODING;

  private EdinburghUniversityPressHashHtmlFilterFactory fact;
  private MockArchivalUnit mau;

  public void setUp() throws Exception {
    super.setUp();
    fact = new EdinburghUniversityPressHashHtmlFilterFactory();
    mau = new MockArchivalUnit();
  }

  private static final String withCitations = "<div id=\"firstPage\">"
      + "<img src=\"/na101/home/literatum/jbctv.2006.3.1.128.fp.png_v03\""
      + "alt=\"Free first page\" class=\"firstPageImage\"/>"
      + "</div>"
      + "<div class=\"citedBySection\">"
      + "<a name=\"citedBySection\"></a> <h2>Cited by</h2>"
      + "<div class=\"citedByEntry\"> <i>"
      + "<span class=\"NLM_source\">Comparative Critical Studies</span></i>"
      + "<span class=\"CbLinks\">"
      + "<a class=\"articleListing_links\" href=\"/doi/abs/10.3366/ccs.2011.0017\">"
      + "Citation</a>"
      + "<a class=\"articleListing_links\" href=\"/doi/pdfplus/10.3366/ccs.2011.0017\">"
      + "<input type=\"image\" alt=\"Pdf With References\""
      + "src=\"/templates/jsp/_style2/_eup/images/icon_toolBar_pdf.gif\"/>"
      + "</a></span> </div> </div>";

  private static final String withoutCitations = "<div id=\"firstPage\">"
      + "<img src=\"/na101/home/literatum/jbctv.2006.3.1.128.fp.png_v03\""
      + "alt=\"Free first page\" class=\"firstPageImage\"/>"
      + "</div>";

  private static final String mainNavHtml ="<div id=\"mainNavContainer\">"
      + "<ul id=\"mainNav\"> "
      + "<li><p> <a href=\"/\">Home</a> </p></li>"
      + "<li><p><a href=\"/page/infoZone/home\">Information Zone</a></p></li>"
      + "<li class=\"institutionBanner institutionBannerText\"><p>\"Serials Department, Green Library\"</p></li>"
      + "</ul> </div>";

  private static final String mainNavHtmlFiltered = "";

  private static final String institutionBannerHtml ="<div id=\"blah\">"
      + "<ul id=\"mainNav\"> "
      + "<li><p> <a href=\"/\">Home</a> </p></li>"
      + "<li><p><a href=\"/page/infoZone/home\">Information Zone</a></p></li>"
      + "<li class=\"institutionBanner institutionBannerText\"><p>\"Serials Department, Green Library\"</p></li>"
      + "</ul> </div>";

  private static final String institutionBannerFiltered = "<div id=\"blah\">"
      + "<ul id=\"mainNav\"> "
      + "<li><p> <a href=\"/\">Home</a> </p></li>"
      + "<li><p><a href=\"/page/infoZone/home\">Information Zone</a></p></li>"
      + "</ul> </div>";  


  private static final String emptyString = "";
  private static final String mastHead =
      "<div id=\"masthead\" class=\"clearfix\">" +
          "<div id=\"globalLinksWrapper\">" +
          "    <ul class=\"globalLinks\">" +
          "        <li class=\"first\"><img src=\"/templates/blah.gif\" alt=\"Help\">" +
          "        <p class=\"globalLinkLabel\"><a href=\"/help\" onclick=\"popupHelp(this.href); return false;\" target=\"_blank\">Help</a></p></li>" +
          "        <li> <p class=\"globalLinkLabel\"><a href=\"/action/ssostart\">Shibboleth</a></p> </li>" +
          "        <li> <p class=\"globalLinkLabel\">" +
          "                <a href=\"https://www.euppublishing.com/action/registration\" class=\"registerLink\" title=\"Register\">Register</a>" +
          "            </p></li>" +
          "        <li>" +
          "            <img src=\"/templates/blah.gif\" alt=\"Log In\">" +
          "            <p class=\"globalLinkLabel\">" +
          "                <a href=\"https://www.blah\" class=\"loginLink\" title=\"Log In\">Log In</a>" +
          "            </p>" +
          "        </li>" +
          "    </ul>" +
          "</div>" +
          "</div>";
  private static final String advSearch = "<div id=\"advSearchNavBottom\"> </div>";

  private static final String mainBreadCrumb = 
      "<div id=\"mainBreadCrumb\">" +
          "        <a href=\"/action/showPublications?display=bySubject&amp;pubType=journal\">Journals</a> &gt;" +
          "        <a href=\"/journal/elr\">JournalTitlew</a> &gt;" +
          "            <a href=\"/loi/elr\">All Issues</a> &gt;" +
          "            <span class=\"currentBreadCrumb\">JANUARY 1997</span>" +
          "</div>";

  private static final String journalTitleContainer =
      "<div id=\"journalTitleContainer\">" +
          "    <h1>Title</h1>" +
          "<div id=\"globalSearchForm\">" +
          "    <form action=\"/action/doSearch\" class=\"search\" method=\"get\"><fieldset>" +
          "            <legend>Search</legend>" +
          "            <label for=\"globalSearchField\">Search</label>" +
          "           <input id=\"globalSearchField\" class=\"text\" type=\"search\" name=\"searchText\" value=\"\">" +
          "            <input class=\"button\" type=\"image\" value=\"\" src=\"/templates.gif\" alt=\"go\">" +
          "            <p><a href=\"/search/advanced\">Advanced Search</a></p>" +
          "        </fieldset></form>" +
          "</div>" +
          "</div>";




  public void testCitationsFiltering() throws Exception {
    InputStream actIn = fact.createFilteredInputStream(mau, new StringInputStream(withCitations),
        Constants.DEFAULT_ENCODING);
    assertEquals(withoutCitations, StringUtil.fromInputStream(actIn));
  }

  public void testInstitutionBannerFiltering() throws Exception {
    InputStream actIn = fact.createFilteredInputStream(mau, new StringInputStream(institutionBannerHtml),
        Constants.DEFAULT_ENCODING);
    assertEquals(institutionBannerFiltered, StringUtil.fromInputStream(actIn));
  } 

  public void testMainNavFiltering() throws Exception {
    InputStream actIn = fact.createFilteredInputStream(mau, new StringInputStream(mainNavHtml),
        Constants.DEFAULT_ENCODING);
    assertEquals(mainNavHtmlFiltered, StringUtil.fromInputStream(actIn));
  } 

  public void testTopFiltering() throws Exception {
    InputStream actIn = fact.createFilteredInputStream(mau, new StringInputStream(mastHead),
        Constants.DEFAULT_ENCODING);
    assertEquals(emptyString, StringUtil.fromInputStream(actIn));

    actIn = fact.createFilteredInputStream(mau, new StringInputStream(advSearch),
        Constants.DEFAULT_ENCODING);
    assertEquals(emptyString, StringUtil.fromInputStream(actIn));

    actIn = fact.createFilteredInputStream(mau, new StringInputStream(mainBreadCrumb),
        Constants.DEFAULT_ENCODING);
    assertEquals(emptyString, StringUtil.fromInputStream(actIn));

    actIn = fact.createFilteredInputStream(mau, new StringInputStream(journalTitleContainer),
        Constants.DEFAULT_ENCODING);
    assertEquals(emptyString, StringUtil.fromInputStream(actIn));
  } 

}
