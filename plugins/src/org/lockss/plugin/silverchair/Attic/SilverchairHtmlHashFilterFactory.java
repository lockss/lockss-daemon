/*
 * $Id: SilverchairHtmlHashFilterFactory.java,v 1.1.2.2 2014-07-18 15:56:32 wkwilson Exp $
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

package org.lockss.plugin.silverchair;

import java.io.*;

import org.htmlparser.*;
import org.htmlparser.filters.*;
import org.lockss.daemon.PluginException;
import org.lockss.filter.*;
import org.lockss.filter.HtmlTagFilter.TagPair;
import org.lockss.filter.html.*;
import org.lockss.plugin.*;
import org.lockss.util.*;

public class SilverchairHtmlHashFilterFactory implements FilterFactory {

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
        /*
         * From the crawl filter
         */
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
        // Cross links: "This article/letter/etc. relates to...", "This erratum
        // concerns...", "See also...", [ACCP, ACP]
        HtmlNodeFilters.tagWithAttribute("div", "class", "linkType"),
        // Corrections create two-way link between articles (AMA)
        HtmlNodeFilters.tagWithAttribute("div", "id", "scm6MainContent_divCorrections"),
        HtmlNodeFilters.tagWithAttribute("div", "id", "scm6MainContent_divCorrectionLinkToParent"),
        // Author disclosures box tends to reference older policy/guidelines documents
        HtmlNodeFilters.tagWithAttribute("div", "id", "scm6MainContent_divDisclosures"),
        // Erratum link from old to new article [APA]
        HtmlNodeFilters.tagWithAttribute("div", "id", "scm6MainContent_divErratum"),
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
        /*
         * Broad area filtering 
         */
        // Document header (e.g. <meta> tags added over time)
        new TagNameFilter("head"),
        // Scripts
        new TagNameFilter("script"),
        new TagNameFilter("noscript"),
        // Comments
        HtmlNodeFilters.comment(),
        // Header
        HtmlNodeFilters.tagWithAttributeRegex("div", "class", "bannerTop"), // [ACCP, ACP, AMA, APA]
        HtmlNodeFilters.tagWithAttribute("div", "id", "globalHeader_dvMastHead"), // [ACCP, AMA]
        HtmlNodeFilters.tagWithAttribute("div", "id", "ctl00_globalHeader_dvMastHead"), // [ACP]
        HtmlNodeFilters.tagWithAttribute("div", "class", "journalHeader"), // [APA]
        // Right column
        // ...(see crawl filter section)
        // Footer
        HtmlNodeFilters.tagWithAttributeRegex("div", "class", "^footerWrap"), // [ACCP, ACP, AMA]
        HtmlNodeFilters.tagWithAttribute("div", "class", "journalFooter"), // [ACCP, ACP, AMA, APA]
        HtmlNodeFilters.tagWithAttribute("div", "class", "Footer"), // [ACCP, ACP, AMA, APA]
        HtmlNodeFilters.tagWithAttribute("div", "class", "bannerBottom"), // [APA]
        /*
         * Various 
         */
        // Ad banner above header [ACCP]
        HtmlNodeFilters.tagWithAttribute("div", "class", "addBanner"), // sic
        // ASP.NET state
        HtmlNodeFilters.tagWithAttribute("input", "type", "hidden"),
        HtmlNodeFilters.tagWithAttribute("div", "class", "aspNetHidden"),
        // Comment form wording varies, "comment" vs. "response"
        HtmlNodeFilters.tagWithAttribute("div", "id", "commentFormContainer"),
        // Some articles' topic keywords are reordered over time
        HtmlNodeFilters.tagWithAttribute("div", "class", "tagsSection"),
        // TOC pages change gensyms used to group articles by type over time
        HtmlNodeFilters.tagWithAttribute("a", "id", "ancArticleTypeBookMark"),
        HtmlNodeFilters.tagWithAttributeRegex("a", "id", "^scm6MainContent_rptdisplayIssues_outer_ancArticleTypeBookMarkJump"),
        // Web of Science number of citing articles [ACCP, AMA, APA]
        HtmlNodeFilters.tagWithAttribute("div", "id", "citingArticles"),
    };
    
    return new HtmlFilterInputStream(in,
                                     encoding,
                                     HtmlNodeFilterTransform.exclude(new OrFilter(nodeFilters)));
  }

//  public static void main(String[] args) throws Exception {
//    String file = "/tmp/p1/f1380160";
//    IOUtils.copy(new SilverchairHtmlHashFilterFactory().createFilteredInputStream(null, new FileInputStream(file), "utf-8"),
//                 new FileOutputStream(file + ".out"));
//  }
  
}
