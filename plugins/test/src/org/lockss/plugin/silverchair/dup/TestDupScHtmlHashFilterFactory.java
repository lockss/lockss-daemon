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

package org.lockss.plugin.silverchair.dup;

import org.apache.commons.io.FileUtils;
import org.lockss.test.LockssTestCase;
import org.lockss.test.MockArchivalUnit;
import org.lockss.test.StringInputStream;
import org.lockss.util.Constants;
import org.lockss.util.StringUtil;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

public class TestDupScHtmlHashFilterFactory extends LockssTestCase {
  static String ENC = Constants.DEFAULT_ENCODING;
  
  private DupScHtmlHashFilterFactory fact;
  private MockArchivalUnit mau;
  
  @Override
  public void setUp() throws Exception {
    super.setUp();
    fact = new DupScHtmlHashFilterFactory();
    mau = new MockArchivalUnit();
  }

  private static final String notIncluded =
    "<script>\n" +
    "googletag.cmd.push(function () { googletag.display('adBlockHeader'); });\n" +
    "</script>\n" +
    "<div class=\"toolbar-wrap\">toolbar wrap content</div>\n" +
    "<div class=\"widget-items\">widget-items content</div>\n" +
    "<div class=\"article-groups\">article-groups content</div>\n" +
    "<h1 class=\"wi-article-title\">h1 title content</h1>\n" +
    "<h2 class=\"backreferences-title\">h2 title content</h1>\n" +
    "<div class=\"article\">\n" +
        "<div class=\"widget other-widget\">\n" +
          "<div id=\"ContentTab\" class=\"content active\">main content</div>\n" +
        "</div>\n" +
    "</div>";

  private static final String notIncludedFiltered = " main content ";

  public void testFiltering() throws Exception {
    InputStream inA;
    String a;

    inA = fact.createFilteredInputStream(mau, new StringInputStream(notIncluded),
        Constants.DEFAULT_ENCODING);
    a = StringUtil.fromInputStream(inA);
    log.debug3("result a = " + a);
    assertEquals(notIncludedFiltered, a);

  }
  
}
