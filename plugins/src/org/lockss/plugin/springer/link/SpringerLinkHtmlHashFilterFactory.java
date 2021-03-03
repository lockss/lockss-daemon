/*

Copyright (c) 2000-2021, Board of Trustees of Leland Stanford Jr. University
All rights reserved.

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
import java.io.Reader;

import org.htmlparser.NodeFilter;
import org.lockss.filter.FilterUtil;
import org.lockss.filter.WhiteSpaceFilter;
import org.lockss.filter.html.*;
import org.lockss.plugin.*;
import org.lockss.util.ReaderInputStream;
import org.htmlparser.filters.OrFilter;

public class SpringerLinkHtmlHashFilterFactory implements FilterFactory {

  public InputStream createFilteredInputStream(ArchivalUnit au,
      InputStream in, String encoding) {

    HtmlFilterInputStream filteredStream = new HtmlFilterInputStream(in, encoding,
      new HtmlCompoundTransform(
      // Remove these parts first
      HtmlNodeFilterTransform.exclude(new OrFilter(new NodeFilter[] {
        // entire footer  (incl. ip address/logged in notifier)
        HtmlNodeFilters.tag("footer"),
        // article visits and other changeables
        HtmlNodeFilters.tagWithAttribute("div", "data-test", "article-metrics"),
        HtmlNodeFilters.tagWithAttribute("div", "id", "altmetric-container"),
        //adds on the side and top
        HtmlNodeFilters.tagWithAttributeRegex("aside", "class", "c-ad"),
        HtmlNodeFilters.tagWithAttribute("div", "class", "skyscraper-ad"),
        HtmlNodeFilters.tagWithAttribute("div", "class", "banner-advert"),
        HtmlNodeFilters.tagWithAttribute("div", "id", "doubleclick-ad"),
      })),
      // now keep these
      HtmlNodeFilterTransform.include(new OrFilter(new NodeFilter[] {
          HtmlNodeFilters.tag("p"),
          HtmlNodeFilters.tag("h1")
      }))
      ));
    Reader filteredReader = FilterUtil.getReader(filteredStream, encoding);
//    Reader noTagFilter = new HtmlTagFilter(new StringFilter(filteredReader, "<", " <"), new TagPair("<", ">"));

      // Remove white space
    return new ReaderInputStream(new WhiteSpaceFilter(filteredReader));
  }
  
}
