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

package org.lockss.plugin.atypon.endocrinesociety;

import java.io.InputStream;
import java.util.regex.Pattern;
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
        // handled by parent: script, sfxlink, stylesheet, pdfplus file sise
        // <head> tag, <li> item has the text "Cited by"
        
        HtmlNodeFilters.tag("noscript"),
        
        // pageHeader
        HtmlNodeFilters.tagWithAttribute("div", "id", "pageHeader"),
        // pageFooter
        HtmlNodeFilters.tagWithAttribute("div", "id", "pageFooter"),
        // top panel with 'subscribe'
        // http://press.endocrine.org/doi/full/10.1210/en.2012-2147
        HtmlNodeFilters.tagWithAttributeRegex("div", "class", "gutterless"),
        // nav journal - current past issues, about, authors
        // http://press.endocrine.org/doi/full/10.1210/en.2012-2147
        HtmlNodeFilters.tagWithAttributeRegex("div", "class", "nav-journal"),
        // toc - breadcrumbs
        // <div class="widget literatumBreadcrumbs
        HtmlNodeFilters.tagWithAttributeRegex("div", "class", 
                                              "literatumBreadcrumbs"),
        // from issue toc or article: previous issue / next issue
        HtmlNodeFilters.tagWithAttributeRegex("div", "class", 
                                              "literatumBookIssueNavigation"),
        // toc - free.gif image
        HtmlNodeFilters.tagWithAttributeRegex("img",  "src", "free.gif"),
        // toc - access icon container
        HtmlNodeFilters.tagWithAttribute("td", "class", "accessIconContainer"),
        // toc - this seems unused but may get turned on
        // http://press.endocrine.org/doi/full/10.1210/er.2013-1012
        HtmlNodeFilters.tagWithAttribute("div",  "id", "MathJax_Message"),
        // full - section choose pulldown appeared in multiple sections
        // http://press.endocrine.org/doi/full/10.1210/er.2013-1012
        HtmlNodeFilters.tagWithAttribute("div",  "class", "sectionJumpTo"),
        // showCitFormats - Support and Help block
        // http://press.endocrine.org/action/showCitFormats?
        // doi=10.1210%2Fjc.2013-1811
        HtmlNodeFilters.tagWithAttributeRegex("div",  "class", 
                                              "twoColumnRightDropZoneColor"),
        // right column of an article - all except Download Citations
        // note: institution banner is inside sidebar-right
        HtmlNodeFilters.allExceptSubtree(
            HtmlNodeFilters.tagWithAttributeRegex( 
                "div", "class", "literatumRightSidebar"),
                HtmlNodeFilters.tagWithAttributeRegex(
                    "a", "href", "/action/showCitFormats\\?"))                       
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
