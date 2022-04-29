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

package org.lockss.plugin.atypon.faseb;

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
public class FasebHtmlCrawlFilterFactory extends BaseAtyponHtmlCrawlFilterFactory {
  
  NodeFilter[] filters = new NodeFilter[] {
      
      // NOTE: overcrawling is an occasional issue with in-line references to "original article"
      
      HtmlNodeFilters.tag("header"),
      HtmlNodeFilters.tag("footer"),
      
      HtmlNodeFilters.tagWithAttribute("li", "id", "pane-pcw-references"),
      HtmlNodeFilters.tagWithAttribute("li", "id", "pane-pcw-related"),
      HtmlNodeFilters.tagWithAttribute("section", "class", "article__metrics"),
      HtmlNodeFilters.tagWithAttribute("section", "class", "article__keyword"),
      HtmlNodeFilters.tagWithAttribute("div", "class", "article__references"),
      
      HtmlNodeFilters.tagWithAttributeRegex("ul", "class", "corrections"),
      HtmlNodeFilters.tagWithAttributeRegex("div", "class", "quickSearch"),
      HtmlNodeFilters.tagWithAttributeRegex("div", "class", "search__filters"),
      HtmlNodeFilters.tagWithAttributeRegex("div", "class", "search-result__"),
      HtmlNodeFilters.allExceptSubtree(
          HtmlNodeFilters.tag("nav"),
          HtmlNodeFilters.tagWithAttributeRegex("a", "href", "^/action/showCitFormats.doi=")),
      
      // never want these links, excluded lists was too long
      // HtmlNodeFilters.tagWithAttributeRegex("a", "href", "/servlet/linkout[?](suffix)="),
      HtmlNodeFilters.tagWithAttributeRegex("a", "href", "^/servlet/linkout[?]"),
      HtmlNodeFilters.tagWithAttributeRegex("a", "href", "^/author/"),
  };
  
  public InputStream createFilteredInputStream(ArchivalUnit au,
      InputStream in, String encoding) throws PluginException{ 
    return super.createFilteredInputStream(au, in, encoding, filters);
  }
  
}
