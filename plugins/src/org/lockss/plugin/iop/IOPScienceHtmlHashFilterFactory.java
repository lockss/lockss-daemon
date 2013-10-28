/*
 * $Id: IOPScienceHtmlHashFilterFactory.java,v 1.7 2013-10-28 21:17:41 etenbrink Exp $
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

package org.lockss.plugin.iop;

import java.io.InputStream;
import java.io.Reader;

import org.htmlparser.NodeFilter;
import org.htmlparser.filters.OrFilter;
import org.htmlparser.filters.TagNameFilter;
import org.lockss.daemon.PluginException;
import org.lockss.filter.*;
import org.lockss.filter.HtmlTagFilter.TagPair;
import org.lockss.filter.html.*;
import org.lockss.plugin.*;
import org.lockss.util.ListUtil;
import org.lockss.util.ReaderInputStream;


public class IOPScienceHtmlHashFilterFactory implements FilterFactory {

  @Override
  public InputStream createFilteredInputStream(ArchivalUnit au,
                                               InputStream in,
                                               String encoding)
      throws PluginException {
    NodeFilter[] filters = new NodeFilter[] {
        // Contains the search box, which changes over time
        HtmlNodeFilters.tagWithAttribute("div", "id", "header-content"),
        // Contains variable links to other content ("users also read", "related review articles", etc.)
        HtmlNodeFilters.tagWithAttributeRegex("div", "class", "alsoRead"),
        // The right column is an accordion that contains toc anchor links, related articles, related review articles, etc
        HtmlNodeFilters.tagWithAttribute("div",  "id", "rightCol"),
        // may not be an issue, but concerned that mathjax exposure will change
        HtmlNodeFilters.tagWithAttribute("div",  "class", "mathJaxControls"),
        // Last 10 articles viewed; not the best characterization but unique enough
        HtmlNodeFilters.tagWithAttribute("div", "class", "tabs javascripted"),
        // Contains the institution name and/or banner
        HtmlNodeFilters.tagWithAttribute("div", "id", "banner"),
        // Contains this year in the copyright notice
        HtmlNodeFilters.tagWithAttribute("div", "id", "footer"),
        // Contains style sheet
        HtmlNodeFilters.tagWithAttribute("link", "type", "text/css"),
        // Contains script
        new TagNameFilter("script"),
        // Contains a jsessionid
        HtmlNodeFilters.tagWithAttributeRegex("form", "action", "jsessionid"),
        // Contains variable ads, promos, etc.
        HtmlNodeFilters.tagWithAttribute("div", "id", "tacticalBanners"),
    };
    
    InputStream filtered = new HtmlFilterInputStream(in,
                                                     encoding,
                                                     HtmlNodeFilterTransform.exclude(new OrFilter(filters)));
    
    Reader filteredReader = FilterUtil.getReader(filtered, encoding);
    Reader tagFilter = HtmlTagFilter.makeNestedFilter(filteredReader,
        ListUtil.list(
            new TagPair("<header>", "</header>"),
            new TagPair("<footer>", "</footer>")
            ));
    return new ReaderInputStream(new WhiteSpaceFilter(tagFilter));
  }

}
