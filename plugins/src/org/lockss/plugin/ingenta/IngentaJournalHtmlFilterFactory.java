/*
 * $Id: IngentaJournalHtmlFilterFactory.java,v 1.12 2010-09-20 22:16:51 pgust Exp $
 */ 

/*

Copyright (c) 2000-2009 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.plugin.ingenta;

import java.io.InputStream;

import org.htmlparser.NodeFilter;
import org.htmlparser.filters.OrFilter;
import org.lockss.daemon.PluginException;
import org.lockss.filter.html.*;
import org.lockss.plugin.*;

public class IngentaJournalHtmlFilterFactory implements FilterFactory {

  public InputStream createFilteredInputStream(ArchivalUnit au,
                                               InputStream in,
                                               String encoding)
      throws PluginException {
    NodeFilter[] filters = new NodeFilter[] {
        // Filter out <div id="footer">...</div>
        HtmlNodeFilters.tagWithAttribute("div", "id", "footer"),
        // Filter out <div id="top-ad-alignment">...</div>
        HtmlNodeFilters.tagWithAttribute("div", "id", "top-ad-alignment"),
        // Filter out <div id="top-ad">...</div>
        HtmlNodeFilters.tagWithAttribute("div", "id", "top-ad"),
        // Filter out <div id="ident">...</div>
        HtmlNodeFilters.tagWithAttribute("div", "id", "ident"),         
        // Filter out <div id="ad">...</div>
        HtmlNodeFilters.tagWithAttribute("div", "id", "ad"),
        // Filter out <div id="vertical-ad">...</div>
        HtmlNodeFilters.tagWithAttribute("div", "id", "vertical-ad"),
        // Filter out <div class="right-col-download">...</div>
        HtmlNodeFilters.tagWithAttribute("div", "class", "right-col-download"),                                                               
        // Filter out <div id="cart-navbar">...</div>
        HtmlNodeFilters.tagWithAttribute("div", "id", "cart-navbar"),   
//         // Filter out <div class="heading-macfix article-access-options">...</div>
//        HtmlNodeFilters.tagWithAttribute("div", "class", "heading-macfix article-access-options"),                                                                           
        // Filter out <div id="baynote-recommendations">...</div>
        HtmlNodeFilters.tagWithAttribute("div", "id", "baynote-recommendations"),
        // Filter out <div id="bookmarks-container">...</div>
        HtmlNodeFilters.tagWithAttribute("div", "id", "bookmarks-container"),   
        // Filter out <div id="llb">...</div>
        HtmlNodeFilters.tagWithAttribute("div", "id", "llb"),   
        // Filter out <a href="...">...</a> where the href value includes "exitTargetId" as a parameter
        HtmlNodeFilters.tagWithAttributeRegex("a", "href", "[\\?&]exitTargetId="),
        // Filter out <input name="exitTargetId">
        HtmlNodeFilters.tagWithAttribute("input", "name", "exitTargetId"),
    };
    
    return new HtmlFilterInputStream(in,
                                     encoding,
                                     HtmlNodeFilterTransform.exclude(new OrFilter(filters)));
  }

}
