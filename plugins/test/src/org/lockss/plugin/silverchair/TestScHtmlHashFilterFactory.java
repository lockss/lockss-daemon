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

package org.lockss.plugin.silverchair;

import java.io.*;

import org.lockss.util.*;
import org.lockss.test.*;

public class TestScHtmlHashFilterFactory extends LockssTestCase {
  static String ENC = Constants.DEFAULT_ENCODING;
  
  private ScHtmlHashFilterFactory fact;
  private MockArchivalUnit mau;
  
  @Override
  public void setUp() throws Exception {
    super.setUp();
    fact = new ScHtmlHashFilterFactory();
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
  
  private static final String withArticleIcons = 
      "<div class=\"articleBodyContainer\">\n" +
      "Hello World " +
      "<span class=\"article-groups left-flag EditorsAward\">\n" + 
      "  <i class=\"icon-award\"></i><span>EDITOR'S AWARD</span>\n" + 
      "</span>" +
      "</div>\n" + 
      "<div class=\"leftColumn\">\n" +
      " More Text" +
      " <div class=\"access-state-logos all-viewports\">   \n" + 
      "  <span class=\"article-groups left-flag EditorsAward\">\n" + 
      "    <i class=\"icon-award\"></i><span>EDITOR'S AWARD</span>\n" + 
      "  </span>\n" + 
      "  <span class=\"article-accessType all-viewports left-flag FreeAccess\">Free</span>\n" + 
      " </div>\n" +
      "</div>\n";
  
  private static final String withoutArticleIcons = " Hello World More Text ";
  
  /*
   */
  public void testFiltering() throws Exception {
    InputStream inA;
    String a;
    
    // nothing kept
    inA = fact.createFilteredInputStream(mau, new StringInputStream(notIncluded),
        Constants.DEFAULT_ENCODING);
    a = StringUtil.fromInputStream(inA);
    assertEquals(notIncludedFiltered, a);
    
    // 
    inA = fact.createFilteredInputStream(mau, new StringInputStream(withArticleIcons),
        Constants.DEFAULT_ENCODING);
    a = StringUtil.fromInputStream(inA);
    assertEquals(withoutArticleIcons, a);
    
    
  }
  
}
