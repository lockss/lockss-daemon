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
import org.lockss.filter.html.HtmlNodeFilters;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.plugin.atypon.BaseAtyponHtmlHashFilterFactory;

import java.io.InputStream;

public class AaasHtmlHashFilterFactory  extends BaseAtyponHtmlHashFilterFactory {

  @Override
  public InputStream createFilteredInputStream(ArchivalUnit au,
                                               InputStream in,
                                               String encoding) {
    NodeFilter[] filters = new NodeFilter[] {
        // menu which prints logged in status and institution
        HtmlNodeFilters.tagWithAttributeRegex("ul", "class", "main-menu"),
        // other types of headers
        HtmlNodeFilters.tagWithAttribute("div", "class", "header-sidebar"),
        HtmlNodeFilters.tagWithAttributeRegex("div", "class", "main-header__secondary"),
        // ad
        HtmlNodeFilters.tagWithAttributeRegex("div", "class", "adplaceholder"),
        // podcast bit
        HtmlNodeFilters.tagWithAttributeRegex("h4", "class", "main-title-2--decorated"),
        HtmlNodeFilters.tagWithAttributeRegex("div", "class", "podcast-item"),
        // recent issues slideshow
        HtmlNodeFilters.tagWithAttributeRegex("h4", "class", "title--decorated"),
        HtmlNodeFilters.tagWithAttribute("div", "class", "slideshow"),

        // ever changing new and recommended articles
        HtmlNodeFilters.tagWithAttributeRegex("article", "class", "news-article-aside"),
        HtmlNodeFilters.tagWithAttributeRegex("div", "class", "show-recommended"),

        // copyright info, metrics, etc, which are subject to change are all in a big core-collatoral div
        HtmlNodeFilters.tagWithAttributeRegex("div", "class", "core-collateral"),

    };
    // super.createFilteredInputStream adds aaas filter to the baseAtyponFilters
    // and returns the filtered input stream using an array of NodeFilters that
    // combine the two arrays of NodeFilters and then applies a white space filter
    return super.createFilteredInputStream(au, in, encoding, filters);
  }

  @Override
  public boolean doWSFiltering() {
    return true;
  }

  @Override
  public boolean doTagRemovalFiltering() {
    return true;
  }

}