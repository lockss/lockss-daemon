/* $Id$
 */

/*

Copyright (c) 2000-2013 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.plugin.atypon.futurescience;

import java.io.InputStream;
import java.util.regex.Pattern;

import org.htmlparser.Node;
import org.htmlparser.NodeFilter;
import org.htmlparser.filters.OrFilter;
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
      // articles have a section "Users who read this also read..." which is tricky to isolate
      // It's a little scary, but <div class="full_text"> seems only to be used for this section (not to be confused with fulltext)
      // though I could verify that it is followed by <div class="header_divide"><h3>Users who read this article also read:</h3></div>
      HtmlNodeFilters.tagWithAttribute("div", "class", "full_text"),

      //bibliography on an article page
      HtmlNodeFilters.tagWithAttribute("table",  "class", "references"),
      
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

  };

  public InputStream createFilteredInputStream(ArchivalUnit au,
      InputStream in, String encoding) throws PluginException{ 
    return super.createFilteredInputStream(au, in, encoding, filters);
  }

}

