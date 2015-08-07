/*
 * $Id$
 */

/*

Copyright (c) 2000-2015 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.plugin.atypon.futurescience;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.util.regex.Pattern;

import org.htmlparser.Node;
import org.htmlparser.NodeFilter;
import org.htmlparser.Remark;
import org.htmlparser.Tag;
import org.htmlparser.Text;
import org.htmlparser.filters.OrFilter;
import org.htmlparser.filters.TagNameFilter;
import org.htmlparser.tags.CompositeTag;
import org.htmlparser.tags.Div;
import org.htmlparser.tags.LinkTag;
import org.htmlparser.tags.Span;
import org.htmlparser.tags.TableColumn;
import org.htmlparser.util.NodeList;
import org.htmlparser.util.ParserException;
import org.htmlparser.visitors.NodeVisitor;
import org.lockss.daemon.PluginException;
import org.lockss.filter.FilterUtil;
import org.lockss.filter.HtmlTagFilter;
import org.lockss.filter.StringFilter;
import org.lockss.filter.WhiteSpaceFilter;
import org.lockss.filter.HtmlTagFilter.TagPair;
import org.lockss.filter.html.HtmlCompoundTransform;
import org.lockss.filter.html.HtmlFilterInputStream;
import org.lockss.filter.html.HtmlNodeFilterTransform;
import org.lockss.filter.html.HtmlNodeFilters;
import org.lockss.filter.html.HtmlTransform;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.plugin.FilterFactory;
import org.lockss.util.Logger;
import org.lockss.util.ReaderInputStream;

/* Don't extend BaseAtyponHtmlHashFilterFactory because 1) FutureScienceGroup is somewhat different
 * AND because we need to keep the comments for some context specific filtering and then
 * we can remove them at the end
 */
/*STANDALONE - DOES NOT INHERIT FROM BASE ATYPON */
public class FutureScienceHtmlHashFilterFactory implements FilterFactory {
  Logger log = Logger.getLogger(FutureScienceHtmlHashFilterFactory.class);
  // String used to see if text matches a size description of an article
  // usually "PDF Plus (527 KB)" or similar
  // (?i) makes it case insensitive
  private static final String SIZE_REGEX = "PDF\\s?(Plus)?\\s?\\(\\s?[0-9]+";
  private static final Pattern SIZE_PATTERN = Pattern.compile(SIZE_REGEX, Pattern.CASE_INSENSITIVE);  
  

