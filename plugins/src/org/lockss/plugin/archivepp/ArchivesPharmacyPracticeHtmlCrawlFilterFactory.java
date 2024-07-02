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

public class ArchivesPharmacyPracticeHtmlCrawlFilterFactory implements FilterFactory {

    public InputStream createFilteredInputStream(ArchivalUnit au,
                                                 InputStream in,
                                                 String encoding)
            throws PluginException {
        NodeFilter[] filters = new NodeFilter[] {
            // all manner of patterns and examples of headers and footers.
            HtmlNodeFilters.tag("header"),
            HtmlNodeFilters.tag("footer"),
            HtmlNodeFilters.tagWithAttributeRegex("div", "id", "article_citation"),
        };

        return new HtmlFilterInputStream(in,
                encoding,
                HtmlNodeFilterTransform.exclude(new OrFilter(filters)));
    }

}
