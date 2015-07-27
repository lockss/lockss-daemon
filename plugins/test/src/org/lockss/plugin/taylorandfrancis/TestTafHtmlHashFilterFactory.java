/*  $Id: TestBaseAtyponHtmlHashFilterFactory.java 39864 2015-02-18 09:10:24Z thib_gc $
 
 Copyright (c) 2000-2003 Board of Trustees of Leland Stanford Jr. University,

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

package org.lockss.plugin.taylorandfrancis;

import java.io.InputStream;

import org.lockss.test.LockssTestCase;
import org.lockss.test.MockArchivalUnit;
import org.lockss.test.StringInputStream;
import org.lockss.util.Constants;
import org.lockss.util.StringUtil;

public class TestTafHtmlHashFilterFactory extends LockssTestCase {
  private TafHtmlHashFilterFactory fact;
  private MockArchivalUnit mau;

  public void setUp() throws Exception {
    super.setUp();
    fact = new TafHtmlHashFilterFactory();
  }
  
  private static final String withPrevNext =
  "    <div class=\"overview borderedmodule-last\">\n" +
  "<div class=\"hd\">\n" +
"      <h2>\n" +
"      <a href=\"vol_47\">Volume 47</a>,\n\n" +
"<span style=\"float: right;margin-right: 5px\">\n" +
"      \n" +
"      \n" +
"<a href=\"/47/2\" title=\"Previous issue\">&lt; Prev</a>\n\n\n" +

"|\n\n\n" +
"<a href=\"47/4\" title=\"Next issue\">Next &gt;</a>\n" +
"</span>\n" +
"      </h2>\n" +
"  </div>\n";
  

 
  private static final String withoutPrevNext =
      " Volume 47 , ";
  
  
  /*
   *  Compare Html and HtmlHashFiltered
   */
  
  public void testWithPrevNext() throws Exception {
    InputStream actIn = fact.createFilteredInputStream(mau,
        new StringInputStream(withPrevNext), Constants.DEFAULT_ENCODING);
    assertEquals(withoutPrevNext, StringUtil.fromInputStream(actIn));
  }
  
  public void testRealContent() throws Exception {
    InputStream actIn = fact.createFilteredInputStream(mau, getResourceAsStream("tafcontent_2.html"), Constants.DEFAULT_ENCODING);
    InputStream actIn2 = fact.createFilteredInputStream(mau, getResourceAsStream("tafcontent_3.html"), Constants.DEFAULT_ENCODING);
    assertEquals(StringUtil.fromInputStream(actIn), StringUtil.fromInputStream(actIn2));
  }
  
}
