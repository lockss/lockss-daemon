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

package org.lockss.plugin.atypon.arrs;

import java.io.InputStream;

import org.htmlparser.NodeFilter;
import org.lockss.filter.html.*;
import org.lockss.plugin.*;
import org.lockss.plugin.atypon.BaseAtyponHtmlHashFilterFactory;

public class ARRSHtmlHashFilterFactory 
  extends BaseAtyponHtmlHashFilterFactory {

  @Override
  public InputStream createFilteredInputStream(ArchivalUnit au,
      InputStream in, String encoding) {
    
    NodeFilter[] filters = new NodeFilter[] {
        
        // Covered by BaseAtypon:
        // header - <div id="header">
        // footer - <div id="footer">
        // Open URL sfxLink
        
        // from toc - accessIcon
        // http://www.ajronline.org/toc/ajr/201/6
        HtmlNodeFilters.tagWithAttributeRegex("img", "class", "accessIcon"),
        
        // from toc - credit icon
        // http://www.ajronline.org/toc/ajr/201/6
        HtmlNodeFilters.tagWithAttributeRegex("img", "class", "CMESAM"),
        
        // from toc, abs, full, suppl - whole left sidebar
	// http://www.ajronline.org/doi/full/10.2214/AJR.12.10221
        HtmlNodeFilters.tagWithAttributeRegex("div", "id", 
                                              "dropzone-Left-Sidebar"),  
                                              
        // from abs, full - Previous Article|Next Article
        // http://www.ajronline.org/doi/abs/10.2214/AJR.12.10039
        HtmlNodeFilters.tagWithAttributeRegex("div", "id", "articleToolsNav"),   

	// from abs, full - Recommended Articles
	// http://www.ajronline.org/doi/full/10.2214/AJR.12.9120
	HtmlNodeFilters.tagWithAttributeRegex("div", "class", 
	                                      "type-recommendedArticles"),  

	// from abs - share/email button below article title
	// http://www.ajronline.org/doi/full/10.2214/AJR.12.10221
	HtmlNodeFilters.tagWithAttributeRegex("div", "class", 
	                                      "articleAdditionalLinks"), 
	                                      
	// from abs, full - 'Choose' pulldown near References section
	// some page collected with 'CITING ARTICLES', some without
	// http://www.ajronline.org/doi/full/10.2214/AJR.12.9121                                     
	HtmlNodeFilters.tagWithAttribute("table", "class", "sectionHeading"),                                       
               
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
