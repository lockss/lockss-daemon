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
package org.lockss.plugin.taar;

import org.htmlparser.NodeFilter;
import org.htmlparser.filters.OrFilter;
import org.lockss.filter.FilterUtil;
import org.lockss.filter.WhiteSpaceFilter;
import org.lockss.filter.html.HtmlFilterInputStream;
import org.lockss.filter.html.HtmlNodeFilterTransform;
import org.lockss.filter.html.HtmlNodeFilters;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.plugin.FilterFactory;
import org.lockss.util.ReaderInputStream;

import java.io.InputStream;
import java.io.Reader;

public class TaarHtmlHashFilterFactory implements FilterFactory {

  static NodeFilter[] excludeFilters = new NodeFilter[] {
      HtmlNodeFilters.tag("script"),
      HtmlNodeFilters.tag("noscript"),
      HtmlNodeFilters.tag("style"),
      HtmlNodeFilters.tag("head"),
      HtmlNodeFilters.tag("meta"),
      HtmlNodeFilters.tag("header"),
      HtmlNodeFilters.tag("footer"),
      HtmlNodeFilters.tagWithAttributeRegex("fieldset", "class", "btn-toolbar"),
      HtmlNodeFilters.tagWithAttribute("a", "title", "Exit"),
      HtmlNodeFilters.tagWithAttribute("form", "class", "form-inline"),
      HtmlNodeFilters.tagWithAttributeRegex("div", "class", "t3-sidebar"),
      HtmlNodeFilters.tagWithAttributeRegex("div", "class", "t3-module"),
      HtmlNodeFilters.tagWithAttributeRegex("div", "class", "tags"),
      HtmlNodeFilters.tagWithAttributeRegex("div", "id", "ref-ol"),
      HtmlNodeFilters.tagWithAttributeRegex("div", "id", "lb(O|B|C)"),
      HtmlNodeFilters.tagWithAttributeRegex("div", "id", "back-to-top"),
      HtmlNodeFilters.tagWithAttributeRegex("div", "id", "cookiehint"),
      HtmlNodeFilters.tagWithAttributeRegex("div", "id", "customerly-container"),
      HtmlNodeFilters.tagWithAttributeRegex("link", "id", "customerly_style"),
      HtmlNodeFilters.tagWithAttributeRegex("ul", "class", "pager|pagenav"),
      HtmlNodeFilters.tagWithAttributeRegex("button", "id", "cite"),
      HtmlNodeFilters.tagWithAttributeRegex("li", "class", "(email|print)-icon"),
      HtmlNodeFilters.tagWithAttributeRegex("aside", "class", "article-aside"),
      HtmlNodeFilters.tagWithAttributeRegex("a", "href", "(component/mailto|cdn-cgi/l/email-protection)"),
      HtmlNodeFilters.tagWithAttributeRegex("a", "rel", "lightbox-text_"),
      // <span id="cloak958367ebb175a015ba3f846f81eddef9">This email address is being protected from spambots. You need JavaScript enabled to view it.</span>
      HtmlNodeFilters.tagWithAttributeRegex("span", "id", "^cloak[0-Z]*"),
  };


  public InputStream createFilteredInputStream(ArchivalUnit au,
                                               InputStream in,
                                               String encoding) {
    // apply the above filters
    InputStream filtered = new HtmlFilterInputStream(in,
        encoding,
        HtmlNodeFilterTransform.exclude(new OrFilter(
            excludeFilters
        )));
    // add whitespace filter
    Reader filteredReader = FilterUtil.getReader(filtered, encoding);
    return new ReaderInputStream(new WhiteSpaceFilter(filteredReader));
  }

}
