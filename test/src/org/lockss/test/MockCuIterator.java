/*
 * $Id$
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

package org.lockss.test;

import java.util.*;

import org.lockss.util.*;
import org.lockss.plugin.*;


/**
 * CiIterator that returns all the CachedUrls from an Iterator or Collection
 */
public class MockCuIterator extends CuIterator {
  static Logger log = Logger.getLogger("MockCuIterator");

  private Iterator cusIter;
  private CachedUrl nextElement = null;

  public MockCuIterator(Iterator cusIter) {
    this.cusIter = cusIter;
  }

  public MockCuIterator(Collection nodes) {
    this.cusIter = nodes.iterator();
  }

  public void remove() {
    throw new UnsupportedOperationException("Not implemented");
  }

  public boolean hasNext() {
    return findNextElement() != null;
  }

  public CachedUrl next() {
    CachedUrl element = findNextElement();
    nextElement = null;

    if (element != null) {
      return element;
    }
    throw new NoSuchElementException();
  }

  public int getExcludedCount() {
    return 0;
  }

  private CachedUrl findNextElement() {
    if (nextElement != null) {
      return nextElement;
    }
    while (cusIter.hasNext()) {
      Object obj = cusIter.next();
      if (obj instanceof CachedUrlSetNode) {
	CachedUrl cu = AuUtil.getCu((CachedUrlSetNode)obj);
	if (cu != null) {
	  nextElement = cu;
	  return cu;
	}
      } else {
	continue;
      }
    }
    return null;
  }
}
