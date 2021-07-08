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

package org.lockss.plugin.atypon.besbjs;

import org.htmlparser.NodeFilter;
import org.lockss.filter.html.HtmlNodeFilters;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.plugin.atypon.BaseAtyponHtmlHashFilterFactory;

import java.io.InputStream;

public class BESBJHtmlHashFilterFactory extends BaseAtyponHtmlHashFilterFactory {

  @Override
  public InputStream createFilteredInputStream(ArchivalUnit au,
      InputStream in, String encoding) {
    
    NodeFilter[] filters = new NodeFilter[] {
        // header filtered in BaseAtypon

        // top right of issue toc - links to previous or next issue
        HtmlNodeFilters.tagWithAttribute("div", "id", "prevNextNav"),

        // top right of an article - links to Previous or Next Article,
        // and Cited By
        HtmlNodeFilters.tagWithAttributeRegex(
            "div", "class", "type-publication-tools"),

        // below type-publication-tools of an article, where ads are inserted    
        HtmlNodeFilters.tagWithAttributeRegex(
            "div", "class", "type-ad-placeholder"),

        // from toc page - Cited count
        HtmlNodeFilters.tagWithAttribute(
            "div", "class", "citation tocCitation"),

        // left column - all except Download Citations
        HtmlNodeFilters.allExceptSubtree(
            HtmlNodeFilters.tagWithAttributeRegex(
                "div", "class", "leftColumn"),
            HtmlNodeFilters.tagWithAttributeRegex(
                "a", "href", "/action/showCitFormats\\?")),

        // right column ads - <div class="mainAd">
        HtmlNodeFilters.tagWithAttributeRegex("div", "class", "mainAd"),

        HtmlNodeFilters.tagWithAttributeRegex("div", "id", "firstPage"),

        HtmlNodeFilters.tagWithAttributeRegex("section", "class", "article__metrics"),
        HtmlNodeFilters.tagWithAttributeRegex("section", "class", "article__history"),
        HtmlNodeFilters.tagWithAttributeRegex("div", "class", "toc-item__footer"),
        HtmlNodeFilters.tagWithAttributeRegex("a", "class", "twitter-timeline"),
        HtmlNodeFilters.tagWithAttributeRegex("li", "class", "tab__nav__item"),
        HtmlNodeFilters.tagWithAttributeRegex("div", "id", "figure-viewer"),

        // footer filtered in BaseAtypon
    };

    return super.createFilteredInputStream(au, in, encoding, filters);
  }

  @Override
  public boolean doWSFiltering() {
    return true;
  }

}
