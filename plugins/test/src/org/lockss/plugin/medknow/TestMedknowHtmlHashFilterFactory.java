/* $Id$

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

package org.lockss.plugin.medknow;

import java.io.*;

import org.lockss.util.*;
import org.lockss.test.LockssTestCase;
import org.lockss.test.MockArchivalUnit;
import org.lockss.test.StringInputStream;


public class TestMedknowHtmlHashFilterFactory extends LockssTestCase {
  private MedknowHtmlHashFilterFactory fact;
  private MockArchivalUnit mau;

  public void setUp() throws Exception {
    super.setUp();
    fact = new MedknowHtmlHashFilterFactory();
  }
  
  private static final String menuBarUsersOnlineHtml =
      "<div class=\"block\" id=\"menuBarUser\">" +
          "<td align=\"right\">Users Online: 17</td>" +
          "</div>";
  
  private static final String menuBarUsersOnlineHtmlHashFiltered =
      "<div class=\"block\" id=\"menuBarUser\"></div>";

  
  private static final String androidMobileAdHtml =
      "<div class=\"block\" id=\"topBarUser\">" +
          "<span id=\"tx\" class=\"other\">&nbsp;&nbsp;&nbsp;" +
          "<a href=\"https://market.android.com/details?id=comm.app.medknow\">" +
          "<strong>Click here</strong></a><strong> " +
          "to download free Android Application for this and other journals</strong>" +
          "&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;<a href=\"/mobile/\">" +
          "<strong>Click here to view optimized website for mobile devices</strong>" +
          "</a>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Journal is indexed " +
          "with <strong>MEDLINE/Index Medicus</strong></span>" +
          "</div>";
  
  private static final String androidMobileAdHtmlHashFiltered =
      "<div class=\"block\" id=\"topBarUser\"></div>";
  
 
  private static final String leftNavCellStatsHtml =
      "<div class=\"block\" id=\"articleAccessStatistics\">" +
          "<table width=\"100%\" cellspacing=\"0\" cellpadding=\"0\"" +
          "class=\"leftnavcell\"><tbody><tr><td colspan=\"2\" height=\"25\">" +
          "<b>&nbsp;Article Access Statistics</b></td></tr>" +
          "<tr><td colspan=\"2\" height=\"1\" class=\"leftnavcell\"></td></tr>" +
          "<tr><td height=\"20\">&nbsp;&nbsp;&nbsp;&nbsp;Viewed</td>" +
          "<td align=\"right\">2102&nbsp;&nbsp;&nbsp;&nbsp;</td></tr>" +
          "<tr><td colspan=\"2\" height=\"1\" class=\"leftnavcell\"></td></tr>" +
          "<tr><td height=\"20\">&nbsp;&nbsp;&nbsp;&nbsp;Printed</td>" +
          "<td align=\"right\">135&nbsp;&nbsp;&nbsp;&nbsp;</td></tr>" +
          "<tr><td colspan=\"2\" height=\"1\" class=\"leftnavcell\"></td></tr>" +
          "<tr><td height=\"20\">&nbsp;&nbsp;&nbsp;&nbsp;Emailed</td>" +
          "<td align=\"right\">0&nbsp;&nbsp;&nbsp;&nbsp;</td></tr>" +
          "<tr><td colspan=\"2\" height=\"1\" class=\"leftnavcell\"></td></tr>" +
          "<tr><td height=\"20\">&nbsp;&nbsp;&nbsp;&nbsp;PDF Downloaded</td>" +
          "<td align=\"right\">196&nbsp;&nbsp;&nbsp;&nbsp;</td></tr>" +
          "<tr><td colspan=\"2\" height=\"1\" class=\"leftnavcell\"></td></tr>" +
          "<tr><td height=\"20\">&nbsp;&nbsp;&nbsp;&nbsp;Comments&nbsp;</td>" +
          "<td align=\"right\"><a href=\"addremark.asp?issn=0189-6725;" +
          "year=2008;volume=5;issue=1;spage=3;epage=7;aulast=Ameh;" +
          "aid=AfrJPaediatrSurg_2008_5_1_3_41627\">[Add]</a>" +
          "&nbsp;&nbsp;&nbsp;&nbsp;</td></tr><tr><td colspan=\"2\"" +
          "height=\"1\" class=\"leftnavcell\"></td></tr>" +
          "<tr><td colspan=\"2\" height=\"1\">" +
          "<script>function insertIt() { " +
          "var _y = document.getElementById('framediv');" +
          "var _x = window.frames[0].document.body.innerHTML;" +
          "_y.innerHTML = _x " +
          "}</script>" +
          "<p></p><div id=\"framediv\"><iframe frameborder=\"no\"" +
          "scrolling=\"no\" width=\"100%\" name=\"aa\" " +
          "src=\"_aagraph.asp?a=AfrJPaediatrSurg_2008_5_1_3_41627\">" +
          "</iframe></div><p></p></td></tr></tbody></table>" +
          "</div>";
          
  private static final String leftNavCellStatsHtmlHashFiltered =
      "<div class=\"block\" id=\"articleAccessStatistics\"></div>";
   
  /*
   *  Compare Html and HtmlHashFiltered
   */
  
  public void testMenuBarUsersOnlineHtmlFiltering() throws Exception {
    InputStream actIn = fact.createFilteredInputStream(mau,
        new StringInputStream(menuBarUsersOnlineHtml),
        Constants.DEFAULT_ENCODING);

    assertEquals(menuBarUsersOnlineHtmlHashFiltered,
                 StringUtil.fromInputStream(actIn));

  }

  public void testAndroidMobileAdHtmlFiltering() throws Exception {
    InputStream actIn = fact.createFilteredInputStream(mau,
        new StringInputStream(androidMobileAdHtml),
        Constants.DEFAULT_ENCODING);

    assertEquals(androidMobileAdHtmlHashFiltered, StringUtil.fromInputStream(actIn));

  }

  public void testLeftNavCellStatsHtmlHtmlFiltering() throws Exception {
    InputStream actIn = fact.createFilteredInputStream(mau,
        new StringInputStream(leftNavCellStatsHtml),
        Constants.DEFAULT_ENCODING);

    assertEquals(leftNavCellStatsHtmlHashFiltered, StringUtil.fromInputStream(actIn));

  }

}
