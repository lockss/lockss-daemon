package org.lockss.plugin.silverchair;

import org.htmlparser.NodeFilter;
import org.lockss.daemon.PluginException;
import org.lockss.filter.html.HtmlNodeFilters;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.util.Logger;

import java.io.InputStream;

public class SilverchairCommonThemeHtmlHashFilterFactory extends BaseScHtmlHashFilterFactory {

    protected boolean doExtraSpecialFilter() {
        return false;
    }

    protected boolean doXForm() {
        return true;
    }

    private static final Logger log = Logger.getLogger(SilverchairCommonThemeHtmlHashFilterFactory.class);

    @Override
    public InputStream createFilteredInputStream(final ArchivalUnit au,
                                                 InputStream in,
                                                 String encoding)
            throws PluginException {

        NodeFilter[] includeFilters = new NodeFilter[] {
                // <div class="widget-ContentBrowseByYearManifest widget-instance-IssueBrowseByYear">
                HtmlNodeFilters.tagWithAttributeRegex("div", "class", "widget-Content.+Manifest"),
                // <div id="ArticleList">
                HtmlNodeFilters.tagWithAttributeRegex("div", "class", "article-list-resources"),
                HtmlNodeFilters.tagWithAttributeRegex("div", "id", "ContentColumn"),
        };

        NodeFilter[] moreExcludeFilters = new NodeFilter[] {
                HtmlNodeFilters.tagWithAttribute("div","class", "kwd-group"),
                HtmlNodeFilters.tagWithAttributeRegex("div", "class", "author-info-wrap"),
                HtmlNodeFilters.tagWithAttributeRegex("div", "class", "pub-history-wrap"),
        };

        return createFilteredInputStream(au, in, encoding, includeFilters, moreExcludeFilters);
    }
}

