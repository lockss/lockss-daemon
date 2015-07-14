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

import java.io.*;

import org.htmlparser.*;
import org.htmlparser.filters.OrFilter;
import org.htmlparser.tags.DoctypeTag;
import org.lockss.daemon.PluginException;
import org.lockss.filter.*;
import org.lockss.filter.html.*;
import org.lockss.plugin.*;
import org.lockss.util.ReaderInputStream;

public class DigitalCommonsRepositoryHtmlHashFilterFactory implements FilterFactory {

  @Override
  public InputStream createFilteredInputStream(ArchivalUnit au,
                                               InputStream in,
                                               String encoding)
      throws PluginException {
    NodeFilter[] filters = new NodeFilter[] {
        /*
         * Broad area filtering 
         */
        // Scripts, styles, comments
        HtmlNodeFilters.tag("script"),
        HtmlNodeFilters.tag("style"),
        HtmlNodeFilters.comment(),
        // Document header
        HtmlNodeFilters.tagWithAttribute("link", "rel", "stylesheet"),
        // Header
        HtmlNodeFilters.tagWithAttribute("div", "id", "header"),
        HtmlNodeFilters.tagWithAttribute("div", "id", "navigation"),
        // Sidebar
        HtmlNodeFilters.tagWithAttribute("div", "id", "sidebar"),
        HtmlNodeFilters.tagWithAttribute("select", "name", "url"), // Obsolete: inside 'sidebar'
        // Footer
        HtmlNodeFilters.tagWithAttribute("div", "id", "footer"),
        /*
         * Main content area
         */
        // Top-right previous/next links
        HtmlNodeFilters.tagWithAttribute("ul", "id", "pager"),
        // Breadcrumbs and accompanying backlinks/decorations
        HtmlNodeFilters.tagWithAttribute("div", "class", "crumbs"), // e.g. http://lawdigitalcommons.bc.edu/ealr/vol2/iss3/4/
        HtmlNodeFilters.tagWithAttribute("div", "id", "breadcrumb"),
        HtmlNodeFilters.tagWithAttribute("div", "id", "series-header"),
        HtmlNodeFilters.tagWithAttribute("div", "id", "series-title"),
        HtmlNodeFilters.tagWithAttribute("h2", "id", "series-title"),
        // Sidebar inside main content area
        HtmlNodeFilters.tagWithAttribute("div", "id", "beta_7-3"),
        HtmlNodeFilters.tagWithAttribute("div", "id", "beta-disciplines"), // Obsolete? inside 'beta_7-3'
        HtmlNodeFilters.tagWithAttribute("div", "id", "share"), // Obsolete? inside 'beta_7-3'
        HtmlNodeFilters.tagWithAttribute("span", "class", "Z3988"), // Obsolete? inside 'beta_7-3'
        // Inline skip links
        HtmlNodeFilters.tagWithAttribute("a", "class", "skiplink"),
        /*
         * Other 
         */
        // Collections of type ir_book and ir_gallery have covers of the books
        // in other years (other than those in e.g. <li class="lockss_2013">)
        HtmlNodeFilters.tagWithAttribute("div", "class", "gallery-tools"),
        // Books can have a purchase button
        // e.g. http://docs.lib.purdue.edu/purduepress_ebooks/29/
        HtmlNodeFilters.tagWithAttribute("div", "id", "buy-link"),
        // 'follow' publication or 'follow' author buttons
        HtmlNodeFilters.tagWithAttribute("p", "class", "publication-follow"),
        HtmlNodeFilters.tagWithAttribute("a", "rel", "nofollow"),
        // <meta name="viewport"> vs. <meta  name="viewport">
        HtmlNodeFilters.tagWithAttribute("meta", "name", "viewport"),
        // <meta name="viewport"> vs. <meta  name="viewport">
        HtmlNodeFilters.tagWithAttribute("meta", "name", "viewport"),
        // <meta name="bepress_is_article_cover_page"> vs. not
        HtmlNodeFilters.tagWithAttribute("meta", "name", "bepress_is_article_cover_page"),
        // <!DOCTYPE html> vs. <!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
        new NodeFilter() {
          @Override public boolean accept(Node node) {
            return node instanceof DoctypeTag;
          }
        },
        /*
         * Inherited from old BePressHtmlFilterFactory
         */
        // News item in top-right corner of some journals
        HtmlNodeFilters.tagWithAttribute("div", "id", "news"),
        // Both contain download numbers and date
        HtmlNodeFilters.tagWithAttribute("div", "id", "custom-fields"), // ?
        HtmlNodeFilters.tagWithAttribute("div", "id", "recommended_citation"), // e.g. http://docs.lib.purdue.edu/clcweb/vol15/iss7/21/ 
        // Misleadingly-named Altmetric widget e.g. http://docs.lib.purdue.edu/clcweb/vol15/iss7/21/
        HtmlNodeFilters.tagWithAttribute("div", "id", "doi"), // Obsolete; inside 'sidebar'
    };

    InputStream filtered = new HtmlFilterInputStream(in,
                                                     encoding, 
                                                     HtmlNodeFilterTransform.exclude(new OrFilter(filters)));
    Reader filteredReader = FilterUtil.getReader(filtered, encoding);
    return new ReaderInputStream(new WhiteSpaceFilter(filteredReader));
  }
  
}
