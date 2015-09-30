/*
 * $Id$
 */

/*

Copyright (c) 2000-2015 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.plugin.pion;

import java.io.*;
import java.util.List;

import org.htmlparser.NodeFilter;
import org.htmlparser.filters.OrFilter;
import org.lockss.daemon.PluginException;
import org.lockss.filter.*;
import org.lockss.filter.html.*;
import org.lockss.plugin.*;
import org.lockss.util.*;

public class PionHashHtmlFilterFactory implements FilterFactory {

  static HtmlTagFilter.TagPair[] tagpairs = {
      new HtmlTagFilter.TagPair("<?tf=", ">"),
      new HtmlTagFilter.TagPair("<!--", "this archival unit -->", true),
    };
  static List<HtmlTagFilter.TagPair> tagList = ListUtil.fromArray(tagpairs);
  
  public InputStream createFilteredInputStream(ArchivalUnit au,
                                               InputStream in,
                                               String encoding)
      throws PluginException {
    // Remove malformed XML processing instruction that confuses HtmlParser
    // and permission statements on some issues
    in = new BufferedInputStream(new ReaderInputStream(
        HtmlTagFilter.makeNestedFilter(FilterUtil.getReader(in, encoding), tagList)));
    
    // First filter with HtmlParser constructs
    NodeFilter[] filters = new NodeFilter[] {
        // contains changing metadata tags and scripts
        HtmlNodeFilters.tag("head"),
        HtmlNodeFilters.tag("script"),
        // not needed
        HtmlNodeFilters.tagWithAttribute("div", "id", "top-buttons"),
        HtmlNodeFilters.tagWithAttribute("div", "id", "_atssh"),
        HtmlNodeFilters.tagWithAttribute("div", "id", "topspace"),
        // found at http://www.envplan.com/abstract.cgi?id=d7912 
        // <div id="at20mc" style="z-index: 1000000;">
        HtmlNodeFilters.tagWithAttribute("div", "id", "at20mc"),
        // Contains an ever-growing list of volumes/years
        HtmlNodeFilters.tagWithAttribute("div", "class", "dropdown"),
        // Contains the year in progress
        HtmlNodeFilters.tagWithAttribute("div", "id", "footer"),
        // Somewhat CLOCKSS-specific: remove Stanford's SFX linking
        HtmlNodeFilters.tagWithAttributeRegex("a", "href", "^http://library\\.stanford\\.edu/sfx"),
    };
    OrFilter orFilter = new OrFilter(filters);
    InputStream filteredInputStream = new HtmlFilterInputStream(in,
                                                                encoding,
                                                                HtmlNodeFilterTransform.exclude(orFilter));

    // Then filter with non-HtmlParser constructs
    String[][] findAndReplace = new String[][] {
        {"<p><p>&nbsp;<br><br><!-- AddThis Button BEGIN -->", ""}, 
        {"<p>&nbsp;<br><br><!-- AddThis Button BEGIN -->", ""},
        {"<p><br><br><!-- AddThis Button BEGIN -->", ""},
        {"<br><br><!-- AddThis Button BEGIN -->", ""},
        {"<p>&nbsp;      <div class=\"space\"></div>", "      <div class=\"space\"></div>"},
        {" \n<p class=ref>", "\n<p class=ref>"},
        {" \n<p class=\"ref\">", "\n<p class=\"ref\">"},
        {" \n<p>", "\n<p>"},
    };
    Reader filteredReader = StringFilter.makeNestedFilter(FilterUtil.getReader(filteredInputStream, encoding),
                                                          findAndReplace,
                                                          false);
    return new ReaderInputStream(new WhiteSpaceFilter(filteredReader));
  }

}
