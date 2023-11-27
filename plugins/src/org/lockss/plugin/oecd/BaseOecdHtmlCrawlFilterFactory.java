/*

Copyright (c) 2000-2023, Board of Trustees of Leland Stanford Jr. University

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

package org.lockss.plugin.oecd;

import org.htmlparser.NodeFilter;
import org.htmlparser.filters.OrFilter;
import org.lockss.daemon.PluginException;
import org.lockss.filter.html.HtmlFilterInputStream;
import org.lockss.filter.html.HtmlNodeFilterTransform;
import org.lockss.filter.html.HtmlNodeFilters;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.plugin.FilterFactory;
import org.lockss.util.Logger;

import java.io.InputStream;
import java.util.Arrays;

public class BaseOecdHtmlCrawlFilterFactory implements FilterFactory {
  protected static Logger log = Logger.getLogger(BaseOecdHtmlCrawlFilterFactory.class);

  static NodeFilter[] baseOecdFilters = new NodeFilter[] {
      HtmlNodeFilters.tag("header"),
      HtmlNodeFilters.tag("footer"),
      HtmlNodeFilters.tagWithAttribute("ol", "class", "breadcrumb"),
      // link to the entire journal in other languges
      HtmlNodeFilters.tagWithAttribute("p", "class", "language"),
      HtmlNodeFilters.tagWithAttributeRegex("li", "class", "furtherReading"),
      HtmlNodeFilters.tagWithAttributeRegex("li", "class", "relatedtitle"),
      HtmlNodeFilters.tagWithAttributeRegex("li", "class", "relatedIndicatortitle"),
      HtmlNodeFilters.tagWithAttributeRegex("li", "class", "relatedDatabasetitle"),
      // sidebar links
      HtmlNodeFilters.tagWithAttributeRegex("ul", "class", "actions vertical"),
      HtmlNodeFilters.tagWithAttribute("div", "class", "related-content-margin"),
      // other volumes in the journal
      HtmlNodeFilters.tagWithAttribute("ul", "class", "volumes-list"),
      // links to other journals
      HtmlNodeFilters.tagWithAttributeRegex("div", "class", "replaced-content"),
      // prev/next buttons on article pages
      HtmlNodeFilters.tagWithAttribute("div", "class", "nav-item-prev"),
      HtmlNodeFilters.tagWithAttribute("div", "class", "nav-item-next"),
      // get rid of all the links in the landing page except the csv file(s)
      HtmlNodeFilters.allExceptSubtree(
          HtmlNodeFilters.tagWithAttributeRegex("div", "class", "section-title"),
          // per OECD we are only to collect the pdfs and epubs
          HtmlNodeFilters.tagWithAttributeRegex("a", "class", "action-(epub|pdf)")
      )
  };

  /** Create an array of NodeFilters that combines the baseOecdFilters with
   *  the given array
   *  @param nodes The array of NodeFilters to add
   */
  private NodeFilter[] addTo(NodeFilter[] nodes) {
    NodeFilter[] result  = Arrays.copyOf(baseOecdFilters, baseOecdFilters.length + nodes.length);
    System.arraycopy(nodes, 0, result, baseOecdFilters.length, nodes.length);
    return result;
  }
  /** Create a FilteredInputStream that excludes the the baseOecdFilters and
   * moreNodes
   * @param au  The archival unit
   * @param in  Incoming input stream
   * @param encoding  The encoding
   * @param moreNodes An array of NodeFilters to be excluded with baseOecdFilters
   */
  public InputStream createFilteredInputStream(ArchivalUnit au,
                                               InputStream in,
                                               String encoding,
                                               NodeFilter[] moreNodes)
      throws PluginException {
    NodeFilter[] bothFilters = addTo(moreNodes);
    return new HtmlFilterInputStream(in, encoding,
        HtmlNodeFilterTransform.exclude(new OrFilter(bothFilters)));
  }

  public InputStream createFilteredInputStream(ArchivalUnit au,
                                               InputStream in, String encoding) throws PluginException {
    return new HtmlFilterInputStream(in,
        encoding,
        HtmlNodeFilterTransform.exclude(new OrFilter(
            baseOecdFilters
        )));
  }

}