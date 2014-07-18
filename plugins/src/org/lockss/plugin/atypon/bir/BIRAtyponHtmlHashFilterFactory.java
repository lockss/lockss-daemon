/*
 * $Id: BIRAtyponHtmlHashFilterFactory.java,v 1.1.2.2 2014-07-18 15:54:34 wkwilson Exp $
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

package org.lockss.plugin.atypon.bir;

import java.io.InputStream;

import java.io.Reader;
import org.htmlparser.NodeFilter;
import org.htmlparser.filters.TagNameFilter;
import org.lockss.filter.FilterUtil;
import org.lockss.filter.WhiteSpaceFilter;
import org.lockss.filter.html.*;
import org.lockss.plugin.*;
import org.lockss.plugin.atypon.BaseAtyponHtmlHashFilterFactory;
import org.lockss.util.Logger;
import org.lockss.util.ReaderInputStream;

/* 
 * Don't extend BaseAtyponHtmlHashFilterFactory because we need to do more 
 * extensive filtering with spaces, etc.
 */
public class BIRAtyponHtmlHashFilterFactory extends BaseAtyponHtmlHashFilterFactory {

  Logger log = Logger.getLogger(BIRAtyponHtmlHashFilterFactory.class);

  @Override
  public InputStream createFilteredInputStream(ArchivalUnit au,
      InputStream in,
      String encoding) {
    NodeFilter[] filters = new NodeFilter[] {
        // div class="citedBySection" handled in BaseAtypon
        // sfxlink handled in BaseAtypon
        //script, comments also in BaseAtypon
        
        // this is controversial - draconian; what about updated metadata
        new TagNameFilter("head"),
        new TagNameFilter("noscript"),
        // issue TOC
        // ad above header
        HtmlNodeFilters.tagWithAttributeRegex("section", "class", "^widget literatumAd"),
        // header of toc - login, etc
        HtmlNodeFilters.tagWithAttributeRegex("section", "class", "^widget pageHeader"),
        // footer
        HtmlNodeFilters.tagWithAttributeRegex("section", "class", "^widget pageFooter"),

        //  BJR image
        HtmlNodeFilters.tagWithAttributeRegex("section", "class", "^widget general-image"),
        // top menu on journal header
        HtmlNodeFilters.tagWithAttributeRegex("section", "class", "^widget menuXml"),
        //  pulldown with sections - may add citedby later
        HtmlNodeFilters.tagWithAttribute("div", "class", "publicationTooldropdownContainer"),
        // right column, current issue
        HtmlNodeFilters.tagWithAttributeRegex("section", "class", "^widget literatumBookIssueNavigation"),
        // social media stuff
        HtmlNodeFilters.tagWithAttributeRegex("section", "class", "^widget general-bookmark-share"),
        // toc - place for free, open, etc 
        HtmlNodeFilters.tagWithAttribute("td", "class", "accessIconContainer"),
        // see notice of redundant publication toc/dmfr/42/8
        HtmlNodeFilters.tagWithAttribute("a",  "class", "relatedLink"),
        // div holding original article link 
        HtmlNodeFilters.tagWithAttribute("div", "class", "relatedLayer"),

        // article page - abs
        // 'Cited By' won't be there until after it's cited
        HtmlNodeFilters.tagWithAttribute("ul", "class", "tab-nav"),
        // right column
        HtmlNodeFilters.tagWithAttributeRegex("section", "class", "^widget literatumArticleToolsWidget"),

        // article page - full
        // once cited, the pulldown menu includes "citing articles" 
        HtmlNodeFilters.tagWithAttribute("div", "class", "sectionJumpTo"),

    };

    // super.createFilteredInputStream adds bir filter to the baseAtyponFilters
    // and returns the filtered input stream using an array of NodeFilters that 
    // combine the two arrays of NodeFilters.
    InputStream superFiltered = super.createFilteredInputStream(au, in, encoding, filters);

    // Also need white space filter to condense multiple white spaces down to 1
    Reader reader = FilterUtil.getReader(superFiltered, encoding);
    return new ReaderInputStream(new WhiteSpaceFilter(reader));
  }

}




