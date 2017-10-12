/* $Id:$
 
Copyright (c) 2000-2017 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.plugin.pub2web.iet;

import java.io.InputStream;
import org.htmlparser.NodeFilter;
import org.htmlparser.filters.*;
import org.lockss.filter.html.*;
import org.lockss.plugin.*;

public class IetHtmlHashFilterFactory implements FilterFactory {

  public InputStream createFilteredInputStream(ArchivalUnit au,
      InputStream in, String encoding) {
    NodeFilter[] filters = new NodeFilter[] {
	HtmlNodeFilters.tag("script"),
	HtmlNodeFilters.tag("noscript"),
	HtmlNodeFilters.tag("head"),
	
	//List of volumes and issues
	HtmlNodeFilters.tagWithAttribute("div", "class", "issueBar"),
	//More recent articles
	HtmlNodeFilters.tagWithAttribute("a", "title", "Cited By"),
	HtmlNodeFilters.tagWithAttribute("li",  "id", "cite"),
	//copyright etc
        HtmlNodeFilters.tagWithAttribute("div", "class", "footercontainer"),
        //banner
        HtmlNodeFilters.tagWithAttribute("div", "id", "header"),
        //Keywords that change order
        HtmlNodeFilters.tagWithAttributeRegex("div", "class", "aboutthisarticle"),
        //right nav bar
        HtmlNodeFilters.tagWithAttribute("div", "id", "sidebar_right"),
        
        //From other children to be safe
        HtmlNodeFilters.tagWithAttribute("div", "id", "previewWrapper"),
        HtmlNodeFilters.tagWithAttribute("div", "id", "footer"),
        HtmlNodeFilters.tagWithAttribute("div", "class", "pubtopright"),
        HtmlNodeFilters.tagWithAttribute("div", "class", "hiddenjsdiv metricsEndDate"),
        HtmlNodeFilters.tagWithAttributeRegex("div",  "class",  "^metrics "),
        HtmlNodeFilters.tagWithAttribute("input",  "name", "copyright"),
        HtmlNodeFilters.tagWithAttribute("div", "class", "crossSelling"),
        HtmlNodeFilters.tagWithAttribute("div", "id", "related"),
        HtmlNodeFilters.tagWithAttribute("div",  "class", "contentTypeOptions"),
        HtmlNodeFilters.tagWithAttribute("div",  "class", "articlenav"),
        HtmlNodeFilters.tagWithAttribute("li",  "class", "previousLinkContainer"),
        HtmlNodeFilters.tagWithAttribute("li",  "class", "indexLinkContainer"),
        HtmlNodeFilters.tagWithAttribute("li",  "class", "nextLinkContainer"),

    };
    return (new HtmlFilterInputStream(in, encoding, HtmlNodeFilterTransform.exclude(new OrFilter(filters))));

    }
    
}
