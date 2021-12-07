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

package org.lockss.plugin.pensoft.oai;

import java.io.InputStream;
import java.io.Reader;

import org.htmlparser.NodeFilter;
import org.htmlparser.filters.*;
import org.lockss.filter.FilterUtil;
import org.lockss.filter.StringFilter;
import org.lockss.filter.html.*;
import org.lockss.plugin.*;
import org.lockss.util.ReaderInputStream;

public class PensoftOaiHtmlHashFilterFactory implements FilterFactory {

  public InputStream createFilteredInputStream(ArchivalUnit au,
                                               InputStream in,
                                               String encoding) {
    NodeFilter[] filters = new NodeFilter[] {
     //filter out script
     new TagNameFilter("noscript"),
     new TagNameFilter("script"),
     new TagNameFilter("style"),
     new TagNameFilter("head"),
     
     HtmlNodeFilters.tagWithAttribute("div", "id", "header"),
     //popup stuff may change
     HtmlNodeFilters.tagWithAttribute("div", "id", "feedback-popup"),
     HtmlNodeFilters.tagWithAttribute("div", "id", "P-Post-Review-Form-Poll"),
     HtmlNodeFilters.tagWithAttribute("div", "class", "popup-background"),
     HtmlNodeFilters.tagWithAttribute("div", "class", "P-Article-References-For-Baloon"),
     HtmlNodeFilters.tagWithAttribute("div", "id", "ArticleBaloon"),
     HtmlNodeFilters.tagWithAttribute("div", "class", "P-clear"),
     HtmlNodeFilters.tagWithAttributeRegex("div", "class", "reflist"),
     HtmlNodeFilters.tagWithAttributeRegex("ul", "class", "references"),
     
     // found in http://compcytogen.pensoft.net/articles.php?id=5304
     // <link type="text/css" href="/lib/css/layout.css?v=1472563221" values change
     HtmlNodeFilters.tagWithAttribute("link", "type", "text/css"),
     
    };
    InputStream filteredStream = new HtmlFilterInputStream(in, encoding,
        HtmlNodeFilterTransform.exclude(new OrFilter(filters)));
    Reader httpFilter = new StringFilter(FilterUtil.getReader(filteredStream, encoding), "http:", "https:");
    return new ReaderInputStream(httpFilter);
  }

}
