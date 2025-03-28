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

package org.lockss.plugin.elifesciences;

import java.io.InputStream;

import org.htmlparser.NodeFilter;
import org.htmlparser.filters.*;
import org.lockss.filter.html.*;
import org.lockss.plugin.*;
import org.lockss.util.Logger;

public class ELifeHtmlCrawlFilterFactory implements FilterFactory {
	
  private static final Logger log = Logger.getLogger(ELifeHtmlCrawlFilterFactory.class);

  public InputStream createFilteredInputStream(ArchivalUnit au,
                                               InputStream in,
                                               String encoding) {
    NodeFilter[] filters = new NodeFilter[] {
     HtmlNodeFilters.tagWithAttribute("header", "class", "section-header"),
     HtmlNodeFilters.tagWithAttribute("footer", "id", "section-footer"),
     // Do not crawl responsive header or references as links not wanted here
     HtmlNodeFilters.tagWithAttribute("div", "id", "region-responsive-header"),
     HtmlNodeFilters.tagWithAttribute("div", "id", "references"),
     HtmlNodeFilters.tagWithAttribute("div", "id", "comments"),
     // The following sections were a source of over-crawl (http://elifesciences.org/content/1/e00067)
     HtmlNodeFilters.tagWithAttributeRegex("div", "class", "other-versions"),
     HtmlNodeFilters.allExceptSubtree(
         HtmlNodeFilters.tagWithAttributeRegex("div", "class", "sidebar-wrapper"),
         HtmlNodeFilters.tagWithAttributeRegex("div", "class", "pane-elife-article-toolbox")),
     // possible links out of AU
     HtmlNodeFilters.tagWithAttributeRegex("div", "class", "elife-article-(corrections|criticalrelation)"),
    };
    return new HtmlFilterInputStream(in,
                                     encoding,
                                     HtmlNodeFilterTransform.exclude(new OrFilter(filters)));
  }

}


