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
    		  HtmlNodeFilters.tagWithAttributeRegex("a", "class", "prev"),
    		  HtmlNodeFilters.tagWithAttributeRegex("a", "class", "next"),
    		  // 6/15/18 - not seeing this header anymore,
    		  HtmlNodeFilters.tagWithAttributeRegex("div", "class", "master-header"),
    		  // now seeing this one. Leaving previous in case
    		  HtmlNodeFilters.tagWithAttributeRegex("div", "class", "widget-SitePageHeader"),
    		  HtmlNodeFilters.tagWithAttributeRegex("div", "class", "widget-SitePageFooter"),

    		  // article left side with image of cover and nav arrows
    		  HtmlNodeFilters.tagWithAttributeRegex("div", "id", "InfoColumn"),
    		  // right side of article - all the latest, most cited, etc
    		  HtmlNodeFilters.tagWithAttributeRegex("div", "id", "Sidebar"),
    		  // top of article - links to correction or original article
    		  HtmlNodeFilters.tagWithAttribute("div", "class", "articlelinks"),
    		  // don't collect the powerpoint version of images
    		  HtmlNodeFilters.tagWithAttribute("div", "class", "downloadImagesppt"),
    		  HtmlNodeFilters.tagWithAttributeRegex("a", "class", "download-slide"),
    		  
    		  //references to the article - contain links to google,pubmed - guard against internal refs
    		  HtmlNodeFilters.tagWithAttributeRegex("div",  "class", "^ref-content"),
    		  // and the references section may contains links to other articles in this journal
    		  // ex:https://academic.oup.com/bja/article/118/6/811/3829424  (look for /article/117)
    		  HtmlNodeFilters.tagWithAttributeRegex("div","class","^ref-list"),
    		  
    		  // Limit access to other issues - nav bar with drop downs
    		  HtmlNodeFilters.tagWithAttributeRegex("div", "class","^issue-browse-top"),
    		  // manifest/start page has hidden dropdown links to other issues
    		  HtmlNodeFilters.tagWithAttribute("div", "class", "navbar"),
    		  // which are also tagged so check this to guard against other locations
    		  HtmlNodeFilters.tagWithAttributeRegex("a",  "class", "^nav-link"),
    		  
    		  // article - author section with notes has some bogus relative links
    		  // which redirect back to article page so are collected as content
    		  // https://academic.oup.com/jnen/article/76/7/578/[XSLTImagePath]
    		  HtmlNodeFilters.tagWithAttributeRegex("div", "class", "al-author-info-wrap"),
    		  HtmlNodeFilters.tagWithAttributeRegex("dive", "class",  "widget-instance-OUP_FootnoteSection"),
    		  
    	  })
      )
    );
  }

}
