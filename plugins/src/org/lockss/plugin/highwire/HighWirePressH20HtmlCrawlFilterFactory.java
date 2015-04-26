/*
 * $Id: HighWireDrupalHtmlCrawlFilterFactory.java 39864 2015-02-18 09:10:24Z thib_gc $
 */

/*

Copyright (c) 2000-2015 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.plugin.highwire;

import java.io.InputStream;

import org.htmlparser.NodeFilter;
import org.htmlparser.filters.*;
import org.lockss.daemon.PluginException;
import org.lockss.filter.html.*;
import org.lockss.plugin.*;

public class HighWirePressH20HtmlCrawlFilterFactory implements FilterFactory {
  
  protected static NodeFilter[] baseHWFilters = new NodeFilter[] {
    // Do not crawl next/prev links
    HtmlNodeFilters.tagWithAttribute("link", "rel", "prev"),
    HtmlNodeFilters.tagWithAttribute("link", "rel", "next"),
    // Do not crawl header or footer for links
    new TagNameFilter("header"),
    new TagNameFilter("footer"),
    // Do not crawl for links from leaderboard-ads
    HtmlNodeFilters.tagWithAttributeRegex("div", "class", "leaderboard-ads"),
    // Do not crawl reference section, sidebar-nav for links
    HtmlNodeFilters.tagWithAttribute("div", "class", "section ref-list"),
    HtmlNodeFilters.tagWithAttributeRegex("div", "class", "sidebar-(nav|qs)"),
    // Do not crawl for links in col3
    HtmlNodeFilters.tagWithAttribute("div", "id", "col-3"),
  };
  
  @Override
  public InputStream createFilteredInputStream(ArchivalUnit au,
                                               InputStream in,
                                               String encoding)
      throws PluginException {
    
    return new HtmlFilterInputStream(in, encoding,
        HtmlNodeFilterTransform.exclude(new OrFilter(baseHWFilters)));
  }
}
