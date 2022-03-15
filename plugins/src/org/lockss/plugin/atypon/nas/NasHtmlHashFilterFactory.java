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

package org.lockss.plugin.atypon.nas;

import org.htmlparser.NodeFilter;
import org.lockss.filter.html.HtmlNodeFilters;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.plugin.atypon.BaseAtyponHtmlHashFilterFactory;
import org.lockss.util.Logger;

import java.io.InputStream;

public class NasHtmlHashFilterFactory extends BaseAtyponHtmlHashFilterFactory {
  protected static final Logger log = Logger.getLogger(NasHtmlHashFilterFactory.class);

  @Override
  public InputStream createFilteredInputStream(ArchivalUnit au,
                                               InputStream in,
                                               String encoding) {
    NodeFilter[] NasFilters = new NodeFilter[] {
      HtmlNodeFilters.tag("iframe"),
      // external links that may appear in different order, or not at all
      HtmlNodeFilters.tagWithAttribute("div", "class", "external-links"),
      HtmlNodeFilters.tagWithAttributeRegex("div", "class", "denial-block"),
      HtmlNodeFilters.tagWithAttributeRegex("div", "class", "available-formats"),
      // cached images with random ids
      // img src="/cms/10.1073/iti0122119/asset/0ea25029-06ce-4afb-8c81-3c6527f7e4b3/assets/images/large/iti0122119unfig03.jpg"
      // img src="/cms/asset/099477d1-3f71-4e50-84b5-bbbf4ed3cdfa/pnas.2022.119.issue-9.largecover.png"
      // div data-cover-src="/cms/asset/56597d67-7abd-41f4-a970-ff42ae8e0057/pnas.2022.119.issue-9.largecover.png" class="cover-image__popup-moving-cover position-fixed d-block"
      // div role="presentation" style="background: linear-gradient(180deg, #000000 0%, rgba(0, 0, 0, 0.74) 42.8%, rgba(0, 0, 0, 0) 84%), url(/cms/asset/3a6137bb-cbbb-465d-bd3f-32b6e37903f5/toc-banner.jpg) no-repeat; background-size: cover;" class="banner-widget__background"
      HtmlNodeFilters.tagWithAttributeRegex("img", "src", "/cms/.*asset"),
      HtmlNodeFilters.tagWithAttributeRegex("div", "data-cover-src", "/cms/.*asset"),
      HtmlNodeFilters.tagWithAttributeRegex("div", "style", "/cms/.*asset"),
      HtmlNodeFilters.tagWithAttributeRegex("a", "href", "/cms/.*asset"),
      // pubmed section has counter
      HtmlNodeFilters.tagWithAttribute("div", "class", "core-pmid"),
      // date and other stamps
      HtmlNodeFilters.tagWithAttributeRegex("span", "class", "card__meta__(date|badge)"),

    };

    // super.createFilteredInputStream adds NasFilters to the baseAtyponFilters
    // and returns the filtered input stream using an array of NodeFilters that
    // combine the two arrays of NodeFilters.
    return super.createFilteredInputStream(au, in, encoding, NasFilters);
  }

  // also do WhiteSpace filtering
  public boolean doWSFiltering() {
    return true;
  }
  // do the id removal for all tags
  public boolean doTagIDFiltering() {
    return true;
  }
}
