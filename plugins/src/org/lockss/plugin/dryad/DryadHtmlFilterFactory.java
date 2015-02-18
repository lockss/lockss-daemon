/*
 * $Id$
 */ 

/*

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

package org.lockss.plugin.dryad;

import java.io.InputStream;
import org.htmlparser.NodeFilter;
import org.htmlparser.filters.OrFilter;
import org.htmlparser.filters.TagNameFilter;
import org.lockss.daemon.PluginException;
import org.lockss.filter.FilterUtil;
import org.lockss.filter.WhiteSpaceFilter;
import org.lockss.filter.html.*;
import org.lockss.plugin.*;
import org.lockss.util.Logger;
import org.lockss.util.ReaderInputStream;

public class DryadHtmlFilterFactory implements FilterFactory {
  Logger log = Logger.getLogger(DryadHtmlFilterFactory.class);
  
  public InputStream createFilteredInputStream(ArchivalUnit au,
                                               InputStream in,
                                               String encoding)
      throws PluginException {
    
    NodeFilter[] filters = new NodeFilter[] {
        // filter out script
        new TagNameFilter("script"),
        // div id="ds-header-wrapper"
        HtmlNodeFilters.tagWithAttribute("div", "id", "ds-header-wrapper"),
        // div id="ds-options-wrapper"
        HtmlNodeFilters.tagWithAttribute("div", "id", "ds-options-wrapper"),
        // div id="ds-footer-wrapper"
        HtmlNodeFilters.tagWithAttribute("div", "id", "ds-footer-wrapper"),
        // div id="sharemediv"
        HtmlNodeFilters.tagWithAttribute("div", "id", "sharemediv"),
        // div id="ds-system-wide-alert"
        HtmlNodeFilters.tagWithAttribute("div", "id", "ds-system-wide-alert"),
        // <span class="Z3988" title="ctx_ver=...rft.dryad=
        HtmlNodeFilters.tagWithAttributeRegex("span", "title", "rft[.]dryad="),
        // filter out tr with inner text "Pageviews"
        // filter out tr with inner text "Downloaded 999 times"
        HtmlNodeFilters.lowestLevelMatchFilter(
            HtmlNodeFilters.tagWithTextRegex("tr", 
                "(?s)<th>(?:Pageviews|Downloaded)</th>[\\s]*<td>[0-9]+(?: times?)?</td>", false)),
        // filter out tr with inner text "Downloaded 999 times"
        HtmlNodeFilters.lowestLevelMatchFilter(
            HtmlNodeFilters.tagWithTextRegex("tr", 
                "(?s)<td>[.]dryad[.](pageviews|downloads)</td>[\\s]*<td>[0-9]+</td>", false)),
    };
    
    // Do the initial html filtering
    InputStream filteredStream = new HtmlFilterInputStream(in,encoding,
        HtmlNodeFilterTransform.exclude(new OrFilter(filters)));
    
    return new ReaderInputStream(new WhiteSpaceFilter(FilterUtil.getReader(
        filteredStream, encoding)));
  }
  
}

