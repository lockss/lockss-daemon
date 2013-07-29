/*
 * $Id: BaseAtyponHtmlHashFilterFactory.java,v 1.1 2013-07-29 22:23:47 aishizaki Exp $
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

import java.io.InputStream;
import java.util.Arrays;
import org.htmlparser.NodeFilter;
import org.htmlparser.filters.*;
import org.lockss.filter.html.*;
import org.lockss.plugin.*;

/**
 * BaseAtyponHtmlHashFilterFactory
 * The basic AtyponHtmlHashFilterFactory
 * Child plugins can extend this class and add publisher specific hash filters,
 * if necessary.  Common hashes can be easily added and be available to children.
 * Otherwise, this can be used by child plugins if no other hash filters are 
 * needed.
 */

public class BaseAtyponHtmlHashFilterFactory implements FilterFactory {
  protected static NodeFilter[] baseAtyponFilters = new NodeFilter[] {
    // 7/22/2013 starting to use a more aggressive hashing policy-
    // these are on both issue and article pages
    // leave only the content
    HtmlNodeFilters.tagWithAttribute("div", "id", "header"),
    HtmlNodeFilters.tagWithAttribute("div", "id", "footer"),
    // filter out javascript
    HtmlNodeFilters.tagWithAttribute("script", "type", "text/javascript"),
    HtmlNodeFilters.tagWithAttribute("link", "rel", "stylesheet"),
    // these are only on issue toc pages
    HtmlNodeFilters.tagWithAttribute("img", "class", "accessIcon"),
    // these are only on article pages
  };

  /** Create an array of NodeFilters that combines the atyponBaseFilters with
   *  the given array
   *  @param nodes The array of NodeFilters to add
   */
  private NodeFilter[] addTo(NodeFilter[] nodes) {
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
        HtmlNodeFilterTransform.exclude(new OrFilter(baseAtyponFilters)));
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
        HtmlNodeFilterTransform.exclude(new OrFilter(bothFilters)));
  }
}
