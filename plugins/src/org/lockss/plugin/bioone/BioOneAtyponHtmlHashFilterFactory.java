/*
 * $Id: BioOneAtyponHtmlHashFilterFactory.java,v 1.2 2011-09-19 19:58:14 thib_gc Exp $
 */

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

Except as contained in this notice, the name of Stanford University shall not
be used in advertising or otherwise to promote the sale, use or other dealings
in this Software without prior written authorization from Stanford University.

*/

package org.lockss.plugin.bioone;

import java.io.InputStream;

import org.htmlparser.NodeFilter;
import org.htmlparser.filters.*;
import org.lockss.daemon.PluginException;
import org.lockss.filter.html.*;
import org.lockss.plugin.*;


public class BioOneAtyponHtmlHashFilterFactory implements FilterFactory {

  @Override
  public InputStream createFilteredInputStream(ArchivalUnit au, InputStream in,
                                               String encoding)
      throws PluginException {
    // First filter with HtmlParser
    NodeFilter[] filters = new NodeFilter[] {
        /*
         * Crawl filter
         */
        // Contains most-read articles in the same journal (etc.)
        HtmlNodeFilters.tagWithAttribute("div", "class", "relatedContent"),
        // Contains related articles (etc.)
        HtmlNodeFilters.tagWithAttribute("div", "id", "relatedArticleSearch"),
        // Contains reverse citations
        HtmlNodeFilters.tagWithAttributeRegex("div", "id", "citingArticles"),
        // Contains reverse citations
        HtmlNodeFilters.tagWithAttribute("div", "class", "citedBySection"),
        /*
         * Hash filter
         */
        // Contains site-specific SFX code
        new TagNameFilter("script"),
        // Contains recent impact factors and journal rankings
        HtmlNodeFilters.tagWithAttribute("div", "id", "articleInfoBox"),
        // Contains site-specific SFX markup
        HtmlNodeFilters.tagWithAttribute("a", "class", "sfxLink"),
        // Contains institution-specific markup
        HtmlNodeFilters.tagWithAttribute("div", "id", "headerLogo"),
    };
    return new HtmlFilterInputStream(in,
                                     encoding,
                                     HtmlNodeFilterTransform.exclude(new OrFilter(filters)));
  }

}
