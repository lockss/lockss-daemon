/*
 * $Id: BioMedCentralHtmlFilterFactory.java,v 1.6 2012-07-04 00:22:34 kendrayee Exp $
 */

/*

Copyright (c) 2000-2008 Board of Trustees of Leland Stanford Jr. University,
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

import java.io.InputStream;
import java.io.Reader;

import org.htmlparser.NodeFilter;
import org.htmlparser.filters.*;
import org.lockss.daemon.PluginException;
import org.lockss.filter.FilterUtil;
import org.lockss.filter.HtmlTagFilter;
import org.lockss.filter.WhiteSpaceFilter;
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
        new TagNameFilter("script"),
        // Contains variable alternatives to the code
        new TagNameFilter("noscript"),
        // Contains ads
        new TagNameFilter("iframe"),
        // Contains ads
        new TagNameFilter("object"),
        // Contains one-time names inside the page
        HtmlNodeFilters.tagWithAttribute("a", "name"),
        // Links to one-time names inside the page
        HtmlNodeFilters.tagWithAttributeRegex("a", "href", "^#"),
        // Contains the institution name; once a 'class', now an 'id'
        HtmlNodeFilters.tagWithAttribute("td", "class", "topnav"),
        HtmlNodeFilters.tagWithAttribute("td", "id", "topnav"),
        // Contains advertising
        HtmlNodeFilters.tagWithAttribute("td", "class", "topad"),
        // Contains advertising
        HtmlNodeFilters.tagWithAttribute("div", "id", "newad"),
        // Contains copyright year; also now references Springer 
        HtmlNodeFilters.tagWithAttribute("table", "class", "footer2t"),
        // Institution-dependent link resolvers
        HtmlNodeFilters.tagWithAttributeRegex("a", "href", "^/sfx_links\\.asp"),
        // Institution-dependent greeting
        HtmlNodeFilters.tagWithAttribute("span", "id", "username"),
        // Malformed HTML
        HtmlNodeFilters.tagWithAttribute("span", "id", "articles-tab")
        
    };
    
    InputStream filtered =  new HtmlFilterInputStream(in, encoding, HtmlNodeFilterTransform.exclude(new OrFilter(filters)));
    
    Reader filteredReader = FilterUtil.getReader(filtered, encoding);
    return new ReaderInputStream(new WhiteSpaceFilter(filteredReader));
  }

}
