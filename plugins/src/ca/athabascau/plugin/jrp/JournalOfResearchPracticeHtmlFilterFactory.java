/*
 * $Id: JournalOfResearchPracticeHtmlFilterFactory.java,v 1.1 2010-05-15 01:12:00 edwardsb1 Exp $
 */
/*
 Copyright (c) 2000-2009 Board of Trustees of Leland Stanford Jr. University,
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
package ca.athabascau.plugin.jrp;

import java.io.InputStream;

import org.htmlparser.NodeFilter;
import org.htmlparser.filters.*;
import org.lockss.daemon.PluginException;
import org.lockss.filter.html.*;
import org.lockss.plugin.*;


/**
 * @author edwardsb
 *
 */
public class JournalOfResearchPracticeHtmlFilterFactory implements
    FilterFactory {

  /* (non-Javadoc)
   * @see org.lockss.plugin.FilterFactory#createFilteredInputStream(org.lockss.plugin.ArchivalUnit, java.io.InputStream, java.lang.String)
   */               
  public InputStream createFilteredInputStream(ArchivalUnit au, InputStream in,
      String encoding) throws PluginException {
    NodeFilter[] filters = new NodeFilter[] {
        // Some OJS sites have a tag cloud
        HtmlNodeFilters.tagWithAttribute("div", "id", "sidebarKeywordCloud"),
        HtmlNodeFilters.tagWithAttribute("table", "id", "table1"),        
    };
    OrFilter orFilter = new OrFilter(filters);
    return new HtmlFilterInputStream(in,
                                   encoding,
                                   HtmlNodeFilterTransform.exclude(orFilter));
}

}
