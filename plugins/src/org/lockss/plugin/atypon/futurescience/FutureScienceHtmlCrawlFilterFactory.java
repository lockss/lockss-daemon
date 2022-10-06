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

package org.lockss.plugin.atypon.futurescience;

import java.io.InputStream;
import java.util.regex.Pattern;

import org.htmlparser.Node;
import org.htmlparser.NodeFilter;
import org.htmlparser.tags.CompositeTag;
import org.htmlparser.tags.LinkTag;
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
public class FutureScienceHtmlCrawlFilterFactory extends BaseAtyponHtmlCrawlFilterFactory {
  protected static final Pattern prev_next = Pattern.compile("Prev. Article|Next Article", Pattern.CASE_INSENSITIVE);
  NodeFilter[] filters = new NodeFilter[] {

          HtmlNodeFilters.allExceptSubtree(
                  HtmlNodeFilters.tagWithAttributeRegex("div","class", "^(?!.*tocArticleEntry).*article-tools"),
                  HtmlNodeFilters.tagWithAttributeRegex(
                          "a", "href", "/action/showCitFormats\\?")),
      
      /* 
       * This first section is from < 2017
       */
      // articles have a section "Users who read this also read..." which is tricky to isolate
      // It's a little scary, but <div class="full_text"> seems only to be used for this section (not to be confused with fulltext)
      // though I could verify that it is followed by <div class="header_divide"><h3>Users who read this article also read:</h3></div>
      HtmlNodeFilters.tagWithAttribute("div", "class", "full_text"),
      //overcrawling is an occasional issue with in-line references to "original article"
      //protect from further crawl by stopping "next/prev" article/TOC/issue
      //I cannot see an obvious way to stop next/prev issue on TOC, so just limit getting to wrong toc
      HtmlNodeFilters.tagWithAttribute("table",  "class", "breadcrumbs"),      
      //irritatingly, next-prev article has no identifier...look at the text
      new NodeFilter() {
        @Override public boolean accept(Node node) {
          if (!(node instanceof LinkTag)) return false;
          String allText = ((CompositeTag)node).toPlainTextString();
          return prev_next.matcher(allText).find();
        }
      },
      /*
       * This section is from 2017+ - skin change and massive overcrawling...
       */
      // TOC - links to all other issues
      HtmlNodeFilters.tagWithAttribute("div",  "class", "loi__banner-list"),      
      HtmlNodeFilters.tagWithAttribute("div",  "class", "loi tab"),
      // Article landing - ajax tabs
      HtmlNodeFilters.tagWithAttributeRegex("li",  "id", "pane-pcw-(R|r)elated"),
      HtmlNodeFilters.tagWithAttributeRegex("li",  "id", "pane-pcw-(R|r)eferences"),
          
  };

  public InputStream createFilteredInputStream(ArchivalUnit au,
      InputStream in, String encoding) throws PluginException{ 
    return super.createFilteredInputStream(au, in, encoding, filters);
  }

}

