/*
 * $Id: PsychiatryOnlineHtmlFilterFactory.java,v 1.3 2010-04-29 08:29:42 thib_gc Exp $
 */

/*

Copyright (c) 2000-2010 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.plugin.psychiatryonline;

import java.io.*;

import org.htmlparser.NodeFilter;
import org.htmlparser.filters.*;
import org.lockss.daemon.PluginException;
import org.lockss.filter.StringFilter;
import org.lockss.filter.html.*;
import org.lockss.plugin.*;
import org.lockss.util.*;

public class PsychiatryOnlineHtmlFilterFactory implements FilterFactory {

  private static Logger logger = Logger.getLogger("PsychiatryOnlineHtmlFilterFactory");
  
  private static final String FILTERED_CHARSET_STRING = "<META http-equiv=\"Content-Type\" content=\"text/html; charset=utf-16\">";

  public InputStream createFilteredInputStream(ArchivalUnit au,
                                               InputStream in,
                                               String encoding)
      throws PluginException {
    // First remove "<META http-equiv="Content-Type" content="text/html; charset=utf-16">"
    try {
      InputStreamReader unfilteredReader = new InputStreamReader(in, encoding);
      StringFilter filteredReader = new StringFilter(unfilteredReader, FILTERED_CHARSET_STRING);
      filteredReader.setIgnoreCase(true);
      in = new ReaderInputStream(filteredReader); 
    }
    catch (UnsupportedEncodingException uee) {
      // Leave in unchanged but log a message
      logger.warning("Unknown InputStreamReader encoding: " + encoding, uee);
    }

    // Then filter out HTML constructs
    
    NodeFilter[] filters = new NodeFilter[] {
        // Filter out <script>
        new TagNameFilter("script"),
        // Filter out <span id=~"lblSeeAlso">
        HtmlNodeFilters.tagWithAttributeRegex("span", "id", "lblSeeAlso"),
        // Filter out <input type="hidden">
        HtmlNodeFilters.tagWithAttribute("input", "type", "hidden"),
    };
    
    return new HtmlFilterInputStream(in,
                                     encoding,
                                     HtmlNodeFilterTransform.exclude(new OrFilter(filters)));
  }

}
