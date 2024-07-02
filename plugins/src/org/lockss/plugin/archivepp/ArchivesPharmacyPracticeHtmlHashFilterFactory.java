package org.lockss.plugin.archivepp;

import org.htmlparser.NodeFilter;
import org.htmlparser.filters.OrFilter;
import org.lockss.daemon.PluginException;
import org.lockss.filter.html.HtmlFilterInputStream;
import org.lockss.filter.html.HtmlNodeFilterTransform;
import org.lockss.filter.html.HtmlNodeFilters;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.plugin.FilterFactory;

import java.io.InputStream;

public class ArchivesPharmacyPracticeHtmlHashFilterFactory implements FilterFactory {
    protected static NodeFilter[] filters = new NodeFilter[]{
            HtmlNodeFilters.tag("head"),
            HtmlNodeFilters.tag("script"),
            HtmlNodeFilters.tag("noscript"),
            HtmlNodeFilters.tag("style"),
            HtmlNodeFilters.tag("header"),
            HtmlNodeFilters.tag("footer"),
            HtmlNodeFilters.tag("iframe"),
            HtmlNodeFilters.comment(),

            HtmlNodeFilters.tagWithAttributeRegex("div", "id", "article_citation"),
            
    };

    @Override
    public InputStream createFilteredInputStream(ArchivalUnit au,
                                                 InputStream in, String encoding) throws PluginException {

        return new HtmlFilterInputStream(in, encoding,
                HtmlNodeFilterTransform.exclude(new OrFilter(filters)));
    }
}
