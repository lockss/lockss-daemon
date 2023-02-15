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

package org.lockss.plugin.bioone;

import java.io.InputStream;

import org.htmlparser.NodeFilter;
import org.htmlparser.filters.*;
import org.lockss.daemon.PluginException;
import org.lockss.filter.html.*;
import org.lockss.plugin.*;

/* this crawl filter is currently not inheriting from BaseAtypon parent */
/*STANDALONE - DOES NOT INHERIT FROM BASE ATYPON */
public class BioOneAtyponHtmlCrawlFilterFactory implements FilterFactory {

  @Override
  public InputStream createFilteredInputStream(ArchivalUnit au, InputStream in,
                                               String encoding)
      throws PluginException {
    NodeFilter[] filters = new NodeFilter[] {

        // filtering to prevent over-crawl: header, mainFooter, articleNav, articlePageHeader, references
        HtmlNodeFilters.tagWithAttribute("div", "id", "header"),
        HtmlNodeFilters.tagWithAttribute("div", "id", "mainFooter"),
        HtmlNodeFilters.tagWithAttribute("div", "class", "articleNav"),
        HtmlNodeFilters.tagWithAttribute("div", "class", "articlePageHeader"),
        HtmlNodeFilters.tagWithAttributeRegex("div", "class", "[rR]eferences"),

        // Can exclude this entire item because we need the download citation links
        //HtmlNodeFilters.tagWithAttribute("div", "class", "relatedContent"),
        // instead we shall filter out the following components on an article page:
        HtmlNodeFilters.tagWithAttribute("div",  "id", "articleViews"),
        HtmlNodeFilters.tagWithAttribute("div",  "id", "Share"),
        HtmlNodeFilters.tagWithAttribute("div",  "id", "share"), //just in case
        HtmlNodeFilters.tagWithAttribute("div", "id", "relatedArticleSearch"),
        HtmlNodeFilters.tagWithAttributeRegex("div", "id", "citingArticles"),
        // and the following on a TOC page:
        HtmlNodeFilters.tagWithAttributeRegex("div", "id", "titleTools"),
        //filter prev-next article as protection in overcrawl
        HtmlNodeFilters.tagWithAttribute("a", "class", "articleToolsNav"),
        //filter next-prev issue on TOC 
        HtmlNodeFilters.tagWithAttribute("div", "class", "issueNav"),
        // and don't follow breadcrumbs back to TOC
        HtmlNodeFilters.tagWithAttribute("div", "id", "breadcrumbs"),

        // Contains reverse citations
        HtmlNodeFilters.tagWithAttributeRegex("div", "class", "citedBy"),
    };
    return new HtmlFilterInputStream(in,
                                     encoding,
                                     HtmlNodeFilterTransform.exclude(new OrFilter(filters)));
  }

}
