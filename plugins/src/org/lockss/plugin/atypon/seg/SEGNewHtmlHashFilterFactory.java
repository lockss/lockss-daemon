package org.lockss.plugin.atypon.seg;

import org.htmlparser.NodeFilter;
import org.htmlparser.filters.OrFilter;
import org.htmlparser.filters.TagNameFilter;
import org.lockss.filter.FilterUtil;
import org.lockss.filter.html.HtmlFilterInputStream;
import org.lockss.filter.html.HtmlNodeFilterTransform;
import org.lockss.filter.html.HtmlNodeFilters;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.plugin.FilterFactory;
import org.lockss.util.ReaderInputStream;

import java.io.InputStream;
import java.io.Reader;

public class SEGNewHtmlHashFilterFactory implements FilterFactory {

    public InputStream createFilteredInputStream(ArchivalUnit au,
                                                 InputStream in,
                                                 String encoding) {
        NodeFilter[] filters = new NodeFilter[] {
                new TagNameFilter("noscript"),
                new TagNameFilter("script"),
                new TagNameFilter("style"),
                new TagNameFilter("head"),
                new TagNameFilter("style"),
                new TagNameFilter("header"),
                new TagNameFilter("footer"),

                // article page: https://library.seg.org/doi/10.1190/INT-2017-1213-SPSEINTRO.1
                HtmlNodeFilters.tagWithAttribute("div", "class", "rlist"),
                HtmlNodeFilters.tagWithAttribute("section", "class", "copywrites"),
                HtmlNodeFilters.tagWithAttribute("section", "class", "publisher"),
                HtmlNodeFilters.tagWithAttribute("section", "class", "article__history"),

                // toc page: https://library.seg.org/toc/inteio/6/1
                HtmlNodeFilters.tagWithAttribute("div", "class", "page-top-banner"),
                HtmlNodeFilters.tagWithAttributeRegex("div", "class", "popup"),
                HtmlNodeFilters.tagWithAttributeRegex("div", "class", "content-navigation"),
                HtmlNodeFilters.tagWithAttribute("div", "class", "shift-up-content"),
                HtmlNodeFilters.tagWithAttribute("div", "class", "content-navigation"),
                HtmlNodeFilters.tagWithAttribute("div", "class", "current-issue"),
                HtmlNodeFilters.tagWithAttribute("div", "class", "table-of-content__navigation"),
                HtmlNodeFilters.tagWithAttribute("div", "class", "social-menus"),
                HtmlNodeFilters.tagWithAttribute("ul", "class", "rlist"),

        };
        InputStream filteredStream = new HtmlFilterInputStream(in, encoding,
                HtmlNodeFilterTransform.exclude(new OrFilter(filters)));
        Reader httpFilter = FilterUtil.getReader(filteredStream, encoding);
        return new ReaderInputStream(httpFilter);
    }

}

