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

package org.lockss.util;

import java.util.*;
import junit.framework.TestCase;
import org.lockss.test.*;

/**
 * This is the test class for org.lockss.util.SetUtil
 */
public class TestSetUtil extends LockssTestCase {
  public static Class testedClasses[] = {
    org.lockss.util.SetUtil.class
  };

  public TestSetUtil(String msg) {
    super(msg);
  }

  private Set s1;

  public void setUp() throws Exception {
    super.setUp();
    s1 = new HashSet();
    s1.add("1");
    s1.add("2");
    s1.add("4");
  }

  public void testArgs() {
    assertEquals(s1, SetUtil.set("1", "2", "4"));
  }

  public void testEmpty() {
    assertEquals(Collections.EMPTY_SET, SetUtil.set());
    assertEquals(Collections.EMPTY_SET, SetUtil.theSet(Collections.EMPTY_LIST));
  }

  public void testFromArray() {
    String arr[] = {"1", "2", "4"};
    assertEquals(s1, SetUtil.fromArray(arr));
  }

  public void testFromCSV() {
    String csv = "1,2,4";
    assertEquals(s1, SetUtil.fromCSV(csv));
  }

  public void testFromIterator() {
    String arr[] = {"1", "2", "4"};
    assertEquals(s1, SetUtil.fromIterator(new ArrayIterator(arr)));
  }

  public void testTheSet() {
    Set s1 = SetUtil.theSet(ListUtil.list("1", "2", "3"));
    assertTrue(s1 instanceof Set);
    assertEquals(3, s1.size());
    assertTrue(s1.contains(new String("1")));
    assertTrue(s1.contains("2"));
    assertTrue(s1.contains("3"));
  }

  public void testImmutableSetOfType() {
    String arr[] = {"1", "2", "4"};
    Set s0 = SetUtil.fromArray(arr);
    Set s1 = SetUtil.immutableSetOfType(s0, String.class);
    assertEquals(s0, s1);
    s0.add("21");
    assertEquals(s0.size(), s1.size() + 1);
    assertEquals(SetUtil.fromArray(arr), s1);
    try {
      s1.add("d");
      fail("Shouldn't be able to add to immutable set");
    } catch (UnsupportedOperationException e) {
    }
  }

  public void testImmutableSetOfSuperType() {
    Set s0 = SetUtil.set(new ArrayList(), new LinkedList());
    Set s1 = SetUtil.immutableSetOfType(s0, List.class);
    assertEquals(s0, s1);
    Set s2 = SetUtil.set(new Error(), new LinkageError());
    Set s3 = SetUtil.immutableSetOfType(s2, Throwable.class);
    assertEquals(s2, s3);
  }

  public void testImmutableSetOfWrongType() {
    Set s0 = SetUtil.set("foo", "bar", new Integer(7));
    try {
      Set s1 = SetUtil.immutableSetOfType(s0, String.class);
      fail("immutableSetOfType accepted wrong type");
    } catch (ClassCastException e) {
    }
    Integer a2[] = {new Integer(4), null};
    Set s2 = SetUtil.fromArray(a2);
    assertEquals(s2, SetUtil.immutableSetOfTypeOrNull(s2, Integer.class));
    try {
      SetUtil.immutableSetOfType(s2, Integer.class);
      fail("immutableSetOfType accepted null");
    } catch (NullPointerException e) {
    }
  }
}
