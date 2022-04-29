/*

Copyright (c) 2000-2022, Board of Trustees of Leland Stanford Jr. University
All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are met:

1. Redistributions of source code must retain the above copyright notice,
this list of conditions and the following disclaimer.

2. Redistributions in binary form must reproduce the above copyright notice,
this list of conditions and the following disclaimer in the documentation
and/or other materials provided with the distribution.

3. Neither the name of the copyright holder nor the names of its contributors
may be used to endorse or promote products derived from this software without
specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
POSSIBILITY OF SUCH DAMAGE.

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
      " ";
  
 
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
          " ";
 
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
          " ";
  
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
      " stuff ";
  
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
      "<dl class=\"list\">\n" + 
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
      " Metrics ";
  
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
      "<dl class=\"videoList\">\n" + 
      "  <dt>PACS</dt>\n" + 
      "  <dd>\n" + 
      "                <p>\n" + 
      "                    <a href=\"/search?searchType=selectedPacsMscCode&amp;primarypacs=04.62.%2bv\">\n" + 
      "                        04.62.+v&nbsp;Quantum fields in curved spacetime\n" + 
      "                    </a>\n" + 
      "                </p>\n" + 
      "                <p>\n" + 
      "                    <a href=\"/search?searchType=selectedPacsMscCode&amp;primarypacs=03.65.-w\">\n" + 
      "                        03.65.-w&nbsp;Quantum mechanics\n" + 
      "                    </a>\n" + 
      "                </p>\n" + 
      "  </dd>\n" + 
      "  <dt>Subjects</dt>\n" + 
      "  <dd>\n" + 
      "            <p>\n" + 
      "               <a class=\"pacsLink linkArrow\" href=\"/search?searchType=category&amp;categorys=Gravitation+and+cosmology\">\n" + 
      "                   Gravitation and cosmology </a>\n" + 
      "            </p>\n" + 
      "            <p>\n" + 
      "               <a class=\"pacsLink linkArrow\" href=\"/search?searchType=category&amp;categorys=Quantum+information+and+quantum+mechanics\">\n" + 
      "                   Quantum information and quantum mechanics </a>\n" + 
      "            </p>\n" + 
      "  </dd>\n" + 
      "  </dl>" +
      "<div id=\"articleOALicense\">Open Access</div>" +
      "</body>\n";
  private static final String miscHtmlFiltered =
      " stuff ";
  
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