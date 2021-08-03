/*

Copyright (c) 2000-2021, Board of Trustees of Leland Stanford Jr. University
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

package uk.org.lockss.plugin.annualreviews;

import java.io.InputStream;

import org.htmlparser.filters.TagNameFilter;
import org.lockss.daemon.PluginException;
import org.lockss.filter.html.*;
import org.lockss.plugin.*;

public class AnnualReviewsHtmlFilterFactory implements FilterFactory {

  public InputStream createFilteredInputStream(ArchivalUnit au,
                                               InputStream in,
                                               String encoding)
      throws PluginException {
    HtmlTransform[] transforms = new HtmlTransform[] {
        // Filter out <select name="url">...</select>
        HtmlNodeFilterTransform.exclude(HtmlNodeFilters.tagWithAttribute("tr",
                                                                         "class",
                                                                         "identitiesBar")),
        // Filter out <div class="CitedBySectionContent">...</div>
        HtmlNodeFilterTransform.exclude(HtmlNodeFilters.tagWithAttribute("div",
                                                                         "class",
                                                                         "CitedBySectionContent")),
        // Filter out <table class="articleEntry">...</table>
        HtmlNodeFilterTransform.exclude(HtmlNodeFilters.tagWithAttribute("table",
                                                                         "class",
                                                                         "articleEntry")),
        // Filter out <link type="application/rss+xml">...</link>
        HtmlNodeFilterTransform.exclude(HtmlNodeFilters.tagWithAttribute("link",
                                                                         "type",
                                                                         "application/rss+xml")),
        // Filter out <a href="...">...</a> where the href value matches a regular exception
        HtmlNodeFilterTransform.exclude(HtmlNodeFilters.tagWithAttributeRegex("a",
                                                                              "href",
                                                                              "/action/showFeed\\?(.*&)?mi=")),
        // Filter out <a href="...">...</a> where the href value matches the incrementing trademark value
        HtmlNodeFilterTransform.exclude(HtmlNodeFilters.tagWithAttributeRegex("a",
                                                                              "href",
                                                                              "/about/trademark\\.aspx")),
        // Filter out <script>...</script>
        HtmlNodeFilterTransform.exclude(new TagNameFilter("script")),
    };
    return new HtmlFilterInputStream(in,
                                     encoding,
                                     new HtmlCompoundTransform(transforms));
  }

}
