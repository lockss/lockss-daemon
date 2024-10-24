/*

Copyright (c) 2000-2024, Board of Trustees of Leland Stanford Jr. University

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

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS \"AS IS\"
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


package org.lockss.plugin.plos;

import java.io.InputStream;

import org.lockss.test.LockssTestCase;
import org.lockss.test.MockArchivalUnit;
import org.lockss.test.StringInputStream;
import org.lockss.util.Constants;
import org.lockss.util.StringUtil;

import kotlin.Experimental;

public class TestPLoSHtmlCrawlFilterFactory extends LockssTestCase{
      static String ENC = Constants.DEFAULT_ENCODING;

  private PLoSHtmlCrawlFilterFactory fact;
  private MockArchivalUnit mau;
  
  public void setUp() throws Exception {
    super.setUp();
    fact = new PLoSHtmlCrawlFilterFactory();
    mau = new MockArchivalUnit();
  }
  
 
  private static final String ammendmentSection =
    "<div class=\"amendment amendment-correction toc-section\">"+
        "<a data-toc=\"amendment-0\" title=\"Correction\" id=\"amendment-0\" name=\"amendment-0\"></a>"+
            "<h2>Correction</h2>"+
            "<div class=\"amendment-citation\">"+
            "<p>"+
                "<span class=\"amendment-date\">"+
                    "16 Mar 2004:"+
                "</span>"+
                "(2004)"+
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>Corrections: The Roles of APC and Axin Derived from Experimental and Theoretical Analysis of the Wnt Pathway."+
                "PLOS Biology  2(3): e89."+

                "<a   href=\"https://doi.org/10.1371/journal.pbio.0020089\">https://doi.org/10.1371/journal.pbio.0020089</a>"+
                "<span class=\"link-separator\"></span>"+
                    "<a href=\"/plosbiology/article?id=10.1371/journal.pbio.0020089\" class=\"amendment-link\">"+
                        "View correction"+
                    "</a>"+
            "</p>"+
        "</div>"+
    "</div>";
  
  private static final String relatedArticles =
  "<div class=\"related-articles-container\">"+
    "<h3>"+
        "Related PLOS Articles"+
    "</h3>"+
    "<ul class=\"related-articles\">"+
        "<li>"+
          "<b>has CORRECTION</b>"+
        "</li>"+
            "<li>"+
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>Corrections: The Roles of APC and Axin Derived from Experimental and Theoretical Analysis of the Wnt Pathway"+
                "<ul class=\"related-article-links\">"+
                    "<li class=\"related-article-link-page\">"+
                        "<a href=\"/plosbiology/article?id=10.1371/journal.pbio.0020089\">View Page</a>"+
                    "</li>"+
                    "<!--  Annotations and issue images do not have printables. They can be filtered by doi. -->"+
                    "<li class=\"related-article-link-download\">"+
                        "<a href=\"/plosbiology/article/file?type=printable&id=10.1371/journal.pbio.0020089\" target=\"_blank\" title=\"PDF opens in new window\">PDF</a>"+
                    "</li>"+
                "</ul>"+
            "</li>"+
        "<li>"+
            "<b>has COMPANION</b>"+
        "</li>"+
        "<li>"+
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>Mathematical Modeling Predicts How Proteins Affect Cellular Communication<ul class=\"related-article-links\">"+
                "<li class=\"related-article-link-page\">"+
                  "<a href=\"/plosbiology/article?id=10.1371/journal.pbio.0000032\">View Page</a>"+
                "</li>"+
                "<!--  Annotations and issue images do not have printables. They can be filtered by doi. -->"+
                "<li class=\"related-article-link-download\">"+
                    "<a href=\"/plosbiology/article/file?type=printable&id=10.1371/journal.pbio.0000032\" target=\"_blank\" title=\"PDF opens in new window\">PDF</a>"+
                "</li>"+
              "</ul>"+
        "</li>"+
        "</ul>"+
    "</div>";
		  
  
  private static final String filtered = "";
  
  public void testFiltering() throws Exception {
    InputStream inA;
    
    inA = fact.createFilteredInputStream(mau, new StringInputStream(ammendmentSection),
			 Constants.DEFAULT_ENCODING);
    assertEquals(filtered, StringUtil.fromInputStream(inA));

    inA = fact.createFilteredInputStream(mau, new StringInputStream(relatedArticles),
            Constants.DEFAULT_ENCODING);
    assertEquals(filtered, StringUtil.fromInputStream(inA));

  }
    
}
