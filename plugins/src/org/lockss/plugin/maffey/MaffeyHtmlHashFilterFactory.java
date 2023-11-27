/*

Copyright (c) 2000-2023, Board of Trustees of Leland Stanford Jr. University

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are met:

1. Redistributions of source code must retain the above copyright notice,
this list of conditions and the following disclaimer.

2. Redistributions in binary form must reproduce the above copyright notice,
this list of conditions and the following disclaimer in the documentation
and/or other materials provided with the distribution.

3. Neither the name of the copyright holder nor the names of its contributors
may be used to endorse or promote products derived from this software without
specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
POSSIBILITY OF SUCH DAMAGE.

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
