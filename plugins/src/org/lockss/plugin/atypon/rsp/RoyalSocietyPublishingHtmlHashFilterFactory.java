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

package org.lockss.plugin.atypon.rsp;

import org.htmlparser.Node;
import org.htmlparser.NodeFilter;
import org.htmlparser.tags.Bullet;
import org.htmlparser.tags.BulletList;
import org.lockss.filter.html.HtmlNodeFilters;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.plugin.atypon.BaseAtyponHtmlHashFilterFactory;

import java.io.InputStream;
import java.util.Vector;

public class RoyalSocietyPublishingHtmlHashFilterFactory extends BaseAtyponHtmlHashFilterFactory {

  @Override
  public InputStream createFilteredInputStream(ArchivalUnit au,
                                               InputStream in,
                                               String encoding) {
    NodeFilter[] includeNodes = new NodeFilter[] {
            // Parsing the manifest page and get the list of publications in standard manifest page format.
            // Often times it is in html bullet list format
            new NodeFilter() {
              @Override
              public boolean accept(Node node) {
                if (HtmlNodeFilters.tagWithAttributeRegex("a", "href",
                        "/toc/").accept(node)) {
                  Node liParent = node.getParent();
                  if (liParent instanceof Bullet) {
                    Bullet li = (Bullet)liParent;
                    Vector liAttr = li.getAttributesEx();
                    if (liAttr != null && liAttr.size() == 1) {
                      Node ulParent = li.getParent();
                      if (ulParent instanceof BulletList) {
                        BulletList ul = (BulletList)ulParent;
                        Vector ulAttr = ul.getAttributesEx();
                        return ulAttr != null && ulAttr.size() == 1;
                      }
                    }
                  }
                }
                return false;
              }
            },
            // toc - contents only
            HtmlNodeFilters.tagWithAttributeRegex("div", "class", "table-of-content"),
            // doi full/abs/reference content
            HtmlNodeFilters.tagWithAttributeRegex("div", "class", "article__body"),
            // Download Citations page
            HtmlNodeFilters.tagWithAttribute("div", "class", "articleList"),
    };

    NodeFilter[] excludeNodes = new NodeFilter[] {
            // Remove from toc page
            HtmlNodeFilters.tagWithAttributeRegex("div", "class", "publication-header"),
            HtmlNodeFilters.tagWithAttributeRegex("div", "class", "navigation-column"),
            HtmlNodeFilters.tagWithAttributeRegex("div", "class", "toc-right-side"),
            HtmlNodeFilters.tagWithAttributeRegex("div", "class", "content-navigation"),


            // Remove from doi/full page
            HtmlNodeFilters.tagWithAttributeRegex("div", "class", "article-row-right")
    };
    return super.createFilteredInputStream(au, in, encoding,
            includeNodes, excludeNodes);
  }

  @Override
  public boolean doTagIDFiltering() {
    return false;
  }

  @Override
  public boolean doWSFiltering() {
    return true;
  }

  /* removes tags and comments after other processing */
  @Override
  public boolean doTagRemovalFiltering() {
    return true;
  }

  @Override
  public boolean doHttpsConversion() {
    return false;
  }

}