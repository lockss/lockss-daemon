/*

Copyright (c) 2000-2020, Board of Trustees of Leland Stanford Jr. University
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
          // use regex, was "recent_paper" and is now "recent_paper_viewport"
          HtmlNodeFilters.tagWithAttributeRegex("div", "id", "recent_paper"),
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
