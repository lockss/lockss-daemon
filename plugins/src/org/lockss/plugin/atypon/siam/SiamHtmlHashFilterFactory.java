/*
 * $Id$
 */

/*

Copyright (c) 2000-2015 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.plugin.atypon.siam;

import java.io.InputStream;
import org.htmlparser.NodeFilter;
import org.lockss.filter.html.HtmlNodeFilters;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.plugin.atypon.BaseAtyponHtmlHashFilterFactory;

/* Siam can use the BaseAtyponHtmlHashFilter and extend it for the extra bits it needs */
public class SiamHtmlHashFilterFactory extends BaseAtyponHtmlHashFilterFactory {

  @Override
  public InputStream createFilteredInputStream(ArchivalUnit au,
      InputStream in,
      String encoding) {
    NodeFilter[] siamfilter = new NodeFilter[] {
        // contains the institution banner on both TOC and article pages, not always in header
        HtmlNodeFilters.tagWithAttribute("div", "class", "institutionBanner"),
        // On TOC contains argument in rss href that is time dependent, but none of this is needed for hash
        HtmlNodeFilters.tagWithAttribute("div", "id", "prevNextNav"),
        // the entire left column which can have browseVolumes, browsing history, tools, etc
        HtmlNodeFilters.tagWithAttribute("div", "id", "dropzone-Left-Sidebar"),
        // the entire right column at article level - tracks changeable history about this article
        HtmlNodeFilters.tagWithAttribute("div", "id", "pubHisDataDiv"),
        // On TOC article item may contain " | Cited x# times"; also contains journal/page for this article, but should be okay to hash out
        HtmlNodeFilters.tagWithAttribute("div", "class", "citation tocCitation"),
        // at top of article, list of types of format of the article will also have "Cited by" anchor once the article has been cited
        HtmlNodeFilters.tagWithAttribute("ul", "id", "articleToolList"),        

        // proactive removal of possible ad location although it's currently empty
        HtmlNodeFilters.tagWithAttribute("div", "class", "mainAd"),  
    };

    // super.createFilteredInputStream adds siamfilter to the baseAtyponFilters
    // and returns the filtered input stream using an array of NodeFilters that 
    // combine the two arrays of NodeFilters.
    return super.createFilteredInputStream(au, in, encoding, siamfilter);

  }
  
  @Override
  public boolean doWSFiltering() {
    return true;
  }
}

