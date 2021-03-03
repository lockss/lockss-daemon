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

package org.lockss.plugin.atypon.americansocietyofcivilengineers;

import java.io.InputStream;

import org.htmlparser.NodeFilter;
import org.lockss.filter.html.*;
import org.lockss.plugin.*;
import org.lockss.plugin.atypon.BaseAtyponHtmlHashFilterFactory;

public class ASCEHtmlHashFilterFactory extends BaseAtyponHtmlHashFilterFactory {
  // include a whitespace filter
  @Override
  public boolean doWSFiltering() {
    return true;
  }
  // include a tag filter - some pages for ASCE changed their html like so:
  // new:<li class="articleToolLi showPDF">
  // old:<li class="articleToolLi">
  // Polls would eventually match, as pages updated, but removing tags
  // would make the hashing go smoother, especially as they switch others of 
  // their journals
  @Override
  public boolean doTagRemovalFiltering() {
    return true;
  }
  @Override
  public InputStream createFilteredInputStream(ArchivalUnit au,
      InputStream in, String encoding) {
    
    NodeFilter[] asceFilters = new NodeFilter[] {
        /*
         * This section is from < 2017
         */
        // <header> filtered in BaseAtypon
        HtmlNodeFilters.tagWithAttribute("div", "id", "issueNav"),
        HtmlNodeFilters.tagWithAttributeRegex("div", "id", "tocTools"),
        HtmlNodeFilters.tagWithAttributeRegex("td", "class", "toggle"),
        //article, toc: <div class="dropzone ui-corner-all " 
        // id="dropzone-Left-Sidebar"> - tornados ad, session history.
        HtmlNodeFilters.tagWithAttribute("div", "id", "dropzone-Left-Sidebar"),	
        //toc: <div class="citation tocCitation">
        HtmlNodeFilters.tagWithAttribute("div", "class", "citation tocCitation"),
        // footer and footer_message filtered in BaseAtypon
        // removing keywords section, author names from html page 
        //  - some versions have "action/doSearch..."
        HtmlNodeFilters.tagWithAttribute("div", "class", "abstractKeywords"),
        HtmlNodeFilters.tagWithAttribute("div", "class", "artAuthors"),
        // removing a doi link that sometimes has a class name
        HtmlNodeFilters.tagWithAttribute("a", "class", "ShowPdfGa"),
        // the addition of "Abstract:" between authors and actual text, seems the only usage
        // oddly, it doesn't always show on the screen, but it's there
        HtmlNodeFilters.tagWithAttribute("h2", "class", "display"),
        /*
         * This section is for 2017+
         */
        // TOC - links to all other issues
        HtmlNodeFilters.tagWithAttributeRegex("div", "class", "journalMetaBackground"),
        // Article landing - ajax tabs
        HtmlNodeFilters.tagWithAttributeRegex("div", "id", "recommendedtabcontent"),
        HtmlNodeFilters.tagWithAttributeRegex("div", "class", "editorialRelated"),

    };
    
    // super.createFilteredInputStream adds asceFilters to the baseAtyponFilters
    // and returns the filtered input stream using an array of NodeFilters that 
    // combine the two arrays of NodeFilters.
    return super.createFilteredInputStream(au, in, encoding, asceFilters);
    }
    
}
