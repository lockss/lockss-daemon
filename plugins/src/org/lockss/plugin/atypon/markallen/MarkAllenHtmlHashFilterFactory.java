/*
 * $Id: MarkAllenHtmlHashFilterFactory.java,v 1.3 2014-11-07 23:03:57 ldoan Exp $
 */

/*

Copyright (c) 2000-2014 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.plugin.atypon.markallen;

import java.io.InputStream;

import org.htmlparser.NodeFilter;
import org.lockss.filter.html.*;
import org.lockss.plugin.*;
import org.lockss.plugin.atypon.BaseAtyponHtmlHashFilterFactory;

public class MarkAllenHtmlHashFilterFactory 
  extends BaseAtyponHtmlHashFilterFactory {

  @Override
  public InputStream createFilteredInputStream(ArchivalUnit au,
      InputStream in, String encoding) {
    
    NodeFilter[] filters = new NodeFilter[] {
        
        // institution banner
        // <section class="widget literatumInstitutionBanner none slogan widget-none" id="dad9d09a-eaed-4707-a2cb-23c70f92032e">
        // http://www.magonlinelibrary.com/doi/ref/10.12968/bjom.2013.21.10.701
        HtmlNodeFilters.tagWithAttributeRegex("section", "class", 
            "literatumInstitutionBanner"),
        
        // top page ad and all other ads with class LiteratumAd
        HtmlNodeFilters.tagWithAttributeRegex("section", "class", 
            "literatumAd"),
        
        // pageHeader - has links to current issue
        HtmlNodeFilters.tagWithAttribute("section", "id", "pageHeader"),
        
        // from toc - ad panel has link to other issue 
        // http://www.magonlinelibrary.com/toc/bjom/21/10
        HtmlNodeFilters.tagWithAttributeRegex("section", "class", 
            "genericSlideshow"),
            
        // for toc - social media
        HtmlNodeFilters.tagWithAttributeRegex("section", "class",
            "general-bookmark-share"),
            
        // from toc - access icon container 
        HtmlNodeFilters.tagWithAttribute("td", "class", "accessIconContainer"),      
	           
        // middle column ad of an article - all article tools with 
        // class literatumArticleToolsWidget except Download Citations
        // http://www.magonlinelibrary.com/doi/abs/10.12968/bjom.2013.21.10.701
        HtmlNodeFilters.allExceptSubtree(
            HtmlNodeFilters.tagWithAttributeRegex( 
                "section", "class", "literatumArticleToolsWidget"),
                HtmlNodeFilters.tagWithAttributeRegex(
                    "a", "href", "/action/showCitFormats\\?")),   
                    
        // from full text - Downloaded count
        // <section class="widget literatumContentItemDownloadCount alignCenter addthisborder widget-box" 
        //      id="3309a539-fe1c-4047-8e2c-68582ab6ff73">
        HtmlNodeFilters.tagWithAttributeRegex("section", "class",
            "literatumContentItemDownloadCount"),            
                    
        // toc, abs, full, text and ref right column - most read 
        // http://www.magonlinelibrary.com/doi/full/10.12968/bjom.2013.21.10.688
        HtmlNodeFilters.tagWithAttributeRegex("section", "class", 
            "layout-tabs"),
        
        // pageFooter
        HtmlNodeFilters.tagWithAttribute("section", "id", "pageFooter"),
        
    };
    
    // super.createFilteredInputStream adds filters to the baseAtyponFilters
    // and returns the filtered input stream using an array of NodeFilters that 
    // combine the two arrays of NodeFilters.
    boolean doWS = true;
    return super.createFilteredInputStream(au, in, encoding, filters, doWS);
  }
    
}
