/*
 * $Id: TestAmmonsScientificHashHtmlFilterFactory.java,v 1.1 2013-08-06 21:24:24 aishizaki Exp $
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
    filt = new BaseAtyponHtmlHashFilterFactory();
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
    "<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Strict//EN\" \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd\">\n" +
    "<html>\n" +
    "<head>\n" +
    "\n</head>\n" +
    "</html>";
  private static final String withScript=
    "<script type=\"text/javascript\"> <!-- // hide it from old browsers  var anyDbId = -1; //stop hiding --> </script>" +
    "<td><script type=\"text/javascript\">" +
    "genSfxLinks('s0', '', '10.2466/04.10.15.PR0.108.1.3-13');"+
    "</script></td>Hello World";
  private static final String withoutScript=
    "<td></td>Hello World";


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

}

