/*
 * $Id$
 */

/*

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

package org.lockss.plugin.atypon.nrcresearchpress;

import java.io.*;

import junit.framework.Test;

import org.lockss.util.*;
import org.lockss.plugin.FilterFactory;
import org.lockss.plugin.atypon.nrcresearchpress.ClockssNRCResearchPressHtmlCrawlFilterFactory;
import org.lockss.test.*;

public class TestNRCResearchPressHtmlCrawlFilterFactory extends LockssTestCase {
  private static FilterFactory fact;
  private static MockArchivalUnit mau;

  //Example instances mostly from pages of strings of HTMl that should be filtered
  //and the expected post filter HTML strings.

  //Related articles
  private static final String HtmlTest1 = 
      "<div id=\"sidebar-left\">"+
          "<h3>Readers of this Article also read </h3>\n" +
          "<ul id=\"readers\">\n" +
          "<li><a href=\"a-new-support-measure-to-quantify-the-impact-of-local-optima-in-phylog-article-a2858\">A New Support Measure to Quantify the Impact of Local Optima in Phylogenetic Analyses</a></li>\n" +
          "<li><a href=\"a-novel-model-for-dna-sequence-similarity-analysis-based-on-graph-theo-article-a2855\">A Novel Model for DNA Sequence Similarity Analysis Based on Graph Theory</a></li>\n" +
          "<li><a href=\"factors-affecting-synonymous-codon-usage-bias-in-chloroplast-genome-of-article-a2916\">Factors Affecting Synonymous Codon Usage Bias in Chloroplast Genome of Oncidium Gower Ramsey</a></li>\n" +
          "<li><a href=\"single-domain-parvulins-constitute-a-specific-marker-for-recently-prop-article-a2835\">Single-Domain Parvulins Constitute a Specific Marker for Recently Proposed Deep-Branching Archaeal Subgroups</a></li>\n" +
          "<li><a href=\"phylogenomics-based-reconstruction-of-protozoan-species-tree-article-a2785\">Phylogenomics-Based Reconstruction of Protozoan Species Tree</a></li>\n" +
          "</ul>\n" +
          "</div>Hello";  
  private static final String HtmlTest1Filtered = "Hello";

  private static final String HtmlTest2 = 
      "<div class=\"ads\">"+
          "<h3>Readers of this Article also read </h3>\n" +
          "<ul id=\"readers\">\n" +
          "<li><a href=\"a-new-support-measure-to-quantify-the-impact-of-local-optima-in-phylog-article-a2858\">A New Support Measure to Quantify the Impact of Local Optima in Phylogenetic Analyses</a></li>\n" +
          "<li><a href=\"a-novel-model-for-dna-sequence-similarity-analysis-based-on-graph-theo-article-a2855\">A Novel Model for DNA Sequence Similarity Analysis Based on Graph Theory</a></li>\n" +
          "<li><a href=\"factors-affecting-synonymous-codon-usage-bias-in-chloroplast-genome-of-article-a2916\">Factors Affecting Synonymous Codon Usage Bias in Chloroplast Genome of Oncidium Gower Ramsey</a></li>\n" +
          "<li><a href=\"single-domain-parvulins-constitute-a-specific-marker-for-recently-prop-article-a2835\">Single-Domain Parvulins Constitute a Specific Marker for Recently Proposed Deep-Branching Archaeal Subgroups</a></li>\n" +
          "<li><a href=\"phylogenomics-based-reconstruction-of-protozoan-species-tree-article-a2785\">Phylogenomics-Based Reconstruction of Protozoan Species Tree</a></li>\n" +
          "</ul>\n" +
          "</div>World"; 
  private static final String HtmlTest2Filtered = "World";

  private static final String HtmlTest3 = 
      "  <td width=\"582\" valign=\"top\">"+
          "    <table border=0  cellspacing=0 cellpadding=0>"+
          "      <tr>"+
          "      <td align=center><a href=\"journals/zookeys/issue/276/\" class=more3>Current Issue</a></td>"+
          "      <td class=red2>|</td>"+
          "      <td align=center><a href=\"journals/zookeys/archive\" class=green>All Issues</a></td>"+
          "      </tr>"+
          "    </table>"+
          "   </td>";

  private static final String HtmlTest3Filtered = 
      "  <td width=\"582\" valign=\"top\">"+
          "    <table border=0  cellspacing=0 cellpadding=0>"+
          "      <tr>"+
          "      <td align=center><a href=\"journals/zookeys/issue/276/\" class=more3>Current Issue</a></td>"+
          "      <td class=red2>|</td>"+
          "      <td align=center><a href=\"journals/zookeys/archive\" class=green>All Issues</a></td>"+
          "      </tr>"+
          "    </table>"+
          "   </td>";

  private static final String HtmlTest4 =
      "<span id=\"hide\"><a href=\"/doi/pdf/10.1046/9999-9999.99999\"><!-- Spider trap link --></a></span>quick brown fox";
  private static final String HtmlTest4Filtered = "quick brown fox";

  private static final String HtmlTest5 =
      "<div class=\"citedBySection\"><a name=\"citart1\"></a><h2>Cited by</h2><p><a href=\"/doi/citedby/10.1139/t99-066\">View all 2 citing articles</a></p></div>Chicken, Chicken, chicken";
  private static final String HtmlTest5Filtered =  "Chicken, Chicken, chicken";

  private static final String HtmlTest6 =
      "<div class=\"box-pad border-gray margin-bottom clearfix\"><!-- /fulltext content --></div>chicken, Chicken, chicken";
  private static final String HtmlTest6Filtered =  "chicken, Chicken, chicken";

  private static final String HtmlTest7 =
      "Jump Over the Lazy Dog <div class=\"box-pad border-gray margin-bottom clearfix\">";

  private static final String HtmlTest7Filtered =  "Jump Over the Lazy Dog ";

  private static final String HtmlTestCrossLinks = 
      "<ul class=\"icon-list-vertical box-gray-border box-pad clear\"><li><a class=\"icon-abstract\" title=\"View the Abstract\" href=\"/doi/abs/10.1139/blah\">" +
          "<span>Abstract</span></a></li><li><a class=\"icon-pdf\" href=\"/doi/pdf/10.1139/blah\">" +
          "PDF (207 K)" +
          "</a></li><li><a class=\"icon-pdf-plus\" href=\"/doi/pdfplus/10.1139/blah\">" +
          "PDF-Plus (208 K)" +
          "</a></li><li><a class=\"icon-figures\" id=\"figures\" href=\"javascript:void(0);\">Figures</a></li>" +
          "<li><a class=\"icon-references\" href=\"#ttl5\">References</a></li><li><a href=\"/action/z\" class=\"icon-recommended\">Also read</a></li>" +
          "<li><a class=\"icon-citing\" href=\"#citart1\">Citing articles</a></li>" +
          "<li><a href=\"/action/\" class=\"icon-recommended\">Also read</a></li>" +
          "<li><a href=\"/doi/abs/10.1139/blah\" class=\"icon-related\">Corrected article</a></li>" +
          "</ul><!-- abstract content -->" +
          "<h2 id=\"ttl12\">References</h2><ul class=\"no-bullet no-padding\">" +
          "<li id=\"ref1\"><span class=\"numbering\"></span>" +
          "</li>" +
          "</ul>";
  private static final String HtmlTestCrossLinksFiltered =
      "<ul class=\"icon-list-vertical box-gray-border box-pad clear\"><li><a class=\"icon-abstract\" title=\"View the Abstract\" href=\"/doi/abs/10.1139/blah\">" +
          "<span>Abstract</span></a></li><li><a class=\"icon-pdf\" href=\"/doi/pdf/10.1139/blah\">" +
          "PDF (207 K)" +
          "</a></li><li><a class=\"icon-pdf-plus\" href=\"/doi/pdfplus/10.1139/blah\">" +
          "PDF-Plus (208 K)" +
          "</a></li><li><a class=\"icon-figures\" id=\"figures\" href=\"javascript:void(0);\">Figures</a></li>" +
          "<li><a class=\"icon-references\" href=\"#ttl5\">References</a></li><li></li>" +
          "<li></li>" +
          "<li></li>" +
          "<li></li>" +
          "</ul><!-- abstract content -->" +
          "<h2 id=\"ttl12\">References</h2><ul class=\"no-bullet no-padding\">" +
          "<li id=\"ref1\"><span class=\"numbering\"></span>" +
          "</li>" +
          "</ul>";
  
  private static final String articleNav=
      "<p class=\"float-right no-margin\">" +
          "<a title=\"Previous Article\" class=\"white-link-right\" href=\"/doi/abs/10.1111/z90-092\">" +
          "      * Previous</a>" +
          " <a class=\"white-link-right\" href=\"http://www.nrcresearchpress.com/toc/test/68/4\">" +
          "   TOC" +
          "  </a>" +
          " <a title=\"Next Article\" class=\"white-link-right\" href=\"/doi/abs/10.1111/z90-094\">" +
          " Next *" +
          " </a>" +
          " </p>";
  private static final String articleNavFiltered=
      "<p class=\"float-right no-margin\">" +
          "   </p>";  
  
  private static final String issueNav=
      "<p class=\"float-right\">" +
          " <a title=\"Previous Issue\" class=\"red-link-right\" href=\"/toc/cjb/91/5\">" +
          " * Previous Issue</a>" +
          " <a title=\"Next Issue\" class=\"red-link-right\" href=\"/toc/cjb/91/7\">" +
          " Next Issue *</a>" +
          " </p>";        
  private static final String issueNavFiltered=
      "<p class=\"float-right\">" +
          "   </p>";        


  //Variant to test with Crawl Filter
  public static class TestCrawl extends TestNRCResearchPressHtmlCrawlFilterFactory {

    public void setUp() throws Exception {
      super.setUp();
      fact = new ClockssNRCResearchPressHtmlCrawlFilterFactory();
    }

  }


  public static Test suite() {
    return variantSuites(new Class[] {
        TestCrawl.class,
    });
  }

  public void testAlsoReadHtmlFiltering() throws Exception {
    InputStream actIn1 = fact.createFilteredInputStream(mau,
        new StringInputStream(HtmlTest1), Constants.DEFAULT_ENCODING);
    InputStream actIn2 = fact.createFilteredInputStream(mau,
        new StringInputStream(HtmlTest2), Constants.DEFAULT_ENCODING);
    InputStream actIn3 = fact.createFilteredInputStream(mau,
        new StringInputStream(HtmlTest3), Constants.DEFAULT_ENCODING);
    InputStream actIn4 = fact.createFilteredInputStream(mau,
        new StringInputStream(HtmlTest4), Constants.DEFAULT_ENCODING);
    InputStream actIn5 = fact.createFilteredInputStream(mau,
        new StringInputStream(HtmlTest5), Constants.DEFAULT_ENCODING);
    InputStream actIn6 = fact.createFilteredInputStream(mau,
        new StringInputStream(HtmlTest6), Constants.DEFAULT_ENCODING);
    InputStream actIn7 = fact.createFilteredInputStream(mau,
        new StringInputStream(HtmlTest7), Constants.DEFAULT_ENCODING);

    assertEquals(HtmlTest1Filtered, StringUtil.fromInputStream(actIn1));
    //assertEquals(HtmlTest2Filtered, StringUtil.fromInputStream(actIn2));
    assertEquals(HtmlTest3Filtered, StringUtil.fromInputStream(actIn3));
    assertEquals(HtmlTest4Filtered, StringUtil.fromInputStream(actIn4));
    assertEquals(HtmlTest5Filtered, StringUtil.fromInputStream(actIn5));
    assertEquals(HtmlTest6Filtered, StringUtil.fromInputStream(actIn6));
    assertEquals(HtmlTest7Filtered, StringUtil.fromInputStream(actIn7));

    actIn1 = fact.createFilteredInputStream(mau,
        new StringInputStream(HtmlTestCrossLinks), Constants.DEFAULT_ENCODING);
    assertEquals(HtmlTestCrossLinksFiltered, StringUtil.fromInputStream(actIn1));
    
    actIn1 = fact.createFilteredInputStream(mau,
        new StringInputStream(articleNav), Constants.DEFAULT_ENCODING);
    assertEquals(articleNavFiltered, StringUtil.fromInputStream(actIn1));

    actIn1 = fact.createFilteredInputStream(mau,
        new StringInputStream(issueNav), Constants.DEFAULT_ENCODING);
    assertEquals(issueNavFiltered, StringUtil.fromInputStream(actIn1));


  }

}
