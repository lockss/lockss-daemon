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
import java.net.*;
import junit.framework.TestCase;
import org.lockss.test.*;

/**
 * This is the test class for org.lockss.util.ListUtil
 */
public class TestListUtil extends LockssTestCase {
  public static Class testedClasses[] = {
    org.lockss.util.ListUtil.class
  };

  public TestListUtil(String msg) {
    super(msg);
  }

  public void testFromArray() {
    String arr[] = {"1", "2", "4"};
    assertIsomorphic(arr, ListUtil.fromArray(arr));
  }

  public void testFromIterator() {
    String arr[] = {"1", "2", "4"};
    assertEquals(ListUtil.fromArray(arr),
		 ListUtil.fromIterator(new ArrayIterator(arr)));
  }

  public void testFromIteratable() {
    List lst = ListUtil.list("1", "2", "4");
    assertEquals(lst, ListUtil.fromIterable(lst));
  }

  public void testFromCSV() {
    String csv = "1,2,4";
    String arr[] = {"1", "2", "4"};
    assertIsomorphic(arr, ListUtil.fromCSV(csv));
  }

  LinkedList lList(List lst) {
    return new LinkedList(lst);
  }

  public void testPrependAll() {
    assertEquals(Collections.EMPTY_LIST,
		 ListUtil.prependAll(null, (LinkedList)null));
    assertEquals(ListUtil.list("1"),
		 ListUtil.prependAll(ListUtil.list("1"), (LinkedList)null));
    assertEquals(ListUtil.list("1"),
		 ListUtil.prependAll(null, lList(ListUtil.list("1"))));
    assertEquals(Collections.EMPTY_LIST,
		 ListUtil.prependAll(Collections.EMPTY_LIST,
				     lList(Collections.EMPTY_LIST)));
    assertEquals(ListUtil.list("1"),
		 ListUtil.prependAll(ListUtil.list("1"),
				     lList(Collections.EMPTY_LIST)));
    assertEquals(ListUtil.list("1"),
		 ListUtil.prependAll(Collections.EMPTY_LIST,
				     lList(ListUtil.list("1"))));
    assertEquals(ListUtil.list("1", "2", "3"),
		 ListUtil.prependAll(Collections.EMPTY_LIST,
				     lList(ListUtil.list("1", "2", "3"))));
    assertEquals(ListUtil.list("1", "2", "3"),
		 ListUtil.prependAll(ListUtil.list("1", "2", "3"),
				     lList(Collections.EMPTY_LIST)));
    assertEquals(ListUtil.list("1", "2", "3"),
		 ListUtil.prependAll(ListUtil.list("1"),
				     lList(ListUtil.list("2", "3"))));
    assertEquals(ListUtil.list("1", "2", "3"),
		 ListUtil.prependAll(ListUtil.list("1", "2"),
				     lList(ListUtil.list("3"))));
  }

  public void testAppend() {
    List l1 = ListUtil.list("1", "2", "3");
    List l2 = ListUtil.list("a", "b", "c");
    List l3 = ListUtil.list("1", "2", "3", "a", "b", "c");
    assertEquals(Collections.EMPTY_LIST, ListUtil.append((List)null));
    assertEquals(Collections.EMPTY_LIST,
		 ListUtil.append((List)null, (List)null));
    assertEquals(l3, ListUtil.append(l1, l2));
  }

  public void testImmutableListOfType() {
    String arr[] = {"1", "2", "4"};
    List l0 = ListUtil.fromArray(arr);
    List l1 = ListUtil.immutableListOfType(l0, String.class);
    assertEquals(l0, l1);
    l0.add("21");
    assertEquals(l0.size(), l1.size() + 1);
    assertIsomorphic(arr, l1);
    try {
      l1.add("d");
      fail("Should be able to add to immutable list");
    } catch (UnsupportedOperationException e) {
    }
  }

  public void testImmutableListOfSuperType() {
    List l0 = ListUtil.list(new ArrayList(), new LinkedList());
    List l1 = ListUtil.immutableListOfType(l0, List.class);
    assertEquals(l0, l1);
    List l2 = ListUtil.list(new Error(), new LinkageError());
    List l3 = ListUtil.immutableListOfType(l2, Throwable.class);
    assertEquals(l2, l3);
  }

  public void testImmutableListOfWrongType() {
    List l0 = ListUtil.list("foo", "bar", new Integer(7));
    try {
      List l1 = ListUtil.immutableListOfType(l0, String.class);
      fail("immutableListOfType accepted wrong type");
    } catch (ClassCastException e) {
    }
    Integer a2[] = {new Integer(4), null};
    List l2 = ListUtil.fromArray(a2);
    assertIsomorphic(a2,
		     ListUtil.immutableListOfTypeOrNull(l2, Integer.class));
    try {
      ListUtil.immutableListOfType(l2, Integer.class);
      fail("immutableListOfType accepted null");
    } catch (NullPointerException e) {
    }
  }

  public void testReverseCopy() {
    assertEquals(0, ListUtil.reverseCopy(new ArrayList()).size());
    List l0 = ListUtil.list("foo", "bar", new Integer(7));
    List r0 = ListUtil.reverseCopy(l0);
    assertNotSame(l0, r0);
    assertEquals(ListUtil.list(new Integer(7), "bar", "foo"), r0);
  }

  public void testEnsureMinimalArrayList() {
    List l1 = new ArrayList(4);
    l1.add("1");
    l1.add("2");
    l1.add("3");
    List l1c = new ArrayList(l1);
    List l1a = ListUtil.ensureMinimalArrayList(l1);
    assertEquals(l1a, l1c);
    assertSame(l1a, l1);

    List l2 = new LinkedList();
    l2.add("1");
    l2.add("2");
    l2.add("3");
    List l2a = ListUtil.ensureMinimalArrayList(l2);
    assertEquals(l2, l2a);
    assertClass(ArrayList.class, l1a);
  }


}
