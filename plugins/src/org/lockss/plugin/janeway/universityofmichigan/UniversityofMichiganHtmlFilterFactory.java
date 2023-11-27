/*

Copyright (c) 2000-2020, Board of Trustees of Leland Stanford Jr. University
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

package org.lockss.plugin.janeway.universityofmichigan;

import org.htmlparser.NodeFilter;
import org.htmlparser.filters.OrFilter;
import org.htmlparser.filters.TagNameFilter;
import org.lockss.filter.FilterUtil;
import org.lockss.filter.html.HtmlFilterInputStream;
import org.lockss.filter.html.HtmlNodeFilterTransform;
import org.lockss.filter.html.HtmlNodeFilters;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.plugin.FilterFactory;
import org.lockss.util.ReaderInputStream;

import java.io.InputStream;
import java.io.Reader;

public class UniversityofMichiganHtmlFilterFactory implements FilterFactory {

  public InputStream createFilteredInputStream(ArchivalUnit au,
                                               InputStream in,
                                               String encoding) {
    NodeFilter[] filters = new NodeFilter[] {
     //filter out script
     new TagNameFilter("noscript"),
     new TagNameFilter("script"),
     new TagNameFilter("style"),
     new TagNameFilter("head"),
     new TagNameFilter("header"),
     new TagNameFilter("footer"),
     new TagNameFilter("aside"),

     // https://www.iastatedigitalpress.com/aglawdigest/article/id/24/
     HtmlNodeFilters.tagWithAttribute("div", "class", "summary"),
     HtmlNodeFilters.tagWithAttribute("div", "class", "sticky"),
     HtmlNodeFilters.tagWithAttribute("div", "id", "options-menu"),
     HtmlNodeFilters.tagWithAttribute("ul", "class", "menu"),
     HtmlNodeFilters.tagWithAttributeRegex("div", "class", "alm"),
     HtmlNodeFilters.tagWithAttributeRegex("div", "id", "reflist"),
     HtmlNodeFilters.tagWithAttributeRegex("ul", "class", "dropdown-content"),
     HtmlNodeFilters.tagWithAttributeRegex("div", "class", "nav-wrapper"),
     HtmlNodeFilters.tagWithAttributeRegex("nav", "class", "header-overrides"),

    };
    InputStream filteredStream = new HtmlFilterInputStream(in, encoding,
        HtmlNodeFilterTransform.exclude(new OrFilter(filters)));
    Reader httpFilter = FilterUtil.getReader(filteredStream, encoding);
    return new ReaderInputStream(httpFilter);
  }

}