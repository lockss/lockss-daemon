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

package org.lockss.plugin.atypon.rsna;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.Vector;

import org.apache.commons.io.IOUtils;
import org.htmlparser.*;
import org.htmlparser.tags.*;
import org.lockss.filter.html.HtmlNodeFilters;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.plugin.atypon.BaseAtyponHtmlHashFilterFactory;
import org.lockss.util.Constants;

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
        HtmlNodeFilters.tag("article"),
        HtmlNodeFilters.tagWithAttributeRegex("div", "class", 
            "literatumPublicationContentWidget"),
        // early 2017- changed to <div class
        HtmlNodeFilters.tagWithAttributeRegex("div", "class", 
            "downloadCitationsWidget"),
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
    };
    return super.createFilteredInputStream(au, in, encoding, 
                                           includeNodes, excludeNodes);
  }

  @Override
  public boolean doTagRemovalFiltering() {
    return true;
  }

  @Override
  public boolean doHttpsConversion() {
    return true;
  }

  @Override
  public boolean doWSFiltering() {
    return true;
  }
  
  public static void main(String[] args) throws Exception {
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
  }
}
