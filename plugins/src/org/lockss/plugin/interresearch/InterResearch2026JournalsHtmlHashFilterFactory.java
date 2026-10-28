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
import org.htmlparser.Tag;
import org.htmlparser.filters.OrFilter;
import org.htmlparser.filters.TagNameFilter;
import org.htmlparser.util.NodeList;
import org.htmlparser.util.ParserException;
import org.htmlparser.visitors.NodeVisitor;
import org.lockss.filter.FilterUtil;
import org.lockss.filter.WhiteSpaceFilter;
import org.lockss.filter.html.HtmlCompoundTransform;
import org.lockss.filter.html.HtmlFilterInputStream;
import org.lockss.filter.html.HtmlNodeFilterTransform;
import org.lockss.filter.html.HtmlNodeFilters;
import org.lockss.filter.html.HtmlTransform;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.plugin.FilterFactory;
import org.lockss.util.ReaderInputStream;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;

/**
 * Hash filter. Removes everything that changes between fetches or as the
 * site grows, so only article / table-of-contents content is compared.
 * Every block below was seen in input/captures/ (meps_article.html,
 * meps_volume774.html).
 */
public class InterResearch2026JournalsHtmlHashFilterFactory implements FilterFactory {

  /**
   * Volume page TOC entries carry
   *   data-sorting='{"pubdate":"2025-11-27",...,"openaccess":0}'
   * which flips when an article turns open access (MEPS is hybrid).
   * Excluding the whole <a> would drop the TOC itself, so only the
   * attribute is removed.
   */
  static final HtmlTransform STRIP_DATA_SORTING = new HtmlTransform() {
    @Override
    public NodeList transform(NodeList nodeList) throws IOException {
      try {
        nodeList.visitAllNodesWith(new NodeVisitor() {
          @Override
          public void visitTag(Tag tag) {
            if (tag.getAttribute("data-sorting") != null) {
              tag.removeAttribute("data-sorting");
            }
          }
        });
      } catch (ParserException pe) {
        throw new IOException(pe);
      }
      return nodeList;
    }
  };

  @Override
  public InputStream createFilteredInputStream(ArchivalUnit au,
                                               InputStream in,
                                               String encoding) {
    NodeFilter[] filters = new NodeFilter[] {

        // Boilerplate. <head> also holds the ?<timestamp> CSS/JS URLs that
        // change on every site redeploy, plus the TYPO3 comment.
        new TagNameFilter("head"),
        new TagNameFilter("header"),
        new TagNameFilter("footer"),
        new TagNameFilter("nav"),
        new TagNameFilter("script"),
        new TagNameFilter("noscript"),
        new TagNameFilter("style"),

        // Journal banner: "Impact Factor 2.3 (JCR 2026 release)",
        // "Total Annual Downloads 1.606.264 (2026)". Changes over time.
        // VERIFY: c3 is a TYPO3 content-element uid; same on both MEPS
        // captures, not yet checked on another journal.
        HtmlNodeFilters.tagWithAttribute("section", "id", "c3"),

        // Breadcrumb
        HtmlNodeFilters.tagWithAttribute("div", "id", "breadcrumb"),

        // Volume page: sidebar list of all volumes, grows with every new
        // volume. Article page: access label, PDF/cite/share box, Crossmark.
        new TagNameFilter("aside"),

        // "Other IR Journals" carousel (volume page)
        HtmlNodeFilters.tagWithAttributeRegex("div", "class", "\\bswiper\\b"),

        // Article page: "Cited by" (filled by JS; changes as citations arrive)
        HtmlNodeFilters.tagWithAttribute("div", "id", "Citations"),

        // Volume page: contact form
        HtmlNodeFilters.tagWithAttribute("div", "id", "contactform"),

        // Volume page, per TOC entry: <span class="article__access">[icon]
        // <span class="article__access-type">open access</span></span>.
        // Same open-access flip as data-sorting above.
        HtmlNodeFilters.tagWithAttribute("span", "class", "article__access"),
    };

    InputStream filteredStream = new HtmlFilterInputStream(in, encoding,
        new HtmlCompoundTransform(
            HtmlNodeFilterTransform.exclude(new OrFilter(filters)),
            STRIP_DATA_SORTING));
    Reader reader = FilterUtil.getReader(filteredStream, encoding);
    return new ReaderInputStream(new WhiteSpaceFilter(reader));
  }
}
