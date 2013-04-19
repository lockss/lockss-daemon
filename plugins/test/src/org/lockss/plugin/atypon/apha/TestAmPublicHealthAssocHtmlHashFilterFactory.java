/*
 * $Id: TestAmPublicHealthAssocHtmlHashFilterFactory.java,v 1.1 2013-04-19 22:49:44 alexandraohlson Exp $
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
  
  private static final String topBannerHtml =
      "<start><div id=\"identity-bar\">" +
          "    <div class=\"headerAd\">" +
          "        <!-- placeholder id=null, description=Header - Mobile Button -->" +
          "    </div>" +
          "        <div class=\"headerAd\">" +
          "            <!-- placeholder id=null, description=Home Banner Ad --><!-- Begin EHS Head Tag -->" +
          "<center><script type=\"text/javascript\">" +
          "var ehs_site=\"ehs.pro.apha.ajph\";" +
          "var ehs_zone=\"\";" +
          "// DO NOT CHANGE ANYTHING BELOW" +
          "var ehs_protocol=(document.location.protocol==\"https:\") ? \"https://\" : \"http://\";" +
          "var ehs_tagsrc=ehs_protocol+'ads.ehealthcaresolutions.com/tag/';" +
          "document.write('<scr'+'ipt type=\"text/javascript\" src=\"'+ehs_tagsrc+'\"></scr'+'ipt>');" +
          "</script><br></center>" +
          "<!-- End EHS Body Tag -->" +
          "</div>" +
          "<span id=\"institution-banner-text\"><span class=\"institutionBannerText\">STANFORD UNIV MED CTR</span> </span>" +
          "    <span id=\"individual-menu\">" +
          "    </span>" +
          "</div><end>";

  private static final String topBannerHtmlFiltered =
      "<start><end>";
  
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
  
  private static final String journalInfoHtml =
      "<start><div class=\"panel panel_228\"  id=\"journalNavPanel\">" +
          "   <div class=\"panelTopLeft\"></div>" +
          "    <div class=\"panelTopMiddle panel_228_width\">" +
          "        <h3>Volume 102, Issue 12 (December 2012)</h3>" +
          "    </div>" +
          "    <div class=\"panelTopRight\"></div>" +
          "    <div class=\"panelBody panel_228_pad\">" +
          "            <div id=\"nextprev\">" +
          "            </div><br/>" +
          "        <div id=\"smallIssueCover\">" +
          "            <a href=\"/loi/ajph\">" +
          "                <img src=\"/na101/blah.cover.jpg\" /><br/>" +
          "            </a>" +
          "        </div><br/>" +
          "        <a href=\"http://ajph.aphapublications.org/toc/ajph/103/5\" id=\"linklargeStyle\">Current Issue</a><br/>" +
          "        <a href=\"/loi/ajph\" id=\"linklargeStyle\">Available Issues</a><br/>" +
          "        <a href=\"http://ajph.aphapublications.org/toc/ajph/0/0\">" +
          "          First Look" +
          "        </a>" +
          "   </div>" +
          "</div>" +
          "<div class=\"panel panel_228\"  id=\"journalInfoPanel\">" +
          "    <div class=\"panelTopLeft\"></div>" +
          "    <div class=\"panelTopMiddle panel_228_width\">" +
          "        <h3>Journal Information</h3>" +
          "    </div>" +
          "    <div class=\"panelTopRight\"></div>" +
          "    <div class=\"panelBody panel_228_pad\">" +
          "    <p style=\"text-align: center;\">LOTS OF INFO<br />" +
          "    </div>" +
          "</div><end>";

  private static final String journalInfoHtmlFiltered =
      "<start><end>";
  
  public void test_topBannerHtml() throws Exception {
    InputStream actIn = filt.createFilteredInputStream(mau,
        new StringInputStream(topBannerHtml),
        Constants.DEFAULT_ENCODING);

    assertEquals(topBannerHtmlFiltered, StringUtil.fromInputStream(actIn));
  }

  public void test_footerHtml() throws Exception {
    InputStream actIn = filt.createFilteredInputStream(mau,
        new StringInputStream(footerHtml),
        Constants.DEFAULT_ENCODING);

    assertEquals(footerHtmlFiltered, StringUtil.fromInputStream(actIn));
  }

  public void test_journalInfoHtml() throws Exception {
    InputStream actIn = filt.createFilteredInputStream(mau,
        new StringInputStream(journalInfoHtml),
        Constants.DEFAULT_ENCODING);

    assertEquals(journalInfoHtmlFiltered, StringUtil.fromInputStream(actIn));
  }

}
