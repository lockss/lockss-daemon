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

package org.lockss.plugin.silverchair.oup;

import java.io.InputStream;

import org.htmlparser.NodeFilter;
import org.htmlparser.filters.OrFilter;
import org.lockss.daemon.PluginException;
import org.lockss.filter.html.*;
import org.lockss.plugin.*;

public class OupScHtmlCrawlFilterFactory implements FilterFactory {



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
