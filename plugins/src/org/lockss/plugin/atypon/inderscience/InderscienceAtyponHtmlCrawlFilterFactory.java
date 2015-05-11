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

package org.lockss.plugin.atypon.inderscience;

import java.io.InputStream;

import org.htmlparser.NodeFilter;
import org.lockss.daemon.PluginException;
import org.lockss.filter.html.*;
import org.lockss.plugin.*;
import org.lockss.plugin.atypon.BaseAtyponHtmlCrawlFilterFactory;

public class InderscienceAtyponHtmlCrawlFilterFactory 
  extends BaseAtyponHtmlCrawlFilterFactory {
  
  static NodeFilter[] filters = new NodeFilter[] {
    // handled by parent:
    // toc previous/next issue and article - <td class="journalNavRightTd">
    
    // toc, full, abs, ref - breadcrumbs
    // http://www.inderscienceonline.com/doi/abs/10.1504/AJAAF.2014.065176
    HtmlNodeFilters.tagWithAttribute("ul", "class", "breadcrumbs"),  
    // toc, full, abs, ref - panel under breadcrumbs with link to
    // Current Issue, or right sidebar top block of abstract, full text and ref
    // http://www.inderscienceonline.com/toc/ajaaf/3/1
    // http://www.inderscienceonline.com/doi/abs/10.1504/AJAAF.2014.065176
    HtmlNodeFilters.tagWithAttributeRegex("div", "class", "body-emphasis"),
    // toc, full, abs, ref - right column most read/most cited
    // http://www.inderscienceonline.com/doi/abs/10.1504/AJAAF.2014.065176
    // http://www.inderscienceonline.com/doi/full/10.1504/AJAAF.2014.065176
    HtmlNodeFilters.tagWithAttribute("div", "aria-relevant", "additions"),
    // full, abs, ref - below <div class="response">, or after the main content
    HtmlNodeFilters.tagWithAttribute("div", "id", "relatedContent"),
    // abs, full, ref - all right column except Citation Mgr (download citations)
    // http://www.inderscienceonline.com/doi/full/10.1504/AJAAF.2014.065176                                      
    HtmlNodeFilters.allExceptSubtree(
        HtmlNodeFilters.tagWithAttributeRegex("div", "class", "articleTools"),
          HtmlNodeFilters.tagWithAttributeRegex(
                 "a", "href", "/action/showCitFormats\\?")) 
  };

  @Override
  public InputStream createFilteredInputStream(ArchivalUnit au,
      InputStream in, String encoding) throws PluginException{
    return super.createFilteredInputStream(au, in, encoding, filters);
  }
  
}
