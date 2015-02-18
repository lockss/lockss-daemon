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
 * Test class for org.lockss.util.EnumerationIterator
 */
public class TestEnumerationIterator extends LockssTestCase {
  public static Class testedClasses[] = {
    org.lockss.util.EnumerationIterator.class
  };

  public TestEnumerationIterator(String msg) {
    super(msg);
  }

  public void testEmpty() {
    Vector v = new Vector();
    Iterator iter = new EnumerationIterator(v.elements());
    assertFalse(iter.hasNext());
  }

  public void testN() {
    Vector v = new Vector();
    v.add("one");
    v.add("two");
    Iterator iter = new EnumerationIterator(v.elements());
    assertTrue(iter.hasNext());
    assertEquals("one", iter.next());
    assertTrue(iter.hasNext());
    assertEquals("two", iter.next());
    assertTrue( ! iter.hasNext());
    try {
      iter.next();
      fail("Should raise java.util.NoSuchElementException");
    } catch (java.util.NoSuchElementException e) {
    }
  }
}
