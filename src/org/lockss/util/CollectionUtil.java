/*
 * $Id: CollectionUtil.java,v 1.16 2008-10-02 06:50:01 tlipkis Exp $
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

/** Utilities for Collections.
 */
public class CollectionUtil {
  /** An immutable empty iterator.  Calling next() throws
   * NoSuchElementException, remove() throws
   * UnsupportedOperationException */
  // Somewhat convoluted way to achieve the above behavior.
  public static final Iterator EMPTY_ITERATOR =
    Collections.unmodifiableList(Collections.EMPTY_LIST).iterator();

  /** Return true iff the two Collections are disjoint */
  public static boolean isDisjoint(Collection a, Collection b) {
    return !org.apache.commons.collections.CollectionUtils.containsAny(a, b);
  }

  /** Return true iff the two iterators enumerate collections of the same
   * size, whose elements are pairwise equal.  The collections need not be of
   * the same type.
   */
  public static boolean isIsomorphic(Iterator a, Iterator b) {
    while (a.hasNext()) {
      if (!b.hasNext()) {
	return false;
      }
      Object ao = a.next();
      Object bo = b.next();
      if (ao == null) {
	if (bo != null) {
	  return false;
	}
      } else if (!ao.equals(bo)) {
	return false;
      }
    }
    return !b.hasNext();
  }

  /** Return true iff the two collections are the same size and have
   * elements that are pairwise equal.  The collections need not be of the
   * same type.
   */
  public static boolean isIsomorphic(Collection a, Collection b) {
    if (a == null) {
      return b == null;
    }
    // Sigh, can't rely on overloading to catch Sets due to static typing.
    // (E.g., we're likely to be called from assertIsomorphic())
    if (a instanceof Set && b instanceof Set) {
      return ((Set)a).equals(b);
    }
    // Similarly.
    if (!isCompatibleTypes(a, b)) {
      return false;
    }
    return isIsomorphic(a.iterator(), b.iterator());
  }

  /** Return true iff the collection and array are isomorphic */
  public static boolean isIsomorphic(Collection a, Object b[]) {
    return isIsomorphic(a.iterator(), new ArrayIterator(b));
  }

  /** Return true iff the array and collection are isomorphic */
  public static boolean isIsomorphic(Object a[], Collection b) {
    return isIsomorphic(new ArrayIterator(a), b.iterator());
  }

  /** Return true iff the two arrays are isomorphic */
  public static boolean isIsomorphic(Object a[], Object b[]) {
    return isIsomorphic(new ArrayIterator(a), new ArrayIterator(b));
  }

  // Check that the two collection types can possibly be equal.
  // Ordered vs. unordered (e.g., Set) can't be.
  private static boolean isCompatibleTypes(Collection a, Collection b) {
    return isOrdered(a) == isOrdered(b);
  }

  private static boolean isOrdered(Collection coll) {
    return coll instanceof List || coll instanceof SortedSet;
  }

  /**
   * Removes and returns an arbitrary element from the collection
   */
  public static Object removeElement(Collection coll) {
    Object ele = getAnElement(coll);
    coll.remove(ele);
    return ele;
  }

  /**
   * Returns an arbitrary element from the collection
   */
  public static Object getAnElement(Collection coll) {
    if (coll == null) {
      throw new IllegalArgumentException("Called with null collection");
    } else if (coll.size() == 0) {
      return null;
    }
    Iterator it = coll.iterator();
    return it.next();
  }

  private static LockssRandom random = new LockssRandom();

  /**
   * Randomly select <i>count</i> entries evenly distributed from
   * collection <i>c</i> and return them in a random order.  If
   * <i>count</i> is equal to <code>c.size()</code>, returns a random
   * permutation of <i>c</i>
   *
   * @param c The collection from which to select entries.
   * @param count The number of items to return.
   * @return A collection of items selected randomly from the list.
   * @throws IllegalArgumentException if count is non-positive or
   *         greater than the size of the collection.
   */
  public static List randomSelection(Collection c, int count) {
    int choiceSize = c.size();
    if (count < 0 || count > choiceSize) {
      throw new IllegalArgumentException("'count' negative "+
                                         "or greater than collection size.");
    }
    if (count == 0) {
      return Collections.EMPTY_LIST;
    }
    Object[] arr = c.toArray();
    ArrayList result = new ArrayList(count);
    while (--count > 0) {
      int idx = random.nextInt(choiceSize);
      result.add(arr[idx]);
      arr[idx] = arr[choiceSize - 1]; // Replace chosen item with last item.
      choiceSize--;
    }
    result.add(arr[0]);
    return result;
  }

  /**
   * Randomly permute a collection
   * @param c The collection to permute
   * @return a permuted collection
   */
  public static List randomPermutation(Collection c) {
    return randomSelection(c, c.size());
  }

  /**
   * Randomly select one item from a list.
   *
   * @param c The list from which to select an item.
   * @return An item randomly selcted from the list.
   */
  public static Object randomSelection(List c) {
    return c.get(random.nextInt(c.size()));
  }

  /**
   * Randomly select one item from a collection.  It is more efficient to
   * use {@link #randomSelection(List)} instead.
   *
   * @param c The collection from which to select an item.
   * @return An item randomly selcted from the collection.
   */
  public static Object randomSelection(Collection c) {
    int idx = random.nextInt(c.size());
    Iterator iter = c.iterator();
    while (--idx >= 0) {
      iter.next();
    }
    return iter.next();
  }

  /**
   * Randomly select <i>count</i> entries from the keys in the map, using
   * the values as weights to influence the selection.  E.g., in the map
   * {[A, 1.0], [B, 2.0]}, B would be chosen approximately twice as often
   * as A.
   * @param itemMap maps items to their weight (a double)
   * @param count The number of items to return.
   * @return A collection of items selected randomly from the list.
   * @throws IllegalArgumentException if count is non-positive or
   *         greater than the size of the collection.
   */
  // XXX Can be inefficient - used items are marked and random numbers are
  // tried repeatedly if necessary to find the next unused item.  Probably
  // should shuffle down instead.  (But that requires two array shuffles,
  // one recomputing weight indices.  Maybe do it every N iterations.)

  public static List weightedRandomSelection(Map itemMap, int count) {
    int mapSize = itemMap.size();
    if (count < 0 || count > mapSize) {
      throw new IllegalArgumentException("'count' negative "+
                                         "or greater than collection size.");
    }
    if (count == 0) {
      return Collections.EMPTY_LIST;
    }
    double[] weightIndices = new double[mapSize];
    Object[] items = new Object[mapSize];
    boolean[] used = new boolean[mapSize];
    double totalWeight = 0.0;
    {
      int ix = 0;
      for (Map.Entry ent : (Set<Map.Entry>)itemMap.entrySet()) {
	items[ix] = ent.getKey();
	double weight = (Double)ent.getValue();
	weightIndices[ix] = totalWeight;
	totalWeight += weight;
	ix++;
      }
    }
    ArrayList result = new ArrayList(count);
    while (--count >= 0) {
      while (true) {
	double weightIx = random.nextDouble() * totalWeight;
	int ix = Arrays.binarySearch(weightIndices, weightIx);
	if (ix < 0) {
	  ix = -2 - ix;
	}
	if (used[ix]) {
	  continue;
	}
	result.add(items[ix]);
	used[ix] = true;
	break;
      }
    }
    return result;
  }

}
