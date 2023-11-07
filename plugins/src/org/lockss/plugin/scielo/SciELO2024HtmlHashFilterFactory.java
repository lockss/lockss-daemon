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

package org.lockss.plugin.scielo;

import java.io.InputStream;

import org.htmlparser.NodeFilter;
import org.htmlparser.filters.OrFilter;
import org.htmlparser.filters.TagNameFilter;
import org.lockss.daemon.PluginException;
import org.lockss.filter.html.HtmlFilterInputStream;
import org.lockss.filter.html.HtmlNodeFilterTransform;
import org.lockss.filter.html.HtmlNodeFilters;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.plugin.FilterFactory;

public class SciELO2024HtmlHashFilterFactory implements FilterFactory{

    @Override
    public InputStream createFilteredInputStream(ArchivalUnit au, InputStream inputstream, String encoding)
            throws PluginException {
        // TODO Auto-generated method stub
        NodeFilter[] filters = new org.htmlparser.NodeFilter[]{

            HtmlNodeFilters.tag("header"), //removing header
            HtmlNodeFilters.tag("footer"), //removing footer
            HtmlNodeFilters.tag("head"),
            HtmlNodeFilters.tag("style"),
            HtmlNodeFilters.tag("script"),
            HtmlNodeFilters.tagWithAttribute("div", "class", "alternativeHeader"), //removing header that appears while scrolling
            HtmlNodeFilters.tagWithAttribute("section", "class", "journalContacts"), //removing address/contact information for journal
            HtmlNodeFilters.tagWithAttributeRegex("section", "class", "\\blevelMenu\\b"), //removing menu that includes previous/next buttons
            HtmlNodeFilters.tagWithAttributeRegex("div", "class", "\\bshare\\b"), //removing list of social media icons
            HtmlNodeFilters.tagWithAttribute("div", "class", "floatingMenuCtt"), //removing floating menu at bottom of page that contains metrics, citations, etc.
            HtmlNodeFilters.tagWithAttributeRegex("ul", "class", "\\bfloatingMenuMobile\\b"), //removing floating menu at bottom of page that appears on iPads
            HtmlNodeFilters.tagWithAttributeRegex("a", "class", "\\bfloatingBtnError\\b"), //removing Accessibility/Report Error floating button on side of page
            HtmlNodeFilters.tagWithAttribute("div", "id", "error_modal_id"),
            HtmlNodeFilters.tagWithAttribute("div", "id", "ModalRelatedArticles"),
            HtmlNodeFilters.tagWithAttribute("div", "id", "metric_modal_id"),
            HtmlNodeFilters.tagWithAttribute("div", "id", "ModalVersionsTranslations"),
            HtmlNodeFilters.tagWithAttribute("div", "id", "ModalScimago"),
            HtmlNodeFilters.tagWithAttributeRegex("div", "class", "\\bbox-filtro\\b"),
            
            HtmlNodeFilters.comment()
        };
        return new HtmlFilterInputStream(inputstream, encoding, HtmlNodeFilterTransform.exclude(new OrFilter(filters)));
    }
    
}
