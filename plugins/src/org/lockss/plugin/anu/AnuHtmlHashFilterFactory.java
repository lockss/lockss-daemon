/*

Copyright (c) 2000-2023, Board of Trustees of Leland Stanford Jr. University

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

package org.lockss.plugin.anu;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;

import org.htmlparser.Node;
import org.htmlparser.NodeFilter;
import org.htmlparser.Tag;
import org.htmlparser.filters.*;
import org.htmlparser.nodes.TextNode;
import org.htmlparser.util.NodeList;
import org.lockss.filter.FilterUtil;
import org.lockss.filter.WhiteSpaceFilter;
import org.lockss.filter.html.*;
import org.lockss.plugin.*;
import org.lockss.util.ReaderInputStream;

public class AnuHtmlHashFilterFactory implements FilterFactory {

  // add a space after every ">", then remove from "<" to ">" essentially the same as xformAllTags
  // Reader addFilteredReader = new HtmlTagFilter(new StringFilter(reader,">", "> "), new TagPair("<",">"));
  protected static HtmlTransform xformAllTags = new HtmlTransform() {
    @Override
    public NodeList transform(NodeList nodeList) throws IOException {
      NodeList nl = new NodeList();
      for (int sx = 0; sx < nodeList.size(); sx++) {
        Node snode = nodeList.elementAt(sx);
        TextNode tn = new TextNode(snode.toPlainTextString() + " ");
        nl.add(tn);
      }
      return nl;
    }
  };
  
  public InputStream createFilteredInputStream(ArchivalUnit au,
                                               InputStream in,
                                               String encoding) {
    
    NodeFilter[] includeNodes = new NodeFilter[] {
        HtmlNodeFilters.tag("body"),
        
    };
    
    NodeFilter[] excludeNodes = new NodeFilter[] {
      // filter out comments
      HtmlNodeFilters.comment(),
      // filter out script
      new TagNameFilter("script"),
      // header & footer
      HtmlNodeFilters.tagWithAttribute("div", "id", "header"),
      HtmlNodeFilters.tagWithAttributeRegex("div", "id", "footer"),
      HtmlNodeFilters.tagWithAttribute("div", "id", "skipnavholder"),
      HtmlNodeFilters.tagWithAttribute("div", "id", "print-hdr"),
      // breadcrumbs
      HtmlNodeFilters.tagWithAttribute("div", "id", "breadcrumb"),
      HtmlNodeFilters.tagWithAttribute("div", "class", "breadcrumb"),
      // related
      HtmlNodeFilters.tagWithAttributeRegex("div", "class", "related"),
      // left menu
      HtmlNodeFilters.tagWithAttributeRegex("div", "class", "publication-lhs"),
      HtmlNodeFilters.tagWithAttributeRegex("div", "class", "business-unit"),
      HtmlNodeFilters.tagWithAttributeRegex("div", "class", "anu-share"),
      // <div id="bnr-wrap" class="bnr-gwy-high" role="banner">
      HtmlNodeFilters.tagWithAttribute("div", "id", "bnr-wrap"),
      HtmlNodeFilters.tagWithAttribute("div", "id", "dark-tab-wrap"),
      HtmlNodeFilters.tagWithAttribute("div", "id", "explore-wrap"),
      HtmlNodeFilters.tagWithAttribute("div", "role", "navigation"),
      HtmlNodeFilters.tagWithAttribute("div", "id", "menu"),
      HtmlNodeFilters.tagWithAttributeRegex("div", "class", "search"),
      HtmlNodeFilters.tagWithTextRegex("a", "^(Previous|Next)$"),
      // 
      new NodeFilter() {
        @Override
        public boolean accept(Node node) {
          // <div class="view view-authors-editors view-id-authors_editors view-display-id-pubs_authors view-dom-id-9cd04bed5c305b423aeaef99f33281e5">
          if (HtmlNodeFilters.tagWithAttributeRegex("div", "class", "dom-id-").accept(node)) {
            ((Tag)node).setAttribute("class", "dom-id");
          }
          return false;
        }
      },
    };
    
    InputStream interStream = new HtmlFilterInputStream(in, encoding,
        new HtmlCompoundTransform(
            HtmlNodeFilterTransform.include(new OrFilter(includeNodes)),
            HtmlNodeFilterTransform.exclude(new OrFilter(excludeNodes)) //, xformAllTags
            ));
    
    Reader reader = FilterUtil.getReader(interStream, encoding);
    return new ReaderInputStream(new WhiteSpaceFilter(reader));
  }

}
