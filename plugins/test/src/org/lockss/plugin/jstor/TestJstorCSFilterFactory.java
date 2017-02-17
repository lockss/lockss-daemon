/*  $Id$

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

package org.lockss.plugin.jstor;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.PrintWriter;

import org.lockss.test.LockssTestCase;
import org.lockss.test.MockArchivalUnit;
import org.lockss.test.StringInputStream;
import org.lockss.util.Constants;
import org.lockss.util.IOUtil;
import org.lockss.util.StringUtil;

public class TestJstorCSFilterFactory extends LockssTestCase {
  private JstorCSHtmlHashFilterFactory hashfact;
  private JstorCSHtmlCrawlFilterFactory crawlfact;
  private MockArchivalUnit mau;

  public void setUp() throws Exception {
    super.setUp();
    hashfact = new JstorCSHtmlHashFilterFactory();
    crawlfact = new JstorCSHtmlCrawlFilterFactory();
  }

  // ?? use this block when manifest pages are up  
  private static final String manifestHtml =
      "    <html>" +
          "<meta name=\"robots\" value=\"noindex,nocache\"/>" +
          "<body>" +
          "<h1>Clockss App Manifest - Journal Issues</h1>\n" +
          "<ul>\n" +
          "  <li><a href=\"http://www.jstor.org/stable/10.2972/hesperia.84.issue-1\">10.2972/hesperia.84.issue-1</a></li>\n" +
          "</ul>\n" +
          "<p>CLOCKSS system has permission to ingest, preserve, and serve this Archival Unit.</p>" +
          "</body>" +
          "</html>";


  private static final String manifestHtmlFiltered =
      "<ul>\n" +
          "  <li><a href=\"http://www.jstor.org/stable/10.2972/hesperia.84.issue-1\">10.2972/hesperia.84.issue-1</a></li>\n" +
          "</ul>";


  private static final String html1 = 
      "<div class=\"toc-view\">" +
          "<ul id=\"export-bulk-drop\" data-dropdown-content class=\"export_citations f-dropdown content txtL\">" +
          "    <li class=\"export_citations mvm\">" +
          "        Using RefWorks? <a href=\"http://support.jstor.org/\" target=\"_blank\" aria-disabled=\"true\">" +
          "Find out how it works with JSTOR</a>" +
          "    </li>" +
          "</ul>" +
          "</div>";

  private static final String html2 =
      "<div class=\"toc-view\">" +
          "<div id=\"journal_info_drop\" class=\"f-dropdown content medium txtL \" " +
          "data-dropdown-content aria-hidden=\"true\" tabindex=\"-1\" aria-autoclose=\"false\">" +
          "    <div class=\"journal lookslikeh2 drop-content-title\">Foo: lll</div>" +
          "    <div class=\"journal_description mtm\"><strong>Description:</strong> <i>Foo</i> is published quarterly." +
          "</div>" +
          "<div class=\"coverage-period mtm\"><strong>Coverage:</strong> 1932-2016 (Vol. 1 - Vol. 85, No. 4)</div>" +
          "<div class=\"moving-wall mtm\">" +
          "    <dl class=\"accordion\" data-accordion>" +
          "        <dt class=\"visuallyhidden\">Moving Wall</dt>" +
          "        <dd class=\"accordion-navigation\">" +
          "            <strong>Moving Wall:</strong> 3 years <a href=\"#panel1\">(What is the moving wall?)</a>" +
          "            <div id=\"panel1\" class=\"content mtm prl\">" +
          "                <dl>" +
          "                    <dt class=\"hide\">Moving Wall</dt>" +
          "                    <dd class=\"alert-box secondary\">" +
          "                        <p>The \"moving wall\" represents</p>" +
          "                    </dd>" +
          "                </dl>" +
          "            </div>" +
          "            " +
          "        </dd>" +
          "    </dl>" +
          "</div>" +     
          "</div></div>";
  private static final String html2_filtered =
      "<div class=\"toc-view\"></div>" +
          "<div class=\"journal_description mtm\"><strong>Description:</strong> <i>Foo</i> is published quarterly." +
          "</div>";
  /*
   *  Compare Html and HtmlFiltered
   */

  public void testManifestFiltering() throws Exception {
    InputStream actIn = hashfact.createFilteredInputStream(mau,
        new StringInputStream(manifestHtml), Constants.DEFAULT_ENCODING);
    assertEquals(manifestHtmlFiltered, StringUtil.fromInputStream(actIn));
  }

  public void testTocFiltering() throws Exception {
    InputStream actIn = hashfact.createFilteredInputStream(mau,
        new StringInputStream(html1), Constants.DEFAULT_ENCODING);
    assertEquals("<div class=\"toc-view\"></div>", StringUtil.fromInputStream(actIn));
    actIn = hashfact.createFilteredInputStream(mau,
        new StringInputStream(html2), Constants.DEFAULT_ENCODING);
    assertEquals(html2_filtered, StringUtil.fromInputStream(actIn)); 
  }
  

}
