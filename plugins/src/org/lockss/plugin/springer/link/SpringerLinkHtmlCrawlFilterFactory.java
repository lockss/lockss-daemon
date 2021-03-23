/*
 * $Id: $
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

package org.lockss.plugin.springer.link;

import java.io.InputStream;

import org.htmlparser.NodeFilter;
import org.htmlparser.filters.*;
import org.lockss.daemon.PluginException;
import org.lockss.filter.html.*;
import org.lockss.plugin.*;


public class SpringerLinkHtmlCrawlFilterFactory implements FilterFactory {
  
  private static final NodeFilter[] filters = new NodeFilter[] {
      //footer, one of:
      HtmlNodeFilters.tagWithAttribute("div", "id", "footer"),
      HtmlNodeFilters.tag("footer"),

      //adds on the side and top
      HtmlNodeFilters.tagWithAttributeRegex("aside", "class", "c-ad"),
      HtmlNodeFilters.tagWithAttribute("div", "class", "skyscraper-ad"),
      HtmlNodeFilters.tagWithAttribute("div", "class", "banner-advert"),
      HtmlNodeFilters.tagWithAttribute("div", "id", "doubleclick-ad"),
      
      //header and search box
      HtmlNodeFilters.tagWithAttribute("div", "id", "header"),
      HtmlNodeFilters.tagWithAttribute("div", "role", "banner"),
      HtmlNodeFilters.tagWithAttribute("div", "class", "banner"),
      
      //// non essentials like metrics and related links
      // in header
      HtmlNodeFilters.tagWithAttribute("div", "data-test", "article-metrics"),
      HtmlNodeFilters.tagWithAttribute("div", "id", "altmetric-container"),
      // and in sidebar
      HtmlNodeFilters.tagWithAttribute("div", "role", "complementary"),
      HtmlNodeFilters.tagWithAttributeRegex("div", "class", "col-aside"),
      HtmlNodeFilters.tagWithAttributeRegex("aside", "class", "col-aside"),
      HtmlNodeFilters.tagWithAttribute("div", "class", "document-aside"),
      HtmlNodeFilters.tagWithAttribute("div", "class", "article-complementary-left"),
      HtmlNodeFilters.tagWithAttribute("div", "class", "document-aside"),

      //citations - filter out other springer links in references
      HtmlNodeFilters.tagWithAttribute("li", "class", "citation"),
      
  };
  
  @Override
  public InputStream createFilteredInputStream(ArchivalUnit au,
                                               InputStream in,
                                               String encoding)
      throws PluginException {
    
    return new HtmlFilterInputStream(in,
                                     encoding,
                                     HtmlNodeFilterTransform.exclude(new OrFilter(filters)));
  }

}
