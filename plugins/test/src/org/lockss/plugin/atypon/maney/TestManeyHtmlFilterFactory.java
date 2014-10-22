/*
 * $Id: TestManeyHtmlFilterFactory.java,v 1.5 2014-10-22 21:38:58 thib_gc Exp $
 */

/*

Copyright (c) 2000-2014 Board of Trustees of Leland Stanford Jr. University,
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

/*
 * This test file tests both the hash and crawl filters  - two subclasses within
 * the larger test so they can share some html strings.
 */

package org.lockss.plugin.atypon.maney;

import junit.framework.Test;

import org.apache.commons.lang.RandomStringUtils;
import org.lockss.util.*;
import org.lockss.plugin.FilterFactory;
import org.lockss.test.*;

public class TestManeyHtmlFilterFactory extends LockssTestCase {

  public FilterFactory fact;

  //private static MockArchivalUnit mau;

  /*
   * SHARED HTML TEST SNIPPETS
   */

  private static final String headerHtml =
      "<!DOCTYPE html>" +
          "<html lang=\"en\" class=\"pb-page\">" +
          "<head data-pb-dropzone=\"head\">" +
          "<link rel=\"schema.DC\" href=\"http://purl.org/DC/elements/1.0/\"></link>" +
          "<meta name=\"dc.Title\" content=\"Family Foo\"></meta>" +
          "</head>" +
          "<body>" +
          "</body>" +
          "</html>";
  private static final String headerHtmlFiltered =
      "<!DOCTYPE html>" +
          "<html lang=\"en\" class=\"pb-page\">" +
          "<body>" +
          "</body>" +
          "</html>";

  private static final String pageHeaderHtml =
      "<foo>" +
          "<section class=\"widget pageHeader none  widget-none  widget-compact\" id=\"pageHeader\">" +
          "<div class=\"page-header\">" +
          "<div data-pb-dropzone=\"main\">" +
          "    <section class=\"widget general-image none  widget-none  widget-compact\" id=\"ec8b7042-594c-48ae-b50a-619a7c70b9b6\">" +
          "    <div class=\"wrapped 1_12\" >" +
          "    <div class=\"widget-body body body-none  body-compact\">" +
          "    <a href=\"/\" <img src=\"/pb/assets/raw/Maney_logo.jpg\"/></a>" +
          "    </div>" +
          "    </div>" +
          "    </section>" +
          "</div>" +
          "</div>" +
          "</section>" +
          "</foo>";
  private static final String pageHeaderHtmlFiltered =
      "<foo>" +
          "</foo>";


  private static final String rc_journalHeader = 
      "<foo>" +
          "<section class=\"widget layout-one-column widget-box\" id=\"fe7fba8b-1481-4cd3-b301-70b4116a3098\">" +
          "    <div class=\"wrapped \" id='compactJournalHeader'>" +
          "        <div data-pb-dropzone=\"center\">" +
          "<section class=\"widget general-heading none  widget-none  widget-compact\" id=\"1efeb7df-3cd9-4ff8-8c80-204ffac0ac8c\">" +
          "    <div class=\"wrapped 1_12\" >" +
          "        <div class=\"widget-body body body-none  body-compact\"><div class=\"page-heading\">" +
          "    <h1>The Title</h1>" +
          "</div></div>" +
          "</section>" +
          "    </div>" +
          "</div>" +
          "</section>" +
          "</foo>";
  private static final String rc_journalHeaderFiltered = 
      "<foo>" +
          "<section class=\"widget layout-one-column widget-box\" id=\"fe7fba8b-1481-4cd3-b301-70b4116a3098\">" +
          " " +
          "</section>" +
          "</foo>";


  private static final String rc_journalTools =
      "<foo>" +
          "<section class=\"widget general-html   widget-titlebar\" id=\"migrated_information\">" +
          "    <div class=\"wrapped \" >" +
          "        <header class=\"widget-header header-titlebar \">Journal services</header>" +
          "        <div class=\"widget-body body body-titlebar \">" +
          "<div class='row-fluid'>" +
          "<div class='width_1_2'><ul class='decoratedLinks'><li>" +
          "<a href='/pricing/dei'>Subscriptions</a></li><li>" +
          "<a href='/pricing/dei'>Access options</a></li><li>" +
          "<a href='/action/recommendation'>Recommend</a></li><li>" +
          "<a href='/advertising/dei'>Advertising</a></li></ul></div>" +
          "<div class='width_1_2'><ul class='decoratedLinks'><li>" +
          "<a href='/bibliometrics/dei'>Bibliometrics</a></li><li>" +
          "<a href='/page/redirect/backissues'>Back issues</a></li><li>" +
          "<a href='/page/redirect/permissions'>Permissions</a></li><li>" +
          "<a href='/page/redirect/reprints'>Reprints</a></li></ul>" +
          "</div></div></div>" +
          "    </div>" +
          "</section>" +
          "</foo>";
  private static final String rc_journalToolsFiltered =
      "<foo>" +
          "</foo>";

