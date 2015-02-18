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
 * This is the test class for org.lockss.util.OrderedObject
 */
public class TestOrderedObject extends LockssTestCase {
  OrderedObject oo01;
  OrderedObject oo12;
  OrderedObject oo13;
  OrderedObject oo24;

  public void setUp() throws Exception {
  }

  public void testEquals() {
    OrderedObject o1 = new OrderedObject("foo", 3);
    OrderedObject o1a = new OrderedObject(new String("foo"), 4);
    OrderedObject o2 = new OrderedObject("bar", 5);
    OrderedObject o3 = new OrderedObject("bar", 3);
    assertEquals(o1, o1a);
    assertNotEquals(o1, o2);
    assertNotEquals(o1, o3);
  }

  public void testGets() {
    OrderedObject o1 = new OrderedObject("foo", 3);
    OrderedObject o2 = new OrderedObject(new Integer(123), 5);
    OrderedObject o3 = new OrderedObject("a", new Integer(33));
    assertEquals("foo", o1.getValue());
    assertEquals("foo", o1.toString());
    assertEquals(new Integer(3), o1.getOrder());
    assertEquals("123", o2.toString());
    OrderedObject o4 = new OrderedObject("bar");
    assertEquals("bar", o4.getOrder());
    assertEquals("bar", o4.getValue());
  }

  public void testSort() {
    OrderedObject o1 = new OrderedObject("foo", 3);
    OrderedObject o2 = new OrderedObject("bar", 5);
    OrderedObject o3 = new OrderedObject("a", new Integer(33));
    List exp = ListUtil.list(o1, o2, o3);
    List test = ListUtil.list(o3, o2, o1);
    Collections.sort(test);
    assertEquals(exp, test);
    Collections.shuffle(test);
    Collections.sort(test);
    assertEquals(exp, test);
  }

  public void testIll() {
    OrderedObject o1 = new OrderedObject("foo", 3);
    OrderedObject o2 = new OrderedObject("bar", 5);
    OrderedObject o3 = new OrderedObject("a", "nonint");
    List test = ListUtil.list(o3, o2, o1);
    try {
      Collections.sort(test);
      fail("Should have thrown ClassCastException");
    } catch (ClassCastException e) {
    }
  }

}
