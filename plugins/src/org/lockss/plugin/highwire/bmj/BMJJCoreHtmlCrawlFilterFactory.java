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

package org.lockss.plugin.highwire.bmj;

import java.io.InputStream;

import org.htmlparser.NodeFilter;
import org.lockss.daemon.PluginException;
import org.lockss.filter.html.HtmlFilterInputStream;
import org.lockss.filter.html.HtmlNodeFilters;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.plugin.highwire.HighWireJCoreHtmlCrawlFilterFactory;
import org.lockss.util.Logger;

public class BMJJCoreHtmlCrawlFilterFactory extends HighWireJCoreHtmlCrawlFilterFactory {
  
  private static final Logger log = Logger.getLogger(BMJJCoreHtmlCrawlFilterFactory.class);
  
  protected static NodeFilter[] filters = new NodeFilter[] {
    HtmlNodeFilters.tagWithAttributeRegex("div", "class", "pager"),
    HtmlNodeFilters.tagWithAttributeRegex("span", "class", "prev"),
    HtmlNodeFilters.tagWithAttributeRegex("span", "class", "next"),
    HtmlNodeFilters.tagWithAttribute("div", "class", "section notes"),
    HtmlNodeFilters.tagWithAttribute("div", "class", "section fn-group"),
    // leave data supplement links for pages like http://www.bmj.com/content/332/7532/11/related
    HtmlNodeFilters.tagWithAttributeRegex("div", "class", "related-articles"),
    //  rapid response body, citations and anciallary links.
    // just remove the entire rapid response div. as the responses are printed in full, no need to get them.
    HtmlNodeFilters.tagWithAttributeRegex("div", "class", "(bmj-rapid-responses|bmj_related_rapid_responses)"),
    // in case something changes in the above regex
    HtmlNodeFilters.tagWithAttribute("div", "class", "response-body"),
    HtmlNodeFilters.tagWithAttribute("div", "class", "rr-right-column"),
    //
    HtmlNodeFilters.tagWithAttributeRegex("div", "class", "cited-by"),
    HtmlNodeFilters.tagWithAttributeRegex("div", "class", "additional-link"),
  };
  
  @Override
  public InputStream createFilteredInputStream(ArchivalUnit au,
                                               InputStream in,
                                               String encoding)
      throws PluginException {
    
    HtmlFilterInputStream filtered =
        (HtmlFilterInputStream) super.createFilteredInputStream(au, in, encoding, filters);
    return filtered;
  }
}
