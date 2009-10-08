/*
 * $Id: IngentaJournalHtmlFilterFactory.java,v 1.8 2009-10-08 07:24:14 thib_gc Exp $
 */ 

/*

Copyright (c) 2000-2009 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.plugin.ingenta;

import java.io.InputStream;

import org.lockss.daemon.PluginException;
import org.lockss.filter.html.*;
import org.lockss.plugin.*;

public class IngentaJournalHtmlFilterFactory implements FilterFactory {

  public InputStream createFilteredInputStream(ArchivalUnit au,
                                               InputStream in,
                                               String encoding)
      throws PluginException {
    HtmlTransform[] transforms = new HtmlTransform[] {
        // Filter out <div id="footer">...</div>
        HtmlNodeFilterTransform.exclude(HtmlNodeFilters.tagWithAttribute("div",
                                                                         "id",
                                                                         "footer")),
        // Filter out <div id="top-ad-alignment">...</div>
        HtmlNodeFilterTransform.exclude(HtmlNodeFilters.tagWithAttribute("div",
                                                                         "id",
                                                                         "top-ad-alignment")),
        // Filter out <div id="top-ad">...</div>
        HtmlNodeFilterTransform.exclude(HtmlNodeFilters.tagWithAttribute("div",
                                                                         "id",
                                                                         "top-ad")),
        // Filter out <div id="ident">...</div>
        HtmlNodeFilterTransform.exclude(HtmlNodeFilters.tagWithAttribute("div",
                                                                         "id",
                                                                         "ident")),         
        // Filter out <div id="ad">...</div>
        HtmlNodeFilterTransform.exclude(HtmlNodeFilters.tagWithAttribute("div",
                                                                         "id",
                                                                         "ad")),
        // Filter out <div id="vertical-ad">...</div>
        HtmlNodeFilterTransform.exclude(HtmlNodeFilters.tagWithAttribute("div",
                                                                         "id",
                                                                          "vertical-ad")),
        // Filter out <div class="right-col-download">...</div>
        HtmlNodeFilterTransform.exclude(HtmlNodeFilters.tagWithAttribute("div",
                                                                         "class",
                                                                         "right-col-download")),                                                               
        // Filter out <div id="cart-navbar">...</div>
        HtmlNodeFilterTransform.exclude(HtmlNodeFilters.tagWithAttribute("div",
                                                                         "id",
                                                                         "cart-navbar")),   
//         // Filter out <div class="heading-macfix article-access-options">...</div>
//        HtmlNodeFilterTransform.exclude(HtmlNodeFilters.tagWithAttribute("div",
//                                                                         "class",
//                                                                         "heading-macfix article-access-options")),                                                                           
        // Filter out <div id="baynote-recommendations">...</div>
        HtmlNodeFilterTransform.exclude(HtmlNodeFilters.tagWithAttribute("div",
                                                                         "id",
                                                                         "baynote-recommendations")),   
    };
    return new HtmlFilterInputStream(in,
                                     encoding,
                                     new HtmlCompoundTransform(transforms));
  }

}
