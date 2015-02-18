/*
 * $Id$
 */

/*

Copyright (c) 2000-2009 Board of Trustees of Leland Stanford Jr. University,
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

