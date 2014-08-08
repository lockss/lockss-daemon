/*
 * $Id: GPOFDSysHtmlFilterFactory.java,v 1.7 2014-08-08 22:09:02 thib_gc Exp $
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

package org.lockss.plugin.usdocspln.gov.gpo.fdsys;

import java.io.*;
import java.util.List;

import org.htmlparser.*;
import org.htmlparser.filters.*;
import org.htmlparser.util.*;
import org.htmlparser.visitors.NodeVisitor;
import org.lockss.daemon.PluginException;
import org.lockss.filter.*;
import org.lockss.filter.HtmlTagFilter.TagPair;
import org.lockss.filter.html.*;
import org.lockss.plugin.*;
import org.lockss.util.*;

public class GPOFDSysHtmlFilterFactory implements FilterFactory {

  public InputStream createFilteredInputStream(ArchivalUnit au,
                                               InputStream in,
                                               String encoding)
      throws PluginException {
	
    NodeFilter[] filters = new NodeFilter[] {
        /*
         * Broad area filtering 
         */
        /* Document header */
        // Differences in the presence and order of <meta> tags and spacing of the <title> tag
        new TagNameFilter("head"),
        /* Scripts */
        new TagNameFilter("script"),
        new TagNameFilter("noscript"),
        /* Header */
        HtmlNodeFilters.tagWithAttributeRegex("div", "id", "top-menu-one"),
        HtmlNodeFilters.tagWithAttributeRegex("div", "id", "top-banner-inside"),
        HtmlNodeFilters.tagWithAttributeRegex("div", "id", "top-menu-two"),
        /* Left column */
        HtmlNodeFilters.tagWithAttributeRegex("div", "id", "page-details-left-mask"),
        /* Footer */
        HtmlNodeFilters.tagWithAttributeRegex("div", "id", "footer"),
        /*
         * Main area
         */
        // Seen in the field: "null" in <a href="/fdsys/search/pagedetails.action?null&amp;bread=true">More Information</a>
        HtmlNodeFilters.tagWithAttributeRegex("span", "id", "breadcrumbs"),
        // Whitespace differences inside <div id="page-details-form-mask">
        HtmlNodeFilters.tagWithAttributeRegex("div", "id", "page-details-form-mask"),
        /*
         * Other
         */
        // Filters the "Email a link to this page" link
        HtmlNodeFilters.tagWithAttributeRegex("a", "href", "^search/notificationPage\\.action\\?emailBody="),
        // Filters session ID from search results
        HtmlNodeFilters.tagWithAttributeRegex("form", "action", "jsessionid="),
        // Differ over time in the presence and placement of rel="nofollow"
        HtmlNodeFilters.tagWithAttributeRegex("a", "href", "^delivery/getpackage\\.action\\?packageId="),
    };
  
    HtmlTransform xform = new HtmlTransform() {
      @Override
      public NodeList transform(NodeList nodeList) throws IOException {
        try {
          nodeList.visitAllNodesWith(new NodeVisitor() {
            @Override
            public void visitTag(Tag tag) {
              String tagName = tag.getTagName().toLowerCase();
              if ("a".equals(tagName)) {
                tag.removeAttribute("onclick");
              }
            }
          });
          return nodeList;
        }
        catch (ParserException pe) {
          throw new IOException("ParserException inside HtmlTransform", pe);
        }
      }
    };
    
    InputStream prefilteredStream =
        new HtmlFilterInputStream(in,
                                  encoding,
                                  new HtmlCompoundTransform(HtmlNodeFilterTransform.exclude(new OrFilter(filters)),
                                                            xform));
  	    
    List pairs = ListUtil.list(
      // May contain a session token in a comment
      new TagPair("<!--<input type=\"hidden\" name=\"struts.token.name\" value=\"struts.token\" />",
                  "\" />-->"),
      // ...Or not in a comment, just two <input> tags in a row
      new TagPair("<input type=\"hidden\" name=\"struts.token.name\" value=\"struts.token\" />",
                  "\" />")
    );
  	      
    try {
      Reader prefilteredReader = new InputStreamReader(prefilteredStream, encoding);
      Reader filteredReader = HtmlTagFilter.makeNestedFilter(prefilteredReader, pairs);
      Reader whitespaceReader = new WhiteSpaceFilter(filteredReader);
      return new ReaderInputStream(whitespaceReader);
    }
    catch (UnsupportedEncodingException uee) {
      throw new PluginException(uee);
    }
  }  
}
   