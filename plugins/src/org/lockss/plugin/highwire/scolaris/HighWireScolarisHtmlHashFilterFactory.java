package org.lockss.plugin.highwire.scolaris;

import org.htmlparser.NodeFilter;
import org.htmlparser.filters.OrFilter;
import org.lockss.filter.FilterUtil;
import org.lockss.filter.WhiteSpaceFilter;
import org.lockss.filter.html.HtmlFilterInputStream;
import org.lockss.filter.html.HtmlNodeFilterTransform;
import org.lockss.filter.html.HtmlNodeFilters;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.plugin.FilterFactory;
import org.lockss.util.ReaderInputStream;

import java.io.InputStream;
import java.io.Reader;

public class HighWireScolarisHtmlHashFilterFactory implements FilterFactory {

  public InputStream createFilteredInputStream(ArchivalUnit au,
                                               InputStream in,
                                               String encoding) {

    NodeFilter[] filters = new NodeFilter[] {
        HtmlNodeFilters.tag("head"),
        // Contains variable ad-generating code
        HtmlNodeFilters.tag("script"),
        // Contains variable ad-generating code
        HtmlNodeFilters.tag("noscript"),
        // Typically contains ads (e.g. American Academy of Pediatrics)
        HtmlNodeFilters.tag("object"),
        // Typically contains ads
        HtmlNodeFilters.tag("iframe"),
        HtmlNodeFilters.tag("header"),
        HtmlNodeFilters.tag("footer"),
        HtmlNodeFilters.tagWithAttributeRegex("a", "class", "visually-hidden"),
        HtmlNodeFilters.tagWithAttributeRegex("div", "class", "searchbar"),
        HtmlNodeFilters.tagWithAttributeRegex("section", "class", "springer-(ecommerce|display-price)"),
        HtmlNodeFilters.tagWithAttribute("div", "id", "edit-group-metrics"),
        HtmlNodeFilters.tagWithAttributeRegex("div", "class", "sidebar-"),
        HtmlNodeFilters.tagWithAttribute("div", "id", "modal-iframe"),
        HtmlNodeFilters.tagWithAttributeRegex("div", "class", "a2a"),
    };


    // First filter with HtmlParser
    OrFilter orFilter = new OrFilter(filters);
    InputStream filtered =
        new HtmlFilterInputStream(in,
            encoding,
            HtmlNodeFilterTransform.exclude(orFilter));

    Reader rdr = FilterUtil.getReader(filtered, encoding);
    // white space filtering by default
    return new ReaderInputStream(new WhiteSpaceFilter(rdr));
  }

}