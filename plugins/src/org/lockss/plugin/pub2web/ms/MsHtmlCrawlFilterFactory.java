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
import org.lockss.daemon.PluginException;
import org.lockss.filter.html.*;
import org.lockss.plugin.*;

/* 
 * don't do include/exclude for crawl filter - too hard to get all the supporting
 * files.
 * It's okay to exclude the "aside" tag which contains the right column
 * We will pick up the export citation options from the TOC article listings
 * ....renderList?items= URLs
 */
public class MsHtmlCrawlFilterFactory implements FilterFactory {
  

  protected static NodeFilter[] xfilters = new NodeFilter[] {
    
    // Get rid of the big chunks
    HtmlNodeFilters.tag("header"),
    HtmlNodeFilters.tag("footer"),
    // right column and metrics, etc
    // citations will come off article listing portions of TOC
    //NOTE: cannot crawl filter all of "aside" or the toc implementation will not work 
    //HtmlNodeFilters.tag("aside"),
    
    // We have the main container, now start taking bits of that out
    HtmlNodeFilters.tagWithAttribute("ol",  "class", "breadcrumb"),
    HtmlNodeFilters.tagWithAttributeRegex("a",  "class", "banner-container journal-banner"),
    HtmlNodeFilters.tagWithAttribute("nav",  "class", "pillscontainer"),
    
    HtmlNodeFilters.tagWithAttribute("div",  "id", "sign-in"),
    HtmlNodeFilters.tagWithAttribute("div",  "id", "share-nav"),
    //remove the TOC navigation links except the full TOC pdf
    
    //remove the stuff on the right column, except the citation export
    HtmlNodeFilters.allExceptSubtree(
        HtmlNodeFilters.tagWithAttributeRegex("div","id", "tools-nav"), 
        HtmlNodeFilters.tagWithAttribute("ul","id", "export-list")),

    //remove article landing page navigation links
    HtmlNodeFilters.tagWithAttribute("li",  "class", "previousLinkContainer"),
    HtmlNodeFilters.tagWithAttribute("li",  "class", "indexLinkContainer"),
    HtmlNodeFilters.tagWithAttribute("li",  "class", "nextLinkContainer"),

    // in-line ref link
    HtmlNodeFilters.tagWithAttribute("span",  "class", "xref"),
    // reference section in full-text html, do both to be extra safe
    HtmlNodeFilters.tagWithAttribute("span",  "class", "references"),
    HtmlNodeFilters.tagWithAttribute("ol",  "class", "references"),
    
    // shows corrigendum and original of the same article - see
    // http://jmm.microbiologyresearch.org/content/journal/jmm/10.1099/jmm.0.000223 & 000059
    HtmlNodeFilters.tagWithAttribute("div",  "class", "consanguinityContainer"),   
    //In-line references to other articles from this journal
    HtmlNodeFilters.tagWithAttribute("a", "target", "xrefwindow"),
    HtmlNodeFilters.tagWithAttribute("div", "id", "relatedContent"),

    // filter out the following https://www.microbiologyresearch.org/content/journal/jgv/91/12

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
    
  };

  @Override
  public InputStream createFilteredInputStream(ArchivalUnit au,
      InputStream in, String encoding) throws PluginException {
    
 
    /* do the usual - just exclude */
    HtmlTransform transform = HtmlNodeFilterTransform.exclude(new OrFilter(xfilters));
    HtmlFilterInputStream fstream = new HtmlFilterInputStream(in,
        encoding,
        transform);
    return fstream;
 
  }
  
}
