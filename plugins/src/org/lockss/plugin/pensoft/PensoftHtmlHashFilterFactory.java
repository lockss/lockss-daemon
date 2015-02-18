/* $Id$ */
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

package org.lockss.plugin.pensoft;

import java.io.*;

import org.htmlparser.Node;
import org.htmlparser.NodeFilter;
import org.htmlparser.Node;
import org.htmlparser.tags.Div;
import org.htmlparser.tags.TableColumn;
import org.htmlparser.tags.Span;


import org.htmlparser.filters.*;
import org.lockss.daemon.PluginException;
import org.lockss.filter.html.*;
import org.lockss.plugin.*;
import org.lockss.util.Logger;

public class PensoftHtmlHashFilterFactory implements FilterFactory {

  Logger log = Logger.getLogger(PensoftHtmlHashFilterFactory.class);

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

    NodeFilter[] filters = new NodeFilter[] {
        /* Do not be alarmed when checking hashes of article pages
         * (not abstract or TOC) - the content is dynamically inserted!
         * so after the hash on an article page, content is not there.
         * On the other hand, do check for content on abstract/TOC pages
         */
      
         HtmlNodeFilters.commentWithString("Load time", true),
         
         // extreme hashing: filters out the left columns
         HtmlNodeFilters.tagWithAttribute("td", "class", "textver10"),

         // remove all script tags
         HtmlNodeFilters.tag("script"), 
         HtmlNodeFilters.tag("noscript"),  

         // some the <a> tags with SESIDs should not be hashed out; these look like
         // &SESID=& (are empty);  otherwise, filter out SESIDs that can change
         HtmlNodeFilters.tagWithAttributeRegex("a", "href", ".*SESID=[^&]", true),
         HtmlNodeFilters.tagWithAttributeRegex("a", "onClick", ".*SESID=.*", true),
         HtmlNodeFilters.tagWithAttributeRegex("input", "name", ".*SESID.*", true),
         HtmlNodeFilters.tagWithAttributeRegex("iframe", "src", ".*SESID=.*", true),
         HtmlNodeFilters.tagWithAttributeRegex("tr", "onClick", ".*SESID=.*", true),

         HtmlNodeFilters.commentWithString("SESID", true),

         // extreme hashing:  right column
         HtmlNodeFilters.tagWithAttribute("table", "width", "186"),
         // filters out the tag with a changing Viewed By counter on issue page
         HtmlNodeFilters.tagWithTextRegex("td", "^Pages", true),

         // removes top center menu items
         HtmlNodeFilters.tagWithAttribute("table", "width", "781"),
         // this will catch the "Current Issue" in the top center, which changes 
         // when the new issue changes, plus another center menu
         HtmlNodeFilters.tagWithAttribute("td", "align", "center"),
         // removes a potentially changing "Impact Factor" number on TOC
         HtmlNodeFilters.tagWithAttribute("span", "style", "color: rgb(128,0,0)"),
         // removes footer stuff
         HtmlNodeFilters.tagWithAttribute("table", "width", "500"),
        
         // filters out the tag with a changing Viewed By counter on abstract page
         // (also filtering out doi and pub date - not ideal, but..
         HtmlNodeFilters.tagWithAttribute("td", "class", "green2"), 
         // the following still removes too much ... 
         //HtmlNodeFilters.tagWithTextRegex("td", "Viewed by:", true),
        
    };

    OrFilter oFilter = new OrFilter(filters);
    // filters out the footer tag with a changing date/time   
    HtmlTransform transform = 
      HtmlNodeFilterTransform.exclude(oFilter);
    return new HtmlFilterInputStream(in,
        encoding,
        transform);
  }

}
