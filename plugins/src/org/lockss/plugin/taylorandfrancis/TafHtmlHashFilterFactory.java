package org.lockss.plugin.taylorandfrancis;

import java.io.InputStream;

import org.htmlparser.NodeFilter;
import org.htmlparser.filters.OrFilter;
import org.lockss.daemon.PluginException;
import org.lockss.filter.html.*;
import org.lockss.plugin.*;

/**
 * This filter will eventually replace
 * {@link TaylorAndFrancisHtmlHashFilterFactory}.
 */
public class TafHtmlHashFilterFactory implements FilterFactory {

  @Override
  public InputStream createFilteredInputStream(ArchivalUnit au,
                                               InputStream in,
                                               String encoding)
      throws PluginException {

    InputStream filtered = new HtmlFilterInputStream(
      in,
      encoding,
      new HtmlCompoundTransform(
        // First throw out everything but the main content areas
        HtmlNodeFilterTransform.include(new OrFilter(new NodeFilter[] {
            // Keep top part of main content area [TOC]
            HtmlNodeFilters.tagWithAttributeRegex("div", "class", "overview"),
            // Keep each article block [TOC]
            HtmlNodeFilters.tagWithAttributeRegex("div", "class", "article"),
        })),
        // Then filter remaining content areas
        HtmlNodeFilterTransform.exclude(new OrFilter(new NodeFilter[] {
            // Drop scripts, styles, comments
            HtmlNodeFilters.tag("script"),
            HtmlNodeFilters.tag("noscript"),
            HtmlNodeFilters.tag("style"),
            HtmlNodeFilters.comment(),
        }))
      )
    );
    
    return filtered;
  }

}
