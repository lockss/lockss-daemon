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
        
        HtmlNodeFilters.tagWithAttribute("div","class", "skip"),
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

