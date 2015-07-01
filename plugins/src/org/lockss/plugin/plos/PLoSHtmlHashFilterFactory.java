/*
 * $Id: ProjectMuseHtmlHashFilterFactory.java 42078 2015-05-15 05:53:28Z etenbrink $*/

/*

Copyright (c) 2000-2015 Board of Trustees of Leland Stanford Jr. University,
all rights reserved.

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in
all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.  IN NO EVENT SHALL
STANFORD UNIVERSITY BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR
IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.

Except as contained in this notice, the name of Stanford University shall not
be used in advertising or otherwise to promote the sale, use or other dealings
in this Software without prior written authorization from Stanford University.

*/

package org.lockss.plugin.plos;

import java.io.*;
import java.util.*;

import org.htmlparser.*;
import org.htmlparser.filters.OrFilter;
import org.htmlparser.util.*;
import org.htmlparser.visitors.NodeVisitor;
import org.lockss.daemon.PluginException;
import org.lockss.filter.FilterUtil;
import org.lockss.filter.WhiteSpaceFilter;
import org.lockss.filter.html.*;
import org.lockss.plugin.*;
import org.lockss.util.ReaderInputStream;

public class PLoSHtmlHashFilterFactory implements FilterFactory {

  protected static final Set<String> backgroundTags =
      Collections.unmodifiableSet(new HashSet<String>(Arrays.asList("body",
                                                                    "table",
                                                                    "td",
                                                                    "th",
                                                                    "tr")));
  
  protected static final Set<String> hrefTags =
      Collections.unmodifiableSet(new HashSet<String>(Arrays.asList("a",
                                                                    "link",
                                                                    "base")));
  
  protected static final Set<String> srcTags =
      Collections.unmodifiableSet(new HashSet<String>(Arrays.asList("frame",
                                                                    "iframe",
                                                                    "img",
                                                                    "script")));
  
  @Override
  public InputStream createFilteredInputStream(final ArchivalUnit au,
                                               InputStream in,
                                               String encoding)
      throws PluginException {
    NodeFilter[] filters = new NodeFilter[] {
        /*
         * From the crawl filter 
         */
        // Contents (including images) change over time
        HtmlNodeFilters.tagWithAttribute("div", "class", "related"),
        HtmlNodeFilters.tagWithAttribute("div", "id", "related-box"),
        /*
         * Broad area filtering
         */
        // Scripts, comments, head
        HtmlNodeFilters.tag("script"),
        HtmlNodeFilters.comment(),
        HtmlNodeFilters.tag("head"),
        // Header and footer
        HtmlNodeFilters.tagWithAttribute("div", "class", "header"),
        HtmlNodeFilters.tagWithAttribute("div", "class", "footer"),
        // Right column
        HtmlNodeFilters.tagWithAttribute("div", "class", "right_nav"),
        // Main content area
        HtmlNodeFilters.tagWithAttribute("div", "class", "breadcrumb"), // Breadcrumbs
        HtmlNodeFilters.tagWithAttribute("div", "id", "citationsblock"), // Inline citation popup, which can evolve
    };
    
    
    // First filter with HtmlParser
    InputStream filtered = 
        new HtmlFilterInputStream(in,
                                  encoding,
                                  HtmlNodeFilterTransform.exclude(new OrFilter(filters)));

    // Then normalize whitespace
    Reader filteredReader = FilterUtil.getReader(filtered, encoding);
    return new ReaderInputStream(new WhiteSpaceFilter(filteredReader));
  }

}
