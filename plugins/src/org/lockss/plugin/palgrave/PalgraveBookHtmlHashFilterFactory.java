/* $Id$
 
Copyright (c) 2000-2014 Board of Trustees of Leland Stanford Jr. University,
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
