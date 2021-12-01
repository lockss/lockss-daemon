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
