/*

Copyright (c) 2000-2022, Board of Trustees of Leland Stanford Jr. University

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

package org.lockss.plugin.usdocspln.gov.gpo.fdsys;

import org.lockss.util.*;
import org.lockss.test.*;

public class TestGPOFDSysHtmlFilterFactory extends LockssTestCase {
  
  private static final String ENC = Constants.DEFAULT_ENCODING;

  private GPOFDSysHtmlFilterFactory fact;

  public void setUp() throws Exception {
    super.setUp();
    fact = new GPOFDSysHtmlFilterFactory();
  }

  public void testHead() throws Exception {
    doTestString("",
                 "<head profile=\"http://www.w3.org/2005/10/profile\">\n" +
                 "       <base href=\"http://www.gpo.gov/fdsys/\" />\n" +
                 "       \n" +
                 "       <meta http-equiv=\"Content-Type\" content=\"text/html; charset=utf-8\" />\n" +
                 "       <meta http-equiv=\"Content-Script-Type\" content=\"text/javascript\" />        \n" +
                 "       <meta http-equiv=\"pragma\" content=\"no-cache\" />\n" +
                 "       <meta http-equiv=\"cache-control\" content=\"no-cache\" />\n" +
                 "       <meta http-equiv=\"expires\" content=\"0\" />     \n" +
                 "       <meta http-equiv=\"keywords\" content=\"\n" +
                 "               Senate Calendars, FINAL - Senate Calendars, Final - 105th Congress, 2nd Session\n" +
                 "       \" />\n" +
                 "       <meta http-equiv=\"description\" content=\"\n" +
                 "               Senate Calendars, FINAL - Senate Calendars, Final - 105th Congress, 2nd Session\n" +
                 "       \" /> \n" +
                 "       \n" +
                 "       <script type=\"text/javascript\">\n" +
                 "       \n" +
                 "       var WT_SERVER = '162.140.239.17';\n" +
                 "       var WT_DCS_ID = 'dcsjsw8h600000gotf0vyrmly_2j7v';\n" +
                 "       var WT_HOST_SERVER_NAME = 'http://www.gpo.gov/fdsys';\n" +
                 "       \n" +
                 "       var BASE_PRESENTATION_PATH = 'http://www.gpo.gov/fdsys/';\n" +
                 "       var BASE_USER_HELP = 'http://www.gpo.gov/help/';        \n" +
                 "       \n" +
                 "               \n" +
                 "                var SEARCHWEBAPP_BUILD = 'FDSYS_R2_28';\n" +
                 "                               \n" +
                 "                               var coopValue='';\n" +
                 "\n" +
                 "    </script>\n" +
                 "       <script type=\"text/javascript\" src=\"http://www.gpo.gov/scripts/gpo.js\"></script>    \n" +
                 "       <script type=\"text/javascript\" src=\"scripts/fdsys.js\"></script>             \n" +
                 "       <script type=\"text/javascript\" src=\"scripts/mktree.js\"></script>\n" +
                 "       <title>\n" +
                 "               Senate Calendars, FINAL - Senate Calendars, Final - 105th Congress, 2nd Session\n" +
                 "       </title>\n" +
                 "       \n" +
                 "               <style type=\"text/css\">\n" +
                 "                       #left-menu-sub-search {\n" +
                 "                               color: #000000;\n" +
                 "                       }\n" +
                 "               </style>                \n" +
                 "       \n" +
                 "\n" +
                 "       <style type=\"text/css\">\n" +
                 "               @import url(http://www.gpo.gov/styles/fdsyspages.css);\n" +
                 "               @import url(http://www.gpo.gov/styles/gpo.css);\n" +
                 "       </style>        \n" +
                 "       <link rel=\"icon\" type=\"image/gif\" href=\"images/gpo_favicon.gif\" />\n" +
                 "</head>");
  }

  public void testScript() throws Exception {
    doTestString("",
                 "<script type=\"text/javascript\">createSkipLink();</script>");
  }
  
  public void testTopMenuOne() throws Exception {
    doTestString("",
                 "<div class=\"top-menu\" id=\"top-menu-one\">\n" +
                 "       <a href=\"/about/\">About GPO</a>\n" +
                 "       &nbsp;\n" +
                 "       <span class=\"top-menu-pipe\">|</span> \n" +
                 "       &nbsp; \n" +
                 "       <a href=\"/newsroom-media/\">Newsroom/Media</a>\n" +
                 "       &nbsp;\n" +
                 "       <span class=\"top-menu-pipe\">|</span> \n" +
                 "       &nbsp;\n" +
                 "       <a href=\"/congressional/\">Congressional Relations</a>\n" +
                 "       &nbsp;\n" +
                 "       <span class=\"top-menu-pipe\">|</span> \n" +
                 "       &nbsp;\n" +
                 "       <a href=\"/oig/\">Inspector General</a>\n" +
                 "       &nbsp;\n" +
                 "       <span class=\"top-menu-pipe\">|</span> \n" +
                 "       &nbsp;\n" +
                 "       <a href=\"/careers/\">Careers</a>\n" +
                 "       &nbsp;\n" +
                 "       <span class=\"top-menu-pipe\">|</span> \n" +
                 "       &nbsp;\n" +
                 "       <a href=\"http://www.gpo.gov/contact.htm\">Contact</a>\n" +
                 "       &nbsp;\n" +
                 "       <span class=\"top-menu-pipe\">|</span> \n" +
                 "       &nbsp;\n" +
                 "       <a href=\"/askgpo/\">askGPO</a>\n" +
                 "       &nbsp;\n" +
                 "<span class=\"top-menu-pipe\">|</span> \n" +
                 "&nbsp;\n" +
                 "<a href=\"/help/index.html#about_fdsys2.htm\" target=\"_blank\">Help</a>\n" +
                 "&nbsp; \n" +
                 "   \n" +
                 " \n" +
                 "</div>");
  }
  
  public void testEmailLink() throws Exception {
    doTestString("",
                 "<a href=\"search/notificationPage.action?emailBody=http%3A%2F%2Fwww.gpo.gov%3A80%2Ffdsys%2Fpkg%2FCCAL-105scal-S2%2Fcontent-detail.html%3Fnull\">\n" +
                 "                            Email a link to this page\n" +
                 "                        </a>");
  }
  
  public void testFormRegex() throws Exception {
    doTestString("",
                 "<form id=\"searchresults\" onsubmit=\"return true;\" action=\"/fdsys/search/searchresults.action;jsessionid=8kDQQXcVX9ncp6rV766Jv0Cq3zhhNCZvQv36DyQBqvknxS1gtYkk!-1331353267!208169223\" method=\"get\" class=\"inline\">\n" +
                 "\n" +
                 "                                                       \n" +
                 "\n" +
                 "\n" +
                 "\n" +
                 "\n" +
                 "\n" +
                 "\n" +
                 "\n" +
                 " \n" +
                 "\n" +
                 " \n" +
                 "\n" +
                 "<table> \n" +
                 "       <tr>\n" +
                 "               <td colspan=\"2\">\n" +
                 "                       <label for=\"basic-search-box\" id=\"basic-search-box-label\">\n" +
                 "                               Search Government Publications\n" +
                 "                       </label>\n" +
                 "                       <span id=\"show-basic-search-error\" class=\"errorMessage\">\n" +
                 "                               (Search string is required)\n" +
                 "                       </span>\n" +
                 "               </td>\n" +
                 "               <td rowspan=\"2\" id=\"basic-search-table-right-td\"> \n" +
                 "                       \n" +
                 "                       <div><a href=\"search/advanced/advsearchpage.action\" class=\"nowrap\">Advanced Search</a></div>\n" +
                 "                       \n" +
                 "                       \n" +
                 "                       \n" +
                 "                       <div><a href=\"search/showcitation.action\">Retrieve by Citation</a></div>\n" +
                 "                       \n" +
                 "                       \n" +
                 "                       \n" +
                 "                       <div><a href=\"javascript:popUpHelp(BASE_USER_HELP + helpLinks.simpleSearch );\" class=\"nowrap\">Help</a></div>\n" +
                 "                       \n" +
                 "               </td>\n" +
                 "       </tr>\n" +
                 "       <tr>\n" +
                 "               <td style=\"vertical-align: bottom;\" class=\"basic-search-td-right\">\n" +
                 "                       <div id=\"wwgrp_basic-search-box\" class=\"wwgrp\">\n" +
                 "<div id=\"wwctrl_basic-search-box\" class=\"wwctrl\">\n" +
                 "<input type=\"text\" name=\"st\" value=\"\" id=\"basic-search-box\"/></div> </div>\n" +
                 "\n" +
                 "               </td>\n" +
                 "               <td style=\"vertical-align: bottom;\" id=\"basic-search-table-btn-td\">                 \n" +
                 "                       \n" +
                 "                       <input id=\"searchid\" type=\"button\" class=\"btn-basic-search\" value=\"Search\" onclick=\"validateSimpleSearch(this.form);\" />\n" +
                 "                       \n" +
                 "               </td>\n" +
                 "       </tr>\n" +
                 "               \n" +
                 "       \n" +
                 "</table>\n" +
                 "                 \n" +
                 "                                               </form>");
  }
  
  public void testLinkNoFollow() throws Exception {
    doTestString("",
                 "<a href=\"delivery/getpackage.action?packageId=CCAL-105scal-S2\" onclick=\"logRetrievalStats(RETRIEVAL_TYPE_CONTENT_DETAIL,'CCAL',FILE_TYPE_ZIP,this.href,'Content Delivery Zip File')\">ZIP file</a>");
    doTestString("",
                 "<a rel=\"nofollow\" href=\"delivery/getpackage.action?packageId=CCAL-105scal-S2\" onclick=\"logRetrievalStats(RETRIEVAL_TYPE_CONTENT_DETAIL,'CCAL',FILE_TYPE_ZIP,this.href,'Content Delivery Zip File')\">ZIP file</a>");
    doTestString("",
                 "<a href=\"delivery/getpackage.action?packageId=CCAL-105scal-S2\" rel=\"nofollow\" onclick=\"logRetrievalStats(RETRIEVAL_TYPE_CONTENT_DETAIL,'CCAL',FILE_TYPE_ZIP,this.href,'Content Delivery Zip File')\">ZIP file</a>");
    doTestString("",
                 "<a href=\"delivery/getpackage.action?packageId=CCAL-105scal-S2\" onclick=\"logRetrievalStats(RETRIEVAL_TYPE_CONTENT_DETAIL,'CCAL',FILE_TYPE_ZIP,this.href,'Content Delivery Zip File')\" rel=\"nofollow\">ZIP file</a>");
  }
  
  public void testFilterRule() throws Exception {
    // Note the space due to the white space filter
    doTestString(" ",
                 "<!--<input type=\"hidden\" name=\"struts.token.name\" value=\"struts.token\" />\n" +
                 "<input type=\"hidden\" name=\"struts.token\" value=\"H34QZK9TF4N77V7LRD94SBULW6JZ9DPB\" />-->\n");
    doTestString(" ",
                 "<input type=\"hidden\" name=\"struts.token.name\" value=\"struts.token\" />\n" +
                 "<input type=\"hidden\" name=\"struts.token\" value=\"H34QZK9TF4N77V7LRD94SBULW6JZ9DPB\" />\n");
  }
  
  public void testShouldFilter() throws Exception {
    // From http://www.gpo.gov/fdsys/pkg/USCODE-2006-title42/html/USCODE-2006-title42-chap1-subchapI.htm
    assertTrue(fact.shouldFilter(new StringInputStream(
"<html><head><link rel=\"stylesheet\" type=\"text/css\" href=\"usc.css\"/><title>U.S.C. Title 42 - THE PUBLIC HEALTH AND WELFARE</title></html></head>\n" +
"<span style=\"font-weight:bold;font-size:12pt;\">42 U.S.C. </span><br/>\n" +
"<span style=\"font-size:10pt\">United States Code, 2006 Edition</span><br/>\n" +
"<span style=\"font-size:10pt\">Title 42 - THE PUBLIC HEALTH AND WELFARE</span><br/>\n" +
"<span style=\"font-size:10pt\">CHAPTER 1 - THE PUBLIC HEALTH SERVICE</span><br/>\n" +
"<span style=\"font-size:10pt\">SUBCHAPTER I - GENERALLY</span><br/>\n" +
"<span style=\"font-size:10pt\">From the U.S. Government Printing Office, <a href=\"http://www.gpo.gov\">www.gpo.gov</a></span><br/><br/>\n" +
"<!-- documentid:42_-ch1-scI currentthrough:20070103 documentPDFPage:3 -->\n" +
"<!-- itempath:/420/CHAPTER 1/SUBCHAPTER I -->\n" +
"<!-- itemsortkey:420AAAC -->\n" +
"<!-- expcite:TITLE 42-THE PUBLIC HEALTH AND WELFARE!@!CHAPTER 1-THE PUBLIC HEALTH SERVICE!@!SUBCHAPTER I-GENERALLY -->\n" +
"<!-- field-start:structuralhead -->\n" +
"<h3 class=\"subchapter-head\">SUBCHAPTER I&mdash;GENERALLY</h3>\n" +
"<!-- field-end:structuralhead -->\n" +
"\n" +
"<!-- documentid:42_1_to_1j  usckey:420000000000100000000000000000000 currentthrough:20070103 documentPDFPage:3 -->\n" +
"<!-- itempath:/420/CHAPTER 1/SUBCHAPTER I/Secs. 1 to 1j -->\n" +
"<!-- itemsortkey:420AAAD -->\n" +
"<!-- expcite:TITLE 42-THE PUBLIC HEALTH AND WELFARE!@!CHAPTER 1-THE PUBLIC HEALTH SERVICE!@!SUBCHAPTER I-GENERALLY!@!Secs. 1 to 1j -->\n" +
"<!-- field-start:repealedhead -->\n" +
"<h3 class=\"section-head\">&sect;&sect;1 to 1j. Repealed. July 1, 1944, ch. 373, title XIII, &sect;1313, 58 Stat. 714</h3>\n" +
"<!-- field-end:repealedhead -->\n" +
"<!-- field-start:repealsummary -->\n" +
"<p class=\"note-body\">Section 1, acts July 1, 1902, ch. 1370, &sect;1, 32 Stat. 712; Aug. 14, 1912, ch. 288, &sect;1, 37 Stat. 309, provided that Public Health and Marine Hospital Service should be known as the Public Health Service. See section 202 of this title.</p>\n" +
"<p class=\"note-body\">Section 1a, act Nov. 11, 1943, ch. 298, &sect;1, 57 Stat. 587, provided for organization and function of Public Health Service. See section 203 of this title.</p>\n" +
"<p class=\"note-body\">Section 1b, act Nov. 11, 1943, ch. 298, &sect;2, 57 Stat. 587, provided for appointment of Assistant Surgeons General, their grade, pay, and allowances. See sections 206, 207, and 210 of this title.</p>\n" +
"<p class=\"note-body\">Section 1c, act Nov. 11, 1943, ch. 298, &sect;3, 57 Stat. 587, provided for chiefs of divisions, their grade, pay and allowances, and creation of a Dental Division and a Sanitary Engineering Division. See sections 206, 207, and 210 of this title.</p>\n"
             ), "UTF-8"));
    // From http://www.gpo.gov/fdsys/search/pagedetails.action?collectionCode=USCODE&searchPath=Title+42%2FChapter+1%2FSUBCHAPTER+I&granuleId=&packageId=USCODE-2006-title42&oldPath=Title+42%2FCHAPTER+1&fromPageDetails=true&collapse=true&ycord=114
    assertTrue(fact.shouldFilter(new StringInputStream(
"\n" +
"\n" +
"\n" +
"\n" +
"<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Strict//EN\" \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd\">\n" +
"\n" +
"\n" +
"\n" +
"\n" +
"\n" +
"       \n" +
"\n" +
"                         \n" +
"\n" +
"\n" +
"\n" +
"\n" +
"       \n" +
"\n" +
"\n" +
"\n" +
"\n" +
"\n" +
"\n" +
"\n" +
"\n" +
"\n" +
"\n" +
"<html xmlns=\"http://www.w3.org/1999/xhtml\" xml:lang=\"en\" lang=\"en\">\n" +
"<head profile=\"http://www.w3.org/2005/10/profile\">\n" +
"       <base href=\"http://www.gpo.gov/fdsys/\" />\n" +
"       \n" +
"       <meta http-equiv=\"Content-Type\" content=\"text/html; charset=utf-8\" />\n" +
"       <meta http-equiv=\"Content-Script-Type\" content=\"text/javascript\" />        \n" +
"       <meta http-equiv=\"pragma\" content=\"no-cache\" />\n" +
"       <meta http-equiv=\"cache-control\" content=\"no-cache\" />\n" +
"       <meta http-equiv=\"expires\" content=\"0\" />     \n" +
"       <meta http-equiv=\"keywords\" content=\"\n" +
"               42 U.S.C.  - THE PUBLIC HEALTH AND WELFARE\n" +
"       \" />\n" +
"       <meta http-equiv=\"description\" content=\"\n" +
"               42 U.S.C.  - THE PUBLIC HEALTH AND WELFARE\n" +
"       \" /> \n" +
"       \n" +
"       <script type=\"text/javascript\">\n" +
"       \n" +
"       var WT_SERVER = '162.140.239.17';\n" +
"       var WT_DCS_ID = 'dcsjsw8h600000gotf0vyrmly_2j7v';\n" +
"       var WT_HOST_SERVER_NAME = 'http://www.gpo.gov/fdsys';\n" +
"       \n" +
"       var BASE_PRESENTATION_PATH = 'http://www.gpo.gov/fdsys/';\n" +
"       var BASE_USER_HELP = 'http://www.gpo.gov/help/';        \n" +
"       \n" +
"               \n" +
"                var SEARCHWEBAPP_BUILD = 'DEV_INT_R3_15';\n" +
"                               \n" +
"                               var coopValue='';\n" +
"\n" +
"    </script>\n" +
"       <script type=\"text/javascript\" src=\"http://www.gpo.gov/fdsysgpopages/scripts/gpo.js\"></script>      \n" +
"       <script type=\"text/javascript\" src=\"scripts/fdsys.js\"></script>             \n" +
"       <script type=\"text/javascript\" src=\"scripts/mktree.js\"></script>\n" +
"       <title>\n" +
"               42 U.S.C.  - THE PUBLIC HEALTH AND WELFARE\n" +
"       </title>\n" +
"       \n" +
"               <style type=\"text/css\">\n" +
"                       #left-menu-sub-search {\n" +
"                               color: #000000;\n" +
"                       }\n" +
"               </style>                \n" +
"       \n" +
"\n" +
"       <style type=\"text/css\">\n" +
"               @import url(http://www.gpo.gov/fdsysgpopages/styles/fdsyspages.css);\n" +
"               @import url(http://www.gpo.gov/fdsysgpopages/styles/gpo.css);\n" +
"       </style>        \n" +
"       <link rel=\"icon\" type=\"image/gif\" href=\"images/gpo_favicon.gif\" />\n" +
"</head>\n" +
"\n" +
"\n" +
"<body onload=\"setFocusTextField(); setScrollXY();\">\n" +
"       <script type=\"text/javascript\" src=\"scripts/wz_tooltip.js\"></script>        \n" +
"       \n" +
"            \n" +
"       <div id=\"wrapper\">\n" +
"\n" +
"               \n" +
"               \n" +
"\n" +
"\n" +
"\n" +
"\n" +
"\n" +
"<!--<input type=\"hidden\" name=\"struts.token.name\" value=\"struts.token\" />\n" +
"<input type=\"hidden\" name=\"struts.token\" value=\"1V1X534Y9933UICYEHEEVP7HPIKML821\" />-->\n" +
"\n" +
"\n" +
"\n" +
"\n" +
"\n" +
"\n" +
"\n" +
"\n" +
"\n" +
"<a href=\"\" id=\"skip-anchor\" >\n" +
"       <img src=\"/images/spacer.gif\" alt=\"Skip to content.\" class=\"skip-anchor-gif\" />\n" +
"</a>\n" +
"<script type=\"text/javascript\">createSkipLink();</script>\n"
        ), "UTF-8"));
    // From http://www.gpo.gov/fdsys/pkg/USCODE-2006-title42/html/USCODE-2006-title42.htm
    // (one that should not be filtered)
    assertFalse(fact.shouldFilter(new StringInputStream(
"<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Transitional//EN\"\n" +
"\"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd\">\n" +
"<html xmlns=\"http://www.w3.org/1999/xhtml\">\n" +
" <head>\n" +
"  <link rel=\"stylesheet\" type=\"text/css\" href=\"usc.css\"  />\n" +
"  <title>U.S.C. Title 42 - THE PUBLIC HEALTH AND WELFARE\n" +
"<!-- AUTHORITIES-PUBLICATION-NAME:2006 Edition -->\n" +
"<!-- AUTHORITIES-PUBLICATION-ID:2006MNED006 -->\n" +
"<!-- AUTHORITIES-PUBLICATION-YEAR:2006 -->\n" +
"<!-- AUTHORITIES-LAWS-ENACTED-THROUGH-DATE:20070103 -->\n" +
"<!-- SEARCHABLE-LAWS-ENACTED-THROUGH-DATE:January 3rd, 2007 -->\n" +
"<!-- AUTHORITIES-USC-TITLE-NAME:TITLE 42 - THE PUBLIC HEALTH AND WELFARE -->\n" +
"<!-- AUTHORITIES-USC-TITLE-ENUM:42 -->\n" +
"<!-- AUTHORITIES-USC-TITLE-STATUS:editorial -->\n" +
"\n" +
"<!-- CONVERSION-PROGRAM:xy2html.pm-0.329-20100225 -->\n" +
"<!-- CONVERSION-DATETIME:20100225153648 -->\n" +
"\n" +
"  </title>\n" +
" </head>\n" +
" <body>\n" +
"<span style=\"font-weight:bold;font-size:12pt;\">42 U.S.C. </span><br/>\n" +
"<span style=\"font-size:10pt\">United States Code, 2006 Edition</span><br/>\n" +
"<span style=\"font-size:10pt\">Title 42 - THE PUBLIC HEALTH AND WELFARE</span><br/>\n" +
"<span style=\"font-size:10pt\">From the U.S. Government Printing Office, <a href=\"http://www.gpo.gov\">www.gpo.gov</a></span><br/><br/>\n" +
"\n" +
"  <div width=\"700\">\n" +
"\n" +
"<!-- PDFPage:1 -->\n" +
"<!-- documentid:42_ currentthrough:20070103 documentPDFPage:-1 -->\n" +
"<!-- itempath:/420 -->\n" +
"<!-- itemsortkey:420AAAA -->\n" +
"<!-- expcite:TITLE 42-THE PUBLIC HEALTH AND WELFARE -->\n" +
"<!-- field-start:titlehead -->\n" +
"<h1 class=\"usc-title-head\">TITLE 42&mdash;THE PUBLIC HEALTH AND WELFARE</h1>\n" +
"<!-- field-end:titlehead -->\n" +
"<!-- field-start:analysis -->\n" +
"<div class=\"analysis\">\n" +
"<div><div class=\"analysis-head-left\">Chap.</div><div class=\"analysis-head-right\">Sec.</div></div>\n" +
"<div><div class=\"three-column-analysis-style-content-left\">1.</div><div class=\"three-column-analysis-style-content-center\" id=\"wide\">The Public Health Service [Repealed or Omitted, See Chapter 6A]</div><div class=\"three-column-analysis-style-content-right\">1</div></div>\n" +
"<div><div class=\"three-column-analysis-style-content-left\">1A.</div><div class=\"three-column-analysis-style-content-center\" id=\"wide\">The Public Health Service; Supplemental Provisions [Transferred or Omitted]</div><div class=\"three-column-analysis-style-content-right\">71</div></div>\n"
        ), "UTF-8"));
  }
  
  private void doTestString(String expectedOutput,
                            String input)
      throws Exception {
    assertEquals(expectedOutput,
                 StringUtil.fromInputStream(fact.createFilteredInputStream(null,
                                                                           new StringInputStream(input),
                                                                           ENC)));
  }
  
}