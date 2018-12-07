/*  $Id$

 Copyright (c) 2018 Board of Trustees of Leland Stanford Jr. University,

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

import java.io.InputStream;

import org.lockss.test.LockssTestCase;
import org.lockss.test.MockArchivalUnit;
import org.lockss.test.StringInputStream;
import org.lockss.util.Constants;
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


  private static final String manifestHtmlFiltered0 =
      "<ul>\n" +
          "  <li><a href=\"https://www.jstor.org/stable/10.2972/hesperia.84.issue-1\">10.2972/hesperia.84.issue-1</a></li>\n" +
          "</ul>";
  private static final String manifestHtmlFiltered =
      "" +
          " 10.2972/hesperia.84.issue-1" +
          " ";


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
  private static final String html2_filtered0 =
      "<div class=\"toc-view\"></div>" +
          "<div class=\"journal_description mtm\"><strong>Description:</strong> <i>Foo</i> is published quarterly." +
          "</div>";
  private static final String html2_filtered =
      "" +
          " Description: Foo is published quarterly." +
          " ";
  
  private static final String ft_html = 
      "<body style=\"position: relative; min-height: 100%; top: 0px;\" class=\"shepherd-active\" data-shepherd-step=\"1\">\n" + 
      "  <div id=\"skipNav\">\n" + 
      "    <a class=\"visuallyhidden\" href=\"#content\">Skip to Main Content</a>\n" + 
      "  </div>\n" + 
      "  <nav>\n" + 
      "    <div class=\"top-bar ptxs\">\n" + 
      "    </div>\n" + 
      "  </nav>\n" + 
      "  <div id=\"content\" role=\"main\" class=\"content row\" data-equalizer=\"\">\n" + 
      "    <div class=\"small-12 xlarge-8 columns maincontent column-max-width\" data-equalizer-watch=\"\">\n" + 
      "      <div id=\"article_view_content\" class=\"article-view-content ptxl\">\n" + 
      "        <div class=\"row collapse\">\n" + 
      "          <div class=\"small-12 xlarge-8 xlarge-pull-4 columns\">\n" + 
      "            <ul id=\"content-tabs\" class=\"tabs\" data-tabs=\"sh3jds-tabs\" data-deep-link=\"true\" data-update-history=\"true\" role=\"tablist\">\n" + 
      "              <li class=\"tabs-title is-active\" id=\"page_scan_tab\" role=\"presentation\">\n" + 
      "                <a href=\"#full_text_tab_contents\" data-sc=\"tab:article tab\" role=\"tab\" id=\"full_text_tab_contents-label\" tabindex=\"0\">\n" + 
      "                Article\n" + 
      "                </a>\n" + 
      "              </li>\n" + 
      "              <li id=\"references_tab\" class=\"tabs-title\" role=\"presentation\">\n" + 
      "                <a href=\"#references_tab_contents\" role=\"tab\" id=\"references_tab_contents-label\" tabindex=\"-1\">References</a>\n" + 
      "              </li>\n" + 
      "            </ul>\n" + 
      "          </div>\n" + 
      "        </div>\n" + 
      "        <div class=\"row\">\n" + 
      "          <div class=\"small-12 columns\">\n" + 
      "            <div class=\"tabs-content\" data-tabs-content=\"content-tabs\">\n" + 
      "              <div id=\"full_text_tab_contents\" class=\"tabs-panel rendition\" role=\"tabpanel\">\n" + 
      "                <!--?xml version=\"1.0\" encoding=\"UTF-8\"?-->\n" + 
      "                <article data-article-type=\"research-article\" lang=\"eng\">\n" + 
      "                  <h1>Text of Geoffrey Chaucer</h1>\n" + 
      "                  <div class=\"abstract\">\n" + 
      "                    <p>text here.</p>\n" + 
      "                  </div>\n" + 
      "                  <div class=\"keywords\"><span class=\"label kwd-label\">Keywords:</span> KW </div>\n" + 
      "                  <div class=\"back\">\n" + 
      "                    <section class=\"references\">\n" + 
      "                      <h2>Notes</h2>\n" + 
      "                      <ol class=\"ref-list\">\n" + 
      "                        <li>\n" + 
      "                          <div class=\"ref-content\">\n" + 
      "                            <div class=\"ref-content\">\n" + 
      "                              <div class=\"citation\">Thanks to everyone for their help with this article.</div>\n" + 
      "                            </div>\n" + 
      "                          </div>\n" + 
      "                        </li>\n" + 
      "                      </ol>\n" + 
      "                    </section>\n" + 
      "                  </div>\n" + 
      "                </article>\n" + 
      "              </div>\n" + 
      "              <div id=\"references_tab_contents\" class=\"tabs-panel notranslate\" role=\"tabpanel\" aria-hidden=\"true\">\n" + 
      "                <div class=\"rendition reading-length\">\n" + 
      "                  <div class=\"back\">\n" + 
      "                    <section class=\"references\">\n" + 
      "                      <h2>Notes</h2>\n" + 
      "                      <ol class=\"ref-list\">\n" + 
      "                        <li>\n" + 
      "                          <div class=\"ref-content\">\n" + 
      "                            <div class=\"ref-content\">\n" + 
      "                              <div class=\"citation\">Thanks to all for their help with this article.</div>\n" + 
      "                            </div>\n" + 
      "                          </div>\n" + 
      "                        </li>\n" + 
      "                        <li id=\"r001\">\n" + 
      "                          <div class=\"ref-label\">1. </div>\n" + 
      "                          <div class=\"ref-content\">\n" + 
      "                            <div class=\"ref-content\">\n" +
      "                              ref\n" + 
      "                            </div>\n" + 
      "                          </div>\n" + 
      "                        </li>\n" + 
      "                      </ol>\n" + 
      "                    </section>\n" + 
      "                  </div>\n" + 
      "                </div>\n" + 
      "              </div>\n" + 
      "            </div>\n" + 
      "          </div>\n" + 
      "        </div>\n" + 
      "      </div>\n" + 
      "    </div>\n" + 
      "    <div class=\"small-12 xlarge-4 columns subcontent\" data-equalizer-watch=\"\">\n" + 
      "      <div class=\"subcontent-body\">\n" + 
      "      </div>\n" + 
      "    </div>\n" + 
      "  </div>\n" + 
      "  <footer class=\"footer\">\n" + 
      "    <div class=\"row\">\n" + 
      "      <div class=\"columns\">\n" + 
      "        <h2 class=\"xlarge-heading\">Explore JSTOR</h2>\n" + 
      "      </div>\n" + 
      "    </div>\n" + 
      "  </footer>\n" + 
      "  <script type=\"text/javascript\" src=\"//translate.google.com/translate_a/element.js\" async=\"\"></script>\n" + 
      "</body>";
  private static final String ft_filtered =
      " Text of Geoffrey Chaucer text here. ";
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
    assertEquals(" ", StringUtil.fromInputStream(actIn));
    actIn = hashfact.createFilteredInputStream(mau,
        new StringInputStream(html2), Constants.DEFAULT_ENCODING);
    assertEquals(html2_filtered, StringUtil.fromInputStream(actIn)); 
  }

  public void testFullTextFiltering() throws Exception {
    InputStream actIn = hashfact.createFilteredInputStream(mau,
        new StringInputStream(ft_html), Constants.DEFAULT_ENCODING);
    assertEquals(ft_filtered, StringUtil.fromInputStream(actIn));
  }


}
