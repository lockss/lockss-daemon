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
import org.lockss.test.*;

/**
 * Test class for org.lockss.util.ArrayIterator
 */

public class TestArrayIterator extends LockssTestCase {

  public static Class testedClasses[] = {
    org.lockss.util.ArrayIterator.class
  };

  public TestArrayIterator(String msg) {
    super(msg);
  }

  public void testNullIter() {
    try {
      Iterator iter = new ArrayIterator(null);
      fail("Should raise NullPointerException");
    } catch (NullPointerException e) {
    }
  }

  static String empty[] = {};
  public void testEmptyIter() {
    Iterator iter = new ArrayIterator(empty);
    assertTrue( ! iter.hasNext());
  }

  static String arr[] = {"one", "two"};
  public void testIter() {
    Iterator iter = new ArrayIterator(arr);
    for (int ix = 0; ix < arr.length; ix++) {
      assertTrue(iter.hasNext());
      assertEquals(arr[ix], (String)iter.next());
    }
    assertTrue( ! iter.hasNext());
  }

}
