/*
 * $Id: IngentaJournalHtmlFilterFactory.java,v 1.24 2013-08-09 17:48:30 wkwilson Exp $
 */ 

/*

Copyright (c) 2000-2009 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.plugin.ingenta;

import java.io.IOException;
import java.io.InputStream;

import org.htmlparser.NodeFilter;
import org.htmlparser.Tag;
import org.htmlparser.filters.OrFilter;
import org.htmlparser.util.NodeList;
import org.htmlparser.util.ParserException;
import org.htmlparser.visitors.NodeVisitor;
import org.lockss.daemon.PluginException;
import org.lockss.filter.FilterUtil;
import org.lockss.filter.WhiteSpaceFilter;
import org.lockss.filter.html.*;
import org.lockss.plugin.*;
import org.lockss.util.Logger;
import org.lockss.util.ReaderInputStream;

public class IngentaJournalHtmlFilterFactory implements FilterFactory {
	Logger log = Logger.getLogger("IngentaJournalHtmlFilterFactory");
  public InputStream createFilteredInputStream(ArchivalUnit au,
                                               InputStream in,
                                               String encoding)
      throws PluginException {
    NodeFilter[] filters = new NodeFilter[] {
        // Filter out <div id="header">...</div>
        HtmlNodeFilters.tagWithAttribute("div", "id", "header"),
        // Filter out <div id="rightnavbar">...</div>
        HtmlNodeFilters.tagWithAttribute("div", "id", "rightnavbar"), 
        // Filter out <div id="footerarea">...</div>
        HtmlNodeFilters.tagWithAttribute("div", "id", "footerarea"),
        // Filter out <div class="article-pager">...</div>
        HtmlNodeFilters.tagWithAttribute("div", "class", "article-pager"),
        // Filter out <div id="purchaseexpand"...>...</div>
        HtmlNodeFilters.tagWithAttribute("div", "id", "purchaseexpand"),
        // Filter out <div id="moredetails">...</div>
        HtmlNodeFilters.tagWithAttribute("div", "id", "moredetails"),
        // Filter out <div id="moreLikeThis">...</div>
        HtmlNodeFilters.tagWithAttribute("div", "id", "moreLikeThis"),
        // filter out <link rel="stylesheet" href="..."> because Ingenta has
        // bad habit of adding a version number to the CSS file name
        HtmlNodeFilters.tagWithAttribute("link", "rel", "stylesheet"),
        // filter out <div class="heading"> that encloses a statement with
        // the number of references and the number that can be referenced: 
        // number of reference links won't be the same because not all 
        // the referenced articles are available at a given institution.
        HtmlNodeFilters.tagWithAttribute("div", "class", "heading"),
        // filter out <div class="advertisingbanner[ clear]"> that encloses 
        // GA_googleFillSlot("TopLeaderboard") & GA_googleFillSlot("Horizontal_banner")
        HtmlNodeFilters.tagWithAttributeRegex("div", "class", "advertisingbanner[^\"]*"),
        // filter out <li class="data"> that encloses a reference for the
        // article: reference links won't be the same because not all 
        // the referenced articles are available at a given institution.
        HtmlNodeFilters.tagWithAttribute("li", "class", "data"),
        // Filter out <div id="subscribe-links" ...> 
        // institution-specific subscription link section
        HtmlNodeFilters.tagWithAttribute("div", "id", "subscribe-links"),
        // Filter out <div id="links">...</div>
        HtmlNodeFilters.tagWithAttribute("div", "id", "links"),
        // Filter out <div id="footer">...</div>
        HtmlNodeFilters.tagWithAttribute("div", "id", "footer"),
        // Filter out <div id="top-ad-alignment">...</div>
        HtmlNodeFilters.tagWithAttribute("div", "id", "top-ad-alignment"),
        // Filter out <div id="top-ad">...</div>
        HtmlNodeFilters.tagWithAttribute("div", "id", "top-ad"),
        // Filter out <div id="ident">...</div>
        HtmlNodeFilters.tagWithAttribute("div", "id", "ident"),         
        // Filter out <div id="ad">...</div>
        HtmlNodeFilters.tagWithAttribute("div", "id", "ad"),
        // Filter out <div id="vertical-ad">...</div>
        HtmlNodeFilters.tagWithAttribute("div", "id", "vertical-ad"),
        // Filter out <div class="right-col-download">...</div>
        HtmlNodeFilters.tagWithAttribute("div", "class", "right-col-download"),
        // Filter out <div id="cart-navbar">...</div>
        HtmlNodeFilters.tagWithAttribute("div", "id", "cart-navbar"),   
        // Filter out <div class="heading-macfix article-access-options">...</div>
        HtmlNodeFilters.tagWithAttribute("div", "class", "heading-macfix"), 
        // Filter out <div id="baynote-recommendations">...</div>
        HtmlNodeFilters.tagWithAttribute("div", "id", "baynote-recommendations"),
        // Filter out <div id="bookmarks-container">...</div>
        HtmlNodeFilters.tagWithAttribute("div", "id", "bookmarks-container"),   
        // Filter out <div id="llb">...</div>
        HtmlNodeFilters.tagWithAttribute("div", "id", "llb"),   
        // Filter out <a href="...">...</a> where the href value includes "exitTargetId" as a parameter
        HtmlNodeFilters.tagWithAttributeRegex("a", "href", "[\\?&]exitTargetId="),
        // Filter out <a onclick="...">...</a> where the onclick javascript argument includes "exitTargetId" as a parameter
        HtmlNodeFilters.tagWithAttributeRegex("a", "onclick", "[\\?&]exitTargetId="),
        // Filter out <input name="exitTargetId">
        HtmlNodeFilters.tagWithAttribute("input", "name", "exitTargetId"),
        // Icon on article reference page
        HtmlNodeFilters.tagWithAttribute("span", "class", "access-icon"),
        // javascript for embedded figure has checksum & expires that changes
        //NOTE - at the moment this does not go beyond nested <p></p> pairs to the closing </a>
        //when possible in the daemon, must subclass and do this for <a> tag
        HtmlNodeFilters.tagWithAttribute("a", "class", "table-popup"),
    };
    
    HtmlTransform xform = new HtmlTransform() {
	    @Override
	    public NodeList transform(NodeList nodeList) throws IOException {
	      try {
	        nodeList.visitAllNodesWith(new NodeVisitor() {
	          @Override
	          public void visitTag(Tag tag) {
	            try {
	              if ("li".equalsIgnoreCase(tag.getTagName()) && tag.getAttribute("class") != null && tag.getAttribute("class").trim().startsWith("rowShade")) {
	                tag.setAttribute("class", "");
	              }
	              else {
	                super.visitTag(tag);
	              }
	            }
	            catch (Exception exc) {
	              log.debug2("Internal error (visitor)", exc); // Ignore this tag and move on
	            }
	          }
	        });
	      }
	      catch (ParserException pe) {
	        log.debug2("Internal error (parser)", pe); // Bail
	      }
	      return nodeList;
	    }
	  };
    
    InputStream filteredStream =  new HtmlFilterInputStream(in,
                                     encoding,
                                     new HtmlCompoundTransform(HtmlNodeFilterTransform.exclude(new OrFilter(filters)),xform));
    
    return new ReaderInputStream(new WhiteSpaceFilter(FilterUtil.getReader(filteredStream, encoding)));
  }

}


