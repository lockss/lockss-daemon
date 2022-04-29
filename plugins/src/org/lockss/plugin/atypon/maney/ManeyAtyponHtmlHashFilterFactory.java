/*

Copyright (c) 2000-2022, Board of Trustees of Leland Stanford Jr. University

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

package org.lockss.plugin.atypon.maney;

import java.io.InputStream;
import org.htmlparser.NodeFilter;
import org.lockss.filter.html.*;
import org.lockss.plugin.*;
import org.lockss.plugin.atypon.BaseAtyponHtmlHashFilterFactory;

public class ManeyAtyponHtmlHashFilterFactory 
  extends BaseAtyponHtmlHashFilterFactory {

  @Override
  public InputStream createFilteredInputStream(ArchivalUnit au,
      InputStream in,
      String encoding) {
    NodeFilter[] filters = new NodeFilter[] {
        // handled by parent: script, sfxlink, stylesheet, pdfplus file sise
        
        //  toc - right below breadcrumbs, journal section with current
        HtmlNodeFilters.tagWithAttribute("div",  "id", "Journal Header"),
        // under TOC issue information, select all access icons and dropdown
        HtmlNodeFilters.tagWithAttributeRegex("ul", "class", "access-options"),
        // abs, full, ref - compact journal header box on right column
        // http://www.maneyonline.com/doi/abs/10.1179/1743676113Y.0000000112
        HtmlNodeFilters.tagWithAttribute("div", "id", "compactJournalHeader"),
        // full - this seems unused but may get turned on
        // http://www.maneyonline.com/doi/full/10.1179/0076609714Z.00000000032
        HtmlNodeFilters.tagWithAttribute("div",  "id", "MathJax_Message"),
        // all pages - verify email message appears in certain content
        // machines but not all
        HtmlNodeFilters.tagWithAttributeRegex("div", "class", 
                                              "literatumMailVerificationWidget"),
        // abs - right sidebar - Citation part
        HtmlNodeFilters.tagWithAttributeRegex("div",  "class", 
                                              "literatumContentItemCitation"),
        // toc - Full/Open access
        HtmlNodeFilters.tagWithAttributeRegex("div", "class", 
                                              "tocListDropZone"),
        // toc - unused - potential issue
        HtmlNodeFilters.tagWithAttributeRegex("div", "class", 
                                              "tocListtDropZone2"),
        // toc, abs, full, ref - News & alerts box near bottom
        // with About this Journal and Editors & Editorial Board tabs  and
        // right column Most read/Most cited/Editor's Choice
        HtmlNodeFilters.tagWithAttribute("div", "aria-relevant", "additions"), 
        //  toc - bottom right column, "Subject resources"
        HtmlNodeFilters.tagWithAttributeRegex("div", "class", "literatumSerialSubjects"),
        // these tools and bookmarks are inconsistent
        HtmlNodeFilters.tagWithAttributeRegex("div", "class", "general-bookmark-share"),
        HtmlNodeFilters.tagWithAttributeRegex("section", "class", "ArticleTools"),
        // http://www.maneyonline.com/doi/ref/10.1179/0309072814Z.00000000030
        // one version contained a contentLinkHolder within retracted-publication
        HtmlNodeFilters.tagWithAttribute("div", "class" ,"retracted-publication"),
        HtmlNodeFilters.tagWithAttribute("div", "class" ,"contentLinkHolder"),
        // extra widgets
        HtmlNodeFilters.tagWithAttributeRegex("div", "class", "widget +general-html +none +"),
        // toc - right column - Published on behalf of,  Journal services,
        // For authors, Related content search,  Usage Downloaded count
        // also abs, full - right column of an article - all article tools 
        // except downloadCitations
        HtmlNodeFilters.allExceptSubtree(
            HtmlNodeFilters.tagWithAttributeRegex( 
                "section", "class", "widget-titlebar"),
            HtmlNodeFilters.tagWithAttributeRegex(
                "a", "href", "/action/showCitFormats\\?"))
    };

    // super.createFilteredInputStream adds maney filters to the 
    // baseAtyponFilters and returns the filtered input stream using an array 
    // of NodeFilters that combine the two arrays of NodeFilters
    return super.createFilteredInputStream(au, in, encoding, filters);
  }

  // turn on all id tags filtering - ids are generated
  @Override
  public boolean doTagIDFiltering() {
    return true;
  }
  
  // turn on white space filter
  @Override
  public boolean doWSFiltering() {
    return true;
  }
  
}

