/*
 * $Id: AnthroSourceHtmlFilterFactory.java,v 1.3 2007-10-02 21:02:17 thib_gc Exp $
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

import java.io.InputStream;

import org.lockss.daemon.PluginException;
import org.lockss.filter.html.*;
import org.lockss.plugin.*;

public class AnthroSourceHtmlFilterFactory implements FilterFactory {

  public InputStream createFilteredInputStream(ArchivalUnit au,
                                               InputStream in,
                                               String encoding)
      throws PluginException {
    InputStream ret = new HtmlFilterInputStream(in,
                                                encoding,
                                                new HtmlCompoundTransform(// Filter out <td class="rightRegion">...</td>
                                                                          HtmlNodeFilterTransform.exclude(HtmlNodeFilters.tagWithAttribute("td",
                                                                                                                                           "class",
                                                                                                                                           "rightRegion")),
                                                                          // Filter out <img class="JournalCover">...</img>
                                                                          HtmlNodeFilterTransform.exclude(HtmlNodeFilters.tagWithAttribute("img",
                                                                                                                                           "class",
                                                                                                                                           "JournalCover"))));
    // Need to nest them by hand in 1.26 becase 'new HtmlCompoundTransform(HtmlTransform[])' only in 1.27
    return new HtmlFilterInputStream(ret,
                                     encoding,
                                     new HtmlCompoundTransform(// Filter out <div class="institutionBanner">...</img>
                                                               HtmlNodeFilterTransform.exclude(HtmlNodeFilters.tagWithAttribute("div",
                                                                                                                                "class",
                                                                                                                                "institutionBanner")),
                                                               // Filter out <div class="citedBySection">...</div>
                                                               HtmlNodeFilterTransform.exclude(HtmlNodeFilters.tagWithAttribute("div",
                                                                                                                                "class",
                                                                                                                                "citedBySection"))));
  }

}
