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

package org.lockss.plugin.atypon.maney;

import java.io.InputStream;
import org.htmlparser.NodeFilter;
import org.lockss.daemon.PluginException;
import org.lockss.filter.html.*;
import org.lockss.plugin.*;
import org.lockss.plugin.atypon.BaseAtyponHtmlCrawlFilterFactory;

public class ManeyAtyponHtmlCrawlFilterFactory 
  extends BaseAtyponHtmlCrawlFilterFactory {
    
  NodeFilter[] filters = new NodeFilter[] {
      
      // handled by parent:
      // previous and next of toc
      // <td class="journalNavLeftTd">
      // <td class="journalNavRightTd">
      // prev-next article
      
      // from toc, abs, full, ref - News & alerts box near bottom
      // with About this Journal and Editors & Editorial Board tabs  
      // http://www.maneyonline.com/toc/aac/112/8                                       
      HtmlNodeFilters.tagWithAttribute("div",  "id", "migrated_news"),
      HtmlNodeFilters.tagWithAttribute("div",  "id", "migrated_aims"),
      HtmlNodeFilters.tagWithAttribute("div",  "id", "migrated_editors"),                                             
      
      // don't go to references - I don't think they link direct, but be safe
      // from ref - whole table of references
      // http://www.maneyonline.com/doi/ref/10.1179/2045772313Y.0000000128
      HtmlNodeFilters.tagWithAttribute("table", "class", "references"),            
      // from toc - sidebar has hard to isolate "most read", "most cited" 
      // "editors choice" - http://www.maneyonline.com/toc/aac/112/8
      HtmlNodeFilters.tagWithAttributeRegex("div", "class", 
                                            "literatumMostReadWidget"),
      HtmlNodeFilters.tagWithAttributeRegex("div", "class", 
                                            "literatumMostCitedWidget"),
      HtmlNodeFilters.tagWithAttributeRegex("div", "class", 
                                            "publicationListWidget"),      
      // from abs, full, ref - compact journal header box on right column
      // http://www.maneyonline.com/doi/abs/10.1179/1743676113Y.0000000112
      HtmlNodeFilters.tagWithAttribute("div", "id", "compactJournalHeader"),      
      // from toc - related content/original article
      // http://www.maneyonline.com/toc/his/37/1
      HtmlNodeFilters.tagWithAttribute("div", "class", "relatedLayer"),      
      // erratum points back to original article
      // http://www.maneyonline.com/doi/full/10.1179/0147888513Z.00000000076
      HtmlNodeFilters.tagWithAttribute("div", "id", "relatedContent")
  };
  @Override
  public InputStream createFilteredInputStream(ArchivalUnit au,
      InputStream in, String encoding) throws PluginException{ 
    return super.createFilteredInputStream(au, in, encoding, filters);
  }

}
