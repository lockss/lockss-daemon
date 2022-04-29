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
    // prev-next article is handled by parent
    
    // The bottom of the left column has a Session History tab which then may
    // contain links to articles (not distinguished by crawl rules). But the
    // link to showCitFormats (the RIS file download page) needs to be
    // picked up.
    HtmlNodeFilters.allExceptSubtree(
        HtmlNodeFilters.tagWithAttributeRegex( 
            "div", "class", "leftColumn"),
        HtmlNodeFilters.tagWithAttributeRegex(
            "a", "href", "/action/showCitFormats\\?")),

    // external links within References section
    // Volume 17, Issue 1
    // http://library.seg.org/doi/full/10.2113/JEEG17.1.1
    // In the References section, there is a link to 
    // http://library.seg.org/doi/full/10.2113/www.estcp.org
    HtmlNodeFilters.tagWithAttribute("div", "class", "abstractReferences"),
    
    // external links from Acknowledgements section
    // Volume 78, Issue 2
    // http://library.seg.org/doi/full/10.1190/geo2012-0303.1
    // In the Acknowledge section, there are links to 
    // http://library.seg.org/doi/full/10.1190/go.egi.eu/pdnon and 
    // http://library.seg.org/doi/full/10.1190/www.mathworks.com
    //          /matlabcentral/fileexchange/24531-accurate-fast-marching
    // external link from Case Studies section
    // Volume 78, Issue 1
    // http://library.seg.org/doi/full/10.1190/geo2012-0113.1
    // In the Case Studies section, there is link to 
    // http://library.seg.org/doi/full/10.1190
    //          /www.rockphysics.ethz.ch/downloads
    HtmlNodeFilters.tagWithAttribute("a", "class", "ext-link"),
    //the top header section has a hidden set of menus that contain links to other
    //books and because book dois are non-deterministic, we must filter out
    //the header
    // library.seg.org/doi/book/10.1190/1.9781560802952 - click on "geophysics" in header for example
    HtmlNodeFilters.tagWithAttribute("div", "id", "header"),
    
  };

  @Override
  public InputStream createFilteredInputStream(ArchivalUnit au,
      InputStream in,
      String encoding)
  throws PluginException{
    return super.createFilteredInputStream(au, in, encoding, filters);
  }
}
