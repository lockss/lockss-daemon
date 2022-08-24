/*

Copyright (c) 2000-2022, Board of Trustees of Leland Stanford Jr. University

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

package org.lockss.plugin.springer.link;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;

import org.htmlparser.NodeFilter;
import org.htmlparser.Tag;
import org.htmlparser.Text;
import org.htmlparser.tags.CompositeTag;
import org.htmlparser.tags.Html;
import org.htmlparser.tags.LinkTag;
import org.htmlparser.util.NodeList;
import org.htmlparser.util.ParserException;
import org.htmlparser.visitors.NodeVisitor;
import org.lockss.filter.FilterUtil;
import org.lockss.filter.WhiteSpaceFilter;
import org.lockss.filter.html.HtmlFilterInputStream;
import org.lockss.filter.html.HtmlNodeFilterTransform;
import org.lockss.filter.html.HtmlCompoundTransform;
import org.lockss.filter.html.HtmlNodeFilters;
import org.lockss.filter.html.HtmlTransform;
import org.lockss.plugin.*;
import org.lockss.util.Logger;
import org.lockss.util.ReaderInputStream;
import org.htmlparser.filters.OrFilter;

public class SpringerLinkHtmlHashFilterFactory implements FilterFactory {

  private static final Logger log = Logger.getLogger(SpringerLinkHtmlHashFilterFactory.class);

  public InputStream createFilteredInputStream(ArchivalUnit au,
      InputStream in, String encoding) {

    HtmlFilterInputStream filteredStream = new HtmlFilterInputStream(in, encoding,
      new HtmlCompoundTransform(
      // Remove these parts first
      HtmlNodeFilterTransform.exclude(new OrFilter(new NodeFilter[] {
        // entire footer  (incl. ip address/logged in notifier)
        HtmlNodeFilters.tag("footer"),
        // article visits and other changeables
        HtmlNodeFilters.tagWithAttribute("div", "data-test", "article-metrics"),
        HtmlNodeFilters.tagWithAttribute("div", "id", "altmetric-container"),
        //adds on the side and top
        HtmlNodeFilters.tagWithAttributeRegex("aside", "class", "c-ad"),
        HtmlNodeFilters.tagWithAttribute("div", "class", "skyscraper-ad"),
        HtmlNodeFilters.tagWithAttribute("div", "class", "banner-advert"),
        HtmlNodeFilters.tagWithAttribute("div", "id", "doubleclick-ad"),
        // <p class="c-article-rights">
        // there is an additional param in the href of an a tag in this p tag.
        // copyright=National%20Society%20of%20Genetic%20Counselors%2C%20Inc
        // would be nice to normalize, or ignore the attribute, but as the p tag only contains the a tag
        // ignoring the whole thing shouldn't be a problem
        HtmlNodeFilters.tagWithAttribute("p", "class", "c-article-rights"),
        // <p class="c-article-access-provider__text" ....
        // this tag can either appear or not appear.
        HtmlNodeFilters.tagWithAttribute("p", "class", "c-article-access-provider__text"),
        HtmlNodeFilters.tagWithAttribute("p", "class", "c-article-metrics__explanation"),
        // <a ... id="ref-link-section-d95262e910">
        // this a tag's id seems to change every time, luckilly the ref-link-section bit is the same
        //HtmlNodeFilters.tagWithAttributeRegex("a", "id", "ref-link-section-"),
        // There is a <p> tag with no attributes, here is is.
        // <p>
        // Please try refreshing the page. If that doesn't work, please contact support so we can address the problem.</p>
        HtmlNodeFilters.tagWithTextRegex("p", "Please try refreshing the page\\. If that doesn't work, please contact support so we can address the problem\\."),
        // Similarly, there is this
        // <p> class="mb0" data-component='SpringerLinkArticleCollections'>
        //	We re sorry, something doesn't seem to be working properly.</p>
        HtmlNodeFilters.tagWithTextRegex("p", "re sorry, something doesn't seem to be working properly\\."),
      })),
      // now keep these
      HtmlNodeFilterTransform.include(new OrFilter(new NodeFilter[] {
          HtmlNodeFilters.tag("p"),
          HtmlNodeFilters.tag("h1")
      })),
      /*
      * many a tags in the articles have this id="ref-link-section-d82226e386" for an id attribute.
      */
      new HtmlTransform() {
        @Override
        public NodeList transform(NodeList nodeList) throws IOException {
          try {
            nodeList.visitAllNodesWith(new NodeVisitor() {
              @Override
              public void visitTag(Tag tag) {
                if ("a".equals(tag.getTagName().toLowerCase()) && tag.getAttribute("id") != null) {
                  if (tag.getAttribute("id").contains("ref-link-section")) {
                    tag.removeAttribute("id");
                  }
                }
              }
            });
          } catch (ParserException pe) {
            IOException ioe = new IOException();
            ioe.initCause(pe);
            throw ioe;
          }
          return nodeList;
        }
      }
    ));

    Reader filteredReader = FilterUtil.getReader(filteredStream, encoding);
    // Reader noTagFilter = new HtmlTagFilter(new StringFilter(filteredReader, "<", " <"), new TagPair("<", ">"));

    // Remove white space
    return new ReaderInputStream(new WhiteSpaceFilter(filteredReader));
  }
  
}
