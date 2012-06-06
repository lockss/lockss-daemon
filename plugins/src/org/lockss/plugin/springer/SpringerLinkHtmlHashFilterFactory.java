/*
 * $Id: SpringerLinkHtmlHashFilterFactory.java,v 1.16 2012-06-06 02:18:58 thib_gc Exp $
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

package org.lockss.plugin.springer;

import java.io.*;

import org.htmlparser.*;
import org.htmlparser.filters.*;
import org.htmlparser.tags.*;
import org.lockss.daemon.PluginException;
import org.lockss.filter.StringFilter;
import org.lockss.filter.WhiteSpaceFilter;
import org.lockss.filter.html.*;
import org.lockss.plugin.*;
import org.lockss.util.ReaderInputStream;

public class SpringerLinkHtmlHashFilterFactory implements FilterFactory {

  public static class FilteringException extends PluginException {
    public FilteringException() { super(); }
    public FilteringException(String msg, Throwable cause) { super(msg, cause); }
    public FilteringException(String msg) { super(msg); }
    public FilteringException(Throwable cause) { super(cause); }
  }
  
  @Override
  public InputStream createFilteredInputStream(ArchivalUnit au,
                                               InputStream in,
                                               String encoding)
      throws PluginException {
    // First filter with HtmlParser
    NodeFilter[] filters = new NodeFilter[] {
        /*
         * Crawl filter
         */
        // Contains cross-links to other articles in other journals/volumes
        HtmlNodeFilters.tagWithAttribute("div", "id", "RelatedSection"),
        /*
         * Hash filter
         */
        // Contains ad-specific cookies
        new TagNameFilter("script"),
        // Contains ads
        HtmlNodeFilters.tagWithAttribute("div", "class", "advertisement"),
        // Contains a lot of variable elements; institution name, gensyms for tag IDs or names, etc.
        HtmlNodeFilters.tagWithAttribute("div", "id", "Header"),
        // Contains account and user agent information
        HtmlNodeFilters.tagWithAttribute("ul", "id", "Footer"),
        // Contains the name and year of the latest known volume
        HtmlNodeFilters.tagWithAttributeRegex("p", "class", "coverage"),
        // Contains SFX links
        HtmlNodeFilters.tagWithAttributeRegex("div", "class", "linkoutView"),
        // Has a session cookie
        HtmlNodeFilters.tagWithAttribute("form", "id", "LoginForm"),
        // Spurious gensyms
        HtmlNodeFilters.tagWithAttributeRegex("link", "href", "^/dynamic-file\\.axd"),
        // CSS file names can be spuriously versioned
        HtmlNodeFilters.tagWithAttributeRegex("link", "href", "^/styles/"),
        // Icon names can be spuriously versioned
        HtmlNodeFilters.tagWithAttributeRegex("img", "src", "^/images/"),
        // Contains ASP state blob
        HtmlNodeFilters.tagWithAttribute("input", "id", "__VIEWSTATE"),
        // Contains ASP state blob
        HtmlNodeFilters.tagWithAttribute("input", "id", "__EVENTVALIDATION"),
        // Contains ever-updated information e.g. most recent issue
        HtmlNodeFilters.tagWithAttribute("div", "id", "AboutSection"),
        // Contains ever-updated information e.g. list of all issues
        HtmlNodeFilters.tagWithAttribute("div", "id", "Modes"),
        // Text includes number of reverse citations
        HtmlNodeFilters.tagWithAttributeRegex("a", "href", "/referrers/$"),
        
        // MAINTENANCE
        
        // Alas, the <title> tag switched from "SpringerLink - Foo" to
        // "Foo - SpringerLink", and the order/number of <meta> tags
        // changes over time
        new TagNameFilter("head"),
        
        // Eventually changed from <h1 lang="en" class="title"> to <h1>
        new TagNameFilter("h1"),
        
        // Over time, <span class="...toolbarSprite..."></span>
        // became <a class="...toolbarSprite...">...</a>
        new NodeFilter() {
          @Override
          public boolean accept(Node node) {
            if (node instanceof Span || node instanceof LinkTag) {
              Tag tag = (Tag)node;
              String attr = tag.getAttribute("class");
              return (attr != null && attr.contains("toolbarSprite"));
            }
            return false;
          }
        },
        
        // The back link to the issue TOC contained in this <div>
        // now has the year parenthesized after the back link
        HtmlNodeFilters.tagWithAttribute("div", "id", "ContentHeading"),
        
        // The inline styling of this <div> has changed from
        // <div class="coverImage" title="Cover Image" style="background-image: url(...)">
        // to
        // <div class="coverImage" title="Cover Image" style="background-image: url(...); background-size: contain;">
        HtmlNodeFilters.tagWithAttribute("div", "class", "coverImage"),
        
    };
    InputStream filteredStream = new HtmlFilterInputStream(in,
                                                           encoding,
                                                           HtmlNodeFilterTransform.exclude(new OrFilter(filters)));

    // Then apply Reader-based transformations
    try {
      // Turn into a Reader (can throw)
      Reader reader1 = new InputStreamReader(filteredStream, encoding);
      // Inconsistent use of "'" and "&#39;"
      Reader reader2 = new StringFilter(reader1, "&#39;", "'");
      // Noisy whitespace
      Reader reader3 = new WhiteSpaceFilter(reader2);
      // Convert back into an InputStream
      return new ReaderInputStream(reader3);
    }
    catch (UnsupportedEncodingException uee) {
      throw new FilteringException(uee);
    }
  }
  
}
