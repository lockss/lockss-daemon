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

package org.lockss.plugin.silverchair.oup;

import java.io.*;

import org.htmlparser.NodeFilter;
import org.lockss.daemon.PluginException;
import org.lockss.filter.html.*;
import org.lockss.plugin.*;
import org.lockss.plugin.silverchair.BaseScHtmlHashFilterFactory;
import org.lockss.util.Logger;

public class OupScHtmlHashFilterFactory extends BaseScHtmlHashFilterFactory {

  private static final Logger log = Logger.getLogger(OupScHtmlHashFilterFactory.class);
  
  @Override
  protected boolean doExtraSpecialFilter() {
    return false;
  }

  @Override
  public InputStream createFilteredInputStream(final ArchivalUnit au,
                                               InputStream in,
                                               String encoding)
      throws PluginException {
    
    NodeFilter[] includeFilters = new NodeFilter[] {
        HtmlNodeFilters.tagWithAttributeRegex("div", "class", "article-list-resources"),
        HtmlNodeFilters.tagWithAttributeRegex("div", "id", "resourceTypeList-OUP_Issue"),
        HtmlNodeFilters.tagWithAttributeRegex("div", "id", "ContentColumn"),
        HtmlNodeFilters.tagWithAttributeRegex("span", "class", "content-inner-wrap"),
        HtmlNodeFilters.tagWithAttributeRegex("div", "class", "article-body"),
        HtmlNodeFilters.tagWithAttributeRegex("div", "class", "OUP_Issues_List"),
        HtmlNodeFilters.tagWithAttributeRegex("div", "class", "IssuesAndVolumeListManifest"),
        HtmlNodeFilters.tagWithAttributeRegex("img", "class", "content-image"),
    };
    
    NodeFilter[] moreExcludeFilters = new NodeFilter[] {
        HtmlNodeFilters.tagWithAttributeRegex("div", "class", "navbar-search"),
    };
    
    return createFilteredInputStream(au, in, encoding, includeFilters, moreExcludeFilters);
  }
}
