/*
 * $Id$
 */
/*

 Copyright (c) 2000-2012 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.plugin.atypon.aiaa;

import java.io.*;

import org.lockss.util.*;
import org.lockss.test.*;

public class TestAIAAHtmlHashFilterFactory extends LockssTestCase {
  static String ENC = Constants.DEFAULT_ENCODING;

  private AIAAHtmlHashFilterFactory fact;
  private MockArchivalUnit mau;

  public void setUp() throws Exception {
    super.setUp();
    fact = new AIAAHtmlHashFilterFactory();
    mau = new MockArchivalUnit();
  }

  /* cited by section */
  private static final String citedByHtml = 
      " <div id=\"articleContent\"> <p class=\"fulltext\"></p> <!-- abstract content -->" +
          " <h1 class=\"arttitle\"> Optimal, Environmentally Friendly Departure Procedures for Civil Aircraft</h1>" +
          " <div class=\"artAuthors\"> <span class=\"NLM_string-name\">R. Torres</span> <span class=\"NLM_x\">; </span> </div>" +
          "<div class=\"citedBySection\"> <a name=\"citedBySection\"></a>" +
          "<h2>Cited by</h2>" +
          "<div class=\"citedByEntry\">" +
          "<div>" +
          "<a class=\"entryAuthor\" href=\"/action/doSearch?action=%2Bauthorsfield%3A(Soler%2C+M.)\">" +
          "<span class=\"NLM_string-name\">M. Soler</span>" +
          "</a>" +
          " .  (2012) Framework for Aircraft Trajectory Planning Toward an Efficient Air Traffic Management." +
          " </div></div></div><!-- /fulltext content --></div>";

  private static final String citedByFiltered = 
      " <div id=\"articleContent\"> <p class=\"fulltext\"></p>" +
          " <h1 class=\"arttitle\"> Optimal, Environmentally Friendly Departure Procedures for Civil Aircraft</h1>" +
          " <div class=\"artAuthors\"> <span class=\"NLM_string-name\">R. Torres</span> <span class=\"NLM_x\">; </span> </div>" +
          "</div>";
      
  /* script call that generates institution specific thingie */
  private static final String scriptHtml = 
      "<table border=\"0\" width=\"100%\" class=\"articleEntry\">" +
          "<tr>" +
          "<td align=\"right\" valign=\"top\" width=\"18\" class=\"nowrap\">1</td>" +
          "<td valign=\"top\">" +
          "<div class=\"art_title\">Green Aviation Papers, Call and Recognition</div>" +
          "<br></br>" +
          "<script type=\"text/javascript\">genSfxLinks('s0', '', '10.2514/1.C031282');</script>" +
          "</td></tr></table>";
  private static final String scriptFiltered = 
      "<table border=\"0\" width=\"100%\" class=\"articleEntry\">" +
          "<tr>" +
          "<td align=\"right\" valign=\"top\" width=\"18\" class=\"nowrap\">1</td>" +
          "<td valign=\"top\">" +
          "<div class=\"art_title\">Green Aviation Papers, Call and Recognition</div>" +
          "<br></br>" +
          "</td></tr></table>";
  

  private static final String commentModified =
      "<input type=\"checkbox\" name=\"markall\" id=\"markall\" onclick=\"onClickMarkAll(frmAbs)\"/>" +
          "<label for=\"markall\">Select All</label>" +
          "<hr/>" +
          "<!--totalCount2--><!--modified:1364963594000--><table border=\"0\" width=\"100%\" class=\"articleEntry\">" +
          "<tr>" +
          "<td align=\"right\" valign=\"top\" width=\"18\"><input type=\"checkbox\" name=\"doi\" value=\"10.2514/1.46304\"></input><br></br>" +
          "<img src=\"/templates/jsp/images/access_full.gif\" alt=\"full access\" title=\"full access\" class=\"accessIcon\" />" +
          "</td><td align=\"right\" valign=\"top\" width=\"18\" class=\"nowrap\">391</td>" +
          "</tr></table>";
  
  private static final String commentModifiedFiltered = 
      "<input type=\"checkbox\" name=\"markall\" id=\"markall\" onclick=\"onClickMarkAll(frmAbs)\"/>" +
          "<label for=\"markall\">Select All</label>" +
          "<hr/>" +
          "<table border=\"0\" width=\"100%\" class=\"articleEntry\">" +
          "<tr>" +
          "<td align=\"right\" valign=\"top\" width=\"18\"><input type=\"checkbox\" name=\"doi\" value=\"10.2514/1.46304\"></input><br></br>" +
          "" +
          "</td><td align=\"right\" valign=\"top\" width=\"18\" class=\"nowrap\">391</td>" +
          "</tr></table>";
  
  private static final String spanIdHtml =
      "<div id=\"sec2.1\" class=\"NLM_sec NLM_sec_level_2\">" +
      "<span class=\"title2\" id=\"d55461e329\">" +
      "A. Fitting the Johnson Distribution to Point Data</span> " +
      "<p>The Johnson distribution</p>";
  
  private static final String spanIdHtmlFiltered =
      "<div id=\"sec2.1\" class=\"NLM_sec NLM_sec_level_2\">" +
      "<span class=\"title2\" >" +
      "A. Fitting the Johnson Distribution to Point Data</span> " +
      "<p>The Johnson distribution</p>";
  
  private static final String adHolderDiv = 
      "<div " +
          "id=\"widget-7132\" class=\"widget type-ad-placeholder ui-helper-clearfix\">" +
          "<div class=\"view\">" +
          "<div class=\"view-inner\">" +
          "<!-- placeholder id=null, description=Admin Message -->" +
          "</div>" +
          "</div>" +
          "</div>";
  private static final String adHolderDivFiltered = 
      "";
  
  private static final String listItemCitedBy =
      "<ul> <li class=\"articleToolLi showPDFPlus\">" +
          "<a href=\"/doi/pdfplus/10.2514/1.36414\" target=\"_blank\">" +
          "PDF Plus (724 KB)" +
          "</a>" +
          "</li>" +
          "<li>" +
          "<a href=\"/doi/abs/10.2514/1.36414#citedBySection\">Cited By</a>" +
          "</li>" +
          "</ul>";
  private static final String listItemCitedByFiltered =
      "<ul> <li class=\"articleToolLi showPDFPlus\">" +
          "<a href=\"/doi/pdfplus/10.2514/1.36414\" target=\"_blank\">" +
          "</a>" +
          "</li>" +
          "</ul>";
      
