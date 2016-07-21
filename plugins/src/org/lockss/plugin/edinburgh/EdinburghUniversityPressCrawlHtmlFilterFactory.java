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

package org.lockss.plugin.edinburgh;

import java.io.InputStream;

import org.htmlparser.NodeFilter;
import org.lockss.daemon.PluginException;
import org.lockss.filter.html.*;
import org.lockss.plugin.*;
import org.lockss.plugin.atypon.BaseAtyponHtmlCrawlFilterFactory;

/*
 *  Edinburgh changed skins in 2016. Leaving in the old filtering so long as it doesn't
 *  break anything. Adding in new filtering to cover new content layout
 */

public class EdinburghUniversityPressCrawlHtmlFilterFactory extends BaseAtyponHtmlCrawlFilterFactory {

  @Override
  public InputStream createFilteredInputStream(ArchivalUnit au,
                                               InputStream in,
                                               String encoding)
      throws PluginException {
    NodeFilter[] edFilters = new NodeFilter[] {
        // Contains logo of institution
        HtmlNodeFilters.tagWithAttribute("img", "id", "accessLogo"),
        // Contains "most downloaded articles" section
        HtmlNodeFilters.tagWithAttribute("div", "id", "journalSidebar"),
        //filter out prev/next article in case of overcrawl
        HtmlNodeFilters.tagWithAttribute("div", "class", "moduleToolBarPaging"),
        //filter out breadcrumb back to TOC in case of overcrawl
        HtmlNodeFilters.tagWithAttribute("div", "id", "mainBreadCrumb"),
        
        //NEW FILTERING to handle new skin - all both TOC and article text
        // navigation - in parent
        //HtmlNodeFilters.tagWithAttributeRegex("div", "class", "literatumBreadcrumbs"),
        // header section of page
        HtmlNodeFilters.tagWithAttributeRegex("div", "class", "page-header"),
        // tabbed info section below content
        HtmlNodeFilters.tagWithAttributeRegex("div", "id", "Publication_info_tabs"),
        // footer section of page
        HtmlNodeFilters.tagWithAttributeRegex("div", "class", "page-footer"),
        // right column - containing most read, etc
        HtmlNodeFilters.tagWithAttribute("div", "class", "col-sm-1-3 right-column"),
        // TOC tabbed section on TOC for listing all issues in journal - in parent
        //HtmlNodeFilters.tagWithAttributeRegex("div", "class", "literatumListOfIssuesWidget"),
        
    };
    return super.createFilteredInputStream(au, in, encoding, edFilters);
  }

}
