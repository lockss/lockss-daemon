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
        HtmlNodeFilters.tagWithAttribute("div", "id", "responses_block")
    };

    @Override
    public InputStream createFilteredInputStream(ArchivalUnit au,
                                                 InputStream in, String encoding) throws PluginException {

        return new HtmlFilterInputStream(in, encoding,
                HtmlNodeFilterTransform.exclude(new OrFilter(filters)));
    }

}

