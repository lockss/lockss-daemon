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
      "<div><div id=\"page_colum_right\" class=\"page_colum\">\n" +
          "<div class=\"page_colum_container CMSCONTAINER\" id=\"page_colum_right_container\">" +
          "<div id=\"copernicus_publications\" class=\"cmsbox \">" +
          "<a href=\"http://publications.copernicus.org\" target=\"_blank\">" +
          "<img src=\"http://www.climate-of-the-past.net/Copernicus_Publications_Logo.jpg\" cofileid=\"154\" alt=\"\" /></a><" +
          "/div></div></div>\n</div>";
  private static final String rightColumnHtmlFiltered =
      "<div> </div>";
  
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
  
  private static final String iFrameHtml =
      "<div id=\"page_colum_foo\" class=\"page_colum\">" +
          "<iframe " +
          "frameborder=\"0\" id=\"co_auth_check_authiframecontainer\" " +
          "style=\"width: 179px; height: 57px; margin: 0; margin-bottom: 5px; margin-left: 10px; margin-top: -15px; padding: 0; border: none; overflow: hidden; background-color: transparent; display: none;\"" +
          " src=\"\"></iframe>" +
          "<div class=\"page_colum_container CMSCONTAINER\">";
  private static final String iFrameFiltered =
      "<div id=\"page_colum_foo\" class=\"page_colum\">" +
          "<div class=\"page_colum_container CMSCONTAINER\">";
  
  private static final String leftColumnHtml =
      "<div class=\"page_colum_container CMSCONTAINER\" id=\"page_colum_left_container\">" +
          "<div id=\"page_navigation_left\" class=\"cmsbox \">" +
          "<ul class=\"co_function_get_navigation get_navigation farbe_auf_hauptnavigation\">" +
          "<li class=\"hintergrundfarbe_journal_hervorgehoben co_function_get_navigation_is_no_parent co_function_get_navigation_is_open\" id=\"co_getnavigation_page_home\">" +
          "<a href=\"http://www.advances-in-geosciences.net/home.html\" pageid=\"1211\" class=\"hintergrundfarbe_journal active_menuitem\">Home</a>" +
          "</li>" +
          "<li class=\"active_menuitem hintergrundfarbe_journal_hervorgehoben\">" +
          "<a href=\"/volumes.html\" class=\"hintergrundfarbe_journal\">Online Library </a></li>" +
          "<ul class=\"subbox_background\">" +
          "<li><a href=\"/recent_papers.html\">Recent Papers</a></li>" +
          "<li><a href=\"/volumes.html\" class=\"active_menuitem\">Volumes</a></li>" +
          "<li><a href=\"/library_search.html\">Library Search</a></li>" +
          "<li><a href=\"/title_and_author_search.html\">Title and Author Search</a></li>" +
          "</ul>" +
          "<li class=\"hintergrundfarbe_journal_hervorgehoben co_function_get_navigation_is_no_parent co_function_get_navigation_is_closed\" id=\"co_getnavigation_page_alerts_and_rss_feeds\">" +
          "<a href=\"http://www.advances-in-geosciences.net/rss_feeds.html\" pageid=\"1212\" class=\"hintergrundfarbe_journal \">RSS Feeds</a></li>" +
          "</ul></div>";
  private static final String leftColumnFiltered =
      "<div class=\"page_colum_container CMSCONTAINER\" id=\"page_colum_left_container\">";
  
  private static final String whiteSpacesV1 = 
      "<a href=\"/1/1/2003/adgeo-1-1-2003.pdf\" >" +
          "Full Article in PDF</a>" +
          " (PDF, 131 KB)&nbsp;&nbsp;&nbsp;<br />" +
          "<br />" +
          "<b>" +
          "Citation:</b>" +
          " Howe,&nbsp;E., Stenseng,&nbsp;L., and Tscherning,&nbsp;C.&nbsp;C.: Analysis of one month of CHAMP state vector and accelerometer data for the recovery of the gravity potential, Adv. Geosci., 1, 1-4, doi:10.5194/adgeo-1-1-2003, 2003.&nbsp;&nbsp;&nbsp;<a href=\"/1/1/2003/adgeo-1-1-2003.bib\">" +
          "Bibtex</a>" +
          "&nbsp;&nbsp;&nbsp;<a href=\"/1/1/2003/adgeo-1-1-2003.ris\">" +
          "EndNote</a>" +
          "&nbsp;&nbsp;&nbsp;<a href=\"/1/1/2003/adgeo-1-1-2003.ris\">" +
          "Reference Manager </a>" +
          "&nbsp;&nbsp;&nbsp;<a href=\"/1/1/2003/adgeo-1-1-2003.xml\">" +
          "XML</a>" +
          "</span>" +
          "<div style=\"padding-top: 150px;\">" +
          "&nbsp;</div>" +
          "</div>" +
          "</div>" +
          " </div>" +
          " <div style=\"height: 1px; width: 622px;\">" +
          "</div>" +
          " </td>" +
          " <td class=\"hintergrundfarbe_spalten page_columntable_colum\" style=\"border-right: none;\">" +
          " </td>" +
          " </tr>" +
          "</table>" +
          " </body>" +
          "</html>";

  private static final String whiteSpacesV2 = 
      "<a href=\"/1/1/2003/adgeo-1-1-2003.pdf\" >" +
          "Full Article in PDF</a>" +
          " (PDF, 131 KB)" +
          "&nbsp; &nbsp;<br />" +
          "<br />" +
          "<b>" +
          "Citation:</b>" +
          " Howe,&nbsp;E., Stenseng,&nbsp;L., and Tscherning,&nbsp;C.&nbsp;C.: Analysis of one month of CHAMP state vector and accelerometer data for the recovery of the gravity potential, Adv. Geosci., 1, 1-4, doi:10.5194/adgeo-1-1-2003, 2003.&nbsp; &nbsp;<a href=\"/1/1/2003/adgeo-1-1-2003.bib\">" +
          "Bibtex</a>" +
          "&nbsp; &nbsp;<a href=\"/1/1/2003/adgeo-1-1-2003.ris\">" +
          "EndNote</a>" +
          "&nbsp; &nbsp;<a href=\"/1/1/2003/adgeo-1-1-2003.ris\">" +
          "Reference Manager </a>" +
          "&nbsp; &nbsp;<a href=\"/1/1/2003/adgeo-1-1-2003.xml\">" +
          "XML</a>" +
          "</span>" +
          "<div style=\"padding-top: 150px;\">" +
          "&nbsp;</div>" +
          "</div>" +
          "</div>" +
          " </div>" +
          " <div style=\"height: 1px; width: 622px;\">" +
          "</div>" +
          " </td>" +
          " <td class=\"hintergrundfarbe_spalten page_columntable_colum\" style=\"border-right: none;\">" +
          " </td>" +
          " </tr>" +
          "</table>" +
          " </body>" +
          "</html>";
  
  private static final String noSpanStyleHtml =
      "dynamics, but are also important for the validation of ozone measurements.</span>" +
          "<span class=\"pb_toc_link\"><br /><br /><a href=\"/14/1111/1996/angeo-14-1111-1996.pdf\" >Full Article</a>" +
          "(PDF, 639 KB)&nbsp;&nbsp;&nbsp;<br /><br /></div></div></div>";
  private static final String spanStyleHtml =
      "dynamics, but are also important for the validation of ozone measurements.</span>" +
          "<span class=\"pb_toc_link\"><br /><br />&nbsp;<span style=\"white-space:nowrap;\"><a href=\"/14/1111/1996/angeo-14-1111-1996.pdf\" >Full Article</a>" +
          "(PDF, 639 KB)</span>&nbsp; &nbsp;<br /><br /></div></div></div>"; 
  
  private static final String headerContentHtml =
      "<body><!-- $$BODY_HEAD$$ -->" +
      "<div id=\"page_header\" class=\"farbe_auf_journaluntergrund\">" +
      "<a id=\"page_header_cover\" title=\"\" href=\"http://www.scientific-drilling.net/index.html\">" +
      "<!-- --></a><a id=\"sd_moodboard\" href=\"http://www.scientific-drilling.net/index.html\"><!-- --></a> " +                      
      "<div id=\"page_header_main\" class=\"hintergrundfarbe_journal\">" +
       "<div id=\"page_header_main_headlines\"></div></div>" +
       "<div id=\"page_header_footer\" class=\"hintergrundfarbe_journal\">" + 
       "</div><!-- TOPRIGHT -->" +
       "<div id=\"page_header_main_right_b0x\" class=\"farbe_auf_journalhintergrund\"></div>" +
       "<!-- TOPRIGHT/ -->" +
       "</div><!-- HEADER/ --></body>";
  private static final String headerContentFiltered =
      "<body></body>";
  
  private static final String allLeftColumnHtml =
      "<table id=\"page_columntable\"><tr>" +
          "<td class=\"hintergrundfarbe_spalten page_columntable_colum\" style=\"border-left: none;\">" +
          "<!-- LEFT COLUM -->" +
          "<div id=\"page_colum_left\" class=\"page_colum\">" +
          "<div class=\"page_colum_container CMSCONTAINER\" id=\"page_colum_left_container\">" +
          "<div id=\"page_navigation_left\" class=\"cmsbox \">" +
          "<ul class=\"co_function_get_navigation get_navigation farbe_auf_hauptnavigation\">" +
          "<li class=\"hintergrundfarbe_journal_hervorgehoben co_function_get_navigation_is_no_parent co_function_get_navigation_is_open\" id=\"co_getnavigation_page_home\">" +
          "<a   href=\"http://foo.html\" pageid=\"4817\" class=\"hintergrundfarbe_journal active_menuitem\">Home</a></li>" +
          " </ul> </div></div></div><!-- LEFT COLUM/ -->" +    
          "</td></tr></table>";

