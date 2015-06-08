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
        // toc, issue - content
        // https://dl.sciencesocieties.org/publications/aj/tocs/106
        // https://dl.sciencesocieties.org/publications/aj/tocs/106/1
        HtmlNodeFilters.tagWithAttribute("div", "class", "acsMarkLogicWrapper"),        
        // abs, full - content block
        HtmlNodeFilters.tagWithAttribute("div", "id", "content-block"),
        // tables-only - tables
        HtmlNodeFilters.tagWithAttribute("div", "class", "table-expansion"),
        // figures-only - images
        HtmlNodeFilters.tagWithAttribute("div", "class", "fig-expansion"),
        // citation manager - footer      
        // https://dl.sciencesocieties.org/publications/citation-manager/prev/zt/aj/106/5/1677
        HtmlNodeFilters.tagWithAttributeRegex("body", "class", "no-sidebars"),                                                          
    };
    
    NodeFilter[] excludeNodes = new NodeFilter[] {
        new TagNameFilter("script"),
        new TagNameFilter("noscript"),
        //filter out comments
        HtmlNodeFilters.comment(),
        // toc - links to facebook and twitter near footer
        HtmlNodeFilters.tagWithAttributeRegex("div", "class", "noPrint"),
        // citation-manager - header
        // https://dl.sciencesocieties.org/publications/citation-manager/prev/zt/aj/106/5/1677
        HtmlNodeFilters.tagWithAttribute("div", "id", "header"),
        // toc, issue, citatton-manager - 
        // containing "Subscription brought to you by Stanford University"
        // or other University
        HtmlNodeFilters.tagWithAttribute("div", "id", "below_header"),
        // citation-manager - has generated id
        HtmlNodeFilters.tagWithAttribute("div", "id", "member_panel"),
        // citation manager - footer 
        HtmlNodeFilters.tagWithAttribute("div", "id", "footer"),    
        // issue - hash out because it might be consistent at different download time
        // https://dl.sciencesocieties.org/publications/aj/tocs/106/1
        HtmlNodeFilters.tagWithAttribute("div", "class", "openAccess"),  
        // full - article footnotes
        HtmlNodeFilters.tagWithAttribute("div", "id", "articleFootnotes"), 
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
