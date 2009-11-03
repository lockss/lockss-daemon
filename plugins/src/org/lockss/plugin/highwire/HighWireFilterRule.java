/*
 * $Id: HighWireFilterRule.java,v 1.12.22.1 2009-11-03 23:44:50 edwardsb1 Exp $
 */

/*

Copyright (c) 2000-2009 Board of Trustees of Leland Stanford Jr. University,
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
import org.lockss.util.*;
import org.lockss.filter.*;
import org.lockss.plugin.FilterRule;

public class HighWireFilterRule implements FilterRule {
  public static final String CITATION_STRING =
      "This article has been cited by other articles:";
  public static final String MEDLINE_STRING = "[Medline]";

  /** Also used by HighWireHtmlFilterFactory, so that the logic is only in
   * one place.  When this class is retired in favor of
   * HighWireHtmlFilterFactory, this logic should be moved there. */
  static Reader makeFilteredReader(Reader reader) {
    /*
     * Needs to be better (TSR 9-2-03):
     * 1)Filtering out everything in a table is pretty good, but over filters
     * 2) May want to filter comments in the future
     */


    List tagList = ListUtil.list(
        new HtmlTagFilter.TagPair("<STRONG>Institution:",
				  "</A>", true),
        new HtmlTagFilter.TagPair("<STRONG>Brought to you by:",
				  "</A>", true),
        new HtmlTagFilter.TagPair("<!-- BEGIN: layout -->",
				  "<!-- END: layout -->", true),
        new HtmlTagFilter.TagPair("<!-- begin ad tag -->",
				  "<!-- End ad tag -->", true),
        new HtmlTagFilter.TagPair("<table", "</table>", true),
        new HtmlTagFilter.TagPair("<", ">", false, false)
        );
    Reader tagFilter = HtmlTagFilter.makeNestedFilter(reader, tagList);
    Reader medFilter = new StringFilter(tagFilter, MEDLINE_STRING);
    Reader citeFilter = new StringFilter(medFilter, CITATION_STRING);
    return new WhiteSpaceFilter(citeFilter);
  }

  public Reader createFilteredReader(Reader reader) {
    return makeFilteredReader(reader);
  }
}
