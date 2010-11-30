package org.lockss.plugin.informaworld;

import java.io.InputStream;

import org.htmlparser.NodeFilter;
import org.htmlparser.filters.*;
import org.lockss.daemon.PluginException;
import org.lockss.filter.html.*;
import org.lockss.plugin.*;

public class InformaworldCrawlHtmlFilterFactory implements FilterFactory {

  public InputStream createFilteredInputStream(ArchivalUnit au,
                                               InputStream in,
                                               String encoding)
      throws PluginException {
    NodeFilter[] filters = new NodeFilter[] {
        // Remove URLs to articles outwith current volume
        HtmlNodeFilters.tagWithAttribute("div", "id", "ifirst"),
        HtmlNodeFilters.tagWithAttribute("div", "class", "sandbox"),
    };
    return new HtmlFilterInputStream(in,
                                     encoding,
                                     HtmlNodeFilterTransform.exclude(new OrFilter(filters)));
  }

}
