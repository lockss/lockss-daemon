/*
 * $Id$
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

package org.lockss.plugin.iop;

import java.io.*;

import org.htmlparser.Node;
import org.htmlparser.NodeFilter;
import org.htmlparser.filters.OrFilter;
import org.htmlparser.filters.TagNameFilter;
import org.lockss.daemon.PluginException;
import org.lockss.filter.*;
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
        // Scripts
        new TagNameFilter("script"),
        // Document header
        new TagNameFilter("head"),
        // header/footer tags
        new TagNameFilter("header"),
        new TagNameFilter("footer"),
        // Header
        HtmlNodeFilters.tagWithAttribute("div", "id", "cookieBanner"),
        /* <header> -- see filter above */
        HtmlNodeFilters.tagWithAttribute("div", "id", "jnl-head-band"),
        HtmlNodeFilters.tagWithAttribute("div", "id", "hdr"), // (old)
        HtmlNodeFilters.tagWithAttribute("div", "id", "nav"), // (old)
        HtmlNodeFilters.tagWithAttribute("div", "id", "header-content"), // (old)
        HtmlNodeFilters.tagWithAttribute("div", "id", "banner"), // (old)
        // Right column
        HtmlNodeFilters.tagWithAttribute("div", "id", "rightCol"),
        HtmlNodeFilters.tagWithAttributeRegex("div", "class", "alsoRead"), // (now within)
        HtmlNodeFilters.tagWithAttribute("div", "id", "tacticalBanners"), // (now within)
        // Contains the search box, which changes over time
        HtmlNodeFilters.tagWithAttribute("div", "id", "login_dialog"),
        // Footer
        /* <footer> -- see filter above */
        HtmlNodeFilters.tagWithAttribute("div", "id", "footer"), // (old)

        /*
         * Main content area 
         */
        // Last 10 articles viewed
        HtmlNodeFilters.tagWithAttribute("div", "class", "tabs javascripted"),

        /*
         * Other
         */
        // may not be an issue, but concerned that mathjax exposure will change
        HtmlNodeFilters.tagWithAttribute("div", "class", "mathJaxControls"),
        // Contains a jsessionid
        HtmlNodeFilters.tagWithAttributeRegex("form", "action", "jsessionid"),
        
        // <div class="sideTabBar">
        HtmlNodeFilters.tagWithAttributeRegex("div", "class", "sideTab(Bar|Box)"),
        // <p class="viewingLinks">
        HtmlNodeFilters.tagWithAttributeRegex("p", "class", "viewingLinks"),
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
        // <div class="sideTabBlock citBlock">
    };
    
    InputStream filtered = new HtmlFilterInputStream(in,
                                                     encoding,
                                                     HtmlNodeFilterTransform.exclude(new OrFilter(filters)));
    Reader filteredReader = FilterUtil.getReader(filtered, encoding);
    return new ReaderInputStream(new WhiteSpaceFilter(filteredReader));
  }

}
