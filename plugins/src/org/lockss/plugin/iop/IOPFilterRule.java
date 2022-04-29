/*

Copyright (c) 2000-2022, Board of Trustees of Leland Stanford Jr. University
All rights reserved.

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

package org.lockss.plugin.iop;

import java.io.*;
import java.util.*;
import org.lockss.util.*;
import org.lockss.filter.*;
import org.lockss.plugin.FilterRule;

/**
 * <p>This URL normalizer goes with the ancient plugin
 * org.lockss.plugin.iop.IOPPlugin, not with current plugins for the
 * IOPscience platform.</p>
 */
public class IOPFilterRule implements FilterRule {
  public Reader createFilteredReader(Reader reader) {
    List tagList = ListUtil.list(
	//cruft in the TOC
        new HtmlTagFilter.TagPair("<td class=\"toc_left\">", "</td>", true),

        new HtmlTagFilter.TagPair("&nbsp;|&nbsp; <a title=\"Citing articles: ", "Citing articles</a>", true),

	//content box
        new HtmlTagFilter.TagPair("<div id=\"art-opts\">", "</div>", true),
        new HtmlTagFilter.TagPair("<ul class=\"art-opts-mm\">", "</ul>", true),
	//hackish, but this will remove the links to the refs, which change
	//over time
        new HtmlTagFilter.TagPair("<span class=\"smltxt\">", "</span>", true),
        new HtmlTagFilter.TagPair("<!--", "-->", true),
        new HtmlTagFilter.TagPair("<script", "</script>", true),
        new HtmlTagFilter.TagPair("<", ">")
        );
    Reader filteredReader = HtmlTagFilter.makeNestedFilter(reader, tagList);
    return new WhiteSpaceFilter(filteredReader);
  }
}
