/*  $Id$
 
 Copyright (c) 2000-2017 Board of Trustees of Leland Stanford Jr. University,

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

package org.lockss.plugin.silverchair.dup;

import org.lockss.test.LockssTestCase;
import org.lockss.test.MockArchivalUnit;
import org.lockss.test.StringInputStream;
import org.lockss.util.Constants;
import org.lockss.util.StringUtil;

import java.io.InputStream;

public class TestDupScHtmlCrawlFilterFactory extends LockssTestCase {
  private DupScHtmlCrawlFilterFactory fact;
  private MockArchivalUnit mau;

  public void setUp() throws Exception {
    super.setUp();
    fact = new DupScHtmlCrawlFilterFactory();
  }
  
  private static final String anchor = 
      "<h2>Notes</h2>\n" +
      "<div class=\"master-header\">master header content</div>\n" +
      "<div class=\"widget-SitePageHeader\">widget-SitePageHeader content</div>\n" +
      "<div class=\"widget-SitePageFooter\">widget-SitePageFooter content</div>\n" +
      "<div id=\"InfoColumn\">InfoColumn content</div>\n" +
      "<div id=\"Sidebar\"><div class=\"sidebar-widget_wrap\">inside links</div></div>\n" +
      "<a class=\"download-slide\"\">download-slide link</a>\n" +
      "<div class=\"issue-browse-top-sec\">issue-browse-top content</div>\n" +
      "<a class=\"nav-link-sec\">nav-link content</a>\n" +
      "<div class=\"al-author-info-wrap\">al-author-info-wrap content</div>\n" +
      "<div class=\"all-issues\">all-issues content</div>\n";
              
  private static final String anchorFiltered = 
      "<h2>Notes</h2>\n\n\n\n\n\n\n\n\n\n\n";
  
  /*
   *  Compare Html and HtmlHashFiltered
   */
  public void testAnchor() throws Exception {
    InputStream actIn = fact.createFilteredInputStream(mau,
        new StringInputStream(anchor), Constants.DEFAULT_ENCODING);
    assertEquals(anchorFiltered, StringUtil.fromInputStream(actIn));
  }
  

}
