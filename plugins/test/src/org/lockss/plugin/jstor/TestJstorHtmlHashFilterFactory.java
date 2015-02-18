/*  $Id$

 Copyright (c) 2000-2003 Board of Trustees of Leland Stanford Jr. University,

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

package org.lockss.plugin.jstor;

import java.io.InputStream;

import org.lockss.test.LockssTestCase;
import org.lockss.test.MockArchivalUnit;
import org.lockss.test.StringInputStream;
import org.lockss.util.Constants;
import org.lockss.util.StringUtil;

public class TestJstorHtmlHashFilterFactory extends LockssTestCase {
  private JstorHtmlHashFilterFactory fact;
  private MockArchivalUnit mau;

  public void setUp() throws Exception {
    super.setUp();
    fact = new JstorHtmlHashFilterFactory();
  }

  private static final String variableRel = 
      "<div class=\"floatingFigure line\" id=\"f1\">" +
          "<div class=\"thumbnailContainer unit size1of4\">" +
          "<div class=\"thumbnail\">" +
          "<a href=\"/action/showPopup\" target=\"_blank\" rel=\"overlay-d7261e249\" title=\"Fig. 1\">" +
          "<img src=\"/literatum/f01.gif\" alt=\"Figure thumbnail\" class=\"thumb\" />" +
          "<span class=\"sizeInKb\">" +
          " (14KB)</span>" +
          "</a></div></div></div>";

  private static final String variableRelFiltered = 
      "<div class=\"floatingFigure line\" id=\"f1\">" +
          "<div class=\"thumbnailContainer unit size1of4\">" +
          "<div class=\"thumbnail\">" +
          "<a href=\"/action/showPopup\" target=\"_blank\"  title=\"Fig. 1\">" +
          "<img src=\"/literatum/f01.gif\" alt=\"Figure thumbnail\" class=\"thumb\" />" +
          "<span class=\"sizeInKb\">" +
          " (14KB)</span>" +
          "</a></div></div></div>";

  //marketing-survey
  private static final String mktSurvey = 
      "<div id=\"marketing-survey\" class=\"hide\">" +
          "<h3 class=\"h4 pan mbn\">Help us out</h3>" +
          "<p class=\"man\">Can we call you for a 10 minute interview?</p>" +
          "</div>";
  private static final String mktSurveyFiltered = 
      "";

  //marketingLinks
  private static final String mktLinks = 
      "<div class=\"marketingLinks\">" +
          "<div class=\"subscribe\">" +
          "</div>" +
          "<div id=\"subscribeInfoOverlay\" style='display:none'>" +
          "</div></div>";
  private static final String mktLinksFiltered = 
      "";

  //banner
  private static final String banner = 
      "<div class=\"mainInner\">" +
          "<div class=\"banner\">" +
          "<div class=\"banner\">" +
          "<div id=\"headerPubLogo\">" +
          "<div class=\"pubLogoSingleline\">" +
          "<a href=\"http://www.jstor.org/action/\">" +
          "<img src=\"/userimages/x/banner\" alt=\"University Press\"/></a>" +
          "</div></div></div></div>" +
          "</div>";
  private static final String bannerFiltered = 
      "<div class=\"mainInner\">" +
          "</div>";  

  //infoBox
  private static final String infoBox = 
      "<div class=\"infoBox\" >" +
          "<div class=\"cite\">" +
          "<div class=\"articleImage\">" +
          "<!-- articleJournalCover.jsp -->" +
          "<a href=\"/action/showPublication?journalCode=x\">" +
          " <img id=\"journalCover\" src=\"/literatum/publisher/foo.jpg\" alt=\"\" />" +
          "</a>" +
          "</div></div></div>";
  private static final String infoBoxFiltered = 
      "";


  //articleBody_author
  private static final String authorInfo =   
      "<div class=\"articleBody_author\">" +
          "<div class=\"authors\">John Smit<div class=\"NLM_bio\"><p class=\"first last\">" +
          "<b>John Smith</b> is associate professor of English at the University.</p></div></div></div>" +
          "<!-- /abstract content -->";
  private static final String authorInfoFiltered =   
      "";

  //journalLinks
  private static final String jnlLinks =   
      "<div id=\"journalLinks\">" +
          "<ul class=\"simpleList\">" +
          "    <li><a href=\"/stable/10.1111/foo\">Latest Issue</a></li>" +
          "    <li><a href=\"/action/showPublication?journalCode=foo\">All Issues</a></li>" +
          "</ul>" +
          "</div>";
  private static final String jnlLinksFiltered =   
      "";

  //issueTools
  private static final String issueTools =   
      "<div class=\"stuff\">" +
          "      <ul class=\"issueTools inline clear\">" +
          "       <li><strong>Citation Tools </strong></li>" +
          "        <li>" +
          "          <a class=\"export\" href=\"foo\">Export</a>" +
          "        </li>" +
          "      </ul>    " +
          "</div>";
  private static final String issueToolsFiltered =   
      "<div class=\"stuff\">" +
          "          " +
          "</div>";

  //subCite
  private static final String subCite =   
      "<div class=\"cite\">" +
          "<div class=\"subCite\">" +
          "<div class=\"tipContainer fLeft\"><img src=\"/templates/foo.gif\" alt=\"\" class=\"accessIcon\" />" +
          "</div><input type=\"checkbox\" name=\"doi\" class=\"checkBox\" value=\"foo\"/>" +
          "</div>" +
          "<div class=\"mainCite\"></div></div>";
  private static final String subCiteFiltered =   
      "<div class=\"cite\">" +
          "<div class=\"mainCite\"></div></div>";

  //SCDataSiteWide
  private static final String scData = 
      "<div id=\"SCDataSiteWide\" data-institution=\"Stanford University\" " +
          "data-loginevent=\"\" data-loginstatus=\"\" data-registrationcomplete=\"\" class=\"hide\">" +
          "</div>";
  private static final String scDataFiltered = 
      "";

  //headGlobalContainer
  private static final String head = 
      "<div class=\"page liquid  fullText\">" +
          "<div class=\"head globalContainer\">" +
          "<div id=\"ufo\">" +
          "       <h1 class=\"ufo\">JSTOR</h1>" +
          "</div>" +
          "<div id=\"skipNav\"><a href=\"#mainContent\" class=\"ufo\">Skip to Main Content</a></div>" +
          "</div>" +
          "</div>";
  private static final String headFiltered = 
      "<div class=\"page liquid  fullText\">" +
          "" +
          "</div>";



  /*
   *  Compare Html and HtmlFiltered
   */

  public void testRelFiltering() throws Exception {
    InputStream actIn = fact.createFilteredInputStream(mau,
        new StringInputStream(variableRel), Constants.DEFAULT_ENCODING);
    assertEquals(variableRelFiltered, StringUtil.fromInputStream(actIn));
  }
  public void testMktFiltering() throws Exception {
    InputStream actIn = fact.createFilteredInputStream(mau,
        new StringInputStream(mktSurvey), Constants.DEFAULT_ENCODING);
    assertEquals(mktSurveyFiltered, StringUtil.fromInputStream(actIn));

    actIn = fact.createFilteredInputStream(mau,
        new StringInputStream(mktLinks), Constants.DEFAULT_ENCODING);
    assertEquals(mktLinksFiltered, StringUtil.fromInputStream(actIn));

  }
  public void testBannerFiltering() throws Exception {
    InputStream actIn = fact.createFilteredInputStream(mau,
       new StringInputStream(banner), Constants.DEFAULT_ENCODING);
    assertEquals(bannerFiltered, StringUtil.fromInputStream(actIn));
  }
  public void testInfoFiltering() throws Exception {
    InputStream actIn = fact.createFilteredInputStream(mau,
        new StringInputStream(infoBox), Constants.DEFAULT_ENCODING);
    assertEquals(infoBoxFiltered, StringUtil.fromInputStream(actIn));
    
    actIn = fact.createFilteredInputStream(mau,
        new StringInputStream(authorInfo), Constants.DEFAULT_ENCODING);
    assertEquals(authorInfoFiltered, StringUtil.fromInputStream(actIn));
  }
  public void testJnlFiltering() throws Exception {
    InputStream actIn = fact.createFilteredInputStream(mau,
        new StringInputStream(jnlLinks), Constants.DEFAULT_ENCODING);
    assertEquals(jnlLinksFiltered, StringUtil.fromInputStream(actIn));
  }
  public void testIssueFiltering() throws Exception {
    InputStream actIn = fact.createFilteredInputStream(mau,
        new StringInputStream(issueTools), Constants.DEFAULT_ENCODING);
    assertEquals(issueToolsFiltered, StringUtil.fromInputStream(actIn));
  }
  public void testCiteFiltering() throws Exception {
    InputStream actIn = fact.createFilteredInputStream(mau,
        new StringInputStream(subCite), Constants.DEFAULT_ENCODING);
    assertEquals(subCiteFiltered, StringUtil.fromInputStream(actIn));
  }
  public void testDataFiltering() throws Exception {
    InputStream actIn = fact.createFilteredInputStream(mau,
        new StringInputStream(scData), Constants.DEFAULT_ENCODING);
    assertEquals(scDataFiltered, StringUtil.fromInputStream(actIn));
  }
  public void testHeadFiltering() throws Exception {
    InputStream actIn = fact.createFilteredInputStream(mau,
        new StringInputStream(head), Constants.DEFAULT_ENCODING);
    assertEquals(headFiltered, StringUtil.fromInputStream(actIn));
  }

}
