/*
 * $Id:$
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

package org.lockss.plugin.pensoft.oai;

import java.io.InputStream;
import java.io.Reader;

import org.htmlparser.NodeFilter;
import org.htmlparser.filters.*;
import org.lockss.filter.FilterUtil;
import org.lockss.filter.StringFilter;
import org.lockss.filter.html.*;
import org.lockss.plugin.*;
import org.lockss.util.ReaderInputStream;

public class PensoftOaiHtmlFilterFactory implements FilterFactory {

  public InputStream createFilteredInputStream(ArchivalUnit au,
                                               InputStream in,
                                               String encoding) {
    NodeFilter[] filters = new NodeFilter[] {
     //filter out script
     new TagNameFilter("script"),
     new TagNameFilter("style"),
     
     HtmlNodeFilters.tagWithAttribute("div", "id", "header"),
     //popup stuff may change
     HtmlNodeFilters.tagWithAttribute("div", "id", "feedback-popup"),
     HtmlNodeFilters.tagWithAttribute("div", "id", "P-Post-Review-Form-Poll"),
     HtmlNodeFilters.tagWithAttribute("div", "class", "popup-background"),
     HtmlNodeFilters.tagWithAttribute("div", "class", "P-Article-References-For-Baloon"),
     HtmlNodeFilters.tagWithAttribute("div", "id", "ArticleBaloon"),
     HtmlNodeFilters.tagWithAttribute("div", "class", "P-clear"),
     
     // found in http://compcytogen.pensoft.net/articles.php?id=5304
     // <link type="text/css" href="/lib/css/layout.css?v=1472563221" values change
     HtmlNodeFilters.tagWithAttribute("link", "type", "text/css"),
     
    };
    InputStream filteredStream = new HtmlFilterInputStream(in, encoding,
        HtmlNodeFilterTransform.exclude(new OrFilter(filters)));
    Reader httpFilter = new StringFilter(FilterUtil.getReader(filteredStream, encoding), "http:", "https:");
    return new ReaderInputStream(httpFilter);
  }

}
