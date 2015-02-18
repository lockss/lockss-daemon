/*
 * $Id$
 */

/*

 Copyright (c) 2000-2013 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.plugin.mathematicalsciencespublishers;

import java.io.*;

import org.lockss.util.*;
import org.lockss.test.*;

public class TestMathematicalSciencesPublishersHtmlFilterFactory extends LockssTestCase {
  static String ENC = Constants.DEFAULT_ENCODING;

  private MathematicalSciencesPublishersHtmlFilterFactory fact;
  private MockArchivalUnit mau;

  public void setUp() throws Exception {
    super.setUp();
    fact = new MathematicalSciencesPublishersHtmlFilterFactory();
    mau = new MockArchivalUnit();
  }

  private static final String withScript = "<head>" +
      "<meta http-equiv=\"Content-Type\" content=\"text/html; charset=utf-8\"/>" +
      "<script type=\"text/javascript\" src=\"/camcos/etc/cover.js\"></script>" +
      "</head>";

  private static final String withoutScript = "<head>" +
      "<meta http-equiv=\"Content-Type\" content=\"text/html; charset=utf-8\"/>" +
      "</head>";

  private static final String withStuff = "<body onload=\"javascript:void(0);\">" +
      "<table cellspacing=\"0\" cellpadding=\"0\" class=\"masthead\" id=\"masthead-area\">" +
      "<tr><td class=\"volume-h\" onclick=\"javascript:window.location='index.xhtml';\">" +
      "  <h4>Vol. 1, No. 1, 2006</h4></td></tr>" +
      "</table>" +
      "<table cellspacing=\"0\" cellpadding=\"0\" class=\"main\" id=\"main-area\">" +
      "<tr><td class=\"activity-column\" id=\"activity-area\">" +
      "  <table cellspacing=\"0\" cellpadding=\"0\" class=\"action\">" +
      "  <tr><td align=\"left\">" +
      "    <table width=\"100%\" cellspacing=\"0\" cellpadding=\"0\">" +
      "    <tr><td colspan=\"2\" class=\"action-title-area\">" +
      "      <div class=\"action-title\">Download this article</div>" +
      "    </td></tr>" +
      "    <tr><td rowspan=\"2\" class=\"download-icon-area\">" +
      "<img class=\"download-icon\" src=\"/camcos/etc/icon-pdf-lg.gif\"" +
      " alt=\"Download this article.\"/></td>" +
      "    <td class=\"download-caption-area\"><a class=\"download-caption\" " +
      " href=\"/camcos/2006/1-1/camcos-v1-n1-p03-s.pdf\">For screen</a></td>" +
      "    </tr>" +
      "    <tr><td class=\"download-caption-area\"><a class=\"download-caption\"" +
      " href=\"/camcos/2006/1-1/camcos-v1-n1-p03-p.pdf\">For printing</a></td>" +
      "    </tr>" +
      "    </table>" +
      "</td></tr></table>" +
      "<table cellspacing=\"0\" cellpadding=\"0\" class=\"pause\">" +
      "<tr><td class=\"action-end\"><img src=\"/camcos/etc/z.gif\" alt=\"\"/></td>" +
      " <!-- underline (close) the action area above -->" +
      "</tr><tr><td class=\"pause\"><img src=\"/camcos/etc/z.gif\" alt=\"\"/></td>" +
      " <!-- create white space between sections -->" +
      "</tr></table>" +
      "<table cellspacing=\"0\" cellpadding=\"0\" class=\"action\">" +
      "<tr><td>" +
      "  <table width=\"100%\" cellspacing=\"0\" cellpadding=\"0\">" +
      "  <tr>" +
      "  <td class=\"action-title-area\"><div class=\"action-title\">Recent Issues</div></td>" +
      "  </tr>" +
      "  <tr><td class=\"issues-area\"> <a class=\"about\" href=\"/camcos/2013/8-1/index.xhtml\">Volume 8, Issue 1 </a></td></tr>" +
      "  <tr><td class=\"issues-area\"> <a class=\"about\" href=\"/camcos/2006/1-1/index.xhtml\">Volume 1, Issue 1 </a></td></tr>" +
      "  </table>" +
      "</td></tr></table>" +
      "<table cellspacing=\"0\" cellpadding=\"0\" class=\"pause\">" +
      "<tr><td class=\"action-end\"><img src=\"/camcos/etc/z.gif\" alt=\"\"/></td>" +
      " <!-- underline (close) the action area above -->" +
      "</tr><tr><td class=\"pause\"><img src=\"/camcos/etc/z.gif\" alt=\"\"/></td>" +
      " <!-- create white space between sections -->" +
      "</tr></table>" +
      "<table cellspacing=\"0\" cellpadding=\"0\" class=\"action\">" +
      "<tr><td>" +
      "  <table width=\"100%\" cellspacing=\"0\" cellpadding=\"0\">" +
      "  <tr><td class=\"action-title-area\"><div class=\"action-title\">The Journal</div></td></tr>" +
      "  <tr><td class=\"about-area\">" +
      "  <a class=\"about\" href=\"/camcos/about/cover/cover.html\">Cover</a></td></tr>" +
      "  <tr><td class=\"about-area\">" +
      "  <a class=\"about\" href=\"/camcos/about/journal/story.html\">About the Cover</a></td></tr>" +
      "  <!--" +
      "  <tr><td class=\"about-area\">" +
      "  <a class=\"about\" href=\"/scripts/ip_status.php?jpath=camcos\">Test your IP address</a></td></tr>" +
      "  --><tr><td class=\"about-area\">" +
      "  <a class=\"about\" href=\"http://msp.berkeley.edu/ef/\">Editorial Login</a></td></tr>" +
      "  <tr><td class=\"about-area\">" +
      "  <a class=\"about\" href=\"/camcos/about/journal/contact.html\">Contacts</a></td></tr>" +
      "  <tr><td class=\"about-area\">" +
      "  <a class=\"about\" href=\"/scripts/ai.php?jpath=camcos\">Author Index</a></td></tr>" +
      "  <tr><td class=\"about-area\">" +
      "  <a class=\"about\" href=\"/scripts/coming.php?jpath=camcos\">To Appear</a></td></tr>" +
      "  <tr><td>&nbsp;</td></tr>" +
      "  <tr><td class=\"about-area\" style=\"font-size:11px\">ISSN: 2157-5452 (e-only)</td></tr>" +
      "  <tr><td class=\"about-area\" style=\"font-size:11px\">ISSN: 1559-3940 (print)</td></tr>" +
      "  </table>" +
      "</td></tr></table>" +
      "</td></tr>" +
      "<tr><td><table cellspacing=\"0\" cellpadding=\"0\" class=\"pause\">" +
      "  <tr><td class=\"action-end\"><img src=\"/camcos/etc/z.gif\" alt=\"\"/></td>" +
      "  </tr></table></td>" +
      "<td id=\"content-area-end\"></td></tr></table>" +
      "<table cellspacing=\"0\" cellpadding=\"0\" id=\"footer-area\">" +
      "<tr><td class=\"box-footer\" onclick=\"javascript:window.location='http://mathscipub.org/';\">" +
      " <img src=\"/mod/images/msp-logo.png\" alt=\"Mathematical Sciences Publishers\"/>" +
      "</td></tr></table></body>";

  private static final String withoutStuff = "<body onload=\"javascript:void(0);\">" +
      "<table cellspacing=\"0\" cellpadding=\"0\" class=\"main\" id=\"main-area\">" +
      "<tr><td class=\"activity-column\" id=\"activity-area\">" +
      "  <table cellspacing=\"0\" cellpadding=\"0\" class=\"action\">" +
      "  <tr><td align=\"left\">" +
      "    <table width=\"100%\" cellspacing=\"0\" cellpadding=\"0\">" +
      "    <tr><td colspan=\"2\" class=\"action-title-area\">" +
      "      <div class=\"action-title\">Download this article</div>" +
      "    </td></tr>" +
      "    <tr><td rowspan=\"2\" class=\"download-icon-area\">" +
      "<img class=\"download-icon\" src=\"/camcos/etc/icon-pdf-lg.gif\"" +
      " alt=\"Download this article.\"/></td>" +
      "    <td class=\"download-caption-area\"><a class=\"download-caption\" " +
      " href=\"/camcos/2006/1-1/camcos-v1-n1-p03-s.pdf\">For screen</a></td>" +
      "    </tr>" +
      "    <tr><td class=\"download-caption-area\"><a class=\"download-caption\"" +
      " href=\"/camcos/2006/1-1/camcos-v1-n1-p03-p.pdf\">For printing</a></td>" +
      "    </tr>" +
      "    </table>" +
      "</td></tr></table>" +
      "<table cellspacing=\"0\" cellpadding=\"0\" class=\"pause\">" +
      "<tr><td class=\"action-end\"><img src=\"/camcos/etc/z.gif\" alt=\"\"/></td>" +
      " <!-- underline (close) the action area above -->" +
      "</tr><tr><td class=\"pause\"><img src=\"/camcos/etc/z.gif\" alt=\"\"/></td>" +
      " <!-- create white space between sections -->" +
      "</tr></table>" +
      "<table cellspacing=\"0\" cellpadding=\"0\" class=\"action\">" +
      "<tr><td>" +
      " " +
      "</td></tr></table>" +
      "<table cellspacing=\"0\" cellpadding=\"0\" class=\"pause\">" +
      "<tr><td class=\"action-end\"><img src=\"/camcos/etc/z.gif\" alt=\"\"/></td>" +
      " <!-- underline (close) the action area above -->" +
      "</tr><tr><td class=\"pause\"><img src=\"/camcos/etc/z.gif\" alt=\"\"/></td>" +
      " <!-- create white space between sections -->" +
      "</tr></table>" +
      "<table cellspacing=\"0\" cellpadding=\"0\" class=\"action\">" +
      "<tr><td>" +
      " " +
      "</td></tr></table>" +
      "</td></tr>" +
      "<tr><td><table cellspacing=\"0\" cellpadding=\"0\" class=\"pause\">" +
      "  <tr><td class=\"action-end\"><img src=\"/camcos/etc/z.gif\" alt=\"\"/></td>" +
      "  </tr></table></td>" +
      "<td id=\"content-area-end\"></td></tr></table>" +
      "<table cellspacing=\"0\" cellpadding=\"0\" id=\"footer-area\">" +
      "<tr><td class=\"box-footer\" onclick=\"javascript:window.location='http://mathscipub.org/';\">" +
      " <img src=\"/mod/images/msp-logo.png\" alt=\"Mathematical Sciences Publishers\"/>" +
      "</td></tr></table></body>";


  public void testFiltering() throws Exception {
    assertFilterToSame(withScript, withoutScript);
    assertFilterToSame(withStuff, withoutStuff);
  }

  private void assertFilterToSame(String str1, String Str2) throws Exception {

    InputStream inA = fact.createFilteredInputStream(mau, new StringInputStream(str1),
        Constants.DEFAULT_ENCODING);
    InputStream inB = fact.createFilteredInputStream(mau, new StringInputStream(Str2),
        Constants.DEFAULT_ENCODING);
    assertEquals(StringUtil.fromInputStream(inA),
        StringUtil.fromInputStream(inB));
  }

}
