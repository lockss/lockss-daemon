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
        
        // toc - pageHeader - top down to breadcrumbs, above journalHeader
        // http://www.maneyonline.com/toc/his/36/4
        HtmlNodeFilters.tagWithAttributeRegex("div", "id", "pageHeader"),
        // pageFooter
        HtmlNodeFilters.tagWithAttribute("div", "id", "pageFooter"),
        //  toc - right below breadcrumbs, journal section with current
        HtmlNodeFilters.tagWithAttribute("div",  "id", "Journal Header"),
        // under TOC issue information, select all access icons and dropdown
        HtmlNodeFilters.tagWithAttributeRegex("ul", "class", "access-options"),
        // toc - access icon status of article
        HtmlNodeFilters.tagWithAttribute("td", "class" ,"accessIconContainer"),
        //  toc - ad above News & Alerts
        // http://www.maneyonline.com/toc/his/36/4
        HtmlNodeFilters.tagWithAttributeRegex("div", "class", "literatumAd"),
        // toc - Prev/Next - probably not a problem, but not content either
        HtmlNodeFilters.tagWithAttributeRegex("div",  "class", 
                                              "literatumBookIssueNavigation"),                                            
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
        // abs - potential issue like 'corrigendum' from a figure page of
        // Endocrine Society
        HtmlNodeFilters.tagWithAttributeRegex("div", "class", 
                                              "articleMetaDrop"), 
        // abs - right sidebar - Citation part
        HtmlNodeFilters.tagWithAttributeRegex("div",  "class", 
                                              "literatumContentItemCitation"),
        // full - section choose pulldown appeared in multiple sections
        HtmlNodeFilters.tagWithAttribute("div",  "class", "sectionJumpTo"),
        // toc - Full/Open access
        HtmlNodeFilters.tagWithAttributeRegex("div", "class", 
                                              "tocListDropZone"),
        // toc - unused - potential issue
        HtmlNodeFilters.tagWithAttributeRegex("div", "class", 
                                              "tocListtDropZone2"),                                                                                    
        // toc - For selected items dropdown next to Full/Open access                                   
        HtmlNodeFilters.tagWithAttributeRegex("div", "class", 
                                              "publicationToolContainer"),       
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

