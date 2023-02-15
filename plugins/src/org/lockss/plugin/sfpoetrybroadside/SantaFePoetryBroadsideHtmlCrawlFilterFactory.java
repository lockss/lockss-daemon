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

package org.lockss.plugin.sfpoetrybroadside;

import java.io.InputStream;

import org.htmlparser.Node;
import org.htmlparser.NodeFilter;
import org.htmlparser.Tag;
import org.htmlparser.filters.*;
import org.htmlparser.nodes.TextNode;
import org.htmlparser.tags.BodyTag;
import org.htmlparser.tags.CompositeTag;
import org.htmlparser.tags.HeadTag;
import org.htmlparser.util.SimpleNodeIterator;
import org.lockss.daemon.PluginException;
import org.lockss.filter.html.*;
import org.lockss.plugin.*;

public class SantaFePoetryBroadsideHtmlCrawlFilterFactory implements FilterFactory {

  public InputStream createFilteredInputStream(ArchivalUnit au,
                                               InputStream in,
                                               String encoding)
      throws PluginException {
    NodeFilter[] filters = new NodeFilter[] {
        /*
         * In early issues (up to #35) no clear div for bio chunks with links
         * but all bio pages (except for issue2 which has no outside issue links) have
         * the same page title so crawl filter out the body section of bio pages
         */
        new NodeFilter() {
          @Override public boolean accept(Node node) {
            if (!(node instanceof BodyTag)) return false;
            Node prevNode = node.getPreviousSibling();
            while (prevNode != null && !(prevNode instanceof HeadTag)) {
              prevNode = prevNode.getPreviousSibling();
            }
            if (prevNode != null && prevNode instanceof HeadTag) {
              for (SimpleNodeIterator iter = ((CompositeTag)prevNode).elements() ; iter.hasMoreNodes() ; ) {
                Node n = iter.nextNode();
                if (n instanceof TextNode) { continue; }
                String tagName = ((Tag)n).getTagName().toLowerCase();
                if ( ("title".equals(tagName))  && (((Tag)n).toPlainTextString()).contains("Bionotes")) {
                  return true; 
                }
              }
            }
            return false;
          }
        },
        /*
         * issue #35 and later there is no <html> start tag on pages so the DOM parsing no longer works, 
         * but the bio pages begin to use a <div class="bio2"> structure around bio page links and we can 
         * remove those. Do it in this order so we don't waste time if we're just ripping out the whole body section
         */
        HtmlNodeFilters.tagWithAttribute("div", "class", "bio2"),
    };
    return new HtmlFilterInputStream(in,
        encoding,
        HtmlNodeFilterTransform.exclude(new OrFilter(filters)));
  }

}
