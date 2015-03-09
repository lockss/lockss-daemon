/* Id: $ */
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

package org.lockss.plugin.atypon.massachusettsmedicalsociety;

import java.io.InputStream;

import org.htmlparser.NodeFilter;
import org.htmlparser.filters.OrFilter;
import org.lockss.daemon.PluginException;
import org.lockss.filter.html.*;
import org.lockss.plugin.*;

/*
 * This Crawl Filter is NOT BASED on BaseAtypon!
 */
public class MassachusettsMedicalSocietyHtmlCrawlFilterFactory implements FilterFactory {

  @Override
  public InputStream createFilteredInputStream(ArchivalUnit au,
                                               InputStream in,
                                               String encoding)
      throws PluginException {
    NodeFilter[] filters = new NodeFilter[] {
        // Contain cross-links to other articles in other journals/volumes
            HtmlNodeFilters.tagWithAttribute("div", "id", "related"),
            HtmlNodeFilters.tagWithAttribute("div", "id", "trendsBox"),
            HtmlNodeFilters.tagWithAttribute("div", "id", "trendsMod"),
            // "Related Articles" 
            HtmlNodeFilters.tagWithAttribute("div", "class", "articleLink"),
            // Contains ads
            HtmlNodeFilters.tagWithAttribute("div", "id", "topAdBar"),
            HtmlNodeFilters.tagWithAttribute("div", "class", "topLeftAniv"),
            HtmlNodeFilters.tagWithAttribute("div", "class", "ad"),
            HtmlNodeFilters.tagWithAttribute("div", "id", "rightRailAd"),
            HtmlNodeFilters.tagWithAttribute("div", "class", "rightAd"),
            HtmlNodeFilters.tagWithAttribute("div", "id", "rightAd"),
            HtmlNodeFilters.tagWithAttribute("div", "class", "toolsAd"),
            HtmlNodeFilters.tagWithAttribute("div", "class", "bottomAd"),
            HtmlNodeFilters.tagWithAttribute("div", "class", "bannerAdTower"),
            //Certain ads do not have a specified div and must be removed based on regex
            HtmlNodeFilters.tagWithAttributeRegex("a", "href", "/action/clickThrough"),
            //Contains comments by users with possible links to articles in other journals/volumes
            HtmlNodeFilters.tagWithAttribute("dd", "id", "comments"),
            // Contains the number of articles currently citing and links to those articles in other journals/volumes
            HtmlNodeFilters.tagWithAttribute("dd", "id", "citedby"),
            //Contains a link to the correction or the article which is possibly part of another au
            HtmlNodeFilters.tagWithAttribute("div", "class", "articleCorrection"),
            /*
             * Links to corrected articles which can be in another au are in this format
             * This is no longer needed because we are now filtering the article text
             * HtmlNodeFilters.tagWithAttributeRegex("a", "href", "^(nejm|enejm).+"),
             */
            //Group of images that link to other articles
            HtmlNodeFilters.tagWithAttribute("div", "id", "galleryContent"),
            //constantly changing discussion thread with links to current article +?sort=newest...
            HtmlNodeFilters.tagWithAttribute("div", "class", "discussion"),
            //Letter possible from a future au
            HtmlNodeFilters.tagWithAttribute("dd", "id", "letters"),
            //may contain direct links to articles outside the au
            HtmlNodeFilters.tagWithAttribute("dd", "id", "article")
    };
    return new HtmlFilterInputStream(in,
                                     encoding,
                                     HtmlNodeFilterTransform.exclude(new OrFilter(filters)));
  }

}