/*
* $Id: $
*/

/*

 Copyright (c) 2000-2016 Board of Trustees of Leland Stanford Jr. University,
 all rights reserved.

 Permission is hereby granted, free of charge, to any person obtaining a copy
 of his software and associated documentation files (the "Software"), to deal
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

package org.lockss.plugin.copernicus;

import java.io.*;
import org.htmlparser.*;
import org.htmlparser.filters.*;
import org.lockss.daemon.PluginException;
import org.lockss.filter.html.*;
import org.lockss.plugin.*;
import org.lockss.util.Logger;

public class CopernicusHtmlCrawlFilterFactory implements FilterFactory {

        private static final Logger log = Logger.getLogger(CopernicusHtmlCrawlFilterFactory.class);
        
        protected static NodeFilter[] copernicusFilters = new NodeFilter[] {
          // The "home_url/index.html" page is picked up because it's a permission page
          // filter out the links to recent articles. Even though they use the volume
          // name (which the crawl filter requires) they use a form of the url that will
          // redirect and we don't correctly pick up that redirection.
          //  eg. http://www.arch-anim-breed.net/58/335/2015/
          // and not
          //     http://www.arch-anim-breed.net/58/335/2015/aab-58-335-2015.html
          // and we already have the article rom the volume/issue specific TOC
          HtmlNodeFilters.tagWithAttribute("div", "id", "recent_paper"),
	  //On the permission page $home_url/index.html we need to filter out:
          // header: current cover photo
          HtmlNodeFilters.tagWithAttribute("div", "id", "w-head"),
          // center section: landing page (current cover photo, links)
          HtmlNodeFilters.tagWithAttribute("div", "id", "landing_page"),
          // center section: recent papers (already done)
          // center section: news (links that change monthly, but usually links to outside)
          HtmlNodeFilters.tagWithAttribute("div", "id", "news_container"),
          // center section: highlighted articles, images
          HtmlNodeFilters.tagWithAttribute("div", "id", "highlight_articles"),
          // center section: logo images to avoid:
          HtmlNodeFilters.tagWithAttribute("div", "id", "essentential-logos-carousel"),

        };
        
        public InputStream createFilteredInputStream(ArchivalUnit au,
            InputStream in, String encoding) throws PluginException{

          return new HtmlFilterInputStream(in, encoding,
              HtmlNodeFilterTransform.exclude(new OrFilter(copernicusFilters)));
        }
}
