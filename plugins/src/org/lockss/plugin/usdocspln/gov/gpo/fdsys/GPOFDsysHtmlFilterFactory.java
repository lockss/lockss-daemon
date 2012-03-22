/*
 * $Id: GPOFDsysHtmlFilterFactory.java,v 1.5 2012-03-22 23:23:27 davidecorcoran Exp $
 */

/*

Copyright (c) 2000-2011 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.plugin.usdocspln.gov.gpo.fdsys;

import java.io.*;
import java.util.List;

import org.htmlparser.NodeFilter;
import org.htmlparser.filters.OrFilter;
import org.lockss.daemon.PluginException;
import org.lockss.filter.HtmlTagFilter;
import org.lockss.filter.HtmlTagFilter.TagPair;
import org.lockss.filter.html.HtmlFilterInputStream;
import org.lockss.filter.html.HtmlNodeFilterTransform;
import org.lockss.filter.html.HtmlNodeFilters;
import org.lockss.plugin.*;
import org.lockss.util.*;

public class GPOFDsysHtmlFilterFactory implements FilterFactory {

public InputStream createFilteredInputStream(ArchivalUnit au,
	                                          InputStream in,
	                                          String encoding)
  throws PluginException {
	
  NodeFilter[] filters = new NodeFilter[] {
  // Filters the "Email a link to this page" link
  HtmlNodeFilters.tagWithAttributeRegex("a", "href", "^search.notificationPage\\.action\\?emailBody=")
  };

  OrFilter combinedFilter = new OrFilter(filters);
  HtmlNodeFilterTransform transform = HtmlNodeFilterTransform.exclude(combinedFilter);
  InputStream prefilteredStream = new HtmlFilterInputStream(in, encoding, transform);
	    
  try {
	      
  List pairs = ListUtil.list(
  // May contain a session token in a comment
  new TagPair("<!--<input type=\"hidden\" name=\"struts.token.name\" value=\"struts.token\" />",
              "\" />-->"),
  // ...Or not in a comment, just two <input> tags in a row
  new TagPair("<input type=\"hidden\" name=\"struts.token.name\" value=\"struts.token\" />",
              "\" />")
  );
	      
  Reader prefilteredReader = new InputStreamReader(prefilteredStream, encoding);
  Reader filteredReader = HtmlTagFilter.makeNestedFilter(prefilteredReader, pairs);
  return new ReaderInputStream(filteredReader);
  }
  
  catch (UnsupportedEncodingException uee) {
  throw new PluginException(uee);
  }
}  
}
   