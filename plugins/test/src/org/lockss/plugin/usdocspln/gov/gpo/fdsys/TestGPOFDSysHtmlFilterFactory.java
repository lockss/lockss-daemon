/*
 * $Id: TestGPOFDSysHtmlFilterFactory.java,v 1.5 2012-07-31 21:42:56 thib_gc Exp $
 */

/*

Copyright (c) 2000-2012 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.plugin.usdocspln.gov.gpo.fdsys;

import org.lockss.util.*;
import org.lockss.plugin.FilterFactory;
import org.lockss.test.*;

public class TestGPOFDSysHtmlFilterFactory extends LockssTestCase {
  
  private static final String ENC = Constants.DEFAULT_ENCODING;

  private FilterFactory fact;

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
    doTestString("",
                 "<!--<input type=\"hidden\" name=\"struts.token.name\" value=\"struts.token\" />\n" +
                 "<input type=\"hidden\" name=\"struts.token\" value=\"H34QZK9TF4N77V7LRD94SBULW6JZ9DPB\" />-->");
    doTestString("",
                 "<input type=\"hidden\" name=\"struts.token.name\" value=\"struts.token\" />\n" +
                 "<input type=\"hidden\" name=\"struts.token\" value=\"H34QZK9TF4N77V7LRD94SBULW6JZ9DPB\" />");
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