/*

Copyright (c) 2000-2023, Board of Trustees of Leland Stanford Jr. University

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

package org.lockss.plugin.michigan;

import java.io.Reader;
import org.htmlparser.NodeFilter;
import org.htmlparser.filters.OrFilter;
import org.htmlparser.filters.TagNameFilter;
import org.lockss.filter.html.HtmlFilterInputStream;
import org.lockss.filter.html.HtmlNodeFilterTransform;
import org.lockss.filter.html.HtmlNodeFilters;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.plugin.FilterFactory;
import org.lockss.util.ReaderInputStream;
import java.io.InputStream;
import org.lockss.filter.FilterUtil;
import org.lockss.filter.WhiteSpaceFilter;
import org.lockss.filter.html.*;

public class UMichHtmlHashFilterFactory implements FilterFactory {

  public InputStream createFilteredInputStream(ArchivalUnit au,
                                               InputStream in,
                                               String encoding) {
    NodeFilter[] includeNodes = new NodeFilter[] {
    		// on an asset page or book landing page, just the description and bibliographic info
            HtmlNodeFilters.tagWithAttributeRegex("div", "id", "maincontent"),
            // https://www.fulcrum.org/epubs/bz60cx371 - is set to poll weight zero

    };

    NodeFilter[] excludeNodes = new NodeFilter[] {

      // filter out comments
      HtmlNodeFilters.comment(),
      // filter out script
      new TagNameFilter("script"),
      // header & footer
      //long item in place for "<!-- COinS for Zotero, etc -->" which changes over time
      HtmlNodeFilters.tagWithAttributeRegex("span", "title", "^ctx_ver="),
    };

    InputStream interStream = new HtmlFilterInputStream(in, encoding,
            new HtmlCompoundTransform(
                    HtmlNodeFilterTransform.include(new OrFilter(includeNodes)),
                    HtmlNodeFilterTransform.exclude(new OrFilter(excludeNodes)) //, xformAllTags
            ));

    Reader reader = FilterUtil.getReader(interStream, encoding);
    return new ReaderInputStream(new WhiteSpaceFilter(reader));
  }

}
