/*
 * $Id: OaiPmhHtmlFilterFactory.java,v 1.1.2.1 2014/05/05 17:32:30 wkwilson Exp $
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

package org.lockss.plugin.jafscd;

import java.io.InputStream;

import org.htmlparser.NodeFilter;
import org.htmlparser.filters.*;
import org.lockss.filter.html.*;
import org.lockss.plugin.*;

public class JafscdHtmlHashFilterFactory implements FilterFactory {

  public InputStream createFilteredInputStream(ArchivalUnit au,
                                               InputStream in,
                                               String encoding) {
    NodeFilter[] filters = new NodeFilter[] {
     //filter out script
     new TagNameFilter("script"),
     //Menu and related articles/other issues
     HtmlNodeFilters.tagWithAttribute("div", "class", "A_Left_Column"),
     //Footer menu that seems to currently be blank
     HtmlNodeFilters.tagWithAttribute("div", "class", "A_Foot_Menu"),
     //Copyright
     HtmlNodeFilters.tagWithAttribute("div", "class", "A_Foot_Copy"),
     //Lazy HTML these are inserted everywhere in an attempt to get the layout to work
     HtmlNodeFilters.tagWithAttribute("div", "style", "clear:both;"),
     //search box with broken layout. they may try to fix this
     HtmlNodeFilters.tagWithAttribute("div", "id", "search"),
     //another blank navigation div
     HtmlNodeFilters.tagWithAttribute("div", "id", "Sub_Top_Nav"),


    };
    return new HtmlFilterInputStream(in,
                                     encoding,
                                     HtmlNodeFilterTransform.exclude(new OrFilter(filters)));
  }

}
