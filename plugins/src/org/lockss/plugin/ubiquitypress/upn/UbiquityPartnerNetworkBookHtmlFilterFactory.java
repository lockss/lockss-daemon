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

package org.lockss.plugin.ubiquitypress.upn;

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

public class UbiquityPartnerNetworkBookHtmlFilterFactory implements FilterFactory {

  public InputStream createFilteredInputStream(ArchivalUnit au,
                                               InputStream in,
                                               String encoding) {
    NodeFilter[] filters = new NodeFilter[] {
            new TagNameFilter("noscript"),
            new TagNameFilter("script"),
            new TagNameFilter("style"),
            new TagNameFilter("head"),
            new TagNameFilter("header"),
            new TagNameFilter("footer"),

            //https://utsepress.lib.uts.edu.au/site/books/10.5130/978-0-6481242-8-3
            HtmlNodeFilters.tagWithAttributeRegex("div", "class", "table-of-contents"),
            HtmlNodeFilters.tagWithAttributeRegex("div", "class", "disciplines"),
            HtmlNodeFilters.tagWithAttributeRegex("div", "class", "keywords"),
            HtmlNodeFilters.tagWithAttributeRegex("div", "class", "metrics-block"),
            HtmlNodeFilters.tagWithAttributeRegex("div", "class", "metrics"),
            HtmlNodeFilters.tagWithAttributeRegex("div", "id", "thread__wrapper"),
            HtmlNodeFilters.tagWithAttributeRegex("div", "class", "read-list"),
            HtmlNodeFilters.tagWithAttributeRegex("a", "class","series-button"),
            HtmlNodeFilters.tagWithAttributeRegex("section", "class", "brand-header"),
            HtmlNodeFilters.tagWithAttributeRegex("section", "class", "book-cover"),
            HtmlNodeFilters.tagWithAttributeRegex("section", "class", "how-tos"),
            HtmlNodeFilters.tagWithAttributeRegex("section", "class","comments"),
            

    };
    InputStream filteredStream = new HtmlFilterInputStream(in, encoding,
            HtmlNodeFilterTransform.exclude(new OrFilter(filters)));
    Reader httpFilter = FilterUtil.getReader(filteredStream, encoding);
    return new ReaderInputStream(httpFilter);
  }

}
