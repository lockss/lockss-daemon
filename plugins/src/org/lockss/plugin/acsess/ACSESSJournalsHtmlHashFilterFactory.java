/*
 * $Id$
 */

/*

Copyright (c) 2000-2015 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.plugin.acsess;

import java.io.InputStream;
import java.io.Reader;

import org.htmlparser.*;
import org.htmlparser.filters.OrFilter;
import org.htmlparser.filters.TagNameFilter;
import org.lockss.filter.FilterUtil;
import org.lockss.filter.WhiteSpaceFilter;
import org.lockss.filter.html.HtmlCompoundTransform;
import org.lockss.filter.html.HtmlFilterInputStream;
import org.lockss.filter.html.HtmlNodeFilterTransform;
import org.lockss.filter.html.HtmlNodeFilters;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.plugin.FilterFactory;
import org.lockss.util.ReaderInputStream;

// Keeps contents only (includeNodes), then hashes out unwanted nodes 
// within the content (excludeNodes).
public class ACSESSJournalsHtmlHashFilterFactory implements FilterFactory {
     
  @Override
  public InputStream createFilteredInputStream(ArchivalUnit au,
                                               InputStream in, 
                                               String encoding) {
    NodeFilter[] includeNodes = new NodeFilter[] {
        // toc - content
        // <div class="acsMarkLogicWrapper">
        HtmlNodeFilters.tagWithAttribute("div", "class", "acsMarkLogicWrapper"),        
        // abs, full - content block - ?? use inside_one to cover citation-manager page
        // <div id="content-block"
        HtmlNodeFilters.tagWithAttribute("div", "id", "content-block"),
        // abs, full - content
        // <div class="inside_one">
        //HtmlNodeFilters.tagWithAttribute("div", "class", "inside_one"),
        // abs, full - content box - left sidebar
        // <div class"content-box"
        HtmlNodeFilters.tagWithAttribute("div", "class", "content-box"),
        // tables-only - tables
        // <div class="table-expansion"
        HtmlNodeFilters.tagWithAttribute("div", "class", "table-expansion"),
        // figures-only - images
        // <div class="fig-expansion"
        HtmlNodeFilters.tagWithAttribute("div", "class", "fig-expansion"),
        // ?? citation manager - footer - what to include - no unique class/id            
        // https://dl.sciencesocieties.org/publications/citation-manager/prev/zt/aj/106/5/1677
        // <body class="not-front not-logged-in page-publications no-sidebars lightbox-processed">
        HtmlNodeFilters.tagWithAttributeRegex("body", "class", "no-sidebars"),                                                          
    };
    
    NodeFilter[] excludeNodes = new NodeFilter[] {
        // generally we should not remove the whole <head> tag
        // since it contains metadata and css. However, since ris file is 
        // used to extract metadata and css paths looks varied, it makes
        // sense to remove the whole <head> tag
        new TagNameFilter("head"),
        new TagNameFilter("script"),
        new TagNameFilter("noscript"),
        // stylesheets
        HtmlNodeFilters.tagWithAttribute("link", "rel", "stylesheet"),
        //filter out comments
        HtmlNodeFilters.comment(),
        // citation-manager - header
        // <div id="header">
        HtmlNodeFilters.tagWithAttribute("div", "id", "header"),
        // citation-manager - has generated id
        // <div id="member_panel"
        HtmlNodeFilters.tagWithAttribute("div", "id", "member_panel"),
        // toc - links to facebook and twitter near footer
        HtmlNodeFilters.tagWithAttributeRegex("div", "class", "noPrint"),  
        // abs, full - all left column except Citation Mgr (download citations)
        HtmlNodeFilters.allExceptSubtree(
           HtmlNodeFilters.tagWithAttributeRegex("div", "class", "cb-contents"),
             HtmlNodeFilters.tagWithAttributeRegex(
                    "a", "href", "/publications/citation-manager/")),                                           
          
        // ?? citation manager - footer - what to include - no unique class/id            
        // https://dl.sciencesocieties.org/publications/citation-manager/prev/zt/aj/106/5/1677
        // <div id="footer"> 
        HtmlNodeFilters.tagWithAttribute("div", "id", "footer"),    
        // ?? hash out because it might be consistent at different download time
        // <div class="openAccess">OPEN ACCESS</div>
        HtmlNodeFilters.tagWithAttribute("div", "class", "openAccess"),  
        // full - article footnotes
        // <div id="articleFootnotes"
        HtmlNodeFilters.tagWithAttribute("div", "id", "articleFootnotes"), 
        
        // abs - bottom 
        // ttps://dl.sciencesocieties.org/publications/aj/abstracts/106/1/1
        // ?? how to hash out copyright
        // <span><span xmlns="">Copyright © 2014. </span>.&nbsp;</span>
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
    return new ReaderInputStream(new WhiteSpaceFilter(reader)); 
  }

}
