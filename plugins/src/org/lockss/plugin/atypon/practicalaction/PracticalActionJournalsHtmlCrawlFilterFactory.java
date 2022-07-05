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

package org.lockss.plugin.atypon.practicalaction;

import java.io.InputStream;

import org.htmlparser.NodeFilter;
import org.lockss.daemon.PluginException;
import org.lockss.filter.html.*;
import org.lockss.plugin.*;
import org.lockss.plugin.atypon.BaseAtyponHtmlCrawlFilterFactory;

public class PracticalActionJournalsHtmlCrawlFilterFactory 
  extends BaseAtyponHtmlCrawlFilterFactory {
  
  static NodeFilter[] filters = new NodeFilter[] {
    // handled by parent:
    // toc previous/next issue and article - <td class="journalNavRightTd">
    // changed - now literatumMostRead literatumMostCited literatumBookIssueNav
    
    // toc, abs, ref - breadcrumbs
    // http://www.developmentbookshelf.com/doi/abs/10.3362/1755-1986.2014.004
    HtmlNodeFilters.tagWithAttribute("ul", "class", "breadcrumbs"),    
    // toc, abs, ref - panel under breadcrumbs with link to
    // Current Issue, or right sidebar top block of abstract, full text and ref
    // http://www.developmentbookshelf.com/toc/edm/25/1
    // http://www.developmentbookshelf.com/doi/abs/10.3362/1755-1986.2014.004
    // but we want to get the cover.gif which is also in the panel
    //HtmlNodeFilters.tagWithAttributeRegex("div", "class", "body-emphasis"),
    HtmlNodeFilters.allExceptSubtree(
        HtmlNodeFilters.tagWithAttributeRegex("div", "class", "body-emphasis"),
        HtmlNodeFilters.tagWithAttributeRegex(
            "img", "alt", "Publication Cover")),
            
    // toc, abs, ref - right column most read/most cited
    // too restrictive - it relates to any tabbed content, which could be main
    // TODO - look for a better solution
    HtmlNodeFilters.tagWithAttribute("div", "aria-relevant", "additions"),
    // in-line links to other chapters, etc
    HtmlNodeFilters.tagWithAttribute("a", "class", "ext-link"),


  };

  @Override
  public InputStream createFilteredInputStream(ArchivalUnit au,
      InputStream in, String encoding) throws PluginException{
    return super.createFilteredInputStream(au, in, encoding, filters);
  }
  
}
