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

package org.lockss.plugin.atypon.markallen;

import java.io.InputStream;

import org.htmlparser.NodeFilter;
import org.lockss.daemon.PluginException;
import org.lockss.filter.html.*;
import org.lockss.plugin.*;
import org.lockss.plugin.atypon.BaseAtyponHtmlCrawlFilterFactory;

// be sure not to CRAWL filter action/showCitFormats link
// might be in left or right column

public class MarkAllenHtmlCrawlFilterFactory 
  extends BaseAtyponHtmlCrawlFilterFactory {
  static NodeFilter[] filters = new NodeFilter[] {

    // handled by parent:
    // previous and next of toc
    // <td class="journalNavLeftTd">
    // <td class="journalNavRightTd">
    
    // from toc - below pageHeader, ad panel has link to other issue
    // http://www.magonlinelibrary.com/toc/bjom/21/10
    HtmlNodeFilters.tagWithAttributeRegex("div", "class", "genericSlideshow"),
                                          
    // tabb'd section in the right column -                
    // can't be in parent - all tabs would get affected, even in content
    // TODO - look at alternative, but for now the only tabs are in right column
    HtmlNodeFilters.tagWithAttribute("div", "aria-relevant", "additions"),                  
  };

  @Override
  public InputStream createFilteredInputStream(ArchivalUnit au,
      InputStream in,
      String encoding)
  throws PluginException{
    return super.createFilteredInputStream(au, in, encoding, filters);
  }
}
