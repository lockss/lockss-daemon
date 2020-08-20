/*
 * $Id: OaiPmhHtmlFilterFactory.java,v 1.1.2.1 2014/05/05 17:32:30 wkwilson Exp $
 */

/*

Copyright (c) 2000-2012 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.plugin.spandidos;

import java.io.Reader;
import org.htmlparser.NodeFilter;
import org.htmlparser.filters.OrFilter;
import org.htmlparser.filters.TagNameFilter;
import org.lockss.filter.html.HtmlFilterInputStream;
import org.lockss.filter.html.HtmlNodeFilterTransform;
import org.lockss.filter.html.HtmlNodeFilters;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.plugin.FilterFactory;
import org.lockss.util.ReaderInputStream;
import java.io.InputStream;
import org.lockss.filter.FilterUtil;
import org.lockss.filter.WhiteSpaceFilter;
import org.lockss.filter.html.*;

public class SpandidosHtmlFilterFactory implements FilterFactory {

  public InputStream createFilteredInputStream(ArchivalUnit au,
                                               InputStream in,
                                               String encoding) {
    NodeFilter[] includeNodes = new NodeFilter[] {
            HtmlNodeFilters.tagWithAttributeRegex("div", "class", "content_main_home"),
            HtmlNodeFilters.tagWithAttributeRegex("div", "class", "content_main"),

    };

    NodeFilter[] excludeNodes = new NodeFilter[] {

      // filter out comments
      HtmlNodeFilters.comment(),
      // filter out script
      new TagNameFilter("script"),
      // header & footer

      //https://www.spandidos-publications.com/
      HtmlNodeFilters.tagWithAttributeRegex("div", "class", "content_journalgrid"),
      HtmlNodeFilters.tagWithAttributeRegex("div", "class", "fixed_width_twitter"),
      HtmlNodeFilters.tagWithAttributeRegex("div", "id", "widget"),
      HtmlNodeFilters.tagWithAttributeRegex("div", "class", "content_heading"),
      HtmlNodeFilters.tagWithAttributeRegex("div", "id", "navbar"),

      //https://www.spandidos-publications.com/ijo/archive
      HtmlNodeFilters.tagWithAttributeRegex("div", "class", "latestIssue"),
      HtmlNodeFilters.tagWithAttributeRegex("div", "id", "archive"),
      HtmlNodeFilters.tagWithAttributeRegex("p", "class", "pushdown2"),

      //https://www.spandidos-publications.com/etm/6/6/1365?text=abstract
      HtmlNodeFilters.tagWithAttributeRegex("table", "class", "ref-list"),
      HtmlNodeFilters.tagWithAttributeRegex("h1", "id", "titleId"),
      HtmlNodeFilters.tagWithAttributeRegex("ul", "class", "article_details"),
      HtmlNodeFilters.tagWithAttributeRegex("a", "class", "moreLikeThis"),

      //https://www.spandidos-publications.com/ol/17/4/3625
      // Above header
      HtmlNodeFilters.tagWithAttributeRegex("div", "class", "super"),
      // Header
      HtmlNodeFilters.tagWithAttributeRegex("div", "class", "header"),
      // Banner
      HtmlNodeFilters.tagWithAttributeRegex("div", "class", "banner"),

      // Content left
      HtmlNodeFilters.tagWithAttributeRegex("div", "class", "content_sidebar"),
      // Content right
      HtmlNodeFilters.tagWithAttributeRegex("div", "class", "journal_information"),
      HtmlNodeFilters.tagWithAttributeRegex("div", "class", "article_sidebar"),

      // Content inside
      HtmlNodeFilters.tagWithAttributeRegex("div", "id", "moreLikeThisDiv"),
      HtmlNodeFilters.tagWithAttributeRegex("div", "class", "article_interactive"),
      HtmlNodeFilters.tagWithAttributeRegex("a", "class","staticContent"),

      // Content inside, however it listed information about the ariticle has been reference by others,
      HtmlNodeFilters.tagWithAttributeRegex("div", "id", "metrics"),
      HtmlNodeFilters.tagWithAttributeRegex("div", "id", "citationDiv"),
      HtmlNodeFilters.tagWithAttributeRegex("span", "class", "__dimensions_badge_embed__"),
      HtmlNodeFilters.tagWithAttributeRegex("span", "class", "altmetric-embed"),


      // Footer
      HtmlNodeFilters.tagWithAttributeRegex("div", "class", "footer")
    };

    InputStream interStream = new HtmlFilterInputStream(in, encoding,
            new HtmlCompoundTransform(
                    HtmlNodeFilterTransform.include(new OrFilter(includeNodes)),
                    HtmlNodeFilterTransform.exclude(new OrFilter(excludeNodes)) //, xformAllTags
            ));

    Reader reader = FilterUtil.getReader(interStream, encoding);
    return new ReaderInputStream(new WhiteSpaceFilter(reader));
  }

}