  private static final String rc_articleTools =  
      "<foo>" +
          "<section class=\"widget literatumArticleToolsWidget none  widget-titlebar\" id=\"51c44ff9-94cd-4ee4-ac3f-75db553aeb94\">" +
          "    <div class=\"wrapped 1_12\" >" +
          "        <header class=\"widget-header header-titlebar \">Article tools</header>" +
          "        <div class=\"widget-body body body-titlebar \"><div class=\"articleTools\">" +
          "<ul class=\"linkList blockLinks separators centered\">" +
          "        <li class=\"addToFavs\">" +
          "                <a href=\"blah\">Add to favourites</a>" +
          "        </li>" +
          "        <li class=\"downloadCitations\">" +
          "                <a href=\"/foo\">Export citation</a>" +
          "        </li>" +
          "</ul>" +
          "</div></div>" +
          "    </div>" +
          "</section>" +
          "</foo>";
  private static final String rc_articleToolsFiltered =  
      "<foo>" +
          "</foo>";


  private static final String rc_forAuthors = 
      "<foo>" +
          "<section class=\"widget general-html   widget-titlebar\" id=\"migrated_forauthors\">" +
          "<div class=\"wrapped \" >" +
          "<header class=\"widget-header header-titlebar \">For authors</header>" +
          "<div class=\"widget-body body body-titlebar \">" +
          "<ul class='decoratedLinks'>" +
          "<li><a href='/ifa/dei'>Instructions for authors</a></li>" +
          "<li><a href='http://www.edmgr.com/dei'>Submit a paper</a></li>" +
          "<li><a href='/page/redirect/authorresources'>Author resources</a></li>" +
          "</ul>" +
          "</div>" +
          "</div>" +
          "</section>" +
          "</foo>";
  private static final String rc_forAuthorsFiltered = 
      "<foo>" +
          "</foo>";


  private static final String rc_relatedSearch =
      "<foo>" +
          "<section class=\"widget literatumRelatedContentSearch none widget-box widget-titlebar  widget-border-toggle\" id=\"6cb38a5a-5c24-4fa0-9eff-c6c1c8afdaa9\">" +
          "<header class=\"widget-header header-titlebar  header-border-toggle\">Related content search</header>" +
          "<div class=\"widget-body body body-titlebar  body-border-toggle\"><div class=\"relatedContentForm\">" +
          "<form action=\"/action/searchDispatcher\" name=\"relatedSearchForm\" method=\"post\">" +
          "<div class=\"relatedAuthors relatedTerms\">" +
          "<span>By Author</span>" +
          "<ul>" +
          "<li><input type=\"checkbox\" name=\"Contrib\" value=\"ZaidmanZait, A\" /> <label>Blah</label></li>" +
          "</ul>" +
          "</div>" +
          "<input class=\"searchButtons\" type=\"submit\" value=\"Search\" />" +
          "</form>" +
          "</div></div>" +
          "</section>" +
          "</foo>";
  private static final String rc_relatedSearchFiltered =
      "<foo>" +
          "</foo>";

  private static final String pageFooterHtml =
      "<section class=\"widget pageFooter none  widget-none  widget-compact\" id=\"pageFooter\">" +
          "    <div class=\"widget-body body body-none  body-compact\">" +
          "    <div class=\"page-footer\">" +
          "    <section class=\"widget layout-three-columns  footerLinks widget-none  widget-compact\" id=\"6745921b-9c3c-4d19-a15e-ba80e0bb307a\">" +
          "    <div class=\"wrapped \" >" +
          "    <div class=\"widget-body body body-none  body-compact\"><ul>" +
          "    <li><a href=\"http://maneypublishing.com/index.php/about_maney/\">About Maney</a></li>" +
          "    <li><a href=\"http://maneypublishing.com/index.php/books \">Books</a></li>" +
          "    <li><a href=\"/page/help\">Help & FAQs</a></li>" +
          "    <li><a href=\"http://maneypublishing.com/index.php/resources/press_room/\">Press room</a></li>" +
          "    <li><a href=\"/page/contact\">Contact us</a></li>" +
          "    </ul><" +
          "    </div>" +
          "    </div>" +
          "    </section>" +
          "    </div>" +
          "    </div>" +
          "</section>";
  private static final String pageFooterHtmlFiltered =
      "";
  
