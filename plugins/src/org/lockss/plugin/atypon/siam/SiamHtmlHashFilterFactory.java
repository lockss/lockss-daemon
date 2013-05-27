/*
 * $Id: SiamHtmlHashFilterFactory.java,v 1.2 2013-05-27 18:23:52 alexandraohlson Exp $
 */

/*

Copyright (c) 2000-2013 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.plugin.atypon.siam;

import java.io.InputStream;

import org.htmlparser.Node;
import org.htmlparser.NodeFilter;
import org.htmlparser.filters.OrFilter;
import org.htmlparser.filters.TagNameFilter;
import org.htmlparser.tags.CompositeTag;
import org.htmlparser.tags.Div;
import org.htmlparser.util.SimpleNodeIterator;
import org.lockss.daemon.PluginException;
import org.lockss.filter.html.HtmlFilterInputStream;
import org.lockss.filter.html.HtmlNodeFilterTransform;
import org.lockss.filter.html.HtmlNodeFilters;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.plugin.FilterFactory;

public class SiamHtmlHashFilterFactory implements FilterFactory {

  @Override
  public InputStream createFilteredInputStream(ArchivalUnit au,
      InputStream in,
      String encoding)
          throws PluginException {
    NodeFilter[] filters = new NodeFilter[] {
        // Variable identifiers - institution doing the crawl  - including genSfxLinks() which is the institution button
        new TagNameFilter("script"),
        // contains the institution banner on both TOC and article pages
        HtmlNodeFilters.tagWithAttribute("div", "class", "institutionBanner"),
        // left panel in article contains "browse volumes" which change over time
        HtmlNodeFilters.tagWithAttribute("div", "id", "volumelisting"),
        // Contains the changeable list of citations
        HtmlNodeFilters.tagWithAttribute("div", "class", "citedBySection"),
        // May contain " | Cited x# times"; also contains journal/page for this article, but should be okay to hash out
        HtmlNodeFilters.tagWithAttribute("div", "class", "citation tocCitation"),
        //Session History variable
        HtmlNodeFilters.tagWithAttribute("div", "id", "sessionHistory"),
        // Contains copyright year
        HtmlNodeFilters.tagWithAttribute("div", "id", "footer"),
        // Contains argument in rss href that is time dependent, but none of this is needed for hash
        HtmlNodeFilters.tagWithAttribute("div", "id", "prevNextNav"),        
        new NodeFilter() {
          @Override public boolean accept(Node node) {
            // on a TOC, side panel items are not obviously marked as such
            // Looking for <div class="box collapsible open"...> with first child <div class="header publicationSideBar"...>
            if (!(node instanceof Div)) return false;
            String divClass = ((CompositeTag)node).getAttribute("class");
            if ( (divClass==null) || !(divClass.contains("box collapsible")) ) return false;
            for (SimpleNodeIterator iter = ((CompositeTag)node).elements() ; iter.hasMoreNodes() ; ) {
              Node n = iter.nextNode();
              if (!(n instanceof Div)) { continue; }
              String childClass = ((CompositeTag)n).getAttribute("class");
              if ( (childClass !=null) && (childClass.contains("publicationSideBar")) ) {
                return true;
              }
            }
            return false;
          }
        }
    };
    return new HtmlFilterInputStream(in,
        encoding,
        HtmlNodeFilterTransform.exclude(new OrFilter(filters)));

  }
}

