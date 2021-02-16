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

package org.lockss.plugin.highwire;

import java.io.InputStream;

import org.htmlparser.NodeFilter;
import org.htmlparser.filters.*;
import org.lockss.daemon.PluginException;
import org.lockss.filter.html.*;
import org.lockss.plugin.*;

public class HighWirePressH20HtmlCrawlFilterFactory implements FilterFactory {
  
  protected static NodeFilter[] baseHWFilters = new NodeFilter[] {
    // Do not crawl header or footer for links
    HtmlNodeFilters.tag("header"),
    HtmlNodeFilters.tag("footer"),
    // Do not crawl for links from leaderboard-ads
    HtmlNodeFilters.tagWithAttributeRegex("div", "class", "leaderboard-ads"),
    // Do not crawl reference section, sidebar-qs for links
    HtmlNodeFilters.tagWithAttribute("div", "class", "section ref-list"),
    HtmlNodeFilters.tagWithAttributeRegex("div", "class", "sidebar-qs"),
    // messages can appear arbitrarily
    HtmlNodeFilters.tagWithAttributeRegex("div", "class", "messages"),
    HtmlNodeFilters.tagWithAttributeRegex("div", "id", "messages"),
    // Do not crawl for links in col3
    HtmlNodeFilters.tagWithAttribute("div", "id", "col-3"),
    // <div id="rel-related-article" class="relmgr-relation related">
    HtmlNodeFilters.tagWithAttributeRegex("div", "id", "(related|cited-by)"),
    // found hidden author index link after requesting HW fix links that 
    // resulted in 503 or connection reset fatal errors
    // <li class="aindex" style="display: none;">
    new AndFilter(
        HtmlNodeFilters.tagWithAttribute("li", "class", "aindex"),
        HtmlNodeFilters.tagWithAttributeRegex("li", "style", "display: *none")),
    // <a rel="issue-aindex" title="Index by Author" href="...author-index" style="display: none;">
    new AndFilter(
        HtmlNodeFilters.tagWithAttribute("a", "rel", "issue-aindex"),
        HtmlNodeFilters.tagWithAttributeRegex("a", "style", "display: *none")),
  };
  
  @Override
  public InputStream createFilteredInputStream(ArchivalUnit au,
                                               InputStream in,
                                               String encoding)
      throws PluginException {
    
    return new HtmlFilterInputStream(in, encoding,
        HtmlNodeFilterTransform.exclude(new OrFilter(baseHWFilters)));
  }
}
