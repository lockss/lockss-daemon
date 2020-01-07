package org.lockss.plugin.highwire.ada;

import org.htmlparser.NodeFilter;
import org.htmlparser.filters.AndFilter;
import org.htmlparser.filters.OrFilter;
import org.lockss.daemon.PluginException;
import org.lockss.filter.html.HtmlFilterInputStream;
import org.lockss.filter.html.HtmlNodeFilterTransform;
import org.lockss.filter.html.HtmlNodeFilters;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.plugin.FilterFactory;

import java.io.InputStream;

public class ADADrupalHtmlCrawlFilterFactory implements FilterFactory {

    protected static NodeFilter[] baseHWFilters = new NodeFilter[] {
            // Do not crawl header or footer for links
            HtmlNodeFilters.tag("header"),
            HtmlNodeFilters.tag("footer"),
            /*
            Need to remove the right panel for the following reason
            http://clinical.diabetesjournals.org/content/35/1/27 ==>
            https://clinical.diabetesjournals.org/content/35/1/27 ==> contains two PDFs,
            https://clinical.diabetesjournals.org/content/diaclin/35/1/27.full-text.pdf on the right side
            along with print/share/like button
            https://clinical.diabetesjournals.org/content/35/1/27.full-text.pdf is on the center top
            The confusion part is https://clinical.diabetesjournals.org/content/35/1/27.full-text.pdf
            will redirected to https://clinical.diabetesjournals.org/content/diaclin/35/1/27.full-text.pdf
            */
            HtmlNodeFilters.tagWithAttributeRegex("div", "id", "mini-panel-jnl_ada_art_tools"),
    };

    @Override
    public InputStream createFilteredInputStream(ArchivalUnit au,
                                                 InputStream in,
                                                 String encoding)
            throws PluginException {

        return new HtmlFilterInputStream(in, encoding,
                HtmlNodeFilterTransform.exclude(new OrFilter(baseHWFilters)));
    }
}

