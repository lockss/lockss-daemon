/*
 * $Id$
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

package org.lockss.plugin.edinburgh;

import java.io.InputStream;

import org.htmlparser.NodeFilter;
import org.lockss.filter.html.*;
import org.lockss.plugin.*;
import org.lockss.plugin.atypon.BaseAtyponHtmlHashFilterFactory;

public class EdinburghUniversityPressHashHtmlFilterFactory extends BaseAtyponHtmlHashFilterFactory {

  @Override
  public InputStream createFilteredInputStream(ArchivalUnit au,
      InputStream in,
      String encoding) {
    NodeFilter[] edFilter = new NodeFilter[] {        
        //Implementing maximal filtering - leave old stuff for safety though already be filtered in larger chunks
        HtmlNodeFilters.tagWithAttribute("div",  "id", "masthead"),
        HtmlNodeFilters.tagWithAttribute("div", "id", "mainNavContainer"),
        HtmlNodeFilters.tagWithAttribute("div", "id", "advSearchNavBottom"),
        HtmlNodeFilters.tagWithAttribute("div", "id", "mainBreadCrumb"),
        HtmlNodeFilters.tagWithAttribute("div", "id", "journalTitleContainer"),
        
        // Contains name and logo of institution
        HtmlNodeFilters.tagWithAttributeRegex("div", "id", "^institutionBanner"),
        // Current web site seems to do it this way...
        HtmlNodeFilters.tagWithAttributeRegex("li", "class", "^institutionBanner"),
        // left column
        HtmlNodeFilters.tagWithAttribute("div", "id", "journalSidebar"),
    };
    // super.createFilteredInputStream adds Edinburgh's filter to the baseAtyponFilters
    // and returns the filtered input stream using an array of NodeFilters that 
    // combine the two arrays of NodeFilters.
    return super.createFilteredInputStream(au, in, encoding, edFilter);

  }

}
