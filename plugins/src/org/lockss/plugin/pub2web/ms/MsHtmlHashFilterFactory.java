/* $Id:$
 
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

package org.lockss.plugin.pub2web.ms;

import java.io.InputStream;
import org.htmlparser.NodeFilter;
import org.htmlparser.filters.*;
import org.lockss.filter.html.*;
import org.lockss.plugin.*;

public class MsHtmlHashFilterFactory implements FilterFactory {
  

  protected static NodeFilter[] infilters = new NodeFilter[] {
    // You need the main-content-container to get the manifest page listing
    // this is also the main container for the article landing page
    HtmlNodeFilters.tagWithAttributeRegex("main", "class", "main-content-container"),
    // The renderList chunks that populate the TOC
    HtmlNodeFilters.tagWithAttributeRegex("div", "class",  "articleListContainer"),
    // for the full-text html crawler version, you need the various article chuns
    HtmlNodeFilters.tagWithAttributeRegex("div", "id", "article-level-0"),
    // for the figures and tables landing pages - it's a list-group of these
    HtmlNodeFilters.tagWithAttributeRegex("div", "class", "singleFigureContainer"),
    //supp data landing page
    HtmlNodeFilters.tagWithAttribute("div", "id", "SuppDataIndexList"),
    
    //do not need the export-list citation download links for hash filter
  };
  protected static NodeFilter[] xfilters = new NodeFilter[] {
    
    HtmlNodeFilters.tag("nav"),
    HtmlNodeFilters.tag("script"),
    HtmlNodeFilters.tag("noscript"),
    
    // We have the main container, now start taking bits of that out
    HtmlNodeFilters.tagWithAttribute("ol",  "class", "breadcrumb"),
    HtmlNodeFilters.tagWithAttributeRegex("a",  "class", "banner-container journal-banner"),
    HtmlNodeFilters.tagWithAttribute("nav",  "class", "pillscontainer"),
    //the "Cited by" tab lists a number that can change
    HtmlNodeFilters.tagWithAttribute("li",  "id", "cite"),
    
    //remove the TOC navigation links except the full TOC pdf

    //remove article landing page navigation links
    HtmlNodeFilters.tagWithAttribute("li",  "class", "previousLinkContainer"),
    HtmlNodeFilters.tagWithAttribute("li",  "class", "indexLinkContainer"),
    HtmlNodeFilters.tagWithAttribute("li",  "class", "nextLinkContainer"),

    //TODO - these came from ASM, must look at MS more closely
    HtmlNodeFilters.tagWithAttributeRegex("div", "class", "(citations|references|relatedContent)"),

    HtmlNodeFilters.tagWithAttribute("div", "class", "hiddenjsdiv metricsEndDate"),
    HtmlNodeFilters.tagWithAttributeRegex("div",  "class",  "^metrics "),
    HtmlNodeFilters.tagWithAttribute("input",  "name", "copyright"),
    HtmlNodeFilters.tagWithAttribute("div", "class", "crossSelling"),
    HtmlNodeFilters.tagWithAttribute("div", "id", "related"),
    //every now and then the order of the xml, html, pdf links are different - dynamically generated
    HtmlNodeFilters.tagWithAttribute("div",  "class", "contentTypeOptions"),
    //every now and then the server fails to serve the next/prev link...what? - on journals
    HtmlNodeFilters.tagWithAttribute("div",  "class", "articlenav"),
    HtmlNodeFilters.tagWithAttribute("div",  "id", "relatedcontent"), //tab contents, not the header
    HtmlNodeFilters.tagWithAttribute("div",  "id", "otherJournals"), // tab contents
    
    // <span class="access_icon_s keyicon accesskey-icon"
    HtmlNodeFilters.tagWithAttributeRegex("span", "class", "access_icon"),

    // filter out the following https://www.microbiologyresearch.org/content/journal/jmm/10.1099/jmm.0.000032     
    HtmlNodeFilters.tagWithAttributeRegex("div", "class", "mostcitedcontainer"),
    HtmlNodeFilters.tagWithAttributeRegex("div", "class", "mostreadcontainer"),
    HtmlNodeFilters.tagWithAttributeRegex("div", "class", "mostviewedloading"),
    HtmlNodeFilters.tagWithAttributeRegex("div", "class", "copyright-info"),
    HtmlNodeFilters.tagWithAttributeRegex("div", "id", "metrics_content"),
    HtmlNodeFilters.tagWithAttributeRegex("form", "id", "pptDwnld"),
    HtmlNodeFilters.tagWithAttributeRegex("div", "class", "itemFullTextLoading"),
    HtmlNodeFilters.tagWithAttributeRegex("div", "class", "hiddenjsdiv"),
    HtmlNodeFilters.tagWithAttributeRegex("form", "id","dataandmedia"),
    HtmlNodeFilters.tagWithAttributeRegex("div", "class","itemDataMediaLoading"),
    HtmlNodeFilters.tagWithAttributeRegex("div", "class", "journal-volume-issue-container"),
    HtmlNodeFilters.tagWithAttributeRegex("div", "class", "article-access-icon-and-access"),
    HtmlNodeFilters.tagWithAttributeRegex("div", "class", "hidden-js-div"),
          
  };

  @Override
  public InputStream createFilteredInputStream(ArchivalUnit au,
      InputStream in, String encoding) {
    
    return new HtmlFilterInputStream(in,
        encoding,
        new HtmlCompoundTransform(
            HtmlNodeFilterTransform.include(new OrFilter(infilters)),
            HtmlNodeFilterTransform.exclude(new OrFilter(xfilters))
            ));
  }
    
}
