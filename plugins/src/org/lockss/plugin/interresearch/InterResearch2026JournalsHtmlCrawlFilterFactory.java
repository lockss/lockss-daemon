/*

Copyright (c) 2000-2026, Board of Trustees of Leland Stanford Jr. University

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

package org.lockss.plugin.interresearch;

import org.htmlparser.NodeFilter;
import org.htmlparser.filters.OrFilter;
import org.htmlparser.filters.TagNameFilter;
import org.lockss.filter.FilterUtil;
import org.lockss.filter.html.HtmlFilterInputStream;
import org.lockss.filter.html.HtmlNodeFilterTransform;
import org.lockss.filter.html.HtmlNodeFilters;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.plugin.FilterFactory;
import org.lockss.util.ReaderInputStream;

import java.io.InputStream;
import java.io.Reader;

/**
 * Crawl filter: affects link extraction only, not stored content.
 *
 * The main job is removing Previous/Next article links. Landing-page URLs
 * have no volume (/journals/meps/articles/meps14992), so the crawl rules
 * can't stop them, and they chain from article to article across the whole
 * journal. Articles must be reached only from this AU's volume page.
 *
 * Other volume pages (sidebar list, Previous/Next Volume) and other journals
 * are already excluded by the crawl rules; header/footer are removed only to
 * cut link noise.
 */
public class InterResearch2026JournalsHtmlCrawlFilterFactory implements FilterFactory {

  @Override
  public InputStream createFilteredInputStream(ArchivalUnit au,
                                               InputStream in,
                                               String encoding) {
    NodeFilter[] filters = new NodeFilter[] {

        // Site-wide navigation and footer menus
        new TagNameFilter("header"),
        new TagNameFilter("footer"),

        // getCompositeStringText() returns the raw inner HTML of the <a>, e.g.
        //   <svg ...>...</svg><span class="font-extrabold text-sm">Previous article</span>
        // so match the words as a whole text node: between '>' and '<'.
        HtmlNodeFilters.tagWithTextRegex("a",
                "(^|>)\\s*(Previous|Next)\\s+article\\s*(<|$)", true),

        // Breadcrumb (journal home, latest volume, this volume): nothing to collect
        HtmlNodeFilters.tagWithAttribute("div", "id", "breadcrumb"),
    };

    InputStream filteredStream = new HtmlFilterInputStream(in, encoding,
        HtmlNodeFilterTransform.exclude(new OrFilter(filters)));
    Reader reader = FilterUtil.getReader(filteredStream, encoding);
    return new ReaderInputStream(reader);
  }
}
