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

package org.lockss.plugin.georgthiemeverlag;

import java.io.*;
import java.util.Vector;

import org.htmlparser.*;
import org.htmlparser.filters.*;
import org.htmlparser.util.NodeList;
import org.htmlparser.util.ParserException;
import org.htmlparser.visitors.NodeVisitor;
import org.lockss.daemon.PluginException;
import org.lockss.filter.FilterUtil;
import org.lockss.filter.WhiteSpaceFilter;
import org.lockss.filter.html.*;
import org.lockss.plugin.*;
import org.lockss.util.Logger;
import org.lockss.util.ReaderInputStream;

public class GeorgThiemeVerlagHtmlFilterFactory implements FilterFactory {
  
  Logger log = Logger.getLogger(GeorgThiemeVerlagHtmlFilterFactory.class);
  
  @Override
  public InputStream createFilteredInputStream(ArchivalUnit au,
                                               InputStream in,
                                               String encoding)
      throws PluginException {
    // First filter with HtmlParser
    NodeFilter[] filters = new NodeFilter[] {
        // Aggressive filtering of non-content tags
        // Contains scripts and tags that change values, do not contain content
        // head & scipt tags contents might change, aggressive filtering
        HtmlNodeFilters.tag("head"),
        HtmlNodeFilters.tag("script"),
        // Remove header/footer items
        HtmlNodeFilters.tag("header"),
        HtmlNodeFilters.tag("footer"),
        // remove ALL comments
        HtmlNodeFilters.comment(),
        // Contains ads that change, not content
        HtmlNodeFilters.tagWithAttributeRegex("div", "id", "adSidebar"),
        // Contains navigation items that are not content
        HtmlNodeFilters.tagWithAttribute("div", "id", "navPanel"),
        HtmlNodeFilters.tagWithAttribute("ul", "id", "overviewNavigation"),
        // Contains functional links, not content
        HtmlNodeFilters.tagWithAttribute("div", "class", "pageFunctions"),
        HtmlNodeFilters.tagWithAttribute("div", "class", "articleFunctions"),
        HtmlNodeFilters.tagWithAttribute("span", "class", "articleCategories"),
        // Contains non-functional anchor, not content
        HtmlNodeFilters.tagWithAttribute("ul", "class", "articleTocList"),
        HtmlNodeFilters.tagWithAttribute("a", "name"),
        // contains information, not content
        HtmlNodeFilters.tagWithAttribute("div", "id", "access-profile-box"),
        // Debug ids change
        HtmlNodeFilters.tagWithAttributeRegex("img", "src", "_debugResources="),
    };
    
    
    // HTML transform to remove generated href attribute like <a href="#N66454">
    HtmlTransform xform = new HtmlTransform() {
      @Override
      public NodeList transform(NodeList nodeList) throws IOException {
        try {
          nodeList.visitAllNodesWith(new NodeVisitor() {
            @Override
            public void visitTag(Tag tag) {
              String tagName = tag.getTagName().toLowerCase();
              try {
                if ("a".equals(tagName) ||
                    "div".equals(tagName) ||
                    "section".equals(tagName)) {
                  Attribute a = tag.getAttributeEx(tagName);
                  Vector<Attribute> v = new Vector<Attribute>();
                  v.add(a);
                  if (tag.isEmptyXmlTag()) {
                    Attribute end = tag.getAttributeEx("/");
                    v.add(end);
                  }
                  tag.setAttributesEx(v);
                }
                super.visitTag(tag);
              }
              catch (Exception exc) {
                log.debug2("Internal error (visitor)", exc); // Ignore this tag and move on
              }
            }
          });
        }
        catch (ParserException pe) {
          log.debug2("Internal error (parser)", pe); // Bail
        }
        return nodeList;
      }
    };
    
    HtmlFilterInputStream filtered = new HtmlFilterInputStream(in, encoding,
        new HtmlCompoundTransform(HtmlNodeFilterTransform.exclude(
            new OrFilter(filters)),xform));
    Reader filteredReader = FilterUtil.getReader(filtered, encoding);
    return new ReaderInputStream(new WhiteSpaceFilter(filteredReader));
  }
  
}
