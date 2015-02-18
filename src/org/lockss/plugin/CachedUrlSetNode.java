/*
 * $Id$
 */

/*

Copyright (c) 2000-2003 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.plugin;

/**
 * This interface a superclass for the CachedUrlSet and CachedUrl interfaces.
 * It simply provides a mechanism for easily getting the urls of lists of those
 * classes.
 */
public interface CachedUrlSetNode {
  /**
   * The type int for a CachedUrlSet.
   */
  public static final int TYPE_CACHED_URL_SET = 0;
  /**
   * The type int for a CachedUrl.
   */
  public static final int TYPE_CACHED_URL = 1;

  /**
   * Returns the url of this node.
   * @return the url
   */
  public String getUrl();

  /**
   * Returns an int representing the type of node.
   * @return the int
   */
  public int getType();

  /**
   * Returns true if the node has content.
   * @return true if content
   */
  public boolean hasContent();

  /**
   * Returns true if a leaf node.
   * @return true if a leaf
   */
  public boolean isLeaf();


}
