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

import java.math.*;
import java.util.*;

/**
 * A general purpose class to iterate over the  combination of n things
 * taken k at a time without repetitions: C(n,k).
 * Unlike permutations, there are no repetitions and the order is unimportant
 * The number of combinations can be calculated as n!/((n-r)! * (r!))
 */
public class CombinationIterator<T> implements Iterator<List<T>> {
  /**
   * The items list - items.size() = n
   */
  private List<T> items;
  /**
   * the number of items to combine at a time (k)
   */
  private int choose;
  /**
   * set when we have completed iterating over all combinations
   */
  private boolean finished;
  /**
   * the array of indices into items of the current k elements
   */
  private int[] current;

  /**
   * the list for results from each call of next
   */
  private List<T> result;

  /**
   * Constructor for a new iterator which takes the list of items and
   * the number of items to 'choose' from the list each time
   * @param items the list of items
   * @param choose the size of each set
   */
  public CombinationIterator(List<T> items, int choose) {
    if (items == null || items.size() == 0) {
      throw new IllegalArgumentException("items");
    }
    if (choose <= 0 || choose > items.size()) {
      throw new IllegalArgumentException("choose");
    }
    this.items = items;
    this.choose = choose;
    this.finished = false;
    this.result = new ArrayList<T>(choose);
  }

  /**
   * This will return the number of combinations for a
   * given C(N,K) i.e. n!/ ((n-r)! * (r!))
   * @param N the number of elements in the list
   * @param K the size of each set of combinations
   * @return the number of combinations.
   */
  static public BigInteger size(final int N, final int K) {
      BigInteger ret = BigInteger.ONE;
      for (int k = 0; k < K; k++) {
          ret = ret.multiply(BigInteger.valueOf(N-k))
                   .divide(BigInteger.valueOf(k + 1));
      }
      return ret;
  }

  /**
   * Returns true if the iteration has more elements ie a call to
   * next() would succeed.
   * @return boolean true iff the iterator has more elements.
   */
  public boolean hasNext() {
    return !finished;
  }

  /**
   * Returns the next element in the iteration. The resulting list is
   * recycled and will be modified on the next call to next()
   * @return List of the next combination of elements
   * @throws NoSuchElementException if iteration has no more elements
   */
  public List<T> next() {
    if (!hasNext()) {
      throw new NoSuchElementException();
    }

    if (current == null) {
      current = new int[choose];
      for (int i = 0; i < choose; i++) {
        current[i] = i;
      }
    }

    result.clear();
    for (int i = 0; i < choose; i++) {
      result.add(items.get(current[i]));
    }

    int n = items.size();
    finished = true;
    for (int i = choose - 1; i >= 0; i--) {
      if (current[i] < n - choose + i) {
        current[i]++;
        for (int j = i + 1; j < choose; j++) {
          current[j] = current[i] - i + j;
        }
        finished = false;
        break;
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
