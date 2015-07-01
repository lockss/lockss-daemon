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

  private static final String includeBit=
      "<div class=\"top\">" +
      "<div id=\"page_content_container\">KEEPME</div>" +
      "</div>";
  private static final String includeBitFiltered=
      "<div id=\"page_content_container\">KEEPME</div>";

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
          "<div class=\"foo\" id=\"page_content_container\">GOODSTUFF</div>";
  
  private static final String minimumBeginning =
      "<html>"+ 
          "<head>HEAD</head>" + 
          "<body>" +
          "<!-- $$BODY_HEAD$$ -->" +
          "<div class=\"foo\" id=\"page_content_container\">";
  private static final String minimumEnding =
      "</div></body></html>";
  private static final String minimumEndingFiltered = "</div>";
  
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
      "<div class=\"foo\" id=\"page_content_container\">" +
      "</tr></table> " + minimumEndingFiltered;
  
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
  

  public void testFiltering() throws Exception {
    InputStream inA;
    InputStream inB;
    
    /* Check basic include/exclude functionality */
    inA = fact.createFilteredInputStream(mau, new StringInputStream(includeBit),
        ENC);
    assertEquals(includeBitFiltered,StringUtil.fromInputStream(inA));
    
    /* Check basic include/exclude functionality */
    inA = fact.createFilteredInputStream(mau, new StringInputStream(basicLayout),
        ENC);
    assertEquals(basicLayoutFiltered,StringUtil.fromInputStream(inA));

    /* remove <script> <noscript> and comments <!-- --> */
    inA = fact.createFilteredInputStream(mau, new StringInputStream(scriptsAndComments),
        ENC);

    assertEquals(scriptsAndCommentsFiltered,StringUtil.fromInputStream(inA));

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
    

    
  }
}


