/*
 * $Id$
 *

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

/**
 * Create a set of Object from a call list. */
public class SetUtil {
  /**
   * Don't construct. */
  private SetUtil() {
  }

  /**
   * Create set from arg list. */
  public static Set set() {
    return new HashSet();
  }

  /**
   * Create a list from any number of arguments. */
  public static Set set(Object... elements) {
    Set l = new HashSet();
    if (elements == null) { return l; }
    for (Object arg : elements) {
      l.add(arg);
    }
    return l;
  }
  
  /**
   * Create set from arg list. */
  public static Set set(Object object1) {
    Set l = new HashSet();
    l.add(object1);
    return l;
  }

  /**
   * Create set from arg list. */
  public static Set set(Object object1, Object object2) {
    Set l = set(object1);
    l.add(object2);
    return l;
  }

  /**
   * Create set from arg list. */
  public static Set set(Object object1,
			Object object2,
			Object object3) {
    Set l = set(object1, object2);
    l.add(object3);
    return l;
  }

  /**
   * Create set from arg list. */
  public static Set set(Object object1,
			Object object2,
			Object object3,
			Object object4) {
    Set l = set(object1, object2, object3);
    l.add(object4);
    return l;
  }

  /**
   * Create set from arg list. */
  public static Set set(Object object1,
			Object object2,
			Object object3,
			Object object4,
			Object object5) {
    Set l = set(object1, object2, object3, object4);
    l.add(object5);
    return l;
  }

  /**
   * Create set from arg list. */
  public static Set set(Object object1,
			Object object2,
			Object object3,
			Object object4,
			Object object5,
			Object object6) {
    Set l = set(object1, object2, object3, object4, object5);
    l.add(object6);
    return l;
  }
  /**
   * Create set from arg list. */
  public static Set set(Object object1,
			Object object2,
			Object object3,
			Object object4,
			Object object5,
			Object object6,
			Object object7) {
    Set l = set(object1, object2, object3, object4, object5,
		object6);
    l.add(object7);
    return l;
  }
  /**
   * Create set from arg list. */
  public static Set set(Object object1,
			Object object2,
			Object object3,
			Object object4,
			Object object5,
			Object object6,
			Object object7,
			Object object8) {
    Set l = set(object1, object2, object3, object4, object5,
		object6, object7);
    l.add(object8);
    return l;
  }
  /**
   * Create set from arg list. */
  public static Set set(Object object1,
			Object object2,
			Object object3,
			Object object4,
			Object object5,
			Object object6,
			Object object7,
			Object object8,
			Object object9) {
    Set l = set(object1, object2, object3, object4, object5,
		object6, object7, object8);
    l.add(object9);
    return l;
  }
  /**
   * Create set from arg list. */
  public static Set set(Object object1,
			Object object2,
			Object object3,
			Object object4,
			Object object5,
			Object object6,
			Object object7,
			Object object8,
			Object object9,
			Object object10) {
    Set l = set(object1, object2, object3, object4, object5,
		object6, object7, object8, object9);
    l.add(object10);
    return l;
  }

  /** Check that all elements of the set are of the specified type, and
   * return an unmodifiable copy of the set.
   * @param set the set.
   * @param type the class with which all items of the set
   * must be assignment-compatible.
   * @throws NullPointerException if the set is null or if any element
   * is null
   * @throws ClassCastException if an item is not of the proper type
   */
  public static Set immutableSetOfType(Set set, Class type) {
    return immutableSetOfType(set, type, false);
  }

  /** Check that all elements of the set are either of the specified type
   * or null, and
   * return an unmodifiable copy of the set.
   * @param set the set.
   * @param type the class with which all items of the set
   * must be assignment-compatible.
   * @throws NullPointerException if the set is null
   * @throws ClassCastException if an item is not of the proper type
   */
  public static Set immutableSetOfTypeOrNull(Set set, Class type) {
    return immutableSetOfType(set, type, true);
  }

  private static Set immutableSetOfType(Set set, Class type,
                                          boolean nullOk) {
    Set s = new HashSet(set.size());
    for (Iterator iter = set.iterator(); iter.hasNext(); ) {
      Object item = iter.next();
      if (item == null) {
        if (!nullOk) {
          throw new NullPointerException("item of list is null");
        }
      } else if (!type.isInstance(item)) {
        throw new ClassCastException("item <" + item +
                                     "> of list is not an instance of "
                                     + type);
      }
      s.add(item);
    }
    return Collections.unmodifiableSet(s);
  }
  /** Create a set containing the elements of an array */
  public static Set fromArray(Object array[]) {
    Set l = set();
    for (int i = 0; i < array.length; i++) {
      l.add(array[i]);
    }
    return l;
  }

  /** Create a set containing the elements of a Collection */
  public static Set theSet(Collection coll) {
    return new HashSet(coll);
  }

  /** Create a set containing the elements of a list */
  public static Set fromList(List list) {
    return theSet(list);
  }

  /** Create a set containing the elements of an iterator */
  public static Set fromIterator(Iterator iterator) {
    Set l = set();
    while (iterator.hasNext()) {
      l.add(iterator.next());
    }
    return l;
  }

  /** Create a set containing the elements of a comma separated string */
  public static Set fromCSV(String csv) {
    Set res = set();
    StringTokenizer st = new StringTokenizer(csv, ",");
    while (st.hasMoreTokens()) {
      String id = (String)st.nextToken();
      res.add(id);
    }
    return res;
  }
}
