/*
 * $Id$
 */

/*

Copyright (c) 2000-2017 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.plugin.atypon.asco;

import java.io.InputStream;

import org.htmlparser.NodeFilter;
import org.htmlparser.filters.AndFilter;
import org.lockss.daemon.PluginException;
import org.lockss.filter.html.HtmlNodeFilters;
import org.lockss.plugin.*;
import org.lockss.plugin.atypon.BaseAtyponHtmlCrawlFilterFactory;

public class AscoHtmlCrawlFilterFactory extends BaseAtyponHtmlCrawlFilterFactory {
  static NodeFilter[] filters = new NodeFilter[] {

    //article page - center column with tools, most-read, etc
    HtmlNodeFilters.allExceptSubtree(
        HtmlNodeFilters.tagWithAttributeRegex("div", "class", "article-tools"),
          HtmlNodeFilters.tagWithAttributeRegex(
                 "a", "href", "/action/showCitFormats\\?")),
    //toc center column
    HtmlNodeFilters.tagWithAttributeRegex("div", "class", "toc-tools"),
    HtmlNodeFilters.tagWithAttribute("table", "class", "references"),
    
    // ASCO has a number of articles with in-line unlabelled links to other
    // volumes. The WORST is here:
    // http://ascopubs.org/doi/full/10.1200/JCO.2016.68.2146
    // JCO-34 alone (a huge volume of 7000+ articles) had 136
    // do not follow "doi/(abs|full)/" href when found within either full or abstract body
    // div class of hlFld-Fulltext or hlFld-Abstrct, 
    new AndFilter(
        HtmlNodeFilters.tagWithAttributeRegex("a", "href", "doi/(abs|full)/"),
        HtmlNodeFilters.ancestor(HtmlNodeFilters.tagWithAttributeRegex("div", "class", "^hlFld-(Fulltext|Abstract)"))),    

  };


  public InputStream createFilteredInputStream(ArchivalUnit au,
                                               InputStream in,
                                               String encoding)
      throws PluginException {
    return super.createFilteredInputStream(au, in, encoding, filters);
  }

}
