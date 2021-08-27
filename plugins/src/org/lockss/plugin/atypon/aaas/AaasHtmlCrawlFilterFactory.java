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

package org.lockss.plugin.atypon.aaas;

import org.htmlparser.NodeFilter;
import org.lockss.daemon.PluginException;
import org.lockss.filter.html.HtmlNodeFilters;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.plugin.atypon.BaseAtyponHtmlCrawlFilterFactory;

import java.io.InputStream;

public class AaasHtmlCrawlFilterFactory extends BaseAtyponHtmlCrawlFilterFactory {

  static NodeFilter[] filters = new NodeFilter[] {
      // Everywhere
        // header/footer/menu(s)
      HtmlNodeFilters.tag("header"),
      HtmlNodeFilters.tag("footer"),
      HtmlNodeFilters.tag("aside"),
      HtmlNodeFilters.tagWithAttributeRegex("div", "class", "main-menu"),
      // TOC pages
        // related products embedded below articles
      HtmlNodeFilters.tagWithAttributeRegex("div", "class", "card-footer"),
      HtmlNodeFilters.tagWithAttributeRegex("div", "class", "card-related"),
        // previous/next issue buttons
      HtmlNodeFilters.tagWithAttributeRegex("div", "class", "content-navigation"),

      // Article pages
        // sidebar
      HtmlNodeFilters.tagWithAttributeRegex("div", "class", "article-aside"),
        // ref, cite, ack, bib, suppl,
      HtmlNodeFilters.tagWithAttribute("div", "id", "acknowledgments"),
      HtmlNodeFilters.tagWithAttribute("div", "id", "pill-references"),
        // citation section lists articles citing, if applicable.
      HtmlNodeFilters.tagWithAttribute("section", "id", "cited-by"),
      HtmlNodeFilters.tagWithAttribute("section", "id", "backmatter"),
      //HtmlNodeFilters.tagWithAttribute("section", "id", "supplementary-materials"),
      HtmlNodeFilters.tagWithAttribute("section", "id", "bibliography"),
      HtmlNodeFilters.tagWithAttribute("section", "id", "elettersSection"),

        // popup
      HtmlNodeFilters.tagWithAttributeRegex("div", "class", "related-content"),

  };


  public InputStream createFilteredInputStream(ArchivalUnit au,
                                               InputStream in,
                                               String encoding)
      throws PluginException {
    return super.createFilteredInputStream(au, in, encoding, filters);
  }

}