/*

Copyright (c) 2000-2026, Board of Trustees of Leland Stanford Jr. University

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

package org.lockss.plugin.pub2web.ms;

import java.io.InputStream;
import org.htmlparser.NodeFilter;
import org.htmlparser.filters.*;
import org.lockss.filter.html.*;
import org.lockss.plugin.*;

public class MsHtmlHashFilterFactory implements FilterFactory {
  

  protected static NodeFilter[] infilters = new NodeFilter[] {
    // You need the main-content-container to get the manifest page listing
    // this is also the main container for the article landing page
    HtmlNodeFilters.tagWithAttributeRegex("main", "class", "main-content-container"),
    // The renderList chunks that populate the TOC
    HtmlNodeFilters.tagWithAttributeRegex("div", "class",  "articleListContainer"),
    // for the full-text html crawler version, you need the various article chuns
    HtmlNodeFilters.tagWithAttributeRegex("div", "id", "article-level-0"),
    // for the figures and tables landing pages - it's a list-group of these
    HtmlNodeFilters.tagWithAttributeRegex("div", "class", "singleFigureContainer"),
    //supp data landing page
    HtmlNodeFilters.tagWithAttribute("div", "id", "SuppDataIndexList"),
    HtmlNodeFilters.tagWithText("title","Side-by-Side view "),
    //do not need the export-list citation download links for hash filter
  };
  protected static NodeFilter[] xfilters = new NodeFilter[] {
    
    HtmlNodeFilters.tag("nav"),
    HtmlNodeFilters.tag("script"),
    HtmlNodeFilters.tag("noscript"),
    
    // We have the main container, now start taking bits of that out
    HtmlNodeFilters.tagWithAttribute("ol",  "class", "breadcrumb"),
    HtmlNodeFilters.tagWithAttributeRegex("a",  "class", "banner-container journal-banner"),
    HtmlNodeFilters.tagWithAttribute("nav",  "class", "pillscontainer"),
    //the "Cited by" tab lists a number that can change
    HtmlNodeFilters.tagWithAttribute("li",  "id", "cite"),
    
    //remove the TOC navigation links except the full TOC pdf

    //remove article landing page navigation links
    HtmlNodeFilters.tagWithAttribute("li",  "class", "previousLinkContainer"),
    HtmlNodeFilters.tagWithAttribute("li",  "class", "indexLinkContainer"),
    HtmlNodeFilters.tagWithAttribute("li",  "class", "nextLinkContainer"),

    //TODO - these came from ASM, must look at MS more closely
    HtmlNodeFilters.tagWithAttributeRegex("div", "class", "(citations|references|relatedContent)"),

    HtmlNodeFilters.tagWithAttribute("div", "class", "hiddenjsdiv metricsEndDate"),
    HtmlNodeFilters.tagWithAttributeRegex("div",  "class",  "^metrics "),
    HtmlNodeFilters.tagWithAttribute("input",  "name", "copyright"),
    HtmlNodeFilters.tagWithAttribute("div", "class", "crossSelling"),
    HtmlNodeFilters.tagWithAttribute("div", "id", "related"),
    //every now and then the order of the xml, html, pdf links are different - dynamically generated
    HtmlNodeFilters.tagWithAttribute("div",  "class", "contentTypeOptions"),
    //every now and then the server fails to serve the next/prev link...what? - on journals
    HtmlNodeFilters.tagWithAttribute("div",  "class", "articlenav"),
    HtmlNodeFilters.tagWithAttribute("div",  "id", "relatedcontent"), //tab contents, not the header
    HtmlNodeFilters.tagWithAttribute("div",  "id", "otherJournals"), // tab contents
    
    // <span class="access_icon_s keyicon accesskey-icon"
    HtmlNodeFilters.tagWithAttributeRegex("span", "class", "access_icon"),

    // filter out the following https://www.microbiologyresearch.org/content/journal/jmm/10.1099/jmm.0.000032     
    HtmlNodeFilters.tagWithAttributeRegex("div", "class", "mostcitedcontainer"),
    HtmlNodeFilters.tagWithAttributeRegex("div", "class", "mostreadcontainer"),
    HtmlNodeFilters.tagWithAttributeRegex("div", "class", "mostviewedloading"),
    HtmlNodeFilters.tagWithAttributeRegex("div", "class", "copyright-info"),
    HtmlNodeFilters.tagWithAttributeRegex("div", "id", "metrics_content"),
    HtmlNodeFilters.tagWithAttributeRegex("form", "id", "pptDwnld"),
    HtmlNodeFilters.tagWithAttributeRegex("div", "class", "itemFullTextLoading"),
    HtmlNodeFilters.tagWithAttributeRegex("div", "class", "hiddenjsdiv"),
    HtmlNodeFilters.tagWithAttributeRegex("form", "id","dataandmedia"),
    HtmlNodeFilters.tagWithAttributeRegex("div", "class","itemDataMediaLoading"),
    HtmlNodeFilters.tagWithAttributeRegex("div", "class", "journal-volume-issue-container"),
    HtmlNodeFilters.tagWithAttributeRegex("div", "class", "article-access-icon-and-access"),
    HtmlNodeFilters.tagWithAttributeRegex("div", "class", "hidden-js-div"),
    HtmlNodeFilters.tagWithAttributeRegex("a", "href", "^/cdn-cgi/")
          
  };

  @Override
  public InputStream createFilteredInputStream(ArchivalUnit au,
      InputStream in, String encoding) {
    
    return new HtmlFilterInputStream(in,
        encoding,
        new HtmlCompoundTransform(
            HtmlNodeFilterTransform.include(new OrFilter(infilters)),
            HtmlNodeFilterTransform.exclude(new OrFilter(xfilters))
            ));
  }
    
}
