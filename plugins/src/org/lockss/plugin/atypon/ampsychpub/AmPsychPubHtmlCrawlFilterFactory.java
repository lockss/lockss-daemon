/* $Id$
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

package org.lockss.plugin.atypon.ampsychpub;

import java.io.InputStream;

import org.htmlparser.Node;
import org.htmlparser.NodeFilter;
import org.htmlparser.tags.Div;
import org.htmlparser.tags.HeadingTag;
import org.lockss.daemon.PluginException;
import org.lockss.filter.html.*;
import org.lockss.plugin.*;
import org.lockss.plugin.atypon.BaseAtyponHtmlCrawlFilterFactory;

/*
 *
 * created because article links are not grouped under a journalid or volumeid,
 * but under article ids - will pull the links from the page, so filtering out
 * extraneous links
 * 
 */
public class AmPsychPubHtmlCrawlFilterFactory extends BaseAtyponHtmlCrawlFilterFactory {
  
  NodeFilter[] filters = new NodeFilter[] {
      
      // NOTE: overcrawling is an occasional issue with in-line references to "original article"
      
      HtmlNodeFilters.tag("header"),
      HtmlNodeFilters.tag("footer"),
      
      // Article landing
      HtmlNodeFilters.tagWithAttribute("div", "id", "relatedContent"),
      HtmlNodeFilters.tagWithAttribute("div", "id", "cme"),
      // Related
      HtmlNodeFilters.tagWithAttributeRegex("div", "id", "(altmetric|trendmd)", true),
      HtmlNodeFilters.tagWithAttributeRegex("div", "class", "(altmetric|trendmd)", true),
      HtmlNodeFilters.tagWithAttributeRegex("div", "class", "bookmark"),
      // never want these links, excluded lists was too long
      HtmlNodeFilters.tagWithAttributeRegex("a", "href", "/servlet/linkout[?](suffix|type)="),
      HtmlNodeFilters.tagWithAttributeRegex("a", "href", "^/author/"),

      // Need to filter out the right side, like related article, metric and references, all those will cause over crawl
      // https://neuro.psychiatryonline.org/doi/10.1176/appi.neuropsych.15090221
      HtmlNodeFilters.tagWithAttribute("div", "id", "trendmd-suggestions"),
      HtmlNodeFilters.tagWithAttribute("section", "class", "article__metrics"),
      HtmlNodeFilters.tagWithAttribute("div", "class", "border-bottom"),
      HtmlNodeFilters.tagWithAttribute("div", "class", "body-references"),
      // Need to filter out the "prev", "next" links to the articles
      // https://ps.psychiatryonline.org/doi/full/10.1176/appi.ps.51.10.1320
      HtmlNodeFilters.tagWithAttribute("div", "class", "book-chapter__nav"),
      
      // Avoid following links in a Related Articles section
      // Some links are only differentiated by the title <h1 class="widget-header header-regular ">Related Articles</h1>
      // XXX NOTE: best if this is the last filter !!! Hopefully not too much time to run
      new NodeFilter() {
        @Override public boolean accept(Node node) {
          if (!(node instanceof Div)) return false;
          Node prevNode = node.getPreviousSibling();
          while (prevNode != null && !(prevNode instanceof HeadingTag)) {
            prevNode = prevNode.getPreviousSibling();
          }
          if (prevNode != null && prevNode instanceof HeadingTag) {
            // XXX use Regex if the heading text changes
            String heading = ((HeadingTag)prevNode).getStringText();
            return "Related Articles".equals(heading);
          }
          return false;
        }
      }
      
  };
  
  public InputStream createFilteredInputStream(ArchivalUnit au,
      InputStream in, String encoding) throws PluginException{ 
    return super.createFilteredInputStream(au, in, encoding, filters);
  }
  
}

