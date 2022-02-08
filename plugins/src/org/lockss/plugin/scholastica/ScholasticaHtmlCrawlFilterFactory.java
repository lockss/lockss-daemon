/*

Copyright (c) 2000-2022, Board of Trustees of Leland Stanford Jr. University
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
package org.lockss.plugin.scholastica;

import org.htmlparser.NodeFilter;
import org.htmlparser.filters.OrFilter;
import org.lockss.daemon.PluginException;
import org.lockss.filter.html.HtmlFilterInputStream;
import org.lockss.filter.html.HtmlNodeFilterTransform;
import org.lockss.filter.html.HtmlNodeFilters;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.plugin.FilterFactory;

import java.io.InputStream;

public class ScholasticaHtmlCrawlFilterFactory implements FilterFactory {

  private static String LI_REGEX;

  public InputStream createFilteredInputStream(ArchivalUnit au,
                                               InputStream in,
                                               String encoding)
      throws PluginException {
    // get the year from the au, and construct the regex of issues in the year in question
    // gets used in a allExceptSubtree
    LI_REGEX = "Vol\\. \\d+, Issue \\d+, " + au.getTdbAu().getYear();

    NodeFilter[] excludeFilters = new NodeFilter[] {
        // all header footer menus
        HtmlNodeFilters.tag("homepage-header"),
        HtmlNodeFilters.tag("inner-page-header"),
        HtmlNodeFilters.tag("nav-bar"),
        HtmlNodeFilters.tag("nav"),
        HtmlNodeFilters.tag("app-footer"),

        //article page header/info area
        HtmlNodeFilters.tagWithAttribute("div", "class", "banner-page-header"),

        // on issues landing page, we get rid of all the issues that are not in our desired year
        // keeping these li elements
        // <li _ngcontent-dis-c15="" class="issue-unit" id="issue-3555">
        //   <div _ngcontent-dis-c15="">
        //     <a _ngcontent-dis-c15="" href="/issue/3555">
        //       <div _ngcontent-dis-c15="" class="img fade-img-in" style="background-image: linear-gradient(360deg, rgb(98, 85, 4), rgba(56, 55, 51, 0.07)), url(&quot;https://s3.amazonaws.com/production.scholastica/issue/3555/cover/small/200202_hb_oh_02_2020.jpg?1629726786&quot;);">
        //         <div _ngcontent-dis-c15="" class="issue-info">Vol. 2, Issue 4, 2020</div>
        //       </div>
        //       <div _ngcontent-dis-c15="" class="lede">This issue focuses on the recent developments in COVID-19, precision medicine in acute myeloid leukemia and palliative care as a standard oncology treatment.</div>
        //     </a>
        //   </div>
        // </li>
        HtmlNodeFilters.allExceptSubtree(
            HtmlNodeFilters.tagWithAttribute("div", "class", "issues-page"),
            HtmlNodeFilters.tagWithTextRegex("li", LI_REGEX)
        ),

        // on article landing pages
        HtmlNodeFilters.tagWithAttribute("div", "id", "article-citations"),
        HtmlNodeFilters.tag("citation"),
    };

    return new HtmlFilterInputStream(in,
        encoding,
        HtmlNodeFilterTransform.exclude(new OrFilter(
            excludeFilters
        )));
  }
}
