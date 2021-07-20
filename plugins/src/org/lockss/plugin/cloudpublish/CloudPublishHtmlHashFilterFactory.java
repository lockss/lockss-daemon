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

package org.lockss.plugin.cloudpublish;

import org.htmlparser.Node;
import org.htmlparser.NodeFilter;
import org.htmlparser.tags.BodyTag;
import org.htmlparser.tags.Bullet;
import org.htmlparser.tags.BulletList;
import org.lockss.filter.html.HtmlNodeFilters;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.plugin.atypon.BaseAtyponHtmlHashFilterFactory;

import java.io.InputStream;
import java.util.Vector;

// Keeps contents only (includeNodes), then hashes out unwanted nodes 
// within the content (excludeNodes).
public class CloudPublishHtmlHashFilterFactory
  extends BaseAtyponHtmlHashFilterFactory  {
  
  @Override
  public InputStream createFilteredInputStream(ArchivalUnit au,
                                               InputStream in, 
                                               String encoding) {
    NodeFilter[] includeNodes = new NodeFilter[] {
        // ?? review when manifest pages up
        // manifest pages
        // <ul> and <li> without attributes (unlike TOC/abs/ref breadcrumbs)
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
            } else if (HtmlNodeFilters.tagWithAttributeRegex("a", "href", 
                "/doi/book/").accept(node)) {
             // book manifest page has single doi/book ref whose parent is just the <body> element
             // http://emeraldinsight.com/clockss/eisbn/9780080549910
               Node liParent = node.getParent();
               if (liParent instanceof BodyTag) {
                 return true;
               } 
            }
            return false;
          }
        },
        // toc - contents only
        // http://online.liverpooluniversitypress.co.uk/toc/bjcs/26/1
        HtmlNodeFilters.tagWithAttributeRegex("div", "class", "tocListWidget"),
        // abs, ref - contents only
        // http://online.liverpooluniversitypress.co.uk/doi/abs/10.3828/bjcs.2013.2
        // http://online.liverpooluniversitypress.co.uk/doi/ref/10.3828/bjcs.2013.3
        HtmlNodeFilters.tagWithAttributeRegex("div", "class", 
                                          "literatumPublicationContentWidget"),
        // showCitFormats
        // http://online.liverpooluniversitypress.co.uk/action/
        //                           showCitFormats?doi=10.3828%2Fbjcs.2013.3 
        HtmlNodeFilters.tagWithAttributeRegex("section", "class", 
                                              "downloadCitationsWidget"),
        // early 2017- changed to <div class 
        HtmlNodeFilters.tagWithAttributeRegex("div", "class", 
                                              "downloadCitationsWidget"), 
    };
    
    // handled by parent: script, sfxlink, stylesheet, pdfplus file sise
    // <head> tag, <li> item has the text "Cited by", accessIcon, 
    NodeFilter[] excludeNodes = new NodeFilter[] { 
      // All exclude filters are in the parent
    };
    return super.createFilteredInputStream(au, in, encoding, 
                                           includeNodes, excludeNodes);
  }

  @Override
  public boolean doTagIDFiltering() {
    return true;
  }
  
  @Override
  public boolean doWSFiltering() {
    return true;
  }
  
}
