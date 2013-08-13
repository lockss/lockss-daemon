/*
 * $Id: TaylorAndFrancisHtmlHashFilterFactory.java,v 1.11 2013-08-13 21:39:25 alexandraohlson Exp $
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

  Logger log = Logger.getLogger("TaylorAndFrancisHtmlHashFilterFactory");
  
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
        // Header contains institution-dependent markup, can contain
        // temporary site warnings (e.g. "Librarians' administration
        // tools are temporarily unavailable. We are working to
        // restore these as soon as possible and apologize for any
        // inconvenience caused."), etc.
        HtmlNodeFilters.tagWithAttribute("div", "id", "hd"),
        // Footer contains copyright year and other potentially variable items
        HtmlNodeFilters.tagWithAttribute("div", "id", "ft"),
        
        // Maximal filtering. Take out non-content sections proactively
        HtmlNodeFilters.tagWithAttribute("div",  "id", "cookieBanner"),//cookie warning
        HtmlNodeFilters.tagWithAttribute("div", "id", "primarySubjects"),//top search are
        HtmlNodeFilters.tagWithAttribute("div", "id", "secondarySubjects"), //bottom affiliations
        HtmlNodeFilters.tagWithAttribute("div", "class", "social clear"), // facebook/twitter etc
        HtmlNodeFilters.tagWithAttribute("div",  "id", "unit1"), //left column
        HtmlNodeFilters.tagWithAttribute("div",  "id", "unit3"), //right column 
        
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
