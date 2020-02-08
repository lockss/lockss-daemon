package org.lockss.plugin.silverchair;

import org.htmlparser.NodeFilter;
import org.htmlparser.filters.OrFilter;
import org.lockss.daemon.PluginException;
import org.lockss.filter.html.HtmlFilterInputStream;
import org.lockss.filter.html.HtmlNodeFilterTransform;
import org.lockss.filter.html.HtmlNodeFilters;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.plugin.FilterFactory;

import java.io.InputStream;

public class SilverchairCommonThemeHtmlCrawlFilterFactory implements FilterFactory {


    @Override
    public InputStream createFilteredInputStream(ArchivalUnit au,
                                                 InputStream in,
                                                 String encoding)
            throws PluginException {
        return new HtmlFilterInputStream(

                in,
                encoding,
                HtmlNodeFilterTransform.exclude(new OrFilter(new NodeFilter[] {
                        // remove larger blocks first
                        HtmlNodeFilters.tagWithAttributeRegex("section", "class", "master-header"),
                        HtmlNodeFilters.tagWithAttributeRegex("section", "class", "footer_wrap"),
                        HtmlNodeFilters.tagWithAttributeRegex("div", "class", "widget-SitePageHeader"),
                        HtmlNodeFilters.tagWithAttributeRegex("div", "class", "widget-SitePageFooter"),
                        // article left side with image of cover and nav arrows
                        HtmlNodeFilters.tagWithAttributeRegex("div", "id", "InfoColumn"),
                        // right side of article - all the latest, most cited, etc
                        HtmlNodeFilters.tagWithAttributeRegex("div", "id", "Sidebar"),

                        // references section - contain links to google,pubmed - guard against internal refs
                        HtmlNodeFilters.tagWithAttributeRegex("div","class","^ref-list"),
                        HtmlNodeFilters.tagWithAttribute("div","class","kwd-group"),
                        // top of article - links to correction or original article
                        HtmlNodeFilters.tagWithAttributeRegex("div", "class", "widget-ArticleLinks"),
                        HtmlNodeFilters.tagWithAttributeRegex("div", "class", "widget-ToolboxSendEmail"),
                        // cannot remove widget-Issue, as it exists in multiple locations, some of which are needed
                        HtmlNodeFilters.tagWithAttributeRegex("div", "class", "issue-browse-top"),
                        HtmlNodeFilters.tagWithAttribute("div", "class", "all-issues"),
                        // don't collect the powerpoint version of images or slides
                        HtmlNodeFilters.tagWithAttributeRegex("a", "class", "download(-slide|Imagesppt)"),

                        // article - author section with notes could have some bogus relative links
                        HtmlNodeFilters.tagWithAttributeRegex("div", "class", "al-author-info-wrap"),
                }))
        );
    }

}

