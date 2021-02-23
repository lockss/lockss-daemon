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

package org.lockss.plugin.atypon.markallen;

import java.io.InputStream;

import org.htmlparser.NodeFilter;
import org.lockss.daemon.PluginException;
import org.lockss.filter.html.*;
import org.lockss.plugin.*;
import org.lockss.plugin.atypon.BaseAtyponHtmlCrawlFilterFactory;

// be sure not to CRAWL filter action/showCitFormats link
// might be in left or right column

public class MarkAllenHtmlCrawlFilterFactory 
  extends BaseAtyponHtmlCrawlFilterFactory {
  static NodeFilter[] filters = new NodeFilter[] {

    // handled by parent:
    // previous and next of toc
    // <td class="journalNavLeftTd">
    // <td class="journalNavRightTd">
    
    // from toc - below pageHeader, ad panel has link to other issue
    // http://www.magonlinelibrary.com/toc/bjom/21/10
    HtmlNodeFilters.tagWithAttributeRegex("div", "class", "genericSlideshow"),
    
    // tabb'd section in the right column -
    // can't be in parent - all tabs would get affected, even in content
    // TODO - look at alternative, but for now the only tabs are in right column
    HtmlNodeFilters.tagWithAttribute("div", "aria-relevant", "additions"),
    
    HtmlNodeFilters.tag("header"),
    HtmlNodeFilters.tag("footer"),
    HtmlNodeFilters.tagWithAttributeRegex("div", "class", "journalHeader"),
    HtmlNodeFilters.tagWithAttributeRegex("div", "class", "content-navigation"),
    // in case there are links in the preview text
    HtmlNodeFilters.tagWithAttributeRegex("div", "class", "toc-item__abstract"),
    // never want these links, excluded lists was too long
    HtmlNodeFilters.tagWithAttributeRegex("a", "href", "^/(author|keyword|personalize)/"),
    HtmlNodeFilters.tagWithAttributeRegex("a", "href", "/servlet/linkout[?]type="),
    // XXX not sure if needed, but ...
    HtmlNodeFilters.tagWithAttributeRegex("li", "class", "(correction|latest-version)"),
  };

  @Override
  public InputStream createFilteredInputStream(ArchivalUnit au,
      InputStream in,
      String encoding)
  throws PluginException{
    return super.createFilteredInputStream(au, in, encoding, filters);
  }
}
