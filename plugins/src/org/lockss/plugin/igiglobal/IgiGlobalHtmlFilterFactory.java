
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

package org.lockss.plugin.igiglobal;

import java.io.*;

import org.htmlparser.*;
import org.htmlparser.filters.*;
import org.htmlparser.tags.*;
import org.htmlparser.util.NodeList;
import org.htmlparser.util.ParserException;
import org.htmlparser.visitors.NodeVisitor;
import org.lockss.daemon.PluginException;
import org.lockss.filter.WhiteSpaceFilter;
import org.lockss.filter.html.*;
import org.lockss.plugin.*;
import org.lockss.util.Logger;
import org.lockss.util.ReaderInputStream;

public class IgiGlobalHtmlFilterFactory implements FilterFactory {

	Logger log = Logger.getLogger("IgiGlobalHtmlFilterFactoryy");
	
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
         * Hash filter
         */
        // Contains ad-specific cookies
        new TagNameFilter("script"),
        // Contains dynamic css URLs
        new TagNameFilter("link"),
        // Contains testimonials, sponsors, and news
        HtmlNodeFilters.tagWithAttribute("div", "class", "SidebarRight"),
        // Contains institution name
        HtmlNodeFilters.tagWithAttribute("span", "class", "InstitutionName"),
        HtmlNodeFilters.tagWithAttributeRegex("span", "id", "CenterContent.*Header"),
        //hidden inputs with changing keys
        HtmlNodeFilters.tagWithAttribute("input", "id", "__VIEWSTATE"),
        HtmlNodeFilters.tagWithAttribute("input", "id", "__EVENTVALIDATION"),
        //Pre-made citations of the article that include an access date
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
        
        // Stylesheets sometimes contain version numbers
        HtmlNodeFilters.tagWithAttribute("link", "rel", "stylesheet"),
        // Institution-specific stuff
        HtmlNodeFilters.tagWithAttribute("div", "class", "Institution"),
        HtmlNodeFilters.tagWithAttribute("img", "src", "/Images/institution-icon.png"),
        // Login page
        HtmlNodeFilters.tagWithAttributeRegex("a", "href", "^/gateway/login"),
        // Article titles
        HtmlNodeFilters.tagWithAttribute("div", "class", "Title1 BorderBottom"),
        HtmlNodeFilters.tagWithAttributeRegex("h2", "style", "border-bottom"),
        // Favorite button
        HtmlNodeFilters.tagWithAttribute("span", "id", "ctl00_ctl00_cphMain_cphCenter_favorite"),
        
        //IGI Global books identifies library with access in header
        HtmlNodeFilters.tagWithAttribute("span", "id", "ctl00_ctl00_cphMain_cphCenter_lblHeader"),
        //In IGI books, the footer contains sponsor image and no clear marker, but also no needed content
        HtmlNodeFilters.tagWithAttribute("div",  "class", "Footer"),
        
        // <h3> replaced <h4> or vice versa at one point
        new TagNameFilter("h3"),
        new TagNameFilter("h4")    
        
    };
    
	return new HtmlFilterInputStream(in,
              						 encoding,
              						 new HtmlCompoundTransform(HtmlNodeFilterTransform.exclude(new OrFilter(filters))));
  }
  
}
