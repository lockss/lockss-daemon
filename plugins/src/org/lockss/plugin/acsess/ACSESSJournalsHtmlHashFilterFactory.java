/*

Copyright (c) 2000-2023, Board of Trustees of Leland Stanford Jr. University

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

package org.lockss.plugin.acsess;

import java.io.InputStream;
import java.io.Reader;

import org.htmlparser.*;
import org.htmlparser.filters.OrFilter;
import org.htmlparser.filters.TagNameFilter;
import org.lockss.filter.FilterUtil;
import org.lockss.filter.html.HtmlCompoundTransform;
import org.lockss.filter.html.HtmlFilterInputStream;
import org.lockss.filter.html.HtmlNodeFilterTransform;
import org.lockss.filter.html.HtmlNodeFilters;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.plugin.FilterFactory;
import org.lockss.util.ReaderInputStream;

// Keeps contents only (includeNodes), then hashes out unwanted nodes 
// within the content (excludeNodes).
public class ACSESSJournalsHtmlHashFilterFactory implements FilterFactory {
     
  @Override
  public InputStream createFilteredInputStream(ArchivalUnit au,
                                               InputStream in, 
                                               String encoding) {
    NodeFilter[] includeNodes = new NodeFilter[] {
        // manifest, toc, abs, full, preview (pdf), citation manager - content we want
        // https://dl.sciencesocieties.org/publications/cns/tocs/47
        // https://dl.sciencesocieties.org/publications/cns/tocs/47/1
        // https://dl.sciencesocieties.org/publications/cns/abstracts/47/1/18/preview
        // https://dl.sciencesocieties.org/publications/cns/abstracts/47/1/32
        // https://dl.sciencesocieties.org/publications/cns/articles/47/1/28
        // https://dl.sciencesocieties.org/publications/citation-manager/prev/zt/cns/47/1/28
        //HtmlNodeFilters.tagWithAttribute("div", "id", "content-block"),
        //<div class="inside_one">
        HtmlNodeFilters.tagWithAttribute("div", "class", "inside_one"),
        // tables-only - tables
        HtmlNodeFilters.tagWithAttribute("div", "class", "table-expansion"),
        // figures-only - images
        HtmlNodeFilters.tagWithAttribute("div", "class", "fig-expansion")
    };
    
    NodeFilter[] excludeNodes = new NodeFilter[] {
        new TagNameFilter("script"),
        new TagNameFilter("noscript"),
        // filter out comments
        HtmlNodeFilters.comment(),
        // manifest, toc - links to facebook and twitter near footer
        HtmlNodeFilters.tagWithAttributeRegex("div", "class", "noPrint"),
        // abs, full -
        // https://dl.sciencesocieties.org/publications/cns/abstracts/47/1/32
        // https://dl.sciencesocieties.org/publications/cns/articles/47/1/28
        // <div class="content-box" id="article-cb-main">
        HtmlNodeFilters.tagWithAttributeRegex("div", "id", "article-cb-main"),
        // https://dl.sciencesocieties.org/publications/aj/tocs/106/1
        HtmlNodeFilters.tagWithAttribute("div", "class", "openAccess"),  
        // full - article footnotes
        HtmlNodeFilters.tagWithAttribute("div", "id", "articleFootnotes"), 
        // full, tables, figures - commnents section
        HtmlNodeFilters.tagWithAttribute("div", "id", "comments"),
        HtmlNodeFilters.tagWithAttribute("div", "id", "commentBox"),
    };
    
    return getFilteredInputStream(au, in, encoding, 
                                  includeNodes, excludeNodes);
  }
  
  // Takes include and exclude nodes as input. Removes white spaces.
  public InputStream getFilteredInputStream(ArchivalUnit au, InputStream in,
      String encoding, NodeFilter[] includeNodes, NodeFilter[] excludeNodes) {
    if (excludeNodes == null) {
      throw new NullPointerException("excludeNodes array is null");
    }  
    if (includeNodes == null) {
      throw new NullPointerException("includeNodes array is null!");
    }   
    InputStream filtered;
    filtered = new HtmlFilterInputStream(in, encoding,
                 new HtmlCompoundTransform(
                     HtmlNodeFilterTransform.include(new OrFilter(includeNodes)),
                     HtmlNodeFilterTransform.exclude(new OrFilter(excludeNodes)))
               );
    
    Reader reader = FilterUtil.getReader(filtered, encoding);
    return new ReaderInputStream(reader); 
  }

}
