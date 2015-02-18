/*
 * $Id$
 */

/*

Copyright (c) 2000-2014 Board of Trustees of Leland Stanford Jr. University,
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
        HtmlNodeFilters.tagWithAttribute("div", "class", "citedBySection"),
    };
    return new HtmlFilterInputStream(in,
                                     encoding,
                                     HtmlNodeFilterTransform.exclude(new OrFilter(filters)));
  }

}
