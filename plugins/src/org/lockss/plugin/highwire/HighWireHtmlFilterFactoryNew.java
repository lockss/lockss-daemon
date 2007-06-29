/*
 * $Id: HighWireHtmlFilterFactoryNew.java,v 1.3 2007-06-29 18:27:13 troberts Exp $
 */

/*

Copyright (c) 2000-2006 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.plugin.highwire;

import java.io.*;
import java.util.List;
import org.htmlparser.*;
import org.htmlparser.filters.*;

import org.lockss.util.*;
import org.lockss.filter.*;
import org.lockss.filter.html.*;
import org.lockss.plugin.*;

public class HighWireHtmlFilterFactoryNew implements FilterFactory {

  // Remove everything on the line after these comments
  static HtmlTagFilter.TagPair[] tagpairs = {
    new HtmlTagFilter.TagPair("<A NAME=\"relation_type_", "</HTML>", 
			      true, false),
    new HtmlTagFilter.TagPair("<A NAME=\"otherarticles\">", "</HTML>"),
    new HtmlTagFilter.TagPair("<", ">"),
  };
  static List tagList = ListUtil.fromArray(tagpairs);

  public InputStream createFilteredInputStream(ArchivalUnit au,
					       InputStream in,
					       String encoding) {

    NodeFilter[] filters = new NodeFilter[8];
    filters[0] =
      HtmlNodeFilters.tagWithAttribute("div", "id", "authenticationstring");

    filters[1] = new TagNameFilter("script");

    filters[2] =
      HtmlNodeFilters.tagWithAttribute("div", "id", "user_nav");

    filters[3] =
      HtmlNodeFilters.tagWithAttribute("table", "class", "content_box_inner_table");

    filters[4] =
      HtmlNodeFilters.tagWithAttribute("a", "class", "contentbox");

    filters[5] =
      HtmlNodeFilters.tagWithAttribute("div", "id", "ArchivesNav");

    filters[6] =
      HtmlNodeFilters.tagWithText("strong", "related", true);

    filters[7] =
      HtmlNodeFilters.lowestLevelMatchFilter(HtmlNodeFilters.tagWithText("table", "Related Content", false));

    OrFilter combineFilter = new OrFilter();
    combineFilter.setPredicates(filters);

    HtmlTransform xform1 =
      HtmlNodeFilterTransform.exclude(combineFilter);

    // Still need to remove actual inverse citation section

    InputStream htmlFilter = new HtmlFilterInputStream(in, encoding, xform1);

    Reader rdr = FilterUtil.getReader(htmlFilter, encoding);
    Reader tagFilter = HtmlTagFilter.makeNestedFilter(rdr, tagList);
    //    return new ReaderInputStream(tagFilter);
    return new ReaderInputStream(new WhiteSpaceFilter(tagFilter));
  }


}
