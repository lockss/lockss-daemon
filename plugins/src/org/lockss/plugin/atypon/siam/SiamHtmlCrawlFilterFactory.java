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

package org.lockss.plugin.atypon.siam;

import java.io.InputStream;

import org.htmlparser.NodeFilter;
import org.lockss.daemon.PluginException;
import org.lockss.filter.html.*;
import org.lockss.plugin.*;
import org.lockss.plugin.atypon.BaseAtyponHtmlCrawlFilterFactory;

/*
 *
 * created because article links are not grouped under a journalid or volumeid,
 * but under article ids - will pull the links from the page, so filtering out
 * extraneous links
 * 
 */
public class SiamHtmlCrawlFilterFactory extends BaseAtyponHtmlCrawlFilterFactory {


  //Browse Volumes section okay because crawl rules will exclude
  NodeFilter[] filters = new NodeFilter[] {
      //prev-next article is covered by parent
      // tracks articles previously viewed
      HtmlNodeFilters.tagWithAttribute("div", "id", "sessionHistory"),
      //  articles that were references for this article
      HtmlNodeFilters.tagWithAttribute("div",  "class", "abstractReferences"),
      // Multiscale Modeling and Simulation Volume 20 article https://epubs.siam.org/doi/10.1137/22M1493707
      // has "corrected" article which goes to different volume
      HtmlNodeFilters.tagWithAttribute("div",  "class", "relation--head"),
      //https://epubs.siam.org/doi/10.1137/15M1022045, citation article will leak to other volumes
      HtmlNodeFilters.tagWithAttribute("div",  "class", "citation"),
  };

  @Override
  public InputStream createFilteredInputStream(ArchivalUnit au,
      InputStream in, String encoding) throws PluginException{ 
    return super.createFilteredInputStream(au, in, encoding, filters);
  }

}

