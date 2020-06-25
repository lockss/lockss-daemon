/*
 * $Id$
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

package org.lockss.plugin.americansocietyofconsultantpharmacists;

import org.htmlparser.Attribute;
import org.htmlparser.Node;
import org.htmlparser.NodeFilter;
import org.htmlparser.Tag;
import org.htmlparser.filters.AndFilter;
import org.htmlparser.filters.HasAttributeFilter;
import org.htmlparser.filters.OrFilter;
import org.htmlparser.filters.TagNameFilter;
import org.htmlparser.nodes.TextNode;
import org.htmlparser.tags.CompositeTag;
import org.htmlparser.tags.LinkTag;
import org.htmlparser.util.NodeList;
import org.lockss.daemon.PluginException;
import org.lockss.filter.FilterUtil;
import org.lockss.filter.HtmlTagFilter;
import org.lockss.filter.HtmlTagFilter.TagPair;
import org.lockss.filter.WhiteSpaceFilter;
import org.lockss.filter.html.*;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.plugin.FilterFactory;
import org.lockss.util.ListUtil;
import org.lockss.util.Logger;
import org.lockss.util.ReaderInputStream;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.util.Vector;

public class AmericanSocietyOfConsultantPharmacistsHtmlFilterFactory implements FilterFactory {

  public InputStream createFilteredInputStream(ArchivalUnit au,
                                               InputStream in,
                                               String encoding) {
    NodeFilter[] filters = new NodeFilter[] {
            //filter out script
            new TagNameFilter("noscript"),
            new TagNameFilter("script"),
            new TagNameFilter("style"),
            new TagNameFilter("head"),
            new TagNameFilter("header"),
            new TagNameFilter("footer"),

            // https://www.ingentaconnect.com/contentone/ascp/tscp/2020/00000035/00000004/art00002
            HtmlNodeFilters.tagWithAttribute("div", "id", "llb"),
            HtmlNodeFilters.tagWithAttribute("div", "class", "device-tab-mobile-buttons-container"),
            HtmlNodeFilters.tagWithAttribute("div", "id", "secure"),
            HtmlNodeFilters.tagWithAttribute("div", "class", "breadcrumb"),
            HtmlNodeFilters.tagWithAttribute("div", "class", "article-pager"),
            HtmlNodeFilters.tagWithAttribute("div", "id", "sign-in-container"),
            HtmlNodeFilters.tagWithAttribute("div", "id", "tools"),
            HtmlNodeFilters.tagWithAttribute("div", "id", "google_translate_element"),
            HtmlNodeFilters.tagWithAttribute("div", "class", "shareContent"),
            HtmlNodeFilters.tagWithAttribute("div", "class", "icon-key"),
            HtmlNodeFilters.tagWithAttribute("div", "class", "advertisingbanner"),
            HtmlNodeFilters.tagWithAttribute("div", "id", "Info"),
            HtmlNodeFilters.tagWithAttribute("ul", "class", "nav-tabs"),
            HtmlNodeFilters.tagWithAttribute("div", "class", "trendmd-widget"),
            HtmlNodeFilters.tagWithAttribute("div", "class", "footerContainer"),
            HtmlNodeFilters.tagWithAttribute("div", "class", "cornerPolicyTab"),
            HtmlNodeFilters.tagWithAttribute("div", "class", "mainCookiesPopUp"),

    };
    InputStream filteredStream = new HtmlFilterInputStream(in, encoding,
            HtmlNodeFilterTransform.exclude(new OrFilter(filters)));
    Reader httpFilter = FilterUtil.getReader(filteredStream, encoding);
    return new ReaderInputStream(httpFilter);
  }

}