  private static final String tocHeader =
  "<foo>" +
  "<section class=\"widget layout-one-column  journalHeader widget-box\" id=\"ea51d700-4847-46d6-9ed6-e660ccea35f3\">" +
  "    <div class=\"wrapped \" id='Journal Header'>" +
  "        <div class=\"widget-body body body-box \"><div class=\"pb-columns row-fluid\"> " +
  "<section class=\"widget layout-two-columns   widget-none  widget-compact\" id=\"d2cb0a48-9d3e-4409-92a2-ccc630131ff9\">" +                                             
  "        <div data-pb-dropzone=\"left\" >" +                                                                                                                            
  "<a href=\"/loi/dei\">" +                                                                                                                                               
  "<img src=\"/pb/assets/raw/Health%20Sciences/Covershots/DEI-150px.jpg\"/>" +                                                                                            
  "</a></div>" +                                                                                                                                                          
  "</section>" +                                                                                                                                                          
  "    </div>" +                                                                                                                                                          
  "</div>" +                                                                                                                                                              
  "</section>" +                                                                                                                                                          
  "</foo>";  
  private static final String tocHeaderFiltered=
  "<foo>" +
  "<section class=\"widget layout-one-column journalHeader widget-box\" id=\"ea51d700-4847-46d6-9ed6-e660ccea35f3\">" +
  " " +                                                                                                                                                              
  "</section>" +                                                                                                                                                          
  "</foo>";     
  
  private static final String navHtml =
  "<foo>" +                                                                                                                                                               
  "<section class=\"widget literatumBookIssueNavigation none  widget-none\" id=\"8bc6a8c3-a165-412c-a7a3-261f51da7f43\">" +                                               
  "        <div class=\"widget-body body body-none \"><div class=\"pager issueBookNavPager\">" +                                                                          
  "    <table cellpadding=\"0\" cellspacing=\"0\" width=\"100%\">" +                                                                                                      
  "        <tr>" +                                                                                                                                                        
  "            <td class=\"journalNavLeftTd\">" +                                                                                                                         
  "                    <div class=\"prev placedLeft\">" +                                                                                                                 
  "                        <a href=\"/toc/dei/13/2\">" +                                                                                                                  
  "                            < PREV" +                                                                                                                                  
  "                        </a>" +                                                                                                                                        
  "                    </div>" +                                                                                                                                          
  "                " +                                                                                                                                                    
  "            </td>" +                                                                                                                                                   
  "        </tr>" +                                                                                                                                                       
  "    </table>" +                                                                                                                                                        
  "</div></div>" +                                                                                                                                                        
  "</section>" +                                                                                                                                                          
  "</foo>"; 
  private static final String navHtmlFiltered =
  "<foo>" +                                                                                                                                                                                                                                                                                                                      
  "</foo>";
  
