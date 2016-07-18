/*
 * $Id$
 */
/*

Copyright (c) 2000-2016 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.plugin.copernicus;

import java.io.*;
import org.lockss.util.*;
import org.lockss.test.*;

public class TestCopernicusHtmlCrawlFilterFactory extends LockssTestCase {
  static String ENC = Constants.DEFAULT_ENCODING;

  private CopernicusHtmlCrawlFilterFactory fact;
  private MockArchivalUnit mau;

  public void setUp() throws Exception {
    super.setUp();
    fact = new CopernicusHtmlCrawlFilterFactory();
    mau = new MockArchivalUnit();
  }
  private static final String withHighlights = 
      "<h2>Highlight articles</h2>" +
      "<div id=\"highlight_articles\"><!--// Highlights start-->" +
      "<!-- ITEM 12-->" +
      "<div class=\"j-news-item\"><span class=\"j-news-item-thumb\">" +
      "<a href=\"http://www.test-journal.net/9/2689/2016/\">" +
      "<img alt=\"\" cofileid=\"27325\" src=\"http://www.test-journal.net/amt-9-2689-2016-f07_50x50.png\" />" +
      "</a>             </span>" +
      "<div class=\"j-news-item-content\">" +
      "<div class=\"j-news-item-header\">" +
      "<!-- the following link is what we're filtering -->" +
      "<div><a href=\"http://www.test-journal.net/9/2689/2016/\">Test Title</a>" +
      "<a class=\"toggle-summary-link\" onclick=\"MM_showHideLayers('Layer12','','show')\" href=\"#12\"><img alt=\"\" cofileid=\"20138\" src=\"http://www.test-journal.net/AMT_icon_summary.png\" />" +
      "</a></div>" +
      "<span class=\"j-news-item-date\">28 Jun 2016</span></div>" +
      "<div id=\"Layer12\" class=\"expanded-text\">" +
      "<p><a onclick=\"MM_showHideLayers('Layer12','','show')\" href=\"#12\"><img border=\"0\" alt=\"\" style=\"padding-left: 15px; float: right\" src=\"https://contentmanager.hello.org/9726/445/locale/ssl\" /></a>A very short article.</p>" +
      "</div>" +
      "<p class=\"j-news-item-body\">A. D. Abacus, and S. D. Calculator</p>" +
      "</div>" +
      "</div>" +
      "</div>";

  private static final String withoutHighlights = 
      "<h2>Highlight articles</h2>" ;
      
  public void testFiltering() throws Exception {
    InputStream inStream;
    inStream = fact.createFilteredInputStream(mau,
        new StringInputStream(withHighlights),
        Constants.DEFAULT_ENCODING);
    assertEquals(withoutHighlights, StringUtil.fromInputStream(inStream));
  }

}
