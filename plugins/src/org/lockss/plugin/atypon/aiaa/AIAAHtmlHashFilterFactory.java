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

package org.lockss.plugin.atypon.aiaa;

import java.io.InputStream;

import org.htmlparser.NodeFilter;
import org.lockss.filter.html.*;
import org.lockss.plugin.*;
import org.lockss.plugin.atypon.BaseAtyponHtmlHashFilterFactory;

public class AIAAHtmlHashFilterFactory extends BaseAtyponHtmlHashFilterFactory {
  

  @Override
  public InputStream createFilteredInputStream(ArchivalUnit au,
      InputStream in,
      String encoding) {
    NodeFilter[] afilters = new NodeFilter[] {
 
        // the entire left column which can have browseVolumes, browsing history, tools, etc
        HtmlNodeFilters.tagWithAttribute("div", "id", "dropzone-Left-Sidebar"),
        // not necessarily used, but we wouldn't want an ad
        HtmlNodeFilters.tagWithAttribute("div",  "class", "mainAd"),
        // these mark out sections that may or may not get filled and we 
        // were caught by a system maintenance temporary message
        HtmlNodeFilters.tagWithAttributeRegex("div", "class", "widget type-ad-placeholder"),
        // on a full text page, each section has pulldown  and prev/next arrows
        //which adds CITATION once the article has citations
        HtmlNodeFilters.tagWithAttribute("table", "class", "sectionHeading"),
        // and at the top "sections:" pulldown there it is harder to identify
        // seems to be <option value="#citart1"..>
        HtmlNodeFilters.tagWithAttributeRegex("option", "value", "#citart"),
        // add the following after AIAA platform upgrade 05/2019
        HtmlNodeFilters.tagWithAttribute("div",  "class", "header__institution-bar"),
        HtmlNodeFilters.tagWithAttribute("div",  "class", "header__search-bar"),
        
    };
    // super.createFilteredInputStream adds aiaa filter to the baseAtyponFilters
    // and returns the filtered input stream using an array of NodeFilters that 
    // combine the two arrays of NodeFilters and then applies a white space filter
    return super.createFilteredInputStream(au, in, encoding, afilters);
  }
  
  @Override
  public boolean doWSFiltering() {
    return true;
  }
  
  @Override
  public boolean doHttpsConversion() {
    return true;
  }

  // enable this filter since AIAA add more "id" attribute to html tag like h2 and a few others after 05/2019 platform change
  @Override
  public boolean doTagIDFiltering() {
    return true;
  }

  @Override
  public boolean doTagRemovalFiltering() { return true; }

}

// when initial citations are added, the drop down selection menu adds the option
// it might stabilize over time, but might as well hash out
//<option value="#_i33">Acknowledgments</option><option value="#_i34">References</option><option value="#citart1">CITING ARTICLES</option></select></form>

// and there might be a link to the reference (prev/next section)
//<a href="#citart1"><img src="/templates/jsp/images/arrow_down.gif" width="11" height="9" border="0" hspace="5" alt="Next section"></img></a></td>