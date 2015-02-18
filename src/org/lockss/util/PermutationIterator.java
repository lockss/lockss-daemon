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

package org.lockss.util;
import java.util.*;
import java.io.*;
import java.lang.reflect.*;

/**
 * An iterator over arrays of permutations of integers.  Each call to
 * {@link #next()} returns <b>the same</b> array of n integers, in a new
 * permutation.
 */

public class PermutationIterator implements Iterator {
  int n;
  int indices[];
  int next[];
  int nleft;
  boolean first;

  /** Create a PermutationIterator that will iterate over permutations of
   * the first n integers (0 - n-1). */
  public PermutationIterator(int n) {
    this.n = n;
    indices = new int[n];
    for (int ix = 0; ix < n; ix++) {
      indices[ix] = ix;
    }
    nleft = n == 0 ? 0 : factorial(n);
    first = true;
  }

  /** Return true if there are more permutations. */
  public boolean hasNext() {
    return nleft > 0;
  }

  /** Return an int array (<code>int[]</code>) containing the next
   * permutation.  <b>Note: the same array is returned each time, so must
   * be copied if the permutation is needed after the next call to
   * <code>next()</code></b>. */
  public Object next() {
    if (!hasNext()) {
      throw new NoSuchElementException();
    }
    nleft--;
    if (first) {
      first = false;
    } else {
      nextPerm();
    }
    return indices;
  }

  /**
   * Unsupported.
   * @throws UnsupportedOperationException
   */
  public void remove() {
    throw new UnsupportedOperationException();
  }

  void nextPerm() {
    int i = n - 1;

    while (indices[i-1] >= indices[i]) {
      i--;
    }
    int j = n;
    while (indices[j-1] <= indices[i-1]) {
      j--;
    }
    swap(i-1, j-1);

    i++;
    j = n;

    while (i < j) {
      swap(i-1, j-1);
      i++;
      j--;
    }
  }

  void swap(int a, int b) {
    int tmp = indices[a];
    indices[a] = indices[b];
    indices[b] = tmp;
  }

  static int factorial(int n) {
    int fact = 1;
    if (n > 1) {
      for (int i = 1; i <= n; i++) {
	fact *= i;
      }
    }
    return fact;
  }

}
