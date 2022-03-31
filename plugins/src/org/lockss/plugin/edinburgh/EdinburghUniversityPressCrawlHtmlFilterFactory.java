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

package org.lockss.plugin.edinburgh;

import java.io.InputStream;

import org.htmlparser.NodeFilter;
import org.lockss.daemon.PluginException;
import org.lockss.filter.html.*;
import org.lockss.plugin.*;
import org.lockss.plugin.atypon.BaseAtyponHtmlCrawlFilterFactory;

/*
 *  Edinburgh changed skins in 2016. Leaving in the old filtering so long as it doesn't
 *  break anything. Adding in new filtering to cover new content layout
 */

public class EdinburghUniversityPressCrawlHtmlFilterFactory extends BaseAtyponHtmlCrawlFilterFactory {

  @Override
  public InputStream createFilteredInputStream(ArchivalUnit au,
                                               InputStream in,
                                               String encoding)
      throws PluginException {
    NodeFilter[] edFilters = new NodeFilter[] {
        // Contains logo of institution
        HtmlNodeFilters.tagWithAttribute("img", "id", "accessLogo"),
        // Contains "most downloaded articles" section
        HtmlNodeFilters.tagWithAttribute("div", "id", "journalSidebar"),
        //filter out prev/next article in case of overcrawl
        HtmlNodeFilters.tagWithAttribute("div", "class", "moduleToolBarPaging"),
        //filter out breadcrumb back to TOC in case of overcrawl
        HtmlNodeFilters.tagWithAttribute("div", "id", "mainBreadCrumb"),
        
        //NEW FILTERING to handle new skin - all both TOC and article text
        // navigation - in parent
        //HtmlNodeFilters.tagWithAttributeRegex("div", "class", "literatumBreadcrumbs"),
        // header section of page
        HtmlNodeFilters.tagWithAttributeRegex("div", "class", "page-header"),
        // tabbed info section below content
        HtmlNodeFilters.tagWithAttributeRegex("div", "id", "Publication_info_tabs"),
        // footer section of page
        HtmlNodeFilters.tagWithAttributeRegex("div", "class", "page-footer"),
        // right column - containing most read, etc
        HtmlNodeFilters.tagWithAttribute("div", "class", "col-sm-1-3 right-column"),
        // TOC tabbed section on TOC for listing all issues in journal - in parent
        //HtmlNodeFilters.tagWithAttributeRegex("div", "class", "literatumListOfIssuesWidget"),
        
        //1/24/19 
        //class="widget pageFooter none  widget-none  widget-compact-all"
        HtmlNodeFilters.tagWithAttributeRegex("div", "class", "pageFooter"),
        HtmlNodeFilters.tagWithAttributeRegex("div", "class", "pageHeader"),
        //verified specificity of right column tagging still in place
        //verified prev/next still handled by parent filter 
        
    };
    return super.createFilteredInputStream(au, in, encoding, edFilters);
  }

}
