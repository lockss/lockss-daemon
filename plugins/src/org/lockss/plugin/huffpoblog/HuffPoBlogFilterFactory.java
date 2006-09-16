/*
 * $Id: HuffPoBlogFilterFactory.java,v 1.1 2006-09-16 23:29:49 tlipkis Exp $
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

package org.lockss.plugin.huffpoblog;

import java.io.*;
import java.util.List;
import org.htmlparser.*;
import org.htmlparser.filters.*;

import org.lockss.util.*;
import org.lockss.filter.*;
import org.lockss.plugin.*;

public class HuffPoBlogFilterFactory implements FilterFactory {

  public InputStream createFilteredInputStream(ArchivalUnit au,
					       InputStream in,
					       String encoding) {
    /*
     * Initial attempt - remove everything between the begin and end
     * ad tag comments
     */

    List tagList = ListUtil.list(
	new HtmlTagFilter.TagPair("<!-- begin ad tag -->", "<!-- End ad tag -->"),
	new HtmlTagFilter.TagPair("<script", "</script>", true),
	new HtmlTagFilter.TagPair("<table", "</table>", true),
	new HtmlTagFilter.TagPair("<ul class=\"relatedposts\">", "</ul>", true),
	new HtmlTagFilter.TagPair("<div class=\"relatedcats\">", "</div>", true)
        );
    Reader tagFilter =
      HtmlTagFilter.makeNestedFilter(FilterUtil.getReader(in,
							  encoding),
				     tagList);
    return new ReaderInputStream(new WhiteSpaceFilter(tagFilter));
  }

  public InputStream xcreateFilteredInputStream(ArchivalUnit au,
					       InputStream in,
					       String encoding) {
    /*
     * Initial attempt - remove everything between the begin and end
     * ad tag comments
     */

    NodeFilter adStart =
      HtmlNodeFilters.commentWithString("begin ad tag", true);
    NodeFilter adEnd =
      HtmlNodeFilters.commentWithString("End ad tag", true);
    HtmlTransform xform1 =
      HtmlNodeSequenceTransform.excludeSequence(adStart, adEnd);

    OrFilter filter = new OrFilter();
    filter.setPredicates(new NodeFilter[] {
      new TagNameFilter("script"),
      new TagNameFilter("table"),
      HtmlNodeFilters.tagWithAttribute("ul", "class", "relatedposts"),
      HtmlNodeFilters.tagWithAttribute("div", "class", "relatedcats"),
    });
    HtmlTransform xform2 = HtmlNodeFilterTransform.exclude(filter);

    HtmlTransform xform = new HtmlCompoundTransform(xform1, xform2);

    InputStream htmlFilter = new HtmlFilterInputStream(in, xform);

    List tagList = ListUtil.list(
	new HtmlTagFilter.TagPair("<!-- begin ad tag -->", "<!-- End ad tag -->"),
	new HtmlTagFilter.TagPair("<script", "</script>", true),
	new HtmlTagFilter.TagPair("<table", "</table>", true),
	new HtmlTagFilter.TagPair("<ul class=\"relatedposts\">", "</ul>", true),
	new HtmlTagFilter.TagPair("<div class=\"relatedcats\">", "</div>", true)
        );
    Reader tagFilter =
      HtmlTagFilter.makeNestedFilter(FilterUtil.getReader(htmlFilter,
							  encoding),
				     tagList);
    return new ReaderInputStream(new WhiteSpaceFilter(tagFilter));
  }


}
