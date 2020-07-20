package org.lockss.plugin.silverchair;

import org.htmlparser.NodeFilter;
import org.htmlparser.filters.OrFilter;
import org.lockss.daemon.PluginException;
import org.lockss.filter.html.HtmlFilterInputStream;
import org.lockss.filter.html.HtmlNodeFilterTransform;
import org.lockss.filter.html.HtmlNodeFilters;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.plugin.FilterFactory;
import org.lockss.util.Logger;

import java.io.InputStream;

public class SilverchairCommonThemeHtmlHashFilterFactory implements FilterFactory {
    protected static NodeFilter[] filters = new NodeFilter[]{
            HtmlNodeFilters.tag("head"),
            HtmlNodeFilters.tag("script"),
            HtmlNodeFilters.tag("noscript"),
            HtmlNodeFilters.tag("style"),
            HtmlNodeFilters.tag("header"),
            HtmlNodeFilters.tag("footer"),
            HtmlNodeFilters.tag("iframe"),

            HtmlNodeFilters.comment(),

            // https://rupress.org/jem/article/215/2/521/42535/T-cells-provide-the-early-source-of-IFN-to
            HtmlNodeFilters.tagWithAttributeRegex("section", "class", "master-header"),
            HtmlNodeFilters.tagWithAttributeRegex("div", "class", "widget-instance-SitePageFooter"),
            HtmlNodeFilters.tagWithAttributeRegex("div", "class", "ad-banner"),
            HtmlNodeFilters.tagWithAttributeRegex("div", "id", "gs-casa-r"),
            HtmlNodeFilters.tagWithAttributeRegex("div", "class", "footer_wrap"),
            HtmlNodeFilters.tagWithAttributeRegex("div", "class", "widget-instance-GdprCookieBanner"),
            HtmlNodeFilters.tagWithAttributeRegex("div", "class", "widget-instance-SiteWideModals"),
            HtmlNodeFilters.tagWithAttributeRegex("div", "class", "ss-ui-only"),
            HtmlNodeFilters.tagWithAttributeRegex("div", "class", "site-theme-header"),
            HtmlNodeFilters.tagWithAttributeRegex("div", "class", "widget-instance-SitePageHeader"),
            HtmlNodeFilters.tagWithAttributeRegex("div", "class", "ref-list"),
            HtmlNodeFilters.tagWithAttributeRegex("div", "class", "vt-related-content"),
            HtmlNodeFilters.tagWithAttributeRegex("div", "class", "artmet-views"),
            HtmlNodeFilters.tagWithAttributeRegex("div", "class", "artmet-citations"),
            HtmlNodeFilters.tagWithAttributeRegex("div", "class", "artmet-altmetric"),
            HtmlNodeFilters.tagWithAttributeRegex("div", "class", "artmet-dimensions"),
            HtmlNodeFilters.tagWithAttributeRegex("div", "class", "widget-IssueInfo"),
            HtmlNodeFilters.tagWithAttributeRegex("div", "class", "content-nav"),
            HtmlNodeFilters.tagWithAttributeRegex("section", "class", "figure-table-wrapper"),
            HtmlNodeFilters.tagWithAttributeRegex("div", "class", "userAlert"),
            HtmlNodeFilters.tagWithAttributeRegex("div", "class", "widget-ArticleNavLinks"),
            HtmlNodeFilters.tagWithAttributeRegex("div", "class", "adblock-wrap"),
            HtmlNodeFilters.tagWithAttributeRegex("div", "class", "panels"),
            HtmlNodeFilters.tagWithAttributeRegex("div", "class", "widget-ArticleDataSupplements"),
            HtmlNodeFilters.tagWithAttributeRegex("div", "class", "widget-instance-SplitView_TabPane"),
            HtmlNodeFilters.tagWithAttributeRegex("div", "class", "authornotes-section-wrapper"),
            HtmlNodeFilters.tagWithAttributeRegex("div", "class", "permissionstatement-section-wrapper"),
            HtmlNodeFilters.tagWithAttributeRegex("div", "class", "toolbar-wrap"),
            
    };

    @Override
    public InputStream createFilteredInputStream(ArchivalUnit au,
                                                 InputStream in, String encoding) throws PluginException {

        return new HtmlFilterInputStream(in, encoding,
                HtmlNodeFilterTransform.exclude(new OrFilter(filters)));
    }
}
