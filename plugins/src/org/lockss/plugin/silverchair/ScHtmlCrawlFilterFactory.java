/*
 * $Id$
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

import java.io.InputStream;

import org.htmlparser.NodeFilter;
import org.htmlparser.filters.OrFilter;
import org.lockss.daemon.PluginException;
import org.lockss.filter.html.*;
import org.lockss.plugin.*;

public class ScHtmlCrawlFilterFactory implements FilterFactory {

  /*
   * AMA = American Medical Association (http://jamanetwork.com/)
   * SPIE = SPIE (http://spiedigitallibrary.org/)
   */

  @Override
  public InputStream createFilteredInputStream(ArchivalUnit au,
                                               InputStream in,
                                               String encoding)
      throws PluginException {
    return new HtmlFilterInputStream(
      in,
      encoding,
      HtmlNodeFilterTransform.exclude(new OrFilter(new NodeFilter[] {
          // DROP right column: related content, etc.
          // KEEP links to article views, citation files, etc.
          // [AMA, SPIE]
          HtmlNodeFilters.allExceptSubtree(HtmlNodeFilters.tagWithAttributeRegex("div", "class", "portletColumn"),
                                           HtmlNodeFilters.tagWithAttribute("div", "id", "scm6MainContent_ToolBox")),
          // DROP link to previous/next article [AMA, SPIE]
          HtmlNodeFilters.tagWithAttribute("a", "class", "prev"),
          HtmlNodeFilters.tagWithAttribute("a", "class", "next"),
          // DROP link back to issue TOC (prune impact of overcrawl) [AMA, SPIE]
          HtmlNodeFilters.tagWithAttribute("a", "id", "scm6MainContent_lnkFullIssueName"),
          // DROP first page preview: sometimes appears, sometimes not [AMA article]
          HtmlNodeFilters.tagWithAttribute("div", "id", "divFirstPagePreview"),
          // DROP corrections link: creates two-way link between articles [AMA article]
          HtmlNodeFilters.tagWithAttribute("div", "id", "scm6MainContent_divCorrections"), // e.g. http://jama.jamanetwork.com/article.aspx?articleid=1456081 [AMA article]
          HtmlNodeFilters.tagWithAttribute("div", "id", "scm6MainContent_divCorrectionLinkToParent"), // e.g. http://jama.jamanetwork.com/article.aspx?articleid=1487482 [AMA article]
          // DROP letters pane (7): creates two-way link between letter and reply [AMA article]
          // e.g. http://jama.jamanetwork.com/article.aspx?articleid=1487491 and http://jama.jamanetwork.com/article.aspx?articleid=1487492
          // [SPIE]: unknown as of yet
          HtmlNodeFilters.tagWithAttribute("div", "id", "tab7"),
          
          // Unvetted from prior version:

          // Some references can be article links (especially in hidden tab) (AMA)
          HtmlNodeFilters.tagWithAttributeRegex("div", "class", "refContainer"),
          // Comments may contain article links? (not in above commentFormContainer div) (AMA)
          HtmlNodeFilters.tagWithAttribute("div", "class", "commentBody"),
          // Cross links: "This article/letter/etc. relates to...", "This erratum
          // concerns...", "See also...", [ACCP, ACP]
          HtmlNodeFilters.tagWithAttribute("div", "class", "linkType"),
          // Author disclosures box tends to reference older policy/guidelines documents
          HtmlNodeFilters.tagWithAttribute("div", "id", "scm6MainContent_divDisclosures"),
          // Erratum link from old to new article [APA]
          HtmlNodeFilters.tagWithAttribute("div", "id", "scm6MainContent_divErratum"),
      }))
    );
  }

}
