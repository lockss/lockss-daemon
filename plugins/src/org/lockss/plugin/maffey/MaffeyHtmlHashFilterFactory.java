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

Except as contained in this notice, tMassachusettsMedicalSocietyHtmlFilterFactoryhe name of Stanford University shall not
be used in advertising or otherwise to promote the sale, use or other dealings
in this Software without prior written authorization from Stanford University.

*/

package org.lockss.plugin.maffey;

import java.io.InputStream;

import org.htmlparser.NodeFilter;
import org.htmlparser.filters.*;
import org.lockss.daemon.PluginException;
import org.lockss.filter.html.*;
import org.lockss.plugin.*;

public class MaffeyHtmlHashFilterFactory implements FilterFactory {

  @Override
  public InputStream createFilteredInputStream(ArchivalUnit au,
                                               InputStream in,
                                               String encoding)
      throws PluginException {
    // [LA] = Libertas Academica
    
    // First filter with HtmlParser
    NodeFilter[] filters = new NodeFilter[] {
        /*
         * Crawl filter
         */
        // Related articles
        HtmlNodeFilters.tagWithAttribute("div", "class", "alsoRead"),
        // "What your colleagues are saying about..." contains author thumbnails [LA]
        HtmlNodeFilters.tagWithAttribute("div", "id", "colleagues"),
        /*
         * Hash filter
         */
        /* Broad area filtering */
        // Scripts, comments
        HtmlNodeFilters.tag("script"),
        HtmlNodeFilters.comment(),
        // Header
        HtmlNodeFilters.tagWithAttribute("div", "id", "headerouter"), // [LA]
        HtmlNodeFilters.tagWithAttributeRegex("div", "class", "nav-bg"), // [LA]
        HtmlNodeFilters.tagWithAttribute("div", "class", "nav"), // redundant: within 'nav-bg' [LA]
        // Right column
        HtmlNodeFilters.tagWithAttribute("div", "class", "rightcolumn"), // [LA]
        // Footer
        HtmlNodeFilters.tagWithAttribute("div", "id", "followus"), // social media [LA]
        HtmlNodeFilters.tagWithAttribute("div", "id", "footer-banner"), // [LA]
        HtmlNodeFilters.tagWithAttribute("div", "id", "footer-links"), // [LA]
        HtmlNodeFilters.tagWithAttribute("div", "id", "footer-copyright"), // [LA]
        // Side tab
        HtmlNodeFilters.tagWithAttribute("div", "id", "sidetab"), // [LA]
        /* Main content area */
        // Number of journal views
        HtmlNodeFilters.tagWithAttribute("div", "class", "journal_heading_stats"), // [LA]
        // Journal menu
        HtmlNodeFilters.tagWithAttribute("div", "class", "journalmenu"), // [LA]
        // Article metrics
        HtmlNodeFilters.tagWithAttribute("div", "class", "yellowbgright1"), // ??
        HtmlNodeFilters.tagWithAttribute("div", "class", "article_meta_stats"), // [LA]
        HtmlNodeFilters.tagWithAttribute("p", "class", "article_views_p"), // [LA]
        // Social media
        HtmlNodeFilters.tagWithAttribute("div", "id", "sharing"), // [LA]
        /* Other */
        // dynamic css/js urls // ??
        HtmlNodeFilters.tag("link"),
        // Ads
        HtmlNodeFilters.tagWithAttribute("div", "id", "ad_holder"),
        // Discussion and comments
        HtmlNodeFilters.tagWithAttribute("div", "id", "commentsBoxes"),
        /* ?? */
        // ??
        HtmlNodeFilters.tagWithAttribute("div", "class", "a-100 assets-light journal-footer"), // redundant: within 'footer-banner' [LA]
        // Rotating user testimonials
        HtmlNodeFilters.tagWithAttribute("div", "class", "categoriescolumn4"), // ??
        // Chat with support availability status changes
        HtmlNodeFilters.tagWithAttribute("div", "class", "searchleft"), // redundant: within 'headerouter'
        // author services, including author testimonials
        HtmlNodeFilters.tagWithAttribute("div", "class", "hideForPrint"), // redundant: within 'footer-banner' [LA]
        // Latest News items change over time
        HtmlNodeFilters.tagWithAttribute("div", "id", "news_holder"), // redundant: within 'rightcolumn'
        // # total libertas academica article views
        HtmlNodeFilters.tagWithAttribute("p", "class", "laarticleviews"), // no longer present? [LA]
        // "Our service promise" [LA]
        HtmlNodeFilters.tagWithAttribute("div", "id", "ourservicepromise"), // no longer present? [LA]
        // Constantly changing reference to css file: css/grid.css?1337026463
        HtmlNodeFilters.tagWithAttributeRegex("link", "href", "css/grid.css\\?[0-9]+"), // no longer present? [LA]
    };
    
    return new HtmlFilterInputStream(in,
              		             encoding,
              			     HtmlNodeFilterTransform.exclude(new OrFilter(filters)));
  }
  
}
