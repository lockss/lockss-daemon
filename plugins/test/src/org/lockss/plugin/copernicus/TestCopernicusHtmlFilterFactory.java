/*

Copyright (c) 2000-2020, Board of Trustees of Leland Stanford Jr. University
All rights reserved.

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

package org.lockss.plugin.copernicus;

import java.io.*;

import org.lockss.util.*;
import org.lockss.test.*;

public class TestCopernicusHtmlFilterFactory extends LockssTestCase {
  static String ENC = Constants.DEFAULT_ENCODING;

  private CopernicusHtmlFilterFactory hfact;
  private MockArchivalUnit mau;

  public void setUp() throws Exception {
    super.setUp();
    hfact = new CopernicusHtmlFilterFactory();
    mau = new MockArchivalUnit();
  }

  private static final String includeBit=
      "<div class=\"top\">" +
      "<div id=\"page_content_container\">KEEPME</div>" +
      "</div>";
  private static final String includeBitFiltered=
      " <div id=\"page_content_container\">KEEPME </div>";

  private static final String basicLayout = 
      "<html>"+ 
          "<head>HEAD</head>" + 
          "<body>" +
          "<!-- $$BODY_HEAD$$ -->" +
          "<div id=\"w-wrapper\">" +
          "<div id=\"w-head\">WHEAD</div>" +
          "<div id=\"w-body\">WBODY" +
          "<div class=\"foo\" id=\"page_colum_left_container\">LEFTCOLUMN</div>" +
          "<div id=\"c-wrapper\"> +" +
          "<div class=\"foo\" id=\"page_content_container\">GOODSTUFF</div>" +
          "</div>" +
          "<div class=\"foo\" id=\"page_colum_right_container\">RIGHTCOLUMN</div>" +
          "</div>" +
          "<noscript>NOSCRIPT</noscript" +
          "</body>" +
          "</html>";
  
  private static final String basicLayoutFiltered =
          " <div id=\"page_content_container\">GOODSTUFF </div>";
  
  private static final String minimumBeginning =
      "<html>"+ 
          "<head>HEAD</head>" + 
          "<body>" +
          "<!-- $$BODY_HEAD$$ -->" +
          "<div class=\"foo\" id=\"page_content_container\">";
  private static final String minimumEnding =
      "</div></body></html>";
  private static final String minimumEndingFiltered = " </div>";
  
  private static final String scriptsAndComments =
      minimumBeginning +
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
          "  <!-- ptpl created 14.11. 05:58:33 by n/a -->" +
          minimumEnding;

  private static final String scriptsAndCommentsFiltered =
      " <div id=\"page_content_container\">" +
      " </tr> </table>" + minimumEndingFiltered;
  
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
      "dynamics, but are also important for the validation of ozone measurements. </span>" +
          " <span class=\"pb_toc_link\"> <br /> <br />&nbsp; <span style=\"white-space:nowrap;\"> <a href=\"/14/1111/1996/angeo-14-1111-1996.pdf\" >Full Article </a>" +
          "(PDF, 639 KB)</span>&nbsp; &nbsp; <br /> <br /> </div> </div> </div>"; 
  
private static final String genericIndexContent =
  "<div class=\"CMSCONTAINER j-content edt-flag\" id=\"page_content_container\">" +
  "<!-- $$CONTENT$$ -->" +
  "<div id=\"landing_page\" class=\"cmsbox j-intro-section j-section\">" +
  "  generic information about this journal" +
  "</div>" +
  "<div id=\"cmsbox_61812\" class=\"cmsbox \"><h2>News</h2>" +
  "<div id=\"news\">" +
  "<div class=\"j-news-item\">" +
  "NEW GOES HERE" +
  "</div>" +
  "</div>" +
  "<h2>Highlight articles</h2>" +
  "<div id=\"highlight_articles\">" +
  "<span class=\"j-news-item-date\">13 Jul 2016</span>" +
  "</div>" +
  "</div>" +
  "<div id=\"recent_paper\" class=\"cmsbox j-article j-article-section\">" +
  "<h2 class=\"title\">Recent articles</h2>" +
  "        <div class=\"a-paper\">" +
  "            <div class=\"journal-type\">" +
  "                AAB" +
  "            </div>" +
  "            <div class=\"paper-teaser\">" +
  "                <a href=\"foo\">articletitle</a>" +
  "            </div>" +
  "            <div class=\"publishing-date\">" +
  "                13 Aug 2015" +
  "            </div>" +
  "        </div>" +
  "</div>" +
  "<div id=\"something else\">" +
  "blah goes here" +
  "</div>" +
  "<div id=\"essentential-logos-carousel\" class=\"cmsbox \">" +
  "<ul class=\"essentential-logos\">" +
  "       <li class=\"essentential-logo\">   " +
  "         logo" +
  "    </li>" +
  "       <li class=\"essentential-logo\">   " +
  "         logo" +
  "    </li>" +
  "       <li class=\"essentential-logo\">   " +
  "         logo" +
  "    </li>" +
  "</ul></div>";

private static final String genericIndexContentFiltered =
" <div id=\"page_content_container\">" +
" <div id=\"cmsbox_61812\" class=\"cmsbox \"> <h2>News </h2>" +
" <h2>Highlight articles </h2>" +
" </div>" +
" <div id=\"something else\">" +
"blah goes here" +
" </div>";


/*  filtered bits for CRAWL filter */
private static final String genericIndexCrawlContent =
"<div class=\"CMSCONTAINER j-content edt-flag\" id=\"page_content_container\">" +
"<!-- $$CONTENT$$ -->" +
"<div id=\"landing_page\" class=\"cmsbox j-intro-section j-section\">" +
"  generic information about this journal" +
"</div>" +
"<div id=\"cmsbox_61812\" class=\"cmsbox \"><h2>News</h2>" +
"<div id=\"news\">" +
"<div class=\"j-news-item\">" +
"NEW GOES HERE" +
"</div>" +
"</div>" +
"</div>" +
"<div id=\"something else\">" +
"blah goes here" +
"</div>" +
"<div id=\"essentential-logos-carousel\" class=\"cmsbox \">" +
"<ul class=\"essentential-logos\">" +
"       <li class=\"essentential-logo\">   " +
"         logo" +
"    </li>" +
"       <li class=\"essentential-logo\">   " +
"         logo" +
"    </li>" +
"       <li class=\"essentential-logo\">   " +
"         logo" +
"    </li>" +
"</ul></div>";

