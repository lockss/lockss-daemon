/*
 * $Id$
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

package org.lockss.plugin.atypon.seg;

import java.io.InputStream;

import org.htmlparser.NodeFilter;
import org.lockss.filter.html.*;
import org.lockss.plugin.*;
import org.lockss.plugin.atypon.BaseAtyponHtmlHashFilterFactory;

public class SEGHtmlHashFilterFactory extends BaseAtyponHtmlHashFilterFactory {

  @Override
  public InputStream createFilteredInputStream(ArchivalUnit au,
      InputStream in, String encoding) {
    
    NodeFilter[] segFilters = new NodeFilter[] {
        // header filtered in BaseAtypon
        
        // top right of issue toc - links to previous or next issue
	HtmlNodeFilters.tagWithAttribute("div", "id", "prevNextNav"),
	
	// top right of an article - links to Previous or Next Article,
	// and Cited By
        HtmlNodeFilters.tagWithAttributeRegex(
            "div", "class", "type-publication-tools"),
       
        // below type-publication-tools of an article, where ads are inserted    
        HtmlNodeFilters.tagWithAttributeRegex(
            "div", "class", "type-ad-placeholder"),
            
        // from toc page - Cited count
        HtmlNodeFilters.tagWithAttribute(
            "div", "class", "citation tocCitation"),
        
        // left column - all except Download Citations
        HtmlNodeFilters.allExceptSubtree(
            HtmlNodeFilters.tagWithAttributeRegex(
                "div", "class", "leftColumn"),
            HtmlNodeFilters.tagWithAttributeRegex(
                "a", "href", "/action/showCitFormats\\?")),                           
                	    
	// right column ads - <div class="mainAd">
        HtmlNodeFilters.tagWithAttributeRegex("div", "class", "mainAd"),
        
        // above footer - click to increase image size
        // http://library.seg.org/doi/abs/10.1190/2013-0823-SPSEINTRO.1
        HtmlNodeFilters.tagWithAttributeRegex("div", "id", "firstPage"),

	// footer filtered in BaseAtypon
    };
    
    // super.createFilteredInputStream adds segFilters to the baseAtyponFilters
    // and returns the filtered input stream using an array of NodeFilters that 
    // combine the two arrays of NodeFilters.
    return super.createFilteredInputStream(au, in, encoding, segFilters);
    }
  
  @Override
  public boolean doWSFiltering() {
    return true;
  }
    
}
