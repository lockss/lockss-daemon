/*
 * $Id: BMCPluginHtmlFilterFactory.java,v 1.1.4.2 2012-06-20 00:02:58 nchondros Exp $
 */

/*

 Copyright (c) 2000-2008 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.plugin.bmc;

import java.io.*;
import java.util.List;

import org.htmlparser.NodeFilter;
import org.htmlparser.filters.OrFilter;
import org.htmlparser.filters.TagNameFilter;
import org.lockss.daemon.PluginException;
import org.lockss.filter.*;
import org.lockss.filter.html.HtmlFilterInputStream;
import org.lockss.filter.html.HtmlNodeFilterTransform;
import org.lockss.filter.html.HtmlNodeFilters;
import org.lockss.plugin.*;
import org.lockss.util.*;

public class BMCPluginHtmlFilterFactory implements FilterFactory {

  public InputStream createFilteredInputStream(ArchivalUnit au, InputStream in,
      String encoding) throws PluginException {
    NodeFilter[] filters = new NodeFilter[] {
        // Contains variable code
        new TagNameFilter("script"),
        // Contains variable alternatives to the code
        new TagNameFilter("noscript"),
        // Contains ads
        new TagNameFilter("iframe"),
        // Contains ads
        new TagNameFilter("object"),
        // Contains one-time names inside the page
        HtmlNodeFilters.tagWithAttribute("a", "name"),
        // Links to one-time names inside the page
        HtmlNodeFilters.tagWithAttributeRegex("a", "href", "^#"),
        // Contains the menu  <ul class="primary-nav">
        HtmlNodeFilters.tagWithAttribute("ul", "class", "primary-nav"),
       // Contains advertising
       HtmlNodeFilters.tagWithAttribute("dl", "class", "google-ad"),
        // Contains advertising<dl class=\"google-ad wide\"", "</dl>"
        HtmlNodeFilters.tagWithAttribute("dl", "class", "google-ad wide"),
        //Contains the terms and conditions,copyright year & links to springer
        HtmlNodeFilters.tagWithAttribute("div", "id", "footer"),
        // Institution-dependent link resolvers
        HtmlNodeFilters.tagWithAttributeRegex("a", "href", "^/sfx_links\\.asp"), };
    return new HtmlFilterInputStream(in, encoding,
        HtmlNodeFilterTransform.exclude(new OrFilter(filters)));
  }

}