/* class id has inconsistent name - just remove for hashing 
 */
private static final String publishedDateIdBefore = 
"<div class=\"CMSCONTAINER j-content edt-flag\" id=\"page_content_container\">" +
"<div class=\"publishedDateAndMsType group\">" +
"<div class=\"msType\"></div>" +
"<div class=\"publishedDate\">10 Jul 2013</div>" +
"</div>"+
"<div class=\"publishedDateAndMsType\">" +
"<div class=\"msType\"></div>" +
"<div class=\"publishedDate\">24 Dec 1999</div>" +
"</div>"+
"<p>HelloWorld" +
"</div>";

private static final String publishedDateIdAfter = 
" <div id=\"page_content_container\">" +
" <p>HelloWorld" +
" </div>";

private static final String extraSpaceWithSpanBefore = 
"<div class=\"CMSCONTAINER j-content edt-flag\" id=\"page_content_container\">" +
"<span>   <p>Hello World</p>   </span>" +
"</div>";
private static final String extraSpaceWithSpanAfter = 
" <div id=\"page_content_container\">" +
" <span> <p>Hello World </p> </span>"+
" </div>";
private static final String noSpaceGenericBefore=
"<div class=\"CMSCONTAINER j-content edt-flag\" id=\"page_content_container\">" +
"<div>  <div><span class=\"pb_toc_link\"><br /><br /><b>Citation:</b> Atlas, J. S., Bay, P. S., and Cove, R. J.: A Long Title NO<sub>2</sub> Yes It Is, Abbrev. More. Tech., 8, 3-15, doi:10.1234/amt-8-123-2015, 2015.</span></div> </div> </div>";
private static final String extraSpaceGenericBefore=
"<div class=\"CMSCONTAINER j-content edt-flag\" id=\"page_content_container\">" +
"  <div>  <div>   <span class=\"pb_toc_link\"><br /><br /><b>Citation:</b> Atlas, J. S., Bay, P. S., and Cove, R. J.: A Long Title NO<sub>2</sub> Yes It Is, Abbrev. More. Tech., 8, 3-15, doi:10.1234/amt-8-123-2015, 2015.</span>  </div>  </div>  </div>";
private static final String extraSpaceGenericAfter=
" <div id=\"page_content_container\">" +
" <div> <div> <span class=\"pb_toc_link\"> <br /> <br /> <b>Citation:</b> Atlas, J. S., Bay, P. S., and Cove, R. J.: A Long Title NO<sub>2</sub> Yes It Is, Abbrev. More. Tech., 8, 3-15, doi:10.1234/amt-8-123-2015, 2015.</span> </div> </div> </div>";
private static final String extraneousDatesBefore=
"<div class=\"CMSCONTAINER j-content edt-flag\" id=\"page_content_container\">" +
"<div class=\"articleDates\">Received: 13 November 2013 &ndash; Published in The Beautiful Frisbee Discuss.: 06 January 2014 <br/>Revised: 17 April 2014 &ndash; Accepted: 22 April 2014 &ndash; Published: 03 June 2014 </div>"+
"Hello World </div>";
private static final String extraneousDatesAfter=
" <div id=\"page_content_container\">" +
"Hello World </div>";

