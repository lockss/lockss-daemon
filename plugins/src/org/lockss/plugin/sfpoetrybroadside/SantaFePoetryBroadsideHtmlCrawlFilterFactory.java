/*
 * $Id$
 */

/*

Copyright (c) 2000-2013 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.plugin.sfpoetrybroadside;

import java.io.InputStream;

import org.htmlparser.Node;
import org.htmlparser.NodeFilter;
import org.htmlparser.Tag;
import org.htmlparser.filters.*;
import org.htmlparser.nodes.TextNode;
import org.htmlparser.tags.BodyTag;
import org.htmlparser.tags.CompositeTag;
import org.htmlparser.tags.Div;
import org.htmlparser.tags.HeadTag;
import org.htmlparser.tags.HeadingTag;
import org.htmlparser.tags.LinkTag;
import org.htmlparser.util.NodeList;
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
