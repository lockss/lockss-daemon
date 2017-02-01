/*
 * $Id$
 */

/*

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

package org.lockss.plugin.silverchair.ama;

import java.io.InputStream;

import org.htmlparser.NodeFilter;
import org.htmlparser.filters.OrFilter;
import org.lockss.daemon.PluginException;
import org.lockss.filter.html.*;
import org.lockss.plugin.*;

public class AmaScHtmlCrawlFilterFactory implements FilterFactory {

  /*
   * XXX Not sure what this indicates, however, it is in the parent version
   * AMA = American Medical Association (http://jamanetwork.com/)
   * Tabs 20151025
   * 1=extract/abstract/article
   * 2=discussion (w/i framework of article contents)
   * 3=figures
   * 4=tables
   * 5=video
   * 6=references
   * 7=letters
   * 8=cme
   * 9=citing
   * 10=comments
   * 12=supplemental
   *
   */

  @Override
  public InputStream createFilteredInputStream(ArchivalUnit au,
                                               InputStream in,
                                               String encoding)
      throws PluginException {
    return new HtmlFilterInputStream(
      in,
      encoding,
      HtmlNodeFilterTransform.exclude(new OrFilter(new NodeFilter[] {
          // DROP right column: related content, etc.
          //    except for citation links
          // KEEP links to article views, citation files, etc.
          
          HtmlNodeFilters.tag("header"),
          HtmlNodeFilters.tagWithAttributeRegex("section", "class", "master-(header|footer)"),
          HtmlNodeFilters.tagWithAttributeRegex("nav", "class", "issue-browse"),
          // http://jamanetwork.com/journals/jamainternalmedicine/fullarticle/2477128
          HtmlNodeFilters.tagWithAttributeRegex("div", "class", "(widget-(article[^ ]*link|EditorsChoice|LinkedContent|WidgetLoader))"),
          HtmlNodeFilters.tagWithAttributeRegex("div", "class", "(nav|(artmet|login)-modal|social-share)"),
          HtmlNodeFilters.tagWithAttributeRegex("div", "class", "(cme-info|no-access|reference|related|ymal)"),
          HtmlNodeFilters.tagWithAttributeRegex("div", "id", "(metrics|(reference|related)-tab|register)"),
          
          HtmlNodeFilters.allExceptSubtree(
              HtmlNodeFilters.tagWithAttributeRegex("div", "class", "sidebar"),
              HtmlNodeFilters.tagWithAttributeRegex("div", "id", "get-citation")),
          HtmlNodeFilters.allExceptSubtree(
              HtmlNodeFilters.tagWithAttributeRegex("ul", "class", "toolbar"),
              HtmlNodeFilters.tagWithAttributeRegex("div", "id", "get-citation")),
          HtmlNodeFilters.tagWithAttributeRegex("a", "class", "(download-ppt|related)"),
          
      }))
    );
  }

}
