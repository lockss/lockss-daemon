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
import junit.framework.TestCase;
import org.lockss.test.LockssTestCase;

/**
 * Test class for org.lockss.util.PermutationIterator
 */

public class TestPermutationIterator extends LockssTestCase {

  public void testIllIter() {
    try {
      Iterator iter = new PermutationIterator(-1);
      fail("Should have thrown");
    } catch (Exception e) {
    }
  }

  void assertPerm(int[][] expected, Iterator actual) {
    int n = expected.length;
    for (int ix = 0; ix < n; ix++) {
      assertTrue("Only " + ix + " permutations, should be " + n,
		 actual.hasNext());
      assertEquals("permutation " + (ix + 1),
		   expected[ix], (int [])actual.next());
    }
    assertFalse("Greater than " + n + " permutations", actual.hasNext());
  }

  public void test0() {
    Iterator iter = new PermutationIterator(0);
    assertFalse(iter.hasNext());
  }

  int[][] e1 = {{0}};
  public void test1() {
    Iterator iter = new PermutationIterator(1);
    assertTrue(iter.hasNext());
    assertPerm(e1, iter);
  }

  int[][] e2 = {{0,1},{1,0}};
  public void test2() {
    Iterator iter = new PermutationIterator(2);
    assertTrue(iter.hasNext());
    assertPerm(e2, iter);
  }

  int[][] e3 = {{0,1,2},{0,2,1},{1,0,2},{1,2,0},{2,0,1},{2,1,0}};
  public void test3() {
    Iterator iter = new PermutationIterator(3);
    assertTrue(iter.hasNext());
    assertPerm(e3, iter);
  }
}
