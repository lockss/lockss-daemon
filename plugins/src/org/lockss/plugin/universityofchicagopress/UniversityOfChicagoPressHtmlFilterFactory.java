/*
 * $Id: UniversityOfChicagoPressHtmlFilterFactory.java,v 1.4 2009-01-20 22:25:44 thib_gc Exp $
 */

/*

Copyright (c) 2000-2009 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.plugin.universityofchicagopress;

import java.io.InputStream;

import org.htmlparser.filters.TagNameFilter;
import org.lockss.daemon.PluginException;
import org.lockss.filter.html.*;
import org.lockss.plugin.*;

public class UniversityOfChicagoPressHtmlFilterFactory implements FilterFactory {

  public InputStream createFilteredInputStream(ArchivalUnit au,
                                               InputStream in,
                                               String encoding)
      throws PluginException {
    HtmlTransform[] transforms = new HtmlTransform[] {
        // Filter out <div id="globalLinks_welcomeMessage">...</div>
        HtmlNodeFilterTransform.exclude(HtmlNodeFilters.tagWithAttribute("div",
                                                                         "id",
                                                                         "globalLinks_welcomeMessage")),
        // Filter out <div id="globalLinks_accessMessage">...</div>
        HtmlNodeFilterTransform.exclude(HtmlNodeFilters.tagWithAttribute("div",
                                                                         "id",
                                                                         "globalLinks_accessMessage")),
        // Filter out <div id="smallColumn">...</div>
        HtmlNodeFilterTransform.exclude(HtmlNodeFilters.tagWithAttribute("div",
                                                                         "id",
                                                                         "smallColumn")),
        // Filter out <div id="copyrightInfo">...</div>
        HtmlNodeFilterTransform.exclude(HtmlNodeFilters.tagWithAttribute("div",
                                                                         "id",
                                                                         "copyrightInfo")),

        // Filter out <div class="citedBySection">...</div>
        HtmlNodeFilterTransform.exclude(HtmlNodeFilters.tagWithAttribute("div",
                                                                         "class",
                                                                         "citedBySection")),
//        // Filter out <table class="articleEntry">...</table>
//        HtmlNodeFilterTransform.exclude(HtmlNodeFilters.tagWithAttribute("table",
//                                                                         "class",
//                                                                         "articleEntry")),
//        // Filter out <div class="footerAd">...</div>
//        HtmlNodeFilterTransform.exclude(HtmlNodeFilters.tagWithAttribute("div",
//                                                                         "class",
//                                                                         "footerAd")),
        // Filter out <link type="application/rss+xml">...</link>
        HtmlNodeFilterTransform.exclude(HtmlNodeFilters.tagWithAttribute("link",
                                                                         "type",
                                                                         "application/rss+xml")),
        // Filter out <a href="...">...</a> where the href value matches a regular exception
        HtmlNodeFilterTransform.exclude(HtmlNodeFilters.tagWithAttributeRegex("a",
                                                                              "href",
                                                                              "/action/showFeed\\?(.*&)?mi=")),
        // Filter out <script>...</script>
        HtmlNodeFilterTransform.exclude(new TagNameFilter("script")),
    };
    return new HtmlFilterInputStream(in,
                                     encoding,
                                     new HtmlCompoundTransform(transforms));
  }

}
