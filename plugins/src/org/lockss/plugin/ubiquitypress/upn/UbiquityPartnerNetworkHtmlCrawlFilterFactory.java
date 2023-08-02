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

package org.lockss.plugin.ubiquitypress.upn;

import java.io.InputStream;

import org.htmlparser.NodeFilter;
import org.htmlparser.filters.*;
import org.lockss.daemon.PluginException;
import org.lockss.filter.html.*;
import org.lockss.plugin.*;


public class UbiquityPartnerNetworkHtmlCrawlFilterFactory implements FilterFactory {

  @Override
  public InputStream createFilteredInputStream(ArchivalUnit au,
                                               InputStream in,
                                               String encoding)
      throws PluginException {
    // Note from Thib: Most of this looks suspiciously like a crawl filter for OJS ???
    
    NodeFilter[] keepers = new NodeFilter[] {
        HtmlNodeFilters.tagWithAttribute("div", "id", "content"),
        HtmlNodeFilters.tag("img"),
        HtmlNodeFilters.tagWithAttribute("div", "id", "sidebarRTArticleTools")
    };
    
    NodeFilter[] filters = new NodeFilter[] {
        
        // Some sidebars contain links to all other issue TOCs
        // e.g. http://ojs.statsbiblioteket.dk/index.php/bras/issue/view/1049
        // Keep sidebar images (important logos) and article toolbox (printer-friendly link, etc.)
        HtmlNodeFilters.allExceptSubtree(HtmlNodeFilters.tagWithAttribute("div", "id", "sidebar"),
                                         new OrFilter(keepers)),
        
        // do not get links from navbar
        HtmlNodeFilters.tagWithAttribute("div", "id", "navbar"),
        
        // nor breadcrumbs
        HtmlNodeFilters.tagWithAttribute("div", "id", "breadcrumb"),
        
        // do not get links from references section
        HtmlNodeFilters.tagWithAttribute("div", "id", "articleCitations"),
        
        /*
         * Now that we are allowing TOC pages, we must be pro-active about
         * preventing overcrawls. The list of issues is in a right-hand sidebar
         * on TOC pages.
         */
        HtmlNodeFilters.tagWithAttribute("div", "class", "volume-list"),
    };
    return new HtmlFilterInputStream(in,
                                     encoding,
                                     HtmlNodeFilterTransform.exclude(new OrFilter(filters)));
  }
  
}
