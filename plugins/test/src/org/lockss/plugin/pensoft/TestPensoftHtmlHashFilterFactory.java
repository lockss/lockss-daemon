/*
 * $Id: TestPensoftHtmlHashFilterFactory.java,v 1.1 2013-03-20 22:03:27 aishizaki Exp $
 */

/*

 Copyright (c) 2000-2006 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.plugin.pensoft;

import java.io.*;

import org.lockss.util.*;
import org.lockss.daemon.PluginException;
import org.lockss.extractor.MetadataField;
import org.lockss.test.*;

public class TestPensoftHtmlHashFilterFactory extends LockssTestCase {
  static String ENC = Constants.DEFAULT_ENCODING;

  private PensoftHtmlHashFilterFactory fact;
  private MockArchivalUnit mau;

  public void setUp() throws Exception {
    super.setUp();
    fact = new PensoftHtmlHashFilterFactory();
    mau = new MockArchivalUnit();
  }
 
  private static final String viewCounterHtmlHashA =
    "<tr>"+
      "<td class=\"green2\" valign=\"top\">"+
      "<b>doi: </b>"+
      "10.3897/compcytogen.v3i1.2"+
      "<br><b>Published:</b> 06.08.2009<br /><br /><b>Viewed by: </b>1106"+
      "<td class=\"more3\"><font class=\"newsdata\">Abstract</font><br><br>"+
      "Hello World"+
      "<p align=\"right\">Full text:"+
      "<a class=\"more3\" href='inc/journals/download.php?fileId=1794&fileTable=J_GALLEYS'>PDF</a> </p>"+
      "</td></tr>";
 
  private static final String viewCounterHtmlHashAFiltered =
    "<tr>"+
    "<td class=\"more3\"><font class=\"newsdata\">Abstract</font><br><br>"+
    "Hello World"+
    "<p align=\"right\">Full text:"+
    "<a class=\"more3\" href='inc/journals/download.php?fileId=1794&fileTable=J_GALLEYS'>PDF</a> </p>"+
    "</td></tr>";  
  private static final String viewCounterHtmlHashB =
    "<tr>"+
      "<td valign=\"top\" width=\"13\"><img src=\"img/kv.gif\" vspace=\"4\" width=\"5\" height=\"5\"></td>"+
      "<td width=\"165\" class=\"green\">Viewed by : <span class=more3 >1106</span></td>"+
    "</tr>";

  private static final String viewCounterHtmlHashBFiltered = 
    "<tr>"+
    "<td valign=\"top\" width=\"13\"><img src=\"img/kv.gif\" vspace=\"4\" width=\"5\" height=\"5\"></td>"+
    "</tr>";
 
  public void testFiltering() throws Exception {
    InputStream inA, inB, inC;

    /* viewed-by test  */ 
    inA = fact.createFilteredInputStream(mau, 
          new StringInputStream(viewCounterHtmlHashA), ENC);
    String filtStrA = StringUtil.fromInputStream(inA);
    assertEquals(viewCounterHtmlHashAFiltered, filtStrA);
  
    inB = fact.createFilteredInputStream(mau, 
        new StringInputStream(viewCounterHtmlHashB), ENC);    
    String filtStrB = StringUtil.fromInputStream(inB);
    assertEquals(viewCounterHtmlHashBFiltered, filtStrB);
   
  }
}