/*
 * $Id: TaylorAndFrancisHtmlHashFilterFactory.java,v 1.23 2015-01-22 04:32:47 thib_gc Exp $
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
         * Other
         */
        // TOCs above content area: icons like "peer review integrity" and
        // "Routledge Open Select" can change over time
        HtmlNodeFilters.tagWithAttributeRegex("div", "class", "journalOverviewAds"),
//        // Added later to TOCs, as of this writing seemingly empty
//        HtmlNodeFilters.tagWithAttribute("span", "class", "subtitles"),
        // Wording in TOCs changed from 'Related' to ' Related articles ' [sic]
        HtmlNodeFilters.tagWithAttributeRegex("a", "href", "/action/doSearch\\?target=related"),
//        // Went from <..."/> to <..." />
//        HtmlNodeFilters.tagWithAttribute("img", "class", "cover"),
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
        // (no longer needed because we no longer get Refworks citation files)
//        // Form at the bottom of downloadCitation pages with RIS data in a <textarea>;
//        // the RIS data itself has a Y2 tag that changes at every fetch
//        HtmlNodeFilters.tagWithAttribute("form", "name", "refworks"),
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
        // Contains a variant phrase "Full access" or "Free access"
        HtmlNodeFilters.tagWithAttributeRegex("div", "class", "accessIconWrapper"),
        // (no longer needed because we strip out inner tag content)
//        // These two equivalent links are found (this is not a regex)
//        HtmlNodeFilters.tagWithAttribute("a", "href", "/"),
//        HtmlNodeFilters.tagWithAttribute("a", "href", "http://www.tandfonline.com"),
        // These two are sometimes found in a temporary overlay
//        // (also requires whitespace normalization to work)
        HtmlNodeFilters.tagWithAttribute("div", "id", "overlay"),
        HtmlNodeFilters.tagWithAttribute("div", "class", "overlay clear overlayHelp"),
        // "Alert me" (varied over time)
        HtmlNodeFilters.tagWithAttributeRegex("div", "id", "alertDiv"),
        HtmlNodeFilters.tagWithAttributeRegex("div", "class", "alertDiv"),
        // Two alternative versions of the same empty section
        HtmlNodeFilters.tagWithAttribute("a", "id", "fpi"),
        HtmlNodeFilters.tagWithAttribute("div", "id", "fpi"),
        // Newer placeholder
        HtmlNodeFilters.tagWithAttribute("li", "id", "citationsTab"),
//        // Some <h4> tags had/have a 'class' attribute
//        new TagNameFilter("h4"),
        // Contains an article view count
        HtmlNodeFilters.tagWithAttribute("div", "class", "articleUsage"),
        // Too much subtly variable markup to keep
        HtmlNodeFilters.tagWithAttributeRegex("div", "class", "^doiMeta"),
        // Added later
        HtmlNodeFilters.tagWithAttribute("input", "id", "singleHighlightColor"),
    };
    
    // (no longer needed because we strip out inner tag content)
