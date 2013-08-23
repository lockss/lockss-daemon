/*
 * $Id: TestAmPublicHealthAssocHtmlHashFilterFactory.java,v 1.2 2013-08-23 20:20:42 alexandraohlson Exp $
 */

/* Copyright (c) 2000-2013 Board of Trustees of Leland Stanford Jr. University, all rights reserved.

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
package org.lockss.plugin.atypon.apha;


import java.io.InputStream;
import org.lockss.test.*;
import org.lockss.util.Constants;
import org.lockss.util.Logger;
import org.lockss.util.StringUtil;

public class TestAmPublicHealthAssocHtmlHashFilterFactory extends LockssTestCase{
  private AmPublicHealthAssocHtmlHashFilterFactory filt;
  private MockArchivalUnit mau;

  public void setUp() throws Exception {
    super.setUp();
    filt = new AmPublicHealthAssocHtmlHashFilterFactory();
    mau = new MockArchivalUnit();
  }
  
  
  private static final String footerHtml =
      "<start><div id=\"footer\">" +
          "<!-- ============= start snippet ============= -->" +
          "<div><cite>American Journal of Public Health<span class=\"fontSize1\"></div>" +
          "<div>Print ISSN: 0090-0036 | Electronic ISSN: 1541-0048</div>" +
          "<div>Copyright © 2012 by the <a class=\"inserted\" target=\"_blank\" title=\"APHA home\" " +
          "href=\"http://www.apha.org\">American Public Health Association</a><span class=\"fontSize1\"></div>" +
          "<!-- ============= end snippet ============= -->" +
          "<div id=\"atyponNote\">" +
          "    Powered by <a href=\"http://www.atypon.com\">Atypon&reg; Literatum</a>" +
          "</div>" +
          "</div>" +
          "</div>" +
          "</body><end>";

  private static final String footerHtmlFiltered =
      "<start></div>" +
          "</body><end>";
  
  
  public void test_footerHtml() throws Exception {
    InputStream actIn = filt.createFilteredInputStream(mau,
        new StringInputStream(footerHtml),
        Constants.DEFAULT_ENCODING);

    assertEquals(footerHtmlFiltered, StringUtil.fromInputStream(actIn));
  }


}
