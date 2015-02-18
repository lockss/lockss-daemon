/* $Id$
 */

/*

Copyright (c) 2000-2013 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.plugin.atypon.bloomsburyqatar;

import java.io.InputStream;

import org.htmlparser.NodeFilter;
import org.lockss.daemon.PluginException;
import org.lockss.filter.html.HtmlNodeFilters;
import org.lockss.plugin.*;
import org.lockss.plugin.atypon.BaseAtyponHtmlCrawlFilterFactory;

/*
 *
 * created because article links are not grouped under a journalid or volumeid,
 * but under article ids - will pull the links from the page, so filtering out
 * extraneous links
 * 
 */
public class BloomsburyQatarHtmlCrawlFilterFactory extends BaseAtyponHtmlCrawlFilterFactory {


  //PrevArt/NextArt and PrevIss/NextIss okay - terminate at boundaries 
  NodeFilter[] filters = new NodeFilter[] {
      //citedBySection is handled in the parent
      
      // can't filter out entire rightCol because we need 'download citations' link 
      HtmlNodeFilters.tagWithAttribute("div", "id", "journalInfoPanel"),
      HtmlNodeFilters.tagWithAttribute("div", "id", "topArticlesTabs"),
      HtmlNodeFilters.tagWithAttribute("div", "id", "toolsPanel"),
      
      // do prev-next article as protection on overcrawling
      HtmlNodeFilters.tagWithAttribute("div", "class", "articleNavigation"),
      // breadcrumb which would lead back to toc as protection on overcrawling
      HtmlNodeFilters.tagWithAttribute("div", "id", "breadcrumb"),
      
      // the tab with references for the article which could lead to other articles
      HtmlNodeFilters.tagWithAttribute("div", "id", "referencesTab"),
      
      
  };

  @Override
  public InputStream createFilteredInputStream(ArchivalUnit au,
      InputStream in, String encoding) throws PluginException{ 
    return super.createFilteredInputStream(au, in, encoding, filters);
  }

}

