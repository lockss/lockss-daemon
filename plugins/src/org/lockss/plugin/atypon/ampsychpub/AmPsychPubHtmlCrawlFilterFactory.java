/*

Copyright (c) 2000-2022, Board of Trustees of Leland Stanford Jr. University

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

