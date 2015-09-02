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

import org.htmlparser.NodeFilter;
import org.htmlparser.filters.*;
import org.lockss.daemon.PluginException;
import org.lockss.filter.*;
import org.lockss.filter.html.*;
import org.lockss.plugin.*;
import org.lockss.util.ReaderInputStream;

public class BMCPluginHtmlFilterFactory implements FilterFactory {

  public InputStream createFilteredInputStream(ArchivalUnit au,
                                               InputStream in,
                                               String encoding)
      throws PluginException {
    NodeFilter[] filters = new NodeFilter[] {
        // malformed html causing low agreement <div id="oas-
        HtmlNodeFilters.tagWithAttributeRegex("div", "id", "^oas-"),
        // head tag - Extreme Hash filtering!
        HtmlNodeFilters.tag("head"),
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
        //filter out comments
        HtmlNodeFilters.comment(),
        // upper area above the article - Extreme Hash filtering!
        HtmlNodeFilters.tagWithAttribute("div", "id", "branding"),
        // left-hand area next to the article - Extreme Hash filtering!
        HtmlNodeFilters.tagWithAttribute("div", "class", "left-article-box"),
        HtmlNodeFilters.tagWithAttribute("div", "id", "left-article-box"),
        // right-hand area next to the article - Extreme Hash filtering!
        HtmlNodeFilters.tagWithAttribute("div", "id", "article-navigation-bar"),
        // alert signup - Extreme Hash filtering!
//        HtmlNodeFilters.tagWithAttribute("div", "class", "article-alert-signup-div"),
        // Contains one-time names inside the page
        HtmlNodeFilters.tagWithAttribute("a", "name"),
        // Links to one-time names inside the page
        HtmlNodeFilters.tagWithAttributeRegex("a", "href", "^#"),
        // Institution-dependent greeting
        HtmlNodeFilters.tagWithAttribute("li", "class", "greeting"),
        
        // Contains the menu  <ul class="primary-nav">
        HtmlNodeFilters.tagWithAttribute("ul", "class", "primary-nav"),
        // remove footer
        //Contains the terms and conditions,copyright year & links to springer
        HtmlNodeFilters.tagWithAttribute("div", "id", "footer"),
        // Contains university name: <ul id="login"
        HtmlNodeFilters.tagWithAttribute("ul", "id", "login"),
        // Contains advertising
        HtmlNodeFilters.tagWithAttributeRegex("dl", "class", "google-ad"),
        // Social networking links (have counters)
        HtmlNodeFilters.tagWithAttribute("ul", "id", "social-networking-links"),
        // A usage counter/glif that gets updated over time
        HtmlNodeFilters.tagWithAttribute("div", "id", "impact-factor"),
        // Contains adverstising <a class="banner-ad"
        HtmlNodeFilters.tagWithAttribute("a", "class", "banner-ad"),
        // Contains adverstising <a class="skyscraper-ad" 
        HtmlNodeFilters.tagWithAttribute("a", "class", "skyscraper-ad"),
        // An open access link/glyph that may get added
        HtmlNodeFilters.tagWithAttributeRegex("a", "href", "/about/access"),
        // A highly accessed link/glyph that may get added
        HtmlNodeFilters.tagWithAttributeRegex("a", "href", "/about/mostviewed"),
        // Institution-dependent image
        HtmlNodeFilters.tagWithAttributeRegex("img", "src", "^/sfx_links\\?"),
        // Institution-dependent link resolvers  v2 - added
        HtmlNodeFilters.tagWithAttributeRegex("a", "href", "^/sfx_links\\?"),
        // Institution-dependent link resolvers   v1
        HtmlNodeFilters.tagWithAttributeRegex("a", "href", "^/sfx_links\\.asp"),
        // Springer branding below the footer
        HtmlNodeFilters.tagWithAttribute("div", "class", "springer"),
        
        // The text of this link changed from "About this article" to "Article metrics"
        HtmlNodeFilters.tagWithAttributeRegex("a", "href", "/about$"),
        // removes mathml inline wierdnesses
        HtmlNodeFilters.tagWithAttribute("p", "class", "inlinenumber"),
        HtmlNodeFilters.tagWithAttributeRegex("div", "style", "display:inline$"),
        HtmlNodeFilters.tagWithAttribute("span", "class", "mathjax"),
        HtmlNodeFilters.tagWithAttribute("span", "class", "inline-math"),
        HtmlNodeFilters.tagWithAttribute("span", "class", "inlinenumber"),
        
        // floating bottom banner announcing access to beta version of new site
        HtmlNodeFilters.tagWithAttributeRegex("div", "class",  "^banner-footer"),
        
    };
    
    InputStream filtered =  new HtmlFilterInputStream(in, encoding, 
        HtmlNodeFilterTransform.exclude(new OrFilter(filters)));
    Reader filteredReader = FilterUtil.getReader(filtered, encoding);
    // added whitespace filter
    return new ReaderInputStream(new WhiteSpaceFilter(filteredReader));
  }

}