  private static final String tocItem =
  "<foo>" +                                                                                                                                                               
  "<!--totalCount4--><!--modified:1399391675000--><h2 class=\"tocHeading\"><div class=\"subject\">EDITORIAL</div></h2>" +                                                 
  "<table border=\"0\" width=\"100%\" class=\"articleEntry\"><tr>" +                                                                                                      
  "<td class=\"accessIconContainer\"><div>" +                                                                                                                             
  "<img class=\"accessIcon fullAccess\" title=\"full access\" src=\"/tmp/access_full.gif\"></img>" +                                                                      
  "</div></td>" +                                                                                                                                                         
  "<td align=\"right\" valign=\"top\" width=\"10\">" +                                                                                                                    
  "<input class=\"tocToolCheckBox\" type=\"checkbox\" name=\"doi\" value=\"10.1179/foo\"></input><br></br></td>" +                                                        
  "<td valign=\"top\">" +                                                                                                                                                 
  "<div class=\"art_title noLink\">Editorial</div>" +                                                                                                                     
  "<div class=\"art_title linkable\"><a class=\"ref nowrap\" href=\"/doi/full/10.1179/foo\">Editorial</a></div>" +                                                        
  "<div class=\"art_meta citation\"><span class=\"issueInfo\">13 (3)</span>" +                                                                                            
  "<span class=\"articlePageRange\"><span class=\"issueInfoComma\">, </span>pp. 91-91</span></div>" +                                                                     
  "<div class=\"tocEPubDate\"><span class=\"maintextleft\">" +                                                                                                            
  "<span class=\"ePubDateLabel\"></span>April 19, 2013</span></div>" +                                                                                                    
  "<div class=\"tocArticleDoi\"><a href=\"http://dx.doi.org/10.1179/foo\">http://dx.doi.org/10.1179/foo</a></div>" +                                                      
  "<div class=\"tocListKeywords\"></div>" +                                                                                                                               
  "<div class=\"tocDeliverFormatsLinks\">" +                                                                                                                              
  "<a class=\"ref nowrap \" href=\"/doi/abs/10.1179/foo\">Citation</a> | " +                                                                                              
  "<a class=\"ref nowrap\" href=\"/doi/full/10.1179/foo\">Full Text</a> | " +                                                                                             
  "<a class=\"ref nowrap\" target=\"_blank\" title=\"Opens new window\" href=\"/doi/pdfplus/10.1179/foo\">PDF (30 KB)</a> | " +                                           
  "<a class=\"ref nowrap\" href=\"/doi/full/10.1179/foo#allImages\">Figures</a><span class=\"publicationRightLink\"> | " +                                                
  "<a class=\"ref rightsLink tocRightLink\" title=\"Opens new window\" href=\"javascript:RightslinkPopUp('foo')\"></a>" +                                                 
  "</span>" +                                                                                                                                                             
  "<div id=\"Absfoo\" class=\"previewViewSection tocPreview\">" +                                                                                                         
  "<p class=\"previewContent\"></p>" +                                                                                                                                    
  "</div></div></td>" +                                                                                                                                                   
  "</tr>" +                                                                                                                                                               
  "</table> " +                                                                                                                                                           
  "</foo>";  
  private static final String tocItemFiltered =
  "<foo>" +                                                                                                                                                               
  "<h2 class=\"tocHeading\"><div class=\"subject\">EDITORIAL</div></h2>" +                                                 
  "<table border=\"0\" width=\"100%\" class=\"articleEntry\"><tr>" +                                                                                                      
  "<td align=\"right\" valign=\"top\" width=\"10\">" +                                                                                                                    
  "<input class=\"tocToolCheckBox\" type=\"checkbox\" name=\"doi\" value=\"10.1179/foo\"></input><br></br></td>" +                                                        
  "<td valign=\"top\">" +                                                                                                                                                 
  "<div class=\"art_title noLink\">Editorial</div>" +                                                                                                                     
  "<div class=\"art_title linkable\"><a class=\"ref nowrap\" href=\"/doi/full/10.1179/foo\">Editorial</a></div>" +                                                        
  "<div class=\"art_meta citation\"><span class=\"issueInfo\">13 (3)</span>" +                                                                                            
  "<span class=\"articlePageRange\"><span class=\"issueInfoComma\">, </span>pp. 91-91</span></div>" +                                                                     
  "<div class=\"tocEPubDate\"><span class=\"maintextleft\">" +                                                                                                            
  "<span class=\"ePubDateLabel\"></span>April 19, 2013</span></div>" +                                                                                                    
  "<div class=\"tocArticleDoi\"><a href=\"http://dx.doi.org/10.1179/foo\">http://dx.doi.org/10.1179/foo</a></div>" +                                                      
  "<div class=\"tocListKeywords\"></div>" +                                                                                                                               
  "<div class=\"tocDeliverFormatsLinks\">" +                                                                                                                              
  "<a class=\"ref nowrap \" href=\"/doi/abs/10.1179/foo\">Citation</a> | " +                                                                                              
  "<a class=\"ref nowrap\" href=\"/doi/full/10.1179/foo\">Full Text</a> | " +                                                                                             
  "<a class=\"ref nowrap\" target=\"_blank\" title=\"Opens new window\" href=\"/doi/pdfplus/10.1179/foo\">PDF (30 KB)</a> | " +                                           
  "<a class=\"ref nowrap\" href=\"/doi/full/10.1179/foo#allImages\">Figures</a><span class=\"publicationRightLink\"> | " +                                                
  "<a class=\"ref rightsLink tocRightLink\" title=\"Opens new window\" href=\"javascript:RightslinkPopUp('foo')\"></a>" +                                                 
  "</span>" +                                                                                                                                                             
  "<div id=\"Absfoo\" class=\"previewViewSection tocPreview\">" +                                                                                                         
  "<p class=\"previewContent\"></p>" +                                                                                                                                    
  "</div></div></td>" +                                                                                                                                                   
  "</tr>" +                                                                                                                                                               
  "</table> " +                                                                                                                                                           
  "</foo>";  

