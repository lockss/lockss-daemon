package org.lockss.plugin.taylorandfrancis;

import java.io.InputStream;

import org.htmlparser.NodeFilter;
import org.htmlparser.filters.OrFilter;
import org.lockss.daemon.PluginException;
import org.lockss.filter.html.*;
import org.lockss.plugin.*;

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
          // Top part of main content area of TOC
          HtmlNodeFilters.tagWithAttributeRegex("div", "class", "overview"),
          // Each article block in TOC
          HtmlNodeFilters.tagWithAttributeRegex("div", "class", "article"),
        }))
      )
    );
    
    return filtered;
  }

}
