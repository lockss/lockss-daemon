/*
 * $Id: TestBoundedTreeSet.java,v 1.1 2007-10-01 08:16:57 tlipkis Exp $
 */

/*

Copyright (c) 2000-2007 Board of Trustees of Leland Stanford Jr. University,
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
import org.lockss.test.*;

public class TestBoundedTreeSet extends LockssTestCase {
  public static Class testedClasses[] = {
    org.lockss.util.BoundedTreeSet.class
  };

  Set set = new BoundedTreeSet(4);

  public void testNatural() {
    Set set = new BoundedTreeSet(4);
    assertTrue(set.isEmpty());
    assertEquals(0, set.size());
    assertFalse(set.contains(null));
    assertTrue(set.add("6"));
    assertTrue(set.add("2"));
    assertTrue(set.add("4"));
    assertTrue(set.add("9"));
    assertEquals(ListUtil.list("2", "4", "6", "9"), new ArrayList(set));
    assertTrue(set.contains("6"));
    assertTrue(set.contains("2"));
    assertTrue(set.contains("4"));
    assertTrue(set.contains("9"));

    assertTrue(set.add("8"));
    assertEquals(ListUtil.list("2", "4", "6", "8"), new ArrayList(set));
    assertTrue(set.contains("8"));
    assertFalse(set.contains("9"));
    assertTrue(set.add("1"));
    assertEquals(ListUtil.list("1", "2", "4", "6"), new ArrayList(set));
  }

  public void testComparator() {
    Set set = new BoundedTreeSet(4, new Comparator() {
	public int compare(Object a, Object b) {
	  return ((Comparable)b).compareTo(((Comparable)a));
	}});
    assertTrue(set.isEmpty());
    assertEquals(0, set.size());
    assertFalse(set.contains(null));
    assertTrue(set.add("6"));
    assertTrue(set.add("2"));
    assertTrue(set.add("4"));
    assertTrue(set.add("9"));
    assertEquals(ListUtil.list("9", "6", "4", "2"), new ArrayList(set));
    assertTrue(set.contains("6"));
    assertTrue(set.contains("2"));
    assertTrue(set.contains("4"));
    assertTrue(set.contains("9"));

    assertTrue(set.add("8"));
    assertEquals(ListUtil.list("9", "8", "6", "4"), new ArrayList(set));
    assertTrue(set.contains("9"));
    assertFalse(set.contains("2"));
    assertTrue(set.add("1"));
    assertEquals(ListUtil.list("9", "8", "6", "4"), new ArrayList(set));
  }
}
