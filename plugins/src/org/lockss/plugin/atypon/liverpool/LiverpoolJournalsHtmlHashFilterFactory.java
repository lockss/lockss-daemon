/*
 * $Id$
 */

/*

Copyright (c) 2000-2015 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.plugin.atypon.liverpool;

import java.io.InputStream;
import java.util.Vector;

import org.htmlparser.*;
import org.htmlparser.tags.*;
import org.lockss.filter.html.HtmlNodeFilters;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.plugin.atypon.BaseAtyponHtmlHashFilterFactory;

// Keeps contents only (includeNodes), then hashes out unwanted nodes 
// within the content (excludeNodes).
public class LiverpoolJournalsHtmlHashFilterFactory 
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
                                                                                                                
    };
    
    // handled by parent: script, sfxlink, stylesheet, pdfplus file sise
    // <head> tag, <li> item has the text "Cited by", accessIcon, 
    NodeFilter[] excludeNodes = new NodeFilter[] {       
        // toc - select pulldown menu under volume title
        HtmlNodeFilters.tagWithAttributeRegex("div", "class",
                                              "publicationToolContainer"),
        // abs - scattering - potentially generated code added (like
        // Endocrine Society)                                      
        // http://online.liverpooluniversitypress.co.uk/doi/abs/10.3828/bjcs.2013.5
        HtmlNodeFilters.tagWithAttributeRegex("div", "class", 
                                              "articleMetaDrop"),            
    
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
