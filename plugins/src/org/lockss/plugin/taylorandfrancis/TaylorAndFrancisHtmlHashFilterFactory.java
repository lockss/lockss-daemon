/*
 * $Id: TaylorAndFrancisHtmlHashFilterFactory.java,v 1.24 2015-01-27 22:30:13 thib_gc Exp $
 */

/*

Copyright (c) 2000-2015 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.plugin.taylorandfrancis;

import java.io.*;
import java.util.*;

import org.apache.commons.io.IOUtils;
import org.htmlparser.*;
import org.htmlparser.filters.*;
import org.htmlparser.tags.CompositeTag;
import org.lockss.daemon.PluginException;
import org.lockss.filter.*;
import org.lockss.filter.HtmlTagFilter.TagPair;
import org.lockss.filter.StringFilter;
import org.lockss.filter.html.*;
import org.lockss.plugin.*;
import org.lockss.util.*;

/*STANDALONE - DOES NOT INHERIT FROM BASE ATYPON */
public class TaylorAndFrancisHtmlHashFilterFactory implements FilterFactory {

  private static final Logger log = Logger.getLogger(TaylorAndFrancisHtmlHashFilterFactory.class);
  
  @Override
  public InputStream createFilteredInputStream(ArchivalUnit au,
                                               InputStream in,
                                               String encoding)
      throws PluginException {
    NodeFilter[] filters = new NodeFilter[] {
        
        /*
         * From the crawl filter
         */
        
        // News articles
        HtmlNodeFilters.tagWithAttribute("div", "id", "newsArticles"),
        // Related and most read articles
        HtmlNodeFilters.tagWithAttribute("div", "id", "relatedArticles"),
        //Ad module
        HtmlNodeFilters.tagWithAttribute("div", "class", "ad module"),
        // Links to other articles
        HtmlNodeFilters.tagWithAttribute("div", "id", "referencesPanel"),
        HtmlNodeFilters.tagWithAttribute("div", "id", "citationsPanel"),
        
        /*
         * Broad area filtering
         */
        
        // Scripts, style, comments
        HtmlNodeFilters.tag("script"),
        HtmlNodeFilters.tag("noscript"),
        HtmlNodeFilters.tag("style"),
        HtmlNodeFilters.comment(),
        // Document header
        HtmlNodeFilters.tag("head"),
        // Header
        HtmlNodeFilters.tagWithAttribute("div", "id", "hd"),
        // EU cookie notification
        HtmlNodeFilters.tagWithAttribute("div",  "id", "cookieBanner"),
        // Top area
        HtmlNodeFilters.tagWithAttributeRegex("div", "id", "^primarySubjects"),
        // Breadcrumb
        HtmlNodeFilters.tagWithAttribute("div",  "id", "breadcrumb"),
        // Left column
        HtmlNodeFilters.tagWithAttribute("div",  "id", "unit1"),
        // Right column
        HtmlNodeFilters.tagWithAttribute("div",  "id", "unit3"),
        // Bottom area (alternatives)
        HtmlNodeFilters.tagWithAttributeRegex("div", "id", "^secondarySubjects"), 
        HtmlNodeFilters.tagWithAttributeRegex("div", "class", "^secondarySubjects"),
        // Footer
        HtmlNodeFilters.tagWithAttribute("div", "id", "ft"),
        
        /*
         * Main content area
         */
        // Box with "View full text", "Download full text" etc. changes when
        // article becomes free
        HtmlNodeFilters.tagWithAttributeRegex("div", "class", "accessmodule"),
        HtmlNodeFilters.tagWithAttributeRegex("div", "class", "accessIconWrapper"), // redundant: enclosed in previous
        // Contains an article view count
        HtmlNodeFilters.tagWithAttribute("div", "class", "articleUsage"),
        // "Alert me" (varied over time)
        HtmlNodeFilters.tagWithAttributeRegex("div", "id", "alertDiv"),
        HtmlNodeFilters.tagWithAttributeRegex("div", "class", "alertDiv"),
        
        /*
         * Other
         */
        
        // TOCs above content area: icons like "peer review integrity" and
        // "Routledge Open Select" can change over time
        HtmlNodeFilters.tagWithAttributeRegex("div", "class", "journalOverviewAds"),
//        // Added later to TOCs, as of this writing seemingly empty
//        HtmlNodeFilters.tagWithAttribute("span", "class", "subtitles"),
        // Wording in TOCs changed from 'Related' to ' Related articles ' [sic]
        HtmlNodeFilters.tagWithAttributeRegex("a", "href", "/action/doSearch\\?target=related"),
        // Maximal filtering. Take out non-content sections proactively
        //top search area - can have addt'l words
        HtmlNodeFilters.tagWithAttribute("div", "class", "social clear"), // facebook/twitter etc 
        // Google Translate related stuff
        HtmlNodeFilters.tagWithAttribute("div", "id", "google_translate_element"),
        HtmlNodeFilters.tagWithAttribute("div", "id", "goog-gt-tt"),
        HtmlNodeFilters.tagWithText("a", "Translator disclaimer"),
        HtmlNodeFilters.tagWithText("a", "Translator&nbsp;disclaimer"),
        // Variants of "Search related articles" tag sequence, added later 
        HtmlNodeFilters.tagWithText("h3", "Related articles"),
        HtmlNodeFilters.tagWithAttribute("a", "class", "relatedLink"),
        HtmlNodeFilters.tagWithAttribute("a", "class", "searchRelatedLink"),
        HtmlNodeFilters.tagWithAttribute("li", "class", "relatedArticleLink"), // TOC in 'Further Information': wording change
        // TOC: wording change
        HtmlNodeFilters.tagWithText("a", "Sample copy"),
        HtmlNodeFilters.tagWithText("a", "Sample this title"),
        // List of outside venues for each Reference changes over time
        // (Web of Science, Crossref, site-specific SFX, etc.)
        // See also the string filter below that addresses the leftover commas
        HtmlNodeFilters.tagWithAttribute("span", "class", "referenceDiv"), // hidden popup at each inline citation
        HtmlNodeFilters.tagWithAttributeRegex("a", "href", "^/servlet/linkout\\?"),
        HtmlNodeFilters.tagWithAttribute("a", "class", "sfxLink"),
        // Tabular data used to be inline, is now separate (like a Figure)
        HtmlNodeFilters.tagWithAttribute("div", "class", "NLM_table-wrap"), // embedded tabular data
        HtmlNodeFilters.tagWithAttribute("center", "class", "fulltext"), // external tabular data
        
        /*
         * ?? 
         */
        
        // what follows here may no longer be all needed due to the maximal filtering - but leave it in
        // Contains a cookie or session ID
        HtmlNodeFilters.tagWithAttributeRegex("a", "href", "&feed=rss"),
        // These two are sometimes found in a temporary overlay
        HtmlNodeFilters.tagWithAttribute("div", "id", "overlay"),
        HtmlNodeFilters.tagWithAttribute("div", "class", "overlay clear overlayHelp"),
        // Two alternative versions of the same empty section
        HtmlNodeFilters.tagWithAttribute("a", "id", "fpi"),
        HtmlNodeFilters.tagWithAttribute("div", "id", "fpi"),
        // Newer placeholder
        HtmlNodeFilters.tagWithAttribute("li", "id", "citationsTab"),
        // Too much subtly variable markup to keep
        HtmlNodeFilters.tagWithAttributeRegex("div", "class", "^doiMeta"),
        // Added later
        HtmlNodeFilters.tagWithAttribute("input", "id", "singleHighlightColor"),
    };
    
    InputStream filtered = new HtmlFilterInputStream(in,
                                                     encoding,
                                                     HtmlNodeFilterTransform.exclude(new OrFilter(filters)))
    .registerTag(new CompositeTag() { @Override public String[] getIds() { return new String[] {"CENTER"}; } }); // FIXME 1.67.4
    
    Reader reader = FilterUtil.getReader(filtered, encoding);

    Reader stringFilter = StringFilter.makeNestedFilter(reader,
                                                        new String[][] {
        // Typographical changes over time
        {"&nbsp;", " "},
        {"&amp;", "&"},
        // Wording change over time
        {"<strong>Published online:</strong>"},
        {"<strong>Available online:</strong>"},
        {"<strong>Version of record first published:</strong>"}
    }, true);

    Reader tagFilter = HtmlTagFilter.makeNestedFilter(stringFilter,
                                                      Arrays.asList(
        // Alternate forms of citation links (no easy to characterize in the DOM)
        new TagPair("<li><strong>Citations:", "</li>", true),
        new TagPair("<li><strong><a href=\"/doi/citedby/", "</li>", true),
        new TagPair("<li><strong>Citation information:", "</li>", true),
        new TagPair("<li><div><strong>Citing Articles:", "</li>", true),
        // List of outside venues for References changes over time
        // (Web of Science, Crossref, site-specific SFX...)
        // Tags removed above but commas are left over afterwards
        // This is about the best we can do in this situation
        new TagPair("</pub-id>", "</li>", true)
    ));
    
    // Remove all inner tag content
    Reader noTagFilter = new HtmlTagFilter(tagFilter, new TagPair("<", ">"));
    
    // Remove white space
    return new ReaderInputStream(new WhiteSpaceFilter(noTagFilter));
  }

//  public static void main(String[] args) throws Exception {
//    for (String file : Arrays.asList("/tmp/w6/file-b1", "/tmp/w6/file-b4")) {
//      IOUtils.copy(new TaylorAndFrancisHtmlHashFilterFactory().createFilteredInputStream(null, new FileInputStream(file), null),
//                   new FileOutputStream(file + ".out"));
//    }
//  }
  
}
