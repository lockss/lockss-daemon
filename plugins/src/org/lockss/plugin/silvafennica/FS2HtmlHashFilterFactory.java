/*
 * $Id$
 */

/*

Copyright (c) 2018 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.plugin.silvafennica;

import java.io.*;

import org.apache.commons.io.input.CountingInputStream;
import org.htmlparser.Node;
import org.htmlparser.NodeFilter;
import org.htmlparser.Tag;
import org.htmlparser.filters.AndFilter;
import org.htmlparser.filters.OrFilter;
import org.htmlparser.util.ParserException;
import org.htmlparser.visitors.NodeVisitor;
//import org.htmlparser.Node;
//import org.htmlparser.tags.Span;
//import org.htmlparser.tags.ParagraphTag;
//import org.htmlparser.util.NodeList;
import org.lockss.daemon.PluginException;
import org.lockss.filter.*;
import org.lockss.filter.HtmlTagFilter.TagPair;
import org.lockss.filter.html.*;
import org.lockss.plugin.*;
import org.lockss.util.Logger;
import org.lockss.util.ReaderInputStream;


public class FS2HtmlHashFilterFactory implements FilterFactory {

  private static final Logger log = Logger.getLogger(FS2HtmlHashFilterFactory.class);
  
  protected static NodeFilter[] excludeFilters = new NodeFilter[] {
    //number of views is displayed so will need to be excluded since it is dynamic
        new AndFilter(HtmlNodeFilters.tagWithAttribute("p", "class", "abstract"), new NodeFilter(){
          @Override
          public boolean accept(Node node){
            if(!(node instanceof Tag)){
              return false;
            }
            class MyVisitor extends NodeVisitor {
              private boolean accepted = false;
              private boolean nested = false;

              @Override
              public void visitTag(Tag tag){
                if(HtmlNodeFilters.tagWithTextRegex("span", "^(Views|Katselukerrat)$").accept(tag)){
                  accepted = true;
                }
                if(HtmlNodeFilters.tagWithAttribute("p", "class", "abstract").accept(tag)){
                  nested = true;
                }
                super.visitTag(tag);
              }
            };
            MyVisitor visitor = new MyVisitor();
            try {
              node.getChildren().visitAllNodesWith(visitor);
              return (!visitor.nested && visitor.accepted);
            } catch (ParserException e) {
              // TODO Auto-generated catch block
              return false;
            }
          }
        })
      // DROP scripts, styles, comments
      /*HtmlNodeFilters.tag("script"),
      HtmlNodeFilters.tag("noscript"),
      HtmlNodeFilters.tag("style"),
      HtmlNodeFilters.comment(),
      HtmlNodeFilters.tagWithAttributeRegex("div", "class", "(header|menu|footer)"),
      HtmlNodeFilters.tagWithAttribute("div", "id", "header"),
      HtmlNodeFilters.tagWithAttribute("ul", "id", "address_list"),
      HtmlNodeFilters.tagWithAttribute("ul", "class", "highlights"),
      HtmlNodeFilters.tagWithAttribute("div", "id", "article_index"),
      HtmlNodeFilters.tagWithAttribute("p", "class", "references"),*/
      /*new NodeFilter() {
        @Override public boolean accept(Node node) {
          if (!(node instanceof Span)) return false;
          String allText = ((CompositeTag)node).toPlainTextString();
          
          if ((allText == null) || !(allText.contentEquals("Views")) ) return false;
          
          String classAttr = ((CompositeTag)node).getAttribute("class");
          // figure out if we meet either situation to warrant checking the text value
          if ((classAttr == null) || !(classAttr.contentEquals("abs-titles")) ) return false;
          
          // could be the Views count in the parent
          Node parentNode = node.getParent();
          if ( (parentNode != null) && (parentNode instanceof ParagraphTag) ) {
            parentNode.setChildren(null);
          }
          return false;
        }
      },*/
      // related content and metrics
  };
  
  protected static NodeFilter[] includeFilters = new NodeFilter[] {
      // TOC, only the bare minimum included for article counts
      HtmlNodeFilters.tagWithAttribute("div", "class", "article_list_item_section1"),
      // only one container for all articles seen
      // HtmlNodeFilters.tagWithAttributeRegex("div", "class", "column_2"),
      // HtmlNodeFilters.tagWithAttributeRegex("div", "id", "_idContainer"),
      HtmlNodeFilters.tag("h1"),
      HtmlNodeFilters.tag("h2"),
      HtmlNodeFilters.tagWithAttribute("p", "class", "body-text"),
      HtmlNodeFilters.tagWithAttribute("p", "class", "authors"),
      HtmlNodeFilters.tagWithAttribute("p", "class", "article-title"),
      HtmlNodeFilters.tagWithAttribute("p", "class", "abstract"),
      // https://www.silvafennica.fi/raw/1442
      HtmlNodeFilters.tagWithAttribute("div", "id", "raw_content"),
      // https://www.silvafennica.fi/large_tables/article1514_table1.html
      // could use <table class="article_table">, but would cause lots of duplication in the main article
      HtmlNodeFilters.tagWithAttribute("td", "class", "No-Table-Style table-caption"),
  };
  
  @Override
  public InputStream createFilteredInputStream(final ArchivalUnit au,
                                               InputStream in,
                                               String encoding)
      throws PluginException {
    
    /*
     * KEEP: throw out everything but main content areas
     * DROP: filter remaining content areas
     */
    HtmlCompoundTransform compoundTransform =
        new HtmlCompoundTransform(
            HtmlNodeFilterTransform.include(new OrFilter(includeFilters)),
            HtmlNodeFilterTransform.exclude(new OrFilter(excludeFilters)));

    InputStream filtered = new HtmlFilterInputStream(in, encoding, encoding, compoundTransform);
    Reader reader = FilterUtil.getReader(filtered, encoding);

    String[][] unifySpaces = new String[][] {
      // inconsistent use of utf8 whitespace - do this replacement first
      {"\u2009", " "},
      {"&nbsp;", " "},
      {"<", " <"},
    };
    Reader altSpaceFilter = StringFilter.makeNestedFilter(reader, unifySpaces, false);
    // Remove all inner tag content
    Reader noTagFilter = new HtmlTagFilter(altSpaceFilter, new TagPair("<", ">"));
    // Remove white space
    Reader whiteSpaceFilter = new WhiteSpaceFilter(noTagFilter);
    // for testing or development use altSpaceFilter in the call below
    InputStream ret = new ReaderInputStream(whiteSpaceFilter);
    // Instrumentation
    return new CountingInputStream(ret) {
      @Override
      public void close() throws IOException {
        long bytes = getByteCount();
        if (bytes <= 100L) {
          log.debug(String.format("%d byte%s in %s", bytes, bytes == 1L ? "" : "s", au.getName()));
        }
        if (log.isDebug2()) {
          log.debug2(String.format("%d byte%s in %s", bytes, bytes == 1L ? "" : "s", au.getName()));
        }
        super.close();
      }
    };
  }
  
}
