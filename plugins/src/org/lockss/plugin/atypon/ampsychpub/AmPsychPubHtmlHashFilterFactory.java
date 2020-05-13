/*
 * $Id$
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

