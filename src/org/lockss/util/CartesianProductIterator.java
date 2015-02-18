/*
 * $Id$
 */

/*

Copyright (c) 2000-2008 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.util;
import java.util.*;

/**
 * An iterator over the cartesian product of a collection of sets.  Each
 * call to {@link #next()} returns one combination as an array of objects.
 * <b>The same</b> array is modified and returned each time - it must be
 * used or copied before next() is called again  */

public class CartesianProductIterator implements Iterator {

  static List[] EMPTY_LIST_ARRAY = new List[0];

  List[] arrayOfSets;
  Object[] ret;
  int num;
  int index[];
  int left;
  

  /** Create an iterator over the cartesian product of the collection of
   * sets.  Sets are required to be lists to make processing easier */
  public CartesianProductIterator(Collection<List> listOfSets) {
    arrayOfSets = (List[])listOfSets.toArray(EMPTY_LIST_ARRAY);
    num = arrayOfSets.length;
    ret = new Object[num];
    index = new int[num];
    Arrays.fill(index, 0);
    index[num - 1] = -1;
    left = 1;
    for (List set : arrayOfSets) {
      left *= set.size();
    }
  }

  /** Return true if there are more permutations. */
  public boolean hasNext() {
    return left > 0;
  }

  public Object next() {
    left--;
    int start = (index[num - 1] >= 0) ? 0 : num - 1;
    for (int curPos = start; curPos < num; curPos++) {
      if (index[curPos] < arrayOfSets[curPos].size() - 1) {
	index[curPos]++;
	for (int copyPos = curPos; copyPos >= 0; copyPos--) {
	  ret[copyPos] = arrayOfSets[copyPos].get(index[copyPos]);
	}
	return ret;
      } else {
	index[curPos] = 0;
      }
    }
    throw new NoSuchElementException();
  }

  /**
   * Unsupported.
   * @throws UnsupportedOperationException
   */
  public void remove() {
    throw new UnsupportedOperationException();
  }
}
