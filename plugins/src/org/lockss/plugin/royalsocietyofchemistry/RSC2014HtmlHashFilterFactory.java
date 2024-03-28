/*

Copyright (c) 2000-2023, Board of Trustees of Leland Stanford Jr. University

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

package org.lockss.plugin.royalsocietyofchemistry;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.util.Vector;

import org.htmlparser.Attribute;
import org.htmlparser.NodeFilter;
import org.htmlparser.Tag;
import org.htmlparser.filters.OrFilter;
import org.htmlparser.util.NodeList;
import org.htmlparser.util.ParserException;
import org.htmlparser.visitors.NodeVisitor;
import org.lockss.daemon.PluginException;
import org.lockss.filter.FilterUtil;
import org.lockss.filter.HtmlTagFilter;
import org.lockss.filter.StringFilter;
import org.lockss.filter.WhiteSpaceFilter;
import org.lockss.filter.HtmlTagFilter.TagPair;
import org.lockss.filter.html.*;
import org.lockss.plugin.*;
import org.lockss.util.Logger;
import org.lockss.util.ReaderInputStream;

public class RSC2014HtmlHashFilterFactory implements FilterFactory {
  
  private static final Logger log = Logger.getLogger(RSC2014HtmlHashFilterFactory.class);
  
  // Transform to remove attributes from html and other tags
  // some tag attributes changed based on IP or other status
  // some attributes changed over time, either arbitrarily or sequentially
  protected static HtmlTransform xform = new HtmlTransform() {
    @Override
    public NodeList transform(NodeList nodeList) throws IOException {
      try {
        nodeList.visitAllNodesWith(new NodeVisitor() {
          @Override
          public void visitTag(Tag tag) {
            String tagName = tag.getTagName().toLowerCase();
            try {
              if ("html".equals(tagName) ||
                  "span".equals(tagName) ||
                  "div".equals(tagName) ||
                  "h1".equals(tagName) ||
                  "h2".equals(tagName) ||
                  "h3".equals(tagName) ||
                  "a".equals(tagName)) {
                Attribute a = tag.getAttributeEx(tagName);
                Vector<Attribute> v = new Vector<Attribute>();
                v.add(a);
                if (tag.isEmptyXmlTag()) {
                  Attribute end = tag.getAttributeEx("/");
                  v.add(end);
                }
                tag.setAttributesEx(v);
              }
            }
            catch (Exception exc) {
              log.debug2("Internal error (visitor)", exc); // Ignore this tag and move on
            }
            // Always
            super.visitTag(tag);
          }
        });
      }
      catch (ParserException pe) {
        log.debug2("Internal error (parser)", pe); // Bail
      }
      return nodeList;
    }
  };
  
  @Override
  public InputStream createFilteredInputStream(ArchivalUnit au, InputStream in,
                                               String encoding)
      throws PluginException {
    // String gurl = au.getConfiguration().get("graphics_url");
    NodeFilter[] filters = new NodeFilter[] {
        // Contains the current year.
        HtmlNodeFilters.tagWithAttributeRegex("div", "class", "footer"),
        // remove header for minor changes in nav & breadcrumbs
        HtmlNodeFilters.tagWithAttribute("div", "class", "header"),
        // Changeable scripts
        HtmlNodeFilters.tag("script"),
        // remove head for metadata changes and JS/CSS version number
        HtmlNodeFilters.tag("head"),
        // <div id="top" class="navigation"  access links intermittent http://xlink.rsc.org/?doi=c3dt52391h
        HtmlNodeFilters.tagWithAttribute("div", "class", "navigation"),
        // Contains images that can change
        HtmlNodeFilters.tagWithAttributeRegex("img", "src", "https?://[^/]+/pubs-core/"),
        // remove abstract links which disappeared from content crawled more recently
        HtmlNodeFilters.tagWithAttribute("div", "class", "absract_links"),
        // more aggressive filtering, the next step would be to remove all remaining tags
        HtmlNodeFilters.tagWithAttribute("div", "class", "page_anchor"),
        HtmlNodeFilters.comment(),
        HtmlNodeFilters.tag("noscript"),
        // filter out potential login credential request
        HtmlNodeFilters.tagWithAttribute("a", "title", "Log in via your home Institution"),
        HtmlNodeFilters.tagWithAttribute("a", "title", "Log in with your member or subscriber username and password"),
        HtmlNodeFilters.tagWithText("div", "To gain access to this content please"),
    };
    
    InputStream filtered =  new HtmlFilterInputStream(in, encoding,
        new HtmlCompoundTransform(HtmlNodeFilterTransform.exclude(new OrFilter(filters)), xform));
    Reader filteredReader = FilterUtil.getReader(filtered, encoding);
    // add a space before the tag "<", then remove from "<" to ">"
    Reader addFilteredReader = new HtmlTagFilter(new StringFilter(filteredReader,"<", " <"), new TagPair("<",">"));
    return new ReaderInputStream(new WhiteSpaceFilter(addFilteredReader));
  }
  
}
