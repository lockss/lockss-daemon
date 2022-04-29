/*

Copyright (c) 2000-2022, Board of Trustees of Leland Stanford Jr. University

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are met:

1. Redistributions of source code must retain the above copyright notice,
this list of conditions and the following disclaimer.

2. Redistributions in binary form must reproduce the above copyright notice,
this list of conditions and the following disclaimer in the documentation
and/or other materials provided with the distribution.

3. Neither the name of the copyright holder nor the names of its contributors
may be used to endorse or promote products derived from this software without
specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
POSSIBILITY OF SUCH DAMAGE.

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
 * Previously: Didn't extend BaseAtyponHtmlHashFilterFactory
 * 
 * Now: Extends and does more extensive filtering with spaces, etc.
 */
public class AMetSocHtmlHashFilterFactory extends BaseAtyponHtmlHashFilterFactory {

  private static final Logger log = Logger.getLogger(AMetSocHtmlHashFilterFactory.class);

  @Override
  public InputStream createFilteredInputStream(ArchivalUnit au,
      InputStream in,
      String encoding) {
    NodeFilter[] filters = new NodeFilter[] {
        // May be empty, may contain "free" gif if appropriate
        HtmlNodeFilters.tagWithAttribute("div", "class", "accessLegend"),
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
        
        //From CRAWL filter
        //next-prev on article page
        HtmlNodeFilters.tagWithAttribute("div", "class", "navigationLinkHolder"),
        //stuff in the right column - might also be filtered as literatumAd
        // but for unmarked stuff (Featured Collections)
        HtmlNodeFilters.tagWithAttributeRegex("div", "class", "rightColumnModule"),
        //TOC - tab for special collections
        HtmlNodeFilters.tagWithAttributeRegex("div", "class", "topicalIndex"),
    };

    // super will add in the base nodeset and also do span-id transform
    InputStream superFiltered = super.createFilteredInputStream(au, in, encoding, filters);

    // Also need white space filter to condense multiple white spaces down to 1
    Reader reader = FilterUtil.getReader(superFiltered, encoding);

    // first substitute plain white space for &nbsp;
    String[][] unifySpaces = new String[][] { 
        // inconsistent use of nbsp v empty space - do this replacement first
        {"&nbsp;", " "}, 
    };
    Reader NBSPFilter = StringFilter.makeNestedFilter(reader, unifySpaces, false);

    //now consolidate white space before doing additional tagfilter stuff
    Reader WSReader = new WhiteSpaceFilter(NBSPFilter);
    Reader filtReader = makeFilteredReader(WSReader);

    // jeez. bogus occasional addition of trailing space in "ref nowrap "
    String[][] findAndReplace = new String[][] {
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

