/*
 * $Id: $
 */

/*

Copyright (c) 2018 Board of Trustees of Leland Stanford Jr. University,
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
