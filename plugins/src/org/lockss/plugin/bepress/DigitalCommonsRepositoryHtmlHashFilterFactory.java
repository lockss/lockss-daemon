/*
 * $Id$
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

package org.lockss.plugin.bepress;

import java.io.InputStream;

import org.htmlparser.NodeFilter;
import org.htmlparser.filters.OrFilter;
import org.lockss.daemon.PluginException;
import org.lockss.filter.html.*;
import org.lockss.plugin.*;
import org.lockss.util.Logger;

public class DigitalCommonsRepositoryHtmlHashFilterFactory implements FilterFactory {

  private static final Logger log =
      Logger.getLogger(DigitalCommonsRepositoryHtmlHashFilterFactory.class);
  
  @Override
  public InputStream createFilteredInputStream(ArchivalUnit au,
                                               InputStream in,
                                               String encoding)
      throws PluginException {
    NodeFilter[] filters = new NodeFilter[] {
        // filter out javascript
        HtmlNodeFilters.tag("script"),
        // filter out comments
        HtmlNodeFilters.comment(),
        // stylesheets
        HtmlNodeFilters.tagWithAttribute("link", "rel", "stylesheet"),
        // top banner
        HtmlNodeFilters.tagWithAttribute("div", "id", "header"),
        // breadcrumb and accompanying backlinks/decorations
        HtmlNodeFilters.tagWithAttribute("div", "id", "breadcrumb"),
        HtmlNodeFilters.tagWithAttribute("div", "id", "series-header"),
        HtmlNodeFilters.tagWithAttribute("div", "id", "series-title"),
        HtmlNodeFilters.tagWithAttribute("h2", "id", "series-title"),
        // skip to main
        HtmlNodeFilters.tagWithAttribute("a", "class", "skiplink"),
        // near top - navigation
        HtmlNodeFilters.tagWithAttribute("div", "id", "navigation"),
        // left sidebar
        HtmlNodeFilters.tagWithAttribute("div", "id", "sidebar"),
        // top right of the article for the year - <previous> and <next>
        // http://repository.cmu.edu/statistics/68/
        HtmlNodeFilters.tagWithAttribute("ul", "id", "pager"),
        // collections of type ir_book and ir_gallery have covers of the books
        // in other years (other than those in e.g. <li class="lockss_2013">)
        HtmlNodeFilters.tagWithAttribute("div", "class", "gallery-tools"),
        // books can have a purchase button
        // e.g. http://docs.lib.purdue.edu/purduepress_ebooks/29/
        HtmlNodeFilters.tagWithAttribute("div", "id", "buy-link"),
        // footer
        HtmlNodeFilters.tagWithAttribute("div", "id", "footer"),
        // 'follow' publication or 'follow' author buttons
        HtmlNodeFilters.tagWithAttribute("p", "class", "publication-follow"),
        HtmlNodeFilters.tagWithAttribute("a", "rel", "nofollow"),
        // right side box 'Included in'
        HtmlNodeFilters.tagWithAttribute("div", "id", "beta-disciplines"),
        // social media - share
        HtmlNodeFilters.tagWithAttribute("div", "id", "share"),
        // hidden Z39.88 field
        HtmlNodeFilters.tagWithAttribute("span", "class", "Z3988")
    };
    return new HtmlFilterInputStream(in,
                                     encoding,
                                     HtmlNodeFilterTransform.exclude(new OrFilter(filters)));
  }
  
}
