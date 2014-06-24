/*
 * $Id: AllExceptSubtreeNodeFilter.java,v 1.2 2014-06-24 23:08:44 thib_gc Exp $
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

package org.lockss.plugin.silverchair;

import org.htmlparser.*;
import org.htmlparser.util.NodeList;
import org.lockss.filter.html.HtmlNodeFilters;

// FIXME 1.65
/**
 * When 1.65 is widely available, use
 * {@link HtmlNodeFilters#allExceptSubtree(NodeFilter, NodeFilter)}
 * (a.k.a. {@link HtmlNodeFilters.AllExceptSubtreeNodeFilter} instead.
 */
public class AllExceptSubtreeNodeFilter implements NodeFilter {

  protected NodeFilter rootNodeFilter;
  
  protected NodeFilter subtreeNodeFilter;
  
  public AllExceptSubtreeNodeFilter(NodeFilter rootNodeFilter,
                                 NodeFilter subtreeNodeFilter) {
    this.rootNodeFilter = rootNodeFilter;
    this.subtreeNodeFilter = subtreeNodeFilter;
  }

  @Override
  public boolean accept(Node node) {
    // Inspect ancestors
    for (Node current = node ; current != null ; current = current.getParent()) {
      if (subtreeNodeFilter.accept(current)) {
        return false; // 'node' is in the subtree: don't select
      }
      if (rootNodeFilter.accept(current)) { // 'node' is in under the root
        NodeList nl = new NodeList();
        node.collectInto(nl, subtreeNodeFilter);
        // If nl is empty, the subtree is not under 'node': select
        return nl.size() == 0;
      }
    }
    return false; // 'node' is not under the root: don't select
  }

}
