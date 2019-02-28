package org.lockss.plugin.atypon.rsp;

import org.htmlparser.NodeFilter;
import org.lockss.daemon.PluginException;
import org.lockss.filter.html.HtmlNodeFilters;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.plugin.atypon.BaseAtyponHtmlCrawlFilterFactory;

import java.io.InputStream;

public class RoyalSocietyPublishingHtmlCrawlFilterFactory extends BaseAtyponHtmlCrawlFilterFactory {

  //Royal Society Publishing moved from Highwire Drupal to Atypon, the html structure is not quite same as other Atypons
  //For example, the following two jid has different html structures
  //https://royalsocietypublishing.org/doi/full/10.1098/rsbm.2018.0002
  //https://royalsocietypublishing.org/toc/rsbm/64
  //https://royalsocietypublishing.org/toc/rsbl/14/10
  //https://royalsocietypublishing.org/doi/full/10.1098/rsbm.2018.0002
  //https://royalsocietypublishing.org/doi/full/10.1098/rsbl.2018.0532
  NodeFilter[] filters = new NodeFilter[] {
          // NOTE: overcrawling is an occasional issue with in-line references to "original article"
          HtmlNodeFilters.tag("header"),
          HtmlNodeFilters.tag("footer"),

          // Remove from toc page
          HtmlNodeFilters.tagWithAttributeRegex("div", "class", "publication-header"),
          HtmlNodeFilters.tagWithAttributeRegex("div", "class", "navigation-column"),
          HtmlNodeFilters.tagWithAttributeRegex("div", "class", "toc-right-side"),
          HtmlNodeFilters.tagWithAttributeRegex("div", "class", "content-navigation"),

          // Remove from doi/full/abs/refererence page
          HtmlNodeFilters.tagWithAttributeRegex("div", "class", "article-row-right"),

          // Need to download citation from this div
          HtmlNodeFilters.allExceptSubtree(
                  HtmlNodeFilters.tagWithAttribute("div", "class", "left-side"),
                  HtmlNodeFilters.tagWithAttributeRegex(
                          "a", "href", "/action/showCitFormats\\?"))
  };

  public InputStream createFilteredInputStream(ArchivalUnit au,
                                               InputStream in, String encoding) throws PluginException {
    return super.createFilteredInputStream(au, in, encoding, filters);
  }

}

