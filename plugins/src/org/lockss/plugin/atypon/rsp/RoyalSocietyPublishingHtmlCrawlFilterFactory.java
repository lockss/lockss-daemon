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

package org.lockss.plugin.atypon.rsp;

import org.htmlparser.NodeFilter;
import org.lockss.daemon.PluginException;
import org.lockss.filter.html.HtmlNodeFilters;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.plugin.atypon.BaseAtyponHtmlCrawlFilterFactory;

import java.io.InputStream;

public class RoyalSocietyPublishingHtmlCrawlFilterFactory extends BaseAtyponHtmlCrawlFilterFactory {

  //Royal Society Publishing moved from Highwire Drupal to Atypon, the html structure is not quite same as other Atypons
  //For example, the following two jid has different html structures
  //https://royalsocietypublishing.org/doi/full/10.1098/rsbm.2018.0002
  //https://royalsocietypublishing.org/toc/rsbm/64
  //https://royalsocietypublishing.org/toc/rsbl/14/10
  //https://royalsocietypublishing.org/doi/full/10.1098/rsbm.2018.0002
  //https://royalsocietypublishing.org/doi/full/10.1098/rsbl.2018.0532
  NodeFilter[] filters = new NodeFilter[] {
          // NOTE: overcrawling is an occasional issue with in-line references to "original article"
          HtmlNodeFilters.tag("header"),
          HtmlNodeFilters.tag("footer"),

          // Remove from toc page
          HtmlNodeFilters.tagWithAttributeRegex("div", "class", "publication-header"),
          HtmlNodeFilters.tagWithAttributeRegex("div", "class", "navigation-column"),
          HtmlNodeFilters.tagWithAttributeRegex("div", "class", "toc-right-side"),
          HtmlNodeFilters.tagWithAttributeRegex("div", "class", "content-navigation"),

          // Remove from doi/full/abs/refererence page
          HtmlNodeFilters.tagWithAttributeRegex("div", "class", "article-row-right"),

          // Need to download citation from this div
          HtmlNodeFilters.allExceptSubtree(
                  HtmlNodeFilters.tagWithAttribute("div", "class", "left-side"),
                  HtmlNodeFilters.tagWithAttributeRegex(
                          "a", "href", "/action/showCitFormats\\?"))
  };

  public InputStream createFilteredInputStream(ArchivalUnit au,
                                               InputStream in, String encoding) throws PluginException {
    return super.createFilteredInputStream(au, in, encoding, filters);
  }

}

