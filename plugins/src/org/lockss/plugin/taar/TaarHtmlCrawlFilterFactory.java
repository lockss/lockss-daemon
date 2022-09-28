package org.lockss.plugin.taar;

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

public class TaarHtmlCrawlFilterFactory implements FilterFactory {
  protected static Logger log = Logger.getLogger(org.lockss.plugin.taar.TaarHtmlCrawlFilterFactory.class);

  static NodeFilter[] excludeFilters = new NodeFilter[] {
      HtmlNodeFilters.tag("header"),
      HtmlNodeFilters.tag("footer"),
      HtmlNodeFilters.tag("nav"),
      HtmlNodeFilters.tagWithAttribute("a", "title", "Exit"),
      HtmlNodeFilters.tagWithAttributeRegex("div", "class", "t3-sidebar"),
      HtmlNodeFilters.tagWithAttributeRegex("div", "class", "t3-module"),
      HtmlNodeFilters.tagWithAttributeRegex("div", "class", "tags"),
      HtmlNodeFilters.tagWithAttributeRegex("div", "id", "ref-ol"),
      HtmlNodeFilters.tagWithAttributeRegex("ul", "class", "pager|pagenav"),
  };

  public InputStream createFilteredInputStream(ArchivalUnit au,
                                               InputStream in, String encoding) throws PluginException {
    return new HtmlFilterInputStream(in,
        encoding,
        HtmlNodeFilterTransform.exclude(new OrFilter(
            excludeFilters
        )));
  }

}
