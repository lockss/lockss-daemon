/*
 * $Id: HtmlNodeSequenceTransform.java,v 1.1 2006-07-31 06:47:26 tlipkis Exp $
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
 * An HtmlTransform that operates on sequences of nodes. */
public class HtmlNodeSequenceTransform implements HtmlTransform {
  private static Logger log = Logger.getLogger("HtmlNodeSequenceTransform");

  private NodeFilter startFilter;
  private NodeFilter endFilter;
  private boolean errorIfNoEndNode = false;

  /**
   * Create a HtmlNodeSequenceTransform that finds and removes sequences of
   * nodes beginning with a node that matches startFilter and ending with a
   * node that matches endFilter
   * @param startFilter matches the first node in the sequence
   * @param endFilter matches the last node in the sequence
   */
  public static
    HtmlNodeSequenceTransform excludeSequence(NodeFilter startFilter,
					      NodeFilter endFilter) {
    return new HtmlNodeSequenceTransform(startFilter, endFilter);
  }

  public HtmlNodeSequenceTransform(NodeFilter startFilter,
				   NodeFilter endFilter) {
    if (startFilter == null || endFilter == null) {
      throw new IllegalArgumentException("null filter");
    }
    this.startFilter = startFilter;
    this.endFilter = endFilter;
  }

  public NodeList transform(NodeList nl) throws IOException {

    outer:
    for (int sx = 0; sx < nl.size(); ) {
      Node snode = nl.elementAt(sx);
      if (startFilter.accept(snode)) {
	for (int ex = sx + 1; ex < nl.size(); ex++) {
	  Node enode = nl.elementAt(ex);
	  if (endFilter.accept(enode)) {
	    for (int rx = ex; rx >= sx; rx--) {
	      nl.remove(rx);
	    }
	    continue outer;
	  }
	}
	if (errorIfNoEndNode) {
	  throw new MissingEndNodeException("End node not found");
	}
      }
      NodeList children = snode.getChildren();
      if (children != null) {
	transform(children);
      }
      sx++;
    }
    return nl;
  }

  public void setErrorIfNoEndNode(boolean val) {
    errorIfNoEndNode = val;
  }   

  public static class MissingEndNodeException extends IOException {
    public MissingEndNodeException(String msg) {
      super(msg);
    }
  }
  
}