private static final String allLeftColumnFiltered =
"<table id=\"page_columntable\"><tr>" +
    "<td class=\"hintergrundfarbe_spalten page_columntable_colum\" style=\"border-left: none;\">" +
    "</td></tr></table>";
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

    /* remove iFrame */
    inA = fact.createFilteredInputStream(mau, new StringInputStream(iFrameHtml),
        ENC);

    assertEquals(iFrameFiltered,StringUtil.fromInputStream(inA));    
    /* remove left column search area */
    inA = fact.createFilteredInputStream(mau, new StringInputStream(leftColumnHtml),
        ENC);

    assertEquals(leftColumnFiltered,StringUtil.fromInputStream(inA));
    
    inA = fact.createFilteredInputStream(mau, new StringInputStream(whiteSpacesV1),
        ENC);
    inB = fact.createFilteredInputStream(mau, new StringInputStream(whiteSpacesV2),
        ENC);
    assertEquals(StringUtil.fromInputStream(inA),
        StringUtil.fromInputStream(inB));
    
    //serving up slightly different files 
    inA = fact.createFilteredInputStream(mau, new StringInputStream(noSpanStyleHtml),
        ENC);
    inB = fact.createFilteredInputStream(mau, new StringInputStream(spanStyleHtml),
        ENC);
    assertEquals(StringUtil.fromInputStream(inA),
        StringUtil.fromInputStream(inB));
    
    /* remove left column search area */
    inA = fact.createFilteredInputStream(mau, new StringInputStream(allLeftColumnHtml),
        ENC);
    assertEquals(allLeftColumnFiltered,StringUtil.fromInputStream(inA));    

    /* remove left column search area */
    inA = fact.createFilteredInputStream(mau, new StringInputStream(headerContentHtml),
        ENC);
    assertEquals(headerContentFiltered,StringUtil.fromInputStream(inA));    

    
  }
}