  @Override
  public InputStream createFilteredInputStream(ArchivalUnit au,
      InputStream in,
      String encoding)
          throws PluginException {
    NodeFilter[] filters = new NodeFilter[] {

        //Stuff on both toc and article pages
        // Variable identifiers - institution doing the crawl  - including genSfxLinks() which is the institution button
        new TagNameFilter("script"),
        //reordering of the meta & link tags in the head
        new TagNameFilter("head"),
        // contains the institution banner on both TOC and article pages
        HtmlNodeFilters.tagWithAttribute("div", "class", "institutionBanner"),
        // welcome and login
        HtmlNodeFilters.tagWithAttribute("td", "class", "identitiesBar"),
        // footer at the bottom with copyright, etc.
        HtmlNodeFilters.tagWithAttribute("div", "class", "bottomContactInfo"),
        // footer at the bottom 
        HtmlNodeFilters.tagWithAttribute("div", "class", "bottomSiteMapLink"),
        // Left side columns has list of Journals (might change over time) and current year's catalog
        HtmlNodeFilters.tagWithAttribute("table", "class", "sideMenu mceItemTable"),
        // rss feed link can have variable text in it; exists both as "link" and "a"
        HtmlNodeFilters.tagWithAttributeRegex("link", "href", "feed=rss"),
        HtmlNodeFilters.tagWithAttributeRegex("a", "href", "feed=rss"),

        //TOC stuff
        HtmlNodeFilters.tagWithAttribute("td",  "class", "MarketingMessageArea"),
        HtmlNodeFilters.tagWithAttribute("table",  "class", "quickLinks"), 


        // article page - right side column - no identifier to remove entire column
        // but we can remove the specific contents
        HtmlNodeFilters.tagWithAttribute("td", "class", "quickLinks_content"),
        HtmlNodeFilters.tagWithAttribute("td", "class", "quickSearch_content"),

        // article pages (abstract, reference, full) have a "cited by" section which will change over time
        HtmlNodeFilters.tagWithAttribute("div", "class", "citedBySection"),

        // articles have a section "Users who read this also read..." which is tricky to isolate
        // It's a little scary, but <div class="full_text"> seems only to be used for this section (not to be confused with fulltext)
        // though I could verify that it is followed by <div class="header_divide"><h3>Users who read this article also read:</h3></div>
        HtmlNodeFilters.tagWithAttribute("div", "class", "full_text"),

        // Some article listings have a "free" glif. That change status over time, so remove the image
        //NOTE - there may still be an issue with extra spaces added when image is present
        HtmlNodeFilters.tagWithAttributeRegex("img", "src", "/images/free.gif$", true),
        // some size notes are within an identifying span
        // (see future science on an article page
        HtmlNodeFilters.tagWithAttribute("span", "class", "fileSize"),        

        new NodeFilter() {      
          // look for a <td> that has a comment <!-- placeholder id=null....--> child somewhere in it. If it's there remove it.
          @Override public boolean accept(Node node) {
            if (!(node instanceof TableColumn) && (!(node instanceof Div))) return false;
            // Add placeholder is in a comment which may have associated enclosing <td></td> if there is an ad
            // Look for <!-- placeholder id=null...--> comment and if there is one, remove the enclosing <td></td>
            if (node instanceof TableColumn) {
              Node childNode = node.getFirstChild();
              while (childNode != null) {
                if (childNode instanceof Remark) {
                  String remarkText = childNode.getText();
                  if ( (remarkText != null) && remarkText.contains("placeholder id=null") ) return true;
                }
                childNode = childNode.getNextSibling();
              }
              return false;
            } else {
               //TODO: (noted 8/6/2015) - expert_reviews is no longer on future-science, but now T&f...
              // remove this logic? verify that no AUs were collected from this platform first             
              // Expert Reviews puts copyright info in unmarked section but it is immediately preceeded by <!--contact info-->
              Node prevSib = node.getPreviousSibling();
              // there may be text nodes before the comment (for newlines)
              while ((prevSib != null) && (prevSib instanceof Text)) {
                prevSib = prevSib.getPreviousSibling();
              }
              if ((prevSib != null) && (prevSib instanceof Remark)) {
                String remarkText = prevSib.getText();
                if ( (remarkText != null) && remarkText.contains("contact info")) return true;
              }
              return false;
            }
          }
        }

    };
    HtmlTransform xform = new HtmlTransform() {
      //; The "id" attribute of <span> tags can have a gensym
        @Override
        public NodeList transform(NodeList nodeList) throws IOException {
          try {
            nodeList.visitAllNodesWith(new NodeVisitor() {
              @Override
              public void visitTag(Tag tag) {
                if (tag instanceof Span && tag.getAttribute("id") != null) {
                  tag.removeAttribute("id");
                } else if 
                (tag instanceof LinkTag && ((CompositeTag) tag).getChildCount() == 1 &&
                ((CompositeTag) tag).getFirstChild() instanceof Text) {
                  if (SIZE_PATTERN.matcher(((CompositeTag) tag).getStringText()).find()) {
                    log.debug3("removing link child text : " + ((CompositeTag) tag).getStringText());
                    ((CompositeTag) tag).removeChild(0);
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
    };   
    
    HtmlTransform xf1 = HtmlNodeFilterTransform.exclude(new OrFilter(filters));
    InputStream is1 = new HtmlFilterInputStream(in,
        encoding,new HtmlCompoundTransform(xf1, xform));
    
    Reader read1 = FilterUtil.getReader(is1, encoding);
    
    // remove all tags now that we're done filtering... in BaseAtypon this
    // would be done by turning on the appropriate flag, but we don't inherit.
    // note that this also removes the comments, so we no longer have to do that separately.
    
    // add a space before the tag "<", then remove from "<" to ">"
    Reader read2 = new HtmlTagFilter(new StringFilter(read1,"<", " <"), new TagPair("<",">"));
    // remove comments now that we're done filtering 
    //Reader read2 = HtmlTagFilter.makeNestedFilter(read1,
    //      ListUtil.list(new HtmlTagFilter.TagPair("<!--", "-->",true)));
    return new ReaderInputStream(new WhiteSpaceFilter(read2));
  }
  

}

