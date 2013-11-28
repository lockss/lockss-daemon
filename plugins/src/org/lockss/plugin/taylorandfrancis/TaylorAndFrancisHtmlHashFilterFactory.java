/*
 * $Id: TaylorAndFrancisHtmlHashFilterFactory.java,v 1.14 2013-11-28 00:10:58 thib_gc Exp $
 */

/*

Copyright (c) 2000-2013 Board of Trustees of Leland Stanford Jr. University,
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

import org.htmlparser.*;
import org.htmlparser.filters.*;
import org.htmlparser.util.*;
import org.htmlparser.visitors.NodeVisitor;
import org.lockss.daemon.PluginException;
import org.lockss.filter.*;
import org.lockss.filter.HtmlTagFilter.TagPair;
import org.lockss.filter.StringFilter;
import org.lockss.filter.html.*;
import org.lockss.plugin.*;
import org.lockss.util.*;

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
        /*
         * Proper to the hash filter
         */
        // Contains site-specific SFX code
        new TagNameFilter("script"),
        // Can change over time
        new TagNameFilter("style"),
        // Went from Taylor &amp; Francis to Taylor & Francis
        new TagNameFilter("title"),
        /*
         * Broad area filtering
         */
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
        
        // Went from <..."/> to <..." />
        HtmlNodeFilters.tagWithAttribute("img", "class", "cover"),
        // Maximal filtering. Take out non-content sections proactively
        //top search area - can have addt'l words
        HtmlNodeFilters.tagWithAttribute("div", "class", "social clear"), // facebook/twitter etc 
        
        // Google translate related stuff - RU 4642 (not on all browsers)
        HtmlNodeFilters.tagWithAttribute("div", "id", "google_translate_element"),
        HtmlNodeFilters.tagWithAttribute("div", "id", "goog-gt-tt"),
        
        // what follows here may no longer be all needed due to the maximal filtering - but leave it in
        // Contains site-specific SFX markup
        HtmlNodeFilters.tagWithAttribute("a", "class", "sfxLink"),
        // Contains a cookie or session ID
        HtmlNodeFilters.tagWithAttribute("link", "type", "application/rss+xml"),
        // Contains a cookie or session ID
        HtmlNodeFilters.tagWithAttributeRegex("a", "href", "&feed=rss"),
        // Contains a variant phrase "Full access" or "Free access"
        HtmlNodeFilters.tagWithAttributeRegex("div", "class", "accessIconWrapper"),
        // Counterpart of the previous clause when there is no integrated SFX
        HtmlNodeFilters.tagWithAttributeRegex("a", "href", "^/servlet/linkout\\?"),
        // These two equivalent links are found (this is not a regex)
        HtmlNodeFilters.tagWithAttribute("a", "href", "/"),
        HtmlNodeFilters.tagWithAttribute("a", "href", "http://www.tandfonline.com"),
        // Spuriously versioned CSS URLs
        HtmlNodeFilters.tagWithAttribute("link", "rel", "stylesheet"),
        // Later addition of <link rel="meta" ...> tags
        HtmlNodeFilters.tagWithAttribute("link", "rel", "meta"),
        // These two are sometimes found in a temporary overlay
        // (also requires whitespace normalization to work)
        HtmlNodeFilters.tagWithAttribute("div", "id", "overlay"),
        HtmlNodeFilters.tagWithAttribute("div", "class", "overlay clear overlayHelp"),
        // Changed from 'id' to 'class'
        HtmlNodeFilters.tagWithAttributeRegex("div", "id", "alertDiv"),
        HtmlNodeFilters.tagWithAttributeRegex("div", "class", "alertDiv"),
        // Two alternative versions of the same empty section
        HtmlNodeFilters.tagWithAttribute("a", "id", "fpi"),
        HtmlNodeFilters.tagWithAttribute("div", "id", "fpi"),
        // These two sections are newer placeholders
        HtmlNodeFilters.tagWithAttribute("li", "id", "citationsTab"),
        HtmlNodeFilters.tagWithAttribute("div", "id", "citationsPanel"),
        // Some <h4> tags had/have a 'class' attribute
        new TagNameFilter("h4"),
        // Contains an article view count
        HtmlNodeFilters.tagWithAttribute("div", "class", "articleUsage"),
        // This feature was added later (search related articles)
        HtmlNodeFilters.tagWithAttribute("a", "class", "relatedLink"),
        // Too much subtly variable markup to keep
        HtmlNodeFilters.tagWithAttributeRegex("div", "class", "^doiMeta"),
        // Added later
        HtmlNodeFilters.tagWithAttribute("input", "id", "singleHighlightColor"),
    };
    
    HtmlTransform xform = new HtmlTransform() {
      @Override
      public NodeList transform(NodeList nodeList) throws IOException {
        try {
          nodeList.visitAllNodesWith(new NodeVisitor() {
            @Override
            public void visitTag(Tag tag) {
              try {
                String tagName = tag.getTagName().toLowerCase();
                switch (tagName.charAt(0)) {
                  case 'a': {
                    /*
                     * <a>
                     */
                    if ("a".equals(tagName)) {
                      // The anchor ID 'content' was renamed 'tandf_content'
                      if (tag.getAttribute("href") != null && "#content".equals(tag.getAttribute("href"))) {
                        tag.setAttribute("href", "#tandf_content");
                        return;
                      }
                      // Inline links used to have class="ref" and target="url",
                      // now only target="_blank"
                      if (tag.getAttribute("target") != null) {
                        tag.removeAttribute("target");
                      }
                      if (tag.getAttribute("class") != null && "ref".equals(tag.getAttribute("class"))) {
                        tag.removeAttribute("class");
                      }
                      return;
                    }
                  } break;
                  case 'd': {
                    /*
                     * <div>
                     */
                    if ("div".equals(tagName)) {
                      if (tag.getAttribute("class") != null && tag.getAttribute("class").startsWith("access ")) {
                        tag.removeAttribute("class");
                        return;
                      }
                      // For a while, there were two <div> tags with 'id' set to 'content'
                      // Now clarified to 'journal_content' and 'tandf_content'
                      else if (tag.getAttribute("id") != null && ("journal_content".equals(tag.getAttribute("id")) || "tandf_content".equals(tag.getAttribute("id")))) {
                        tag.setAttribute("id", "content");
                        return;
                      }
                    }
                  } break;
                  case 'i': {
                    /*
                     * <img>
                     */
                    // Images used to have an ID
                    if ("img".equals(tagName)) {
                      if (tag.getAttribute("id") != null) {
                        tag.removeAttribute("id");
                      }
                      // Prevent the latter from causing spurious <.../> vs. <... />
                      String src = tag.getAttribute("alt");
                      tag.removeAttribute("alt");
                      tag.setAttribute("alt", src);
                      return;
                    }
                  } break;
                  case 'm': {
                    /*
                     * <meta>
                     */
                    if ("meta".equals(tagName)) {
                      // Some dc.Creator tags evolved to have extraneous whitespace
                      if (tag.getAttribute("name") != null && "dc.Creator".equals(tag.getAttribute("name")) && tag.getAttribute("content") != null) {
                        tag.setAttribute("content", tag.getAttribute("content").trim());
                      }
                    }
                  } break;
                  case 's': {
                    /*
                     * <span>
                     */
                    if ("span".equals(tagName)) {
                      if (tag.getAttribute("id") != null) {
                        tag.removeAttribute("id");
                        return;
                      }
                    }
                  } break;
                  case 't': {
                    /*
                     * <table>
                     */
                    if ("table".equals(tagName)) {
                      // From <table class="listgroup " border="0" width="95%">
                      // to <table xmlns:mml="http://www.w3.org/1998/Math/MathML" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" class="listgroup " border="0" width="95%">
                      if (tag.getAttribute("xmlns:mml") != null) {
                        tag.removeAttribute("xmlns:mml");
                      }
                      if (tag.getAttribute("xmlns:xsi") != null) {
                        tag.removeAttribute("xmlns:xsi");
                      }
                      return;
                    }
                  } break;
                }
                // Still here: tag has not been visited
                super.visitTag(tag);
              }
              catch (Exception exc) {
                log.debug2("Internal error (visitor)", exc); // Ignore this tag and move on
              }
            }
          });
        }
        catch (ParserException pe) {
          log.debug2("Internal error (parser)", pe); // Bail
        }
        return nodeList;
      }
    };
    
    InputStream filtered = new HtmlFilterInputStream(in,
                                                     encoding,
                                                     new HtmlCompoundTransform(HtmlNodeFilterTransform.exclude(new OrFilter(filters)),
                                                                               xform));
    
    Reader reader = FilterUtil.getReader(filtered, encoding);
    Reader tagFilter = HtmlTagFilter.makeNestedFilter(reader,
                                                      ListUtil.list(
        // Two alternate forms of citation links (no easy to characterize in the DOM)
        new TagPair("<strong>Citations:", "</strong>"),
        new TagPair("<strong><a href=\"/doi/citedby/", "</strong>"),
        // some comments contain an opaque hash or timestamp
        new TagPair("<!--", "-->")
    ));
    Reader stringFilter = tagFilter;
    // Wording change
    stringFilter = new StringFilter(stringFilter,
                                    "<strong>Available online:</strong>",
                                    "<strong>Version of record first published:</strong>");
    // Artifact of whitespace filter
    stringFilter = new StringFilter(stringFilter,
                                    "</div><div",
                                    "</div> <div");
    return new ReaderInputStream(new WhiteSpaceFilter(stringFilter));
  }

}
