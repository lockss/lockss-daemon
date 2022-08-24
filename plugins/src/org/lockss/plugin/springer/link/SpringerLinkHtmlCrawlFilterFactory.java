/*

Copyright (c) 2000-2022, Board of Trustees of Leland Stanford Jr. University

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are met:

1. Redistributions of source code must retain the above copyright notice,
this list of conditions and the following disclaimer.

2. Redistributions in binary form must reproduce the above copyright notice,
this list of conditions and the following disclaimer in the documentation
and/or other materials provided with the distribution.

3. Neither the name of the copyright holder nor the names of its contributors
may be used to endorse or promote products derived from this software without
specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
POSSIBILITY OF SUCH DAMAGE.

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
