/*

Copyright (c) 2000-2021, Board of Trustees of Leland Stanford Jr. University
All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are met:

1. Redistributions of source code must retain the above copyright notice,
this list of conditions and the following disclaimer.

2. Redistributions in binary form must reproduce the above copyright notice,
this list of conditions and the following disclaimer in the documentation
and/or other materials provided with the distribution.

3. Neither the name of the copyright holder nor the names of its contributors
may be used to endorse or promote products derived from this software without
specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
POSSIBILITY OF SUCH DAMAGE.

*/

package org.lockss.plugin.atypon.massachusettsmedicalsociety;

import java.io.InputStream;

import org.htmlparser.NodeFilter;
import org.htmlparser.filters.OrFilter;
import org.lockss.daemon.PluginException;
import org.lockss.filter.html.*;
import org.lockss.plugin.*;
import org.lockss.plugin.atypon.BaseAtyponHtmlCrawlFilterFactory;

/*
 * This Crawl Filter is NOT BASED on BaseAtypon!
 */
public class MassachusettsMedicalSocietyHtmlCrawlFilterFactory extends BaseAtyponHtmlCrawlFilterFactory {

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
            HtmlNodeFilters.tagWithAttribute("dd", "id", "article"),
            /*
             * within the showImage viewer there are "downloadFigure" links that generate .ppt files
             * we do not want them because they are variable.
             */
            HtmlNodeFilters.tagWithAttributeRegex("li", "class", "^downloadSlides"),
            
            HtmlNodeFilters.tag("header"),
            HtmlNodeFilters.tag("aside"),
            HtmlNodeFilters.tag("nav"),
            HtmlNodeFilters.tag("footer"),
            
            HtmlNodeFilters.tagWithAttribute("div", "data-widget-def"),
            HtmlNodeFilters.tagWithAttribute("div", "id", "nejm_jobs"),
            HtmlNodeFilters.tagWithAttributeRegex("ol", "class", "article-reference"),
            HtmlNodeFilters.tagWithAttributeRegex("section", "id", "article_(letter|correspondence|reference|citing)"),
            HtmlNodeFilters.tagWithAttribute("section", "id", "author_affiliations"),
            HtmlNodeFilters.tagWithAttributeRegex("div", "class", "(^(footer-)?ad$|-banner|-institution|ArticleListWidget)"),
            HtmlNodeFilters.tagWithAttributeRegex("p", "class", "alert-bar"),
            
            HtmlNodeFilters.allExceptSubtree(
                HtmlNodeFilters.tagWithAttribute("ul", "class", "m-article-tools"),
                new OrFilter( // HtmlCompoundTransform(
                    HtmlNodeFilters.tagWithAttributeRegex("a", "href", "^/doi/pdf/"),
                    HtmlNodeFilters.tagWithAttributeRegex("a", "href", "^/action/showCitFormats[?]"))),
            
    };
    return super.createFilteredInputStream(au, in, encoding, filters);
  }

}