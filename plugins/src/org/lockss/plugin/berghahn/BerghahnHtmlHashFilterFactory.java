/*
 * $Id$
 */

/*

Copyright (c) 2000-2018 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.plugin.berghahn;

import java.io.InputStream;
import java.io.Reader;
import java.util.Vector;

import org.htmlparser.*;
import org.htmlparser.filters.OrFilter;
import org.htmlparser.filters.TagNameFilter;
import org.htmlparser.tags.Bullet;
import org.htmlparser.tags.Div;
import org.lockss.filter.FilterUtil;
import org.lockss.filter.html.HtmlCompoundTransform;
import org.lockss.filter.html.HtmlFilterInputStream;
import org.lockss.filter.html.HtmlNodeFilterTransform;
import org.lockss.filter.html.HtmlNodeFilters;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.plugin.FilterFactory;
import org.lockss.util.ReaderInputStream;

// Keeps contents only (includeNodes), then hashes out unwanted nodes 
// within the content (excludeNodes).
public class BerghahnHtmlHashFilterFactory implements FilterFactory {
     
  @Override
  public InputStream createFilteredInputStream(ArchivalUnit au,
                                               InputStream in, 
                                               String encoding) {
    NodeFilter[] includeNodes = new NodeFilter[] {

    		
    		// manifest just doesn't have much other than the links
    	    new NodeFilter() {
    			  @Override
    			  public boolean accept(Node node) {
    				  //plan <div> with plain <li> each with one href
    				  //href="/view/journals/boyhood-studies/10/2/boyhood-studies.10.issue-2.xml"
    			    if (HtmlNodeFilters.tagWithAttributeRegex("a", "href", "/[^/]+issue[^/]+\\.xml").accept(node)) {
    				  Node liParent = node.getParent();
    				  if (liParent instanceof Bullet) {
    				    Bullet li = (Bullet)liParent;
    					Vector liAttr = li.getAttributesEx();
    					if (liAttr != null && liAttr.size() == 1) {
    					  Node divParent = li.getParent();
    					  if (divParent instanceof Div) {
    					    Div div = (Div)divParent;
    						Vector divAttr = div.getAttributesEx();
    						return divAttr != null && divAttr.size() == 1;
    				      }
    					}
    				  }
    			    } 
    				return false;
    		      }
    		    },
    		//main content of TOC and article landing page (on abstract or pdf tab)
        HtmlNodeFilters.tagWithAttribute("div", "id", "readPanel"),
        // citation overlay for download of ris - this has download date
        HtmlNodeFilters.tagWithAttribute("div","id","previewWrapper"),

        // https://www.berghahnjournals.com/view/journals/boyhood-studies/12/2/bhs120201.xml -- html structure changed Oct/2020
        HtmlNodeFilters.tagWithAttributeRegex("div","class","content-box"),
    };
    
    NodeFilter[] excludeNodes = new NodeFilter[] {
        new TagNameFilter("script"),
        new TagNameFilter("noscript"),
        // filter out comments
        HtmlNodeFilters.comment(),
        // citation overlay for download of ris - this has download date
        // and the ris citation has a one-time key
        // so just keep the referring article as a way of hashing
        HtmlNodeFilters.allExceptSubtree(
            HtmlNodeFilters.tagWithAttribute("div", "id", "previewWrapper"),
            HtmlNodeFilters.tagWithAttributeRegex("a", "href", "/view/journals/")),
        //html structure change in Oct/2020
        //https://www.berghahnjournals.com/view/journals/boyhood-studies/12/1/bhs120101.xml
        HtmlNodeFilters.tagWithAttributeRegex("div", "id", "headerWrap"),
        HtmlNodeFilters.tagWithAttributeRegex("div", "id", "footerWrap"),
        HtmlNodeFilters.tagWithAttributeRegex("div", "class", "fixed-controls"),

    };
    
    return getFilteredInputStream(au, in, encoding, 
                                  includeNodes, excludeNodes);
  }
  
  // Takes include and exclude nodes as input. Removes white spaces.
  public InputStream getFilteredInputStream(ArchivalUnit au, InputStream in,
      String encoding, NodeFilter[] includeNodes, NodeFilter[] excludeNodes) {
    if (excludeNodes == null) {
      throw new NullPointerException("excludeNodes array is null");
    }  
    if (includeNodes == null) {
      throw new NullPointerException("includeNodes array is null!");
    }   
    InputStream filtered;
    filtered = new HtmlFilterInputStream(in, encoding,
                 new HtmlCompoundTransform(
                     HtmlNodeFilterTransform.include(new OrFilter(includeNodes)),
                     HtmlNodeFilterTransform.exclude(new OrFilter(excludeNodes)))
               );
    
    Reader reader = FilterUtil.getReader(filtered, encoding);
    return new ReaderInputStream(reader); 
  }

}
