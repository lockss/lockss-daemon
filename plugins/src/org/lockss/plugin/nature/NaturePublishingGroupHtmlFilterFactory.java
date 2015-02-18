/*
 * $Id$
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

package org.lockss.plugin.nature;

import java.io.*;

import org.htmlparser.*;
import org.htmlparser.filters.*;
import org.htmlparser.util.*;
import org.htmlparser.visitors.NodeVisitor;
import org.lockss.daemon.PluginException;
import org.lockss.filter.FilterUtil;
import org.lockss.filter.WhiteSpaceFilter;
import org.lockss.filter.html.*;
import org.lockss.plugin.*;
import org.lockss.util.*;

/**
 * <p>Normalizes HTML pages from Nature Publishing Group journals.</p>
 * @author Thib Guicherd-Callin
 */
public class NaturePublishingGroupHtmlFilterFactory implements FilterFactory {

  private static final Logger log = Logger.getLogger(NaturePublishingGroupHtmlFilterFactory.class);
  
  public InputStream createFilteredInputStream(ArchivalUnit au,
                                               InputStream in,
                                               String encoding)
      throws PluginException {
    NodeFilter[] filters = new NodeFilter[] {
        /*
         * Broad area filtering
         */
        // Document header
        new TagNameFilter("head"),
        // Scripting
        new TagNameFilter("script"),
        new TagNameFilter("noscript"),
        // Header
        HtmlNodeFilters.tagWithAttributeRegex("div", "id", "constrain-header"),
        // Right column (advertising, jobs ticker, news ticker...)
        HtmlNodeFilters.tagWithAttribute("div", "id", "extranav"),
        // Footer
        HtmlNodeFilters.tagWithAttribute("div", "id", "constrain-footer"),
        
        /*
         * Now covered by broad area filtering
         */
        // Formerly on its own for the document header
        HtmlNodeFilters.tagWithAttribute("meta", "name", "WT.site_id"), // (old)
        // Formerly on its own for the header
        HtmlNodeFilters.tagWithAttribute("div", "id", "header"), // (old name)
        HtmlNodeFilters.tagWithAttributeRegex("div", "class", "login-nav"), // (old)
        HtmlNodeFilters.tagWithAttribute("div", "class", "logon"), // (old)
        HtmlNodeFilters.tagWithAttribute("div", "id", "breadcrumbs"), // Breadcrumbs
        HtmlNodeFilters.tagWithAttribute("div", "id", "breadcrumb"), // Breadcrumbs (old name)
        HtmlNodeFilters.tagWithAttribute("ul", "id", "drop-menu"), // Drop-down menu
        // Formerly on its own for the right column
        HtmlNodeFilters.tagWithAttribute("div", "id", "quick-nav"), // Journal navigation
        HtmlNodeFilters.tagWithAttribute("div", "id", "journalnav"), // Journal navigation (old name)
        HtmlNodeFilters.tagWithAttribute("div", "id", "nature-jobs-events-box"), // Jobs and events ticker
        HtmlNodeFilters.tagWithAttribute("div", "id", "natjob"), // Jobs ticker (old name)
        HtmlNodeFilters.tagWithAttribute("div", "id", "related-top-content"), // Related and most read articles
        HtmlNodeFilters.tagWithAttribute("div", "id", "related-links"), // Related articles (old name)
        HtmlNodeFilters.tagWithAttribute("div", "id", "open-innovation-box"), // Open Innovation Pavilion        
        HtmlNodeFilters.tagWithAttribute("div", "id", "natpav"), // Open Innovation Challenge ticker (old name)        
        HtmlNodeFilters.tagWithAttribute("div", "id", "more-like-this"), // (old)
        HtmlNodeFilters.tagWithAttribute("div", "id", "news-carousel"), // (old)
        // Formerly on its own for the footer
        HtmlNodeFilters.tagWithAttribute("div", "class", "footer"),
        HtmlNodeFilters.tagWithAttribute("div", "id", "footer-copyright"), // (old)
        HtmlNodeFilters.tagWithAttribute("div", "id", "footer-links"), // (old)
        HtmlNodeFilters.tagWithAttributeRegex("ul", "class", "footer-links"), // (old)
        // Advertising (various)
        HtmlNodeFilters.tagWithAttribute("div", "class", "leaderboard"),
        HtmlNodeFilters.tagWithAttribute("div", "class", "ad-vert"),
        HtmlNodeFilters.tagWithAttributeRegex("div", "class", "^ad-rh "),
        HtmlNodeFilters.tagWithAttributeRegex("div", "class", "^ad "),
        
        /*
         * Main article section
         */
        // Toolbar
        HtmlNodeFilters.tagWithAttributeRegex("div", "class", "^top-links"),
        // Casing of each section's navigation links changed
        HtmlNodeFilters.tagWithAttributeRegex("ul", "class", "^section-nav"),
        // Internal structure of boxed figures changed
        HtmlNodeFilters.tagWithAttributeRegex("div", "id", "^bx[0-9]+"),
        // Tweaks in each reference's links
        HtmlNodeFilters.tagWithAttributeRegex("ul", "class", "has-ref-links"),
        // User-submitted comments
        HtmlNodeFilters.tagWithAttribute("div", "id", "comments "),
        HtmlNodeFilters.tagWithAttributeRegex("ul", "class", "^comments "), // (old)
        HtmlNodeFilters.tagWithText("p", "There are currently no comments."),
        
        /*
         * Other
         */
        // HTML comments (FIXME 1.64)
        new NodeFilter() {
          public boolean accept(org.htmlparser.Node node) {
            return (node instanceof Remark);
          };
        },
        // Modernized markup of images, significant changes
        new TagNameFilter("img"),
        // Slightly changed search form components
        new TagNameFilter("input"),
        HtmlNodeFilters.tagWithAttributeRegex("a", "href", "^/search/executeSearch"),
        // ?
        HtmlNodeFilters.tagWithAttribute("div", "class", "baseline-wrapper"),
        // global message on pages - e.g. request to fill out a survey
        HtmlNodeFilters.tagWithAttributeRegex("div", "class", "^global-message"),
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
                  case 'b': {
                    // <body>
                    if ("body".equals(tagName)) {
                      // 'class' attribute from 'small-screen' to 'www-nature-com correspondence' (etc.)
                      tag.removeAttribute("class");
                    }
                  } break;
                  case 'd': {
                    // <div>
                    if ("div".equals(tagName) && tag.getAttribute("class") != null) {
                      // Some 'class' attributes with unexpanded '${sectionClass}' (etc.)
                      tag.removeAttribute("class");
                    }
                  } break;
                }
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
    
    InputStream filtered =  new HtmlFilterInputStream(in, 
                                                      encoding,
                                                      new HtmlCompoundTransform(HtmlNodeFilterTransform.exclude(new OrFilter(filters)),
                                                                                xform));
    
    return new ReaderInputStream(new WhiteSpaceFilter(FilterUtil.getReader(filtered, encoding)));
  }

}
