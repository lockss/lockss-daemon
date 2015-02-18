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
import org.lockss.daemon.PluginException;
import org.lockss.filter.html.*;
import org.lockss.plugin.*;
import org.lockss.plugin.atypon.BaseAtyponHtmlCrawlFilterFactory;

public class ManeyAtyponHtmlCrawlFilterFactory 
  extends BaseAtyponHtmlCrawlFilterFactory {
    
  NodeFilter[] filters = new NodeFilter[] {
      
      // handled by parent:
      // previous and next of toc
      // <td class="journalNavLeftTd">
      // <td class="journalNavRightTd">
      // prev-next article
      
      // from toc, abs, full, ref - News & alerts box near bottom
      // with About this Journal and Editors & Editorial Board tabs  
      // http://www.maneyonline.com/toc/aac/112/8                                       
      HtmlNodeFilters.tagWithAttribute("div",  "id", "migrated_news"),
      HtmlNodeFilters.tagWithAttribute("div",  "id", "migrated_aims"),
      HtmlNodeFilters.tagWithAttribute("div",  "id", "migrated_editors"),                                             
      
      // don't go to references - I don't think they link direct, but be safe
      // from ref - whole table of references
      // http://www.maneyonline.com/doi/ref/10.1179/2045772313Y.0000000128
      HtmlNodeFilters.tagWithAttribute("table", "class", "references"),            
      // from toc - sidebar has hard to isolate "most read", "most cited" 
      // "editors choice" - http://www.maneyonline.com/toc/aac/112/8
      HtmlNodeFilters.tagWithAttributeRegex("div", "class", 
                                            "literatumMostReadWidget"),
      HtmlNodeFilters.tagWithAttributeRegex("div", "class", 
                                            "literatumMostCitedWidget"),
      HtmlNodeFilters.tagWithAttributeRegex("div", "class", 
                                            "publicationListWidget"),      
      // from abs, full, ref - compact journal header box on right column
      // http://www.maneyonline.com/doi/abs/10.1179/1743676113Y.0000000112
      HtmlNodeFilters.tagWithAttribute("div", "id", "compactJournalHeader"),      
      // from toc - related content/original article
      // http://www.maneyonline.com/toc/his/37/1
      HtmlNodeFilters.tagWithAttribute("div", "class", "relatedLayer"),      
      // erratum points back to original article
      // http://www.maneyonline.com/doi/full/10.1179/0147888513Z.00000000076
      HtmlNodeFilters.tagWithAttribute("div", "id", "relatedContent")
  };
  @Override
  public InputStream createFilteredInputStream(ArchivalUnit au,
      InputStream in, String encoding) throws PluginException{ 
    return super.createFilteredInputStream(au, in, encoding, filters);
  }

}
