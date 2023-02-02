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

package org.lockss.plugin.hispanicissuesonline;

import java.io.*;
import java.util.List;

import org.lockss.daemon.PluginException;
import org.lockss.filter.*;
import org.lockss.plugin.*;
import org.lockss.util.*;

public class HispanicIssuesOnLineHtmlFilterFactory implements FilterFactory {

  public InputStream createFilteredInputStream(ArchivalUnit au,
                                               InputStream in,
                                               String encoding)
      throws PluginException {
    List pairs = ListUtil.list(new HtmlTagFilter.TagPair(// Filter between "<!-- BEGIN PARENT LINKS -->"...
                                                         "<!-- BEGIN PARENT LINKS -->",
                                                         // ...and "<!-- END PARENT LINKS -->"
                                                         "<!-- END PARENT LINKS -->"),
                               new HtmlTagFilter.TagPair(// Filter between "<!-- BEGIN U of M TEMPLATE HEADER -->"...
                                                         "<!-- BEGIN U of M TEMPLATE HEADER -->",
                                                         // ...and "<!-- END U of M TEMPLATE HEADER -->"
                                                         "<!-- END U of M TEMPLATE HEADER -->"),
                               new HtmlTagFilter.TagPair(// Filter between "<!-- BEGIN U of M TEMPLATE FOOTER -->"...
                                                         "<!-- BEGIN U of M TEMPLATE FOOTER -->",
                                                         // ...and "<!-- END U of M TEMPLATE FOOTER -->"
                                                         "<!-- END U of M TEMPLATE FOOTER -->"));
    Reader reader = FilterUtil.getReader(in, encoding);
    Reader pairFilter = HtmlTagFilter.makeNestedFilter(reader, pairs);
    return new ReaderInputStream(pairFilter);
  }
}
