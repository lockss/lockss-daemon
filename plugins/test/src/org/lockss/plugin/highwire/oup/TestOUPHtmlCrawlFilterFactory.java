/*
 * $Id: TestAPSHtmlCrawlFilterFactory.java 39864 2015-02-18 09:10:24Z thib_gc $
 */

/*

Copyright (c) 2000-2015 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.plugin.highwire.oup;

import java.io.*;

import org.lockss.util.*;
import org.lockss.test.*;

public class TestOUPHtmlCrawlFilterFactory extends LockssTestCase {
  static String ENC = Constants.DEFAULT_ENCODING;
  
  private OUPHtmlCrawlFilterFactory fact;
  private MockArchivalUnit mau;
  
  @Override
  public void setUp() throws Exception {
    super.setUp();
    fact = new OUPHtmlCrawlFilterFactory();
    mau = new MockArchivalUnit();
  }
  
  private static final String withHeader = "<div id=\"page\">" +
      "<header id=\"section-header\" class=\"section section-header\">" +
      "</header>\n" +
      "</div>";
  
  private static final String withoutHeader = "<div id=\"page\">" +
      "\n" +
      "</div>";
  
  private static final String withFooter = "<div id=\"page\">" +
      "<footer class=\"section section-footer\" id=\"section-footer\">\n" + 
      "</footer>\n" +
      "</div>";
  
  private static final String withoutFooter = "<div id=\"page\">" +
      "\n" +
      "</div>";
  
  
  public void testFiltering() throws Exception {
    InputStream inA;
    String a;
    
    // header
    inA = fact.createFilteredInputStream(mau, new StringInputStream(withHeader),
        Constants.DEFAULT_ENCODING);
    a = StringUtil.fromInputStream(inA);
    assertEquals(withoutHeader, a);
    
    // footer
    inA = fact.createFilteredInputStream(mau, new StringInputStream(withFooter),
        Constants.DEFAULT_ENCODING);
    a = StringUtil.fromInputStream(inA);
    assertEquals(withoutFooter, a);
    
  }
  
}
