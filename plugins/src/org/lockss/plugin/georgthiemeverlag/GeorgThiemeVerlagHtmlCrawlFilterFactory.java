/*
 * $Id: GeorgThiemeVerlagHtmlCrawlFilterFactory.java,v 1.2 2014-03-26 17:13:17 etenbrink Exp $
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
        // Do not crawl header or footer for links
        new TagNameFilter("header"),
        new TagNameFilter("footer"),
        // div id="navPanel"
        HtmlNodeFilters.tagWithAttribute("div", "id", "navPanel"),
        // ul id="overviewNavigation" from issue toc
        HtmlNodeFilters.tagWithAttribute("ul", "id", "overviewNavigation"),
        // div class="pageFunctions"
        HtmlNodeFilters.tagWithAttribute("div", "class", "pageFunctions"),
        // div class="relatedArticles"
        HtmlNodeFilters.tagWithAttribute("div", "class", "relatedArticles"),
        // div class="toggleMenu articleToggleMenu"  from article page
        HtmlNodeFilters.tagWithAttributeRegex("div", "class", "articleToggleMenu"),
        // div id="adSidebarBottom"  div id="adSidebar"
        HtmlNodeFilters.tagWithAttributeRegex("div", "id", "adSidebar"),
        // ul class="literaturliste"
        HtmlNodeFilters.tagWithAttribute("ul", "class", "literaturliste"),
        // a class="anchorc" correction anchor?
        HtmlNodeFilters.tagWithAttribute("a", "class", "anchorc"),
    };
    InputStream filtered = new HtmlFilterInputStream(in, encoding,
        HtmlNodeFilterTransform.exclude(new OrFilter(filters)))
    .registerTag(new HtmlTags.Header())
    .registerTag(new HtmlTags.Footer()); // XXX registerTag can be removed after 1.65
    return filtered;
  }
  
}
