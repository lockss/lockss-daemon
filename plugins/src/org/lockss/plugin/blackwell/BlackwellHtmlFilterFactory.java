/*
 * $Id: BlackwellHtmlFilterFactory.java,v 1.9 2007-06-13 18:42:24 troberts Exp $
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

package org.lockss.plugin.blackwell;

import java.io.*;
import java.util.List;
import org.htmlparser.*;
import org.htmlparser.filters.*;

import org.lockss.util.*;
import org.lockss.filter.*;
import org.lockss.filter.html.*;
import org.lockss.plugin.*;

public class BlackwellHtmlFilterFactory implements FilterFactory {

  // Remove everything on the line after these comments
  static HtmlTagFilter.TagPair[] tagpairs = {
    new HtmlTagFilter.TagPair("<!-- Institution/Society Banners -->", "\n"),
    new HtmlTagFilter.TagPair("<!-- Ad Placeholder", "\n"),
    new HtmlTagFilter.TagPair("<", ">"),
  };
  static List tagList = ListUtil.fromArray(tagpairs);

  public InputStream createFilteredInputStream(ArchivalUnit au,
					       InputStream in,
					       String encoding) {

    // Remove inverse citations

    // <option value="#citart1">This article is cited by the following
    // articles in Blackwell Synergy and CrossRef</option>

    NodeFilter[] filters = new NodeFilter[11];
    filters[0] =
      HtmlNodeFilters.tagWithText("option", "article is cited by", true);

    filters[1] =
      HtmlNodeFilters.tagWithAttribute("ul", "class", "citedBy");

    filters[2] =
      HtmlNodeFilters.tagWithText("h3", "article is cited by", true);

    filters[3] =
      HtmlNodeFilters.tagWithAttribute("div", "class", "xlink");

    filters[4] =
      HtmlNodeFilters.tagWithAttribute("h2", "class", "title_surtitle");

    filters[5] =
      HtmlNodeFilters.tagWithAttribute("div", "class", "article_begins");

    filters[6] =
      HtmlNodeFilters.tagWithAttribute("div", "id", "header");

    filters[7] =
      HtmlNodeFilters.tagWithAttribute("form", "id", "quick_link");

    filters[8] =
      HtmlNodeFilters.tagWithAttribute("div", "id", "left_column");

    filters[9] = new TagNameFilter("head");

    filters[10] =
      HtmlNodeFilters.tagWithAttribute("p", "class", "issue_details");

    //    "<h3 id=\"CitedBy\">This article is cited by:</h3></div>" +

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
