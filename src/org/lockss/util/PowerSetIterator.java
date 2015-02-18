/*
 * $Id$
 */
/*

Copyright (c) 2000-2012 Board of Trustees of Leland Stanford Jr. University,
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
 * A general purpose class to iterate over the power set of a set. This is the
 * set of all subsets of a set of elements, including the set itself and the
 * empty set.
 * The size of a powerset is 2^N.
 */
public class PowerSetIterator<T> implements Iterator<List<T>> {
  /**
   * The items list - items.size() = n
   */
  private List<T> items;

  /**
   * A mask for the items currently selected
   */
  BitSet mask;

  /**
   * The list of elements in the last set
   */
  List<T> result;

  /**
   * Constructor for new iterator which a takes a set of items from which
   * to generate subsets.
   * @param items the list of items
   */
  public PowerSetIterator(List<T> items)
  {
    if (items == null) {
      throw new IllegalArgumentException("items");
    }
    this.items = items;
    this.mask = new BitSet(items.size() + 1);
    result = new ArrayList<T>();
  }


  /**
   * This will return the number of sets in a power set
   * ie 2^N
   * @param N the number of elements in the list
   * @return the number of combinations.
   */
  static public long resultSize(final int N) {

    return (long)Math.pow(2, N);
  }

  public void reset()
  {
    result.clear();
    this.mask = new BitSet(items.size()+1);
  }

  /**
   * Returns true if the iteration has more elements ie a call to
   * next() would succeed.
   * @return boolean true iff the iterator has more elements.
   */
  public boolean hasNext() {
    return !mask.get(items.size());
  }

  /**
   * Returns the next element in the iteration. The resulting list is
   * recycled and will be modified on the next call to next()
   * @return List of the next set of elements
   * @throws NoSuchElementException if iteration has no more elements
   */
  public List<T> next() {
    if (!hasNext()) {
      throw new NoSuchElementException();
    }
    result.clear();
    for(int i = 0; i < items.size(); i++)
    {
        if(mask.get(i))  {
            result.add(items.get(i));
        }
    }

    //increment mask
    for(int i = 0; i < mask.size(); i++)
    {
      if(!mask.get(i)) {
        mask.set(i);
        break;
      }
      else {
        mask.clear(i);
      }
    }

    return result;
  }

  /**
   * Removes from the underlying collection the last element returned by the
   * collection.  The remove operation is not supported by this Iterator.
   * @throws UnsupportedOperationException
   */
  public void remove() {
    throw new UnsupportedOperationException();
  }

}