  private static final String logoHtml =
  "<foo>" +                                                                                                                                                               
  "    <div class=\"wrapped \" id='Society Logo'>" +                                                                                                                      
  "        <header class=\"widget-header header-titlebar \">Affiliated with</header>" +                                                                                   
  "        <div class=\"widget-body body body-titlebar \">" +                                                                                                             
  "<div class=\"pb-columns row-fluid\">" +                                                                                                                                
  "<a href=\"http://www.batod.org.uk/\" title=\"British Association for Teachers of the Deaf\">" +                                                                        
  "<img src=\"/pb/assets/raw/Society%20logos/BATOD-75px.jpg\"" +                                                                                                          
  "alt=\"British Association for Teachers of the Deaf\"/>" +                                                                                                              
  "</a></div>" +                                                                                                                                                          
  "    </div>" +                                                                                                                                                          
  "</div>" +                                                                                                                                                              
  "</foo>"; 
  private static final String logoHtmlFiltered =
  "<foo>" +                                                                                                                                                               
  " " +                                                                                                                                                              
  "</foo>";   
  
  /* These next four are to be crawl filtered out - links from TOC and ARticle
   * page that go to/from correction/original article
   */
  private static final String toc_Original =
  "<foo correct>" +
  "<td valign=\"top\">" +
  "<div class=\"art_title noLink\">Corrigendum</div>" +
  "<div class=\"art_title linkable\">" +
  "<a class=\"ref nowrap\" href=\"/blah\">Corrigendum</a></div>" +
  "<div class=\"art_meta citation\">" +
  "<span class=\"issueInfo\">37 (1)</span>" +
  "<span class=\"articlePageRange\"><span class=\"issueInfoComma\">, </span>pp. 39-39</span></div><div class=\"tocEPubDate\">" +
  "<span class=\"maintextleft\">" +
  "<span class=\"ePubDateLabel\"></span>March 6, 2014</span></div><div class=\"tocArticleDoi\"></div>" +
  "<div class=\"tocDeliverFormatsLinks\">" +
  "<a class=\"ref nowrap \" href=\"/doi/abs/1\">Citation</a> | " +
  "</span>" +
  "<span class=\"linkDemarcator\"> | </span>" +
  "<a class=\"ref\" href=\"/blah\">Original Article</a><div id=\"Absblah\" class=\"previewViewSection tocPreview\">" +
  "</div></div></td>" +
  "</foo>";
  private static final String toc_OriginalFiltered =
  "<foo correct>" +
  "<td valign=\"top\">" +
  "<div class=\"art_title noLink\">Corrigendum</div>" +
  "<div class=\"art_title linkable\">" +
  "</div>" +
  "<div class=\"art_meta citation\">" +
  "<span class=\"issueInfo\">37 (1)</span>" +
  "<span class=\"articlePageRange\"><span class=\"issueInfoComma\">, </span>pp. 39-39</span></div><div class=\"tocEPubDate\">" +
  "<span class=\"maintextleft\">" +
  "<span class=\"ePubDateLabel\"></span>March 6, 2014</span></div><div class=\"tocArticleDoi\"></div>" +
  "<div class=\"tocDeliverFormatsLinks\">" +
  "<a class=\"ref nowrap \" href=\"/doi/abs/1\">Citation</a> | " +
  "</span>" +
  "<span class=\"linkDemarcator\"> | </span>" +
  "<div id=\"Absblah\" class=\"previewViewSection tocPreview\">" +
  "</div></div></td>" +
  "</foo>";

  private static final String art_Original =
  "    <div class=\"contentLink correctionLink margin_left10 contentLinkText\">" +
  "       <a href=\"/doi/full/10.1179/2046023611y.0000000005\" class=\"defaultLinkDecoration\">" +
  "           Original Article&nbsp;" +
  "       </a>" +
  "    </div>";
  private static final String art_Correction =
  "    <div class=\"contentLink correctionLink margin_left10 contentLinkText\">" +
  "       <a href=\"/doi/full/10.1179/0147888514Z.00000000082\" class=\"defaultLinkDecoration\">" +
  "           View correction published for this article&nbsp;" +
  "       </a>" +
  "    </div>";
  private static final String art_LinkFiltered =
"    ";

