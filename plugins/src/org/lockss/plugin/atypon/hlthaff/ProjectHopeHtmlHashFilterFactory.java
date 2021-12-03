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

package org.lockss.plugin.atypon.hlthaff;

import java.io.InputStream;
import java.util.Vector;

import org.htmlparser.*;
import org.htmlparser.tags.*;
import org.lockss.filter.html.HtmlNodeFilters;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.plugin.atypon.BaseAtyponHtmlHashFilterFactory;

// Keeps contents only (includeNodes), then hashes out unwanted nodes 
// within the content (excludeNodes).
public class ProjectHopeHtmlHashFilterFactory 
  extends BaseAtyponHtmlHashFilterFactory  {
  
  @Override
  public InputStream createFilteredInputStream(ArchivalUnit au,
                                               InputStream in, 
                                               String encoding) {
    NodeFilter[] includeNodes = new NodeFilter[] {
        // manifest pages need to include something
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
        // abs, ref - contents only
        HtmlNodeFilters.tagWithAttributeRegex("div", "class", "article__content"),
        // Citation
        HtmlNodeFilters.tagWithAttributeRegex("div", "class", "articleList"),
        
    };
    
    // handled by parent: script, sfxlink, stylesheet, pdfplus file size
    // <head> tag, <li> item has the text "Cited by", accessIcon, 
    NodeFilter[] excludeNodes = new NodeFilter[] {
        // on article page <div class="article__breadcrumbs">
        HtmlNodeFilters.tagWithAttributeRegex("div", "class", "scroll-to-target"),
        HtmlNodeFilters.tagWithAttributeRegex("div", "class", "article__breadcrumbs"),
        // References
        HtmlNodeFilters.tagWithAttributeRegex("li", "class", "references__item"),
    };
    return super.createFilteredInputStream(au, in, encoding, 
                                           includeNodes, excludeNodes);
  }
  
  @Override
  public boolean doTagRemovalFiltering() {
    return true;
  }
  
  @Override
  public boolean doWSFiltering() {
    return true;
  }
  
}
