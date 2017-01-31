/*
 * $Id: $
 */

/*

Copyright (c) 2000-2016 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.plugin.springer.link;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.util.List;
import java.util.regex.Pattern;

import org.htmlparser.Node;
import org.htmlparser.NodeFilter;
import org.htmlparser.Tag;
import org.htmlparser.filters.*;
import org.htmlparser.tags.BodyTag;
import org.htmlparser.tags.CompositeTag;
import org.htmlparser.tags.Div;
import org.htmlparser.util.NodeList;
import org.htmlparser.util.ParserException;
import org.htmlparser.visitors.NodeVisitor;
import org.lockss.filter.FilterUtil;
import org.lockss.filter.HtmlTagFilter;
import org.lockss.filter.StringFilter;
import org.lockss.filter.WhiteSpaceFilter;
import org.lockss.filter.html.*;
import org.lockss.filter.html.HtmlTags.Section;
import org.lockss.plugin.*;
import org.lockss.util.ListUtil;
import org.lockss.util.ReaderInputStream;

public class SpringerLinkHtmlHashFilterFactory implements FilterFactory {
  /**
   * TODO - remove after 1.70 when the daemon recognizes this as an html composite tag
   */
	
  private static final Pattern IMPACT_PATTERN = Pattern.compile("impact factor", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE);
  public static class MyButtonTag extends CompositeTag {
    private static final String[] mIds = new String[] {"button"};
    public String[] getIds() { return mIds; }
  }
  
  private static final NodeFilter[] filters = new NodeFilter[] {
      HtmlNodeFilters.tag("script"),
      HtmlNodeFilters.tag("noscript"),
      HtmlNodeFilters.tag("input"),
      HtmlNodeFilters.tag("head"),
      HtmlNodeFilters.tag("aside"),
      HtmlNodeFilters.tag("footer"),
      // filter out comments
      HtmlNodeFilters.comment(),
      // all meta and link tags - some have links with names that change
      HtmlNodeFilters.tag("meta"),
      HtmlNodeFilters.tag("link"),

      //google iframes with weird ids
      HtmlNodeFilters.tag("iframe"),
      
      //footer
      HtmlNodeFilters.tagWithAttribute("div", "id", "footer"),
      HtmlNodeFilters.tagWithAttributeRegex("div", "class", "footer"),
      
      //more links to pdf and article
      HtmlNodeFilters.tagWithAttribute("div", "class", "bar-dock"),
      
      //adds on the side
      HtmlNodeFilters.tagWithAttribute("div", "class", "banner-advert"),
      HtmlNodeFilters.tagWithAttribute("div", "id", "doubleclick-ad"),
      
      //header and search box
      HtmlNodeFilters.tagWithAttribute("div", "id", "header"),
      HtmlNodeFilters.tagWithAttribute("div", "role", "banner"),
      
      //non essentials like metrics and related links
      HtmlNodeFilters.tagWithAttribute("div", "role", "complementary"),
      HtmlNodeFilters.tagWithAttribute("div", "class", "col-aside"),
      HtmlNodeFilters.tagWithAttribute("div", "class", "document-aside"),
      
      //random divs floating around
      HtmlNodeFilters.tagWithAttribute("div", "id", "MathJax_Message"),
      HtmlNodeFilters.tagWithAttribute("div", "id", "web-trekk-abstract"),
      HtmlNodeFilters.tagWithAttribute("div", "class", "look-inside-interrupt"),
      HtmlNodeFilters.tagWithAttribute("div", "id", "colorbox"),
      HtmlNodeFilters.tagWithAttribute("div", "id", "cboxOverlay"),
      HtmlNodeFilters.tagWithAttribute("div", "id", "gimme-satisfaction"),
      HtmlNodeFilters.tagWithAttribute("div", "class", "crossmark-tooltip"),
      HtmlNodeFilters.tagWithAttribute("div", "id", "crossMark"),
      HtmlNodeFilters.tagWithAttribute("div", "class", "banner"),
      
      HtmlNodeFilters.tagWithAttribute("p", "class", "skip-to-links"),

      // button - let's get rid of all of them...
      HtmlNodeFilters.tag("button"),
     /*class="StickySideButton_left StickySideButton_left--feedback"*/
      
      HtmlNodeFilters.allExceptSubtree(HtmlNodeFilters.tag("div"),
              new OrFilter(HtmlNodeFilters.tag("section"),
            		  HtmlNodeFilters.tag("p"))),
      
      new NodeFilter() {
          @Override public boolean accept(Node node) {
            if (!(node instanceof Section)) return false;
            if (!("features".equals(((CompositeTag)node).getAttribute("class")))) return false;
            String allText = ((CompositeTag)node).toPlainTextString();
            //using regex for case insensitive match on "Impact factor"
            // the "i" is for case insensitivity; the "s" is for accepting newlines
            return IMPACT_PATTERN.matcher(allText).matches();
            }
        }
  };
  
  HtmlTransform xform = new HtmlTransform() {
    @Override
    public NodeList transform(NodeList nodeList) throws IOException {
      try {
        nodeList.visitAllNodesWith(new NodeVisitor() {
          @Override
          // the <body '<body class="company XYZ" data-name="XYZ"'>
          // tag has changed to XYZ from abc- pruning those attributes
          //the "rel" attribute on link tags are using variable named values
          public void visitTag(Tag tag) {
            if (tag instanceof BodyTag && tag.getAttribute("class") != null) {
              tag.removeAttribute("class");
            }
            if (tag instanceof BodyTag && tag.getAttribute("data-name") != null) {
              tag.removeAttribute("data-name");
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


  public InputStream createFilteredInputStream(ArchivalUnit au,
      InputStream in, String encoding) {
    
    //HtmlFilterInputStream filteredStream = new HtmlFilterInputStream(in, encoding,
      //  HtmlNodeFilterTransform.exclude(new OrFilter(filters)));
    HtmlFilterInputStream filteredStream = new HtmlFilterInputStream(in, encoding,
      new HtmlCompoundTransform(HtmlNodeFilterTransform.exclude(new OrFilter(filters)), xform));
    filteredStream.registerTag(new MyButtonTag());
    Reader filteredReader = FilterUtil.getReader(filteredStream, encoding);
    Reader httpFilter = new StringFilter(filteredReader, "http:", "https:");
    
    // Remove white space
    return new ReaderInputStream(new WhiteSpaceFilter(httpFilter));
  }
  
}
