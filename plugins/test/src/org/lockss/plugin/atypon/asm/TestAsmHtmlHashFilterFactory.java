/*

 Copyright (c) 2000-2021 Board of Trustees of Leland Stanford Jr. University,
 all rights reserved.

 Permission is hereby granted, free of charge, to any person obtaining a copy
 of his software and associated documentation files (the "Software"), to deal
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

package org.lockss.plugin.atypon.asm;

import org.lockss.test.LockssTestCase;
import org.lockss.test.MockArchivalUnit;
import org.lockss.test.StringInputStream;
import org.lockss.util.Constants;
import org.lockss.util.StringUtil;

import java.io.InputStream;

public class TestAsmHtmlHashFilterFactory extends LockssTestCase {
  static String ENC = Constants.DEFAULT_ENCODING;

  private AsmHtmlHashFilterFactory fact;
  private MockArchivalUnit mau;

  public void setUp() throws Exception {
    super.setUp();
    fact = new AsmHtmlHashFilterFactory();
    mau = new MockArchivalUnit();
  }

  /* cited by section */
  private static final String citation =
    "<div class=\"labeled\" role=\"doc-biblioentry\">" +
      "<div class=\"label\">3.</div>" +
      "<div id=\"B3\" class=\"citations\">" +
        "<div class=\"citation\">" +
          "<div class=\"citation-content\">" +
            "Telesh IV, Khlebovich VV. 2010. Principal processes within the estuarine salinity gradient: a review. " +
            "<em>Mar Pollut Bull</em> " +
            "61:149\u0013155." +
          "</div>" +
          "<div class=\"external-links\">" +
            "<div><a href=\"https://doi.org/10.1016/j.marpolbul.2010.02.008\">Crossref</a></div>" +
            "<div><a href=\"https://www.ncbi.nlm.nih.gov/pubmed/20304437\">PubMed</a></div>" +
            "<div><a href=\"https://scholar.google.com/scholar_lookup?title=Principal%20processes%20within%20the%20estuarine%20salinity%20gradient%3A%20a%20review&amp;author=IV+Telesh&amp;author=VV+Khlebovich&amp;publication_year=2010&amp;pages=149-155\">Google Scholar</a></div>" +
          "</div>" +
        "</div>" +
      "</div>" +
    "</div>";

  private static final String filteredCitation =
    "<div class=\"labeled\" role=\"doc-biblioentry\">" +
      "<div class=\"label\">3.</div>" +
      "<div  class=\"citations\">" +  // NOTE: Removal of id attribute occurs
        "<div class=\"citation\">" +
          "<div class=\"citation-content\">" +
            "Telesh IV, Khlebovich VV. 2010. Principal processes within the estuarine salinity gradient: a review. " +
            "<em>Mar Pollut Bull</em> " +
            "61:149\u0013155." +
          "</div>" +
        "</div>" +
      "</div>" +
    "</div>";

  private static final String imgWSrc =
    "<div class=\"cover-image__image\">" +
      "<img src=\"/cms/asset/112d9259-0de9-430e-a20f-214984ce1915/jcm.2020.58.issue-8.cover.gif\" alt=\"Journal of Clinical Microbiology cover image\"/>" +
    "</div>";

  private static final String filteredImgWSrc =
    "<div class=\"cover-image__image\">" +
    "</div>";

  private static final String h3Id =
    "<section class=\"toc__section\">" +
      "<h3 id=\"h_d234716e5959\" class=\"to-section\">" +
        "Amplicon Sequence Collections" +
      "</h3>" +
    "</section>";

  private static final String filteredH3Id =
    "<section class=\"toc__section\">" +
      "<h3  class=\"to-section\">" +
        "Amplicon Sequence Collections" +
      "</h3>" +
    "</section>";

  private static final String weRecommend =
    "<div>" +
      "<article>" +
        "..." +
      "</article>" +
      "<div class=\"container\">" +
        "" +
        "\n" +
        "" +
        "<div class=\"row\">" +
          "<div class=\"col-12\">" +
            "\n" +
            "<section aria-label=\"we recommend\" class=\"we-recommend\">" +
              "<h3 class=\"we-recommend__title\">" +
                "We Recommend" +
              "</h3>" +
            "</section>" +
          "</div>" +
        "</div>" +
      "</div>" +
    "</div>";

  private static final String filteredWeRecommend =
    "<div>" +
      "<article>" +
        "..." +
      "</article>" +
    "</div>";

  private static final String pubHistory =
    "<section class=\"core-history\">" +
      "<h4>History</h4>" +
      "<div>Received: 11 February 2020</div>" +
      "<div>Accepted: 18 February 2020</div>" +
      "<div>Published online: 24 February 2020</div>" +
      "<br>" +
      "<div class=\"pubmed\">" +
        "<b>PubMed: </b>" +
        "<a href=\"https://pubmed.ncbi.nlm.nih.gov/32094260\" style=\"color:#1554b2\">32094260</a>" +
      "</div>" +
    "</section>";

  private static final String filteredPubHistory =
    "<section class=\"core-history\">" +
      "<h4>History</h4>" +
      "<div>Received: 11 February 2020</div>" +
      "<div>Accepted: 18 February 2020</div>" +
      "<div>Published online: 24 February 2020</div>" +
    "</section>";

  private static final String footnote =
    "<div class=\"notes\">" +
      "<div class=\"labeled\" role=\"doc-footnote\">" +
        "<div class=\"label\">" +
          "a" +
        "</div>" +
        "<div id=\"T1F1\" role=\"paragraph\">" +
          "The hydrodynamic radii and limiting molar conductivities were reported previously (" +
          "<a href=\"#B51\" role=\"doc-biblioref\" data-xml-rid=\"B51\">51</a>" +
          ", " +
          "<a href=\"#B52\" role=\"doc-biblioref\" data-xml-rid=\"B52\">52</a>" +
          ")." +
        "</div>" +
      "</div>" +
      "<div class=\"labeled\" role=\"doc-footnote\">" +
        "<div class=\"label\">" +
          "<sup>b</sup>" +
        "</div>" +
        "<div id=\"T1F2\" role=\"paragraph\">" +
          "Measured under 80\tmV in a solution of 1 M electrolyte and 10\tmM HEPES at pH 6." +
        "</div>" +
      "</div>" +
    "</div>";


  private static final String filteredFootnote =
    "<div class=\"notes\">" +
      "<div class=\"labeled\" role=\"doc-footnote\">" +
        "<div  role=\"paragraph\">" +
          "The hydrodynamic radii and limiting molar conductivities were reported previously (" +
          "<a href=\"#B51\" role=\"doc-biblioref\" data-xml-rid=\"B51\">51</a>" +
          ", " +
          "<a href=\"#B52\" role=\"doc-biblioref\" data-xml-rid=\"B52\">52</a>" +
          ")." +
        "</div>" +
      "</div>" +
      "<div class=\"labeled\" role=\"doc-footnote\">" +
        "<div  role=\"paragraph\">" +
          "Measured under 80\tmV in a solution of 1 M electrolyte and 10\tmM HEPES at pH 6." +
        "</div>" +
      "</div>" +
    "</div>";

  public void testCitationFilter() throws Exception {
    InputStream in;
    in = fact.createFilteredInputStream(mau, new StringInputStream(citation),
        ENC);
    assertEquals(filteredCitation,StringUtil.fromInputStream(in));
  }

  public void testImgSrcFilter() throws Exception {
    InputStream in;
    in = fact.createFilteredInputStream(mau, new StringInputStream(imgWSrc),
        ENC);
    assertEquals(filteredImgWSrc,StringUtil.fromInputStream(in));
  }

  public void testIdFilter() throws Exception {
    InputStream in;
    in = fact.createFilteredInputStream(mau, new StringInputStream(h3Id),
        ENC);
    assertEquals(filteredH3Id,StringUtil.fromInputStream(in));
  }

  public void testWeRecommendFilter() throws Exception {
    InputStream in;
    in = fact.createFilteredInputStream(mau, new StringInputStream(weRecommend),
        ENC);
    assertEquals(filteredWeRecommend,StringUtil.fromInputStream(in));
  }

  public void testPubHistoryFilter() throws Exception {
    InputStream in;
    in = fact.createFilteredInputStream(mau, new StringInputStream(pubHistory),
        ENC);
    assertEquals(filteredPubHistory,StringUtil.fromInputStream(in));
  }

  public void testFootnoteFilter() throws Exception {
    InputStream in;
    in = fact.createFilteredInputStream(mau, new StringInputStream(footnote),
        ENC);
    assertEquals(filteredFootnote,StringUtil.fromInputStream(in));
  }

}