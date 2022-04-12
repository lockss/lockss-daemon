/*

Copyright (c) 2000-2022, Board of Trustees of Leland Stanford Jr. University

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

package org.lockss.plugin.portlandpress;

import java.io.*;
import java.util.List;

import org.htmlparser.NodeFilter;
import org.htmlparser.filters.*;
import org.lockss.daemon.PluginException;
import org.lockss.filter.HtmlTagFilter;
import org.lockss.filter.html.*;
import org.lockss.plugin.*;
import org.lockss.util.*;

public class PortlandPressHtmlFilterFactory implements FilterFactory {

  public InputStream createFilteredInputStream(ArchivalUnit au,
                                               InputStream in,
                                               String encoding)
      throws PluginException {

    NodeFilter[] filters = new NodeFilter[] {
        // Contains the institution name
        HtmlNodeFilters.tagWithAttribute("div", "class", "courtesy_box"),
        // Contains the institution name (e.g. Biochemical Journal)
        HtmlNodeFilters.tagWithAttribute("td", "class", "BJnameLogo"),
        // Contains ads
        HtmlNodeFilters.tagWithAttribute("div", "class", "RHAdsBox"),
        // Contains ads
        HtmlNodeFilters.tagWithAttribute("div", "class", "RHAdvert"),
        // Contains the editorial board which may change over time (e.g. Biochemical Journal)
        HtmlNodeFilters.tagWithAttribute("td", "id", "LeftPanel"),
        // Contains the editorial board which may change over time (e.g. Biochemical Journal)
        new TagNameFilter("script"),
        // Contains variable links to recent issues (e.g. Biochemical Journal)
        HtmlNodeFilters.tagWithAttribute("td", "id", "RightPanel"),
        // Relook at x-plugin html Jan15 - update
        HtmlNodeFilters.tagWithAttribute("div", "class", "RHSocialBox"),
        // all of IWA and some of bioscirep, essay.biochemistry.org,
        // right side links (current issue and sample issue change 
        HtmlNodeFilters.tagWithAttribute("table","class","sidelinks"),
        // headers
        HtmlNodeFilters.tagWithAttribute("td","class","backgmast"),
        HtmlNodeFilters.tagWithAttribute("td","class","backg"),
        // http://www.biochemj.org/bj/455/2/default.htm
        //http://www.biochemsoctrans.org/bst/041/5/default.htm
        HtmlNodeFilters.tagWithAttribute("div","class","Nav_Panel_Right"),
        HtmlNodeFilters.tagWithAttribute("div","class","Page_Footer_Container"),
        HtmlNodeFilters.tagWithAttribute("div","class","Page_Header_Container"),
        // http://www.clinsci.org/cs/128/5/default.htm
        HtmlNodeFilters.tagWithAttribute("div","id","Banner"),
        HtmlNodeFilters.tagWithAttribute("div","id","RightHandDiv"),
        // ex: http://www.clinsci.org/cs/128/0321/cs1280321.htm
        // on article page, links to aspects, but also to citing article/similar paper pulldown which change
        HtmlNodeFilters.tagWithAttribute("div","class","NavPaperLinksBoxContainer"),
        
    };

    OrFilter combinedFilter = new OrFilter(filters);
    HtmlNodeFilterTransform transform = HtmlNodeFilterTransform.exclude(combinedFilter);
    InputStream prefilteredStream = new HtmlFilterInputStream(in, encoding, transform);
    
    try {
      
      List pairs = ListUtil.list(
          // Contains variable links to recent issues (e.g. Clinical Science)
          new HtmlTagFilter.TagPair("<!--- MID TEMPLATE --->", "<!--- END MID TEMPLATE --->"),
          // Contains variable links to recent issues (e.g. Clinical Science)
          new HtmlTagFilter.TagPair(">Immediate Publications<", ">Browse archive<")
      );
      
      Reader prefilteredReader = new InputStreamReader(prefilteredStream, encoding);
      Reader filteredReader = HtmlTagFilter.makeNestedFilter(prefilteredReader, pairs);
      return new ReaderInputStream(filteredReader);
    }
    catch (UnsupportedEncodingException uee) {
      throw new PluginException(uee);
    }

  }
  
}

