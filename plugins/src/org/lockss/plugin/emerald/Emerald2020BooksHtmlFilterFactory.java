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

package org.lockss.plugin.emerald;

import org.htmlparser.NodeFilter;
import org.htmlparser.filters.OrFilter;
import org.htmlparser.filters.TagNameFilter;
import org.lockss.filter.FilterUtil;
import org.lockss.filter.html.HtmlFilterInputStream;
import org.lockss.filter.html.HtmlNodeFilterTransform;
import org.lockss.filter.html.HtmlNodeFilters;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.plugin.FilterFactory;
import org.lockss.util.ReaderInputStream;

import java.io.InputStream;
import java.io.Reader;

public class Emerald2020BooksHtmlFilterFactory implements FilterFactory {

  public InputStream createFilteredInputStream(ArchivalUnit au,
                                               InputStream in,
                                               String encoding) {
    NodeFilter[] filters = new NodeFilter[] {
            new TagNameFilter("noscript"),
            new TagNameFilter("script"),
            new TagNameFilter("style"),
            new TagNameFilter("head"),
            new TagNameFilter("style"),
            new TagNameFilter("footer"),

            // https://www.emerald.com/insight/publication/issn/1012-8255/vol/33/iss/1
            HtmlNodeFilters.tagWithAttributeRegex("div", "class", "intent_further_information"),
            
            // https://www.emerald.com/insight/content/doi/10.1108/ARLA-01-2018-0028/full/html
            HtmlNodeFilters.tagWithAttributeRegex("div", "id", "trendmd-widget"),
            HtmlNodeFilters.tagWithAttributeRegex("section", "id", "abstract"),
            HtmlNodeFilters.tagWithAttributeRegex("section", "id", "keywords_list"),
            HtmlNodeFilters.tagWithAttributeRegex("section", "class", "Citation"),

            // https://www.emerald.com/insight/content/doi/10.1108/978-1-78714-501-620171005/full/html
            HtmlNodeFilters.tagWithAttributeRegex("div", "id", "cookies-consent"),
            HtmlNodeFilters.tagWithAttributeRegex("div", "id", "collapse-book-chapters"),
            HtmlNodeFilters.tagWithAttributeRegex("div", "id", "bookChapters"),
            HtmlNodeFilters.tagWithAttributeRegex("div", "id", "tocscroll"),
            HtmlNodeFilters.tagWithAttributeRegex("section", "class", "References"),
            HtmlNodeFilters.tagWithAttributeRegex("div", "id", "feedback-strip"),
            HtmlNodeFilters.tagWithAttributeRegex("div", "id", "feedback-underlay"),
            HtmlNodeFilters.tagWithAttributeRegex("button", "class", "intent_back_to_top"),

            HtmlNodeFilters.tagWithAttributeRegex("h2", "id", "page__publisher-label"),
            HtmlNodeFilters.tagWithAttributeRegex("p", "class", "publisher"),
            HtmlNodeFilters.tagWithAttributeRegex("p", "class", "Citation__identifier"),
            HtmlNodeFilters.tagWithAttributeRegex("span", "class", "intent_copyright_text")

    };
    InputStream filteredStream = new HtmlFilterInputStream(in, encoding,
            HtmlNodeFilterTransform.exclude(new OrFilter(filters)));
    Reader httpFilter = FilterUtil.getReader(filteredStream, encoding);
    return new ReaderInputStream(httpFilter);
  }

}
