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

    public InputStream createFilteredInputStream(ArchivalUnit au,
                                                 InputStream in,
                                                 String encoding)
            throws PluginException {
        NodeFilter[] filters = new NodeFilter[] {
            // all manner of patterns and examples of headers and footers.
            HtmlNodeFilters.tag("header"),
            HtmlNodeFilters.tag("footer"),
            HtmlNodeFilters.tagWithAttributeRegex("section", "class", "master-header"),
            HtmlNodeFilters.tagWithAttributeRegex("section", "class", "footer_wrap"),
            HtmlNodeFilters.tagWithAttributeRegex("section", "class", "site-footer"),
            HtmlNodeFilters.tagWithAttributeRegex("div", "class", "SitePageFooter"),
            HtmlNodeFilters.tagWithAttributeRegex("div", "class", "site-theme-footer"),
            HtmlNodeFilters.tagWithAttributeRegex("div", "class", "global-footer"),
            /*
                Need to filter out all the articles referenced in the article bottom part:
                For example:
                https://pubs.geoscienceworld.org/sepm/jsedres/article/91/11/1133/609365/Environmental-magnetism-evidence-for-longshore

                <div class="comment">doi:<a class="link" href="10.3329/bjsir.v45i1.5173" target="_blank">10.3329/bjsir.v45i1.5173</a>.</div>

                will references to:
                https://pubs.geoscienceworld.org/sepm/jsedres/article-standard/91/11/1133/609365/10.3133/ds231
                https://pubs.geoscienceworld.org/sepm/jsedres/article-standard/91/11/1133/609365/10.3133/ofr20111155
                https://pubs.geoscienceworld.org/sepm/jsedres/article-standard/91/11/1133/609365/10.3329/bjsir.v45i1.5173
                https://pubs.geoscienceworld.org/sepm/jsedres/article-standard/91/11/1133/609365/10.3390/min4040758
                https://pubs.geoscienceworld.org/sepm/jsedres/article-standard/91/11/1133/609365/10.5327/s1519-874x2006000300002
             */
            //"global-nav"

            HtmlNodeFilters.tagWithAttribute("div", "class", "comment"),
            HtmlNodeFilters.tagWithAttributeRegex("div", "class", "ref-list"),
            HtmlNodeFilters.tagWithAttributeRegex("div", "class", "global-footer"),
            HtmlNodeFilters.tagWithAttributeRegex("a", "href", "^10[.]"),
        };

        return new HtmlFilterInputStream(in,
                encoding,
                HtmlNodeFilterTransform.exclude(new OrFilter(filters)));
    }

}
