/*
 * $Id$
 */

/*

Copyright (c) 2017 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.plugin.highwire;

import java.io.InputStream;
import java.util.Arrays;

import org.htmlparser.NodeFilter;
import org.htmlparser.filters.*;
import org.lockss.daemon.PluginException;
import org.lockss.filter.html.*;
import org.lockss.plugin.*;

public class HighWireDrupalHtmlCrawlFilterFactory implements FilterFactory {
  
  protected static NodeFilter[] baseHWDrupalFilters = new NodeFilter[] {
    // Do not crawl header or footer for links 
    HtmlNodeFilters.tag("header"),
    HtmlNodeFilters.tag("footer"),
    // Do not crawl for links from aside in BMJ, etc
    HtmlNodeFilters.tag("aside"),
    HtmlNodeFilters.tag("script"),
    // filter nav tag; found outside of header tag in http://elements.geoscienceworld.org/content/8/1/76.long
    HtmlNodeFilters.tag("nav"),
    // Do not crawl reference section, right sidebar for links; common with APS & OUP
    HtmlNodeFilters.tagWithAttribute("div", "class", "section ref-list"),
    // Title bar on toc with link to current issue
    HtmlNodeFilters.tagWithAttributeRegex("div", "class", "title-menu-and-about"),
    // Right side and all other links to prev & next
    HtmlNodeFilters.tagWithAttributeRegex("div", "class", "sidebar-right-wrapper"),
    HtmlNodeFilters.tagWithAttributeRegex("div", "class", "pager"),
    HtmlNodeFilters.tagWithAttributeRegex("span", "class", "prev"),
    HtmlNodeFilters.tagWithAttributeRegex("span", "class", "next"),
    // messages now contain correction lists
    // do not filter issue-toc-section issue-toc-section-messages-from-munich
    HtmlNodeFilters.tagWithAttributeRegex("div", "class", "messages(?!-from-)"),
    // do NOT crawl breadcrumbs
    HtmlNodeFilters.tagWithAttributeRegex("div", "id", "breadcrumb"),
    HtmlNodeFilters.tagWithAttributeRegex("div", "class", "breadcrumb"),
    // Do not crawl issue links (http://pediatrics.aappublications.org/content/137/2/e20154272 link to off-AU article with issue link
    HtmlNodeFilters.tagWithAttributeRegex("a", "class", "issue-link"),
    // Commentary links found inside
    HtmlNodeFilters.tagWithAttributeRegex("div", "class", "relationship-manager"),
    // Do not crawl any links with by/year/ or by/volume
    HtmlNodeFilters.tagWithAttributeRegex("a", "href", "by/(year|volume)"),
  };
  
  @Override
  public InputStream createFilteredInputStream(ArchivalUnit au,
                                               InputStream in,
                                               String encoding)
      throws PluginException {
     
    return doFiltering(in, encoding, null);
  }
  
  public InputStream createFilteredInputStream(ArchivalUnit au,
                                               InputStream in,
                                               String encoding,
                                               NodeFilter[] moreNodes)
      throws PluginException {
    
    return doFiltering(in, encoding, moreNodes);
  }
  
  /* the shared portion of the filtering
   * pick up the extra nodes from the child if there are any
   */
  protected InputStream doFiltering(InputStream in, String encoding, NodeFilter[] moreNodes) {
    NodeFilter[] filters = baseHWDrupalFilters;
    if (moreNodes != null) {
      filters = addTo(moreNodes);
    }
    
    return new HtmlFilterInputStream(in, encoding,
        HtmlNodeFilterTransform.exclude(new OrFilter(filters)));
  }
  /** Create an array of NodeFilters that combines the baseHWDrupalFilters with
   *  the given array
   *  @param nodes The array of NodeFilters to add
   */
  protected NodeFilter[] addTo(NodeFilter[] nodes) {
    NodeFilter[] result  = Arrays.copyOf(baseHWDrupalFilters, baseHWDrupalFilters.length + nodes.length);
    System.arraycopy(nodes, 0, result, baseHWDrupalFilters.length, nodes.length);
    return result;
  }
  
}
