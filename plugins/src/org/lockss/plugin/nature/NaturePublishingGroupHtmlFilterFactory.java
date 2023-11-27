/*

Copyright (c) 2000-2023, Board of Trustees of Leland Stanford Jr. University

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
        HtmlNodeFilters.tag("head"),
        // Scripting
        HtmlNodeFilters.tag("script"),
        HtmlNodeFilters.tag("noscript"),
        // Header
        HtmlNodeFilters.tagWithAttributeRegex("div", "id", "constrain-header"),
        HtmlNodeFilters.tagWithAttribute("div", "id", "hdr"),
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
        HtmlNodeFilters.comment(),

        /*
         * Other
         */

        // Modernized markup of images, significant changes
        HtmlNodeFilters.tag("img"),
        // Slightly changed search form components
        HtmlNodeFilters.tag("input"),
        HtmlNodeFilters.tagWithAttributeRegex("a", "href", "^/search/executeSearch"),
        // ?
        HtmlNodeFilters.tagWithAttribute("div", "class", "baseline-wrapper"),
        // global message on pages - e.g. request to fill out a survey
        HtmlNodeFilters.tagWithAttributeRegex("div", "class", "^global-message"),
        // http://www.nature.com/jhg/journal/v61/n8/index.html 
        HtmlNodeFilters.tagWithAttribute("span", "class", "free"),
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
