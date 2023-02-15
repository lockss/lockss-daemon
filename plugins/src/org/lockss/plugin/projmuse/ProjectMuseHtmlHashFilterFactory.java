/*

Copyright (c) 2000-2021, Board of Trustees of Leland Stanford Jr. University
All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are met:

1. Redistributions of source code must retain the above copyright notice,
this list of conditions and the following disclaimer.

2. Redistributions in binary form must reproduce the above copyright notice,
this list of conditions and the following disclaimer in the documentation
and/or other materials provided with the distribution.

3. Neither the name of the copyright holder nor the names of its contributors
may be used to endorse or promote products derived from this software without
specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
POSSIBILITY OF SUCH DAMAGE.

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

public class ProjectMuseHtmlHashFilterFactory implements FilterFactory {

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
        /*
         * From older versions of the filter (may be moot) 
         */
        HtmlNodeFilters.tagWithAttribute("div", "id", "sliver"),
        HtmlNodeFilters.tagWithAttribute("div", "id", "header"),
        HtmlNodeFilters.tagWithAttribute("div", "id", "footer"),
        HtmlNodeFilters.tagWithAttribute("div", "id", "toolbar"), // ?
        HtmlNodeFilters.tagWithAttribute("div", "id", "breadcrumb"), // ?
        HtmlNodeFilters.tagWithAttribute("div", "id", "sidebar1"), // ?
        HtmlNodeFilters.tagWithAttribute("div", "id", "sidebar2"), // ?
        HtmlNodeFilters.tagWithAttribute("div", "id", "credits"), // ?
        HtmlNodeFilters.tagWithAttribute("div", "class", "main_footer"), // ?
    };
    
    HtmlTransform xform = null;
      xform = new HtmlTransform() {
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
