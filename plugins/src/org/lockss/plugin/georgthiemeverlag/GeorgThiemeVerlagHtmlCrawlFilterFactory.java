/*
 * $Id$
 */

/*

Copyright (c) 2000-2014 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.plugin.georgthiemeverlag;

import java.io.InputStream;

import org.htmlparser.NodeFilter;
import org.htmlparser.filters.*;
import org.lockss.daemon.PluginException;
import org.lockss.filter.html.*;
import org.lockss.plugin.*;

public class GeorgThiemeVerlagHtmlCrawlFilterFactory implements FilterFactory {
  
  @Override
  public InputStream createFilteredInputStream(ArchivalUnit au,
                                               InputStream in,
                                               String encoding)
      throws PluginException {
    NodeFilter[] filters = new NodeFilter[] {
        // Aggressive filtering of non-content tags
        // Do not crawl header or footer for links
        new TagNameFilter("header"),
        new TagNameFilter("footer"),
        // Contains navigation items that can link off AU
        HtmlNodeFilters.tagWithAttribute("div", "id", "navPanel"),
        HtmlNodeFilters.tagWithAttribute("ul", "id", "overviewNavigation"),
        // Contains links to non-relevant pages 
        HtmlNodeFilters.tagWithAttribute("div", "class", "pageFunctions"),
        // Can contain links to original articles from Errata as well as other links
        HtmlNodeFilters.tagWithAttribute("div", "class", "relatedArticles"),
        HtmlNodeFilters.tagWithAttributeRegex("div", "class", "articleToggleMenu"),
        // No need to crawl ad links
        HtmlNodeFilters.tagWithAttributeRegex("div", "id", "adSidebar"),
        // No need to crawl reference list 
        HtmlNodeFilters.tagWithAttribute("ul", "class", "literaturliste"),
        // Appears that correction anchors have class="anchorc" XXX
        HtmlNodeFilters.tagWithAttribute("a", "class", "anchorc"),
    };
    InputStream filtered = new HtmlFilterInputStream(in, encoding,
        HtmlNodeFilterTransform.exclude(new OrFilter(filters)))
    .registerTag(new HtmlTags.Header())
    .registerTag(new HtmlTags.Footer()); // XXX registerTag can be removed after 1.65
    return filtered;
  }
  
}
