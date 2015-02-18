/*
 * $Id$
 */

/*

Copyright (c) 2000-2014 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.plugin.iop;

import java.io.*;

import org.lockss.util.*;
import org.lockss.test.*;

public class TestIOPScienceHtmlCrawlFilterFactory extends LockssTestCase {
  static String ENC = Constants.DEFAULT_ENCODING;
  
  private IOPScienceHtmlCrawlFilterFactory fact;
  private MockArchivalUnit mau;
  
  public void setUp() throws Exception {
    super.setUp();
    fact = new IOPScienceHtmlCrawlFilterFactory();
    mau = new MockArchivalUnit();
  }
  
  // "div",  "id", "rightCol"
  private static final String withRightCol = "<div id=\"page\">" +
      "<div id=\"rightCol\" class=\"rightColClass\">" +
      "    <a href=\"/content/early/recent\">Articles in Press</a>\n" +
      "</div>\n" + 
      "</div>";
  private static final String withoutRightCol = "<div id=\"page\">" +
      "\n" +
      "</div>";
  
  // div", "class", "alsoRead
  private static final String withAlsoRead = "<div id=\"page\">" +
      "<div id=\"idalsoRead\" class=\" alsoRead \">" +
      "    <a href=\"/content/early/recent\">Articles in Press</a>\n" +
      "</div>\n" + 
      "</div>";
  private static final String withoutAlsoRead = "<div id=\"page\">" +
      "\n" +
      "</div>";
  
  // Last 10 articles
  private static final String withLast10List = "<div id=\"page\">\n" +
      "<div id=\"idalsoRead\" class=\" alsoRead \">" +
      "    <a href=\"/content/early/recent\">Articles in Press</a>\n" +
      "</div>\n" + 
      "</div>";
  private static final String withoutLast10List = "<div id=\"page\">\n" +
      "\n" +
      "</div>";
  
  // Hidden in each reference link 
  private static final String withRef = "<dd>" +
      "<a href=\"?sid=IOPP%3Ajnl_ref&amp;id=doi%3A10.1051%2F0004-6361%3A20053695\" title=\"\">\n" +
      "</a>\n" +
      "</dd>";
  private static final String withoutRef = "<dd>" +
      "\n" +
      "</dd>";
  
  
  public void testFiltering() throws Exception {
    InputStream inA;
    String a;
    
    // div id rightCol
    inA = fact.createFilteredInputStream(mau, new StringInputStream(withRightCol),
        Constants.DEFAULT_ENCODING);
    a = StringUtil.fromInputStream(inA);
    assertEquals(withoutRightCol, a);
    
    // div class alsoRead
    inA = fact.createFilteredInputStream(mau, new StringInputStream(withAlsoRead),
        Constants.DEFAULT_ENCODING);
    a = StringUtil.fromInputStream(inA);
    assertEquals(withoutAlsoRead, a);
    
    // Last 10 articles
    inA = fact.createFilteredInputStream(mau, new StringInputStream(withLast10List),
        Constants.DEFAULT_ENCODING);
    a = StringUtil.fromInputStream(inA);
    assertEquals(withoutLast10List, a);
    
    // Hidden in each reference link
    inA = fact.createFilteredInputStream(mau, new StringInputStream(withRef),
        Constants.DEFAULT_ENCODING);
    a = StringUtil.fromInputStream(inA);
    assertEquals(withoutRef, a);
    
  }
  
}
