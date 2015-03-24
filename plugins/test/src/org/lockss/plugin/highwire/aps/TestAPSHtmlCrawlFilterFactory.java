/*
 * $Id$
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

package org.lockss.plugin.highwire.aps;

import java.io.*;

import org.lockss.util.*;
import org.lockss.test.*;

public class TestAPSHtmlCrawlFilterFactory extends LockssTestCase {
  static String ENC = Constants.DEFAULT_ENCODING;
  
  private APSHtmlCrawlFilterFactory fact;
  private MockArchivalUnit mau;
  
  @Override
  public void setUp() throws Exception {
    super.setUp();
    fact = new APSHtmlCrawlFilterFactory();
    mau = new MockArchivalUnit();
  }
  
  private static final String withPager = "<div id=\"page\">" +
      "<div class=\"panel-pane pane-highwire-node-pager\">\n" + 
      "  <div class=\"pane-content\">\n" + 
      "    <div class=\"pager highwire-pager pager-mini clearfix highwire-node-pager " +
      "highwire-article-pager\">" +
      "<span class=\"pager-prev\">" +
      "<a class=\"pager-link-prev link-icon\" rel=\"prev\" title=\"receptor neurons\"" +
      " href=\"/content/4/1/E1\">" +
      "<i class=\"icon-circle-arrow-left\"></i> Previous</a></span>" +
      "<span class=\"pager-next\">" +
      "<a class=\"pager-link-next link-icon link-icon-right\" rel=\"next\" title=\"insulin\"" +
      " href=\"/content/5/1/E1\">Next <i class=\"icon-circle-arrow-right\"></i></a></span>" +
      "</div>  </div>\n" + 
      "  </div>\n" +
      "</div>";
  
  private static final String withoutPager = "<div id=\"page\">" +
      "\n" +
      "</div>";
  
  private static final String withAuTT = "<div id=\"page\">\n" +
      "<div class=\"author-tooltip-name\">" +
      "<span class=\"nlm-given-names\">Nae</span>" +
      " <span class=\"nlm-surname\">Soup</span> </div>" +
      "</div>";
  
  private static final String withoutAuTT = "<div id=\"page\">" +
      "\n" +
      "</div>";
  
  private static final String withHwLink = "<div id=\"page\">" +
      "<a class=\"hw-link hw-abstract\"" +
      " href=\"http://ajpregu.physiology.org/content/304/3/R218.abstract\">View Abstract</a>" +
      "</div>";
  
  private static final String withoutHwLink = "<div id=\"page\">" +
      "</div>";
  
  
  public void testFiltering() throws Exception {
    InputStream inA;
    String a;
    
    // node pager
    inA = fact.createFilteredInputStream(mau, new StringInputStream(withPager),
        Constants.DEFAULT_ENCODING);
    a = StringUtil.fromInputStream(inA);
    assertEquals(withoutPager, a);
    
    // author tooltip
    inA = fact.createFilteredInputStream(mau, new StringInputStream(withAuTT),
        Constants.DEFAULT_ENCODING);
    a = StringUtil.fromInputStream(inA);
    assertEquals(withoutAuTT, a);
    
    // HwLink
    inA = fact.createFilteredInputStream(mau, new StringInputStream(withHwLink),
        Constants.DEFAULT_ENCODING);
    a = StringUtil.fromInputStream(inA);
    assertEquals(withoutHwLink, a);
    
  }
  
}
