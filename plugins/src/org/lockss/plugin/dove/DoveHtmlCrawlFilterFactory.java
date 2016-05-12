/*
 * $Id$
 */

/*

Copyright (c) 2000-2016 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.plugin.dove;

import java.io.InputStream;

import org.htmlparser.NodeFilter;
import org.htmlparser.filters.OrFilter;
import org.lockss.daemon.PluginException;
import org.lockss.filter.html.*;
import org.lockss.plugin.*;

/*
 * Non deterministic article links (uses journal abbreviation, but nothing for volume/year/issue)
 * Need to avoid the links to other volumes of this journal on the issue TOC...
 * Need to avoid the "Other article(s) by this author" and "Readers of this article also read"
 * sections on an article page
 * UNfortunately, Dove uses pretty much unlabelled html so we have to read headings...
 */
public class DoveHtmlCrawlFilterFactory implements FilterFactory {

  @Override
  public InputStream createFilteredInputStream(ArchivalUnit au,
                                               InputStream in,
                                               String encoding)
      throws PluginException {
    // [LA] = Libertas Academica
    NodeFilter[] filters = new NodeFilter[] {
      // Contain cross-links to other year issues for this journal
      // see: https://www.dovepress.com/reports-in-theoretical-chemistry-i1116-j129        
      HtmlNodeFilters.tagWithAttributeRegex("div", "class", "journal-articles group"),
      
      // the categories-bg group on the TOC page, we DO want
      //https://www.dovepress.com/clinical-interventions-in-aging-i983-j4
      // but not on an article page where it hold "Other articles by this author" and 
      // "readers of the article also read..."
      // fortunately the TOC has a sub-div "volume-issues...."
      // <div class="volume-issues issue-983 "> 
      HtmlNodeFilters.allExceptSubtree(
          HtmlNodeFilters.tagWithAttributeRegex("div", "class", "categories-bg group"),
            HtmlNodeFilters.tagWithAttributeRegex(
                   "div", "class", "volume-issues")),

    };
    return new HtmlFilterInputStream(in,
        encoding,
        encoding,
        HtmlNodeFilterTransform.exclude(new OrFilter(filters)));
  }

}
