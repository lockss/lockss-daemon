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

package org.lockss.plugin.pensoft;

import java.io.*;

import junit.framework.Test;

import org.lockss.util.*;
import org.lockss.plugin.FilterFactory;
import org.lockss.plugin.pensoft.PensoftHtmlCrawlFilterFactory;
import org.lockss.test.*;

public class TestPensoftHtmlCrawlFilterFactory extends LockssTestCase {
  private static FilterFactory fact;
  private static MockArchivalUnit mau;
  
  //Example instances mostly from pages of strings of HTMl that should be filtered
  //and the expected post filter HTML strings.
  
  //Related articles
  private static final String HtmlTest1 = 
    "<td width=\"586\" valign=\"top\" class=textver10>"+
    "<h3>Readers of this Article also read </h3>\n" +
    "<ul id=\"readers\">\n" +
    "<li><a href=\"a-new-support-measure-to-quantify-the-impact-of-local-optima-in-phylog-article-a2858\">A New Support Measure to Quantify the Impact of Local Optima in Phylogenetic Analyses</a></li>\n" +
    "<li><a href=\"a-novel-model-for-dna-sequence-similarity-analysis-based-on-graph-theo-article-a2855\">A Novel Model for DNA Sequence Similarity Analysis Based on Graph Theory</a></li>\n" +
    "<li><a href=\"factors-affecting-synonymous-codon-usage-bias-in-chloroplast-genome-of-article-a2916\">Factors Affecting Synonymous Codon Usage Bias in Chloroplast Genome of Oncidium Gower Ramsey</a></li>\n" +
    "<li><a href=\"single-domain-parvulins-constitute-a-specific-marker-for-recently-prop-article-a2835\">Single-Domain Parvulins Constitute a Specific Marker for Recently Proposed Deep-Branching Archaeal Subgroups</a></li>\n" +
    "<li><a href=\"phylogenomics-based-reconstruction-of-protozoan-species-tree-article-a2785\">Phylogenomics-Based Reconstruction of Protozoan Species Tree</a></li>\n" +
    "</ul>\n" +
    "</td>Hello";  
  private static final String HtmlTest1Filtered = "Hello";
  
 
  private static final String HtmlTest2 = 
    "  <td width=\"582\" valign=\"top\">"+
    "    <table border=0  cellspacing=0 cellpadding=0>"+
    "      <tr>"+
    "      <td align=center><a href=\"journals/zookeys/issue/276/\" class=more3>Current Issue</a></td>"+
    "      <td class=red2>|</td>"+
    "      <td align=center><a href=\"journals/zookeys/archive\" class=green>All Issues</a></td>"+
    "      </tr>"+
    "    </table>"+
    "   </td>";

  private static final String HtmlTest2Filtered = 
    "  <td width=\"582\" valign=\"top\">"+
    "    <table border=0  cellspacing=0 cellpadding=0>"+
    "      <tr>"+
    "      <td align=center></td>"+
    "      <td class=red2>|</td>"+
    "      <td align=center><a href=\"journals/zookeys/archive\" class=green>All Issues</a></td>"+
    "      </tr>"+
    "    </table>"+
    "   </td>";

 //Variant to test with Crawl Filter
 public static class TestCrawl extends TestPensoftHtmlCrawlFilterFactory {
          
          public void setUp() throws Exception {
                  super.setUp();
                  fact = new PensoftHtmlCrawlFilterFactory();
          }

  }
  

public static Test suite() {
    return variantSuites(new Class[] {
        TestCrawl.class,
      });
  }
  
  public void testAlsoReadHtmlFiltering() throws Exception {
    InputStream actIn1 = fact.createFilteredInputStream(mau,
                        new StringInputStream(HtmlTest1),
                        Constants.DEFAULT_ENCODING);
    InputStream actIn2 = fact.createFilteredInputStream(mau,
        new StringInputStream(HtmlTest2),
        Constants.DEFAULT_ENCODING);

    
    assertEquals(HtmlTest1Filtered, StringUtil.fromInputStream(actIn1));
    assertEquals(HtmlTest2Filtered, StringUtil.fromInputStream(actIn2));

  }
  
}
