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

package org.lockss.plugin.secrecynews;

import java.io.*;

import org.htmlparser.NodeFilter;
import org.htmlparser.filters.OrFilter;
import org.lockss.daemon.PluginException;
import org.lockss.filter.html.HtmlFilterInputStream;
import org.lockss.filter.html.HtmlNodeFilterTransform;
import org.lockss.filter.html.HtmlNodeFilters;
import org.lockss.plugin.*;
import org.lockss.util.*;

public class SecrecyNewsHtmlFilterFactory implements FilterFactory {
    
static Logger log = Logger.getLogger("SecrecyNewsHtmlFilterFactory");

public InputStream createFilteredInputStream(ArchivalUnit au,
	                                          InputStream in,
	                                          String encoding)
  throws PluginException {
	
  NodeFilter[] filters = new NodeFilter[] {
  // Filters Akismet comment nonce
  HtmlNodeFilters.tagWithAttribute("input", "id", "akismet_comment_nonce"),
  // Filters Advertising Manager load time
  HtmlNodeFilters.commentWithString("Advertising Manager"),
  // Filters All in One SEO Pack
  HtmlNodeFilters.commentWithRegex("All in One SEO Pack"),
  // Filters Archives menu
  HtmlNodeFilters.tagWithText("li", "Archives"),
  // Filters category count
  HtmlNodeFilters.tagWithAttribute("li", "class", "categories"),
  // Other filters
  HtmlNodeFilters.commentWithRegex("[0-9]+ queries. [0-9.]+ seconds."),
  
  };

  OrFilter combinedFilter = new OrFilter(filters);
  HtmlNodeFilterTransform transform = HtmlNodeFilterTransform.exclude(combinedFilter);
  InputStream prefilteredStream = new HtmlFilterInputStream(in, encoding, transform);

  try {
	      
  Reader prefilteredReader = new InputStreamReader(prefilteredStream, encoding);
  log.debug("Returning filteredReader from SecrecyNewsHtmlFilterFactory");
  return new ReaderInputStream(prefilteredReader);
  }
  
  catch (UnsupportedEncodingException uee) {
  throw new PluginException(uee);
  }
}  
}
   