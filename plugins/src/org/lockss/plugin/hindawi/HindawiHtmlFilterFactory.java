/*
 * $Id$
 */

/*

Copyright (c) 2000-2016 Board of Trustees of Leland Stanford Jr. University,
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

import java.io.*;
import java.util.*;

import org.htmlparser.*;
import org.htmlparser.filters.*;
import org.lockss.daemon.PluginException;
import org.lockss.filter.*;
import org.lockss.filter.StringFilter;
import org.lockss.filter.HtmlTagFilter.TagPair;
import org.lockss.filter.html.*;
import org.lockss.plugin.*;
import org.lockss.util.*;

public class HindawiHtmlFilterFactory implements FilterFactory {
  
  public InputStream createFilteredInputStream(ArchivalUnit au,
                                               InputStream in,
                                               String encoding)
      throws PluginException {
    NodeFilter[] filters = new NodeFilter[] {
        // Contains changing <meta> tags, <link> tags for CSS, etc.
        new TagNameFilter("head"),
        // There used to be <link> tags in mid-<body>
        new TagNameFilter("link"),
        // Filter out <script> tags that are edited often
        new TagNameFilter("script"),
        // Filter out all non-essential areas of pages
        HtmlNodeFilters.tagWithAttribute("div", "id", "site_head"),
        HtmlNodeFilters.tagWithAttribute("div", "id", "dvLinks"),
        HtmlNodeFilters.tagWithAttribute("div", "id", "ctl00_dvLinks"), // old name
        HtmlNodeFilters.tagWithAttribute("div", "id", "banner"),
        HtmlNodeFilters.tagWithAttribute("div", "id", "journal_navigation"),
        HtmlNodeFilters.tagWithAttribute("div", "id", "left_column"),
        HtmlNodeFilters.tagWithAttribute("div", "class", "right_column_actions"),
        HtmlNodeFilters.tagWithAttribute("div", "class", "footer_space"),
        HtmlNodeFilters.tagWithAttribute("div", "id", "footer"),
        // widget that used to appear
        HtmlNodeFilters.tagWithAttribute("div", "id", "dropmenudiv"),
        // In reference pages, links to Scopus, PubMed, etc. were added
        HtmlNodeFilters.tagWithAttribute("span", "class", "reflinks"),
    };
    InputStream afterHtmlParser = new HtmlFilterInputStream(in,
                                                            encoding,
                                                            HtmlNodeFilterTransform.exclude(new OrFilter(filters)));
    Reader filteredReader = FilterUtil.getReader(afterHtmlParser, encoding);
    Reader httpFilter = new StringFilter(filteredReader, "http:", "https:");
    List<TagPair> tagPairs = Arrays.asList(// DOCTYPE varies and may include entity embedded definitions
                                           new TagPair("[><!ENTITY", ">"), // Extra '>' added spuriously by HtmlParser
                                           new TagPair("<!DOCTYPE", ">"),
                                           // SVG markup style changed over time
                                           new TagPair("<svg", "</svg>"),
                                           // DOI became a link
                                           new TagPair("<br /><a href=\"https://dx.doi.org/", "</pre>"),
                                           new TagPair("<br /><a href=\"http://dx.doi.org/", "</pre>"),
                                           new TagPair("<br />doi:", "</pre>"),
                                           // Aggressive filtering
                                           new TagPair("<", ">"));
    HtmlTagFilter afterRemovingTags = HtmlTagFilter.makeNestedFilter(httpFilter,
                                                                     tagPairs);
    WhiteSpaceFilter afterRemovingWhiteSpace = new WhiteSpaceFilter(afterRemovingTags);
    return new ReaderInputStream(afterRemovingWhiteSpace);
  }
  
}
