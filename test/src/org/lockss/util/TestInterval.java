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
 * This is the test class for org.lockss.util.Interval
 */
public class TestInterval extends LockssTestCase {
  Interval int01;
  Interval int12;
  Interval int13;
  Interval int24;

  public void setUp() throws Exception {
    int01 = new Interval(0,1);
    int12 = new Interval(1,2);
    int13 = new Interval(1,3);
    int24 = new Interval(2,4);
  }

  public void testIsBefore() {
    assertTrue(int01.isBefore(int12));
    assertFalse(int12.isBefore(int01));
    assertFalse(int13.isBefore(int24));
  }
  public void testIsDisjoint() {
    assertTrue(int01.isDisjoint(int12));
    assertTrue(int01.isDisjoint(int24));
    assertFalse(int12.isDisjoint(int13));
    assertFalse(int13.isDisjoint(int24));
  }
  public void testSubsumes() {
    assertTrue(int13.subsumes(int12));
    assertTrue(int13.subsumes(int13));
    assertFalse(int12.subsumes(int13));
    assertFalse(int01.subsumes(int24));
  }
  public void testContains() {
    assertFalse(int13.contains(new Integer(0)));
    assertTrue(int13.contains(new Integer(1)));
    assertTrue(int13.contains(new Integer(2)));
    assertFalse(int13.contains(new Integer(3)));
  }

  public void testEquals() {
    assertFalse(int01.equals(int12));
    assertFalse(int12.equals(int13));
    assertFalse(int12.equals(new Interval(0, 2)));
    assertTrue(int01.equals(int01));
    assertTrue(int13.equals(new Interval(1, 3)));
  }

  public void testHash() {
    assertEquals(new Interval(2,4).hashCode(),
		 new Interval(2,4).hashCode());
  }
}
