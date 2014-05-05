/*
 * $Id: SilverchairHtmlCrawlFilterFactory.java,v 1.1.2.1 2014-05-05 17:32:31 wkwilson Exp $
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
in this Software without prior written authorization from Stanford University.
be used in advertising or otherwise to promote the sale, use or other dealings

*/

package org.lockss.plugin.silverchair;

import java.io.*;

import org.apache.commons.io.IOUtils;
import org.htmlparser.*;
import org.htmlparser.filters.*;
import org.lockss.daemon.PluginException;
import org.lockss.filter.HtmlTagFilter;
import org.lockss.filter.HtmlTagFilter.TagPair;
import org.lockss.filter.html.*;
import org.lockss.plugin.*;
import org.lockss.util.*;

public class SilverchairHtmlCrawlFilterFactory implements FilterFactory {

  private static final Logger logger = Logger.getLogger(SilverchairHtmlCrawlFilterFactory.class);
  
  @Override
  public InputStream createFilteredInputStream(ArchivalUnit au,
                                               InputStream in,
                                               String encoding)
      throws PluginException {
    /*
     * Unfortunately, some of the Javascript code contains string literals of
     * HTML markup that confuses HTMLparser, causing it to consider there are
     * unclosed <script> tags.
     */
    try {
      in = new BufferedInputStream(new ReaderInputStream(new HtmlTagFilter(new InputStreamReader(in, encoding),
                                                                           new TagPair("<script", "</script>", true, false)),
                                                                           encoding));
    }
    catch (UnsupportedEncodingException uee) {
      throw new PluginException(uee);
    }
    
    NodeFilter[] nodeFilters = new NodeFilter[] {
        // Cross-links to previous/next issue/article (AMA, ACCP)
        HtmlNodeFilters.tagWithAttributeRegex("a", "class", "prev"),
        HtmlNodeFilters.tagWithAttributeRegex("a", "class", "next"),
        // Link back to issue TOC (to prune impact of potential overcrawl)
        HtmlNodeFilters.tagWithAttribute("a", "id", "scm6MainContent_lnkFullIssueName"), // [ACCP, AMA, APA]
        HtmlNodeFilters.tagWithAttribute("a", "id", "ctl00_scm6MainContent_lnkFullIssueName"), // [ACP]
        // Letter links are article links (AMA)
        HtmlNodeFilters.tagWithAttributeRegex("div", "class", "letterSubmitForm"),
        HtmlNodeFilters.tagWithAttribute("div", "class", "letterBody"), // Ideally, div that contains a such div as a child
        // Some references can be article links (especially in hidden tab) (AMA)
        HtmlNodeFilters.tagWithAttributeRegex("div", "class", "refContainer"),
        // Comments may contain article links? (not in above commentFormContainer div) (AMA)
        HtmlNodeFilters.tagWithAttribute("div", "class", "commentBody"),
        // Corrections create two-way link between articles (AMA)
        HtmlNodeFilters.tagWithAttribute("div", "id", "scm6MainContent_divCorrections"),
        HtmlNodeFilters.tagWithAttribute("div", "id", "scm6MainContent_divCorrectionLinkToParent"),
        // Cross links: "This article/letter/etc. relates to...", "This erratum
        // concerns...", "See also...", [ACCP, ACP]
        HtmlNodeFilters.tagWithAttribute("div", "class", "linkType"),
        /*
         * The right column ('portletColumn') is likely to reference articles
         * (most read, related articles, most recent articles...), but it also
         * contains the links to the main article views and citation files in
         * a recognizable <div> ('scm6MainContent_ToolBox'). This node filter
         * prunes the subtree under portletColumn so that it only contains the
         * scm6MainContent_ToolBox subtree.
         * ACCP, AMA, APA:
         *     <div id="scm6MainContent_ToolBox"> inside <div class="portletColumn">
         * ACP:
         *     <div id="ctl00_scm6MainContent_ToolBox"> but not inside <div class="portletColumn">
         */
        new AllExceptSubtreeNodeFilter(HtmlNodeFilters.tagWithAttributeRegex("div", "class", "portletColumn"),
                                    HtmlNodeFilters.tagWithAttribute("div", "id", "scm6MainContent_ToolBox")),
    };
    return new HtmlFilterInputStream(in,
                                     encoding,
                                     encoding,
                                     HtmlNodeFilterTransform.exclude(new OrFilter(nodeFilters)));
  }

  public static void main(String[] args) throws Exception {
    String file = "/tmp/foo2/f3";
    IOUtils.copy(new SilverchairHtmlCrawlFilterFactory().createFilteredInputStream(null, new FileInputStream(file), "utf-8"),
                 new FileOutputStream(file + ".out"));
  }
  
}
