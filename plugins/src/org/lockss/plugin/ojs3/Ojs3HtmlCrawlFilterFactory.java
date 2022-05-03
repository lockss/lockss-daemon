
/*

Copyright (c) 2019 Board of Trustees of Leland Stanford Jr. University,
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

Except as contained in this notice, tMassachusettsMedicalSocietyHtmlFilterFactoryhe name of Stanford University shall not
be used in advertising or otherwise to promote the sale, use or other dealings
in this Software without prior written authorization from Stanford University.

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
