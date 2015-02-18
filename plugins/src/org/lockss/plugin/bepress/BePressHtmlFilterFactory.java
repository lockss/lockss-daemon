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

package org.lockss.plugin.bepress;

import java.io.InputStream;

import org.htmlparser.NodeFilter;
import org.htmlparser.filters.OrFilter;
import org.lockss.daemon.PluginException;
import org.lockss.filter.html.*;
import org.lockss.plugin.*;

public class BePressHtmlFilterFactory implements FilterFactory {

  public InputStream createFilteredInputStream(ArchivalUnit au,
                                               InputStream in,
                                               String encoding)
      throws PluginException {
    NodeFilter[] filters = new NodeFilter[] {
        // News item in top-right corner of some journals
        HtmlNodeFilters.tagWithAttribute("div", "id", "news"),
        // Monotonically-growing drop-down list of issues
        HtmlNodeFilters.tagWithAttribute("select", "name", "url"),
        // Sidebar can contain short-term customizations (e.g. promos about special issues)
        HtmlNodeFilters.tagWithAttribute("div", "id", "sidebar"),
        // this contains changing download numbers and date - only
        // on articles at http://docs.lib.purdue.edu/clcweb/ (so far)
        HtmlNodeFilters.tagWithAttribute("div", "id", "custom-fields"),
        HtmlNodeFilters.tagWithAttribute("div", "id", "recommended_citation"),
        // also removing right sidebar wi/an "AltMetric box" which indicates "buzz" on an article
        HtmlNodeFilters.tagWithAttribute("div", "id", "beta_7-3"),
        HtmlNodeFilters.tagWithAttribute("div", "id", "doi"),

    };
    return new HtmlFilterInputStream(in,
                                     encoding,
                                     HtmlNodeFilterTransform.exclude(new OrFilter(filters)));
  }

}
