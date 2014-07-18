/*
 * $Id: ManeyAtyponHtmlHashFilterFactory.java,v 1.4.2.2 2014-07-18 15:54:41 wkwilson Exp $
 */

/*

Copyright (c) 2000-2014 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.plugin.atypon.maney;

import java.io.InputStream;
import java.io.Reader;
import org.htmlparser.NodeFilter;
import org.htmlparser.filters.TagNameFilter;
import org.lockss.filter.FilterUtil;
import org.lockss.filter.WhiteSpaceFilter;
import org.lockss.filter.html.*;
import org.lockss.plugin.*;
import org.lockss.plugin.atypon.BaseAtyponHtmlHashFilterFactory;
import org.lockss.util.Logger;
import org.lockss.util.ReaderInputStream;

/* 
 * Don't extend BaseAtyponHtmlHashFilterFactory because we need to do more 
 * extensive filtering with spaces, etc.
 */
public class ManeyAtyponHtmlHashFilterFactory extends BaseAtyponHtmlHashFilterFactory {

  Logger log = Logger.getLogger(ManeyAtyponHtmlHashFilterFactory.class);
  
  @Override
  public InputStream createFilteredInputStream(ArchivalUnit au,
      InputStream in,
      String encoding) {
    NodeFilter[] filters = new NodeFilter[] {
        // div class="citedBySection" handled in BaseAtypon
        // sfxlink handled in BaseAtypon
        
        // this is controversial - draconian; what about updated metadata
        new TagNameFilter("head"),
        // head section & banner of page
        HtmlNodeFilters.tagWithAttribute("section",  "id", "pageHeader"),
        // footer section and copyright on page
        HtmlNodeFilters.tagWithAttribute("section",  "id", "pageFooter"),
        // this seems unused but may get turned on 
        HtmlNodeFilters.tagWithAttribute("div",  "id", "MathJax_Message"),
        // No obvious way to identify the entire right column
        // right column, top - picture of current journal, etc
        HtmlNodeFilters.tagWithAttribute("div",  "id", "compactJournalHeader"),
        // right column, "journal services"
        HtmlNodeFilters.tagWithAttribute("section",  "id", "migrated_information"),
        // right column, "For Authors"
        HtmlNodeFilters.tagWithAttribute("section",  "id", "migrated_forauthors"),
        // right column, "Related Content Search"
        HtmlNodeFilters.tagWithAttributeRegex("section",  "class", "literatumRelatedContentSearch"),
        // right column, tabs - most read, most cited, editor's choice
        // leave the tabs and header (no unique identifier), just remove contents
        HtmlNodeFilters.tagWithAttributeRegex("section", "class", "literatumMostReadWidget"),
        HtmlNodeFilters.tagWithAttributeRegex("section", "class", "literatumMostCitedWidget"),
        HtmlNodeFilters.tagWithAttributeRegex("section", "class", "publicationListWidget"),
        // ad placement
        HtmlNodeFilters.tagWithAttributeRegex("section", "class", "literatumAd"),

        //TOC - journal section with current
        HtmlNodeFilters.tagWithAttribute("div",  "id", "Journal Header"),
        // TOC - Prev/Next - probably not a problem, but not content either
        HtmlNodeFilters.tagWithAttributeRegex("section",  "class", "literatumBookIssueNavigation"),
        //TOC - access icon status of article
        HtmlNodeFilters.tagWithAttribute("td",  "class" ,"accessIconContainer"),
        //TOC - published on behalf of 
        HtmlNodeFilters.tagWithAttribute("div",  "id" ,"Society Logo"),
        // For the next three - no uniquely named chunk to allow removal of tabs
        // so just remove the contents within the tab blocks
        // in bottom TOC tabs block - news & alerts
        HtmlNodeFilters.tagWithAttribute("section",  "id", "migrated_news"),
        // in bottom TOC tabs block - about this journal
        HtmlNodeFilters.tagWithAttribute("section",  "id", "migrated_aims"),
        // in bottom TOC tabs block - editors & editorial board
        HtmlNodeFilters.tagWithAttribute("section",  "id", "migrated_editors"),
        // just under TOC issue information, select all access icons
        HtmlNodeFilters.tagWithAttributeRegex("ul", "class", "access-options"),
        // TOC - right column at the bottom - no identifiable way to remove
        // header statement, but can remove list of subjects
        HtmlNodeFilters.tagWithAttributeRegex("section",  "class",  "literatumSerialSubjects"),
      
        
        // HASHING ONLY - NOT CRAWLING
        // right column, "Article Tools" - not this article's citation info 
        HtmlNodeFilters.tagWithAttributeRegex("section", "class", "literatumArticleToolsWidget"),

    
    };

    // super.createFilteredInputStream adds maney filter to the baseAtyponFilters
    // and returns the filtered input stream using an array of NodeFilters that 
    // combine the two arrays of NodeFilters.
    InputStream superFiltered = super.createFilteredInputStream(au, in, encoding, filters);

    // Also need white space filter to condense multiple white spaces down to 1
    Reader reader = FilterUtil.getReader(superFiltered, encoding);
    return new ReaderInputStream(new WhiteSpaceFilter(reader));
  }

}




