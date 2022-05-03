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

package org.lockss.plugin.ojs2;

import java.io.InputStream;

import org.htmlparser.NodeFilter;
import org.htmlparser.filters.*;
import org.lockss.daemon.PluginException;
import org.lockss.filter.html.*;
import org.lockss.plugin.*;


public class OJS2HtmlCrawlFilterFactory implements FilterFactory {

  @Override
  public InputStream createFilteredInputStream(ArchivalUnit au,
                                               InputStream in,
                                               String encoding)
      throws PluginException {
    NodeFilter[] keepers = new NodeFilter[] {
        HtmlNodeFilters.tagWithAttribute("div", "id", "content"),
        HtmlNodeFilters.tag("img"),
        HtmlNodeFilters.tagWithAttribute("div", "id", "sidebarRTArticleTools")
    };
    
    NodeFilter[] filters = new NodeFilter[] {
        
        // Some sidebars contain links to all other issue TOCs
        // e.g. http://ojs.statsbiblioteket.dk/index.php/bras/issue/view/1049
        // Keep sidebar images (important logos) and article toolbox (printer-friendly link, etc.)
        HtmlNodeFilters.allExceptSubtree(HtmlNodeFilters.tagWithAttribute("div", "id", "sidebar"),
                                         new OrFilter(keepers)),
        
        // do not get links from navbar
        HtmlNodeFilters.tagWithAttribute("div", "id", "navbar"),
        
        // nor breadcrumbs
        HtmlNodeFilters.tagWithAttribute("div", "id", "breadcrumb"),
        
        // do not get links from references section
        HtmlNodeFilters.tagWithAttribute("div", "id", "articleCitations"),
        
        // do not get links that contain https?%25...
        HtmlNodeFilters.tagWithAttributeRegex("a", "href", "https?[%]25"),
        
        //Athabasca is now putting ALL issue volumes on the manifest page in the footer
        // see http://jrp.icaap.org/index.php/jrp/gateway/lockss?year=2018
        HtmlNodeFilters.tagWithAttribute("div", "id", "pageFooter"),

        // ignore the setLocale urls, we dont want the other languages until they stop redirecting to the landing pages.
        HtmlNodeFilters.tagWithAttributeRegex("a", "href", "/user/setLocale")
        
    };
    return new HtmlFilterInputStream(in,
                                     encoding,
                                     HtmlNodeFilterTransform.exclude(new OrFilter(filters)));
  }
  
}
