/* $Id$
 */

/*

Copyright (c) 2018 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.plugin.atypon.aps;

import java.io.InputStream;

import org.htmlparser.NodeFilter;
import org.lockss.daemon.PluginException;
import org.lockss.filter.html.*;
import org.lockss.plugin.*;
import org.lockss.plugin.atypon.BaseAtyponHtmlCrawlFilterFactory;

/*
 *
 * created because article links are not grouped under a journalid or volumeid,
 * but under article ids - will pull the links from the page, so filtering out
 * extraneous links
 * 
 */
public class AmPhysSocHtmlCrawlFilterFactory extends BaseAtyponHtmlCrawlFilterFactory {
  
  NodeFilter[] filters = new NodeFilter[] {
      
      // NOTE: overcrawling is an occasional issue with in-line references to "original article"
      
      HtmlNodeFilters.tag("header"),
      HtmlNodeFilters.tag("footer"),
      HtmlNodeFilters.tagWithAttributeRegex("div", "class", "publication(_header|-menu)"),
      HtmlNodeFilters.tagWithAttributeRegex("div", "class", "content-navigation"),
      // in case there are links in the preview text
      HtmlNodeFilters.tagWithAttributeRegex("div", "class", "toc-item__abstract"),
      // Article landing - ajax tabs
      HtmlNodeFilters.tagWithAttribute("li", "id", "pane-pcw-references"),
      HtmlNodeFilters.tagWithAttribute("li", "id", "pane-pcw-related"),
      // References
      HtmlNodeFilters.tagWithAttributeRegex("li", "class", "references__item"),
      // XXX following may not be needed
      HtmlNodeFilters.tagWithAttribute("span", "class", "references__suffix"),
      HtmlNodeFilters.tagWithAttribute("a", "class", "sfxLink"),
      // never want these links, excluded lists was too long
      HtmlNodeFilters.tagWithAttributeRegex("a", "class", "rightslink"),
      HtmlNodeFilters.tagWithAttributeRegex("a", "href", "/servlet/linkout[?]type="),
      HtmlNodeFilters.tagWithAttributeRegex("a", "href", "^/author/"),
      HtmlNodeFilters.tagWithAttributeRegex("li", "class", "(correction|latest-version)"),
  };
  
  public InputStream createFilteredInputStream(ArchivalUnit au,
      InputStream in, String encoding) throws PluginException{ 
    return super.createFilteredInputStream(au, in, encoding, filters);
  }
  
}

