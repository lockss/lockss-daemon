/* $Id: PensoftHtmlHashFilterFactory.java,v 1.4 2013-10-10 23:17:52 aishizaki Exp $ */
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

Except as contained in this notice, tMassachusettsMedicalSocietyHtmlFilterFactoryhe name of Stanford University shall not
be used in advertising or otherwise to promote the sale, use or other dealings
in this Software without prior written authorization from Stanford University.

 */

package org.lockss.plugin.pensoft;

import java.io.*;

import org.htmlparser.*;
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
         * On the other hand, do check for content on abstract/TOC pages-
         * there's an issue where that content disappears after certain
         * types of hashes, as their html may have a problem...
         */
         HtmlNodeFilters.commentWithString("Load time", true),
         // filters out the left columns
         HtmlNodeFilters.tagWithAttribute("td", "class", "textver10"),
         HtmlNodeFilters.tagWithAttribute("div", "id", "newscont"),
         // this should pick up all the right columns
         HtmlNodeFilters.tagWithAttribute("table", "width", "186"),

         // remove all script tags
         HtmlNodeFilters.tag("script"), 
         HtmlNodeFilters.tag("noscript"),  

         // filters out tags with SESIDs which can change
         HtmlNodeFilters.tagWithAttributeRegex("a", "href", ".*SESID=.*", true),
         HtmlNodeFilters.tagWithAttributeRegex("a", "onClick", ".*SESID=.*", true),
         HtmlNodeFilters.tagWithAttributeRegex("input", "name", ".*SESID.*", true),
         HtmlNodeFilters.tagWithAttributeRegex("iframe", "src", ".*SESID=.*", true),
         HtmlNodeFilters.tagWithAttributeRegex("tr", "onClick", ".*SESID=.*", true),

         HtmlNodeFilters.commentWithString("SESID", true),

         // filters out the tag with a changing Viewed By counter   
         HtmlNodeFilters.tagWithAttribute("td", "class", "green2"),        
         HtmlNodeFilters.tagWithAttribute("td", "class", "green"),

         //
         HtmlNodeFilters.tagWithAttribute("td", "class", "texttah11"),
         HtmlNodeFilters.tagWithAttribute("table", "class", "texttah11"),

         // this will catch the "Current Issue" in the top center, which changes 
         // when the new issue changes, plus another center menu
         HtmlNodeFilters.tagWithAttribute("td", "align", "center"),

         //HtmlNodeFilters.tagWithTextRegex("td", "Current Issue", true),    // removes most content (TOC/abstract)!
         //HtmlNodeFilters.tagWithTextRegex("td", "Viewed by", true),        // removes most content (TOC/abstract)!

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
