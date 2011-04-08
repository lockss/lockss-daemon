/*
 * $Id: SpringerLinkHtmlHashFilterFactory.java,v 1.8 2011-04-08 23:40:19 thib_gc Exp $
 */

/*

Copyright (c) 2000-2011 Board of Trustees of Leland Stanford Jr. University,
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

import java.io.InputStream;

import org.htmlparser.NodeFilter;
import org.htmlparser.filters.*;
import org.lockss.daemon.PluginException;
import org.lockss.filter.html.*;
import org.lockss.plugin.*;

public class SpringerLinkHtmlHashFilterFactory implements FilterFactory {

  @Override
  public InputStream createFilteredInputStream(ArchivalUnit au,
                                               InputStream in,
                                               String encoding)
      throws PluginException {
    NodeFilter[] filters = new NodeFilter[] {
        // Contains ad-specific cookies
        new TagNameFilter("script"),
        // Contains cross-links to other articles in other journals/volumes
        HtmlNodeFilters.tagWithAttribute("div", "id", "RelatedSection"),
        // Contains ads
        HtmlNodeFilters.tagWithAttribute("div", "class", "advertisement"),
        // Contains a lot of variable elements; institution name, gensyms for tag IDs or names, etc.
        HtmlNodeFilters.tagWithAttribute("div", "id", "Header"),
        // Contains account and user agent information
        HtmlNodeFilters.tagWithAttribute("ul", "id", "Footer"),
        // Contains SFX links
        HtmlNodeFilters.tagWithAttributeRegex("div", "class", "linkoutView"),
        // Has a session cookie
        HtmlNodeFilters.tagWithAttribute("form", "id", "LoginForm"),
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
        // Static, but was added after thousands of AUs had crawled already
        HtmlNodeFilters.tagWithAttribute("meta", "name", "robots"),
    };
    return new HtmlFilterInputStream(in,
                                     encoding,
                                     HtmlNodeFilterTransform.exclude(new OrFilter(filters)));
  }
  
  public static void main(String[] args) throws Exception {
    if (args.length == 0) {
      args = new String[] {
          "/tmp/HashCUS1", "/tmp/HashCUS2"
      };
    }
    FilterFactory filt = new SpringerLinkHtmlHashFilterFactory();
    for (String str : args) {
      InputStream in = filt.createFilteredInputStream(null, new java.io.FileInputStream(str), "UTF-8");
      org.apache.commons.io.IOUtils.copy(in, new java.io.FileOutputStream(str + ".out"));
    }
  }

}
