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
import java.util.Vector;

import org.htmlparser.Node;
import org.htmlparser.NodeFilter;
import org.htmlparser.tags.Bullet;
import org.htmlparser.tags.BulletList;
import org.lockss.filter.html.HtmlNodeFilters;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.plugin.atypon.BaseAtyponHtmlHashFilterFactory;
import org.lockss.util.Logger;

// Keeps contents only (includeNodes), then hashes out unwanted nodes 
// within the content (excludeNodes).
public class AmPsychPubHtmlHashFilterFactory extends BaseAtyponHtmlHashFilterFactory {
  Logger log = Logger.getLogger(AmPsychPubHtmlHashFilterFactory.class);
  
  @Override
  public InputStream createFilteredInputStream(ArchivalUnit au,
                                               InputStream in, 
                                               String encoding) {
    NodeFilter[] includeNodes = new NodeFilter[] {
        // manifest pages need to include something
        new NodeFilter() {
          @Override
          public boolean accept(Node node) {
            if (HtmlNodeFilters.tagWithAttributeRegex("a", "href", "/toc/").accept(node)) {
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
        HtmlNodeFilters.tagWithAttributeRegex("div", "class", "tocContent"),
        // article page, references
        HtmlNodeFilters.tagWithAttributeRegex("div", "class", "literatumPublicationContentWidget"),
        // XXX possible alternative filter HtmlNodeFilters.tag("article")
        // Download Citations page
        HtmlNodeFilters.tagWithAttribute("div", "class", "articleList"),
        // showPopup&citid=citart1
        HtmlNodeFilters.tagWithAttributeRegex("body", "class", "popupBody"),
        // Starting 05/2020, the webpage no longer provide text for abstract and full text
        // Instead, it just provide an image of their PDF file. Only article title and pubdate is in text.
        // https://focus.psychiatryonline.org/doi/abs/10.1176/foc.7.4.foc475
        HtmlNodeFilters.tagWithAttributeRegex("h1", "class", "citation__title"),
        HtmlNodeFilters.tagWithAttributeRegex("div", "class", "epub-section"),
        // https://focus.psychiatryonline.org/doi/full/10.1176/foc.7.4.foc475
        HtmlNodeFilters.tagWithAttributeRegex("div", "class", "hlFld-Abstract"),
        HtmlNodeFilters.tagWithAttributeRegex("div", "class", "hlFld-Fulltext"),
        HtmlNodeFilters.tagWithAttributeRegex("div", "class", "table-of-content")
    };
    
    // handled by parent: script, sfxlink, stylesheet, pdfplus file sise
    // <head> tag, <li> item has the text "Cited by", accessIcon, 
    NodeFilter[] excludeNodes = new NodeFilter[] {
        // toc - okay so far
        // <div id="Absappips201600413" class="previewViewSection tocPreview">
        // <div class="tocDeliverFormatsLinks">
        // article page
        HtmlNodeFilters.tagWithAttributeRegex("div", "class", "(abstractKeywords|sectionJumpTo|response)"),
        HtmlNodeFilters.tagWithAttributeRegex("div", "id", "(relatedContent|cme)"),
        HtmlNodeFilters.tagWithAttributeRegex("ul", "class", "tab-nav"),
        // references page
        HtmlNodeFilters.tagWithAttribute("table", "class", "references"),
        
        };
    return super.createFilteredInputStream(au, in, encoding, includeNodes, excludeNodes);
  }

  @Override
  public boolean doTagIDFiltering() {
    return true;
  }
  
  @Override
  public boolean doWSFiltering() {
    return false;
  }
  
  /* removes tags and comments after other processing */
  @Override
  public boolean doTagRemovalFiltering() {
    return false;
  }
  
  @Override
  public boolean doHttpsConversion() {
    return false;
  }
  
}

