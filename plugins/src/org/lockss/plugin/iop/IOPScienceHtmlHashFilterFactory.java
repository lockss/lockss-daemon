/*
 * $Id: IOPScienceHtmlHashFilterFactory.java,v 1.8 2013-12-05 00:40:59 thib_gc Exp $
 */

/*

Copyright (c) 2000-2013 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.plugin.iop;

import java.io.*;

import org.apache.commons.compress.utils.IOUtils;
import org.htmlparser.NodeFilter;
import org.htmlparser.filters.OrFilter;
import org.htmlparser.filters.TagNameFilter;
import org.lockss.daemon.PluginException;
import org.lockss.filter.*;
import org.lockss.filter.HtmlTagFilter.TagPair;
import org.lockss.filter.html.*;
import org.lockss.plugin.*;
import org.lockss.util.ListUtil;
import org.lockss.util.ReaderInputStream;


public class IOPScienceHtmlHashFilterFactory implements FilterFactory {

  @Override
  public InputStream createFilteredInputStream(ArchivalUnit au,
                                               InputStream in,
                                               String encoding)
      throws PluginException {
    NodeFilter[] filters = new NodeFilter[] {
        /*
         * Broad area filtering
         */
        // Scripts
        new TagNameFilter("script"),
        // Document header
        HtmlNodeFilters.tagWithAttribute("link", "type", "text/css"),
        // Header
        HtmlNodeFilters.tagWithAttribute("div", "id", "cookieBanner"),
        /* <header> -- see string filter below FIXME HTML5 */
        HtmlNodeFilters.tagWithAttribute("div", "id", "jnl-head-band"),
        HtmlNodeFilters.tagWithAttribute("div", "id", "hdr"), // (old)
        HtmlNodeFilters.tagWithAttribute("div", "id", "nav"), // (old)
        HtmlNodeFilters.tagWithAttribute("div", "id", "header-content"), // (old)
        HtmlNodeFilters.tagWithAttribute("div", "id", "banner"), // (old)
        // Right column
        HtmlNodeFilters.tagWithAttribute("div",  "id", "rightCol"),
        HtmlNodeFilters.tagWithAttributeRegex("div", "class", "alsoRead"), // (now within)
        HtmlNodeFilters.tagWithAttribute("div", "id", "tacticalBanners"), // (now within)
        // Footer
        /* <footer> -- see string filter below FIXME HTML5 */
        HtmlNodeFilters.tagWithAttribute("div", "id", "footer"), // (old)

        /*
         * Main content area 
         */
        // Last 10 articles viewed
        HtmlNodeFilters.tagWithAttribute("div", "class", "tabs javascripted"),

        /*
         * Other
         */
        // may not be an issue, but concerned that mathjax exposure will change
        HtmlNodeFilters.tagWithAttribute("div",  "class", "mathJaxControls"),
        // Contains a jsessionid
        HtmlNodeFilters.tagWithAttributeRegex("form", "action", "jsessionid"),
    };
    
    InputStream filtered = new HtmlFilterInputStream(in,
                                                     encoding,
                                                     HtmlNodeFilterTransform.exclude(new OrFilter(filters)));
    
    Reader filteredReader = FilterUtil.getReader(filtered, encoding);
    Reader tagFilter = HtmlTagFilter.makeNestedFilter(filteredReader,
        ListUtil.list(
            new TagPair("<header>", "</header>"), // FIXME HTML5
            new TagPair("<footer>", "</footer>") // FIXME HTML5
        ));
    return new ReaderInputStream(new WhiteSpaceFilter(tagFilter));
  }

  public static void main(String[] args) throws Exception {
    String file = "/tmp/HashCUSA3a";
    IOUtils.copy(new IOPScienceHtmlHashFilterFactory().createFilteredInputStream(null,
                                                                                 new FileInputStream(file),
                                                                                 null),
                 new FileOutputStream(file + ".out"));
  }
  
}
