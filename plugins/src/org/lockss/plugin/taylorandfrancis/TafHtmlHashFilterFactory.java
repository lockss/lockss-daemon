/*
 * $Id$
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
import java.util.Arrays;

import org.apache.commons.io.IOUtils;
import org.apache.commons.io.input.CountingInputStream;
import org.htmlparser.NodeFilter;
import org.htmlparser.filters.OrFilter;
import org.htmlparser.tags.CompositeTag;
import org.lockss.daemon.PluginException;
import org.lockss.filter.*;
import org.lockss.filter.HtmlTagFilter.TagPair;
import org.lockss.filter.html.*;
import org.lockss.plugin.*;
import org.lockss.util.*;

/**
 * This filter will eventually replace
 * {@link TaylorAndFrancisHtmlHashFilterFactory}.
 */
public class TafHtmlHashFilterFactory implements FilterFactory {

  private static final Logger log = Logger.getLogger(TafHtmlHashFilterFactory.class);
  
  @Override
  public InputStream createFilteredInputStream(final ArchivalUnit au,
                                               InputStream in,
                                               String encoding)
      throws PluginException {

    HtmlFilterInputStream filtered = new HtmlFilterInputStream(
      in,
      encoding,
      new HtmlCompoundTransform(
        /*
         * KEEP: throw out everything but main content areas
         */
        HtmlNodeFilterTransform.include(new OrFilter(new NodeFilter[] {
            // KEEP manifest page elements (no distinctive characteristics) [manifest]
            HtmlNodeFilters.tagWithAttributeRegex("a", "href", "^(https?://[^/]+)?/toc/[^/]+/[^/]+/[^/]+/?$"),
            // KEEP top part of main content area [TOC, abs, full, ref]
            HtmlNodeFilters.tagWithAttributeRegex("div", "class", "overview"),
            // KEEP each article block [TOC]
            HtmlNodeFilters.tagWithAttributeRegex("div", "class", "\\barticle\\b"), // avoid match on pageArticle
            // KEEP abstract and preview [abs, full, ref]
            HtmlNodeFilters.tagWithAttributeRegex("div", "class", "abstract"),
            HtmlNodeFilters.tagWithAttribute("div", "id", "preview"),
            // KEEP active content area [abs, full, ref, suppl]
            HtmlNodeFilters.tagWithAttribute("div", "id", "informationPanel"), // article info [abs]
            HtmlNodeFilters.tagWithAttribute("div", "id", "fulltextPanel"), // full text [full]
            HtmlNodeFilters.tagWithAttribute("div", "id", "referencesPanel"), // references [ref]
            HtmlNodeFilters.tagWithAttribute("div", "id", "supplementaryPanel"), // supplementary materials [suppl]
            // KEEP citation format form [showCitFormats]
            HtmlNodeFilters.tagWithAttributeRegex("div", "class", "citationContainer"),
            // KEEP popup window content area [showPopup]
            HtmlNodeFilters.tagWithAttribute("body", "class", "popupBody"),
        })),
        /*
         * DROP: filter remaining content areas
         */
        HtmlNodeFilterTransform.exclude(new OrFilter(new NodeFilter[] {
            // DROP scripts, styles, comments
            HtmlNodeFilters.tag("script"),
            HtmlNodeFilters.tag("noscript"),
            HtmlNodeFilters.tag("style"),
            HtmlNodeFilters.comment(),
            // DROP social media bar [overview]
            HtmlNodeFilters.tagWithAttributeRegex("div", "class", "social"),
            // DROP access box (changes e.g. when the article becomes free) [article block, abs/full/ref/suppl overview]
            HtmlNodeFilters.tagWithAttributeRegex("div", "class", "accessmodule"),
            // DROP number of article views [article block, abs/full/ref/suppl overview]
            HtmlNodeFilters.tagWithAttribute("div", "class", "articleUsage"),
            // DROP "Related articles" variants [article block, abs/full/ref/suppl overview]
            HtmlNodeFilters.tagWithAttribute("a", "class", "relatedLink"), // old?
            HtmlNodeFilters.tagWithAttribute("li", "class", "relatedArticleLink"), // [article block]
            HtmlNodeFilters.tagWithText("h3", "Related articles"), // [abs/full/ref/suppl overview]
            HtmlNodeFilters.tagWithAttribute("a", "class", "searchRelatedLink"), // [abs/full/ref/suppl overview]
            // DROP title options (e.g. 'Publication History', 'Sample this title') [TOC overview]
            HtmlNodeFilters.tagWithAttribute("div", "class", "options"),
            // DROP title icons (e.g. 'Routledge Open Select') [TOC overview]
            HtmlNodeFilters.tagWithAttributeRegex("div", "class", "journalOverviewAds"),
            // DROP Google Translate artifacts [abs/full/ref/suppl overview]
            HtmlNodeFilters.tagWithAttribute("div", "id", "google_translate_element"), // current
            HtmlNodeFilters.tagWithAttribute("div", "id", "goog-gt-tt"), // old
            HtmlNodeFilters.tagWithText("a", "Translator disclaimer"),
            HtmlNodeFilters.tagWithText("a", "Translator&nbsp;disclaimer"),
            // DROP "Alert me" variants [abs/full/ref overview]
            HtmlNodeFilters.tagWithAttributeRegex("div", "class", "alertDiv"), // current
            HtmlNodeFilters.tagWithAttributeRegex("div", "id", "alertDiv"), // old
            // DROP "Publishing models and article dates explained" link [abs/full/ref/suppl overview]
            HtmlNodeFilters.tagWithAttributeRegex("a", "href", "models-and-dates-explained"),
            // DROP outgoing links and SFX links [article block, full, ref]
            HtmlNodeFilters.allExceptSubtree(HtmlNodeFilters.tagWithAttribute("span", "class", "referenceDiv"),
                                             HtmlNodeFilters.tagWithAttribute("a", "class", "dropDownLabel")), // popup at each inline citation [full]
            HtmlNodeFilters.tagWithAttributeRegex("a", "href", "^/servlet/linkout\\?"), // [article block, full/ref referencesPanel]
            HtmlNodeFilters.tagWithAttribute("a", "class", "sfxLink"), // [article block, full/ref referencesPanel]
            // DROP "Jump to section" popup menus [full]
            HtmlNodeFilters.tagWithAttributeRegex("div", "class", "summationNavigation"),
        }))
      )
    );
    
    // FIXME 1.67.4
    filtered.registerTag(new CompositeTag() { @Override public String[] getIds() { return new String[] {"CENTER"}; } });
    
    Reader reader = FilterUtil.getReader(filtered, encoding);

    Reader stringFilter = StringFilter.makeNestedFilter(reader,
                                                        new String[][] {
        // Markup changes over time
        {"&nbsp;", " "},
        {"&amp;", "&"},
        // Wording change over time [article block, abs/full/ref/suppl overview]
        {"<strong>Published online:</strong>"}, // current
        {"<strong>Available online:</strong>"}, // old
        {"<strong>Version of record first published:</strong>"} // old
    }, true);

    Reader tagFilter = HtmlTagFilter.makeNestedFilter(stringFilter,
                                                      Arrays.asList(
        // Alternate forms of citation links [article block]
        new TagPair("<li><div><strong>Citing Articles:", "</li>", true), // current
        new TagPair("<li><strong>Citations:", "</li>", true), // old?
        new TagPair("<li><strong><a href=\"/doi/citedby/", "</li>", true), // old?
        new TagPair("<li><strong>Citation information:", "</li>", true), // old?
        // Leftover commas after outgoing/SFX links removed [full/ref referencesPanel]
        new TagPair("</pub-id>", "</li>", true)
    ));
    
    // Remove all inner tag content
    Reader noTagFilter = new HtmlTagFilter(new StringFilter(tagFilter, "<", " <"), new TagPair("<", ">"));
    
    // Remove white space
    Reader noWhiteSpace = new WhiteSpaceFilter(noTagFilter);
    
    InputStream ret = new ReaderInputStream(noWhiteSpace);

    // Instrumentation
    return new CountingInputStream(ret) {
      @Override
      public void close() throws IOException {
        long bytes = getByteCount();
        if (bytes <= 100L) {
          log.debug(String.format("%d byte%s in %s", bytes, bytes == 1L ? "" : "s", au.getName()));
        }
        if (log.isDebug2()) {
          log.debug2(String.format("%d byte%s in %s", bytes, bytes == 1L ? "" : "s", au.getName()));
        }
        super.close();
      }
    };
  }

//  public static void main(String[] args) throws Exception {
//    for (String file : Arrays.asList("/tmp/e1/file-b1",
//                                     "/tmp/e1/file-b4")) {
//      IOUtils.copy(new TafHtmlHashFilterFactory().createFilteredInputStream(null, new FileInputStream(file), null),
//                   new FileOutputStream(file + ".out"));
//    }
//  }

}
