/*

Copyright (c) 2000-2022, Board of Trustees of Leland Stanford Jr. University
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

package org.lockss.plugin.atypon.nas;

import org.htmlparser.NodeFilter;
import org.htmlparser.tags.Html;
import org.lockss.daemon.PluginException;
import org.lockss.filter.html.HtmlNodeFilters;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.plugin.atypon.BaseAtyponHtmlCrawlFilterFactory;

import java.io.InputStream;

public class NasHtmlCrawlFilterFactory extends BaseAtyponHtmlCrawlFilterFactory {
  NodeFilter[] filters = new NodeFilter[] {
      // div class="article-further-reading...
      HtmlNodeFilters.tagWithAttributeRegex("div",  "class", "article-further-reading"),
      // div id="nav-d25abac7-d1cf-406b-b915-0c426445c9d1-most-read-pane
      // div id="nav-d25abac7-d1cf-406b-b915-0c426445c9d1-most-cited-pane
      HtmlNodeFilters.tagWithAttributeRegex("div",  "id", "most-(cited|read)-pane"),
      // on the toc pages, there are references to articles embedded below an article title/link
      // typically in the Corrections, or Letters sections.
      // these can be from previous volumes, need to ignore
      // div class="card__extra-info"
      //   <!-- with child
      //   a class="card__extra-info__link"
      HtmlNodeFilters.tagWithAttributeRegex("div",  "class", "extra-info"),
      HtmlNodeFilters.tagWithAttributeRegex("a",  "class", "extra-info"),
      // similar to above, but on the article pages
      HtmlNodeFilters.tagWithAttributeRegex("div",  "class", "core-relations"),
      HtmlNodeFilters.tagWithAttribute("div", "class", "relationsGroup"),
  };
  @Override
  public InputStream createFilteredInputStream(ArchivalUnit au,
                                               InputStream in, String encoding) throws PluginException {
    return super.createFilteredInputStream(au, in, encoding, filters);
  }
}
