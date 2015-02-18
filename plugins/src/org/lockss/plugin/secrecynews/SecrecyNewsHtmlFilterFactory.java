/*
 * $Id$
 */

/*

Copyright (c) 2000-2012 Board of Trustees of Leland Stanford Jr. University,
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
   