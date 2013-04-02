/*
 * $Id: ASCEHtmlHashFilterFactory.java,v 1.1 2013-04-02 21:16:22 ldoan Exp $
 */

/*

Copyright (c) 2000-2012 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.plugin.americansocietyofcivilengineers;

import java.io.InputStream;

import org.htmlparser.NodeFilter;
import org.htmlparser.filters.*;
import org.lockss.filter.html.*;
import org.lockss.plugin.*;

public class ASCEHtmlHashFilterFactory implements FilterFactory {

  public InputStream createFilteredInputStream(ArchivalUnit au,
      InputStream in, String encoding) {
    NodeFilter[] filters = new NodeFilter[] {
        // infrastructure assessment ad
        // http://ascelibrary.org/doi/abs/10.1061/%28ASCE%291076-0431%282010%2916%3A1%2837%29
        // <a href="/action/clickThrough?id=1238&url=%2Fpage%2Fjitse4%2Finfrastructureassessmentandpublicpolicy&loc=%2Fdoi%2Fabs%2F10.1061%2F%2528ASCE%25290733-9364%25282005%2529131%253A1%252815%2529&pubId=40087620">
        HtmlNodeFilters.tagWithTextRegex("a", "infrastructureassessmentandpublicpolicy", true),
        // concrete construction ad
        // http://ascelibrary.org/doi/abs/10.1061/%28ASCE%291076-0431%282010%2916%3A1%2837%29
        HtmlNodeFilters.tagWithTextRegex("a", "concreteconstructionandrepairpavementandmanagemen", true),
        // footer ads on full text page
        // <map name="PubPartners950px1.jpg">
        // http://ascelibrary.org/doi/abs/10.1061/%28ASCE%291076-0431%282010%2916%3A1%2837%29
        // <map name="PubPartners950px1.jpg">
        // <img src="/sda/1164/PubPartners950px1.jpg" alt="PubPartners950px1.jpg" usemap="#PubPartners950px1.jpg"/>
        // <area shape="rect" coords="187,58,305,101" href="http://www.crossref.org/" title="crossref.org" alt="crossref.org" />
        // <area shape="rect" coords="361,41,467,113" href="http://www.inasp.info/" title="www.inasp.info" alt="www.inasp.info" />
        // <area shape="rect" coords="327,29,619,122" href="http://www.clockss.org/" title="clockss.org" alt="clockss.org" />
        // <area shape="rect" coords="690,50,810,98" href="http://copyright.com/" title="copyright.com" alt="copyright.com" />
        // <area shape="rect" coords="873,36,920,116" href="http://publicationethics.org/" title="publicationethics.org" alt="publicationethics.org" />
        HtmlNodeFilters.tagWithAttribute("map", "name", "PubPartners950px1.jpg"),
        HtmlNodeFilters.tagWithAttribute("img", "usemap", "#PubPartners950px1.jpg"),
        HtmlNodeFilters.tagWithAttribute("area", "href", "http://www.crossref.org/"),
        HtmlNodeFilters.tagWithAttribute("area", "href", "http://www.inasp.info/"),
        HtmlNodeFilters.tagWithAttribute("area", "href", "http://www.clockss.org/"),
        HtmlNodeFilters.tagWithAttribute("area", "href", "http://copyright.com/"),
        HtmlNodeFilters.tagWithAttribute("area", "href", "http://publicationethics.org/"),
        // footer ads on abs page
        // <map name="PubPartners950px2.jpg">
        // <img src="/sda/1166/PubPartners950px2.jpg" alt="PubPartners950px2" usemap="#PubPartners950px2.jpg"/>
        // <area shape="rect" coords="184,50,277,92" href="http://www.crossref.org/" title="crossref.org" alt="crossref.org" />
        // <area shape="rect" coords="341,61,477,88" href="http://www.projectcounter.org/" title="projectcounter.org" alt="projectcounter.org" />
        // <area shape="rect" coords="525,37,621,119" href="http://www.portico.org/" title="portico.org" alt="portico.org" />
        // <area shape="rect" coords="677,45,804,103" href="http://www.rightslink.com/" title="rightslink.com" alt="rightslink.com" />
        // <area shape="rect" coords="847,59,968,85" href="http://www.atypon.com/" title="atypon.com" alt="atypon.com" />
        HtmlNodeFilters.tagWithAttribute("map", "name", "PubPartners950px2.jpg"),
        HtmlNodeFilters.tagWithAttribute("img", "usemap", "#PubPartners950px2.jpg"),
        HtmlNodeFilters.tagWithAttribute("area", "href", "http://www.crossref.org/"),
        HtmlNodeFilters.tagWithAttribute("area", "href", "http://www.projectcounter.org/"),
        HtmlNodeFilters.tagWithAttribute("area", "href", "http://www.portico.org/"),
        HtmlNodeFilters.tagWithAttribute("area", "href", "http://www.rightslink.com/"),
        HtmlNodeFilters.tagWithAttribute("area", "href", "http://www.atypon.org/"),
        // footer copyright @ 1996-2013
        // <div id="copyright">
        // http://ascelibrary.org/toc/jaeied/18/4
        HtmlNodeFilters.tagWithAttribute("div", "id", "footer_message"),
        new TagNameFilter("script"),
    };
    return new HtmlFilterInputStream(in, encoding,
        HtmlNodeFilterTransform.exclude(new OrFilter(filters)));
    }
    
}
