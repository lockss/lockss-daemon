/*
 * $Id$
 */

/*

 Copyright (c) 2000-2014 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.plugin.americanmathematicalsociety;

import java.io.InputStream;

import org.lockss.util.*;
import org.lockss.test.*;

public class TestAmericanMathematicalSocietyHtmlFilterFactory extends LockssTestCase {
  static String ENC = Constants.DEFAULT_ENCODING;

  private AmericanMathematicalSocietyHtmlFilterFactory fact;
  private MockArchivalUnit mau;

  public void setUp() throws Exception {
    super.setUp();
    fact = new AmericanMathematicalSocietyHtmlFilterFactory();
    mau = new MockArchivalUnit();
  }

  private static final String withScript = "<head>" +
      "<script type=\"text/javascript\">\n" + 
      "  var _gaq = _gaq || [];\n" + 
      "</script>" +
      "<meta http-equiv=\"Content-Type\" content=\"text/html; charset=utf-8\"/>" +
      "</head>\n" +
      "<script type=\"text/javascript\" src=\"/camcos/etc/cover.js\"></script>";
  
  private static final String withoutScript = "<head>" +
      "<meta http-equiv=\"Content-Type\" content=\"text/html; charset=utf-8\"/>" +
      "</head>\n";
  
  private static final String withStuff = "<body>" +
      "<table width=\"100%\" cellspacing=\"0\" cellpadding=\"0\" border=\"0\" id=\"ribbon\">\n" + 
      "<tbody><tr>\n" + 
      "<td><a href=\"http://www.ams.org\"><img width=\"69\" height=\"34\" src=\"/images/bannerNav.gif\"></a></td>\n" + 
      "<td><span id=\"ribbonLinks\"> \n" + 
      "<a href=\"/publications/\">Publications</a> \n" + 
      "<a href=\"/about-us/\">About the AMS</a>\n" + 
      "</span>\n" + 
      "</td>\n" + 
      "</tr>\n" + 
      "</tbody></table>" +
      "" +
      "<div data-link-target=\"_blank\" class=\"altmetric-embed\">" +
      "<a href=\"http://www.altmetric.com/details.php?domain=www.ams.org\"></a></div>" +
      "" +
      "<table align=\"center\" summary=\"Table that holds logos and navigation\">\n" + 
      "  <tbody><tr>\n" + 
      "    <td width=\"62\" valign=\"middle\" align=\"left\"></td>\n" + 
      "    <td width=\"63\" valign=\"middle\" align=\"left\"></td>\n" + 
      "    <td width=\"350\"><div align=\"center\">\n" + 
      "    <a href=\"/journals/jams\"></a></div></td>\n" + 
      "    <td align=\"left\"><ul class=\"jrnlsGlobalNav\">\n" + 
      "      <li><a href=\"/publications/journals\">Journals Home</a></li>\n" + 
      "      <li><a href=\"/publications/journals/help\">Help</a></li>\n" + 
      "      </ul></td>\n" + 
      "  </tr>\n" + 
      "</tbody></table>" +
      "" +
      "<table>\n" + 
      "        <tbody><tr>\n" + 
      "            <td class=\"navCell\" id=\"navCell\" colspan=\"3\">\n" + 
      "            <div align=\"center\"><a href=\"/jams/2012-25-04/\">Previous issue</a>\n" + 
      "| <a href=\"/jams/2013-26-01/\">This issue</a>\n" + 
      "| <a href=\"javascript: goto_most_recent_jams()\">Most recent issue</a>\n" + 
      "| <a href=\"/jourcgi/jrnl_toolbar_nav/jams_all\">All issues (1988&ndash;Present)</a>\n" + 
      "| <a href=\"/jams/2013-26-02/\">Next issue</a>\n" + 
      "<br>\n" + 
      "<a href=\"/jams/2013-26-01/S0894-0347-2012-00753-X/\">Previous article</a>\n" + 
      "            </td>\n" + 
      "        </tr>\n" + 
      "    </tbody></table>\n" +
      "" +
      "<table id=\"footer\">\n" + 
      "  <tbody><tr>\n" + 
      "    <td valign=\"top\" align=\"center\"><p>\n" + 
      "    2014, American Mathematical Society \n" + 
      "      <br>\n" + 
      "    </td>\n" + 
      "  </tr>\n" + 
      "  <tr>\n" + 
      "    <td valign=\"top\" align=\"center\" colspan=\"3\">\n" + 
      "<div id=\"socialIcons\">" +
      " <a href=\"http://www.ams.org/about-us/social\">" +
      "<img title=\"Connect with us\" alt=\"Connect with us\" " +
      "src=\"/images/socialFooterIcons-connect_32.png\"></a> " +
      " <a href=\"http://www.ams.org/rss/mathmoments.xml\">" +
      "<img title=\"Podcasts\" alt=\"Podcasts\"" +
      " src=\"/images/socialFooterIcons-podcasts_32.png\"></a>" +
      " <a href=\"http://en.wikipedia.org/wiki/American_Mathematical_Society\">" +
      "<img title=\"Wikipedia\" alt=\"Wikipedia\"" +
      " src=\"/images/socialFooterIcons-wikipedia.png\"></a>" +
      "</div>\n" + 
      "    </td>\n" + 
      "  </tr>\n" + 
      "</tbody></table>" +
      "" +
      "</body>";
  
  private static final String withoutStuff = "<body>" +
      "<table>\n" + 
      "        <tbody><tr>\n" +
      "            \n" + 
      "        </tr>\n" + 
      "    </tbody></table>\n" +
      "</body>";
  
  public void testFiltering() throws Exception {
    assertFilterToSame(withScript, withoutScript);
    assertFilterToSame(withStuff, withoutStuff);
  }
  
  private void assertFilterToSame(String str1, String Str2) throws Exception {
    InputStream inA = fact.createFilteredInputStream(mau, new StringInputStream(str1),
        Constants.DEFAULT_ENCODING);
    InputStream inB = fact.createFilteredInputStream(mau, new StringInputStream(Str2),
        Constants.DEFAULT_ENCODING);
    String a = StringUtil.fromInputStream(inA);
    String b = StringUtil.fromInputStream(inB);
    assertEquals(a, b);
  }
  
}
