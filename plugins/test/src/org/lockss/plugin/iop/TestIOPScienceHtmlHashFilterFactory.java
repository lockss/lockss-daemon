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

package org.lockss.plugin.iop;

import java.io.*;

import org.lockss.util.*;
import org.lockss.test.*;

public class TestIOPScienceHtmlHashFilterFactory extends LockssTestCase {
  static String ENC = Constants.DEFAULT_ENCODING;

  private IOPScienceHtmlHashFilterFactory fact;
  private MockArchivalUnit mau;

  public void setUp() throws Exception {
    super.setUp();
    fact = new IOPScienceHtmlHashFilterFactory();
    mau = new MockArchivalUnit();
  }

  // test removal of tags by the hash filter
  private static final String tagsHtmlHash =
      "<head>" +
      "<link type=\"text/css\"/>" +
      "</head>" +
      "<body>" +
      "<div>" +
      "<div class=\"alsoRead\"><br></div>" +
      "<div class=\"tabs javascripted\"><br></div>" +
      "<div id=\"banner\"><br></div>" +
      "<div id=\"footer\"><br></div>" +
      "<script type=\"javascript\"/>var x=0;</script>" +
      "<form action=\"foo?jsessionId=bar\"><br></form>" +
      "<div id=\"tacticalBanners\"><br></div>" +
      "</div>" +
      "</body>";
  // only outer div should remain
  private static final String tagsHtmlHashFiltered =
      "<body><div></div></body>";
  
 
  private static final String WhiteSpace1 = "\n  <li><a href=\"/content/pdf/1477-7525-8-103.pdf\">PDF</a>\n (543KB)\n </li>";
  
  private static final String WhiteSpace2 = "\n\n      <li><a href=\"/content/pdf/1477-7525-8-103.pdf\">PDF</a>\n       (543KB)\n      </li>";
 
  
  private static final String mathJaxHtml =
      "<div class=\"mathJaxControls\" style=\"display:none\">" +
          "<!-- add mathjax logo here and hide mathjax text -->" +
          "<a class=\"mjbadge\" href=\"http://www.mathjax.org/\">Mathjax</a>" +
          "<a href=\"#\" id=\"mathJaxOn\">On</a> | <a href=\"#\" class=\"selectedMathJaxOption\" id=\"mathJaxOff\">Off</a>" +
          "</div>" +
          "<br clear=\"all\"/>";
  private static final String mathJaxHtmlFiltered =
          "<br clear=\"all\"/>";
 
  private static final String rightColHtml =
      "<div id=\"rightCol\">" +
          "<ul class=\"accordion\">" +
          "    <li><h5>Contents</h5>" +
          "<ol class=\"accordion open\">" +
          "<li><a href=\"#artAbst\">Abstract</a></li>" +
          "    <li>" +
          "        <h5>Related Articles</h5>" +
          "        <ol class=\"accordion\">" +
          "                <li>" +
          "                    <a href=\"/xx?rel=sem&amp;relno=1\"" +
          "                       title=\"Semicond. Sci. Technol., xx, yy\"" +
          "                       >" +
          "                            1. title here" +
          "                    </a>" +
          "                </li>" +
          "                <li>" +
          "                    <a href=\"/zz?rel=sem&amp;relno=2\"" +
          "                       title=\"Semicond. Sci. Technol., zz, qq\"" +
          "                       >" +
          "                            2. another title here" +
          "                    </a>" +
          "                </li>" +
          "        </ol>" +
          "    </li>" +
          "    <li>" +
          "        <h5>Related Review Articles</h5>" +
          "        <ol class=\"accordion\">" +
          "                <li>" +
          "                    <a href=\"/pp?rel=rev&amp;relno=1\"" +
          "                       title=\"Semicond. Sci. Technol., pp, ss\"" +
          "                       >" +
          "                            1. title three" +
          "                    </a>" +
          "                </li>" +
          "        </ol>" +
          "    </li>" +
          "<li>" +
          "    <h5>Journal links</h5>" +
          "    <ul class=\"accordion\">" +
          "    <li><a href=\"/nn\" title=\"Journal home\">Journal home</a></li>" +
          "    <li><a href=\"/nn/page/Scope\">Scope</a></li>" +
          "    </ul>" +
          "</li>" +
          "</ul>" +
          "<div id=\"tabStop\">&nbsp;</div>" +
          "</div>" +
          "<br clear=\"all\"/>";
  private static final String rightColHtmlFiltered =
          "<br clear=\"all\"/>";
  
  // test removal of header & footer tags by the hash filter
  private static final String hrtagsHtmlHash =
      "<header>\n" +
      "<div id=\"header-content\">\n" + 
      "<a title=\"IOP science\"></a>\n" + 
      "</div>head \n" +
      "</header>\n" +
      "<body>stuff</body>\n" +
      "<footer> foot \n" +
      "</footer>";
  private static final String hrtagsHtmlHashFiltered =
      " <body>stuff</body> ";
  
