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

package org.lockss.plugin.pubfactory;

import org.htmlparser.Node;
import org.htmlparser.NodeFilter;
import org.htmlparser.Tag;
import org.htmlparser.filters.OrFilter;
import org.htmlparser.filters.TagNameFilter;
import org.htmlparser.tags.Bullet;
import org.htmlparser.tags.Div;
import org.htmlparser.util.NodeList;
import org.htmlparser.visitors.NodeVisitor;
import org.lockss.filter.FilterUtil;
import org.lockss.filter.html.*;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.plugin.FilterFactory;
import org.lockss.util.ReaderInputStream;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.util.Vector;

// Keeps contents only (includeNodes), then hashes out unwanted nodes 
// within the content (excludeNodes).
public class PubFactoryHtmlHashFilterFactory implements FilterFactory {
     
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
        /*
        // Metrics on AMetSoc https://journals.ametsoc.org/view/journals/wcas/12/2/wcas-d-19-0115.1.xml
        // class name is big e.g. "component component-content-item component-container container-metrics container-wrapper-43132"
        HtmlNodeFilters.tagWithAttributeRegex("div", "class", "container-metrics"),
        // same with related content
        HtmlNodeFilters.tagWithAttributeRegex("div", "class", "component-related-content"),
        */
        // Get rid of entire sidebar, as it has lots of dynamic ids etc
        // "component component-content-item component-container container-sideBar container-wrapper-43148 container-accordion"
        HtmlNodeFilters.tagWithAttributeRegex("div", "class", "container-sideBar"),
        // get rid of volume dropdown, it similarly has generated ids
        HtmlNodeFilters.tagWithAttributeRegex("div", "class", "component-volume-issue-selector"),
    };
    
    return getFilteredInputStream(au, in, encoding, 
                                  includeNodes, excludeNodes);
  }
  /*
  HtmlTransform xform = new HtmlTransform() {
    @Override
    public NodeList transform(NodeList nodeList) throws IOException {
      try {
        nodeList.visitAllNodesWith(new NodeVisitor() {
          @Override
          public void visitTag(Tag tag) {
            String tagName = tag.getTagName().toLowerCase();
            /* remove these
            * <ul class="ajax-zone m-0 t-zone" id="zone115228561_1">
            * <ul data-menu-list="list-id-567363a7-9393-49e7-
            * <li ... data-menu-item="list-id-fe284
            *
            *

            if ("ul".equals(tagName) && (tag.getAttribute("id") != null) {
              String tagAttr = tag.getAttribute("id");
              if (tagAttr.contains("zone") || tagAttr.contains("zone")) {
                tag.setAttribute("id", "");
              }
            } /* remove these
             * <div data-popover-fullscreen="false"

            else if (("div".equals(tagName)) && (tag.getAttribute("id")!= null) &&
                ) {
              if (tag.getAttribute("class") != null)
                tag.removeAttribute("class");
            } /* remove these
             * <button data-popover-anchor="0979a884-7df8-4d05-a54...

            else if () {

          }
        });
      }
      catch (Exception exc) {
        log.debug2("Internal error (visitor)", exc); // Ignore this tag and move on
      }
      return nodeList;
    }
  };
*/
  
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
