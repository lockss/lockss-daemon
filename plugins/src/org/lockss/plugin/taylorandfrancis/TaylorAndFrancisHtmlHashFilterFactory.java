/*
 * $Id: TaylorAndFrancisHtmlHashFilterFactory.java,v 1.1 2011-10-05 00:55:55 thib_gc Exp $
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

package org.lockss.plugin.taylorandfrancis;

import java.io.InputStream;

import org.htmlparser.NodeFilter;
import org.htmlparser.filters.*;
import org.lockss.daemon.PluginException;
import org.lockss.filter.html.*;
import org.lockss.plugin.*;


public class TaylorAndFrancisHtmlHashFilterFactory implements FilterFactory {

  @Override
  public InputStream createFilteredInputStream(ArchivalUnit au, InputStream in,
                                               String encoding)
      throws PluginException {
    NodeFilter[] filters = new NodeFilter[] {
        /*
         * From the crawl filter
         */
        // News articles
        HtmlNodeFilters.tagWithAttribute("div", "id", "newsArticles"),
        // Related and most read articles
        HtmlNodeFilters.tagWithAttribute("div", "id", "relatedArticles"),
        /*
         * Proper to the has filter
         */
        // Contains site-specific SFX code
        new TagNameFilter("script"),
        // Contains site-specific SFX markup
        HtmlNodeFilters.tagWithAttribute("a", "class", "sfxLink"),
        // Contains institution-specific markup
        HtmlNodeFilters.tagWithAttribute("div", "id", "branding"),
        // Contains a cookie or session ID
        HtmlNodeFilters.tagWithAttribute("link", "type", "application/rss+xml"),
        // Contains the current year in a copyright statement
        HtmlNodeFilters.tagWithAttributeRegex("div", "class", "credits"),
        // Contains a cookie or session ID
        HtmlNodeFilters.tagWithAttributeRegex("a", "href", "&feed=rss"),
    };
    return new HtmlFilterInputStream(in,
                                     encoding,
                                     HtmlNodeFilterTransform.exclude(new OrFilter(filters)));
  }

}
