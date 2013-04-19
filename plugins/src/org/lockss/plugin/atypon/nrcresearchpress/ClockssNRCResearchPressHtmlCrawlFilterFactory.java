/* $Id: ClockssNRCResearchPressHtmlCrawlFilterFactory.java,v 1.1 2013-04-19 22:49:44 alexandraohlson Exp $
 */

/*

Copyright (c) 2000-2010 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.plugin.atypon.nrcresearchpress;

import java.io.InputStream;

import org.htmlparser.NodeFilter;
import org.htmlparser.filters.OrFilter;
import org.lockss.daemon.PluginException;
import org.lockss.filter.html.*;
import org.lockss.plugin.*;

/*
 *
 * created because article links are not grouped under a journalid or volumeid,
 * but under article ids - will pull the links from the page, so filtering out
 * extraneous links
 * 
 */
public class ClockssNRCResearchPressHtmlCrawlFilterFactory implements FilterFactory {

  @Override
  public InputStream createFilteredInputStream(ArchivalUnit au,
                                               InputStream in,
                                               String encoding)
      throws PluginException {
    NodeFilter[] filters = new NodeFilter[] {
      // Will exclude these tags from the stream:
      //   all stuff in the left sidebar
      HtmlNodeFilters.tagWithAttribute("div", "id", "sidebar-left"),
      //   all the stuff in the right sidebar
      HtmlNodeFilters.tagWithAttribute("div", "id", "sidebar-right"),
      //  strip below the main title with links to Home, About, Journals, etc...
      HtmlNodeFilters.tagWithAttribute("div", "id", "nav-wrapper"),
      // from issue TOC (e.g. http://www.nrcresearchpress.com/toc/cgj/36/5)
      // center area above the current issue (has links to prev/next/all issues)
      HtmlNodeFilters.tagWithAttribute("div", "class", "box-pad border-gray margin-bottom clearfix"),
      //   area with links to articles that cite this one
      HtmlNodeFilters.tagWithAttribute("div", "class", "citedBySection"),
      //   spider link in this tag
      HtmlNodeFilters.tagWithAttribute("span", "id", "hide"),
    };
    return new 
      HtmlFilterInputStream(in,
                            encoding,
                            HtmlNodeFilterTransform.exclude(new OrFilter(filters)));
  }

}
