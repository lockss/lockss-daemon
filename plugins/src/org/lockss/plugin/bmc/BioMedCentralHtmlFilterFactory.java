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

package org.lockss.plugin.bmc;

import java.io.*;

import org.htmlparser.*;
import org.htmlparser.filters.*;
import org.htmlparser.tags.*;
import org.lockss.daemon.PluginException;
import org.lockss.filter.*;
import org.lockss.filter.html.*;
import org.lockss.plugin.*;
import org.lockss.util.ReaderInputStream;

public class BioMedCentralHtmlFilterFactory implements FilterFactory {

  public InputStream createFilteredInputStream(ArchivalUnit au,
                                               InputStream in,
                                               String encoding)
      throws PluginException {
    NodeFilter[] filters = new NodeFilter[] {
        // Contains variable code
        HtmlNodeFilters.tag("script"),
        // Contains variable alternatives to the code
        HtmlNodeFilters.tag("noscript"),
        // remove all style tags!
        HtmlNodeFilters.tag("style"),
        // Contains ads
        HtmlNodeFilters.tag("iframe"),
        // Contains ads
        HtmlNodeFilters.tag("object"),
        // CSS and RSS links varied over time
        HtmlNodeFilters.tag("link"),
        // Contains one-time names inside the page
        HtmlNodeFilters.tagWithAttribute("a", "name"),
        // Links to one-time names inside the page
        HtmlNodeFilters.tagWithAttributeRegex("a", "href", "^#"),
        // Contains the institution name; once a 'class', now an 'id'
        // following six not in springeropen 
        HtmlNodeFilters.tagWithAttribute("td", "class", "topnav"),
        HtmlNodeFilters.tagWithAttribute("td", "id", "topnav"),
        // Contains advertising
        HtmlNodeFilters.tagWithAttribute("td", "class", "topad"),
        // Contains advertising
        HtmlNodeFilters.tagWithAttribute("div", "id", "newad"),
        // Contains copyright year; also now references Springer 
        HtmlNodeFilters.tagWithAttribute("table", "class", "footer2t"),
        // Institution-dependent link resolvers
        HtmlNodeFilters.tagWithAttributeRegex("a", "href", "^/sfx_links\\?.*"),
        
        // Institution-dependent greeting
        HtmlNodeFilters.tagWithAttribute("li", "class", "greeting"),
        // Malformed HTML
        HtmlNodeFilters.tagWithAttribute("span", "id", "articles-tab"),
        // A usage counter/glif that gets updated over time
        HtmlNodeFilters.tagWithAttribute("div", "id", "impact-factor"),
	// Contains adverstising <a class="banner-ad"
	HtmlNodeFilters.tagWithAttribute("a", "class", "banner-ad"),
	// Contains adverstising <a class="skyscraper-ad" 
	HtmlNodeFilters.tagWithAttribute("a", "class", "skyscraper-ad"),
	// google ad - may change?
        HtmlNodeFilters.tagWithAttributeRegex("dl", "class", "google-ad.*"),
        // Social networking links (have counters)
        HtmlNodeFilters.tagWithAttribute("ul", "id", "social-networking-links"),
        // An open access link/glyph that may get added
        HtmlNodeFilters.tagWithAttributeRegex("a", "href", ".*/about/access"),
        // A highly accessed link/glyph that may get added
        HtmlNodeFilters.tagWithAttributeRegex("a", "href", ".*/about/mostviewed"),
        // Springer branding below the footer
        HtmlNodeFilters.tagWithAttribute("div", "class", "springer"),
        // remove footer
        HtmlNodeFilters.tagWithAttribute("div", "id", "footer"),
        // remove the mobile sidebar
        HtmlNodeFilters.tagWithAttribute("div", "id", "mobile-sidebar"),
        // remove the right side column
        HtmlNodeFilters.tagWithAttribute("div", "id", "article-navigation-bar"),

        // removing empty <div> tag that moves around..
        //HtmlNodeFilters.tagWithAttribute("div", "id", "biome-badge"),
        // The text of this link changed from "About this article" to "Article metrics"
        HtmlNodeFilters.tagWithAttributeRegex("a", "href", "/about$"),
        // Journal of Cheminformatics -  an "accesses" and/or "citations" block
        // but the id is associated with the <h2>, not with the sibling <div>
        
        // removes mathml inline wierdnesses
        HtmlNodeFilters.tagWithAttribute("p", "class", "inlinenumber"),
        HtmlNodeFilters.tagWithAttributeRegex("div", "style", ".*display:inline$"),
        HtmlNodeFilters.tagWithAttribute("span", "class", "mathjax"),
        HtmlNodeFilters.tagWithAttribute("span", "class", "inline-math"),
        HtmlNodeFilters.tagWithAttribute("span", "class", "inlinenumber"),
        
        new NodeFilter() {
          @Override public boolean accept(Node node) {
            if (!(node instanceof Div)) return false;
            Node prevNode = node.getPreviousSibling();
            while (prevNode != null && !(prevNode instanceof HeadingTag)) {
              prevNode = prevNode.getPreviousSibling();
            }
            if (prevNode != null && prevNode instanceof HeadingTag) {
              CompositeTag prevTag = (CompositeTag)prevNode;
              String id = prevTag.getAttribute("id");
              return "accesses".equals(id) || "citations".equals(id);
            }
            return false;
          }
        }
    };
    
    InputStream filtered =  new HtmlFilterInputStream(in, encoding, HtmlNodeFilterTransform.exclude(new OrFilter(filters)));
    
    Reader filteredReader = FilterUtil.getReader(filtered, encoding);
    return new ReaderInputStream(new WhiteSpaceFilter(filteredReader));
  }

}
