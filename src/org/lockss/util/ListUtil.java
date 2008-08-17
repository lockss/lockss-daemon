/*
 * $Id: ListUtil.java,v 1.14 2008-08-17 08:49:15 tlipkis Exp $
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
 * Create a list of Object from a call list. */
public class ListUtil {
  /**
   * Don't construct. */
  private ListUtil() {
  }
  
  /**
   * Return an empty array list. */
  public static List list() {
    return new ArrayList();
  }

  /**
   * Create a list from any number of arguments. */
  public static List list(Object... elements) {
    ArrayList l = new ArrayList();
    if (elements == null) { return l; }
    for (Object arg : elements) {
      l.add(arg);
    }
    l.trimToSize();
    return l;
  }
  
  /* NOTE:  The following non-varargs methods cannot yet be removed, because
   * external plugin code relies on them as of the 1.26 daemon release.
   * Until the plugin code is audited and updated, these methods must remain.
   */

  /**
   * Create list from arg list. */
  private static List list1(Object object1) {
      List l = new ArrayList();
      l.add(object1);
      return l;
  }

  /**
   * Create list from arg list. */
  public static List list(Object object1) {
      List l = new ArrayList(1);
      l.add(object1);
      return l;
  }

  /**
   * Create list from arg list. */
  public static List list(Object object1, Object object2) {
      List l = list1(object1);
      l.add(object2);
      return l;
  }

  /**
   * Create list from arg list. */
  public static List list(Object object1,
                          Object object2,
                          Object object3) {
      List l = list(object1, object2);
      l.add(object3);
      return l;
  }

  /**
   * Create list from arg list. */
  public static List list(Object object1,
                          Object object2,
                          Object object3,
                          Object object4) {
      List l = list(object1, object2, object3);
      l.add(object4);
      return l;
  }

  /**
   * Create list from arg list. */
  public static List list(Object object1,
                          Object object2,
                          Object object3,
                          Object object4,
                          Object object5) {
      List l = list(object1, object2, object3, object4);
      l.add(object5);
      return l;
  }

  /**
   * Create list from arg list. */
  public static List list(Object object1,
                          Object object2,
                          Object object3,
                          Object object4,
                          Object object5,
                          Object object6) {
      List l = list(object1, object2, object3, object4, object5);
      l.add(object6);
      return l;
  }
  /**
   * Create list from arg list. */
  public static List list(Object object1,
                          Object object2,
                          Object object3,
                          Object object4,
                          Object object5,
                          Object object6,
                          Object object7) {
      List l = list(object1, object2, object3, object4, object5,
                    object6);
      l.add(object7);
      return l;
  }
  /**
   * Create list from arg list. */
  public static List list(Object object1,
                          Object object2,
                          Object object3,
                          Object object4,
                          Object object5,
                          Object object6,
                          Object object7,
                          Object object8) {
      List l = list(object1, object2, object3, object4, object5,
                    object6, object7);
      l.add(object8);
      return l;
  }
  /**
   * Create list from arg list. */
  public static List list(Object object1,
                          Object object2,
                          Object object3,
                          Object object4,
                          Object object5,
                          Object object6,
                          Object object7,
                          Object object8,
                          Object object9) {
      List l = list(object1, object2, object3, object4, object5,
                    object6, object7, object8);
      l.add(object9);
      return l;
  }
  /**
   * Create list from arg list. */
  public static List list(Object object1,
                          Object object2,
                          Object object3,
                          Object object4,
                          Object object5,
                          Object object6,
                          Object object7,
                          Object object8,
                          Object object9,
                          Object object10) {
      List l = list(object1, object2, object3, object4, object5,
                    object6, object7, object8, object9);
      l.add(object10);
      return l;
  }

  /**
   * Create list from arg list. */
  public static List list(Object object1,
                          Object object2,
                          Object object3,
                          Object object4,
                          Object object5,
                          Object object6,
                          Object object7,
                          Object object8,
                          Object object9,
                          Object object10,
                          Object object11) {
      List l = list(object1, object2, object3, object4, object5,
                    object6, object7, object8, object9, object10);
      l.add(object11);
      return l;
  }

  /**
   * Append lists together. */
  public static List append(List... lists) {
    List res = new ArrayList();
    for (int ix = 0; ix < lists.length; ix++) {
      if (lists[ix] != null) {
        res.addAll(lists[ix]);
      }
    }
    return res;
  }
  
  /** Create a list containing the elements of an array */
  public static List fromArray(Object array[]) {
    List l = new ArrayList(array.length);
    for (Object o : array) {
      l.add(o);
    }
    return l;
  }
  
  /** Add all elements of ofList to toList  */
  public static LinkedList prependAll(List ofList, LinkedList toList) {
    if (ofList == null) {
      if (toList == null) {
	return new LinkedList();
      } else {
	return toList;
      }
    }
    if (toList == null) {
      return new LinkedList(ofList);
    }
    List revOfList = new ArrayList(ofList);
    Collections.reverse(revOfList);
    for (Iterator iter = revOfList.iterator(); iter.hasNext(); ) {
      toList.addFirst(iter.next());
    }
    return toList;
  }

  /** Create a list containing the elements of an iterator */
  public static List fromIterator(Iterator iterator) {
    List l = list();
    while (iterator.hasNext()) {
      l.add(iterator.next());
    }
    return l;
  }

  /** Create a list containing the elements of a comma separated string */
    public static List fromCSV(String csv) {
	List res = list();
	StringTokenizer st = new StringTokenizer(csv, ",");
	while (st.hasMoreTokens()) {
	    String id = (String)st.nextToken();
	    res.add(id);
	}
	return res;
    }

  /** Check that all elements of the list are of the specified type, and
   * return an unmodifiable copy of the list.
   * @param list the list.
   * @param type the class with which all items of the list
   * must be assignment-compatible.
   * @throws NullPointerException if the list is null or if any element
   * is null
   * @throws ClassCastException if an item is not of the proper type
   */
  public static List immutableListOfType(List list, Class type) {
    return immutableListOfType(list, type, false);
  }

  /** Check that all elements of the list are either of the specified type
   * or null, and
   * return an unmodifiable copy of the list.
   * @param list the list.
   * @param type the class with which all items of the list
   * must be assignment-compatible.
   * @throws NullPointerException if the list is null
   * @throws ClassCastException if an item is not of the proper type
   */
  public static List immutableListOfTypeOrNull(List list, Class type) {
    return immutableListOfType(list, type, true);
  }

  private static List immutableListOfType(List list, Class type,
					  boolean nullOk) {
    List l = new ArrayList(list.size());
    for (Iterator iter = list.iterator(); iter.hasNext(); ) {
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
      l.add(item);
    }
    return Collections.unmodifiableList(l);
  }

  /** Return a copy of the list, in reverse order.
   * @param list the List to reverse.
   * @return A new list with elements in reverse order of the original list.
   */
  public static List reverseCopy(List list) {
    List res = new ArrayList(list);
    Collections.reverse(res);
    return res;
  }
}
