/*
 * $Id: HighWireHtmlFilterFactory.java,v 1.12 2013-12-13 04:20:02 etenbrink Exp $
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

import org.htmlparser.*;
import org.htmlparser.filters.*;
import org.lockss.util.*;
import org.lockss.filter.*;
import org.lockss.filter.html.*;
import org.lockss.plugin.*;

public class HighWireHtmlFilterFactory implements FilterFactory {
  // Remove everything on the line after these comments
  static HtmlTagFilter.TagPair[] tagpairs = {
    new HtmlTagFilter.TagPair("<STRONG>Institution:", "</A>", true),
    new HtmlTagFilter.TagPair("<A NAME=\"relation_type_", "</HTML>",
                              true, false),
    new HtmlTagFilter.TagPair("<A NAME=\"otherarticles\">", "</HTML>"),
    new HtmlTagFilter.TagPair("<", ">"),
  };
  static List<HtmlTagFilter.TagPair> tagList = ListUtil.fromArray(tagpairs);

  public InputStream createFilteredInputStream(ArchivalUnit au,
					       InputStream in,
					       String encoding) {

    NodeFilter[] filters = new NodeFilter[] {
        // filter head for Red Book
        new TagNameFilter("head"),
        // Contains variable ad-generating code
        new TagNameFilter("script"),
        // Contains variable ad-generating code
        new TagNameFilter("noscript"),
        // Typically contains ads (e.g. American Academy of Pediatrics)
        new TagNameFilter("object"),
        // Typically contains ads 
        new TagNameFilter("iframe"),
        // Contains ads (e.g. American Medical Association)
        HtmlNodeFilters.tagWithAttribute("div", "id", "advertisement"),
        HtmlNodeFilters.tagWithAttribute("div", "id", "authenticationstring"),
        // Contains institution name (e.g. SAGE Publications)
        HtmlNodeFilters.tagWithAttribute("div", "id", "universityarea"),
        // Contains institution name (e.g. Oxford University Press)
        HtmlNodeFilters.tagWithAttribute("div", "id", "inst_logo"),
        // Contains institution name (e.g. American Medical Association)
        HtmlNodeFilters.tagWithAttribute("p", "id", "UserToolbar"),
        HtmlNodeFilters.tagWithAttribute("div", "id", "user_nav"),
        HtmlNodeFilters.tagWithAttribute("table", "class", "content_box_inner_table"),
        HtmlNodeFilters.tagWithAttribute("a", "class", "contentbox"),
        HtmlNodeFilters.tagWithAttribute("div", "id", "ArchivesNav"),
        // tagWithText creates a CompositeStringFilter, which requires the node 
        // to be an instanceof CompositeTag, <strong> is not a CompositeTag, 
        //   HtmlNodeFilters.tagWithText("strong", "related", true),
        HtmlNodeFilters.lowestLevelMatchFilter(HtmlNodeFilters.tagWithText("table", "Related Content", false)),
        // Contains the current year (e.g. Oxford University Press)
        HtmlNodeFilters.tagWithAttribute("div", "id", "copyright"),
        // Contains the current year (e.g. SAGE Publications)
        HtmlNodeFilters.tagWithAttribute("div", "id", "footer"),
        // Contains the current date and time (e.g. American Medical Association)
        HtmlNodeFilters.tagWithAttribute("a", "target", "help"),
        // Contains the name and date of the current issue (e.g. Oxford University Press)
        HtmlNodeFilters.tagWithAttribute("li", "id", "nav_current_issue"),
        // Contains ads or variable banners (e.g. Oxford University Press)
        HtmlNodeFilters.tagWithAttribute("div", "id", "oas_top"),
        // Contains ads or variable banners (e.g. Oxford University Press)
        HtmlNodeFilters.tagWithAttribute("div", "id", "oas_bottom"),
        // Optional institution-specific citation resolver (e.g. SAGE Publications)
        HtmlNodeFilters.tagWithAttributeRegex("a", "href", "^/cgi/openurl"),
        // Contains ad-dependent URLs (e.g. American Academy of Pediatrics)
        HtmlNodeFilters.tagWithAttributeRegex("a", "href", "^http://ads.adhostingsolutions.com/"),
        // alt for less/greater than confuses WhiteSpace filter
        HtmlNodeFilters.tagWithAttributeRegex("img", "alt", "[<>]"),
        //CMAJ (c)year tag
        HtmlNodeFilters.tagWithAttribute("div", "class", "slugline-copyright"),
        // filter ads & institution for Red Book
        HtmlNodeFilters.tagWithAttribute("div", "class", "leaderboard-ad"),
        HtmlNodeFilters.tagWithAttribute("div", "id", "header"),
        
    };


    // First filter with HtmlParser
    OrFilter orFilter = new OrFilter(filters);
    InputStream filtered = new HtmlFilterInputStream(in,
                                                     encoding,
                                                     HtmlNodeFilterTransform.exclude(orFilter));

    // Then filter with HighWireFilterRule
    Reader rdr = FilterUtil.getReader(filtered, encoding);
    Reader tagFilter = HtmlTagFilter.makeNestedFilter(rdr, tagList);
    return new ReaderInputStream(new WhiteSpaceFilter(tagFilter));
  }

}

