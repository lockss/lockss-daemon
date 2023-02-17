/*

Copyright (c) 2000-2023, Board of Trustees of Leland Stanford Jr. University

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are met:

1. Redistributions of source code must retain the above copyright notice,
this list of conditions and the following disclaimer.

2. Redistributions in binary form must reproduce the above copyright notice,
this list of conditions and the following disclaimer in the documentation
and/or other materials provided with the distribution.

3. Neither the name of the copyright holder nor the names of its contributors
may be used to endorse or promote products derived from this software without
specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
POSSIBILITY OF SUCH DAMAGE.

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
