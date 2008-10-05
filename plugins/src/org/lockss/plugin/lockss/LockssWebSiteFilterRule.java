/*
 * $Id: LockssWebSiteFilterRule.java,v 1.1 2006-07-31 23:35:29 thib_gc Exp $
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

package org.lockss.plugin.lockss;

import java.io.Reader;
import java.util.List;

import org.lockss.filter.*;
import org.lockss.plugin.FilterRule;
import org.lockss.util.ListUtil;

public class LockssWebSiteFilterRule implements FilterRule {

  public Reader createFilteredReader(Reader reader) {
    // Remove meta KEYWORDS tag
    // Remove the parser cache comment "<!-- Saved in parser cache with key wikidb:pcache:idhash:1481-0!1!0!0!!en!2 and timestamp 20060724164541 -->"
    // Remove the timer comment "<!-- Served by www1 in 0.26 secs. -->"
    // Remove the list item "This page has been accessed 15.124 times."
    List tagList = ListUtil.list(new HtmlTagFilter.TagPair("<meta name=\"KEYWORDS\"",
                                                           "/>",
                                                           false),
                                 new HtmlTagFilter.TagPair("<!-- Saved in parser cache with key",
                                                           "-->",
                                                           false),
                                 new HtmlTagFilter.TagPair("<!-- Served by",
                                                           "-->",
                                                           false),
                                 new HtmlTagFilter.TagPair("<li id=\"f-viewcount\">",
                                                           "</li>",
                                                           false));
    Reader htmlTagReader = HtmlTagFilter.makeNestedFilter(reader, tagList);
    // Normalize whitespace
    Reader removeWhiteSpaceReader = new WhiteSpaceFilter(htmlTagReader);
    Reader addWhiteSpaceReader = new StringFilter(removeWhiteSpaceReader,
                                                  "><",
                                                  "> <");
    return addWhiteSpaceReader;
  }

}
