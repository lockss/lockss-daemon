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

package org.lockss.plugin.atypon.endocrinesociety;

import java.io.InputStream;
import org.htmlparser.NodeFilter;
import org.lockss.filter.html.*;
import org.lockss.plugin.*;
import org.lockss.plugin.atypon.BaseAtyponHtmlHashFilterFactory;

public class EndocrineSocietyHtmlHashFilterFactory 
  extends BaseAtyponHtmlHashFilterFactory {
  
  @Override
  public InputStream createFilteredInputStream(ArchivalUnit au,
      InputStream in, String encoding) {
    
    NodeFilter[] filters = new NodeFilter[] {
        // handled by parent: script, sfxlink, stylesheet, pdfplus file sise
        // <head> tag, <li> item has the text "Cited by"
        
        // top panel with 'subscribe'
        // http://press.endocrine.org/doi/full/10.1210/en.2012-2147
        HtmlNodeFilters.tagWithAttributeRegex("div", "class", "gutterless"),
        // nav journal - current past issues, about, authors
        // http://press.endocrine.org/doi/full/10.1210/en.2012-2147
        HtmlNodeFilters.tagWithAttributeRegex("div", "class", "nav-journal"),
        // toc - free.gif image
        HtmlNodeFilters.tagWithAttributeRegex("img", "src", "free.gif"),
        // toc - this seems unused but may get turned on
        // http://press.endocrine.org/doi/full/10.1210/er.2013-1012
        HtmlNodeFilters.tagWithAttribute("div",  "id", "MathJax_Message"),
        // showCitFormats - Support and Help block
        // http://press.endocrine.org/action/showCitFormats?
        // doi=10.1210%2Fjc.2013-1811
        HtmlNodeFilters.tagWithAttributeRegex("div", "class", 
                                              "twoColumnRightDropZoneColor"),
        // on TOC, the links to various aspects (pdf, abstract, permissions) gets 
        // added to and we can hash it out. NOT crawl filter
        HtmlNodeFilters.tagWithAttribute("div", "class", "tocDeliverFormatsLinks"),

        // on full text and referenes page the ways to linkout to the reference get
        // added to (GoogleScholar, Medline, ISI, abstract, etc)
        // leave the content (NLM_article-title, NLM_year, etc),
        // but remove everything else (links and punctuation)
        HtmlNodeFilters.allExceptSubtree(
            HtmlNodeFilters.tagWithAttributeRegex( 
                "div", "class", "references"),
            HtmlNodeFilters.tagWithAttributeRegex(
                "span", "class", "NLM_")),

        // right column of an article - hash doesn't care about download link
        // note: institution banner is inside sidebar-right
        HtmlNodeFilters.tagWithAttributeRegex("div", "class", "literatumRightSidebar"),
    };
    
    // super.createFilteredInputStream adds filters to the baseAtyponFilters
    // and returns the filtered input stream using an array of NodeFilters that 
    // combine the two arrays of NodeFilters.
    return super.createFilteredInputStream(au, in, encoding, filters);
    }
    
  @Override
  public boolean doTagIDFiltering() {
    return true;
  }
  
  @Override
  public boolean doWSFiltering() {
    return true;
  }
  
}
