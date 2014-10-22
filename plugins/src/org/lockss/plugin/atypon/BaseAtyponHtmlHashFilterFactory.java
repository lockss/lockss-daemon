/*
 * $Id: BaseAtyponHtmlHashFilterFactory.java,v 1.8 2014-10-22 21:51:12 alexandraohlson Exp $
 */

/*

Copyright (c) 2000-2013 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.plugin.atypon;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.util.Arrays;
import java.util.regex.Pattern;

import org.htmlparser.NodeFilter;
import org.htmlparser.Tag;
import org.htmlparser.Text;
import org.htmlparser.filters.*;
import org.htmlparser.tags.CompositeTag;
import org.htmlparser.tags.LinkTag;
import org.htmlparser.tags.Span;
import org.htmlparser.util.NodeList;
import org.htmlparser.util.ParserException;
import org.htmlparser.visitors.NodeVisitor;
import org.lockss.filter.FilterUtil;
import org.lockss.filter.WhiteSpaceFilter;
import org.lockss.filter.html.*;
import org.lockss.plugin.*;
import org.lockss.util.Logger;
import org.lockss.util.ReaderInputStream;

/**
 * BaseAtyponHtmlHashFilterFactory
 * The basic AtyponHtmlHashFilterFactory
 * Child plugins can extend this class and add publisher specific hash filters,
 * if necessary.  Common hashes can be easily added and be available to children.
 * Otherwise, this can be used by child plugins if no other hash filters are 
 * needed.
 */

public class BaseAtyponHtmlHashFilterFactory implements FilterFactory {
  Logger log = Logger.getLogger(BaseAtyponHtmlHashFilterFactory.class);
  // String used to see if text matches a size description of an article
  // usually "PDF Plus (527 KB)" or similar
  // (?i) makes it case insensitive
  private static final String SIZE_REGEX = "PDF\\s?(Plus)?\\s?\\(\\s?[0-9]+";
  private static final Pattern SIZE_PATTERN = Pattern.compile(SIZE_REGEX, Pattern.CASE_INSENSITIVE);  

  protected static NodeFilter[] baseAtyponFilters = new NodeFilter[] {
    // 7/22/2013 starting to use a more aggressive hashing policy-
    // these are on both issue and article pages
    // leave only the content
    HtmlNodeFilters.tagWithAttribute("div", "id", "header"),
    HtmlNodeFilters.tagWithAttribute("div", "id", "footer"),
    // filter out javascript
    new TagNameFilter("script"),
    //filter out comments
    HtmlNodeFilters.commentWithRegex(".*"),
    // crossref to site library
    HtmlNodeFilters.tagWithAttribute("a", "class", "sfxLink"),
    // stylesheets
    HtmlNodeFilters.tagWithAttribute("link", "rel", "stylesheet"),
    // these are only on issue toc pages
    HtmlNodeFilters.tagWithAttributeRegex("img", "class", "^accessIcon"),
    // Contains the changeable list of citations
    HtmlNodeFilters.tagWithAttribute("div", "class", "citedBySection"),
    // some size notes are within an identifying span
    // (see future science on an article page
    HtmlNodeFilters.tagWithAttribute("span", "class", "fileSize"),
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
            // some size notes are just text children of the link tag
            // eg <a ..> PDF Plus (123 kb)</a>
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

  /** Create an array of NodeFilters that combines the atyponBaseFilters with
   *  the given array
   *  @param nodes The array of NodeFilters to add
   */
  protected NodeFilter[] addTo(NodeFilter[] nodes) {
    NodeFilter[] result  = Arrays.copyOf(baseAtyponFilters, baseAtyponFilters.length + nodes.length);
    System.arraycopy(nodes, 0, result, baseAtyponFilters.length, nodes.length);
    return result;
  }
  
  /** Create a FilteredInputStream that excludes the the atyponBaseFilters
   * @param au  The archival unit
   * @param in  Incoming input stream
   * @param encoding  The encoding
   */
  public InputStream createFilteredInputStream(ArchivalUnit au,
      InputStream in, String encoding) {

    return new HtmlFilterInputStream(in, encoding,
        new HtmlCompoundTransform(HtmlNodeFilterTransform.exclude(new OrFilter(baseAtyponFilters)), xform));
  }
  
  /** Create a FilteredInputStream that excludes the the atyponBaseFilters and
   * moreNodes
   * @param au  The archival unit
   * @param in  Incoming input stream
   * @param encoding  The encoding
   * @param moreNodes An array of NodeFilters to be excluded with atyponBaseFilters
   */ 
  public InputStream createFilteredInputStream(ArchivalUnit au,
      InputStream in, String encoding, NodeFilter[] moreNodes) {
    NodeFilter[] bothFilters = addTo(moreNodes);
    return new HtmlFilterInputStream(in, encoding,
        new HtmlCompoundTransform(HtmlNodeFilterTransform.exclude(new OrFilter(bothFilters)), xform));
  }
  
  /** Create a FilteredInputStream that excludes the the atyponBaseFilters and
   * moreNodes as specified in the params, also do a WhiteSpace filter if boolean set
   * @param au  The archival unit
   * @param in  Incoming input stream
   * @param encoding  The encoding
   * @param moreNodes An array of NodeFilters to be excluded with atyponBaseFilters
   * @param doWhiteSpace A boolean to indicate if returned stream should also have  white spaces consolidated
   */ 
  public InputStream createFilteredInputStream(ArchivalUnit au,
      InputStream in, String encoding, NodeFilter[] moreNodes, boolean doWS) {
    NodeFilter[] bothFilters = addTo(moreNodes);
    
    InputStream combinedFiltered = new HtmlFilterInputStream(in, encoding,
        new HtmlCompoundTransform(HtmlNodeFilterTransform.exclude(new OrFilter(bothFilters)), xform));
    if (doWS == true) {
      Reader reader = FilterUtil.getReader(combinedFiltered, encoding);
      return new ReaderInputStream(new WhiteSpaceFilter(reader)); 
    } else { 
       return combinedFiltered;
    }
  }
  
  
  
}
