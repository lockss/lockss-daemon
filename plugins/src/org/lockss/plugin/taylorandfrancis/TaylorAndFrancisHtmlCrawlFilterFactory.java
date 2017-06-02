/*
 * $Id$
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
