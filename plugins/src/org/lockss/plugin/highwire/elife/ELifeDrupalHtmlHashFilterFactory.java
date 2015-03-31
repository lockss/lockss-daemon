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

package org.lockss.plugin.highwire.elife;

import java.io.InputStream;

import org.htmlparser.NodeFilter;
import org.lockss.daemon.PluginException;
import org.lockss.filter.html.*;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.plugin.highwire.HighWireDrupalHtmlFilterFactory;
import org.lockss.util.Logger;

public class ELifeDrupalHtmlHashFilterFactory extends HighWireDrupalHtmlFilterFactory {
  
  private static final Logger log = Logger.getLogger(ELifeDrupalHtmlHashFilterFactory.class);
  
  protected static NodeFilter[] filters = new NodeFilter[] {
    // Do not hash responsive header (from crawl filter)
    HtmlNodeFilters.tagWithAttribute("div", "id", "region-responsive-header"),
    // this replaces references filter as references should not change
    HtmlNodeFilters.tagWithAttributeRegex("div", "class", "elife-reflink-links-wrapper"),
    // this was a source of over-crawl & can be simpler than (from crawl filter)
    HtmlNodeFilters.tagWithAttributeRegex("div", "class", "sidebar-wrapper"),
    // The next filter is not needed, we care about the correction for the hash
    // HtmlNodeFilters.tagWithAttributeRegex("div", "class", "elife-article-corrections"),
    
    // No relevant content in these headers
    HtmlNodeFilters.tagWithAttribute("div", "id", "zone-header-wrapper"),
    HtmlNodeFilters.tagWithAttribute("div", "class", "page_header"),
    HtmlNodeFilters.tagWithAttribute("ul", "class", "elife-article-categories"),
  };
  
  @Override
  public boolean doWSFiltering() {
    return true;
  }
  @Override
  public boolean doTagAttributeFiltering() {
    return false;
  }
  @Override
  public boolean doXformToText() {
    return true;
  }
  
  @Override
  public InputStream createFilteredInputStream(ArchivalUnit au,
                                               InputStream in,
                                               String encoding)
      throws PluginException {
    
    return super.createFilteredInputStream(au, in, encoding, filters);
  }
  
}
