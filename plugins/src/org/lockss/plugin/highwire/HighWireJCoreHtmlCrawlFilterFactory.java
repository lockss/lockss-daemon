/*

Copyright (c) 2000-2021, Board of Trustees of Leland Stanford Jr. University
All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are met:

1. Redistributions of source code must retain the above copyright notice,
this list of conditions and the following disclaimer.

2. Redistributions in binary form must reproduce the above copyright notice,
this list of conditions and the following disclaimer in the documentation
and/or other materials provided with the distribution.

3. Neither the name of the copyright holder nor the names of its contributors
may be used to endorse or promote products derived from this software without
specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
POSSIBILITY OF SUCH DAMAGE.

*/

package org.lockss.plugin.highwire;

import java.io.InputStream;
import java.util.Arrays;

import org.htmlparser.NodeFilter;
import org.htmlparser.filters.*;
import org.lockss.daemon.PluginException;
import org.lockss.filter.html.*;
import org.lockss.plugin.*;

public class HighWireJCoreHtmlCrawlFilterFactory implements FilterFactory {
  
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
    // Right side except pdf links & <div> supplementary material section (accardiol)
    //  <a href="/content/347/bmj.f5250.full.pdf" title="PDF" class="pdf-link"></a>
    //  <a href="/content/198/8/3045.full-text.pdf" target="_blank" class="link-icon"></a>
    //  add exception for supplementary-material in JACC
    HtmlNodeFilters.allExceptSubtree(
        HtmlNodeFilters.tagWithAttributeRegex("div", "class", "sidebar-right-wrapper"),
        new OrFilter( // HtmlCompoundTransform(
            HtmlNodeFilters.tagWithAttributeRegex("a", "class", "(pdf-link|link-icon)"),
            HtmlNodeFilters.tagWithAttributeRegex("div", "id", "supplementary-material"))),
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
    // never want these links, excluded lists was too long
    HtmlNodeFilters.tagWithAttributeRegex("a", "href", "(" +
        "[.](abstract|full.txt|powerpoint|ppt)$" +
        "|" +
        "\tab-(article-info|e-letters)$" +
        "|" +
        "^/(keyword/|lookup/(google-scholar|external-ref)|panels_ajax_tab|powerpoint/|search/author)" +
        ")"),
    
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
