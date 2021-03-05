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

package org.lockss.plugin.atypon.emeraldgroup;

import java.io.InputStream;

import org.htmlparser.NodeFilter;
import org.htmlparser.filters.*;
import org.lockss.daemon.PluginException;
import org.lockss.filter.html.*;
import org.lockss.plugin.*;
import org.lockss.plugin.atypon.BaseAtyponHtmlCrawlFilterFactory;

public class EmeraldGroupHtmlCrawlFilterFactory 
  extends BaseAtyponHtmlCrawlFilterFactory {
  
  static NodeFilter[] filters = new NodeFilter[] {
    
    //BaseAtypon covers most literaturFoo (Breacrumbs, ads, etc), 
    // but not this (journal info/nav at top of toc and article)
    HtmlNodeFilters.tagWithAttributeRegex("div", "class", "literatumSeriesNavigation"),
    
    // toc, abs, full - panel under breadcrubs with link to Current Issue,
    // http://www.emeraldinsight.com/toc/aaaj/26/8
    HtmlNodeFilters.tagWithAttributeRegex("li", "id", "currIssue"),
                          
    // toc, abs, full -  right column
    // there are 2 data-pb-dropzone="right", one of them is part of the top ad
    // it's not unique tag, but I think it's OK for Emerald
    // http://www.emeraldinsight.com/toc/aaaj/26/8
    // at some point, options was placed under <div data-pb-dropzone="right"
    HtmlNodeFilters.allExceptSubtree(
        HtmlNodeFilters.tagWithAttribute("div", "data-pb-dropzone", "right"),
        HtmlNodeFilters.tagWithAttributeRegex("div", "class", "options")),
    // from abs - all Articles Options and Tools except Download Citation
    // <div class="options">
    HtmlNodeFilters.allExceptSubtree(
        HtmlNodeFilters.tagWithAttributeRegex("div", "class", "options"),
          new OrFilter(new NodeFilter[] {
              HtmlNodeFilters.tagWithAttributeRegex("li", "class", "ref"),
              HtmlNodeFilters.tagWithAttributeRegex(
                  "a", "href", "^/action/showCitFormats\\?")})),
                  
    // tabb'd section in the right column -                
    // can't be in parent - all tabs would get affected, even in content
    // TODO - look at alternative, but for now the only tabs are in right column
    HtmlNodeFilters.tagWithAttribute("div", "aria-relevant", "additions"),
    // toc and article page
    HtmlNodeFilters.tagWithAttributeRegex("div", "class", "pageHeader"),
    HtmlNodeFilters.tagWithAttributeRegex("div", "class", "pageFooter"),

  };

  @Override
  public InputStream createFilteredInputStream(ArchivalUnit au,
      InputStream in, String encoding) throws PluginException{
    return super.createFilteredInputStream(au, in, encoding, filters);
  }
  
}
