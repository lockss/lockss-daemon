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

package org.lockss.plugin.anthrosource;

import java.io.*;

import org.htmlparser.util.NodeList;
import org.lockss.daemon.PluginException;
import org.lockss.filter.html.*;
import org.lockss.plugin.*;

public class AnthroSourceHtmlFilterFactory implements FilterFactory {

  // A re-implementation of HtmlCompoundTransform to ship with 1.26
  // Revisit after 1.27
  public static class HtmlCompoundTransform implements HtmlTransform {
    protected HtmlTransform[] transforms;
    public HtmlCompoundTransform(HtmlTransform[] transforms) { this.transforms = transforms; }
    public NodeList transform(NodeList nodeList) throws IOException {
      for (int ix = 0; ix < transforms.length; ix++) { nodeList = transforms[ix].transform(nodeList); }
      return nodeList;
    }
  }

  public InputStream createFilteredInputStream(ArchivalUnit au,
                                               InputStream in,
                                               String encoding)
      throws PluginException {
    HtmlTransform[] transforms = new HtmlTransform[] {
        // Filter out <td class="rightRegion">...</td>
        HtmlNodeFilterTransform.exclude(HtmlNodeFilters.tagWithAttribute("td",
                                                                         "class",
                                                                         "rightRegion")),
        // Filter out <img class="JournalCover">...</img>
        HtmlNodeFilterTransform.exclude(HtmlNodeFilters.tagWithAttribute("img",
                                                                         "class",
                                                                         "JournalCover")),
        // Filter out <div class="institutionBanner">...</img>
        HtmlNodeFilterTransform.exclude(HtmlNodeFilters.tagWithAttribute("div",
                                                                         "class",
                                                                         "institutionBanner")),
        // Filter out <div class="citedBySection">...</div>
        HtmlNodeFilterTransform.exclude(HtmlNodeFilters.tagWithAttribute("div",
                                                                         "class",
                                                                         "citedBySection")),
    };
    return new HtmlFilterInputStream(in,
                                     encoding,
                                     new HtmlCompoundTransform(transforms));
  }

}