public static final String sectionsPulldown =  
  "<table width=\"50%\" cellpadding=\"0\" cellspacing=\"0\"><tr>" +
  "<td nowrap=\"\" height=\"16\" width=\"40\" align=\"left\">" +
  "<span class=\"fulltext\">Sections:</span> </td>" +
  "<td nowrap=\"\" height=\"16\" width=\"92\" align=\"left\" valign=\"middle\">" +
  "<form style=\"margin-bottom:0\">" +
  "<select name=\"select23\" class=\"fulltextdd\" onChange=\"GoTo(this, 'self')\">" +
  "<option value=\"#\" selected=\"\">Choose</option>" +
  "<option value=\"#_i16\">References</option>" +
  "<option value=\"#citart1\">CITING ARTICLES</option></select></form>" +
  "</td></tr></table>";
public static final String sectionsPulldownFiltered =  
"<table width=\"50%\" cellpadding=\"0\" cellspacing=\"0\"><tr>" +
"<td nowrap=\"\" height=\"16\" width=\"40\" align=\"left\">" +
"<span class=\"fulltext\">Sections:</span> </td>" +
"<td nowrap=\"\" height=\"16\" width=\"92\" align=\"left\" valign=\"middle\">" +
"<form style=\"margin-bottom:0\">" +
"<select name=\"select23\" class=\"fulltextdd\" onChange=\"GoTo(this, 'self')\">" +
"<option value=\"#\" selected=\"\">Choose</option>" +
"<option value=\"#_i16\">References</option>" +
"</select></form>" +
"</td></tr></table>";

public static final String sectionHeading =
  "<div class=\"sectionHeadingContainer\"><div class=\"sectionHeadingContainer2\">" +
  "<table class=\"sectionHeading\" width=\"100%\" cellpadding=\"0\" cellspacing=\"0\"><tr>" +
  "<th align=\"left\" valign=\"middle\" width=\"95%\">I. Introduction</th>" +
  "<td nowrap=\"\" class=\"sectionHeading\" align=\"right\">" +
  "<form style=\"margin-bottom:0\">" +
  "<select name=\"select23\" class=\"fulltextdd\" onChange=\"GoTo(this, 'self')\">" +
  "<option value=\"\" selected=\"\">Choose</option>" +
  "<option value=\"#\">Top of page</option>" +
  "<option value=\"#_i16\">References</option>" +
  "<option value=\"#citart1\">CITING ARTICLES</option></select></form></td>" +
  "<td height=\"16\" width=\"16\" nowrap=\"\">" +
  "<a href=\"#_i3\">" +
  "<img src=\"/templates/jsp/images/arrow_down.gif\" alt=\"Next section\"></img>" +
  "</a></td></tr></table></div></div>";
public static final String sectionHeadingFiltered =
"<div class=\"sectionHeadingContainer\"><div class=\"sectionHeadingContainer2\">" +
"</div></div>";

  
  public void testFiltering() throws Exception {
    InputStream inA;


    inA = fact.createFilteredInputStream(mau, new StringInputStream(scriptHtml),
        ENC);
    assertEquals(scriptFiltered,StringUtil.fromInputStream(inA));
    
    inA = fact.createFilteredInputStream(mau, new StringInputStream(citedByHtml),
        ENC);
    assertEquals(citedByFiltered,StringUtil.fromInputStream(inA));
    
    inA = fact.createFilteredInputStream(mau, new StringInputStream(commentModified),
        ENC);
    assertEquals(commentModifiedFiltered,StringUtil.fromInputStream(inA));
    
    inA = fact.createFilteredInputStream(mau, new StringInputStream(spanIdHtml),
        ENC);
    assertEquals(spanIdHtmlFiltered,StringUtil.fromInputStream(inA));
    
    inA = fact.createFilteredInputStream(mau, new StringInputStream(adHolderDiv),
        ENC);
    assertEquals(adHolderDivFiltered,StringUtil.fromInputStream(inA));

    inA = fact.createFilteredInputStream(mau, new StringInputStream(listItemCitedBy),
        ENC);
    assertEquals(listItemCitedByFiltered,StringUtil.fromInputStream(inA));

    inA = fact.createFilteredInputStream(mau, new StringInputStream(sectionsPulldown),
        ENC);
    assertEquals(sectionsPulldownFiltered,StringUtil.fromInputStream(inA));

    inA = fact.createFilteredInputStream(mau, new StringInputStream(sectionHeading),
        ENC);
    assertEquals(sectionHeadingFiltered,StringUtil.fromInputStream(inA));
 
  }
}