  private static final String toc_Correction =
  "<td valign=\"top\">" +
  "<div class=\"art_meta citation\"><span class=\"issueInfo\">35 (1)</span>" +
  "<span class=\"articlePageRange\"><span class=\"issueInfoComma\">, </span>pp. 11-16</span></div>" +
  "<div class=\"tocEPubDate\"><span class=\"maintextleft\">" +
  "<span class=\"ePubDateLabel\"></span>November 12, 2013</span></div>" +
  "<div class=\"tocArticleDoi\"><a href=\"http://dx.doi.org/1\">http://dx.doi.org/1</a></div>" +
  "<div class=\"tocDeliverFormatsLinks\">" +
  "<a class=\"ref nowrap \" href=\"/doi/abs/1\">Abstract</a> | " +
  "<a class=\"ref nowrap\" href=\"/doi/abs/1\">Correction</a>" +
  "<span class=\"publicationRightLink\"> | " +
  "<div id=\"Abs1\" class=\"previewViewSection tocPreview\">" +
  "</div></div></td>";
  private static final String toc_CorrectionFiltered =
  "<td valign=\"top\">" +
  "<div class=\"art_meta citation\"><span class=\"issueInfo\">35 (1)</span>" +
  "<span class=\"articlePageRange\"><span class=\"issueInfoComma\">, </span>pp. 11-16</span></div>" +
  "<div class=\"tocEPubDate\"><span class=\"maintextleft\">" +
  "<span class=\"ePubDateLabel\"></span>November 12, 2013</span></div>" +
  "<div class=\"tocArticleDoi\"><a href=\"http://dx.doi.org/1\">http://dx.doi.org/1</a></div>" +
  "<div class=\"tocDeliverFormatsLinks\">" +
  "<a class=\"ref nowrap \" href=\"/doi/abs/1\">Abstract</a> | " +
  "<span class=\"publicationRightLink\"> | " +
  "<div id=\"Abs1\" class=\"previewViewSection tocPreview\">" +
  "</div></div></td>";
  
  private static final String accessControlsList = 
  "<div class=\"widget-body body body-none \"><ul class=\"horz-list float-right access-options\">" +
  "<li>" +
  "<img src=\"/templates/jsp/_style2/_maney/images/access_full.gif\">" +
  "<span>Full access</span>" +
  "</li><li><img src=\"/templates/jsp/_style2/_maney/images/openAccess.png\">" +
  "<span>Open access</span>" +
  "</li>" +
  "</ul></div>";
  private static final String accessControlsListFiltered = 
  "<div class=\"widget-body body body-none \"></div>";
  
  private static final String tabsHtml =
      "<section class=\"widget layout-tabs none  widget-none\" id=\"b4b69001-859b-4295-bc9a-b4dac496fa36\">" +
          "  <div class=\"\" data-pb-dropzone=\"tab-58132d06-cf2f-4e31-a696-f4f2aa0cdd9a\" data-pb-dropzone-name=\"Most &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;<br />read\">" +
          "  <section class=\"widget general-rich-text   widget-none\" id=\"d13c3a52-1150-4806-af2e-9e897bf1c5c2\">" +
          "    <div class=\"widget-body body body-none \">" +
          "    <p>The most frequently downloaded articles</p>" +
          "    </div>" +
          "  </section>" +
          "  <section class=\"widget literatumMostReadWidget none  widget-none\" id=\"14bb26fc-aa97-437b-a0b6-0d97b9b45396\">" +
          "  <a href=\"/action/showMostReadArticles?journalCode=dei\">See more ></a>" +
          "  </section>" +
          "  </div>" +
          "  <div class=\"\" data-pb-dropzone=\"tab-b6de7b7c-de82-45a5-9538-313dd15c6659\" data-pb-dropzone-name=\"Most &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;<br />cited\">" +
          "  <section class=\"widget general-rich-text   widget-none\" id=\"40d3ff66-4e32-4c86-b82c-cc5b0842c50e\">" +
          "    <div class=\"widget-body body body-none \">" +
          "    <p>The most frequently cited articles</p>" +
          "    </div>" +
          "  </section>" +
          "  <section class=\"widget literatumMostCitedWidget none  widget-none\" id=\"9510437b-5508-4e80-91ec-1b7b8c604584\">" +
          "  <a href=\"/action/showMostCitedArticles?journalCode=dei\">See more ></a>" +
          "  </section>" +
          "  </div>" +
          "  <div class=\"\" data-pb-dropzone=\"tab-bf403da7-9e3a-4c20-8d3f-d0fa90626a9b\" data-pb-dropzone-name=\"Editors'<br />Choice &nbsp; \">" +
          "  <section class=\"widget general-rich-text   widget-none\" id=\"da318547-57d8-4836-8b08-fdeabe57ec80\">" +
          "    <div class=\"widget-body body body-none \">" +
          "    <p>The Editors' selection</p>" +
          "    </div>" +
          "  </section>" +
          "  <section class=\"widget publicationListWidget none  widget-none  widget-compact\" id=\"071e3c44-fb81-4464-bc66-69cc7bc444fa\">" +
          "    <div>" +
          "    Foo" +
          "    </div>" +
          "  </section>" +
          "  </div>" +
          "</section>";

