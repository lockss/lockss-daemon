/*
 * $Id: SEGHtmlCrawlFilterFactory.java,v 1.1 2014-09-04 03:14:49 ldoan Exp $
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

package org.lockss.plugin.atypon.seg;

import java.io.InputStream;

import org.htmlparser.NodeFilter;
import org.lockss.daemon.PluginException;
import org.lockss.filter.html.*;
import org.lockss.plugin.*;
import org.lockss.plugin.atypon.BaseAtyponHtmlCrawlFilterFactory;

// be sure not to CRAWL filter out entire left column "dropzone-Left-sidebar" 
// because we need to be able to pick up action/showCitFormats link

public class SEGHtmlCrawlFilterFactory 
  extends BaseAtyponHtmlCrawlFilterFactory {
  static NodeFilter[] filters = new NodeFilter[] {
    
    // top right of issue toc - links to previous or next issue
    HtmlNodeFilters.tagWithAttribute("div", "id", "prevNextNav"),
    
    // top right of an article - links to previous or next article
    HtmlNodeFilters.tagWithAttribute("div", "id", "articleToolsNav"),
    
    // left column of an article - all except Download Citations
    HtmlNodeFilters.allExceptSubtree(
        HtmlNodeFilters.tagWithAttribute( 
            "div", "class", "yui3-u yui3-u-1-4 leftColumn"),
            HtmlNodeFilters.tagWithAttributeRegex(
                "a", "href", "/action/showCitFormats\\?")),

    // errata section       
    HtmlNodeFilters.tagWithAttribute("div", "id", "sec_Errata"),
    
    // external links within References section
    // ex: www.estcp.org
    HtmlNodeFilters.tagWithAttribute("div", "class", "abstractReferences"),
    
    // links within short-legend of a figure
    HtmlNodeFilters.tagWithAttribute("div", "class", "short-legend"),
   
    // external links from Acknowledgements section
    // ex: .../go.egi.eu/pdnon
    //     .../matlabcentral/fileexchange/24531-accurate-fast-marching
    // external link from Case Studies section
    // ex: .../www.rockphysics.ethz.ch/downloads
    HtmlNodeFilters.tagWithAttribute("a", "class", "ext-link"),
    
    // previous/next link from showFullPopup page, reconstructed from
    // javascript:popRefFull()
    //    http://library.seg.org/action/showFullPopup?id=f4&doi=10.1190%2Fgeo2012-0065.1
    HtmlNodeFilters.tagWithAttribute("span", "id", "prevnext"),
    
  };

  @Override
  public InputStream createFilteredInputStream(ArchivalUnit au,
      InputStream in,
      String encoding)
  throws PluginException{
    return super.createFilteredInputStream(au, in, encoding, filters);
  }
}
