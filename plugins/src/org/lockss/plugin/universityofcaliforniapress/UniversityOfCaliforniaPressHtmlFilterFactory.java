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

package org.lockss.plugin.universityofcaliforniapress;

import java.io.*;

import org.htmlparser.NodeFilter;
import org.htmlparser.filters.*;
import org.lockss.daemon.PluginException;
import org.lockss.filter.*;
import org.lockss.filter.StringFilter;
import org.lockss.filter.HtmlTagFilter.TagPair;
import org.lockss.filter.html.*;
import org.lockss.plugin.*;
import org.lockss.util.*;

public class UniversityOfCaliforniaPressHtmlFilterFactory implements FilterFactory {

  public InputStream createFilteredInputStream(ArchivalUnit au,
                                               InputStream in,
                                               String encoding)
      throws PluginException {
    NodeFilter[] filters = new NodeFilter[] {
        // Variable scripting contents
        new TagNameFilter("script"),
        // Too many variants
        new TagNameFilter("head"),
        // Switched from <input>...</input> to <input />
        // Hard to express as a combination of HTML filter and string filters
        new TagNameFilter("form"),
        // Institution-dependent
        HtmlNodeFilters.tagWithAttribute("table", "class", "identitiesBar"),
        // Advertising
        HtmlNodeFilters.tagWithAttribute("div", "class", "footerAd"),
        // Next two for versioned RSS feed links
        HtmlNodeFilters.tagWithAttribute("link", "type", "application/rss+xml"),
        HtmlNodeFilters.tagWithAttributeRegex("a", "href", "/action/showFeed\\?(.*&)?mi="),
        // Variable file size
        HtmlNodeFilters.tagWithAttribute("span", "class", "fileSize"),
        // The URL structure of the <img> within changed
        HtmlNodeFilters.tagWithAttribute("div", "id", "firstPage"),
        // Significantly different variant
        HtmlNodeFilters.tagWithAttributeRegex("table", "class", "footer"),
        // Javascript specifics changed
        HtmlNodeFilters.tagWithAttributeRegex("a", "href", "^javascript:RightslinkPopUp"),
        // Some article views were added or removed over time
        HtmlNodeFilters.tagWithAttribute("div", "class", "article_link"),
        // Reverse citations feature added later
        HtmlNodeFilters.tagWithAttribute("div", "class", "citedBySection"),
        // Link to the latest issues
        HtmlNodeFilters.tagWithAttributeRegex("a", "href", "^/toc/"),
    };

    InputStream filtered = new HtmlFilterInputStream(in,
                                                     encoding,
                                                     HtmlNodeFilterTransform.exclude(new OrFilter(filters)));
    
    Reader reader = FilterUtil.getReader(filtered, encoding);
    Reader tagFilter = HtmlTagFilter.makeNestedFilter(reader,
                                                      ListUtil.list(
        // Comments that appeared or disappeared over time                                                                    
        new TagPair("<!-- placeholder", "-->"),
        // Contains a hard to characterize block of metadata (e.g. DOI)
        // whose formatting changed
        new TagPair("<!-- Start title of page and review -->",
                    "<!-- End title of page and review -->")
    ));
    // Use of "&nbsp;" or " " inconsistent over time
    Reader stringFilter = new StringFilter(tagFilter, "&nbsp;", " ");
    return new ReaderInputStream(new WhiteSpaceFilter(stringFilter));
  }

}
