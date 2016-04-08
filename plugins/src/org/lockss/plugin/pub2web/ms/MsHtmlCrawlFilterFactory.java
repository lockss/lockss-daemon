/*
 * $Id:$
 */

/*

Copyright (c) 2000-2016 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.plugin.pub2web.ms;

import java.io.InputStream;
import org.htmlparser.NodeFilter;
import org.htmlparser.filters.*;
import org.lockss.daemon.PluginException;
import org.lockss.filter.html.*;
import org.lockss.plugin.*;

/* 
 * don't do include/exclude for crawl filter - too hard to get all the supporting
 * files.
 * It's okay to exclude the "aside" tag which contains the right column
 * We will pick up the export citation options from the TOC article listings
 * ....renderList?items= URLs
 */
public class MsHtmlCrawlFilterFactory implements FilterFactory {
  

  protected static NodeFilter[] xfilters = new NodeFilter[] {
    
    // Get rid of the big chunks
    HtmlNodeFilters.tag("header"),
    HtmlNodeFilters.tag("footer"),
    // right column and metrics, etc
    // citations will come off article listing portions of TOC
    //NOTE: cannot crawl filter all of "aside" or the toc implementation will not work 
    //HtmlNodeFilters.tag("aside"),
    
    // We have the main container, now start taking bits of that out
    HtmlNodeFilters.tagWithAttribute("ol",  "class", "breadcrumb"),
    HtmlNodeFilters.tagWithAttributeRegex("a",  "class", "banner-container journal-banner"),
    HtmlNodeFilters.tagWithAttribute("nav",  "class", "pillscontainer"),
    //remove the TOC navigation links except the full TOC pdf
    
    //remove the stuff on the right column, except the citation export
    HtmlNodeFilters.allExceptSubtree(
        HtmlNodeFilters.tagWithAttributeRegex("div","id", "tools-nav"), 
        HtmlNodeFilters.tagWithAttribute("ul","id", "export-list")),

    //remove article landing page navigation links
    HtmlNodeFilters.tagWithAttribute("li",  "class", "previousLinkContainer"),
    HtmlNodeFilters.tagWithAttribute("li",  "class", "indexLinkContainer"),
    HtmlNodeFilters.tagWithAttribute("li",  "class", "nextLinkContainer"),

    // in-line ref link
    HtmlNodeFilters.tagWithAttribute("span",  "class", "xref"),
    // reference section in full-text html, do both to be extra safe
    HtmlNodeFilters.tagWithAttribute("span",  "class", "references"),
    HtmlNodeFilters.tagWithAttribute("ol",  "class", "references"),
    
    //TODO: The following are from ASM examples, look for Ms equivalents
    // shows earlier or later versions of the same article - see
    // content/journal/ecosalplus/10.1128/(ecosalplus.5.5 & ecosalplus.ESP-0002-2013) 
    //HtmlNodeFilters.tagWithAttribute("div",  "class", "consanguinityContainer"),   
    //agh. In-line references to other articles from this journal
    // content/journal/ecosalplus/10.1128/ecosalplus.ESP-0005-2013 - full text version
    //HtmlNodeFilters.tagWithAttribute("a", "target", "xrefwindow"),
    //don't pick up cover images for books in "ASM recommends" and "Customers also bought"
    //at the bottom of book landing page
    //HtmlNodeFilters.tagWithAttribute("div", "class", "crossSelling"),
    // similar for a journal article landing page
    //HtmlNodeFilters.tagWithAttribute("div", "id", "related"),
    
  };

  @Override
  public InputStream createFilteredInputStream(ArchivalUnit au,
      InputStream in, String encoding) throws PluginException {
    
 
    /* do the usual - just exclude */
    HtmlTransform transform = HtmlNodeFilterTransform.exclude(new OrFilter(xfilters));
    HtmlFilterInputStream fstream = new HtmlFilterInputStream(in,
        encoding,
        transform);
    return fstream;
 
  }
  
}
