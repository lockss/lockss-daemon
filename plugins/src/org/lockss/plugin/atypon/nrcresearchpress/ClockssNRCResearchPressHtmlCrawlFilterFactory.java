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

package org.lockss.plugin.atypon.nrcresearchpress;

import java.io.InputStream;
import org.htmlparser.NodeFilter;
import org.lockss.daemon.PluginException;
import org.lockss.filter.html.*;
import org.lockss.plugin.*;
import org.lockss.plugin.atypon.BaseAtyponHtmlCrawlFilterFactory;

/*
 *
 * created because article links are not grouped under a journalid or volumeid,
 * but under article ids - will pull the links from the page, so filtering out
 * extraneous links
 * 
 */
public class ClockssNRCResearchPressHtmlCrawlFilterFactory extends BaseAtyponHtmlCrawlFilterFactory {
    NodeFilter[] filters = new NodeFilter[] {
      // Will exclude these tags from the stream:
      //   citedBySection handled by BaseAtypon
      //   all stuff in the left sidebar
      HtmlNodeFilters.tagWithAttribute("div", "id", "sidebar-left"),
      // Cannot filter out entire sidebar-right because we need to pick up the 
      //dowload citations -so remove smaller chunk
      // do not pick up the img links inside the ads
      HtmlNodeFilters.tagWithAttribute("div",  "class", "ads"),
      HtmlNodeFilters.tagWithAttributeRegex("div", "class", "addthis_toolbox"),
      //  strip below the main title with links to Home, About, Journals, etc...
      HtmlNodeFilters.tagWithAttribute("div", "id", "nav-wrapper"),
      // from issue TOC (e.g. http://www.nrcresearchpress.com/toc/cgj/36/5)
      // center area above the current issue (has links to prev/next/all issues)
      HtmlNodeFilters.tagWithAttribute("div", "class", "box-pad border-gray margin-bottom clearfix"),
      //   spider link in this tag
      HtmlNodeFilters.tagWithAttribute("span", "id", "hide"),
      // Remove link to "citing articles" 
      HtmlNodeFilters.tagWithAttribute("a",  "class", "icon-citing"), 
      // Remove link to "also read" 
      HtmlNodeFilters.tagWithAttribute("a",  "class", "icon-recommended"), 
      // Remove link to correction/corrected article 
      HtmlNodeFilters.tagWithAttribute("a",  "class", "icon-related"),
      // in case of overcrawl, don't follow next-prev-toc article links
      HtmlNodeFilters.tagWithAttribute("a",  "class", "white-link-right"),
      // in case of overcrawl, don't follow next-prev issue links on toc
      HtmlNodeFilters.tagWithAttributeRegex("a",  "title", "(Previous|Next) Issue"),
      // download of image now seems to be ppt slides which can cause hash issues
      // and are currently causing 500 errors. Just dont' collect
      HtmlNodeFilters.tagWithAttribute("a",  "id", "pptLink"),
      // the references section on the full text and abstract page is not well
      // identified but it contains direct links that lead to overcrawling when
      // from the same site
      HtmlNodeFilters.tagWithAttributeRegex("li",  "id", "ref(g)?[0-9]+"),
    };
    @Override
    public InputStream createFilteredInputStream(ArchivalUnit au,
        InputStream in, String encoding) throws PluginException{ 
      return super.createFilteredInputStream(au, in, encoding, filters);
    }


}
