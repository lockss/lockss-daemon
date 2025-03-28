/*

Copyright (c) 2000-2025, Board of Trustees of Leland Stanford Jr. University

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

package org.lockss.plugin.silverchair.ash;

import org.htmlparser.NodeFilter;
import org.htmlparser.filters.OrFilter;
import org.lockss.daemon.PluginException;
import org.lockss.filter.html.HtmlFilterInputStream;
import org.lockss.filter.html.HtmlNodeFilterTransform;
import org.lockss.filter.html.HtmlNodeFilters;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.plugin.FilterFactory;

import java.io.InputStream;

public class ASHHtmlCrawlFilterFactory implements FilterFactory{
    
    public InputStream createFilteredInputStream(ArchivalUnit au,
                                                 InputStream in,
                                                 String encoding)
            throws PluginException {
        NodeFilter[] filters = new NodeFilter[] {
            // all manner of patterns and examples of headers and footers.
            HtmlNodeFilters.tag("header"),
            HtmlNodeFilters.tag("footer"),
            HtmlNodeFilters.tag("style"),
            HtmlNodeFilters.tagWithAttributeRegex("link","href","custom"),
            HtmlNodeFilters.tagWithAttributeRegex("section", "class", "master-header"),
            HtmlNodeFilters.tagWithAttributeRegex("section", "class", "footer_wrap"),
            HtmlNodeFilters.tagWithAttributeRegex("section", "class", "site-footer"),
            HtmlNodeFilters.tagWithAttributeRegex("div", "class", "SitePageFooter"),
            HtmlNodeFilters.tagWithAttributeRegex("div", "class", "site-theme-footer"),
            HtmlNodeFilters.tagWithAttributeRegex("div", "class", "global-footer"),
            HtmlNodeFilters.tagWithAttribute("div", "class", "comment"),
            HtmlNodeFilters.tagWithAttributeRegex("div", "class", "ref-list"),
            HtmlNodeFilters.tagWithAttributeRegex("div", "class", "global-footer"),
            HtmlNodeFilters.tagWithAttributeRegex("a", "href", "^10[.]"),
            HtmlNodeFilters.tagWithAttributeRegex("div", "id", "Sidebar"),
            HtmlNodeFilters.tagWithAttributeRegex("div", "class", "global-nav"),
            HtmlNodeFilters.tagWithAttributeRegex("div", "class", "new-and-popular"),
            HtmlNodeFilters.tagWithAttributeRegex("div", "class", "global-footer-link-wrap"),
            HtmlNodeFilters.tagWithAttributeRegex("div", "class", "issue-browse-top"),
            HtmlNodeFilters.tagWithAttributeRegex("li", "class", "geoRef-coordinate"),
            HtmlNodeFilters.tagWithAttributeRegex("div", "id", "Sidebar"),
            HtmlNodeFilters.tagWithAttributeRegex("div", "class", "related-content"),
            HtmlNodeFilters.tagWithAttributeRegex("div", "class", "related-content__block"),
            HtmlNodeFilters.tagWithAttributeRegex("a", "class", "related-content"),
            HtmlNodeFilters.tagWithAttributeRegex("div", "class", "widget-IssueInfo"),
            HtmlNodeFilters.tagWithAttributeRegex("div", "class", "widget-ArticleNavLinks"),
        };

        return new HtmlFilterInputStream(in,
                encoding,
                HtmlNodeFilterTransform.exclude(new OrFilter(filters)));
    }
}
