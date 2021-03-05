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

package org.lockss.plugin.atypon.aslha;

import java.io.InputStream;

import org.htmlparser.NodeFilter;
import org.lockss.daemon.PluginException;
import org.lockss.filter.html.*;
import org.lockss.plugin.*;
import org.lockss.plugin.atypon.BaseAtyponHtmlCrawlFilterFactory;

/*
 *
 * created because article links are not grouped under a journalid or volumeid,
 * but under article ids - will pull the links from the page, so filtering out
 * extraneous links
 * 
 */
public class AmericanSpeechLanguageHearingAssocHtmlCrawlFilterFactory extends BaseAtyponHtmlCrawlFilterFactory {

  NodeFilter[] filters = new NodeFilter[] {
          // NOTE: overcrawling is an occasional issue with in-line references to "original article"
          HtmlNodeFilters.tag("header"),
          HtmlNodeFilters.tag("footer"),

          // Remove from toc page
          HtmlNodeFilters.tagWithAttributeRegex("div", "class", "page-top-banner"),
          // Remove from toc page
          HtmlNodeFilters.tagWithAttributeRegex("div", "class", "publication__menu"),
          // Remove from toc page
          // Need to download citation from this div
          HtmlNodeFilters.allExceptSubtree(
                  HtmlNodeFilters.tagWithAttribute("div", "class", "actionsbar"),
                  HtmlNodeFilters.tagWithAttributeRegex(
                          "a", "href", "/action/showCitFormats\\?")),
          // Remove from toc page
          HtmlNodeFilters.tagWithAttributeRegex("div", "class", "social-menus"),
          // Remove from toc page
          HtmlNodeFilters.tagWithAttributeRegex("div", "class", "table-of-content__navigation"),
          // Remove from toc page, this is the birdview image
          HtmlNodeFilters.tagWithAttributeRegex("div", "class", "current-issue"),
          // Remove from toc page
          HtmlNodeFilters.tagWithAttributeRegex("div", "class", "advertisement"),

          // Remove from doi/full page
          HtmlNodeFilters.tagWithAttributeRegex("div", "class", "page-top-panel"),
          // Remove from doi/full page
          HtmlNodeFilters.tagWithAttributeRegex("ul", "class", "tab__nav"),
          // Remove from doi/full page
          HtmlNodeFilters.tagWithAttributeRegex("ul", "class", "tab__content"),
          // Remove from doi/full page
          HtmlNodeFilters.tagWithAttributeRegex("div", "class", "eCommercePurchaseAccessWidget")

  };
  
  public InputStream createFilteredInputStream(ArchivalUnit au,
      InputStream in, String encoding) throws PluginException{ 
    return super.createFilteredInputStream(au, in, encoding, filters);
  }
  
}

