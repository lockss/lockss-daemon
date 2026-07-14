/*

Copyright (c) 2000-2026, Board of Trustees of Leland Stanford Jr. University

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

package org.lockss.plugin.resiliencealliance;

import org.htmlparser.NodeFilter;
import org.htmlparser.filters.OrFilter;
import org.lockss.daemon.PluginException;
import org.lockss.filter.html.HtmlFilterInputStream;
import org.lockss.filter.html.HtmlNodeFilterTransform;
import org.lockss.filter.html.HtmlNodeFilters;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.plugin.FilterFactory;

import java.io.InputStream;

public class ResilienceAllianceHashFilterFactory implements FilterFactory {
    protected static NodeFilter[] filters = new NodeFilter[]{
        HtmlNodeFilters.tag("head"),
        HtmlNodeFilters.tag("script"),
        HtmlNodeFilters.tag("noscript"),
        HtmlNodeFilters.tag("style"),
        HtmlNodeFilters.tag("header"),
        HtmlNodeFilters.tag("footer"),

        HtmlNodeFilters.comment(),

        // top menu on all pages https://www.ace-eco.org/vol14/iss1/art3/  
        HtmlNodeFilters.tagWithAttribute("div", "id", "ms_menu"),
        // right column on https://www.ace-eco.org/vol14/iss1/art3/
        HtmlNodeFilters.tagWithAttribute("div", "id", "att_panel"),
        // other sections inside the page https://www.ace-eco.org/vol14/iss1/art3/
        HtmlNodeFilters.tagWithAttribute("div", "id", "proof_copyright"),
        HtmlNodeFilters.tagWithAttribute("div", "id", "proof_citation"),
        HtmlNodeFilters.tagWithAttribute("div", "id", "proof_section"),
        HtmlNodeFilters.tagWithAttribute("div", "id", "affiliations"),
        HtmlNodeFilters.tagWithAttribute("ul", "id", "article_toc"),
        HtmlNodeFilters.tagWithAttribute("div", "id", "acknowledgments"),
        HtmlNodeFilters.tagWithAttribute("div", "id", "author_address"),
        HtmlNodeFilters.tagWithAttribute("div", "id", "attachments"),
        HtmlNodeFilters.tagWithAttribute("div", "id", "ms_uparrow"),
        HtmlNodeFilters.tagWithAttribute("div", "id", "authors"),
        HtmlNodeFilters.tagWithAttribute("div", "id", "ms_keywords"),
        HtmlNodeFilters.tagWithAttribute("div", "id", "responses_block"),
        HtmlNodeFilters.tagWithAttribute("div", "class", "article__social-share")
    };

    @Override
    public InputStream createFilteredInputStream(ArchivalUnit au,
                                                 InputStream in, String encoding) throws PluginException {

        return new HtmlFilterInputStream(in, encoding,
                HtmlNodeFilterTransform.exclude(new OrFilter(filters)));
    }

}

