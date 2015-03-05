/*
 * $Id$
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

package org.lockss.plugin.atypon.americanmeteorologicalsociety;

import java.io.InputStream;
import java.io.Reader;
import java.util.List;
import org.htmlparser.NodeFilter;
import org.lockss.filter.FilterUtil;
import org.lockss.filter.HtmlTagFilter;
import org.lockss.filter.StringFilter;
import org.lockss.filter.WhiteSpaceFilter;
import org.lockss.filter.HtmlTagFilter.TagPair;
import org.lockss.filter.html.*;
import org.lockss.plugin.*;
import org.lockss.plugin.atypon.BaseAtyponHtmlHashFilterFactory;
import org.lockss.util.ListUtil;
import org.lockss.util.Logger;
import org.lockss.util.ReaderInputStream;

/* 
 * Don't extend BaseAtyponHtmlHashFilterFactory because we need to do more 
 * extensive filtering with spaces, etc.
 */
public class AMetSocHtmlHashFilterFactory extends BaseAtyponHtmlHashFilterFactory {

  private static final Logger log = Logger.getLogger(AMetSocHtmlHashFilterFactory.class);

  @Override
  public InputStream createFilteredInputStream(ArchivalUnit au,
      InputStream in,
      String encoding) {
    NodeFilter[] filters = new NodeFilter[] {
        // May be empty, may contain "free" glif if appropriate
        HtmlNodeFilters.tagWithAttribute("div",  "class", "accessLegend"),
        // Contains "current issue" link which will change over time
        HtmlNodeFilters.tagWithAttribute("div", "id", "journalNavPanel"),
        // Contains "current issue" link which will change over time
        HtmlNodeFilters.tagWithAttribute("div", "id", "journalInfoPanel"),
        // Contains "current issue" link which will change over time
        HtmlNodeFilters.tagWithAttribute("div", "id", "sitetoolsPanel"),

        // Remove <hX> tags that have no content or only spaces as content
        // It would be nicer to do this as "all heading tags" but that doesn't seem available in the api
        HtmlNodeFilters.tagWithTextRegex("h1","^(\\s|(&nbsp;))*$"),
        HtmlNodeFilters.tagWithTextRegex("h2","^(\\s|(&nbsp;))*$"),
        HtmlNodeFilters.tagWithTextRegex("h3","^(\\s|(&nbsp;))*$"),
        HtmlNodeFilters.tagWithTextRegex("h4","^(\\s|(&nbsp;))*$"),
        HtmlNodeFilters.tagWithTextRegex("h5","^(\\s|(&nbsp;))*$"),
        HtmlNodeFilters.tagWithTextRegex("h6","^(\\s|(&nbsp;))*$"),
    };

    // super will add in the base nodeset and also do span-id transform
    InputStream superFiltered = super.createFilteredInputStream(au, in, encoding, filters);

    // Also need white space filter to condense multiple white spaces down to 1
    Reader reader = FilterUtil.getReader(superFiltered, encoding);

    // first subsitute plain white space for &nbsp;
    String[][] unifySpaces = new String[][] { 
        // inconsistent use of nbsp v empty space - do this replacement first
        {"&nbsp;", " "}, 
    };
    Reader NBSPFilter = StringFilter.makeNestedFilter(reader,
        unifySpaces, false);   

    //now consolidate white space before doing additional tagfilter stuff
    Reader WSReader = new WhiteSpaceFilter(NBSPFilter);
    Reader filtReader = makeFilteredReader(WSReader);

    // jeez. bogus occasional addition of trailing space in "ref nowrap "
    String[][] findAndReplace = new String[][] {
        // use of &nbsp; or " " inconsistent over time
        {"ref nowrap ", "ref nowrap"}, 
    };

    Reader stringFilter = StringFilter.makeNestedFilter(filtReader,
                                                          findAndReplace,
                                                          false);
    return new ReaderInputStream(stringFilter);
  }


  // Noisy whitespace has already been removed by the time we get to this call
  static Reader makeFilteredReader(Reader reader) {
    /* comments contain dates specific stuff, like
     * <!--totalCount14--><!--modified:1368461028000-->
     * we aren't currently using comments for any other search/replace, so just remove them all
     */
    List tagList = ListUtil.list(
        // Remove DOCTYPE declaration which seems to vary but is not a node in the DOM
        new TagPair("<!--","-->")
        );
    Reader tagFilter = HtmlTagFilter.makeNestedFilter(reader, tagList);

    /*
     *  Now remove tags with a single white space in between them
     */
    String[][] findAndReplace = new String[][] { 
        // remove empty space between tags
        {"> <", "><"},
        // remove leading space after tags (extra spaces will already have been consolidated down to one 
        {"> ", ">"},
    };
    Reader stringFilter = StringFilter.makeNestedFilter(tagFilter,
        findAndReplace, false);
    return stringFilter;
  }
}

