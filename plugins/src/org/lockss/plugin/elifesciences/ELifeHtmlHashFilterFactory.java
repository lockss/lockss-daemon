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

package org.lockss.plugin.elifesciences;

import java.io.IOException;
import java.io.InputStream;
import java.util.Vector;

import org.htmlparser.Attribute;
import org.htmlparser.Node;
import org.htmlparser.NodeFilter;
import org.htmlparser.Tag;
import org.htmlparser.filters.*;
import org.htmlparser.tags.Div;
import org.htmlparser.util.NodeList;
import org.htmlparser.util.ParserException;
import org.htmlparser.visitors.NodeVisitor;
import org.lockss.filter.html.*;
import org.lockss.plugin.*;
import org.lockss.util.Logger;

public class ELifeHtmlHashFilterFactory implements FilterFactory {
	
  private static final Logger log = Logger.getLogger(ELifeHtmlHashFilterFactory.class);
  
  // Transform to remove attributes from some tags
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
              if ("input".equals(tagName) ||
                  "body".equals(tagName) ||
                  "div".equals(tagName) ||
                  "p".equals(tagName) ||
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
  
  public InputStream createFilteredInputStream(ArchivalUnit au,
                                               InputStream in,
                                               String encoding) {
    NodeFilter[] filters = new NodeFilter[] {
     //filter out script, noscript
     HtmlNodeFilters.tag("script"),
     HtmlNodeFilters.tag("head"),
     HtmlNodeFilters.tag("video"),
     HtmlNodeFilters.tagWithAttribute("header", "class", "section-header"),
     HtmlNodeFilters.tagWithAttribute("header", "id", "section-header"),
     HtmlNodeFilters.tagWithAttribute("footer", "id", "section-footer"),
     // Do not hash responsive header (from crawl filter)
     HtmlNodeFilters.tagWithAttribute("div", "id", "region-responsive-header"),
     // this replaces references filter as references should not change
     HtmlNodeFilters.tagWithAttributeRegex("div", "class", "elife-reflink-links-wrapper"),
     // this was a source of over-crawl & can be simpler than (from crawl filter)
     HtmlNodeFilters.tagWithAttributeRegex("div", "class", "sidebar-wrapper"),
     // The next filter is not needed, we care about the correction for the hash
     // HtmlNodeFilters.tagWithAttributeRegex("div", "class", "elife-article-corrections"),
     HtmlNodeFilters.tagWithAttributeRegex("div", "class", "elife-article-(criticalrelation)"),
     // Decision-letter, author response & comments are dynamic and change
     //  http://elifesciences.org/content/3/e04094.full
     HtmlNodeFilters.tagWithAttribute("div", "id", "decision-letter"),
     HtmlNodeFilters.tagWithAttribute("div", "id", "author-response"),
     HtmlNodeFilters.tagWithAttribute("div", "id", "comments"),
     HtmlNodeFilters.tagWithAttribute("div", "id", "references"),
     // No relevant content in these headers
     HtmlNodeFilters.tagWithAttribute("div", "id", "zone-header-wrapper"),
     HtmlNodeFilters.tagWithAttribute("div", "class", "page_header"),
     HtmlNodeFilters.tagWithAttribute("ul", "class", "elife-article-categories"),
     // Remove from TOC 
     HtmlNodeFilters.tagWithAttributeRegex("div", "class", "form-item"),
     // Remove the changeable portion of "Comments" section
     HtmlNodeFilters.tagWithAttribute("div", "id", "disqus_thread"),
     // Found a Comments section div that did not have an id attribute of "comments"
     HtmlNodeFilters.tagWithAttribute("div", "class", "panel-separator"),
     new AndFilter(
         HtmlNodeFilters.tagWithAttributeRegex("div", "class", "ctools-collapsible-"),
         new NodeFilter() {
           @Override
           public boolean accept(Node node) {
             if (!(node instanceof Div)) return false;
             Node childNode = node.getFirstChild();
             while (childNode != null) {
               if (childNode instanceof Tag) {
                 if (((Tag) childNode).getTagName().equalsIgnoreCase("h2") && 
                     childNode.toPlainTextString().equalsIgnoreCase("comments")) {
                   return true;
                 }
               }
               childNode = childNode.getNextSibling();
             }
             return false;
           }
         }
         ),
     
    };
    InputStream filtered =  new HtmlFilterInputStream(in, encoding,
        new HtmlCompoundTransform(HtmlNodeFilterTransform.exclude(new OrFilter(filters)), xform));
    return filtered;
  }

}

