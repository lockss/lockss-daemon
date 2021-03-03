/*

Copyright (c) 2000-2021, Board of Trustees of Leland Stanford Jr. University
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

package org.lockss.plugin.atypon.americansocietyofcivilengineers;

import java.io.InputStream;

import org.htmlparser.NodeFilter;
import org.lockss.daemon.PluginException;
import org.lockss.filter.html.*;
import org.lockss.plugin.*;
import org.lockss.plugin.atypon.BaseAtyponHtmlCrawlFilterFactory;


// be sure not to CRAWL filter out entire left column "dropzone-Left-sidebar" because
// we need to be able to pick up action/showCitFormats link

public class ASCEHtmlCrawlFilterFactory extends BaseAtyponHtmlCrawlFilterFactory {
  static NodeFilter[] filters = new NodeFilter[] {
    //parent handles much, including...
    // div id="relatedContent" which used to be in here...
    // left column section history
    // <div class="sessionViewed">
    // http://ascelibrary.org/toc/jaeied/18/4
    // http://ascelibrary.org/doi/full/10.1061/(ASCE)CO.1943-7862.0000372
    
    /*
     * This section is from < 2017
     */
    HtmlNodeFilters.tagWithAttribute("div", "class", "sessionViewed"),

    // on a book landing page the titleInfo leads back to the series of books and overcrawling
    // http://ascelibrary.org/doi/book/10.1061/9780784478820
    HtmlNodeFilters.tagWithAttribute("div", "class", "box-inner titleInfo"),
    /*
     * This section is from 2017+ - skin change
     */
    // Article landing - ajax tabs
    HtmlNodeFilters.tagWithAttributeRegex("div",  "id", "recommendedtabcontent"),
    HtmlNodeFilters.tagWithAttributeRegex("div",  "id", "reftabcontent"),
    HtmlNodeFilters.tagWithAttributeRegex("div",  "id", "infotabcontent"),
    HtmlNodeFilters.tagWithAttributeRegex("div",  "class", "editorialRelated"),
    
    
  };

  @Override
  public InputStream createFilteredInputStream(ArchivalUnit au,
      InputStream in,
      String encoding)
  throws PluginException{
    return super.createFilteredInputStream(au, in, encoding, filters);
  }
}
