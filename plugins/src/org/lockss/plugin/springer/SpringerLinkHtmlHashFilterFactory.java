/*
 * $Id$
 */

/*

Copyright (c) 2000-2012 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.plugin.springer;

import java.io.*;

import org.htmlparser.*;
import org.htmlparser.filters.*;
import org.htmlparser.tags.*;
import org.htmlparser.util.*;
import org.htmlparser.visitors.NodeVisitor;
import org.lockss.daemon.PluginException;
import org.lockss.filter.StringFilter;
import org.lockss.filter.WhiteSpaceFilter;
import org.lockss.filter.html.*;
import org.lockss.plugin.*;
import org.lockss.util.*;

public class SpringerLinkHtmlHashFilterFactory implements FilterFactory {

  protected static final Logger logger = Logger.getLogger(SpringerLinkHtmlHashFilterFactory.class);
  
  public static class FilteringException extends PluginException {
    public FilteringException() { super(); }
    public FilteringException(String msg, Throwable cause) { super(msg, cause); }
    public FilteringException(String msg) { super(msg); }
    public FilteringException(Throwable cause) { super(cause); }
  }
  
  @Override
  public InputStream createFilteredInputStream(ArchivalUnit au,
                                               InputStream in,
                                               String encoding)
      throws PluginException {
    // First filter with HtmlParser
    NodeFilter[] filters = new NodeFilter[] {
        /*
         * Crawl filter
         */
        // Contains cross-links to other articles in other journals/volumes
        HtmlNodeFilters.tagWithAttribute("div", "id", "ContentSecondary"),
        /*
         * Hash filter
         */
        // Contains ad-specific cookies and other variable content
        new TagNameFilter("script"),
        // Order of <meta> tags, <title> contents, etc.
        new TagNameFilter("head"),
        // Eventually changed from <h1 lang="en" class="title"> to <h1>
        new TagNameFilter("h1"),
        // Tiles around the main content, whose content gets re-arranged
        HtmlNodeFilters.tagWithAttribute("div", "id", "Header"),
        HtmlNodeFilters.tagWithAttribute("div", "id", "ContentHeading"),
        HtmlNodeFilters.tagWithAttribute("div", "id", "ContentToolbar"),
        HtmlNodeFilters.tagWithAttribute("div", "id", "ShareToolbar"),
        HtmlNodeFilters.tagWithAttribute("div", "id", "ContentFooter"),
        HtmlNodeFilters.tagWithAttribute("ul", "id", "Footer"),
        // Contains ads
        HtmlNodeFilters.tagWithAttribute("div", "class", "advertisement"),
        // Contains SFX links
        HtmlNodeFilters.tagWithAttributeRegex("div", "class", "linkoutView"),
        // Has a session cookie
        HtmlNodeFilters.tagWithAttribute("form", "id", "LoginForm"),
        // Spurious gensyms
        HtmlNodeFilters.tagWithAttributeRegex("link", "href", "^/dynamic-file\\.axd"),
        // CSS file names can be spuriously versioned
        HtmlNodeFilters.tagWithAttributeRegex("link", "href", "^/styles/"),
        // Icon names can be spuriously versioned
        HtmlNodeFilters.tagWithAttributeRegex("img", "src", "^/images/"),
        // Contains ASP state blob
        HtmlNodeFilters.tagWithAttribute("input", "id", "__VIEWSTATE"),
        HtmlNodeFilters.tagWithAttribute("input", "id", "__EVENTVALIDATION"),
        // Text includes number of reverse citations
        HtmlNodeFilters.tagWithAttributeRegex("a", "href", "/referrers/$"),
        
        // The end volume and year of a journal's coverage keeps moving forward
        // This needs to stay in for '/about' pages, it's not only in <div id="ContentSecondary">
        new NodeFilter() {
          @Override public boolean accept(Node node) {
            if (node instanceof DefinitionListBullet) {
              Tag tag = (Tag)node;
              if ("DD".equals(tag.getTagName())) {
                Node prevNode = tag.getPreviousSibling();
                while (prevNode != null && !(prevNode instanceof DefinitionListBullet)) {
                  prevNode = prevNode.getPreviousSibling();
                }
                if (prevNode != null && prevNode instanceof DefinitionListBullet) {
                  CompositeTag prevTag = (CompositeTag)prevNode;
                  return "Coverage".equals(prevTag.getStringText());
                }
              }
            }
            return false;
          }
        },
        
        // MAINTENANCE
        
        // Over time, <span class="...toolbarSprite..."></span>
        // became <a class="...toolbarSprite...">...</a>
        new NodeFilter() {
          @Override public boolean accept(Node node) {
            if (node instanceof Span || node instanceof LinkTag) {
              Tag tag = (Tag)node;
              String attr = tag.getAttribute("class");
              return (attr != null && attr.contains("toolbarSprite"));
            }
            return false;
          }
        },
        
        // The inline styling of this <div> has changed from
        // <div class="coverImage" title="Cover Image" style="background-image: url(...)">
        // to
        // <div class="coverImage" title="Cover Image" style="background-image: url(...); background-size: contain;">
        HtmlNodeFilters.tagWithAttribute("div", "class", "coverImage"),

    };
    
    HtmlTransform xform = new HtmlTransform() {
      @Override
      public NodeList transform(NodeList nodeList) throws IOException {
        try {
          nodeList.visitAllNodesWith(new NodeVisitor() {
            @Override
            public void visitTag(Tag tag) {
              if (tag instanceof FormTag) {
                // Top-level <form> tag's 'action' has been changing from words to numbers
                tag.removeAttribute("action");
              }
            }
          });
        }
        catch (ParserException pe) {
          logger.debug2("Internal error (parser)", pe);
        }
        return nodeList;
      }
    };
    
    InputStream filteredStream = new HtmlFilterInputStream(in,
                                                           encoding,
                                                           new HtmlCompoundTransform(HtmlNodeFilterTransform.exclude(new OrFilter(filters)),
                                                                                     xform));

    // Then apply Reader-based transformations
    try {
      // Turn into a Reader (can throw)
      Reader reader1 = new InputStreamReader(filteredStream, encoding);
      // Inconsistent use of "'" and "&#39;"
      Reader reader2 = new StringFilter(reader1, "&#39;", "'");
      // Noisy whitespace
      Reader reader3 = new WhiteSpaceFilter(reader2);
      // Convert back into an InputStream
      return new ReaderInputStream(reader3);
    }
    catch (UnsupportedEncodingException uee) {
      throw new FilteringException(uee);
    }
  }

}