  private static final String tabsHtmlFiltered = 
      "<section class=\"widget layout-tabs none widget-none\" id=\"b4b69001-859b-4295-bc9a-b4dac496fa36\">" +
         " <div class=\"\" data-pb-dropzone=\"tab-58132d06-cf2f-4e31-a696-f4f2aa0cdd9a\" data-pb-dropzone-name=\"Most &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;<br />read\">" +
          " <section class=\"widget general-rich-text widget-none\" id=\"d13c3a52-1150-4806-af2e-9e897bf1c5c2\">" +
          " <div class=\"widget-body body body-none \">" +
          " <p>The most frequently downloaded articles</p>" +
          " </div>" +
          " </section>" +
          " </div>" +
          " <div class=\"\" data-pb-dropzone=\"tab-b6de7b7c-de82-45a5-9538-313dd15c6659\" data-pb-dropzone-name=\"Most &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;<br />cited\">" +
          " <section class=\"widget general-rich-text widget-none\" id=\"40d3ff66-4e32-4c86-b82c-cc5b0842c50e\">" +
          " <div class=\"widget-body body body-none \">" +
          " <p>The most frequently cited articles</p>" +
          " </div>" +
          " </section>" +
          " </div>" +
          " <div class=\"\" data-pb-dropzone=\"tab-bf403da7-9e3a-4c20-8d3f-d0fa90626a9b\" data-pb-dropzone-name=\"Editors'<br />Choice &nbsp; \">" +
          " <section class=\"widget general-rich-text widget-none\" id=\"da318547-57d8-4836-8b08-fdeabe57ec80\">" +
          " <div class=\"widget-body body body-none \">" +
          " <p>The Editors' selection</p>" +
          " </div>" +
          " </section>" +
          " </div>" +
          "</section>";
  
  private static final String tabsHtmlCrawlFiltered = 
      "<section class=\"widget layout-tabs none  widget-none\" id=\"b4b69001-859b-4295-bc9a-b4dac496fa36\">" +
          "  <div class=\"\" data-pb-dropzone=\"tab-58132d06-cf2f-4e31-a696-f4f2aa0cdd9a\" data-pb-dropzone-name=\"Most &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;<br />read\">" +
          "  <section class=\"widget general-rich-text   widget-none\" id=\"d13c3a52-1150-4806-af2e-9e897bf1c5c2\">" +
          "    <div class=\"widget-body body body-none \">" +
          "    <p>The most frequently downloaded articles</p>" +
          "    </div>" +
          "  </section>" +
          "  " +
          "  </div>" +
          "  <div class=\"\" data-pb-dropzone=\"tab-b6de7b7c-de82-45a5-9538-313dd15c6659\" data-pb-dropzone-name=\"Most &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;<br />cited\">" +
          "  <section class=\"widget general-rich-text   widget-none\" id=\"40d3ff66-4e32-4c86-b82c-cc5b0842c50e\">" +
          "    <div class=\"widget-body body body-none \">" +
          "    <p>The most frequently cited articles</p>" +
          "    </div>" +
          "  </section>" +
          "  " +
          "  </div>" +
          "  <div class=\"\" data-pb-dropzone=\"tab-bf403da7-9e3a-4c20-8d3f-d0fa90626a9b\" data-pb-dropzone-name=\"Editors'<br />Choice &nbsp; \">" +
          "  <section class=\"widget general-rich-text   widget-none\" id=\"da318547-57d8-4836-8b08-fdeabe57ec80\">" +
          "    <div class=\"widget-body body body-none \">" +
          "    <p>The Editors' selection</p>" +
          "    </div>" +
          "  </section>" +
          "  " +
          "  </div>" +
          "</section>";



  /**
   * CRAWL FILTER VARIANT
   */
  public static class TestCrawl extends TestManeyHtmlFilterFactory {


    public void setUp() throws Exception {
      super.setUp();
      fact = new ManeyAtyponHtmlCrawlFilterFactory();
    }