private static final String volumes_toc=
"<div class=\"CMSCONTAINER j-content edt-flag\" id=\"page_content_container\">" +
"<div id=\"generator\" class=\"level1Toc\">" +
   "<div class=\"grid-container\"></div>" +
"</div></div>";

private static final String volumes_toc_filtered=
" <div id=\"page_content_container\">" +
" </div>";

private static final String issue_toc=
"<div id=\"page_content_container\">" +
    "<div id=\"generator\" class=\"level2Toc\">" +
    "<div class=\"grid-container\"></div></div></div>";

private static final String issue_toc_filtered=
" <div id=\"page_content_container\">" +
    " <div id=\"generator\" class=\"level2Toc\">" +
    " <div class=\"grid-container\"> </div>" +
 " </div> </div>";

  public void testHashFiltering() throws Exception {
    InputStream inA;
    InputStream inB;
    
    /* Check basic include/exclude functionality */
    inA = hfact.createFilteredInputStream(mau, new StringInputStream(includeBit),
        ENC);
   assertEquals(includeBitFiltered,StringUtil.fromInputStream(inA));
    
    /* Check basic include/exclude functionality */
    inA = hfact.createFilteredInputStream(mau, new StringInputStream(basicLayout),
        ENC);
    assertEquals(basicLayoutFiltered,StringUtil.fromInputStream(inA));

    /* remove <script> <noscript> and comments <!-- --> */
    inA = hfact.createFilteredInputStream(mau, new StringInputStream(scriptsAndComments),
        ENC);
    assertEquals(scriptsAndCommentsFiltered,StringUtil.fromInputStream(inA));

    inA = hfact.createFilteredInputStream(mau, new StringInputStream(whiteSpacesV1),
        ENC);
    inB = hfact.createFilteredInputStream(mau, new StringInputStream(whiteSpacesV2),
        ENC);
    assertEquals(StringUtil.fromInputStream(inA), StringUtil.fromInputStream(inB));
    
    //serving up slightly different files 
    inA = hfact.createFilteredInputStream(mau, new StringInputStream(noSpanStyleHtml),
        ENC);
    inB = hfact.createFilteredInputStream(mau, new StringInputStream(spanStyleHtml),
        ENC);
    assertEquals(StringUtil.fromInputStream(inA), StringUtil.fromInputStream(inB));
    
    /* remove contents from the home_url index page <!-- --> */
    inA = hfact.createFilteredInputStream(mau, new StringInputStream(genericIndexContent),
        ENC);
    assertEquals(genericIndexContentFiltered,StringUtil.fromInputStream(inA));


    /* remove the div class="publishedDateAndMsType" */
    inA = hfact.createFilteredInputStream(mau, new StringInputStream(publishedDateIdBefore), ENC);
    assertEquals(publishedDateIdAfter,StringUtil.fromInputStream(inA));
    
    /* remove the extra space before end of </span> and after a <span> " */
    inA = hfact.createFilteredInputStream(mau, new StringInputStream(extraSpaceWithSpanBefore), ENC);
    assertEquals(extraSpaceWithSpanAfter,StringUtil.fromInputStream(inA));
   
    /* check of adding space(s) between "><" before whitespace filter */
    inA = hfact.createFilteredInputStream(mau, new StringInputStream(extraSpaceGenericBefore), ENC);
    inB = hfact.createFilteredInputStream(mau, new StringInputStream(noSpaceGenericBefore), ENC);
    assertEquals(StringUtil.fromInputStream(inA),StringUtil.fromInputStream(inB));

    /* remove the div class="articleDates" */
    inA = hfact.createFilteredInputStream(mau, new StringInputStream(extraneousDatesBefore), ENC);
    assertEquals(extraneousDatesAfter,StringUtil.fromInputStream(inA));

    /* remove the base_url/volumes.html volumes listing" */
    inA = hfact.createFilteredInputStream(mau, new StringInputStream(volumes_toc), ENC);
    assertEquals(volumes_toc_filtered,StringUtil.fromInputStream(inA));

    /* don't remove the issue article toc */
    inA = hfact.createFilteredInputStream(mau, new StringInputStream(issue_toc), ENC);
    assertEquals(issue_toc_filtered,StringUtil.fromInputStream(inA));
    
  }
    
}