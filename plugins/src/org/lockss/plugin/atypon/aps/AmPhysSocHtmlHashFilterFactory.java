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

package org.lockss.plugin.atypon.aps;

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

//Keeps contents only (includeNodes), then hashes out unwanted nodes 
//within the content (excludeNodes).
public class AmPhysSocHtmlHashFilterFactory extends BaseAtyponHtmlHashFilterFactory {
  Logger log = Logger.getLogger(AmPhysSocHtmlHashFilterFactory.class);
  
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
        HtmlNodeFilters.tagWithAttributeRegex("div", "class", "toc_content"),
        // article
        HtmlNodeFilters.tagWithAttributeRegex("div", "class", "article__content"),
        // XXX possible alternative filter HtmlNodeFilters.tag("article")
        HtmlNodeFilters.tagWithAttribute("div", "class", "article_list"),
        
    };
    
    // handled by parent: script, sfxlink, stylesheet, pdfplus file sise
    // <head> tag, <li> item has the text "Cited by", accessIcon, 
    NodeFilter[] excludeNodes = new NodeFilter[] {
        // toc - select pulldown menu under volume title
        // HtmlNodeFilters.tagWithAttributeRegex("div", "class", "publicationToolContainer"),
        // on article page
        HtmlNodeFilters.tag("nav"),
        HtmlNodeFilters.tagWithAttributeRegex("div", "class", "content.?navigation"),
        // XXX following would be needed if alternative filter HtmlNodeFilters.tag("article") is used
        // HtmlNodeFilters.tagWithAttributeRegex("div", "class", "article-row-right"),
        HtmlNodeFilters.tagWithAttribute("div", "class", "badges"),
        HtmlNodeFilters.tagWithAttribute("a", "class", "rightslink"),
        // filter all lists, aggressive filtering
        HtmlNodeFilters.tagWithAttributeRegex("ul", "class", "rlist"),
        // XXX alternatively, only reference items HtmlNodeFilters.tagWithAttributeRegex("li", "class", "references__item"),
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

