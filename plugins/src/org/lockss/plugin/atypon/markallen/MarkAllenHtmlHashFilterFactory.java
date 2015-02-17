/*
 * $Id: MarkAllenHtmlHashFilterFactory.java,v 1.5 2015-01-16 19:31:03 ldoan Exp $
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

package org.lockss.plugin.atypon.markallen;

import java.io.InputStream;

import org.htmlparser.NodeFilter;
import org.htmlparser.filters.TagNameFilter;
import org.lockss.filter.html.*;
import org.lockss.plugin.*;
import org.lockss.plugin.atypon.BaseAtyponHtmlHashFilterFactory;

public class MarkAllenHtmlHashFilterFactory 
  extends BaseAtyponHtmlHashFilterFactory {

  @Override
  public InputStream createFilteredInputStream(ArchivalUnit au,
      InputStream in, String encoding) {
    
    NodeFilter[] filters = new NodeFilter[] {
        // handled by parent: script, sfxlink, stylesheet
        
        // this is controversial - draconian; what about updated metadata
        new TagNameFilter("head"),
        new TagNameFilter("noscript"),

        // from toc - institution banner
        // http://www.magonlinelibrary.com/doi/ref/10.12968/bjom.2013.21.10.701
        HtmlNodeFilters.tagWithAttributeRegex("div", "class", 
                                              "literatumInstitutionBanner"),
        
        // from toc - top page ad and all other ads with class LiteratumAd
        HtmlNodeFilters.tagWithAttributeRegex("div", "class", "literatumAd"),
        
        // from toc - pageHeader - has links to current issue
        HtmlNodeFilters.tagWithAttributeRegex("div", "id", "pageHeader"),
        
        // from toc - ad panel has link to other issue 
        // http://www.magonlinelibrary.com/toc/bjom/21/10
        HtmlNodeFilters.tagWithAttributeRegex("div", "class", 
                                              "genericSlideshow"),
            
        // for toc - social media
        HtmlNodeFilters.tagWithAttributeRegex("div", "class",
                                              "general-bookmark-share"),
            
        // from toc - access icon container 
        HtmlNodeFilters.tagWithAttribute("td", "class", "accessIconContainer"),      
	           
        // middle column ad of an article - all article tools with 
        // class literatumArticleToolsWidget except Download Citations
        // http://www.magonlinelibrary.com/doi/abs/10.12968/bjom.2013.21.10.701
        HtmlNodeFilters.allExceptSubtree(
            HtmlNodeFilters.tagWithAttributeRegex( 
                "div", "class", "literatumArticleToolsWidget"),
                HtmlNodeFilters.tagWithAttributeRegex(
                    "a", "href", "/action/showCitFormats\\?")),   
                    
        // from full text - Downloaded count
        // http://www.magonlinelibrary.com/doi/full/10.12968/bjom.2013.21.10.692            
        HtmlNodeFilters.tagWithAttributeRegex("div", "class",
                                              "literatumContentItemDownloadCount"),            
                    
        // toc, abs, full, text and ref right column - most read 
        // http://www.magonlinelibrary.com/doi/full/10.12968/bjom.2013.21.10.688
        HtmlNodeFilters.tagWithAttributeRegex("div", "class", 
                                              "literatumMostReadWidget"),
        
        // pageFooter
        HtmlNodeFilters.tagWithAttribute("div", "id", "pageFooter"),
        
    };
    
    // super.createFilteredInputStream adds filters to the baseAtyponFilters
    // and returns the filtered input stream using an array of NodeFilters that 
    // combine the two arrays of NodeFilters.
    return super.createFilteredInputStream(au, in, encoding, filters);
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
