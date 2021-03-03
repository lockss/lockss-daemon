/*
 * $Id: $
 */

/*

Copyright (c) 2000-2016 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.plugin.springer.link;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.util.List;
import java.util.regex.Pattern;

import org.htmlparser.Node;
import org.htmlparser.NodeFilter;
import org.htmlparser.Tag;
import org.htmlparser.filters.*;
import org.htmlparser.tags.BodyTag;
import org.htmlparser.tags.CompositeTag;
import org.htmlparser.tags.Div;
import org.htmlparser.util.NodeList;
import org.htmlparser.util.ParserException;
import org.htmlparser.visitors.NodeVisitor;
import org.lockss.filter.FilterUtil;
import org.lockss.filter.HtmlTagFilter;
import org.lockss.filter.StringFilter;
import org.lockss.filter.WhiteSpaceFilter;
import org.lockss.filter.html.*;
import org.lockss.filter.html.HtmlTags.Section;
import org.lockss.plugin.*;
import org.lockss.util.ListUtil;
import org.lockss.util.ReaderInputStream;
import java.io.*;
import java.util.Arrays;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.input.CountingInputStream;
import org.htmlparser.NodeFilter;
import org.htmlparser.filters.OrFilter;
import org.lockss.daemon.PluginException;
import org.lockss.filter.*;
import org.lockss.filter.HtmlTagFilter.TagPair;
import org.lockss.filter.html.*;
import org.lockss.plugin.*;
import org.lockss.util.Logger;
import org.lockss.util.ReaderInputStream;

public class SpringerLinkHtmlHashFilterFactory implements FilterFactory {

  public InputStream createFilteredInputStream(ArchivalUnit au,
      InputStream in, String encoding) {

    HtmlFilterInputStream filteredStream = new HtmlFilterInputStream(in, encoding,
      new HtmlCompoundTransform(
      // Remove these parts first
      HtmlNodeFilterTransform.exclude(new OrFilter(new NodeFilter[] {
        // entire footer  (incl. ip address/logged in notifier)
        HtmlNodeFilters.tag("footer"),
        // article visits and other changeables
        HtmlNodeFilters.tagWithAttribute("div", "data-test", "article-metrics"),
        HtmlNodeFilters.tagWithAttribute("div", "id", "altmetric-container"),
        //adds on the side and top
        HtmlNodeFilters.tagWithAttributeRegex("aside", "class", "c-ad"),
        HtmlNodeFilters.tagWithAttribute("div", "class", "skyscraper-ad"),
        HtmlNodeFilters.tagWithAttribute("div", "class", "banner-advert"),
        HtmlNodeFilters.tagWithAttribute("div", "id", "doubleclick-ad"),
      })),
      // now keep these
      HtmlNodeFilterTransform.include(new OrFilter(new NodeFilter[] {
          HtmlNodeFilters.tag("p"),
          HtmlNodeFilters.tag("h1")
      }))
      ));
    Reader filteredReader = FilterUtil.getReader(filteredStream, encoding);
//    Reader noTagFilter = new HtmlTagFilter(new StringFilter(filteredReader, "<", " <"), new TagPair("<", ">"));

      // Remove white space
    return new ReaderInputStream(new WhiteSpaceFilter(filteredReader));
  }
  
}
