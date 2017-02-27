/*
 * $Id$*/

/*

Copyright (c) 2017 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.plugin.projmuse;

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

public class ProjectMuse2017HtmlHashFilterFactory implements FilterFactory {

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
        HtmlNodeFilters.tagWithAttribute("div", "class", "header"),
        HtmlNodeFilters.tagWithAttribute("div", "class", "breadcrumb"),
        HtmlNodeFilters.tagWithAttribute("div", "class", "right_nav"),
        HtmlNodeFilters.tagWithAttribute("div", "class", "footer"),
        HtmlNodeFilters.tagWithAttribute("div", "id", "map"),
        HtmlNodeFilters.tagWithAttributeRegex("a", "class", "(prev|next)issue"),
        
        /*
         * Broad area filtering
         */
        // Scripts, comments, head
        HtmlNodeFilters.tag("script"),
        HtmlNodeFilters.comment(),
        HtmlNodeFilters.tag("head"),
        /*
         * From older versions of the filter (may be moot) 
          // Main content area
          HtmlNodeFilters.tagWithAttribute("div", "id", "citationsblock"), // Inline citation popup, which can evolve
          // Contents (including images) change over time
          HtmlNodeFilters.tagWithAttribute("div", "class", "related"),
          HtmlNodeFilters.tagWithAttribute("div", "id", "related-box"),
         */
    };
    
    HtmlTransform xform = new HtmlTransform() {
      @Override
      public NodeList transform(NodeList nodeList) throws IOException {
        try {
          nodeList.visitAllNodesWith(new NodeVisitor() {
            @Override
            public void visitTag(Tag tag) {
              String name = tag.getTagName().toLowerCase();
              if ("meta".equals(name)) {
                String val = tag.getAttribute("content");
                if (val != null) {
                  tag.setAttribute("content", AuUtil.normalizeHttpHttpsFromBaseUrl(au, val));
                }
                return;
              }
              if (backgroundTags.contains(name)) {
                String val = tag.getAttribute("background");
                if (val != null) {
                  tag.setAttribute("background", AuUtil.normalizeHttpHttpsFromBaseUrl(au, val));
                }
                return;
              }
              if (hrefTags.contains(name)) {
                String val = tag.getAttribute("href");
                if (val != null) {
                  tag.setAttribute("href", AuUtil.normalizeHttpHttpsFromBaseUrl(au, val));
                }
                return;
              }
              if (srcTags.contains(name)) {
                String val = tag.getAttribute("src");
                if (val != null) {
                  tag.setAttribute("src", AuUtil.normalizeHttpHttpsFromBaseUrl(au, val));
                }
                return;
              }
            }
          });
          return nodeList;
        }
        catch (ParserException pe) {
          throw new IOException(pe);
        }
      }
    };
    
    // First filter with HtmlParser
    InputStream filtered = new HtmlFilterInputStream(in,
                                                     encoding,
                                                     new HtmlCompoundTransform(HtmlNodeFilterTransform.exclude(new OrFilter(filters)),
                                                                               xform));
    
    // Then normalize whitespace
    Reader filteredReader = FilterUtil.getReader(filtered, encoding);
    return new ReaderInputStream(new WhiteSpaceFilter(filteredReader));
  }

}
