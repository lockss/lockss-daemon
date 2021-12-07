/*

Copyright (c) 2000-2021, Board of Trustees of Leland Stanford Jr. University
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
