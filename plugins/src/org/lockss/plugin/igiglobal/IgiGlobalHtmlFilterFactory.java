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

package org.lockss.plugin.igiglobal;

import java.io.*;

import org.htmlparser.*;
import org.htmlparser.filters.*;
import org.htmlparser.tags.*;
import org.lockss.daemon.PluginException;
import org.lockss.filter.FilterUtil;
import org.lockss.filter.HtmlTagFilter;
import org.lockss.filter.StringFilter;
import org.lockss.filter.WhiteSpaceFilter;
import org.lockss.filter.HtmlTagFilter.TagPair;
import org.lockss.filter.html.*;
import org.lockss.plugin.*;
import org.lockss.util.Logger;
import org.lockss.util.ReaderInputStream;

public class IgiGlobalHtmlFilterFactory implements FilterFactory {

  static Logger log = Logger.getLogger(IgiGlobalHtmlFilterFactory.class);
  
  @Override
  public InputStream createFilteredInputStream(ArchivalUnit au,
                                               InputStream in,
                                               String encoding)
      throws PluginException {
    
    // First filter with HtmlParser
    NodeFilter[] filters = new NodeFilter[] {
        /*
         * Broad area filtering
         */
        // Header
        HtmlNodeFilters.tagWithAttribute("div", "class", "HeaderTop"),
        HtmlNodeFilters.tagWithAttribute("div", "class", "HeaderBottom"),
        // Left column
        HtmlNodeFilters.tagWithAttribute("div", "id", "SidebarLeft"),
        HtmlNodeFilters.tagWithAttribute("div", "class", "SidebarLeft"), // (old)
        // Right column
        HtmlNodeFilters.tagWithAttribute("div", "class", "SidebarRight"),
        // Footer
        HtmlNodeFilters.tagWithAttribute("div", "class", "Footer"),
        
        /*
         * Hash filter
         */
        // Contains ad-specific cookies
        new TagNameFilter("script"),
        // Contains dynamic css URLs
        new TagNameFilter("link"),
        // Changed from "IGI Global - Foo" to "Foo | IGI Global"
        new TagNameFilter("title"),
        // Various <br> tags added or removed over time
        new TagNameFilter("br"),
        //comments
        HtmlNodeFilters.comment(),

        HtmlNodeFilters.tagWithAttributeRegex("span", "id", "CenterContent.*Header"),
        //hidden inputs with changing keys
        HtmlNodeFilters.tagWithAttribute("input", "id", "__VIEWSTATE"),
        HtmlNodeFilters.tagWithAttribute("input", "id", "__EVENTVALIDATION"),
        // Pre-made citations of the article that include an access date
        HtmlNodeFilters.tagWithAttribute("div", "id", "citation"),
        // Trial access icon this conceivably will go away or depend on the subscription type of the library.
        // The surrounding div is hard to identify, but likely to be removed along with the image so we find 
        // the image and then remove the surrounding div.
        new NodeFilter() {
            public boolean accept(Node node) {
            	if (!(node instanceof Div)) return false;
            	Div div = (Div)node;
            	if (!"FloatRight".equalsIgnoreCase(div.getAttribute("class"))) return false;
            	String divContents = div.getChildrenHTML();
            	return divContents.length() < 150 && divContents.contains("/Images/trialaccess.png");

            }
        },
        
        // 'login' sometimes clickable, sometimes with base URL
        HtmlNodeFilters.tagWithAttributeRegex("a", "href", "/gateway/login/?$"),
        // Article titles
        HtmlNodeFilters.tagWithAttribute("div", "class", "Title1 BorderBottom"),
        HtmlNodeFilters.tagWithAttributeRegex("h2", "style", "border-bottom"),

        
        // Search box
        HtmlNodeFilters.tagWithAttribute("span", "class", "search-contents"),
        // Styling and markup of full text icons changed over time
        HtmlNodeFilters.tagWithAttributeRegex("a", "id", "_ucViewFullText_lnkView(Pdf|Xml)$"),
        // Structure of PDF landing pages changed over time
        HtmlNodeFilters.tagWithAttribute("div", "id", "ctl00_cphCenterContent_pnlContentNav"),
        HtmlNodeFilters.tagWithAttribute("a", "id", "ctl00_cphCenterContent_lnkDownloadPdf"),
        HtmlNodeFilters.tagWithAttribute("div", "id", "ctl00_cphCenterContent_pnlContentNavBottom"),
        
        // IGI Global books identifies library with access in header
        HtmlNodeFilters.tagWithAttribute("span", "id", "ctl00_ctl00_cphMain_cphCenter_lblHeader"),
        // removing cite/cited by/favorite buttons, if it's in a <span style="display:inline-block;"> tag
        new TagNameFilter("span") {
          @Override
          public boolean accept(Node node) {
            if (super.accept(node)) {
              String spanStyle = ((CompositeTag) node).getAttribute("style");
              if(spanStyle != null && spanStyle.equals("display:inline-block;")){
                String spanText = ((CompositeTag) node).toPlainTextString();
                if ((spanText.toLowerCase().contains("cite")) || 
                    (spanText.toLowerCase().contains("favorite"))) {
                  return true;
                }
              } 
            }
            return false;
          }
        },
        // <h3> replaced <h4> or vice versa at one point
        new TagNameFilter("h3"),
        new TagNameFilter("h4"),
        
    };
    
    // Do the initial html filtering
    InputStream filteredStream = new HtmlFilterInputStream(in, encoding,
        HtmlNodeFilterTransform.exclude(new OrFilter(filters)));

    // Remove all inner tag content
    Reader noTagFilter = new HtmlTagFilter(new StringFilter(FilterUtil.getReader(filteredStream, encoding), "<", " <"), new TagPair("<", ">"));
    
    Reader noWhiteSpace = new WhiteSpaceFilter(noTagFilter);
    
    // Do WhiteSpaceFilter
    return new ReaderInputStream(noWhiteSpace);
  }
  
}
