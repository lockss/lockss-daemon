/*
 * $Id: ScHtmlHashFilterFactory.java,v 1.1 2015-01-31 03:57:40 thib_gc Exp $
 */

/*

Copyright (c) 2000-2015 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.plugin.silverchair;

import java.io.*;
import java.util.Arrays;

import org.apache.commons.io.IOUtils;
import org.htmlparser.NodeFilter;
import org.htmlparser.filters.OrFilter;
import org.lockss.daemon.PluginException;
import org.lockss.filter.*;
import org.lockss.filter.HtmlTagFilter.TagPair;
import org.lockss.filter.html.*;
import org.lockss.plugin.*;
import org.lockss.util.ReaderInputStream;

public class ScHtmlHashFilterFactory implements FilterFactory {

  @Override
  public InputStream createFilteredInputStream(ArchivalUnit au,
                                               InputStream in,
                                               String encoding)
      throws PluginException {

    InputStream filtered = new HtmlFilterInputStream(
      in,
      encoding,
      new HtmlCompoundTransform(
        /*
         * KEEP: throw out everything but main content areas
         */
        HtmlNodeFilterTransform.include(new OrFilter(new NodeFilter[] {
            // KEEP manifest page content [AMA]
            HtmlNodeFilters.tagWithAttributeRegex("div", "class", "widget-IssuesAndVolumeListManifest"),
            // KEEP main area of TOCs [AMA]
            HtmlNodeFilters.tagWithAttribute("div", "class", "articleBodyContainer"),
            // KEEP main area of article [AMA]
            HtmlNodeFilters.tagWithAttributeRegex("div", "class", "contentColumn"),
            // KEEP proper citation of article [AMA]
            HtmlNodeFilters.tagWithAttribute("span", "class", "citationCopyPaste"),
        })),
        /*
         * DROP: filter remaining content areas
         */
        HtmlNodeFilterTransform.exclude(new OrFilter(new NodeFilter[] {
            // DROP scripts, styles, comments
            HtmlNodeFilters.tag("script"),
            HtmlNodeFilters.tag("noscript"),
            HtmlNodeFilters.tag("style"),
            HtmlNodeFilters.comment(),
            // DROP eventual "Free" text-icon [AMA TOC/article]
            HtmlNodeFilters.tagWithAttributeRegex("span", "class", "freeArticle"),
            // DROP RSS and e-mail alert buttons [AMA TOC]
            HtmlNodeFilters.tagWithAttribute("div", "class", "subscribe"),
            // DROP expand/collapse buttons [AMA TOC]
            HtmlNodeFilters.tagWithAttribute("div", "class", "expandCollapse"),
            // DROP previous/next article links [AMA TOC/article]
            HtmlNodeFilters.tagWithAttribute("div", "class", "pageHeaderInfo"), // [AMA TOC]
            HtmlNodeFilters.tagWithAttribute("div", "class", "contentPageHeader"), // [AMA article]
            // DROP decorative horizontal separator [AMA article]
            HtmlNodeFilters.tagWithAttribute("span", "class", "separator"),
            // DROP text size picker [AMA article]
            HtmlNodeFilters.tagWithAttribute("div", "class", "textSize"),
            // DROP internal jump links [AMA article]
            HtmlNodeFilters.tagWithAttributeRegex("div", "class", "contentJumpLinks"),
            // DROP sections appended to end of main area [AMA article]
            HtmlNodeFilters.tagWithAttribute("div", "id", "divSignInSubscriptionUpsell"),
            HtmlNodeFilters.tagWithAttribute("div", "class", "relatedArticlesMobile"),
            HtmlNodeFilters.tagWithAttribute("div", "class", "collectionsMobile"),
            // DROP parts of figures/tables other than captions [AMA article/figures/tables]
            HtmlNodeFilters.allExceptSubtree(HtmlNodeFilters.tagWithAttributeRegex("div", "class", "figureSection"),
                                             new OrFilter(HtmlNodeFilters.tagWithAttributeRegex("h6", "class", "figureLabel"),
                                                          HtmlNodeFilters.tagWithAttribute("div", "class", "figureCaption"))),
            HtmlNodeFilters.allExceptSubtree(HtmlNodeFilters.tagWithAttributeRegex("div", "class", "tableSection"),
                                             HtmlNodeFilters.tagWithAttributeRegex("span", "class", "^Table ")),
            HtmlNodeFilters.tagWithAttributeRegex("span", "class", "^Figure "), // freeform, e.g. http://jama.jamanetwork.com/article.aspx?articleid=1487499
            // DROP Letters (7), CME (8) and Responses (10) tabs and panels [AMA article]
            HtmlNodeFilters.tagWithAttributeRegex("div", "class", "tab(7|8|10)Div"),
            HtmlNodeFilters.tagWithAttributeRegex("div", "id", "^tab(7|8|10)$"),
            // DROP external links in References [AMA article/references]
            HtmlNodeFilters.tagWithAttributeRegex("span", "class", "pubmedLink"),
            HtmlNodeFilters.tagWithAttributeRegex("span", "class", "crossrefDoi"),
        }))
      )
    );
    
    Reader reader = FilterUtil.getReader(filtered, encoding);

    // Remove all inner tag content
    Reader noTagFilter = new HtmlTagFilter(new StringFilter(reader, "<", " <"), new TagPair("<", ">"));
    
    // Remove white space
    return new ReaderInputStream(new WhiteSpaceFilter(noTagFilter));
  }

  public static void main(String[] args) throws Exception {
    for (String file : Arrays.asList("/tmp/e3/man1",
                                     "/tmp/e3/toc1",
                                     "/tmp/e3/art1",
                                     "/tmp/e3/art2",
                                     "/tmp/e3/art3",
                                     "/tmp/e3/art4",
                                     "/tmp/e3/art5",
                                     "/tmp/e3/art6")) {
      IOUtils.copy(new ScHtmlHashFilterFactory().createFilteredInputStream(null, new FileInputStream(file), null),
                   new FileOutputStream(file + ".out"));
    }
  }

}
