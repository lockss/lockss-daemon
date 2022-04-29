package org.lockss.plugin.pubfactory;

import org.htmlparser.NodeFilter;
import org.htmlparser.filters.OrFilter;
import org.lockss.daemon.PluginException;
import org.lockss.filter.html.HtmlFilterInputStream;
import org.lockss.filter.html.HtmlNodeFilterTransform;
import org.lockss.filter.html.HtmlNodeFilters;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.plugin.FilterFactory;

import java.io.InputStream;

public class PubFactoryHtmlCrawlFilterFactory implements FilterFactory {

  public InputStream createFilteredInputStream(ArchivalUnit au,
                                               InputStream in,
                                               String encoding)
      throws PluginException {

    NodeFilter[] excludeFilters = new NodeFilter[] {
      // dropdown for issues and volumes. just unnecessary to crawl.
      HtmlNodeFilters.tagWithAttributeRegex("div", "class", "component-volume-issue-selector"),
      // references box has unnecessary urls, and also improperly formatted href attributes that result in 403s
      // e.g. Ajtmh - https://www.ajtmh.org/view/journals/tpmd/92/2/article-p454.xml
        // sidebar references
        HtmlNodeFilters.tagWithAttributeRegex("div", "class", "content-references-list"),
        // article content references
        HtmlNodeFilters.tagWithAttributeRegex("section", "class", "refSection"),
        // class="chap-endnotes"
        HtmlNodeFilters.tagWithAttribute("section", "class", "chap-endnotes"),
        // ibid
        HtmlNodeFilters.tagWithAttributeRegex("ul", "class", "refList"),
      // related content is similarly unnecessary
      HtmlNodeFilters.tagWithAttributeRegex("div", "class", "component-related-content"),
      HtmlNodeFilters.tagWithAttributeRegex("a", "class", "relatedArticle"),
    };

    return new HtmlFilterInputStream(in,
      encoding,
      HtmlNodeFilterTransform.exclude(new OrFilter(
        excludeFilters
      )));
  }
}