/*
 * $Id$
 */ 

/*

Copyright (c) 2000-2014 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.plugin.americanmathematicalsociety;

import java.io.InputStream;
import org.htmlparser.NodeFilter;
import org.htmlparser.filters.OrFilter;
import org.htmlparser.filters.TagNameFilter;
import org.lockss.daemon.PluginException;
import org.lockss.filter.html.*;
import org.lockss.plugin.*;
import org.lockss.util.Logger;

public class AmericanMathematicalSocietyHtmlFilterFactory implements FilterFactory {
  Logger log = Logger.getLogger(AmericanMathematicalSocietyHtmlFilterFactory.class);

  public InputStream createFilteredInputStream(
      ArchivalUnit au, InputStream in, String encoding)
          throws PluginException {
    NodeFilter[] filters = new NodeFilter[] {
        // Aggressive filtering of non-content tags
        new TagNameFilter("script"),
        // Ribbon is at top of page with society links and icons
        HtmlNodeFilters.tagWithAttribute("table", "id", "ribbon"),
        // Journal link block does not have a another useful attribute, hope this text
        // remains constant: <table summary="Table that holds logos and navigation">
        HtmlNodeFilters.tagWithAttributeRegex("table", "summary", "logos and navigation"),
        // issue and article navigation links that can change
        HtmlNodeFilters.tagWithAttribute("td", "id", "navCell"),
        // not sure what this contains but in case the number changes, removing XXX
        HtmlNodeFilters.tagWithAttribute("div", "class", "altmetric-embed"),
        // Changeable copyright and links
        HtmlNodeFilters.tagWithAttribute("table", "id", "footer"),
        
    };
    
    // Do the initial html filtering
    InputStream filteredStream = new HtmlFilterInputStream(in,encoding,
        HtmlNodeFilterTransform.exclude(new OrFilter(filters)));
    
    return filteredStream;
  }
  
}

