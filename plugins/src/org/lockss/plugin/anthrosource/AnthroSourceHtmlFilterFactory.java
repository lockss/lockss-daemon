/*
 * $Id: AnthroSourceHtmlFilterFactory.java,v 1.4 2007-10-03 16:25:56 thib_gc Exp $
 */

/*

Copyright (c) 2000-2007 Board of Trustees of Leland Stanford Jr. University,
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
