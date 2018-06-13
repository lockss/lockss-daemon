/*
 * $Id$*/

/*

Copyright (c) 2017 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.plugin.projmuse;

import java.io.*;

import org.htmlparser.NodeFilter;
import org.htmlparser.filters.OrFilter;
import org.lockss.daemon.PluginException;
import org.lockss.filter.html.*;
import org.lockss.plugin.*;

public class ProjectMuse2017HtmlCrawlFilterFactory implements FilterFactory {

  public InputStream createFilteredInputStream(ArchivalUnit au,
                                               InputStream in,
                                               String encoding)
      throws PluginException {
    NodeFilter[] filters = new NodeFilter[] {
            HtmlNodeFilters.tagWithAttribute("div", "class", "header"),
            HtmlNodeFilters.tagWithAttribute("div", "class", "breadcrumb"),
            HtmlNodeFilters.tagWithAttribute("div", "class", "right_nav"),
            HtmlNodeFilters.tagWithAttribute("div", "class", "footer"),
            HtmlNodeFilters.tagWithAttribute("div", "id", "map"),
            HtmlNodeFilters.tagWithAttributeRegex("a", "class", "(prev|next)issue"),
            HtmlNodeFilters.tagWithAttributeRegex("a", "href", "/results[?].*(searchtype|section1)="),
            // in June 2018 launching new site - these updates based off beta site TODO - revisit after launch
            HtmlNodeFilters.tagWithAttribute("div", "id", "header"),
            HtmlNodeFilters.tagWithAttribute("div", "id", "rightnav_wrap"),
            HtmlNodeFilters.tagWithAttribute("div", "id", "footer_block"),
            //prev|next article, prev|next issue
            HtmlNodeFilters.tagWithAttribute("div", "id", "previous_next_interface"),
            // breadcrumb equivalent - journal, issue links
            // since we can't avoid picking up errata links (in line no designation) block going to issue from article
            HtmlNodeFilters.tagWithAttribute("li", "class", "designation"),
            // in-line links can't be helped, but this can
            HtmlNodeFilters.tagWithAttribute("p", "class", "related-article-box"),
            
            
        
        
    };
    return new HtmlFilterInputStream(in,
                                     encoding,
                                     HtmlNodeFilterTransform.exclude(new OrFilter(filters)));
  }
  
}
