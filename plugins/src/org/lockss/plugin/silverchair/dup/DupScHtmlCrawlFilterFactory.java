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

package org.lockss.plugin.silverchair.dup;

import org.htmlparser.NodeFilter;
import org.htmlparser.filters.OrFilter;
import org.lockss.daemon.PluginException;
import org.lockss.filter.html.HtmlFilterInputStream;
import org.lockss.filter.html.HtmlNodeFilterTransform;
import org.lockss.filter.html.HtmlNodeFilters;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.plugin.FilterFactory;

import java.io.InputStream;

public class DupScHtmlCrawlFilterFactory implements FilterFactory {



  @Override
  public InputStream createFilteredInputStream(ArchivalUnit au,
                                               InputStream in,
                                               String encoding)
      throws PluginException {
    return new HtmlFilterInputStream(
        in,
        encoding,
        HtmlNodeFilterTransform.exclude(new OrFilter(new NodeFilter[] {
        HtmlNodeFilters.tagWithAttributeRegex("div", "class", "master-header"),
        // now seeing this one. Leaving previous in case
        HtmlNodeFilters.tagWithAttributeRegex("div", "class", "widget-SitePageHeader"),
        HtmlNodeFilters.tagWithAttributeRegex("div", "class", "widget-SitePageFooter"),

        // article left side with image of cover and nav arrow
        HtmlNodeFilters.tagWithAttributeRegex("div", "id", "InfoColumn"),
        // right side of article - all the latest, most cited, etc

        HtmlNodeFilters.tagWithAttributeRegex("div", "id", "Sidebar"),
        HtmlNodeFilters.tagWithAttributeRegex("div", "class", "widget-RelatedContentSolr"),
        HtmlNodeFilters.tagWithAttributeRegex("div", "class", "widget-RelatedTags"),
        HtmlNodeFilters.tagWithAttributeRegex("div", "class", "widget-alerts"),
        HtmlNodeFilters.tagWithAttributeRegex("div", "class", "widget-links_wrap"),
        HtmlNodeFilters.tagWithAttributeRegex("div", "class", "ReferencesCss"),
        HtmlNodeFilters.tagWithAttributeRegex("div", "class", "widget-ArticleNavLinks"),

        // download link
        HtmlNodeFilters.tagWithAttributeRegex("a", "class", "download-slide"),
              
        // Limit access to other issues - nav bar with drop downs
        HtmlNodeFilters.tagWithAttributeRegex("div", "class","^issue-browse-top"),

        // which are also tagged so check this to guard against other locations
        HtmlNodeFilters.tagWithAttributeRegex("a",  "class", "^nav-link"),

        // article - author section with notes has some bogus relative links
        // which redirect back to article page so are collected as content
        HtmlNodeFilters.tagWithAttributeRegex("div", "class", "al-author-info-wrap"),

        //https://read.dukeupress.edu/american-literature/issue/90/1
        HtmlNodeFilters.tagWithAttributeRegex("div", "class", "all-issues"),
              
        })
      )
    );
  }

}
