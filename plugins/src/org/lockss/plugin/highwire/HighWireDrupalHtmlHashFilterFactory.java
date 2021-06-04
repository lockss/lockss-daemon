/*

Copyright (c) 2000-2021 Board of Trustees of Leland Stanford Jr. University,
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
import java.io.Reader;

import org.htmlparser.NodeFilter;
import org.htmlparser.filters.OrFilter;
import org.lockss.filter.FilterUtil;
import org.lockss.filter.WhiteSpaceFilter;
import org.lockss.filter.html.HtmlFilterInputStream;
import org.lockss.filter.html.HtmlNodeFilterTransform;
import org.lockss.filter.html.HtmlNodeFilters;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.plugin.FilterFactory;
import org.lockss.util.ReaderInputStream;

public class HighWireDrupalHtmlHashFilterFactory implements FilterFactory {

  public InputStream createFilteredInputStream(ArchivalUnit au,
                                               InputStream in,
                                               String encoding) {

    NodeFilter[] filters = new NodeFilter[] {
        HtmlNodeFilters.tag("head"),
        HtmlNodeFilters.tag("header"),
        HtmlNodeFilters.tag("footer"),
        // Contains variable ad-generating code
        HtmlNodeFilters.tag("script"),
        // Contains variable ad-generating code
        HtmlNodeFilters.tag("noscript"),
        // Typically contains ads
        HtmlNodeFilters.tag("object"),
        // Typically contains ads
        HtmlNodeFilters.tag("iframe"),

        // top bar ad
        HtmlNodeFilters.tagWithAttribute("div", "id", "region-ad-top"),
        HtmlNodeFilters.tagWithAttribute("div", "id", "zone-advertising-top-wrapper"),

        // sidebar ads - DoubleClick for Publishers, Mid-page Unit
        HtmlNodeFilters.tagWithAttributeRegex("div", "class", "dfp-pane"),
          // pane-advert-ros-rhs-bottom, pane-advert-mpu etc.
        HtmlNodeFilters.tagWithAttributeRegex("div", "class", "pane-advert"),

        // sidebar metrics, social links and widgets
        HtmlNodeFilters.tagWithAttributeRegex("div", "class", "pane-highwire-altmetrics"),
        HtmlNodeFilters.tagWithAttributeRegex("div", "class", "pane-jnl-ada-share"),
        HtmlNodeFilters.tagWithAttributeRegex("div", "class", "highwire_clipboard_form_ajax_shareit"),
        HtmlNodeFilters.tagWithAttributeRegex("div", "class", "pane-service-links"),
        HtmlNodeFilters.tagWithAttributeRegex("div", "class", "pane-highwire-article-trendmd"),
        HtmlNodeFilters.tagWithAttribute("div", "id", "highwire_article_accordion_container"),

        // check for updates widget after author info,
        HtmlNodeFilters.tagWithAttributeRegex("div", "class", "pane-highwire-article-crossmark"),

        // cookies query popup
        HtmlNodeFilters.tagWithAttribute("div", "id", "sliding-popup"),

    };

    // First filter with HtmlParser
    OrFilter orFilter = new OrFilter(filters);
    InputStream filtered =
        new HtmlFilterInputStream(in,
            encoding,
            HtmlNodeFilterTransform.exclude(orFilter));
    // Then convert to reader and filter white space
    Reader rdr = FilterUtil.getReader(filtered, encoding);
    return new ReaderInputStream(new WhiteSpaceFilter(rdr));
  }

}

