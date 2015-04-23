/*
 * $Id:$
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

package org.lockss.plugin.pub2web.asm;

import java.io.InputStream;
import org.htmlparser.NodeFilter;
import org.htmlparser.filters.*;
import org.lockss.daemon.PluginException;
import org.lockss.filter.html.*;
import org.lockss.plugin.*;

public class AsmHtmlCrawlFilterFactory implements FilterFactory {
  protected static NodeFilter[] filters = new NodeFilter[] {
    
    HtmlNodeFilters.tagWithAttribute("div", "id", "related"),
    // prev article, toc, next article links - just in case of overcrawl
    HtmlNodeFilters.tagWithAttribute("li",  "class", "previousLinkContainer"),
    HtmlNodeFilters.tagWithAttribute("li",  "class", "indexLinkContainer"),
    HtmlNodeFilters.tagWithAttribute("li",  "class", "nextLinkContainer"),

    // they don't seem internal, but just to be safe, don't crawl links within reference containers
    HtmlNodeFilters.tagWithAttribute("div",  "class", "refcontainer"),
    
    // shows earlier or later versions of the same article - see
    // content/journal/ecosalplus/10.1128/(ecosalplus.5.5 & ecosalplus.ESP-0002-2013) 
    HtmlNodeFilters.tagWithAttribute("div",  "class", "consanguinityContainer"),
    
    //agh. In-line references to other articles from this journal
    // content/journal/ecosalplus/10.1128/ecosalplus.ESP-0005-2013 - full text version
    HtmlNodeFilters.tagWithAttribute("a", "target", "xrefwindow"),
    
    //don't pick up cover images for books in "ASM recommends" and "Customers also bought"
    //at the bottom of book landing page
    HtmlNodeFilters.tagWithAttribute("div", "class", "crossSelling"),
    // similar for a journal article landing page
    HtmlNodeFilters.tagWithAttribute("div", "id", "related"),
    
  };

  @Override
  public InputStream createFilteredInputStream(ArchivalUnit au,
      InputStream in, String encoding) throws PluginException{

    return new HtmlFilterInputStream(in, encoding,
        HtmlNodeFilterTransform.exclude(new OrFilter(filters)));
  }
  
}
