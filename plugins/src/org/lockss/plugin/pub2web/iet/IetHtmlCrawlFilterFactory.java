/*
 * $Id:$
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

package org.lockss.plugin.pub2web.iet;

import java.io.InputStream;
import org.htmlparser.NodeFilter;
import org.htmlparser.filters.*;
import org.lockss.daemon.PluginException;
import org.lockss.filter.html.*;
import org.lockss.plugin.*;

public class IetHtmlCrawlFilterFactory implements FilterFactory {
  protected static NodeFilter[] filters = new NodeFilter[] {
    
    //footer
    HtmlNodeFilters.tagWithAttribute("div", "class", "footercontainer"),
    //banner
    HtmlNodeFilters.tagWithAttribute("div", "id", "header"),
    //Popular articles
    HtmlNodeFilters.tagWithAttribute("div", "id", "mostcited"),
    HtmlNodeFilters.tagWithAttribute("div", "id", "mostviewed"),
    HtmlNodeFilters.tagWithAttributeRegex("div", "id", "morelikethiscontentcontainer"),

    // prev article, toc, next article links - just in case of overcrawl
    HtmlNodeFilters.tagWithAttribute("div",  "class", "articlenav"),
    //must keep these in for books which don't have a top div for this navigator
    HtmlNodeFilters.tagWithAttribute("li",  "class", "previousLinkContainer"),
    HtmlNodeFilters.tagWithAttribute("li",  "class", "indexLinkContainer"),
    HtmlNodeFilters.tagWithAttribute("li",  "class", "nextLinkContainer"),

    // they don't seem internal, but just to be safe, don't crawl links within reference containers
    HtmlNodeFilters.tagWithAttribute("div",  "class", "refcontainer"),
    
    //from other children just in case
    HtmlNodeFilters.tagWithAttribute("div",  "class", "consanguinityContainer"),
    HtmlNodeFilters.tagWithAttribute("a", "target", "xrefwindow"),
    HtmlNodeFilters.tagWithAttribute("div", "class", "crossSelling"),
    
    //3/21/18 - html has changed - add to crawl filter
    // on TOC - most cited and most read
    HtmlNodeFilters.tagWithAttributeRegex("div", "class", "morelikethiscontentcontainer"),
    // on article page - tabs for references <div class="references hide tabbedsection">
    HtmlNodeFilters.tagWithAttributeRegex("div", "class", "references.*tabbedsection"),
    // on article page - tabs for keywords <div class="aboutthisarticle hide tabbedsection">
    HtmlNodeFilters.tagWithAttributeRegex("div", "class", "aboutthisarticle.*tabbedsection"),
    //on article page - tabs for cited by <div class="cite citations hidden-js-div tabbedsection tab-pane hide tabbedsection">
    HtmlNodeFilters.tagWithAttributeRegex("div", "class", "cite.*tabbedsection"),
    //on article page - tabs for related <div class="relatedContent hide tabbedsection">
    HtmlNodeFilters.tagWithAttributeRegex("div", "class", "relatedContent.*tabbedsection"),
    //on article right column - except for citation information download links
    HtmlNodeFilters.allExceptSubtree(
            HtmlNodeFilters.tagWithAttribute("div", "id", "sidebar_right"),
              HtmlNodeFilters.tagWithAttributeRegex(
                     "a", "href", "/content/journals/.*/cite/[a-z]+$")),

    
  };

  @Override
  public InputStream createFilteredInputStream(ArchivalUnit au,
      InputStream in, String encoding) throws PluginException{

    return new HtmlFilterInputStream(in, encoding,
        HtmlNodeFilterTransform.exclude(new OrFilter(filters)));
  }
  
}