  // test removal of metrics-panel, etc.
  private static final String metricsHtml =
      "<div class=\" metrics-panel\">\n" + 
      "    \n" + 
      "    <!--  Start of Internal Stats Section -->\n" + 
      "    <p>Please see the page <a href=\"/info/page/article-level-metrics\">article level metrics in IOPscience</a> for more information about the statistics available. Article usage data are updated once a week.</p>\n" + 
      "    <h4 id=\"internalStatsHeaderId\" class=\"subhead\">Article usage</h4>\n" + 
      "    <br clear=\"all\">\n" + 
      "      \n" + 
      "\n" + 
      "<div class=\"hideMetrics\" id=\"crossrefErrorId\">\n" + 
      "  Results for CrossRef are currently unavailable for this article.\n" + 
      "</div>\n" + 
      "\n" + 
      "      <h4 id=\"bookmarksSectionId\" class=\"subhead hideMetrics\">Shares and bookmarks</h4>\n" + 
      "\n" + 
      "</div>" +
      "" +
      "<dl class=\"videoList\">\n" + 
      "      <dt>Metrics</dt>\n" + 
      "        <dd>\n" + 
      "          <p>\n" + 
      "                Total article downloads:\n" + 
      "                <strong>408</strong>\n" + 
      "                \n" + 
      "          </p>\n" + 
      "        <p>\n" + 
      "          <a href=\"\" id=\"abstractMoreMetricsId\">More metrics</a>\n" + 
      "        </p>\n" + 
      "      </dd>\n" + 
      "        <dd>\n" + 
      "                Download data unavailable\n" + 
      "        <p>\n" + 
      "          <a href=\"\" id=\"abstractMoreMetricsId\">More metrics</a>\n" + 
      "        </p>\n" + 
      "      </dd>\n" + 
      "</dl>\n";
  private static final String metricsHtmlFiltered =
      "<dl class=\"videoList\"> <dt>Metrics</dt> </dl> ";
  
  // test removal of sideTabBar, viewingLinks, metrics-panel, 
  private static final String miscHtml =
      "\n<body>stuff" +
      "<div id=\"sideTabBar\">\n" + 
      "  <input type=\"hidden\" value=\"0268-1242/27/1/015002\" name=\"articleId\">\n" + 
      "\n" + 
      "<div class=\"sideTabBar\">\n" + 
      "  <input type=\"hidden\" value=\"0268-1242/27/1/015002\" name=\"articleId\">\n" + 
      "\n" + 
      "<div style=\"display: none;\">\n" + 
      "</div>\n" +
      "" +
      "<p class=\"viewingLinks\">\n" + 
      "  \n" + 
      "    <a title=\"Tag this article\"> Tag this article</a>\n" + 
      "  \n" + 
      "</p>" +
      "" +
      "<div class=\"jnlTocIssueNav\">\n" + 
      "  <a href=\"/1742-6596/475/1\" class=\"nextprevious\">« previous issue</a> \n" + 
      "  <a href=\"/1742-6596/477/1\" class=\"nextprevious\">next issue »</a>\n" + 
      "</div>" +
      "</body>\n";
  private static final String miscHtmlFiltered =
      " <body>stuff</body> ";
  
  public void testFiltering() throws Exception {
    InputStream inA;
    InputStream inB;
    
    /* impactFactor test */
    inA = fact.createFilteredInputStream(mau, new StringInputStream(tagsHtmlHash),
        ENC);

    assertEquals(tagsHtmlHashFiltered,StringUtil.fromInputStream(inA));

    /* whiteSpace test */
    inA = fact.createFilteredInputStream(mau, new StringInputStream(WhiteSpace1),
        ENC);
    
    inB = fact.createFilteredInputStream(mau, new StringInputStream(WhiteSpace2),
        ENC);

    assertEquals(StringUtil.fromInputStream(inA),StringUtil.fromInputStream(inB));
    
    /* rightCol test */
    inA = fact.createFilteredInputStream(mau, new StringInputStream(rightColHtml),
        ENC);
    assertEquals(rightColHtmlFiltered,StringUtil.fromInputStream(inA));
    
    /* mathjax text */
    inA = fact.createFilteredInputStream(mau, new StringInputStream(mathJaxHtml),
        ENC);
    assertEquals(mathJaxHtmlFiltered,StringUtil.fromInputStream(inA));
    
    // header & footer test
    inA = fact.createFilteredInputStream(mau, new StringInputStream(hrtagsHtmlHash),
        ENC);
    assertEquals(hrtagsHtmlHashFiltered, StringUtil.fromInputStream(inA));
    
    // metrics test
    inA = fact.createFilteredInputStream(mau, new StringInputStream(metricsHtml),
        ENC);
    assertEquals(metricsHtmlFiltered, StringUtil.fromInputStream(inA));
    
    // misc test
    inA = fact.createFilteredInputStream(mau, new StringInputStream(miscHtml),
        ENC);
    assertEquals(miscHtmlFiltered, StringUtil.fromInputStream(inA));
    
  }
}