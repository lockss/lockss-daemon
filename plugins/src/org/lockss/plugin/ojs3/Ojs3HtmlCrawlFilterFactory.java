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

package org.lockss.plugin.ojs3;

import java.io.InputStream;

import org.htmlparser.NodeFilter;
import org.htmlparser.filters.OrFilter;
import org.lockss.filter.html.HtmlFilterInputStream;
import org.lockss.filter.html.HtmlNodeFilterTransform;
import org.lockss.filter.html.HtmlNodeFilters;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.plugin.FilterFactory;
import org.lockss.util.Logger;

/*
 * SO far we only have two publishers who have migrated to OJS3 and the html is basic and
 * very similar 
 */

public class Ojs3HtmlCrawlFilterFactory implements FilterFactory {

  private static final Logger log = Logger.getLogger(Ojs3HtmlCrawlFilterFactory.class);

  
  private static final NodeFilter[] excludeNodes = new NodeFilter[] {
		  // Need the header for the download content in the pdf viewing frame
		  //HtmlNodeFilters.tag("header"),
		    HtmlNodeFilters.tag("footer"),
		    HtmlNodeFilters.tag("aside"),
		    HtmlNodeFilters.tag("script"),
		    HtmlNodeFilters.tag("nav"),
		  
	        // on the article landing page - remove the bottom stuff
	        HtmlNodeFilters.tagWithAttribute("section","class","article-more-details"),
//	        HtmlNodeFilters.tagWithAttribute("aside","id","sidebar"),
	        // on the article page - most read articles by this author
	        HtmlNodeFilters.tagWithAttributeRegex("div", "id", "articlesBySameAuthor"),
			// ignore the setLocale urls, we dont want the other languages until they stop redirecting to the landing pages.
			HtmlNodeFilters.tagWithAttributeRegex("a", "href", "/user/setLocale")
	    };  
 
  @Override
  public InputStream createFilteredInputStream(ArchivalUnit au,
      InputStream in, 
      String encoding) {

	  return new HtmlFilterInputStream(in,
              encoding,
              HtmlNodeFilterTransform.exclude(new OrFilter(excludeNodes)));
    
  }

}