    public void testCorrections() throws Exception {

      assertEquals(toc_CorrectionFiltered,
          StringUtil.fromInputStream(fact.createFilteredInputStream(null,
              new StringInputStream(toc_Correction),
              Constants.DEFAULT_ENCODING)));
      assertEquals(toc_OriginalFiltered,
          StringUtil.fromInputStream(fact.createFilteredInputStream(null,
              new StringInputStream(toc_Original),
              Constants.DEFAULT_ENCODING)));
      assertEquals(art_LinkFiltered,
          StringUtil.fromInputStream(fact.createFilteredInputStream(null,
              new StringInputStream(art_Correction),
              Constants.DEFAULT_ENCODING)));
      assertEquals(art_LinkFiltered,
          StringUtil.fromInputStream(fact.createFilteredInputStream(null,
              new StringInputStream(art_Original),
              Constants.DEFAULT_ENCODING)));
      assertEquals(tabsHtmlCrawlFiltered,
          StringUtil.fromInputStream(fact.createFilteredInputStream(null,
              new StringInputStream(tabsHtml),
              Constants.DEFAULT_ENCODING)));
      
      /* and make sure we *don't* crawl filter out citation link */
      /* test that the filter keeps the entire chunk intact */
      assertEquals(rc_articleTools,
          StringUtil.fromInputStream(fact.createFilteredInputStream(null,
              new StringInputStream(rc_articleTools),
              Constants.DEFAULT_ENCODING)));

    }


  }

  /**
   * HASH FILTER VARIANT
   */
  public static class TestHash extends TestManeyHtmlFilterFactory {

    public void setUp() throws Exception {
      super.setUp();
      fact = new ManeyAtyponHtmlHashFilterFactory();
    }

    public void testArticlePageHash() throws Exception {

      assertEquals(headerHtmlFiltered,
          StringUtil.fromInputStream(fact.createFilteredInputStream(null,
              new StringInputStream(headerHtml),
              Constants.DEFAULT_ENCODING)));
      assertEquals(pageHeaderHtmlFiltered,
          StringUtil.fromInputStream(fact.createFilteredInputStream(null,
              new StringInputStream(pageHeaderHtml),
              Constants.DEFAULT_ENCODING)));
      assertEquals(rc_journalHeaderFiltered,
          StringUtil.fromInputStream(fact.createFilteredInputStream(null,
              new StringInputStream(rc_journalHeader),
              Constants.DEFAULT_ENCODING)));
      assertEquals(rc_journalToolsFiltered,
          StringUtil.fromInputStream(fact.createFilteredInputStream(null,
              new StringInputStream(rc_journalTools),
              Constants.DEFAULT_ENCODING)));
      assertEquals(rc_articleToolsFiltered,
          StringUtil.fromInputStream(fact.createFilteredInputStream(null,
              new StringInputStream(rc_articleTools),
              Constants.DEFAULT_ENCODING)));
      assertEquals(rc_forAuthorsFiltered,
          StringUtil.fromInputStream(fact.createFilteredInputStream(null,
              new StringInputStream(rc_forAuthors),
              Constants.DEFAULT_ENCODING)));
      assertEquals(rc_relatedSearchFiltered,
          StringUtil.fromInputStream(fact.createFilteredInputStream(null,
              new StringInputStream(rc_relatedSearch),
              Constants.DEFAULT_ENCODING)));
      assertEquals(pageFooterHtmlFiltered,
          StringUtil.fromInputStream(fact.createFilteredInputStream(null,
              new StringInputStream(pageFooterHtml),
              Constants.DEFAULT_ENCODING)));
      assertEquals(tabsHtmlFiltered,
          StringUtil.fromInputStream(fact.createFilteredInputStream(null,
              new StringInputStream(tabsHtml),
              Constants.DEFAULT_ENCODING)));
 
    }
    
    public void testTOCPageHash() throws Exception {

      assertEquals(tocHeaderFiltered,
          StringUtil.fromInputStream(fact.createFilteredInputStream(null,
              new StringInputStream(tocHeader),
              Constants.DEFAULT_ENCODING)));
      assertEquals(navHtmlFiltered,
          StringUtil.fromInputStream(fact.createFilteredInputStream(null,
              new StringInputStream(navHtml),
              Constants.DEFAULT_ENCODING)));
      assertEquals(tocItemFiltered,
          StringUtil.fromInputStream(fact.createFilteredInputStream(null,
              new StringInputStream(tocItem),
              Constants.DEFAULT_ENCODING)));
      assertEquals(logoHtmlFiltered,
          StringUtil.fromInputStream(fact.createFilteredInputStream(null,
              new StringInputStream(logoHtml),
              Constants.DEFAULT_ENCODING)));
      assertEquals(accessControlsListFiltered,
          StringUtil.fromInputStream(fact.createFilteredInputStream(null,
              new StringInputStream(accessControlsList),
              Constants.DEFAULT_ENCODING)));
    }


  }

  public static Test suite() {
    return variantSuites(new Class[] {
        TestCrawl.class,
        TestHash.class
    });
  }

  protected static String rand() {
    return RandomStringUtils.randomAlphabetic(30);
  }

}
