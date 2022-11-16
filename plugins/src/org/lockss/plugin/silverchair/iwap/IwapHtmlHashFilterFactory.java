/*
 * $Id$
 */

/*

Copyright (c) 2018 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.plugin.silverchair.iwap;

import java.io.*;

import org.htmlparser.NodeFilter;
import org.lockss.daemon.PluginException;
import org.lockss.filter.html.*;
import org.lockss.plugin.*;
import org.lockss.plugin.silverchair.BaseScHtmlHashFilterFactory;
import org.lockss.util.Logger;

public class IwapHtmlHashFilterFactory extends BaseScHtmlHashFilterFactory {

  protected boolean doExtraSpecialFilter() {
    return false;
  }

  protected boolean doXForm() {
    return true;
  }

  private static final Logger log = Logger.getLogger(IwapHtmlHashFilterFactory.class);
  
  @Override
  public InputStream createFilteredInputStream(final ArchivalUnit au,
                                               InputStream in,
                                               String encoding)
      throws PluginException {
    
    NodeFilter[] includeFilters = new NodeFilter[] {
        // <div class="widget-ContentBrowseByYearManifest widget-instance-IssueBrowseByYear">
        HtmlNodeFilters.tagWithAttributeRegex("div", "class", "widget-Content.+Manifest"),
        // <div id="ArticleList">
        HtmlNodeFilters.tagWithAttributeRegex("div", "class", "article-list-resources"),
        HtmlNodeFilters.tagWithAttributeRegex("div", "id", "ContentColumn"),
        //In Nov/2022, it is found ".tif" file are wrapped in html as "html/text" format,
        // For example: https://iwaponline.com/view-large/figure/2854688/h2open-d-21-00139f01.tif,
        HtmlNodeFilters.tagWithAttributeRegex("div", "class", "figure-wrapper"),
        HtmlNodeFilters.tagWithAttributeRegex("div", "class", "site-theme-header-menu-item")
    };
    
    NodeFilter[] moreExcludeFilters = new NodeFilter[] {
        HtmlNodeFilters.tagWithAttribute("div","class", "kwd-group"),
        HtmlNodeFilters.tagWithAttributeRegex("div", "class", "author-info-wrap"),
        HtmlNodeFilters.tagWithAttributeRegex("div", "class", "pub-history-wrap"),
    };
    
    return createFilteredInputStream(au, in, encoding, includeFilters, moreExcludeFilters);
  }
}
