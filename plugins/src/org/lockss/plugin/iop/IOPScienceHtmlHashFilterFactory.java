/*

Copyright (c) 2000-2022, Board of Trustees of Leland Stanford Jr. University
All rights reserved.

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

package org.lockss.plugin.iop;

import java.io.*;

import org.htmlparser.Node;
import org.htmlparser.NodeFilter;
import org.htmlparser.filters.OrFilter;
import org.htmlparser.filters.TagNameFilter;
import org.htmlparser.nodes.TextNode;
import org.lockss.daemon.PluginException;
import org.lockss.filter.*;
import org.lockss.filter.HtmlTagFilter.TagPair;
import org.lockss.filter.html.*;
import org.lockss.plugin.*;
import org.lockss.util.*;


public class IOPScienceHtmlHashFilterFactory implements FilterFactory {

  private static final Logger log = Logger.getLogger(IOPScienceHtmlHashFilterFactory.class);
  
  @Override
  public InputStream createFilteredInputStream(final ArchivalUnit au,
                                               InputStream in,
                                               String encoding)
      throws PluginException {
    NodeFilter[] filters = new NodeFilter[] {
        /*
         * Broad area filtering
         */
        // Comments, scripts, style
        HtmlNodeFilters.comment(),
        HtmlNodeFilters.tag("script"),
        HtmlNodeFilters.tag("style"),
        // Document header
        HtmlNodeFilters.tag("head"),
        // Header
        HtmlNodeFilters.tag("header"),

        HtmlNodeFilters.tagWithAttribute("div", "id", "cookieBanner"),
        HtmlNodeFilters.tagWithAttribute("div", "id", "jnl-head-band"),
        HtmlNodeFilters.tagWithAttribute("div", "id", "hdr"), // (old)
        HtmlNodeFilters.tagWithAttribute("div", "id", "nav"), // (old)
        HtmlNodeFilters.tagWithAttribute("div", "id", "header-content"), // (old)
        HtmlNodeFilters.tagWithAttribute("div", "id", "banner"), // (old)
        // Right column
        // missed filter maybe due to malformed html?? <li xmlns:job="http://brightrecruits.com/ns/job/">
        HtmlNodeFilters.tagWithAttributeRegex("li", "xmlns:job", "."),
        HtmlNodeFilters.tagWithAttribute("div", "id", "rightCol"),
        HtmlNodeFilters.tagWithAttributeRegex("div", "class", "alsoRead"), // (now within)
        HtmlNodeFilters.tagWithAttribute("div", "id", "tacticalBanners"), // (now within)
        // Footer
        HtmlNodeFilters.tag("footer"),
        HtmlNodeFilters.tagWithAttribute("div", "id", "footer"), // (old)
        // <div class="beta-footer"> added recently
        HtmlNodeFilters.tagWithAttributeRegex("div", "class", "footer"),
        HtmlNodeFilters.tagWithAttribute("div", "id", "login_dialog"), // (now within 'footer')

        /*
         * Main content area 
         */
        // MathJax on/off buttons
        HtmlNodeFilters.tagWithAttribute("div", "class", "mathJaxControls"),
        // Social media etc.
        HtmlNodeFilters.tagWithAttributeRegex("ul", "class", "articleTools"),
        // Text of the link to the PDF: "PDF" vs. "Full Text PDF", size in MB vs. not
        new NodeFilter() {
          @Override
          public boolean accept(Node node) {
            return node instanceof TextNode
                && HtmlNodeFilters.tagWithAttribute("a", "class", "icon pdf").accept(node.getParent());
          }
        },
        // Metrics, reprint request
        HtmlNodeFilters.tagWithAttribute("a", "id", "enhancedArticleMetricsId"),
        HtmlNodeFilters.tagWithAttribute("a", "id", "enhancedCopyrightLinkId"),
        // Last 10 articles viewed
        HtmlNodeFilters.tagWithAttribute("div", "class", "tabs javascripted"),

        /*
         * Main body
         */
        // Powerpoints have not always been on the page (we don't collect them)
        HtmlNodeFilters.tagWithAttributeRegex("a", "href", "^powerpoint/"),
        
        /*
         * Other
         */
        // Contains a jsessionid
        HtmlNodeFilters.tagWithAttributeRegex("form", "action", "jsessionid"),
        // <div class="sideTabBar"> & <div id="sideTabBox">
        // <div class="sideTabBlock citBlock">
        HtmlNodeFilters.tagWithAttributeRegex("div", "id", "sideTab(Bar|Box)"),
        HtmlNodeFilters.tagWithAttributeRegex("div", "class", "sideTab(Bar|Box)"),
        // <p class="viewingLinks">
        HtmlNodeFilters.tagWithAttributeRegex("p", "class", "viewingLinks"),
        // <dl class="videoList"> PACS & Subjects appear, disappear, reappear
        HtmlNodeFilters.tagWithAttribute("dl", "class", "videoList"),
        // <a title="CrossRef" can appear later
        HtmlNodeFilters.tagWithAttribute("a", "title", "CrossRef"),
        // <div class=" metrics-panel">
        HtmlNodeFilters.tagWithAttributeRegex("div", "class", "metrics-panel"),
        // <dd> <p> Total article downloads: <strong>1193</strong> </p>...</dd>
        new TagNameFilter("dd") {
          // DEBUG
          public void logException(Throwable thr, String plainText) {
            StringBuilder sb = new StringBuilder();
            sb.append("toLowerCase threw in AU ");
            sb.append(au.getName());
            sb.append(" on the following input: <begin quote>");
            sb.append(plainText);
            sb.append("<end quote>, which translates to:");
            for (int i = 0 ; i < plainText.length() ; ++i) {
              sb.append(String.format(" %04X", plainText.charAt(i)));
            }
            log.warning(sb.toString(), thr);
          }
          @Override
          public boolean accept(Node node) {
            boolean ret = false;
            if (super.accept(node)) {
              String plainText = node.toPlainTextString();
              try {
                String allText = plainText.toLowerCase();
                ret = allText.contains("total article downloads") ||
                    (allText.contains("download data unavailable") &&
                     allText.contains("more metrics"));
                return ret;
              }
              catch (InternalError interr) {
                logException(interr, plainText);
              }
            }
            return ret;
          }
        },
        // next/previous can change
        HtmlNodeFilters.tagWithAttribute("div", "class", "jnlTocIssueNav"),
        // <span class="boxBut free-article">
        HtmlNodeFilters.tagWithAttributeRegex("span", "class", "free-article"),
        // citation link was always not present, and display link is not content
        HtmlNodeFilters.tagWithAttributeRegex("a", "id", "DisplayLink"),
        // Open Access can change http://iopscience.iop.org/1674-1137/38/6/063103 (was there/not now)
        HtmlNodeFilters.tagWithAttribute("div", "id", "articleOALicense"),
    };
    
    InputStream filtered = new HtmlFilterInputStream(in,
                                                     encoding,
                                                     HtmlNodeFilterTransform.exclude(new OrFilter(filters)));
    
    Reader filteredReader = FilterUtil.getReader(filtered, encoding);
    
    Reader noTagFilter = new HtmlTagFilter(new StringFilter(filteredReader, "<", " <"), new TagPair("<", ">"));

    return new ReaderInputStream(new WhiteSpaceFilter(noTagFilter));
  }

}
