/*
 * $Id$
 */

/*

Copyright (c) 2000-2017 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.plugin.atypon.asco;

import java.io.InputStream;

import org.htmlparser.NodeFilter;
import org.lockss.filter.html.HtmlNodeFilters;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.plugin.atypon.BaseAtyponHtmlHashFilterFactory;
import org.lockss.util.Logger;

public class AscoHtmlHashFilterFactory 
  extends BaseAtyponHtmlHashFilterFactory {

  private static final Logger log = Logger.getLogger(AscoHtmlHashFilterFactory.class);
  
  @Override
  public InputStream createFilteredInputStream(ArchivalUnit au,
                                               InputStream in,
                                               String encoding) {
    NodeFilter[] filters = new NodeFilter[] {
        // handled by parent: script, sfxlink, stylesheet

        HtmlNodeFilters.tag("style"),
        
        // page header: login, register, etc., and journal menu such as
        // subscribe, alerts, ...
        // subscribe, alerts, ...
        HtmlNodeFilters.tagWithAttribute("header", "class", "page-header"),
        // page footer
        HtmlNodeFilters.tagWithAttributeRegex("div", "class", "pageFooter"),
        
        // toc - middle column; right column is literatumAd
        HtmlNodeFilters.tagWithAttributeRegex("div", "class", "toc-tools"),
        // article right column; right column is literatumAd
        HtmlNodeFilters.tagWithAttributeRegex("div", "class", "article-tools"),
        
        HtmlNodeFilters.tagWithAttribute("div", "class", "sectionJumpTo"),
        
        //showCitationsPage just verify the articleList info
        HtmlNodeFilters.allExceptSubtree(
            HtmlNodeFilters.tagWithAttributeRegex(
                "div", "class", "downloadCitationsWidget"),
                HtmlNodeFilters.tagWithAttribute(
                    "div", "class", "articleList")),        
        
        // toc - article type seems to change and this isn't important
        HtmlNodeFilters.tagWithAttribute("span", "class", "ArticleType"),
        // on full text and referenes page the ways to linkout to the reference get                                                                                                                   
        // added to (GoogleScholar, Medline, ISI, abstract, etc)                                                                                                                                      
        // leave the content (NLM_article-title, NLM_year, etc),                                                                                                                                      
        // but remove everything else (links and punctuation between options)  
        HtmlNodeFilters.allExceptSubtree(
            HtmlNodeFilters.tagWithAttribute(
                "table", "class", "references"),
                HtmlNodeFilters.tagWithAttributeRegex(
                    "span", "class", "NLM_")),
 
    };
    // super.createFilteredInputStream adds bir filter to the baseAtyponFilters
    // and returns the filtered input stream using an array of NodeFilters that 
    // combine the two arrays of NodeFilters.
    return super.createFilteredInputStream(au, in, encoding, filters);
  }

  
  public boolean doTagRemovalFiltering() {
    return true;
  }
   
  @Override
  public boolean doWSFiltering() {
    return true;
  }

}




