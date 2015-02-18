/*
 * $Id$
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

package org.lockss.plugin.universityofcaliforniapress;

import java.io.*;

import org.htmlparser.NodeFilter;
import org.htmlparser.filters.*;
import org.lockss.daemon.PluginException;
import org.lockss.filter.*;
import org.lockss.filter.StringFilter;
import org.lockss.filter.HtmlTagFilter.TagPair;
import org.lockss.filter.html.*;
import org.lockss.plugin.*;
import org.lockss.util.*;

public class UniversityOfCaliforniaPressHtmlFilterFactory implements FilterFactory {

  public InputStream createFilteredInputStream(ArchivalUnit au,
                                               InputStream in,
                                               String encoding)
      throws PluginException {
    NodeFilter[] filters = new NodeFilter[] {
        // Variable scripting contents
        new TagNameFilter("script"),
        // Too many variants
        new TagNameFilter("head"),
        // Switched from <input>...</input> to <input />
        // Hard to express as a combination of HTML filter and string filters
        new TagNameFilter("form"),
        // Institution-dependent
        HtmlNodeFilters.tagWithAttribute("table", "class", "identitiesBar"),
        // Advertising
        HtmlNodeFilters.tagWithAttribute("div", "class", "footerAd"),
        // Next two for versioned RSS feed links
        HtmlNodeFilters.tagWithAttribute("link", "type", "application/rss+xml"),
        HtmlNodeFilters.tagWithAttributeRegex("a", "href", "/action/showFeed\\?(.*&)?mi="),
        // Variable file size
        HtmlNodeFilters.tagWithAttribute("span", "class", "fileSize"),
        // The URL structure of the <img> within changed
        HtmlNodeFilters.tagWithAttribute("div", "id", "firstPage"),
        // Significantly different variant
        HtmlNodeFilters.tagWithAttributeRegex("table", "class", "footer"),
        // Javascript specifics changed
        HtmlNodeFilters.tagWithAttributeRegex("a", "href", "^javascript:RightslinkPopUp"),
        // Some article views were added or removed over time
        HtmlNodeFilters.tagWithAttribute("div", "class", "article_link"),
        // Reverse citations feature added later
        HtmlNodeFilters.tagWithAttribute("div", "class", "citedBySection"),
        // Link to the latest issues
        HtmlNodeFilters.tagWithAttributeRegex("a", "href", "^/toc/"),
    };

    InputStream filtered = new HtmlFilterInputStream(in,
                                                     encoding,
                                                     HtmlNodeFilterTransform.exclude(new OrFilter(filters)));
    
    Reader reader = FilterUtil.getReader(filtered, encoding);
    Reader tagFilter = HtmlTagFilter.makeNestedFilter(reader,
                                                      ListUtil.list(
        // Comments that appeared or disappeared over time                                                                    
        new TagPair("<!-- placeholder", "-->"),
        // Contains a hard to characterize block of metadata (e.g. DOI)
        // whose formatting changed
        new TagPair("<!-- Start title of page and review -->",
                    "<!-- End title of page and review -->")
    ));
    // Use of "&nbsp;" or " " inconsistent over time
    Reader stringFilter = new StringFilter(tagFilter, "&nbsp;", " ");
    return new ReaderInputStream(new WhiteSpaceFilter(stringFilter));
  }

}
