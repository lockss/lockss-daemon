/*

Copyright (c) 2000-2022, Board of Trustees of Leland Stanford Jr. University
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
