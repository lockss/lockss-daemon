/* $Id: FutureScienceHtmlCrawlFilterFactory.java,v 1.2 2013-08-06 21:09:32 aishizaki Exp $
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

package org.lockss.plugin.atypon.futurescience;

import java.io.InputStream;

import org.htmlparser.NodeFilter;
import org.htmlparser.filters.OrFilter;
import org.lockss.daemon.PluginException;
import org.lockss.filter.html.*;
import org.lockss.plugin.*;
import org.lockss.plugin.atypon.BaseAtyponHtmlCrawlFilterFactory;

/*
 *
 * created because article links are not grouped under a journalid or volumeid,
 * but under article ids - will pull the links from the page, so filtering out
 * extraneous links
 * 
 */
public class FutureScienceHtmlCrawlFilterFactory extends BaseAtyponHtmlCrawlFilterFactory {
  //PrevArt/NextArt and PrevIss/NextIss okay - terminate at boundaries 
  //Browse Volumes section okay because crawl rules will exclude
  NodeFilter[] filters = new NodeFilter[] {
      // article pages (abstract, reference, full) have a "cited by" section which will change over time
      //HtmlNodeFilters.tagWithAttribute("div", "class", "citedBySection"),

      // articles have a section "Users who read this also read..." which is tricky to isolate
      // It's a little scary, but <div class="full_text"> seems only to be used for this section (not to be confused with fulltext)
      // though I could verify that it is followed by <div class="header_divide"><h3>Users who read this article also read:</h3></div>
      HtmlNodeFilters.tagWithAttribute("div", "class", "full_text"),

      //bibliography on an article page
      HtmlNodeFilters.tagWithAttribute("table",  "class", "references"),

  };

  public InputStream createFilteredInputStream(ArchivalUnit au,
      InputStream in, String encoding) throws PluginException{ 
    return super.createFilteredInputStream(au, in, encoding, filters);
  }

}

