/*
 * $Id$
 */

/* Copyright (c) 2000-2010 Board of Trustees of Leland Stanford Jr. University, all rights reserved.

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
package org.lockss.plugin.atypon.ammonsscientific;


import java.io.InputStream;
import org.lockss.test.*;
import org.lockss.util.Constants;
import org.lockss.util.Logger;
import org.lockss.util.StringUtil;
import org.lockss.plugin.atypon.BaseAtyponHtmlHashFilterFactory;

public class TestAmmonsScientificHashHtmlFilterFactory extends LockssTestCase{
  private BaseAtyponHtmlHashFilterFactory filt;
  private MockArchivalUnit mau;

  public void setUp() throws Exception {
    super.setUp();
    filt = new AmmonsScientificHtmlHashFilterFactory();
    mau = new MockArchivalUnit();
  }

  // testing removal of the institution-banner-text line and the stuff within <script> </script>
  private static final String withHeader =
    "<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Strict//EN\" \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd\">\n" +
    "<html>\n" +
    "<head>\n" +	 
    "<div id=\"header\">" +
    "<span id=\"institution-banner-text\"><span class=\"institutionBannerText\">STANFORD UNIVERSITY</span> <img id=\"accessLogo\" src=\"/userimages/8552/banner\" alt=\"STANFORD UNIVERSITY\"  />  <br><br></span>" +		
    "</div>" +
    "\n</head>\n" +
    "</html>";

  private static final String withoutHeader =
    "<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Strict//EN\" \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd\"> " +
    "<html> " +
    "</html>";
  private static final String withScript=
    "<script type=\"text/javascript\"> <!-- // hide it from old browsers  var anyDbId = -1; //stop hiding --> </script>" +
    "<td><script type=\"text/javascript\">" +
    "genSfxLinks('s0', '', '10.2466/04.10.15.PR0.108.1.3-13');"+
    "</script></td>Hello World";
  private static final String withoutScript=
    "<td></td>Hello World";
  
  private static final String accessIconHtml=
      "<table border=\"0\" width=\"100%\" class=\"articleEntry\">" +
          "<tr>" +
          "<td align=\"right\" valign=\"top\" width=\"18\">" +
          "<input type=\"checkbox\" name=\"doi\" value=\"10.2466/30.24.PMS.117xxxxx\" />" +
          "<br />" +
          "<img src=\"/templates/jsp/_style2/_premium/images/access_no.gif\" alt=\"No Access\" title=\"No Access\" class=\"accessIcon noAccess\" />" +
          "</td>" +
          "<td valign=\"top\">" +
          "<div class=\"art_title\">" +
          "<span class=\"hlFld-Title\">SOCCER" +
          "</span>" +
          "</div>" +
          "<a class=\"entryAuthor\" href=\"/action/doSearch?Contrib=Green\">Green</a>" +
          "<div class=\"art_meta\">Skills Volume 117, Issue 1, August 2013:  1-2.</div>" +
          "<a class=\"ref nowrap \" href=\"/doi/abs/10.2466/30.24.PMS.117xxxxx\">Abstract" +
          "</a> | " +
          "<a class=\"ref nowrap\" href=\"/doi/full/10.2466/30.24.PMS.117xxxxx\">Full Text" +
          "</a> | " +
          "<a class=\"ref nowrap pdf\" target=\"_blank\" title=\"Opens new window\" href=\"/doi/pdf/10.2466/30.24.PMS.117xxxxx\">PDF (152 KB)" +
          "</a> | " +
          "<a class=\"ref nowrap pdfplus\" target=\"_blank\" title=\"Opens new window\" href=\"/doi/pdfplus/10.2466/30.24.PMS.117xxxxx\">PDF Plus (203 KB)" +
          "</a> " +
          "</td>" +
          "</tr>" +
          "</table>";
  
  private static final String accessIconHtmlFiltered=
      "<table border=\"0\" width=\"100%\" class=\"articleEntry\">" +
          "<tr>" +
          "<td align=\"right\" valign=\"top\" width=\"18\">" +
          "<input type=\"checkbox\" name=\"doi\" value=\"10.2466/30.24.PMS.117xxxxx\" />" +
          "<br />" +
          "</td>" +
          "<td valign=\"top\">" +
          "<div class=\"art_title\">" +
          "<span class=\"hlFld-Title\">SOCCER" +
          "</span>" +
          "</div>" +
          "<a class=\"entryAuthor\" href=\"/action/doSearch?Contrib=Green\">Green</a>" +
          "<div class=\"art_meta\">Skills Volume 117, Issue 1, August 2013: 1-2.</div>" +
          "<a class=\"ref nowrap \" href=\"/doi/abs/10.2466/30.24.PMS.117xxxxx\">Abstract" +
          "</a> | " +
          "<a class=\"ref nowrap\" href=\"/doi/full/10.2466/30.24.PMS.117xxxxx\">Full Text" +
          "</a> | " +
          "<a class=\"ref nowrap pdf\" target=\"_blank\" title=\"Opens new window\" href=\"/doi/pdf/10.2466/30.24.PMS.117xxxxx\">" +
          "</a> | " +
          "<a class=\"ref nowrap pdfplus\" target=\"_blank\" title=\"Opens new window\" href=\"/doi/pdfplus/10.2466/30.24.PMS.117xxxxx\">" +
          "</a> " +
          "</td>" +
          "</tr>" +
          "</table>";
  
  private static final String accessLegend = 
      "<div class=\"accessLegend\">" +
          "            <div class=\"accessIcon_free\">"+
          "                <img src=\"/templates/jsp/_style2/_premium/_ammons/images/access_free.gif\" />" +
          "                Denotes Open Access Content" +
          "            </div>" +
          "    </div>";

  public void testHeader() throws Exception {
    InputStream inTest;

    inTest = filt.createFilteredInputStream(mau,
        new StringInputStream(withHeader), Constants.DEFAULT_ENCODING);

    assertEquals( withoutHeader,
        StringUtil.fromInputStream(inTest));
  }	
  public void testScript() throws Exception {
    InputStream inTest;

    inTest = filt.createFilteredInputStream(mau,
        new StringInputStream(withScript), Constants.DEFAULT_ENCODING);

    assertEquals( withoutScript,
        StringUtil.fromInputStream(inTest));
  }
  public void testAccessIcon() throws Exception {
    InputStream inTest;

    inTest = filt.createFilteredInputStream(mau,
        new StringInputStream(accessIconHtml), Constants.DEFAULT_ENCODING);

    assertEquals( accessIconHtmlFiltered,
        StringUtil.fromInputStream(inTest));
    
    // check div class=accessLegend gets filtered out
    inTest = filt.createFilteredInputStream(mau,
        new StringInputStream(accessLegend), Constants.DEFAULT_ENCODING);
    assertEquals("",
        StringUtil.fromInputStream(inTest));
  }
}

