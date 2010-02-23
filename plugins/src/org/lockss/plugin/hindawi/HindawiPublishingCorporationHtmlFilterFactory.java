/*
 * $Id: HindawiPublishingCorporationHtmlFilterFactory.java,v 1.7 2010-02-23 20:52:27 greya Exp $
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

package org.lockss.plugin.hindawi;

import java.io.InputStream;

import org.htmlparser.NodeFilter;
import org.htmlparser.filters.*;
import org.lockss.daemon.PluginException;
import org.lockss.filter.*;
import org.lockss.filter.html.*;
import org.lockss.plugin.*;
import org.lockss.util.ReaderInputStream;

public class HindawiPublishingCorporationHtmlFilterFactory implements FilterFactory {

  public InputStream createFilteredInputStream(ArchivalUnit au,
                                               InputStream in,
                                               String encoding)                                     
                                               
      throws PluginException {
        NodeFilter[] filters = new NodeFilter[] {
        // Filter out <div id="left_column">...</div>
        HtmlNodeFilters.tagWithAttribute("div", "id", "left_column"),
        // Filter out <input type="hidden" name="__VIEWSTATE" id="VIEWSTATE" 
        HtmlNodeFilters.tagWithAttribute("input", "id", "VIEWSTATE"),
        // Filter out <input type="hidden" name="__EVENTVALIDATION" id="EVENTVALIDATION"
        HtmlNodeFilters.tagWithAttribute("input", "id", "EVENTVALIDATION"),
    };


    InputStream htmlFilter = new HtmlFilterInputStream(in,
                                                       encoding,
                                                       HtmlNodeFilterTransform.exclude(new OrFilter(filters)));

    return new ReaderInputStream(new WhiteSpaceFilter(FilterUtil.getReader(htmlFilter, encoding)));
  }

}

