/*
 * $Id: BioOneAtyponHtmlHashFilterFactory.java,v 1.5 2014-10-22 21:51:12 alexandraohlson Exp $
 */

/*

Copyright (c) 2000-2014 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.plugin.bioone;

import java.io.*;
import org.htmlparser.*;
import org.htmlparser.filters.*;
import org.htmlparser.nodes.TagNode;
import org.htmlparser.tags.Div;
import org.htmlparser.tags.LinkTag;
import org.htmlparser.tags.Span;
import org.htmlparser.util.*;
import org.htmlparser.visitors.NodeVisitor;
import org.lockss.daemon.PluginException;
import org.lockss.filter.*;
import org.lockss.filter.html.*;
import org.lockss.plugin.*;
import org.lockss.util.*;

/*STANDALONE - DOES NOT INHERIT FROM BASE ATYPON */
public class BioOneAtyponHtmlHashFilterFactory implements FilterFactory {
  private static final String refNodeClassLabel = "refnumber";

  @Override
  public InputStream createFilteredInputStream(ArchivalUnit au, InputStream in,
      String encoding)
          throws PluginException {
    // First filter with HtmlParser
    NodeFilter[] filters = new NodeFilter[] {
        /*
         * Crawl filter
         */
        // Contains most-read articles in the same journal (etc.)
        HtmlNodeFilters.tagWithAttribute("div", "class", "relatedContent"),
        // Contains related articles (etc.)
        HtmlNodeFilters.tagWithAttribute("div", "id", "relatedArticleSearch"),
        // Contains reverse citations
        HtmlNodeFilters.tagWithAttributeRegex("div", "id", "citingArticles"),
        // Contains reverse citations
        HtmlNodeFilters.tagWithAttribute("div", "class", "citedBySection"),
        HtmlNodeFilters.tagWithAttribute("div",  "id", "titleTools"),
        /*
         * Hash filter
         */
        //implementing maximal filtering concept - leaving in legacy filters for safety
        HtmlNodeFilters.tagWithAttribute("div", "id", "header"),
        HtmlNodeFilters.tagWithAttribute("div", "id", "mainFooter"),
        HtmlNodeFilters.tagWithAttribute("div", "id", "goog-gt-tt"),
        HtmlNodeFilters.tagWithAttribute("div", "id", "contentNav"),
        HtmlNodeFilters.tagWithAttribute("div", "id", "google_translate_element"),
        HtmlNodeFilters.tagWithAttribute("div", "id", "contentSidebar"),
        new TagNameFilter("script"), //javascript
        HtmlNodeFilters.commentWithRegex(".*"), //comments
        HtmlNodeFilters.tagWithAttribute("link", "rel", "stylesheet"), //stylesheets
        HtmlNodeFilters.tagWithAttribute("img", "class", "accessIcon"), //free or restricted   
        HtmlNodeFilters.tagWithAttribute("div", "class", "gWidgetContainer"), //google widget stuff

        // Contains site-specific SFX code
        new TagNameFilter("script"),
        // Contains recent impact factors and journal rankings
        HtmlNodeFilters.tagWithAttribute("div", "id", "articleInfoBox"),
        // Contains site-specific SFX markup
        HtmlNodeFilters.tagWithAttribute("a", "class", "sfxLink"),
        // Contains institution-specific markup
        HtmlNodeFilters.tagWithAttribute("div", "id", "headerLogo"),

        // when the <a href=> tag is a child or grandchild of a <div class="refnumber"> remove it
        // both because the number of links to reference links could change and because
        // the link arguments, especially 'tollfreelink' argument changes over time
        new NodeFilter() {
          @Override public boolean accept(Node node) {
            if (!(node instanceof LinkTag)) return false;
            Node linkParent = node.getParent();
            Node gParent = (linkParent != null) ? linkParent.getParent() : null;
            if ((linkParent instanceof Div && (refNodeClassLabel.equals(((TagNode) linkParent).getAttribute("class")))) ||
                (gParent != null && (refNodeClassLabel.equals(((TagNode) gParent).getAttribute("class")))) ) {
              return true;
            } 
            return false;
          }
        },
    };
    HtmlTransform xf1 = HtmlNodeFilterTransform.exclude(new OrFilter(filters));

    //; The "id" attribute of <span> tags can have a gensym
    HtmlTransform xf2 = new HtmlTransform() {
      @Override
      public NodeList transform(NodeList nodeList) throws IOException {
        try {
          nodeList.visitAllNodesWith(new NodeVisitor() {
            @Override
            public void visitTag(Tag tag) {
              if (tag instanceof Span && tag.getAttribute("id") != null) {
                tag.removeAttribute("id");
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
    };

    InputStream is1 = new HtmlFilterInputStream(in,
        encoding,
        new HtmlCompoundTransform(xf1, xf2));

    Reader read1 = FilterUtil.getReader(is1, encoding);
    Reader read2 = HtmlTagFilter.makeNestedFilter(read1,
        ListUtil.list(// Debug output (or similar)
        new HtmlTagFilter.TagPair("<!--totalCount",
            "-->",
            true),
            // Time stamp (or similar)
            new HtmlTagFilter.TagPair("<!--modified:",
                "-->",
                true)));
    return new ReaderInputStream(read2);
  }

}
