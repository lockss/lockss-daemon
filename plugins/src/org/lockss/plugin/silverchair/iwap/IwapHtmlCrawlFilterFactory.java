/*
 * $Id$
 */

/*

Copyright (c) 2000-2018 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.plugin.silverchair.iwap;

import java.io.InputStream;

import org.htmlparser.NodeFilter;
import org.htmlparser.filters.OrFilter;
import org.lockss.daemon.PluginException;
import org.lockss.filter.html.*;
import org.lockss.plugin.*;

public class IwapHtmlCrawlFilterFactory implements FilterFactory {


  @Override
  public InputStream createFilteredInputStream(ArchivalUnit au,
                                               InputStream in,
                                               String encoding)
      throws PluginException {
    return new HtmlFilterInputStream(
        
        in,
        encoding,
        HtmlNodeFilterTransform.exclude(new OrFilter(new NodeFilter[] {
            // remove larger blocks first
            HtmlNodeFilters.tagWithAttributeRegex("section", "class", "master-header"),
            HtmlNodeFilters.tagWithAttributeRegex("section", "class", "footer_wrap"),
            HtmlNodeFilters.tagWithAttributeRegex("div", "class", "widget-SitePageHeader"),
            HtmlNodeFilters.tagWithAttributeRegex("div", "class", "widget-SitePageFooter"),
            // article left side with image of cover and nav arrows
            HtmlNodeFilters.tagWithAttributeRegex("div", "id", "InfoColumn"),
            // right side of article - all the latest, most cited, etc
            HtmlNodeFilters.tagWithAttributeRegex("div", "id", "Sidebar"),
            
            // references section - contain links to google,pubmed - guard against internal refs
            HtmlNodeFilters.tagWithAttributeRegex("div","class","^ref-list"),
            HtmlNodeFilters.tagWithAttribute("div","class","kwd-group"),
            // top of article - links to correction or original article
            HtmlNodeFilters.tagWithAttributeRegex("div", "class", "widget-ArticleLinks"),
            HtmlNodeFilters.tagWithAttributeRegex("div", "class", "widget-ToolboxSendEmail"),
            // cannot remove widget-Issue, as it exists in multiple locations, some of which are needed
            HtmlNodeFilters.tagWithAttributeRegex("div", "class", "issue-browse-top"),
            HtmlNodeFilters.tagWithAttribute("div", "class", "all-issues"),
            // don't collect the powerpoint version of images or slides
            HtmlNodeFilters.tagWithAttributeRegex("a", "class", "download(-slide|Imagesppt)"),
            
            // article - author section with notes could have some bogus relative links
            HtmlNodeFilters.tagWithAttributeRegex("div", "class", "al-author-info-wrap"),

            // Exclude this ".tif" <div class="site-theme-header-menu-item"><a href="/my-account/register?siteId=1&amp;returnUrl=%2fview-large%2ffigure%2f3010502%2fh2open-d-22-00026f03.tif" class="register at-register js-register-user-modals">Register</a></div>
            HtmlNodeFilters.tagWithAttributeRegex("div", "class", "site-theme-header-menu-item"),
            }))
        );
  }

}
