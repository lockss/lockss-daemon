/*
 * $Id$
 */

/*

Copyright (c) 2000-2016 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.plugin.dove;

import java.io.InputStream;

import org.htmlparser.NodeFilter;
import org.htmlparser.filters.*;
import org.lockss.filter.html.*;
import org.lockss.plugin.*;

/*
 * This is a include-exclude filter.
 * First tell the the filter the set of items to INCLUDE 
 * and then a list of anything to EXCLUDE from what was included.
 * This is extreme so be sure not to miss entire chunks....
 */
public class DoveHtmlHashFilterFactory implements FilterFactory {

    // With include-exclude you need at least one chunk from every html page type 
    protected static NodeFilter[] infilters = new NodeFilter[] {
      // manifest page
      // <base>/lockss.php?t=clockss&pa=issue&j_id=#&year=#
      HtmlNodeFilters.tagWithAttribute("div", "class", "copy sitemap"),
      // issue TOC: <base>/pub_title-i#-j# 
      HtmlNodeFilters.tagWithAttributeRegex("div", "class", "volume-issues"),
      // article landing page and article full-text
      // <base>/article-title-article-J_ABBR
      HtmlNodeFilters.tagWithAttribute("div", "class", "articles"),
      
    };
    
    /*
     * This seems to be a fairly basic site, so by limiting what we've included
     * we shouldn't need to exclude much
     */
    protected static NodeFilter[] xfilters = new NodeFilter[] {
      
      HtmlNodeFilters.tag("script"),
      HtmlNodeFilters.tag("noscript"),
      // video capture has a "views" counter
      //https://www.dovepress.com/patient-factors-influencing-dermal-filler-complications-prevention-ass-peer-reviewed-article-CCID
      HtmlNodeFilters.tagWithAttribute("div", "class", "video-figcaption"),
      // TOC page changes the labels associated with articles, adding "highly-accessed" etc
      HtmlNodeFilters.tagWithAttributeRegex("div",  "class", "article-labels "),
      // article pages - just under the editor identity
      HtmlNodeFilters.tagWithAttribute("div",  "class", "altmetric-embed"),
      // these shouldn't be in the include, but be safe
      HtmlNodeFilters.tagWithAttribute("span",  "class", "ticker-num"),

      };

    @Override
    public InputStream createFilteredInputStream(ArchivalUnit au,
        InputStream in, String encoding) {
      
     return new HtmlFilterInputStream(in,
          encoding,
          new HtmlCompoundTransform(
              HtmlNodeFilterTransform.include(new OrFilter(infilters)),
              HtmlNodeFilterTransform.exclude(new OrFilter(xfilters))
              ));
    }
  
}
