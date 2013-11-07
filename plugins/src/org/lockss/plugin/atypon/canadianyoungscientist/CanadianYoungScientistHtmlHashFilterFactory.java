/* 
 * $Id: CanadianYoungScientistHtmlHashFilterFactory.java,v 1.1 2013-11-07 00:57:27 ldoan Exp $ 
 */

/*

Copyright (c) 2000-2013 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.plugin.atypon.canadianyoungscientist;

import java.io.InputStream;
import org.htmlparser.NodeFilter;
import org.lockss.filter.html.*;
import org.lockss.plugin.*;
import org.lockss.plugin.atypon.BaseAtyponHtmlHashFilterFactory;
import org.lockss.util.Logger;

public class CanadianYoungScientistHtmlHashFilterFactory 
 extends BaseAtyponHtmlHashFilterFactory {
  
  protected static Logger log = Logger.getLogger(
      CanadianYoungScientistHtmlHashFilterFactory.class);
  
  @Override
  public InputStream createFilteredInputStream(ArchivalUnit au,
                                               InputStream in,
                                               String encoding) {
    NodeFilter[] cysfilters = new NodeFilter[] {
        // hash filter
        // comments, scripts, header, footer handled by parent
        // top header
        HtmlNodeFilters.tagWithAttribute("div",  "id", "top-bar-wrapper"),
        // banner 
        HtmlNodeFilters.tagWithAttribute("div",  "class", "banner"),
        HtmlNodeFilters.tagWithAttribute("div",  "id", "nav-wrapper"),
        HtmlNodeFilters.tagWithAttribute("div",  "id", "breadcrumbs"),
        // Remove link to "also read" near top main section
        HtmlNodeFilters.tagWithAttribute("a",  "class", "icon-recommended"),
        // spider link in this tag
        HtmlNodeFilters.tagWithAttribute("span", "id", "hide"),
        // hash out entire left sidebar
        HtmlNodeFilters.tagWithAttribute("div",  "id", "sidebar-left"), 
        // can't crawl filter this because it has citation download link,
        // but ok to hash
        HtmlNodeFilters.tagWithAttribute("div",  "id", "sidebar-right"), 
        
    };

    // super.createFilteredInputStream adds cys filter to the baseAtyponFilters
    // and returns the filtered input stream using an array of NodeFilters that 
    // combine the two arrays of NodeFilters.
    return super.createFilteredInputStream(au, in, encoding, cysfilters);
  }

}
