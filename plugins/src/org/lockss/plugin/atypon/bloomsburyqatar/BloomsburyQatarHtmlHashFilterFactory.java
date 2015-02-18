/*
 * $Id$
 */

/*

Copyright (c) 2000-2013 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.plugin.atypon.bloomsburyqatar;

import java.io.InputStream;
import org.htmlparser.NodeFilter;
import org.lockss.filter.html.HtmlNodeFilters;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.plugin.atypon.BaseAtyponHtmlHashFilterFactory;

/* Siam can use the BaseAtyponHtmlHashFilter and extend it for the extra bits it needs */
public class BloomsburyQatarHtmlHashFilterFactory extends BaseAtyponHtmlHashFilterFactory {

  @Override
  public InputStream createFilteredInputStream(ArchivalUnit au,
      InputStream in,
      String encoding) {
    NodeFilter[] bqfilter = new NodeFilter[] {
        HtmlNodeFilters.tagWithAttribute("div", "id", "top"),
        HtmlNodeFilters.tagWithAttribute("div", "id", "rightCol"), // can't crawl filter this one
        HtmlNodeFilters.tagWithAttribute("div", "class", "altmetric-embed"),
        HtmlNodeFilters.tagWithAttribute("div", "id", "hiddenCitationDiv"),
        HtmlNodeFilters.tagWithAttribute("div", "id", "hiddenCommentsDiv"),
        HtmlNodeFilters.tagWithAttribute("div", "id", "hiddenSupplDiv"),
        
        HtmlNodeFilters.tagWithAttributeRegex("div",  "class",  "addthis_toolbox"),
        HtmlNodeFilters.tagWithAttributeRegex("div",  "class",  "fb-comments"),
        HtmlNodeFilters.tagWithAttributeRegex("div",  "id",  "wibiya"),
    };

    // super.createFilteredInputStream adds bqfilter to the baseAtyponFilters
    // and returns the filtered input stream using an array of NodeFilters that 
    // combine the two arrays of NodeFilters.
    return super.createFilteredInputStream(au, in, encoding, bqfilter);

  }
}

