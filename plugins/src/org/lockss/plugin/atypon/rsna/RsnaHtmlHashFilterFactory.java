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

package org.lockss.plugin.atypon.rsna;

import java.io.InputStream;
import java.util.Vector;

//import java.io.FileInputStream;
//import java.io.FileOutputStream;
//import org.apache.commons.io.IOUtils;
//import org.lockss.util.Constants;

import org.htmlparser.*;
import org.htmlparser.tags.*;
import org.lockss.filter.html.HtmlNodeFilters;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.plugin.atypon.BaseAtyponHtmlHashFilterFactory;

// Keeps contents only (includeNodes), then hashes out unwanted nodes 
// within the content (excludeNodes).
public class RsnaHtmlHashFilterFactory extends BaseAtyponHtmlHashFilterFactory {
  
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
        HtmlNodeFilters.tagWithAttributeRegex("div", "class", "tocListWidget"),
        // abs, ref - contents only
        // older content had both tags, newer only article tag
        HtmlNodeFilters.allExceptSubtree(
            HtmlNodeFilters.tagWithAttributeRegex("div", "class", "literatumPublicationContentWidget"),
            HtmlNodeFilters.tag("article")),
        HtmlNodeFilters.tag("article"),
        // early 2017- changed to <div class
        HtmlNodeFilters.tagWithAttributeRegex("div", "class", "downloadCitationsWidget"),
        // http://pubs.rsna.org/action/showPopup?citid=citart1&id=eq3&doi=10.1148%2Fradiol.2016151832
        HtmlNodeFilters.tagWithAttributeRegex("body", "class", "popupBody"),
        // citation
        HtmlNodeFilters.tagWithAttributeRegex("div", "class", "articleList"),
        
    };
    
    // handled by parent: script, sfxlink, stylesheet, pdfplus file size
    // <head> tag, <li> item has the text "Cited by", accessIcon, 
    // publicationToolContainer, articleMetaDrop
    NodeFilter[] excludeNodes = new NodeFilter[] {
        // on toc - button "test SA-CME" next to each article
        HtmlNodeFilters.tagWithAttributeRegex("div", "class", "btn-holder"),
        HtmlNodeFilters.tagWithAttributeRegex("div", "class", "header"),
        HtmlNodeFilters.tagWithAttributeRegex("div", "class", "article__aside"),
        HtmlNodeFilters.tagWithAttribute("section", "class", "article__metrics"),
        HtmlNodeFilters.tag("nav"),
        HtmlNodeFilters.tagWithAttribute("ul", "class", "tab-nav"),
        HtmlNodeFilters.tagWithAttributeRegex("div", "id", "relatedContent"),
        HtmlNodeFilters.tagWithAttributeRegex("div", "class", "(articleMeta|copyright|tocHeading)"),
        HtmlNodeFilters.tagWithAttributeRegex("div", "class", "publicationContent(Authors|Doi|Licence)"),
        // for older content
        HtmlNodeFilters.tagWithAttribute("table", "class", "references"),
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
  
  /*public static void main(String[] args) throws Exception {
    String file1 = "/tmp/data/rsna1.html";
    String file2 = "/tmp/data/rsna2.html";
    String file3 = "/tmp/data/rsna3.html";
    String file4 = "/tmp/data/rsna4.html";
    IOUtils.copy(new RsnaHtmlHashFilterFactory().createFilteredInputStream(null, 
        new FileInputStream(file1), Constants.ENCODING_UTF_8), 
        new FileOutputStream(file1 + ".hout"));
    IOUtils.copy(new RsnaHtmlHashFilterFactory().createFilteredInputStream(null,
        new FileInputStream(file2), Constants.ENCODING_UTF_8),
        new FileOutputStream(file2 + ".hout"));
    IOUtils.copy(new RsnaHtmlHashFilterFactory().createFilteredInputStream(null,
        new FileInputStream(file3), Constants.ENCODING_UTF_8),
        new FileOutputStream(file3 + ".hout"));
    IOUtils.copy(new RsnaHtmlHashFilterFactory().createFilteredInputStream(null,
        new FileInputStream(file4), Constants.ENCODING_UTF_8),
        new FileOutputStream(file4 + ".hout"));
  }*/
}
