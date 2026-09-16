/*

Copyright (c) 2000-2026, Board of Trustees of Leland Stanford Jr. University

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

package org.lockss.plugin.americanmathematicalsociety;

import java.io.InputStream;

import org.htmlparser.NodeFilter;
import org.htmlparser.filters.*;
import org.lockss.daemon.PluginException;
import org.lockss.filter.html.*;
import org.lockss.plugin.*;


public class AmsHtmlCrawlFilterFactory implements FilterFactory {

  /*
   * Because the crawl rules explicitly exclude all volumes but this one, this 
   * shouldn't be necessary. But there are some links in references to the alternate
   * (previous) form of volume page
   * http://www.ams.org/ert/2005-009-02/
   * which is in some cases serving a 403 which breaks the crawl.
   * We can't exlcude the url pattern because it used to be the only volume
   * page format.
   * So instead, just exclude the references that were serving it. 
   * See page:
   * http://www.ams.org/journals/ert/2005-009-20/S1088-4165-05-00296-7/
   * though they might have fixed the 403 problem in the mean time.
   * 
   */
	
  @Override
  public InputStream createFilteredInputStream(ArchivalUnit au,
                                               InputStream in,
                                               String encoding)
      throws PluginException {
    NodeFilter[] filters = new NodeFilter[] {
      
    		// references with links to journal page and alternate form of volume page
    	     HtmlNodeFilters.tagWithAttribute("div", "id", "EnhancedReferences"),
        //there are hrefs that are phone numbers
           HtmlNodeFilters.tagWithAttributeRegex("a", "href", "tel:"),
        //do not crawl references section or sidebar
           HtmlNodeFilters.tagWithAttributeRegex("div", "id", "articleReferencesContent"),
           HtmlNodeFilters.tagWithAttribute("div","id", "sidebar"),
      
    };
    return new HtmlFilterInputStream(in,
                                     encoding,
                                     HtmlNodeFilterTransform.exclude(new OrFilter(filters)));
  }

}
