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

package org.lockss.plugin.internationalunionofcrystallography.oai;

import java.io.InputStream;

import org.htmlparser.*;
import org.htmlparser.filters.*;
import org.htmlparser.tags.LinkTag;
import org.lockss.filter.html.*;
import org.lockss.plugin.*;

public class IUCrOaiHtmlHashFilterFactory implements FilterFactory {

  public InputStream createFilteredInputStream(ArchivalUnit au,
                                               InputStream in,
                                               String encoding) {
    NodeFilter[] filters = new NodeFilter[] {
      //filter out script
      HtmlNodeFilters.tag("script"),
      HtmlNodeFilters.tagWithAttribute("div", "id", "header"),
      HtmlNodeFilters.tagWithAttribute("div", "class", "clear"),
      HtmlNodeFilters.tagWithAttribute("div", "id", "footer"),
      HtmlNodeFilters.tagWithAttribute("div", "id", "footersearch"),
      HtmlNodeFilters.tagWithAttribute("div", "id", "journalsocialmedia"),
      HtmlNodeFilters.tagWithAttribute("div", "id", "bibl"),
      HtmlNodeFilters.tagWithAttributeRegex("div", "class", "article_functions"),
      
      /*
       * Abstract and full text HTML pages have a view counter that is not very
       * distinctive:
       * 
       * <a style="font-size:80%" href="//scripts.iucr.org/cgi-bin/citedin?wm5120">Viewed by <span style="border: 1px solid #401434;position:relative;top:1px;display:inline-block;padding-left:1px; padding-right:1px;">1307</span></a>
       */
      new NodeFilter() {
        @Override
        public boolean accept(Node node) {
          if (node instanceof LinkTag) {
            LinkTag linkTag = (LinkTag)node;
            String href = linkTag.getAttribute("href");
            if (href != null && href.contains("citedin")) {
              String text = linkTag.getStringText();
              return text != null && text.contains("Viewed by"); 
            }
          }
          return false;
        }
      },
    };
    return new HtmlFilterInputStream(in,
                                     encoding,
                                     HtmlNodeFilterTransform.exclude(new OrFilter(filters)));
  }

}
