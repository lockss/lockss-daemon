/*
 * $Id$
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

package org.lockss.plugin.atypon.rsna;

import java.io.InputStream;

import org.htmlparser.NodeFilter;
import org.lockss.daemon.PluginException;
import org.lockss.filter.html.HtmlNodeFilters;
import org.lockss.plugin.*;
import org.lockss.plugin.atypon.BaseAtyponHtmlCrawlFilterFactory;

public class RsnaHtmlCrawlFilterFactory extends BaseAtyponHtmlCrawlFilterFactory {

  NodeFilter[] filters = new NodeFilter[] {
      // links from full text to related or previous articles
      // see: http://pubs.rsna.org/doi/full/10.1148/radiol.2016164024
      HtmlNodeFilters.tagWithAttribute("a", "class", "ext-link"),
      HtmlNodeFilters.tagWithAttributeRegex("div", "class", "header"),
      HtmlNodeFilters.tag("header"),
      HtmlNodeFilters.tag("footer"),
      // in case there are links in the preview text
      HtmlNodeFilters.tagWithAttributeRegex("div", "class", "toc-item__abstract"),
      HtmlNodeFilters.tagWithAttributeRegex("span", "class", "article__breadcrumbs"),
      // Article landing - ajax tabs
      HtmlNodeFilters.tagWithAttribute("li", "id", "pane-pcw-references"),
      HtmlNodeFilters.tagWithAttribute("li", "id", "pane-pcw-related"),
      HtmlNodeFilters.tagWithAttributeRegex("div", "class", "article__references"),
      HtmlNodeFilters.tagWithAttributeRegex("div", "id", "related"),
      HtmlNodeFilters.tagWithAttributeRegex("div", "class", "related"),
      HtmlNodeFilters.tagWithAttribute("section", "class", "article__metrics"),
      
      // never want these links, excluded lists was too long
      HtmlNodeFilters.tagWithAttributeRegex("a", "href", "^/author/"),
      HtmlNodeFilters.tagWithAttributeRegex("a", "href", "^/servlet/linkout[?]"),
      // did nor see any of these
      // HtmlNodeFilters.tagWithAttributeRegex("li", "class", "(correction|latest-version)"),
  };
  
  public InputStream createFilteredInputStream(ArchivalUnit au,
                                               InputStream in,
                                               String encoding)
      throws PluginException {
    return super.createFilteredInputStream(au, in, encoding, filters);
  }

}
