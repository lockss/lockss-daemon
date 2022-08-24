/*

Copyright (c) 2000-2022, Board of Trustees of Leland Stanford Jr. University

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

package org.lockss.plugin.taylorandfrancis;

import java.io.InputStream;
import org.htmlparser.NodeFilter;
import org.lockss.daemon.PluginException;
import org.lockss.filter.html.*;
import org.lockss.plugin.*;
import org.lockss.plugin.atypon.BaseAtyponHtmlCrawlFilterFactory;

/* Changing this to inherit from the BaseAtypon crawl filter. 
 allows for inheritance of various Atypon elements to avoid future issues with
 skin changes
 There is also the option to turn on WS filtering or tag removal as needed.
 */
public class TaylorAndFrancisHtmlCrawlFilterFactory extends BaseAtyponHtmlCrawlFilterFactory {

  static NodeFilter[] filters = new NodeFilter[]{

    /***** 8/23/22 a change from literatum -> ajax. This may be implemented in BaseAtypon... at some point. ****/
    HtmlNodeFilters.tagWithAttributeRegex("div", "class", "ajaxAltmetricTopCitedArticlesWidget"),
    HtmlNodeFilters.tagWithAttributeRegex("div", "class", "ajaxMostReadWidget"),
    HtmlNodeFilters.tagWithAttributeRegex("div", "class", "ajaxMostCitedWidget"),
    HtmlNodeFilters.tagWithAttributeRegex("div", "class", "ajaxMostRecentWidget"),
    HtmlNodeFilters.tagWithAttributeRegex("div", "class", "ajaxOpenAccessWidget"),
    HtmlNodeFilters.tagWithAttributeRegex("div", "class", "ajaxBookIssueNavigation"),
    HtmlNodeFilters.tagWithAttributeRegex("div", "class", "ajaxAd"),
    HtmlNodeFilters.tagWithAttributeRegex("div", "class", "ajaxListOfIssuesWidget"),
    HtmlNodeFilters.tagWithAttributeRegex("div", "class", "ajaxListOfIssuesResponsiveWidget"),
    HtmlNodeFilters.tagWithAttributeRegex("div", "class", "ajaxBreadcrumbs"),
    HtmlNodeFilters.tagWithAttributeRegex("div", "class", "ajaxRelatedArticles"),
        
    /*******based on analysis on 3/31/17*****************/
    // found overcrawling due to new section at the bottom of full-text called "Notes" with
    // live links to references AND the same live links in spans used for scrollable overlays.
    // see http://www.tandfonline.com/doi/full/10.1080/0163660X.2015.1064710
    // <span class="ref-overlay scrollable-ref"> 
    HtmlNodeFilters.tagWithAttributeRegex("span", "class", "^ref-overlay"), 
    //<div class="summation-section">
    HtmlNodeFilters.tagWithAttributeRegex("div", "class", "summation-section"), 
    
    /*******based on analysis on 9/15/16*****************/
    
    // http://www.tandfonline.com/toc/twst20/29/1
    // TOC left side column stuff - literatumAd handled by parent
    HtmlNodeFilters.tagWithAttributeRegex("div", "class", "journalMenuWidget"), 
    // TOC main column stuff - handled in parent 
    // literatumMostRecentWidget, literatumMostRead, literatumMostCited
    // literatumListOfIssuesRepsonsiveWidget, literatumOpenAccessWidget 
    
    // http://www.tandfonline.com/doi/full/10.1080/02678373.2015.1004225
    // article page - top navigation
    // id=relatedContent, id=metrics-content
    HtmlNodeFilters.tagWithAttributeRegex("div", "class", "publicationSerialHeader"),
    // article - right column with relatedArt, relatedItem, etc
    HtmlNodeFilters.tagWithAttributeRegex("div", "class", "combinedRecommendationsWidget"), 
    // don't need to worry about pageFooter, pageHeader for crawling
    
    /********below are from original T&F crawl filter *********************/
    // News articles
    HtmlNodeFilters.tagWithAttribute("div", "id", "newsArticles"),
    // Related and most read articles
    HtmlNodeFilters.tagWithAttribute("div", "id", "relatedArticles"),
    //Ads from the publisher
    HtmlNodeFilters.tagWithAttribute("div", "class", "ad module"),
    // links to T&F articles go directly to other article
    HtmlNodeFilters.tagWithAttribute("div",  "id", "referencesPanel"),
    // If cited by other T&F could go directly to other article 
    HtmlNodeFilters.tagWithAttribute("div",  "id", "citationsPanel"), 
    //full article has in-line references with direct links 
    //example: http://www.tandfonline.com/doi/full/10.1080/09064702.2012.670665#.U0weNlXC02c
    // reference #20
    HtmlNodeFilters.tagWithAttribute("span",  "class", "referenceDiv"),     
    // full page with references in a list at the bottom - some with direct links, see
    //example: http://www.tandfonline.com/doi/full/10.1080/09064702.2012.670665#.U0weNlXC02c
    // reference #20
    // modified to be regex on 6/2/17 to handle ul class="references other word" id="references-Section"
    // see http://www.tandfonline.com/doi/full/10.1080/12265934.2016.1182053
    HtmlNodeFilters.tagWithAttributeRegex("ul",  "class", "^references"),     
    // if has "doi/mlt" will crawl filter out - but remove in case it doesn't
    HtmlNodeFilters.tagWithAttribute("li",  "class", "relatedArticleLink"),
    
    // 2/1/19 - erratum <--> original link on article page in keyword section
    HtmlNodeFilters.tagWithAttribute("div","class","related-original"),

    //do not follow breadcrumb back to TOC in case of overcrawl to article
    HtmlNodeFilters.tagWithAttribute("div", "id", "breadcrumb"),
    //and if you get to TOC, don't follow links in header (next/prev)
    HtmlNodeFilters.tagWithAttribute("div", "class", "hd"),

  };

  @Override
  public InputStream createFilteredInputStream(ArchivalUnit au,
      InputStream in,
      String encoding)
          throws PluginException {
    return super.createFilteredInputStream(au, in, encoding, filters);
  }

}
