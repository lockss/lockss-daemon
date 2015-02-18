/*
 * $Id$
 */

/*

Copyright (c) 2000-2014 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.plugin.nationalweatherassociation;

import java.io.InputStream;

import org.htmlparser.NodeFilter;
import org.htmlparser.filters.*;
import org.lockss.filter.html.*;
import org.lockss.plugin.*;

/*
 * This journal does not have list of issues. Articles are listed 
 * by year/volume.
 *  journal home - <nwabase>.org/<journal_id>/
 *  volume toc - <nwabase>.org/<journal_id>/publications2013.php
 *  start url/volume page - 
 *      <nwabase>.org/<journal_id>/include/publications2013.php
 *  abstract  - 
 *      <nwabase>.org/<journal_id>/abstracts/2013/2013-JOM22/abstract.php
 */
public class NationalWeatherAssociationHtmlHashFilterFactory 
  implements FilterFactory {
  
  public InputStream createFilteredInputStream(ArchivalUnit au, 
                                               InputStream in,
                                               String encoding) {

    NodeFilter[] filters = new NodeFilter[] {
        new TagNameFilter("script"),
        // filter out comments
        HtmlNodeFilters.comment(),
        // stylesheets
        HtmlNodeFilters.tagWithAttribute("link", "rel", "stylesheet"),
        // top header
        HtmlNodeFilters.tagWithAttribute("div", "id", "header"),
        // left sidebar
        HtmlNodeFilters.tagWithAttribute("div", "id", "left"),
        // right sidebar deadlines
        HtmlNodeFilters.tagWithAttribute("div", "id", "deadlines"),
        // footer
        HtmlNodeFilters.tagWithAttribute("div", "id", "footer"),
        // banner
        HtmlNodeFilters.tagWithAttributeRegex("img", "src", "banner\\.png"),
        // last updated date
        HtmlNodeFilters.tagWithTextRegex("p", ".*Last updated.*", true), 
    };

    return new HtmlFilterInputStream(in, encoding, 
        HtmlNodeFilterTransform.exclude(new OrFilter(filters)));
  }
    
}
