/*
 * $Id: ASCEHtmlCrawlFilterFactory.java,v 1.2 2013-08-06 21:09:32 aishizaki Exp $
 */

/*

Copyright (c) 2000-2012 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.plugin.atypon.americansocietyofcivilengineers;

import java.io.InputStream;

import org.htmlparser.NodeFilter;
import org.htmlparser.filters.*;
import org.lockss.daemon.PluginException;
import org.lockss.filter.html.*;
import org.lockss.plugin.*;
import org.lockss.plugin.atypon.BaseAtyponHtmlCrawlFilterFactory;


public class ASCEHtmlCrawlFilterFactory extends BaseAtyponHtmlCrawlFilterFactory {
  static NodeFilter[] filters = new NodeFilter[] {
    // left column section history
    // <div class="sessionViewed">
    // http://ascelibrary.org/toc/jaeied/18/4
    // necessary since some urls are opaque (not including year and/or volume)
    // so can't differentiate urls from different AUs.
    // http://ascelibrary.org/doi/full/10.1061/(ASCE)CO.1943-7862.0000372
    HtmlNodeFilters.tagWithAttribute("div", "class", "sessionViewed"),
    // <div class="citedBySection"></div>
    //HtmlNodeFilters.tagWithAttribute("div", "class", "citedBySection"),
    // <div id="relatedContent"
    HtmlNodeFilters.tagWithAttribute("div", "id", "relatedContent"),
    // right near top - links to previous or next issue
    // <div id="prevNextNav">
    HtmlNodeFilters.tagWithAttribute("div", "id", "prevNextNav"),
  };

  @Override
  public InputStream createFilteredInputStream(ArchivalUnit au,
      InputStream in,
      String encoding)
  throws PluginException{
    return super.createFilteredInputStream(au, in, encoding, filters);
  }
}
