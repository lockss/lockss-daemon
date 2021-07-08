/*
 * $Id: OaiPmhHtmlFilterFactory.java,v 1.1.2.1 2014/05/05 17:32:30 wkwilson Exp $
 */

/*

Copyright (c) 2000-2012 Board of Trustees of Leland Stanford Jr. University,
all rights reserved.

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in
all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.  IN NO EVENT SHALL
STANFORD UNIVERSITY BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR
IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.

Except as contained in this notice, the name of Stanford University shall not
be used in advertising or otherwise to promote the sale, use or other dealings
in this Software without prior written authorization from Stanford University.

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
