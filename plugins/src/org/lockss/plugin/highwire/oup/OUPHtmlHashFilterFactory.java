/*
 * $Id: OUPHtmlHashFilterFactory.java,v 1.2 2015-02-04 07:14:09 etenbrink Exp $
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

package org.lockss.plugin.highwire.oup;

import java.io.InputStream;

import org.htmlparser.NodeFilter;
import org.lockss.daemon.PluginException;
import org.lockss.filter.html.*;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.plugin.highwire.HighWireDrupalHtmlFilterFactory;
import org.lockss.util.Logger;

public class OUPHtmlHashFilterFactory extends HighWireDrupalHtmlFilterFactory {
  
  private static final Logger log = Logger.getLogger(OUPHtmlHashFilterFactory.class);
  
  @Override
  public boolean doWSFiltering() {
    return false;
  }
  @Override
  public boolean doTagAttributeFiltering() {
    return true;
  }
  @Override
  public boolean doXformToText() {
    return false;
  }
  
  @Override
  public InputStream createFilteredInputStream(ArchivalUnit au,
                                               InputStream in,
                                               String encoding)
      throws PluginException {
    NodeFilter[] filters = new NodeFilter[] {
        // HtmlNodeFilters.tagWithAttributeRegex("a", "class", "hw-link"),
        // right sidebar 
        HtmlNodeFilters.tagWithAttributeRegex("div", "class", "sidebar-right-wrapper"),
        // content-header from QJM
        HtmlNodeFilters.tagWithAttributeRegex("div", "class", "content-header"),
        // <div class="panel-pane pane-panels-mini pane-oup-explore-related-articles"
        HtmlNodeFilters.tagWithAttributeRegex("div", "class", "(citing|related)-articles?"),
        // don't remove any div tags with login, as they should not happen and we don't want to hide
        // HtmlNodeFilters.tagWithAttributeRegex("div", "class", "-login"),
    };
    
    InputStream filtered = super.createFilteredInputStream(au, in, encoding, filters);
    
    return filtered;
  }
}
