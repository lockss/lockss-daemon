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

package org.lockss.plugin.palgrave;

import java.io.InputStream;
import java.io.Reader;

import org.htmlparser.NodeFilter;
import org.htmlparser.filters.*;
import org.lockss.filter.FilterUtil;
import org.lockss.filter.WhiteSpaceFilter;
import org.lockss.filter.html.*;
import org.lockss.plugin.*;
import org.lockss.util.ReaderInputStream;

public class PalgraveBookHtmlHashFilterFactory implements FilterFactory {

  public InputStream createFilteredInputStream(ArchivalUnit au,
      InputStream in, String encoding) {
    NodeFilter[] filters = new NodeFilter[] {
	// http://www.palgraveconnect.com/pc/doifinder/10.1057/9780230597655
	new TagNameFilter("script"),
	new TagNameFilter("noscript"),
	// some unique numbers in metadata tab - remove all metadata
	// <meta name="emanating" content="171X66X236X16" />
	new TagNameFilter("meta"),
	// added by audrey: Extreme Hashing
	// there are differences in some of their comments; remove them all!
	HtmlNodeFilters.comment(),
	// removing the citation tab because it includes the access date
	HtmlNodeFilters.tagWithAttribute("dl", "class", "citation-list"),
	// header, footer in http://www.palgraveconnect.com/pc/doifinder/10.1057/9781137283351
        // institutional info in the constrain-header
	HtmlNodeFilters.tagWithAttribute("div", "id", "constrain-header"),
        HtmlNodeFilters.tagWithAttribute("div", "id", "constrain-footer"),
        // right sidebar
        // the following is in the crawl filter, but we don't filter it because 
        // it's within the "column-width-sidebar column-r"
        // HtmlNodeFilters.tagWithAttribute("div", "class", "box-well"),
        HtmlNodeFilters.tagWithAttribute("div", "class", "column-width-sidebar column-r"),
        // only keeping stuff in the left sidebar
        // on the Left Sidebar, removing the tab(s) that can show related books
        // (which can keep changing), a search tab and a references tab
        HtmlNodeFilters.tagWithAttribute("div", "id", "infoPanel3"),    // search
        HtmlNodeFilters.tagWithAttribute("div", "id", "infoPanel4"),    // reference
        HtmlNodeFilters.tagWithAttribute("div", "id", "infoPanel5"),    // related
        // looks like publisher is slowly switching its url conventions, leaving us
        // with little changes in urls (adding a ".page=0" to some urls
        HtmlNodeFilters.tagWithAttribute("li", "class", "view-cta"),
        HtmlNodeFilters.tagWithAttribute("li", "class", "download-cta"),
        HtmlNodeFilters.tagWithAttribute("li", "class", "kindle-cta"),
        // new stuff as of jan 2015: this contains "related content"
        HtmlNodeFilters.tagWithAttribute("div", "class", "similar-content cleared"),

    };
    InputStream filtered =  new HtmlFilterInputStream(in, encoding, HtmlNodeFilterTransform.exclude(new OrFilter(filters)));
    
    Reader filteredReader = FilterUtil.getReader(filtered, encoding);
    return new ReaderInputStream(new WhiteSpaceFilter(filteredReader));

    }
    
}
