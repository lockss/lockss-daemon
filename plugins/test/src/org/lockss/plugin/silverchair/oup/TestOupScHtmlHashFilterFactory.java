/*
 * $Id$
 */

/*

Copyright (c) 2017 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.plugin.silverchair.oup;

import java.io.*;

import org.lockss.util.*;
import org.lockss.test.*;

public class TestOupScHtmlHashFilterFactory extends LockssTestCase {
  static String ENC = Constants.DEFAULT_ENCODING;
  
  private OupScHtmlHashFilterFactory fact;
  private MockArchivalUnit mau;
  
  @Override
  public void setUp() throws Exception {
    super.setUp();
    fact = new OupScHtmlHashFilterFactory();
    mau = new MockArchivalUnit();
  }
  
  private static final String notIncluded = "<div class=\"ad-banner\">\n" +
    "<div class=\"widget widget-AdBlock widget-instance-HeaderAd\">\n" +
    "<div id=\"adBlockHeader\" style=' '>\n" +
    "<script>\n" +
    "googletag.cmd.push(function () { googletag.display('adBlockHeader'); });\n" +
    "</script>/n" +
    "</div>\n" +
    "</div>\n" +
    "</div>\n";
  
  private static final String notIncludedFiltered = "";
  
  private static final String withLinksComments = "<div class=\"article-body\">\n" +
      "Hello World <div class=\"graphic-wrap\"><a href=\"link\">link text</a></div> More Text\n" + 
      "<div class=\"comment-body__wrap\">\n" + 
      "Comments" + 
      "</div>\n" + 
      "</div>";
  
  private static final String withoutLinksComments = "<div> Hello World More Text </div>";
  
  /*
   */
  public void testFiltering() throws Exception {
    InputStream inA;
    String a;
    
    // header
    inA = fact.createFilteredInputStream(mau, new StringInputStream(notIncluded),
        Constants.DEFAULT_ENCODING);
    a = StringUtil.fromInputStream(inA);
    assertEquals(notIncludedFiltered, a);
    
    // footer
    inA = fact.createFilteredInputStream(mau, new StringInputStream(withLinksComments),
        Constants.DEFAULT_ENCODING);
    a = StringUtil.fromInputStream(inA);
    assertEquals(withoutLinksComments, a);
    
    
  }
  
}
