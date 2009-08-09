/*
 * $Id: CrawlUrlData.java,v 1.3 2009-08-09 07:38:50 tlipkis Exp $
 */

/*

Copyright (c) 2000-2009 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.crawler;
import java.util.*;

/**
 * Represents a URL and the minimum depth at which we encountered it in the
 * crawl.  If supplied with links {@link #addChild(CrawlUrlData)} will traverse
 * graph whenever a node's depth is reduced, maintaining minimum depth info
 * and optionally calling a handler for any node whose depth is reduced.
 */
public class CrawlUrlData implements CrawlUrl {
  public static final int IS_FETCHED = 1;
  public static final int IS_FAILED_FETCH = 2;
  public static final int IS_FAILED_PARSE = 4;

  private final String url;
  private int depth;
  private int flags;
  private ArrayList<CrawlUrlData> children;

  public CrawlUrlData(String url, int depth) {
    if (url == null) throw new NullPointerException();
    if (depth < 0) throw new IllegalArgumentException();
    this.url = url;
    this.depth = depth;
  }

  /** Return the URL */
  public String getUrl() {
    return url;
  }

  /** Return the minimum depth at which this URL has been seen */
  public int getDepth() {
    return depth;
  }

  public boolean isFetched() {
    return (flags & IS_FETCHED) != 0;
  }

  public void setFetched(boolean val) {
    if (val) {
      flags |= IS_FETCHED;
    } else {
      flags &= ~IS_FETCHED;
    }
  }

  public boolean isFailedFetch() {
    return (flags & IS_FAILED_FETCH) != 0;
  }

  public void setFailedFetch(boolean val) {
    if (val) {
      flags |= IS_FAILED_FETCH;
    } else {
      flags &= ~IS_FAILED_FETCH;
    }
  }

  public boolean isFailedParse() {
    return (flags & IS_FAILED_PARSE) != 0;
  }

  public void setFailedParse(boolean val) {
    if (val) {
      flags |= IS_FAILED_PARSE;
    } else {
      flags &= ~IS_FAILED_PARSE;
    }
  }

  /** If this is a new minimum depth, record it and return true, else
   * return false */
  public boolean encounteredAtDepth(int n) {
    return encounteredAtDepth(n, null);
  }

  /** If this is a new minimum depth, record it and return true, else
   * return false. Call the ReducedDepthHandler on this or any descendant
   * whose depth is reduced. */
  private boolean encounteredAtDepth(int n, ReducedDepthHandler rdh) {
    if (n < 0) throw new IllegalArgumentException();
    if (n < depth) {
      int olddepth = depth;
      depth = n;
      if (children != null) {
	for (CrawlUrlData child : children) {
	  child.encounteredAtDepth(depth + 1, rdh);
	}
      }
      if (rdh != null) {
	rdh.depthReduced(this, olddepth, n);
      }
      return true;
    }
    return false;
  }

  /** Add a child node.  If the child's depth is reduced and it has
   * children, reduce their depths as necessary */
  public void addChild(CrawlUrlData child) {
    addChild(child, null);
  }

  /** Add a child node.  If the child's depth is reduced and it has
   * children, reduce their depths as necessary, notifying the
   * ReducedDepthHandler */
  public void addChild(CrawlUrlData child, ReducedDepthHandler rdh) {
    if (children == null) {
      children = new ArrayList<CrawlUrlData>();
    }
    children.add(child);
    child.encounteredAtDepth(depth + 1, rdh);
  }

  /** When finished updating the child list, converts it into a more
   * storage-efficient structure */
  public void trimChildren() {
    if (children != null) {
      children.trimToSize();
    }
  }

  // used only by unit tests
  boolean isChild(CrawlUrlData curl) {
    return children != null && children.contains(curl);
  }

  /** Reinitialize the child list */
  public void clearChildren() {
    children = null;
  }

  /** For unit test */
  public int numChildren() {
    return children == null ? 0 : children.size();
  }

  public String toString() {
    return "[curl: " + getDepth() + ", " + getUrl() + "]";
  }

  public interface ReducedDepthHandler {
    public void depthReduced(CrawlUrlData curl, int oldDepth, int newDepth);
  }
}