//    HtmlTransform xform = new HtmlTransform() {
//      @Override
//      public NodeList transform(NodeList nodeList) throws IOException {
//        try {
//          nodeList.visitAllNodesWith(new NodeVisitor() {
//            @Override
//            public void visitTag(Tag tag) {
//              if (tag instanceof CompositeTag) {
//                CompositeTag ctag = (CompositeTag)tag;
//                Vector<Attribute> vec = new Vector<Attribute>();
//                vec.add(new Attribute(ctag.getRawTagName(), null, '\0'));
//                tag.setAttributesEx(vec);
//              }
////              try {
////                String tagName = tag.getTagName().toLowerCase();
////                switch (tagName.charAt(0)) {
////                  case 'a': {
////                    /*
////                     * <a>
////                     */
////                    if ("a".equals(tagName)) {
////                      // The anchor ID 'content' was renamed 'tandf_content'
////                      if ("#content".equals(tag.getAttribute("href"))) {
////                        tag.setAttribute("href", "#tandf_content");
////                        return;
////                      }
////                      // Inline links used to have class="ref" and target="url",
////                      // now only target="_blank"
////                      if (tag.getAttribute("target") != null) {
////                        tag.removeAttribute("target");
////                      }
////                      if ("ref".equals(tag.getAttribute("class"))) {
////                        tag.removeAttribute("class");
////                      }
////                      return;
////                    }
////                  } break;
////                  case 'd': {
////                    /*
////                     * <div>
////                     */
////                    if ("div".equals(tagName)) {
////                      String clas = tag.getAttribute("class");
////                      if (clas != null && clas.startsWith("access ")) {
////                        // ??
////                        tag.removeAttribute("class");
////                        return;
////                      }
////                      // From <div class="bodyFooterContent clear floatclear"> 
////                      // to <div class="bodyFooterContent clear floatclear" style="margin-bottom: -15px"> 
////                      if (clas != null && clas.contains("bodyFooterContent")) {
////                        if (tag.getAttribute("style") != null) {
////                          tag.removeAttribute("style");
////                        }
////                        // Prevent the latter from causing spurious <.../> vs. <... />
////                        tag.removeAttribute("class");
////                        tag.setAttribute("class", clas);
////                        return;
////                      }
////                      // For a while, there were two <div> tags with 'id' set to 'content'
////                      // Now clarified to 'journal_content' and 'tandf_content'
////                      if ("journal_content".equals(tag.getAttribute("id")) || "tandf_content".equals(tag.getAttribute("id"))) {
////                        tag.setAttribute("id", "content");
////                        return;
////                      }
////                    }
////                  } break;
////                  case 'i': {
////                    /*
////                     * <img>
////                     */
////                    // Images used to have an ID
////                    if ("img".equals(tagName)) {
////                      if (tag.getAttribute("id") != null) {
////                        tag.removeAttribute("id");
////                      }
////                      // Prevent the latter from causing spurious <.../> vs. <... />
////                      String alt = tag.getAttribute("alt");
////                      tag.removeAttribute("alt");
////                      tag.setAttribute("alt", alt);
////                      return;
////                    }
////                  } break;
////                  case 's': {
////                    /*
////                     * <span>
////                     */
////                    if ("span".equals(tagName)) {
////                      if (tag.getAttribute("id") != null) {
////                        tag.removeAttribute("id");
////                      }
////                      return;
////                    }
////                  } break;
////                  case 't': {
////                    /*
////                     * <table>
////                     */
////                    if ("table".equals(tagName)) {
////                      // From <table class="listgroup " border="0" width="95%">
////                      // to <table xmlns:mml="http://www.w3.org/1998/Math/MathML" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" class="listgroup " border="0" width="95%">
////                      if (tag.getAttribute("xmlns:mml") != null) {
////                        tag.removeAttribute("xmlns:mml");
////                      }
////                      if (tag.getAttribute("xmlns:xsi") != null) {
////                        tag.removeAttribute("xmlns:xsi");
////                      }
////                      return;
////                    }
////                    /*
////                     * <td>
////                     */
////                    if ("td".equals(tagName)) {
////                      // From <td valign="top">
////                      // to <td valign="top" class="lilabel">
////                      if ("top".equals(tag.getAttribute("valign"))) {
////                        if ("lilabel".equals(tag.getAttribute("class"))) {
////                          tag.removeAttribute("class");
////                        }
////                        tag.removeAttribute("valign");
////                        tag.setAttribute("valign", "top");
////                      }
////                      return;
////                    }
////                  } break;
////                  case 'u': {
////                    /*
////                     * <ul>
////                     */
////                    if ("ul".equals(tagName)) {
////                      // Varied between <ul> and <ul style="clear:both">
////                      if (tag.getAttribute("style") != null) {
////                        tag.removeAttribute("style");
////                      }
////                      return;
////                    }
////                  } break;
////                }
////                // Still here: tag has not been visited
////                super.visitTag(tag);
////              }
////              catch (Exception exc) {
////                log.debug2("Internal error (visitor)", exc); // Ignore this tag and move on
////              }
//            }
//          });
//        }
//        catch (ParserException pe) {
//          log.debug2("Internal error (parser)", pe); // Bail
//        }
//        return nodeList;
//      }
//    };
    
    InputStream filtered = new HtmlFilterInputStream(in,
                                                     encoding,
                                                     HtmlNodeFilterTransform.exclude(new OrFilter(filters)))
    .registerTag(new CompositeTag() { @Override public String[] getIds() { return new String[] {"CENTER"}; } }); // FIXME 1.67.4
    
    Reader reader = FilterUtil.getReader(filtered, encoding);

    Reader stringFilter = StringFilter.makeNestedFilter(reader,
                                                        new String[][] {
        // Typographical changes over time
        {"&nbsp;", " "},
//        {"  ,", ","}, // in inline citations
//        {" ,", ","}, // in inline citations
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
    
    // (no longer needed because we strip out inner tag content)
//    stringFilter = new StringFilter(stringFilter,
//                                    "</div><div",
//                                    "</div> <div");

    // Remove all inner tag content
//    Reader noTagFilter = new HtmlTagFilter(new StringFilter(tagFilter, "<", " <"), new TagPair("<", ">"));
    Reader noTagFilter = new HtmlTagFilter(tagFilter, new TagPair("<", ">"));
    
    // Remove white space
    return new ReaderInputStream(new WhiteSpaceFilter(noTagFilter));
  }

  public static void main(String[] args) throws Exception {
    String file = "/tmp/w4/file-b1";
    IOUtils.copy(new TaylorAndFrancisHtmlHashFilterFactory().createFilteredInputStream(null, new FileInputStream(file), null),
                 new FileOutputStream(file + ".out"));
  }
  
}
