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

package org.lockss.plugin.highwire.aaas;

import java.io.InputStream;

import org.htmlparser.NodeFilter;
import org.lockss.daemon.PluginException;
import org.lockss.filter.html.HtmlFilterInputStream;
import org.lockss.filter.html.HtmlNodeFilters;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.plugin.highwire.HighWireJCoreHtmlCrawlFilterFactory;
import org.lockss.util.Logger;

public class AAASHtmlCrawlFilterFactory extends HighWireJCoreHtmlCrawlFilterFactory {
  
  private static final Logger log = Logger.getLogger(AAASHtmlCrawlFilterFactory.class);
  
  @Override
  public InputStream createFilteredInputStream(ArchivalUnit au,
                                               InputStream in,
                                               String encoding)
      throws PluginException {
    
    String base_url = au.getConfiguration().get("base_url");
    String volume = au.getConfiguration().get("volume_name");
    
    String regexStr = base_url + "content/(?!" + volume + "/)";
    
    NodeFilter[] filters = new NodeFilter[] {
        HtmlNodeFilters.tagWithAttributeRegex("div", "class", "pager"),
        HtmlNodeFilters.tagWithAttribute("div", "class", "section notes"),
        HtmlNodeFilters.tagWithAttribute("div", "class", "section fn-group"),
        HtmlNodeFilters.tagWithAttributeRegex("div", "class", "related-articles"),
        HtmlNodeFilters.tagWithAttributeRegex("div", "class", "cited-by"),
        HtmlNodeFilters.tagWithAttributeRegex("div", "class", "additional-link"),
        HtmlNodeFilters.tagWithAttributeRegex("div", "class", "promo"),
        HtmlNodeFilters.tagWithAttributeRegex("div", "class", "foot"),
        HtmlNodeFilters.tagWithAttribute("div", "class", "secondary"),
        HtmlNodeFilters.tagWithAttributeRegex("a", "href", regexStr),
      };
    
    HtmlFilterInputStream filtered =
        (HtmlFilterInputStream) super.createFilteredInputStream(au, in, encoding, filters);
    return filtered;
  }
}
