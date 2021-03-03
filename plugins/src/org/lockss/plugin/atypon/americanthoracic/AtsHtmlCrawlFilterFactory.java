/*

Copyright (c) 2000-2021, Board of Trustees of Leland Stanford Jr. University
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

package org.lockss.plugin.atypon.americanthoracic;

import java.io.InputStream;
import java.util.regex.Pattern;

import org.htmlparser.Node;
import org.htmlparser.NodeFilter;
import org.htmlparser.tags.CompositeTag;
import org.htmlparser.tags.LinkTag;
import org.lockss.daemon.PluginException;
import org.lockss.plugin.*;
import org.lockss.plugin.atypon.BaseAtyponHtmlCrawlFilterFactory;

public class AtsHtmlCrawlFilterFactory extends BaseAtyponHtmlCrawlFilterFactory {
  protected static final Pattern related_articlelink_pattern = Pattern.compile("related( article|editorial)?", Pattern.CASE_INSENSITIVE);

  NodeFilter[] filters = new NodeFilter[] {
    //Leave in this specific filter for American Thoracis which often puts 
      //related links on the TOC alongside the other formats (abs, full, pdf) 
      // and the only difference is the text "related, related editorial, related article...yeesh"
      //see: http://www.atsjournals.org/toc/ajrccm/193/2
          new NodeFilter() {
            @Override public boolean accept(Node node) {
              if (!(node instanceof LinkTag)) return false;
              String allText = ((CompositeTag)node).toPlainTextString();
              return related_articlelink_pattern.matcher(allText).find();
            }
        },
  };
  public InputStream createFilteredInputStream(ArchivalUnit au,
                                               InputStream in,
                                               String encoding)
      throws PluginException {
    return super.createFilteredInputStream(au, in, encoding, filters);
  }

}
