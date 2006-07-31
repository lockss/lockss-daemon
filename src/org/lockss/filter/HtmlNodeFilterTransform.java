/*
 * $Id: HtmlNodeFilterTransform.java,v 1.1 2006-07-31 06:47:26 tlipkis Exp $
 */

/*

Copyright (c) 2000-2006 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.filter;

import java.io.*;
import java.util.List;

import org.htmlparser.*;
import org.htmlparser.lexer.*;
import org.htmlparser.filters.*;
import org.htmlparser.util.*;

import org.lockss.config.*;
import org.lockss.util.*;

/**
 * An HtmlTransform that applies a NodeFilter to each node in the parse
 * tree, either removing matching nodes or collecting only matching nodes
 * (into a NodeList) */

public class HtmlNodeFilterTransform implements HtmlTransform {
  private static Logger log = Logger.getLogger("HtmlNodeFilterTransform");

  private NodeFilter filter;
  private boolean exclude = false;

  /**
   * Create a HtmlNodeFilterTransform that excludes all nodes that match
   * the NodeFilter
   * @param filter nodes matching the filter will be removed
   */
  public static HtmlNodeFilterTransform exclude(NodeFilter filter) {
    return new HtmlNodeFilterTransform(new NotFilter(filter), true);
  }

  /**
   * Create a HtmlNodeFilterTransform that includes all nodes that match
   * the NodeFilter
   * @param filter nodes matching the filter will be returned in a new
   * NodeList
   */
  public static HtmlNodeFilterTransform include(NodeFilter filter) {
    return new HtmlNodeFilterTransform(filter, false);
  }

  private HtmlNodeFilterTransform(NodeFilter filter, boolean exclude) {
    if (filter == null) {
      throw new IllegalArgumentException("Called with null filter");
    }
    this.filter = filter;
    this.exclude = exclude;
  }

  public NodeList transform(NodeList nodeList) throws IOException {
    if (exclude) {
      nodeList.keepAllNodesThatMatch(filter, true);
    } else {
      nodeList = nodeList.extractAllNodesThatMatch(filter, true);
    }
    return nodeList;
  }
}
