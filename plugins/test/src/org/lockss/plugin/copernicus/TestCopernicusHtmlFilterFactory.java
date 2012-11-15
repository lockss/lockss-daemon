/*
 * $Id: TestCopernicusHtmlFilterFactory.java,v 1.1 2012-11-15 21:36:52 alexandraohlson Exp $
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

package org.lockss.plugin.copernicus;

import java.io.*;

import org.lockss.util.*;
import org.lockss.test.*;

public class TestCopernicusHtmlFilterFactory extends LockssTestCase {
  static String ENC = Constants.DEFAULT_ENCODING;

  private CopernicusHtmlFilterFactory fact;
  private MockArchivalUnit mau;

  public void setUp() throws Exception {
    super.setUp();
    fact = new CopernicusHtmlFilterFactory();
    mau = new MockArchivalUnit();
  }

  /* example of journal metrics block in left column */
  private static final String journalMetricsHtml = 
      "<div><div style=\"margin-top: 25px;\" id=\"journal_metrics\"><map name=\"m_graphic_cp_journal_metrics\" id=\"m_graphic_cp_journal_metrics\">" +
          "<area alt=\"Scopus Scimago Journal Rank (SJR) 2011, as of April 2012\" title=\"Scopus Scimago Journal Rank (SJR) 2011, as of April 2012\" target=\"Keine\" coords=\"14,173,166,215\" shape=\"rect\" />" +
          "<div class=\"journal_metrics_definitions\" border=\"0\" cellpadding=\"0\" cellspacing=\"0\" width=\"130\"> </div>" +
          "</map></div></div>";

  private static final String journalMetricsHtmlFiltered = 
      "<div></div>";

  private static final String rightColumnHtml =
      "<div><div id=\"page_colum_right\" class=\"page_colum\">" +
          "<div class=\"page_colum_container CMSCONTAINER\" id=\"page_colum_right_container\">" +
          "<div id=\"copernicus_publications\" class=\"cmsbox \">" +
          "<a href=\"http://publications.copernicus.org\" target=\"_blank\">" +
          "<img src=\"http://www.climate-of-the-past.net/Copernicus_Publications_Logo.jpg\" cofileid=\"154\" alt=\"\" /></a><" +
          "/div></div></div></div>";
  private static final String rightColumnHtmlFiltered =
      "<div></div>";
  
  private static final String scriptsAndComments =
      "</tr></table><script type=\"text/javascript\">" +
          "                  /* <![CDATA[ */" +
          "                        var cookieEnabled=(navigator.cookieEnabled)? true : false;" +
          "                    //cannot determine cookie state. just have a try" +
          "                 if (!cookieEnabled && typeof navigator.cookieEnabled==\"undefined\")" +
          "                 {" +
          "                         document.cookie = \"testcookie\";" +
          "                         cookieEnabled = document.cookie.indexOf(\"testcookie\")!=-1;"+
          "                 }" +
          "         /* ]]> " +
          "         </script>" +
          " <script type=\"text/javascript\" language=\"Javascript\">/* <![CDATA[ */ var x=-1,y=-1,d=document;if(self.innerHeight){x=self.innerWidth;y=self.innerHeight;}else if (d.body){x=d.body.clientWidth;y=document.body.clientHeight;} /* ]]> */</script>" +
          "    <noscript><img alt=\"\" src=\"http://contentmanager.copernicus.org/webservices/webbug.php?pt=library&t=g&p=-1&s=417\" width=\"0\" height=\"0\" style=\"visibility: hidden\" /></noscript>" +
          "  <!-- ptpl created 14.11. 05:58:33 by n/a --></body></html>";

  private static final String scriptsAndCommentsFiltered =
      "</tr></table> </body></html>";

  public void testFiltering() throws Exception {
    InputStream inA;
    InputStream inB;
    
    /* journalMetrics test */
    inA = fact.createFilteredInputStream(mau, new StringInputStream(journalMetricsHtml),
        ENC);

    assertEquals(journalMetricsHtmlFiltered,StringUtil.fromInputStream(inA));
    
    /* remove right column test */
    inA = fact.createFilteredInputStream(mau, new StringInputStream(rightColumnHtml),
        ENC);

    assertEquals(rightColumnHtmlFiltered,StringUtil.fromInputStream(inA));
    
    /* remove <script> <noscript> and comments <!-- --> */
    inA = fact.createFilteredInputStream(mau, new StringInputStream(scriptsAndComments),
        ENC);

    assertEquals(scriptsAndCommentsFiltered,StringUtil.fromInputStream(inA));

  }
}