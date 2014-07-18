/*
 * $Id: ManeyAtyponHtmlCrawlFilterFactory.java,v 1.2.2.2 2014-07-18 15:54:41 wkwilson Exp $
 */

/*

Copyright (c) 2000-2014 Board of Trustees of Leland Stanford Jr. University,
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
import java.util.regex.Pattern;

import org.htmlparser.Node;
import org.htmlparser.NodeFilter;
import org.htmlparser.filters.*;
import org.htmlparser.tags.CompositeTag;
import org.htmlparser.tags.Div;
import org.htmlparser.tags.LinkTag;
import org.lockss.daemon.PluginException;
import org.lockss.filter.html.*;
import org.lockss.plugin.*;
import org.lockss.plugin.atypon.BaseAtyponHtmlCrawlFilterFactory;

public class ManeyAtyponHtmlCrawlFilterFactory extends BaseAtyponHtmlCrawlFilterFactory {
    
  NodeFilter[] filters = new NodeFilter[] {
      // BaseAtypon takes care of citedBySection
      // don't go to references - I don't think they link direct, but be safe
      HtmlNodeFilters.tagWithAttribute("table", "class", "references"),
      // Corrigendum/Errata/OriginalArticle  -- article page
      // use regex because class name has other stuff in it
      HtmlNodeFilters.tagWithAttributeRegex("div", "class", "correctionLink"),
      // sidebar has hard to isolate "most read", "most cited" "editors choice"
      HtmlNodeFilters.tagWithAttributeRegex("section", "class", "literatumMostReadWidget"),
      HtmlNodeFilters.tagWithAttributeRegex("section", "class", "literatumMostCitedWidget"),
      HtmlNodeFilters.tagWithAttributeRegex("section", "class", "publicationListWidget"),

  };
  @Override
  public InputStream createFilteredInputStream(ArchivalUnit au,
      InputStream in, String encoding) throws PluginException{ 
    return super.createFilteredInputStream(au, in, encoding, filters);
  }

}
