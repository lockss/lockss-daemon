/*

Copyright (c) 2000-2021, Board of Trustees of Leland Stanford Jr. University
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

package org.lockss.plugin.pensoft;

import java.io.*;

import org.htmlparser.NodeFilter;


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
