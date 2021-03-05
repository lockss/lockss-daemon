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

package org.lockss.plugin.atypon.asco;

import java.io.InputStream;

import org.htmlparser.NodeFilter;
import org.lockss.filter.html.HtmlNodeFilters;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.plugin.atypon.BaseAtyponHtmlHashFilterFactory;
import org.lockss.util.Logger;

public class AscoHtmlHashFilterFactory 
  extends BaseAtyponHtmlHashFilterFactory {

  private static final Logger log = Logger.getLogger(AscoHtmlHashFilterFactory.class);
  
  @Override
  public InputStream createFilteredInputStream(ArchivalUnit au,
                                               InputStream in,
                                               String encoding) {
    NodeFilter[] filters = new NodeFilter[] {
        // handled by parent: script, sfxlink, stylesheet
        // style
        // page header: login, register, etc., and journal menu such as
        // subscribe, alerts, ...
        HtmlNodeFilters.tagWithAttribute("header", "class", "page-header"),
        
        // toc - middle column; right column is literatumAd
        HtmlNodeFilters.tagWithAttributeRegex("div", "class", "toc-tools"),
        // article right column; right column is literatumAd
        HtmlNodeFilters.tagWithAttributeRegex("div", "class", "article-tools"),
        
        //showCitationsPage just verify the articleList info
        HtmlNodeFilters.allExceptSubtree(
            HtmlNodeFilters.tagWithAttributeRegex(
                "div", "class", "downloadCitationsWidget"),
            HtmlNodeFilters.tagWithAttribute(
                "div", "class", "articleList")), 
        
        // on full text and referenes page the ways to linkout to the reference get 
        // added to (GoogleScholar, Medline, ISI, abstract, etc) 
        // leave the content (NLM_article-title, NLM_year, etc), 
        // but remove everything else (links and punctuation between options) 
        HtmlNodeFilters.allExceptSubtree(
            HtmlNodeFilters.tagWithAttribute("table", "class", "references"),
            HtmlNodeFilters.tagWithAttributeRegex("span", "class", "NLM_")),

    };
    // super.createFilteredInputStream adds bir filter to the baseAtyponFilters
    // and returns the filtered input stream using an array of NodeFilters that 
    // combine the two arrays of NodeFilters.
    return super.createFilteredInputStream(au, in, encoding, filters);
  }

  public boolean doTagRemovalFiltering() {
    return true;
  }

  @Override
  public boolean doWSFiltering() {
    return true;
  }

}

