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

package org.lockss.plugin.atypon.endocrinesociety;

import java.io.InputStream;
import org.htmlparser.NodeFilter;
import org.lockss.daemon.PluginException;
import org.lockss.filter.html.*;
import org.lockss.plugin.*;
import org.lockss.plugin.atypon.BaseAtyponHtmlCrawlFilterFactory;


public class EndocrineSocietyHtmlCrawlFilterFactory 
  extends BaseAtyponHtmlCrawlFilterFactory {
  static NodeFilter[] filters = new NodeFilter[] {
   

    // external links within Table figures or middle of a paragraph
    // ex: http://press.endocrine.org/doi/full/10.1210/en.2012-2254
    //     http://press.endocrine.org/doi/full/10.1210/en.2012-1768
    //     http://press.endocrine.org/doi/full/10.1210/en.2012-1820
    HtmlNodeFilters.tagWithAttribute("a", "class", "ext-link"),
    // Do not filter out Download Citation
    HtmlNodeFilters.allExceptSubtree(
        HtmlNodeFilters.tagWithAttributeRegex("div", "class", "literatumRightSidebar"),
          HtmlNodeFilters.tagWithAttributeRegex(
                 "a", "href", "/action/showCitFormats\\?")),
         
  };

  @Override
  public InputStream createFilteredInputStream(ArchivalUnit au,
      InputStream in,
      String encoding)
  throws PluginException{
    return super.createFilteredInputStream(au, in, encoding, filters);
  }

}