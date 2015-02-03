/*
 * $Id: EndocrineSocietyHtmlHashFilterFactory.java,v 1.5 2015-02-03 21:38:21 ldoan Exp $
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

package org.lockss.plugin.atypon.endocrinesociety;

import java.io.InputStream;

import org.htmlparser.NodeFilter;
import org.lockss.filter.html.*;
import org.lockss.plugin.*;
import org.lockss.plugin.atypon.BaseAtyponHtmlHashFilterFactory;

public class EndocrineSocietyHtmlHashFilterFactory 
  extends BaseAtyponHtmlHashFilterFactory {

  @Override
  public InputStream createFilteredInputStream(ArchivalUnit au,
      InputStream in, String encoding) {
    
    NodeFilter[] filters = new NodeFilter[] {
        
        // covered by BaseAtypon = next/previous
        //<td class="journalNavLeftTd"> - 
        // <td class="journalNavRightTd"> - 
        
        // pageHeader
        HtmlNodeFilters.tagWithAttribute("section", "id", "pageHeader"),
  
        // nav journal - current past issues, about, authors
        // http://press.endocrine.org/doi/full/10.1210/en.2012-2147
        HtmlNodeFilters.tagWithAttributeRegex("section", "class", 
            "nav-journal"),
        
        // top panel with 'subscribe'
        // http://press.endocrine.org/doi/full/10.1210/en.2012-2147
        HtmlNodeFilters.tagWithAttributeRegex("div", "class", "gutterless"),
        
        // right column of an article - all except Download Citations
        // note: institution banner is inside sidebar-right
        HtmlNodeFilters.allExceptSubtree(
            HtmlNodeFilters.tagWithAttributeRegex( 
                "section", "class", "literatumRightSidebar"),
                HtmlNodeFilters.tagWithAttributeRegex(
                    "a", "href", "/action/showCitFormats\\?")),     
         
        // from toc - access icon container
        // http://press.endocrine.org/toc/edrv/35/3            
        HtmlNodeFilters.tagWithAttribute("td", "class", "accessIconContainer"), 
        
        // pageFooter
        HtmlNodeFilters.tagWithAttribute("section", "id", "pageFooter"),
	
    };
    
    // super.createFilteredInputStream adds filters to the baseAtyponFilters
    // and returns the filtered input stream using an array of NodeFilters that 
    // combine the two arrays of NodeFilters.
    return super.createFilteredInputStream(au, in, encoding, filters);
    }
    
    @Override
    public boolean doWSFiltering() {
      return true;
    }
    
}
