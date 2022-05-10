/*

Copyright (c) 2000-2022, Board of Trustees of Leland Stanford Jr. University

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

package org.lockss.plugin.ojs3;

import java.io.InputStream;


//import java.io.FileInputStream;
//import java.io.FileOutputStream;
//import org.apache.commons.io.IOUtils;

import org.htmlparser.NodeFilter;
import org.htmlparser.filters.OrFilter;
import org.lockss.filter.html.HtmlCompoundTransform;
import org.lockss.filter.html.HtmlFilterInputStream;
import org.lockss.filter.html.HtmlNodeFilterTransform;
import org.lockss.filter.html.HtmlNodeFilters;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.plugin.FilterFactory;
import org.lockss.util.Logger;

/*
 * SO far we only have two publishers who have migrated to OJS3 and the html is basic and
 * very similar 
 */

public class Ojs3HtmlHashFilterFactory implements FilterFactory {

  private static final Logger log = Logger.getLogger(Ojs3HtmlHashFilterFactory.class);

  private static final NodeFilter[] includeNodes = new NodeFilter[] {
	    	// manifest page
	    	HtmlNodeFilters.tagWithAttributeRegex("div",  "class", "page clockss"),
            HtmlNodeFilters.tagWithAttributeRegex("div",  "class", "page lockss"),
	        // toc - contents only
	        HtmlNodeFilters.tagWithAttribute("div", "class", "issue-toc"),
	        // abstract landing page html
	        HtmlNodeFilters.tagWithAttribute("article", "class", "article-details"),
	        // pdf landing page - just get the header with title and link information
	        HtmlNodeFilters.tagWithAttribute("header", "class", "header_view"),
            // article page, https://journals.vgtu.lt/index.php/BME/article/view/10292

            HtmlNodeFilters.tagWithAttribute("h2", "class", "headings"),
            HtmlNodeFilters.tagWithAttribute("div", "class", "authors"),
            HtmlNodeFilters.tagWithAttribute("div", "id", "article_tab"),
            // article page: https://www.clei.org/cleiej/index.php/cleiej/article/view/202
            HtmlNodeFilters.tagWithAttribute("div", "class", "main_entry"),

            // article page: 	https://haematologica.org/article/view/10276
            HtmlNodeFilters.tagWithAttributeRegex("div", "class", "article-container"),
            // issue page: https://haematologica.org/issue/view/377
            HtmlNodeFilters.tagWithAttributeRegex("div", "class", "one-article-intoc"),
            // the citation files https://ojs.aut.ac.nz/hospitality-insights/citationstylelanguage/get/acm-sig-proceedings?submissionId=58
            HtmlNodeFilters.tagWithAttribute("div", "class=", "csl-entry"), // alternatively div.csl-bib-body
	    };

    private static final NodeFilter[] includeNodes2 = new NodeFilter[] {};

    private static final NodeFilter[] excludeNodes = new NodeFilter[] {
	        // on the article landing page - remove the bottom stuff
	        HtmlNodeFilters.tagWithAttribute("section","class","article-more-details"),

	    };

    private static final NodeFilter[] excludeNodes2 = new NodeFilter[] {};
 
  @Override
  public InputStream createFilteredInputStream(ArchivalUnit au,
      InputStream in, 
      String encoding) {

      return new HtmlFilterInputStream(in,
            encoding,
            new HtmlCompoundTransform(
                HtmlNodeFilterTransform.include(new OrFilter(includeNodes)),
                HtmlNodeFilterTransform.exclude(new OrFilter(excludeNodes))
                ));
    
  }

}
